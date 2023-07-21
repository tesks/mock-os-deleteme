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


import jpl.gds.dictionary.api.decom.params.CaseParams;
import jpl.gds.dictionary.api.decom.types.ICaseBlockDefinition;

/**
 * Implementation class for generic decom case block definitions.
 *
 */
public class CaseBlockDefinition extends StatementContainerDefinition implements ICaseBlockDefinition {
	
	private final boolean isDefault;
	private final long value;

	/**
	 * Create a new instance initialized from the  given paramaters.
	 * @param params
	 */
	public CaseBlockDefinition(CaseParams params) {
		super(params);
		value = params.getValue();
		isDefault = params.isDefault();
	}

	@Override
	public long getValue() {
		return value;
	}

	@Override
	public boolean isDefault() {
		return isDefault;
	}

}
