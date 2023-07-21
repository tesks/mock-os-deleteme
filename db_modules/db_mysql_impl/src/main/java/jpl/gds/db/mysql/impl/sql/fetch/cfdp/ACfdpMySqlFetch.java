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
import jpl.gds.db.api.sql.fetch.PreFetchType;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.mysql.impl.sql.fetch.AbstractMySqlFetch;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.DbTimeUtility;
import org.springframework.context.ApplicationContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.List;

/**
 * {@code ACfdpMySqlFetch} is an abstract class for CFDP-specific MySql fetch classes.
 *
 * @version MPCS-10869 -  Moved getSqlWhere function from child classes to here.
 */
public abstract class ACfdpMySqlFetch extends AbstractMySqlFetch {

    protected boolean useContext;
    protected IDbSessionPreFetch spf;
    protected IDbContexConfigPreFetch cpf;

    /**
     * The abbreviation that should as a variable name in an SQL statement using the
     * CFDP File Generation table
     */
    private final String tableAbbrev;

    /**
     * Creates an instance of ACfdpMySqlFetch.
     *
     * @param appContext   the Spring Application Context
     * @param printSqlStmt The flag that indicates whether the fetch class should print out
     *                     the SQL statement only or execute it.
     */
    protected ACfdpMySqlFetch(final ApplicationContext appContext, final boolean printSqlStmt) {
        super(appContext, printSqlStmt);
        tableAbbrev = queryConfig.getTablePrefix(getDbTable());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends IDbRecord> get(IDbContextInfoProvider tsi,
                                         DatabaseTimeRange range,
                                         int batchSize,
                                         Object... params) throws DatabaseException {
        return get(tsi, range, batchSize, false, params);
    }

    /**
     * Utility method for populating context and session information into database query object.
     *
     * @param trace    Tracer object for logging
     * @param spf      Session prefetch object
     * @param warnings Warnings object for logging
     * @param dp       Database query object
     * @param results  Results object from SQL-fetch of database
     * @throws SQLException      Exception thrown related to SQL
     * @throws DatabaseException Exception thrown related to AMPCS database
     */
    protected void populateUpdaterWithContextAndSessionInfo(final Tracer trace,
                                                            final IDbSessionPreFetch spf,
                                                            final IDbContexConfigPreFetch cpf,
                                                            final List<SQLWarning> warnings,
                                                            final IDbQueryable dp,
                                                            final ResultSet results) throws SQLException, DatabaseException {
        final long contextId = this.results.getLong(CONTEXT_ID);

        dp.setContextId(contextId);

        final long sessionId = this.results.getLong(SESSION_ID);

        dp.setSessionId(sessionId);

        warnings.clear();

        // MPCS-10094 No session fragment in CFDP archive artifacts
        // dp.setSessionFragment(SessionFragmentHolder.getFromDbRethrow(results, FRAGMENT_ID, warnings));
        //SqlExceptionTools.logWarning(warnings, trace);

        final int contextHostId = results.getInt(CONTEXT_HOST_ID);
        final int hostId = results.getInt(HOST_ID);

        dp.setContextHostId(contextHostId);

        //context or session host
        final String contextHost = useContext ? cpf.lookupHost(contextHostId) : spf.lookupHost(hostId);

        if ((contextHost == null) || (contextHost.length() == 0)) {
            throw new DatabaseException("Unable to get contextHost for " + " contextHostId " + contextHostId);
        }

        dp.setContextHost(contextHost);

        dp.setSessionHostId(hostId);

        //context or session host
        final String sessionHost = useContext ? cpf.lookupHost(contextHostId) : spf.lookupHost(hostId);

        if ((sessionHost == null) || (sessionHost.length() == 0)) {
            throw new DatabaseException("Unable to get sessionHost for " + " hostId " + hostId);
        }

        dp.setSessionHost(sessionHost);
    }

    public List<? extends IDbRecord> get(final String tableAbbrev,
                                         final IDbContextInfoProvider tsi, DatabaseTimeRange range, final int batchSize,
                                         final boolean useContext, final Object... params) throws DatabaseException {
        this.useContext = useContext;

        if (range == null) {
            range = new DatabaseTimeRange(DatabaseTimeType.EVENT_TIME);
        }

        initQuery(batchSize);

        String whereClause = "";
        String contextClause = "";

        spf = fetchFactory.getSessionPreFetch(false, PreFetchType.NORMAL);
        cpf = fetchFactory.getContextConfigPreFetch();

        try {
            //when we have session ID, session + context, or none, join with Session table
            if (!useContext) {

                if (printStmtOnly) {
                    // Dummy to get SQL statement printed
                    final IDbSessionPreFetch dummy = fetchFactory.getSessionPreFetch(true, PreFetchType.NORMAL);

                    try {
                        dummy.get(tsi);
                    } finally {
                        dummy.close();
                    }

                }

                spf.get(tsi);
                whereClause = spf.getIdHostWhereClause(tableAbbrev);
                contextClause = createContextIdClause(tsi.getParentKeyList(), tableAbbrev, true);
                whereClause = addToWhere(whereClause, contextClause);

            } else {
                //when we only have context ID, join with Context table
                if (printStmtOnly) {
                    // Dummy to get SQL statement printed
                    final IDbContexConfigPreFetch dummy = fetchFactory.getContextConfigPreFetch(true);

                    try {
                        dummy.get(tsi);
                    } finally {
                        dummy.close();
                    }

                }

                cpf.get(tsi);
                whereClause = cpf.getIdHostWhereClause(tableAbbrev);
            }

            whereClause = getSqlWhereClause(whereClause, range, params);
        } finally {
            spf.close();
            cpf.close();
        }

        return populateAndExecute(tsi, range, whereClause, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSqlWhereClause(final String testSqlTemplate, final DatabaseTimeRange range,
                                    final Object... params) throws DatabaseException {
        if (testSqlTemplate == null) {
            throw new IllegalArgumentException("Input test information template was null");
        } else if (range == null) {
            throw new IllegalArgumentException("The input time range information was null");
        }

        String sqlWhere = null;

        if (!testSqlTemplate.equals("")) {
            sqlWhere = addToWhere(sqlWhere, testSqlTemplate);
        }

        if (!range.getTimeType().equals(DatabaseTimeType.EVENT_TIME)) {
            TraceManager.getTracer(Loggers.DB_FETCH).warn("AMPCS CFDP tables do not support the time type " + range.getTimeType() + ". Results will not be filtered by time.");
        } else {

            String timeWhere = DbTimeUtility.generateTimeWhereClause(tableAbbrev, range, false, false,
                    _extendedDatabase);

            if (timeWhere.length() > 0) {
                sqlWhere = addToWhere(sqlWhere, timeWhere);
            }
        }

        // Add the ordering

        final StringBuilder sb = new StringBuilder();

        if (sqlWhere != null) {
            sb.append(sqlWhere);
        }

        return sb.toString();
    }

    abstract protected String getDbTable();

    protected String getTableAbbrev() {
        return this.tableAbbrev;
    }

}