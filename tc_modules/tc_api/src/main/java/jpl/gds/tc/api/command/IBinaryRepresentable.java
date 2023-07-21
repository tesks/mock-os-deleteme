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
package jpl.gds.tc.api.command;

import jpl.gds.tc.api.exception.BlockException;
import jpl.gds.tc.api.exception.UnblockException;

/**
 *
 * Interface implemented by all command-related objects that need to be translated
 * to and from bit strings (e.g. command arguments).
 *
 *
 */
public interface IBinaryRepresentable
{
	/**
	 * Translate the current object into a binary representation.  This is the logical
	 * inverse of the "parseFromBitString" function.
	 * 
	 * @return A bit string representation of the current object
	 * (the string only contains '1' and '0' characters).
	 * 
	 * @throws BlockException If a translation error occurs.
	 */
	public abstract String toBitString() throws BlockException;
    
	/**
	 * Parse the input binary string and use the values contained therein to set the corresponding
	 * fields on this object.  This is the logical inverse of the "toBitString" function.
	 * 
	 * @param bitString The bit string to parse data from (only contains '1' and '0' characters)
	 * 
	 * @param offset The offset into the bit string where parsing should begin
	 * 
	 * @return An integer indicating the number of bits that were read.
	 * 
	 * @throws UnblockException If a translation error occurs.
	 */
    public abstract int parseFromBitString(final String bitString,final int offset) throws UnblockException;
}
