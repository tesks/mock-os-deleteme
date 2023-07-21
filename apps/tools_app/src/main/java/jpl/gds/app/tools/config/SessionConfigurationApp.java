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
package jpl.gds.app.tools.config;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.bootstrap.ChannelLadBootstrapConfiguration;
import jpl.gds.common.config.bootstrap.options.ChannelLadBootstrapCommandOptions;
import jpl.gds.common.config.connection.options.ConnectionCommandOptions;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.security.options.AccessControlCommandOptions;
import jpl.gds.common.options.DownlinkAutostartOption;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.options.GladClientCommandOptions;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.options.MessageServiceCommandOptions;
import jpl.gds.perspective.ApplicationType;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.options.PerspectiveCommandOptions;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.SessionConfigurationValidator;
import jpl.gds.session.config.gui.SessionConfigShell;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.telem.input.api.config.BufferedInputModeTypeOption;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import org.apache.commons.cli.ParseException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * SessionConfigurationApp is an application that takes all the fields in a test
 * configuration as optional command line arguments, displays the test
 * configuration GUI (with the command line values as overrides) and saves the
 * resulting test configuration to a file. A command line option to skip the GUI
 * and just write the test configuration is provided.
 *
 * replaced password file with keytab file
 * SPV is now enabled by default
 *  Changed static flag to local and old conditional code to use SPV or not
 */

public class SessionConfigurationApp extends AbstractCommandLineApp
{
    /** Short option name for output file */
    public static final String OUTPUT_FILE_SHORT = "u";
    /** Long option name for output file */
    public static final String OUTPUT_FILE_LONG = "outputFile";

    private boolean autoRun;
    private final SessionConfiguration testConfig;
    private String outputFile;
    private boolean cancelled = false;
    private final boolean isIntegrated;
    private final boolean isWebApp;
    private final Tracer log;

    private SessionCommandOptions sessionOpts;
    private ConnectionCommandOptions connectionOpts;
    private DictionaryCommandOptions dictOpts;
    private PerspectiveCommandOptions perspectiveOpts;
    private MessageServiceCommandOptions jmsOptions;
    private DatabaseCommandOptions dbOptions;
    private AccessControlCommandOptions accessOptions;
    private ContextCommandOptions        contextOptions;
    /* Add global LAD port/host options */
    private GladClientCommandOptions           gladClientOptions;

    private ChannelLadBootstrapCommandOptions bootstrapOptions;
    private DownlinkAutostartOption autostartOption;

    private final ApplicationContext appContext;

    private final FileOption outputFileOption = new FileOption(OUTPUT_FILE_SHORT, OUTPUT_FILE_LONG,
                                                               "filename", "output test configuration file", false, false);

    /**
     * Creates an instance of SessionConfigurationApp.
     */
    public SessionConfigurationApp() {
        super();

        appContext = SpringContextFactory.getSpringContext(true);
        testConfig = new SessionConfiguration(appContext);

        // This indicates whether this application is running as prelude
        // to the integrated GUI
        isIntegrated = GdsSystemProperties.isIntegratedGui();
        isWebApp = ApplicationConfiguration.getApplicationName().contains("_web");
        log = TraceManager.getTracer(appContext, Loggers.DEFAULT);
    }

    /**
     * Gets the current application context in use by this application.
     *
     * @return application context
     */
    public ApplicationContext getApplicationContext() {
        return this.appContext;
    }


    /**
     * Creates an Options object containing possible command line arguments/options.
     * @return the Options object
     */
    @Override
    public BaseCommandOptions createOptions() {

        if (optionsCreated.get()) {
            return options;
        }

        super.createOptions(appContext.getBean(BaseCommandOptions.class, this));

        sessionOpts = new SessionCommandOptions(testConfig);
        options.addOptions(sessionOpts.getAllUplinkAndDownlinkOptions());
        options.addOption(sessionOpts.SUPPRESS_FSW_DOWN);
        options.addOption(sessionOpts.SUPPRESS_SSE_DOWN);

        dictOpts = new DictionaryCommandOptions(testConfig.getDictionaryConfig());
        options.addOptions(dictOpts.getAllOptions());

        connectionOpts = new ConnectionCommandOptions(testConfig.getConnectionConfiguration());
        options.addOptions(connectionOpts.getAllOptions());

        options.addOption(BaseCommandOptions.AUTORUN);
        autostartOption = new DownlinkAutostartOption();

        if (!isIntegrated) {
            options.addOption(outputFileOption);
        } else {
            perspectiveOpts = new PerspectiveCommandOptions(appContext.getBean(PerspectiveConfiguration.class), ApplicationType.UNKNOWN);
            options.addOption(perspectiveOpts.PERSPECTIVE);
            options.addOption(autostartOption);
        }

        jmsOptions = new MessageServiceCommandOptions(appContext.getBean(MessageServiceConfiguration.class));
        options.addOptions(jmsOptions.getAllOptionsWithoutNoJms());

        dbOptions = new DatabaseCommandOptions(appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
        options.addOptions(dbOptions.getAllLocationOptions());

        accessOptions = new AccessControlCommandOptions(appContext.getBean(SecurityProperties.class), testConfig.getAccessControlParameters());
        options.addOptions(accessOptions.getAllGuiOptions());

        // allow buffer command line option (for passing through to perspective app)
        options.addOption(new BufferedInputModeTypeOption(false, appContext.getBean(TelemetryInputProperties.class),
                                                          false, false));

        contextOptions = new ContextCommandOptions(testConfig);
        options.addOption(contextOptions.PUBLISH_TOPIC);

        // Add GLAD host/port options (for passing through to perspective app)
        gladClientOptions = new GladClientCommandOptions(GlobalLadProperties.getGlobalInstance());
        options.addOption(gladClientOptions.SERVER_HOST_OPTION);
        options.addOption(gladClientOptions.REST_PORT_OPTION);
        options.addOption(gladClientOptions.SOCKET_PORT_OPTION);

        bootstrapOptions = new ChannelLadBootstrapCommandOptions(appContext.getBean(ChannelLadBootstrapConfiguration.class));
        options.addOptions(bootstrapOptions.getBootstrapCommandOptions());

        // Add noGUI option for TI/TP support via chill_web and chill_down_web
        // Also allow chill_web to use --noGUI for WSTS
        // --noGUI should NOT be supported by the legacy chill application.
        if (isWebApp) {
            options.addOption(BaseCommandOptions.NO_GUI);
        }


        return options;
    }

    /**
     * Parses the command line arguments, set appropriate flags, creates the
     * session configuration.
     * @param commandLine a CommandLine object initialized using the current
     *            command line arguments.
     * @throws ParseException Thrown if unable to parse the command line.
     */
    @Override
    @SuppressWarnings({"DM_EXIT" })
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        autoRun = BaseCommandOptions.AUTORUN.parse(commandLine);

        /* Remove the validation argument from this call */
        sessionOpts.parseAllUplinkAndDownlinkOptionsAsOptional(commandLine, true, autoRun);

        /* Added back parse of FSW/SSE supression options */
        sessionOpts.SUPPRESS_FSW_DOWN.parse(commandLine);
        sessionOpts.SUPPRESS_SSE_DOWN.parse(commandLine);

        if (perspectiveOpts != null) {
            perspectiveOpts.PERSPECTIVE.parse(commandLine);
        }

        jmsOptions.parseAllOptionsAsOptional(commandLine);

        dbOptions.parseAllOptionsAsOptional(commandLine);

        accessOptions.parseAllGuiOptionsAsOptional(commandLine);

        bootstrapOptions.parseBootstrapOptions(commandLine);

        final boolean haveConfigFile = commandLine.hasOption(sessionOpts.SESSION_CONFIGURATION.getLongOpt());

        if (haveConfigFile) {

            // Only integrated session configuration files should be accepted by this application.
            final boolean missionHasUplink = appContext.getBean(MissionProperties.class).isUplinkEnabled();
            if (testConfig.isDownlinkOnly() && missionHasUplink) {
                throw new ParseException("Supplied session configuration file is for a downlink-only " +
                                                 "configuration and cannot be used by this integrated uplink/downlink application");
            }
            if (testConfig.isUplinkOnly()) {
                if (missionHasUplink) {
                    throw new ParseException("Supplied session configuration file is for an uplink-only " +
                                                     "configuration and cannot be used by this integrated uplink/downlink application");
                } else {
                    throw new ParseException("Supplied session configuration file is for an uplink-only " +
                                                     "configuration and this mission does not perform uplink via AMPCS");
                }
            }

            // set host local host
            testConfig.clearFieldsForNewConfiguration();
        }

        final IVenueConfiguration venueConfig = appContext.getBean(IVenueConfiguration.class);

        // If venue present, set default hosts and ports
        if (commandLine.hasOption(sessionOpts.VENUE_TYPE.getLongOpt())) {
            testConfig.getConnectionConfiguration().
                    setDefaultNetworkValuesForVenue(venueConfig.getVenueType(),
                                                    venueConfig.getTestbedName(), venueConfig.getDownlinkStreamId());
        }

        if (!haveConfigFile) {
            dictOpts.parseAllOptionsAsOptionalWithDefaults(commandLine);

            // Now parse hosts and ports from command line
            testConfig.getConnectionConfiguration().setDefaultNetworkValuesForVenue(venueConfig.getVenueType(),
                                                                                    venueConfig.getTestbedName(), venueConfig.getDownlinkStreamId());
            connectionOpts.parseAllOptionsAsOptional(commandLine);

            // Integrated chill --uplinkConnectionType wasnt working. Added this new method to fix it.
            if(isIntegrated) {
                connectionOpts.parseUplinkConnectionTypeOption(commandLine);
            }
        }

        final String publishTopic = contextOptions.PUBLISH_TOPIC.parse(commandLine);
        if (publishTopic != null) {
            log.debug("Setting root topic ",
                      contextOptions.PUBLISH_TOPIC.getLongOpt(), "=", publishTopic);
        }

        outputFile = outputFileOption.parse(commandLine);

        // Add GLAD parsing of host/port options
        gladClientOptions.SERVER_HOST_OPTION.parseWithDefault(commandLine, false, true);
        gladClientOptions.REST_PORT_OPTION.parseWithDefault(commandLine, false, true);
        gladClientOptions.SOCKET_PORT_OPTION.parseWithDefault(commandLine, false, true);

        /* Validate the final session configuration */
        final SessionConfigurationValidator scv = new SessionConfigurationValidator(testConfig);

        if (!scv.validate(false, autoRun)) {
            throw new ParseException(scv.getErrorsAsMultilineString());
        }

    }

    /**
     * Executes the logic of the application.
     */
    @SuppressWarnings({"DM_EXIT" })
    public void run() {
        Display.setAppName("MPCS Session Configuration");

        if (!autoRun) {
            final Display mainDisplay = checkDisplay();

            final boolean showUplink = appContext.getBean(MissionProperties.class).isUplinkEnabled();

            /* Pass session config */

            final SessionConfigShell ts =
                    new SessionConfigShell(appContext, mainDisplay, showUplink, true, testConfig);

            /*
             * Added Dispose listener for sigterm handling.
             * The assumption is if the user did not cancel the gui (click 'X'/'Exit' button or 'Exit' via drop-down)
             * AND they did not click 'Run Session', a sigerm signal was sent (ctrl+c).
             *
             * doGuiLoop() below safely loops forever while the gui is not disposed (sleeping when no events to dispatch).
             * Once the GUI shell is disposed, it could be one of three cases:
             *
             * 1) User exit via red 'X', 'Exit' button or drop-down item
             * 2) User selects 'Run Session'
             * 3) User sends sigterm (ctrl+c) to console
             *
             * Case 1 and 2 are the same:
             * - doGuiLoop() finishes and the 'cancelled' status is set.
             * - Return to main and call System.exit with an exit code based on cancelled state.
             * - When canceled is false (Run session clicked), exit with status 0 = nominal. Chill apps launch
             * - When canceled is true (user exit), exit with status 1 = ERROR. Do not launch chill apps
             *
             * Case 3: Is described here - https://bugs.eclipse.org/bugs/show_bug.cgi?id=56910
             * - User sends ctrl+c while doGuiLoop() is running, more than likely during a sleep.
             * - SWT calls exit(0) and disposes the GUI. There's nothing we can do about this.
             * - The code DOES NOT return from doGuiLoop(), instead there is an immediate shutdown from exit call.
             * - So I added this dispose listener and check cancelled AND run status.
             * - If Run was not pushed and user did not cancel, assume ctrl+c and call System.exit(130).
             * - Without System.exit(130), SWT exits with '0' and integrated chill continues to launch apps.
             */
            mainDisplay.addListener(SWT.Dispose, listener -> {
                if (!ts.wasCanceled() && !ts.wasRunClicked()) {
                    System.exit(130); // Prevents SWT from exiting with 0
                }
            });

            ts.open();

            doGuiLoop(mainDisplay, ts);

        } else {
            // Clear time-related fields from previous sessions.
            testConfig.clearFieldsForNewConfiguration();
        }
        if (!cancelled) {
            if (outputFile == null) {
                outputFile =
                        GdsSystemProperties.getUserConfigDir() + File.separator
                                + appContext.getBean(GeneralProperties.class).getDefaultContextConfigFileName();
            }
            boolean mkdirResult = false;
            try {

                final File f = new File(GdsSystemProperties.getUserConfigDir());
                mkdirResult = f.mkdirs();
                testConfig.save(outputFile);
            } catch (final IOException e) {
                if (mkdirResult) {
                    System.err.println("Error writing output file "
                                               + outputFile + ": " + e.toString());
                } else {
                    System.err
                            .println("Error no directories were created and unable"
                                             + " to write output file " + outputFile
                                             + ": " + e.toString());
                }

            }
        }
    }

    private Display checkDisplay() throws SWTError {
        try {
            return new Display();
        } catch (final SWTError e) {
            if (e.getMessage().indexOf("No more handles") != -1) {
                log.error("Unable to initialize user interface.");
                log.error("If you are using X-Windows," + " make sure your DISPLAY variable is set.");
                System.exit(1);
            } else {
                throw (e);
            }
        }
        return null;
    }

    private void doGuiLoop(final Display mainDisplay, final SessionConfigShell ts) {
        while (!ts.getShell().isDisposed()) {
            if (!mainDisplay.readAndDispatch()) {
                mainDisplay.sleep();
            }
        }

        if (ts.wasCanceled()) {
            cancelled = true;
        }

        mainDisplay.dispose();
    }

    /**
     * Display application arguments and options.
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final BaseCommandOptions options = createOptions();

        final PrintWriter pw = new PrintWriter(System.out);

        pw.print("Usage: " + ApplicationConfiguration.getApplicationName());
        pw.println(" [--autoRun --autoStart --sessionName <name> --fswVersion <version> --sessionConfig <filename>");
        pw.println("                          --fswDictionaryDir <directory> --sseDictionaryDir <directory>");
        pw.println("                          --downlinkConnectionType <type> --uplinkConnectionType <type>");
        pw.println("                          --sseVersion <version> --downlinkStreamID <stream ID> --sessionDescription <description>");
        pw.println("                          --sessionType <type> --fswUplinkPort <port> --sseUplinkPort <port>");
        pw.println("                          --fswDownlinkPort <port> --sseDownlinkPort <port> --fswHost <host>");
        pw.println("                          --sseHost <host> --spacecraftID <scid> --suppressFswDown");
        pw.println("                          --suppressSseDown --inputFormat <format> --venueType <venueType>");
        pw.println("                          --inputFile <filename> --testbedName <name>");
        pw.println("                          --bufferedInput <enabledIn> --jmsPort <port> --jmsHost <host>");
        pw.println("                          --subtopic <subtopic> --databaseHost <host> --databasePort <port>");
        pw.print("                            --sessionUser <username>" + (isIntegrated ? "" : " --outputFile <filename>]\n\n"));
        if (isIntegrated) {
            pw.println(" --perspective <perspectivePath>]\n");
        }

        pw.println("                   ");

        /* Add logic for chill_web help dialog */
        if (isWebApp) {
            setupChillWebHelp();
        }
        options.getOptions().printOptions(pw);

        if (!isIntegrated) {
            pw.println("\nBy default, session configuration output is written to $HOME/CHILL/" +
                               appContext.getBean(GeneralProperties.class).getDefaultContextConfigFileName() + ".");
            pw.println("Output location can be overridden using --outputFile.\n");
        }

        pw.flush();
    }

    private void setupChillWebHelp() {
        autostartOption.setDescription("Automatically start telemetry services after setup");
        sessionOpts.SUPPRESS_FSW_DOWN.setDescription("Disables the usage of FSW Downlink");
        sessionOpts.SUPPRESS_SSE_DOWN.setDescription("Disables the usage of SSE Downlink");
        BaseCommandOptions.NO_GUI.setDescription("Skips launching the MCGUI in a web browser");
    }

    /**
     * Indicates whether the test run was canceled by the user.
     * @return true if the test run was canceled
     */
    public boolean wasCancelled() {
        return cancelled;
    }

    /**
     * Main method for the application.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        try {
            final SessionConfigurationApp app = new SessionConfigurationApp();
            try {
                final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
                app.configure(commandLine);
            } catch(final ParseException e) {
                TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(e));
                System.exit(1);
            }
            app.run();
            if (app.wasCancelled()) {
                System.exit(1);
            } else {
                System.exit(0);
            }
        } catch (final Exception e) {
            TraceManager.getDefaultTracer().error("Unexpected exception: " + ExceptionTools.getMessage(e), e);
            System.exit(1);
        }
    }
}
