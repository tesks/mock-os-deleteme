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
package jpl.gds.db.api;

public interface IDbInteractor {
    /** Common database table field for sessionId */
    String SESSION_ID = "sessionId";

    /** Common database table field for contextId */
    String CONTEXT_ID = "contextId";

    /** Common database table field for sessionFragment */
    String FRAGMENT_ID = "sessionFragment";

    /** Common database table field for hostId */
    String HOST_ID = "hostId";

    /** Common database table field for contexthostId */
    String CONTEXT_HOST_ID = "contextHostId";

    /**
     * Close all the connection resources for this database class
     */
    void close();

    /**
     * Indicates whether a database connection is present. This doesn't prove
     * that the connection/statement is actually functional.
     *
     * @return true if connected to the database; false otherwise
     */
    default boolean isConnected() {
        return false;
    }

    /**
     * Start this store running
     */
    default void start() {
        // do nothing
    }
    
   /**
    * Stop this store from running
    */
    default void stop() {
        // do nothing
    }
}