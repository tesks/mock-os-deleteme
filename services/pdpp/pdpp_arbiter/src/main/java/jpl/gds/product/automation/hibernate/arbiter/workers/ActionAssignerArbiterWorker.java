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
import jpl.gds.product.automation.hibernate.dao.ProductAutomationActionDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationClassMapDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationProcessDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationClassMap;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;
import jpl.gds.product.processors.exceptions.AutomationProcessException;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Product automation arbiter worker.  Finds all unassigned actions for a given action type and assigns to processes. 
 * 
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
public class ActionAssignerArbiterWorker extends AbstractArbiterWorker {
	/**
	 * MPCS-6970 2/2015 - Add a list to hold action IDs that need to be skipped
	 * due to a missing dictionary. We want to be able to skip querying these actions if need be. 
	 */
	private static final long CHECK_TIME_DIFF = 30000;
	
	/**
	 * MPCS-7174 - When a process is started by the process cache this time will be used to wait.  If the process
	 * is not started after this amount of time kill it and start again.
	 * 
	 * MPCS-8180 07/26/16 - changed to config value
	 */
	
	private Map<Long, Set<Long>> deadActions;
	private Long deadActionCheckTimeStamp;
	private final long processStartWaitTime;

	
	/**
	 * Main constructor for the action assigner, which assigns actions to processors
	 * 
	 * @param actionType the ProductAutomationClassMap object representing the type of actions that need to be assigned
	 * @param transactionBlockSize the number of transactions to be processed per block moved around
	 * @param maxAssigns the maximum number of actions to be assigned to a process
	 * @param maxErrors the maximum number of errors that a process may encounter
	 * @param cycleTime the time to wait, in milliseconds, between each transaction block
	 * @param dbPort the database host
	 */
	public ActionAssignerArbiterWorker(ProductAutomationClassMap actionType, int transactionBlockSize,
			int maxAssigns, int maxErrors, Long cycleTime, String dbHost,  ApplicationContext appContext) {
		super(actionType, transactionBlockSize, maxAssigns, maxErrors, cycleTime, dbHost, appContext);
		
		processStartWaitTime = appContext.getBean(ProductAutomationProperties.class).getProcessIdleKillTimeMS();
		deadActions = new ConcurrentHashMap<Long, Set<Long>>();
		deadActionCheckTimeStamp = System.currentTimeMillis();
	}

	/**
	 * call to the processor object check for reactivated fsw ids.
	 */
	private void reactivateActions() {
		log.debug("Checking for actions that can be reactivated");
		if (!deadActions.isEmpty() && 
			System.currentTimeMillis() - deadActionCheckTimeStamp > CHECK_TIME_DIFF) {
			
			/**
			 * Check for reactivated dictionaries.  All that is required is we remove the mappings for 
			 * the reactivated fsw ids.
			 */
			for (Long id : getProcessCache().checkBogusDictionaries()) {
				log.debug("Removing FSW build ID " + id + " from dead actions");
				deadActions.remove(id);
			}
			
			deadActionCheckTimeStamp = System.currentTimeMillis();
		}
	}
	
	private Collection<Long> getAllDeadActionIds() {
		if (deadActions.isEmpty()) {
			return Collections.emptySet();
		} else {
			Collection<Long> da = new ArrayList<Long>(20);
			
			for (final Collection<Long> dead : deadActions.values()) {
				da.addAll(dead);
			}
			
			return da;
		}
	}
	
	private void addDeadAction(final Long fswId, final Long actionId) {
		if (!deadActions.containsKey(fswId)) {
			deadActions.put(fswId, new TreeSet<Long>());
		}
		
		deadActions.get(fswId).add(actionId);
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		log.info("Started action assigner for action type " + getActionType().getMnemonic());
		nowRunning();
		
		while (isRunning()) {
			
			/**
			 * MPCS-6972 1/2015 - Check to see if this action type is disabled.  If it is skip this work cycle.
			 */
			ProductAutomationClassMapDAO classMapDao = appContext.getBean(ProductAutomationClassMapDAO.class); 
			classMapDao.getSession().refresh(actionType);
			
			if (actionType.getEnabled() == ProductAutomationClassMapDAO.Abled.DISABLED.value()) {
				try {
					doSleep();
				} catch (InterruptedException e) {
					// Don't care.
				}			
				classMapDao.closeSession();
				continue;
			}
			
			
			int assignedCount = 0;
			
			ProductAutomationActionDAO actionInstance = appContext.getBean(ProductAutomationActionDAO.class);
			ProductAutomationProcessDAO processInstance = appContext.getBean(ProductAutomationProcessDAO.class);
			
			boolean workDoneInCycle;
	
			do {
				workDoneInCycle = false;
				
				/**
				 * MPCS-6970 -  2/2015 - Get actions not in the dead list.  
				 */
				
				try {
					for (ProductAutomationAction action : actionInstance.getUnassignedActions(actionType, getTransactionBlockSize(), getAllDeadActionIds())) {
						workDoneInCycle = true;
						
						ProductAutomationProcess process;
						
						try {
							process = getProcessCache().getProcessor(action);
						} catch (AutomationProcessException e) {
							/**
							 * MPCS-6970 -  2/2015 - If the get process throws an automation process exception
							 * the dictionary has an issue.  Add the action to the dead list, log an error and continue 
							 * to the next action.
							 */
								addDeadAction(action.getProduct().getFswBuildId(), action.getActionId());
								log.error(String.format("Action with actionId %d and action type %s cannot be be performed because "
										+ "the dictionary with dictionary %s and FSW Build ID %d in the directory %s was not mapped or does not exist.", 
										action.getActionId(), actionType.getMnemonic(), action.getProduct().getDictVersion(), action.getProduct().getFswBuildId(), action.getProduct().getFswDirectory()));
								continue;
						}
						
						/**
						 * Check to see if the process is initialized before using it.  
						 * 
						 * MPCS-7148 -  - Allowing the process to try to start up for a certain amount of time and then 
						 * we will kill the process and let it get started again.
						 */
						if (process.getStartTime() == null && 
							System.currentTimeMillis() - process.getInitializeTime().getTime() > processStartWaitTime) {
							log.info(String.format("Process with processId %d has not started in the alloted start up time of %d milliseconds.",
									process.getProcessId(), processStartWaitTime));
							
							getProcessCache().killProcess(process);
							break;
						}  else if (process.getStartTime() == null) {
							log.info("Process returned by process cache has not initialized.  Skipping action until process is ready");
							workDoneInCycle = false;
							break;
						}
						
						boolean wasReassigned = action.getReassignBool();
						
						log.debug("was reassigned? " + wasReassigned);
						log.debug("startingTransation");
						
						actionInstance.startTransaction();

						action.setProcess(process);
						
						if (wasReassigned) {
							action.setReassign(0);
						}
						
						actionInstance.updateAction(action);
						assignedCount++;
						
						resetErrorCount();
						actionInstance.commit();
						
						log.debug("assigning action...");
						// Either way this is the same call.
						processInstance.actionAssigned(process);
					}
				} catch (Exception e) {
					log.error(String.format("Exception encountered when trying to assign actions: %s",
							e.getMessage()));
					e.printStackTrace();
					
					actionInstance.rollbackAndClose();
					
					incrementErrorCount();
					continue;
				} finally {
					actionInstance.closeSession();
				}
			} while (isRunning() && workDoneInCycle && assignedCount < getMaxAssigns() && !overErrorCount());
			
			if (overErrorCount()) {
				log.fatal("Action assigner has exceeded the maximum number of consecutive errors and is halting execution.");
				break;
			} else if (!workDoneInCycle) {
				try {
					doSleep();
				} catch (InterruptedException e) {
					// Don't care.
				}
			}
			
			/**
			 * MPCS-6970 -  2/2015 - At the end of the main loop call the reactivate method.
			 */
			reactivateActions();
		}	
		
		log.info("Action assigner for action type " + getActionType().getMnemonic() + " has shut down.");

	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.msl.product.automation.hibernate.arbiter.workers.AbstractArbiterWorker#getWorkerName()
	 */
	@Override
	public String getWorkerName() {
		return "ActionAssigner";
	}
}
