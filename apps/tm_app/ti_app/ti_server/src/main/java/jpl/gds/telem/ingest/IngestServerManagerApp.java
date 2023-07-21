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

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.service.telem.ITelemetryIngestorSummary;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOption;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.exceptions.RestfulTelemetryException;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.common.app.mc.AbstractTelemetryServerApp;
import jpl.gds.telem.ingest.app.TelemetryIngestWorker;
import jpl.gds.telem.input.api.config.BufferedInputModeType;
import jpl.gds.telem.input.api.config.BufferedInputModeTypeOption;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.PrintWriter;
import java.util.Collections;

import static jpl.gds.shared.metadata.context.ContextConfigurationType.TELEM_INGEST_SERVER;

/**
 * Telemetry Ingestor server and manager for IIngestWorker classes
 *
 */
@Service
public class IngestServerManagerApp extends AbstractTelemetryServerApp {

    private static final String WAIT_OPTION_SHORT              = "w";
    private static final String WAIT_OPTION_LONG               = "wait";

    private final IngestConfiguration         downConfig;
    private final TelemetryInputProperties    inputProperties;

    private IMySqlAdaptationProperties dbProperties;

    // options
    /** Session command options container */
    private      SessionCommandOptions sessionOpts;
    private      DictionaryCommandOptions    dictOpts;
    private      UnsignedLongOption          waitOption;
    private      BufferedInputModeTypeOption bufferOption;

    private IAccurateDateTime startTime;

    /**
     * Constructor accepting an application context
     *
     * @param ctx
     *            Spring app context
     * @param t
     *            The <Tracer> to log with
     * @param taskExecutor
     *            The Task executor
     * @param downConfig
     *            The <IngestConfiguration>
     */
    public IngestServerManagerApp(final ApplicationContext ctx, final Tracer t, final ThreadPoolTaskExecutor taskExecutor,
                                  final IngestConfiguration downConfig) {
        super(ctx, t, taskExecutor);

        this.downConfig = downConfig;

        this.writeableProperties = downConfig.getFeatureSet();
        this.inputProperties = parentContext.getBean(TelemetryInputProperties.class);
        this.dbProperties = parentContext.getBean(IMySqlAdaptationProperties.class);
    }

    @Override
    @PostConstruct
    public void init() {
        super.commonInit();
    }

    /**
     * Creates a session configured with a worker app context, suitable for use in createWorker
     * 
     * @return SessionConfiguration object
     */
    private SessionConfiguration createSession() {
        return createSession(createWorkerContext());
    }


    /**
     * Inserts a new session entry in the database with the default mission
     * configuration and returns the new session context id.
     *
     * @return the context id of the new session
     */
    public IContextIdentification obtainNewSessionNumber() {

        final IContextConfiguration ctxConfig = createSession();

        // Start the session stores if they have not been started already.
        if (!archiveController.isSessionStoresStarted()) {
            archiveController.startSessionStoresWithoutInserting();
        }

        try {
            // do not set the session number into server config
            archiveController.getSessionStore().insertTestConfig(ctxConfig);

            // Write LDI for new sessions
            if (dbProperties.getExportLDIAny()) {
                archiveController.getSessionStore().writeLDI(ctxConfig);
            }
        } catch (DatabaseException e) {
            throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Encountered an unexpected error condition while attempting " +
                            "to create a new session: " + e.getMessage());
        }
        tracer.info("Created session ", ctxConfig.getContextId().getContextKey());
        return ctxConfig.getContextId();
    }

    @Override
    public ISimpleContextConfiguration createServerContext(){
        return contextFactory.createContextConfiguration(TELEM_INGEST_SERVER, false);
    }

    /**
     * Creates a session configured with the given app context
     * @param ctx Spring application context
     * @return SessionConfiguration object
     */
    SessionConfiguration createSession(final ApplicationContext ctx){
        final SessionConfiguration sessionConfig = new SessionConfiguration(ctx);
        //save worker start time to restore later
        startTime = sessionConfig.getContextId().getStartTime();
        sessionConfig.copyValuesFrom(tempSession);
        sessionConfig.getContextId().setStartTime(startTime);

        return sessionConfig;
    }


    @Override
    public IContextIdentification createWorker(final SessionConfiguration config, final String[] args) throws
            ApplicationException {
        final ApplicationContext processContext = config.getApplicationContext();

        setValuesFromParent(processContext, config);
        final TelemetryIngestWorker app = (TelemetryIngestWorker) processContext.getBean(IIngestWorker.class, config,
                                                                                         sseContextFlag, secureLoader);


        return launchWorker(app, args);
    }

    /**
     * Create a new TI worker with the passed in arguments and attach it to the existing
     * session identified by the specified key and host.
     *
     * Implemented to support the case where multiple TI workers will be attached to the same session.
     *
     * @param key The session key
     * @param host The session host
     * @param args The list arguments used to configure the new worker
     * @return IContextKey The context key of the new worker
     * @throws RestfulTelemetryException when an error occurs
     */
    public IContextKey attachWorkerToSession(final long key, final String host, final String[] args) throws ApplicationException{
        final SessionConfiguration sessionConfigFromDb;
        try {
            sessionConfigFromDb = getSessionConfigurationFromDatabase(key, host);
        } catch (Exception e) {
            throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Encountered an unexpected error condition while attempting " +
                            "to construct a session configuration object based on an " +
                            "existing session within the database identified by key: '" +
                            key + "' and host: '" + host + "', " + e.getMessage());
        }
        final SessionConfiguration workerSessionConfig = createSession();

        workerSessionConfig.copyValuesFrom(sessionConfigFromDb);
        workerSessionConfig.getContextId().setStartTime(startTime);
        workerSessionConfig.getContextId().setNumber(sessionConfigFromDb.getContextId().getNumber());
        workerSessionConfig.getContextId().setHost(sessionConfigFromDb.getContextId().getHost());
        final IContextIdentification contextConfig = createWorker(workerSessionConfig, args);

        return contextConfig.getContextKey();
    }

    @Override
    public IContextIdentification createWorker(final SessionConfiguration config) throws ApplicationException{
        return createWorker(config, new String[0]);
    }

    @Override
    public void run() {
        Thread.currentThread().setName(ApplicationConfiguration.getApplicationName());
    }

    @Override
    public BaseCommandOptions createOptions() {
        if (options != null) {
            return options;
        }

        super.createOptions(baseCommandOptions);

        sessionOpts = new SessionCommandOptions((SessionConfiguration) tempSession);
        dictOpts = new DictionaryCommandOptions(parentDictionaryProperties);

        waitOption = new UnsignedLongOption(WAIT_OPTION_SHORT, WAIT_OPTION_LONG, "interval",
                                            "input meter interval (milliseconds)", false);

        waitOption.setDefaultValue(UnsignedLong.MIN_VALUE);
        options.addOption(waitOption);

        bufferOption = new BufferedInputModeTypeOption(false, inputProperties, true, false);
        options.addOption(bufferOption);

        options.addOptions(dictOpts.getAllOptions());

        // only add ouptutDir
        options.addOptions(sessionOpts.getOutputDirOption());

        return options;
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        // default REST port depends on SSE option; has to happen first
        contextOpts.parseAllOptionsAsOptional(commandLine);

        //set defaults for REST port
        restOptions.REST_PORT_OPTION.setDefaultValue(sseContextFlag.isApplicationSse() ? downConfig.getFeatureSet().getRestPortSse() :
                                               downConfig.getFeatureSet().getRestPortFsw());

        //will also parse rest port
        super.configure(commandLine);

        dictOpts.parseAllOptionsAsOptionalWithDefaults(commandLine);
        tempSession.getConnectionConfiguration().setDefaultNetworkValuesForVenue(venueConfig.getVenueType(),
                                                                                       venueConfig.getTestbedName(), venueConfig.getDownlinkStreamId());
        // only add ouptutDir
        sessionOpts.parseOutputDir(commandLine);

        final long meterInterval = waitOption.parseWithDefault(commandLine, false, true).longValue();
        if (meterInterval > 0) {
            downConfig.setMeterInterval(meterInterval);
        }


        downConfig.setUseMessageService(messageSvcConfig.getUseMessaging());

        downConfig.setUseDb(dbOptions.getDatabaseConfiguration().getUseDatabase());

        if (venueConfig.getVenueType() == null || tempSession.getContextId().getName() == null) {
            throw new ParseException("To use the " + BaseCommandOptions.AUTORUN.getLongOpt()
                                             + " option you must either supply a session configuration file\n"
                                             + "or a venue type and a session name");
        }
        if (tempSession.getConnectionConfiguration().getDownlinkConnection().getInputType() == null) {
            tempSession.getConnectionConfiguration().getDownlinkConnection()
                       .setInputType(parentContext.getBean(ConnectionProperties.class)
                                                    .getDefaultSourceFormat(venueConfig.getVenueType(),
                                                                            sseContextFlag.isApplicationSse()));
        }

        final BufferedInputModeType bufferMode = bufferOption.parse(commandLine);

        if (bufferMode != null) {
            inputProperties.setBufferedInputMode(bufferOption.parse(commandLine));
        }
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
        pw.println("\n");
        options.getOptions().printOptions(pw);

        pw.flush();
    }

    /**
     * Create a new application context for a worker
     *
     * @return a worker application context
     */
    private ApplicationContext createWorkerContext() {
        return SpringContextFactory.getSpringContext(
                Collections.singletonList(TelemetryIngestWorker.class.getPackage().getName()+ ".bootstrap"),
                true);
    }

    @Override
    public IIngestWorker getWorker(final long key, final String host, final int fragment) {
        final ITelemetryWorker worker = super.getWorker(key, host, fragment);
        return (worker == null ? null : ((IIngestWorker) worker));
    }

    @Override
    public ITelemetryIngestorSummary getSessionSummary(final long key, final String host, final int fragment) {
        final ITelemetrySummary summary = super.getSessionSummary(key, host, fragment);
        return summary == null ? null : ((ITelemetryIngestorSummary) summary);
    }

}
