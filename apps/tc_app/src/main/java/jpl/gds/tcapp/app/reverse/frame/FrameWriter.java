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
package jpl.gds.tcapp.app.reverse.frame;

import jpl.gds.shared.checksum.IChecksumCalculator;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tcapp.app.reverse.AbstractDataWriter;
import jpl.gds.tcapp.app.reverse.pdu.IPduParser;
import jpl.gds.tcapp.app.reverse.pdu.IPduParserResult;
import jpl.gds.tcapp.app.reverse.pdu.IPduWriter;
import jpl.gds.tcapp.app.reverse.pdu.PduParserResult;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility that takes in a list of TcTransferFrames, decomposes each one into frame metadata and PDUs, and
 * writes the contents to the console.
 *
 */
public class FrameWriter extends AbstractDataWriter implements IFrameWriter {

    private final String FRAME_START_FORMAT = SINGLE_LINE + "---------------FRAME #%d--------------" + DOUBLE_LINE;
    private final String FRAME_END_FORMAT   = SINGLE_LINE + "------------------------------------" + DOUBLE_LINE;
    private final int FRAME_FECF_LENGTH     = 2;

    private final IPduParser             pduParser;
    private final IPduWriter             pduWriter;
    private final CommandFrameProperties commandFrameProperties;
    private final Tracer log;

    private final IChecksumCalculator checksumCalculator;

    /**
     * Constructor for FrameWriter
     *
     * @param pduWriter class that handles PDU decomposition
     * @param checksumCalculator utility that calculates CRC16 checksum
     * @param pduParser class that can parse PDUs from a byte array
     * @param printWriter class that writes data to the console
     */
    public FrameWriter(final IPduWriter pduWriter, final IChecksumCalculator checksumCalculator,
                       final IPduParser pduParser, final PrintWriter printWriter,
                       final CommandFrameProperties commandFrameProperties) {
        super(printWriter);
        this.checksumCalculator = checksumCalculator;
        this.pduParser = pduParser;
        this.pduWriter = pduWriter;
        this.commandFrameProperties = commandFrameProperties;
        this.log = TraceManager.getTracer(Loggers.TLM_PRODUCT);
    }

    /**
     * Decomposes each frame in the list and writes it and its PDUs to the console
     *
     * @param frames
     *      the list of ITcTransferFrames to be written to the console and have their PDUs extracted
     * @return a list of bytes containing the data inside the PDUs
     */
    @Override
    public List<byte[]> doReverseFrames(List<ITcTransferFrame> frames) {
        // MCSECLIV-438 @jfwagner 2/9/21 pdu writer inherits quiet setting from frame writer
        this.pduWriter.setSuppressOutput(this.getSuppressOutput());

        List<byte[]> extractedPdus = new ArrayList<>();
        int frameCounter = 0;

        // write the frame header data and extract any PDUs
        for (final ITcTransferFrame tcFrame : frames) {
            log.trace("FRAME ", tcFrame);
            try {
                frameCounter += 1;
                printFrameHeader(tcFrame, frameCounter);

                IPduParserResult pduParserResult = getPdusFromFrame(tcFrame);

                pduWriter.doReversePdus(pduParserResult.getPdus());

                // PDU parsing may have returned an exception, so write that out if it happened
                if(pduParserResult.getException() != null) {
                    log.error(pduParserResult.getException());
                    printWriter.write(String.format("A problem was encountered during PDU parsing. Please see errors for details.%s", SINGLE_LINE));
                }

                printFrameEnd();

                printWriter.flush();

                // get the PDU data to return
                pduParserResult.getPdus().forEach((n) -> extractedPdus.add(n.getData()));

            } catch (Exception ex) {
                log.error("Unable to parse frame. Exception thrown: ", ex);
                printWriter.write(String.format("Unable to parse PDU from frame. Frame Hex = %s%s", BinOctHexUtility.toHexFromBytes(tcFrame.getData()), SINGLE_LINE));
            }
        }

        return extractedPdus;
    }

    /**
     * Prints the data from the TcTransferFrame Header, along with the frameCounter, which tracks this frame's
     * position in the list of frames provided
     *
     * @param tcFrame
     *          the frame that will have its header data printed
     * @param frameCounter
     *          the location of the frame in the original list of frames given
     */
    protected void printFrameHeader(ITcTransferFrame tcFrame, int frameCounter) {
        // MCSECLIV-438 2/8/21 Added quiet option for output suppression
        if (!getSuppressOutput()) {
            printWriter.write(String.format(FRAME_START_FORMAT, frameCounter));
	        printWriter.write(tcFrame.getHeaderString());
	        printWriter.write(String.format("Virtual Channel ID = %s%s", tcFrame.getVirtualChannelId(), SINGLE_LINE));
	        printWriter.write(String.format("CE String Mnemonic = %s%s", getComputeElementString(tcFrame), SINGLE_LINE));
	        printWriter.write(String.format("FECF = %s%s", (tcFrame.hasFecf() ? BinOctHexUtility.toHexFromBytes(tcFrame.getFecf()) : "N/A"), SINGLE_LINE));
	        printWriter.write(String.format("(If CRC16) Does FECF match? %s%s", getCrc16(tcFrame), SINGLE_LINE));
        }
    }

    /**
     * Prints the necessary formatting for the end of the frame
     */
    protected void printFrameEnd() {
        if (!getSuppressOutput()) {
            printWriter.write(FRAME_END_FORMAT);
        }
    }

    /**
     * Converts a ITcTransferFrame into PDU(s).
     * Built to handle the possibility of multiple PDUs in one Tc Transfer Frame.
     *
     * @param tcTransferFrame
     *          the ITcTransferFrame to be parsed into PDU(s)
     * @return PduParserResult the result of the PDU parsing. Contains the any PDUs found and any errors that occurred during parsing
     */
    protected IPduParserResult getPdusFromFrame(ITcTransferFrame tcTransferFrame) {

        writeHexBlob("Hex frame (including header) =", tcTransferFrame.getData());

        // remove header, fecf (if present), and filler
        byte[] dataMinusFrameHeader = Arrays.copyOfRange(tcTransferFrame.getData(), FRAME_HEADER_SIZE_BYTES, (tcTransferFrame.getLength()+1) - getFecfLengthBytes(tcTransferFrame));

        IPduParserResult result = new PduParserResult(); // we want it to be empty, not null

        try {
            // use the PduParser to get one or more PDUs from the byte array
            result = pduParser.parsePdus(dataMinusFrameHeader);
        } catch (Exception ex) {
            printWriter.write(String.format("Unable to parse PDU from frame. Frame Hex = %s%s", BinOctHexUtility.toHexFromBytes(dataMinusFrameHeader), SINGLE_LINE));
            log.error("Unable to parse PDUs. Exception: ", ex);
        }

        return result;
    }

    /**
     * Method to calculate the CRC-16/CCITT-FALSE checksum for a given transfer frame
     * Expecting the tcFrame.getData to include the data header
     *
     * @param tcFrame
     *          the ITcTransferFrame to use for the checksum calculation
     * @return the calculated CRC16 checksum as a hex string
     */
    protected String getCrc16(ITcTransferFrame tcFrame) {

        // The data we are using to calculate the CRC16 is selected as follows:
        // it's the raw data from the frame, plus 1, since the data lengths is stored as one less than the
        // actual number of bytes (per CCSDS). Then we subtract two because the data in the frame
        // still has its FECF (2 bytes) and we don't want to include the FECF in the FECF calculation.
        byte[] data = Arrays.copyOfRange(tcFrame.getData(), 0, (tcFrame.getLength()+1)-2);

        long checksum = checksumCalculator.calculateChecksum(data, 0, data.length);

        return Long.toHexString(checksum);
    }

    /**
     * Returns the integer length of the frame's FECF in bytes
     *
     * @param tcFrame
     *          the frame to use to determine whether FECF has length or not
     * @return byte length of the FECF
     */
    protected int getFecfLengthBytes(ITcTransferFrame tcFrame) {
        if (tcFrame.hasFecf()) {
            return FRAME_FECF_LENGTH;
        } else {
            return 0;
        }
    }

    /**
     * Uses AMPCS configuration to find the mnemonic string for the compute element byte indicated
     * on the TC Transfer Frame
     *
     * @param tcFrame
     *          the frame to use to determine CE String
     * @return CE string in mnemonic form
     */
    protected String getComputeElementString(ITcTransferFrame tcFrame) {
        ExecutionStringType[] ceStrings = ExecutionStringType.values();

        for (ExecutionStringType t : ceStrings) {
            if(commandFrameProperties.getStringIdVcidValue(t.toString()) == tcFrame.getExecutionString()) {
                return t.toString();
            }
        }

        throw new IllegalStateException("The CE String found in the frame does not match any of the Strings configured for this mission.");
    }
}
