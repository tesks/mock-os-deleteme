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
package jpl.gds.db.mysql.impl.sql.fetch;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.FrameWhereControl;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.IFrameFetch;
import jpl.gds.db.api.sql.fetch.IFrameQueryOptionsProvider;
import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.sql.fetch.PreFetchType;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.fetch.WhereControl.WhereControlException;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.api.sql.store.ldi.IFrameLDIStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbFrameFactory;
import jpl.gds.db.api.types.IDbFrameProvider;
import jpl.gds.db.api.types.IDbFrameUpdater;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.exception.DictionaryLoadingException;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.station.api.IStationHeaderFactory;
import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.station.api.InvalidFrameCode;
import jpl.gds.station.api.sle.ISleHeader;

/**
 * This is the database read/retrieval interface to the Frame table in the
 * MPCS database.  This class will retrieve one or more frames from the database
 * based on a number of different query input parameters.
 *
 * The general way to use this class is:
 * <ol>
 * <li>Create a TestSessionInfo object and set all the information on it that should be used
 * to search the database for test sessions</li>
 * <li>Create any other necessary query values such as vcid</li>
 * <li>Call one of the "getFrames(...)" methods and specify a batch size for the size
 * of lists that should be returned.  The "getFrames(...)" methods will return only
 * the first batch of results.</li>
 * <li>Make further calls to getNextResultBatch() to retrieve the rest of the results from
 * the query</li>
 *
 * Eventually should handle finer ERT.
 *
 */
public class FrameFetch extends AbstractMySqlFetch implements IFrameFetch
{
    private boolean addASM = false;
	private List<ITransferFrameDefinition> tff = null;

    private IDbSessionPreFetch spf = null;

    /** If true need bodies */
    private boolean needBodies = false;

    /** If true restore headers and trailers to bodies */
    private boolean addHeaders = false;

    private final IDbFrameFactory          dbFrameFactory;

    private final IOrderByTypeFactory      orderByTypeFactory;

	private final IStationHeaderFactory stationHeaderFactory;


	/**
     * Creates an instance of FrameFetch.
     * 
     * @param appContext
     *            the Spring Application COntext
     * @param printSqlStmt
     *            The flag that indicates whether the fetch
     *            class should print out the SQL statement
     *            only or execute it.
     */
	public FrameFetch(final ApplicationContext appContext, final boolean printSqlStmt)
	{
		super(appContext, printSqlStmt);
        dbFrameFactory = appContext.getBean(IDbFrameFactory.class);
        orderByTypeFactory = appContext.getBean(IOrderByTypeFactory.class);
		addASM = false;

        /**
         * MPCS-9001 -  Must enable and load the TF dict.
         */
        try {
			appContext.getBean(FlightDictionaryLoadingStrategy.class)
				.enableFrame()
				.loadAllEnabled(appContext, false);
		} catch (final Exception e) {
			throw new DictionaryLoadingException(DictionaryType.FRAME, e);
		}

		stationHeaderFactory = appContext.getBean(IStationHeaderFactory.class);
	}


	/**
     * Creates an instance of FrameFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public FrameFetch(final ApplicationContext appContext)
	{
		this(appContext, false);
	}


    /**
     * Retrieve a list of frames based on the search information given in the
     * testSessionInfo input object and the other frame parameters. The batch
     * size will specify how many frame results will be returned at a time.
     * This method will return the first batch of results; to get more results
     * from this query, the getNextResultBatch() method should be called.
     * If this method is called a second time before all results from the first
     * call to this method are retrieved, any unretrieved results will be lost
     * (in other words, this object only remembers the results of one query at
     * a time).
     *
     * @param tsi
     *            Used to find test sessions in the database
     * @param range
     *            Time range for the query, start, start/stop, stop, or
     *            null
     * @param batchSize
     *            The number of results returned at once.
     *            This specifies an upper limit on the number of results
     *            that will be returned at once.
     * 
     * @param params
     *            params[0] = FrameQueryOptions
     *            Query options used to build the where clause.
     *            params[1] = boolean
     *            True if ASM is to be added to the frames.
     *            params[2] = AbstractOrderByType
     *            The enumerated value specifying how the output should
     *            be ordered. If null, the default ordering will be
     *            used.
     *            params[3] = FrameWhereControl
     *            Where clause builder for the query
     *            params[4] = AttachBody
     *            Whether to attach the framebody to the IDbFrameProvider.
     *            params[5] = Attach headers and trailers
     *            Whether to attach the headers and trailer to the IDbFrameProvider.
     *
     * @return The list of frames that matched the input search information.
     *         This list will have a maximum size of "batchSize"
     *
     * @throws DatabaseException
     *             If there is a problem retrieving the frames
     */
	@Override
	public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, DatabaseTimeRange range, final int batchSize,
	                                     final Object... params) throws DatabaseException {
		if(tsi == null)
		{
			throw new IllegalArgumentException("Input test session information was null");
		}

		addASM = (Boolean)params[1];

		if(!dbProperties.getUseDatabase())
		{
            return (new ArrayList<IDbFrameProvider>(0));
		}

		if(range == null)
		{
			range = new DatabaseTimeRange(DatabaseTimeType.ERT);
		}

		initQuery(batchSize);

		String whereClause = null;

		final FrameWhereControl fwc = new FrameWhereControl("");

		//TODO: For now this is a kludge to make the weird implementation of framefetch
		//fit the common interface used by the rest of the query tools
		final Object[] newParams = new Object[params.length+2];

		System.arraycopy(params,0,newParams,0,params.length);

		newParams[3] = fwc;

        // Create pre-fetch query and execute

        if (printStmtOnly)
        {
            // Dummy to get SQL statement printed

            final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true, PreFetchType.NORMAL);
            try {
                dummy.get(tsi);
            } finally {
                dummy.close();
            }
        }

        // Must always run, even with printStmtOnly, to populate main query

        spf = fetchFactory.getSessionPreFetch(false, PreFetchType.NORMAL);
        
        final String canned;
        
        try {
            spf.get(tsi);

            canned = spf.getIdHostWhereClause(DB_FRAME_TABLE_ABBREV);
        } finally {
            spf.close();
        }

        // Force "where control" to take it as is

        fwc.setCanned(canned);

        whereClause = getSqlWhereClause(canned, range, newParams);

		return populateAndExecute(tsi, range, whereClause, newParams);
	}


	/**
     * {@inheritDoc}
	 */
	@Override
	public String getSqlWhereClause(final String testSqlTemplate, final DatabaseTimeRange range,
	                                final Object... params) throws DatabaseException
	{
		final IFrameQueryOptionsProvider qo = (IFrameQueryOptionsProvider)params[0];
		final IDbOrderByType orderType = (IDbOrderByType)params[2];
		final FrameWhereControl fwc = (FrameWhereControl)params[3];

		final StringBuilder sb = new StringBuilder(1024);

		try
		{
			fwc.addQueryForType(qo.getFrameType());
			fwc.addQueryForVcid(qo.getVcid());
			fwc.addQueryForVcfc(qo.getVcfcs());
			fwc.addQueryForDss(qo.getDss());
			fwc.addQueryForBadReason(qo.getGood());
			fwc.addQueryForRelayId(qo.getRelayId());
			fwc.addQueryForId(qo.getFrameId());

            /** MPCS-6808 Add RCT */
            if (range.getTimeType().equals(DatabaseTimeType.ERT))
            {
                fwc.addQueryForErtCoarseFine(range);
            }
            else if (range.getTimeType().equals(DatabaseTimeType.RCT))
            {
                fwc.addQueryForRctCoarseFine(range);
            }
            else
            {
                throw new WhereControlException("Database time type must be ERT or RCT: " + range.getTimeType());
            }

			sb.append(fwc.generateWhereClause(DB_FRAME_TABLE_ABBREV, ISessionFetch.DB_SESSION_TABLE_ABBREV));
			sb.append(" ");
            sb.append(orderType == null
                    ? orderByTypeFactory.getOrderByType(OrderByType.FRAME_ORDER_BY).getOrderByClause()
                    : orderType.getOrderByClause());
		}
		catch(final WhereControlException e)
		{
			throw new DatabaseException("Error generating frame query WHERE clause: " + e.getMessage(), e);
		}

		return(sb.toString());
	}


    /**
     * {@inheritDoc}
	 */
	@Override
	protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
	                                                       final String whereClause, final Object... params) throws DatabaseException
	{
	    if ((params.length >= 5) && (params[4] != null))
        {
	        needBodies = ((Boolean) params[4]).booleanValue();
	    }

        if (needBodies && (params.length >= 6) && (params[5] != null))
        {
	        addHeaders = ((Boolean) params[5]).booleanValue();
        }
	    
		if(tsi == null)
		{
			throw new IllegalArgumentException("The input test session information was null");
		}

		if(whereClause == null)
		{
			throw new IllegalArgumentException("The input where clause was null");
		}

		final FrameWhereControl fwc = (FrameWhereControl)params[3];

		try
		{
			final int i = 1;

			QueryClauseType clauseType = null;

            // Force because pre-query gives us the sessions

            if (addHeaders)
            {
                clauseType = QueryClauseType.NO_JOIN_BODY_HEADER;
            }
            else if (needBodies)
            {
                clauseType = QueryClauseType.NO_JOIN_BODY;
            }
            else
            {
                clauseType = QueryClauseType.NO_JOIN;
            }

			// The combination of FORWARD_ONLY, CONCUR_READ_ONLY, and a fetch
			// size of Integer.MIN_VALUE signals to the MySQL JDBC driver to
			// stream the results row by row rather than trying to load the
			// whole result set into memory. Any other settings will result
			// in a heap overflow. Note that this will probably break if we
			// change database vendors.

			final String selectClause =
				queryConfig.getQueryClause(clauseType,
						IFrameLDIStore.DB_FRAME_DATA_TABLE_NAME);

			statement = getPreparedStatement(
					selectClause + whereClause,
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY);

			statement.setFetchSize(Integer.MIN_VALUE);

			fwc.setParameters(statement, i);

			if (printStmtOnly)
			{
				printSqlStatement(statement);
			}
			else
			{
				results = statement.executeQuery();
			}
		}
		catch(final SQLException e)
		{
			throw new DatabaseException("Error retrieving frames from database: " + e.getMessage(), e);
		}
		catch(final WhereControlException e)
		{
			throw new DatabaseException("Error in SQL WHERE clause: " + e.getMessage(), e);
		}

		return getResults();
	}


	/**
     * {@inheritDoc}
	 */
	@Override
    public List<IDbFrameProvider> getNextResultBatch()
            throws DatabaseException
	{
		return getResults();
	}


	/**
     * This is the internal class method that keeps track of the JDBC ResultSet returned by
     * a query. Every call to this method will return a list of test configurations that match
     * the original query. The size of the returned lists is determined by the batch size that was
     * entered when the query was made. When there are no more results, this method will return an
     * empty list.
     *
     * @return The list of frames that is part of the results of a query executed using the
     *         "getFrames(...)" methods. When an empty list is returned, it means no more results are left.
     *
     *         NB: If we fetch the bodies, we wind up setting the length to the actual final length,
     *         i.e., with whatever we tack on (headers, trailers, ASM.)
     *
     *         If we do not fetch the bodies we just set the basic body length.
     *
     * @throws DatabaseException
     *             If there happens to be an exception retrieving the next set of results
     */
	@Override
    protected List<IDbFrameProvider> getResults() throws DatabaseException
	{
		int currentTransferFrameFormat = 0;
        final List<IDbFrameProvider> refs = new ArrayList<IDbFrameProvider>();

		if(results == null) {
			return (refs);
		}

		int count = 0;
		try
        {
            /** MPCS-6718  Look for warnings */

            final List<SQLWarning> warnings = new ArrayList<SQLWarning>();

			//loop through until we fill up our first batch or we've
			//got no more results
			while(count < batchSize) {
				if(results.next() == false) {
					break;
				}

                if (null == tff) {
				    final ITransferFrameDefinitionProvider frameDict = appContext.getBean(ITransferFrameDefinitionProvider.class);
				    tff = frameDict.getFrameDefinitions();
				}

                final IDbFrameUpdater frame = dbFrameFactory.createQueryableUpdater();

                // Begin MPCS-5189  Always get body length

                byte[] aBody      = null;
                final int    bodyLength = results.getInt(DB_FRAME_TABLE_ABBREV + ".bodyLength");
                String type       = null;
    
                if (needBodies)
                {
                    type  = results.getString(DB_FRAME_TABLE_ABBREV + ".type").toUpperCase();
                    aBody = fetchBody(results, bodyLength);

                    // Set the body and the length

                    frame.setRecordBytes(aBody);
                } 
                else
                {
                    // Don't set the body, just the length

                    frame.setRecordBytesLength(bodyLength);
                }

                // End MPCS-5189

                // Do ASM before restoring the headers/trailers

				if (needBodies && addASM)
                {
					int nextASM = 0;
					byte[] anASM = null;
					byte[] theBody = null;

					for ( currentTransferFrameFormat = 0; currentTransferFrameFormat < tff.size(); ++currentTransferFrameFormat ) {
						if ( tff.get(currentTransferFrameFormat).getName().equals ( type ) ) {
						    
						    /* 
						     * MPCS-7993 - Does not apply only to turbo frames any more,
						     * but to any frame type that arrives without ASM. 
						     */
							if ( !tff.get(currentTransferFrameFormat).arrivesWithASM()) {
								anASM = tff.get(currentTransferFrameFormat).getASM();
							}
							break;
						}
					}
					// currentTransferFrameFormat contains the current tff format

					if ( anASM != null ) {
						// Now we have an ASM as a byte[].
						// It prefixes the body.
						// The length is the sum of the two.

						theBody = new byte [ anASM.length + aBody.length ];

						boolean asmDoesNotAlreadyExist = true;
						nextASM = 0;
						do {
							asmDoesNotAlreadyExist = anASM [ nextASM ] == aBody [ nextASM ];
						} while (asmDoesNotAlreadyExist && (++nextASM < anASM.length));

						// asmDoesNotAlreadyExist is inverted.
						asmDoesNotAlreadyExist = !asmDoesNotAlreadyExist;
						if ( asmDoesNotAlreadyExist ) {
						    
							/* MPCS-7993 -  I just could not stand that this was
							 * copying the whole frame one byte at a time. I changed this to
							 * use arraycopy. Maybe it is not in truth any faster but it makes
							 * me feel better.
							 */
							System.arraycopy(anASM, 0, theBody, 0, anASM.length);
							System.arraycopy(aBody, 0, theBody, anASM.length, aBody.length);
							

							// ASM was restored.

							frame.setRecordBytes(theBody);
						} else {
							// ASM was already attached
						    frame.setRecordBytes(aBody);
						}
					} else {
						// Not a TURBO type frame --- we leave the frame alone
                        frame.setRecordBytes(aBody);
					}
				}
                else if (needBodies) // MPCS-5189
                {
					// do not add ASM for any frame type

                    frame.setRecordBytes(aBody);
				}

                // Now we can restore the headers and trailers

                if (needBodies && addHeaders)
                {
                    aBody = fetchRestoredBody(aBody, type);

                    frame.setRecordBytes(aBody);
                }

				//pull out all the information for this frame and add it
				//to the return list
				frame.setType(results.getString(DB_FRAME_TABLE_ABBREV + ".type"));

				if(addHeaders) {
					frame.setSleMetadata(parseSleMetadata());
				}

                final long coarse = results.getLong(DB_FRAME_TABLE_ABBREV + ".ertCoarse");
                final int  fine   = results.getInt(DB_FRAME_TABLE_ABBREV + ".ertFine");

				frame.setErt(
                    new AccurateDateTime(
                            DbTimeUtility.exactFromErtCoarseFine(coarse, fine),
                            DbTimeUtility.fineFromErtFine(fine)));

                frame.setRct(
                    DbTimeUtility.dateFromCoarseFine(
                        results.getLong(DB_FRAME_TABLE_ABBREV + ".rctCoarse"),
                        results.getInt(DB_FRAME_TABLE_ABBREV + ".rctFine")));

				frame.setRelaySpacecraftId(results.getInt(DB_FRAME_TABLE_ABBREV + ".relaySpacecraftId"));
				frame.setVcid(results.getInt(DB_FRAME_TABLE_ABBREV + ".vcid"));

                /** MPCS-6809  Treat as unsigned */
                /** MPCS-7639 Use new method */
                final UnsignedLong vcfc = getUnsignedLong(results, DB_FRAME_TABLE_ABBREV + ".vcfc");

                if (vcfc == null)
                {
                    frame.setVcfc(null);
                }
                else
                {
                    frame.setVcfc(vcfc.intValue());
                }

                // MPCS-4839  Verify and log and correct
                // MPCS-6349 : DSS ID not set properly
                frame.setRecordDssId(getCorrectedStation(results, DB_FRAME_TABLE_ABBREV + ".dssId"));

				frame.setBitRate((double) results.getFloat(DB_FRAME_TABLE_ABBREV + ".bitRate"));

				frame.setFillFrame(
                    results.getInt(DB_FRAME_TABLE_ABBREV + ".fillFrame") != 0);

                final long sessionId =
                               results.getLong(DB_FRAME_TABLE_ABBREV + "." + SESSION_ID);

				frame.setSessionId(sessionId);

                /** MPCS-6718 Look for warnings */

                warnings.clear();

                frame.setSessionFragment(
                    SessionFragmentHolder.getFromDbRethrow(
                        results,
                        DB_FRAME_TABLE_ABBREV + "." + FRAGMENT_ID,
                        warnings));

                SqlExceptionTools.logWarning(warnings, trace);

                final int    hostId      = results.getInt(
                                               DB_FRAME_TABLE_ABBREV + "." + HOST_ID);
                final String sessionHost = spf.lookupHost(hostId);

                if ((sessionHost == null) || (sessionHost.length() == 0))
                {
                    throw new DatabaseException("Unable to get sessionHost for " +
                                           " hostId "                       +
                                           hostId);
                }

				frame.setSessionHost(sessionHost);

                /** MPCS-6809 Treat as unsigned */
                /** MPCS-7639  Use new method */
                final UnsignedLong id = getUnsignedLong(results, DB_FRAME_TABLE_ABBREV + ".id");

                if (id == null)
                {
                    frame.setId(null);
                }
                else
                {
                    frame.setId(id.longValue());
                }

				final String str =
					results.getString(DB_FRAME_TABLE_ABBREV + ".badReason");

				// NULL means a good frame, hence no "bad reason"

				if (str != null)
				{
                    /** MPCS-8027 Refactor */
                    // Empty or unknown goes to UNKNOWN
                    // UNKNOWN_VCID goes to BAD_VCID
                    // UNKNOWN_VCID is redundant and will be removed from
                    // database schema eventually. This is just a precaution,
                    // as there will not actually be any UNKNOWN_VCID values
                    // in the database.

					final String br = str.trim();

                    InvalidFrameCode ifc = InvalidFrameCode.UNKNOWN;

                    if (! br.isEmpty())
                    {
                        if (br.equals("UNKNOWN_VCID"))
                        {
                            ifc = InvalidFrameCode.BAD_VCID;
                        }
                        else
                        {
                            try
                            {
                                ifc = InvalidFrameCode.valueOf(br);
                            }
                            catch (final IllegalArgumentException iae)
                            {
                                ifc = InvalidFrameCode.UNKNOWN;
                            }
                        }
                    }

					frame.setBadReason(ifc);
				}

				refs.add(frame);
				count++;

                // Handle any unhandled warnings
                /** MPCS-6718 */
                SqlExceptionTools.logWarning(trace, results);
			}

			//if we're all done with results, clean up
			//all the resources
			if(results.isAfterLast())
			{
				results.close();
				statement.close();

				results = null;
				statement = null;
			}
		}
		catch(final SQLException e) {
			throw new DatabaseException("Error retrieving frames from database: " + e.getMessage(), e);
		}
		catch(final BeansException be) {
			throw new DatabaseException ("Could not get TransferFrameDictionary " + be.getMessage(), be);
		}
		return (refs);
	}

	/** Parse SLE metadata as key-value string */
	private String parseSleMetadata() throws SQLException {
		final byte[] headerBytes = results.getBytes(DB_FRAME_BODY_TABLE_ABBREV + ".header");

		if(headerBytes == null){
			return null;
		}

		//see if we have a SLE header
		final IStationTelemHeader stationHeader = stationHeaderFactory.createSleHeader();
		try {
			stationHeader.load(headerBytes, 0);
		}
		catch (IOException e){
			trace.warn("Could not parse SLE metadata", e);
		}

		//parse SLE metadata as key-value
		if(stationHeader instanceof ISleHeader){
			return ((ISleHeader) stationHeader).getMetadata().toString();
		}

		return null;
	}
}
