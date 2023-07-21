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

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IChannelValueMessage;
import jpl.gds.shared.message.MessageRegistry;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * InteranlChannelValueMessage is the most primitive message type for publishing
 * a channelized telemetry values.
 *
 */
public class InternalChannelValueMessage extends AbstractChannelMessage implements IChannelValueMessage
{

    private IServiceChannelValue channelVal;

    /**
     * Creates an instance of InternalChannelValueMessage with a current event time and the supplied channel value.
     *
     * @param val               channel value
     * @param missionProperties mission properties
     */
    public InternalChannelValueMessage(final IServiceChannelValue val, final MissionProperties missionProperties) {
        super(EhaMessageType.ChannelValue, System.currentTimeMillis(), missionProperties);
        setChannelValue(val);
    }

    /**
     * Retrieves the realtime flag, indicating whether this is a realtime or a
     * recorded channel
     * 
     * @return true if the channel value is realtime, false if recorded
     */
    @Override
    public boolean isRealtime() {
        return channelVal == null ? true : channelVal.isRealtime();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        if (channelVal == null) {
            return "Empty Channel Value";
        }
        return "ID=" + channelVal.getChanId() + " type= " +
        channelVal.getChannelType().toString() + " DN=" + channelVal.stringValue() +
        " realtime= " + isRealtime();
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(EhaMessageType.ChannelValue));
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return getOneLineSummary();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IServiceChannelValue getChannelValue() {
        return channelVal;
    }

    /**
     * Sets the channel value.
     *
     * @param channelVal The channelVal to set.
     */
    protected void setChannelValue(final IServiceChannelValue channelVal) {
        this.channelVal = channelVal;
    }
    
}
