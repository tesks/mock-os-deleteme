package jpl.gds.globallad.data;

import java.nio.ByteBuffer;
import java.util.Arrays;

import jpl.gds.shared.gdr.GDR;

/**
 * Convienience methods to extract data from byte buffers as well as set values into 
 * byte byffers.
 */
public interface IByteBufferManipulator {

	/**
	 * Creates an integer from the buffer.
	 * 
	 * @param buffer
	 * @param numBytes
	 * @return
	 * @throws GlobalLadDataException
	 */
	default int getNextInt(final ByteBuffer buffer, final int numBytes) throws GlobalLadDataException {
		checkOffset(buffer, numBytes, "Create int value");
		if (numBytes > 0 && numBytes < 5) {
			final byte[] dataBuffer = new byte[4];
			buffer.get(dataBuffer, dataBuffer.length-numBytes, numBytes);
			return GDR.get_i32(dataBuffer, 0);
		} else {
			throw new GlobalLadDataException("Unsupported number of bytes for data: " + numBytes);
		}
	
	}

	/**
	 * Creates an unsigned long using the number of bytes given.  These bytes will always be the least significant bytes.  
	 * 
	 * @param buffer
	 * @param numBytes - 1 - 8 bytes supported.
	 * @return - long value.
	 * @throws GlobalLadDataException - Invalid numBytes of not enough bytes in buffer to perform the required operation.
	 */
	default long getNextUnsignedValue(final ByteBuffer buffer, final int numBytes) throws GlobalLadDataException {
		checkOffset(buffer, numBytes, "Create unsigned value");
		if (numBytes > 0 && numBytes < 5) {
			final byte[] dataBuffer = new byte[4];
			buffer.get(dataBuffer, dataBuffer.length-numBytes, numBytes);
			return GDR.get_u32(dataBuffer, 0);
		} else if (numBytes > 4 && numBytes < 9) {
			final byte[] dataBuffer = new byte[8];
			buffer.get(dataBuffer, dataBuffer.length-numBytes, numBytes);
			return GDR.get_u64(dataBuffer, 0);
		} else {
			throw new GlobalLadDataException("Unsupported number of bytes for data: " + numBytes);
		}
	}

	/**
	 * Checks the buffer to make sure there are at least numBytes left in the buffer.
	 * 
	 * @param buffer
	 * @param numBytes
	 * @param description - A string to detail the operation in the case an exception needs to be thrown. 
	 * 
	 * @throws GlobalLadDataException if there are not enough bytes remaining.
	 */
	default void checkOffset(final ByteBuffer buffer, final int numBytes, final String description) throws GlobalLadDataException {
		if (buffer.remaining() < numBytes) {
			throw new GlobalLadDataException("Byte buffer does not have enough bytes remaining to perform the given operation: " + description);
		}
	}

	/**
	 * Returns numBytes from the byte buffer and returns a new byte array. The current pointer of the byte buffer will be adjusted.  If 
	 * numBytes is 0 returns an empty array.
	 * 
	 * @param buffer
	 * @param numBytes
	 * @return - Asked for bytes in an array.
	 * @throws GlobalLadDataException - Not enough bytes left for given operation.
	 */
	default byte[] getNextBytes(final ByteBuffer buffer, final int numBytes) throws GlobalLadDataException {
		if (numBytes <= 0) {
			return new byte[0];
		}
		
		checkOffset(buffer, numBytes, "Extract byte slice.");
	
		final byte[] data = new byte[numBytes];
		buffer.get(data);
		return data;
	}

	/**
	 * There is a need to only take a certain number of bytes from long values because to set
	 * in a byte[].  This will set the number of bytes specified at the current position of the buffer.
	 * 
	 * @param value
	 * @param numBytes
	 */
	default void setLongUnsigned(final ByteBuffer buffer, final long value, final int numBytes) {
		final byte[] data = new byte[8];
		GDR.set_u64(data, 0, value);
		buffer.put(data, 0, numBytes);
	}

	/**
	 * There is a need to only take a certain number of bytes from long values because to set
	 * in a byte[].  This will set the number of bytes specified at the current position of the buffer.
	 * 
	 * @param value
	 * @param numBytes
	 */
	default void setLongSigned(final ByteBuffer buffer, final long value, final int numBytes) {
		final byte[] data = new byte[8];
		GDR.set_i64(data, 0, value);
		buffer.put(data, 0, numBytes);
	}
	
	/**
	 * Computes boolean based on buffer.
	 * 
	 * @param buffer
	 * @return - False if the long value computed from buffer is zero elst True.
	 */
	default Boolean getBooleanValue(final byte[] buffer) {
		return getLongValue(buffer, false) == 0;
	}
	
	/**
	 * Converts the byte[] to string.
	 * 
	 * @param stringBuffer byte arry to be converted.  If this value is null returns an emtpy string.
	 * @return - Strinv value of stringBuffer.
	 */
	default String getStringValue(final byte[] stringBuffer) {
		/**
		 * Handling null.
		 */
		
		return stringBuffer == null ? "" : new String(stringBuffer);
	}
	
	/**
	 * Creates a new string from the stringBuffer and then interns the value before returning.
	 * 
	 * @param stringBuffer byte arry to be converted.  If this value is null returns an emtpy string.
	 * @return - String value of stringBuffer.
	 */
	default String getStringValueAndIntern(final byte[] stringBuffer) {
		return this.getStringValue(stringBuffer).intern();
	}
	
	/**
	 * Uses the current position of the byte buffer and gets numBytes from it and converts to a string.
	 * 
	 * @param buffer
	 * @param numBytes
	 * @return - String value of numBytes of buffer from the current position in buffer.
	 * @throws GlobalLadDataException
	 */
	default String getStringValue(final ByteBuffer buffer, final int numBytes) throws GlobalLadDataException {
		return getStringValue(getNextBytes(buffer, numBytes));
	}
	
	/**
	 * Wraps buffer in a byte buffer and uses the getDouble method. Because of
	 * this it is up to the caller to ensure that buffer has at least eight
	 * bytes.
	 * 
	 * @param buffer
	 *            bytes to convert to double
	 * @return Double value of buffer using the first 8 bytes of buffer.
	 */
	default double getDoubleValue(final byte[] buffer) {
		final ByteBuffer bb = ByteBuffer.wrap(buffer);
		return bb.getDouble();
	}
	
	/**
	 * Wraps buffer in a byte buffer and uses the getFloat method. Because of
	 * this it is up to the caller to ensure that buffer has at least four
	 * bytes.
	 * 
	 * @param buffer
	 *            bytes to convert to float
	 * @return Float value of buffer using the first 4 bytes of buffer.
	 */
	default float getFloatValue(final byte[] buffer) {
		final ByteBuffer bb = ByteBuffer.wrap(buffer);
		return bb.getFloat();
	}

	/**
	 * Checks if the first element buffer is negative.
	 * @param buffer buffer to check
	 * @return if the first value in the buffer is negative.
	 */
	default boolean firstIsNegative(final byte[] buffer) {
		return buffer.length > 0 && buffer[0] < 0;
	}
	
	/**
	 * Adds buffer to a new ByteBuffer.
	 * 
	 * @param buffer
	 *            data to add to the buffer.
	 * @param isSigned
	 *            if true checks the first byte in the array. If it is negative,
	 *            fills with -1 before adding data from buffer.
	 * @param finalSize
	 *            final size of the byte buffer.
	 * @return ByteBuffer with size >= finalSize and filled with the data from buffer or wrapping buffer.
	 */
	default ByteBuffer initialize(final byte[] buffer, final boolean isSigned, final int finalSize) {
		/**
		 * Added a way to initialize a byte buffer for conversions.
		 * This is important because a signed number needs to have the buffer filled with -1s.
		 */
		ByteBuffer bb;
		
		if (buffer.length >= finalSize) {
			bb = ByteBuffer.wrap(buffer);
		} else {
			if (isSigned && firstIsNegative(buffer)) {
				// Initialize the buffer with -1s.
				final byte[] neg = new byte[finalSize];
				Arrays.fill(neg, (byte)-1);
				bb = ByteBuffer.wrap(neg);
			} else {
				bb = ByteBuffer.allocate(finalSize);
			}

			// Expected to be in big endian.
			bb.position(finalSize-buffer.length);
			bb.put(buffer);
			bb.position(0);
		}
		
		return bb;
	}

	/**
	 * To allow for any size input buffer, buffer is either wrapped in a 
	 * ByteBuffer if the size is 8 or larger, or it allocates a new ByteBuffer
	 * and adds the content of buffer to that. 
	 * 
	 * @param buffer
	 * @param isSigned
	 * @return long value of buffer using the first 8 bytes of buffer.
	 */
	default long getLongValue(final byte[] buffer, final boolean isSigned) {
		final ByteBuffer bb = initialize(buffer, isSigned, 8);

		return isSigned ? 
				GDR.get_i64(bb.array(), 0) :
				GDR.get_u64(bb.array(), 0);
	}
	
	/**
	 * To allow for any size input buffer, buffer is either wrapped in a 
	 * ByteBuffer if the size is 8 or larger, or it allocates a new ByteBuffer
	 * and adds the content of buffer to that. 
	 * 
	 * @param buffer
	 * @return long value of buffer using the first 4 bytes of buffer as an unsigned int value.
	 */
	default long getUnsignedIntValue(final byte[] buffer) {
		final ByteBuffer bb = initialize(buffer, false, 4);

		return GDR.get_u32(bb.array(), 0);
	}	
	
	/**
	 * To allow for any size input buffer, buffer is either wrapped in a 
	 * ByteBuffer if the size is 4 or larger, or it allocates a new ByteBuffer
	 * and adds the content of buffer to that. 
	 * 
	 * @param buffer
	 * @return integer signed value of buffer using the first 4 bytes of buffer.
	 */
	default int getSignedIntValue(final byte[] buffer) {
		final ByteBuffer bb = initialize(buffer, true, 4);

		return GDR.get_i32(bb.array(), 0);
	}
	

}