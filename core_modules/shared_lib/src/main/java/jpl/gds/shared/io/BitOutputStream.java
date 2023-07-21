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
package jpl.gds.shared.io;

import java.io.IOException;
import java.io.OutputStream;

import jpl.gds.shared.gdr.GDR;

/**
 * Output stream for writing bit-packed values
 * 
 * 
 *
 */
public class BitOutputStream extends OutputStream {
	private final OutputStream out;
	private int bytes_written=0;
	private int bit_offset=0;
	private int buffer=0;

	/**
	 * @param out stream to write to
	 */
	public BitOutputStream(OutputStream out) {
		this.out=out;
	}

	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(int)
	 * writes the LS 8 bits of arg to output stream
	 */
	@Override
	public void write(int arg) throws IOException {
		bytes_written++;
		out.write(arg);
	}
	
	/* (non-Javadoc)
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
    public void write(byte[] data, int offset, int size) throws IOException {
		super.write(data, offset, size);
		bytes_written += (size-offset);
	}

	/**
	 * Writes the the given number of LS bits from value to the next available position in the bit stream
	 * starting at the MS bit of the byte at the current output position.
	 * Use writeLongbits if bits > 16.
	 * 
	 * @param value value to be output
	 * @param bits number of bits to write (value will be truncated to this size; cannot exceed 16bits)
	 * @throws IOException if there is an error during writing
	 */
	public void writeBits(final int value, final int bits) throws IOException {
		if (bits > 16) throw new IllegalArgumentException("bit size must be <= 16");
		int mask = GDR.makeBitMask(bits);
		int xvalue = value & mask;
		
		int xbits = bits;
		while (xbits > 7) {
			int xmask = GDR.makeBitMask(8-bit_offset);
			int xxvalue = xvalue >> bit_offset;
			buffer = (buffer & ~xmask) | xxvalue;
			write(buffer);
			xbits -= (8-bit_offset);
			bit_offset=0;
		}
		if (xbits > 0) {
			int extent = bit_offset+xbits;
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
				bit_offset = bit_offset + bits - 8;
			} else {
				xvalue = xvalue << shift;
				mask = mask << shift;
				buffer = (buffer & ~mask) | xvalue;
				bit_offset += xbits;
			}
		}
		
		while (bit_offset > 7) {
			shift();
		}
	}
	
	/**
	 * Allow for values > 16 bits
	 * Only the MS byte needs masking, if that.
	 * 
	 * @param value value to write
	 * @param bits number of bits to write
	 * @throws IOException if there is an issue with the writing
	 */
	public void writeLongBits(final int value, final int bits) throws IOException {
		if (bits > 24) throw new IllegalArgumentException("bit size must be <= 24");
		int xbits=bits;
		
		while (xbits > 8) {
			int bytes = xbits/8 + (xbits % 8 > 0 ? 1:0);
			int shift = 8 * (bytes - 1);
			int msb = value >> shift;
			int size = xbits-shift;
			xbits = xbits - size;
			writeBits(msb, size);
		}
		// write LSB 16 bits
		writeBits(value, xbits);
	}

	/**
	 * Write out the LS byte in the buffer and shift remaining contents down 8 bits
	 * @throws IOException
	 */
	private void shift() throws IOException {
		write(buffer);
		buffer = buffer >> 8;
		bit_offset -= 8;
	}
	
	/**
	 * Flush any buffered values to output.
	 * This will flush out the current byte of data if any bits have been written to it.
	 */
	@Override
    public void flush() throws IOException {
		while (bit_offset > 0) {
			shift();
		}
		bit_offset=0;
		out.flush();
	}

	/**
	 * Gets the number of bytes written.
	 * 
	 * @return byte count
	 */
	public int bytesWritten() {
		return bytes_written;
	}
}
