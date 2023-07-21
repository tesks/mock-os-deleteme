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

import jpl.gds.db.api.types.cfdp.IDbCfdpFileUplinkFinishedFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpFileUplinkFinishedProvider;
import jpl.gds.db.api.types.cfdp.IDbCfdpFileUplinkFinishedUpdater;
import jpl.gds.db.impl.types.AbstractDbQueryableFactory;
import org.springframework.context.ApplicationContext;

/**
 * {@code DbCfdpFileUplinkFinishedFactory} implements IDbCfdpFileUplinkFinishedFactory and is used to create CFDP File
 * Uplink Finished database objects.
 */
public class DbCfdpFileUplinkFinishedFactory
        extends AbstractDbQueryableFactory<IDbCfdpFileUplinkFinishedProvider, IDbCfdpFileUplinkFinishedUpdater>
        implements IDbCfdpFileUplinkFinishedFactory {
    /**
     * Private Constructor
     *
     * @param appContext the Spring Application Context
     */
    public DbCfdpFileUplinkFinishedFactory(final ApplicationContext appContext) {
        super(appContext, IDbCfdpFileUplinkFinishedUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpFileUplinkFinishedProvider createQueryableProvider() {
        return new DatabaseCfdpFileUplinkFinished(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpFileUplinkFinishedUpdater createQueryableUpdater() {
        return new DatabaseCfdpFileUplinkFinished(appContext);
    }

}
