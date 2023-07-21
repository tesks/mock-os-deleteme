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
package jpl.gds.db.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpl.gds.shared.time.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.AlarmControl;
import jpl.gds.db.api.sql.fetch.ChannelTypeSelect;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchMarkers;
import jpl.gds.db.api.sql.fetch.aggregate.IAggregateFetchConfig;
import jpl.gds.db.api.sql.fetch.aggregate.IChannelAggregateFetch;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator;
import jpl.gds.db.api.sql.order.IChannelAggregateOrderByType;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.mysql.impl.sql.order.ChannelAggregateOrderByType;
import jpl.gds.db.mysql.impl.sql.order.ChannelValueOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.sys.FlushBool;


/**
 * The channel aggregate fetch app is the command line application used to retrieve
 * channel aggregates from the database.
 *
 */
public class ChannelAggregateFetchApp extends AbstractChannelValueFetchApp
{
    private static final String  APP_NAME                 = ApplicationConfiguration.getApplicationName("chill_get_chanvals");

    /** Long option for includePacketInfo */
    public static final String   INCLUDE_PACKET_INFO_LONG = "includePacketInfo";

    /** Long option for fromUniquePackets */
    public static final String   FROM_UNIQUE_PACKETS_LONG = "fromUniquePackets";

    private final String         CHUNK_SIZE_LONG          = "chunkSize";
    private final String         CHUNK_DIR_LONG           = "chunkDir";
    private final String         PARALLEL_THREADS_LONG    = "pthreads";
    private final String		 KEEP_TEMP_FILES          = "keepTempFiles";
    

    /** Size of query parameter array */
    public static final int      NUM_QUERY_PARAMS         = 11;

    /**
     * Alarms to query for
     */
    protected final AlarmControl alarms                   = new AlarmControl();

    /**
     * True if all channel values are required
     */
    protected boolean            allChannelValues         = true;
    
    /** If true, join with packet */
    private boolean              includePacketInfo        = false;

    /** If true, remove duplicates */
    private boolean              fromUniquePackets        = false;
    
    private IAggregateFetchConfig config;

    private int            chunkSize                = 0;
    private String               chunkDir                 = "";
    private int                  pthreads                 = 4;
    
    private static long appStartTime;
    private static long appEndTime;
    
    private IEhaAggregateQueryCoordinator ehaAggregateQueryQoordinator;
    private boolean working = false;

    /**
     * Creates an instance of ChannelAggregateFetchApp.
     *
     */
    public ChannelAggregateFetchApp()
    {
        super(APP_NAME, "ChanvalQuery");
        this.config = appContext.getBean(IAggregateFetchConfig.class);
        this.ehaAggregateQueryQoordinator = appContext.getBean(IEhaAggregateQueryCoordinator.class);

        suppressInfo();
    }

    /**
     *
     * @param appName
     *            the name of the applicaation
     * @param app
     *            CSV application type
     */
    protected ChannelAggregateFetchApp(final String appName, final String app) {
        super(appName, app);
    }


    /**
     * Get alarms.
     *
     * @return Alarms
     */
    public AlarmControl getAlarms()
    {
        return alarms;
    }


    /**
     * Get is-all-channel-values state.
     *
     * @return Is-all-channel-values state
     */
    @Override
    public boolean isAllChannelValues()
    {
        return allChannelValues;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void addGlobalContext()
    {
        DateFormat df = null;
        try
        {
            df = TimeUtility.getFormatterFromPool();

            final int scid = missionProps.getDefaultScid();
            final String name =  missionProps.mapScidToName(scid);

            globalContext.put("spacecraftID", scid);
            if (name != null) {
                globalContext.put("spacecraftName", name);
            } else {
                globalContext.put("spacecraftName", "");
            }
            globalContext.put("productCreationTime",df.format(new AccurateDateTime()));
            globalContext.put("missionName", missionProps.getMissionLongName());
            if (beginTimeString != null) {
                globalContext.put("startTime",beginTimeString);
            } else {
                globalContext.put("startTime","");
            }
            if (endTimeString != null) {
                globalContext.put("stopTime",endTimeString);
            } else {
                globalContext.put("stopTime","");
            }

            final String[] channelIds = getChannelIds();

            final StringBuilder channelList = new StringBuilder(1024);
            channelList.append("[");
            if(channelIds.length > 0)
            {
                channelList.append(channelIds[0]);
                for(int i=1; i < channelIds.length; i++)
                {
                    channelList.append(" ");
                    channelList.append(channelIds[i]);
                }
            }
            channelList.append("]");
            globalContext.put("channelValues",channelList.toString());
        }
        finally
        {
            if (df != null)
            {
                TimeUtility.releaseFormatterToPool(df);
            }
        }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void createRequiredOptions() throws ParseException
    {
        super.createRequiredOptions();

        requiredOptions.add(ALARM_ONLY_LONG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException {

        super.configureApp(cmdline);
        
        if (cmdline.hasOption(FILE_SHORT))
        {

            final String outFile =
                StringUtil.emptyAsNull(cmdline.getOptionValue(FILE_SHORT));

        	if (outFile == null)
        	{
        		throw new MissingArgumentException("--" + FILE_LONG + " requires a filename argument");
        	}

            try
            {
                setOutputFilename(outFile);
            }
            catch (final FileNotFoundException fnfe)
            {
                throw new ParseException("File " +
                                         outFile +
                                         " cannot be created");
            }
            config.setOutputFile(outFile);
        }
        
        if (cmdline.hasOption(CHUNK_SIZE_LONG)) {
            try {
                chunkSize = Integer.parseInt(cmdline.getOptionValue(CHUNK_SIZE_LONG));
                config.setChunkSize(chunkSize);
            }
            catch (final Exception e) {
                throw new ParseException("Invalid chunk size " + cmdline.getOptionValue(CHUNK_SIZE_LONG)
                        + " " + ExceptionTools.getMessage(e));
            }
        }

        if (cmdline.hasOption(CHUNK_DIR_LONG)) {
            final String value = cmdline.getOptionValue(CHUNK_DIR_LONG);
            final File f = new File(value);
            if (!(f.exists() || f.isDirectory())) {
                throw new ParseException("Invalid chunk directory " + value);
            }
            else {
                this.chunkDir = f.getAbsolutePath();
            }
            config.setChunkDir(chunkDir);
        }

        if (cmdline.hasOption(PARALLEL_THREADS_LONG)) {
            try {
                this.pthreads = Integer.parseInt(cmdline.getOptionValue(PARALLEL_THREADS_LONG));
            }
            catch (final Exception e) {
                throw new ParseException("Invalid thread size " + cmdline.getOptionValue(CHUNK_SIZE_LONG) + " "
                        + ExceptionTools.getMessage(e));
            }
            config.setParallelThreads(pthreads);
        }       
        
        if (cmdline.hasOption(KEEP_TEMP_FILES)) {
            config.setKeepTempFiles(true);
        }
        
        final List<ChannelAggregateOrderByType> obt =
                new ArrayList<ChannelAggregateOrderByType>();
        
        if (orderByString == null) {
        	orderByString = config.getOrderByString();
        }
        
        orderByString = orderByString.trim();
 
        ChannelAggregateOrderByType cvobt = null;
        try
        {
            cvobt = new ChannelAggregateOrderByType(orderByString);
            
            config.setOrderByType(cvobt.getValueAsInt());
            
            if (!cvobt.equals(ChannelAggregateOrderByType.NONE)) {
            	config.setSortEnabled();
            }
        }
        catch (final IllegalArgumentException iae)
        {
            throw new ParseException("The value '" +
                                     orderByString +
                                     "' is not a legal ordering value for this application");
        }
        
        obt.add(cvobt);
        
        // Are all channel values required or just changes?
        allChannelValues = ! cmdline.hasOption(CHANGE_VALUES_LONG);
        
        if(!allChannelValues) {
            // --changesOnly works with default CSV output only
            // and can't be combined with Velocity Template output
            // format option: -o,--outputFormat
            if (template != null) {
                throw new ParseException("--" + CHANGE_VALUES_LONG + " is supported with default CSV output only, "
                        + "cannot be combined with -o,--outputFormat option.");
            }
            config.setChangesOnly();
        }
        
        if (! allChannelValues      &&
            (orderByString != null) &&
            orderByString.equalsIgnoreCase(ChannelAggregateOrderByType.NONE.getValueAsString()))
        {
            throw new ParseException("--"                                        +
                                     CHANGE_VALUES_LONG                         +
                                     " does not work properly with order-by of " +
                                     ChannelValueOrderByType.NONE);
        }

        if(cmdline.hasOption(ALARM_ONLY_LONG))
        {
            alarms.processOptions(cmdline.getOptionValue(ALARM_ONLY_LONG),ALARM_ONLY_LONG);
            config.setAlarms(alarms);
        }

        includePacketInfo = cmdline.hasOption(INCLUDE_PACKET_INFO_LONG);


        if (includePacketInfo)
        {
            resetApplicationType("ChanvalPacketQuery");
        }

        fromUniquePackets = cmdline.hasOption(FROM_UNIQUE_PACKETS_LONG);

        // Process channel type selection arguments

        final ChannelTypeSelect cts = getChannelTypeSelect();

        getChannelTypesForAggregates(cmdline, cts, obt);

        setVcids(DssVcidOptions.parseVcid(
                     missionProps,
                     cmdline,
                     cts.monitor ? 'M' : null,
                     null,
                     CHANNEL_TYPES_LONG));

        if (fromUniquePackets   &&
            ! includePacketInfo &&
            (cts.fswRealtime ||
             cts.fswRecorded ||
             cts.header      ||
             cts.sse         ||
             cts.sseHeader))
        {
                throw new ParseException("--"                     +
                                         FROM_UNIQUE_PACKETS_LONG +
                                         " requires --"           +
                                         INCLUDE_PACKET_INFO_LONG +
                                         " for f, r, h, s, and g channels");
        }


        processIndexHints(cmdline);
    }




    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage()
    {

        final String NL = "\n[--";
        
        return (APP_NAME + " --" + CHANNEL_TYPES_LONG + " <hmsfrg>" +
                NL + CHANNEL_IDS_LONG + " <string,...> |"           +
                " --" + CHANNEL_ID_FILE_LONG + " <string>]"         +
                NL + MODULE_LONG + " <name>]"                       +
                NL + CHANGE_VALUES_LONG + "]"                       +
                NL + INCLUDE_PACKET_INFO_LONG + "]"                 +
                NL + FROM_UNIQUE_PACKETS_LONG + "]"                 +
                NL + FILE_LONG + " <filename> ]"                    +
                NL + CHUNK_SIZE_LONG + " <int>]"                    + 
                NL + CHUNK_DIR_LONG + " <directory>]"               +
                NL + PARALLEL_THREADS_LONG + " <int>]" +
                "\n[" + getExtraUsage() + "]"                       +
                "\n[Session search options - Not required]\n");
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected void showHelp()
    {
        super.showHelp();

        System.out.println("\nFor --alarmOnly, the possible alarm types are:\n" +
                           "    ANY\n"                                          +
                           "    DN DN-R DN-Y DN-RY DN-YR\n"                     +
                           "    EU EU-R EU-Y EU-RY EU-YR\n"                     +
                           "    RED R YELLOW Y\n");
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void addAppOptions() 
    {
        super.addAppOptions();

        OptionBuilder.withLongOpt(CHANGE_VALUES_LONG);
        OptionBuilder.withDescription("Channel values should only be reported on change; default is all channel values");
        options.addOption(OptionBuilder.create(CHANGE_VALUES_SHORT));
        
        addOption(FILE_SHORT,FILE_LONG, FILE_LONG, "Output file for channel data");
        

        addOption(null,
                  CHANNEL_TYPES_LONG,
                  "string",
                  ChannelTypeSelect.RETRIEVE);

        addOption(null,
                  USE_INDEX_LONG,
                  "string,...",
                  "Use index");

        addOption(null,
                  FORCE_INDEX_LONG,
                  "string,...",
                  "Force index");

        addOption(null,
                  IGNORE_INDEX_LONG,
                  "string,...",
                  "Ignore index");

        options.addOption(ReservedOptions.createOption(
                              INCLUDE_PACKET_INFO_LONG,
                              null,
                              "Include info from Packet"));

        options.addOption(ReservedOptions.createOption(
                              FROM_UNIQUE_PACKETS_LONG,
                              null,
                              "Include info from unique Packets only" +
                                  " (also works for monitor)"));

        addOption(ALARM_ONLY_SHORT,ALARM_ONLY_LONG,"alarm,...","Only retrieve values in alarm");
        
        addOption(null, CHUNK_SIZE_LONG, "grouping size",
                  "Sort size (as number of records) for grouped eha channels");
        
        addOption(null, CHUNK_DIR_LONG, "directory",
                                              "Root directory to write the sorted chunks");
        
        addOption(null, PARALLEL_THREADS_LONG, "parallel threads",
                                                  "Number of parallel sort threads to use during the sort process");
        
        addOption(null, KEEP_TEMP_FILES, null,
                "Don't delete temporary batch files");
                
        addOrderOption();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IChannelAggregateFetch getFetch(final boolean sqlStmtOnly)
    {
        fetch = appContext.getBean(IDbSqlFetchFactory.class).getChannelAggregateFetch(isSqlStmtOnly(),
                        getChannelIds(),
                        (getModule() != null) ? new String[] {getModule()}
                                              : null,
                        includePacketInfo,
                        false,
                        fromUniquePackets);
        return (IChannelAggregateFetch) fetch;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getFetchParameters()
    {
        final Object[] params = new Object[NUM_QUERY_PARAMS];

        params[0]  = new String[0]; //getChannelIds();
        params[1]  = getModule();
        params[2]  = getChannelTypeSelect();
        params[3]  = getOrderings();
        params[4]  = allChannelValues;
        params[5]  = alarms;
        params[6]  = Boolean.FALSE; // Descending
        params[7]  = getVcids();
        params[8]  = getDssIds();
        params[9]  = Collections.<Long>emptySet(); // APID holder
        params[10] = getIndexHints();

        return params;
    }


    /**
     * Get list of possible orderings.
     *
     * @return List of possible orderings
     */
    private List<IChannelAggregateOrderByType> getOrderings()
    {
        final List<IChannelAggregateOrderByType> orders = new ArrayList<IChannelAggregateOrderByType>();
        IChannelAggregateOrderByType orderType = (IChannelAggregateOrderByType) orderByTypeFactory.getOrderByType(OrderByType.CHANNEL_AGGREGATE_ORDER_BY);
        if(orderByString != null)
        {
            try
            {
                orderType = new ChannelAggregateOrderByType(orderByString.trim());
            }
            catch(final IllegalArgumentException iae)
            {
                throw new IllegalArgumentException("The value \"" + orderByString + "\" is not a legal ordering value for this application.",iae);
            }
            orders.add(orderType);
        } else {
            if (times != null) {
                switch(times.getTimeType().getValueAsInt())
                {
                case DatabaseTimeType.ERT_TYPE:
                    orderType = ChannelAggregateOrderByType.ERT;
                    break;
                case DatabaseTimeType.SCET_TYPE:
                    orderType = ChannelAggregateOrderByType.SCET;
                    break;
                case DatabaseTimeType.SCLK_TYPE:
                    orderType = ChannelAggregateOrderByType.SCLK;
                    break;
                case DatabaseTimeType.LST_TYPE:
                    orderType = ChannelAggregateOrderByType.LST;
                    break;
                case DatabaseTimeType.RCT_TYPE:
                    orderType = ChannelAggregateOrderByType.RCT;
                    break;
                default:
                    orderType = ChannelAggregateOrderByType.ERT;
                    break;
                }
            }
            orders.add(orderType);
        }
        return(orders);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseTimeType getDefaultTimeType()
    {
        return DatabaseTimeType.ERT;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void checkTimeType(final DatabaseTimeRange range) throws ParseException
    {
        switch(range.getTimeType().getValueAsInt())
        {
        case DatabaseTimeType.ERT_TYPE:
        case DatabaseTimeType.SCLK_TYPE:
        case DatabaseTimeType.SCET_TYPE:
        case DatabaseTimeType.LST_TYPE:
            break;
        default:
            throw new ParseException("TimeType is not one of: ERT, SCLK, SCET, LST");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getOrderByValues()
    {
        return (ChannelAggregateOrderByType.orderByTypes);
    }


    /**
     * Process arguments for selecting the channel types.
     *
     * NB: Called from CTab, etc.
     *
     * @param cl  Command line
     * @param cts Object to populate with selections
     * @param obt List of order-by types
     *
     * @throws ParseException           Thrown on parameter error
     * @throws MissingArgumentException Thrown on missing value
     */
    public void getChannelTypesForAggregates(final CommandLine cl, final ChannelTypeSelect cts,
            final List<ChannelAggregateOrderByType> obt)
        throws ParseException,
               MissingArgumentException
    {
        super.computeChannelTypes(cl, cts);

        final boolean ob     = ((obt != null) && ! obt.isEmpty());
        final boolean packet = cl.hasOption(INCLUDE_PACKET_INFO_LONG);
        final boolean tt     = cl.hasOption(TIME_TYPE_LONG);

        if (cts.monitor && tt)
        {
            final String type = StringUtil.safeTrim(
                                    cl.getOptionValue(TIME_TYPE_LONG));

            if ((type.length() > 0) &&
                ! type.equalsIgnoreCase("ERT") &&
                ! type.equalsIgnoreCase("RCT"))
            {
                throwTimeTypeException("M", type);
            }
        }

        if (cts.monitor && ob)
        {
            for (final ChannelAggregateOrderByType cvobt : obt)
            {
                switch (cvobt.getValueAsInt())
                {
                    case ChannelAggregateOrderByType.SCLK_TYPE:
                    case ChannelAggregateOrderByType.SCET_TYPE:
                    case ChannelAggregateOrderByType.LST_TYPE:
                    case ChannelAggregateOrderByType.MODULE_TYPE:

                        throwOrderException("M", cvobt);
                        break;

                    default:
                        break;
                }
            }
        }

        if (cts.header && ob)
        {
            for (final ChannelAggregateOrderByType cvobt : obt)
            {
                if (cvobt.getValueAsInt() ==
                		ChannelAggregateOrderByType.MODULE_TYPE)
                {
                    throwOrderException("H", cvobt);
                }
            }
        }


        if (cts.sseHeader && ob)
        {
            for (final ChannelAggregateOrderByType cvobt : obt)
            {
                if (cvobt.getValueAsInt() ==
                    IChannelAggregateOrderByType.MODULE_TYPE)
                {
                    throwOrderException("G", cvobt);
                }
            }
        }

        if (cts.monitor && packet)
        {
            throw new ParseException("Cannot set both --" +
                                     CHANNEL_TYPES_LONG   +
                                     " M and --"          +
                                     INCLUDE_PACKET_INFO_LONG);
        }
    }


    /**
     * Perform extra writes to context as needed.
     *
     * @param context Context to write to
     */
    @Override
    protected void writeHeaderMetaDataExtra(final Map<String, Object> context)
    {
        context.put("hasPacket", includePacketInfo);
    }


    /**
     * The main method to run the application
     *
     * @param args The command line arguments
     */
    public static void main(final String[] args)
    {
    	appStartTime = System.nanoTime();
        final ChannelAggregateFetchApp app = new ChannelAggregateFetchApp();
        trace.info("ChannelAggregateFetchApp main start");
        app.runMain(args);
        trace.info("ChannelAggregateFetchApp main end");
        appEndTime = System.nanoTime();
        trace.info("Total run time: " + (appEndTime-appStartTime)/(1000000.0) + " msecs");
    }
    
    
    @Override
    public void run() {
        setExitCode(SUCCESS);
        working = true;

        addGlobalContext();


        fetch = getFetch(sqlStmtOnly);

        // Make sure we connected to the database
        if (!fetch.isConnected()) {
            setExitCode(OTHER_ERROR);
            working = false;
            return;
        }
        
        PrintWriter pw = null;

        try {

            specificStartup();

            if (reportRows) {
                pw = getOverridingPrintWriter(FlushBool.YES);

                if (pw == null) {
                	if (getOutputFilename() != null) {
                		pw = new PrintWriter(new File(getOutputFilename()));
                	} else {
                		pw = new PrintWriter(System.out, true);
                	}   
                }
                config.setPrintWriter(pw);
            }


            final long resultSetStart = System.nanoTime();
            // chill_get_chanvals time based query performance issue
            // SQL has been adjusted to use the Begin Time only instead of both begin and end times.
            // See JIRA for more detail.
            // Ex SCET query: ...(ca.beginScetCoarse >= 1617323437) AND (ca.beginScetCoarse <= 1617332812)...
            //
            // This section of logic is not the fix for the performance issue. Its required to handle a boundary
            // condition due to the changes done to fix the performance issue.
            //
            // When a time based query is ran, we need to adjust the user specified start time slightly (make it earlier)
            // so that the aggregate record which spans the user specified start time boundary can be retrieved as well.
            // An aggregate record can fall right on the boundary where the aggregate begin time falls before the
            // user specified start-time and the aggregate end time right after. Output produced will still be filtered
            // by the Java code based on the User Specified time range. The adjusted time is only used for the SQL query.
            //
            // Aggregate record spans the user specified start time
            //    |--------------------... <-- User specified time range
            //  _____  _____  _____
            // [_____][_____][_____]...... <-- Sequence of contiguous aggregate records
            final int beginTimePadSeconds = config.getBeginTimePad();
            final DatabaseTimeRange adjustedTimeRange = new DatabaseTimeRange(times.getTimeType());
            if (times.isRangeSpecified()) {
                if(times.getTimeType().equals(DatabaseTimeType.SCLK)) {
                    if (times.getStartSclk() != null) {
                        final ISclk startSclk = times.getStartSclk();
                        final ISclk adjustedStartSclk = startSclk.decrement(beginTimePadSeconds, 0);
                        adjustedTimeRange.setStartSclk(adjustedStartSclk);
                    }
                    adjustedTimeRange.setStopSclk(times.getStopSclk());
                } else {
                    if (times.getStartTime() != null) {
                        final IAccurateDateTime startTime = times.getStartTime();
                        // Avoid rolling back the start time if the start time is within
                        // less than the 30s padding. issue observed when start time is within 30s of unix epoch (0).
                        final long rollBack = beginTimePadSeconds * 1000L;
                        if (startTime.getTime() >= rollBack) {
                            final IAccurateDateTime adjustedStartTime = startTime.roll(rollBack, 0,
                                    false);
                            adjustedTimeRange.setStartTime(adjustedStartTime);
                        }
                    }
                    adjustedTimeRange.setStopTime(times.getStopTime());
                }
            }
            final ResultSet resultSet = ((IChannelAggregateFetch) fetch).getResultSet(dbSessionInfo, adjustedTimeRange,
                    defaultBatchSize,
                    getFetchParameters());
            
            if (resultSet == null) {
                working = false;
            	return;
            } else if (!resultSet.isBeforeFirst()) {
                trace.debug(AggregateFetchMarkers.CHANNEL_AGGREGATE_FETCH_APP, "ResultSet is empty");
                // chill_get_chanvals exit code 2 when specifying time range with no data
                // Don't set exit code to 2 if we get empty results
                working = false;
                return;
            }
            trace.debug(AggregateFetchMarkers.CHANNEL_AGGREGATE_FETCH_APP, "Got the ResultsSet");
            final long resultSetEnd = System.nanoTime();
            final long processingStart = System.nanoTime();
            
            final Set<String> channelLookup = new HashSet<>();
            channelLookup.addAll(((IChannelAggregateFetch)fetch).getFullChannelIdList());
            
            final List<IChannelAggregateOrderByType> orderings = getOrderings();
            config.setTableName(tableName);
            config.setTemplateName(cmdline.getOptionValue(OUTPUT_FORMAT_SHORT, null));
            config.setTemplateGlobalContext(globalContext);
            config.setCsvHeaders(csvHeaders);
            config.setCsvColumns(csvColumns);
            config.setChannelLookupTable(channelLookup);
            config.setOrderings(orderings);
            // chill_get_chanvals time based query performance issue
            // DO NOT set this time range to the adjusted time. This should be the user specified time range.
            // User specified time range is necessary here for proper filtering.
            config.setDatabaseTimeRanges(getTimes());
            config.setModulePattern(getModule());
            config.setShowColumnHeaders(showColHeaders);
            config.setIncludePacketInfo(includePacketInfo);
            config.setSessionPreFetch(((IChannelAggregateFetch)fetch).getSessionPreFetch());
            
            ehaAggregateQueryQoordinator.setResultSet(resultSet);
        	
        	final Thread coordinatorThread = new Thread(ehaAggregateQueryQoordinator, "EhaAggregateQueryCoordinator");
        	coordinatorThread.start();
        	coordinatorThread.join();
        	trace.debug(AggregateFetchMarkers.CHANNEL_AGGREGATE_FETCH_APP, "ChannelAggregate after coordinatorThread.join()");
            	
            trace.debug(AggregateFetchMarkers.CHANNEL_AGGREGATE_FETCH_APP, "QueryResController is finished");
            final long processingEnd = System.nanoTime();
            
            trace.debug(AggregateFetchMarkers.CHANNEL_AGGREGATE_FETCH_APP, "ResultSet Query took: " + (resultSetEnd-resultSetStart)/1000000.0 + " msecs");
            trace.debug(AggregateFetchMarkers.CHANNEL_AGGREGATE_FETCH_APP, "Data Processing took: " + (processingEnd-processingStart)/1000000.0 + " msecs");
        }
        catch (final DatabaseException e) {
        	/*
            if (DEBUG) {
                e.printStackTrace();
            }*/

            if (shutdown) {
                setExitCode(OTHER_ERROR);
            }
            else {
                trace.error(AggregateFetchMarkers.CHANNEL_AGGREGATE_FETCH_APP, "SQL problem encountered while retrieving records: " + e.getMessage());
                setExitCode(OTHER_ERROR);
            }
        }
        catch (final Exception e) {
        	/*
            if (DEBUG) {
                e.printStackTrace();
            }*/

            final String message = e.getMessage() == null ? "" : ": " + e.getMessage();
            e.printStackTrace(System.out);
            trace.error(AggregateFetchMarkers.CHANNEL_AGGREGATE_FETCH_APP, "Problem encountered while retrieving records" + message);
            setExitCode(OTHER_ERROR);
        } finally {
            working = false;
        }
    }

    @Override
    public void exitCleanly() {
        //  First close db fetch connection \
        super.exitCleanly();

        // Next wait for threads to stop before cleaning up tmp files
        // we want to ensure no new files are created during or after cleanup
        while (true) {
            if (!ehaAggregateQueryQoordinator.threadsAlive() && !working) {
                break;
            }
        }
        // chill_get_chanvals does not clean up temporary
        // files when process is interrupted through a CTRL+C
        ehaAggregateQueryQoordinator.cleanUpTempFiles();
    }
}
