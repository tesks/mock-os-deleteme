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


import jpl.gds.dictionary.api.decom.params.RepeatBlockParams;
import jpl.gds.dictionary.api.decom.types.IRepeatBlockDefinition;

/**
 * Implementation class for repeat blocks that occur within decom maps.
 *
 */
public class RepeatBlockDefinition extends StatementContainerDefinition implements IRepeatBlockDefinition {

	private final LengthType lengthType;
	private final int bitLength;

	/**
	 * Create a new instance initialized from the given parameter object.
	 * @param params
	 */
	public RepeatBlockDefinition(RepeatBlockParams params) {
		super(params);
		lengthType = params.getLengthType();
		bitLength = params.getBitLength();
	}

	@Override
	public LengthType getLengthType() {
		return lengthType;
	}

	@Override
	public int getLength() {
		return bitLength;
	}


}
