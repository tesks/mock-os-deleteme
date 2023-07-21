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
package jpl.gds.tc.impl.icmd;

import java.util.List;

import gov.nasa.jpl.icmd.schema.UplinkRequest;
import jpl.gds.common.config.mission.StationMapper;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.IUplinkMetadata;
import jpl.gds.tc.api.icmd.ICpdObjectFactory;

public class CpdObjectFactory implements ICpdObjectFactory {

    @Override
    public ICpdUplinkStatus createCpdUplinkStatus(final StationMapper stationMap) {
        return new CpdUplinkStatus(stationMap);
    }

    @Override
    public ICpdUplinkStatus createCpdUplinkStatus(final StationMapper stationMap, final UplinkRequest ur) {
        return new CpdUplinkStatus(stationMap, ur);
    }

    @Override
    public ICpdUplinkStatus createCpdUplinkStatus(final StationMapper stationMap, final ICpdUplinkStatus cus) {
        return new CpdUplinkStatus(stationMap, cus);
    }

    @Override
    public ICpdUplinkStatus createCpdUplinkStatus(final StationMapper stationMap, final String id, final CommandStatusType status,
                                                  final IAccurateDateTime timestamp, final String filename,
                                                  final List<Float> bitrates, final List<Float> estRadDurations,
                                                  final String userId,
                                                  final String roleId, final String submitTimeStr,
                                                  final String includedInExeListStr, final String uplinkMetadataString,
                                                  final String checksum, final int totalCltus,
                                                  final IAccurateDateTime bit1RadTime,
                                                  final IAccurateDateTime lastBitRadTime) {

        return new CpdUplinkStatus(stationMap, id, status, timestamp, filename, bitrates, 
                estRadDurations, userId, roleId, submitTimeStr, includedInExeListStr, uplinkMetadataString, checksum, totalCltus, 
                bit1RadTime, lastBitRadTime);
    }

    @Override
    public IUplinkMetadata createUplinkMetadata(final long sessionId, final int hostId, final String jmsTopicName, final int scid) {
        return new UplinkMetadata(sessionId, hostId, jmsTopicName, scid);
    }

    @Override
    public IUplinkMetadata createUplinkMetadata(final String metadataString) {
        return new UplinkMetadata(metadataString);
    }

}
