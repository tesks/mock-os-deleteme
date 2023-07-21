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

import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.InvalidFrameCode;

public interface IDbFrameFactory extends IDbQueryableFactory<IDbFrameProvider, IDbFrameUpdater> {
    /**
     * Constructor.
     * 
     * @param id
     *            Id
     * @param type
     *            Type
     * @param ert
     *            ERT
     * @param relaySpacecraftId
     *            Relay S/C id
     * @param vcid
     *            VCID
     * @param vcfc
     *            VCFC
     * @param dssId
     *            DSS id
     * @param bitRate
     *            Bit rate
     * @param body
     *            Body bytes
     * @param badReason
     *            Bad reason
     * @param testSessionId
     *            Session id
     * @param sessionHost
     *            Session host
     * @param fileByteOffset
     *            File byte offset
     * @param fillFrame
     *            True if fill frame
     * @param sleMetadata
     *            SLE header metadata
     * @return an instance of an IDbFrameProvider
     */
    IDbFrameProvider createQueryableProvider(Long id, String type, IAccurateDateTime ert,
                                             Integer relaySpacecraftId, Integer vcid, Integer vcfc, Integer dssId,
                                             Double bitRate, byte[] body, InvalidFrameCode badReason,
                                             Long testSessionId, String sessionHost, Long fileByteOffset,
                                             boolean fillFrame, String sleMetadata);

    /**
     * Constructor.
     * 
     * @param id
     *            Id
     * @param type
     *            Type
     * @param ert
     *            ERT
     * @param relaySpacecraftId
     *            Relay S/C id
     * @param vcid
     *            VCID
     * @param vcfc
     *            VCFC
     * @param dssId
     *            DSS id
     * @param bitRate
     *            Bit rate
     * @param body
     *            Body bytes
     * @param badReason
     *            Bad reason
     * @param testSessionId
     *            Session id
     * @param sessionHost
     *            Session host
     * @param fileByteOffset
     *            File byte offset
     * @param fillFrame
     *            True if fill frame
     * @param sleMetadata
     *            SLE header metadata
     * @return an instance of an IDbFrameUpdater
     */
    IDbFrameUpdater createQueryableUpdater(Long id, String type, IAccurateDateTime ert,
                                           Integer relaySpacecraftId, Integer vcid, Integer vcfc, Integer dssId,
                                           Double bitRate, byte[] body, InvalidFrameCode badReason, Long testSessionId,
                                           String sessionHost, Long fileByteOffset, boolean fillFrame, String sleMetadata);
}