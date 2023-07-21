/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.product.automation;

import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.file.IProductMetadata;
import jpl.gds.product.automation.app.AbstractPostDownlinkProductProcessorApp;
import jpl.gds.product.automation.app.options.PostDownlinkProductProcessorAppOptions;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationActionDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationProcessDAO;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationStatusDAO;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;
import jpl.gds.product.processors.IPostDownlinkProductProcessor;
import jpl.gds.product.processors.PostDownlinkProductProcessorOptions;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.SpringContextFactory;
import org.apache.commons.cli.ParseException;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.TimeUnit;



/**
 * Executable class that is started by the product automation arbiter. Finds the
 * PDPP class to use in the database and will process assigned actions.
 *
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20
 *          adaptation
 *
 * MCSADAPT-180 - 12/3/2019 - This class was adapted from M20/MSL to allow for multimission PDPP.
 *          Comments were brought over intact for their historical value.
 */
public class ReferenceAutomationProcessorApp extends AbstractPostDownlinkProductProcessorApp {

    //removed property names - now in ProductAutomationConfig.java;

    // Need a delay before starting so that the arbiter can update the database with all the relevant information needed for this
    // process.  If we don't do this, the arbiter gets a stale object exception.
    private final Long cycleTime;
    private final Long selfKillTime;
    private final int maxConsecutiveErrors;
    private final int processLookup;
    private final Long idleTimeToCloseDb;
    private final int PAUSED_VALUE = 1;

    private int consectutiveErrorCount;
    //MPCS-8379 10/03/16 - removed processIdOption and property names - moved to PostDownlinkProductProcessorAppOptions.java
    private Long processId = null;
    private boolean readyToRoll;
    private Long lastProductProcessedTime;
    // Keeping track of the store controller so that the call to close is not done continuously.
    private boolean storeControllerClosed = false;

    private boolean workWasDone;

    /**
     * MPCS-6544 -  - Adding a lock object to use to wait instead of
     * using Thread.sleep. The idea here is that the process will wait for some
     * time if there is nothing to do. However, we want to have it start the
     * shutdown process as soon as we receive a kill command, so will then call
     * lock.notifyAll to wake this guy up to shutdown.
     *
     * Adding a boolean running to check so we can stop processing data once we
     * get the kill command.
     *
     * MPCS-6978 - The running flag is not working the way it should. There is a
     * race condition where the process is working on a product and the shutdown
     * is called and completes but the process is working on a product. The
     * shutdown will unclaim all the actions even though an action is currently
     * under way. The process tries to commit the hibernate session after the
     * work is done and it fails to update even though the work was already
     * done. This causes the product not to be added to the db and the work to
     * be done multiple times.
     *
     * The issue was that the doSleep and doWakeup were using the lock to do the
     * wait which caused the working loop to release all monitors. The solution
     * is to have the sleep and wakeup sync on this and use lock to sync the
     * actual work.
     *
     * Changing running to thread safe atomic and requiring all of the major
     * functions hold the lock.
     *
     *
     */
    private final Object lock;
    private boolean running;


    // DAO instances
    private final ProductAutomationProcessDAO processDao;
    private final ProductAutomationActionDAO actionInstance;
    private final ProductAutomationStatusDAO statusInstance;
    private final IFswToDictionaryMapper mapper;


    /**
     * Basic constructor for the ReferenceAutomationProcessorApp. Gets it ready, but
     * doesn't start doing work
     *
     * @param appContext
     *            The current application context
     */
    public ReferenceAutomationProcessorApp(final ApplicationContext appContext) {
        super(appContext);
        readyToRoll = false;
        lastProductProcessedTime = System.currentTimeMillis();

        running = false;
        workWasDone = false;

        lock = new Object();

        final ProductAutomationProperties config = appContext.getBean(ProductAutomationProperties.class);

        cycleTime = config.getProcessCycleTimeMS();
        selfKillTime = config.getSelfKillTimeMS();
        maxConsecutiveErrors = config.getMaxConsecutiveErrors();
        processLookup = config.getProcessLookupRetryCount();

        // property fetch now guarantees it's over 0
        idleTimeToCloseDb = config.getIdleTimeToCloseDbConnectionsMS();

        this.processDao = appContext.getBean(ProductAutomationProcessDAO.class);
        this.actionInstance = appContext.getBean(ProductAutomationActionDAO.class);
        this.statusInstance = appContext.getBean(ProductAutomationStatusDAO.class);

        this.mapper = appContext.getBean(IFswToDictionaryMapper.class);
    }

    /**
     * States if the app is ready to process
     * @return TRUE if the app is initialized, FALSE if not
     */
    public boolean isInitialized() {
        return readyToRoll;
    }

    /**
     * Finds and returns a product processor that will perform a specific type
     * of action
     *
     * @param options
     *            the PostDownlinkProductProcessorOptions object that is to be
     *            used in the creation
     *
     * @return a PostDownlinkProductProcessor object that can process products
     * @throws Exception
     *             an exception is occurred while creating the processor
     */
    public IPostDownlinkProductProcessor createProductProcessor(final PostDownlinkProductProcessorOptions options) throws Exception {
        // Get the process object from the db to find out what class needs to be set.
        final ProductAutomationProcess process = processDao.findProcess(processId);

        if (process == null) {
            throw new AutomationException("ProcessId supplied to application returned a null value.  Can not initialize process: (processId=" + processId);
        } else {
            log.debug("Found process object in the database", processId);
        }

        return appContext.getBean(IPostDownlinkProductProcessor.class, options,
                                  process.getAction().getMnemonic());

    }

    /* (non-Javadoc)
     * @see jpl.gds.app.AbstractPostDownlinkProductProcessorApp#createOptions()
     */
    // MPCS-8379 10/03/16 - Updated to use BaseCommandOptions
    @Override
    public BaseCommandOptions createOptions() {
        final BaseCommandOptions options = super.createOptions();
        // MPCS-8379 10/03/16 - property moved to PostDownlinkProductProcessorAppOptions.java
        options.addOption(PostDownlinkProductProcessorAppOptions.PROCESS_ID_OPTION);

        return options;
    }


    /* (non-Javadoc)
     * @see jpl.gds.cli.legacy.app.AbstractPostDownlinkProductProcessorApp#init(java.lang.String[])
     */
    @Override
    public PostDownlinkProductProcessorOptions init(final String[] args) throws Exception {
        final PostDownlinkProductProcessorOptions options = super.init(args);

        updateProcess();

        // TODO move this or take out.  not in correct spot anymore.
        log.debug("Starting AutomationProcess with the following properties: cycleTime="
                + cycleTime + " selfKillTime=" + selfKillTime + " MaxErrors=" + maxConsecutiveErrors
                + " ProcessorId=" + this.processId + " ProcessToString=> " + getProcess(), processId);

        return options;
    }

    private void wakeUp() {
        /**
         * MPCS-6978 -  1/2015 - Was synching on the lock object and the wait
         * caused the monitor to be lost.  Use this instead.
         */
        synchronized(this) {
            this.notifyAll();
        }
    }

    /**
     * @throws InterruptedException
     */
    private void doSleep() throws InterruptedException {
        /**
         * MPCS-6978 -  1/2015 - Was synching on the lock object and the wait
         * caused the monitor to be lost.  Use this instead.  Notify on this.
         */		synchronized(this) {
            this.wait(cycleTime);
        }
    }

    /**
     * Finds all actions assigned to this process and will unclaim them.  Updates the process and stats table through the respective DAO.
     *
     * @throws AutomationException
     */
    protected void unclaimActions() throws AutomationException {
        /**
         * 	MPCS-4281 -  - After each batch, check to see if the action is still enabled.  If it is not, unclaim all actions
         *  for this process and exit.
         *
         *  MPCS-6544 -  - Adding calls to update the stats and process table.
         */
        int retryCount = 0;

        do {
            try {
                int actionCount = 0;
                actionInstance.startTransaction();
                /**
                 * MPCS-6544 -  - No claiming so just get all actions assigned to this process.
                 */
                for (final ProductAutomationAction action : actionInstance.getAllAssignedActions(getProcess())) {
                    log.info("Unclaiming action " + action.getActionId(), processId, action.getProduct().getProductId());
                    actionInstance.unclaimAction(action);

                    actionCount++;
                }

                actionInstance.commit();
                final ProductAutomationProcess process = getProcess();

                processDao.actionsReassigned(process, actionCount);

                break;
            } catch (final Exception e) {
                retryCount++;

                if (retryCount > 5) {
                    /**
                     * Change this to start warning after 5 failed attempts.  The first couple are not really errors since we rollback
                     * and try again.  These collisions happen often.
                     */
                    log.warn(String.format("Attempt %d to unclaim actions has failed: %s", retryCount, e.getMessage()));
                }
                actionInstance.rollback();
            } finally {
                actionInstance.closeSession();
            }
        } while (retryCount < 10);
    }

    /**
     * MPCS-6544 -  9/2014 - It is unnecessary for a process to do a claim on actions.
     */

    /**
     * Processes assigned actions.  Adheres to the max query size defined in the system configuration for each
     * group of actions worked on in a single cycle.  If work was done in a previous cycle it will continue to work.
     * If no work was done will wait for the specified cycle time from the config.
     */
    protected void doWork() {
        try {
            /**
             * MPCS-6544 -  -  No more claiming actions since the processes are being monitored by the arbiter.  Just get
             * a set of actions assigned to this process and get to work.
             */
            for (ProductAutomationAction action : actionInstance.getAllAssignedActionsWithLimit(getProcess())) {
                /**
                 * MPCS-6978 -  1/2015 - Sync on the lock object so the shutdown is not called
                 * while we are working on a product.
                 */
                synchronized (lock) {
                    if (!running) {
                        log.debug("No longer working so exiting working loop.");
                        // Shutdown was called.
                        break;
                    }

                    workWasDone = true;
                    log.info("Processing action: " + action.getActionId(), processId, action.getProduct().getProductId());
                    storeControllerClosed = false;

                    // MPCS-4330 -  - Get the product id only once at the top for the log messages.
                    final Long productId = action.getProduct().getProductId();

                    try {
                        // first, do the work on the product.  Do a catch on that separately because if there are issues,
                        // we can still add work within the hibernate session.
                        boolean ok = false;
                        IProductMetadata md = null;
                        Long now = 0L;
                        try {
                            // Do this first and flush the changes so if we get an error, we will not have done any work yet.
                            actionInstance.startTransaction();
                            actionInstance.setClaimedTime(action);
                            actionInstance.commit();

                            final IPostDownlinkProductProcessor proc = getProductProcessor();

                            // Just in case it was never set, check if the proc is null and error out if it is.
                            if (proc == null) {
                                throw new Exception("Product processor was not created");
                            }

                            statusInstance.startTransaction();
                            now = System.currentTimeMillis();
                            log.debug("Starting process products: " + now, processId, productId);

                            // 1/7/2012 -  - Not sure how this happened, but never added a started status.  Going to put that in now.
                            statusInstance.addStarted(action.getProduct(), action.getPassNumber());

                            /**
                             * MPCS-7265 -  - Add the started status and commit.  Doing this because in the case we have issues
                             * saving all the action info once the process does the work and we do a retry, this status will not
                             * get created.  Just make this a single unit of work and if it errors we roll back and don't do the actual
                             * PDPP work.
                             */
                            statusInstance.commit();

                            md = proc.processProduct(action.getProduct().getProductPath(), action.getProcess().getProcessId(), action.getProduct().getProductId());

                            final long diff = (System.currentTimeMillis() - now) / 1000;
                            log.debug("Processing time: " +
                                    String.format("%02d:%02d:%02d.%06d", TimeUnit.MILLISECONDS.toHours(diff), TimeUnit.MILLISECONDS.toMinutes(diff), TimeUnit.MILLISECONDS.toSeconds(diff), diff % 1000));

                            ok = true;
                        } catch (final ProductException e) {
                            // If we can not read the product, this is a failed product.
                            log.warn("Exception encountered when trying to process product.  Marking product " + action.getProduct().getProductPath() + " as failed: " + e.getMessage(), processId, productId);

                        } catch (final Exception e) {
                            e.printStackTrace();
                            log.warn("Unknown exception encountered: " + e.getMessage(), processId, productId);
                        }

                        log.info("Completed processing action: " + action.getActionId(), processId, productId);

                        /**
                         * MPCS-7265 -  5/2015 - If we get here and ok is true, meaning the product processing was completed
                         * we need to make sure that all the completed info is added no matter what.  Must catch any concurrent mod exceptions.
                         */
                        int updateErrorCount = 0;

                        do {
                            try {
                                if (ok) {
                                    // Add a completed status
                                    statusInstance.addCompleted(action.getProduct(), action.getPassNumber());
                                } else {
                                    // Add a failed status
                                    statusInstance.addFailed(action.getProduct(), action.getPassNumber());
                                }

                                // MPCS-4293 -  - Adding / Changing the parentPath column to instead be a
                                // Many-to-one mapping of a parent product.  Will make all things easier.
                                // If the pdpp was successful, will return a md object.  If it is the same as what was given, no need to add the new product.


                                if (ok && md != null && !action.getProduct().getProductPath().equals(md.getFullPath())) {
                                    /**
                                     * For non-MLS EMD files, dictionary metadata is not included. Just copy it from the parent.
                                     */
                                    Long fswVersion = action.getProduct().getFswBuildId();
                                    String fswDictionaryVersion = action.getProduct().getDictVersion();
                                    String fswDictionaryDirectory = action.getProduct().getFswDirectory();

                                    //MPCS-8568 12/12/16 - added. Uncategorized action now takes fswDirectory.
                                    statusInstance.addUncategorized(md.getFullPath(),
                                            action.getProduct(), // Changed from the parent path to a ref to the parent.
                                            fswVersion,
                                            fswDictionaryVersion,
                                            fswDictionaryDirectory,
                                            md.getSessionId(),
                                            md.getSessionHost(),
                                            md.getApid(),
                                            md.getVcid(),
                                            md.getSclkCoarse(),
                                            md.getSclkFine(),
                                            md.getIsCompressed() ? 1 : 0,
                                            0);
                                }

                                // If the product fails, or there is no resultant product, does not matter.
                                // in both cases, the action should be marked complete.
                                actionInstance.setCompletedTime(action);
                                actionInstance.commit();

                                /**
                                 * If we get here we are good and do not need to loop.
                                 */
                                break;
                            } catch (final Exception e) {
                                updateErrorCount++;
                                log.error(e.getMessage());

                                /**
                                 * MPCS-6978 -  - The action needs to be reread from the database if we
                                 * get here.  Otherwise the version will not get updated and no matter what this will
                                 * fail.
                                 */
                                actionInstance.rollbackAndClose();

                                /**
                                 * Get the action again.
                                 */
                                action = actionInstance.getSession().get(ProductAutomationAction.class, action.getActionId());

                                /**
                                 * A bit redundant but want to handle the error case of it erroring 10 times.
                                 */
                                if (updateErrorCount == 10) {
                                    throw new AutomationException("Attempted to save action values and encountered 10 consecutive errors for action " + action + " . Last error: " + e.getMessage());
                                }
                            }
                        } while(updateErrorCount < 10);

                        this.consectutiveErrorCount = 0;
                        lastProductProcessedTime = System.currentTimeMillis();
                        log.debug("===> starting next loop" + (lastProductProcessedTime - now), processId, productId);

                        processDao.actionCompleted(getProcess());
                    } catch (final Exception e) {
                        log.error("Error encountered when trying to process product.  Rolling back transaction: " + e.getMessage(), processId, productId);
                        actionInstance.rollbackAndClose();
                        this.consectutiveErrorCount++;
                        break;
                    }
                }
            }
        } finally {
            // Close the session after everything is done, or a rollback has been done.
            actionInstance.closeSession();
        }
    }

    /**
     * Checks the db to see if the arbiter has marked self as shutdown.  No need to hang around if no work is going to be assigned.
     */
    private boolean markedAsShutdown() {
        final ProductAutomationProcess self = getProcess();

        return self == null || self.getShutDownTime() != null || self.getKiller() != null;
    }

    /**
     * Checks to see if we are paused.  If we are, checks to see if we need to set the pause ack flag.  Returns a boolean
     * true we are paused, false we are not.
     *
     * @return TRUE if the process is paused, false if not
     */
    protected boolean checkAndSetPauseAck() {
        final ProductAutomationProcess process = getProcess();
        final int paused = process.getPause();
        final int pauseAck = process.getPauseAck();

        // This is very simple.  If paused and pauseAck are not equal, set pauseAck equal to paused.  That is it.
        if (paused != pauseAck) {
            try {
                processDao.startTransaction();
                process.setPauseAck(paused);
                processDao.updateProcess(process);
                processDao.commit();
            } catch (final Exception e) {
                log.error("Could not update pause acknowledge flag: " + e.getMessage(), processId);
                processDao.rollback();
            } finally {
                processDao.closeSession();
            }
        }
        // The process needs to be evicted so that it will be re-queried the next time this is called.
        processDao.getSession().evict(process);

        return paused == PAUSED_VALUE;
    }


    /* (non-Javadoc)
     * @see jpl.gds.cli.legacy.app.AbstractPostDownlinkProductProcessorApp#start()
     */
    @Override
    protected void start() {
        /*
         * 11/21/2012 -  - MPCS-4387 - When this starts will try to add the default amount of connections for a process.
         */
        processDao.increaseConnectionsDefault();
        running = true;

        try {
            while(running && System.currentTimeMillis() - lastProductProcessedTime < selfKillTime) {
                // Check the consecutive errors.  Throw an exception if it fails.
                if (consectutiveErrorCount > maxConsecutiveErrors) {
                    throw new AutomationException("Process had more than " + maxConsecutiveErrors + " consecutive errors encountered.  Exiting processes");
                } else if (markedAsShutdown()) {
                    // Next, make sure that the process has not been marked as shutdown by the arbiter.  If so, log and exit.
                    log.info("Process " + processId + " has been marked as shutdown.  Killing self.", processId);
                    break;
                } else if (checkAndSetPauseAck()) {
                    // MPCS-4248 -  - Check if we are paused.  If so, sleep for the cycle time and continue the loop.
                    log.info("Process has been marked as paused.  Waiting to be unpaused.", processId);

                    // Wait for the next cycle and continue.
                    doSleep();

                    continue;
                } else if (getProcess().getAction().getEnabled() == 0) {
                    unclaimActions();
                    running = false;
                } else {
                    /**
                     * MPCS-6544 -  - changing the work flow.  No longer need to claim actions we can do that as part of the
                     * work cycle.
                     */
                    doWork();
                }

                /**
                 * MPCS-6978 -  1/2015 - Check if we are running so we don't wait
                 * unnecessarily when we shutdown.
                 */
                if (running && !workWasDone) {
                    doSleep();
                    workWasDone = false;
                }
            }

            checkStoreController();
        } catch (final AutomationException e) {
            log.error(e.getMessage(), processId);
        } catch (final InterruptedException e) {
            // got an interrupt, just exit quietly.
        } catch (final HibernateException e) {
            log.error("Hibernate exception encountered.  Exiting process => " + e.getMessage(), processId);
        } catch (final Exception e) {
            log.error("Unknown exception encountered.  Exiting process => " + e.getMessage(), processId);
        }
    }

    /**
     * Checks to see if the store controller should be shutdown based on the last time we did any work and the
     * idle time to close db value set in the configuration.
     */
    private void checkStoreController() {
        // MPCS-4245 - Check idle time and shutdown store controllers if the idle time has been reached.
        if (!storeControllerClosed && System.currentTimeMillis() - lastProductProcessedTime > idleTimeToCloseDb) {
            log.debug("Process idle time has exceeded db connection idle time.  Closing store controllers", processId);
            try {
                /**
                 * MPCS-7078 -  2/2015 - Close method is not thread safe so must synchronize the calls.  Just call the
                 * super.close which is synchronized.
                 */
                super.close();
                storeControllerClosed = true;
            } catch (final Exception e) {
                log.error("Encountered error when trying to close store controllers: " + e.getMessage(), processId);
            }
        }
    }

    /* (non-Javadoc)
     * @see jpl.gds.cli.legacy.app.AbstractPostDownlinkProductProcessorApp#shutdown()
     */
    @Override
    protected void shutdown() {
        // Need to set the shutdown time for this process.
        // Once out of the loop, time to shtudown.  Mark the process as finished.
        synchronized (lock) {
            /**
             * MPCS-6978 -  1/2015 - synchronize on the lock.
             */
            running = false;
            wakeUp();

            /**
             * R8 refactor -  - Not sure how to test this or if we will even get here if it fails.  Taking this out for now.
             */
//			if (AutomationSessionFactory.getSessionFactory() == null) {
//				Log4jTracer.getDefaultTracer().fatal("Session factory did not initialize.  Finished with shutdown");

//			} else {
            /**
             * MPCS-6544 -  - 9/2014 - Must make this check more robust because the new arbiter architecture is
             * updating the process table all the time so we need to handle the stale state exception.  Also, going to set pauseAck
             * because this will cause the arbiter to stop assigning actions to this process.
             */

            int tryCount = 0;

            try {
                do {
                    try {
                        processDao.setPauseNow(processId, true);

                        processDao.startTransaction();
                        processDao.setProcessShutDownTime(getProcess());

                        unclaimActions();
                        processDao.commit();
                        break;
                    } catch (final NullPointerException e) {
                        log.error("Process ID (" + processId + ") supplied was not found in database.  Could not mark process as completed", processId);
                        break;
                    } catch (final AutomationException e) {
                        // Not much can be done at this point.  The arbiter will close this out, so just need to
                        // log and exit.
                        log.error("Failed to set the shutdown time for process with processId=" + processId, processId);
                        break;
                    } catch (final StaleObjectStateException e) {
                        log.info("Found stale state on process for shutdown attempt " + tryCount);
                        tryCount++;
                    } finally {
                        processDao.closeSession();
                    }
                } while (tryCount < 10);
            } finally {
                /*
                 * 11/21/2012 -  - MPCS-4387 - Just before exiting decrease the number of connections.
                 * No matter what, we want to decrease the number of connections.
                 */
                processDao.decreaseConnectionsDefault();
                log.info("Finished with shutdown and now exiting: " + getProcess(), processId);
            }
        }
        super.shutdown();
    }

    /**
     * Hibernate objects are inherently non-thread safe and should not be stored.  This will use the
     * ProcessDAO to get the process when it is needed.  The processId must be set for this to work.
     * If it is not, will return null.
     *
     * @return the ProductAutomationProcess object from the database associated with this process
     */
    protected ProductAutomationProcess getProcess() {
        return processDao.findProcess(processId);
    }

    /**
     * Will attempt to get the process from the database and mark as started.
     *
     */
    private void updateProcess() {
        ProductAutomationProcess process = null;

        int retryCount = 0;

        /**
         * MPCS-6544 -  9/2014 - Updating this whole method to be cleaner.
         */
        do {
            try {
                process = getProcess();

                if (process == null) {
                    log.info("Process instance was null...");
                    retryCount++;
                    Thread.sleep(500L);
                } else {
                    processDao.startTransaction();
                    processDao.setStartTime(process);

                    process.setPid(Long.valueOf(GdsSystemProperties.getPid()));
                    processDao.commit();
                    readyToRoll = true;
                }
            } catch (final StaleObjectStateException e) {
                log.info("Got a stale state on the process.  Going to reset retry, rollback transaction and try again.  Wish me luck...", processId);
                process = null;
                retryCount = 0;
                processDao.rollback();
            } catch (final Exception e) {
                log.error("Unknown exception caught.  Could not start process: " + e.getMessage(), processId);
                e.printStackTrace();
                processDao.rollback();
                retryCount++;
            } finally {
                processDao.closeSession();
            }
        } while (!readyToRoll && process == null && retryCount < processLookup);
    }


    /**
     * Parses the command line into a configuration object
     * @param commandLine the command line used to start this app
     * @return a PostDownlinkProductProcessorOptions object configured from the command line
     * @throws ParseException
     */
    @Override
    public PostDownlinkProductProcessorOptions getConfigurationOptions(final ICommandLine commandLine) throws ParseException {
        final PostDownlinkProductProcessorOptions options = super.getConfigurationOptions(commandLine);
        // MPC-8379 10/03/16 - can return a null instead of a Long, check it first.
        final Long pidVal = PostDownlinkProductProcessorAppOptions.PROCESS_ID_OPTION.parse(commandLine);
        if(pidVal != null) {
            processId = pidVal;
        }

        return options;
    }

    /**
     * Starts up the instance of the app, initializes it, sets up the actual processor, and executes
     * @param args the command line arguments
     */
    public static void main(final String[] args)  {
        final ApplicationContext appContext = SpringContextFactory.getSpringContext(true);

        // Must do a delay because it could take the arbiter a while to get set up and commit the process to the db.
        try {
            Thread.sleep(appContext.getBean(ProductAutomationProperties.class).getStartUpDelayMS());

        } catch (final InterruptedException e1) {
            TraceManager.getDefaultTracer().error("Got interrupt before initialization");
            System.exit(1);
        }

        final ReferenceAutomationProcessorApp app = new ReferenceAutomationProcessorApp(appContext);

        try {
            // Need to do a delay before we init, so the arbiter can commit the process to the database.
            final PostDownlinkProductProcessorOptions options = app.init(args);

            if (!app.isInitialized()) {
                throw new AutomationException("Process was unable to update process in database.");
            }

            System.err.println(String.format("Automation process with processId %d and mnemonic %s",
                    app.processId, app.getProcess().getAction().getMnemonic()));

            final IPostDownlinkProductProcessor pp = app.createProductProcessor(options);
            app.setProductProcessor(pp);

            TraceManager.getTracer(Loggers.PDPP)
                    .info("Processor initialized successfully.", app.processId);
            app.start();
            System.exit(0);
        } catch (final Exception e) {
            TraceManager.getDefaultTracer()
                    .error("Failed to start up automation process: " + e + " => " + e.getMessage());

            e.printStackTrace();
            System.exit(1);
        }
    }
}