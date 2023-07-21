package jpl.gds.tcapp.app;

import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.cltu.ICltuFactory;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tcapp.app.reverse.cltu.ICltuWriter;
import org.apache.commons.cli.ParseException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * This application will attempt to extract binary (PDU) data from an
 * ITcTransferFrame within a CLTU.
 * The extracted PDU can be written to an output directory.
 * The extracted PDU can be compared with a PDU file.
 * 
 * Initial version - CLTU -> PDU Reversal
 *          The CLTU file input is expected to have 1 CLTU, 1 TC frame, and 1 PDU.
 * 
 * 3/15/18 - Added improvements for parsing multiple cltu's and frames.
 *          Comparison pdu option is now a directory
 *
 * 6/3/19  - Added handling for multiple PDUs in one CLTU.
 * 
 *
 */
public class CltuToPduApp extends AbstractPduReverseApp {
    /** Long opt for the CLTU we will try and reverse */
    public static final String       LONG_REVERSE_CLTUS_FILE_OPT = "reverseCltus";

    private final FileOption reverseCltuFileOpt                      = new FileOption(null, LONG_REVERSE_CLTUS_FILE_OPT,
                                                                                      "inputFile",
                                                                                      "CLTU file containing PDU(s) to extract",
                                                                                      true, true);

    private ICltuWriter cltuPduWriter;

    /**
     * Constructor for chill_cltu_to_pdu
     */
    public CltuToPduApp() {
        super();
    }

    /**
     * "Reverses" a file containing one or more CLTU(s).
     * Attempts to extract byte[] pdu data from TC frames found in the cltu(s).
     * The extracted frames and pdu's will be written to the console
     * The extracted pdu's are compared with expected pdu files in a directory supplied via command-line
     * The extracted pdu's can be written to an output directory supplied via command-line
     *
     * @return true if successful, false otherwise
     */
    public ReversalErrorCode cltuToPduReversal() throws Exception {
        cltuPduWriter = appContext.getBean(ICltuWriter.class);
        ICltuFactory cltuFactory = appContext.getBean(ICltuFactory.class);

        // MCSECLIV-438 2/8/21 Added quiet option for output suppression
        cltuPduWriter.setSuppressOutput(quiet);

        boolean didCtsCreateFrames = false;
        List<ICltu> cltuList = new ArrayList<>();
        try {
            cltuList = cltuFactory.parseCltusFromBytes(inputData);
            log.trace("Created ", cltuList.size(), " CLTU(s) from bytes=", inputData.length);
        } catch (final CltuEndecException e) {
            log.error("Error processing CLTU(s) from ", reverseFileName, ":", ExceptionTools.getMessage(e), " ... Shutting down. ");
            return ReversalErrorCode.FAILURE;
        }
        if (cltuList.isEmpty()) {
            log.error("No CLTU(s) processed from ", reverseFileName);
            return ReversalErrorCode.FAILURE;
        }

        if(!cltuList.get(0).getFrames().isEmpty()) {
            didCtsCreateFrames = true;
        }

        final List<byte[]> allExtractedPdus = cltuPduWriter.doReverseCltus(cltuList);
        writeExtractedPdusToOutputDir(allExtractedPdus);

        // Success cases
        // 1. no comparison option was provided
        // 2. comparison option was provided and comparision was successful

        if (pduDirectory == null ||
                (!comparisonPdus.isEmpty() && comparePdus(comparisonPdus.values(), allExtractedPdus))) { // if comparison fails, an exception gets thrown)
            if(didCtsCreateFrames) {
                // if comparison was successful (or not necessary) *AND* CTS successfully parsed the CLTUs into frames
                return ReversalErrorCode.SUCCESS;
            } else {
                return ReversalErrorCode.PARTIAL_SUCCESS;
            }
        }

        // if we ended up here, something went wrong
        return ReversalErrorCode.FAILURE;
    }

    /**
     * Clean up open resources
     */
    @Override
    public void exitCleanly() {
        cltuPduWriter.cleanUp();
        super.exitCleanly();
    }

    /**
     * Application specific configuration. Specifically, the CLTU file to reverse and the optional output directory
     * for extracted PDUs.
     *
     * @param commandLine values entered by the user on the command line
     * @throws ParseException
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        // File option validates input files
        reverseFileName = reverseCltuFileOpt.parse(commandLine);
        if (reverseFileName != null && !Files.exists(Paths.get(reverseFileName))) {
            throw new ParseException("Invalid cltu file " + reverseFileName + " specified by " + LONG_REVERSE_CLTUS_FILE_OPT);
        }

    }

    /**
     * Help message
     */
    @Override
    public void showHelp() {
        if (helpDisplayed.get()) {
            return;
        }
        showHelp("Usage: " + ApplicationConfiguration.getApplicationName() + " [options]\n"
                         + "This application will attempt to extract binary (PDU) data from an ITcTransferFrame within a CLTU. \n"
                         + "The extracted PDUs can be written to a specified directory if outputDir option is provided.\n"
                         + "The extracted PDUs can be compared to other PDUs in a directory if specified.\n"
                         + "Note: this tool will only run on a Linux operating system.");
    }

    /**
     * Creating the command line options for the app
     *
     * @return object containing basic command options, pduDirectory (for comparison),
     * file to reverse, and output directory
     */
    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }
        options = super.createOptions();

        options.addOption(reverseCltuFileOpt);

        return options;
    }

    /**
     * Main
     *
     * @param args
     *            Command-line arguments
     */
    public static void main(final String[] args) {

        final CltuToPduApp app = new CltuToPduApp();
        final Tracer log = TraceManager.getDefaultTracer();

        try {
            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true, true, true);
            app.configure(commandLine);
            app.readFileInputs();
        }
        catch (final ParseException e) {
            log.error(ExceptionTools.getMessage(e));
        }

        ReversalErrorCode exitCode = ReversalErrorCode.FAILURE;

        try {
            exitCode = app.cltuToPduReversal();
            log.info(exitCode);
        } catch (Exception ex) {
            log.warn(exitCode, " - " + ex.getMessage());
        }

        System.exit(ReversalErrorCode.getValue(exitCode));
    }
}
