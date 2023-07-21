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
package jpl.gds.jms.util;

import jpl.gds.message.api.external.MessageServiceException;

/**
 * A factory that creates MessageServiceExceptions from JMS-related exceptions.
 * Exists so that the specific JMS exception representing a message service 
 * disconnect can be detected.
 */
public class JmsMessageServiceExceptionFactory {

    /**
     * Creates a MessageServiceException.
     * 
     * @param message detailed error message
     * @param e the exception to be wrapped
     * @return new exception instance
     */
    public static MessageServiceException createException(String message, Exception e) {
        MessageServiceException mse = new MessageServiceException(message, e);
        if (e instanceof javax.jms.IllegalStateException) {
            mse.setDisconnect(true);
        }
        return mse;
    }
    
    /**
     * Creates a MessageServiceException.
     * 
     * @param e the exception to be wrapped
     * @return new exception instance
     */
    public static MessageServiceException createException(Exception e) {
        return createException(null, e);
    }
}
