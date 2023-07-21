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
 * An interface to be implemented by CFDP metadata PDUs.
 * 
 *
 * @since R8
 *
 */
public interface ICfdpMetadataPdu extends ICfdpFileDirectivePdu {
    
    /**
     * Gets the CFDP Segmentation control type from the metadata PDU.
     * 
     * @return  CfdpSegmentationControlType
     */
    public CfdpSegmentationControlType getSegmentationControl();
    
    /**
     * Gets the file size from the metadata PDU.
     * 
     * @return file size in bytes
     */
    public long getFileSize();
    
    /**
     * Gets the source file name from the metadata PDU.
     * 
     * @return source file name
     */
    public String getSourceFileName();
    
    /**
     * Gets the destination file name from the metadata PDU.
     * 
     * @return destination file name
     */
    public String getDestinationFileName();
    
    /**
     * Gets the list of TLV options from the metadata PDU.
     * 
     * @return list of CfdpTlv
     */ 
    public List<CfdpTlv> getOptions();
    
}
