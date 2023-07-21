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

package jpl.gds.telem.ingest.app;

import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.options.ConnectionCommandOptions;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.context.api.EnableFswDownlinkContextFlag;
import jpl.gds.context.api.EnableSseDownlinkContextFlag;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.options.GladClientCommandOptions;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.options.SessionCommandOptions;
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
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.metadata.context.ContextConfigurationType;
import jpl.gds.shared.performance.PerformanceSummaryMessage;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.app.mc.rest.resources.DownlinkStatusResource;
import jpl.gds.telem.common.state.WorkerState;
import jpl.gds.telem.common.worker.AbstractTelemetryWorker;
import jpl.gds.telem.ingest.IIngestWorker;
import jpl.gds.telem.ingest.IngestConfiguration;
import jpl.gds.telem.ingest.IngestSessionManager;
import jpl.gds.telem.ingest.app.mc.rest.resources.TelemetryIngestorSummaryDelegateResource;
import jpl.gds.telem.ingest.state.IIngestWorkerStatus;
import jpl.gds.telem.ingest.state.IngestWorkerStatus;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.IFrameSummaryMessage;
import jpl.gds.tm.service.api.packet.IPacketSummaryMessage;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

/**
 * Worker for Telemetry Ingestor
 *
 */
public class TelemetryIngestWorker extends AbstractTelemetryWorker implements IIngestWorker {
    private final GlobalLadProperties              gladConfig;
    private final IngestConfiguration              ingestConfig;


    private DatabaseCommandOptions                 dbOpts;
    private MessageServiceCommandOptions           jmsOpts;
    private GladClientCommandOptions               gladClientOpts;
    private SessionCommandOptions                  sessionOpts;
    private ConnectionCommandOptions               connectionOpts;

    private PerformanceProperties perfProps;
    private DownlinkStatusResource downStatus;
    private TelemetryIngestorSummaryDelegateResource telemStatus;

    /**
     * Creates an instance of TelemetryTelemetryProcessWorker.
     * @param springContext
     *            the Spring Application Context
     * @param sessionConfig The session configuration to use with this worker
     * @param sseContextFlag The SSE context flag to use
     * @param secureLoader The parent servers secure class loader
     */
    public TelemetryIngestWorker(final ApplicationContext springContext, final SessionConfiguration sessionConfig,
                                 SseContextFlag sseContextFlag, AmpcsUriPluginClassLoader secureLoader) {
        super(springContext, sessionConfig, secureLoader);
        this.sseFlag = sseContextFlag;
        this.gladConfig = GlobalLadProperties.getGlobalInstance();

        this.ingestConfig = springContext.getBean(IngestConfiguration.class);
        this.tracer = TraceManager.getTracer(springContext, Loggers.INGEST);
        this.workerThreadNamePrefix = "TI Worker-";

        this.perfProps = springContext.getBean(PerformanceProperties.class);
        this.downStatus = new DownlinkStatusResource(springContext);
        this.telemStatus = new TelemetryIngestorSummaryDelegateResource(this);
        this.publicationBus = springContext.getBean(IMessagePublicationBus.class);

        // Subscribe to summary messages
        this.publicationBus.subscribe(TmServiceMessageType.TelemetryFrameSummary, this);
        this.publicationBus.subscribe(TmServiceMessageType.TelemetryPacketSummary, this);
        this.publicationBus.subscribe(CommonMessageType.PerformanceSummary, this);
    }

    @Override
    public BaseCommandOptions createOptions() {
        super.createOptions();


        dbOpts = new DatabaseCommandOptions(databaseProps);
        options.addOptions(dbOpts.getAllLocationOptions());
        options.addOption(dbOpts.NO_DATABASE);

        jmsOpts = new MessageServiceCommandOptions(msgServiceConfig);
        options.addOptions(jmsOpts.getAllOptionsWithoutNoJms());


        gladClientOpts = new GladClientCommandOptions(gladConfig);
        options.addOption(gladClientOpts.SERVER_HOST_OPTION);
        options.addOption(gladClientOpts.SOCKET_PORT_OPTION);

        sessionOpts = new SessionCommandOptions((SessionConfiguration) contextConfig);
        connectionOpts = new ConnectionCommandOptions(contextConfig.getConnectionConfiguration());


        // This includes required SSE options
        options.addOptions(sessionOpts.getAllFswDownlinkOptions());
        // This includes required SSE options
        options.addOptions(connectionOpts.getAllDownlinkOptions());

        return options;
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        /* If sse flag is set, assume the same logic as SseDownlinkApp because we may
         * be running integrated chill_web app */
        if (sseFlag.isApplicationSse()) {
            fswDownFlag.setFswDownlinkEnabled(false);
            sseDownFlag.setSseDownlinkEnabled(true);
            setupSseIngest(); // must happen before super.configure()
        } else {
            fswDownFlag.setFswDownlinkEnabled(true);
            sseDownFlag.setSseDownlinkEnabled(false);
        }

        super.configure(commandLine);

        contextConfig.getConnectionConfiguration().setDefaultNetworkValuesForVenue(venueCfg.getVenueType(),
                                                                                   venueCfg.getTestbedName(),
                                                                                   venueCfg.getDownlinkStreamId() );

        sessionOpts.parseAllDownlinkOptionsAsOptional(commandLine, false, false);
        // parse testbed name with default
        sessionOpts.parseTestbedNameWithDefault(commandLine);

        connectionOpts.parseAllOptionsAsOptionalWithoutDefaults(commandLine);

        jmsOpts.parseAllOptionsAsOptional(commandLine);

        dbOpts.parseAllOptionsAsOptional(commandLine);

        ingestConfig.setUseMessageService(msgServiceConfig.getUseMessaging());

        ingestConfig.setUseDb(databaseProps.getUseDatabase());

        setup();
    }

    private void setupSseIngest() throws ParseException {
        final TelemetryConnectionType originalDct =
                contextConfig.getConnectionConfiguration().getSseDownlinkConnection().getDownlinkConnectionType();

        final TelemetryConnectionType newDct = connProps.getSseOverrideTelemetryConnectionType(venueCfg.getVenueType(), originalDct);

        final TelemetryInputType originalInputFormat = contextConfig.getConnectionConfiguration().getSseDownlinkConnection().getInputType();
        final TelemetryInputType newTelemetryInputFormat = connProps.getSseOverrideTelemetryInputType(venueCfg.getVenueType(), newDct,
                                                                                                      originalInputFormat);
        contextConfig.getConnectionConfiguration().createSseDownlinkConnection(newDct);

        IDownlinkConnection newDc = contextConfig.getConnectionConfiguration().getSseDownlinkConnection();
        newDc.setInputType(newTelemetryInputFormat);
        tracer.info("SSE downlink connection type will be " ,
                    newDct , " and input format will be " , newTelemetryInputFormat);
    }

    private void setup() {
        setupWorker();

        // insert the session
        // Create the session manager
        sessionManager = new IngestSessionManager(springContext, contextConfig);

        try {
            // set type back to SESSION
            contextConfig.getContextId().getContextKey().setType(ContextConfigurationType.SESSION);
            // will be updated later to appName if empty
            contextConfig.getContextId().setType("");

            // actually inserts session to DB; will update key number and host
            sessionManager.startSessionDatabase();

            internalChangeWorkerState(WorkerState.READY);
        }
        catch (final DatabaseException e) {
            tracer.error("The session could not be inserted: " + ExceptionTools.getMessage(e));
            tracer.error(Markers.SUPPRESS, "The session could not be inserted: " , ExceptionTools.getMessage(e), e);
            internalChangeWorkerState(WorkerState.STOPPED);
        }
    }


    @Override
    public ITelemetrySummary getSessionSummary() {
        if (sessionManager == null) {
            return null;
        }
        return sessionManager.getProgressSummary();
    }

    @Override
    public IngestConfiguration getIngestConfiguration() {
        return ingestConfig;
    }

    @Override
    public IIngestWorkerStatus getStatus() {
        // Add sync status in addition to connected and flowing
        return new IngestWorkerStatus(
                ((IngestSessionManager)sessionManager).isConnected(),
                ((IngestSessionManager)sessionManager).isFlowing(),
                ((IngestSessionManager)sessionManager).isInSync());
    }

    @Override
    public TelemetryIngestorSummaryDelegateResource getTelemStatus(){
        return telemStatus;
    }

    @Override
    public DownlinkStatusResource getPerfStatus(){
        return downStatus;
    }

    public void handleMessage(final IMessage message) {
        try {
            if (message instanceof IFrameSummaryMessage) {
                handleFrameSumMessage((IFrameSummaryMessage) message);
            }
            else if (message instanceof IPacketSummaryMessage) {
                handlePacketSumMessage((IPacketSummaryMessage) message);
            }
            else if (message instanceof PerformanceSummaryMessage) {
                handlePerformanceSumMessage((PerformanceSummaryMessage) message, perfProps, downStatus);
            }
        }
        catch (Exception e){
            tracer.error(ExceptionTools.getMessage(e));
        }
    }

    private void handleFrameSumMessage(final IFrameSummaryMessage msg) {
        telemStatus.setNumberOfFrames(msg.getNumFrames());
        telemStatus.setNumberOfOutOfSyncFrames(msg.getOutOfSyncCount());
        telemStatus.setBitrate(msg.getBitrate());
    }

    private void handlePacketSumMessage(final IPacketSummaryMessage msg) {
        telemStatus.setNumberOfValidPackets(msg.getNumValidPackets());
        telemStatus.setNumberOfInvalidPackets(msg.getNumInvalidPackets());
        telemStatus.setNumberOfFrameRepeats(msg.getNumFrameRepeats());
        telemStatus.setNumberOfFrameGaps(msg.getNumFrameGaps());
        telemStatus.setNumberOfStationPackets(msg.getNumStationPackets());
        telemStatus.setNumberOfIdlePackets(msg.getNumFillPackets());
    }
}
