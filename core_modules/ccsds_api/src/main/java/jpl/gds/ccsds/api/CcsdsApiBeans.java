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
package jpl.gds.ccsds.api;

/**
 * Interface that defines static bean names for the Spring configuration 
 * in the CCSDS projects.
 *
 * @since R8
 */
public interface CcsdsApiBeans {  
    /**
     * Bean name for the CCSDS properties bean.
     */
    public static final String CCSDS_PROPERTIES = "CCSDS_PROPERTIES";
 
    /**
     * Bean name for the secondary packet header lookup bean.
     */
    public static final String SECONDARY_PACKET_HEADER_LOOKUP = "SECONDARY_PACKET_HEADER_LOOKUP";
    
    /**
     * Bean name for the CFDP PDU factory.
     */
    public static final String CFDP_PDU_FACTORY = "CFDP_PDU_FACTORY";

    /**
     * Bean name for secondary header extractor factory.
     */
    public static final String SECONDARY_HEADER_EXTRACTOR_FACTORY = "SECONDARY_HEADER_EXTRACTOR_FACTORY";

}
