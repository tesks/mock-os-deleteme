/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.cfdp.message.api;

import jpl.gds.shared.message.IMessageType;

/**
 * Class CfdpMessageType
 *
 *  @since R8
 */
public enum CfdpMessageType implements IMessageType {
	
    /** CFDP Indication message type */
    CfdpIndication,
    
    /** CFDP File Generation message type */
    CfdpFileGeneration,
    
    /** CFDP Uplink Finished message type */
    CfdpFileUplinkFinished,
    
    /** CFDP Request Received message type */
    CfdpRequestReceived,

    /** CFDP Request Result message type */
    CfdpRequestResult,

    /** CFDP PDU Received message type */
    CfdpPduReceived,
    
    /** CFDP PDU Sent message type */
    CfdpPduSent;

    @Override
    public String getSubscriptionTag() {
        return name();
    }
    
}
