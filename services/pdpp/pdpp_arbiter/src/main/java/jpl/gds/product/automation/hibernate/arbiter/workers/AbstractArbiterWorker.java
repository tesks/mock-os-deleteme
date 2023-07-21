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
package jpl.gds.product.automation.hibernate.arbiter.workers;

import jpl.gds.product.automation.hibernate.IAutomationLogger;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationClassMap;
import jpl.gds.product.processors.IProductAutomationProcessCache;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for the product automation arbiter workers.  
 * 
 *
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
public abstract class AbstractArbiterWorker extends Thread {
	
	/** Number of errors encountered by this worker */
	protected Integer errorCount;
	/** Maximum number of products that can be assigned to this worker */
	protected Integer maxAssigns;
	/** Max number of errors this worker can encounter before terminating */
	protected Integer maxErrors;
	/** Number of transactions it will take and work on in a group */
	protected Integer transactionBlockSize;
	/**  amount of time, in milliseconds, between each block of products to be worked on */
	protected Long cycleTime;
	private final AtomicBoolean running;
	
	/** Database host */
	protected String dbHost;
	
	/** the mnemonic, or type of action, to be performed by this worker */
	protected ProductAutomationClassMap actionType;
	
	protected final ApplicationContext appContext;
    protected final IAutomationLogger    log;
	
	/**
	 * @param actionType
	 * @param transactionBlockSize
	 * @param maxAssigns
	 * @param maxErrors
	 * @param cycleTime
	 * @param dbHost
	 */
	public AbstractArbiterWorker(final ProductAutomationClassMap actionType, 
			final Integer transactionBlockSize, final Integer maxAssigns, final Integer maxErrors, final Long cycleTime, final String dbHost, final ApplicationContext appContext) {
		
		this.appContext = appContext;
		this.actionType = actionType;
		this.errorCount = 0;
		this.maxAssigns = maxAssigns;
		this.maxErrors = maxErrors;
		this.transactionBlockSize = transactionBlockSize;
		this.cycleTime = cycleTime;
		this.dbHost = dbHost;
        this.log = appContext.getBean(IAutomationLogger.class);

		this.running = new AtomicBoolean(false);
				
	    setName(String.format("ArbiterWorker-%s-%s", getWorkerName(), getActionNameStr()));
	    setDaemon(true);
	}

	

	/**
	 * Returns the worker name.
	 * 
	 * @return the worker name
	 */
	public abstract String getWorkerName();
	
	/**
	 * ^C shutdown hook
	 */
	public synchronized void shutdown() {
		running.set(false);
		notifyAll();
	}
	
	/**
	 * This is used to name the thread from within the constructor using the action type mnemonic.  If the action type is
	 * null than there will only be a single instance of this subclass because it is not action specific and a string '1' is 
	 * returned.
	 *  
	 * @return the action name / mnemonic
	 */
	protected String getActionNameStr() {
		return actionType == null ? "1" : actionType.getMnemonic();
	}
	
	/**
	 * Increase the error count by one
	 */
	protected void incrementErrorCount() {
		this.errorCount++;
	}
	
	/**
	 * Resets the error count back to zero
	 */
	protected void resetErrorCount() {
		this.errorCount = 0;
	}

	/**
	 * Get the current error count
	 * 
	 * @return the error count value
	 */
	protected Integer getErrorCount() {
		return errorCount;
	}

	/**
	 * Get the maximum number of assigned actions for this worker
	 * 
	 * @return the maximum number of assigned actions for this worker
	 */
	protected Integer getMaxAssigns() {
		return maxAssigns;
	}

	/**
	 * Get the maximum number of errors that this worker can encounter before
	 * shutting down
	 * 
	 * @return the maximum number of errors that this worker can encounter
	 */
	protected Integer getMaxErrors() {
		return maxErrors;
	}

	/**
	 * The number of transactions that can be worked on in a chunk
	 * 
	 * @return the number of transactions that can be worked on in a chunk
	 */
	protected Integer getTransactionBlockSize() {
		return transactionBlockSize;
	}

	/**
	 * TRUE if the worker is running, FALSE if not
	 * 
	 * @return state of the running flag.
	 */
	protected boolean isRunning() {
		return running.get();
	}
	
	/**
	 * Sets internal running flag to true.
	 */
	protected void nowRunning() {
		running.set(true);
	}
	
	/**
	 * Sets internal running flag to false.
	 */
	protected void notRunning() {
		running.set(false);
	}

	/**
	 * There is a ProductAutomationClassMap object that is associated with this
	 * worker. It determines what action name/mnemonic that states the type of
	 * action performed by this worker and the class that extends this abstract
	 * worker
	 * 
	 * @return the ProductAutomationClassMap associated with this worker
	 */
	protected ProductAutomationClassMap getActionType() {
		return actionType;
	}
	
	/**
	 * Get if this worker has encountered at least as many errors than the max
	 * error value
	 * 
	 * @return TRUE if this process has encountered too many errors, false
	 *         otherwise
	 */
	protected boolean overErrorCount() {
		return errorCount >= maxErrors;
	}
	
	/**
	 * Have this worker sleep for the designated amount of milliseconds time
	 * 
	 * @param sleepTime
	 *            the time, in milliseconds, for this worker to sleep
	 * @throws InterruptedException
	 *             if the sleep encounters an exception
	 */
	protected synchronized void doSleep(final long sleepTime) throws InterruptedException {
		wait(sleepTime);
	}
	
	/**
	 * Have this worker sleep for the amount of time set in the class
	 * 
	 * @throws InterruptedException
	 *             if the sleep encounters an exception
	 */
	protected synchronized void doSleep() throws InterruptedException {
		doSleep(cycleTime);
	}
	
	/**
	 * Gets the ProductAutomationProcessCache associated with this worker
	 * 
	 * @return the ProductAutomationProcessCache for this worker
	 */
	protected IProductAutomationProcessCache getProcessCache() {
		return appContext.getBean(IProductAutomationProcessCache.class);
	}
}

