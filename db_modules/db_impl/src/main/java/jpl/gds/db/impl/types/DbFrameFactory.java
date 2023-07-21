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

import jpl.gds.db.api.types.IDbFrameFactory;
import jpl.gds.db.api.types.IDbFrameProvider;
import jpl.gds.db.api.types.IDbFrameUpdater;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.InvalidFrameCode;


public class DbFrameFactory extends AbstractDbQueryableFactory<IDbFrameProvider, IDbFrameUpdater>
        implements IDbFrameFactory {
    /**
     * Private Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public DbFrameFactory(final ApplicationContext appContext) {
        super(appContext, IDbFrameUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbFrameProvider createQueryableProvider() {
        return new DatabaseFrame(appContext);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IDbFrameProvider createQueryableProvider(final Long id,
                                                    final String type, final IAccurateDateTime ert,
                                                    final Integer relaySpacecraftId, final Integer vcid,
                                                    final Integer vcfc, final Integer dssId, final Double bitRate,
                                                    final byte[] body, final InvalidFrameCode badReason,
                                                    final Long testSessionId, final String sessionHost,
                                                    final Long fileByteOffset, final boolean fillFrame,
                                                    final String sleMetadata) {
        return new DatabaseFrame(appContext, id, type, ert, relaySpacecraftId, vcid, vcfc, dssId, bitRate, body,
                                 badReason, testSessionId, sessionHost, fileByteOffset, fillFrame, sleMetadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbFrameUpdater createQueryableUpdater(final Long id, final String type, final IAccurateDateTime ert,
                                                  final Integer relaySpacecraftId, final Integer vcid,
                                                  final Integer vcfc, final Integer dssId, final Double bitRate,
                                                  final byte[] body, final InvalidFrameCode badReason,
                                                  final Long testSessionId, final String sessionHost,
                                                  final Long fileByteOffset, final boolean fillFrame,
                                                  final String sleMedatada) {
        return convertProviderToUpdater(createQueryableProvider(id, type, ert, relaySpacecraftId, vcid,
                                                                vcfc, dssId, bitRate, body, badReason, testSessionId,
                                                                sessionHost, fileByteOffset, fillFrame, sleMedatada));
    }
}
