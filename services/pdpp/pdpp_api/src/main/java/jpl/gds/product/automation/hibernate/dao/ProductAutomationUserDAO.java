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
package jpl.gds.product.automation.hibernate.dao;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.springframework.context.ApplicationContext;

import jpl.gds.product.automation.hibernate.AutomationSessionFactory;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationLog;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;

/**
 * Product automation user table data accessor object.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original
 *          version in MPCS for MSL G9.
 * MPCS-8180 - 08/09/16 - In alias enums, changed base ALIAS enum
 *          to be set to the alias category value instead of null
 */
public class ProductAutomationUserDAO extends AbstractAutomationDAO {
	
	public ProductAutomationUserDAO(ApplicationContext appContext, AutomationSessionFactory sessionFactory) {
		super(appContext, sessionFactory);
	}

	private enum TimeQuery{
		EVENT,
		PRODUCT;
	}
	
	private enum TimeType{
		PRODUCT,
		STATUS;
	}
	
	private static final long MS_DIVISOR = 1000;
	private static final int LOAD_MAX_VALUE = 100;
	
	// These detached queries can be defined here and used when a query is done.  However, care must be taken when setting aliases for 
	// the queries in each of the methdos, because the aliases must be defined in this.  Use the statics for each alias defined below.
	// Static aliases for queries.
	
	private static final String QUERY_LIKE_STRING = "%%%s%%";
	
	//aliases
	private enum ProcessActionAlias{
		
		ALIAS(null),
		ACTION(ProductAutomationClassMapDAO.Column.MNEMONIC.toString());
		
		
		private String aliasCategory = "productActionalias";
		private String fullAlias;
		
		private ProcessActionAlias(String substring){
			if (substring == null){
				fullAlias = aliasCategory;
			} else{
				fullAlias = aliasCategory + "." + substring;
			}
		}
		
		@Override
		public String toString(){
			return fullAlias;
		}
	}
	
	private enum ProductAlias{
		
		ALIAS(null),
		ID(ProductAutomationProductDAO.Column.PRODUCT_ID.toString()),
		SESSION(ProductAutomationProductDAO.Column.SESSION_ID.toString()),
		HOST(ProductAutomationProductDAO.Column.SESSION_HOST.toString()),
		APID(ProductAutomationProductDAO.Column.APID.toString()),
		TIME(ProductAutomationProductDAO.Column.ADD_TIME.toString()),
		SCLK(ProductAutomationProductDAO.Column.SCLK_COARSE.toString()),
		PATH(ProductAutomationProductDAO.Column.PATH.toString()),
		PARENT(ProductAutomationProductDAO.Column.PARENT.toString());
		
		
		private String aliasCategory = "productAlias";
		private String fullAlias;
		
		private ProductAlias(String substring){
			if (substring == null){
				fullAlias = aliasCategory;
			} else{
				fullAlias = aliasCategory + "." + substring;
			}
		}
		
		@Override
		public String toString(){
			return fullAlias;
		}
	}

	private enum StatusAlias{
		
		ALIAS(null),
		ID(ProductAutomationStatusDAO.Column.STATUS_ID.toString()),
		NAME(ProductAutomationStatusDAO.Column.STATUS.toString()),
		TIME(ProductAutomationStatusDAO.Column.STATUS_TIME.toString());
		
		
		private String aliasCategory = "statusAlias";
		private String fullAlias;
		
		private StatusAlias(String substring){
			if (substring == null){
				fullAlias = aliasCategory;
			} else{
				fullAlias = aliasCategory + "." + substring;
			}
		}
		
		@Override
		public String toString(){
			return fullAlias;
		}
	}
	
	private enum ClassmapAlias{
		
		ALIAS(null),
		MNEMONIC(ProductAutomationClassMapDAO.Column.MNEMONIC.toString());
		
		
		private String aliasCategory = "classmapAlias";
		private String fullAlias;
		
		private ClassmapAlias(String substring){
			if (substring == null){
				fullAlias = aliasCategory;
			} else{
				fullAlias = aliasCategory + "." + substring;
			}
		}
		
		@Override
		public String toString(){
			return fullAlias;
		}
	}
	
	
	private static final DetachedCriteria STATUS_FOR_PRODUCT = DetachedCriteria.forClass(ProductAutomationStatus.class)
			.add(Restrictions.eqProperty(ProductAutomationStatusDAO.Column.PRODUCT.toString(), ProductAlias.ID.toString()));
	
	private static final DetachedCriteria LATEST_STATUS_DETACHED = STATUS_FOR_PRODUCT
			.setProjection(Projections.max(ProductAutomationStatusDAO.Column.STATUS_ID.toString()));
	
	// Detached for bottom up view to get products that are not parents to anything and their parent is not null.
	private static final DetachedCriteria BOTTOM_UP_PRODUCT = DetachedCriteria.forClass(ProductAutomationProduct.class)
			.add(Restrictions.eqProperty(ProductAlias.ID.toString(), 
					ProductAutomationProductDAO.Column.PARENT.toString() + "." + ProductAutomationProductDAO.Column.PRODUCT_ID.toString()))
			.setProjection(Projections.rowCount());
	
	/*
	 * MPCS-8182 08/09/16 - Reintroducted FINISHED_STATUSES,
	 * FINISHED_STATUSES_ALL, and PENDING_STATUSES. These were present in MPCS
	 * for MSL
	 */
	/** A collection of the String values for statuses when a product has been completed by automation*/
	public static final List<String> FINISHED_STATUSES;
	/** A collection of the String values for statuses when a product has been completed by any source */
	public static final List<String> FINISHED_STATUSES_ALL;
	/** A collection of the String values for status when a product has not been processed, but is (or will be) processed by automation */
	public static final List<String> PENDING_STATUSES;
	
	private static final Long NO_PRODUCT_LOG_PROCESSID = Long.valueOf(-1);
	private static final Long ARBITER_LOG_PROCESSID = Long.valueOf(0);
	
	static {
		FINISHED_STATUSES_ALL = Arrays.asList(ProductAutomationStatusDAO.Status.COMPLETED.toString(),
										  	  ProductAutomationStatusDAO.Status.UNKNOWN_COMPLETE.toString(),
										  	  ProductAutomationStatusDAO.Status.FAILED.toString(),
										  	  ProductAutomationStatusDAO.Status.COMPLETE_PRE_PB.toString());

		FINISHED_STATUSES = Arrays.asList(ProductAutomationStatusDAO.Status.COMPLETED.toString(),
				  						  ProductAutomationStatusDAO.Status.UNKNOWN_COMPLETE.toString(),
				  						  ProductAutomationStatusDAO.Status.FAILED.toString());
		
		PENDING_STATUSES = Arrays.asList(ProductAutomationStatusDAO.Status.CATEGORIZED.toString(),
				ProductAutomationStatusDAO.Status.UNCATEGORIZED.toString(),
										 ProductAutomationStatusDAO.Status.IGNORED.toString(),
										 ProductAutomationStatusDAO.Status.REASSIGNED.toString(),
										 ProductAutomationStatusDAO.Status.STARTED.toString(),
										 ProductAutomationStatusDAO.Status.TIMEOUT.toString());
	}	
	
	/**
	 * This is used for sorting a mix of the entity objects. 
	 */
	public static final Comparator<Object> MESH_COMPARATOR = new Comparator<Object>() {
	
		@Override
		/**
		 * Special comparator to for statuses actions and logs. If the time
		 * values are equal will sort based on the type. Status will be first,
		 * then logs then actions.
		 * 
		 * @param o1
		 *            The first object to be compared
		 * @param o2
		 *            The second object to be compared
		 * @return A negative integer if the second object is less than the
		 *         first, zero if the two objects are equal(or both are of
		 *         ProductAutomation types that cannot be compared by this
		 *         method), or a positive integer if the second object is
		 *         greater than the first
		 */
		public int compare(Object o1, Object o2) {
			Timestamp t1;
			Timestamp t2;
			Integer s1;
			Integer s2;
			
			// Have to get the time for whichever deal.
			if (o1 instanceof ProductAutomationStatus) {
				t1 = ((ProductAutomationStatus) o1).getStatusTime();
				s1 = 0;
			} else if (o1 instanceof ProductAutomationLog) {
				t1 = ((ProductAutomationLog) o1).getEventTime();
				s1 = 1;
			} else if (o1 instanceof ProductAutomationAction) {
				t1 = ((ProductAutomationAction) o1).getCompletedTime();
				
				if (t1 == null) {
					t1 = ((ProductAutomationAction) o1).getAssignedTime();
				}
				
				s1 = 2;
			} else {
				t1 = null;
				s1 = null;
			}
			
			if (o2 instanceof ProductAutomationStatus) {
				t2 = ((ProductAutomationStatus) o2).getStatusTime();
				s2 = 0;
			} else if (o2 instanceof ProductAutomationLog) {
				t2 = ((ProductAutomationLog) o2).getEventTime();
				s2 = 1;
			} else if (o2 instanceof ProductAutomationAction) {
				t2 = ((ProductAutomationAction) o2).getCompletedTime();
				
				if (t2 == null) {
					t2 = ((ProductAutomationAction) o2).getAssignedTime();
				}
				
				s2 = 2;
			} else {
				t2 = null;
				s2 = null;
			}
	
			if (t1 == null && t2 == null) {
				return 0;
			} else if (t1 == null && t2 != null) {
				return -1;
			} else if (t1 != null && t2 == null) {
				return 1;
			} else {
				Integer result = t1.compareTo(t2);
				
				if (result == 0) {
					// Means the two times are equal.  But it is possible to have the same 
					result = s1.compareTo(s2);
					
					// If these are equal, means they are the same type and the same time. 
					// So need to check if they are equal since these could still be different
					// things.
					
					if (result == 0 && !o1.equals(o2)) {
						// Just arbitrariliy set result.
						result = 1;
					} 
				}
				
				return result;
			}
		}
	};

	/**
	 * Methods to convert query results into the proper class.  Just makes it cleaner and in one place.
	 **/
	private SortedSet<ProductAutomationProduct> objectListToProductSet(List<?> queryResults) {
		SortedSet<ProductAutomationProduct> result = new TreeSet<ProductAutomationProduct>();
		
		for (Object thing : queryResults) {
			result.add((ProductAutomationProduct) thing);
		}
		
		return result;
	}
	
	
	private SortedSet<Long> objectToLongSortedSet(Collection<?> queryResults) {
		SortedSet<Long> ss = new TreeSet<Long>();
		
		for (Object thing : queryResults) {
			if (thing instanceof Long) {
				ss.add((Long) thing);
			}
		}
		
		return ss;
	}
		
	private SortedSet<ProductAutomationProcess> objectToProcessSortedSet(List<?> queryResults) {
		SortedSet<ProductAutomationProcess> ss = new TreeSet<ProductAutomationProcess>();
		
		for (Object thing : queryResults) {
			ss.add((ProductAutomationProcess) thing);
		}
		
		return ss;
	}
	
	/**
	 * Builds the criteria search based on the inputs given. This uses joins to
	 * find the
	 * 
	 * 
	 * @param product
	 *            the String name of the type of product to be specified by the
	 *            criteria
	 * @param sessionId
	 *            the session to be specified by the criteria
	 * @param host
	 *            the hose name to be specified by the criteria
	 * @param passNumber
	 *            the pass number to be specified by the criteria
	 * @param apid
	 *            the application process ID to be specified by the criteria
	 * @param timeType
	 *            Indicates the time type when timeTypeEntity specifies an event
	 *            time query. 767 if status time type, 777 if product time type
	 * @param timeTypeEntity
	 *            Indicates what type of time query is to be included in the
	 *            criteria, if any. 999 if event time query, 888 if product time
	 *            query
	 * @param latestStatus
	 *            true if the criteria is to find only the most recent status,
	 *            false if not
	 * @param lowerBound
	 *            the Timestamp indicating the earliest time allowed
	 * @param upperBound
	 *            the Timestamp indicating the latest time allowed
	 * @param statusNames
	 *            A String collection of the statuses allowed
	 * @param maxResults
	 *            The maximum number of results to be allowed by the criteria
	 * @param ascending
	 *            False if the results are to be ordered oldest to newest, true
	 *            if newest to oldest. If max results is set, then the results
	 *            may be cut off and not reach the full time frame.
	 * @param productQuery
	 *            if this is true, the id's returned will be for products.
	 * @param bottomUp
	 *            true if the products are to be returned from child to parent,
	 *            false if parent to child.
	 * 
	 * @return a Criteria object for performing a query in the database
	 */
	private Criteria getStatusCriteria(String product, Long sessionId, String host, Long passNumber, Integer apid, TimeType timeType, TimeQuery timeTypeEntity, boolean latestStatus, Timestamp lowerBound, 
			Timestamp upperBound, Collection<String> statusNames, Integer maxResults, boolean ascending, boolean productQuery, boolean bottomUp) {
		Criteria criteria = getSession().createCriteria(ProductAutomationStatus.class, StatusAlias.ALIAS.toString())
				.createAlias(ProductAutomationStatusDAO.Column.PRODUCT.toString(), ProductAlias.ALIAS.toString());
		
		if (product != null) {
			// Do a like query.
			criteria.add(Restrictions.like(ProductAlias.PATH.toString(), String.format(QUERY_LIKE_STRING, product)));
		}
		
		if (apid != null) {
			criteria.add(Restrictions.eq(ProductAlias.APID.toString(), apid));
		}
		
		if (sessionId != null) {
			criteria.add(Restrictions.eq(ProductAlias.SESSION.toString(), sessionId));
		}
		
		if (host != null) {
			criteria.add(Restrictions.eq(ProductAlias.HOST.toString(), host));
		}
		
		if (passNumber != null) {
			criteria.add(Restrictions.eq(ProductAutomationStatusDAO.Column.PASS_NUMBER.toString(), passNumber));
		}
		
		if (latestStatus) {
			criteria.add(Subqueries.propertyEq(StatusAlias.ID.toString(), LATEST_STATUS_DETACHED
					.setProjection(Projections.max(ProductAutomationStatusDAO.Column.STATUS_ID.toString()))));
		}
		
		if (statusNames != null && !statusNames.isEmpty()) {
			criteria.add(Restrictions.in(StatusAlias.NAME.toString(), statusNames));
		}
		
		// Check if there is any time restrictions needed.
		if (lowerBound != null || upperBound != null) {
			// Check if it is an event time or product time.
			
			// First, figure out if it is a product sclk time query or an event time query.
			if (timeTypeEntity == TimeQuery.EVENT) {
				String alias = ( (timeType == TimeType.STATUS) ? StatusAlias.TIME.toString() : ProductAlias.TIME.toString());
				
				if (lowerBound != null) {
					criteria.add(Restrictions.ge(alias, lowerBound));
				}
				
				if (upperBound != null) {
					criteria.add(Restrictions.lt(alias, lowerBound));
				}
			} else if (timeTypeEntity == TimeQuery.PRODUCT) {
				// Much simpler.  This will just look at the product sclk time.

				if (lowerBound != null) {
					criteria.add(Restrictions.ge(ProductAlias.SCLK.toString(), lowerBound.getTime() / MS_DIVISOR));
				}
				
				if (upperBound != null) {
					criteria.add(Restrictions.lt(ProductAlias.SCLK.toString(), upperBound.getTime() / MS_DIVISOR));
				}
			}
		}
			
		// If this is a product query need to do some checks on the product. 
		if (productQuery) {
			// First, set projections as the status product.
			if (bottomUp) {
				criteria.add(Restrictions.isNotNull(ProductAlias.PARENT.toString()))
					.add(Subqueries.eq(0L, BOTTOM_UP_PRODUCT));
			} else {
				// For top down, we want only products that do not have parents or the parent. 
				criteria.add(Restrictions.isNull(ProductAlias.PARENT.toString()));
			}
		}
		
		if (maxResults != null) {
			criteria.addOrder(ascending ? Order.asc(StatusAlias.ID.toString()) : Order.desc(StatusAlias.ID.toString()))
				.setMaxResults(maxResults);
		}

		if (productQuery) {
			criteria.setProjection(Projections.property(ProductAlias.ID.toString()));
		} else {
			criteria.setProjection(Projections.id());
		}
		
		criteria.setCacheable(true);

		return criteria;
	}
	
	/**
	 * Gets the status criteria and then loads the statuses from the cache or
	 * the db if not found.
	 * 
	 * @param product
	 *            the product for which all status will be found
	 * @param sessionId
	 *            the particular session that the product was produced
	 * @param host
	 *            the name of the machine that ran the particular session
	 * @param passNumber
	 *            the PDPP pass that relates to all statuses returned
	 * @param apid
	 *            the application process ID for all statuses returned
	 * @param latestStatus
	 *            true if only the most recent status is to be found, false if
	 *            not
	 * @param lowerBound
	 *            the earliest time that can be included in the returned results
	 * @param upperBound
	 *            the latest time that can be included in the returned results
	 * @param statusNames
	 *            the types of statuses that can be returned
	 * @param maxResults
	 *            the maximum number of results to be returned.
	 * @param ascending
	 *            true if the results start with the oldest and end with newest,
	 *            false if newest to oldest
	 * @return a collection of ProductAutomationStatus objects that conform to
	 *         the provided parameters and are ordered by their time and sorted
	 *         by the event time
	 */
	public Collection<ProductAutomationStatus> getStatusesByEventTime(String product, Long sessionId, String host, Long passNumber, Integer apid, boolean latestStatus, Timestamp lowerBound, 
			Timestamp upperBound, Collection<String> statusNames, Integer maxResults, boolean ascending) {
		
			return loadStatuses(getStatusCriteria(product, 
					sessionId, 
					host, 
					passNumber, 
					apid, 				
					TimeType.STATUS, // timeType, 
					TimeQuery.EVENT, // Time entity
					latestStatus, 
					lowerBound, 
					upperBound, 
					statusNames, 
					maxResults, 
					ascending, 
					false,
					false)
					.list());
	}
	
	/**
	 * Give a bunch of product paths and will return a set of product objects.
	 * 
	 * @param productPaths the file paths of the ProductAutomationProduct objects to be returned
	 * 
	 * @return a SortedSet of ProductAutomationProduct objects for the file paths given
	 */
	public SortedSet<ProductAutomationProduct> getTheseProducts(Collection<String> productPaths) {
		return objectListToProductSet(getSession().createCriteria(ProductAutomationProduct.class, ProductAlias.ALIAS.toString())
				.add(Restrictions.in(ProductAlias.PATH.toString(), productPaths))
				.list());
	}
	
	/**
	 * Gets the status criteria and then loads the statuses from the cache or the db if not found. 
	 * 
	 * @param product
	 *            the product for which all status will be found
	 * @param sessionId
	 *            the particular session that the product was produced
	 * @param host
	 *            the name of the machine that ran the particular session
	 * @param passNumber
	 *            the PDPP pass that relates to all statuses returned
	 * @param apid
	 *            the application process ID for all statuses returned
	 * @param latestStatus
	 *            true if only the most recent status is to be found, false if
	 *            not
	 * @param lowerBound
	 *            the earliest time that can be included in the returned results
	 * @param upperBound
	 *            the latest time that can be included in the returned results
	 * @param statusNames
	 *            the types of statuses that can be returned
	 * @param maxResults
	 *            the maximum number of results to be returned.
	 * @param ascending
	 *            true if the results start with the oldest and end with newest,
	 *            false if newest to oldest
	 * @return a collection of ProductAutomationStatus objects that conform to
	 *         the provided parameters and are ordered by their time and sorted
	 *         by the product time
	 */
	public Collection<ProductAutomationStatus> getStatusesByProductTime(String product, Long sessionId, String host, Long passNumber, Integer apid, boolean latestStatus, Timestamp lowerBound, 
			Timestamp upperBound, Collection<String> statusNames, Integer maxResults, boolean ascending) {
		
			return loadStatuses(getStatusCriteria(product, 
					sessionId, 
					host, 
					passNumber, 
					apid, 				
					TimeType.PRODUCT, // timeType, 
					TimeQuery.PRODUCT, // timeTypeEntity, 
					latestStatus, 
					lowerBound, 
					upperBound, 
					statusNames, 
					maxResults, 
					ascending,
					false, 
					false)
					.list());
	}
	
	/**
	 * Conv method. Queries with inputs and uses the status event time for to
	 * query for time ranges.
	 * 
	 * @param product
	 *            the product for which all status will be found
	 * @param sessionId
	 *            the particular session that the product was produced
	 * @param host
	 *            the name of the machine that ran the particular session
	 * @param passNumber
	 *            the PDPP pass that relates to all statuses returned
	 * @param apid
	 *            the application process ID for all statuses returned
	 * @param latestStatus
	 *            true if only the most recent status is to be found, false if
	 *            not
	 * @param lowerBound
	 *            the earliest time that can be included in the returned results
	 * @param upperBound
	 *            the latest time that can be included in the returned results
	 * @param statusNames
	 *            the types of statuses that can be returned
	 * @param maxResults
	 *            the maximum number of results to be returned.
	 * @param ascending
	 *            true if the results start with the oldest and end with newest,
	 *            false if newest to oldest
	 * @param bottomUp
	 *            true if the products are to be returned from child to parent,
	 *            false if parent to child.
	 * @return a collection of ProductAutomationStatus objects that conform to
	 *         the provided parameters and are ordered by their time and sorted
	 *         by the product time
	 */
	public Collection<ProductAutomationProduct> getProductsByEventTime(String product, Long sessionId, String host, Long passNumber, Integer apid, boolean latestStatus, Timestamp lowerBound, 
			Timestamp upperBound, Collection<String> statusNames, Integer maxResults, boolean ascending, boolean bottomUp) {
		// To ensure there are no duplicates, going to add the results to a sorted set first.
		
		return loadProducts(getStatusCriteria(product, 
				sessionId, 
				host, 
				passNumber, 
				apid, 				
				TimeType.STATUS, // timeType, 
				TimeQuery.EVENT, // timeTypeEntity, 
				latestStatus, 
				lowerBound, 
				upperBound, 
				statusNames, 
				maxResults, 
				ascending,
				true, 
				bottomUp)
				.list());
	}
	
	/**
	 * Conv method. Queries with inputs and uses the product sclk time for to
	 * query for time ranges.
	 * 
	 * @param product
	 *            the product for which all status will be found
	 * @param sessionId
	 *            the particular session that the product was produced
	 * @param host
	 *            the name of the machine that ran the particular session
	 * @param passNumber
	 *            the PDPP pass that relates to all statuses returned
	 * @param apid
	 *            the application process ID for all statuses returned
	 * @param latestStatus
	 *            true if only the most recent status is to be found, false if
	 *            not
	 * @param lowerBound
	 *            the earliest time that can be included in the returned results
	 * @param upperBound
	 *            the latest time that can be included in the returned results
	 * @param statusNames
	 *            the types of statuses that can be returned
	 * @param maxResults
	 *            the maximum number of results to be returned.
	 * @param ascending
	 *            true if oldest to newest, false if descending (newest to
	 *            oldest). If max results is set and true, the newest results
	 *            are trimmed. If max results is set and false, the oldest
	 *            results are trimmed
	 * @param bottomUp
	 *            true if the products are to be returned from child to parent,
	 *            false if parent to child.
	 * @return a Collection of ProductAutomationProduct objects organized by the
	 *         product time
	 */	
	public Collection<ProductAutomationProduct> getProductsByProductTime(String product, Long sessionId, String host, Long passNumber, Integer apid, boolean latestStatus, Timestamp lowerBound, 
			Timestamp upperBound, Collection<String> statusNames, Integer maxResults, boolean ascending, boolean bottomUp) {
		return loadProducts(getStatusCriteria(product, 
				sessionId, 
				host, 
				passNumber, 
				apid, 				
				TimeType.PRODUCT, // timeType, 
				TimeQuery.PRODUCT, // timeTypeEntity, 
				latestStatus, 
				lowerBound, 
				upperBound, 
				statusNames, 
				maxResults, 
				ascending,
				true, 
				bottomUp)
				.list());
	}
	
	/**
	 * Method used to find products for the ancestor loading. Gets all products
	 * in the time range given.
	 * 
	 * @param lowerBound
	 *            A Unix time for the oldest product to be found
	 * @param upperBound
	 *            A Unix time for the newest product to be found
	 * @return A collection of ProductAutomationProduct objects
	 */
	public Collection<ProductAutomationProduct> getProductsForLoad(Long lowerBound, Long upperBound) {
		return getProductsByEventTime(null, // product, 
				null, // sessionId, 
				null, // host, 
				null, // passNumber, 
				null, //apid, 
				false, // latestStatus, 
				lowerBound == null ? null : new Timestamp(lowerBound), // lowerBound, 
				upperBound == null ? null : new Timestamp(upperBound), // upperBound, 
				null, //statusNames
				LOAD_MAX_VALUE, // Max number.  Just get everything
				false,
				false
				);
	}

	/**
	 * Get all of the descendant products of the given product. AKA, any products
	 * that have been directly created from this product
	 * 
	 * @param product
	 *            the ProductAutomationProduct that is to be considered the
	 *            parent
	 * 
	 * @return a set of productIds that are all descendants of the given product
	 */
	public SortedSet<Long> getProductDescendants(ProductAutomationProduct product) {
		Criteria criteria = getSession().createCriteria(ProductAutomationProduct.class)
				.add(Restrictions.eq(ProductAutomationProductDAO.Column.PARENT.toString(), product))
				.setProjection(Projections.property(ProductAutomationProductDAO.Column.PRODUCT_ID.toString()));
		
		return objectToLongSortedSet(criteria.list());
	}
	
	/**
	 * Get all of the actions entries from the database that have been performed
	 * on a particular product during a particular pass
	 * 
	 * @param product
	 *            the name of the product in question
	 * @param host
	 *            the name of the host that performed the action
	 * @param sessionId
	 *            the session that completed the action
	 * @param passNumber
	 *            the pass number for all actions returned
	 * @param apid
	 *            the application process ID of this product
	 * @param lowerBound
	 *            the oldest time for the results returned
	 * @param upperBound
	 *            the latest time for the results returned
	 * @param completed
	 *            true if the action has been completed, false if not
	 * @param pending
	 *            true if the action is pending, false if not
	 * @param maxResults
	 *            the maximum number of results to be returned
	 * @param ascending
	 *            true if oldest to newest, false if descending (newest to
	 *            oldest). If max results is set and true, the newest results
	 *            are trimmed. If max results is set and false, the oldest
	 *            results are trimmed
	 * @param actionMnemonics
	 *            a collection of the types of actions to be included in the
	 *            resutls
	 * 
	 * @return a collection of ProductAutomationAction objects that conform to
	 *         the query parameters given
	 */
	public Collection<ProductAutomationAction> getActions(String product, String host, Long sessionId, Long passNumber,
			Integer apid, Timestamp lowerBound, Timestamp upperBound, boolean completed, boolean pending, 
			Integer maxResults, boolean ascending, Collection<String> actionMnemonics) {
		return loadActions(getActionCriteria(product, 
				host, 
				sessionId, 
				passNumber, 
				apid, 
				lowerBound, 
				upperBound, 
				completed, 
				pending,
				maxResults,
				ascending,
				actionMnemonics)
				.list());
	}

	private Criteria getActionCriteria(String product, String host, Long sessionId, Long passNumber,
			Integer apid, Timestamp lowerBound, Timestamp upperBound, boolean completed, boolean pending, 
			Integer maxResults, boolean ascending, Collection<String> actionMnemonics) {
		Criteria criteria = getSession().createCriteria(ProductAutomationAction.class)
				.createAlias(ProductAutomationActionDAO.Column.PRODUCT.toString(), ProductAlias.ALIAS.toString())
				.createAlias(ProductAutomationActionDAO.Column.ACTION_NAME.toString(), ClassmapAlias.ALIAS.toString());
		
		if (product != null) {
			criteria.add(Restrictions.eq(ProductAlias.PATH.toString(), String.format(QUERY_LIKE_STRING, product)));
		}
		
		if (host != null) {
			criteria.add(Restrictions.like(ProductAlias.HOST.toString(), String.format(QUERY_LIKE_STRING, host)));
		}
		
		if (sessionId != null) {
			criteria.add(Restrictions.eq(ProductAlias.SESSION.toString(), sessionId));
		}
		
		if (apid != null) {
			criteria.add(Restrictions.eq(ProductAlias.APID.toString(), apid));
		}
		
		if (passNumber != null) {
			criteria.add(Restrictions.eq(ProductAutomationActionDAO.Column.PASS_NUMBER.toString(), passNumber));
		}
		
		if (lowerBound != null) {
			criteria.add(Restrictions.ge(ProductAutomationActionDAO.Column.ASSIGNED.toString(), lowerBound));
		}

		if (upperBound != null) {
			criteria.add(Restrictions.lt(ProductAutomationActionDAO.Column.ASSIGNED.toString(), upperBound));
		}
		
		if (actionMnemonics != null) {
			criteria.add(Restrictions.in(ClassmapAlias.MNEMONIC.toString(), actionMnemonics));
		}
		
		// Only check for completed if one of the flags are false.
		if (!(completed && pending)) {
			if (completed) {
				criteria.add(Restrictions.isNotNull(ProductAutomationActionDAO.Column.COMPLETE.toString()));
			}

			if (pending) {
				criteria.add(Restrictions.isNull(ProductAutomationActionDAO.Column.COMPLETE.toString()));
			}
		}
		
		if (maxResults != null) {
			criteria.addOrder(ascending ? Order.asc(ProductAutomationActionDAO.Column.ASSIGNED.toString()) : Order.desc(ProductAutomationActionDAO.Column.ASSIGNED.toString()))
				.setMaxResults(maxResults);
		}
		
		// Set the projection to only give the ids.
		criteria.setProjection(Projections.id());
		
		criteria.setCacheable(true);

		return criteria;
	}
	
	/**
	 * Get a set of logs from the database that are refined by a set of
	 * arguments
	 * 
	 * @param host
	 *            the name of the host that generated the log
	 * @param lowerBound
	 *            the oldest time for the results returned
	 * @param upperBound
	 *            the latest time for the results returned
	 * @param logLevels
	 *            the trace severity for the log messages to be returned
	 * @param processId
	 *            the process that created the logs
	 * @param arbiter
	 *            true if arbiter log messages are to be included, false if not
	 * @param process
	 *            true if non-arbiter processor log messages are to be included,
	 *            false if not
	 * @param productMessages
	 *            true if product log messages are to be included, false if not
	 * @param maxResults
	 *            the maximum number of results to be returned
	 * @param ascending
	 *            true if oldest to newest, false if descending (newest to
	 *            oldest). If max results is set and true, the newest results
	 *            are trimmed. If max results is set and false, the oldest
	 *            results are trimmed
	 * @return A collection of ProductAutomationLog objects that conform to the
	 *         given criteria
	 */
	public Collection<ProductAutomationLog> getLogs(String host, Timestamp lowerBound, Timestamp upperBound, 
			Collection<String> logLevels, Long processId, boolean arbiter, boolean process, boolean productMessages, 
			Integer maxResults, boolean ascending) {
		return loadLogs(getLogsCriteria(host, 
				lowerBound, 
				upperBound, 
				arbiter, 
				process, 
				productMessages, 
				logLevels, 
				processId,
				maxResults, 
				ascending)
				.list());
	}

	
	private Criteria getLogsCriteria(String host, Timestamp lowerBound, Timestamp upperBound, 
			boolean arbiter, boolean process, boolean productMessages, Collection<String> logLevels,
			Long processId, Integer maxResults, boolean ascending) {
		Criteria criteria = getSession().createCriteria(ProductAutomationLog.class);
		
		if (host != null) {
			criteria.add(Restrictions.like(ProductAutomationLogsDAO.Column.HOST.toString(), String.format(QUERY_LIKE_STRING, host)));
		}
		
		if (processId != null) {
			criteria.add(Restrictions.eq(ProductAutomationLogsDAO.Column.PROCESSOR_ID.toString(), processId));
		}
		
		if (lowerBound != null) {
			criteria.add(Restrictions.ge(ProductAutomationLogsDAO.Column.EVENT_TIME.toString(), lowerBound));
		}

		if (upperBound != null) {
			criteria.add(Restrictions.lt(ProductAutomationLogsDAO.Column.EVENT_TIME.toString(), upperBound));
		}
		
		// No need to add any restrictions at all if all of the log types are true.
		if (! (arbiter && process && productMessages)) {
			// No reason to do anything if arbiter and process is set.
			if (arbiter && process) {
				// no need to filter on processor id.
			} else if (arbiter) {
				// Filter on processor id's that are equal to 0.
				criteria.add(Restrictions.eq(ProductAutomationLogsDAO.Column.PROCESSOR_ID.toString(), ARBITER_LOG_PROCESSID));
			} else if (process) {
				// Filter on processorId's that are greater than 0.
				criteria.add(Restrictions.gt(ProductAutomationLogsDAO.Column.PROCESSOR_ID.toString(), ARBITER_LOG_PROCESSID));
			} else {
				// do nothing.  Should not get here.
			}
			
			// If product messages is set, don't filter on anything.
			if (!productMessages) {
				// Only want messages that are equal to -1, which means they are not product messages.
				criteria.add(Restrictions.eq(ProductAutomationLogsDAO.Column.PRODUCT.toString(), NO_PRODUCT_LOG_PROCESSID));
			}
		}
		
		if (logLevels != null) {
			criteria.add(Restrictions.in(ProductAutomationLogsDAO.Column.LEVEL.toString(), logLevels));
		}
		
		if (maxResults != null) {
			criteria.addOrder(ascending ? Order.asc(ProductAutomationLogsDAO.Column.LOG_ID.toString()) : Order.desc(ProductAutomationLogsDAO.Column.LOG_ID.toString()))
				.setMaxResults(maxResults);
		}
		
		criteria.setProjection(Projections.id());
		
		// set as querable.
		criteria.setCacheable(true);
		
		return criteria;
	}
	
	/**
	 * Get a set of processes entries from the database that are refined by a
	 * set of arguments
	 * 
	 * @param host
	 *            the name of the host that performed the process
	 * @param lowerBound
	 *            the oldest time that can be associated with a process
	 * @param upperBound
	 *            the most recent time that can be associated with a process
	 * @param actionNames
	 *            the action mnemonics that can be associated with the process
	 * @param running
	 *            true if the process is running, false if not
	 * @param maxResults
	 *            the maximum number of results to be returned
	 * @param ascending
	 *            true if oldest to newest, false if descending (newest to
	 *            oldest). If max results is set and true, the newest results
	 *            are trimmed. If max results is set and false, the oldest
	 *            results are trimmed
	 * @return a SortedSet of ProductAutomationProcess objects conforming to the
	 *         supplied arguments
	 */
	public SortedSet<ProductAutomationProcess> getProcesses(String host, Timestamp lowerBound, Timestamp upperBound, 
			Collection<String> actionNames, boolean running, Integer maxResults, boolean ascending) {
		return objectToProcessSortedSet(getProcessCriteria(host, 
				lowerBound, 
				upperBound, 
				actionNames,
				running,
				maxResults, 
				ascending)
				.list());
	}
	
	private Criteria getProcessCriteria(String host, Timestamp lowerBound, Timestamp upperBound, 
			Collection<String> actionNames, boolean running, Integer maxResults, boolean ascending) {
		Criteria criteria = getSession().createCriteria(ProductAutomationProcess.class)
				.createAlias(ProductAutomationProcessDAO.Column.ACTION.toString(), ProcessActionAlias.ALIAS.toString());
		
		if (actionNames != null) {
			criteria.add(Restrictions.in(ProcessActionAlias.ACTION.toString(), actionNames));
		}
		
		if (host != null) {
			criteria.add(Restrictions.like(ProductAutomationProcessDAO.Column.MACHINE.toString(), String.format(QUERY_LIKE_STRING, host)));
		}
		
		if (lowerBound != null) {
			criteria.add(Restrictions.ge(ProductAutomationProcessDAO.Column.START_TIME.toString(), lowerBound));
		}
		
		if (upperBound != null) {
			criteria.add(Restrictions.lt(ProductAutomationProcessDAO.Column.START_TIME.toString(), upperBound));
		}
		
		if (running) {
			criteria.add(Restrictions.isNull(ProductAutomationProcessDAO.Column.SD_TIME.toString()));
		}
		
		if (maxResults != null) {
			criteria.addOrder(ascending ? 
				Order.asc(ProductAutomationProcessDAO.Column.START_TIME.toString()) : Order.desc(ProductAutomationProcessDAO.Column.START_TIME.toString()))
				.setMaxResults(maxResults);
		}
		
		criteria.setCacheable(true);

		return criteria;
	}
	
	
	/**
	 *  - 2/23/2013 - Going a different direction for doing queries.  First, going to
	 * be using the 2LC.  In order to do this, have to use session.get instead of using criteria
	 * queries to actually get the objects needs.  SO, we still need to use criteria searches 
	 * still to find the updated rows in the databases, but will only be finding the identifiers.
	 * From there, will go to the cache by using gets.  Setting up methods to do this work 
	 * below.
	 * 
	 * The criteria query for the ids can still take a considerable amount of time, but 
	 * we will be using the query cache as well to speed this up.
	 */
	
	/**
	 * Gets the products from the cache if available or from the db if not in the cache.  
	 * Assumes you are passing in the object list from a criteria search.
	 * 
	 * @param A collection of logId integer values
	 * 
	 * @return A SortedSet of the logs identified, or an empty set if none are found.
	 */
	private Collection<ProductAutomationLog> loadLogs(Collection<?> ids) {
		SortedSet<ProductAutomationLog> logs = new TreeSet<ProductAutomationLog>();
		
		for (Long id : objectToLongSortedSet(ids)) {
			ProductAutomationLog log = (ProductAutomationLog) getSession().get(ProductAutomationLog.class, id);
			
			// Since we are getting the id from the db, this should never happen, but need to make sure.
			if (log != null) {
				logs.add(log);
			}
		}
		
		return logs;
	}
	
	/**
	 * Gets the products from the cache if available or from the db if not in
	 * the cache. Assumes you are passing in the object list from a criteria
	 * search.
	 * 
	 * @param ids
	 *            a collection of productId integer values
	 * 
	 * @return a SortedSet of the products identified, or an empty set if none
	 *         are found
	 */
	private Collection<ProductAutomationProduct> loadProducts(Collection<?> ids) {
		SortedSet<ProductAutomationProduct> products = new TreeSet<ProductAutomationProduct>();
		
		for (Long id : objectToLongSortedSet(ids)) {
			ProductAutomationProduct product = (ProductAutomationProduct) getSession().get(ProductAutomationProduct.class, id);
			
			// Since we are getting the id from the db, this should never happen, but need to make sure.
			if (product != null) {
				products.add(product);
			}
		}
		
		return products;
	}
	
	/**
	 * Gets the products from the cache if available or from the db if not in
	 * the cache.
	 * 
	 * @param ids
	 *            a collection of statusId integer values
	 * 
	 * @return a SortedSet of the statuses identified, or an empty set if none
	 *         are found
	 */
	private Collection<ProductAutomationStatus> loadStatuses(Collection<?> ids) {
		SortedSet<ProductAutomationStatus> statuses = new TreeSet<ProductAutomationStatus>();
		
		for (Long id : objectToLongSortedSet(ids)) {

			ProductAutomationStatus status = (ProductAutomationStatus) getSession().get(ProductAutomationStatus.class,id);
			
			// Since we are getting the id from the db, this should never happen, but need to make sure.
			if (status != null) {
				statuses.add(status);
			}
		}
		
		return statuses;
	}
	
	/**
	 * Gets the products from the cache if available or from the db if not in
	 * the cache.
	 * 
	 * @param ids
	 *            a collection of actionId integer values
	 * @return a SortedSet of the actions identified, or an emtpy set if none
	 *         are found
	 */
	private Collection<ProductAutomationAction> loadActions(Collection<?> ids) {
		SortedSet<ProductAutomationAction> actions = new TreeSet<ProductAutomationAction>();

		for (Long id : objectToLongSortedSet(ids)) {
			ProductAutomationAction action = (ProductAutomationAction) getSession().get(ProductAutomationAction.class, id);
			
			// Since we are getting the id from the db, this should never happen, but need to make sure.
			if (action != null) {
				actions.add(action);
			}
		}
		
		return actions;
	}
}
