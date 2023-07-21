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
package jpl.gds.eha.impl.service.channel;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.shared.gdr.GDR;

/**
 * An extraction utility for DSN monitor channels.
 * 
 *
 */
public class DsnMonitorChannelExtractor {
	private final IChannelDefinitionProvider channelDict;
    private final IChannelValueFactory chanFactory;
	
	/**
	 * Constructor.
	 * 
	 * @param chanTable channel definition provider
	 * @param chanFactory channel value factory
	 */
	public DsnMonitorChannelExtractor(final IChannelDefinitionProvider chanTable, final IChannelValueFactory chanFactory) {
		super();
		channelDict = chanTable;
		this.chanFactory = chanFactory;
	}
	
	private IServiceChannelValue doChan(final String chanStr, final byte[] pkt,final int off, final int bitLen)
	{
		final IChannelDefinition chanDef = channelDict.getDefinitionFromChannelId(chanStr);
		final ChannelType ct = chanDef.getChannelType();

		switch(ct) {
		case SIGNED_INT: {
			int val = 0;
			if (bitLen == 8) {
				val = GDR.get_i8(pkt, off);
			} else if (bitLen == 16) {
				val = GDR.get_i16(pkt, off);
			} else if (bitLen == 24) {
				val = GDR.get_i24(pkt, off);
			} else if (bitLen == 32) {
				val = GDR.get_i32(pkt, off);
			}
			return(chanFactory.createServiceChannelValue(chanDef, val)); 
		}
		case UNSIGNED_INT:
		case TIME: {
			int val = 0;
			if (bitLen == 8) {
				val = GDR.get_u8(pkt, off);
			} else if (bitLen == 16) {
				val = GDR.get_u16(pkt, off);
			} else if (bitLen == 24) {
				val = GDR.get_u24(pkt, off);
			} else if (bitLen == 32) {
				val = (int)GDR.get_u32(pkt, off);
			}
			return(chanFactory.createServiceChannelValue(chanDef, val)); 
		}
		case DIGITAL: {
			int val = 0;
			if (bitLen == 8) {
				val = GDR.get_u8(pkt, off);
			} else if (bitLen == 16) {
				val = GDR.get_u16(pkt, off);
			} else if (bitLen == 24) {
				val = GDR.get_u24(pkt, off);
			} else if (bitLen == 32) {
				val = (int )GDR.get_u32(pkt, off);
			}
			return(chanFactory.createServiceChannelValue(chanDef, val));  
		}
		case STATUS: {
			int val = 0;
			if (bitLen == 8) {
				val = GDR.get_i8(pkt, off);
			} else if (bitLen == 16) {
				val = GDR.get_i16(pkt, off);
			} else if (bitLen == 24) {
				val = GDR.get_i24(pkt, off);
			} else if (bitLen == 32) {
				val = GDR.get_i32(pkt, off);
			}
			return(chanFactory.createServiceChannelValue(chanDef, val));  
		}
		case FLOAT: {
			double val = 0.0;

			if (bitLen == 32) {
				val = GDR.get_float(pkt, off);
			} else {
				val = GDR.get_double(pkt, off);
			}
			return(chanFactory.createServiceChannelValue(chanDef, val)); 
		}
		case ASCII: {   
		    return(chanFactory.createServiceChannelValue(chanDef, GDR.stringValue(pkt,off,bitLen/8)));		
		}
	

		case BOOLEAN: {
			int val = 0;
			if (bitLen == 8) {
				val = GDR.get_u8(pkt, off);
			} else if (bitLen == 16) {
				val = GDR.get_u16(pkt, off);
			} else if (bitLen == 24) {
				val = GDR.get_u24(pkt, off);
			} else if (bitLen == 32) {
				val = (int )GDR.get_u32(pkt, off);
			}
            return(chanFactory.createServiceChannelValue(chanDef, val));   
		}
		default:
			return null;
		}  
	}
	
	private IServiceChannelValue doChanBits(final String chanStr, final byte[] pkt,final int off,final int startBit, final int bitLength)
	{
		final IChannelDefinition chanDef = channelDict.getDefinitionFromChannelId(chanStr);
		final ChannelType ct = chanDef.getChannelType();

		if (ct.equals(ChannelType.ASCII)) {
		    return chanFactory.createServiceChannelValue(chanDef, GDR.stringValue(pkt,off,bitLength/8));
		}

		int val = 0;
		if ((bitLength <= 8 || (startBit + bitLength) < 8)) {
			val = GDR.get_u8(pkt, off, startBit, bitLength); 
		} else if ((bitLength > 8 || (startBit + bitLength) > 8) && (bitLength <= 16 || (startBit + bitLength) < 16)) {
			val = GDR.get_u16(pkt, off, startBit, bitLength);    
		} else if ((bitLength > 16 || (startBit + bitLength) > 16) && (bitLength <= 24 || (startBit + bitLength) < 24)) {
			val = GDR.get_u24(pkt, off, startBit, bitLength);
		} else if ((bitLength > 24 || (startBit + bitLength) > 24) && (bitLength <= 32 || (startBit + bitLength) < 32)) {   
			val = (int )GDR.get_u32(pkt, off, startBit, bitLength);    
		} 
		return(chanFactory.createServiceChannelValue(chanDef, val)); 
	}    
	
	    /**
     * Extracts a channel value from a monitor packet byte array.
     * 
     * @param chanStr
     *            the channel ID to extract
     * @param in
     *            the incoming byte array
     * @param byte_off
     *            the starting byte offset of the channel in the byte array
     * @param bit_off
     *            the bit offset of the channel, within start byte
     * @param bit_len
     *            the bit length of the channel in the data
     * @return new channel value
     */
	public  IServiceChannelValue getChannel(final String chanStr, final byte[] in, final int byte_off, final int bit_off, final int bit_len) {
		if (bit_off == 0 && (bit_len % 8) == 0) {
			return doChan(chanStr, in, byte_off, bit_len);
		} else {
			return doChanBits(chanStr, in, byte_off, bit_off, bit_len);
		}
	}
}
