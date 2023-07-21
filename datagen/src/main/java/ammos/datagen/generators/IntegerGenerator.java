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

import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.IntegerGeneratorSeed;
import ammos.datagen.generators.util.TrackerType;
import ammos.datagen.generators.util.UsageTrackerMap;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;

/**
 * This is a signed integer value generator class. The values to be produced by
 * the generator are loaded from a seed file, which can be overridden. The seed
 * data tells it how large the generated integer objects are, i.e, whether they
 * are Byte, Short, Integer, or Long. These are the data types returned by
 * getNext() and getRandom().
 * 
 * 
 *
 */
public class IntegerGenerator extends AbstractFileSeededGenerator implements
		IFileSeededGenerator {

	private static final String DEFAULT_64_BIT_TABLE = "config/Default64IntegerTable.txt";
	private static final String DEFAULT_32_BIT_TABLE = "config/Default32IntegerTable.txt";
	private static final String DEFAULT_16_BIT_TABLE = "config/Default16IntegerTable.txt";
	private static final String DEFAULT_8_BIT_TABLE = "config/Default8IntegerTable.txt";

	private IntegerGeneratorSeed seedData;
	private boolean tracking = true;

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.AbstractFileSeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed) {

		super.setSeedData(seed);

		if (!(seed instanceof IntegerGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type IntegerGeneratorSeed");
		}
		this.seedData = (IntegerGeneratorSeed) seed;
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
			throw new IllegalStateException("Integer generator is unseeded");
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
			if (this.tracking) {
				UsageTrackerMap

						.getGlobalTrackers()
						.addTracker(
								this.seedData.isUnsigned() ? TrackerType.UNSIGNED_64.getMapType()
										: TrackerType.INTEGER_64.getMapType(),
								getTracker());
			}
			break;
		case 4:
			if (getSeedFile().equals(IFileSeededGenerator.DEFAULT_TABLE_NAME)) {
				setSeedFile(DEFAULT_32_BIT_TABLE);
			}
			if (this.tracking) {
				UsageTrackerMap
						.getGlobalTrackers()
						.addTracker(
								this.seedData.isUnsigned() ? TrackerType.UNSIGNED_32.getMapType()
										: TrackerType.INTEGER_32.getMapType(),
								getTracker());
			}
			break;
		case 2:
			if (getSeedFile().equals(IFileSeededGenerator.DEFAULT_TABLE_NAME)) {
				setSeedFile(DEFAULT_16_BIT_TABLE);
			}
			if (this.tracking) {
				UsageTrackerMap
						.getGlobalTrackers()
						.addTracker(
								this.seedData.isUnsigned() ? TrackerType.UNSIGNED_16.getMapType()
										: TrackerType.INTEGER_16.getMapType(),
								getTracker());
			}
			break;
		case 1:
			if (getSeedFile().equals(IFileSeededGenerator.DEFAULT_TABLE_NAME)) {
				setSeedFile(DEFAULT_8_BIT_TABLE);
			}
			if (this.tracking) {
				UsageTrackerMap
						.getGlobalTrackers()
						.addTracker(
								this.seedData.isUnsigned() ? TrackerType.UNSIGNED_8.getMapType()
										: TrackerType.INTEGER_8.getMapType(),
								getTracker());
			}
			break;
		default:
			throw new IllegalStateException(
					"Maximum integer value size is unsupported: "
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
			throw new IllegalStateException("Integer generator is unseeded");
		}
		try {
			final long val = GDR.parse_long(line);
			switch (this.seedData.getByteSize()) {
			case 8:
				return Long.valueOf(val);
			case 4:
				return Integer.valueOf((int) val);
			case 2:
				return Short.valueOf((short) val);
			case 1:
				return Byte.valueOf((byte) val);
			default:
				throw new IllegalStateException(
						"Maximum integer value size is unsupported: "
								+ this.seedData.getByteSize());
			}
		} catch (final NumberFormatException e) {
			TraceManager.getDefaultTracer().error(

					"Non-parseable integer value found in integer seed file: "
							+ getSeedFile());
			return null;
		}
	}

	/**
	 * Sets the flag indicating whether values produced by this generator are
	 * tracked with a UsageTracker. Must be set before load() is called to be
	 * effective. It is sometimes useful to turn off tracking if the generator
	 * is being used for some other purpose than basic field generation. The
	 * default is to track.
	 * 
	 * @param tracking
	 *            true to track, false to not
	 */
	public void setTracking(final boolean tracking) {

		this.tracking = tracking;
	}
}
