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
package jpl.gds.shared.types;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import jpl.gds.shared.gdr.GDR;

/** This class is an adaptor for the java.nio.ByteBuffer class to suit needs such as
 * reading an arbitrary number of bits as an integer.
 * Originally developed to suit the needs of NSYT seismic data decompression.
 * 
 * The idea is to keep a ByteBuffer and control access to it using composition.  Cache the last byte read/written
 * and manipulate it until it is consumed, then move on to the next byte.
 *
 */
public class BitBuffer {
	
	/** Small enum to represent the sign representation for integral numbers for purposes of sign expansion. */
	public enum SignedRepresentation {
		/** The most significant bit is a 1 for a negative, 0 for a positive, and the remaining bits are the numbers value. */
		MAGNITUDE,
		/** The sign is represented as a two's complement. */
		TWOS_COMPLEMENT
	}

	private SignedRepresentation representation = SignedRepresentation.TWOS_COMPLEMENT;
	private byte currentByte; // Cache the last byte from the underlying ByteBuffer
	private ByteBuffer byteBuffer;
	
	/** Offset of the next bit to retrieve from current byte where the MSB is bit offset zero.
	* E.g., -128, represented as "b10000 0000", when bitOffset == 8, the bit we should retrieve is a 1.
	* If bitOffset is 0, it is time to read another byte.
	*/
	private int bitOffset;   
	
	
	private int markedBitOffset;
	private byte markedByte;
	private int markedByteOffset;
	
	private int limit;
	
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
	
	/** Constructs a BitBuffer with a reference to the specified backing array.
	 * This is a shallow copy
	 * @param array - the backign array for the BitBuffer
	 */
	private BitBuffer (byte[] array) {
		byteBuffer = ByteBuffer.wrap(array);
		limit = byteBuffer.limit() * Byte.SIZE;
		bitOffset = Byte.SIZE;
	}
	
	/** Constructs a BitBuffer with a reference to the specified backing array, limited to the sub-array
	 * starting at offset and of the specified length.
	 * @param array - the backing array for the BitBuffer
	 * @param offset - the offset in the array to use as the start of the backing subarray
	 * @param length - the length of the subarray to use
	 */
	private BitBuffer (byte[] array, int offset, int length) {
		limit = offset + length;
		int byteLength = (int) Math.ceil((double) length / Byte.SIZE);	
		byteBuffer = ByteBuffer.wrap(array, offset / Byte.SIZE, byteLength);
		if (offset % Byte.SIZE != 0) {
			currentByte = byteBuffer.get();
			bitOffset = offset % Byte.SIZE;
		} else {
			bitOffset = Byte.SIZE;
		}
	}
	
	/**
	 * Set how to extrapolate a larger bit sequence from a smaller one.  For example,
	 * some bit sequences could be meant to be in two's complement.
	 * @param rep - How to interpret the most-significant bit of a bit stream.
	 */
	public void setRepresentation (SignedRepresentation rep) {
		representation = rep;
	}
	
	/**
	 * @return the number of bits that can still be read from the backing ByteBuffer
	 */
	public int remaining() {
		return byteBuffer.remaining() * Byte.SIZE + Byte.SIZE - bitOffset;
	}

	/** Read a number of bits from the buffer and construct an integer.  If extension is necessary, 
	 * use zeroes.
	 * @param numBits - the number of bits to read.
	 * @return a 32-bit integer left-padded with zeroes.
	 */
	 public int getUnsignedInt (int numBits) {
			if (numBits > Integer.SIZE  || numBits <= 0) {
				throw new IllegalArgumentException("Cannot construct long from " + numBits + " bytes.");
			}
			if (remaining() < numBits) {
				throw new BufferUnderflowException();
			}
			return (int) getUnsignedLong(numBits);
			
	 }
	 
	 /**
	  * Get a single-precision float from the backing buffer. Assumes IEEE-750 encodign.
	  * @return extracted float at the ucrrent buffer position
	  */
	 public float getFloat() {
		 byte[] floatBytes = this.get(Float.BYTES);
		 return GDR.get_float(floatBytes, 0);
	 }
	 
	 /**
	  * Get a double-precision float from the backing buffer. Assumes IEEE-750 encodign.
	  * @return extracted double at the current buffer position
	  */
	 public double getDouble() {
		byte[] doubleBytes = this.get(Double.BYTES); 
		return GDR.get_double(doubleBytes, 0);
	 }
	 
	 
	 /**
	  * Get a single precision MIL-1750A encoded float. 
	  * @return extracted float at the current buffer position
	  */
	 public double getMILFloat() {
		 byte[] mil32Data = this.get(4);
		 return GDR.getMIL32(mil32Data, 0);
	 }
	 
	 /**
	  * Get a double precision MIL_1750A encoding float.
	  * @return extracted double at the current buffer position
	  */
	 public double getMILDouble() {
		 byte[] mil48Data = this.get(6);
		 return GDR.getMIL48(mil48Data, 0);
	 }

 	 /**
 	  * 
 	  * Get an ASCII encoded string from the underlying buffer.
 	  * @param length the number of bytes of the encoded string
 	  * @return the String extracted at the current buffer position
 	  */
 	 public String getAsciiString(int length) {
 		byte[] stringData = this.get(length);
 		return StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(stringData).order(byteOrder)).toString();
	 }
	 
 	 /**
 	  * 
 	  * Get a UTF-8 encoded string from the underlying buffer.
 	  * @param length the number of bytes of the encoded string
 	  * @return the String extracted at the current buffer position
 	  */
	 public String getUtf8String(int length) {
 		byte[] stringData = this.get(length);
 		return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(stringData).order(byteOrder)).toString();
	 }
	  
	 /**
	  * Get raw data from the backing buffer. The returned byte array is a copy.
	  * This method is ideal for getting a byte-aligned chunk of data from the bit buffer
	  * when the buffer position may be in the middle of a byte.
	  * @param numBytes the number of bytes to get from the backing buffer
	  * @return a set of bytes beginning at the current buffer position
	  */
	 public byte[] get(int numBytes) {
		 byte[] result = new byte[numBytes];
		 
		 // Fast path if the buffer position is byte aligned
		 if (bitOffset == 8) {
			 byteBuffer.get(result, 0, numBytes);
			 return result;
		 }

		 int bitsExtracted = 0;
		 // Not byte aligned
		 int myBitOffset = 0; // Track bit offset in result byte array
		 int numBits = numBytes * Byte.SIZE;
		 while (bitsExtracted < numBits) {
			 // Read the next byte from the backing array if necessary
			 int bitsAvailable = Byte.SIZE - bitOffset;
			 if (bitsAvailable == 0) {
				 currentByte = byteBuffer.get();
				 bitsAvailable = Byte.SIZE;
				 bitOffset = 0;
			 }
			 int bitsNeeded = Byte.SIZE - (myBitOffset % Byte.SIZE); 
			 int bitsToRead = bitsNeeded < bitsAvailable ? bitsNeeded : bitsAvailable;
			 result[bitsExtracted / Byte.SIZE] <<= bitsToRead;
			 result[bitsExtracted / Byte.SIZE] |= ((currentByte << bitOffset) & 0xff) >>> (Byte.SIZE - bitsToRead);

			 bitsExtracted += bitsToRead;
			 bitOffset = bitOffset + bitsToRead;
			 myBitOffset += bitsToRead;
		 }
		 return result;
	 }
	 

	 
	 

	/** Get the next bytes from the buffer and parse into an integers.  Interprets the bits as a signed 
	 * integer. 
	 * @param numBits - How many bits to parse into an integer.  
	 * @return Either the two's complement sign-extended value, or treats the value as a "signed magnitude" number, 
	 * in which case it returns the value of the first numBytes * 8 - 1, 
	 * multiplied by -1 if the most significant bit is set.
	 * @throws BufferUnderflowException if the number of bits exceeds the number of bits remaining
	 * @throws IllegalArgumentException if the number of bits provided is negative or greater
	 * 		   than the number of bits in a Java int
	 */
	public int getInt(int numBits) throws IllegalArgumentException, BufferUnderflowException {
		if (numBits > Integer.SIZE  || numBits <= 0) {
			throw new IllegalArgumentException("Cannot construct long from " + numBits + " bytes.");
		}

		
		return (int) getLong(numBits);
	}

	
	/**
	 * @see java.nio.ByteBuffer#wrap(byte[])
	 * @param array the byte array this bit buffer wraps
	 * @return BitBuffer wrapping the array
	 */
	public static BitBuffer wrap(byte[] array) {
	
		return new BitBuffer(array);
	}
	
	/**
	 * @see java.nio.ByteBuffer#wrap(byte[], int, int)
	 * @param array the byte array this bit buffer wraps
	 * @param offset the starting offset in the byte array 
	 * @param length the number of bytes in the byte array to use
	 * @return BitBuffer wrapping the array
	 */
	public static BitBuffer wrap(byte[] array, int offset, int length) {
		return new BitBuffer(array, offset, length);
	}
	
	/**
	 * @see java.nio.Buffer#mark()
	 * @return this buffer
	 */
	public BitBuffer mark() {
		byteBuffer.mark();
		markedBitOffset = bitOffset;
		markedByteOffset = byteBuffer.position();
		markedByte = currentByte;
		return this;
	}
	
	
	/**
	 * @see java.nio.Buffer#reset()
	 * @return this buffer
	 */
	public BitBuffer reset() {
		byteBuffer.reset();
		bitOffset = markedBitOffset;
		currentByte = markedByte;
		return this;
	}

	/**
	 * @see java.nio.Buffer#position(int)
	 * @param newPosition the new position value in bits. Cannot be negative or greater
	 * 		  than the buffer's limit
	 * @return this buffer
	 */
	public BitBuffer position(int newPosition) {
		
		if (newPosition > limit()) {
			throw new IllegalArgumentException("New position exceeds buffer's limit");
		}
		
		// If position moves forward within the same byte, then just adjust the current bit offset.
		// Doing otherwise will reset the ByteBuffer's marker
		int newByteBufferPosition = newPosition/Byte.SIZE;
		if((newByteBufferPosition + 1) == byteBuffer.position() && (newPosition >= position())){
			bitOffset = newPosition % Byte.SIZE;
		    return this;
		}

		int savedBitOffset = bitOffset;
		bitOffset = newPosition % Byte.SIZE;
		int savedByteBufferPos = byteBuffer.position();
		byte savedCurrentByte = currentByte;
		byteBuffer.position(newByteBufferPosition);
		try {
			if (bitOffset == 0) {
				bitOffset = 8;
			} else {
				currentByte = byteBuffer.get();
			}
			if (byteBuffer.position() * Byte.SIZE + bitOffset < markedByteOffset * Byte.SIZE + markedBitOffset) {
				markedByteOffset = 0;
				markedBitOffset = 8;
			}
		} catch (BufferUnderflowException e) {
			byteBuffer.position(savedByteBufferPos);
			currentByte = savedCurrentByte;
			bitOffset = savedBitOffset;
			throw new IllegalArgumentException("New position exceeds buffer's limit", e);
		}
		return this;
	}
	
	/**
	 * @see java.nio.Buffer#position(int)
	 * @return the current position in the buffer, in bits
	 */
	public int position() {
		return byteBuffer.position() * Byte.SIZE - (Byte.SIZE - bitOffset);
	}

	/**
	 * Get a signed short from the next available bits in the buffer.
	 * @param numBits the number of bits to interpret as the short's value
	 * @return the sign extended short value
	 */
	public short getShort(int numBits) {
		if (numBits > Short.SIZE  || numBits <= 0) {
			throw new IllegalArgumentException("Cannot construct long from " + numBits + " bits.");
		}
		return (short) getLong(numBits);
	}

	/**
	 * Get a signed long from the next available bits in the buffer.
	 * @param numBits the number of bits to interpret as the long's value
	 * @return the sign extended long value
	 */
	public long getLong(int numBits) {
		// Original SteimLite decompression requires signed magnitude interpretation, but commonly could be two's complement
		// Do final processing of the sign bit
		long returnVal = getUnsignedLong(numBits);

		if ( representation == SignedRepresentation.TWOS_COMPLEMENT ) {
			// Sign extend
			returnVal <<= Long.SIZE - numBits ;
			returnVal >>= Long.SIZE - numBits ;
		} else {
			// Check MSB
			long bitMask = 0x1 << (numBits - 1);
			long negate = returnVal & bitMask;
			if (negate != 0) {
				// Force MSB to zero
				returnVal &= ~(bitMask);
				returnVal *= -1;
			}
		}

		return returnVal;
	}
	
	/**
	 * Get a signed byte value from the next available bits in the buffer.
	 * @param numBits the number of bits to interpret as the byte's value
	 * @return the sign extended byte value
	 */
	public byte getByte(int numBits) {
		if (numBits > Byte.SIZE  || numBits <= 0) {
			throw new IllegalArgumentException("Cannot construct long from " + numBits + " bytes.");
		}
		return (byte) getLong(numBits);
	}

	/**
	 * Get an unsigned byte from the next available bits in the buffer.
	 * @param numBits the number of bits to interpret as the byte's value
	 * @return the zero-extended byte value
	 */
	public byte getUnsignedByte(int numBits) {
		if (numBits > Byte.SIZE  || numBits <= 0) {
			throw new IllegalArgumentException("Cannot construct long from " + numBits + " bytes.");
		}
		return (byte) getUnsignedLong(numBits);
	}

	/**
	 * Get an unsigned long from the next available bits in the buffer.
	 * @param numBits the number of bits to interpret as the long's value
	 * @return the zero-extended long value
	 */
	public long getUnsignedLong(int numBits) {
		long returnVal = 0;
		int bitsExtracted = 0;
		
		if (numBits > Long.SIZE  || numBits <= 0) {
			throw new IllegalArgumentException("Cannot construct long from " + numBits + " bytes.");
		}
		if (remaining() < numBits) {
			throw new BufferUnderflowException();
		}
		
		// The meat of the parsing
		// Both big endian and little endian data are extracted first as a big endian, zero-extended
		// long. The resulting long is manipulated further if the data is little endian.
		while (bitsExtracted < numBits) {
			
			// Check how many bits remain in the current byte (i.e. how many bits can be consumed before
			// getting another byte from the underlying buffer
			int bitsAvailable = Byte.SIZE - bitOffset;
			if (bitsAvailable == 0) {
				currentByte = byteBuffer.get();
				bitsAvailable = Byte.SIZE;
				bitOffset = 0;
			}
			int bitsNeeded = numBits - bitsExtracted;
			
			/** Determine whether we should read all the bits left in the current byte, or if we need fewer
			  * bits.  Ternary statement reads:
			  * 	if bitsNeeded < bitsAvailable, set bitsToRead = bitsAvailable. 
			  * 	else, set bitsToRead = bitsAvailable.
	 		  */
	 		int bitsToRead = bitsNeeded < bitsAvailable ? bitsNeeded : bitsAvailable;
			
			// Save any previously read bits by shifting them up.  Note: assumes Big Endianness.
			returnVal <<= bitsToRead;
			/** Append bitsToRead number of bits in the least significant bits of returnVal.
			 * a = (currentByte << bitOffset): Erase upper bits of currentByte that have been used previously
			 * b = a & 0xff: Since Java promotes everything to an int in bit operations, kill any sign extension
			 * c = b >>> (Byte.SIZE - bitsToRead): we want the bits from this byte to be in the lowest bits possible.
			 * (c continued) this is achieved by a logical right shift- we will deal with sign extension later
			 */
			returnVal |= ((currentByte << bitOffset) & 0xff) >>> (Byte.SIZE - bitsToRead);
			bitsExtracted += bitsToRead;
			bitOffset = bitOffset + bitsToRead;
			
		}

		if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
			// First, pad the most recently extracted byte with zeroes
			// and shift the rest of the number to the left.
			int msbPadBits = (Byte.SIZE - (numBits % 8)) % 8;
			long temp = returnVal << msbPadBits;
			returnVal = (temp & ~0x00ff) | ((temp >>> msbPadBits) & (0x00ff >> msbPadBits));
			// Endian swap
			returnVal = Long.reverseBytes(returnVal);
			// Shift right to account for the fact that extract bytes that weren't
			// read were introduced in the reverse
			// For example, if you extract only one byte, 0x01, it should still result in the
			// value 1.  But swapping gives us  a value of 2 ^ 60 initially, so we need to shift
			// by 59 to get back to the right value
			int byteShift = Long.BYTES - numBits / Byte.SIZE;
			if (numBits % Byte.SIZE != 0) {
				byteShift -= 1;
			}
			returnVal >>>= (byteShift * Byte.SIZE);
		}

		return returnVal;
	}
	
	/**
	 * @see ByteBuffer#limit()
	 * @return the limit of this buffer in bits
	 */
	public int limit() {
		return limit;
	}
	
	/**
	 * @see ByteBuffer#rewind()
	 * @return this buffer
	 */
	public BitBuffer rewind() {
		byteBuffer.rewind();
		bitOffset = 8;
		return this;
	}
	
	/**
	 * @see ByteBuffer#order()
	 * @return the current byte order of the buffer
	 */
	public ByteOrder order() {
		return byteOrder;
	}
	
	/**
	 * @see ByteBuffer#order(ByteOrder)
	 * @param order The new byte order, either BIG_ENDIAN OR LITTLE_ENDIAN
	 */
	public void order(ByteOrder order) {
		byteOrder = order;
	}

}
