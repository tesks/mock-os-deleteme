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
package jpl.gds.message.api.app;

/**
 * A class containing constants that are shared by messaging applications
 * and other applications.
 */
public class MessageAppConstants {
    /**
     * This message in the log indicates the message router in the recorded
     * engineering product watcher is ready to receive messages.
     */
    public static final String MESSAGE_ROUTER_UP_MESSAGE = "MessageRouter is Up";
    
    /**
     * This message in the log indicates the message router in the recorded
     * engineering product watcher has shut down.
     */
    public static final String MESSAGE_ROUTER_DOWN_MESSAGE = "MessageRouter shutdown is complete";
    
    /**
     * This message in the log indicates the message router in the recorded
     * engineering product watcher is shutting down its message listeners.
     */
    public static final String  LISTENERS_DOWN_MESSAGE = "MessageRouter is shutting down message listener";
    
    /**
     * String format pattern for the product message handler's shutdown flag file.
     * The %s should be replaced with a unique ID.
     */
    public static final String PRODUCT_HANDLER_FLAG_PATTERN = "ProductMessageHandler.%s.flag";

}
