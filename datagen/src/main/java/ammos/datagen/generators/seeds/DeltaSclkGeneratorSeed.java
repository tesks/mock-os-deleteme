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
package ammos.datagen.generators.seeds;

/**
 * This is the seed class for the DeltaSclkGenerator. It provides everything needed
 * to initialize the generator.
 * 
 *
 */
public class DeltaSclkGeneratorSeed implements ISeedData {
	private long startCoarse;
	private long startFine;
	private long deltaCoarse;
	private long deltaFine;

	/**
	 * Gets the starting coarse SCLK, which is used for the first generated
	 * packet.
	 * 
	 * @return start coarse value
	 */
	public long getStartCoarse() {

		return this.startCoarse;
	}

	/**
	 * Sets the starting coarse SCLK, which is used for the first generated
	 * packet.
	 * 
	 * @param startCoarse
	 *            start coarse value; must be >=0
	 */
	public void setStartCoarse(final long startCoarse) {

		if (startCoarse < 0) {
			throw new IllegalArgumentException("startCoarse must be >=0");
		}
		this.startCoarse = startCoarse;
	}

	/**
	 * Gets the starting fine SCLK, which is used for the first generated
	 * packet.
	 * 
	 * @return start fine value
	 */
	public long getStartFine() {

		return this.startFine;
	}

	/**
	 * Sets the starting fine SCLK, which is used for the first generated
	 * packet.
	 * 
	 * @param startFine
	 *            start fine value; must be >=0
	 */
	public void setStartFine(final long startFine) {

		if (startFine < 0) {
			throw new IllegalArgumentException("startFine must be >=0");
		}
		this.startFine = startFine;
	}

	/**
	 * Sets the coarse SCLK delta, or the value the coarse SCLK will increment
	 * between packets.
	 * 
	 * @return the delta coarse value
	 */
	public long getDeltaCoarse() {

		return this.deltaCoarse;
	}

	/**
	 * Gets the coarse SCLK delta, or the value the coarse SCLK will increment
	 * between packets.
	 * 
	 * @param deltaCoarse
	 *            the delta coarse value
	 */
	public void setDeltaCoarse(final long deltaCoarse) {

		this.deltaCoarse = deltaCoarse;
	}

	/**
	 * Sets the fine SCLK delta, or the value the fine SCLK will increment (or
	 * decrement) between packets.
	 * 
	 * @return the delta fine value
	 */
	public long getDeltaFine() {

		return this.deltaFine;
	}

	/**
	 * Gets the fine SCLK delta, or the value the fine SCLK will increment (or
	 * decrement) between packets.
	 * 
	 * @param deltaFine
	 *            the delta fine value
	 */
	public void setDeltaFine(final long deltaFine) {

		this.deltaFine = deltaFine;
	}
}
