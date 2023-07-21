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
package jpl.gds.message.api;

/**
 * An interface that defines bean names for the Spring configuration in the
 * message projects.
 */
public interface MessageApiBeans {
    /**
     * Bean name for the Message Service configuration object.
     */
    String MESSAGE_SERVICE_CONFIG = "MESSAGE_SERVICE_CONFIG";
    
    /**
     * Bean name for the Message Service Portal object.
     */
    String MESSAGE_SERVICE_PORTAL = "MESSAGE_SERVICE_PORTAL";
    
    /**
     * Bean name for the Message Service Client factory object.
     */
    String MESSAGE_CLIENT_FACTORY = "MESSAGE_CLIENT_FACTORY";
    
    /**
     * Bean name for JndiProperties object.
     */
    String JNDI_PROPERTIES = "JNDI_PROPERTIES";
    
    /**
     * Bean name for the IStatusMessageFactory object.
     */
    String STATUS_MESSAGE_FACTORY = "STATUS_MESSAGE_FACTORY";
    
    /**
     * Bean name for the IExternalMessageUtility object.
     */
    String EXTERNAL_MESSAGE_UTIL = "EXTERNAL_MESSAGE_UTIL";
    
    /**
     * Bean name for the IClientHeartbeatPublisher object.
     */
    String CLIENT_HEARTBEAT_PUBLISHER = "CLIENT_HEARTBEAT_PUBLISHER";
    
    /**
     * Bean name for IQueuingMessageHandler object.
     */ 
    String QUEUING_MESSAGE_HANDLER = " QUEUING_MESSAGE_HANDLER";
    
    /** 
     * Name for the message capture handler bean 
     */
    String MESSAGE_CAPTURE_HANDLER = "MESSAGE_CAPTURE_HANDLER";

    /**
     * Name for the spill processor bean
     */
    String SPILL_PROCESSOR = "SPILL_PROCESSOR";

    /**
     * Name for the spill processor bean
     */
    String INTERNAL_BUS_PUBLISHER = "INTERNAL_BUS_PUBLISHER";

}
