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

import jpl.gds.shared.message.IMessageType;


/**
 * An interface to be implemented by external message utilities, which are used
 * to parse vendor-specific messages using a generic interface and to get header
 * properties from messages.
 */
public interface IExternalMessageUtility {

    /**
     * Gets the internal message type corresponding to the given external 
     * message by accessing the message type property in the message header.
     * 
     * @param msg the external message to check for type
     * @return the internal message type; if null, then no type property existed
     *         in the message
     * @throws MessageServiceException if any error encountered;the actual error
     *         will be attached as root cause
     */
    public IMessageType getInternalType(IExternalMessage msg) throws MessageServiceException;

    /**
     * Instantiates a list of internal message objects from an external message object, which
     * is assumed to contain internal messages with a single message type.
     * 
     * @param msg the external message
     * @return array of internal messages
     * 
     * @throws MessageServiceException if there is an error of some type; the actual error
     *         will be attached as root cause
     * 
     */
    public jpl.gds.shared.message.IMessage[] instantiateMessages(IExternalMessage msg) throws MessageServiceException;

    /**
     * Returns text detailing the header information in the given external message;
     * the amount of text is dictated by the header mode.
     * @param message the external message
     * @param headersMode indicates whether returned information should include
     *        the AMPCS message header information, as opposed to just the vendor
     *        message timestamp and ID: HEADERS_OFF or HEADERS_ON
     * @return a text String suitable for display
     */
    public String getHeaderText(IExternalMessage message, MessageHeaderMode headersMode);
    
    /**
     * Gets an AMPCS header property of type String from an external message.
     * 
     * @param message the IExternalMessage to get the property from
     * @param propName the name of the header property
     * @return property value, or null if not found
     * @throws MessageServiceException if there is an error accessing the message header
     */
    public String getStringHeaderProperty(IExternalMessage message, String propName) throws MessageServiceException;
    
    /**
     * Gets an AMPCS header property of type Integer from an external message.
     * 
     * @param message the IExternalMessage to get the property from
     * @param propName the name of the header property
     * @return property value, or null if not found
     * @throws MessageServiceException if there is an error accessing the message header
     */
  
    public Integer getIntHeaderProperty(IExternalMessage message, String propName) throws MessageServiceException;
    
    /**
     * Gets an AMPCS header property of type Long from an external message.
     * 
     * @param message the IExternalMessage to get the property from
     * @param propName the name of the header property
     * @return property value, or null if not found
     * @throws MessageServiceException if there is an error accessing the message header
     */
    public Long getLongHeaderProperty(IExternalMessage message, String propName) throws MessageServiceException;
    
    /**
     * Gets an AMPCS header property of type Float from an external message.
     * 
     * @param message the IExternalMessage to get the property from
     * @param propName the name of the header property
     * @return property value, or null if not found
     * @throws MessageServiceException if there is an error accessing the message header
     */
    public Float getFloatHeaderProperty(IExternalMessage message, String propName) throws MessageServiceException;
    
    /**
     * Gets an AMPCS header property of type Double from an external message.
     * 
     * @param message the IExternalMessage to get the property from
     * @param propName the name of the header property
     * @return property value, or null if not found
     * @throws MessageServiceException if there is an error accessing the message header
     */
    public Double getDoubleHeaderProperty(IExternalMessage message, String propName) throws MessageServiceException;
    
    /**
     * Gets the message ID. This is a vendor-specific value.
     * 
     * @param message the IExternalMessage to get the ID for
     * @return message ID string
     * @throws MessageServiceException if there is an error accessing the message header
     */
    public String getMessageId(IExternalMessage message) throws MessageServiceException;
    
    /**
     * Gets the message publication timestamp. 
     * 
     * @param message the IExternalMessage to get the time for
     * @return time in milliseconds since Jan 1, 1970
     * @throws MessageServiceException if there is an error accessing the message header
     */
    public long getMessageTimestamp(IExternalMessage message) throws MessageServiceException;
    
    /**
     * Gets a text version of the message body from an external message. If the message
     * contains binary content, this will be a hex dump.
     * 
     * @param message  the IExternalMessage to get content from
     * @return message body content as text
     * @throws MessageServiceException if there is an error accessing the message content
     */
    public String getContentDump(IExternalMessage message) throws MessageServiceException;
    
    /**
     * Gets the message service topic on which the message was published. It is possible that
     * this may return "unknown" if the message service vendor does not attach topic to messages.
     * 
     * @param message the IExternalMessage to get topic from
     * @return message topic; may be "unknown"
     */
    public String getTopic(IExternalMessage message);
}