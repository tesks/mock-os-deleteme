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
package jpl.gds.telem.process;

import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.telem.common.ITelemetryWorker;

public interface IProcessWorker extends ITelemetryWorker {

    /**
     * Gets the ProcessConfiguration object reflecting the current settings, including the test configuration.
     *
     * @return a ProcessConfiguration
     */
    ProcessConfiguration getProcessConfiguration();

    /**
     * Retrieves the current TimeComparisonStrategy
     *
     * @return the current TimeComparisonStrategy
     */
    TimeComparisonStrategy getTimeComparisonStrategy();

    /**
     * Sets the TimeComparisonStrategy
     * @param strategy to set
     */
    void setTimeComparisonStrategy(TimeComparisonStrategy strategy);

    /**
     * Gets the SuspectChannelTable object from the downlink context manager object
     *
     * @return SuspectChannelTable, or null if none has been initialized
     */
    ISuspectChannelService getSuspectChannelService();

    /**
     * Get the current contents of the downlink-local LAD in a VERY LARGE string
     *
     * @return a string containing the contents of the lad as CSV
     */
    String getLadContentsAsString();

    /**
     * Save the current contents of the downlink-local LAD to a file.
     *
     * @param filename the file path
     * @return true if successful, false if not
     */
    boolean saveLadToFile(String filename);

    /**
     * Clears the downlink-local channel state (LAD, alarm history)
     */
    void clearChannelState();

    /**
     * Gets the processing state of the Telemetry Process worker
     *
     * @return IProcessWorkerStatus
     */
    IProcessWorkerStatus getStatus();
}