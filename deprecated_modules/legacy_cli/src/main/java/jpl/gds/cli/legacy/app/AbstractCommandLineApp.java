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
package jpl.gds.cli.legacy.app;

import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.BeanUtil;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.sys.QuitSignalHandler;
import jpl.gds.shared.sys.Shutdown;
import jpl.gds.shared.template.ApplicationTemplateManager;
import jpl.gds.shared.template.MissionConfiguredTemplateManagerFactory;
import jpl.gds.shared.template.TemplateException;

/**
 * This is a basic attempt to create a top-level abstract class to be extended
 * by all command line applications. It handles some common things like the
 * standardized help and version options.
 * 
 * 8/17/17. Added default shutdown hook handler
 */
public class AbstractCommandLineApp implements CommandLineApp, IQuitSignalHandler {
    protected QuitSignalHandler sigterm;

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     */
    protected AbstractCommandLineApp() {
        sigterm = new QuitSignalHandler(this);
        Runtime.getRuntime().addShutdownHook(new Thread(sigterm, Shutdown.THREAD_NAME));
    }

    @Override
    public void exitCleanly() {
        TraceManager.getDefaultTracer().debug(this.getClass().getName(),
                " shutting down using default QuitSignalHandler");
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.cli.legacy.app.CommandLineApp#showHelp()
     */ 
    @Override
	public void showHelp() {
        final Options options = createOptions();

        final HelpFormatter formatter = new HelpFormatter();
        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName()
                + " [options]");
        pw.println("                   ");

        final int width = 80;
        final int leftPad = 7;
        final int descPad = 10;

        formatter.printOptions(pw, width, options, leftPad, descPad);

        printTemplateStylesAndDirectories(pw);
        
        pw.close();
    }

	/**
	 * Starting with AMPCS R3, there is now an application-based templating
	 * feature available. Available templates should be output on "help".
	 * 
	 * @param pw
	 *            PrintWriter to output available templates to
	 */
	private void printTemplateStylesAndDirectories(final PrintWriter pw) {
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
		
    }
  
    /**
     * {@inheritDoc}
     * @see jpl.gds.cli.legacy.app.CommandLineApp#createOptions()
     */
    @Override
	public Options createOptions() {

        final Options options = new Options();

        options.addOption(ReservedOptions.HELP);
        options.addOption(ReservedOptions.VERSION);

        return options;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.cli.legacy.app.CommandLineApp#configure(
     *      org.apache.commons.cli.CommandLine)
     */
    @Override
	public void configure(final CommandLine commandLine) throws ParseException {
        ReservedOptions.parseHelp(commandLine, this);
        ReservedOptions.parseVersion(commandLine);
    }

    @Override
    public void setCommandLine(final ICommandLine cmdline) {
        // NO Support for legacy cmdline apps
    }
}
