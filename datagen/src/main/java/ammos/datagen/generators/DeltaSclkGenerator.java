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
package ammos.datagen.generators;

import ammos.datagen.generators.seeds.DeltaSclkGeneratorSeed;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;

/**
 * This is a SCLK generator, which can be used to populate packet SCLKs. It is
 * seeded with a string SCLK and a desired delta between SCLK values. Unlike
 * other generators, it does not support random value generation, only
 * sequential value generation, though the requested SCLK delta can be negative,
 * producing descending instead of ascending SCLKS.
 * 
 *
 */
public class DeltaSclkGenerator implements ISclkGenerator {

	/**
	 * Current coarse value - MPCS-6864 - 12/1/14. Made protected.
	 */
	protected long currentCoarse;
	/**
	 * Current fine value. - MPCS-6864 - 12/1/14. Made protected.
	 */
	protected long currentFine;
	/**
	 * Current seed object. - MPCS-6864 - 12/1/14. Made protected.
	 */
	protected DeltaSclkGeneratorSeed seedData;
	/**
	 * First instance generation flag. - MPCS-6864 - 12/1/14. Made
	 * protected.
	 */
	protected boolean first = true;

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed)
			throws InvalidSeedDataException, IllegalArgumentException {

		if (!(seed instanceof DeltaSclkGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type DeltaSclkGeneratorSeed");
		}
		reset();
		this.seedData = (DeltaSclkGeneratorSeed) seed;
		this.currentCoarse = this.seedData.getStartCoarse();
		this.currentFine = this.seedData.getStartFine();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#reset()
	 */
	@Override
	public void reset() {

		this.seedData = null;
		this.currentCoarse = 0;
		this.currentFine = 0;
		this.first = true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type from this method is Sclk.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getNext()
	 */
	@Override
	public Object getNext() {

		if (this.seedData == null) {
			throw new IllegalStateException("SCLK generator is unseeded.");
		}

        ISclk sclk = new Sclk(this.currentCoarse, this.currentFine);
		if (!this.first) {
			if (this.seedData.getDeltaCoarse() < 0) {
				sclk = sclk.decrement(Math.abs(this.seedData.getDeltaCoarse()),
						Math.abs(this.seedData.getDeltaFine()));
			} else {
				sclk = sclk.increment(this.seedData.getDeltaCoarse(),
						this.seedData.getDeltaFine());
			}
			this.currentCoarse = sclk.getCoarse();
			this.currentFine = sclk.getFine();
		} else {
			this.first = false;
		}
		return sclk;
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
				"Random SCLK generation from delta sclk seed is not supported");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Sclk.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */
	@Override
	public Object get() {

		return getNext();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISclkGenerator#isExhausted()
	 */
	@Override
	public boolean isExhausted() {

		return false;
	}
}
