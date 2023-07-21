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
package jpl.gds.telem.down.app;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import jpl.gds.telem.common.app.DownlinkErrorManager;
import jpl.gds.telem.common.app.ExitCodeHandler;
import org.apache.commons.cli.ParseException;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.common.config.bootstrap.ChannelLadBootstrapConfiguration;
import jpl.gds.common.config.bootstrap.options.ChannelLadBootstrapCommandOptions;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.connection.IDatabaseConnectionSupport;
import jpl.gds.common.config.connection.options.ConnectionCommandOptions;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.config.types.DatabaseConnectionKey;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.error.ErrorCode;
import jpl.gds.common.options.DownlinkAutostartOption;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.EnableFswDownlinkContextFlag;
import jpl.gds.context.api.EnableSseDownlinkContextFlag;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.eha.api.channel.IChannelLad;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.eha.api.service.alarm.IAlarmNotifierService;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.options.GladClientCommandOptions;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.perspective.ApplicationConfiguration;
import jpl.gds.perspective.options.PerspectiveCommandOptions;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.gui.SessionConfigShell;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.context.cli.app.mc.AbstractRestfulServerCommandLineApp;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.telem.common.app.mc.DownlinkProcessingState;
import jpl.gds.telem.common.app.mc.IRestfulTelemetryApp;
import jpl.gds.telem.down.DownConfiguration;
import jpl.gds.telem.down.DownlinkSessionManager;
import jpl.gds.telem.down.IDownlinkApp;
import jpl.gds.telem.down.gui.AbstractDownShell;
import jpl.gds.telem.input.api.config.BufferedInputModeType;
import jpl.gds.telem.input.api.config.BufferedInputModeTypeOption;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;

/**
 * AbstractDownlinkApp is the actual workhorse of the downlink application, from which
 * FSW and SSE instances of the downlink are extended. This application manages the downlink 
 * session from start to finish.
 * 
 *
 */

public abstract class AbstractDownlinkApp extends AbstractRestfulServerCommandLineApp implements IDownlinkApp {
    /**
     * Trace logger to share with subclasses.
     */
    protected Tracer                                log;

    private static final String                     WAIT_OPTION_SHORT              = "w";
    public static final String                      WAIT_OPTION_LONG               = "wait";

    private static final String                     SUPPLIED_SESSION               = "Supplied session ";
    private static final String                     YOU_MUST_SPECIFY               = "You must specify --";
    private static final String                     DOWNLINK_CONNECTION            = " downlink connection";


    /**
     * The downlink telemetry input port number.
     */
    protected int                                   port                           = -1;

    /**
     * Downlink source host.
     */
    protected String                                host;

    /**
     * Application configuration for this application, from the user perspective.
     */
    protected ApplicationConfiguration              appConfig;

    /**
     * Context configuration for the downlink session.
     */
    protected IContextConfiguration                 contextConfig;

    private final String                            appName;
    private IMessagePublicationBus                  context;
    private IMessagePortal                          jmsPortal;
    private boolean                                 gui                            = true;

    private DownConfiguration                       downConfig;
    protected boolean                               autoRun;
    private boolean                                 autoStart;
    private boolean                                 quiet;
    private long                                    meterInterval;
    private DownlinkSessionManager                  sessionManager;
    private boolean                                 isAbort;
    private Display                                 mainDisplay                    = null;
    private SessionConfigShell                      configShell                    = null;
    private AbstractDownShell                       guiShell                       = null;
    private IChannelLad                             channelLad;
    private IAlarmHistoryProvider                   alarmHistory;

    /** Session command options container */
    protected SessionCommandOptions                 sessionOpts;
    /** Dictionary command options container */
    protected DictionaryCommandOptions              dictOpts;
    /** Perspective command options container */
    protected PerspectiveCommandOptions             perspectiveOpts;
    private DatabaseCommandOptions                  dbOptions;
    private MessageServiceCommandOptions            jmsOptions;
    /** Connection command options container */
    protected ConnectionCommandOptions              connectionOpts;
    private UnsignedLongOption                      waitOption;
    private DownlinkAutostartOption                 autostartOption;
    
    private GladClientCommandOptions                gladClientOptions;

    /** Buffered input mode command line option */
    protected BufferedInputModeTypeOption           bufferOption;
    /** Buffered Context command line option */
    protected ContextCommandOptions                 contextOpts;
    /** ChannelLadBootstrap command options container */
    protected ChannelLadBootstrapCommandOptions     bootstrapOpts;
    /** Current application context */
    protected final ApplicationContext              springContext;
    /** Current Time Comparison Strategy */
    private final TimeComparisonStrategyContextFlag timeStrategy;
    /** Secure class loader */
    protected AmpcsUriPluginClassLoader             secureLoader;

    private final AtomicBoolean                     exiting                        = new AtomicBoolean(false);

    private DownlinkProcessingState                 currentDownlinkProcessingState = DownlinkProcessingState.UNBOUND;

    private final Object                            restFulStartMonitor            = new Object();

    private boolean                                 hasBeenStarted                 = false;

    /**
     * Creates an instance of AbstractDownlinkApp.
     * 
     * @param springContext
     *            the Spring Application Context
     * 
     * @param appName
     *            the name of the application script (for help display)
     */
    protected AbstractDownlinkApp(final ApplicationContext springContext, final String appName) {
        //Reinstate the standard quit signal handler by passing true here.
        super(true);
        this.springContext = springContext;
        this.timeStrategy = springContext.getBean(TimeComparisonStrategyContextFlag.class);
        this.appName = appName;
    }

    /**
     * @throws BeansException
     *             if initialization fails
     */
    @PostConstruct
    public void init() throws BeansException {
        contextConfig = new SessionConfiguration(springContext);
        log = TraceManager.getTracer(springContext, Loggers.DOWNLINK);
        downConfig = springContext.getBean(DownConfiguration.class);
        context = springContext.getBean(IMessagePublicationBus.class);
        channelLad = springContext.getBean(IChannelLad.class);
        alarmHistory = springContext.getBean(IAlarmHistoryProvider.class);
        secureLoader = springContext.getBean(AmpcsUriPluginClassLoader.class);
    }

// R8 Refactor - Commenting out everything related to session restart
//    /**
//     * Resets state for session restart.
//     * 
//     * @param resetSessionNumber true to also reset the session configuration 
//     * to get a new key, false to not
//     * 
//     * @return true if the reset succeeded, false if not.
//     * 
//     */
//    @SuppressWarnings({"DM_GC", "PMD.DoNotCallGarbageCollectionExplicitly"})
//    public boolean resetSession( final boolean resetSessionNumber )
//    {
//    	return resetSession( resetSessionNumber, true );
//    }
//    
//    /**
//     * Resets state for session restart.
//     * 
//     * @param resetSessionNumber true to also reset the session configuration 
//     * to get a new key, false to not
//     * @param stopPortal true allows the jmsPortal to de-queue existing messages
//     * 
//     * @return true if reset successful, false if not
//     * 
//     *
//     * @TODO  R8 Refactor TODO Figure out how to reset reliably when using
//     *             the spring context. Resetting static factories won't do it.
//     */
//    @SuppressWarnings({"DM_GC", "PMD.DoNotCallGarbageCollectionExplicitly"})
//    public boolean resetSession(final boolean resetSessionNumber, final boolean stopPortal) {
//
//    	try {
//    		if (sessionManager != null) {
//    			sessionManager.stopHeartbeat();
//    		}
//    		if (jmsPortal != null) {
//    			if ( stopPortal )
//    			{
//    				jmsPortal.stopService();
//    			}
//    			jmsPortal = null;
//    		}
//    		/* Moved reset of session number and 
//    		 * restart of portal to the end of the reset logic.
//    		 */
//    		springContext.getBean(IChannelDictionaryManager.class).clearAll();
////    		ApidDictionaryFactory.resetStaticInstance();
////    		EvrDictionaryFactory.resetStaticInstance();
////    		ProductMissionAdaptorFactory.reset();
////    		CommandDictionaryFactory.resetStaticInstance();
////    		SequenceDictionaryFactory.resetStaticInstance();
////            // Use decom factory.
////            ChannelDecomDictionaryFactory.resetStaticInstance();
//
//
//    		PacketHeaderFactory.reset();
//
//    		// FindBugs and PMD warning on this is suppressed.  We want the app as small as possible
//    		// for each session restart. 
//    		System.gc();
//
//    		if (resetSessionNumber) {
//    			sessionConfig.clearFieldsForNewConfiguration();
//    		}
//
//    		startMessagingInterfaces();
//    		
//    	} catch (Exception | Error e) {
//    		e.printStackTrace();
//    		log.fatal("Session reset failed due to unexpected error (" + e.toString() + "): No new session can be started");
//    		return false;
//    	}
//    	return true;
//    	
//    }


    @Override
    public boolean isAutoStart() {
        return autoStart;
    }


    @Override
    public boolean isAutoRun() {
        return autoRun;
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
     * Getter for meterInterval
     * 
     * @return meterInterval as long
     */
    public long getMeterInterval() {
        return meterInterval;
    }


    @Override
    public boolean hasBeenStarted() {
        return hasBeenStarted;
    }

    /**
     * Method to create the GUI shell object appropriate for this application.
     * 
     * @param display
     *            the Display parent for the new shell.
     * @return a AbstractDownShell object, which should be initialized by not opened
     */
    protected abstract AbstractDownShell createGuiShell(Display display);


    @Override
    public void abort() {
        isAbort = true;
        currentDownlinkProcessingState = DownlinkProcessingState.UNBOUND;
    }


    @Override
    public void launchApp() {
        if (useGui()) {
            launchGuiApp();
        }
        else {
            launchCommandLineApp();
        }
    }


    @Override
    public void launchCommandLineApp() {

        // initialize the non-session aspects of the application
        initializeApplication();

        // Update the host configuration from command line parameters
        // and initialize the test configuration. The false parameter
        // indicates that this is not a shared session with any other
        // application so session information should be created in the
        // database.
        setContextConfiguration(contextConfig, false);

        // Write out session configuration.
        // Add flag to call to control whether config is written
        IRestfulTelemetryApp.writeOutContextConfig(springContext, contextConfig, false);

        // Write out configuration properties
        IRestfulTelemetryApp.writeOutConfigProperties(springContext);

        // Add dictionary lib jar files to the classpath
        contextConfig.getDictionaryConfig()
                     .loadDictionaryJarFiles(false, secureLoader, log);

        /*
         * if RESTful interface is enabled and --autoStart is NOT specified, wait for
         * RESTful 'start' command before beginning DownlinkData Processing
         */
        final boolean restFulInterfaceEnabled = springContext.getBean(ChillDownSpringBootstrap.REST_CONTAINER_ENABLED,
                                                                      Boolean.class);
        if (restFulInterfaceEnabled) {
            contextConfig.setRestPort(UnsignedInteger.valueOf(getRestPort()));
        }
        if (restFulInterfaceEnabled && !autoStart) {
            synchronized (restFulStartMonitor) {
                try {
                    log.info("Waiting for RESTful 'start' command to begin Downlink Data Processing...");
                    // wait indefinitely until RESTful 'start' command calls startDownload().
                    restFulStartMonitor.wait();
                }
                catch (final InterruptedException e) {
                    // Unexpected shutdown the non-session aspects of the application
                    shutdownApplication();
                    throw new IllegalStateException("Downlink session aborted: " + e.getLocalizedMessage());
                }
            }
        }

        // run the telemetry processing session
        run();

        // shutdown the non-session aspects of the application
        shutdownApplication();
    }


    @Override
    public void startDownlink() {
        /*
         * if RESTful interface is enabled and --autoStart is NOT specified, release
         * wait() condition in launchCommandLineApp()
         */
        final boolean restFulInterfaceEnabled = springContext.getBean(ChillDownSpringBootstrap.REST_CONTAINER_ENABLED,
                                                                      Boolean.class);
        if (restFulInterfaceEnabled) {
            contextConfig.setRestPort(UnsignedInteger.valueOf(getRestPort()));
        }
        if (restFulInterfaceEnabled && !autoStart) {
            log.info("Received RESTful 'start' command -- Beginning Downlink Data Processing now...");
            synchronized (restFulStartMonitor) {
                // notify launchCommandLineApp() that RESTful 'start' command has been received.
                restFulStartMonitor.notifyAll();
                try {
                    // wait indefinitely for run() method releases after starting downlink data processing
                    restFulStartMonitor.wait();
                }
                catch (final InterruptedException e) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Displays the GUI application. The input flags are used only if not in autoRun mode
     * to set the default launch flags in the session configuration shell.
     * 
     * @param runFsw
     *            true if the default for "Launch FSW Downlink" should be true
     * @param runSse
     *            true if the default for "Launch SSE Downlink" should be true
     */
    @SuppressWarnings({ "DM_EXIT" })
    protected void launchGuiApp(final boolean runFsw, final boolean runSse) {
        // create the GUI Display object
        Display.setAppName(getAppName());
        try {
            try {
                mainDisplay = Display.getDefault();
            }
            catch (final SWTError e) {
                if (e.getMessage().indexOf("No more handles") != -1) {
                    log.error("Unable to initialize user interface.");
                    log.error("Error: If you are using X-Windows, make sure your DISPLAY variable is set.");
                }
                else {
                    throw (e);
                }
            }
            // need to set whether autoRun or not
            if (!GdsSystemProperties.isIntegratedGui()) {
                springContext.getBean(EnableFswDownlinkContextFlag.class).setFswDownlinkEnabled(runFsw);
                springContext.getBean(EnableSseDownlinkContextFlag.class).setSseDownlinkEnabled(runSse);
            }
            // If in autoRun, skip the session configuration window. Otherwise, display it and wait
            // for the user to enter the session data.
            if (!autoRun) {

                // initialize and display the configuration shell


                configShell = new SessionConfigShell(springContext, mainDisplay, false, true, true,
                                                     (SessionConfiguration) contextConfig);
                configShell.open();

                // wait for it
                while (!configShell.getShell().isDisposed()) {
                    if (!mainDisplay.readAndDispatch()) {
                        mainDisplay.sleep();
                    }
                }
            }

            // If the user did not cancel the session, reset the test configuration to the
            // new one. The true argument to setContextConfiguration() indicates that we are not using
            // a shared configuration, and so a new session should be created. We know this
            // because in an integrated, shared session, the autoRun flag would be set.
            final boolean configCancelled = (null != configShell) && configShell.wasCanceled();
            if (!configCancelled) {
                if (configShell != null) {
                    contextConfig = configShell.getSessionConfiguration();
                }

                // Change true condition in setContextConfiguration to check
                // if we are integrated gui. In R7 this bit of code was inside the if (!autoRun) condition.
                setContextConfiguration(contextConfig, !GdsSystemProperties.isIntegratedGui());
                configShell = null;

                // Write out session configuration.
                // Add flag to call to control whether config is written
                IRestfulTelemetryApp.writeOutContextConfig(springContext, contextConfig, false);

                // Write out configuration properties
                IRestfulTelemetryApp.writeOutConfigProperties(springContext);

                // Add dictionary lib jar files to the classpath
                contextConfig.getDictionaryConfig()
                             .loadDictionaryJarFiles(false, secureLoader, log);

                // initialize the non-session aspects of the application before any GUI display
                initializeApplication();

                // Create the main GUI shell and display it
                this.guiShell = createGuiShell(mainDisplay);
                this.guiShell.open();

                // Wait for the GUI to exit. The shell will execute the run() method
                while (!this.guiShell.getShell().isDisposed()) {
                    if (!mainDisplay.readAndDispatch()) {
                        mainDisplay.sleep();
                    }
                }
                // Clean up the non-session aspects of the application
                // Let shutdown hook execute exitCleanly()
            }
        }
        catch (final Exception e) {
            log.error("Unexpected exception in chill_down GUI processing: " + ExceptionTools.getMessage(e), e);
        }
        finally {
            if (this.guiShell != null) {
                this.guiShell.stopGuiListeners();
            }
            // mainDisplay.dispose();
        }
    }

    /**
     * Initializes the application prior to a session run. The messaging interfaces must
     * be started first, because the remaining startup will publish messages that need to
     * be published externally.
     */
    private void initializeApplication() {

        // initContextFromSession();

        // start the messaging
        startMessagingInterfaces();

        createOutputDirectory();

        // Notify the user that the process is starting
        sendRunningMessage();
    }
    //
    // private void initContextFromSession() {
    // springContext.getBean(IGeneralContextInformation.class).setRootPublicationTopic(ContextTopicNameFactory.getTopicNameFromConfigValue(springContext,
    // TopicNameToken.APPLICATION));
    // }

    /**
     * Creates the session output directory.
     */
    private void createOutputDirectory() {
        // create the output directory if it doesn't exist
        // 5/23/13 - Fixed PMD nested IF violation and
        // raw exception violation
        final File testDirFile = new File(springContext.getBean(IGeneralContextInformation.class).getOutputDir());
        if (!testDirFile.exists() && !testDirFile.mkdirs()) {
            log.error("Unable to create session output directory: "
                    + springContext.getBean(IGeneralContextInformation.class).getOutputDir());
            throw new IllegalStateException("Downlink session cannot be started");
        }
    }

    /**
     * Cleans up the application after a session run. The messaging components
     * must be the last to go in the event that any other shutdown activities
     * publish messages.
     *
     */
    private void shutdownApplication() {

        // Now stop the messaging -- MUST BE the last thing to go
        stopMessagingInterfaces();
    }


    @Override
    public void run() {

        // Create the session manager
        sessionManager = new DownlinkSessionManager(springContext, contextConfig, !GdsSystemProperties.isIntegratedGui());

        // Start up the downlink session. See DbSqlService for detailed
        // explanation of the boolean flag.
        //

        /*
         * Removed boolean ok flag. Exceptions
         * are now thrown from helper methods below instead of returning false
         * when there is a problem. Added a try-catch.
         */

        try {
            sessionManager.startSessionDatabase();

            if (downConfig.getFeatureSet().isEnablePreChannelizedDecom()) {

                // give it time to start the listener before sending any session messages
                SleepUtilities.fullSleep(1000L, log, "AbstractDownlinkApp.run Error sleeping");
            }



            // Send start message and start heartbeat message publication
            sessionManager.startSession();
            currentDownlinkProcessingState = DownlinkProcessingState.STARTED;
            synchronized (restFulStartMonitor) {
                // Notify RESTful command handler in startDownlink() that dawnlink has been started
                restFulStartMonitor.notifyAll();
            }
            hasBeenStarted = true;

            // Process the telemetry
            final boolean inputOk = sessionManager.processInput();
            currentDownlinkProcessingState = DownlinkProcessingState.BOUND;

            if (!inputOk) {
                log.error("There was an unexpected problem processing the input data or executing the session. The session has been killed.");
                errorCode = ErrorCode.BAD_SESSION_ERROR_CODE.getNumber();
            }

            // End/cleanup the downlink session
            if (!sessionManager.isSessionEnded()) {
                sessionManager.endSession(!useGui());
            }

        }
        catch (final Exception e) {
            currentDownlinkProcessingState = DownlinkProcessingState.BOUND;
            synchronized (restFulStartMonitor) {
                // Notify RESTful command handler in startDownlink() that dawnlink has been started
                restFulStartMonitor.notifyAll();
            }

            /*
             * Now prints information about exception and
             * dumps a stack trace in the event of an unexpected error that causes a
             * FATAL exception.
             */
            log.error("The session could not be started: " + e.toString(), e.getCause());

            // Make sure the GUI knows the session never started, if there is a GUI.
            if (this.guiShell != null) {
                this.guiShell.forceSessionStop();
            }

            // End/cleanup the downlink session
            if (!sessionManager.isSessionEnded()) {
                sessionManager.endSession(false);
            }

            errorCode = DownlinkErrorManager.getSessionErrorCode(e);
        }
    }

    @Override
    public void exitCleanly() {
        /* 
         * This method hangs if it tries to log after a help or
         * version request. Just return if so.
         */
        if (helpDisplayed.get() || versionDisplayed.get()) {
            return;
        }
        // Global 'Tracer can be null if spring did not initialize correctly
        final Tracer quitTracer = TraceManager.getTracer(Loggers.DOWNLINK);
        
        quitTracer.debug("Application is shutting down");
        
        if (exiting.getAndSet(true)) {
            return;
        }

        /*
         * Added unconditional call to sessionManager.stop()
         * to free up a possibly retrying socket connection.
         */
        if (sessionManager != null) {
            quitTracer.info("Stopping session manager");
            sessionManager.stop();
        }

        if (!isAbort) {
            if (sessionManager != null) {
                quitTracer.debug("Not in abort mode, session manager=" , sessionManager, ", session ended=", sessionManager.isSessionEnded());
                if (!sessionManager.isSessionEnded()) {
                    quitTracer.info("Ending session");
                    sessionManager.endSession(useGui());
                }
                quitTracer.info("Stopping heartbeat");
                sessionManager.stopHeartbeat();
            }
            else {
                quitTracer.debug("Not in abort mode, Session manager is null");
            }
            quitTracer.debug("Shutdown application");
            shutdownApplication();
        }
        else {
            quitTracer.info("In abort mode. Exiting without shutdown.");
        }

        // Closing spring context should be last
        if (springContext != null && springContext instanceof ConfigurableApplicationContext) {
                final ConfigurableApplicationContext closable = (ConfigurableApplicationContext) springContext;
                if (closable.isActive() && closable.isRunning()) {
                    SpringApplication.exit(springContext, new ExitCodeHandler(this));

                }
            }
    }

    /**
     * Starts up the messaging components and connections.
     *
     */
    private void startMessagingInterfaces() {
        // Start the portal to the external JMS bus
        if (jmsPortal == null) {

            jmsPortal = springContext.getBean(IMessagePortal.class);

            jmsPortal.startService();

        }
    }

    /**
     * Stops the messaging components and connections.
     *
     */
    private void stopMessagingInterfaces() {

        if (jmsPortal != null) {
            jmsPortal.stopService();
            jmsPortal = null;
        }

        if (context != null) {
            context.unsubscribeAll();
        }
    }

    /**
     * Publishes a "process running" message.
     *
     */
    private void sendRunningMessage() {
        final IPublishableLogMessage rm = springContext.getBean(IStatusMessageFactory.class)
                                                       .createRunningMessage(getAppName());

        if (context != null) {
            context.publish(rm);
        }
        log.log(rm);
    }

    @Override
    public void setContextConfiguration(final IContextConfiguration config, final boolean setInstanceFields) {
        this.contextConfig = config;

        if (setInstanceFields) {
            // Do a more thorough job of resetting certain fields.
            contextConfig.clearFieldsForNewConfiguration();
        }
    }

    @Override
    public synchronized void setTimeComparisonStrategy(final TimeComparisonStrategy strategy) {
        // set the strategy
        timeStrategy.setTimeComparisonStrategy(strategy);
        // clear the chill_down LAD
        clearChannelState();
    }


    @Override
    public TimeComparisonStrategy getTimeComparisonStrategy() {
        return timeStrategy.getTimeComparisonStrategy();
    }


    @Override
    public DownlinkProcessingState getProcessingState() {
        final AbstractDownShell shell = guiShell;
        if (useGui() && (shell != null)) {
            return shell.getProcessingState();
        }
        else {
            return currentDownlinkProcessingState;
        }
    }


    @Override
    public void pause() {
        if (sessionManager != null) {
            sessionManager.pause();
            currentDownlinkProcessingState = DownlinkProcessingState.PAUSED;
        }
    }


    @Override
    public void resume() {
        if (sessionManager != null) {
            sessionManager.resume();
            currentDownlinkProcessingState = DownlinkProcessingState.STARTED;
        }
    }


    @Override
    public void stop() {
        if (sessionManager != null) {
            sessionManager.stop();
            currentDownlinkProcessingState = DownlinkProcessingState.UNBOUND;
        }
    }

    @Override
    public boolean useGui() {
        return gui;
    }


    @Override
    public DownConfiguration getDownConfiguration() {
        return downConfig;
    }


    @Override
    public IContextConfiguration getContextConfiguration() {
        return contextConfig;
    }


    @Override
    public ITelemetrySummary getSessionSummary() {
        if (sessionManager == null) {
            return null;
        }
        return sessionManager.getSummary();
    }
    
    @Override
    public String getAppName() {
        return appName;
    }
    
    @Override
    public void clearInputStreamBuffer() throws IOException {
        // check for null before calling method
        if (sessionManager != null) {
            sessionManager.clearInputStreamBuffer();
        }
    }

    ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////
    /// Everything below this line is for command line parsing///
    ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////

    /**
     * Note that the log object is not initialized when running via unit tests,
     * as this is done via Spring in init()
     */
    @Override
    @SuppressWarnings({ "DM_EXIT" })
    public void configure(final ICommandLine commandLine) throws ParseException {

        super.configure(commandLine);

        // quiet option
        quiet = BaseCommandOptions.QUIET.parse(commandLine);

        // gui option
        if (commandLine.hasOption(BaseCommandOptions.GUI.getLongOpt())) {
            gui = true;
        }
        else if (commandLine.hasOption(BaseCommandOptions.NO_GUI.getLongOpt())) {
            gui = false;
        }
        autoRun = BaseCommandOptions.AUTORUN.parse(commandLine);

        bootstrapOpts.parseBootstrapOptions(commandLine);

        final boolean haveConfig = commandLine.hasOption(sessionOpts.SESSION_CONFIGURATION.getLongOpt());

        final IVenueConfiguration venueConfig = springContext.getBean(IVenueConfiguration.class);

        sessionOpts.parseAllDownlinkOptionsAsOptional(commandLine, true, autoRun);
        if (!haveConfig) {
            dictOpts.parseAllOptionsAsOptionalWithDefaults(commandLine);
        	contextConfig.getConnectionConfiguration().setDefaultNetworkValuesForVenue(venueConfig.getVenueType(), 
        			venueConfig.getTestbedName(), venueConfig.getDownlinkStreamId());
            connectionOpts.parseAllOptionsAsOptional(commandLine);
        }

        final String publishTopic = contextOpts.PUBLISH_TOPIC.parse(commandLine);

        // test config file
        if (haveConfig) {

            // clear fields when not in integrated gui mode
            if (!GdsSystemProperties.isIntegratedGui()) {
                contextConfig.clearFieldsForNewConfiguration();
            }

            final SessionConfiguration sessionConfig = (SessionConfiguration) contextConfig;

            if (sessionConfig.isUplinkOnly()) {
                throw new ParseException(SUPPLIED_SESSION +
                        "configuration file is for an uplink-only " +
                        "configuration and cannot be used by this downlink " +
                        "application");
            }
            else if (sessionConfig.isSseDownlinkOnly() && 
                    !contextConfig.getSseContextFlag()) {
                throw new ParseException(SUPPLIED_SESSION +
                        "configuration file is for an SSE downlink-only " +
                        "configuration and cannot be used by this FSW " +
                        "downlink application");
            }
            else if (sessionConfig.isFswDownlinkOnly() && 
                    contextConfig.getSseContextFlag()) {
                throw new ParseException(SUPPLIED_SESSION +
                        "configuration file is for a FSW downlink-only " +
                        "configuration and cannot be used by this SSE " +
                        "downlink application");
            }
        }
        

        meterInterval = waitOption.parseWithDefault(commandLine, false, true).longValue();
        if (meterInterval > 0) {
            downConfig.setMeterInterval(meterInterval);
        }

        jmsOptions.parseAllOptionsAsOptional(commandLine);
        downConfig.setUseMessageService(springContext.getBean(MessageServiceConfiguration.class).getUseMessaging());

        dbOptions.parseAllOptionsAsOptional(commandLine);
        downConfig.setUseDb(dbOptions.getDatabaseConfiguration().getUseDatabase());

        autoStart = autostartOption.parse(commandLine);

        if (autoRun) {
            if (venueConfig.getVenueType() == null || contextConfig.getContextId().getName() == null) {
                throw new ParseException("To use the " + BaseCommandOptions.AUTORUN.getLongOpt()
                        + " option you must either supply a session configuration file\n"
                        + "or a venue type and a session name");
            }
            if (contextConfig.getConnectionConfiguration().getDownlinkConnection().getInputType() == null) {
                contextConfig.getConnectionConfiguration().getDownlinkConnection()
                             .setInputType(springContext.getBean(ConnectionProperties.class)
                                                        .getDefaultSourceFormat(venueConfig.getVenueType(), false));
            }
        }

        checkDatabaseOptions();
   
        setContextConfiguration(contextConfig, !GdsSystemProperties.isIntegratedGui());

        final BufferedInputModeType bufferMode = bufferOption.parse(commandLine);

        if (bufferMode != null) {
            springContext.getBean(TelemetryInputProperties.class).setBufferedInputMode(bufferOption.parse(commandLine));
        }

        // If running quiet, only fatal messages will be displayed
        // to the console or GUI
        if (quiet) {
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.ERROR);
        }

        appConfig = perspectiveOpts.APPLICATION_CONFIGURATION.parse(commandLine);
        
        // Add GLAD parsing of host/port options
        gladClientOptions.SERVER_HOST_OPTION.parseWithDefault(commandLine, false, true);
        gladClientOptions.SOCKET_PORT_OPTION.parseWithDefault(commandLine, false, true);

    }


    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        if (options == null) {
            createOptions();
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.print("Usage: " + jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName());

        pw.println(" --sessionName <name> --venueType <venue-type>\n"
                + "[--inputFormat <format>] [--spacecraftID <scid>]\n"
                + "[--testbedName <testbedName> --downlinkStreamID <streamID>] [options]\n" + "[input-file|PVL-file]");

        options.getOptions().printOptions(pw);

        pw.println("\n<input-file> should be the name of an input file that will only\n"
                + "be used if the connection type is FILE or TDS. If the venue type is TESTBED\n"
                + "or ATLO, the testbed name is required. The stream ID is needed in the\n"
                + "TESTBED or ATLO to determine the input source and messaging topic\n"
                + "name. If any venue is configured to use a TDS connection type, the\n"
                + "<PVL-file> should be specified. The session host (--" + connectionOpts.DB_SOURCE_HOST.getLongOpt()
                + ") and\n" + "session key (--" + connectionOpts.DB_SOURCE_KEY.getLongOpt()
                + ") are required for DATABASE venues. ");
        pw.flush();
    }


    @Override
    public BaseCommandOptions createOptions() {

        if (options != null) {
            return options;
        }

        super.createOptions(springContext.getBean(BaseCommandOptions.class, this));

        sessionOpts = new SessionCommandOptions((SessionConfiguration) contextConfig);
        dictOpts = new DictionaryCommandOptions(springContext.getBean(DictionaryProperties.class));
        dbOptions = new DatabaseCommandOptions(springContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
        jmsOptions = new MessageServiceCommandOptions(springContext.getBean(MessageServiceConfiguration.class));
        connectionOpts = new ConnectionCommandOptions(contextConfig.getConnectionConfiguration());
        contextOpts = new ContextCommandOptions(contextConfig);
        bootstrapOpts = new ChannelLadBootstrapCommandOptions(springContext.getBean(ChannelLadBootstrapConfiguration.class));

        options.addOption(BaseCommandOptions.GUI);
        options.addOption(BaseCommandOptions.NO_GUI);
        options.addOption(BaseCommandOptions.AUTORUN);
        options.addOption(BaseCommandOptions.QUIET);

        waitOption = new UnsignedLongOption(WAIT_OPTION_SHORT, WAIT_OPTION_LONG, "interval",
                                            "input meter interval (milliseconds)", false);

        waitOption.setDefaultValue(UnsignedLong.MIN_VALUE);
        options.addOption(waitOption);

        autostartOption = new DownlinkAutostartOption();
        options.addOption(autostartOption);

        options.addOptions(jmsOptions.getAllOptions());

        options.addOptions(dbOptions.getAllLocationOptions());
        options.addOption(dbOptions.NO_DATABASE);

        // only support publish topic
        options.addOption(contextOpts.PUBLISH_TOPIC);
        
        gladClientOptions = new GladClientCommandOptions(GlobalLadProperties.getGlobalInstance());
        options.addOption(gladClientOptions.SERVER_HOST_OPTION);
        options.addOption(gladClientOptions.SOCKET_PORT_OPTION);

        options.addOptions(bootstrapOpts.getBootstrapCommandOptions());

        return options;
    }


    @Override
    public ISuspectChannelService getSuspectChannelService() {
        if (sessionManager != null) {
            return sessionManager.getSuspectChannelService();
        }
        else {
            return null;
        }
    }

    @Override
    public String getLadContentsAsString() {
        final StringWriter writer = new StringWriter();
        final boolean ok = this.channelLad.writeCsv(writer);
        try {
            writer.close();
        }
        catch (final IOException e) {
            // ignore
        }
        return ok ? writer.toString() : "";
    }


    @Override
    public boolean saveLadToFile(final String filename) {

        try {
            final FileWriter writer = new FileWriter(filename);
            final boolean status = this.channelLad.writeCsv(writer);
            writer.close();

            return status;

        }
        catch (final IOException e) {
            // already logged by the LAD
            return false;
        }

    }

    // R8 Refactor TODO - This was all being done by the channel LAD, but it
    // can no longer access either AlarmHistory or AlarmNotifierService. This may
    // result in strange behavior, because really clearing all of these needs to
    // be a synchronized set of actions. On the other hand, anyone clearing EHA
    // processing state while actively flowing data is a fool. Perhaps we should
    // just not worry about it.
    @Override
    public void clearChannelState() {
        this.channelLad.clearAll();
        this.alarmHistory.clearValues();
        final IAlarmNotifierService notifier = getAlarmNotifier();
        if (notifier != null) {
            notifier.clearCache();
        }
    }

    /**
     * Gets the AlarmNotifierService object from the downlink session manager object
     * 
     * @return AlarmNotifierService, or null if none has been initialized
     */
    private IAlarmNotifierService getAlarmNotifier() {
        if (sessionManager != null) {
            return sessionManager.getAlarmNotifier();
        }
        else {
            return null;
        }
    }

    /**
     * Check required options for database downlink connection type.
     *
     * @throws ParseException
     *             If not specified correctly
     */
    private void checkDatabaseOptions() throws ParseException {

        if (contextConfig.getConnectionConfiguration().getDownlinkConnection()
                         .getDownlinkConnectionType() != TelemetryConnectionType.DATABASE) {
            return;
        }

        // In this context the lists must be empty or singleton.

        final DatabaseConnectionKey dsi = ((IDatabaseConnectionSupport) contextConfig.getConnectionConfiguration()
                                                                                     .getDownlinkConnection()).getDatabaseConnectionKey();
        final List<String> hosts = dsi.getHostPatternList();
        final List<Long> keys = dsi.getSessionKeyList();

        if (hosts.size() > 1) {
            throw new ParseException("For a " + TelemetryConnectionType.DATABASE + " downlink connection there can be "
                    + "but one session host");
        }

        if (keys.size() > 1) {
            throw new ParseException("For a " + TelemetryConnectionType.DATABASE + " downlink connection there can be "
                    + "but one session key");
        }

        final String local = HostPortUtility.getLocalHostName();
        boolean noHost = hosts.isEmpty();
        boolean noKey = keys.isEmpty();
        String currentHost = (!noHost ? StringUtil.emptyAsNull(hosts.get(0)) : null);
        Long currentKey = (!noKey ? keys.get(0) : null);

        // If host is null or empty, or if key is less than 1, ignore them

        noHost = noHost || (currentHost == null);
        noKey = noKey || (currentKey == null) || (currentKey.longValue() <= 0L);

        if (!gui) {
            if (noHost && noKey) {
                throw new ParseException(YOU_MUST_SPECIFY + connectionOpts.DB_SOURCE_HOST.getLongOpt() + " and --"
                        + connectionOpts.DB_SOURCE_KEY.getLongOpt() + " options for a "
                        + TelemetryConnectionType.DATABASE + DOWNLINK_CONNECTION);
            }

            if (noHost) {
                throw new ParseException(YOU_MUST_SPECIFY + connectionOpts.DB_SOURCE_HOST.getLongOpt()
                        + " option for a " + TelemetryConnectionType.DATABASE + DOWNLINK_CONNECTION);
            }

            if (noKey) {
                throw new ParseException(YOU_MUST_SPECIFY + connectionOpts.DB_SOURCE_KEY.getLongOpt() + " option for a "
                        + TelemetryConnectionType.DATABASE + DOWNLINK_CONNECTION);
            }
        }
        else {
            // Set some reasonable defaults just to have something.

            if (noHost) {
                currentHost = local;
            }

            if (noKey) {
                currentKey = 1L;
            }
        }

        // At this point we have a host and a key.

        // Do not allow "localhost", rather convert it to the real name.

        if (HostPortUtility.LOCALHOST.equalsIgnoreCase(currentHost)) {
            currentHost = local;
        }

        dsi.setHostPattern(currentHost.toLowerCase());
        dsi.setSessionKey(currentKey);
    }

    @Override
    public AbstractDownShell getDownlinkShell() {
        if (!useGui()) {
            throw new IllegalStateException("Cannot retrieve Downlink GUI Shell. GUI is desabled.");
        }
        return guiShell;
    }

    @Override
    public SessionConfigShell getConfigShell() {
        if (!useGui()) {
            throw new IllegalStateException("Cannot retrieve Downlink GUI Shell. GUI is desabled.");
        }
        return configShell;
    }

    @Override
    public ApplicationContext getAppContext() {
        return springContext;
    }

    /*
     * Must force initialization of DownlinkConfiguration. This is a test-ism.
     * In production, the configuration is loaded as a bean by the Spring context. The setDownConfig()
     * method is package protected, and can only be called from a test in the same package.
     */
    /* package */ void setDownConfig(final DownConfiguration downConfig) {
        this.downConfig = downConfig;
    }
}
