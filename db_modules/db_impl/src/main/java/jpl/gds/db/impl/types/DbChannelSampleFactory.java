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

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.types.IDbChannelSampleFactory;
import jpl.gds.db.api.types.IDbChannelSampleProvider;
import jpl.gds.db.api.types.IDbChannelSampleUpdater;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;


public class DbChannelSampleFactory
        extends AbstractDbQueryableFactory<IDbChannelSampleProvider, IDbChannelSampleUpdater>
        implements IDbChannelSampleFactory {
    /**
     * Private Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public DbChannelSampleFactory(final ApplicationContext appContext) {
        super(appContext,
              IDbChannelSampleUpdater.class);
    }

    @Override
    public IDbChannelSampleProvider createQueryableProvider() {
        return new DatabaseChannelSample(appContext);
    }

    @Override
    public IDbChannelSampleProvider createQueryableProvider(final Long sessionId, final Boolean fromSse,
                                                            final Boolean isRealtime, final ISclk sclk,
                                                            final IAccurateDateTime ert, final IAccurateDateTime scet,
                                                            final ILocalSolarTime sol, final Object v,
                                                            final ChannelType ct, final String cid, final Long cindex,
                                                            final String module, final String sessionHost,
                                                            final Double eu, final String dnAlarm, final String euAlarm,
                                                            final String status, final Integer scid, final String name,
                                                            final Integer dssId, final Integer vcid,
                                                            final IAccurateDateTime rct, final PacketIdHolder packetId,
                                                            final Long frameId) {
        return new DatabaseChannelSample(appContext, sessionId, fromSse, isRealtime, sclk, ert, scet, sol, v, ct, cid,
                                         cindex, module, sessionHost, eu, dnAlarm, euAlarm, status, scid, name, dssId,
                                         vcid, rct, packetId, frameId);
    }

    @Override
    public IDbChannelSampleUpdater createQueryableUpdater(final Long sessionId, final Boolean fromSse,
                                                          final Boolean isRealtime, final ISclk sclk,
                                                          final IAccurateDateTime ert, final IAccurateDateTime scet,
                                                          final ILocalSolarTime sol, final Object v,
                                                          final ChannelType ct, final String cid, final Long cindex,
                                                          final String module, final String sessionHost,
                                                          final Double eu, final String dnAlarm, final String euAlarm,
                                                          final String status, final Integer scid, final String name,
                                                          final Integer dssId, final Integer vcid,
                                                          final IAccurateDateTime rct, final PacketIdHolder packetId,
                                                          final Long frameId) {
        return convertProviderToUpdater(createQueryableProvider(sessionId, fromSse, isRealtime, sclk, ert, scet, sol, v,
                                                                ct, cid, cindex, module, sessionHost, eu, dnAlarm,
                                                                euAlarm, status, scid, name, dssId, vcid, rct, packetId,
                                                                frameId));
    }

	@Override
    public IDbChannelSampleUpdater createQueryableAggregateProvider(final Long sessionId, final Boolean fromSse,
            final Boolean isRealtime, final ISclk sclk,
            final IAccurateDateTime ert, final IAccurateDateTime scet,
            final ILocalSolarTime sol, final Object v,
            final ChannelType ct, final String cid, final Long cindex,
            final String module, final String sessionHost,
            final Double eu, final String dnAlarm, final String euAlarm,
            final String status, final Integer scid, final String name,
            final Integer dssId, final Integer vcid,
            final IAccurateDateTime rct, final PacketIdHolder packetId,
            final Long frameId) {
		
        return new DatabaseAggregateChannelSample(appContext, sessionId, fromSse, isRealtime, sclk, ert, scet, sol, v,
                                                  ct, cid,
                cindex, module, sessionHost, eu, dnAlarm, euAlarm, status, scid, name, dssId,
                vcid, rct, packetId, frameId);
	}
}
