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
package jpl.gds.telem.process;

import jpl.gds.common.service.telem.ITelemetryFeatureManager;
import jpl.gds.common.service.telem.ITelemetryProcessorSummary;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.common.service.telem.TelemetryProcessorSummary;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.eha.api.feature.IEhaFeatureManager;
import jpl.gds.eha.api.service.alarm.IAlarmNotifierService;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.shared.email.EmailCenter;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.*;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.telem.common.feature.*;
import jpl.gds.telem.common.manager.AbstractSessionManager;
import jpl.gds.telem.process.server.IProcessSessionManager;
import org.springframework.context.ApplicationContext;

/**
 * ProcessSessionManager handles the setup, execution, and tear down of a downlink processing session. It is responsible
 * for starting the database stores, the session heartbeat, all of the telemetry processing features required by the
 * downlink configuration, the frame, packet, and product meters, and the performance summary publisher. Then it is
 * responsible for starting the processing of the telemetry input stream. When the session is terminated, it stops all
 * of the publishers, features, and data stores it started.
 *
 */
public class ProcessSessionManager extends AbstractSessionManager implements IProcessSessionManager {
    private final ProcessConfiguration processConfig;
    private final IProcessProperties   processProps;

    private       IEhaFeatureManager         ehaManager;

    private final boolean isEnableAnyHeaderChannelizer;
    private final boolean isEnableFrameHeaderChannelizer;
    private final boolean isEnablePacketHeaderChannelizer;
    private final boolean isEnableSfduHeaderChannelizer;
    private final boolean isEnableEvrDecom;
    private final boolean isEnablePduExtract;
    private final boolean isEnablePreChannelizedDecom;
    private final boolean isEnableGenericChannelDecom;
    private final boolean isEnableGenericEvrDecom;
    private final boolean isEnableAlarms;
    private final boolean isEnableProductGen;

    /**
     * Creates an instance of ProcessSessionManager.
     *
     * @param inputContext      the current application context
     * @param contextConfig     the IContextConfiguration object configured by the user's input
     */
    public ProcessSessionManager(final ApplicationContext inputContext, final IContextConfiguration contextConfig) {
        super(inputContext, contextConfig, TraceManager.getTracer(inputContext, Loggers.PROCESSOR),
              inputContext.getBean(ProcessConfiguration.class));

        this.processConfig = inputContext.getBean(ProcessConfiguration.class);
        this.processProps = ((IProcessProperties) featureSet);

        this.isEnableAnyHeaderChannelizer = processProps.isEnableAnyHeaderChannelizer();
        this.isEnableFrameHeaderChannelizer = processProps.isEnableFrameHeaderChannelizer();
        this.isEnablePacketHeaderChannelizer = processProps.isEnablePacketHeaderChannelizer();
        this.isEnableSfduHeaderChannelizer = processProps.isEnableSfduHeaderChannelizer();
        this.isEnableEvrDecom = processProps.isEnableEvrDecom();
        this.isEnablePduExtract = processProps.isEnablePduExtract();
        this.isEnablePreChannelizedDecom = processProps.isEnablePreChannelizedDecom();
        this.isEnableGenericChannelDecom = processProps.isEnableGenericChannelDecom();
        this.isEnableGenericEvrDecom = processProps.isEnableGenericEvrDecom();
        this.isEnableAlarms = processProps.isEnableAlarms();
        this.isEnableProductGen = processProps.isEnableProductGen();
    }

    @Override
    public boolean initSession() throws ApplicationException {
        /*
         * Feature managers are now
         * created as local variables and added to a list of
         * feature managers which is a member variable.
         */
        // Create and enable/disable feature managers based upon the feature set
        // for this session
        final HeaderChannelizationFeatureManager headerChannelManager = new HeaderChannelizationFeatureManager();
        allFeatureManagers.add(headerChannelManager);
        final ITelemetryFeatureManager evrManager = new EvrFeatureManager();
        allFeatureManagers.add(evrManager);
        final ITelemetryFeatureManager pduManager = new PduExtractionFeatureManager();
        allFeatureManagers.add(pduManager);
        final PacketFeatureManager packetManager = new PacketFeatureManager();
        allFeatureManagers.add(packetManager);

        // add packet manager for tracking statistics
        packetManager.setEnablePacketExtract(false);
        packetManager.setEnablePacketTracking(true);
        packetManager.enable(true);

        /*
         * Do not add EHA manager to the list of feature
         * managers here. It must be done last.
         */
        ehaManager = appContext.getBean(IEhaFeatureManager.class);

        final ITelemetryFeatureManager productGenManager = new ProductGeneratorFeatureManager();
        allFeatureManagers.add(productGenManager);

        headerChannelManager.enable(isEnableAnyHeaderChannelizer);
        headerChannelManager.setFrameHeaderChannelsEnabled(isEnableFrameHeaderChannelizer);
        headerChannelManager.setPacketHeaderChannelsEnabled(isEnablePacketHeaderChannelizer);

        /**
         * Set SFDU header channelization flag into the
         * header channelization feature manager.
         */
        headerChannelManager.setSfduHeaderChannelsEnabled(isEnableSfduHeaderChannelizer);
        evrManager.enable(isEnableEvrDecom);
        pduManager.enable(isEnablePduExtract);
        ehaManager.enable(isEnablePreChannelizedDecom);
        ehaManager.enableGenericDecom(isEnableGenericChannelDecom);
        ehaManager.enableGenericEvrDecom(isEnableGenericEvrDecom);
        ehaManager.enableAlarmProcessing(isEnableAlarms);
        productGenManager.enable(isEnableProductGen && !sseFlag.isApplicationSse());

        // Starting with AMPCS R3, we now allow additional feature managers that
        // can simply be configured (need arose from supporting de-ssl's NEN/SN
        // interface, or directive issuance).

        /**
         * check if the monitor needs to be loaded if the NEN or DSN misc
         * features are set up and enabled.
         */
        boolean enableMonitorDict = false;

        if (featureSet.getMiscFeatures() != null) {
            // Begin factory-style instantiations of misc features.
            for (final String miscFeatureClassName : featureSet.getMiscFeatures()) {
                Class<?> c = null;
                ITelemetryFeatureManager dfm = null;

                /*
                 * Added exception to the error
                 * log messages below. Error is unanticipated and cannot be caused by
                 * any incorrect user action. The exception should be dumped.
                 */
                try {
                    c = Class.forName(miscFeatureClassName);
                    dfm = (ITelemetryFeatureManager) c.newInstance();
                    log.trace("Successfully instantiated process feature " , miscFeatureClassName);

                    /**
                     * check if the monitor needs to be loaded if the NEN or DSN misc
                     * features are set up and enabled.
                     */
                    enableMonitorDict = enableMonitorDict || dfm instanceof NenStatusDecomFeatureManager
                            || dfm instanceof DsnMonitorChannelizationFeatureManager;

                }
                catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    log.error("Class " ,
                              miscFeatureClassName , " could not be instantiated: " , ExceptionTools.getMessage(e));
                    log.error(Markers.SUPPRESS,
                              "Class " ,
                              miscFeatureClassName , " could not be instantiated: " , ExceptionTools.getMessage(e),
                              e);
                    continue;

                }
                allFeatureManagers.add(dfm);
                if (dfm instanceof RecordedEngineeringFeatureManager) {
                    dfm.enable(featureSet.isEnableMiscFeatures() && productGenManager.isEnabled());
                } else {
                    dfm.enable(featureSet.isEnableMiscFeatures());
                }
            }
        }

        /**
         * Now add the EHA feature manager. This
         * must be added after all the feature managers that may load channel
         * dictionaries, because this manager loads the alarm dictionary.
         *
         * @TODO IT IS LESS THAN DESIREABLE THAT THE ALARM CAPABILITY DOES NOT HAVE
         *       ITS OWN FEATURE MANAGER. If it was separate, we could just make
         *       sure that one was added last.
         *
         *       Except that it must be loaded BEFORE configured NSYT managers.
         */
        allFeatureManagers.add(ehaManager);

        /**
         * Enable all required dictionaries. This should enable
         * everything that could be used and the feature managers will be responsible for loading
         * the required dictionaries. Using the isSse property and the feature managers to enable and load
         * all the required dictionaries for the downlink up front before anything is initialized.
         *
         * Note that the monitor enabling is not done here since the dsn channelizing is a misc feature.
         *
         * TODO right now there are two load strategies, one for flight one for SSE. Maybe these should
         * be merged at some point.
         */
        try {
            final boolean isSse = sseFlag.isApplicationSse();
            log.debug(getClass().getName(), " isSse ?", isSse);
            if (isSse) {
                /* Enable alarm and decom dictionary. SSE users are people too. */
                appContext.getBean(SseDictionaryLoadingStrategy.class).enableApid()
                          .setHeader(headerChannelManager.isEnabled()).setEvr(evrManager.isEnabled())
                          .setChannel(ehaManager.isEnabled()).setAlarm(ehaManager.isEnabled())
                          .setDecom(ehaManager.isEnabled()).loadAllEnabled(appContext, false);
            }
            else {
                // TI/TP: Opcode diff failures against chill_down Queries
                // Need to load the command dictionary so that the Opcode -> Command stem mapping gets done
                // Using the same approach as chill_down (DownlinkSessionManager) which is based on the
                // Evr Manager enabled/disabled state
                appContext.getBean(FlightDictionaryLoadingStrategy.class).enableApid()
                          .setHeader(headerChannelManager.isEnabled()).setEvr(evrManager.isEnabled())
                          .setCommand(evrManager.isEnabled())
                          .setSequence(evrManager.isEnabled()).setChannel(ehaManager.isEnabled())
                          .setAlarm(ehaManager.isEnabled()).setDecom(ehaManager.isEnabled())
                          .setMonitor(enableMonitorDict).setProduct(productGenManager.isEnabled())
                          .setFrame(isEnablePduExtract)
                          .loadAllEnabled(appContext, false, true);
            }
        }
        catch (final Exception e) {
            throw new ApplicationException("Failed to load all required dictionaries", e);
        }

        // Create the session output directory. Products, reports, SCMFs,
        // and debug files will go in this directory
        setupSessionOutputDirectory();

        // Prepare the object that will be populated with session statistics
        setupSessionSummary();

        /*
         * Start the performance publisher
         * (replaces backlog summary publisher).
         */
        startPerformancePublisher();

        /*
         * Feature managers are now
         * created all on one list, so they can be started by this
         * loop instead of one by one.
         */
        // Now initialize all the feature managers
        boolean ok = initFeatureManagers();

        /*
         * SCLK/SCET now initialized at start of session and version logged.
         * Failure to load SCLK/SCET file is now FATAL!
         */
        if (ok) {
            ok = loadSclkSet();
        }

        // Ensure the following bean gets created, we have transfer frames.
        try {
            appContext.getBean(ITransferFrameDefinitionProvider.class);
        }
        catch (final Exception e1) {
            log.error("Unable to start raw input because could not load transfer frame dicitonary", e1);
            return false;
        }

        return ok;
    }

    @Override
    public boolean processInput() {
        // Wait for end of data, or an interrupt, to return control to the caller.
        try {
            synchronized (this.endOfDataReceived) {
                while (!this.endOfDataReceived.get()) {
                    this.endOfDataReceived.wait();
                }
                log.debug("End of data received.");
            }
        }
        catch (final InterruptedException e) {
            synchronized (this.endOfDataReceived) {
                this.endOfDataReceived.set(true);
            }
            log.debug("Input processing has been interrupted.");
            Thread.currentThread().interrupt();
        }

        return true;
    }

    @Override
    public synchronized boolean endSession(final boolean showSummary) {
        boolean ok = endSession();


        /**  Email shutdown here from endSession() */
        EmailCenter.closeAll();
        if (ok) {
            try {
                // Flush out and close the peripheral database connections
                if (dbProps.getUseDatabase()) {
                    IPublishableLogMessage logm = statusMessageFactory
                            .createPublishableLogMessage(TraceSeverity.INFO, "Clearing database backlog",
                                                         LogMessageType.PERFORMANCE);
                    context.publish(logm);
                    log.log(logm);
                    stopPeripheralDatabases();
                }

                if (dbProps.getUseDatabase()) {
                    archiveController.updateSessionEndTime(contextConfig,
                                                           summary == null ? new TelemetryProcessorSummary() : summary);
                    IPublishableLogMessage logm = statusMessageFactory
                            .createPublishableLogMessage(TraceSeverity.INFO, "Shutting down remaining database stores.",
                                                         LogMessageType.PERFORMANCE);
                    context.publish(logm);
                    log.log(logm);
                    stopDatabase();
                }

                cleanupSession(showSummary);

            }
            catch (final Exception e) {
                log.warn("There was an anomaly shutting down the session: " + e.toString(), e);
                ok = false;
            }
        }
        ended = true;
        return ok;
    }

    @Override
    public void stop() {
        synchronized (this.endOfDataReceived) {
            this.endOfDataReceived.getAndSet(true);
            this.endOfDataReceived.notifyAll();
        }
    }


    /**
     * Sets up the session summary object, which will eventually be populated
     * with statistics.
     * See {@link #initSession()}
     */
    protected void setupSessionSummary() {
        if (summary == null) {
            summary = new TelemetryProcessorSummary();
            summary.setFullName(contextConfig.getContextId().getFullName());
            summary.setOutputDirectory(contextConfig.getGeneralInfo().getOutputDir());
        }
    }

    @Override
    public ITelemetrySummary populateSummary(final IAccurateDateTime startTime,
                                                       final IAccurateDateTime endTime) {
        if (summary == null) {
            summary = new TelemetryProcessorSummary();
        }
        summary.populateBasicSummary(startTime, endTime, contextConfig.getContextId().getFullName(),
                                     contextConfig.getGeneralInfo().getOutputDir(),
                                     contextConfig.getContextId().getNumber() != null ?
                                             contextConfig.getContextId().getNumber() :
                                             null);

        if (contextConfig.getContextId().getNumber() != null) {
            summary.setContextKey(contextConfig.getContextId().getNumber());
        }

        return summary;
    }

    @Override
    public ITelemetryProcessorSummary getProgressSummary() {
        final TelemetryProcessorSummary summary = (TelemetryProcessorSummary) getSummary();
        for (final ITelemetryFeatureManager fm : allFeatureManagers) {
            fm.populateSummary(summary);
        }
        return summary;
    }

    @Override
    public synchronized void startDatabase() throws DatabaseException {
        // needed stores: Product, HeaderChannelAggregate, MonitorChannelAggregate,
        // ChannelAggregate / SseChannelAggregate, Evr / SseEvr
        if (sseFlag.isApplicationSse()) {
            startDatabase(StoreIdentifier.Product, StoreIdentifier.HeaderChannelAggregate,
                          StoreIdentifier.MonitorChannelAggregate,
                          StoreIdentifier.SseChannelAggregate, StoreIdentifier.SseEvr);
        } else {
            startDatabase(StoreIdentifier.Product, StoreIdentifier.HeaderChannelAggregate,
                          StoreIdentifier.MonitorChannelAggregate,
                          StoreIdentifier.ChannelAggregate, StoreIdentifier.Evr);
        }
    }

    /**
     * Stops the non-session database components and connections.
     */
    private void stopPeripheralDatabases() {
        if (this.archiveController != null) {
            this.archiveController.stopPeripheralStores();
        }
    }

    /**
     * Gets the SuspectChannelTable object from the EHA feature manager in this downlink session manager object
     *
     * @return SuspectChannelTable, or null if none has been initialized
     */
    @Override
    public ISuspectChannelService getSuspectChannelService() {
        if (ehaManager != null) {
            return ehaManager.getSuspectChannelService();
        }
        else {
            return null;
        }
    }

    /*
     * 1/30/14 - MPCS -5736: Need AlarmNotifierService access in chill_down
     * GUI
     */

    /**
     * Gets the AlarmNotifierService object from the EHA feature manager in this downlink session manager object
     *
     * @return AlarmNotifierService, or null if none has been initialized
     */
    @Override
    public IAlarmNotifierService getAlarmNotifier() {
        return ehaManager != null ? ehaManager.getAlarmNotifier() : null;
    }

    @Override
    public boolean hasReceivedEndOfData() {
        return endOfDataReceived.get();
    }
}
