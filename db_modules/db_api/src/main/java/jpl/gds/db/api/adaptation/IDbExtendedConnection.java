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
package jpl.gds.db.api.adaptation;

import jpl.gds.db.api.DatabaseException;


/**
 * This interface defines classes that wrap a JDBC Connection and a Statement
 * so we can monitor things. We do NOT use PreparedStatement.
 *
 * It does recreate things if they time-out or drop mysteriously. A simpler
 * set of classes are under IMySqlDbBasicConnection.
 *
 * We do not expect to be called by multiple threads.
 *
 */
public interface IDbExtendedConnection extends IDbConnection
{
    /**
     * Executes the given SQL using the current statement, re-attempting upon
     * failure if necessary. 
     * 
     * @param query Query string to execute
     *
     * @return boolean True if the statement execution succeeded
     *
     * @throws DatabaseException Database error
     */
    public boolean execute(final String query) throws DatabaseException;
}
