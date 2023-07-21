/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.product.context;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.processors.descriptions.IPdppDescription;
import jpl.gds.session.config.SessionConfiguration;
import org.springframework.context.ApplicationContext;

/**
 * Interface for PdppSessionFetcher to allow for instantiation with Spring
 */
public interface IPdppSessionFetcher {

    /**
     * @param sessionQuery
     * @return
     * @throws DatabaseException
     */
    IDbSessionUpdater issueQueryNoneTypeOrder(IDbSessionInfoProvider sessionQuery) throws DatabaseException;

    /**
     * Retrieve a session based on product metadata
     *
     * @param md
     * @return reconstructed session configuration.  If session not found, null.
     * @throws DatabaseException
     */
    IContextConfiguration getSession(IProductMetadataProvider md) throws DatabaseException;

    /**
     * @param dbSession
     * @return
     */
    SessionConfiguration getSessionConfigurationFromDatabaseSession(IDbSessionUpdater dbSession);


    /**
     * Retrieve a session based on session Id and hostname
     *
     * @param sessionId
     * @param sessionHost
     * @return
     * @throws DatabaseException
     */
    IDbSessionUpdater getDatabaseSession(Long sessionId, String sessionHost)
            throws DatabaseException;

    /**
     * Retrieve a session based on product metadata
     *
     * @param md
     * @return
     * @throws DatabaseException
     */
    IDbSessionUpdater getDatabaseSession(IProductMetadataProvider md) throws DatabaseException;

    /**
     * This will not set up anything special in the child context.  All dictionary values should already be set, this will
     * just create the new session config if necessary.
     *
     * @param parent
     * @param childContext
     * @param fswVersion
     * @param description
     * @return
     * @throws DatabaseException
     */
    IContextConfiguration getOrCreateChildSession(IContextConfiguration parent,
                                                 ApplicationContext childContext,
                                                 String fswVersion,
                                                 IPdppDescription description) throws DatabaseException;

    /**
     * Based on the parent session config information, retrieve the child session
     *
     * @param parent
     * @param fswVersion
     * @param description
     * @return IDbSessionUpdater
     * @throws DatabaseException
     */
    IDbSessionUpdater getChildDatabaseSession(IContextConfiguration parent,
                                              String fswVersion,
                                              IPdppDescription description) throws DatabaseException;
}
