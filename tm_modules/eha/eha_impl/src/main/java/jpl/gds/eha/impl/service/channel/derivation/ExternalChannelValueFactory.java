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
package jpl.gds.eha.impl.service.channel.derivation;

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.channel.api.IChannelSampleFactory;
import jpl.gds.eha.channel.api.InvalidChannelValueException;
import jpl.gds.eha.impl.channel.ASCIIChannelValue;
import jpl.gds.eha.impl.channel.BooleanChannelValue;
import jpl.gds.eha.impl.channel.FloatChannelValue;
import jpl.gds.eha.impl.channel.IntegerChannelValue;
import jpl.gds.eha.impl.channel.UnsignedChannelValue;

/**
 * ExternalChannelValueFactory is used to create IInternalChannelValue objects
 * in external (customer) APIs.
 *
 * @see IServiceChannelValue
 * @see IChannelDefinition
 */
public class ExternalChannelValueFactory implements IChannelSampleFactory {
    
    private final IChannelDefinitionProvider defProvider;
    
    /**
     * Constructor.
     * 
     * @param prov
     *            the channel definition provider
     */
    public ExternalChannelValueFactory(final IChannelDefinitionProvider prov) {
        defProvider = prov;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.eha.channel.api.IChannelSampleFactory#create(java.lang.String, java.lang.Number)
     */
    @Override
    public IServiceChannelValue create(final String cid, final Number dn) throws IllegalStateException,
        InvalidChannelValueException {
        
        IServiceChannelValue val = null;

        final IChannelDefinition def = getDefinition(cid);
        
        if (def == null) {
            throw new IllegalStateException("Channel ID " + cid + " is not found in the channel dictionary");
        }
        
        switch (def.getChannelType()) {
        case SIGNED_INT:
        case STATUS:
            val = new IntegerChannelValue(dn);
            break;
        case UNSIGNED_INT:
        case DIGITAL:
        case TIME:
            val = new UnsignedChannelValue(dn);
            break;
        case FLOAT:
            val = new FloatChannelValue(dn);
            break;
        case BOOLEAN:
            val = new BooleanChannelValue(dn);
            break;
        default:
            throw new IllegalArgumentException("Unexpected channel data type " + def.getChannelType() + " for channel " + cid);
        }
        val.setChannelDefinition(def);
        return val;
    }

  
    /**
     * @{inheritDoc}
     * @see jpl.gds.eha.channel.api.IChannelSampleFactory#create(java.lang.String, java.lang.String)
     */
    @Override
    public IServiceChannelValue create(final String cid, final String value) {
        IServiceChannelValue val = null;

        final IChannelDefinition def = getDefinition(cid);

        switch (def.getChannelType()) {
        case ASCII:
            val = new ASCIIChannelValue(value);
            break;
        default:
            throw new IllegalArgumentException(
                    "Channel type for channel "+ cid + " is not string");
        }

        val.setChannelDefinition(def);

        return val;
    }

    
    /**
     * @{inheritDoc}
     * @see jpl.gds.eha.channel.api.IChannelSampleFactory#create(java.lang.String, boolean)
     */
    @Override
    public IServiceChannelValue create(final String cid, final boolean value) {
        IServiceChannelValue val = null;

        final IChannelDefinition def = getDefinition(cid);

        switch (def.getChannelType()) {
        case BOOLEAN:
            val = new BooleanChannelValue(value);
            break;
        default:
            throw new IllegalArgumentException(
                    "Channel type for channel "+ cid + " is not boolean");
        }

        val.setChannelDefinition(def);

        return val;
    }
    /**
     * Attempts to fetch the definition of the given channel ID from the channel
     * table. If it doesn't exist, this method creates a new definition.
     * CAUTION: IF no channel definition exists and one is created, the
     * definition created will assume the channel is a FSW Channel
     * 
     * @param cid
     *            channel ID
     * @return IChannelDefinition
     * 
     */
    private IChannelDefinition getDefinition(final String cid) {

        if (defProvider == null) {
            throw new IllegalStateException("Channel definition provider is not set");
        }
        return defProvider.getDefinitionFromChannelId(cid);
    }
}
