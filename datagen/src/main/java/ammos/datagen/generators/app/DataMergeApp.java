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
package ammos.datagen.generators.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import ammos.datagen.cmdline.DatagenOptions;
import ammos.datagen.config.GeneralMissionConfiguration;
import ammos.datagen.config.IMissionConfiguration;
import ammos.datagen.generators.util.FileSuffixes;
import ammos.datagen.generators.util.GeneratorStatistics;
import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.shared.annotation.CoverageIgnore;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.LoggingConstants;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.CoarseFineEncoding;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.types.Pair;
import jpl.gds.shared.types.Triplet;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * This is an application class that will merge the results from two data
 * generator runs together. It must be supplied with two data generator output
 * directories, a mission configuration file, and an output directory location.
 * It will merge the truth and data files from the input directories to the
 * output directory.
 * 
 *
 * MPCS-7750 =- 10/23/15. Changed to use new command line option
 *          strategy throughput.
 * 
 */
public class DataMergeApp extends AbstractCommandLineApp implements IQuitSignalHandler {

    /**
     * Short option name for the input directory 1 option.
     */
    public static final String INPUT_DIR_1_SHORT_OPT = "1";
    /**
     * Long option name for the input directory 1 option.
     */
    public static final String INPUT_DIR_1_LONG_OPT = "dirOne";
    /**
     * Short option name for the input directory 2 option.
     */
    public static final String INPUT_DIR_2_SHORT_OPT = "2";
    /**
     * Long option name for the input directory 2 option.
     */
    public static final String INPUT_DIR_2_LONG_OPT = "dirTwo";
    /**
     * Short option name for the report interval option.
     */
    public static final String REPORT_INTERVAL_SHORT_OPT = "R";
    /**
     * Long option name for the report interval option.
     */
    public static final String REPORT_INTERVAL_LONG_OPT = "reportInterval";

    /**
     * Success exit code.
     */
    public static final int SUCCESS = 0;
    /**
     * Command line parsing error exit code.
     */
    public static final int COMMAND_LINE_FAILURE = 1;
    /**
     * Run failure exit code.
     */
    public static final int RUN_FAILURE = 2;
    /**
     * Initialization failure exit code.
     */
    public static final int INIT_FAILURE = 3;

    private static final String MERGE = "merge";

    /**
     * The output file name for the merged data file.
     */
    public static final String DATA_FILE_NAME = MERGE
            + FileSuffixes.RAW_PKT.getSuffix();

    /**
     * The output file name for the log file.
     */
    public static final String LOG_FILE_NAME = MERGE
            + FileSuffixes.LOG.getSuffix();

    /**
     * The output file name for the truth file.
     */
    public static final String TRUTH_FILE_NAME = MERGE
            + FileSuffixes.TRUTH.getSuffix();
    /**
     * The output file name for the statistics file.
     */
    public static final String STATS_FILE_NAME = MERGE
            + FileSuffixes.STATISTICS.getSuffix();

    private File inputDir1;
    private File inputDir2;
    private String outputPath;
    private String missionConfigPath;
    private GeneralMissionConfiguration missionConfig;
    private Tracer statusLogger;
    private GeneratorStatistics stats;

    // These variables control graceful exit even if there is a TERM signal
    private final AtomicBoolean readyToExit = new AtomicBoolean(true);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(true);

    // These are used in progress reporting
    private int lastPercentReported;
    private long lastReportTime;
    private int reportInterval = 5 * 60 * 1000;
    private long startTime;

    /* MPCS-7750 - 10/23/15. Define our own command line options. */
    private DirectoryOption input1Option;
    private DirectoryOption input2Option;
    private UnsignedIntOption reportIntervalOption;

    private ISecondaryPacketHeaderLookup secPacketHeaderLookup;
    private ApplicationContext appContext;
    private CcsdsProperties ccsdsProperties; 
    
    /**
     * Initializes the application. This includes establishing the log file,
     * loading the mission configuration, logging the command line arguments,
     * and configuring the AMPCS adaptation.
     * 
     * @return true if the initialization was successful, false if not
     */
    private boolean init() {
        
        
        appContext = SpringContextFactory.getSpringContext(true);
        
        secPacketHeaderLookup = appContext.getBean(ISecondaryPacketHeaderLookup.class);
        
        ccsdsProperties = appContext.getBean(CcsdsProperties.class);

        /*
         * Setup the log file. From this point on, logs will go to both the
         * console and the data generator log file, because the AMPCS default
         * tracer is re-defined here.
         */
        final String logFile = this.outputPath + File.separator + LOG_FILE_NAME;
        MDC.put(LoggingConstants.FILE_APP_LOG_PATH, logFile);
        /* MPCS-9595 - 5/2/18 - Use status tracer, which has a log file appender */
        this.statusLogger = TraceManager.getTracer(Loggers.DATAGEN_STATUS);


        /*
         * Log current command line configuration to the console log.
         */
        logCommandLine();

        /*
         * Load the mission configuration file.
         */
        if (!loadConfiguration()) {
            return false;
        }

        /*
         * Configure the AMPCS adaptation for this mission.
         */
        configureAmpcsMission();

        /*
         * Set up the statistics object for this run.
         */
        this.stats = new GeneratorStatistics();
        GeneratorStatistics.setGlobalStatistics(this.stats);

        return true;
    }

    /**
     * Displays the application help text to the console. This default
     * implementation displays the application name and dumps the command line
     * options.
     * 
     * @see jpl.gds.shared.cli.app.ICommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        /* MPCS-7750 - 10/23/15. Get nested Options object */
        final OptionSet options = createOptions().getOptions();

        final PrintWriter pw = new PrintWriter(System.out);
        /*
         * 9/16/15 - MPCS-7679. Updated help text to reflect removal of
         * the mission option.
         */
        pw.println("Usage: "
                + ApplicationConfiguration.getApplicationName()
                + " --dirOne <directory> --dirTwo <directory> --outputDir <directory> --missionConfig <file-path> --reportInterval <minutes>");
        pw.println("                   ");

        /*
         * 10/6/14 - MPCS-6698. Add help text.
         */
        options.printOptions(pw);
        pw.println("This application will merge two files of binary packets that");
        pw.println("were created by previous runs of a data generator. It must be");
        pw.println("supplied with two data-generator created directories that");
        pw.println("contain the two files. Optionally, the mission configuration");
        pw.println("file can be supplied.  If it is not supplied, multimission");
        pw.println("defaults for packet header and SCLK format, and the fill APID");
        pw.println("will be used, as defined in the mission configuraton schema. Packet");
        pw.println("statistics and merged truth data will be written to the");
        pw.println("indicated output directory along with the merged packet file.");
        pw.flush();
    }

    @Override
    public void exitCleanly() {
        
        final boolean alreadyShuttingDown = DataMergeApp.this.shuttingDown.getAndSet(true);

        /*
         * If shutting down flag is already set, the run method has exited. Do
         * not report premature exit. In either case, set the flag, which will
         * cause the run method to stop when the current packet is complete.
         */
        if (!alreadyShuttingDown) {
            if (DataMergeApp.this.statusLogger != null) {
                DataMergeApp.this.statusLogger.warn(ApplicationConfiguration.getApplicationName()
                        + " received a kill request.  Shutting down prematurely...");
            } else {
                System.err.println(ApplicationConfiguration.getApplicationName()
                        + " received a kill request.  Shutting down prematurely...");
            }
        }

        /*
         * Wait for the run method to exit before closing files. For some reason
         * this handling does not work in junit testing.
         */
        if (!GdsSystemProperties.getSystemProperty("datagen.test", "false").equals("true")) {
            while (!DataMergeApp.this.readyToExit.get()) {
                try {
                    SleepUtilities.fullSleep(500);
                } catch (final ExcessiveInterruptException e) {
                    // someone really wants out?
                    break;
                }
            }
        }

        /*
         * Write out the statistics information upon shutdown.
         */
        if (!alreadyShuttingDown) {
            writeStatistics();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.app.CommandLineApp#createOptions()
     */
    @Override
    public DatagenOptions createOptions() {

        /*
         * MPCS-7750 - 10/23/15. Changed to return DatagenOptions and to
         * use new option creation strategy throughout.
         */

        final DatagenOptions options = new DatagenOptions(this);

        options.addOption(DatagenOptions.MISSION_CONFIG);
        options.addOption(DatagenOptions.OUTPUT_DIRECTORY);

        /*
         *  9/16/15 - MPCS-7679. Removed the mission option.
         */

        /*
         * There is a reserved option for input directory but it's general, and
         * we need two, so create them both here.
         */
        this.input1Option = new DirectoryOption(INPUT_DIR_1_SHORT_OPT,
                INPUT_DIR_1_LONG_OPT, "directory",
                "first generator output directory", true, true);
        options.addOption(this.input1Option);

        this.input2Option = new DirectoryOption(INPUT_DIR_2_SHORT_OPT,
                INPUT_DIR_2_LONG_OPT, "directory",
                "second generator output directory", true, true);
        options.addOption(this.input2Option);

        /*
         * Add the report interval option.
         */
        this.reportIntervalOption = new UnsignedIntOption(
                REPORT_INTERVAL_SHORT_OPT, REPORT_INTERVAL_LONG_OPT, "minutes",
                "desired progress report interval (minutes)", false);
        options.addOption(this.reportIntervalOption);

        return options;

    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {

        /*
         * Super method handles help and version options.
         */
        super.configure(commandLine);

        /*
         * MPCS-7750 - 10/23/15. Changed to use new option parsing
         * strategy througout.
         */

        /* MPCS-7679 - 9/16/15. Removed parsing of the mission option. */

        this.outputPath = DatagenOptions.OUTPUT_DIRECTORY.parse(commandLine,
                true);
        this.inputDir1 = getAndCheckInputDir(commandLine, this.input1Option);
        this.inputDir2 = getAndCheckInputDir(commandLine, this.input2Option);

        /** MPCS-7639 Use UnsignedInteger not Int */
        final UnsignedInteger report = this.reportIntervalOption.parse(commandLine,
                                                                       false);
        if (report != null) {
            this.reportInterval = report.intValue() * 60 * 1000;
        }
    }

    /**
     * Logs the command line configuration to the log file.
     */
    private void logCommandLine() {

        /* 10/6/14 - MPCS-6698. Handle null mission config */
        this.statusLogger.info("System mission set to "
                + GdsSystemProperties.getSystemMission());

        if (this.missionConfigPath != null) {
            final File mcf = new File(this.missionConfigPath);

            this.statusLogger.info("Mission Configuration File: "
                    + mcf.getAbsolutePath());
            this.statusLogger.info("Configuration Last Modified: "
                    + new Date(mcf.lastModified()));
        } else {
            this.statusLogger.info("No mission configuration file supplied");
        }

        this.statusLogger.info("Input Directory 1: "
                + this.inputDir1.getAbsolutePath());
        this.statusLogger.info("Input Directory 1 Last Modified: "
                + new Date(this.inputDir1.lastModified()));
        this.statusLogger.info("Input Directory 2: "
                + this.inputDir2.getAbsolutePath());
        this.statusLogger.info("Input Directory 2 Last Modified: "
                + new Date(this.inputDir2.lastModified()));
        final File outputDir = new File(this.outputPath);
        this.statusLogger.info("Output Directory: "
                + outputDir.getAbsolutePath());
    }

    /**
     * Loads the mission configuration file.
     * 
     * @return true if the configuration was loaded successfully, false if not.
     */
    private boolean loadConfiguration() {

        /* 10/6/14 - MPCS-6698. Handle null mission config */
        if (this.missionConfigPath == null) {
            return true;
        }

        this.missionConfig = new GeneralMissionConfiguration();

        /*
         * Turning off schema validation before load allows multiple types of
         * mission configuration files to be used, no matter what tool they were
         * originally for
         */
        this.missionConfig.setValidating(false);
        return this.missionConfig.load(this.missionConfigPath);
    }

    /**
     * Writes a log message summarizing the generator output location and then
     * writes the current global statistics information to the statistics file.
     */
    private void writeStatistics() {

        this.statusLogger.info(this.stats.getTotalPacketSize()
                + " bytes of packet data written");
        this.statusLogger.info("All output written to directory "
                + this.outputPath);
        final String statsFile = this.outputPath + File.separator
                + STATS_FILE_NAME;
        this.stats.writeToFile(statsFile);
    }

    /**
     * Pulls values from the data generator configuration to populate the
     * configuration necessary to use AMPCS classes we are dependent upon.
     */
    private void configureAmpcsMission() {

        if (this.missionConfigPath == null) {
            return;
        }


        /*
         * Configure the AMPCS SCLK adaptation. First read the properties that
         * define the mission SCLK from the mission configuration.
         */
        final int coarseLen = this.missionConfig.getIntProperty(
                IMissionConfiguration.SCLK_COARSE_LEN, 32);
        final int fineLen = this.missionConfig.getIntProperty(
                IMissionConfiguration.SCLK_FINE_LEN, 16);
        final boolean useFractional = this.missionConfig.getBooleanProperty(
                IMissionConfiguration.USE_FRACTIONAL_SCLK, false);
        final String sepChar = this.missionConfig.getStringProperty(
                IMissionConfiguration.SCLK_SEPARATOR_CHAR, ".");

        /*
         * Now set the AMPCS properties to establish the SCLK adaptation.
         */
        final CoarseFineEncoding sclkEncoding = new CoarseFineEncoding(coarseLen, fineLen);
        final String otherSepChar = sepChar.equals(".") ? "-" : ".";

        final SclkFormatter sclkFmt;
        if (useFractional) {
        	sclkFmt = new SclkFormatter(sepChar, otherSepChar, true, sclkEncoding);
        } else {
        	sclkFmt = new SclkFormatter(sepChar, otherSepChar, false, sclkEncoding);
        }
        final TimeProperties tc = TimeProperties.getInstance();
        tc.setCanonicalSclkEncoding(sclkEncoding);
        tc.setSclkFormatter(sclkFmt);

    }

    /**
     * Parses the specified command line options representing an input
     * directory. Verifies the directory exists and contains a packet data file.
     * 
     * @param commandLine
     *            the parsed CommandLine object
     * @param longOptName
     *            the long option name of the input directory command line
     *            option to parse
     * @return File object for the input directory
     * @throws ParseException
     *             if there is any problem parsing the option
     */
    private File getAndCheckInputDir(final ICommandLine commandLine,
            final ICommandLineOption<String> opt) throws ParseException {

        /*
         * MPCS-7750 - 10/23/15. ICommandLineOption object now does most
         * of the work to check and/or create the directory.
         */

        final String dirValue = opt.parse(commandLine, true);

        final File dirFile = new File(dirValue);
        final String[] datFilesInDir = dirFile.list(new PacketFileFilter());
        if (datFilesInDir.length == 0) {
            throw new ParseException("There is no packet file in " + dirValue);
        } else if (datFilesInDir.length > 1) {
            throw new ParseException("There is more than one packet file in "
                    + dirValue);
        }

        return dirFile;
    }

    /**
     * Resets variables used for progress reporting.
     */
    private void resetProgress() {

        this.lastPercentReported = 0;
        this.startTime = System.currentTimeMillis();
        this.lastReportTime = this.startTime;
    }

    /**
     * Reports current progress to the log file. Progress is reported at every
     * 10% of completion and at the configured report interval in the run
     * configuration. Percent complete is currently determined by comparing
     * total amount of data desired to the amount already generated.
     * 
     * @param desiredBytes
     *            the number of bytes representing the final goal
     * @param currentBytes
     *            the number of bytes already written
     * @param messagePrefix
     *            a prefix for the progress log (should indicate the type of
     *            file in progress)
     */
    private void reportProgress(final long desiredBytes,
            final long currentBytes, final String messagePrefix) {

        /*
         * Compute percent of the data file that is complete and get the current
         * time.
         */
        int percent = (int) ((Float.valueOf(currentBytes) / Float
                .valueOf(desiredBytes)) * 100);
        final long currentTime = System.currentTimeMillis();
        if (percent > 100) {
            percent = 100;
        }

        /*
         * Report at 10% completion intervals, and at configured time interval.
         */
        if ((percent >= this.lastPercentReported + 10)
                || ((currentTime - this.lastReportTime) >= this.reportInterval)) {

            /*
             * Compute remaining time, based upon what has been completed in the
             * elapsed time so far.
             */
            final long elapsed = System.currentTimeMillis() - this.startTime;
            final long elapsedPerPercentPoint = elapsed / percent;
            final long remaining = elapsedPerPercentPoint * (100 - percent);

            /*
             * All this hassle is to get the output formatted into minutes and
             * seconds.
             */
            final long mins = TimeUnit.MILLISECONDS.toMinutes(remaining);
            final long secs = TimeUnit.MILLISECONDS.toSeconds(remaining)
                    - TimeUnit.MINUTES.toSeconds(mins);
            final long msecs = remaining - TimeUnit.SECONDS.toMillis(secs)
                    - TimeUnit.MINUTES.toMillis(mins);

            final String timeStr = String.format("%d minutes %d.%03d seconds",
                    mins, secs, msecs);
            this.statusLogger.info(messagePrefix + " is " + percent
                    + "% complete. Estimated remaining time is " + timeStr);

            /*
             * We report on 10% intervals regardless of desired report interval.
             * So no matter what the percent we just reported, round it down to
             * the last multiple of 10.
             */
            this.lastPercentReported = percent - percent % 10;
            this.lastReportTime = currentTime;
        }
    }

    /**
     * Writes a truth record to a given output stream and returns a total number
     * of bytes written. Returns 0 if the record is empty.
     * 
     * @param bw
     *            output buffer writer to write to
     * @param packetRecord
     *            a truth record
     * @return Number of bytes written. Returns 0 if the record is empty.
     * 
     * @throws IOException
     *             if there is a problem writing to the merge file.
     */

    public long writeTruthRecord(final BufferedWriter bw,
            final List<String> packetRecord) throws IOException {
        int bytes = 0;
        if (packetRecord != null) {
            for (final String line : packetRecord) {
                bytes += line.length();
                bw.write(line);
                bw.newLine();
            }
        }
        return bytes;
    }

    /**
     * Reads a truth record from the given stream and returns a Triplet
     * containing the SCLK, a list of record information including EVR,
     * arguments, channel sample info, packet info, and a total length of a
     * record. Returns null if the input file reaches EOF.
     * 
     * @param br
     *            input buffer reader
     * @return A triplet, consisting of SCLK, list of EVR record information,
     *         and total record length for a regular packet. A triplet of null
     *         SCLK and null packet information and a correct number of record
     *         length for fill packet. A null if there are no more records in
     *         the input file (EOF).
     * 
     * @throws IOException
     *             if there is a problem reading the input truth file.
     */
    public static Triplet<ISclk, List<String>, Integer> readTruthRecord(
            final BufferedReader br) throws IOException {
        try {
            final String fillStr = "Fill Packet: ";
            final String packetStr = "Packet: ";
            final List<String> record = new ArrayList<String>();
            ISclk sclk = new Sclk();
            int length = 0;
            String line = br.readLine();
            /* Continue reading until the next record */
            while (line != null) {
                record.add(line);
                length += line.length();
                if (line.startsWith(packetStr)) {
                    final String[] packetFields = line.split(",");
                    final String sclkStr = packetFields[3];
                    sclk = Sclk.getSclkFromString(sclkStr);
                    return new Triplet<ISclk, List<String>, Integer>(sclk,
                            record, length);
                } else if (line.startsWith(fillStr)) {
                    return new Triplet<ISclk, List<String>, Integer>(null, null,
                            length);
                }
                line = br.readLine();
            }
            return null; /* EOF */
        } catch (final EOFException e) {
            return null;
        }
    }

    /**
     * Merges truth files from the two input directories and creates one truth
     * file in the output directory.
     * 
     * @return true if successful, false if an error or control-C occurs
     */
    private boolean mergeTruth() {

        final Pair<File, File> truthFiles = getTruthFiles();
        if (truthFiles == null) {
            return true;
        }

        BufferedWriter outputWriter = null;
        BufferedReader inputReader1 = null;
        BufferedReader inputReader2 = null;
        boolean success = true;

        resetProgress();

        this.statusLogger.info("Starting merge of truth files.");

        try {

            final Pair<File, File> dataFiles = getTruthFiles();
            long desiredBytes = dataFiles.getOne().length()
                    + dataFiles.getTwo().length();
            long bytesWritten = 0;
            boolean endOfFile1 = false;
            boolean endOfFile2 = false;
            List<String> recordFile1 = null;
            List<String> recordFile2 = null;

            outputWriter = new BufferedWriter(new FileWriter(this.outputPath
                    + File.separator + TRUTH_FILE_NAME));
            inputReader1 = new BufferedReader(
                    new FileReader(dataFiles.getOne()));
            inputReader2 = new BufferedReader(
                    new FileReader(dataFiles.getTwo()));

            Triplet<ISclk, List<String>, Integer> readFromBR1 = readTruthRecord(inputReader1);
            Triplet<ISclk, List<String>, Integer> readFromBR2 = readTruthRecord(inputReader2);

            while (!this.shuttingDown.get() && readFromBR1 != null
                    || readFromBR2 != null) {
                if (!endOfFile1) {
                    recordFile1 = readFromBR1.getTwo();
                }
                if (!endOfFile2) {
                    recordFile2 = readFromBR2.getTwo();
                }
                /*
                 * Handle record from file2 while file1 may has fill record or
                 * reaches EOF.
                 */
                if (recordFile1 == null && !endOfFile2) {
                    /*
                     * Remove size of fill packet record for progress reporting
                     * purpose
                     */
                    if (!endOfFile1) {
                        desiredBytes -= readFromBR1.getThree();
                    }
                    bytesWritten += writeTruthRecord(outputWriter, recordFile2);
                    recordFile2 = null;
                }
                /*
                 * Handle record from file1 while file2 may has fill record or
                 * reaches EOF.
                 */
                else if (recordFile2 == null && !endOfFile1) {
                    /*
                     * Remove size of fill packet record for progress reporting
                     * purpose
                     */
                    if (!endOfFile2) {
                        desiredBytes -= readFromBR2.getThree();
                    }
                    bytesWritten += writeTruthRecord(outputWriter, recordFile1);
                    recordFile1 = null;
                } else if (!endOfFile1 && !endOfFile2) {
                    final ISclk sclk1 = readFromBR1.getOne();
                    final ISclk sclk2 = readFromBR2.getOne();
                    if (sclk1.compareTo(sclk2) <= 0) {
                        bytesWritten += writeTruthRecord(outputWriter,
                                recordFile1);
                        recordFile1 = null;
                        bytesWritten += writeTruthRecord(outputWriter,
                                recordFile2);
                        recordFile2 = null;
                    } else {
                        bytesWritten += writeTruthRecord(outputWriter,
                                recordFile2);
                        recordFile2 = null;
                        bytesWritten += writeTruthRecord(outputWriter,
                                recordFile1);
                        recordFile1 = null;
                    }
                    reportProgress(desiredBytes, bytesWritten, "Truth File");
                }
                if (recordFile1 == null) {
                    readFromBR1 = readTruthRecord(inputReader1);
                    if (readFromBR1 == null) {
                        endOfFile1 = true;
                    }
                }
                if (recordFile2 == null) {
                    readFromBR2 = readTruthRecord(inputReader2);
                    if (readFromBR2 == null) {
                        endOfFile2 = true;
                    }
                }
                if (endOfFile1 && endOfFile2) {
                    break;
                }
            }

        } catch (final Exception e) {
            this.statusLogger.error("Unexpected Exception merging truth files",
                    e);
            success = false;

        } finally {

            try {
                if (outputWriter != null) {
                    outputWriter.close();
                }
                if (inputReader1 != null) {
                    inputReader1.close();
                }
                if (inputReader2 != null) {
                    inputReader2.close();
                }
            } catch (final IOException e) {
                this.statusLogger.error("IO Exception closing truth files", e);
                success = false;
            }
        }

        if (success && !this.shuttingDown.get()) {
            this.statusLogger.info("Finished merge of truth files");
        }
        return success;

    }

    /**
     * Reads a packet from the given stream and returns a Triplet containing the
     * packet header, SCLK, and data. Returns null if the stream is at EOF.
     * 
     * @param stream
     *            the stream to read from
     * @return A Triplet, consisting of packet header, SCLK, and packet data.
     *         Will be null if there are no more packets in the stream (EOF).
     * 
     * @throws IOException
     *             if there is a problem reading the packet file.
     * @throws PacketHeaderExtractionException 
     */
    private Triplet<ISpacePacketHeader, ISclk, byte[]> readPacket(
            final FileInputStream stream) throws IOException {

        try {
            /*
             * Read header bytes first.
             */
            final ISpacePacketHeader header = PacketHeaderFactory.create(ccsdsProperties.getPacketHeaderFormat());
            final byte[] headBuffer = new byte[header.getPrimaryHeaderLength()];
            int bytesRead = stream.read(headBuffer, 0, headBuffer.length);
            /*
             * If we get a -1 here, we have a clean EOF, so we return null to
             * the caller.
             */
            if (bytesRead == -1) {
                return null;
            }
            /*
             * This is also an EOF, but not a valid one. File should not end in
             * the middle of a packet header.
             */
            if (bytesRead != headBuffer.length) {
                throw new IOException(
                        "Ran out of bytes in packet file trying to read packet header");
            }
            header.setPrimaryValuesFromBytes(headBuffer, 0);

            /*
             * Get number of data bytes form header and read the data bytes.
             */
            final byte[] dataBuffer = new byte[header.getPacketDataLength() + 1];
            bytesRead = stream.read(dataBuffer, 0, dataBuffer.length);
            /*
             * This is also an EOF, but not a valid one. File should not end in
             * the middle of a packet's data.
             */
            if (bytesRead != dataBuffer.length) {
                throw new IOException(
                        "Ran out of bytes in packet file trying to read packet data");
            }

            final ISecondaryPacketHeaderExtractor secHdrExtractor = secPacketHeaderLookup.lookupExtractor(header);
            if (!secHdrExtractor.hasEnoughBytes(dataBuffer, 0)) {
            		throw new IOException(
                        "Ran out of bytes in packet file trying to read secondary packet header");
            }

            final ISclk sclk = secHdrExtractor
            		.extract(dataBuffer, header.getPrimaryHeaderLength())
            		.getSclk();

            /*
             * Now create the return triplet.
             */
            return new Triplet<ISpacePacketHeader, ISclk, byte[]>(header, sclk,
                    dataBuffer);
        } catch (final EOFException e) {
            return null;
        }
    }

    /**
     * Merges packet files from the two input directories and creates one packet
     * file in the output directory.
     * 
     * @return true if successful, false if an error or control-C occurs
     */
    private boolean mergeData() {

        FileOutputStream outputStream = null;
        FileInputStream inputStream1 = null;
        FileInputStream inputStream2 = null;
        boolean success = true;

        /*
         * 10/6/14 - MPCS-6698. Handle null mission config when setting
         * fill APID
         */
        int fillApid = 2047;
        if (this.missionConfig == null) {
            this.statusLogger.info("Fill APID defaulted to 2047");
        } else {
            fillApid = this.missionConfig.getIntProperty(
                    IMissionConfiguration.FILL_PACKET_APID, 2047);
        }
        final Pair<File, File> dataFiles = getDataFiles();
        final long desiredBytes = dataFiles.getOne().length()
                + dataFiles.getTwo().length();

        resetProgress();

        this.statusLogger.info("Starting merge of packet files "
                + dataFiles.getOne().getName() + " and "
                + dataFiles.getTwo().getName());

        try {
            outputStream = new FileOutputStream(this.outputPath
                    + File.separator + DATA_FILE_NAME);
            inputStream1 = new FileInputStream(dataFiles.getOne());
            inputStream2 = new FileInputStream(dataFiles.getTwo());
            Triplet<ISpacePacketHeader, ISclk, byte[]> file1Packet = readPacket(inputStream1);
            Triplet<ISpacePacketHeader, ISclk, byte[]> file2Packet = readPacket(inputStream2);

            while (!this.shuttingDown.get()
                    && (file1Packet != null || file2Packet != null)) {

                Triplet<ISpacePacketHeader, ISclk, byte[]> writePacket = null;
                if (file1Packet == null) {
                    writePacket = file2Packet;
                    file2Packet = null;
                } else if (file2Packet == null) {
                    writePacket = file1Packet;
                    file1Packet = null;
                } else {
                    final ISclk sclk1 = file1Packet.getTwo();
                    final ISclk sclk2 = file2Packet.getTwo();
                    if (sclk1.compareTo(sclk2) < 0) {
                        writePacket = file1Packet;
                        file1Packet = null;
                    } else {
                        writePacket = file2Packet;
                        file2Packet = null;
                    }
                }

                final byte[] headerBytes = writePacket.getOne().getBytes();
                final byte[] dataBytes = writePacket.getThree();
                final int totalPacketLen = headerBytes.length
                        + dataBytes.length;
                outputStream.write(headerBytes);
                outputStream.write(dataBytes);

                if (writePacket.getOne().getApid() == fillApid) {
                    this.stats.updateFillPacketStatistics(totalPacketLen,
                            writePacket.getOne().getApid(),
                            writePacket.getTwo(), new AccurateDateTime().getTime());
                } else {
                    this.stats.updatePacketStatistics(totalPacketLen,
                            writePacket.getOne().getApid(),
                            writePacket.getTwo(), new AccurateDateTime().getTime());
                }

                reportProgress(desiredBytes, this.stats.getTotalPacketSize(),
                        "Packet File");

                if (file1Packet == null) {
                    file1Packet = readPacket(inputStream1);
                }
                if (file2Packet == null) {
                    file2Packet = readPacket(inputStream2);
                }
            }

        } catch (final Exception e) {
            this.statusLogger.error(
                    "Unexpected Exception merging packet files", e);
            success = false;

        } finally {

            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream1 != null) {
                    inputStream1.close();
                }
                if (inputStream2 != null) {
                    inputStream2.close();
                }
            } catch (final IOException e) {
                this.statusLogger.error("IO Exception closing packet files", e);
                success = false;
            }
        }
        if (success && !this.shuttingDown.get()) {
            this.statusLogger.info("Finished merge of packet files");
        }
        return success;

    }

    /**
     * Clears shutdown flags for start of data generation. Should be called
     * immediately prior to start of packet generation.
     */
    private void clearShutdownFlags() {

        this.readyToExit.set(false);
        this.shuttingDown.set(false);
    }

    /**
     * Sets shutdown flags for clean exit. Should be called at end of packet
     * generation.
     */
    private void setShutdownFlags() {

        this.readyToExit.set(true);
        this.shuttingDown.set(true);
    }

    /**
     * Executes main application logic. Merges all files.
     * 
     * @return application exit code
     */
    private int run() {

        /*
         * Application is not in a state to shut down.
         */
        clearShutdownFlags();

        /*
         * Merge data files.
         */
        boolean ok = true;
        if (!this.shuttingDown.get()) {
            ok = mergeData();
        }

        /*
         * Merge truth files.
         */
        if (ok && !this.shuttingDown.get()) {
            ok = mergeTruth();
        }

        /*
         * Set exit code.
         */
        int exitCode = SUCCESS;
        if (!ok || this.shuttingDown.get()) {
            exitCode = RUN_FAILURE;
        }

        /*
         * Application is now in a state to shut down.
         */
        setShutdownFlags();

        return exitCode;
    }

    /**
     * Creates a Pair object containing the File objects for the two packet
     * files to be merged.
     * 
     * @return Pair of File objects
     */
    private Pair<File, File> getDataFiles() {

        /*
         * Note that presence of one and only one packet file in each directory
         * was checked in configure().
         */
        final File[] datFilesInDir1 = this.inputDir1
                .listFiles(new PacketFileFilter());
        final File[] datFilesInDir2 = this.inputDir2
                .listFiles(new PacketFileFilter());

        return new Pair<File, File>(datFilesInDir1[0], datFilesInDir2[0]);
    }

    /**
     * Creates a Pair object containing the File objects for the two truth files
     * to be merged. Returns null if there is no truth file in either one of the
     * input directories.
     * 
     * @return Pair of File objects
     */
    private Pair<File, File> getTruthFiles() {

        boolean ok = true;

        /*
         * Check for one and only one truth file in the first input directory.
         * Did not check this in command line parsing, because it is not an
         * error to be lacking truth data. It just means we will not merge it.
         */
        final File[] truthFilesInDir1 = this.inputDir1
                .listFiles(new TruthFileFilter());
        if (truthFilesInDir1.length == 0) {
            this.statusLogger.info("There is no truth file in "
                    + this.inputDir1.getPath());
            this.statusLogger.info("Truth data will not be merged");
            ok = false;
        } else if (truthFilesInDir1.length > 1) {
            this.statusLogger.info("There is more than one truth file in "
                    + this.inputDir1.getPath());
            this.statusLogger.info("Truth data will not be merged");
            ok = false;
        }

        /*
         * This means we will not merge truth.
         */
        if (!ok) {
            return null;
        }

        /*
         * Check for one and only one truth file in the second input directory.
         */
        final File[] truthFilesInDir2 = this.inputDir2
                .listFiles(new TruthFileFilter());
        if (truthFilesInDir2.length == 0) {
            this.statusLogger.info("There is no truth file in "
                    + this.inputDir2.getPath());
            this.statusLogger.info("Truth data will not be merged");
            ok = false;
        } else if (truthFilesInDir2.length > 1) {
            this.statusLogger.info("There is more than one truth file in "
                    + this.inputDir2.getPath());
            this.statusLogger.info("Truth data will not be merged");
            ok = false;
        }

        /*
         * This means we will not merge truth.
         */
        if (!ok) {
            return null;
        }

        return new Pair<File, File>(truthFilesInDir1[0], truthFilesInDir2[0]);
    }

    /**
     * File filter class for truth files.
     * 
     *
     */
    private static class TruthFileFilter implements FilenameFilter {

        /**
         * {@inheritDoc}
         * 
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        @Override
        public boolean accept(final File arg0, final String arg1) {

            return arg1.matches(".*" + FileSuffixes.TRUTH.getSuffix());
        }
    }

    /**
     * File filter class for raw packet files.
     * 
     *
     */
    private static class PacketFileFilter implements FilenameFilter {

        /**
         * {@inheritDoc}
         * 
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        @Override
        public boolean accept(final File arg0, final String arg1) {
            String argLower = arg1.toLowerCase();
            return argLower.matches(".*" + FileSuffixes.RAW_PKT.getSuffix().toLowerCase());
        }
    }

    /**
     * Main application entry point.
     * 
     * @param args
     *            Command line arguments from the user
     */
    @CoverageIgnore
    public static void main(final String[] args) {

        final DataMergeApp app = new DataMergeApp();

        // Parse the command line arguments
        try {

            /*
             * MPCS-7750 - 10/23/15. Changed to use creatoOptions() rather
             * than creating a new options object.
             */
            final ICommandLine commandLine = app.createOptions()
                    .parseCommandLine(args, true);
            app.configure(commandLine);
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(e.getMessage());

            System.exit(COMMAND_LINE_FAILURE);
        }

        // Initialize the application
        final boolean ok = app.init();
        if (!ok) {
            System.exit(INIT_FAILURE);
        }
        try {
            System.exit(app.run());

        } catch (final Exception e) {
            // something totally unexpected happened
            e.printStackTrace();
            System.exit(RUN_FAILURE);
        }
    }
}
