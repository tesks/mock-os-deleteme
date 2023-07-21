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

import jpl.gds.dictionary.api.decom.IDecomStatement;

/**
 * Base interface for information common to all data within decom maps.
 * This interface should be extended by all new interfaces defining data types
 * that can be extracted or labelled via generic decom.
 * 
 *
 */
public interface IDecomDataDefinition extends IDecomStatement {

	/**
	 * Get the format string that should be used for printing the decom
	 * data for viewing applications.
	 * @return the string format
	 */
	public String getFormat();
	
	/**
	 * Get the name of the data field that should be used for printing the
	 * decom data for viewing applications. The name is also used to reference
	 * the data as a variable in other decom map elements, if the data is
	 * usable and marked as a variable.
	 * @return the name for the data
	 */
	public String getName();
	
	/**
	 * Get the integer bit offset this data starts at relative to a virtual cursor
	 * during decommutation. Only valid when {@link #offsetSpecified()} returns
	 * true.
	 * @return the bit offset this data starts at
	 */
	public int getBitOffset();
	
	/**
	 * Check whether the offset was specified for this decom data.  If this is false,
	 * the offset {@link #getBitOffset()} should be disregarded, and the offset should
	 * be assumed to be at the bit immediately following the decom processor's current position.
	 * @return  true if the offset is specified (and therefore valid) for this data
	 */
	public boolean offsetSpecified();
	
	/**
	 * Get the description string for this data.
	 * @return the description as a string
	 */
	public String getDescription();

}
