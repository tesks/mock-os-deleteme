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

import jpl.gds.dictionary.api.decom.params.DynamicArrayParams;
import jpl.gds.dictionary.api.decom.types.IDynamicArrayDefinition;

/**
 * Implementation class for dynamic arrays occuring in generic decom maps.
 *
 */
public class DynamicArrayDefinition extends BaseCompositeDataDefinition implements IDynamicArrayDefinition {

	private final String varName;
	
	/**
	 * Create an instance initialized from the given parameter object.
	 * @param params
	 */
	public DynamicArrayDefinition(DynamicArrayParams params) {
		super(params);
		varName = params.getLengthVariableName();
	}

	@Override
	public String getLengthVariableName() {
		return varName;
	}

}
