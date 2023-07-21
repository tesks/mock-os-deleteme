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

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.message.IRawDataMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;

/**
 * This interface defines the necessary functionalities for classes to be a
 * Packet Processor Helper. Packet Processor Helper classes extracts metadata
 * from RawInputMetadata to build objects required for Packet data processing.
 * 
 *
 */
public interface IPktProcessorHelper extends IDataProcessorHelper {
	/**
	 * Extracts relevant data from <code>IRawDataMessage</code> to create a
	 * <code>ISpacePacketHeader</code> object
	 * 
	 * @param message the <code>IRawDataMessage</code> object containing the
	 *        necessary data to create a <code>ISpacePacketHeader</code> object
	 * @return the <code>ISpacePacketHeader</code> object created from the data in
	 *         the provided <code>IRawDataMessage</code> object
	 * @throws RawInputException if there is an error creating the
	 *         <code>ISpacePacketHeader</code> object.
	 */
	public ISpacePacketHeader getPacketHeader(IRawDataMessage message)
	        throws RawInputException;

	/**
	 * Extracts relevant data from <code>RawInputMetadata</code> to create a
	 * <code>IPacketInfo</code> object
	 * 
	 * @param message the <code>RawInputMetadata</code> object containing the
	 *        necessary data to create a <code>IPacketInfo</code> object
	 * @return the <code>IPacketInfo</code> object created from the data in the
	 *         provided <code>RawInputMetadata</code> object
	 * @throws RawInputException if there is an error creating the
	 *         <code>IPacketInfo</code> object.
	 */
	public ITelemetryPacketInfo getPacketInfo(IRawDataMessage message)
	        throws RawInputException;

	/**
	 * Returns an IFrameInfo with default SCID, FCFC, and VCID, since packet
	 * header does not provide this information.
	 * 
	 * @param message the <code>RawInputMetadata</code> object containing the
	 *        necessary data to create a <code>IFrameInfo</code> object
	 * @return the <code>IFrameInfo</code> object with default values
	 * @throws RawInputException RawInputException if there is an error creating
	 *         the <code>IFrameInfo</code> object.
	 */
	public ITelemetryFrameInfo getSourceFrameInfo(IRawDataMessage message)
	        throws RawInputException;

}
