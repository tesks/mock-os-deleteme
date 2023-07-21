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
package jpl.gds.db.api.sql.fetch;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.IDbInteractor;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.shared.holders.ApidHolder;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;

public interface IDbSqlFetch extends IDbInteractor {
    /**
     * This method formats and prints to standard output the SQL used in a
     * statement or prepared statement.
     * 
     * @param stmtStr
     *            SQL from statement
     * @param name
     *            Name of statement
     */
    public static void printSqlStatement(final String stmtStr, final String name) {
        final StringBuilder sb = new StringBuilder("\n\n");

        if (name != null) {
            sb.append(name).append(": ");
        }

        int ix = stmtStr.toUpperCase().indexOf("SELECT");
        final int paren = stmtStr.indexOf('(');

        if (paren < ix) {
            ix = paren;
        }

        sb.append((ix >= 0) ? stmtStr.substring(ix) : stmtStr).append('\n');

        System.out.println(sb);
    }

    /**
     * This method formats and prints to standard output the toString() value of
     * a PreparedStatement object.
     * 
     * @param stmt
     *            Prepared statement
     * @param name
     *            Name of statement
     */
    public static void printSqlStatement(final PreparedStatement stmt, final String name) {
        printSqlStatement(stmt.toString(),
                          name);
    }

    /**
     * Adds the given condition to the given where clause, and returns the new
     * where clause
     * 
     * @param oldWhere
     *            the existing where clause; if null, the WHERE keyword will be
     *            added first
     * @param newClause
     *            the new condition to be ANDed with the existing where clause
     * @return the new where clause
     */
    public static String addToWhere(final String oldWhere, final String newClause) {
        final String newC = (newClause != null) ? newClause : "";
        final String oldW = (oldWhere != null) ? oldWhere : "";

        if (oldW.length() == 0) {
            if (newC.length() > 0) {
                return " WHERE " + newC;
            }

            return "";
        }

        if (newC.length() > 0) {
            return (oldW + " AND " + newC);
        }

        return oldW;
    }

    /**
     * Adds the given condition to the given where clause, and returns the new
     * where clause.
     *
     * @param oldWhere
     *            the existing where clause; if empty, the WHERE keyword will be
     *            added first
     * @param newClause
     *            the new condition to be ANDed with the existing where clause
     * 
     * @return the new where clause
     */
    public static StringBuilder addToWhere(final StringBuilder oldWhere, final String newClause) {
        /** MPCS-6032 New method */

        final StringBuilder oldW = ((oldWhere != null) ? oldWhere : new StringBuilder());
        final String newC = StringUtil.safeTrim(newClause);

        if (newC.isEmpty()) {
            return oldW;
        }

        if (oldW.length() == 0) {
            oldW.append(" WHERE ");
        }
        else {
            oldW.append(" AND ");
        }

        return oldW.append(newC);
    }

    /**
     * Generate where clause segment for DSS ids. We no longer add zero as a
     * wildcard.
     *
     * @param dssIds
     *            Collection of DSS ids
     * @param abbrev
     *            Table abbreviation
     *
     * @return Where clause segment for the DSS ids
     */
    public static String generateDssIdWhere(final Collection<Integer> dssIds, final String abbrev) {
        if ((dssIds == null) || dssIds.isEmpty()) {
            return "";
        }

        final Set<Integer> extra = new TreeSet<Integer>(dssIds);

        // Make sure we have zero, the wildcard
        // extra.add(0);

        final StringBuilder sb = new StringBuilder();
        boolean first = true;

        // if (nullable)
        // {
        // sb.append('(');
        // }

        sb.append('(').append(abbrev).append(".dssId IN (");

        for (final int dssId : extra) {
            if (first) {
                first = false;
            }
            else {
                sb.append(',');
            }

            sb.append(dssId);
        }

        sb.append("))");

        // if (nullable)
        // {
        // sb.append(" OR (").append(abbrev).append(".vcid IS NULL))");
        // }

        return sb.toString();
    }

    /**
     * Generate where clause segment for VCIDs. If column is nullable, NULL is
     * added as a wildcard.
     *
     * @param vcids
     *            Collection of VCIDs
     * @param abbrev
     *            Table abbreviation
     * @param nullable
     *            True if vcid column is nullable
     *
     * @return Where clause segment for the VCIDs
     */
    public static String generateVcidWhere(final Collection<Integer> vcids,
                                           final String abbrev,
                                           final boolean nullable) {
        if ((vcids == null) || vcids.isEmpty()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        boolean first = true;


        sb.append('(').append(abbrev).append(".vcid IN (");

        for (final int vcid : vcids) {
            if (first) {
                first = false;
            }
            else {
                sb.append(',');
            }

            sb.append(vcid);
        }

        sb.append("))");


        return sb.toString();
    }

    /**
     * Generate where clause segment for APIDs
     *
     * @param apids
     *            Collection of APIDs.
     * @param abbrev
     *            Table abbreviation
     *
     * @return Where clause segment for the APIDS
     */
    public static String generateApidWhere(final Collection<ApidHolder> apids, final String abbrev) {
        if ((apids == null) || apids.isEmpty()) {
            return "";
        }

        final Set<ApidHolder> sorted = new TreeSet<ApidHolder>(apids);
        final StringBuilder sb = new StringBuilder();
        boolean first = true;

        sb.append('(').append(abbrev).append(".apid IN (");

        for (final ApidHolder apid : sorted) {
            if (first) {
                first = false;
            }
            else {
                sb.append(',');
            }

            sb.append(apid);
        }

        sb.append("))");

        return sb.toString();
    }

    /**
     * Retrieve a list of rows based on the search information given in the
     * testSessionInfo input object and the other parameters. The batch size
     * will specify how many results will be returned at a time. This method
     * will return the first batch of results; to get more results from this
     * query, the getNextResultBatch() method should be called. When an empty
     * list is returned, there are no more results left.
     * 
     * NEW METHOD: Does not take Database Session Info object,
     * but retrieves it internally from the IDbSqlArchiveController's default
     * session.
     *
     * @param range
     *            Time-range to query on
     * @param batchSize
     *            Maximum number of rows to return per batch
     * @param params
     *            Array of parameters that provide where-clause values
     * 
     * @return the first list of rows
     * @throws DatabaseException
     *             if a database error occurs
     */
    List<? extends IDbRecord> get(DatabaseTimeRange range, int batchSize, Object... params) throws DatabaseException;

    List<? extends IDbRecord> getByContext(DatabaseTimeRange range, int batchSize, Object... params) throws DatabaseException;

    /**
     * Retrieve a list of database entries based on the search information given
     * in the testSessionInfo input object and the other channel value
     * parameters. The batch size will specify how many channel value results
     * will be returned at a time. This method will return the first batch of
     * results; to get more results from this query, the getNextResultBatch()
     * method should be called. If this method is called a second time before
     * all results from the first call to this method are retrieved, any
     * un-retrieved results will be lost (in other words, this object only
     * remembers the results of one query at a time).
     *
     * @param tsi
     *            The search information used to find contexts in the
     *            database (may not be null)
     * @param range
     *            The database time range specifying the type of time and time
     *            range for querying. If null, the default time type and a time
     *            range of 0 to infinity will be used.
     * @param batchSize
     *            The number of results returned at once. This specifies an
     *            upper limit on the number of results that will be returned at
     *            once, but there is no lower limit.
     * @param params
     *            Parameters to fill in where-clause
     *
     * @return The list of channel values that matched the input search
     *         information. This list will have a maximum size of "batchSize".
     *         NOTE: If both the channelIds and channelIndexes array parameters
     *         are null or empty, then channel values for all channels will be
     *         returned.
     * @throws DatabaseException
     *             if an error occurs
     *
     * MPCS-9891 - Replaced first parameter with IDbContextInfoProvider
     */
    List<? extends IDbRecord> get(IDbContextInfoProvider tsi, DatabaseTimeRange range, int batchSize, Object... params)
            throws DatabaseException;

    /**
     * See get(IDbContextInfoProvider, DatabaseTimeRange, batchSize, params)
     *
     * @param useContext Whether we use session or context info
     * MPCS-10119 - Added member
     */
    default List<? extends IDbRecord> get(IDbContextInfoProvider tsi, DatabaseTimeRange range, int batchSize,
                                  boolean useContext, Object... params)
            throws DatabaseException {
        //by default join by session
        return get(tsi, range, batchSize, params);
    }

    /**
     * Return next batch of results from query.
     *
     * @return List of rows
     * @throws DatabaseException
     *             if a database error occurs
     */
    List<? extends IDbRecord> getNextResultBatch() throws DatabaseException;

    /**
     * Aborts the in-progress query by discarding the result set. This causes
     * the query to disappear in the server. If you don't call this, and all the
     * results from the last query are not read, the query will continue running
     * in the server.
     *
     * There is really nothing to do if we get an exception. We cannot do
     * anything at this point, and the exception may be caused by our killing
     * stuff anyway.
     */
    void abortQuery();

    /**
     * Get the string representation of the "WHERE" portion of the SQL insert
     * query.
     *
     * @param testSqlTemplate
     *            The templated SQL string with WHERE clause conditions
     * @param range
     *            The database time range specifying the type of time and time
     *            range for querying.
     * @param params
     *            Parameters to fill in where-clause values
     *
     * @return The complete templated SQL WHERE clause for the query
     *
     * @throws DatabaseException
     *             if a database error occurs
     *
     * MPCS-9891 - Removed IDbSessionInfoProvider parameter
     */
    String getSqlWhereClause(final String testSqlTemplate,
                             final DatabaseTimeRange range,
                             final Object... params)
            throws DatabaseException;
}