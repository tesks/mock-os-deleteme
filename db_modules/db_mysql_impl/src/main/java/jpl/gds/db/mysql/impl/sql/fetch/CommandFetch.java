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
import java.util.Collection;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.ICommandFetch;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.store.ldi.ICommandMessageLDIStore;
import jpl.gds.db.api.types.CommandType;
import jpl.gds.db.api.types.IDbCommandFactory;
import jpl.gds.db.api.types.IDbCommandProvider;
import jpl.gds.db.api.types.IDbCommandUpdater;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.mysql.impl.sql.order.CommandOrderByType;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.tc.api.CommandStatusType;


/**
 * This is the database read/retrieval interface to the HardwareCommandMessage table in the
 * MPCS database.  This class will retrieve one or more command messages from the database
 * based on a number of different query input parameters.
 *
 * The general way to use this class is:
 * <ol>
 * <li>Create a TestSessionInfo object and set all the information on it that should be used
 * to search the database for test sessions</li>
 * <li>Create any other necessary query values such as commandType</li>
 * <li>Call one of the "getCommandMessages(...)" methods and specify a batch size for the size
 * of lists that should be returned.  The "getCommandMessages(...)" methods will return only
 * the first batch of results.</li>
 * <li>Make further calls to getNextResultBatch() to retrieve the rest of the results from
 * the query</li>
 *
 */
public class CommandFetch extends AbstractMySqlFetch implements ICommandFetch
{


    /** Final column name */
    private final String            FINAL      = COMMAND_STATUS_TABLE_ABBREV + ".final";

    /** RequestId column name */
    private final String REQUEST_ID = COMMAND_MESSAGE_TABLE_ABBREV + ".requestId";

    /** Status column name */
    private final String STATUS = COMMAND_STATUS_TABLE_ABBREV + ".status";

    /** Status column name for last-status-only */
    private final String STATUS2 = COMMAND_STATUS_TABLE_ABBREV_1 + ".status";

	//Templated WHERE clauses for restricting the query

	private final String commandStringClause;

	private final String commandTypeClause;

    private IDbSessionPreFetch spf = null;

    /** Last query executed */
    private String lastQuery = null;

    private final IDbCommandFactory dbCommandFactory;


	/**
     * Creates an instance of CommandMessageFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            The flag that indicates whether the fetch
     *            class should print out the SQL statement
     *            only or execute it.
     */
	public CommandFetch(final ApplicationContext appContext, final boolean printSqlStmt)
	{
		super(appContext, printSqlStmt);
		
        dbCommandFactory = appContext.getBean(IDbCommandFactory.class);

		commandStringClause =
				queryConfig.getQueryClause(QueryClauseType.MESSAGE_CONDITION, DB_TABLE);

		commandTypeClause =
				queryConfig.getQueryClause(QueryClauseType.COMMAND_TYPE_CONDITION,
						DB_TABLE);
	}

	/**
     * Creates an instance of CommandMessageFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public CommandFetch(final ApplicationContext appContext)
	{
		this(appContext, false);
	}


    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.ICommandFetch#getLastQuery()
	 */
    @Override
	public String getLastQuery()
    {
        return lastQuery;
    }

    /**
     * {@inheritDoc}
	 */
    @Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, DatabaseTimeRange range, final int batchSize,
            final Object... params) throws DatabaseException {
		if ( tsi == null )
		{
			throw new IllegalArgumentException("Input test session information was null");
		}

		if(this.dbProperties.getUseDatabase() == false)
		{
            return (new ArrayList<IDbCommandProvider>());
		}

		if(range == null)
		{
			range = new DatabaseTimeRange(DatabaseTimeType.EVENT_TIME);
		}

		initQuery(batchSize);

        String whereClause = null;

        // Create pre-fetch query and execute

        if (printStmtOnly)
        {
            // Dummy to get SQL statement printed

            final IDbSessionPreFetch dummy = fetchFactory.getSessionPreFetch(true);

            try {
                dummy.get(tsi);
            } finally {
                dummy.close();
            }
        }

        // Must always run, even with printStmtOnly, to populate main query

        if (spf == null)
        {
            spf = fetchFactory.getSessionPreFetch(false);
        }

        try
        {
            spf.get(tsi);

            whereClause = getSqlWhereClause(
                              spf.getIdHostWhereClause(COMMAND_MESSAGE_TABLE_ABBREV), range, params);
        }
        finally
        {
            if (printStmtOnly)
            {
                spf.close();

                spf = null;
            }
        }

		return populateAndExecute(tsi, range, whereClause, params);
	}


	/**
     * {@inheritDoc}
	 */
    @Override
	public String getSqlWhereClause(final String testSqlTemplate,
                                       final DatabaseTimeRange range,
                                       final Object... params)
        throws DatabaseException
	{
		if(testSqlTemplate == null)
		{
			throw new IllegalArgumentException("Input test information template was null");
		}
		
		final String cmdString = (String)params[0];
        final CommandType cmdType = (CommandType) params[1];
		IDbOrderByType orderType = (IDbOrderByType)params[2];

        final Boolean finl = ((params.length >= 4) ? (Boolean) params[3] : null);

        final CommandStatusType statusType = ((params.length >= 5)
                                                  ? (CommandStatusType) params[4] : null);

        final Collection<String> requestId =
            SystemUtilities.<Collection<String>>castNoWarning(
                (params.length >= 6) ? params[5] : null);

        final Boolean lastStatusOnly = ((params.length >= 7)
                                            ? (Boolean) params[6] : null);

		String sqlWhere = null;

		if(!testSqlTemplate.equals(""))
		{
			sqlWhere = addToWhere(sqlWhere, testSqlTemplate);
		}

        if (finl != null)
        {
            sqlWhere = addToWhere(sqlWhere, finalClause(finl));
        }

        if (statusType != null)
        {
            sqlWhere = addToWhere(sqlWhere, statusClause(statusType));
        }

        if ((requestId != null) && ! requestId.isEmpty())
        {
            sqlWhere = addToWhere(sqlWhere, requestIdClause(requestId));
        }

        if ((lastStatusOnly != null) && lastStatusOnly)
        {
            sqlWhere = addToWhere(sqlWhere, lastStatusOnlyClause());
        }

		//add command string filtering if needed
		if(cmdString != null)
		{
			sqlWhere = addToWhere(sqlWhere, commandStringClause);
		}

		//add command type filtering if needed
		if(cmdType != null)
		{
			sqlWhere = addToWhere(sqlWhere, commandTypeClause);
		}
		
		// Add the time filtering
        /** MPCS-8384 Extended support */

        final String timeWhere =
            DbTimeUtility.generateTimeWhereClause(
                                     COMMAND_STATUS_TABLE_ABBREV,
                range,
                false,
                false,
                _extendedDatabase);

		if (timeWhere.length() > 0)
        {
			sqlWhere = addToWhere(sqlWhere, timeWhere);
		}

		// Add ordering

        if (orderType == null)
		{
			orderType = CommandOrderByType.DEFAULT;	// Default OrderBy
		}

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
		
		final String cmdString = (String)params[0];
        final CommandType cmdType = (CommandType) params[1];
        final Boolean lastStatusOnly = ((params.length >= 7)
                                            ? (Boolean) params[6] : null);

		try
		{
			int i = 1;

            QueryClauseType clauseType = null;

            // Force because pre-query gives us the sessions

            if ((lastStatusOnly == null) || ! lastStatusOnly)
            {
                clauseType = QueryClauseType.NO_JOIN;
            }
            else
            {
                clauseType = QueryClauseType.JOIN_SINGLE;
            }

			// The combination of FORWARD_ONLY, CONCUR_READ_ONLY, and a fetch size
			// of Integer.MIN_VALUE signals to the MySQL JDBC driver to stream the
			// results row by row rather than trying to load the whole result set
			// into memory. Any other settings will result in a heap overflow. Note
			// that this will probably break if we change database vendors.
			final String selectClause = queryConfig.getQueryClause(clauseType, ICommandMessageLDIStore.DB_COMMAND_MESSAGE_DATA_TABLE_NAME);
			this.statement = getPreparedStatement(
                                 selectClause + whereClause,
					             ResultSet.TYPE_FORWARD_ONLY,
                                 ResultSet.CONCUR_READ_ONLY);

			this.statement.setFetchSize(Integer.MIN_VALUE);

			//set the command string in the query
			if(cmdString != null)
			{
				this.statement.setString(i++,cmdString);
			}

			//set the command type in the query
			if(cmdType != null)
			{
				this.statement.setString(i++,cmdType.toString());
			}

			if (this.printStmtOnly) {
				printSqlStatement(this.statement);
			}
			else {
				//execute the select query

                lastQuery = this.statement.toString();

				this.results = this.statement.executeQuery();
			}
			
		}
		catch(final SQLException e)
		{
			throw new DatabaseException ( "Error retrieving command messages from database: " + e.getMessage() );
		}

		return ( getResults() );
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
    public List<IDbCommandProvider> getNextResultBatch() throws DatabaseException
	{
		return(getResults());
	}


    /**
     * Construct clause to check "final" status. Note that this
     * is no longer CommandMessage.finalized but now
     * CommandStatus.final.
     *
     * @param finl Select rows with final or non-final rows
     *
     * @return Portion of where clause
     */
    private String finalClause(final boolean finl)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append('(');
        sb.append(FINAL).append('=').append(finl ? '1' : '0');
        sb.append(')');

        return sb.toString();
    }


    /**
     * Construct clause to check "requestId".
     *
     * @param requestIds Request-ids desired
     *
     * @return Portion of where clause
     */
    private String requestIdClause(final Collection<String> requestIds)
    {
        final StringBuilder sb    = new StringBuilder();
        boolean             first = true;

        sb.append('(');
        sb.append(REQUEST_ID).append(" IN (");

        for (final String requestId : requestIds)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(',');
            }

            sb.append("'").append(requestId).append("'");
        }

        sb.append("))");

        return sb.toString();
    }



    /**
     * Construct clause to check "lastStatusOnly".
     *
     * @return Portion of where clause
     */
    private String lastStatusOnlyClause()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append('(');
        sb.append(STATUS2).append(" IS NULL");
        sb.append(')');

        return sb.toString();
    }


    /**
     * Construct clause to check "status" status.
     *
     * @param cst Status to look for
     *
     * @return Portion of where clause
     */
    private String statusClause(final CommandStatusType cst)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append('(');
        sb.append(STATUS).append("='").append(cst).append("'");
        sb.append(')');

        return sb.toString();
    }


	/**
     * This is the internal class method that keeps track of the JDBC ResultSet returned by
     * a query. Every call to this method will return a list of test configurations that match
     * the original query. The size of the returned lists is determined by the batch size that was
     * entered when the query was made. When there are no more results, this method will return an
     * empty list.
     *
     * @return The list of command messages that is part of the results of a query executed using the
     *         "getCommandMessages(...)" methods. When an empty list is returned, it means no more results are left.
     *
     * @throws DatabaseException
     *             If there happens to be an exception retrieving the next set of results
     */
	@Override
    protected List<IDbCommandProvider> getResults() throws DatabaseException
	{
        final List<IDbCommandProvider> refs = new ArrayList<IDbCommandProvider>(this.batchSize);

		if(this.results == null)
		{
			return ( refs );
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

                final IDbCommandUpdater command = dbCommandFactory.createQueryableUpdater();
				
				//pull out all the information for this command message and add it
				//to the return list

                command.setEventTime(
                    DbTimeUtility.dateFromCoarseFine(
                        this.results.getLong(COMMAND_STATUS_TABLE_ABBREV + ".eventTimeCoarse"),
                        this.results.getInt(COMMAND_STATUS_TABLE_ABBREV + ".eventTimeFine")));

                command.setRct(
                    DbTimeUtility.dateFromCoarseFine(
                        this.results.getLong(COMMAND_STATUS_TABLE_ABBREV + ".rctCoarse"),
                        this.results.getInt(COMMAND_STATUS_TABLE_ABBREV + ".rctFine")));

				final String ct =
                    StringUtil.safeTrim(
                        this.results.getString(COMMAND_MESSAGE_TABLE_ABBREV + ".type"));

				command.setType(
                    (ct.length() > 0) ? new CommandType(ct)
                                        : CommandType.UNKNOWN_COMMAND);

				command.setOriginalFile(this.results.getString(COMMAND_MESSAGE_TABLE_ABBREV + ".originalFile"));
				command.setScmfFile(this.results.getString(COMMAND_MESSAGE_TABLE_ABBREV + ".scmfFile"));

				final String commandString = this.results.getString(COMMAND_MESSAGE_TABLE_ABBREV + ".message");
				command.setCommandString(commandString);
				
				switch(command.getType().getValueAsInt())
				{
                    case CommandType.HARDWARE_COMMAND_TYPE:
                    case CommandType.FLIGHT_SOFTWARE_COMMAND_TYPE:
                    case CommandType.SSE_COMMAND_TYPE:
                    case CommandType.RAW_UPLINK_DATA_TYPE:
                    case CommandType.SEQUENCE_DIRECTIVE_TYPE:
                    case CommandType.FILE_LOAD_TYPE:
                    case CommandType.SCMF_TYPE:
                    case CommandType.FILE_CFDP_TYPE:
                    case CommandType.CLTU_F_TYPE:
						break;

                    case CommandType.UNKNOWN_COMMAND_TYPE:
					default:

						trace.warn("Unrecognized command message type " + command.getType().getValueAsString() + " received by command message fetch.");
						continue;
				}

                final long sessionId =
                               this.results.getLong(COMMAND_MESSAGE_TABLE_ABBREV + "." + SESSION_ID);

				command.setSessionId(sessionId);

                final int    hostId      = results.getInt(
                                               COMMAND_MESSAGE_TABLE_ABBREV + "." + HOST_ID);
                final String sessionHost = spf.lookupHost(hostId);

                if ((sessionHost == null) || (sessionHost.length() == 0))
                {
                    throw new DatabaseException("Unable to get sessionHost for " +
                                           " hostId "                       +
                                           hostId);
                }
                
				command.setSessionHostId(hostId);

                command.setSessionHost(sessionHost);

                /** MPCS-6718  Look for warnings */

                warnings.clear();

                // Some users need this, such as UplinkStatusPublisher
                command.setSessionFragment(SessionFragmentHolder.getFromDbRethrow(results,
                                                                                  COMMAND_MESSAGE_TABLE_ABBREV +
                                                                                      "."     +
                                                                                      FRAGMENT_ID,
                                                                                  warnings));
                SqlExceptionTools.logWarning(warnings, trace);

				command.setFailReason(this.results.getString(
                                          COMMAND_STATUS_TABLE_ABBREV + ".failReason"));

				command.setCommandedSide(
                    this.results.getString(COMMAND_MESSAGE_TABLE_ABBREV + ".commandedSide"));

				command.setFinalized(
                    this.results.getInt(COMMAND_MESSAGE_TABLE_ABBREV + ".finalized") != 0);

				command.setFinal(
                    this.results.getInt(COMMAND_STATUS_TABLE_ABBREV + ".final") != 0);

				command.setRequestId(results.getString(COMMAND_MESSAGE_TABLE_ABBREV + ".requestId"));

				final String temp = StringUtil.emptyAsNull(results.getString(COMMAND_STATUS_TABLE_ABBREV + ".status"));

                if (temp != null)
                {
                    try
                    {
                       command.setStatus(CommandStatusType.valueOf(temp));
                    }
                    catch (final IllegalArgumentException iae)
                    {
                        command.setStatus(CommandStatusType.UNKNOWN);

                        trace.warn("Unknown CommandStatus.status: '" + temp + "'");
                    }
                }
                else
                {
                    command.setStatus(CommandStatusType.UNKNOWN);
                }

                final long checksum = results.getLong(COMMAND_MESSAGE_TABLE_ABBREV + ".checksum");

                command.setChecksum(results.wasNull() ? null : checksum);

                final long totalCltus = results.getLong(COMMAND_MESSAGE_TABLE_ABBREV + ".totalCltus");

                command.setTotalCltus(results.wasNull() ? null : totalCltus);

                // MPCS-4839 Verify and log and correct
                // MPCS-6349 : DSS ID not set properly
                command.setRecordDssId(getCorrectedStation(results, COMMAND_STATUS_TABLE_ABBREV + ".dssId"));

                long    radTimeCoarse = results.getLong(COMMAND_STATUS_TABLE_ABBREV + ".bit1RadTimeCoarse");
                boolean coarseNull    = results.wasNull();
                int     radTimeFine   = results.getInt(COMMAND_STATUS_TABLE_ABBREV + ".bit1RadTimeFine");
                boolean fineNull      = results.wasNull();

                if (! coarseNull && ! fineNull)
                {
                    command.setBit1RadTime(
                        DbTimeUtility.dateFromCoarseFine(radTimeCoarse, radTimeFine));
                }
                else
                {
                    command.setBit1RadTime(null);
                }

                radTimeCoarse = results.getLong(COMMAND_STATUS_TABLE_ABBREV + ".lastBitRadTimeCoarse");
                coarseNull    = results.wasNull();
                radTimeFine   = results.getInt(COMMAND_STATUS_TABLE_ABBREV + ".lastBitRadTimeFine");
                fineNull      = results.wasNull();

                if (! coarseNull && ! fineNull)
                {
                    command.setLastBitRadTime(
                        DbTimeUtility.dateFromCoarseFine(radTimeCoarse, radTimeFine));
                }
                else
                {
                    command.setLastBitRadTime(null);
                }

				refs.add(command);

				count++;

                // Handle any unhandled warnings
                /** MPCS-6718  */
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
			throw new DatabaseException("Error retrieving command messages from database: " + e.getMessage());
		}

		return(refs);
	}


    /**
     * Close SPF, if any, then continue closing.
     */
    @Override
	public void close()
    {
        if (spf != null)
        {
            spf.close();

            spf = null;
        }

        super.close();
    }
}
