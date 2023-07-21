/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.telem.common.worker;

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.EnableFswDownlinkContextFlag;
import jpl.gds.context.api.EnableSseDownlinkContextFlag;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.*;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.performance.HealthStatus;
import jpl.gds.shared.performance.HeapPerformanceData;
import jpl.gds.shared.performance.PerformanceSummaryMessage;
import jpl.gds.shared.performance.ProviderPerformanceSummary;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.common.app.mc.rest.resources.DownlinkStatusResource;
import jpl.gds.telem.common.manager.ISessionManager;
import jpl.gds.telem.common.state.WorkerState;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract class for common code and operations for an ITelemetryWorker
 */
public abstract class AbstractTelemetryWorker implements ITelemetryWorker {
    /** Current application context */
    protected final ApplicationContext springContext;
    /** Context configuration for the downlink session */
    protected IContextConfiguration contextConfig;
    /** Tracer to log with */
    protected Tracer tracer;
    /** Venue configuration */
    protected final IVenueConfiguration venueCfg;
    /** database properties */
    protected final IDatabaseProperties databaseProps;
    /** Secure class loader */
    protected AmpcsUriPluginClassLoader secureLoader;
    /** connection properties */
    protected ConnectionProperties connProps;
    /** Message service configuration */
    protected MessageServiceConfiguration msgServiceConfig;
    /** SSE Context flag*/
    protected SseContextFlag sseFlag;

    /** Options object to use */
    protected BaseCommandOptions options;

    /** dictionary command options */
    protected     DictionaryCommandOptions dictOpts;
    protected final DictionaryProperties     dictProps;

    private ContextCommandOptions contextOpts;

    /** Message publication bus */
    protected  IMessagePublicationBus publicationBus;
    /** JMS message service */
    protected IMessagePortal jmsPortal;

    /** Exit status */
    protected final AtomicBoolean exiting = new AtomicBoolean(false);

    /** Whether or not an ITelemetryWorker has been started */
    protected boolean hasBeenStarted = false;

    /** The current ITelemetryWorker WorkerState */
    protected WorkerState currentWorkerState = WorkerState.INITIALIZING;

    /** Status Message Factory */
    private final IStatusMessageFactory msgFactory;

    /** Session Manager */
    protected ISessionManager sessionManager;

    /** Original thread name */
    protected String originalThreadName;

    /** Worker thread name prefix set by the child classes */
    protected String workerThreadNamePrefix;


    protected final EnableFswDownlinkContextFlag fswDownFlag;
    protected final EnableSseDownlinkContextFlag sseDownFlag;




    /**
     * Creates an instance of TelemetryTelemetryProcessWorker.
     *
     * @param springContext
     *            the Spring Application Context
     * @param sessionConfig
     *            The Session Configuration to use
     * @param secureLoader The parent servers secure classloader
     */
    protected AbstractTelemetryWorker(final ApplicationContext springContext,
                                      final SessionConfiguration sessionConfig,
                                      final AmpcsUriPluginClassLoader secureLoader) {
        this.springContext = springContext;
        this.contextConfig = sessionConfig;

        this.msgFactory = springContext.getBean(IStatusMessageFactory.class);
        this.secureLoader = secureLoader;

        // TI/TP: M20 Channels Output missing ground derived channels
        // The SECURE_CLASS_LOADER that gets passed in above is the parent class loader.
        // That change was made to resolve the "Access Denied" Errors
        // when trying to process derived channels.
        //
        // Fix for above is to override the SECURE_CLASS_LOADER within the Worker's Spring Context
        // with the SECURE_CLASS_LOADER of the parent (Server). This is necessary because the DerivationMap
        // class retrieves the SECURE_CLASS_LOADER from the Worker's Spring Context.
        // Note: with this implementation we are essentially using a single instance of the
        // SECURE_CLASS_LOADER for all workers. There could be an issue where multiple TP workers
        // running on different sessions using different dictionary versions share the same SECURE_CLASS_LOADER
        // caching implementation.
        final ConfigurableApplicationContext configContext = (ConfigurableApplicationContext)springContext;
        final ConfigurableBeanFactory configurableBeanFactory = configContext.getBeanFactory();
        final BeanDefinitionRegistry beanDefinitionRegistry =
                (BeanDefinitionRegistry) springContext.getAutowireCapableBeanFactory();
        // Check to make sure Bean Definition exists before removing
        if (beanDefinitionRegistry.containsBeanDefinition(CommonSpringBootstrap.SECURE_CLASS_LOADER)) {
            beanDefinitionRegistry.removeBeanDefinition(CommonSpringBootstrap.SECURE_CLASS_LOADER);
        }
        configurableBeanFactory.registerSingleton(CommonSpringBootstrap.SECURE_CLASS_LOADER, secureLoader);
        this.msgServiceConfig = springContext.getBean(MessageServiceConfiguration.class);

        this.venueCfg = contextConfig.getVenueConfiguration();
        this.connProps = contextConfig.getConnectionConfiguration().getConnectionProperties();
        this.databaseProps = springContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES,
                                                   IDatabaseProperties.class);

        this.dictProps = sessionConfig.getDictionaryConfig();

        this.fswDownFlag = springContext.getBean(EnableFswDownlinkContextFlag.class);
        this.sseDownFlag = springContext.getBean(EnableSseDownlinkContextFlag.class);

        originalThreadName = Thread.currentThread().getName();
    }

    /**
     * Initializes the application prior to a session run. The messaging interfaces must be started first, because the
     * remaining startup will publish messages that need to be published externally.
     */
    private void initializeApplication() {

        /*
         * start the messaging
         * Now won't cause another process worker to be spawned.
         * NEEDED for recorded engineering product handler.
         */
        startMessagingInterfaces();

        createOutputDirectory();
    }

    /**
     * Starts up the messaging components and connections.
     */
    private void startMessagingInterfaces() {
        // Start the portal to the external JMS bus
        if (jmsPortal == null) {
            jmsPortal = springContext.getBean(IMessagePortal.class);
        }
        jmsPortal.startService();
    }

    /**
     * Internal method to handle worker 'setup'
     */
    protected void setupWorker() {
        // start messaging, etc
        initializeApplication();
        setContextConfiguration(contextConfig);

        contextConfig.getDictionaryConfig().loadDictionaryJarFiles(false, secureLoader, tracer);
    }

    @Override
    public void abort() {
        if (currentWorkerState == WorkerState.ABORTING) {
            throw new IllegalStateException("Telemetry worker is already " + WorkerState.ABORTING);
        }
        if (currentWorkerState == WorkerState.STOPPED) {
            throw new IllegalStateException("Telemetry worker is already " + WorkerState.STOPPED);
        }
        internalChangeWorkerState(WorkerState.ABORTING);
        exitCleanly();
    }

    @Override
    public void release() throws IllegalStateException {
        tracer.info(this, " is now released");
        ((GenericApplicationContext) getAppContext()).close();
    }

    @Override
    public boolean hasBeenStarted() {
        return hasBeenStarted;
    }


    @Override
    public IContextConfiguration getContextConfiguration() {
        return contextConfig;
    }


    /**
     * Sets the context configuration and resets the instance fields if the input flag is set. The latter will force a
     * new context configuration to be created in the database during execution.
     *
     * @param config            the IContextConfiguration to set
     */
    private void setContextConfiguration(final IContextConfiguration config) {
        contextConfig = config;
    }


    @Override
    public WorkerState getState() {
        return currentWorkerState;
    }



    @Override
    public ApplicationContext getAppContext() {
        return springContext;
    }


    @Override
    public void run() {
        internalStartupWorker();

        boolean ok = false;
        if (!(currentWorkerState == WorkerState.STOPPING || currentWorkerState == WorkerState.ABORTING || currentWorkerState == WorkerState.STOPPED)) {
            // ok
            ok = internalRun();
        }

        Thread.currentThread().setName(originalThreadName);
        internalStopWorker(ok);

        exitCleanly();
    }

    /**
     * This method start the input processing and should be called after the
     * worker has been properly initialized and is prepared to handle the data.
     *
     * @return true if input processing is successfully started and terminates
     * without error, false if there is an exception processing
     * telemetry
     */
    protected boolean internalRun() {
        internalChangeWorkerState(WorkerState.ACTIVE);
        hasBeenStarted = true;

        boolean inputOk = false;

        tracer.debug(this, " has started internalRun");

        try {
            inputOk = sessionManager.processInput();
        } catch (Exception e) {
            tracer.error("Unexpected ", e.getClass().getName(), " ", ExceptionTools.getMessage(e));
            tracer.error(Markers.SUPPRESS,"RawInputException= ", ExceptionTools.getMessage(e), e);

            internalChangeWorkerState(WorkerState.STOPPING);
        }
        if (!inputOk) {
            tracer.error("There was an unexpected problem processing the input data or executing the session. The worker has been killed.");
        }

        return inputOk;
    }


    /**
     * Places the worker in the transient STOPPING state until the worker has reached the
     * steady STOPPED state. Goes through the proper shutdown sequence by stopping
     * the Session Manager. If the showSummary parameter is set to 'true' the session
     * summary will be output to the terminal.
     *
     * @param showSummary true if the session summary should be shown, false otherwise
     * @throws IllegalStateException when an invalid state transition occurs
     */
    protected void internalStopWorker(boolean showSummary) throws IllegalStateException {
        if (currentWorkerState == WorkerState.STOPPED || currentWorkerState == WorkerState.STOPPING || currentWorkerState == WorkerState.ABORTING) {
            return;
        }
        internalChangeWorkerState(WorkerState.STOPPING);
        tracer.debug(this, " has started internalStop");

        sessionManager.stop();

        // End/cleanup the downlink session
        if (!sessionManager.isSessionEnded()) {
            sessionManager.endSession(showSummary);
        }
    }

    /**
     * Stops the messaging components and connections.
     */
    private void stopMessagingInterfaces() {
        tracer.debug("Shutting down ", this , " message interface");

        if (jmsPortal != null) {
            jmsPortal.stopService();
            jmsPortal = null;
        }

        if (publicationBus != null) {
            publicationBus.unsubscribeAll();
        }
    }

    /**
     * Places the worker in the transient STARTING state until the worker has reached the
     * steady ACTIVE state. Starts the Session Manager and sends the "process running"
     * message.
     */
    protected void internalStartupWorker() {
        internalChangeWorkerState(WorkerState.STARTING);
        final Thread processingThread = Thread.currentThread();

        this.originalThreadName = processingThread.getName();
        processingThread.setName(workerThreadNamePrefix + Thread.currentThread().getId());

        // Send start message and start heartbeat message publication
        tracer.trace(ApplicationConfiguration.getApplicationName(), " starting session");

        try {
            sessionManager.startSession();
        } catch (ApplicationException e) {
            tracer.error("The session could not be started, see log file for more details. " ,
                         ExceptionTools.getMessage(e));
            tracer.error(Markers.SUPPRESS, "The session could not be started: " + ExceptionTools.getMessage(e), e);

            internalChangeWorkerState(WorkerState.STOPPING);
        }

        // Notify the user that the process is starting
        sendRunningMessage();
    }

    /**
     * Publishes a "process running" message.
     */
    private void sendRunningMessage() {
        final IPublishableLogMessage rm = msgFactory.createRunningMessage(this.toString());

        if (publicationBus != null) {
            publicationBus.publish(rm);
        }
        tracer.log(rm);
    }

    /**
     * Creates the session output directory.
     */
    private void createOutputDirectory() {
        // create the output directory if it doesn't exist
        // 5/23/13 - Fixed PMD nested IF violation and
        // raw exception violation
        final File testDirFile = new File(this.contextConfig.getGeneralInfo().getOutputDir());
        if (!testDirFile.exists() && !testDirFile.mkdirs()) {
            tracer.error("Unable to create session output directory: " + this.contextConfig.getGeneralInfo().getOutputDir());
            throw new IllegalStateException("Downlink session cannot be started");
        }
        tracer.debug(this, " creating session output directory ", this.contextConfig.getGeneralInfo().getOutputDir());
    }


    @Override
    public void stop() {
        if (currentWorkerState == WorkerState.STOPPING) {
            return;
        }
        if (currentWorkerState != WorkerState.ACTIVE) {
            throw new IllegalStateException("Telemetry worker is NOT " + WorkerState.ACTIVE);
        }

        internalStopWorker(true);

        exitCleanly();
    }

    /**
     * Internal method for logging and updating telemetry worker states
     *
     * @param newState The state to change to
     */
    protected synchronized void internalChangeWorkerState(WorkerState newState) {
        if (currentWorkerState == newState) {
            return; // no-op
        }
        tracer.info(this, " is now in ", newState.toString(), " state");

        currentWorkerState = newState;
    }

    @Override
    public String toString() {
        return StringUtils.substringBefore(getClass().getSimpleName(), "Worker") +
                " [" + currentWorkerState.toString() + "]: {"
                + contextConfig.getContextId().getContextKey().getContextId()
                + "}";
    }

    @Override
    public void updateContextDbSessionsList(final String sessionIds) throws ApplicationException {
        sessionManager.updateContextDbSessionsList(sessionIds);
    }

    @Override
    public Tracer getTracer() {
        return tracer;
    }

    @Override
    public BaseCommandOptions createOptions() {
        if (options != null) {
            return options;
        }
        options = new BaseCommandOptions(this, true);

        dictOpts = new DictionaryCommandOptions(dictProps);
        options.addOptions(dictOpts.getAllOptions());


        contextOpts = new ContextCommandOptions(contextConfig);
        options.addOption(contextOpts.PUBLISH_TOPIC);

        return options;
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        dictOpts.parseAllOptionsAsOptionalWithDefaults(commandLine);
        contextOpts.PUBLISH_TOPIC.parse(commandLine);
    }

    @Override
    public void exitCleanly() {
        if (currentWorkerState == WorkerState.STOPPED) {
            return;
        }

        boolean inAbort = currentWorkerState == WorkerState.ABORTING;
        //  Fix shutdown issue when aborting a worker that is in STOPPING
        if (exiting.getAndSet(true)) {
            return;
        }

        if (!inAbort) {
            internalChangeWorkerState(WorkerState.STOPPING);
        }
        // Global Tracer can be null if spring did not initialize correctly
        final Tracer quitTracer = TraceManager.getTracer(springContext, Loggers.UTIL);

        quitTracer.info(this, " received shutdown request");


        /*
         * Added unconditional call to
         * sessionManager.stop() to free up a possibly retrying socket connection.
         */
        if (sessionManager != null) {
            quitTracer.debug("Stopping ", this, " session manager");
            sessionManager.stop();
        }

        if (!inAbort) {
            if (sessionManager != null) {
                quitTracer.debug("Not in abort mode, session manager=", sessionManager, ", session ended=",
                                 sessionManager.isSessionEnded());
                if (!sessionManager.isSessionEnded()) {
                    quitTracer.info("Stopping ", this, " session ");
                    sessionManager.endSession(true);
                }
            } else {
                quitTracer.debug("Not in abort mode, Session manager is null");
            }
            quitTracer.debug("Shutdown application");
            stopMessagingInterfaces();
        } else {
            quitTracer.warn(this, " has been aborted! Unable to safely shut down...");
            if (sessionManager != null) {
                sessionManager.stopPerformancePublisher();
            }
        }

        internalChangeWorkerState(WorkerState.STOPPED);

        /*
         * Change TI/TP worker start procedures
         *
         * Closing the application context here causes problems when we attempt to
         * retrieve the worker statistics/summary after it has been stopped. Its still
         * best to close the context when a worker is stopped for memory management
         * purposes but we need to fix the summary/telemetry retrieval logic first.
         *
         */

    }

    /**
     * Handles a performance summary message.
     *
     * @param msg
     *            the message to handle
     *
     * @param perfProps
     *            Performance properties
     * @param downStatus
     *            Downlink status resource (heap)
     */
    protected void handlePerformanceSumMessage(final PerformanceSummaryMessage msg,
                                               final PerformanceProperties perfProps,
                                               final DownlinkStatusResource downStatus) {
        /*
         * If we have no message (at startup) then
         * set health status indicator based upon heap status only.
         */
        HealthStatus status;
        if (msg == null) {
            status = new HeapPerformanceData(perfProps).getHealthStatus();
        }
        else {
            status = msg.getOverallHealth();
        }
        downStatus.setHeapHealth(status);
        if (msg != null) {
            HeapPerformanceData heapPerfData = msg.getHeapStatus();
            if (heapPerfData == null) {
                heapPerfData = new HeapPerformanceData(perfProps);
            }
            downStatus.setHeapPerfData(heapPerfData);
            final Map<String, ProviderPerformanceSummary> provMap = msg.getPerformanceData();
            for (final ProviderPerformanceSummary sum : provMap.values()) {
                downStatus.setPerformanceData(sum);
            }
        }
    }
}
