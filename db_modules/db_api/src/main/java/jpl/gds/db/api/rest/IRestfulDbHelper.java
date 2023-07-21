/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.db.api.rest;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.types.IDbLogProvider;
import jpl.gds.db.api.types.IDbSessionProvider;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.StringResponse;

import java.util.List;

/**
 * Restful interface to DB logging
 *
 * Currently supports:
 *  Getting SESSION logs
 *  Getting CONTEXT logs
 *  Inserting logs
 *  Getting a list of SESSIONS from the database
 *  Getting specific SESSION entry from the database
 */
public interface IRestfulDbHelper {

    /**
     * "Inserts" A log into the database by creating an IPublishableLogMessage from
     * the supplied params and logging it, log4j takes care of the rest.
     * DEBUG and TRACE log levels will be ignored unless it has been enabled for
     * the logger in log4j2.xml
     *
     * @param level the level to log as
     * @param message the message to log
     * @param log the Tracer to log with
     *
     * @return StringResponse if the message has been successfully queued to the logger
     * @throws Exception If an error occurs
     */
    StringResponse logMessage(final String level, final String message, final Tracer log) throws Exception;

    /**
     * Gets logs for a specified session
     *
     * @param sessionKey the session key
     * @param sessionHost the session host
     *
     * @return log records associated with the session
     * @throws DatabaseException If an error occurs retrieving the logs
     */
    List<IDbLogProvider> getLogsFromSession(final Long sessionKey, final String sessionHost) throws
            DatabaseException;

    /**
     * Gets logs for a specified session
     *
     * @param sessionKey the session key
     * @param sessionHost the session host
     * @param limit the maximum number of most recent log lines to return. Only applies when greater than 0
     *
     * @return log records associated with the session
     * @throws DatabaseException If an error occurs retrieving the logs
     */
    List<IDbLogProvider> getLogsFromSession(final Long sessionKey, final String sessionHost, int limit) throws
                                                                                             DatabaseException;


    /**
     * Gets logs for a specified context
     *
     * @param contextKey the session key
     * @param contextHost the session host
     *
     * @return log records associated with the context
     * @throws DatabaseException If an error occurs retrieving the logs
     */
    List<IDbLogProvider> getLogsFromContext(final Long contextKey, final String contextHost) throws DatabaseException;

    /**
     * Gets logs for a specified context
     *
     * @param contextKey the session key
     * @param contextHost the session host
     * @param limit the maximum number of most recent log lines to return. Only applies when greater than 0
     *
     * @return log records associated with the context
     * @throws DatabaseException If an error occurs retrieving the logs
     */
    List<IDbLogProvider> getLogsFromContext(final Long contextKey, final String contextHost, int limit) throws DatabaseException;


    /**
     * Retrieves a session from the database, if it exists
     * @param key the session key to look up
     * @param host the session host to look up
     *
     * @return SessionConfiguration matching the supplied key and host, if it exists
     *
     * @throws DatabaseException When an error occurs retrieving the session
     */
    SessionConfiguration getSessionConfigurationFromDatabase(final Long key, final String host) throws
            DatabaseException;

    /**
     * Retrieves a list of sessions from the database
     * @param key session key to look up
     * @param host session host to look up
     *
     * @return a list of sessions which can be empty
     * @throws DatabaseException When an error occurs retrieving the session
     */
    List<IDbSessionProvider> getSessions(final Long key, final String host) throws DatabaseException;

}
