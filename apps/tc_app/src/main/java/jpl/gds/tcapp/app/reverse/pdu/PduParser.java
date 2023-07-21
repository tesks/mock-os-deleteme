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

import jpl.gds.ccsds.api.cfdp.ICfdpPduFactory;
import jpl.gds.ccsds.api.cfdp.ICfdpPduHeader;
import jpl.gds.shared.util.BinOctHexUtility;

import java.util.Arrays;

/**
 * Utility to parse a byte array into one or more CFDP PDUs
 *
 */
public class PduParser implements IPduParser {

    private final ICfdpPduFactory pduFactory;

    /**
     * Constructor
     * @param pduFactory a tool from AMPCS core that can parse a single PDU from byte data
     */
    public PduParser(final ICfdpPduFactory pduFactory) {
        this.pduFactory = pduFactory;
    }

    /**
     * Allows users to enter a data set and attempts to parse it to a list of PDUs.
     * @param pduData
     * @return a list of ICfdpPdus
     */
    @Override
    public IPduParserResult parsePdus(final byte[] pduData) throws PduParsingException {

        PduParserResult result = new PduParserResult();

        int pduStartIndex = 0;

        // Since there is the possibility of more than one PDU per frame,
        // we iterate until we have consumed all of the transfer frame data
        while(pduData.length > pduStartIndex && pduData.length - pduStartIndex > ICfdpPduHeader.FIXED_PDU_HEADER_LENGTH) {

            int pduEndIndex = 0;
            byte[] dataToTry = new byte[0];

            try {
                // Creating the PDU header to get metadata
                dataToTry = Arrays.copyOfRange(pduData, pduStartIndex, pduData.length);
                ICfdpPduHeader header = pduFactory.createPduHeader();
                int headerSizeBytes = header.load(dataToTry, 0);
                int totalPduLengthBytes = headerSizeBytes + header.getDataLength();
                pduEndIndex = pduStartIndex + totalPduLengthBytes;

                // now that we know the start and end, get all the bytes for this PDU
                byte[] pduBytes = Arrays.copyOfRange(pduData, pduStartIndex, pduEndIndex);

                result.addPdu(pduFactory.createPdu(pduBytes));

            } catch(Exception ex) {
                // if we get one a PDU, then we can't guarantee the integrity of the stream. Best to leave the method.
                result.setException(new PduParsingException(
                        String.format(
                            "Unable to parse the following data into a CFDP PDU: %s. Exception: %s",
                            BinOctHexUtility.toHexFromBytes(dataToTry),
                            ex.getMessage())));
                break; // we've hit an exception, so leave the loop
            }

            pduStartIndex = pduEndIndex;
        }

        return result;

    }
}
