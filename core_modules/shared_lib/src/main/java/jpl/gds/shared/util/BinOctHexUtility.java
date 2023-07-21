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
package jpl.gds.shared.util;


import jpl.gds.shared.exceptions.ExceptionTools;

/**
 * This class contains methods used by various components to manipulate binary,
 * octal, and hexadecimal numbers.
 * 
 */
public final class BinOctHexUtility {
	/** The value used to pad empty space in an array */
	private static final byte PADDING_VALUE = 0x55;

	/** Allowable binary values */
	private final static String[] binSymbols = { "0", "1" };

	/** Allowable octal values */
	private final static String[] octSymbols = { "0", "1", "2", "3", "4", "5",
			"6", "7" };

	/** Allowable hex values */
	private final static String[] hexSymbols = { "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

	private final static int BITS_PER_OCT_DIGIT = 3;

	private final static int BITS_PER_HEX_DIGIT = 4;

	/** The radix for binary numbers */
	public final static int BINARY_RADIX = 2;

	/** The radix for octal numbers */
	public final static int OCTAL_RADIX = 8;

	/** The radix for decimal numbers */
	public final static int DECIMAL_RADIX = 10;

	/** The radix for hexadecimal numbers */
	public final static int HEXADECIMAL_RADIX = 16;

	/** Binary string prefix, one */
	public static final String BINARY_STRING_PREFIX1 = "0b";

	/** Binary string prefix, two */
	public static final String BINARY_STRING_PREFIX2 = "0B";

	/** The regular expression for a binary string */
	private static final String BINARY_REGEXP = "((" + BINARY_STRING_PREFIX1
			+ ")|(" + BINARY_STRING_PREFIX2 + ")){0,1}[0-1]{0,}";

	/** Octal string prefix */
	public static final String OCTAL_PREFIX = "0";

	/** The regular expression for an octal string */
	private static final String OCTAL_REGEXP = "[0-7]{0,}";

	/** Hex string prefix, one */
	public static final String HEX_STRING_PREFIX1 = "0x";

	/** Hex string prefix, two */
	public static final String HEX_STRING_PREFIX2 = "0X";

	/** The regular expression for a hex string */
	private static final String HEX_REGEXP = "^(0x|0X)?[a-fA-F0-9]+$";

	private BinOctHexUtility(){
	}

	/**
	 * Takes the given input and returns a String containing the hex
	 * representation of the byte.
	 *
	 * @param b
	 *            byte the byte to be converted
	 *
	 * @return String the hex conversion of the byte (2 characters)
	 */
	public static String toHexFromByte(final byte b) {
		// need to and the shift result as java maintains the
		// sign bit and puts it back after the shift
		byte leftSymbol = (byte) ((b >>> BITS_PER_HEX_DIGIT) & 0x0f);
		byte rightSymbol = (byte) (b & 0x0f);

		return (hexSymbols[leftSymbol] + hexSymbols[rightSymbol]);
	}

	/**
	 * Takes the given input characters containing two hex symbols and returns
	 * the byte value which those symbols represent
	 *
	 * @param leftFourBitsHexSymbol
	 *            char the hex symbol representing the four leftmost bits of the
	 *            byte
	 * @param rightFourBitsHexSymbol
	 *            char the hex symbol representing the four rightmost bits of
	 *            the byte
	 *
	 * @return byte the byte value represented by the hex characters
	 *
	 *
	 */
	public static byte toByteFromHexChars(final char leftFourBitsHexSymbol,
			final char rightFourBitsHexSymbol) {
		byte wholeByte = 0;
		byte leftByte = 0;
		byte rightByte = 0;

		try {
			leftByte = Byte.parseByte("" + leftFourBitsHexSymbol, 16);
			rightByte = Byte.parseByte("" + rightFourBitsHexSymbol, 16);

			wholeByte = leftByte;
			wholeByte = (byte) (wholeByte << BITS_PER_HEX_DIGIT);
			wholeByte = (byte) (wholeByte | rightByte);
		} catch (java.lang.NumberFormatException nfe) {
			throw new IllegalArgumentException("unable to parse " + "0x"
					+ leftFourBitsHexSymbol + rightFourBitsHexSymbol
					+ " to a valid byte value - " + nfe.getMessage() + ".");
		}

		return wholeByte;
	}

	/**
	 * Takes the given input and returns a String containing the hex
	 * representation of the bytes.
	 *
	 * @param bytes
	 *            byte[] the bytes to be converted
	 *
	 * @return String the hex conversion of the bytes (2 * bytes.length
	 *         characters), and empty String if a null array is passed
	 *
	 */
	public static String toHexFromBytes(final byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return ("");
		}

		// there are 2 hex digits per byte
		StringBuilder hexBuffer = new StringBuilder(bytes.length * 2);

		// for each byte, convert it to hex and append it to the buffer
		for (int i = 0; i < bytes.length; i++) {
			hexBuffer.append(toHexFromByte(bytes[i]));
		}

		return (hexBuffer.toString());
	}

	/**
	 * Takes the given input and returns a String containing the hex
	 * representation of the bytes.
	 *
	 * @param bytes
	 *            byte[] the bytes to be converted
	 * @param offset
	 *            starting offset into the byte array
	 * @param length
	 *            number of bytes to convert
	 *
	 * @return String the hex conversion of the bytes (2 * length characters),
	 *         and empty String if a null array is passed
	 *
	 */
	public static String toHexFromBytes(final byte[] bytes, int offset,
			int length) {
		if (bytes == null || bytes.length == 0) {
			return ("");
		}

		if (offset > bytes.length || offset + length > bytes.length) {
			throw new IllegalArgumentException(
					"Offset/length combination exceeds length of input byte array");
		}

		// there are 2 hex digits per byte
		StringBuilder hexBuffer = new StringBuilder(length * 2);

		// for each byte, convert it to hex and append it to the buffer
		for (int i = offset; i < offset + length; i++) {
			hexBuffer.append(toHexFromByte(bytes[i]));
		}

		return (hexBuffer.toString());
	}

	/**
	 * Takes the given array of bytes and returns a String containing the octal
	 * representation of the bytes.
	 *
	 * @param bytes
	 *            The bytes to be converterd (total BITS must be a multiple of
	 *            3)
	 * @return The octal String if successful or null if there was an error
	 */
	public static String toOctFromBytes(final byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return ("");
		}

		// make sure the string can actually be converted to octal...bits must
		// be a multiple of 3
		byte[] newBytes = null;
		int mod = (bytes.length * 8) % BITS_PER_OCT_DIGIT;
		int copyIndex = 0;
		if (mod == 1) {
			newBytes = new byte[bytes.length + 2];
			newBytes[0] = 0x00;
			newBytes[1] = 0x00;
			copyIndex = 2;
		} else if (mod == 2) {
			newBytes = new byte[bytes.length + 1];
			newBytes[0] = 0x00;
			copyIndex = 1;
		} else {
			newBytes = new byte[bytes.length];
			copyIndex = 0;
		}
		System.arraycopy(bytes, 0, newBytes, copyIndex, bytes.length);

		if (newBytes.length == 0) {
			return ("");
		}

		StringBuilder octBuffer = new StringBuilder();
		int byteArrayIndex = 0;
		int currentByteIndex = 0;

		// This parsing can get a little nasty because octal numbers don't split
		// evenly into newBytes, so certain
		// octal digits are the end of one byte combined with the beginning of
		// the next. This is why we have to
		// keep track of the current byte and the next byte.
		//
		// Parsing newBytes into octal falls into a pattern. Every three
		// newBytes the pattern repeats. The 8 scenarios in
		// the switch statement below represents the 8 possible cases that
		// occur. The cases will ALWAYS occur in
		// this order: 0, 3, 6, 1, 4, 7, 2, 5 and this pattern will repeat
		// indefinitely.
		while (byteArrayIndex < newBytes.length) {
			byte symbol = 0x00;

			// this is the current byte in the array that we're working on
			byte currentByte = newBytes[byteArrayIndex];

			// get the next byte only if it's not past the end of the byte array
			byte nextByte = 0x00;
			if (byteArrayIndex != (newBytes.length - 1)) {
				nextByte = newBytes[byteArrayIndex + 1];
			}

			// When talking about the bits in a byte, bit 0 is the MSB and
			// bit 7 is the LSB. So for instance, bits 0-2 of current byte
			// means the 3 uppermost bits of the byte. Also, I always make
			// that I shift the three bits being grabbed all the way to the
			// right
			// and fill the rest of the number with zeroes so I can use an octal
			// digit.
			switch (currentByteIndex) {
			// get bits 0-2 of current byte (use mask = 11100000)
			case (0):

				symbol = (byte) ((currentByte & 0xe0) >>> 5);

				break;

			// get bits 1-3 of current byte (use mask = 01110000)
			case (1):

				symbol = (byte) ((currentByte & 0x70) >>> 4);

				break;

			// get bits 2-4 of current byte (use mask = 00111000)
			case (2):

				symbol = (byte) ((currentByte & 0x38) >>> 3);

				break;

			// get bits 3-5 of current byte (use mask = 00011100)
			case (3):

				symbol = (byte) ((currentByte & 0x1c) >>> 2);

				break;

			// get bits 4-6 of current byte (use mask = 00001110)
			case (4):

				symbol = (byte) ((currentByte & 0x0e) >>> 1);

				break;

			// get bits 5-7 of current byte (use mask = 00000111)
			case (5):

				symbol = (byte) (currentByte & 0x07);

				break;

			// get bits 6-7 of current byte (use mask = 00000011) and
			// get bit 0 of next byte (use mask = 10000000) and then
			// add them together
			case (6):

				byte twoUpperBits = (byte) ((currentByte & 0x03) << 1);
				byte oneLowerBit = (byte) ((nextByte & 0x80) >>> 7);

				symbol = (byte) ((twoUpperBits + oneLowerBit) & 0x07);

				break;

			// get bit 7 of current byte (use mask = 00000001) and
			// get bits 0-1 of next byte (use mask = 11000000) and then
			// add them together
			case (7):

				byte oneUpperBit = (byte) ((currentByte & 0x01) << 2);
				byte twoLowerBits = (byte) ((nextByte & 0xc0) >>> 6);

				symbol = (byte) ((oneUpperBit + twoLowerBits) & 0x07);

				break;

			default:
				return (null);
			}

			// an index of 5, 6, or 7 means that we have to move on to
			// the next byte
			if (currentByteIndex > 4) {
				byteArrayIndex++;
			}

			// append the current octal symbol to the buffer
			octBuffer.append(octSymbols[symbol]);

			// we grab three bits from the byte each time so
			// we keep incrementing this by three and keeping it in
			// the range of 0-7 (the indexes of the bits in a byte)
			currentByteIndex = (currentByteIndex + BITS_PER_OCT_DIGIT) % 8;
		}

		return (octBuffer.toString());
	}

	/**
	 * Takes the given input characters containing two hex symbols and returns
	 * the byte value which those symbols represent
	 *
	 * @param leftFourBitsHexSymbol
	 *            char the hex symbol representing the four leftmost bits of the
	 *            byte
	 * @param rightFourBitsHexSymbol
	 *            char the hex symbol representing the four rightmost bits of
	 *            the byte
	 *
	 * @return byte the byte value represented by the hex characters
	 *
	 */
	public static byte toByteFromHex(final char leftFourBitsHexSymbol,
			final char rightFourBitsHexSymbol) {

		// improved hex char detection
		if (Character.digit(leftFourBitsHexSymbol, HEXADECIMAL_RADIX) < 0 ||
				Character.digit(rightFourBitsHexSymbol, HEXADECIMAL_RADIX) < 0) {
			throw new IllegalArgumentException("Invalid hex symbols specified");
		}

		byte wholeByte = 0x00;
		byte leftByte = 0x00;
		byte rightByte = 0x00;

		try {
			// get the two parts of the byte
			leftByte = Byte.parseByte(String.valueOf(leftFourBitsHexSymbol), HEXADECIMAL_RADIX);
			rightByte = Byte.parseByte(String.valueOf(rightFourBitsHexSymbol), HEXADECIMAL_RADIX);

			wholeByte = (byte) (leftByte << BITS_PER_HEX_DIGIT);
			wholeByte = (byte) (wholeByte | rightByte);
		} catch (java.lang.NumberFormatException nfe) {
			throw new IllegalArgumentException("unable to parse " + "0x"
					+ leftFourBitsHexSymbol + rightFourBitsHexSymbol
					+ " to a valid byte value - " + nfe.getMessage() + ".");
		}

		return (wholeByte);
	}

	/**
	 * Takes the given input String containing hex symbols and returns the byte
	 * value which those symbols represent
	 *
	 * @param hexSymbols
	 *            String the hex symbols representing the bytes to be
	 *            constructed
	 *
	 * @return byte[] the byte values represented by the hex characters
	 *
	 */
	public static byte[] toBytesFromHex(String hexSymbols) {
		byte[] bytes = null;
		if (hexSymbols == null || hexSymbols.trim().length() == 0) {
			return (new byte[0]);
		} else if (!isValidHex(hexSymbols)) {
			throw new IllegalArgumentException("Invalid hex string specified: "
					+ hexSymbols);
		}

		//use string builder to optimize string concat
		StringBuilder hexSymbolsBuilder = new StringBuilder(stripHexPrefix(hexSymbols));
		while ((hexSymbolsBuilder.length() % 2) != 0) {
			hexSymbolsBuilder.insert(0, "0");
		}
		hexSymbols = hexSymbolsBuilder.toString();

		// X hex symbols generate X/2 bytes
		bytes = new byte[hexSymbols.length() / 2];

		// loop through all the hex symbols
		for (int i = 0; i < hexSymbols.length(); i = i + 2) {
			bytes[i / 2] = toByteFromHex(hexSymbols.charAt(i),
					hexSymbols.charAt(i + 1));
		}

		return (bytes);
	}

	/**
	 * Given a String of hex digits, convert them to octal representation
	 *
	 * @param hexSymbols
	 *            A string of hex digits
	 * @return The corresponding string of octal digits
	 */
	public static String toOctFromHex(final String hexSymbols) {
		return (toOctFromBin(toBinFromHex(hexSymbols)));
	}

	/**
	 * Given a String of octal digits, convert them to hex representation
	 *
	 * @param octSymbols
	 *            A string of octal digits
	 * @return The corresponding string of hex digits
	 */
	public static String toHexFromOct(final String octSymbols) {
		return (toHexFromBin(toBinFromOct(octSymbols)));
	}

	/**
	 * Given a String of octal digits, return the corresponding byte array
	 *
	 * @param octSymbols
	 *            A string of octal digits
	 * @return The corresponding byte array
	 */
	public static byte[] toBytesFromOct(String octSymbols) {
		if (octSymbols == null || octSymbols.trim().length() == 0) {
			return (new byte[0]);
		} else if (isValidOct(octSymbols) == false) {
			throw new IllegalArgumentException(
					"Invalid octal string specified: " + octSymbols);
		}

		while (((octSymbols.length() * BITS_PER_OCT_DIGIT) % 8) != 0) {
			octSymbols = "0" + octSymbols;
		}

		// there are 3 bits per octal symbol and 8 bits in a byte
		int numBytes = (octSymbols.length() * BITS_PER_OCT_DIGIT) / 8;
		byte[] bytes = new byte[numBytes];

		// the index in the array of output bytes
		int byteArrayIndex = 0;

		// the index from 0-7 within the current byte
		int currentByteIndex = 0;

		// the current byte being output
		byte currentByte = 0x00;

		// the next byte being used
		byte nextByte = 0x00;

		// This parsing can get a little nasty because octal numbers don't split
		// evenly into bytes, so we
		// may have to set bits in two different bytes at the same time. This is
		// why we have to
		// keep track of the current byte and the next byte.
		//
		// Parsing octal into bytes falls into a pattern. Every three bytes the
		// pattern repeats. The 8 scenarios in
		// the switch statement below represents the 8 possible cases that
		// occur.

		for (int i = 0; i < octSymbols.length(); i++) {
			// transform the current octal digit into a numeric value
			int octDigit = (Integer.parseInt(octSymbols.substring(i, i + 1))) & 0x07;

			// When talking about the bits in a byte, bit 0 is the MSB and
			// bit 7 is the LSB. So for instance, bits 0-2 of current byte
			// means the 3 uppermost bits of the byte.
			switch (currentByteIndex) {
			// set bits 0-2 of current byte
			case (0):

				currentByte = (byte) (currentByte | (octDigit << 5));

				break;

			// set bits 1-3 of current byte
			case (1):

				currentByte = (byte) (currentByte | (octDigit << 4));

				break;

			// set bits 2-4 of current byte
			case (2):

				currentByte = (byte) (currentByte | (octDigit << 3));

				break;

			// set bits 3-5 of current byte
			case (3):

				currentByte = (byte) (currentByte | (octDigit << 2));

				break;

			// set bits 4-6 of current byte
			case (4):

				currentByte = (byte) (currentByte | (octDigit << 1));

				break;

			// set bits 5-7 of current byte
			case (5):

				currentByte = (byte) (currentByte | octDigit);

				break;

			// set bits 6-7 of current byte and
			// set bit 0 of next byte
			case (6):

				currentByte = (byte) (currentByte | ((octDigit & 0x06) >>> 1));
				nextByte = (byte) (nextByte | ((octDigit & 0x01) << 7));

				break;

			// set bit 7 of current byte and
			// set bits 0-1 of next byte
			case (7):

				currentByte = (byte) (currentByte | ((octDigit & 0x04) >>> 2));
				nextByte = (byte) (nextByte | ((octDigit & 0x03) << 6));

				break;

			default:
				return (null);
			}

			// if the index is 5, 6, or 7 we've moved onto the next byte
			if (currentByteIndex > 4) {
				bytes[byteArrayIndex] = currentByte;
				currentByte = nextByte;
				nextByte = 0x00;
				byteArrayIndex++;
			}

			// octal digits are 3 bits so we always increment by 3
			currentByteIndex = (currentByteIndex + BITS_PER_OCT_DIGIT) % 8;
		}

		return (bytes);
	}

	/**
	 * Creates a new array from the input, formatted to be of the given array
	 * length, with bytes appended to the original bytes as padding to reach the
	 * given array length, using the default fill byte specified in
	 * PADDING_VALUE
	 *
	 * @param originalBytes
	 *            byte[] the bytes to be formatted into a padded array
	 * @param standardArrayLength
	 *            int the number of bytes required to be in the returned array
	 * @return byte[] a new array containing the original bytes, padded with 0
	 *         bytes to reach the given array length
	 *
	 */
	public static byte[] createPaddedArray(final byte[] originalBytes,
			final int standardArrayLength) {
		byte[] paddedBytes = null;
		if (originalBytes == null) {
			throw new IllegalArgumentException("original bytes parameter is null");
		} else if (originalBytes.length > standardArrayLength) {
			throw new IllegalArgumentException(
					"number of bytes in the original array is greater than the "
							+ "given standard array length");
		} else if (standardArrayLength < 0) {
			throw new IllegalArgumentException(
					"invalid standard array length passed: "
							+ standardArrayLength);
		} else {
			paddedBytes = new byte[standardArrayLength];
			// copy original bytes over
			System.arraycopy(originalBytes, 0, paddedBytes, 0,
					originalBytes.length);

			for (int i = originalBytes.length; i < paddedBytes.length; i++) {
				paddedBytes[i] = PADDING_VALUE;
			}
		}

		return paddedBytes;
	}

	/**
	 * Creates a new array from the input, formatted to be of the given array
	 * length, with 0 filled bytes appended to the original bytes as padding to
	 * reach the given array length.
	 *
	 * @param originalHex
	 *            String hex symbols representing the bytes to be formatted into
	 *            a padded array
	 * @param standardArrayLength
	 *            int the number of bytes required to be in the returned array
	 * @param fillByte
	 *            byte the byte to be used as the fill byte(s)
	 *
	 * @return byte[] a new array containing the original bytes, padded with 0
	 *         bytes to reach the given array length
	 *
	 *
	 */
	public static byte[] createPaddedArray(final String originalHex,
			final int standardArrayLength, final byte fillByte) {
		return createPaddedArray(toBytesFromHex(originalHex),
				standardArrayLength, fillByte);
	}

	/**
	 * Creates a new array from the input, formatted to be of the given array
	 * length, with bytes appended to the original bytes as padding to reach the
	 * given array length, using the given fill byte.
	 *
	 * @param originalBytes
	 *            byte[] the bytes to be formatted into a padded array
	 * @param standardArrayLength
	 *            int the number of bytes required to be in the returned array
	 * @param fillByte
	 *            byte the byte to be used as the fill byte(s)
	 *
	 * @return byte[] a new array containing the original bytes, padded with 0
	 *         bytes to reach the given array length
	 *
	 *
	 */
	public static byte[] createPaddedArray(final byte[] originalBytes,
			final int standardArrayLength, final byte fillByte) {
		byte[] paddedBytes = null;
		if (originalBytes == null) {
			throw new IllegalArgumentException("original bytes parameter is null");
		} else if (originalBytes.length > standardArrayLength) {
			throw new IllegalArgumentException(
					"number of bytes in the original array is greater than the "
							+ "given standard array length");
		} else if (standardArrayLength < 0) {
			throw new IllegalArgumentException(
					"invalid standard array length passed: "
							+ standardArrayLength);
		} else {
			paddedBytes = new byte[standardArrayLength];
			// copy original bytes over
			System.arraycopy(originalBytes, 0, paddedBytes, 0,
					originalBytes.length);

			for (int i = originalBytes.length; i < paddedBytes.length; i++) {
				paddedBytes[i] = fillByte;
			}
		}

		return paddedBytes;
	}

	/**
	 * Attempts to return the index of the first occurance of the sequence of
	 * bytes in the target array within the searched array.
	 *
	 * @param searched
	 *            byte[] the byte array in which to search for a match
	 * @param target
	 *            byte[] the byte array to be searched for
	 * @param startIndex
	 *            int the index of the array to be searched at which to begin
	 *            the search for the target
	 *
	 * @return int the index in the array at which the first occurance of the
	 *         target bytes is encountered, -1 if the target bytes are not
	 *         encountered in the searched array
	 *
	 *
	 */
	public static int indexOf(final byte[] searched, final byte[] target,
			final int startIndex) {
		int index = -1;

		if (searched == null) {
			throw new IllegalArgumentException("Array to be searched is null");
		} else if (target == null) {
			throw new IllegalArgumentException("Target array is null");
		} else if (target.length > searched.length) {
			throw new IllegalArgumentException("Target array length is "
					+ "greater than the searched " + "array length");
		} else if ((startIndex < 0) || (startIndex > (searched.length - 1))) {
			throw new IllegalArgumentException("Start index " + startIndex
					+ " is invalid");
		} else {
			boolean found = false;
			for (int i = startIndex; ((i < (searched.length - (target.length - 1))) && (found == false)); i++) {
				boolean match = true;
				for (int j = 0; j < target.length && match; j++) {
					match = (searched[i + j] == target[j]);
				}

				if (match == true) {
					found = true;
					index = i;
				}
			}
		}

		return index;
	}

	/**
	 * Attempts to return the index of the first occurance of the sequence of
	 * bytes in the target array within the searched array, beginning the search
	 * at byte 0 of the searched array.
	 *
	 * @param searched
	 *            byte[] the byte array in which to search for a match
	 * @param target
	 *            byte[] the byte array to be searched for
	 *
	 * @return int the index in the array at which the first occurance of the
	 *         target bytes is encountered, -1 if the target bytes are not
	 *         encountered in the searched array
	 *
	 *
	 */
	public static int indexOf(final byte[] searched, final byte[] target) {
		return indexOf(searched, target, 0);
	}

	/**
	 * Given a string of binary symbols, convert it to an array of bytes
	 *
	 * @param binSymbols
	 *            The binary string to convert
	 * @return The byte array corresponding to the input string
	 */
	public static byte[] toBytesFromBin(String binSymbols) {
		if (binSymbols == null || binSymbols.trim().length() == 0) {
			return (new byte[0]);
		} else if (isValidBin(binSymbols) == false) {
			throw new IllegalArgumentException("Invalid bin string specified");
		}

		binSymbols = stripBinaryPrefix(binSymbols);
		while ((binSymbols.length() % 8) != 0) {
			binSymbols = "0" + binSymbols;
		}

		// 8 bits in a byte
		int numBytes = binSymbols.length() / 8;
		byte[] bytes = new byte[numBytes];

		// loop through all the input symbols
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = toByteFromBin(binSymbols.substring(i * 8, (i * 8) + 8));
		}

		return (bytes);
	}

	/**
	 * Get a binary string from the given array of bytes
	 *
	 * @param bytes
	 *            The bytes to convert
	 * @return The binary string corresponding to the input byte array
	 */
	public static String toBinFromBytes(final byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			return ("");
		}

		StringBuilder binBuffer = new StringBuilder(bytes.length * 8);
		for (int i = 0; i < bytes.length; i++) {
			binBuffer.append(toBinFromByte(bytes[i]));
		}

		return (binBuffer.toString());
	}

	/**
	 * Transform a byte into a bitstring (of length 8)
	 *
	 * @param b
	 *            The byte to convert
	 * @return The binary String representing the input byte
	 */
	public static String toBinFromByte(final byte b) {
		StringBuilder binBuffer = new StringBuilder(8);

		// We need to read each of the 8 bits out of the
		// input byte and append them one by one to the
		// output bit string
		binBuffer.append(binSymbols[((b & 0x80) >>> 7)]);
		binBuffer.append(binSymbols[((b & 0x40) >>> 6)]);
		binBuffer.append(binSymbols[((b & 0x20) >>> 5)]);
		binBuffer.append(binSymbols[((b & 0x10) >>> 4)]);
		binBuffer.append(binSymbols[((b & 0x08) >>> 3)]);
		binBuffer.append(binSymbols[((b & 0x04) >>> 2)]);
		binBuffer.append(binSymbols[((b & 0x02) >>> 1)]);
		binBuffer.append(binSymbols[(b & 0x01)]);

		return (binBuffer.toString());
	}

	/**
	 * Transform a string of BITS_PER_HEX_DIGIT bits into a hex char.
	 *
	 * @param bin
	 *            A string of binary symbols
	 * @return The char representing the input bit string
	 */
	public static char toHexCharFromBin(final String bin) {
		String bits = stripBinaryPrefix(bin);
		while (bits.length() < BITS_PER_HEX_DIGIT) {
			bits = "0" + bits;
		}

		if (bits.length() > BITS_PER_HEX_DIGIT) {
			throw new IllegalArgumentException("Input bit string \"" + bin
					+ "\" is too long to be a hexadecimal character.");
		}

		int value = Integer.parseInt(bits, BINARY_RADIX);
		return (hexSymbols[value].charAt(0));
	}

	/**
	 * Transform a hex char into a string of BITS_PER_HEX_DIGIT bits.
	 *
	 * @param hex
	 *            A hex character
	 *
	 * @return Bit string
	 */
	public static String toBinFromHexChar(final char hex) {
		switch (hex) {
		case '0':
			return ("0000");
		case '1':
			return ("0001");
		case '2':
			return ("0010");
		case '3':
			return ("0011");
		case '4':
			return ("0100");
		case '5':
			return ("0101");
		case '6':
			return ("0110");
		case '7':
			return ("0111");
		case '8':
			return ("1000");
		case '9':
			return ("1001");
		case 'a':
		case 'A':
			return ("1010");
		case 'b':
		case 'B':
			return ("1011");
		case 'c':
		case 'C':
			return ("1100");
		case 'd':
		case 'D':
			return ("1101");
		case 'e':
		case 'E':
			return ("1110");
		case 'f':
		case 'F':
			return ("1111");
		default:
			throw new IllegalArgumentException("The input character \'" + hex
					+ "\'is not a valid hexadecimal character.");
		}
	}

	/**
	 * Transform an octal char into a string of BITS_PER_OCT_DIGIT bits.
	 *
	 * @param oct
	 *            An octal character
	 *
	 * @return Bit string
	 */
	public static String toBinFromOctChar(final char oct) {
		switch (oct) {
		case '0':
			return ("000");
		case '1':
			return ("001");
		case '2':
			return ("010");
		case '3':
			return ("011");
		case '4':
			return ("100");
		case '5':
			return ("101");
		case '6':
			return ("110");
		case '7':
			return ("111");
		default:
			throw new IllegalArgumentException("The input character \'" + oct
					+ "\'is not a valid octal character.");
		}
	}

	/**
	 * Transform a string of BITS_PER_OCT_DIGIT bits into an octal char.
	 *
	 * @param bin
	 *            A string of binary symbols
	 * @return The char representing the input bit string
	 */
	public static char toOctCharFromBin(final String bin) {
		String bits = stripBinaryPrefix(bin);
		while (bits.length() < BITS_PER_OCT_DIGIT) {
			bits = "0" + bits;
		}

		if (bits.length() > BITS_PER_OCT_DIGIT) {
			throw new IllegalArgumentException("Input bit string \"" + bin
					+ "\" is too long to be an octal character.");
		}

		int value = Integer.parseInt(bits, BINARY_RADIX);
		return (octSymbols[value].charAt(0));
	}

	/**
	 * Transform a string of 8 bits into a byte
	 *
	 * @param binSymbols
	 *            A string of binary symbols of length 8
	 * @return The byte representing the input bit string
	 */
	public static byte toByteFromBin(String binSymbols) {
		if (isValidBin(binSymbols) == false) {
			throw new IllegalArgumentException(
					"Illegal characters in bin string");
		}

		binSymbols = stripBinaryPrefix(binSymbols);

		if (binSymbols.length() > 8) {
			throw new IllegalArgumentException(
					"More than 8 bits in input bit string, cannot convert to a single byte");
		}

		while (binSymbols.length() != 8) {
			binSymbols = "0" + binSymbols;
		}

		// make a corresponding bit out of each symbol in the input string
		//
		// we make a single bit by reading in the symbol, shifting it to the
		// correct place
		// in the byte and zeroing the rest of the byte. If we add all 8 of
		// these bytes together
		// we'll get the correct final byte value
		//
		// bit 0 is MSB, bit 7 is LSB
		byte bit0 = (byte) (((Integer.parseInt(binSymbols.substring(0, 1))) & 0x01) << 7);
		byte bit1 = (byte) (((Integer.parseInt(binSymbols.substring(1, 2))) & 0x01) << 6);
		byte bit2 = (byte) (((Integer.parseInt(binSymbols.substring(2, 3))) & 0x01) << 5);
		byte bit3 = (byte) (((Integer.parseInt(binSymbols.substring(3, 4))) & 0x01) << 4);
		byte bit4 = (byte) (((Integer.parseInt(binSymbols.substring(4, 5))) & 0x01) << 3);
		byte bit5 = (byte) (((Integer.parseInt(binSymbols.substring(5, 6))) & 0x01) << 2);
		byte bit6 = (byte) (((Integer.parseInt(binSymbols.substring(6, 7))) & 0x01) << 1);
		byte bit7 = (byte) ((Integer.parseInt(binSymbols.substring(7, 8))) & 0x01);

		return ((byte) (bit0 + bit1 + bit2 + bit3 + bit4 + bit5 + bit6 + bit7));
	}

	/**
	 * Transform a string of hex symbols to a string of binary symbols
	 *
	 * @param hexSymbols
	 *            The hex symbol string to transform
	 * @return The binary string that corresponds to input hex string
	 */
	public static String toBinFromHex(final String hexSymbols) {
		String hex = stripHexPrefix(hexSymbols);

		StringBuilder bits = new StringBuilder(hex.length()
				* BITS_PER_HEX_DIGIT);
		for (int i = 0; i < hex.length(); i++) {
			bits.append(toBinFromHexChar(hex.charAt(i)));
		}

		return (bits.toString());
	}

	/**
	 * Transform a string of octal symbols to a string of binary symbols
	 *
	 * @param octSymbols
	 *            The oct symbol string to transform
	 * @return The binary string that corresponds to the input octal string
	 */
	public static String toBinFromOct(final String octSymbols) {
		StringBuilder bits = new StringBuilder(octSymbols.length()
				* BITS_PER_OCT_DIGIT);
		for (int i = 0; i < octSymbols.length(); i++) {
			bits.append(toBinFromOctChar(octSymbols.charAt(i)));
		}

		return (bits.toString());
	}

	/**
	 * Transform a string of binary symbols to a string of hex symbols
	 *
	 * @param binSymbols
	 *            The binary symbol string to transform
	 * @return The hex string corresponding to the input binary string
	 */
	public static String toHexFromBin(final String binSymbols) {
		String bits = stripBinaryPrefix(binSymbols);

		while ((bits.length() % BITS_PER_HEX_DIGIT) != 0) {
			bits = "0" + bits;
		}

		StringBuilder hex = new StringBuilder(bits.length()
				/ BITS_PER_HEX_DIGIT);
		for (int i = 0; i < bits.length(); i += BITS_PER_HEX_DIGIT) {
			String bitsToAdd = null;
			if ((i + BITS_PER_HEX_DIGIT) < bits.length()) {
				bitsToAdd = bits.substring(i, i + BITS_PER_HEX_DIGIT);
			} else {
				bitsToAdd = bits.substring(i);
			}
			hex.append(toHexCharFromBin(bitsToAdd));
		}

		return (hex.toString());
	}

	/**
	 * Transform a string of binary symbols to a string of octal symbols
	 *
	 * @param binSymbols
	 *            The binary symbol string to transform
	 * @return The octal string corresponding to the binary string
	 */
	public static String toOctFromBin(final String binSymbols) {
		String bits = stripBinaryPrefix(binSymbols);

		while ((bits.length() % BITS_PER_OCT_DIGIT) != 0) {
			bits = "0" + bits;
		}

		StringBuilder oct = new StringBuilder(bits.length()
				/ BITS_PER_OCT_DIGIT);
		for (int i = 0; i < bits.length(); i += BITS_PER_OCT_DIGIT) {
			String bitsToAdd = null;
			if ((i + BITS_PER_OCT_DIGIT) < bits.length()) {
				bitsToAdd = bits.substring(i, i + BITS_PER_OCT_DIGIT);
			} else {
				bitsToAdd = bits.substring(i);
			}
			oct.append(toOctCharFromBin(bitsToAdd));
		}

		return (oct.toString());
	}

	/**
	 * Check for hex string.
	 *
	 * @param hexSymbols
	 *            String to check
	 *
	 * @return True if hex
	 */
	public static boolean isValidHex(final String hexSymbols) {
		return (hexSymbols.matches(HEX_REGEXP));
	}

	/**
	 * Check for octal string.
	 *
	 * @param octSymbols
	 *            String to check
	 *
	 * @return True if octal
	 */
	public static boolean isValidOct(final String octSymbols) {
		return (octSymbols.matches(OCTAL_REGEXP));
	}

	/**
	 * Check for binary string.
	 *
	 * @param binSymbols
	 *            String to check
	 *
	 * @return True if binary
	 */
	public static boolean isValidBin(final String binSymbols) {
		return (binSymbols.matches(BINARY_REGEXP));
	}

	/**
	 * Helper function to check if there is a binary or hex prefix
	 *
	 * @param input String to check
	 *
	 * @return true if input has hex or binary prefix; false otherwise
	 */
	public static boolean hasHexOrBinPrefix(final String input) {
		return hasHexPrefix(input) || hasBinaryPrefix(input);
	}

	/**
	 * Format bytes as a series of lines..
	 *
	 * @param input
	 *            byte array
	 *
	 * @return Formatted string
	 */
	public static String formatBytes(final byte[] input) {
		String byteString = toHexFromBytes(input);

		StringBuilder buf = new StringBuilder(byteString.length());

		int spc = 0;
		int lnc = 0;
		for (int index = 0; index < byteString.length(); index++) {
			buf.append(byteString.charAt(index));
			spc++;
			lnc++;

			if (spc >= 4) {
				buf.append(" ");
				spc = 0;
			}

			if (lnc >= 32) {
				buf.append("\n");
				lnc = 0;
			}
		}

		return buf.toString();
	}

	/**
	 * Add spaces to hex string.
	 *
	 * @param hexString
	 *            String to modify
	 *
	 * @return Modified string
	 */
	public static String spaceHexString(String hexString) {
		hexString = stripHexPrefix(hexString);

		int i = 0;
		StringBuilder buf = new StringBuilder(hexString.length() * 2);
		for (i = 0; i < hexString.length(); i += BITS_PER_HEX_DIGIT) {
			if (i + BITS_PER_HEX_DIGIT >= hexString.length()) {
				break;
			}

			buf.append(hexString.substring(i, i + BITS_PER_HEX_DIGIT));
			buf.append(" ");
		}
		buf.append(hexString.substring(i));

		return (buf.toString());
	}

	/**
	 * Remove binary prefix.
	 *
	 * @param binSymbols
	 *            String to strip
	 *
	 * @return Stripped string
	 */
	public static String stripBinaryPrefix(String binSymbols) {
		String tempSymbols = binSymbols;
		if (tempSymbols.startsWith(BINARY_STRING_PREFIX1)) {
			tempSymbols = tempSymbols.substring(BINARY_STRING_PREFIX1.length());
		} else if (binSymbols.startsWith(BINARY_STRING_PREFIX2)) {
			tempSymbols = tempSymbols.substring(BINARY_STRING_PREFIX2.length());
		}

		return (tempSymbols);
	}

	/**
	 * Check for binary prefix.
	 *
	 * @param binSymbols
	 *            String to check
	 *
	 * @return True if has prefix
	 */
	public static boolean hasBinaryPrefix(final String binSymbols) {
		return (binSymbols.startsWith(BINARY_STRING_PREFIX1) || binSymbols
				.startsWith(BINARY_STRING_PREFIX2));
	}

	/**
	 * Remove hex prefix.
	 *
	 * @param hexSymbols
	 *            String to strip
	 *
	 * @return Stripped string
	 */
	public static String stripHexPrefix(String hexSymbols) {
		String tempSymbols = hexSymbols;
		if (tempSymbols.startsWith(HEX_STRING_PREFIX1)) {
			tempSymbols = tempSymbols.substring(HEX_STRING_PREFIX1.length());
		} else if (tempSymbols.startsWith(HEX_STRING_PREFIX2)) {
			tempSymbols = tempSymbols.substring(HEX_STRING_PREFIX2.length());
		}

		return (tempSymbols);
	}

	/**
	 * Check for hex prefix.
	 *
	 * @param hexSymbols
	 *            String to check
	 *
	 * @return True if has prefix
	 */
	public static boolean hasHexPrefix(final String hexSymbols) {
		return (hexSymbols.startsWith(HEX_STRING_PREFIX1) || hexSymbols
				.startsWith(HEX_STRING_PREFIX2));
	}

	/**
	 * Format hex string.
	 *
	 * @param input
	 *            String to format
	 * @param charsPerLine
	 *            Maximum line size
	 *
	 * @return Formatted string
	 */
	public static String formatHexString(final String input,
			final int charsPerLine) {
		StringBuilder buf = new StringBuilder(input.length() * 2);

		int index = 0;
		while ((index + charsPerLine) < input.length()) {
			String str = input.substring(index, index + charsPerLine);
			buf.append(spaceHexString(str));
			buf.append("\n");
			index += charsPerLine;
		}

		String str = input.substring(index);
		buf.append(spaceHexString(str));

		return (buf.toString());
	}

	/**
	 * Given a byte array, shifts the bits to the right (toward
	 * least-significant bit) by one. The resulting most-significant bit is 0
	 * (i.e. unsigned shift).
	 *
	 * @param buf
	 *            array of bytes to shift
	 */
	public static void shiftRight(byte[] buf) {
    	int prevlsb = 0;

    	for (int i=0; i< buf.length; ++i) {
    	    int currentlsb = buf[i] & 0x01;
    	    buf[i] = (byte) ((buf[i] >>> 1) & 0x7F);
    	    buf[i] = (byte) (buf[i] | (prevlsb << 7));
    	    prevlsb = currentlsb;
    	}

	}

	/**
	 * Given a string with a numeric in hex, binary or decimal, convert it to a
	 * bit string.
	 *
	 * @param value
	 *            The numeric value to be converted to a bit string
	 *
	 * @return The bit string representation of the input string value
	 *
	 * @throws IllegalArgumentException if there's an error converting the value to a binary string
	 */
	public static String getBitsFromNumericString(final String value) throws IllegalArgumentException {

		String valueBits;
		if (BinOctHexUtility.hasHexPrefix(value)) {
			valueBits = BinOctHexUtility.toBinFromHex(BinOctHexUtility.stripHexPrefix(value));
		} else if (BinOctHexUtility.hasBinaryPrefix(value)) {
			valueBits = BinOctHexUtility.stripBinaryPrefix(value);
		} else {
			try {
				valueBits = Long.toBinaryString(Long.parseLong(value));
			} catch (final Exception e) {
				throw new IllegalArgumentException(ExceptionTools.getMessage(e));
			}
		}

		return (valueBits);
	}

}
