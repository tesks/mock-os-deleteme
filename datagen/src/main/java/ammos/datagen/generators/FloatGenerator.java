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

import ammos.datagen.generators.seeds.FloatGeneratorSeed;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.util.TrackerType;
import ammos.datagen.generators.util.UsageTrackerMap;
import jpl.gds.shared.log.TraceManager;

/**
 * This is a floating point value generator class. The values to be produced by
 * the generator are loaded from a seed file, which can be overridden. The seed
 * data tells it how large the generated integer objects are, i.e, whether they
 * are Float or Double. These are the data types returned by getNext() and
 * getRandom().
 * 
 * 
 *
 */
public class FloatGenerator extends AbstractFileSeededGenerator implements
		IFileSeededGenerator {

	private static final String DEFAULT_64_BIT_TABLE = "config/Default64FloatTable.txt";
	private static final String DEFAULT_32_BIT_TABLE = "config/Default32FloatTable.txt";

	private FloatGeneratorSeed seedData;

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.AbstractFileSeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed) {

		super.setSeedData(seed);

		if (!(seed instanceof FloatGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type FloatGeneratorSeed");
		}
		this.seedData = (FloatGeneratorSeed) seed;

		if (this.seedData.isIncludeNanInfinite()) {
			if (this.seedData.getByteSize() == 8) {
				addValue(Double.NaN);
				addValue(Double.POSITIVE_INFINITY);
				addValue(Double.NEGATIVE_INFINITY);
			} else {
				addValue(Float.NaN);
				addValue(Float.POSITIVE_INFINITY);
				addValue(Float.NEGATIVE_INFINITY);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.AbstractFileSeededGenerator#reset()
	 */
	@Override
	public void reset() {

		super.reset();
		this.seedData = null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.AbstractFileSeededGenerator#load()
	 */
	@Override
	public boolean load() {

		if (this.seedData == null) {
			throw new IllegalStateException("Float generator is unseeded");
		}
		/*
		 * MPCS-6864 - 12/1/14. Trackers were not being added unless the
		 * default tables were used. Modified the logic below to fix that.
		 */

		switch (this.seedData.getByteSize()) {
		case 8:
			if (getSeedFile().equals(IFileSeededGenerator.DEFAULT_TABLE_NAME)) {
				setSeedFile(DEFAULT_64_BIT_TABLE);
			}
			UsageTrackerMap.getGlobalTrackers().addTracker(
					TrackerType.FLOAT_64.getMapType(), getTracker());
			break;
		case 4:
			if (getSeedFile().equals(IFileSeededGenerator.DEFAULT_TABLE_NAME)) {
				setSeedFile(DEFAULT_32_BIT_TABLE);
			}
			UsageTrackerMap.getGlobalTrackers().addTracker(
					TrackerType.FLOAT_32.getMapType(), getTracker());
			break;
		default:
			throw new IllegalStateException(
					"Maximum float value size is unsupported: "
							+ this.seedData.getByteSize());
		}

		return super.load();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.AbstractFileSeededGenerator#convertLineToPrimitive(java.lang.String)
	 */
	@Override
	protected Object convertLineToPrimitive(final String line) {

		if (this.seedData == null) {
			throw new IllegalStateException("float generator is unseeded");
		}
		try {
			final double val = Double.valueOf(line);
			switch (this.seedData.getByteSize()) {
			case 8:
				return Double.valueOf(val);
			case 4:
				return Float.valueOf((float) val);
			default:
				throw new IllegalStateException(
						"Maximum float value size is unsupported: "
								+ this.seedData.getByteSize());
			}
		} catch (final NumberFormatException e) {
			TraceManager.getDefaultTracer().error("Non-parseable floating point value found in float seed file: " + getSeedFile());
			return null;
		}
	}
}
