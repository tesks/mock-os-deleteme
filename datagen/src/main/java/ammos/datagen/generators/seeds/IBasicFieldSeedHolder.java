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

import java.util.Map;

/**
 * This class is implemented by generator seeds that hold other seeds required
 * for generation of fields of basic data types: integer, unsigned, float, and
 * enum.
 * 
 *
 */
public interface IBasicFieldSeedHolder {

	/**
	 * Gets the map of EnumGeneratorSeeds to use when generating enum fields.
	 * Each seed is accessed in the map using the name of the enumeration in the
	 * dictionary.
	 * 
	 * @return Map of String to EnumGeneratorSeed; returns an empty map if none
	 *         configured
	 */
	public abstract Map<String, EnumGeneratorSeed> getEnumSeeds();

	/**
	 * Sets the map of EnumGeneratorSeeds to use when generating enum fields.
	 * Each seed is accessed in the map using the name of the enumeration in the
	 * dictionary.
	 * 
	 * @param enumSeeds
	 *            Map of String (enumeration name) to EnumGeneratorSeed; set an
	 *            empty map if none configured
	 */
	public abstract void setEnumSeeds(
			final Map<String, EnumGeneratorSeed> enumSeeds);

	/**
	 * Gets the seed data for integer field generation.
	 * 
	 * @param byteSize
	 *            byte size for the integer seed to get: 1, 2, 4, or 8
	 * @return IntegerGeneratorSeed object
	 */
	public abstract IntegerGeneratorSeed getIntegerSeed(final int byteSize);

	/**
	 * Sets the seed data for integer field generation.
	 * 
	 * @param integerSeed
	 *            IntegerGeneratorSeed object to set
	 * @param byteSize
	 *            byte size of this seed: 1, 2, 4, or 8
	 */
	public abstract void setIntegerSeed(final IntegerGeneratorSeed integerSeed,
			final int byteSize);

	/**
	 * Gets the seed data for unsigned field generation.
	 * 
	 * @param byteSize
	 *            byte size for the unsigned seed to get: 1, 2, 4, or 8
	 * @return IntegerGeneratorSeed object
	 */
	public abstract IntegerGeneratorSeed getUnsignedSeed(final int byteSize);

	/**
	 * Sets the seed data for unsigned field generation.
	 * 
	 * @param unsignedSeed
	 *            IntegerGeneratorSeed object to set
	 * @param byteSize
	 *            byte size of this seed: 1, 2, 4, or 8
	 */
	public abstract void setUnsignedSeed(
			final IntegerGeneratorSeed unsignedSeed, final int byteSize);

	/**
	 * Gets the seed data for float field generation.
	 * 
	 * @param byteSize
	 *            byte size for the float seed to get: 4, or 8
	 * @return FloatGeneratorSeed object
	 */
	public abstract FloatGeneratorSeed getFloatSeed(final int byteSize);

	/**
	 * Sets the seed data for float field generation.
	 * 
	 * @param floatSeed
	 *            FloatGeneratorSeed object to set
	 * @param byteSize
	 *            byte size of this seed: 4, or 8
	 */
	public abstract void setFloatSeed(final FloatGeneratorSeed floatSeed,
			final int byteSize);

	/**
	 * Sets seed data for string field generation.
	 * 
	 * @param strSeed
	 *            string seed to set
	 */
	public abstract void setStringSeed(StringGeneratorSeed strSeed);

}