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
package jpl.gds.tm.service.api.frame;

import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;

/**
 * An interface to be implemented by factories that create messages related
 * to telemetry frame processing.
 * 
 *
 * @since R8
 */
public interface IFrameMessageFactory {

    /**
     * Creates a bad frame event message.
     * 
     * @param stationInfo the StationInfo object associated with this message
     * @param frameInfo the IFrameInfo object associated with this message
     * 
     * @return new message instance
     */
    IFrameEventMessage createBadFrameMessage(
            IStationTelemInfo stationInfo, ITelemetryFrameInfo frameInfo);

    /**
     * Creates an in synchronization (sync lock) frame event message.
     * 
     * @param stationInfo the StationInfo object associated with this message
     * @param frameInfo the IFrameInfo object associated with this message
     * 
     * @return new message instance
     */
    IFrameEventMessage createInSyncMessage(
            IStationTelemInfo stationInfo, ITelemetryFrameInfo frameInfo);

    /**
     * Creates a frame sequence anomaly event message.
     * 
     * @param stationInfo the StationInfo object associated with this message
     * @param frameInfo the IFrameInfo object associated with this message
     * @param eventType the LogMessageType for this anomaly message
     * @param expected the expected frame VCFC for this anomaly
     * @param actual the actual frame VCFC for this anomaly
     * 
     * @return new message instance
     */
    IFrameSequenceAnomalyMessage createFrameSequenceAnomalyMessage(
            IStationTelemInfo stationInfo, ITelemetryFrameInfo frameInfo,
            LogMessageType eventType, long expected, long actual);

    /**
     * Creates a loss of synchronization frame event message.
     * 
     * @param stationInfo the StationInfo object associated with this message
     * @param frameInfo the IFrameInfo object associated with this message
     * @param reason a text string containing the reason for loss of sync, if known
     * @param lastErt the ERT of the last in-sync frame
     * 
     * @return new message instance
     */
    ILossOfSyncMessage createLossOfSyncMessage(
            IStationTelemInfo stationInfo, ITelemetryFrameInfo frameInfo,
            String reason, IAccurateDateTime lastErt);

    /**
     * Creates an out of synchronization data frame event message.
     * 
     * @param stationInfo the StationInfo object associated with this message
     * @param dataLength the number of out-of-sync bytes
     * 
     * @return new message instance
     */
    IOutOfSyncDataMessage createOutOfSyncMessage(
            IStationTelemInfo stationInfo, long dataLength);

    /**
     * Creates an out of synchronization data frame event message.
     * 
     * @param stationInfo the StationInfo object associated with this message
     * @param outOfSyncData the out-of-sync bytes
     * 
     * @return new message instance
     */
    IOutOfSyncDataMessage createOutOfSyncMessage(IStationTelemInfo stationInfo,
            byte[] outOfSyncData); 
    
    /**
     * Creates an pre-sync frame event message containing raw data that has not
     * yet undergone frame synchronization.
     * 
     * @param stationInfo the StationInfo object associated with this message
     * @param buff the buffer containing incoming frame data
     * @param offset the offset in the supplied buffer of the raw data to be sent in the message
     * @param bitLen the length in bits of the raw data to be sent
     * @param ert the ERT of the received data 
     * 
     * @return new message instance
     */
    IPresyncFrameMessage createPresyncFrameMessage(
            IStationTelemInfo stationInfo, byte buff[], int offset, int bitLen,
            IAccurateDateTime ert);


    /**
     * Creates a telemetry frame message.
     * 
     * @param stationInfo the StationInfo object associated with this message
     * @param frameInfo the IFrameInfo object associated with this message
     * @param size the size in bytes of the entire frame, less station headers and trailers
     * @param buff the buffer containing the frame data
     * @param off the start offset of the frame data in the input buffer
     * @param hdr the holder for the station header; may be null
     * @param tr the holder for the station trailer; may be null
     * 
     * @return new message instance
     */
    ITelemetryFrameMessage createTelemetryFrameMessage(
            IStationTelemInfo stationInfo, ITelemetryFrameInfo frameInfo,
            int size, byte[] buff, int off, HeaderHolder hdr,
            TrailerHolder tr);


    /**
     * Creates a telemetry frame summary (statistics) message.
     * 
     * @param isInSync true if the current state of the frame synchronizer is "in sync"
     * @param numFrames number of frames identified so far
     * @param frameBytes total bytes of in-sync frame data processed so far
     * @param outSyncBytes total number of out-of-sync data bytes seen so far
     * @param outSyncCount total number of out-of-sync data events seen so far
     * @param numIdle number of idle frames seen so far
     * @param numDead number of dead frames seen so far
     * @param numBad of invalid frames seen so far
     * @param bitrate the last reported bit rate
     * 
     * @return new message instance
     */
    IFrameSummaryMessage createFrameSummaryMessage(boolean isInSync,
            long numFrames, long frameBytes, long outSyncBytes,
            long outSyncCount, long numIdle, long numDead, long numBad,
            double bitrate);
}