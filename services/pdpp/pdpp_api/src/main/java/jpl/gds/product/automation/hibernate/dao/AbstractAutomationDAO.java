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

import jpl.gds.product.automation.ProductAutomationProperties;
import jpl.gds.product.automation.hibernate.AutomationSessionFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Abstract DAO (data accessor object) for all DAO classes.  These are assumed to be singletons.  This has
 * methods to get the session and roll back and the like.
 * 
 *
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 */

public class AbstractAutomationDAO {

	/**
	 * trace manager for DAO classes. Publishes on the ProductAutomationTracer
	 */
	protected final Tracer trace; 


	private static int RUN_TIME_MIN_CONNECTION_VALUE = -1;
	private static final String CONNECTION_CHANGE_QUERY = "set global max_connections = (select @@global.max_connections) + %d";
	private static final String CURRENT_CONNECTIONS_QUERY = "show variables like 'max_connections'";
	
	// MPCS-8179 06/07/16 - moved property names to ProductAutomationConfig
	private final int connectionChangeValue;
	private final int minMaxConnectionValue;
	private final int maxMaxConnectionValue;
	private final boolean doAdjustments;
	
	protected final ApplicationContext appContext;
	protected final AutomationSessionFactory sessionFactory;
	
	public AbstractAutomationDAO(final ApplicationContext appContext, final AutomationSessionFactory sessionFactory) {
		this.appContext = appContext;
        this.trace = TraceManager.getTracer(appContext, Loggers.PDPP);
		this.sessionFactory = sessionFactory;
    	final ProductAutomationProperties config = appContext.getBean(ProductAutomationProperties.class);
		
		// 11/21/2012 - MPCS-4387 - Property values used to manage the number of db connections.  Set the values.
		// If the values are not set, then it will use the same number of MIN and MAX and should not change anything when the methods are called.
		connectionChangeValue = config.getConnectionChanges();
		minMaxConnectionValue = config.getMinMaxConnections();
		maxMaxConnectionValue = config.getMaxMaxConnections();
		// If there is an issue where the config items are not set, then set adustments to false. 
		final boolean allSet = connectionChangeValue > 0 && minMaxConnectionValue > 0 && maxMaxConnectionValue > 0;
		
		if (!allSet) {
			trace.warn("Configuration values for connection throttling are not set.  Adjustments will be turned off.");
		}
		
		doAdjustments = allSet ? config.getDoAdjustments() : false;

	}
	
	
	enum TableNames{
		
		ACTION("action"),
		CLASSMAP("classmaps"),
		LOGS("logs"),
		PROCESS("process"),
		PRODUCT("products"),
		STATS("stats"),
		STATUS("status");
		
		
		private String name;
		
		private TableNames(final String name){
			this.name = name;
		}
		
		@Override
		public String toString(){
			return name;
		}
	}
	
	/**
	 * Hibernate query lists return a list of objects, and it is annoying to have to deal with it every time.  This will cast the objects
	 * into an array list of the desired class objects and return a new list.  If the input is null or empty it will return an empty arraylist.
	 * 
	 * @param objectList list of objects to be casted
	 * @param clazz a Class object for the class the objects will be casted to
	 * 
	 * @return the list of objects cased to the specified class
	 */
	protected <T> Collection<T> convertList(final List<?> objectList, final Class<T> clazz) {
		final Collection<T> result = new ArrayList<T>();
		
		// Just in case
		if (objectList != null) {
			for (final Object obj : objectList) {
				result.add(clazz.cast(obj));
			}
		}
		
		return result;
	}
	
	/**
	 * Gets the automation session from the AutomationSessionFactory
	 * 
	 * @return the current automation session
	 * 
	 * @see jpl.gds.product.automation.hibernate.AutomationSessionFactory#getSession()
	 */
	public Session getSession() {
		return sessionFactory.getSession(appContext);
	}
	
	/**
	 * This will check to see if the transaction for the current thread is
	 * alive. If it is not, will call the beginTransaction. If it is running,
	 * will just return the current transaction.
	 * 
	 * @return the Transaction instance
	 * 
	 * @see org.hibernate.SharedSessionContract#beginTransaction()
	 * @see org.hibernate.SharedSessionContract#getTransaction() 
	 */
	public Transaction startTransaction() {
		if (!isTransactionActive()) {
			return getSession().beginTransaction();
		} else {
			return getTransaction();
		}
	}
	
	/**
	 * Get the Transaction instance associated with this session. The concrete
	 * type of the returned Transaction object is determined by the
	 * hibernate.transaction_factory property
	 * 
	 * @return a Transaction instance
	 * 
	 * @see org.hibernate.SharedSessionContract#getTransaction()
	 */
	public Transaction getTransaction() {
		return getSession().getTransaction();
	}
	
	/**
	 * Returns Returns true if the transaction is active, false if not.
	 *  
	 * @return True if the current Transaction instance is active, false if not.
	 */
	public boolean isTransactionActive() {
		return getTransaction().getStatus().equals(TransactionStatus.ACTIVE);
	}
	
	/**
	 * Commit this transaction. This might entail a number of things depending
	 * on the context: <br>
	 * <ul>
	 * <li>If the underlying transaction was initiated from this Transaction the
	 * Session will be flushed, unless the Session is in FlushMode.MANUAL
	 * FlushMode.</li><br>
	 * <li>If the underlying transaction was initiated from this Transaction,
	 * commit the underlying transaction.</li><br>
	 * <li>Coordinate various callbacks</li>
	 * </ul>
	 * 
	 * @see org.hibernate.Transaction#commit()
	 */
	public void commit() {
		if (isTransactionActive()) {
			flush();
			getTransaction().commit();
		}
	}
	
	
	/**
	 * Rollback this transaction. Either rolls back the underlying transaction
	 * or ensures it cannot later commit (depending on the actual underlying
	 * strategy).
	 * 
	 * @see org.hibernate.Transaction#rollback()
	 */
	public void rollback() {
		if (isTransactionActive()) {
			getTransaction().rollback();
		}
	}

	/**
	 * Rollback the transaction and close the session
	 * 
	 * @see org.hibernate.Transaction#rollback()
	 *      {@link jpl.gds.product.automation.hibernate.dao.AbstractAutomationDAO#closeSession()}
	 */
	public void rollbackAndClose() {
		rollback();
		closeSession();
	}
	
	/**
	 * Force this session to flush. Must be called at the end of a unit of work,
	 * before committing the transaction and closing the session (depending on
	 * setFlushMode(FlushMode), Transaction.commit() calls this method).
	 * <br><br>
	 * Flushing is the process of synchronizing the underlying persistent store
	 * with persistable state held in memory.
	 * 
	 * @see org.hibernate.Session#flush()
	 */
	public void flush() {
		if (sessionOpen()) {
			getSession().flush();
		}
	}
	
	/**
	 * Check if the session is still open.
	 * 
	 * @return True if the session is still open, false otherwise
	 */
	public boolean sessionOpen() {
		return getSession().isOpen();
	}
	
	/**
	 * End the session by releasing the JDBC connection and cleaning up. It is
	 * not strictly necessary to close the session but you must at least
	 * disconnect() it.
	 */
	public void closeSession() {
		if (sessionOpen()) {
			getSession().close();
		}
	}
	
	/**
	 * Commit this Transaction and close the session
	 */
	public void commitAndClose() {
		commit();
		closeSession();
	}
	
	/**
	 * 11/21/2012 - MPCS-4387 - When using hibernate and many pdpp's at one time, the max db connections get filled up pretty easily.  The following
	 * methods are used to add and remove connections.  The idea would be that when a process starts, it adds the defined number of connections for iteself.  When 
	 * it shutsdown, it removes that number of connections.
	 * 
	 */
	
	/**
	 * Will do a query for the max connections. If it was not found for some
	 * reason -1.
	 * 
	 * @return current max connection value
	 */
	public Integer currentMaxConnections() {
		final Integer max = (Integer) getSession().createSQLQuery(CURRENT_CONNECTIONS_QUERY).addScalar("Value", StandardBasicTypes.INTEGER).uniqueResult();
		
		return max == null ? -1 : max;
	}

	/**
	 * General method to do the actual work for setting the max connections.
	 * 
	 * @param diffValue
	 *            - This value will be added to the current max connections. If
	 *            you want to decrease, pass in a negative number.
	 * 
	 */
	protected void setMaxConnections(final int diffValue) {
		// First things first, check if the adjustment flag was set.  If not, return without doing anything.
		if (!doAdjustments) {
			trace.debug("The doConnectionAdjustments flag was set to false.  Skipping max connection management.");
			return;
		}
		
		final Integer current = currentMaxConnections();
		// First check for failure case when getting the max connections.
		if (current == -1) {
			trace.error("Failed to retrieve the MYSQL db variable for max_connections.  No change in connections will be made.");	
			return;
		}
		
		// Check the run time min value.  If this has not been set, set it.  This run time value will be used as the minimum.  
		// The possiblity that the current connections when starting is less than min_max is there, so have to work with what is already set.
		if (RUN_TIME_MIN_CONNECTION_VALUE < 0) {
			RUN_TIME_MIN_CONNECTION_VALUE = current < minMaxConnectionValue ? current : minMaxConnectionValue;
		}
		
		// Check the current and the run time or max value.  Adjust the diffValue based on the head way so that the actual minimum and max are the min and max
		// set points.
		int adjustedDiff = 0;
		final int adjustedConnections = current + diffValue;
		
		if (diffValue < 0) {
			// This is a decrease.
			final int headway = RUN_TIME_MIN_CONNECTION_VALUE - current;
			adjustedDiff = adjustedConnections <= RUN_TIME_MIN_CONNECTION_VALUE ? headway : diffValue;
		} else {
			// This is an increase.
			final int headway = maxMaxConnectionValue - current;
			adjustedDiff = adjustedConnections >= maxMaxConnectionValue ? headway : diffValue;
		}
		
		// Now, just call the query, no need to do any checks.  If the value would put it over or under, the diff would be zero, so no harm in doing the sql call.
		// Do the query and execute the update.
		try {
			getSession().beginTransaction();
			getSession().createNativeQuery(String.format(CONNECTION_CHANGE_QUERY, adjustedDiff)).executeUpdate();
			getSession().getTransaction().commit();
		} catch (final Exception e) {
			trace.warn("Could not update MYSQL db max connections value.  " + e.getMessage());
		} finally {
			getSession().close();
		}
	}
	
	/**
	 * Will check the current max connections before trying to do the increase.
	 * Checks the min and max for max connections from the config before doing
	 * the work.
	 * 
	 * @param increaseValue
	 */
	public void increaseConnections(final int increaseValue) {
		setMaxConnections(increaseValue);
	}
	
	/**
	 * Will check the current max connections before trying to do the decrease.
	 * Checks the min and max for max connections from the config before doing
	 * the work.
	 * 
	 * @param decreaseValue
	 */
	public void decreaseConnections(final int decreaseValue) {
		setMaxConnections(-decreaseValue);
	}
	
	/**
	 * Uses the value set in the config and will raise the number of connections
	 * by that amount.
	 */
	public void increaseConnectionsDefault() {
		increaseConnections(connectionChangeValue);
	}

	/**
	 * Uses the value set in the config and will decrease the number of connections by that amount.  This will check before it does it and will 
	 * never go below the set minimum max connections.
	 */
	public void decreaseConnectionsDefault() {
		decreaseConnections(connectionChangeValue);
	}
}
