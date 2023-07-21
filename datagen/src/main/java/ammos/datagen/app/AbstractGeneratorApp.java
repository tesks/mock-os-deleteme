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
package ammos.datagen.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import ammos.datagen.cmdline.DatagenOptions;
import ammos.datagen.config.IMissionConfiguration;
import ammos.datagen.config.IRunConfiguration;
import ammos.datagen.generators.DeltaSclkGenerator;
import ammos.datagen.generators.FileSeededSclkGenerator;
import ammos.datagen.generators.ISclkGenerator;
import ammos.datagen.generators.PacketHeaderGenerator;
import ammos.datagen.generators.seeds.FileSeededSclkGeneratorSeed;
import ammos.datagen.generators.seeds.IPacketSeedMaker;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.seeds.PacketHeaderGeneratorSeed;
import ammos.datagen.generators.util.GeneratorStatistics;
import ammos.datagen.generators.util.TruthFile;
import ammos.datagen.generators.util.UsageTrackerMap;
import jpl.gds.ccsds.api.packet.IPacketFormatDefinition;
import jpl.gds.ccsds.api.packet.IPacketFormatDefinition.TypeName;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketFormatFactory;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.LoggingConstants;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.CoarseFineEncoding;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;

/**
 * This is an abstract application class to be extended by data generator
 * application classes for generators that require a mission configuration, a
 * run configuration, a dictionary, and an output directory. This class provides
 * common command line parsing, as well as methods to track and report progress
 * of data file generation, and to handle quit signals. It also manages the data
 * and truth file streams. It extends AbstractCommandLineApp, which provides the
 * framework for command line definition and parsing.
 * 
 *
 * MPCS-7750 - 10/23/15. Updated throughout to accommodate
 *          changes to BaseCommandOptions.
 * 
 */
public abstract class AbstractGeneratorApp extends AbstractCommandLineApp implements IQuitSignalHandler {

    /* MPCS-7229 - 4/20/2015. Added exit status values. */
    /**
     * Any kind of error status.
     */
    public static final int ERROR = 1;
    /**
     * Success status.
     */
    public static final int SUCCESS = 0;

    /**
     * Percentage of completion after which generation of mandatory invalid data
     * is enabled.
     */
    protected static final double ENABLE_INVALID_PERCENT = 10.0;

    /**
     * Starting size for fill packets;
     */
    public static final long STARTING_FILL_SIZE = 512;

    // These store command line values
    /** Main dictionary file name */
    protected String dictFile;
    private String missionConfigFile;
    private String outputDir;
    private String runConfigFile;
    private boolean generateTruth;

    // These are used in progress reporting
    private int lastPercentReported;
    private long lastReportTime;
    private int reportInterval;
    private long startTime;
    private long desiredBytes;
    /* MPCS-6864 - 12/5/14. Add desired packet count. */
    /** Number of desired packets */
    protected long desiredPackets;

    // These are the output objects
    private GeneratorStatistics stats = new GeneratorStatistics();
    private UsageTrackerMap trackers = new UsageTrackerMap();
    private TruthFile truthFile;
    private FileOutputStream dataStream;
    private String statsFileName;
    /** Console logger that routes also to a file */
    protected Tracer                            statusLogger;


    // These hold the loaded configuration data
    private IMissionConfiguration missionConfig;
    private IRunConfiguration runConfig;

    // These variables control graceful exit even if there is a TERM signal
    private final AtomicBoolean readyToExit = new AtomicBoolean(true);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(true);

    /* MPCS-7229 - 4/20/15. Added exit status */
    private int exitStatus = SUCCESS;

    // Generators common to all generator applications
    private ISclkGenerator sclkGen;
    private Map<Integer, PacketHeaderGenerator> packetGenerators;
    /** Generator for fill packets */
    protected PacketHeaderGenerator fillPacketGenerator;

    /** This is the percentage of fill packets */
    protected float fillPercent;

    // This is the randomizer for fill packet generation.
    private final Random fillRandomizer = new Random();

    // Indicates if schema source (mission) is needed on the command line
    // MPCS-6864 - 12/3/14. Added member.
    private boolean needSourceSchema = true;

    // Indicates if dictionary is needed on the command line
    // MPCS-6864 - 12/3/14. Added member.
    private boolean needDictionary = true;
    private IPacketFormatDefinition packetFormat;
    
    /** The current application context */
    protected ApplicationContext                appContext;
    
    /*  MPCS-9375 - 1/3/18 - Added number of data files */
    /** Number of data files to write */
    protected int numFiles = 1;
    
    /**
     * Constructor.
     */
    protected AbstractGeneratorApp() {
        super();
    }
    
    /**
     * Setup the log file. From this point on, logs will go to both the
     * console and the data generator log file indicated, because the
     * tracer is re-defined here.
     * 
     * @param logFileName log file name to use, no directory path
     * 
     * MPCS-9375 - 1/2/18 - Split out from init, use different logger
     */
    protected void initLogFile(final String logFileName) {
        final String logFile = Paths.get(this.outputDir, logFileName).toAbsolutePath().toString();
        MDC.put(LoggingConstants.FILE_APP_LOG_PATH, logFile);
        /* MPCS-9595 -  5/2/18 - Use status tracer, which has a log file appender */
        this.statusLogger = TraceManager.getTracer(Loggers.DATAGEN_STATUS);
    }

    /**
     * Initializes generator before use. This involves initializing the log
     * files, logging command line information, loading configuration files,
     * loading the dictionary, and opening output files. Abstract methods are
     * invoked where these capabilities differ between generators.
     * 
     * @param logFileName
     *            path to the console log file
     * @param truthFileName
     *            path to the truth file
     * @param dataFileName
     *            path to the data file
     * @param statsFileName
     *            path to the statistics file
     * 
     * @return true if initialization succeeded, false if not
     */
    public boolean init(final String logFileName, final String truthFileName,
            final String dataFileName, final String statsFileName) {


        /*  MPCS-9375 - 1/3/18 - Use new function to init logging BEFORE spring context */
        initLogFile(logFileName);

        appContext = SpringContextFactory.getSpringContext(true);

        /*
         * Set the statistics file path. Used at shutdown.
         */
        this.statsFileName = statsFileName;

        /*
         * Set default global statistics object and usage tracker map. The
         * subclass may override these defaults by calling setStatistics() and
         * setTrackers().
         */
        UsageTrackerMap.setGlobalTrackers(this.trackers);
        GeneratorStatistics.setGlobalStatistics(this.stats);

        /*
         * Log current command line configuration to the console log.
         */
        logCommandLine();

        /*
         * Load the configuration files using the subclass method. The subclass
         * method should in turn set the runConfig and missionConfig objects it
         * creates back into the member variables in this class.
         */
        if (!loadConfiguration()) {
            return false;
        }

        /*
         * Set GDS configuration properties needed to use AMPCS classes.
         */
        configureAmpcsMission();

        /*
         * Load dictionaries needed by the generating using the subclass method.
         * 
         * 12/3/14 - MPCS-6864. Made dictionary optional.
         */
        if (this.needDictionary && !loadDictionary()) {
            return false;
        }

        /*
         * Create the fill packet header generator if so configured.
         */
        createFillGenerator();
        
        /*  MPCS-9375 - 1/3/18 - Initialize number of desired files from config. If not one, 
         * there will be more than one set of data and truth files, so append a .1 to the file names
         * below. 
         */
        
        numFiles = this.runConfig.getIntProperty(IRunConfiguration.DESIRED_NUM_FILES, 1);
        final boolean multiFile = numFiles != 1;

        /*
         * Create the truth file object and open the data file stream.
         */
        return openFiles(truthFileName  + (multiFile ? ".1" : ""), dataFileName + (multiFile ? ".1" : ""));

    }

    /**
     * Sets the last percent of completion reported in progress logs.
     * 
     * @param lastPercentReported
     *            the last reported percentage.
     */
    protected void setLastPercentReported(final int lastPercentReported) {

        this.lastPercentReported = lastPercentReported;
    }

    /**
     * Sets the last time that completion status was reported in the progress
     * logs.
     * 
     * @param lastReportTime
     *            time of last report, milliseconds since epoch
     */
    protected void setLastReportTime(final long lastReportTime) {

        this.lastReportTime = lastReportTime;
    }

    /**
     * Gets the last percent of completion reported in progress logs.
     * 
     * @return the last reported percentage.
     */
    protected int getLastPercentReported() {

        return this.lastPercentReported;
    }

    /**
     * Gets the last time that completion status was reported in the progress
     * logs.
     * 
     * @return time of last report, milliseconds since epoch
     */
    protected long getLastReportTime() {

        return this.lastReportTime;
    }

    /**
     * Sets the time interval at which completion status should be reported.
     * 
     * @return report interval, in milliseconds
     */
    protected int getReportInterval() {

        return this.reportInterval;
    }

    /**
     * Gets the start time of the current generator run.
     * 
     * @return start time, in milliseconds since epoch
     */
    protected long getStartTime() {

        return this.startTime;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {

        super.configure(commandLine);

        /*
         * MPCS-7750 - 10/23/15. Use new option parsing methodology
         * throughout.
         */

        /* 10/6/14 - MPCS-6698. Add source schema option. */
        /* 12/3/14 - MPCS-6864. Made source schema optional */
        if (this.needSourceSchema) {
            /* 9/16/15 - MPCS-7679. Add default for source schema */
            String sourceMission = DatagenOptions.SOURCE_SCHEMA.parse(
                    commandLine, false);
            if (sourceMission == null) {
                sourceMission = "mm";
            }
            /*
             * MPCS-7679 - 9/16/15. Schema name is no longer mission +
             * sse. It is just sse. We pretend the schema name is the mission so
             * the right configuration will be loaded. In the sse case, there is
             * no mission name. We do not want to load an SSE config that is
             * mission specific. Believe it or not, setting the mission name to
             * nothing works.
             */
            if (sourceMission.equals("sse")) {
                appContext.getBean(SseContextFlag.class).setApplicationIsSse(true);
                GdsSystemProperties.setSystemMission("");
            } else {
                GdsSystemProperties.setSystemMission(sourceMission);
            }
            validateSourceSchema(sourceMission);
        }
        /* 12/3/14 - MPCS-6864. Made dictionary optional */
        if (this.needDictionary) {
            this.dictFile = DatagenOptions.DICTIONARY.parse(commandLine, true);
        }
        this.outputDir = DatagenOptions.OUTPUT_DIRECTORY.parse(commandLine,
                true);
        this.missionConfigFile = DatagenOptions.MISSION_CONFIG.parse(
                commandLine, true);
        this.runConfigFile = DatagenOptions.RUN_CONFIG.parse(commandLine, true);
        this.generateTruth = DatagenOptions.GENERATE_TRUTH.parse(commandLine);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
     */
    @Override
    public DatagenOptions createOptions() {

        /*
         * MPCS-7750 - 10/23/15. Override return type with DatagenOptions.
         * Use new option creation methodology throughout.
         */
        final DatagenOptions options = new DatagenOptions(this);

        /* 12/3/14 - MPCS-6864. Made dictionary optional */
        if (this.needDictionary) {
            options.addOption(DatagenOptions.DICTIONARY);
        }
        options.addOption(DatagenOptions.OUTPUT_DIRECTORY);
        options.addOption(DatagenOptions.MISSION_CONFIG);
        options.addOption(DatagenOptions.RUN_CONFIG);
        options.addOption(DatagenOptions.GENERATE_TRUTH);
        /* 10/6/14 - MPCS-6698. Added source schema option. */
        /* 12/3/14 - MPCS-6864. Made source schema optional */
        if (this.needSourceSchema) {
            options.addOption(DatagenOptions.SOURCE_SCHEMA);
        }

        return options;
    }

    /**
     * Validates the source schema designator.
     * 
     * @param schemaName
     *            the input schema name from the command line
     * @throws ParseException
     *             if the entry is not valid
     * 
     * 10/6/14 - MPCS-6698. Added method.
     */
    protected void validateSourceSchema(final String schemaName)
            throws ParseException {
        if (GdsSystemProperties.getSystemProperty("datagen.test") != null) {
            return;
        }
        final String configPath = GdsSystemProperties
                .getSystemConfigDir()
                + File.separator
                + schemaName
                + File.separator + GdsHierarchicalProperties.CONSOLIDATED_PROPERTY_FILE_NAME;
        if (!new File(configPath).exists()) {
            throw new ParseException("There is no "
                    + GdsHierarchicalProperties.CONSOLIDATED_PROPERTY_FILE_NAME
                    + " for the specified source schema " + schemaName);

        }
        if (schemaName.equalsIgnoreCase("monitor")) {
            throw new ParseException(
                    "Data cannot be generated for the 'monitor' mission or schema");
        }
    }

    /**
     * Writes a log message summarizing the generator output location and then
     * writes the current global statistics and usage tracking information to
     * the statistics file.
     */
    protected void writeStatistics() {

        this.statusLogger.info(this.stats.getTotalPacketSize()
                + " bytes of packet data written");
        this.statusLogger.info("All output written to directory "
                + this.outputDir);
        final String statsFile = this.outputDir + File.separator
                + this.statsFileName;
        this.stats.writeToFile(statsFile);
        this.trackers.writeToFile(statsFile, true);
    }

    /**
     * Logs the command line configuration to the log file.
     */
    private void logCommandLine() {

        final File mcf = new File(this.missionConfigFile);
        final File rcf = new File(this.runConfigFile);
        final File dir = new File(this.outputDir);

        this.statusLogger.info("Mission Configuration File: "
                + mcf.getAbsolutePath());
        this.statusLogger.info("Configuration Last Modified: "
                + new Date(mcf.lastModified()));
        this.statusLogger.info("Run Configuration File: "
                + rcf.getAbsolutePath());
        this.statusLogger.info("Configuration Last Modified: "
                + new Date(rcf.lastModified()));
        /* 12/3/14 - MPCS-6864. Made dictionary optional. */
        if (this.needDictionary) {
            final File dict = new File(this.dictFile);
            this.statusLogger
            .info("Dictionary File: " + dict.getAbsolutePath());
            this.statusLogger.info("Dictionary Last Modified: "
                    + new Date(dict.lastModified()));
        }
        this.statusLogger.info("Output Directory: " + dir.getAbsolutePath());
    }

    /**
     * Opens the truth and data files.
     * @param truthFile base name of the truth file (no path)
     * @param dataFile base name of the data file (no path)
     * @return true if the operation succeeded, false if not
     * 
     * MPCS-9375 - 01/02/18. Made protected.
     */
    protected boolean openFiles(final String truthFile, final String dataFile) {

        if (this.generateTruth) {
            final String truthFileName = this.outputDir + File.separator
                    + truthFile;
            try {
                this.truthFile = new TruthFile(truthFileName, packetFormat);
            } catch (final IOException e) {
                this.statusLogger.error("Unable to open truth file "
                        + truthFileName + " (" + e.toString() + ")");
                return false;
            }
        }

        final String dataFileName = this.outputDir + File.separator + dataFile;
        try {
            this.dataStream = new FileOutputStream(dataFileName);
        } catch (final IOException e) {
            this.statusLogger.error("Unable to open data file " + dataFileName
                    + " (" + e.toString() + ")");
            return false;
        }

        return true;
    }

    /**
     * Closes the truth and data files.
     * 
     * MPCS-9375 - 01/02/18. Made protected.
     */
    protected void closeFiles() {

        if (this.truthFile != null) {
            this.truthFile.close();
        }

        try {
            if (this.dataStream != null) {
                this.dataStream.close();
            }
        } catch (final IOException e) {
            this.statusLogger.warn("IO Error closing data file", e);
        }
    }

    /*
     * Creates the packet header generator for fill packets if required by the
     * configuration.
     */
    private void createFillGenerator() {

        /*
         * Get the fill percentage and the fill APID from the configuration.
         */
        this.fillPercent = this.runConfig.getFloatProperty(
                IRunConfiguration.DESIRED_FILL_PERCENT, (float) 0.0);
        final int fillApid = this.missionConfig.getIntProperty(
                IMissionConfiguration.FILL_PACKET_APID, 2047);

        if (this.fillPercent > 0.0) {
            this.fillPacketGenerator = new PacketHeaderGenerator(this.packetFormat);
            final PacketHeaderGeneratorSeed packetSeed = new PacketHeaderGeneratorSeed();
            packetSeed.setApid(fillApid);
            this.fillPacketGenerator.setSeedData(packetSeed);
        }
    }

    /**
     * Generates a fill packet to the output file, if so configured and the
     * randomizer determines we should do so. Will update packet statistics and
     * write the fill packet to the truth file also.
     * 
     * @return true if a fill packet was generated, false if not
     * @throws IOException
     *             if there is a problem writing the fill packet
     */
    protected boolean generateFillPacket() throws IOException {

        boolean needFill = false;

        /*
         * Not generating fill packets, so just return.
         */
        if (this.fillPacketGenerator == null) {
            return false;
        }

        /*
         * Determine where we want a fill packet based upon desired % fill, a
         * random number, and whether we are generating enough fill data so far.
         */
        this.fillRandomizer.nextFloat();
        final float rand = this.fillRandomizer.nextFloat() * (float) 100.0;
        if (rand <= this.fillPercent
                && this.stats.getFillPercent() <= this.fillPercent) {
            needFill = true;
        }

        /*
         * We need a fill packet.
         */
        if (needFill) {
            /*
             * Create fill packets that average the same size as the data
             * packets we have generated. If no data packets have been generated
             * yet, we use a default size.
             */
            long averageSize = this.stats.getAveragePacketSize();
            if (averageSize == 0) {
                averageSize = STARTING_FILL_SIZE;
            }

            /*
             * Generate packet header.
             */
            final ISpacePacketHeader header = (ISpacePacketHeader) this.fillPacketGenerator
                    .getNext();

            /*
             * Generate the SCLK and get the SCLK bytes.
             */
            final ISclk sclk = (ISclk) getSclkGenerator().getNext();
            final byte[] sclkBytes = sclk.getBytes();

            /*
             * Set the data length in the header. Must be a less one value.
             */
            header.setPacketDataLength(sclkBytes.length + (int) averageSize - 1);
            final byte[] headerBytes = header.getBytes();

            /*
             * Generate packet bytes.
             */
            final byte[] packetBytes = new byte[(int) averageSize];
            Arrays.fill(packetBytes, (byte) 0xFF);

            /*
             * Compute packet size.
             */
            final int packetSize = headerBytes.length + sclkBytes.length
                    + packetBytes.length;

            /*
             * Write all the bytes to the output file: header, sclk, fill body.
             */
            writeToDataStream(headerBytes);
            writeToDataStream(sclkBytes);
            writeToDataStream(packetBytes);

            // Write fill packet info to the truth file
            if (isGenerateTruth()) {
                getTruthFile().writeFillPacket(header, sclk);
            }

            // Update the fill packet statistics
            getStatistics().updateFillPacketStatistics(packetSize,
                    header.getApid(), sclk, System.currentTimeMillis());
        }
        return needFill;
    }

    /**
     * Creates the packet SCLK generator. Must be invoked by the subclass,
     * supplying the data generator-specific seed maker.
     * 
     * @param seedMaker
     *            the packet seed maker object specific to this generator
     * @return true if success, false if not
     */
    protected boolean createSclkGenerator(final IPacketSeedMaker seedMaker) {

        final ISeedData seed = seedMaker.createSclkGeneratorSeed();
        try {
            if (seed instanceof FileSeededSclkGeneratorSeed) {
                this.sclkGen = new FileSeededSclkGenerator();
                this.sclkGen.setSeedData(seed);
                return ((FileSeededSclkGenerator) this.sclkGen).load();
            } else {
                this.sclkGen = new DeltaSclkGenerator();
                this.sclkGen.setSeedData(seed);
            }
        } catch (final InvalidSeedDataException e) {
            this.statusLogger.error("Unexpected Exception: " + ExceptionTools.getMessage(e));
            return false;
        }
        return true;
    }

    /**
     * Retrieves the packet SCLK generator. The generator must first be created
     * by calling createSclkGenerator() or this method will throw.
     * 
     * @return SCLK generator
     */
    protected ISclkGenerator getSclkGenerator() {

        if (this.sclkGen == null) {
            throw new IllegalStateException(
                    "Packet SCLK generator has not been created");
        }
        return this.sclkGen;
    }

    /**
     * Creates the packet header generators. Must be invoked by the subclass,
     * supplying the data generator-specific seed maker.
     * 
     * @param seedMaker
     *            the packet seed maker object specific to this generator
     * @return true if success, false if not
     */
    protected boolean createPacketHeaderGenerators(
            final IPacketSeedMaker seedMaker) {

        /*
         * Ask the application-specific seed maker object to create the packet
         * header generator seeds. If there are no such seeds defined, return
         * failure.
         */
        final Map<Integer, PacketHeaderGeneratorSeed> packetSeeds = seedMaker
                .createPacketHeaderGeneratorSeeds();

        /*
         * Packet header generators go into a map accessed by packet APID.
         */
        this.packetGenerators = new HashMap<>();
        try {
            /*
             * Create a packet header generator for each packet generator seed
             * and install it into the map.
             */
            for (final PacketHeaderGeneratorSeed seed : packetSeeds.values()) {
                final PacketHeaderGenerator gen = new PacketHeaderGenerator(this.packetFormat);
                gen.setSeedData(seed);
                this.packetGenerators.put(seed.getApid(), gen);
            }
        } catch (final InvalidSeedDataException e) {
            this.statusLogger.error("Unexpected Exception: " + ExceptionTools.getMessage(e));
            return false;
        }
        return true;
    }

    /**
     * Retrieves the packet header generator for a specific packet APID. The
     * generators must first be created by calling
     * createPacketHeaderGenerators() or this method will throw.
     * 
     * @param apid
     *            the packet APID
     * @return the PacketHeaderGenerator object for the given APID, or null if
     *         none exists
     */
    protected PacketHeaderGenerator getPacketHeaderGenerator(final int apid) {

        if (this.packetGenerators == null) {
            throw new IllegalStateException(
                    "Packet headers generators have not been created");
        }
        return this.packetGenerators.get(apid);
    }

    /**
     * Initializes variables for progress reporting. Should be called
     * immediately prior to start of packet generation.
     */
    protected void initStartTime() {

        this.startTime = System.currentTimeMillis();
        this.lastReportTime = this.startTime;
        this.reportInterval = this.runConfig.getIntProperty(
                IRunConfiguration.DESIRED_REPORT_INTERVAL, 5);
        this.reportInterval = this.reportInterval * 60 * 1000;
        this.desiredBytes = this.runConfig.getLongProperty(
                IRunConfiguration.DESIRED_FILE_SIZE, 0);
        /*  MPCS-9375 - 1/3/18 - Desired bytes must be multiplied by 
         * number of desired files 
         */
        this.desiredBytes = this.desiredBytes * numFiles;
        /* MPCS-6864 - 12/5/14. Add init of desired packet count. */
        this.desiredPackets = this.runConfig.getIntProperty(
                IRunConfiguration.DESIRED_NUM_PACKETS, 0);
    }

    /**
     * Clears shutdown flags for start of data generation. Should be called
     * immediately prior to start of packet generation.
     */
    protected void clearShutdownFlags() {

        this.readyToExit.set(false);
        this.shuttingDown.set(false);
    }

    /**
     * Sets shutdown flags for clean exit. Should be called at end of packet
     * generation.
     */
    protected void setShutdownFlags() {

        this.readyToExit.set(true);
        this.shuttingDown.set(true);
    }

    /**
     * Indicates whether the application has received a shutdown request.
     * 
     * @return true if shutdown pending, false if not
     */
    protected boolean isShuttingDown() {

        return this.shuttingDown.get();
    }

    /**
     * Writes bytes to the data output file.
     * 
     * @param data
     *            bytes to write
     * @throws IOException
     *             if an IO error occurs during the write
     */
    protected void writeToDataStream(final byte[] data) throws IOException {

        this.dataStream.write(data);
        this.dataStream.flush();
    }

    /**
     * Indicates whether truth data is to be generated. Set via command line
     * flag.
     * 
     * @return true if truth should be generated, false if not
     */
    public boolean isGenerateTruth() {

        return this.generateTruth;
    }

    /**
     * Retrieves the TruthFile object used to write truth data.
     * 
     * @return TruthFile object, or null if no truth being generated
     */
    protected TruthFile getTruthFile() {

        return this.truthFile;
    }

    /**
     * Gets the path to the mission configuration file; set via command line
     * option.
     * 
     * @return file path to mission configuration
     */
    public String getMissionConfigFile() {

        return this.missionConfigFile;
    }

    /**
     * Gets the path to the run configuration file; set via command line option.
     * 
     * @return file path to run configuration
     */
    public String getRunConfigFile() {

        return this.runConfigFile;
    }

    /**
     * Gets the path to the dictionary file; set via command line option.
     * 
     * @return file path to dictionary
     */
    protected String getDictionaryFile() {

        return this.dictFile;
    }

    /**
     * Sets the parsed run configuration object for this generator. The parsing
     * must occur in the subclass.
     * 
     * @param runConfig
     *            the IRunConfiguration object to set
     */
    protected void setRunConfig(final IRunConfiguration runConfig) {

        this.runConfig = runConfig;
    }

    /**
     * Sets the parsed mission configuration object for this generator. The
     * parsing must occur in the subclass.
     * 
     * @param missionConfig
     *            the IMissionConfiguration object to set
     */
    protected void setMissionConfig(final IMissionConfiguration missionConfig) {

        this.missionConfig = missionConfig;
    }

    /**
     * Sets the global statistics object for this generator. The object must be
     * created by the subclass.
     * 
     * @param stats
     *            GeneratorStatistics object to set
     */
    protected void setStatistics(final GeneratorStatistics stats) {

        this.stats = stats;
        GeneratorStatistics.setGlobalStatistics(this.stats);
    }

    /**
     * Gets the global statistics object for this generator.
     * 
     * @return GeneratorStatistics object
     */
    protected GeneratorStatistics getStatistics() {

        return this.stats;
    }

    /**
     * Sets the global usage tracker map object for this generator. The object
     * must be created by the subclass.
     * 
     * @param trackers
     *            UsageTrackerMap object to set
     */
    protected void setTrackers(final UsageTrackerMap trackers) {

        this.trackers = trackers;
        UsageTrackerMap.setGlobalTrackers(this.trackers);
    }

    /**
     * Pulls values from the data generator configuration to populate the
     * configuration necessary to use AMPCS classes we are dependent upon. This
     * method sets AMPCS configuration values common to all data generators.
     */
    protected void configureAmpcsMission() {

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
        TimeProperties.getInstance().setCanonicalSclkEncoding(sclkEncoding);
        TimeProperties.getInstance().setSclkFormatter(sclkFmt);

        /*  MPCS-7663 - 9/10/15. Configure the packet adaptation. */
        final String packetClass = this.missionConfig.getStringProperty(
                IMissionConfiguration.PACKET_HEADER_CLASS, 
                IPacketFormatDefinition.TypeName.CCSDS.getDefaultPacketHeaderClass());
        try {
            final IPacketFormatDefinition.TypeName type = TypeName.valueOf(packetClass);
            packetFormat = PacketFormatFactory.create(type);
        } catch (final IllegalArgumentException e) {
            packetFormat = PacketFormatFactory.create(TypeName.CUSTOM_CLASS, packetClass);
        }
       
    }

    /**
     * Reports current progress to the log file. Progress is reported at every
     * 10% of completion and at the configured report interval in the run
     * configuration. Percent complete is currently determined by comparing
     * total amount of packet data generated (from the global statistics object)
     * to the desired amount as expressed in the run configuration. This method
     * also enables the invalid flags in the global statistics object when a
     * certain percentage of completion is reached, signaling underlying
     * generators that they can begin to inject invalid values.
     */
    protected void reportProgress() {

        /*
         * Compute percent of the data file that is complete and get the current
         * time - MPCS-6864 - 12/5/14. Add option to use desired packet
         * count.
         */
        int percent = 0;
        if (this.desiredBytes != 0) {
            percent = (int) ((Float.valueOf(this.stats.getTotalPacketSize()) / Float
                    .valueOf(this.desiredBytes)) * 100);
        }
        else if (this.desiredPackets != 0) {
            percent = (int) ((Float.valueOf(this.stats.getTotalPacketCount()) / Float
                    .valueOf(this.desiredPackets)) * 100);
        }
        final long currentTime = System.currentTimeMillis();
        if (percent > 100) {
            percent = 100;
        }

        /*
         * If we have completed 10%, enable generation of the minimal invalid
         * data. This keeps all the invalid data from being generated as the
         * very first thing in the file.
         * 
         * @ToDo("Make this percentage configurable")
         */
        if (percent > ENABLE_INVALID_PERCENT) {
            this.stats.enableGetAndSetInvalidFlags(true);
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
            this.statusLogger.info(percent
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
     * Sets the flag indicating if the schema source command line option is
     * required. This flag defaults to true. This allows the option to be
     * disabled if the mission or source schema is known. Must be invoked before
     * the call to createOptions to be effective.
     * 
     * @param need
     *            true to enable the command line option, false to disable
     * 
     * MPCS-6864 - 12/3/14. Added method.
     */
    protected void setNeedSourceSchema(final boolean need) {
        this.needSourceSchema = need;
    }

    /**
     * Sets the flag indicating if the dictionary command line option is
     * required. This flag defaults to true. This allows the option to be
     * disabled if the dictionary is known or not required. Must be invoked
     * before the call to createOptions to be effective.
     * 
     * @param need
     *            true to enable the command line option, false to disable
     * 
     * MPCS-6864 - 12/3/14. Added method.
     */
    protected void setNeedDictionary(final boolean need) {
        this.needDictionary = need;
    }

    /**
     * Method to load the mission and run configuration files. Must be
     * implemented by the subclass. The subclass method should call
     * setRunConfig() and setMissionConfig() to establish the configuration
     * objects in this class.
     * 
     * @return true if configuration load was successful, false if not
     */
    protected abstract boolean loadConfiguration();

    /**
     * Method to load the the dictionar(ies) required by the current data
     * generator. Must be implemented by the subclass.
     * 
     * @return true if dictionary load was successful, false if not
     */
    protected abstract boolean loadDictionary();

    /**
     * Gets the application exit status: SUCCESS or ERROR
     * 
     * @return exit status
     * 
     * MPCS-7229 - 4/20/15. Added method.
     */
    public int getExitStatus() {
        return this.exitStatus;
    }

    @Override
    public void exitCleanly() {
        
        final boolean alreadyShutdown = AbstractGeneratorApp.this.shuttingDown.getAndSet(true);

        /*
         * If shutting down flag is already set, the run method has exited. Do
         * not report premature exit. In either case, set the flag, which will
         * cause the run method to stop when the current packet is complete.
         */
        if (!alreadyShutdown) {
            if (AbstractGeneratorApp.this.statusLogger != null) {
                AbstractGeneratorApp.this.statusLogger.warn(ApplicationConfiguration.getApplicationName()
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
            while (!AbstractGeneratorApp.this.readyToExit.get()) {
                try {
                    SleepUtilities.fullSleep(500);
                } catch (final ExcessiveInterruptException e) {
                    // someone really wants out?
                    break;
                }
            }
        }

        /*
         * Write out the statistics and usage tracking information upon
         * shutdown.
         * 
         * MPCS-7229 - 4/20/15. Added check for exit status.
         */ 
        if (getExitStatus() == SUCCESS && !alreadyShutdown) {
            writeStatistics();
        }

        /*
         * Close output files before exiting.
         */
        closeFiles();

    }

    /**
     * Sets the application exit status: SUCCESS or ERROR
     * 
     * @param exitStatus
     *            status to set
     * 
     * MPCS-7229 - 4/20/15. Added method.
     */
    public void setExitStatus(final int exitStatus) {
        this.exitStatus = exitStatus;
    }

    /**
     * Return output directory
     * 
     * @return output dir
     * 
     * MPCS-9586 - 4/3/18. Added method.
     */
    public String getOutputDir() {
        return outputDir;
    }

}
