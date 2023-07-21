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

import java.io.Serializable;

/**
 * An interface used to pass message service messages around, without any knowledge
 * of which message service is actually in use.  Really just a wrapper around a
 * native vendor or protocol-specific message objects.
 *
 * Added serializable. Please ensure all implementing classes can be properly serialized,
 * as external messages may be sent to the spill processor.
 */
public interface IExternalMessage extends Serializable {
    
    /**
     * Gets the native message object.
     * 
     * @return message object specific to message vendor in use
     */
    public Object getMessageObject();
    
    /**
     * Indicates if the content of the message is binary or text.
     * @return true if binary, false if text
     */
    public boolean isBinary();
    
    /**
     * Gets the binary body of the message as a byte array.  If the message
     * is not binary, returns null;
     * 
     * @return byte array or null if message is not binary
     * @throws MessageServiceException if there is a problem extracting message content
     */
    public byte[] getBinaryContent() throws MessageServiceException;
      
    /**
     * Gets the body of the message as a String.  If the message
     * is not textual, returns null;
     * 
     * @return text context as String, or null if message is not text
     * @throws MessageServiceException if there is a problem extracting message content
     */
    public String getTextContent() throws MessageServiceException;

}
