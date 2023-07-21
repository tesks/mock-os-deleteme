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

import jpl.gds.dictionary.api.decom.params.BooleanParams;
import jpl.gds.dictionary.api.decom.types.IBooleanDefinition;

/**
 * Simple, immutable data class that holds information pertaining to
 * a boolean field in a decom map.
 * 
 *
 */
public class BooleanDefinition extends NumericDataDefinition implements IBooleanDefinition {
	
	private final String trueString;
	private final String falseString;

	/**
	 * Create a boolean definition instance with fields initialized
	 * from the parameter object passed in.
	 * @param params field parameters to initialize members from
	 */
	public BooleanDefinition(BooleanParams params) {
		super(params);
		trueString = params.getTrueString();
		falseString = params.getFalseString();
	}

	@Override
	public String getTrueString() {
		return trueString;
	}

	@Override
	public String getFalseString() {
		return falseString;
	}
	
	

}
