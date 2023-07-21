/*
 * Copyright 2006-2016. California Institute of Technology.
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
package jpl.gds.dictionary.api.decom.types;

import jpl.gds.dictionary.api.decom.IDecomStatement;

/**
 * Represents a variable which may be used in a decom map that has a value
 * which is defined within the map itself, as opposed to being extracted from telemetry data.
 * 
 *
 */
public interface IGroundVariableDefinition extends IDecomStatement {
	
	/**
	 * Get the variable name
	 * @return the string by which the variable should be referenced
	 */
	public String getName();
	
	/**
	 * Get the variable's value
	 * @return the value the variable has been set to
	 */
	public long getValue();

}
