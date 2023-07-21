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
package jpl.gds.shared.time;

import java.util.Map;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.types.BitBuffer;

/**
 * A class for extracting coarse-fine time codes from byte arrays.
 * 
 *
 */
public class CoarseFineExtractor implements ISclkExtractor {
	
	private final CoarseFineEncoding targetEncoding;
	private final CoarseFineEncoding sourceEncoding;

	/**
	 * Constructor.
	 * 
	 * @param encoding the coarse-fine encoding definition object 
	 */
	public CoarseFineExtractor(CoarseFineEncoding encoding) {
		targetEncoding = encoding;
		sourceEncoding = encoding;
	}

	/** 
	 * Create a CoarseFineExtractor using a number of source bits that may differ from
	 * the target size.  Use constructor that only takes a CoarseFineEncoding object if there is no
	 * deviation.
	 * @param targetEncoding the coarse-fine encoding object defining the result we want extracted
	 * @param sourceEncoding the coarse-fine encoding object defining the source timecode
	 */
	public CoarseFineExtractor(CoarseFineEncoding targetEncoding, CoarseFineEncoding sourceEncoding) {
		this.targetEncoding = targetEncoding;
		this.sourceEncoding = sourceEncoding;
	}

	@Override
	public void setStaticArgs(Map<String, Object> params) {
		// do nothing
		
	}
	
	/**
	 * Extracts a long value of a given bit length from a byte array.
	 * 
	 * @param buff the byte array containing the input data 
	 * @param offset the starting offset of the value in the byte array
	 * @param bitSize bit size of the field to extract
	 * @param byteSize byte size of the field to extract
	 * @param fieldName name of the field being extracted, for error reporting
	 * @return extracted value
	 */
	public static long extractLong(byte[] buff, int offset, int bitSize, int byteSize, String fieldName) {
		// TODO move from this class
		if(buff == null) {
			throw new IllegalArgumentException("Null input byte buffer");
		}
		else if(offset < 0 || offset >= buff.length) {
			throw new ArrayIndexOutOfBoundsException("Input offset falls outside the bounds of" +
			" the input byte buffer");
		}
		else if((offset + byteSize) > buff.length) {
			throw new ArrayIndexOutOfBoundsException(String.format("%d segment must be %d bytes"
			 +" long, but this extends past the end of the input buffer", fieldName, byteSize));
		}
		long result;
		
		if (bitSize == 0) {
			result = 0;
		} else if(bitSize <= 8) {
			result = GDR.get_u8(buff, offset);
		} else if(bitSize  <= 16) {
			result = GDR.get_u16(buff, offset);
		} else if(bitSize  <= 24) {
			result = GDR.get_u24(buff, offset);
		} else if(bitSize  <= 32) {
			result = GDR.get_u32(buff, offset);
		} else {
			// TODO this length is actually unsupported. should this throw? or let ISclk do it?
			result = GDR.get_u64(buff, offset);
		}

		return result;
	}

	/**
	 * Extract the coarse time portion from a byte array.
	 * 
	 * @param buff the byte array containing the input data
	 * @param offset the starting offset of the coarse time in the byte array
	 * @return coarse time
	 */
	public long extractCoarse(byte[] buff, int offset) {
		return extractLong(buff, offset, targetEncoding.getCoarseBits(), targetEncoding.getCoarseByteLength(), "coarse");
	}
	
	/**
     * Extract the fine time portion from a byte array.
     * 
     * @param buff the byte array containing the input data
     * @param offset the starting offset of the fine time in the byte array
     * @return fine time
     */
	public long extractFine(byte[] buff, int offset) {
		if (sourceEncoding.getFineBits() == 0) {
			return 0;
		}
		return extractLong(buff, offset, targetEncoding.getFineBits(), targetEncoding.getFineByteLength(), "fine");
	}

	@Override
	public ISclk getValueFromBytes(byte[] buff, int startingOffset) {

		if(buff == null)
		{
			throw new IllegalArgumentException("Null input byte buffer");
		}
		else if(startingOffset < 0 || startingOffset >= buff.length)
		{
			throw new ArrayIndexOutOfBoundsException("Input offset falls outside the bounds of" +
			" the input byte buffer");
		}
		else if((startingOffset + sourceEncoding.getByteLength()) > buff.length)
		{
			throw new ArrayIndexOutOfBoundsException("SCLK must be " + sourceEncoding.getByteLength() + " bytes" +
					" long, but this extends past the end of the input buffer (length = " + buff.length +
					") when the starting offset is " + startingOffset);
		}

		int offset = startingOffset;

		long coarse = extractCoarse(buff,offset);
		long fine = extractFine(buff,offset + sourceEncoding.getCoarseByteLength());
		if(sourceEncoding.getMaxFine() != targetEncoding.getMaxFine()) {
			fine = fine * (targetEncoding.getMaxFine() + 1) / (sourceEncoding.getMaxFine() + 1);
		}

		return new Sclk(coarse, fine, targetEncoding);
	}

	@Override
	public boolean hasEnoughBytes(byte[] buff, int startingOffset) {
		return (buff.length - startingOffset) >= sourceEncoding.getByteLength();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Source encoding: " + sourceEncoding + " ")
		.append("Target encoding: " + targetEncoding);
		return builder.toString();
	}

	@Override
	public ISclk getValueFromBits(BitBuffer buffer, Map<String, Object> args) {
		long coarse = buffer.getUnsignedLong(sourceEncoding.getCoarseBits());
		final long fine;
		if (sourceEncoding.getFineBits() == 0) {
			fine = 0;
		} else {
			fine = buffer.getUnsignedLong(sourceEncoding.getFineBits());
		}
		return new Sclk(coarse, fine, targetEncoding);
	}
	
}
