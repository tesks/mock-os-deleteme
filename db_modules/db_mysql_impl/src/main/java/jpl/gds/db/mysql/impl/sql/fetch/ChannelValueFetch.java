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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.VenueType;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.fetch.ChannelTypeSelect;
import jpl.gds.db.api.sql.fetch.IAlarmControl;
import jpl.gds.db.api.sql.fetch.IChannelDataPreFetch;
import jpl.gds.db.api.sql.fetch.IChannelValueFetch;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.PreFetchType;
import jpl.gds.db.api.sql.fetch.QueryClauseType;
import jpl.gds.db.api.sql.order.IChannelValueOrderByType;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.IEcdrMonitorOrderByType;
import jpl.gds.db.api.sql.order.IEcdrOrderByType;
import jpl.gds.db.api.sql.store.ldi.IChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.IHeaderChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILDIStore;
import jpl.gds.db.api.sql.store.ldi.IMonitorChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseChannelValueLDIStore;
import jpl.gds.db.api.types.IDbChannelSampleProvider;
import jpl.gds.db.api.types.IDbChannelSampleUpdater;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.ParentTypeEnum;
import jpl.gds.db.mysql.impl.sql.order.AbstractOrderByType;
import jpl.gds.db.mysql.impl.sql.order.ChannelValueOrderByType;
import jpl.gds.db.mysql.impl.sql.order.EcdrMonitorOrderByType;
import jpl.gds.db.mysql.impl.sql.order.EcdrOrderByType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.ChannelChangeFilter;
import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.shared.annotation.ToDo;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.holders.ApidHolder;
import jpl.gds.shared.holders.ApidNameHolder;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.holders.SpscHolder;
import jpl.gds.shared.holders.VcfcHolder;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.types.UnsignedLong;


/**
 * This is the database read/retrieval interface to the ChannelValue table in the
 * MPCS database.  This class will retrieve one or more channel values from the database
 * based on a number of different query input parameters.
 *
 * The general way to use this class is:
 * <ol>
 * <li>Create a TestSessionInfo object and set all the information on it that should be used
 * to search the database for test sessions</li>
 * <li>Create any other necessary query values such as channel IDs</li>
 * <li>Call one of the "getChannelValues(...)" methods and specify a batch size for the size
 * of lists that should be returned.  The "getChannelValues(...)" methods will return only
 * the first batch of results.</li>
 * <li>Make further calls to getNextResultBatch() to retrieve the rest of the results from
 * the query</li>
 *
 * ECDR mode uses a different order-by class and supplements the SQL with
 * a GROUP BY. It implies joining with Packet.
 *
 * MPCS-2473
 * Strict mode now applies when and only when module patterns are present. There is no
 * reason to force it. Thd old verbose strict mode is replaced with a streamlined one.
 * All we do now is to add a double-check of module to the main WHERE clause. This makes
 * the WHERE clause only a little bit bigger instead of massively bigger. Note that we
 * handle multiple module patterns although we will only be passed a single pattern at
 * present.
 *
 * The strict mode is implemented for all four channel types, even though header and monitor
 * do not have modules. That is done because module is actually in ChannelData, so logically
 * all can have it. That's harmless because the parameters are checked way before we get here
 * and channel types thrown out if module is provided. But at this level we do not care and
 * do not make special cases.
 *
 * The purpose of strict mode is to provide for the case where a given channel-id is in a
 * module in one session and not in another.
 *
 * MPCS-5008
 * NB: An important optimization is sometimes used when GROUP BY is specified. MySQL
 * GROUP BY automatically performs an ORDER BY if the query does not provide one explicitly.
 * That ORDER BY is never what we want.
 *
 * If there is no UNION (i.e., we query from a single table), and we have an ORDER BY,
 * nothing need be done. But if we are ordering by NONE, we need to add an ORDER BY NULL
 * in order to eliminate the pointless sort.
 *
 * If we have a UNION, then our ORDER BY is on the aggregate, not on the subqueries.
 * That means that if GROUP BY is specified, each subquery will likely perform a pointless
 * filesort, and then the aggregated rows will be finally sorted the way we want.
 *
 * So, if we have a UNION, to cancel the automatic ORDER BY we add ORDER BY NULL after
 * the GROUP BY. But for technical reasons MySQL will ignore that ORDER BY unless
 * accompanied by LIMIT. Since we do not want to limit the rows returned, we supply a
 * huge number to the LIMIT. The combination gets rid of the unwanted sorts.
 *
 */
@ToDo("Refactor this horrible mess")
public class ChannelValueFetch extends AbstractMySqlFetch implements IChannelValueFetch
{

    /** Table name for ChannelValue table */
    private final String DB_TABLE = IChannelValueLDIStore.DB_CHANNEL_VALUE_TABLE_NAME;
    /** Table name for MonitorChannelValue table */
    private final String DB_MONITOR_TABLE = IMonitorChannelValueLDIStore.DB_MONITOR_CHANNEL_VALUE_TABLE_NAME;
    /** Table name for HeaderChannelValue table */
    private final String DB_HEADER_TABLE = IHeaderChannelValueLDIStore.DB_HEADER_CHANNEL_VALUE_TABLE_NAME;
    /** Table name for SseChannelValue table */
    private final String DB_SSE_TABLE = ISseChannelValueLDIStore.DB_SSE_CHANNEL_VALUE_DATA_TABLE_NAME;
    /** Table name for ChannelData table */
    private final String DB_DATA_TABLE = IChannelValueLDIStore.DB_CHANNEL_DATA_TABLE_NAME;
    
    /* BEGIN:  MPCS-5254: make channel ID special prefixes
     * configurable */
    /** Prefix for monitor channel values */
    private final String              MONITOR_PREFIX          = appContext.getBean(IMySqlAdaptationProperties.class)
                                                                          .getMonitorPrefix();
    
    /** Prefix for header channel values */
    private final String              HEADER_PREFIX           = appContext.getBean(IMySqlAdaptationProperties.class)
                                                                          .getHeaderPrefix();
    

    /** MPCS-5610  Now done in SQL */
// MPCS-8322 : removed 'FAKE_SCET'
//    private static final boolean FAKE_SCET = false;

    /*
     * MPCS-4920. We can no longer rely upon SSE channels beginning with "SSE",
     * so that check is removed below. The result is that all SSE channels will
     * go to the FSW list and all FSW channels will go to the SSE list. They
     * will then be ignored if they do not apply; that will make the WHERE
     * clauses larger. But don't fret, because those lists are only used when
     * the pre-query is not run, and the pre-query is run by default.
     */
    /** Table abbreviation for channel value */
    public final String tableAbbrev = queryConfig.getTablePrefix(DB_TABLE);
    /** Table abbreviation for monitor channel value */
    public final String tableMonitorAbbrev = queryConfig.getTablePrefix(DB_MONITOR_TABLE);
    /** Table abbreviation for header channel value */
    public final String tableHeaderAbbrev = queryConfig.getTablePrefix(DB_HEADER_TABLE);
    /** Table abbreviation for header channel value with Packet */
    public final String tableHeaderPacketAbbrev = queryConfig.getPacketPrefix(DB_HEADER_TABLE);
    /** Table abbreviation for SSE channel value */
    public final String tableSseAbbrev = queryConfig.getTablePrefix(DB_SSE_TABLE);
    /** Table packet abbreviation for channel value */
    public final String packetAbbrev = queryConfig.getPacketPrefix(DB_TABLE);
    /** Table packet abbreviation for header channel value */
    public final String packetHeaderAbbrev = queryConfig.getPacketPrefix(DB_HEADER_TABLE);
    /** Table abbreviation for SSE channel value */
    public final String packetSseAbbrev = queryConfig.getPacketPrefix(DB_SSE_TABLE);
    /** Table abbreviation for channel data */
    public final String tableDataAbbrev = queryConfig.getTablePrefix(DB_DATA_TABLE);

    /** Special code to optimize GROUP BY. See comments at top. MPCS-5008 */
    private static final String GROUP_ORDER_BY =
        " ORDER BY NULL LIMIT 18446744073709551615";

    private static final String RCT_COARSE_COL = "rctCoarse";
    private static final String RCT_FINE_COL   = "rctFine";

    private static final String SCLK_COARSE_COL = "sclkCoarse";
    private static final String SCLK_FINE_COL   = "sclkFine";

    private static final String ERT_COARSE_COL = "ertCoarse_mstCoarse";
    private static final String ERT_FINE_COL   = "ertFine_mstFine";

    private static final String SCET_COARSE_COL = "scetCoarse";
    private static final String SCET_FINE_COL   = "scetFine";

    private static final String CHANNEL_ID_COL = "channelId";
    private static final String CHANNEL_INDEX_COL = "channelIndex";
    private static final String CHANNEL_NAME_COL = "name";
    private static final String CHANNEL_TYPE_COL = "type";

    private static final String MODULE_COL      = "module";
    private static final String FROM_SSE_COL    = "fromSse";
    private static final String REALTIME_COL    = "isRealtime";
    private static final String EU_COL          = "eu";
    private static final String EU_FLAG_COL     = "euFlag";
    private static final String SIGNED_COL      = "dnIntegerValue";
    private static final String UNSIGNED_COL    = "dnUnsignedValue";
    private static final String DOUBLE_COL      = "dnDoubleValue";
    private static final String DOUBLE_FLAG_COL = "dnDoubleFlag";
    private static final String STRING_COL      = "dnStringValue";
    private static final String DN_ALARM_COL    = "dnAlarmState";
    private static final String EU_ALARM_COL    = "euAlarmState";
    private static final String DSS_ID_COL      = "dssId";
    private static final String VCID_COL        = "vcid";
    private static final String PACKET_ID_COL   = "packetId";
    private static final String FRAME_ID_COL    = "frameId";
    private static final String PARENT_TYPE_COL = "parentType";
    private static final String DN_FORMAT_COL   = "dnFormat";
    private static final String EU_FORMAT_COL   = "euFormat";

    /** Columns from Packet or SsePacket */
    private static final String APID_COL     = "apid";
    private static final String APIDNAME_COL = "apidName";
    private static final String SPSC_COL     = "spsc";
    private static final String PRCTC_COL    = "packetRctCoarse";
    private static final String PRCTF_COL    = "packetRctFine";
    private static final String VCFC_COL     = "sourceVcfc";

    // MPCS-2473
    private static final char SQ = '\'';

    private boolean allChangeValues = true;
    private final ChannelChangeFilter changeFilter;

    /** MPCS-7106 Get rid of typeLookup */

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

    /** If true, in ECDR mode */
    private final boolean ecdrMode;

    /** If true, in deduplication mode */
    private final boolean uniqueMode;

    //private static final Tracer log = Log4jTracer.getDefaultTracer();



    /**
     * Creates an instance of IChannelValueFetch.
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
    public ChannelValueFetch(final ApplicationContext appContext,
    		                 final boolean  printSqlStmt,
                             final String[] channelIds,
                             final String[] modules,
                             final boolean  doPacket,
                             final boolean  ecdr)
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
     * Creates an instance of IChannelValueFetch.
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
    public ChannelValueFetch(final ApplicationContext appContext,
    		                 final boolean  printSqlStmt,
                             final String[] channelIds,
                             final String[] modules,
                             final boolean  doPacket,
                             final boolean  ecdr,
                             final boolean  unique)
    {
        super(appContext, printSqlStmt, false); // Don't want prepared statement

        includePacketInfo = doPacket;
        ecdrMode          = ecdr;
        uniqueMode        = unique;

        // Expand ranges, check for nulls, etc., already performed.
        // Still have wildcards,
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

        changeFilter = new ChannelChangeFilter(appContext.getBean(EhaProperties.class));
    }


    /**
     * Creates an instance of IChannelValueFetch.
     *
     * @param appContext
     *            the Spring Application Context
     * @param channelId
     *            Channel id to query on or null
     */
    public ChannelValueFetch(final ApplicationContext appContext, final String channelId)
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
     * Creates an instance of IChannelValueFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public ChannelValueFetch(final ApplicationContext appContext)
    {
        this(appContext, false, new String[0], new String[0], false, false);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelValueFetch#get(jpl.gds.db.api.types.IDbSessionInfoProvider, jpl.gds.shared.time.DatabaseTimeRange, int, java.lang.Object)
     */
    @Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi,
                                           final DatabaseTimeRange         range,
                                           final int                 batchSize,
                                           final Object...           params)
        throws DatabaseException
    {
        if(tsi == null)
        {
            throw new IllegalArgumentException("Input test session information was null");
        }

        if (! dbProperties.getUseDatabase())
        {
            return new ArrayList<IDbChannelSampleProvider>();
        }

        final DatabaseTimeRange good_range =
            (range != null) ? range
                            : new DatabaseTimeRange(DatabaseTimeType.ERT);

        initQuery(batchSize);

        String[] whereClauses = null;

        // Create pre-fetch query and execute

        if (printStmtOnly)
        {
            // Dummy to get SQL statement printed

            final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true, PreFetchType.GET_SCID);
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
                                   spf.getIdHostWhereClause(tableAbbrev),
                                   spf.getIdHostWhereClause(tableMonitorAbbrev),
                                   spf.getIdHostWhereClause(tableHeaderAbbrev),
                                   spf.getIdHostWhereClause(tableSseAbbrev),
                                   spf.getIdHostWhereClause(tableHeaderAbbrev)
                               },
                               good_range,
                               params);
        }
        finally
        {
            spf.close();
        }
        return populateAndExecute(tsi, good_range, whereClauses, params);
    }


    /**
     * Get the string representation of the "WHERE" portion of the SQL insert query. Does
     * both ChannelValue and MonitorChannelValue and HeaderChannelValue and SseChannelValue.
     *
     * @param testSqlTemplate The templated SQL strings with WHERE clause conditions
     * @param range           The database time range specifying the type of time and time
     *                        range for querying. If null, the default time type and a time
     *                        range of 0 to infinity will be used.
     * @param params          Array of parameters to fill in where-clause
     *
     * @return The complete templated SQL WHERE clauses for the query
     *
     * @throws DatabaseException SQL exception
     */
    protected String[] getSqlWhereClauses(final String[]          testSqlTemplate,
                                          final DatabaseTimeRange range,
                                          final Object...         params)
        throws DatabaseException
    {
        /** MPCS-5008  Throughout */

        String[] channelIds = (String[])params[0];
        // final String module = (String)params[1];

        final ChannelTypeSelect cts = (ChannelTypeSelect) params[2];

        List<AbstractOrderByType> orders =
            SystemUtilities.<List<AbstractOrderByType>>castNoWarning(
                params[3]);

        this.allChangeValues = params[4] != null ? (Boolean)params[4] : Boolean.TRUE.booleanValue();
        final IAlarmControl alarms = (IAlarmControl) params[5];

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
        final boolean channelIdMode = ((channelIdParameters.length > 0) ||
                                       (moduleParameters.length    > 0));

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

        if (channelIdMode             &&
            (channelList.length == 0) &&
            (cts.fswRealtime || cts.fswRecorded))
        {
            cts.fswRealtime = false;
            cts.fswRecorded = false;
        }

        // FSW channels
        // BEGIN MPCS-2473  Add module checks

        if (channelList.length > 0)
        {
            if (strict)
            {
                // Add module checks

                channelWhere = addToWhere(channelWhere,
                                          moduleWhere(moduleParameters));
            }

            // END MPCS-2473

            // Add the list of channel IDs to search for (if there are any)
            // Make sure that list is parenthesized in case of multiple
            // elements separated by OR,
            // otherwise the WHERE clause may be interpreted incorrectly.

             channelWhere = addToWhere(channelWhere,
                                       channelWhere(channelList,
                                                    tableAbbrev));
         }

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

        if (channelIdMode             &&
            (channelList.length == 0) &&
            cts.monitor)
        {
            cts.monitor = false;
        }

        // BEGIN MPCS-2473  Add module checks

        if (channelList.length > 0)
        {
            // MPCS-2473 Add module checks

            if (strict)
            {
                // Add module checks

                monitorWhere = addToWhere(monitorWhere,
                                          moduleWhere(moduleParameters));
            }

            // Add the list of channel IDs to search for (if there are any)
            // Make sure that list is parenthesized in case of multiple
            // elements separated by OR,
            // otherwise the WHERE clause may be interpreted incorrectly.

            monitorWhere = addToWhere(monitorWhere,
                                      channelWhere(channelList,
                                                   tableMonitorAbbrev));
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

        /** MPCS-5008  */
        if (channelIdMode && (channelList.length == 0))
        {
            cts.header = false;
        }

        // BEGIN MPCS-2473  Add module checks

        if (channelList.length > 0)
        {
            // MPCS-2473 Add module checks

            if (strict)
            {
                // Add module checks

                headerWhere = addToWhere(headerWhere,
                                         moduleWhere(moduleParameters));
            }

            // Add the list of channel IDs to search for (if there are any)
            // Make sure that list is parenthesized in case of multiple
            // elements separated by OR,
            // otherwise the WHERE clause may be interpreted incorrectly.

            headerWhere = addToWhere(headerWhere,
                                     channelWhere(channelList,
                                                  tableHeaderAbbrev));
         }

        // END OF Header

        // SSE

        temp.clear();

        for (final String s : channelIds)
        {
            // MPCS-4920 Don't check for SSE any more

            if (! s.startsWith(HEADER_PREFIX) &&
                ! s.startsWith(MONITOR_PREFIX))
            {
                temp.add(s);
            }
        }

        channelList = (cdpf != null) ? IChannelValueFetch.asArray(cdpf.lookupSseChannelIds())
                : IChannelValueFetch.asArray(temp);

        if (channelIdMode             &&
            (channelList.length == 0) &&
            cts.sse)
        {
            cts.sse = false;
        }

        // BEGIN MPCS-2473 Add module checks

        if (channelList.length > 0)
        {
            if (strict)
            {
                // Add module checks

                sseWhere = addToWhere(sseWhere,
                                      moduleWhere(moduleParameters));
            }

            // END MPCS-2473

            // Add the list of channel IDs to search for (if there are any)
            // Make sure that list is parenthesized in case of multiple
            // elements separated by OR,
            // otherwise the WHERE clause may be interpreted incorrectly.

            sseWhere = addToWhere(sseWhere,
                                  channelWhere(channelList,
                                               tableSseAbbrev));
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

        if (channelIdMode && (channelList.length == 0))
        {
            cts.sseHeader = false;
        }

        if (channelList.length > 0)
        {
            if (strict)
            {
                // Add module checks

                sseheaderWhere = addToWhere(sseheaderWhere,
                                            moduleWhere(moduleParameters));
            }

            // Add the list of channel IDs to search for (if there are any)
            // Make sure that list is parenthesized in case of multiple
            // elements separated by OR,
            // otherwise the WHERE clause may be interpreted incorrectly.

            sseheaderWhere = addToWhere(sseheaderWhere,
                                        channelWhere(channelList,
                                                     tableHeaderAbbrev));
         }

        // END OF SSE Header

        // Add the proper time clause and order by the time type
        // if no ordering has been specified
        /** MPCS-8384  Extended support */
        /** MPCS-8639 Move config item */

        final String fswTimeWhere =
            DbTimeUtility.generateTimeWhereClause(
                tableAbbrev, range, false, false, _extendedDatabase);
        final String sseTimeWhere =
            DbTimeUtility.generateTimeWhereClause(
                tableSseAbbrev, range, false, false,  _extendedDatabase);

        String headerTimeWhere    = null;
        String sseheaderTimeWhere = null;

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

        /** MPCS-8384 Extended support */

        switch(range.getTimeType().getValueAsInt())
        {
            case DatabaseTimeType.SCET_TYPE:
                if(orders.isEmpty())
                {
                    if (ecdrMode)
                    {
                        orders.add(EcdrOrderByType.SCET);
                    }
                    else
                    {
                        orders.add(ChannelValueOrderByType.SCET);
                    }
                }

                headerTimeWhere    = DbTimeUtility.generateTimeWhereClause(
                                         tableHeaderPacketAbbrev, range, false, false,  _extendedDatabase);
                sseheaderTimeWhere = headerTimeWhere;
                break;

            case DatabaseTimeType.LST_TYPE:
                if(orders.isEmpty())
                {
                    orders.add(ChannelValueOrderByType.LST);
                }

                headerTimeWhere    = DbTimeUtility.generateTimeWhereClause(
                                         tableHeaderPacketAbbrev, range, false, false,  _extendedDatabase);
                sseheaderTimeWhere = headerTimeWhere;
                break;

            case DatabaseTimeType.ERT_TYPE:
                final String monitorTimeWhere =
                    DbTimeUtility.generateTimeWhereClause(
                        tableMonitorAbbrev, range, false, true,  _extendedDatabase);

                if (monitorTimeWhere.length() > 0)
                {
                    monitorWhere = addToWhere(monitorWhere, monitorTimeWhere);
                }

                if (orders.isEmpty())
                {
                    if (ecdrMode)
                    {
                        if (cts.monitor)
                        {
                            // Can have ONLY monitor
                            orders.add(EcdrMonitorOrderByType.ERT);
                        }
                        else
                        {
                            orders.add(EcdrOrderByType.ERT);
                        }
                    }
                    else
                    {
                        orders.add(ChannelValueOrderByType.ERT);
                    }
                }

                headerTimeWhere    = DbTimeUtility.generateTimeWhereClause(
                                         tableHeaderAbbrev, range, false, false, _extendedDatabase);
                sseheaderTimeWhere = headerTimeWhere;
                break;

            case DatabaseTimeType.SCLK_TYPE:
                if(orders.isEmpty())
                {
                    if (ecdrMode)
                    {
                        orders.add(EcdrOrderByType.SCLK);
                    }
                    else
                    {
                        orders.add(ChannelValueOrderByType.SCLK);
                    }
                }

                headerTimeWhere    = DbTimeUtility.generateTimeWhereClause(
                                         tableHeaderPacketAbbrev, range, false, false, _extendedDatabase);
                sseheaderTimeWhere = headerTimeWhere;
                break;

            default:
                break;
        }

        if ((headerTimeWhere != null) && ! headerTimeWhere.isEmpty())
        {
            // Header now takes all time types

            headerWhere = addToWhere(headerWhere, headerTimeWhere);
        }

        if ((sseheaderTimeWhere != null) && ! sseheaderTimeWhere.isEmpty())
        {
            // Header now takes all time types

            sseheaderWhere = addToWhere(sseheaderWhere, sseheaderTimeWhere);
        }

        if (cts.fswRealtime != cts.fswRecorded)
        {
            // Not both

            final StringBuilder realtimeClause =
                new StringBuilder();

            realtimeClause.append('(');
            realtimeClause.append(tableAbbrev);
            realtimeClause.append(".isRealtime ");

            if (cts.fswRealtime)
            {
                // FSW realtime
                realtimeClause.append("!= 0)");
            }
            else
            {
                // FSW recorded
                realtimeClause.append("= 0)");
            }

            channelWhere = addToWhere(channelWhere, realtimeClause.toString());
        }

        if ((alarms != null) && ! alarms.isEmpty())
        {
            channelWhere   = addToWhere(channelWhere,   alarms.whereClause(tableAbbrev));
            monitorWhere   = addToWhere(monitorWhere,   alarms.whereClause(tableMonitorAbbrev));
            headerWhere    = addToWhere(headerWhere,    alarms.whereClause(tableHeaderAbbrev));
            sseWhere       = addToWhere(sseWhere,       alarms.whereClause(tableSseAbbrev));
            sseheaderWhere = addToWhere(sseheaderWhere, alarms.whereClause(tableHeaderAbbrev));
        }

        if ((vcids != null) && ! vcids.isEmpty())
        {
            channelWhere = addToWhere(channelWhere,
                                      generateVcidWhere(vcids,
                                                        tableAbbrev,
                                                        true));
            headerWhere = addToWhere(headerWhere,
                                     generateVcidWhere(vcids,
                                                       tableHeaderAbbrev,
                                                       true));
        }

        if ((dssIds != null) && ! dssIds.isEmpty())
        {
            channelWhere = addToWhere(channelWhere,
                                      generateDssIdWhere(dssIds,
                                                         tableAbbrev));
            monitorWhere = addToWhere(monitorWhere,
                                      generateDssIdWhere(dssIds,
                                                         tableMonitorAbbrev));
            headerWhere = addToWhere(headerWhere,
                                     generateDssIdWhere(dssIds,
                                                        tableHeaderAbbrev));
        }

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
        }

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
     * {@inheritDoc}
     */
    @Override
    public String getSqlWhereClause(final String            testSqlTemplate,
                                       final DatabaseTimeRange range,
                                       final Object...         params) throws DatabaseException
    {
        String[] clauses;
        clauses = getSqlWhereClauses(new String[] { testSqlTemplate }, range, params);

        final StringBuilder clause = new StringBuilder();

        if (clauses.length > 0)
        {
            clause.append(clauses[0]);

            if (clauses.length > 1)
            {
                // last clause should be an order by clause or an empty string!
                clause.append(clauses[clauses.length - 1]);
            }
        }

        return clause.toString();
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

            if (ob instanceof IEcdrOrderByType)
            {
                clause = ((EcdrOrderByType) ob).getOrderByClause(descending);
            }
            else if (ob instanceof IEcdrMonitorOrderByType)
            {
                clause = ((EcdrMonitorOrderByType) ob).getOrderByClause(
                             descending);
            }
            else
            {
                clause = ((ChannelValueOrderByType) ob).getOrderByClause(
                             descending);
            }

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
     * Version with a single where clause instead of an array. See below.
     *
     * @param tsi         The test session information object
     * @param range       The database time range
     * @param whereClause The templated where clause for the SQL query
     * @param params      Array of parameters used to fill in where-clause values
     *
     * @return The list of channel values that matched the input search information
     */
    @Override
    protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
            final String whereClause, final Object... params) throws DatabaseException {
        return populateAndExecute(tsi, range, new String[] {whereClause}, params);
    }


    /**
     * Populate the templated WHERE clause portion of the SQL and then execute
     * the query and return the results of the query.
     *
     * @param tsi
     *            The test session information object used to specify test
     *            sessions in the database to retrieve. This object should be
     *            exactly the same as the one passed to "getChannelValues(...)"
     *            or unpredictable behavior may occur.
     * @param range
     *            The database time range specifying the type of time and time
     *            range for querying. If null, the default time type and a time
     *            range of 0 to infinity will be used.
     * @param whereClause
     *            The templated where clause(s) for the SQL query (templated
     *            means that there are ?'s in place of all the values that need
     *            to be filled in).
     * @param params
     *            Array of parameters used to fill in where-clause values
     *
     * @return The list of channel values that matched the input search
     *         information
     * @throws DatabaseException
     *             if a database error occurs
     */
    protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
            final String[] whereClause, final Object... params) throws DatabaseException {
        /** MPCS-5008  Throughout */

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

        // final String[] channelIds = (String[]) params[0];

        final String            module = (String)            params[1];
        final ChannelTypeSelect cts    = (ChannelTypeSelect) params[2];

        final List<AbstractOrderByType> orders =
            SystemUtilities.<List<AbstractOrderByType>>castNoWarning(
                params[3]);

        final QueryClauseType queryClause =
            (includePacketInfo ? QueryClauseType.PACKET_JOIN
                               : QueryClauseType.NO_JOIN);

        /** MPCS-5008  */
        final QueryClauseType queryClauseSseHeader =
            (includePacketInfo ? QueryClauseType.PACKET_JOIN_SSE
                               : QueryClauseType.NO_JOIN_SSE);

        String sql = null; // SQL used in query

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
                                   getActualTableName(IChannelValueLDIStore.DB_CHANNEL_VALUE_TABLE_NAME));

            selectClauses[1] = queryConfig.getQueryClause(
                                   queryClause,
                                   IMonitorChannelValueLDIStore.DB_MONITOR_CHANNEL_VALUE_TABLE_NAME);

            selectClauses[2] = queryConfig.getQueryClause(
                                   queryClause,
                                   getActualTableName(IHeaderChannelValueLDIStore.DB_HEADER_CHANNEL_VALUE_TABLE_NAME));

            selectClauses[3] = queryConfig.getQueryClause(
                                   queryClause,
                                   getActualTableName(ISseChannelValueLDIStore.DB_SSE_CHANNEL_VALUE_DATA_TABLE_NAME));

            selectClauses[4] = queryConfig.getQueryClause(
                                   queryClauseSseHeader,
                                   getActualTableName(IHeaderChannelValueLDIStore.DB_HEADER_CHANNEL_VALUE_TABLE_NAME));

            final String[] groupByClauses = new String[5];

            Arrays.fill(groupByClauses, null);

            if (ecdrMode || uniqueMode)
            {
                groupByClauses[0] = queryConfig.getQueryClause(
                                        QueryClauseType.ECDR_GROUP_BY,
                                        getActualTableName(IChannelValueLDIStore.DB_CHANNEL_VALUE_TABLE_NAME));

                groupByClauses[1] =
                    queryConfig.getQueryClause(
                        QueryClauseType.ECDR_GROUP_BY,
                        IMonitorChannelValueLDIStore.DB_MONITOR_CHANNEL_VALUE_TABLE_NAME);

                groupByClauses[2] =
                    queryConfig.getQueryClause(
                        QueryClauseType.ECDR_GROUP_BY,
                        getActualTableName(IHeaderChannelValueLDIStore.DB_HEADER_CHANNEL_VALUE_TABLE_NAME));

                /** MPCS-5008 SSE allowed for ECDR now */

                groupByClauses[3] =
                    queryConfig.getQueryClause(
                        QueryClauseType.ECDR_GROUP_BY,
                        getActualTableName(ISseChannelValueLDIStore.DB_SSE_CHANNEL_VALUE_DATA_TABLE_NAME));

                groupByClauses[4] = groupByClauses[2];
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
                sqlClause.append("SELECT 1 FROM " + IChannelValueLDIStore.DB_CHANNEL_VALUE_TABLE_NAME + " WHERE (0=1)");
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

        return(getResults());
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelValueFetch#getNextResultBatch()
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
    protected List<IDbChannelSampleProvider> getResults() throws DatabaseException
    {
        final List<IDbChannelSampleProvider> refs = new ArrayList<IDbChannelSampleProvider>();

        if(this.results == null)
        {
            return(refs);
        }

        int count = 0;
        try
        {
            /** MPCS-6718 Look for warnings */

            final List<SQLWarning> warnings = new ArrayList<SQLWarning>();

            //loop through until we fill up our first batch or we've
            //got no more results
            while(count < this.batchSize)
            {
                if(this.results.next() == false)
                {
                    break;
                }

                final Long testSessionId = this.results.getLong(SESSION_ID);

                /** MPCS-6718  Look for warnings */

                warnings.clear();

                final SessionFragmentHolder sessionFragment =
                    SessionFragmentHolder.getFromDbRethrow(results, FRAGMENT_ID, warnings);

                SqlExceptionTools.logWarning(warnings, trace);

                final int hostId = this.results.getInt(HOST_ID);

                final String host = spf.lookupHost(hostId);

                if ((host == null) || host.length() == 0)
                {
                    throw new DatabaseException("Failure to find sessionHost for "+
                                           "hostId " + hostId);
                }

                final int scid = spf.lookupSCID(hostId, testSessionId);

                final String channelId = this.results.getString(CHANNEL_ID_COL);
                final String name = this.results.getString(CHANNEL_NAME_COL);

                final long temp         = results.getLong(CHANNEL_INDEX_COL);
                Long       channelIndex = null;

                if (! results.wasNull() && (temp > 0L))
                {
                    channelIndex = temp;
                }

                final IAccurateDateTime rct = DbTimeUtility.dateFromCoarseFine(
                                     this.results.getLong(RCT_COARSE_COL),
                                     this.results.getInt(RCT_FINE_COL));

                // Set SCLK as dummy if result is NULL
                final long coarseSclk = this.results.getLong(SCLK_COARSE_COL);
                boolean    wasNull    = this.results.wasNull();
                ISclk       sclk       = null;

                if (wasNull)
                {
                    sclk = new Sclk(true);
                }
                else
                {
                    final long fineSclk = this.results.getLong(SCLK_FINE_COL);

                    sclk = new Sclk(coarseSclk, fineSclk);
                }

                long coarse = this.results.getLong(ERT_COARSE_COL);
                int  fine   = this.results.getInt(ERT_FINE_COL);

                final IAccurateDateTime ert = new AccurateDateTime(
                                                     DbTimeUtility.exactFromErtCoarseFine(coarse, fine),
                                                     DbTimeUtility.fineFromErtFine(fine));

                coarse  = this.results.getLong(SCET_COARSE_COL);
                wasNull = this.results.wasNull();
                fine    = this.results.getInt(SCET_FINE_COL);

                 /** MPCS-8384  */
                if (! _extendedDatabase)
                {
                    fine *= DbTimeUtility.SCET_SHORT_CONVERSION;
                }

                /** BEGIN MPCS-5456 3 Move parentType here from below and redo SCET */

                final ChannelCategoryEnum cce =
                    ParentTypeEnum.convertFromExtendedEnum(results.getString(PARENT_TYPE_COL));

                IAccurateDateTime scet         = null;
                boolean          haveRealScet = false;

                if (! wasNull)
                {
                    /** MPCS-8384*/
                    scet = new AccurateDateTime(DbTimeUtility.exactFromScetCoarseFine(coarse, fine),
                                                DbTimeUtility.fineFromScetFine(fine));

                    haveRealScet = true;
                }
// MPCS-8322 : removed 'FAKE_SCET'
//                else if (FAKE_SCET && (cce == ChannelCategoryEnum.FRAME_HEADER)) /** MPCS-5610  */
//                {
//                    // Set from the ERT (so not real)
//                    scet = new AccurateDateTime(ert.getTime());
//                }
                else
                {
                    // Set as a dummy (certainly not real)
                    scet = new AccurateDateTime(true);
                }

                ILocalSolarTime sol = null;

                // Set local solar time for channels that have a real SCET (see method comments)

                if (haveRealScet)
                {
                    final VenueType vt = spf.lookupVenue(hostId, testSessionId);

                    if (missionProps.getVenueUsesSol(vt) && TimeProperties.getInstance().usesLst())
                    {
                        sol = LocalSolarTimeFactory.getNewLst(scet, scid);
                    }
                }

                /** END MPCS-5456 */

                // Armor our enum against a bad enum from DB. Yes, it just
                // postpones the error.

                final String ct =
                    StringUtil.safeTrim(
                        this.results.getString(CHANNEL_TYPE_COL));

                /** BEGIN MPCS-7917  Make sure bad value is UNKNOWN */
                /** MPCS-7106 No need for typeLookup map */

                ChannelType channelType = ChannelType.UNKNOWN;

                try
                {
                    channelType = ChannelType.valueOf(ct);
                }
                catch (final IllegalArgumentException iae)
                {
                    trace.warn("Unsupported channel type '" +
                             ct                           +
                             "' found, setting to "       +
                             ChannelType.UNKNOWN);
                }

                /** END MPCS-7917 */

                final String module = this.results.getString(MODULE_COL);
                final String dnFormat = this.results.getString(DN_FORMAT_COL);
                final String euFormat = this.results.getString(EU_FORMAT_COL);

                final boolean fromSse = GDR.getBooleanFromInt(this.results.getInt(FROM_SSE_COL));
                final boolean isRealtime = GDR.getBooleanFromInt(this.results.getInt(REALTIME_COL));

                final Double eu = fetchSafeDouble(results, true);

                Object val = null;
                String status = null;
                /* MPCS-6115 -  Added TIME case below. */
                switch(channelType)
                {
                    case ASCII:

                        val = this.results.getString(STRING_COL);

                        break;

                    case BOOLEAN:
                        /** MPCS-6809 As unsigned */
                        /** MPCS-7639 Use new method */

                        val = getUnsignedLong(results, UNSIGNED_COL).longValue();

                        if(((Long)val).longValue() == 0)
                        {
                            status = Boolean.FALSE.toString();
                        }
                        else
                        {
                            status = Boolean.TRUE.toString();
                        }

                        break;

                    case DIGITAL:
                    case UNSIGNED_INT:
                    case TIME:    
                        /** MPCS-6809  As unsigned */
                        /** MPCS-7639  Use new method */

                        val = getUnsignedLong(results, UNSIGNED_COL).longValue();

                        break;


                     case STATUS:

                        // Note: We changed the definition of a status channel from unsigned to signed in the v3_1 database.
                        // So look for first one, then the other.

                        /** MPCS-6809 As unsigned */
                        /** MPCS-7639  Use new method */

                        final UnsignedLong ul = getUnsignedLong(results, UNSIGNED_COL);

                        if (ul != null)
                        {
                            val = ul.longValue();
                        }
                        else
                        {
                            val = results.getLong(SIGNED_COL);
                        }

                        status = this.results.getString(STRING_COL);
                        break;

                    case FLOAT:

                        /* MPCS-6115 - Removed check for DOUBLE type. */
                        val = fetchSafeDouble(results, false);

                        break;

                    case SIGNED_INT:

                        val = this.results.getLong(SIGNED_COL);

                        break;

                    /** MPCS-7917 Do not throw */
                    case UNKNOWN:
                    default:

                        val = "";

                        trace.warn("Unsupported channel type " +
                                 channelType                 +
                                 ", assuming empty string");
                        break;
                }

                String dnAlarm = this.results.getString(DN_ALARM_COL);
                if (dnAlarm != null && dnAlarm.isEmpty()) {
                    dnAlarm = null;
                }
                String euAlarm = this.results.getString(EU_ALARM_COL);
                if (euAlarm != null && euAlarm.isEmpty()) {
                    euAlarm = null;
                }

                // MPCS-4839  Verify and log and correct
                final int dssId = getCorrectedStation(results, DSS_ID_COL);

                Integer vcid = this.results.getInt(VCID_COL);

                if (results.wasNull() || (vcid < 0))
                {
                    vcid = null;
                }

                PacketIdHolder packetId = null;

                try
                {
                    /** MPCS-6718 Look for warnings */

                    warnings.clear();

                    packetId = PacketIdHolder.getFromDb(results, PACKET_ID_COL, warnings);

                    SqlExceptionTools.logWarning(warnings, trace);
                }
                catch (final HolderException he)
                {
                    throw new DatabaseException("Problem reading Packet columns", he);
                }

                /** MPCS-6809  As unsigned */
                /** MPCS-7639  Use new method */

                final UnsignedLong ul      = getUnsignedLong(results, FRAME_ID_COL);
                Long               frameId = null;

                if (ul != null)
                {
                    frameId = ul.longValue();
                }

                /** MPCS-5456  Move parentType up */

                // If we are joining with Packet, get extra columns

                ApidHolder apid = null;
                ApidNameHolder apidName = null;
                SpscHolder spsc = null;
                IAccurateDateTime packetRct = null;
                VcfcHolder vcfc = null;

                if (includePacketInfo)
                {
                    try
                    {
                        /** MPCS-6718  Look for warnings */

                        warnings.clear();

                        apid      = ApidHolder.getFromDb(results, APID_COL, warnings);
                        apidName  = ApidNameHolder.getFromDb(results,
                                                             APIDNAME_COL, warnings);
                        spsc      = SpscHolder.getFromDb(results, SPSC_COL, warnings);
                        packetRct = DbTimeUtility.dateFromCoarseFine(
                                        results.getLong(PRCTC_COL),
                                        results.getInt( PRCTF_COL));
                        vcfc      = VcfcHolder.getFromDb(results,
                                                         VCFC_COL,
                                                         warnings);

                        SqlExceptionTools.logWarning(warnings, trace);
                    }
                    catch (final HolderException he)
                    {
                        throw new DatabaseException(
                                      "Problem reading Packet columns",
                                      he);
                    }
                }

                final IDbChannelSampleUpdater ref = dbChannelSampleFactory.createQueryableUpdater(
                                                                      testSessionId,
                                                                      fromSse,
                                                                      isRealtime,
                                                                      sclk,
                                                                      ert,
                                                                      scet,
                                                                      sol,
                                                                      val,
                                                                      channelType,
                                                                      channelId,
                                                                      channelIndex,
                                                                      module,
                                                                      host,
                                                                      eu,
                                                                      dnAlarm,
                                                                      euAlarm,
                                                                      status,
                                                                      scid,
                                                                      name,
                                                                      dssId,
                                                                      vcid,
                                                                      rct,
                                                                      packetId,
                                                                      frameId);
                ref.setDnFormat(dnFormat);
                ref.setEuFormat(euFormat);
                ref.setCategory(cce);
                ref.setSessionFragment(sessionFragment);

                if (includePacketInfo)
                {
                    ref.setPacketInfo(apid, apidName, spsc, packetRct, vcfc);
                }

                // if user wants all values put all
                // the values we get on the return list
                if (this.allChangeValues)
                {
                    refs.add(ref);
                    count++;
                } else {
                    // user wants only changes
                    final boolean doNotFilter = this.changeFilter.getFilteredValue(ref.getChannelId(),
                            ref.getChannelType(), ref.getValue());
                    if (doNotFilter) {
                        refs.add(ref);
                        ref.setDeltaValue(this.changeFilter.getDelta(channelId));
                        ref.setPreviousValue(this.changeFilter.getPreviousValue(channelId));
                        count++;
                    }
                }
            }

            // Handle any unhandled warnings
            /** MPCS-6718  */
            SqlExceptionTools.logWarning(trace, results);

            //if we're all done with results, clean up
            //all the resources
            if(this.results.isAfterLast() == true)
            {
                this.results.close();
                this.plainStatement.close();

                this.results = null;
                this.plainStatement = null;
            }
        }
        catch(final SQLException e)
        {
            throw new DatabaseException("Error retrieving channel values " +
                                       "from database: "              +
                                       e.getMessage(),
                                   e);
        }

        return refs;
    }

    /**
     * Produce where clause for channel ids.
     *
     * @param channelIds Array of channel ids
     * @param abbrev     Table abbreviation
     *
     * @return Where clause as string
     */
    private String channelWhere(final String[] channelIds,
                                final String   abbrev)
    {
        if (channelIds.length == 0)
        {
            return "";
        }

        final StringBuilder sb      = new StringBuilder();
        final boolean       justOne = (channelIds.length == 1);
        boolean             first   = true;

        if (! justOne)
        {
            sb.append('(');
        }

        for (final String cid : channelIds)
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

            sb.append('(');
            sb.append(abbrev).append(".channelIdCrc=");

            if (cdpf != null)
            {
                final Long crc32 = cdpf.lookupCRC32(cid);

                if (crc32 != null)
                {
                    sb.append(crc32);
                }
                else
                {
                    sb.append("CRC32('").append(cid).append("')");
                }
            }
            else
            {
                sb.append("CRC32('").append(cid).append("')");
            }

            sb.append(')');

            sb.append(" AND ");

            sb.append('(');
            sb.append(tableDataAbbrev).append(".channelId");
            sb.append('=').append(SQ).append(cid).append(SQ);
            sb.append(')');

            sb.append(')');
        }

        if (! justOne)
        {
            sb.append(')');
        }

        return sb.toString();
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


    /**
     * Look for any wildcards in a module pattern.
     *
     * @param module Module pattern
     *
     * @return True if there is at least one wildcard
     */
    private static boolean hasWildcard(final String module)
    {
        return (module.contains(ILDIStore.WILDCARD1) ||
                module.contains(ILDIStore.WILDCARD2));
    }


    /**
     * Fetch a double value, with special checks for NaN and infinity.
     *
     * @param results
     *            Result set
     * @param eu
     *            True if EU, else DN
     *
     * @return Result, which might be NaN or infinite
     *
     * @throws DatabaseException
     */
    private static Double fetchSafeDouble(final ResultSet results, final boolean eu) throws DatabaseException {
        try {
            final double val = results.getDouble(eu ? EU_COL : DOUBLE_COL);

            if (!results.wasNull()) {
                // Not a NULL, so take the value as is
                return val;
            }

            // NULL, so check flag column

            final String flag = results.getString(eu ? EU_FLAG_COL : DOUBLE_FLAG_COL);

            if (flag == null) {
                // Not set, which means that extra information was not
                // stored, which means old data, so we use zero for DN.
                // EU can actually be legitimately NULL.

                return (eu ? null : 0.0D);
            }

            // Turn it back to a double

            if (flag.equalsIgnoreCase(ILDIStore.INFINITY)) {
                return Double.POSITIVE_INFINITY;
            }

            if (flag.equalsIgnoreCase(ILDIStore.NEGATIVE_INFINITY)) {
                return Double.NEGATIVE_INFINITY;
            }

            if (flag.equalsIgnoreCase(ILDIStore.NAN)) {
                return Double.NaN;
            }

            // Not what was expected; treat like null

            if (eu) {
                trace.warn("Unexpected EU flag value '" + flag + "'");
            }
            else {
                trace.warn("Unexpected dnDouble flag value '" + flag + "'");
            }

            return (eu ? null : 0.0D);
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }

    /**
     * Produce where clause for modules.
     *
     * @param modules Array of module patterns
     *
     * @return Where clause as string
     */
    private String moduleWhere(final String[] modules)
    {
        // MPCS-2473  New method

        if (modules.length == 0)
        {
            return "";
        }

        final StringBuilder sb      = new StringBuilder();
        final boolean       justOne = (modules.length == 1);
        boolean             first   = true;

        if (! justOne)
        {
            sb.append('(');
        }

        for (final String module : modules)
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

            sb.append(tableDataAbbrev).append(".module");

            if (hasWildcard(module))
            {
                sb.append(" LIKE ");
            }
            else
            {
                sb.append('=');
            }

            sb.append(SQ).append(module).append(SQ);

            sb.append(')');
        }

        if (! justOne)
        {
            sb.append(')');
        }

        return sb.toString();
    }
}
