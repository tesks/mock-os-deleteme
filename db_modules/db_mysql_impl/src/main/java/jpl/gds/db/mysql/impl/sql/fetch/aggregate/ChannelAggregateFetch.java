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
package jpl.gds.db.mysql.impl.sql.fetch.aggregate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.fetch.ChannelTypeSelect;
import jpl.gds.db.api.sql.fetch.IChannelDataPreFetch;
import jpl.gds.db.api.sql.fetch.IChannelValueFetch;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.PreFetchType;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.fetch.aggregate.IChannelAggregateFetch;
import jpl.gds.db.api.sql.order.IChannelValueOrderByType;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.IEcdrOrderByType;
import jpl.gds.db.api.sql.store.ldi.IChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IHeaderChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IMonitorChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.ISseChannelAggregateLDIStore;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.mysql.impl.sql.fetch.AbstractMySqlFetch;
import jpl.gds.db.mysql.impl.sql.order.AbstractOrderByType;
import jpl.gds.db.mysql.impl.sql.order.ChannelAggregateOrderByType;
import jpl.gds.shared.holders.ApidHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.DbTimeUtility;


/**
 * This is the database read/retrieval interface to the ChannelAggregate table in the
 * MPCS database.  This class will retrieve one or more channel aggregates from the database
 * based on a number of different query input parameters.
 *
 * The general way to use this class is:
 *
 * Derived from ChannelValueFetch.
 *
 */
public class ChannelAggregateFetch extends AbstractMySqlFetch implements IChannelAggregateFetch
{
    private static final Tracer LOG         = TraceManager.getDefaultTracer();
    private static final int    BUFFER_SIZE = 10000;
    
    private static final String GROUP_ORDER_BY =
            " ORDER BY NULL LIMIT 18446744073709551615";
    
    /** Need this up here so we can get spacecraft id from it as needs be */
    private IDbSessionPreFetch spf = null;
    
    /** If present, contains evaluated channel id wildcards */
    private IChannelDataPreFetch cdpf = null;
    
    /** True if we must run the channel data pre-fetch */
    private final boolean runCdpf;
    
    /** Passed in channel ids (and wildcards) */
    private final String[] channelIdParameters;
    
    /** Passed in modules (and wildcards) */
    private final String[] moduleParameters;
    
    /** True if we must use strict channel-id queries */
    private final boolean strict;
    
    /** If true, join with Packet (or SsePacket) */
    private final boolean includePacketInfo;
    
    /** Prefix for monitor channel values */
    private final String              MONITOR_PREFIX          = appContext.getBean(IMySqlAdaptationProperties.class)
                                                                          .getMonitorPrefix();
    /** Prefix for header channel values */
    private final String              HEADER_PREFIX           = appContext.getBean(IMySqlAdaptationProperties.class)
                                                                          .getHeaderPrefix();
    
    String tableChanAggAbbrev = "ca";
    String tableMonAggAbbrev = "ma";
    String tableHeaderAggAbbrev = "ha";
    String tableSseAggAbbrev = "sca";
    // -------
    
    private final List<String> fullChannelIdList;
    
    /**
     * Creates an instance of ChannelAggregateFetch.
     *
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            The flag that indicates whether the fetch class should print
     *            out the SQL statement only or execute it.
     * @param channelIds
     *            Channel ids to query on or null
     * @param modules
     *            Modules to query on or null
     * @param doPacket
     *            Include Packet data
     * @param ecdr
     *            ECDR mode (implies doPacket)
     */
    public ChannelAggregateFetch(final ApplicationContext appContext,
                                 final boolean            printSqlStmt,
                                 final String[]           channelIds,
                                 final String[]           modules,
                                 final boolean            doPacket,
                                 final boolean            ecdr)
    {
        this(appContext,
             printSqlStmt,
             channelIds,
             modules,
             doPacket,
             ecdr,
             false);
    }


    /**
     * Creates an instance of ChannelAggregateFetch.
     *
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            The flag that indicates whether the fetch class should print
     *            out the SQL statement only or execute it.
     * @param channelIds
     *            Channel ids to query on or null
     * @param modules
     *            Modules to query on or null
     * @param doPacket
     *            Include Packet data
     * @param ecdr
     *            ECDR mode (implies doPacket)
     * @param unique
     *            Unique packets only
     */
    public ChannelAggregateFetch(final ApplicationContext appContext,
                                 final boolean            printSqlStmt,
                                 final String[]           channelIds,
                                 final String[]           modules,
                                 final boolean            doPacket,
                                 final boolean            ecdr,
                                 final boolean            unique)
    {
        super(appContext, printSqlStmt, false); // Don't want prepared statement
        
        fullChannelIdList = new ArrayList<>();
        
        includePacketInfo = doPacket;
        
        channelIdParameters = channelIds;
        moduleParameters = IChannelValueFetch.clean(modules);

        final boolean haveChannels = (channelIdParameters.length > 0);
        final boolean haveModules  = (moduleParameters.length    > 0);

        // MPCS-2473 Strict is just for modules now
        strict               = haveModules;
        final boolean force  = dbProperties.getAlwaysRunChannelPrequery() &&
                               haveChannels;
        final boolean wild   = haveWildcards(channelIdParameters);

        // We want to run the prequery when:
        //     We have a channel-id wildcard
        //     We have some channel ids and force is in effect
        //     We have any module

        runCdpf = (wild || force || strict);
    }


    /**
     * Creates an instance of ChannelAggregateFetch.
     *
     * @param appContext
     *            the Spring Application Context
     * @param channelId
     *            Channel id to query on or null
     */
    public ChannelAggregateFetch(final ApplicationContext appContext,
                                 final String             channelId)
    {
        this(appContext,
             false,
             (channelId != null) ? new String[] {channelId}
                                 : new String[0],
             null,
             false,
             false);
    }


    /**
     * Creates an instance of ChannelAggregateFetch.
     *
     * @param appContext
     *            the Spring Application Context
     */
    public ChannelAggregateFetch(final ApplicationContext appContext)
    {
        this(appContext, false, new String[0], new String[0], false, false);
    }
    
    @Override
	public ResultSet getResultSet(final IDbSessionInfoProvider tsi, final DatabaseTimeRange range, final int batchSize,
			final Object... params) throws DatabaseException {
		if (tsi == null) {
			throw new IllegalArgumentException("Input test session information was null");
		}

		if (!dbProperties.getUseDatabase()) {
			return null;
		}

		final DatabaseTimeRange good_range = (range != null) ? range : new DatabaseTimeRange(DatabaseTimeType.ERT);

		initQuery(batchSize);
        String[] whereClauses = null;

        // Create pre-fetch query and execute

        if (printStmtOnly)
        {
            // Dummy to get SQL statement printed

            final IDbSessionPreFetch dummy = fetchFactory.getSessionPreFetch(true, PreFetchType.GET_SCID);
            try
            {
                dummy.get(tsi);
            }
            finally
            {
                dummy.close();
            }
        }

        // Must always run, even with printStmtOnly, to populate main query
        // and channel data pre-query.
        // Get spacecraft id as well
        
        spf = fetchFactory.getSessionPreFetch(false,  PreFetchType.GET_SCID);
        try
        {
            spf.get(tsi);
            
            if (printStmtOnly && runCdpf)
            {
                // Dummy to get SQL statement printed

                final IChannelDataPreFetch dummy =
                		fetchFactory.getChannelDataPreFetch(true,
                                            spf,
                                            channelIdParameters,
                                            moduleParameters);
                try
                {
                    dummy.get();
                }
                finally
                {
                    dummy.close();
                }
            }

            /** MPCS-5008  Add for SSE header */
            whereClauses = getSqlWhereClauses(
                               new String[]
                               {
                                   spf.getIdHostWhereClause(tableChanAggAbbrev),
                                   spf.getIdHostWhereClause(tableMonAggAbbrev),
                                   spf.getIdHostWhereClause(tableHeaderAggAbbrev),
                                   spf.getIdHostWhereClause(tableSseAggAbbrev),
                                   spf.getIdHostWhereClause(tableHeaderAggAbbrev)
                               },
                               good_range,
                               tsi,
                               params);
        }
        finally
        {
            spf.close();
        }
		return populateExecuteAndReturnResultSetNew(tsi, good_range, whereClauses, params);
	}


    private ResultSet populateExecuteAndReturnResultSetNew(
    		final IDbSessionInfoProvider tsi,
            final DatabaseTimeRange      range,
            final String[]               whereClause,
            final Object...              params) throws DatabaseException {
        
        if (tsi == null)
        {
            throw new IllegalArgumentException("The input test session information was null");
        }

        if (range == null)
        {
            throw new IllegalArgumentException("The input time range information was null");
        }

        final ChannelTypeSelect cts = (ChannelTypeSelect) params[2];

        String sql = null; // SQL used in query

        final String            module = (String)            params[1];

        final List<AbstractOrderByType> orders =
            SystemUtilities.<List<AbstractOrderByType>>castNoWarning(
                params[3]);

        final QueryClauseType queryClause;
      	
        if (includePacketInfo && _extendedDatabase) {
        	queryClause = QueryClauseType.PACKET_JOIN2;
        } else if(includePacketInfo && !_extendedDatabase) {
        	queryClause = QueryClauseType.PACKET_JOIN;
        } else {
        	queryClause = QueryClauseType.NO_JOIN;
        }
        
        /** MPCS-5008 */
        final QueryClauseType queryClauseSseHeader =
            (includePacketInfo ? QueryClauseType.PACKET_JOIN_SSE
                               : QueryClauseType.NO_JOIN_SSE);

        try
        {
            // The combination of FORWARD_ONLY, CONCUR_READ_ONLY, and a fetch size
            // of Integer.MIN_VALUE signals to the MySQL JDBC driver to stream the
            // results row by row rather than trying to load the whole result set
            // into memory. Any other settings will result in a heap overflow. Note
            // that this will probably break if we change database vendors.

            /** MPCS-8384 Add extended support */

            final String[] selectClauses = new String[5];

            selectClauses[0] = queryConfig.getQueryClause(
                                   queryClause,
                                   getActualTableName(IChannelAggregateLDIStore.DB_CHANNEL_AGGREGATE_TABLE_NAME));

            selectClauses[1] = queryConfig.getQueryClause(
                                   queryClause,
                                   IMonitorChannelAggregateLDIStore.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME);

            selectClauses[2] = queryConfig.getQueryClause(
                                   queryClause,
                                   getActualTableName(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME));

            selectClauses[3] = queryConfig.getQueryClause(
                                   queryClause,
                                   getActualTableName(ISseChannelAggregateLDIStore.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME));

            selectClauses[4] = queryConfig.getQueryClause(
                                   queryClauseSseHeader,
                                   getActualTableName(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME));

            final String[] groupByClauses = new String[5];

            Arrays.fill(groupByClauses, null);
            
            if (includePacketInfo) {            	
            	groupByClauses[0] = queryConfig.getQueryClause(
                        QueryClauseType.PACKET_JOIN_GROUP_BY,
                        getActualTableName(IChannelValueLDIStore.DB_CHANNEL_AGGREGATE_TABLE_NAME));
 
            }
            
            final StringBuilder sqlClause = new StringBuilder();

            boolean headerOrderBySuppress  = false;
            boolean monitorOrderBySuppress = false;

            if (orders != null)
            {
                // If there is an order-by of anything unsupportable we
                // suppress HeaderChannelValue or MonitorChannelValue

                for (final AbstractOrderByType o : orders)
                {
                    final int oi = o.getValueAsInt();

                    if (o instanceof IChannelValueOrderByType)
                    {
                        if (oi == IChannelValueOrderByType.MODULE_TYPE)
                        {
                            headerOrderBySuppress  = true;
                            monitorOrderBySuppress = true;
                        }
                    }
                    else if (o instanceof IEcdrOrderByType)
                    {
                        switch (oi)
                        {
                            case IEcdrOrderByType.SCLK_TYPE:
                            case IEcdrOrderByType.SCET_TYPE:
                                monitorOrderBySuppress = true;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }

            if ((module != null)
                    || ! cts.monitor
                    || monitorOrderBySuppress
                    || (range.isRangeSpecified() &&
                        (range.getTimeType().getValueAsInt() !=
                         DatabaseTimeType.ERT_TYPE)))
            {
                whereClause[1] = "";

                cts.monitor = false;
            }

            if ((module != null) ||
                ! cts.header     ||
                headerOrderBySuppress)
            {
                whereClause[2] = "";

                cts.header = false;
            }

            if (! cts.sse)
            {
                whereClause[3] = "";
            }

            if (! cts.fswRealtime && ! cts.fswRecorded)
            {
                whereClause[0] = "";
            }

            if ((module != null) ||
                ! cts.sseHeader  ||
                headerOrderBySuppress)
            {
                whereClause[4] = "";

                cts.sseHeader = false;
            }

            int unionCount = 0;

            for (int k = 0; k < 5; ++k)
            {
                if (! StringUtil.isNullOrEmpty(
                          StringUtil.safeTrim(whereClause[k])))
                {
                    ++unionCount;
                }
            }

            boolean first = true;

            final boolean finalOrderBy = ! StringUtil.isNullOrEmpty(
                                               StringUtil.safeTrim(whereClause[5]));

            for (int k = 0; k < 5; ++k)
            {
                if (StringUtil.isNullOrEmpty(StringUtil.safeTrim(whereClause[k])))
                {
                    continue;
                }

                if (first)
                {
                    first = false;
                }
                else
                {
                    sqlClause.append(" UNION ALL ");
                }

                if (unionCount > 1)
                {
                    sqlClause.append('(');
                }

                sqlClause.append(selectClauses[k]);
                sqlClause.append(whereClause[k]);

                final String groupBy = groupByClauses[k];
                
                if (groupBy != null)
                {
                    sqlClause.append(' ').append(groupBy);

                    if ((unionCount > 1) ||
                        ((unionCount == 1) && ! finalOrderBy))
                    {
                        sqlClause.append(GROUP_ORDER_BY);
                    }
                }

                if (unionCount > 1)
                {
                    sqlClause.append(')');
                }
            }

            if (finalOrderBy)
            {
                sqlClause.append(whereClause[5]);
            }

            if (! cts.isActive())
            {
                // We have no channel types, so just use a dummy statement
                // that will return no rows.

                sqlClause.setLength(0);
                // MPCS-11098 : chill_get_chanvals outputs unexpected
                // SQL output when specifying non-existing channel id
                sqlClause.append("SELECT 1 FROM " + IChannelAggregateLDIStore.DB_CHANNEL_AGGREGATE_TABLE_NAME + " WHERE (0=1)");
            }

            // We do NOT use a prepared statement; not needed and too slow
            
            sql = insertIndexHints(sqlClause.toString(),
                                   (params.length > 10)
                                       ? StringUtil.safeTrim((String) params[10])
                                       : "");
			
            
            plainStatement = getStatement();

            // Will use FORWARD_ONLY and CONCUR_READ_ONLY
            // which is required to get streaming.

            plainStatement.setFetchSize(Integer.MIN_VALUE);

            if (this.printStmtOnly)
            {
                printSqlStatement(sql, "Main");
            }
            else
            {
                results = plainStatement.executeQuery(sql);
            }
        }
        catch (final SQLException sqle)
        {
            throw new DatabaseException("Error retrieving channel values from " +
                                       "database: "                        +
                                       sqle.getMessage()                   +
                                       "(SQL Error="                       +
                                       sql                                 +
                                       ")",
                                   sqle);
        }

        return results;
    }
    
    
    /**
     * Get the string representation of the "WHERE" portion of the SQL insert query. Does
     * both ChannelValue and MonitorChannelValue and HeaderChannelValue and SseChannelValue.
     *
     * @param testSqlTemplate The templated SQL strings with WHERE clause conditions
     * @param range           The database time range specifying the type of time and time
     *                        range for querying. If null, the default time type and a time
     *                        range of 0 to infinity will be used.
     * @param tsi             Database session information
     * @param params          Array of parameters to fill in where-clause
     *
     * @return The complete templated SQL WHERE clauses for the query
     *
     * @throws DatabaseException SQL exception
     */
    protected String[] getSqlWhereClauses(final String[]          testSqlTemplate,
                                          final DatabaseTimeRange range,
                                          final IDbSessionInfoProvider     tsi,
                                          final Object...         params)
        throws DatabaseException
    {
        /** MPCS-5008  Throughout */

        String[] channelIds = (String[])params[0];
        if (channelIds == null) {
        	channelIds = new String[0];
        }

        final ChannelTypeSelect cts = (ChannelTypeSelect) params[2];

        List<AbstractOrderByType> orders =
            SystemUtilities.<List<AbstractOrderByType>>castNoWarning(
                params[3]);

        Boolean isDescending = Boolean.FALSE;

        if ((params.length > 6) && (params[6] != null))
        {
            isDescending = (Boolean) params[6];
        }

        final Set<Integer> vcids =
            SystemUtilities.<Set<Integer>>castNoWarning(
                (params.length >= 8) ? params[7] : null);

        final Set<Integer> dssIds =
            SystemUtilities.<Set<Integer>>castNoWarning(
                (params.length >= 9) ? params[8] : null);

        final Set<ApidHolder> apids =
            SystemUtilities.<Set<ApidHolder>>castNoWarning(
                (params.length >= 10) ? params[9] : null);

        if(orders == null)
        {
            orders = new ArrayList<AbstractOrderByType>();
        }

        if(testSqlTemplate == null)
        {
            throw new IllegalArgumentException("Input test information template was null");
        }

        // Clean passed-in channel ids and remove ranges but not wildcards
        channelIds = channelIdParameters;

        // Channels are required if at least one channel id or module
        // was specified
        final boolean channelIdMode = (channelIdParameters != null && channelIdParameters.length > 0);
        
        
        if (runCdpf)
        {
            cdpf = fetchFactory.getChannelDataPreFetch(false,
                                           spf,
                                           channelIdParameters,
                                           moduleParameters);
            try
            {
                cdpf.get();

                // MPCS-2473  Remove if with strict
            }
            finally
            {
                cdpf.close();
            }
        }

        final String[] sqlWhereClauses = new String[6];

        String channelWhere   = null;
        String monitorWhere   = null;
        String headerWhere    = null;
        String sseWhere       = null;
        String sseheaderWhere = null;

        if (! testSqlTemplate[0].isEmpty())
        {
            channelWhere = addToWhere(channelWhere,testSqlTemplate[0]);
        }

        if (testSqlTemplate.length > 1 && ! testSqlTemplate[1].isEmpty())
        {
            monitorWhere = addToWhere(monitorWhere, testSqlTemplate[1]);
        }

        if (testSqlTemplate.length > 2 && ! testSqlTemplate[2].isEmpty())
        {
            headerWhere = addToWhere(headerWhere, testSqlTemplate[2]);
        }

        if (testSqlTemplate.length > 3 && ! testSqlTemplate[3].isEmpty())
        {
            sseWhere = addToWhere(sseWhere, testSqlTemplate[3]);
        }

        if (testSqlTemplate.length > 4 && ! testSqlTemplate[4].isEmpty())
        {
            sseheaderWhere = addToWhere(sseheaderWhere, testSqlTemplate[4]);
        }

        final List<String> temp = new ArrayList<String>(channelIds.length);

        for (final String s : channelIds)
        {
            // MPCS-4920  Don't check for SSE any more

            if (! s.startsWith(HEADER_PREFIX) &&
                ! s.startsWith(MONITOR_PREFIX))
            {
                temp.add(s);
            }
        }

        String[] channelList = (cdpf != null)
                ? IChannelValueFetch.asArray(cdpf.lookupFswChannelIds()) : IChannelValueFetch.asArray(temp);
        
		if (channelList.length > 0) {
			fullChannelIdList.addAll(Arrays.asList(channelList));
		}
                
        if (channelIdMode             &&
            (channelList.length == 0) &&
            (cts.fswRealtime || cts.fswRecorded))
        {
            cts.fswRealtime = false;
            cts.fswRecorded = false;
        }

        // FSW channels

        // Monitor

        temp.clear();

        for (final String s : channelIds)
        {
            if (s.startsWith(MONITOR_PREFIX))
            {
                temp.add(s);
            }
        }

        channelList = (cdpf != null) ? IChannelValueFetch.asArray(cdpf.lookupMonitorChannelIds())
                : IChannelValueFetch.asArray(temp);
        
        if (channelList.length > 0) {
        	fullChannelIdList.addAll(Arrays.asList(channelList));
        }
        
        if (channelIdMode             &&
            (channelList.length == 0) &&
            cts.monitor)
        {
            cts.monitor = false;
        }

        // END OF Monitor

        // Header

        temp.clear();

        for (final String s : channelIds)
        {
            if (s.startsWith(HEADER_PREFIX))
            {
                temp.add(s);
            }
        }

        channelList = (cdpf != null) ? IChannelValueFetch.asArray(cdpf.lookupHeaderChannelIds())
                : IChannelValueFetch.asArray(temp);
        
        if (channelList.length > 0) {
        	fullChannelIdList.addAll(Arrays.asList(channelList));
        }
        
        /** MPCS-5008  */
        if (channelIdMode && (channelList.length == 0))
        {
            cts.header = false;
        }

        // END OF Header

        // SSE

        temp.clear();

        for (final String s : channelIds)
        {
            // MPCS-49 Don't check for SSE any more

            if (! s.startsWith(HEADER_PREFIX) &&
                ! s.startsWith(MONITOR_PREFIX))
            {
                temp.add(s);
            }
        }

        channelList = (cdpf != null) ? IChannelValueFetch.asArray(cdpf.lookupSseChannelIds())
                : IChannelValueFetch.asArray(temp);
        
        if (channelList.length > 0) {
        	fullChannelIdList.addAll(Arrays.asList(channelList));
        }
        
        if (channelIdMode             &&
            (channelList.length == 0) &&
            cts.sse)
        {
            cts.sse = false;
        }

        // END OF SSE

        // SSE Header

        temp.clear();

        for (final String s : channelIds)
        {
            if (s.startsWith(HEADER_PREFIX))
            {
                temp.add(s);
            }
        }

        channelList = (cdpf != null) ? IChannelValueFetch.asArray(cdpf.lookupSseHeaderChannelIds())
                : IChannelValueFetch.asArray(temp);
        
        if (channelList.length > 0) {
        	fullChannelIdList.addAll(Arrays.asList(channelList));
        }
        
        if (channelIdMode && (channelList.length == 0))
        {
            cts.sseHeader = false;
        }
        

        // Add the proper time clause and order by the time type
        // if no ordering has been specified
        /** MPCS-8384 Extended support */
        /** MPCS-8639  Move config item */

        final String fswTimeWhere =
            DbTimeUtility.generateAggregateTimeWhereClause(
                tableChanAggAbbrev, range, false);
        final String sseTimeWhere =
            DbTimeUtility.generateAggregateTimeWhereClause(
                tableSseAggAbbrev, range, false);


        if (fswTimeWhere.length() > 0)
        {
            // FSW takes all time types

            channelWhere = addToWhere(channelWhere, fswTimeWhere);
        }

        if (sseTimeWhere.length() > 0)
        {
            // SSE takes all time types

            sseWhere = addToWhere(sseWhere, sseTimeWhere);
        }

        if (cts.fswRealtime != cts.fswRecorded)
        {
            // Not both

            final StringBuilder realtimeClause =
                new StringBuilder();

            realtimeClause.append('(');
            realtimeClause.append(tableChanAggAbbrev);
            realtimeClause.append(".channelType ");

            if (cts.fswRealtime)
            {
                // FSW realtime
                realtimeClause.append("= 'FSW_RT')");
            }
            else
            {
                // FSW recorded
                realtimeClause.append("= 'FSW_REC')");
            }

            channelWhere = addToWhere(channelWhere, realtimeClause.toString());
        }

        if ((vcids != null) && ! vcids.isEmpty())
        {
            channelWhere = addToWhere(channelWhere,
                                      generateVcidWhere(vcids,
                                                        tableChanAggAbbrev,
                                                        true));
            headerWhere = addToWhere(headerWhere,
                                     generateVcidWhere(vcids,
                                                       tableHeaderAggAbbrev,
                                                       true));
        }

        if ((dssIds != null) && ! dssIds.isEmpty())
        {
            channelWhere = addToWhere(channelWhere,
                                      generateDssIdWhere(dssIds,
                                                         tableChanAggAbbrev));
            monitorWhere = addToWhere(monitorWhere,
                                      generateDssIdWhere(dssIds,
                                                         tableMonAggAbbrev));
            headerWhere = addToWhere(headerWhere,
                                     generateDssIdWhere(dssIds,
                                                        tableHeaderAggAbbrev));
        }

        /*
        if ((apids != null) && ! apids.isEmpty())
        {
            // Used with --includePacketInfo, but not for monitor
            channelWhere = addToWhere(channelWhere,
                                      generateApidWhere(apids,
                                                        packetAbbrev));
            headerWhere = addToWhere(headerWhere,
                                     generateApidWhere(apids,
                                                       packetHeaderAbbrev));
            sseWhere = addToWhere(sseWhere,
                                  generateApidWhere(apids,
                                                    packetSseAbbrev));
            sseheaderWhere = addToWhere(sseheaderWhere,
                                        generateApidWhere(apids,
                                                          packetHeaderAbbrev));
        }*/

        sqlWhereClauses[0] = channelWhere;
        sqlWhereClauses[1] = monitorWhere;
        sqlWhereClauses[2] = headerWhere;
        sqlWhereClauses[3] = sseWhere;
        sqlWhereClauses[4] = sseheaderWhere;

        if (cts.isActive())
        {
            sqlWhereClauses[5] =
                " " + computeOrderByClause(orders, isDescending);
        }
        else
        {
            sqlWhereClauses[5] = "";
        }

        return sqlWhereClauses;
    }
    
    
    /**
     * Make order-by clause for all order-bys.
     *
     * @param orders     List of order-bys
     * @param descending True if order-bys are descending instead of ascending
     *
     * @return String with complete order-by clause for all order-bys
     */
    private String computeOrderByClause(
                       final List<AbstractOrderByType> orders,
                       final boolean                         descending)
    {
        if ((orders == null) || orders.isEmpty())
        {
            return "";
        }

        final StringBuilder sb    = new StringBuilder();
        boolean             first = true;

        for (final IDbOrderByType ob : orders)
        {
            String clause = null;
            
            clause = ((ChannelAggregateOrderByType) ob).getOrderByClause();

            if (clause.isEmpty())
            {
                // Order-by None
                continue;
            }

            if (first)
            {
                sb.append(' ').append(IDbOrderByType.ORDER_BY_PREFIX);
                sb.append(' ');

                first = false;
            }
            else
            {
                sb.append(',');
            }

            if (clause.startsWith(IDbOrderByType.ORDER_BY_PREFIX))
            {
                clause = clause.substring(
                             IDbOrderByType.ORDER_BY_PREFIX.length());
            }

            sb.append(clause);

        }

        return sb.toString();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends IDbRecord> getNextResultBatch() throws DatabaseException
    {
        return null;
    }


    /**
     * Look for any wildcards among the channel ids.
     *
     * @param channelIds Channel ids
     *
     * @return True if there is at least one wildcard
     */
    private static boolean haveWildcards(final String[] channelIds)
    {
        for (final String cid : channelIds)
        {
            if (cid.contains(ILDIStore.WILDCARD1) ||
                cid.contains(ILDIStore.WILDCARD2))
            {
                return true;
            }
        }

        return false;
    }
    
    
	@Override
	public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, final DatabaseTimeRange range, final int batchSize,
			final Object... params) throws DatabaseException {
		return null;
	}


	@Override
	public String getSqlWhereClause(final String testSqlTemplate, final DatabaseTimeRange range, final Object... params)
			throws DatabaseException {
		return null;
	}


	@Override
	protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
			final String whereClause, final Object... params) throws DatabaseException {
		return null;
	}


	@Override
	public IDbSessionPreFetch getSessionPreFetch() {
		return spf;
	}


	@Override
	public List<String> getFullChannelIdList() {
		return fullChannelIdList;
	}


    @Override
    protected List<? extends IDbRecord> getResults() throws DatabaseException {
        return null;
    }
}

