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

import jpl.gds.cfdp.data.api.*;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpIndicationFetch;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpIndicationLDIStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.cfdp.IDbCfdpIndicationFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpIndicationProvider;
import jpl.gds.db.api.types.cfdp.IDbCfdpIndicationUpdater;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.DbTimeUtility;
import org.springframework.context.ApplicationContext;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the database read/retrieval interface to the CfdpIndication table in
 * the MPCS database. This class will retrieve one or more indications from the
 * database based on a number of different query input parameters.
 * <p>
 * The general way to use this class is:
 * <ol>
 * <li>Create a TestSessionInfo object and set all the information on it that
 * should be used to search the database for test sessions</li>
 * <li>Create any other necessary query values</li>
 * <li>Call one of the "getCfdpIndications(...)" methods and specify a batch
 * size for the size of lists that should be returned. The
 * "getCfdpIndications(...)" methods will return only the first batch of
 * results.</li>
 * <li>Make further calls to getNextResultBatch() to retrieve the rest of the
 * results from the query</li>
 *
 * @version MPCS-10869 -  added Direction enum, updated getSqlWhere to extend ACfdpMySqlFetch (add filtering
 * by direction and stubs for filtering by final/nonfinal and last states)
 */
public class CfdpIndicationFetch extends ACfdpMySqlFetch implements ICfdpIndicationFetch {

    public enum Direction {
        IN,
        OUT,
        ALL
    }

    protected String getDbTable() {
        return ICfdpIndicationLDIStore.DB_CFDP_INDICATION_DATA_TABLE_NAME;
    }
    protected String getDbTable1() { return getDbTable() + "1"; }

    protected String getDirectionField() {
        return getTableAbbrev() + ".transactionDirection";
    }

    private final IDbCfdpIndicationFactory dbCfdpIndicationFactory;

    /**
     * Creates an instance of CfdpIndicationFetch.
     *
     * @param appContext   the Spring Application Context
     * @param printSqlStmt The flag that indicates whether the fetch class should print out
     *                     the SQL statement only or execute it.
     */
    public CfdpIndicationFetch(final ApplicationContext appContext, final boolean printSqlStmt) {
        super(appContext, printSqlStmt);
        dbCfdpIndicationFactory = appContext.getBean(IDbCfdpIndicationFactory.class);
    }

    /**
     * Retrieve a list of database entries based on the search information given
     * in the testSessionInfo input object and the other CFDP Indication value
     * parameters. The batch size will specify how many CFDP Indication results
     * will be returned at a time. This method will return the first batch of
     * results; to get more results from this query, the getNextResultBatch()
     * method should be called. If this method is called a second time before
     * all results from the first call to this method are retrieved, any
     * un-retrieved results will be lost (in other words, this object only
     * remembers the results of one query at a time).
     *
     * @param tsi       The search information used to find contexts in the
     *                  database (may not be null)
     * @param range     The database time range specifying the type of time and time
     *                  range for querying. If null, the default time type and a time
     *                  range of 0 to infinity will be used.
     * @param batchSize The number of results returned at once. This specifies an
     *                  upper limit on the number of results that will be returned at
     *                  once, but there is no lower limit.
     * @param params    Parameters to fill in where-clause
     * @return The list of CFDP Indications that matched the input search
     * information. This list will have a maximum size of "batchSize".
     * @throws DatabaseException if an error occurs
     *                           <p>
     *                          MPCS-9891 - Replaced first parameter with IDbContextInfoProvider
     */
    @Override
    public List<? extends IDbRecord> get(IDbContextInfoProvider tsi, DatabaseTimeRange range, int batchSize, Object... params) throws DatabaseException {
        return get(tsi, range, batchSize, false, params);
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
            return (new ArrayList<IDbCfdpIndicationProvider>());
        }

        return super.get(DB_CFDP_INDICATION_TABLE_ABBREV, tsi, range, batchSize, useContext, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSqlWhereClause(final String testSqlTemplate, final DatabaseTimeRange range,
                                    final Object... params) throws DatabaseException {


        //ACfdpMySqlFetch doesn't use the params!
        String whereClause = super.getSqlWhereClause(testSqlTemplate, range, params).replaceAll("eventTime", "indicationTime");

        //any params are because the request is coming from CommandFetchApp
        final Boolean finl = params.length >= 4 ? (Boolean) params[3] : null;
        final Boolean lastStatusOnly = params.length >= 7 ? (Boolean) params[6] : null;
        Direction direction = Direction.ALL;

        if (params.length >= 8 && params[7] != null) {
            direction = (Direction) params[7];
        }

        if (finl != null) {
            whereClause = addToWhere(whereClause, finalClause(finl));
        }

        //MPCS-11063: Fixed NPE
        if (lastStatusOnly != null && lastStatusOnly) {
            whereClause = addToWhere(whereClause, lastStatusOnlyClause());
        }

        if (direction != Direction.ALL) {
            whereClause = addToWhere(whereClause, directionClause(direction));
        }

        return whereClause;
    }

    private String finalClause(final Boolean finl) {
        StringBuilder sb = new StringBuilder();

        if(!finl) {
            sb.append(" NOT ");
        }

        sb.append("(type='TRANSACTION_FINISHED' or type='ABANDONED') ");

        return sb.toString();
    }

    private String lastStatusOnlyClause() {
        StringBuilder sb = new StringBuilder();

        sb.append('(');
        sb.append(getTableAbbrev()).append("1").append(".type").append(" IS NULL");
        sb.append(')');

        return sb.toString();
    }

    private String directionClause(final Direction direction) {
        if(direction == Direction.ALL) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(getDirectionField()).append("='").append(direction).append("'");
        sb.append(')');

        return sb.toString();
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

        final Boolean lastStatusOnly = params.length >= 7 ? (Boolean) params[6] : null;

        try {
            int i = 1;

            QueryClauseType clauseType = null;

            // Force because pre-query gives us the sessions

            if((lastStatusOnly == null) || !lastStatusOnly) {
                clauseType = QueryClauseType.NO_JOIN;
            } else {
                clauseType = QueryClauseType.JOIN_SINGLE;
            }

            String selectClause = queryConfig.getQueryClause(clauseType,
                    getActualTableName(ICfdpIndicationLDIStore.DB_CFDP_INDICATION_DATA_TABLE_NAME));

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
            throw new DatabaseException("Error retrieving CFDP Indications from database: " + e.getMessage(), e);
        }

        return (getResults());
    }

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.db.api.sql.fetch.AbstractMySqlFetch#getNextResultBatch()
     */
    @Override
    public List<IDbCfdpIndicationProvider> getNextResultBatch() throws DatabaseException {
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
     * @return The list of CFDP Indications that is part of the results of a query executed
     * using the "get(...)" methods. When an empty list is returned,
     * it means no more results are left.
     * @throws DatabaseException If there happens to be an exception retrieving the next set of
     *                           results
     */
    @Override
    protected List<IDbCfdpIndicationProvider> getResults() throws DatabaseException {
        final List<IDbCfdpIndicationProvider> refs = new ArrayList<IDbCfdpIndicationProvider>();

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

                // pull out all the information for this CFDP Indication and add it
                // to the return list
                final IDbCfdpIndicationUpdater dp = dbCfdpIndicationFactory.createQueryableUpdater();

                dp.setIndicationTime(DbTimeUtility.dateFromCoarseFine(this.results.getLong("indicationTimeCoarse"),
                        this.results.getInt("indicationTimeFine")));
                dp.setCfdpProcessorInstanceId(this.results.getString("cfdpProcessorInstanceId"));
                dp.setType(ECfdpIndicationType.valueOf(StringUtil.safeTrim(this.results.getString("type"))));

                final String conditionStr = StringUtil.safeTrim(this.results.getString("faultCondition"));

                if (!this.results.wasNull()) {

                    try {
                        dp.setCondition(ECfdpFaultCondition.valueOf(conditionStr));
                    } catch (IllegalArgumentException iae) {
                        dp.setCondition(ECfdpNonFaultCondition.valueOf(conditionStr));
                    }

                }

                dp.setTransactionDirection(ECfdpTransactionDirection
                        .valueOf(StringUtil.safeTrim(this.results.getString("transactionDirection"))));
                dp.setSourceEntityId(((BigInteger) this.results.getObject("sourceEntityId")).longValue());
                dp.setTransactionSequenceNumber(((BigInteger) this.results.getObject("transactionSequenceNumber")).longValue());
                dp.setServiceClass(this.results.getByte("serviceClass"));
                dp.setDestinationEntityId(((BigInteger) this.results.getObject("destinationEntityId")).longValue());
                dp.setInvolvesFileTransfer(this.results.getBoolean("involvesFileTransfer"));
                dp.setTotalBytesSentOrReceived(((BigInteger) this.results.getObject("totalBytesSentOrReceived")).longValue());
                dp.setTriggeringType(
                        ECfdpTriggeredByType.valueOf(StringUtil.safeTrim(this.results.getString("triggeringType"))));
                dp.setPduId(this.results.getString("pduId"));

                // Populate triggering header if exists
                byte pduHeaderVersion = this.results.getByte("pduHeaderVersion");

                if (!this.results.wasNull()) {
                    // Triggering header exists, populate
                    FixedPduHeader h = new FixedPduHeader();
                    h.setVersion(pduHeaderVersion);
                    h.setType(ECfdpPduType.valueOf(StringUtil.safeTrim(this.results.getString("pduHeaderType"))));
                    h.setDirection(ECfdpPduDirection
                            .valueOf(StringUtil.safeTrim(this.results.getString("pduHeaderDirection"))));
                    h.setTransmissionMode(ECfdpTransmissionMode
                            .valueOf(StringUtil.safeTrim(this.results.getString("pduHeaderTransmissionMode"))));
                    h.setCrcFlagPresent(this.results.getBoolean("pduHeaderCrcFlagPresent"));
                    h.setDataFieldLength(this.results.getShort("pduHeaderDataFieldLength"));
                    h.setEntityIdLength(this.results.getByte("pduHeaderEntityIdLength"));
                    h.setTransactionSequenceNumberLength(
                            this.results.getByte("pduHeaderTransactionSequenceNumberLength"));
                    h.setSourceEntityId(((BigInteger) this.results.getObject("pduHeaderSourceEntityId")).longValue());
                    h.setTransactionSequenceNumber(((BigInteger) this.results.getObject("pduHeaderTransactionSequenceNumber")).longValue());
                    h.setDestinationEntityId(((BigInteger) this.results.getObject("pduHeaderDestinationEntityId")).longValue());
                    dp.setTriggeringPduFixedHeader(h);
                }

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
            throw new DatabaseException("Error retrieving CFDP Indications from database: " + e.getMessage(), e);
        }

        return (refs);
    }

}
