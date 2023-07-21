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
package jpl.gds.tcapp.app;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.connection.ConnectionKey;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.options.ConnectionCommandOptions;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.security.options.AccessControlCommandOptions;
import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.common.config.service.ServiceConfiguration.ServiceParams;
import jpl.gds.common.config.service.ServiceConfiguration.ServiceType;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.EnableFswDownlinkContextFlag;
import jpl.gds.context.api.EnableSseDownlinkContextFlag;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.store.StoreInitiationException;
import jpl.gds.db.api.sql.store.ldi.ICommandMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.message.api.portal.IMessagePortal;
import jpl.gds.perspective.gui.PerspectiveListener;
import jpl.gds.security.cam.AccessControl;
import jpl.gds.security.cam.AccessControlException;
import jpl.gds.security.cam.ExitableSecureApplication;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.SessionConfigurationValidator;
import jpl.gds.session.config.gui.SessionConfigShell;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.session.message.SessionHeartbeatMessage;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.sys.Shutdown;
import jpl.gds.shared.sys.Shutdown.IShutdown;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.icmd.exception.AuthenticationException;
import jpl.gds.tc.api.options.UplinkCommandOptions;
import jpl.gds.tc.api.output.ISseCommandSocket;

import static jpl.gds.tc.api.options.UplinkCommandOptions.UPLINK_FILE_PARAM_SHORT;

/**
 * This is the abstract superclass for all uplink-related command line
 * applications including:
 *
 * chill_send_cmd, chill_up, chill_send_file, chill_send_scmf,
 * chill_send_raw_data & MTAK's uplink server
 *
 * NB: The PerspectiveListener is just passed on to AccessControl
 * by the subclass. It can be null. It is not otherwise used here
 * nor is it made accessible outside.
 *
 *
 * SPV is now enabled by default
 * Changed static flag to local and old conditional code to use SPV or not
 */
public abstract class AbstractUplinkApp extends AbstractCommandLineApp
        implements ExitableSecureApplication, IQuitSignalHandler {
    public enum ShutdownFunctorsEnum {
        /*
         * Removed ParameterPoller because
         * it's no longer used. But leaving this enum intact in case of future
         * extensions.
         */

        /** Release SSO token */
        SsoToken;
    }
    private static final String ACCESS_CONTROL_MANAGER_CLASS_NAME = "gov.nasa.jpl.ammos.css.accesscontrol.AccessControlManager";

    static {
        // Originally done in the constructor, but Tracer was being
        // instantiated before the constructor set the time zone.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /** Database interface */
    private IDbSqlArchiveController              archiveController;

    /** Message service interface */
    protected IMessagePortal                     jmsPortal;

    /** Logging interface */
    protected static Tracer                      log;

    /** Internal message bus interface */
    protected static IMessagePublicationBus      context;

    /** Shared reference to the command configuration **/
    protected final CommandProperties            cmdConfig;
    /** Shared reference to the CLTU configuration **/
    protected final CltuProperties               cltuConfig;
    /** Shared reference to the PLOP configuration **/
    protected final PlopProperties               plopConfig;
    /** Shared reference to the frame configuration **/
    protected final CommandFrameProperties       frameConfig;
    /** Shared reference to the SCMF configuration **/
    protected final ScmfProperties               scmfConfig;
    /** Shared reference to the Security Properties */
    protected final SecurityProperties           securityConfig;
    /** Shared reference to the database configuration **/
    protected final IMySqlAdaptationProperties   dbProperties;
    /** Shared reference to the Host configuration **/
    protected final IConnectionMap               hostConfig;

    /** True if configuration should be skipped, false otherwise */
    protected boolean                            autorun;
    /** True if the user supplied a session config, false otherwise */
    protected boolean                            haveTestConfig;
    /** True if the user supplied a session key, false otherwise */
    protected boolean                            testKeySpecified;
    /** The binary output file (if the user supplied one) */
    protected String                             binaryOutputFile;
    /** A timer for sending heartbeat messages */
    protected Timer                              heartbeatTimer;

    private static AccessControl                 accessControl = null;

    protected CommandUserRole                      role          = null;
    protected LoginEnum                            loginMethod   = null;
    protected String                               keytabFile    = null;
    //added username option for keytab
    protected String                               username      = null;


    private PerspectiveListener                  listener      = null;

    private final Shutdown<ShutdownFunctorsEnum> _shutdown;

    /**
     * The current application context.
     */
    protected final ApplicationContext           appContext;
    private final MissionProperties              missionProps;
    private final ConnectionProperties           connectionProps;
    private final MessageServiceConfiguration    msgConfig;

    /** The context configuration */
    protected IContextConfiguration               sessionConfig;

    /** Session command options container */
    protected SessionCommandOptions              sessionOpts;

    /** Dictionary command options container */
    protected DictionaryCommandOptions           dictOpts;
    
    /** Database command options container */
    private DatabaseCommandOptions               dbOptions;

    /** Message Service command options container */
    private MessageServiceCommandOptions         jmsOptions;

    /** Connection command options container */
    protected ConnectionCommandOptions           connectionOpts;
   
    /** Access Control (Security) options container */
    protected AccessControlCommandOptions        securityOptions;

    /** Uplink specific command options container */
    protected UplinkCommandOptions               uplinkOptions;
    
    /** Context command options container */
    protected ContextCommandOptions              contextOptions;

    /**
     * Initialize all the various uplink resources this class depends on
     */
    protected AbstractUplinkApp() {
        super();

        appContext = SpringContextFactory.getSpringContext(true);
        sessionConfig = new SessionConfiguration(appContext);
        missionProps = appContext.getBean(MissionProperties.class);
        connectionProps = appContext.getBean(ConnectionProperties.class);
        msgConfig = appContext.getBean(MessageServiceConfiguration.class);
        hostConfig = appContext.getBean(IConnectionMap.class);
        dbProperties = appContext.getBean(IMySqlAdaptationProperties.class);
        cmdConfig = appContext.getBean(CommandProperties.class);
        context = appContext.getBean(IMessagePublicationBus.class);
        archiveController = appContext.getBean(IDbSqlArchiveController.class);
        log = TraceManager.getTracer(appContext, Loggers.UPLINK);

        _shutdown = new Shutdown<>(ShutdownFunctorsEnum.class, true, log);

        cltuConfig = appContext.getBean(CltuProperties.class);
        plopConfig = appContext.getBean(PlopProperties.class);
        frameConfig = appContext.getBean(CommandFrameProperties.class);
        scmfConfig = appContext.getBean(ScmfProperties.class);
        securityConfig = appContext.getBean(SecurityProperties.class);

        haveTestConfig = false;
        testKeySpecified = false;
        binaryOutputFile = null;


    }

    @Override
    public void exitCleanly() {
        try {
            stopExternalInterfaces();
        }
        catch (final Exception e) {
            // Log exception to file and suppress from console
            log.warn(Markers.SUPPRESS, "Exception encountered while shutting down ",
                     ApplicationConfiguration.getApplicationName(), ": ", ExceptionTools.getMessage(e), e);
        }
    }


    /**
     * One stop method to enable the command dictionary and load it. There is no
     * good place to do this so it is up to the apps extending this to make the call.
     * 
     *
     * @throws DictionaryException
     *             if an error occurs accessing a dictionary
     */
    public void loadConfiguredCommandDictionary() throws DictionaryException {
        appContext.getBean(FlightDictionaryLoadingStrategy.class).enableCommand().loadAllEnabled(appContext, false,
                                                                                                 false);
    }

    /**
     * Start up the interface to the database and message buss.
     *
     * @throws AuthenticationException
     *             if Access Control is enabled and user
     *             authentication fails
     */
    public void startExternalInterfaces() throws AuthenticationException {
        startExternalInterfaces(true);
    }

    /**
     * Start up the interface to the database and message bus.
     *
     * @param alsoLog
     *            If true start log as well as command
     * @throws AuthenticationException
     *             if Access Control is enabled and user
     *             authentication fails
     *             if an error occurs
     */
    public void startExternalInterfaces(final boolean alsoLog)
            throws AuthenticationException {
        createOutputDirectory();

        if (dbProperties.getUseDatabase() && !scmfConfig.getOnlyWriteScmf()
                && cmdConfig.getBinaryOutputFile() == null) {
            startTestConfigDatabase(alsoLog);
            archiveController.startLogCommandStores(alsoLog);

        }
        else {
            log.info("Database is disabled.");
        }

        if (msgConfig.getUseMessaging() && !scmfConfig.getOnlyWriteScmf() && cmdConfig.getBinaryOutputFile() == null) {
            startMessagingInterfaces();
        }
        else {
            log.info("Message service is disabled.");
        }

        if (appContext.getBean(SecurityProperties.class).getEnabled()
                && hostConfig.getFswUplinkConnection().getUplinkConnectionType()
                             .equals(UplinkConnectionType.COMMAND_SERVICE)) {
            // check to see if CSS API jar is in classpath
            try {
                Class.forName(ACCESS_CONTROL_MANAGER_CLASS_NAME);
            }
            catch (final ClassNotFoundException e) {
                throw new IllegalStateException("Unable to load CSS dependencies. CSS may not be installed in your environment or installed in an incorrect location.");
            }

            try {
                // use username supplied. If none, use login user
                final String user = username == null || username.isEmpty() ? sessionConfig.getContextId().getUser()
                        : username;

                // Do not recreate access control if already created,
                // otherwise this will throw upon session restart instead of re-authenticating.
                if (accessControl == null) {
                    /** Pass listener to AccessControl */
                    accessControl = AccessControl.createAccessControl(appContext.getBean(SecurityProperties.class),
                                                                      user, role, loginMethod, keytabFile,
                                                                      cmdConfig.getShowGui(), null, this, log);


                    final IShutdown rtf = accessControl.getReleaseTokenFunctor();

                    if (rtf != null) {
                        _shutdown.addFunctorHook(ShutdownFunctorsEnum.SsoToken, rtf);
                    }

                    /*
                     * Removed code for
                     * getting CpdParametersPoller's stop-all functor.
                     * CpdParamatersPoller no longer exists, and its new
                     * replacement, CpdDmsBroadcastStatusMessagesPoller adds
                     * itself to the shutdown hook.
                     */

                }
            }
            catch (final AccessControlException ace) {
                throw new AuthenticationException("Error encountered while attempting to authenticate user", ace);
            }

            try {
                accessControl.requestSsoToken();

                // Now get the real user

                appContext.getBean(AccessControlParameters.class).setUserId(accessControl.getUser());
            }
            catch (final AccessControlException ace) {
                throw new AuthenticationException("Unable to authenticate user", ace);
            }
        }
        else {
            log.info("Access control is disabled");

            accessControl = null;
        }

        if (missionProps.missionHasSse()) {
            appContext.getBean(ISseCommandSocket.class).enableSseSocket(hostConfig.getSseUplinkConnection());
        }
    }

    /**
     * Stop the interface to the database and message buss.
     */
    public void stopExternalInterfaces() {
        stopTestConfigDatabase();
        if (archiveController != null) {
            archiveController.shutDown();
        }
        stopMessagingInterfaces();
        archiveController = null;

        if (missionProps.missionHasSse()) {
            appContext.getBean(ISseCommandSocket.class).disableSseSocket();
        }
    }

    /**
     * Start the interface to the session database store
     *
     * @param alsoLog
     *            If true start log as well as command
     */
    protected final void startTestConfigDatabase(final boolean alsoLog) {
        if (archiveController == null) {
            archiveController = appContext.getBean(IDbSqlArchiveController.class);
        }

        archiveController.addNeededStore(ICommandMessageLDIStore.STORE_IDENTIFIER);

        if (alsoLog) {
            archiveController.addNeededStore(ILogMessageLDIStore.STORE_IDENTIFIER);
        }


        if (!archiveController.startSessionStores()) {
            throw new StoreInitiationException("Unable to start test session store");
        }
    }

    /**
     * Create the session output directory if necessary
     */
    protected void createOutputDirectory() {
        // create the output directory if it doesn't exist
        final File testDirFile = new File(appContext.getBean(IGeneralContextInformation.class).getOutputDir());
        if (!testDirFile.exists() && !testDirFile.mkdirs()) {
            log.error("Unable to create session output directory " + testDirFile.getPath());
        }
    }

    /**
     * Stop the session database interface
     */
    protected final void stopTestConfigDatabase() {
        if (archiveController != null) {
            archiveController.stopSessionStore();
        }
    }

    /**
     * Start the message bus interface (including heartbeat timer)
     */
    protected void startMessagingInterfaces() {

        if (jmsPortal == null) {

//            appContext.getBean(IGeneralContextInformation.class).setRootPublicationTopic(
//                    ContextTopicNameFactory.getTopicNameFromConfigValue(appContext,
//                                                                                    TopicNameToken.APPLICATION));

            jmsPortal = appContext.getBean(IMessagePortal.class);

            jmsPortal.enableImmediateFlush(true);
            jmsPortal.startService();
            startHeartbeat();
        }
    }

    /**
     * Stop the message bus interface (including heartbeat timer)
     */
    protected final void stopMessagingInterfaces() {
        // Stop the portal to the external message service
        if (jmsPortal != null) {
            jmsPortal.stopService();
            jmsPortal = null;
            stopHeartbeat();
        }
    }

    /**
     * Send a messages to the message bus to show that this app is still alive.
     */
    protected void sendHeartbeatMessage() {
        // publish session heartbeat message
        final IContextHeartbeatMessage heartbeat = new SessionHeartbeatMessage(sessionConfig);

        final ServiceConfiguration sc = new ServiceConfiguration();

        ServiceParams sp = new ServiceParams(ServiceType.LOMS, dbProperties.getUseDatabase(),
                                             HostPortUtility.cleanHostName(dbProperties.getHost()),
                                             dbProperties.getPort(), dbProperties.getDatabaseName());
        sc.addService(sp);

        sp = new ServiceParams(ServiceType.JMS, msgConfig.getUseMessaging(),
                               HostPortUtility.cleanHostName(msgConfig.getMessageServerHost()),
                               msgConfig.getMessageServerPort(), null);
        sc.addService(sp);

        heartbeat.setServiceConfiguration(sc);
        context.publish(heartbeat);
    }

    /**
     * Initialize the heartbeat sender
     */
    protected final void startHeartbeat() {
        // if the application is stand alone then run
        // if the application is integrated but will not run the FSW Chill Down
        // then run
        if (!GdsSystemProperties.isIntegratedGui()) {
            final long heartbeatInterval = appContext.getBean(GeneralProperties.class).getContextHeartbeatInterval();
            heartbeatTimer = new Timer();
            heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sendHeartbeatMessage();
                }
            }, heartbeatInterval, heartbeatInterval);
        }
    }

    /**
     * Destroy the heartbeat sender
     */
    protected final void stopHeartbeat() {
        // heartbeat for standalone AND integrated GUI
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
        }
    }


    /**
     * Writes out the current contextConfig to the user's CHILL directory.
     *
     */
    protected void writeOutSessionConfig() {

        if (!GdsSystemProperties.isIntegratedGui()) {
            final String outputFile = GdsSystemProperties.getUserConfigDir() + File.separator
                    + appContext.getBean(GeneralProperties.class).getDefaultContextConfigFileName();

            try {
                final File f = new File(GdsSystemProperties.getUserConfigDir());
                if (!f.exists() && !f.mkdirs()) {
                    log.warn("Error writing session configuration output file " + outputFile);
                    return;
                }
                sessionConfig.save(outputFile);
            } catch (final IOException e) {
                log.warn("Error writing session configuration output file " + outputFile + ": " + e.toString());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));

        sessionOpts = new SessionCommandOptions((SessionConfiguration) sessionConfig);
        dictOpts = new DictionaryCommandOptions(appContext.getBean(DictionaryProperties.class));
        dbOptions = new DatabaseCommandOptions(appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
        jmsOptions = new MessageServiceCommandOptions(appContext.getBean(MessageServiceConfiguration.class));
        connectionOpts = new ConnectionCommandOptions(sessionConfig.getConnectionConfiguration());
        uplinkOptions = new UplinkCommandOptions(appContext);
        securityOptions = new AccessControlCommandOptions(securityConfig, sessionConfig.getAccessControlParameters());
        contextOptions = new ContextCommandOptions(appContext.getBean(IContextConfiguration.class));

        
        options.addOption(BaseCommandOptions.DEBUG);

        options.addOptions(sessionOpts.getAllUplinkOptionsNoDssId());

        options.addOptions(uplinkOptions.getBasicUplinkOptions());

        /* Change to FSW options only.  Not all uplink apps need SSE options */
        options.addOptions(connectionOpts.getAllFswUplinkOptions());

        options.addOptions(dbOptions.getAllOptions());

        options.addOptions(jmsOptions.getAllOptions());
        
        options.addOption(contextOptions.PUBLISH_TOPIC);

        return options;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        // Kill these so they do not get set in the database
        appContext.getBean(EnableFswDownlinkContextFlag.class).setFswDownlinkEnabled(false);
        appContext.getBean(EnableSseDownlinkContextFlag.class).setSseDownlinkEnabled(false);

        autorun = BaseCommandOptions.AUTORUN.parse(commandLine);
        cmdConfig.setDebug(BaseCommandOptions.DEBUG.parse(commandLine));

        jmsOptions.parseAllOptionsAsOptional(commandLine);
        dbOptions.parseAllOptionsAsOptional(commandLine);

        // parsing the option validates the connection type
        // Removed validation argument to this call.
        sessionOpts.parseAllUplinkOptionsAsOptional(commandLine, true, autorun);

        /* Parse all Uplink Options */
        uplinkOptions.parseAllBasicOptionsAsOptionalWithDefault(commandLine);

        testKeySpecified = commandLine.hasOption(SessionCommandOptions.SESSION_KEY_LONG);
        
        /* 
         * ReservedOptions no longer performs the query for the existing session. Do it here.
         */
        if (testKeySpecified) {
            queryForSession(sessionConfig.getContextId().getNumber(), sessionConfig.getContextId().getHost());
        }
        
        final boolean sessionConfigFile = commandLine.hasOption(SessionCommandOptions.SESSION_CONFIG_LONG);
        haveTestConfig = sessionConfigFile || testKeySpecified;
        if (sessionConfigFile && ((SessionConfiguration) sessionConfig).isDownlinkOnly()) {
            throw new ParseException("Supplied session configuration file is for a downlink-only "
                    + "configuration and cannot be used by this uplink application");
        }
        if (testKeySpecified && ((SessionConfiguration) sessionConfig).isDownlinkOnly()) {
            throw new ParseException("Supplied session configuration key is for a downlink-only "
                    + "configuration and cannot be used by this uplink application");
        }
        // Clear fields when not in integrated gui mode
        if (sessionConfigFile && !GdsSystemProperties.isIntegratedGui()) {
            sessionConfig.clearFieldsForNewConfiguration();
        }

        if (GdsSystemProperties.isIntegratedGui()) {
            final AccessControlParameters accessParams = appContext.getBean(AccessControlParameters.class);
            role = accessParams.getUserRole();
            loginMethod = accessParams.getLoginMethod();
            keytabFile = accessParams.getKeytabFile();
            username = accessParams.getUserId();
        }

        final IVenueConfiguration venueConfig = appContext.getBean(IVenueConfiguration.class);

        /*
         * Default uplink connection type when
         * just generating file output, or things fail later.
         */
        UplinkConnectionType uplinkConnType = connectionOpts.UPLINK_CONNECTION_TYPE.parse(commandLine);
        if (scmfConfig.getOnlyWriteScmf() || cmdConfig.getBinaryOutputFile() != null) {
            if (uplinkConnType != UplinkConnectionType.SOCKET) {
                hostConfig.createFswUplinkConnection(UplinkConnectionType.SOCKET);
            }
            uplinkConnType = UplinkConnectionType.SOCKET;
        }

        if ((autorun && !haveTestConfig) || !cmdConfig.getShowGui() && !haveTestConfig && !scmfConfig.getOnlyWriteScmf()
                && cmdConfig.getBinaryOutputFile() == null) {

            // if we are using COMMAND_SERVICE uplink in TESTSET venue
            if (UplinkConnectionType.COMMAND_SERVICE == uplinkConnType) {
                connectionOpts.FSW_UPLINK_HOST.parseWithDefault(commandLine, false, true);
                connectionOpts.FSW_UPLINK_PORT.parseWithDefault(commandLine, false, true);
            }

            /**
             * Do not configure downlink hosts and ports.
             *
             */

            hostConfig.setDefaultNetworkValuesForVenue(venueConfig.getVenueType(), venueConfig.getTestbedName(),
                                                       venueConfig.getDownlinkStreamId(),
                                                       !GdsSystemProperties.isIntegratedGui());
        }

        // Parse FSW info
        connectionOpts.FSW_UPLINK_HOST.parseWithDefault(commandLine, false, !haveTestConfig);
        connectionOpts.FSW_UPLINK_PORT.parseWithDefault(commandLine, false, !haveTestConfig);
        dictOpts.FSW_DICTIONARY_DIRECTORY.parseWithDefault(commandLine, false, true);
        dictOpts.FSW_VERSION.parseWithDefault(commandLine, false, !haveTestConfig);

        // Parse SSE info also
        if (appContext.getBean(MissionProperties.class).missionHasSse()) {
            connectionOpts.SSE_UPLINK_HOST.parseWithDefault(commandLine, false, true);
            connectionOpts.SSE_UPLINK_PORT.parseWithDefault(commandLine, false, true);

            dictOpts.SSE_DICTIONARY_DIRECTORY.parse(commandLine);
            dictOpts.SSE_VERSION.parse(commandLine);
        }

        if (autorun && sessionConfig.getContextId().getNumber() == null) {
            if (sessionConfig.getContextId().getName() == null) {
                throw new ParseException("In order to use the --" + BaseCommandOptions.AUTORUN.getLongOpt()
                        + " command line option, " + "the --" + sessionOpts.SESSION_NAME.getLongOpt()
                        + " argument must also be specified on the command line, in a specified in a session configuration file, or"
                        + " indirectly via a test key.");
            }
            else if (venueConfig.getVenueType() == null) {
                throw new ParseException("In order to use the --" + BaseCommandOptions.AUTORUN.getLongOpt()
                        + " command line option, " + "the --" + sessionOpts.VENUE_TYPE.getLongOpt()
                        + " argument must also be specified on the command line, in a specified in a session configuration file, or"
                        + " indirectly via a test key.");
            }
            else if ((venueConfig.getVenueType() == VenueType.TESTBED || venueConfig.getVenueType() == VenueType.ATLO)
                    && venueConfig.getTestbedName() == null) {
                throw new ParseException("In order to use the --" + BaseCommandOptions.AUTORUN.getLongOpt()
                        + " command line option, " + "the --" + sessionOpts.SESSION_NAME.getLongOpt()
                        + " argument must also be specified on the command line, in a specified in a test configuration file, or"
                        + " indirectly via a test key when" + " the --" + sessionOpts.VENUE_TYPE.getLongOpt() + " is "
                        + VenueType.TESTBED.toString() + " or " + VenueType.ATLO.toString());
            }
        }



        // Set up uplink security
        role = securityOptions.USER_ROLE.parseWithDefault(commandLine, false, true);
        username = securityOptions.USER_ID.parseWithDefault(commandLine, false, true);


        contextOptions.PUBLISH_TOPIC.parse(commandLine);
        
        /* Remove the downlink connections from the session configuration */
        if (!GdsSystemProperties.isIntegratedGui()) {
            sessionConfig.getConnectionConfiguration().remove(ConnectionKey.FSW_DOWNLINK);
            sessionConfig.getConnectionConfiguration().remove(ConnectionKey.SSE_DOWNLINK);
         }
        
        /* Validate the final session configuration */
        final SessionConfigurationValidator scv = new SessionConfigurationValidator((SessionConfiguration) sessionConfig);
        
        if (!scv.validate(false, autorun)) {
            throw new ParseException(scv.getErrorsAsMultilineString());
        }
    }
    
    protected void checkLoginOptions() throws ParseException {
        
        if ((loginMethod == LoginEnum.KEYTAB_FILE) && keytabFile.isEmpty()) {
            throw new ParseException("No keytab file provided");
        }
        if ((loginMethod == LoginEnum.KEYTAB_FILE) && username.isEmpty()) {
            throw new ParseException("No username specified");
        }
    }

    /**
     * Validate uplink connection type against venue and S/C. Make available to
     * integrated chill.
     *
     * @param sc
     *            Session configuration
     *
     * @throws ParseException
     *             If conflict
     */
    /**
     * public void validateUplinkConnectionType(final SessionConfiguration sc)
     * throws ParseException { if (!missionProps.isUplinkEnabled()) { // Do NOT
     * validate uplink connection type if uplink is not // supported because it
     * may not be valid (and it doesn't matter)
     * 
     * return; }
     * 
     * // Certain UplinkConnectionTypes are not valid for the venue. If that's
     * // the case, just kill the uplink right here.
     * 
     * final UplinkConnectionType uct =
     * hostConfig.getFswUplinkConnection().getUplinkConnectionType();
     * 
     * if (uct != null) { final VenueType vt =
     * appContext.getBean(IVenueConfiguration.class).getVenueType();
     * 
     * if ((vt != null) && !connectionProps.getAllowedUplinkConnectionTypes(vt,
     * false).contains(uct)) { throw new ParseException("The uplink connection
     * type of " + uct + " is not valid for the venue type of " + vt + ". The
     * uplink application cannot be launched."); }
     * 
     * } }
     */

    // MTAK uplink proxy queries for session config
    // values, so this method is added so that MTAK uplink can override
    /**
     * Allow MTAK uplink to be able to override properties when necessary
     * 
     * @param commandLine
     *            the command line that may override proerties
     * @throws ParseException
     *             an error was encountered during command line parsing
     */
    protected void checkSessionParameterConflicts(final ICommandLine commandLine) throws ParseException {
        final UnsignedLong contextKey = sessionOpts.SESSION_KEY.parseWithDefault(commandLine, false, true);
        if (null == contextKey) {
            return;
        }

        // This is now handled internally by the Aliasable Command Line Parsers
    }


    /**
     * Show the session configuration GUI so the user can graphically input
     * session information
     *
     * @param display
     *            The SWT display environment
     *
     * @return False if the user canceled the GUI, true otherwise
     */
    protected boolean showSessionConfigGui(final Display display) {


        if (!this.autorun) {
            if (!GdsSystemProperties.isIntegratedGui()) {
                // Find all the uplink connection types.

                final Set<UplinkConnectionType> uplinkTypes = connectionProps.getAllowedUplinkConnectionTypes(false);

                // There were no connection types that support uplink

                if (uplinkTypes.isEmpty()) {
                    throw new IllegalStateException("This mission has no uplink connection types defined");
                }
            }

            /** Pass session config to shell */

            final SessionConfigShell testShell = new SessionConfigShell(appContext, display, true, false, true,
                                                                        (SessionConfiguration) sessionConfig);

            testShell.open();

            while (!testShell.getShell().isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }

            if (testShell.wasCanceled()) {
                display.dispose();
            }
            else {
                if (!GdsSystemProperties.isIntegratedGui() && !testKeySpecified) {
                    sessionConfig.getContextId().setNumber(null);
                    sessionConfig.getContextId().setStartTime(new AccurateDateTime());
                }
            }

            return (testShell.wasCanceled());
        }

        return (false);
    }

    /**
     * Get the SWT display environment
     *
     * @return The root display for SWT
     */
    protected Display getDisplay() {
        final String app = jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName("chill_up");
        Display.setAppName(app);
        Display display = null;
        try {
            display = Display.getDefault();
        }
        catch (final SWTError e) {
            if (e.getMessage().indexOf("No more handles") != -1) {
                throw new IllegalStateException("Unable to initialize user interface.  If you are using X-Windows, make sure your DISPLAY variable is set.",
                                                e);
            }
            else {
                throw (e);
            }
        }

        return (display);
    }

    /**
     * Set perspective listener.
     *
     * @param pl
     *            Perspective listener
     */
    protected void setPerspectiveListener(final PerspectiveListener pl) {
        listener = pl;
    }

    /**
     * Get the shutdown object.
     *
     * @return Shutdown object
     */
    protected final Shutdown<ShutdownFunctorsEnum> getShutdown() {

        return _shutdown;
    }

    @Override
    public boolean getExit() {
        if (this.listener == null) {
            return false;
        }
        return listener.getPerspectiveExit();
    }

    /**
     * Get the application context used by this UplinkApp
     * 
     * @return the ApplicationContext for this UplinkApp
     */
    public ApplicationContext getApplicationContext() {
        return this.appContext;
    }
    
    /**
     * Queries for a session given a key
     * @param key the session ID
     * @param host the session host
     * @throws ParseException if the session cannot be queried
     * 
     */
    private void queryForSession(final Long key, final String host) throws ParseException {
        
        try {
            /*  Use fetch factory rather than archive controller */
            final IDbSqlFetch tsf = appContext.getBean(IDbSqlFetchFactory.class).getSessionFetch(false);
            final IDbSessionInfoFactory dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);

            /** Database Session now adaptable. */
            final IDbSessionInfoUpdater tsi = dbSessionInfoFactory.createQueryableUpdater();
            tsi.addSessionKey(key);

            final List<? extends IDbRecord> testSessions = tsf.get(tsi,null,1,(IDbOrderByType)null);
            if(testSessions.isEmpty())
            {
                throw new ParseException("Value of --" + SessionCommandOptions.SESSION_KEY_LONG + " option must be a valid pre-existing session key. No session with the key '" + key + "' was found.");
            }

            final IDbSessionUpdater dsc = (IDbSessionUpdater) testSessions.get(0);
            dsc.setIntoContextConfiguration(sessionConfig);
        } catch(final DatabaseException s) {
            throw new ParseException("Error Connecting to the database while looking up the specified session key: " + s.getMessage());
        }

    }

}
