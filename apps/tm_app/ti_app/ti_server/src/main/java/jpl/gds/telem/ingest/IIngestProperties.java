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

import jpl.gds.telem.common.config.IDownlinkProperties;

/**
 * Interface for properties unique to the telemetry ingestor
 */
public interface IIngestProperties extends IDownlinkProperties {


    /**
     * Gets the enable frame sync flag, which controls the synchronization of
     * raw frame data during processing.
     *
     * @return true if frame sync enabled, false if not
     */
    boolean isEnableFrameSync();


    /**
     * Gets the enable packet extract flag, which controls packet extraction from
     * frames during processing.
     *
     * @return true if packet extraction is enabled, false if disabled
     */
    boolean isEnablePacketExtract();

    /**
     * Gets the enable time correlation flag, which controls the correlation of time packets
     * to reference frames and the generation of time correlation messages.
     *
     * @return true if time correlation is enabled, false if not
     */
    boolean isEnableTimeCorr();
}
