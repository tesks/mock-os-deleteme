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
package jpl.gds.telem.input.api.data;

import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.config.StreamType;
import jpl.gds.telem.input.api.data.helper.IDataProcessorHelper;

/**
 * Instances of <code>IRawDataProcessor</code> is responsible for subscribing to
 * internal MPCS messaging service and processing incoming raw data in the form
 * of <code>IRawDataMessage</code> objects.
 * 
 *
 */
public interface IRawDataProcessor extends MessageSubscriber {

	/**
	 * Initializes the raw data processor.
	 * 
	 * @param helper the IDataProcessorHelper object for this raw data processor
	 * @param streamType the stream type that will determine which
	 *        RawDataMessage this data processor subscribes to
	 * @param connType the connection type
	 * 
	 * @throws RawInputException if initialization fails
	 */
	public void init(IDataProcessorHelper helper, StreamType streamType, TelemetryConnectionType connType)
	        throws RawInputException;

	/**
	 * Starts the data processor
	 */
	public void start();
	
	/**
	 * Stops the data processor
	 */
	public void stop();
}
