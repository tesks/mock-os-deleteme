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

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IContextConfigFetch;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.types.IDbContextConfigFactory;
import jpl.gds.db.api.types.IDbContextConfigProvider;
import jpl.gds.db.api.types.IDbContextConfigUpdater;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.shared.types.Pair;

import org.springframework.context.ApplicationContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *  This is the database read/retrieval interface to the context config table in the
 *  * MPCS database.  This class will retrieve one or more context configurations from the database
 *  * based on a number of different query input parameters.
 *  *
 *  * The general way to use this class is:
 *  * <ol>
 *  * <li>Create a ContextConfigFetch object and set all the information on it that should be used
 *  * to search the database for context configurations</li>
 *  * <li>Call one of the "getContextConfig(...)" methods and specify a batch size for the size
 *  * of lists that should be returned.  The methods will return only
 *  * the first batch of results.</li>
 *  * <li>Make further calls to getNextResultBatch() to retrieve the rest of the results from
 *  * the query</li>
 *  *
 *  * Note that we need to do a join on the ContextConfigKeyValue table in order to get the
 *  * key-value mappings.
 *
 */
public class ContextConfigFetch extends AbstractMySqlFetch implements IContextConfigFetch {
    private static final int RESULT_SIZE = 1024;

    //field names in DB
    private static final String FIELD_CONTEXT_ID = "contextId";
    private static final String FIELD_HOST_ID = "hostId";
    private static final String FIELD_HOST_NAME = "host";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_USER = "user";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_MPCS_VERSION = "mpcsVersion";
    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_PARENT_ID = "parentId";
    private static final String FIELD_KEY = "keyName";
    private static final String FIELD_VALUE = "value";

    /**
     * The SELECT and FROM portions of the SQL query that will be used to
     * retrieve context config information from the database.
     */

    private final String selectClause = queryConfig.getQueryClause(QueryClauseType.CONTEXT_JOIN,
                                                                   DB_CONTEXT_TABLE_NAME);

    /** debug flag */
    private static final boolean DEBUG = false;

    private final IDbContextConfigFactory dbConfigFactory;

    //True if it's the very first set of results from a query hat are being fetched
    private boolean isFirst;

     // The current context key that was read from the database (key: contextId, sessionId)
    private Pair<Integer, Integer> currentKey;

    // The next context key read from the database
    private Pair<Integer, Integer> nextKey;

    /**
     * The current context being parsed
     */
    private IDbContextConfigUpdater currentContext;

    /**
     *
     * Creates an instance of ContextConfigFetch
     *
     * @param appContext the Spring Application Context
     * @param printSqlStmt The flag that indicates whether the fetch class should print out the SQL statement only or
     *                    execute it.
     */
    public ContextConfigFetch(final ApplicationContext appContext, final boolean printSqlStmt) {
        super(appContext, printSqlStmt);
        this.isFirst    = true;
        this.nextKey    = null;
        this.currentKey = null;
        this.currentContext = null;
        dbConfigFactory = appContext.getBean(IDbContextConfigFactory.class);
    }

    /**
     * Creates an instance of ContextConfigFetch.
     *
     * @param appContext
     *            the Spring Application Context
     */
    public ContextConfigFetch(final ApplicationContext appContext) {
        this(appContext, false);
    }

    @Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
                                         final int batchSize, final Object... params)
            throws DatabaseException {

        this.nextKey    = null;
        this.currentKey = null;
        this.currentContext = null;

        if(tsi == null)
        {
            throw new IllegalArgumentException("Null context configuration input information");
        }
        else if(batchSize <= 0)
        {
            throw new IllegalArgumentException("Invalid batch size of " + batchSize + " passed in.");
        }

        if (!this.dbProperties.getUseDatabase())
        {
            return(new ArrayList<IDbContextConfigProvider>());
        }

        //initialize the fetch query
        initQuery(batchSize);

        String whereClause = getSqlWhereClause(tsi.getSqlTemplate(DB_CONTEXT_TABLE_ABBREV), range, tsi, params);
        String sessionClause = createContextIdClause(tsi.getParentKeyList(), DB_CONTEXT_TABLE_ABBREV, false);
        whereClause = addToWhere(whereClause, sessionClause);

        return populateAndExecute(tsi,range,whereClause,params);
    }

    @Override
    public List<? extends IDbRecord> getNextResultBatch() throws DatabaseException {
        this.isFirst = false;
        return getResults();
    }

    @Override
    public String getSqlWhereClause(final String testSqlTemplate, final DatabaseTimeRange range,
                                    final Object... params)
            throws DatabaseException {
        if (testSqlTemplate == null) {
            throw new IllegalArgumentException("Input test information template was null");
        }

        String sqlWhere = "";

        if (!testSqlTemplate.equals("")) {
            sqlWhere = addToWhere(sqlWhere, testSqlTemplate);
        }

        final String timeWhere = DbTimeUtility.generateTimeWhereClause(DB_CONTEXT_TABLE_ABBREV, range, false, false,
                                                                       _extendedDatabase);

        if (timeWhere.length() > 0) {
            sqlWhere = addToWhere(sqlWhere, timeWhere);
        }

        return sqlWhere;
    }

    @Override
    protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi,
                                                           final DatabaseTimeRange range,
                                                           final String whereClause, final Object... params)
            throws DatabaseException {
        if(tsi == null)
        {
            throw new IllegalArgumentException("The input context configuration information was null");
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
                    selectClause + (whereClause != null ? whereClause : ""),
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);

            this.statement.setFetchSize(Integer.MIN_VALUE);


            //fill in all the parameters for the context
            dbContextInfoFactory.convertProviderToUpdater(tsi).fillInSqlTemplate(i, this.statement);

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
            throw new DatabaseException("Error retrieving context configs from database: " + e.getMessage());
        }

        this.isFirst = true;
        return getResults();
    }

    /*
     * This retrieval is trickier than the others. The select query does a left join on the ContextConfig and
     * ContextConfigKeyValue tables, so each config ID will show up once for each piece of metadata it has.
     *
     * For instance, if context with ID=1 has the key/value pairs (key1,value1), (key2,value2), and (key3,value3), then
     * that context alone will return three rows from our query.  So when grabbing results, we don't have a new
     * context on each call to results.next(), but instead we have a new context result when the current context ID does
     * match the the ID of the context we get out of the result set.
     *
     * Because we have to read the next context out before knowing if its ID is the same as or different from the
     * current context ID, we need to be able to store the keys and context in this object until the user comes back
     * to get more results.
     */
    @Override
    protected List<? extends IDbContextConfigUpdater> getResults() throws DatabaseException {
        final List<IDbContextConfigUpdater> refs = new ArrayList<>(RESULT_SIZE);

        //if no query was executed or we're done with the current
        //result set, just return an empty list
        if(this.results == null)
        {
            return(refs);
        }

        try {
            // loop through until we fill up our first batch or we've
            // got no more results
            while (refs.size() < this.batchSize) {
                //if this is the first trip to this method after a query, we need to grab
                //the first result from the result set
                if(this.isFirst) {
                    this.isFirst = false;
                    if(!this.results.next()) {
                        break;
                    }
                }

                // Read the unique context ID from the next row in the results (contextId, hostId, id)
                this.nextKey = new Pair<>(results.getInt(FIELD_CONTEXT_ID), results.getInt(FIELD_HOST_ID));

                // If the next key matches our current key, then the result row is just another metadata
                // keyword/value pair for our current context

                if (this.nextKey.equals(this.currentKey)){
                    final String keyword = this.results.getString(FIELD_KEY);
                    final String value   = this.results.getString(FIELD_VALUE);
                    currentContext.setValue(keyword, value);

                }
                else{
                    //if the next key doesn't match our current key, then the result row is a new context
                    // add the current context to the output list (if there is one)

                    //create a new context for the new row we just read
                    currentContext = dbConfigFactory.createQueryableUpdater();
                    currentContext.setContextId(results.getLong(FIELD_CONTEXT_ID));
                    currentContext.setSessionHostId(results.getInt(FIELD_HOST_ID));
                    currentContext.setSessionHost(results.getString(FIELD_HOST_NAME));
                    currentContext.setType(results.getString(FIELD_TYPE));
                    currentContext.setUser(results.getString(FIELD_USER));
                    currentContext.setName(results.getString(FIELD_NAME));
                    currentContext.setMpcsVersion(results.getString(FIELD_MPCS_VERSION));
                    currentContext.setSessionId(results.getLong(FIELD_SESSION_ID));
                    currentContext.setParentId(results.getLong(FIELD_PARENT_ID));
                    if(results.getString(FIELD_KEY) != null) {
                        currentContext.setValue(results.getString(FIELD_KEY), results.getString(FIELD_VALUE));
                    }

                    // Grab the first metadata key/value pair for the new EVR we just read. It need not have one.
                    final String keyword = this.results.getString(FIELD_KEY);
                    final String value   = this.results.getString(FIELD_VALUE);
                    if ((keyword != null) || (value != null)) {
                        this.currentContext.setValue(keyword, value);
                    }

                    refs.add(this.currentContext);
                }

                //set the keys equal before the next result read
                this.currentKey = this.nextKey;

                // this actually advances the iterator to the next result
                // AND checks if there is a next result at the same time
                if(!this.results.next()) {
                    break;
                }

                SqlExceptionTools.logWarning(trace, results);
            }

            // if we're all done with results, clean up all the resources
            if (this.results.isAfterLast()) {
                this.results.close();
                this.statement.close();

                this.results = null;
                this.statement = null;
            }
        } catch (final SQLException e) {
            throw new DatabaseException("Error retrieving context configs from database: " + e.getMessage(), e);
        }

        return refs;
    }
}
