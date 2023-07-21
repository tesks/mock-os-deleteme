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
package jpl.gds.telem.input.api.service;

import jpl.gds.shared.interfaces.IService;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.connection.RawInputConnectionStatusListener;

import java.io.IOException;

/**
 * An interface to be implemented by the telemetry input service, which is responsible for
 * all telemetry acquisition from a data source.
 * 
 *
 * @since R8
 *
 */
public interface ITelemetryInputService extends IService, RawInputConnectionStatusListener {

    /**
     * Instructs the input service to connect to the telemetry source.
     * 
     * @return true if successful, false if not
     * 
     * @throws RawInputException if there is a problem making the connection
     */
    public boolean connect() throws RawInputException;
    
    /**
     * Instructs the input service to disconnect from the telemetry source.
     * 
     * @throws IOException if there is a problem breaking the connection
     */
    public void disconnect() throws IOException;
    
    /**
     * Commences reading of input telemetry. Classes interested in the processed raw
     * input should be subscribed to MessageContext prior to invoking this
     * method.
     * 
     * @throws RawInputException if errors are encountered during the processing
     *         of raw input
     */
    public void startReading() throws RawInputException;

    /**
     * Terminates reading of input telemetry. Does not disconnect from the source.
     * 
     * @throws IOException if an error was encountered.
     */
    public void stopReading() throws IOException;

    /**
     * Pauses telemetry input processing.
     */
    public void pause();

    /**
     * Resumes telemetry input processing.
     */
    public void resume();

    /**
     * Clear the buffer within the telemetry input service's input stream.
     * 
     * @throws UnsupportedOperationException
     *            if the IRawInputStream does not support buffer clearing
     * @throws IOException
     *             if the <code>clearBufferCallable</code> threw an exception,
     *             was interrupted while waiting, or could not be scheduled for
     *             execution
     * @throws IllegalStateException
     *             if the buffer is not in an operational state
     */
    public void clearInputStreamBuffer() throws UnsupportedOperationException,
            IOException, IllegalStateException;

    /**
     * Set the metering interval. This is how long the system will wait after
     * processing each block of data read from the source.
     * 
     * @param meterInterval the meter interval in milliseconds.
     */
    public void setMeterInterval(long meterInterval);

    /**
     * Gets whether or not the input service is in a connected state
     *
     * @return true if the input service is connected; false otherwise
     */
    boolean isConnected();

    /**
     * Gets whether or not the input service is reading telemetry
     *
     * @return true if the input service is reading; false otherwise
     */
    boolean isReading();

}