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
package jpl.gds.tm.service.impl.frame;

import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.IFrameMessageFactory;
import jpl.gds.tm.service.api.frame.IFrameSequenceAnomalyMessage;
import jpl.gds.tm.service.api.frame.IFrameSummaryMessage;
import jpl.gds.tm.service.api.frame.ILossOfSyncMessage;
import jpl.gds.tm.service.api.frame.IOutOfSyncDataMessage;
import jpl.gds.tm.service.api.frame.IPresyncFrameMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameMessage;

/**
 * A factory for the creation of telemetry-frame related messages.
 * 
 * @since R8
 */
public class FrameMessageFactory implements IFrameMessageFactory {

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameMessageFactory#createBadFrameMessage(jpl.gds.station.api.IStationTelemInfo, jpl.gds.tm.service.api.frame.ITelemetryFrameInfo)
     */
    @Override
    public IFrameEventMessage createBadFrameMessage(IStationTelemInfo stationInfo, ITelemetryFrameInfo frameInfo) {
            return new BadFrameMessage(stationInfo, frameInfo);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameMessageFactory#createInSyncMessage(jpl.gds.station.api.IStationTelemInfo, jpl.gds.tm.service.api.frame.ITelemetryFrameInfo)
     */
    @Override
    public IFrameEventMessage createInSyncMessage(IStationTelemInfo stationInfo, ITelemetryFrameInfo frameInfo) {
            return new InSyncMessage(stationInfo, frameInfo);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameMessageFactory#createFrameSequenceAnomalyMessage(jpl.gds.station.api.IStationTelemInfo, jpl.gds.tm.service.api.frame.ITelemetryFrameInfo, jpl.gds.shared.log.LogMessageType, long, long)
     */
    @Override
    public IFrameSequenceAnomalyMessage createFrameSequenceAnomalyMessage(IStationTelemInfo stationInfo,
            ITelemetryFrameInfo frameInfo, LogMessageType eventType, long expected, long actual) {
            return new FrameSequenceAnomalyMessage(stationInfo, frameInfo,
                    eventType, expected, actual); 
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameMessageFactory#createLossOfSyncMessage(jpl.gds.station.api.IStationTelemInfo, jpl.gds.tm.service.api.frame.ITelemetryFrameInfo, java.lang.String, jpl.gds.shared.time.IAccurateDateTime)
     */
    @Override
    public ILossOfSyncMessage createLossOfSyncMessage(IStationTelemInfo stationInfo, ITelemetryFrameInfo frameInfo, String reason, IAccurateDateTime lastErt) {
            return new LossOfSyncMessage(stationInfo, frameInfo, reason, lastErt);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameMessageFactory#createOutOfSyncMessage(jpl.gds.station.api.IStationTelemInfo, long)
     */
    @Override
    public IOutOfSyncDataMessage createOutOfSyncMessage(IStationTelemInfo stationInfo, long dataLength) {
            return new OutOfSyncDataMessage(stationInfo, dataLength, null);
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameMessageFactory#createOutOfSyncMessage(jpl.gds.station.api.IStationTelemInfo, byte[])
     */
    @Override
    public IOutOfSyncDataMessage createOutOfSyncMessage(IStationTelemInfo stationInfo, byte[] outOfSyncData) {
        if (outOfSyncData == null) {
            return createOutOfSyncMessage(stationInfo, 0);
        }
        return new OutOfSyncDataMessage(stationInfo, outOfSyncData.length, outOfSyncData);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameMessageFactory#createPresyncFrameMessage(jpl.gds.station.api.IStationTelemInfo, byte[], int, int, jpl.gds.shared.time.IAccurateDateTime)
     */
    @Override
    public IPresyncFrameMessage createPresyncFrameMessage(IStationTelemInfo stationInfo, byte buff[], int offset, int bitLen, IAccurateDateTime ert) {

            return new PresyncFrameMessage(stationInfo, buff, offset, bitLen, ert);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameMessageFactory#createTelemetryFrameMessage(jpl.gds.station.api.IStationTelemInfo, jpl.gds.tm.service.api.frame.ITelemetryFrameInfo, int, byte[], int, jpl.gds.shared.holders.HeaderHolder, jpl.gds.shared.holders.TrailerHolder)
     */
    @Override
    public ITelemetryFrameMessage createTelemetryFrameMessage(IStationTelemInfo stationInfo,
            final ITelemetryFrameInfo    frameInfo,
            final int           bodySize,
            final byte[]        buff,
            final int           off,
            final HeaderHolder  hdr,
            final TrailerHolder tr) {

            return new TelemetryFrameMessage(stationInfo, frameInfo, bodySize, buff, off, hdr, tr);   
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameMessageFactory#createFrameSummaryMessage(boolean, long, long, long, long, long, long, long, double)
     */
    @Override
    public IFrameSummaryMessage createFrameSummaryMessage(final boolean isInSync, final long _numFrames,
            final long _frameBytes, final long _outSyncBytes,
            final long _outSyncCount, final long _numIdle, final long _numDead,
            final long _numBad, double _bitrate) {
            return new FrameSummaryMessage(isInSync, _numFrames,
                    _frameBytes, _outSyncBytes, _outSyncCount, _numIdle, _numDead, _numBad, _bitrate);
    }

}
