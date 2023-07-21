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

import jpl.gds.telem.common.config.IDownlinkProperties;

/**
 * Interface for properties unique to the telemetry processor
 */
public interface IProcessProperties extends IDownlinkProperties {

    /**
     * Gets the enable EHA aggregation flag, which controls the grouping of
     * multiple channels into aggregated blobs.
     *
     * @return true if aggregated EHA is enabled, false if disabled
     */
    boolean isEnableAggregatedEha();


    /**
     * Gets the enable generic channel decom flag, which controls the channelization
     * of input telemetry using a decom map.
     *
     * @return true if generic decom is enabled, false if disabled
     */
    boolean isEnableGenericChannelDecom();

    /**
     * Gets the enable generic EVR decom flag, which controls the EVR extraction from
     * input telemetry using a decom map.
     *
     * @return true if generic decom is enabled, false if disabled
     */
    boolean isEnableGenericEvrDecom();


    /**
     * Gets the enable generic decom flag, which controls the channelization
     * of input telemetry using a decom map.
     *
     * @return true if generic decom is enabled, false if disabled
     */
    boolean isEnableAlarms();


    /**
     * Gets the enable EHA decom flag, which controls the decom of pre-channelized
     * packets during processing.
     *
     * @return true if EHA decom enabled, false if not
     */
    boolean isEnablePreChannelizedDecom();


    /**
     * Gets the enable EVR decom flag, which controls the decom of EVR packets
     * during processing.
     *
     * @return true if EVR decom is enabled, false if not
     */
    boolean isEnableEvrDecom();

    /**
     * Gets the enable packet header channelization flag, which controls channelization
     * of value in the packet headers.
     *
     * @return true if packet header channelization enabled, false if disabled
     */
    boolean isEnablePacketHeaderChannelizer();


    /**
     * Gets the enable frame header channelization flag, which controls channelization
     * of value in the frame headers.
     *
     * @return true if frame header channelization enabled, false if disabled
     */
    boolean isEnableFrameHeaderChannelizer();


    /**
     * Gets the enable frame header channelization flag, which controls channelization
     * of value in the frame headers.
     *
     * @return true if frame header channelization enabled, false if disabled
     */
    boolean isEnableSfduHeaderChannelizer();

    /**
     * Indicates if any header channel capability is enabled.
     *
     * @return true if ANY header channels enabled, false if not
     */
    boolean isEnableAnyHeaderChannelizer();


    /**
     * Gets the enable product generation flag, which controls the generation
     * of data products during processing.
     *
     * @return true if data product generation is enabled, false if not
     */
    boolean isEnableProductGen();


    /**
     * Gets the enable PDU extraction flag, which controls the extraction
     * of PDUs from applicable data during processing.
     *
     * @return true if PDU extraction is enabled, false if not
     */
    boolean isEnablePduExtract();

    /**
     * Gets the size of the handler queue used to buffer incoming telemetry messages
     * @return Queue size
     */
    int getMessageHandlerQueueSize();

    /**
     * Gets the wait timeout after which a TP worker can be stopped if no telemetry
     * @return Telemetry wait timeout in seconds
     */
    int getTelemetryWaitTimeout();

    /**
     * Gets the size of the spill processor queue used to buffer incoming telemetry messages
     * @return Queue size
     */
    int getSpillProcessorQueueSize();

    /**
     * Gets the wait timeout for a message in the in-memory queue before it attempts to read buffered messages from disk
     * @return Telemetry wait timeout in seconds
     */
    long getSpillWaitTimeout();
}
