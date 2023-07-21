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
package jpl.gds.db.api.sql.fetch;

public enum FetchIdentifier {
    /**
     * 
     */
    NONE,

    /**
     * 
     */
	CHANNEL_DATA_PRE_FETCH,

    /**
     * 
     */
    CHANNEL_SUMMARY_FETCH,

    /**
     * 
     */
    CHANNEL_VALUE_FETCH,

    /**
     * 
     */
    COMMAND_FETCH,

    /**
     * 
     */
    END_SESSION_FETCH,

    /**
     * 
     */
    EVR_FETCH,

    /**
     * 
     */
    FRAME_FETCH,

    /**
     * 
     */
    HOST_FETCH,

    /**
     * 
     */
    LOG_FETCH,

    /**
     * 
     */
    PACKET_FETCH,

    /**
     * 
     */
    PRODUCT_FETCH,

    /**
     * 
     */
    SESSION_FETCH,

    /**
     *
     */
    CONTEXT_CONFIG_FETCH,

    /**
     * 
     */
    SESSION_PRE_FETCH,

    /**
     *
     */
    CONTEXT_CONFIG_PRE_FETCH,

    /**
     * 
     */
    ECDR_FETCH,
    
    /**
     * 
     */
    CFDP_INDICATION_FETCH,
    
    /**
     * 
     */
    CFDP_FILE_GENERATION_FETCH,
    
    /**
     * 
     */
    CFDP_FILE_UPLINK_FINISHED_FETCH,
    
    /**
     * 
     */
    CFDP_REQUEST_RECEIVED_FETCH,
    
    /**
     * 
     */
    CFDP_REQUEST_RESULT_FETCH,
    
    /**
     * 
     */
    CFDP_PDU_RECEIVED_FETCH,
    
    /**
     * 
     */
    CFDP_PDU_SENT_FETCH, 
    
    /**
     * 
     */
    CHANNEL_AGGREGATE_FETCH;
}
