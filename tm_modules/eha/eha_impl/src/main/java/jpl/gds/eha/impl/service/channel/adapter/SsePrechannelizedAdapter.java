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
import jpl.gds.eha.api.service.channel.PrechannelizedAdapterException;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.CoarseFineEncoding;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ISclkExtractor;
import jpl.gds.shared.time.LocalSolarTimeFactory;
import jpl.gds.shared.time.SclkScetUtility;
import jpl.gds.shared.time.TimeProperties;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * 
 * SsePrechannelizedAdapter extracts EHA from packets according to the SSE EHA
 * packet format. The format is: 6 byte CCSDS packet header, 48-bit SCLK, and
 * then a repeating series of: 16-bit channel index, 48-bit SCLK, N-bit channel
 * value, 64-bit UTC.
 * 
 */
public class SsePrechannelizedAdapter extends AbstractPrechannelizedAdapter
{
    /** Byte length of the UTC seconds field in an SSE pre-channelized packet */
    public final static int UTC_SECS_BYTE_LENGTH = 4;
    /**
     * Byte length of the UTC milliseconds field in an SSE pre-channelized
     * packet
     */
	public final static int UTC_MSECS_BYTE_LENGTH = 4;
    /** Total byte length of UTC field in a SSE pre-channelized packet */
	public final static int UTC_BYTE_LENGTH = UTC_SECS_BYTE_LENGTH + UTC_MSECS_BYTE_LENGTH;

	// This is used to record missing channel indices that have been logged.  Yes, it is
	// a hashtable in which both the key and the value are the same, because this is
	// the fastest data structure for doing this.
	private final HashMap<Integer, Integer> reportedChans = new HashMap<Integer, Integer>();

	private final CoarseFineEncoding sclkEncoding;
	private final ISclkExtractor sclkExtractor;
	private ITelemetryPacketMessage currentPacket;

	    /**
     * Creates an instance of SsePrechannelizedAdapter.
     * 
     * @param context
     *            the current application context
     */
    public SsePrechannelizedAdapter(final ApplicationContext context)
	{
		super(context);
		sclkEncoding = TimeProperties.getInstance().getCanonicalEncoding();
		sclkExtractor =  TimeProperties.getInstance().getCanonicalExtractor();
	}
    
    
    /**
     * 
     * Creates an instance of SsePrechannelizedEhaAdapter.
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
     SsePrechannelizedAdapter(Tracer logTracer, int scid, boolean setSols,
    		IChannelValueFactory chanValFactory, boolean isSse, boolean isStrict,
    		IChannelDefinitionProvider chanDefProvider) {
    	
    	super(logTracer, scid, setSols, chanValFactory, isSse, isStrict, chanDefProvider);
    	sclkEncoding = TimeProperties.getInstance().getCanonicalEncoding();
		sclkExtractor =  TimeProperties.getInstance().getCanonicalExtractor();    	
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.eha.impl.service.channel.adapter.AbstractPrechannelizedAdapter#extractEha(jpl.gds.tm.service.api.packet.ITelemetryPacketMessage)
	 */
	@Override
	public List<IServiceChannelValue> extractEha(final ITelemetryPacketMessage pm) throws PrechannelizedAdapterException
	{     
		currentPacket = pm;

		//point the offset to the beginning of the data portion of the packet
		final int offset = pm.getPacketInfo().getPrimaryHeaderLength() + pm.getPacketInfo().getSecondaryHeaderLength();

		final int packetSize = pm.getPacketInfo().getSize();
		final byte[] packetBytes = pm.getPacket();

		return extractEha(packetBytes, offset, packetSize);       
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.eha.impl.service.channel.adapter.AbstractPrechannelizedAdapter#extractEha(byte[], int,
	 *      int)
	 */
	@Override
	public List<IServiceChannelValue> extractEha(final byte[] packetBytes,
			final int offset, final int packetSize) throws PrechannelizedAdapterException {

		final List<IServiceChannelValue> chanValList = new ArrayList<IServiceChannelValue>(32);

		int off = offset;

		//loop through the packet until we're past the end of the data buffer
		while((off + INDEX_BYTE_LENGTH) < packetSize)
		{
			//read in the channel index to identify which channel the value is for
			final int channelIndex = GDR.get_u16(packetBytes,off);
			off += INDEX_BYTE_LENGTH;

			// If index is 0, the rest of the packet is fill.
			if (channelIndex == 0) {
				return(chanValList);
			}

			/* Specifically get definition for SSE channel. */
			//pull the channel definition from the dictionary via the index we just read in
			final IChannelDefinition chanDef = this.chanDefIndices.get(channelIndex); 
			if(chanDef == null)	{

				//report an unknown channel index (only if we haven't reported it once already)
				final Integer channelIndexObj = channelIndex;
				if(reportedChans.get(channelIndexObj) == null || this.isStrict)	{
					reportedChans.put(channelIndexObj,channelIndexObj);
					final StringBuilder sb = new StringBuilder(1024);
					sb.append("Channel with index = ");
					sb.append(channelIndex);
					sb.append(" not found in channel dictionary. ");
					sb.append(currentPacket == null ? "" : currentPacket.getPacketInfo().getIdentifierString());


					log.warn(Markers.TLM, sb.toString());
				}

				//if we find an unidentified channel index, we have to skip the rest of the packet
				//because we don't know where the next channel index should start (we don't know how long
				//the value for the unidentified index is)
				return this.isStrict ? Collections.emptyList() : chanValList;
			}

			//////////////////
			//Read in the SCLK value for this particular
			//EHA channel value
			//////////////////
			if (!sclkExtractor.hasEnoughBytes(packetBytes, off)) {
				final StringBuilder sb = new StringBuilder(1024);
				sb.append("Unable to extract SCLK from EHA packet: SCLK size is ");
				sb.append(sclkEncoding.getByteLength());
				sb.append(" bytes, but the ");
				sb.append("SCLK value starts at byte ");
				sb.append(off);
				sb.append(" and the total size of the packet is only ");
				sb.append(packetSize);
				sb.append(" bytes. ");
				sb.append(currentPacket.getPacketInfo().getIdentifierString());

                log.warn(Markers.TLM, sb.toString());
				return this.isStrict ? Collections.emptyList() : chanValList;
			}
			final ISclk sclk = sclkExtractor.getValueFromBytes(packetBytes, off);
			off += sclk.getByteLength();

			//////////////////
			//Create a channel value object from the bytes and 
			//check to make sure that the current channel value we're trying to read in won't cause us
			//to read off the end of the packet (if it does, send a log message error)
			//////////////////
			IServiceChannelValue chanVal = null;
			try	{
				chanVal = createChannelValueFromBytes(chanDef,
						packetBytes,
						off,
						0);
			} catch(final ArrayIndexOutOfBoundsException e)	{
				final StringBuilder sb = new StringBuilder(1024);
				sb.append("Unable to extract channel value from EHA packet: byte size of channel with index = ");
				sb.append(channelIndex); 
				sb.append(" (ID = ");
				sb.append(chanDef.getId());
				sb.append(") is ");
				sb.append(chanDef.getSize()/8);
				sb.append(", but the channel value starts at byte ");
				sb.append(off);
				sb.append(" and the total size of the packet is only ");
				sb.append(packetSize);
				sb.append(" bytes. ");
				sb.append(currentPacket == null ? "" : currentPacket.getPacketInfo().getIdentifierString());

                log.error(Markers.TLM, sb.toString());
				return this.isStrict ? Collections.emptyList() : chanValList;
			}
			off += chanDef.getSize()/8;

			///////////////////
			//Read the UTC for this particular EHA channel value
			///////////////////
			if ((off + UTC_BYTE_LENGTH)  > packetSize) {
				final StringBuilder sb = new StringBuilder(1024);
				sb.append("Unable to extract UTC from EHA packet: UTC size is ");
				sb.append(UTC_BYTE_LENGTH);
				sb.append(" bytes, but the UTC value starts at byte ");
				sb.append(off);
				sb.append(" and the total size of the packet is only ");
				sb.append(packetSize);
				sb.append(" bytes. ");
				sb.append(currentPacket ==  null ? "" :currentPacket.getPacketInfo().getIdentifierString());

                log.error(Markers.TLM, sb.toString());
				return this.isStrict ? Collections.emptyList() : chanValList;
			}

			final long seconds = GDR.get_u32(packetBytes,off);
			off += UTC_SECS_BYTE_LENGTH; 
			final long milliseconds = GDR.get_u32(packetBytes,off);
			off += UTC_MSECS_BYTE_LENGTH;
			final IAccurateDateTime utc = new AccurateDateTime(seconds * 1000 + milliseconds);

			chanVal.setSclk(sclk);
            final IAccurateDateTime scet = SclkScetUtility.getScet(sclk, null, scid, log);
			if(scet != null) {
				chanVal.setScet(scet);

				//SSE is never used in a venue that has SOL, but I'll leave the
				//check in here just in case (brn)
				if(setSols)	{
					chanVal.setLst(LocalSolarTimeFactory.getNewLst(scet, scid));
				}
			} else {
				log.warn("Could not calculate SCET time for current EHA value.  Reverting to packet SCET time...");
				if (this.isStrict) {
					return Collections.emptyList();
				}
			}
			chanVal.setErt(utc);

			chanValList.add(chanVal);
		}

		return(chanValList);        
	}
}
