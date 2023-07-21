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
package jpl.gds.db.impl.types;

import java.util.Date;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.types.IDbPacketFactory;
import jpl.gds.db.api.types.IDbPacketProvider;
import jpl.gds.db.api.types.IDbPacketUpdater;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;


public class DbPacketFactory extends AbstractDbQueryableFactory<IDbPacketProvider, IDbPacketUpdater>
        implements IDbPacketFactory {
    /**
     * Private Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public DbPacketFactory(final ApplicationContext appContext) {
        super(appContext,
              IDbPacketUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbPacketProvider createQueryableProvider() {
        return new DatabasePacket(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbPacketProvider createQueryableProvider(final PacketIdHolder id, final Long sessionId,
                                                     final String sessionHost, final IAccurateDateTime rct,
                                                     final IAccurateDateTime scet, final IAccurateDateTime ert,
                                                     final ISclk sclk, final ILocalSolarTime sol, final Integer apid,
                                                     final String apidName, final int dssId, final Integer vcid,
                                                     final Integer spsc, final Boolean fromSse, final byte[] body,
                                                     final List<Long> vcfcs, final Long fileByteOffset,
                                                     final Long frameId, final boolean fillFlag) {
        return new DatabasePacket(appContext, id, sessionId, sessionHost, rct, scet, ert, sclk, sol, apid, apidName,
                                  dssId, vcid, spsc, fromSse, body, vcfcs, fileByteOffset, frameId, fillFlag);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbPacketUpdater createQueryableUpdater() {
        return new DatabasePacket(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbPacketUpdater createQueryableUpdater(final PacketIdHolder id, final Long sessionId,
                                                   final String sessionHost, final Date rct,
                                                   final IAccurateDateTime scet, final IAccurateDateTime ert,
                                                   final ISclk sclk, final ILocalSolarTime sol, final Integer apid,
                                                   final String apidName, final int dssId, final Integer vcid,
                                                   final Integer spsc, final Boolean fromSse, final byte[] body,
                                                   final List<Long> vcfcs, final Long fileByteOffset,
                                                   final Long frameId, final boolean fillFlag) {
        return this.convertProviderToUpdater(createQueryableProvider(id, sessionId, sessionHost, rct, scet, ert, sclk, sol, apid, apidName,
                                                                     dssId, vcid, spsc, fromSse, body, vcfcs,
                                                                     fileByteOffset, frameId, fillFlag));
    }
}
