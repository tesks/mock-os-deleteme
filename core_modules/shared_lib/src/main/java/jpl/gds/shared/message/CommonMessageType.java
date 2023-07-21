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
package jpl.gds.shared.message;

/**
 * An enumeration defining cross-package message types. Message types used only within one sub-project
 * should not be in here.
 * 
 *
 * @since R8
 */
public enum CommonMessageType implements IMessageType {
    /** Message client heartbeat type */
    ClientHeartbeat,
    /** End of data stream type */
    EndOfData,
    /** Log message type */
    Log,
    /** Start of data stream message type */
    StartOfData,
    /** Performance summary message type */
    PerformanceSummary;
    
    @Override
    public String getSubscriptionTag() {
        return name();
    }
   
       

}
