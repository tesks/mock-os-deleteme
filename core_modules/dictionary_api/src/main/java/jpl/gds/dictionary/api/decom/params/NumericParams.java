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
package jpl.gds.dictionary.api.decom.params;

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.types.INumericDataDefinition;
import jpl.gds.dictionary.api.eu.IEUDefinition;
import jpl.gds.dictionary.api.eu.IEUSupport;

/**
 * Parameter builder class for creating {@link INumericDataDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public abstract class NumericParams extends StorableDataParams implements IEUSupport {

	private int bitLength;
	
	private String units;
	
	private String unitsType;
	
	private String euFormat = "%16.9e";

	private IEUDefinition dnToEu;

	private boolean hasEu;

	private String euUnits;
	
	/**
	 * Set the bit length of the numeric data
	 * @param bitLength
	 */
	public void setBitLength(int bitLength) {
		this.bitLength = bitLength;
	}
	
	/**
	 * Set the units for the numeric data.
	 * If the units argument is null, it is substituted
	 * for the empty string.
	 * @param units
	 */
	public void setUnits(String units) {
		if (units == null) {
			this.units = "";
		} else {
			this.units = units;
		}
	}
	
	/**
	 * Set the unit type for the numeric data.
	 * If the units type argument is null, it is substituted
	 * for the empty string.
	 * @param unitsType
	 */
	public void setUnitsType(String unitsType) {
		if (unitsType == null) {
			this.unitsType = "";
		} else {
			this.unitsType = unitsType;
		}
	}
	
	/**
	 * @see INumericDataDefinition#getBitLength()
	 * @return the length, in bits, of the numeric data
	 */
	public int getBitLength() {
		return bitLength;
	}

	/**
	 * @see INumericDataDefinition#getUnits()
	 * @return the units string to be displayed along with the numeric data
	 */
	public String getUnits() {
		return units;
	}

	/**
	 * @see INumericDataDefinition#getUnitsType()
	 * @return the units type string to be displayed along with the numeric data
	 */
	public String getUnitsType() {
		return unitsType;
	}

	@Override
	public void reset() {
		super.reset();
		unitsType = "";
		units = "";
		bitLength = 0;
	}

	@Override
	public String getEuFormat() {
		return euFormat;
	}
	
	@Override
	public void setEuFormat(String format) {
		euFormat = format;
	}
	
	@Override
	public void setDnToEu(IEUDefinition dnToEu) {
		this.dnToEu = dnToEu;
	}


	@Override
	public void setHasEu(boolean yes) {
		this.hasEu = yes;
	}


	@Override
	public void setEuUnits(String unitStr) {
		this.euUnits = unitStr;
		
	}


	@Override
	public IEUDefinition getDnToEu() {
		return dnToEu;
	}


	@Override
	public boolean hasEu() {
		return hasEu;
	}


	@Override
	public String getEuUnits() {
		return euUnits;
	}
}
