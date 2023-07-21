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
package ammos.datagen.verification.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import ammos.datagen.cmdline.DatagenOptions;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.process.FileLineHandler;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.process.StderrLineHandler;
import jpl.gds.shared.spring.context.SpringContextFactory;

/**
 * This is an abstract application class that can be used to build specific
 * applications that will compare data generator results to AMPCS query results.
 * It takes an input directory, which corresponds to the output directory from a
 * data generator run, and a set of query options to pass to chill_get... as
 * command line options.
 * 
 *
 * MPCS-7750 - 10/23/15. Changed to use new BaseCommandOptions
 *          and new command line option strategy throughout.
 */
public class AbstractVerificationApp extends AbstractCommandLineApp {

    /**
     * Trace logger for this application.
     */
    protected static Tracer log;
    /**
     * SUCCESS exit code.
     */
    protected static final int SUCCESS = 0;
    /**
     * FAILURE exit code.
     */
    protected static final int FAILURE = 1;

    /**
     * Input directory supplied on the command line.
     */
    protected String inputDir;
    /**
     * Query options supplied on the command line.
     */
    protected String queryOptions;
    /**
     * Query output file object.
     */
    protected File outputFile;
    
    protected ApplicationContext appContext;
    
    protected AbstractVerificationApp() {
        appContext = SpringContextFactory.getSpringContext(true);
        log = TraceManager.getDefaultTracer(appContext);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
     */
    @Override
    public DatagenOptions createOptions() {

        final DatagenOptions options = new DatagenOptions(this);
        options.addOption(DatagenOptions.INPUT_DIRECTORY);
        options.addOption(DatagenOptions.CHILL_QUERY_OPTIONS);
        return options;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.cmdline.ICommandLine)
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {

        super.configure(commandLine);

        this.inputDir = DatagenOptions.INPUT_DIRECTORY.parse(commandLine, true);
        this.queryOptions = DatagenOptions.CHILL_QUERY_OPTIONS.parse(
                commandLine, true);
    }

    /**
     * Initializes the application by verifying that AMPCS can be found and
     * creating the temporary query file.
     * 
     * @return true if initialization successful, false if not.
     */
    public boolean init() {

        /*
         * Make sure we can find AMPCS.
         */
        final String chillDir = GdsSystemProperties.getGdsDirectory();
        if (chillDir == null) {
            log.error("Cannot locate AMPCS installation");
            return false;
        }

        /*
         * Make sure we can create the temporary query output file.
         */
        try {
            this.outputFile = File.createTempFile("adg_", "_query.csv");
        } catch (final IOException e1) {
            e1.printStackTrace();
            log.error("Could not create query output file");
            return false;
        }

        return true;
    }

    /**
     * Executes chill_get utility and writes results to a temporary file.
     * 
     * @param commandScript
     *            the full path to the query utility to run
     * @param extraOptions
     *            additional query options to add beyond those the user supplied
     *            on the command line
     * 
     * @return true if the query was successful, false if not
     */
    protected boolean runChillQuery(final String commandScript,
            final String extraOptions) {

        /*
         * Construct the command line for chill_get..., adding the required
         * query options from the datagen configuration, and the user's query
         * options from the command line.
         */
        final String commandLine = commandScript + " " + this.queryOptions
                + " " + extraOptions;

        try {

            /*
             * The chill_get_evrs process launcher is configure to send stdout
             * to our temporary query file and stderr back through this process
             * to the console.
             */
            final ProcessLauncher launcher = new ProcessLauncher();
            final FileLineHandler output = new FileLineHandler(this.outputFile);
            launcher.setOutputHandler(output);
            launcher.setErrorHandler(new StderrLineHandler());

            /*
             * Here we go... launch the query.
             */
            log.info("Running " + commandLine);
            final boolean ok = launcher.launch(commandLine);
            if (!ok) {
                log.info("Failed to launch " + commandLine + "...");
                return false;
            } else {
                /*
                 * Wait for the process to complete.
                 */
                try {
                    launcher.join();
                } catch (final ExcessiveInterruptException e) {
                    e.printStackTrace();
                    return false;
                }
                /*
                 * Grab exits status and close off the output file.
                 */
                final int exitStatus = launcher.getExitValue();
                output.shutdown();

                /*
                 * Notify user whether it succeeded or failed.
                 */
                if (exitStatus == 0) {
                    log.info("chill_get run succeeded");
                    log.info("Query output saved to "
                            + this.outputFile.getPath());
                } else {
                    log.error("chill_get run failed with status " + exitStatus);
                    return false;
                }
            }

        } catch (final IOException e) {
            e.printStackTrace();
            TraceManager.getDefaultTracer().error(

                    "I/O Error executing chill_get utility");
            return false;
        }

        /*
         * If we reached this point, all was well with the query run.
         */
        return true;
    }

    /**
     * Compares query output to the data generator truth file.
     * 
     * @param truthFile
     *            the path to the truth file
     * @param prefix
     *            the truth line prefix on records to compare, up through the
     *            separator colon; lines in the truth file without this prefix
     *            will be ignore
     * 
     * @return true if comparison succeeded, false if there was an error or
     *         differences were found.
     */
    protected boolean compareTruthToQueryOutput(final String truthFile,
            final String prefix) {

        /*
         * Assume everything matches.
         */
        boolean success = true;

        try {
            /*
             * Open both truth and query files. Use a buffered reader to get one
             * line at a time.
             */
            final BufferedReader truthReader = new BufferedReader(
                    new FileReader(truthFile));
            final BufferedReader queryReader = new BufferedReader(
                    new FileReader(this.outputFile));

            /*
             * Read an initial line from each file and start line number
             * counters for both.
             */
            String truthLine = truthReader.readLine();
            String queryLine = queryReader.readLine();
            int truthLineNum = 1;
            int queryLineNum = 2;

            /*
             * Find the first line in the truth file with the prefix we are
             * looking for.
             */
            while (truthLine != null && !truthLine.startsWith(prefix)) {
                truthLineNum++;
                truthLine = truthReader.readLine();
            }
            /*
             * Now start comparing lines between truth and query. Stop when we
             * run out of data in one file or the other.
             */
            while (truthLine != null && queryLine != null) {
                /*
                 * Wack off the prefix from the truth and compare. If comparison
                 * fails, report line number in both truth and query files and
                 * set failure flag.
                 */
                truthLine = truthLine.substring(prefix.length() + 1);
                if (!truthLine.equals(queryLine)) {
                    log.error("Difference found at truth line " + truthLineNum
                            + ", query line " + queryLineNum);
                    log.error("Expected: " + truthLine);
                    log.error("Found: " + queryLine);
                    success = false;
                }
                /*
                 * Move the truth file line forward to the next line that
                 * matches the prefix.
                 */
                while (truthLine != null && !truthLine.startsWith(prefix)) {
                    truthLineNum++;
                    truthLine = truthReader.readLine();
                }
                /*
                 * Move the query file line forward one line.
                 */
                queryLine = queryReader.readLine();
                queryLineNum++;
            }

            /*
             * When we get here, we ran out of data in one file or the other. If
             * we did not run out of both, we have a mismatch.
             */
            if (truthLine == null && queryLine != null) {
                log.error("There are more records in the query file than in the truth file");
                success = false;
            } else if (queryLine == null && truthLine != null) {
                log.error("There are more records in the truth file than in the query file");
                success = false;
            }
            /*
             * Done. Close both files.
             */
            truthReader.close();
            queryReader.close();
        } catch (final FileNotFoundException e) {
            log.error("Truth file " + truthFile + " was not found");
            success = false;
        } catch (final IOException e) {
            e.printStackTrace();
            log.error("I/O error reading either truth or query file");
            success = false;
        }

        /*
         * Log success. Failures will already be logged.
         */
        if (success) {
            log.info("Output from query matches the truth file " + truthFile);
        }
        return success;
    }
}
