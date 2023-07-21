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
/**
 * 
 */
package jpl.gds.dictionary.impl.decom;

import jpl.gds.dictionary.api.decom.ISkipStatementDefinition;

/**
 * 
 * This class represents a skip statement in a generic decom map used for
 * packet decommutation.
 * 
 *
 *
 */
public class SkipStatementDefinition extends Statement implements ISkipStatementDefinition {
	
	private int skipBits;
	
	/**
	 * Constructor.
	 * 
	 * @param bitsToSkip the number of bits to skip in the data stream.
	 */
	/* package */ SkipStatementDefinition(final int bitsToSkip) {
		
		if (bitsToSkip < 0) {
			throw new IllegalArgumentException("Cannot skip negative number of bits (got " + bitsToSkip + ")");
		}
		
		skipBits = bitsToSkip;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISkipStatementDefinition#getNumberOfBitsToSkip()
	 */
	@Override
    public int getNumberOfBitsToSkip() {
		return skipBits;
	}
 
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISkipStatementDefinition#setNumberOfBitsToSkip(int)
     */
    @Override
    public void setNumberOfBitsToSkip(final int bitsToSkip) {
        if (bitsToSkip < 0) {
            throw new IllegalArgumentException("Cannot skip negative number of bits (got " + bitsToSkip + ")");
        }       
        skipBits = bitsToSkip;
    }
	
}
