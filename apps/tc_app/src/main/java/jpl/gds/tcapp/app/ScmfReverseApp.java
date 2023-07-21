/*
 * Copyright 2006-2019. California Institute of Technology.
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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.cli.legacy.app.AbstractCommandLineApp;
import jpl.gds.cli.legacy.options.ReservedOptions;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.tc.api.ICommandWriteUtility;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.ITewUtility;
import jpl.gds.tc.api.TcApiBeans;
import jpl.gds.tc.api.exception.ScmfParseException;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tcapp.app.reverse.scmf.ReverseScmfWriter;
import org.apache.commons.cli.*;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.util.Arrays;
import java.util.TimeZone;

/**
 * The command line application chill_scmf_reverse.
 * <p>
 * This application is used parse an SCMF file's binary contents and generate an ASCII text report of what the SCMF
 * contained.  This application will reverse all of the translations in the various levels of the uplink protocol
 * stack.
 * <p>
 * An SCMF (a SpaceCraft Message File) is an Deep Space Network (DSN) interface that contains pre-translated uplink
 * ready for transmission to a flight system.
 *
 * 2019-08-08 - SCMF parsing is now done through the TEW utility, moved SCMF writing logic to ReverseScmfWriter, misc
 * refactoring
 */
public class ScmfReverseApp extends AbstractCommandLineApp {
    static {
        // Originally done in the constructor, but Tracer was being
        // instantiated before the constructor set the time zone.
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Logger instance
     */
    protected static Tracer trace;

    /**
     * The name of this application
     */
    private static final String APP_NAME = ApplicationConfiguration.getApplicationName("chill_scmf_reverse");
    /**
     * Short command option for the view dictionary release option
     **/
    public static final String RELEASE_PARAM_SHORT = "r";
    /**
     * Long command option for the view dictionary release option
     **/
    public static final String RELEASE_PARAM_LONG = "release";
    /**
     * Short command option for the output file
     **/
    public static final String OUTPUT_FILE_SHORT = "o";
    /**
     * Long command option for the output file
     **/
    public static final String OUTPUT_FILE_LONG = "outputFile";


    /**
     * The user-supplied input SCMF file
     */
    protected File inFile;
    /**
     * The corresponding translated output file (if it exists)
     */
    protected File outFile;
    /**
     * The actual SCMF file object from the input file
     */
    protected IScmf scmf;

    private final DictionaryProperties dictProps;
    private final ICommandWriteUtility cmdWriteUtil;
    private final FlightDictionaryLoadingStrategy strategy;
    private final ApplicationContext appContext;

    /**
     * Create an instance of the SCMF reverse application.
     */
    public ScmfReverseApp(final ApplicationContext appContext) {
        super();
        this.appContext = appContext;
        ReservedOptions.setApplicationContext(appContext);
        inFile = null;
        outFile = null;
        scmf = null;

        dictProps = appContext.getBean(DictionaryProperties.class);
        cmdWriteUtil = appContext.getBean(ICommandWriteUtility.class);
        strategy = appContext.getBean(FlightDictionaryLoadingStrategy.class).enableCommand();
    }

    /**
     * Get application context
     *
     * @return Application context
     */
    public ApplicationContext getApplicationContext() {
        return this.appContext;
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#configure(org.apache.commons.cli.CommandLine)
     */
    @SuppressWarnings("DM_EXIT")
    @Override
    public void configure(final CommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        // reorganize calls; parseReleaseOption(...) needs this
        // 1) FSW version needs to be parsed from CLI, but dict should only load if necessary.
        // 2) parseReleaseOption needs the command dict to be loaded, then exits.
        // 3) if all other checks clear, load command dict

        //parse the FSW dictionary information
        ReservedOptions.parseFswDictionaryDir(commandLine, false, true);
        ReservedOptions.parseFswVersion(commandLine, false, true);

        // exit if release option is present
        this.parseReleaseOption(commandLine);

        // pull the input filename off the command line, exits if not present or correct
        this.parseFilenameArgument(commandLine);

        // see if the user supplied an output file
        this.parseOutputFilenameOption(commandLine);

        // load command dict to continue
        this.loadCommandDictionary();
    }

    /**
     * Parse the input filename argument from the command line
     *
     * @param commandLine
     * @throws ParseException
     */
    private void parseFilenameArgument(CommandLine commandLine) throws ParseException {
        final String[] leftoverArgs = commandLine.getArgs();
        if (leftoverArgs.length == 0) {
            throw new MissingArgumentException("There was no filename on the command line.");
        } else if (leftoverArgs.length > 1) {
            throw new ParseException("Too many leftover command line values: " + Arrays
                    .toString(leftoverArgs) + ".  This application only" +
                    " accepts one command line argument (the name of the input SCMF file)");
        }

        inFile = new File(leftoverArgs[0]);
        if (!inFile.exists()) {
            throw new ParseException("The specified input SCMF file " + leftoverArgs[0] + " does not exist.");
        }
    }

    private void loadCommandDictionary() {
        // Trigger parsing of command dictionary
        try {
            strategy.loadAllEnabled(appContext, false, false);
        } catch (final Exception e) {
            trace.warn("Could not parse the command dictionary: " + e.getMessage() + ". Attempting to continue...");
        }
    }

    /**
     * Parse the dictionary release command line option
     *
     * @param commandLine The Apache CLI command line object
     */
    @SuppressWarnings({"DM_EXIT"})
    private void parseReleaseOption(final CommandLine commandLine) {
        if (commandLine.hasOption(RELEASE_PARAM_SHORT)) {
            // load command dictionary before fetch
            loadCommandDictionary();
            ICommandDefinitionProvider dictionary = appContext.getBean(ICommandDefinitionProvider.class);

            /*
             * Technically only MSL-formatted
             * dictionaries are required to have a build or release version ID.
             * I have modified the CommandDefinitionTable to return the GDS
             * version ID for build and release IDs if they are null. This
             * used to be the other way around, basically.
             */
            System.out.println("Command dictionary FSW version ID = " + dictionary.getBuildVersionId() +
                    ", dictionary = " + dictionary.getGdsVersionId() +
                    ", release = " + dictionary.getReleaseVersionId());
            System.exit(0);
        }
    }

    /**
     * Parse the user-supplied output file name
     *
     * @param commandLine The Apache CLI command line object
     * @throws ParseException If the filename supplied is not a valid output file
     */
    private void parseOutputFilenameOption(final CommandLine commandLine) throws ParseException {
        if (commandLine.hasOption(OUTPUT_FILE_SHORT)) {
            final String outFilename = commandLine.getOptionValue(OUTPUT_FILE_SHORT, null);
            if (outFilename == null) {
                throw new ParseException("Output filename option requires a value.");
            }

            outFile = new File(outFilename.trim());
        }
    }

    /**
     * Reverse the SCMF into ASCII text.
     *
     * @throws IOException             For all filesystem-related errors
     * @throws DictionaryException     if there is an error locating or parsing the dictionaries
     * @throws ScmfWrapUnwrapException if there is an error parsing the SCMF
     */
    private void doReverse() throws
            IOException,
            DictionaryException,
            ScmfWrapUnwrapException,
            ScmfParseException {

        // moved a bean lookup back. it's a sad thing, but it's also the path of least resistance.
        // this should be provided via a factory in future.
        ITewUtility tewUtility = appContext.getBean(TcApiBeans.MPS_TEW_UTILITY, ITewUtility.class);
        scmf = tewUtility.reverseScmf(inFile.getAbsolutePath());
        final ReverseScmfWriter reverseScmf = new ReverseScmfWriter(dictProps, cmdWriteUtil);
        reverseScmf.setTracer(trace);

        final PrintWriter writer = createPrintWriter(outFile);

        reverseScmf.doReverse(scmf, writer);
    }

    private PrintWriter createPrintWriter(final File outFile) {
        //write to the output file if given, otherwise write to the console
        if (outFile != null) {
            try {
                return new PrintWriter(new FileOutputStream(outFile));
            } catch (final FileNotFoundException e) {
                trace.error("Exception encountered while opening output file " + outFile.getAbsolutePath() + ": " + e
                        .getMessage() + ".  Output will be redirected to the console.");
                return new PrintWriter(System.out);
            }
        } else {
            return new PrintWriter(System.out);
        }
    }

    /**
     * Gets the file object for the input SCMF file.
     *
     * @return the user-supplied input file.
     */
    public File getInFile() {
        return inFile;
    }

    /**
     * Gets the file object for the output file.
     *
     * @return the user-supplied output file or null if there isn't one.
     */
    public File getOutFile() {
        return outFile;
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#createOptions()
     */
    @Override
    public Options createOptions() {
        final Options options = super.createOptions();

        options.addOption(ReservedOptions.FSW_VERSION);
        options.addOption(ReservedOptions.FSW_DICTIONARY_DIRECTORY);
        options.addOption(ReservedOptions.createOption(OUTPUT_FILE_SHORT, OUTPUT_FILE_LONG, "filename",
                "Redirect SCMF reverse output to the specified file."));
        options.addOption(ReservedOptions.createOption(RELEASE_PARAM_SHORT, RELEASE_PARAM_LONG, null,
                "View the release version of the dictionary being used."));

        return (options);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.cli.legacy.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        System.out.println("\n" + APP_NAME + " <options> scmf_file_name\n\n" +
                "\tscmf_file_name - The SCMF file whose contents will be reserved.\n\n" +
                "Output will be sent to standard output by default.\n\n");
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(80, "\n", "\n", createOptions(), "\n");
    }

    /**
     * The command line interface.
     *
     * @param args The command line arguments
     */
    public static void main(final String[] args) {
        try {
            final ApplicationContext appContext = SpringContextFactory.getSpringContext(true);
            ScmfReverseApp.trace = TraceManager.getDefaultTracer(appContext);
            final ScmfReverseApp reverse = new ScmfReverseApp(appContext);
            final CommandLine commandLine = ReservedOptions.parseCommandLine(args, reverse);
            reverse.configure(commandLine);
            reverse.doReverse();
        } catch (final ParseException e) {
            trace.error("Exception was encountered while running SCMF reverse: " + e
                    .getMessage() + " (Use the -h option for usage instructions).");
            System.exit(1);
        } catch (final ScmfWrapUnwrapException e) {
            trace.error("SCMF Format Exception was encountered while running SCMF reverse: " + e.getMessage());
            System.exit(1);
        } catch (final Exception npe) {
            trace.error("Exception was encountered while running SCMF reverse: " + npe.getMessage(), npe);
            System.exit(1);
        }
    }
}
