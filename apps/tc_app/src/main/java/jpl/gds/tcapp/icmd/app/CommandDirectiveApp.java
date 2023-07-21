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
package jpl.gds.tcapp.icmd.app;

import java.util.Calendar;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.core.NestedRuntimeException;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.cli.legacy.app.CommandLineApp;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.common.error.ErrorCode;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.IHostFetch;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.security.cam.AccessControl;
import jpl.gds.security.cam.AccessControlException;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.LoggingConstants;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.sys.QuitSignalHandler;
import jpl.gds.shared.sys.Shutdown;
import jpl.gds.tc.api.UplinkLogger;
import jpl.gds.tc.api.icmd.CpdDirective;
import jpl.gds.tc.api.icmd.CpdDirectiveArgument;
import jpl.gds.tc.api.icmd.CpdResponse;
import jpl.gds.tc.api.icmd.ICpdClient;
import jpl.gds.tc.api.icmd.exception.AuthenticationException;
import jpl.gds.tc.api.icmd.exception.AuthorizationException;
import jpl.gds.tc.api.icmd.exception.CpdConnectionException;
import jpl.gds.tc.api.icmd.exception.CpdException;
import jpl.gds.tc.api.icmd.exception.ICmdErrorManager;
import jpl.gds.tc.api.icmd.exception.ICmdException;

/**
 * This is the main class for the command line application to send directives to
 * the Command Service in Integrated Command (ICMD).
 *
 * @since AMPCS R5
 */
public class CommandDirectiveApp implements CommandLineApp {
    /** The command line application name. */
    public static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_cmd_directive");

    /** The short command line option to specify the directive to send. */
    protected static final String SHORT_DIRECTIVE_OPTION = "d";

    /** The long command line option to specify the directive to send. */
    protected static final String LONG_DIRECTIVE_OPTION = "directive";

    /** The description of the directive option. */
    private static final String DIRECTIVE_DESCRIPTION = "A name that "
            + "identifies which CPD directive to issue";

    /** The short command line option to specify the directive arguments. */
    protected static final String SHORT_DIRECTIVE_ARGS_OPTION = "a";

    /** The long command line option to specify the directive arguments. */
    protected static final String LONG_DIRECTIVE_ARGS_OPTION = "directiveArgs";

    /** The description of the directive arguments option. */
    private static final String DIRECTIVE_ARGS_DESCRIPTION =
            "A comma-separated set of keyword=value pairs supplying "
                    + "arguments for the directive to be issued";

    /** The short command line option to specify the log file to log to. */
    protected static final String SHORT_LOG_FILE_OPTION = "l";

    /** The long command line option to specify the log file to log to. */
    protected static final String LONG_LOG_FILE_OPTION = "logFile";

    /** The description of the log file option. */
    private static final String LOG_FILE_DESCRIPTION =
            "Location of file to log activity to.";

    /** The short command line option to enable database logging. */
    protected static final String SHORT_LOG_DB_OPTION = "L";

    /** The long command line option to enable database logging. */
    protected static final String LONG_LOG_DB_OPTION = "logToDb";

    /** The description of the log to DB option. */
    private static final String LOG_DB_DESCRIPTION =
            "Specify this option to write logs to the database";

    /** The long command line option for log service URL. */
    private static final String LONG_LOG_SERVICE_URL_OPTION = "logServiceUrl";

    /** The description of the log service URL option. */
    private static final String LOG_SERVICE_URL_DESCRIPTION =
            "URL of REST service to send log messages to";

    /** The long command line option for log service URL. */
    private static final String LONG_LOG_LEVEL_PARAM_OPTION = "logLevelParam";

    /** The description of the log service URL option. */
    private static final String LOG_LEVEL_PARAM_DESCRIPTION =
            "Log Level parameter to set for log service rest request";

    /** The long command line option for log service URL. */
    private static final String LONG_LOG_MESSAGE_PARAM_OPTION =
            "logMessageParam";

    /** The description of the log service URL option. */
    private static final String LOG_MESSAGE_PARAM_DESCRIPTION =
            "Log Message parameter to set for log service rest request";

    /** The short command line option to send a ping to the command service. */
    protected static final String SHORT_PING_OPTION = "p";

    /** The long command line option to send a ping to the command service. */
    protected static final String LONG_PING_OPTION = "ping";

    /** The description of the ping option. */
    private static final String PING_DESCRIPTION =
            "Ping the command service to see if it is available. "
                    + "This tool will exit successfuly (exit code 0) if the command "
                    + "service is available. Otherwise it will exit with an error "
                    + "(exit code 54). The --" + LONG_DIRECTIVE_OPTION
                    + " option is ignored if --" + LONG_PING_OPTION
                    + " option is used.";

    /** The long command line option to run in debug mode. */
    protected static final String LONG_DEBUG_MODE_OPTION = "debug";

    /** The description of the debug option. */
    private static final String DESCRIPTION_DEBUG_MODE = "Debug mode";

    /** The session configuration. */
    private final IContextConfiguration config;

    /** The command line options. */
    private final Options options;

    /** The directive to issue. */
    protected CpdDirective directive;

    /** The logger to use for logging. */
    private UplinkLogger logger;

    /** Flag to indicate whether or not to log to the database. */
    protected boolean logToDb;

    /** StoreController to handle stores */
    protected IDbSqlArchiveController archiveController = null;

    /** The response from the command service. */
    protected CpdResponse response;

    /** Flag to indicate whether or not this run is a ping. */
    private boolean ping;

    /** Flag to indicate debug mode. */
    private boolean debugMode;

    // added username option for keytab
    /** Command security user */
    private String username = null;

    /**
     * Flag to indicate whether or not to resolve the host ID using the provide
     * host name
     */
    protected boolean resolveHostId;
    
    private final ApplicationContext appContext;

    private Tracer                      cmdDirTracer;

    /**
     * Constructor.
     */
    public CommandDirectiveApp() {

        /* Add QuitSignalHandler so logging will be shutdown */
        Runtime.getRuntime().addShutdownHook(new Thread(new QuitSignalHandler(this), Shutdown.THREAD_NAME));
        
        this.appContext = SpringContextFactory.getSpringContext(true);
        this.config = new SessionConfiguration(this.appContext);
        ReservedOptions.setApplicationContext(appContext);
    	
        this.options = new Options();

        // we have the option to turn this off, but as of R5, we want this
        // always on
        this.resolveHostId = true;

        // make sure we're using the local time zone, per requirement from use case
        TimeZone.setDefault(Calendar.getInstance().getTimeZone());

        ILogMessageLDIStore.setMsgPrefixForSession("");

    }
    
    
    @Override
    public void exitCleanly() {
        TraceManager.getDefaultTracer().debug(this.getClass().getName(),
                " shutting down using default QuitSignalHandler");
    }


    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.cli.legacy.app.CommandLineApp#showHelp()
     */
    @Override
    public final void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final Options options = this.createOptions();
        final HelpFormatter formatter = new HelpFormatter();
        formatter
                .printHelp(
                        70,
                        APP_NAME
                                + " --directive <directive-name> [--directiveArgs <keyword=value,..>]",
                        "<options>\n", options, "");

        final CpdDirective[] directives = CpdDirective.values();
        if (directives.length == 0) {
            System.out
                    .println("\nA list of directives is currently unavailable.");
            return;
        }

        System.out.println("\nAvailable directives:");

        for (int i = 0; i < directives.length; i++) {
            System.out.print("   ");

            final Set<CpdDirectiveArgument> args =
                    directives[i].getRequiredArguments();

            final StringBuilder builder = new StringBuilder();

            for (final CpdDirectiveArgument cda : args) {
                builder.append(cda.toString());
                builder.append(", ");
            }

            String argsStr = null;

            if (builder.length() > 0) {
                argsStr = builder.substring(0, builder.length() - 2);
            } else {
                argsStr = "NONE";
            }

            System.out.println(directives[i] + ", args: " + argsStr);
        }

        System.out.println();
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.cli.legacy.app.CommandLineApp#configure(org.apache.commons.cli.CommandLine)
     */
    @Override
    public final void configure(final CommandLine commandLine)
            throws ParseException {
        this.debugMode = commandLine.hasOption(LONG_DEBUG_MODE_OPTION);

        ReservedOptions.parseVersion(commandLine);
        ReservedOptions.parseHelp(commandLine, this);

        if (commandLine.hasOption(SHORT_LOG_FILE_OPTION)) {
            final String logFilePath = commandLine.getOptionValue(SHORT_LOG_FILE_OPTION);
            if ((logFilePath != null) && (logFilePath.length() > 0)) {
                MDC.put(LoggingConstants.FILE_APP_LOG_PATH, logFilePath);
            }
        }
        cmdDirTracer = TraceManager.getTracer(appContext, Loggers.UPLINK);


        if (commandLine.hasOption(SHORT_LOG_DB_OPTION)) {
            this.logToDb = true;

            // If we specify a test key we will get a new fragment;
            // otherwise we will get a new session.

            ReservedOptions.parseSessionKey(commandLine, false, true);

            ReservedOptions.parseSessionHost(commandLine, false);

            if (this.resolveHostId) {
                final IHostFetch fetch = appContext.getBean(IDbSqlFetchFactory.class).getHostFetch();

                try {
                    this.config.getContextId().setHostId(fetch.get(this.config.getContextId().getHost()));
                }
                catch (final DatabaseException e) {
                    throw new ParseException(
                            "Unable to resolve host ID from host name");
                }
            }

            ReservedOptions.parseDatabaseHost(commandLine, false);
            ReservedOptions.parseDatabasePort(commandLine, false);
            ReservedOptions.parseDatabaseUsername(commandLine, false);
            ReservedOptions.parseDatabasePassword(commandLine, false);
        }

        /* added options for AUTO integrated logging */
        if (commandLine.hasOption(LONG_LOG_SERVICE_URL_OPTION)
                && commandLine.hasOption(LONG_LOG_LEVEL_PARAM_OPTION)
                && commandLine.hasOption(LONG_LOG_MESSAGE_PARAM_OPTION)) {
            final String logServiceUrl =
                    commandLine.getOptionValue(LONG_LOG_SERVICE_URL_OPTION);
            final String logLevelParam =
                    commandLine.getOptionValue(LONG_LOG_LEVEL_PARAM_OPTION);
            final String logMessageParam =
                    commandLine.getOptionValue(LONG_LOG_MESSAGE_PARAM_OPTION);

            this.logger =
                    new UplinkLogger(appContext,
                    		cmdDirTracer, this.logToDb, logServiceUrl,
                            logLevelParam, logMessageParam);
        } else {
            this.logger = new UplinkLogger(appContext, cmdDirTracer, this.logToDb);
        }

        ReservedOptions.parseLoginMethod(commandLine, false);
        ReservedOptions.parseUserRole(commandLine);

        //  replaced password file with keytab files
        ReservedOptions.parseKeytabFile(commandLine);

        final AccessControlParameters accessParams = appContext.getBean(AccessControlParameters.class);
        
        if ((accessParams.getLoginMethod() == LoginEnum.KEYTAB_FILE)
                && accessParams.getKeytabFile().isEmpty()) {
            throw new ParseException("No keytab file provided");
        }

        this.username = ReservedOptions.parseUsername(commandLine);

        if ((accessParams.getLoginMethod() == LoginEnum.KEYTAB_FILE)
                && this.username.isEmpty()) {
            throw new ParseException("No username specified");
        }

        this.ping = commandLine.hasOption(LONG_PING_OPTION);

        if (!this.ping) {
            String directiveStr = null;

            if (commandLine.hasOption(SHORT_DIRECTIVE_OPTION)) {
                directiveStr =
                        commandLine.getOptionValue(SHORT_DIRECTIVE_OPTION);
            }

            if ((directiveStr == null) || (directiveStr.length() == 0)) {
                throw new ParseException("Missing required option --"
                        + LONG_DIRECTIVE_OPTION);
            }

            try {
                this.directive =
                        CpdDirective.valueOf(directiveStr.toUpperCase().trim());
            } catch (final Exception e) {
                throw new ParseException("Invalid CPD directive: "
                        + directiveStr);
            }

            if (commandLine.hasOption(SHORT_DIRECTIVE_ARGS_OPTION)) {
                final String directiveArgs =
                        commandLine.getOptionValue(SHORT_DIRECTIVE_ARGS_OPTION);

                final String[] args = directiveArgs.split(",");

                for (final String a : args) {
                    final String[] keyValue = a.split("=");

                    try {
                        if (keyValue.length < 2) {
                            throw new ICmdException(
                                    "Invalid CPD argument format. Must be in the format of [name]=[value]");
                        }

                        this.directive.addArgument(keyValue[0], keyValue[1]);
                    } catch (final ICmdException e) {
                        throw new ParseException(
                                "Error parsing CPD directive argument, " + a
                                        + ": " + e.getMessage());
                    }
                }
            } else if (this.directive.requireArgument()) {
                throw new ParseException("The directive, "
                        + this.directive.toString()
                        + ", requires arguments, but none were provided");
            }
        }

        ReservedOptions.parseFswUplinkHost(commandLine, false, true);
        ReservedOptions.parseFswUplinkPort(commandLine, false, true);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.cli.legacy.app.CommandLineApp#createOptions()
     */
    @Override
    public final Options createOptions() {

        this.options.addOption(ReservedOptions
                .getOption(ReservedOptions.VERSION_SHORT_VALUE));
        this.options.addOption(ReservedOptions
                .getOption(ReservedOptions.HELP_SHORT_VALUE));

        this.options.addOption(SHORT_DIRECTIVE_OPTION, LONG_DIRECTIVE_OPTION,
                true, DIRECTIVE_DESCRIPTION);
        this.options.addOption(SHORT_DIRECTIVE_ARGS_OPTION,
                LONG_DIRECTIVE_ARGS_OPTION, true, DIRECTIVE_ARGS_DESCRIPTION);

        this.options.addOption(SHORT_LOG_FILE_OPTION, LONG_LOG_FILE_OPTION,
                true, LOG_FILE_DESCRIPTION);
        this.options.addOption(SHORT_LOG_DB_OPTION, LONG_LOG_DB_OPTION, false,
                LOG_DB_DESCRIPTION);

        this.options.addOption(SHORT_PING_OPTION, LONG_PING_OPTION, false,
                PING_DESCRIPTION);

        this.options.addOption(null, LONG_LOG_SERVICE_URL_OPTION, true,
                LOG_SERVICE_URL_DESCRIPTION);
        this.options.addOption(null, LONG_LOG_LEVEL_PARAM_OPTION, true,
                LOG_LEVEL_PARAM_DESCRIPTION);
        this.options.addOption(null, LONG_LOG_MESSAGE_PARAM_OPTION, true,
                LOG_MESSAGE_PARAM_DESCRIPTION);

        this.options.addOption(ReservedOptions.FSW_UPLINK_HOST);
        this.options.addOption(ReservedOptions.FSW_UPLINK_PORT);
        this.options.addOption(ReservedOptions.SESSION_KEY);
        this.options.addOption(ReservedOptions.SESSION_HOST);
        this.options.addOption(ReservedOptions.DATABASE_HOST);
        this.options.addOption(ReservedOptions.DATABASE_PORT);
        this.options.addOption(ReservedOptions.DATABASE_USERNAME);
        this.options.addOption(ReservedOptions.DATABASE_PASSWORD);
        this.options.addOption(ReservedOptions.LOGIN_METHOD_NON_GUI);
        this.options.addOption(ReservedOptions.USER_ROLE);
        this.options.addOption(ReservedOptions.KEYTAB_FILE);
        this.options.addOption(ReservedOptions.USERNAME);

        this.options.addOption(null, LONG_DEBUG_MODE_OPTION, false,
                DESCRIPTION_DEBUG_MODE);

        return this.options;
    }

    /**
     * Executes the main application logic
     *
     * @throws ICmdException if there is an error processing the directive to
     *             send to the command service
     * @throws AuthorizationException if the user cannot be authenticated
     * @throws AuthenticationException if the user/role does not have
     *             permissions to send the directive
     * @throws CpdConnectionException if the command service cannot be contacted
     * @throws CpdException if the command service received the request, but had
     *             trouble processing it
     */
    public final void run() throws AuthenticationException,
            AuthorizationException, ICmdException, CpdConnectionException,
            CpdException {
        this.startServices();

        ICpdClient client;
        try {
            client = appContext.getBean(ICpdClient.class);
            client.setLogger(logger);

            if (this.ping) {
                final String uplinkHost =
                        this.config.getConnectionConfiguration().getFswUplinkConnection().getHost();
                final int uplinkPort =
                		this.config.getConnectionConfiguration().getFswUplinkConnection().getPort();

                this.logger.info("Pinging command service at: " + uplinkHost + ":" + uplinkPort);
                final boolean success = client.pingCommandService();
                if (success) {
                    this.logger.info("Successfully pinged command service");
                } else {
                    throw new CpdConnectionException(
                            "Unable to ping command service at: " + uplinkHost
                                    + ":" + uplinkPort);
                }
            } else {
                this.logger.info("Sending directive: "
                        + this.directive.toString()
                        + (this.directive.hasArguments() ? ", args: "
                                + this.directive.getArgumentsString() : ""));

                this.response = client.issueDirective(this.directive);

                if (this.response.isSuccessful()) {
                    this.logger.info("CPD Response: " + response.toKeyValueCsv());
                    System.out.println("CPD Response: " + response.toKeyValueCsv());
                } else {
                    final String cpdMessage =
                            this.response.getDiagnosticMessage();
                    String errorMessage =
                            "Error encountered by CPD while trying to process the request";

                    if (cpdMessage != null) {
                        errorMessage = "CPD Error - diagnostic message: " + cpdMessage;
                        // Remove error log here preventing duplicate logs
                        // because exception gets thrown to main and logged there
                    }
                    System.out.println("CPD Response: " + response.toKeyValueCsv());
                    throw new CpdException(errorMessage);
                }
            }

        	/*
			 * It's important that these
			 * separate catch clauses are not combined into a generic Exception
			 * catch clause. That will affect which exception type the AUTO
			 * scripts will receive.
			 */
//        } catch (final JAXBException e) {
//            throw new ICmdException(e);
//        } catch (final ParserConfigurationException e) {
//        	throw new ICmdException(e);
            
            
        } catch (final NestedRuntimeException e) {
        	/* R8 Refactor TODO - I have tried to do this correctly according to Josh's
        	 * comment above, but it really needs to be tested with AUTO.
        	 */
        	if (e.getMostSpecificCause() instanceof JAXBException) {
        		 throw new ICmdException(e);
        	} else if (e.getMostSpecificCause() instanceof ParserConfigurationException) {
        		throw new ICmdException(e);
        	}
        }
    }

    private void startServices() throws ICmdException {
        if (this.logToDb) {
            // We need to make a new session or fragment.
            // Otherwise, all we do is log.

        	this.archiveController = appContext.getBean(IDbSqlArchiveController.class);
            this.archiveController.addNeededStore(ILogMessageLDIStore.STORE_IDENTIFIER);

            // Not integrated chill
            this.archiveController.startAllStores();
        }

        if (appContext.getBean(SecurityProperties.class).getEnabled()) {
            AccessControl ac = null;
            final AccessControlParameters accessParams = appContext.getBean(AccessControlParameters.class);        		
            try {
                //  use username supplied. If none, use login user

            	// Set username if empty
            	final String user =
                        this.username == null || this.username.isEmpty() ?
                        		this.config.getContextId().getUser() : this.username;

                ac =
                        AccessControl.createAccessControl(
                        		appContext.getBean(SecurityProperties.class),
                        		user,
                                accessParams.getUserRole(),
                                accessParams.getLoginMethod(),
                                accessParams.getKeytabFile(), false, null,
                                                          cmdDirTracer);

            } catch (final AccessControlException ace) {
                throw new AuthenticationException(
                        "Error encountered while attempting to authenticate user",
                        ace);
            }

            try {
                ac.requestSsoToken();

                // Now get the real user

                accessParams.setUserId(ac.getUser());
            } catch (final AccessControlException ace) {
                throw new AuthenticationException(
                        "Unable to authenticate user", ace);
            }
        } else {
            this.logger.info("Access control is disabled");
        }
    }

    private void stopServices() {
        if (this.archiveController != null) {
            this.archiveController.shutDown();
        }
    }

    /**
     * Get the last received response from the command server
     *
     * @return the last received response from the command server
     */
    public final CpdResponse getResponse() {
        return this.response;
    }
    
    /**
     * @return the Spring Application Context
     */
    public ApplicationContext getApplicationContext() {
    	return this.appContext;
    }

    /**
     * Get username used for authentication
     *
     * @return username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Main method for this application.
     *
     * @param args list of user supplied command line arguments
     */
    @SuppressWarnings("DM_EXIT")
    public static void main(final String[] args) {
        final CommandDirectiveApp theApp = new CommandDirectiveApp();
        Exception ex = null;
        int statusCode = 0;

        try {
            final CommandLine commandLine =
                    ReservedOptions.parseCommandLine(args, theApp);
            theApp.configure(commandLine);
            theApp.run();
        } catch (final ParseException e) {
            ex = e;
            /* Error codes are stored in
             * ErrorCode enum now */
            statusCode = ErrorCode.BAD_REQUEST_ERROR_CODE.getNumber();
        } catch (final ICmdException e) {
            ex = e;
            statusCode = ICmdErrorManager.getErrorCode(e);
        } 
        catch (final Exception e) {
            ex = e;
            /* Error codes are stored in
             * ErrorCode enum now */
            statusCode = ErrorCode.GENERIC_ICMD_ERROR_CODE.getNumber();
        } finally {
            if (ex != null) {
                if (theApp.debugMode) {
                    ex.printStackTrace();
                }

                if ((theApp.logger != null) && !(ex instanceof ParseException)) {
                    // Removed throwable from uplinkLogger, hides stack trace from console
                    // Added second log with SUPPRESS marker and throwable so stack traces appear in the log file
                    theApp.logger.error(statusCode + ": " + ExceptionTools.getMessage(ex));
                    TraceManager.getDefaultTracer().error(Markers.SUPPRESS,statusCode + ": " + ExceptionTools.getMessage(ex), ex); 
                }
            }

            theApp.stopServices();
        }

        System.exit(statusCode);
    }
}
