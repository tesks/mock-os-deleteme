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
package jpl.gds.product.processors;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.mapper.IFswToDictMapping;
import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.automation.AutomationException;
import jpl.gds.product.automation.ProductAutomationProperties;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationProcessDAO;
import jpl.gds.product.automation.hibernate.entity.*;
import jpl.gds.product.processors.exceptions.AutomationProcessException;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.process.ProcessLauncher;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Starts and stops product automation processes and keeps the process launcher
 * in an internal cache. This is a singleton class and all caching is done using
 * ConcurrentHashMaps so it is thread safe.
 *
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original
 *          version in MPCS for MSL G9.
 * MPCS-8568 - 12/12/16 - Updated keep track of processes by
 *          IFswToDictMappings. Change startProcessAndUpdate - remove fswBuildId
 *          from processScript, add fswDictioanryDir and fswVersion
 */
public class ProductAutomationProcessCache implements IProductAutomationProcessCache {

	private final HashMap<String, Integer> maxParallelCount;
	private final Integer processBacklogCount;
	private final String processScript;

	private final ConcurrentHashMap<Long, ProcessObj> processIdMap;
	// MPCS-8568  12/12/16 - changed to IFswToDictMapping from Long (fswBuildId)
	private final ConcurrentHashMap<String,HashMap<IFswToDictMapping,List<ProcessObj>>> typeMap;

	private final Tracer log;
	private final String host;

	/**
	 * MPCS-6970 -  2/2015 - Add cache for bogus dictionaries that we should not attempt to
	 * assign to a new process.
	 */
	private final Set<Long> bogusDictionaries;
	private final IFswToDictionaryMapper mapper;
	private final ProductAutomationProcessDAO processDao;

	/**
	 * @param host
	 */
	public ProductAutomationProcessCache(final String host, final ApplicationContext appContext) {
		this.host = host;
		this.mapper = appContext.getBean(IFswToDictionaryMapper.class);
        this.log = TraceManager.getTracer(appContext, Loggers.PDPP);
		processDao = appContext.getBean(ProductAutomationProcessDAO.class);

		typeMap = new ConcurrentHashMap<String, HashMap<IFswToDictMapping, List<ProcessObj>>>();
		processIdMap = new ConcurrentHashMap<Long, ProcessObj>();
		bogusDictionaries = new TreeSet<Long>();

		final File dir = new File(GdsSystemProperties.getGdsDirectory());

		final ProductAutomationProperties config = appContext.getBean(ProductAutomationProperties.class);
		processScript = dir.getAbsolutePath() + "/bin/admin/" + config.getProcessScript();
		processBacklogCount = config.getMaxProcessorBacklog();
		maxParallelCount = new HashMap<String, Integer>();


		final List<String> processors = config.getCheckOrder();
		for (final String proc : processors) {

			// move the value-getting logic to the config file
			final Integer parallelCount = config.getParallelProcessorsCount(proc);
			maxParallelCount.put(proc, parallelCount);
		}

		checkBogusDictionaries();
	}

	/**
	 * Parses the FSW mapper file if there are bogus dictionaries and checks
	 * them again.
	 *
	 * @return a list of bogus ids that have been returned to active status.
	 */
	public Collection<Long> checkBogusDictionaries() {
		log.debug("Checking bogus dictionaries");
		if (bogusDictionaries.isEmpty()) {
			return Collections.emptySet();
		} else {
			synchronized (ProductAutomationProcessCache.class) {
				final Collection<Long> active = new TreeSet<Long>();

				for (final Long fswId : bogusDictionaries) {
					if (isExists(fswId)) {
						log.debug("Removing " + fswId + " from bogusDictionaries.");
						active.add(fswId);
					}
				}

				/**
				 * Remove the good ones from the bogus list.
				 */
				bogusDictionaries.removeAll(active);

				return active;
			}
		}
	}

	/**
	 * Finds the least busy process for the given action.  If no process is running will start a new one.
	 *
	 * @param action
	 *
	 * @return ProcessAutomationProcess object
	 *
	 * @throws AutomationException - Automation error occured
	 * @throws AutomationProcessException - Process could not start due to an unmapped or missing dictionary.
	 * @throws DictionaryException
	 */
	public ProductAutomationProcess getProcessor(final ProductAutomationAction action)
			throws AutomationException, AutomationProcessException, DictionaryException {
		return getProcessor(action.getProduct(), action.getActionName());
	}

	/**
	 * Finds the least busy process for the given action type and status.  If no process is running will start a new one.
	 *
	 * @param actionClassMap
	 * @param status
	 *
	 * @return ProcessAutomationProcess object
	 *
	 * @throws AutomationException - Automation error occured
	 * @throws AutomationProcessException - Process could not start due to an unmapped or missing dictionary.
	 * @throws DictionaryException
	 */
	public ProductAutomationProcess getProcessor(final ProductAutomationClassMap actionClassMap, final ProductAutomationStatus status)
			throws AutomationException, AutomationProcessException, DictionaryException {
		return getProcessor(status.getProduct(), actionClassMap);
	}

	/**
	 * Finds the least busy process for the given fsw build id and class map.  If no process is running will start a new one.
	 *
	 * @param product The product that needs to find a processor
	 * @param actionClassMap The action to be performed on the product
	 *
	 * @return ProcessAutomationProcess The process that will performed the desired action on the specified product
	 *
	 * @throws AutomationException - Automation error occurred
	 * @throws AutomationProcessException - Process could not start due to an unmapped or missing dictionary.
	 * @throws DictionaryException  - error loading mapper file.
	 */
	public ProductAutomationProcess getProcessor(final ProductAutomationProduct product, final ProductAutomationClassMap actionClassMap) throws AutomationException, AutomationProcessException, DictionaryException {

		// MPCS-8568 12/12/16 - If the product has info for a dictionary, but it's not mapped, add it to the mapper
		// reload the dictionary map as it may have been reset.
		if(!isExists(product.getFswBuildId()) && !product.getDictVersion().equals(mapper.getUnmatchedDictionaryVersion())){
			log.debug(String.format("Adding dictionary %s with FSW Build ID %d in directory %s to the mapper because a product references it, but it is not mapped", product.getDictVersion(), product.getFswBuildId(), product.getFswDirectory()));
			mapper.addFswToDictMapping(product.getFswBuildId(), product.getDictVersion(), product.getDictVersion(), null, product.getFswDirectory(), null, null, null);
		}

		final IFswToDictMapping dictionary = mapper.getDictionary(product.getFswBuildId());

		final ProcessObj processObject = getProcess(dictionary, actionClassMap);

		if (processObject == null) {
			final String exceptionString = String.format("Could not start new process for action type %s using dictionary %s with FSW Build ID %d in %s directory because the "
					+ "dictionary either did not exist or was not properly mapped in the dictionary mapper file.",
					actionClassMap.getMnemonic(), dictionary.getDictionaryVersion(), dictionary.getFswBuildVersion(), dictionary.getFswDirectory());

			/*
			 * MPCS-8568 12/15/16 - If a process wasn't returned, either the
			 * mapping was faulty or nonexistent. The former will have caused a
			 * bogusDictionary entry in getProcess. However, the latter will
			 * only add -1 to the bogusDictionary set. Add the product's FSW
			 * build ID to the bogusDictionary set here so it's definitely in
			 * there.
			 */
			bogusDictionaries.add(product.getFswBuildId());
			log.debug(exceptionString);
			throw new AutomationProcessException(exceptionString);
		} else {
			return processDao.findProcess(processObject.getProcId());
		}
	}

	/**
	 * Checks all running processes and will check to see if they are dead.  This is done by
	 * checking if the process has assigned actions but has not completed any for a specific amount of time.
	 *
	 * @param deadTime Amount of time to be considered dead.
	 * @return a list of processIds for all dead processes
	 */
	public List<Long> getDeadProcessIds(final int deadTime) {
		final ArrayList<Long> dead = new ArrayList<Long>();

		for (final ProcessObj process : processIdMap.values()) {
			if (process.isDead(deadTime)) {
				dead.add(process.getProcId());
			}
		}

		return dead;
	}

	/**
	 * This will kill all the processes in the cache.  This MUST NOT be called in a transaction, it handles all its
	 * own session / transaction work.
	 */
	public void killAllProcesses() {
		for (final ProcessObj process : processIdMap.values()) {
			try {
				killProcess(process);
			} catch (final AutomationException e) {
				log.error(String.format("The killing of process %d met with exception: %s", process.getProcId(), e.getMessage()));
			}
		}
	}

	/**
	 * Uses the process object to shutdown the launcher and will mark all the proper places in
	 * the database for the process to shutdown.
	 *
	 * @param process
	 * @throws AutomationException
	 */
	public void killProcess(final ProductAutomationProcess process) throws AutomationException {
		killProcess(process.getProcessId());
	}

	/**
	 * Uses the process object to shutdown the launcher and will mark all the proper places in
	 * the database for the process to shutdown.
	 *
	 * This MUST NOT be called in a transaction, it will handle all hibernate session / transactions.
	 *
	 * @param processId
	 * @throws AutomationException
	 */
	public void killProcess(final Long processId) throws AutomationException {
		if (!processIdMap.containsKey(processId)) {
			log.error(String.format("Process with id=%d was not found in the internal cache and could not be closed.", processId));
		} else {
			killProcess(processIdMap.get(processId));
		}
	}

	/**
	 * Uses the process object to shutdown the launcher and will mark all the proper places in
	 * the database for the process to shutdown.
	 *
	 * This will handle its own hibernate session / transactions and will commit when completed.
	 *
	 * @param process
	 * @throws AutomationException
	 */
	public void killProcess(final ProcessObj process) throws AutomationException {
		try {
			process.getProcess().destroy();
			final ProductAutomationProcess proc = processDao.findProcess(process.getProcId());

			if (proc.getShutDownTime() == null || proc.getKiller() == null) {
				processDao.setArbiterShutDownTime(proc);
				log.debug("Killed and marked process shutdown with processId=" + process.getProcId());
			}

			/**
			 *  Update the stats and the process.  Once a process is shutdown it should not have any actions assigned to it.
			 */
		} finally {
			removeProcess(process);
		}
	}

	/**
	 * Starts a new process using the ProcessLauncher class. Creates a new
	 * processor stubbed object and persists the object.
	 *
	 * @param actionName
	 *            The action of the process being started
	 * @param dictionary
	 *            The dictionary mapping for the process being started
	 * @throws AutomationException
	 *             If an error occurs while starting the process
	 */
	private ProcessObj startProcessAndUpdate(final ProductAutomationClassMap actionName, final IFswToDictMapping dictionary) throws AutomationException  {
		final ProductAutomationProcess process = processDao.addStubbedProcess(actionName, dictionary, host);

		// Build the command.
		final String cmd = processScript + " --fswDictionaryDir " + dictionary.getFswDirectory() + " --fswVersion " + dictionary.getDictionaryVersion() + " --hibernateProcessId " + process.getProcessId().toString();

		boolean wasStarted = false;
		ProcessLauncher launcher = null;

		try {
			launcher = new ProcessLauncher();
			log.debug("Running command: " + cmd);
            wasStarted = launcher.launch(cmd);
		} catch (final IOException e) {
			throw new AutomationException("Could not start new process (" + e.getMessage() + ")");
		}

		// If the process was started, add the process launcher to the internal map and update the process with the PID.
		if (wasStarted) {
			//Can not get the pid currently, so all will be -1 for now.
			process.setPid(-1L);

			// Update the process and save to db.
			processDao.updateProcess(process);

			/**
			 * MPCS-7174 -  - adding deeper info info.
			 */
			log.info(String.format("New process started for build %d using the dictionary %s in directory %s with action type %s and processId %d",
					dictionary.getFswBuildVersion(), dictionary.getDictionaryVersion(), dictionary.getFswDirectory(), actionName.getMnemonic(), process.getProcessId()));
		} else {
			throw new AutomationException("Process launcher reported process was unable to start for unknown reasons");
		}

		return new ProcessObj(process.getProcessId(),
				process.getFswBuildId(),
				launcher,
				actionName.getMnemonic(),
				processDao);
	}

	/**
	 * This is the main method that is used to find a process for an action map / fswId combo.  All other methods eventually land here.
	 * Any optimization should be done in this method.
	 *
	 * This will find a process to assign an action to and will start processes if no processes have been started or
	 * if a given action has open slots for parallel processes.
	 *
	 * This MUST be called in a transaction and committed after calling.
	 *
	 * @param dictionaryMapping
	 * @param actionMap
	 * @return
	 * @throws AutomationException
	 * @throws DictionaryException  - error with reloading dictionary mapper file
	 */
	private ProcessObj getProcess(final IFswToDictMapping dictionaryMapping, final ProductAutomationClassMap actionMap) throws AutomationException, DictionaryException  {
		final String actionName = actionMap.getMnemonic();

		/**
		 * MPCS-6970 -  2/2015 - If the dictionary for the action is known to be bogus return null early.
		 */
		synchronized (this.bogusDictionaries) {
			boolean ok;

			if (bogusDictionaries.contains(dictionaryMapping.getFswBuildVersion())) {
				log.debug(String.format("Not starting process for %s with the FSW Build ID %s because it is listed as a bogus dictionary.",
						actionMap.getMnemonic(), dictionaryMapping.toString()));

				ok = false;

			} else if (!isExists(dictionaryMapping.getFswBuildVersion())) {
				/**
				 * First time we have seen this.  Add the id to the map and warn.
				 */
				bogusDictionaries.add(dictionaryMapping.getFswBuildVersion());
				log.error(String.format("Not starting process for %s. The FSW Build ID %s is not mapped to a dictionary.",
						actionMap.getMnemonic(), dictionaryMapping.toString()));

				ok = false;
			} else {
				ok = true;
			}

			if (!ok) {
				log.debug("returning null process due to a bogus dictionary");
				return null;
			}
		}

		ProcessObj leastBusy = null;
		Long lastCount = Long.valueOf(-1);

		final List<ProcessObj> procs = getObjectList(actionName, dictionaryMapping);

		if (procs.size() > 1) {
			/**
			 * MPCS-7207 -  3/2015 - Check the state of the procs and close out the ones that are dead.
			 * Also recreate the dict map if we find dead ones.
			 *
			 * This is cause for concern since we do not do parallel processes.  This should indicate
			 * that some processes have died and we did not detect it.  Clear out the process map
			 * of dead processes.
			 */
			boolean doMapper = false;

			for (int i = procs.size() - 1; i >= 0; i--) {
				if (!procs.get(i).isRunning()) {
					killProcess(procs.get(i));
					doMapper = true;
				}
			}

			if(doMapper) {
				mapper.reload();
			}
		}

		for (final ProcessObj obj : procs) {
			final Long count = obj.getIncompleteCount();


			/**
			 * Check to see if the process is paused since we don't want to give any actions
			 * to a process that is paused or is not running.
			 */
			if (!obj.isPaused() && obj.isRunning() && (lastCount < 0 || count < lastCount)) {
				leastBusy = obj;
				lastCount = count;
			}
		}

		/**
		 *  If null there is no process so a new one needs to be started.  If not null check to see if the number of
		 *  actions for it are over the deal.
		 *
		 */
		if (leastBusy == null ||
				isParallelSlotsOpen(actionName, dictionaryMapping) && lastCount > processBacklogCount) {

			if (null != leastBusy) {
				log.info(String.format("Starting parallel process for action type %s due to high backlog.", actionName));
			}

			leastBusy = startProcessAndUpdate(actionMap, dictionaryMapping);

			// Add to the maps
			getObjectList(actionName, dictionaryMapping).add(leastBusy);

			processIdMap.put(leastBusy.getProcId(), leastBusy);
		}

		return leastBusy;
	}

	private boolean isParallelSlotsOpen(final String actionName, final IFswToDictMapping dictionaryMapping) {
		return maxParallelCount.get(actionName) > getActionTypeCount(actionName, dictionaryMapping);
	}

	private Integer getActionTypeCount(final String actionName, final IFswToDictMapping dictionaryMapping) {
		return getObjectList(actionName, dictionaryMapping).size();
	}

	/**
	 * Conv method to get the process map from the fswIdmap.  This will initialize the maps and lists if they were never found and
	 * return an ArrayList associated with the given pair of inputs.
	 *
	 * @param actionName
	 * @param dictMapping
	 * @return
	 */
	private List<ProcessObj> getObjectList(final String actionName, final IFswToDictMapping dictMapping) {
		if (!typeMap.containsKey(actionName)) {
			// Need to initialize the maps.
			typeMap.put(actionName, new HashMap<IFswToDictMapping, List<ProcessObj>>());
		}

		// Find the proper list to find the process and add new ones if needed.
		List<ProcessObj> processObjects;

		if (!typeMap.get(actionName).containsKey(dictMapping)) {
			processObjects = Collections.synchronizedList(new ArrayList<ProcessObj>());
			typeMap.get(actionName).put(dictMapping, processObjects);
		}

		return typeMap.get(actionName).get(dictMapping);
	}

	/**
	 * @param process
	 */
	private void removeProcess(final ProcessObj process) {
		if (process == null) {
			return;
		} else {
			try {
				processIdMap.remove(process.getProcId());
				typeMap.get(process.getActionType()).get(mapper.getDictionary(process.getFswId())).remove(process);
			} catch (final Exception e) {
				log.error("Failed to remove process object from all internal caches: " + e.getMessage());
			}
		}
	}

	/**
	 * MPCS-6970 -  2/2015 - Add ways to check to see if the dictionary exists on the file system.
	 * MPCS-8131  - 05/16/16 - moved both isExists from FswToDictionaryMapper to factory
	 */

	/**
	 * Checks if the dictionary version is mapped. If it is makes checks to see
	 * if it exists. Uses the static instance of FswToDictionaryMapper within
	 * this factory. If it is not instantiated and properly mapped, this method
	 * will return false even if the dictionary does exist.
	 *
	 * @param dict
	 *            the name (only, not path) of the dictionary file to find
	 * @param dictDir
	 * 		the directory, minus the mission name, that the dictionary lives in.
	 * @return TRUE if the dictionary file exists, FALSE if not.
	 * 06/15/16 - MPCS-8179 changed to static method, if the
	 *          instance is null, return false
	 */
	public boolean isExists(final String dict, final String dictDir) {
		if (mapper != null && mapper.isMapped(dict)) {

			/**
			 * Create a new dictionary configuration and then check if the dict exists.
			 */
			final DictionaryProperties dc = new DictionaryProperties(false);
			dc.setFswVersion(dict);
			dc.setFswDictionaryDir(dictDir);

			try {
				final String dfm = dc.getDictionaryFile(dict);
				return dfm != null;
			} catch (final DictionaryException e) {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Checks to see if a dictionary exists for the specified FSW build ID. Uses
	 * the static instance of FswToDictionaryMapper within this factory. If it
	 * is not instantiated and properly mapped, this method will return false
	 * even if the dictionary does exist.
	 *
	 * @param fswId
	 *            FSW build ID being checked
	 * @return TRUE if a dictionary for the provided FSW build ID exists, FALSE
	 *         if not
	 * 06/15/16 - MPCS-8179 changed to static method, if the
	 *          instance is null, return false
	 */
	public boolean isExists(final Long fswId) {
		if (mapper.isMapped(fswId)) {
			final IFswToDictMapping entry = mapper.getDictionary(fswId);

			return isExists(entry.getDictionaryVersion(), entry.getFswDirectory());
		} else {
			log.debug("returning hard false");
			return false;
		}
	}
	
	/**
	 * Class to be used to hold all info about a launched process, as well
	 * as the launched process object.
	 * 
	 */
	protected static class ProcessObj implements Comparable<ProcessObj> {

		private final Long procId;
		private final Long fswId;
		private final ProcessLauncher process;
		private final String actionType;
		private final ProductAutomationProcessDAO processDao;
		
		private Long lastIncompleteCount;
		private final Long initTime;
		
		/**
		 * @param procId
		 * @param fswId
		 * @param process
		 * @param actionType
		 */
		public ProcessObj(final Long procId, final Long fswId, final ProcessLauncher process, final String actionType, final ProductAutomationProcessDAO processDao) {
			super();
			this.procId = procId;
			this.fswId = fswId;
			this.process = process;
			this.actionType = actionType;
			this.processDao = processDao;
			
			lastIncompleteCount = 0L;
			
			initTime = System.currentTimeMillis();
		}
		
		public Long getProcId() {
			return procId;
		}
		
		public Long getFswId() {
			return fswId;
		}
		
		public ProcessLauncher getProcess() {
			return process;
		}
		
		public String getActionType() {
			return actionType;
		}
		
		public Long getLastIncompleteCount() {
			return lastIncompleteCount;
		}

		public boolean isPaused() {
			final ProductAutomationProcess process = processDao.findProcess(getProcId());
			
			return process.getPause() == 1 || process.getPauseAck() == 1;
		}
		
		/**
		 * Gets the number of assigned actions for the process.  Sets the last incomplete value and time as well.
		 * 
		 * @return the number of assigned actions
		 */
		public Long getIncompleteCount() {
			try {
				return processDao.findProcess(getProcId())
						.getAssignedActions();
			} finally {
				processDao.closeSession();
			}
		}
		
		public boolean isRunning() {
			final ProductAutomationProcess process = processDao.findProcess(getProcId());
			
			return process.getShutDownTime() == null && process.getKiller() == null && !this.process.isComplete();
		}
		
		/**
		 * Checks to see if the given process is dead.
		 * 
		 * @param maxTimeDelta
		 * @return true if the process is declared dead, false if not
		 */
		public boolean isDead(final Integer maxTimeDelta) {
			try {
				final ProductAutomationProcess proc = processDao.findProcess(getProcId());
				
				final Long lastIncomplete = getLastIncompleteCount();
				final Long currentIncompleteCount = proc.getAssignedActions();
				
				// Set the last count.
				lastIncompleteCount = currentIncompleteCount;
				
				/**
				 *  The last complete time could be zero.  This could mean it died right away or that we just started it and 
				 *  it has not had enough time to initialize.  Either way, if it is zero use the init time.
				 */
				final Long checkTime = proc.getLastCompleteTime() == 0 ? this.initTime : proc.getLastCompleteTime();
				
				return proc.getPauseAck() == 0 && // Process is not paused.
						lastIncomplete > 0 && // We had actions to handle since the last is dead check.
						maxTimeDelta < System.currentTimeMillis() - checkTime ; // The time since the last complete is greater than the delta time given.
			} finally {
				processDao.closeSession();
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(final ProcessObj o) {
			if (o == null || !(o instanceof ProcessObj)) {
				return 1;
			} else {
				return getIncompleteCount().compareTo(o.getIncompleteCount());
			}
		}
	}
}
