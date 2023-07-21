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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream for writing bit-packed values. This code is taken from AMPCS
 * core's, but because we don't want to introduce a dependency on AMPCS core
 * just for this class, it was basically copied over. In AMPCSR8, when SLE
 * capability is integrated into AMPCS architecture itself, this duplication can
 * then be removed.
 * 
 */
public class BitOutputStream extends OutputStream {

	/**
	 * The output stream that this class's object wraps around.
	 */
	private final OutputStream outputStream;

	/**
	 * Counter for bytes written to the output stream.
	 */
	private int bytesWritten = 0;

	/**
	 * Tracker for the bit offset.
	 */
	private int bitOffset = 0;

	/**
	 * Data buffer that will be written to the output stream.
	 */
	private int buffer = 0;

	/**
	 * Constructor.
	 * 
	 * @param outputStream
	 *            <code>OutputStream</code> to write data to
	 */
	public BitOutputStream(final OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(int) writes the LS 8 bits of arg to
	 * output stream
	 */
	@Override
	public final void write(final int arg) throws IOException {
		bytesWritten++;
		outputStream.write(arg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	public final void write(final byte[] data, final int offset, final int size) throws IOException {
		super.write(data, offset, size);
		bytesWritten += (size - offset);
	}

	/**
	 * Writes the the given number of LS bits from alue to the next available
	 * position in the bit stream starting at the MS bit of the byte at the
	 * current output position. Use writeLongbits if bits > 16.
	 * 
	 * @param value
	 *            Value to be output
	 * @param bits
	 *            Number of bits to write (value will be truncated to this size;
	 *            cannot exceed 16bits)
	 * @throws IOException
	 *             Thrown when IOException is encountered while writing the bits
	 */
	public final void writeBits(final int value, final int bits) throws IOException {

		if (bits > 16) {
			throw new IllegalArgumentException("bit size must be <= 16");
		}

		int mask = GDR.makeBitMask(bits);
		int xvalue = value & mask;

		int xbits = bits;
		while (xbits > 7) {
			int xmask = GDR.makeBitMask(8 - bitOffset);
			int xxvalue = xvalue >> bitOffset;
			buffer = (buffer & ~xmask) | xxvalue;
			write(buffer);
			xbits -= (8 - bitOffset);
			bitOffset = 0;
		}
		if (xbits > 0) {
			int extent = bitOffset + xbits;
			int shift = 8 - extent;
			if (shift < 0) {
				// need to write this value out in parts
				shift += 8;
				int xmask = mask << shift;
				xvalue = xvalue << shift;
				int xbuffer = buffer << 8;
				xbuffer = (xbuffer & ~xmask) | xvalue;
				buffer = xbuffer & 0xff;
				xbuffer = xbuffer >> 8;
				write(xbuffer);
				bitOffset = bitOffset + bits - 8;
			} else {
				xvalue = xvalue << shift;
				mask = mask << shift;
				buffer = (buffer & ~mask) | xvalue;
				bitOffset += xbits;
			}
		}

		while (bitOffset > 7) {
			shift();
		}
	}

	/**
	 * Allow for values > 16 bits Only the MS byte needs masking, if that.
	 * 
	 * @param value
	 *            Value to be output
	 * @param bits
	 *            Number of bits to write
	 * @throws IOException
	 *             Thrown when IOException is encountered while writing the bits
	 */
	public final void writeLongBits(final int value, final int bits) throws IOException {

		if (bits > 24) {
			throw new IllegalArgumentException("bit size must be <= 24");
		}

		int xbits = bits;

		while (xbits > 8) {
			int bytes = xbits / 8 + (xbits % 8 > 0 ? 1 : 0);
			int shift = 8 * (bytes - 1);
			int msb = value >> shift;
			int size = xbits - shift;
			xbits = xbits - size;
			writeBits(msb, size);
		}

		// write LSB 16 bits
		writeBits(value, xbits);
	}

	/**
	 * Write out the LS byte in the buffer and shift remaining contents down 8
	 * bits.
	 * 
	 * @throws IOException
	 *             Thrown when IOException is encoutered while writing the bits
	 */
	private void shift() throws IOException {
		write(buffer);
		buffer = buffer >> 8;
		bitOffset -= 8;
	}

	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#flush()
	 */
	public final void flush() throws IOException {
		
		while (bitOffset > 0) {
			shift();
		}
		
		bitOffset = 0;
		outputStream.flush();
	}

	/**
	 * Get the bytes written.
	 * 
	 * @return Bytes written
	 */
	public final int getBytesWritten() {
		return bytesWritten;
	}
	
}
