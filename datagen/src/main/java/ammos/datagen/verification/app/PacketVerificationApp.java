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

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;

import ammos.datagen.config.DatagenProperties;
import ammos.datagen.generators.util.FileSuffixes;
import jpl.gds.shared.annotation.CoverageIgnore;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;

/**
 * This is an application that will compare a data generator truth file to the
 * output of the AMPCS chill_get_packets query utility. It takes an input
 * directory, which corresponds to the output directory from a data generator
 * run, and a set of query options to pass to chill_get_packets, as command line
 * options.
 * 
 *
 * MPCS-7750 - 10/23/15. Changed to use new BaseCommandOptions
 *          and new command line option strategy throughout.
 * 
 */
public class PacketVerificationApp extends AbstractVerificationApp {

    private String getPacketsPath;
    private String packetTruthFile;
    private DatagenProperties datagenProps ;

    /**
     * Initializes the application by verifying that the needed AMPCS
     * applications can be found, locating the truth file for comparison, and
     * creating the temporary query file.
     * 
     * @return true if initialization successful, false if not.
     */
    @Override
    public boolean init() {

        if (!super.init()) {
            return false;
        }
        datagenProps = new DatagenProperties();
        /*
         * Make sure we can find AMPCS chill_get_packets utility.
         */
        this.getPacketsPath = GdsSystemProperties.getGdsDirectory()
                + File.separator
                + datagenProps.getPacketQueryScript();
        if (!new File(this.getPacketsPath).exists()) {
            log.error("Cannot locate chill_get_packets in "
                    + this.getPacketsPath);
            return false;
        }

        /*
         * The truth file should be in the input directory supplied on the
         * command line. We have to find it because we don't know which
         * generator created it, so we don't know the exact name.
         */
        final File[] files = new File(this.inputDir)
        .listFiles(new TruthFileFilter());
        if (files.length == 0) {
            log.error("Cannot locate a truth file in " + this.inputDir);
            return false;
        }
        if (files.length != 1) {
            log.error("Found more than one truth file in " + this.inputDir);
            return false;
        }
        this.packetTruthFile = files[0].getAbsolutePath();

        return true;
    }

    /**
     * Executes the main logic for this application: Runs chill_get_packets and
     * verifies results against then data generator truth file.
     * 
     * @return application exit code: SUCCESS or FAILURE
     */
    public int run() {

        /*
         * Run the chill_get_packets query and save results to temp file.
         */
        if (!runChillQuery()) {
            return FAILURE;
        }
        if (!comparePacketTruthToQueryOutput()) {
            return FAILURE;
        }

        return SUCCESS;
    }

    /**
     * Executes chill_get_packets using the datagen verification template for
     * output and writes results to a temporary file.
     * 
     * @return true if the query was successful, false if not
     */
    private boolean runChillQuery() {

        return super.runChillQuery(this.getPacketsPath, datagenProps.getPacketQueryOptions());
    }

    /**
     * Compares packet query output to the datagen truth file.
     * 
     * @return true if the comparison succeeded, false if there was an error or
     *         any differences were found.
     */
    private boolean comparePacketTruthToQueryOutput() {

        return compareTruthToQueryOutput(this.packetTruthFile, "Packet:");
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.println("Usage: " + ApplicationConfiguration.getApplicationName()
                + " --inputDir <directory> --queryOptions <chill options>");
        pw.println("                   ");

        final OptionSet options = createOptions().getOptions();

        options.printOptions(pw);
        pw.println("This is a test verification application that will compare the");
        pw.println("truth file generated by a data generator with the AMPCS packet");
        pw.println("query output for an AMPCS session.\n");

        pw.flush();
    }

    /**
     * File filter class for truth files.
     * 
     *
     */
    private static class TruthFileFilter implements FilenameFilter {

        /**
         * {@inheritDoc}
         * 
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        @Override
        public boolean accept(final File arg0, final String arg1) {

            return arg1.matches(".*" + FileSuffixes.TRUTH.getSuffix());
        }
    }

    /**
     * Main application entry point.
     * 
     * @param args
     *            Command line arguments from the user
     */
    @CoverageIgnore
    public static void main(final String[] args) {

        final PacketVerificationApp app = new PacketVerificationApp();

        // Parse the command line arguments
        try {
            final ICommandLine commandLine = app.createOptions()
                    .parseCommandLine(args, true);
            app.configure(commandLine);
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(e.getMessage());
            System.exit(1);
        }

        // Initialize the application
        final boolean ok = app.init();
        if (!ok) {
            System.exit(1);
        }
        try {
            final int code = app.run();
            System.exit(code);
        } catch (final Exception e) {
            // something totally unexpected happened
            e.printStackTrace();
            System.exit(1);
        }
    }
}
