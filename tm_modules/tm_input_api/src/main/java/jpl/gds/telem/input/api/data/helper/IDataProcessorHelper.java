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

import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.telem.input.api.RawInputException;

/**
 * This interface defines the required methods for an
 * <code>IDataProcessorHelper</code>
 * 
 *
 */
public interface IDataProcessorHelper {
	/**
	 * Initializes the helper
	 * 
	 * @param inputType the raw input type
	 * @throws RawInputException if initialization failed. Do not proceed to use
	 *         this class if initialization does not succeed. Behavior cannot be
	 *         guaranteed to be correct.
	 */
	public void init(TelemetryInputType inputType) throws RawInputException;
}
