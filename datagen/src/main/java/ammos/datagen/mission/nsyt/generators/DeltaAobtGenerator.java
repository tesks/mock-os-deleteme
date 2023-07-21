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
package ammos.datagen.mission.nsyt.generators;

import ammos.datagen.generators.DeltaSclkGenerator;
import ammos.datagen.mission.nsyt.instrument.Aobt;

/**
 * This class is a generator for NSYT AOBT (APSS Onboard Time) instrument
 * timestamps, using a delta algorithm. It extends the DeltaSclkGenerator
 * because AOBTs basically behave like SCLKs in that they have a 32.16
 * coarse.fine format; they just roll differently, so a SCLK object cannot be
 * used.
 * <p>
 * The seed data for this generator is identical to that for the
 * DeltaSclkGenerator, so the DeltaSclkGeneratorSeed has been re-purposed by
 * this class and no AOBT seed class exists.
 * 
 * MPCS-6864 - 12/1/14. Added class.
 */
public class DeltaAobtGenerator extends DeltaSclkGenerator {

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type from this method is Aobt.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getNext()
	 */
	@Override
	public Object getNext() {

		if (this.seedData == null) {
			throw new IllegalStateException("AOBT generator is unseeded.");
		}

		final Aobt aobt = new Aobt(this.currentCoarse, (int) this.currentFine);

		if (!this.first) {
			if (this.seedData.getDeltaCoarse() < 0) {
				aobt.decrement(this.seedData.getDeltaCoarse(),
						(int) this.seedData.getDeltaFine());
			} else {
				aobt.increment(this.seedData.getDeltaCoarse(),
						(int) this.seedData.getDeltaFine());
			}
			this.currentCoarse = aobt.getCoarse();
			this.currentFine = aobt.getFine();
		} else {
			this.first = false;
		}
		return aobt;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Aobt.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */
	@Override
	public Object get() {

		return getNext();
	}
}
