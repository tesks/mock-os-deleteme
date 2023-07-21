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

import java.util.Random;

import ammos.datagen.config.TraversalType;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.StringGeneratorSeed;

/**
 * This is a string generator class. The values to be produced by the generator
 * are loaded from a seed file, which can be overridden. The seed data tells it
 * how long the generated string objects are. These are the string lengths
 * returned by getNext() and getRandom().
 * 
 * 
 *
 */
public class StringGenerator implements ISeededGenerator {

	private final Random random = new Random();
	private StringGeneratorSeed seedData;
	private int index;

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.AbstractFileSeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed) {

		if (!(seed instanceof StringGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type StringGeneratorSeed");
		}
		this.seedData = (StringGeneratorSeed) seed;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.AbstractFileSeededGenerator#reset()
	 */
	@Override
	public void reset() {

		this.seedData = null;
		this.index = 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Actual return type of this method is Integer
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */
	@Override
	public String get() {

		if (this.seedData == null) {
			throw new IllegalStateException("String generator is unseeded");
		}

		if (this.seedData.getTraversalType().equals(TraversalType.SEQUENTIAL)) {
			return getNext();
		} else {
			return getRandom();
		}
	}

	/**
	 * Gets the index into the string character set.
	 * 
	 * @return index
	 */
	private int getIndex() {

		return this.index;
	}

	/**
	 * Saves the index of the string character set.
	 * 
	 * @param index
	 *            into the string character set.
	 */
	private void saveIndex(final int index) {

		if (index >= this.seedData.getCharSet().length()) {
			throw new IllegalArgumentException("String index is out of bound");
		}
		this.index = index;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Actual return type of this method is Integer
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */

	private int getRandomLen() {

		if (this.seedData == null) {
			throw new IllegalStateException("String generator is unseeded");
		}
		if (this.seedData.isIncludeEmptyStrings()) {
			// Generate a random length from 0 - STRING_MAX_LEN
			return this.random.nextInt(this.seedData.getMaxStringLength());

		} else {
			// Generate a random length from 1 - STRING_MAX_LEN
			return (this.random.nextInt(this.seedData.getMaxStringLength()) + 1);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a random string value from the string character set.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getRandom()
	 */
	@Override
	public String getRandom() {

		StringBuilder strArgVal = null;
		final StringBuilder sb = new StringBuilder();
		if (this.seedData == null) {
			throw new IllegalStateException("String generator is unseeded.");
		}
		final StringBuilder strCharSet = new StringBuilder(
				this.seedData.getCharSet());

		final int len = getRandomLen();
		if (len == 0) {
			// System.out.println("Empty String!!!!!!!!");
			strArgVal = sb.append("");
			return strArgVal.toString();
		}

		final int charSetLen = strCharSet.length();
		// Get a random number for an index from 0 to max character set length -
		// 1
		final int index = this.random.nextInt(charSetLen - 1);
		if (index + len <= charSetLen) {
			strArgVal = sb.append(strCharSet, index, index + len);
		} else { // wrap around case
			strArgVal = sb.append(strCharSet, index, charSetLen);
			int tempLen = charSetLen - index;
			while (len - tempLen > charSetLen) {
				strArgVal.append(strCharSet, 0, charSetLen);
				tempLen += charSetLen;
			}
			strArgVal.append(strCharSet, 0, (index + len) % charSetLen);
		}
		// System.out.println(" str = " + strArgVal + " len = "
		// + strArgVal.length());
		return strArgVal.toString();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a sequential string value from the string character
	 * set.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getNext()
	 */
	@Override
	public String getNext() {

		StringBuilder strArgVal = null;
		final StringBuilder sb = new StringBuilder();
		if (this.seedData == null) {
			throw new IllegalStateException("String generator is unseeded.");
		}
		final StringBuilder strCharSet = new StringBuilder(
				this.seedData.getCharSet());

		final int index = getIndex();
		final int len = getRandomLen();
		if (len == 0) {
			// System.out.println("Empty String!!!!!!!!");
			strArgVal = sb.append("");
			return strArgVal.toString();

		}
		final int charSetLen = strCharSet.length();
		if (index + len < charSetLen) {
			strArgVal = sb.append(strCharSet, index, index + len);
			saveIndex(index + len);
		} else {
			if (index + len == charSetLen) {
				strArgVal = sb.append(strCharSet, index, charSetLen);
				saveIndex(0);
			} else { // wrap around case
				strArgVal = sb.append(strCharSet, index, charSetLen);
				strArgVal.append(strCharSet, 0, (index + len) % charSetLen);
				saveIndex((index + len) % charSetLen);
			}
		}
		// System.out.println(" str = " + strArgVal + " len = "
		// + strArgVal.length());
		return strArgVal.toString();
	}
}
