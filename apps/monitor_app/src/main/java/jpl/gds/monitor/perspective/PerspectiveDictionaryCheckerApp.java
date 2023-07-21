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
package jpl.gds.monitor.perspective;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.perspective.ApplicationType;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.options.PerspectiveCommandOptions;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Cross references the channels in a given config file with a dictionary and
 * prints the channels that are not specified in the dictionary
 * 
 */
public class PerspectiveDictionaryCheckerApp extends AbstractCommandLineApp {

    /**
     * Command line application name
     */
    public static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_dict_checker");

    /**
     * Short command line option for passing the perspective dictionary checker
     * application a file
     */
    public static final String FILENAME_SHORT = "f";

    /**
     * Long command line option for passing the perspective dictionary checker
     * application a file
     */
    public static final String FILENAME_LONG = "filename";

    /**
     * Short command line option for passing the perspective dictionary checker
     * application a file
     */
    public static final String DIRECTORY_SHORT = "d";

    /**
     * Long command line option for passing the perspective dictionary checker
     * application a directory name
     */
    public static final String DIRECTORY_LONG = "directory";

    /**
     * Short command line option for passing the perspective dictionary checker
     * application a perspective name
     */
    public static final String PERSPECTIVE_SHORT = "p";

    /**
     * Long command line option for passing the perspective dictionary checker
     * application a perspective
     */
    public static final String PERSPECTIVE_LONG = "perspective";

    /**
     * Short command line option for passing the perspective dictionary checker
     * application a flag to search for missing channels
     */
    public static final String MISSING_SHORT = "m";

    /**
     * Long command line option for passing the perspective dictionary checker
     * application a flag to search for missing channels
     */
    public static final String MISSING_LONG = "missing";

    /**
     * Short command line option for passing the perspective dictionary checker
     * application a flag to search for undefined channels
     */
    public static final String UNDEFINED_SHORT = "u";

    /**
     * Long command line option for passing the perspective dictionary checker
     * application a flag to search for undefined channels
     */
    public static final String UNDEFINED_LONG = "undefined";

    private String filename;
    private String directory;
    private String perspective;
    private boolean missing;
    private boolean undefined;
    private final DictionaryProperties dictConfig;
    private DictionaryCommandOptions dictOptions;
    private PerspectiveCommandOptions perspectiveOptions;
    private final ApplicationContext appContext;

    public PerspectiveDictionaryCheckerApp() {
    	this.appContext = SpringContextFactory.getSpringContext(true);
    	this.dictConfig = this.appContext.getBean(DictionaryProperties.class);

        final PerspectiveProperties pp = new PerspectiveProperties(PerspectiveConstants.PERSPECTIVE_PROPERTY_FILE,
                                                                   appContext.getBean(SseContextFlag.class));
    	appContext.getBean(PerspectiveProperties.class).addProperties(pp);
    }
    
    /**
     * Create all application options that can be supplied on command line.
     * 
     * @return Set of options as an Options object
     */
    @Override
	public BaseCommandOptions createOptions() {
        
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));
               
        perspectiveOptions = new PerspectiveCommandOptions(this.appContext.getBean(PerspectiveConfiguration.class), ApplicationType.UNKNOWN);
        dictOptions = new DictionaryCommandOptions(this.dictConfig);
        
        this.options.addOption(new FileOption(FILENAME_SHORT,
                FILENAME_LONG, "filenamePath",
                "Path to existing fixed layout file", false, true));
        this.options.addOption(new DirectoryOption(DIRECTORY_SHORT,
                DIRECTORY_LONG, "directoryPath", "Path to directory containing fixed layout files to check", false, true));
        
        this.options.addOption(perspectiveOptions.PERSPECTIVE);
        this.options.addOption(new FlagOption(MISSING_SHORT,
                MISSING_LONG, "Flag that returns all the missing channels",
                false));
        this.options.addOption(new FlagOption(UNDEFINED_SHORT,
                UNDEFINED_LONG, "Flag that returns all the undefined channels",
                false));
        this.options.addOptions(dictOptions.getAllOptions());

        return this.options;
    }

    /**
     * Gets the filename set on the command line
     * 
     * @return the file path entered on the command line
     */
    public String getFilename() {
        return this.filename;
    }

    /**
     * Gets the directory set on the command line
     * 
     * @return the directory path entered on the command line
     */
    public String getDirectory() {
        return this.directory;
    }

    /**
     * Gets the perspective set on the command line
     * 
     * @return the perspective path entered on the command line
     */
    public String getPerspective() {
        return this.perspective;
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.cli.legacy.app.CommandLineApp#configure(org.apache.commons.cli.CommandLine)
	 */
	@Override
	public void configure(final ICommandLine commandLine) throws ParseException
	{
	    super.configure(commandLine);

        if (commandLine.hasOption(FILENAME_LONG)) {
            this.filename = (String) this.options.getOption(FILENAME_LONG).parse(commandLine);
        } else if (commandLine.hasOption(DIRECTORY_LONG)) {
            this.directory = (String) this.options.getOption(DIRECTORY_LONG).parse(commandLine);
        } else if (commandLine.hasOption(PERSPECTIVE_SHORT)) {
            this.perspective = perspectiveOptions.PERSPECTIVE.parse(commandLine);
        } else {
            throw new MissingOptionException(
                    "A filename, directory or perspective must be supplied");
        }

        this.missing = (Boolean) this.options.getOption(MISSING_LONG).parse(commandLine);
        this.undefined = (Boolean) this.options.getOption(UNDEFINED_LONG).parse(commandLine);
      
        if (!this.missing && !this.undefined) {
            throw new MissingOptionException(
                    "A missing and/or undefined flag must be supplied");
        }

        dictOptions.parseAllOptionsAsOptionalWithDefaults(commandLine);
       
    }

    /**
     * Show application help.
     */
    @Override
	public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: "
                + ApplicationConfiguration.getApplicationName()
                + " --filename <fixed-layout-file> | --perspective <chill perspective> |\n"
                + "--directory <absolute path> --missing | --undefined\n"
                + "[--fswDictionaryDir <directory> --fswVersion <version>]\n"
                + "[--sseDictionaryDir <directory> --sseVersion <version>]");
        pw.println("                   ");

        final OptionSet options = createOptions().getOptions();


        options.printOptions(pw);
        pw.flush();
        
    }

    /**
     * Launch the perspective dictionary checker application
     */
    public void run() {

        final PerspectiveDictionaryChecker dictChecker = new PerspectiveDictionaryChecker(appContext);

        // show undefined channels in fixed layout
        if (this.undefined) {
            List<String> undefinedChannels = new ArrayList<String>();
            if (this.filename != null) {
                undefinedChannels = dictChecker
                        .findUndefinedChannelsInFile(this.filename);
            } else if (this.perspective != null) {
                undefinedChannels = dictChecker
                        .findUndefinedChannelsInPerspective(this.perspective);
            } else if (this.directory != null) {
                undefinedChannels = dictChecker
                        .findUndefinedChannels(this.directory);
            }

            if (dictChecker.hasNullChannelIds()) {
                System.out
                        .println("One or more fixed field elements contain null channel IDs. Please check that the channel ID specifications in the XML abide by the Fixed View schema.");
            }

            if (undefinedChannels.size() == 0) {
                System.out.println("No undefined channels were found.");
            } else {
                // print result
                System.out.println("The following channels are undefined: ");
                for (final String channel : undefinedChannels) {
                    System.out.println(channel);
                }
            }
            System.out.println("");
        }

        // TODO show missing channels in fixed layout
        if (this.missing) {
            List<String> missingChannels = new ArrayList<String>();
            if (this.filename != null) {
                missingChannels = dictChecker
                        .findMissingChannelsInFile(this.filename);
            } else if (this.perspective != null) {
                missingChannels = dictChecker
                        .findMissingChannelsInPerspective(this.perspective);
            } else if (this.directory != null) {
                missingChannels = dictChecker
                        .findMissingChannelsInDir(this.directory);
            }

            if (dictChecker.hasNullChannelIds()) {
                System.out
                        .println("One or more fixed field elements contain null channel IDs. Please check that the channel ID specifications in the XML abide by the Fixed View schema.");
            }

            if (missingChannels.size() == 0) {
                System.out.println("No missing channels were found.");
            } else {
                // print result
                System.out.println("The following channels are missing: ");
                for (final String channel : missingChannels) {
                    System.out.println(channel);
                }
            }
        }
    }
    
	/**
	 * Main entry method.
	 * 
	 * @param args command line arguments 
	 */
	public static void main(final String[] args) {
		final PerspectiveDictionaryCheckerApp app = new PerspectiveDictionaryCheckerApp();
		try {
		    
			final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
			app.configure(commandLine);
		} catch (final ParseException e) {
			TraceManager.getDefaultTracer().fatal(e.getMessage());

			System.exit(1);
		}
		app.run();
		System.exit(0);
	}

	public ApplicationContext getApplicationContext() {
		return this.appContext;
	}
}
