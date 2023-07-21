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
package jpl.gds.eha.api.channel;

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.serialization.Proto3ChannelValue;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

/**
 * An interface to be implemented by read-write channel value objects. These
 * objects are generally used only by the services internal to telemetry
 * processing.
 * 
 * @since R8
 */
public interface IServiceChannelValue extends IClientChannelValue { 

    /**
     * Sets the record creation time (RCT) of this channel value.
     * @param postTime the record creation time to set
     */
    
    public abstract void setRct(final IAccurateDateTime postTime);

    /**
     * Sets the DSS (receiving station) ID for this channel value.
     * @param dss the DSS ID; 0 for no station
     */
    
    public abstract void setDssId(final int dss);

    /**
     * Sets the VCID (virtual channel id) for this channel value.
     * @param vcid null for unknown
     */
    
    public abstract void setVcid(final Integer vcid);

    /**
     * Sets the spacecraft event time (SCET) for this channel value.
     * @param scet the scet to set
     */
    
    public abstract void setScet(final IAccurateDateTime scet);

    /**
     * Sets the local solar time (LST) for this channel value
     * @param sol the lst to set
     */
    
    public abstract void setLst(final ILocalSolarTime sol);

    /**
     * Sets the engineering units value for this channel value. BEWARE: Will automatically
     * set the hasEu flag in the associated channel definition.
     * 
     * @param euVal the EU value
     */
    
    public abstract void setEu(final double euVal);

    /**
     * Sets the spacecraft clock (SCLK) associated with this channel value.
     *
     * @param sclk The sclk to set.
     */
    
    public abstract void setSclk(final ISclk sclk);

    /**
     * Sets the earth receive time (ERT) time associated with this channel value.
     *
     * @param ert The ert to set.
     */
    
    public abstract void setErt(final IAccurateDateTime ert);


    /**
     * Sets the raw data number object.
     * 
     * @param dn the DN to set.
     */
    
    public abstract void setDn(final Object dn);

    /**
     * Sets the channel definition for this channel value.
     *
     * @param chanDef The definition to set
     * 
     */
    
    public abstract void setChannelDefinition(final IChannelDefinition chanDef);
    
    /**
     * Sets the DN of this channel value from the given GDR ordered byte array.
     * The channel definition must be set for this to work.
     * 
     * @param stuff array of bytes containing the DN
     * @param len length of the DN in bytes
     */
    public abstract void setDnFromBytes(final byte[] stuff, final int len);

    /**
     * Sets the alarm states on this channel value.
     *
     * @param alarms The alarms to set.
     */
    
    public abstract void setAlarms(final IAlarmValueSet alarms);

    /**
     * Sets the DN value from an input String.
     * 
     * @param value the string representation of the value
     * @param componentLength the length of the value
     */   
    public abstract void setDnFromString(String value, int componentLength);

    /**
     * Sets the realtime flag indicating this is a realtime versus recorded value.
     * 
     * @param realtime true to set the value as realtime, false for recorded
     */   
    public abstract void setRealtime(final boolean realtime);

//    /**
//     * Sets the channel definition object for this channel value.
//     * 
//     * @param def IChannelDefinition to set
//     */  
//    public abstract void setDefinition(final IChannelDefinition def);


    /**
     * Gets the packet id for this channel value.
     *
     * @return the packet id; should not be null
     */  
    public abstract PacketIdHolder getPacketId();


    /**
     * Sets the packet id for this channel value.
     *
     * @param packetId Packet id, should not be null
     */  
    public abstract void setPacketId(final PacketIdHolder packetId);

    /**
     * Gets the frame id for this channel value.
     *
     * @return the frame id; null for unknown
     */
    public abstract Long getFrameId();
    
    /**
     * Sets the frame id for this channel value.
     *
     * @param frameId Frame id, null for unknown
     */
    
    public abstract void setFrameId(final Long frameId);

    /**
     * Sets the channel value category for this channel value.
     *
     * @param cce Channel value category
     */
    public abstract void setChannelCategory(final ChannelCategoryEnum cce);
    
    /**
     * Copies the current IChannelValue to a new instance.
     * 
     * @param factory
     *            the channel value factory to use to create the copy
     * 
     * @return new IChannelValue that is a copy of this one
     */
    public abstract IServiceChannelValue copy(IChannelValueFactory factory);
    
    /**
     * Gets the channel value category for this channel value.
     *
     * @return the channel value category
     */
    public abstract ChannelCategoryEnum getChannelCategory();
    
    /**
     *  Returns the channel definition for this channel value.
     *  
     * @return the channel definition
     */
    public abstract IChannelDefinition getChannelDefinition();
    
    /**
     * Populate this ChannelValue with the values represented in the provided
     * protobuf message
     * 
     * @param msg
     *            the protobuf message to be restored
     */
    public void load(Proto3ChannelValue msg);
}
