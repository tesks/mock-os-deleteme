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
package jpl.gds.product.impl.builder;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.product.api.IPduType;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.builder.*;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.product.api.message.IPartReceivedMessage;
import jpl.gds.product.api.message.IProductMessageFactory;
import jpl.gds.product.api.message.InternalProductMessageType;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.product.impl.message.AgingTimeoutMessage;
import jpl.gds.product.impl.message.ForcePartialMessage;
import jpl.gds.shared.annotation.Jira;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.file.ISharedFileLock;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.QueuePerformanceData;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.SclkScetUtility;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AbstractDiskProductStorage contains common code responsible for writing data products and data product
 * parts to disk for a single virtual channel. It is also responsible for
 * triggering product assembly. It listens for PartReceived, ForcePartial, and
 * AgingTimeout messages. It generates PartialProduct and ProductAssembled
 * messages. Incoming product messages are queued to a blocking queue for writing,
 * in order to avoid blocking the incoming telemetry thread as much as possible.
 * When the product storage queue limit is reached, however, this object will block on
 * incoming messages.
 * <br>
 * This class also keeps a static table of all the DiskProductStorage objects in play.
 * DiskProductStorage objects are created and shutdown by ProductBuilder.
 *
 * @see IProductBuilderService
 */
public abstract class AbstractDiskProductStorage extends Thread implements IProductStorage {

    protected final Tracer log;


    /**
     * The product builder mission adaptor
     */
    protected IProductMissionAdaptor missionAdaptation;
    /**
     * The internal publication bus to use
     */
    protected IMessagePublicationBus messageContext;
    /**
     * The file object for the active product directory
     */
    protected File activeDirectory;
    /**
     * File object for the product publishing directory
     */
    private File publishedDirectory;

    /**
     * Set when EndOfData message received
     */
    private final AtomicBoolean endOfData = new AtomicBoolean(false);

    /**
     * Set true when we are shutting down
     */
    private final AtomicBoolean done = new AtomicBoolean(false);

    /**
     * Set true when message is accepted but not yet queued
     */
    private final AtomicBoolean inProgress = new AtomicBoolean(false);

    /**
     * Set true when thread has exited
     */
    private final AtomicBoolean threadDone = new AtomicBoolean(true);

    private final int vcid;
    private final BlockingQueue<IMessage> messageQueue;
    private final Map<Integer, AbstractDiskProductStorage> dpsTable;
    private ProductMessageSubscriber subscriber;
    private final int productLockRetryInterval;
    private final int productLockRetryCount;

    /**
     * Queue performance object for the message queue.
     */
    protected final QueuePerformanceData queuePerformance;

    /**
     * Tracks the largest queue length attained.
     */
    protected long highWaterMark;

    /**
     * Keep track of what product is currently being assembled
     */
    protected ConcurrentMap<String, Integer> assemblyMap;

    /**
     * moving this from a service to an instance variable that each
     * disk product storage holds on to.
     */
    protected final ProductScheduler scheduler;
    /**
     * Product builder configuration properties
     */
    protected final IProductPropertiesProvider productConfig;
    /**
     * Flag indicating if partials should be forced out upon shutdown
     */
    protected final boolean forcePartialsOnShutdown;
    /**
     * Current application context
     */
    protected final ApplicationContext appContext;
    /**
     * APID dictionary instance
     */
    protected final IApidDefinitionProvider apidDefs;
    /**
     * Factory for high volume product builder objects
     */
    protected final IProductBuilderObjectFactory productInstanceFactory;
    /**
     * Factory for product messages
     */
    protected final IProductMessageFactory messageFactory;
    /**
     * Received parts tracker instance
     */
    protected final IReceivedPartsTracker tracker;
    /**
     * Override to product output directory
     */
    protected final String productDirOverrideConfig;

    private final IProductOutputDirectoryUtil productOutputDirectoryUtil;

    /**
     * Allows product cache directories to be archived for debug.
     */
    private final boolean archiveCacheDirectories;
    private final SseContextFlag sseFlag;

    /**
     * Creates an instance of AbstractDiskProductStorage.
     *
     * @param context the current application context
     * @param vcid    the virtual channel ID of products to process
     */
    public AbstractDiskProductStorage(final ApplicationContext context, final int vcid) {
        this.appContext = context;
        log = TraceManager.getTracer(context, Loggers.TLM_PRODUCT);
        messageContext = appContext.getBean(IMessagePublicationBus.class);
        productInstanceFactory = appContext.getBean(IProductBuilderObjectFactory.class);
        messageFactory = appContext.getBean(IProductMessageFactory.class);
        tracker = appContext.getBean(IReceivedPartsTracker.class);
        this.sseFlag = appContext.getBean(SseContextFlag.class);

        productConfig = appContext.getBean(IProductPropertiesProvider.class);
        forcePartialsOnShutdown = productConfig.isForcePartialsOnShutdown();
        messageQueue = new LinkedBlockingQueue<>(productConfig.getDpsMsgQueueLimit());
        productLockRetryInterval = productConfig.getProductLockRetryInterval();
        productLockRetryCount = productConfig.getProductLockRetryCount();

        dpsTable = new HashMap<>();

        this.vcid = vcid;

        queuePerformance = new QueuePerformanceData(appContext.getBean(PerformanceProperties.class),
                "Product Storage (vcid " + vcid + ")", productConfig.getDpsMsgQueueLimit(), true, true, "messages");
        queuePerformance.setYellowBound(productConfig.getQueueYellowPercentage());
        queuePerformance.setRedBound(productConfig.getQueueRedPercentage());

        /*
         * Keep track of what product is currently being assembled
         */
        assemblyMap = new ConcurrentHashMap<>();

        synchronized (dpsTable) {
            dpsTable.put(vcid, this);
        }

        this.setName("Disk ProductStore " + vcid);

        this.scheduler = new ProductScheduler(productConfig, vcid, messageContext);
        this.apidDefs = appContext.getBean(IApidDefinitionProvider.class);
        this.productDirOverrideConfig = productConfig.getOverrideProductDir();
        this.archiveCacheDirectories = productConfig.isCacheArchived();

        productOutputDirectoryUtil = appContext.getBean(IProductOutputDirectoryUtil.class);
    }

    /**
     * Gets the DiskProductStorage object from the table of objects in play based
     * upon the given VCID.
     *
     * @param vcid the virtual channel number of the storage object to look for
     * @return DiskProductStorage object for the selected VCID, or null if
     * no match is found
     * @TODO R8 Refactor TODO - appears to be unused?
     */
    public AbstractDiskProductStorage getDiskProductStorageByVcid(final int vcid) {
        synchronized (dpsTable) {
            return dpsTable.get(vcid);
        }
    }

    /**
     * Sets the product builder mission adaptor object used for creating mission-specific
     * product-related objects.
     *
     * @param missionAdaptation the adaptor object for the current mission
     */
    @Override
    public void setMissionAdaptation(
            final IProductMissionAdaptor missionAdaptation) {
        this.missionAdaptation = missionAdaptation;
    }

    /**
     * Sets the directory to which products and parts are written.
     *
     * @param directory the File object for the product directory
     */
    @Override
    public void setDirectory(final File directory) {
        publishedDirectory = directory;
        if (!appContext.getBean(IVenueConfiguration.class).getVenueType().isOpsVenue()) {
            activeDirectory = new File(directory,
                    ProductStorageConstants.ACTIVE_DIR + "-" + vcid);
        } else {
            final String opsStorage = productConfig.getOpsStorageDir();

            if (opsStorage != null) {
                activeDirectory = new File(opsStorage + File.separator
                        + GdsSystemProperties.getSystemMissionIncludingSse(sseFlag.isApplicationSse())
                        + File.separator
                        + productConfig.getStorageSubdir(),
                        ProductStorageConstants.ACTIVE_DIR + "-" + vcid);
            } else {
                activeDirectory = new File(GdsSystemProperties.getGdsDirectory()
                        + File.separator
                        + IGeneralContextInformation.ROOT_OUTPUT_SUBDIR
                        + File.separator
                        + ReleaseProperties.getProductLine().toLowerCase()
                        + File.separator
                        + productConfig.getStorageSubdir(),
                        ProductStorageConstants.ACTIVE_DIR + "-" + vcid);
            }
        }

        log.info("Setting product publication directory to " + publishedDirectory);
        log.info("Setting product temporary directory to " + activeDirectory);
    }

    /**
     * Starts processing of messages by this object.
     */
    @Override
    public void startSubscriptions() {

        subscriber = new ProductMessageSubscriber();

        // PartReceivedMessages are generated by ProductInput and received here
        // for storage to the transaction log
        messageContext.subscribe(ProductMessageType.ProductPart, subscriber);

        // Aging timeout messages are generated by the Scheduler when a part has
        // not been seen for an in-progress product in some time. They are
        // received here, where a partial product will be generated.
        messageContext.subscribe(InternalProductMessageType.AgingTimeout, subscriber);

        // ForcePartial messages are generated by the scheduler when the session
        // has ended but products are still pending. They are received here,
        // where a partial product will be generated
        messageContext.subscribe(InternalProductMessageType.ForcePartial, subscriber);

        messageContext.subscribe(CommonMessageType.EndOfData, subscriber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storePart(final IProductPartProvider part)
            throws ProductStorageException {
        AssemblyTrigger triggerAssembly = AssemblyTrigger.NO_TRIGGER;
        ISharedFileLock fileLock = null;
        final String txId = part.getTransactionId();

        try {
            fileLock = this.productInstanceFactory.createFileLock(part.getVcid(), txId, getProductDirectory(part));
            final String infoMessage = "product ID: " + part.getTransactionId() + ", part number: " + part.getPartNumber() + ", lock file path: " + fileLock.getLockFilePath();
            final boolean obtained = obtainLock(fileLock, infoMessage);
            if (!obtained) {
                throw new ProductStorageException("Cannot obtain product file lock in DiskProductStorage");
            }

            /**
             * Reset the timer in the scheduler since we got a new part.
             */
            this.scheduler.resetTimerForPart(part);

            /**
             * Update the tracker with the required information based on the PDU type.  This
             * was moved to this abstract version so the other version did not have to deal with this in the different
             * store methods.
             */
            final IPduType pduType = part.getPartPduType();

            if (pduType.isMetadata()) {
                synchronized (tracker) {
                    tracker.addMpdu(part.getVcid(), txId, part.getMetadata().getErt());
                }
            } else if (pduType.isEnd()) {
                // This means a separate EPDU was received.
                synchronized (tracker) {
                    tracker.addEpdu(part.getVcid(), txId, part.getMetadata().getErt());
                }
            } else if (pduType.isEndOfData()) {
                // Data end types mean the EPDU was embedded in the last data part.  Must also
                // add this as a part.
                synchronized (tracker) {
                    /**
                     * Call to add part must be done first because it clears the tracker and loads it before adding and
                     * will remove the addEpdu call if done after it without saving.
                     */
                    tracker.addPart(part.getVcid(), txId, part.getPartNumber(), part.getMetadata().getErt());
                    tracker.addEpdu(part.getVcid(), txId, part.getMetadata().getErt());
                }
            } else { // No other option, must be a data PDU.
                // All other PDU types are data and are added to the parts tracker using the addPart.
                synchronized (tracker) {
                    tracker.addPart(part.getVcid(), txId, part.getPartNumber(), part.getMetadata().getErt());
                }
            }

            /*
             *
             * Updating of received parts tracker was outside of the file lock. (Bad.)
             * Also, received parts tracker info was cached. This does not work
             * when there are multiple processes building data products. The addPart() method
             * has now been updated to clear the parts tracker for the products, and
             * load the file every time.
             */

            // First check to see if the new part will trigger assembly of a
            // previous partial product. If so, get the last part stored and
            // assemble that product first.
            triggerAssembly = missionAdaptation.getAssemblyTrigger(part);
            if (triggerAssembly.equals(AssemblyTrigger.PROD_CHANGE)) {

                final IProductPartProvider lastPart = missionAdaptation
                        .getLastPart(part.getVcid());
                log.debug("Assembling previous product due to ", triggerAssembly, " ", lastPart.getTransactionId());
                try {
                    assembleProduct(lastPart, triggerAssembly, fileLock);
                    /**
                     * If we assemble, we need to clear the timer as well.
                     */
                    scheduler.stopTimerForPartDueToAssembly(lastPart);
                    missionAdaptation.clearLastPart(part.getVcid());
                } catch (final Exception e) {
                    log.error("Could not assemble previous product "
                            + lastPart.getTransactionId() + " due to error: " + e.getMessage());
                }
            }

            // Now store the new part
            final IProductStorageMetadata storageMetadata = missionAdaptation.storePartData(activeDirectory, part);
            missionAdaptation.storePartReceivedEvent(activeDirectory, part, storageMetadata);

            /*
             *
             * Persist the receive parts tracker information. Must happen AFTER the
             * storePartReceivedEvent() call, but before actual assembly.
             */
            synchronized (tracker) {
                tracker.store(part.getVcid(), txId);
            }

            // See if the new part triggers assembly of the current product
            triggerAssembly = missionAdaptation.getAssemblyTrigger(part);
            if (!triggerAssembly.equals(AssemblyTrigger.NO_TRIGGER)) {
                log.debug("Assembling current product due to ", triggerAssembly, " on part number ",
                        part.getPartNumber(), " ", txId);
                assembleProduct(part, triggerAssembly, fileLock);
                /**
                 * If we assemble, we need to clear the timer as well.
                 */
                scheduler.stopTimerForPartDueToAssembly(part);
                missionAdaptation.clearLastPart(part.getVcid());
            }

        } catch (final ProductException e) {
            log.warn("Unable to determine if product part is metadata only: "
                    + e.getMessage());
            throw new ProductStorageException("Could not store product part", e);
        } finally {
            try {
                if (fileLock != null) {
                    fileLock.release();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Runs a timer that will try to obtain a product lock. Number of retries
     * and interval for attempting are specified in the config. Storing the
     * part or assembling the product will wait until this thread finishes.
     *
     * @param fileLock    lock object that was created for the product part that
     *                    is currently being processed.
     * @param infoMessage message displayed in case lock cannot be obtained
     * @return true if lock obtained, false if not
     */
    protected boolean obtainLock(final ISharedFileLock fileLock, final String infoMessage) {
        /**
         * I want to simplify this.  First, creating an executor each time did not make sense because
         * it was making debugging hell since i created a new Executor and thread pool for every single
         * product part received.  Also, there was no reason to do this anyway because we are blocking on
         * the execution of the runnable future.  I am removing the executor and just doing the check
         * directly on the current thread.  It will be less complicated and act exactly the same way.
         */
        boolean obtained = false;
        int retryCount = 0;

        do {
            try {
                obtained = fileLock.lock();
            } catch (final IOException e) {
                //intentionally left empty, error cases are handled below
            } finally {
                retryCount++;
            }

            //success
            if (obtained) {
                if (retryCount > 1) {
                    log.info("Product file lock acquired for " + infoMessage);
                }
                break;
            }
            //if lock has not been obtained and this is not the last attempt, print this message
            else if (retryCount < productLockRetryCount) {
                log.info("Product file lock could not be obtained for " + infoMessage + " (" + retryCount + "/" + productLockRetryCount + ")");

                // Only wait if we have attempts left.
                try {
                    SleepUtilities.fullSleep(productLockRetryInterval);
                } catch (final ExcessiveInterruptException e) {
                    // Should never happen, so just ignore and move on.
                }
            }
            //print this message when lock has not been obtained and all of the retries have been exhausted
            else if (retryCount >= productLockRetryCount) {
                log.error("Product file lock could not be obtained after " + productLockRetryCount + " attempts for " + infoMessage);
            }
        } while (!obtained && retryCount < productLockRetryCount);

        return obtained;
    }

    /**
     * Gets a File object for the product output directory for the product containing
     * the given part.
     *
     * @param part product part
     * @return File object for directory
     */
    protected File getProductDirectory(final IProductPartProvider part) {

        final IVenueConfiguration vc = appContext.getBean(IVenueConfiguration.class);

        File destDir = new File(publishedDirectory, part.getDirectoryName());
        if (productDirOverrideConfig != null && !vc.getVenueType().isOpsVenue()) {
            destDir = new File(productDirOverrideConfig + File.separator + productConfig.getStorageSubdir() + File.separator + part.getDirectoryName());
        } else if (!vc.getVenueType().isOpsVenue()) {
            destDir = new File(publishedDirectory, part.getDirectoryName());
        } else {
            if (part.getMetadata().getDvtCoarse() != 0
                    && vc.getVenueType().isOpsVenue()) {
                final IAccurateDateTime scet = SclkScetUtility.getScet(part
                                .getMetadata().getSclk(), part.getMetadata().getErt(),
                        appContext.getBean(IContextIdentification.class)
                                .getSpacecraftId(),
                        log);
                destDir = new File(productOutputDirectoryUtil.getProductOutputDir(productDirOverrideConfig, scet, part
                        .getDirectoryName()));
            } else {
                destDir = new File(productOutputDirectoryUtil.getProductOutputDir(productDirOverrideConfig,null, part
                        .getDirectoryName()));
            }
        }

        return destDir;
    }

    /**
     * Assembles the product.
     *
     * @param part     product part that triggered the assembly
     * @param reason   why the product is being assembled
     * @param fileLock lock object for the product
     * @throws ProductStorageException if there is an issue assembling the product
     */
    protected abstract void assembleProduct(final IProductPartProvider part,
                                            final AssemblyTrigger reason, ISharedFileLock fileLock) throws ProductStorageException;

    /**
     * Cleans up after product assembly. Cleans up the received parts tracker. Removes temporary
     * files. Purges pending messages for the product from the message queue.
     *
     * @param sourceDir directory location of the active/temporary product
     * @param tx        product transaction object
     * @param md        product metadata
     */
    protected void cleanupAfterProduct(final File sourceDir, final IProductTransactionProvider tx,
                                       final IProductMetadataProvider md) {

        /*
         *
         * Force partial and aging timeout messages may have been queued
         * by the product scheduler, which does not have any knowledge of the
         * fact that part received messages were already queued for the
         * same product. These must be purged.
         */
        purgeAgingAndForceMessages(tx.getId());

        synchronized (tracker) {
            tracker.clearProduct(md.getVcid(),
                    tx.getId());
        }

        boolean ok;

        if (archiveCacheDirectories) {
            final File archive = new File(sourceDir.getParentFile(),
                    String.format("%s-%s", md.getFilename(), md.getProductVersion()));
            ok = sourceDir.renameTo(archive);
        } else {
            ok = deleteDir(sourceDir);
        }

        if (!ok) {
            log.error(String.format("Problem %s temporary product directory %s",
                    archiveCacheDirectories ? "archiving" : "deleting",
                    sourceDir.getPath()));
        }
    }

    /**
     * Purges aging timeout and force messages from the queue for the product
     * with the given transaction ID.
     *
     * @param tx the product transaction ID
     */
    private void purgeAgingAndForceMessages(final String tx) {


        for (final IMessage m : this.messageQueue) {
            if (m.isType(InternalProductMessageType.ForcePartial)) {
                /*
                 * Force partial messages are put into the queue when the scheduler sees
                 * an end of data message. But this can occur before this class has written
                 * all the parts in the queue. If a complete product is written, then the
                 * force partial event is unnecessary and should be removed.
                 */
                final ForcePartialMessage fpm = (ForcePartialMessage) m;
                if (fpm.getPart().getTransactionId().equals(tx)) {
                    log.info("Removing queued force partial message for complete product " + tx);
                    this.messageQueue.remove(m);
                }
            } else if (m.isType(InternalProductMessageType.AgingTimeout)) {
                /*
                 * Aging timeout messages are put into the queue when the scheduler sees
                 * too long a delay between part messages. If a complete product is written,
                 * then the aging timeout event is unnecessary and should be removed.
                 */
                final AgingTimeoutMessage fpm = (AgingTimeoutMessage) m;
                if (fpm.getPart().getTransactionId().equals(tx)) {
                    log.info("Removing queued aging timeout message for complete product " + tx);
                    this.messageQueue.remove(m);
                }
            } else if (m.isType(ProductMessageType.ProductPart)) {
                /*
                 * If we run across a new part message in the queue, then all bets are
                 * off. We may have written a complete product, but another part
                 * is outstanding, which may again create a partial product
                 * situation.  So if we see another part message, we do not
                 * remove any further aging timeout messages from
                 * the queue beyond the part message. They may still apply.
                 */
                final IPartReceivedMessage fpm = (IPartReceivedMessage) m;
                if (fpm.getPart().getTransactionId().equals(tx)) {
                    log.debug("Stopped removal of aging timeout messages from the queue because found a queued part received message for the product ",
                            tx);
                    break;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        threadDone.set(false);

        if (missionAdaptation == null) {
            throw new IllegalStateException("Mission adapation has not been set, product builder will not run.");
        }

        // Keep going until the request-to-stop flag (done) is set AND
        // the queue is empty AND nothing is being added to the queue.
        //
        // No matter how we exit the loop, set that the thread is done.
        //
        // Do NOT use isDone() to exit loop.

        try {
            while (!done.get() || !messageQueue.isEmpty() || inProgress.get()) {
                /* Added tracking of high water mark */
                highWaterMark = Math.max(messagesLeftInQueue(), highWaterMark);

                try {
                    final IMessage m = SleepUtilities.fullPoll(messageQueue, 2000L,
                            TimeUnit.MILLISECONDS).getValue();
                    if (m != null) {
                        if (m.isType(ProductMessageType.ProductPart)) {
                            handlePartReceivedMessage((IPartReceivedMessage) m);
                        } else if (m.isType(InternalProductMessageType.AgingTimeout)) {
                            handleAgingTimeoutMessage((AgingTimeoutMessage) m);
                        } else if (m.isType(InternalProductMessageType.ForcePartial)) {
                            handleForcePartialMessage((ForcePartialMessage) m);
                        } else if (m.isType(CommonMessageType.EndOfData)) {
                            handleEndOfDataMessage();
                        } else {
                            log.error("DiskProductStorage had queued unexpected message type");
                        }
                    }

                } catch (final ExcessiveInterruptException eie) {
                    log.error("DiskProductStorage.run could not poll: "
                            + ExceptionTools.rollUpMessagesOnly(eie));
                } catch (final Error e) {
                    log.error("Unexpected error processing data product: "
                            + e.toString());
                    e.printStackTrace();
                } catch (final Exception e) {
                    log.error("Unexpected error processing data product: "
                            + e.toString());
                    e.printStackTrace();
                }
                if (forcePartialsOnShutdown && done.get() && !inProgress.get() && messageQueue.isEmpty()) {
                    /**
                     * If we are to create partials on shutdown, the done flag was set via the shutdown method,
                     * we are not in the process or working on a product and the messageQueue has no more messages in it
                     * we will then get the aging timeouts for any pending products, add those to the internal queue and remove
                     * them from the product scheduler.  Doing this ensures that the last thing we do before closing down disk product
                     * is close out any pending partial products.
                     */
                    final Iterator<Entry<String, TimerTask>> iterator = scheduler.entrySet().iterator();

                    while (iterator.hasNext()) {
                        final Entry<String, TimerTask> es = iterator.next();
                        final AgingTimeoutTask task = (AgingTimeoutTask) es.getValue();

                        /*
                         *    Only perform if the task was active. If it was inactive,
                         *    then we've already build this partial product as it is.
                         *    However, if it was active, then we need to rebuild the partial.
                         */
                        // Cancel the timer.
                        if (task.cancel()) {
                            // Get the aging timeout message and add to our queue.
                            final IMessage m = task.getTimeoutMessage();
                            messageQueue.add(m);
                        }

                        // remove the task from the scheduler.
                        iterator.remove();
                    }
                }
            }
        } finally {
            threadDone.set(true);

            // Kill the scheduler timer as well so the thread can be GC'd.
            scheduler.shutdown();
        }
    }


    /**
     * Indicates if this object has been asked to stop
     * AND the thread has exited (which implies that the
     * queue is empty AND the last element has been processed).
     * <p>
     * Note that the thread could have died before we were asked
     * to stop, so we check the done flag as well. Normally
     * the done flag would have been set before the thread
     * finished.
     * <p>
     * No need to check the queue size of in-progress flag.
     *
     * @return True if truly done
     */
    @Override
    public boolean isDone() {
        return (done.get() && threadDone.get());
    }

    private void handlePartReceivedMessage(final IPartReceivedMessage m) {
        try {
            // Before we do anything, make sure we seem to have a valid part.
            // for a variety of reasons, we cannot detect all invalid packets.
            // From these, we get wild sort of parts which will screw up all
            // of the following logic. So I'm doing a sanity check here, which
            // won't catch everything, but should make things a little more
            // robust.
            final IProductPartProvider pp = m.getPart();
            if (pp.getVcid() != vcid) {
                log.trace("DiskProductStorage for vcid ", vcid, " is discarding ProductPartMessage for vcid ", pp.getVcid());
                return;
            }

            /**
             * Call the validate method on the part.
             */
            try {
                pp.validate();
            } catch (final ProductException e) {
                throw new ProductStorageException("Product part validation failed", e);
            }

            /*
             *
             * The code to update received parts tracker was outside of the file lock
             * and has been moved into storePart(). Sorry you missed its engagement at this
             * location.
             */

            storePart(m.getPart());
        } catch (final ProductStorageException e) {
            String message = "Couldn't store part for product " + m.getPart() + ": " + e.getMessage();

            if (e.getCause() != null) {
                message += " (" + e.getCause().getMessage() + ")";
            }
            log.error(message);
        }

    }

    private void handleAgingTimeoutMessage(final AgingTimeoutMessage m) {
        try {
            if (m.getVcid() != vcid) {
                log.trace("DiskProductStorage for vcid ", vcid, " is discarding AgingTimeoutMessage for vcid ", m.getVcid());
                return;
            }
            log.info("Assembling product due to latency timeout: ", m.getPart().getVcid(), "/",
                    m.getPart().getTransactionId());
            assembleProduct(m.getPart(), AssemblyTrigger.AGING_TIMER, null);

        } catch (final ProductStorageException e) {
            log.error("Couldn't assemble product " + m.getTransactionId()
                    + ": " + ExceptionTools.getMessage(e));
            log.debug(e);
        }
    }

    private void handleForcePartialMessage(final ForcePartialMessage m) {
        try {
            if (m.getVcid() != vcid) {
                log.trace("DiskProductStorage for vcid ", vcid, " is discarding AgingTimeoutMessage for vcid ", m.getVcid());
                return;
            }
            log.info("Assembling product due to end of data or session: " + m.getPart().getVcid() + "/" + m.getPart().getTransactionId());
            assembleProduct(m.getPart(),
                    (endOfData.get() ? AssemblyTrigger.END_DATA
                            : AssemblyTrigger.END_TEST),
                    null);
        } catch (final ProductStorageException e) {
            log.error("Couldn't assemble product " + m.getTransactionId()
                    + ": " + e.getMessage());
            log.debug(e);
        }
    }


    private void handleEndOfDataMessage() {
        endOfData.set(true);
    }


    /**
     * Ask to shut down processing in this object.
     */
    @Override
    public void shutdown() {
        done.set(true);
    }


    /**
     * Retrieves the number of messages in the product stroage queue.
     *
     * @return queue length
     */
    public int messagesLeftInQueue() {
        //no need to synchronize, getting queue size is atomic
        return messageQueue.size();
    }

    @Override
    public File getActiveProductDirectory() {
        return activeDirectory;
    }

    /**
     * This class implements the internal message subscriber for product part,
     * aging timeout, and force partial messages.
     *
     */
    private class ProductMessageSubscriber implements MessageSubscriber {

        /**
         * Internal message subscription event handler.
         * <p>
         * When message is accepted, flag that it is in progress. That is because it is not yet
         * physically on the queue but is logically on the queue.
         * <p>
         * Clear the in-progress flag no matter how we exit.
         */
        @Override
        public void handleMessage(final IMessage m) {
            try {
                if (done.get()) {
                    log.warn("Product builder is shutting down. Incoming messages will not be queued.");
                    return;
                }

                // We have accepted this message, tentatively
                inProgress.set(true);

                int vcid = -1;
                if (m.isType(ProductMessageType.ProductPart)) {
                    vcid = ((IPartReceivedMessage) m).getPart().getMetadata()
                            .getVcid();
                } else if (m.isType(InternalProductMessageType.AgingTimeout)) {
                    vcid = ((AgingTimeoutMessage) m).getVcid();
                } else if (m.isType(InternalProductMessageType.ForcePartial)) {
                    vcid = ((ForcePartialMessage) m).getVcid();
                }

                if (vcid != -1 && vcid != AbstractDiskProductStorage.this.vcid) {
                    // message is not for this product builder
                    return;
                }

                try {
                    while (!SleepUtilities.fullOffer(
                            messageQueue,
                            m,
                            productConfig.getDpsMsgQueueOfferTimeout(),
                            TimeUnit.MILLISECONDS).getTaken()) {
                        log.warn("DiskProductStorage message queue offer timeout, retrying (queue size/remaining: "
                                + messageQueue.size()
                                + "/"
                                + messageQueue.remainingCapacity() + ")");
                        SleepUtilities.checkedSleep(10);
                    }
                } catch (final ExcessiveInterruptException eie) {
                    log.error("DiskProductStorage.startSubscriptions subscriber could not offer: "
                            + ExceptionTools.rollUpMessagesOnly(eie));
                }
            } finally {
                inProgress.set(false);
            }
        }
    }


    /**
     * Stops all the subscriptions to the internal message bus.
     */
    @Override
    public void closeSubscriptions() {
        if (subscriber != null) {
            messageContext.unsubscribeAll(subscriber);
            subscriber = null;
        }

    }

    /**
     * Deletes all files and subdirectories under dir.  If a deletion fails, the method
     * stops attempting to delete and returns false.
     *
     * @param dir directory File object
     * @return true if successful
     */
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    private boolean deleteDir(final File dir) {
        if (dir.isDirectory()) {
            final String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                final boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    /**
     * Supplies performance data for the performance summary. This object just
     * has one performance object for the message queue.
     *
     * @return List of IPerformanceData objects
     */
    @Override
    public List<IPerformanceData> getPerformanceData() {
        queuePerformance.setCurrentQueueSize(messagesLeftInQueue());
        queuePerformance.setHighWaterMark(highWaterMark);
        return Arrays.asList((IPerformanceData) queuePerformance);

    }

    /*
     * Including methods to add and remove products from the
     * assembly map.  These are synchronized.  After updating the product builder the data was processed
     * too fast and the features were shutting down before we were able to assemble the products and have the
     * schedulers shutdown the timers, so at the end of data some complete products had duplicates created.
     */

    /**
     * Indicates if products are still being assembled.
     *
     * @return true or false
     */
    public boolean isAssemblingProducts() {
        synchronized (assemblyMap) {
            return !assemblyMap.isEmpty();
        }
    }

    /**
     * Gets the number of products still being assembled.
     *
     * @return product count
     */
    public int numberProductsBeingAssembled() {
        synchronized (assemblyMap) {
            return assemblyMap.size();
        }
    }

    @Override
    public boolean isBeingAssembled(final String transactionId) {
        synchronized (assemblyMap) {
            return assemblyMap.containsKey(transactionId);
        }
    }

    /**
     * Checks the map to see if the product with transactionId is currently being assembled.  It it is not
     * adds the transaction.
     *
     * @param transactionId product transaction ID
     * @return boolean indicating if we are currently working on the product.
     * True means the product was added and should be assembled.
     */
    protected boolean assemblingProductStart(final String transactionId) {
        synchronized (assemblyMap) {
            if (assemblyMap.containsKey(transactionId)) {
                return false;
            } else {
                assemblyMap.put(transactionId, 1);
                return true;
            }
        }
    }

    /**
     * Removes the transaction from the map.  Returns true if the product was present and was removed.
     *
     * @param transactionId the product transaction ID
     * @return true if removed, false if not
     * @TODO R8 Refactor TODO - looks strange, does not seem to do anything but return false if the
     * item was not in the assembly map in the first place
     */
    protected boolean assemblingProductFinished(final String transactionId) {
        synchronized (assemblyMap) {
            if (assemblyMap.containsKey(transactionId)) {
                return false;
            } else {
                assemblyMap.remove(transactionId);
                return true;
            }
        }
    }

    /**
     * <p>
     * Keep track of what product is currently being assembled
     *
     * @return a list of products currently in process of being assembled
     */
    public List<String> getProductsPendingAssemglyCompletion() {
        return new ArrayList<String>(assemblyMap.keySet());
    }
}
