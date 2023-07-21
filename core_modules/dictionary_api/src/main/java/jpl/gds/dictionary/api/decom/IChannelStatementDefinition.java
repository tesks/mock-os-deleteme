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
package jpl.gds.dictionary.api.decom;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.decom.types.IDecomDataDefinition;

/**
 * The IChannelStatementDefinition interface is to be implemented by all channel
 * statement definition objects found in IDecomMapDefinitions.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IChannelStatementDefinition object is the multi-mission representation of
 * an channel statement specification in a decommutation map. A channel
 * statement specifically directs the decom processor to extract a channel
 * sample. IChannelDecomDictionary implementations must parse mission-specific
 * channel decom dictionary files and create IDecomMapDefinitions with attached
 * IChannelStatementsDefinition objects for the channel extraction statements
 * found therein. In order to isolate the mission adaptation from changes in the
 * multi-mission core, IChannelDecomDictionary implementations define a
 * mission-specific class that implements this interface. All interaction with
 * these objects in mission adaptations should use the
 * IChannelStatementDefinition interface, rather than directly interacting with
 * the objects themselves.
 * 
 *
 *
 */
public interface IChannelStatementDefinition extends IDecomDataDefinition {

    /**
     * Indicates whether there is a bit width associated with this channel statement.
     * 
     * @return true if the statement specifies a width, false if not
     */
    public boolean widthSpecified();

    /**
     * Indicates whether there is a bit offset associated with this channel statement.
     * 
     * @return true if the statement specifies an offset, false if not
     */
    public boolean offsetSpecified();

    /**
     * Gets the channel ID for this channel statement, indicating which
     * channel is to be extracted.
     * 
     * @return the channelId; never null
     */
    public String getChannelId();

    /**
     * Gets the bit width specified with this channel statement, if any.
     * This is the number of bits that will be extracted.
     * 
     * @return the width of the channel in bits; 0 if not specified
     * 
     * @see #widthSpecified()
     */
    public int getWidth();

    /**
     * Sets the bit width specified with this channel statement, if any.
     * This is the number of bits that will be extracted.
     * 
     * @param w the width of the channel in bits; 0 if not specified
     * 
     * @see #widthSpecified()
     */
    public void setWidth(int w);

    /**
     * Gets the bit offset specified with this channel statement, if any.
     * 
     * @return the offset of the channel in bits; 0 if not specified
     * 
     * @see #offsetSpecified()
     */
    public int getOffset();

    /**
     * Sets the bit offset specified with this channel statement, if any.
     * 
     * @param o the offset of the channel in bits; 0 if not specified
     * 
     * @see #offsetSpecified()
     */
    public void setOffset(int o);

    /**
     * Gets the channel definition for the channel associated with this statement.
     * 
     * @return channel definition object; never null
     */
    public IChannelDefinition getChannelDefinition();

    /**
     * Sets the channel definition for the channel associated with this statement.
     * 
     * @param cd IChannelDefinition object to set
     */
    public void setChannelDefinition(IChannelDefinition cd);

    /**
     * Gets the data type of the channel associated with this statement.
     * 
     * @return the channel type; never null
     */
    public ChannelType getChannelType();

}