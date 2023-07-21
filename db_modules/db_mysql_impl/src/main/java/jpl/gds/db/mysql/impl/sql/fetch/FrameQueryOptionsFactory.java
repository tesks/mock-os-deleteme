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
package jpl.gds.db.mysql.impl.sql.fetch;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.fetch.FrameQueryOptions;
import jpl.gds.db.api.sql.fetch.IFrameQueryOptionsFactory;
import jpl.gds.db.api.sql.fetch.IFrameQueryOptionsProvider;
import jpl.gds.db.api.sql.fetch.IFrameQueryOptionsUpdater;

public class FrameQueryOptionsFactory implements IFrameQueryOptionsFactory {
    @SuppressWarnings("unused")
    private final ApplicationContext appContext;

    /**
     * @param appContext
     *            the Spring Application Context (unused for now)
     */
    public FrameQueryOptionsFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
    } 

    /**
     * {@inheritDoc}
     */
    @Override
    public IFrameQueryOptionsProvider createFrameQuereyOptions() {
        return new FrameQueryOptions();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public IFrameQueryOptionsUpdater convertProviderToUpdater(final IFrameQueryOptionsProvider frameQueryOptionsProvider) {
        return IFrameQueryOptionsUpdater.class.cast(frameQueryOptionsProvider);
    }
}
