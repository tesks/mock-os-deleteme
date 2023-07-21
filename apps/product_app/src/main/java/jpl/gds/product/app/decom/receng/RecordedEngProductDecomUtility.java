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
package jpl.gds.product.app.decom.receng;

import java.util.LinkedList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbProductFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.order.IProductOrderByType;
import jpl.gds.db.api.sql.order.ISessionOrderByType;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.api.types.IDbProductMetadataProvider;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.product.api.ProductException;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.time.DatabaseTimeType;


/**
 * Utilities for recorded engineering decom
 */
public final class RecordedEngProductDecomUtility {

	private RecordedEngProductDecomUtility(){
	}
	
	private static final int PRODUCT_FETCH_SIZE = 500;
	private static ISessionFetch sessionFetch;
	
    private static IOrderByTypeFactory orderByTypeFactory;

	/**
     * Retrieve an instance of IDbSessionProvider from database by its Session Key (ID)
     * and session host.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param sessionKey
     *            the ID or SessionKey of the requested session.
     * @param sessionHost
     *            the host name of the requested session
	 * @param sessionFragment
	  *            the fragment of the requested session
     * @return a populated IDbSessionProvider object representing the requested Session.
     * @throws ProductException
     *             if there is not one and only one session satisfying this query.
     */
	public synchronized static IDbSessionUpdater retrieveSessionById(final ApplicationContext appContext, final long sessionKey,
	           final String sessionHost, final int sessionFragment) throws ProductException {
        final IDbSessionInfoFactory dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        final IDbSessionInfoUpdater sessionInfo = dbSessionInfoFactory.createQueryableUpdater();

        orderByTypeFactory = appContext.getBean(IOrderByTypeFactory.class);

		sessionInfo.setSessionKey(sessionKey);
		final IDbOrderByType orderBy = orderByTypeFactory.getOrderByType(OrderByType.SESSION_ORDER_BY,
		                                                            ISessionOrderByType.NONE_TYPE);

		sessionInfo.setStartTimeLowerBound(new AccurateDateTime(0L));
		if (sessionHost != null) {
			sessionInfo.setHostPattern(sessionHost);
		}
		final int batchSize = 1;
		try {
			// Use a static fetch object rather than creating a new DB connection
			// every time
			if (sessionFetch == null) {
				sessionFetch = appContext.getBean(IDbSqlFetchFactory.class).getSessionFetch(false);
			}
            @SuppressWarnings("unchecked")
            final List<IDbSessionUpdater> list = (List<IDbSessionUpdater>) sessionFetch.get(sessionInfo, null,
                                                                                            batchSize, orderBy, sessionFragment);
			final int size = list.size();
			if (size > 1) {
				throw new ProductException("Discrepency exists between database session information and metadata: " + size
						+ " sessions exist with Session ID: " + sessionKey + ".");
					}
			if (size < 1) {
				throw new ProductException(
						"Discrepency exists between database session information and metadata: No session exists with Session ID: "
								+ sessionKey + ".");
			}
			return list.get(0);
		}
		catch (final DatabaseException e) {
			throw new ProductException("Error querying database for session id: " + sessionKey);
		}
	}
	
	/**
     * Fetches the data file names for all data products with types matching an APID
     * in the given apid list belonging to the session with the given key
     * and host name.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param key
     *            session ID to fetch products from
     * @param host
     *            host of the session to fetch products from
     * @param apidList
     *            list of recorded product apids for product APIDs to match
     * @return array of product data file names, or null if no matching products found
     */
    @SuppressWarnings("unchecked")
    public static String[] getSessionProductFiles(final ApplicationContext appContext, final long key,
                                                  final String host, final List<Integer> apidList) {
		IDbProductFetch pf = null;
		try {
			pf = appContext.getBean(IDbSqlFetchFactory.class).getProductFetch(false);
		} catch (final Exception e) {
			e.printStackTrace();
            TraceManager.getDefaultTracer().error("Unable to create product fetch adapter for database");

			return null;
		}
        final IDbSessionInfoFactory dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        final IDbSessionInfoUpdater sessionInfo = dbSessionInfoFactory.createQueryableUpdater();
		sessionInfo.setSessionKey(key);
		sessionInfo.setStartTimeLowerBound(new AccurateDateTime(0L));
		if (host != null) {
			sessionInfo.setHostPattern(host);
		}

		final Object[] params = new Object[6];

		params[0] = null;
		params[1] = null;
		params[2] = false;
        params[3] = orderByTypeFactory.getOrderByType(OrderByType.PRODUCT_ORDER_BY, IProductOrderByType.DVT_SCET_TYPE);
		params[4] = null;
		params[5] = null;

		final List<String> results = new LinkedList<String>();

		try
		{
            List<IDbProductMetadataProvider> products = (List<IDbProductMetadataProvider>) pf.get(sessionInfo,
                                                                                               new DatabaseTimeRange(DatabaseTimeType.SCET),
                                                                                               PRODUCT_FETCH_SIZE,
                                                                                               params);

			while (! products.isEmpty())
			{
				for (final IDbProductMetadataProvider md: products)
				{
					if (apidList.contains(md.getApid()))
					{
						results.add(md.getFullPath());
					}
				}

                products = (List<IDbProductMetadataProvider>) pf.getNextResultBatch();
			}
        }
        catch (final DatabaseException e) {
			e.printStackTrace();
            TraceManager.getDefaultTracer()
                    .error("Unable to fetch products from database for session " + key);

					return null;
		}

		if (results.isEmpty()) {
            TraceManager.getDefaultTracer()
                    .info("Session " + key + " contains no recorded engineering products that require data extraction");

			return null;
		}
		final String[] arrayResult = new String[results.size()];

		return results.toArray(arrayResult);
	}
}
