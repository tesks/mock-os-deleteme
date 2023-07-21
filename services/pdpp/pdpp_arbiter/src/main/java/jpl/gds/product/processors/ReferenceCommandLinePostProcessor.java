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

package jpl.gds.product.processors;

import jpl.gds.dictionary.api.mapper.IFswToDictMapping;
import jpl.gds.dictionary.api.mapper.IFswToDictionaryMapper;
import jpl.gds.product.AbstractPostDownlinkProductProcessor;
import jpl.gds.product.PdppApiBeans;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.file.IProductFilename;
import jpl.gds.product.api.file.IProductFilenameBuilderFactory;
import jpl.gds.product.api.file.IProductMetadata;
import jpl.gds.product.automation.AutomationException;
import jpl.gds.product.automation.app.AbstractPostDownlinkProductProcessorApp;
import jpl.gds.product.automation.hibernate.IAutomationLogger;
import jpl.gds.product.automation.hibernate.dao.*;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationAction;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationClassMap;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProcess;
import jpl.gds.product.automation.hibernate.entity.ProductAutomationProduct;
import jpl.gds.product.utilities.file.ReferenceProductFilename;
import jpl.gds.product.utilities.file.ReferenceProductMetadata;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.SpringContextFactory;
import org.hibernate.StaleObjectStateException;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Initial reference implementation copied from M20.
 * Comments were brought over intact for their historical value.
 */
public class ReferenceCommandLinePostProcessor extends AbstractPostDownlinkProductProcessorApp {

    private final IAutomationLogger log;

    private static final int PAUSE_ACK_SLEEP = 500;
    private String mnemonic;

    private SortedSet<Long> buildIds;
    /**
     * MPCS-8377  8/18/2016 - adding flag to indicate if the clean up step is required.
     */
    private boolean wasFullyInitialized = false;

    private final ProductAutomationClassMapDAO classMapDao;
    private final ProductAutomationProcessDAO processDao;
    private final ProductAutomationActionDAO actionInstance;
    private final ProductAutomationStatusDAO statusInstance;
    private final ProductAutomationProductDAO productDao;

    private final IFswToDictionaryMapper mapper;


    public ReferenceCommandLinePostProcessor(final ApplicationContext appContext) {
        super(appContext);
        log = appContext.getBean(IAutomationLogger.class);
        this.classMapDao = appContext.getBean(ProductAutomationClassMapDAO.class);
        this.processDao = appContext.getBean(ProductAutomationProcessDAO.class);
        this.actionInstance = appContext.getBean(ProductAutomationActionDAO.class);
        this.statusInstance = appContext.getBean(ProductAutomationStatusDAO.class);
        this.productDao = appContext.getBean(ProductAutomationProductDAO.class);
        this.mapper = appContext.getBean(IFswToDictionaryMapper.class);

    }

    @Override
    public PostDownlinkProductProcessorOptions init(final String[] args) throws Exception {
        mnemonic = GdsSystemProperties.getPdppMnemonicProperty();

        if (mnemonic == null) {
            throw new Exception("Mnemonic system property not set.  No way to know what class needs to be created");
        }

        processingOptions = super.init(args);

        final Long buildId = processingOptions.getFswBuildId();

        if (buildId != null) {
            // Create a new set and add this bad boy.
            buildIds = new TreeSet<Long>();
            buildIds.add(buildId);
        } else {
            // This is more intensive.  There could be multiple build ID's for the same dict version if wsts dictionaries are in
            // the mapper file.
            final String dictVer = processingOptions.getOverridingDictionary();

            if (dictVer == null) {
                throw new Exception("FSW Build ID as well as the Dictionary version could not be found.");
            }

            buildIds = appContext.getBean(IFswToDictionaryMapper.class).getBuildVersionIds(dictVer);

            // Last check to make sure the dict maps to something.
            if (buildIds.isEmpty()) {
                throw new Exception(String.format("Dictionary version %s did not map to any FSW Build ids in the mapper file", dictVer));
            }
        }

        //MPCS-8181  08/03/16 - added updateProcess
        updateProcess();

        /**
         * MPCS-8377  8/18/2016 - If we get here everything was initialized and we are going to attempt real work.
         */

        wasFullyInitialized = true;

        return processingOptions;
    }

    /**
     * Based on the property Mnemonic does a lookup to find the product processor class that needs to be created for this instance.  Will try to create the
     * object and set the processor object.
     *
     * @param options
     * @return
     * @throws Exception
     */
    public IPostDownlinkProductProcessor createProductProcessor(final PostDownlinkProductProcessorOptions options) throws Exception {
        return appContext.getBean(IPostDownlinkProductProcessor.class, options, mnemonic);
    }

    /**
     * This works with its own transactions so it must NOT be called
     * from inside of one.
     *
     * @return boolean true if all the processes are paused.
     */
    private boolean pauseProcesses() {

        // Need to check for each build ID, so we can start this with true since each pause request will try to && this value to
        // find out if all the processes have been paused for each build id.
        boolean isPaused = true;

        for (final Long buildId : buildIds) {
            log.info("Attempting to pause all automation processes for mnemonic " + mnemonic + " and FSW Build ID " + buildId);

            // Would have already had an issue when class was instantiated
            final IFswToDictMapping dictionary = mapper.getDictionary(buildId);

            boolean paused = false;

            try {
                processDao.startTransaction();
                processDao.pauseProcess(dictionary, mnemonic);
                processDao.commit();


                // Wait for the process to be paused.  Only wait for so long.
                for (int tries = 10; tries > 0; tries--) {
                    paused = processDao.isPaused(dictionary, mnemonic);

                    if (paused) {
                        log.info("All automation processes for mnemonic " + mnemonic + " and FSW Build ID " + buildId + " have been paused");
                        break;
                    } else {
                        processDao.getSession().clear();
                        Thread.sleep(PAUSE_ACK_SLEEP);
                    }
                }
            } catch (final Exception e) {
                log.error("Could not pause processes: " + e.getMessage());
                processDao.rollback();
            } finally {
                processDao.closeSession();
                isPaused = isPaused && paused;
            }
        }

        return isPaused;
    }

    /**
     * This works with its own transactions so it must NOT be called
     * from inside of one.
     *
     * @return boolean true if all the processes are unpaused.
     */
    private boolean unpauseProcesses() {
        boolean isUnpaused = true;

        for (final Long buildId : buildIds) {
            log.info("Attempting to unpause all automation processes for mnemonic " + mnemonic + " and FSW Build ID " + buildId);

            // Would have already had an issue when class was instantiated
            final IFswToDictMapping dictionary = mapper.getDictionary(buildId);

            boolean unpause = false;

            try {
                processDao.startTransaction();
                processDao.unpauseProcess(dictionary, mnemonic);
                processDao.commit();
                // Wait for the process to be paused.  Only wait for so long.
                for (int tries = 10; tries > 0; tries--) {
                    unpause = processDao.isUnpaused(dictionary, mnemonic);

                    if (unpause) {
                        log.info("All automation processes for mnemonic " + mnemonic + " and FSW Build ID " + buildId + " have been unpaused");
                        break;
                    } else {
                        // Clear the cache to make sure we get the updates in time.  To pick up changes in place we need to close
                        // the session and start a new transaction.  I don't want to do that here, so just clear the cache, which is
                        // basically the same thing.
                        processDao.getSession().clear();
                        Thread.sleep(PAUSE_ACK_SLEEP);
                    }
                }
            } catch (final Exception e) {
                log.error("Could not pause processes: " + e.getMessage());
                processDao.rollback();
            } finally {
                processDao.closeSession();
                isUnpaused = isUnpaused && unpause;
            }
        }
        return isUnpaused;
    }

    /**
     * Very simple method, but want to be able to tell when this fails.
     *
     * @param fullProductPath - the full product path to either an data file or metadata file.
     * @return
     */
    protected IProductMetadata getMetadata(final String fullProductPath) {
        ReferenceProductMetadata md = new ReferenceProductMetadata(appContext);

        /**
         * MPCS-8337  9/12/2016 - Use the new builder pattern to create the product file name objects.
         */
        try {
            // MPCS-11653 - need to specify the PDPP factory, since another factory is defined in the product bootstrap
            final IProductFilename fn = appContext.getBean(PdppApiBeans.PDPP_PRODUCT_FILENAME_BUILDER_FACTORY, IProductFilenameBuilderFactory.class)
                    .createBuilder()
                    .addFullProductPath(fullProductPath)
                    .build();

            md.loadFile(fn.getMetadataFilePath());

            md.setPartial(fn.isPartial());
        } catch (final Exception e) {
            log.error("Could not create metadata object for product " + fullProductPath);
            md = null;
        }

        return md;
    }

    /**
     * Order of operation for this method:
     * 1.  Pause all processes with the same build id and action.  Wait until it has been paused.
     * 2.  Figure out what products need to be worked and what has already been done.
     * 3.  Add a categorized status (along with the product) for all products in the working list.
     * 4.  Add an action for the product.
     * 5.  Do the work on the product.
     * 6.  Mark the completion status and mark action completed..
     */
    @Override
    protected void start() {
        final ProductAutomationProcess process = processDao.getCommandLineProcess();

        // Puase all processes first.
        if (pauseProcesses()) {
            // Work through the list of products.

            log.info("-------------------------------------------------------");
            for (String productPath : inputFileNames) {

                /*
                 * MPCS-8181 08/04/16 - added. If relative paths are given on the command line an additional product
                 *           is being created.
                 */

                //convert the potentially relative path to abosolute
                final Path inputPath = Paths.get(productPath);
                productPath = inputPath.toAbsolutePath().toString();

                final boolean completed = statusInstance.isProductCompleted(productPath);

                try {
                    statusInstance.startTransaction();

                    if (completed && !forceReprocess) {
                        log.warn("Product has already been processed and the force flag is not set.  Skipping product: " + productPath);
                    } else { // Either the force flag is set, or it has not been completed.
                        // Add a categorized status. Have to get the metadata object for this first.
                        final IProductMetadata md = getMetadata(productPath);

                        // Check if it is null, if so can not do any more work.
                        if (md == null) {
                            // Could not read the deal and can not put in the product so no work can be done.  Will just have to
                            // work with the output message.
                        } else {
                            final Long passNumber = statusInstance.addCategorized(productPath,
                                    null, // parentPath,
                                    md.getFswVersion(), // fswBuildId,
                                    md.getEmdDictionaryVersion(), // dictVersion,
                                    md.getEmdDictionaryDir(), //fswDirectory,
                                    md.getSessionId(), // sessionId,
                                    md.getSessionHost(), // sessionHost,
                                    md.getApid(), // apid,
                                    md.getVcid(), // vcid,
                                    md.getSclkCoarse(), // sclkCoarse,
                                    md.getSclkFine(), // sclkFine,
                                    0, //md.isCompressed() ? 1 : 0,
                                    0
                            );

                            // No action will be created because actions are tied to a automated process.  So just do the work.
                            // MPCS-4248 - This was incorrectly not catching product exceptions here and making a failed
                            // status.
                            final ProductAutomationProduct product = productDao.getProduct(productPath);
                            ProductAutomationAction action = actionInstance.findAction(product, passNumber);
                            IProductMetadata childMetadata;

                            // Need to find any actions for this product and set them to claimed.  If there is an action,
                            // then set the processor id to be the default for a command line run and save the action.

                            // If the action is null, need to create a new action.
                            if (action == null) {
                                action = new ProductAutomationAction(classMapDao.getClassMap(mnemonic), process, product, passNumber);
                            } else {
                                action.setProcess(process);
                            }

                            actionInstance.startTransaction();
                            actionInstance.setClaimedTime(action);
                            actionInstance.commit();

                            processDao.actionAssigned(process);

                            try {

                                statusInstance.addStarted(product, passNumber);

                                childMetadata = getProductProcessor().processProduct(md, action.getProcess().getProcessId(), action.getProduct().getProductId());
                            } catch (final ProductException pe) {
                                log.warn("Exception encountered when processing product: " + pe.getMessage());
                                childMetadata = null;
                            }

                            // Add a completed status if we get here.
                            if (childMetadata == null) {
                                // If the metadata is null, it failed.
                                statusInstance.addFailed(product, passNumber);
                            } else {
                                // Add a completed action.
                                statusInstance.addCompleted(product, passNumber);

                                // for Reference, emd file does not contain fsw dictionary metadata.
                                // Copy from parent.
                                childMetadata.setFswVersion(product.getFswBuildId());
                                childMetadata.setEmdDictionaryDir(product.getFswDirectory());
                                childMetadata.setEmdDictionaryVersion(product.getDictVersion());
                            }

                            processDao.actionCompleted(process);

                            // now set the action to complete
                            actionInstance.setCompletedTime(action);

                            // If the childMetadata is there, need to add a new status.
                            // Need to check the metadata paths for the parent and child.  If they are different, means there is a new product.
                            if (childMetadata != null && !md.getFullPath().equals(childMetadata.getFullPath())) {
                                log.debug("Child product was created: " + productPath + " --> " + childMetadata.getFullPath());
                                statusInstance.addUncategorized(childMetadata.getFullPath(),
                                        action.getProduct(), // parentPath,
                                        childMetadata.getFswVersion(), // fswBuildId,
                                        childMetadata.getEmdDictionaryVersion(), // dictVersion,
                                        childMetadata.getEmdDictionaryDir(), // fswDirectory,
                                        childMetadata.getSessionId(), // sessionId,
                                        childMetadata.getSessionHost(), // sessionHost,
                                        childMetadata.getApid(), // apid,
                                        childMetadata.getVcid(), // vcid,
                                        childMetadata.getSclkCoarse(), // sclkCoarse,
                                        childMetadata.getSclkFine(), // sclkFine,
                                        0,//childMetadata.isCompressed() ? 1 : 0,
                                        0
                                );
                            }
                        }
                    }

                    statusInstance.commit();
                } catch (final Exception e) {
                    log.warn("Could not do work on input product due to exception: " + e.getMessage());
                    statusInstance.rollback();
                } finally {
                    statusInstance.closeSession();
                }
            }
        } else {
            log.error("Processes did not pause in the alloted time.  No product processing will be conducted." );
        }

        // Either way, try to unpause all the processes.
        if (unpauseProcesses()) {
            log.debug("All processes have been unpaused.");
        } else {
            log.error("Could not unpause all processes in the alloted time");
        }

        log.info("-------------------------------------------------------");

        try {
            if (null != getProductProcessor()) {
                log.info("Processing Complete.");
                log.info(String.format("%4d total product(s) processed:", getProductProcessor().getTotalProductsProcessed()));
                log.info(String.format("    %4d product(s) processed successfully.", getProductProcessor().getProductsSuccessfullyProcessed()));
                log.info(String.format("    %4d product(s) aborted due to dictionary mismatch.", getProductProcessor().getProductsAbortedDueToDictionaryMismatch()));
                log.info(String.format("    %4d product(s) failed for other reasons.", getProductProcessor().getProductsFailedForOtherReasons()));
            }
        } catch (final PostDownlinkProductProcessingException e) {
            // Should never happen at this point.  If it does, well just do nothing.
        }

        close();
        shutdown();
        log.info("Shutting down.");
    }

    /**
     * Main method, called when any of the individual PDPP action scripts are called.
     *
     * @param args command line arguments split up as an array.
     */
    public static void main(final String args[]) {
        final ApplicationContext appContext = SpringContextFactory.getSpringContext(true);

        final ReferenceCommandLinePostProcessor app = new ReferenceCommandLinePostProcessor(appContext);

        try {
            final PostDownlinkProductProcessorOptions options = app.init(args);

            final IPostDownlinkProductProcessor pp = app.createProductProcessor(options);
            app.setProductProcessor(pp);

            app.start();

            /**
             * The apps were not exiting after everything was done.  Needed to add this to make them close.
             */
            System.exit(0);

        } catch (final Exception e) {
            //  - MPCS-6467 - Invocation exceptions were just showing up as null.  This will show
            // the cause exception message when exiting.
            TraceManager.getDefaultTracer()
                    .error(e.getCause() == null ? e.getMessage() : e.getCause().getMessage());

            System.exit(1);
        }
    }

    /*
     * MPCS-8181 08/04/16 - Added updateProcess. A process entry must be used in the database for the command line process,
     * might as well keep it up to date with the info from its most recent usage
     */
    private void updateProcess(){
        ProductAutomationProcess process = null;
        final ProductAutomationClassMap actionName = classMapDao.getClassMap(mnemonic);


        //get the hostname
        String processHost;
        try {
            processHost = java.net.InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            processHost = "localhost";
        }

        int retryCount = 0;

        do {
            try {

                //update the host name and actionName/mnemonic
                process = processDao.getCommandLineProcess();

                processDao.startTransaction();

                //set/clear all of the members because it may have been used previously
                process.setAction(actionName);
                process.setProcessHost(processHost);
                process.setInitializeTime(new Timestamp(System.currentTimeMillis()));
                // it's going to run right away
                process.setStartTime(new Timestamp(System.currentTimeMillis()));
                process.setShutDownTime(null);
                process.setKiller(null);
                process.setAssignedActions((long) 0);
                process.setCompletedActions((long) 0);
                process.setLastCompleteTime((long) 0);
                process.setPause(0);

                processDao.commit();
            } catch (final StaleObjectStateException e) {
                log.info("Got a stale state on the process.  Going to reset retry, rollback transaction and try again.  Wish me luck...");
                process = null;
                retryCount = 0;
                processDao.rollback();
            } catch (final Exception e) {
                log.error("Unknown exception caught.  Could not start process: " + e.getMessage());
                e.printStackTrace();
                processDao.rollback();
                retryCount++;
            } finally {
                processDao.closeSession();
            }
        } while (process == null && retryCount < 10);
    }

    // MPCS-8181  08/04/16 - added shutdown hook. now the command line process can be properly annotated as shut down when complete.
    @Override
    protected void shutdown(){
        /**
         * MPCS-8377  8/18/2016 - Only clean up if required.
         */
        if (wasFullyInitialized) {
            final ProductAutomationProcess process = processDao.getCommandLineProcess();

            try {

                processDao.setPauseValue(process, 1);
                processDao.setProcessShutDownTime(process);

            } catch (final AutomationException e) {
                log.error("Failed to set the shutdown time for the process");
                // do nothing, we're done anyway...
            }
        }

        super.shutdown();
    }

}