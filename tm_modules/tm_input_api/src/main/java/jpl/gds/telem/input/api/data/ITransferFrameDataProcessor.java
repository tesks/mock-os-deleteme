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

import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;

/**
 * An interface to be implemented by IRawDataProcessor classes that process
 * transfer frames.
 * 
 *
 * @since R8
 */
public interface ITransferFrameDataProcessor extends IRawDataProcessor {

	/**
	 * Retrieve the ITelemetryFrameInfo of the last transfer frame processed
	 * 
	 * @return the IFrameInfo of the last transfer frame processed
	 */
	public abstract ITelemetryFrameInfo getLastTfInfo();

	/**
	 * Retrieve the ERT of the last transfer frame processed
	 * @return the ERT of the last transfer frame processed
	 */
	public abstract IAccurateDateTime getLastFrameErt();

}