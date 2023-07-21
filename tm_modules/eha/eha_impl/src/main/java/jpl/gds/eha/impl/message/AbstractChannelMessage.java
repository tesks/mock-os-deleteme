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
package jpl.gds.eha.impl.message;

import com.google.protobuf.InvalidProtocolBufferException;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.message.api.IStreamMessage;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.Message;
import org.xml.sax.SAXException;

import java.util.Map;

/**
 * AbstractChannelMessage is the base class for channel messages of various
 * types.
 *
 */
public abstract class AbstractChannelMessage extends Message implements IStreamMessage
{

    /**
     * Stream Id.
     */
    protected String streamId;

    protected MissionProperties missionProperties;

    /**
     * Constructor.
     *
     * @param type              the type of the message
     * @param time              event time for the message, milliseconds
     * @param missionProperties mission properties
     */
    public AbstractChannelMessage(final IMessageType type, final long time, final MissionProperties missionProperties) {
        super(type, time);
        this.missionProperties = missionProperties;
    }

    /**
     * Constructor.
     *
     * @param type
     *            the type of message
     * @param msg
     *            A protobuf message containing the values of an
     *            AbstractChannelMessage
     * @throws InvalidProtocolBufferException
     *             an error was encountered while reading values from the
     *             protobuf message
     */
    public AbstractChannelMessage(final IMessageType type, final Proto3AbstractMessage msg)
            throws InvalidProtocolBufferException {
    	super(type, msg);
    }

    /**
     * Constructor.
     *
     * @param type              the type of message
     * @param msg               A protobuf message containing the values of an AbstractChannelMessage
     * @param missionProperties mission properties
     * @throws InvalidProtocolBufferException an error was encountered while reading values from the protobuf message
     */
    public AbstractChannelMessage(final IMessageType type, final Proto3AbstractMessage msg,
                                  final MissionProperties missionProperties)
            throws InvalidProtocolBufferException {
        super(type, msg);
        this.missionProperties = missionProperties;
    }

    /**
     * Gets the streamId, which is used to associated channel messages
     * together.
     * @return Returns the streamId.
     */
    @Override
    public String getStreamId() {
        return this.streamId;
    }

    /**
     * Sets the streamId, which is used to associated channel messages
     * together.
     * @param streamId The streamId to set.
     */
    @Override
    public void setStreamId(final String streamId) {
        this.streamId = streamId;
    }

    /**
     * Adds key/value pairs representing object member (metadata) values to a HashMap
     * used to format outgoing messages using Velocity.
     * @param map the HashMap to add metadata to
     */
    @Override
	public synchronized void setTemplateContext(final Map<String,Object> map) {
        super.setTemplateContext(map);

        if (this.streamId != null) {
            map.put("streamId", this.streamId);
        } else {
            map.put("streamId","");
        }

    }

    /**
     * Parses and sets a field value from the XML element value with the given name.
     * @param elementName the name of the XML element in the XML message
     * @param text the value of the element
     * @throws SAXException thrown if a parse exception is encountered.
     */
    protected void parseFromElement(final String elementName, final String text) throws SAXException {

        if (elementName.equalsIgnoreCase("streamId")) {
            setStreamId(text);
        }

    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((streamId == null) ? 0 : streamId.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final AbstractChannelMessage other = (AbstractChannelMessage) obj;
		if (streamId == null) {
			if (other.streamId != null)
				return false;
		} else if (!streamId.equals(other.streamId))
			return false;
		return true;
	}
}
