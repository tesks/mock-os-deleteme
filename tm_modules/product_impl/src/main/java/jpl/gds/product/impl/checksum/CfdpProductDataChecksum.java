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
package jpl.gds.product.impl.checksum;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import jpl.gds.product.api.checksum.IProductDataChecksum;
import jpl.gds.product.api.checksum.ProductDataChecksumException;
import jpl.gds.shared.types.ByteArraySlice;

/**
 * Calculates CFDP (CCSDS File Delivery Protocol) compliant product data 
 * checksum from byte arrays or files.
 *
 */
public class CfdpProductDataChecksum implements IProductDataChecksum {
	
	private static final int maskFor_8_bitSign = makeMask(7, 1);
	private static final int leading1sFor8BitSignedInt = -256;
	private static final int BMASK = (1 << Byte.SIZE)  - 1;
	private static final long IMASK = 0x00000000ffffffffL;
	
	/**
     * Constructor
     */
    public CfdpProductDataChecksum()
    {
        super();
    }

    /**
     * Computes the checksum by separating the bytes into 4-octet words and 
     * summing these.
     *
     * The result is returned as an int.
     *
     * @param bytes the data bytes
     * @param offset the starting offset into the data byte array
     * @param length the number of bytes to check
     *
     * @return int checksum (32-bit addition of 4-octet words)
     *
     * @throws ProductDataChecksumException thrown if there is a problem when 
     *              computing the checksum
     */
    @Override
    public long computeChecksum(final byte[] bytes,
                                      final int    offset,
                                      final int    length)
        throws ProductDataChecksumException {
    	
    	if (bytes == null)
        {
            throw new ProductDataChecksumException(
                          "ProductDataChecksum.computeChecksum Null bytes");
        }

    	//checks for out-of-bounds offset and invalid length
        if ((offset < 0)            ||
            (offset > bytes.length) ||
            (length < 0)            ||
            (length > (bytes.length - offset)))
        {
            throw new ProductDataChecksumException(
                          "ProductDataChecksum.computeChecksum Found " +
                          offset                                       +
                          "/"                                          +
                          length                                       +
                          " against "                                  +
                          bytes.length);
        }

        long checksum = 0;
        
        //if the offset is not a multiple of 4, the data needs to be aligned 
        //by inserting preceding octets of value 'zero'
        final int alignmentNeeded = offset % 4;
        final byte[] alignedBytes = new byte[length + alignmentNeeded];
    	System.arraycopy(bytes, offset, alignedBytes, alignmentNeeded, length);

    	//if the length of the data is not a multiple of 4, the data needs to 
    	//be padded with trailing octets of value 'zero'
        int paddingNeeded = 4 - (alignedBytes.length % 4);
        if(paddingNeeded >= 4) {
        	paddingNeeded = 0;
        }
        final byte[] paddedBytes = new byte[alignedBytes.length + paddingNeeded];
    	System.arraycopy(alignedBytes, 0, paddedBytes, 0, alignedBytes.length);
        
    	//perform the checksum
        for(int i=0; i<paddedBytes.length; i += 4) {
        	
        	final int firstOctet = getOctet(paddedBytes[i]);
        	final int secondOctet = getOctet(paddedBytes[i+1]);
        	final int thirdOctet = getOctet(paddedBytes[i+2]);
        	final int lastOctet = getOctet(paddedBytes[i+3]);
        	
        	//form the 32-bit word
        	final long word = ((firstOctet<<24) & 0xff000000) 
        			| ((secondOctet<<16) & 0x00ff0000)
        			| ((thirdOctet<<8) & 0x0000ff00)
        			| ((lastOctet) & 0x000000ff);
        	
        	//add the words
        	checksum += word;
        }
        
        return checksum & IMASK;
    }
    
    //This method ensures that the leading sign bit for a negative 2's 
    //complement number is preserved
    private int getOctet(final byte b) {
    	if((b & maskFor_8_bitSign) > 0L) { //sign bit is 1
    		return leading1sFor8BitSignedInt | b;
    	}
    	else {
    		return b & BMASK;
    	}
    }


    
    /**
     * Compute the checksum on a list of ByteArraySlice objects.
     *
     * The result is returned as an int.
     *
     * @param bytes List of ByteArraySlice objects containing the data bytes
     *
     * @return int checksum is the addition of the computed checksum for each 
     * 			   byte slice in the list
     *
     * @throws ProductDataChecksumException thrown if there is a problem when 
     *              computing the checksum
     */
    @Override
    public long computeChecksum(final List<ByteArraySlice> bytes)
        throws ProductDataChecksumException {
    	if (bytes == null || bytes.size() == 0)
        {
            throw new ProductDataChecksumException(
                          "ProductDataChecksum.computeChecksum " +
                          "no bytes to check");
        }
      
        long checksum = 0;

        for(final ByteArraySlice slice : bytes) {
        	checksum += computeChecksum(slice.array, slice.offset, slice.length);
        }

        return checksum & IMASK;
    }


    /**
     * Computes the checksum on an entire list of bytes
     * 
     * @param bytes the data bytes to check
     *
     * @return int checksum
     *
     * @throws ProductDataChecksumException thrown if there is a problem when 
     *              computing the checksum
     */
    @Override
    public long computeChecksum(final byte[] bytes)
        throws ProductDataChecksumException {
    	if (bytes == null)
        {
            throw new ProductDataChecksumException(
                          "ProductDataChecksum.computeChecksum Null bytes");
        }

        return computeChecksum(bytes, 0, bytes.length);
    }


    /**
     * Computes the checksum on the number of bytes specified by the length in 
     * the given file starting at the offset
     *
     * @param file   the file containing the data to be checked
     * @param offset the starting offset
     * @param length the number of bytes to sum
     *
     * @return int checksum
     *
     * @throws ProductDataChecksumException thrown if there is a problem when 
     *              computing the checksum
     */
    @Override
    public long computeChecksum(final File file,
                                      final int  offset,
                                      final int  length)
        throws ProductDataChecksumException{
    	if (file == null)
        {
            throw new ProductDataChecksumException(
                          "ProductDataChecksum.computeChecksum Null file");
        }

        if (offset < 0)
        {
            throw new ProductDataChecksumException(
                          "ProductDataChecksum.computeChecksum " +
                          "Negative offset");
        }

        final int last = offset + length; // Not used if length < 0
        FileInputStream fis = null;
        int countRead = 0;
        long checksum   = 0;

        try {
            try
            {
                fis = new FileInputStream(file);
            }
            catch (final FileNotFoundException fnfe)
            {
                throw new ProductDataChecksumException(
                              "ProductDataChecksum.computeChecksum " +
                                  "File could not be opened: "       +
                                  file.getPath(),
                              fnfe);
            }
    
            int countComputed = 0;
            int lengthToBeComputed = 0;
            final byte[]    buffer = new byte[1024];
            
            while (true)
            {
            	int numberOfBytesInBuffer = 0;
    
                try
                {
                	// -1 if at end of file
                    numberOfBytesInBuffer = fis.read(buffer);
                }
                catch (final IOException ioe)
                {
                    throw new ProductDataChecksumException(
                                  "ProductDataChecksum.computeChecksum " +
                                      "Read error: "                     +
                                      file.getPath(),
                                  ioe);
                }
                
                //there is no more data because the end of file has been reached
                if (numberOfBytesInBuffer < 0)
                {
                    break;
                }
                
                countRead += numberOfBytesInBuffer;
    
                //if total length of data we want to compute checksum on is 
                //greater than how many bytes have been read, then we can 
                //calculate checksum on everything in buffer
                if(length > countRead || length == -1) {
                	lengthToBeComputed = numberOfBytesInBuffer;
                }
                //if length is less than how many bytes have been read, then we do 
                //NOT want to compute checksum on everything in buffer
                else {
                	lengthToBeComputed = length - countComputed;
                }
                
                //Might need to align data in case the buffered data does not 
                //begin at a multiple of 4
                final int alignmentNeeded = countComputed % 4;
                
                if(alignmentNeeded != 0) {
                	final byte[] alignedBytes = new byte[buffer.length + alignmentNeeded];
                	System.arraycopy(buffer, 0, alignedBytes, alignmentNeeded, lengthToBeComputed);
                	checksum += computeChecksum(alignedBytes, offset, alignedBytes.length);
                }
                else {
                	checksum += computeChecksum(buffer, offset, lengthToBeComputed);
                }
                
                countComputed = countRead;
            }
        }
        finally {
            try
            {
                fis.close();
            }
            catch (final IOException e)
            {
                // Ignore
            }
        }
        
        if (length >= 0)
        {
            if (countRead != last)
            {
                throw new ProductDataChecksumException(
                              "ProductDataChecksum.computeChecksum "        +
                                  "Did not read expected number of bytes: " +
                                  last);
            }
        }
        else if (countRead < offset)
        {
            // We should have at least read through the offset

            throw new ProductDataChecksumException(
                          "ProductDataChecksum.computeChecksum " +
                              "Did not read offset bytes: "      +
                              offset);
        }

        return checksum & IMASK;
    }


    /**
     * Computes the checksum for an entire file.
     *
     * @param file the file containing the data to be checked
     *
     * @return int checksum
     *
     * @throws ProductDataChecksumException thrown if there is a problem when 
     *              computing the checksum
     */
    @Override
    public long computeChecksum(final File file)
        throws ProductDataChecksumException {
        return computeChecksum(file, 0, -1);
    }
    
    /**
	 * Makes a mask for a single bit range
	 *
	 * @param startBit the starting bit of the bit range
	 * @param length   the length of the bit range
	 *
	 * @return the bit mask
	 */
	private static int makeMask(final int startBit,
			final int length)
	{
		final int endBit = startBit + length;
		int      result = 0;

		for (int i = startBit; i < endBit; ++i)
		{
			result |= (1 << i);
		}

		return result;
	}

}
