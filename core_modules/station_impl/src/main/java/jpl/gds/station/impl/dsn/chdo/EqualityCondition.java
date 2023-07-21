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
package jpl.gds.station.impl.dsn.chdo;

import jpl.gds.station.api.dsn.chdo.IEqualityCondition;

/**
 * This class defines an equality or non-equality condition for a CHDO field. 
 */
class EqualityCondition implements IEqualityCondition
{
	private String name;
	private String value;
	private boolean equalityValue;
	
	/**
	 * Constructs an Equality condition.
	 * 
	 * @param name Name of this condition
	 * @param value value to compare the actual CHDO field value with
	 * @param equalityValue true if we want the values to be equal, false if we want them not equal
	 */
	public EqualityCondition(final String name,final String value, final boolean equalityValue)
	{
		this.name = name;
		this.value = value;
		this.equalityValue = equalityValue;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IEqualityCondition#getEqualityValue()
     */
	@Override
    public boolean getEqualityValue()
	{
		return this.equalityValue;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IEqualityCondition#setEqualityValue(boolean)
     */
	@Override
    public void setEqualityValue(boolean equalityValue)
	{
		this.equalityValue = equalityValue;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IEqualityCondition#getName()
     */
	@Override
    public String getName()
	{
		return this.name;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IEqualityCondition#setName(java.lang.String)
     */
	@Override
    public void setName(final String name)
	{
		this.name = name;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IEqualityCondition#getValue()
     */
	@Override
    public String getValue()
	{
		return this.value;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IEqualityCondition#setValue(java.lang.String)
     */
	@Override
    public void setValue(final String value)
	{
		this.value = value;
	}
}