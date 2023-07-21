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
package ammos.datagen.frame.app;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;

import ammos.datagen.app.AbstractGeneratorApp;
import ammos.datagen.cmdline.DatagenOptions;
import ammos.datagen.config.GeneralMissionConfiguration;
import ammos.datagen.config.InvalidConfigurationException;
import ammos.datagen.frame.config.FrameRunConfiguration;
import ammos.datagen.frame.generators.FrameBodyGenerator;
import ammos.datagen.frame.generators.seeds.FrameBodyGeneratorSeed;
import ammos.datagen.frame.generators.seeds.FrameSeedMaker;
import ammos.datagen.generators.PacketHeaderGenerator;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.seeds.PacketHeaderGeneratorSeed;
import ammos.datagen.generators.util.FieldGeneratorStatistics;
import ammos.datagen.generators.util.FileSuffixes;
import ammos.datagen.generators.util.UsageTrackerMap;
import jpl.gds.ccsds.api.packet.IPacketFormatDefinition;
import jpl.gds.ccsds.api.packet.PacketFormatFactory;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionary;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionaryFactory;
import jpl.gds.shared.annotation.CoverageIgnore;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.types.Triplet;


/**
 * This is a test data generator application class that will create a file of
 * AOS transfer frames according to a flight project dictionary. It requires
 * a generic mission configuration file, a Frame run configuration file, an
 * input file that is valid RAW_PKT file, and an output directory.
 * It will produce a CCSDS-compliant AOS transfer frame file.
 * 
 * TODO: add support for TM frames
 * 
 * <p>
 * This is an AMPCS application extension. As such, it relies upon classes in
 * the AMPCS shared library and the AMPCS multi-mission core. It shares the AMPCS
 * configuration methods, as well. This means it expects to load the GDS
 * configuration files GdsSystemConfig.xml, TimeSystemConfig.xml, and
 * LogSystemConfig.xml at startup.
 * 
 *
 * MPCS-9623 - 4/18/18. Use mission and run config, removed abstract base class
 *
 */
public class FrameGeneratorApp extends AbstractGeneratorApp {
    /** fill packet APID */
    public static final int FILL_PACKET_APID      = 2047;

    private static final String FRAME = "frame";

    /** The output file name for the frame data file. */
    public static final String DATA_FILE_NAME = FRAME + FileSuffixes.RAW_TF.getSuffix();
    /** The output file name for the log file. */
    public static final String LOG_FILE_NAME = FRAME + "_generator"+ FileSuffixes.LOG.getSuffix();
    /** The output file name for the truth file. */
    public static final String TRUTH_FILE_NAME = FRAME + FileSuffixes.TRUTH.getSuffix();
    /** The output file name for the statistics file. */
    public static final String STATS_FILE_NAME = FRAME + FileSuffixes.STATISTICS.getSuffix();

    // These hold the loaded configuration data
    private GeneralMissionConfiguration missionConfig;
    private FrameRunConfiguration       runConfig;

    // The dictionary
    private ITransferFrameDictionary     frameDict;
    private DictionaryProperties         dictConfig;
    private ITransferFrameDefinition    frameDef;

    // Generators unique to this application
    private FrameBodyGenerator           frameBodyGen;

    private String                       frameType;
    private File                         inputFile;

	/**
	 * Basic constructor. Must call configure() and init() to initialize this
	 * instance.
	 */
	public FrameGeneratorApp() { 
        super();
        // We do not need the command line option to identify the source mission/schema
        setNeedSourceSchema(false);
	}

    /**
     * Initializes the frame generator before use. This involves loading the
     * configuration files and the dictionary, and creating the necessary
     * generator objects.
     * 
     * @return true if initialization succeeded, false if not
     * 
     */
    public boolean init() {
        if (!super.init(LOG_FILE_NAME, TRUTH_FILE_NAME, DATA_FILE_NAME, STATS_FILE_NAME)) {
            setExitStatus(ERROR);
            return false;
        }

        dictConfig = appContext.getBean(DictionaryProperties.class);

        // Set up the statistics and usage tracker objects for this run.
        setStatistics(new FieldGeneratorStatistics());
        setTrackers(new UsageTrackerMap());

        // initialize with empty mission/run configs
        final FrameSeedMaker seedMaker = new FrameSeedMaker(missionConfig, runConfig, frameDict);

        try {
            seedMaker.validate();
        }
        catch (final InvalidConfigurationException e) {
            this.statusLogger.error(e.getMessage());
            setExitStatus(ERROR);
            return false;
        }

        createFillPacketGenerator();
        final boolean ok = createFrameBodyGenerator(seedMaker);
        if (!ok) {
            setExitStatus(ERROR);
        }
        return ok;
    }

    @Override
    protected boolean loadConfiguration() {
        // Load mission configuration
        missionConfig = new GeneralMissionConfiguration();
        if (!missionConfig.load(getMissionConfigFile())) {
            return false;
        }
        setMissionConfig(missionConfig);

        // Load run configuration
        runConfig = new FrameRunConfiguration();
        if (!runConfig.load(getRunConfigFile())) {
            return false;
        }
        setRunConfig(runConfig);

        this.frameType = runConfig.getStringProperty(FrameRunConfiguration.FRAME_TYPE, null);

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ammos.datagen.app.AbstractGeneratorApp#loadDictionary()
     */
    @Override
    protected boolean loadDictionary() {
        try {
            frameDict = appContext.getBean(ITransferFrameDictionaryFactory.class).getNewInstance(dictConfig,
                                                                                                 getDictionaryFile());
            // FrameType is set
            frameDef = frameDict.findFrameDefinition(frameType);
            if (frameDef == null) {
                throw new DictionaryException("No matching frame type found " + this.frameType);
            }
            statusLogger.info("Loaded TF frame type: " + frameType);
            return true;
        }
        catch (final DictionaryException e) {
            this.statusLogger.error("Unable to load TransferFrame dictionary " + e.getMessage());
            setExitStatus(ERROR);
            return false;
        }
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
    }

	/**
	 * Executes the main logic to generate the transfer frame output file.
	 * 
	 * @throws IOException
	 *             if there is an error opening or writing to the output file
	 */
    public void run() throws IOException {

		// Up to this point, we could shutdown without special handling.
		// This basically enables the shutdown handler in the superclass.
		clearShutdownFlags();

        try {
            long recordCounter = 1;

			initStartTime();

            while (!this.isShuttingDown()) {
				if (isGenerateTruth()) { 
                    getTruthFile().writeRecord(recordCounter++);
				}

                // Generate bytes for the Frame body
                @SuppressWarnings("unchecked")
                final Triplet<byte[], byte[], byte[]> frameBytes = (Triplet<byte[], byte[], byte[]>) this.frameBodyGen.getNext();
                if (frameBytes == null) {
                    break;
                }
				
                // Write all the bytes to the output file: asm, frameArray, encoding
                writeToDataStream(frameBytes.getOne());
                writeToDataStream(frameBytes.getTwo());
                writeToDataStream(frameBytes.getThree());

                // Compute and print progress
                desiredPackets = inputFile.length() / frameDef.getCADUSizeBytes();
                reportProgress();
			}

            frameBodyGen.closeInputFile();

        }
        catch (final Exception e) {
            e.printStackTrace();
            this.statusLogger.error("Unexpected exception", e);
            setExitStatus(ERROR);
        }

        writeStatistics();

		// We are ready to exit now. Tell the shutdown handler.
		setShutdownFlags();
	}

	/**
	 * Main application entry point.
	 * 
	 * @param args
	 *            Command line arguments from the user
	 */
	@CoverageIgnore
	public static void main(final String[] args) {

		final FrameGeneratorApp app = new FrameGeneratorApp();
		// Parse the command line arguments
		try {
            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
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
		// Run the application
		try {
			app.run();
			System.exit(app.getExitStatus());
		} catch (final Exception e) {
			// something totally unexpected happened
            TraceManager.getTracer(Loggers.DATAGEN).error(e.getMessage());
            System.exit(AbstractGeneratorApp.ERROR);
		}
	}

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
	@Override

	public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        final String input = DatagenOptions.INPUT_FILE.parse(commandLine, true);
        final String regex = ".*" + FileSuffixes.RAW_PKT.getSuffix();
        // check extension
        if (!input.toUpperCase().matches(regex)) {
            throw new ParseException("Input file name does not match " + regex);
        }

        this.inputFile = new File(input);
        this.dictFile = DatagenOptions.DICTIONARY.parse(commandLine, true);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName()
                + " ----inputFile <file path> --outputDir <directory> --dictionary <file path> --missionConfig <file path> --runConfig <file path>");
        pw.println("                   ");

        final OptionSet options = createOptions().getOptions();

        options.printOptions(pw);

        pw.println("This is a test data generator application that will create a file of");
        pw.println("AOS Transfer Frames according to a flight project dictionary. It requires an");
        pw.println("an input file name, an output directory name, a dictionary file ");
        pw.println("(transfer_frame.xml), and a Frame run configuration file. It will produce a");
        pw.println("CCSDS-compliant AOS Transfer Frame file, a truth data file detailing each frame,");
        pw.println("a statistics file supplying statistics about the transfer frame file");
        pw.println("and the generation process, and a log file, which will contain errors and");
        pw.println("progress reports.");

        pw.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DatagenOptions createOptions() {
        final DatagenOptions options = super.createOptions();
        options.addOption(DatagenOptions.INPUT_FILE);
        return options;
    }

    /**
     * Creates the Frame body generator.
     * 
     * @param seedMaker
     *            the frame seed maker object
     * 
     * @return true if success, false if not
     */
    private boolean createFrameBodyGenerator(final FrameSeedMaker seedMaker) {
        final FrameBodyGeneratorSeed frameBodySeed = seedMaker.createFrameBodySeed(this.fillPacketGenerator);
        try {
            this.frameBodyGen = isGenerateTruth() ? new FrameBodyGenerator(inputFile, getStatistics(), getTruthFile())
                    : new FrameBodyGenerator(inputFile, getStatistics());
            this.frameBodyGen.setSeedData(frameBodySeed);
        }
        catch (final InvalidSeedDataException e) {
            statusLogger.error(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Creates the Fill packet generator (APID 2047)
     * Superclass only creates it if fillPercent > 0
     * 
     */
    private void createFillPacketGenerator() {
        this.fillPercent = 0f;
        this.fillPacketGenerator = new PacketHeaderGenerator(PacketFormatFactory.create(IPacketFormatDefinition.TypeName.CCSDS));
        final PacketHeaderGeneratorSeed packetSeed = new PacketHeaderGeneratorSeed();
        packetSeed.setApid(FILL_PACKET_APID);
        this.fillPacketGenerator.setSeedData(packetSeed);
    }


    // getters used by tests - must be public as app / test is in different package

    /**
     * Get input file
     * 
     * @return input file as File object
     */
    public File getInputFile() {
        return inputFile;
    }
}
