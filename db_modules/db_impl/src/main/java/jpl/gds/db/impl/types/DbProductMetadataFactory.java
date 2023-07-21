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

import jpl.gds.db.api.types.IDbProductMetadataFactory;
import jpl.gds.db.api.types.IDbProductMetadataProvider;
import jpl.gds.db.api.types.IDbProductMetadataUpdater;


public class DbProductMetadataFactory
        extends AbstractDbQueryableFactory<IDbProductMetadataProvider, IDbProductMetadataUpdater>
        implements IDbProductMetadataFactory {
    /**
     * Private Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public DbProductMetadataFactory(final ApplicationContext appContext) {
        super(appContext,
              IDbProductMetadataUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbProductMetadataProvider createQueryableProvider() {
        return new ReferenceDatabaseProductMetadata(appContext);
    }
}
