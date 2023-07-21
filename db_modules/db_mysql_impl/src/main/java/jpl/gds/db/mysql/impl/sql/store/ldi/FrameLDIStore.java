/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.db.mysql.impl.sql.store.ldi;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.IFrameLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILDIStore;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeTooLargeException;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.InvalidFrameCode;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;


/**
 * This is the database write/storage interface to the Frame table in the
 * MPCS database. This class will receive an input frame and write it and
 * its metadata to the Frame table in the database. It uses LDI to insert.
 *
 * This class is very similar to the equivalent non-LDI class. The fields are
 * dumped into a BytesBuilder instead of a PreparedStatement, and written by
 * means of a table-specific call to the superclass.
 *
 * startResource and stopResource also make table-specific calls to enable or
 * disable processing.
 *
 * Note that we do NOT store a zero-length (or NULL) trailer or header, but we
 * DO store a zero-length headerLength or trailerLength. A zero-length header
 * or trailer represents a not-supplied optional header/trailer. We do not
 * validate anything here except the maximum lengths.
 *
 */
public class FrameLDIStore extends AbstractLDIStore implements IFrameLDIStore
{
    private static final int TYPE_LENGTH = 32;

    private final BytesBuilder _bb = new BytesBuilder();
    private final Integer          _sessionDss;  // Zero means not set
    private final Integer      _sessionVcid; // null means not set
    private final boolean      _storeIdle;


    /**
     * Creates an instance of IFrameLDIStore.
     *
     * MPCS-4839 Do not allow negative values here. Use constants.
     *
     * @param appContext The test configuration for the current test session
     */
    public FrameLDIStore(final ApplicationContext appContext)
    {
    	/* 
    	 * MPCS-7135 -  Add second argument to indicate this
    	 * store does not operate asynchronously.
    	 */
        super(appContext, IFrameLDIStore.STORE_IDENTIFIER, false);

        _storeIdle   = dbProperties.getStoreIdleFrames();

		_sessionDss  = appContext.getBean(IContextFilterInformation.class).getDssId();
		_sessionVcid =  appContext.getBean(IContextFilterInformation.class).getVcid();

    }

//        if (! isStationInRange(_sessionDss))
//        {
//            throw new StoreInitiationException("Configured station "              +
//                                               _sessionDss                        +
//                                               " is outside of range ["           +
//                                               SessionConfiguration.MINIMUM_DSSID +
//                                               "-"                                +
//                                               SessionConfiguration.MAXIMUM_DSSID +
//                                               "]");
//        }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ldi.IFrameLDIStore#insertFrame(java.lang.String, jpl.gds.shared.time.IAccurateDateTime, int, int, int, int, double, byte[], jpl.gds.shared.holders.HeaderHolder, jpl.gds.shared.holders.TrailerHolder, jpl.gds.shared.holders.FrameIdHolder, jpl.gds.station.api.InvalidFrameCode, boolean)
     */
    @Override
    public void insertFrame(final String           frameType,
                            final IAccurateDateTime ert,
                            final int              relayScid,
                            final int              vcid,
                            final int              rawVcfc,
                            final int              dss,
                            final double           bitRate,
                            final byte[]           frame,
                            final HeaderHolder     header,
                            final TrailerHolder    trailer,
                            final FrameIdHolder    frameId,
                            final InvalidFrameCode badReason,
                            final boolean          fillFrame)
        throws DatabaseException
    {
        if (!dbProperties.getUseDatabase() || isStoreStopped.get())
        {
            return;
        }

        checkBinaryLength(frame, ILDIStore.MAX_FRAME, "body");

        /** MPCS-6809  Expand to unsigned long */
        final UnsignedLong vcfc = UnsignedLong.valueOfIntAsUnsigned(rawVcfc);

        /*
         * BEGIN MPCS-4839
         * For historical reasons we allow negative values here and fix them.
         * Otherwise must be within bounds.
         */

        final int mdss = Math.max(dss, StationIdHolder.MIN_VALUE);

//        if (! isStationInRange(mdss))
//        {
//            logError("Frame ",
//                     vcfc,
//                     " DSS ",
//                     mdss,
//                     " is outside of range [",
//                     SessionConfiguration.MINIMUM_DSSID,
//                     "-",
//                     SessionConfiguration.MAXIMUM_DSSID,
//                     "] and will be forced to ",
//                     SessionConfiguration.MINIMUM_DSSID);
//
//            mdss = SessionConfiguration.MINIMUM_DSSID;
//        }

        if (! isValidDssId(mdss, _sessionDss))
        {
            trace.warn(Markers.DB,
                    "Frame " , vcfc , " DSS " , mdss , " does not match Session DSS " , _sessionDss == null
                    ? StationIdHolder.MIN_VALUE : _sessionDss , " and will not be stored");
            return;
        }

        /*
         * END MPCS-4839
         */

        final int mvcid = Math.max(vcid, 0);

        if ((_sessionVcid != null) && (mvcid != _sessionVcid))
        {
            trace.warn(Markers.DB, "Frame " , vcfc , " VCID " , mvcid , " does not match Session VCID " , _sessionVcid
                    , " and will not be stored");
            return;
        }

        try
        {
            synchronized (this)
            {
                if (!archiveController.isUp())
                {
                    throw new IllegalStateException(
                                  "This connection has already been closed");
                }

                _bb.clear();

                // Format as a line for LDI

                _bb.insert(contextConfig.getContextId().getNumber().longValue());
                _bb.insertSeparator();
                _bb.insert(contextConfig.getContextId().getHostId());
                _bb.insertSeparator();

                _bb.insert(contextConfig.getContextId().getFragment());
                _bb.insertSeparator();

                /** MPCS-6809 Expand to unsigned long */
                /** MPCS-5932  Use holder */

                frameId.insert(_bb);
                _bb.insertSeparator();

                final String type = StringUtil.safeTrim(frameType);

                if (type.isEmpty())
                {
                    trace.warn(Markers.DB, "Empty Frame.type");
                }

                /** MPCS-5153 */
                _bb.insertTextComplainReplace(checkLength("Frame.type",
                                                          TYPE_LENGTH,
                                                          type));
                _bb.insertSeparator();

                final IAccurateDateTime rct = new AccurateDateTime();

                try
                {
                    _bb.insertDateAsCoarseFineSeparate(rct);
                }
                catch (final TimeTooLargeException ttle)
                {
                    trace.warn(Markers.DB, dateExceedsWarning("Frame.rct", null, rct));
                }

                try
                {
                    _bb.insertErtAsCoarseFineSeparate(ert);
                }
                catch (final TimeTooLargeException ttle)
                {
                    trace.warn(Markers.DB, ertExceedsWarning("Frame.ert", null, ert));
                }

                if (relayScid != 0)
                {
                    _bb.insert(relayScid);
                }
                else
                {
                    _bb.insertNULL();
                }

                _bb.insertSeparator();

                _bb.insert(mvcid);
                _bb.insertSeparator();

                /** MPCS-6809  Now unsigned long */
                if (vcfc != null)
                {
                    _bb.insert(vcfc);
                }
                else
                {
                    _bb.insertNULL();
                }

                _bb.insertSeparator();

                _bb.insert(mdss);
                _bb.insertSeparator();

                if (Double.isNaN(bitRate))
                {
                    trace.error(Markers.DB, "NaN bitRate set to NULL");

                    _bb.insertNULL();
                }
                else if (Double.isInfinite(bitRate))
                {
                    trace.error(Markers.DB, "Infinite bitRate set to NULL");

                    _bb.insertNULL();
                }
                else if (bitRate <= 0.0D)
                {
                    _bb.insertNULL();
                }
                else
                {
                    _bb.insert((float) bitRate);
                }

                _bb.insertSeparator();

                if (badReason != null)
                {
                    _bb.insertSafe(badReason.toString());
                }
                else
                {
                    _bb.insertNULL();
                }

                _bb.insertSeparator();

                _bb.insert(fillFrame ? 1 : 0);
                _bb.insertSeparator();

                _bb.insert((frame != null) ? frame.length : 0);
                _bb.insertSeparator();

                // headerLength: can be zero length

                header.insertLength(_bb);
                _bb.insertSeparator();

                // trailerLength: can be zero length

                trailer.insertLength(_bb);
                _bb.insertTerminator();

                // Add the line statement to the LDI batch

                final BytesBuilder bb_body = new BytesBuilder();
                
                bb_body.insert(contextConfig.getContextId().getNumber().longValue());
                bb_body.insertSeparator();
                bb_body.insert(contextConfig.getContextId().getHostId());
                bb_body.insertSeparator();

                bb_body.insert(contextConfig.getContextId().getFragment());
                bb_body.insertSeparator();

                bb_body.insertBlob((frame != null) ? frame: new byte[0]);
                bb_body.insertSeparator();

                // header: do not store zero length

                header.insert(bb_body);
                bb_body.insertSeparator();

                // trailer: do not store zero length

                trailer.insert(bb_body);
                bb_body.insertSeparator();

                /** MPCS-6809 Expand to unsigned long */
                /** MPCS-5932 Use holder */

                frameId.insert(bb_body);
                bb_body.insertTerminator();
                
                writeToStream(_bb, bb_body);
            }
        }
        catch (final RuntimeException re) {
            throw re;
        }
        catch (final Exception e) {
            final DatabaseException de = new DatabaseException("Error inserting Frame record into database");
            de.initCause(e);
            throw de;
        }
    }

    @Override
    protected void startResource() {
        super.startResource();

        handler = new BaseMessageHandler() {
            @Override
            public synchronized void handleMessage(final IMessage m) {
                handleTFMessage((ITelemetryFrameMessage) m);
            }
        };

        // Subscribe to transfer frame messages
        if (messagePublicationBus != null) {
            messagePublicationBus.subscribe(TmServiceMessageType.TelemetryFrame, handler);
        }
    }

    @Override
    protected void stopResource() {
        super.stopResource();

        // Unsubscribe from transfer frame messages
        if (messagePublicationBus != null) {
            messagePublicationBus.unsubscribe(TmServiceMessageType.TelemetryFrame, handler);
        }
    }

    /**
     * Check the length of a binary type and throw if exceeded.
     *
     * @param binary
     *            The byte length or null
     * @param max
     *            The maximum length
     * @param type
     *            Type as string
     *
     * @throws IllegalArgumentException
     *             if the length of the binary is too long
     */
    protected void checkBinaryLength(final Integer binary,
                                          final int     max,
                                          final String  type)
        throws IllegalArgumentException
    {
        if ((binary != null) && (binary.intValue() > max))
        {
            throw new IllegalArgumentException("Frame "              +
                                               type                  +
                                               " length too large: " +
                                               binary                +
                                               " exceeds "           +
                                               max);
        }
    }


    /**
     * Check the length of a binary type and throw if exceeded.
     *
     * @param binary
     *            The bytes
     * @param max
     *            The maximum length
     * @param type
     *            Type as string
     *
     * @throws IllegalArgumentException
     *             if the length of the binary is too long
     */
    protected void checkBinaryLength(final byte[] binary,
                                          final int    max,
                                          final String type)
        throws IllegalArgumentException
    {
        checkBinaryLength((binary != null)
                              ? Integer.valueOf(binary.length)
                              : null,
                          max,
                          type);
    }


    /**
     * Receive a transfer frame message from the internal bus. If the frame is
     * an idle frame, throw it out, otherwise insert it into the database.
     *
     * @param tfm The transfer frame message received from the internal bus
     */
    protected void handleTFMessage(final ITelemetryFrameMessage tfm)
    {
        final ITelemetryFrameInfo tf = tfm.getFrameInfo();
        final boolean         idle = tf.isIdle();

        if (idle && ! _storeIdle)
        {
            // Do not store idle/fill frames
            return;
        }

        final IStationTelemInfo    dsn    = tfm.getStationInfo();
        InvalidFrameCode reason = tf.getBadReason();
        boolean          isbad  = tf.isBad();

        if (isbad)
        {
            if (reason == null)
            {
                reason = InvalidFrameCode.UNKNOWN;
            }
        }
        else
        {
            isbad = (reason != null);
        }

        try
        {
            if (tf.isDeadCode() ||
                (isbad && ! dbProperties.getStoreBadFrames()))
            {
                return;
            }

            /** MPCS-5932 Use frame id from message */

            insertFrame(tf.getType(),
                        dsn.getErt(),
                        dsn.getRelayScid(),
                        tf.getVcid(),
                        tf.getSeqCount(),
                        dsn.getDssId(),
                        dsn.getBitRate(),
                        tfm.getFrame(),
                        tfm.getRawHeader(),
                        tfm.getRawTrailer(),
                        tfm.getFrameId(),
                        reason,
                        idle);
            
			endSessionInfo.updateTimes(dsn.getErt(),null,null);

        }
        catch (final DatabaseException de)
        {
            trace.error("LDI transfer frame store failed: ", de);
        }
    }
}
