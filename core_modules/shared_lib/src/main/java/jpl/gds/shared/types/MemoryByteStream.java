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



/**
 * Class implements a stream using byte array slices.
 *
 */
public class MemoryByteStream extends ByteStream
{
	private final ByteArraySlice data;
	private int offset = 0;


    /**
     * Constructor.
     *
     * @param data Byte array slice
     */
	public MemoryByteStream(final ByteArraySlice data)
    {
		this.data = data;
	}


	/**
     * {@inheritDoc}
	 **/
	@Override
	public void reset() {
		offset = 0;
	}

	/**
     * {@inheritDoc}
	 **/
	@Override
	public long getLength() {
		return data.length;
	}

	/**
     * {@inheritDoc}
	 **/
	@Override
	public long getOffset() {
		return offset;
	}


	/**
     * {@inheritDoc}
	 **/
	@Override
	public void skip(long bytes) {
		if ((offset + bytes) > data.length) {
			throw new IndexOutOfBoundsException("Tried to skip to byte "
					+ (offset + bytes)
					+ " of a " + data.length
					+ " byte stream");
		}
		offset += bytes;
	}


	/**
     * {@inheritDoc}
	 **/
	@Override
	public ByteArraySlice read(int bytes) {
		if ((offset + bytes) > data.length) {
			throw new IndexOutOfBoundsException("Tried to read to byte "
					+ (offset + bytes)
					+ " of a " + data.length
					+ " byte stream");
		}
		ByteArraySlice slice = new ByteArraySlice(data.array, offset, bytes);
		offset += bytes;
		return slice;
	}
}
