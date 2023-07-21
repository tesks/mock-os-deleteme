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
package jpl.gds.common.config.connection;

import jpl.gds.common.config.types.DatabaseConnectionKey;

/**
 * An interface to be implemented by connection configurations that use
 * the database as source or sink.
 * 
 * @since R8
 */
public interface IDatabaseConnectionSupport {
	/**
	 * Sets the database connection key for this connection.
	 * 
	 * @param key the key to set; may not be null
	 */
	public void setDatabaseConnectionKey(DatabaseConnectionKey key);

	/**
	 * Gets the database connection key for this connection.
	 * 
	 * @return database connection key, never null
	 */
	public DatabaseConnectionKey getDatabaseConnectionKey();
     
}
