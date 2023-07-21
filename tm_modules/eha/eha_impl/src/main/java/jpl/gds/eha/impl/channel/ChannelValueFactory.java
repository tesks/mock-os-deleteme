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
package jpl.gds.eha.impl.channel;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.serialization.Proto3ChannelValue;

/**
 * ChannelValueFactory is used to create IInternalChannelValue objects.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * An IInternalChannelValue object is the multi-mission representation of a channel
 * sample. Channel derivation algorithms and EHA Adapter implementations must
 * create IInternalChannelValue objects via this factory. An IInternalChannelValue object must
 * have an associated IChannelDefinition, which will be automatically created or
 * fetched from the currently loaded Channel Definitions and assigned to the
 * IInternalChannelValue objects created by the factory.
 * <p>
 * This class contains only static methods. Once the IInternalChannelValue object is
 * returned by this factory, its additional members can be set through the
 * methods in the IInternalChannelValue interface.
 * 
 *
 * @see IServiceChannelValue
 * @see IChannelDefinition
 */
public class ChannelValueFactory implements IChannelValueFactory {
    
    @Override
    public IClientChannelValue createClientChannelValue(final String channelId, final ChannelType type) {
        return new ClientChannelValue(channelId, type);  
    }
    
    @Override
    public IClientChannelValue createClientChannelValue(final IChannelDefinition def) {
        if (def == null) {
            throw new IllegalArgumentException("Channel definition is null");
        }
        return new ClientChannelValue(def);
        
    }
    
    @Override
    public IClientChannelValue createClientChannelValue(final IChannelDefinition def, final Object o) {
        if (def == null) {
            throw new IllegalArgumentException("Channel definition is null");
        }
        
        return  new ClientChannelValue(def, o);
    }
    
    @Override
    public IClientChannelValue createClientChannelValue(final Proto3ChannelValue msg) {
        return new ClientChannelValue(msg);
    }
    
    @Override
    public IServiceChannelValue createServiceChannelValue(final IChannelDefinition def)  {
        if (def == null) {
            throw new IllegalArgumentException("Channel definition is null");
        }
        
        IServiceChannelValue val = null;
        
        switch (def.getChannelType()) {
        case SIGNED_INT:
        case STATUS:
            val = new IntegerChannelValue();
            break;
        case UNSIGNED_INT:
        case DIGITAL:
        case TIME:
            val = new UnsignedChannelValue();
            break;
        case FLOAT:
            val = new FloatChannelValue();
            break;
        case BOOLEAN:
            val = new BooleanChannelValue();
            break;
        case ASCII:
            val = new ASCIIChannelValue();
            break;
        default:
            throw new IllegalArgumentException("Invalid channel type: " + def.getChannelType());
        }
        val.setChannelDefinition(def);
        return val;
    }
    
    @Override
    public IServiceChannelValue createServiceChannelValue(final IChannelDefinition def, final Number n) {
        final IServiceChannelValue val = createServiceChannelValue(def);
        val.setDn(n);
        return val;
    }
    
    @Override
    public IServiceChannelValue createServiceChannelValue(final IChannelDefinition def, final String s) {
        final IServiceChannelValue val = createServiceChannelValue(def);
        val.setDn(s);
        return val;
        
    }
    
    @Override
    public IServiceChannelValue createServiceChannelValue(final IChannelDefinition def, final boolean b) {
        final IServiceChannelValue val = createServiceChannelValue(def);
        val.setDn(b);
        return val;
    }
    

}
