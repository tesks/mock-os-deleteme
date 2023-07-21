/**
 * 
 */
package jpl.gds.product.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jpl.gds.product.automation.hibernate.IAutomationLogger;
import jpl.gds.product.processors.descriptions.IPdppDescription;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.order.ISessionOrderByType;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.api.types.IDbSessionFactory;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionProvider;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.ProductException;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.time.AccurateDateTime;

/**
 * Class PdppSessionFetcher
 *
 */
public class PdppSessionFetcher implements IPdppSessionFetcher {
    public final IAutomationLogger log;
	/**
	 * 07/09/2012  - MPCS-3916 Added constants that are directly related to MPCS for MSL Database Schema's Session field sizes.
	 * 
	 * 07/26/2016  - MPCS-8180 - changed field lengths so they're now pulled from ISessionStore
	 * 
	 * MPCS-9572  - 4/5/18. Use fetch factory rather than archive controller
	 * 
	 */
	private final IDbSqlFetchFactory sessionFetchFactory;
    private final IDbSessionInfoFactory   dbSessionInfoFactory;
    private final IDbSessionFactory       dbSessionFactory;
	private final MissionProperties missionProperties;
	private final ConnectionProperties connectionProperties;
    private final IOrderByTypeFactory     orderByTypeFactory;

	public PdppSessionFetcher(final ApplicationContext appContext) {
        this.log = appContext.getBean(IAutomationLogger.class);
		this.sessionFetchFactory = appContext.getBean(IDbSqlFetchFactory.class);
        this.dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        this.dbSessionFactory = appContext.getBean(IDbSessionFactory.class);
		this.missionProperties = appContext.getBean(MissionProperties.class);
		this.connectionProperties = appContext.getBean(ConnectionProperties.class);
        this.orderByTypeFactory = appContext.getBean(IOrderByTypeFactory.class);
	}

	/**
	 * @param sessionQuery
	 * @return
	 * @throws DatabaseException 
	 */
	private IDbSessionProvider issueQueryIdDescending(final IDbSessionInfoProvider sessionQuery)
            throws DatabaseException {
		return issueQuery(sessionQuery, ISessionOrderByType.ID_DESC_TYPE);
	}

	/**
	 * @param sessionQuery
	 * @return
	 * @throws DatabaseException 
	 */
	public IDbSessionUpdater issueQueryNoneTypeOrder(final IDbSessionInfoProvider sessionQuery)
            throws DatabaseException {
		return issueQuery(sessionQuery, ISessionOrderByType.NONE_TYPE);
	}

	private IDbSessionUpdater issueQuery(final IDbSessionInfoProvider sessionQuery, final int orderBy)
            throws DatabaseException {
		final int batchSize = 1;
		final ISessionFetch sessionFetch = sessionFetchFactory.getSessionFetch(false);
		try {
			@SuppressWarnings("unchecked")
            final
            List<IDbSessionUpdater> list = (List<IDbSessionUpdater>) sessionFetch.get(sessionQuery, null, batchSize,
                                                                                      orderByTypeFactory.getOrderByType(OrderByType.SESSION_ORDER_BY,
					orderBy));

			if (list.isEmpty()) {
				return null;
			} else {
				return list.get(0);
			}
		}
		// MPCS-8180 07/18/16 Added close
		finally{
			sessionFetch.close();
		}
	}


	/**
	 * Attempts to build a session configuration for the provided metadata object by querying the database 
	 * for the session and building an object.  
	 * @param md 
	 * @return reconstructed session configuration.  If session not found, null.
	 * @throws DatabaseException
	 */
	@Override
	public IContextConfiguration getSession(final IProductMetadataProvider md) throws DatabaseException {
        final IDbSessionUpdater ds = this.getDatabaseSession(md);

		if (ds == null) {
			return null;
		} else {
			final IContextConfiguration sc = new SessionConfiguration(missionProperties, connectionProperties, false);
			ds.setIntoContextConfiguration(sc);
			return sc;
		}
	}
	
	/**
	 * @param dbSession
	 * @return
	 */
	@Override
	public SessionConfiguration getSessionConfigurationFromDatabaseSession(final IDbSessionUpdater dbSession) {
		final SessionConfiguration sc = new SessionConfiguration(missionProperties, connectionProperties, false);
		dbSession.setIntoContextConfiguration(sc);
		return sc;
	}

	/**
	 * @param sessionId
	 * @param sessionHost
	 * @return
	 * @throws DatabaseException 
	 */
    @Override
	public IDbSessionUpdater getDatabaseSession(final Long sessionId, final String sessionHost)
            throws DatabaseException {
        final IDbSessionInfoUpdater sessionQuery = dbSessionInfoFactory.createQueryableUpdater();
		sessionQuery.setStartTimeLowerBound(new AccurateDateTime(0));
		sessionQuery.setStartTimeUpperBound(new AccurateDateTime(Long.MAX_VALUE));
		sessionQuery.setSessionKey(sessionId);
		sessionQuery.setHostPattern(sessionHost);

		return issueQueryNoneTypeOrder(sessionQuery);
	}
	
	/**
	 * @param md
	 * @return
	 * @throws DatabaseException 
	 */
    @Override
	public IDbSessionUpdater getDatabaseSession(final IProductMetadataProvider md) throws DatabaseException {
		return getDatabaseSession(md.getSessionId(), md.getSessionHost());
	}

	/**
	 * This will not set up anything special in the child context.  All dictionary values should already be set, this will 
	 * just create the new session config if necessary. 
	 * 
	 * This will not start the controller...
	 * @param parent
	 * @param childContext
	 * @param fswVersion
	 * @param description
	 * @return
	 * @throws DatabaseException
	 */
	@Override
	public IContextConfiguration getOrCreateChildSession(final IContextConfiguration parent,
														final ApplicationContext childContext,
														final String fswVersion,
														final IPdppDescription description) throws DatabaseException {
		// First check if the session already exists.
        final IDbSessionUpdater dbSession = getChildDatabaseSession(parent, fswVersion, description);

		final SessionConfiguration child = new SessionConfiguration(childContext);

		if (dbSession == null) {
			// Set the that matter into the configuration.
			child.getContextId().setName(description.generateName(parent.getContextId().getName()));
			child.getContextId().setDescription(description.generateDescription(parent.getContextId().getDescription()));
			child.getContextId().setType(description.generateType(parent.getContextId().getNumber()));
		} else {
			// Use everything from the db session config.
			dbSession.setIntoContextConfiguration(child);
		}

		return child;
	}
	
	/**
	 * @param parent
	 * @param fswVersion
	 * @param description
	 * @return
	 * @throws ProductException
	 * @throws DatabaseException 
	 */
    @Override
	public IDbSessionUpdater getChildDatabaseSession(final IContextConfiguration parent,
													 final String fswVersion,
													 final IPdppDescription description) throws DatabaseException {
		/*
		 * Set up Database fetch criteria
		 */
        final IDbSessionInfoUpdater sessionQuery = dbSessionInfoFactory.createQueryableUpdater();
		sessionQuery.addNamePattern(description.generateName(parent.getContextId().getName()));
		sessionQuery.addDescriptionPattern(description.generateDescription(parent.getContextId().getDescription()));
		/*
		 * Any date range
		 */
		sessionQuery.setStartTimeLowerBound(new AccurateDateTime(0));
		sessionQuery.setStartTimeUpperBound(new AccurateDateTime(Long.MAX_VALUE));
		sessionQuery.setDescriptionPatternList(Arrays.asList("%(" + description.getSessionSuffix() + ")"));

		/*
		 * Retrieved session must use the same dictionary as the template.
		 * 
		 */
		sessionQuery.setFswVersionPatternList(Arrays.asList(fswVersion));

		/*
		 * Retrieved session must have the same back-link.
		 */
		sessionQuery.setTypePatternList(Arrays.asList(description.generateType(parent.getContextId().getNumber())));

		sessionQuery.setSessionKeyList(new ArrayList<Long>());

		return issueQueryNoneTypeOrder(sessionQuery);
	}

}
