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

import jpl.gds.db.api.types.IDbChannelMetaDataFactory;
import jpl.gds.db.api.types.IDbChannelMetaDataProvider;
import jpl.gds.db.api.types.IDbChannelMetaDataUpdater;


public class DbChannelMetaDataFactory extends
        AbstractDbQueryableFactory<IDbChannelMetaDataProvider, IDbChannelMetaDataUpdater>
        implements IDbChannelMetaDataFactory {
    /**
     * @param appContext
     *            the Spring Application Context
     */
    public DbChannelMetaDataFactory(final ApplicationContext appContext) {
        super(appContext, IDbChannelMetaDataUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbChannelMetaDataProvider createQueryableProvider(final String channelStem, final String channelId,
                                                              final String channelName) {
        return new DatabaseChannelMetaData(appContext, channelStem, channelId, channelName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbChannelMetaDataProvider createQueryableProvider(final String channelStem, final String channelId,
                                                              final String channelName, final int count) {
        return new DatabaseChannelMetaData(appContext, channelStem, channelId, channelName, count);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbChannelMetaDataUpdater createQueryableUpdater(final String channelStem, final String channelId,
                                                            final String channelName) {
        return convertProviderToUpdater(createQueryableProvider(channelStem, channelId, channelName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbChannelMetaDataUpdater createQueryableUpdater(final String channelStem, final String channelId,
                                                            final String channelName, final int count) {
        return convertProviderToUpdater(createQueryableProvider(channelStem, channelId, channelName, count));
    }
}
