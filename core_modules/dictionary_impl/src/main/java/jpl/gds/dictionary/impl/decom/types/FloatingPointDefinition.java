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

import jpl.gds.dictionary.api.decom.params.FloatingPointParams;
import jpl.gds.dictionary.api.decom.types.IFloatingPointDefinition;

/**
 * Implementation class for floating point data definition occuring in generic decom maps. 
 *
 */
public class FloatingPointDefinition extends NumericDataDefinition implements IFloatingPointDefinition {

	private final FloatEncoding encoding;
	private final Precision precision;
	
	/**
	 * Create a new instance initialized from the given parameter object.
	 * @param params
	 */
	public FloatingPointDefinition(FloatingPointParams params) {
		super(params);
		encoding = params.getEncoding();
		precision = params.getPrecision();
		if (params.getFormat() == null) {
			format = "%16.9e";
		}
	}

	@Override
	public FloatEncoding getEncoding() {
		return encoding;
	}

	@Override
	public Precision getPrecision() {
		return precision;
	}

}
