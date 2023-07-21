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

import java.io.IOException;

import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.data.IRawDataProcessor;
import jpl.gds.telem.input.api.message.RawInputMetadata;

/**
 * <code>IRawStreamProcessor</code> is responsible for converting the raw data
 * read in by IRawInputConnection into <code>IRawDataMessages</code> and
 * publishing them on the MPCS <code>MessageContext</code>
 * 
 */
public interface IRawStreamProcessor {
	/**
	 * Initializes the raw stream processor
     *
     * @param inputType    Raw input type
     * @param isRemoteMode True if remote mode
	 */
	public void init(TelemetryInputType inputType, boolean isRemoteMode);

	/**
	 * Processes the raw <code>IRawInputStream</code> by converting the stream
	 * to internal MPCS messages
	 * 
	 * @param inputStream the input stream
	 * @param metadata the associated <code>IRawInputMetadata</code>
	 * @throws RawInputException if errors are encountered during raw data
	 *         processing
	 * @throws IOException if the processing encounters an error reading from
	 *         the stream
	 */
	public void processRawData(IRawInputStream inputStream,
	        RawInputMetadata metadata) throws RawInputException, IOException;

	/**
	 * Indicates if stream processing is in a paused state
	 * 
	 * @return true if stream processing is paused, false otherwise
	 */
	public boolean isPaused();

	/**
	 * Pause stream processing
	 */
	public void pause();

	/**
	 * Resume stream processing. Also sends a resume message. Does nothing if
	 * input was not paused.
	 */
	public void resume();

	/**
	 * Indicates if stream processing has stopped
	 * 
	 * @return true if stream processing has stopped, false otherwise
	 */
	public boolean isStopped();

	/**
	 * Stops stream processing. Also sends a stop message. Does nothing if input
	 * is already stopped.
	 */
	public void stop();

	/**
	 * Indicates whether this input object is still awaiting its first data.
	 * 
	 * @return true if no data has been read yet; false if it has
	 */
	public boolean awaitingFirstData();

	/**
	 * Sets the flag indicating whether this input object is still awaiting its
	 * first data.
	 * 
	 * @param isFirst true if first data has been not been read; false if it has
	 */
	public void setAwaitingFirstData(boolean isFirst);

	/**
	 * Gets the interval between reads of input.
	 * 
	 * @return Returns the interval in milliseconds.
	 */
	public long getMeterInterval();

	/**
	 * Sets the interval between reads of input.
	 * 
	 * @param meterInterval the time interval in milliseconds
	 */
	public void setMeterInterval(final long meterInterval);

	/**
	 * Sets whether or not the connection has been lost.
	 * 
	 * @param setConnectionLost true if connection is lost, false otherwise
	 */
	public void setConnectionLost(boolean setConnectionLost);


    /**
     * Get status of EOF on stream.
     *
     * @return Status
     */
    public boolean getEofOnStreamStatus();


    /**
     * Setter for EOF on stream status.
     *
     * @param status True if EOF detected
     */
    public void setEofOnStreamStatus(final boolean status);

    /**
     * Setter for the IRawDataProcessor
     * 
     *
     * @param dataProc
     *            IRawDataProcessor
     */
    public void setDataProcessor(final IRawDataProcessor dataProc);
}
