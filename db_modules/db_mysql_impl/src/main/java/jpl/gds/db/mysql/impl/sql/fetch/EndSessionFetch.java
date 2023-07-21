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
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.IEndSessionFetch;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.store.IEndSessionStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbEndSessionFactory;
import jpl.gds.db.api.types.IDbEndSessionProvider;
import jpl.gds.db.api.types.IDbEndSessionUpdater;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.DbTimeUtility;


/**
 * Class to fetch EndSession rows from the database.
 *
 */
public class EndSessionFetch extends AbstractMySqlFetch implements IEndSessionFetch
{
    String DB_TABLE = IEndSessionStore.DB_END_SESSION_DATA_TABLE_NAME;

    String selectClause = queryConfig.getQueryClause(QueryClauseType.END_SESSION_SELECT, DB_TABLE);

    /** Table abbreviation */
    String tableAbbrev = queryConfig.getTablePrefix(DB_TABLE);

    private final IDbEndSessionFactory dbEndSessionFactory;

    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public EndSessionFetch(final ApplicationContext appContext) {
        this(appContext, false);
    }


    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            If true, just show SQL statement
     */
    public EndSessionFetch(final ApplicationContext appContext, final boolean printSqlStmt) {
        super(appContext, printSqlStmt);
        dbEndSessionFactory = appContext.getBean(IDbEndSessionFactory.class);
    }

    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IEndSessionFetch#get(jpl.gds.db.api.types.IDbSessionInfoProvider, jpl.gds.shared.time.DatabaseTimeRange, int, java.lang.Object)
	 */
    @Override
	public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, DatabaseTimeRange range, final int batchSize,
            final Object... params) throws DatabaseException {

        if (tsi == null) {
            throw new IllegalArgumentException(
                "Input test session information was null");
        }

        if (this.dbProperties.getUseDatabase() == false) {
            return new ArrayList<IDbEndSessionProvider>();
        }

        if (range == null) {
            range = new DatabaseTimeRange(DatabaseTimeType.SCET);
        }

        initQuery(batchSize);

        String whereClause = null;

        if (printStmtOnly) {
            // Dummy to get SQL statement printed

            final IDbSessionPreFetch dummy = fetchFactory.getSessionPreFetch(true);

            try {
                dummy.get(tsi);
            } finally {
                dummy.close();
            }
        }

        // Must always run, even with printStmtOnly, to populate main query
        IDbSessionPreFetch spf = null;
        try
        {
            spf = fetchFactory.getSessionPreFetch(false);

            spf.get(tsi);

            whereClause =
                getSqlWhereClause(spf.getIdHostWhereClause(tableAbbrev),
                                  range,
                                  tsi,
                                  params);
        }
        finally
        {
            if (spf != null)
            {
                spf.close();
            }
        }

        return populateAndExecute(tsi, range, whereClause, params);
    }

    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IEndSessionFetch#getNextResultBatch()
	 */
    @Override
    public List<IDbEndSessionProvider> getNextResultBatch() throws DatabaseException {
        return getResults();
    }

    @Override
    protected List<IDbEndSessionProvider> getResults() throws DatabaseException {

        final List<IDbEndSessionProvider> refs = new ArrayList<IDbEndSessionProvider>();

        if (this.results == null) {
            return (refs);
        }

        int count = 0;

        try {
            while (count < this.batchSize) {
                if (this.results.next() == false) {
                    break;
                }

                final IDbEndSessionUpdater des = dbEndSessionFactory.createQueryableUpdater();

                des.setSessionId(this.results.getLong(
                    tableAbbrev + "." + SESSION_ID));

                des.setSessionHostId(this.results.getInt(
                    tableAbbrev + "." + HOST_ID));

                final long coarse = this.results.getLong(
                                  tableAbbrev + ".endTimeCoarse");
                final int  fine   = this.results.getInt(tableAbbrev + ".endTimeFine");

                des.setEndTime(DbTimeUtility.dateFromCoarseFine(coarse, fine));

                refs.add(des);

                count++;

                // Handle any unhandled warnings
                /** MPCS-6718  */
                SqlExceptionTools.logWarning(trace, results);
            }

            if (this.results.isAfterLast() == true) {
                this.results.close();
                this.statement.close();

                this.results = null;
                this.statement = null;
            }
        } catch (final SQLException e) {
            throw new DatabaseException("Error retrieving packets from database: "
                    + e.getMessage());
        }

        return refs;
    }

    @Override
    public String getSqlWhereClause(final String testSqlTemplate,
            final DatabaseTimeRange range, final Object... params)
            throws DatabaseException {

        if (testSqlTemplate == null) {
            throw new IllegalArgumentException(
                "Input test information template was null");
        }

        if (range == null) {
            throw new IllegalArgumentException(
                "The input time range information was null");
        }
        
        String sqlWhere = null;

        if (!testSqlTemplate.equals("")) {
            sqlWhere = addToWhere(sqlWhere, testSqlTemplate);
        }

        return sqlWhere;
    }

    @Override
    protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
            final String whereClause, final Object... params) throws DatabaseException {

        if (tsi == null) {
            throw new IllegalArgumentException(
                "The input test session information was null");
        }

        if (range == null) {
            throw new IllegalArgumentException(
                "The input time range information was null");
        }

        if (whereClause == null) {
            throw new IllegalArgumentException(
                "The input where clause was null");
        }

        try {
            this.statement = getPreparedStatement(selectClause + whereClause,
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            this.statement.setFetchSize(Integer.MIN_VALUE);

            if (this.printStmtOnly) {
                printSqlStatement(this.statement, "Main");
            } else {
                this.results = this.statement.executeQuery();
            }

        } catch (final SQLException e) {
            throw new DatabaseException("Error retrieving packets from database: "
                    + e.getMessage());
        }

        return getResults();
    }

}
