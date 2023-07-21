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
package jpl.gds.db.mysql.impl.removal;

import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.mysql.impl.sql.AbstractMySqlInteractor;

/**
 * Abstract class for removing table rows from database.
 *
 */
public class AbstractRemover extends AbstractMySqlInteractor
{
	/**
	 * The statement used to build and execute fetch queries
	 */
	protected Statement statement;

	/**
     * Creates an instance of AbstractRemover.
     * 
     * @param appContext
     *            the Spring Application Context
     */
	public AbstractRemover(final ApplicationContext appContext)
	{
		super(appContext, false, true); // DB uses a Statement, not a PreparedStatement
		
		this.statement = null;
	}
	
	/**
	 * Releases the connection resources for the query. It is a good idea to call close after completely processing the query results.
	 * 
	 */
	@Override
	public void close()
	{
		if(this.statement != null)
		{
			try
			{
				this.statement.close();
			}
			catch(final SQLException ignore)
			{
				 // ignore this - nothing we can do 
			}
			this.statement = null;
		}
		super.close();
	}
}
