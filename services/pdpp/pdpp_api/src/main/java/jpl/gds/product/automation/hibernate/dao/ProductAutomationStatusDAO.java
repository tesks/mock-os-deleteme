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

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.ApplicationContext;

import jpl.gds.product.automation.AutomationException;
import jpl.gds.product.automation.hibernate.AutomationSessionFactory;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;

/**
 * Product automation status table data accessor object.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 */
public class ProductAutomationStatusDAO extends AbstractAutomationDAO {
	private final ProductAutomationProductDAO productDao;
	
	public ProductAutomationStatusDAO(ApplicationContext appContext, AutomationSessionFactory sessionFactory) {
		super(appContext, sessionFactory);
		
		productDao = appContext.getBean(ProductAutomationProductDAO.class);
	}

	/**
	 * Status value enumeration.
	 * 
	 * MPCS-8182 - 08/09/16 - Changed access control to public.
	 *          Used by PDPP GUI in different package
	 */
	public enum Status{
		
		/**Indicates that the the associated product has not been categorized*/
		UNCATEGORIZED("uncategorized"),
		
		/** Indicates that the associated product has timed out */
		TIMEOUT("timedout"),
		
		/** Indicates that the associated product has been categorized */
		CATEGORIZED("categorized"),
		
		/** Indicates that the associated product has started processing */
		STARTED("started"),
		
		/** Indicates that the associated product has successfully completed processing */
		COMPLETED("completed"),
		
		/** Indicates that the associated product has failed processing */
		FAILED("failed"),
		
		/** Indicates that the associated product has been ignored */
		IGNORED("ignored"),
		
		/** Indicates that the associated product has been reassigned */
		REASSIGNED("reassigned"),
		
		/** Indicates that the associated product has an unknown completion status */
		UNKNOWN_COMPLETE("unknown_complete"),
		
		/** Indicates that the associated product has been completed previous to being given to PDPP */
		COMPLETE_PRE_PB("completed_pre");
		
		private String statusName;
		
		private Status(String statusName){
			this.statusName = statusName;
		}
		
		@Override
		public String toString(){
			return statusName;
		}
	}
	
	//queried column names as enum
	enum Column{
		
		STATUS_ID("statusId"),
		STATUS("statusName"),
		PRODUCT("product"),
		PASS_NUMBER("passNumber"),
		STATUS_TIME("statusTime");
		
		private String columnName;
		
		private Column(String columnName){
			this.columnName = columnName;
		}
		
		@Override
		public String toString(){
			return columnName;
		}
	}
	
	// Table constants
	private static final String STATUS_TABLE_OBJECT = "ProductAutomationStatus";
	
	// HQL Query 
	// Query for all uncat that only have uncat statuses and nothing else.
	private static final String CATEGORIZING_QUERY = "FROM " + STATUS_TABLE_OBJECT
			+ " as stat WHERE stat.statusName = '" + Status.UNCATEGORIZED + 
			"' AND (SELECT count(*) FROM " + STATUS_TABLE_OBJECT + " as compare WHERE compare.product = stat.product AND " +
			"compare.passNumber = stat.passNumber AND compare.statusName != '" + Status.UNCATEGORIZED + "') = 0";
	
	// Want all timedout that do not have the following associated with the pass and product: (completed, failed, ignored).
	// Needs get all 
	private static final String TIMEOUT_QUERY = String.format("FROM %s as stat WHERE stat.statusName = '%s' "
			+ "AND (SELECT count(*) FROM %s as compare WHERE compare.product = stat.product " +
			"AND compare.passNumber = stat.passNumber AND (" +
			"compare.statusName = '%s' " + // COMPLETED
			"OR compare.statusName = '%s' " + // FAILED
			"OR compare.statusName = '%s' " + // IGNORED
			"OR compare.statusName = '%s' " + // COMPLETED CHILL DOWN
			"OR compare.statusName = '%s' " + // UNKNOWN_COMPLETED
			"OR compare.statusName = '%s' AND compare.statusTime > stat.statusTime" + // A reassignment time after the timeout status. 
			")) = 0",
			STATUS_TABLE_OBJECT, Status.TIMEOUT, STATUS_TABLE_OBJECT, Status.COMPLETED, Status.FAILED, Status.IGNORED, Status.COMPLETE_PRE_PB,
			Status.UNKNOWN_COMPLETE, Status.REASSIGNED);
	
	// Adders - There are no updates, only adds.  These all add new statuses.
	/**
	 * This is the base method to add a row to the status table.  This will try to start a transaction
	 * if need be but will not call the commit or flush if there are issues.
	 * 
	 * @param product the ProductAutomationProduct for which a status row is being added
	 * 
	 * @param statusEnum the status to be stored
	 * 
	 * @param passNumber the pass number for this status
	 * 
	 * @throws AutomationException an error was encountered while storing the status
	 */
	protected void addStatus(ProductAutomationProduct product, Status statusEnum, Long passNumber) throws AutomationException {
		ProductAutomationStatus status = new ProductAutomationStatus(statusEnum.toString(), product, passNumber);
		startTransaction();
		try {
			getSession().save(status);
		} catch (Exception e) {
			throw new AutomationException("Could not add status object: " + e.getMessage(), e);
		}
	}
 
	/**
	 * The only time an actual product row should ever be physically added to
	 * the product table is in an initial add from chill_down or when a process
	 * has a child product that needs to be handled. This creates a product
	 * object in the database from if one is not already there. Will then create
	 * the proper status and add it to the status table, with the product as a
	 * reference.
	 * 
	 * NOTE: This does not commit the transaction. It will start a new
	 * transaction if one is not running at call time, but it is up to the
	 * caller to call the DAO.commit or rollback if an exception is thrown.
	 * 
	 * @param productPath
	 *            filepath of the product to be found or constructed
	 * @param parent
	 *            parent product of the product to be found or constructed
	 * @param fswBuildId
	 *            the flight software build ID of the dictionary to be used with
	 *            the found or constructed product
	 * @param dictVersion
	 *            the dictionary version to be utilized with the found or
	 *            constructed product
	 * @param fswDirectory
	 *            the directory where the dictionary is located
	 * @param sessionId
	 *            the session that is to be associated with the found or
	 *            constructed product
	 * @param sessionHost
	 *            the host that is to be associated with the found or
	 *            constructed product
	 * @param apid
	 *            the application process ID of the product to be found or
	 *            constructed
	 * @param vcid
	 *            the virtual channel ID of the product to be found or
	 *            constructed
	 * @param sclkCoarse
	 *            the coarse spacecraft clock value of the product to be found
	 *            or constructed
	 * @param sclkFine
	 *            the fine spacecraft clock value of the product to be found or
	 *            constructed
	 * @param isCompressed
	 *            the compressed flag value of the product to be found or
	 *            constructed
	 * 
	 * @param realTimeExtraction
	 *            1 if the product was processed by chill_down, 0 otherwise
	 * 
	 * @return The pass number for this set of statuses
	 * 
	 * @throws AutomationException
	 *             an error was encountered while storing the status or creating
	 *             the product
	 * 
	 * MPCS-8568 - 12/12/2016 - added fswDirectory
	 */
	public Long addUncategorized(String productPath, ProductAutomationProduct parent, Long fswBuildId, String dictVersion, String fswDirectory, Long sessionId,
			String sessionHost, Integer apid, Integer vcid, Long sclkCoarse, Long sclkFine, int isCompressed, int realTimeExtraction) throws AutomationException {
		/**
		 * MPCS-6468 -  8/2014 - Real time extraction flag is no longer used.  Also adding the
		 * is compressed flag so that all information required for categorizing required product processing
		 * is stored in the product table.
		 */
		ProductAutomationProduct product = productDao.addProduct(productPath, parent, fswBuildId, 
				dictVersion, fswDirectory, sessionId, sessionHost, apid, vcid, sclkCoarse, sclkFine, isCompressed, realTimeExtraction);
		
		// Get the next pass number
		Long passNumber = getNextPassNumber(product);
		addStatus(product, Status.UNCATEGORIZED, passNumber);
		
		// Return the pass number for this newly added product run.
		return passNumber;
	}
	
	// MPCS-4248 -  - 11/29/2012 - Adding methods to be used with the command line pdpp's.
	
	/**
	 * Get the largest pass number for a product.
	 * 
	 * @param product
	 *            the ProductAutomationProduct in question
	 * 
	 * @return the highest pass number for the supplied product
	 */
	public Long getLastPassNumber(ProductAutomationProduct product) {
		return (Long) getSession()
		.createCriteria(ProductAutomationStatus.class)
		.add(Restrictions.eq(Column.PRODUCT.toString(), product))
		.setProjection(Projections.max(Column.PASS_NUMBER.toString()))
		.uniqueResult();
	}
	
	/**
	 * Given the pass number checks to see if there is a completed status for
	 * this pass.
	 * 
	 * @param product
	 *            the ProductAutomationProduct in question
	 * 
	 * @param passNumber
	 *            the pass number in question
	 * 
	 * @return true if the pass has been completed, false if not
	 * 
	 */
	public boolean isPassCompleted(ProductAutomationProduct product, Long passNumber) {
		ProductAutomationStatus lastStatus = (ProductAutomationStatus) getSession().createCriteria(ProductAutomationStatus.class)
			.add(Restrictions.eq(Column.PRODUCT.toString(), product))
			.add(Restrictions.eq(Column.PASS_NUMBER.toString(), passNumber))
			.add(Restrictions.disjunction() // Must do an or on the status.
					.add(Restrictions.eq(Column.STATUS.toString(), Status.COMPLETED.toString()))
					.add(Restrictions.eq(Column.STATUS.toString(), Status.FAILED.toString()))
					.add(Restrictions.eq(Column.STATUS.toString(), Status.UNKNOWN_COMPLETE.toString()))
					.add(Restrictions.eq(Column.STATUS.toString(), Status.COMPLETE_PRE_PB.toString())))
			.uniqueResult();
		
		return lastStatus != null;
	}
	
	/**
	 * The command line tools need to add products, but they are not to be uncategorized.  Use this
	 * to add a product if it does not exist in the database as well as adding a categorized status.  
	 * 
	 * If productComplete is true will return the pass number of the current product pass and will not add a new status.  If it is false, 
	 * will add a new pass number to the product.
	 * 
	 * @param productPath
	 *            filepath of the product to be found or constructed
	 * @param parent
	 *            parent product of the product to be found or constructed
	 * @param fswBuildId
	 *            the flight software build ID of the dictionary to be used with
	 *            the found or constructed product
	 * @param dictVersion
	 *            the dictionary version to be utilized with the found or
	 *            constructed product
	 * @param fswDirectory
	 *            the directory where the dictionary is located
	 * @param sessionId
	 *            the session that is to be associated with the found or
	 *            constructed product
	 * @param sessionHost
	 *            the host that is to be associated with the found or
	 *            constructed product
	 * @param apid
	 *            the application process ID of the product to be found or
	 *            constructed
	 * @param vcid
	 *            the virtual channel ID of the product to be found or
	 *            constructed
	 * @param sclkCoarse
	 *            the coarse spacecraft clock value of the product to be found
	 *            or constructed
	 * @param sclkFine
	 *            the fine spacecraft clock value of the product to be found or
	 *            constructed
	 * @param isCompressed
	 *            the compressed flag value of the product to be found or
	 *            constructed
	 * @param realTimeExtraction 1 if the product was processed by chill_down, 0 if not
	 * 
	 * @return The pass number for this set of statuses
	 * 
	 * @throws AutomationException
	 *             an error was encountered while storing the status or creating
	 *             the product
	 *             
	 * MPCS-8568 - 12/12/2016 - added fswDirectory
	 */
	public Long addCategorized(String productPath, ProductAutomationProduct parent, Long fswBuildId, String dictVersion, String fswDirectory, Long sessionId,
			String sessionHost, Integer apid, Integer vcid, Long sclkCoarse, Long sclkFine, int isCompressed, int realTimeExtraction) throws AutomationException {
		
		ProductAutomationProduct product = productDao.addProduct(productPath, parent, fswBuildId, 
				dictVersion, fswDirectory, sessionId, sessionHost, apid, vcid, sclkCoarse, sclkFine, isCompressed, realTimeExtraction);
		
		Long passNumber = getLastPassNumber(product);
		
		if (passNumber == null) {
			// If the pass number is null, means product was never added.  So it should be 1.
			passNumber = new Long(1);
		} else if (isPassCompleted(product, passNumber)) {
			// The pass was completed.  Need to increment the pass for a new pass.  Also, add a status for this pass.
			passNumber++;
		} else {
			// The pass was not complete.  So all we need to do use use the last pass number we just got.  So this is a no-op.
		}

		addStatus(product, Status.CATEGORIZED, passNumber);
		return passNumber;
	}	
	
	/**
	 * Adds a categorized status for the given product on the given pass
	 * 
	 * @param product
	 *            the ProductAutomationProduct for this status record
	 * @param passNumber
	 *            the pass for this status record
	 * @throws AutomationException
	 *             an error was encountered while storing the status
	 */
	public void addCategorized(ProductAutomationProduct product, Long passNumber) throws AutomationException {
		addStatus(product, Status.CATEGORIZED, passNumber);
	}

	/**
	 * Adds a started status for the given product on the given pass
	 * 
	 * @param product
	 *            the ProductAutomationProduct for this status record
	 * @param passNumber
	 *            the pass for this status record
	 * @throws AutomationException
	 *             an error was encountered while storing the status
	 */
	public void addStarted(ProductAutomationProduct product, Long passNumber) throws AutomationException {
		addStatus(product, Status.STARTED, passNumber);
	}
	
	/**
	 * Adds a completed status for the given product on the given pass
	 * 
	 * @param product
	 *            the ProductAutomationProduct for this status record
	 * @param passNumber
	 *            the pass for this status record
	 * @throws AutomationException
	 *             an error was encountered while storing the status
	 */
	public void addCompleted(ProductAutomationProduct product, Long passNumber) throws AutomationException {
		addStatus(product, Status.COMPLETED, passNumber);
	}
	
	// MPCS-4371 -  - 12/4/2012 
	/**
	 * Adds a completed by something before automation status for the given product on the given
	 * pass
	 * 
	 * @param product
	 *            the ProductAutomationProduct for this status record
	 * @param passNumber
	 *            the pass for this status record
	 * @throws AutomationException
	 *             an error was encountered while storing the status
	 */
	public void addCompletedPrevious(ProductAutomationProduct product, Long passNumber) throws AutomationException {
		addStatus(product, Status.COMPLETE_PRE_PB, passNumber);
	}	
	
	/**
	 * Adds a failed status for the given product on the given pass
	 * 
	 * @param product
	 *            the ProductAutomationProduct for this status record
	 * @param passNumber
	 *            the pass for this status record
	 * @throws AutomationException
	 *             an error was encountered while storing the status
	 */
	public void addFailed(ProductAutomationProduct product, Long passNumber) throws AutomationException {
		addStatus(product, Status.FAILED, passNumber);
	}
	
	/**
	 * Adds an ignored status for the given product on the given pass
	 * 
	 * @param product
	 *            the ProductAutomationProduct for this status record
	 * @param passNumber
	 *            the pass for this status record
	 * @throws AutomationException
	 *             an error was encountered while storing the status
	 */
	public void addIgnored(ProductAutomationProduct product, Long passNumber) throws AutomationException {
		addStatus(product, Status.IGNORED, passNumber);
	}

	/**
	 * Adds a timed out status for the given product on the given pass
	 * 
	 * @param product
	 *            the ProductAutomationProduct for this status record
	 * @param passNumber
	 *            the pass for this status record
	 * @throws AutomationException
	 *             an error was encountered while storing the status
	 */
	public void addTimedOut(ProductAutomationProduct product, Long passNumber) throws AutomationException {
		addStatus(product, Status.TIMEOUT, passNumber);
	}
	
	/**
	 * Adds a reassigned status for the given product on the given pass
	 * 
	 * @param product
	 *            the ProductAutomationProduct for this status record
	 * @param passNumber
	 *            the pass for this status record
	 * @throws AutomationException
	 *             an error was encountered while storing the status
	 */
	public void addReassigned(ProductAutomationProduct product, Long passNumber) throws AutomationException {
		addStatus(product, Status.REASSIGNED, passNumber);
	}
	
	/**
	 * Adds an unknown completion status for the given product on the given pass
	 * 
	 * @param product
	 *            the ProductAutomationProduct for this status record
	 * @param passNumber
	 *            the pass for this status record
	 * @throws AutomationException
	 *             an error was encountered while storing the status
	 */
	public void addUnknownComplete(ProductAutomationProduct product, Long passNumber) throws AutomationException {
		addStatus(product, Status.UNKNOWN_COMPLETE, passNumber);
	}	
	// Getters
	/**
	 * Conversion method to convert from the hibernate query List of objects to
	 * an arrayList of status objects.
	 * 
	 * @param objects
	 *            hibernate query objects to be converted
	 * 
	 * @return a Collection of objects converted to ProductAutomationStatus
	 *         objects
	 */
	private Collection<ProductAutomationStatus> objectsToStatuses(List<?> objects) {
		Collection<ProductAutomationStatus> result = new TreeSet<ProductAutomationStatus>();
		
		for (Object obj : objects) {
			result.add((ProductAutomationStatus) obj);
		}
		
		return result;
	}
	
	/**
	 * Get the status associated with a unique status ID
	 * 
	 * @param statusId
	 *            the unique id of the status to be found
	 * 
	 * @return the ProductAutomationStatus object that is attached to the
	 *         specified status
	 */
	public ProductAutomationStatus getStatus(Long statusId) {
		return (ProductAutomationStatus) getSession().get(ProductAutomationStatus.class, statusId);
	}
	
	/**
	 * All statuses need to be grouped to know which iteration of the product
	 * the status is for. This will find the last uncategorized (added) status
	 * for the product to get the pass number. Will increment this and return.
	 * 
	 * If there are no statuses for the product, returns 1.
	 * 
	 * @param product
	 *            the ProductAutomationProduct that will be receiving a new or
	 *            updated pass number
	 * 
	 * @return a Long value greater than or equal to 1 indicating what pass
	 *         number is going to be performed
	 */
	protected Long getNextPassNumber(ProductAutomationProduct product) {
		Long lastPassNumber = getLastPassNumber(product);
		return lastPassNumber == null ? 1 : lastPassNumber + 1;
	}
	
	/**
	 * Returns a list for all the statuses of a certain type regardless of
	 * product and passnumber.
	 * 
	 * @param statusName
	 *            the status type being requested (eg: Uncategorized, Started,
	 *            Failed, etc.)
	 * 
	 * @return A collection of ProductAutomationStatus objects for ALL
	 *         occurances of the requested status, both previous and current
	 */
	public Collection<ProductAutomationStatus> getStatuses(String statusName) {
		return objectsToStatuses(getSession().createCriteria(ProductAutomationStatus.class)
				.add(Restrictions.eq(Column.STATUS.toString(), statusName)).list());
	}
	
	/**
	 * Returns a scroller for all the statuses of a certain type for a certain
	 * products. Orders the output by pass number.
	 * 
	 * @param statusName
	 *            a status name to be used to restrict the collection of
	 *            statuses returned (eg: Uncategorized, Started, Failed, etc.)
	 * 
	 * @param product
	 *            the ProductAutomationProduct that all returned status will
	 *            refer to
	 * 
	 * @throws RuntimeException
	 *             Indicates a problem either translating the criteria to SQL,
	 *             exeucting the SQL or processing the SQL results.
	 * 
	 * @return A collection of statuses with the given status name and in
	 *         regards to the specified product
	 * 
	 */
	public Collection<ProductAutomationStatus> getStatuses(String statusName, ProductAutomationProduct product) {
		return objectsToStatuses(getSession().createCriteria(ProductAutomationStatus.class)
				.add(Restrictions.eq(Column.STATUS.toString(), statusName))
				.add(Restrictions.eq(Column.PRODUCT.toString(), product))
				.addOrder(Order.desc(Column.PASS_NUMBER.toString())).list());
	}
	
	/**
	 * Finds all the status object for a certain product, regardless of the
	 * status and pass number.
	 * 
	 * @param product
	 *            the ProductAutomationProduct that all status are being
	 *            requested
	 * 
	 * @throws RuntimeException
	 *             Indicates a problem either translating the criteria to SQL,
	 *             exeucting the SQL or processing the SQL results.
	 * 
	 * @return A collection of all statuses for a specific product
	 */
	public Collection<ProductAutomationStatus> getStatuses(ProductAutomationProduct product) {
		return objectsToStatuses(getSession().createCriteria(ProductAutomationStatus.class)
				.add(Restrictions.eq(Column.PRODUCT.toString(), product)).list());
	}
	
	/**
	 * This is the method used by the categorizer. It will find all the
	 * uncategorized status objects such that there is not other status object
	 * with the type "categorized" with the same increment number.
	 * 
	 * @param maxResults
	 *            the maximum number of ProductAutomationStatus objects to be in
	 *            the returned results
	 * 
	 * @throws RuntimeException
	 *             Indicates a problem either translating the criteria to SQL,
	 *             exeucting the SQL or processing the SQL results.
	 * 
	 * @return A collection of ProductAutomationStatus objects that are of the
	 *         status "Uncategorized" and are the only status object referring
	 *         to a product
	 */
	public Collection<ProductAutomationStatus> getStatusesForCategorizing(int maxResults) {
		return objectsToStatuses(getSession().createQuery(CATEGORIZING_QUERY)
				.setMaxResults(maxResults)
				.list());
	}

	/**
	 * Finds all of the current timed out status objects. These refer to a
	 * product that is being worked on, but the time that has been given to this
	 * work is greater than the amount of time that it is allowed.
	 * 
	 * @param maxResults
	 *            the maxmimum number of ProductAutomationStatusObjects to be in
	 *            the returned data
	 * 
	 * @return A collection of ProductAutomationStatus objects that are of the
	 *         status "timedout" and are the most recent status for a product
	 */
	public Collection<ProductAutomationStatus> getTimeOutStatuses(int maxResults) {
		return objectsToStatuses(getSession().createQuery(TIMEOUT_QUERY)
				.setMaxResults(maxResults)
				.list());
	}
	
	// MPCS-4248 - Need a simple way to check if a product was worked on and has a status that means it was complete. (completed, failed, completed_cd, unknown_complete)
	/**
	 * Get if a product has completed processing. Processing may have completed,
	 * but failed.
	 * 
	 * @param productPath
	 *            the file path of the product in question
	 * 
	 * @return True if processing has been completed for the product, false if
	 *         not.
	 */
	public boolean isProductCompleted(String productPath) {
		return isProductCompleted(productDao.getProduct(productPath));
	}
	
	/**
	 * Get if a product has completed processing. Processing may have completed, but failed.
	 * 
	 * @param product the ProductAutomationProduct object for the product in question
	 * 
	 * @return True if processing has been completed for the product, false if not.
	 */
	public boolean isProductCompleted(ProductAutomationProduct product) {
		Long completedStatuses = (Long) getSession().createCriteria(ProductAutomationStatus.class)
			.add(Restrictions.eq(Column.PRODUCT.toString(), product))
			.add(Restrictions.disjunction() // Must do an or on the status.
					.add(Restrictions.eq(Column.STATUS.toString(), Status.COMPLETED.toString()))
					.add(Restrictions.eq(Column.STATUS.toString(), Status.FAILED.toString()))
					.add(Restrictions.eq(Column.STATUS.toString(), Status.UNKNOWN_COMPLETE.toString()))
					.add(Restrictions.eq(Column.STATUS.toString(), Status.COMPLETE_PRE_PB.toString())))
			.setProjection(Projections.rowCount())
			.uniqueResult();
			
		return completedStatuses > 0;
	}
}
