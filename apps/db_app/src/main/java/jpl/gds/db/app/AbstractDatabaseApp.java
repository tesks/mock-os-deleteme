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

package jpl.gds.db.app;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.sys.QuitSignalHandler;

/**
 * AbstractDatabaseApp is a base class for all database-related command line applications.
 * It is responsible for structuring how database applications will run and controlling their
 * common command line options.
 *
 */
public abstract class AbstractDatabaseApp implements Runnable, IQuitSignalHandler {
    /** Success status */
    public static final int                 SUCCESS        = 0;

    /** Command-line error status */
    public static final int                 CMD_LINE_ERROR = 1;

    /** Other error status */
    public static final int                 OTHER_ERROR    = 2;

    /** No-action specified error status */
    public static final int                 NO_ACTION      = 3;

    /** Default tracer */
    protected static final Tracer        trace          = TraceManager.getTracer(Loggers.DB_FETCH);

    /**
     * Application exit code. For getting return value from run().
     */
    protected int                           exitCode       = SUCCESS;

    /**
     * The list of command line options for this application
     */
    protected Options                       options;

    /**
     * The name of the primary database table that this application uses
     * (or null if there isn't one). Generally used for the retrieval
     * of database-specific velocity templates.
     */
    protected String                        tableName;

    /**
     * If true, debugging print statements are enabled, otherwise they are not
     */
    protected boolean                       debug;

    /**
     * If true, suppress all but FATAL trace messages to the console
     */
    protected boolean                       quiet;

    /**
     * The name of the current application (for help display).
     */
    protected String                        appName;

    /**
     * The Spring Application Context
     */
    protected ApplicationContext            appContext;

    /**
     * Database configuration.
     */
    protected IMySqlAdaptationProperties  dbProperties;

    /**
     * Mission Properties.
     */
    protected MissionProperties             missionProps;

    /*
     * Archive controller and order by factory members. should be
     * created based upon whether a fetch or store/update application
     * class extends this one.
     */

    /**
     * 
     * Creates an instance of AbstractDatabaseApp.
     * 
     * @param appName
     *            the application name
     */
    public AbstractDatabaseApp(final String appName) {

        Runtime.getRuntime().addShutdownHook(new Thread(new QuitSignalHandler(this)));
        
        this.appContext = SpringContextFactory.getSpringContext(true);
        ReservedOptions.setApplicationContext(appContext);

        this.dbProperties = appContext.getBean(IMySqlAdaptationProperties.class);
        this.missionProps = appContext.getBean(MissionProperties.class);


        this.tableName = null;
        this.debug = false;
        this.appName = appName;
        trace.setAppContext(appContext);

    }

    /**
     * 
     * Creates an instance of AbstractDatabaseApp.
     * 
     * @param tableName
     *            The primary database table used by this application
     * @param appName
     *            the application name
     */
    public AbstractDatabaseApp(final String tableName, final String appName) {
        this(appName);

        this.tableName = tableName;
    }

    /**
     * Sets the application exit code.
     * 
     * @param code
     *            the exit code
     */
    protected void setExitCode(final int code) {
        this.exitCode = code;
    }

    /**
     * Gets the application exit code.
     * 
     * @return the exit code
     */
    public int getExitCode() {
        return this.exitCode;
    }

    /**
     * Runs this object as an application. Since this function will use
     * System.exit when errors are encountered, it should only be called from
     * an application's "main" method.
     * 
     * @param args
     *            the command line arguments to the application
     * @return the return status of the application; 0 indicates success; non-zero indicates an error
     */
    public int runAsApp(final String[] args) {
        try {
            final CommandLine cmdline = parseCommandLine(args);
            configure(cmdline);
            configureApp(cmdline);

            final String[] leftoverArgs = cmdline.getArgs();
            if (leftoverArgs.length != 0) {
                final StringBuilder msg = new StringBuilder("Extra unrecognized text on the command line: ");
                for (int i = 0; i < leftoverArgs.length; i++) {
                    msg.append(leftoverArgs[i]).append(' ');
                }
                throw new ParseException(msg.toString());
            }
        }
        catch (final NullPointerException npe) {
            npe.printStackTrace();
            TraceManager.getDefaultTracer().error("Command line error: " + npe.getMessage());
            TraceManager.getDefaultTracer()
                        .info("Use the -" + ReservedOptions.HELP_SHORT_VALUE + " to display the application usage");
            return CMD_LINE_ERROR;
        }
        catch (final Exception e) {
            TraceManager.getDefaultTracer().error("Command line error: " + e.getMessage());
            TraceManager.getDefaultTracer()
                        .info("Use the -" + ReservedOptions.HELP_SHORT_VALUE + " to display the application usage");
            return CMD_LINE_ERROR;
        }

        if (this.debug) {
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.DEBUG);
        }
        else if (this.quiet) {
            TraceManager.getTracer(Loggers.AMPCS_ROOT_TRACER).setLevel(TraceSeverity.ERROR);
        }

        try {
            run();
        }
        catch (final Exception e) {
            e.printStackTrace();
            TraceManager.getDefaultTracer()
                        .error("Application error: " + e.getMessage() == null ? e.toString() : e.getMessage());
            return getExitCode();
        }

        return getExitCode();
    }

    /**
     * Parses the application command line. Takes care of displaying help
     * and version information if the user requests it.
     * 
     * @param args
     *            the list of command line arguments
     * 
     * @return a CommandLine object
     *
     * @throws ParseException
     *             Parse error
     */
    @SuppressWarnings({ "DM_EXIT" })
    public CommandLine parseCommandLine(final String[] args) throws ParseException {
        this.options = createOptions();
        addAppOptions();
        CommandLine commandLine = null;

        try {
            final CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(this.options, args);
        }
        catch (final MissingOptionException e) {
            boolean helpUsed = false;
            for (int i = 0; i < args.length; ++i) {
                if (args[i].equals(ReservedOptions.HELP_SHORT_VALUE)
                        || args[i].equals(ReservedOptions.HELP_LONG_VALUE)) {
                    showHelp();
                    helpUsed = true;
                }

                if (args[i].equals(ReservedOptions.VERSION_SHORT_VALUE)
                        || args[i].equals(ReservedOptions.VERSION_LONG_VALUE)) {
                    showVersion();
                    helpUsed = true;
                }
            }

            if (helpUsed) {
                System.exit(1);
            }
        }

        return (commandLine);
    }

    /**
     * Implemented by subclasses to configure the application from command
     * line parameters.
     * 
     * @param cmd
     *            the parsed CommandLine object
     * 
     * @throws ParseException
     *             if any command line error is found
     */
    public abstract void configureApp(CommandLine cmd) throws ParseException;

    /**
     * Implemented by subclasses to return command line usage text.
     * 
     * @return the usage text
     */
    public abstract String getUsage();

    /**
     * Default method for display application arguments and options.
     * Can be overriden by subclasses to create more specific help text.
     */
    protected void showHelp() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(70, getUsage(), "\n", this.options, "\n");
    }

    /**
     * Displays the application version.
     *
     */
    public void showVersion() {
        System.out.println(ReleaseProperties.getProductLine() + " " + this.appName + " "
                + ReleaseProperties.getVersion());
    }

    /**
     * Performs configuration of this DB application from a CommandLine object,
     * for the command line options in this shared base class. Configuration
     * from application-specific command line options should be done in
     * configureApp()
     * 
     * @see #configureApp(CommandLine)
     * @param commandLine
     *            the parsed command line object
     */
    @SuppressWarnings({ "DM_EXIT" })
    public void configure(final CommandLine commandLine) {
        // help options
        if (commandLine == null) {
            showHelp();
            System.exit(1);
        }

        if (commandLine.hasOption(ReservedOptions.HELP_SHORT_VALUE)) {
            showHelp();
            System.exit(1);
        }

        // version option
        if (commandLine.hasOption(ReservedOptions.VERSION_SHORT_VALUE)) {
            showVersion();
            System.exit(1);
        }

        // debug option
        if (commandLine.hasOption(ReservedOptions.DEBUG_SHORT_VALUE)) {
            this.debug = true;
        }
    }

    /**
     * Creates an Options object containing common command line
     * arguments/options. Application specific command line options should be
     * specified in addAppOptions().
     * 
     * @return the Options object
     */
    protected Options createOptions() {
        final Options options = new Options();

        // set up standard chill options
        options.addOption(ReservedOptions.getOption(ReservedOptions.HELP_SHORT_VALUE));
        options.addOption(ReservedOptions.getOption(ReservedOptions.VERSION_SHORT_VALUE));
        options.addOption(ReservedOptions.getOption(ReservedOptions.DEBUG_SHORT_VALUE));

        return (options);
    }

    /**
     * Creates an Options object contaning possible command line arguments/options
     * that are specfic to an application.
     * 264
     */
    protected abstract void addAppOptions();

    /**
     * Adds the given command line option to the Options object.
     *
     * @param opt
     *            the Option to add
     */
    protected void addOption(final Option opt) {
        this.options.addOption(opt);
    }

    /**
     * Adds a command line option to an Options object.
     * 
     * @param shortOpt
     *            the short name (letter) for the option
     * @param longOpt
     *            the long name for the option
     * @param argName
     *            the name of the option argument, or null if the option takes no argument
     * @param description
     *            the description of the option
     */
    protected void addOption(final String shortOpt, final String longOpt, final String argName,
                             final String description) {
        this.options.addOption(ReservedOptions.createOption(shortOpt, longOpt, argName, description));
    }
}
