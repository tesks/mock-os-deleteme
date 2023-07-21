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
package jpl.gds.telem.common.manager;

import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.telem.input.api.RawInputException;

/**
 * Interface for basic functions of a "Session Manager" to implement
 *
 */
public interface ISessionManager {
    /**
     * Starts up the database components and connections
     *
     * @throws DatabaseException could not start database
     *   Throw database exception to capture that error code.
     *   Findbugs fix for IDbSqlArchiveController synchronization.
     */
    void startDatabase() throws DatabaseException;


    /**
     * Sets up everything needed to run the session. Initializes the feature
     * managers, output directory, statistics meters, debugging objects, and Raw
     * Input adapter.
     *
     * @return true if initialization was successful
     * @throws ApplicationException when an error occurs initializing the session
     */
    boolean initSession() throws ApplicationException;

    /**
     * Starts the database stores for the session. This creates the session in
     * the database.
     *
     * @throws DatabaseException could not initialize database
     *  Throw database exception to capture that specific error code.
     */
    void startSessionDatabase() throws DatabaseException;


    /**
     * Starts a session, this will call also call the implementation of initSession().
     * This method also updates the session configuration file on disk.
     * This call does not start the database, which  must be accomplished by calling
     * startDatabase() prior to invoking this method.
     *
     * @throws ApplicationException When unable to start a session
     * Throw exception to capture error code later on.
     */
    void startSession() throws ApplicationException;


    /**
     * Ends a session. This means displaying the session summary (command line
     * mode only), cleaning up after the downlink components, flushing out the
     * message service queue, and stopping the performance summary publisher.
     *
     * This method will automatically call stop(), stopPerformancePublisher()
     *
     * @param showSummary true if the session summary should be displayed on the
     *                    console, false if not
     * @return true if all session shutdown goes normally
     */
    boolean endSession(final boolean showSummary);


    /**
     * Indicates whether the session has already ended.
     *
     * @return true if the session has ended
     */
    boolean isSessionEnded();

    /**
     * Processes telemetry until told to stop or the data runs out.
     *
     * @return true if input processing is successfully started and terminates
     * without error, false if there is an exception processing
     * telemetry
     * @throws RawInputException When a processing error occurs
     */
    boolean processInput() throws RawInputException;


    /**
     * Stops processing of telemetry input. The input stream is no longer read.
     *
     * Throws IllegalStateException if rawInputHandler is null
     */
    void stop();

    /**
     * Populates an ITelemetrySummary object with data gathered during the
     * telemetry processing such as start time, end time, and context information
     *
     * @param startTime the start time of the session
     * @param endTime   the end time of the session
     *
     * @return ITelemetrySummary object
     */
    ITelemetrySummary populateSummary(final IAccurateDateTime startTime, final IAccurateDateTime endTime);

    /**
     * Gets an ITelemetrySummary including statistics from every feature manager
     *
     * @return ITelemetrySummary session summary
     */
    ITelemetrySummary getProgressSummary();

    /**
     * Update context database with the list of associated session IDs
     *
     * @param sessionIds List of updated session IDs (separated by colon)
     *
     * @throws ApplicationException If exceptions saving to database
     */
    void updateContextDbSessionsList(String sessionIds) throws ApplicationException;


    /**
     * Stops the performance summary publisher.
     *
     */
    void stopPerformancePublisher();
}
