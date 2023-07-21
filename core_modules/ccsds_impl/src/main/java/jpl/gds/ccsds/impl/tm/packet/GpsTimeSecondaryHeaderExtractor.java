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
package jpl.gds.ccsds.impl.tm.packet; 
import java.util.Map;

import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ISclkExtractor;

/**
 * A secondary header extractor for packets with GPS time codes.
 * 
 */
public class GpsTimeSecondaryHeaderExtractor implements ISecondaryPacketHeaderExtractor {

	ISclkExtractor sclkExtractor;

	@Override
	public ISecondaryPacketHeader extract(byte[] data, int offset) {
		final ISclk sclk = sclkExtractor.getValueFromBytes(data, offset);
		return new SclkSecondaryHeader(sclk, sclk.getByteLength());
	}

	@Override
	public boolean hasEnoughBytes(byte[] data, int offset) {
		return sclkExtractor.hasEnoughBytes(data, offset);
	}

	@Override
	public void setStaticArgs(Map<String, Object> args) {
		// TODO Auto-generated method stub
		
	}

}
