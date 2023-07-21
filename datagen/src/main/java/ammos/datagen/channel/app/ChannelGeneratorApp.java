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
package ammos.datagen.channel.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.ParseException;

import ammos.datagen.app.AbstractGeneratorApp;
import ammos.datagen.channel.config.ChannelMissionConfiguration;
import ammos.datagen.channel.config.ChannelPacketMode;
import ammos.datagen.channel.config.ChannelRunConfiguration;
import ammos.datagen.channel.generators.ByApidChannelBodyGenerator;
import ammos.datagen.channel.generators.CustomChannelBodyGenerator;
import ammos.datagen.channel.generators.RandomChannelBodyGenerator;
import ammos.datagen.channel.generators.seeds.ChannelBodyGeneratorSeed;
import ammos.datagen.channel.generators.seeds.ChannelSeedMaker;
import ammos.datagen.channel.util.ChannelGeneratorStatistics;
import ammos.datagen.channel.util.ChannelUsageTrackerMap;
import ammos.datagen.config.InvalidConfigurationException;
import ammos.datagen.generators.ISeededGenerator;
import ammos.datagen.generators.PacketHeaderGenerator;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.FileSuffixes;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.channel.IChannelDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.annotation.CoverageIgnore;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.Pair;

/**
 * This is a test data generator application class that will create a file of
 * binary channel packets according to a flight project dictionary. It requires
 * a channel mission configuration file, a channel run configuration file, a
 * channel dictionary file, and an output directory name. It will produce a
 * CCSDS-compliant channel packet file, a truth data file detailing each packet
 * in the packet file, a statistics file supplying statistics about the packet
 * file and the generation process, and a log file, which will contain errors
 * and progress reports.
 * <p>
 * This is an AMPCS application extension. As such, it relies upon classes in
 * the AMPCS shared library and the AMPCS multimission core. It shares the AMPCS
 * configuration methods, as well. This means it expects to load the GDS
 * configuration files GdsSystemConfig.xml, TimeSystemConfig.xml, and
 * LogSystemConfig.xml at startup.
 * 
 *
 */
public class ChannelGeneratorApp extends AbstractGeneratorApp {

    private static final String CHANNEL = "channel";

    /**
     * The output file name for the channel data file.
     */
    public static final String DATA_FILE_NAME = CHANNEL
            + FileSuffixes.RAW_PKT.getSuffix();

    /**
     * The output file name for the log file.
     */
    public static final String LOG_FILE_NAME = CHANNEL + "_generator"
            + FileSuffixes.LOG.getSuffix();

    /**
     * The output file name for the truth file.
     */
    public static final String TRUTH_FILE_NAME = CHANNEL
            + FileSuffixes.TRUTH.getSuffix();
    /**
     * The output file name for the statistics file.
     */
    public static final String STATS_FILE_NAME = CHANNEL
            + FileSuffixes.STATISTICS.getSuffix();

    // Generators unique to this application
    private ISeededGenerator channelBodyGen;

    // These hold the loaded configuration data
    private ChannelMissionConfiguration channelMissionConfig;
    private ChannelRunConfiguration channelRunConfig;

    // / The dictionary
    private IChannelDictionary channelDict;

    // Needed for progress reporting in custom mode.
    private long desiredPacketCount;

	private DictionaryProperties dictConfig;

    /**
     * Basic constructor. Must call configure() and init() to initialize this
     * instance.
     */
    public ChannelGeneratorApp() {
        super();
    }

    /**
     * Initializes the channel generator before use. This involves loading the
     * configuration files and the dictionary, and creating the necessary
     * generator objects.
     * 
     * @return true if initialization succeeded, false if not
     * 
     * MPCS-7229 - 4/20/15. Set exit status to error upon false return
     */
    public boolean init() {
        
        /*
         * Superclass method sets up the log, statistics, and initial truth and
         * data files. It invokes methods in this class to load the
         * configuration and the dictionary.
         */
        if (!super.init(LOG_FILE_NAME, TRUTH_FILE_NAME, DATA_FILE_NAME,
                STATS_FILE_NAME)) {
            setExitStatus(ERROR);
            return false;
        }

        /*
         * Set up the statistics and usage tracker objects for this run.
         */
        setStatistics(new ChannelGeneratorStatistics());
        setTrackers(new ChannelUsageTrackerMap());

        /*
         * Initialize variables for progress reporting.
         */
        this.desiredPacketCount = this.channelRunConfig.getCustomPackets()
                .size();

        /*
         * Create the channel seed maker, and then use it to create necessary
         * data generators.
         */
        final ChannelSeedMaker seedMaker = new ChannelSeedMaker(
                this.channelMissionConfig, this.channelRunConfig,
                this.channelDict);
        try {
            seedMaker.validate();
        } catch (final InvalidConfigurationException e) {
            this.statusLogger.error(e.getMessage());
            setExitStatus(ERROR);
            return false;
        }
        boolean ok = createChannelBodyGenerator(seedMaker);
        ok = ok && createPacketHeaderGenerators(seedMaker);
        ok = ok && createSclkGenerator(seedMaker);

        if (!ok) {
            setExitStatus(ERROR);
        }
        return ok;
    }

    /**
     * Pulls values from the data generator configuration to populate the
     * configuration necessary to use AMPCS classes we are dependent upon. The
     * superclass method handles setup of generic AMPCS configuration such as
     * SCLK size. This method handles the setting of channel-specific adaptation
     * in AMPCS.
     */
    @Override
    protected void configureAmpcsMission() {

        /*
         * Handle generic AMPCS configuration in the super class.
         */
        super.configureAmpcsMission();

        dictConfig = appContext.getBean(DictionaryProperties.class);

        /*
         * Configure the AMPCS channel adaptation. First read the necessary
         * values out of the mission configuration, then set them into the AMPCS
         * GDS configuration.
         */
        final String chanDictClass = this.channelMissionConfig
                .getStringProperty(
                        ChannelMissionConfiguration.CHANNEL_DICTIONARY_CLASS,
                        null);
        /*
         * If this is null, we default to the value in the data generator's
         * GdsSystemConfig.xml file.
         */
        if (chanDictClass != null) {
            dictConfig
                    .setDictionaryClass(DictionaryType.CHANNEL, new DictionaryClassContainer(chanDictClass));
        }

    }

    /**
     * Creates the channel body generator, which will be responsible for
     * generating the binary bodies of channel packets.
     * 
     * @param seedMaker
     *            the channel seed maker object
     * @return true if success, false if not
     */
    private boolean createChannelBodyGenerator(final ChannelSeedMaker seedMaker) {

        final ChannelBodyGeneratorSeed chanSeed = seedMaker
                .createChannelBodyGeneratorSeed();
        try {
            if (this.channelRunConfig.getPacketMode() == ChannelPacketMode.RANDOM) {
                this.channelBodyGen = isGenerateTruth() ? new RandomChannelBodyGenerator(
                        getTruthFile()) : new RandomChannelBodyGenerator();
            } else if (this.channelRunConfig.getPacketMode() == ChannelPacketMode.BY_APID) {
                this.channelBodyGen = isGenerateTruth() ? new ByApidChannelBodyGenerator(
                        getTruthFile()) : new ByApidChannelBodyGenerator();
            } else {
                this.channelBodyGen = isGenerateTruth() ? new CustomChannelBodyGenerator(
                        getTruthFile()) : new CustomChannelBodyGenerator();
            }
            this.channelBodyGen.setSeedData(chanSeed);
        } catch (final InvalidSeedDataException e) {
            this.statusLogger.error("Unexpected exception: ", ExceptionTools.getMessage(e));
            return false;
        }
        return true;
    }

    /**
     * Uses AMPCS libraries to load the channel dictionary. Must be called after
     * configureAmpcsMission().
     * 
     * @see #configureAmpcsMission()
     * 
     * @return an IChannelDictionary object, or null if the load failed
     */
    @Override
    protected boolean loadDictionary() {

        /*
         * Create and load the channel dictionary using the AMPCS factory.
         */
        try {
            this.channelDict = appContext.getBean(IChannelDictionaryFactory.class)
                    .getNewInstance(dictConfig, getDictionaryFile());
            return true;
        } catch (final DictionaryException e) {
            this.statusLogger.error("Unable to load channel dictionary");
            return false;
        }
    }

    /**
     * Loads both the mission and run configuration files.
     * 
     * @return true if success, false if not
     */
    @Override
    protected boolean loadConfiguration() {

        // Load mission configuration
        this.channelMissionConfig = new ChannelMissionConfiguration();
        if (!this.channelMissionConfig.load(getMissionConfigFile())) {
            return false;
        }
        setMissionConfig(this.channelMissionConfig);

        // Load run configuration
        this.channelRunConfig = new ChannelRunConfiguration();
        if (!this.channelRunConfig.load(getRunConfigFile())) {
            return false;
        }
        setRunConfig(this.channelRunConfig);

        return true;
    }

    /**
     * Executes the main logic to generate the channel output file.
     * 
     * @throws IOException
     *             if there is an error opening or writing to the output file
     */
    @SuppressWarnings("unchecked")
    public void run() throws IOException {

        // Up to this point, we could shutdown without special handling.
        // This basically enables the shutdown handler in the superclass.
        clearShutdownFlags();

        try {
            long recordCounter = 1;

            // This is the desired number of bytes in the output file
            final long localDesiredBytes = this.channelRunConfig.getLongProperty(
                    ChannelRunConfiguration.DESIRED_FILE_SIZE, 0);

            // This is the maximum channel packet size
            final int maxPacket = this.channelMissionConfig.getIntProperty(
                    ChannelRunConfiguration.MAX_CHANNEL_PACKET_SIZE, 65535);

            // Record the run start time and start the last report time tracking
            initStartTime();
            
            /*  MPCS-9375 - 1/3/18 - Support generation of multiple files */            
            if (numFiles > 1) {
                this.statusLogger.info("Starting generation of fileset 1");
            }

            int numWrittenFiles = 0;
            
            /*  MPCS-9375 - 1/3/18 - Support generation of multiple files by
             * adding outer loop 
             */  
            while (numWrittenFiles < numFiles && !isShuttingDown() && !getSclkGenerator().isExhausted()) {
                final long startBytes = getStatistics().getTotalPacketSize();

                // Generate channel packets until the desired file size is reached
                while (getStatistics().getTotalPacketSize() - startBytes < localDesiredBytes
                        && !getSclkGenerator().isExhausted() && !isShuttingDown()) {

                    if (isGenerateTruth()) {
                        getTruthFile().writeRecord(recordCounter++);
                    }

                    /*
                     * See if it is time to generate a fill packet. If one is
                     * generated, report progress and skip everything else.
                     */
                    if (this.channelRunConfig.getPacketMode() != ChannelPacketMode.CUSTOM
                            && generateFillPacket()) {
                        reportProgress();
                        continue;
                    }

                    /*
                     * Generate a channel body.
                     */
                    final Pair<Integer, byte[]> chanPair = (Pair<Integer, byte[]>) this.channelBodyGen
                            .get();

                    /*
                     * If we are running in CUSTOM mode, the channel body generator
                     * is going to return null when the end of the custom packet
                     * list is reached, so we stop, regardless of file size
                     * generated.
                     */
                    if (chanPair == null) {
                        if (isGenerateTruth()) {
                            getTruthFile().writeLine("No Packet");
                        }
                        break;
                    }

                    final int apid = chanPair.getOne();
                    final byte[] chanBytes = chanPair.getTwo();

                    final PacketHeaderGenerator packetGen = getPacketHeaderGenerator(apid);
                    final ISpacePacketHeader header = (ISpacePacketHeader) packetGen
                            .getNext();

                    // Generate the SCLK
                    final ISclk sclk = (ISclk) getSclkGenerator().getNext();

                    // Get the byte representation of the current SCLK
                    final byte[] sclkBytes = sclk.getBytes();

                    // Now we know the packet body length - must be a less one value
                    // in the header
                    header.setPacketDataLength(sclk.getByteLength()
                            + chanBytes.length - 1);

                    // Get the bytes for the primary header
                    final byte[] headerBytes = header.getBytes();

                    // Compute total packet size and check against mission maximum
                    final int packetSize = headerBytes.length + sclkBytes.length
                            + chanBytes.length;

                    if (packetSize > maxPacket) {
                        getStatistics().incrementInvalidPacketCount();
                        this.statusLogger
                        .warn("Skipped writing packet with size ("
                                + packetSize
                                + "), which is greater than the configured channel packet maximum ("
                                + maxPacket + ")");
                        if (isGenerateTruth()) {
                            getTruthFile().writeLine("No Packet");
                        }
                        continue;
                    }

                    // Write all the bytes to the output file: header, sclk, channel
                    // body
                    writeToDataStream(headerBytes);
                    writeToDataStream(sclkBytes);
                    writeToDataStream(chanBytes);

                    // Write packet info to the truth file
                    if (isGenerateTruth()) {
                        getTruthFile().writePacket(header, sclk);
                    }

                    // Update the statistics
                    getStatistics().updatePacketStatistics(packetSize,
                            header.getApid(), sclk, System.currentTimeMillis());

                    // Compute and print progress
                    reportProgress();
                }

                /*  MPCS-9375 - 1/3/18 - Support generation of multiple files */  
                numWrittenFiles++;

                if (numWrittenFiles < numFiles) {

                    this.statusLogger.info("Completed generation of fileset " + numWrittenFiles);

                    final String truthFile = TRUTH_FILE_NAME  + "." + String.valueOf(numWrittenFiles + 1);
                    final String dataFile = DATA_FILE_NAME + "." + String.valueOf(numWrittenFiles + 1);

                    closeFiles();

                    if (!openFiles(truthFile, dataFile)) {
                        setExitStatus(ERROR);
                        break;
                    } else {
                        this.channelBodyGen.resetTruthFile(getTruthFile());
                        this.statusLogger.info("Starting generation of fileset " + String.valueOf(numWrittenFiles + 1));
                    }

                } 
            }
        } catch (final Exception e) {
            this.statusLogger.error("Unexpected exception: ", ExceptionTools.getMessage(e));
            /* MPCS-7229 - 4/20/15. Set ERROR status upon exception */
            setExitStatus(ERROR);
        }
        
        /*  MPCS-9375 - 1/3/18 - Fix bug with stats not coming out */
        writeStatistics();
        
        // We are ready to exit now. Tell the shutdown handler.
        setShutdownFlags();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.app.AbstractGeneratorApp#reportProgress()
     */
    @Override
    protected void reportProgress() {

        /*
         * Progress reporting in the super class is adequate unless we are in
         * CUSTOM mode.
         */
        if (this.channelRunConfig.getPacketMode() != ChannelPacketMode.CUSTOM) {
            super.reportProgress();
            return;
        }

        /*
         * Compute percent of the data file that is complete and get the current
         * time.
         */
        int percent = (int) ((Float.valueOf(getStatistics()
                .getTotalPacketCount()) / Float
                .valueOf(this.desiredPacketCount)) * 100);
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
            getStatistics().enableGetAndSetInvalidFlags(true);
        }

        /*
         * Report at 10% completion intervals, and at configured time interval.
         */
        if ((percent >= getLastPercentReported() + 10)
                || ((currentTime - getLastReportTime()) >= getReportInterval())) {

            /*
             * Compute remaining time, based upon what has been completed in the
             * elapsed time so far.
             */
            final long elapsed = System.currentTimeMillis() - getStartTime();
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
            setLastPercentReported(percent - percent % 10);
            setLastReportTime(currentTime);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        /* 0/6/14 - MPCS-6698. Update usage line */
        pw.println("Usage: "
                + ApplicationConfiguration.getApplicationName()
                + " --missionConfig <file path> --runConfig <file path> --dictionary <file path> --outputDir <directory> [--sourceSchema <schema name>]");
        pw.println("                   ");

        /* MPCS-7750 - 10/23/15. Get nested Options object. */
        final OptionSet options = createOptions().getOptions();

        /*
         * 10/6/14 - MPCS-6698. Added notes to help regarding source
         * schema settings.
         * 
         * MPCS-7679 - 9/16/15. Update help text to reflect new usage of
         * the source schema option.
         */

        options.printOptions(pw);
        pw.println("This is a test data generator application that will create a file of");
        pw.println("binary channel packets according to a flight project dictionary. It requires a");
        pw.println("channel mission configuration file, a channel run configuration file, a channel");
        pw.println("dictionary file, and an output directory name. It will produce a");
        pw.println("CCSDS-compliant channel packet file, a truth data file detailing each packet in");
        pw.println("the packet file, a statistics file supplying statistics about the packet file");
        pw.println("and the generation process, and a log file, which will contain errors and");
        pw.println("progress reports. If the source schema (mission) is supplied, then");
        pw.println("that will be the dictionary XML format expected by this tool. The source");
        pw.println("schema defaults to 'mm' (for multimission). This tool will accept 'mm',");
        pw.println("msl', 'smap', 'sse', or 'monitor'.");
        pw.flush();
    }

    /**
     * Main application entry point.
     * 
     * @param args
     *            Command line arguments from the user
     */
    @CoverageIgnore
    public static void main(final String[] args) {

        final ChannelGeneratorApp app = new ChannelGeneratorApp();

        // Parse the command line arguments
        try {
            /*
             * MPCS-7750 - 10/23/15. Use createOptions() rather than
             * creating a new reserved/base options object.
             */

            final ICommandLine commandLine = app.createOptions()
                    .parseCommandLine(args, true);
            app.configure(commandLine);
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(e.getMessage());

            System.exit(AbstractGeneratorApp.ERROR);
        }

        // Initialize the application
        final boolean ok = app.init();
        if (!ok) {
            System.exit(AbstractGeneratorApp.ERROR);
        }
        try {
            app.run();
            /* MPCS-7119 - 4/20/15. Added exit status */
            System.exit(app.getExitStatus());

        } catch (final Exception e) {
            // something totally unexpected happened
            TraceManager.getDefaultTracer().error("Unexpected exception: " + ExceptionTools.getMessage(e));
            System.exit(AbstractGeneratorApp.ERROR);
        }
    }

}
