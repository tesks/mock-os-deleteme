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
package jpl.gds.sleproxy.server.chillinterface.downlink.ampcsutil;

import java.util.BitSet;
import java.util.regex.Pattern;

/**
 * This class defines static methods for the standard mapping between
 * <b>CCSDS</b> standard Global Data Representation data items and Java data.
 * 
 * This code is taken from AMPCS core's, but because we don't want to introduce
 * a dependency on AMPCS core just for this class, it was basically copied over.
 * In AMPCSR8, when SLE capability is integrated into AMPCS architecture itself,
 * this duplication can then be removed.
 * 
 */
public class GDR {
	/** GDR 8 size */
	public static final int GDR_8_SIZE = 1;

	/** GDR 16 size */
	public static final int GDR_16_SIZE = 2;

	/** GDR 24 size */
	public static final int GDR_24_SIZE = 3;

	/** GDR 32 size */
	public static final int GDR_32_SIZE = 4;

	/** GDR 64 size MPCS-5932 09/02/15 Added */
	public static final int GDR_64_SIZE = 8;

	/** GDR float size */
	public static final int GDR_FLOAT_SIZE = 4;

	/** GDR double size */
	public static final int GDR_DOUBLE_SIZE = 8;

	/** GDR time size */
	public static final int GDR_TIME_SIZE = 6;

	/**
	 * Holds a simple mask to access lower bits of a 1 to 32 bit field.
	 * 
	 * @see #makeBitMask
	 */
	private static final int bitMask[] = { 0x0, 0x1, 0x3, 0x7, 0xf, 0x1f, 0x3f, 0x7f, 0xff, 0x1ff, 0x3ff, 0x7ff, 0xfff,
			0x1fff, 0x3fff, 0x7fff, 0xffff, 0x1ffff, 0x3ffff, 0x7ffff, 0xfffff, 0x1fffff, 0x3fffff, 0x7fffff, 0xffffff,
			0x1ffffff, 0x3ffffff, 0x7ffffff, 0xfffffff, 0x1fffffff, 0x3fffffff, 0x7fffffff, 0xffffffff,

	};

	/**
	 * Returns the bit mask from a bit length.
	 * 
	 * @param length
	 *            length of bit mask in bits from 0 to 32
	 * @return bit mask from 0x0 to 0xffffffff
	 * @throws IllegalArgumentException
	 *             if length < 0 or > 32
	 */
	public static int makeBitMask(final int length) throws IllegalArgumentException {
		if (length < 0 || length > 32) {
			throw new IllegalArgumentException("makeBitMask length must be in the range 0..32 " + length);
		}
		return bitMask[length];
	}

	/**
	 * Fills a fixed length buffer using a fill character (fill value) on the
	 * LEFT, i.e., justifies the string to the RIGHT.
	 * 
	 * @param val
	 *            string value to fill
	 * @param fillSize
	 *            desired string length after fill
	 * @param fillChar
	 *            character to use as fill, usually '0' or ' '
	 * @return a String of length fillSize, or the input string if the
	 *         fillLength is less than the length of the input String
	 */
	public static String fillStr(final String val, final int fillSize, final char fillChar) {
		StringBuilder tmp = new StringBuilder(1024);
		int i;
		int toSize;

		toSize = fillSize - val.length();
		for (i = 0; i < toSize; ++i) {
			tmp.append(fillChar);
		}
		tmp.append(val);
		return tmp.toString();
	}

	/**
	 * Fills a fixed length buffer using a fill character (fill value) on the
	 * RIGHT, i.e, justifies the String to the LEFT.
	 * 
	 * @param val
	 *            string value to fill
	 * @param fillSize
	 *            desired string length after fill
	 * @param fillChar
	 *            character to use as fill, usually '0' or ' '
	 * @return a String of length fillSize, or the input string if the
	 *         fillLength is less than the length of the input String
	 */
	public static String leftFillStr(final String val, final int fillSize, final char fillChar) {
		StringBuilder tmp = new StringBuilder(1024);
		int i;
		int toSize;

		tmp.append(val);
		toSize = fillSize - val.length();
		for (i = 0; i < toSize; ++i) {
			tmp.append(fillChar);
		}
		return tmp.toString();
	}

	/**
	 * Fills a fixed length buffer using a space character on the LEFT, i.e.,
	 * justifies the string to the RIGHT.
	 * 
	 * @param val
	 *            string value to fill
	 * @param fillSize
	 *            desired string length after fill
	 * @return a String of length fillSize, or the input string if the
	 *         fillLength is less than the length of the input String
	 */
	public static String fillStr(final String val, final int fillSize) {
		return fillStr(val, fillSize, ' ');
	}

	/**
	 * Fills a fixed length buffer using a '0' character on the LEFT, i.e.,
	 * justifies the string to the RIGHT.
	 * 
	 * @param val
	 *            string value to fill
	 * @param fillSize
	 *            desired string length after fill
	 * @return a String of length fillSize, or the input string if the
	 *         fillLength is less than the length of the input String
	 */
	public static String fillZero(final String val, final int fillSize) {
		return fillStr(val, fillSize, '0');
	}

	/**
	 * Fills a fixed length buffer using a '.' character on the LEFT, i.e.,
	 * justifies the string to the RIGHT.
	 * 
	 * @param val
	 *            string value to fill
	 * @param fillSize
	 *            desired string length after fill
	 * @return a String of length fillSize, or the input string if the
	 *         fillLength is less than the length of the input String
	 */
	public static String fillDot(final String val, final int fillSize) {
		return fillStr(val, fillSize, '.');
	}

	/**
	 * Fills a fixed length buffer using a space character on the RIGHT, i.e,
	 * justifies the String to the LEFT.
	 * 
	 * @param val
	 *            string value to fill
	 * @param fillSize
	 *            desired string length after fill
	 * @return a String of length fillSize, or the input string if the
	 *         fillLength is less than the length of the input String
	 */
	public static String leftFillStr(final String val, final int fillSize) {
		return leftFillStr(val, fillSize, ' ');
	}

	/**
	 * Fills a fixed length buffer using a '0' character on the RIGHT, i.e,
	 * justifies the String to the LEFT.
	 * 
	 * @param val
	 *            string value to fill
	 * @param fillSize
	 *            desired string length after fill
	 * @return a String of length fillSize, or the input string if the
	 *         fillLength is less than the length of the input String
	 */
	public static String leftFillZero(final String val, final int fillSize) {
		return leftFillStr(val, fillSize, '0');
	}

	/**
	 * Fills a fixed length buffer using a '.' character on the RIGHT, i.e,
	 * justifies the String to the LEFT.
	 * 
	 * @param val
	 *            string value to fill
	 * @param fillSize
	 *            desired string length after fill
	 * @return a String of length fillSize, or the input string if the
	 *         fillLength is less than the length of the input String
	 */
	public static String leftFillDot(final String val, final int fillSize) {
		return leftFillStr(val, fillSize, '.');
	};

	/**
	 * Holds number of bits on for each byte value (0..255)
	 * 
	 * @see #numBitsOn
	 */
	private static final int bitsOn[] = { 0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3,
			3, 4, 3, 4, 4, 5, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5,
			5, 6, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3,
			3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 1, 2, 2, 3, 2, 3,
			3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4,
			4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5,
			5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 4, 5,
			5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8, };

	/**
	 * Find the number of bits that are on (i.e., set to one) in a byte buffer.
	 * 
	 * @param buff
	 *            Byte array
	 * @param off
	 *            starting offset
	 * @param length
	 *            number of bytes to check
	 * @return number of bits on
	 */
	public static int numBitsOn(final byte buff[], final int off, final int length) {
		if (off + length > buff.length) {
			throw new IllegalArgumentException("Not enough bytes in buffer to check " + length + " bytes");
		}
		int on, i;

		on = 0;
		for (i = 0; i < length; ++i) {
			on += bitsOn[0xff & buff[off + i]];
		}
		return on;
	}

	/**
	 * Find the number of bits on (set to one) in a byte.
	 * 
	 * @param b
	 *            byte value
	 * @return number of bits on
	 */
	public static int numBitsOn(final byte b) {
		return bitsOn[0xff & b];
	}

	/**
	 * Find the number of bits on (set to one) in a short.
	 * 
	 * @param b
	 *            short value
	 * @return number of bits on
	 */
	public static int numBitsOn(final short b) {
		byte[] buff = new byte[2];
		set_u16(buff, 0, b);
		return numBitsOn(buff, 0, 2);
	}

	/**
	 * Find the number of bits on (set to one) in an int.
	 * 
	 * @param b
	 *            short value
	 * @return number of bits on
	 */
	public static int numBitsOn(final int b) {
		byte[] buff = new byte[4];
		set_u32(buff, 0, b);
		return numBitsOn(buff, 0, 4);
	}

	/**
	 * Find the number of bits on (set to one) in a long.
	 * 
	 * @param b
	 *            long value
	 * @return number of bits on
	 */
	public static int numBitsOn(final long b) {
		byte[] buff = new byte[8];
		set_u32(buff, 0, (int) (b >> 32));
		set_u32(buff, 4, (int) (b & 0xffffffff));
		return numBitsOn(buff, 0, 8);
	}

	/**
	 * Find the number of bits on (set to one) in a BitSet.
	 * 
	 * @see java.util.BitSet
	 * @param b
	 *            is a BitSet
	 * @return number of bits on
	 */
	public static int numBitsOn(final BitSet b) {
		int i, on;
		on = 0;
		for (i = 0; i < b.size(); ++i) {
			if (b.get(i)) {
				++on;
			}
		}
		return on;
	}

	/**
	 * GDR, get java String value from byte array. Characters with an ASCII
	 * value less than that of the space character, except for line feed and tab
	 * characters, are replaced by a period. A character value of 0, when found,
	 * ends the string construction regardless of the value of the length
	 * argument.
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @param length
	 *            length of string in bytes
	 * @return String value
	 */
	public static String stringValue(final byte buff[], final int off, final int length) {
		StringBuilder s = new StringBuilder(1024);
		int i;

		for (i = 0; i < length; ++i) {
			if (buff[off + i] == 0) {
				break;
			}
			if (buff[off + i] == '\n' || buff[off + i] == '\t') {
				s.append((char) (0xff & buff[off + i]));
			} else if (buff[off + i] < ' ') {
				s.append('.');
			} else {
				s.append((char) (0xff & buff[off + i]));
			}
		}
		return s.toString();
	}

	/**
	 * GDR, get unsigned 8 bit value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return int value
	 */
	public static int get_u8(final byte[] buff, final int off) {
		int val;
		val = 0xff & buff[off];
		return val;
	}

	/**
	 * GDR, get unsigned 16 bit value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return int value
	 */
	public static int get_u16(final byte[] buff, final int off) {
		int val;
		val = ((0xff & buff[off]) << 8) | (0xff & buff[off + 1]);
		return val;
	}

	/**
	 * GDR, get unsigned 24 bit value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return int value
	 */
	public static int get_u24(final byte[] buff, final int off) {
		int val;
		val = ((0xff & buff[off]) << 16) | ((0xff & buff[off + 1]) << 8) | (0xff & buff[off + 2]);
		return val;
	}

	/**
	 * GDR, get unsigned 32 bit value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return long value
	 */
	public static long get_u32(final byte[] buff, final int off) {
		long val = ((buff[off] << 24) & 0xff000000L) | ((buff[off + 1] << 16) & 0x00ff0000L)
				| ((buff[off + 2] << 8) & 0x0000ff00L) | ((buff[off + 3]) & 0x000000ffL);
		return val;
	}

	/**
	 * GDR, get an unsigned 64 bit value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return long value
	 */
	public static long get_u64(final byte buff[], final int off) {
		long lval;
		long highv = (get_u32(buff, off) << 32) & 0xffffffff00000000L;
		long lowv = get_u32(buff, off + 4) & 0xffffffffL;
		lval = (highv | lowv);
		return lval;
	}

	/**
	 * Get a signed integer value from a byte array.
	 * 
	 * CAUTION: This method only works with the following byte-aligned bit
	 * lengths: 8, 16, 24, 32, 64
	 * 
	 * @param bytes
	 *            The byte array to read the value out of
	 * @param offset
	 *            The offset into the byte array where the value to read starts
	 * @param bitLength
	 *            The length in bits of the value to read (should be one of 8,
	 *            16, 24, 32, 64)
	 * 
	 * @throws IllegalArgumentException
	 *             If the input bit length is not one of: 8, 16, 24, 32, 64
	 * 
	 * @return A signed long value of the number
	 */
	public static long getSignedInteger(final byte[] bytes, final int offset, final int bitLength)
			throws IllegalArgumentException {
		switch (bitLength) {
		case 8:
			return (get_i8(bytes, offset));

		case 16:
			return (get_i16(bytes, offset));

		case 24:
			return (get_i24(bytes, offset));

		case 32:
			return (get_i32(bytes, offset));

		case 64:
			return (get_i64(bytes, offset));

		default:

			throw new IllegalArgumentException(
					"A bit length of " + bitLength + " is not supported by the getSignedInteger(...) function.");
		}
	}

	/**
	 * Get an unsigned integer value from a byte array.
	 * 
	 * CAUTION: This method only works with the following byte-aligned bit
	 * lengths: 8, 16, 24, 32
	 * 
	 * @param bytes
	 *            The byte array to read the value out of
	 * @param offset
	 *            The offset into the byte array where the value to read starts
	 * @param bitLength
	 *            The length in bits of the value to read (should be one of 8,
	 *            16, 24, 32)
	 * 
	 * @throws IllegalArgumentException
	 *             If the input bit length is not one of: 8, 16, 24, 32
	 * 
	 * @return An unsigned long value of the number
	 */
	public static long getUnsignedInteger(final byte[] bytes, final int offset, final int bitLength)
			throws IllegalArgumentException {
		switch (bitLength) {
		case 8:
			return (get_u8(bytes, offset));

		case 16:
			return (get_u16(bytes, offset));

		case 24:
			return (get_u24(bytes, offset));

		case 32:
			return (get_u32(bytes, offset));

		case 64:
		case 63:
			return (get_u64(bytes, offset));

		default:

			throw new IllegalArgumentException(
					"A bit length of " + bitLength + " is not supported by the getUnsignedInteger(...) function.");
		}
	}

	/**
	 * Get an unsigned integer value from a byte array.
	 * 
	 * CAUTION: This method only works with the following byte-aligned bit
	 * lengths: 32, 64
	 * 
	 * @param bytes
	 *            The byte array to read the value out of
	 * @param offset
	 *            The offset into the byte array where the value to read starts
	 * @param bitLength
	 *            The length in bits of the value to read (should be one of 32,
	 *            64)
	 * 
	 * @throws IllegalArgumentException
	 *             If the input bit length is not one of: 32, 64
	 * 
	 * @return An unsigned long value of the number
	 */
	public static double getFloatingPoint(final byte[] bytes, final int offset, final int bitLength)
			throws IllegalArgumentException {
		switch (bitLength) {
		case 32:
			return (get_float(bytes, offset));

		case 64:
			return (GDR.get_double(bytes, offset));

		default:

			throw new IllegalArgumentException(
					"A bit length of " + bitLength + " is not supported by the getFloatingPoint(...) function.");
		}
	}

	/**
	 * GDR, get signed 8 bit value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return int value
	 */
	public static int get_i8(final byte[] buff, final int off) {
		return buff[off];
	}

	/**
	 * GDR, get signed 16 bit value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return int value
	 */
	public static int get_i16(final byte[] buff, final int off) {
		int val;
		val = ((buff[off]) << 8) | (0xff & buff[off + 1]);
		return val;
	}

	/**
	 * GDR, get signed 24 bit value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return int value
	 */
	public static int get_i24(final byte[] buff, final int off) {
		int val;
		val = ((0xff & buff[off]) << 16) | ((0xff & buff[off + 1]) << 8) | (0xff & buff[off + 2]);
		return val;
	}

	/**
	 * GDR, get signed 32 bit value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return int value
	 */
	public static int get_i32(final byte[] buff, final int off) {
		int val;
		val = ((0xff & buff[off]) << 24) | ((0xff & buff[off + 1]) << 16) | ((0xff & buff[off + 2]) << 8)
				| (0xff & buff[off + 3]);
		return val;
	}

	/**
	 * GDR, get signed 64 bit value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return int value
	 */
	public static long get_i64(final byte[] buff, final int off) {
		long val;
		val = ((0xffL & buff[off]) << 56) | ((0xffL & buff[off + 1]) << 48) | ((0xffL & buff[off + 2]) << 40)
				| ((0xffL & buff[off + 3]) << 32) | ((0xffL & buff[off + 4]) << 24) | ((0xffL & buff[off + 5]) << 16)
				| ((0xffL & buff[off + 6]) << 8) | (0xffL & buff[off + 7]);
		return val;
	}

	/**
	 * GDR, get java short value from byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @return short value
	 */
	public static short shortValue(final byte buff[], final int off) {
		return (short) get_i16(buff, off);
	}

	/**
	 * Get signed 8 bit value from byte array at the given byte + bit offset,
	 * masking out the high bits on the return value to achieve the specified
	 * bit length
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @param firstBit
	 *            bit offset into the starting byte
	 * @param bitLength
	 *            the
	 * @return int the resulting value
	 */
	public static int get_i8(final byte[] buff, int off, int firstBit, final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 8) {
			throw new IllegalArgumentException("bit length cannot be greater than 8");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		int val;
		int needBits = firstBit + bitLength;
		if (needBits <= 8) {
			if (buff.length < off + 1) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_i8(buff, off);
			shift = 8 - needBits;
		} else {
			if (buff.length < off + 2) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_i16(buff, off);
			shift = 16 - needBits;
		}
		val = val >> shift;
		return mask & val;
	}

	/**
	 * Get unsigned 8 bit value from byte array at the given byte + bit offset,
	 * masking out the high bits on the return value to achieve the specified
	 * bit length
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @param firstBit
	 *            bit offset into the starting byte
	 * @param bitLength
	 *            the
	 * @return int the resulting value
	 */
	public static int get_u8(final byte[] buff, int off, int firstBit, final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 8) {
			throw new IllegalArgumentException("bit length cannot be greater than 8");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		int val;
		int needBits = firstBit + bitLength;
		if (needBits <= 8) {
			if (buff.length < off + 1) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_u8(buff, off);
			shift = 8 - needBits;
		} else {
			if (buff.length < off + 2) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_u16(buff, off);
			shift = 16 - needBits;
		}
		val = val >> shift;
		return val & mask;
	}

	/**
	 * Get signed 16 bit value from byte array at the given byte + bit offset,
	 * masking out the high bits on the return value to achieve the specified
	 * bit length
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @param firstBit
	 *            bit offset into the starting byte
	 * @param bitLength
	 *            the
	 * @return int the resulting value
	 */
	public static int get_i16(final byte[] buff, int off, int firstBit, final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 16) {
			throw new IllegalArgumentException("bit length cannot be greater than 16");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		int val;
		int needBits = firstBit + bitLength;
		if (needBits <= 16) {
			if (buff.length < off + 2) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_i16(buff, off);
			shift = 16 - needBits;
		} else {
			if (buff.length < off + 3) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_i24(buff, off);
			shift = 24 - needBits;
		}
		val = val >> shift;
		return val & mask;
	}

	/**
	 * Get unsigned 16 bit value from byte array at the given byte + bit offset,
	 * masking out the high bits on the return value to achieve the specified
	 * bit length
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @param firstBit
	 *            bit offset into the starting byte
	 * @param bitLength
	 *            the
	 * @return int the resulting value
	 */
	public static int get_u16(final byte[] buff, int off, int firstBit, final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 16) {
			throw new IllegalArgumentException("bit length cannot be greater than 16");
		}
		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		int val;
		int needBits = firstBit + bitLength;
		if (needBits <= 16) {
			if (buff.length < (off + 2)) {
				throw new IllegalArgumentException("(1) Not enough bytes in buffer to get " + bitLength + " bits off="
						+ off + " buff.length=" + buff.length);
			}
			val = get_u16(buff, off);
			shift = 16 - needBits;
		} else {
			if (buff.length < (off + 3)) {
				System.out.println("get_U16 Exception 1 buff.length=" + buff.length + " off=" + off);
				throw new IllegalArgumentException("(2) Not enough bytes in buffer to get " + bitLength + " bits off="
						+ off + " buff.length=" + buff.length);
			}
			val = get_u24(buff, off);
			shift = 24 - needBits;
		}
		val = val >> shift;
		return val & mask;
	}

	/**
	 * Gets signed 24 bit value from byte array at the given byte + bit offset,
	 * masking out the high bits on the return value to achieve the specified
	 * bit length
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @param firstBit
	 *            bit offset into the starting byte
	 * @param bitLength
	 *            the
	 * @return int the resulting value
	 */
	public static int get_i24(final byte[] buff, int off, int firstBit, final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 24) {
			throw new IllegalArgumentException("bit length cannot be greater than 24");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		long val;
		int needBits = firstBit + bitLength;
		if (needBits <= 24) {
			if (buff.length < off + 3) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_i24(buff, off);
			shift = 24 - needBits;
		} else {
			if (buff.length < off + 4) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_i32(buff, off);
			shift = 32 - needBits;
		}
		val = val >> shift;
		return (int) val & mask;
	}

	/**
	 * Get unsigned 24 bit value from byte array at the given byte + bit offset,
	 * masking out the high bits on the return value to achieve the specified
	 * bit length
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @param firstBit
	 *            bit offset into the starting byte
	 * @param bitLength
	 *            the
	 * @return int the resulting value
	 */
	public static int get_u24(final byte[] buff, int off, int firstBit, final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 24) {
			throw new IllegalArgumentException("bit length cannot be greater than 24");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		long val;
		int needBits = firstBit + bitLength;
		if (needBits <= 24) {
			if (buff.length < off + 3) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_u24(buff, off);
			shift = 24 - needBits;
		} else {
			if (buff.length < off + 4) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_u32(buff, off);
			shift = 32 - needBits;
		}
		val = val >> shift;
		return (int) val & mask;
	}

	/**
	 * Get signed 32 bit value from byte array at the given byte + bit offset,
	 * masking out the high bits on the return value to achieve the specified
	 * bit length
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @param firstBit
	 *            bit offset into the starting byte
	 * @param bitLength
	 *            the
	 * @return int the resulting value
	 */
	public static long get_i32(final byte[] buff, int off, int firstBit, final int bitLength) {
		long mask;
		int shift;

		if (bitLength > 32) {
			throw new IllegalArgumentException("bit length cannot be greater than 32");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		long val;
		int needBits = firstBit + bitLength;
		if (needBits <= 32) {
			if (buff.length < off + 4) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_i32(buff, off);
			shift = 32 - needBits;
		} else {
			if (buff.length < off + 5) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = ((0xffL & buff[off]) << 32) | ((0xffL & buff[off + 1]) << 24) | ((0xffL & buff[off + 2]) << 16)
					| ((0xffL & buff[off + 3]) << 8) | (0xffL & buff[off + 4]);
			shift = 40 - needBits;
		}
		val = val >> shift;
		val = (val & 0xffffffffL);
		val = val & mask;
		return val;
	}

	/**
	 * Get signed 32 bit value from byte array at the given byte + bit offset,
	 * masking out the high bits on the return value to achieve the specified
	 * bit length
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @param firstBit
	 *            bit offset into the starting byte
	 * @param bitLength
	 *            the
	 * @return int the resulting value
	 */
	public static long get_i64(final byte[] buff, final int off, final int firstBit, final int bitLength) {
		long mask;

		if (bitLength > 64) {
			throw new IllegalArgumentException("bit length cannot be greater than 64");
		}

		if (bitLength < 32) {
			mask = bitMask[bitLength];
		} else {
			mask = ((long) (bitMask[bitLength - 32]) << 32) | 0xffffffff;
		}
		long val;
		int needBits = firstBit + bitLength;
		if (needBits <= 64) {
			if (buff.length < off + 8) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_i64(buff, off);
		} else {
			throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
		}
		val = val >> firstBit;
		val = val & mask;
		return val;
	}

	/**
	 * Get unsigned 32 bit value from byte array at the given byte + bit offset,
	 * masking out the high bits on the return value to achieve the specified
	 * bit length
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset in array
	 * @param firstBit
	 *            bit offset into the starting byte
	 * @param bitLength
	 *            the
	 * @return int the resulting value
	 */
	public static long get_u32(final byte[] buff, int off, int firstBit, final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 32) {
			throw new IllegalArgumentException("bit length cannot be greater than 32");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		long val;
		int needBits = firstBit + bitLength;
		if (needBits <= 32) {
			if (buff.length < off + 4) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = get_u32(buff, off);
			shift = 32 - needBits;
		} else {
			if (buff.length < off + 5) {
				throw new IllegalArgumentException("Not enough bytes in buffer to get " + bitLength + " bits");
			}
			val = (((long) buff[off] << 32) & 0xff00000000L) | ((buff[off + 1] << 24) & 0x00ff000000L)
					| ((buff[off + 2] << 16) & 0x0000ff0000L) | ((buff[off + 3] << 8) & 0x000000ff00L)
					| ((buff[off + 4]) & 0x00000000ffL);
			shift = 40 - needBits;
		}
		val = val >> shift;
		val = (val & 0xffffffffL);
		val = val & mask;
		return val;
	}

	/**
	 * GDR, get java float from byte array.
	 * 
	 * @param buff
	 *            the byte array containing the float
	 * @param off
	 *            the starting offset into the array of the float
	 * @return the float value
	 */
	public static float get_float(final byte[] buff, final int off) {
		long lval;
		lval = get_u32(buff, off);
		int ival = (int) (0xffffffff & lval);
		return Float.intBitsToFloat(ival);
	}

	/**
	 * GDR, get java double from byte array.
	 * 
	 * @param buff
	 *            the byte array containing the double
	 * @param off
	 *            the starting offset into the array of the double
	 * @return the double value
	 */
	public static double get_double(final byte[] buff, final int off) {
		long lval;
		double dval;
		long highv = (get_u32(buff, off) << 32) & 0xffffffff00000000L;
		long lowv = get_u32(buff, off + 4) & 0xffffffffL;
		lval = (highv | lowv);
		dval = Double.longBitsToDouble(lval);
		return dval;
	}

	/**
	 * GDR, set unsigned 8 bit value into byte array from a byte
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_u8(final byte[] buff, final int off, final byte val) {
		buff[off] = val;
		return 1;
	}

	/**
	 * GDR, set unsigned 8 bit value into byte array from an int
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_u8(final byte[] buff, final int off, final int val) {
		return set_u8(buff, off, (byte) val);
	}

	/**
	 * GDR, set 16 bit unsigned value into byte array from a short
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_u16(final byte[] buff, final int off, final short val) {
		buff[off] = (byte) (val >>> 8);
		buff[off + 1] = (byte) (0xff & val);
		return 2;
	}

	/**
	 * GDR, set 16 bit unsigned value into byte array from an int
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_u16(final byte[] buff, final int off, final int val) {
		return set_u16(buff, off, (short) val);
	}

	/**
	 * GDR, set unsigned 24 bit value into byte array from an int
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_u24(final byte[] buff, final int off, final int val) {
		if (val > 0xffffff) {
			throw new IllegalArgumentException("value is too large to assign to 3 bytes");
		}
		buff[off] = (byte) (0xff & (val >>> 16));
		buff[off + 1] = (byte) (0xff & (val >>> 8));
		buff[off + 2] = (byte) (0xff & val);
		return 3;
	}

	/**
	 * GDR, set 32 bit unsigned value into byte array from an int
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_u32(final byte[] buff, final int off, final int val) {
		buff[off] = (byte) (0xff & (val >>> 24));
		buff[off + 1] = (byte) (0xff & (val >>> 16));
		buff[off + 2] = (byte) (0xff & (val >>> 8));
		buff[off + 3] = (byte) (0xff & val);
		return 4;
	}

	/**
	 * GDR, set 32 bit unsigned value into byte array from a long
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_u32(final byte[] buff, final int off, final long val) {
		return set_u32(buff, off, (int) val);
	}

	/**
	 * GDR, set 64 bit unsigned long value into byte array from a long
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_u64(final byte[] buff, final int off, final long val) {
		buff[off] = (byte) (0xff & (val >>> 56));
		buff[off + 1] = (byte) (0xff & (val >>> 48));
		buff[off + 2] = (byte) (0xff & (val >>> 40));
		buff[off + 3] = (byte) (0xff & (val >>> 32));
		buff[off + 4] = (byte) (0xff & (val >>> 24));
		buff[off + 5] = (byte) (0xff & (val >>> 16));
		buff[off + 6] = (byte) (0xff & (val >>> 8));
		buff[off + 7] = (byte) (0xff & val);
		return 8;
	}

	/**
	 * GDR, set signed 8 bit value into byte array from an int
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_i8(final byte[] buff, final int off, final int val) {
		buff[off] = (byte) val;
		return 1;
	}

	/**
	 * GDR, set signed 16 bit value into byte array from an int
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_i16(final byte[] buff, final int off, final int val) {
		buff[off] = (byte) (val >>> 8);
		buff[off + 1] = (byte) (0xff & val);
		return 2;
	}

	/**
	 * GDR, set signed 24 bit value into byte array from an int
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_i24(final byte[] buff, final int off, final int val) {
		buff[off] = (byte) (0xff & (val >>> 16));
		buff[off + 1] = (byte) (0xff & (val >>> 8));
		buff[off + 2] = (byte) (0xff & val);
		return 3;
	}

	/**
	 * GDR, set signed 32 bit value into byte array from an int
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_i32(final byte[] buff, final int off, final int val) {
		buff[off] = (byte) (0xff & (val >>> 24));
		buff[off + 1] = (byte) (0xff & (val >>> 16));
		buff[off + 2] = (byte) (0xff & (val >>> 8));
		buff[off + 3] = (byte) (0xff & val);
		return 4;
	}

	/**
	 * GDR, set signed 64 bit value into byte array from a long
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_i64(final byte[] buff, final int off, final long val) {
		buff[off] = (byte) (0xff & (val >>> 56));
		buff[off + 1] = (byte) (0xff & (val >>> 48));
		buff[off + 2] = (byte) (0xff & (val >>> 40));
		buff[off + 3] = (byte) (0xff & (val >>> 32));
		buff[off + 4] = (byte) (0xff & (val >>> 24));
		buff[off + 5] = (byte) (0xff & (val >>> 16));
		buff[off + 6] = (byte) (0xff & (val >>> 8));
		buff[off + 7] = (byte) (0xff & val);
		return 8;
	}

	/**
	 * GDR, set 32 bit floating point value int byte array
	 * 
	 * @param buff
	 *            the byte array to populate
	 * @param off
	 *            the starting offset in the byte array
	 * @param val
	 *            the float value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_float(final byte[] buff, final int off, final double val) {
		long lval;
		lval = Float.floatToIntBits((float) val);
		set_u32(buff, off, lval);
		return 4;
	}

	/**
	 * GDR, set 64 bit floating point value into byte array
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset into the byte array
	 * @param val
	 *            value to set
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_double(final byte[] buff, final int off, final double val) {
		long lval = Double.doubleToLongBits(val);
		long lowv, high;
		lowv = (0xffffffffL & lval);
		high = (0xffffffffL & (lval >> 32));
		GDR.set_u32(buff, off, (int) high);
		GDR.set_u32(buff, off + 4, (int) lowv);
		return 8;
	}

	/**
	 * GDR, set String value into byte array. If the string has an odd number of
	 * characters, it will be padded with a null character.
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset into the byte array
	 * @param val
	 *            String value to set
	 * @param len
	 *            number of bytes to set; if it exceeds the string length, the
	 *            length will be reduced to the string length
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_string(final byte[] buff, final int off, final String val, int len) {
		int i;
		if (len > val.length()) {
			len = val.length();
		}
		for (i = 0; i < len; ++i) {
			char ch = val.charAt(i);
			buff[off + i] = (byte) (0xff & ch);
		}
		if ((i % 2) != 0) {
			buff[off + i] = 0;
			++i;
		}
		return i;
	}

	/**
	 * GDR, set String value into byte array, without any padding
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset into the byte array
	 * @param val
	 *            String value to set
	 * 
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_string_no_pad(final byte[] buff, final int off, final String val) {
		int i;
		int len = val.length();

		for (i = 0; i < len; ++i) {
			char ch = val.charAt(i);
			buff[off + i] = (byte) (0xff & ch);
		}
		return i;
	}

	/**
	 * GDR, set String value into byte array. If the string has an odd number of
	 * characters, it will be padded with a null character.
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset into the byte array
	 * @param val
	 *            String value to set be reduced to the string length
	 * @return number of bytes set (to add to offset)
	 */
	public static int set_string(final byte[] buff, final int off, final String val) {
		return set_string(buff, off, val, val.length());
	}

	/**
	 * Sets unsigned 8 bit value into byte buffer from an int, starting at the
	 * given byte + bit offset in the buffer.
	 * 
	 * @param buff
	 *            Byte arraty
	 * @param off
	 *            Byte offset
	 * @param val
	 *            Value
	 * @param first_bit
	 *            First bit
	 * @param bit_length
	 *            Bit length
	 * @return 1
	 * @throws NumberFormatException
	 *             Bit offsets outside of byte range
	 */
	public static int set_u8(final byte[] buff, final int off, final int val, final int first_bit, final int bit_length)
			throws NumberFormatException {
		if (first_bit < 0 || (first_bit + bit_length) > 8) {
			throw new NumberFormatException("bit offsets outside of byte range");
		}
		int mask;
		int shift;
		int inVal;
		int outVal;
		outVal = buff[off];
		mask = bitMask[bit_length];
		inVal = mask & val;
		shift = 8 - bit_length - first_bit;
		inVal = inVal << shift;
		outVal = outVal | inVal;
		buff[off] = (byte) outVal;
		return 1;
	}

	/**
	 * Sets signed 8 bit value into byte buffer from an int, starting at the
	 * given byte + bit offset in the buffer.
	 * 
	 * @param buff
	 *            Byte array
	 * @param off
	 *            Offset
	 * @param val
	 *            Value
	 * @param first_bit
	 *            First bit
	 * @param bit_length
	 *            Bit size
	 * 
	 * @return 1
	 * 
	 * @throws NumberFormatException
	 *             Bit offsets outside of byte range
	 */
	public static int set_i8(final byte[] buff, final int off, final int val, final int first_bit, final int bit_length)
			throws NumberFormatException {
		if (first_bit < 0 || (first_bit + bit_length) > 8) {
			throw new NumberFormatException("bit offsets outside of byte range");
		}
		int mask;
		int shift;
		int inVal;
		int outVal;
		outVal = buff[off];
		mask = bitMask[bit_length];
		inVal = mask & val;
		shift = 8 - bit_length - first_bit;
		inVal = inVal << shift;
		outVal = outVal | inVal;
		buff[off] = (byte) outVal;
		return 1;
	}

	/**
	 * Sets unsigned 16 bit value into byte buffer from an int, starting at the
	 * given byte + bit offset in the buffer.
	 * 
	 * @param buff
	 *            Byte array
	 * @param off
	 *            Offset
	 * @param val
	 *            Value
	 * @param first_bit
	 *            First bit
	 * @param bit_length
	 *            Bit size
	 * 
	 * @return 2
	 * 
	 * @throws NumberFormatException
	 *             Bit offsets outside of byte range
	 */
	public static int set_u16(final byte[] buff, final int off, final int val, final int first_bit,
			final int bit_length) throws NumberFormatException {
		if (first_bit < 0 || (first_bit + bit_length) > 16) {
			throw new NumberFormatException("bit offsets outside of byte range");
		}
		int mask;
		int shift;
		int inVal;
		int outVal;
		outVal = get_u16(buff, off);
		mask = bitMask[bit_length];
		inVal = mask & val;
		shift = 16 - bit_length - first_bit;
		inVal = inVal << shift;
		outVal = outVal | inVal;
		set_u16(buff, off, outVal);
		return 2;
	}

	/**
	 * Sets signed 16 bit value into byte buffer from an int, starting at the
	 * given byte + bit offset in the buffer.
	 * 
	 * @param buff
	 *            Byte array
	 * @param off
	 *            Offset
	 * @param val
	 *            Value
	 * @param first_bit
	 *            First bit
	 * @param bit_length
	 *            Bit size
	 * 
	 * @return 2
	 * 
	 * @throws NumberFormatException
	 *             Bit offsets outside of byte range
	 */
	public static int set_i16(final byte[] buff, final int off, final int val, final int first_bit,
			final int bit_length) throws NumberFormatException {
		if (first_bit < 0 || (first_bit + bit_length) > 16) {
			throw new NumberFormatException("bit offsets outside of byte range");
		}
		int mask;
		int shift;
		int inVal;
		int outVal;
		outVal = get_u16(buff, off);
		mask = bitMask[bit_length];
		inVal = mask & val;
		shift = 16 - bit_length - first_bit;
		inVal = inVal << shift;
		outVal = outVal | inVal;
		set_i16(buff, off, outVal);
		return 2;
	}

	/**
	 * Sets unsigned 32 bit value into byte buffer from an int, starting at the
	 * given byte + bit offset in the buffer.
	 * 
	 * @param buff
	 *            Byte array
	 * @param off
	 *            Offset
	 * @param val
	 *            Value
	 * @param first_bit
	 *            First bit
	 * @param bit_length
	 *            Bit size
	 * 
	 * @return 4
	 * 
	 * @throws NumberFormatException
	 *             Bit offsets outside of byte range
	 */
	public static int set_u32(final byte[] buff, final int off, final long val, final int first_bit,
			final int bit_length) throws NumberFormatException {
		if (first_bit < 0 || (first_bit + bit_length) > 32) {
			throw new NumberFormatException("bit offsets outside of byte range");
		}
		long mask;
		int shift;
		long inVal;
		long outVal;
		outVal = get_u32(buff, off);
		mask = bitMask[bit_length];
		inVal = mask & val;
		shift = 32 - bit_length - first_bit;
		inVal = inVal << shift;
		outVal = outVal | inVal;
		set_u32(buff, off, outVal);
		return 4;
	}

	/**
	 * Sets signed 32 bit value into byte buffer from an int, starting at the
	 * given byte + bit offset in the buffer.
	 * 
	 * @param buff
	 *            Byte array
	 * @param off
	 *            Offset
	 * @param val
	 *            Value
	 * @param first_bit
	 *            First bit
	 * @param bit_length
	 *            Bit size
	 * 
	 * @return 4
	 * 
	 * @throws NumberFormatException
	 *             Bit offsets outside of byte range
	 */
	public static int set_i32(final byte[] buff, final int off, final int val, final int first_bit,
			final int bit_length) throws NumberFormatException {
		if (first_bit < 0 || (first_bit + bit_length) > 32) {
			throw new NumberFormatException("bit offsets outside of byte range");
		}
		int mask;
		int shift;
		int inVal;
		int outVal;
		outVal = get_i32(buff, off);
		mask = bitMask[bit_length];
		inVal = mask & val;
		shift = 32 - bit_length - first_bit;
		inVal = inVal << shift;
		outVal = outVal | inVal;
		set_i32(buff, off, outVal);
		return 4;
	}

	/**
	 * Utility function, determines if the given year is a leap year.
	 * 
	 * @param yr
	 *            year (full 4-digit represenation)
	 * @return true if year is a leap year
	 */
	public static boolean isLeapYear(final int yr) {
		return ((yr % 400) == 0) || (((yr % 4) == 0) && ((yr % 100) != 0));
	}

	/**
	 * Holds days in month for non-leap years
	 */
	private static int monthdays[] = { 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334 };
	/**
	 * Holds days in month for leap years
	 */
	private static int ly_monthdays[] = { 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335 };

	/**
	 * Calculate day in year, accounting for leap year.
	 * 
	 * @param month
	 *            1..12
	 * @param day
	 *            in month
	 * @param year
	 *            (1900..)
	 * @return day in year
	 */
	public static int day_in_year(final int month, final int day, final int year) {
		if (month < 1 || month > 12) {
			throw new IllegalArgumentException("Month must be in the range 1..12");
		}
		if (isLeapYear(year)) {
			return ly_monthdays[month] + day - 1;
		} else {
			return monthdays[month] + day - 1;
		}
	}

	/**
	 * Calculate month in year, accounting for leap year, from doy.
	 * 
	 * @param year
	 *            (1900..)
	 * @param doy
	 *            1..366
	 * @return month 1..12
	 */
	public static int month_in_doy(final int year, final int doy) {
		int month = 0;
		for (month = 0; month < 13; ++month) {
			if (isLeapYear(year)) {
				if (doy < ly_monthdays[month]) {
					return month;
				}
			} else {
				if (doy < monthdays[month]) {
					return month;
				}
			}
		}
		return 12;
	}

	/**
	 * Calculate day in month in year, accounting for leap year, from doy.
	 * 
	 * @param year
	 *            (1900..)
	 * @param doy
	 *            1..366
	 * @return day in month 1..31
	 */
	public static int month_day_in_doy(final int year, final int doy) {
		int month = 0;
		for (month = 0; month < 13; ++month) {
			if (isLeapYear(year)) {
				if (doy <= ly_monthdays[month]) {
					return doy - ly_monthdays[month] + 1;
				}
			} else {
				if (doy <= monthdays[month]) {
					return doy - monthdays[month] + 1;
				}
			}
		}
		//
		if (isLeapYear(year)) {
			return doy - ly_monthdays[12] + 1;
		} else {
			return doy - monthdays[12] + 1;
		}
	}

	/**
	 * Count the number of a character in a string. Useful for custom parsing
	 * 
	 * @param tchar
	 *            test character
	 * @param tstr
	 *            source string
	 * @return num 0 for none, value for number up to tstr.size()
	 */
	public int count_chars(char tchar, String tstr) {
		int count = 0;
		int idx = 0;
		while (idx < tstr.length()) {
			if (tstr.indexOf(tchar, idx) == -1) {
				break;
			}
			count += 1;
			idx = tstr.indexOf(tchar, idx) + 1;
		}
		return count;
	}

	/**
	 * Convert month name to number.
	 * 
	 * @param monName
	 *            Month name
	 * 
	 * @return Number
	 */
	public int month_name_to_num(String monName) {
		String[] name_mon = new String[13];
		name_mon[0] = "";
		name_mon[1] = "Jan";
		name_mon[2] = "Feb";
		name_mon[3] = "Mar";
		name_mon[4] = "Apr";
		name_mon[5] = "May";
		name_mon[6] = "Jun";
		name_mon[7] = "Jul";
		name_mon[8] = "Aug";
		name_mon[9] = "Sep";
		name_mon[10] = "Oct";
		name_mon[11] = "Nov";
		name_mon[12] = "Dec";
		for (int idx = 1; idx < name_mon.length; ++idx) {
			if (monName.equalsIgnoreCase(name_mon[idx])) {
				return idx;
			}
		}
		return -1;
	}

	/**
	 * Convert month number to name.
	 * 
	 * @param monNum
	 *            Month number
	 * 
	 * @return Name
	 */
	public String month_num_to_name(int monNum) {
		String[] name_mon = new String[13];
		name_mon[0] = "";
		name_mon[1] = "Jan";
		name_mon[2] = "Feb";
		name_mon[3] = "Mar";
		name_mon[4] = "Apr";
		name_mon[5] = "May";
		name_mon[6] = "Jun";
		name_mon[7] = "Jul";
		name_mon[8] = "Aug";
		name_mon[9] = "Sep";
		name_mon[10] = "Oct";
		name_mon[11] = "Nov";
		name_mon[12] = "Dec";
		return name_mon[monNum];
	}

	/**
	 * Generic encoding text function for hiding marked characters such as url
	 * and query strings.
	 * 
	 * @param inStr
	 *            input string
	 * @param MarkCh
	 *            character to use as marker
	 * @param replaceChars
	 *            characters to replace in input string
	 * @return returns replaced string
	 */
	public static String encodeText(final String inStr, final char MarkCh, final String replaceChars) {
		StringBuilder ret = new StringBuilder(1024);
		char tchar;
		int i;
		for (i = 0; i < inStr.length(); ++i) {
			tchar = inStr.charAt(i);
			if (tchar == MarkCh) {
				ret.append(MarkCh);
				ret.append(MarkCh);
			} else if (replaceChars.indexOf(tchar) != -1) {
				ret.append(MarkCh);
				ret.append(GDR.fillZero(Integer.toHexString(tchar), 2));
			} else {
				ret.append(tchar);
			}
		}
		return ret.toString();
	}

	/**
	 * Encode URL query text
	 * 
	 * @param inStr
	 *            input string
	 * @return encoded text
	 */
	public static String encodeText(final String inStr) {
		return encodeText(inStr, '%', ",.<>/?;:\"'\\|]}[{=-+_)(*&^$#@! \n\t\r");
	}

	/**
	 * Decoded previously encoded text
	 * 
	 * @see #encodeText
	 * @param inStr
	 *            input string
	 * @param MarkCh
	 *            marking character
	 * @return unencoded text
	 */
	public static String decodeText(final String inStr, final char MarkCh) {
		StringBuilder ret = new StringBuilder(1024);
		char tchar;
		int i;
		for (i = 0; i < inStr.length(); ++i) {
			tchar = inStr.charAt(i);
			if (tchar == MarkCh) {
				if ((i + 1) < inStr.length()) {
					i++;
					tchar = inStr.charAt(i);
					if (tchar == MarkCh) {
						ret.append(MarkCh);
					} else {
						StringBuilder tmp = new StringBuilder();
						tmp.append(tchar);
						if ((i + 1) < inStr.length()) {
							i++;
							tmp.append(inStr.charAt(i));
							String str = tmp.toString();
							int val = Integer.parseInt(str, 16);
							ret.append((char) val);
						} else {
						}
					}
				} else {
				}
			} else {
				ret.append(tchar);
			}
		}
		return ret.toString();
	}

	/**
	 * Decode URL query text
	 * 
	 * @param inStr
	 *            Input string
	 * @return decoded text
	 */
	public static String decodeText(final String inStr) {
		return decodeText(inStr, '%');
	}

	/**
	 * GDR, get MODCOMP Double floating point number used in Monitor 5-15 blocks
	 * 
	 * @param buff
	 *            byte array
	 * @param off
	 *            offset into array
	 * @return floating point number
	 */
	public static double getMODCOMP(final byte[] buff, final int off) {
		long temp = GDR.get_u32(buff, off);
		if (temp == 0) {
			return 0.0d;
		}
		byte[] dbuff = new byte[8];
		temp = ((temp & 0x80000000L) == 0x80000000L) ? ~(temp - 1) : temp;
		long ntmp = (GDR.get_u32(buff, off) & 0x80000000L) | (((temp & 0x7fc00000L) >> 2) + 0x2fe00000L)
				| (((temp >> 1) & 0xfffffL));
		GDR.set_u32(dbuff, 0, (int) (0xffffffffL & ntmp));
		GDR.set_u32(dbuff, 4, (int) (0xffffffffL & (temp << 31)));
		return GDR.get_double(dbuff, 0);
	}

	/**
	 * Get MIL-STD-1750A 16-bit from byte array.
	 * 
	 * @param buff
	 *            The buffer to read the bytes from
	 * @param off
	 *            The offset into the buffer
	 * 
	 * @return double
	 */
	public static double getMIL16(final byte[] buff, final int off) {
		byte temp[] = { buff[off], 0, 0, buff[off + 1] };

		return getMIL32(temp, 0);
	}

	/**
	 * Get MIL-STD-1750A 32-bit from byte array.
	 * 
	 * @param buff
	 *            The buffer to read the bytes from
	 * @param off
	 *            The offset into the buffer
	 * 
	 * @return double
	 */
	public static double getMIL32(final byte[] buff, final int off) {
		int expadjust = 1;
		int temp = (int) GDR.get_u32(buff, off);

		int sign = (temp & 0x80000000);

		int mantissa = temp >>> 8;
		if (mantissa == 0) {
			return 0.0;
		}
		if (mantissa == 0x800000) {
			expadjust = 0;
		}
		if (sign != 0) {
			mantissa = ~(mantissa - 1);
		}
		mantissa = mantissa & 0x3fffff;

		int exponent = buff[3] + 1023 - expadjust;

		int hiword = sign | (exponent << 20) | (mantissa >>> 2);
		int loword = mantissa << 30;

		byte[] dbuff = new byte[8];
		GDR.set_u32(dbuff, 0, (0xffffffff & hiword));
		GDR.set_u32(dbuff, 4, (0xffffffff & loword));
		return GDR.get_double(dbuff, 0);
	}

	/**
	 * Get MIL-STD-1750A 48-bit from byte array (not tested).
	 * 
	 * @param buff
	 *            The buffer to read the bytes from
	 * @param off
	 *            The offset into the buffer
	 * 
	 * @return double
	 */
	public static double getMIL48(final byte[] buff, final int off) {
		long carry = 0;
		long expadjust = 1;
		long hitemp = GDR.get_u32(buff, off);
		long lotemp = GDR.get_u32(buff, off + 4);

		long himantissa = hitemp >>> 8;
		long lomantissa = lotemp >>> 16;
		if ((himantissa == 0L) && (lomantissa == 0L)) {
			return 0.0;
		}

		if ((himantissa == 0x800000) && (lomantissa == 0)) {
			expadjust = 0;
		}

		if ((himantissa & 0x800000) != 0) { // negative
			if (lomantissa != 0) {
				carry = 1;
			}
			lomantissa -= 1;
			lomantissa = ~lomantissa & 0xffff;
			himantissa = ~(himantissa - 1 + carry) & 0x3fffff;
		}

		long hiword = (hitemp & 0x80000000L) | ((buff[3] + 1023 - expadjust) << 20) | (himantissa >>> 2);
		long loword = (himantissa << 30) | (lomantissa << 14);

		byte[] dbuff = new byte[8];
		GDR.set_u32(dbuff, 4, (int) (0xffffffffL & hiword));
		GDR.set_u32(dbuff, 0, (int) (0xffffffffL & loword));
		return GDR.get_double(dbuff, 0);
	}

	/**
	 * Get ATAC from byte array
	 * 
	 * @param buff
	 *            The buffer to read the bytes from
	 * @param off
	 *            The offset into the buffer
	 * 
	 * @return double
	 */
	public static double getATAC(final byte[] buff, final int off) {
		int expadjust = 1;
		int temp = (int) GDR.get_u32(buff, off);

		int sign = temp & 0x80000000;

		int mantissa = (temp >> 8);
		if (mantissa == 0) {
			return 0.0;
		}
		if (sign != 0) {
			mantissa = ~(mantissa - 1);
		}
		if (mantissa == 0x800000) {
			expadjust = 0;
		}
		mantissa = mantissa & 0x3fffff;

		int exponent = (temp & 0xff) + 895 - expadjust;

		int hiword = sign | (exponent << 20) | (mantissa >>> 2);
		int loword = mantissa << 30;

		byte[] dbuff = new byte[8];
		GDR.set_u32(dbuff, 0, (0xffffffff & hiword));
		GDR.set_u32(dbuff, 4, (0xffffffff & loword));
		return GDR.get_double(dbuff, 0);
	}

	/**
	 * Get an 8-bit integer value from a set of bits in a byte.
	 * 
	 * @param buff
	 *            The buffer to read the byte from
	 * @param off
	 *            The offset into the buffer
	 * @param bitoff
	 *            The bit offset into the byte (starting from the left of the
	 *            byte)
	 * @param size
	 *            The number of bits to read
	 * @return int
	 */
	public static int cbits_to_u8(final byte[] buff, final int off, final int bitoff, final int size) {
		long mask;
		int shift;
		mask = bitMask[size];
		shift = 8 - size - bitoff;
		return (int) (mask & ((0xff & buff[off]) >> shift));
	}

	/**
	 * Get a 16-bit integer value from a set of bits in a byte array.
	 * 
	 * @param buff
	 *            The buffer to read the bytes from
	 * @param off
	 *            The offset into the buffer
	 * @param bitoff
	 *            The bit offset into the first byte (starting from the left of
	 *            the byte)
	 * @param size
	 *            The number of bits to read
	 * @return int
	 */
	public static int cbits_to_u16(final byte[] buff, final int off, final int bitoff, final int size) {
		long mask;
		int shift;
		mask = bitMask[size];
		shift = 16 - size - bitoff;
		return (int) (mask & (GDR.get_u16(buff, off) >> shift));
	}

	/**
	 * u16 to cbits.
	 * 
	 * @param buff
	 *            Byte array to be modified
	 * @param off
	 *            Offset
	 * @param val
	 *            Value to insert
	 * @param bitoff
	 *            Bit offset
	 * @param size
	 *            Bit size
	 * 
	 * @return 1
	 */
	public static int u8_to_cbits(final byte[] buff, final int off, final int val, final int bitoff, final int size) {
		long mask, rval;
		int shift;
		rval = buff[off];
		shift = 8 - size - bitoff;
		mask = bitMask[size] << shift;
		rval = rval & ~mask;
		rval = 0xff & (rval | (mask & (val << shift)));
		buff[off] = (byte) rval;
		return 1;
	}

	/**
	 * u16 to cbits.
	 * 
	 * @param buff
	 *            Byte array to be modified
	 * @param off
	 *            Offset
	 * @param val
	 *            Value to insert
	 * @param bitoff
	 *            Bit offset
	 * @param size
	 *            Bit size
	 * 
	 * @return 1
	 */
	public static int u16_to_cbits(final byte[] buff, final int off, final int val, final int bitoff, final int size) {
		long mask, rval;
		int shift;
		rval = GDR.get_u16(buff, off);
		shift = 16 - size - bitoff;
		mask = bitMask[size] << shift;
		rval = rval & ~mask;
		rval = (rval | (mask & (val << shift)));
		GDR.set_u16(buff, off, (int) rval);
		return 1;
	}

	/**
	 * Get string from byte array as int array.
	 * 
	 * @param val
	 *            Destination int array
	 * @param buff
	 *            Byte array
	 * @param off
	 *            Offset
	 * @param len
	 *            Length
	 */
	public static void get_string(final int[] val, final byte[] buff, final int off, final int len) {
		for (int i = 0; i < len; ++i) {
			val[i] = buff[i + off];
		}
	}

	/**
	 * Get string from byte array.
	 * 
	 * @param buff
	 *            Byte array
	 * @param off
	 *            Offset
	 * @param len
	 *            Length
	 * 
	 * @return String
	 */
	public static String get_string(final byte[] buff, final int off, final int len) {
		if (buff[off] == 0) {
			return "";
		}
		return new String(buff, off, len);
	}

	/**
	 * Get printable string from byte array by removing junk. Assumes 8-bit
	 * ASCII characters.
	 * 
	 * @param buff
	 *            Byte array
	 * @param off
	 *            Offset
	 * @param len
	 *            Length
	 * 
	 * @return Printable string
	 */
	public static String get_printable_string(final byte[] buff, final int off, final int len) {
		if (buff[off] == 0) {
			return "";
		}
		char[] printableChars = new char[len];
		for (int i = 0; i < len; ++i) {
			printableChars[i] = (char) buff[off + i];
			if (printableChars[i] == 0) {
				// It seems string is now 0-padded, indicating end-of-string.
				printableChars[i] = 0;
				break;
			} else if ((printableChars[i] < 32) || (printableChars[i] > 126)) {
				printableChars[i] = '?';
			}
		}
		return String.valueOf(printableChars).trim();
	}

	/**
	 * Make test data.
	 * 
	 * @param len
	 *            Length
	 * @param fillP
	 *            Byte to use as fill pattern
	 * 
	 * @return Byte array
	 */
	public static byte[] makeTestData(final int len, final int fillP) {
		byte[] b = new byte[len];
		for (int i = 0; i < len; ++i) {
			b[i] = (byte) fillP;
		}
		return b;
	}

	/**
	 * Check for int.
	 * 
	 * @param test
	 *            Test string
	 * 
	 * @return True if is int string
	 */
	public static boolean isIntString(final String test) {
		if (Pattern.matches("0[bB][01]+", test)) {
			return true;
		}
		if (Pattern.matches("0[xX][0-9a-fA-F]+", test)) {
			return true;
		}
		if (Pattern.matches("[0-9][0-9]*", test)) {
			return true;
		}
		if (Pattern.matches("-[1-9][0-9]*", test)) {
			return true;
		}
		if (Pattern.matches("0[0-7]+", test)) {
			return true;
		}
		return false;
	}

	/**
	 * Check for boolean. boolean 0|1|true|false are XML Standards (note case).
	 * 
	 * @param test
	 *            Test string
	 * 
	 * @return True if boolean
	 */
	public static boolean isBooleanString(final String test) {
		if (Pattern.matches("0|1", test)) {
			return true;
		}
		if (Pattern.matches("yes|no", test)) {
			return true;
		}
		if (Pattern.matches("true|false", test)) {
			return true;
		}
		return false;
	}

	/**
	 * Parse boolean.
	 * 
	 * @param val
	 *            Value
	 * 
	 * @return As boolean
	 * 
	 * @throws NumberFormatException
	 *             Unable to parse
	 */
	public static boolean parse_boolean(final String val) throws NumberFormatException {
		if (val.equalsIgnoreCase("true")) {
			return true;
		}
		if (val.equals("1")) {
			return true;
		}
		if (val.equalsIgnoreCase("yes")) {
			return true;
		}
		if (val.equalsIgnoreCase("y")) {
			return true;
		}
		if (val.equalsIgnoreCase("false")) {
			return false;
		}
		if (val.equals("0")) {
			return false;
		}
		if (val.equalsIgnoreCase("no")) {
			return false;
		}
		if (val.equalsIgnoreCase("n")) {
			return false;
		}
		throw new NumberFormatException("boolean value not valid (0,1,true,false,yes,no,y,n) " + val);
	}

	/**
	 * Getint from boolean.
	 * 
	 * @param val
	 *            Value
	 * 
	 * @return As int
	 */
	public static int getIntFromBoolean(final boolean val) {
		return (val ? 1 : 0);
	}

	/**
	 * Get boolean from int.
	 * 
	 * @param val
	 *            Value
	 * 
	 * @return As boolean
	 */
	public static boolean getBooleanFromInt(final int val) {
		return (val != 0);
	}

	/**
	 * Get double into byte array as bits.
	 * 
	 * @param buff
	 *            Byte array
	 * @param coff
	 *            Array offset
	 * @param startBit
	 *            Start bit
	 * @param bitLength
	 *            Bit length
	 * 
	 * @return As double
	 */
	public static double get_double(final byte[] buff, final int coff, final int startBit, final int bitLength) {
		// Method: get_double
		// Enclosing Type: GDR
		long lval;
		double dval;
		long highv = (get_u32(buff, coff, startBit, bitLength) << 32) & 0xffffffff00000000L;
		long lowv = get_u32(buff, coff + 4, startBit, bitLength) & 0xffffffffL;
		lval = (highv | lowv);
		dval = Double.longBitsToDouble(lval);
		return dval;
	}

	/**
	 * Set double into byte array as bits.
	 * 
	 * @param buff
	 *            Byte array
	 * @param off
	 *            Offset
	 * @param val
	 *            Length
	 * @param startBit
	 *            Start bit
	 * @param bitLength
	 *            Bit length
	 * 
	 * @return 8
	 */
	public static int set_double(final byte[] buff, final int off, final double val, final int startBit,
			final int bitLength) {
		// Method: set_double
		// Enclosing Type: GDR
		long lval = Double.doubleToLongBits(val);
		long lowv, high;
		lowv = (0xffffffffL & lval);
		high = (0xffffffffL & (lval >> 32));
		GDR.set_u32(buff, off, (int) high, startBit, bitLength);
		GDR.set_u32(buff, off + 4, (int) lowv, startBit, bitLength);
		return 8;
	}

}
