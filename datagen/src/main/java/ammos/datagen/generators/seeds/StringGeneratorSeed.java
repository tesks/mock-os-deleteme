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
 * This is the seed class for the StringGenerator. It contains all the data
 * necessary to initialize the generator.
 * 
 *
 */
public class StringGeneratorSeed implements ISeedData {
	/** Default character set constant for string characters */
	public static final String DEFAULT_CHAR_SET = "default";
	private boolean includeEmptyStrings = false;
	private boolean includeNullCharacter = false;
	private int maxLen;
	private TraversalType traversalType = TraversalType.RANDOM;
	private String characterSet;

	/** Default character set for string characters */
	/*
	 * MPCS-6768 - 2/26/14. Removed 0x5C (backslash character). Added 0x5D
	 * (right square bracket character)
	 */
	public static final String defaultCharacterSet = new String(new byte[] {
			(byte) 0x20, (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24,
			(byte) 0x25, (byte) 0x26, (byte) 0x27, (byte) 0x28, (byte) 0x29,
			(byte) 0x2A, (byte) 0x2B, (byte) 0x2C, (byte) 0x2E, (byte) 0x2F,
			(byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34,
			(byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39,
			(byte) 0x3A, (byte) 0x3B, (byte) 0x3C, (byte) 0x3E, (byte) 0x3F,
			(byte) 0x40, (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44,
			(byte) 0x45, (byte) 0x46, (byte) 0x47, (byte) 0x48, (byte) 0x49,
			(byte) 0x4A, (byte) 0x4B, (byte) 0x4C, (byte) 0x4D, (byte) 0x4E,
			(byte) 0x4F, (byte) 0x50, (byte) 0x51, (byte) 0x52, (byte) 0x53,
			(byte) 0x54, (byte) 0x55, (byte) 0x56, (byte) 0x57, (byte) 0x58,
			(byte) 0x59, (byte) 0x5A, (byte) 0x5B, (byte) 0x5D, (byte) 0x5E,
			(byte) 0x5F, (byte) 0x60, (byte) 0x61, (byte) 0x62, (byte) 0x63,
			(byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68,
			(byte) 0x69, (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6E,
			(byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
			(byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
			(byte) 0x79, (byte) 0x7A, (byte) 0x7B, (byte) 0x7C, (byte) 0x7E });

	/**
	 * Sets the string character set created by users.
	 * 
	 * @param string
	 *            Sequence of printable characters defined by users or a default
	 *            set of characters ranging from 0x20 to 0x7e.
	 * 
	 */
	public void setCharSet(final String string) {

		if (string.equals(DEFAULT_CHAR_SET)) {
			this.characterSet = defaultCharacterSet;
		} else {
			this.characterSet = string;
		}
	}

	/**
	 * Gets the max string length of strings created by the generator.
	 * 
	 * @return max string length
	 */
	public String getCharSet() {

		return this.characterSet;
	}

	/**
	 * 
	 * Indicates whether the run configuration stated that empty string should
	 * be included in the output of the data generator.
	 * 
	 * @return includeEmptyStrings true to include empty strings, false to not
	 */
	public boolean isIncludeEmptyStrings() {

		return this.includeEmptyStrings;
	}

	/**
	 * Sets the flag indicating whether the run configuration stated that empty
	 * string should be included in the output of the data generator.
	 * 
	 * @param includeEmptyStrings
	 *            true to include empty string, false to not
	 */
	public void setIncludeEmptyStrings(final boolean includeEmptyStrings) {

		this.includeEmptyStrings = includeEmptyStrings;
	}

	/**
	 * 
	 * Indicates whether the run configuration stated that null character should
	 * be included in the output of the data generator.
	 * 
	 * @return includeNullCharacter true to include null character, false to not
	 */
	public boolean isIncludeNullCharacter() {

		return this.includeNullCharacter;
	}

	/**
	 * Sets the flag indicating whether the run configuration stated that empty
	 * string should be included in the output of the data generator.
	 * 
	 * @param includeNullCharacter
	 *            true to include null character, false to not
	 */
	public void setIncludeNullCharacter(final boolean includeNullCharacter) {

		this.includeNullCharacter = includeNullCharacter;
	}

	/**
	 * Gets the max string length of strings created by the generator.
	 * 
	 * @return max string length
	 */
	public int getMaxStringLength() {

		return this.maxLen;
	}

	/**
	 * Sets the max string length for strings created by the generator.
	 * 
	 * @param maxLen
	 *            the maximum length of string.
	 */
	public void setMaxStringLength(final int maxLen) {

		if (!this.isIncludeEmptyStrings() && maxLen == 0) {
			throw new IllegalArgumentException("Unsupported Empty Strings: ");
		}
		this.maxLen = maxLen;
	}

	/**
	 * Gets the traversal type for string generation: RANDOM or SEQUENTIAL.
	 * 
	 * @return TraversalType enumeration value
	 */
	public TraversalType getTraversalType() {

		return this.traversalType;
	}

	/**
	 * Gets the traversal type for string generation: RANDOM or SEQUENTIAL.
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

}
