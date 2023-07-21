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
package jpl.gds.db.api.types;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;


public interface IDbChannelSampleFactory
        extends IDbQueryableFactory<IDbChannelSampleProvider, IDbChannelSampleUpdater> {

    /**
     * Creates an instance of IDbChannelSampleProvider.
     * 
     * @return an IDbChannelSampleProvider object instantiated with no data
     */
    @Override
    IDbChannelSampleProvider createQueryableProvider();

    /**
     * Creates an instance of IDbChannelSampleProvider.
     * 
     * @param sessionId
     *            The initial test session ID
     * @param fromSse
     *            True if this value is from the SSE, false otherwise
     * @param isRealtime
     *            True if this value is real-time, false otherwise
     * @param sclk
     *            The initial SCLK
     * @param ert
     *            The initial ERT
     * @param scet
     *            The initial SCET
     * @param sol
     *            the initial LST
     * @param v
     *            The initial value
     * @param ct
     *            The initial channel type
     * @param cid
     *            The initial channel ID
     * @param cindex
     *            The initial channel index
     * @param module
     *            the initial FSW module for the channel
     * @param sessionHost
     *            the initial session host
     * @param eu
     *            the initial EU value
     * @param dnAlarm
     *            the initial DN alarm value
     * @param euAlarm
     *            the initial EU alarm value
     * @param status
     *            the initial status
     * @param scid
     *            the initial S/C id
     * @param name
     *            the initial name
     * @param dssId
     *            the initial DSS id
     * @param vcid
     *            the initial VCID
     * @param rct
     *            RCT
     * @param packetId
     *            The packet id
     * @param frameId
     *            The frame id
     * @return an IDbChannelSampleProvider object instantiated with no data
     */
    IDbChannelSampleUpdater createQueryableUpdater(Long sessionId, Boolean fromSse, Boolean isRealtime, ISclk sclk,
                                                   IAccurateDateTime ert, IAccurateDateTime scet, ILocalSolarTime sol,
                                                   Object v, ChannelType ct, String cid, Long cindex, String module,
                                                   String sessionHost, Double eu, String dnAlarm, String euAlarm,
                                                   String status, Integer scid, String name, Integer dssId,
                                                   Integer vcid, IAccurateDateTime rct, PacketIdHolder packetId,
                                                   Long frameId);

    /**
     * Creates an instance of IDbChannelSampleUpdater.
     * 
     * @return an IDbChannelSampleUpdater object instantiated with no data
     */
    @Override
    IDbChannelSampleUpdater createQueryableUpdater();

    /**
     * Creates an instance of IDbChannelSampleUpdater.
     * 
     * @param sessionId
     *            The initial test session ID
     * @param fromSse
     *            True if this value is from the SSE, false otherwise
     * @param isRealtime
     *            True if this value is real-time, false otherwise
     * @param sclk
     *            The initial SCLK
     * @param ert
     *            The initial ERT
     * @param scet
     *            The initial SCET
     * @param sol
     *            the initial LST
     * @param v
     *            The initial value
     * @param ct
     *            The initial channel type
     * @param cid
     *            The initial channel ID
     * @param cindex
     *            The initial channel index
     * @param module
     *            the initial FSW module for the channel
     * @param sessionHost
     *            the initial session host
     * @param eu
     *            the initial EU value
     * @param dnAlarm
     *            the initial DN alarm value
     * @param euAlarm
     *            the initial EU alarm value
     * @param status
     *            the initial status
     * @param scid
     *            the initial S/C id
     * @param name
     *            the initial name
     * @param dssId
     *            the initial DSS id
     * @param vcid
     *            the initial VCID
     * @param rct
     *            RCT
     * @param packetId
     *            The packet id
     * @param frameId
     *            The frame id
     * @return an IDbChannelSampleUpdater object instantiated with no data
     */
    IDbChannelSampleProvider createQueryableProvider(Long sessionId, Boolean fromSse, Boolean isRealtime, ISclk sclk,
                                                     IAccurateDateTime ert, IAccurateDateTime scet, ILocalSolarTime sol,
                                                     Object v, ChannelType ct, String cid, Long cindex, String module,
                                                     String sessionHost, Double eu, String dnAlarm, String euAlarm,
                                                     String status, Integer scid, String name, Integer dssId,
                                                     Integer vcid, IAccurateDateTime rct, PacketIdHolder packetId,
                                                     Long frameId);

    IDbChannelSampleUpdater createQueryableAggregateProvider(Long sessionId, Boolean fromSse,
            Boolean isRealtime, ISclk sclk,
            IAccurateDateTime ert, IAccurateDateTime scet,
            ILocalSolarTime sol, Object v,
            ChannelType ct, String cid, Long cindex,
            String module, String sessionHost,
            Double eu, String dnAlarm, String euAlarm,
            String status, Integer scid, String name,
            Integer dssId, Integer vcid,
            IAccurateDateTime rct, PacketIdHolder packetId,
            Long frameId);
}