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
package jpl.gds.shared.time;

import java.util.Map;

import jpl.gds.shared.types.BitBuffer;

public class FineTimeExtractor implements ISclkExtractor {

	private final int bitSize;
	private final int byteSize;
	
	private final int ticksPerCoarse;
	private final CoarseFineEncoding targetEncoding;
	
	public FineTimeExtractor(int bitSize, int ticksPerCoarse, CoarseFineEncoding targetEncoding) {
		this.bitSize = bitSize;
		this.byteSize = (int) Math.ceil((double) bitSize / Byte.SIZE);
		this.ticksPerCoarse = ticksPerCoarse;
		this.targetEncoding = targetEncoding;
	}

	@Override
	public void setStaticArgs(Map<String, Object> params) {

	}

	@Override
	public ISclk getValueFromBytes(byte[] buff, int startingOffset) {
		long timeValue = CoarseFineExtractor.extractLong(buff, startingOffset, bitSize, byteSize, "fine");
		return valueToSclk(timeValue);
	}

	@Override
	public boolean hasEnoughBytes(byte[] buff, int startingOffset) {
		return buff.length - startingOffset >= byteSize;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		return builder.append("bit size = ")
		.append(bitSize)
		.append(", modulus = ")
		.append(ticksPerCoarse)
		.toString();
	}

	@Override
	public ISclk getValueFromBits(BitBuffer buffer, Map<String, Object> args) {
		long timeValue = buffer.getUnsignedLong(bitSize);
		return valueToSclk(timeValue);
	}
	
	private ISclk valueToSclk(long timeValue) {
		long coarse = timeValue / ticksPerCoarse;
		long fine =  timeValue % ticksPerCoarse;
		fine = CoarseFineEncoding.normalizeFine(fine, ticksPerCoarse - 1, targetEncoding.getMaxFine()); 
		return new Sclk(coarse, fine, targetEncoding);
	}
}
