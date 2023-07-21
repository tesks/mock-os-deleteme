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
package ammos.datagen.mission.nsyt.generators.seeds;

import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.mission.nsyt.instrument.Lobt;

/**
 * This is the seed class for the DeltaLobtGenerator. It provides everything
 * needed to initialize the generator. A LOBT is an NSYT SEIS instrument Local
 * Onboard Time.
 * 
 *
 * MPCS-6864 - 11/21/14. Added class.
 */
public class DeltaLobtGeneratorSeed implements ISeedData {
	private long start;
	private long delta;

	/**
	 * Gets the starting LOBT value.
	 * 
	 * @return start value
	 */
	public long getStart() {

		return this.start;
	}

	/**
	 * Sets the starting LOBT, which is used for the first generated packet.
	 * 
	 * @param start
	 *            start value; must be >=0
	 */
	public void setStart(final long start) {

		if (start < 0 || start > Lobt.MAX_VALUE) {
			throw new IllegalArgumentException("start must be >=0 and <="
					+ Lobt.MAX_VALUE);
		}
		this.start = start;
	}

	/**
	 * Gets the LOBT delta, or the value the LOBT will increment between
	 * packets.
	 * 
	 * @return the delta value
	 */
	public long getDelta() {

		return this.delta;
	}

	/**
	 * Sets the LOBT delta, or the value the LOBT will increment between
	 * packets.
	 * 
	 * @param delta
	 *            the delta coarse value
	 */
	public void setDelta(final long delta) {

		if (delta < -Lobt.MAX_VALUE || delta > Lobt.MAX_VALUE) {
			throw new IllegalArgumentException("delta must be between "
					+ -Lobt.MAX_VALUE + " and " + Lobt.MAX_VALUE);
		}

		this.delta = delta;
	}
}
