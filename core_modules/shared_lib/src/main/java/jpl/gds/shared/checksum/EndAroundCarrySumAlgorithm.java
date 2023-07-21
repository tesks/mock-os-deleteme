/*
 * Copyright 2006-2019. California Institute of Technology.
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

import java.nio.ByteBuffer;

/**
*
* This class implements the end around carry sum encoding algorithm
* similar to that used by the TCP/IP protocol stack.  This algorithm
* is generally used to calculate the frame error correction field (FECF)
* on telecommand frames.
* 
*
*/
public class EndAroundCarrySumAlgorithm
{
	/**
     * Takes the given input and returns a checksum computed
     * over the input using the algorithm embodied by the
     * implementing algorithm.  This algorithm returns a 16 bit
     * checksum over the data.
     *
     *
     * @param data byte[] the data over which the checksum will be
     *                    computed by the algorithm
     *
     * @return byte[] the checksum resulting from the computation over
     *                the input data by the algorithm, 16 bits in 
     *                two bytes with the high order bit being the leftmost
     *                bit of the first byte returned
     */
    public static byte[] doEncode(final byte[] data) 
    {
    	return(doEncode(data,(short)0x55aa));
    }
    
	/**
     * Takes the given input and returns a checksum computed
     * over the input using the algorithm embodied by the
     * implementing algorithm.  This algorithm returns a 16 bit
     * checksum over the data.
     *
     *
     * @param data byte[] the data over which the checksum will be
     *                    computed by the algorithm
     * @param seed short  Seed for algorithm
     *
     * @return byte[] the checksum resulting from the computation over
     *                the input data by the algorithm, 16 bits in 
     *                two bytes with the high order bit being the leftmost
     *                bit of the first byte returned
     */
    public static byte[] doEncode(final byte[] data, final short seed)
    {
    	//this algorithm returns a 16 bit checksum
    	byte[] checksum = {(byte)0, (byte)0};
    	
    	//ByteBuffers are used throughout to accomodate required bit
    	//and byte manipulation made difficult due to a lack
    	//of "unsigned" data types in java
    	if(data != null)
    	{
    		ByteBuffer byteBuffer = null;
    		//if we have an odd number of bytes in the data 
    		//the final byte calculation requires appending
    		//one zero byte and doing the final calculation
    		//on the resulting 16 bits - per the 
    		//algorithm specification
    		if(data.length%2 == 0)
    		{
    			byteBuffer = ByteBuffer.wrap(data);
    		}
    		else //append an extra 0 byte
    		{
    			byte[] appendedBytes = new byte[data.length+1];
    			System.arraycopy(data, 0, appendedBytes, 0, data.length);
    			appendedBytes[appendedBytes.length-1] = 0;
    			byteBuffer = ByteBuffer.wrap(appendedBytes);
    		}

    		//a 32 bit buffer is used during calculation
    		//to avoid overflows possible from strictly short type addition
    		//continued use of the intBuffer is necessary to avoid unwanted
    		//promotion of the sign bit of the 16 bit segments taken
    		//which would result if a simple short data type were
    		//used in the addition part of the calculation
    		ByteBuffer intBuffer = ByteBuffer.allocate(4);
    		intBuffer.putShort(2, seed);
    		int sum = intBuffer.getInt();
    		while(byteBuffer.hasRemaining())
    		{
    			//data is computed 16 bits at a time
    			intBuffer.putShort(2, byteBuffer.getShort());
    			sum += intBuffer.getInt(0);
    			if ((sum & 0xffff0000) != 0) 
    			{
    				sum++;
    				sum &= 0x0000ffff;
    			}
    		}

    		//the 2 low order bytes of the intBuffer now contain
    		//the checksum
    		intBuffer.putInt(0, sum);
    		checksum[0] = intBuffer.get(2);
    		checksum[1] = intBuffer.get(3);
    	}

		return(checksum);
    }
}