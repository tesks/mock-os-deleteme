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
package jpl.gds.telem.input.api.stream;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * This class defines the methods required for an <code>IRawInputStream</code>
 * so it can be properly processed by an <code>IRawStreamProcessor</code>
 * 
 *
 */
public interface IRawInputStream {
	/**
	 * Get the <code>DataInputStream</code>
	 * 
	 * @return the <code>DataInputStream</code>
	 */
	public DataInputStream getDataInputStream();

	/**
	 * Get the data stored in this stream. This is used for connection types
	 * where each input stream contains a finite amount of data (e.g. a single
	 * packet)
	 * 
	 * @return the data stored in this stream
	 */
	public byte[] getData();

	/**
	 * Close the stream.
	 * @throws IOException if there is an error closing the stream
	 */
	public void close() throws IOException;

	// MPCS-7832 01/12/16 - Added function to interface for current and potential future use of DiskBackedBufferedInputStream
	/**
	 * Clear the buffer within the IRawInputStream interfaced class.
	 * 
	 * @throws UnsupportedOperationException
	 *             if the IRawInputStream does not support buffer clearing
	 * @throws IOException
	 *             if the clearBuffer request threw an exception, was
	 *             interrupted while waiting, or could not be scheduled for
	 *             execution
	 * @throws IllegalStateException
	 *             if the buffer is not in an operational state
	 */
	public void clearInputStreamBuffer() throws UnsupportedOperationException, IOException, IllegalStateException;
}
