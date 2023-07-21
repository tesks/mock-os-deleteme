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


/**
 * An interface to be implemented by CFDP EOF directive PDUs.
 * 
 *
 * @since R8
 *
 */
public interface ICfdpEofPdu extends ICfdpFileDirectivePdu {
    
    /**
     * Gets the file checksum from the EOF PDU.
     * 
     * @return checksum
     */
    public long getFileChecksum();
    
    /**
     * Gets the file size from the EOF PDU.
     * 
     * @return the file size in bytes
     */
    public long getFileSize();
    
    /**
     * Gets the fault location from the EOF PDU.
     * 
     * @return CfdpTlv object
     */
    public CfdpTlv getFaultLocation();

    /**
     * Gets the file condition code from the EOF PDU.
     * 
     * @return FileDirectiveConditionCode
     */
    public FileDirectiveConditionCode getConditionCode(); 
}

