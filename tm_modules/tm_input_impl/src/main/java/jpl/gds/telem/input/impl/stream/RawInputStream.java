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
package jpl.gds.telem.input.impl.stream;

import java.io.DataInputStream;
import java.io.IOException;

import jpl.gds.telem.input.api.stream.IRawInputStream;

/**
 * This is a standard input stream that wraps a <code>DataInputStream</code> so
 * it can be processed by an <code>IRawStreamProcessor</code>
 * 
 *
 */
public class RawInputStream implements IRawInputStream {
	private final DataInputStream dis;
	private final byte[] data;

	/**
	 * Constructor
	 * 
	 * @param dis the <code>DataInputStream</code> to wrap
	 */
	public RawInputStream(DataInputStream dis) {
		this(dis, null);
	}

	/**
	 * Constructor
	 * 
	 * @param dis the <code>DataInputStream</code> to wrap
	 * @param data the data in a byte[]
	 */
	public RawInputStream(DataInputStream dis, byte[] data) {
		this.dis = dis;
		this.data = data;
	}


	@Override
	public DataInputStream getDataInputStream() {
		return this.dis;
	}


	@Override
	public byte[] getData() {
		return this.data;
	}


	@Override
	public void close() throws IOException {
		if (dis != null) {
			dis.close();
		}
	}

	// MPCS-7832 01/12/16 - Added function due to change in IRawInputStream
	/**
	 * Clear buffer if present, otherwise an error is thrown. Currently this
	 * InputStream does not utilize a buffer.
	 * 
	 * @throws UnsupportedOperationException
	 *             The buffer does not exist
	 */
	@Override
	public void clearInputStreamBuffer() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("RawInputStream does not utilize a buffer");
		
	}
}
