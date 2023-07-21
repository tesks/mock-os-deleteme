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
package jpl.gds.monitor.guiapp.app;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;
import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.options.GladClientCommandOptions;
import jpl.gds.message.api.MessageUtility;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.CheckMessageService;
import jpl.gds.message.api.external.MessageHeaderMode;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.message.api.util.MessageCaptureHandler;
import jpl.gds.message.api.util.MessageCaptureHandler.CaptureType;
import jpl.gds.monitor.config.MonitorDictionaryUtility;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.gui.MonitorMessageController;
import jpl.gds.monitor.guiapp.gui.MonitorPerspectiveActor;
import jpl.gds.monitor.guiapp.gui.MonitorPerspectiveListener;
import jpl.gds.monitor.guiapp.gui.MonitorShell;
import jpl.gds.monitor.guiapp.gui.MonitorStartupShell;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.perspective.ApplicationConfiguration;
import jpl.gds.perspective.ApplicationType;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.perspective.DisplayType;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.options.PerspectiveCommandOptions;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.MessageTypesOption;
import jpl.gds.shared.cli.options.OutputFormatOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.sys.QuitSignalHandler;
import jpl.gds.shared.template.MessageTemplateManager;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * MonitorApp is the main class for a message display utility (currently called
 * "chill_monitor"). It subscribes to a set of topics on the message service and
 * displays messages as it receives them, and/or writes them to files or a
 * directory. It can be run in GUI or command line mode. The configuration of
 * the GUI is completely controlled by the user perspective.
 *
 * This class is responsible for parsing the command line, establishing the
 * current MPCS session configuration, reading the initial perspective, starting
 * the messaging interface, and then launching the remainder of either the
 * command line or GUI application.
 */
public class MonitorApp extends AbstractCommandLineApp implements IQuitSignalHandler
{
    // Directory option
    public static final String                DIRECTORY  = "directory";

    // FileName option
    public static final String                FILENAME   = "filename";
    
    private static final String DEPRECATED = " (DEPRECATED)";
    
    private final Tracer                            log;

    private final String                      appName;
    private MonitorMessageController          messanger;
    private final IContextConfiguration        testConfig;
    private boolean                           topicComplete;
    private ApplicationConfiguration          appConfig;
    private final ApplicationContext          appContext;
    private final MessageServiceConfiguration msgConfig;

    private boolean                           gui = true;
    private boolean                           quiet = false;
    private boolean                           autorun = false;
    private boolean                           properties = false;
    private String                            dirName;
    private String                            fileName;

    private SessionCommandOptions             sessionOpts;
    private DictionaryCommandOptions          dictOpts;
    private MessageServiceCommandOptions      jmsOpts;
    private PerspectiveCommandOptions         perspectiveOpts;
    private ContextCommandOptions             contextOpts;
    private GladClientCommandOptions           gladClientOptions;

    // Defines long and short command line options specific to chill_monitor
    private OutputFormatOption                formatOpt;
    private DirectoryOption                   captureDirOpt;
    private FileOption                        captureFileOpt;
    private MessageTypesOption                msgTypeOpt;
    private FlagOption                        propertiesOpt;

    /**
	 * Main method.
	 *
	 * @param args list of command line argument Strings
	 */
	@SuppressWarnings("DM_EXIT")
    public static void main(final String[] args) {
		final boolean success = mainNoKill(args);
		
		// ActiveMQ JMS threads won't die otherwise
		System.exit(success ? 0 : 1);
	}
	
	/**
	 * Main method to support jUnit-SWTBot automated GUI testing;
	 * main method kills all threads before test is able to pass.
	 * 
	 * @param args command line arguments.
	 * @return boolean success true if successful, false otherwise
	 */
	public static boolean mainNoKill(final String[] args)
	{
		boolean success = true;
		
		// Interpret all times in the app as GMT. This is very critical.
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		
		try {
			
			// Create the application class and tell it to parse the command line arguments.
			// Exit out upon failure
            final MonitorApp app = new MonitorApp(
                    jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName("chill_monitor"));
            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
			try {
				app.configure(commandLine);
			} catch (final ParseException e) {
                TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(e));
				System.exit(1);
			}
			
			// Now run the app in either GUI or command line mode
			if (app.gui) {
				success = app.showGUI();
			} else {
				app.run();
			}
		} catch (final Exception e) {
            TraceManager.getDefaultTracer().error("Unexpected error: " + ExceptionTools.getMessage(e), e);
			System.exit(1);
		}
		
		return success;
	}


	/**
	 * Constructs a MonitorApp with the given script name.
	 *
	 * @param name the displayed application name
	 */
	public MonitorApp(final String name) {
        Runtime.getRuntime().addShutdownHook(new Thread(new QuitSignalHandler(this)));
		appName = name;
		
		appContext = SpringContextFactory.getSpringContext(Arrays.asList(this.getClass().getPackage().getName()),true);
        msgConfig = appContext.getBean(MessageServiceConfiguration.class);
        // Start with a default session configuration and make it the global one
		testConfig = new SessionConfiguration(appContext);
        log = TraceManager.getTracer(appContext, Loggers.DEFAULT);
	}

	/**
	 * Enables or disables GUI mode.
	 * @param b true for GUI mode
	 */
	private void setGui(final boolean b) {
		gui = b;
		appContext.getBean(MonitorChannelLad.class).setMonitorIsGui(b);
		appContext.getBean(MessageCaptureHandler.class).setEnableConsole(!b);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        if (options == null) {
            options = createOptions();
        }
        final PrintWriter pw = new PrintWriter(System.out);

		final List<IMessageConfiguration> availTypes = MessageRegistry.getAllMessageConfigs(true);
		final List<String> availStyles = MessageUtility.getMessageStyles(availTypes.toArray(new IMessageConfiguration[] {}));
		final StringBuilder types = new StringBuilder("Available message types are:\n   ");
		for (int i = 0; i < availTypes.size(); i++) {
			types.append(availTypes.get(i).getSubscriptionTag() + ' ');
			if (i != 0 && i % 5 == 0) {
				types.append("\n   ");
			}
		}
		
		final StringBuilder styles = new StringBuilder(1024);
		styles.append("\n\nKnown available message styles are:\n   ");
		for (final String s: availStyles) {
			styles.append(s + " ");
		}
		styles.append("\n\nNote that not all styles apply to all message types.");
		styles.append('\n');
		pw.println("Usage: " + appName + " [session options] [message service options]");
	    pw.println("       " + appName + " [--topics <topic[,topic...]>] [message service options]\n");
        options.getOptions().printOptions(pw);

        pw.println("\nThe command line (--noGUI) mode for this application is DEPRECATED.");
        pw.println("It will still function, but one of the watcher applications should be used instead.");
		pw.println("\nIf the --" + contextOpts.SUBSCRIBER_TOPICS.getLongOpt() + " option is supplied, the values should be root topics");
		pw.println("as opposed to data subtopics. The application will expand these to relevant");
		pw.println("data topics. If this option is not supplied, subscription topics will be created");
		pw.println("automatically from supplied session options.\n");
		pw.println("Message service host and port default to the values in the local AMPCS configuration.\n");
		pw.print(types.toString());
		pw.print(styles.toString());
		pw.print(MessageUtility.getTemplateDirectories());
		pw.flush();
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.cli.legacy.app.CommandLineApp#createOptions()
	 */
	@Override
    public BaseCommandOptions createOptions() {
	    if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));

        options.addOption(BaseCommandOptions.GUI);
        BaseCommandOptions.GUI.setDescription(BaseCommandOptions.GUI.getDescription() + DEPRECATED);
        options.addOption(BaseCommandOptions.QUIET);
        options.addOption(BaseCommandOptions.DEBUG);
        options.addOption(BaseCommandOptions.NO_GUI);
        BaseCommandOptions.NO_GUI.setDescription(BaseCommandOptions.NO_GUI.getDescription() + DEPRECATED);
        options.addOption(BaseCommandOptions.AUTORUN);
        
        sessionOpts = new SessionCommandOptions((SessionConfiguration) testConfig);
        options.addOptions(sessionOpts.getAllMonitorOptions(false));
                
        dictOpts = new DictionaryCommandOptions(appContext.getBean(DictionaryProperties.class));
        options.addOptions(dictOpts.getAllOptions());
        
        jmsOpts = new MessageServiceCommandOptions(appContext.getBean(MessageServiceConfiguration.class));
        options.addOptions(jmsOpts.getAllOptionsWithoutNoJms());
        
        perspectiveOpts = new PerspectiveCommandOptions(appContext.getBean(PerspectiveConfiguration.class),
                ApplicationType.MONITOR);     
        options.addOption(perspectiveOpts.PERSPECTIVE);
        options.addOption(perspectiveOpts.APPLICATION_CONFIGURATION);
        
        contextOpts = new ContextCommandOptions(testConfig);
        options.addOption(contextOpts.SUBSCRIBER_TOPICS);
        
        formatOpt = new OutputFormatOption(false);
        formatOpt.setDescription(formatOpt.getDescription() + DEPRECATED);
        captureDirOpt = new DirectoryOption("l", DIRECTORY,
                DIRECTORY, "Writes messages to separate files in the given directory as they "
                        + "are received. Only one of --" + DIRECTORY + " or --" + FILENAME + " can be specified. (DEPRECATED)",
                false, true);
        captureFileOpt = new FileOption("f", FILENAME, FILENAME,
                "Writes all messages to a single file as they are received. (DEPRECATED)", false, true);
        msgTypeOpt = new MessageTypesOption(false);
        msgTypeOpt.setDescription(msgTypeOpt.getDescription() + DEPRECATED);
        
        propertiesOpt = new FlagOption("r", "properties", "enables message header property display. (DEPRECATED)", false);

        // These are monitor-unique options
        options.addOption(formatOpt);
        options.addOption(captureDirOpt);
        options.addOption(captureFileOpt);
        options.addOption(msgTypeOpt);
        options.addOption(propertiesOpt);

        gladClientOptions = new GladClientCommandOptions(GlobalLadProperties.getGlobalInstance());
        options.addOption(gladClientOptions.SERVER_HOST_OPTION);
        options.addOption(gladClientOptions.REST_PORT_OPTION);

		return options;
	}


	/**
     * {@inheritDoc}
     */
	@Override
	@SuppressWarnings("DM_EXIT")
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

		// Create MonitorMessageController, which is the central object that manages subscriptions and 
		messanger = appContext.getBean(MonitorMessageController.class);

        autorun = BaseCommandOptions.AUTORUN.parse(commandLine);
        // Parse the autorun option. If this is set, we want topic completeness
        // check later
        topicComplete = autorun;

		// If there is a supplied session configuration, load it into the current
		// global session configuration

        sessionOpts.parseAllMonitorOptionsAsOptional(commandLine, false, true, true, autorun);

		final MessageCaptureHandler capture = appContext.getBean(MessageCaptureHandler.class);


		// If in console mode, turn off GUI display. Also tell the capture handler
		// so it knows whether to write to the console
		this.setGui(true);
        if (BaseCommandOptions.NO_GUI.parse(commandLine)) {
			this.setGui(false);
        } 

        if (commandLine.hasOption(sessionOpts.VENUE_TYPE.getLongOpt())) {
			final IVenueConfiguration venueConfig = appContext.getBean(IVenueConfiguration.class);
			appContext.getBean(IConnectionMap.class).setDefaultNetworkValuesForVenue(venueConfig.getVenueType(),
	                venueConfig.getTestbedName(),
	                venueConfig.getDownlinkStreamId());
		}

		// FSW and SSE dictionary options. If any specified, turn off auto-load of session dictionaries
        dictOpts.parseAllOptionsAsOptionalWithDefaults(commandLine);

        appContext.getBean(GeneralMessageDistributor.class)
                .setAutoLoadDictionary(!(commandLine.hasOption(dictOpts.FSW_DICTIONARY_DIRECTORY.getLongOpt())
                        || commandLine.hasOption(dictOpts.SSE_DICTIONARY_DIRECTORY.getLongOpt())
                        || commandLine.hasOption(dictOpts.FSW_VERSION.getLongOpt())
                        || commandLine.hasOption(dictOpts.SSE_VERSION.getLongOpt())));
	
        // Parse the application configuration file. This link to the perspective is supplied when 
        // chill_monitor runs as part of the integrated GUI
        appConfig = perspectiveOpts.APPLICATION_CONFIGURATION.parse(commandLine);
        
        // If this is a stand alone monitor and a perspective is supplied then load the perspective.
        // If this is a stand alone monitor and NO perspective is supplied then
        // load the default perspective.
        if (appConfig == null && commandLine.hasOption(perspectiveOpts.PERSPECTIVE.getLongOpt())) {
            perspectiveOpts.PERSPECTIVE.parse(commandLine);
            final PerspectiveConfiguration pc = appContext.getBean(PerspectiveConfiguration.class);
            log.info("Using perspective " + pc.getConfigPath());
            appConfig = pc.getApplicationConfiguration(ApplicationType.MONITOR);
            if (appConfig == null) {
                throw new ParseException("Application configuration file was not found in perspective directory " + pc.getConfigPath());
            }
        } else if (appConfig == null) {
            final PerspectiveConfiguration pc = appContext.getBean(PerspectiveConfiguration.class);
            try {   
                pc.create();
                pc.assignNewAppId();
                pc.writeAppId();
                log.info("Using perspective " + pc.getConfigPath());
            } catch (final Exception e) {
                throw new ParseException("Problem loading default perspective configuration " + 
                        ": " + e.getMessage() == null ?
                                e.toString() : e.getMessage());
            }
            appConfig = pc.getApplicationConfiguration(ApplicationType.MONITOR);
            if (appConfig == null) {
                throw new ParseException("Application configuration file was not found in default perspective directory");
            }
        }
        
		

		// If the quiet option is supplied, turn off console display of messages
        if (BaseCommandOptions.QUIET.parse(commandLine)) {
			quiet = true;
			appContext.getBean(MessageCaptureHandler.class).setEnableConsole(false);
        }
        
        // Set log level based upon debug and quiet options
        if (BaseCommandOptions.DEBUG.parse(commandLine)) {
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.DEBUG);
        } else if (quiet) {
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.ERROR);
        }

		// If the output to directory option is supplied, check and save directory name
        dirName = captureDirOpt.parse(commandLine);
        if (dirName != null && !capture.setOutputDir(dirName)) {
            throw new ParseException("Invalid output directory: " + dirName);
        }

		// If the output to file option is supplied, check and save filename
        fileName = captureFileOpt.parse(commandLine);
        if (fileName != null && !capture.getWriteMode().equals(CaptureType.WRITE_NONE)) {
            throw new ParseException("You cannot specify both --" + captureFileOpt.getLongOpt() + "and --"
                    + captureDirOpt.getLongOpt() + " options.");
        }
        if (fileName != null && !capture.setOutputFile(fileName)) {
            throw new ParseException("Invalid output file: " + fileName);
        }

		// Parse the option that filters for given message types
        if (!msgTypeOpt.parse(commandLine).isEmpty()) {
            capture.setCaptureMessageFilter(commandLine.getOptionValue(msgTypeOpt.getLongOpt()));
        }

		// Parse the option enabling message header property display
        properties = propertiesOpt.parse(commandLine);
        if (properties) {
			capture.setHeaderMode(MessageHeaderMode.HEADERS_ON);
		}

		// Parse option for default message output format style
        final String outputFmt = formatOpt.parse(commandLine);
        if (outputFmt != null) {
            capture.setCaptureMessageStyle(outputFmt);
        } else {
            capture.setCaptureMessageStyle(MessageTemplateManager.XML_MESSAGE_STYLE);
        }
	
		// If these options are not entered or defaulted, and we are not going on to
		// the monitor startup window where the user can enter it, then error out
		if ( testConfig.getContextId().getUser() == null && (!gui || topicComplete)) {
			throw new MissingOptionException("You must supply the username of the account generating messages");
		}
		if (testConfig.getContextId().getHost() == null && (!gui || topicComplete)) {
			throw new ParseException("You must supply the name of the host generating the messages.");
		}

		// Parse the JMS host and port options
        jmsOpts.parseAllOptionsAsOptional(commandLine);
        
        contextOpts.SUBSCRIBER_TOPICS.parse(commandLine);

        // Removed db parsing opts. They were being parsed in AMPCS R7 but not
        // present in the --help dialog

        gladClientOptions.SERVER_HOST_OPTION.parseWithDefault(commandLine, false, true);
        gladClientOptions.REST_PORT_OPTION.parseWithDefault(commandLine, false, true);	
	}

	/**
	 * Runs the application in GUI mode
	 * 
	 * @return true if the GUI executed successfully, false otherwise
	 */
	public boolean showGUI()
	{
        
		 // Turn on SWT Device debugging
		//Display.DEBUG = true; 
		
		// This makes the chill_monitor menu show up with that name on the Mac menu bar
		Display.setAppName(appName);
		
		// Create the SWT display.  The exception checks for lack of X-Windows DISPLAY variable.
		// Otherwise, the message is incomprehensible to users
		Display display = null;
		try {
			display = Display.getDefault();
		} catch (final SWTError e) {
			if (e.getMessage().indexOf("No more handles") != -1) {
				log.error("Unable to initialize user interface.");
				log.error("If you are using X-Windows, make sure your DISPLAY variable is set.");
				return false;
			} else {
				throw(e);
			}
		}

		MonitorStartupShell testShell = null;

		// If autorun option was supplied and we have all the info we need to start listening on the
		// right topics, we can skip the startup shell. Otherwise, display it to get the rest
		// of the configuration information we need
		if (!topicComplete)
        {
            testShell = new MonitorStartupShell(appContext, display);
	    testShell.getShell().setVisible(false);
                        testShell.open();
			while (!testShell.getShell().isDisposed()) {
				if (!display.readAndDispatch())
				{
					display.sleep();
				}
			}
			//user hit "Cancel" button
			if (testShell.wasCanceled()) {
				display.dispose();
				return true;
			}
			//exit if the shell is disposed and the user did not click the "Run" button
			if(testShell.getShell().isDisposed() && !testShell.getRunMonitor()) {
				display.dispose();
				return true;
			}
			appContext.getBean(GeneralMessageDistributor.class).setAutoLoadDictionary(testShell.isAutoLoadDictionary());
		}

		// Verify the message service is running and exit if not
		if (! CheckMessageService.checkMessageServiceRunning(msgConfig, msgConfig.getMessageServerHost(), 0, log, true))
		{
			// There's nothing we can do
			return false;
		}
		
		try {
            appContext.getBean(MonitorDictionaryUtility.class).init(appContext);
            appContext.getBean(MonitorDictionaryUtility.class).loadAllDictionaries();
        } catch (final Exception e) {
            log.error("Unable to load required dictionaries: " + e.getMessage() + " - This monitor will exit.");
            log.debug("Exception was: " + e.toString(), e);
            return false;
        }

		// This while-not-exit insanity is to allow restart with a modified perspective
		boolean exit = false;
		while (!exit) {
			
			DisplayConfiguration dispConfig = null;
			if (appConfig != null) {
				dispConfig = appConfig.getDisplayConfig(DisplayType.MESSAGE);
			}
			
			// Fire up the master monitor SWT shell. Assign a perspective listener to it, which
			// will send, receive, and respond to perspective messages.
			final MonitorShell ms = new MonitorShell(appContext, display, testConfig, dispConfig);
			if (appConfig != null) {
				/*
				 * Pass false to the PerspectiveActor so it
				 * does not create its own client heartbeat. The MonitorMessageController
				 * will create the heartbeat. Setting the PerspectiveActor in the
				 * MonitorMessageController will cause the PerspectiveActor to become
				 * a heartbeat listener so it can know when the message service goes down.
				 */
				final MonitorPerspectiveListener listener = new MonitorPerspectiveActor(appContext, appConfig, ms, false);
				ms.setPerspectiveListener(listener);
				appContext.getBean(MonitorMessageController.class).setPerspectiveActor((MonitorPerspectiveActor)listener);
			}
			ms.init();
			ms.open();
			
			// If global LAD enabled, trigger a fetch of the global LAD to get the latest data from chill_down
			if (GlobalLadProperties.getGlobalInstance().isEnabled()) {
				appContext.getBean(MonitorChannelLad.class).triggerLadFetch();
			}
			
			// Start processing SWT events
			while (!ms.getShell().isDisposed()) {
				if (!display.readAndDispatch())
				{
					display.sleep();
				}
			} 
			
			// If restarting, load the latest application configuration from the perspective
			// Restart occurs when user elects to edit the perspective and then requests restart
			exit = !ms.isRestart();
			if (!exit && appConfig != null) {
				appConfig.load(appConfig.getConfigFilename());
			}
		}

		// We're exiting. Close the messaging connections and dispose of SWT resources
		shutdown();
		display.dispose();
		return true;
	}

	/**
	 * Runs the application in command line mode.
	 */
	public void run()
	{

		// Make sure the message service is running and exit if not
		if (! CheckMessageService.checkMessageServiceRunning(msgConfig, null, 0, log, true))
		{
			// There's nothing we can do
			return;
		}

		try {
            appContext.getBean(MonitorDictionaryUtility.class);
            appContext.getBean(MonitorDictionaryUtility.class).loadAllDictionaries();
        } catch (final Exception e) {
            log.error("Unable to load dictionaries: " + e.getMessage());
            return;
        }        	

		// Fire up the message bus subscriptions
		messanger.createSubscribers();
		log.info("Waiting for messages...\n");
		messanger.startMessageReceipt();

		// Sleep forever or until interrupted by user. Messages will be processed
		// by MessageCaptureHandler
		while (true)
		{
			if (SleepUtilities.checkedSleep(3600000L))
			{
				shutdown();
				break;
			}
		}
	}

	/**
	 * Shuts down the application resources, which currently consists of stopping message subscribers.
	 */
	public void shutdown() {
		messanger.closeSubscribers();

		appContext.getBean(MonitorChannelLad.class).cancelLadTimer();
	}

    @Override
    public void exitCleanly() {
        log.debug(appName + " received a shutdown request.  Shutting down gracefully...");
        try {
            shutdown();
        } catch (final Exception e) {
            // don't care - can't do anything at this point anyway
        }

    }

    /**
     * Get the application context used by this app
     * 
     * @return the ApplicationContext for this app
     */
    public ApplicationContext getApplicationContext() {
        return appContext;
    }

    /**
     * Getter for autorun flag
     * 
     * @return autorun flag as boolean
     */
    public boolean isAutoRun() {
        return autorun;
    }

    /**
     * Getter for quiet flag
     * 
     * @return quiet flag as boolean
     */
    public boolean isQuiet() {
        return quiet;
    }

    /**
     * Getter for the gui flag
     * 
     * @return gui as boolean
     */
    public boolean isGui() {
        return gui;
    }

    /**
     * Getter for properties flag
     * Message header properties will or won't be displayed based on it
     * 
     * @return properties flag as boolean
     */
    public boolean hasProperties() {
        return properties;
    }

    /**
     * Getter for capture directory name
     * Can be null if not capturing
     * 
     * @return Capture directory as string
     */
    public String getDirName() {
        return dirName;
    }

    /**
     * Getter for the capture file name
     * Can be null if not capturing
     * 
     * @return Capture file name as string
     */
    public String getFileName() {
        return fileName;
    }

}
