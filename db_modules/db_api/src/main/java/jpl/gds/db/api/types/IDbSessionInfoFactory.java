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
package jpl.gds.db.api.types;

import jpl.gds.context.api.IContextConfiguration;


public interface IDbSessionInfoFactory
        extends IDbQueryableFactory<IDbSessionInfoProvider, IDbSessionInfoUpdater> {

    /**
     * @param args
     *            arguments necessary to create a new IDbSessionProvider object
     * @return a new IDbSessionProvider object
     */
    @Override
    IDbSessionInfoProvider createQueryableProvider();

    /**
     * @param contextConfig
     *            the Context Configuration to install in the returned IDbSessionInfoProvider
     * @return a new IDbSessionProvider object
     */
    IDbSessionInfoProvider createQueryableProvider(IContextConfiguration contextConfig);

    /**
     * @param contextConfig
     *            the Context Configuration to install in the returned IDbSessionInfoUpdater
     * @return a new IDbSessionUpdater object
     */
    IDbSessionInfoUpdater createQueryableUpdater(IContextConfiguration contextConfig);
}