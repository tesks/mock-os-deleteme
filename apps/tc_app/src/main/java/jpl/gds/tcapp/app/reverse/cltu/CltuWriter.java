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
package jpl.gds.tcapp.app.reverse.cltu;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.IBchCodeblock;
import jpl.gds.tc.api.ITcTransferFrame;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.VirtualChannelType;
import jpl.gds.tc.api.exception.FrameWrapUnwrapException;
import jpl.gds.tc.api.frame.ITcTransferFrameParser;
import jpl.gds.tcapp.app.reverse.AbstractDataWriter;
import jpl.gds.tcapp.app.reverse.frame.IFrameWriter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility that takes in a list of CLTUs, decomposes each one into frames and PDUs, and
 * writes the contents to the console.
 *
 */
public class CltuWriter extends AbstractDataWriter implements ICltuWriter {

    private final String CLTU_START_FORMAT  = SINGLE_LINE + "~~~~~~~~~~~~~~~~~~~~~~~CLTU #%d~~~~~~~~~~~~~~~~~~~~~~~" + DOUBLE_LINE;
    private final String CLTU_END_FORMAT    = SINGLE_LINE + "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" + DOUBLE_LINE;

    private final Tracer log;
    private final ITcTransferFrameParser parser;
    private final IFrameWriter           frameWriter;
    private final CommandFrameProperties commandFrameProperties;

    /**
     * Constructor for the ReverseCltuPduWriter
     *
     * @param parser
     *          ITcTransferFrameParser that will be used to parse CLTUs into Tc Transfer Frames
     * @param frameWriter
     *          utility to write contents of frames
     * @param printWriter
     *          PrintWriter that writes the contents to the console
     */
    public CltuWriter(final ITcTransferFrameParser parser, final IFrameWriter frameWriter, final PrintWriter printWriter, final CommandFrameProperties commandFrameProperties) {
        super(printWriter);
        this.parser = parser;
        this.frameWriter = frameWriter;
        this.commandFrameProperties = commandFrameProperties;

        this.log = TraceManager.getTracer(Loggers.TLM_PRODUCT);
    }

    /**
     * The main entry point of the class. Kicks off the extraction of frame and PDU data.
     *
     * @param cltus
     *           a list of ICltus to be decomposed into frames and PDUs
     * @return the PDU data contained within the CLTUs
     */
    public List<byte[]> doReverseCltus(List<ICltu> cltus) {
        // MCSECLIV-438 @jfwagner 2/9/21 frame writer inherits quiet setting from CLTU writer
        this.frameWriter.setSuppressOutput(this.getSuppressOutput());

        final List<byte[]> allExtractedPdus = new ArrayList<>();
        int cltuCounter = 0;

        // iterate through the CLTUs provided and write the contents of each to the console
        for (final ICltu cltu : cltus) {

            log.trace("CLTU ", cltu.getHexDisplayString());
            cltuCounter+=1;

            printCltuAcqAndStart(cltu, cltuCounter);

            try {
                // extract frames from CLTU. We only expect one frame in the list, but we're using a list to keep things flexible.
                // NB for Europa context: [FGICD-009] The GDS shall encapsulate only ONE (1) TC transfer frame per CLTU.
                List<ITcTransferFrame> frames = getFramesFromCltu(cltu);

                if (!getSuppressOutput()) {
                    printWriter.write(String.format("Total Frames in CLTU = %d%s", frames.size(), DOUBLE_LINE));
                }

                // process frames
                allExtractedPdus.addAll(frameWriter.doReverseFrames(frames));

            } catch (final Exception ex) {
                // Don't suppress error output even if --quiet was provided
                printWriter.write(String.format("Unable to extract frames from CLTUs. %s", SINGLE_LINE));
                writeHexBlob("CLTU hex: ", cltu.getData());
                log.error("Exception encountered during CLTU decomposition: ", ExceptionTools.getMessage(ex));
            }

            printCltuTailAndIdle(cltu);
            printWriter.flush();
        }

        log.debug(allExtractedPdus.size(), " bytes of pdu data extracted");

        cleanUp();

        return allExtractedPdus;
    }

    /**
     * Prints only the CLTU acquisition and start sequences, along with the cltuCounter, which is the position
     * of this CLTU in the list provided
     *
     * @param cltu
     *          the CLTU that will have its start and acquisition sequences printed
     * @param cltuCounter
     *          integer number that tracks which number CLTU this one is in the list provided
     */
    protected void printCltuAcqAndStart(ICltu cltu, int cltuCounter) {
        if (!getSuppressOutput()) {
            // write acq sequence
            writeHexBlob("Acquisition Sequence (Hex) = ", cltu.getAcquisitionSequence());
            printWriter.write(String.format("Start Acquisition Bit Length = %d%s", cltu.getAcquisitionSequence().length * 8, SINGLE_LINE));

            // write start sequence
            printWriter.write(String.format(CLTU_START_FORMAT, cltuCounter));
            printWriter.write(String.format("Start Sequence (Hex) = %s%s", BinOctHexUtility.toHexFromBytes(cltu.getStartSequence()), SINGLE_LINE));
            printWriter.write(String.format("Start Sequence Bit Length = %d%s", cltu.getStartSequence().length * 8, SINGLE_LINE));
        }
    }

    /**
     * Prints only the CLTU tail and idle sequences
     *
     * @param cltu
     *          the CLTU that will have its tail and idle sequences printed
     */
    protected void printCltuTailAndIdle(ICltu cltu) {
        if (!getSuppressOutput()) {
            // write tail sequence
            printWriter.write(String.format("Tail Sequence (Hex) = %s%s", BinOctHexUtility.toHexFromBytes(cltu.getTailSequence()), SINGLE_LINE));
            printWriter.write(String.format("Tail Sequence Bit Length = %d%s", cltu.getTailSequence().length * 8, SINGLE_LINE));
            printWriter.write(CLTU_END_FORMAT);

            // write idle sequence
            printWriter.write(String.format("Idle Sequence (Hex) = %s%s", BinOctHexUtility.toHexFromBytes(cltu.getIdleSequence()), SINGLE_LINE));
            printWriter.write(String.format("Start Idle Bit Length = %d%s", cltu.getAcquisitionSequence().length * 8, SINGLE_LINE));
            printWriter.write(DOUBLE_LINE);
        }
    }


    /**
     * Takes in a CLTU and returns the frames inside. Accounts for two cases that can occur during
     * CLTU decomposition:
     * 1. the case in which the CLTU was created with frames
     * 2. the case in which the CLTU was created, but CTS didn't populate frames
     *
     * For the purposes of decomposing the contents of the CLTU to the console, it's more helpful to have
     * the data portion of the frame contain both the header data and the user data, because we need
     * both to be able to calculate the FECF.
     *
     * @param cltu
     *          CLTU to be decomposed into ITcTransferFrames
     * @return a list of ITcTransferFrames
     */
    protected List<ITcTransferFrame> getFramesFromCltu(ICltu cltu) throws FrameWrapUnwrapException {
        List<ITcTransferFrame> frames = new ArrayList<ITcTransferFrame>();

        // if MPS had a problem parsing the CLTUs, then the frames may be empty. In that case, we will parse differently.
        if(!cltu.getFrames().isEmpty()) {

            // but MPS returns the frame data with no frame header. In order to calculate FECF down the road,
            // we need that header, and the ITcTransferFrame doesn't include it. We have to get it manually.
            for (ITcTransferFrame frame : cltu.getFrames() ) {
                byte[] frameHeader = Arrays.copyOfRange(cltu.getData(), 0, FRAME_HEADER_SIZE_BYTES);
                byte[] dataWithFrameHeader = new byte[frameHeader.length + frame.getData().length];
                System.arraycopy(frameHeader, 0, dataWithFrameHeader, 0, frameHeader.length);
                System.arraycopy(frame.getData(), 0, dataWithFrameHeader, frameHeader.length, frame.getData().length);
                frame.setData(dataWithFrameHeader);
                frames.add(frame);
            }
            return frames;
        }

        if (!getSuppressOutput()) {
            printWriter.write(String.format("Nominal CTS workflow did not parse frames for this CLTU. Proceeding with alternate CTS parsing of CLTU data. Is your telecmd.xml file up-to-date? %s", SINGLE_LINE));
        }

        ITcTransferFrame frame = parser.parse(cltu.getData(), true);

        // In this workflow, the bytes getting set into the frame's data field are still BCH encoded.
        // We can't create PDUs out of BCH encoded data, so we decode it using the codeblocks on the frame,
        // and replace the data field.
        String decodedData = "";
        List<IBchCodeblock> codeblocks = cltu.getCodeblocks();
        for (IBchCodeblock block : codeblocks) {
            decodedData = decodedData.concat(BinOctHexUtility.toHexFromBytes(block.getData()));
        }

        // CTS sets FECF incorrectly, so we grab the last two valid bytes of the decoded data as our FECF
        byte[] decodedDataBytes = BinOctHexUtility.toBytesFromHex(decodedData);

        overrideHasFecf(frame);

        if (frame.hasFecf()) {
            // CTS sets FECF incorrectly, so we grab the last two valid bytes of the decoded data as our FECF
            byte[] fecf = new byte[] { decodedDataBytes[frame.getLength() - 1], decodedDataBytes[frame.getLength()] };
            frame.setFecf(fecf);
        }

        frame.setData(decodedDataBytes);

        frames.add(frame);

        return frames;
    }

    /**
     * Overrides frame's hasFecf value with true/false based on command frame configuration
     *
     * @param tcTransferFrame
     *              the frame to be modified
     */
    private void overrideHasFecf(ITcTransferFrame tcTransferFrame) {
        boolean hasFecf = commandFrameProperties.hasFecf(getVirtualChannelType(tcTransferFrame));
        tcTransferFrame.setHasFecf(hasFecf);
    }

    /**
     * Retrieves the Virtual Channel Type of the frame
     * @param tcTransferFrame
     *              the frame to be analyzed for VC type
     * @return VirtualChannelType enum
     */
    private VirtualChannelType getVirtualChannelType(ITcTransferFrame tcTransferFrame) {
        switch (tcTransferFrame.getVirtualChannelNumber()) {
            case 0: return VirtualChannelType.HARDWARE_COMMAND;
            case 1: return VirtualChannelType.FLIGHT_SOFTWARE_COMMAND;
            case 2: return VirtualChannelType.FILE_LOAD;
            case 3: return VirtualChannelType.CFDP;
            case 5: return VirtualChannelType.DELIMITER;
            default:
                log.warn("Unexpected Virtual Channel Type: " + tcTransferFrame.getVirtualChannelNumber() + ". Defaulting to CFDP Type, which has FECF. Note that if the frame does not have FECF, this assumption will throw off the parsing.");
                return VirtualChannelType.CFDP;
        }
    }
}