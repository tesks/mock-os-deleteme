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
package jpl.gds.product.automation.app.options;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.app.ICommandLineApp;
import jpl.gds.shared.cli.cmdline.ICommandLineParser;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.numeric.LongOption;

/**
 * PostDownlinkProductProcessorAppOptions is a class for containing and parsing
 * command line options specifically just for use in Post Downlink Product
 * Processing applications. Each of the options can be retrieved from this class
 * and either added to an instance of this class or added to another compatible
 * options container. When the options are parsed no special processing or
 * storage of the values is performed.
 * 
 * These options were consolidated and converted from options created and used in PDPP applications.
 * 
 * MPCS-8379 - 10/05/16 - added class.
 *
 */
public class PostDownlinkProductProcessorAppOptions extends BaseCommandOptions {
	// MPC-8180  - 07/26/16 - removed product directory options
	/** The options for displaying to console */
	private static final String						OPTION_SHORT_DISPLAY_TO_CONSOLE	= "t";
	private static final String						OPTION_LONG_DISPLAY_TO_CONSOLE	= "displayToConsole";
	private static final String						DESCRIPTION_DISPLAY_TO_CONSOLE	= "Write text output to console";

	// MPCS-8180 - 07/26/16 - removed input file list short option, "f" is already used
	/** The long option for including input files */
	private static final String						OPTION_LONG_INPUT_FILE_LIST		= "inputFileList";
	private static final String						DESCRIPTION_INPUT_FILE_LIST		= "Specify a file from which to read input files to process, one fully qualified file name per line.";

	/*
	 *  - using the same options for chill_get_products for fsw build id.
	 *  - 4/16/2012 - MPCS-3611 - adding options for fsw build id and dictionary string id.
	 */
	// MPCS-8180 - 07/26/16 - removed fsw build id short option, "C" is already used
	/** The long option for specifying the fsw version*/
	private static final String						OPTION_LONG_FSW_BUILD_ID		= "fswVersionId";
	private static final String						ARGUMENT_FSW_BUILD_ID			= "fswBuildId";
	private static final String						DESCRIPTION_FSW_BUILD_ID		= "Specify the FSW build id of the products that should be channelized";

	/**
	 *  - 5/30/2012 - MPCS-3768 - Adding CLI option to basically not do the build ID check between the user supplied
	 * build id and the id from the product meta data.  
	 */
	private static final String						OPTION_SHORT_OVERRIDE			= "V";
	private static final String						OPTION_LONG_OVERRIDE			= "fswVersionCheckOverride";
	private static final String						DESCRIPTION_OVERRIDE			= "If this flag is set, the product will be extracted using the FSW version given with the --fswVersionId or ---fswVersion option even if this version does not match the version in the product metadata.  Use with caution.";


	/**  - 11/29/2012 - MPCS-4387 - adding options for forceReprocessing of products already completed. */
	private static final String						OPTION_LONG_FORCE_REPROCESS		= "forceReprocess";
	private static final String						DESCRIPTION_FORCE_REPROCESS		= "If this flag is present products that have already been processed will be processed again.";
	
	
	//converted from reserved options
	private static final String						OPTION_LONG_PROCESS_ID = "hibernateProcessId";
	private static final String						ARGUMENT_PROCESS_ID = "processId";
	private static final String						DESCRIPTION_PROCESS_ID = "The process id in the hibernate database table that is reseverd for this process";
	
	/** Write text output to console option */
	public static final FlagOption DISPLAY_TO_CONSOLE_OPTION = new FlagOption(OPTION_SHORT_DISPLAY_TO_CONSOLE, OPTION_LONG_DISPLAY_TO_CONSOLE, DESCRIPTION_DISPLAY_TO_CONSOLE, false);
	/** Specify a file from which to read input files to process */
	public static final FileOption INPUT_FILE_LIST_OPTION = new FileOption(null, OPTION_LONG_INPUT_FILE_LIST, OPTION_LONG_INPUT_FILE_LIST, DESCRIPTION_INPUT_FILE_LIST, false, false);
	/** Specify a specific FSW build ID to process with */
	public static final StringOption FSW_BUILD_ID_OPTION = new StringOption(null, OPTION_LONG_FSW_BUILD_ID, ARGUMENT_FSW_BUILD_ID, DESCRIPTION_FSW_BUILD_ID, false);
	/** Force PDPP to use the supplied fswVersion or fswBuildId to extract products, regardless of the fswVersion in the product metadata */
	public static final FlagOption OVERRIDE_OPTION = new FlagOption(OPTION_SHORT_OVERRIDE, OPTION_LONG_OVERRIDE, DESCRIPTION_OVERRIDE, false);
	/** Force products that have already been processed to be processed again/ */
	public static final FlagOption FORCE_REPROCESS_OPTION = new FlagOption(null, OPTION_LONG_FORCE_REPROCESS, DESCRIPTION_FORCE_REPROCESS, false);
	
	/*
	 *  - 4/16/2012 - MPCS-3611 - adding options for fsw build id and fsw version string.
	 */
	/** The process id in the hibernate database table that is reseverd for this process */
	public static final LongOption PROCESS_ID_OPTION = new LongOption(null, OPTION_LONG_PROCESS_ID, ARGUMENT_PROCESS_ID, DESCRIPTION_PROCESS_ID, false);

	/**
	 * Constructor for an instance
	 * @param app the app creating this instance
	 * @param appContext the current application context
	 * 
	 * MPCS-9361 - 1/3/18 - added app context parameter
	 */
	public PostDownlinkProductProcessorAppOptions(final ICommandLineApp app, final ApplicationContext appContext) {
		super(app, true, appContext.getBean(ICommandLineParser.class));
		addHelpOption();
		addVersionOption();
	}
}
