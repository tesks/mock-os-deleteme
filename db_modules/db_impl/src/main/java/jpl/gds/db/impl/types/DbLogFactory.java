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

import jpl.gds.db.api.types.IDbLogFactory;
import jpl.gds.db.api.types.IDbLogProvider;
import jpl.gds.db.api.types.IDbLogUpdater;


public class DbLogFactory extends AbstractDbQueryableFactory<IDbLogProvider, IDbLogUpdater>
        implements IDbLogFactory {
    /**
     * Private Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public DbLogFactory(final ApplicationContext appContext) {
        super(appContext,
              IDbLogUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbLogProvider createQueryableProvider() {
        return new DatabaseLog(appContext);
    }
}