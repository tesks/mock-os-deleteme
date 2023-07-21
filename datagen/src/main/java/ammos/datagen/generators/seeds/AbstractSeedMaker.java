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

import java.util.HashMap;
import java.util.Map;

import ammos.datagen.config.IMissionConfiguration;
import ammos.datagen.config.IRunConfiguration;
import ammos.datagen.config.InvalidConfigurationException;
import ammos.datagen.config.TraversalType;
import ammos.datagen.generators.IFileSeededGenerator;
import jpl.gds.dictionary.api.EnumerationDefinition;

/**
 * This base class is extended by generator-specific seed maker classes. It
 * provides common methods for creating data generator seeds, such as those for
 * integer, float, enumeration, and unsigned value generation. It also can
 * create the packet SCLK generator seed, and perform some basic validation on
 * the common aspects of the mission and run configurations.
 * 
 *
 */
public class AbstractSeedMaker {

	private final IMissionConfiguration missionConfig;
	private final IRunConfiguration runConfig;

	/**
	 * Constructor.
	 * 
	 * @param missionConfig
	 *            the IMissionConfiguration object for the current data
	 *            generator.
	 * @param runConfig
	 *            the IRunConfiguration object for the current data generator.
	 */
	public AbstractSeedMaker(final IMissionConfiguration missionConfig,
			final IRunConfiguration runConfig) {

		this.missionConfig = missionConfig;
		this.runConfig = runConfig;
	}

	/**
	 * Validates common aspects of the current channel generator configuration
	 * that cannot be verified through XML schema validation, including
	 * relationships among configuration elements. At this time this means
	 * validating the SCLK-related values.
	 * 
	 * @throws InvalidConfigurationException
	 *             if the configuration is found to be in error.
	 */
	public void validate() throws InvalidConfigurationException {

		/*
		 * Get the configured mission SCLK sizes.
		 */
		final int coarseLen = this.missionConfig.getIntProperty(
				IMissionConfiguration.SCLK_COARSE_LEN, 32);
		final int fineLen = this.missionConfig.getIntProperty(
				IMissionConfiguration.SCLK_FINE_LEN, 16);

		/*
		 * If there is a SCLK seed table, packet SCLKs are being read from a
		 * file, and no further validation is done here. If there is no seed
		 * table, the configuration has defined a delta SCLK, and the values
		 * configured there can be validated.
		 */
		if (this.runConfig.getStringProperty(IRunConfiguration.SCLK_SEED_TABLE,
				null) == null) {

			/*
			 * Get the initial packet SCLK values.
			 */
			final int coarseInit = this.missionConfig.getIntProperty(
					IRunConfiguration.INITIAL_SCLK_COARSE, 0);
			final int fineInit = this.missionConfig.getIntProperty(
					IRunConfiguration.INITIAL_SCLK_FINE, 0);

			/*
			 * Verify they fit within mission maximums and throw if not.
			 */
			if (coarseInit > Math.pow(2.0, coarseLen)) {
				throw new InvalidConfigurationException(
						"Initial coarse SCLK value exceeds configured maximum for mission");

			}
			if (fineInit > Math.pow(1.0, fineLen)) {
				throw new InvalidConfigurationException(
						"Initial fine SCLK value exceeds configured maximum for mission");
			}

			/*
			 * Get the delta packet SCLK values.
			 */
			final int coarseDelta = this.missionConfig.getIntProperty(
					IRunConfiguration.SCLK_COARSE_DELTA, 0);
			final int fineDelta = this.missionConfig.getIntProperty(
					IRunConfiguration.SCLK_FINE_DELTA, 0);
			if (coarseDelta > Math.pow(2.0, coarseLen)) {
				throw new InvalidConfigurationException(
						"Delta coarse SCLK value exceeds configured maximum for mission");

			}

			/*
			 * Verify they fit within mission maximums and throw if not.
			 */
			if (fineDelta > Math.pow(1.0, fineLen)) {
				throw new InvalidConfigurationException(
						"Delta fine SCLK value exceeds configured maximum for mission");
			}
		}
	}

	/**
	 * Creates a packet SCLK generator seed from current configuration.
	 * 
	 * @return an ISeedData object, which will be either a
	 *         DeltaSclkGeneratorSeed or a FileSeededSClkGeneratorSeed
	 */
	public ISeedData createSclkGeneratorSeed() {

		/*
		 * Packet SCLK can be seeded from a file, or can be computing using
		 * initial and delta values. We know which is configured by checking to
		 * see if there is a SCLK seed table defined.
		 */
		final String seedFile = this.runConfig.getStringProperty(
				IRunConfiguration.SCLK_SEED_TABLE, null);

		if (seedFile != null) {
			/*
			 * Using a SCLK seed table. Set up the the file seed for the file
			 * seeded SCLK generator.
			 */
			final FileSeededSclkGeneratorSeed sclkSeed = new FileSeededSclkGeneratorSeed();
			sclkSeed.setSeedFile(seedFile);
			sclkSeed.setStopWhenExhausted(this.runConfig.getBooleanProperty(
					IRunConfiguration.SCLK_STOP_WHEN_EXHAUSTED, false));
			return sclkSeed;

		} else {
			/*
			 * Using a delta SCLK. Set up the seed for the delta SCLK generator.
			 */
			final DeltaSclkGeneratorSeed sclkSeed = new DeltaSclkGeneratorSeed();
			sclkSeed.setStartCoarse(this.runConfig.getLongProperty(
					IRunConfiguration.INITIAL_SCLK_COARSE, 0));
			sclkSeed.setStartFine(this.runConfig.getLongProperty(
					IRunConfiguration.INITIAL_SCLK_FINE, 0));
			sclkSeed.setDeltaCoarse(this.runConfig.getLongProperty(
					IRunConfiguration.SCLK_COARSE_DELTA, 0));
			sclkSeed.setDeltaFine(this.runConfig.getLongProperty(
					IRunConfiguration.SCLK_FINE_DELTA, 0));
			return sclkSeed;
		}
	}

	/**
	 * Builds the integer generator seeds and attaches them to the given parent
	 * seed.
	 * 
	 * @param parentSeed
	 *            IBasicFieldSeedHolder to attach integer seeds to
	 */
	protected void makeIntegerSeeds(final IBasicFieldSeedHolder parentSeed) {

		/*
		 * This tells us whether integers will be selected sequentially or
		 * randomly from the seed tables.
		 */
		final TraversalType traverse = this.runConfig.getTraversalTypeProperty(
				IRunConfiguration.INTEGER_TRAVERSAL_TYPE, TraversalType.RANDOM);

		/*
		 * There are 4 integer seeds, for 8, 16, 32, and 64 bit integer values.
		 * Set up an integer generator seed for each, setting the traversal type
		 * on each.
		 */
		final IntegerGeneratorSeed int8Seed = new IntegerGeneratorSeed(1,
				this.runConfig.getStringProperty(
						IRunConfiguration.INTEGER_8_SEED_TABLE, IFileSeededGenerator.DEFAULT_TABLE_NAME));
		int8Seed.setTraversalType(traverse);
		final IntegerGeneratorSeed int16Seed = new IntegerGeneratorSeed(2,
				this.runConfig.getStringProperty(
						IRunConfiguration.INTEGER_16_SEED_TABLE, IFileSeededGenerator.DEFAULT_TABLE_NAME));
		int16Seed.setTraversalType(traverse);
		final IntegerGeneratorSeed int32Seed = new IntegerGeneratorSeed(4,
				this.runConfig.getStringProperty(
						IRunConfiguration.INTEGER_32_SEED_TABLE, IFileSeededGenerator.DEFAULT_TABLE_NAME));
		int32Seed.setTraversalType(traverse);
		final IntegerGeneratorSeed int64Seed = new IntegerGeneratorSeed(8,
				this.runConfig.getStringProperty(
						IRunConfiguration.INTEGER_64_SEED_TABLE, IFileSeededGenerator.DEFAULT_TABLE_NAME));
		int64Seed.setTraversalType(traverse);

		/*
		 * Attach these to the parent seed, per supported byte sizes of 1, 2, 4,
		 * and 8.
		 */
		parentSeed.setIntegerSeed(int8Seed, 1);
		parentSeed.setIntegerSeed(int16Seed, 2);
		parentSeed.setIntegerSeed(int32Seed, 4);
		parentSeed.setIntegerSeed(int64Seed, 8);
	}

	/**
	 * Builds the unsigned generator seeds and attaches them to the given parent
	 * seed.
	 * 
	 * @param parentSeed
	 *            IBasicFieldSeedHolder to attach integer seeds to
	 */
	protected void makeUnsignedSeeds(final IBasicFieldSeedHolder parentSeed) {

		/*
		 * This tells us whether unsigned values will be selected sequentially
		 * or randomly from the seed tables.
		 */
		final TraversalType traverse = this.runConfig
				.getTraversalTypeProperty(
						IRunConfiguration.UNSIGNED_TRAVERSAL_TYPE,
						TraversalType.RANDOM);
		/*
		 * There are 4 unsigned seeds, for 8, 16, 32, and 64 bit integer values.
		 * Set up an integer generator seed for each, setting the unsigned flag
		 * and traversal type on each seed.
		 */
		final IntegerGeneratorSeed uint8Seed = new IntegerGeneratorSeed(1,
				this.runConfig.getStringProperty(
						IRunConfiguration.UNSIGNED_8_SEED_TABLE, IFileSeededGenerator.DEFAULT_TABLE_NAME));
		uint8Seed.setUnsigned(true);
		uint8Seed.setTraversalType(traverse);
		final IntegerGeneratorSeed uint16Seed = new IntegerGeneratorSeed(2,
				this.runConfig.getStringProperty(
						IRunConfiguration.UNSIGNED_16_SEED_TABLE, IFileSeededGenerator.DEFAULT_TABLE_NAME));
		uint16Seed.setUnsigned(true);
		uint16Seed.setTraversalType(traverse);
		final IntegerGeneratorSeed uint32Seed = new IntegerGeneratorSeed(4,
				this.runConfig.getStringProperty(
						IRunConfiguration.UNSIGNED_32_SEED_TABLE, IFileSeededGenerator.DEFAULT_TABLE_NAME));
		uint32Seed.setUnsigned(true);
		uint32Seed.setTraversalType(traverse);
		final IntegerGeneratorSeed uint64Seed = new IntegerGeneratorSeed(8,
				this.runConfig.getStringProperty(
						IRunConfiguration.UNSIGNED_64_SEED_TABLE, IFileSeededGenerator.DEFAULT_TABLE_NAME));
		uint64Seed.setUnsigned(true);
		uint64Seed.setTraversalType(traverse);

		/*
		 * Attach these to the parent seed, per supported byte sizes of 1, 2, 4,
		 * and 8.
		 */
		parentSeed.setUnsignedSeed(uint8Seed, 1);
		parentSeed.setUnsignedSeed(uint16Seed, 2);
		parentSeed.setUnsignedSeed(uint32Seed, 4);
		parentSeed.setUnsignedSeed(uint64Seed, 8);
	}

	/**
	 * Builds the float generator seeds and attaches them to the given parent
	 * seed.
	 * 
	 * @param parentSeed
	 *            IBasicFieldSeedHolder to attach float seeds to
	 */
	protected void makeFloatSeeds(final IBasicFieldSeedHolder parentSeed) {

		/*
		 * This tells us whether floats will be selected sequentially or
		 * randomly from the seed tables.
		 */
		final TraversalType traverse = this.runConfig.getTraversalTypeProperty(
				IRunConfiguration.FLOAT_TRAVERSAL_TYPE, TraversalType.RANDOM);

		/*
		 * There are 2 float seeds, for 32 and 64 bit floating point values. Set
		 * up a float generator seed for each, setting the traversal type on
		 * each seed.
		 */
		final FloatGeneratorSeed float32Seed = new FloatGeneratorSeed(4,
				this.runConfig.getStringProperty(
						IRunConfiguration.FLOAT_32_SEED_TABLE, IFileSeededGenerator.DEFAULT_TABLE_NAME));
		float32Seed.setTraversalType(traverse);
		final FloatGeneratorSeed float64Seed = new FloatGeneratorSeed(8,
				this.runConfig.getStringProperty(
						IRunConfiguration.FLOAT_64_SEED_TABLE, IFileSeededGenerator.DEFAULT_TABLE_NAME));
		float64Seed.setTraversalType(traverse);

		/*
		 * Attach these to the parent seed, per supported byte sizes of 4 and 8.
		 */
		parentSeed.setFloatSeed(float32Seed, 4);
		parentSeed.setFloatSeed(float64Seed, 8);
	}

	/**
	 * Makes the seeds for the EnumGenerator, one for each enumeration table in
	 * the dictionary.
	 * 
	 * @param parentSeed
	 *            the IBasicFieldSeedHolder to attach enum seeds to
	 * @param enumDefs
	 *            a Map of enumeration name to EnumerationDefinition object for
	 *            every enumeration defined in the dictionary.
	 */
	protected void makeEnumSeeds(final IBasicFieldSeedHolder parentSeed,
			final Map<String, EnumerationDefinition> enumDefs) {

		/*
		 * These flags apply to all enumeration generators, indicating whether
		 * to generate invalid values and how to traverse each enumeration,
		 * sequentially or randomly.
		 */
		final boolean useInvalid = this.runConfig.getBooleanProperty(
				IRunConfiguration.INCLUDE_INVALID_ENUMS, false);
		final float percent = this.runConfig.getFloatProperty(
				IRunConfiguration.INVALID_ENUM_PERCENT, (float) 0.0);
		final TraversalType traverse = this.runConfig.getTraversalTypeProperty(
				IRunConfiguration.ENUM_TRAVERSAL_TYPE, TraversalType.RANDOM);

		/*
		 * We need one enumeration seed for each enmumeration defined in the
		 * dictionary. Loop through them and create a seed for each, setting
		 * flags on each seed as determined above. These get put into a map,
		 * where they can be accessed using the enumeration name.
		 */
		final Map<String, EnumGeneratorSeed> enumSeeds = new HashMap<String, EnumGeneratorSeed>();

		for (final String key : enumDefs.keySet()) {
			final EnumGeneratorSeed seed = new EnumGeneratorSeed(key,
					enumDefs.get(key), useInvalid);
			seed.setInvalidPercent(percent);
			seed.setTraversalType(traverse);
			enumSeeds.put(key, seed);
		}

		/*
		 * Attach the enumeration seed map to the parent seed.
		 */
		parentSeed.setEnumSeeds(enumSeeds);
	}

	/**
	 * Builds the string generator seed and attaches it to the given parent
	 * seed.
	 * 
	 * @param parentSeed
	 *            IBasicFieldSeedHolder to attach string seed to
	 */
	protected void makeStringSeed(final IBasicFieldSeedHolder parentSeed) {

		final StringGeneratorSeed str8Seed = new StringGeneratorSeed();
		str8Seed.setMaxStringLength(this.runConfig.getIntProperty(
				IRunConfiguration.STRING_MAX_LEN, 80));
		str8Seed.setIncludeNullCharacter(this.runConfig.getBooleanProperty(
				IRunConfiguration.INCLUDE_NULL_CHAR, false));
		str8Seed.setIncludeEmptyStrings(this.runConfig.getBooleanProperty(
				IRunConfiguration.INCLUDE_EMPTY_STRINGS, false));
		str8Seed.setTraversalType(this.runConfig.getTraversalTypeProperty(
				IRunConfiguration.STRING_TRAVERSAL_TYPE, TraversalType.RANDOM));
		str8Seed.setCharSet(this.runConfig.getStringProperty(
				IRunConfiguration.STRING_CHAR_SET,
				StringGeneratorSeed.DEFAULT_CHAR_SET));
		parentSeed.setStringSeed(str8Seed);
	}
}
