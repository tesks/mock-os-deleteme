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

import java.util.Date;
import java.util.List;

import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

public interface IDbPacketFactory
        extends IDbQueryableFactory<IDbPacketProvider, IDbPacketUpdater> {
    /**
     * @return a new IDbPacketProvider object
     */
    @Override
    public IDbPacketProvider createQueryableProvider();

    /**
     * Constructor.
     * 
     * @param id
     *            Id
     * @param sessionId
     *            Session id
     * @param sessionHost
     *            Session host
     * @param rct
     *            RCT
     * @param scet
     *            SCET
     * @param ert
     *            ERT
     * @param sclk
     *            SCLK
     * @param sol
     *            LST
     * @param apid
     *            APID
     * @param apidName
     *            APID name
     * @param dssId
     *            DSS id
     * @param vcid
     *            VCID
     * @param spsc
     *            SPSC
     * @param fromSse
     *            From-SSE state
     * @param body
     *            Body bytes
     * @param vcfcs
     *            VCFCS
     * @param fileByteOffset
     *            File byte offset
     * @param frameId
     *            Frame id
     * @param fillFlag
     *            Fill flag
     * @return a new IDbPacketProvider object
     */
    IDbPacketUpdater createQueryableUpdater(PacketIdHolder id, Long sessionId, String sessionHost, Date rct,
                                            IAccurateDateTime scet, IAccurateDateTime ert, ISclk sclk,
                                            ILocalSolarTime sol, Integer apid, String apidName, int dssId, Integer vcid,
                                            Integer spsc, Boolean fromSse, byte[] body, List<Long> vcfcs,
                                            Long fileByteOffset, Long frameId, boolean fillFlag);

    /**
     * Constructor.
     * 
     * @param id
     *            Id
     * @param sessionId
     *            Session id
     * @param sessionHost
     *            Session host
     * @param rct
     *            RCT
     * @param scet
     *            SCET
     * @param ert
     *            ERT
     * @param sclk
     *            SCLK
     * @param sol
     *            LST
     * @param apid
     *            APID
     * @param apidName
     *            APID name
     * @param dssId
     *            DSS id
     * @param vcid
     *            VCID
     * @param spsc
     *            SPSC
     * @param fromSse
     *            From-SSE state
     * @param body
     *            Body bytes
     * @param vcfcs
     *            VCFCS
     * @param fileByteOffset
     *            File byte offset
     * @param frameId
     *            Frame id
     * @param fillFlag
     *            Fill flag
     * @return a new IDbPacketUpdater object
     */
    IDbPacketProvider createQueryableProvider(PacketIdHolder id, Long sessionId, String sessionHost,
                                              IAccurateDateTime rct, IAccurateDateTime scet, IAccurateDateTime ert,
                                              ISclk sclk, ILocalSolarTime sol, Integer apid, String apidName, int dssId,
                                              Integer vcid, Integer spsc, Boolean fromSse, byte[] body,
                                              List<Long> vcfcs, Long fileByteOffset, Long frameId, boolean fillFlag);
}