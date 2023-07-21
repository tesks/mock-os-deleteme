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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.ApidRanges;
import jpl.gds.db.api.sql.fetch.IDbProductFetch;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.PreFetchType;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.fetch.QueryConfig;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.mysql.impl.sql.order.ProductOrderByType;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.ProductStatusType;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DataValidityTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.TimeProperties;


/**
 * This is the database read/retrieval interface to the Product table in the
 * MPCS database.  This class will retrieve one or more products from the database
 * based on a number of different query input parameters.
 * 
 * The general way to use this class is:
 * <ol>
 * <li>Create a TestSessionInfo object and set all the information on it that should be used
 * to search the database for test sessions</li>
 * <li>Create any other necessary query values such as apid</li>
 * <li>Call one of the "getProducts(...)" methods and specify a batch size for the size
 * of lists that should be returned.  The "getProducts(...)" methods will return only
 * the first batch of results.</li>
 * <li>Make further calls to getNextResultBatch() to retrieve the rest of the results from
 * the query</li>
 *
 */
public abstract class AbstractMySqlProductFetch extends AbstractMySqlFetch implements IDbProductFetch
{
	/** MPCS-8384  This section redone for extended table support. */

	// Templated WHERE clauses for restricting the query

	private final String requestedIdClause = queryConfig.getQueryClause(QueryClauseType.REQUEST_ID_IN_CONDITION,
			getActualTableName(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME));

	private final String completionClause = queryConfig.getQueryClause(QueryClauseType.PRODUCT_PARTIAL_CONDITION,
			getActualTableName(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME));

    /**
     * Make available so we can get outputDirectory
     */
	protected IDbSessionPreFetch spf = null;

    /**
     * Creates an instance of AbstractMySqlProductFetch.
     * 
     * Make available so we can get outputDirectory * @param printSqlStmt The
     * flag that indicates whether the fetch class should print out the SQL
     * statement only or execute it.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            true causes the SQL statement to be only be printed false
     *            causes the SQL statement to be executed
     */
    protected AbstractMySqlProductFetch(final ApplicationContext appContext, final boolean printSqlStmt)
    {
        super(appContext, printSqlStmt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, DatabaseTimeRange range, final int batchSize, final Object... params) throws DatabaseException
    {
        if(tsi == null)
        {
            throw new IllegalArgumentException("Input test session information was null");
        }
        
        if(dbProperties.getUseDatabase() == false)
        {
            return new ArrayList<IDbRecord>();
        }
        
        if(range == null)
        {
            range = new DatabaseTimeRange(DatabaseTimeType.CREATION_TIME);
        }
        
        initQuery(batchSize);

        String whereClause = null;

        // Create pre-fetch query and execute

        if (printStmtOnly)
        {
            // Dummy to get SQL statement printed

            final IDbSessionPreFetch dummy =
                fetchFactory.getSessionPreFetch(true, PreFetchType.GET_OD);
            try{
                dummy.get(tsi);
            }
            finally{
                dummy.close();
            }
        }

        // Must always run, even with printStmtOnly, to populate main query

        spf = fetchFactory.getSessionPreFetch(false, PreFetchType.GET_OD);
        
        try {
            spf.get(tsi);

            whereClause = getSqlWhereClause(spf
                .getIdHostWhereClause(DB_PRODUCT_TABLE_ABBREV), range, params);
        } finally {
            spf.close();
        }
        
        return populateAndExecute(tsi,range,whereClause,params);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getSqlWhereClause(final String testSqlTemplate, final DatabaseTimeRange range,
                                    final Object... params) throws DatabaseException
    {  
        if(testSqlTemplate == null)
        {
            throw new IllegalArgumentException("Input test information template was null");
        }
        
        if(range == null)
        {
            throw new IllegalArgumentException("The input time range information was null");
        }
        
        final ApidRanges apid = (ApidRanges)params[0];
        final String[] requestId = (String[])params[1];
        final Boolean isPartial = (Boolean)params[2];
        IDbOrderByType orderType = (IDbOrderByType)params[3];

		final Set<Integer> vcid =
            SystemUtilities.<Set<Integer>>castNoWarning(params[4]);

        String sqlWhere = null;

        if(!testSqlTemplate.equals(""))
        {
            sqlWhere = addToWhere(sqlWhere, testSqlTemplate);
        }
        
        if((apid != null) && ! apid.isEmpty())
        {
            sqlWhere = addToWhere(sqlWhere, apid.whereClause(DB_PRODUCT_TABLE_ABBREV));
        }
        
        if ((vcid != null) && ! vcid.isEmpty())
        {
            // Product.vcid is not nullable
            sqlWhere = addToWhere(sqlWhere,
                                  generateVcidWhere(vcid, DB_PRODUCT_TABLE_ABBREV, false));
        }

        if(requestId != null)
        {
            StringBuilder requestVals = new StringBuilder("");
            for ( int i = 0; i < requestId.length; ++i ) {
                if ( i < (requestId.length - 1)) {
                    requestVals.append("?,");
                } else {
                    requestVals.append("?");
                }
            }
            requestVals = new StringBuilder(requestedIdClause.replace(QueryConfig.PARAM_SERIES_REPLACEMENT_TOKEN, requestVals.toString()));
            sqlWhere = addToWhere(sqlWhere, requestVals.toString());
        }

        if(isPartial != null)
        {
            sqlWhere = addToWhere(sqlWhere, completionClause);
        }
        
        // Add time filtering
        /** MPCS-8384  Extended support */

        final String timeWhere =
            DbTimeUtility.generateTimeWhereClause(
                DB_PRODUCT_TABLE_ABBREV,
                range,
                true,
                false,
                _extendedDatabase);

        if (timeWhere.length() > 0)
        {
            sqlWhere = addToWhere(sqlWhere, timeWhere);
        }

        switch(range.getTimeType().getValueAsInt())
        {
            case DatabaseTimeType.ERT_TYPE:
                if(orderType == null) {
                    orderType = ProductOrderByType.ERT;
                }
                break;

            case DatabaseTimeType.SCET_TYPE:
                if(orderType == null) {
                    orderType = ProductOrderByType.DVT_SCET;
                }
                break;
                
            case DatabaseTimeType.LST_TYPE:
                // TODO - ADD SOL SUPPORT HERE
                if(orderType == null) {
                    orderType = ProductOrderByType.DVT_LST;
                }
                break;
                
            case DatabaseTimeType.CREATION_TIME_TYPE:
                if(orderType == null) {
                    orderType = ProductOrderByType.CREATION_TIME;
                }
                break;

            case DatabaseTimeType.SCLK_TYPE:
                if(orderType == null) {
                    orderType = ProductOrderByType.DVT_SCLK;
                }
                break;

            /** MPCS-6808 Added */
            case DatabaseTimeType.RCT_TYPE:
                if(orderType == null) {
                    orderType = ProductOrderByType.RCT;
                }
                break;
                
            default:
                throw new DatabaseException("No valid TimeRange type");
        }
        
        // Add the ordering

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
    @SuppressWarnings("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING")
    @Override
    protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
                                                           final String whereClause, final Object... params) throws DatabaseException
    {       
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
        
        final String[] requestId = (String[])params[1];
        final Boolean isPartial = (Boolean)params[2];
        
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
            /** MPCS-8384  Support for extended tables */
            final String selectClause = queryConfig.getQueryClause(clauseType, 
            				getActualTableName(IDbTableNames.DB_PRODUCT_DATA_TABLE_NAME));

            this.statement = getPreparedStatement(
                                 selectClause + whereClause,
                                 ResultSet.TYPE_FORWARD_ONLY, 
                                 ResultSet.CONCUR_READ_ONLY);

            this.statement.setFetchSize(Integer.MIN_VALUE);

            // set the requestId in the query
            if(requestId != null)
            {
                final String[] csvComponents = requestId;
                for (int j=0;j<csvComponents.length;++j)
                {
                    this.statement.setLong (i++,Long.valueOf(csvComponents[j]));
                }
            }

            // set the product completion status in the query
            if(isPartial != null)
            {
                this.statement.setInt(i++,GDR.getIntFromBoolean(isPartial.booleanValue()));
            }

            if (this.printStmtOnly) {
                printSqlStatement(this.statement);
            }
            else {
                this.results = this.statement.executeQuery();
            }
        }
        catch(final SQLException e)
        {
            final DatabaseException se = new DatabaseException("Error retrieving products from database: " + e);

            se.initCause(e);

            throw se;
        }
        
        return(getResults());
    }
    
    /*
     * (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.AbstractMySqlFetch#getNextResultBatch()
     */
    @Override
    public List<? extends IDbRecord> getNextResultBatch() throws DatabaseException
    {
        return(getResults());
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected List<? extends IDbRecord> getResults() throws DatabaseException {
        final List<IDbRecord> refs = new ArrayList<IDbRecord>();

        if (this.results == null) {
            return (refs);
        }

        int count = 0;
        try {
            while (count < this.batchSize) {
                if (this.results.next() == false) {
                    break;
                }

                // pull out all the information for this product and add it
                // to the return list
                refs.add((IDbRecord) createAndPopulateMetadata());
                count++;

                // Handle any unhandled warnings
                /** MPCS-6718*/
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
        }
        catch (final SQLException e) {
            throw new DatabaseException("Error retrieving products from database: " + e.getMessage());
        }

        return (refs);
    }

    /**
     * Create the mission specific product metadata object and populate it with
     * any mission specific values
     * 
     * @return A mission-specific product metadata object with mission-specific
     *         values already set
     * 
     * @throws DatabaseException
     *             If the creation and population of the metadata fails
     */
    protected <T extends IProductMetadataUpdater> T createAndPopulateMetadata() throws DatabaseException {
        return populateMetadata(null);
    }

    /**
     * Populate specified metadata object with any mission specific values
     * 
     * @param pmd
     *            the specified metadata object to populate
     * @return A mission-specific product metadata object with mission-specific
     *         values already set
     * 
     * @throws DatabaseException
     *             If the creation and population of the metadata fails
     */
    protected <T extends IProductMetadataUpdater> T populateMetadata(T pmd) throws DatabaseException {
        if (null == pmd) {
            pmd = createEmptyMetadata();
        }
        if (this.results != null) {
    
            /** MPCS-6809 As unsigned */
            /** MPCS-7639  Use new method */
            /** MPCS-6718 Look for warnings */
    
            final List<SQLWarning> warnings = new ArrayList<SQLWarning>();
    
            // loop through until we fill up our first batch or we've
            // got no more results
            try {
                final Long sessionId = this.results.getLong(DB_PRODUCT_TABLE_ABBREV + "." + SESSION_ID);
                final int hostId = results.getInt(DB_PRODUCT_TABLE_ABBREV + "." + HOST_ID);
                final String sessionHost = spf.lookupHost(hostId);
    
                if ((sessionHost == null) || (sessionHost.length() == 0)) {
                    throw new DatabaseException("Unable to get sessionHost for hostId " + hostId);
                }
    
                /** MPCS-6718 Look for warnings */
    
                warnings.clear();
    
                pmd.setSessionFragment(
                        SessionFragmentHolder.getFromDbRethrow(results, DB_PRODUCT_TABLE_ABBREV + "." + FRAGMENT_ID, warnings));
    
                SqlExceptionTools.logWarning(warnings, trace);

                pmd.setProductCreationTime(
                        DbTimeUtility.dateFromCoarseFine(this.results.getLong(DB_PRODUCT_TABLE_ABBREV + ".creationTimeCoarse"),
                                this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".creationTimeFine")));
    
                pmd.setRct(DbTimeUtility.dateFromCoarseFine(this.results.getLong(DB_PRODUCT_TABLE_ABBREV + ".rctCoarse"),
                        this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".rctFine")));
    
                long coarse = this.results.getLong("dvtScetCoarse");
                int fine = this.results.getInt("dvtScetFine");
    
                /** MPCS-8384  */
                if (! _extendedDatabase)
                {
                    // Convert fine milliseconds to nanoseconds
                    // by multiplying by a million.

                    fine *= DbTimeUtility.SCET_SHORT_CONVERSION;
                }

                /** MPCS-8384  */
                pmd.setScet(
                    new AccurateDateTime(DbTimeUtility.exactFromScetCoarseFine(coarse, fine),
                                         DbTimeUtility.fineFromScetFine(fine)));
    
                final VenueType vt = spf.lookupVenue(hostId, sessionId);
    
                if (missionProps.getVenueUsesSol(vt)
                        && TimeProperties.getInstance().usesLst()) {
                    pmd.setSol(LocalSolarTimeFactory.getNewLst(pmd.getScet(), spf.lookupSCID(hostId, sessionId)));
                }
    
                pmd.setProductVersion(this.results.getString(DB_PRODUCT_TABLE_ABBREV + ".version"));
                pmd.setPartial(this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".isPartial") == 1);
                pmd.setApid(this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".apid"));
                pmd.setProductType(this.results.getString(DB_PRODUCT_TABLE_ABBREV + ".apidName"));
    
                pmd.setVcid(this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".vcid"));
                pmd.setSequenceId(this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".sequenceId"));
                pmd.setSequenceVersion(this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".sequenceVersion"));
                pmd.setCommandNumber(this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".commandNumber"));
                pmd.setXmlVersion(this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".xmlVersion"));
                pmd.setTotalParts(this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".totalParts"));
    
                final long sclkCoarse = this.results.getLong(DB_PRODUCT_TABLE_ABBREV + ".dvtSclkCoarse");
                final long sclkFine = this.results.getLong(DB_PRODUCT_TABLE_ABBREV + ".dvtSclkFine");
    
                pmd.setDvtCoarse(sclkCoarse);
                pmd.setDvtFine(sclkFine);
    
                pmd.setFullPath(this.results.getString(DB_PRODUCT_TABLE_ABBREV + ".fullPath"));
                pmd.setSessionId(sessionId);
                // MPCS-8019  - set sessionDssId from session prefetch
                // data
                pmd.setSessionDssId(spf.lookupDss(hostId, sessionId));
                pmd.setSessionHost(sessionHost);
                pmd.setSessionHostId(hostId);
    
                pmd.setSclk(new DataValidityTime(sclkCoarse, sclkFine));
    
                coarse = this.results.getLong(DB_PRODUCT_TABLE_ABBREV + ".ertCoarse");
                fine = this.results.getInt(DB_PRODUCT_TABLE_ABBREV + ".ertFine");
    
                pmd.setErt(new AccurateDateTime(DbTimeUtility.exactFromErtCoarseFine(coarse, fine),
                        DbTimeUtility.fineFromErtFine(fine)));
    
                final String groundStatus = StringUtil.safeTrim(this.results.getString(DB_PRODUCT_TABLE_ABBREV + ".groundStatus"));
    
                if (groundStatus.length() == 0) {
                    pmd.setGroundStatus(ProductStatusType.UNKNOWN);
                }
                else {
                    pmd.setGroundStatus(Enum.valueOf(ProductStatusType.class, groundStatus));
                }
    
                final String sequenceCategory = results.getString(DB_PRODUCT_TABLE_ABBREV + ".sequenceCategory");
    
                pmd.setSequenceCategory(!results.wasNull() ? sequenceCategory : null);
                final long sequenceNumber = results.getLong(DB_PRODUCT_TABLE_ABBREV + ".sequenceNumber");
    
                pmd.setSequenceNumber(!results.wasNull() ? sequenceNumber : null);
            }
            catch (final SQLException e) {
                throw new DatabaseException("Error retrieving products from result set: " + e.getMessage());
            }
        }
        return pmd;
    }

//  /**
//   * Get the mission-specific select fields for the SQL select query
//   * 
//   * @return A comma-separated list of database fields (with the proper table prefix) and no leading
//   * or trailing commas
//   */
//  protected abstract String getSelectFields();
    
    /* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.IDbProductFetch#countProductsByApid(jpl.gds.db.api.types.DatabaseSessionInfo, java.lang.Boolean)
	 */
    @Override
    public Map<Integer, Integer> countProductsByApid(final IDbSessionInfoProvider dbSession, final Boolean completeOnly) throws DatabaseException {
        if(dbProperties.getUseDatabase() == false)
        {
            return (new HashMap<Integer,Integer>());
        }

        if (! isConnected())
        {
            throw new IllegalStateException(
                          "This connection has already been closed");
        }
        
        final List<Long> keys = dbSession.getSessionKeyList();
        if (keys.isEmpty())
        {
            throw new IllegalArgumentException("There are no test keys available to query for");
        }
        else if(keys.size() > 1)
        {
            throw new IllegalArgumentException("There can only be one test key for a summary query, but there are " + keys.size());
        }

        final HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();

        /** MPCS-8384  Support for extended tables */
        try
        {
            // Case 1: retrieve all products
            if(completeOnly == null)
            {
                this.statement = getPreparedStatement(
                        queryConfig.getQueryClause(QueryClauseType.PRODUCT_APID_SUMMARY, IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME));

                this.statement.setLong(1,keys.get(0));
            }
            // Case 2: retrieve complete products
            else if(completeOnly)
            {
                this.statement = getPreparedStatement(
                        queryConfig.getQueryClause(QueryClauseType.PRODUCT_APID_SUMMARY_COMPLETION, IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME));

                this.statement.setLong(1,keys.get(0));
                this.statement.setInt(2, GDR.getIntFromBoolean(true));          
            }
            // Case 3: retrieve partial products
            else if(!completeOnly)
            {
                this.statement = getPreparedStatement(
                        queryConfig.getQueryClause(QueryClauseType.PRODUCT_APID_SUMMARY_COMPLETION, IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME));

                this.statement.setLong(1,keys.get(0));
                this.statement.setInt(2, GDR.getIntFromBoolean(false));         
            }

            if (this.printStmtOnly)
            {
                printSqlStatement(this.statement);
            }
            else
            {
                ResultSet result = null;

                try
                {
                    result = this.statement.executeQuery(); 

                    while (result.next())
                    {
                        final Integer apid = result.getInt("apid");
                        final int count = result.getInt("number");
                        map.put(apid, count);
                    }
                }
                finally
                {
                    if (result != null)
                    {
                        result.close();
                    }
                }
            }
        }
        catch (final SQLException e)
        {
            throw new DatabaseException("Error getting products from database: " +
                                   e.getMessage());
        }
        finally
        {
            if (this.statement != null)
            {
                try {
                    this.statement.close();
                }
                catch (final SQLException e) {
                    throw new DatabaseException(e.getMessage(), e);
                }
            }

            this.statement = null;
        }

        return map;
    }
}
