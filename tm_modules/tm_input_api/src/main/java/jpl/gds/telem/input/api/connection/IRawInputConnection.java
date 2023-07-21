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
package jpl.gds.telem.input.api.connection;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.message.RawInputMetadata;
import jpl.gds.telem.input.api.stream.IRawInputStream;

/**
 * <p>
 * <code>IRawInputConnection</code> defines methods needed by Raw Input
 * connection adapters. The Raw Input Connection adapters define how to connect
 * to a raw input source and obtain telemetry
 * <p>
 * 
 *
 * MPCS-4632 - 10/13/16 - added shouldReconnectOnIOException.
 */
public interface IRawInputConnection {

	/**
	 * Creates a connection to a raw input source. Parameters to connect to the
	 * input source is specified in the <code>SessionConfiguration</code>
	 * 
	 * @return true if connection is successful, false otherwise
	 * @throws RawInputException if it fails to connect
	 */
	public boolean openConnection() throws RawInputException;

	/**
	 * Loads the next DataInputStream (and associated metadata). Call
	 * <code>getDataInputStream</code> (or <code>getMetadata</code>) method to
	 * retrieve the next DataInputStream if this method returns true. If this
	 * method returns false, then a call to <code>getDataInputStream</code> (or
	 * <code>getMetadata</code>) will return null.
	 *
     * @param handlerStopping Set non-null and true if input handler is stopping
     *
	 * @return true if there are more DataInputStreams available, false
	 *         otherwise.
	 * @throws RawInputException if there were problems loading data
	 * @throws IOException if loading the next set of data fails
	 */
	public boolean loadData(final AtomicBoolean handlerStopping)
        throws RawInputException, IOException;

	/**
	 * Retrieve the metadata extracted from the telemetry source. Must call
	 * openConnection prior to this method to retrieve metadata. Otherwise this
	 * method will return null.
	 * 
	 * @return TelemetrySourceMetadata object that contains the metadata
	 *         extracted from the telemetry source.
	 */
	public RawInputMetadata getMetadata();

	/**
	 * Retrieve a data input stream to read data from the source. For telemetry
	 * sources that reads in chunks of data at a time each call to this method
	 * should return subsequent <code>IRawInputStream</code> to access
	 * subsequent data. When available data from the input source ends, a call
	 * to this method should return null.
	 * 
	 * @return <code>IRawInputStream</code> from the raw input source, or null
	 *         if there is no more data from the source.
	 */
	public IRawInputStream getRawInputStream();

	/**
	 * Returns a connection string that specifies information regarding the
	 * connection. Connection string contents depends on the connection type
	 * 
	 * @return String containing connection metadata or null if no connection
	 *         has been established.
	 */
	public String getConnectionString();

	/**
	 * Terminates the raw input connection and cleans up appropriately
	 * 
	 * @throws IOException On any I/O error
	 */
	public void closeConnection() throws IOException;

	/**
	 * Terminates the current connection (if still open) and establishes a new
	 * connection.
	 * 
	 * @throws RawInputException if it fails to reconnect
	 */
	public void reconnect() throws RawInputException;

	/**
	 * Indicates if the connection is still alive
	 * 
	 * @return true if the connection is still alive, false otherwise
	 */
	public boolean isConnected();

	/**
	 * Adds a connection listener to receive notifications of certain connection
	 * related events
	 * 
	 * @param listener the <code>RawInputConnectionStatusListener</code> to
	 *        handle certain connection related events
	 */
	public void addConnectionListener(RawInputConnectionStatusListener listener);

	/**
	 * Removes a connection listener
	 * 
	 * @param listener the <code>RawInputConnectionStatusListener</code> to
	 *        remove
	 */
	public void removeConnectionListener(
	        RawInputConnectionStatusListener listener);

	/**
	 * Indicates if the connection is to a data stream or a finite data set
	 * 
	 * @return true if the connection is to a data stream, false if the
	 *         connection is to a finite data set
	 */
	public boolean isDataStream();


    /**
     * Returns true if EOF-on-stream. Used in RawInputHandler.
     *
     * @return Status
     */
    public boolean needsEofOnStreamStatus(); //
    
	/**
	 * Indicates if this IRawInputConnection should reconnect after an
	 * IOException
	 * 
	 * @param processorEofStatus
	 *            True if an EOF signal has been received from the
	 *            RawInputStream, false otherwise
	 * @return True if this IRawINputConnection should reconnect, false otherwise
	 * 
	 */
    public boolean shouldReconnectOnIOException(boolean processorEofStatus);
}
