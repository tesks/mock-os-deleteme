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

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;


public class DbSessionInfoFactory extends AbstractDbQueryableFactory<IDbSessionInfoProvider, IDbSessionInfoUpdater>
        implements IDbSessionInfoFactory {
    /**
     * @param appContext
     *            the Spring Application Context
     */
    public DbSessionInfoFactory(final ApplicationContext appContext) {
        super(appContext,
              IDbSessionInfoUpdater.class);
    }

    @Override
    public IDbSessionInfoProvider createQueryableProvider() {
        return new DatabaseSessionInfo();
    }

    @Override
    public IDbSessionInfoProvider createQueryableProvider(final IContextConfiguration contextConfig) {
        return new DatabaseSessionInfo(contextConfig);
    }

    @Override
    public IDbSessionInfoUpdater createQueryableUpdater(final IContextConfiguration contextConfig) {
        return convertProviderToUpdater(createQueryableProvider(contextConfig));
    }
}
