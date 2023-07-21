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
package jpl.gds.telem.input.api.message;

import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;


/**
 * This interface defines the required methods for an
 * <code>IRawDataMessage</code>. <code>IRawDataMessage</code> instances are
 * internal MPCS messages that are used to transport raw data from the stream
 * processor to the data processors. It is the intermediate format between the
 * raw data stream and processed data.
 * 
 *
 */
public interface IRawDataMessage {
	/**
	 * Retrieve the metadata for this chunk of telemetry
	 * 
	 * @return a <code>RawInputMetadata</code> object containing the metadata
	 */
	public RawInputMetadata getMetadata();

	/**
	 * Retrieve the data of this chunk of telemetry
	 * 
	 * @return a byte[] representing the data
	 */
	public byte[] getData();


	/**
	 * Retrieve the header of this chunk of telemetry.
     * May be zero-length or NULL or populated.
	 * 
	 * @return a HeaderHolder representing the header
	 */
	public HeaderHolder getRawHeader();


	/**
	 * Retrieve the trailer of this chunk of telemetry.
     * May be zero-length or NULL or populated.
	 * 
	 * @return a TrailerHolder representing the trailer
	 */
	public TrailerHolder getRawTrailer();
}
