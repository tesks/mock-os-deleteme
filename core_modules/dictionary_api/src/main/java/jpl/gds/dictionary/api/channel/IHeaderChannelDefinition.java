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
package jpl.gds.dictionary.api.channel;


/**
 * The IHeaderChannelDefinition interface is to be implemented by all channel
 * definition classes that specifically apply to telemetry header channels.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * IHeaderChannelDefinition defines methods needed to interact with Header
 * Channel Definition objects as required by the IChannelDictionary interface.
 * Header Channel Definitions extend standard Channel Definitions, as this
 * interface extends IChannelDefinition. This interface is primarily used by
 * channel file parser implementations in conjunction with the
 * ChannelDefinitionFactory, which is used to create actual Header Channel
 * Definition objects in the parsers. IChannelDictionary objects should interact
 * with all Channel Definition objects only through the Factory and the
 * IChannelDefinition interfaces. Interaction with the actual Channel Definition
 * implementation classes in an IChannelDictionary implementation is contrary to
 * multi-mission development standards.
 * <p>
 * The special capability added to the basic IChannelDefinition interface
 * by this interface is the mapping of the header channel to a corresponding
 * telemetry field in a packet, frame, or CHDO SFDU header. It is possible
 * to map a single channel to more than one of these, specifically, to map
 * it to a packet source and to an SFDU source, or to a frame source and
 * an SFDU source. Mapping a channel to all three, however, may have 
 * unpredictable results.
 * 
 *
 *
 * @see IChannelDictionary
 * @see ChannelDefinitionFactory
 */
public interface IHeaderChannelDefinition extends IChannelDefinition {
    /**
     * Sets the packet header field mapped to this channel.
     * 
     * @param fieldName PacketHeaderFieldName this channel maps to
     */
    public void setPacketHeaderField(PacketHeaderFieldName fieldName);

    /**
     * Sets the frame header field mapped to this channel.
     * 
     * @param fieldName FrameHeaderFieldName this channel maps to
     */
    public void setFrameHeaderField(FrameHeaderFieldName fieldName);

    /**
     * Sets the CHDO SFDU header field mapped to this channel. The
     * field name must map to a corresponding CHDO field defined
     * in the CHDO dictionary, or nothing will be produced
     * for this channel at runtime.
     * 
     * @param fieldName CHDO SFDU field this channel maps to
     */
    public void setSfduHeaderField(String fieldName);
    
    /**
     * Gets the packet header field mapped to this channel.
     * 
     * @return PacketHeaderFieldName this channel maps to, or null
     * if there is no packet field mapping for this channel
     */
    public PacketHeaderFieldName getPacketHeaderField();

    /**
     * Gets the frame header field mapped to this channel.
     * 
     * @return FrameHeaderFieldName this channel maps to, or null
     * if there is no frame field mapping for this channel
     */
    public FrameHeaderFieldName getFrameHeaderField();

    /**
     * Gets the CHDO SFDU header field mapped to this channel.
     * 
     * @return CHDO SFDU field this channel maps to, or null
     * if there is no SFDU field mapping for this channel
     */
    public String getSfduHeaderField();
    
}
