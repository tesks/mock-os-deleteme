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
package jpl.gds.shared.gdr;

import java.util.regex.Pattern;

import jpl.gds.shared.util.BinOctHexUtility;

/**
 * This class defines static methods for the standard mapping between
 * <b>CCSDS</b> standard Global Data Representation data items and Java data.
 * 
 */
public class GDR {
    
    /**
     * A piece of whitespace (includes space, newline, carriage return, tab,
     * etc.)
     */
    public static final String WHITESPACE_REGEXP = "[ \r\n\t\b\f\0]{1,}";
    
	/** GDR 8 size */
	public static final int GDR_8_SIZE = 1;

	/** GDR 16 size */
	public static final int GDR_16_SIZE = 2;

	/** GDR 24 size */
	public static final int GDR_24_SIZE = 3;

	/** GDR 32 size */
	public static final int GDR_32_SIZE = 4;

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
	private static final int bitMask[] = { 0x0, 0x1, 0x3, 0x7, 0xf, 0x1f, 0x3f,
			0x7f, 0xff, 0x1ff, 0x3ff, 0x7ff, 0xfff, 0x1fff, 0x3fff, 0x7fff,
			0xffff, 0x1ffff, 0x3ffff, 0x7ffff, 0xfffff, 0x1fffff, 0x3fffff,
			0x7fffff, 0xffffff, 0x1ffffff, 0x3ffffff, 0x7ffffff, 0xfffffff,
			0x1fffffff, 0x3fffffff, 0x7fffffff, 0xffffffff,

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
	public static int makeBitMask(final int length)
			throws IllegalArgumentException {
		if (length < 0 || length > 32) {
			throw new IllegalArgumentException(
					"makeBitMask length must be in the range 0..32 " + length);
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
	public static String fillStr(final String val, final int fillSize,
			final char fillChar) {
		final StringBuilder tmp = new StringBuilder(1024);
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
	public static String leftFillStr(final String val, final int fillSize,
			final char fillChar) {
		final StringBuilder tmp = new StringBuilder(1024);
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
	public static String stringValue(final byte buff[], final int off,
			final int length) {
		final StringBuilder s = new StringBuilder(1024);
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
		val = ((0xff & buff[off]) << 16) | ((0xff & buff[off + 1]) << 8)
				| (0xff & buff[off + 2]);
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
		final long val = ((buff[off] << 24) & 0xff000000L)
				| ((buff[off + 1] << 16) & 0x00ff0000L)
				| ((buff[off + 2] << 8) & 0x0000ff00L)
				| ((buff[off + 3]) & 0x000000ffL);
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
		final long highv = (get_u32(buff, off) << 32) & 0xffffffff00000000L;
		final long lowv = get_u32(buff, off + 4) & 0xffffffffL;
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
	public static long getSignedInteger(final byte[] bytes, final int offset,
			final int bitLength) throws IllegalArgumentException {
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
					"A bit length of "
							+ bitLength
							+ " is not supported by the getSignedInteger(...) function.");
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
	public static long getUnsignedInteger(final byte[] bytes, final int offset,
			final int bitLength) throws IllegalArgumentException {
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
					"A bit length of "
							+ bitLength
							+ " is not supported by the getUnsignedInteger(...) function.");
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
	public static double getFloatingPoint(final byte[] bytes, final int offset,
			final int bitLength) throws IllegalArgumentException {
		switch (bitLength) {
		case 32:
			return (get_float(bytes, offset));

		case 64:
			return (GDR.get_double(bytes, offset));

		default:

			throw new IllegalArgumentException(
					"A bit length of "
							+ bitLength
							+ " is not supported by the getFloatingPoint(...) function.");
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
		val = ((0xff & buff[off]) << 16) | ((0xff & buff[off + 1]) << 8)
				| (0xff & buff[off + 2]);
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
		val = ((0xff & buff[off]) << 24) | ((0xff & buff[off + 1]) << 16)
				| ((0xff & buff[off + 2]) << 8) | (0xff & buff[off + 3]);
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
		val = ((0xffL & buff[off]) << 56) | ((0xffL & buff[off + 1]) << 48)
				| ((0xffL & buff[off + 2]) << 40)
				| ((0xffL & buff[off + 3]) << 32)
				| ((0xffL & buff[off + 4]) << 24)
				| ((0xffL & buff[off + 5]) << 16)
				| ((0xffL & buff[off + 6]) << 8) | (0xffL & buff[off + 7]);
		return val;
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
	public static int get_i8(final byte[] buff, int off, int firstBit,
			final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 8) {
			throw new IllegalArgumentException(
					"bit length cannot be greater than 8");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		int val;
		final int needBits = firstBit + bitLength;
		if (needBits <= 8) {
			if (buff.length < off + 1) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
			}
			val = get_i8(buff, off);
			shift = 8 - needBits;
		} else {
			if (buff.length < off + 2) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
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
	public static int get_u8(final byte[] buff, int off, int firstBit,
			final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 8) {
			throw new IllegalArgumentException(
					"bit length cannot be greater than 8");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		int val;
		final int needBits = firstBit + bitLength;
		if (needBits <= 8) {
			if (buff.length < off + 1) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
			}
			val = get_u8(buff, off);
			shift = 8 - needBits;
		} else {
			if (buff.length < off + 2) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
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
	public static int get_i16(final byte[] buff, int off, int firstBit,
			final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 16) {
			throw new IllegalArgumentException(
					"bit length cannot be greater than 16");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		int val;
		final int needBits = firstBit + bitLength;
		if (needBits <= 16) {
			if (buff.length < off + 2) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
			}
			val = get_i16(buff, off);
			shift = 16 - needBits;
		} else {
			if (buff.length < off + 3) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
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
	public static int get_u16(final byte[] buff, int off, int firstBit,
			final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 16) {
			throw new IllegalArgumentException(
					"bit length cannot be greater than 16");
		}
		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		int val;
		final int needBits = firstBit + bitLength;
		if (needBits <= 16) {
			if (buff.length < (off + 2)) {
				throw new IllegalArgumentException(
						"(1) Not enough bytes in buffer to get " + bitLength
								+ " bits off=" + off + " buff.length="
								+ buff.length);
			}
			val = get_u16(buff, off);
			shift = 16 - needBits;
		} else {
			if (buff.length < (off + 3)) {
				System.out.println("get_U16 Exception 1 buff.length="
						+ buff.length + " off=" + off);
				throw new IllegalArgumentException(
						"(2) Not enough bytes in buffer to get " + bitLength
								+ " bits off=" + off + " buff.length="
								+ buff.length);
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
	public static int get_i24(final byte[] buff, int off, int firstBit,
			final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 24) {
			throw new IllegalArgumentException(
					"bit length cannot be greater than 24");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		long val;
		final int needBits = firstBit + bitLength;
		if (needBits <= 24) {
			if (buff.length < off + 3) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
			}
			val = get_i24(buff, off);
			shift = 24 - needBits;
		} else {
			if (buff.length < off + 4) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
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
	public static int get_u24(final byte[] buff, int off, int firstBit,
			final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 24) {
			throw new IllegalArgumentException(
					"bit length cannot be greater than 24");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		long val;
		final int needBits = firstBit + bitLength;
		if (needBits <= 24) {
			if (buff.length < off + 3) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
			}
			val = get_u24(buff, off);
			shift = 24 - needBits;
		} else {
			if (buff.length < off + 4) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
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
	public static long get_i32(final byte[] buff, int off, int firstBit,
			final int bitLength) {
		long mask;
		int shift;

		if (bitLength > 32) {
			throw new IllegalArgumentException(
					"bit length cannot be greater than 32");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		long val;
		final int needBits = firstBit + bitLength;
		if (needBits <= 32) {
			if (buff.length < off + 4) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
			}
			val = get_i32(buff, off);
			shift = 32 - needBits;
		} else {
			if (buff.length < off + 5) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
			}
			val = ((0xffL & buff[off]) << 32) | ((0xffL & buff[off + 1]) << 24)
					| ((0xffL & buff[off + 2]) << 16)
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
	public static long get_i64(final byte[] buff, final int off,
			final int firstBit, final int bitLength) {
		long mask;

		if (bitLength > 64) {
			throw new IllegalArgumentException(
					"bit length cannot be greater than 64");
		}

		if (bitLength < 32) {
			mask = bitMask[bitLength];
		} else {
			mask = ((long) (bitMask[bitLength - 32]) << 32) | 0xffffffff;
		}
		long val;
		final int needBits = firstBit + bitLength;
		if (needBits <= 64) {
			if (buff.length < off + 8) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
			}
			val = get_i64(buff, off);
		} else {
			throw new IllegalArgumentException(
					"Not enough bytes in buffer to get " + bitLength + " bits");
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
	public static long get_u32(final byte[] buff, int off, int firstBit,
			final int bitLength) {
		int mask;
		int shift;

		if (bitLength > 32) {
			throw new IllegalArgumentException(
					"bit length cannot be greater than 32");
		}

		if (firstBit >= 8) {
			off += firstBit / 8;
			firstBit = firstBit % 8;
		}
		mask = bitMask[bitLength];
		long val;
		final int needBits = firstBit + bitLength;
		if (needBits <= 32) {
			if (buff.length < off + 4) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
			}
			val = get_u32(buff, off);
			shift = 32 - needBits;
		} else {
			if (buff.length < off + 5) {
				throw new IllegalArgumentException(
						"Not enough bytes in buffer to get " + bitLength
								+ " bits");
			}
			val = (((long) buff[off] << 32) & 0xff00000000L)
					| ((buff[off + 1] << 24) & 0x00ff000000L)
					| ((buff[off + 2] << 16) & 0x0000ff0000L)
					| ((buff[off + 3] << 8) & 0x000000ff00L)
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
		final int ival = (int) (0xffffffff & lval);
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
		final long highv = (get_u32(buff, off) << 32) & 0xffffffff00000000L;
		final long lowv = get_u32(buff, off + 4) & 0xffffffffL;
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
			throw new IllegalArgumentException(
					"value is too large to assign to 3 bytes");
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
	public static int set_float(final byte[] buff, final int off,
			final double val) {
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
	public static int set_double(final byte[] buff, final int off,
			final double val) {
		final long lval = Double.doubleToLongBits(val);
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
	public static int set_string(final byte[] buff, final int off,
			final String val, int len) {
		int i;
		if (len > val.length()) {
			len = val.length();
		}
		for (i = 0; i < len; ++i) {
			final char ch = val.charAt(i);
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
	public static int set_string_no_pad(final byte[] buff, final int off,
			final String val) {
		int i;
		final int len = val.length();

		for (i = 0; i < len; ++i) {
			final char ch = val.charAt(i);
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
	public static int set_string(final byte[] buff, final int off,
			final String val) {
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
	public static int set_u8(final byte[] buff, final int off, final int val,
			final int first_bit, final int bit_length)
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
	public static int set_i8(final byte[] buff, final int off, final int val,
			final int first_bit, final int bit_length)
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
	public static int set_u16(final byte[] buff, final int off, final int val,
			final int first_bit, final int bit_length)
			throws NumberFormatException {
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
	public static int set_i16(final byte[] buff, final int off, final int val,
			final int first_bit, final int bit_length)
			throws NumberFormatException {
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
	public static int set_u32(final byte[] buff, final int off, final long val,
			final int first_bit, final int bit_length)
			throws NumberFormatException {
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
	public static int set_i32(final byte[] buff, final int off, final int val,
			final int first_bit, final int bit_length)
			throws NumberFormatException {
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
		final byte temp[] = { buff[off], 0, 0, buff[off + 1] };

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
		final int temp = (int) GDR.get_u32(buff, off);

		final int sign = (temp & 0x80000000);

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

		final int exponent = buff[3] + 1023 - expadjust;

		final int hiword = sign | (exponent << 20) | (mantissa >>> 2);
		final int loword = mantissa << 30;

		final byte[] dbuff = new byte[8];
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
		final long hitemp = GDR.get_u32(buff, off);
		final long lotemp = GDR.get_u32(buff, off + 4);

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

		final long hiword = (hitemp & 0x80000000L)
				| ((buff[3] + 1023 - expadjust) << 20) | (himantissa >>> 2);
		final long loword = (himantissa << 30) | (lomantissa << 14);

		final byte[] dbuff = new byte[8];
		GDR.set_u32(dbuff, 4, (int) (0xffffffffL & hiword));
		GDR.set_u32(dbuff, 0, (int) (0xffffffffL & loword));
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
	public static int cbits_to_u8(final byte[] buff, final int off,
			final int bitoff, final int size) {
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
	public static int cbits_to_u16(final byte[] buff, final int off,
			final int bitoff, final int size) {
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
	public static int u8_to_cbits(final byte[] buff, final int off,
			final int val, final int bitoff, final int size) {
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
	public static int u16_to_cbits(final byte[] buff, final int off,
			final int val, final int bitoff, final int size) {
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
	public static String get_string(final byte[] buff, final int off,
			final int len) {
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
	public static String get_printable_string(final byte[] buff, final int off,
			final int len) {
		if (buff[off] == 0) {
			return "";
		}
		final char[] printableChars = new char[len];
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
	 * Parse byte.
	 * 
	 * @param inputVal
	 *            Value
	 * 
	 * @return As byte
	 * 
	 * @throws NumberFormatException
	 *             Unable to parse
	 */
	public static byte parse_byte(final String inputVal)
			throws NumberFormatException {
		byte returnVal = 0;
		if (BinOctHexUtility.hasBinaryPrefix(inputVal)) {
			String bitString = BinOctHexUtility.stripBinaryPrefix(inputVal);
			bitString = GDR.fillStr(bitString, Byte.SIZE, bitString.charAt(0));
			final byte[] bytes = BinOctHexUtility.toBytesFromBin(bitString);
			returnVal = (byte) GDR.get_i8(bytes, 0);
		} else if (BinOctHexUtility.hasHexPrefix(inputVal)) {
			final String hexString = BinOctHexUtility.stripHexPrefix(inputVal);
			String bitString = BinOctHexUtility.toBinFromHex(hexString);
			bitString = GDR.fillStr(bitString, Byte.SIZE, bitString.charAt(0));
			final byte[] bytes = BinOctHexUtility.toBytesFromBin(bitString);
			returnVal = (byte) GDR.get_i8(bytes, 0);
		} else {
			returnVal = Byte
					.parseByte(inputVal, BinOctHexUtility.DECIMAL_RADIX);
		}

		return (returnVal);
	}

	/**
	 * Parse short.
	 * 
	 * @param inputVal
	 *            Value
	 * 
	 * @return As short
	 * 
	 * @throws NumberFormatException
	 *             Unable to parse
	 */
	public static short parse_short(final String inputVal)
			throws NumberFormatException {
		short returnVal = 0;
		if (BinOctHexUtility.hasBinaryPrefix(inputVal)) {
			String bitString = BinOctHexUtility.stripBinaryPrefix(inputVal);
			bitString = GDR.fillStr(bitString, Short.SIZE, bitString.charAt(0));
			final byte[] bytes = BinOctHexUtility.toBytesFromBin(bitString);
			returnVal = (short) GDR.get_i16(bytes, 0);
		} else if (BinOctHexUtility.hasHexPrefix(inputVal)) {
			final String hexString = BinOctHexUtility.stripHexPrefix(inputVal);
			String bitString = BinOctHexUtility.toBinFromHex(hexString);
			bitString = GDR.fillStr(bitString, Short.SIZE, bitString.charAt(0));
			final byte[] bytes = BinOctHexUtility.toBytesFromBin(bitString);
			returnVal = (short) GDR.get_i16(bytes, 0);
		} else {
			returnVal = Short.parseShort(inputVal,
					BinOctHexUtility.DECIMAL_RADIX);
		}

		return (returnVal);
	}

	/**
	 * Parse int.
	 * 
	 * @param inputVal
	 *            Value
	 * 
	 * @return As int
	 * 
	 * @throws NumberFormatException
	 *             Unable to parse
	 */
	public static int parse_int(final String inputVal) throws NumberFormatException {
		int returnVal = 0;
		if (BinOctHexUtility.hasBinaryPrefix(inputVal)) {
			String bitString = BinOctHexUtility.stripBinaryPrefix(inputVal);
			bitString = GDR.fillStr(bitString, Integer.SIZE,
					bitString.charAt(0));
			final byte[] bytes = BinOctHexUtility.toBytesFromBin(bitString);
			returnVal = GDR.get_i32(bytes, 0);
		} else if (BinOctHexUtility.hasHexPrefix(inputVal)) {
			final String hexString = BinOctHexUtility.stripHexPrefix(inputVal);
			String bitString = BinOctHexUtility.toBinFromHex(hexString);
			bitString = GDR.fillStr(bitString, Integer.SIZE,
					bitString.charAt(0));
			final byte[] bytes = BinOctHexUtility.toBytesFromBin(bitString);
			returnVal = GDR.get_i32(bytes, 0);
		} else {
			returnVal = Integer.parseInt(inputVal);
		}

		return (returnVal);
	}

	/**
	 * Parse long.
	 * 
	 * @param inputVal
	 *            Value
	 * 
	 * @return As long
	 * 
	 * @throws NumberFormatException
	 *             Unable to parse
	 */
	public static long parse_long(final String inputVal)
			throws NumberFormatException {
		long returnVal = 0;
		if (BinOctHexUtility.hasBinaryPrefix(inputVal)) {
			String bitString = BinOctHexUtility.stripBinaryPrefix(inputVal);
			bitString = GDR.fillStr(bitString, Long.SIZE, bitString.charAt(0));
			final byte[] bytes = BinOctHexUtility.toBytesFromBin(bitString);
			returnVal = GDR.get_i64(bytes, 0);
		} else if (BinOctHexUtility.hasHexPrefix(inputVal)) {
			final String hexString = BinOctHexUtility.stripHexPrefix(inputVal);
			String bitString = BinOctHexUtility.toBinFromHex(hexString);
			bitString = GDR.fillStr(bitString, Long.SIZE, bitString.charAt(0));
			final byte[] bytes = BinOctHexUtility.toBytesFromBin(bitString);
			returnVal = GDR.get_i64(bytes, 0);
		} else {
			returnVal = Long.parseLong(inputVal);
		}

		return (returnVal);
	}

	/**
	 * Parse unsigned.
	 * 
	 * @param inputVal
	 *            Value
	 * 
	 * @return As long
	 * 
	 * @throws NumberFormatException
	 *             Unable to parse
	 */
	public static long parse_unsigned(final String inputVal)
			throws NumberFormatException {
		long returnVal = 0;
		String bitString = null;
		if (BinOctHexUtility.hasBinaryPrefix(inputVal)) {
			bitString = BinOctHexUtility.stripBinaryPrefix(inputVal);
		} else if (BinOctHexUtility.hasHexPrefix(inputVal)) {
			final String hexString = BinOctHexUtility.stripHexPrefix(inputVal);
			bitString = BinOctHexUtility.toBinFromHex(hexString);
		} else {
			bitString = Long.toBinaryString(Long.parseLong(inputVal));
		}

		bitString = GDR.fillStr(bitString, Long.SIZE, '0');
		if (bitString.charAt(0) == '1') {
			throw new NumberFormatException("Input value " + inputVal
					+ " is too large to be converted to an unsigned value.");
		}

		final byte[] bytes = BinOctHexUtility.toBytesFromBin(bitString);
		returnVal = GDR.get_u64(bytes, 0);

		return (returnVal);
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
	public static boolean parse_boolean(final String val)
			throws NumberFormatException {
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
		throw new NumberFormatException(
				"boolean value not valid (0,1,true,false,yes,no,y,n) " + val);
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
	 * Extract float from bit string.
	 * 
	 * @param bitString
	 *            String
	 * 
	 * @return Result
	 */
	public static float getFloatFromBits(String bitString) {
		if (BinOctHexUtility.isValidBin(bitString) == false) {
			throw new IllegalArgumentException(
					"Input bit string is not a valid binary string");
		} 
		/*
		 *  Length of input string was
		 * being checked to see if it matches size of float BEFORE
		 * the binary prefix was stripped. I reversed the order.
		 */
		if (BinOctHexUtility.hasBinaryPrefix(bitString)) {
		    bitString = BinOctHexUtility.stripBinaryPrefix(bitString);
		}
		if (bitString.length() != Float.SIZE) {
		    throw new IllegalArgumentException("Input bit string \""
		            + bitString + "\" is not the correct length of "
		            + Float.SIZE + " bits for a float value.");
		}


		int result = 0x00000000;
		for (int i = 0; i < bitString.length(); i++) {
		    final int c = Character.digit(bitString.charAt(i),
		            BinOctHexUtility.BINARY_RADIX);
		    result = (result << 1) | c;
		}

		return (Float.intBitsToFloat(result));
	}

	/**
	 * Extract float from hex string.
	 * 
	 * @param hexString
	 *            String
	 * 
	 * @return Result
	 */
	public static float getFloatFromHex(final String hexString) {
		if (BinOctHexUtility.isValidHex(hexString) == false) {
			throw new IllegalArgumentException(
					"Input hex string is not a valid hexadecimal string");
		} 
		
		/*
		 * Length of input string was
		 * being checked to see if it matches size of float BEFORE
		 * the hex prefix was stripped. I reversed the order.
		 */
		String localHexString = hexString;

		if (BinOctHexUtility.hasHexPrefix(hexString)) {
		    localHexString = BinOctHexUtility.stripHexPrefix(hexString);
		}

		if (localHexString.length() != (Float.SIZE / 4)) {
		    throw new IllegalArgumentException("Input hex string \""
		            + hexString + "\" is not the correct length of "
		            + Float.SIZE + " bits for a float value.");
		}

		return (getFloatFromBits(BinOctHexUtility.toBinFromHex(hexString)));
	}

	/**
	 * Extract double from bit string.
	 * 
	 * @param bitString
	 *            String
	 * 
	 * @return Result
	 */
	public static double getDoubleFromBits(String bitString) {
		if (BinOctHexUtility.isValidBin(bitString) == false) {
			throw new IllegalArgumentException("Input bit string \""
					+ bitString + "\" is not a valid binary string");
		} 
		
		/*
		 * Length of input string was
		 * being checked to see if it matches size of double BEFORE
		 * the binary prefix was stripped. I reversed the order.
		 */
		if (BinOctHexUtility.hasBinaryPrefix(bitString)) {
		    bitString = BinOctHexUtility.stripBinaryPrefix(bitString);
		}

		if (bitString.length() != Double.SIZE) {
		    throw new IllegalArgumentException(
		            "Input bit string is not the correct length of "
		                    + Double.SIZE + " bits for a float value.");
		}

		long result = 0x0000000000000000L;
		for (int i = 0; i < bitString.length(); i++) {
		    final int c = Character.digit(bitString.charAt(i),
		            BinOctHexUtility.BINARY_RADIX);
		    result = (result << 1) | c;
		}

		return (Double.longBitsToDouble(result));
	}

	/**
	 * Extract double from hex string.
	 * 
	 * @param hexString
	 *            String
	 * 
	 * @return Result
	 */
	public static double getDoubleFromHex(final String hexString) {
	    if (BinOctHexUtility.isValidHex(hexString) == false) {
			throw new IllegalArgumentException(
					"Input hex string is not a valid hexadecimal string");
		}

	    /*
	     * Length of input string was
	     * being checked to see if it matches size of double BEFORE
	     * the hex prefix was stripped. I reversed the order.
	     */
	    String localHexString = hexString;

	    if (BinOctHexUtility.hasHexPrefix(hexString)) {
	        localHexString = BinOctHexUtility.stripHexPrefix(hexString);
	    }
	    if (localHexString.length() != (Double.SIZE / 4)) {
	        throw new IllegalArgumentException("Input hex string \""
	                + hexString + "\" is not the correct length of "
	                + Double.SIZE + " bits for a double value.");
		}

		return (getDoubleFromBits(BinOctHexUtility.toBinFromHex(hexString)));
	}

	/**
	 * Remove white-space from string.
	 * 
	 * @param in
	 *            String
	 * 
	 * @return Result
	 */
	public static String removeWhitespaceFromString(final String in) {
		return (in.replaceAll(WHITESPACE_REGEXP, ""));
	}
}
