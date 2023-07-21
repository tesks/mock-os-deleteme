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
package ammos.datagen.cmdline;

import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.app.ICommandLineApp;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.IntegerOption;

/**
 * DatagenOptions is a class for defining command line options whose usages are
 * universally common in the AMPCS Data Generator applications.
 * 
 */
public class DatagenOptions extends BaseCommandOptions {

    // MPCS-9576 - 4/11/18 - Added command options constants

    /** input dir long option */
    public static final String          INPUT_DIRECTORY_LONG = "inputDir";
    
    /** output dir long option */
    public static final String          OUTPUT_DIRECTORY_LONG = "outputDir";

    /** input file long option */
    public static final String          INPUT_FILE_LONG       = "inputFile";

    /** output file long option */
    public static final String          OUTPUT_FILE_LONG      = "outputFile";

    /** mission config long option */
    public static final String          MISSION_CONFIG_LONG   = "missionConfig";

    /** run config long option */
    public static final String          RUN_CONFIG_LONG       = "runConfig";

    /** dictionary long option */
    public static final String          DICTIONARY_LONG       = "dictionary";

    /** generate truth long option */
    public static final String          GENERATE_TRUTH_LONG   = "generateTruth";

    /** VCID long option */
    public static final String          VCID_LONG             = "vcid";

    /** frame name long option */
    public static final String          FRAME_NAME_LONG       = "frameName";

    /** OUTPUT_DIRECTORY Option object */
    public static final DirectoryOption OUTPUT_DIRECTORY = new DirectoryOption(
            "O", OUTPUT_DIRECTORY_LONG, "directory",
            "directory for saving output files of program", false, true, true);

    /** OUTPUT_FILE Option object */
    public static final FileOption OUTPUT_FILE = new FileOption("F",
            OUTPUT_FILE_LONG, "file path",
            "location for saving output file of program", false, false);

    /** INPUT_DIRECTORY Option object */
    public static final DirectoryOption INPUT_DIRECTORY = new DirectoryOption(
            "I", INPUT_DIRECTORY_LONG, "directory",
            "directory for reading input files for a program", false, true);

    /** DICTIONARY Option object */
    public static final FileOption DICTIONARY = new FileOption("D",
            DICTIONARY_LONG, "file path", "path to dictionary file", false, true);

    /** MISSION_CONFIG Option object */
    public static final FileOption MISSION_CONFIG = new FileOption("M",
            MISSION_CONFIG_LONG, "file path", "path to mission configuration file",
            false, true);

    /** RUN_CONFIG Option object */
    public static final FileOption RUN_CONFIG = new FileOption("R",
            RUN_CONFIG_LONG, "file path", "path to run configuration file", false,
            true);

    /** GENERATE_TRUTH Option object */
    public static final FlagOption GENERATE_TRUTH = new FlagOption("G", GENERATE_TRUTH_LONG,
            "if option supplied, generate a detailed truth file", false);

    /** CHILL_QUERY_OPTIONS Option object */
    public static final StringOption CHILL_QUERY_OPTIONS = new StringOption(
            "Q", "queryOptions", "chill options",
            "AMPCS (chill) query command line options (must be quoted)", false);

    /* MPCS-6698 - 10/6/14. Re-added MISSION option. */
    /** MISSION Option object */
    public static final StringOption MISSION = new StringOption("m", "mission",
            "AMPCS-mission",
            "Target mission; mission to which generated data applies", false);
    /* MPCS-6641 - 9/18/14. Changed MISSION to SOURCE_SCHEMA. */
    /*
     * MPCS-7663 - 9/10/15. Changed description. This can no longer take a
     * mission name as argument. It MUST be source schema type.
     */
    /** SOURCE_SCHEMA option object */
    public static final StringOption SOURCE_SCHEMA = new StringOption(
            "s",
            "sourceSchema",
            "schema-name",
            "Schema used by the input dictionary ('mm', 'sse', 'smap', 'msl', 'header', or 'monitor')",
            false);
    
    /*  MPCS-8013 - 9/6/16: Added frame definition name option for the FrameGeneratorApp */
    /** FRAME_TYPE option object */
    public static final StringOption FRAME_TYPE = new StringOption("T", FRAME_NAME_LONG,
            "Frame Name",
            "The telemetry frame definition name", false);
    
    /*  MPCS-8013 - 9/6/16: Added VCID option for the FrameGeneratorApp */
    /** VCID option object */
    public static final StringOption VCID = new StringOption("V", VCID_LONG, "VCID", 
                                                             "AOS Transfer Frame VCID", false);

    /* MPCS-8503 - 11/3/17: Add SCID option for the FrameGeneratorApp */
    /** SCID Option object */
    public static final IntegerOption SCID = new IntegerOption(null, "scid", "SCID", 
                                                               "Transfer Frame Spacecraft ID", false);

    //  MPCS-9576 - 4/11/18 - Added input file option
    /** INPUT_FILE_OPTION Option object */
    public static final FileOption INPUT_FILE = new FileOption("I", INPUT_FILE_LONG, "file path", 
                                                                   "Input file name", true, true);

    /**
     * Constructor.
     * 
     * @param app
     *            the command line application instance these options are for
     */
    public DatagenOptions(final ICommandLineApp app) {

        super(app);
        addHelpOption();
        addVersionOption();

    }

}
