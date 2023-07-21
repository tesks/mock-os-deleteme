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
 * Directs a decom processor to move some number of bits before
 * processing the next decom instruction in a decom map.
 *
 */
public interface IMoveStatementDefinition extends IDecomStatement {

	/**
	 * This enum represents how to interpret the numeric offset
	 * associated with the move definition.
	 *
	 */
	public enum Direction {
		/** 
		 * Indicates that the bit offset should be interpreted as a positive number
		 * of bits to skip ahead in data being decommutated.
		 */
		FORWARD,
		/**
		 * Indicates that the bit offset should be interpreted as a positive number of bits
		 * to skip backwards in data being decommutated.
		 */
		BACKWARD,

		/**
		 * Indicates that the bit offset should be interpreted as an absolute offset
		 * from the beginning of the data block the containing map is being applied to.
		 */
		ABSOLUTE
	}
	
	/**
	 * Returns a multiplier for a move offset. Will be 1 if the offset is given in number
	 * of bits or 8 if given in bytes, for example.
	 * @return the multiplier to apply to an offset value to convert offset to bits 
	 */
	public int offsetMultiplier();
		
	/**
	 * 
	 * @return the direction to be used to interpret how to apply the offset.
	 */
	public Direction getDirection();

}
