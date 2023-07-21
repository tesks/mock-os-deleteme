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

import jpl.gds.shared.util.BinOctHexUtility;


/**
 * This class is meant to be a replacement for the existing "BchAlgorithm" class
 * which was external code imported by MPCS.  The problem with the existing class is
 * that it is very restrictive on the number of bytes that can be supplied to algorithm
 * and so a lot of cases, especially ones that come up in the fault injector, cause the
 * other BchAlgorithm class to throw exceptions even though a valid calculation can
 * still be done.
 * 
 * See the CCSDS TC Synchronization and Channel Coding Blue Book for the genesis
 * of this algorithm.
 * 
 * 
 */
public class NewBchAlgorithm
{
	/**
	 * Generate the Error Detection & Correction (EDAC) value for a byte array.
	 * 
	 * @param data The array whose EDAC should be calculated
	 * 
	 * @return The byte[] representation of the EDAC for the input data
	 */
	public static byte[] doEncode(final byte[] data)
	{
		String bits = BinOctHexUtility.toBinFromBytes(data);
		String encodedBits = doEncode(bits);
		return(BinOctHexUtility.toBytesFromBin(encodedBits));
	}
	
	/**
	 * Generate the Error Detection & Correction (EDAC) value for a bit string.
	 * 
	 * @param bits The bit string whose EDAC should be calculated
	 * 
	 * @return The bit string representation of the EDAC for the input data.
	 */
	public static String doEncode(final String bits)
	{
		byte in = 0;
		byte x0 = 0;
		byte x1 = 0;
		byte x2 = 0;
		byte x3 = 0;
		byte x4 = 0;
		byte x5 = 0;
		byte x6 = 0;
		
		//run the generator polynomial
		for(int i=0; i < bits.length(); i++)
		{
			in = (byte)(Byte.valueOf(String.valueOf(bits.charAt(i))) ^ x6);
			x6 = (byte)(in ^ x5);
			x5 = x4;
			x4 = x3;
			x3 = x2;
			x2 = (byte)(in ^ x1);
			x1 = x0;
			x0 = in;
		}
		
		//invert the bits
		StringBuilder sb = new StringBuilder(8);
		byte[] result = new byte[] { x6, x5, x4, x3, x2, x1, x0 };
		for(int i=0; i < result.length; i++)
		{
			sb.append(result[i] == 0 ? 1 : 0);
		}
		//EDAC is 7 bits long, add 1 bit of fill
		sb.append("0");
		
		return(sb.toString());
	}
}
