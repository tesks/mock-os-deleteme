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
package jpl.gds.telem.common.app.mc;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.bootstrap.ChannelLadBootstrapConfiguration;
import jpl.gds.common.config.bootstrap.options.ChannelLadBootstrapCommandOptions;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.gdsdb.DatabaseProperties;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.common.websocket.IWebSocketConnectionManager;
import jpl.gds.context.api.*;
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.context.api.message.util.IContextMessageFactory;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.context.api.options.RestCommandOptions;
import jpl.gds.context.cli.app.mc.AbstractRestServerApp;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.order.ISessionOrderByType;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.types.IDbSessionFactory;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.options.GladClientCommandOptions;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.*;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.metadata.context.ContextKey;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.ITelemetryServer;
import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.common.app.DownlinkErrorManager;
import jpl.gds.shared.exceptions.RestfulTelemetryException;
import jpl.gds.telem.common.app.mc.rest.resources.WorkerId;
import jpl.gds.telem.common.state.WorkerState;
import jpl.gds.telem.common.worker.WorkerUtils;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract class for persistent telemetry restful servers (e.g Telemetry Ingestor, Telemetry Processor)
 *
 *
 */
public abstract class AbstractTelemetryServerApp extends AbstractRestServerApp
        implements ITelemetryServer {

    /** Maximum amount of time to wait for worker to become Active in seconds */
    private static final int MAX_WAIT_TIME_TO_BECOME_ACTIVE_IN_SECONDS = 30;

    private static final String INGEST_APP_NAME = "chill_telem_ingest";

    /**
     * Session Map for <ITelemetryWorker>
     * Contains both active and finished sessions; when release is called, a worker is removed from the map
     * Do NOT use IContextKey object (multable) as keys as they may change
     * (e.g. Telemetry processor will change fragment to 2)
     */
    private final Map<String, ITelemetryWorker>  workerMap = new ConcurrentHashMap<>();

    /** Session Map for <ITelemetrySummary> */
    private final Map<String, ITelemetrySummary> summaryMap = new ConcurrentHashMap<>();

    /** Exiting Atomic Boolean */
    protected volatile AtomicBoolean exit = new AtomicBoolean(false);

    private AtomicBoolean springInitialized = new AtomicBoolean(false);

    /** BaseCommandOptions object */
    protected final BaseCommandOptions baseCommandOptions;

    /** GlobalLAD client command line options object */
    private GladClientCommandOptions gladClientOptions;

    /** Message Service command line options object */
    private MessageServiceCommandOptions jmsOptions;

    /** Database command line options object */
    protected DatabaseCommandOptions dbOptions;

    /** Context command line options object */
    protected ContextCommandOptions contextOpts;

    /** GLAD bootstrap command line options object */
    private ChannelLadBootstrapCommandOptions bootstrapOpts;

    /** Tracer */
    protected Tracer tracer;

    /** Thread pool task executor */
    protected ThreadPoolTaskExecutor executor;

    /** Timer */
    private Timer heartbeatTimer;

    /** Simple context */
    private ISimpleContextConfiguration simpleContext;

    /** session used for command line parsing etc; not persisted */
    protected SessionConfiguration tempSession;

    /** Message Portal */
    private IMessagePortal jmsPortal;

    /** Command-line arguments */
    @Autowired
    protected ApplicationArguments arguments;

    /** ICommandLine object */
    protected ICommandLine commandLine;

    /** DB Archive Controller */
    @Autowired
    protected IDbSqlArchiveController archiveController;

    /** Message bus */
    @Autowired
    protected IMessagePublicationBus publicationBus;

    /** Spring app context */
    protected ApplicationContext parentContext;

    /** Service configuration */
    @Autowired
    protected ServiceConfiguration serviceConfiguration;

    /** Message Service Configuration */
    @Autowired
    protected MessageServiceConfiguration messageSvcConfig;

    /** GLAD bootstrap configuration object */
    @Autowired
    protected ChannelLadBootstrapConfiguration bootstrapConfig;

    /** Database Properties */
    @Autowired
    @Qualifier(CommonSpringBootstrap.DATABASE_PROPERTIES)
    protected DatabaseProperties databaseProps;

    /** General Properties */
    @Autowired
    protected GeneralProperties generalProperties;

    /** Parent Dictionary properties */
    @Autowired
    protected DictionaryProperties parentDictionaryProperties;

    /** The SSE context flag */
    @Autowired
    protected SseContextFlag sseContextFlag;

    /** The IVenueConfiguration */
    @Autowired
    protected IVenueConfiguration venueConfig;

    /** Factory for creating a context configuration */
    @Autowired
    protected IContextConfigurationFactory contextFactory;

    /** Context message utility */
    @Autowired
    private IContextMessageFactory contextMessageUtil;

    /** Web Socket Connection Manager */
    private IWebSocketConnectionManager connectionManager;

    /** Secure classloader to use */
    protected AmpcsUriPluginClassLoader secureLoader;

    private static final String NO_WORKER = "Process '%s' does not exist! Unable to %s";

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     * 
     * @param appContext
     *            The current spring application context
     * @param log
     *            Tracer logger
     * @param taskExecutor
     *            The Task executor
     * 
     */
    public AbstractTelemetryServerApp(final ApplicationContext appContext, final Tracer log,
            final ThreadPoolTaskExecutor taskExecutor) {
        super();
        this.parentContext = appContext;
        this.tracer = log;

        this.baseCommandOptions = parentContext.getBean(BaseCommandOptions.class, this);
        this.secureLoader = parentContext.getBean(AmpcsUriPluginClassLoader.class);
        this.executor = taskExecutor;

    }

    /**
     * Abstract method to create a server context configuration
     * Sub-classes will provide an implementation
     *
     * @return ISimpleContextConfiguration
     */
    public abstract ISimpleContextConfiguration createServerContext();

    /**
     * Common initialization for an <ITelemetryServer>
     */
    protected void commonInit() {
        tempSession = new SessionConfiguration(parentContext, false);
        simpleContext = createServerContext();

        createOptions();

        try {
            this.commandLine = options.parseCommandLine(arguments.getSourceArgs(), true);
            // also sets rest port
            configure(commandLine);
        }
        catch (final ParseException e) {
            tracer.error(ExceptionTools.getMessage(e));
            return;
        }

        if (!startNeededStores()) {
            return;
        }

        // need to init simple context after rest port has been set, but before JMS starts
        saveContext(this.simpleContext);

        startJmSPortal();

        startHeartbeatTimer();

        // Add call to load dictionary jars
        tempSession.getDictionaryConfig().loadDictionaryJarFiles(false, secureLoader, tracer);

        tracer.info(this, " has been started");
        springInitialized.getAndSet(true);
    }

    @Override
    public void exitCleanly() {
        if (helpDisplayed.get() || versionDisplayed.get()) {
            return;
        }

        if (exit.getAndSet(true)) {
            return;
        }

        //  Don't run shutdown hook stuff if the application did not start OK
        if (!springInitialized.get()) {
            return;
        }

        tracer.info(this, " has received a shutdown request...");

        //stop workers if not already stopped
        List<ITelemetryWorker> allWorkers = getAllWorkers();
        tracer.log(TraceSeverity.INFO,
                   allWorkers.isEmpty() ?
                           "No processes to clean up" :
                           ("Cleaning up " + allWorkers.size() + " process(s)"));
        for(final ITelemetryWorker worker : allWorkers){
            worker.exitCleanly();
        }

        this.executor.shutdown();

        stopHeartbeatTimer();

        stopJmsPortal();

        if (archiveController != null) {
            tracer.info(ApplicationConfiguration.getApplicationName(), " database is shutting down...");
            archiveController.shutDown();
            archiveController = null;
        }

        tracer.info(this, " has finished shutting down all processes and services...");

        // Closing spring context should be last
        final ConfigurableApplicationContext context = (ConfigurableApplicationContext) parentContext;
        if (context.isActive()) {
            context.close();
        }
    }

    /**
     * Start database stores
     * 
     * @return whether or not the needed stores were started
     */
    private synchronized boolean startNeededStores() {
        if (!parentContext.getBean(IMySqlAdaptationProperties.class).getUseDatabase()) {
            tracer.debug(getClass().getName(), "Skipping database startup");
            return true;
        }

        try {
            archiveController.addNeededStore(StoreIdentifier.LogMessage);

            final boolean ok = archiveController.startContextConfigStore();
            if (!ok) {
                throw new DatabaseException();
            }
            archiveController.startPeripheralStores();
            return true;
        }
        catch (final Exception e) {
            tracer.error("Error starting database stores " + ExceptionTools.getMessage(e), e.getCause());
            errorCode = DownlinkErrorManager.getSessionErrorCode(e);
            return false;
        }
    }

    /**
     * start heartbeat timer
     * 
     */
    private void startHeartbeatTimer() {
        final String app = ApplicationConfiguration.getApplicationName();
        tracer.trace("Starting " , app , " Server context heartbeat timer.");
        heartbeatTimer = new Timer("Telemetry " + app + " Heartbeat Timer");

        final long interval = generalProperties.getContextHeartbeatInterval();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    final IContextHeartbeatMessage heartbeat =
                            contextMessageUtil.createContextHeartbeatMessage(simpleContext);
                    heartbeat.setServiceConfiguration(serviceConfiguration);
                    tracer.trace(heartbeat, ": (", simpleContext.getContextId().getName(), ")");
                    publicationBus.publish(heartbeat);
                }
                catch (final Exception e) {
                    tracer.error("Unknown exception ", ExceptionTools.getMessage(e), e);
                }
            }
        }, interval, interval);
    }

    /** Stop heartbeat timer */
    private void stopHeartbeatTimer() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    /**
     * Starts up the messaging components and connections.
     */
    private void startJmSPortal() {
        // Start the portal to the external JMS bus
        if (jmsPortal == null) {
            jmsPortal = parentContext.getBean(IMessagePortal.class);
            jmsPortal.startService();
        }
    }

    /**
     * Insert context configuration into DB
     * 
     * @param ctx
     *            The context to insert into the DB
     */
    private void saveContext(final ISimpleContextConfiguration ctx) {
        //save to database
        try {
            archiveController.getContextConfigStore().insertContext(ctx);
            tracer.info(Markers.CONTEXT, "Created context ID " + ctx.getContextId().getNumber(),
                        " at time ", ctx.getContextId().getStartTimeStr());
        } catch (final DatabaseException e) {
            tracer.error("Error saving context: " + e.toString());
        }
    }

    @Override
    public void startWorker(final long key, final String host, final int fragment) {
        ITelemetryWorker worker = workerMap.get(createWorkerKey(key, host, fragment));
        tracer.info(this, " attempting to start ", worker);

        executor.execute(worker);
        // Give worker up to 30 seconds to become ACTIVE
        try {
            WorkerUtils.pollWorker(worker, WorkerState.ACTIVE, MAX_WAIT_TIME_TO_BECOME_ACTIVE_IN_SECONDS);
        } catch (ExcessiveInterruptException e) {
            throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Encountered exception while waiting for Worker to start with id: "
                    + worker.getContextConfiguration().getContextId().getContextKey()
                            + " :" + ExceptionTools.getMessage(e));
        }

        if (worker.getState() != WorkerState.ACTIVE) {
            worker.abort();
            throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to start process with id: "
                    + worker.getContextConfiguration().getContextId().getContextKey());
        }
    }


    /**
     * Stops the messaging components and connections.
     */
    private void stopJmsPortal() {
        if (jmsPortal != null) {
            jmsPortal.stopService();
            jmsPortal = null;
        }

        if (publicationBus != null) {
            publicationBus.unsubscribeAll();
        }
    }

    @Override
    public void stopWorker(final long key, final String host, final int fragment) {
        String workerKey = createWorkerKey(key, host, fragment);
        if (workerMap.containsKey(workerKey)) {
            final ITelemetryWorker worker = workerMap.get(workerKey);
            worker.stop();
            summaryMap.put(workerKey, worker.getSessionSummary());
        } else {
            tracer.info(String.format(NO_WORKER, workerKey, "stop"));
        }
    }

    @Override
    public void releaseWorker(final long key, final String host, final int fragment) {
        String workerKey = createWorkerKey(key, host, fragment);
        if (workerMap.containsKey(workerKey)) {
            final ITelemetryWorker worker = workerMap.get(workerKey);
            worker.release();
            summaryMap.remove(workerKey);
            workerMap.remove(workerKey);
        } else {
            tracer.info(String.format(NO_WORKER, workerKey, "release"));
        }
    }

    @Override
    public void abortWorker(final long key, final String host, final int fragment) {
        String workerKey = createWorkerKey(key, host, fragment);
        if (workerMap.containsKey(workerKey)) {
            final ITelemetryWorker worker = workerMap.get(workerKey);
            worker.abort();
            summaryMap.put(workerKey, worker.getSessionSummary());
        } else {
            tracer.info(String.format(NO_WORKER, workerKey, "abort"));
        }
    }


    @Override
    public WorkerState getWorkerState(final long key, final String host, final int fragment) {
        String workerKey = createWorkerKey(key, host, fragment);
        final ITelemetryWorker worker = workerMap.get(workerKey);
        if (worker != null) {
            return worker.getState();
        } else {
            tracer.debug(String.format(NO_WORKER, workerKey, "get state"));
        }
        return null;

    }

    @Override
    public ITelemetryWorker getWorker(final long key, final String host, final int fragment) {
        String workerKey = createWorkerKey(key, host, fragment);
        if (workerMap.containsKey(workerKey)) {
            return workerMap.get(workerKey);
        } else {
            tracer.debug(String.format(NO_WORKER, workerKey, "get worker"));
        }
        return null;
    }

    @Override
    public List<WorkerId> getWorkers() {
        List<WorkerId> workerIdList = new ArrayList<>();
        for(final String key : workerMap.keySet()){
            ITelemetryWorker worker = workerMap.get(key);
            workerIdList.add(new WorkerId(worker.getContextConfiguration().getContextId().getContextKey()));
        }
        return workerIdList;
    }

    @Override
    public List<ITelemetryWorker> getAllWorkers() {
        List<ITelemetryWorker> workers = new ArrayList<>();
        for (Map.Entry<String, ITelemetryWorker> entry : workerMap.entrySet()){
            workers.add(entry.getValue());
        }
        return Collections.unmodifiableList(workers);
    }

    @Override
    public ITelemetrySummary getSessionSummary(final long key, final String host, final int fragment) {
        return summaryMap.get(createWorkerKey(key, host, fragment));
    }

    @Override
    public boolean hasBeenStarted(final long key, final String host, final int fragment) {
        final ITelemetryWorker worker = getWorker(key, host, fragment);
        return worker != null && worker.hasBeenStarted();
    }


    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        jmsOptions.parseAllOptionsAsOptional(commandLine);

        bootstrapOpts.parseBootstrapOptions(commandLine);

        dbOptions.parseAllOptionsAsOptional(commandLine);

        gladClientOptions.SERVER_HOST_OPTION.parseWithDefault(commandLine, false, true);
        gladClientOptions.SOCKET_PORT_OPTION.parseWithDefault(commandLine, false, true);

        if (sseContextFlag.isApplicationSse()) {
            parentContext.getBean(EnableFswDownlinkContextFlag.class).setFswDownlinkEnabled(false);
            parentContext.getBean(EnableSseDownlinkContextFlag.class).setSseDownlinkEnabled(true);
        } else {
            parentContext.getBean(EnableSseDownlinkContextFlag.class).setSseDownlinkEnabled(false);
            parentContext.getBean(EnableFswDownlinkContextFlag.class).setFswDownlinkEnabled(true);
        }
    }

    @Override
    public BaseCommandOptions createOptions(final BaseCommandOptions opts) {
        if (optionsCreated.get()) {
            return options;
        }
        // use special context aware rest options
        restOptions = new RestCommandOptions(simpleContext);

        options = super.createOptions(opts);

        
        contextOpts = new ContextCommandOptions(simpleContext);
        // only support SSE
        options.addOption(contextOpts.SSE_OPTION);

        bootstrapOpts = new ChannelLadBootstrapCommandOptions(bootstrapConfig);
        options.addOptions(bootstrapOpts.getBootstrapCommandOptions());

        dbOptions = new DatabaseCommandOptions(databaseProps);
        options.addOptions(dbOptions.getAllLocationOptions());
        options.addOption(dbOptions.NO_DATABASE);

        jmsOptions = new MessageServiceCommandOptions(messageSvcConfig);
        options.addOptions(jmsOptions.getAllOptionsWithoutNoJms());

        gladClientOptions = new GladClientCommandOptions(GlobalLadProperties.getGlobalInstance());
        options.addOption(gladClientOptions.SERVER_HOST_OPTION);
        options.addOption(gladClientOptions.SOCKET_PORT_OPTION);


        return options;
    }


    @Override
    public ApplicationContext getAppContext() {
        return parentContext;
    }

    @Override
    public ISimpleContextConfiguration getContextConfiguration() {
        return simpleContext;
    }

    /**
     * Create worker key suitable to be used a map key
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return Worker key as String
     */
    private String createWorkerKey(final long key, final String host, final int fragment){
        IContextKey contextKey = new ContextKey();
        contextKey.setNumber(key);
        contextKey.setHost(host);
        contextKey.setFragment(fragment);
        return contextKey.toString();
    }


    /**
     * Get a SessionConfiguration object based on an existing session in the database identified by the
     * specified session key and host.
     *
     * Note: Most of this code was taken from AutoManager and should ideally be placed in a Util class
     * which can be shared. Will require some refactoring work.
     *
     * @param key The Session key
     * @param host The Session host
     * @return SessionConfiguration
     * @throws Exception when an issue occurs
     */
    @SuppressWarnings("unchecked")
    protected SessionConfiguration getSessionConfigurationFromDatabase(final long key, final String host) throws ApplicationException {

        if (parentContext == null) {
            throw new IllegalStateException("ApplicationContext is null in: " + this.getClass());
        }

        final IDbSessionInfoFactory dbSessionInfoFactory = parentContext.getBean(IDbSessionInfoFactory.class);

        final IDbSessionInfoUpdater dbSessionInfo = dbSessionInfoFactory.createQueryableUpdater();
        dbSessionInfo.setSessionKey(key);
        dbSessionInfo.setHostPattern(host);

        final IDbSqlFetchFactory fetchFactory = parentContext.getBean(IDbSqlFetchFactory.class);
        final IOrderByTypeFactory orderByTypeFactory = parentContext.getBean(IOrderByTypeFactory.class);
        final IDbSqlFetch sessionFetch = fetchFactory.getSessionFetch();

        final ISessionOrderByType startTimeOrderDesc = (ISessionOrderByType) orderByTypeFactory.getOrderByType(OrderByType.SESSION_ORDER_BY,
                ISessionOrderByType.ID_DESC_TYPE);

        final int DEFAULT_SESSION_FETCH_BATCH_SIZE = 50;
        List<IDbSessionProvider> databaseSessions = null;
        try {
            databaseSessions = (List<IDbSessionProvider>) sessionFetch.get(dbSessionInfo, null, DEFAULT_SESSION_FETCH_BATCH_SIZE,
                    startTimeOrderDesc);
        } catch (final DatabaseException e) {
            throw new ApplicationException("Encountered error while fetching sessions from database: " + e.getMessage());
        }

        if (databaseSessions.isEmpty()) {
            tracer.debug("No sessions found in the database");
        } else {
            tracer.debug("Returned ", databaseSessions.size(), " session records ");
        }

        IDbSessionProvider matchingSession = null;

        while (!databaseSessions.isEmpty()) {
            try {
                for (final IDbSessionProvider ds : databaseSessions) {
                    final TelemetryConnectionType telemConnType = ds.getConnectionType();
                    if ((telemConnType != null) && !telemConnType.equals(TelemetryConnectionType.UNKNOWN)) {
                        matchingSession = ds;
                        // Get the latest ingest session for FSW/SSE
                        if(ds.getType().equals(INGEST_APP_NAME)){
                            boolean isSse = sseContextFlag.isApplicationSse();
                            if(((isSse && ds.getSseDownlinkFlag())) || (!isSse && ds.getFswDownlinkFlag())) {
                                break;
                            }
                        }
                    }
                }
                databaseSessions = (List<IDbSessionProvider>) sessionFetch.getNextResultBatch();

                if (databaseSessions.isEmpty()) {
                    tracer.debug("No more sessions found in the database");
                } else {
                    tracer.debug("Returned ", databaseSessions.size(), " additional session records ");
                }

            } catch (final DatabaseException e) {
                tracer.warn(Markers.SUPPRESS, ExceptionTools.getMessage(e), e);
                throw new ApplicationException("Encountered error while fetching sessions from database: " + ExceptionTools.getMessage(e));
            }
        }

        if (matchingSession == null) {
            throw new ApplicationException("No sessions found in the database. "
                    + "Provide a valid session configuration file " + "to create a new session.");
        }

        /* Can no longer create session straight
         * from database object. Replace with multiple steps.
         */
        final SessionConfiguration sessionConfig = new SessionConfiguration(parentContext.getBean(MissionProperties.class),
                parentContext.getBean(ConnectionProperties.class), false);

        final IDbSessionFactory dbSessionFactory = parentContext.getBean(IDbSessionFactory.class);
        dbSessionFactory.convertProviderToUpdater(matchingSession).setIntoContextConfiguration(sessionConfig);

        sessionFetch.close();
        return sessionConfig;
    }

    /**
     * Set the Server Application Context as the parent of the Worker
     * process Application Context
     *
     * @param applicationContext The Worker Process Application Context
     */
    private void setParentContext(final ApplicationContext applicationContext) {
        ((GenericApplicationContext) applicationContext).setParent(parentContext);
    }

    /**
     * Set the database properties (primarily host and port) of the worker process
     * to the same database as the server, otherwise the worker will attempt to
     * connect to the database on the same host as the server process by default.
     *
     * @param workerDbProps The worker database properties
     * @param serviceDbProps The server database properties
     */
    private void setDbProperties(final IDatabaseProperties workerDbProps, final IDatabaseProperties serviceDbProps) {
        workerDbProps.setHost(serviceDbProps.getHost());
        workerDbProps.setPort(serviceDbProps.getPort());
    }

    /**
     * Auto wires the web socket connection manager
     * @param connectionManager The web socket connection manager
     */
    @Autowired
    public void setSocketConnectionManager(IWebSocketConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public String toString() {
        return ApplicationConfiguration.getApplicationName()
                + ": {" + simpleContext.getContextId().getContextKey().getContextId() + "}";
    }

    @Override
    public void setValuesFromParent(final ApplicationContext workerContext, final IContextConfiguration workerSession) {
        // Add log endpoints to TI, TP and CFDP
        // Need to set the parent context so that the correct WebSocketConnectionManager object
        // can be retrieved within the WebSocketAppender
        setParentContext(workerContext);


        // Server can be configured through the command line to connect to a remote database.
        // Need to make sure the worker process uses the same database as the server, otherwise
        // worker will attempt to connect to the database on the same machine as the server by default.
        final IDatabaseProperties serviceDbProps = parentContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, DatabaseProperties.class);
        final IDatabaseProperties workerDbProps = workerContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, DatabaseProperties.class);
        setDbProperties(workerDbProps, serviceDbProps);

        final DictionaryProperties props = workerContext.getBean(DictionaryProperties.class);
        props.copyValuesFrom(workerSession.getDictionaryConfig());
        tracer.trace("Setup ", ApplicationConfiguration.getApplicationName(), " worker dictionary ", props);

        // Fix worker and parent context id issue
        final IContextKey processContextKey = workerContext.getBean(IContextKey.class);
        final IContextKey serverContextKey  = simpleContext.getContextId().getContextKey();

        processContextKey.setParentHostId(serverContextKey.getHostId());
        processContextKey.setParentNumber(serverContextKey.getNumber());

        workerSession.getGeneralInfo().setRootPublicationTopic(null);

        workerSession.getGeneralInfo().getSseContextFlag().setApplicationIsSse(sseContextFlag.isApplicationSse());
    }

    /**
     * Method to encapsulate worker configuration (CLI Parsing) and exception handling
     *
     * @param worker to configure
     * @param args arguments to provide; may be null
     * @throws ApplicationException When parsing or database errors occur
     *
     * @return the configured workers IContextIdentification
     */
    public IContextIdentification launchWorker(final ITelemetryWorker worker, final String[] args) throws ApplicationException{
        try {
            ICommandLine cli = null;
            // Use CLI parsing for provided arguments
            cli = worker.createOptions().parseCommandLine(args, false, false, false);
            tracer.debug("Parsed worker CLI: ", Arrays.toString(args));
            worker.configure(cli);

            final IContextIdentification ctx = worker.getContextConfiguration().getContextId();
            workerMap.put(ctx.getContextKey().toString(), worker);

            // update list of session IDs
            simpleContext.addSessionId(ctx.getNumber());
            worker.updateContextDbSessionsList(simpleContext.getSessionIdsAsString());

            return worker.getContextConfiguration().getContextId();
        }
        catch (final ParseException e) {
            tracer.error("Command line parsing error while launching process: ", ExceptionTools.getMessage(e));
            // re-throw exception as ApplicationException
            throw new ApplicationException(e.getMessage());
        }
        catch (final ApplicationException e) {
            tracer.error("Database error while launching process: ",ExceptionTools.getMessage(e));
            // re-throw exception
            throw e;
        //should not happen
        } catch (final Exception e) {
            tracer.error("Error while launching process: ",ExceptionTools.getMessage(e));
            return null;
        }
    }

    @Override
    public List<String> getAvailableDictionaries(final String directory) {
        final boolean inDirectory = directoryExists(directory);
        if (sseContextFlag.isApplicationSse()) {
            if (inDirectory) {
                return parentDictionaryProperties.getAvailableSseVersions(directory);
            } else {
                return parentDictionaryProperties.getAvailableSseVersions();
            }
        } else {
            if (inDirectory) {
                return parentDictionaryProperties.getAvailableFswVersions(directory);
            } else {
                return parentDictionaryProperties.getAvailableFswVersions();
            }
        }
    }

    /**
     * Helper method to determine whether or not a directory exists
     *
     * @param path to check if it exists
     * @return true if it exists; otherwise false
     */
    public boolean directoryExists(final String path) {
        if (path != null) {
            final File file = new File(path);
            return file.exists();
        }
        return false;
    }
}
