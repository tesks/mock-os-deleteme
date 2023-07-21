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
import org.hibernate.criterion.Restrictions;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.mapper.IFswToDictMapping;
import jpl.gds.product.automation.AutomationException;
import jpl.gds.product.automation.hibernate.AutomationSessionFactory;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationClassMap;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;

/**
 * Product automation proces table data accessor object.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original
 *          version in MPCS for MSL G9.
 * MPCS-8568 - 12/12/16 - Added fswVersion and fswDirectory
 *          columns. Processes are referenced by IFswToDictMapping instead of
 *          just fswBuildId
 */

public class ProductAutomationProcessDAO extends AbstractAutomationDAO {
	private static Timestamp NULL_TIMESTAMP = Timestamp.valueOf("2000-01-01 00:00:00");
	private ProductAutomationClassMapDAO classMapDao;

	public ProductAutomationProcessDAO(ApplicationContext appContext, AutomationSessionFactory sessionFactory) {
		super(appContext, sessionFactory);
		
		classMapDao = appContext.getBean(ProductAutomationClassMapDAO.class);
	}


	//queried column names as enum
	enum Column{
	
		PROCESS_ID("processId"),
		FSW_BUILD_ID("fswBuildId"),
		FSW_VERSION("fswVersion"),
		FSW_DIRECTORY("fswDirectory"),
		ACTION("action"),
		MACHINE("processHost"),
		PID("pid"),
		INITIALIZE_TIME("initalizeTime"),
		START_TIME("startTime"),
		SD_TIME("shutDownTime"),
		KILLER("killer"),
		PAUSE("pause"),
		PAUSE_ACK("pauseAck"),
		ASSIGNED_ACTIONS("assignedActions"),
		COMPLETED_ACTIONS("completedActions"),
		LAST_COMPLETE_TIME("lastCompleteTime"),
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
	
	
	private static final String ARBITER = "arbiter";
	private static final String PROC = "process";
	
	private static final int PAUSE_ACK_ENABLED = 1;
	private static final int PAUSE_ACK_DISABLED = 0;
	
	// This is used to find the command line process which is just a place holder with all null values and PID=COMMAND_LINE_PID
	private static final Long COMMAND_LINE_PID = new Long(-100);
	
	// MPCS-8179 06/17/16 - added, this way all of the fail count checks can be updated in one location
	private static final int MAX_FAILURES = 10;
	
	/**
	 * Processes have a unique adding cycle. Call this to add a new process
	 * object. This will set the processId and the initialize time for the
	 * process only.. All other fields will not be set. Once the process starts
	 * it will fill in the blanks.
	 * 
	 * @return A new ProductAutomationProcess object
	 */
	public ProductAutomationProcess addStubbedProcess() {
		ProductAutomationProcess process = new ProductAutomationProcess();
		process.setInitializeTime(new Timestamp(System.currentTimeMillis()));
		
		// All we need to do is save it and return it.
		getSession().save(process);
		
		return process;
	}

	/**
	 * This will set the start time of the product to the current time, and will
	 * save it to the start time of the process. If the start time is already
	 * set, this is a no-op.
	 * 
	 * @param process
	 *            the ProductAutomationProcess to have its start time set
	 * 
	 * @throws AutomationException
	 *             An error is encountered while setting the start time
	 */
	public void setStartTime(ProductAutomationProcess process) throws AutomationException {
		if (process.getStartTime() == null) {
			process.setStartTime(new Timestamp(System.currentTimeMillis()));
			updateProcess(process);
		}
	}
	
	/**
	 * <b>This method is to be used only by the arbiter.</b> <br>
	 * <br>
	 * Sets the shutdown time. Checks to make sure that the fields it is
	 * supposed to set are null. If they are not, this is a no-op. <br>
	 * This method is only to be used by the arbiter because it sets the killer
	 * flag to indicate that it was performed by the arbiter
	 * 
	 * @param process
	 *            the ProductAtuomationProcess to have its shut down time set
	 * 
	 * @throws AutomationException
	 *             An error is encountered while setting the shut down time
	 */	
	public void setArbiterShutDownTime(ProductAutomationProcess process) throws AutomationException {
		setShutDownTime(process, true);
	}
	
	/**
	 * Sets the shutdown time. Checks to make sure that the fields it is
	 * supposed to set are null. If they are not, this is a no-op. This method
	 * is to be utilized by the process itself because it sets the killer flag
	 * to self-killed
	 * 
	 * @param process
	 *            the ProductAtuomationProcess to have its shut down time set
	 * 
	 * @throws AutomationException
	 *             An error is encountered while setting the shut down time
	 */
	public void setProcessShutDownTime(ProductAutomationProcess process) throws AutomationException {
		setShutDownTime(process, false);
	}
	
	/**
	 * Sets the shutdown time in the product and persists the update. If
	 * arbiterKill is true, sets the killer enum to indicate the arbiter marked
	 * this process as shutdown. Otherwise is marked as a self kill.
	 * 
	 * This checks to see if either the shutdown time or the killer are null. If
	 * either is null, will set the shutdown time and killer. If they are
	 * already set, this is a no-op.
	 * 
	 * Handles its own hibernate sessions / transactions and commits when
	 * completed.
	 * 
	 * @param process
	 *            the ProductAutomationProcess to have its shut down time set
	 * 
	 * @param arbiterKill
	 *            true if the process has been killed by the arbiter, false
	 *            otherwise
	 * 
	 * @throws AutomationException
	 *             when the number of attempts for the operation have exceeded the maximum failure count
	 */
	public void setShutDownTime(ProductAutomationProcess process, boolean arbiterKill) throws AutomationException {
		/**
		 * MPCS-7013  1/2015 - Making this handle its own sessions and transactions with retry.
		 */
		if (process.getShutDownTime() == null || process.getKiller() == null) {
			int failCount = 0;
			Session session = null;
			Throwable lastError = null;
			
			do {
				try {
					session = getNewSession();
					
					process.setShutDownTime(new Timestamp(System.currentTimeMillis()));
					process.setKiller(arbiterKill ? ARBITER : PROC); 
					
					// If the start time is not set, put in a dummy value
					if (process.getStartTime() == null) {
						process.setStartTime(NULL_TIMESTAMP);
					}
					
					updateProcess(process);
					session.beginTransaction();
					session.saveOrUpdate(process);
					commitSession(session);
					return;
				} catch (Exception e) {
					lastError = e;
					rollbackSession(session);
					failCount++;
				} finally {
					closeSession(session);
				}
				
			} while (failCount < MAX_FAILURES);
			
			throw new AutomationException("Exceeded the fail count to set the shutdown time of the process: " + 
					lastError == null ? "UNKNOWN" : lastError.getMessage());
		}
	}
	
	/**
	 * This will update the process in the db. The processId must be set or it
	 * will throw an exception. This update must be called within a session /
	 * transaction. No session maintenance is done.
	 * 
	 * @param process
	 *            the ProductAutomationProcess to be updated
	 * 
	 * @throws AutomationException
	 *             When the current session does not already contain the process
	 *             that is being updated
	 */
	public void updateProcess(ProductAutomationProcess process) throws AutomationException {
		if (getSession().contains(process)) {
			getSession().saveOrUpdate(process);
		} else {
			throw new AutomationException("Process object must be persisted to update: " + process.toString());
		}
	}
	
	
	// Getters
	/**
	 * Hibernate query lists return a list of objects, and it is annoying to
	 * have to deal with it every time. This will cast the objects into an array
	 * list of action objects and return a new list. If the input is null or
	 * empty it will return an empty arraylist.
	 * 
	 * @param objectList
	 *            the set of objects to be casted to ProductAutomationProcess
	 *            objects
	 * 
	 * @return the casted set of objects
	 */
	private Collection<ProductAutomationProcess> convertList(List<?> objectList) {
		Collection<ProductAutomationProcess> result = new TreeSet<ProductAutomationProcess>();
		
		// Just in case
		if (objectList != null) {
			for (Object obj : objectList) {
				result.add((ProductAutomationProcess) obj);
			}
		}
		
		return result;
	}
	
	/**
	 * Returns the process if it is found. Otherwise null. This is used by the
	 * process to find the reserved row.
	 * 
	 * @param processId
	 *            the processId of the ProductAutomationProcess to be found
	 * 
	 * @return the ProductAutomationProcess corresponding to the supplied
	 *         processId
	 */
	public ProductAutomationProcess findProcess(Long processId) {
		/**
		 * MPCS-6544 - 9/2014 - Going a get instead of a query.
		 */
		
		return (ProductAutomationProcess) getSession().get(ProductAutomationProcess.class, processId);
	}

	/**
	 * A getter to find the default command line process.  If it is not found, will create it and return. This must be called from 
	 * a transaction just like all other DAO methods.
	 * 
	 * @return the ProductAutomationProcess that has been designated as the command line process
	 * 
	 * @version MPCS-4248 -
	 */
	public ProductAutomationProcess getCommandLineProcess() {
		ProductAutomationProcess process = (ProductAutomationProcess) getSession().createCriteria(ProductAutomationProcess.class)
			.add(Restrictions.eq(Column.PID.toString(), COMMAND_LINE_PID))
			.uniqueResult();
		
		if (process == null) {
			// Need to create a stubbed process and set the PID.
			process = addStubbedProcess();
			process.setPid(COMMAND_LINE_PID);
		}
		
		return process;
	}
	
	/**
	 * Does a query to find the processes with the action and build id. Returns
	 * an array of processes that are running using the action and build id
	 * supplied. The processes could be from any machine.
	 * 
	 * @param action
	 *            the ProductAutomationClassMap that all results must share
	 * 
	 * @param dictionary
	 *            the dictionary being queried
	 * 
	 * @return a collection of the ProductAutomationProcess objects that have
	 *         the requested action and use the requested dictionary
	 * 
	 * MPCS-8568 - 12/12/2016 - get processes with fswBuildId, fswVersion, and fswDirectory
	 */
	public Collection<ProductAutomationProcess> getAllProcess(ProductAutomationClassMap action, IFswToDictMapping dictionary) {
		return convertList(getSession().createCriteria(ProductAutomationProcess.class)
				.add(Restrictions.isNull(Column.KILLER.toString()))
				.add(Restrictions.isNull(Column.SD_TIME.toString()))
				.add(Restrictions.eq(Column.ACTION.toString(), action))
				.add(Restrictions.eq(Column.FSW_BUILD_ID.toString(), dictionary.getFswBuildVersion()))
				.add(Restrictions.eq(Column.FSW_VERSION.toString(), dictionary.getDictionaryVersion()))
				.add(Restrictions.eq(Column.FSW_DIRECTORY.toString(), dictionary.getFswDirectory()))
				.list());
	}
	
	/**
	 * Finds all processes that have not been initialized and either killer or
	 * shutdown time are null.
	 * 
	 * @return a collection of the ProductAutomationProcess objects that have
	 *         not been initialized
	 */
	public Collection<ProductAutomationProcess> getAllActiveUninitializedProcesses() {
		return convertList(getSession().createCriteria(ProductAutomationProcess.class)
				.add(Restrictions.isNull(Column.START_TIME.toString()))
				.add(Restrictions.disjunction() // This is an OR.
						.add(Restrictions.isNull(Column.SD_TIME.toString()))
						.add(Restrictions.isNull(Column.KILLER.toString())))
				.list());
	}
	
	/**
	 * Finds the process with the given info that is still running (shutDownTime
	 * and killer are null)
	 * 
	 * @param action
	 *            the ProductAutomationClassMap that all results must share
	 * 
	 * @param dictionary
	 *            the dictionary being queried
	 * 
	 * @param processHost
	 *            the name of the machine that has the process
	 * 
	 * @return a collection of the ProductAutomationProcess objects that have
	 *         the same classmap, use the same fswBuildId, are on the same host,
	 *         and have not been shut down or killed
	 * 
	 * MPCS-8568 - 12/12/2016 - get processes with fswBuildId, fswVersion, and fswDirectory
	 */
	public ProductAutomationProcess getProcess(ProductAutomationClassMap action, IFswToDictMapping dictionary, String processHost) {
		//TODO make this find the process that has the least amount of products assigned to it.
		List<?> processes = getSession().createCriteria(ProductAutomationProcess.class)
				.add(Restrictions.eq(Column.ACTION.toString(), action))
				.add(Restrictions.eq(Column.FSW_BUILD_ID.toString(), dictionary.getFswBuildVersion()))
				.add(Restrictions.eq(Column.FSW_VERSION.toString(), dictionary.getDictionaryVersion()))
				.add(Restrictions.eq(Column.FSW_DIRECTORY.toString(), dictionary.getFswDirectory()))
				.add(Restrictions.eq(Column.MACHINE.toString(), processHost))
				.add(Restrictions.isNull(Column.SD_TIME.toString()))
				.add(Restrictions.isNull(Column.KILLER.toString()))
				.setMaxResults(1)
				.list();

		return processes.isEmpty() ? null : (ProductAutomationProcess) processes.get(0);
	}
	
	/**
	 * Pass in a process object and this will set the pause value and update the process.  Does not do a commit.
	 * 
	 * @param process the ProductAutomationProcess to be marked as paused
	 * 
	 * @param pauseValue 1 if paused, otherwise not paused
	 * 
	 * @throws AutomationException when the pause value of the process is unable to be changed
	 */
	public void setPauseValue(ProductAutomationProcess process, int pauseValue) throws AutomationException {
		process.setPause(pauseValue);
		updateProcess(process);
	}
	
	/**
	 * Pass in a process object and this will set the pause value to indicate it
	 * is paused. Does not do a commit.
	 * 
	 * @param process
	 *            the ProductAutomationProcess to be marked as paused
	 * 
	 * @throws AutomationException
	 *             when the process is unable to be paused
	 */
	public void pauseProcess(ProductAutomationProcess process) throws AutomationException {
		setPauseValue(process, PAUSE_ACK_ENABLED);
	}
	
	/**
	 * Pass in a process object and this will set the paluse value to indicate
	 * it is not paused. Does not do a commit.
	 * 
	 * @param process
	 *            the ProductAutomationProcess to be marked as not paused
	 * 
	 * @throws AutomationException
	 *             when the process is unable to be unpaused
	 */
	public void unpauseProcess(ProductAutomationProcess process) throws AutomationException {
		setPauseValue(process, PAUSE_ACK_DISABLED);
	}
	
	/**
	 * Finds the process and sets the pause value to 1 which indicates that it
	 * is paused.
	 * 
	 * @param dictionary
	 *            the dictionary utilized by the process in question
	 * 
	 * @param actionMnemonic
	 *            the action of the process in question. This indicates which
	 *            ProductAutomationClassMap is in question.
	 * 
	 * @throws AutomationException
	 *             when the process is unable to be paused
	 */
	public void pauseProcess(IFswToDictMapping dictionary, String actionMnemonic) throws AutomationException {
		pauseProcess(dictionary, classMapDao.getClassMap(actionMnemonic));
	}
	
	/**
	 * This will find all processes running the action type and pauses them,
	 * regardless of machine.
	 * 
	 * @param dictionary
	 *            the dictionary utilized by the processes in question
	 * @param classMapping
	 *            the classmap action used by the processes to be paused
	 * @throws AutomationException
	 *             when the processes are unable to be paused
	 */
	public void pauseProcess(IFswToDictMapping dictionary, ProductAutomationClassMap classMapping) throws AutomationException {
		for (ProductAutomationProcess process : getAllProcess(classMapping, dictionary)) {
			pauseProcess(process);
		}
	}

	/**
	 * Finds the process and sets the pause value to 0 which indicates that it
	 * is not paused.
	 * 
	 * @param dictionary
	 *            the dictionary utilized by the process in question
	 * 
	 * @param actionMnemonic
	 *            the action of the process in question. This indicates which
	 *            ProductAutomationClassMap is in question.
	 * 
	 * @throws AutomationException
	 *             when the process is unable to be unpaused
	 */
	public void unpauseProcess(IFswToDictMapping dictionary, String actionMnemonic) throws AutomationException {
		unpauseProcess(dictionary, classMapDao.getClassMap(actionMnemonic));
	}
	
	/**
	 * This will find all processes running the action type and unpauses them,
	 * regardless of machine.
	 * 
	 * @param dictionary
	 *            the dictionary utilized by the process in questio
	 * @param classMapping
	 *            the classmap action used by the processes to be unpaused
	 * @throws AutomationException
	 *             when the processes are unable to be unpaused
	 */
	public void unpauseProcess(IFswToDictMapping dictionary, ProductAutomationClassMap classMapping) throws AutomationException {
		for (ProductAutomationProcess process : this.getAllProcess(classMapping, dictionary)) {
			unpauseProcess(process);
		}
	}	
	
	/**
	 * Given the process id set the pause acknowledge value. No reason to do any
	 * other methods with inputs other than the process id since only a process
	 * itself will ack it is paused.
	 * 
	 * @param processId
	 *            the processId of the ProductAutomationProcess to be found
	 * 
	 * @param pauseAckValue
	 *            1 if the process is to acknowledge it is paused, 0 otherwise.
	 * 
	 * @throws AutomationException
	 *             when an error is encountered during the operation
	 */
	public void pauseAcknowledge(Long processId, int pauseAckValue) throws AutomationException {
		ProductAutomationProcess process = findProcess(processId);
		
		if (process != null) {
			process.setPauseAck(pauseAckValue);
			
			updateProcess(process);
		}
	}
	
	/**
	 * Call this when the process goes into pause mode.
	 * 
	 * @param processId
	 *            the processId of the ProductAutomationProcess to be found
	 * 
	 * @throws AutomationException
	 *             when an error is encountered during the operation
	 */
	public void enablePauseAck(Long processId) throws AutomationException {
		pauseAcknowledge(processId, PAUSE_ACK_ENABLED);
	}
	
	/**
	 * call this when the pause is shut off from outside and the process can go
	 * back to work.
	 * 
	 * @param processId
	 *            the processId of the ProductAutomationProcess to be found
	 *            
	 * @throws AutomationException
	 *             when an error is encountered during the operation
	 */
	public void disablePauseAck(Long processId) throws AutomationException {
		pauseAcknowledge(processId, PAUSE_ACK_DISABLED);
	}

	/**
	 * Returns true if all of the processes with this build id and action
	 * mnemonic are pause ack enabled
	 * 
	 * @param dictionary
	 *            the dictionary utilized by the processes in question
	 * 
	 * @param actionMnemonic
	 *            the action of the process in question. This indicates which
	 *            ProductAutomationClassMap is in question.
	 * 
	 * @return true if all of the processes that match the criteria are pause
	 *         ack enabled, false otherwise
	 */
	public boolean isPaused(IFswToDictMapping dictionary, String actionMnemonic) {
		return isPaused(dictionary, classMapDao.getClassMap(actionMnemonic));
	}

	/**
	 * Returns true if all the processes with this build id and class are pause
	 * ack enabled
	 * 
	 * @param dictionary
	 *            the dictionary utilized by the processes in question
	 * 
	 * @param classMapping
	 *            the classmap action used by the processes to be checked if
	 *            they are paused
	 * 
	 * @return true if all of the processes that match the criteria are pause
	 *         ack enabled, false otherwise
	 */
	public boolean isPaused(IFswToDictMapping dictionary, ProductAutomationClassMap classMapping) {
		// Need to set the initial to true so that the case where there are no processes, will return true.
		boolean paused = true;
		
		for (ProductAutomationProcess process : getAllProcess(classMapping, dictionary)){
			paused = process.getPauseAck() == PAUSE_ACK_ENABLED;
		}
		
		return paused;
	}

	/**
	 * Returns true if all the processes with this build id and class are not
	 * paused ack enabled.
	 * 
	 * @param dictionary
	 *            the dictionary utilized by the processes in question
	 * 
	 * @param actionMnemonic
	 *            the action of the process in question. This indicates which
	 *            ProductAutomationClassMap is in question.
	 * 
	 * @return true if all of the processes that match the criteria are not
	 *         pause ack enabled, false otherwise
	 */
	public boolean isUnpaused(IFswToDictMapping dictionary, String actionMnemonic) {
		return isUnpaused(dictionary, classMapDao.getClassMap(actionMnemonic));
	}

	/**
	 * Returns true if all the processes with this build id and class are not
	 * paused ack enabled.
	 * 
	 * @param dictionary
	 *            the dictionary utilized by the processes in question
	 * 
	 * @param classMapping
	 *            the classmap action used by the processes to be checked if
	 *            they are paused
	 * 
	 * @return true if all of the processes that match the criteria are not
	 *         pause ack enabled, false otherwise
	 */
	public boolean isUnpaused(IFswToDictMapping dictionary, ProductAutomationClassMap classMapping) {
		// Need to set the initial to true so that the case where there are no processes, will return true.
		boolean unPaused = true;
		
		for (ProductAutomationProcess process : getAllProcess(classMapping, dictionary)){
			unPaused = process.getPauseAck() == PAUSE_ACK_DISABLED;
		}
		
		return unPaused;
	}	
	
	/**
	 * MPCS-6544  9/2014 - Adding support to adjust the assigned and completed counts for a process.
	 * Since this is going to be accessed from multiple threads and processes the contingency set of methods is 
	 * needed which includes getting fresh session from the session factory.  When adjusting a specific process
	 * we use the processId to look up the object in a different session because if there is an error the session 
	 * will be toast and we do not want to corrupt anything.  
	 * 
	 * All of the methods below this point use the getNewSession method and do not use the default getSession method.
	 */
	
	/**
	 * Creates a process and sets a limited amount of information for the
	 * process. Saves the process using a new session and commits that, then
	 * loads the process from the current session and returns that.
	 * 
	 * @param actionName
	 *            the ProductAutomationClassMap to be set in the process
	 * 
	 * @param dictionary
	 *            the dictionary to be utilized in this process
	 * 
	 * @param hostName
	 *            the name of the host for this process
	 * 
	 * @return a ProductAutomationProcess with the specified properties set
	 * 
	 * MPCS-8568 - 12/12/2016 - add stubbed process with fswBuildId, fswVersion, and fswDirectory
	 */
	public ProductAutomationProcess addStubbedProcess(ProductAutomationClassMap actionName, IFswToDictMapping dictionary, String hostName) {
		ProductAutomationProcess process = new ProductAutomationProcess();
		process.setInitializeTime(new Timestamp(System.currentTimeMillis()));
		process.setAction(actionName);
		process.setFswBuildId(dictionary.getFswBuildVersion());
		process.setFswVersion(dictionary.getDictionaryVersion());
		process.setFswDirectory(dictionary.getFswDirectory());
		process.setProcessHost(hostName);
		
		Session session = getNewSession();
		
		session.beginTransaction();
		session.save(process);
		session.flush();
		commitSession(session);
		closeSession(session);

		return findProcess(process.getProcessId());
	}
	
	
	/**
	 * Increments the assigned count for process.
	 * 
	 * @param process
	 *            the process that will have its assigned count incremented
	 * 
	 * @throws AutomationException
	 *             an error is encountered during the operation
	 */
	public void actionAssigned(ProductAutomationProcess process) throws AutomationException {
		updateProcessWithRetry(process, 1, 0);
	}

	/**
	 * Increments the completed count and decrements the assigned count for
	 * process.
	 * 
	 * @param process
	 *            the process that will have its assigned count decremented
	 * @throws AutomationException
	 *             an error is encountered during the operation
	 */
	public void actionCompleted(ProductAutomationProcess process) throws AutomationException {
		updateProcessWithRetry(process, -1, 1);
	}
	
	/**
	 * Adjusts the assigned count by actionAdjust for the process.
	 * 
	 * @param process
	 *            The process that will have its assigned count decremented
	 * 
	 * @param actionAdjust
	 *            the positive integer value that the process will have its
	 *            action count decreased by
	 * 
	 * @throws AutomationException
	 *             an error is encountered during the operation
	 */
	public void actionsReassigned(ProductAutomationProcess process, int actionAdjust) throws AutomationException {
		updateProcessWithRetry(process, -actionAdjust, 0);
	}

	/**
	 * Adjusts the assigned count by actionAdjust for the process with the
	 * designated processId.
	 * 
	 * @param processId
	 *            The id of the process that will have its assigned count
	 *            decremented
	 * 
	 * @param actionAdjust
	 *            the positive integer value that the process will have its
	 *            action count decreased by
	 * 
	 * @throws AutomationException
	 *             an error is encountered during the operation
	 */
	public void actionsReassigned(Long processId, int actionAdjust) throws AutomationException {
		actionsReassigned(findProcess(processId), actionAdjust);
	}

	/**
	 * Overriding because the logs DAO needs to be able to open a transaction each time and can not be tied down to a thread local
	 * session that the session factory creates.  However, this needs to be smart enough to create a session factory if one has 
	 * not already been created.
	 * 
	 * @return A new Session object
	 * 
	 * @see org.hibernate.SessionFactory#openSession()
	 */
	public Session getNewSession() {
		return sessionFactory.getSessionFactory().openSession();
	}
	
	private void rollbackSession(Session session) {
		if (session.getTransaction().getStatus() == TransactionStatus.ACTIVE) {
			session.getTransaction().rollback();
		}
		
		if (session.isDirty()){
			session.clear();
		}
	}

	private void commitSession(Session session) {
		if (session.getTransaction().getStatus() == TransactionStatus.ACTIVE) {
			session.getTransaction().commit();
		}
	}

	private void closeSession(Session session) {
		if (session.isOpen() && session.getTransaction().isActive()) {
			session.flush();
		}
		
		session.close();
	}
	
	/**
	 * Creates a new session and will set the pause value of the product to
	 * paused
	 * 
	 * @param processId
	 *            the ID of the process being assigned to the new session
	 * 
	 * @throws AutomationException
	 *             when the operation fails more times than the designated value
	 */
	public void pauseNow(Long processId) throws AutomationException {
		setPauseNow(processId, true);
	}
	
	/**
	 * @param processId
	 * @throws AutomationException 
	 */
	public void unpauseNow(Long processId) throws AutomationException {
		setPauseNow(processId, false);
	}	
	
	/**
	 * Creates a new session and will set the pause value for the product.
	 * 
	 * @param processId
	 *            the Id of the process being assigned to the new session
	 * 
	 * @param paused
	 *            true if the process is to be paused, false if not
	 * 
	 * @throws AutomationException
	 *             when the number of attempts for the operation have exceeded
	 *             the failure count
	 */
	public void setPauseNow(Long processId, boolean paused) throws AutomationException {
		int failCount = 0;
		Session session = null;
		
		do {
			try {
				session = getNewSession();
				
				ProductAutomationProcess process =  (ProductAutomationProcess) session
						.load(ProductAutomationProcess.class, processId);
				
				process.setPause(paused ? 1 : 0);
				session.beginTransaction();
				session.saveOrUpdate(process);
				commitSession(session);
				return;
			} catch (Exception e) {
				rollbackSession(session);
				failCount++;
			} finally {
				closeSession(session);
			}
			
		} while (failCount < MAX_FAILURES);
		
		throw new AutomationException("Exceeded the fail count to update process counts.");
	}
	
	/**
	 * If an update fails due to a version change, this will attempt to rectify
	 * the issue by creating a new session and loading the new state before
	 * making the update. It is not necessary to have a session or transaction
	 * open when calling this since all session related work is handled
	 * internally. If this is called from within a transaction the transaction
	 * will not be affected. However the process will be stale in the old
	 * session so if it is going to be used in an open session you must do a
	 * session.get on that object to force the session to requry the updated
	 * state of the object.
	 * 
	 * 
	 * @param process
	 *            the process to be updated
	 * 
	 * @param asAdjust
	 *            how much the assigned value is to be changed
	 * 
	 * @param compAdjust
	 *            how much the completed value is to be changed
	 * 
	 * @throws AutomationException
	 *             when the number of attempts for the operation have exceeded
	 *             the failure count
	 */
	public void updateProcessWithRetry(ProductAutomationProcess process, int asAdjust, int compAdjust) throws AutomationException {
		int failCount = 0;
		Session session = null;
		
		do {
			try {
				session = getNewSession();

				ProductAutomationProcess loadedProcess = (ProductAutomationProcess) session
						.load(ProductAutomationProcess.class, process.getProcessId());
				
				loadedProcess.adjust(asAdjust, compAdjust);
				
				session.beginTransaction();
				session.saveOrUpdate(loadedProcess);
				session.flush();
				commitSession(session);
				
				return;
			} catch (Exception e) {
				if (null != session) {
					rollbackSession(session);
				}
				failCount++;
			} finally {
				if (null != session) {
					closeSession(session);
				}
			}
		} while (failCount < MAX_FAILURES);
		
		throw new AutomationException("Exceeded the fail count to update process counts.");
	}

	
	/**
	 * Returns the last complete time field for the given process id. If field is null returns 0.
	 * 
	 * @param process the process that is being queried for a completion time
	 * 
	 * @return the last complete time in milliseconds
	 */
	public Long getLastCompleteTimeMS(ProductAutomationProcess process) {
		Long ct = process.getLastCompleteTime();
		
		return null == ct ? new Long(0) : ct;
	}
	
	/**
	 * Returns the last complete time field for the given process id. 0 if the
	 * field is null or the process is not found.
	 * 
	 * @param processId
	 *            the ID associated with the process that is being queried for a
	 *            completion time
	 * 
	 * @return the last complete time for the product in milliseconds
	 */
	public Long getLastCompleteTimeMS(Long processId) {
		ProductAutomationProcess process = findProcess(processId);
		
		if (process == null) {
			trace.warn(String.format("Could not find process %d to check alive status.  Assuming process is dead.", processId));
			return new Long(0);
		} else {
			return getLastCompleteTimeMS(process);
		}
	}
}
