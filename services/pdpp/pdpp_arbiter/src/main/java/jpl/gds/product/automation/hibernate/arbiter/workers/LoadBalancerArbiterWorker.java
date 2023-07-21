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

import org.springframework.context.ApplicationContext;

import jpl.gds.product.automation.AutomationException;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationActionDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationProcessDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;

/**
 * Product automation arbiter worker.  Monitors processes to detect overloaded or dead processes.
 * Redistributes actions in the case of an overloaded process or will mark for reassignment
 * in the case of a dead process.
 * 
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
public class LoadBalancerArbiterWorker extends AbstractArbiterWorker {

	private Integer deadTime;
	private Integer loadDiff;
	private ProductAutomationActionDAO actionInstance;
	private ProductAutomationProcessDAO processInstance;

	/**
	 * @param deadTime
	 * @param loadDiff
	 * @param maxErrors
	 * @param cycleTime
	 */
	public LoadBalancerArbiterWorker(int deadTime, int loadDiff, int maxErrors, Long cycleTime, ApplicationContext appContext) {
		super(null, null, null, maxErrors, cycleTime, null, appContext);
		this.deadTime = deadTime;
		this.loadDiff = loadDiff;

		actionInstance = appContext.getBean(ProductAutomationActionDAO.class);
		processInstance = appContext.getBean(ProductAutomationProcessDAO.class);
	}

	/**
	 * @param processId
	 * @param loadBalanced - If true it means the call to this method is balancing the load between multiple processors.  
	 * 						 If it is false we are taking actions away from a dead process.  This dictates the query that is being
	 * 					     used to find the actions to be unassigned.
	 * 
	 * @throws AutomationException
	 */
	private void takeAwayActions(Long processId, boolean loadBalanced) throws AutomationException {

		try {
			actionInstance.startTransaction();
			int actionCount = 0;
			Long fswId = null;
			
			/**
			 * MPCS-6800 -  - Catch any exception and roll-back this un-claim step.
			 */
			for (ProductAutomationAction action : loadBalanced ? 
					actionInstance.getMostRecentAssignedActions(processId, loadDiff) : // We are load balancing.
					actionInstance.getAllAssignedActions(processId)) // Process is dead and we are taking them away.
			{
				actionInstance.unclaimAction(action);
				actionCount++;
				
				if (null == fswId) {
					fswId = action.getProduct().getFswBuildId();
				}
			}
			
			actionInstance.commit();
			
			if (actionCount > 0) {
				// If we get here, all the actions have been stripped.  Adjust the process to reflect this change.
				processInstance.actionsReassigned(processId, actionCount);
			}
		} catch (Exception e) {
			actionInstance.rollback();
			throw new AutomationException("Failed to unclaim actions: " + e.getMessage());
		} finally {
			actionInstance.closeSession();
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		log.info("Started load balancer.");
		nowRunning();
		
		while(isRunning()) {
			/**
			 * MPCS-7238 -  - Must detect stranded actions, actions that are assigned to dead processes.
			 * process them in a block.
			 */
			try {
				actionInstance.startTransaction();
				
				for (ProductAutomationAction action : actionInstance.getStrandedActions()) {
					log.info("Found stranded action: " + action);
					actionInstance.unclaimAction(action);
				}
				
				actionInstance.commit();
			} catch (Exception e) {
				log.debug("Exception encountered trying to reassign stranded actions: " + e.getMessage());
			} finally {
				actionInstance.closeSession();
			}
			
			/**
			 * Detect dead processes.
			 */
			for (Long processId : getProcessCache().getDeadProcessIds(deadTime)) {
				log.info(String.format("Found dead process with process id %d.", processId));
				
				try {
					// As a courtesy, mark this paused.
					processInstance.pauseNow(processId);
					
					/**
					 * MPCS-7013 -  1/2015 - Sleep for a time to allow the process to pause ack and stop working.
					 */
					try {
						doSleep(3000L);
					} catch (InterruptedException e) {
						log.info("Got interrupted from sleep, moving on to take away actions");
					}
					
					/**
					 * MPCS-7147 -  3/2015 - If this fails most likely the process is not dead.  Just stop trying 
					 * to kill it and move to the next one.  Only send out a debug message since this will happen often and 
					 * is not really an error.
					 */
					try {
						takeAwayActions(processId, false); 
					} catch (AutomationException e) {
						log.debug(String.format("Failed to take away actions for process %d because it got to work.", processId));
						processInstance.unpauseNow(processId);
						continue;
					}
					
					/**
					 * MPCS-7013 -  1/2015 - killProcess handles its own hibernate sessions / transactions. 
					 */					
					getProcessCache().killProcess(processId);
					resetErrorCount();
				} catch (AutomationException e) {
					log.error(String.format("Failed to kill dead process with process id %d: %s", 
							processId, e.getMessage()));
					incrementErrorCount();
					
					if (overErrorCount()) {
						break;
					}
				}
			}
			
			
			/**
			 * Do load balancing.  This will find any processes that are overloaded and will unassigne loadDiff
			 * actions.  The action assigner will then detect those processes and reassign them to a process that is
			 * not so busy.  In order for any processes to be deemed overloaded there must be at least 2 processes
			 * running for the same action type and fsw id.
			 */
			
			/**
			 * MPCS-6800 -  - Commenting out this code because we can not do parallel processes anyway.  
			 */
//			for (Long processId : getProcessCache().getOverloadedProcessors(loadDiff)) {
//				log.info(String.format("Detected overloaded process with process id %d.", processId));
//				
//				try {
//					/**
//					 * All of these method calls handle their own sessions / transactions, so no committing or rolling back is necessary.
//					 */
//					processInstance.pauseNow(processId);
//					takeAwayActions(processId, true); 
//					processInstance.unpauseNow(processId);
//					
//					resetErrorCount();
//				} catch (AutomationException e) {
//					log.error(String.format("Failed to take away actions from process with id %d and type %s for load balancing: %s", 
//							processId, actionType.getMnemonic(), e.getMessage()));
//					
//					incrementErrorCount();
//					
//					if (overErrorCount()) {
//						break;
//					}
//				} finally {
//					processInstance.closeSession();
//				}
//			}

			
			if (overErrorCount()) {
				log.fatal("Load balancer has exceeded the maximum number of consecutive errors and is halting execution.");
				break;
			} else {
				try {
					doSleep();
				} catch (InterruptedException e) {
					// whatever
				}
			}
		}
		
		log.info("Load balancer has shut down.");

	}

	/* (non-Javadoc)
	 * @see jpl.gds.msl.product.automation.hibernate.arbiter.workers.AbstractArbiterWorker#getWorkerName()
	 */
	@Override
	public String getWorkerName() {
		return "LoadBalancer";
	}
}
