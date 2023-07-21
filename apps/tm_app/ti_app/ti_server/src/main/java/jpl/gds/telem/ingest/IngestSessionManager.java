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
package jpl.gds.telem.ingest;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.service.telem.ITelemetryFeatureManager;
import jpl.gds.common.service.telem.ITelemetryIngestorSummary;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.common.service.telem.TelemetryIngestorSummary;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.message.EndOfSessionMessage;
import jpl.gds.session.message.SessionHeartbeatMessage;
import jpl.gds.session.message.StartOfSessionMessage;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.*;
import jpl.gds.shared.message.*;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.telem.common.feature.FrameFeatureManager;
import jpl.gds.telem.common.feature.PacketFeatureManager;
import jpl.gds.telem.common.feature.TimeCorrelationFeatureManager;
import jpl.gds.telem.common.manager.AbstractSessionManager;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.TmInputMessageType;
import jpl.gds.telem.input.api.message.ITelemetrySummaryMessage;
import jpl.gds.telem.input.api.service.ITelemetryInputService;
import jpl.gds.tm.service.api.TmServiceMessageType;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * IngestSessionManager handles the setup, execution, and tear down of a
 * downlink processing session. It is responsible for starting the database
 * stores, the session heartbeat, all of the telemetry processing features
 * required by the downlink configuration, the frame, packet, and product
 * meters, and the performance summary publisher. Then it is responsible for
 * starting the processing of the telemetry input stream. When the session is
 * terminated, it stops all of the publishers, features, and data stores it
 * started.
 *
 */
public class IngestSessionManager extends AbstractSessionManager {
    private final IngestConfiguration ingestConfig;
    private final IIngestProperties ingestProps;
    private final GeneralProperties generalProperties;

    private final long meterInterval;
    private ITelemetryInputService rawInputHandler;

    private final boolean isEnableFrameSync;
    private final boolean isEnablePacketExtract;
    private final boolean isEnableTimeCorr;

    // New flags for status/state
    private boolean isConnected;
    private boolean isFlowing;
    private boolean isInSync;


    /**
     * Creates an instance of IngestSessionManager.
     *
     * @param inputContext      the current application context
     * @param contextConfig     the IContextConfiguration object configured by the user's
     *                          input
     */
    public IngestSessionManager(final ApplicationContext inputContext, final IContextConfiguration contextConfig) {
        super(inputContext, contextConfig, TraceManager.getTracer(inputContext, Loggers.INGEST),
              inputContext.getBean(IngestConfiguration.class));
        this.ingestConfig = inputContext.getBean(IngestConfiguration.class);
        this.meterInterval = ingestConfig.getMeterInterval();
        this.generalProperties = inputContext.getBean(GeneralProperties.class);

        this.ingestProps = ((IIngestProperties) featureSet);
        this.isEnableFrameSync = ingestProps.isEnableFrameSync();
        this.isEnablePacketExtract = ingestProps.isEnableFrameSync();
        this.isEnableTimeCorr = ingestProps.isEnableTimeCorr();

        // Subscribe for the following message types so we can configure
        // the state of connected, flowing and in sync flags
        this.context.subscribe(TmInputMessageType.TelemetryInputSummary, this);
        this.context.subscribe(CommonMessageType.StartOfData, this);
        this.context.subscribe(CommonMessageType.EndOfData, this);
        this.context.subscribe(TmServiceMessageType.InSync, this);
        this.context.subscribe(TmServiceMessageType.LossOfSync, this);
    }

    @Override
    public void handleMessage(final IMessage message) {
        super.handleMessage(message);

        if (message instanceof ITelemetrySummaryMessage) {
            final ITelemetrySummaryMessage msg = (ITelemetrySummaryMessage)message;
            isConnected = msg.isConnected();
            isFlowing = msg.isFlowing();
        }

        // Set In Sync to true when we receive InSync
        if (message.isType(TmServiceMessageType.InSync)) {
            isInSync = true;
        }

        // Set In Sync to false when we receive LossOfSync
        if (message.isType(TmServiceMessageType.LossOfSync)) {
            isInSync = false;
        }

        // Set flowing to true when we receive StartOfData
        if (message.isType(CommonMessageType.StartOfData)) {
            isFlowing = true;
        }

        // Set flowing to false when we receive EndOfData
        // and since data is no longer flowing, In Sync
        // should be set to false as well.
        if (message.isType(CommonMessageType.EndOfData)) {
            isFlowing = false;
            isInSync = false;
        }

        // If we don't have connection then both
        // flowing and In Sync should be false
        if (!isConnected) {
            isFlowing = false;
            isInSync = false;
        }

        // If data is not flowing we can't be In Sync
        if (!isFlowing) {
            isInSync = false;
        }

    }

    @Override
    public boolean initSession() throws ApplicationException {
        final TelemetryInputType inputType = contextConfig.getConnectionConfiguration().getDownlinkConnection()
                                                          .getInputType();

        if (inputType.needsFrameSync() && !isEnableFrameSync) {
            log.warn("The input data type requires framesync but it is not enabled in your configuration; no telemetry will be processed");
        }

        if (inputType.needsPacketExtract() && !isEnablePacketExtract) {
            log.warn("The input data type requires packet extraction but it is not enabled in your configuration; no packets will be processed");
        }

        /*
         * Feature managers are now
         * created as local variables and added to a list of
         * feature managers which is a member variable.
         */
        // Create and enable/disable feature managers based upon the feature set
        // for this session
        final FrameFeatureManager frameManager = new FrameFeatureManager();
        allFeatureManagers.add(frameManager);
        final PacketFeatureManager packetManager = new PacketFeatureManager();
        allFeatureManagers.add(packetManager);

        // Moved time correlation feature here
        final ITelemetryFeatureManager timeCorrManager = new TimeCorrelationFeatureManager();
        allFeatureManagers.add(timeCorrManager);
        timeCorrManager.enable(isEnableTimeCorr && inputType.hasFrames());

        frameManager.setEnableFrameSync(isEnableFrameSync && inputType.needsFrameSync());
        frameManager.setEnableFrameTracking(inputType.hasFrames());
        frameManager.enable(frameManager.isEnableFrameSync() || frameManager.isEnableFrameTracking());

        packetManager.setEnablePacketExtract(isEnablePacketExtract && inputType.needsPacketExtract());
        packetManager.setEnablePacketTracking(true);
        packetManager.enable(packetManager.isEnablePacketExtract() || packetManager.isEnablePacketTracking());

        // Starting with AMPCS R3, we now allow additional feature managers that
        // can simply be configured (need arose from supporting de-ssl's NEN/SN
        // interface, or directive issuance).

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
                    log.trace("Successfully instantiated ingest feature " , miscFeatureClassName);

                    /**
                     * check if the monitor needs to be loaded if the NEN or DSN misc
                     * features are set up and enabled.
                     */

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
                dfm.enable(featureSet.isEnableMiscFeatures());
            }
        }

        /**
         * Now add the EHA feature manager. This
         * must be added after all the feature managers that may load channel
         * dictionaries, because this manager loads the alarm dictionary.
         * TODO IT IS LESS THAN DESIREABLE THAT THE ALARM CAPABILITY DOES NOT HAVE
         * ITS OWN FEATURE MANAGER. If it was separate, we could just make
         * sure that one was added last.
         *
         */

        /**
         *  Enable all required dictionaries.  This should enable
         * everything that could be used and the feature managers will be responsible for loading
         * the required dictionaries.  Using the isSse property and the feature managers to enable and load
         * all the required dictionaries for the downlink up front before anything is initialized.
         *
         * Note that the monitor enabling is not done here since the dsn channelizing is a misc feature.
         *
         * TODO right now there are two load strategies, one for flight one for SSE.  Maybe these should
         * be merged at some point.
         */
        try {
            log.debug(getClass().getName(), " isSse ? ", sseFlag.isApplicationSse());
            if (sseFlag.isApplicationSse()) {
                /* Enable alarm and decom dictionary. SSE users are people too. */
                appContext.getBean(SseDictionaryLoadingStrategy.class).enableApid()

                          .loadAllEnabled(appContext, false);
            }
            else {
                appContext.getBean(FlightDictionaryLoadingStrategy.class).enableApid()
                          .setFrame(frameManager.isEnabled())

                          .loadAllEnabled(appContext, false, true);
            }
        }
        catch (final Exception e) {
            throw new ApplicationException("Failed to load all required dictionaries", e);
        }

        // Create the session output directory. Products, reports, SCMFs, log,
        // and
        // debug files will go in this directory
        setupSessionOutputDirectory();


        // Prepare the object that will be populated with session statistics
        setupSessionSummary();

        /*
         * Start the performance publisher (replaces
         * backlog summary publisher).
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

        // Now start up the input adapter that will read the telemetry
        if (ok) {
            ok = startRawInput();
        }
        return ok;
    }

    /**
     * Sets up the telemetry input service for reading the downlink data stream.
     *
     * @return true if the input service is successfully initialized
     */
    public boolean startRawInput() {
        log.trace("Instantiating telemetry input service");
        final TelemetryInputType inputType = appContext.getBean(IConnectionMap.class).getDownlinkConnection()
                                                       .getInputType();

        if (inputType.hasFrames()) {
            log.trace("Input type has frames is true, instantiating ITransferFrameDefinitionProvider");
            try {
                appContext.getBean(ITransferFrameDefinitionProvider.class);
            }
            catch (final Exception e1) {
                log.error("Unable to start raw input because could not load transfer frame dicitonary", e1);
                return false;
            }
        }

        rawInputHandler = appContext.getBean(ITelemetryInputService.class);
        if (rawInputHandler.startService()) {
            rawInputHandler.setMeterInterval(meterInterval);
            log.debug("TelemetryInputService for ", inputType, " successfully created. Using handler ",
                      rawInputHandler.getClass().getName(), " with interval=", meterInterval);
            return true;
        }
        else {
            log.debug("Failed to start Telemetry input service!!");
            return false;
        }

    }

    @Override
    public synchronized boolean processInput() throws RawInputException {
        //  Add checks for ended and null handler

        if (ended) {
            log.error("Begin reading raw input after end");
            return false;
        }

        if (rawInputHandler == null) {
            log.error("No raw input handler");
            return false;
        }

        try {
            log.debug("Begin reading telemetry input");
            if (rawInputHandler.connect()) {
                rawInputHandler.startReading();

                while (!this.endOfDataReceived.get()) {
                    synchronized (this.endOfDataReceived) {
                        //TODO fix
                        this.endOfDataReceived.wait();
                    }
                }
                rawInputHandler.stopReading();
                // removed this rawInputHandler disconnect call.
                log.debug("Stopped reading telemetry input");
            }
        }
        catch (final RawInputException rie) {
            log.error("Could not read from raw input: " + rie.getMessage());
            log.error("There was a problem processing the input data or accessing the input source");

            /* Throw raw input exception to
             * capture that specific error code */
            throw rie;
        }
        catch (final Exception e) {
            /* Added general catch so we won't throw all the way out and freeze the app. */
            log.error("Unexpected processing error (" + e.toString() + ")", e);
            return false;
        }

        return true;
    }

    @Override
    public synchronized boolean endSession(final boolean showSummary) {
        boolean ok = endSession();
        if (ok) {
            try {
                // Flush out and close the non-session database connections
                if (dbProps.getUseDatabase()) {
                    IPublishableLogMessage logm = statusMessageFactory
                            .createPublishableLogMessage(TraceSeverity.INFO, "Clearing database backlog",
                                                         LogMessageType.PERFORMANCE);
                    context.publish(logm);
                    log.log(logm);
                    stopDatabase();
                }
                cleanupSession(showSummary);

                /*
                 * Send the end of test message and tell the message portal to flush it.
                 */
                if (startTime != null) {
                    sendEndOfSessionMessage(startTime, setSessionEndTime());

                    if (msgConfig.getUseMessaging()) {
                        jmsPortal.clearAllQueuedMessages();
                    }
                }
                stopHeartbeat();
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
        if (this.rawInputHandler != null) {
            log.trace("Stopping Raw Input Handler");
            this.rawInputHandler.stopService();

        }
    }

    /**
     * Clear the buffer within the telemetry input service InputStream
     *
     * @throws IOException if the <code>clearBufferCallable</code> threw an exception,
     *                     was interrupted while waiting, or could not be scheduled for
     *                     execution
     */
    public void clearInputStreamBuffer() throws IOException {
        if (this.rawInputHandler != null) {
            this.rawInputHandler.clearInputStreamBuffer();
        }
        else {
            throw new IllegalStateException(
                    "IngestSessionManager: RawInputHandler is null at this time. No data to be cleared.");
        }
        log.trace("Cleared Raw Input Handler buffer");
    }

    @Override
    public synchronized void startDatabase() throws DatabaseException {
        if (sseFlag.isApplicationSse()) {
            startDatabase(StoreIdentifier.SsePacket);
        } else {

            startDatabase(StoreIdentifier.Frame, StoreIdentifier.Packet);
        }
    }

    /**
     * See {@link #initSession()}
     */
    protected void setupSessionSummary() {
        if (summary == null) {
            summary = new TelemetryIngestorSummary();
            summary.setFullName(contextConfig.getContextId().getFullName());
            summary.setOutputDirectory(contextConfig.getGeneralInfo().getOutputDir());
        }
    }

    @Override
    public ITelemetrySummary populateSummary(final IAccurateDateTime startTime,
                                                       final IAccurateDateTime endTime) {
        if (summary == null) {
            summary = new TelemetryIngestorSummary();
        }
        summary.populateBasicSummary(startTime, endTime, contextConfig.getContextId().getFullName(),
                                     contextConfig.getGeneralInfo().getOutputDir(),
                                     contextConfig.getContextId().getNumber() != null ?
                                             contextConfig.getContextId().getNumber().longValue() :
                                             null);

        return summary;
    }

    @Override
    public ITelemetryIngestorSummary getProgressSummary() {
        TelemetryIngestorSummary summary = (TelemetryIngestorSummary) getSummary();
        for (ITelemetryFeatureManager fm : allFeatureManagers) {
            fm.populateSummary(summary);
        }
        return summary;
    }

    @Override
    public synchronized void startSession() throws ApplicationException {
        super.startSession();
        startHeartbeat(generalProperties.getContextHeartbeatInterval());

        // Send the session start message
        startTime = sendStartOfSessionMessage();
    }



    /**
     * Starts the session heartbeat timer. It is started for any standalone
     * downlink instance, or if running integrated and this is not the SSE
     * downlink instance.
     *
     * @param heartbeatInterval the interval between heartbeats in milliseconds
     */
    private void startHeartbeat(final long heartbeatInterval) {
        if (!contextConfig.getSseContextFlag() || (!((SessionConfiguration) contextConfig).getRunFsw().isFswDownlinkEnabled())) {
            log.info("Starting session heartbeat on a ", heartbeatInterval, " milisecond interval");
            /* Name the timer thread */
            heartbeatTimer = new Timer("Session Heartbeat Timer");
            heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sendHeartbeatMessage();
                }
            }, heartbeatInterval, heartbeatInterval);
        }
    }


    /**
     * Stops the session heartbeat.
     */
    private void stopHeartbeat() {
        log.info("Stopping heartbeat");
        if (heartbeatTimer != null) {
            log.trace("Stopping session heartbeat");
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }


    /**
     * Publishes a session heartbeat message.
     */
    private void sendHeartbeatMessage() {
        final SessionHeartbeatMessage heartbeat = new SessionHeartbeatMessage(contextConfig);
        heartbeat.setServiceConfiguration(serviceConfiguration);
        log.trace(heartbeat , ": (" , contextConfig.getContextId().getName() , ")");
        context.publish(heartbeat);
    }

    /**
     * Publishes a start of session message.
     *
     * @return the session start time
     */
    private IAccurateDateTime sendStartOfSessionMessage() {
        final IAccurateDateTime localStartTime = contextConfig.getContextId().getStartTime();
        final StartOfSessionMessage start = new StartOfSessionMessage(contextConfig);

        /*
         * Added service configuration to start of session message.
         */
        start.setServiceConfiguration(serviceConfiguration);

        // Add pid, station, and VCID

        final Integer vcid = contextConfig.getFilterInformation().getVcid();

        log.info(Markers.SESSION, "Start of Session [key=", contextConfig.getContextId().getNumber(), " javaVmPid=",
                 GdsSystemProperties.getJavaVmPid(), " dssId=", contextConfig.getFilterInformation().getDssId(),
                 " vcid=", ((vcid != null) ? vcid : "NOT APPLICABLE"), " start=",
                 TimeUtility.getFormatter().format(localStartTime), "] (", contextConfig.getContextId().getFullName(),
                 ")");

        context.publish(start);
        log.info(Markers.SESSION, start.getOneLineSummary());

        log.trace("Start of session message sent");
        return (localStartTime);
    }

    /**
     * Publishes an end of session message.
     *
     * @param startTime the session start time, to include in the end message
     * @return the end of session message
     */
    private EndOfSessionMessage sendEndOfSessionMessage(final IAccurateDateTime startTime,
                                                          final IAccurateDateTime endTime) {

        contextConfig.getContextId().setStartTime(startTime);
        contextConfig.getContextId().setEndTime(endTime);
        final EndOfSessionMessage end = new EndOfSessionMessage(contextConfig.getContextId(), getSummary());
        context.publish(end);
        log.info(Markers.SESSION, end.getOneLineSummary());
        return end;
    }

    /**
     * Sets the session end time
     *
     * @return IAccurateDateTime end of session time
     */
    private IAccurateDateTime setSessionEndTime() {
        IAccurateDateTime endTime = null;
        // started a session
        if (startTime != null) {
            endTime = new AccurateDateTime();

            contextConfig.getContextId().setEndTime(endTime);

            // Populate summary including end time
            getSummary();
        }
        return endTime;
    }

    /**
     * Gets whether or not telemetry is flowing
     *
     * @return true if telemetry is flowing; false otherwise
     */
    public boolean isFlowing() {
        return isFlowing;
    }

    /**
     * Gets whether or not the session is connected to a data source
     *
     * @return true if connected; false otherwise
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Gets whether or not frame sync has been acquired
     *
     * @return true if In Sync, false otherwise
     */
    public boolean isInSync() { return isInSync; }
}


