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

import jpl.gds.db.api.types.cfdp.IDbCfdpPduSentFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpPduSentProvider;
import jpl.gds.db.api.types.cfdp.IDbCfdpPduSentUpdater;
import jpl.gds.db.impl.types.AbstractDbQueryableFactory;
import org.springframework.context.ApplicationContext;

/**
 * {@code DbCfdpPduSentFactory} implements IDbCfdpPduSentFactory and is used to create CFDP PDU Sent database objects.
 */
public class DbCfdpPduSentFactory
        extends AbstractDbQueryableFactory<IDbCfdpPduSentProvider, IDbCfdpPduSentUpdater>
        implements IDbCfdpPduSentFactory {
    /**
     * Private Constructor
     *
     * @param appContext the Spring Application Context
     */
    public DbCfdpPduSentFactory(final ApplicationContext appContext) {
        super(appContext, IDbCfdpPduSentUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpPduSentProvider createQueryableProvider() {
        return new DatabaseCfdpPduSent(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpPduSentUpdater createQueryableUpdater() {
        return new DatabaseCfdpPduSent(appContext);
    }

}
