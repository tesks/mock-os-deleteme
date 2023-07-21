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


import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.fetch.*;
import jpl.gds.db.api.sql.fetch.WhereControl.WhereControlException;
import jpl.gds.db.api.sql.store.ldi.*;
import jpl.gds.db.api.sql.store.ldi.aggregate.IChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IHeaderChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IMonitorChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.ISseChannelAggregateLDIStore;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.app.PacketFetchApp.PacketTypeSelect;
import jpl.gds.db.mysql.impl.sql.AbstractMySqlInteractor;
import jpl.gds.db.mysql.impl.sql.order.ChannelAggregateOrderByType;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.process.LineHandler;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.process.StderrLineHandler;
import jpl.gds.shared.time.*;
import jpl.gds.shared.types.Pair;
import org.apache.commons.cli.*;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


/**
 * The DataSummaryApp is the command line application used to
 * return the counts of frames, packets, channels, evrs, log messages,
 * and commands over a range of sessions and/or time.
 *
 * This "fetch" app deviates from the typical pairing of App and Fetch
 * classes, since we're returning counts and not actual records--hence
 * eliminating need for a Fetch class.
 *
 * Counts are in fact executed on the database server (using SQL
 * "COUNT" statement) to enhance performance.
 *
 * This tool is written based on the MPCS database v5.7 structure. Any
 * major future modifications to the database structure may require
 * some of the tool's code to change accordingly.
 *
 * Note that the result sets are closed at some remove from their creation,
 * hence the need for the suppresses.
 *
 */
public class DataSummaryApp extends ChannelValueFetchApp 
{
    private static final boolean USE_EXTENDED_SCET =
        TimeProperties.getInstance().useExtendedScet();

    /**
     * Application name given to this tool, as it'll appear after
     * build.
     */
    private static final String APP_NAME =
        ApplicationConfiguration.getApplicationName("chill_data_summary");

    /** Format specifier for the column header. */
    private static final String HEADER_FORMAT = "|%1$8s|%2$10s|%3$10s|%4$8s|%5$12s|%6$8s|%7$8s|%8$8s|\n";

    /** Columns format specifier for the typical session-split rows. */
    private static final String ROW_FORMAT = "|%1$8d|%2$10d|%3$10d|%4$8d|%5$12d|%6$8d|%7$8d|%8$8d|\n";

    /** Columns format specifier for the no-session, time-based count output. */
    private static final String ROW_WITHOUT_KEY_FORMAT = "|%1$8s|%2$10d|%3$10d|%4$8d|%5$12d|%6$8d|%7$8d|%8$8d|\n";

    private final IDbSessionInfoUpdater     dbSessionInfo;

    /**
     * List of command line options that this app must have (at
     * least one of them).
     */
    private List<String> requiredOptions;

    /** VCIDs to query for */
    private Set<Integer> vcids = null;

    /** DSS ids to query for */
    private Set<Integer> dssIds = null;

    /** Channel types desired */
    private final ChannelTypeSelect channelTypeSelect =
        new ChannelTypeSelect();

    /** EVR types desired */
    private final EvrFetchApp.EvrTypeSelect evrTypeSelect = new EvrFetchApp.EvrTypeSelect();

    /** Packet types desired */
    private final PacketTypeSelect packetTypeSelect =
        new PacketTypeSelect();

    /*
     * The following an alphabetized list of all the command line parameters
     * that are used by all the subclasses of this class.
     */
    private static final String OUTPUT_EMPTY_SESSIONS_SHORT = "i";
    private static final String OUTPUT_EMPTY_SESSIONS_LONG = "outputEmptySessions";
    private static final String SPLIT_SESSIONS_SHORT = "p";

    /** Long split-sessions option */
    public static final String SPLIT_SESSIONS_LONG = "splitSessions";

    private static final boolean checkRequiredOptions = true;

    /** Indicates whether to simply print the SQL statement rather than execute. */
    private boolean sqlStmtOnly;

    private boolean outputEmptySessions;
    private boolean splitSessions;

    /**
     * The contents of the query configuration file are stored in this object.
     */
    private final QueryConfig queryConfig;

    /**
     * The ERT time range to use when querying (also includes time
     * type).
     */
    private DatabaseTimeRange ertTimes;

    /**
     * The event time range to use when querying (also includes time
     * type). (Used for tables that don't have ERTs.)
     */
    private DatabaseTimeRange eventTimes;

    private String beginTimeString;
    private String endTimeString;


    /*
     * Keeps track of COUNT rows returned by the database server.
     * What's returned from the database is rows of "SELECT COUNT",
     * not actual rows of data. Each row represents a session.
     */
    private long frameRecordCount;
    /*
     * COUNT rows returned by the database server is saved in these
     * maps. Key = session ID, value = data count for that session.
     */
    private Map<Long, Long> frameCountTable;

    private long packetRecordCount = 0L;
    private final Map<Long, Long> packetCountTable = new HashMap<Long, Long>();

    private long ssePacketRecordCount = 0L;
    private final Map<Long, Long> ssePacketCountTable = new HashMap<Long, Long>();

    private long evrRecordCount = 0L;
    private final Map<Long, Long> evrCountTable = new HashMap<Long, Long>();

    private long sseEvrRecordCount = 0L;
    private final Map<Long, Long> sseEvrCountTable = new HashMap<Long, Long>();

    private long chanvalRecordCount = 0L;
    private final Map<Long, Long> chanvalCountTable = new HashMap<Long, Long>();

    private long monchanvalRecordCount = 0L;
    private final Map<Long, Long> monchanvalCountTable = new HashMap<Long, Long>();

    private long headerchanvalRecordCount = 0L;
    private final Map<Long, Long> headerchanvalCountTable = new HashMap<Long, Long>();

    private long ssechanvalRecordCount = 0L;
    private final Map<Long, Long> ssechanvalCountTable = new HashMap<Long, Long>();

    private long sseheaderchanvalRecordCount = 0L;
    private final Map<Long, Long> sseheaderchanvalCountTable = new HashMap<Long, Long>();

    private long commandRecordCount;

    private Map<Long, Long> commandCountTable;
    private long productRecordCount;
    private Map<Long, Long> productCountTable;
    private long logRecordCount;
    private Map<Long, Long> logCountTable;

    /**
     * Flag indicating user interrupt
     */
    private boolean shutdown;

    /**
     * Object that executes the count queries.
     */
    private CountQueriesExecutor cqe;

    private final IDbSqlFetchFactory fetchFactory;

    private static final String VELOCITY_TEMPLATE = "data_summary";
    private ChanvalLineHandler chanvalLineHandler;


    /**
     * Creates an instance of DataSummaryApp.
     */
    public DataSummaryApp() {
        super(APP_NAME, null);
        queryConfig = appContext.getBean(QueryConfig.class);
           
        final IDbSessionInfoFactory dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        dbSessionInfo = dbSessionInfoFactory.createQueryableUpdater();
        
        fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);

        outputEmptySessions = false; // Default is to truncate 0-count sessions
        splitSessions = false; // Default is to output time-based counts in only one row

        frameRecordCount = 0;
        packetRecordCount = 0;
        ssePacketRecordCount = 0;
        evrRecordCount = 0;
        sseEvrRecordCount = 0;
        chanvalRecordCount = 0;
        monchanvalRecordCount = 0;
        headerchanvalRecordCount = 0;
        ssechanvalRecordCount = 0;

        sseheaderchanvalRecordCount = 0;

        commandRecordCount = 0;
        productRecordCount = 0;
        logRecordCount = 0;
    }

    /**
     * Cleanly exits in the event of a control
     */
    @Override
    public void exitCleanly() {
        try {
            shutdown = true;
            if (cqe != null) {
                cqe.close();
            }
        } catch (final Exception e) {
            // don't care - can't do anything at this point anyway
        }

        super.exitCleanly();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        setExitCode(SUCCESS);

        cqe = new CountQueriesExecutor(appContext, sqlStmtOnly);

        if (!cqe.isConnected()) {
            setExitCode(OTHER_ERROR);
            return;
        }

        /* This tool outputs to std out. */
        final PrintWriter pw = new PrintWriter(System.out, false);

        try {

            /*
             * These methods do both fetching of the counts from
             * database and also outputting.
             */
            doFswAndSseSummary(cqe, pw);

            trace.debug("Retrieved " + frameRecordCount + " sessions' non-empty FRAME COUNT records.");
            trace.debug("Retrieved " + packetRecordCount + " sessions' non-empty PACKET COUNT records.");
            trace.debug("Retrieved " + ssePacketRecordCount + " sessions' non-empty SSE PACKET COUNT records.");
            trace.debug("Retrieved " + evrRecordCount + " sessions' non-empty EVR COUNT records.");
            trace.debug("Retrieved " + sseEvrRecordCount + " sessions' non-empty SSE EVR COUNT records.");

            trace.debug("Retrieved " + chanvalRecordCount          + " sessions' non-empty CHANVAL COUNT records.");
            trace.debug("Retrieved " + monchanvalRecordCount       + " sessions' non-empty MONCHANVAL COUNT records.");
            trace.debug("Retrieved " + headerchanvalRecordCount    + " sessions' non-empty HEADERCHANVAL COUNT records.");
            trace.debug("Retrieved " + ssechanvalRecordCount       + " sessions' non-empty SSECHANVAL COUNT records.");

            trace.debug("Retrieved " + sseheaderchanvalRecordCount + " sessions' non-empty SSEHEADERCHANVAL COUNT records.");

            trace.debug("Retrieved " + commandRecordCount + " sessions' non-empty COMMAND COUNT records.");
            trace.debug("Retrieved " + productRecordCount + " sessions' non-empty CHANVAL COUNT records.");
            trace.debug("Retrieved " + logRecordCount + " sessions' non-empty COMMAND COUNT records.");

        } catch (final DatabaseException e) {
            e.printStackTrace(System.out);
            if (!shutdown) {
                trace.error("Problem encountered while retrieving records: " + e.getMessage());
            }
            setExitCode(OTHER_ERROR);

        } catch (final Exception e) {
            e.printStackTrace(System.out);
            trace.error("Problem encountered while retrieving records: " + e.getMessage());
            setExitCode(OTHER_ERROR);

        } finally {
            cqe.close();
        }

    }

    private void doFswAndSseSummary(final CountQueriesExecutor cqe,
                                      final PrintWriter pw) throws DatabaseException, IOException {

        frameCountTable = new HashMap<Long, Long>(dbSessionInfo.getSessionKeyList().size());

        commandCountTable = new HashMap<Long, Long>(dbSessionInfo.getSessionKeyList().size());
        productCountTable = new HashMap<Long, Long>(dbSessionInfo.getSessionKeyList().size());
        logCountTable = new HashMap<Long, Long>(dbSessionInfo.getSessionKeyList().size());

        /*
         * Query for frame counts and populate internal table.
         */
        List<Pair<Long, Long>> out = cqe.getFrameCounts(dbSessionInfo, ertTimes,
                AbstractFetchApp.defaultBatchSize);

        while (out.size() != 0) {
            final ListIterator<Pair<Long, Long>> iter = out.listIterator();
            while (iter.hasNext() == true) {
                final Pair<Long, Long> ctPair = iter.next();
                frameCountTable.put(ctPair.getOne(), ctPair.getTwo());
                frameRecordCount++;
            }

            out = cqe.getNextFrameCountResultsBatch();
        }

        /*
         * Query for packet counts and populate internal table.
         */

        if (packetTypeSelect.fsw)
        {
            out = cqe.getPacketCounts(dbSessionInfo, ertTimes, AbstractFetchApp.defaultBatchSize);

            while (out.size() != 0) {
                final ListIterator<Pair<Long, Long>> iter = out.listIterator();
                while (iter.hasNext() == true) {
                    final Pair<Long, Long> ctPair = iter.next();
                    packetCountTable.put(ctPair.getOne(), ctPair.getTwo());
                    packetRecordCount++;
                }

                out = cqe.getNextPacketCountResultsBatch();
            }
        }

        /*
         * Query for EVR counts and populate internal table.
         */

        if (evrTypeSelect.fswRealtime || evrTypeSelect.fswRecorded)
        {
            out = cqe.getEvrCounts(dbSessionInfo, ertTimes, AbstractFetchApp.defaultBatchSize);

            while (out.size() != 0) {
                final ListIterator<Pair<Long, Long>> iter = out.listIterator();
                while (iter.hasNext() == true) {
                    final Pair<Long, Long> ctPair = iter.next();
                    evrCountTable.put(ctPair.getOne(), ctPair.getTwo());
                    evrRecordCount++;
                }

                out = cqe.getNextEvrCountResultsBatch();
            }
        }

        /*
         * Query for channel value counts from aggregates and populate internal table.
         */

        // chill_data_summary --beginTime --endTime EHA counts not
        // accurate under certain conditions
        // When the beginTime or endTime does not fall at exact aggregate boundaries, the channel
        // count information will not be correct. We essentially need to open up the aggregates.
        // Easiest and cleanest approach for doing this is to execute chill_get_chanvals and do
        // the count on the CSV output produced.
        //
        // Note that the existing code keeps a separate table for different channel types
        // which is only used for debugging. Running a separate chill_get_chanvals process
        // for each channel type would be very inefficient.
        if (ertTimes != null && (ertTimes.getStartTime() != null || ertTimes.getStopTime() != null)) {

            // build command line arguments
            final List<String> argList = constructChanvalsCommandLine();
            // run chill_get_chanvals
            executeChillGetChanvals(argList);
            // build the count tables
            for (final Map.Entry<Integer, Integer> entry : chanvalLineHandler.getChannelCountMap().entrySet()) {
                chanvalCountTable.put(Long.valueOf(entry.getKey()), Long.valueOf(entry.getValue()));
            }
        } else {
            if (channelTypeSelect.fswRealtime || channelTypeSelect.fswRecorded)
            {
                out = cqe.getChannelAggregateCounts(dbSessionInfo, ertTimes, AbstractFetchApp.defaultBatchSize);

                while (out.size() != 0) {
                    final ListIterator<Pair<Long, Long>> iter = out.listIterator();
                    while (iter.hasNext() == true) {
                        final Pair<Long, Long> ctPair = iter.next();
                        chanvalCountTable.put(ctPair.getOne(), ctPair.getTwo());
                        chanvalRecordCount++;
                    }

                    out = cqe.getNextChannelAggregateCountResultsBatch();
                }
            }

            /*
             * Query for monitor channel value counts from aggregates and populate internal table.
             */

            if (channelTypeSelect.monitor)
            {
                out = cqe.getMonitorChannelAggregateCounts(dbSessionInfo, ertTimes, AbstractFetchApp.defaultBatchSize);

                while (out.size() != 0) {
                    final ListIterator<Pair<Long, Long>> iter = out.listIterator();
                    while (iter.hasNext() == true) {
                        final Pair<Long, Long> ctPair = iter.next();
                        monchanvalCountTable.put(ctPair.getOne(), ctPair.getTwo());
                        monchanvalRecordCount++;
                    }

                    out = cqe.getNextMonitorChannelAggregateCountResultsBatch();
                }
            }

            /*
             * Query for header channel value counts from aggregates and populate internal table.
             */

            if (channelTypeSelect.header)
            {
                out = cqe.getHeaderChannelAggregateCounts(dbSessionInfo, ertTimes, AbstractFetchApp.defaultBatchSize);

                while (out.size() != 0) {
                    final ListIterator<Pair<Long, Long>> iter = out.listIterator();
                    while (iter.hasNext() == true) {
                        final Pair<Long, Long> ctPair = iter.next();
                        headerchanvalCountTable.put(ctPair.getOne(), ctPair.getTwo());
                        headerchanvalRecordCount++;
                    }

                    out = cqe.getNextHeaderChannelAggregateCountResultsBatch();
                }
            }

            /*
             * Query for SSE channel value counts from aggregates and populate internal table.
             */

            if (channelTypeSelect.sse)
            {
                out = cqe.getSseChannelAggregateCounts(dbSessionInfo, ertTimes, AbstractFetchApp.defaultBatchSize);

                while (out.size() != 0) {
                    final ListIterator<Pair<Long, Long>> iter = out.listIterator();
                    while (iter.hasNext() == true) {
                        final Pair<Long, Long> ctPair = iter.next();
                        ssechanvalCountTable.put(ctPair.getOne(), ctPair.getTwo());
                        ssechanvalRecordCount++;
                    }

                    out = cqe.getNextSseChannelAggregateCountResultsBatch();
                }
            }

            /*
             * Query for SSE header channel value counts from aggregates and populate internal table.
             */

            if (channelTypeSelect.sseHeader)
            {
                out = cqe.getSseHeaderChannelAggregateCounts(dbSessionInfo, ertTimes, AbstractFetchApp.defaultBatchSize);

                while (out.size() != 0) {
                    final ListIterator<Pair<Long, Long>> iter = out.listIterator();
                    while (iter.hasNext() == true) {
                        final Pair<Long, Long> ctPair = iter.next();
                        sseheaderchanvalCountTable.put(ctPair.getOne(), ctPair.getTwo());
                        sseheaderchanvalRecordCount++;
                    }

                    out = cqe.getNextSseHeaderChannelAggregateCountResultsBatch();
                }
            }
        }



        /*
         * Query for command message counts and populate internal table.
         */
        out = cqe.getCommandCounts(dbSessionInfo, eventTimes, AbstractFetchApp.defaultBatchSize);

        while (out.size() != 0) {
            final ListIterator<Pair<Long, Long>> iter = out.listIterator();
            while (iter.hasNext() == true) {
                final Pair<Long, Long> ctPair = iter.next();
                commandCountTable.put(ctPair.getOne(), ctPair.getTwo());
                commandRecordCount++;
            }

            out = cqe.getNextCommandCountResultsBatch();
        }

        /*
         * Query for product counts and populate internal table.
         */
        out = cqe.getProductCounts(dbSessionInfo, ertTimes, AbstractFetchApp.defaultBatchSize);

        while (out.size() != 0) {
            final ListIterator<Pair<Long, Long>> iter = out.listIterator();
            while (iter.hasNext() == true) {
                final Pair<Long, Long> ctPair = iter.next();
                productCountTable.put(ctPair.getOne(), ctPair.getTwo());
                productRecordCount++;
            }

            out = cqe.getNextProductCountResultsBatch();
        }

        /*
         * Query for log message counts and populate internal table.
         */
        out = cqe.getLogCounts(dbSessionInfo, eventTimes, AbstractFetchApp.defaultBatchSize);

        while (out.size() != 0) {
            final ListIterator<Pair<Long, Long>> iter = out.listIterator();
            while (iter.hasNext() == true) {
                final Pair<Long, Long> ctPair = iter.next();
                logCountTable.put(ctPair.getOne(), ctPair.getTwo());
                logRecordCount++;
            }

            out = cqe.getNextLogCountResultsBatch();
        }

        /*
         * Query for SSE packet counts and populate internal table.
         */

        if (packetTypeSelect.sse)
        {
            out = cqe.getSsePacketCounts(dbSessionInfo, ertTimes, AbstractFetchApp.defaultBatchSize);

            while (out.size() != 0) {
                final ListIterator<Pair<Long, Long>> iter = out.listIterator();
                while (iter.hasNext() == true) {
                    final Pair<Long, Long> ctPair = iter.next();
                    ssePacketCountTable.put(ctPair.getOne(), ctPair.getTwo());
                    ssePacketRecordCount++;
                }

                out = cqe.getNextSsePacketCountResultsBatch();
            }
        }

        /*
         * Query for SSE EVR counts and populate internal table.
         */
        if (evrTypeSelect.sse)
        {
            out = cqe.getSseEvrCounts(dbSessionInfo, ertTimes, AbstractFetchApp.defaultBatchSize);

            while (out.size() != 0) {
                final ListIterator<Pair<Long, Long>> iter = out.listIterator();
                while (iter.hasNext() == true) {
                    final Pair<Long, Long> ctPair = iter.next();
                    sseEvrCountTable.put(ctPair.getOne(), ctPair.getTwo());
                    sseEvrRecordCount++;
                }

                out = cqe.getNextSseEvrCountResultsBatch();
            }
        }

        /*
         * Don't print header if running in SQL-statement-only mode.
         */
        if (!sqlStmtOnly) {
            pw.format(HEADER_FORMAT,
                      "Id",
                      "Frame",
                      "Packet",
                      "EVR",
                      "EHA",
                      "CMD",
                      "PROD",
                      "LOG");
            pw.flush();
        }

        /*
         * If this is a time-based count query, see if we need to
         * split the sessions into different rows or not.
         */
        if (ertTimes == null || (ertTimes != null && splitSessions)) {

            /*
             * Print out the count summary(or -ies).
             */
            for (final long key : getSessionKeysToOutput()) {
                final Long val1 = frameCountTable.get(key);
                final Long val2 = packetCountTable.get(key);
                final Long val2sse = ssePacketCountTable.get(key);
                final Long val3 = evrCountTable.get(key);
                final Long val3sse = sseEvrCountTable.get(key);

                final Long val4          = chanvalCountTable.get(key);
                final Long val4mon       = monchanvalCountTable.get(key);
                final Long val4header    = headerchanvalCountTable.get(key);
                final Long val4sse       = ssechanvalCountTable.get(key);

                final Long val4sseheader = sseheaderchanvalCountTable.get(key);

                final Long val5 = commandCountTable.get(key);
                final Long val6 = productCountTable.get(key);
                final Long val7 = logCountTable.get(key);
                pw.format(ROW_FORMAT,
                        key,
                        (val1 == null ? 0 : val1),
                        (val2 == null ? 0 : val2) + (val2sse == null ? 0 : val2sse), // Add FSW and SSE packet counts.
                        (val3 == null ? 0 : val3) + (val3sse == null ? 0 : val3sse), // Add FSW and SSE EVR counts.
                        (val4 == null ? 0 : val4)                    +
                            (val4mon       == null ? 0 : val4mon)    +   // Add M-channel counts if any.
                            (val4header    == null ? 0 : val4header) +   // Add H-channel counts if any.
                            (val4sse       == null ? 0 : val4sse)    +   // Add SSE-channel counts if any
                            (val4sseheader == null ? 0 : val4sseheader), // Add SSE-header-channel counts if any.
                        (val5 == null ? 0 : val5),
                        (val6 == null ? 0 : val6),
                        (val7 == null ? 0 : val7));
                pw.flush();
            }

        } else {
            /*
             * Since this is a time-based query and we need to output
             * only one row summarizing all the counts, sum up the
             * counts from different sessions.
             */

            long frameTotal = 0, packetTotal = 0, evrTotal = 0, chanvalTotal = 0,
                 commandTotal = 0, productTotal = 0, logTotal = 0;

            for (final long val : frameCountTable.values()) {
                frameTotal += val;
            }

            for (final long val : packetCountTable.values()) {
                packetTotal += val;
            }

            for (final long val : ssePacketCountTable.values()) {
                packetTotal += val;
            }

            for (final long val : evrCountTable.values()) {
                evrTotal += val;
            }

            for (final long val : sseEvrCountTable.values()) {
                evrTotal += val;
            }

            for (final long val : chanvalCountTable.values()) {
                chanvalTotal += val;
            }

            for (final long val : monchanvalCountTable.values()) {
                chanvalTotal += val;
            }

            for (final long val : headerchanvalCountTable.values()) {
                chanvalTotal += val;
            }

            for (final long val : ssechanvalCountTable.values()) {
                chanvalTotal += val;
            }

            for (final long val : sseheaderchanvalCountTable.values()) {
                chanvalTotal += val;
            }

            for (final long val : commandCountTable.values()) {
                commandTotal += val;
            }

            for (final long val : productCountTable.values()) {
                productTotal += val;
            }

            for (final long val : logCountTable.values()) {
                logTotal += val;
            }

            pw.format(ROW_WITHOUT_KEY_FORMAT,
                    "--------",
                    frameTotal,
                    packetTotal,
                    evrTotal,
                    chanvalTotal,
                    commandTotal,
                    productTotal,
                    logTotal);
            pw.flush();
        }

    }

    private void executeChillGetChanvals(final List<String> argList) throws IOException {

        chanvalLineHandler = new ChanvalLineHandler();

        // Set up the process launcher for chill_get_chanvals command
        final ProcessLauncher processLauncher = new ProcessLauncher();
        processLauncher.setOutputHandler(chanvalLineHandler);
        processLauncher.setErrorHandler(new StderrLineHandler());

        // Launch the chill_get_chanvals process
        if (!processLauncher.launch(
                argList.toArray(new String[argList.size()]),
                GdsSystemProperties.getSystemProperty(GdsSystemProperties.JAVA_IO_TEMPDIR))) {
            trace.error("Unable to launch command: " + String.join(" ", argList));
        }

        trace.debug("Launched command: " + String.join(" ", argList));

        // Wait for the process to complete
        processLauncher.waitForExit();

    }

    // package private for testing
    List<String> constructChanvalsCommandLine() {
        final List<String> argList = new ArrayList<>();

        final String scriptName =
                GdsSystemProperties.getSystemProperty(
                        GdsSystemProperties.DIRECTORY_PROPERTY) +
                        File.separator +
                        "bin/chill_get_chanvals";
        argList.add(scriptName);

        // We want to count all channel types
        argList.add("--" + CHANNEL_TYPES_LONG);
        final StringBuilder channelTypesString = new StringBuilder();
        if (channelTypeSelect.fswRealtime) {
            channelTypesString.append("f");
        }
        if (channelTypeSelect.fswRecorded) {
            channelTypesString.append("r");
        }
        if (channelTypeSelect.header) {
            channelTypesString.append("h");
        }
        if (channelTypeSelect.monitor) {
            channelTypesString.append("m");
        }
        if (channelTypeSelect.sse) {
            channelTypesString.append("s");
        }
        if (channelTypeSelect.sseHeader) {
            channelTypesString.append("g");
        }
        argList.add(channelTypesString.toString());

        final IAccurateDateTime startERT = ertTimes.getStartTime();
        if (startERT != null) {
            argList.add("--" + BEGIN_TIME_LONG);
            argList.add(startERT.getFormattedErtFast(true));
        }

        final IAccurateDateTime stopERT = ertTimes.getStopTime();
        if (stopERT != null) {
            argList.add("--" + END_TIME_LONG);
            argList.add(stopERT.getFormattedErtFast(true));
        }

        argList.add("--"+ OUTPUT_FORMAT_LONG);
        argList.add(VELOCITY_TEMPLATE);

        // Data time sort order is irrelevant, so use NONE for
        // efficient processing by chill_get_chanvals
        argList.add("--" + ORDER_BY_LONG);
        argList.add(ChannelAggregateOrderByType.NONE.toString());

        return argList;
    }

    /**
     * Determine list of session keys in this count query. This is
     * either determined from the "testKey" argument provided by the
     * user or (if time-based query) by gathering up the set of keys
     * returned by the database itself.
     *
     * @return list of session keys supplied or available in this query
     */
    private List<Long> getSessionKeysToOutput() {

        if (outputEmptySessions && dbSessionInfo.getSessionKeyList().size() > 0) {
            return dbSessionInfo.getSessionKeyList();
        }

        // union all the data keys
        final Set<Long> newKeysSet = new TreeSet<Long>();

        newKeysSet.addAll(frameCountTable.keySet());

        newKeysSet.addAll(packetCountTable.keySet());

        newKeysSet.addAll(ssePacketCountTable.keySet());

        newKeysSet.addAll(evrCountTable.keySet());

        newKeysSet.addAll(sseEvrCountTable.keySet());

        newKeysSet.addAll(chanvalCountTable.keySet());

        newKeysSet.addAll(monchanvalCountTable.keySet());

        newKeysSet.addAll(headerchanvalCountTable.keySet());

        newKeysSet.addAll(ssechanvalCountTable.keySet());

        newKeysSet.addAll(sseheaderchanvalCountTable.keySet());

        newKeysSet.addAll(commandCountTable.keySet());
        newKeysSet.addAll(productCountTable.keySet());
        newKeysSet.addAll(logCountTable.keySet());

        final List<Long> newKeysList = new ArrayList<Long>(newKeysSet); // already sorted
        return newKeysList;
    }

    /**
     * Checks if the command line has the required options for this app.
     *
     * @param cmdline Command-line
     *
     * @throws MissingArgumentException Missing argument
     * @throws ParseException           Parse error
     */
    @Override
    public void requiredOptionsCheck(final CommandLine cmdline)
        throws MissingArgumentException, ParseException
    {
        createRequiredOptions();

        final Option[] options = cmdline.getOptions();
        boolean hasRequiredOption = false;
        for (final Option op : options) {
            if (requiredOptions.contains(op.hasLongOpt() ? op.getLongOpt() : op.getOpt())) {
                hasRequiredOption = true;
                break;
            }
        }

        if (!hasRequiredOption) {
            throw new MissingArgumentException("You have provided no search options to qualify your query.  Please provide" +
                    " at least one search option.");
        }
    }

    /**
     * Populates the requiredOptions member variable with the required
     * option name strings.  Adds the long option name by default unless
     * there is none, then it adds the short option name.
     *
     * @throws ParseException Parse error
     */
    @Override
    public void createRequiredOptions() throws ParseException
    {
        requiredOptions = new ArrayList<String>();
        requiredOptions.add(ReservedOptions.TESTKEY_LONG_VALUE);
        requiredOptions.add(AbstractFetchApp.BEGIN_TIME_LONG);
        requiredOptions.add(AbstractFetchApp.END_TIME_LONG);

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException {

        if(cmdline == null)
        {
            throw new IllegalArgumentException("Null input command line");
        }


        if (checkRequiredOptions)
        {
            requiredOptionsCheck(cmdline);
        }

        //read in the unique test key
        if (cmdline.hasOption(ReservedOptions.TESTKEY_SHORT_VALUE))
        {
            final String testKeyStr = cmdline.getOptionValue(ReservedOptions.TESTKEY_SHORT_VALUE);
            if (testKeyStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.TESTKEY_SHORT_VALUE + " requires a " +
                "comma-separated list or range of numeric test keys as an argument");
            }

            final String[] testKeys = testKeyStr.split(",");

            for(final String testKey: testKeys){
                final String[] testKeyStrings = testKey.trim().split("\\.\\.");
                if (testKeyStrings.length == 2)
                {
                    try
                    {
                        final long a = Long.valueOf(testKeyStrings[0].trim());
                        final long b = Long.valueOf(testKeyStrings[1].trim());
                        if (a > b)
                        {
                            throw new ParseException("Value of -" + ReservedOptions.TESTKEY_SHORT_VALUE + " option must be a " +
                                    "range of integer values, delimited by \"..\", but \"" + testKeyStr + "\" has starting range greater than ending.");
                        }
                        dbSessionInfo.addSessionKeyRange(a,b);
                    }
                    catch (final NumberFormatException e1)
                    {
                        throw new ParseException("Value of -" + ReservedOptions.TESTKEY_SHORT_VALUE + " option must be a " +
                                "range of integer values, delimited by \"..\", but \"" + testKeyStr + "\" is invalid.");
                    }
                }
                else
                {
                    try
                    {
                        dbSessionInfo.addSessionKey(Long.valueOf(testKey));
                    }
                    catch (final NumberFormatException e1)
                    {
                        throw new ParseException("Value of -" + ReservedOptions.TESTKEY_SHORT_VALUE + " option must be a " +
                                "list of comma-separated integer values, but the value \"" + testKey + "\" is invalid.");
                    }
                }
            }
        }

        //read in the test host
        if(cmdline.hasOption(ReservedOptions.TESTHOST_SHORT_VALUE))
        {
            final String hostStr = cmdline.getOptionValue(ReservedOptions.TESTHOST_SHORT_VALUE);
            if (hostStr == null)
            {
                throw new MissingArgumentException("-" + ReservedOptions.TESTHOST_SHORT_VALUE + " requires a " +
                "comma-separated list of hostname patterns as an argument");
            }

            final String[] hostStrings = hostStr.split(",");
            for(int i=0; i < hostStrings.length; i++)
            {
                dbSessionInfo.addHostPattern(hostStrings[i].trim());
            }
        }

        ReservedOptions.parseDatabaseHost(cmdline,false);
        ReservedOptions.parseDatabasePort(cmdline,false);
        ReservedOptions.parseDatabaseUsername(cmdline,false);
        ReservedOptions.parseDatabasePassword(cmdline,false);

        sqlStmtOnly = cmdline.hasOption(AbstractFetchApp.SQL_STATEMENT_ONLY_LONG);

        if (cmdline.hasOption(AbstractFetchApp.BEGIN_TIME_SHORT)) {
            beginTimeString = cmdline.getOptionValue(AbstractFetchApp.BEGIN_TIME_SHORT);

            if (beginTimeString == null) {
                throw new MissingOptionException("The option -" + AbstractFetchApp.BEGIN_TIME_SHORT + " requires a value");
            }

            beginTimeString = beginTimeString.trim();
        }

        if (cmdline.hasOption(AbstractFetchApp.END_TIME_SHORT)) {
            endTimeString = cmdline.getOptionValue(AbstractFetchApp.END_TIME_SHORT);

            if (endTimeString == null) {
                throw new MissingOptionException("The option -" + AbstractFetchApp.END_TIME_SHORT + " requires a value");
            }

            endTimeString = endTimeString.trim();
        }

        if (beginTimeString != null || endTimeString != null) {
            ertTimes = new DatabaseTimeRange(DatabaseTimeType.ERT);
            eventTimes = new DatabaseTimeRange(DatabaseTimeType.EVENT_TIME);

            try {

                if (beginTimeString != null) {
                    ertTimes.setStartTime(new AccurateDateTime(beginTimeString));
                    eventTimes.setStartTime(new AccurateDateTime(beginTimeString));
                }

            } catch(final java.text.ParseException e1) {
                throw new ParseException("Begin time value has an invalid format. Should be YYYY-MM-DDThh:mm:ss.ttt or YYYY-DOYThh:mm:ss.ttt");
            }

            try {

                if(endTimeString != null) {
                    ertTimes.setStopTime(new AccurateDateTime(endTimeString));
                    eventTimes.setStopTime(new AccurateDateTime(endTimeString));
                }

            } catch(final java.text.ParseException e1) {
                throw new ParseException("End time value has an invalid format. Should be YYYY-MM-DDThh:mm:ss.ttt or YYYY-DOYThh:mm:ss.ttt");
            }

        }

        outputEmptySessions = cmdline.hasOption(OUTPUT_EMPTY_SESSIONS_SHORT);
        splitSessions = cmdline.hasOption(SPLIT_SESSIONS_SHORT);

        // Process channel type selection arguments
        super.getChannelTypes(cmdline, channelTypeSelect, null);

        vcids  = DssVcidOptions.parseVcid(missionProps, 
        		                          cmdline,
                                          channelTypeSelect.monitor ? 'M' : null,
                                          channelTypeSelect.sse     ? 'S' : null,
                                          ChannelValueFetchApp.CHANNEL_TYPES_LONG);

        dssIds = DssVcidOptions.parseDssId(cmdline,
                                           channelTypeSelect.sse ? 'S' : null,
                                           ChannelValueFetchApp.CHANNEL_TYPES_LONG);

        // Process EVR type selection arguments
        EvrFetchApp.getEvrTypes(dbProperties, cmdline, evrTypeSelect);

        vcids  = DssVcidOptions.parseVcid(missionProps, cmdline,
                                          evrTypeSelect.sse ? 'S' : null,
                                          null,
                                          EvrFetchApp.EVR_TYPES_LONG);

        dssIds = DssVcidOptions.parseDssId(cmdline,
                                           evrTypeSelect.sse ? 'S' : null,
                                           EvrFetchApp.EVR_TYPES_LONG);

        // Process packet type selection arguments
        PacketFetchApp.getPacketTypes(dbProperties, cmdline, packetTypeSelect);

        vcids  = DssVcidOptions.parseVcid(missionProps, cmdline,
                                          packetTypeSelect.sse ? 'S' : null,
                                          null,
                                          PacketFetchApp.PACKET_TYPES_LONG);

        dssIds = DssVcidOptions.parseDssId(cmdline,
                                           packetTypeSelect.sse ? 'S' : null,
                                           PacketFetchApp.PACKET_TYPES_LONG);
     }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsage() {
        return APP_NAME + " [--" + ReservedOptions.TESTKEY_LONG_VALUE + " <session_id(s)>]\n" +
            "                   [--" + AbstractFetchApp.BEGIN_TIME_LONG + " <time>]\n" +
            "                   [--" + AbstractFetchApp.END_TIME_LONG + " <time>] [OPTIONS]\n";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void showHelp() {
        System.out.println(getUsage());
        final PrintWriter pw = new PrintWriter(System.out);
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printOptions(pw, 80, options, 7, 2);
        pw.flush();
        System.out.println("\nMultiple query parameters will be ANDed together. Query values");
        System.out.println("are NOT case sensitive. Time values should use the format");
        System.out.println("YYYY-MM-DDThh:mm:ss.ttt or  YYYY-DOYThh:mm:ss.ttt. Timezone for");
        System.out.println("all times is GMT. All string parameters whose long option name");
        System.out.println("contains the word \"Pattern\" may be entered using SQL pattern");
        System.out.println("matching syntax such as -" + ReservedOptions.TESTHOST_SHORT_VALUE + " %MyTestHost%, which would find all ");
        System.out.println("sessions with names that contain the string \"MyTestHost\"\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAppOptions() {
        AbstractFetchApp.createHostPortUserPwdOptions(options);

        //some of the reserved options are slightly edited in this section to make their long argument names contain
        //the word "Pattern" to indicate that they can use SQL pattern matching syntax because they are queried using
        //the keyword LIKE instead of a simple = (strangely enough we can edit the long option name, but there's no
        //mutator for the description)
        final String patternString = "Pattern";
        final String desc1String = ". Individual entries may be specified using an SQL LIKE pattern with wildcards like % and _. ";
        final String desc2String = " Multiple values may be supplied in a comma-separated value (CSV) format.";
        final String descString = desc1String + desc2String;

        final Option hostOpt = ReservedOptions.getOption(ReservedOptions.TESTHOST_SHORT_VALUE);
        Option opt = new Option(hostOpt.getOpt(),hostOpt.getLongOpt() + patternString, hostOpt.hasArg(), hostOpt.getDescription() + descString);
        opt.setArgName(ReservedOptions.TESTHOST_ARGNAME);
        options.addOption(opt);

        final Option keyOpt = ReservedOptions.getOption(ReservedOptions.TESTKEY_SHORT_VALUE);
        opt = new Option(keyOpt.getOpt(),keyOpt.getLongOpt(), keyOpt.hasArg(), keyOpt.getDescription() + desc2String + " (A range separated by \"..\" also accepted.)");
        opt.setArgName(ReservedOptions.TESTKEY_ARGNAME);
        options.addOption(opt);

        final Option sqlStmtOpt = ReservedOptions.createOption(AbstractFetchApp.SQL_STATEMENT_ONLY_LONG, null, "Instead of executing the database query, print the SQL statement (useful for debugging)");
        options.addOption(sqlStmtOpt);

        final Option beginTimeOpt = ReservedOptions.createOption(AbstractFetchApp.BEGIN_TIME_SHORT,AbstractFetchApp.BEGIN_TIME_LONG, "time", "Begin time of range");
        options.addOption(beginTimeOpt);

        final Option endTimeOpt = ReservedOptions.createOption(AbstractFetchApp.END_TIME_SHORT,AbstractFetchApp.END_TIME_LONG, "time", "End time of range");
        options.addOption(endTimeOpt);

        final Option outputEmptySessionsOpt = ReservedOptions.createOption(OUTPUT_EMPTY_SESSIONS_SHORT, OUTPUT_EMPTY_SESSIONS_LONG, null, "Include sessions with no data in the output");
        options.addOption(outputEmptySessionsOpt);

        final Option splitSessionsOpt = ReservedOptions.createOption(SPLIT_SESSIONS_SHORT, SPLIT_SESSIONS_LONG, null, "Split time-based summary into different sessions");
        options.addOption(splitSessionsOpt);

        DssVcidOptions.addVcidOption(options);
        DssVcidOptions.addDssIdOption(options);

		addOption(null,
                  ChannelValueFetchApp.CHANNEL_TYPES_LONG,
                  "string",
                  ChannelTypeSelect.RETRIEVE);

		addOption(null,
                  EvrFetchApp.EVR_TYPES_LONG,
                  "string",
                  "Retrieve selected types: " +
                      "s=SSE "                +
                      "f=FSW-realtime "       +
                      "r=FSW-recorded");

		addOption(null,
                  PacketFetchApp.PACKET_TYPES_LONG,
                  "string",
                  "Retrieve selected types: " +
                      "s=SSE "                +
                      "f=FSW");
    }


    /**
     * The main application entry point.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        final DataSummaryApp app = new DataSummaryApp();

        try {
            final int status = app.runAsApp(args);
            System.exit(status);
        } catch (final Exception e) {
            e.printStackTrace();
            TraceManager.getDefaultTracer().error("Unexpected error: ", ExceptionTools.getMessage(e));

        }

        System.exit(app.getExitCode());

    }

    /**
     * This class is used to parse the CSV records output by the
     * chill_get_chanvals tool. Output produced by chill_get_chanvals
     * is parsed to compute channel count information.
     */
    public static final class ChanvalLineHandler implements LineHandler {

        /**
         * Map that holds the channel sample count per session
         */
        private Map<Integer, Integer> channelCountMap = new TreeMap<>();

        /**
         * Variables used for input parsing
         */
        private String[] recArray;
        private Integer sessionId;
        private String channelId;
        private String channelName;

        @Override
        public void handleLine(final String line) {

            // Parse chill_get_chanvals output line formatted with the use of the 'data_summary' template.
            // Expected format is 'SessionId,ChannelId,ChannelName'
            try {
                recArray = line.split(",");
                sessionId = Integer.valueOf(recArray[0]);
                channelId = recArray[1];
                channelName = recArray[2];
            } catch (Exception e) {
                trace.warn("Unable to parse the following line, will be skipped: " + line);
                return;
            }

            if (channelCountMap.containsKey(sessionId)) {
                Integer count = channelCountMap.get(sessionId);
                count++;
                channelCountMap.put(sessionId, count);
            } else {
                channelCountMap.put(sessionId, 1);
            }
        }

        public Map<Integer, Integer> getChannelCountMap() {
            return channelCountMap;
        }
    }



    /**
     * CountQueriesExecutor builds counts fetch SQL statements and
     * executes them. It serves the purpose of typical "Fetch" classes
     * without limiting the fetch to a single database table and
     * actual row selects.
     *
     * General usage of this class:
     * <ol>
     * <li>For the desired count values (e.g. sessions' frames),
     * call the get[Type]Counts method to retrieve the initial batch
     * of results, where [Type] is the type of data to count (e.g.
     * Frame).</li>
     * <li>If the result returned by the initial call above wasn't
     * empty, there may be more results remaining to be retrieved, so
     * iteratively call getNext[Type]CountResultsBatch until results
     * returned is empty.</li>
     * </ol>
     */
    private class CountQueriesExecutor extends AbstractMySqlInteractor
    {

        public static final int NUM_OF_TABLES = 10;

        /**
         * The statements used to build and execute fetch queries.
         * Each database table has one statement. This map is keyed
         * on the database table names.
         */
        protected Map<String, PreparedStatement> stmtsMap;

        /**
         * The flag that indicates whether the SQL statement should
         * simply be printed out or executed.
         */
        protected boolean printStmtOnly;

        /**
         * The sets of results obtained by executing queries. Each
         * database table has its own set of results. This map is
         * keyed on the database table names.
         */
        protected Map<String, ResultSet> resultsMap;

        /**
         * The sizes of the batches of results that will be returned
         * to the user. Each database table has its own value. This
         * map is keyed on the database table names.
         */
        protected Map<String, Integer> batchSizeMap;

        /**
         * Creates an instance of AbstractCountQueryExecutor.
         * 
         * @param appContext
         *            Spring Application Context
         * @param printSqlStmt
         *            The flag that indicates whether the fetch class should
         *            print out the SQL statement only or execute it.
         */
        public CountQueriesExecutor(final ApplicationContext appContext,
                                    final boolean             printSqlStmt)
        {
            super (appContext, true, true); // Need a prepared statement

            printStmtOnly = printSqlStmt;
            stmtsMap = new HashMap<String, PreparedStatement>(NUM_OF_TABLES);
            resultsMap = new HashMap<String, ResultSet>(NUM_OF_TABLES);
            batchSizeMap = new HashMap<String, Integer>(NUM_OF_TABLES);
            // Initialize all batch sizes to 1
            batchSizeMap.put(IDbTableNames.DB_FRAME_DATA_TABLE_NAME, 1);
            batchSizeMap.put(IDbTableNames.DB_PACKET_DATA_TABLE_NAME, 1);
            batchSizeMap.put(IDbTableNames.DB_SSE_PACKET_DATA_TABLE_NAME, 1);
            batchSizeMap.put(IDbTableNames.DB_EVR_DATA_TABLE_NAME, 1);
            batchSizeMap.put(IDbTableNames.DB_SSE_EVR_DATA_TABLE_NAME, 1);
            batchSizeMap.put(IDbTableNames.DB_CHANNEL_VALUE_TABLE_NAME, 1);
            batchSizeMap.put(IDbTableNames.DB_MONITOR_CHANNEL_VALUE_TABLE_NAME, 1);
            batchSizeMap.put(IDbTableNames.DB_COMMAND_MESSAGE_DATA_TABLE_NAME, 1);
            batchSizeMap.put(IDbTableNames.DB_PRODUCT_DATA_TABLE_NAME, 1);
            batchSizeMap.put(IDbTableNames.DB_LOG_MESSAGE_DATA_TABLE_NAME, 1);
        }

        /**
         * Close the ResultSet object and remove from results map for
         * the specified database table.
         *
         * @param dbTableName Full name of the database table to free
         *                       the results for.
         */
        protected void closeResults(final String dbTableName) {
            final ResultSet results = resultsMap.get(dbTableName);

            if(results != null)
            {
                try
                {
                    results.close();
                }
                catch(final SQLException ignore)
                {

                }

            }

            resultsMap.remove(dbTableName);

        }

        /**
         * Close the PreparedStatement object and remove from
         * statements map for the specified database table.
         *
         * @param dbTableName Full name of the database table to free
         *                       the statement for.
         */
        protected void closeStatement(final String dbTableName) {
            final PreparedStatement statement = stmtsMap.get(dbTableName);

            if(statement != null)
            {
                try
                {
                    statement.cancel();
                    statement.close();
                }
                catch(final SQLException ignore)
                {

                }

            }

            stmtsMap.remove(dbTableName);

        }

        /**
         * Convenience method to close both the results and statement
         * for the specified database table, and to initialize the
         * batch size back to 1.
         *
         * @param dbTableName Full name of the database table to close
         *                       the resources for.
         */
        protected void closeResource(final String dbTableName) {
            closeResults(dbTableName);
            closeStatement(dbTableName);
            batchSizeMap.put(dbTableName, 1);
        }


        /**
         * Close Resources
         */
        @Override
        public void close() {
            for (final Map.Entry<String,Integer> entry : batchSizeMap.entrySet()) {
                closeResource(entry.getKey());
            }
            super.close();
        }

        private boolean useDatabase() {
            return dbProperties.getUseDatabase();
        }

        private void initQuery(final String dbTableName, final int batchSize) {
            final ResultSet results = resultsMap.get(dbTableName);

            //make sure the result set is closed
            if(results != null)
            {
                try
                {
                    results.close();
                }
                catch(final SQLException sqle)
                {
                    //don't care
                }
            }

            resultsMap.remove(dbTableName);

            if (!isConnected())
            {
                throw new IllegalStateException(
                "This connection has already been closed");
            }

            batchSizeMap.put(dbTableName, batchSize);
        }


        /**
         * Build the SQL statement, execute it, and return the results
         * for frame counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextFrameCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getFrameCounts(final IDbSessionInfoProvider tsi,
                                                     DatabaseTimeRange range,
                                                     final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.ERT);
            }

            initQuery(IFrameLDIStore.DB_FRAME_DATA_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute
            if (printStmtOnly)
            {
                // Dummy to get SQL statement printed

                final IDbSessionPreFetch dummy = fetchFactory.getSessionPreFetch(true, PreFetchType.NORMAL);
                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query
			final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false, PreFetchType.NORMAL);
            try {
	            spf.get(tsi);
	
	            final FrameWhereControl fwc = new FrameWhereControl("");
                final String frameTableAbbrev = queryConfig.getTablePrefix(IFrameLDIStore.DB_FRAME_DATA_TABLE_NAME);
	            final String canned = spf.getIdHostWhereClause(frameTableAbbrev);
	
	            // Force "where control" to take it as is
	
	            fwc.setCanned(canned);
	
	            String whereClause = null;
	
	            try {
	                fwc.addQueryForErtCoarseFine(range);
	                whereClause = fwc.generateWhereClause(frameTableAbbrev,
	                                                      ISessionFetch.DB_SESSION_TABLE_ABBREV);
	            } catch (final WhereControlException e) {
	                throw new DatabaseException("Error generating frame query WHERE clause: " + e.getMessage());
	            }
	
	            if (whereClause == null) {
	                throw new DatabaseException("FrameWhereControl generated null SQL WHERE clause");
	            }
	
                whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                            IDbSqlFetch.generateDssIdWhere(dssIds,
                                                                               frameTableAbbrev));
	
                whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                            IDbSqlFetch.generateVcidWhere(vcids,
	                                                                                   frameTableAbbrev,
	                                                                                   false));
	
            final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                                                            IFrameLDIStore.DB_FRAME_DATA_TABLE_NAME);
	
                closeStatement(IFrameLDIStore.DB_FRAME_DATA_TABLE_NAME); // Just in case
	            final PreparedStatement statement = getPreparedStatement(
	                    selectClause + whereClause + " GROUP BY " + frameTableAbbrev + "." + SESSION_ID,
	                    ResultSet.TYPE_FORWARD_ONLY,
	                    ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(IFrameLDIStore.DB_FRAME_DATA_TABLE_NAME, statement);
	
	            statement.setFetchSize(Integer.MIN_VALUE);
	
	            try {
	                fwc.setParameters(statement, 1);
	            } catch (final WhereControlException e) {
	                throw new DatabaseException("Error in SQL WHERE clause: " + e.getMessage());
	            }
	
	            if (printStmtOnly)
	            {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
	            }
	            else
	            {
	                final ResultSet results = statement.executeQuery();
                    resultsMap.put(IFrameLDIStore.DB_FRAME_DATA_TABLE_NAME, results);
	            }
	
                return getCountResults(IFrameLDIStore.DB_FRAME_DATA_TABLE_NAME);
            }
            catch (final SQLException e) {
            	throw new DatabaseException(e.getMessage(), e);
            }
            finally {
            	spf.close();
            }
        }

        /**
         * Convenience method to retrieve remaining results from the
         * channel aggregate count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextChannelAggregateCountResultsBatch()
        throws DatabaseException {
            return getNextCountResultsBatch(IChannelAggregateLDIStore.DB_CHANNEL_AGGREGATE_TABLE_NAME);
        }

        /**
         * Convenience method to retrieve remaining results from the
         * monitor channel value count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextMonitorChannelAggregateCountResultsBatch()
        throws DatabaseException
        {
            return getNextCountResultsBatch(IMonitorChannelAggregateLDIStore.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME);
        }

        /**
         * Convenience method to retrieve remaining results from the
         * header channel value count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextHeaderChannelAggregateCountResultsBatch()
        throws DatabaseException
        {
            return getNextCountResultsBatch(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME);
        }        
        
        /**
         * Convenience method to retrieve remaining results from the
         * SSE channel value count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextSseChannelAggregateCountResultsBatch()
        throws DatabaseException
        {
            return getNextCountResultsBatch(ISseChannelAggregateLDIStore.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME);
        }

        /**
         * Convenience method to retrieve remaining results from the
         * SSE header channel value count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextSseHeaderChannelAggregateCountResultsBatch()
        throws DatabaseException
        {
            return getNextCountResultsBatch(IHeaderChannelValueLDIStore.DB_HEADER_CHANNEL_VALUE_TABLE_NAME);
        }        
        
        /**
         * Convenience method to retrieve remaining results from the
         * EVR count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextEvrCountResultsBatch()
        throws DatabaseException {
            return getNextCountResultsBatch(IEvrLDIStore.DB_EVR_DATA_TABLE_NAME);
        }

        /**
         * Convenience method to retrieve remaining results from the
         * SSE EVR count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextSseEvrCountResultsBatch()
        throws DatabaseException {
            return getNextCountResultsBatch(ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME);
        }

        /**
         * Convenience method to retrieve remaining results from the
         * frame count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextFrameCountResultsBatch()
        throws DatabaseException {
            return getNextCountResultsBatch(IFrameLDIStore.DB_FRAME_DATA_TABLE_NAME);
        }

        /**
         * Convenience method to retrieve remaining results from the
         * FSW packet count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextPacketCountResultsBatch()
        throws DatabaseException {
            return getNextCountResultsBatch(IDbTableNames.DB_PACKET_DATA_TABLE_NAME);
        }

        /**
         * Convenience method to retrieve remaining results from the
         * SSE packet count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextSsePacketCountResultsBatch()
        throws DatabaseException {
            return getNextCountResultsBatch(ISsePacketLDIStore.DB_SSE_PACKET_DATA_TABLE_NAME);
        }

        /**
         * Convenience method to retrieve remaining results from the
         * command message count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextCommandCountResultsBatch()
        throws DatabaseException {
            return getNextCountResultsBatch(ICommandMessageLDIStore.DB_COMMAND_MESSAGE_DATA_TABLE_NAME);
        }

        /**
         * Convenience method to retrieve remaining results from the
         * log message count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextLogCountResultsBatch()
        throws DatabaseException {
            return getNextCountResultsBatch(ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME);
        }

        /**
         * Convenience method to retrieve remaining results from the
         * product count query. May have to be called repeatedly
         * until all results are returned (i.e. returned result is
         * finally empty).
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getNextProductCountResultsBatch()
        throws DatabaseException {
            return getNextCountResultsBatch(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME);
        }

        private List<Pair<Long, Long>> getNextCountResultsBatch(final String tableName)
        throws DatabaseException {
            return getCountResults(tableName);
        }

        /**
         * Build the SQL statement, execute it, and return the results
         * for FSW packet counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextPacketCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getPacketCounts(final IDbSessionInfoProvider tsi,
                                                       DatabaseTimeRange range,
                                                       final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.SCET);
            }

            initQuery(IDbTableNames.DB_PACKET_DATA_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute
            if (printStmtOnly)
            {
                // Dummy to get SQL statement printed

                final IDbSessionPreFetch dummy = fetchFactory.getSessionPreFetch(true, PreFetchType.NORMAL);
                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query
			final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false, PreFetchType.NORMAL);
            try {
	            spf.get(tsi);
	
	            String whereClause = null;
	
	            // *** Below originally from PacketFetch's getSqlWhereClause ***
	
            final String packetTableAbbrev = queryConfig.getTablePrefix(IDbTableNames.DB_PACKET_DATA_TABLE_NAME);
	            final String testSqlTemplate = spf.getIdHostWhereClause(packetTableAbbrev);
	
	            if (!testSqlTemplate.equals("")) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
	            }
	
	            // Add the proper time clause
	
	            final String timeWhere =
                DbTimeUtility.generateTimeWhereClause(
                    packetTableAbbrev, range, false, false, USE_EXTENDED_SCET);
	
	            if (timeWhere.length() > 0) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         timeWhere);
	            }
	
	            // *** Below originally from PacketFetch's populateAndExecute ***
	
	            if (whereClause == null) {
	                throw new DatabaseException("Generated null SQL WHERE clause for packets");
	            }
	
                whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                            IDbSqlFetch.generateDssIdWhere(dssIds,
	                                                                                    packetTableAbbrev));
	
                whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                            IDbSqlFetch.generateVcidWhere(vcids,
	                                                                                   packetTableAbbrev,
	                                                                                   true));
	
            final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
            		IDbTableNames.DB_PACKET_DATA_TABLE_NAME);
	
            closeStatement(IDbTableNames.DB_PACKET_DATA_TABLE_NAME); // Just in case
	            final PreparedStatement statement = getPreparedStatement(
	                    selectClause + whereClause + " GROUP BY " + packetTableAbbrev + "." + SESSION_ID,
	                    ResultSet.TYPE_FORWARD_ONLY,
	                    ResultSet.CONCUR_READ_ONLY);
            stmtsMap.put(IDbTableNames.DB_PACKET_DATA_TABLE_NAME, statement);
	
	            statement.setFetchSize(Integer.MIN_VALUE);
	
	            if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
	            }
	            else {
	                final ResultSet results = statement.executeQuery();
                resultsMap.put(IDbTableNames.DB_PACKET_DATA_TABLE_NAME, results);
	            }
	
            return getCountResults(IDbTableNames.DB_PACKET_DATA_TABLE_NAME);
            }
            catch (final SQLException e) {
            	throw new DatabaseException(e.getMessage(), e);
            }
            finally {
            	spf.close();
            }
        }

        /**
         * Build the SQL statement, execute it, and return the results
         * for SSE packet counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextSsePacketCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getSsePacketCounts(final IDbSessionInfoProvider tsi,
                                                         DatabaseTimeRange range,
                                                         final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.SCET);
            }

            initQuery(ISsePacketLDIStore.DB_SSE_PACKET_DATA_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute
            if (printStmtOnly)
            {
                // Dummy to get SQL statement printed

                final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true, PreFetchType.NORMAL);
                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false,
                                                      PreFetchType.NORMAL);
            try {
	            spf.get(tsi);
	
	            String whereClause = null;
	
	            // *** Below originally from PacketFetch's getSqlWhereClause ***
	
                final String ssePacketTableAbbrev = queryConfig.getTablePrefix(ISsePacketLDIStore.DB_SSE_PACKET_DATA_TABLE_NAME);
	            final String testSqlTemplate = spf.getIdHostWhereClause(ssePacketTableAbbrev);
	
	            if (!testSqlTemplate.equals("")) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
	            }
	
	            // Add the proper time clause
	
	            final String timeWhere =
                DbTimeUtility.generateTimeWhereClause(
                    ssePacketTableAbbrev, range, false, false, USE_EXTENDED_SCET);
	
	            if (timeWhere.length() > 0) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         timeWhere);
	            }
	
	            // *** Below originally from PacketFetch's populateAndExecute ***
	
	            if (whereClause == null) {
	                throw new DatabaseException("Generated null SQL WHERE clause for SSE packets");
	            }
	
            final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                                                            ISsePacketLDIStore.DB_SSE_PACKET_DATA_TABLE_NAME);
	
                closeStatement(ISsePacketLDIStore.DB_SSE_PACKET_DATA_TABLE_NAME); // Just in case
	            final PreparedStatement statement = getPreparedStatement(
	                    selectClause + whereClause + " GROUP BY " + ssePacketTableAbbrev + "." + SESSION_ID,
	                    ResultSet.TYPE_FORWARD_ONLY,
	                    ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(ISsePacketLDIStore.DB_SSE_PACKET_DATA_TABLE_NAME, statement);
	
	            statement.setFetchSize(Integer.MIN_VALUE);
	
	            if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
	            }
	            else {
	                final ResultSet results = statement.executeQuery();
                    resultsMap.put(ISsePacketLDIStore.DB_SSE_PACKET_DATA_TABLE_NAME, results);
	            }
	
                return getCountResults(ISsePacketLDIStore.DB_SSE_PACKET_DATA_TABLE_NAME);
            }
            catch (final SQLException e) {
            	throw new DatabaseException(e.getMessage(), e);
            }
            finally {
            	spf.close();
            }
       }

        /**
         * This is the internal class method that keeps track of the JDBC
         * ResultSet returned by a query. Every call to this method will return
         * a list of results that match the original query. The size of the
         * returned lists is determined by the batch size that was entered when
         * the query was made. When there are no more results, this method will
         * return an empty list.
         * 
         * @param tableName
         *            the name of the table to query
         *
         * @return The list of (session ID, count) pairs that is part of the
         *         results of a query executed using the "get[Type]Counts"
         *         methods. When an empty list is returned, it means no more
         *         results are left.
         *
         * @throws DatabaseException
         *             If there happens to be an exception retrieving the next
         *             set of results.
         */
        protected List<Pair<Long, Long>> getCountResults(final String tableName) throws DatabaseException {
            final List<Pair<Long, Long>> refs = new ArrayList<Pair<Long, Long>>();
            final ResultSet results = resultsMap.get(tableName);

            if(results == null)
            {
                return refs;
            }

            int count = 0;
            final int batchSize = batchSizeMap.get(tableName);

            try
            {
                /*
                 * Loop through until we fill up our first batch or we've
                 * got no more results.
                 */

                while(count < batchSize) {

                    if (results.next() == false) {
                        break;
                    }

                    final Pair<Long, Long> sessionCount = new Pair<Long, Long>();

                    sessionCount.setOne(results.getLong(queryConfig.getTablePrefix(tableName) + "." + SESSION_ID));
                    sessionCount.setTwo(results.getLong("count"));

                    refs.add(sessionCount);
                    count++;

                }

                // Handle any unhandled warnings
                SqlExceptionTools.logWarning(trace, results);

                /*
                 * If we're all done with results, clean up all the
                 * resources.
                 */
                if (results.isAfterLast() == true) {
                    closeResults(tableName);
                    closeStatement(tableName);
                }

            }
            catch(final SQLException e)
            {
                throw new DatabaseException("Error retrieving " + tableName + " records from database: " + e.getMessage());
            }

            return refs;
        }

        /**
         * Build the SQL statement, execute it, and return the results
         * for FSW EVR counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextEvrCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getEvrCounts(final IDbSessionInfoProvider tsi,
                                                   DatabaseTimeRange range,
                                                   final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.SCET);
            }

            initQuery(IEvrLDIStore.DB_EVR_DATA_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute

            if (printStmtOnly)
            {
                // Dummy to get SQL statement printed

                final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true, PreFetchType.NORMAL);

                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false,
                                      PreFetchType.NORMAL);
            try {
	            spf.get(tsi);
	
	            // *** Below taken from EvrFetch's getSqlWhereClause ***
	
                final String evrTableAbbrev = queryConfig.getTablePrefix(IEvrLDIStore.DB_EVR_DATA_TABLE_NAME);
	            final String testSqlTemplate = spf.getIdHostWhereClause(evrTableAbbrev);
	            String whereClause = null;
	
	            if (!testSqlTemplate.equals(""))
	            {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
	            }
	
	            // *** Below taken from EvrFetch's populateAndExecute ***
	
	            if (whereClause == null)
	            {
	                throw new DatabaseException(
	                              "Generated null SQL WHERE clause for EVRs");
	            }
	
	            whereClause =
                        IDbSqlFetch.addToWhere(whereClause,
                                               IDbSqlFetch.generateDssIdWhere(
	                                             dssIds,
	                                             evrTableAbbrev));
	            whereClause =
                        IDbSqlFetch.addToWhere(whereClause,
                                               IDbSqlFetch.generateVcidWhere(
	                                             vcids,
	                                             evrTableAbbrev,
	                                             true));
	
	            if (evrTypeSelect.fswRealtime != evrTypeSelect.fswRecorded)
	            {
                    whereClause = IDbSqlFetch.addToWhere(
	                                  whereClause,
	                                  generateRtClause(evrTableAbbrev,
	                                                   evrTypeSelect.fswRealtime));
	            }
	
	            final String selectClause =
                getSelectClause(QueryClauseType.COUNT_SELECT,
                                        IEvrLDIStore.DB_EVR_DATA_TABLE_NAME);
	
                closeStatement(IEvrLDIStore.DB_EVR_DATA_TABLE_NAME); // Just in case


            final String trc =
            		fetchFactory.getEvrFetch(sqlStmtOnly).getTimeRangeClause(range, true, USE_EXTENDED_SCET);

            final PreparedStatement statement = getPreparedStatement(selectClause       +
                                                                   whereClause    +
                                                                   trc            +
                                                                   " GROUP BY "   +
                                                                   evrTableAbbrev +
                                                                   "."            +
                                                                   SESSION_ID,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(IEvrLDIStore.DB_EVR_DATA_TABLE_NAME, statement);
	
	            statement.setFetchSize(Integer.MIN_VALUE);
	
	            if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
	            }
	            else {
	                final ResultSet results = statement.executeQuery();
                    resultsMap.put(IEvrLDIStore.DB_EVR_DATA_TABLE_NAME, results);
	            }
	
                return getCountResults(IEvrLDIStore.DB_EVR_DATA_TABLE_NAME);
            }
            catch (final SQLException e) {
            	throw new DatabaseException(e.getMessage(), e);
            }
            finally {
            	spf.close();
            }
       }

        /**
         * Build the SQL statement, execute it, and return the results
         * for SSE EVR counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextSseEvrCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getSseEvrCounts(final IDbSessionInfoProvider tsi,
                                                      DatabaseTimeRange range,
                                                      final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.SCET);
            }

            initQuery(ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute

            if (printStmtOnly)
            {
                // Dummy to get SQL statement printed

                final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true, PreFetchType.NORMAL);

                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false,
                                      PreFetchType.NORMAL);
            try {
	            spf.get(tsi);
	
	            // *** Below taken from EvrFetch's getSqlWhereClause ***
	
                final String sseEvrTableAbbrev = queryConfig.getTablePrefix(ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME);
	            final String testSqlTemplate = spf.getIdHostWhereClause(sseEvrTableAbbrev);
	            String whereClause = null;
	
	            if (!testSqlTemplate.equals(""))
	            {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
	            }
	
	            // *** Below taken from EvrFetch's populateAndExecute ***
	
	            if (whereClause == null) {
	                throw new DatabaseException("Genereated null SQL WHERE clause for SSE EVRs");
	            }
	
            final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                                                            ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME);
	
                closeStatement(ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME); // Just in case

            final PreparedStatement statement =
                getPreparedStatement(
                    selectClause                                                                  +
                        whereClause                                                               +
                        fetchFactory.getEvrFetch(sqlStmtOnly).getTimeRangeClause(range, false, USE_EXTENDED_SCET) +
                        " GROUP BY "                                                              +
                        sseEvrTableAbbrev                                                         +
                        "."                                                                       +
                        SESSION_ID,
	                    ResultSet.TYPE_FORWARD_ONLY,
	                    ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME, statement);
	
	            statement.setFetchSize(Integer.MIN_VALUE);
	
	            if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
	            }
	            else {
	                final ResultSet results = statement.executeQuery();
                    resultsMap.put(ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME, results);
	            }
	
                return getCountResults(ISseEvrLDIStore.DB_SSE_EVR_DATA_TABLE_NAME);
            }
            catch (final SQLException e) {
            	throw new DatabaseException(e.getMessage(), e);
            }
            finally {
            	spf.close();
            }
        }

        /**
         * Build the SQL statement, execute it, and return the results
         * for channel aggregate counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextChannelAggregateCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getChannelAggregateCounts(final IDbSessionInfoProvider tsi,
                                                            DatabaseTimeRange range,
                                                            final int batchSize)
                throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.ERT);
            }

            initQuery(IDbTableNames.DB_CHANNEL_AGGREGATE_TABLE_NAME,
                      batchSize);

            // Create pre-fetch query and execute

            if (printStmtOnly) {
                // Dummy to get SQL statement printed

               final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true, PreFetchType.GET_SCID);
                dummy.get(tsi);
                dummy.close();
           }

            // Must always run, even with printStmtOnly, to populate main query
            // Get spacecraft id as well

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false,
                                      PreFetchType.GET_SCID);

            try {
                spf.get(tsi);
    
                String whereClause = null;
    
                // *** Below taken from ChannelValueFetch's getSqlWhereClauses ***
    
                final String chanAggTableAbbrev = queryConfig.getTablePrefix(IChannelAggregateLDIStore.DB_CHANNEL_AGGREGATE_TABLE_NAME);
                final String testSqlTemplate = spf.getIdHostWhereClause(chanAggTableAbbrev);
    
                if (!testSqlTemplate.equals("")) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
                }
    
                // Add the proper time clause

                final String timeWhere =
                DbTimeUtility.generateAggregateTimeWhereClause(
                    chanAggTableAbbrev, range, false);
    
                if (timeWhere.length() > 0) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         timeWhere);
                }
    
                // *** Below taken from ChannelValueFetch's populateAndExecute ***
    
                if (whereClause == null) {
                    throw new DatabaseException("Generated null SQL WHERE clause for chanvals");
                }
    
                whereClause =
                        IDbSqlFetch.addToWhere(whereClause,
                                               IDbSqlFetch.generateDssIdWhere(
                                                 dssIds,
                                                 chanAggTableAbbrev));
    
                whereClause =
                        IDbSqlFetch.addToWhere(whereClause,
                                               IDbSqlFetch.generateVcidWhere(
                                                 vcids,
                                                 chanAggTableAbbrev,
                                                 true));
    
                if (channelTypeSelect.fswRealtime != channelTypeSelect.fswRecorded)
                {
                    whereClause = IDbSqlFetch.addToWhere(
                                      whereClause,
                                      generateAggregateRtClause(
                                          chanAggTableAbbrev,
                                          channelTypeSelect.fswRealtime));
                }
    
                final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                                                            IChannelAggregateLDIStore.DB_CHANNEL_AGGREGATE_TABLE_NAME);
    
                closeStatement(IChannelAggregateLDIStore.DB_CHANNEL_AGGREGATE_TABLE_NAME); // Just in case
                final PreparedStatement statement = getPreparedStatement(
                        selectClause + whereClause + " GROUP BY " + chanAggTableAbbrev + "." + SESSION_ID,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(IChannelAggregateLDIStore.DB_CHANNEL_AGGREGATE_TABLE_NAME, statement);
    
                statement.setFetchSize(Integer.MIN_VALUE);
    
                if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
                }
                else
                {
                    final ResultSet results = statement.executeQuery();
                    resultsMap.put(IChannelAggregateLDIStore.DB_CHANNEL_AGGREGATE_TABLE_NAME, results);
                }
                return getCountResults(IChannelAggregateLDIStore.DB_CHANNEL_AGGREGATE_TABLE_NAME);
            }
            catch (final SQLException e) {
                throw new DatabaseException(e.getMessage(), e);
            }
            finally {
                spf.close();
            }
        }
        
        /**
         * Build the SQL statement, execute it, and return the results
         * for SSE channel aggregate counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextSseChannelValueCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getSseChannelAggregateCounts(final IDbSessionInfoProvider tsi,
                                                               DatabaseTimeRange range,
                                                               final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.ERT);
            }

            initQuery(ISseChannelAggregateLDIStore.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute

            if (printStmtOnly) {
                // Dummy to get SQL statement printed

               final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true, PreFetchType.GET_SCID);
                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query
            // Get spacecraft id as well

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false, PreFetchType.GET_SCID);

            try {
                spf.get(tsi);
    
                String whereClause = null;
    
                final String sseChanAggTableAbbrev = queryConfig.getTablePrefix(ISseChannelAggregateLDIStore.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME);
                final String testSqlTemplate = spf.getIdHostWhereClause(sseChanAggTableAbbrev);
    
                if (!testSqlTemplate.equals("")) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
                }
    
                // Add the proper time clause
    
                final String timeWhere =
                DbTimeUtility.generateAggregateTimeWhereClause(
                    sseChanAggTableAbbrev, range, false);
    
                if (timeWhere.length() > 0) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         timeWhere);
                }
    
                // *** Below taken from ChannelValueFetch's populateAndExecute ***
    
                if (whereClause == null) {
                    throw new DatabaseException("Generated null SQL WHERE clause for SSE chanvals");
                }
    
                final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                        ISseChannelAggregateLDIStore.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME);
    
                closeStatement(ISseChannelAggregateLDIStore.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME); // Just in case
                final PreparedStatement statement = getPreparedStatement(
                        selectClause + whereClause + " GROUP BY " + sseChanAggTableAbbrev + "." + SESSION_ID,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(ISseChannelAggregateLDIStore.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME, statement);
    
                statement.setFetchSize(Integer.MIN_VALUE);
    
                if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
                }
                else {
                    final ResultSet results = statement.executeQuery();
                    resultsMap.put(ISseChannelAggregateLDIStore.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME, results);
                }
                return getCountResults(ISseChannelAggregateLDIStore.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME);
            }
            catch (final SQLException e) {
                throw new DatabaseException(e.getMessage(), e);
            }
            finally {
                spf.close();
            }
        }        
        
        /**
         * Build the SQL statement, execute it, and return the results
         * for monitor channel aggregate counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextMonitorChannelAggregateCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getMonitorChannelAggregateCounts(final IDbSessionInfoProvider tsi,
                                                                   DatabaseTimeRange range,
                                                                   final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.ERT);
            }

            initQuery(IMonitorChannelAggregateLDIStore.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute

            if (printStmtOnly) {
                // Dummy to get SQL statement printed

                final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true,
                            PreFetchType.GET_SCID);
                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query
            // Get spacecraft id as well

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false,
                    PreFetchType.GET_SCID);

            try {
                spf.get(tsi);
    
                String whereClause = null;
    
                final String monChanAggTableAbbrev = queryConfig.getTablePrefix(IMonitorChannelAggregateLDIStore.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME);
                final String testSqlTemplate = spf.getIdHostWhereClause(monChanAggTableAbbrev);
    
                if (!testSqlTemplate.equals("")) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
                }
    
                // Add the proper time clause  
                final String timeWhere =
                DbTimeUtility.generateAggregateTimeWhereClause(
                    monChanAggTableAbbrev, range, true);
    
                if (timeWhere.length() > 0) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         timeWhere);
                }
    
                if (whereClause == null) {
                    throw new DatabaseException("Generated null SQL WHERE clause for chanvals");
                }
    
                whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                     IDbSqlFetch.generateDssIdWhere(dssIds,
                                                                                        monChanAggTableAbbrev));
    
                final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                        IMonitorChannelAggregateLDIStore.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME);
    
                closeStatement(IMonitorChannelAggregateLDIStore.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME); // Just in case
                final PreparedStatement statement = getPreparedStatement(
                        selectClause + whereClause + " GROUP BY " + monChanAggTableAbbrev + "." + SESSION_ID,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(IMonitorChannelAggregateLDIStore.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME, statement);
    
                statement.setFetchSize(Integer.MIN_VALUE);
    
                if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
                }
                else {
                    final ResultSet results = statement.executeQuery();
                    resultsMap.put(IMonitorChannelAggregateLDIStore.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME, results);
                }
                return getCountResults(IMonitorChannelAggregateLDIStore.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME);
            }
            catch (final SQLException e) {
                throw new DatabaseException(e.getMessage(), e);
            }
            finally {
                spf.close();
            }
        }

        /**
         * Build the SQL statement, execute it, and return the results
         * for header channel aggregate counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextHeaderChannelValueCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getHeaderChannelAggregateCounts(final IDbSessionInfoProvider tsi,
                                                                  DatabaseTimeRange range,
                                                                  final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.ERT);
            }

            initQuery(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute

            if (printStmtOnly) {
                // Dummy to get SQL statement printed

                final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true,
                            PreFetchType.GET_SCID);
                dummy.get(tsi);
                dummy.close();
           }

            // Must always run, even with printStmtOnly, to populate main query
            // Get spacecraft id as well

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false,
                    PreFetchType.GET_SCID);

            try {
                spf.get(tsi);
    
                String whereClause = null;
        
                final String headerChanAggTableAbbrev = queryConfig.getTablePrefix(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME);
                final String testSqlTemplate = spf.getIdHostWhereClause(headerChanAggTableAbbrev);
    
                if (!testSqlTemplate.equals("")) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
                }
    
                // Add the proper time clause    
                final String timeWhere =
                DbTimeUtility.generateAggregateTimeWhereClause(
                    headerChanAggTableAbbrev, range, false);
    
                if (timeWhere.length() > 0) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         timeWhere);
                }
    
                if (whereClause == null) {
                    throw new DatabaseException("Generated null SQL WHERE clause for header chanvals");
                }
    
                whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                     IDbSqlFetch.generateDssIdWhere(dssIds,
                                                                                        headerChanAggTableAbbrev));
                whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                     IDbSqlFetch.generateVcidWhere(vcids,
                                                                                       headerChanAggTableAbbrev,
                                                                                       true));
                whereClause = IDbSqlFetch.addToWhere(whereClause,
                              "(" + headerChanAggTableAbbrev + ".channelType != 'SSE_HEADER')");
    
                final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                        IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME);
    
                closeStatement(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME); // Just in case
                final PreparedStatement statement = getPreparedStatement(
                        selectClause + whereClause + " GROUP BY " + headerChanAggTableAbbrev + "." + SESSION_ID,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME, statement);
    
                statement.setFetchSize(Integer.MIN_VALUE);
    
                if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
                }
                else {
                    final ResultSet results = statement.executeQuery();
                    resultsMap.put(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME, results);
                }
                return getCountResults(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME);
            }
            catch (final SQLException e) {
                throw new DatabaseException(e.getMessage(), e);
            }
            finally {
                spf.close();
            }
        }        
        
        /**
         * Build the SQL statement, execute it, and return the results
         * for SSE header channel value counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextSseHeaderChannelValueCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         *
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getSseHeaderChannelAggregateCounts(final IDbSessionInfoProvider tsi,
                                                                     DatabaseTimeRange range,
                                                                     final int batchSize) throws DatabaseException
        {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.ERT);
            }

            initQuery(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute

            if (printStmtOnly) {
                // Dummy to get SQL statement printed

               final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true, PreFetchType.GET_SCID);
                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query
            // Get spacecraft id as well

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false,
                                      PreFetchType.GET_SCID);

            try {
                spf.get(tsi);
    
                String whereClause = null;
    
                final String sseHeaderChanAggTableAbbrev = queryConfig.getTablePrefix(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME);
                final String testSqlTemplate = spf.getIdHostWhereClause(sseHeaderChanAggTableAbbrev);
    
                if (!testSqlTemplate.equals("")) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
                }
    
                // Add the proper time clause
                final String timeWhere =
                DbTimeUtility.generateAggregateTimeWhereClause(
                    sseHeaderChanAggTableAbbrev, range, false);
    
                if (timeWhere.length() > 0) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         timeWhere);
                }
                    
                whereClause = IDbSqlFetch.addToWhere(whereClause,
                        "(" + sseHeaderChanAggTableAbbrev + ".channelType='SSE_HEADER')");
    
                if (whereClause == null) {
                    throw new DatabaseException("Generated null SQL WHERE clause for SSE header chanvals");
                }
    
                final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                        IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME);
    
                closeStatement(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME); // Just in case
                final PreparedStatement statement = getPreparedStatement(
                        selectClause + whereClause + " GROUP BY " + sseHeaderChanAggTableAbbrev + "." + SESSION_ID,
                        ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME, statement);
    
                statement.setFetchSize(Integer.MIN_VALUE);
    
                if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
                }
                else {
                    final ResultSet results = statement.executeQuery();
                    resultsMap.put(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME, results);
                }
                return getCountResults(IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME);
            }
            catch (final SQLException e) {
                throw new DatabaseException(e.getMessage(), e);
            }
            finally {
                spf.close();
            }
        }        
        
        /**
         * Build the SQL statement, execute it, and return the results
         * for command message counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextCommandCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range Event time range to query by.
         * @param batchSize Size of results batch to return.
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getCommandCounts(final IDbSessionInfoProvider tsi,
                                                       DatabaseTimeRange range,
                                                       final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if (range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.EVENT_TIME);
            }

            initQuery(ICommandMessageLDIStore.DB_COMMAND_MESSAGE_DATA_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute

            if (printStmtOnly)
            {
                // Dummy to get SQL statement printed

                final IDbSessionPreFetch dummy = fetchFactory.getSessionPreFetch(true);

                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false);

            try {
	            spf.get(tsi);
	
	            String whereClause = null;
	
	            // *** Below taken from CommandFetch's getSqlWhereClause ***
	
                final String commandTableAbbrev = queryConfig.getTablePrefix(ICommandMessageLDIStore.DB_COMMAND_MESSAGE_DATA_TABLE_NAME);
	            final String testSqlTemplate = spf.getIdHostWhereClause(commandTableAbbrev);
	
	            if (!testSqlTemplate.equals("")) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
	            }
	
	            // Add the time filtering

                final String timeWhere = DbTimeUtility.generateTimeWhereClause(
                        ICommandFetch.COMMAND_STATUS_TABLE_ABBREV,
                        range,
                        false,
                        false,
                        USE_EXTENDED_SCET);
	
	            if (timeWhere.length() > 0) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         timeWhere);
	            }
	
	            // *** Below taken from CommandFetch's populateAndExecute ***
	
	            if (whereClause == null) {
	                throw new DatabaseException("Generated null SQL WHERE clause for commands");
	            }
	
                final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                                                            ICommandMessageLDIStore.DB_COMMAND_MESSAGE_DATA_TABLE_NAME);
	
                closeStatement(ICommandMessageLDIStore.DB_COMMAND_MESSAGE_DATA_TABLE_NAME); // Just in case
	            final PreparedStatement statement = getPreparedStatement(
	                    selectClause + whereClause + " GROUP BY " + commandTableAbbrev + "." + SESSION_ID,
	                    ResultSet.TYPE_FORWARD_ONLY,
	                    ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(ICommandMessageLDIStore.DB_COMMAND_MESSAGE_DATA_TABLE_NAME, statement);
	
	            statement.setFetchSize(Integer.MIN_VALUE);
	
	            if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
	            }
	            else {
	                final ResultSet results = statement.executeQuery();
                    resultsMap.put(ICommandMessageLDIStore.DB_COMMAND_MESSAGE_DATA_TABLE_NAME, results);
	            }
                return getCountResults(ICommandMessageLDIStore.DB_COMMAND_MESSAGE_DATA_TABLE_NAME);
            }
            catch (final SQLException e) {
            	throw new DatabaseException(e.getMessage(), e);
            }
            finally {
            	spf.close();
            }
        }

        /**
         * Build the SQL statement, execute it, and return the results
         * for product counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextProductCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range ERT time range to query by.
         * @param batchSize Size of results batch to return.
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getProductCounts(final IDbSessionInfoProvider tsi,
                                                       DatabaseTimeRange range,
                                                       final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if(range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.CREATION_TIME);
            }

            initQuery(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute

            if (printStmtOnly) {
                // Dummy to get SQL statement printed

                final IDbSessionPreFetch dummy =
                    fetchFactory.getSessionPreFetch(true, PreFetchType.GET_OD);
                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false,
                                      PreFetchType.GET_OD);
            try {
	            spf.get(tsi);
	
	            String whereClause = null;
	
	            // *** Below taken from AbstractProductFetch's getSqlWhereClause ***
	
                final String productTableAbbrev = queryConfig.getTablePrefix(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME);
	            final String testSqlTemplate = spf.getIdHostWhereClause(productTableAbbrev);
	
	            if (!testSqlTemplate.equals("")) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
	            }
	
	            // Add time filtering
	
	            final String timeWhere =
	                DbTimeUtility.generateTimeWhereClause(productTableAbbrev,
	                                                      range,
	                                                      true,
                                                      false,
                                                      USE_EXTENDED_SCET);
	            if (timeWhere.length() > 0)
	            {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         timeWhere);
	            }
	
	            // *** Below taken from AbstractProductFetch's populateAndExecute ***
	
	            if (whereClause == null) {
	                throw new DatabaseException("Generated null SQL WHERE clause for products");
	            }

	
                whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                     IDbSqlFetch.generateVcidWhere(vcids,
	                                                                                   productTableAbbrev,
	                                                                                   false));
	
                final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                                                            IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME);
	
                closeStatement(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME); // Just in case
	            final PreparedStatement statement = getPreparedStatement(
	                    selectClause + whereClause + " GROUP BY " + productTableAbbrev + "." + SESSION_ID,
	                    ResultSet.TYPE_FORWARD_ONLY,
	                    ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME, statement);
	
	            statement.setFetchSize(Integer.MIN_VALUE);
	
	            if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
	            }
	            else {
	                final ResultSet results = statement.executeQuery();
                    resultsMap.put(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME, results);
	            }
                return getCountResults(IProductLDIStore.DB_PRODUCT_DATA_TABLE_NAME);
            }
            catch (final SQLException e) {
            	throw new DatabaseException(e.getMessage(), e);
            }
            finally {
            	spf.close();
            }
        }

        /**
         * Build the SQL statement, execute it, and return the results
         * for log counts. This method should only be called once.
         * Returned is the first batch of results. After that, all
         * subsequent batches of results should be retrieved using
         * repeated calls to getNextLogCountResultsBatch. When an
         * empty list is returned, there are no more results left.
         *
         * @param tsi Database session information object.
         * @param range Event time range to query by.
         * @param batchSize Size of results batch to return.
         * @return The list of objects that is part of the results of
         *            the count query. When an empty list is returned, it
         *            means there are no more results left. Each list
         *            item is a pair of session ID (left) and count
         *            (right).
         * @throws DatabaseException If there happens to be an exception
         *                         retrieving the next set of results.
         */
        public List<Pair<Long, Long>> getLogCounts(final IDbSessionInfoProvider tsi,
                                                   DatabaseTimeRange range,
                                                   final int batchSize) throws DatabaseException {

            if (tsi == null) {
                throw new IllegalArgumentException("Input test session information was null");
            }

            if (!useDatabase()) {
                return new ArrayList<Pair<Long, Long>>(0);
            }

            if(range == null) {
                range = new DatabaseTimeRange(DatabaseTimeType.EVENT_TIME);
            }

            initQuery(ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME, batchSize);

            // Create pre-fetch query and execute

            if (printStmtOnly) {
                // Dummy to get SQL statement printed

                final IDbSessionPreFetch dummy = fetchFactory.getSessionPreFetch(true);

                dummy.get(tsi);
                dummy.close();
            }

            // Must always run, even with printStmtOnly, to populate main query

            final IDbSessionPreFetch spf = fetchFactory.getSessionPreFetch(false);

            try {
	            spf.get(tsi);
	
	            String whereClause = null;
	
	            // *** Below taken from LogFetch's getSqlWhereClause ***
	
                final String logTableAbbrev = queryConfig.getTablePrefix(ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME);
	            final String testSqlTemplate = spf.getIdHostWhereClause(logTableAbbrev);
	
	            if (!testSqlTemplate.equals("")) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         testSqlTemplate);
	            }
	
	            // Add the time filtering
	
	            final String timeWhere = DbTimeUtility.generateTimeWhereClause(
	                                         logTableAbbrev,
	                                         range,
	                                         false,
                                         false,
                                         USE_EXTENDED_SCET);
	
	            if (timeWhere.length() > 0) {
                    whereClause = IDbSqlFetch.addToWhere(whereClause,
                                                         timeWhere);
	            }
	
	            // *** Below taken from LogFetch's populateAndExecute ***
	
	            if (whereClause == null) {
	                throw new DatabaseException("Generated null SQL WHERE clause for logs");
	            }
	
                final String selectClause = getSelectClause(QueryClauseType.COUNT_SELECT,
                                                            ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME);
	
                closeStatement(ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME); // Just in case
	            final PreparedStatement statement = getPreparedStatement(
	                    selectClause + whereClause + " GROUP BY " + logTableAbbrev + "." + SESSION_ID,
	                    ResultSet.TYPE_FORWARD_ONLY,
	                    ResultSet.CONCUR_READ_ONLY);
                stmtsMap.put(ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME, statement);
	
	            statement.setFetchSize(Integer.MIN_VALUE);
	
	            if (printStmtOnly) {
                    IDbSqlFetch.printSqlStatement(statement,
                                                  "Main");
	            }
	            else {
	                final ResultSet results = statement.executeQuery();
                    resultsMap.put(ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME, results);
	            }
                return getCountResults(ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_TABLE_NAME);
            }
            catch (final SQLException e) {
            	throw new DatabaseException(e.getMessage(), e);
            }
            finally {
            	spf.close();
            }
       }
    }


    /**
     * Generate real-time clause.
     *
     * @param abbrev   Table abbreviation
     * @param realTime True is real-time, else recorded
     *
     * @return Clause as string
     */
    private static String generateRtClause(final String  abbrev,
                                           final boolean realTime)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append('(');

        sb.append(abbrev).append(".isRealtime ");
        sb.append(realTime ? "!= 0" : "= 0");

        sb.append(')');

        return sb.toString();
    }

    
    /**
     * Generate real-time clause for EHA aggregates
     *
     * @param abbrev   Table abbreviation
     * @param realTime True is real-time, else recorded
     *
     * @return Clause as string
     */
    private static String generateAggregateRtClause(final String  abbrev,
                                           final boolean realTime) {
        final StringBuilder sb = new StringBuilder();

        sb.append('(');

        sb.append(abbrev).append(".channelType ");
        sb.append(realTime ? "= 'FSW_RT'" : "= 'FSW_REC'");

        sb.append(')');

        return sb.toString();
    }    
    

    /**
     * Add postfix to table name if necessary.
     *
     * @param nominalTable Nominal table name
     *
     * @return Actual table name
     *
     */
    private String getActualTableName(final String nominalTable)
    {
        String table = nominalTable;

        if (USE_EXTENDED_SCET &&
            dbProperties.getExtendedTables().contains(nominalTable))
        {
            table += dbProperties.getExtendedPostfix();
        }

        return table;
    }


    /**
     * Get select clause for proper table and clsuse.
     *
     * @param qct 
     *
     * @return SQL string
     *
     */
    private String getSelectClause(final QueryClauseType qct,
                                          final String          table)
    {
        return queryConfig.getQueryClause(qct, getActualTableName(table));
    }
}
