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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.DssVcidOptions;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.common.config.CsvQueryProperties;
import jpl.gds.common.config.CsvQueryPropertiesException;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.db.api.sql.fetch.ChannelTypeSelect;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.types.IDbChannelSampleFactory;
import jpl.gds.db.api.types.IDbCommandFactory;
import jpl.gds.db.api.types.IDbEvrFactory;
import jpl.gds.db.api.types.IDbLog1553Factory;
import jpl.gds.db.api.types.IDbLog1553Updater;
import jpl.gds.db.api.types.IDbLogFactory;
import jpl.gds.db.api.types.IDbProductMetadataFactory;
import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.db.app.util.OutputFormatType;
import jpl.gds.db.app.util.OutputFormatter;
import jpl.gds.db.app.util.OutputFormatterFactory;
import jpl.gds.shared.channel.ChannelIdUtility;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.types.Pair;

/**
 * <pre>
 * This is a replacement of chill_session_report. It will eventually add more
 * features in the future. 
 * 
 * It currently supports 3 output formats: .txt, .csv, and .xlsx. If no command
 * line option is supplied for the output format (ie -o csv or -o excel) it will 
 * print .csv files by default. 
 * If --csv is supplied than the MPCS csv output format is used. 
 * If --excel is supplied than the MPCS csv output format is used, and in
 * addition a excel sorted file with all data is created. Note, that currently no
 * excel single files are created.
 * 
 * It creates time sorted (by ERT (default) or SCET) files for products, commands,
 * evrs, logs, 1553 logs, and channels. Also, creates a time sorted file of all
 * of the data in one file. This is the sorted file. Note that if SCET sorting is
 * selected, 1553 logs are not included. Also, commands and logs are sorted in the 
 * time sorted file of all data based on the available realtime data, not recorded data.
 * 
 * This class utilizes various velocity templates to print the various output formats,
 * thus those classes must be modified to change the output formats 
 * (sr_text.vm and sr_csv.vm).
 * 
 * In order to add any command line options to chill_get_everything, you must 
 * parse it from the command line in configureApp, where you also add it to the
 * relevant app(s) command line(s). You must also add....
 * 
 * TODO Things to do: 
 *      May want to incorporate option of both excel formats (.xls or
 *         .xlsx) in a config file or command line option 
 *      Anything that is configurable width, colors, etc should eventually be put 
 *         into a config file. 
 *      Add a app option to only display channels when they change 
 *      SCET sorting for 1553 logs SCLK sorting
 *      Ben Cichy's mslpp tool uses /proj/msl/sys/tools/lib/mslsr.py to
 *         parse the session report output
 * </pre>
 *
 */

public class GetEverythingApp extends AbstractFetchApp
{
    /** Environment Variables */
    public static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_get_everything");
    
    /*
     * Command line options
     */
    private static final String SESSION_APP_COMMAND = "chill_get_sessions";
    private static final String GET_EVERYTHING_APP_COMMAND = "chill_get_everything";
    private static final String CHANNEL_APP_COMMAND = "chill_get_chanvals";
    private static final String COMMAND_APP_COMMAND = "chill_get_commands";
    private static final String EVR_APP_COMMAND = "chill_get_evrs";
    private static final String LOG_APP_COMMAND = "chill_get_logs";
    private static final String PRODUCT_APP_COMMAND = "chill_get_products";
    private static final String LOG1553_APP_COMMAND = "chill_decode_1553";
    private static final String PATTERN = "Pattern";
    private static final String FSWVERSION_PATTERN_SHORT = ReservedOptions.FSWVERSION_SHORT_VALUE;
    private static final String FSWVERSION_PATTERN_LONG = ReservedOptions.FSWVERSION_LONG_VALUE
            + PATTERN;
    private static final String TESTDESCRIPTION_PATTERN_SHORT = ReservedOptions.TESTDESCRIPTION_SHORT_VALUE;
    private static final String TESTDESCRIPTION_PATTERN_LONG = ReservedOptions.TESTDESCRIPTION_LONG_VALUE
            + PATTERN;
    private static final String TESTNAME_PATTERN_SHORT = ReservedOptions.TESTNAME_SHORT_VALUE;
    private static final String TESTNAME_PATTERN_LONG = ReservedOptions.TESTNAME_LONG_VALUE
            + PATTERN;
    private static final String TESTHOSTNAME_PATTERN_LONG = ReservedOptions.TESTHOST_LONG_VALUE
            + PATTERN;
    private static final String TESTUSER_PATTERN_LONG = ReservedOptions.TESTUSER_LONG_VALUE
            + PATTERN;
    private static final String TESTUSER_PATTERN_SHORT = ReservedOptions.TESTUSER_SHORT_VALUE;
    private static final String TESTTYPE_PATTERN_LONG = ReservedOptions.TESTTYPE_LONG_VALUE
            + PATTERN;
    private static final String TESTTYPE_PATTERN_SHORT = ReservedOptions.TESTTYPE_SHORT_VALUE;

    // Various static strings
    private static final String REQUIRES_VALUE = " requires a command line value";
    private static final String BIN = "/bin/";
    private static final String DOUBLE_DASH = "--";
    private static final String SINGLE_DASH = "-";
    private static final String COMMAND_PREFIX = "Command: ";
    private static final String STRING_ARG = "string";
    private static final String ERT = "ERT";
    private static final String SCET = "SCET";
    private static final String CANNOT_ENTER = "You cannot enter both the --";
    private static final String AND_THE = " and the --";
    private static final String COMMAND_LINE = " command line options";
    
    /**
     * A tree map that stores ALL necessary information for EACH data type. It
     * does so by mapping the data type to a ArrayList with each necessary
     * information/also for evr levels/counts.
     */
    private Map<DataType, ArrayList<Object>> DataMap;
    private Map<String, ArrayList<Object>> EvrLevelMap;
    private Map<String, Integer> LogLevelMap;
    private Map<String, Integer> EhaMap;

    /**
     * Variables used to determine data location in ArrayList in EvrLevelMap
     * There is only the output buffer and the current count.
     */
    private static final int EVR_OUT_BUFF = 0;
    private static final int EVR_COUNT = 1;

    /**
     * Variables used to keep track of overall counts for different data types.
     */
    private int totalEvrs = 0;
    private int totalCmds = 0;
    private int totalProds = 0;
    private int totalCount = 0;
    private int totalLogs = 0;
    private int totalEha = 0;
    private int totalLog1553 = 0;

    /**
     * Variables used to determine data location in ArrayList in DataMap
     */

    /**
     * DONT_GET is true or false depending on if the user specified not to
     * return a data type, or if they specified to return channels, but didn't
     * actually specify any on the command line.
     */
    private static final int DONT_GET = 0;

    /**
     * CMD_LINE is each data types command line as created by processing the
     * command line in configureApp
     */
    private static final int CMD_LINE_STR = 1;
    private static final int CMD_LINE_LIST = 2;

    /**
     * EMPTY is determined while reading the files. It starts as false, but if a
     * user specifies "DONT_GET" or reading a file returns no more data, it is
     * set to true.
     */
    private static final int EMPTY = 3;

    /**
     * LINE is the most current processed line from reading the file
     */
    private static final int LINE = 4;

    /**
     * PROCESSED_HEADER tells us if we have successfully processed the header
     * for this data type yet. it is also used at the end to discard any files
     * who never actually processed any data.
     */
    private static final int PROCESSED_HEADER = 5;

    /**
     * ERT_POSN, SCLK_POSN, RT_POSN tell us which position in a "line" that type
     * of item is for each data, so we may quickly retrieve it later.
     */
    private static final int ERT_POSN = 6;
    private static final int SCLK_POSN = 7;
    private static final int RT_POSN = 8;

    /**
     * INPUT_BUFF and OUTPUT_BUFF are our buffered readers/writers to read
     * in/print out our data.
     */
    private static final int INPUT_BUFF = 9;
    private static final int OUTPUT_BUFF = 10;

    /**
     * PROCESS_BUILDER AND PROCESS are for starting the processes for each
     * chill_get_datatype.
     */
    private static final int PROCESS_BUILDER = 11;
    private static final int PROCESS = 12;

    /**
     * FILENAME is the filename for this datatype.
     */
    private static final int FILENAME = 13;

    /**
     * HEADER is used to store the header for each data type
     */
    private static final int HEADER = 14;

    /**
     * LEVEL_POSN stores the posn in a evr line of the level.
     */
    private static final int LEVEL_POSN = 15;

    /**
     * EHA_ID/NAME_POSN stores the posn in eha of the id/name.
     */
    private static final int EHA_ID_POSN = 16;
    private static final int SESSION_POSN = 17;
    private int ehaNamePosn= 0;

    /**
     * This is the BufferedWriter to write to the main sorted file.
     */
    private BufferedWriter outAll = null;

    /**
     * This lets us know we are only processing the sql only outputs
     */
    private boolean sqlOnly = false;
    /**
     * The ArrayLists used for parsing out the command line to be sent to each
     * get_*
     */
    private final List<String> cmdlineGe = new ArrayList<String>();
    private final List<String> cmdlineSession = new ArrayList<String>();
    private final List<String> cmdlineChan = new ArrayList<String>();
    private final List<String> cmdlineCmd = new ArrayList<String>();
    private final List<String> cmdlineEvr = new ArrayList<String>();
    private final List<String> cmdlineLog = new ArrayList<String>();
    private final List<String> cmdlineProd = new ArrayList<String>();
    private final List<String> cmdline1553 = new ArrayList<String>();
    /**
     * These are booleans that help keep track of the first pass through each
     * type of data, if any data has been processed...
     */
    private boolean firstPass = true;
    private boolean processedData = false;
    /**
     * The raw 1553 bus log file name entered by user.
     */
    private String raw1553File;
    private String decodeFile;
    /**
     * Flag that is set to true if 1553 log data will be incorporated in the
     * report
     */
    private boolean has1553Logs;
    /**
     * Variables to store our lowest overall datatype, the current lowest CMD
     * and LOG for sclk sorting...
     */
    private DataType lowestDataType;
    private DataType lowestERTDataType;
    private SortDataStructure lowestCMD;
    private SortDataStructure lowestLOG;
    /**
     * The desired output directory and file
     */
    private String outputDir;
    private File outputDirFile;
    private OutputFormatter outFormatterExcel;
    private static final String csvHeader = "\"RECORD_TYPE\",\"SESSION_ID\",\"SESSION_HOST\"";
    private static final String csvSRHeader = "ERT,SCET,SCLK,SOURCE,TYPE,ID,DATA,SEQUENCE_ID";
    private static final String txtSRHeader = "ERT                           	SCET                          	SCLK                          	";
    private static final String csv1553Header = "recordType,sessionId,sessionHost,systemTime,sclk,bus,remoteTerminal,subAddress,transmitReceiveStatus,data";
    private static final String text1553Header = "SYSTEM_TIME	SCET	SCLK	BUS	REMOTE_TERMINAL	SUB_ADDRESS	TR_STATUS	DATA";

    /**
     * True if the the output format is in a csv/excel/text/SRcsv format
     */
    private boolean outCSV;
    private boolean outEXCEL;
    private boolean outSRTEXT;
    private boolean outSRCSV;
    private boolean outTEXT;
    /**
     * The file extension for output files
     */
    private String ext;
    /**
     * Boolean that is set to true if using the default directory to hold the
     * output files from this report
     */
    private boolean useDefaultDir;
    /**
     * The current date
     */
    private final IAccurateDateTime date;
    /**
     * Priority Queue used for storing the latest (max 5) items.
     */
    private final PriorityQueue<SortDataStructure> sortPQ = new PriorityQueue<SortDataStructure>();

    /**
     * The list of session ids that are queried
     */
    private List<String> sessionIdList;

    /**
     * The list of fsw dict directories/versions for each session
     */
    private List<String> fswList;

    /**
     * True if the combined output (sorted file) should be sorted by ERT
     */
    private boolean doERT;

    /**
     * True if the combined output (sorted file) should be sorted by SCET
     */
    private boolean doSCET;

    /**
     * True if there should be no channels/evrs/cmds/prods/logs/1553logs in the
     * report
     */
    private boolean noEHA;
    private boolean noEVR;
    private boolean noCMD;
    private boolean noPROD;
    private boolean noLOG;
    private boolean no1553LOG;
    private boolean noSession = true;
    
    private final IDbChannelSampleFactory          dbChannelSampleFactory;
    private final IDbEvrFactory                    dbEvrFactory;
    private final IDbCommandFactory                dbCommandFactory;
    private final IDbProductMetadataFactory        dbProductMetadataFactory;
    private final IDbLogFactory                    dbLogFactory;
    private final IDbLog1553Factory                dbLog1553Factory;
    private final SseContextFlag                   sseFlag;

    /**
     * The various types of data that this app handles
     */
    private enum DataType {
        EHA, EVR, CMD, PROD, LOG, LOG_1553, SORTED
    }

    /**
     * The various header titles this app processes
     */
    private enum ProcessType {
        ERT, SCLK, REALTIME, LEVEL, EHA, SESSION
    }

    /**
     * The private inner class used to sort each line returned from the get
     * tools
     */
    private class SortDataStructure implements Comparable<Object> {
        // We store the ert, sclk, datatype and line of each
        public String ert;
        public String sclk;
        public DataType dataType;
        public String line;
        public boolean realTime;

        public SortDataStructure(final String fullLine, final DataType dt)
                throws ChillGetEverythingException {
            ert = processLine(fullLine, dt, ProcessType.ERT);
            sclk = processLine(fullLine, dt, ProcessType.SCLK);
            dataType = dt;
            line = fullLine;
            String rt;
            switch (dt) {
            // Only EHA and EVR can be realtime.
            case EHA:
                rt = processLine(fullLine, dt, ProcessType.REALTIME)
                        .replaceAll("\"", "").replaceAll("[\\p{Punct}(]", "")
                        .replaceAll("[\\p{Punct})]", "");
                realTime = rt.equals("true");
                break;
            case EVR:
                rt = processLine(fullLine, dt, ProcessType.REALTIME)
                        .replaceAll("\"", "").replaceAll("[\\p{Punct}(]", "")
                        .replaceAll("[\\p{Punct})]", "");
                realTime = rt.equals("true");
                break;
            default:
                realTime = false;
                break;
            }
        }

        /**
         * {@inheritDoc}
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(final Object ob) {
        	if (!(ob instanceof SortDataStructure )) {
        		return false;
        	}
        	return compareTo(ob) == 0;
        }
        
        /**
         * {@inheritDoc}
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
        	  assert false : "hashCode not designed";
        	  return 42; // any arbitrary constant will do 
        }
        
        /**
         * {@inheritDoc}
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        @Override
        public int compareTo(final Object ob) {
            String tData = "";
            String oData = "";
            final SortDataStructure t = this;
            final SortDataStructure o = (SortDataStructure) ob;
            // If they want to sort by ERT, it is easy. Just compare ERT values.
            if (doERT) {
                tData = t.ert;
                oData = (o).ert;
            } else { // doSCET.
                tData = this.sclk;
                oData = (o).sclk;
            } // Ends the else (ie doSCET)

            if (tData.compareTo(oData) == 0) { // They are equal, so we return
                                               // based on what data type they
                                               // are.
                                               // Currently it is EHA < PROD <
                                               // EVR < LOG < CMD
                if ((t.dataType).equals(DataType.EHA)) {
                    return -1;
                }
                if (((o).dataType).equals(DataType.EHA)) {
                    return 1;
                }
                if ((t.dataType).equals(DataType.CMD)) {
                    return 1;
                }
                if (((o).dataType).equals(DataType.CMD)) {
                    return -1;
                }
                if ((t.dataType).equals(DataType.PROD)
                        && ((o.dataType.equals(DataType.EVR)) || (o.dataType
                                .equals(DataType.LOG)))) {
                    return -1;
                }
                if ((t.dataType).equals(DataType.EVR) 
                        && ((o.dataType.equals(DataType.PROD)))) {
                    return 1;
                }
                if ((t.dataType).equals(DataType.EVR)
                        && ((o.dataType.equals(DataType.LOG)))) {
                    return -1;
                }
                if ((t.dataType).equals(DataType.LOG)
                        && ((o.dataType.equals(DataType.PROD)) || (o.dataType
                                .equals(DataType.EVR)))) {
                    return 1;
                }
                else {
                    return 0;
                }
            } else if ((tData).compareTo(oData) > 0) {
                return 1;
            } else {
                return -1;
            }
        } // Ends the compareTo()
    } // Ends the sortDataStructure Class


    /**
     * Constructor
     */
    public GetEverythingApp()
    {
        super(null, APP_NAME, "Log1553");
        
        dbChannelSampleFactory = appContext.getBean(IDbChannelSampleFactory.class);
        dbEvrFactory = appContext.getBean(IDbEvrFactory.class);
        dbCommandFactory = appContext.getBean(IDbCommandFactory.class);
        dbProductMetadataFactory = appContext.getBean(IDbProductMetadataFactory.class);
        dbLogFactory = appContext.getBean(IDbLogFactory.class);
        dbLog1553Factory = appContext.getBean(IDbLog1553Factory.class);
        this.sseFlag = appContext.getBean(SseContextFlag.class);

        suppressInfo();

        date = new AccurateDateTime();
        useDefaultDir = true; // Default is to use the default output directory
        outCSV = true; // Default is to use the CSV format
        ext = ".csv"; // Default is csv
        doERT = true; // Default is to sort by ERT
        noEHA = true;

        TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.WARN);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.IDbFetchApp#checkTimeType(jpl.gds.db.api.types.DatabaseTimeRange)
     */
    @Override
    public void checkTimeType(final DatabaseTimeRange range)
            throws ParseException {

        if (range.getTimeType().getValueAsInt() == DatabaseTimeType.EVENT_TIME_TYPE
                || range.getTimeType().getValueAsInt() == DatabaseTimeType.ERT_TYPE) {
            throw new ParseException("Time type must be "
                    + DatabaseTimeType.EVENT_TIME.toString() + " or "
                    + DatabaseTimeType.ERT.toString()
                    + " for this application.");
        }
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.IDbFetchApp#getDefaultTimeType()
     */
    @Override
    public DatabaseTimeType getDefaultTimeType() {

        return (DatabaseTimeType.ERT);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.IDbFetchApp#getFetch(boolean)
     */
    @Override
    public IDbSqlFetch getFetch(final boolean sqlStmtOnly) {
        // There is no fetch class for this app
        return null;
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.IDbFetchApp#getFetchParameters()
     */
    @Override
    public Object[] getFetchParameters() {
        return null; // There is no fetch class for this app
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.IDbFetchApp#getOrderByValues()
     */
    @Override
    public String[] getOrderByValues() {
        return new String [] { ERT, SCET };
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.AbstractDatabaseApp#getUsage()
     */
    @Override
    public String getUsage() {
        return (APP_NAME + " --" + ReservedOptions.TESTKEY_LONG_VALUE
                + " <number> " + DOUBLE_DASH + CHANNEL_IDS_LONG
                + " <string,string,...,string> |" + " --"
                + CHANNEL_ID_FILE_LONG + " <string> "
                + "[Session search options - " + "Not required]\n");
    }

    /**
     * Used to add a command to all five chill_get_* command line strings, with
     * no argument.
     * @param st The string that is to be added to the command lines.
     */
    public void addAppCommand(final String st) {
        cmdlineChan.add(st);
        cmdlineCmd.add(st);
        cmdlineEvr.add(st);
        cmdlineLog.add(st);
        cmdlineProd.add(st);
        cmdlineGe.add(st);
    }

    /**
     * Used to add a command to all five chill_get_* command line strings, with
     * an argument.
     * @param st1 The string that is the command line option to be added
     * @param st2 The string that is the argument to be added.
     */
    public void addAppCommand(final String st1, final String st2) {
        cmdlineChan.add(st1);
        cmdlineChan.add(st2);
        cmdlineCmd.add(st1);
        cmdlineCmd.add(st2);
        cmdlineEvr.add(st1);
        cmdlineEvr.add(st2);
        cmdlineLog.add(st1);
        cmdlineLog.add(st2);
        cmdlineProd.add(st1);
        cmdlineProd.add(st2);
        cmdlineGe.add(st1);
        cmdlineGe.add(st2);
    }

    /**
     * Additional configuration for the 1553 log application.
     * 
     * @param cmdline
     *            parsed CommandLine object
     * @throws ParseException
     */
    private void configure1553Log(final CommandLine cmdline)
            throws ParseException {
        // If a option to restrict the 1553 Log output is supplied on the
        // command line This helps limit the amount of log 1553 data to process:
        if (!(cmdline.hasOption(SYS_TIME_START_LONG)
                || cmdline.hasOption(SYS_TIME_END_LONG)
                || cmdline.hasOption(SCLK_START_LONG)
                || cmdline.hasOption(SCLK_END_LONG)
                || cmdline.hasOption(RT_LONG) || cmdline.hasOption(SA_LONG))) {
            trace.warn(DOUBLE_DASH
                    + SYS_TIME_START_LONG
                    + " or --"
                    + SYS_TIME_END_LONG
                    + " or --"
                    + SCLK_START_LONG
                    + " or "
                    + DOUBLE_DASH
                    + SCLK_END_LONG
                    + " or --"
                    + RT_LONG
                    + " or --"
                    + SA_LONG
                    + " needs to be supplied also if using 1553 logs in the report."
                    + "  1553 Logs will not be included in the report output!");
            has1553Logs = false;
        } else {
            has1553Logs = true;
            // The raw 1553 log file:
            raw1553File = cmdline.getOptionValue(FILE_1553_LONG);
            if (raw1553File == null) {
                throw new MissingArgumentException(DOUBLE_DASH + FILE_1553_LONG
                        + REQUIRES_VALUE);
            }
            raw1553File = raw1553File.trim();
            cmdlineGe.add(DOUBLE_DASH + FILE_1553_LONG);
            cmdlineGe.add(raw1553File);
            decodeFile = outputDirFile.getAbsolutePath() + "/"
                    + "chill_decode_1553_output.csv";
            cmdline1553.add("--decodeFile=" + decodeFile);
            cmdline1553.add(raw1553File);
            /*
             * Always outputting is csv format so that it is easy to parse
             */
            final String outFormat = "csv";
            cmdline1553.add(DOUBLE_DASH + AbstractFetchApp.OUTPUT_FORMAT_LONG + "="
                    + outFormat);
            if (cmdline.hasOption(RT_LONG)) {
                final String rts = cmdline.getOptionValue(RT_LONG);
                if (rts == null) {
                    throw new MissingArgumentException(DOUBLE_DASH + RT_LONG
                            + REQUIRES_VALUE);
                }
                cmdline1553.add(DOUBLE_DASH + RT_LONG + "=" + rts);
                cmdlineGe.add(DOUBLE_DASH + RT_LONG);
                cmdlineGe.add(rts);
            }
            if (cmdline.hasOption(SA_LONG)) {
                final String sa = cmdline.getOptionValue(SA_LONG);
                if (sa == null) {
                    throw new MissingArgumentException(DOUBLE_DASH + SA_LONG
                            + REQUIRES_VALUE);
                }
                cmdline1553.add(DOUBLE_DASH + SA_LONG + "=" + sa);
                cmdlineGe.add(DOUBLE_DASH + SA_LONG);
                cmdlineGe.add("sa");
            }
            if (cmdline.hasOption(SYS_TIME_START_LONG)) {
                final String time = cmdline.getOptionValue(SYS_TIME_START_LONG);
                if (time == null) {
                    throw new MissingArgumentException(DOUBLE_DASH
                            + SYS_TIME_START_LONG
                            + REQUIRES_VALUE);
                }
                cmdline1553.add(DOUBLE_DASH + SYS_TIME_START_LONG + "=" + time);
                cmdlineGe.add(DOUBLE_DASH + SYS_TIME_START_LONG);
                cmdlineGe.add(time);
            }
            if (cmdline.hasOption(SYS_TIME_END_LONG)) {
                final String time = cmdline.getOptionValue(SYS_TIME_END_LONG);
                if (time == null) {
                    throw new MissingArgumentException(DOUBLE_DASH + SYS_TIME_END_LONG
                            + REQUIRES_VALUE);
                }
                cmdline1553.add(DOUBLE_DASH + SYS_TIME_END_LONG + "=" + time);
                cmdlineGe.add(DOUBLE_DASH + SYS_TIME_END_LONG);
                cmdlineGe.add(time);
            }
            if (cmdline.hasOption(SCLK_START_LONG)) {
                final String time = cmdline.getOptionValue(SCLK_START_LONG);
                if (time == null) {
                    throw new MissingArgumentException(DOUBLE_DASH + SCLK_START_LONG
                            + REQUIRES_VALUE);
                }
                cmdline1553.add(DOUBLE_DASH + SCLK_START_LONG + "=" + time);
                cmdlineGe.add(DOUBLE_DASH + SCLK_START_LONG);
                cmdlineGe.add(time);
            }
            if (cmdline.hasOption(SCLK_END_LONG)) {
                final String time = cmdline.getOptionValue(SCLK_END_LONG);
                if (time == null) {
                    throw new MissingArgumentException(DOUBLE_DASH + SCLK_END_LONG
                            + REQUIRES_VALUE);
                }
                cmdline1553.add(DOUBLE_DASH + SCLK_END_LONG + "=" + time);
                cmdlineGe.add(DOUBLE_DASH + SCLK_END_LONG);
                cmdlineGe.add(time);
            }
        }
    }

    /**
     * This is the manner CGE configures its app. It must go through the 
     * command line it is passed and parse each option so that it can send them
     * to their relevant chill_get_* command lines and correctly configure CGE.
     * 
     * @param cmdline This is the command line passed to chill_get_everything.
     * @throws ParseException If it cannot parse the command line it throws this.
     */
    @Override
    public void configureApp(final CommandLine cmdline) throws ParseException {
        if (cmdline == null) {
            throw new IllegalArgumentException("Null input command line");
        }
        /*
         * First we add each apps path to their cmd line
         */
        final String gdsDirectory = GdsSystemProperties.getSystemProperty("GdsDirectory");
        final String geAppPath = gdsDirectory + BIN
                + GET_EVERYTHING_APP_COMMAND;
        final String sessionAppPath = gdsDirectory + BIN
                + SESSION_APP_COMMAND;
        final String chanAppPath = gdsDirectory + BIN + CHANNEL_APP_COMMAND;
        final String cmdAppPath = gdsDirectory + BIN + COMMAND_APP_COMMAND;
        final String evrAppPath = gdsDirectory + BIN + EVR_APP_COMMAND;
        final String logAppPath = gdsDirectory + BIN + LOG_APP_COMMAND;
        final String prodAppPath = gdsDirectory + BIN + PRODUCT_APP_COMMAND;
        final String log1553AppPath = gdsDirectory + BIN
                + LOG1553_APP_COMMAND;
        cmdline1553.add(log1553AppPath);
        cmdlineGe.add(geAppPath);
        cmdlineSession.add(sessionAppPath);
        cmdlineSession.add("-m");
        cmdlineChan.add(chanAppPath);
        cmdlineCmd.add(cmdAppPath);
        cmdlineEvr.add(evrAppPath);
        cmdlineLog.add(logAppPath);
        cmdlineProd.add(prodAppPath);

        if (cmdline.hasOption(SQL_STATEMENT_ONLY_LONG)) {
            sqlOnly = true;
        }
        /*
         * Now we parse each command line option, and add it to each applicable
         * app(s) individual command line string.
         */
        noEHA = cmdline.hasOption(NO_EHA_LONG);
        if (noEHA) {
            cmdlineGe.add(DOUBLE_DASH + NO_EHA_LONG);
        }
        noEVR = cmdline.hasOption(NO_EVR_LONG);
        if (noEVR) {
            cmdlineGe.add(DOUBLE_DASH + NO_EVR_LONG);
        }
        noCMD = cmdline.hasOption(NO_CMD_LONG);
        if (noCMD) {
            cmdlineGe.add(DOUBLE_DASH + NO_CMD_LONG);
        }
        noPROD = cmdline.hasOption(NO_PROD_LONG);
        if (noPROD) {
            cmdlineGe.add(DOUBLE_DASH + NO_PROD_LONG);
        }
        noLOG = cmdline.hasOption(NO_LOG_LONG);
        if (noLOG) {
            cmdlineGe.add(DOUBLE_DASH + NO_LOG_LONG);
        }
        no1553LOG = !cmdline.hasOption(FILE_1553_LONG);

        if (noEHA && noEVR && noCMD && noPROD && noLOG && no1553LOG) {
            throw new MissingArgumentException(
                    "Currently no output files will be produced.  You must allow for some output files."
                            + "  Note that SCET ordering does not produce 1553_LOG files.");
        } else {
            has1553Logs = false;
        }
        if (!noEHA) {
            // If a channel id file or channel id is supplied on the command
            // line
            // This helps limit the amount of channel data to process:
            if (cmdline.hasOption(CHANNEL_ID_FILE_SHORT)
                    || cmdline.hasOption(CHANNEL_IDS_SHORT)) {
                noEHA = false;
            }
            // Will still let the tool run if they don't supply these options,
            // but display a warning
            else {
                noEHA = true;
                trace.warn(SINGLE_DASH
                        + CHANNEL_ID_FILE_SHORT
                        + " or "
                        + SINGLE_DASH
                        + CHANNEL_IDS_SHORT
                        + " needs to be supplied also if using channels in the report."
                        + "  Channels will not be included in the report output!");
            }
        }
        /*
         * Option -a, --outputDir <string> Used internally to output files.
         */
        if (cmdline.hasOption(OUTPUT_DIR_SHORT)) {
            outputDir = cmdline.getOptionValue(OUTPUT_DIR_SHORT);
            if (outputDir == null) {
                throw new MissingArgumentException(SINGLE_DASH + OUTPUT_DIR_SHORT
                        + REQUIRES_VALUE);
            }
            outputDir = outputDir.trim();
            useDefaultDir = false;
            cmdlineGe.add(DOUBLE_DASH + OUTPUT_DIR_LONG);
            cmdlineGe.add(outputDir);
        }
        /*
         * -b, --beginTime <time>...-e, --endTime <time> The begin/end of the
         * time range for ERT
         */
        if (cmdline.hasOption(BEGIN_TIME_SHORT)) {
            final String bts = cmdline.getOptionValue(BEGIN_TIME_SHORT);
            if (bts == null) {
                throw new MissingArgumentException(SINGLE_DASH + BEGIN_TIME_SHORT
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + BEGIN_TIME_LONG, bts);
        }
        if (cmdline.hasOption(END_TIME_SHORT)) {
            final String ets = cmdline.getOptionValue(END_TIME_SHORT);
            if (ets == null) {
                throw new MissingArgumentException(SINGLE_DASH + END_TIME_SHORT
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + END_TIME_LONG, ets);
        }
        /*
         * Options -D, --fswVersionPattern <version> Used internally to output
         * files.
         */
        if (cmdline.hasOption(FSWVERSION_PATTERN_SHORT)) {
            final String opt = cmdline.getOptionValue(FSWVERSION_PATTERN_SHORT);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH
                        + FSWVERSION_PATTERN_SHORT
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + FSWVERSION_PATTERN_LONG, opt);
        }
        /*
         * Options -d, --debug
         */
        if (cmdline.hasOption(ReservedOptions.DEBUG_SHORT_VALUE)
                || cmdline.hasOption(ReservedOptions.DEBUG_LONG_VALUE)) {
            addAppCommand(SINGLE_DASH + ReservedOptions.DEBUG_SHORT_VALUE);
        }
        /*
         * Option --dbPwd <password> & --dbUser <username>
         */
        final String dbPwdOpt = ReservedOptions.DATABASE_PASSWORD.getLongOpt();
        if (cmdline.hasOption(dbPwdOpt)) {
            final String opt = cmdline.getOptionValue(dbPwdOpt);
            if (opt == null) {
                throw new MissingArgumentException(DOUBLE_DASH + dbPwdOpt
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + dbPwdOpt, opt);
            cmdlineSession.add(DOUBLE_DASH + dbPwdOpt);
            cmdlineSession.add(opt);
        }
        final String dbUserOpt = ReservedOptions.DATABASE_USERNAME.getLongOpt();
        if (cmdline.hasOption(dbUserOpt)) {
            final String opt = cmdline.getOptionValue(dbUserOpt);
            if (opt == null) {
                throw new MissingArgumentException(DOUBLE_DASH + dbUserOpt
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + dbUserOpt, opt);
            cmdlineSession.add(DOUBLE_DASH + dbUserOpt);
            cmdlineSession.add(opt);
        }
        /*
         * --doErt and --doScet We get how the user would like to sort the file.
         * Default is ERT You cannot put both options.
         */
        doERT = cmdline.hasOption(DO_ERT_LONG);
        if (doERT) {
            cmdlineGe.add(DOUBLE_DASH + DO_ERT_LONG);
        }
        doSCET = cmdline.hasOption(DO_SCET_LONG);
        if (doSCET) {
            cmdlineGe.add(DOUBLE_DASH + DO_SCET_LONG);
        }
        // They cannot enter both
        if (doERT && doSCET) {
            throw new ParseException(CANNOT_ENTER
                    + DO_ERT_LONG + AND_THE + DO_SCET_LONG
                    + COMMAND_LINE);
        }
        // If they enter neither, then the default is ERT
        if (!doERT && !doSCET) {
            doERT = true;
        }
        cmdlineChan.add("-m");
        cmdlineEvr.add("-m");
        cmdlineLog.add("-m");
        cmdlineCmd.add("-m");
        cmdlineProd.add("-m");
        cmdlineChan.add(SINGLE_DASH + ORDER_BY_SHORT);
        cmdlineEvr.add(SINGLE_DASH + ORDER_BY_SHORT);
        cmdlineLog.add(SINGLE_DASH + ORDER_BY_SHORT);
        cmdlineCmd.add(SINGLE_DASH + ORDER_BY_SHORT);
        cmdlineProd.add(SINGLE_DASH + ORDER_BY_SHORT);
        // We order Commands and Logs (ground data) by EventTime regardless,
        // but then the sorting algorithm to insert them is different.
        cmdlineCmd.add("EventTime");
        cmdlineLog.add("EventTime");
        if (doERT) {
            cmdlineChan.add(ERT);
            cmdlineEvr.add(ERT);
            cmdlineProd.add(ERT);
        } else if (doSCET) {
            cmdlineChan.add("SCLK");
            cmdlineEvr.add("SCLK");
            cmdlineProd.add("DVTSCLK");
        }
        /*
         * Option -E, --downlinkStreamId <stream>
         */
        if (cmdline.hasOption(ReservedOptions.DOWNLINKSTREAM_SHORT_VALUE)) {
            final String opt = cmdline
                    .getOptionValue(ReservedOptions.DOWNLINKSTREAM_SHORT_VALUE);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH
                        + ReservedOptions.DOWNLINKSTREAM_SHORT_VALUE
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + ReservedOptions.DOWNLINKSTREAM_LONG_VALUE, opt);
        }
        /*
         * Options -f, --partialOnly and -g, --completeOnly
         */
        final boolean partialOnlyS = cmdline.hasOption(PRODUCT_PARTIAL_SHORT);
        final boolean partialOnlyL = cmdline.hasOption(PRODUCT_PARTIAL_LONG);
        final boolean completeOnlyS = cmdline.hasOption(PRODUCT_COMPLETE_SHORT);
        final boolean completeOnlyL = cmdline.hasOption(PRODUCT_COMPLETE_LONG);
        if (partialOnlyS && completeOnlyS) {
            throw new ParseException(CANNOT_ENTER
                    + PRODUCT_PARTIAL_SHORT + AND_THE
                    + PRODUCT_COMPLETE_SHORT + COMMAND_LINE);
        }
        if (partialOnlyS && completeOnlyL) {
            throw new ParseException(CANNOT_ENTER
                    + PRODUCT_PARTIAL_SHORT + AND_THE
                    + PRODUCT_COMPLETE_LONG + COMMAND_LINE);
        }
        if (partialOnlyL && completeOnlyS) {
            throw new ParseException(CANNOT_ENTER
                    + PRODUCT_PARTIAL_LONG + AND_THE
                    + PRODUCT_COMPLETE_SHORT + COMMAND_LINE);
        }
        if (partialOnlyL && completeOnlyL) {
            throw new ParseException(CANNOT_ENTER
                    + PRODUCT_PARTIAL_LONG + AND_THE
                    + PRODUCT_COMPLETE_LONG + COMMAND_LINE);
        }
        if (partialOnlyS || partialOnlyL) {
            cmdlineProd.add(DOUBLE_DASH + PRODUCT_PARTIAL_LONG);
            cmdlineGe.add(DOUBLE_DASH + PRODUCT_PARTIAL_LONG);
        } else if (completeOnlyS || completeOnlyL) {
            cmdlineProd.add(DOUBLE_DASH + PRODUCT_COMPLETE_LONG);
            cmdlineGe.add(DOUBLE_DASH + PRODUCT_COMPLETE_LONG);
        }
        /*
         * Option -j, --databaseHost <host>
         */
        if (cmdline.hasOption(ReservedOptions.DATABASE_HOST_SHORT_VALUE)) {
            final String opt = cmdline
                    .getOptionValue(ReservedOptions.DATABASE_HOST_SHORT_VALUE);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH
                        + ReservedOptions.DATABASE_HOST_SHORT_VALUE
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + ReservedOptions.DATABASE_HOST_LONG_VALUE, opt);
            cmdlineSession.add(DOUBLE_DASH + ReservedOptions.DATABASE_HOST_LONG_VALUE);
            cmdlineSession.add(opt);
        }
        /*
         * Option -K, --testKey <id>
         */
        if (cmdline.hasOption(ReservedOptions.TESTKEY_SHORT_VALUE)) {
            final String opt = cmdline
                    .getOptionValue(ReservedOptions.TESTKEY_SHORT_VALUE);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH
                        + ReservedOptions.TESTKEY_SHORT_VALUE
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + ReservedOptions.TESTKEY_LONG_VALUE, opt);
        }

        /*
         * Option --stringId
         */
        String stringIds = null;
        if (cmdline.hasOption(DssVcidOptions.STRING_ID_OPTION_LONG)) {
        	stringIds = cmdline.getOptionValue(DssVcidOptions.STRING_ID_OPTION_LONG);
        	cmdlineChan.add(DOUBLE_DASH + DssVcidOptions.STRING_ID_OPTION_LONG);
        	cmdlineChan.add(stringIds);
        	cmdlineGe.add(DOUBLE_DASH + DssVcidOptions.STRING_ID_OPTION_LONG);
        	cmdlineGe.add(stringIds);
        	cmdlineEvr.add(DOUBLE_DASH + DssVcidOptions.STRING_ID_OPTION_LONG);
        	cmdlineEvr.add(stringIds);
        	cmdlineProd.add(DOUBLE_DASH + DssVcidOptions.STRING_ID_OPTION_LONG);
        	cmdlineProd.add(stringIds);        
        }
        /*
         * Option --evrTypes
         */
        String evrTypes = null;
        if (cmdline.hasOption(EvrFetchApp.EVR_TYPES_LONG)) {
            evrTypes = cmdline.getOptionValue(EvrFetchApp.EVR_TYPES_LONG);
            cmdlineEvr.add(DOUBLE_DASH + EvrFetchApp.EVR_TYPES_LONG);
            cmdlineEvr.add(evrTypes);
            cmdlineGe.add(DOUBLE_DASH + EvrFetchApp.EVR_TYPES_LONG);
            cmdlineGe.add(evrTypes);
        }

        /*
         * Option --channelTypes
         */
        String channelTypes = null;
        if (cmdline.hasOption(ChannelValueFetchApp.CHANNEL_TYPES_LONG)) {
            channelTypes = cmdline
                    .getOptionValue(ChannelValueFetchApp.CHANNEL_TYPES_LONG);
            cmdlineChan.add(DOUBLE_DASH + ChannelValueFetchApp.CHANNEL_TYPES_LONG);
            cmdlineChan.add(channelTypes);
            cmdlineGe.add(DOUBLE_DASH + ChannelValueFetchApp.CHANNEL_TYPES_LONG);
            cmdlineGe.add(channelTypes);
        }

        /*
         * Option --changesOnly
         */
        if (cmdline.hasOption(CHANGE_VALUES_LONG)) {
            cmdlineChan.add(DOUBLE_DASH + CHANGE_VALUES_LONG);
            cmdlineGe.add(DOUBLE_DASH + CHANGE_VALUES_LONG);
        }

        /*
         * Options -L, --testDescriptionPattern <description>
         */
        if (cmdline.hasOption(TESTDESCRIPTION_PATTERN_SHORT)) {
            final String opt = cmdline.getOptionValue(TESTDESCRIPTION_PATTERN_SHORT);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH
                        + TESTDESCRIPTION_PATTERN_SHORT
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + TESTDESCRIPTION_PATTERN_LONG, opt);
        }
        /*
         * Options -M, --testNamePattern <name>
         */
        if (cmdline.hasOption(TESTNAME_PATTERN_SHORT)) {
            final String opt = cmdline.getOptionValue(TESTNAME_PATTERN_SHORT);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH + TESTNAME_PATTERN_SHORT
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + TESTNAME_PATTERN_LONG, opt);
        }

        /*
         * Options -n, --databasePort <port>
         */
        if (cmdline.hasOption(ReservedOptions.DATABASE_PORT_SHORT_VALUE)) {
            final String opt = cmdline
                    .getOptionValue(ReservedOptions.DATABASE_PORT_SHORT_VALUE);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH
                        + ReservedOptions.DATABASE_PORT_SHORT_VALUE
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + ReservedOptions.DATABASE_PORT_LONG_VALUE, opt);
            cmdlineSession.add(DOUBLE_DASH + ReservedOptions.DATABASE_PORT_LONG_VALUE);
            cmdlineSession.add(opt);
        }
        /*
         * Options -O, --testHostPattern <hostname>
         */
        if (cmdline.hasOption(TESTHOSTNAME_PATTERN_LONG)) {
            final String opt = cmdline.getOptionValue(TESTHOSTNAME_PATTERN_LONG);
            if (opt == null) {
                throw new MissingArgumentException(DOUBLE_DASH
                        + TESTHOSTNAME_PATTERN_LONG
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + TESTHOSTNAME_PATTERN_LONG, opt);
            cmdlineSession.add(DOUBLE_DASH + TESTHOSTNAME_PATTERN_LONG);
            cmdlineSession.add(opt);
        }
        /*
         * Option -o, --outputFormat <format> Used to tell get* cmds how to
         * output files. Default is csv
         */
        String typeOpt = "csv"; // Use the default...csv...
        String type = null;
        if (cmdline.hasOption(OUTPUT_FORMAT_SHORT)) {
            type = StringUtil.safeTrim(cmdline
                    .getOptionValue(OUTPUT_FORMAT_SHORT));
            if (type == null) {
                throw new MissingArgumentException(SINGLE_DASH + OUTPUT_FORMAT_SHORT
                        + REQUIRES_VALUE);
            }
        } else if (cmdline.hasOption(OUTPUT_FORMAT_LONG)) {
            type = StringUtil.safeTrim(cmdline
                    .getOptionValue(OUTPUT_FORMAT_LONG));
            if (type == null) {
                throw new MissingArgumentException(DOUBLE_DASH + OUTPUT_FORMAT_LONG
                        + REQUIRES_VALUE);
            }
        }
        if (type != null) // Something was passed for output format
        {
            outCSV = false;
            if (type.equalsIgnoreCase("csv")) {
                // csv is the chill_get default, so do nothing.
                outCSV = true;
                cmdlineGe.add(DOUBLE_DASH + OUTPUT_FORMAT_LONG);
                cmdlineGe.add("csv");
            } else if (type.equalsIgnoreCase("text")) {
                typeOpt = "sr_text";
                ext = ".txt";
                outSRTEXT = true;
                addAppCommand(DOUBLE_DASH + OUTPUT_FORMAT_LONG, typeOpt);
            } else if (type.equalsIgnoreCase("excel")) {
                // for excel the individual gets return csv, so do nothing
                ext = ".csv";
                outEXCEL = true;
                cmdlineGe.add(DOUBLE_DASH + OUTPUT_FORMAT_LONG);
                cmdlineGe.add("excel");
            } else if (type.equalsIgnoreCase("sr_csv")) {
                typeOpt = "sr_csv";
                ext = "_sr.csv";
                outSRCSV = true;
                addAppCommand(DOUBLE_DASH + OUTPUT_FORMAT_LONG, typeOpt);
            } else {
                throw new ParseException(
                        "You have entered an invalid -o or --outputFormat command line option. The options are csv, sr_csv, text, or excel.");
            }
        } 
        /*
         * Options -P, --testUserPattern <username>
         */
        if (cmdline.hasOption(TESTUSER_PATTERN_SHORT)) {
            final String opt = cmdline.getOptionValue(TESTUSER_PATTERN_SHORT);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH + TESTUSER_PATTERN_SHORT
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + TESTUSER_PATTERN_LONG, opt);
        }
        /*
         * Options -p, --channelIdFile <string> OR -z, --channelIds <string>
         * (copied directly from the chanvalApp.configureApp())
         */
        String chanIdString = null;
        if (cmdline.hasOption(CHANNEL_IDS_SHORT)) {
            chanIdString = cmdline.getOptionValue(CHANNEL_IDS_SHORT);
            if (chanIdString == null) {
                throw new MissingArgumentException(SINGLE_DASH + CHANNEL_IDS_SHORT
                        + REQUIRES_VALUE);
            } else
            // chanIdString!=null
            {
                String[] channelIds = new String[0];
                chanIdString = chanIdString.trim();
                channelIds = chanIdString.split(",{1}");
                for (int i = 0; i < channelIds.length; i++) {
                    channelIds[i] = channelIds[i].trim();
                    if (ChannelIdUtility.isChanIdString(channelIds[i]) == false) {
                        throw new ParseException(
                                "The input channel ID "
                                        + channelIds[i]
                                        + " is not a valid channel ID."
                                        + "  Channel IDs should follow the regular expression "
                                        + ChannelIdUtility.CHANNEL_ID_REGEX);
                    }
                }
                // If it gets here, there were channels listed, and they were
                // correct.
                cmdlineChan.add(DOUBLE_DASH + CHANNEL_IDS_LONG);
                cmdlineChan.add(chanIdString);
                cmdlineGe.add(DOUBLE_DASH + CHANNEL_IDS_LONG);
                cmdlineGe.add(chanIdString);
            }
        }
        if (cmdline.hasOption(CHANNEL_ID_FILE_SHORT)) {
            String channelIdFile = cmdline
                    .getOptionValue(CHANNEL_ID_FILE_SHORT);
            if (channelIdFile == null) {
                throw new MissingArgumentException(SINGLE_DASH + CHANNEL_ID_FILE_SHORT
                        + REQUIRES_VALUE);
            } else
            // chanIdFile not null, I don't check it I just pass it.
            {
                channelIdFile = channelIdFile.trim();
                cmdlineChan.add(DOUBLE_DASH + CHANNEL_ID_FILE_LONG);
                cmdlineChan.add(channelIdFile);
                cmdlineGe.add(DOUBLE_DASH + CHANNEL_ID_FILE_LONG);
                cmdlineGe.add(channelIdFile);
            }
        }
        /*
         * Options -Q, --testTypePattern <type>
         */
        if (cmdline.hasOption(TESTTYPE_PATTERN_SHORT)) {
            final String opt = cmdline.getOptionValue(TESTTYPE_PATTERN_SHORT);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH + TESTTYPE_PATTERN_SHORT
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + TESTTYPE_PATTERN_LONG, opt);
        }
        /*
         * Options --dssId <int,...>
         */
        if (cmdline.hasOption(DssVcidOptions.DSS_ID_OPTION_LONG)) {
            final String opt = cmdline
                    .getOptionValue(DssVcidOptions.DSS_ID_OPTION_LONG);
            if (opt == null) {
                throw new MissingArgumentException(DOUBLE_DASH
                        + DssVcidOptions.DSS_ID_OPTION_LONG
                        + REQUIRES_VALUE);
            }
            cmdlineChan.add(DOUBLE_DASH + DssVcidOptions.DSS_ID_OPTION_LONG);
            cmdlineChan.add(opt);
            cmdlineEvr.add(DOUBLE_DASH + DssVcidOptions.DSS_ID_OPTION_LONG);
            cmdlineEvr.add(opt);


            cmdlineGe.add(DOUBLE_DASH + DssVcidOptions.DSS_ID_OPTION_LONG);
            cmdlineGe.add(opt);
        }

        
        /*
         * Options --vcid <int,...>
         */
        if (cmdline.hasOption(DssVcidOptions.VCID_OPTION_LONG)) {
            final String opt = cmdline
                    .getOptionValue(DssVcidOptions.VCID_OPTION_LONG);
            if (opt == null) {
                throw new MissingArgumentException(DOUBLE_DASH
                        + DssVcidOptions.VCID_OPTION_LONG
                        + REQUIRES_VALUE);
            }
            cmdlineChan.add(DOUBLE_DASH + DssVcidOptions.VCID_OPTION_LONG);
            cmdlineChan.add(opt);
            cmdlineEvr.add(DOUBLE_DASH + DssVcidOptions.VCID_OPTION_LONG);
            cmdlineEvr.add(opt);
            cmdlineProd.add(DOUBLE_DASH + DssVcidOptions.VCID_OPTION_LONG);
            cmdlineProd.add(opt);
            cmdlineGe.add(DOUBLE_DASH + DssVcidOptions.VCID_OPTION_LONG);
            cmdlineGe.add(opt);
        }
        /*
         * Options --sqlStatementOnly
         */
        if (cmdline.hasOption(SQL_STATEMENT_ONLY_LONG)) {
            addAppCommand(DOUBLE_DASH + SQL_STATEMENT_ONLY_LONG);
        }
        /*
         * Options -v, --version
         */
        if (cmdline.hasOption(ReservedOptions.VERSION_SHORT_VALUE)) {
            addAppCommand(DOUBLE_DASH + ReservedOptions.VERSION_LONG_VALUE);
        }
        /*
         * Options -W, --sseVersionPattern <version>
         */
        if (cmdline.hasOption(ReservedOptions.SSEVERSION_SHORT_VALUE)) {
            final String opt = cmdline
                    .getOptionValue(ReservedOptions.SSEVERSION_SHORT_VALUE);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH
                        + ReservedOptions.SSEVERSION_SHORT_VALUE
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + ReservedOptions.SSEVERSION_LONG_VALUE, opt);
        }
        /*
         * Options -w, --fromTestStart <time> & -x, --toTestStart <time>
         */
        if (cmdline.hasOption(TEST_START_LOWER_SHORT)) {
            final String opt = cmdline.getOptionValue(TEST_START_LOWER_SHORT);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH + TEST_START_LOWER_SHORT
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + TEST_START_LOWER_LONG, opt);
        }
        if (cmdline.hasOption(TEST_START_UPPER_SHORT)) {
            final String opt = cmdline.getOptionValue(TEST_START_UPPER_SHORT);
            if (opt == null) {
                throw new MissingArgumentException(SINGLE_DASH + TEST_START_UPPER_SHORT
                        + REQUIRES_VALUE);
            }
            addAppCommand(DOUBLE_DASH + TEST_START_UPPER_LONG, opt);
        }
        // Set up output directory:
        setupDirectory();
        // 1553 Logs:
        // if they didn't say NOT to do 1553 logs, and the output format is
        // not session report format, we need to configure the 1553 logs.
        if (!no1553LOG && !outSRCSV) {
            // We start has1553Logs to false until we prove otherwise.
            has1553Logs = false;
            configure1553Log(cmdline);
        }

        /* SHOULD WE KEEP THIS ????*/

        //  Removed this code. Now that factories
        // are established for all
        // decendents of IDbQueryable.
        // if (!noPROD) {
        // // Get a product object of the appropriate type:
        // IProductBuilderObjectFactory instanceFactory = null;
        // try {
        // instanceFactory =
        // appContext.getBean(IProductBuilderObjectFactory.class);
        // productDQ = instanceFactory.createProductMetadata();
        // } catch (final Exception e) {
        // // e.printStackTrace();
        // setExitCode(1);
        // throw new ChillGetEverythingException("Application error: "
        // + (e.getMessage() == null ? e.toString()
        // : e.getMessage()));
        // }
        // }

        // Products need to be told that the time type is ERT
        // We can do that even if times are not supplied
        cmdlineProd.add("-t ERT");
    }

    /**
     * This takes the command line as a List and parses it and turns it into a
     * string. It also checks and if the command line contains a db password, it
     * turns it into ***
     */
    private String getCommandString(final List<String> cmd) {
        // First we need to go through the list and turn the db password into
        // *** if it exists.
        int index = -1;
        index = cmd.indexOf(DOUBLE_DASH
                + ReservedOptions.DATABASE_PASSWORD.getLongOpt());
        if (index != -1) {
            // This means it has the db password.
            cmd.set(index + 1, "***");
        }
        // Now we take that command and turn it into a string
        final StringBuilder sb = new StringBuilder();
        for (final String a : cmd) {
            sb.append(a);
            sb.append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * This initializes the Array List and data for the data type and command
     * line being processed. It is used for the Initial starting of EHA, EVR,
     * LOG, CMD, PROD, 1553LOG.
     * 
     * @throws ChillGetEverythingException
     */
    private void initializeDataMapProcesses(final DataType dt, final List<String> cmd,
            final String fext) throws ChillGetEverythingException {
        /*
         * Used to create each app's command line version as a string
         */
        final String editedCommand = getCommandString(cmd);
        final ArrayList<Object> data = new ArrayList<Object>(20);
        try {
            data.ensureCapacity(20);
            // 0 DONT_GET, starts as false because we are only adding it if they
            // wanted to get it.
            data.add(DONT_GET, false);
            data.add(CMD_LINE_STR, editedCommand);
            data.add(CMD_LINE_LIST, cmd);
            data.add(EMPTY, true);
            data.add(LINE, "");
            data.add(PROCESSED_HEADER, false);
            data.add(ERT_POSN, 0);
            data.add(SCLK_POSN, 0);
            data.add(RT_POSN, 0);
            BufferedReader inBuff;
            String header = "";
            final ProcessBuilder procBuilder = new ProcessBuilder();
            procBuilder.command(cmd);
            procBuilder.redirectErrorStream(true);
            System.out.println("Running: " + editedCommand);
            final Process p = procBuilder.start();
            final String filename = outputDirFile.getPath() + fext;
            final BufferedWriter outBuff = new BufferedWriter(
                    new FileWriter(filename));
            if (dt.equals(DataType.LOG_1553)) {
                // this means it is a 1553 Log.
                final BufferedReader errBuff = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                final String dat = errBuff.readLine();
                errBuff.close();
                if (dat != null) {
                    // there is an error from the decode
                    throw new ChillGetEverythingException(
                            "chill_decode_1553 returned an error: " + dat);
                }
                try {
                    p.waitFor();
                } catch (final InterruptedException e) {
                    throw new ChillGetEverythingException(
                            "Error waiting for a process.", e);
                }
                // Now we set our inbuff to read from the created file.
                inBuff = new BufferedReader(new FileReader(decodeFile));
                if (outCSV || outEXCEL) {
                    header = csv1553Header;
                }
                if (outSRTEXT) {
                    header = text1553Header;
                }
            } else {
                // We need to read the input buffer to the file.
                final BufferedReader inStreamBuff = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                String line = inStreamBuff.readLine();
                while (line != null) {
                    // while the input stream has data, write it to it's file...
                    outBuff.write(line);
                    outBuff.newLine();
                    line = inStreamBuff.readLine();
                }
                // finished redirecting output to a file. Close it and get input
                // buffer.
                outBuff.flush();
                outBuff.close();
                inStreamBuff.close();
                inBuff = new BufferedReader(new FileReader(filename));
            }
            data.add(INPUT_BUFF, inBuff);
            data.add(OUTPUT_BUFF, outBuff);
            data.add(PROCESS_BUILDER, procBuilder);
            data.add(PROCESS, p);
            data.add(FILENAME, filename);
            data.add(HEADER, header);
            data.add(LEVEL_POSN, 0);
            data.add(EHA_ID_POSN, 0);
            data.add(SESSION_POSN, 0);
            DataMap.put(dt, data);
        } catch (final IOException e) {
            // e.printStackTrace();
            trace.warn("Error Initializing Process for data type: "
                    + dt.toString() + ". Error: "
                    + (e.getMessage() == null ? e.toString() : e.getMessage())
                    + "\nThe command line ran was: " + data.get(CMD_LINE_STR));
            throw new ChillGetEverythingException(
                    "Error Initializing Process for data type: "
                            + dt.toString()
                            + ". Error: "
                            + (e.getMessage() == null ? e.toString()
                                    : e.getMessage())
                            + "\nThe command line ran was: "
                            + data.get(CMD_LINE_STR), e);
        }
    }

    /**
     * This adds the evr level/output buffer/count for the EVR level line being
     * processed. It is used for the Initial starting of any EVR level that has
     * yet to be processed yet.
     * @param level the EVR level/severity string
     * 
     * @throws ChillGetEverythingException
     */
    private void initializeEvrMap(final String level)
            throws ChillGetEverythingException {
        try {
            final String filename = outputDirFile.getPath() + "/evr_"
                    + level.replaceAll("\"", "") + ext;
            BufferedWriter outBuff;
            outBuff = new BufferedWriter(new FileWriter(filename));
            final ArrayList<Object> evrData = DataMap.get(DataType.EVR);
            final String header = (String) evrData.get(HEADER);
            final ArrayList<Object> data = new ArrayList<Object>(3);
            data.add(EVR_OUT_BUFF, outBuff);
            data.add(EVR_COUNT, 1);
            EvrLevelMap.put(level, data);
            totalEvrs = totalEvrs + 1;
            outBuff.write(header);
            outBuff.newLine();
        } catch (final IOException e) {
            throw new ChillGetEverythingException(
                    "Error Initializing EVR Map: "
                            + (e.getMessage() == null ? e.toString()
                                    : e.getMessage()), e);
        }
    }

    /**
     * This returns the buffered writer for the evr file of the level being
     * passed
     * 
     * @param level
     *            - the level of the Evrs we want the BW for.
     * @return the BufferedWriter for this level evr.
     */
    private BufferedWriter getEvrLevelOutputBuffer(final String level) {
        final ArrayList<Object> data = EvrLevelMap.get(level);
        return (BufferedWriter) data.get(EVR_OUT_BUFF);
    }

    /**
     * This allows us to increase the count for the EVR currently being
     * processed with level of level.
     * 
     * @param level
     *            - the level of the EVR being processed.
     */
    private void increaseEvrLevelCount(final String level) {
        final ArrayList<Object> data = EvrLevelMap.get(level);
        final int count = (Integer) data.get(EVR_COUNT);
        data.set(EVR_COUNT, count + 1);
        totalEvrs = totalEvrs + 1;
    }

    /**
     * This takes an EVR line and processes it. This means if it isn't in our
     * EvrLevelMap we add it. If it is, then we simply increase its count.
     * Either way we print it to its new EvrLevel file.
     * 
     * @param line
     * @throws ChillGetEverythingException
     */
    private void processEVR(final String line) throws ChillGetEverythingException {
        String level = processLine(line, DataType.EVR, ProcessType.LEVEL)
                .trim();
        if (level.equals("\"\"") || level.equals("") || level.equals("\"")
                || (level == null) || (level.length() == 0)) {
            level = "NO_LEVEL";
        }
        if (EvrLevelMap.containsKey(level)) {
            // All we have to do is increase the count...
            increaseEvrLevelCount(level);
        } else {
            // we need to add this level to the map
            initializeEvrMap(level);
        }
        final BufferedWriter bw = getEvrLevelOutputBuffer(level);
        // Now regardless of if it was new or not, we print it.
        try {
            bw.write(line);
            bw.newLine();
        } catch (final IOException e) {
            throw new ChillGetEverythingException("Error writing to evr file.",
                    e);
        }
    }

    /**
     * This processes a channel by storing it's id, name and count to the EhaMap
     * 
     * @param line
     * @throws ChillGetEverythingException
     */
    private void processEHA(final String line) throws ChillGetEverythingException {
        String idName = processLine(line, DataType.EHA, ProcessType.EHA);
        if (idName == null) {
            idName = "NO_ID NO_NAME";
        }
        if (EhaMap.containsKey(idName)) {
            int count = EhaMap.get(idName);
            EhaMap.put(idName, ++count);
        } else {
            EhaMap.put(idName, 1);
        }
        ++totalEha;
    }

    /**
     * This processes a log by storing it's severity and count to the
     * LogLevelMap
     * 
     * @param line
     * @throws ChillGetEverythingException
     */
    private void processLOG(final String line) throws ChillGetEverythingException {
        String level = processLine(line, DataType.LOG, ProcessType.LEVEL)
                .trim();
        if (level.equals("\"\"") || (level == null) || (level.length() == 0)) {
            level = "NO_LEVEL";
        }
        if (LogLevelMap.containsKey(level)) {
            // All we have to do is increase the count...
            int count = LogLevelMap.get(level);
            LogLevelMap.put(level, ++count);
        } else {
            LogLevelMap.put(level, 1);
        }
        totalLogs = totalLogs + 1;
    }

    /**
     * This processes a 1553 log by processing it's passed csv string and
     * returning the newly processed csv or sr_csv string (depending on the
     * user's choice)
     * 
     * @param line
     * @param header
     */
    private String process1553Log(final String line) {
        final IDbLog1553Updater dq = dbLog1553Factory.createQueryableUpdater();
        // Parse the data line and populate dq fields:
        dq.parseCsvCD1553(line);
        if (outCSV || outEXCEL) {
            return dq.toCsv(csvColumns);
        } else if (outSRTEXT || outTEXT) {
            final String NO_DATA = " ";
            final String NO_DATA_SCET = "-----------------------"; // 23 dashes
            final int DEFAULT_WIDTH = 30;
            final int SMALL_WIDTH = 10;
            final int LARGE_WIDTH = 50;
            // Sets up the data map utilizing data from IDbQueryable.
            final Map<String, String> dqMap = dq.getFileData(NO_DATA);
            final Map<String, Pair<String, Integer>> widthMap = new LinkedHashMap<String, Pair<String, Integer>>();
            widthMap.put("SYSTEM_TIME",
                    new Pair<String, Integer>(dqMap.get("sysTime"),
                            DEFAULT_WIDTH));
            widthMap.put(SCET, new Pair<String, Integer>(NO_DATA_SCET,
                    DEFAULT_WIDTH));
            widthMap.put("SCLK", new Pair<String, Integer>(dqMap.get("sclk"),
                    DEFAULT_WIDTH));
            widthMap.put("BUS", new Pair<String, Integer>(dqMap.get("bus"),
                    DEFAULT_WIDTH));
            widthMap.put("REMOTE_TERMINAL",
                    new Pair<String, Integer>(dqMap.get("remoteTerminal"),
                            DEFAULT_WIDTH));
            widthMap.put("SUB_ADDRESS",
                    new Pair<String, Integer>(dqMap.get("subAddress"),
                            LARGE_WIDTH));
            widthMap.put(
                    "TR_STATUS",
                    new Pair<String, Integer>(dqMap
                            .get("transmitReceiveStatus"), SMALL_WIDTH));
            widthMap.put("DATA", new Pair<String, Integer>(dqMap.get("data"),
                    DEFAULT_WIDTH));

            final StringBuilder sb = new StringBuilder();
            final SprintfFormat format = new SprintfFormat(
                                                           appContext.getBean(IContextIdentification.class)
                                                                     .getSpacecraftId());
            if (widthMap != null) {
                for (final Entry<String, Pair<String, Integer>> entry : widthMap
                        .entrySet()) {
                    final String data = entry.getValue().getOne();
                    sb.append(format.anCsprintf("%-"
                            + entry.getValue().getTwo() + "s\t", data));
                }
                return sb.toString();
            }
        }
        return null;
    }

    /**
     * This is used to close each file.
     */
    private void closeFiles() throws ChillGetEverythingException {
        try {
            // Close all the files the user ASKED to process
            if (processedData) {
                outAll.close();
                if (outEXCEL) {
                    // Prints the final excel sorted file
                    outFormatterExcel.printFormatterSpecificFile();
                }
            }
            for (final Entry<DataType, ArrayList<Object>> entry : DataMap.entrySet()) {
                final ArrayList<Object> data = entry.getValue();
                final BufferedWriter bw = (BufferedWriter) data.get(OUTPUT_BUFF);
                bw.close();
                if ((Boolean) data.get(PROCESSED_HEADER) == false) {
                    // this means it didn't actually RETURN data from the get.
                    // delete it...
                    final boolean ok = new File((String) data.get(FILENAME)).delete();
                    if (!ok) {
                    	// do nothing
                    }
                }
            }
            for (final Entry<String, ArrayList<Object>> entry : EvrLevelMap
                    .entrySet()) {
                final ArrayList<Object> data = entry.getValue();
                final BufferedWriter bw = (BufferedWriter) data.get(EVR_OUT_BUFF);
                bw.close();
            }
            if (!no1553LOG && !outSRCSV && doERT && !doSCET) {
                final boolean ok = new File((decodeFile)).delete();
                if (!ok) {
                	// do nothing
                }
            }
        } catch (final IOException e) {
            throw new ChillGetEverythingException(
                    "There was an error closing the files: "
                            + (e.getMessage() == null ? e.toString()
                                    : e.getMessage()), e);
        }
    }

    /**
     * This is used to retrieve a data types command line in string format.
     * 
     * @param dt
     * @return String - the data types command line used.
     */
    private String getCmdLine(final DataType dt) {
        final ArrayList<Object> data = DataMap.get(dt);
        return (String) data.get(CMD_LINE_STR);
    }

    /**
     * This is used to set the ERT, SCLK and RT Positions for each datatype
     */
    private void setDataMapPositions(final DataType dt, final int ertPost, final int sclkPost,
            final int rtPost, final int evrPost, final int ehaIdPost, final int sessPost) {
        final ArrayList<Object> data = DataMap.get(dt);
        data.set(ERT_POSN, ertPost);
        data.set(SCLK_POSN, sclkPost);
        data.set(RT_POSN, rtPost);
        data.set(LEVEL_POSN, evrPost);
        data.set(EHA_ID_POSN, ehaIdPost);
        data.set(SESSION_POSN, sessPost);
        DataMap.put(dt, data);
    }

    /**
     * This is used to retrieve the data position for the Data type and process
     * type being passed
     * 
     * @param dt
     * @param pt
     * @return
     */
    private int getDataPosition(final DataType dt, final ProcessType pt) {
        final ArrayList<Object> data = DataMap.get(dt);
        int location = 0;
        switch (pt) {
        case ERT:
            location = ERT_POSN;
            break;
        case SCLK:
            location = SCLK_POSN;
            break;
        case REALTIME:
            location = RT_POSN;
            break;
        case LEVEL:
            location = LEVEL_POSN;
            break;
        case EHA:
            location = EHA_ID_POSN;
            break;
        case SESSION:
            location = SESSION_POSN;
            break;
        default:
            break;
        }
        return (Integer) data.get(location);
    }

    /**
     * This tells us if the data file for a data type dt is empty yet (from the
     * data map).
     * 
     * @param dt
     * @return
     */
    private boolean getDataFileEmpty(final DataType dt) {
        final ArrayList<Object> data = DataMap.get(dt);
        return (Boolean) data.get(EMPTY);
    }

    /**
     * This is used to print both the summary information to the console and
     * summary file. This is copied from the original chill_get_everything by
     * Ghorang.
     * 
     * @throws ChillGetEverythingException
     */
    private void printSummaryFile() throws ChillGetEverythingException {
        try {
            getSessionInfo();
            PrintWriter consoleWriter = null;
            PrintWriter summaryFile = null;
            final String NOT_USED = "User selected not to display this in the report";
            final List<String> dataList = new ArrayList<String>();
            summaryFile = new PrintWriter(outputDirFile.getPath() + "/"
                    + "summary.txt");
            consoleWriter = new PrintWriter(System.out, false);
            consoleWriter
                    .write("=============================================================================================\n");
            dataList.add("SUMMARY FILE\n\n" + "Date: "
                    + date.getFormattedErt(true) + "\n\n");
            dataList.add("Output Directory is: "
                    + outputDirFile.getAbsolutePath() + "\n\n");
            dataList.add("Dictionaries are in:\n");
            if (fswList != null && !noSession) {
                final int length = fswList.size();
                for (int k = 0; k < length; k++) {
                    dataList.add(fswList.get(k) + "\n");
                }
            } else {
                dataList.add("No fsw directories/fsw versions were found \n");
            }
            dataList.add("\nSession Ids are:\n");
            if (!noSession) {
                for (final String id : sessionIdList) {
                    dataList.add(id + "\n");
                }
            } else {
                dataList.add("No session ids were found \n");
            }
            // Commands count:
            dataList.add("\nNumber in query:\n" + "    Commands = "
                    + (noCMD ? NOT_USED : totalCmds) + "\n");
            // Product count:
            dataList.add("    Products = " + (noPROD ? NOT_USED : totalProds)
                    + "\n");
            // Log counts:
            dataList.add("    Logs = " + (noLOG ? NOT_USED : totalLogs) + "\n");
            for (final Entry<String, Integer> entry : LogLevelMap.entrySet()) {
                dataList.add("       LOG "
                        + entry.getKey().toUpperCase().replaceAll("\"", "")
                        + " = " + entry.getValue() + "\n");
            }
            // Evr counts:
            dataList.add("    Evrs = " + (noEVR ? NOT_USED : totalEvrs) + "\n");
            for (final Entry<String, ArrayList<Object>> entry : EvrLevelMap
                    .entrySet()) {
                final ArrayList<Object> data = entry.getValue();
                dataList.add("       EVR "
                        + entry.getKey().replaceAll("\"", "") + " = "
                        + ((Integer) data.get(EVR_COUNT)).toString() + "\n");
            }
            // Eha counts:
            dataList.add("    Channels = " + (noEHA ? NOT_USED : totalEha)
                    + "\n");
            for (final Entry<String, Integer> entry : EhaMap.entrySet()) {
                dataList.add("       CH " + entry.getKey() + " = "
                        + entry.getValue() + "\n");
            }
            dataList.add("    1553 Logs = "
                    + (!has1553Logs ? NOT_USED : totalLog1553) + "\n");
            // Total count:
            dataList.add("    Total = " + totalCount + "\n");
            dataList.add("\nActual command lines for this report:\n");
            final StringBuilder sb = new StringBuilder();
            for (final String a : cmdlineGe) {
                sb.append(a);
                sb.append(" ");
            }
            sb.deleteCharAt(sb.length() - 1);
            dataList.add("GET_EVERYTHING: " + sb + "\n");
            dataList.add("EVR: "
                    + (!noEVR ? getCmdLine(DataType.EVR) : NOT_USED) + "\n");
            dataList.add("EHA: "
                    + (!noEHA ? getCmdLine(DataType.EHA) : NOT_USED) + "\n");
            dataList.add("PRODUCT: "
                    + (!noPROD ? getCmdLine(DataType.PROD) : NOT_USED) + "\n");
            dataList.add("COMMAND: "
                    + (!noCMD ? getCmdLine(DataType.CMD) : NOT_USED) + "\n");
            dataList.add("LOG: "
                    + (!noLOG ? getCmdLine(DataType.LOG) : NOT_USED) + "\n");
            dataList.add("1553 LOG: " + (has1553Logs ? cmdline1553 : NOT_USED)
                    + "\n");
            for (final String str : dataList) {
                summaryFile.write(str);
                consoleWriter.write(str);
            }
            consoleWriter
                    .write("=============================================================================================\n");
            if (summaryFile != null) {
                summaryFile.flush();
                summaryFile.close();
            }
            if (consoleWriter != null) {
                consoleWriter.close();
            }
        } catch (final IOException e) {
            throw new ChillGetEverythingException(
                    "There was an error printing the summary: "
                            + (e.getMessage() == null ? e.toString()
                                    : e.getMessage()), e);
        }
    }

    /**
     * Uses the sessionApp fetch methods to retrieve data from the database and
     * populates sessionIdList, fswVersionList, fswDirList, and hostNameList.
     * 
     * @throws ChillGetEverythingException
     */
    private void getSessionInfo() throws ChillGetEverythingException {
        try {
            // we need to spawn the session with the cmdlineSession, then
            // process
            // it's returns....
            final String mission = "/" + GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse())
                    + "/";
            fswList = new ArrayList<String>();
            int fswDictDirPosition = 0;
            int fswVersPosition = 0;
            if (!sessionIdList.isEmpty()) {
                cmdlineSession.add("-K");
                final StringBuilder sb = new StringBuilder();
                for (final String id : sessionIdList) {
                    sb.append(id);
                    sb.append(",");
                }
                sb.deleteCharAt(sb.length() - 1);
                cmdlineSession.add(sb.toString());
                noSession = false;
            } else {
                noSession = true;
            }
            if (!noSession) {
                BufferedReader inBuff;
                ProcessBuilder procBuilder;
                Process p;
                procBuilder = new ProcessBuilder();
                procBuilder.command(cmdlineSession);
                procBuilder.redirectErrorStream(true);
                System.out.println("Running: "
                        + getCommandString(cmdlineSession));
                p = procBuilder.start();
                inBuff = new BufferedReader(new InputStreamReader(
                        p.getInputStream()));
                String line = inBuff.readLine();
                boolean procHead = false;
                while (line != null) {
                	// Changed "warning" to "warn" and "failure" to "fatal", (current warn and fatal error message prefixes)
                	// Moved to check ALL lines, since warning messages for invalid columns are printed AFTER the headers, before data.
                    
                    // if the chill_get_session has info/warning, we
                    // ignore...error we throw...
                    final String errorDetect = line.split(" ")[0];
                    if (errorDetect.equalsIgnoreCase("info")
                            || errorDetect.equalsIgnoreCase("warn")) {
                    	trace.debug("The chill_get_session may have encountered a problem. Output was: \n" + line);
                    } else if ( errorDetect.equalsIgnoreCase("error")
                    		|| errorDetect.equalsIgnoreCase("fatal")) {
                        throw new ChillGetEverythingException(
                                "The chill_get_session may have returned an error. Output was: \n"
                                        + line);
                    } else if (!procHead) {
                    	procHead = true;
                    	// We split the VALID data line.
                    	final String stringArr[] = line.split(",");
                    	if (stringArr == null || stringArr.length == 0) {
                    		throw new ChillGetEverythingException(
                    				"Unable to parse line for chill_get_sessions. The output was: \n"
                    						+ line);
                    	}
                    	// Then we store the key details for this header.
                    	for (int i = 0; i < stringArr.length; i++) {
                    		if (stringArr[i].trim().equalsIgnoreCase(
                    				"fswDictionaryDir")) {
                    			// we store this index position
                    			fswDictDirPosition = i;
                    		} else if (stringArr[i].trim()
                    				.equalsIgnoreCase("fswVersion")) {
                    			// we store this index position
                    			fswVersPosition = i;
                    		}
                        }
                    } else {
                        final String[] stringArr = line.split(",");
                        final String fsw = stringArr[fswDictDirPosition].replaceAll(
                                "\"", "")
                                + mission
                                + stringArr[fswVersPosition].replaceAll("\"",
                                        "");
                        if (!fswList.contains(fsw)) {
                            fswList.add(fsw);
                        }
                    }
                    
                    line = inBuff.readLine();
                    
                }
                inBuff.close();
            } 
        } catch (final IOException e) {
            throw new ChillGetEverythingException(
                    "error getting the sessions summary information", e);
        }
    }

    /**
     * This allows us to read the header line from the input buffer for this
     * data type.
     * 
     * @param dt
     * @throws ChillGetEverythingException
     */
    private void readHeader(final DataType dt) throws ChillGetEverythingException {
        boolean success = false;
        String line = "";
        final ArrayList<Object> data = DataMap.get(dt);
        final BufferedReader br = (BufferedReader) data.get(INPUT_BUFF);
        try {
            while (!success) {
            	line = br.readLine();
                if (line != null) {
                    // we have to try to process this line of data.
                    success = processHeader(line, dt);
                    if (success) {
                        // It is already printed to it's indiv file...
                        // The header was processed, so we print it
                        // BufferedWriter bw = (BufferedWriter)
                        // data.get(OUTPUT_BUFF);
                        // bw.write(line);
                        // bw.newLine();
                        data.set(PROCESSED_HEADER, true);
                        processedData = true;
                        data.set(LINE, line);
                        // this means there is data being returned
                        data.set(EMPTY, false);
                        data.set(HEADER, line);
                    }
                } else {
                    // this means the line was null, so there is no header to
                    // process.
                    success = true;
                    data.set(EMPTY, true);
                }
            }
        } catch (final IOException e) {
            throw new ChillGetEverythingException(
                    "There was an IOException reading the header from data type: "
                            + dt
                            + ". The error was: "
                            + (e.getMessage() == null ? e.toString()
                                    : e.getMessage())
                            + "\nThe command line ran was: "
                            + (String) data.get(CMD_LINE_STR), e);
        }
    }

    /**
     * This allows us to read a line from the input buffer for this data type
     * 
     * @throws ChillGetEverythingException
     */
    private void readLine(final DataType dt) throws ChillGetEverythingException {
        final ArrayList<Object> data = DataMap.get(dt);
        try {
            if (!(Boolean) data.get(EMPTY)) {
                final BufferedReader br = (BufferedReader) data.get(INPUT_BUFF);
                
                // Change initial value of line to null, skip any log message lines from reading
                String line = null;
                
                //as the check originally did in processLine, only check for errors if it's not a 1553 log line
                if(!dt.equals(DataType.LOG_1553)){
                	// check each line for it being a log message. If info or warning, get the next line. If fatal throw an error.
                	boolean throwaway=true;
                	while(throwaway){
                		line = br.readLine();
                		if(line == null){
                			throwaway = false;
                		} else {
                			final String errorDetect = line.split(" ")[0].toLowerCase();
                			switch(errorDetect){
                			case "info":
                			case "warn":
                				trace.debug("The chill_get_session may have encountered a problem. Output was: \n" + line);
                				break;
                			case "error":
                			case "fatal":
                				throw new ChillGetEverythingException("The chill_get_session may have returned an error. Output was: \n" + line);
                			default:
                				throwaway = false;	
                			}
                		}
                	}
                } else{
                	line = br.readLine();
                }
                
                if (line != null) {
                    if (dt.equals(DataType.LOG_1553)) {
                        line = process1553Log(line);
                        final BufferedWriter bw = (BufferedWriter) data
                                .get(OUTPUT_BUFF);
                        bw.write(line);
                        bw.newLine();
                        totalLog1553 = totalLog1553 + 1;
                        add(line, dt);
                    }
                    // we have to try to process this line of data.
                    // it is already written to the indiv file.
                    // BufferedWriter bw = (BufferedWriter)
                    // data.get(OUTPUT_BUFF);
                    // bw.write(line);
                    if (dt.equals(DataType.EVR)) {
                        // Run processEVR to store various info for
                        // Evr_levels...
                        processEVR(line);
                    } else if (dt.equals(DataType.CMD)) {
                        totalCmds = totalCmds + 1;
                    } else if (dt.equals(DataType.PROD)) {
                        totalProds = totalProds + 1;
                    } else if (dt.equals(DataType.LOG)) {
                        processLOG(line);
                    } else if (dt.equals(DataType.EHA)) {
                        processEHA(line);
                    }
                    // for 1553logs, we don't parse for session
                    if (!dt.equals(DataType.LOG_1553)) {
                        final String session = processLine(line, dt, ProcessType.SESSION).replaceAll("\"", "").trim();
                        if (!sessionIdList.contains(session)) {
                            sessionIdList.add(session);
                        }
                    }
                    totalCount = totalCount + 1;
                    if (!dt.equals(DataType.LOG_1553)) {
                        add(line, dt);
                    }
                } else { // No more of this datatype data to process.
                    data.set(EMPTY, true);
                    if (!firstPass) {
                        setLowestDataType();
                    }
                }
            }
        } catch (final IOException e) {
            throw new ChillGetEverythingException(
                    "There was an IOException reading a line from data type: "
                            + dt
                            + ". The error was: "
                            + (e.getMessage() == null ? e.toString()
                                    : e.getMessage())
                            + "\nThe command line ran was: "
                            + (String) data.get(CMD_LINE_STR), e);
        }
    }

    /**
     * Used to process each chill_get_* line as a header and
     * returns whether or not it was successful in doing so.
     * 
     * @param data This is the line of data that is being processed as a header.
     * @param dt This is the datatype being processed.
     * @return It returns true or false based on the success of processing the header.
     * @throws ChillGetEverythingException It throws this to be caught by the calling method.
     */
    public boolean processHeader(final String data, final DataType dt)
            throws ChillGetEverythingException {
        final ArrayList<Object> dat = DataMap.get(dt);
        String splitChar = " ";
        String ert = "";
        String eventTime = "";
        String searchERT = "";
        String searchSCLK = "sclk";
        final String searchREALTIME = "realtime";
        String searchLEVEL = "level";
        String searchEHAid = "";
        String searchEHAname = "name";
        String searchSess = "session";
        int ertPost = 0;
        int sclkPost = 0;
        int rtPost = 0;
        int levelPost = 0;
        int ehaIdPost = 0;
        int sessPost = 0;
        if (outSRCSV) {
            splitChar = ",";
            ert = ERT;
            eventTime = ERT;
            searchERT = ERT;
            searchSCLK = "SCLK";
            searchSess = "SESSION";
            searchEHAid = "id";
            searchEHAname = "id";
            // The level for all types for SRCSV is "type"
        }
        if (outCSV || outEXCEL) {
            splitChar = ",";
            ert = "ert";
            eventTime = "eventTime";
            searchEHAid = "channelId";
            searchEHAname = "name";
            searchSess = "sessionId";
        }
        if (outSRTEXT) {
            splitChar = "\t";
            ert = "ert";
            eventTime = "event_time";
            searchEHAid = "chid:chName";
            searchEHAname = "chid:chName";
        }
        if (dt.equals(DataType.LOG_1553)) {
            searchERT = "systemTime";
            if (outSRTEXT) {
                searchERT = "system_Time";
            }
        }
        if (dt.equals(DataType.EHA) || dt.equals(DataType.EVR)
                || dt.equals(DataType.PROD)) {
            searchERT = ert;
            // eha&prod don't need level...
            // but the csv/srText/excel level for evrs is "level"
            if (outCSV || outEXCEL || outSRTEXT) {
                searchLEVEL = "level";
            }
            // but the srCSV level for evrs is "type"
            if (outSRCSV) {
                searchLEVEL = "type";
            }
        }
        if (dt.equals(DataType.LOG) || dt.equals(DataType.CMD)) {
            searchERT = eventTime;
            // cmd doesn't need level...
            // but the csv/excel level for logs is "severity"
            if (outCSV || outEXCEL) {
                searchLEVEL = "severity";
            }
            // but the srTEXT level for logs is "level"
            if (outSRTEXT) {
                searchLEVEL = "level";
            }
            if (outSRCSV) {
                searchLEVEL = "type";
            }

        }
        String[] dataArr = null;
        // if the header starts with anything such as info/error/warning we
        // deal accordingly
        final String errorDetect = data.split(" ")[0];
        if (!dt.equals(DataType.LOG_1553)) {
        	// Changed "warning" to "warn" , updated to match the current warn error prefix.
            if (errorDetect.equalsIgnoreCase("info")
                    || errorDetect.equalsIgnoreCase("warn")) {
                // here we return true so they know to try and process
                // another
                // line.
            	// add check to see if the style template failed to load
            	if(data.toLowerCase().contains("template")){
            		throw new ChillGetEverythingException("The chill_get for data type " + dt
            				+ " had an error while retrieving the requested style template. Output was: "
            				+ data + "\nThe command called was: "
            				+ dat.get(CMD_LINE_STR));
            	}
                trace.warn(data);
                return false;
            } else if (errorDetect.equalsIgnoreCase("fatal")) {
                throw new ChillGetEverythingException(
                        "The chill_get for data type " + dt
                                + " may have returned an error. Output was: "
                                + data + "\nThe command called was: "
                                + dat.get(CMD_LINE_STR));
            }
        }
        // We split the VALID data line.
        dataArr = data.split(splitChar);
        // System.out.println("I detected nothing but a valid header...so I try to split it. ");
        if (dataArr == null || dataArr.length == 0) {
            throw new ChillGetEverythingException(
                    "Unable to parse header for data type: " + dt
                            + ". Output was: " + data
                            + "\nThe command called was: "
                            + dat.get(CMD_LINE_STR));
        }
        // Then we store the key details for this datatype.
        for (int i = 0; i < dataArr.length; i++) {
            if (dataArr[i].trim().equalsIgnoreCase(searchERT)) {
                // we store this index position
                ertPost = i;
            } else if (dataArr[i].trim().equalsIgnoreCase(searchSCLK)) {
                // we store this index position
                sclkPost = i;
            } else if (dataArr[i].trim().equalsIgnoreCase(searchREALTIME)) {
                // we store this index position
                rtPost = i;
            } else if (dataArr[i].trim().equalsIgnoreCase(searchLEVEL)) {
                levelPost = i;
            } else if (dataArr[i].trim().equalsIgnoreCase(searchEHAid)) {
                ehaIdPost = i;
            } else if (dataArr[i].trim().equalsIgnoreCase(searchEHAname)) {
                // we only search for and store the name for eha...
                if (dt.equals(DataType.EHA)) {
                    ehaNamePosn= i;
                }
            } else if (dataArr[i].trim().equalsIgnoreCase(searchSess)) {
                sessPost = i;
            }
        }
        // if it gets here, there was no "info" or "warning" or "failure"
        // output, so we return true.
        setDataMapPositions(dt, ertPost, sclkPost, rtPost, levelPost,
                ehaIdPost, sessPost);
        // System.out.println("Header: " + data);
        // System.out.println("Level posn: " + levelPost);
        // System.out.println("Session posn: " + sessPost);
        return true;
    }

    
    /**
     * Used to process each chill_get_* line to store in the summary counts and
     * returns the value being used as a key.
     * 
     * @param data This is the line of data that is being processed as a header.
     * @param dt This is the datatype being processed.
     * @param pt This is what the line is being processed to look 
     * for (ie EHA/SCLK/etc).
     * @return It returns the value that is being used as a key.
     * @throws ChillGetEverythingException It throws this to be caught by the calling method.
     */
    public String processLine(final String data, final DataType dt, final ProcessType pt)
            throws ChillGetEverythingException {
        int dataPosition = getDataPosition(dt, pt);
        String splitChar = " ";
        if (outCSV) {
            splitChar = ",\"";
        } else if (outSRCSV || outEXCEL) {
            splitChar = ",";
        } else if (outTEXT || outSRTEXT) {
            splitChar = "\t";
        }

        final String[] stringArr = data.split(splitChar);
        if (stringArr == null || stringArr.length == 0) {
            throw new ChillGetEverythingException(
                    "Unable to parse line for data type: " + dt
                            + ". Output was: \n" + data);
        }
        if (pt.equals(ProcessType.EHA)) {
            String id = null;
            String name = null;
            if (outCSV || outEXCEL) {
                id = stringArr[dataPosition].replaceAll("\"", "");
                name = stringArr[ehaNamePosn].replaceAll("\"", "");
            } else if (outSRCSV) {
                name = stringArr[dataPosition].split(" ")[0];
                id = stringArr[dataPosition].split(" ")[1].replaceAll(
                        "[\\p{Punct}(]", "").replaceAll("[\\p{Punct})]", "");
            } else if (outSRTEXT) {
                id = stringArr[dataPosition].split(":")[0];
                name = stringArr[dataPosition].split(":")[1];
            } else {
            	  throw new ChillGetEverythingException(
                          "Unable to parse line for data type: " + dt
                                  + ". Output was: \n" + data);
            }
            return id.trim() + " " + name.trim();
        }
        if (outSRCSV && pt.equals(ProcessType.SESSION)) {
            dataPosition = stringArr.length - 1;
        }
        try {
            if (pt.equals(ProcessType.SESSION) && !dt.equals(DataType.LOG_1553)) {
                final String temp = stringArr[dataPosition].replaceAll("\"", "").trim();
                if (temp.contains(" ")) {
                    // this means the session is invalid, throw an exception.
                    throw new ChillGetEverythingException(
                            "Error getting the session ID. The invalid session found is: "
                                    + stringArr[dataPosition]);
                }
                // we want temp to be an integer, so we try to parse it.
                Integer.parseInt(temp);
            }
        } catch (final NumberFormatException e) {
            throw new ChillGetEverythingException(
                    "Error getting the session ID. The invalid session found is not an integer: "
                            + stringArr[dataPosition], e);
        }
        if ((pt.equals(ProcessType.LEVEL) && outSRCSV)
                && (dt.equals(DataType.EVR) || dt.equals(DataType.LOG))) {
            // this means we are processing an evr for its level for sr_csv,
            // so we must parse out the extra EVR text.
            try {
                // we try to return the second spot for the level
                return stringArr[dataPosition].split(" ")[1];
            } catch (final Exception e) {
                // trace.warn("Error getting EVR level. The invalid level found is: "
                // + stringArr[dataPosition]);
                // throw e;
                // if it couldn't split, then we return a empty string.
                return "";
            }
        }
        return stringArr[dataPosition];
    }

    /**
     * Used to add a line of data passed to our sorted temp structure.
     * 
     * @param line This is the line being added.
     * @param dt This is the data type of the line being added
     * @throws ChillGetEverythingException It throws this to be caught by the calling method.
     */
    public void add(final String line, final DataType dt)
            throws ChillGetEverythingException {
        // Need to just insert the new line into my priority queue.
        // Then update our variable lowestDataType
        final SortDataStructure sds = new SortDataStructure(line, dt);
        // If we are doing ERT, we don't care what the data type is. They
        // all go to priority queue.
        // If we are doing SCET, only EHA, EVR, PROD get added to priority
        // queue
        if (doERT
                || (doSCET && (dt.equals(DataType.EHA)
                        || dt.equals(DataType.EVR) || dt.equals(DataType.PROD)))) {
            sortPQ.add(sds);
        } else if (doSCET && dt.equals(DataType.CMD)) {
            // This means it is a cmd, so we set the lowestCMD Object
            lowestCMD = sds;
        } else if (doSCET && dt.equals(DataType.LOG)) {
            // This means it is a log, so we set the lowestLOG Object
            lowestLOG = sds;
        }
        if (!firstPass) {
            setLowestDataType();
        }
    }

    /**
     * Used to set the new lowestDataType.
     */
    public void setLowestDataType() {
        if (doERT) {
            // we won't use this so it must be null.
            lowestERTDataType = null;
        }
        // IF doScet, First we need to recalculate the lowestERTDataType (cmd or
        // log)
        Boolean CMDnotProcessed = true;
        if (DataMap.containsKey(DataType.CMD)) {
            CMDnotProcessed = !(Boolean) DataMap.get(DataType.CMD).get(
                    PROCESSED_HEADER)
                    || getDataFileEmpty(DataType.CMD);
        }
        Boolean LOGnotProcessed = true;
        if (DataMap.containsKey(DataType.LOG)) {
            LOGnotProcessed = !(Boolean) DataMap.get(DataType.LOG).get(
                    PROCESSED_HEADER)
                    || getDataFileEmpty(DataType.LOG);
        }
        if (doSCET) {
            if ((noCMD || CMDnotProcessed) && (noLOG || LOGnotProcessed)) {
                // There are no cmds or logs
                lowestERTDataType = null;
            } else if (noCMD || CMDnotProcessed) {
                // There will be no CMDS OR none was returned from the get OR
                // none are left to process.
                lowestERTDataType = DataType.LOG;
            } else if (noLOG || LOGnotProcessed) {
                // There will be no LOGS OR none was returned from the get OR
                // none are left to process.
                lowestERTDataType = DataType.CMD;
            } else
            // There will be both cmds and logs. Compare lowest...
            {
                // we have to make sure both lowestCMD/lowestLOG aren't null
                if (lowestCMD == null && lowestLOG == null) {
                    lowestERTDataType = null;
                } else if (lowestCMD == null) {
                    lowestERTDataType = DataType.LOG;
                } else if (lowestLOG == null) {
                    lowestERTDataType = DataType.CMD;
                    // otherwise they are both set so we can compare them :)
                } else {
                    if (lowestCMD.ert.compareTo(lowestLOG.ert) <= 0) {
                        lowestERTDataType = DataType.CMD;
                    } else {
                        // if(lowestCMD.ert.compareTo(lowestLOG.ert) > 0) or =
                        lowestERTDataType = DataType.LOG;
                    }
                }
            }
        }
        if (sortPQ.peek() == null) {
            if (lowestERTDataType == null) {
                lowestDataType = null;
            } else {
                lowestDataType = lowestERTDataType;
            }
        } else {
            final DataType dt = sortPQ.peek().dataType;
            final boolean rt = sortPQ.peek().realTime;
            final String ert = sortPQ.peek().ert;
            // If we do ERT, or SCET with no commands and logs, we don't care
            // about the datatype. The lowest is the
            // first in the queue.
            if (doERT
                    || (doSCET && (noCMD || CMDnotProcessed) && (noLOG || LOGnotProcessed))) {
                lowestDataType = dt;
            } else {
                // Do scet with cmds or logs (or both). The lowest is based
                // on the datatypes If the lowest in the queue isn't a realtime
                // eha, it
                // is recorded. So it is automatically the lowest (Owen said we
                // cannot compare these to the CMD/LOG ERT's.)
                if (!rt) {
                    // The lowest in the queue is not realtime
                    lowestDataType = dt;
                } else {
                    // This means the lowest in the queue is a realtime eha/evr,
                    // and
                    // there is still CMD/LOG data to compare, so we
                    // can use it to see if we can sort our CMD or LOGs
                    if (lowestERTDataType.equals(DataType.LOG)) {
                        // The LOG is the lowest of CMD/LOG (or only)
                        if (lowestLOG.ert.compareTo(ert) < 0) {
                            // The LOG is the lowest of LOG/realtime sclk
                            lowestDataType = DataType.LOG;
                        } else {
                            // The realtime sclk is the lowest of LOG/ realtime
                            // sclk
                            lowestDataType = dt;
                        }
                    } else if (lowestERTDataType.equals(DataType.CMD)) {
                        // The CMD is the lowest of CMD/LOG (or only)
                        if (lowestCMD.ert.compareTo(ert) < 0) {
                            // The CMD is the lowest of CMD/realtime sclk
                            lowestDataType = DataType.CMD;
                        } else {
                            // The realtime sclk is the lowest of CMD/ realtime
                            // sclk
                            lowestDataType = dt;
                        }
                    }
                }
            }
        }
    }

    /**
     * Used to print the 'lowest' line of data from our sorted temp structure
     * and take it off then WE DON'T update the lowestDataType b/c we want to
     * use it the next time around.
     * @throws ChillGetEverythingException It throws this to be caught by the calling method.
     */
    public void printLineToFile() throws ChillGetEverythingException {
        try {
            String writeLine = null;
            Boolean print = false;
            // If we are doing ert, or doing SCET and the lowest data type is
            // EHA/EVR/PROD, we print from the priority queue
            if (doERT
                    || (doSCET && (lowestDataType.equals(DataType.EHA)
                            || lowestDataType.equals(DataType.EVR) || lowestDataType
                            .equals(DataType.PROD))))
                if (sortPQ.peek() != null) { // There is something in the
                                             // structure to print.
                    writeLine = sortPQ.poll().line;
                    print = true;
                } else {
                    // Nothing to print
                    print = false;
                }
            else {
                // This means we are doing SCET, and the lowest data type is
                // CMD/LOG. We must print the lowest then delete it.
                if (lowestDataType.equals(DataType.CMD)) {
                    writeLine = lowestCMD.line;
                    print = true;
                    lowestCMD = null;
                } else {
                    writeLine = (lowestLOG.line);
                    print = true;
                    lowestLOG = null;
                }
            }
            if (print) {
                // there was something for us to print to the sorted file.
                outAll.write(writeLine);
                outAll.newLine();
                if (outEXCEL) {
                    IDbQueryable dq = null;
                    // If the dataString is from a .csv file (note that when
                    // using
                    // excel output we also create .csv files):
                    // I am assuming that cmds, logs, and 1553 logs are not
                    // included
                    // when using scet ordering!

                    
                    String type = null;

                    if (outEXCEL) {
                        if (lowestDataType == DataType.EHA) {
                            dq = dbChannelSampleFactory.createQueryableProvider();
                            type = "ChanvalQuery";
                        } else if (lowestDataType == DataType.EVR) {
                            dq = dbEvrFactory.createQueryableProvider();
                            type = "EvrQuery";
                        } else if (lowestDataType == DataType.CMD) {
                            dq = dbCommandFactory.createQueryableProvider();
                            type = "CommandQuery";
                        } else if (lowestDataType == DataType.PROD) {
                            dq = dbProductMetadataFactory.createQueryableProvider();
                            type = "ProductQuery";
                        } else if (lowestDataType == DataType.LOG) {
                            dq = dbLogFactory.createQueryableProvider();
                            type = "LogQuery";
                        } else if (lowestDataType == DataType.LOG_1553) {
                            dq = dbLog1553Factory.createQueryableProvider();
                            type = "Log1553";
                        } else {
                        	  throw new ChillGetEverythingException(
                                      "Unrecognized lowest data type during sort" + lowestDataType);
                        }

                        try
                        {
                            final Pair<List<String>, List<String>> lists =
                                CsvQueryProperties.instance().getCsvLists(type);

                            // Parses the data from the CSV line of data
                            dq.parseCsv(writeLine, lists.getOne());
                        }
                        catch (final CsvQueryPropertiesException cce)
                        {
                            throw new ChillGetEverythingException("Unable to get CSV columns for " + type, cce);
                        }

                    }
                    outFormatterExcel.writeObjectCustom(dq,
                                                        false);
                }
                return;
            } else {
                // nothing to print
                return;
            }
        } catch (final IOException e) {
            // e.printStackTrace();
            throw new ChillGetEverythingException(
                    "Unable to write to the sorted file: "
                            + (e.getMessage() == null ? e.toString()
                                    : e.getMessage()), e);
        }
    }

    /**
     * Used to sort the data from the chill_get_* processes and store it's
     * output to the relevant files.
     * 
     * @throws ChillGetEverythingException It throws this to be caught by the calling method.
     */
    public void processSql() throws ChillGetEverythingException {
        /*
         * Now we must setup the MAP that maps each dataType to their
         * required/tracked information
         */
        DataMap = new TreeMap<DataType, ArrayList<Object>>();
        EvrLevelMap = new TreeMap<String, ArrayList<Object>>();
        LogLevelMap = new TreeMap<String, Integer>();
        EhaMap = new TreeMap<String, Integer>();
        sessionIdList = new ArrayList<String>();
        if (!no1553LOG && !outSRCSV && doERT && !doSCET) {
            initializeDataMapProcesses(DataType.LOG_1553, cmdline1553,
                    "/log_1553" + ext);
            has1553Logs = true;
        } else {
            has1553Logs = false;
        }
        if (!noEHA) {
            initializeDataMapProcesses(DataType.EHA, cmdlineChan, "/eha" + ext);
        }
        if (!noEVR) {
            initializeDataMapProcesses(DataType.EVR, cmdlineEvr, "/evr" + ext);
        }
        if (!noCMD) {
            initializeDataMapProcesses(DataType.CMD, cmdlineCmd, "/cmd" + ext);
        }
        if (!noLOG) {
            initializeDataMapProcesses(DataType.LOG, cmdlineLog, "/log" + ext);
        }
        if (!noPROD) {
            initializeDataMapProcesses(DataType.PROD, cmdlineProd, "/prod"
                    + ext);
        }
        // Note, we have already opened all of the files we want to use for
        // EHA, EVR, CMD, LOG, PROD, 1553LOG.
        /*
         * Go through each chill_get, if data is returned process the sql lines
         */
        // EHA, EVR, CMD, PROD, LOG, LOG_1553, SORTED
        try {
            // Channels:
            if (!noEHA) {
                String line = "";
                final ArrayList<Object> data = DataMap.get(DataType.EHA);
                final BufferedReader br = (BufferedReader) data.get(INPUT_BUFF);
                System.out.println(COMMAND_PREFIX + data.get(CMD_LINE_STR));
                line = br.readLine();
                while (line != null) {
                    // we have to try to process this line of data.
                    System.out.println(line);
                    line = br.readLine();
                }
                System.out.println();
            }
            // EVR:
            if (!noEVR) {
                String line = "";
                final ArrayList<Object> data = DataMap.get(DataType.EVR);
                final BufferedReader br = (BufferedReader) data.get(INPUT_BUFF);
                System.out.println(COMMAND_PREFIX + data.get(CMD_LINE_STR));
                line = br.readLine();
                while (line != null) {
                    // we have to try to process this line of data.
                    System.out.println(line);
                    line = br.readLine();
                }
                System.out.println();
            }
            // Products:
            if (!noPROD) {
                String line = "";
                final ArrayList<Object> data = DataMap.get(DataType.PROD);
                final BufferedReader br = (BufferedReader) data.get(INPUT_BUFF);
                System.out.println(COMMAND_PREFIX + data.get(CMD_LINE_STR));
                line = br.readLine();
                while (line != null) {
                    // we have to try to process this line of data.
                    System.out.println(line);
                    line = br.readLine();
                }
                System.out.println();
            }
            // Logs:
            if (!noLOG) {
                String line = "";
                final ArrayList<Object> data = DataMap.get(DataType.LOG);
                final BufferedReader br = (BufferedReader) data.get(INPUT_BUFF);
                System.out.println(COMMAND_PREFIX + data.get(CMD_LINE_STR));
                line = br.readLine();
                while (line != null) {
                    // we have to try to process this line of data.
                    System.out.println(line);
                    line = br.readLine();
                }
                System.out.println();
            }
            // Commands:
            if (!noCMD) {
                String line = "";
                final ArrayList<Object> data = DataMap.get(DataType.CMD);
                final BufferedReader br = (BufferedReader) data.get(INPUT_BUFF);
                System.out.println(COMMAND_PREFIX + data.get(CMD_LINE_STR));
                line = br.readLine();
                while (line != null) {
                    // we have to try to process this line of data.
                    System.out.println(line);
                    line = br.readLine();
                }
                System.out.println();
            }
        } catch (final IOException e) {
            throw new ChillGetEverythingException(
                    "There was an IOException reading a line of data. The error was: "
                            + (e.getMessage() == null ? e.toString()
                                    : e.getMessage()), e);
        }
    }

    /**
     * Used to sort the data from the chill_get_* processes and store it's
     * output to the relevant files.
     * @throws ChillGetEverythingException It throws this to be caught by the calling method.
     */
    public void processData() throws ChillGetEverythingException {
        /*
         * Now we must setup the MAP that maps each dataType to their
         * required/tracked information
         */
        DataMap = new TreeMap<DataType, ArrayList<Object>>();
        EvrLevelMap = new TreeMap<String, ArrayList<Object>>();
        LogLevelMap = new TreeMap<String, Integer>();
        EhaMap = new TreeMap<String, Integer>();
        sessionIdList = new ArrayList<String>();
        if (!no1553LOG && !outSRCSV && doERT && !doSCET) {
            initializeDataMapProcesses(DataType.LOG_1553, cmdline1553,
                    "/log_1553" + ext);
            has1553Logs = true;
        } else {
            has1553Logs = false;
        }
        if (!noEHA) {
            initializeDataMapProcesses(DataType.EHA, cmdlineChan, "/eha" + ext);
        }
        if (!noEVR) {
            initializeDataMapProcesses(DataType.EVR, cmdlineEvr, "/evr" + ext);
        }
        if (!noCMD) {
            initializeDataMapProcesses(DataType.CMD, cmdlineCmd, "/cmd" + ext);
        }
        if (!noLOG) {
            initializeDataMapProcesses(DataType.LOG, cmdlineLog, "/log" + ext);
        }
        if (!noPROD) {
            initializeDataMapProcesses(DataType.PROD, cmdlineProd, "/prod"
                    + ext);
        }
        // Note, we have already opened all of the files we want to use for
        // EHA, EVR, CMD, LOG, PROD, 1553LOG.
        /*
         * Go through each chill_get, if data is returned process the headers
         */
        // EHA, EVR, CMD, PROD, LOG, LOG_1553, SORTED
        // Channels:
        if (!noEHA) {
            readHeader(DataType.EHA);
        }
        // EVR:
        if (!noEVR) {
            readHeader(DataType.EVR);
        }
        // Products:
        if (!noPROD) {
            readHeader(DataType.PROD);
        }
        // Logs:
        if (!noLOG) {
            readHeader(DataType.LOG);
        }
        // Commands:
        if (!noCMD) {
            readHeader(DataType.CMD);
        }
        String data1553 = null;
        if (has1553Logs) {
            try {
                // the 1553 output has no header, so we just try to read the
                // first line.
                boolean success = false;
                final ArrayList<Object> data = DataMap.get(DataType.LOG_1553);
                final BufferedReader br = (BufferedReader) data.get(INPUT_BUFF);
                data1553 = br.readLine();
                if (data1553 != null) {
                    final String header = (String) data.get(HEADER);
                    // We have to try to process this header
                    success = processHeader(header, DataType.LOG_1553);
                    if (success) {
                        // The header was processed, so we print it
                        final BufferedWriter bw = (BufferedWriter) data
                                .get(OUTPUT_BUFF);
                        bw.write(header);
                        bw.newLine();
                        data.set(PROCESSED_HEADER, true);
                        processedData = true;
                        // this means there is data being returned
                        data.set(EMPTY, false);
                        data.set(HEADER, header);
                    }
                } else {
                    // this means the line was null, so there is no data to
                    // process.
                    success = true;
                    has1553Logs = false;
                    data.set(EMPTY, true);
                }
            } catch (final IOException e) {
                throw new ChillGetEverythingException(
                        "Error procssing the 1553 logs header.", e);
            }
        }
        // Now, if data was processed we setup the Sorted file & continue
        if (processedData) { // Some data was processed to sort. Need
                             // to setup sort file
            String filename = "";
            if (doERT) {
                filename = outputDirFile.getPath() + "/ert_sorted";
            } else {
                filename = outputDirFile.getPath() + "/scet_sorted";
            }
            try {
                if (outEXCEL) {
                    outFormatterExcel = OutputFormatterFactory
                            .getFormatter(appContext, OutputFormatType.EXCEL);
                    outFormatterExcel.setUpSorted(filename);
                    outFormatterExcel.writeObjectCustom(null,
                                                        true); // Print a heade
                                                               // to the excel
                                                               // file
                }
            } catch (final IOException e) {
                throw new ChillGetEverythingException(
                        "Error writing to sorted Excel file.", e);
            }
            try {
                outAll = new BufferedWriter(new FileWriter(filename + ext));
                if (ext.equals(".csv")) {
                    outAll.write(csvHeader);
                    outAll.newLine();
                } else if (ext.equals("_sr.csv")) {
                    outAll.write(csvSRHeader);
                    outAll.newLine();
                } else if (ext.equals("_sr.txt") || ext.equals(".txt")) {
                    outAll.write(txtSRHeader);
                    outAll.newLine();
                }
            } catch (final IOException e) {
                throw new ChillGetEverythingException(
                        "Error writing to sorted file.", e);
            }
            /*
             * Now we go through and start collecting data, and sorting...
             */
            // Channels:
            if (!noEHA) {
                readLine(DataType.EHA);
            }
            // EVR:
            if (!noEVR) {
                readLine(DataType.EVR);
            }
            // Products:
            if (!noPROD) {
                readLine(DataType.PROD);
            }
            // Logs:
            if (!noLOG) {
                readLine(DataType.LOG);
            }
            // Commands:
            if (!noCMD) {
                readLine(DataType.CMD);
            }
            if (has1553Logs) {
                try {
                    final DataType dt = DataType.LOG_1553;
                    // we already stored the first line as a string, data1553
                    final ArrayList<Object> data = DataMap.get(dt);
                    if ((data1553) != null) {
                        // we have to try to process this line of data.\
                        final String line = process1553Log(data1553);
                        final BufferedWriter bw = (BufferedWriter) data
                                .get(OUTPUT_BUFF);
                        bw.write(line);
                        bw.newLine();
                        // Run process1553Log to store various info for
                        // counts...
                        totalLog1553 = totalLog1553 + 1;
                        totalCount = totalCount + 1;
                        add(line, dt);
                        data.set(EMPTY, false);
                    } else { // No more of this datatype data to process.
                        data.set(EMPTY, true);
                        has1553Logs = false;
                    }
                } catch (final IOException e) {
                    throw new ChillGetEverythingException(
                            "Error writing to 1553 file.", e);
                }
            }
            /*
             * Now we've processed the first line of each get that is returning
             * data.
             */
            firstPass = false;
            setLowestDataType();
        }
        while ((!noEHA && !getDataFileEmpty(DataType.EHA))
                || (!noEVR && !getDataFileEmpty(DataType.EVR))
                || (!noCMD && !getDataFileEmpty(DataType.CMD))
                || (!noLOG && !getDataFileEmpty(DataType.LOG))
                || (!noPROD && !getDataFileEmpty(DataType.PROD))
                || (has1553Logs && !getDataFileEmpty(DataType.LOG_1553))) {
            /*
             * Now we first print out whomever is the lowest, then we continue
             * to process each line of whichever has the lowest time value. Once
             * we process their data, if they have any or not, it will now
             * recalculate the lowest (either in the add, or directly through
             * setLowestDataType), so it can do it all over again.
             */
            printLineToFile();
            if (lowestDataType.equals(DataType.EHA)) {
                readLine(DataType.EHA);
            } else if (lowestDataType.equals(DataType.EVR)) {
                readLine(DataType.EVR);
            } else if (lowestDataType.equals(DataType.CMD)) {
                readLine(DataType.CMD);
            } else if (lowestDataType.equals(DataType.LOG)) {
                readLine(DataType.LOG);
            } else if (lowestDataType.equals(DataType.PROD)) {
                readLine(DataType.PROD);
            } else if (lowestDataType.equals(DataType.LOG_1553)) {
                readLine(DataType.LOG_1553);
            }
        } // Ends the while going through all the data.
          // Now we're done processing all the data.
          // Close all of the files/delete any non used files.
        closeFiles();
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.IDbFetchApp#addAppOptions()
     */
    @Override
    protected void addAppOptions() {
        super.addAppOptions();

        addOption(
                OUTPUT_DIR_SHORT,
                OUTPUT_DIR_LONG,
                STRING_ARG,
                "Output directory where generated files are placed."
                        + "The default directory location will be SR_HOSTNAME_SESSIONID_TIME,"
                        + " where the TIME is when you ran the report in the format"
                        + " specified by the Gds Configuration files, and the HOSTNAME/"
                        + "SESSIONID are the first ones retrieved from the database.");
        addOption(
                OUTPUT_FORMAT_SHORT,
                OUTPUT_FORMAT_LONG,
                "format",
                "The formatting style for the query output with a Velocity template."
                        + " Default is CSV. Options are csv, text, excel, sr_csv.");

        addOption(null, ChannelValueFetchApp.CHANNEL_TYPES_LONG, STRING_ARG,
                  ChannelTypeSelect.RETRIEVE);

        // Modify chill_get_everything to support (--changesOnly)
        addOption(CHANGE_VALUES_SHORT, CHANGE_VALUES_LONG, null,
                "Channel values should only be reported on change; default is all channel values");

        addOption(null, EvrFetchApp.EVR_TYPES_LONG, STRING_ARG,
                "Retrieve selected types: " + "s=SSE " + "f=FSW-realtime "
                        + "r=FSW-recorded");

        addOption(BEGIN_TIME_SHORT, BEGIN_TIME_LONG, "Time",
                "Begin time of range (ERT times only)");
        addOption(END_TIME_SHORT, END_TIME_LONG, "Time",
                "End time of range (ERT times only)");
        
        final Option ertOpt = ReservedOptions.createOption(null, DO_ERT_LONG, null,
                "Combined output is sorted by ERT (default).");
        options.addOption(ertOpt);
        final Option scetOpt = ReservedOptions.createOption(null, DO_SCET_LONG, null,
                "Combined output is sorted by SCET.");
        options.addOption(scetOpt);
        final Option noEhaOpt = ReservedOptions.createOption(NO_EHA_LONG, null,
                "No channels in the report.");
        options.addOption(noEhaOpt);

        final Option noEvrOpt = ReservedOptions.createOption(NO_EVR_LONG, null,
                "No EVRs in the report.");
        options.addOption(noEvrOpt);

        final Option noCmdOpt = ReservedOptions.createOption(NO_CMD_LONG, null,
                "No commands in the report.");
        options.addOption(noCmdOpt);

        final Option noProdOpt = ReservedOptions.createOption(NO_PROD_LONG, null,
                "No products in the report.");
        options.addOption(noProdOpt);

        final Option noLogOpt = ReservedOptions.createOption(NO_LOG_LONG, null,
                "No logs in the report.");
        options.addOption(noLogOpt);

        // Options for 1553 logs:
        final Option file1553Opt = ReservedOptions.createOption(FILE_1553_LONG,
                STRING_ARG, "The raw 1553 log file to be used in this report.");
        options.addOption(file1553Opt);

        final Option rtOpt = ReservedOptions
                .createOption(
                        RT_LONG,
                        STRING_ARG,
                        "The list of remote terminals (RTs) to include in the "
                                + "1553 log output. Can be a single value (e.g. \"10\"), a range of values (e.g. \"10..14\"), or a comma-separated list of "
                                + "ranges and individual values (e.g. \"1..4,10..14,28\").");
        options.addOption(rtOpt);

        final Option saOpt = ReservedOptions
                .createOption(
                        SA_LONG,
                        STRING_ARG,
                        "The list of SubAddresses (SAs) to include in the "
                                + "1553 log output. Can be a single value (e.g. \"10\"), a range of values (e.g. \"10..14\"), or a comma-separated list of "
                                + "ranges and individual values (e.g. \"1..4,10..14,28\").");
        options.addOption(saOpt);

        final Option sysStartOpt = ReservedOptions
                .createOption(
                        SYS_TIME_START_LONG,
                        STRING_ARG,
                        "Begin time of desired 1553 log entry time range using log "
                                + "file system time. The time may be specified in one of three formats: ISO (YYYY-MM-DDThh:mm:ss), "
                                + "DOY (YYYY-DDDThh:mm:ss) or 1553 (YYYYMMDDTHHMMSS).");
        options.addOption(sysStartOpt);

        final Option sysEndOpt = ReservedOptions
                .createOption(
                        SYS_TIME_END_LONG,
                        STRING_ARG,
                        "End time of desired 1553 log entry time range using log "
                                + "file system time. The time may be specified in one of three formats: ISO (YYYY-MM-DDThh:mm:ss), "
                                + "DOY (YYYY-DDDThh:mm:ss) or 1553 (YYYYMMDDTHHMMSS).");
        options.addOption(sysEndOpt);

        final Option sclkStartOpt = ReservedOptions
                .createOption(
                        SCLK_START_LONG,
                        STRING_ARG,
                        "Begin time of desired 1553 log entry time range using "
                                + "Spacecraft Clock (SCLK). The time may be specified in SECONDS.SUBSECONDS "
                                + "format, COARSE-FINE format or as an integer containing SCLK_MICROSECONDS "
                                + "(like in the raw 1553 log).");
        options.addOption(sclkStartOpt);

        final Option sclkEndOpt = ReservedOptions
                .createOption(
                        SCLK_END_LONG,
                        STRING_ARG,
                        "End time of desired 1553 log entry time range using "
                                + "Spacecraft Clock (SCLK). The time may be specified in SECONDS.SUBSECONDS "
                                + "format, COARSE-FINE format or as an integer containing SCLK_MICROSECONDS "
                                + "(like in the raw 1553 log).");
        options.addOption(sclkEndOpt);
        addOption(CHANNEL_ID_FILE_SHORT, CHANNEL_ID_FILE_LONG, STRING_ARG,
                "The name of a file containing a list of channel names");
        addOption(
                CHANNEL_IDS_SHORT,
                CHANNEL_IDS_LONG,
                STRING_ARG,
                "A comma-separated list of channel names or ranges (e.g. A-0051,B-%,C-0123..C0200)");
        addOption(PRODUCT_COMPLETE_SHORT, PRODUCT_COMPLETE_LONG, null,
                "Flag to indicate that only complete products will be retrieved.");
        addOption(PRODUCT_PARTIAL_SHORT, PRODUCT_PARTIAL_LONG, null,
                "Flag to indicate that only partial products will be retrieved.");
        DssVcidOptions.addVcidOption(options);
        DssVcidOptions.addDssIdOption(options);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.app.IDbFetchApp#showHelp()
     */
    @Override
    public void showHelp() {
        super.showHelp();
        System.out
                .println("\nERT sorting is used by default. \n"
                        + "You may NOT put BOTH --doErt and --doScet.\n"
                        + "If SCET ordering is chosen then 1553 logs are left out of the report.\n"
                        + "If SCET ordering is chosen, logs and commands are ert sorted and placed "
                        + "within the other data based on the realtime data only.\n\n"
                        +

                        "The default output format is csv.\n"
                        + "To use a different output format you must use the -o/--outputFormat option.\n"
                        + "The \"-o/--outputFormat excel\" option will provide a sorted excel file and .csv files.\n"
                        + "The old session report format for csv files is no longer supplied as a subfolder, but only supplied if you use -o/--outputFormat sr_csv.\n"
                        + "Note, that 1553 logs will NOT be included in the old session report format.\n"
                        + "If using channels in the report, make sure to supply a channel id or channel id file.\n\n"
                        +

                        "Multiple sessions can be used with this application.\n\n"
                        +

                        "If using 1553 logs in the report, make sure to supply the input raw 1553 file and a time constraint,"
                        + "\n or a Remote Terminal constraint, or a SubAddress constraint.\n\n"
                        +

                        "Warning, in order to not interrupt downlink loading to the database\n"
                        + "this should be run at the end of the session or when no downlink session is in progress.\n");
    }

    /**
     * Creates the output directory to hold the output files generated by this
     * app. Creates a sub folder within the main folder to hold all of the
     * session report format csv files.
     */
    private void setupDirectory() throws ChillGetEverythingException {
    	// set up a default directory if none is supplied:
    	if (useDefaultDir) {
    		outputDir = "SR_"
    				+ date.getFormattedErt(true).replace(':', '_');
    	}
    	outputDirFile = new File(outputDir);
    	// makes the directory:
    	if (!outputDirFile.exists() && !outputDirFile.mkdirs()) {
    		throw new ChillGetEverythingException(
    				"Unable to create output directory "
    						+ outputDirFile.getPath());

    	}
    }

    /**
     * Runs the application. Overriding IDbFetchApp run() because this App
     * does not query the database. This app calls other apps to query the
     * database.
     */
    @Override
    public void run() {
        try {
            // System.out.println("Running Get Everything App!");
            setExitCode(SUCCESS);
            addGlobalContext();
            if (sqlOnly) {
                processSql();
            } else {
                // creates/starts the chill_get processes.
                // Processes (and then sorts) the data from the processes.
                processData();
                // Create/Prints the summary file:
                printSummaryFile();
            }
        } catch (final ChillGetEverythingException e) {
            // These two commented out lines are useful for debugging.
            // e.printStackTrace();
            // trace.warn((e.getMessage() == null ? e.toString() :
            // e.getMessage()));
            trace.fatal("Application error: " + (e));
            setExitCode(1);
        }

    }

    /**
     * The main method to start the application.
     * 
     * @param args list of arguments to the application
     */
    public static void main(final String[] args) {
        final GetEverythingApp app = new GetEverythingApp();
        app.runMain(args);
    }
}
