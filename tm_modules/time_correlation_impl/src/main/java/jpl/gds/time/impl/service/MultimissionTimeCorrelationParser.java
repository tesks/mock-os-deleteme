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
package jpl.gds.time.impl.service;

import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.config.CcsdsProperties;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;
import jpl.gds.time.api.config.TimeCorrelationProperties;
import jpl.gds.time.api.service.ITimeCorrelationParser;

/**
 * Common implementation of a time correlation packet parser. Known missions this 
 * is compatible with: SMAP, MSL. No guarantees it will work for any other mission.
 * 
 */
public class MultimissionTimeCorrelationParser implements ITimeCorrelationParser {
	
	
	private int vcid;
	private long vcfc;
	private ISclk sclk;
	private EncodingType encType;
	private long rateIndex;
	private final ISecondaryPacketHeaderLookup secHeaderLookup;
	private final ApplicationContext appContext;
	private final int rateIndexSize;
	
	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public MultimissionTimeCorrelationParser(final ApplicationContext appContext) {
		this.appContext = appContext;
		this.secHeaderLookup = appContext.getBean(ISecondaryPacketHeaderLookup.class);
		this.rateIndexSize = appContext.getBean(TimeCorrelationProperties.class).getFlightRateIndexSize();
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.time.api.service.ITimeCorrelationParser#parse(byte[])
	 */
	@Override
    public void parse(final byte[] packetData) {
    final ISpacePacketHeader header = PacketHeaderFactory.create(appContext.getBean(CcsdsProperties.class).getPacketHeaderFormat());
		header.setPrimaryValuesFromBytes(packetData, 0);
		final ISecondaryPacketHeaderExtractor extractor = secHeaderLookup.lookupExtractor(header);
		if (!extractor.hasEnoughBytes(packetData, header.getPrimaryHeaderLength())) {
            TraceManager.getDefaultTracer(appContext).error("Ran out of bytes for secondary packet header");

			return;
		}
		
		final ISecondaryPacketHeader secondaryHeader = extractor.extract(packetData, header.getPrimaryHeaderLength());
		
		int offset = header.getPrimaryHeaderLength() + secondaryHeader.getSecondaryHeaderLength();
		vcid = GDR.get_u8(packetData, offset);
		offset += 1;
		vcfc = GDR.get_u24(packetData, offset);
		offset += 3;
		final long secs = GDR.get_u32(packetData, offset);
		offset += 4;
		final long subs = GDR.get_u16(packetData, offset);
		offset += 2;
		GDR.get_u8(packetData, offset); //skip last 4 bits of SCLK fine and 4 bits of fill
		offset += 1;

		sclk = new Sclk(secs, subs);
		
	    final int encoding = GDR.get_u8(packetData, offset);
	    offset += 1;
	    encType = EncodingType.BYPASS;
	    if (encoding == 0) {
	    	encType = EncodingType.REED_SOLOMON;
	    } else if (encoding == 1) {
	    	encType = EncodingType.ANY_TURBO;
	    }
	    switch (rateIndexSize) {
	    case 1:
	    	rateIndex = GDR.get_u8(packetData, offset);
	    	offset += 1;
	    	break;
	    case 2:
	    	rateIndex = GDR.get_u16(packetData, offset);
	    	offset += 2;
	    	break;
	    case 4:
	    	rateIndex = GDR.get_u32(packetData, offset);
	    	offset += 4;
	    	break;
	    default:
	        TraceManager.getDefaultTracer().error("Unknown or unsupported size for bit rate index field in TC packet");		

	    }
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.time.api.service.ITimeCorrelationParser#getVcid()
	 */
	@Override
    public int getVcid() {
	
		return vcid;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.time.api.service.ITimeCorrelationParser#getVcfc()
	 */
	@Override
    public long getVcfc() {
	
		return vcfc;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.time.api.service.ITimeCorrelationParser#getSclk()
	 */
	@Override
    public ISclk getSclk() {
	
		return sclk;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.time.api.service.ITimeCorrelationParser#getEncType()
	 */
	@Override
    public EncodingType getEncType() {
	
		return encType;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.time.api.service.ITimeCorrelationParser#getRateIndex()
	 */
	@Override
    public long getRateIndex() {
	
		return rateIndex;
	}
}
