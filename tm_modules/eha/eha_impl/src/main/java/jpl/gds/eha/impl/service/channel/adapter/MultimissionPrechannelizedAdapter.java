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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * 
 * MultimissionPrechannelizeddapter extracts EHA from packets according to the
 * default pre-channelized EHA packet format used by most JPL flight projects.
 * The format is: 6 byte CCSDS packet header, SCLK (size determined based upon
 * mission setting), and then a repeating series of: 16-bit channel index, N-bit
 * channel value
 *
 *
 */
public class MultimissionPrechannelizedAdapter extends AbstractPrechannelizedAdapter
{
	private final static int INDEX_BYTE_LENGTH = 2;

	/** This is used to record missing channel indices that have been logged.  Yes, it is
	 * a hash table in which both the key and the value are the same, because this is
	 * the fastest data structure for doing this.
	 */
	private final HashMap<Integer, Integer> reportedChans;

	/**
	 * Keep a reference to the current packet we're processing. This allows us
	 * to do some things like beef up the error message for
	 * "channel index not found" with some more information that can actually
	 * help track down the problem.
	 */
	protected ITelemetryPacketMessage currentPacket;

	    /**
     * Creates an instance of MultimissionPrechannelizedEhaAdapter.
     * 
     * @param context
     *            the current application context
     */
    public MultimissionPrechannelizedAdapter(final ApplicationContext context)
	{
		super(context);
		reportedChans = new HashMap<Integer, Integer>(16);
	}
    
    /**
     * 
     * Creates an instance of MultimissionPrechannelizedEhaAdapter.
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
     MultimissionPrechannelizedAdapter(Tracer logTracer, int scid, boolean setSols,
    		IChannelValueFactory chanValFactory, boolean isSse, boolean isStrict,
    		IChannelDefinitionProvider chanDefProvider) {
    	
    	super(logTracer, scid, setSols, chanValFactory, isSse, isStrict, chanDefProvider);
    	reportedChans = new HashMap<Integer, Integer>(16);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.api.service.channel.IPrechannelizedAdapter#extractEha(byte[], int, int)
	 */
	@Override
	public List<IServiceChannelValue> extractEha(final byte[] data, final int offset, final int length) {

		final List<IServiceChannelValue> chanValList = new ArrayList<IServiceChannelValue>(32);

		int curOffset = offset;
		final int end = offset + length;

		//loop through the packet until we're past the end of the data buffer
		while((curOffset + INDEX_BYTE_LENGTH) < end)
		{
			//read in the channel index to identify which channel the value is for
			final int channelIndex = GDR.get_u16(data,curOffset);
			curOffset += INDEX_BYTE_LENGTH;

			/* Removed code to stop when channel index is 0. This
			 * seems to have been a long standing misconception and completely prevents use
			 * of legacy channel A-0000.
			 */
			
			//pull the channel definition from the dictionary via the index we just read in
			final IChannelDefinition chanDef = this.chanDefIndices.get(channelIndex); 

			if(chanDef == null) {
			    //report an unknown channel index (only if we haven't reported it once already)
				/**
				  * Now, warn every time if strict processing is turned on.
				  */
			    final Integer channelIndexObj = channelIndex;
			    if(reportedChans.get(channelIndexObj) == null || this.isStrict) {
			        reportedChans.put(channelIndexObj,channelIndexObj);
			        final StringBuilder sb = new StringBuilder(1024);
			        sb.append("Channel with index = ");
			        sb.append(channelIndex);
			        sb.append(" not found in channel dictionary. ");
			        if (currentPacket != null) {
			            sb.append(currentPacket.getPacketInfo().getIdentifierString());
			        }

                    log.warn(Markers.TLM, sb.toString());
			    } 

			    //if we find an unidentified channel index, we have to skip the rest of the packet
			    //because we don't know where the next channel index should start (we don't know how long
			    //the value for the unidentified index is)
				/** Return empty list if strict processing is turned on. */
			    return this.isStrict ? Collections.emptyList() : chanValList;
			}

			//Create a channel value object from the bytes and 
			//check to make sure that the current channel value we're trying to read in won't cause us
			//to read off the end of the packet (if it does, send a log message error)
			/*
	         * Only add chanVal to ehaList
			 * if createChannelValueFromBytes() succeeds
			 */
			try	{
			    final IServiceChannelValue chanVal = createChannelValueFromBytes(
			            chanDef,
			            data,
			            curOffset,
			            0);
			    chanValList.add(chanVal);
			} catch(final ArrayIndexOutOfBoundsException e)	{
			    final StringBuilder sb = new StringBuilder(1024);
			    sb.append("Unable to extract channel value from EHA packet or product: byte size of channel with index = ");
			    sb.append(channelIndex);
			    sb.append(" (ID = ");
			    sb.append(chanDef.getId());
			    sb.append(") is ");
			    sb.append(chanDef.getSize()/8);
			    sb.append(", but the channel value starts at byte ");
			    sb.append(curOffset);
			    sb.append(" and the total size of the data is only ");
			    sb.append(end);
			    sb.append(" bytes. ");
			    sb.append((currentPacket != null)
			            ? currentPacket.getPacketInfo().getIdentifierString()
			                    : "");

                log.error(Markers.TLM, sb.toString());
                /**  Return empty list if strict processing is turned on. */
			    return this.isStrict ? Collections.emptyList() : chanValList;
			} catch(final Exception e) {
			    e.printStackTrace();
			    final StringBuilder sb = new StringBuilder(1024);
			    sb.append("Unable to extract channel value from EHA packet or product for reason: ");
			    sb.append(e.getMessage());
			    sb.append(" (ID = ");
			    sb.append(chanDef.getId());
			    sb.append("). ");
			    sb.append((currentPacket != null)
			            ? currentPacket.getPacketInfo().getIdentifierString()
			                    : "");

                log.error(Markers.TLM, sb.toString());
                if (isStrict()) {
                	return Collections.emptyList();
				}
			}
			curOffset += chanDef.getSize() / 8;

			/*
			 * Removed chanValList.add(chanVal). Only add chanVal to ehaList if
			 * createChannelValueFromBytes() succeeds
			 */
		}

		return chanValList;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.impl.service.channel.adapter.AbstractPrechannelizedAdapter#extractEha(jpl.gds.tm.service.api.packet.ITelemetryPacketMessage)
	 */
	@Override
	public List<IServiceChannelValue> extractEha(final ITelemetryPacketMessage pm)
	{     
		try
		{
			currentPacket = pm;

			//point the offset to the beginning of the data portion of the packet
			final int offset = pm.getPacketInfo().getPrimaryHeaderLength() + pm.getPacketInfo().getSecondaryHeaderLength(); 
			final int packetSize = pm.getPacketInfo().getSize();
			final byte[] packetBytes = pm.getPacket();

			final List<IServiceChannelValue> chanvals = extractEha(packetBytes, offset, packetSize - offset);
			return chanvals;
		}
		finally
		{
			currentPacket = null;
		}
	}
}
