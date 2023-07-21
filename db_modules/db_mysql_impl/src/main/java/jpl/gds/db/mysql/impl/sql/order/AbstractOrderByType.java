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

package jpl.gds.db.mysql.impl.sql.order;

import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.shared.types.EnumeratedType;

/**
 * This class is the parent class of all the classes used to determine
 * the ordering for database query results.
 *
 */
public abstract class AbstractOrderByType extends EnumeratedType implements IDbOrderByType
{
	/**
	 * 
	 * Creates an instance of AbstractOrderByType.
	 * 
	 * @param strVal The initial value
	 */
	protected AbstractOrderByType(final String strVal)
	{
		super(strVal);
	}
	
	/**
	 * 
	 * Creates an instance of AbstractOrderByType.
	 * 
	 * @param intVal The initial value
	 */
	protected AbstractOrderByType(final int intVal)
	{
		super(intVal);
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.db.mysql.impl.sql.order.IDbOrderByType#getOrderByClause()
	 */
	@Override
    public abstract String getOrderByClause();

	/* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.order.IDbOrderByType#getOrderByColumns()
     */
    @Override
    public abstract String[] getOrderByColumns();
}
