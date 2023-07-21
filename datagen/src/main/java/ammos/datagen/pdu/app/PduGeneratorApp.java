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
package ammos.datagen.pdu.app;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;

import ammos.datagen.app.AbstractGeneratorApp;
import ammos.datagen.cmdline.DatagenOptions;
import ammos.datagen.config.GeneralMissionConfiguration;
import ammos.datagen.config.IMissionConfiguration;
import ammos.datagen.config.InvalidConfigurationException;
import ammos.datagen.generators.PacketHeaderGenerator;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.FieldGeneratorStatistics;
import ammos.datagen.generators.util.FileSuffixes;
import ammos.datagen.generators.util.UsageTrackerMap;
import ammos.datagen.pdu.app.client.CfdpClient;
import ammos.datagen.pdu.cfdp.DatagenDirectoriesConfigurationLookup;
import ammos.datagen.pdu.cfdp.DatagenFinishedTransactionsHistory;
import ammos.datagen.pdu.cfdp.DatagenMetadataConfigurationLookup;
import ammos.datagen.pdu.cfdp.DatagenSequenceNumberGenerator;
import ammos.datagen.pdu.cfdp.DatagenStatManager;
import ammos.datagen.pdu.config.PduRunConfiguration;
import ammos.datagen.pdu.generators.PduBodyGenerator;
import ammos.datagen.pdu.generators.seeds.PduBodyGeneratorSeed;
import ammos.datagen.pdu.generators.seeds.PduSeedMaker;
import cfdp.engine.ampcs.FileUtil;
import cfdp.engine.ampcs.FinishedTransactionsHistoryUtil;
import cfdp.engine.ampcs.MetadataFileUtil;
import cfdp.engine.ampcs.StatUtil;
import cfdp.engine.ampcs.TransactionSequenceNumbersGenerationUtil;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.shared.annotation.CoverageIgnore;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOption;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.UnsignedLong;

/**
 * This is a test data generator application class that will create a file of
 * binary PDU packets. It requires a generic mission configuration file, a PDU
 * run configuration file, a transaction ID, and input file and an output directory.
 * It will produce a CCSDS-compliant PDU packet file, a truth data file detailing
 * each packet in the packet file, a statistics file supplying statistics about
 * the packet file and the generation process, and a log file, which will contain
 * errors and progress reports.
 *
 * MPCS-10525 - 03/12/19 - Added support for transaction number and
 *          dest file name (set in PDU header)
 * 
 *
 */
public class PduGeneratorApp extends AbstractGeneratorApp {
    private static final String PDU = "pdu";

    /** The output file name for the PDU data file */
    public static final String  DATA_FILE_NAME = PDU + FileSuffixes.RAW_PKT.getSuffix();

    /** The output file name for the log file */
    public static final String  LOG_FILE_NAME   = PDU + "_generator" + FileSuffixes.LOG.getSuffix();

    /** The output file name for the truth file */
    public static final String  TRUTH_FILE_NAME = PDU + FileSuffixes.TRUTH.getSuffix();

    /** The output file name for the statistics file */
    public static final String  STATS_FILE_NAME = PDU + FileSuffixes.STATISTICS.getSuffix();
    
    /** long option for transaction number */
    public static final String TRANS_NUM_LONG = "transactionNumber";

    /** long option for transaction number */
    public static final String META_DEST_LONG = "destFile";

    private static final UnsignedLongOption TRANS_NUMBER = new UnsignedLongOption("T", TRANS_NUM_LONG, "unsigned long",
                                                                                 "Transaction Number",
                                                                                 true);

    private static final FileOption META_DEST_OPTION = new FileOption("D", META_DEST_LONG, "file name",
                                                                        "Metadata destination file name (optional)", false,
                                                                        false);

    // Generators unique to this application
    private PduBodyGenerator               pduBodyGen;

    // These hold the loaded configuration data
    private GeneralMissionConfiguration pduMissionConfig;
    private PduRunConfiguration pduRunConfig;

    //set via command line options
    private String inputFile;
    private UnsignedLong transactionNumber;
    private String metaDestFile;

    private CfdpClient                     client;

    /**
     * Basic constructor.
     */
    public PduGeneratorApp() {
        super();
        // We do not need the command line option to identify the source mission/schema, or a dictionary.
        setNeedSourceSchema(false);
        setNeedDictionary(false);
    }

    /**
     * Initializes the PDU generator before use. This involves loading the
     * configuration files and creating the necessary generator objects.
     * 
     * @return true if initialization succeeded, false if not
     * 
     */
    public boolean init() {
        if (!super.init(LOG_FILE_NAME, TRUTH_FILE_NAME, DATA_FILE_NAME, STATS_FILE_NAME)) {
            setExitStatus(ERROR);
            return false;
        }

        /*
         * Set up the statistics and usage tracker objects for this run.
         */
        setStatistics(new FieldGeneratorStatistics());
        setTrackers(new UsageTrackerMap());

        // Create the seed maker, and then use it to create necessary generators
        final PduSeedMaker seedMaker = new PduSeedMaker(pduMissionConfig, pduRunConfig);
        try {
            seedMaker.validate();
        }
        catch (final InvalidConfigurationException e) {
            statusLogger.error(e.getMessage());
            setExitStatus(ERROR);
            return false;
        }

        client = new CfdpClient(pduMissionConfig, pduRunConfig, transactionNumber);

        //set CFDP dependencies
        TransactionSequenceNumbersGenerationUtil.INSTANCE.setGenerator(new DatagenSequenceNumberGenerator());
        FileUtil.INSTANCE.setTopLevelDirectoriesConfigurationLookup(new DatagenDirectoriesConfigurationLookup());
        StatUtil.INSTANCE.setStatManager(new DatagenStatManager());
        MetadataFileUtil.INSTANCE.setMetadataConfigurationLookup(new DatagenMetadataConfigurationLookup());
        FinishedTransactionsHistoryUtil.INSTANCE.setRemovedTransactionsHistory(new DatagenFinishedTransactionsHistory());

        //if not provided, match input file
        String metaDest = metaDestFile != null && metaDestFile.length() > 0  ? metaDestFile : inputFile;
        client.executePutRequest(inputFile, metaDest);

        // CFDP client must be initialized at this point
        boolean ok = createPduBodyGenerator(seedMaker);
        ok = ok && createPacketHeaderGenerators(seedMaker);
        ok = ok && createSclkGenerator(seedMaker);

        if (!ok) {
            setExitStatus(ERROR);
        }
        return ok;

    }

    @Override
    protected boolean loadConfiguration() {
        // Load mission configuration
        pduMissionConfig = new GeneralMissionConfiguration();
        if (!pduMissionConfig.load(getMissionConfigFile())) {
            return false;
        }
        setMissionConfig(pduMissionConfig);

        // Load run configuration
        pduRunConfig = new PduRunConfiguration();
        if (!pduRunConfig.load(getRunConfigFile())) {
            return false;
        }
        setRunConfig(pduRunConfig);

        return true;
    }

    @Override
    protected boolean loadDictionary() {
        // no dictionary needed for this app
        return true;
    }

    /**
     * Executes the main logic to generate the PDU output file.
     * 
     * @throws IOException if there is an error opening or writing to the output file
     */
    public void run() throws IOException {
        // This is the mission maximum packet size
        final int maxPacket = pduMissionConfig.getIntProperty(IMissionConfiguration.PACKET_MAX_LEN, 65535);

        // Up to this point, we could shutdown without special handling.
        // This basically enables the shutdown handler in the superclass.
        clearShutdownFlags();

        try {
            long recordCounter = 1;

            // Record the run start time and start the last report time tracking
            initStartTime();
            
            //estimate number of desired packets for progress reporting
            //input file exists, checked in configure()
            desiredPackets = new File(inputFile).length()
                    / pduRunConfig.getIntProperty(PduRunConfiguration.PREF_PDU_LENGTH, 1000);

            // Generate PDU packets, wrapped in space packets
            while (client.hasMoreData() && !getSclkGenerator().isExhausted() && !isShuttingDown()) {
                if (isGenerateTruth()) {
                    getTruthFile().writeRecord(recordCounter++);
                }

                // Generate bytes for the PDU body
                final byte[] pduBytes = (byte[]) this.pduBodyGen.get();
                // check for PDUs skipped via error injection
                if (pduBytes == null) {
                    continue;
                }

                // Generate the packet header
                final int apid = pduRunConfig.getIntProperty(PduRunConfiguration.PACKET_APID, 1);
                final PacketHeaderGenerator packetGen = getPacketHeaderGenerator(apid);
                final ISpacePacketHeader header = (ISpacePacketHeader) packetGen.getNext();

                // Generate the SCLK
                final ISclk sclk = (ISclk) getSclkGenerator().getNext();

                // Get the byte representation of the current SCLK
                final byte[] sclkBytes = sclk.getBytes();

                // Now we know the packet body length - must be a less one value in the header
                header.setPacketDataLength(sclk.getByteLength() + pduBytes.length - 1);

                // Get the bytes for the primary header
                final byte[] headerBytes = header.getBytes();

                // Compute total packet size and check against mission maximum
                final int packetSize = headerBytes.length + sclkBytes.length + pduBytes.length;

                if (packetSize > maxPacket) {
                    getStatistics().incrementInvalidPacketCount();
                    statusLogger.warn("Skipped writing packet with size (" + packetSize
                            + "), which is greater than the configured mission maximum (" + maxPacket + ")");
                    if (isGenerateTruth()) {
                        getTruthFile().writeLine("No Packet");
                    }
                    continue;
                }

                // Write all the bytes to the output file: header, sclk, PDU body
                writeToDataStream(headerBytes);
                writeToDataStream(sclkBytes);
                writeToDataStream(pduBytes);

                // Write packet info to the truth file
                if (isGenerateTruth()) {
                    getTruthFile().writePacket(header, sclk);
                }

                // Update the statistics
                getStatistics().updatePacketStatistics(packetSize, header.getApid(), sclk, System.currentTimeMillis());

                // Compute and print progress
                reportProgress();
            }
        }
        catch (final Exception e) {
            statusLogger.error("Unexpected exception: ", e.getMessage());
            setExitStatus(ERROR);
        }

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
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName()
                + " --transactionNumber <unsigned long> --missionConfig <file path> --runConfig <file path> "
                           + "--inputFile <file path> --outputDir <directory>");
        pw.println("                   ");

        final OptionSet options = createOptions().getOptions();
        options.printOptions(pw);
        pw.println("This is a test data generator application that will create a file of");
        pw.println("binary PDU packets. It requires a generic mission configuration file, a PDU");
        pw.println("a PDU run configuration file, a transaction ID, and input and an output file name.");
        pw.println("It will produce a CCSDS-compliant PDU packet file, a truth data file detailing");
        pw.println(" each packet in the packet file, a statistics file supplying statistics about");
        pw.println("the packet file and the generation process, and a log file, which will contain");
        pw.println(" errors and progress reports.");
        pw.flush();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
     */
    @Override
    public DatagenOptions createOptions() {
        final DatagenOptions options = super.createOptions();
        options.addOption(TRANS_NUMBER);
        options.addOption(META_DEST_OPTION);
        options.addOption(DatagenOptions.INPUT_FILE);
        return options;
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);
        inputFile = DatagenOptions.INPUT_FILE.parse(commandLine, true);
        // check for empty file, in which case the CFDP Engine will not detect an EOF PDU
        if (new File(inputFile).length() == 0) {
            throw new ParseException("Input file is empty.");
        }
        transactionNumber = TRANS_NUMBER.parse(commandLine, true);
        //optional parameters
        metaDestFile = META_DEST_OPTION.parse(commandLine);
    }

    /**
     * Main application entry point.
     * 
     * @param args Command line arguments from the user
     */
    @CoverageIgnore
    public static void main(final String[] args) {

        final PduGeneratorApp app = new PduGeneratorApp();

        // Parse the command line arguments
        try {
            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
            app.configure(commandLine);
        }
        catch (final ParseException e) {
            TraceManager.getTracer(Loggers.DATAGEN).error(e.getMessage());
            System.exit(AbstractGeneratorApp.ERROR);
        }

        // Initialize the application
        final boolean ok = app.init();
        if (!ok) {
            System.exit(AbstractGeneratorApp.ERROR);
        }
        try {
            app.run();
            System.exit(app.getExitStatus());

        }
        catch (final Exception e) {
            // something totally unexpected happened
            TraceManager.getTracer(Loggers.DATAGEN).error(e.getMessage());
            System.exit(AbstractGeneratorApp.ERROR);
        }
    }

    /**
     * Creates the PDU body generator.
     * 
     * @param seedMaker
     *            the PDU seed maker object
     * @return true if success, false if not
     */
    private boolean createPduBodyGenerator(final PduSeedMaker seedMaker) {

        final PduBodyGeneratorSeed pduSeed = seedMaker.createPduBodyGeneratorSeed();
        try {
            pduBodyGen = isGenerateTruth() ? new PduBodyGenerator(client, getTruthFile())
                    : new PduBodyGenerator(client);
            pduBodyGen.setSeedData(pduSeed);
        }
        catch (final InvalidSeedDataException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // getters for tests

    String getInputFile() {
        return inputFile;
    }

    UnsignedLong getTransactionNumber(){
        return transactionNumber;
    }

    String getMetaDestFile() {
        return metaDestFile;
    }
}
