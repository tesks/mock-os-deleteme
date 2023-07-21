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
package ammos.datagen.mission.nsyt.generators.app;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;

import ammos.datagen.app.AbstractGeneratorApp;
import ammos.datagen.config.GeneralMissionConfiguration;
import ammos.datagen.config.InvalidConfigurationException;
import ammos.datagen.generators.PacketHeaderGenerator;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.FileSuffixes;
import ammos.datagen.mission.nsyt.config.ContinuousRunConfiguration;
import ammos.datagen.mission.nsyt.generators.ContinuousBodyGenerator;
import ammos.datagen.mission.nsyt.generators.ContinuousGeneratorStatistics;
import ammos.datagen.mission.nsyt.generators.ContinuousUsageTrackerMap;
import ammos.datagen.mission.nsyt.generators.seeds.ContinuousBodyGeneratorSeed;
import ammos.datagen.mission.nsyt.generators.seeds.ContinuousSeedMaker;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.shared.annotation.CoverageIgnore;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.ISclk;

/**
 * This application generates CCSDS packets in the NSYT SEIS/APSS instrument
 * "continuous science" format. These packets contain an instrument header and a
 * series of channel samples in either uncompressed or Steim-Lite compressed
 * format.
 * <p>
 * The format of the packets generated is described by JPL D-78505-SEIS-FGICD.
 * The code is based upon a September 26, 2014 draft.
 * 
 *
 * MPCS-6864 - 12/1/14. Added class.
 * MPCS-7750 - 10/23/15. Changed to use new BaseCommandOptions
 *          and new command line option strategy throughout.
 */
public class NsytContinuousGeneratorApp extends AbstractGeneratorApp {

    private static final String CONTINUOUS = "nsyt_continuous";

    /**
     * The output file name for the packet data file.
     */
    public static final String DATA_FILE_NAME = CONTINUOUS
            + FileSuffixes.RAW_PKT.getSuffix();
    /**
     * The output file name for the log file.
     */
    public static final String LOG_FILE_NAME = CONTINUOUS + "_generator"
            + FileSuffixes.LOG.getSuffix();
    /**
     * The output file name for the truth file.
     */
    public static final String TRUTH_FILE_NAME = CONTINUOUS
            + FileSuffixes.TRUTH.getSuffix();
    /**
     * The output file name for the statistics file.
     */
    public static final String STATS_FILE_NAME = CONTINUOUS
            + FileSuffixes.STATISTICS.getSuffix();

    /* Generators unique to this application */
    private ContinuousBodyGenerator contBodyGen;
    private PacketHeaderGenerator packetGen;

    /* These hold the loaded configuration data */
    private GeneralMissionConfiguration contMissionConfig;
    private ContinuousRunConfiguration contRunConfig;

    /**
     * Basic constructor. Must call configure() and init() to initialize this
     * instance.
     */
    public NsytContinuousGeneratorApp() {

        SystemUtilities.doNothing();
    }

    /**
     * Initializes the generator before use. This involves loading the
     * configuration files and creating the necessary generator objects.
     * 
     * @return true if initialization succeeded, false if not
     * 
     * MPCS-7229 - 4/20/15. Set exit status to error upon false return
     */
    public boolean init() {

        if (!super.init(LOG_FILE_NAME, TRUTH_FILE_NAME, DATA_FILE_NAME,
                STATS_FILE_NAME)) {
            setExitStatus(ERROR);
            return false;
        }

        /* Set up the statistics and usage tracker objects for this run. */
        setStatistics(new ContinuousGeneratorStatistics());
        setTrackers(new ContinuousUsageTrackerMap());

        /*
         * Create the application seed maker, and then use it to create
         * necessary generators
         */
        final ContinuousSeedMaker seedMaker = new ContinuousSeedMaker(
                this.contMissionConfig, this.contRunConfig);
        try {
            seedMaker.validate();
        } catch (final InvalidConfigurationException e) {
            this.statusLogger.fatal(e.getMessage());
            setExitStatus(ERROR);
            return false;
        }

        boolean ok = createContinuousBodyGenerator(seedMaker);

        /*
         * This application currently only generates packets for one APID so
         * store the one packet header generator in a local member
         */
        ok = ok && createPacketHeaderGenerators(seedMaker);
        if (ok) {
            this.packetGen = getPacketHeaderGenerator(this.contRunConfig
                    .getIntProperty(ContinuousRunConfiguration.APID, 0));
        }

        ok = ok && createSclkGenerator(seedMaker);

        if (!ok) {
            setExitStatus(ERROR);
        }

        return ok;
    }

    /**
     * Creates the continuous body generator, which is used to generate the
     * packet bodies.
     * 
     * @param seedMaker
     *            the continuous seed maker object
     * @return true if success, false if not
     */
    private boolean createContinuousBodyGenerator(
            final ContinuousSeedMaker seedMaker) {

        ContinuousBodyGeneratorSeed contSeed = null;

        try {
            contSeed = seedMaker.createContinuousBodyGeneratorSeed();
            this.contBodyGen = isGenerateTruth() ? new ContinuousBodyGenerator(
                    getTruthFile()) : new ContinuousBodyGenerator();
            this.contBodyGen.setSeedData(contSeed);

        } catch (final InvalidSeedDataException e) {
            e.printStackTrace();
            return false;

        } catch (final InvalidConfigurationException e) {
            this.statusLogger.error(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Loads both the mission and run configuration files.
     * 
     * @return true if success, false if not
     */
    @Override
    protected boolean loadConfiguration() {

        /* Load mission configuration */
        this.contMissionConfig = new GeneralMissionConfiguration();
        if (!this.contMissionConfig.load(getMissionConfigFile())) {
            return false;
        }
        setMissionConfig(this.contMissionConfig);

        /* Load run configuration */
        this.contRunConfig = new ContinuousRunConfiguration();
        if (!this.contRunConfig.load(getRunConfigFile())) {
            return false;
        }
        setRunConfig(this.contRunConfig);

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.app.AbstractGeneratorApp#loadDictionary()
     */
    @Override
    protected boolean loadDictionary() {
        /* This application needs no dictionary */
        return true;
    }

    /**
     * Executes the main logic to generate the continuous packet output file.
     * 
     * @throws IOException
     *             if there is an error opening or writing to the output file
     */
    public void run() throws IOException {

        /*
         * Up to this point, we could shutdown without special handling. This
         * basically enables the shutdown handler in the superclass.
         */
        clearShutdownFlags();

        try {
            long recordCounter = 1;

            /* This is the desired number of bytes in the output file */
            final long desiredBytes = this.contRunConfig.getLongProperty(
                    ContinuousRunConfiguration.DESIRED_FILE_SIZE, 0);

            /*
             * On the other hand, the config file might have stated number of
             * desired packets rather than number of desired bytes, but never
             * both.
             */
            int desiredPackets = 0;
            boolean useDesiredPackets = false;
            if (desiredBytes == 0) {
                desiredPackets = this.contRunConfig.getIntProperty(
                        ContinuousRunConfiguration.DESIRED_NUM_PACKETS, 0);
                useDesiredPackets = true;
            }

            /* This is the mission maximum packet size */
            final int maxPacket = this.contMissionConfig.getIntProperty(
                    GeneralMissionConfiguration.PACKET_MAX_LEN, 65535);

            /* Record the run start time and start the last report time tracking */
            initStartTime();

            /*
             * Generate packets until the desired file size is reached, desired
             * packet count is reached, or the SCLK seed file is exhausted
             */
            while (((useDesiredPackets && getStatistics().getTotalPacketCount() < desiredPackets) || (!useDesiredPackets && getStatistics()
                    .getTotalPacketSize() < desiredBytes))
                    && !getSclkGenerator().isExhausted() && !isShuttingDown()) {

                /* Write record counter to truth data */
                if (isGenerateTruth()) {
                    getTruthFile().writeRecord(recordCounter++);
                }

                /*
                 * Generate bytes for the packet body. This is the bulk of the
                 * work.
                 */
                final byte[] dataBytes = (byte[]) this.contBodyGen.get();

                /* Generate the packet header */
                final ISpacePacketHeader header = (ISpacePacketHeader) this.packetGen
                        .getNext();

                /* Generate the SCLK and get the bytes representing it */
                final ISclk sclk = (ISclk) getSclkGenerator().getNext();
                final byte[] sclkBytes = sclk.getBytes();

                /*
                 * Now we know the packet body length - must be set as a less
                 * one value in the header
                 */
                header.setPacketDataLength(sclk.getByteLength()
                        + dataBytes.length - 1);

                // Get the bytes for the primary header
                final byte[] headerBytes = header.getBytes();

                /* Compute total packet size and check against mission maximum */
                final int packetSize = headerBytes.length + sclkBytes.length
                        + dataBytes.length;

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

                /*
                 * Write all the bytes to the output file: header, sclk, packet
                 * body
                 */
                writeToDataStream(headerBytes);
                writeToDataStream(sclkBytes);
                writeToDataStream(dataBytes);

                /* Write packet info to the truth file */
                if (isGenerateTruth()) {
                    getTruthFile().writePacket(header, sclk);
                }

                /* Update the packet statistics */
                getStatistics().updatePacketStatistics(packetSize,
                        header.getApid(), sclk, System.currentTimeMillis());

                /* Compute and print progress */
                reportProgress();
            }
        } catch (final Exception e) {
            e.printStackTrace();
            this.statusLogger.error("Unexpected exception", e);
            /* MPCS-7229 - 4/20/15. Set exit status upon exception. */
            setExitStatus(ERROR);
        }
        
        /* MPCS-9375 - 1/3/18 - Fix bug with stats not coming out */
        writeStatistics();

        /* We are ready to exit now. Tell the shutdown handler. */
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
        pw.println("Usage: "
                + ApplicationConfiguration.getApplicationName()
                + " --missionConfig <file path> --runConfig <file path> --outputDir <directory>");
        pw.println("                   ");

        final OptionSet options = createOptions().getOptions();

        options.printOptions(pw);
        pw.println("This is a test data generator application that will create a file of");
        pw.println("packets in the NYST SEIS/APSS intruments' 'continuous science packet'");
        pw.println("format. The mission setting defaults to 'nsyt'. The generator requires a");
        pw.println("general mission configuration file, an NSYT continuous run configuration file,");
        pw.println("file, and an output directory name. It will produce a CCSDS-compliant packet");
        pw.println("file, a truth data file detailing each packet in the packet file, a statistics");
        pw.println("file supplying statistics about the packet file and the generation process");
        pw.println("and a log file, which will contain errors and progress reports. SCLK");
        pw.println("length and other mission parameters are defaulted to those for the NSYT");
        pw.println("mission. Values in the mission configuration then override those values.");
        pw.println("It should also be noted that the truth file, if generated, will contain");
        pw.println("instrument channel IDs, not flight channel IDs, and the results of an AMPCS");
        pw.println("channel query cannot be directly compared to the truth file.");

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

        final NsytContinuousGeneratorApp app = new NsytContinuousGeneratorApp();

        /*
         * This app is for NSYT only. We do not need the command line option to
         * identify the source mission/schema, or a dictionary.
         */
        app.setNeedSourceSchema(false);
        app.setNeedDictionary(false);
        GdsSystemProperties.setSystemMission("nsyt");

        /* Parse the command line arguments */
        try {

            final ICommandLine commandLine = app.createOptions()
                    .parseCommandLine(args, true);
            app.configure(commandLine);

        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(e.getMessage());

            System.exit(AbstractGeneratorApp.ERROR);
        }

        /* Initialize the application */
        final boolean ok = app.init();
        if (!ok) {
            System.exit(AbstractGeneratorApp.ERROR);
        }

        /* Generate the output file */
        try {
            app.run();
            /* MPCS-7229 - 4/20/15. Set exit status */
            System.exit(app.getExitStatus());

        } catch (final Exception e) {
            /* something totally unexpected happened */
            e.printStackTrace();
            System.exit(AbstractGeneratorApp.ERROR);
        }
    }

}
