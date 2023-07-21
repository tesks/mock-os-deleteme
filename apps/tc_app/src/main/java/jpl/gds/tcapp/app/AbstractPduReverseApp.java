package jpl.gds.tcapp.app;
/*
 * Copyright 2006-2020. California Institute of Technology.
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

import com.google.common.primitives.Bytes;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.util.BinOctHexUtility;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Abstract class that contains functionality common between the ReverseApps
 *
 */
public class AbstractPduReverseApp extends AbstractCommandLineApp {

    protected final ApplicationContext appContext;
    protected final Tracer log;

    protected String                        pduDirectory;
    protected String                        reverseFileName;
    private String                          outputDirectory;
    protected final Map<File, byte[]>       comparisonPdus;
    protected byte[]                        inputData;

    /** Long opt for pdu file directory */
    protected static final String       LONG_PDU_DIR_OPT            = "pduDir";
    /** Long opt for the output directory (if any) */
    public static final String       LONG_OUTPUT_DIR_OPT         = "outputDir";

    protected final DirectoryOption pduDirOpt                   = new DirectoryOption(null, LONG_PDU_DIR_OPT,
                                                                                      "pdu directory",
                                                                                      "Directory to scan for pdu files",
                                                                                      false, true, false);
    private final DirectoryOption    outputDirOpt                = new DirectoryOption(null, LONG_OUTPUT_DIR_OPT,
                                                                                       "output directory",
                                                                                       "Desired output directory for PDU file",
                                                                                       false, true, true);

    protected boolean quiet = false;

    public AbstractPduReverseApp() {
        appContext = SpringContextFactory.getSpringContext(true);
        log = TraceManager.getDefaultTracer();
        comparisonPdus = new HashMap<>();
    }

    /**
     * Internal method for comparing two lists of byte[] data, in this context they represent pdu's.
     *
     * @param expectedPdus
     *            List of expected byte[] data (pdus)
     * @param allExtractedPdus
     *            List of extracted byte[] data from cltus
     * @return
     */
    protected boolean comparePdus(final Collection<byte[]> expectedPdus, final Collection<byte[]> allExtractedPdus) throws InputMismatchException {
        log.info("Comparing PDUs.");

        if (expectedPdus.size() == allExtractedPdus.size()) {
            log.trace("Same number of extracted pdu's... and original pdus ", allExtractedPdus.size() , " vs ", expectedPdus.size());
        } // Size can be different in some cases. E.g. Extracting the same pdu from multiple cltu(s)

        // Don't know the order of pdu's, sorting would probably help for simpler compare?
        // This is probably sufficient for now since it a test utility, IMO

        // Reversing comparison from checking entire CLTU content against single PDU to
        // checking single PDU against entire CLTU content. This way supports the aggregate case better without
        // requiring PDU order.

        boolean equals = false;
        int extractedPduLength = 0;
        int expectedPduLength = 0;

        // get total length of extracted PDUs
        for (byte[] extractedPdu : allExtractedPdus) {
            extractedPduLength += extractedPdu.length;
        }

        // get total length of expected PDUs
        for (byte[] expectedPdu : expectedPdus) {
            expectedPduLength += expectedPdu.length;
        }

        for (final byte[] data : expectedPdus) { // loop through PDUs the directory; try to find each PDU in extracted CLTU
            equals = false;

            for (final byte[] data2 : allExtractedPdus) {
                log.trace("Comparing extracted\n",
                          BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(data2), 40),
                          "\nagainst expected\n",
                          BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(data), 40));

                // if extracted contains expected
                if((new String(data2)).contains(new String(data))) {
                    equals = true;
                    break;
                }
            }

            if (!equals) {
                log.warn("Extracted pdus from ", reverseFileName, " does not match pdus found in ", pduDirectory);
                log.trace("PDU\n", BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(data), 40),
                          "\nwas not found in ", pduDirectory);
                break;
            }
        }

        // checking the case where CLTUs can contain multiple PDUs: total lengths must match
        if (extractedPduLength != expectedPduLength) {
            equals = false;
            log.info("Extracted pdus from ", reverseFileName, " do not match length of pdus found in ", pduDirectory);
        }
        if(equals) {
            return equals;
        } else {
            throw new InputMismatchException("PDUs did not match.");
        }
    }

    /**
     * Loads cltu and pdu input files into memory
     *
     * @throws ParseException
     *             if an error occurs reading input files
     */
    public void readFileInputs() throws ParseException {
        // first, read in the files that are supposed to be used for comparison
        if (pduDirectory != null) {
            for (final File f : new File(pduDirectory).listFiles()) {
                try {
                    if(f.isHidden()) {
                        continue;
                    }
                    if(f.isDirectory()) {
                        continue;
                    }
                    final byte[] expectedPdu = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
                    log.debug("Loading PDU from ", f.getAbsolutePath(), " in ", pduDirectory, "\n",
                              BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(expectedPdu), 40));
                    comparisonPdus.put(f, expectedPdu);
                }
                catch (final IOException e) {
                    throw new ParseException("Unexpected error reading files: " + ExceptionTools.getMessage(e));
                }
            }
            if (comparisonPdus.isEmpty()) {
                log.warn("No files found in ", pduDirectory, ". Nothing to compare against extracted PDU.");
            }
        }

        try {
            inputData = Files.readAllBytes(Paths.get(reverseFileName));
            log.info("Reversing data from ", reverseFileName, ". Size=", inputData.length);
        }
        catch (final IOException e) {
            throw new ParseException("Unexpected error reading file: " + ExceptionTools.getMessage(e));
        }

    }

    /**
     * Write the extracted PDUs to an output directory if specified
     *
     * @param data
     *            extracted pdu data
     */
    protected void writeExtractedPdusToOutputDir(final List<byte[]> data) {
        if (outputDirectory != null && !outputDirectory.isEmpty()) {
            try {
                // write PDUs to individual files
                for (int i=0; i< data.size() ; ++i) {
                    final String pduFile = ApplicationConfiguration.getApplicationName() + "-" + i + ".out";
                    final Path p = Paths.get(outputDirectory, pduFile);
                    Files.write(p, data.get(i));
                    log.info("Wrote PDU data to ", outputDirectory);
                }

                // write all PDUs, concatenated, to one file
                final String aggregatedPduFile = ApplicationConfiguration.getApplicationName() + "-aggregated.out";
                Path aggregatePath = Paths.get(outputDirectory + "/aggregated", aggregatedPduFile);

                // create aggregated directory if it doesn't exist already
                if(!aggregatePath.getParent().toFile().exists()) {
                    Files.createDirectories(aggregatePath.getParent());
                }
                Files.write(aggregatePath, Bytes.concat(data.toArray(new byte[0][])));
                log.info("Wrote aggregated PDU data to ", aggregatePath.toAbsolutePath());

            } catch (final IOException e) {
                log.warn(ExceptionTools.getMessage(e));
                log.error(Markers.SUPPRESS, "Error writing to output file ", e); // stack trace to log file (no console)
            }
        }
    }

    /**
     * Creating the common command line options. For now, that's just the pduDirectory.
     *
     * @return object containing basic command options and pduDirectory
     */
    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }

        options = super.createOptions();

        options.addOption(pduDirOpt);
        options.addOption(outputDirOpt);
        options.addOption(BaseCommandOptions.QUIET);

        return options;
    }

    /**
     * Setting the common command line options. Right now, that's only setting the pduDirectory,
     * which is the directory that stores PDU files that will be compared to the PDUs the application
     * parses out.
     *
     * @param commandLine values entered by the user on the command line
     * @throws ParseException
     */
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        pduDirectory = pduDirOpt.parse(commandLine);
        if (pduDirectory != null && !Files.exists(Paths.get(pduDirectory))) {
            throw new ParseException("Invalid pdu directory " + pduDirectory + " specified by " + LONG_PDU_DIR_OPT);
        }
        outputDirectory = outputDirOpt.parse(commandLine);
        if (outputDirectory != null && !Files.exists(Paths.get(outputDirectory))) {
            throw new ParseException("Invalid pdu directory " + outputDirectory + " specified by " + LONG_OUTPUT_DIR_OPT);
        }

        // MCSECLIV-438 2/8/21 Added quiet option for output suppression
        quiet = BaseCommandOptions.QUIET.parse(commandLine, false);

    }
}
