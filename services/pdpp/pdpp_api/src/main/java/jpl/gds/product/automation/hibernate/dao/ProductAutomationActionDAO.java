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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.ApplicationContext;

import jpl.gds.product.automation.AutomationException;
import jpl.gds.product.automation.ProductAutomationProperties;
import jpl.gds.product.automation.hibernate.AutomationSessionFactory;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationClassMap;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;

/**
 * Product automation action table data accessor object.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 */
public class ProductAutomationActionDAO extends AbstractAutomationDAO {
	// These are used only for quering the assigned actions by a process so that it does not get hung up working, and the 
	// actions that are unclaimed end up getting timed out.
	private final int maxQueryResults;
	private final ProductAutomationProcessDAO processDao;
	
	public ProductAutomationActionDAO(ApplicationContext appContext, AutomationSessionFactory sessionFactory) {
		super(appContext, sessionFactory);
		maxQueryResults = appContext.getBean(ProductAutomationProperties.class).getMaxQueryResults();
		processDao = appContext.getBean(ProductAutomationProcessDAO.class);
	}

	//queried column names as enum
	enum Column{
		ACTION_ID("actionId"),
		ACTION_NAME("actionName"),
		PROCESS("process"),
		PRODUCT("product"),
		PASS_NUMBER("passNumber"),
		ASSIGNED("assignedTime"),
		ACCEPT("acceptedTime"),
		COMPLETE("completedTime"),
		REASSIGN("reassign"),
		VERSION_ID("versionId");
		
		private String columnName;
		
		private Column(String columnName){
			this.columnName = columnName;
		}
		
		@Override
		public String toString(){
			return columnName;
		}
	}
	
	// Adders - setters 
	
	
	/**
	 * Given an action this will call save for the session.
	 * 
	 * @param action
	 *            the ProductAutomationAction to be saved
	 * 
	 * @throws AutomationException
	 *             when an error is encountered while saving the action
	 * 
	 * @see org.hibernate.Session#save()
	 */
	public void addAction(ProductAutomationAction action) throws AutomationException {
		try {
			getSession().save(action);
		} catch (Exception e) {
			trace.error("Failed to add action: " + action);
			throw new AutomationException("Could not add new action: " + action.toString(), e);
		}
	}
	
	/**
	 * Assembles an action with the provided arguments and calls save for the
	 * session
	 * 
	 * @param actionName
	 *            name of the action
	 * @param process
	 *            the process for the action
	 * @param status
	 *            the status of the action
	 * 
	 * @throws AutomationException
	 *             when an error is encountered while saving the action
	 */
	public void addAction(ProductAutomationClassMap actionName, ProductAutomationProcess process, ProductAutomationStatus status) throws AutomationException {
		ProductAutomationAction action = new ProductAutomationAction();
		action.setProcess(process);
		action.setActionName(actionName);
		action.setProduct(status.getProduct());
		action.setPassNumber(status.getPassNumber());
		
		addAction(action);
	}
	
	/**
	 * MPCS-6469 -  8/2014 - Ading an action creator that does not assign to a process.
	 */
	
	/**
	 * Creates an action for a product but does not assign it to a process.
	 * 
	 * @param actionName
	 *            name of the action
	 * @param status
	 *            the status of the action
	 * 
	 * @throws AutomationException
	 *             when an error is encountered while saving the action
	 */
	public void addAction(ProductAutomationClassMap actionName, ProductAutomationStatus status) throws AutomationException {
		ProductAutomationAction action = new ProductAutomationAction();
		action.setProcess(null);
		action.setActionName(actionName);
		action.setProduct(status.getProduct());
		action.setPassNumber(status.getPassNumber());
		
		addAction(action);
	}
	
	// Updaters
	/**
	 * Either {@link #save(Object)} or {@link #update(Object)} the given
	 * instance, depending upon resolution of the unsaved-value checks (see the
	 * manual for discussion of unsaved-value checking).
	 * <p/>
	 * This operation cascades to associated instances if the association is
	 * mapped with {@code cascade="save-update"}
	 *
	 * @param action
	 *            a transient or detached instance containing new or updated
	 *            state
	 * 
	 * @throws AutomationException
	 *             when an error is encountered while updating the action
	 *
	 * @see Session#save(java.lang.Object)
	 * @see Session#update(Object object)
	 */
	public void updateAction(ProductAutomationAction action) throws AutomationException {
		try {
			getSession().saveOrUpdate(action);
		} catch (Exception e) {
			throw new AutomationException("Could not update the action: " + action.toString(), e);
		}
	}
	
	/**
	 * Sets claimed time of the action to the current time.
	 * 
	 * @param action
	 *            the action having the claimed time set
	 * 
	 * @throws AutomationException
	 *             when an error is encountered while updating the action
	 */
	public void setClaimedTime(ProductAutomationAction action) throws AutomationException {
		action.setAcceptedTime(new Timestamp(System.currentTimeMillis()));
		updateAction(action);
	}
	
	/**
	 * If a process exits because the action it is performing is disabled, the
	 * process needs to unclaim all the actions it claimed before it kills
	 * itself. This will set the claimed time to null.
	 * 
	 * Also sets the reassign flag which tells the assigner that this action
	 * needs to be reassigned.
	 * 
	 * @param action
	 *            the action to have its claimed status removed
	 * 
	 * @throws AutomationException
	 *             when an error is encountered while updating the action
	 * 
	 * @version MPCS-4281 -  - added
	 * @version MPCS-6544 -  - Also sets the reassign flag
	 */
	public void unclaimAction(ProductAutomationAction action) throws AutomationException {
		action.setAcceptedTime(null);
		action.setProcess(null);
		action.setReassign(1);
		updateAction(action);
	}
	
	/**
	 * Sets completion time of the action to the current time.
	 * 
	 * @param action
	 *            the action to have its completion time set
	 * 
	 * @throws AutomationException
	 *             when an error is encountered while updating the action
	 */
	public void setCompletedTime(ProductAutomationAction action) throws AutomationException {
		action.setCompletedTime(new Timestamp(System.currentTimeMillis()));
		updateAction(action);
	}

	/**
	 * If a timeout happens and a process is taking too long to finish an
	 * action, the arbiter will reassign it to a different process. This will
	 * update the action with the new process. It also makes sure that the
	 * acceptedTime and completedTime are set back to null.
	 * 
	 * @param action
	 *            the action to be reassigned
	 * 
	 * @param newProcess
	 *            the process that is receiving the action
	 * 
	 * @throws AutomationException
	 *             when an error is encountered while updating the action
	 */
	public void reassign(ProductAutomationAction action, ProductAutomationProcess newProcess) throws AutomationException {
		action.setProcess(newProcess);
		action.setAssignedTime(new Timestamp(System.currentTimeMillis()));
		action.setAcceptedTime(null);
		action.setCompletedTime(null);
		updateAction(action);
	}
	
	// Getters
	
	/**
	 * Hibernate query lists return a list of objects, and it is annoying to
	 * have to deal with it every time. This will cast the objects into an array
	 * list of action objects and return a new list. If the input is null or
	 * empty it will return an empty arraylist.
	 * 
	 * @param objectList
	 *            the List of objects to be casted
	 * 
	 * @return the objectList casted as ProductAutomationAction objects and
	 *         stored in a Collection
	 */
	/* package */ Collection<ProductAutomationAction> convertList(List<?> objectList) {
		Collection<ProductAutomationAction> result = new ArrayList<ProductAutomationAction>();
		
		// Just in case
		if (objectList != null) {
			for (Object obj : objectList) {
				result.add((ProductAutomationAction) obj);
			}
		}
		
		return result;
	}
	
	/**
	 * Returns all actions found for a given product.
	 * 
	 * @param product
	 *            the product being queried
	 * 
	 * @return a Collection of actions associated with the given product
	 */
	public Collection<ProductAutomationAction> getActions(ProductAutomationProduct product) {
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.eq(Column.PRODUCT.toString(), product))
				.list()
				);
	}
	
	/**
	 * Gets the action for pass number 1.  A product should only ever have one type of action so get
	 * the first if there are any.
	 * 
	 * @param product the product being queried
	 * 
	 * @return a Collection of actions associated with the given product
	 */
	public ProductAutomationAction getAction(ProductAutomationProduct product) {
		return (ProductAutomationAction) getSession().createCriteria(ProductAutomationAction.class)
		.add(Restrictions.eq(Column.PRODUCT.toString(), product))
		.add(Restrictions.eq(Column.PASS_NUMBER.toString(), 1L))
		.uniqueResult();
	}
	
	/**
	 * Finds all the actions assigned to the given process that are still
	 * unclaimed. This means that the completed and accepted times are set to
	 * null.
	 * 
	 * @param process
	 *            Process object for which to get actions.
	 * 
	 * @return a Collection of actions that are assigned to a specific process
	 *         and are unclaimed
	 */
	public Collection<ProductAutomationAction> getUnclaimedAssignedActions(ProductAutomationProcess process) {
//		return convertList(getSession().createCriteria(ProductAutomationAction.class)
		/**
		 * MPCS-6544 -  9/2014 - Only getting unclaimed actions again.
		 * We were updating the claimed time because of an issue with timing out actions.
		 * We are no longer doing that the same way, so it is not necessary to keep doing this.
		 * This will also fix some concurrancy issues we are having due to continuously updating
		 * the action and the version.
		 */
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.eq(Column.PROCESS.toString(), process))
				.add(Restrictions.isNull(Column.COMPLETE.toString()))
				.add(Restrictions.isNull(Column.ACCEPT.toString()))
				.list());
	}

	/**
	 * Finds up to the maxReults number of actions assigned to the given process
	 * that are still unclaimed. This means that the completed and accepted
	 * times are set to null.
	 * 
	 * @param process
	 *            Process object for which to get actions.
	 * @param maxResults
	 *            Max actions to get.
	 * @return a Collection of actions that are assigned to a specific process
	 *         and are unclaimed
	 */
	public Collection<ProductAutomationAction> getUnclaimedAssignedActions(ProductAutomationProcess process, Integer maxResults) {
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.isNull(Column.COMPLETE.toString()))
				.add(Restrictions.isNull(Column.ACCEPT.toString()))
				.add(Restrictions.eq(Column.PROCESS.toString(), process))
				.addOrder(Order.asc(Column.ASSIGNED.toString()))
				.setMaxResults(maxResults)
				.list());
	}
	
	
	/**
	 * MPCS-4246 -  - added an order by of ascending order based on the product sessionId and the assigned time.  
	 * Also adding a configuration item that will set the max query results.
	 * This is needed because if there are many actions assigned, and you get all, and more are assigned, the new ones could go unclaimed and timeout while the process
	 * is working on the working list.
	 * 
	 * This will use the MAX_QUERY_RESULTS that is set in the system config to set the query results count.
	 */
	
	/**
	 * Find all the actions that were claimed and owned by the given process.
	 * means that init and accept are not null, and completed is null.
	 * 
	 * Uses MAX_CLAIMED_RESULTS so that the commits to the database are made
	 * faster.
	 * 
	 * @param process
	 *            Process object for which to get actions.
	 * 
	 * @return a Collection of actions that are assigned to a specific process
	 *         and have been claimed
	 */
	public Collection<ProductAutomationAction> getClaimedAssignedActions(ProductAutomationProcess process) {
		return getClaimedAssignedActions(process, maxQueryResults);
	}
	
	/**
	 * Gets the claimed actions for a given process and limits the result to
	 * maxResults. Orders the results by session id and assigned time.
	 * 
	 * @param process
	 *            Process object for which to get actions.
	 * @param maxResults
	 *            Max actions to get
	 * @return a Collection of actions that are assigned to a specific process
	 *         and have been claimed
	 */
	public Collection<ProductAutomationAction> getClaimedAssignedActions(ProductAutomationProcess process, Integer maxResults) {
		// MPCS-8180 07/19/16 - Changed alias to use Column.PRODUCT instead of TableNames.PRODUCT to fix call
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.isNotNull(Column.ACCEPT.toString()))
				.add(Restrictions.isNull(Column.COMPLETE.toString()))
				.add(Restrictions.eq(Column.PROCESS.toString(), process))
				.createAlias(Column.PRODUCT.toString(), "prod")
				.addOrder(Order.asc("prod." + ProductAutomationProductDAO.Column.SESSION_ID.toString()))
				.addOrder(Order.asc(Column.ASSIGNED.toString()))
				.setMaxResults(maxResults)
				.list());
	}
	
	/**
	 * Gets all claimed actions for a given process. Orders the results by
	 * session id and assigned time.
	 * 
	 * @param process
	 *            ProductAutomationProcess object for which to get actions.
	 * @return a Collection of actions that are assigned to a specific process
	 *         and have been claimed
	 */
	public Collection<ProductAutomationAction> getAllClaimedAssignedActions(ProductAutomationProcess process) {
		// MPCS-8180 07/19/16 - Changed alias to use Column.PRODUCT instead of TableNames.PRODUCT to fix call
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.eq(Column.PROCESS.toString(), process))
				.add(Restrictions.isNotNull(Column.ACCEPT.toString()))
				.add(Restrictions.isNull(Column.COMPLETE.toString()))
				.createAlias(Column.PRODUCT.toString(), "prod")
				.addOrder(Order.asc("prod." + ProductAutomationProductDAO.Column.SESSION_ID.toString()))
				.addOrder(Order.asc(Column.ACCEPT.toString()))
				.list());
	}

	
	/**
	 * Gets the the most recent actions assigned to a process. The process is
	 * specified by its processId and the number of results is limited to a
	 * requested number. This is used for finding actions that need to be
	 * reassigned due to heavy loads.
	 * 
	 * @param processId
	 *            the processId of the ProductAutomationProcess in question
	 * @param count
	 *            the maximum number of actions to be returned
	 * @return a Collection of actions that have been most recently assigned to
	 *         a process
	 */
	public Collection<ProductAutomationAction> getMostRecentAssignedActions(Long processId, Integer count) {
		return this.getMostRecentAssignedActions(processDao.findProcess(processId), count);
	}

	
	/**
	 * Finds count of the most recently assigned actions for a given process.
	 * This is used for finding actions that need to be reassigned due to heavy
	 * loads.
	 * 
	 * @param process
	 *            the ProductAutomationProcess object in question
	 * @param count
	 *            the maximum number of actions to be returned
	 * @return a Collection of actions that have been most recently assigned to
	 *         a process
	 */
	public Collection<ProductAutomationAction> getMostRecentAssignedActions(ProductAutomationProcess process, Integer count) {
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.eq(Column.PROCESS.toString(), process))
				.add(Restrictions.isNotNull(Column.ACCEPT.toString()))
				.add(Restrictions.isNull(Column.COMPLETE.toString()))
				.addOrder(Order.desc(Column.ASSIGNED.toString()))
				.setMaxResults(count)
				.list());	
		}
	
	/**
	 * Finds all of the assigned actions for a given process.
	 * 
	 * @param process
	 *            the ProductAutomationProcess object in question
	 * 
	 * @return a Collection of all actions that have been assigned to a process
	 */
	public Collection<ProductAutomationAction> getAllAssignedActions(ProductAutomationProcess process) {
		// MPCS-8180 07/19/16 - Changed alias to use Column.PRODUCT instead of TableNames.PRODUCT to fix call
		/**
		 * MPCS-8442  11/16/2016 - Adding more sorting to sort by action id, which would in effect sort 
		 * by the time the arbiter processed the input uncategorized status.  
		 */
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.isNull(Column.COMPLETE.toString()))
				.add(Restrictions.eq(Column.PROCESS.toString(), process))
				.createAlias(Column.PRODUCT.toString(), "prod")
				.addOrder(Order.asc("prod." + ProductAutomationProductDAO.Column.SESSION_ID.toString()))
				.addOrder(Order.asc(Column.ACTION_ID.toString()))
				.list());
	}
	
	/**
	 * Fins all of the assigned actions for a given process.
	 * 
	 * @param processId
	 *            the ID of the process in question
	 * 
	 * @return a Collection of all actions that have been assigned to a process
	 */
	public Collection<ProductAutomationAction> getAllAssignedActions(Long processId) {
		return getAllAssignedActions(processDao.findProcess(processId));
	}

	/**
	 * MPCS-6544 -  9/2014 - Added method with query limits.
	 */
	
	/**
	 * Finds up to the configuration set maximum number of assigned actions for
	 * a given process.
	 * 
	 * @param process
	 *            the ProductAutomationProcess object in question
	 * @return a Collection of actions that have been assigned to a process
	 */
	public Collection<ProductAutomationAction> getAllAssignedActionsWithLimit(ProductAutomationProcess process) {
		// MPCS-8180 07/19/16 - Changed alias to use Column.PRODUCT instead of TableNames.PRODUCT to fix call
		/**
		 * MPCS-8442  11/16/2016 - Adding more sorting to sort by action id, which would in effect sort 
		 * by the time the arbiter processed the input uncategorized status.  
		 */
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.isNull(Column.COMPLETE.toString()))
				.add(Restrictions.eq(Column.PROCESS.toString(), process))
				.createAlias(Column.PRODUCT.toString(), "prod")
				.addOrder(Order.asc("prod." + ProductAutomationProductDAO.Column.SESSION_ID.toString()))
				.addOrder(Order.asc(Column.ACTION_ID.toString()))
				.setMaxResults(maxQueryResults)
				.list());
	}
	
	/**
	 * Gets all claimed actions for a given processId. Orders the results by
	 * session id and assigned time.
	 * 
	 * @param processId
	 *            the processId of the ProductAutomationProcess object being queried.
	 * @return a Collection of actions that are assigned to a specific process
	 *         and have been claimed
	 */
	public Collection<ProductAutomationAction> getAllClaimedAssignedActions(Long processId) {
		return getAllClaimedAssignedActions(processDao.findProcess(processId));
	}

	/**
	 * This will find all the actions that have been assigned but are not
	 * completed.
	 * 
	 * @return a Collection of all claimed, but not completed, actions for all
	 *         processes.
	 */
	public Collection<ProductAutomationAction> getAllClaimedActions() {
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.isNotNull(Column.ACCEPT.toString()))
				.add(Restrictions.isNull(Column.COMPLETE.toString()))
				.list());
	}
	
	/**
	 * Finds all the actions that are assigned but they have not yet been
	 * claimed by the process.
	 * 
	 * @return a Collection of all actions that have been assigned to a process,
	 *         but have not been claimed
	 */
	public Collection<ProductAutomationAction> getAllUnclaimedAssignedActions() {
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.isNull(Column.ACCEPT.toString()))
				.add(Restrictions.isNull(Column.COMPLETE.toString()))
				.list());
	}
	
	/**
	 * Finds all actions that are incomplete and where the process they are
	 * assigned to is no longer running.
	 * 
	 * @return a Collection of all stranded (incomplete and assigned to a no
	 *         longer valid process) actions
	 * 
	 * @version MPCS-7238
	 */
	public Collection<ProductAutomationAction> getStrandedActions() {
		return convertList(getSession().createCriteria(ProductAutomationAction.class, TableNames.ACTION.toString())
				.createAlias(TableNames.ACTION.toString() + "." + Column.PROCESS.toString(), TableNames.PROCESS.toString())
				.add(Restrictions.isNull(Column.COMPLETE.toString()))
				.add(Restrictions.isNotNull(Column.PROCESS.toString()))
				.add(Restrictions.disjunction()
					.add(Restrictions.isNotNull(TableNames.PROCESS.toString() + "." + ProductAutomationProcessDAO.Column.KILLER.toString()))
					.add(Restrictions.isNotNull(TableNames.PROCESS.toString() + "." + ProductAutomationProcessDAO.Column.SD_TIME.toString()))
				).list());
	}

	/**
	 * Returns a list of actions where the complete time is NULL. This will also
	 * check for all actions that have have timeout statuses pending, ie, the
	 * last status for the product pass number pair is a timedout status.
	 * 
	 * @param maxResults
	 *            the maximum number of results to be returned
	 * 
	 * @return a list of actions where the complete time is NULL
	 */
	public Collection<ProductAutomationAction> getAllIncompleteActions(int maxResults) {
		String query = "from ProductAutomationAction as act where act." + Column.COMPLETE + " is null " +
				"and (select stat." + ProductAutomationStatusDAO.Column.STATUS + " from ProductAutomationStatus as stat " +
				"where stat." + ProductAutomationStatusDAO.Column.PRODUCT + " = act." + Column.PRODUCT + " " +
				"and stat." + ProductAutomationStatusDAO.Column.PASS_NUMBER + " = act." + Column.PASS_NUMBER + " " +
				"and stat." + ProductAutomationStatusDAO.Column.STATUS_ID + " = " +
				"(select max(" + ProductAutomationStatusDAO.Column.STATUS_ID + ") from ProductAutomationStatus as am where " +
				"am." + ProductAutomationStatusDAO.Column.PRODUCT + " = stat." + ProductAutomationStatusDAO.Column.PRODUCT + " " +
				"and am." + ProductAutomationStatusDAO.Column.PASS_NUMBER + " = stat." + ProductAutomationStatusDAO.Column.PASS_NUMBER + ")) " +
				"!= '" + ProductAutomationStatusDAO.Status.TIMEOUT + "'";
		
		return convertList(getSession().createQuery(query)
				.setMaxResults(maxResults)
				.list());
	}	
	
	/**
	 * Given the product and pass number, will return the action associated with
	 * those.
	 * 
	 * @param product
	 *            the ProductAutomationProduct object being queried
	 * 
	 * @param passNumber
	 *            the number of passes that have been performed on the product
	 * 
	 * @return the ProductAutomationAction object associated with the
	 *         ProductAutomationProduct at a specific pass
	 */
	public ProductAutomationAction findAction(ProductAutomationProduct product, Long passNumber) {
		List<?> found = getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.eq(Column.PRODUCT.toString(), product))
				.add(Restrictions.eq(Column.PASS_NUMBER.toString(), passNumber))
				.list();
		
		return (ProductAutomationAction) (found.isEmpty() ? null : found.get(0));
	}

	/**
	 * MPCS-6544 -  9/2014 -  Adding methods to get some statistics about processes.  This will be used 
	 * to spread the work load among parallell processes.
	 */

	/**
	 * Finds a list of actions that have not been assigned to any process up
	 * to a specified number of actions.
	 * 
	 * @param maxResults the maximum number of results
	 * 
	 * @return a Collection of actions that have not been assigned to a process
	 */
	public Collection<ProductAutomationAction> getUnassignedActions(int maxResults) {
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.isNull(Column.PROCESS.toString()))
				.addOrder(Order.asc(Column.ACTION_ID.toString()))
				.setMaxResults(maxResults)
				.list()
				);
	}

	/**
	 * MPCS-6544 -  9/2014 - Added method to find actions marked for reassign.
	 */
	
	
	/**
	 * Finds a list of actions that have not been assigned to any process, up to
	 * a specified number, for a given class map.
	 * 
	 * @param actionName
	 *            the specific ProductAutomationClassMap item for all of the
	 *            actions to be returned
	 * 
	 * @param maxResults
	 *            the maximum number of results
	 * 
	 * @return a Collection of actions that are unassigned and belong to a certain
	 *         classmap
	 */
	public Collection<ProductAutomationAction> getUnassignedActions(ProductAutomationClassMap actionName, int maxResults) {
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.eq(Column.ACTION_NAME.toString(), actionName))
				.add(Restrictions.isNull(Column.PROCESS.toString()))
				.addOrder(Order.asc(Column.ACTION_ID.toString()))
				.setMaxResults(maxResults)
				.list()
				);
	}
	
	// MPCS-6970 - added a method to get unassigned actions where the action id is not in the given collection.
	
	/**
	 * Finds a list of actions that have not been assigned to any process, up to
	 * a specified number, for a given class map. The results excludes any actions that are associated with a specified list of actionIds.
	 * 
	 * @param actionName the specific ProductAutomationClassMap item for all of the
	 *            actions to be returned  
	 * 
	 * @param maxResults the maximum number of results
	 * 
	 * @param notInSet a Collection of actionIds that are not desired in the results
	 *   
	 * @return a Collection of actions that are unassigned, belong to a certain classmap, and are not associated with certain actionIds
	 */
	public Collection<ProductAutomationAction> getUnassignedActions(ProductAutomationClassMap actionName, int maxResults, Collection<Long> notInSet) {
		Criteria crit = getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.eq(Column.ACTION_NAME.toString(), actionName))
				.add(Restrictions.isNull(Column.PROCESS.toString()))
				.addOrder(Order.asc(Column.ACTION_ID.toString()))
				.setMaxResults(maxResults);
		
		if (!notInSet.isEmpty()) {
			crit.add(Restrictions.not(Restrictions.in(Column.ACTION_ID.toString(), notInSet)));
		}
		
		return convertList(crit.list());
	}
	
	/**
	 * Finds a list of actions, up to a specified number, that have their reassignment flag set. 
	 * 
	 * @param actionName the specific ProductAutomationClassMap item for all of the
	 *            actions to be returned 
	 * @param maxResults the maximum number of results
	 * 
	 * @return a Collection of actions that have been flagged for reassignment
	 */
	public Collection<ProductAutomationAction> getActionsMarkedForReassign(ProductAutomationClassMap actionName, int maxResults) {
		return convertList(getSession().createCriteria(ProductAutomationAction.class)
				.add(Restrictions.eq(Column.ACTION_NAME.toString(), actionName))
				.add(Restrictions.eq(Column.REASSIGN.toString(), 1))
				.addOrder(Order.asc(Column.ACTION_ID.toString()))
				.setMaxResults(maxResults)
				.list()
				);
	}
}
