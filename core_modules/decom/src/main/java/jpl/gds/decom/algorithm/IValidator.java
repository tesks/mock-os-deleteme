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
package jpl.gds.decom.algorithm;

import java.util.Map;

import jpl.gds.shared.algorithm.IGenericAlgorithm;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.types.BitBuffer;

/**
 * Interface for algorithms that perform some sort of
 * validation logic on arbitrary data.
 *
 */
@CustomerAccessible(immutable = true)
public interface IValidator extends IGenericAlgorithm {

	/**
	 * Validate the data within the provided buffer.
	 * @param buffer read-only input buffer
	 * @param args the runtime arguments to the algorithm 
	 * @return true if the buffer contains valid data,
	 * 			otherwise false
	 */
	public boolean validate(BitBuffer buffer, Map<String, Object> args);
}
