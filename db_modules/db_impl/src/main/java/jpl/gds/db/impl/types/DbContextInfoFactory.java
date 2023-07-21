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

import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.db.api.types.IDbContextInfoFactory;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbContextInfoUpdater;

import org.springframework.context.ApplicationContext;

/**
 * Factory for Context Config  provider / updater objects
 */
public class DbContextInfoFactory extends AbstractDbQueryableFactory<IDbContextInfoProvider, IDbContextInfoUpdater>
        implements IDbContextInfoFactory {

    public DbContextInfoFactory(final ApplicationContext appContext){
        super(appContext, IDbContextInfoUpdater.class);
    }

    @Override
    public IDbContextInfoProvider createQueryableProvider() {
        return new DatabaseContextInfo();
    }

    @Override
    public IDbContextInfoProvider createQueryableProvider(final ISimpleContextConfiguration contextConfig) {
        return new DatabaseContextInfo(contextConfig);
    }

    @Override
    public IDbContextInfoUpdater createQueryableUpdater(final ISimpleContextConfiguration contextConfig) {
        return convertProviderToUpdater(createQueryableProvider(contextConfig));
    }
}
