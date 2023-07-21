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

import jpl.gds.product.automation.ProductAutomationProperties;
import jpl.gds.product.automation.hibernate.checkers.IProductAutomationChecker;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationActionDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationClassMapDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationStatusDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationClassMap;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationStatus;
import jpl.gds.shared.reflect.ReflectionToolkit;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;

/**
 * Product automation arbiter worker. Responsible for finding all uncategorized
 * products and creating actions.
 * 
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
public class ActionCreatorArbiterWorker extends AbstractArbiterWorker {
	
	// MPCS-8180 07/01/16 - removed property names, moved to ProductAutomationConfig
	
	/** The abriter process id,*/
	public static final Long ARBITER_PROCESS_ID = new Long(0);
	
	
	/**
	 * MPCS-6469  9/2014 - Adding objects to keep the checker classes and the order they should be run.
	 */
	private List<String> checkOrder;
	private HashMap<String, IProductAutomationChecker> checkers;


	private ProductAutomationClassMapDAO classMapDao;
	
	/**
	 * Constructor for action creator subprocess of the arbiter
	 * 
	 * @param transactionBlockSize
	 *            the number of transactions to be pulled as a block of
	 *            processes
	 * @param maxErrors
	 *            the maximum number of errors that can be encountered
	 * @param cycleTime
	 *            the time, in milliseconds, between each transaction block
	 */
	public ActionCreatorArbiterWorker(int transactionBlockSize, int maxErrors, Long cycleTime, ApplicationContext appContext) {
		super(null, transactionBlockSize, null, maxErrors, cycleTime, null, appContext);
		
		init();
		classMapDao = appContext.getBean(ProductAutomationClassMapDAO.class); 
	}	
	
	private void init() {
		/**
		 * MPCS-6469  9/2014 - Initialize the checker related objects.
		 */
		ProductAutomationProperties config = appContext.getBean(ProductAutomationProperties.class);
		checkOrder = config.getCheckOrder();
		checkers = new HashMap<String, IProductAutomationChecker>();
		
		for (String mnemonic : checkOrder) {
			String className = config.getCheckerClass(mnemonic);
			
			if (className == null) {
				System.err.println(String.format("Could not find automation checker configuration for PDPP mnemonic %s", mnemonic));
			} else {
				try {
					Class<?>[] argTypes = {String.class, ApplicationContext.class};
					Constructor<?> ctor = ReflectionToolkit.getConstructor(className, argTypes);
					IProductAutomationChecker c = (IProductAutomationChecker)ctor.newInstance(mnemonic, appContext);
					
					checkers.put(mnemonic, c);
				} catch (Exception e) {
					System.err.println(String.format("Encountered error when trying to create checker class for mnemonic %s which points to class %s: %s",
							mnemonic, className, e.getMessage()));
				}
			}
		}
	}



	/**
	 * Cycles through the checker classes to find the proper action for each
	 * product. Null is returned if none of the current processors need to be
	 * used on the product
	 * 
	 * @param product
	 *            the ProductAutomationProduct to be worked on
	 * @return the ProductAutomationClassMap that will be the next type of
	 *         action to be performed on the product.
	 */
	private ProductAutomationClassMap getActionName(ProductAutomationProduct product) {
		for (String mnemonic : checkOrder) {
			if (checkers.get(mnemonic).isProcessingRequired(product)) {
				return classMapDao.getClassMap(mnemonic);
			}
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		log.info("Started action creator");
		nowRunning();
		
		while(isRunning()) {
			ProductAutomationActionDAO actionInstance = appContext.getBean(ProductAutomationActionDAO.class);
			ProductAutomationStatusDAO statusInstance = appContext.getBean(ProductAutomationStatusDAO.class);
			
			// Can not close session after each status.  This will make the status object that were queried
			// bad to the current session.  You must only commit or roll back the transaction.  Close the session after the loop.
			/**
			 * Gets blockSize statuses and will create actions for them all in one transaction.  
			 */
			boolean workDoneOnLastCycle = false;

			try {
				for (ProductAutomationStatus status : statusInstance.getStatusesForCategorizing(getTransactionBlockSize())) {
					workDoneOnLastCycle = true;
					
					Long productId = status.getProduct().getProductId();
					statusInstance.startTransaction();
				
					if (status.getProduct().getFswBuildId() <= 0) {
						/*
						 * MPCS-4308 -  - 10/31/2012 - Adding log message for this failure case.
						 */
						log.warn("Failed to process product " + status.getProduct().getProductPath() + " because the FSW BuildID of the product is less than or equal to zero", ARBITER_PROCESS_ID, productId);
						// This means it is a partial, and there is no mpdu.  Mark as failed.
						statusInstance.addFailed(status.getProduct(), status.getPassNumber());
						statusInstance.commit();
					} else {
						ProductAutomationClassMap actionName = getActionName(status.getProduct());

						if (actionName == null) {
							// No work required.  Add a completed chill_down status.
							statusInstance.addCompletedPrevious(status.getProduct(), status.getPassNumber());
							statusInstance.commit();
						} else if (actionName.getEnabled() == 0) {
							// This was an info message, but it will totally spam the log table on every cycle when something is disabled.  So changing
							// to a debug.  It would be easy to figure out why products were not getting processed if there is an issue in this case.
							log.trace("Action " + actionName.getMnemonic() + " has been disabled.  Leaving status for product uncategorized: " + status.getProduct().getProductPath(), ARBITER_PROCESS_ID, productId);
							
						} else {
							/**
							 * Just create an action but don't assign it to a process.  
							 */
							log.debug(String.format("Creating action for status %d", 
									status.getStatusId()), 
									ARBITER_PROCESS_ID, 
									status.getProduct().getProductId());
							
							actionInstance.addAction(actionName, status);
							
							// Create a status that it has been categorized.
							statusInstance.addCategorized(status.getProduct(), status.getPassNumber());	
							statusInstance.commit();
							resetErrorCount();
						}
					}
				}
			} catch (Exception e) {
				log.error("Exception encountered in the categorizing loop.  Transaction is being rolled back and closing session: " + e.getMessage());
				e.printStackTrace();
				statusInstance.rollbackAndClose();
				incrementErrorCount();
				
			} finally {
				// Everything went fine, commit and close the session.
				statusInstance.closeSession();
			}
			
			if (overErrorCount()) {
				log.fatal("Action creator has exceeded the maximum number of consecutive errors and is halting execution.");
				break;
			} else if (!workDoneOnLastCycle) {
				try {
					doSleep();
				} catch (InterruptedException e) {
					// Whatever
				}
			}
		}
		
		log.info("Action creator has shut down.");
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.msl.product.automation.hibernate.arbiter.workers.AbstractArbiterWorker#getWorkerName()
	 */
	@Override
	public String getWorkerName() {
		return "ActionCreator";
	}
}