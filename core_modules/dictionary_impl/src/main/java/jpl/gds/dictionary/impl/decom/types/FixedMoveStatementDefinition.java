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

import jpl.gds.dictionary.api.decom.types.IFixedMoveStatementDefinition;

/**
 * Implementation class for move statements with constant offset values.
 *
 */
public class FixedMoveStatementDefinition implements IFixedMoveStatementDefinition {

	private final int offset;
	private final Direction direction;
	private final int multiplier;
	
	/**
	 * Create a new move statement definition.
	 * @param offset the numeric bit offset for the move
	 * @param direction the direction (how to interpret the offset)
	 * @param multiplier 
	 */
	public FixedMoveStatementDefinition(int offset, Direction direction, int multiplier) {
		this.offset = offset;
		this.direction = direction;
		this.multiplier = multiplier;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	@Override
	public Direction getDirection() {
		return direction;
	}

	@Override
	public int offsetMultiplier() {
		return multiplier;
	}

}
