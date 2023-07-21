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
package jpl.gds.eha.impl.service.channel.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.eha.api.service.channel.IPrechannelizedAdapter;
import jpl.gds.eha.api.service.channel.PrechannelizedAdapterException;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * AbstractEhaAdaptor is used as the super-class for most EHA extractor
 * adaptations. The EHA extractor is responsible for extracting EHA values from
 * packets and properly interpreting them into Channel Value objects.
 * 
 */
public abstract class AbstractPrechannelizedAdapter implements IPrechannelizedAdapter
{
    /**
     * Length of the index value in a pre-channelized packets (in bytes)
     */
    public final static int INDEX_BYTE_LENGTH = 2;

    /**
     * Trace log object to share with subclasses.
     */
    protected final Tracer log;


    /**
     * spacecraft ID
     */
    protected int scid;
    /**
     * Indicates whether to set LST times.
     * 
     */
    protected boolean setSols;
    /**
     * Map of channel index to channel definition.
     */
    protected Map<Integer, IChannelDefinition> chanDefIndices = new HashMap<Integer, IChannelDefinition>();
    /**
     * Factory for creating channel values.
     */
    protected IChannelValueFactory chanFactory;

	protected boolean isStrict;
    
    /**
     *
     * Creates an instance of AbstractPrechannelizedEhaAdapter.
     * 
     * @param context
     *            the current application context
     */
    public AbstractPrechannelizedAdapter(final ApplicationContext context) {
        
        this(TraceManager.getTracer(context, Loggers.TLM_EHA), 
        		context.getBean(IContextIdentification.class).getSpacecraftId(),
        		context.getBean(EnableLstContextFlag.class).isLstEnabled(),
        		context.getBean(IChannelValueFactory.class),
        		context.getBean(SseContextFlag.class).isApplicationSse(),
        		context.getBean(EhaProperties.class).isStrictProcessing(),
        		context.getBean(IChannelDefinitionProvider.class));
    }
    
    /**
     * 
     * Creates an instance of AbstractPrechannelizedEhaAdapter.
     * 
     * @param logTracer
     *            Logger
     * @param scid
     *            Space craft ID
     * @param setSols
     *            Indicates whether to set LST Times
     * @param chanValFactory
     *            Factory for creating channel values
     * @param isSse
     *            Used to determine if this object gets Flight channels or SSE channels
     * @param chanDefProvider
     *            Used to get the channel definitions
     */
     AbstractPrechannelizedAdapter(Tracer logTracer, int scid, boolean setSols,
    		IChannelValueFactory chanValFactory, boolean isSse, boolean isStrict,
    		IChannelDefinitionProvider chanDefProvider) {
    	
    	this.log = logTracer;
    	this.scid = scid;
    	this.setSols =setSols;
    	this.chanFactory = chanValFactory;
        this.isStrict = isStrict;
    	
    	 /* Get SSE flag */
        for (final IChannelDefinition def: chanDefProvider.getChannelDefinitionMap().values()) {
            
            /* 
             * Use isDerived() rather than checking for undefined channel index.
             */
            if (!def.isDerived()) {
                /* 
                 * Check SSE flag. We only want flight channel definition if it
                 * is false, and SSE channels if it is true. 
                 */
                final boolean channelWanted = isSse ? 
                        def.getDefinitionType() == ChannelDefinitionType.SSE : def.getDefinitionType() == ChannelDefinitionType.FSW;
                if (channelWanted) {
                    chanDefIndices.put(def.getIndex(), def);
                }
            }
        }    	
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.eha.api.service.channel.IPrechannelizedAdapter#extractEha(jpl.gds.tm.service.api.packet.ITelemetryPacketMessage)
     */
    @Override
    public abstract List<IServiceChannelValue> extractEha(ITelemetryPacketMessage pm) throws PrechannelizedAdapterException;

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.eha.api.service.channel.IPrechannelizedAdapter#extractEha(byte[],
     *      int, int)
     */
    @Override
    public abstract List<IServiceChannelValue> extractEha(final byte[] data,
            final int offset, final int length) throws PrechannelizedAdapterException;

    /**
     * Given a channel definition, a byte array, and an offset into that byte
     * array, create a channel value object corresponding to the given channel
     * definition.
     * 
     * @param chanDef The dictionary definition of the channel whose value will
     *        be created
     * @param bytes The byte array to read channel data out of
     * @param offset The offset into the byte array where data should start
     *        being read
     * @param length Byte length value to override what's specified in chanDef.
     *        Currently only used for variable string channels. Should be set to
     *        0 if not being used.
     * 
     * @return An IInternalChannelValue object corresponding to a value for the channel
     *         specified by the input ChannelDefinition
     * 
     * @throws ArrayIndexOutOfBoundsException If the offset is bad or reading
     *         the channel value causes a read to go off the end of the input
     *         byte array
     */
    protected IServiceChannelValue createChannelValueFromBytes(
            final IChannelDefinition chanDef,
            final byte[]             bytes,
            final int                offset,
            final int                length)
                    throws ArrayIndexOutOfBoundsException
                    {
        if (chanDef == null) {
            throw new IllegalArgumentException("Null input channel definition");
        } else if(bytes == null)
        {
            throw new IllegalArgumentException("Null input byte array");
        }
        else if(offset < 0 || offset >= bytes.length)
        {
            throw new ArrayIndexOutOfBoundsException("The offset of " + offset + " falls outside the bounds of the input byte array (0 to " + (bytes.length-1) + ")");
        }

        int bitLength = chanDef.getSize();
        int byteLength = bitLength/8;

        if (chanDef.getChannelType().equals(ChannelType.ASCII) && length > 0) {
            // This is a variable-length ASCII channel. Use the override length value.
            byteLength = length;
            bitLength = length * 8;
        }

        if((offset + byteLength) > bytes.length)
        {
            throw new ArrayIndexOutOfBoundsException("For channel " + chanDef.getId() + ": The offset is " + offset + ", but the channel value byte length of " + byteLength +
                    " falls outside the bounds of the input byte array (0 to " + (bytes.length-1) + ")");
        }

        //read the raw channel value into its own byte array
        final byte[] byteValue = new byte[byteLength];
        System.arraycopy(bytes, offset, byteValue, 0, byteValue.length);

        //create the channel value object based on the defined type in the channel definition
        IServiceChannelValue chanval = null;
        /* Added TIME case below. */
        switch(chanDef.getChannelType())
        {
        case SIGNED_INT:
        case STATUS:
            chanval = chanFactory.createServiceChannelValue(chanDef,GDR.getSignedInteger(byteValue,0,bitLength));
            break;

        case UNSIGNED_INT:
        case DIGITAL:
        case BOOLEAN:
        case TIME:
            chanval = chanFactory.createServiceChannelValue(chanDef, GDR.getUnsignedInteger(byteValue,0,bitLength));
            break;

        case FLOAT:
            /* Removed check for DOUBLE TYPE above. */
            chanval = chanFactory.createServiceChannelValue(chanDef, GDR.getFloatingPoint(byteValue,0,bitLength));
            break;

        case ASCII:
            chanval = chanFactory.createServiceChannelValue(chanDef, GDR.stringValue(byteValue,0,byteLength));
            break;

        default:
            throw new RuntimeException("Unable to create channel value of type " + chanDef.getChannelType());           
        }

        /*
         * No longer compute EU here. EU computation is now done
         * by the EhaPublisherUtility.
         */

        return(chanval);
    }

    public void setStrict(boolean isStrict) {
        this.isStrict = isStrict;
    }

    public boolean isStrict() {
        return this.isStrict;
    }
}
