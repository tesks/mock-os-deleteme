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

import jpl.gds.dictionary.api.decom.IOffsetStatementDefinition;

/**
 * 
 * This class represents an offset statement in a generic decom map used for
 * packet decommutation.
 * 
 *
 */
public class OffsetStatementDefinition extends Statement implements IOffsetStatementDefinition{

	private final boolean dataOffset;
	private int offsetValue;
	private OffsetType offsetType;

	/**
	 * Constructor for use when the offset is DATA.
	 */
	/*package*/ OffsetStatementDefinition() {
		dataOffset = true;
	}
	
	/**
     * Constructor for use when the offset is not DATA (is an actual offset).
     * 
     * @param offset offset value in bits
     * @param type the OffsetType enumeration value
     */
	/*package*/ OffsetStatementDefinition(final int offset, final OffsetType type) {
		
		if (offset < 0) {
			throw new IllegalArgumentException("Offset cannot be negative (got " + offset + ")");
		}
		
		if (type == null) {
			throw new IllegalArgumentException("null offset type encountered");
		}
		
		dataOffset = false;
		offsetValue = offset;
		offsetType = type;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IOffsetStatementDefinition#isDataOffset()
	 */
	@Override
    public boolean isDataOffset() {
		return dataOffset;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IOffsetStatementDefinition#getOffset()
	 */
	@Override
    public int getOffset() {
		return offsetValue;
	}
	
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IOffsetStatementDefinition#setOffset(int)
     */
    @Override
    public void setOffset(final int offValue) {
        offsetValue = offValue;
    }

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IOffsetStatementDefinition#getOffsetType()
	 */
	@Override
    public OffsetType getOffsetType() {
		return offsetType;
	}

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IOffsetStatementDefinition#setOffsetType(jpl.gds.dictionary.impl.impl.api.decom.adaptation.IOffsetStatementDefinition.OffsetType)
     */
    @Override
    public void setOffsetType(final OffsetType offType) {
        offsetType = offType;
    }

}
