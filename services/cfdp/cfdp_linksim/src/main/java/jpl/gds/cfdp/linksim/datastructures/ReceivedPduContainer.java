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
package jpl.gds.cfdp.linksim.datastructures;

import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;

/**
 * {@code ReceivedPduContainer} is a simple container data structure to hold an incoming PDU's byte array data and,
 * if any, the {@code ICfdpPduMessage} object that the PDU original came in. This allows for preserving the message's
 * metadata for republishing.
 *
 */
public class ReceivedPduContainer {
    
    private byte[] data;
    private ICfdpPduMessage originalPduMessage;

    public ReceivedPduContainer(byte[] data, ICfdpPduMessage originalPduMessage) {
        this.data = data;
        this.originalPduMessage = originalPduMessage;
    }

    public byte[] getData() {
        return data;
    }

    public ICfdpPduMessage getOriginalPduMessage() {
        return originalPduMessage;
    }

}
