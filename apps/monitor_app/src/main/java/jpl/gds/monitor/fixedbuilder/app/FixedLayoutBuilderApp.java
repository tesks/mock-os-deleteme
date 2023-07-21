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
package jpl.gds.monitor.fixedbuilder.app;

import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.monitor.fixedbuilder.FixedBuilderShell;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;

/**
 * This is the main application class for the Fixed View Builder.
 * 
 */
public class FixedLayoutBuilderApp extends AbstractCommandLineApp {

    /**
     * Command line application name
     */
    public static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_fixed_builder");

    // Non-standard command line option constants
    /**
     * Long command line option for passing the fixed builder application a file
     */
    public static final String FILENAME_LONG = "filename";
    /**
     * Short command line option for passing the fixed builder application a file
     */
    public static final String FILENAME_SHORT = "f";

    // Filename input by the user
    private String filename;

    private final FileOption fixedFileOption = new FileOption(FILENAME_SHORT, FILENAME_LONG, "filename", "Path to existing fixed layout file", false, true);
    private DictionaryCommandOptions dictOptions;
    private final Tracer log;
    
    private final ApplicationContext appContext;
    
    /**
     * Cosntructor
     */
    public FixedLayoutBuilderApp() {
    	appContext = SpringContextFactory.getSpringContext(true);
        log = TraceManager.getDefaultTracer(appContext);
    }
    
    /**
     * @return the current application context
     */
    public ApplicationContext getApplicationContext() {
    	return this.appContext;
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        
        super.configure(commandLine);
        dictOptions.parseAllOptionsAsOptionalWithDefaults(commandLine);
        this.filename = fixedFileOption.parse(commandLine); 
    }

    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }
        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));
        
        this.options.addOption(fixedFileOption);
        this.dictOptions = new DictionaryCommandOptions(appContext.getBean(DictionaryProperties.class));
        this.options.addOptions(dictOptions.getAllOptions());
        return this.options;
    }

    /**
     * Gets the filename set on the command line; may be null.
     * 
     * @return the file path entered on the command line
     */
    public String getFilename() {
        return this.filename;
    }

	/**
	 * Executes the fixed view builder
	 */
	public void run() {
		Display.setAppName(APP_NAME);
		Display display = null;

        // Check for an X display
        try {
            display = Display.getDefault();
        } catch (final SWTError e) {
            if (e.getMessage().indexOf("No more handles") != -1) {
                log.error("Unable to initialize user interface.");
                log.error("If you are using X-Windows, make sure your DISPLAY variable is set.");
                return;
            } else {
                throw (e);
            }
        }
        appContext.getBean(FlightDictionaryLoadingStrategy.class)
        .enableChannel()
        .enableAlarm();

        // Only enable sse if mission has it.
        if (appContext.getBean(MissionProperties.class).missionHasSse()) {
        	appContext.getBean(SseDictionaryLoadingStrategy.class)
        	.enableChannel()
        	.enableAlarm();
        }

        // Load the channel dictionaries
        try {
        	appContext.getBean(IChannelUtilityDictionaryManager.class).loadAll(false);
        } catch (final Exception e) {
            log.error(
                    "There was an error loading the channel dictionary");
            log.error(
                    e.getMessage() == null ? e.toString() : e.getMessage(), e);
            return;
        }

        // Execute the GUI application
        final FixedBuilderShell shell = new FixedBuilderShell(appContext, display,
                this.filename);
        shell.open();

        while (!shell.getShell().isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.cli.legacy.app.CommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }
        
        final PrintWriter pw = new PrintWriter(System.out);
        
        pw.println("Usage: " + APP_NAME + " [--fswVersion <version> --sseVersion <version> \n" +
                   "                            --fswDictionaryDir <directory> --sseDictionaryDir <directory>\n" +
                   "                            --filename <fixed-layout-file>]\n");

        options.getOptions().printOptions(pw);
        
        pw.flush();
    }

	/**
	 * Main entry method.
	 * 
	 * @param args the command line arguments 
	 */
	public static void main(final String[] args) {
		final FixedLayoutBuilderApp app = new FixedLayoutBuilderApp();
		try {
			final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
			app.configure(commandLine);
		} catch (final ParseException e) {
			TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(e));
			System.exit(1);
		}
		app.run();
		System.exit(0);
	}
}
