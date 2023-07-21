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
package ammos.datagen.evr.app;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;

import ammos.datagen.app.AbstractGeneratorApp;
import ammos.datagen.config.InvalidConfigurationException;
import ammos.datagen.evr.config.EvrLevel;
import ammos.datagen.evr.config.EvrMissionConfiguration;
import ammos.datagen.evr.config.EvrRunConfiguration;
import ammos.datagen.evr.generators.EvrBodyGenerator;
import ammos.datagen.evr.generators.seeds.EvrBodyGeneratorSeed;
import ammos.datagen.evr.generators.seeds.EvrSeedMaker;
import ammos.datagen.evr.util.EvrGeneratorStatistics;
import ammos.datagen.evr.util.EvrUsageTrackerMap;
import ammos.datagen.generators.PacketHeaderGenerator;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.FileSuffixes;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDictionary;
import jpl.gds.dictionary.api.evr.IEvrDictionaryFactory;
import jpl.gds.shared.annotation.CoverageIgnore;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.Pair;

/**
 * This is a test data generator application class that will create a file of
 * binary EVR packets according to a flight project dictionary. It requires an
 * EVR mission configuration file, an EVR run configuration file, an EVR
 * dictionary file, and an output directory name. It will produce a
 * CCSDS-compliant EVR packet file, a truth data file detailing each packet in
 * the packet file, a statistics file supplying statistics about the packet file
 * and the generation process, and a log file, which will contain errors and
 * progress reports.
 * 
 *
 */
public class EvrGeneratorApp extends AbstractGeneratorApp {

    private static final String EVR = "evr";

    /**
     * The output file name for the EVR data file.
     */
    public static final String DATA_FILE_NAME = EVR
            + FileSuffixes.RAW_PKT.getSuffix();
    /**
     * The output file name for the log file.
     */
    public static final String LOG_FILE_NAME = EVR + "_generator"
            + FileSuffixes.LOG.getSuffix();
    /**
     * The output file name for the truth file.
     */
    public static final String TRUTH_FILE_NAME = EVR
            + FileSuffixes.TRUTH.getSuffix();
    /**
     * The output file name for the statistics file.
     */
    public static final String STATS_FILE_NAME = EVR
            + FileSuffixes.STATISTICS.getSuffix();

    // Generators unique to this application
    private EvrBodyGenerator evrBodyGen;

    // These hold the loaded configuration data
    private EvrMissionConfiguration evrMissionConfig;
    private EvrRunConfiguration evrRunConfig;

    // / The dictionary
    private IEvrDictionary evrDict;

	private DictionaryProperties dictConfig;

    /**
     * Basic constructor. Must call configure() and init() to initialize this
     * instance.
     */
    public EvrGeneratorApp() {
    }

    /**
     * Initializes the EVR generator before use. This involves loading the
     * configuration files and the dictionary, and creating the necessary
     * generator objects.
     * 
     * @return true if initialization succeeded, false if not
     * 
     * 
     * MPCS-7229 - 4/20/15. Set exit status to error upon false
     *          return
     */
    public boolean init() {

        if (!super.init(LOG_FILE_NAME, TRUTH_FILE_NAME, DATA_FILE_NAME,
                STATS_FILE_NAME)) {
            setExitStatus(ERROR);
            return false;
        }

        // Set up the statistics and usage tracker objects for this run
        setStatistics(new EvrGeneratorStatistics());
        setTrackers(new EvrUsageTrackerMap());

        // Create the seed maker, and then use it to create necessary generators
        final EvrSeedMaker seedMaker = new EvrSeedMaker(this.evrMissionConfig,
                this.evrRunConfig, this.evrDict);
        try {
            seedMaker.validate();
        } catch (final InvalidConfigurationException e) {
            this.statusLogger.error(e.getMessage());
            setExitStatus(ERROR);
            return false;
        }

        boolean ok = createEvrBodyGenerator(seedMaker);
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
     * SCLK size. This method handles the setting of EVR-specific adaptation in
     * AMPCS.
     */
    @Override
    protected void configureAmpcsMission() {

        // Handle generic AMPCS configuration in the super class.
        super.configureAmpcsMission();

        dictConfig = appContext.getBean(DictionaryProperties.class);

        // Configure the AMPCS EVR adaptation. First read the necessary values
        // out of the datagen mission configuration.
        final String evrDictClass = this.evrMissionConfig.getStringProperty(
                EvrMissionConfiguration.EVR_DICTIONARY_CLASS, null);

        if (evrDictClass != null) {
            dictConfig.setDictionaryClass(DictionaryType.EVR, new DictionaryClassContainer(evrDictClass));
        }
    }

    /**
     * Creates the EVR body generator.
     * 
     * @param seedMaker
     *            the EVR seed maker object
     * @return true if success, false if not
     */
    private boolean createEvrBodyGenerator(final EvrSeedMaker seedMaker) {

        final EvrBodyGeneratorSeed evrSeed = seedMaker
                .createEvrBodyGeneratorSeed();
        final IEvrDictionaryFactory fact = appContext.getBean(IEvrDictionaryFactory.class);
        try {
            this.evrBodyGen = isGenerateTruth() ? new EvrBodyGenerator(dictConfig,
                    getTruthFile(), fact) : new EvrBodyGenerator(dictConfig, fact);
                    this.evrBodyGen.setSeedData(evrSeed);
        } catch (final InvalidSeedDataException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Uses AMPCS libraries to load the EVR dictionary.
     * 
     * @return an IEvrDictionary object, or null if the load failed
     */
    @Override
    protected boolean loadDictionary() {

        try {
            this.evrDict = appContext.getBean(IEvrDictionaryFactory.class)
                    .getNewInstance(dictConfig, getDictionaryFile());
            return true;
        } catch (final DictionaryException e) {
            this.statusLogger.error("Unable to load EVR dictionary");
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
        this.evrMissionConfig = new EvrMissionConfiguration();
        if (!this.evrMissionConfig.load(getMissionConfigFile())) {
            return false;
        }
        setMissionConfig(this.evrMissionConfig);

        // Load run configuration
        this.evrRunConfig = new EvrRunConfiguration();
        if (!this.evrRunConfig.load(getRunConfigFile())) {
            return false;
        }
        setRunConfig(this.evrRunConfig);

        return true;
    }

    /**
     * Executes the main logic to generate the EVR output file.
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
            final long desiredBytes = this.evrRunConfig.getLongProperty(
                    EvrRunConfiguration.DESIRED_FILE_SIZE, 0);

            // This is the mission maximum packet size
            final int maxPacket = this.evrMissionConfig.getIntProperty(
                    EvrMissionConfiguration.PACKET_MAX_LEN, 65535);

            // Record the run start time and start the last report time tracking
            initStartTime();

            // Generate EVR packets until the desired file size is reached
            while (getStatistics().getTotalPacketSize() < desiredBytes
                    && !getSclkGenerator().isExhausted() && !isShuttingDown()) {

                if (isGenerateTruth()) {
                    getTruthFile().writeRecord(recordCounter++);
                }

                /*
                 * See if it is time to generate a fill packet. If one is
                 * generated, report progress and skip everything else.
                 */
                if (generateFillPacket()) {
                    reportProgress();
                    continue;
                }

                // Generate bytes for the EVR body
                final Pair<IEvrDefinition, byte[]> evrPair = (Pair<IEvrDefinition, byte[]>) this.evrBodyGen
                        .get();

                // Unable to generate an EVR because a non-configured level was
                // found. Go on to the next one.
                if (evrPair == null) {
                    if (isGenerateTruth()) {
                        getTruthFile().writeLine("No Packet");
                    }
                    continue;
                }

                final IEvrDefinition def = evrPair.getOne();
                final byte[] evrBytes = evrPair.getTwo();

                // Generate the packet header
                final EvrLevel level = this.evrMissionConfig.getEvrLevel(def
                        .getLevel());
                final PacketHeaderGenerator packetGen = getPacketHeaderGenerator(level
                        .getLevelApid());
                final ISpacePacketHeader header = (ISpacePacketHeader) packetGen
                        .getNext();

                // Generate the SCLK
                final ISclk sclk = (ISclk) getSclkGenerator().getNext();

                // Get the byte representation of the current SCLK
                final byte[] sclkBytes = sclk.getBytes();

                // Now we know the packet body length - must be a less one value
                // in the header
                header.setPacketDataLength(sclk.getByteLength()
                        + evrBytes.length - 1);

                // Get the bytes for the primary header
                final byte[] headerBytes = header.getBytes();

                // Compute total packet size and check against mission maximum
                final int packetSize = headerBytes.length + sclkBytes.length
                        + evrBytes.length;

                if (packetSize > maxPacket) {
                    getStatistics().incrementInvalidPacketCount();
                    this.statusLogger
                    .warn("Skipped writing packet with size ("
                            + packetSize
                            + "), which is greater than the configured mission maximum ("
                            + maxPacket + ")");
                    if (isGenerateTruth()) {
                        getTruthFile().writeLine("No Packet");
                    }
                    continue;
                }

                // Write all the bytes to the output file: header, sclk, EVR
                // body
                writeToDataStream(headerBytes);
                writeToDataStream(sclkBytes);
                writeToDataStream(evrBytes);

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
        } catch (final Exception e) {
            e.printStackTrace();
            this.statusLogger.error("Unexpected exception", e);
            /* MPCS-7229 - 4/20/15. Set error status upon exception. */
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
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        /* 10/6/14 - MPCS-6698. Update usage line */
        pw.println("Usage: "
                + ApplicationConfiguration.getApplicationName()
                + " --missionConfig <file path> --runConfig <file path> --dictionary <file path> --outputDir <directory> [--sourceSchema <schema name>]");
        pw.println("                   ");

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
        pw.println("binary EVR packets according to a flight project dictionary. It requires an");
        pw.println("EVR mission configuration file, an EVR run configuration file, an EVR");
        pw.println("dictionary file, and an output directory name. It will produce a");
        pw.println("CCSDS-compliant EVR packet file, a truth data file detailing each packet in");
        pw.println("the packet file, a statistics file supplying statistics about the packet file");
        pw.println("and the generation process, and a log file, which will contain errors and");
        pw.println("progress reports. If the source schema (mission) is supplied, then");
        pw.println("that will be the dictionary XML format expected by this tool. The source");
        pw.println("schema defaults to 'mm' (for multimission). This tool will accept 'mm',");
        pw.println("msl', or 'smap'");
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

        final EvrGeneratorApp app = new EvrGeneratorApp();

        // Parse the command line arguments
        try {
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
            /* MPCS-7229 - 4/20/15. Set exit status */
            System.exit(app.getExitStatus());

        } catch (final Exception e) {
            // something totally unexpected happened
            e.printStackTrace();
            System.exit(AbstractGeneratorApp.ERROR);
        }
    }

}
