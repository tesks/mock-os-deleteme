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

import jpl.gds.dictionary.api.decom.IStatementContainer;

/**
 * Represents a block of decommutation statements that should be executed
 * by a decom processor until the length limit is reached, or a break statement
 * is encountered.  The repeating block can define either an absolute length,
 * a maximum length, or no length at all.  Decom processors should use
 * the type of declared length to interpret how to evaluate the repeat block.
 *
 */
public interface IRepeatBlockDefinition extends IStatementContainer {
	
	
	/**
	 * Indicates how the length of a repeat block should be interpreted.
	 *
	 */
	public enum LengthType {
		/**
		 *  Indicates the length of the block is absolute.  Upon early termination
		 *  of the repeat block, a decom processor should skip the remaining bits.
		 */
		ABSOLUTE,
		/**
		 * Indicates the length of the block is only a maximum, and early termination
		 * of the block should not result in skipping any bits. The decom processor
		 * should terminate the repeat block once it has processed this number of bits.
		 */
		MAX,
		/**
		 * The length of the repeat block is not specified. The decom processor should
		 * continue until all data is consumed or a break statement is encountered.
		 */
		ABSENT 
	}
	
	/**
	 * 
	 * Get the type of length specification for the repeat block. Needed
	 * in order to interpret how to handle the termination of the repeat block.
	 * @return the type of length specification for the repeat block
	 */
	public LengthType getLengthType();
	
	/**
	 * Get the bit length associated with the repeat block. If the length type is
	 * {@linkplain LengthType#ABSENT}, can be ignored.
	 * @return the bit length associated with the repeat blcok
	 */
	public int getLength();

}
