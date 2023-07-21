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
package jpl.gds.shared.checksum;

/**
 * This class calculates the Internet Checksum of a byte buffer (RFC 1071 -
 * http://www.faqs.org/rfcs/rfc1071.html).
 * <p>
 * The RFC-1071 algorithm is described as follows:
 * <p>
 * (1) Adjacent octets to be checksummed are paired to form 16-bit integers, and
 * the 1's complement sum of these 16-bit integers is formed.
 * <p>
 * (2) To generate a checksum, the checksum field itself is cleared, the 16-bit
 * 1's complement sum is computed over the octets concerned, and the 1's
 * complement of this sum is placed in the checksum field.
 * <p>
 * (3) To check a checksum, the 1's complement sum is computed over the same set
 * of octets, including the checksum field. If the result is all 1 bits (-0 in
 * 1's complement arithmetic), the check succeeds. But note that this does
 * not work for odd-length arrays unless a pad byte is inserted into the
 * data block containing the checksum.
 * <p>
 * 
 */
public class InternetChecksum implements IChecksumCalculator {

	/**
	 * Calculates the Internet Checksum of a byte array.
	 * 
	 * @param buffer
	 *            the array of bytes to checksum. If the checksum bytes are
	 *            included in the array, they should be cleared before
	 *            performing the computation.
	 * @param offset
	 *            the starting offset of the data to checksum in the byte array
	 * @param dataLength
	 *            the number of bytes to compute checksum across
	 * 
	 * @return the checksum value, 1's complemented and ready to write into the data
	 */
	public long calculateChecksum(byte[] buffer, int offset, int dataLength) {
		int length = dataLength;
		int i = offset;

		int sum = 0;
		int data;

		/*
		 * Handle all pairs of bytes.
		 */
		while (length > 1) {
			/*
			 * Put a pair of bytes into one integer value. Add this value to the
			 * overall sum.
			 */
			data = (((buffer[i] << 8) & 0xFF00) | ((buffer[i + 1]) & 0xFF));
			sum += data;

			i += 2;
			length -= 2;
		}

		/*
		 * Handle the remaining byte in odd length buffers.
		 */
		if (length > 0) {

			/*
			 * Create an integer from the one byte, assuming the low order by is
			 * 0, and add it to the sum.
			 * 
			 */
			sum += buffer[i] << 8;
		}

		/*
		 * Anything in the high two bytes is a carry value for the end-around-carry.
		 * Isolate that value, clear the upper two bytes of the sum, and add the 
		 * carry value to it.
		 */
		if ((sum & 0xFFFF0000) > 0) {
			int carry = (sum & 0xFFFF0000) >> 16;
			sum = sum & 0xFFFF;
			sum += carry;
		}

		/*
		 * Do a final 1's complement value correction to 16-bits
		 */
		sum = ~sum;
		sum = sum & 0xFFFF;
		return sum;

	}
}
