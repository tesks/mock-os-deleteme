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
package jpl.gds.telem.input.api.data.helper;

import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;

/**
 * This interface defines the necessary functionalities for classes to be a
 * Transfer Frame Processor Helper. Transfer Frame Processor Helper classes
 * extracts data from IRawDataMessage to build objects required for Transfer
 * Frame data processing.
 * 
 *
 */
public interface ITfProcessorHelper extends IDataProcessorHelper {

	/**
	 * Extracts relevant data from <code>IRawDataMessage</code> to create a
	 * <code>IFrameInfo</code> object
	 * 
	 * @param message the <code>IRawDataMessage</code> object containing the
	 *        necessary data to create a <code>IFrameInfo</code> object
	 * @return the <code>IFrameInfo</code> object created from the data in the
	 *         provided <code>IRawDataMessage</code> object
	 * @throws RawInputException if there is an error creating the <code>IFrameInfo</code> object.
	 */
	public ITelemetryFrameInfo getFrameInfo(IRawDataMessage message)
	        throws RawInputException;

	/**
	 * Extracts relevant data from <code>IRawDataMessage</code> to create a
	 * <code>ITelemetryFrameHeader</code> object
	 * 
	 * @param message the <code>IRawDataMessage</code> object containing the
	 *        necessary data to create a <code>ITelemetryFrameHeader</code> object
	 * @return the <code>ITelemetryFrameHeader</code> object created from the data in the
	 *         provided <code>IRawDataMessage</code> object
	 * @throws RawInputException if there is an error creating the <code>ITelemetryFrameHeader</code> object.
	 */
	public ITelemetryFrameHeader getFrameHeader(IRawDataMessage message)
	        throws RawInputException;

	/**
	 * Extracts relevant data from <code>RawInputMetadata</code> to create a
	 * <code>ITransferFrameDefinition</code> object
	 * @param message the <code>IRawDataMessage</code> object containing the
	 *        necessary data to create a <code>TransferFrameDefinition</code> object
	 * 
	 * @return the <code>ITransferFrameDefinition</code> object created from the data
	 *         in the provided <code>RawInputMetadata</code> object
	 * @throws RawInputException if there is an error creating the
	 *         <code>ITransferFrameDefinition</code> object.
	 */
	public ITransferFrameDefinition getTransferFrameFormat(IRawDataMessage message)
	        throws RawInputException;
	
	/**
	 * Returns the size of the frame data in the given message in number of bytes
	 * @param message the message containing raw frame data
	 * @return the size of the frame in number of bytes
	 * @throws RawInputException if the number of bytes cannot be determined
	 */
	public int getNumFrameBytes(IRawDataMessage message) throws RawInputException;
}
