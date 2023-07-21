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

import jpl.gds.db.api.types.cfdp.IDbCfdpPduReceivedFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpPduReceivedProvider;
import jpl.gds.db.api.types.cfdp.IDbCfdpPduReceivedUpdater;
import jpl.gds.db.impl.types.AbstractDbQueryableFactory;
import org.springframework.context.ApplicationContext;

/**
 * {@code DbCfdpPduReceivedFactory} implements IDbCfdpPduReceivedFactory and is used to create CFDP PDU Received
 * database objects.
 */
public class DbCfdpPduReceivedFactory
        extends AbstractDbQueryableFactory<IDbCfdpPduReceivedProvider, IDbCfdpPduReceivedUpdater>
        implements IDbCfdpPduReceivedFactory {
    /**
     * Private Constructor
     *
     * @param appContext the Spring Application Context
     */
    public DbCfdpPduReceivedFactory(final ApplicationContext appContext) {
        super(appContext, IDbCfdpPduReceivedUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpPduReceivedProvider createQueryableProvider() {
        return new DatabaseCfdpPduReceived(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpPduReceivedUpdater createQueryableUpdater() {
        return new DatabaseCfdpPduReceived(appContext);
    }

}
