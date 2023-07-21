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
package jpl.gds.db.impl.types.cfdp;

import jpl.gds.db.api.types.cfdp.IDbCfdpRequestResultFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpRequestResultProvider;
import jpl.gds.db.api.types.cfdp.IDbCfdpRequestResultUpdater;
import jpl.gds.db.impl.types.AbstractDbQueryableFactory;
import org.springframework.context.ApplicationContext;

/**
 * {@code DbCfdpRequestResultFactory} implements IDbCfdpRequestResultFactory and is used to create CFDP Request Result
 * database objects.
 */
public class DbCfdpRequestResultFactory
        extends AbstractDbQueryableFactory<IDbCfdpRequestResultProvider, IDbCfdpRequestResultUpdater>
        implements IDbCfdpRequestResultFactory {
    /**
     * Private Constructor
     *
     * @param appContext the Spring Application Context
     */
    public DbCfdpRequestResultFactory(final ApplicationContext appContext) {
        super(appContext, IDbCfdpRequestResultUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpRequestResultProvider createQueryableProvider() {
        return new DatabaseCfdpRequestResult(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpRequestResultUpdater createQueryableUpdater() {
        return new DatabaseCfdpRequestResult(appContext);
    }
}
