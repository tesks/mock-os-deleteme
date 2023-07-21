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


import jpl.gds.dictionary.api.decom.params.NumericParams;
import jpl.gds.dictionary.api.decom.types.INumericDataDefinition;
import jpl.gds.dictionary.api.eu.IEUDefinition;

import java.util.Optional;

/**
 * Base class for definitions of numeric data occurring within decom maps.
 *
 */
public abstract class NumericDataDefinition extends StorableDataDefinition implements INumericDataDefinition {
	private final int bitLength;
	private String units;
	private final String unitTypes;
	private Optional<IEUDefinition> euDef;
	private String euFormat;
	private boolean hasEu;

	/**
	 * Create a new instance initialized from the given parameter object.
	 * @param params
	 */
	public NumericDataDefinition(NumericParams params) {
		super(params);
		euFormat = params.getEuFormat();
		bitLength = params.getBitLength();
		units = params.getUnits();
		unitTypes = params.getUnitsType();
		euDef = Optional.ofNullable(params.getDnToEu());
	}

	@Override
	public int getBitLength() {
		return bitLength;
	}

	@Override
	public String getUnits() {
		return units;
	}

	@Override
	public String getUnitsType() {
		return unitTypes;
	}

	@Override
	public IEUDefinition getDnToEu() {
		return euDef.orElse(null);
	}


	@Override
	public boolean hasEu() {
		return hasEu && euDef.isPresent();
	}

	@Override
	public String getEuUnits() {
		return units;
	}

	@Override
	public String getEuFormat() {
		return euFormat;
	}

	@Override
	public String toString() {
		return super.toString() + ", bit_length=" + bitLength;
	}

}
