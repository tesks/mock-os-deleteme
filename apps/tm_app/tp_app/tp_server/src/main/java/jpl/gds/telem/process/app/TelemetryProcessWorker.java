/*
 * Copyright 2006-2018. California Institute of Technotracery.
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
package jpl.gds.telem.process.app;

import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.common.config.bootstrap.ChannelLadBootstrapConfiguration;
import jpl.gds.common.config.bootstrap.options.ChannelLadBootstrapCommandOptions;
import jpl.gds.common.service.telem.ITelemetryProcessorSummary;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.eha.api.channel.IChannelLad;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.eha.api.service.alarm.IAlarmNotifierService;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.message.api.IInternalBusPublisher;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.message.api.spill.ISpillProcessor;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.performance.PerformanceSummaryMessage;
import jpl.gds.telem.common.app.mc.rest.resources.DownlinkStatusResource;
import jpl.gds.telem.common.state.WorkerState;
import jpl.gds.telem.common.worker.AbstractTelemetryWorker;
import jpl.gds.telem.process.*;
import jpl.gds.telem.process.app.bootstrap.TelemetryProcessWorkerBootstrap;
import jpl.gds.telem.process.app.mc.rest.resources.TelemetryProcessorSummaryDelegateResource;
import jpl.gds.telem.process.server.IProcessSessionManager;
import jpl.gds.telem.process.server.event.TelemetryMessageSubscriber;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * TelemetryProcessWorker is the actual workhorse of the TelemetryProcessorApp application,
 * from which FSW and SSE instances of the telemetry processing are extended.
 * This application also manages the downlink session from start to finish.
 *
 * Added publishing of event messages
 * Buffers messsages with a Spill Processor
 *
 */
public class TelemetryProcessWorker extends AbstractTelemetryWorker implements IProcessWorker {
    /** Current Time Comparison Strategy */
    private final TimeComparisonStrategyContextFlag timeStrategy;

    /** The downlink telemetry input port number */
    protected int port = -1;
    /** Downlink source host */
    protected String host;

    private ProcessConfiguration processConfig;

    private IChannelLad channelLad;
    private IAlarmHistoryProvider alarmHistory;

    private TelemetryMessageSubscriber tlmMessageSub;
    private final IContextKey foreignContextKey;
    private int telemetryTimeout;
    private IInternalBusPublisher internalBusPublisher;

    private IQueuingMessageHandler frameHandler;
    private IQueuingMessageHandler packethandler;
    private IQueuingMessageHandler stationHandler;
    private IExternalMessageUtility messageUtil;
    private ISpillProcessor<IExternalMessage> spillProcessor;

    private PerformanceProperties perfProps;
    private DownlinkStatusResource downStatus;
    private TelemetryProcessorSummaryDelegateResource telemStatus;
    private ChannelLadBootstrapCommandOptions bootstrapOpts;

    @Override
    public BaseCommandOptions createOptions() {
        BaseCommandOptions options = super.createOptions();

        bootstrapOpts = new ChannelLadBootstrapCommandOptions(springContext.getBean(ChannelLadBootstrapConfiguration.class));
        options.addOptions(bootstrapOpts.getBootstrapCommandOptions());

        return options;

    }

    /**
     * Creates an instance of TelemetryTelemetryProcessWorker.
     * @param springContext        the Spring Application Context
     * @param timeCompStratCtxFlag the time comparison strategy
     * @param sessionConfig        the session configuration
     * @param foreignContextKey    context key from TI session start
     * @param processConfig        Process configuration properties
     * @param secureLoader The parent servers secure classloader
     */
    @SuppressWarnings("unchecked")
    public TelemetryProcessWorker(final ApplicationContext springContext,
                                  final TimeComparisonStrategyContextFlag timeCompStratCtxFlag,
                                  final SessionConfiguration sessionConfig, final IContextKey foreignContextKey,
                                  final ProcessConfiguration processConfig,
                                  final AmpcsUriPluginClassLoader secureLoader) {
        super(springContext, sessionConfig, secureLoader);
        this.timeStrategy = timeCompStratCtxFlag;
        this.foreignContextKey = foreignContextKey;
        this.sseFlag = contextConfig.getGeneralInfo().getSseContextFlag();
        this.tracer = TraceManager.getTracer(springContext, Loggers.PROCESSOR);

        this.processConfig = springContext.getBean(ProcessConfiguration.class);
        this.messageUtil = springContext.getBean(IExternalMessageUtility.class);

        this.telemetryTimeout = processConfig.getFeatureSet().getTelemetryWaitTimeout();
        int handlerQueueSize = processConfig.getFeatureSet().getMessageHandlerQueueSize();
        int spillQueueSize = processConfig.getFeatureSet().getSpillProcessorQueueSize();
        long spillTimeout = processConfig.getFeatureSet().getSpillWaitTimeout();
        this.packethandler = springContext.getBean(IQueuingMessageHandler.class, handlerQueueSize);
        this.frameHandler = springContext.getBean(IQueuingMessageHandler.class, handlerQueueSize);
        this.stationHandler = springContext.getBean(IQueuingMessageHandler.class, handlerQueueSize);

        //spill processor
        final BlockingQueue<IExternalMessage> internalMessageToSend = new ArrayBlockingQueue<>(spillQueueSize);
        //unique spill dir
        final String spillDir = contextConfig.getGeneralInfo().getOutputDir() +
                File.separator + foreignContextKey.getNumber();
        this.spillProcessor = springContext.getBean(ISpillProcessor.class,
                                                    spillDir, internalMessageToSend, spillQueueSize,
                                                    true,// isSpillEnabled
                                                    IExternalMessage.class,
                                                    contextConfig.getGeneralInfo().getRootPublicationTopic(),
                                                    false,//isKeepSpillFilesEnabled
                                                    spillTimeout, tracer, sseFlag);

        this.workerThreadNamePrefix = "TP Worker-";
        originalThreadName = Thread.currentThread().getName();

        this.perfProps = springContext.getBean(PerformanceProperties.class);
        this.downStatus = new DownlinkStatusResource(springContext);
        this.telemStatus = new TelemetryProcessorSummaryDelegateResource(this);
        this.publicationBus = springContext.getBean(IMessagePublicationBus.class);

        // Subscribe to summary messages
        this.publicationBus.subscribe(CommonMessageType.PerformanceSummary, this);
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        // Set run flags so session entries have correct config
        if (sseFlag.isApplicationSse()) {
            fswDownFlag.setFswDownlinkEnabled(false);
            sseDownFlag.setSseDownlinkEnabled(true);
        } else {
            fswDownFlag.setFswDownlinkEnabled(true);
            sseDownFlag.setSseDownlinkEnabled(false);
        }
        super.configure(commandLine);
        bootstrapOpts.parseBootstrapOptions(commandLine);

        setup();
    }

    private void setup() {
        processConfig.setUseMessageService(msgServiceConfig.getUseMessaging());

        processConfig.setUseDb(true);
        if (contextConfig.getConnectionConfiguration().getDownlinkConnection().getInputType() == null) {
            contextConfig.getConnectionConfiguration().getDownlinkConnection()
                         .setInputType(connProps.getDefaultSourceFormat(venueCfg.getVenueType(), false));
        }

        setupWorker();

        final String sessionFilterString = foreignContextKey.getNumber() + "/" + foreignContextKey.getHost() + "/%";

        // Create the session manager
        sessionManager = new ProcessSessionManager(springContext, contextConfig);
        try {
            sessionManager.startSessionDatabase();
        } catch (DatabaseException e) {
            tracer.error("The session database could not be started, see log file for more details. " ,
                         ExceptionTools.getMessage(e));
            tracer.error(Markers.SUPPRESS, "The session database could not be started: " + ExceptionTools.getMessage(e), e);

            internalChangeWorkerState(WorkerState.STOPPING);
        }

        try{
            //internal bus publisher
            this.internalBusPublisher = springContext.getBean(IInternalBusPublisher.class, spillProcessor, tracer,
                                                              publicationBus, messageUtil);

            this.tlmMessageSub = new TelemetryMessageSubscriber(frameHandler, packethandler, stationHandler,
                                                                tracer, (SessionConfiguration) contextConfig,
                                                                sessionFilterString, internalBusPublisher);
            internalChangeWorkerState(WorkerState.READY);
        }
        catch (final MessageServiceException e) {
            internalChangeWorkerState(WorkerState.STOPPED);
            tracer.error("Encountered exception setting up worker ", ExceptionTools.getMessage(e));
            tracer.error(Markers.SUPPRESS,"Encountered exception setting up worker ", ExceptionTools.getMessage(e), e);
        }
    }


    @Override
    public void exitCleanly() {
        super.exitCleanly();
        if (tlmMessageSub != null) {
            tlmMessageSub.stop();
        }
    }

    @Override
    public void stop() {
        // Before stopping, wait for the queue to be empty (all messages processed)
        // OR when an idle message timeout is reached
        while (tlmMessageSub.hasBacklog() || tlmMessageSub.getIdleTime() < telemetryTimeout) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }

        super.stop();
    }

    @Override
    public ITelemetryProcessorSummary getSessionSummary() {
        if (sessionManager == null) {
            return null;
        }
        return (ITelemetryProcessorSummary) sessionManager.getProgressSummary();
    }


    @Override
    public ISuspectChannelService getSuspectChannelService() {
        if (sessionManager != null) {
            return ((IProcessSessionManager) sessionManager).getSuspectChannelService();
        } else {
            return null;
        }
    }

    @Override
    public String getLadContentsAsString() {
        final StringWriter writer = new StringWriter();
        final boolean      ok     = this.channelLad.writeCsv(writer);
        try {
            writer.close();
        } catch (final IOException e) {
            // ignore
        }
        return ok ? writer.toString() : "";
    }

    @Override
    public boolean saveLadToFile(final String filename) {
        final Path f = Paths.get(filename);
        try {
            if (!f.toAbsolutePath().toFile().exists()) {
                final boolean ok = f.toAbsolutePath().toFile().createNewFile();
                tracer.debug("Created file '", filename, "' ? ", ok);
            }
        }
        catch (final Exception e) {
            tracer.error("Unexpected error ", ExceptionTools.getMessage(e));
            return false;
        }

        try {
            final FileWriter writer = new FileWriter(f.toAbsolutePath().toFile());
            final boolean    status = this.channelLad.writeCsv(writer);
            writer.close();

            tracer.trace("Wrote lad? ", status, "\n", getLadContentsAsString());
            return status;
        }
        catch (final Exception e) {
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
            return  ((IProcessSessionManager) sessionManager).getAlarmNotifier();
        } else {
            return null;
        }
    }


    @Override
    public ProcessConfiguration getProcessConfiguration() {
        return processConfig;
    }

    @Override
    public TimeComparisonStrategy getTimeComparisonStrategy() {
        return timeStrategy.getTimeComparisonStrategy();
    }


    @Override
    public void setTimeComparisonStrategy(TimeComparisonStrategy strategy) {
        timeStrategy.setTimeComparisonStrategy(strategy);
        // clear the LAD
        clearChannelState();
    }

    /**
     * Get internal bus publisher
     * @return internal bus publisher
     */
    IInternalBusPublisher getInternalBusPublisher() {
        return internalBusPublisher;
    }

    /*
     * Must force initialization of
     * DownlinkConfiguration. This is a test-ism. In production, the configuration
     * is loaded as a bean by the Spring publicationBus. The setDownConfig() method
     * is package protected, and can only be called from a test in the same package.
     */
    @Autowired
    @Qualifier(TelemetryProcessWorkerBootstrap.PROCESS_CONFIGURATION)
    public void setProcessConfig(final ProcessConfiguration processConfig) {
        this.processConfig = processConfig;
    }

    @Autowired
    public void setPublicationBus(final IMessagePublicationBus bus) {
        this.publicationBus = bus;
    }

    @Autowired
    public void setAlarmHistoryProvider(final IAlarmHistoryProvider ahp) {
        this.alarmHistory = ahp;
    }

    @Autowired
    public void setChannelLad(final IChannelLad cl) {
        this.channelLad = cl;
    }

    @Autowired
    public void setMessagePortal(final IMessagePortal msgPortal) {
        this.jmsPortal = msgPortal;
    }

    @Override
    public IProcessWorkerStatus getStatus() {
        return new ProcessWorkerStatus(
                hasBeenStarted(),
                tlmMessageSub.hasBacklog(),
                sessionManager.isSessionEnded() || ( (ProcessSessionManager)(sessionManager)).hasReceivedEndOfData(),
                tlmMessageSub.getIdleTime());
    }

    @Override
    public TelemetryProcessorSummaryDelegateResource getTelemStatus() {
        return telemStatus;
    }

    @Override
    public DownlinkStatusResource getPerfStatus(){
        return downStatus;
    }

    public void handleMessage(final IMessage message) {
        try {
            if (message instanceof PerformanceSummaryMessage) {
                handlePerformanceSumMessage((PerformanceSummaryMessage) message, perfProps, downStatus);
            }
        } catch (final Exception e) {
            tracer.error(ExceptionTools.getMessage(e));
        }
    }
}
