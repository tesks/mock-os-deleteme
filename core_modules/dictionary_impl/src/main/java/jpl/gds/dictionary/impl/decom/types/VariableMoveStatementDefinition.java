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
package jpl.gds.dictionary.impl.decom.types;

import jpl.gds.dictionary.api.decom.types.IVariableMoveStatementDefinition;

/**
 * Implementation class for move statements with a variable offset value.
 *
 */
public class VariableMoveStatementDefinition implements IVariableMoveStatementDefinition {

	private final String offsetVariable;
	private final Direction direction;
	private final int multiplier;
	
	/**
	 * Create a new move statement definition.
	 * @param offsetVariable the name of the variable that will hold the bit offset for the move
	 * @param direction the direction (how to interpret the offset)
	 * @param multiplier the multiplier to apply to the offsetAmount to convert to bits
	 */
	public VariableMoveStatementDefinition(String offsetVariable, Direction direction, int multiplier) {
		this.offsetVariable = offsetVariable;
		this.direction = direction;
		this.multiplier = multiplier;
	}

	@Override
	public Direction getDirection() {
		return direction;
	}

	@Override
	public String getOffsetVariable() {
		return offsetVariable;
	}

	@Override
	public int offsetMultiplier() {
		return multiplier;
	}

}
