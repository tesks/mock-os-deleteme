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
import jpl.gds.station.api.dsn.chdo.IChdoProperty;

/**
 * Defines a computed property of a Compressed Header Data Object (CHDO).
 * It is essentially a named set of conditions that can be evaluated against
 * a CHDO.
 * 
 */
class ChdoProperty implements IChdoProperty
{
	private final String name;
	private final List<IChdoCondition> chdoConditions; 
	
	/**
	 * Basic constructor.
	 * 
	 * @param name name for the property
	 */
	public ChdoProperty(final String name)
	{
		this.name = name;
		this.chdoConditions = new ArrayList<IChdoCondition>(5);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void addCondition(final IChdoCondition condition)
	{
		if(this.chdoConditions.contains(condition) == false)
		{
			this.chdoConditions.add(condition);
		}
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoProperty#getChdoConditions()
     */
	@Override
    public List<IChdoCondition> getChdoConditions()
	{
		return this.chdoConditions;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.station.api.dsn.chdo.IChdoProperty#getName()
     */
	@Override
    public String getName()
	{
		return this.name;
	}
}
