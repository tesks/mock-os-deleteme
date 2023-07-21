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
package ammos.datagen.mission.nsyt.instrument;

import java.nio.ByteBuffer;

import ammos.datagen.util.UnsignedUtil;

/**
 * This class represents an NSYT SEIS instrument Local Onboard Time (LOBT).
 * LOBTs consist of a single 40-bit number. While the value can just be carried
 * in a long, this class is used because the roll behavior for the value is
 * based upon a 40-bit max, rather than a 64-bit max.
 * <p>
 * The format of the LOBT generated is described by JPL D-78505-SEIS-FGICD. The
 * code is based upon a September 26, 2014 draft.
 * 
 * MPCS-6864 - 12/1/14. Added class.
 * 
 */
public class Lobt implements InstrumentTime {
	/** Maximum value for a LOBT */
	public static final long MAX_VALUE = (long) Math.pow(2, 40) - 1;

	private long value;

	/**
	 * Constructor.
	 * 
	 * @param value
	 *            the initial value. Must be <= to MAX_VALUE.
	 */
	public Lobt(final long value) {
		setValue(value);
	}

	/**
	 * Retrieves the current LOBT value.
	 * 
	 * @return current value
	 */
	public long getValue() {
		return this.value;
	}

	/**
	 * Sets the current LOBT value.
	 * 
	 * @param value
	 *            the value to set. Must be <= to MAX_VALUE.
	 */

	public void setValue(final long value) {
		if (value < 0 || value > MAX_VALUE) {
			throw new IllegalArgumentException(
					"A LOBT time value must be > 0 and <= " + MAX_VALUE);
		}
		this.value = value;
	}

	/**
	 * Increments the time by the absolute value of the given delta.
	 * 
	 * @param deltaValue
	 *            the amount by which to increment the time value
	 */
	public void increment(final long deltaValue) {
		if (Math.abs(deltaValue) > MAX_VALUE) {
			throw new IllegalArgumentException(
					"A LOBT delta value cannot be greater than " + MAX_VALUE);
		}

		/*
		 * If value is now too big to fit in 40 bits, the value rolls to the
		 * positive remainder after mod by MAX_VALUE.
		 */
		long temp = this.value + Math.abs(deltaValue);
		if (temp > MAX_VALUE) {
			temp = temp % MAX_VALUE;
		}

		setValue(temp);
	}

	/**
	 * Decrements the time by the absolute value of the given delta.
	 * 
	 * @param deltaValue
	 *            the amount by which to decrement the time value
	 */
	public void decrement(final long deltaValue) {
		if (Math.abs(deltaValue) > MAX_VALUE) {
			throw new IllegalArgumentException(
					"A LOBT delta value cannot be greater than " + MAX_VALUE);
		}

		/*
		 * If value is now less than zero, the value rolls to MAX_VALUE less the
		 * remainder after mod by MAX_VALUE.
		 */
		long temp = this.value - Math.abs(deltaValue);
		if (temp < 0) {
			temp = MAX_VALUE - (Math.abs(temp) % MAX_VALUE);
		}

		setValue(temp);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return (int) this.value;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof Lobt)) {
			return false;
		}
		return this.value == ((Lobt) o).value;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.mission.nsyt.instrument.InstrumentTime#getPaddedBytes()
	 */
	@Override
	public byte[] getPaddedBytes(final int padToLength) {
		if (padToLength < 5) {
			throw new IllegalArgumentException(
					"Pad length is too short for LOBT time");
		}
		final byte[] result = new byte[padToLength];
		final ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
		buffer.putLong(this.value);
		/*
		 * The actual time consists of the lower order 5 bytes of the time
		 * value, plus 0 pad bytes.
		 */
		System.arraycopy(buffer.array(), 3, result, 0, 5);
		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return UnsignedUtil.formatAsUnsigned(this.value);
	}

}
