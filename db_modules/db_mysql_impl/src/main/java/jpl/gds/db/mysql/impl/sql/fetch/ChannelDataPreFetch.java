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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.fetch.IChannelDataPreFetch;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.store.ldi.IChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILDIStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.time.DatabaseTimeRange;


/**
 * Read channel ids and associated master key from DB. Used as pre-fetch to
 * expand wildcards and modules into channel ids.
 *
 */
public class ChannelDataPreFetch extends AbstractMySqlFetch implements IChannelDataPreFetch
{
    /* BEGIN: MPCS-5254: make channel ID special prefixes
     * configurable */
    /** Prefix for monitor channel values */
    /* TODO: MPCS-8984 : Remove references to database properties specifying MONITOR and HEADER Prefixes */
    private final String                                  MONITOR_PREFIX          = appContext.getBean(IMySqlAdaptationProperties.class)
                                                                                              .getMonitorPrefix();
    
    /** Prefix for header channel values */
    /* TODO: MPCS-8984 : Remove references to database properties specifying MONITOR and HEADER Prefixes */
    private final String                                  HEADER_PREFIX           = appContext.getBean(IMySqlAdaptationProperties.class)
                                                                                              .getHeaderPrefix();
    
    /* END: MPCS-5254: make channel ID special prefixes
     * configurable */

    private static final String AND = " AND ";
    private static final String OR  = " OR ";

    private static final String DB_TABLE = IChannelValueLDIStore.DB_CHANNEL_DATA_TABLE_NAME;

    private final String TABLE_ABBREV   = queryConfig.getTablePrefix(DB_TABLE);
    private final String HOST_ID_COL    = TABLE_ABBREV + "." + HOST_ID;
    private final String SESSION_ID_COL = TABLE_ABBREV + "." + SESSION_ID;
    private final String FROM_SSE       = TABLE_ABBREV + "." + "fromSse";
    private final String CHANNEL_ID     = TABLE_ABBREV + "." + "channelId";
    private final String MODULE         = TABLE_ABBREV + "." + "module";
    private final String CRC            = "crc"; // No abbreviation

    private final String SELECT_CLAUSE = "SELECT "                 +
                                                HOST_ID                   +
                                                ","                       +
                                                SESSION_ID                +
                                                ","                       +
                                                "fromSse"                 +
                                                ","                       +
                                                "channelId"               +
                                                ","                       +
                                                "CRC32(channelId) AS crc" +
                                                " FROM "                  +
                                                DB_TABLE                  +
                                                " AS "                    +
                                                TABLE_ABBREV;

    

    /** MPCS-4920  Get rid of SSE prefix */

    /** FSW channel ids returned from the database, by master key */
    private final Map<Integer, Map<Integer, Set<String>>> _fswChannelIdsMap =
        new TreeMap<Integer, Map<Integer, Set<String>>>();

    /** Monitor channel ids returned from the database, by master key */
    private final Map<Integer, Map<Integer, Set<String>>>
        _monitorChannelIdsMap =
            new TreeMap<Integer, Map<Integer, Set<String>>>();

    /** Header channel ids returned from the database, by master key */
    private final Map<Integer, Map<Integer, Set<String>>> _headerChannelIdsMap =
        new TreeMap<Integer, Map<Integer, Set<String>>>();

    /** SSE channel ids returned from the database, by master key */
    private final Map<Integer, Map<Integer, Set<String>>> _sseChannelIdsMap =
        new TreeMap<Integer, Map<Integer, Set<String>>>();

    /** SSE header channel ids returned from the database, by master key */
    /** MPCS-5008  */
    private final Map<Integer, Map<Integer, Set<String>>> _sseheaderChannelIdsMap =
        new TreeMap<Integer, Map<Integer, Set<String>>>();

    /** MPCS-4920  Do not need bad channel list any more */

    /** channelId => CRC32 lookup */
    private final Map<String, Long> _crc32Map = new HashMap<String, Long>();

    /** FSW channel ids returned from the database */
    private final Set<String> _fswChannelIds = new TreeSet<String>();

    /** Monitor channel ids returned from the database */
    private final Set<String> _monitorChannelIds = new TreeSet<String>();

    /** Header channel ids returned from the database */
    private final Set<String> _headerChannelIds = new TreeSet<String>();

    /** SSE channel ids returned from the database */
    private final Set<String> _sseChannelIds = new TreeSet<String>();

    /** SSE Header channel ids returned from the database */
    /** MPCS-5008 */
    private final Set<String> _sseheaderChannelIds = new TreeSet<String>();

    /** The raw channel ids */
    private final Set<String> _rawChannelIds = new TreeSet<String>();

    /** The raw channel wildcards */
    private final Set<String> _rawChannelWildcards = new TreeSet<String>();

    /** The raw modules */
    private final Set<String> _rawModules = new TreeSet<String>();

    /** The raw module wildcards */
    private final Set<String> _rawModuleWildcards = new TreeSet<String>();

    /** Session pre-fetch object */
    private final IDbSessionPreFetch _spf;


    /**
     * Creates an instance of ChannelDataPreFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            If true, we are just printing the SQL
     * @param spf
     *            Session pre-fetch object
     * @param channelIds
     *            Channel ids (may be wildcards)
     * @param modules
     *            Modules (may be wildcards)
     */
    public ChannelDataPreFetch(final ApplicationContext appContext,
    		                   final boolean         printSqlStmt,
                               final IDbSessionPreFetch spf,
                               final String[]        channelIds,
                               final String[]        modules)
    {
        super(appContext, printSqlStmt);

        if (spf == null)
        {
            throw new IllegalArgumentException("Null session pre-fetch");
        }

        _spf = spf;

        int total = 0;

        if (channelIds != null)
        {
            for (final String c : channelIds)
            {
                if (c == null)
                {
                    continue;
                }

                final String temp = c.trim().toUpperCase();

                if (temp.contains(ILDIStore.WILDCARD1) || temp.contains(ILDIStore.WILDCARD2))
                {
                    _rawChannelWildcards.add(temp);
                }
                else
                {
                    _rawChannelIds.add(temp);
                }

                ++total;
            }
        }

        if (modules != null)
        {
            for (final String m : modules)
            {
                if (m == null)
                {
                    continue;
                }

                final String temp = m.trim().toUpperCase();

                if (temp.contains(ILDIStore.WILDCARD1) || temp.contains(ILDIStore.WILDCARD2))
                {
                    _rawModuleWildcards.add(temp);
                }
                else
                {
                    _rawModules.add(temp);
                }

                ++total;
            }
        }

        if (total == 0)
        {
            throw new IllegalArgumentException("No channel ids or modules");
        }
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#get()
     */
    @Override
    public void get() throws DatabaseException
    {
        // Initialize the fetch query

        initQuery(Integer.MAX_VALUE); // No batching is actually done

        // Build the actual query string

        final StringBuilder query = new StringBuilder(SELECT_CLAUSE);

        // Build where clause

        query.append(" WHERE ");

        // Specify desired host ids and session ids

        query.append(_spf.getIdHostWhereClause(TABLE_ABBREV));

        // We know there is at least one clause.

        // Specify channel-ids

        final String       c1 = produceInWhereClause(_rawChannelIds,
                                                     CHANNEL_ID);
        final List<String> c2 = produceLikeWhereClauses(_rawChannelWildcards,
                                                        CHANNEL_ID);

        if ((c1 != null) || (c2 != null))
        {
            query.append(AND);

            stitch(query, c1, c2);
        }

        // Specify modules

        final String       m1 = produceInWhereClause(_rawModules, MODULE);
        final List<String> m2 = produceLikeWhereClauses(_rawModuleWildcards,
                                                        MODULE);

        if ((m1 != null) || (m2 != null))
        {
            query.append(AND);

            stitch(query, m1, m2);
        }

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

            if (printStmtOnly)
            {
                printSqlStatement(statement, "Channel data pre-query");
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

                    // Get all columns

                    final int     hostId    = results.getInt(HOST_ID_COL);
                    final int     sessionId = results.getInt(SESSION_ID_COL);
                    final boolean fromSSE   = (results.getInt(FROM_SSE) != 0);
                    final long    crc32     = results.getLong(CRC);
                    final String  channelId =
                        results.getString(CHANNEL_ID).trim().toUpperCase();

                    // Keep CRC32
                    _crc32Map.put(channelId, crc32);

                    // Add channel id to maps and sets corresponding to
                    // master key and/or type

                    Map<Integer, Map<Integer, Set<String>>> map = null;
                    Set<String>                             set = null;

                    // MPCS-4920 Get rid of check before else

                    /** MPCS-5008 */
                    final boolean isHeader = channelId.startsWith(
                    		HEADER_PREFIX);
                    
                    if (fromSSE && isHeader)
                    {
                        map = _sseheaderChannelIdsMap;
                        set = _sseheaderChannelIds;
                    }
                    else if (isHeader)
                    {
                        map = _headerChannelIdsMap;
                        set = _headerChannelIds;
                    }
                    else if (fromSSE)
                    {
                        // MPCS-4920 Get rid of check for prefix

                        map = _sseChannelIdsMap;
                        set = _sseChannelIds;
                    }
                    else if (channelId.startsWith(MONITOR_PREFIX))
                    {
                        map = _monitorChannelIdsMap;
                        set = _monitorChannelIds;
                    }
                    else
                    {
                        map = _fswChannelIdsMap;
                        set = _fswChannelIds;
                    }

                    addChannelId(map, hostId, sessionId, channelId);

                    set.add(channelId);

                    // Handle any unhandled warnings
                    /** MPCS-6718 */
                    SqlExceptionTools.logWarning(trace, results);
                }
            }
        }
        catch (final SQLException sqle)
        {
            throw new DatabaseException("Error retrieving channel data", sqle);
        }
        finally
        {
            if (results != null)
            {
                try {
                    results.close();
                }
                catch (final SQLException e) {
                    // ignore
                }

                results = null;
            }

            if (statement != null)
            {
                try {
                    statement.close();
                }
                catch (final SQLException e) {
                    // ignore
                }

                statement = null;
            }
        }
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#get(jpl.gds.db.api.types.IDbSessionInfoProvider, jpl.gds.shared.time.DatabaseTimeRange, int, java.lang.Object)
     */
	@Override
    public List<IDbSessionUpdater> get(final IDbContextInfoProvider tsi,
                                     final DatabaseTimeRange   range,
                                     final int                 batchSize,
                                     final Object...           params)
        throws DatabaseException
    {
        throw new UnsupportedOperationException();
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#getNextResultBatch()
     */
	@Override
    public List<IDbSessionUpdater> getNextResultBatch()
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
                         final String              testSqlTemplate,
                         final DatabaseTimeRange   range,
                         final Object...           params) throws DatabaseException
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
    protected List<IDbSessionUpdater> getResults() throws DatabaseException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Look up channel ids corresponding to master key.
     *
     * @param map       Map to look up in
     * @param hostId    Host id
     * @param sessionId Session id
     *
     * @return Set of Channel ids
     */
    private static Set<String> lookupChannelIds(
        final Map<Integer, Map<Integer, Set<String>>> map,
        final int                                     hostId,
        final int                                     sessionId)
    {
        final Map<Integer, Set<String>> sessionMap = map.get(hostId);

        if (sessionMap == null)
        {
            return null;
        }

        final Set<String> set = sessionMap.get(sessionId);

        if (set == null)
        {
            return null;
        }

        return Collections.unmodifiableSet(set);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupFsWChannelIds(int, int)
     */
    @Override
    public Set<String> lookupFsWChannelIds(final int hostId,
                                           final int sessionId)
    {
        return lookupChannelIds(_fswChannelIdsMap, hostId, sessionId);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupMonitorChannelIds(int, int)
     */
    @Override
    public Set<String> lookupMonitorChannelIds(final int hostId,
                                               final int sessionId)
    {
        return lookupChannelIds(_monitorChannelIdsMap, hostId, sessionId);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupHeaderChannelIds(int, int)
     */
    @Override
    public Set<String> lookupHeaderChannelIds(final int hostId,
                                              final int sessionId)
    {
        return lookupChannelIds(_headerChannelIdsMap, hostId, sessionId);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupSseChannelIds(int, int)
     */
    @Override
    public Set<String> lookupSseChannelIds(final int hostId,
                                           final int sessionId)
    {
        return lookupChannelIds(_sseChannelIdsMap, hostId, sessionId);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupSseHeaderChannelIds(int, int)
     */
    @Override
    public Set<String> lookupSseHeaderChannelIds(final int hostId,
                                                 final int sessionId)
    {
        /** MPCS-5008  New */
        return lookupChannelIds(_sseheaderChannelIdsMap, hostId, sessionId);
    }


    // MPCS-4920  Do not need lookupBadChannelIds any more


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupFswChannelIds()
     */
    @Override
    public Set<String> lookupFswChannelIds()
    {
        return Collections.unmodifiableSet(_fswChannelIds);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupMonitorChannelIds()
     */
    @Override
    public Set<String> lookupMonitorChannelIds()
    {
        return Collections.unmodifiableSet(_monitorChannelIds);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupHeaderChannelIds()
     */
    @Override
    public Set<String> lookupHeaderChannelIds()
    {
        return Collections.unmodifiableSet(_headerChannelIds);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupSseChannelIds()
     */
    @Override
    public Set<String> lookupSseChannelIds()
    {
        return Collections.unmodifiableSet(_sseChannelIds);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupSseHeaderChannelIds()
     */
    @Override
    public Set<String> lookupSseHeaderChannelIds()
    {
        /** MPCS-5008  New */
        return Collections.unmodifiableSet(_sseheaderChannelIds);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelDataPreFetch#lookupCRC32(java.lang.String)
     */
    @Override
    public Long lookupCRC32(final String channelId)
    {
        return _crc32Map.get(channelId);
    }


    /**
     * Produce IN where clause from set of non-wildcarded items.
     * Returns null if no items.
     *
     * @param items  Set of non-wildcarded items
     * @param column Column in question
     *
     * @return Where-clause segment
     */
    private static String produceInWhereClause(final Set<String> items,
                                               final String      column)
    {
        if ((items == null) || items.isEmpty())
        {
            return null;
        }

        final StringBuilder sb = new StringBuilder();

        sb.append('(').append(column).append(" IN (");

        boolean first = true;

        for (final String item : items)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(',');
            }

            sb.append("'").append(item).append("'");
        }

        sb.append("))");

        return sb.toString();
    }


    /**
     * Produce LIKE where clauses from set of wildcarded items.
     * Returns null if no items.
     *
     * @param items  Set of wildcarded items
     * @param column Column in question
     *
     * @return Where-clause segments
     */
    private static List<String> produceLikeWhereClauses(
                                    final Set<String> items,
                                    final String      column)
    {
        if ((items == null) || items.isEmpty())
        {
            return null;
        }

        final List<String>  result = new ArrayList<String>(items.size());
        final StringBuilder sb     = new StringBuilder();

        for (final String item : items)
        {
            sb.setLength(0);

            sb.append('(').append(column).append(" LIKE '");
            sb.append(item).append("')");

            result.add(sb.toString());
        }

        return result;
    }


    /**
     * Stitch together the IN clause and the LIKE clauses into a single
     * group of ORs as necessary. The individual clauses are already
     * parenthesized.
     *
     * @param nonwild IN clause or null
     * @param wild    List of LIKE clauses or null
     */
    private void stitch(final StringBuilder sb,
                        final String        nonwild,
                        final List<String>  wild)
    {
        if ((wild != null) && wild.isEmpty())
        {
            throw new IllegalArgumentException(
                          "Wild must have at least one clause");
        }

        final boolean wildEmpty = ((wild == null) || wild.isEmpty());

        if (nonwild != null)
        {
            if (! wildEmpty)
            {
                sb.append('(');

                sb.append(nonwild);

                for (final String clause : wild)
                {
                    sb.append(OR).append(clause);
                }

                sb.append(')');
            }
            else
            {
                sb.append(nonwild);
            }
        }
        else if (! wildEmpty)
        {
            final boolean justOne = (wild.size() == 1);

            if (! justOne)
            {
                sb.append('(');
            }

            boolean first = true;

            for (final String clause : wild)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(OR);
                }

                sb.append(clause);
            }

            if (! justOne)
            {
                sb.append(')');
            }
        }
    }


    /**
     * Add channel id to lookup map.
     *
     * @param channelIds Map to add to
     * @param hostId     Host id
     * @param sessionId  Session id
     * @param channelId  Channel id to add
     */
    private static void addChannelId(
        final Map<Integer, Map<Integer, Set<String>>> channelIds,
        final int                                     hostId,
        final int                                     sessionId,
        final String                                  channelId)
    {
        Map<Integer, Set<String>> sessionMap = channelIds.get(hostId);

        if (sessionMap == null)
        {
            sessionMap = new TreeMap<Integer, Set<String>>();

            channelIds.put(hostId, sessionMap);
        }

        Set<String> soFar = sessionMap.get(sessionId);

        if (soFar == null)
        {
            soFar = new TreeSet<String>();

            sessionMap.put(sessionId, soFar);
        }

        soFar.add(channelId);
    }


    // MPCS-2473 Remove all generateStrict* methods

    // MPCS-2473  Remove all generateSessions method

    // MPCS-2473  Remove all generateChannelIds method
}
