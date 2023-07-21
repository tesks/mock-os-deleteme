package jpl.gds.app.tools.spring;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.AnnotatedBeanLocator;
import jpl.gds.shared.spring.context.SpringContextFactory;

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

/**
 * An application that dumps spring bran definition and bootstrap locations.
 */
public class SpringBeanDumperApp extends AbstractCommandLineApp {
    /** outputFile option */
    public static final String OUTPUT_FILE_OPTION = "outputFile";
    
    private final FileOption outputFileOption = new FileOption(null, OUTPUT_FILE_OPTION, "file", "Output file; Defaults to stdout.", false, false);
    private String outfile;
    
    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions();
        options.addOption(outputFileOption);
        return options;
    }
    
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName() + "[--outputFile <file>]");
        createOptions().getOptions().printOptions(pw);
        
        pw.println("\nDumps the spring bean configuration for the current mission to a file or console.");
        
        pw.flush();
    }
    
    @Override
    public void configure(final ICommandLine cmdLine) throws ParseException {
        super.configure(cmdLine);
        outfile = outputFileOption.parse(cmdLine);
    }
    
    /**
     * Executes the bean dump to file or console.
     */
    public void execute() {
        final ApplicationContext context = SpringContextFactory.getSpringContext(true);
        try {
            if (outfile != null) {
                new AnnotatedBeanLocator(context, false).generateReport(outfile);
            } else {
                System.out.println(new AnnotatedBeanLocator(context, false).buildReport());
            }
        } catch (ClassNotFoundException | IOException e) {
            TraceManager.getDefaultTracer().error("Error running bean locator: " + e.toString(), e);
        }
    }
    
    /**
     * Main application entry point.
     * 
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        
        try {
            final SpringBeanDumperApp app = new SpringBeanDumperApp();
            final ICommandLine cmdline = app.createOptions().parseCommandLine(args, true);
            app.configure(cmdline);
            app.execute();
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(e.getMessage() != null ? e.getMessage() : e.toString());
            System.exit(1);
        } catch (final Exception e) {
            TraceManager.getDefaultTracer().error("Unexpected processing error: " + e.toString(), e);
            System.exit(1);
        }
    }

    // package private getters for tests

    String getOutfile() {
        return outfile;
    }

}
