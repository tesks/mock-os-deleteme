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

import jpl.gds.dictionary.api.decom.IWidthStatementDefinition;

/**
 * This class represents a width statement in a generic decom map used for
 * packet decommutation.
 * 
 *
 */
public class WidthStatementDefinition implements IWidthStatementDefinition {
	
	private int width;

	/**
	 * Constructor.
	 * 
	 * @param w width in bits
	 */
	/*package*/ WidthStatementDefinition(final int w) {
		
		if (w < 1) {
			throw new IllegalArgumentException("Width cannot be 0 or negative (got " + w + ")");
		}
		
		width = w;
	}
		
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IWidthStatementDefinition#getWidth()
	 */
	@Override
    public int getWidth() {
		return width;
	}

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IWidthStatementDefinition#setWidth(int)
     */
    @Override
    public void setWidth(final int w) {
        
        if (w < 1) {
            throw new IllegalArgumentException("Width cannot be 0 or negative (got " + w + ")");
        } else {        
            width = w;
        }
    }
	
}
