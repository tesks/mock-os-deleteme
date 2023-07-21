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

import jpl.gds.db.api.types.cfdp.IDbCfdpIndicationFactory;
import jpl.gds.db.api.types.cfdp.IDbCfdpIndicationProvider;
import jpl.gds.db.api.types.cfdp.IDbCfdpIndicationUpdater;
import jpl.gds.db.impl.types.AbstractDbQueryableFactory;
import org.springframework.context.ApplicationContext;

/**
 * {@code DbCfdpIndicationFactory} implements IDbCfdpIndicationFactory and is used to create CFDP Indication database
 * objects.
 */
public class DbCfdpIndicationFactory
        extends AbstractDbQueryableFactory<IDbCfdpIndicationProvider, IDbCfdpIndicationUpdater>
        implements IDbCfdpIndicationFactory {
    /**
     * Private Constructor
     *
     * @param appContext the Spring Application Context
     */
    public DbCfdpIndicationFactory(final ApplicationContext appContext) {
        super(appContext, IDbCfdpIndicationUpdater.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpIndicationProvider createQueryableProvider() {
        return new DatabaseCfdpIndication(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbCfdpIndicationUpdater createQueryableUpdater() {
        return new DatabaseCfdpIndication(appContext);
    }

}
