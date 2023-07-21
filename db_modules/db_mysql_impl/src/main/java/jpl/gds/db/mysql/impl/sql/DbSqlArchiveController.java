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
package jpl.gds.db.mysql.impl.sql;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.ICommandUpdateStore;
import jpl.gds.db.api.sql.store.IContextConfigStore;
import jpl.gds.db.api.sql.store.IDbSqlStore;
import jpl.gds.db.api.sql.store.IDbSqlStoreFactory;
import jpl.gds.db.api.sql.store.IEndSessionStore;
import jpl.gds.db.api.sql.store.IHostStore;
import jpl.gds.db.api.sql.store.ISessionStore;
import jpl.gds.db.api.sql.store.IStoreConfigurationMap;
import jpl.gds.db.api.sql.store.IStoreMonitor;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ICommandMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.IEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.IFrameLDIStore;
import jpl.gds.db.api.sql.store.ldi.IGatherer;
import jpl.gds.db.api.sql.store.ldi.IHeaderChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.IInserter;
import jpl.gds.db.api.sql.store.ldi.ILDIStore;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.IMonitorChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.IPacketLDIStore;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISsePacketLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IHeaderChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IMonitorChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.ISseChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpFileGenerationLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpFileUplinkFinishedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpIndicationLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpPduReceivedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpPduSentLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpRequestReceivedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpRequestResultLDIStore;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.mysql.impl.sql.store.AggregateStoreMonitor;
import jpl.gds.db.mysql.impl.sql.store.EndSessionStore;
import jpl.gds.db.mysql.impl.sql.store.HostStore;
import jpl.gds.db.mysql.impl.sql.store.StoreMonitor;
import jpl.gds.db.mysql.impl.sql.store.ldi.Gatherer;
import jpl.gds.db.mysql.impl.sql.store.ldi.Inserter;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.shared.types.Pair;

/**
 * The store controller is the central object that is the interface for starting
 * and shutting down all the database stores used by chill down.
 *
 */
public class DbSqlArchiveController implements IDbSqlArchiveController {
    /**
     * Tracer for this and subclasses
     */
    protected final Tracer                            log;

    /**
     * The command itself, with space for data to be inserted:
     */
    private final String                              pid                      = GdsSystemProperties.getPid();

    /**
     * Host id distinguisher for file names
     */
    private String                                    hostId;

    /**
     * Directory where LDI files are stored
     */
    private final AtomicReference<String>             fileBase                 = new AtomicReference<>(null);

    /**
     * If LDI files should be exported, the directory
     */
    private final AtomicReference<String>             ldiExportDirectory       = new AtomicReference<>(null);

    /**
     * Flush every so often, no matter how many packets we have
     */
    private final AtomicLong                          flushTime                = new AtomicLong(IDbSqlArchiveController.MIN_FLUSH);

    /**
     * Index used to create unique file names
     */
    private final AtomicLong                          unique                   = new AtomicLong(0L);

    /**
     * Flag used to signal the serialization queue it is time to idle-down.
     */
    private final AtomicBoolean                       serializationFlush       = new AtomicBoolean(false);

    /**
     * Field list for the product being used
     */
    private final AtomicReference<String>             productFields            = new AtomicReference<>(null);

    /**
     * Distinguisher for file names
     */
    private final AtomicReference<String>             fswSse                   = new AtomicReference<>(null);

    /**
     * We support only one start and one stop sequence
     */
    private final AtomicBoolean                       isStarted                = new AtomicBoolean(false);
    private final AtomicBoolean                       isStopped                = new AtomicBoolean(false);

    /**
     * prevent multiple initialization of database stores
     */
    private final AtomicBoolean                       isInitialized            = new AtomicBoolean(false);

    /**
     * True if configuration file has been read
     */
    private final AtomicBoolean                       isConfigured             = new AtomicBoolean(false);

    /**
     * Used during shutdown to make the gatherer flush everything and exit
     */
    private final AtomicBoolean                       gathererFlushing         = new AtomicBoolean(false);

    /**
     * MPCS-8384  Pre-fetch these three
     */
    private final boolean                             dbExtended;
    private final Set<String>                         dbExtendedTables;
    private final String                              dbExtendedPostfix;

    /** MPCS-7714 */
    private final long                                ldiRowLimit;
    private final long                                ldiRowExceed;

    /**
     * True if LDI files should be saved instead of deleted
     */
    private final boolean                             saveFiles;

    /**
     * Set to indicate whether current application is SSE or FSW
     */
    private final SseContextFlag                      applicationIsSse;

    /** MPCS-7733  No more flush timer apparatus */

    /**
     * A reference to the database fields from the GDS configuration
     */
    private final IMySqlAdaptationProperties        dbProperties;

    /**
     * A reference to the test configuration for the current test
     */
    private IContextConfiguration                     contextConfig;

    /**
     * A reference to the Context Information object for this database
     * controller
     */
    private final IGeneralContextInformation          contextInfo;

    /**
     * A reference to the Context ID object for this database controller
     */
    private IContextIdentification                    contextId;

    /**
     * Flag that determines (for the database controller only) whether it is
     * servicing an integrated execution (chill vs. chill_down).
     */
    private final boolean                             isIntegrated;

    /**
     * If empty, then all stores are to be started. Otherwise, contains the
     * names of those desired stores. Set by apps desiring to speed up the
     * start-up and shut-down process.
     */
    private final Set<StoreIdentifier>                neededStores             = new HashSet<>();

    /**
     * Spring Application Context
     */
    private final ApplicationContext                  appContext;

    /**
     * 
     */
    private final IDbSqlStoreFactory                  storeFactory;


    private final IStatusMessageFactory               messageFactory;

    /**
     * 
     */
    private final IDbSessionInfoFactory               dbSessionInfoFactory;

    /**
     * Map of all active store monitors.
     */
    private final Map<StoreIdentifier, IStoreMonitor> storeMonitorMap          = new HashMap<>();

    /**
     * Daemon thread that waits to shut down the store controller
     */
    private final StoreShutDownDaemon                 shutDownDaemon;

    /**
     * Inner Gatherer Thread
     */
    private Gatherer                                  gatherer;

    /**
     * True if stores must be stopped
     */
    private boolean                                   sessionStoresStarted     = false;

    /**
     * A common test session info object for database fetches based on the
     * common test configuration
     */
    private IDbSessionInfoProvider                    dbSessionInfoProvider    = null;

    /**
     * The configuration map that translates a StoreIdentifier into a
     * StoreMonitor object. This is retrieved from the Spring Application
     * Context, and is an adaptation point so missions can define different
     * adaptation specific stores, tables, etc.
     */
    private IStoreConfigurationMap                    storeConfigMap           = null;

    // This group is synchronized
    private long                                      fswChannelValueIdCounter = ILDIStore.ID_START;
    private final Map<String, Long>                   fswIds                   = new HashMap<>();

    // This group is synchronized
    private long                                      sseChannelValueIdCounter = ILDIStore.ID_START;
    private final Map<String, Long>                   sseIds                   = new HashMap<>();

    private final SseContextFlag                      sseFlag;

    /**
     * Package Private Constructor.
     * 
     * @param appContext
     *            The current application context
     */
    public DbSqlArchiveController(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.dbProperties = appContext.getBean(IMySqlAdaptationProperties.class);
        this.contextInfo = appContext.getBean(IGeneralContextInformation.class);

        this.storeConfigMap = appContext.getBean(IStoreConfigurationMap.class);
        this.storeFactory = appContext.getBean(IDbSqlStoreFactory.class);
        this.dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        this.messageFactory = appContext.getBean(IStatusMessageFactory.class);
        this.sseFlag = appContext.getBean(SseContextFlag.class);

        this.log = TraceManager.getTracer(appContext, Loggers.LDI_INSERTER);
        this.applicationIsSse = sseFlag;

        /*
         * MPCS-11389 / MPCS-11400  - for ancillary processes such as chill_recorded_eng_watacher,
         * this property should be false. Please see rationale in comment below for MPCS-4916.
         */
        this.isIntegrated = GdsSystemProperties.isIntegratedGui();

        this.dbExtended = TimeProperties.getInstance().useExtendedScet();
        this.dbExtendedTables = dbProperties.getExtendedTables();
        this.dbExtendedPostfix = dbProperties.getExtendedPostfix();

        this.saveFiles = dbProperties.getSaveLDI();
        this.shutDownDaemon = new StoreShutDownDaemon();

        log.debug("Save LDI files: " + this.saveFiles);

        /** MPCS-7714  New */
        ldiRowLimit = dbProperties.getLdiRowLimit();
        ldiRowExceed = ldiRowLimit + 1L;

        this.setFswSse(sseFlag.isApplicationSse() ? "_SSE_" : "_FSW_");
        this.setExportDirectory(this.dbProperties.getExportLDIDir());
        log.debug("Export LDI directory: " + this.getExportDirectory());
        log.debug("Process id: " + this.getPid());

        /** MPCS-7733 - Don't use old batch flush */
        this.setFlushTime(Math.max(this.dbProperties.getLdiFlushMilliseconds(), IDbSqlArchiveController.MIN_FLUSH));
        log.debug("Using flush time of " + this.getFlushTime() / IDbSqlArchiveController.D_ONE_SECOND + " seconds");

        resetLDIStores();
    }

    /**
     * Creates specific individual stores. Must be called after construction.
     */
    @Override
    public void init(final StoreIdentifier... storesToStart) {
        init(Arrays.asList(storesToStart));
    }

    /**
     * Creates all individual stores. Must be called after construction.
     */
    @Override
    public void init(final Collection<StoreIdentifier> storesToStartList) {
        neededStores.addAll(storesToStartList);
        init();
    }

    /**
     * Creates specific individual stores. Must be called after construction.
     */
    @Override
    public void init() {
        if (isInitialized.getAndSet(true)) {
            return;
        }

        this.contextConfig = appContext.getBean(IContextConfiguration.class);
        this.contextId = contextConfig.getContextId();
        this.hostId = contextId != null ? contextId.getHostId() != null ? contextId.getHostId().toString() : "" : "";

        String od = IDbSqlArchiveController.DEFAULT_BASE;
        if (contextInfo != null) {
            od = contextInfo.getOutputDir();
            this.setFileBase(od + File.separator + IDbSqlArchiveController.LDI_DIR + File.separator);
        }
        else {
            this.setFileBase(od);
        }

        log.debug("Using base directory of ", this.getFileBase());
        if (!makeDirectoryExist(this.getFileBase())) {
            log.error("Unable to create LDI file base in parent: ", this.getFileStatus(od));
            throw new IllegalStateException("Unable to create LDI file base: " + this.getFileBase());
        }

        // @formatter: off
        setSessionStore(storeFactory.getSessionStore());
        setFrameStore(isNeededStore(IFrameLDIStore.STORE_IDENTIFIER) ? storeFactory.getFrameStore() : null);
        setSsePacketStore(isNeededStore(ISsePacketLDIStore.STORE_IDENTIFIER) ? storeFactory.getSsePacketStore() : null);
        setPacketStore(isNeededStore(IPacketLDIStore.STORE_IDENTIFIER) ? storeFactory.getPacketStore() : null);
        setLogMessageStore(isNeededStore(ILogMessageLDIStore.STORE_IDENTIFIER) ? storeFactory.getLogMessageStore()
                : null);
        setCommandMessageStore(isNeededStore(ICommandMessageLDIStore.STORE_IDENTIFIER)
                ? storeFactory.getCommandMessageStore() : null);
        setSseEvrStore(isNeededStore(ISseEvrLDIStore.STORE_IDENTIFIER) ? storeFactory.getSseEvrStore() : null);
        setEvrStore(isNeededStore(IEvrLDIStore.STORE_IDENTIFIER) ? storeFactory.getEvrStore() : null);
                
        setCommandUpdateStore(isNeededStore(ICommandUpdateStore.STORE_IDENTIFIER) ? storeFactory.getCommandUpdateStore()
				: null);
		setProductStore(isNeededStore(IProductLDIStore.STORE_IDENTIFIER) ? storeFactory.getProductStore() : null);
		setCfdpIndicationStore(
				isNeededStore(ICfdpIndicationLDIStore.STORE_IDENTIFIER) ? storeFactory.getCfdpIndicationStore() : null);
		setCfdpFileGenerationStore(
				isNeededStore(ICfdpFileGenerationLDIStore.STORE_IDENTIFIER) ? storeFactory.getCfdpFileGenerationStore()
						: null);
		setCfdpFileUplinkFinishedStore(isNeededStore(ICfdpFileUplinkFinishedLDIStore.STORE_IDENTIFIER)
				? storeFactory.getCfdpFileUplinkFinishedStore()
				: null);
		setCfdpRequestReceivedStore(isNeededStore(ICfdpRequestReceivedLDIStore.STORE_IDENTIFIER)
				? storeFactory.getCfdpRequestReceivedStore()
				: null);
		setCfdpRequestResultStore(
				isNeededStore(ICfdpRequestResultLDIStore.STORE_IDENTIFIER) ? storeFactory.getCfdpRequestResultStore()
						: null);
		setCfdpPduReceivedStore(
				isNeededStore(ICfdpPduReceivedLDIStore.STORE_IDENTIFIER) ? storeFactory.getCfdpPduReceivedStore()
						: null);
		setCfdpPduSentStore(
				isNeededStore(ICfdpPduSentLDIStore.STORE_IDENTIFIER) ? storeFactory.getCfdpPduSentStore() : null);
        setContextConfigStore(storeFactory.getContextConfigStore());
        
        setChannelAggregateStore(
                isNeededStore(IChannelAggregateLDIStore.STORE_IDENTIFIER) ? storeFactory.getChannelAggregateStore()
                        : null);
        setHeaderChannelAggregateStore(isNeededStore(IHeaderChannelAggregateLDIStore.STORE_IDENTIFIER)
                ? storeFactory.getHeaderChannelAggregateStore()
                : null);
        setSseChannelAggregateStore(isNeededStore(ISseChannelAggregateLDIStore.STORE_IDENTIFIER)
                ? storeFactory.getSseChannelAggregateStore()
                : null);
        setMonitorChannelAggregateStore(isNeededStore(IMonitorChannelAggregateLDIStore.STORE_IDENTIFIER)
                ? storeFactory.getMonitorChannelAggregateStore()
                : null);
        
        startGatherer();

        this.shutDownDaemon.start();
        // @formatter:on
    }

    /**
     * Restart the archive store controller with a different context configuration
     *
     * @return the new context configuration
     */
    @Override
    public synchronized IContextConfiguration restartArchiveWithNewContext(final IContextConfiguration contextConfig,
                                                                           final StoreIdentifier... siList) {
        if (isUp()) {
            log.log(messageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                    "Shutting down Database Archive Controller for Context ID: " + this.contextConfig + "...",
                    LogMessageType.INSERTER));
            shutDown();
        }
        // MPCS-10239
        // set this here incase init() has not yet been called
        // this cannot be initialized in the constructor because it registers context singletons
        // early than they should be. This was causing issues with PDPP child contexts.
        this.contextConfig = appContext.getBean(IContextConfiguration.class);

        log.log(messageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                "Changing Context to: " + contextConfig + "...", LogMessageType.INSERTER));
        this.contextConfig.copyValuesFrom(contextConfig);
        log.log(messageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                "Restarting Database Archive Controller...", LogMessageType.INSERTER));
        init(siList);
        startAllStores();
        log.log(messageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                "Database Archive Controller Restarted with Context ID: " + this.contextConfig,
                LogMessageType.INSERTER));
        return contextConfig;
    }

    @Override
    public boolean isSessionStoresStarted() {
        return sessionStoresStarted;
    }

    /**
     * Reset all LDI processing.
     */
    @Override
    public synchronized void resetLDIStores() {
        storeMonitorMap.clear();
        isConfigured.set(false);
        isStarted.set(false);
        isStopped.set(false);
        isInitialized.set(false);
    }

    /**
     * Add table name of store that is needed. I.e., that will be started.
     *
     * @param si
     *            Database table name
     */
    @Override
    public void addNeededStore(final StoreIdentifier si) {
        if (si != null) {
            neededStores.add(si);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.IDbStoreController#startAllStores(boolean)
     */
    @Override
    public boolean startAllStores() {
        init();

        if (!startSessionStores()) {
            return false;
        }

        if (!startContextConfigStore()) {
            return false;
        }

        startPeripheralStores();
        return true;
    }

    @Override
    public boolean startAllNonSessionStores() {
        init();

        if (!startContextConfigStore()) {
            return false;
        }

        startPeripheralStores();
        return true;
    }

    /** MPCS-7733  Get rid of store list */

    @Override
    public boolean startSessionStores() {
        init();

        if (dbProperties.getUseDatabase() == false) {
            return false;
        }

        final ISessionStore sessionStore = getSessionStore();

        /*
         * MPCS-4916  If integrated, use existing session fragment.
         *
         * We need to know that we we are running the chill integrated app so
         * that we can properly decide whether to create a new session or a new
         * fragment of the existing session, or to just use the existing session
         * and fragment.
         *
         * If there is no sessionid , then a new session with fragment one is
         * always created. If there is a session id, then usually a new fragment
         * will be created for that session.
         *
         * But in integrated chill, we want the main subprocesses (FSW down, SSE
         * down, and uplink) to use the session an fragment already created.
         *
         * Ancillary processes (chill_recorded_eng_watcher, maybe others) will
         * always use a new session fragment. They do not know that they are
         * running under integrated chill.
         */
        if (!dbProperties.getUseDatabase()) {
            return false;
        }

        try {
            sessionStore.start();

            if (!sessionStore.isConnected()) {
                return false;
            }

            // If we don't have a test number, that means this is a new test
            // so we have to insert this test configuration into the database,
            // get the new test key, and set it back onto the test
            // configuration

            final Long sessionId = contextConfig.getContextId().getNumber();

            if ((sessionId == null) || (sessionId.longValue() < 1L)) {
                // Make a new sessionId with sessionFragment of one
                contextConfig.getContextId().setNumber(sessionStore.insertTestConfig(contextConfig, 0L));
            }
            else if (!this.isIntegrated) {
                // MPCS-4916  If integrated, use existing
                // session fragment
                // Make a new sessionFragment for this sessionId
                contextConfig.getContextId().setNumber(sessionStore.insertTestConfig(contextConfig, sessionId));
            }

            startHostStore();
            startEndSessionStore();
            if (dbProperties.getExportLDIAny()) {
                // Create LDI file and write it directly to the export
                // directory. It is needed when exporting.
                final IHostStore hostStore = getHostStore();
                hostStore.writeLDI(contextConfig);
                sessionStore.writeLDI(contextConfig);
            }
        }
        catch (final Exception e) {
            log.error("Exception starting test configuration database store: " + e.getMessage(), e);
            return false;
        }

        dbSessionInfoProvider = dbSessionInfoFactory.createQueryableProvider(contextConfig);
        sessionStoresStarted = true;
        return true;
    }

    /**
     * @return true if successful, false if not
     */
    @Override
    public boolean startSessionStoresWithoutInserting() {
        init();

        if (dbProperties.getUseDatabase() == false) {
            return false;
        }

        final ISessionStore sessionStore = getSessionStore();

        /*
         * MPCS-4916 -  If integrated, use existing session fragment.
         *
         * We need to know that we we are running the chill integrated app so
         * that we can properly decide whether to create a new session or a new
         * fragment of the existing session, or to just use the existing session
         * and fragment.
         *
         * If there is no sessionid , then a new session with fragment one is
         * always created. If there is a session id, then usually a new fragment
         * will be created for that session.
         *
         * But in integrated chill, we want the main subprocesses (FSW down, SSE
         * down, and uplink) to use the session an fragment already created.
         *
         * Ancillary processes (chill_recorded_eng_watcher, maybe others) will
         * always use a new session fragment. They do not know that they are
         * running under integrated chill.
         */
        if (!dbProperties.getUseDatabase()) {
            return false;
        }

        try {
            sessionStore.start();

            if (!sessionStore.isConnected()) {
                return false;
            }

            startHostStore();
            startEndSessionStore();
        }
        catch (final Exception e) {
            log.error("Exception starting test configuration database store: " + e.getMessage(), e);
            return false;
        }

        dbSessionInfoProvider = dbSessionInfoFactory.createQueryableProvider(contextConfig);
        sessionStoresStarted = true;
        return true;
    }

    @Override
    public boolean startContextConfigStore() {
        init();
        final IContextConfigStore contextStore = getContextConfigStore();

        if (!dbProperties.getUseDatabase()) {
            return false;
        }

        try{
            contextStore.start();
            if (!contextStore.isConnected()) {
                return false;
            }
        }
        catch (final Exception e) {
            log.error("Exception starting context configuration database store: " + e.getMessage(), e);
            return false;
        }
        return true;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.api.sql.store.IDbStoreController#startTestEndSessionStore()
     */
    private boolean startHostStore() {
        init();

        if (!dbProperties.getUseDatabase()) {
            return false;
        }

        try {
            setHostStore(new HostStore(appContext));
            getHostStore().start();
        }
        catch (final Exception e) {
            log.error("Exception starting host database store: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.api.sql.store.IDbStoreController#startTestEndSessionStore()
     */
    private boolean startEndSessionStore() {
        init();

        if (!dbProperties.getUseDatabase()) {
            return false;
        }

        try {
            setEndSessionStore(new EndSessionStore(appContext));
            getEndSessionStore().start();
        }
        catch (final Exception e) {
            log.error("Exception starting end session database store: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.IDbStoreController#startPeripheralStores()
     */
    @Override
    public void startPeripheralStores() {
        init();

        startStoreIfNeeded(getFrameStore());
        startStoreIfNeeded(getPacketStore());
        startStoreIfNeeded(getSsePacketStore());
        startStoreIfNeeded(getLogMessageStore());
        startStoreIfNeeded(getCommandMessageStore());
        startStoreIfNeeded(getProductStore());
        startStoreIfNeeded(getEvrStore());
        startStoreIfNeeded(getSseEvrStore());
        // MPCS-10842 -
        // Removed legacy ChannelValue stores
        startStoreIfNeeded(getChannelAggregateStore());
        startStoreIfNeeded(getHeaderChannelAggregateStore());
        startStoreIfNeeded(getMonitorChannelAggregateStore());
        startStoreIfNeeded(getSseChannelAggregateStore());
        startStoreIfNeeded(getCfdpIndicationStore());
        startStoreIfNeeded(getCfdpFileGenerationStore());
        startStoreIfNeeded(getCfdpFileUplinkFinishedStore());
        startStoreIfNeeded(getCfdpRequestReceivedStore());
        startStoreIfNeeded(getCfdpRequestResultStore());
        startStoreIfNeeded(getCfdpPduReceivedStore());
        startStoreIfNeeded(getCfdpPduSentStore());
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.IDbStoreController#startLogCommandStores()
     */
    @Override
    public void startLogCommandStores() {
        init();

        startLogCommandStores(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.IDbStoreController#startLogCommandStores(
     * boolean)
     */
    @Override
    public void startLogCommandStores(final boolean alsoLog) {
        init();

        startStoreIfNeeded(getCommandMessageStore());

        if (alsoLog) {
            startStoreIfNeeded(getLogMessageStore());
        }
    }

    /**
     * Start a store, but only if it is both requested in the configuration AND
     * actually needed by the app (or he doesn't specify any stores, which means
     * he wants all of them.)
     *
     * Test session and end test session stores are started separately.
     *
     * @param store
     */
    private void startStoreIfNeeded(final IDbSqlStore store) {
        if (store == null) {
            return;
        }
        store.start();
    }

    /**
     * Start the LDI Gatherer Thread
     */
    @Override
    public synchronized void startGatherer() {
        if (null == gatherer) {
            // Initialize Gatherer
            gatherer = new Gatherer(appContext);
            gathererFlushing.set(false);
        }
        gatherer.start();
    }

    /**
     * Start the LDI Gatherer Thread
     */
    @Override
    public synchronized void stopGatherer() {
        if (null != gatherer) {
            gatherer.interrupt();
        }
    }

    @Override
    public void shutDownInserters() {
        // Ask the inserters to exit (and close the statements, etc.)
        for (final StoreIdentifier si : StoreIdentifier.values()) {
            final IStoreMonitor monitor = getStoreMonitor(si);
            if (monitor == null) {
                continue;
            }
            final IInserter inserter = monitor.getInserter();
            if (!inserter.isStarted()) {
                // Never started
                continue;
            }

            inserter.informShutDown(); // Effectively inserter
            SleepUtilities.checkedJoin((Thread) inserter, IDbSqlArchiveController.INSERTER_JOIN, "inserter " + si, log);

            // Close the store
            monitor.getStore().close();
            log.debug(si + " values processed: " + monitor.getValuesProcessed());
            log.debug(si + " metadata processed: " + monitor.getValuesProcessed());
        }
    }

    /**
     * Shutdown the controller and all database stores
     */
    @Override
    public void shutDown() {
        if (isUp()) {
            stopAllStores();
            while (!isStopped.get()) {
                try {
                    Thread.sleep(500);
                }
                catch (final InterruptedException e) {
                    log.debug("Shutdown daemon thread stop sequence was interrupted", e.getCause());
                }
            }
            try {
                shutDownDaemon.join(IDbSqlArchiveController.SHUTDOWN_JOIN);
            }
            catch (final InterruptedException e) {
                log.debug("Shutdown daemon thread stop sequence was interrupted", e.getCause());
                Thread.currentThread().interrupt();
            }
            resetLDIStores();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.IDbStoreController#stopAllStores()
     */
    @Override
    public void stopAllStores() {
        //MPCS-10617 - Stop all stores to allow server shutdown
        if (!dbProperties.getUseDatabase()) {
            isStopped.set(true);
            return;
        }
        stopPeripheralStores();
        stopContextConfigStore();
        stopSessionStores();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.IDbStoreController#stopSessionStores()
     */
    @Override
    public void stopSessionStores() {
        if (!dbProperties.getUseDatabase() || !sessionStoresStarted) {
            return;
        }
        stopSessionStore();
        stopEndSessionStore();
        stopHostStore();

        sessionStoresStarted = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.IDbStoreController#stopTestSessionStore()
     */
    @Override
    public void stopSessionStore() {
        final ISessionStore sessionStore = getSessionStore();
        if (sessionStore != null) {
            sessionStore.stop();
            sessionStore.close();
        }
    }

    @Override
    public void stopContextConfigStore(){
        final IContextConfigStore contextStore = getContextConfigStore();
        if(contextStore != null){
            contextStore.stop();
            contextStore.close();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.api.sql.store.IDbStoreController#stopTestEndSessionStore()
     */
    @Override
    public void stopEndSessionStore() {
        final IEndSessionStore endSessionStore = getEndSessionStore();
        if (endSessionStore != null) {
            endSessionStore.stop();
            endSessionStore.close();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.IDbStoreController#stopPeripheralStores()
     */
    @Override
    public void stopPeripheralStores() {
        stopStoreIfNeeded(getFrameStore());
        stopStoreIfNeeded(getPacketStore());
        stopStoreIfNeeded(getSsePacketStore());
        stopStoreIfNeeded(getLogMessageStore());
        stopStoreIfNeeded(getCommandMessageStore());
        stopStoreIfNeeded(getProductStore());
        stopStoreIfNeeded(getEvrStore());
        stopStoreIfNeeded(getSseEvrStore());
        // MPCS-10842 -
        // Removed legacy ChannelValue stores
        stopStoreIfNeeded(getChannelAggregateStore());
        stopStoreIfNeeded(getHeaderChannelAggregateStore());
        stopStoreIfNeeded(getMonitorChannelAggregateStore());
        stopStoreIfNeeded(getSseChannelAggregateStore());        
        stopStoreIfNeeded(getCfdpIndicationStore());
        stopStoreIfNeeded(getCfdpFileGenerationStore());
        stopStoreIfNeeded(getCfdpFileUplinkFinishedStore());
        stopStoreIfNeeded(getCfdpRequestReceivedStore());
        stopStoreIfNeeded(getCfdpRequestResultStore());
        stopStoreIfNeeded(getCfdpPduReceivedStore());
        stopStoreIfNeeded(getCfdpPduSentStore());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.api.sql.store.IDbStoreController#stopTestEndSessionStore()
     */
    @Override
    public void stopHostStore() {
        final IHostStore hostStore = getHostStore();
        if (hostStore != null) {
            hostStore.stop();
            hostStore.close();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.IDbStoreController#stopLogCommandStores()
     */
    @Override
    public void stopLogCommandStores() {
        stopStoreIfNeeded(getLogMessageStore());
        stopStoreIfNeeded(getCommandMessageStore());
    }

    /**
     * Stop a store, but only if needed.
     *
     * Test session and end test session stores are started separately.
     *
     * @param store
     */
    private void stopStoreIfNeeded(final IDbSqlStore store) {
        if (store == null) {
            return;
        }
        store.stop();
    }

    @Override
    public boolean getUseArchive(final StoreIdentifier si) {
        String valueTableName = storeConfigMap.get(si).getValueTableName();
        String metaTableName = storeConfigMap.get(si).getMetadataTableName();

        if (valueTableName != null) {
            valueTableName = valueTableName.replaceAll("Sse", "");
        }

        if (metaTableName != null) {
            metaTableName = metaTableName.replaceAll("Sse", "");
        }

        // System.out.println("si=" + si + ", valueTableName=" + valueTableName + ", metaTableName=" + metaTableName);

        return dbProperties.getUseArchive(valueTableName) || dbProperties.getUseArchive(metaTableName);
    }

    /**
     * @return the neededStores
     */
    public Set<StoreIdentifier> getNeededStores() {
        return neededStores;
    }

    /**
     * Check whether store is needed.
     *
     * @param si Store identifier
     *
     * @return True if the store is needed.
     */
    private boolean isNeededStore(final StoreIdentifier si) {
        if (si == null || storeConfigMap.get(si) == null) {
            return false;
        }

        /*
         * ALWAYS instantiate HeaderChannelAggregate and LogMessage stores.
         */
        if ((StoreIdentifier.HeaderChannelAggregate == si) || (StoreIdentifier.LogMessage == si)) {
            return true;
        }

        /*
         * Instantiate this store if the neededStores set contains its StoreIdentifier
         */
        boolean needed = false;

        /*
         * MPCS-8825:  Changed behavior of DbSqlArchiveController such that, if any stores are
         * specified in its init()
         * method, it will ignore any stores specified in the GdsConfiguration. The GdsConfiguration will only
         * be used to determine what stores will be instantiated if no stores are specified.
         */
        if (neededStores.isEmpty()) {
            if (getUseArchive(si)) {
                needed = true;
            }
        }
        else if (neededStores.contains(si)) {
            needed = true;
        }

        /*
         * If the store being tested and the current application context do not agree
         * on whether we are processing FSW or SSE/GSE data, then do NOT instantiate
         * this store.
         * 
         */
        if (storeConfigMap.get(si).isSse() != this.applicationIsSse.isApplicationSse()) {
            // logWarningNoDb("The ", si, " store is not being started because the store is ",
            // (storeConfigMap.get(si).isSse() ? "" : "NOT "),
            // "configured for SSE, and the application is ", (this.applicationIsSse? "" : "NOT"), " configured for
            // SSE.");
            needed = false;
        }

        return needed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.api.sql.store.IDbStoreController#updateSessionEndTime(jpl.gds.
     * core.session.config.SessionConfiguration,
     * jpl.gds.session.util.SessionSummary)
     */
    @Override
    public void updateSessionEndTime(final IContextConfiguration contextConfig, final ITelemetrySummary sum) {
        final IEndSessionStore endSessionStore = getEndSessionStore();
        if (endSessionStore == null) {
            return;
        }
        try {
            endSessionStore.insertEndSession(contextConfig.getContextId());
        }
        catch (final DatabaseException de) {
            log.error("Could not update End of Session Table: " + de.toString(), de.getCause());
        }
    }

    /**
     * @return true if extended database is storing "Extended Precision SCET"
     */
    @Override
    public boolean isExtendedDatabase() {
        return dbExtended;
    }

    /**
     * @return the PostFix for extended SCET compliant tables
     */
    @Override
    public String getExtendedPostfix() {
        return dbProperties.getExtendedPostfix();
    }

    /**
     * Will modify table name based upon whether DB is using extended SCET
     * tables.
     * 
     * @param table
     *            the table name to translate
     * @return the modified table name
     */
    @Override
    public String getActualTableName(final String table) {
        return getActualTableName(new StringBuilder(), table).toString();
    }

    /**
     * Will modify table name based upon whether DB is using extended SCET
     * tables.
     * 
     * @param sb
     *            the table name to translate
     * @return modified the modified table name
     */
    @Override
    public StringBuilder getActualTableName(final StringBuilder sb, final String table) {
        sb.append(table);
        if (dbExtended && dbExtendedTables.contains(table)) {
            sb.append(dbExtendedPostfix);
        }
        return sb;
    }

    /**
     * @return the Context Configuration associated with this Database
     *         Controller.
     */
    @Override
    public IContextConfiguration getContextConfiguration() {
        return contextConfig;
    }

    /**
     * @return an instance of an IDbSessionInfoProvider object created from the
     *         initialized IContextConfiguration after the context is stored to
     *         the database.
     */
    @Override
    public IDbSessionInfoProvider getDbSessionInfoProvider() {
        if (!sessionStoresStarted) {
            throw new IllegalStateException("Cannot ask Database Controller for a Database Session Info Provider until Session Stores are started.");
        }
        return dbSessionInfoProvider;
    }

    /**
     * @return the sessionStore
     */
    @Override
    public ISessionStore getSessionStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.Session);
        return (monitor == null) ? null : (ISessionStore) monitor.getStore();
    }

    @Override
    public IContextConfigStore getContextConfigStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.ContextConfig);
        return (monitor == null) ? null : (IContextConfigStore) monitor.getStore();
    }

    /**
     * @return the endSessionStore
     */
    @Override
    public IEndSessionStore getEndSessionStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.EndSession);
        return (monitor == null) ? null : (IEndSessionStore) monitor.getStore();
    }

    /**
     * @return the hostStore
     */
    @Override
    public IHostStore getHostStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.Host);
        return (monitor == null) ? null : (IHostStore) monitor.getStore();
    }

    /**
     * @return the channelValueStore
     */
    @Override
    public IChannelValueLDIStore getChannelValueStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.ChannelValue);
        return (monitor == null) ? null : (IChannelValueLDIStore) monitor.getStore();
    }

    /**
     * @return the commandMessageStore
     */
    @Override
    public ICommandMessageLDIStore getCommandMessageStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.CommandMessage);
        return (monitor == null) ? null : (ICommandMessageLDIStore) monitor.getStore();
    }

    /**
     * @return the evrStore
     */
    @Override
    public IEvrLDIStore getEvrStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.Evr);
        return (monitor == null) ? null : (IEvrLDIStore) monitor.getStore();
    }

    /**
     * @return the frameStore
     */
    @Override
    public IFrameLDIStore getFrameStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.Frame);
        return (monitor == null) ? null : (IFrameLDIStore) monitor.getStore();
    }

    /**
     * @return the headerChannelValueStore
     */
    @Override
    public IHeaderChannelValueLDIStore getHeaderChannelValueStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.HeaderChannelValue);
        return (monitor == null) ? null : (IHeaderChannelValueLDIStore) monitor.getStore();
    }

    /**
     * @return the logMessageStore
     */
    @Override
    public ILogMessageLDIStore getLogMessageStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.LogMessage);
        return (monitor == null) ? null : (ILogMessageLDIStore) monitor.getStore();
    }

    /**
     * @return the monitorChannelValueStore
     */
    @Override
    public IMonitorChannelValueLDIStore getMonitorChannelValueStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.MonitorChannelValue);
        return (monitor == null) ? null : (IMonitorChannelValueLDIStore) monitor.getStore();
    }

    /**
     * @return the packetStore
     */
    @Override
    public IPacketLDIStore getPacketStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.Packet);
        return (monitor == null) ? null : (IPacketLDIStore) monitor.getStore();
    }

    /**
     * @return the productStore
     */
    @Override
    public IProductLDIStore getProductStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.Product);
        return (monitor == null) ? null : (IProductLDIStore) monitor.getStore();
    }

    /**
     * @return the sseChannelValueStore
     */
    @Override
    public ISseChannelValueLDIStore getSseChannelValueStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.SseChannelValue);
        return (monitor == null) ? null : (ISseChannelValueLDIStore) monitor.getStore();
    }

    /**
     * @return the sseEvrStore
     */
    @Override
    public ISseEvrLDIStore getSseEvrStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.SseEvr);
        return (monitor == null) ? null : (ISseEvrLDIStore) monitor.getStore();
    }

    /**
     * @return the ssePacketStore
     */
    @Override
    public ISsePacketLDIStore getSsePacketStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.SsePacket);
        return (monitor == null) ? null : (ISsePacketLDIStore) monitor.getStore();
    }

    @Override
    public ICommandUpdateStore getCommandUpdateStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.CommandUpdate);
        return (monitor == null) ? null : (ICommandUpdateStore) monitor.getStore();
    }

    /**
     * Return the StoreMonitor object for the specified Store
     * 
     * @param si
     *            the StoreIdentfier of the IStoreMonitor to be returned
     * @return the IStoreMonitor requested
     */
    @Override
    public IStoreMonitor getStoreMonitor(final StoreIdentifier si) {
        return storeMonitorMap.get(si);
    }

	@Override
	public ICfdpIndicationLDIStore getCfdpIndicationStore() {
		final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.CfdpIndication);
		return (monitor == null) ? null : (ICfdpIndicationLDIStore) monitor.getStore();
	}

	@Override
	public ICfdpFileGenerationLDIStore getCfdpFileGenerationStore() {
		final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.CfdpFileGeneration);
		return (monitor == null) ? null : (ICfdpFileGenerationLDIStore) monitor.getStore();
	}

	@Override
	public ICfdpFileUplinkFinishedLDIStore getCfdpFileUplinkFinishedStore() {
		final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.CfdpFileUplinkFinished);
		return (monitor == null) ? null : (ICfdpFileUplinkFinishedLDIStore) monitor.getStore();
	}

	@Override
	public ICfdpRequestReceivedLDIStore getCfdpRequestReceivedStore() {
		final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.CfdpRequestReceived);
		return (monitor == null) ? null : (ICfdpRequestReceivedLDIStore) monitor.getStore();
	}

	@Override
	public ICfdpRequestResultLDIStore getCfdpRequestResultStore() {
		final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.CfdpRequestResult);
		return (monitor == null) ? null : (ICfdpRequestResultLDIStore) monitor.getStore();
	}

	@Override
	public ICfdpPduReceivedLDIStore getCfdpPduReceivedStore() {
		final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.CfdpPduReceived);
		return (monitor == null) ? null : (ICfdpPduReceivedLDIStore) monitor.getStore();
	}

	@Override
	public ICfdpPduSentLDIStore getCfdpPduSentStore() {
		final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.CfdpPduSent);
		return (monitor == null) ? null : (ICfdpPduSentLDIStore) monitor.getStore();
	}

    @Override
    public IChannelAggregateLDIStore getChannelAggregateStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.ChannelAggregate);
        return (monitor == null) ? null : (IChannelAggregateLDIStore) monitor.getStore();
    }

	@Override
	public IHeaderChannelAggregateLDIStore getHeaderChannelAggregateStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.HeaderChannelAggregate);
        return (monitor == null) ? null : (IHeaderChannelAggregateLDIStore) monitor.getStore();
	}
	
	@Override
	public IMonitorChannelAggregateLDIStore getMonitorChannelAggregateStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.MonitorChannelAggregate);
        return (monitor == null) ? null : (IMonitorChannelAggregateLDIStore) monitor.getStore();
	}
	
	@Override
	public ISseChannelAggregateLDIStore getSseChannelAggregateStore() {
        final IStoreMonitor monitor = storeMonitorMap.get(StoreIdentifier.SseChannelAggregate);
        return (monitor == null) ? null : (ISseChannelAggregateLDIStore) monitor.getStore();
	}	
	
	
    /**
     * @return the sessionStore
     */
    @Override
    public IStoreMonitor setStoreMonitor(final StoreIdentifier si, final IDbSqlStore store) {
        IStoreMonitor monitor = null;
        synchronized (storeMonitorMap) {
            if (store != null) {
                if (storeMonitorMap.containsKey(si)) {
                    throw new IllegalStateException(si + " Store has already been created");
                }
                
                // MPCS-10410 : Add performance metric debug
                switch (si) {
                    case ChannelAggregate:
                    case HeaderChannelAggregate:
                    case MonitorChannelAggregate:
                    case SseChannelAggregate:
                        monitor = new AggregateStoreMonitor(store, si);
                        break;
                default:
                    monitor = new StoreMonitor(store, si);
                    break;
                }
                
                monitor.setInserter(new Inserter(appContext, si, log));

                final boolean export_all = this.dbProperties.getExportLDI();
                switch (monitor.getSi()) {
                    case ChannelValue:
                    case HeaderChannelValue:
                    case MonitorChannelValue:
                    case SseChannelValue:
                        monitor.setExport(export_all || this.dbProperties.getExportLDIChannel());
                        break;
                    case CommandMessage:
                        monitor.setExport(export_all || this.dbProperties.getExportLDICommands());
                        break;
                    case Evr:
                    case SseEvr:
                        monitor.setExport(export_all || this.dbProperties.getExportLDIEvrs());
                        break;
                    case Frame:
                        monitor.setExport(export_all || this.dbProperties.getExportLDIFrame());
                        break;
                    case LogMessage:
                        monitor.setExport(export_all || this.dbProperties.getExportLDILog());
                        break;
                    case Packet:
                    case SsePacket:
                        monitor.setExport(export_all || this.dbProperties.getExportLDIPacket());
                        break;
                    case Product:
                        monitor.setExport(export_all || this.dbProperties.getExportLDIProduct());
                        break;
                    case CfdpIndication:
                    case CfdpFileGeneration:
                    case CfdpFileUplinkFinished:
                    case CfdpRequestReceived:
                    case CfdpRequestResult:
                    case CfdpPduReceived:
                    case CfdpPduSent:
                        monitor.setExport(export_all || this.dbProperties.getExportLDICfdp());
                        break;
                    case ChannelAggregate:
                    case HeaderChannelAggregate:
                    case MonitorChannelAggregate:
                    case SseChannelAggregate:
                        monitor.setExport(export_all || this.dbProperties.getExportLDIChannelAggregates());
                        break;
                    case None:
                    default:
                        break;
                }
                storeMonitorMap.put(si, monitor);
            }
        }
        return monitor;
    }

    private void setSessionStore(final ISessionStore store) {
        setStoreMonitor(StoreIdentifier.Session, store);
    }

    private void setEndSessionStore(final IEndSessionStore store) {
        setStoreMonitor(StoreIdentifier.EndSession, store);
    }

    private void setHostStore(final IHostStore store) {
        setStoreMonitor(StoreIdentifier.Host, store);
    }

    private void setCommandMessageStore(final ICommandMessageLDIStore store) {
        setStoreMonitor(StoreIdentifier.CommandMessage, store);
    }

    private void setEvrStore(final IEvrLDIStore store) {
        setStoreMonitor(StoreIdentifier.Evr, store);
    }

    private void setFrameStore(final IFrameLDIStore store) {
        setStoreMonitor(StoreIdentifier.Frame, store);
    }

    private void setLogMessageStore(final ILogMessageLDIStore store) {
        setStoreMonitor(StoreIdentifier.LogMessage, store);
    }

    private void setPacketStore(final IPacketLDIStore store) {
        setStoreMonitor(StoreIdentifier.Packet, store);
    }

    private void setProductStore(final IProductLDIStore store) {
        setStoreMonitor(StoreIdentifier.Product, store);
    }

    private void setSseEvrStore(final ISseEvrLDIStore store) {
        setStoreMonitor(StoreIdentifier.SseEvr, store);
    }

    private void setSsePacketStore(final ISsePacketLDIStore store) {
        setStoreMonitor(StoreIdentifier.SsePacket, store);
    }

    private void setCommandUpdateStore(final ICommandUpdateStore store) {
        setStoreMonitor(StoreIdentifier.CommandUpdate, store);
    }

    private void setCfdpIndicationStore(final ICfdpIndicationLDIStore store) {
   		setStoreMonitor(StoreIdentifier.CfdpIndication, store);
    }
    
    private void setCfdpFileGenerationStore(final ICfdpFileGenerationLDIStore store) {
   		setStoreMonitor(StoreIdentifier.CfdpFileGeneration, store);
    }

    private void setCfdpFileUplinkFinishedStore(final ICfdpFileUplinkFinishedLDIStore store) {
   		setStoreMonitor(StoreIdentifier.CfdpFileUplinkFinished, store);
    }

    private void setCfdpRequestReceivedStore(final ICfdpRequestReceivedLDIStore store) {
   		setStoreMonitor(StoreIdentifier.CfdpRequestReceived, store);
    }

    private void setCfdpRequestResultStore(final ICfdpRequestResultLDIStore store) {
   		setStoreMonitor(StoreIdentifier.CfdpRequestResult, store);
    }

    private void setCfdpPduReceivedStore(final ICfdpPduReceivedLDIStore store) {
   		setStoreMonitor(StoreIdentifier.CfdpPduReceived, store);
    }

    private void setCfdpPduSentStore(final ICfdpPduSentLDIStore store) {
   		setStoreMonitor(StoreIdentifier.CfdpPduSent, store);
    }

    private void setContextConfigStore(final IContextConfigStore store) {
        setStoreMonitor(StoreIdentifier.ContextConfig, store);
    }

    private void setChannelAggregateStore(final IChannelAggregateLDIStore store) {
        setStoreMonitor(StoreIdentifier.ChannelAggregate, store);
    }

    private void setHeaderChannelAggregateStore(final IHeaderChannelAggregateLDIStore store) {
        setStoreMonitor(StoreIdentifier.HeaderChannelAggregate, store);
    }
    
    private void setMonitorChannelAggregateStore(final IMonitorChannelAggregateLDIStore store) {
        setStoreMonitor(StoreIdentifier.MonitorChannelAggregate, store);
    }
    
    private void setSseChannelAggregateStore(final ISseChannelAggregateLDIStore store) {
        setStoreMonitor(StoreIdentifier.SseChannelAggregate, store);
    }
    
    
    /**
     * Construct a stream on a newly-created file
     *
     * A unique file name is generated, the file created, and a stream opened
     * for writing. Care is taken in case the file already exists.
     *
     * File names are made up of the table name, the time-of-day in
     * milliseconds, and a unique index.
     *
     * We return a Pair because the File object cannot be gotten from the
     * stream, and the inserter needs it.
     *
     * The base is usually set to _file_base, but when exporting LDI files, can
     * be the export directory.
     *
     * @param table
     *            The name of the database table
     * @param base
     *            The file base path
     * @param export
     *            True if file is not standard LDI (no longer used)
     *
     * @return The file object and its stream
     */
    @Override
    public Pair<File, FileOutputStream> openStream(final String table, final String base, final boolean export) {
        if (base == null) {
            throw new IllegalStateException("LDI file base is null");
        }

        final File baseFile = new File(base);
        final StringBuilder sb = new StringBuilder(base);

        /** MPCS-8384 Modify table name if required */
        getActualTableName(sb, table);

        sb.append((getFswSse().toString().equals("null")) ? "_" : getFswSse());
        sb.append(getPid()).append('_');
        sb.append(hostId);
        sb.append(System.currentTimeMillis()).append('_');

        final int prefix_length = sb.length();
        String fileName = null;
        File file = null;

        for (int i = 0; i < IDbSqlArchiveController.OPEN_REPEAT; ++i) {
            appendUnique(getAndIncrementUnique(), sb);

            fileName = sb.toString();
            file = new File(fileName);

            try {
                if (file.createNewFile()) {
                    // if (! archiveController.isSaveFiles().get())
                    // {
                    // Try to make it go away even if we crash
                    // BRN: 04/28/2009 We're disabling the delete on exit so
                    // that if users kill the downlink early
                    // we'll still have the leftover LDI files around as
                    // proof
                    // file.deleteOnExit();
                    // }
                    break;
                }

                log.warn("File already exists: '" + fileName + "'");
            }
            catch (final IOException ioe) {
                log.warn("Unable to create file '" + fileName + "': " + ioe + ", attempt " + i + 1, ioe.getCause());

                checkDirectoryExistence(baseFile, base);
            }

            // Try again
            sb.setLength(prefix_length);

            fileName = null;
            file = null;

            // Sleep a short random time to break synchronization with other
            // processes

            SleepUtilities.randomSleep(IDbSqlArchiveController.ONE_SECOND);
        }

        if (file == null) {
            // Use sb instead of fileName to get last attempted name;
            // fileName will be null

            log.error("Unable to create unique file '" + sb + "' after " + IDbSqlArchiveController.OPEN_REPEAT
                    + " tries");

            return null;
        }

        try {
            return new Pair<File, FileOutputStream>(file, new FileOutputStream(file));
        }
        catch (final FileNotFoundException fnfe) {
            log.error("Unable to open unique file '" + fileName + "' :" + fnfe.getMessage(), fnfe.getCause());

            return null;
        }
    }

    /**
     * See openStream/2. This version uses the standard file base.
     *
     * @param table
     *            Name of database table
     *
     * @return Pair<File, FileOutputStream>
     */
    @Override
    public Pair<File, FileOutputStream> openStream(final String table) {
        return openStream(table, getFileBase(), false);
    }

    /**
     * Check that the directory exists. Try several times.
     *
     * @param file
     *            Directory path
     * @param name
     *            Name for logging
     *
     * @return True if everything is OK
     */
    private boolean checkDirectoryExistence(final File file, final String name) {
        for (int i = 0; i < IDbSqlArchiveController.TRY_OPERATION; ++i) {
            if (i > 0) {
                // Sleep a short random time to break synchronization with other
                // processes

                SleepUtilities.randomSleep(IDbSqlArchiveController.ONE_SECOND);
            }

            if (file.exists()) {
                return true;
            }
        }

        log.warn("Directory check failed, no such directory '" + name + "'");

        return false;
    }

    /**
     * Create directory if necessary.
     *
     * @param name
     *            Directory path
     *
     * @return True if everything is OK
     */
    private boolean makeDirectoryExist(final String name) {
        final File file = new File(name);

        for (int i = 0; i < IDbSqlArchiveController.TRY_OPERATION; ++i) {
            if (i > 0) {
                // Sleep a short random time to break synchronization with other
                // processes

                SleepUtilities.randomSleep(IDbSqlArchiveController.ONE_SECOND);
            }

            if (file.exists()) {
                return true;
            }

            if (!file.mkdirs()) {
                log.warn("Directory create failed for '" + name + "' attempt " + i + 1);
            }
        }

        log.error("Directory create failed for '" + name + "'");

        return false;
    }

    /**
     * Convert long to string with prepended zeroes and append it to a string
     * builder.
     *
     * @param l
     *            Long value to convert
     * @param sb
     *            Where to put it
     */
    private void appendUnique(final long l, final StringBuilder sb) {
        final String formatted = String.valueOf(l);

        switch (formatted.length()) {
            case 1:
                sb.append("00000");
                break;
            case 2:
                sb.append("0000");
                break;
            case 3:
                sb.append("000");
                break;
            case 4:
                sb.append("00");
                break;
            case 5:
                sb.append("0");
                break;
            default:
                break;
        }

        sb.append(formatted);
    }

    /**
     * Dump out the status of a file.
     *
     * @param name
     *            Name of file
     *
     * @return String
     */
    @Override
    public String getFileStatus(final String name) {
        return getFileStatus((name != null) ? new File(name) : null);
    }

    /**
     * Dump out the status of a file.
     *
     * @param file
     *            File in question
     *
     * @return String
     */
    @Override
    public String getFileStatus(final File file) {
        if (file == null) {
            return "File null";
        }

        final StringBuilder sb = new StringBuilder("File '");

        sb.append(file.getAbsolutePath());
        sb.append("' r ").append(file.canRead());
        sb.append(" w ").append(file.canWrite());
        sb.append(" x ").append(file.canExecute());
        sb.append(" d ").append(file.isDirectory());
        sb.append(" f ").append(file.isFile());
        sb.append(" h ").append(file.isHidden());
        sb.append(" e ").append(file.exists());

        return sb.toString();
    }

    /**
     * @return true if database configuration saves files, false if not
     */
    @Override
    public boolean isSaveFiles() {
        return saveFiles;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.IDbSqlArchiveController#getTestConfig()
     */
    @Override
    public IContextConfiguration getTestConfig() {
        return contextConfig;
    }

    @Override
    public long getLdiRowExceed() {
        return this.ldiRowExceed;
    }

    @Override
    public long getLdiRowLimit() {
        return this.ldiRowLimit;
    }

    /** MPCS-7135  Moved checkedJoin() to SleepUtilities. */
    /**
     * We don't use the connection in the superclass.
     *
     * @return True if up and running
     */
    @Override
    public boolean isUp() {
        return (getStarted() && !getStopped());
    }

    @Override
    public boolean isSerializationFlush() {
        return serializationFlush.get();
    }

    @Override
    public boolean isAnyStoreActive() {
        boolean stillActive = false;
        for (final StoreIdentifier si : StoreIdentifier.values()) {
            final IStoreMonitor monitor = getStoreMonitor(si);
            if (monitor != null) {
                synchronized (monitor.getSyncMonitor()) {
                    stillActive |= monitor.isBulkLoadableAndActive();
                    if (stillActive) {
                        break;
                    }
                }
            }
        }
        return stillActive;
    }

    private class StoreShutDownDaemon extends Thread implements Runnable {
        /**
         * No-arg constructor that identifies the thread
         */
        public StoreShutDownDaemon() {
            super("Store Controller Shutdown Daemon");
        }

        /**
         * This method waits for the Store Controller to start up, and then waits
         * for all of it's bulk stores to shut-down, after which, it cleans up.
         */
        @Override
        public void run() {
            try {
                /*
                 * Cannot shut down until it has started
                 */
                while (!isStarted.get()) {
                    Thread.sleep(1000L);
                }

                /*
                 * Cannot shut down if any Bulk Loading store is still active
                 */
                while (isAnyStoreActive()) {
                    Thread.sleep(500L);
                }

                // RAP- The following code is NOT thread safe. Only one thread can
                // safely
                // signal the gatherer in the current implementation.

                // All tables are shut down, so we can flush the gatherer
                log.debug("Flushing gatherer");
                setGathererFlushing(true);

                //MPCS-7155 - If the gatherer is sleeping,
                // nothing happens until the sleep interval expires. So
                // interrupt it.
                stopGatherer();

                // Wait until the gatherer is done flushing.
                // Note that we are sure that no more records can be written because
                // all
                // of the "active" flags are clear

                while (isGathererFlushing()) {
                    log.debug("Waiting for gatherer to flush");
                    Thread.sleep(100L);
                }

                SleepUtilities.checkedJoin((Thread) getGatherer(), IDbSqlArchiveController.GATHERER_JOIN, "gatherer",
                                           log);

                /*
                 * Wait for all stores to complete idling down
                 */
                waitForIdleDownToComplete();
                if (IDbSqlArchiveController.EXTRA_DELAY) {
                    // We're done, but wait a bit more before closing the statement
                    log.debug("Extra wait");
                    try {
                        SleepUtilities.fullSleep(60L * IDbSqlArchiveController.ONE_SECOND);
                    }
                    catch (final ExcessiveInterruptException eie) {
                        log.error("Error in extra wait: " + rollUpMessages(eie));
                    }
                }

                /*
                 * Shut down all active stores' LDI Inserters
                 */
                shutDownInserters();

                isStopped.set(true);

                log.debug("Context ID:       " + contextId.getNumber());
                log.debug("Context Fragment: " + contextId.getFragment());
                log.debug("Stores closed");
            }
            catch (final Throwable t) {
                log.debug("Error shutting down Store Controller.", t);
            }
        }
    }

    @Override
    public void waitForIdleDownToComplete() {
        boolean isStillIdlingDown = true;
        while (isStillIdlingDown) {
            isStillIdlingDown = false;
            for (final StoreIdentifier si : StoreIdentifier.values()) {
                final IStoreMonitor monitor = getStoreMonitor(si);
                if (monitor == null) {
                    continue;
                }

                final IInserter inserter = monitor.getInserter();
                final int size = inserter.size();
                if (size > 0) {
                    final long mark = System.currentTimeMillis();
                    log.log(messageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                            "Waiting for idle-down to complete " + si + " database updates: " + size + " entries",
                            LogMessageType.INSERTER));
                    if (!inserter.isEmpty() && inserter.isStarted()) {
                        log.log(messageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                                si.toString() + " Database LDI Store still has " + inserter.size() + " entries",
                                LogMessageType.INSERTER));
                        SleepUtilities.checkedSleep(IDbSqlArchiveController.INSERTER_CHECK);
                        isStillIdlingDown = true;
                        break;
                    }
                    log.debug("Idle-down " + si + " finished in "
                            + (System.currentTimeMillis() - mark) / IDbSqlArchiveController.D_ONE_SECOND + " seconds");
                }
            }
        }
    }

    @Override
    public boolean isGathererFlushing() {
        return gathererFlushing.get();
    }

    /**
     * @return the applicationIsSse
     */
    @Override
    public boolean isApplicationIsSse() {
        return applicationIsSse.isApplicationSse();
    }

    @Override
    public boolean setGathererFlushing(final boolean value) {
        return gathererFlushing.getAndSet(value);
    }

    @Override
    public String getExportDirectory() {
        return ldiExportDirectory.get();
    }

    @Override
    public long getFlushTime() {
        return this.flushTime.get();
    }

    @Override
    public boolean getConfigured() {
        return this.isConfigured.get();
    }

    @Override
    public String getFileBase() {
        return fileBase.get();
    }

    @Override
    public String getFswSse() {
        return this.fswSse.get();
    }

    @Override
    public String getPid() {
        return this.pid;
    }

    @Override
    public boolean getStarted() {
        return this.isStarted.get();
    }

    @Override
    public boolean getStopped() {
        return this.isStopped.get();
    }

    @Override
    public boolean getAndSetStarted(final boolean isStarted) {
        return this.isStarted.getAndSet(isStarted);
    }

    @Override
    public String setFileBase(final String fileBase) {
        return this.fileBase.getAndSet(fileBase);
    }

    @Override
    public long setFlushTime(final long flushTime) {
        return this.flushTime.getAndSet(flushTime);
    }

    @Override
    public String setProductFields(final String fields) {
        return this.productFields.getAndSet(fields);
    }

    @Override
    public String setFswSse(final String value) {
        return this.fswSse.getAndSet(value);
    }

    @Override
    public String setExportDirectory(final String exportLDIDir) {
        return this.ldiExportDirectory.getAndSet(exportLDIDir);
    }

    @Override
    public boolean setConfigured(final boolean b) {
        return this.isConfigured.getAndSet(b);
    }

    @Override
    public void setSerializationFlush(final boolean b) {
        this.serializationFlush.set(b);
    }

    @Override
    public long getAndIncrementUnique() {
        return this.unique.getAndIncrement();
    }

    @Override
    public IGatherer getGatherer() {
        return this.gatherer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pair<Long, Boolean> getAssociatedId(final String channelId, final boolean fromSse) {
        return fromSse ? getAssociatedSseId(channelId) : getAssociatedFswId(channelId);
    }

    /**
     * If the channel id has been seen, return the associated id. Otherwise, get
     * a new id and associate it. Also specify whether or not this is a
     * newly-seen channel id.
     *
     * @param channelId
     *            Channel id
     *
     * @return Pair of id and true if this is a newly seen channel id.
     */
    private Pair<Long, Boolean> getAssociatedFswId(final String channelId) {
        Long idToUse = null;
        Boolean state = null;

        final String cid = channelId.toUpperCase();

        synchronized (fswIds) {
            final Long currentId = fswIds.get(cid);

            if (currentId != null) {
                idToUse = currentId;
                state = Boolean.FALSE;
            }
            else {
                idToUse = fswChannelValueIdCounter++;
                state = Boolean.TRUE;

                fswIds.put(cid, idToUse);
            }
        }

        return new Pair<Long, Boolean>(idToUse, state);
    }

    /**
     * Remove all entries in the table. Used by HeaderChannelValue and
     * MonitorChannelValue stores.
     */
    @Override
    public void clearFswIds() {
        clearFswIds("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearFswIds(final String... prefixes) {
        synchronized (fswIds) {
            for (final String prefix : prefixes) {
                final Iterator<String> it = fswIds.keySet().iterator();
                while (it.hasNext()) {
                    final String key = it.next();
                    if ((null == prefix) || prefix.isEmpty() || key.startsWith(prefix)) {
                        it.remove();
                    }
                }
                if (fswIds.isEmpty()) {
                    fswChannelValueIdCounter = ILDIStore.ID_START;
                }
            }
        }
    }

    /**
     * If the channel id has been seen, return the associated id. Otherwise, get
     * a new id and associate it. Also specify whether or not this is a
     * newly-seen channel id.
     *
     * @param channelId
     *            Channel id
     *
     * @return Pair of id and true if this is a newly seen channel id.
     */
    private Pair<Long, Boolean> getAssociatedSseId(final String channelId) {
        /** MPCS-5008 Now public like FSW version */

        Long idToUse = null;
        Boolean state = null;

        final String cid = channelId.toUpperCase();

        synchronized (sseIds) {
            final Long currentId = sseIds.get(cid);

            if (currentId != null) {
                idToUse = currentId;
                state = Boolean.FALSE;
            }
            else {
                idToUse = sseChannelValueIdCounter++;
                state = Boolean.TRUE;

                sseIds.put(cid, idToUse);
            }
        }

        return new Pair<Long, Boolean>(idToUse, state);
    }

    /**
     * Remove all entries in the table.
     * Used by HeaderChannelValue and MonitorChannelValue stores.
     */
    @Override
    public void clearSseIds() {
        clearSseIds("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSseIds(final String... prefixes) {
        synchronized (fswIds) {
            for (final String prefix : prefixes) {
                final Iterator<String> it = sseIds.keySet().iterator();
                while (it.hasNext()) {
                    final String key = it.next();
                    if ((null == prefix) || prefix.isEmpty() || key.startsWith(prefix)) {
                        it.remove();
                    }
                }
                if (sseIds.isEmpty()) {
                    sseChannelValueIdCounter = ILDIStore.ID_START;
                }
            }
        }
    }

}
