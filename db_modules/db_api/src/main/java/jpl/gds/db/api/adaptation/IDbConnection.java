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
 * We do not expect to be called by multiple threads.
 */
public interface IDbConnection
{
    /**
     * Get adapter name.
     *
     * @return Adapter name
     */
    public String getAdapterName();

    /**
     * Not called by us! A trap to detect unexpected closes.
     *
     * @throws DatabaseException Database error
     */
    public void close() throws DatabaseException;


    /**
     * Called by us to close the connection and statement.
     */
    public void closeAtEnd();
}
