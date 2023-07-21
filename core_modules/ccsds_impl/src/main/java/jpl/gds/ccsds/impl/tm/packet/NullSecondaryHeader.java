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

import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;

/** 
 * 	NullSecondaryHeader should be used in the case of packets
 *  that have no secondary header. It will return a 0 ISclk
 *  and a header length of 0.
 *  
 *  This class is immutable and can be used as a singleton.
 */
public class NullSecondaryHeader implements ISecondaryPacketHeader {

	/**
	 * Singleton static instance.
	 */
	public static final NullSecondaryHeader INSTANCE = new NullSecondaryHeader();

	private final int secondaryHeaderLength = 0;
	private final ISclk sclk = new Sclk(0, 0);

	private NullSecondaryHeader() { }

	@Override
	public int getSecondaryHeaderLength() {
		return secondaryHeaderLength;
	}

	@Override
    public ISclk getSclk() {
		return sclk;
	}

}
