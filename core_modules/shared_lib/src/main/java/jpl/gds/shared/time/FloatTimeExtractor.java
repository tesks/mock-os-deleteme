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

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.types.BitBuffer;

/**
 * Extracts single or double precision floating points and
 * converts them to integer number of coarse ticks and fine ticks.
 * Only supports IEEE-754 floating points at this time.
 *
 */
public class FloatTimeExtractor implements ISclkExtractor {

	private final int byteSize;
	private final CoarseFineEncoding targetEncoding;

	/**
	 * Create a new floating point time extractor.
	 * @param byteSize the size, in bytes, of the floating point
	 * 		  number to extract. Must be 4 or 8.
	 * @param targetEncoding the target SCLK encoding for the floating point time
	 */
	public FloatTimeExtractor(final int byteSize, final CoarseFineEncoding targetEncoding) {
		if (byteSize != 4 && byteSize != 8) {
			throw new IllegalArgumentException("Invalid byte size"
					+ "for floating point: " + byteSize);
		}
		this.byteSize = byteSize;
		this.targetEncoding = targetEncoding;
	}

	@Override
	public void setStaticArgs(final Map<String, Object> params) {
		// Do nothing
	}

	@Override
    public ISclk getValueFromBytes(final byte[] buff, final int startingOffset) {
		final ISclk result;
		if (byteSize == Float.BYTES) {
			final float time = GDR.get_float(buff, startingOffset);
			result = sclkFromFloat(time);
		} else if (byteSize == Double.BYTES) {
			final double time = GDR.get_double(buff, startingOffset);
			result = sclkFromDouble(time);
		} else {
			// Should never happen (see constructor), but construct helpful exception just in case.
			throw new IllegalStateException(this.getClass().getSimpleName()
					+ " configured with invalid byte size for floating point extraction");
		}
		return result;
	}

	@Override
	public boolean hasEnoughBytes(final byte[] buff, final int startingOffset) {
		return (buff.length - startingOffset >= byteSize);
	}
	
	@Override
	public String toString() {
		return new StringBuilder()
				.append("bytes = ")
				.append(byteSize)
				.toString();
	}

	@Override
	public ISclk getValueFromBits(final BitBuffer buffer, final Map<String, Object> args) {
		if (byteSize == Float.SIZE) {
			return sclkFromFloat(buffer.getFloat());
		} else if (byteSize == Double.SIZE) {
			return sclkFromDouble(buffer.getDouble());
		} else {
			// Should never happen (see constructor), but construct helpful exception just in case.
			throw new IllegalStateException(this.getClass().getSimpleName()
					+ " configured with invalid byte size for floating point extraction");
			
		}
	}
	
	private ISclk sclkFromFloat(final float time) {
		final float fraction = time % 1;
		final long fineTicks = (long) (fraction * (targetEncoding.getMaxFine() + 1));
		final long coarseTicks = (long) (time - fraction);
		return new Sclk(coarseTicks, fineTicks, targetEncoding, byteSize * Byte.SIZE);
	}
	
	private ISclk sclkFromDouble(final double time) {
		final double fraction = time % 1;
		final long fineTicks = (long) (fraction * (targetEncoding.getMaxFine() + 1));
		final long coarseTicks = (long) (time - fraction);
		return new Sclk(coarseTicks, fineTicks, targetEncoding, byteSize * Byte.SIZE);
	}

}
