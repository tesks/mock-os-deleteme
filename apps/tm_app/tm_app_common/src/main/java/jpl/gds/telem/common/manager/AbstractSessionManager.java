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
package jpl.gds.telem.common.manager;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.common.service.telem.ITelemetryFeatureManager;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.types.IDbContextInfoFactory;
import jpl.gds.db.api.types.IDbContextInfoUpdater;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.*;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.performance.PerformanceSummaryPublisher;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.SclkScetConverter;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.telem.common.app.mc.IRestfulTelemetryApp;
import jpl.gds.telem.common.config.IDownlinkConfiguration;
import jpl.gds.telem.common.config.IDownlinkProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract class implementing the ISessionManager interface to facilitate session management for persistent services
 */
public abstract class AbstractSessionManager implements ISessionManager, MessageSubscriber {


    protected final Tracer                 log;
    protected final IContextConfiguration  contextConfig;
    protected final IMessagePublicationBus context;

    protected       IAccurateDateTime      startTime;
    protected       Timer                  heartbeatTimer;
    protected final IMessagePortal         jmsPortal;
    protected final AtomicBoolean          endOfDataReceived = new AtomicBoolean(false);

    protected final List<ITelemetryFeatureManager> allFeatureManagers = new LinkedList<>();
    protected final IDownlinkProperties         featureSet;
    protected       boolean                        ended              = true;
    protected  PerformanceSummaryPublisher    performancePublisher;
    protected final ApplicationContext             appContext;
    protected final IDbSqlArchiveController        archiveController;
    protected final IDatabaseProperties dbProps;
    protected final IMySqlAdaptationProperties mysqlProps;
    protected final IStatusMessageFactory          statusMessageFactory;
    protected final PerformanceProperties performanceProperties;
    protected final MessageServiceConfiguration msgConfig;
    protected ServiceConfiguration           serviceConfiguration;

    protected final SseContextFlag sseFlag;

    protected ITelemetrySummary summary;

    private IDbContextInfoUpdater dbContextInfoUpdater;

    /**
     * Constructor for instantiating session managers
     *
     * @param appContext the current application context
     * @param contextConfig the context configuration
     * @param log the tracer to log with
     * @param downConfig the general IDownlinkConfiguration to get enabled features from
     */
    public AbstractSessionManager(final ApplicationContext appContext, final IContextConfiguration contextConfig,
                                  final Tracer log, final IDownlinkConfiguration downConfig) {
        this.appContext = appContext;
        this.contextConfig = contextConfig;
        this.log = log;
        this.featureSet = downConfig.getFeatureSet();

        this.sseFlag = appContext.getBean(SseContextFlag.class);
        this.archiveController = appContext.getBean(IDbSqlArchiveController.class);
        this.jmsPortal = appContext.getBean(IMessagePortal.class);
        this.statusMessageFactory = appContext.getBean(IStatusMessageFactory.class);
        this.dbProps = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class);
        this.mysqlProps = appContext.getBean(IMySqlAdaptationProperties.class);
        this.performanceProperties = appContext.getBean(PerformanceProperties.class);
        this.msgConfig = appContext.getBean(MessageServiceConfiguration.class);

        this.context = appContext.getBean(IMessagePublicationBus.class);
        this.context.subscribe(CommonMessageType.EndOfData, this);

        this.dbContextInfoUpdater = appContext.getBean(IDbContextInfoFactory.class).createQueryableUpdater();
    }


    /**
     * Starts the performance summary publisher.
     *
     */
    protected synchronized void startPerformancePublisher() {
        performancePublisher = appContext.getBean(PerformanceSummaryPublisher.class);
        performancePublisher.start(performanceProperties.getSummaryInterval());
    }

    @Override
    public synchronized void stopPerformancePublisher() {
        if (performancePublisher != null) {
            performancePublisher.stop();
            performancePublisher = null;
        }
    }

    @Override
    public synchronized void startSessionDatabase() throws DatabaseException {
        try {
            startDatabase();
        }
        catch (final Exception e) {
            log.error("There was an error initializing the session database", ExceptionTools.getMessage(e));
            log.error(Markers.SUPPRESS, "There was an error initializing the session database",
                      ExceptionTools.getMessage(e), e);
            throw new DatabaseException(e);
        }
    }

    /**
     * Stops the non-session database components and connections.
     */
    protected void stopDatabase() {
        if (this.archiveController != null) {
            this.archiveController.shutDown();
        }
    }

    @Override
    public synchronized void startSession() throws ApplicationException {
        ended = false;
        try {

            // Initialize feature managers and meters
            final boolean ok = initSession();

            if (!ok) {
                shutdownSession();
            }

            // Add flag to call to control whether config is written
            IRestfulTelemetryApp.writeOutContextConfig(appContext, contextConfig, false);
        }
        catch (final Exception e) {
            log.error("There was an error initializing the session", e);
            throw new ApplicationException(e);
        }
    }



    /**
     * Retrieves the session summary object. This object is not complete until
     * an EndOfSession message has been published.
     *
     * @return SessionSummary
     */
    protected ITelemetrySummary getSummary() {
        return populateSummary(contextConfig.getContextId().getStartTime(), contextConfig.getContextId().getEndTime());
    }


    /**
     * Prepares the output directory where session output will be written,
     * including log files, session reports, products, and debug information.
     * See #{@link #initSession()}
     */
    protected void setupSessionOutputDirectory() {
        final String testPath = contextConfig.getGeneralInfo().getOutputDir();
        final File f = new File(testPath);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    @Override
    public synchronized boolean isSessionEnded() {
        return ended;
    }


    @Override
    public void handleMessage(final IMessage message) {

        // Message type was not being checked
        if (message.isType(CommonMessageType.EndOfData)) {
            synchronized (this.endOfDataReceived) {
                this.endOfDataReceived.set(true);
                this.endOfDataReceived.notifyAll();
            }
        }
    }


    /**
     * Handles loading sclk/scet file after intiializing feature managers
     * @return whether or not sclk/scet files were successfully loaded
     * see #{@link #initSession()}
     */
    protected boolean loadSclkSet() {
        boolean ok = false;
        final int scid = contextConfig.getContextId().getSpacecraftId();
        SclkScetConverter sclkScetConverter = SclkScetUtility.getConverterFromSpacecraftId(scid, log); // add new
        if (sclkScetConverter == null) {
            /*
             * If the project sclkscet file did not load try load the default one (0)
             */
            log.warn(Markers.TIME_CORR, "Could not open SCLK/SCET file " ,
                     FileUtility.createFilePathLogMessage(GdsSystemProperties.getMostLocalPath("sclkscet." + scid, sseFlag.isApplicationSse()))
                    , " (for SCID=" , scid , ") loading default sclkscet file...");
            sclkScetConverter = SclkScetUtility.getConverterFromSpacecraftId(0, log);
        }
        if (sclkScetConverter == null) {
            log.error(Markers.TIME_CORR, "Could not open SCLK/SCET file " ,
                      FileUtility.createFilePathLogMessage(GdsSystemProperties.getMostLocalPath("sclkscet." + 0, sseFlag.isApplicationSse())));
            ok = false;
        }
        else {
            log.debug(Markers.TIME_CORR, " Successfully instantiated SCLK/SCET file ",
                      sclkScetConverter.getFilename(), " for scid ", scid);
            ok = true;
        }
        return ok;
    }

    /**
     * Performs session "cleans up" upon completion. This method handles shutting down all
     * ITelemetryFeatureManager's, populating an ITelemetrySummary,
     * removing downlink and notifier services attached to each feature.
     * NOTE: This is called by #{@link #endSession(boolean)}
     * NOTE: This does not terminate or end the session, see #{@link #endSession(boolean)}
     */
    protected void shutdownSession() {

        /*
         * Feature managers are now
         * all in a list, so they can be shutdown with this loop
         * instead of one by one.
         */
        for (final ITelemetryFeatureManager fm : allFeatureManagers) {
            log.debug("Stopping feature manager " , fm);
            fm.stopAllServices();
            log.debug("Shutting down ", fm.getClass().getName());
            if (summary != null) {
                fm.populateSummary(summary);
            }
            fm.clearAllServices();
        }
    }


    /**
     * See {@link #startDatabase()}
     */
    protected synchronized void startDatabase(StoreIdentifier ... storesToStart) throws DatabaseException {
        if (mysqlProps.getUseDatabase()) {
            List<StoreIdentifier> stores = new ArrayList<>();
            stores.add(StoreIdentifier.LogMessage);

            for (StoreIdentifier storeIdentifier : storesToStart) {
                stores.add(storeIdentifier);
            }

            internalStartDb(stores);
        }
    }


    /**
     * See {@link #startDatabase()}
     */
    private synchronized void internalStartDb(List<StoreIdentifier> storesToStart) throws DatabaseException {
        try {
            for (StoreIdentifier storeIdentifier : storesToStart) {
                this.archiveController.addNeededStore(storeIdentifier);
            }

            final boolean ok = this.archiveController.startSessionStores();
            if (!ok) {
                throw new DatabaseException("Unable to start session stores ");
            }
            this.archiveController.startContextConfigStore();
            this.archiveController.startPeripheralStores();
        } catch(Exception e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * See {@link #endSession(boolean)}
     */
    protected boolean endSession() {
        boolean ok = true;
        if (ended) {
            log.debug("Session already ended in endSession");
            return true;
        }
        /*
         *  DO NOT UNDER ANY CIRCUMSTANCES CHANGE THE ORDER OF SHUTDOWN IN
         *  THIS METHOD WITHOUT PERMISSION OF THE COG-E. THIS ORDERING EXISTS FOR A REASON.
         */
        try {
            // Stop the raw input and send an end of test message
            // assuming we actually started a session
            // DO NOT stop heartbeat messages, it will break monitoring applications
            if (startTime != null) {
                stop();
            }

            // Performance publisher replaces backlog summary publisher.

            // Start sending performance summary messages more frequently
            /* Moved this BEFORE the service
             * shutdown begins, so that performance reporting is accelerated
             * during shutdown of all services.
             */
            if (performancePublisher != null) {
                /**
                 * Look up the configured intervals from the configuration.
                 */
                final int shutdownInterval = performanceProperties.getShutdownSummaryInterval();
                performancePublisher.setShutdownRate(shutdownInterval);
            }

            // Shutdown feature managers and meters
            shutdownSession();

            // Send out summary log before the log database is closed. The
            // summary has no end time but that's ok
            IPublishableLogMessage logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO,
                                                                                           getSummary().toStringNoBlanks(
                    "SESSION SUMMARY: Session ID: "), LogMessageType.RAW_INPUT_SUMMARY);
            context.publish(logm);
            log.log(logm);

            // Flush out any pending JMS messages
            if (msgConfig.getUseMessaging()) {
                logm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO, "Clearing message backlog",
                                                                        LogMessageType.PERFORMANCE);
                context.publish(logm);
                log.log(logm);
                jmsPortal.clearAllQueuedMessages();
            }

        } catch( final Exception e) {
            log.warn("There was an anomaly shutting down the session: " + e.toString(), e);
            ok = false;
        }
        return ok;
    }


    /**
     * See {@link #endSession(boolean)}
     */
    protected void cleanupSession(final boolean showSummary) {

        /*
         * Stop the performance publisher (replaces backlog summary publisher).
         */
        stopPerformancePublisher();

        /* Display the long summary info to the console. This is different than the summary message,
         * which is a log message that goes to the database.
         */
        if (showSummary) {
            log.info(Markers.INPUT_SUMMARY, getSummary());
        }
    }


    /**
     * See {@link #initSession()}
     */
    protected boolean initFeatureManagers() throws ApplicationException {
        boolean ok = false;
        for (final ITelemetryFeatureManager fm : allFeatureManagers) {
            ok = fm.init(appContext);
            if (!ok) {
                log.error("Startup of downlink service " , fm , " failed");
                throw new ApplicationException("Startup of downlink service " + fm + "failed");
            }
            else {
                log.debug("Started feature manager: " , fm);
            }
        }
        return ok;
    }

    /**
     * Update sessions ID into a context in DB
     */
    @Override
    public void updateContextDbSessionsList(final String updatedSessionIds) throws ApplicationException {
        //save in database
        dbContextInfoUpdater.addSessionKey(contextConfig.getContextId().getContextKey().getParentNumber());

        try {
            archiveController.getContextConfigStore().updateSessionId(dbContextInfoUpdater, updatedSessionIds);
        } catch (DatabaseException e) {
            throw new ApplicationException(ExceptionTools.getMessage(e), e.getCause());
        }
    }

    @Autowired
    public void setServiceConfiguration(ServiceConfiguration svcConfig) {
        this.serviceConfiguration = svcConfig;
    }


}
