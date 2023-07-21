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

import jpl.gds.db.api.types.cfdp.IDbCfdpRequestReceivedFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpRequestReceivedProvider;
import jpl.gds.db.api.types.cfdp.IDbCfdpRequestReceivedUpdater;
import jpl.gds.db.impl.types.AbstractDbQueryableFactory;
import org.springframework.context.ApplicationContext;

/**
 * {@code DbCfdpRequestReceivedFactory} implements IDbCfdpRequestReceivedFactory and is used to create CFDP Request
 * Received database objects.
 */
public class DbCfdpRequestReceivedFactory
        extends AbstractDbQueryableFactory<IDbCfdpRequestReceivedProvider, IDbCfdpRequestReceivedUpdater>
        implements IDbCfdpRequestReceivedFactory {
    /**
     * Private Constructor
     *
     * @param appContext the Spring Application Context
     */
    public DbCfdpRequestReceivedFactory(final ApplicationContext appContext) {
        super(appContext, IDbCfdpRequestReceivedUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpRequestReceivedProvider createQueryableProvider() {
        return new DatabaseCfdpRequestReceived(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpRequestReceivedUpdater createQueryableUpdater() {
        return new DatabaseCfdpRequestReceived(appContext);
    }

}
