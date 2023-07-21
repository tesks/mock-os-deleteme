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


public class GpsTimeExtractor implements ISclkExtractor {

	private final int weekBits, secondBits, subsecondBits;
	private final long subsecondMax;
	private final int bytesNeeded;
	private final int bitsNeeded;
	
	private int weekMask = 0x0fff;
	
	// GPS time does not use leap seconds.  There are always the same number
	// of seconds in a day, and days in a week, hence the constant below;
	private static final int SECONDS_PER_WEEK = 604800;

	private final CoarseFineEncoding targetEncoding;

	public GpsTimeExtractor(CoarseFineEncoding targetEncoding, int weekBits, int secondBits, int subsecondBits, long subsecondModulus) {
		this.targetEncoding = targetEncoding;
		this.weekBits = weekBits;
		this.secondBits = secondBits;
		this.subsecondBits = subsecondBits;
		this.subsecondMax = subsecondModulus;
		this.bitsNeeded = weekBits + secondBits + subsecondBits;
		this.bytesNeeded = (int) Math.ceil((double) bitsNeeded / Byte.SIZE);
	}

	public GpsTimeExtractor(CoarseFineEncoding targetEncoding, int weekBits, int secondBits, int subsecondBits) {
		this(targetEncoding, weekBits, secondBits, subsecondBits, (long) Math.pow(2, subsecondBits) - 1);
	}

	@Override
	public void setStaticArgs(Map<String, Object> params) {
		// Do nothing
	}

	@Override
	public ISclk getValueFromBytes(byte[] buff, int startingOffset) {
		long weeks = CoarseFineExtractor.extractLong(buff, startingOffset, weekBits, weekBits / Byte.SIZE, "GPS weeks");
		startingOffset += weekBits / Byte.SIZE;
		long seconds = CoarseFineExtractor.extractLong(buff, startingOffset, secondBits, secondBits / Byte.SIZE, "GPS seconds");
		startingOffset += secondBits / Byte.SIZE;
		long subseconds = CoarseFineExtractor.extractLong(buff, startingOffset, subsecondBits, subsecondBits / Byte.SIZE, "GPS subseconds");
		
		return sclkFromFields(weeks, seconds, subseconds);
	}

	@Override
	public boolean hasEnoughBytes(byte[] buff, int startingOffset) {
		return buff.length - startingOffset >= bytesNeeded;
	}

	private ISclk sclkFromFields(long rawWeeks, long rawSeconds, long rawSubseconds) {
		long weeks = rawWeeks & weekMask;
		long seconds = rawSeconds + (weeks * SECONDS_PER_WEEK);
		long subseconds = CoarseFineEncoding.normalizeFine(rawSubseconds, subsecondMax, targetEncoding.getMaxFine());
		return new Sclk(seconds, subseconds, targetEncoding);
	}

	@Override
	public ISclk getValueFromBits(BitBuffer buffer, Map<String, Object> args) {
		long weeks = buffer.getUnsignedLong(weekBits);
		long seconds = buffer.getUnsignedLong(secondBits);
		long subseconds = buffer.getUnsignedLong(subsecondBits);
		return sclkFromFields(weeks, seconds, subseconds);
	}


}
