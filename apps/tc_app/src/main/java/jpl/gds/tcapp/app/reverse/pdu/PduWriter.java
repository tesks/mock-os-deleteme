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
package jpl.gds.tcapp.app.reverse.pdu;

import jpl.gds.ccsds.api.cfdp.*;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tcapp.app.reverse.AbstractDataWriter;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Utility that takes in a list of CFDP PDUs, decomposes each one into its contents according to its type, and
 * writes the contents to the console.
 *
 */
public class PduWriter extends AbstractDataWriter implements IPduWriter {

    private static final String PDU_START_FORMAT   = SINGLE_LINE + "=========PDU #%d============" + DOUBLE_LINE;
    private static final String PDU_END_FORMAT     = SINGLE_LINE + "============================" + DOUBLE_LINE;

    // the PDU CRC algorithm is not currently configurable. Goddard uses CRC-16 (see HDR.java in JavaCFDP repo).
    private static final int PDU_CRC_LENGTH_BYTES  = 2;

    private final Tracer log;

    /**
     * Constructor for PduWriter
     *
     * @param printWriter PrintWriter writes data to the console
     */
    public PduWriter(final PrintWriter printWriter) {
        super(printWriter);
        this.log = TraceManager.getTracer(Loggers.TLM_PRODUCT);
    }

    /**
     * The main entry point of the class. Kicks off the extraction PDU data.
     *
     * @param pdus List of PDUs to write
     */
    @Override
    public void doReversePdus(List<ICfdpPdu> pdus) {
        // MCSECLIV-438 2/8/21 Added quiet option for output suppression
        if (getSuppressOutput()) {
            log.debug("Skipping PDU output to console because quiet mode is TRUE");
            return;
        }
        int pduCounter = 0;

        for (ICfdpPdu pdu : pdus) {
            pduCounter+=1;

            try {
                printPduHeader(pdu, pduCounter);

                printPduDetailsByType(pdu);

                printPduEnd(pdu);

            } catch(Exception ex) {
                log.error("Unable to display PDU contents. Exception thrown: ", ex);
                printWriter.write(String.format("Unable to display PDU contents. PDU = %s%s", BinOctHexUtility.toHexFromBytes(pdu.getData()), SINGLE_LINE));
            }

            printWriter.flush();
        }
    }

    /**
     * Prints the contents of a PDU header
     *
     * @param pdu
     *          the ICfdpPdu whose data and header content will be printed to the console
     * @param pduCounter
     *          the position of the PDU inside the frame
     */
    protected void printPduHeader(ICfdpPdu pdu, int pduCounter) {
        log.trace("PDU ", pdu.getData());

        ICfdpPduHeader header = pdu.getHeader();

        printWriter.write(String.format(PDU_START_FORMAT, pduCounter));
        printWriter.write(String.format("Version = %s%s", header.getVersion(), SINGLE_LINE));
        printWriter.write(String.format("PDU Type = %s%s", header.getType().name(), SINGLE_LINE));
        printWriter.write(String.format("Direction = %s%s", header.getDirection().name(), SINGLE_LINE));
        printWriter.write(String.format("Transmission Mode = %s%s", header.getTransmissionMode().name(), SINGLE_LINE));
        printWriter.write(String.format("CRC Flag = %s%s", header.hasCrc(), SINGLE_LINE));
        printWriter.write(String.format("PDU Data Length = %s%s", header.getDataLength(), SINGLE_LINE));
        printWriter.write(String.format("Length of Entity IDs = %s%s", header.getEntityIdLength(), SINGLE_LINE));
        printWriter.write(String.format("Transaction Sequence Number Length = %s%s", header.getTransactionSequenceLength(), SINGLE_LINE));
        printWriter.write(String.format("Source Entity ID = %s%s", header.getSourceEntityId(), SINGLE_LINE));
        printWriter.write(String.format("Transaction Sequence Number = %s%s", Long.toUnsignedString(header.getTransactionSequenceNumber()), SINGLE_LINE));
        printWriter.write(String.format("Destination Entity ID = %s%s", header.getDestinationEntityId(), SINGLE_LINE));

        if (header.hasCrc()) {
            printWriter.write(String.format("PDU CRC = %s%s", BinOctHexUtility.toHexFromBytes(getCrc(pdu)), SINGLE_LINE));
        }
    }

    /**
     * Prints the end of the PDU format, including PDU data, a line, and some space
     *
     * @param pdu the PDU containing data to print
     */
    protected void printPduEnd(ICfdpPdu pdu) {

        // write pdu data only if we haven't already written it
        if(!(ICfdpFileDataPdu.class.isAssignableFrom(pdu.getClass()))) {
            writeHexBlob("PDU Data = ", pdu.getData());
        }

        printWriter.write(PDU_END_FORMAT);
    }

    /**
     * Logic to choose which type of detailed PDU data to print, based on the type of PDU
     *
     * @param pdu the PDU containing data to print
     */
    protected void printPduDetailsByType(ICfdpPdu pdu) {
        printWriter.write(SINGLE_LINE);

        // If METADATA
        if(ICfdpMetadataPdu.class.isAssignableFrom(pdu.getClass())) {
            printMetadataPdu((ICfdpMetadataPdu) pdu);
        }

        // If any type of ACK
        if(ICfdpAckPdu.class.isAssignableFrom(pdu.getClass())) {
            printAckPdu((ICfdpAckPdu)pdu);
        }

        // If any type of NAK
        if(ICfdpNakPdu.class.isAssignableFrom(pdu.getClass())) {
            printNakPdu((ICfdpNakPdu) pdu);
        }

        // If EOF
        if(ICfdpEofPdu.class.isAssignableFrom(pdu.getClass())) {
            printEofPdu((ICfdpEofPdu) pdu);
        }

        // If File Data
        if(ICfdpFileDataPdu.class.isAssignableFrom(pdu.getClass())) {
            printFileDataPdu((ICfdpFileDataPdu) pdu);
        }

        // If FIN
        if(ICfdpFinishedPdu.class.isAssignableFrom(pdu.getClass())) {
            printFinPdu((ICfdpFinishedPdu) pdu);
        }

        printWriter.write(SINGLE_LINE);
    }

    /**
     * Prints the specific contents of an ACK PDU
     *
     * @param ackPdu the PDU containing data to print
     */
    protected void printAckPdu(ICfdpAckPdu ackPdu) {
        printWriter.write(SINGLE_LINE);
        printWriter.write(String.format("Directive Type = %s%s", ackPdu.getDirectiveCode(), SINGLE_LINE));

        FileDirectiveCode directiveCode = getAckFileDirectiveCode(ackPdu);
        printWriter.write(String.format("Acknowledgement Directive Code = %s%s", directiveCode.toString(), SINGLE_LINE));

        AckTransactionStatus statusCode = getAckTransactionStatus(ackPdu);
        printWriter.write(String.format("Acknowledgement Transaction Status = %s%s", statusCode.toString(), SINGLE_LINE));
        // Missing fields required by the FGICD. Not currently supported by AMPCS core (9/28/2020)
        // * Directive Subtype Code
        // * Condition Code
    }

    /**
     * Prints the specific contents of an EOF PDU
     *
     * @param eofPdu the PDU containing data to print
     */
    protected void printEofPdu(ICfdpEofPdu eofPdu) {
        printWriter.write(SINGLE_LINE);
        printWriter.write(String.format("Directive Type = %s%s", eofPdu.getDirectiveCode(), SINGLE_LINE));
        printWriter.write(String.format("Condition Code = %s%s", eofPdu.getConditionCode(), SINGLE_LINE));
        printWriter.write(String.format("Fault Location = %s%s", eofPdu.getFaultLocation(), SINGLE_LINE));
        printWriter.write(String.format("Checksum = %s%s", Long.toHexString(eofPdu.getFileChecksum()), SINGLE_LINE));
        printWriter.write(String.format("File Size (bytes) = %s%s", eofPdu.getFileSize(), SINGLE_LINE));
    }

    /**
     * Prints the specific contents of a File Data PDU
     *
     * @param fileDataPdu the PDU containing data to print
     */
    protected void printFileDataPdu(ICfdpFileDataPdu fileDataPdu) {
        printWriter.write(SINGLE_LINE);

        writeHexBlob("File Data", fileDataPdu.getData());
    }

    /**
     * Prints the specific contents of a FIN PDU
     *
     * @param finPdu the PDU containing data to print
     */
    protected void printFinPdu(ICfdpFinishedPdu finPdu) {
        printWriter.write(SINGLE_LINE);
        printWriter.write(String.format("Directive Type = %s%s", finPdu.getDirectiveCode(), SINGLE_LINE));
        printWriter.write(String.format("Condition Code = %s%s", finPdu.getConditionCode(), SINGLE_LINE));
        printWriter.write(String.format("End System Status = %s%s", finPdu.getEndSystemStatus(), SINGLE_LINE));
        printWriter.write(String.format("Delivery Code = %s%s", finPdu.getDeliveryCode(), SINGLE_LINE));
        printWriter.write(String.format("File Status = %s%s", finPdu.getFileStatus(), SINGLE_LINE));

        // print filestore responses
        if(finPdu.getFilestoreResponses() != null) {
            finPdu.getFilestoreResponses().forEach((n) ->
                    printWriter.write(String.format("Filestore Response: %s%s", n, SINGLE_LINE)));
        }

        printWriter.write(String.format("Fault Location = %s%s", finPdu.getFaultLocation(), SINGLE_LINE));
    }

    /**
     * Prints the specific contents of a Metadata PDU
     *
     * @param mPdu the PDU containing data to print
     */
    protected void printMetadataPdu(ICfdpMetadataPdu mPdu) {
        printWriter.write(SINGLE_LINE);
        printWriter.write(String.format("Directive Type = %s%s", mPdu.getDirectiveCode(), SINGLE_LINE));
        printWriter.write(String.format("Source File Name = %s%s", mPdu.getSourceFileName(), SINGLE_LINE));
        printWriter.write(String.format("Destination File Name = %s%s", mPdu.getDestinationFileName(), SINGLE_LINE));
        printWriter.write(String.format("File Size (bytes) = %s%s", mPdu.getFileSize(), SINGLE_LINE));
        printWriter.write(String.format("Segmentation Control = %s%s", mPdu.getSegmentationControl(), SINGLE_LINE));
        printWriter.write(String.format("Options = %s%s", mPdu.getOptions(), SINGLE_LINE));
    }

    /**
     * Prints the specific contents of a NAK PDU
     *
     * @param nakPdu the PDU containing data to print
     */
    protected void printNakPdu(ICfdpNakPdu nakPdu) {
        printWriter.write(SINGLE_LINE);
        printWriter.write(String.format("Directive Type = %s%s", nakPdu.getDirectiveCode(), SINGLE_LINE));
        printWriter.write(String.format("Start of Scope = %s%s", nakPdu.getStartOfScope(), SINGLE_LINE));
        printWriter.write(String.format("End of Scope = %s%s", nakPdu.getEndOfScope(), SINGLE_LINE));

        // print segment requests
        nakPdu.getSegmentRequests().forEach((n) ->
                printWriter.write(String.format("Segment Request: {Start: %s, End:%s}%s", n.getStartOffset(), n.getEndOffset(), SINGLE_LINE)));
    }

    /**
     * Method to isolate the retrieval of the acknowledged directive code. Separating it like this makes
     * unit testing easier, and allows us to refactor more easily if the underlying core implementation changes.
     *
     * @param pdu ACK PDU containing the acknowledged directive code
     * @return the ACK PDU's FileDirectiveCode
     */
    protected FileDirectiveCode getAckFileDirectiveCode(ICfdpAckPdu pdu) {
        return ICfdpAckPdu.getAcknowledgedDirectiveCode(pdu.getData(), pdu.getHeader().getHeaderLength());
    }

    /**
     * Method to isolate the retrieval of the acknowledged transaction status code. Separating it like this makes
     * unit testing easier, and allows us to refactor more easily if the underlying core implementation changes.
     *
     * @param pdu ACK PDU containing the acknowledged transaction status
     * @return the ACK PDU's AckTransactionStatus
     */
    protected AckTransactionStatus getAckTransactionStatus(ICfdpAckPdu pdu) {
        return ICfdpAckPdu.getAcknowledgedTransactionStatus(pdu.getData(), pdu.getHeader().getHeaderLength());
    }

    /**
     * Extracts the CRC, which is the last two bytes of the PDU if CRC is being used. For now, only CRC-16
     * is supported, so the CRC length is fixed.
     * CFDP BlueBook v4 4.1.1.3.2
     *
     * @param pdu a PDU with a checksum
     * @return the last two bytes of the PDU data
     */
    protected byte[] getCrc(ICfdpPdu pdu) {

        if(pdu.getData() != null && pdu.getData().length > PDU_CRC_LENGTH_BYTES) {
            int pduLength = pdu.getData().length;
            return Arrays.copyOfRange(pdu.getData(), pduLength - PDU_CRC_LENGTH_BYTES, pduLength);
        }
        return new byte[]{};
    }
}
