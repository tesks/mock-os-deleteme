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

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILDIStore;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.TimeTooLargeException;
import jpl.gds.shared.types.Pair;

/**
 * This is the database write/storage interface to the ChannelValue table in
 * the MPCS database. This class will receive an input channel value and write
 * it to the ChannelValue table in the database. This is done via LDI.
 *
 * This class is very similar to the equivalent non-LDI class. The fields are
 * dumped into a BytesBuilder instead of a PreparedStatement, and written by
 * means of a table-specific call to the superclass.
 *
 * startResource and stopResource also make table-specific calls to enable or
 * disable processing.
 *
 * See also MonitorChannelValueLDIStore, HeaderChannelValueLDIStore, and
 * ISseChannelValueLDIStore.
 *
 * Important note:
 *
 * Depending upon the circumstances, the initial value of the id counter may be
 * set to something other than one.
 *
 * If we are inserting into a preexisting session, we will need to obtain first
 * the current value of the id so we can make fresh entries.
 *
 * If we are running at the same time as chill_down, we must use the top half
 * of the id range to avoid conflicts.
 *
 * Both can be true, in which case we make sure that we are in the top half AND
 * greater than the largest used id.
 *
 * For preexisting sessions, we also fetch the current set of mappings from
 * channel-id to id so we can use those.
 *
 * For text columns, I consider the value of an ASCII channel to be a "message"
 * type value that can contain out-of-bounds characters to be replaced. It is
 * an error for the other text columns to contain out-of-bound characters,
 * It's a judgement call for boolean and status.
 *
 */
public class ChannelValueLDIStore extends AbstractLDIStore implements IChannelValueLDIStore
{
    /** Maximum length of a string value */
    public final int             VALUE_LENGTH  = 255;

    /** String column lengths */
    private static final int     CID_LENGTH    = 9;
    private static final int     MODULE_LENGTH = 32;
    private static final int     NAME_LENGTH   = 64;
    private static final int     FORMAT_LENGTH = 16;

    /**
     * Byte Builder for metadata
     */
    protected final BytesBuilder bb            = new BytesBuilder();

    /**
     * Byte Builder for Channel Data
     */
    protected final BytesBuilder bbcd          = new BytesBuilder();
    
    /**
     * the DSSID for the session
     */
	protected final Integer      sessionDss;  // Zero means not set

    /**
     * the VCID for the session
     */
	protected final Integer      sessionVcid; // null means not set
	
	protected boolean archivePackets;


	/**
     * Creates an instance of IChannelValueLDIStore.
     *
     * MPCS-4839  Do not allow negative values here. Use constants.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public ChannelValueLDIStore(final ApplicationContext appContext)
	{
		/* 
		 * MPCS-7135 -  Add second argument to indicate this
		 * store can operate asynchronously.
		 */
		this(appContext, IChannelValueLDIStore.STORE_IDENTIFIER);
	}

    /**
     * MPCS-8475 : Added to support subclass initialization.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param si
     *            the Store Identifier for this store
     */
    protected ChannelValueLDIStore(final ApplicationContext appContext, final StoreIdentifier si) {
        /* 
         * MPCS-7135 -  Add second argument to indicate this
         * store can operate asynchronously.
         */
        super(appContext, si, true);
        sessionDss  = appContext.getBean(IContextFilterInformation.class).getDssId();
        sessionVcid =  appContext.getBean(IContextFilterInformation.class).getVcid();
        
        archivePackets = archiveController.getUseArchive(StoreIdentifier.Packet);
     }

    /**
     * Prepare a ChannelData for insertion into the database.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param contextConfig
     *            Session configuration
     * @param channelId
     *            Channel id
     * @param fromSse
     *            From SSE (true) or FSW (false)
     * @param type
     *            Channel type
     * @param cd
     *            Channel definition
     * @param id
     *            Index to match with *ChannelValue.
     * @param bb
     *            Where to put it all.
     *
     * @throws DatabaseException
     *             SQL error
     */
    public void prepareChannelData(
    		final ApplicationContext    appContext,
            final IContextConfiguration contextConfig,
            final String                channelId,
            final boolean               fromSse,
            final ChannelType           type,
            final IChannelDefinition    cd,
            final long                  id,
            final BytesBuilder          bb)
                    throws DatabaseException
    {
        try {
            bb.clear();

            bb.insert(contextConfig.getContextId().getNumber().longValue());
            bb.insertSeparator();

            bb.insert(contextConfig.getContextId().getHostId());
            bb.insertSeparator();

            bb.insert(contextConfig.getContextId().getFragment());
            bb.insertSeparator();

            bb.insert(id);
            bb.insertSeparator();

            /** MPCS-5153 */
            bb.insertTextComplainReplace(checkLength("ChannelData.channelId", CID_LENGTH, channelId.toUpperCase()));

            bb.insertSeparator();

            bb.insert(fromSse ? 1 : 0);
            bb.insertSeparator();

            /** MPCS-5153 */
            bb.insertTextComplainReplace(type.toString());
            bb.insertSeparator();

            final int index = cd.getIndex();

            if (index > 0) {
                bb.insert(index);
            }
            else {
                bb.insertNULL();
            }

            bb.insertSeparator();

            final String module = checkLength("ChannelData.module", MODULE_LENGTH,
                                              StringUtil.emptyAsNull(cd.getCategory(IChannelDefinition.MODULE))); /*
                                                                                                                   * MHT
                                                                                                                   * -
                                                                                                                   * MPCS
                                                                                                                   * -
                                                                                                                   * 7033
                                                                                                                   * -
                                                                                                                   * 11/
                                                                                                                   * 4/
                                                                                                                   * 15
                                                                                                                   */
            if (module != null) {
                /** MPCS-515 */
                bb.insertTextComplainReplace(module.toUpperCase());
            }
            else {
                bb.insertNULL();
            }

            bb.insertSeparator();

            /** MPCS-6624 Prefer name to title */
            /**
             * MPCS-6624 , Reverse previous decision. Prefer title to name
             * but leave the new null check.
             */
            String name = StringUtil.emptyAsNull(cd.getTitle());
            if (name == null) {
                name = StringUtil.emptyAsNull(cd.getName());
            }

            /** MPCS-5153 */
            bb.insertTextOrNullComplainReplace(checkLength("ChannelData.name", NAME_LENGTH, name));

            bb.insertSeparator();

            /** MPCS-5153  */
            bb.insertTextOrNullComplainReplace(checkLength("ChannelData.dnFormat", FORMAT_LENGTH, cd.getDnFormat()));

            bb.insertSeparator();

            /** MPCS-5153  */
            bb.insertTextOrNullComplainReplace(checkLength("ChannelData.euFormat", FORMAT_LENGTH, cd.getEuFormat()));

            bb.insertTerminator();
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


	/**
     * Format common portion of *ChannelValue for LDI. dnStringValue will be
     * trimmed before checking or inserting; also, null goes in as the empty
     * string, which is what we want.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param contextConfig
     *            Test configuration
     * @param bb
     *            BytesBuilder to populate
     * @param val
     *            The channel value to insert
     * @param name
     *            Table name
     * @param key
     *            Channel value key
     *
     * @throws DatabaseException
     *             Throws exception on error
     */
	public void formatChannelValueCommon(
			final ApplicationContext    appContext,
			final IContextConfiguration contextConfig,
			final BytesBuilder          bb,
			final IServiceChannelValue  val,
			final String                name,
			final long                  key)
					throws DatabaseException
					{
		if (val == null)
		{
			throw new IllegalArgumentException("Null input channel value");
		}

		final String ci = val.getChanId().toUpperCase();

		if (ci == null)
		{
			throw new IllegalArgumentException(
					"Input channel value has a null channel id");
		}

		final IChannelDefinition cd = val.getChannelDefinition();

		if (cd == null)
		{
			throw new IllegalArgumentException(
					"Input channel value has a null channel definition");
		}

		final ChannelType ct = val.getChannelType();

		if (ct == null)
		{
			throw new IllegalArgumentException(
					"Input channel value has a null channel type in " +
					"its channel definition");
		}

		bb.clear();

		bb.insert(contextConfig.getContextId().getNumber().longValue());
		bb.insertSeparator();

		bb.insert(contextConfig.getContextId().getHostId());
		bb.insertSeparator();

		bb.insert(contextConfig.getContextId().getFragment());
		bb.insertSeparator();

		bb.insert(key);
		bb.insertSeparator();

        try {
            /** MPCS-5153 */
            bb.insertTextComplainReplace(ci);

            bb.insertSeparator();

            final IAccurateDateTime rct = new AccurateDateTime();

            try {
                bb.insertDateAsCoarseFineSeparate(rct);
            }
            catch (final TimeTooLargeException ttle) {
                trace.warn(Markers.DB, dateExceedsWarning(name + ".rct", ci, rct));
            }

            /* MPCS-6276 -  Use hasEu() on channel value rather than on definition. */
            final Double eu = (val.hasEu() ? val.getEu() : null);

            // Every channel value row in the database has four value
            // columns, but only one of these columns should be non-null ...
            // figure out what type of channel we have and take the
            // appropriate action. There's now also a dnDoubleFlag. which is
            // used to flag infinite and NaN values.
            //
            // Status and boolean also set string value

            /* MPCs-6115 - Added TIME case below. */
            switch (ct) {
                case SIGNED_INT:
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insert(val.longValue());
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();

                    break;

                case UNSIGNED_INT:
                case DIGITAL:
                case TIME:
                    /** MPCS-5466 */
                    /** MPCS-6809 */

                    bb.insertLongAsUnsigned(val.longValue());
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();

                    break;

                case BOOLEAN:
                    /** MPCS-5466 */

                    final long actual = val.longValue();
                    final long used = ((actual != 0L) ? 1L : 0L);

                    if ((actual != 0L) && (actual != 1L)) {
                        trace.warn(Markers.DB, ct , " channel " , ci , " received " , actual , ", forced to " , used);
                    }

                    bb.insert(used);
                    bb.insertSeparator();

                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();

                    /** MPCS-5153 */
                    bb.insertTextComplainReplace(val.getStatus());

                    bb.insertSeparator();

                    break;

                case STATUS:
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insert(val.longValue());
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();

                    /** MPCS-5153 */
                    bb.insertTextComplainReplace(val.getStatus());

                    bb.insertSeparator();

                    break;

                case FLOAT:
                    /* MPCS-6115 -  Removed check for DOUBLE type. */
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();

                    // Takes care of both dnDouble and dnDoubleFlag
                    storeSafeDouble(val.doubleValue(), bb);

                    bb.insertNULL();
                    bb.insertSeparator();

                    break;

                case ASCII:
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();
                    bb.insertNULL();
                    bb.insertSeparator();

                    /** MPCS-5153 */
                    bb.insertTextAllowReplace(checkLength(name + ".dnStringValue", VALUE_LENGTH, val.stringValue()));

                    bb.insertSeparator();

                    break;

                case UNKNOWN:
                default:
                    throw new DatabaseException("Unimplemented channel type '" + ct + "' found");
            }

            if (eu != null) {
                storeSafeDouble(eu, bb);
            }
            else {
                bb.insertNULL();
                bb.insertSeparator();

                bb.insertNULL();
                bb.insertSeparator();
            }

            bb.insertTextComplainReplace(val.getDnAlarmLevel().toString());
            bb.insertSeparator();

            bb.insertTextComplainReplace(val.getEuAlarmLevel().toString());
            bb.insertSeparator();
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }

    }

	/**
     * Format packet id portion of *ChannelValue for LDI.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param dbProperties
     *            the currently active Database Properites object
     * @param bb
     *            BytesBuilder to populate
     * @param val
     *            The channel value to insert
     * @param terminate
     *            Terminate if true
     *
     * @throws DatabaseException
     *             Throws exception on error
     */
    public void formatChannelValuePacketId(final ApplicationContext appContext,
                                           final IMySqlAdaptationProperties dbProperties, final BytesBuilder bb,
                                           final IServiceChannelValue val, final boolean terminate)
            throws DatabaseException {
        try {
            /** MPCS-7681 Avoid dead link to *Packet */
            if (archivePackets) {
                val.getPacketId().insert(bb);
            }
            else {
                bb.insertNULL();
            }

            if (terminate) {
                bb.insertTerminator();
            }
            else {
                bb.insertSeparator();
            }
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


    /**
     * Format ERT/MST portion of *ChannelValue for LDI.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param bb
     *            BytesBuilder to populate
     * @param val
     *            The channel value to insert
     * @param name
     *            Table name
     * @param type
     *            Time name
     *
     * @throws DatabaseException
     *             Throws exception on error
     */
    public void formatChannelValueErt(final ApplicationContext appContext, 
    	                                     final BytesBuilder  bb,
                                             final IClientChannelValue val,
                                             final String        name,
                                             final String        type)
        throws DatabaseException
    {
        final IAccurateDateTime ert = val.getErt();

        try
        {
            bb.insertErtAsCoarseFineSeparate(ert);
        }
        catch (final TimeTooLargeException ttle)
        {
            trace.warn(Markers.DB, ertExceedsWarning(name + "." + type,
                       val.getChanId().toUpperCase(),
                       ert));
        }
    }


    /**
     * Format SCLK and SCET portion of *ChannelValue for LDI.
     *
     * @param appContext
     *            the Spring Application Context
     * @param bb
     *            BytesBuilder to populate
     * @param val
     *            The channel value to insert
     * @param name
     *            Table name
     *
     * @throws DatabaseException
     *             Throws exception on error
     *
     * @version MPCS-8384
     */
    public void formatChannelValueSclk(final ApplicationContext appContext,
    		                           final BytesBuilder  bb,
                                       final IClientChannelValue val,
                                       final String        name)
        throws DatabaseException
    {
        final ISclk sclk = val.getSclk();

        bb.insertSclkAsCoarseFineSeparate(sclk);

        final IAccurateDateTime scet = val.getScet();

        try
        {
            if (_extendedDatabase)
            {
                bb.insertScetAsCoarseFineSeparate(scet);
            }
            else
            {
                bb.insertScetAsCoarseFineSeparateShort(scet);
            }
        }
        catch (final TimeTooLargeException ttle)
        {
            trace.warn(Markers.DB, scetExceedsWarning(name + ".scet",
                                          val.getChanId().toUpperCase(),
                                          scet,
                                          val.getErt()));
        }
    }


    /**
     * Format DSS id portion of *ChannelValue for LDI.
     *
     * The suppresses are to get rid of warnings for something we have to do
     * and is harmless.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param bb
     *            BytesBuilder to populate
     * @param val
     *            The channel value to insert
     * @param name
     *            Table name
     * @param sessionDss
     *            DSS from session
     * @param terminate
     *            Terminate if true
     *
     * @throws DatabaseException
     *             Throws exception on error
     */
    public void formatChannelValueDssId(
    		final ApplicationContext appContext,
    		final BytesBuilder  bb,
			final IServiceChannelValue val,
            final String        name,
            final int           sessionDss,
            final boolean       terminate)
                    throws DatabaseException
                    {
		// Necessary to get past JVM bug on mushroom
		@java.lang.SuppressWarnings("unused")
        final String nme = val.getClass().getName();

        /*
         * BEGIN MPCS-4839
         * For historical reasons we allow negative values here and fix them.
         * Otherwise must be within bounds.
         */

        final int dss = Math.max(val.getDssId(), StationIdHolder.MIN_VALUE);

//        if (! isStationInRange(dss))
//        {
//            logError(name,
//                    " ",
//                    val.getChanId().toUpperCase(),
//                    " DSS ",
//                    dss,
//                    " is outside of range [",
//                    SessionConfiguration.MINIMUM_DSSID,
//                    "-",
//                    SessionConfiguration.MAXIMUM_DSSID,
//                    "] and will be forced to ",
//                    SessionConfiguration.MINIMUM_DSSID);
//
//            dss = SessionConfiguration.MINIMUM_DSSID;
//        }

//        /*
//         * END MPCS-4839
//         */
//
        if (! isValidDssId(dss, sessionDss))
        {
            trace.warn(Markers.DB, name , " " , val.getChanId().toUpperCase() , " DSS " , dss
                    , " does not match Session DSS " , sessionDss);
        }

        bb.insert(dss);

        if (terminate)
        {
            bb.insertTerminator();
        }
        else
        {
            bb.insertSeparator();
        }
    }


    /**
     * Format VCID portion of *ChannelValue for LDI.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param bb
     *            BytesBuilder to populate
     * @param val
     *            The channel value to insert
     * @param name
     *            Table name
     * @param sessionVcid
     *            VCID from session
     * @param terminate
     *            Terminate if true
     *
     * @throws DatabaseException
     *             Throws exception on error
     */
    public void formatChannelValueVcid(final ApplicationContext appContext,
    		final BytesBuilder  bb,
			final IServiceChannelValue val,
            final String        name,
            final Integer       sessionVcid,
            final boolean       terminate)
                    throws DatabaseException
                    {
        final Integer vcid  = val.getVcid();
        final Integer mvcid = ((vcid != null) && (vcid >= 0)) ? vcid : null;

        if (sessionVcid != null)
        {
            if (mvcid != null)
            {
                if (mvcid.intValue() != sessionVcid.intValue())
                {
                    trace.warn(Markers.DB, name , " " , val.getChanId().toUpperCase() , " VCID " , mvcid
                            , " does not match Session VCID " ,
                            sessionVcid);
                }
            }
            else
            {
                trace.warn(Markers.DB, name , " " , val.getChanId().toUpperCase() , " VCID null does "
                        , "not match Session VCID " ,
                        sessionVcid);
            }
        }

        if (mvcid != null)
        {
            bb.insert(mvcid);
        }
        else
        {
            bb.insertNULL();
        }

        if (terminate)
        {
            bb.insertTerminator();
        }
        else
        {
            bb.insertSeparator();
        }
                    }


    /**
     * Format isRealtime portion of ChannelValue for LDI.
     * Always terminates.
     *
     * @param bb  BytesBuilder to populate
     * @param val The channel value to insert
     *
     * @throws DatabaseException Throws exception on error
     */
    protected void formatChannelValueIsRealtime(final BytesBuilder  bb,
			final IServiceChannelValue val)
                    throws DatabaseException
                    {
        bb.insert(val.isRealtime() ? 1 : 0);
        bb.insertTerminator();
                    }

    /**
     * Insert a double value, taking special care of NaN and infinity.
     *
     * Normally dnDouble is set to the DN double value and dnDoubleFlag is set
     * NULL.
     *
     * But if DN is a NaN or infinite, we set dnDouble to NULL and stash the
     * flag in dnDoubleFlag.
     *
     * Also used for EU. In both cases, the flag column comes right after
     * the regular column.
     *
     * @param dn
     *            DN value
     * @param bb
     *            Bytes builder to insert into
     *
     * @throws DatabaseException
     *             if a database error occurs
     */
    protected void storeSafeDouble(final double       dn,
            final BytesBuilder bb)
                    throws DatabaseException
                    {
        final boolean infiniteDn = Double.isInfinite(dn);
        final boolean nanDn      = Double.isNaN(dn);

        if (infiniteDn)
        {
            bb.insertNULL();
            bb.insertSeparator();

            bb.insertSafe((dn > 0.0D) ? ILDIStore.INFINITY : ILDIStore.NEGATIVE_INFINITY);
            bb.insertSeparator();
        }
        else if (nanDn)
        {
            bb.insertNULL();
            bb.insertSeparator();

            bb.insertSafe(NAN);
            bb.insertSeparator();
        }
        else
        {
            bb.insert(dn);
            bb.insertSeparator();

            bb.insertNULL();
            bb.insertSeparator();
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
                handleEhaChannelMessage((IAlarmedChannelValueMessage) m);
            }
        };

        // Subscribe to channel messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.subscribe(EhaMessageType.AlarmedEhaChannel, handler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void stopResource() {
        super.stopResource();

        // Unsubscribe from channel messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.unsubscribe(EhaMessageType.AlarmedEhaChannel, handler);
        }
        clearChannelIds();
    }
    
    /**
     * Remove all entries for which this store is responsible
     */
    protected void clearChannelIds() {
        archiveController.clearFswIds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean serializeToLDIFile(final Object toInsert) {
		final IServiceChannelValue chanObject = (IServiceChannelValue) toInsert;

        try {
            insertChannelValue(chanObject);
            return true;
        }
        catch (final DatabaseException de) {
            trace.error(Markers.DB, this.getClass().getSimpleName() , " failed for channel " , chanObject.getChanId()
                    , " with value " , chanObject.getDn() ,
                    ": ", de);
            return false;
        }
    }

    /**
     * @param cv
     *            the Service Channel Value to write
     */
    protected void writeChannelValueToLDIFile(final IServiceChannelValue cv) {
        /*
         * MPCS-7135 - Modified logic below to queue to
         * serialization queue if async, otherwise make the serialize call
         * (which calls insertChannelValue()) directly.
         */
        try {
            if (this.doAsyncSerialization) {
                queueForSerialization(cv);
            }
            else {
                serializeToLDIFile(cv);
            }
        }
        catch (final Exception anye) {
            trace.error(Markers.DB, "LDI ChannelValue storage failed for channel " , cv.getChanId().toUpperCase()
                    , " with value " , cv.getDn() , ": " ,
                    anye);
        }
    }

    /**
     * Receive an EHA Channel message from the internal message bus, pull out
     * the pertinent information, and insert the information into the database
     *
     * @param message The EHA channel message received on the internal bus
     */
    protected void handleEhaChannelMessage(final IAlarmedChannelValueMessage message)
    {
        /*
         * Filter all channels that are SSE channels (By Message)
         */
        if (message.isFromSse()) {
            return;
        }
        
        /*
         * Filter all channels that are not FSW channels both Definition and Category.
         * If either indicates that they are FSW channels, then they will be processed here.
         */
        final IServiceChannelValue cv = (IServiceChannelValue) message.getChannelValue();
        final boolean filterByDefType = ChannelDefinitionType.FSW != cv.getChannelDefinition().getDefinitionType();
        if (filterByDefType) {
            return;
        }
        writeChannelValueToLDIFile(cv);
    }

    /**
     * Insert a channel value into the database
     *
     * @param val The channel value to insert
     *
     * @throws DatabaseException Throws exception on error
     */
    @Override
    public void insertChannelValue(final IServiceChannelValue val) throws DatabaseException {
        if (val == null) {
            throw new IllegalArgumentException("Null input channel value");
        }
    
        if (!dbProperties.getUseDatabase() || isStoreStopped.get()) {
            return;
        }
        
        if (val.getChanId() == null) {
            throw new IllegalArgumentException("Input channel value has a null channel id");
        }
        final String ci = val.getChanId().toUpperCase();
    
        final ChannelCategoryEnum cce = val.getChannelCategory();
        if (cce == null) {
            throw new IllegalArgumentException("Null channel category");
        }
        
       /*
        * No need to check for SSE or HEADER 
        */
    
        final IChannelDefinition cd = val.getChannelDefinition();
        if (cd == null) {
            throw new IllegalArgumentException("Input channel value has a null channel definition");
        }
    
        final ChannelType ct = val.getChannelType();
        if (ct == null) {
            throw new IllegalArgumentException("Input channel value has a null channel type in its channel definition");
        }

        final boolean applicationIsSse = archiveController.isApplicationIsSse();
        final boolean channelFromSse = (cd.getDefinitionType() == ChannelDefinitionType.SSE);
        if (channelFromSse != applicationIsSse) {
            trace.warn(Markers.DB, "Channel \"" , ci , "\" is not consistent with application context: channelFromSse="
                    , channelFromSse , ", applicationIsSse="
                    , applicationIsSse);
            return;
        }
 
        final Pair<Long, Boolean> idPair         = archiveController.getAssociatedId(ci, channelFromSse);
        final long                id             = idPair.getOne();
        final boolean             doData         = idPair.getTwo();
        
        try {
            synchronized (this) {
                if (!archiveController.isUp()) {
                    throw new IllegalStateException("This connection has already been closed");
                }

                formatChannelValueCommon(appContext, contextConfig, bb, val, DB_CHANNEL_VALUE_TABLE_NAME, id);
                formatChannelValuePacketId(appContext, dbProperties, bb, val, false);
                formatChannelValueErt(appContext, bb, val, DB_CHANNEL_VALUE_TABLE_NAME, "ert");
                formatChannelValueSclk(appContext, bb, val, DB_CHANNEL_VALUE_TABLE_NAME);
                formatChannelValueDssId(appContext, bb, val, DB_CHANNEL_VALUE_TABLE_NAME, sessionDss == null ? StationIdHolder.MIN_VALUE : sessionDss, false);
                formatChannelValueVcid(appContext, bb, val, DB_CHANNEL_VALUE_TABLE_NAME, sessionVcid, false);
                formatChannelValueIsRealtime(bb, val);

                if (doData) {
                    prepareChannelData(appContext, 
                            contextConfig, 
                            ci, 
                            channelFromSse, // SSE
                            ct, 
                            cd, 
                            id, 
                            bbcd);

                    // Add the lines to the LDI batch

                    writeToStream(bb, bbcd);
                }
                else {
                    // Add the line to the LDI batch

                    writeToStream(bb);
                }
            }
        }
        catch (final RuntimeException re) {
            throw re;
        }
        catch (final Exception e) {
            throw new DatabaseException("Error inserting ChannelValue record into " + "database", e);
        }
    }
}
