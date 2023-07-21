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
package jpl.gds.db.api.sql.order;

public interface IDbOrderByType {
	/**
	 * The SQL "ORDER BY" command
	 */
	public String ORDER_BY_PREFIX = "ORDER BY ";

	/**
	 * Return an SQL "order by" clause based on the value of this enumerated
	 * type in ascending order (default)
	 * 
	 * @return A piece of SQL that represents a valid ORDER BY clause that can
	 *         be appended to the end of a query for the database table
	 *         associated with this object.
	 */
	public String getOrderByClause();

	/**
	 * Return an SQL "order by" clause based on the value of this enumerated
	 * type.
	 * 
	 * @return Array of column names
	 */
	public String[] getOrderByColumns();
}