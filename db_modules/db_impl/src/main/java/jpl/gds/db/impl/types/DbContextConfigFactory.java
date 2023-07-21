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

import jpl.gds.db.api.types.IDbContextConfigFactory;
import jpl.gds.db.api.types.IDbContextConfigProvider;
import jpl.gds.db.api.types.IDbContextConfigUpdater;
import org.springframework.context.ApplicationContext;

/**
 * Implementation for IDbContextConfigFactory
 */
public class DbContextConfigFactory extends AbstractDbQueryableFactory<IDbContextConfigProvider, IDbContextConfigUpdater>
        implements IDbContextConfigFactory {

    /**
     * @param appContext
     *            the Spring Application Context
     */
    public DbContextConfigFactory(final ApplicationContext appContext) {
        super(appContext, IDbContextConfigUpdater.class);
    }

    @Override
    public IDbContextConfigProvider createQueryableProvider() {
        return new DatabaseContextConfig(appContext);
    }
}