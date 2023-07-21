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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.VenueType;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.ApidRanges;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.IPacketFetch;
import jpl.gds.db.api.sql.fetch.PreFetchType;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.fetch.SpscRanges;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.store.ldi.IPacketLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISsePacketLDIStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbPacketFactory;
import jpl.gds.db.api.types.IDbPacketProvider;
import jpl.gds.db.api.types.IDbPacketUpdater;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.mysql.impl.sql.order.PacketOrderByType;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.types.UnsignedLong;


/**
 * This is the database read/retrieval interface to the Packet table in the
 * MPCS database.  This class will retrieve one or more packets from the database
 * based on a number of different query input parameters.
 *
 * The general way to use this class is:
 * <ol>
 * <li>Create a TestSessionInfo object and set all the information on it that should be used
 * to search the database for test sessions</li>
 * <li>Create any other necessary query values such as APID</li>
 * <li>Call one of the "getPackets(...)" methods and specify a batch size for the size
 * of lists that should be returned.  The "getPackets(...)" methods will return only
 * the first batch of results.</li>
 * <li>Make further calls to getNextResultBatch() to retrieve the rest of the results from
 * the query</li>
 *
 */
public class PacketFetch extends AbstractMySqlFetch implements IPacketFetch
{
    private final String DB_TABLE = IPacketLDIStore.DB_PACKET_DATA_TABLE_NAME;
    
    private final String DB_SSE_TABLE = ISsePacketLDIStore.DB_SSE_PACKET_DATA_TABLE_NAME;

    /** The abbreviation that should as a variable name in an SQL statement using the packet table */
    private final String tableAbbrev = queryConfig.getTablePrefix(DB_TABLE);

    private final String tableSseAbbrev = queryConfig.getTablePrefix(DB_SSE_TABLE);
    
    //Templated WHERE clauses for restricting the query

//  String fromSseClause =
//      queryConfig.getQueryClause(QueryClauseType.FROM_SSE_CONDITION,
//                                 DB_TABLE);

    private final String idClause =
        queryConfig.getQueryClause(QueryClauseType.RECORD_ID_CONDITION,
                getActualTableName(DB_TABLE));

    private final String sseIdClause =
        queryConfig.getQueryClause(QueryClauseType.RECORD_ID_CONDITION,
                getActualTableName(DB_SSE_TABLE));

    /**
	 * Need this here to get venue type for each session.
	 */
	private IDbSessionPreFetch spf = null;
	 
    /** If true need bodies */
    private boolean needBodies = false;

    /** If true restore headers and trailers to bodies */
    private boolean addHeaders = false;

    private final IDbPacketFactory dbPacketFactory;


	/**
     * Creates an instance of PacketFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            The flag that indicates whether the fetch
     *            class should print out the SQL statement
     *            only or execute it.
     */
	public PacketFetch(final ApplicationContext appContext, final boolean printSqlStmt)
	{
		super(appContext, printSqlStmt);
        dbPacketFactory = appContext.getBean(IDbPacketFactory.class);
	}

	/**
     * Creates an instance of PacketFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public PacketFetch(final ApplicationContext appContext)
	{
		this(appContext, false);
	}
	
    /**
     * {@inheritDoc}
	 */
	@Override
	public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, DatabaseTimeRange range, final int batchSize,
                                         final Object... params) throws DatabaseException {
		if(tsi == null)
		{
			throw new IllegalArgumentException("Input test session information was null");
		}

        if(!dbProperties.getUseDatabase())
		{
            return (new ArrayList<IDbPacketProvider>());
		}

		if(range == null)
		{
			range = new DatabaseTimeRange(DatabaseTimeType.SCET);
		}

		initQuery(batchSize);

		final Boolean fromSse = (Boolean)params[3];
		
        String whereClause = null;

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
        try {
            spf.get(tsi);

            if (fromSse == null || !fromSse) {
                whereClause = getSqlWhereClause(spf
                    .getIdHostWhereClause(tableAbbrev), range, params);
            } else {
                whereClause = getSqlWhereClause(spf
                    .getIdHostWhereClause(tableSseAbbrev), range, params);
            }
        } finally {
            spf.close();
        }

		return populateAndExecute(tsi, range, whereClause, params);
	}


	/**
     * {@inheritDoc}
	 */
	@Override
	public String getSqlWhereClause(final String testSqlTemplate, final DatabaseTimeRange range,
                                    final Object... params) throws DatabaseException
	{
		if(testSqlTemplate == null)
		{
			throw new IllegalArgumentException("Input test information template was null");
		}

		if(range == null)
		{
			throw new IllegalArgumentException("The input time range information was null");
		}
		
		final ApidRanges    apid      = (ApidRanges)params[0];

		@SuppressWarnings("unchecked")
		final Set<Integer>  vcid      = (Set<Integer>) params[1];

		final SpscRanges     spsc      = (SpscRanges)          params[2];
		final Boolean        fromSse   = (Boolean)             params[3];
		final PacketIdHolder id        = (PacketIdHolder)      params[4];
		IDbOrderByType  orderType = (IDbOrderByType) params[5];

		@SuppressWarnings("unchecked")
		final Set<Integer>  dssId     = (Set<Integer>)
                                        ((params.length >= 8)
                                             ? params[7]
                                             : null);

        final boolean       isSse     = ((fromSse != null) && fromSse);
		final String        abbrev    = (isSse ? tableSseAbbrev : tableAbbrev);
		String              sqlWhere  = null;

        if(! testSqlTemplate.equals(""))
		{
			sqlWhere = addToWhere(sqlWhere, testSqlTemplate);
		}

        // Add apid filtering if needed

        if ((apid != null) && ! apid.isEmpty())
        {
        	sqlWhere = addToWhere(sqlWhere, apid.whereClause(abbrev));
		}

        // Add SPSC filtering if needed

		if ((spsc != null) && ! spsc.isEmpty())
		{
			sqlWhere = addToWhere(sqlWhere, spsc.whereClause(abbrev));
		}

        if (! isSse)
        {
            if ((vcid != null) && ! vcid.isEmpty())
            {
                // Packet.vcid is nullable

			    sqlWhere = addToWhere(sqlWhere,
                                      generateVcidWhere(vcid,
                                                        tableAbbrev,
                                                        true));
            }

            if ((dssId != null) && ! dssId.isEmpty())
            {
			    sqlWhere = addToWhere(sqlWhere,
                                      generateDssIdWhere(dssId, tableAbbrev));
            }
        }

        if (id != null)
        {
            if (! isSse)
            {
                sqlWhere = addToWhere(sqlWhere, idClause);
            }
            else
            {
                sqlWhere = addToWhere(sqlWhere, sseIdClause);
            }
        }

		// Add the proper time clause and order by the time type
		// if no ordering has been specified
        /** MPCS-8384 Extended support */

        final String timeWhere =
            DbTimeUtility.generateTimeWhereClause(
                abbrev, range, false, false,  _extendedDatabase);

        if (timeWhere.length() > 0)
        {
            sqlWhere = addToWhere(sqlWhere, timeWhere);
        }

		switch(range.getTimeType().getValueAsInt())
		{
			case DatabaseTimeType.SCET_TYPE:
				if(orderType == null)
				{
					orderType = PacketOrderByType.SCET;
				}
				break;

			case DatabaseTimeType.ERT_TYPE:
				if(orderType == null)
				{
					orderType = PacketOrderByType.ERT;
				}
				break;

			case DatabaseTimeType.RCT_TYPE:
				if(orderType == null)
				{
					orderType = PacketOrderByType.RCT;
				}
				break;

			case DatabaseTimeType.SCLK_TYPE:
				if(orderType == null)
				{
					orderType = PacketOrderByType.SCLK;
				}
				break;
			
			case DatabaseTimeType.LST_TYPE:
				if(orderType == null)
				{
					orderType = PacketOrderByType.LST;
				}
				break;

			default:
				throw new IllegalArgumentException("Invalid database time type \"" + range.getTimeType().toString() + "\" received by " + this.getClass().getName());
		}

		// Add the ordering

        final StringBuilder sb = new StringBuilder();

		if (sqlWhere != null)
        {
            sb.append(sqlWhere);
		}

        sb.append(' ').append(orderType.getOrderByClause());

		return sb.toString();
	}

    /**
     * {@inheritDoc}
	 */
	@Override
	protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
            final String whereClause, final Object... params) throws DatabaseException {
        if(tsi == null)
		{
			throw new IllegalArgumentException("The input test session information was null");
		}

		if(range == null)
		{
			throw new IllegalArgumentException("The input time range information was null");
		}

		if(whereClause == null)
		{
			throw new IllegalArgumentException("The input where clause was null");
		}
		
		final Boolean        fromSse = (Boolean)        params[3];
		final PacketIdHolder id      = (PacketIdHolder) params[4];

        if ((params.length >= 7) && (params[6] != null))
        {
            needBodies = ((Boolean) params[6]).booleanValue();
        }

        if (needBodies && (params.length >= 9) && (params[8] != null))
        {
            addHeaders = ((Boolean) params[8]).booleanValue();
        }
	
		try
		{
			int i = 1;

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

		    String selectClause;
			
		    if (fromSse == null || !fromSse)
            {
		        selectClause = queryConfig.getQueryClause(
                                   clauseType,
                                   getActualTableName(IPacketLDIStore.DB_PACKET_DATA_TABLE_NAME));
	        }
            else
            {
	            selectClause = queryConfig.getQueryClause(
                                   clauseType,
                                   getActualTableName(ISsePacketLDIStore.DB_SSE_PACKET_DATA_TABLE_NAME));
	        }
		    
            // The combination of FORWARD_ONLY, CONCUR_READ_ONLY, and a fetch size
			// of Integer.MIN_VALUE signals to the MySQL JDBC driver to stream the
			// results row by row rather than trying to load the whole result set
			// into memory. Any other settings will result in a heap overflow. Note
			// that this will probably break if we change database vendors.

			this.statement = getPreparedStatement(
                                 selectClause + whereClause,
					             ResultSet.TYPE_FORWARD_ONLY,
                                 ResultSet.CONCUR_READ_ONLY);

			this.statement.setFetchSize(Integer.MIN_VALUE);

//			if (fromSse != null)
//			{
//              this.statement.setInt(
//                  i++,
//                  GDR.getIntFromBoolean(fromSse.booleanValue()));
//			}

            if (id != null)
            {
                 id.insert(statement, i, null);

                 ++i;
            }

			if (this.printStmtOnly) {
				printSqlStatement(this.statement, "Main");
			}
			else {
				this.results = this.statement.executeQuery();
			}
			
		}
		catch(final SQLException e)
		{
			throw new DatabaseException("Error retrieving packets from database: " + e.getMessage(), e);
		}

		return(getResults());
	}

	/*
	 * (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.AbstractMySqlFetch#getNextResultBatch()
	 */
	@Override
    public List<IDbPacketProvider> getNextResultBatch() throws DatabaseException
	{
		return ( getResults() );
	}


	/**
     * This is the internal class method that keeps track of the JDBC ResultSet returned by
     * a query. Every call to this method will return a list of test configurations that match
     * the original query. The size of the returned lists is determined by the batch size that was
     * entered when the query was made. When there are no more results, this method will return an
     * empty list.
     *
     * Warning is a false positive as we are just wrapping;
     *
     * @return The list of packets that is part of the results of a query executed using the
     *         "getPackets(...)" methods. When an empty list is returned, it means no more results are left.
     *
     * @throws DatabaseException
     *             If there happens to be an exception retrieving the next set of results
     */
	@Override
    protected List<IDbPacketProvider> getResults() throws DatabaseException
	{
        final List<IDbPacketProvider> refs = new ArrayList<IDbPacketProvider>();

		if(this.results == null)
		{
			return(refs);
		}

		int count = 0;

		try
		{
            /** MPCS-6718 Look for warnings */

            final List<SQLWarning> warnings = new ArrayList<SQLWarning>();

			//loop through until we fill up our first batch or we've
			//got no more results
			while(count < this.batchSize)
			{
				if(this.results.next() == false)
				{
					break;
				}

				//pull out all the information for this packet and add it
				//to the return list
                final IDbPacketUpdater dp = dbPacketFactory.createQueryableUpdater();

                /** BEGIN MPCS-5189  */

                final int bodyLength = results.getInt("bodyLength");

                if (needBodies)
                {
                    byte[] aBody = null;

                    if (addHeaders)
                    {
                        aBody = fetchRestoredBody(bodyLength, null);
                    }
                    else
                    {
                        aBody = fetchBody(results, bodyLength);
                    }

                    // Set body and length

                    dp.setRecordBytes(aBody);
                }
                else
                {
                    // Set just the length

                    dp.setRecordBytesLength(bodyLength);
                }

                /** END MPCS-5189 */

				dp.setRct(DbTimeUtility.dateFromCoarseFine(
                              this.results.getLong("rctCoarse"),
                              this.results.getInt("rctFine")));

                long coarse = this.results.getLong("ertCoarse");
                int  fine   = this.results.getInt("ertFine");

                dp.setErt(
                    new AccurateDateTime(
                            DbTimeUtility.exactFromErtCoarseFine(coarse, fine),
                            DbTimeUtility.fineFromErtFine(fine)));

                coarse = this.results.getLong("scetCoarse");
                fine   = this.results.getInt("scetFine");

                 /** MPCS-8384  */
                if (! _extendedDatabase)
                {
                    fine *= DbTimeUtility.SCET_SHORT_CONVERSION;
                }

                /** MPCS-8384  */
                dp.setScet(
                    new AccurateDateTime(DbTimeUtility.exactFromScetCoarseFine(coarse, fine),
                                         DbTimeUtility.fineFromScetFine(fine)));

                final long coarseSclk = this.results.getLong("sclkCoarse");
                final long fineSclk   = this.results.getLong("sclkFine");
				final ISclk sclk       = new Sclk(coarseSclk, fineSclk);

                dp.setSclk(sclk);

                // MPCS-4839  Verify and log and correct
                // MPCS-6349 : DSS ID not set properly
                dp.setRecordDssId(getCorrectedStation(results, "dssId"));

                final int vcid = this.results.getInt("vcid");

                dp.setVcid((! this.results.wasNull()) ? vcid : null);
 
                dp.setApid(this.results.getInt("apid"));
                dp.setApidName(this.results.getString("apidName"));
				dp.setSpsc(this.results.getInt("spsc"));
				dp.setFromSse(this.results.getInt("fromSse") != 0);

                final long sessionId = this.results.getLong(SESSION_ID);

                dp.setSessionId(sessionId);

                /** MPCS-6718  Look for warnings */

                warnings.clear();

                dp.setSessionFragment(
                    SessionFragmentHolder.getFromDbRethrow(results, FRAGMENT_ID, warnings));

                SqlExceptionTools.logWarning(warnings, trace);

                final int    hostId      = results.getInt(HOST_ID);
                final String sessionHost = spf.lookupHost(hostId);

                if ((sessionHost == null) || (sessionHost.length() == 0))
                {
                    throw new DatabaseException("Unable to get sessionHost for " +
                                           " hostId "                       +
                                           hostId);
                }

                dp.setSessionHost(sessionHost);

                final VenueType vt = spf.lookupVenue(hostId, dp.getSessionId());

                if (missionProps.getVenueUsesSol(vt) &&
                        TimeProperties.getInstance().usesLst())
                {
                    dp.setLst(LocalSolarTimeFactory.getNewLst(dp.getScet(), spf.lookupSCID(hostId, dp.getSessionId())));
                }

                try
                {
                    /** MPCS-6718 Look for warnings */

                    warnings.clear();

                    dp.setPacketId(PacketIdHolder.getFromDb(results, "id", warnings));

                    SqlExceptionTools.logWarning(warnings, trace);
                }
                catch (final HolderException he)
                {
                    throw new DatabaseException("Bad value for id", he);
                }

                /** MPCS-6809  Treat as unsigned */
                /** MPCS-7639  Use new method */
                final UnsignedLong vcfc = getUnsignedLong(results, "sourceVcfc");

                if (vcfc == null)
                {
                    dp.setSourceVcfcs(null);
                }
                else
                {
                    dp.setSourceVcfcs(Collections.singletonList(vcfc.longValue()));
                }

                /** MPCS-6809  Treat as unsigned */
                /** MPCS-7639  Use new method */
                final UnsignedLong frameId = getUnsignedLong(results, "frameId");

                if (frameId == null)
                {
                    dp.setFrameId(null);
                }
                else
                {
                    dp.setFrameId(frameId.longValue());
                }

                dp.setFillFlag(results.getInt("fillFlag") != 0);

				refs.add(dp);
				count++;

                // Handle any unhandled warnings
                /** MPCS-6718 */
                SqlExceptionTools.logWarning(trace, results);
			}

			//if we're all done with results, clean up
			//all the resources
			if(this.results.isAfterLast() == true)
			{
				this.results.close();
				this.statement.close();

				this.results = null;
				this.statement = null;
			}
		}
		catch(final SQLException e)
		{
			throw new DatabaseException("Error retrieving packets from database: " + e.getMessage(), e);
		}

		return(refs);
	}
}
