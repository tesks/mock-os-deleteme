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

import ammos.datagen.config.TraversalType;

/**
 * This is the seed class for the FloatGenerator. It contains all the data
 * necessary to initialize the generator.
 * 
 *
 */
public class FloatGeneratorSeed implements IFileSeedData {
	private int maxSize;
	private String floatTableName = IFileSeedData.DEFAULT_TABLE_NAME;
	private TraversalType traversalType = TraversalType.RANDOM;
	private boolean includeNanInfinite;

	/**
	 * Basic constructor.
	 */
	public FloatGeneratorSeed() {

		// do nothing
	}

	/**
	 * Constructor that takes size and seed table path.
	 * 
	 * @param byteSize
	 *            size of the floats to generate (4 or 8 bytes)
	 * @param seedFile
	 *            the path to the seed file
	 */
	public FloatGeneratorSeed(final int byteSize, final String seedFile) {

		setByteSize(byteSize);
		setSeedFile(seedFile);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IFileSeedData#getTraversalType()
	 */
	@Override
	public TraversalType getTraversalType() {

		return this.traversalType;
	}

	/**
	 * Gets the traversal type for float generation: RANDOM or SEQUENTIAL.
	 * 
	 * @param traverse
	 *            TraversalType enumeration value; may not be null
	 */
	public void setTraversalType(final TraversalType traverse) {

		if (traverse == null) {
			throw new IllegalArgumentException("traverse may not be null");
		}
		this.traversalType = traverse;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IFileSeedData#getSeedFile()
	 */
	@Override
	public String getSeedFile() {

		return this.floatTableName;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IFileSeedData#setSeedFile(java.lang.String)
	 */
	@Override
	public void setSeedFile(final String integerTableName) {

		this.floatTableName = integerTableName;
	}

	/**
	 * Gets the byte size for integers created by the generator.
	 * 
	 * @return integer byte size
	 */
	public int getByteSize() {

		return this.maxSize;
	}

	/**
	 * Sets the byte size for integers created by the generator.
	 * 
	 * @param maxSize
	 *            the size of integers to generate. Must be 1, 2, 4, or 8.
	 */
	public void setByteSize(final int maxSize) {

		if (maxSize != 4 && maxSize != 8) {
			throw new IllegalArgumentException(
					"Unsupported integer byte size: " + maxSize);
		}
		this.maxSize = maxSize;
	}

	/**
	 * Indicates whether the floating point seed table should be augmented with
	 * NaN and Infinite values.
	 * 
	 * @return true if NaNs and Infinities should be included, false if not.
	 */
	public boolean isIncludeNanInfinite() {

		return this.includeNanInfinite;
	}

	/**
	 * Sets the flag indicating whether the floating point seed table should be
	 * augmented with NaN and Infinite values.
	 * 
	 * @param includeNanInfinite
	 *            true if NaNs and Infinities should be included, false if not.
	 */
	public void setIncludeNanInfinite(final boolean includeNanInfinite) {

		this.includeNanInfinite = includeNanInfinite;
	}
}
