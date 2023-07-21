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
package jpl.gds.dictionary.impl.channel;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.FrameHeaderFieldName;
import jpl.gds.dictionary.api.channel.HeaderChannelSourceType;
import jpl.gds.dictionary.api.channel.IHeaderChannelDefinition;
import jpl.gds.dictionary.api.channel.PacketHeaderFieldName;



/**
 * Implements a channel definition specifically for header channels. Basically,
 * adds the ability to get and set header field mappings to the base channel
 * definition. These are actually stored as key-value attributes in the 
 * superclass.
 * 
 *
 *
 */
public class HeaderChannelDefinition extends ChannelDefinition implements IHeaderChannelDefinition {

    /**
     * Creates an instance of a typed HeaderChannelDefinition with the given
     * ID and data type.
     * @param ct the ChannelType enumeration value
     * @param cid the ID of the new channel
     */
    HeaderChannelDefinition(ChannelType ct, String cid) {
        super(ct, cid);
    }

    /**
     * Creates an instance of an untyped HeaderChannelDefinition with the given ID.
     * 
     * @param cid the ID of the new channel
     * 
     */
    HeaderChannelDefinition(final String cid) {
        super(cid);
    }

    
    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IHeaderChannelDefinition#setPacketHeaderField(jpl.gds.dictionary.impl.impl.api.channel.PacketHeaderFieldName)
     */
    @Override
    public void setPacketHeaderField(PacketHeaderFieldName fieldName) {
        setKeyValueAttribute(HeaderChannelSourceType.PACKET_SOURCE.toString(), fieldName.toString());
        
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IHeaderChannelDefinition#setFrameHeaderField(jpl.gds.dictionary.impl.impl.api.channel.FrameHeaderFieldName)
     */
    @Override
    public void setFrameHeaderField(FrameHeaderFieldName fieldName) {
        setKeyValueAttribute(HeaderChannelSourceType.FRAME_SOURCE.toString(), fieldName.toString());
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IHeaderChannelDefinition#setSfduHeaderField(java.lang.String)
     */
    @Override
    public void setSfduHeaderField(String fieldName) {
        setKeyValueAttribute(HeaderChannelSourceType.SFDU_SOURCE.toString(), fieldName);
        
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IHeaderChannelDefinition#getPacketHeaderField()
     */
    @Override
    public PacketHeaderFieldName getPacketHeaderField() {
        String temp = getKeyValueAttribute(HeaderChannelSourceType.PACKET_SOURCE.toString());
        if (temp != null) {
            return Enum.valueOf(PacketHeaderFieldName.class, temp);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IHeaderChannelDefinition#getFrameHeaderField()
     */
    @Override
    public FrameHeaderFieldName getFrameHeaderField() {
        String temp = getKeyValueAttribute(HeaderChannelSourceType.FRAME_SOURCE.toString());
        if (temp != null) {
            return Enum.valueOf(FrameHeaderFieldName.class, temp);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IHeaderChannelDefinition#getSfduHeaderField()
     */
    @Override
    public String getSfduHeaderField() {
        return getKeyValueAttribute(HeaderChannelSourceType.SFDU_SOURCE.toString());
    }

}
