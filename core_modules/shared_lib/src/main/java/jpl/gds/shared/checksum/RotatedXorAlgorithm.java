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

import java.util.Arrays;

import jpl.gds.shared.gdr.GDR;


/**
 * This class calculates a Rotated XOR Checksum on a byte array.  This checksum can
 * work with block size N = 8, 16, or 32 (hence the three functions).  In a normal XOR checksum,
 * the first block is XORed with the second block, the second block is XORed with the third block, and
 * so on until the end of the input is reached.  In a Rotated XOR checksum, the intermediate checksum is rotated
 * circularly left 1 bit after each XOR.  The rotation is necessary for reducing collisions in checksums over
 * data like ASCII text which always has a 0 value for the uppermost bit.
 *
 *
 */
public class RotatedXorAlgorithm
{
	/**
	 * Calculate a rotated checksum with a block size of 1 byte.  The initial
	 * checksum value starts at zero before any operations are done.
	 *
	 * @param bytes The bytes to calculate a checksum over
	 *
	 * @return The 8-bit checksum for the input bytes
	 */
	public static short calculate8BitChecksum(final byte[] bytes)
	{
		return(calculate8BitChecksum(bytes,(byte)0x00));
	}

	/**
	 * Calculate a rotated checksum with a block size of 1 byte
	 *
	 * @param bytes The bytes to calculate a checksum over
	 * @param startValue The starting value of the checksum field before any operations are done
	 *
	 * @return The 8-bit checksum for the input bytes
	 */
	public static short calculate8BitChecksum(final byte[] bytes,final byte startValue)
	{
		if(bytes == null)
		{
			throw new IllegalArgumentException("Null input byte array");
		}

		byte checksum = startValue;

		for(int i=0; i < bytes.length; i++)
		{
			//circular rotate left
			int firstBit = ((checksum & 0x80) >>> 7) & 0x01;
			checksum = (byte)((checksum << 1) | firstBit);

			//XOR
			checksum = (byte)(checksum ^ bytes[i]);
		}

		return((short)(((short)0x00FF) & checksum));
	}

	/**
	 * Calculate a rotated checksum with a block size of 2 bytes.  The initial
	 * checksum value starts at zero before any operations are done.
	 *
	 * @param bytes The bytes to calculate a checksum over
	 *
	 * @return The 16-bit checksum for the input bytes
	 */
	public static int calculate16BitChecksum(final byte[] bytes)
	{
		return(calculate16BitChecksum(bytes,(short)0x0000));
	}

	/**
	 * Calculate a rotated checksum with a block size of 2 bytes
	 *
	 * @param bytes The bytes to calculate a checksum over
	 * @param startValue The starting value of the checksum field before any operations are done
	 *
	 * @return The 16-bit checksum for the input bytes
	 */
	public static int calculate16BitChecksum(final byte[] bytes,final short startValue)
	{
		byte[] tempBytes = null;
		if(bytes == null)
		{
			throw new IllegalArgumentException("Null input byte array");
		}
		else if((bytes.length % 2) != 0)
		{
			tempBytes = new byte[bytes.length+1];
			Arrays.fill(tempBytes,(byte)0x00);
			System.arraycopy(bytes,0,tempBytes,0,bytes.length);
		}
		else
		{
			tempBytes = bytes;
		}

		short checksum = startValue;

		for(int i=0; i < tempBytes.length; i += 2)
		{
			//circular rotate left
			int firstBit = ((checksum & 0x8000) >>> 15) & 0x0001;
			checksum = (short)((checksum << 1) | firstBit);

			//get the next block from the input array
			short currentBlock = (short)GDR.get_u16(tempBytes,i);

			//XOR
			checksum = (short)(checksum ^ currentBlock);
		}

		return(0x0000FFFF & checksum);
	}


	/**
	 * Calculate a rotated checksum with a block size of 4 bytes.  The initial
	 * checksum value starts at zero before any operations are done.
	 *
	 * @param bytes The bytes to calculate a checksum over
	 *
	 * @return The 32-bit checksum for the input bytes
	 */
	public static long calculate32BitChecksum(final byte[] bytes)
	{
		return(calculate32BitChecksum(bytes,0x00000000));
	}

	/**
	 * Calculate a rotated checksum with a block size of 4 bytes
	 *
	 * @param bytes The bytes to calculate a checksum over
	 * @param startValue The starting value of the checksum field before any operations are done
	 *
	 * @return The 32-bit checksum for the input bytes
	 */
	public static long calculate32BitChecksum(final byte[] bytes,final int startValue)
	{
		byte[] tempBytes = null;
		if(bytes == null)
		{
			throw new IllegalArgumentException("Null input byte array");
		}
		else if((bytes.length % 4) != 0)
		{
			int numZeroBytes = 4 - (bytes.length % 4);
			tempBytes = new byte[bytes.length + numZeroBytes];
			Arrays.fill(tempBytes,(byte)0x00);
			System.arraycopy(bytes,0,tempBytes,0,bytes.length);
		}
		else
		{
			tempBytes = bytes;
		}

		int checksum = startValue;

		for(int i=0; i < tempBytes.length; i += 4)
		{
			//circular rotate left
			int firstBit = ((checksum & 0x80000000) >>> 31) & 0x00000001;
			checksum = (checksum << 1) | firstBit;

			//get the next block from the input array
			int currentBlock = (int)GDR.get_u32(tempBytes,i);

			//XOR
			checksum = checksum ^ currentBlock;
		}

		return(0x00000000FFFFFFFFL & checksum);
	}
}