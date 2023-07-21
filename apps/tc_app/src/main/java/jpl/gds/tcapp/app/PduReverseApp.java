package jpl.gds.tcapp.app;

import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tcapp.app.reverse.pdu.IPduParser;
import jpl.gds.tcapp.app.reverse.pdu.IPduParserResult;
import jpl.gds.tcapp.app.reverse.pdu.IPduWriter;
import org.apache.commons.cli.ParseException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * New application that writes the contents of a PDU to the console, and
 * allows for comparison of one PDU to another.
 *
 */
public class PduReverseApp extends AbstractPduReverseApp {

    /** Long opt for the PDU we will try to reverse */
    public static final String       LONG_REVERSE_PDU_FILE_OPT = "reversePdus";
    /** Long opt for pdu file directory */
    public static final String       LONG_PDU_DIR_OPT            = "pduDir";

    private final FileOption reversePduFileOpt = new FileOption(null, LONG_REVERSE_PDU_FILE_OPT,
                                                                "inputFile",
                                                                "PDU file containing PDU(s) to extract",
                                                                true, true);

    private IPduWriter               pduWriter;
    private IPduParser               pduParser;

    /**
     * Constructor for the application
     */
    public PduReverseApp() {
        super();
        pduWriter = appContext.getBean(IPduWriter.class);
        pduParser = appContext.getBean(IPduParser.class);
    }

    /**
     * "Reverses" a file containing a PDU.
     * The extracted PDUs will be written to the console
     * The extracted PDUs are compared with expected pdu files in a directory supplied via command-line
     *
     * @return true if successful, false otherwise
     */
    public ReversalErrorCode reversePdus() throws Exception {
        IPduParserResult result;
        if (inputData.length == 0) {
            log.error("Provided PDU file was empty: ", reverseFileName);
            return ReversalErrorCode.FAILURE;
        }
        try {
            result = pduParser.parsePdus(inputData);
            log.trace("Created ", result.getPdus().size(), " PDU(s) from bytes=", inputData.length);
        } catch (Exception e) {
            log.error("Error processing PDUs from ", reverseFileName, ":", ExceptionTools.getMessage(e), " ... Shutting down. ");
            return ReversalErrorCode.FAILURE;
        }
        if (result.getPdus().isEmpty()) {
            log.error("No PDUs processed from ", reverseFileName);
            return ReversalErrorCode.FAILURE;
        }

        pduWriter.doReversePdus(result.getPdus());
        if(result.getException() != null) {
            log.error(result.getException());
        }

        // extract pdu data from pdu list
        List<byte[]> pduData = new ArrayList<>();
        result.getPdus().forEach((n) -> pduData.add(n.getData()));

        writeExtractedPdusToOutputDir(pduData);

        // Success cases
        // 1. no comparison option was provided
        // 2. comparison option was provided and comparison was successful

        if (pduDirectory == null ||
                (comparisonPdus != null && !comparisonPdus.isEmpty() && comparePdus(comparisonPdus.values(), pduData))) { // if comparison fails, an exception gets thrown)
            return ReversalErrorCode.SUCCESS;
        } else {
            return ReversalErrorCode.FAILURE;
        }
    }

    /**
     * Application specific configuration. Specifically, the PDU file to reverse.
     *
     * @param commandLine values entered by the user on the command line
     * @throws ParseException
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        // File option validates input files
        reverseFileName = reversePduFileOpt.parse(commandLine);
        if (reverseFileName != null && !Files.exists(Paths.get(reverseFileName))) {
            throw new ParseException("Invalid cltu file " + reverseFileName + " specified by " + LONG_REVERSE_PDU_FILE_OPT);
        }

        // MCSECLIV-438 2/8/21 Added quiet option for output suppression
        pduWriter.setSuppressOutput(quiet);
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
                         + "This application will attempt to extract binary (PDU) data from an file containing one or more CFDP PDUs. \n"
                         + "The extracted PDUs can be written to a specified directory if specified.\n"
                         + "The extracted PDUs can be compared to other PDUs in a directory if specified.");
    }

    /**
     * Creating the command line options for the app
     *
     * @return object containing basic command options, pduDirectory (for comparison), and
     * file to reverse
     */
    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }

        options = super.createOptions();

        options.addOption(reversePduFileOpt);

        return options;
    }


    /**
     * Main
     *
     * @param args
     *            Command-line arguments
     */
    public static void main(final String[] args) {

        final PduReverseApp app = new PduReverseApp();
        final Tracer log = TraceManager.getDefaultTracer();

        try {
            final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
            app.configure(commandLine);
            app.readFileInputs();
        }
        catch (final ParseException e) {
            log.error(ExceptionTools.getMessage(e));
        }
        try {
            ReversalErrorCode exitCode = app.reversePdus();
            log.info(exitCode);
            System.exit(ReversalErrorCode.getValue(exitCode));
        } catch (Exception ex) {
            log.warn(ReversalErrorCode.FAILURE, " - " + ex.getMessage());
            System.exit(ReversalErrorCode.getValue(ReversalErrorCode.FAILURE));
        }
    }
}