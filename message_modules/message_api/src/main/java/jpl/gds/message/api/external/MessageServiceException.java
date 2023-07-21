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
package jpl.gds.message.api.external;

/**
 * A general message type for message-service-related exceptions. Used to
 * wrap exceptions coming from a specific type of message service, such as
 * JMSException, so that message service users do not have to know the type
 * of the underlying message service.
 */
public class MessageServiceException extends Exception {
    
    private boolean isDisconnect;

    /**
     * Serial version ID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param message detailed message text
     */
    public MessageServiceException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param cause Throwable that triggered this exception
     */
    public MessageServiceException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     * 
     * @param message detailed message text
     * @param cause Throwable that triggered this exception
     */
    public MessageServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Sets the flag indicating whether this exception seems to represent
     * a disconnection from the message service.
     * 
     * @param isDisconnect true or false
     */
    public void setDisconnect(boolean isDisconnect) {
        this.isDisconnect = isDisconnect;
    }

    /**
     * Gets the flag indicating whether this exception seems to represent
     * a disconnection from the message service.
     * 
     * @return true or false
     */
    public boolean isDisconnect() {
        return this.isDisconnect;
    }
}
