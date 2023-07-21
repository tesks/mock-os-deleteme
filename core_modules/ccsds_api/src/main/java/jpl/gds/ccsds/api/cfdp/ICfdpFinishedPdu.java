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


import java.util.List;

/**
 * An interface to be implemented by CFDP Finished directive PDUs.
 * 
 *
 * @since R8.1
 *
 */
public interface ICfdpFinishedPdu extends ICfdpFileDirectivePdu {
    
    /**
     * Gets the end system status from the Finished PDU.
     * 
     * @return the end system status
     */
    public CfdpEndSystemStatus getEndSystemStatus();
    
    /**
     * Gets the delivery code from the Finished PDU.
     * 
     * @return the delivery code
     */
    public CfdpDeliveryCode getDeliveryCode();

    /**
     * Gets the file status from the Finished PDU.
     *
     * @return the delivery code
     */
    public CfdpFileStatus getFileStatus();

    /**
     * Gets the filestore responses from the Finished PDU.
     *
     * @return filestore responses
     */
    public List<CfdpTlv> getFilestoreResponses();

    /**
     * Gets the fault location from the Finished PDU.
     * 
     * @return CfdpTlv object
     */
    public CfdpTlv getFaultLocation();

    /**
     * Gets the file condition code from the Finished PDU.
     * 
     * @return FileDirectiveConditionCode
     */
    public FileDirectiveConditionCode getConditionCode(); 
}

