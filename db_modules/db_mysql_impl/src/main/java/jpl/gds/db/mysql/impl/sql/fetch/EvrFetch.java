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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.VenueType;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.IEvrFetch;
import jpl.gds.db.api.sql.fetch.PreFetchType;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.store.ldi.IEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseEvrLDIStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbEvrFactory;
import jpl.gds.db.api.types.IDbEvrProvider;
import jpl.gds.db.api.types.IDbEvrUpdater;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.mysql.impl.sql.order.EvrOrderByType;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.gdr.GDR;
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
import jpl.gds.shared.types.Quintuplet;

/**
 * This is the database read/retrieval interface to the Evr table in the
 * MPCS database.  This class will retrieve one or more EVRs from the database
 * based on a number of different query input parameters.
 * Note that the final ordering involves multiple columns. That is
 * necessary for the fetch logic to properly associate the Evr metadata to the
 * Evr.
 *
 * The general way to use this class is:
 * <ol>
 * <li>Create a TestSessionInfo object and set all the information on it that should be used
 * to search the database for test sessions</li>
 * <li>Create any other necessary query values such as level</li>
 * <li>Call one of the "getEvrs(...)" methods and specify a batch size for the size
 * of lists that should be returned.  The "getEvrs(...)" methods will return only
 * the first batch of results.</li>
 * <li>Make further calls to getNextResultBatch() to retrieve the rest of the results from
 * the query</li>
 *
 */
public class EvrFetch extends AbstractMySqlFetch implements IEvrFetch
{
    private final String DB_TABLE = IEvrLDIStore.DB_EVR_DATA_TABLE_NAME;

    /** The abbreviations that should as variable names in an SQL statement using the EVR table */
    private final String        tableAbbrev            = queryConfig.getTablePrefix(DB_TABLE);
    private final String        metadataTableAbbrev    = queryConfig.getTablePrefix(IEvrLDIStore.DB_EVR_METADATA_TABLE_NAME);

    private final String        DB_SSE_TABLE           = ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME;
    
    private final String        tableSseAbbrev         = queryConfig.getTablePrefix(DB_SSE_TABLE);

    private final String        metadataTableSseAbbrev = queryConfig.getTablePrefix(ISseEvrLDIStore.DB_SSE_EVR_METADATA_TABLE_NAME);

    //Templated WHERE clauses for restricting the query
    private final String        nameClause             =
        queryConfig.getQueryClause(QueryClauseType.EVR_NAME_CONDITION,
                getActualTableName(DB_TABLE));

    private final String        eventIdClause          =
        queryConfig.getQueryClause(QueryClauseType.EVR_EVENT_ID_CONDITION,
                getActualTableName(DB_TABLE));

    private final String        levelClause            =
        queryConfig.getQueryClause(QueryClauseType.EVR_LEVEL_CONDITION,
                getActualTableName(DB_TABLE));

    private final String        moduleClause           =
        queryConfig.getQueryClause(QueryClauseType.MODULE_CONDITION, getActualTableName(DB_TABLE));

    private final String        messageClause          =
        queryConfig.getQueryClause(QueryClauseType.MESSAGE_CONDITION, getActualTableName(DB_TABLE));
    
    private final String        nameSseClause          =
        queryConfig.getQueryClause(QueryClauseType.EVR_NAME_CONDITION,
                getActualTableName(DB_SSE_TABLE));

    private final String        eventSseIdClause       =
        queryConfig.getQueryClause(QueryClauseType.EVR_EVENT_ID_CONDITION,
                getActualTableName(DB_SSE_TABLE));

    private final String        levelSseClause         =
        queryConfig.getQueryClause(QueryClauseType.EVR_LEVEL_CONDITION,
                getActualTableName(DB_SSE_TABLE));

    private final String        moduleSseClause        =
        queryConfig.getQueryClause(QueryClauseType.MODULE_CONDITION,
                getActualTableName(DB_SSE_TABLE));

    private final String        messageSseClause       =
        queryConfig.getQueryClause(QueryClauseType.MESSAGE_CONDITION,
                getActualTableName(DB_SSE_TABLE));


	/**
	 * A query to summarize the number of EVRs for each level
	 */
	private static final String levelSummaryQuery = "SELECT distinct level, COUNT(level) AS number FROM " + IEvrLDIStore.DB_EVR_DATA_TABLE_NAME + " WHERE sessionId = ? GROUP BY level";

	/**
	 * Need this here to get venue type for each session.
	 */
	private IDbSessionPreFetch spf = null;
	
	/**
	 * The current EVR being parsed
	 */
    private IDbEvrUpdater       currentEvr;

	/**
	 * The current EVR key that was read from the database
	 * (the key for the current EVR)
	 */
    private KeyClass            currentKey;

	/**
	 * The next EVR key read from the database
	 */
    private KeyClass            nextKey;

	/**
	 * True if it's the very first set of results from a query
	 * that are being fetched
	 */
	protected boolean isFirst;
	
	private final SseEvrFetch sseEvrFetch = new SseEvrFetch();

    private final IDbEvrFactory dbEvrFactory;

    /**
     * Inner class to compute where classes for SSE Evr
     */
    private class SseEvrFetch {

        public String getSqlWhereClause(final DatabaseTimeRange range,
                final IDbContextInfoProvider tsi, final Object... params)
                throws DatabaseException {

            // ensure fromSse is set to true.
            params[5] = true;
            
            String sqlWhere;

            spf = fetchFactory.getSessionPreFetch(false, PreFetchType.NORMAL);

            try {
                spf.get(tsi);

                sqlWhere = EvrFetch.this.getSqlWhereClause(
                        spf.getIdHostWhereClause(tableSseAbbrev, metadataTableSseAbbrev), range, params);
            }
            finally {
                spf.close();
            }

            return sqlWhere;
        }


        /**
         * Get select clause for clause type.
         *
         * @param clauseType Clause type
         *
         * @return SQL string
         */
        protected String getSqlSelectClause(final QueryClauseType clauseType)
        {
            /** MPCS-8384  Adapted to support the extended tables. */

            return queryConfig.getQueryClause(
                       clauseType, getActualTableName(ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME));
        }
    }
	
	/**
     * Creates an instance of IEvrFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            The flag that indicates whether the fetch class should print
     *            out the SQL statement only or execute it.
     */
	public EvrFetch(final ApplicationContext appContext, final boolean printSqlStmt)
	{
		super(appContext, printSqlStmt);
        this.dbEvrFactory = appContext.getBean(IDbEvrFactory.class);
		this.currentEvr = null;
		this.currentKey = null;
		this.nextKey    = null;
		this.isFirst    = true;
	}

	/**
     * Creates an instance of EvrFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public EvrFetch(final ApplicationContext appContext)
	{
		this(appContext, false);
	}


    /*
     * If fromSse is null, we can do both FSW and SSE.
     *
     * @param params
     *
     * @return true or false
     */
    private static boolean doUnion(final Object[] params)
    {
		final Boolean fromSse = (Boolean) params[5];

        return (fromSse == null);
    }


    /*
     * If we do not do union, we do just FSW unless SSE
     * is specifically set.
     *
     * @param params
     *
     * @return true or false
     */
    private static boolean doFsw(final Object[] params)
    {
		final Boolean fromSse = (Boolean) params[5];

        return ! doUnion(params) && ((fromSse == null) || ! fromSse);
    }


    /*
     * We need a realtime where clause if the realtime state
     * is specified, and if we do union or just FSW. Note
     * that the clause is added only to the first (FSW) part
     * of a union.
     *
     * @param params
     *
     * @return true or false
     */
    private static boolean needRealtimeClause(final Object[] params)
    {
		final Boolean isRealtime = (Boolean) params[6];

        return (isRealtime != null) && (doUnion(params) || doFsw(params));
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
            return (new ArrayList<IDbEvrProvider>());
		}

		if(range == null)
		{
			range = new DatabaseTimeRange(DatabaseTimeType.SCET);
		}

		initQuery(batchSize);

		this.currentEvr = null;
		this.currentKey = null;
		this.nextKey    = null;

        String whereClause = null;

        // Create pre-fetch query and execute

        if (printStmtOnly)
        {
            // Dummy to get SQL statement printed
            fetchFactory.getSessionPreFetch(true, PreFetchType.NORMAL).get(tsi);
        }

        // Must always run, even with printStmtOnly, to populate main query

        spf = fetchFactory.getSessionPreFetch(false, PreFetchType.NORMAL);
        spf.get(tsi);

        whereClause = getSqlWhereClause(spf.getIdHostWhereClause(tableAbbrev, metadataTableAbbrev), range, params);

        return (populateAndExecute(tsi, range, whereClause, params));
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
		
		final String name = (String)params[0];
		final Long eventId = (Long)params[1];
		final String level = (String)params[2];
		final String module = (String)params[3];
		final String message = (String)params[4];
		final Boolean fromSse = (Boolean)params[5];
		final Boolean isRealtime = (Boolean)params[6];
		// AbstractOrderByType order = (AbstractOrderByType)params[7];
		final boolean secondHalf = (params.length >= 9) && ((Boolean) params[8]);
        @SuppressWarnings("unchecked")
        final Set<Integer> vcids =
            (Set<Integer>)
            (((params.length >= 10) && (params[9] != null))
                 ? params[9]
                 : null);

        @SuppressWarnings("unchecked")
        final Set<Integer> dssIds =
            (Set<Integer>)
            (((params.length >= 11) && (params[10] != null))
                 ? params[10]
                 : null);

		String sqlWhere = null;

		if(!testSqlTemplate.equals(""))
		{
			sqlWhere = addToWhere(sqlWhere, testSqlTemplate);
		}

		if(name != null)
		{
			sqlWhere = addToWhere(sqlWhere, (fromSse == null || fromSse == false) ? nameClause : nameSseClause);
		}

		//add event ID filtering if needed
		if(eventId != null)
		{
			sqlWhere = addToWhere(sqlWhere, (fromSse == null || fromSse == false) ? eventIdClause : eventSseIdClause);
		}

		//add module filtering if needed
		if(module != null)
		{
			sqlWhere = addToWhere(sqlWhere, (fromSse == null || fromSse == false) ? moduleClause : moduleSseClause);
		}

		//add level filtering if needed
		if(level != null)
		{
			sqlWhere = addToWhere(sqlWhere, (fromSse == null || fromSse == false) ? levelClause : levelSseClause);
		}

		//add message filtering if needed
		if(message != null)
		{
			sqlWhere = addToWhere(sqlWhere, (fromSse == null || fromSse == false) ? messageClause : messageSseClause);
		}

		// Add recorded/realtime filtering if needed
		if (needRealtimeClause(params) && ! secondHalf)
		{
			sqlWhere = addToWhere(sqlWhere, generateRtClause(tableAbbrev,
                                                             isRealtime));
		}

        if ((vcids != null) && ! vcids.isEmpty() && ! secondHalf)
        {
            // Evr.vcid is nullable

            sqlWhere = addToWhere(sqlWhere,
                                  generateVcidWhere(vcids, tableAbbrev, true));
        }

        if ((dssIds != null) && ! dssIds.isEmpty() && ! secondHalf)
        {
            sqlWhere = addToWhere(sqlWhere,
                                  generateDssIdWhere(dssIds, tableAbbrev));
        }

		if (sqlWhere == null){
		    sqlWhere = "";
		}

		return (sqlWhere);
	}


    /**
     * Turn time-range into where-clause segment.
     *
     * @param range Time range
     * @param fsw   True if FSW
     * @param extended True if extended tables are in use
     *
     * @return Segment as string
     *
     * @version MPCS-8384  Extended support
     */
	@Override
	public String getTimeRangeClause(final DatabaseTimeRange range,
                                            final boolean           fsw,
                                            final boolean           extended)
    {
        final StringBuilder sb = new StringBuilder(512);

        final String where = DbTimeUtility.generateTimeWhereClause(
                                 fsw ? tableAbbrev : tableSseAbbrev,
                                 range,
                                 false,
                                 false,
                                 extended);

        if (where.length() > 0)
        {
            sb.append(" AND ").append(where);
        }

        return sb.toString();
	}


	private String getOrderBy(final DatabaseTimeRange range,
                              final Object...         params)
    {
	    IDbOrderByType order = (IDbOrderByType)params[7];
	    
	  //add the proper time clause and order by the time type
        //if no ordering has been specified
        switch(range.getTimeType().getValueAsInt())
        {
            case DatabaseTimeType.SCET_TYPE:
                if(order == null)
                {
                    order = EvrOrderByType.SCET;
                }
                break;

			case DatabaseTimeType.LST_TYPE:
			    if(order == null)
                {
                    order = EvrOrderByType.LST;
                }
				break;
				
            case DatabaseTimeType.ERT_TYPE:
                if(order == null)
                {
                    order = EvrOrderByType.ERT;
                }
                break;

            case DatabaseTimeType.RCT_TYPE:
                if(order == null)
                {
                    order = EvrOrderByType.RCT;
                }
                break;

            case DatabaseTimeType.SCLK_TYPE:
                if(order == null)
                {
                    order = EvrOrderByType.SCLK;
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid database time type \"" + range.getTimeType().toString() + "\" received by " + this.getClass().getName());
        }

        // Add the ordering

        final StringBuilder sb = new StringBuilder();

        sb.append(' ').append(order.getOrderByClause());

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
		
		final String name = (String)params[0];
		final Long eventId = (Long)params[1];
		final String level = (String)params[2];
		final String module = (String)params[3];
		final String message = (String)params[4];
        // final Boolean fromSse = (Boolean)params[5];
        // final Boolean isRealtime = (Boolean)params[6];

		try
		{
			int i = 1;

            QueryClauseType clauseType = null;

            // Force because pre-query gives us the sessions

            clauseType = QueryClauseType.NO_JOIN;

            // The combination of FORWARD_ONLY, CONCUR_READ_ONLY, and a fetch size
			// of Integer.MIN_VALUE signals to the MySQL JDBC driver to stream the
			// results row by row rather than trying to load the whole result set
			// into memory. Any other settings will result in a heap overflow. Note
			// that this will probably break if we change database vendors.

            /** MPCS-8384 Adapted to support the extended tables. */

            final String selectClause = queryConfig.getQueryClause(
                                            clauseType,
                                            getActualTableName(IEvrLDIStore.DB_EVR_DATA_TABLE_NAME));
            int clauses = 1;
            
            String sqlClause = null;

            /** MPCS-8384  Adapted to support the extended tables. */
            /** MPCS-8639  Moved config item */

            final String rangeClause    = getTimeRangeClause(range, true,   _extendedDatabase);
            final String rangeSseClause = getTimeRangeClause(range, false,  _extendedDatabase);

            if (doUnion(params))
            {
                // KLUDGE to tell getSqlWhereClause we're on the SSE half of the union

                final Object[] extendParams =
                    (params.length < 9) ? new Object[params.length + 1]
                                        : new Object[params.length];

                Arrays.fill(extendParams, null);

                System.arraycopy(params, 0, extendParams, 0, params.length);

                extendParams[8] = true;

                sqlClause = "(" + selectClause + whereClause + rangeClause
                        + ") UNION ALL ("
                        + sseEvrFetch.getSqlSelectClause(clauseType)
                        + sseEvrFetch.getSqlWhereClause(range, tsi, extendParams)
                        + rangeSseClause
                        + ")";
                clauses++;

            }
            else if (doFsw(params))
            {
                // FSW only

                sqlClause = selectClause + whereClause + rangeClause;

            }
            else
            {
                // SSE only
                sqlClause = sseEvrFetch.getSqlSelectClause(clauseType) +
                            sseEvrFetch.getSqlWhereClause(range, tsi, params) +
                            rangeSseClause;
            }

            final StringBuilder sb = new StringBuilder(sqlClause);

            sb.append(getOrderBy(range, params));

            this.statement = getPreparedStatement(
                                 sb.toString(),
                                 ResultSet.TYPE_FORWARD_ONLY,
                                 ResultSet.CONCUR_READ_ONLY);

			this.statement.setFetchSize(Integer.MIN_VALUE);
			
			for (int j = 0; j < clauses; j++)
            {
    			if(name != null)
    			{
    				this.statement.setString(i++, name);
    			}
    
    			//set the event ID in the query
    			if(eventId != null)
    			{
    				this.statement.setLong(i++, eventId.longValue());
    			}
    
    			//set the task in the query
    			if(module != null)
    			{
    				this.statement.setString(i++, module);
    			}
    
    			//set the level in the query
    			if(level != null)
    			{
    				this.statement.setString(i++, level);
    			}
    
    			//set the message in the query
    			if(message != null)
    			{
    				this.statement.setString(i++, message);
    			}
    
    			//set the SSE value in the query
//    			if(fromSse != null)
//    			{
//                  this.statement.setInt(
//                      i++,
//                      GDR.getIntFromBoolean(fromSse.booleanValue()));
//    			}
            }

			if (this.printStmtOnly) {
				printSqlStatement(this.statement);
			}
			else {
				//execute the select query
				this.results = this.statement.executeQuery();
			}
			
		}
		catch(final SQLException e)
		{
			throw new DatabaseException("Error retrieving EVRs from database: " + e.getMessage());
		}

		this.isFirst = true;
		return (getResults());
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
    public List<IDbEvrProvider> getNextResultBatch() throws DatabaseException
	{
		this.isFirst = false;
		return (getResults());
	}

	/**
	 * This is the internal class method that keeps track of the JDBC ResultSet returned by
	 * a query.  Every call to this method will return a list of test configurations that match
	 * the original query.  The size of the returned lists is determined by the batch size that was
	 * entered when the query was made.  When there are no more results, this method will return an
	 * empty list.
	 *
	 * @return The list of EVRs that is part of the results of a query executed using the
	 * "getEvrs(...)" methods.  When an empty list is returned, it means no more results are left.
	 */
	@Override
    protected List<IDbEvrProvider> getResults() throws DatabaseException
	{
        final List<IDbEvrProvider> refs = new ArrayList<IDbEvrProvider>(1024);

		if(this.results == null)
		{
			return (refs);
		}

		/*
		 * This retrieval is trickier than the others. The select query does a left join
		 * on the EVR and EVR metadata database tables, so each EVR ID will show up once for
		 * each piece of metadata it has.
		 *
		 * For instance, if EVR with ID=1 has the key/value pairs (key1,value1), (key2,value2), and (key3,value3), then
		 * that EVR alone will return three rows from our query.  So when grabbing results, we don't have a new
		 * EVR on each call to results.next(), but instead we have a new EVR result when the current EVR ID does
		 * match the the ID of the EVR we get out of the result set.
		 *
		 * Because we have to read the next EVR out before knowing if its ID is the same as or different from the
		 * current EVR ID, we need to be able to store the keys and EVR in this object until the user comes back
		 * to get more results.
		 */

		try
		{
            /** MPCS-6718 Look for warnings */

            final List<SQLWarning> warnings = new ArrayList<SQLWarning>();

			//loop through until we fill up our first batch or we've
			//got no more results
			while(refs.size() < this.batchSize)
			{
				//if this is the first trip to this method after a query, we need to grab
				//the first result from the result set
				if(this.isFirst)
				{
					this.isFirst = false;
					if(this.results.next() == false)
					{
						break;
					}
				}

                // Read the unique evr ID from the next row in the results
                this.nextKey = new KeyClass(spf, this.results);

                // If the next key matches our current key, then the result
                // row is just another metadata keyword/value pair for our
                // current EVR

				if (this.nextKey.equals(this.currentKey))
				{
                    /*final String keyword =
                        this.results.getString(metadataTableAbbrev + ".keyword");
                    final String value   =
                        this.results.getString(metadataTableAbbrev + ".value");
                    */
				    final String keyword =
                        this.results.getString("keyword");
                    final String value   =
                        this.results.getString("value");
                    
                    currentEvr.addKeyValue(keyword, value);
				}
				else
				{
                    //if the next key doesn't match our current key, then the result row is a new
                    //EVR row

					// add the current EVR to the output list (if there is one)
					if (this.currentEvr != null)
					{
						refs.add(this.currentEvr);
					}

					//create a new EVR for the new row we just read
                    this.currentEvr = dbEvrFactory.createQueryableUpdater();
					//this.currentEvr.setEventId(this.results.getLong(tableAbbrev + ".eventId"));
					this.currentEvr.setEventId(this.results.getLong("eventId"));
					//this.currentEvr.setName(this.results.getString(tableAbbrev + ".name"));
					this.currentEvr.setName(this.results.getString( "name"));

                    long coarse = this.results.getLong("ertCoarse");
                    int  fine   = this.results.getInt("ertFine");

                    this.currentEvr.setErt(
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

                    /** MPCS-8384 */
                    currentEvr.setScet(
                        new AccurateDateTime(DbTimeUtility.exactFromScetCoarseFine(coarse, fine),
                                             DbTimeUtility.fineFromScetFine(fine)));

                    currentEvr.setRct(DbTimeUtility.dateFromCoarseFine(
                                          this.results.getLong("rctCoarse"),
                                          this.results.getInt("rctFine")));


                    final long coarseSclk = this.results.getLong("sclkCoarse");
                    final long fineSclk   = this.results.getLong("sclkFine");
                    final ISclk sclk       = new Sclk(coarseSclk, fineSclk);

                    currentEvr.setSclk(sclk);

                    // MPCS-4839 Verify and log and correct
                    // MPCS-6349 : DSS ID not set properly
                    currentEvr.setRecordDssId(getCorrectedStation(results, "dssId"));

                    Integer vcid = this.results.getInt("vcid");

                    if (results.wasNull() || (vcid < 0))
                    {
                        vcid = null;
                    }

                    currentEvr.setVcid(vcid);

                    /** MPCS-6809 Treat as unsigned */
                    /** MPCS-7639  Use new method */
                    /** MPCS-5935  Use holder */

                    try
                    {
                        warnings.clear();

                        this.currentEvr.setPacketId(PacketIdHolder.getFromDb(results, "packetId", warnings));

                        SqlExceptionTools.logWarning(warnings, trace);
                    }
                    catch (final HolderException he)
                    {
                        throw new DatabaseException("Problem reading packet column", he);
                    }

					//this.currentEvr.setLevel(this.results.getString(tableAbbrev + ".level"));
					this.currentEvr.setLevel(this.results.getString( "level"));
					//this.currentEvr.setModule(this.results.getString(tableAbbrev + ".module"));
					this.currentEvr.setModule(this.results.getString( "module"));
					//this.currentEvr.setMessage(this.results.getString(tableAbbrev + ".message"));
					this.currentEvr.setMessage(this.results.getString( "message"));
					//this.currentEvr.setFromSse(GDR.getBooleanFromInt(this.results.getInt(tableAbbrev + ".fromSse")));
					this.currentEvr.setFromSse(GDR.getBooleanFromInt(this.results.getInt( "fromSse")));
					//this.currentEvr.setIsRealtime(GDR.getBooleanFromInt(this.results.getInt(tableAbbrev + ".isRealtime")));
					this.currentEvr.setIsRealtime(GDR.getBooleanFromInt(this.results.getInt( "isRealtime")));

                    final int  hostId    = results.getInt(HOST_ID);
                    final long sessionId = this.results.getLong(SESSION_ID);
					
					this.currentEvr.setSessionId(sessionId);

                    /** MPCS-6718  Look for warnings */

                    warnings.clear();

                    currentEvr.setSessionFragment(
                        SessionFragmentHolder.getFromDbRethrow(results, FRAGMENT_ID, warnings));

                    SqlExceptionTools.logWarning(warnings, trace);

					this.currentEvr.setSessionHost(getSessionHost(spf, results));

                    final VenueType vt = spf.lookupVenue(hostId, currentEvr.getSessionId());

                    if (missionProps.getVenueUsesSol(vt) && TimeProperties.getInstance().usesLst())
                    {
                        currentEvr.setLst(
                        		LocalSolarTimeFactory.getNewLst(currentEvr.getScet(), spf.lookupSCID(hostId, currentEvr.getSessionId())));
                    }
	                
					// Grab the first metadata key/value pair for the new EVR we just read
                    // It need not have one.

					//final String keyword = this.results.getString(metadataTableAbbrev + ".keyword");
					final String keyword = this.results.getString( "keyword");
					//final String value   = this.results.getString(metadataTableAbbrev + ".value");
					final String value   = this.results.getString( "value");

                    if ((keyword != null) || (value != null))
                    {
                        this.currentEvr.addKeyValue(keyword, value);
                    }
				}

                // Handle any unhandled warnings
                /** MPCS-6718  */
                SqlExceptionTools.logWarning(trace, results);

				//set the keys equal before the next result read
				this.currentKey = this.nextKey;

				// this actually advances the iterator to the next result
				// AND checks if there is a next result at the same time
				if(this.results.next() == false)
				{
					break;
				}
			}

			//If we have a current EVR and we've gone past the end of the result, then tag
			//that EVR onto the end of our returned list (this doesn't go perfectly with the
			//batch size idea, but potentially adding one extra result on the last query shouldn't
			//hurt anything)...close up the resources too when we're done
			if(this.currentEvr != null && this.results.isAfterLast() == true)
			{
				refs.add(this.currentEvr);

				this.results.close();
				this.results = null;

                this.currentEvr = null;
                this.currentKey = null;
			}
		}
		catch(final SQLException e)
		{
			throw new DatabaseException("Error getting EVRs from database: " + e.getMessage());
		}

		return (refs);
	}

	@Override
    public Map<String,Integer> countEvrsByLevel(
                                   final IDbContextInfoProvider testSession)
        throws DatabaseException
	{
		if (! dbProperties.getUseDatabase())
		{
			return (new HashMap<String,Integer>(0));
		}

		if (! isConnected())
		{
			throw new IllegalStateException(
                          "This connection has already been closed");
		}

        final List<Long> keys = testSession.getSessionKeyList();
        final int        size = keys.size();

		if (size == 0)
		{
			throw new IllegalArgumentException("There are no test keys " +
                                               "available to query for");
		}
		else if (size > 1)
		{
			throw new IllegalArgumentException("There can only be one test "   +
                                               "key for a summary query, but " +
                                               "there are "                    +
                                               size);
		}

        final HashMap<String,Integer> map = new HashMap<String,Integer>();

		try
		{
			this.statement = getPreparedStatement(levelSummaryQuery);

			this.statement.setLong(1, keys.get(0));

			if (this.printStmtOnly)
            {
				printSqlStatement(this.statement);
			}
			else
            {
                ResultSet result = null;

                try
                {
                    result = this.statement.executeQuery();

                    while (result.next())
                    {
                        final String level = result.getString("level");
                        final int count = result.getInt("number");
                        map.put(level,Integer.valueOf(count));
                    }
                }
                finally
                {
                    if (result != null)
                    {
                        result.close();
                    }
                }
			}
		}
		catch (final SQLException e)
		{
			final SQLException se =
                new SQLException("Error getting EVRs from database: " + e);

            se.initCause(e);

            throw new DatabaseException(se);
		}
        finally {
            if (this.statement != null) {
                try {
                    this.statement.close();
                }
                catch (final SQLException e) {
                    // ignore
                }
                this.statement = null;
            }
        }

        return map;
	}


    /**
     * Holds key (sessionId, sessionHost, fromSse, sessionFragment, id)
     * that distinguishes one EVR from another. We are using the equals
     * method primarily.
     *
     * To keep things orderly use the same order as EvrOrderByType.BASE_ORDER.
     *
     * MPCS-5990  Redo for fragment,
     */
    private static class KeyClass
        extends Quintuplet<Long, String, Boolean, Integer, BigInteger>
    {
		private static final long serialVersionUID = 1L;


		/**
         * Constructor from current result set.
         *
         * @param spf
         *            the Session Pre-Fetch Object
         * @param results
         *            the Session Pre-Fetch result set to query for key
         *            information
         *
         * @throws DatabaseException
         *             if a SQL error occurs querying the result set
         */
        public KeyClass(final IDbSessionPreFetch spf, final ResultSet results) throws DatabaseException {
            super(getLong(results, SESSION_ID), getSessionHost(spf, results), (getInt(results, "fromSse") != 0),
                  getInt(results, "sessionFragment"), toBigInteger(getBigDecimal(results, "id")));
        }

        private static int getInt(final ResultSet results, final String columnLabel) throws DatabaseException {
            try {
                return results.getInt(columnLabel);
            }
            catch (final SQLException se) {
                throw new DatabaseException(se);
            }
        }

        private static long getLong(final ResultSet results, final String columnLabel) throws DatabaseException {
            try {
                return results.getLong(columnLabel);
            }
            catch (final SQLException se) {
                throw new DatabaseException(se);
            }
        }

        private static BigDecimal getBigDecimal(final ResultSet results, final String columnLabel)
                throws DatabaseException {
            try {
                return results.getBigDecimal(columnLabel);
            }
            catch (final SQLException se) {
                throw new DatabaseException(se);
            }
        }
    }

    private static BigInteger toBigInteger(final BigDecimal bd)
        throws DatabaseException
    {
        /** MPCS-5990 New */

        // Very unlikely to happen

        try
        {
            return bd.toBigIntegerExact();
        }
        catch (final ArithmeticException ae)
        {
            throw new DatabaseException(ae);
        }
    }


    private static String getSessionHost(final IDbSessionPreFetch spf,
                                         final ResultSet       results)
        throws DatabaseException
    {
        try {
            final int    hostId      = results.getInt(HOST_ID);
            final String sessionHost = spf.lookupHost(hostId);
    
            if ((sessionHost == null) || (sessionHost.length() == 0))
            {
                throw new DatabaseException("Unable to get sessionHost for " +
                                       " hostId "                       +
                                       hostId);
            }
    
            return sessionHost;
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


    /**
     * Generate real-time clause.
     *
     * @param abbrev   Table abbreviation
     * @param realTime True is real-time, else recorded
     *
     * @return Clause as string
     */
    private static String generateRtClause(final String  abbrev,
                                           final boolean realTime)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append('(');

        sb.append(abbrev).append(".isRealtime ");
        sb.append(realTime ? "!= 0" : "= 0");

        sb.append(')');

        return sb.toString();
    }
}
