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

import jpl.gds.dictionary.api.decom.params.TimeParams;
import jpl.gds.dictionary.api.decom.types.ITimeDefinition;

/**
 * Implementation class for time tags defined within decom maps.
 *
 */
public class TimeDefinition extends AlgorithmInvocation implements ITimeDefinition {

	private final boolean isDelta;

	/**
	 * Create a new instance initialized from the given parameter object.
	 * @param timeParams
	 */
	public TimeDefinition(TimeParams timeParams) {
		super(timeParams);
		isDelta = timeParams.isDelta();
	}

	@Override
	public boolean isDelta() {
		return isDelta;
	}

}
