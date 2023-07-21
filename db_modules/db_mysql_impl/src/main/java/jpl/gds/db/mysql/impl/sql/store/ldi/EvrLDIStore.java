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
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IEvrLDIStore;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.evr.api.EvrMetadata;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessage;
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
 * This is the database write/storage interface to the EVR tables in the
 * MPCS database. EVRs are different than other MPCS database-stored objects
 * because they are spread across two tables in the database. There is a base
 * EVR table that stores all the EVR-specific information such as event ID and
 * then there is an EVR metadata table responsible for storing all the EVR
 * keyword/value metadata pairs. This class will receive an input EVR and write
 * it and all of its metadata to the EVR and EVR metadata tables in the
 * database. Insert is done via LDI.
 *
 * This class is very similar to the equivalent non-LDI class. The fields are
 * dumped into a BytesBuilder instead of a PreparedStatement, and written by
 * means of a table-specific call to the superclass.
 *
 * startResource and stopResource also make table-specific calls to enable or
 * disable processing.
 *
 */
public class EvrLDIStore extends AbstractLDIStore implements IEvrLDIStore
{
    private final BytesBuilder _bb = new BytesBuilder();
    private final Integer      _sessionDss;  // Zero means not set
    private final Integer      _sessionVcid; // null means not set

    // A counter to form the unique ID of each inserted EVR.
    // We don't bother to check for exceeding a maximum, because the
    // range is so wide.
    private long evrKeyCounter = ID_START;
    private final boolean archivePackets;


    /**
     * Creates an instance of EvrLDIStore
     *
     * MPCS-4839 Do not allow negative values here. Use constants.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public EvrLDIStore(final ApplicationContext appContext) {
    	this(appContext, true);
	}

    /**
     * Creates an instance of EvrLDIStore
     *
     * MPCS-4839 Do not allow negative values here. Use constants.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param supportsAsync
     *            true if this LDI store should support ASYNC, false if not.
     */
    public EvrLDIStore(final ApplicationContext appContext, final boolean supportsAsync)
    {
    	/* 
    	 * MPCS-7135 - Add second argument to indicate this
    	 * store can operate asynchronously.
    	 */
    	this(appContext, IEvrLDIStore.STORE_IDENTIFIER, supportsAsync);
    }

    /**
     * MPCS-8475 - : Added to support subclass initialization.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param si
     *            the Store Identifier of this store
     * @param supportsAsync
     *            true if this LDI store should support ASYNC, false if not.
     */
    protected EvrLDIStore(final ApplicationContext appContext, final StoreIdentifier si, final boolean supportsAsync)
    {
        /* 
         * MPCS-7135 -  Add second argument to indicate this
         * store can operate asynchronously.
         */
        super(appContext, si, supportsAsync);

        _sessionDss  = appContext.getBean(IContextFilterInformation.class).getDssId();
        _sessionVcid = appContext.getBean(IContextFilterInformation.class).getVcid();
        
        archivePackets = archiveController.getUseArchive(StoreIdentifier.Packet);

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
     * Format common portion of Evr/SseEvr for LDI.
     * 
     * @param dbProperties
     *            the currently active database properties object
     * @param tc
     *            Test configuration
     * @param bb
     *            BytesBuilder to populate
     * @param evr
     *            EVR object
     * @param name
     *            Table name
     * @param key
     *            Evr key
     * @param terminate
     *            If true, terminate
     *
     * @throws DatabaseException
     *             SQL exception
     */
    public void formatEvrCommon(final IMySqlAdaptationProperties dbProperties,
    		                    final IContextConfiguration tc,
                                final BytesBuilder          bb,
                                final IEvr                  evr,
                                final String                name,
                                final long                  key,
                                final boolean               terminate)
        throws DatabaseException
    {
        try {
            bb.clear();
    
            bb.insert(tc.getContextId().getNumber().longValue());
            bb.insertSeparator();
    
            bb.insert(tc.getContextId().getHostId());
            bb.insertSeparator();
    
            bb.insert(tc.getContextId().getFragment());
            bb.insertSeparator();
    
            /** MPCS-6809  Store as usigned long */
            bb.insertLongAsUnsigned(key);
            bb.insertSeparator();
    
            /** MPCS-6809 Store as usigned long */
            /** MPCS-7681 Avoid dead link to Packet */
            /** MPCS-5935  Use holder */
            if (archivePackets)
            {
                evr.getPacketId().insert(bb);
            }
            else
            {
                bb.insertNULL();
            }
    
            bb.insertSeparator();
    
            /** MPCS-5153  New */
            bb.insertTextOrNullComplainReplace(
                checkLength(name + ".name",
                            NAME_LENGTH,
                            StringUtil.emptyAsNull(evr.getName())));
    
            bb.insertSeparator();
    
            bb.insert(evr.getEventId());
            bb.insertSeparator();
    
            final IAccurateDateTime ert = evr.getErt();
    
            try
            {
               bb.insertErtAsCoarseFineSeparate(ert);
            }
            catch (final TimeTooLargeException ttle)
            {
                trace.warn(Markers.DB, ertExceedsWarning(name + ".ert", null, ert));
            }
    
            final IAccurateDateTime scet = evr.getScet();
    
            /** MPCS-8384  Modify for extended */
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
                                              null,
                                              scet,
                                              evr.getErt()));
            }
    
            final IAccurateDateTime rct = new AccurateDateTime();
    
            try
            {
                bb.insertDateAsCoarseFineSeparate(rct);
            }
            catch (final TimeTooLargeException ttle)
            {
                trace.warn(Markers.DB, dateExceedsWarning(name + ".rct", null, rct));
            }
    
            final ISclk sclk = evr.getSclk();
    
            bb.insertSclkAsCoarseFineSeparate(sclk);
    
            /** MPCS-5153  */
            bb.insertTextComplainReplace(
                checkLength(name + ".level",
                            LEVEL_LENGTH,
                            StringUtil.safeTrim(evr.getLevel())));
    
            bb.insertSeparator();
    
            final String module = checkLength(
                                      name + ".module",
                                      MODULE_LENGTH,
                                      StringUtil.emptyAsNull(evr.getCategory(IEvrDefinition.MODULE))); /* MHT - MPCS-7033 - 11/4/15 */
            if (module != null)
            {
                /** MPCS-5153 */
                bb.insertTextComplainReplace(module.toUpperCase());
            }
            else
            {
                bb.insertNULL();
            }
    
            bb.insertSeparator();
    
            /** MPCS-5153  */
            bb.insertTextAllowReplace(
                checkLength(name + ".message",
                            MESSAGE_LENGTH,
                            StringUtil.safeTrim(evr.getMessage())));
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
     * Format FSW portion of Evr for LDI.
     *
     * @param bb          BytesBuilder to append to
     * @param evr         EVR object
     * @param sessionDss  Session DSS
     * @param sessionVcid Session VCID
     *
     * @throws DatabaseException SQL exception
     */
    protected void formatEvrFsw(final BytesBuilder bb,
                                     final IEvr         evr,
                                     final int          sessionDss,
                                     final Integer      sessionVcid)
        throws DatabaseException
    {
        /*
         * BEGIN MPCS-4839
         * For historical reasons we allow negative values here and fix them.
         * Otherwise must be within bounds.
         */

        final int mdss = Math.max(evr.getDssId(),
                            StationIdHolder.MIN_VALUE);


        /*
         * END MPCS-4839
         */

        if (! isValidDssId(mdss, sessionDss))
        {
            trace.warn(Markers.DB, "Evr DSS " , mdss , " does not match Session DSS " , sessionDss);
        }

        bb.insert(mdss);
        bb.insertSeparator();

        final Integer vcid  = evr.getVcid();
        final Integer mvcid = ((vcid != null) && (vcid >= 0))
                                  ? vcid
                                  : null;

        if (sessionVcid != null)
        {
            if (mvcid != null)
            {
                if (mvcid.intValue() != sessionVcid.intValue())
                {
                    trace.warn(Markers.DB, "Evr VCID " , mvcid , " does not match Session VCID " , sessionVcid);
                }
            }
            else
            {
                trace.warn(Markers.DB, "Evr VCID null does not match Session VCID " , sessionVcid);
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

        bb.insertSeparator();

        bb.insert(evr.isRealtime() ? 1 : 0);
        bb.insertTerminator();
    }


    /**
     * Format a EvrMetadata/SseEvrMetadata for LDI.
     *
     * @param contextConfig   Test configuration
     * @param evr  EVR object
     * @param name Table name
     * @param key  Evr key counter
     *
     * @return List of populated BytesBuilder
     *
     * @throws DatabaseException SQL exception
     */
    public List<BytesBuilder> formatEvrMetadata(
                                         final IContextConfiguration contextConfig,
                                         final IEvr                  evr,
                                         final String                name,
                                         final long                  key)
        throws DatabaseException
    {
        try {
            final EvrMetadata metadata = evr.getMetadata();

            if (metadata == null) {
                throw new IllegalArgumentException("Null input metadata");
            }

            final List<BytesBuilder> bbs = new ArrayList<BytesBuilder>(metadata.size());

            final String evrValue = name + ".value";

            // Format all the metadata to the EVR metadata SQL

            for (final Pair<EvrMetadataKeywordEnum, String> p : metadata.asStrings()) {
                final BytesBuilder bb_meta = new BytesBuilder();

                bbs.add(bb_meta);

                bb_meta.insert(contextConfig.getContextId().getNumber().longValue());
                bb_meta.insertSeparator();

                bb_meta.insert(contextConfig.getContextId().getHostId());
                bb_meta.insertSeparator();

                bb_meta.insert(contextConfig.getContextId().getFragment());
                bb_meta.insertSeparator();

                /** MPCS-6809 Store as unsigned long */
                bb_meta.insertLongAsUnsigned(key);
                bb_meta.insertSeparator();

                /** MPCS-5153 */
                bb_meta.insertTextComplainReplace(p.getOne().toString());

                bb_meta.insertSeparator();

                /** MPCS-5153  */
                bb_meta.insertTextAllowReplace(checkLength(evrValue, VALUE_LENGTH, StringUtil.safeTrim(p.getTwo())));

                bb_meta.insertTerminator();
            }

            return bbs;
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ldi.IEvrLDIStore#insertEvr(jpl.gds.evr.IEvr)
     */
    @Override
    public void insertEvr(final IEvr evr) throws DatabaseException
    {
        if (!dbProperties.getUseDatabase() || isStoreStopped.get())
        {
            return;
        }

        try
        {
            synchronized(this)
            {
                if (!archiveController.isUp())
                {
                    throw new IllegalStateException(
                                  "This connection has already been closed");
                }

                // Do the common portion and do not terminate
                formatEvrCommon(dbProperties, 
                                contextConfig,
                                _bb,
                                evr,
                                DB_EVR_DATA_TABLE_NAME,
                                evrKeyCounter,
                                false);

                // Add the FSW portion
                formatEvrFsw(_bb,
                             evr,
                             null == _sessionDss ? StationIdHolder.MIN_VALUE : _sessionDss,
                             _sessionVcid);

                // Do the metadata
                final List<BytesBuilder> metas =
                    formatEvrMetadata(contextConfig,
                                      evr,
                                      DB_EVR_METADATA_TABLE_NAME,
                                      evrKeyCounter);

                // Add the lines to the LDI batch

                writeToStream(_bb, metas.toArray(new BytesBuilder[metas.size()]));

                // Increment the unique EVR id counter
                ++evrKeyCounter;
            }
        }
        catch (final RuntimeException re)
        {
            throw re;
        }
        catch (final Exception e)
        {
            throw new DatabaseException("Error storing Evr records in database: " +
                                   e);
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
                handleEvrMessage((IEvrMessage) m);
            }
        };

        // Subscribe to EVR messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.subscribe(EvrMessageType.Evr, handler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void stopResource() {
        super.stopResource();

        // Unsubscribe from EVR messages on the internal bus
        if (messagePublicationBus != null) {
            messagePublicationBus.unsubscribe(EvrMessageType.Evr, handler);
        }
    }

    /**
     * Consume an EVR message from the internal message bus and insert
     * it and its metadata into the database.
     *
     * @param em The EVR message to consume
     */
    protected void handleEvrMessage(final IEvrMessage em)
    {
        final IEvr evr = em.getEvr();

        if (evr.isFromSse())
        {
            // Not for us
            return;
        }

        /*
         * MPCS-7135 - Modified logic below to queue to
         * serialization queue if async, otherwise make the serialize call
         * (which calls insertEvr()) directly.
         */
        try {
        	if (this.doAsyncSerialization) {
        		queueForSerialization(evr);
        	} else {
        		serializeToLDIFile(evr);
        	}
        } catch (final Exception anye) {
            trace.error("LDI Evr store failed: ", anye);
        }
    
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean serializeToLDIFile(final Object toInsert) {
    	final IEvr evrObject = (IEvr) toInsert;

    	try {
    		insertEvr(evrObject);
    		return true;
        }
        catch (final DatabaseException de) {
            trace.error("LDI EVR store failed for EVR " , evrObject.getEventId() , " with message "
                    , evrObject.getMessage() , ": ", de);
    		return false;
    	}
    }
}
