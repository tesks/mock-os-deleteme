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

import jpl.gds.common.config.types.VenueType;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.IDbInteractor;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.PreFetchType;
import jpl.gds.db.api.sql.store.ISessionStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;


/**
 * Read session id and host id from DB. Used as pre-fetch to allow subsequent
 * queries to be more efficient by supplying full session id and host id in
 * where clauses.
 *
 */
public class SessionPreFetch extends AbstractMySqlFetch implements Closeable, IDbSessionPreFetch
{
    /** MPCS-6032 Add Closeable */
    private final String                             _tableAbbrev;

    private final int                                _batchSize;
    private final String                             _sessionKey;
    private final String                             _host;
    private final String                             _hostIdCol;
    private final String                             _fragment;
    private final String                             _scid;
    private final String                             _od;
    private final String                             _vt;

    // MPCS-8019- add abbreviation for dssId
    private final String                             DSS;

    // MPCS-8019 - add dssId to data queried in all cases
    private final String                             _selectClause;

    private final String                             _selectClauseScid;

    private final String                             _selectClauseOd;

    // The host ids and ids returned from the database
    private final Map<Integer, HostAggregate>        _hostAggregates;

    // Lookup host id/id to SCID in two steps
    private final Map<Integer, Map<Long, Integer>>   _scids;

    // Lookup host id/id to OD in two steps
    private final Map<Integer, Map<Long, String>>    _ods;

    // Lookup host id/id to venue type in two steps
    private final Map<Integer, Map<Long, VenueType>> _vts;

    // MPCS-8019 - added
    // Lookup host id/id to dss in two steps
    private final Map<Integer, Map<Long, Integer>>   _dss;

    // Lookup host from hostId
    private final Map<Integer, String>               _hosts;

    // Specifies what extra data (if any) we need to fetch
    private final PreFetchType                       _preFetchType;

	/**
     * Creates an instance of SessionPreFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            True means just print statement.
     * @param preFetchType
     *            Specify extra data, if any
     */
	public SessionPreFetch(final ApplicationContext appContext, final boolean printSqlStmt,
			final PreFetchType preFetchType) {
		super(appContext, printSqlStmt);
		_preFetchType = preFetchType;

		/** MPCS-6032 Add Closeable */
		_tableAbbrev = queryConfig.getTablePrefix(ISessionStore.DB_SESSION_DATA_TABLE_NAME);

		_batchSize = 1000;
		_sessionKey = _tableAbbrev + "." + SESSION_ID;
		_host = _tableAbbrev + "." + "host";
		_hostIdCol = _tableAbbrev + "." + HOST_ID;
		_fragment = _tableAbbrev + "." + FRAGMENT_ID;
		_scid = _tableAbbrev + "." + "spacecraftId";
		_od = _tableAbbrev + "." + "outputDirectory";
		_vt = _tableAbbrev + "." + "venueType";
		// MPCS-8019  - add abbreviation for dssId
		DSS = _tableAbbrev + "." + "dssId";

		// MPCS-8019  - add dssId to data queried in all cases
		_selectClause = "SELECT " + _sessionKey + "," + _host + "," + _hostIdCol + "," + _vt + "," + DSS + " FROM "
				+ ISessionStore.DB_SESSION_DATA_TABLE_NAME + " AS " + _tableAbbrev;

		_selectClauseScid = "SELECT " + _sessionKey + "," + _host + "," + _hostIdCol + "," + _scid + "," + _vt + ","
				+ DSS + " FROM " + ISessionStore.DB_SESSION_DATA_TABLE_NAME + " AS " + _tableAbbrev;

		_selectClauseOd = "SELECT " + _sessionKey + "," + _host + "," + _hostIdCol + "," + _od + "," + _vt + "," + DSS
				+ " FROM " + ISessionStore.DB_SESSION_DATA_TABLE_NAME + " AS " + _tableAbbrev;

		// The host ids and ids returned from the database
		_hostAggregates = new HashMap<Integer, HostAggregate>();

		// Lookup host id/id to SCID in two steps
		_scids = new HashMap<Integer, Map<Long, Integer>>();

		// Lookup host id/id to OD in two steps
		_ods = new HashMap<Integer, Map<Long, String>>();

		// Lookup host id/id to venue type in two steps
		_vts = new HashMap<Integer, Map<Long, VenueType>>();

		// MPCS-8019 - added
		// Lookup host id/id to dss in two steps
		_dss = new HashMap<Integer, Map<Long, Integer>>();

		// Lookup host from hostId
		_hosts = new HashMap<Integer, String>();
	}

    /**
     * Creates an instance of SessionPreFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            True means just print statement.
     */
    public SessionPreFetch(final ApplicationContext appContext, final boolean printSqlStmt)
    {
        this(appContext, printSqlStmt, PreFetchType.NORMAL);
    }


    /**
     * Creates an instance of SessionPreFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public SessionPreFetch(final ApplicationContext appContext)
    {
        this(appContext, false, PreFetchType.NORMAL);
    }

    @Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, final DatabaseTimeRange range, final int batchSize, final Object... params)
            throws DatabaseException {
        throw new UnsupportedOperationException();
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionPreFetch#get(jpl.gds.db.api.types.DatabaseSessionInfo)
     */
    @Override
    public void get() throws DatabaseException
    {
        /* MPCS-9572 - use DB session factory rather than archive controller */
        get(this.dbSessionInfoFactory.createQueryableProvider());
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionPreFetch#get(jpl.gds.db.api.types.DatabaseSessionInfo)
     */
    @Override
    public void get(final IDbContextInfoProvider tsi) throws DatabaseException
    {
        if (tsi == null)
        {
            throw new IllegalArgumentException("Null session info");
        }
    
        if (! dbProperties.getUseDatabase())
        {
            return;
        }
    
        // Initialize the fetch query
    
        initQuery(_batchSize); // No batching is actually done
    
        // Build the actual query string
    
        String select = _selectClause;
    
        switch (_preFetchType)
        {
            case GET_SCID:
                select = _selectClauseScid;
                break;
            case GET_OD:
                select = _selectClauseOd;
                break;
            default:
                select = _selectClause;
                break;
        }
    
        final StringBuilder query = new StringBuilder(select);
        final String where = StringUtil.safeTrim(tsi.getSqlTemplate(_tableAbbrev));
    
        if (!where.isEmpty()){
            query.append(" WHERE ");
            query.append(where);
        }

        // MPCS-10431 - Removed hardcoded sessionFragment = 1
    
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
            dbSessionInfoFactory.convertProviderToUpdater((IDbSessionInfoProvider) tsi).fillInSqlTemplate(1, statement);

            if (printStmtOnly)
            {
                printSqlStatement(statement, "Pre-query");
            }
            else
            {
                results = statement.executeQuery();
            }
    
            if (results != null)
            {
                while (true)
                {
                    if (! results.next())
                    {
                        break;
                    }
    
                    final String  host   = protectNull(results.getString(_host));
                    final int     hostId = results.getInt(_hostIdCol);
                    final long    id     = results.getLong(_sessionKey);
                    HostAggregate ha     = _hostAggregates.get(hostId);
    
                    if (ha == null)
                    {
                        ha = new HostAggregate(hostId);
    
                        _hostAggregates.put(hostId, ha);
                    }
    
                    ha.addId(id);
    
                    final String vt =
                    	protectNull(results.getString(_vt));
                    
                    Map<Long, VenueType> vtMap = _vts.get(hostId);
                    
                    if (vtMap == null)
                    {
                    	vtMap = new HashMap<Long, VenueType>();
                    	
                    	_vts.put(hostId, vtMap);
                    }
                    
                    VenueType vtEnum = VenueType.UNKNOWN;
    
                    try
                    {
                    	vtEnum = VenueType.valueOf(vt);
                    }
                    catch (final IllegalArgumentException e)
                    {
                    	e.printStackTrace();
                    }
    
                    vtMap.put(id, vtEnum);
                    
                    // MPCS-8019  - add storing of dss in the dssMap
                    final Integer dss = protectNull(results.getInt(DSS));
                    
                    Map<Long, Integer> dssMap = _dss.get(hostId);
                    if(dssMap == null)
                    {
                    	dssMap = new HashMap<Long, Integer>();
                    	
                    	_dss.put(hostId,dssMap);
                    }
                    
                    dssMap.put(id, dss);
    
                    // Be able to lookup from hostId to host
                    _hosts.put(hostId, host);
    
                    switch (_preFetchType)
                    {
                        case GET_SCID:
                            final Integer scid = results.getInt(_scid);
    
                            Map<Long, Integer> scidMap = _scids.get(hostId);
    
                            if (scidMap == null)
                            {
                                scidMap = new HashMap<Long, Integer>();
    
                                _scids.put(hostId, scidMap);
                            }
    
                            scidMap.put(id, scid);
                            break;
    
                        case GET_OD:
                            final String od =
                                protectNull(results.getString(_od));
    
                            Map<Long, String> odMap = _ods.get(hostId);
    
                            if (odMap == null)
                            {
                                odMap = new HashMap<Long, String>();
    
                                _ods.put(hostId, odMap);
                            }
    
                            odMap.put(id, od);
                            break;
    
                        default:
                            break;
                    }
    
                    // Handle any unhandled warnings
                    /** MPCS-6718 */
                    SqlExceptionTools.logWarning(trace, results);

                }
            }
        }
        catch (final SQLException sqle)
        {
            throw new DatabaseException("Error retrieving pre-sessions: " +
                                       sqle.getMessage(),
                                   sqle);
        }
        finally
        {
            if (results != null)
            {
                try {
                    results.close();
                }
                catch (final SQLException e) {
                   throw new DatabaseException("Error closing Result Set.", e);
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
                    throw new DatabaseException("Error closing Prepared Statement.", e);
                }
                finally {
                    statement = null;
                }
            }
    
            // Do not close, chill_status_publisher reuses.
            // close();
        }
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionPreFetch#getIdHostWhereClause(java.lang.String)
     */
    @Override
    public String getIdHostWhereClause(final String abbrev)
    {
        return getIdHostWhereClause(abbrev, null, HostFetch.TABLE_ABBREV);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionPreFetch#getIdHostWhereClause(java.lang.String, java.lang.String)
     */
    @Override
    public String getIdHostWhereClause(final String abbrev,
                                       final String abbrev2)
    {
        return getIdHostWhereClause(abbrev, abbrev2, HostFetch.TABLE_ABBREV);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionPreFetch#getIdHostWhereClause(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String getIdHostWhereClause(final String abbrev,
                                       final String abbrev2,
                                       final String abbrevHost)
    {
        final int size = _hostAggregates.size();

        if (size == 0)
        {
            return "(0=1)"; // Impossible where clause
        }

        final StringBuilder sb = new StringBuilder(1024);

        if (size > 1)
        {
            sb.append('(');
        }

        boolean first = true;

        for (final HostAggregate ha : _hostAggregates.values())
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(" OR ");
            }

            sb.append('(');

            ha.produceWhere(abbrev, sb, IDbInteractor.SESSION_ID, HOST_ID);

            sb.append(')');
        }

        if (size > 1)
        {
            sb.append(')');
        }

        return sb.toString();
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionPreFetch#getNextResultBatch()
     */
	@Override
    public List<IDbQueryable> getNextResultBatch()
            throws DatabaseException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * Not used here.
     */
	@Override
    public String getSqlWhereClause(
            final String                 testSqlTemplate,
            final DatabaseTimeRange      range,
            final Object...              params) throws DatabaseException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * Not used here.
     */
    @Override
	protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
            final String whereClause, final Object... params) throws DatabaseException {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     * 
     * Not used here.
     */
	@Override
	protected List<? extends IDbRecord> getResults() throws DatabaseException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String lookupHost(final int hostId)
    {
        return protectNull(_hosts.get(hostId));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int lookupSCID(final Integer hostId,
                          final Long    id)
    {
        final Map<Long, Integer> scidMap = _scids.get(hostId);

        if (scidMap == null)
        {
            // Nothing found for that host

            return 0;
        }

        return protectNull(scidMap.get(id));
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionPreFetch#lookupOD(java.lang.Integer, java.lang.Long)
     */
    @Override
    public String lookupOD(final Integer hostId,
                           final Long    id)
    {
        final Map<Long, String> odMap = _ods.get(hostId);

        if (odMap == null)
        {
            // Nothing found for that host

            return "";
        }

        return protectNull(odMap.get(id));
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionPreFetch#lookupDss(java.lang.Integer, java.lang.Long)
     */
    @Override
    public int lookupDss(final Integer hostId,
    		             final Long    id)
    {
    	final Map<Long, Integer> dssMap = _dss.get(hostId);
    	
    	if (dssMap == null){
    		
    		// Nothing found for that host
    		
    		return 0;
    	}
    	
    	return protectNull(dssMap.get(id));
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionPreFetch#lookupVT(java.lang.Integer, java.lang.Long)
     */
    @Override
    public VenueType lookupVT(final Integer hostId,
                              final Long    id)
    {
        final Map<Long, VenueType> vtMap = _vts.get(hostId);

        if (vtMap == null)
        {
            // Nothing found for that host

            return VenueType.UNKNOWN;
        }

        final VenueType vt = vtMap.get(id);

        return ((vt != null) ? vt : VenueType.UNKNOWN);
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


    /**
     * Make sure that value is not null.
     *
     * @param value
     *
     * @return Value or empty string
     */
    private static Integer protectNull(final Integer value)
    {
        return ((value != null) ? value : Integer.valueOf(0));
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSessionPreFetch#lookupVenue(java.lang.Integer, java.lang.Long)
     */
	@Override
    public VenueType lookupVenue(final Integer hostId,
                                 final Long    sessionId)
    {
        return lookupVT(hostId, sessionId);
	}
}
