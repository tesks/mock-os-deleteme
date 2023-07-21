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
package jpl.gds.shared.cli.app;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.annotation.CoverageIgnore;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.BeanUtil;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.QuitSignalHandler;
import jpl.gds.shared.sys.Shutdown;
import jpl.gds.shared.template.ApplicationTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;

/**
 * This is a top-level abstract class to be extended by all command line
 * applications. It provides basic methods related to command line parsing. It
 * handles some common things like the standardized help and version options.
 * Note that this class depends upon the BaseCommandOptions,
 * ApplicationConfiguration, and Release classes.
 * 
 *
 * @see ApplicationConfiguration
 * @see ReleaseProperties
 * @see BaseCommandOptions
 *
 */
public class AbstractCommandLineApp implements ICommandLineApp {
    /** This application's exit code */
    protected int               errorCode;

    /** Shutdown hook for SIGTERM signals */
    protected QuitSignalHandler sigterm;
    
    /** Shared command options object */
    protected BaseCommandOptions options;
    
    protected final AtomicBoolean  optionsCreated      = new AtomicBoolean(false);

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     */
    protected AbstractCommandLineApp() {
        this(true);
    }

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     * 
     * @param addHook
     *            whether or not to add the shutdown hook
     */
    protected AbstractCommandLineApp(final boolean addHook) {
        if (addHook) {
            sigterm = new QuitSignalHandler(this);
            Runtime.getRuntime().addShutdownHook(new Thread(sigterm, Shutdown.THREAD_NAME));
        }
    }


    @Override
    public void exitCleanly() {
        TraceManager.getDefaultTracer().debug(this.getClass().getName(),
                " shutting down using default QuitSignalHandler");
    }

    /**
     * Displays the application help text to the console. This default
     * implementation displays the application name and dumps the command line
     * options.
     * 
     * @see jpl.gds.shared.cli.app.ICommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        showHelp("Usage: " + ApplicationConfiguration.getApplicationName() + " [options]");
    }

    /**
     * Displays the application help text to the console. This default
     * implementation displays the application name and dumps the command line
     * options.
     * 
     * @param preAmble
     *            the first line of the help message, used for command line example
     * 
     * @see jpl.gds.shared.cli.app.ICommandLineApp#showHelp()
     */
    protected void showHelp(final String preAmble) {
        createOptions().getOptions();

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println(preAmble);
        pw.println("                   ");

        options.getOptions().printOptions(pw);
        
        printTemplateStylesAndDirectories(pw);

        pw.flush();
    }

    /**
     * Displays the application version to standard output. This default
     * implementation of this method uses the ApplicationConfiguration object to
     * get the application name, and the Release object to get the version
     * number.
     * 
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.ICommandLineApp#showVersion()
     * @see ApplicationConfiguration
     * @see ReleaseProperties
     */
    @Override
    public void showVersion() {
        if (versionDisplayed.getAndSet(true)) {
            return;
        }

        final String appName = ApplicationConfiguration.getApplicationName();
        System.out.println(ReleaseProperties.getProductLine() + " " + appName + " "
                + ReleaseProperties.getVersion());
    }

    /**
     * Creates command line options enclosed in a BaseCommandOptions object. This default
     * implementation adds HELP and VERSION options and defines the BaseCommandOptions
     * to support aliasing.
     * 
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.app.ICommandLineApp#createOptions()
     */
    @Override
    public BaseCommandOptions createOptions() {
        
        if (optionsCreated.get()) {
            return options;
        }
        
        return createOptions(new BaseCommandOptions(this, true));
    }
    

    /**
     * Creates command line options using the supplied command options
     * object. This allows subclasses to pass in a context-aware options
     * object.
     * 
     * @param opts BaseCommandOptions to use
     * 
     * @return BaseCommandOptions
     * 
     */
    protected BaseCommandOptions createOptions(final BaseCommandOptions opts) {
        
        if (optionsCreated.getAndSet(true)) {
            return options;
        }
        
        options = opts;
        
        options.addHelpOption();
        options.addVersionOption();
        return options;
    }

    /**
     * Configures the application from the parsed command line object by
     * extracting command line values and setting member variables and
     * configuration in the application object. This default implementation of
     * the configure method looks for help and version options, and responds to
     * them if and exits the VM with ICommandLineOption.USER_HELP_REQUEST if
     * found.
     * 
     * @param commandLine
     *            ICommandLine parsed command line
     * @throws ParseException
     *             if there is an error configuring the application
     * 
     * 
     * @see jpl.gds.shared.cli.app.ICommandLineApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
    @CoverageIgnore
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {

        final BaseCommandOptions optionsParser = createOptions();

        optionsParser.getHelpOption().parseWithExit(commandLine, false, true);
        optionsParser.getVersionOption().parseWithExit(commandLine, false, true);
        setCommandLine(commandLine);
    }
    
    /**
     * Starting with AMPCS R3, there is now an application-based templating
     * feature available. Available templates should be output on "help".
     * 
     * @param pw
     *            PrintWriter to output available templates to
     */
    protected void printTemplateStylesAndDirectories(final PrintWriter pw) {
        ApplicationTemplateManager tm = null;
        String[] types = null;
        
        try {
            try {
                tm = MissionConfiguredTemplateManagerFactory.getNewApplicationTemplateManager(this,
                                                                                              BeanUtil.getBean(SseContextFlag.class));
            }
            catch (final Exception e) {
                tm = MissionConfiguredTemplateManagerFactory.getNewApplicationTemplateManager(this);
            }
            types = tm.getTypeNames();

        } catch (final TemplateException e) {
            // assume that this application doesn't not support templating
            return;
        }
        
        for (int i = 0; i < types.length; i++) {
            String[] styles = null;
            
            try {
                styles = tm.getStyleNames(types[i]);
            } catch (final TemplateException e) {
                continue;
            }
            
            pw.print("\nAvailable formatting styles for " + types[i] + " type are:");
            
            for (int j = 0; j < styles.length; j++) {
                
                if (j % 4 == 0) {
                    pw.println();
                    pw.print("   ");
                }
                
                pw.print(styles[j] + " ");
            }
                
        }
        pw.println();

        final List<String> directories = tm.getTemplateDirectories();
        
        pw.println("\nTemplate directories searched are:");
        
        for (final String d : directories) {
            pw.println("   " + d);
        }
        
        pw.flush();
        
    }


    @Override
    public void setErrorCode(final int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public void setCommandLine(final ICommandLine cmdline) {
        if (sigterm != null && cmdline != null) {
            sigterm.setCommandLine(cmdline);
        }
    }
}
