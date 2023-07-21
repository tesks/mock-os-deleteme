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
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.ApplicationContext;

import jpl.gds.product.automation.ProductAutomationProperties;
import jpl.gds.product.automation.hibernate.AutomationLogger;
import jpl.gds.product.automation.hibernate.AutomationSessionFactory;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationLog;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.shared.log.AmpcsLog4jMessage;

/**
 * Mainly used by the automation appender to add log messages into the logs
 * table of the database.  
 * 
 * This is attempting to be a special DAO. This WILL get sessions and transactions and close them for you.  It will
 * use a different session than all the other DAO's.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 */
public class ProductAutomationLogsDAO extends AbstractAutomationDAO {
	
	private final String host;

	public ProductAutomationLogsDAO(final ApplicationContext appContext, final AutomationSessionFactory sessionFactory) {
		super(appContext, sessionFactory);

		final String tempHost = appContext.getBean(ProductAutomationProperties.class).getArbiterHost();
		
		this.host = tempHost != null ? tempHost : "localHost";
	}

	//column names as enum
	enum Column{
		
		LOG_ID("logId"),
		LEVEL("level"),
		MESSAGE("message"),
		HOST("host"),
		PROCESSOR_ID("processorId"),
		PRODUCT("product"),
		EVENT_TIME("eventTime");
		
		private String columnName;
		
		private Column(final String columnName){
			this.columnName = columnName;
		}
		
		@Override
		public String toString(){
			return columnName;
		}
	}
	
	@Override
	/**
	 * Overriding because the logs DAO needs to be able to open a transaction
	 * each time and can not be tied down to a thread local session that the
	 * session factory creates. However, this needs to be smart enough to create
	 * a session factory if one has not already been created.
	 */
	public Session getSession() {
		return sessionFactory.getSessionFactory().openSession();
	}
	
	@Override 
	public Transaction startTransaction() {
		return null;
	}
	
	/**
	 * Given an event passed to a log4j appender, this will create a hibernate
	 * object and add to the session. Must be called inside an active
	 * transaction.
	 * 
	 * @param event
	 *            the event to be logged in the database
	 * 
	 * @see org.apache.log4j.spi.LoggingEvent
	 */
    public void addLogMessage(final AmpcsLog4jMessage event) {
		// The message should be <message><SEP><processorId><SEP><productId>
 		final String m = event.getMessage().toString();
		final String[] splits = m.split(String.valueOf(AutomationLogger.SEPERATOR_STRING));
		
		String msg = null;
		Long pid = null;
		Long product = null;
		
		switch(splits.length) {
		case 1:
			// No metadata in the message.  
			msg = m;
			pid = new Long(-1);
			product = new Long(-1);
			break;
		case 2:
			// The process id has been given.
			msg = splits[0];
			product = new Long(-1);
			
			try {
				pid = Long.parseLong(splits[1]);
			} catch (final Exception e) {
				pid = new Long(-1);
			}
			break;
		case 3:
			// All three have been given.
			msg = splits[0];
			try {
				pid = Long.parseLong(splits[1]);
			} catch (final Exception e) {
				pid = new Long(-1);
			}			
			
			try {
				product = Long.parseLong(splits[2]);
			} catch (final Exception e) {
				product = new Long(-1);
			}
		}

		// Parse level
		// PDPP Log table only accepts "WARN" -- "WARNING" will cause a SQL error
		String severity = event.getSeverity().toString().toUpperCase();
		String level = severity.equals("WARNING") ? "WARN" : severity;

		// Build the object and add it to the db.
		final ProductAutomationLog logObject = new ProductAutomationLog(
				level,  // Level
				msg, // Message 
				host, // Host 
				pid, // Processor ID 
				product, // Product
                new Timestamp(event.getEventTime().getTime()) // Event time
				);
		
		addLog(logObject);
	}
	
	private void addLog(final ProductAutomationLog logObject) {
		final Session session = getSession();
		final Transaction tx = session.beginTransaction();
		
		try {
			session.save(logObject);
			tx.commit();
		} catch (final Exception e) {
			tx.rollback();
		} finally {
			session.close();
		}
	}
	
	
	private Collection<ProductAutomationLog> objectsToLogs(final List<?> objects) {
		final Collection<ProductAutomationLog> result = new TreeSet<ProductAutomationLog>();
		
		for (final Object obj : objects) {
			result.add((ProductAutomationLog) obj);
		}
		
		return result;
	}
	
	/**
	 * Gets a list of all of the stored logs associated with a product.
	 * 
	 * @param product
	 *            a ProductAutomationProduct for which logs are being requested
	 * 
	 * @return a collection of ProductAutomationLog objects
	 */
	public Collection<ProductAutomationLog> getLogs(final ProductAutomationProduct product) {
		return objectsToLogs(getSession().createCriteria(ProductAutomationLog.class)
				.add(Restrictions.eq(Column.PRODUCT.toString(), product.getProductId()))
				.list()
				);
	}

}
