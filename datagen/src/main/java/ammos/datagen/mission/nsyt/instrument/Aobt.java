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

import ammos.datagen.util.UnsignedUtil;
import jpl.gds.shared.gdr.GDR;

/**
 * This class represents an NSYT APSS instrument Onboard Time (AOBT). AOBTs work
 * like SCLKs in that they consist of 32 bit coarse and 16 bit fine values. The
 * reason a SCLK object cannot be used is because AOBTs do not utilize the full
 * 16-bit range of their fine value. The fine value ranges from 0 to 999, above
 * which the coarse (seconds) value will roll. In other words, a SCLK fine
 * increment is 1/(2**N) of a second, where N is the fine length, whereas the
 * AOBT fine increment is always 1/1000th of a second.
 * <p>
 * The format of the AOBT generated is described by JPL D-78505-SEIS-FGICD. The
 * code is based upon a September 26, 2014 draft.
 * 
 * MPCS-6864 - 12/1/14. Added class.
 * 
 */
public class Aobt implements InstrumentTime {
	private static final int FINE_UPPER_LIMIT = 999;

	private long coarse;
	private int fine;

	/**
	 * Constructor.
	 * 
	 * @param coarse
	 *            coarse (seconds) value
	 * @param fine
	 *            fine (sub-seconds) value, 0 to 999
	 */
	public Aobt(final long coarse, final int fine) {
		setCoarse(coarse);
		setFine(fine);
	}

	/**
	 * Retrieves the current coarse (seconds) value.
	 * 
	 * @return coarse value
	 */
	public long getCoarse() {
		return this.coarse;
	}

	/**
	 * Sets the current coarse (seconds) value.
	 * 
	 * @param coarse
	 *            the coarse value to set
	 */
	public void setCoarse(final long coarse) {
		if (coarse < 0) {
			throw new IllegalArgumentException("AOBT coarse time must be > 0");
		}
		this.coarse = coarse;
	}

	/**
	 * Retrieves the current fine (sub-seconds) value.
	 * 
	 * @return fine value
	 */
	public int getFine() {
		return this.fine;
	}

	/**
	 * Sets the current fine (sub-seconds) value.
	 * 
	 * @return fine the fine value to set
	 */
	public void setFine(final int fine) {
		if (fine < 0 || fine > FINE_UPPER_LIMIT) {
			throw new IllegalArgumentException(
					"AOBT fine value must be > 0 and cannot be greater than "
							+ FINE_UPPER_LIMIT);
		}
		this.fine = fine;
	}

	/**
	 * Decrements the current AOBT by the absolute value of the given deltas.
	 * 
	 * @param coarseDelta
	 *            the value by which to decrement coarse time
	 * @param fineDelta
	 *            the value by which to decrement fine time
	 */
	public void decrement(final long coarseDelta, final int fineDelta) {
		long tempCoarse = this.coarse - Math.abs(coarseDelta);
		int tempFine = this.fine - Math.abs(fineDelta);

		/*
		 * If fine is < 0, then we must roll the coarse time backwards by the
		 * number of fine ticks / 1000.
		 */
		if (tempFine < 0) {
			int count = 1;

			tempFine += FINE_UPPER_LIMIT + 1;
			while (this.fine < 0) {
				count++;
				this.fine += FINE_UPPER_LIMIT + 1;
			}

			tempCoarse -= count;
		}
		setCoarse(tempCoarse);
		setFine(tempFine);
	}

	/**
	 * Increments the current AOBT by the absolute value of the given deltas.
	 * 
	 * @param coarseDelta
	 *            the value by which to increment coarse time
	 * @param fineDelta
	 *            the value by which to increment fine time
	 */
	public void increment(final long coarseDelta, final int fineDelta) {
		long tempSecs = this.coarse + Math.abs(coarseDelta);
		int tempFine = this.fine + Math.abs(fineDelta);

		/*
		 * If fine is > 999, then we must roll the coarse time forwards by the
		 * number of fine ticks / 1000. The fine value then becomes the
		 * remainder.
		 */
		if (tempFine > FINE_UPPER_LIMIT) {
			tempSecs += (tempFine / (FINE_UPPER_LIMIT + 1));
			tempFine = (tempFine % (FINE_UPPER_LIMIT + 1));
		}
		setCoarse(tempSecs);
		setFine(tempFine);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return (int) (this.coarse + this.fine);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof Aobt)) {
			return false;
		}
		return this.coarse == ((Aobt) o).coarse && this.fine == ((Aobt) o).fine;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.mission.nsyt.instrument.InstrumentTime#getPaddedBytes()
	 */
	@Override
	public byte[] getPaddedBytes(final int padToLength) {
		if (padToLength < 6) {
			throw new IllegalArgumentException(
					"Pad length is too short for AOBT time");
		}
		final byte[] result = new byte[padToLength];
		GDR.set_u32(result, 0, this.coarse);
		GDR.set_u16(result, 4, this.fine);
		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return UnsignedUtil.formatAsUnsigned(this.coarse) + "."
				+ UnsignedUtil.formatAsUnsigned(this.fine);
	}
}
