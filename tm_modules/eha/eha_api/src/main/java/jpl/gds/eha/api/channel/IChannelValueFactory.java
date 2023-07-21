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

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.serialization.Proto3ChannelValue;

/**
 * An interface to be implemented by factories that create channel values.
 * 
 * @since R8
 */
public interface IChannelValueFactory {

    /**
     * Creates a client channel value.
     * 
     * @param channelId
     *            dictionary ID of the channel
     * @param type
     *            the data type of the channel
     * @return new channel value instance, which will have no value
     */
    public IClientChannelValue createClientChannelValue(String channelId, ChannelType type);
    
    /**
     * Creates a client channel value.
     * 
     * @param def
     *            the dictionary channel definition
     *
     * @return new channel value instance, which will have no value
     */
    public IClientChannelValue createClientChannelValue(IChannelDefinition def);
    
    /**
     * Creates a client channel value.
     * 
     * @param def
     *            the dictionary channel definition
     * @param o
     *            the channel value to assign
     *
     * @return new channel value instance, with value assigned
     */
    public IClientChannelValue createClientChannelValue(IChannelDefinition def, Object o);
    
    /**
     * Creates a client channel value
     * 
     * @param msg
     *            the protobuf message containing a channel value
     * @return new channel value instance, with value assigned
     */
    public IClientChannelValue createClientChannelValue(Proto3ChannelValue msg);
    
    /**
     * Creates a service channel value.
     * 
     * @param def
     *            the dictionary channel definition
     *
     * @return new channel value instance, with no value
     */
    public IServiceChannelValue createServiceChannelValue(final IChannelDefinition def);
    
    /**
     * Creates a service channel value with a numeric value.
     * 
     * @param def
     *            the dictionary channel definition
     * @param n
     *            the channel value to assign
     *
     * @return new channel value instance, with assigned value
     */
    public IServiceChannelValue createServiceChannelValue(final IChannelDefinition def, Number n);

    /**
     * Creates a service channel value with a string value.
     * 
     * @param def
     *            the dictionary channel definition
     * @param s
     *            the channel value to assign
     *
     * @return new channel value instance, with assigned value
     */
    public IServiceChannelValue createServiceChannelValue(final IChannelDefinition def, String s);
    
    /**
     * Creates a service channel value with a boolean value.
     * 
     * @param def
     *            the dictionary channel definition
     * @param b
     *            the channel value to assign
     *
     * @return new channel value instance, with assigned value
     */
    public IServiceChannelValue createServiceChannelValue(final IChannelDefinition def, boolean b);
}
