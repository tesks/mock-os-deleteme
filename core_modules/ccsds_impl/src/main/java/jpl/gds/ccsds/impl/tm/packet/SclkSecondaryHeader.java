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

/**
 * SclkSecondaryHeader represents a secondary header that only contains
 * a single hardware timestamp (which gets has been converted to a canonical Sclk).
 */
public class SclkSecondaryHeader implements ISecondaryPacketHeader {

	private final ISclk sclk;
	private final int secHdrLength;

	/** Construct a header instance from a canonical
	 * Sclk.  Byte length will be inferred from the ISclk itself.
	 * @param sclk canonical sclk to store
	 */
    public SclkSecondaryHeader(final ISclk sclk) {
		this.sclk = sclk;
		secHdrLength = sclk.getByteLength();
	}


	/** Construct a header instance with a sclk and a length
	 * that may not match the byte length of the canonical Sclk.
	 * @param sclk canonical sclk to store
	 * @param headerLength the length, in bytes, to report
	 * 			in subsequent calls to getSecondaryHeaderLength
	 */
    public SclkSecondaryHeader(final ISclk sclk,
			final int headerLength) {
		this.sclk = sclk;
		this.secHdrLength = headerLength;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSecondaryHeaderLength() {
		return secHdrLength;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public ISclk getSclk() {
		return sclk;
	}

}
