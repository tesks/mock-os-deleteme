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

import java.util.ArrayList;
import java.util.List;

import jpl.gds.station.api.dsn.chdo.IChdoCondition;
import jpl.gds.station.api.dsn.chdo.IChdoDefinition;
import jpl.gds.station.api.dsn.chdo.IEqualityCondition;

/**
 * This class represents a conditional evaluation of CHDO fields. Conditions are
 * defined in the chdo.xml file and consist of a series of comparisons between
 * expected CHDO field values and actual values in the CHDO. Comparisons may be
 * either "equals" or "not equals" comparisons.
 * 
 */
class ChdoCondition implements IChdoCondition
{
	private int chdoType;
	private List<IEqualityCondition> equalityConditions;
	
	/**
	 * Defines a condition for the CHDO type defined in the given ChdoDefinition.
	 * 
	 * @param chdoDef the ChdoDefinition object for the CHDO type to evaluate
	 */
	public ChdoCondition(final IChdoDefinition chdoDef)
	{
		this.chdoType = chdoDef.getType();
		this.equalityConditions = new ArrayList<IEqualityCondition>(5);
	}
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoCondition#addEqualityCondition(java.lang.String, java.lang.String, boolean)
     */
	@Override
    public void addEqualityCondition(final String name,final String value, final boolean equalityValue)
	{
		EqualityCondition ec = new EqualityCondition(name,value,equalityValue);
		
		if(this.equalityConditions.contains(ec) == false)
		{
			this.equalityConditions.add(ec);
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoCondition#getChdoType()
     */
	@Override
    public int getChdoType()
	{
		return this.chdoType;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoCondition#getEqualityConditions()
     */
	@Override
    public List<IEqualityCondition> getEqualityConditions()
	{
		return this.equalityConditions;
	}
}