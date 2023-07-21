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

package jpl.gds.telem.ingest;

import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.ingest.state.IIngestWorkerStatus;

/**
 * An interface to be implemented by the telemetry ingest worker.
 *
 * @since R8
 */
public interface IIngestWorker extends ITelemetryWorker {

    /**
     * Gets the IngestConfiguration object reflecting the current settings,
     * including the test configuration.
     *
     * @return a IngestConfiguration
     */
    IngestConfiguration getIngestConfiguration();

    /**
     * Gets the processing state/status of the Telemetry Ingest worker
     * which should include input source connection, telemetry flow and
     * frame sync status.
     *
     * @return IIngestWorkerStatus
     */
    IIngestWorkerStatus getStatus();
}