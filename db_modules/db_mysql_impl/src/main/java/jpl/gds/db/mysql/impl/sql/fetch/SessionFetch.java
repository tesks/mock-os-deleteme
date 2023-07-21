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

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.store.IEndSessionStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionFactory;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.api.types.IDbSessionProvider;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.db.mysql.impl.sql.order.SessionOrderByType;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DbTimeUtility;


/**
 * This is the database read/retrieval interface to the Test Session table in the
 * MPCS database.  This class will retrieve one or more test sessions from the database
 * based on a number of different query input parameters.
 *
 * The general way to use this class is:
 * <ol>
 * <li>Create a TestSessionInfo object and set all the information on it that should be used
 * to search the database for test sessions</li>
 * <li>Call one of the "getTestSessions(...)" methods and specify a batch size for the size
 * of lists that should be returned.  The "getTestSessions(...)" methods will return only
 * the first batch of results.</li>
 * <li>Make further calls to getNextResultBatch() to retrieve the rest of the results from
 * the query</li>
 *
 * Note that we need to do a join on the EndSession table in order to get the
 * end time. We do an "outer join" in case the EndSession is missing.
 *
 */
public class SessionFetch extends AbstractMySqlFetch implements ISessionFetch
{
    /**
     * The SELECT and FROM portions of the SQL query that will be used to
     * retrieve test session information from the database.
     */
    private final String selectClause = queryConfig.getQueryClause(QueryClauseType.ANY_SESSION_SELECT, DB_SESSION_TABLE_NAME);

    /** debug flag */
    boolean DEBUG = false;

    private final IDbSessionFactory dbSessionFactory;

    /**
     *
     * Creates an instance of TestSessionFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            The flag that indicates whether the fetch
     *            class should print out the SQL statement
     *            only or execute it.
     */
	public SessionFetch(final ApplicationContext appContext, final boolean printSqlStmt)
	{
		super(appContext, printSqlStmt);
        dbSessionFactory = appContext.getBean(IDbSessionFactory.class);
	}

	/**
     * Creates an instance of TestSessionFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public SessionFetch(final ApplicationContext appContext)
	{
		this(appContext, false);
	}

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionFetch#get(jpl.gds.db.api.types.IDbAccessItem, jpl.gds.shared.time.DatabaseTimeRange, int, java.lang.Object)
     */
	@Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, final DatabaseTimeRange range, final int batchSize, final Object... params)
            throws DatabaseException {
		if(tsi == null)
		{
			throw new IllegalArgumentException("Null test session input information");
		}
		else if(batchSize <= 0)
		{
			throw new IllegalArgumentException("Invalid batch size of " + batchSize + " passed in.");
		}

        if (!this.dbProperties.getUseDatabase())
		{
			return(new ArrayList<IDbSessionProvider>());
		}
		
		//initialize the fetch query
		initQuery(batchSize);
		
		final String whereClause =
            getSqlWhereClause(
                tsi.getSqlTemplate(DB_SESSION_TABLE_ABBREV),
                range,
                params);

		return(populateAndExecute(tsi,range,whereClause,params));
	}


    /**
     * Get the string representation of the where and order-by portions of the SQL insert query
     *
     * @param testSqlTemplate The templated SQL string with WHERE clause conditions
     * @param range           Time-range to query over
     * @param params          Array of parameters used to fill in where clause
     *
     * @return The complete templated SQL WHERE clause for the query
     */
    @Override
    public String getSqlWhereClause(
            final String                 testSqlTemplate,
            final DatabaseTimeRange      range,
            final Object...              params) throws DatabaseException
    {
        //MPCS-10768 - Refactored to support lack of session fragment

        StringBuilder sqlWhere = new StringBuilder();
        final String tst = StringUtil.safeTrim(testSqlTemplate);

        if (!tst.isEmpty()){
            sqlWhere = addToWhere(sqlWhere, tst);
        }

        // Look for a specific sessionFragment. Normally we use one, but
        // JUnits need to be able to specify others.

        //MPCS-10768 - Add session fragment, only if not null
        if(params.length >= 2 && params[1] != null) {
            sqlWhere = addToWhere(sqlWhere, "(" + DB_SESSION_TABLE_ABBREV + "." + FRAGMENT_ID + "=" + params[1] + ")");

        }

        // add SessionVcid and sesionDssId
        // MPCS-8979
        @SuppressWarnings("unchecked")
        final Set<Integer> vcid = (Set<Integer>) ((params.length >= 3) ? params[2] : null);

        @SuppressWarnings("unchecked")
        final Set<Integer> dssId = (Set<Integer>) ((params.length >= 4) ? params[3] : null);
        // building Vcid query
        if ((vcid != null) && !vcid.isEmpty()) {
            // if it only contains 1 vcid
            if (vcid.size() == 1) {
                sqlWhere = addToWhere(sqlWhere, "(" + DB_SESSION_TABLE_ABBREV + "." + ISessionFetch.VCID + "=" +
                        vcid.iterator().next() + ")");
            }
            // it contains multiple Vcids
            else {
                StringBuilder vcWhere = new StringBuilder("(");
                while (vcid.iterator().hasNext()) {
                    vcWhere.append('(');
                    vcWhere.append(DB_SESSION_TABLE_ABBREV).append('.').append(ISessionFetch.VCID);
                    vcWhere.append('=').append(vcid.iterator().next());
                    vcWhere.append(')');
                    // remove the element
                    vcid.remove(vcid.iterator().next());
                    if (!vcid.isEmpty()) {
                        vcWhere.append(" OR ");
                    }
                    else {
                        vcWhere.append(')');
                    }
                }
                sqlWhere = addToWhere(sqlWhere, vcWhere.toString());
            }
        }

        // building dssId query
        if ((dssId != null) && !dssId.isEmpty()) {
            // if it only contains 1 vcid
            if (dssId.size() == 1) {
                sqlWhere = addToWhere(sqlWhere, "(" + DB_SESSION_TABLE_ABBREV + "." + ISessionFetch.DSS_ID + "=" +
                        dssId.iterator().next() + ")");
            }
            // it contains multiple DSS IDs
            else {
                StringBuilder dssWhere = new StringBuilder("(");
                while (dssId.iterator().hasNext()) {
                    dssWhere.append('(');
                    dssWhere.append(DB_SESSION_TABLE_ABBREV).append('.').append(ISessionFetch.DSS_ID);
                    dssWhere.append('=').append(dssId.iterator().next());
                    dssWhere.append(')');
                    // remove the element
                    dssId.remove(dssId.iterator().next());
                    if (!dssId.isEmpty()) {
                        dssWhere.append(" OR ");
                    }
                    else {
                        dssWhere.append(')');
                    }
                }
                sqlWhere = addToWhere(sqlWhere, dssWhere.toString());
            }
        }

        IDbOrderByType orderType = (IDbOrderByType) params[0];

        if (orderType == null)
        {
            orderType = SessionOrderByType.DEFAULT;	// default OrderBy
        }

        sqlWhere.append("\n    ").append(orderType.getOrderByClause());

        return sqlWhere.toString();
    }

    /**
     * Populate the templated WHERE clause portion of the SQL and then execute the query and return the
     * results of the query.
     *
     * @param tsi
     *            The test session information object used to specify test sessions in the database to retrieve. This
     *            object
     *            should be exactly the same as the one passed to "getTestSessions(...)" or unpredictable behavior may
     *            occur.
     * @param range
     *            Time-range to query over
     * @param whereClause
     *            The templated where clause for the SQL query (templated means that there are ?'s in place of all the
     *            values that need to be filled in).
     * @param params
     *            Array of parameters used to fill in where-clause values
     *
     * @return The list of test configurations that matched the input search information
     * @throws DatabaseException
     *             If there is a problem retrieving the test sessions from the database
     */
    @Override
    protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
            final String whereClause, final Object... params) throws DatabaseException {
		if(tsi == null)
		{
			throw new IllegalArgumentException("The input test session information was null");
		}

		if(whereClause == null)
		{
			throw new IllegalArgumentException("The input where clause was null");
		}

		try
		{
			final int i = 1;
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


			//fill in all the parameters for the test session
            dbSessionInfoFactory.convertProviderToUpdater((IDbSessionInfoProvider)tsi).fillInSqlTemplate(i, this.statement);

            if (DEBUG)
            {
                System.out.println("SQL: " + statement);
            }

			if (this.printStmtOnly) {
				printSqlStatement(this.statement);
			}
			else {
				//execute the query
				this.results = this.statement.executeQuery();
			}

		}
		catch(final SQLException e)
		{
			throw new DatabaseException("Error retrieving test sessions from database: " + e.getMessage());
		}

		return(getResults());
	}

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.fetch.IDbSessionFetch#getNextResultBatch()
     */
    @Override
    public List<IDbSessionUpdater> getNextResultBatch() throws DatabaseException {
        return (getResults());
    }

	/**
     * This is the internal class method that keeps track of the JDBC ResultSet returned by
     * a query. Every call to this method will return a list of test configurations that match
     * the original query. The size of the returned lists is determined by the batch size that was
     * entered when the query was made. When there are no more results, this method will return an
     * empty list.
     *
     * @return The list of test configurations that is part of the results of a query executed using the
     *         "getTestSessions(...)" methods. When an empty list is returned, it means no more results are left.
     *
     * @throws DatabaseException
     *             If there happens to be an exception retrieving the next set of results
     */
	@Override
    protected List<IDbSessionUpdater> getResults() throws DatabaseException
	{
        final List<IDbSessionUpdater> refs = new ArrayList<>(1024);

		//if no query was executed or we're done with the current
		//result set, just return an empty list
		if(this.results == null)
		{
			return(refs);
		}

		int count = 0;
		try
		{
            /** MPCS-6718 Look for warnings */

            final List<SQLWarning> warnings = new ArrayList<>();

			//loop until we have enough to fill a batch
			while(count < this.batchSize)
			{
				//advance the result set and then check whether or not
				//the result set has moved past the last one...if we're
				//past the end, then we can break the loop because there
				//are no more results
                if (!this.results.next())
				{
					break;
				}

				//fill out the new test configuration object and add it to the output list
                final IDbSessionUpdater dsc = dbSessionFactory.createQueryableUpdater();

				dsc.setSessionId(Long.valueOf(this.results.getLong(DB_SESSION_TABLE_ABBREV + "." + SESSION_ID)));
				dsc.setName(this.results.getString(DB_SESSION_TABLE_ABBREV + ".name"));
				dsc.setType(this.results.getString(DB_SESSION_TABLE_ABBREV + ".type"));
				dsc.setDescription(this.results.getString(DB_SESSION_TABLE_ABBREV + ".description"));
				dsc.setFullName(this.results.getString(DB_SESSION_TABLE_ABBREV + ".fullName"));
				dsc.setUser(this.results.getString(DB_SESSION_TABLE_ABBREV + ".user"));
				dsc.setSessionHost(this.results.getString(DB_SESSION_TABLE_ABBREV + ".host"));
				dsc.setSessionHostId(this.results.getInt(DB_SESSION_TABLE_ABBREV + "." + HOST_ID));

                /** MPCS-6718  Look for warnings */

                warnings.clear();

                dsc.setSessionFragment(SessionFragmentHolder.getFromDbRethrow(results,
                                                                              DB_SESSION_TABLE_ABBREV + "." + FRAGMENT_ID,
                                                                              warnings));
                SqlExceptionTools.logWarning(warnings, trace);

                // NULL values for connection types are not UNKNOWN. UNKNOWN is for bad values.
				
				String ct = StringUtil.emptyAsNull(this.results.getString(DB_SESSION_TABLE_ABBREV + ".downlinkConnectionType"));

                if (ct == null)
                {
                    dsc.setConnectionType(TelemetryConnectionType.UNKNOWN);
                }
                else
                {
                    final TelemetryConnectionType dct = TelemetryConnectionType.safeValueOf(ct);

                    if (dct != null)
                    {
                        dsc.setConnectionType(dct);
                    }
                    else
                    {
                        dsc.setConnectionType(TelemetryConnectionType.UNKNOWN);

                        trace.warn("Session.downlinkConnectionType has bad value: '" + ct + "'");
                    }
                }
				
                ct = StringUtil.emptyAsNull(results.getString(DB_SESSION_TABLE_ABBREV + ".uplinkConnectionType"));

                if (ct == null)
                {
                    dsc.setUplinkConnectionType(UplinkConnectionType.UNKNOWN);
                }
                else
                {
                    final UplinkConnectionType uct = UplinkConnectionType.safeValueOf(ct);

                    if (uct != null)
                    {
                        dsc.setUplinkConnectionType(uct);
                    }
                    else
                    {
                        dsc.setUplinkConnectionType(UplinkConnectionType.UNKNOWN);

                        trace.warn("Session.uplinkConnectionType has bad value: '" + ct + "'");
                    }
                }

				dsc.setOutputDirectory(this.results.getString(DB_SESSION_TABLE_ABBREV + ".outputDirectory"));
				dsc.setSseDictionaryDir(this.results.getString(DB_SESSION_TABLE_ABBREV + ".sseDictionaryDir"));
				dsc.setFswDictionaryDir(this.results.getString(DB_SESSION_TABLE_ABBREV + ".fswDictionaryDir"));
				dsc.setSseVersion(this.results.getString(DB_SESSION_TABLE_ABBREV + ".sseVersion"));
				dsc.setFswVersion(this.results.getString(DB_SESSION_TABLE_ABBREV + ".fswVersion"));

				final String vt =
                    StringUtil.safeTrim(
                        this.results.getString(DB_SESSION_TABLE_ABBREV + ".venueType"));

				dsc.setVenueType(
                    VenueType.valueOf((vt.length() > 0) ? vt : "UNKNOWN"));

				dsc.setTestbedName(this.results.getString(DB_SESSION_TABLE_ABBREV + ".testbedName"));

                // May be NULL
				final String rit =
                    StringUtil.safeTrim(
                        this.results.getString(DB_SESSION_TABLE_ABBREV + ".rawInputType"));

				dsc.setRawInputType(
                    TelemetryInputType.valueOf((rit.length() > 0) ? rit : "UNKNOWN"));

                final long starttimecoarse =
                    this.results.getLong(DB_SESSION_TABLE_ABBREV + ".startTimeCoarse");

                final int starttimefine =
                    this.results.getInt(DB_SESSION_TABLE_ABBREV + ".startTimeFine");

				dsc.setStartTime(DbTimeUtility.dateFromCoarseFine(starttimecoarse, starttimefine));

                // Take endTime from EndSession if present, otherwise use
                // the startTime

                long endtimecoarse =
                    this.results.getLong(IEndSessionStore.tableAbbrev +
                                         ".endTimeCoarse");

                int endtimefine =
                    this.results.getInt(IEndSessionStore.tableAbbrev +
                                        ".endTimeFine");

                if ((endtimecoarse == 0L) && (endtimefine == 0))
                {
                    endtimecoarse = starttimecoarse;
                    endtimefine   = starttimefine;
                }

				dsc.setEndTime(DbTimeUtility.dateFromCoarseFine(endtimecoarse, endtimefine));

				dsc.setSpacecraftId(this.results.getInt(DB_SESSION_TABLE_ABBREV + ".spacecraftId"));

                // MPCS-4819 BEGIN Catch null or bad value, force to NOT_APPLICABLE and emit error
                // instead of letting convert take the null and throw on bad value

                final String                dlsis = this.results.getString(DB_SESSION_TABLE_ABBREV + ".downlinkStreamId");
                DownlinkStreamType dlsi  = null;

                // Must correspond to the ENUM and not be NULL

                try
                {
                    dlsi = DownlinkStreamType.convert(dlsis);
                }
                catch (final IllegalArgumentException iae)
                {
                    dlsi = DownlinkStreamType.NOT_APPLICABLE;

                    trace.error("Bad value for DownlinkStreamType: '" +
                                dlsis                                          +
                                "' forced to "                                 +
                                dlsi);
                }

				dsc.setDownlinkStreamId(dlsi);

                // MPCS-4819 End

				dsc.setMpcsVersion(this.results.getString(DB_SESSION_TABLE_ABBREV + ".mpcsVersion"));

				dsc.setFswDownlinkHost(this.results.getString(DB_SESSION_TABLE_ABBREV + ".fswDownlinkHost"));
				dsc.setFswUplinkHost(this.results.getString(DB_SESSION_TABLE_ABBREV + ".fswUplinkHost"));

                // Ports are never zero, but instead NULL. But NULL will be converted to 0.

				dsc.setSseHost(this.results.getString(DB_SESSION_TABLE_ABBREV + ".sseHost"));
				
				// Mods here made as part of master Jira MPCS-4576. Host/Session
				// configuration was using a hard-coded constant for UNDEFINED PORTS which was -1.
				// Database is returning 0 for the undefined case. Since the fields are unsigned
				// the value must be converted back to the -1 value here for consistency of all
				// session configuration output.
				// MPCS-4814. Set undefined ports to NULL rather than 0 or -1
				int tempPort = this.results.getInt(DB_SESSION_TABLE_ABBREV + ".fswUplinkPort");
				dsc.setFswUplinkPort(tempPort == 0 ? null : tempPort);
				
				tempPort = this.results.getInt(DB_SESSION_TABLE_ABBREV + ".fswDownlinkPort");
				dsc.setFswDownlinkPort(tempPort == 0 ? null: tempPort);
				
				tempPort = this.results.getInt(DB_SESSION_TABLE_ABBREV + ".sseUplinkPort");
				dsc.setSseUplinkPort(tempPort == 0 ? null: tempPort);
				
				tempPort = this.results.getInt(DB_SESSION_TABLE_ABBREV + ".sseDownlinkPort");
				dsc.setSseDownlinkPort(tempPort == 0 ? null : tempPort);
				
				dsc.setInputFile(this.results.getString(DB_SESSION_TABLE_ABBREV + ".inputFile"));
				dsc.setTopic(StringUtil.emptyAsNull(this.results.getString(DB_SESSION_TABLE_ABBREV + ".topic")));

                // V4 additions. No primitives are passed to dsc, even if they cannot be NULL.

                // Work object
                final StringBuilder sb = new StringBuilder();

                // Not NULL
                dsc.setOutputDirectoryOverride(getBooleanWithoutNull(sb, "outputDirectoryOverride"));

                // May be NULL
                dsc.setSubtopic(getStringWithNull(sb, "subtopic"));

                // MPCS-4839 Verify and log and correct
                /*
                 *
                 * MPCS-6034 : MTAK get_eha after downlink summary
                 * MPCS-6035 : MTAK doesn't seem to get the last value of the MON channel
                 * 
                 * Replaced setStation with setSessionDssId
                 */
                dsc.setSessionDssId(getCorrectedStation(results, DB_SESSION_TABLE_ABBREV + ".dssId"));

                // May be NULL
                dsc.setVcid(getLongWithNull(sb, "vcid"));

                // Not NULL
                dsc.setFswDownlinkFlag(getBooleanWithoutNull(sb, "fswDownlinkFlag"));

                // Not NULL
                dsc.setSseDownlinkFlag(getBooleanWithoutNull(sb, "sseDownlinkFlag"));

                // Not NULL
                dsc.setUplinkFlag(getBooleanWithoutNull(sb, "uplinkFlag"));

                // May be NULL
                dsc.setDatabaseSessionId(getLongWithNull(sb, "databaseSessionId"));

                // May be NULL
                dsc.setDatabaseHost(getStringWithNull(sb, "databaseHost"));

				refs.add(dsc);

				count++;

                // Handle any unhandled warnings
                /** MPCS-6718  */
                SqlExceptionTools.logWarning(trace, results);
			}

			//if the result set has moved past its last entry, then its ok
			//to go ahead and close all the JDBC stuff so it can be GCed
            if (this.results.isAfterLast())
			{
				this.results.close();
				this.statement.close();

				this.results = null;
				this.statement = null;

                close();
			}
		}
		catch(final SQLException e)
		{
			throw new DatabaseException("Error retrieving test sessions from database: " + e.getMessage());
		}

		return(refs);
	}


    /**
     * Get boolean object, cannot be NULL.
     *
     * @param sb     StringBuilder work object
     * @param column Column name
     *
     * @return Value as a Boolean
     *
     * @throws DatabaseException On any database problem
     */
    private Boolean getBooleanWithoutNull(final StringBuilder sb,
                                          final String        column)
        throws DatabaseException
    {
        sb.setLength(0);

        sb.append(DB_SESSION_TABLE_ABBREV).append('.').append(column);

        try {
            return ((results.getInt(sb.toString()) != 0) ? Boolean.TRUE : Boolean.FALSE);
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


    // MPCS-4839 getIntegerWithoutNull deleted


    /**
     * Get Long object, possibly as a NULL.
     *
     * @param sb
     *            StringBuilder work object
     * @param column
     *            Column name
     *
     * @return Value as a Long
     *
     * @throws DatabaseException
     *             On any database problem
     */
    private Long getLongWithNull(final StringBuilder sb, final String column) throws DatabaseException {
        sb.setLength(0);

        sb.append(DB_SESSION_TABLE_ABBREV).append('.').append(column);

        try {
            final long value = results.getLong(sb.toString());
            if (results.wasNull()) {
                return null;
            }
            return Long.valueOf(value);
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }

    /**
     * Get string object, possibly as a NULL.
     *
     * @param sb     StringBuilder work object
     * @param column Column name
     *
     * @return Value as a String
     *
     * @throws DatabaseException On any database problem
     */
    private String getStringWithNull(final StringBuilder sb,
                                     final String        column)
        throws DatabaseException
    {
        sb.setLength(0);

        sb.append(DB_SESSION_TABLE_ABBREV).append('.').append(column);

        try {
            return StringUtil.emptyAsNull(results.getString(sb.toString()));
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }
}
