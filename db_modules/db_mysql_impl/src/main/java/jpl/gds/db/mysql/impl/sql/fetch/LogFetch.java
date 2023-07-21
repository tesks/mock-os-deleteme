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
import java.util.List;
import java.util.Set;

import jpl.gds.shared.string.StringUtil;
import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbContexConfigPreFetch;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.ILogFetch;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.db.api.types.IDbLogFactory;
import jpl.gds.db.api.types.IDbLogProvider;
import jpl.gds.db.api.types.IDbLogUpdater;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.mysql.impl.sql.order.LogOrderByType;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.DbTimeUtility;


/**
 * This is the database read/retrieval interface to the LogMessage table in the
 * MPCS database.  This class will retrieve one or more log messages from the database
 * based on a number of different query input parameters.
 * 
 * The general way to use this class is:
 * <ol>
 * <li>Create a TestSessionInfo object and set all the information on it that should be used
 * to search the database for test sessions</li>
 * <li>Create any other necessary query values such as classification</li>
 * <li>Call one of the "getLogMessages(...)" methods and specify a batch size for the size
 * of lists that should be returned.  The "getLogMessages(...)" methods will return only
 * the first batch of results.</li>
 * <li>Make further calls to getNextResultBatch() to retrieve the rest of the results from
 * the query</li>
 *
 * @version MPCS-10119 - Added support for query by context
 */
public class LogFetch extends AbstractMySqlFetch implements ILogFetch
{
    // Templated WHERE clauses for restricting the query

    private final String classificationClause = queryConfig.getQueryClause(QueryClauseType.LOG_CLASSIFICATION_CONDITION, DB_LOG_MESSAGE_TABLE_NAME);;

    private final String messageClause = queryConfig.getQueryClause(QueryClauseType.MESSAGE_CONDITION, DB_LOG_MESSAGE_TABLE_NAME);

    private IDbSessionPreFetch spf = null;

	private IDbContexConfigPreFetch ctxPreFetch = null;

    private final IDbLogFactory dbLogFactory;

    private boolean useContext;

    private String[] classifications = null;

	/**
     * Creates an instance of LogMessageFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            The flag that indicates whether the fetch
     *            class should print out the SQL statement
     *            only or execute it.
     */
	public LogFetch(final ApplicationContext appContext, final boolean printSqlStmt)
	{
		super(appContext, printSqlStmt);
        dbLogFactory = appContext.getBean(IDbLogFactory.class);
	}

	/**
     * Creates an instance of LogMessageFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public LogFetch(final ApplicationContext appContext)
	{
		this(appContext, false);
	}

	@Override
	public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
	                                     final int batchSize, final Object... params)
			throws DatabaseException {
		return get(tsi, range, batchSize, false, params);
	}

	@Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, DatabaseTimeRange range, final int batchSize,
            boolean useContext, final Object... params) throws DatabaseException {
		this.useContext = useContext;
		if(tsi == null)
		{
			throw new IllegalArgumentException("Input test session information was null");
		}
		
		if(!this.dbProperties.getUseDatabase())
		{
            return (new ArrayList<IDbLogProvider>());
		}
		
		if(range == null)
		{
			range = new DatabaseTimeRange(DatabaseTimeType.EVENT_TIME);
		}
		
		initQuery(batchSize);

        // Create pre-fetch query and execute
	    String whereClause = "";
	    String contextClause = "";
	    String sessionClause = "";

	    spf = fetchFactory.getSessionPreFetch(false);
	    ctxPreFetch = fetchFactory.getContextConfigPreFetch();

        try {
            //when we have session ID, session + context, or none, join with Session table
	        if(!useContext){
		        if (printStmtOnly) {
			        // Dummy to get SQL statement printed
			        fetchFactory.getSessionPreFetch(true).get(tsi);
		        }
		        spf.get(tsi);
		        whereClause = spf.getIdHostWhereClause(DB_LOG_MESSAGE_TABLE_ABBREV);
		        contextClause = createContextIdClause(tsi.getParentKeyList(), DB_LOG_MESSAGE_TABLE_ABBREV, true);
		        whereClause = addToWhere(whereClause, contextClause);
	        }
	        //when we only have context ID, join with Context table
	        else{
		        if (printStmtOnly) {
			        // Dummy to get SQL statement printed
			        fetchFactory.getContextConfigPreFetch(true).get(tsi);
		        }
	        	ctxPreFetch.get(tsi);
		        whereClause = ctxPreFetch.getIdHostWhereClause(DB_LOG_MESSAGE_TABLE_ABBREV);
		        //MPCS-10431 -  Support session IDs in where clause
		        sessionClause = createContextIdClause(tsi.getParentKeyList(), DB_LOG_MESSAGE_TABLE_ABBREV, false);
		        whereClause = addToWhere(whereClause, sessionClause);
	        }


	        // Split classification levels
			String classification = (String)params[0];
			if (classification != null) {
				this.classifications = classification.split(",");
			}
            whereClause = getSqlWhereClause(whereClause, range, params);
        } finally {
            spf.close();
            ctxPreFetch.close();
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
		final String classification = (String)params[0];
		@SuppressWarnings("unchecked")
		final Set<String> logTypes = (Set<String>)params[1];
		final String message = (String)params[2];
		IDbOrderByType orderType = (IDbOrderByType)params[3];

		if(testSqlTemplate == null)
		{
			throw new IllegalArgumentException("Input test information template was null");
		}
		
		StringBuilder sqlWhere = new StringBuilder();

		if(!testSqlTemplate.equals(""))
		{
			sqlWhere = addToWhere(sqlWhere,testSqlTemplate);
		}
		
		//add classification filtering if needed
		if(this.classifications != null)
		{
			//MPCS-11340 - Update to support all log classifications
			sqlWhere = addClassificationToWhere(sqlWhere, this.classifications);
		}

        if ((logTypes != null) && ! logTypes.isEmpty())
        {
			sqlWhere = addToWhere(sqlWhere,logTypesWhereClause(logTypes));
        }
		
		//add message filtering if needed
		if(message != null)
		{
			sqlWhere = addToWhere(sqlWhere,messageClause);
		}
		
		// Add the time filtering

        /** MPCS-8384 Extended support */
        final String timeWhere =
            DbTimeUtility.generateTimeWhereClause(
                                     DB_LOG_MESSAGE_TABLE_ABBREV,
                range,
                false,
                false,
                _extendedDatabase);

		if (timeWhere.length() > 0)
        {
			sqlWhere = addToWhere(sqlWhere, timeWhere);
		}

		//MPCS-10768 - Add session fragment, only if not null
		if(params.length >= 5 && params[4] != null) {
			sqlWhere = addToWhere(sqlWhere, DB_LOG_MESSAGE_TABLE_ABBREV + "." + FRAGMENT_ID + "=" + params[4]);
		}
		
		//add the ordering
		if(orderType == null)
		{
			orderType = LogOrderByType.getDefaultValue();
		}
		
		// Add the ordering
        sqlWhere.append(' ').append(orderType.getOrderByClause());

		return sqlWhere.toString();
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
		
		final String classification = (String)params[0];
		final String message = (String)params[2];
		
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
			final String selectClause = queryConfig.getQueryClause(clauseType, ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME);
			this.statement = getPreparedStatement(
                                 selectClause + whereClause,
					             ResultSet.TYPE_FORWARD_ONLY,
                                 ResultSet.CONCUR_READ_ONLY);

			this.statement.setFetchSize(Integer.MIN_VALUE);

			//set the classification in the query
			if (this.classifications != null) {
				// MPCS-11340 - Update to support all log classifications
				for (String level: this.classifications) {
					this.statement.setString(i++, level);
				}
			}

			//set the message in the query
			if(message != null)
			{
				this.statement.setString(i++,message);
			}

			if (this.printStmtOnly) {
				printSqlStatement(this.statement);
			}
			else {
				//execute the select query
				this.results = this.statement.executeQuery();
			}
			
		}
		catch (final SQLException e)
		{
			throw new DatabaseException("Error retrieving log messages from " +
                                       "database: "                      +
                                       e.getMessage(),
                                   e);
		}
		
		return getResults();
	}


    /**
     * {@inheritDoc}
     */
    @Override	
    public List<IDbLogProvider> getNextResultBatch() throws DatabaseException
	{
		return(getResults());
	}
	
	/**
     * This is the internal class method that keeps track of the JDBC ResultSet returned by
     * a query. Every call to this method will return a list of test configurations that match
     * the original query. The size of the returned lists is determined by the batch size that was
     * entered when the query was made. When there are no more results, this method will return an
     * empty list.
     * 
     * @return The list of log messages that is part of the results of a query executed using the
     *         "getLogMessages(...)" methods. When an empty list is returned, it means no more results are left.
     * 
     * @throws DatabaseException
     *             If there happens to be an exception retrieving the next set of results
     */
	@Override
    protected List<IDbLogProvider> getResults() throws DatabaseException
	{
        final List<IDbLogProvider> refs = new ArrayList<IDbLogProvider>(this.batchSize);
		
		if(this.results == null)
		{
			return(refs);
		}
		
		int count = 0;
		try
		{
            /** MPCS-6718  Look for warnings */

            final List<SQLWarning> warnings = new ArrayList<SQLWarning>();

			//loop through until we fill up our first batch or we've
			//got no more results
			while(count < this.batchSize)
			{
				if(!this.results.next())
				{
					break;
				}
				
                final IDbLogUpdater msg = dbLogFactory.createQueryableUpdater();
				
				//pull out all the information for this log message and add it
				//to the return list

                msg.setEventTime(
                    DbTimeUtility.dateFromCoarseFine(
                        this.results.getLong(DB_LOG_MESSAGE_TABLE_ABBREV + ".eventTimeCoarse"),
                        this.results.getInt(DB_LOG_MESSAGE_TABLE_ABBREV + ".eventTimeFine")));

                msg.setRct(
                    DbTimeUtility.dateFromCoarseFine(
                        this.results.getLong(DB_LOG_MESSAGE_TABLE_ABBREV + ".rctCoarse"),
                        this.results.getInt(DB_LOG_MESSAGE_TABLE_ABBREV + ".rctFine")));

				msg.setMessage(this.results.getString(DB_LOG_MESSAGE_TABLE_ABBREV + ".message"));
				
				final String classification = this.results.getString(DB_LOG_MESSAGE_TABLE_ABBREV + ".classification");
				msg.setClassification(classification);
				msg.setSeverity(TraceSeverity.fromStringValue(classification));
				
				final String type = this.results.getString(DB_LOG_MESSAGE_TABLE_ABBREV + ".type");
				msg.setType(LogMessageType.fromStringValue(type));

                final long sessionId = this.results.getLong(DB_LOG_MESSAGE_TABLE_ABBREV + "." + SESSION_ID);
				final long contextId = this.results.getLong(DB_LOG_MESSAGE_TABLE_ABBREV + "." + CONTEXT_ID);
				
				msg.setSessionId(sessionId);
				msg.setContextId(contextId);

                /** MPCS-6718  Look for warnings */

                warnings.clear();

                msg.setSessionFragment(
                    SessionFragmentHolder.getFromDbRethrow(
                        results,
                        DB_LOG_MESSAGE_TABLE_ABBREV + "." + FRAGMENT_ID,
                        warnings));

                SqlExceptionTools.logWarning(warnings, trace);

                final int hostId      = results.getInt(DB_LOG_MESSAGE_TABLE_ABBREV + "." + HOST_ID);
                final int contextHostId = results.getInt(DB_LOG_MESSAGE_TABLE_ABBREV + "." + CONTEXT_HOST_ID);
                //context or session host
                final String sessionHost = useContext ? ctxPreFetch.lookupHost(contextHostId) : spf.lookupHost(hostId);

                if ((sessionHost == null) || (sessionHost.length() == 0))
                {
                    throw new DatabaseException("Unable to get sessionHost for " + " hostId " + hostId);
                }

				msg.setSessionHost(sessionHost);

				refs.add(msg);
				count++;

                // Handle any unhandled warnings
                /** MPCS-6718  */
                SqlExceptionTools.logWarning(trace, results);
			}
			
			//if we're all done with resuls, clean up
			//all the resources
			if(this.results.isAfterLast())
			{
				this.results.close();
				this.statement.close();
				
				this.results = null;
				this.statement = null;
			}
		}
		catch(final SQLException e)
		{
			throw new DatabaseException("Error retrieving log messages from database: " + e.getMessage());
		}
		
		return(refs);
	}


    /**
     * Compute where clause segment for desired log types
     *
     * @param logTypes
     *
     * @return Where clause segment
     */
    private String logTypesWhereClause(final Set<String> logTypes)
    {
        if ((logTypes == null) || logTypes.isEmpty())
        {
            return "";
        }

        final StringBuilder sb    = new StringBuilder("(");
        boolean             first = true;

        for (final String next : logTypes)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(" OR ");
            }

            sb.append("(");

            sb.append(DB_LOG_MESSAGE_TABLE_ABBREV).append(".type LIKE ");
            sb.append("'%").append(next).append("%'");

            sb.append(")");
        }

        sb.append(")");

        return sb.toString();
    }

	/**
	 * Adds classification conditions (based on classification list provided) to the given where
	 * clause, and returns the new where clause.
	 *
	 * @param oldWhere  the existing where clause
	 * @param classifications a string array of classifications to add to where clause
	 * MPCS-11340 - 11/20/19 - Update to support all log classifications
	 * @return the new where clause
	 */
	private StringBuilder addClassificationToWhere(final StringBuilder oldWhere,
		final String[] classifications) {

		if (classifications.length < 1) {
			return oldWhere;
		}

		final StringBuilder oldW = ((oldWhere != null) ? oldWhere : new StringBuilder());
		final String        newC = StringUtil.safeTrim(classificationClause);
		boolean             first = true;

		oldW.append(" AND ").append("(");
		for (String level : classifications) {
			if (first) {
				first = false;
			} else {
				oldW.append(" OR ");
			}
			oldW.append(newC);
		}
		oldW.append(")");
		return oldW;
	}
}
