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
package jpl.gds.db.mysql.impl.sql.fetch.cfdp;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbContexConfigPreFetch;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpRequestResultFetch;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpRequestResultLDIStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.cfdp.IDbCfdpRequestResultFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpRequestResultProvider;
import jpl.gds.db.api.types.cfdp.IDbCfdpRequestResultUpdater;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DbTimeUtility;
import org.springframework.context.ApplicationContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the database read/retrieval interface to the CfdpRequestResult table
 * in the MPCS database. This class will retrieve one or more indications from
 * the database based on a number of different query input parameters.
 * <p>
 * The general way to use this class is:
 * <ol>
 * <li>Create a TestSessionInfo object and set all the information on it that
 * should be used to search the database for test sessions</li>
 * <li>Create any other necessary query values</li>
 * <li>Call one of the "getCfdpFileGenerations(...)" methods and specify a batch
 * size for the size of lists that should be returned. The
 * "getCfdpFileGenerations(...)" methods will return only the first batch of
 * results.</li>
 * <li>Make further calls to getNextResultBatch() to retrieve the rest of the
 * results from the query</li>
 *
 */
public class CfdpRequestResultFetch extends ACfdpMySqlFetch implements ICfdpRequestResultFetch {

    protected String getDbTable() {
        return ICfdpRequestResultLDIStore.DB_CFDP_REQUEST_RESULT_DATA_TABLE_NAME;
    }

    private final IDbCfdpRequestResultFactory dbCfdpRequestResultFactory;

    /**
     * Creates an instance of CfdpRequestResultFetch.
     *
     * @param appContext   the Spring Application Context
     * @param printSqlStmt The flag that indicates whether the fetch class should print out
     *                     the SQL statement only or execute it.
     */
    public CfdpRequestResultFetch(final ApplicationContext appContext, final boolean printSqlStmt) {
        super(appContext, printSqlStmt);
        dbCfdpRequestResultFactory = appContext.getBean(IDbCfdpRequestResultFactory.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, DatabaseTimeRange range, final int batchSize,
                                         final boolean useContext, final Object... params) throws DatabaseException {

        if (tsi == null) {
            throw new IllegalArgumentException("Input test session information was null");
        }

        if (dbProperties.getUseDatabase() == false) {
            return (new ArrayList<IDbCfdpRequestResultProvider>());
        }

        return super.get(DB_CFDP_REQUEST_RESULT_TABLE_ABBREV, tsi, range, batchSize, useContext, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi,
                                                           final DatabaseTimeRange range, final String whereClause, final Object... params) throws DatabaseException {
        if (tsi == null) {
            throw new IllegalArgumentException("The input test session information was null");
        }

        if (range == null) {
            throw new IllegalArgumentException("The input time range information was null");
        }

        if (whereClause == null) {
            throw new IllegalArgumentException("The input where clause was null");
        }

        try {
            int i = 1;

            QueryClauseType clauseType = null;

            // Force because pre-query gives us the sessions

            clauseType = QueryClauseType.NO_JOIN;

            String selectClause = queryConfig.getQueryClause(clauseType,
                    getActualTableName(ICfdpRequestResultLDIStore.DB_CFDP_REQUEST_RESULT_DATA_TABLE_NAME));

            // The combination of FORWARD_ONLY, CONCUR_READ_ONLY, and a fetch size
            // of Integer.MIN_VALUE signals to the MySQL JDBC driver to stream the
            // results row by row rather than trying to load the whole result set
            // into memory. Any other settings will result in a heap overflow. Note
            // that this will probably break if we change database vendors.

            this.statement = getPreparedStatement(selectClause + whereClause, ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);

            this.statement.setFetchSize(Integer.MIN_VALUE);

            final String type = (String) params[0];

            if (type != null) {
                this.statement.setString(i++, type);
            }

            if (this.printStmtOnly) {
                printSqlStatement(this.statement);
            } else {
                this.results = this.statement.executeQuery();
            }

        } catch (final SQLException e) {
            throw new DatabaseException("Error retrieving CFDP Request Result from database: " + e.getMessage(), e);
        }

        return (getResults());
    }

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.db.api.sql.fetch.AbstractMySqlFetch#getNextResultBatch()
     */
    @Override
    public List<IDbCfdpRequestResultProvider> getNextResultBatch() throws DatabaseException {
        return getResults();
    }

    /**
     * This is the internal class method that keeps track of the JDBC ResultSet
     * returned by a query. Every call to this method will return a list of test
     * configurations that match the original query. The size of the returned lists
     * is determined by the batch size that was entered when the query was made.
     * When there are no more results, this method will return an empty list.
     * <p>
     * Warning is a false positive as we are just wrapping;
     *
     * @return The list of CFDP Request Result that is part of the results of a query executed
     * using the "get(...)" methods. When an empty list is returned,
     * it means no more results are left.
     * @throws DatabaseException If there happens to be an exception retrieving the next set of
     *                           results
     */
    @Override
    protected List<IDbCfdpRequestResultProvider> getResults() throws DatabaseException {
        final List<IDbCfdpRequestResultProvider> refs = new ArrayList<IDbCfdpRequestResultProvider>();

        if (this.results == null) {
            return (refs);
        }

        int count = 0;

        try {
            /** MPCS-6718 Look for warnings */

            final List<SQLWarning> warnings = new ArrayList<SQLWarning>();

            // loop through until we fill up our first batch or we've
            // got no more results
            while (count < this.batchSize) {
                if (this.results.next() == false) {
                    break;
                }

                // pull out all the information for this CFDP Request Result and add it
                // to the return list
                final IDbCfdpRequestResultUpdater dp = dbCfdpRequestResultFactory.createQueryableUpdater();

                dp.setEventTime(DbTimeUtility.dateFromCoarseFine(this.results.getLong("eventTimeCoarse"),
                        this.results.getInt("eventTimeFine")));
                dp.setCfdpProcessorInstanceId(this.results.getString("cfdpProcessorInstanceId"));
                dp.setRequestId(this.results.getString("requestId"));
                dp.setRejected(this.results.getBoolean("rejected"));
                dp.setResultContent(this.results.getString("resultContent"));

                populateUpdaterWithContextAndSessionInfo(trace, spf, cpf, warnings, dp, results);

                refs.add(dp);
                count++;

                SqlExceptionTools.logWarning(trace, results);
            }

            // if we're all done with results, clean up
            // all the resources
            if (this.results.isAfterLast() == true) {
                this.results.close();
                this.statement.close();

                this.results = null;
                this.statement = null;
            }
        } catch (final SQLException e) {
            throw new DatabaseException("Error retrieving CFDP Request Result events from database: " + e.getMessage(), e);
        }

        return (refs);
    }

}
