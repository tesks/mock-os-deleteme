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
package jpl.gds.app.tools.integrated;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.bootstrap.ChannelLadBootstrapConfiguration;
import jpl.gds.common.config.bootstrap.options.ChannelLadBootstrapCommandOptions;
import jpl.gds.common.config.connection.ConnectionKey;
import jpl.gds.common.config.connection.DownlinkFileConnection;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.security.options.AccessControlCommandOptions;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.common.options.DownlinkAutostartOption;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.options.SubscriberTopicsOption;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.options.GladClientCommandOptions;
import jpl.gds.globallad.options.GladRestPortOption;
import jpl.gds.globallad.options.GladServerHostOption;
import jpl.gds.globallad.options.GladSocketPortOption;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.perspective.ApplicationConfiguration;
import jpl.gds.perspective.ApplicationType;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.options.PerspectiveCommandOptions;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.process.StderrLineHandler;
import jpl.gds.shared.process.StdoutLineHandler;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.telem.input.api.config.BufferedInputModeType;
import jpl.gds.telem.input.api.config.BufferedInputModeTypeOption;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;
/**
 * The PerspectiveApp class is the main entry point into the Chill 
 * integrated GUI. This class will spawn the necessary processes to
 * run all the GUI applications in a user's perspective.
 * replaced password file with keytab file
 */
public class PerspectiveApp extends AbstractCommandLineApp implements IQuitSignalHandler
{

    private static final int SUCCESS = 0;
    private static final int CMD_LINE_ERROR = 1;
    private static final int OTHER_ERROR = 2;

    private static final String NOT_EXECUTING = "Not executing ";
    private static final String TWO_DASHES = "--";

    private final Tracer trace;
    private final List<ProcessLauncher> childProcesses;
    private final IContextConfiguration testConfig;
    private int exitCode = SUCCESS;
    private boolean autoStart = false;
    private MessageServiceCommandOptions jmsOptions;
    private DatabaseCommandOptions dbOptions;
    private PerspectiveCommandOptions perspectiveOptions;
    private SessionCommandOptions sessionOptions;
    private AccessControlCommandOptions accessOptions;
    private final DownlinkAutostartOption autostartOption = new DownlinkAutostartOption();
    private BufferedInputModeTypeOption bufferOption;
    private GladClientCommandOptions gladClientOptions;

    private ChannelLadBootstrapCommandOptions bootstrapOptions;
    private final ChannelLadBootstrapConfiguration  bootstrapConfig;

    private CommandUserRole role = null;
    private LoginEnum loginMethod = null;
    private String keytabFile = null;
    private String username = null;

    private BufferedInputModeType bufferInMode = null;

    private final ApplicationContext appContext;
    private final SseContextFlag                   sseFlag;

    /**
     * Creates an instance of PerspectiveApp.
     */
    public PerspectiveApp()
    {
        appContext = SpringContextFactory.getSpringContext(true);
        childProcesses = new Vector<>();
        testConfig = new SessionConfiguration(appContext);
        trace = TraceManager.getTracer(appContext, Loggers.UTIL);
        bootstrapConfig = appContext.getBean(ChannelLadBootstrapConfiguration.class);
        sseFlag = appContext.getBean(SseContextFlag.class);
    }

    /**
     * Gets the current Spring Application Context.
     *
     * @return ApplicationContext
     */
    public ApplicationContext getAppContext() {
        return this.appContext;
    }

    /**
     * Gets the current auto start flag.
     *
     * @return true or false
     */
    public boolean isAutostart() {
        return this.autoStart;
    }

    /**
     * Gets the current input buffering mode.
     *
     * @return BufferedInputModeType
     */
    public BufferedInputModeType getBufferMode() {
        return this.bufferInMode;
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public void exitCleanly() {

        trace.info("Integrated GUI received a shutdown request.  Shutting down gracefully...");

        try {
            for (int i = 0; i < childProcesses.size(); i++) {
                childProcesses.get(i).destroy();
            }
        } catch (final Exception e) {
            // don't care
        }

    }

    /**
     * Main entry method.
     *
     * @param args
     *            main arguments
     *
     */
    public static void main(final String[] args) {
        final PerspectiveApp app = new PerspectiveApp();
        try {
            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
            app.configure(commandLine);
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(e));
            System.exit(CMD_LINE_ERROR);
        }
        app.run();
        System.exit(app.getExitCode());
    }

    /**
     * Gets the exit code for the application. The code is set by the run() method().
     * @return the exit code: SUCCESS or OTHER_ERROR
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Main application processing; starts all the applications in the perspective.
     *
     */
    public void run()
    {

        // Load user perspective. If it does not exist, the files will be created
        // under the user's home directory. If the perspective is not new, assign a new unique 
        // application ID that will tie all the spawned applications together.

        final PerspectiveConfiguration userPerspective = appContext.getBean(PerspectiveConfiguration.class);
        if (userPerspective.getAppId() == null) {
            userPerspective.assignNewAppId();
            try {
                userPerspective.writeAppId();
            } catch (final IOException e) {
                trace.error("Cannot write application ID file. Cannot execute");
                exitCode = OTHER_ERROR;
                return;
            }
        }

        // Get application configurations from perspective
        final ApplicationConfiguration[] configs = userPerspective.getApplicationConfigurations();

        final Thread[] childThreads = new ProcessThread[configs.length];

        boolean isFromChillWeb = jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName().contains("_web");
        // Execute each configured application
        int threadCount = 0;
        for (int index = 0; index < configs.length; index++)
        {
            trace.debug(jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName(),
                        " reading config for ", configs[index].getApplicationType());

            /* skip chill_down launching if master app is chill_web */
            if (configs[index].getApplicationType().equals(ApplicationType.DOWNLINK) && isFromChillWeb) {
                trace.debug(NOT_EXECUTING , configs[index].getAppExeName(), " because master application ",
                            " does not support ", configs[index].getAppExeName());
                continue;
            }

            // If the user did not elect to run SSE-only apps,
            // and this application config is for an SSE-only app,
            // DO NOT run it.
            if (!((SessionConfiguration) testConfig).getRunSse().isSseDownlinkEnabled() && configs[index].isSseOnly()) {
                trace.debug(NOT_EXECUTING + "SSE " + configs[index].getAppExeName() + " because user did not elect to run it");
                continue;
            }
            // Regardless of what is configured, do not run SSE only apps of the
            // current mission does not have an sse.
            if (!appContext.getBean(MissionProperties.class).missionHasSse() && configs[index].isSseOnly()) {
                trace.debug(NOT_EXECUTING + configs[index].getAppExeName() + " because mission does not have an sse");
                continue;
            }

            // Regardless of what is configured, do not run SSE only apps if venue is OPS.
            if (appContext.getBean(IVenueConfiguration.class).getVenueType().isOpsVenue() && configs[index].isSseOnly()) {
                trace.debug(NOT_EXECUTING + configs[index].getAppExeName() + " because this is an OPS venue");
                continue;
            }

            // Regardless of what is configured, do not run SSE only apps if downlink connection type is FILE.

            if (testConfig.getConnectionConfiguration().
                    get(ConnectionKey.FSW_DOWNLINK) instanceof DownlinkFileConnection
                    && configs[index].isSseOnly())
            {
                trace.debug(NOT_EXECUTING              +
                                    configs[index].getAppExeName() +
                                    " because downlink connection type is FILE");
                continue;
            }

            // Regardless of what is configured, do not run Uplink apps if mission has no uplink

            if (configs[index].getApplicationType().equals(ApplicationType.UPLINK) &&
                    !appContext.getBean(MissionProperties.class).isUplinkEnabled())
            {
                trace.debug(NOT_EXECUTING + configs[index].getAppExeName() + " because uplink is not supported");
                continue;
            }

            // If the user did not elect to run FSW-only apps,
            // and this application config is for an FSW-only app,
            // DO NOT run it.
            if (!((SessionConfiguration) testConfig).getRunFsw().isFswDownlinkEnabled() && configs[index].isFswOnly()) {
                trace.debug(NOT_EXECUTING + "FSW " + configs[index].getAppExeName() + " because user did not elect to run it");
                continue;
            }
            childThreads[threadCount++] = executeApplication(configs[index]);
        }

        for (int i = 0; i < threadCount; i++)
        {
            try
            {
                if (childThreads[i] != null)
                {
                    SleepUtilities.fullJoin(childThreads[i]);
                }
            }
            catch (final ExcessiveInterruptException eie)
            {
                trace.error("PerspectiveApp.run Error joining: " +
                                    rollUpMessages(eie));
            }
        }
    }


    /**
     * Executes a chill application in a subprocess
     * @param configuration the application configuration for the new application 
     */
    private Thread executeApplication(final ApplicationConfiguration configuration)
    {
        if (configuration == null) {
            trace.error("Application configuration is null, cannot execute");
            exitCode = OTHER_ERROR;
            return null;
        }

        trace.debug("Executing application configuration " + configuration.getName());

        final ProcessThread pThread = new ProcessThread(configuration);
        pThread.start();
        return pThread;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
    @Override
    @SuppressWarnings({"DM_EXIT"})
    public void configure(final ICommandLine commandLine) throws ParseException
    {
        super.configure(commandLine);

        autoStart = autostartOption.parse(commandLine);
        sessionOptions.SESSION_CONFIGURATION.parse(commandLine, true);

        if (perspectiveOptions.PERSPECTIVE_NULL_OVERWRITE.parse(commandLine) == null) {
            final PerspectiveConfiguration pc = appContext.getBean(PerspectiveConfiguration.class);
            pc.create();
        }

        if (BaseCommandOptions.DEBUG.parse(commandLine)) {
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.TRACE);
        }

        jmsOptions.parseAllOptionsAsOptional(commandLine);
        dbOptions.parseAllOptionsAsOptional(commandLine);

        /* Access control options must be optionally parsed, because they all cause defaults
         * to be set.  We do not want the defaults if nothing was passed in.
         */
        if (commandLine.hasOption(accessOptions.USER_ROLE.getLongOpt())) {
            role = accessOptions.USER_ROLE.parse(commandLine);
        }
        if (commandLine.hasOption(accessOptions.USER_ID.getLongOpt())) {
            username = accessOptions.USER_ID.parse(commandLine);
        }
        if (commandLine.hasOption(accessOptions.LOGIN_METHOD_GUI.getLongOpt())) {
            loginMethod = accessOptions.LOGIN_METHOD_GUI.parse(commandLine);
        }
        if (commandLine.hasOption(accessOptions.KEYTAB_FILE.getLongOpt())) {
            keytabFile = accessOptions.KEYTAB_FILE.parse(commandLine);
        }

        bufferInMode = bufferOption.parse(commandLine);

        bootstrapOptions.parseBootstrapOptions(commandLine);
        gladClientOptions.SERVER_HOST_OPTION.parseWithDefault(commandLine, false, true);
        gladClientOptions.REST_PORT_OPTION.parseWithDefault(commandLine, false, true);
        gladClientOptions.SOCKET_PORT_OPTION.parseWithDefault(commandLine, false, true);

        bootstrapOptions.parseBootstrapOptions(commandLine);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
     */
    @Override
    public BaseCommandOptions createOptions() {

        if (optionsCreated.get()) {
            return options;
        }

        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));

        options.addOption(BaseCommandOptions.DEBUG);

        dbOptions = new DatabaseCommandOptions(appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
        options.addOptions(dbOptions.getAllLocationOptions());

        jmsOptions = new MessageServiceCommandOptions(appContext.getBean(MessageServiceConfiguration.class));
        options.addOptions(jmsOptions.getAllOptionsWithoutNoJms());

        perspectiveOptions = new PerspectiveCommandOptions(appContext.getBean(PerspectiveConfiguration.class), ApplicationType.UNKNOWN);
        options.addOption(perspectiveOptions.PERSPECTIVE_NULL_OVERWRITE);

        sessionOptions = new SessionCommandOptions((SessionConfiguration) this.testConfig);
        options.addOption(sessionOptions.SESSION_CONFIGURATION);

        accessOptions = new AccessControlCommandOptions(appContext.getBean(SecurityProperties.class), testConfig.getAccessControlParameters());
        options.addOptions(accessOptions.getAllGuiOptions());

        options.addOption(autostartOption);

        // Allow buffer command line option
        bufferOption = new BufferedInputModeTypeOption(false, appContext.getBean(TelemetryInputProperties.class), false,
                                                       false);
        options.addOption(bufferOption);
        // GLAD host/port options
        gladClientOptions = new GladClientCommandOptions(GlobalLadProperties.getGlobalInstance());
        options.addOption(gladClientOptions.SERVER_HOST_OPTION);
        options.addOption(gladClientOptions.REST_PORT_OPTION);
        options.addOption(gladClientOptions.SOCKET_PORT_OPTION);

        bootstrapOptions = new ChannelLadBootstrapCommandOptions(bootstrapConfig);
        options.addOptions(bootstrapOptions.getBootstrapCommandOptions());


        return options;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final BaseCommandOptions options = createOptions();

        final PrintWriter pw = new PrintWriter(System.out);

        pw.println("Usage: " + jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName()
                           +  " --sessionConfig <filename> [--perspective <directory>]");

        options.getOptions().printOptions(pw);
    }

    /**
     * ProcessThread is a class responsible for spawning a chill application.
     */
    class ProcessThread extends Thread {
        private final ApplicationConfiguration appConfig;
        private final boolean isSse;

        /**
         * Creates an instance of ProcessThread to spawn the application
         * with the given configuration.
         * @param app the ApplicationConfiguration object for the new 
         * application
         */
        ProcessThread(final ApplicationConfiguration app) {
            appConfig = app;
            isSse = app.isSseOnly() || sseFlag.isApplicationSse();
        }

        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            final String userDir = GdsSystemProperties.getUserConfigDir();
            final String tempFile = userDir + File.separator + appContext.getBean(GeneralProperties.class).getDefaultContextConfigFileName();

            final ArrayList <String> argList = new ArrayList<>();

            argList.add(TWO_DASHES + perspectiveOptions.APPLICATION_CONFIGURATION.getLongOpt());
            argList.add(appContext.getBean(PerspectiveConfiguration.class).getAppIdFileName());

            argList.add(TWO_DASHES + SessionCommandOptions.SESSION_CONFIG_LONG);
            argList.add(tempFile);

            argList.add(TWO_DASHES + BaseCommandOptions.AUTORUN.getLongOpt());

            if (appConfig.getApplicationType().usesMessageService()) {
                argList.add(TWO_DASHES + jmsOptions.JMS_HOST.getLongOpt());
                argList.add(appContext.getBean(MessageServiceConfiguration.class).getMessageServerHost());
                argList.add(TWO_DASHES + jmsOptions.JMS_PORT.getLongOpt());
                argList.add(Integer.toString(appContext.getBean(MessageServiceConfiguration.class).getMessageServerPort()));
            }

            if (appConfig.getApplicationType().equals(ApplicationType.DOWNLINK)) {
                if (autoStart) {
                    argList.add(TWO_DASHES + autostartOption.getLongOpt());
                }

                argList.add(TWO_DASHES + BaseCommandOptions.GUI.getLongOpt());

                // pass the buffer mode into the downlink app
                if(bufferInMode != null){
                    argList.add(TWO_DASHES + bufferOption.getLongOpt());
                    argList.add(bufferInMode.toString());
                }
                if (bootstrapConfig.getLadBootstrapFlag()) {
                    argList.add(TWO_DASHES + ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_LONG);
                }
                if (!bootstrapConfig.getLadBootstrapIds().isEmpty()) {
                    argList.add(TWO_DASHES + ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_IDS_LONG);
                    argList.add(bootstrapConfig.getLadBootstrapIds().toString());
                }

                // Global LAD location options for the downlink app
                argList.add(TWO_DASHES + GladServerHostOption.LONG_OPTION);
                argList.add(GlobalLadProperties.getGlobalInstance().getServerHost());
                argList.add(TWO_DASHES + GladSocketPortOption.LONG_OPTION);
                argList.add(String.valueOf(GlobalLadProperties.getGlobalInstance().getSocketServerPort()));

            }

            if (appConfig.getApplicationType().equals(ApplicationType.MONITOR)) {
                argList.add(TWO_DASHES + SubscriberTopicsOption.LONG_OPTION);
                argList.add(testConfig.getGeneralInfo().getRootPublicationTopic());

                // Global LAD location options for the monitor app
                argList.add(TWO_DASHES + GladServerHostOption.LONG_OPTION);
                argList.add(GlobalLadProperties.getGlobalInstance().getServerHost());
                argList.add(TWO_DASHES + GladRestPortOption.LONG_OPTION);
                argList.add(String.valueOf(GlobalLadProperties.getGlobalInstance().getRestPort()));
            }

            if (appConfig.getApplicationType().usesDatabase()) {
                argList.add(TWO_DASHES + dbOptions.DATABASE_HOST.getLongOpt());
                argList.add(appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class).getHost());
                argList.add(TWO_DASHES + dbOptions.DATABASE_PORT.getLongOpt());
                argList.add(Integer.toString(appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class).getPort()));
            }

            if (appConfig.getApplicationType().equals(ApplicationType.UPLINK)) {

                if (appContext.getBean(SecurityProperties.class).getEnabled()) {
                    final AccessControlParameters acp = accessOptions.getAccessControlParameters();
                    if (role != null) {
                        argList.add(TWO_DASHES+ accessOptions.USER_ROLE.getLongOpt());
                        argList.add(acp.getUserRole().name());
                    }
                    if (loginMethod != null) {
                        argList.add(TWO_DASHES+ accessOptions.LOGIN_METHOD_GUI.getLongOpt());
                        argList.add(acp.getLoginMethod().name());
                    }
                    if (keytabFile != null) {
                        argList.add(TWO_DASHES+ accessOptions.KEYTAB_FILE.getLongOpt());
                        argList.add(acp.getKeytabFile());
                    }
                    if (username != null) {
                        argList.add(TWO_DASHES+ accessOptions.USER_ID.getLongOpt());
                        argList.add(acp.getUserId());
                    }
                }

            }

            final String[] arr = new String[argList.size() + 1];

            final String gdsDirectory = GdsSystemProperties.getSystemProperty(GdsSystemProperties.DIRECTORY_PROPERTY);

            //remove the mission name from the script name (BRN)
            String mission = "";
            if (isSse) {
                mission = "sse_";
            }
            final File gdsDir = new File(gdsDirectory);
            final String fullPath = gdsDir.getAbsolutePath();

            final String shortMission = mission.toLowerCase();
            arr[0] = fullPath + "/bin/" + shortMission + appConfig.getAppExeName();

            for (int i = 1; i < arr.length; i++) {
                arr[i] = argList.get(i - 1);
            }

            printDebugCommandLine(arr);

            final ProcessLauncher launcher = new ProcessLauncher();
            launcher.setOutputHandler(new MyOutHandler());
            launcher.setErrorHandler(new MyErrorHandler());

            try {
                /* Check for launch failure */
                if (!launcher.launch(arr, GdsSystemProperties.getSystemProperty("user.dir"))) {
                    trace.error("Error running process: " + launcher);
                    exitCode = OTHER_ERROR;
                    return;
                }
                childProcesses.add(launcher);
            }
            catch (final IOException e) {
                trace.error("Error running process: " + e.getMessage());
                exitCode = OTHER_ERROR;
                return;
            }
            final int procExitCode = launcher.waitForExit();
            if (procExitCode != 0) {
                trace.error("Script " + arr[0] + " returned exit code " + procExitCode);
            }
        }

        /**
         * MyOutHandler attaches to the stdout stream of a process
         * and displays any output to the console.
         */
        class MyOutHandler extends StdoutLineHandler {

            /* (non-Javadoc)
             * @see jpl.gds.process.LineHandler#handleLine(java.lang.String)
             */
            @Override
            public void handleLine(final String line) throws IOException
            {
                String prefix = "";
                if (appConfig.isFswOnly()) {
                    prefix = "FSW ";
                } else if (appConfig.isSseOnly()) {
                    prefix = "SSE ";
                }
                System.out.println(prefix + appConfig.getApplicationType() + ": " + line);
                System.out.flush();
            }
        }

        /**
         * MyErrorHandler attaches to the stderr stream of a process
         * and displays any output to the console.
         */
        class MyErrorHandler extends StderrLineHandler {

            /* (non-Javadoc)
             * @see jpl.gds.process.LineHandler#handleLine(java.lang.String)
             */
            @Override
            public void handleLine(final String line) throws IOException
            {
                // I can find no other way to get rid of the obnoxious "broken pipe" errors
                // that come out of chill_ping_jms if JMS is not up.
                if (line.contains("chill_ping_jms")) {
                    return;
                }
                String prefix = "";
                if (appConfig.isFswOnly()) {
                    prefix = "FSW ";
                } else if (appConfig.isSseOnly()) {
                    prefix = "SSE ";
                }
                System.err.println(prefix + appConfig.getApplicationType() + ": " + line);
                System.err.flush();
            }
        }
    }

    /**
     * Print a debug log of a command line argument array.
     * @param args list of command line arguments
     */
    private void printDebugCommandLine(final String[] args) {
        if (args.length == 0) {
            return;
        }

        final StringBuilder builder = new StringBuilder(args[0]);
        for (int i = 1; i < args.length; i++) {
            builder.append(' ');
            builder.append(args[i]);
        }
        trace.info("Execing command: " + builder.toString());
    }

}
