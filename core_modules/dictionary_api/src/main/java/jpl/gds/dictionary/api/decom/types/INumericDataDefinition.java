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
package jpl.gds.dictionary.api.decom.types;

import jpl.gds.dictionary.api.eu.IImmutableEUSupport;

/**
 * Base interface for numeric decom data types.  Holds information
 * common to those types.
 *
 */
public interface INumericDataDefinition extends IStorableDataDefinition, IImmutableEUSupport  {

	/**
	 * 
	 * @return the length, in bits, of the numeric data
	 */
	public int getBitLength();

	/**
	 * 
	 * @return the units to display with the numeric data
	 */
	public String getUnits(); 

	/**
	 * 
	 * @return the type of units to display with the numeric data
	 */
	public String getUnitsType(); 

}
