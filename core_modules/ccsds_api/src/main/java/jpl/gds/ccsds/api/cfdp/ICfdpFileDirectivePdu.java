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
package jpl.gds.ccsds.api.cfdp;

import jpl.gds.shared.gdr.GDR;

/**
 * An interface to be implemented by CFDP file directove PDUs.
 * 
 * @since R8
 */
public interface ICfdpFileDirectivePdu extends ICfdpPdu {
    
    /**
     * A static utility method that gets the file directive code from the PDU header
     * given a byte buffer.
     * 
     * @param buffer the byte buffer containing the PDU header data.
     * @param offset the starting offset of the header in the buffer
     * @return FileDirectiveCode
     */
    public static FileDirectiveCode getDirectiveCode(byte[] buffer, int offset) {
        final int val = GDR.get_u8(buffer, offset);
        for (final FileDirectiveCode code: FileDirectiveCode.values()) {
           if (code.getBinaryValue() == val) {
               return code;
           }
        }
        
        throw new IllegalStateException("File directive code in PDU buffer does not match any known directive code");
    }


    /**
     * Gets the file directive code from the PDU header.
     * 
     * @return FileDirectiveCode
     */
    public FileDirectiveCode getDirectiveCode();    
 
}
