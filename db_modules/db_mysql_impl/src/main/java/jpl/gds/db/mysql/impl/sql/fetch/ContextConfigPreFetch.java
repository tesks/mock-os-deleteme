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

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.IDbInteractor;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.IDbContexConfigPreFetch;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;

/**
 * Read context id and host id from DB. Used as pre-fetch to allow subsequent
 * queries to be more efficient by supplying full session id and host id in
 * where clauses.
 *
 */
public class ContextConfigPreFetch extends AbstractMySqlFetch implements Closeable, IDbContexConfigPreFetch {

    private static final int BATCH_SIZE = 1000;

    // The host ids and ids returned from the database
    private final Map<Integer, HostAggregate> hostAggregates;

    private final String selectClause;
    private final String tableAbbrev;
    private final String sessionKey;
    private final String host;
    private final String hostIdCol;

    // Lookup host from hostId
    private final Map<Integer, String> hosts;

    /**
     * Creates an instance of ContextConfigPreFetch.
     *
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            True means just print statement.
     */
    public ContextConfigPreFetch(final ApplicationContext appContext, final boolean printSqlStmt){
        super(appContext, printSqlStmt);
        tableAbbrev = queryConfig.getTablePrefix(IDbTableNames.DB_CONTEXT_CONFIG_TABLE_NAME);
        // The host ids and ids returned from the database
        hostAggregates = new HashMap<>();
        sessionKey = tableAbbrev + "." + CONTEXT_ID;
        host = tableAbbrev + "." + "host";
        hostIdCol = tableAbbrev + "." + HOST_ID;
        selectClause =    "SELECT " + sessionKey + "," + host + "," + hostIdCol + " FROM "
                + IDbTableNames.DB_CONTEXT_CONFIG_TABLE_NAME + " AS " + tableAbbrev;
        // Lookup host from hostId
        hosts = new HashMap<>();
    }

    /**
     * Creates an instance of ContextConfigPreFetch.
     *
     * @param appContext
     *            the Spring Application Context
     */
    public ContextConfigPreFetch(final ApplicationContext appContext) {
        this(appContext, false);
    }

    @Override
    public void get() throws DatabaseException {
        get(this.dbContextInfoFactory.createQueryableProvider());
    }

    @Override
    public void get(final IDbContextInfoProvider tsi) throws DatabaseException {
        if (tsi == null)
        {
            throw new IllegalArgumentException("Null context info");
        }

        if (! dbProperties.getUseDatabase())
        {
            return;
        }

        // Initialize the fetch query

        initQuery(BATCH_SIZE); // No batching is actually done

        // Build the actual query string

        final StringBuilder query = new StringBuilder(selectClause);
        final String        where =
                StringUtil.safeTrim(tsi.getSqlTemplate(tableAbbrev));

        if (! where.isEmpty())
        {
            query.append(" WHERE ");
            query.append(where);
        }

        // Note no order-by clause

        try
        {
            // The combination of FORWARD_ONLY, CONCUR_READ_ONLY, and a fetch
            // size of Integer.MIN_VALUE signals to the MySQL JDBC driver to
            // stream the results row by row rather than trying to load the
            // whole result set into memory. Any other settings will result in
            // a heap overflow. Note that this will probably break if we change
            // database vendors.

            statement = getPreparedStatement(query.toString(),
                                             ResultSet.TYPE_FORWARD_ONLY,
                                             ResultSet.CONCUR_READ_ONLY);

            statement.setFetchSize(Integer.MIN_VALUE);

            // Fill in all the parameters for the test session
            dbContextInfoFactory.convertProviderToUpdater(tsi).fillInSqlTemplate(1, statement);

            if (printStmtOnly) {
                printSqlStatement(statement, "Pre-query");
            }
            else {
                results = statement.executeQuery();
            }

            if (results != null) {
                while (true)
                {
                    if (! results.next())
                    {
                        break;
                    }

                    final String  host   = protectNull(results.getString(this.host));
                    final int     hostId = results.getInt(hostIdCol);
                    final long    id     = results.getLong(sessionKey);
                    HostAggregate ha     = hostAggregates.get(hostId);

                    if (ha == null)
                    {
                        ha = new HostAggregate(hostId);

                        hostAggregates.put(hostId, ha);
                    }

                    ha.addId(id);


                    // Be able to lookup from hostId to host
                    hosts.put(hostId, host);

                    // Handle any unhandled warnings
                    SqlExceptionTools.logWarning(trace, results);

                }
            }
        }
        catch (final SQLException sqle)
        {
            throw new DatabaseException("Error retrieving pre-contexts: " + sqle.getMessage(), sqle);
        }
        finally
        {
            if (results != null)
            {
                try {
                    results.close();
                }
                catch (final SQLException e) {
                    SqlExceptionTools.logWarning(trace, e);
                }
                finally {
                    results = null;
                }
            }

            if (statement != null)
            {
                try {
                    statement.close();
                }
                catch (final SQLException e) {
                    SqlExceptionTools.logWarning(TraceManager.getDefaultTracer(), e);
                }
                finally {
                    statement = null;
                }
            }

            close();
        }
    }

    @Override
    public String getIdHostWhereClause(final String abbrev) {
        final int size = hostAggregates.size();

        if (size == 0) {
            return "(0=1)"; // Impossible where clause
        }

        final StringBuilder sb = new StringBuilder(1024);

        if (size > 1) {
            sb.append('(');
        }

        boolean first = true;

        for (final HostAggregate ha : hostAggregates.values()) {
            if (first) {
                first = false;
            }
            else {
                sb.append(" OR ");
            }

            sb.append('(');

            ha.produceWhere(abbrev, sb, IDbInteractor.CONTEXT_ID, CONTEXT_HOST_ID);

            sb.append(')');
        }

        if (size > 1) {
            sb.append(')');
        }

        return sb.toString();
    }

    @Override
    public String lookupHost(final int hostId) {
        return protectNull(hosts.get(hostId));
    }

    @Override
    protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi,
                                                           final DatabaseTimeRange range, final String whereClause,
                                                           final Object... params) throws DatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<? extends IDbRecord> getResults() throws DatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
                                         final int batchSize, final Object... params) throws DatabaseException {
        throw new UnsupportedOperationException();

    }

    @Override
    public List<? extends IDbRecord> getNextResultBatch() throws DatabaseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSqlWhereClause(final String testSqlTemplate, final DatabaseTimeRange range, final Object... params)
            throws DatabaseException {
        throw new UnsupportedOperationException();
    }

    /**
     * Make sure that value is not null.
     *
     * @param value
     *
     * @return Value or empty string
     */
    private static String protectNull(final String value)
    {
        return ((value != null) ? value : "");
    }

}
