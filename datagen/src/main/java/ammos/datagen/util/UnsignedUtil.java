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
package ammos.datagen.util;

import java.math.BigInteger;

/**
 * This class contains static utility methods for manipulating unsigned
 * integers.
 * 
 *
 */
public final class UnsignedUtil {
	/**
	 * Formats the given byte as an unsigned number and returns a string.
	 * 
	 * @param val
	 *            byte value to format
	 * @return Unsigned string representation of the input value
	 */
	public static String formatAsUnsigned(final Byte val) {

		final short longerVal = (short) (val.byteValue() & 0xFF);
		return String.valueOf(longerVal);
	}

	/**
	 * Formats the given short as an unsigned number and returns a string.
	 * 
	 * @param val
	 *            short value to format
	 * @return Unsigned string representation of the input value
	 */
	public static String formatAsUnsigned(final Short val) {

		final int longerVal = (val.shortValue() & 0xFFFF);
		return String.valueOf(longerVal);
	}

	/**
	 * Formats the given integer as an unsigned number and returns a string.
	 * 
	 * @param val
	 *            integer value to format
	 * @return Unsigned string representation of the input value
	 */
	public static String formatAsUnsigned(final Integer val) {

		final long longerVal = (val.intValue() & 0xFFFFFFFFL);
		return String.valueOf(longerVal);
	}

	/**
	 * Formats the given long as an unsigned number and returns a string.
	 * 
	 * @param val
	 *            long value to format
	 * @return Unsigned string representation of the input value
	 */
	public static String formatAsUnsigned(final Long val) {

		BigInteger longerVal = BigInteger.valueOf(val);
		longerVal = longerVal.and(new BigInteger("FFFFFFFFFFFFFFFF", 16));

		return longerVal.toString();
	}
}
