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
 * This is the seed class for the IntegerGenerator. It contains all the data
 * necessary to initialize the generator.
 * 
 *
 */
public class IntegerGeneratorSeed implements IFileSeedData {
	private int maxSize;
	private String integerTableName = IFileSeedData.DEFAULT_TABLE_NAME;
	private TraversalType traversalType = TraversalType.RANDOM;
	private boolean unsigned;

	/**
	 * Basic constructor.
	 */
	public IntegerGeneratorSeed() {

		// do nothing
	}

	/**
	 * Constructor that takes size and seed table path.
	 * 
	 * @param byteSize
	 *            size of the integers to generate (1,2,4 or 8 bytes)
	 * @param seedFile
	 *            the path to the seed file
	 */
	public IntegerGeneratorSeed(final int byteSize, final String seedFile) {

		setByteSize(byteSize);
		setSeedFile(seedFile);
	}

	/**
	 * Indicates whether this generator seed is for unsigned integers, as
	 * opposed to signed integers.
	 * 
	 * @return true if unsigned, false if signed
	 */
	public boolean isUnsigned() {

		return this.unsigned;
	}

	/**
	 * Sets the flag indicating whether this generator seed is for unsigned
	 * integers, as opposed to signed integers.
	 * 
	 * @param isUnsigned
	 *            true if unsigned, false if signed
	 */
	public void setUnsigned(final boolean isUnsigned) {

		this.unsigned = isUnsigned;
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
	 * Gets the traversal type for integer generation: RANDOM or SEQUENTIAL.
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

		return this.integerTableName;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IFileSeedData#setSeedFile(java.lang.String)
	 */
	@Override
	public void setSeedFile(final String integerTableName) {

		this.integerTableName = integerTableName;
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

		if (maxSize != 1 && maxSize != 2 && maxSize != 4 && maxSize != 8) {
			throw new IllegalArgumentException(
					"Unsupported integer byte size: " + maxSize);
		}
		this.maxSize = maxSize;
	}
}
