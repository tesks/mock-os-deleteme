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

import ammos.datagen.generators.ISeededGenerator;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.mission.nsyt.generators.seeds.DeltaLobtGeneratorSeed;
import ammos.datagen.mission.nsyt.instrument.Lobt;

/**
 * This class is a generator for NSYT LOBT (SEIS Local Onboard Time) instrument
 * timestamps, using a delta algorithm. It takes a seed data object that tells
 * it how to generate values.
 * 
 * MPCS-6864 - 12/4/14. Added class.
 */
public class DeltaLobtGenerator implements ISeededGenerator {
	private DeltaLobtGeneratorSeed seedData;
	private long currentValue;;
	private boolean first = true;

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed)
			throws InvalidSeedDataException, IllegalArgumentException {
		if (!(seed instanceof DeltaLobtGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type DeltKLobtGeneratorSeed");
		}
		reset();
		this.seedData = (DeltaLobtGeneratorSeed) seed;
		this.currentValue = this.seedData.getStart();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#reset()
	 */
	@Override
	public void reset() {
		this.seedData = null;
		this.currentValue = 0;
		this.first = true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type from this method is Lobt.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getNext()
	 */
	@Override
	public Object getNext() {

		if (this.seedData == null) {
			throw new IllegalStateException("LOBT generator is unseeded.");
		}

		final Lobt lobt = new Lobt(this.currentValue);

		if (!this.first) {
			if (this.seedData.getDelta() < 0) {
				lobt.decrement(this.seedData.getDelta());
			} else {
				lobt.increment(this.seedData.getDelta());
			}
			this.currentValue = lobt.getValue();
		} else {
			this.first = false;
		}
		return lobt;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method is unsupported by this class.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getRandom()
	 */
	@Override
	public Object getRandom() {

		throw new UnsupportedOperationException(
				"Random LOBT generation from delta LOBT seed is not supported");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Lobt.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */
	@Override
	public Object get() {
		return getNext();
	}

}
