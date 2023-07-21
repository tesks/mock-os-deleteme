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

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.decom.params.EnumDataParams;
import jpl.gds.dictionary.api.decom.types.IEnumDataDefinition;

/**
 * Implementation class for enum definitions within generic decom maps.
 *
 */
public class EnumDataDefinition extends NumericDataDefinition implements IEnumDataDefinition {

	private final String enumFormat;
	private final String enumName;
	
	private EnumerationDefinition table;
	
	/**
	 * Create an instance initialized with the given parameter object.
	 * @param params
	 */
	public EnumDataDefinition(EnumDataParams params) {
		super(params);
		enumFormat = params.getEnumFormat();
		enumName = params.getEnumName();
		table = params.getEnumDefinition();
	}

	@Override
	public String getEnumFormat() {
		return enumFormat;
	}

	@Override
	public String getEnumName() {
		return enumName;
	}

	@Override
	public void setLookupTable(EnumerationDefinition table) {
		this.table = table;
	}

	@Override
	public EnumerationDefinition getLookupTable() {
		return table;
	}

}
