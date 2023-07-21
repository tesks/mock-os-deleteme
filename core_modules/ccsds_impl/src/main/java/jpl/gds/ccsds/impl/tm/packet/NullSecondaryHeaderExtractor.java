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

/** 
 *  This class can be used to handle packets without secondary headers.
 *  Any packet processor using it can freely lookup an extractor and 
 *  perform "extraction" safely.
 */
public class NullSecondaryHeaderExtractor implements
		ISecondaryPacketHeaderExtractor {

	@Override
	public ISecondaryPacketHeader extract(byte[] data, int offset) {
		return NullSecondaryHeader.INSTANCE;
	}

	@Override
	public boolean hasEnoughBytes(byte[] data, int offset) {
		return true;
	}

	@Override
	public void setStaticArgs(Map<String, Object> args) {
		// Do nothing
	}

}
