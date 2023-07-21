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

import com.google.protobuf.GeneratedMessageV3;

import jpl.gds.shared.metadata.IMetadataHeaderProvider;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * Interface for internal messages.
 *
 *
 */
public interface IMessage extends Templatable, IMetadataHeaderProvider {

    /** Event time tag */
    public static final String EVENT_TIME_TAG = "eventTime";
    /** Event time tag */
    public static final String EVENT_TIME_EXACT_TAG = "eventTimeExact";
    /** From SSE tag */
    public static final String FROM_SSE_TAG = "fromSse";
   
    /**
     * Sets the context key associated with this message.
     * 
     * @param toSet key to assign
     */
    public void setContextKey(IContextKey toSet);
    
    /**
     * Gets the context key associated with this message.
     * 
     * @return context key; may be null
     */
    public IContextKey getContextKey();
    
    /**
     * Gets the message event time (wall clock time).
     * 
     * @return the eventTime.
     */
    public abstract IAccurateDateTime getEventTime();

    /**
     * Sets the message event time (wall clock time). Usually
     * the event time is set when the message object is created.
     * It should be overwritten only for a specific reason.
     * 
     * @param paramEventTime
     *            The eventTime to set.
     */
    public abstract void setEventTime(final IAccurateDateTime paramEventTime);

    /**
     * Gets the project standard string representation of the event time.
     * 
     * @return the formatted time String
     */
    public abstract String getEventTimeString();
    
    /**
     * Gets the message event time as raw milliseconds.
     * 
     * @return time as milliseconds
     */
    public abstract long getRawEventTime();

    /**
     * Sets the flag indicating whether or not this message was generated in
     * response to an SSE/GSE event or a FSW event.
     * 
     * @param paramFromSse
     *            true if message is for SSE/GSE, false if not
     */
    public abstract void setFromSse(final boolean paramFromSse);

    /**
     * Gets the flag indicating whether or not this message was generated in
     * response to an SSE/GSE event or a FSW event.
     * 
     * @return true if message is for SSE/GSE, false if not
     */
    public abstract boolean isFromSse();

    /**
     * Returns an XML representation of the message suitable for external
     * publication to an outside source (e.g. message service). It is not mandatory that
     * XML serialization be supplied for all messages.
     * 
     * @return The XML string representation of this message; messages that do
     *         not have an XML serialization may return an empty string, a
     *         summy string, or throw UnsupportedOperationException.
     */
    public abstract String toXml();

    /**
     * Returns a protobuf binary representation of this message suitable for external
     * publication to an outside source (e.g., message service). It is not mandatory
     * that binary serialization be implemented for all messages.
     * 
     * @return a protobuf byte array representing the content of the message
     *         object. Messages that do not have a binary serialization should
     *         throw UnsupportedOperationException.
     */
    public abstract byte[] toBinary();

    /**
     * Returns a string suitable for putting in a debug log file. Including this
     * in this interface mandates that the default Object.toString() be
     * overridden.
     * <p>
     * Something like this: <blockquote> <code>
     * return "SomeMessage[foo=" + getFoo() + ", bar=" + getBar() + "]";
     * </code> </blockquote>
     * 
     * @return debug suitable string; may not be null
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

    /**
     * Returns a one line summary suitable for display in a message table.
     * Assume the message type and event time do not need to be included. Do not
     * use the value of toString() as the return value; toString() is suitable
     * for debugging. This is for display in a user interface. If the message is
     * never externally displayed, the return value does not matter, other than
     * it should not be null.
     * 
     * @return a one line display string, or the empty string
     */
    public abstract String getOneLineSummary();

    /**
     * Sets the message metadata context header.
     * 
     * @param header context header to set
     */
    public void setContextHeader(ISerializableMetadata header);
    
    /**
     * Indicates if this message matches the message type in the given
     * message configuration.
     * 
     * @param config IMessageConfiguration to compare to
     * @return true if matched, false if not
     */
    public boolean isType(IMessageType config);

    /**
     * Gets the IMessageType object for this message.
     * 
     * @return IMessageType
     */
    public IMessageType getType();
    
    /**
     * Get the IMessage as a protobuf message
     * 
     * @return this IMessage as a protobuf message
     */
    public GeneratedMessageV3 build();


    /**
     * Check to see if this message is allowed to be published over an external bus (eg, JMS).
     * Defaults to true.
     *
     * @return boolean message is externally publishable
     */
    public boolean isExternallyPublishable();

    /**
     * Change if the message is allowed to be published over an external bus (eg, JMS)
     *
     * @param value true if the message is allowed on an external bus, false if not.
     */
    public void setIsExternallyPublishable(boolean value);
}
