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

import java.sql.SQLException;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.ILDIStore;
import jpl.gds.db.api.sql.store.ldi.IPacketLDIStore;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.TimeTooLargeException;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;


/**
 * This is the database write/storage interface to the Packet table in the
 * MPCS database. This class will receive an input packet and write it and
 * its body to the Packet table in the database. This is done through
 * LOAD DATA INFILE.
 *
 * This class is very similar to the equivalent non-LDI class. The fields are
 * dumped into a BytesBuilder instead of a PreparedStatement, and written by
 * means of a table-specific call to the superclass.
 *
 * startResource and stopResource also make table-specific calls to enable or
 * disable processing.
 *
 * Id values now obtained from PacketIdHolder.
 *
 */
public class PacketLDIStore extends AbstractLDIStore implements IPacketLDIStore
{
    private static final int APID_NAME_LENGTH = 64;

    /**
     * ByteBuilder for packet metadata
     */
    protected final BytesBuilder _bb     = new BytesBuilder();

    /**
     * ByteBuilder for packet body
     */
    protected final BytesBuilder _bbBody = new BytesBuilder();

    private final Integer      _sessionDss;  // Zero means not set
    private final Integer      _sessionVcid; // null means not set
    private final boolean      _storeIdle;

    private final boolean archiveFrames;

    /**
     * Creates an instance of PacketLDIStore.
     *
     * MPCS-4839 Do not allow negative values here. Use constants.
     *
     * @param appContext
     *            the Spring Application Context
     */
    public PacketLDIStore(final ApplicationContext appContext)
    {
    	this(appContext, false);
    }

    /**
     * Creates an instance of PacketLDIStore.
     *
     * MPCS-4839 Do not allow negative values here. Use constants.
     *
     * @param appContext
     *            the Spring Application Context
     * @param supportsAsync
     *            true if this LDI Store is to support ASYNC writing, false if not
     */
    public PacketLDIStore(final ApplicationContext appContext, final boolean supportsAsync)
    {
    	/* 
    	 * MPCS-7135 - Add second argument to indicate this
    	 * store does not operate asynchronously.
    	 */
        this(appContext, IPacketLDIStore.STORE_IDENTIFIER, supportsAsync);
    }

    /**
     * MPCS-8475 - Added to support subclass initialization.
     *
     * @param appContext
     *            the Spring Application Context
     * @param si
     *            the Store Identifier for this LDI store
     * @param supportsAsync
     *            true if this LDI Store is to support ASYNC writing, false if not
     */
    protected PacketLDIStore(final ApplicationContext appContext, final StoreIdentifier si, final boolean supportsAsync)
    {
        /* 
         * MPCS-7135 -  Add second argument to indicate this
         * store does not operate asynchronously.
         */
        super(appContext, si, supportsAsync);

        _sessionDss  = appContext.getBean(IContextFilterInformation.class).getDssId();
        _sessionVcid =  appContext.getBean(IContextFilterInformation.class).getVcid();
        _storeIdle   = dbProperties.getStoreIdlePackets();  
        
        
        archiveFrames = archiveController.getUseArchive(StoreIdentifier.Frame);
        

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
    }

    /**
     * Format common portion of Packet/SsePacket for LDI.
     *
     * @param contextConfig         Test configuration
     * @param bb         BytesBuilder to populate
     * @param pi         Packet info object
     * @param packetData Raw binary packet
     * @param name       Table name
     * @param key        Packet key
     * @param header     Header holder
     * @param trailer    Trailer holder
     * @param terminate  If true, terminate
     *
     * @throws DatabaseException SQL error
     */
    public void formatPacketCommon(
                           final IContextConfiguration contextConfig,
                           final BytesBuilder          bb,
                           final ITelemetryPacketInfo  pi,
                           final byte[]                packetData,
                           final String                name,
                           final PacketIdHolder        key,
                           final HeaderHolder          header,
                           final TrailerHolder         trailer,
                           final boolean               terminate)
        throws DatabaseException
    {
        try {
            if ((packetData != null) && (packetData.length > ILDIStore.MAX_PACKET)) {
                throw new IllegalArgumentException("Input " + name + " length too large: " + packetData.length
                        + " exceeds " + ILDIStore.MAX_PACKET);
            }

            if ((key == null) || key.isUnsupported()) {
                throw new IllegalArgumentException("Database packet id cannot " + "be null or unsupported");
            }

            bb.clear();

            bb.insert(contextConfig.getContextId().getNumber().longValue());
            bb.insertSeparator();

            bb.insert(contextConfig.getContextId().getHostId());
            bb.insertSeparator();

            bb.insert(contextConfig.getContextId().getFragment());
            bb.insertSeparator();

            key.insert(bb);
            bb.insertSeparator();

            final IAccurateDateTime rct = new AccurateDateTime();

            try {
                bb.insertDateAsCoarseFineSeparate(rct);
            }
            catch (final TimeTooLargeException ttle) {
                trace.warn(Markers.DB, dateExceedsWarning(name + ".rct", null, rct));
            }

            final IAccurateDateTime scet = pi.getScet();
            final IAccurateDateTime ert = pi.getErt();

            /** MPCS-8384 Modify for extended */
            try {
                if (_extendedDatabase) {
                    bb.insertScetAsCoarseFineSeparate(scet);
                }
                else {
                    bb.insertScetAsCoarseFineSeparateShort(scet);
                }
            }
            catch (final TimeTooLargeException ttle) {
                trace.warn(Markers.DB, scetExceedsWarning(name + ".scet", null, scet, ert));
            }

            try {
                bb.insertErtAsCoarseFineSeparate(ert);
            }
            catch (final TimeTooLargeException ttle) {
                trace.warn(Markers.DB, ertExceedsWarning(name + ".ert", null, ert));
            }

            final ISclk sclk = pi.getSclk();

            bb.insertSclkAsCoarseFineSeparate(sclk);

            bb.insert(pi.getApid());
            bb.insertSeparator();

            final String apidName = StringUtil.emptyAsNull(pi.getApidName());

            if ((apidName != null) && !apidName.equalsIgnoreCase("Unknown")) {
                /** MPCS-5153  */
                bb.insertTextComplainReplace(checkLength(name + ".apidName", APID_NAME_LENGTH, apidName));
            }
            else {
                bb.insertNULL();
            }

            bb.insertSeparator();

            final int seqCount = pi.getSeqCount();

            if (seqCount < 0) {
                throw new IllegalArgumentException("Negative input packet sequence counter: " + seqCount);
            }

            bb.insert(seqCount);
            bb.insertSeparator();

            bb.insertNULL(); // badReason
            bb.insertSeparator();

            bb.insert((packetData != null) ? packetData.length : 0);
            bb.insertSeparator();

            // headerLength: can be zero length

            header.insertLength(bb);
            bb.insertSeparator();

            // trailerLength: can be zero length

            trailer.insertLength(bb);

            if (terminate)
            {
                bb.insertTerminator();
            }
            else
            {
                bb.insertSeparator();
            }
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


    /**
     * Format FSW portion of Packet for LDI.
     *
     * @param dbProperties
     *            the currently active Database Properties object
     * @param bb
     *            BytesBuilder to append to
     * @param pi
     *            Packet info object
     * @param frameId
     *            Parent frame
     * @param dssId
     *            DSS
     * @param sessionDss
     *            Session DSS
     * @param sessionVcid
     *            Session VCID
     *
     * @throws DatabaseException
     *             On insertion problem
     *
     * @version MPCS-5932 Use frame id holder
     */
    protected void formatPacketFsw(final IMySqlAdaptationProperties dbProperties,
    		                            final BytesBuilder  bb,
                                        final ITelemetryPacketInfo   pi,
                                        final FrameIdHolder frameId,
                                        final int           dssId,
                                        final int           sessionDss,
                                        final Integer       sessionVcid)
        throws DatabaseException
    {
        try {
            /** MPCS-7681  Avoid dead link to Frame */
            if (archiveFrames) {
                /** May be inserted as NULL */
                frameId.insert(bb);
            }
            else {
                bb.insertNULL();
            }
            bb.insertSeparator();

            /*
             * BEGIN MPCS-4839
             * For historical reasons we allow negative values here and fix them.
             * Otherwise must be within bounds.
             */
            final int mdss = Math.max(dssId, StationIdHolder.MIN_VALUE);
            /*
             * END MPCS-4839
             */

            if (!isValidDssId(mdss, sessionDss)) {
                trace.warn(Markers.DB, "Packet DSS " , mdss , " does not match Session DSS " , sessionDss);
            }

            bb.insert(mdss);
            bb.insertSeparator();

            final Integer vcid = pi.getVcid();
            final Integer mvcid = ((vcid != null) && (vcid >= 0)) ? vcid : null;

            if (sessionVcid != null)
            {
                if (mvcid != null) {
                    if (mvcid.intValue() != sessionVcid.intValue()) {
                        trace.warn(Markers.DB, "Packet VCID " , mvcid , " does not match Session VCID " , sessionVcid);
                    }
                }
                else
                {
                    trace.warn(Markers.DB, "Packet VCID null does not match " , "Session VCID " , sessionVcid);
                }
            }

            if (mvcid != null) {
                bb.insert(mvcid);
            }
            else
            {
                bb.insertNULL();
            }

            bb.insertSeparator();

            final List<Long> sourceVcfcs = pi.getSourceVcfcs();

            if ((sourceVcfcs == null) || sourceVcfcs.isEmpty())
            {
                bb.insertNULL();
            }
            else
            {
                final Long svcfcs = sourceVcfcs.get(0);

                if (svcfcs != null) {
                    /** MPCS-6809 Treat as unsigned */
                    bb.insertLongAsUnsigned(svcfcs);
                }
                else {
                    bb.insertNULL();
                }
            }

            bb.insertSeparator();

            bb.insert(pi.isFill() ? 1 : 0);
            bb.insertTerminator();
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


    /**
     * Format a PacketBody/SsePacketBody for LDI.
     *
     * @param contextConfig         Test configuration
     * @param bb         BytesBuilder to populate
     * @param packetData The raw binary packet
     * @param key        Packet key counter
     * @param header     Header holder
     * @param trailer    Trailer holder
     *
     * @throws DatabaseException SQL exception
     */
    public void formatPacketBody(final IContextConfiguration contextConfig,
                                        final BytesBuilder          bb,
                                        final byte[]                packetData,
                                        final PacketIdHolder        key,
                                        final HeaderHolder          header,
                                        final TrailerHolder         trailer)
        throws DatabaseException
    {
        try {
            if ((key == null) || key.isUnsupported()) {
                throw new IllegalArgumentException("Database packet id cannot " + "be null or unsupported");
            }

            bb.clear();

            bb.insert(contextConfig.getContextId().getNumber().longValue());
            bb.insertSeparator();

            bb.insert(contextConfig.getContextId().getHostId());
            bb.insertSeparator();

            bb.insert(contextConfig.getContextId().getFragment());
            bb.insertSeparator();

            bb.insertBlob((packetData != null) ? packetData : new byte[0]);
            bb.insertSeparator();

            // header: do not store zero length

            header.insert(bb);
            bb.insertSeparator();

            // trailer: do not store zero length

            trailer.insert(bb);
            bb.insertSeparator();

            key.insert(bb);
            bb.insertTerminator();
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ldi.IPacketLDIStore#insertPacket(jpl.gds.tm.service.api.packet.ITelemetryPacketInfo, byte[], jpl.gds.shared.holders.FrameIdHolder, int, jpl.gds.shared.holders.PacketIdHolder, jpl.gds.shared.holders.HeaderHolder, jpl.gds.shared.holders.TrailerHolder)
     */
    @Override
    public void insertPacket(final ITelemetryPacketInfo    pi,
                             final byte[]         packetData,
                             final FrameIdHolder  frameId,
                             final int            dssId,
                             final PacketIdHolder packetId,
                             final HeaderHolder   hdr,
                             final TrailerHolder  tr)
        throws DatabaseException
    {
        if (!dbProperties.getUseDatabase() || isStoreStopped.get())
        {
            return;
        }

        final HeaderHolder  header  = HeaderHolder.getSafeHolder(hdr);
        final TrailerHolder trailer = TrailerHolder.getSafeHolder(tr);

        try
        {
            synchronized (this)
            {
                if (!archiveController.isUp())
                {
                    throw new IllegalStateException(
                                  "This connection has already been closed");
                }

                // Format common portion and do not terminate
                formatPacketCommon(contextConfig,
                                   _bb,
                                   pi,
                                   packetData,
                                   IDbTableNames.DB_PACKET_DATA_TABLE_NAME,
                                   packetId,
                                   header,
                                   trailer,
                                   false);

                // Add the FSW portion
                formatPacketFsw(dbProperties, 
                		        _bb,
                                pi,
                                frameId,
                                dssId,
                                _sessionDss == null ? StationIdHolder.MIN_VALUE : _sessionDss,
                                _sessionVcid);

                // Do the body
                formatPacketBody(contextConfig,
                                 _bbBody,
                                 packetData,
                                 packetId,
                                 header,
                                 trailer);

                // Add the line to the LDI batch
                writeToStream(_bb, _bbBody);
            }
        }
        catch (final RuntimeException re)
        {
            throw re;
        }
        catch (final Exception e)
        {
            throw new DatabaseException(
                          "Error inserting Packet record into database: " + e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startResource() {
        super.startResource();

        handler = new BaseMessageHandler() {
            @Override
            public synchronized void handleMessage(final IMessage m) {
                handlePacketMessage((ITelemetryPacketMessage) m);
            }
        };

        // Subscribe to packet messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.subscribe(TmServiceMessageType.TelemetryPacket, handler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void stopResource() {
        super.stopResource();

        // Unsubscribe from packet messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.unsubscribe(TmServiceMessageType.TelemetryPacket, handler);
        }
    }

    /**
     * Consume a packet message from the internal message bus. If the packet
     * is an idle packet, then throw it away, otherwise insert it and its
     * body into the database
     *
     * @param pm The packet message to consume
     */
    protected void handlePacketMessage(final ITelemetryPacketMessage pm)
    {
        if (pm == null)
        {
            throw new IllegalArgumentException("Null input packet message");
        }

        final ITelemetryPacketInfo pi = pm.getPacketInfo();

        if (pi.isFromSse())
        {
            // Not for us
            return;
        }

        // Do not store idle/fill packets unless authorized

        if (pi.isFill() && ! _storeIdle)
        {
            // Ignore it
            return;
        }

        /* MPCS-7289 -  Removed dependency upon DSNInfo object.
         * Go through IPacketInfo for everything.
         */

        /*
         * BEGIN MPCS-4839
         * For historical reasons we allow negative values here and fix them.
         * Otherwise must be within bounds.
         */

        final int dssId = ((pi != null)
                         ? Math.max(pi.getDssId(),
                                    StationIdHolder.MIN_VALUE)
                         : StationIdHolder.MIN_VALUE);
        /*
         * END MPCS-4839
         */

        try
        {
            /** MPCS-5932  Get frame id */

            insertPacket(pi,
                         pm.getPacket(),
                         pm.getFrameId(),
                         dssId,
                         pm.getPacketId(),
                         pm.getHeader(),
                         pm.getTrailer());
        }
        catch (final DatabaseException de)
        {
            trace.error("LDI Packet store failed: ", de);
        }
    }
}
