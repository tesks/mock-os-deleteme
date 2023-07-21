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

import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.ldi.IEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseEvrLDIStore;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.shared.database.BytesBuilder;


/**
 * This is the database write/storage interface to the SseEVR tables in the
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
public class SseEvrLDIStore extends EvrLDIStore implements ISseEvrLDIStore
{
    /** Table fields as CSV */
    public final String FIELDS = IEvrLDIStore.FIELDS_COMMON;
    
    /** Metadata fields as CSV */
    public final String METADATA_FIELDS = IEvrLDIStore.DB_EVR_METADATA_FIELDS;

    private final BytesBuilder _bb = new BytesBuilder();

    // A counter to form the unique ID of each inserted SSE EVR
    private long sseEvrKeyCounter = IEvrLDIStore.ID_START;

    /**
     * Creates an instance of EvrLDIStore
     *
     * @param appContext
     *            The test configuration for the current test session
     */
    public SseEvrLDIStore(final ApplicationContext appContext)
    {
    	/* 
    	 * MPCS-7135 - Add second argument to indicate this
    	 * store can operate asynchronously.
    	 */
        super(appContext, ISseEvrLDIStore.STORE_IDENTIFIER, true);
    }

    
    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ldi.ISseEvrLDIStore#insertSseEvr(jpl.gds.evr.IEvr)
     */
    @Override
    public void insertSseEvr(final IEvr evr)
        throws DatabaseException
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

                // Do the common portion and terminate
                formatEvrCommon(dbProperties, 
                                            contextConfig,
                                            _bb,
                                            evr,
                                            ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME,
                                            sseEvrKeyCounter,
                                            true);

                // Do the metadata
                final List<BytesBuilder> metas =
                    formatEvrMetadata(contextConfig,
                                                  evr,
                                                  ISseEvrLDIStore.DB_SSE_EVR_METADATA_TABLE_NAME,
                                                  sseEvrKeyCounter);

                // Add the lines to the LDI batch

                writeToStream(_bb, metas.toArray(new BytesBuilder[metas.size()]));

                // Increment the unique SSE EVR id counter
                ++sseEvrKeyCounter;
            }
        }
        catch (final RuntimeException re)
        {
            throw re;
        }
        catch (final Exception e)
        {
            throw new DatabaseException(
                    "Error storing SseEvr records in database: ", e);
        }
    }

    /**
     * Consume an EVR message from the internal message bus and insert
     * it and its metadata into the database.
     *
     * @param em The EVR message to consume
     */
    @Override
    protected void handleEvrMessage(final IEvrMessage em)
    {
        final IEvr evr = em.getEvr();

        if (! evr.isFromSse())
        {
            // Not for us
            return;
        }

        /*
         * MPCS-7135 -  Modified logic below to queue to
         * serialization queue if async, otherwise make the serialize call
         * (which calls insertSseEvr()) directly.
         */
        try {
        	if (this.doAsyncSerialization) {
        		queueForSerialization(evr);
        	} else {
        		serializeToLDIFile(evr);
        	}
        } catch (final Exception anye) {
            trace.error("LDI SseEvr store failed: ", anye);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean serializeToLDIFile(final Object toInsert) {
    	final IEvr evrObject = (IEvr) toInsert;

    	try {
    		insertSseEvr(evrObject);
    		return true;
        }
        catch (final DatabaseException de) {
            trace.error("LDI Sse EVR store failed for EVR " , evrObject.getEventId() , " with message "
                    , evrObject.getMessage() ,
                    ": ", de);
    		return false;
    	}
    }
}
