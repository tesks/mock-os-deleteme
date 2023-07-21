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
package jpl.gds.watcher.responder.handlers;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jpl.gds.context.api.ISimpleContextConfiguration;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.context.api.message.IEndOfContextMessage;
import jpl.gds.context.api.message.IStartOfContextMessage;
import jpl.gds.message.api.app.MessageAppConstants;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.FileByteStream;
import jpl.gds.watcher.IResponderAppHelper;
import jpl.gds.watcher.responder.app.MessageResponderApp;
import jpl.gds.watcher.responder.app.ProductHandlerApp;
import jpl.gds.watcher.responder.app.ProductHandlerApp.ProductHandler;
import jpl.gds.watcher.responder.app.ProductHandlerApp.ProductHandlerException;


/**
 * ProductMessageHandler is the MessageHandler implementation used by
 * the message responder application to listen for ProductAssembled
 * messages and trigger special processing on the product data.
 *
 * We use a queue so as not to block message delivery unnecessarily.
 */
public class ProductMessageHandler extends AbstractMessageHandler 
{
    private static final String QUEUE_FULL = "'s queue is full at ";
    private static final String DISCARDING = ", discarding ";

    /** Check for flag file every so often */
    private static final long FLAG_WAIT = 5L * 1000L;

    /** Number of times to try opening a product file */
    private static final int MAX_TRIAL = 3;

    /** Time to wait between attempts to open a product file */
    private static final long OPEN_DELAY = 3L * 1000L;

    private static final String NAME  = "ProductMessageHandler";
    private static final String TNAME = "ProductQueueThread";

    /** How long to sleep while waiting for thread to drain and exit */
    public static final long DEFAULT_JOIN_TIMEOUT = 20000;

    /** How long to sleep before checking for shutdown */
    private static final long PERIODIC_TIMEOUT = 5L * 1000L;

    /** How long to sleep before reporting queue size */
    private static final long REPORT_TIMEOUT = 15L * 1000L;

    /** Configured queue limit for products */
    private static final int DEFAULT_QUEUE_LIMIT = 3000;

    /** Queue used by thread */
    private final BlockingQueue<IMessage> queue =
        new LinkedBlockingQueue<>();

    /** Shut-down flag */
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    /** Timer to run FileWatcher */
    private final Timer timer = new Timer(NAME + "Timer", true);

    /** Thread that handles products on queue */
    private final Thread thisThread = new Thread(new ProductQueueThread(), TNAME);

    /** Thread-started flag */
    private final AtomicBoolean isThreadStarted = new AtomicBoolean(false);

    private final AtomicReference<ISimpleContextConfiguration> currentContext =
        new AtomicReference<>(null);

    private final AtomicLong                                   productAssembledMessageCount = new AtomicLong(0L);
    private volatile long                                lastProductAssembledMessageTime;
    private long                                         numExpectedProductMessages   = 0L;

    private ProductHandlerApp appHelper = null;
    
    private int queueLimit = DEFAULT_QUEUE_LIMIT;
    private long joinTimeout = DEFAULT_JOIN_TIMEOUT;


    /**
     * Creates an instance of ProductMessageHandler.
     * @param appContext the current application context
     */
    public ProductMessageHandler(final ApplicationContext appContext)
    {
        super(appContext);

        thisThread.setDaemon(true);

        // Check for flag file every so often */


        final String flagFile = appContext.getBean(IGeneralContextInformation.class).getOutputDir() + 
                                File.separator +
                                String.format(MessageAppConstants.PRODUCT_HANDLER_FLAG_PATTERN, 
                                       MessageResponderApp.getInstance().getUnique());

        timer.schedule(new FileWatcher(flagFile), FLAG_WAIT, FLAG_WAIT);
    }


    /**
     * Gets the current context configuration known to this handler.
     * @return Context configuration object
     */
    public ISimpleContextConfiguration getCurrentContextConfiguration() {
        return this.currentContext.get();
    }

    /**
     * Gets the number of ProductAssembled messages received by this
     * handler.
     * @return the ProductAssembled message count
     */
    public long getHandledProductCount() {
        return this.productAssembledMessageCount.get();
    }


    /**
     * We try to limit the number of products on the queue, but not the other
     * kind of messages. Because of the thread, the queue size is only
     * approximate.
     *
     * @param m message to process
     */
    @Override
    public void handleMessage(final IExternalMessage m)
    {
        if (! isThreadStarted.getAndSet(true))
        {
            thisThread.start();
        }

        IMessage[] messages = null;
    
        try
        {
            messages = externalMessageUtil.instantiateMessages(m);
        }
        catch (final Exception e)
        {

            writeError(NAME + " could not process message: " + e, e);

            return;
        }

        if (messages == null)
        {
            return;
        }

        /* TODO - There is no guarantee that there are
         * no more messages coming in just because the shutdown flag was
         * seen. We should be processing products util there are no more, period.
         * Really, the whole asynchronous shutdown method here is flawed.
         */
//        if (shuttingDown.get())
//        {
//            // Don't complain if just heartbeats
//
//            if (notAllHeartbeat(messages))
//            {
//                writeError(NAME                                            +
//                           " received a message while shutting down, " +
//                           "discarded");
//            }
//
//            return;
//        }

        synchronized (this)
        {
            for (final IMessage message : messages)
            {
                final int    size = queue.size();

                if (message instanceof IStartOfContextMessage ||
                        message instanceof IContextHeartbeatMessage ||
                        message instanceof IEndOfContextMessage)
                {
                    // Try to queue no matter what the queue size is

                    if (! queue.offer(message))
                    {
                        // Should not happen
                        writeError(NAME                   +
                                   QUEUE_FULL +
                                   size                   +
                                   DISCARDING        +
                                   message.getType());
                    }
                }
                else if (message.isType(ProductMessageType.ProductAssembled))
                {
               
                    if (size < queueLimit)
                    {
                        if (! queue.offer(message))
                        {
                            // Should not happen
                            writeError(NAME                   +
                                       QUEUE_FULL +
                                       size                   +
                                       DISCARDING        +
                                       message.getType());
                        }
                    }
                    else
                    {
                        writeError(NAME                   +
                                   QUEUE_FULL +
                                   size                   +
                                   DISCARDING        +
                                   message.getType());
                    }
                }
                else
                {
                    writeError(NAME                                  +
                               " got an unrecognized message type: " +
                               message.getType());
                }
            }
        }
    }


    /**
     * Handle the message. Used by the thread.
     *
     * @param message Message to process
     */
    private void internalHandle(final IMessage message)
    {
        final IMessageType type = message.getType();

        try
        {
            if (message.isType(ProductMessageType.ProductAssembled))
            {
                handleProductMessage((IProductAssembledMessage) message);
            }
            else if (message instanceof IStartOfContextMessage)
            {
                startContext((IStartOfContextMessage) message);
            }
            else if (message instanceof IContextHeartbeatMessage)
            {
                startContext((IContextHeartbeatMessage) message);
            }
            else if (message instanceof IEndOfContextMessage)
            {
                handleEndOfContext((IEndOfContextMessage) message);
            }
            else
            {
                // Shouldn't happen since we checked earlier
                writeError(NAME                                  +
                           " got an unrecognized message type: " +
                           type);
            }
        }
        catch (final Exception e)
        {
            writeError(NAME + " could not process message: " + e, e);
        }
    }


    private void startContext(final IStartOfContextMessage message) {
        final ISimpleContextConfiguration newConfig = message.getContextConfiguration();
        writeLog(NAME + " got Start of Context message for context " + newConfig.getContextId().getNumber());

        this.currentContext.set(newConfig);
    }

    private void startContext(final IContextHeartbeatMessage message)
    {
        final ISimpleContextConfiguration newConfig = message.getContextConfiguration();

        if (this.currentContext.get() == null || !this.currentContext.get().getContextId().getNumber().equals(newConfig.getContextId().getNumber())) {
            writeLog(NAME + " got first Heartbeat message for context {" + newConfig.getContextId().getNumber() + "}");
            this.currentContext.set(newConfig);
        }
    }

    private void handleProductMessage(final IProductAssembledMessage message) {
        this.productAssembledMessageCount.incrementAndGet();
        this.lastProductAssembledMessageTime = System.currentTimeMillis();

        writeLog(NAME + " got ProductAssembled message: " + message.getOneLineSummary());
        final IProductMetadataProvider metadata = message.getMetadata();
        
        if (metadata == null) {
            writeError(NAME + " aborting processing of message because it doesn't include product metadata");
            return;
        }
        
        final String metadataProductType = metadata.getProductType().trim();

        if (metadataProductType == null) {
            writeError(NAME + " aborting processing of message because metadata's ProductType is null");
            return;
        }

        boolean found = false;
        for (final String s: appHelper.getProductNames()) {
            /*
             * Product handler using both product type and upper case product type
             * to be safe.
             */
            if (metadataProductType.matches(s) || metadataProductType.toUpperCase().matches(s)) {
                found = true;
            }
        }
        
        if (!found) {
            writeLog(NAME + " skipping message because ProductType=" +
                     metadataProductType.trim() +
                     " but looking for " +
                     appHelper.getProductNames());
            return;
        }
        
        final String fullpath = metadata.getFullPath();
        
        if (fullpath == null) {
            writeError(NAME + " aborting processing of message because it doesn't include product file path");
            return;
        }

        // Try a few times in case the file create has been queued.
        // We can do this because we are in a thread.
        // Remember that FileNotFoundException is an IOException
        // so must be caught first.

        FileByteStream bytestream = null;

        for (int trial = 0; trial < MAX_TRIAL; ++trial)
        {
            try
            {
                bytestream = new FileByteStream(fullpath, trace);

                break;
            }
            catch (final FileNotFoundException fnfe)
            {
                writeDebug(NAME                  +
                           " waiting for file: " +
                           fullpath);

                SleepUtilities.checkedSleep(OPEN_DELAY);
            }
            catch (final IOException ioe)
            {
                writeError(NAME                                        +
                           " aborting processing of message "          +
                           "because could not load stored product at " +
                           fullpath                                    +
                           ": "                                        +
                           ioe.toString(), ioe);

                return;
            }
        }

        if (bytestream == null)
        {
            writeError(NAME                                        +
                       " aborting processing of message "          +
                       "because could not load stored product at " +
                       fullpath                                    +
                       " after "                                   +
                       MAX_TRIAL                                   +
                       " attempts");
            return;
        }
        
        /*
         * Product handler using both product type and upper case product type
         * to be safe.
         */
        ProductHandler handler = appHelper.getProductHandler(metadata.getProductType());
        if (handler == null) {
            handler = appHelper.getProductHandler(metadata.getProductType().toUpperCase());
        }
        if (handler == null) {
            writeError(NAME + " aborting processing of message because ProductHandler doesn't exist");
            return;
        }
        
        try {
            handler.handle(metadata, bytestream);
        }
        catch (final ProductHandlerException phe) {
            writeError(NAME + " exception. " + phe.toString(), phe);
            return;
        }
    }


    /**
     * Shut down, waiting until the queue is drained and the thread exits.
     */
    @Override
    public void shutdown()
    {
        if (shuttingDown.get())
        {
            return;
        }

        while (numExpectedProductMessages > productAssembledMessageCount.get()
                && System.currentTimeMillis() - lastProductAssembledMessageTime < joinTimeout) {
            writeInfo(NAME + " has not received all expected messages. Waiting a little longer before shutdown...");
            SleepUtilities.checkedSleep(1000);
        }

        shuttingDown.set(true);

        if (isThreadStarted.get())
        {
            writeInfo(NAME + " is draining queue in order to shut down");

            final long timeout = System.currentTimeMillis() + joinTimeout;
            // Wait until updates seem to stop occurring

            while (System.currentTimeMillis() < timeout)
            {
                if (! thisThread.isAlive())
                {
                    break;
                }
                SleepUtilities.checkedJoin(thisThread, 5L * 1000L);
            }

            if (thisThread.isAlive())
            {
                writeError(NAME                          +
                           " timed out draining queue, " +
                        "exiting anyway. ");
            }
            if (numExpectedProductMessages > productAssembledMessageCount.get()) {
                writeError(NAME + " has not received all " + productAssembledMessageCount.get() + " of "
                        + numExpectedProductMessages + " expected messages and is shutting down");
            }
            else
            {
                writeInfo(NAME +
                          " has drained queue and is shutting down");
            }
        }
        else
        {
            writeInfo(NAME + " is shutting down");
        }

        synchronized (this)
        {
            appHelper.shutdown();
        }
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IMessageHandler#handleEndOfContext(IEndOfContextMessage)
     */
    @Override
    public void handleEndOfContext(final IEndOfContextMessage m) {
        final IContextKey tc = m.getContextKey();
        if (tc == null || tc.getNumber() == null) {
             writeLog(NAME + " received End of Context message for an unknown context; skipping");
             return;
        }

        writeLog(NAME + " received End of Context message for context " + tc.getNumber());

        this.currentContext.set(null);

        if (this.appHelper != null && this.appHelper.isExitContext()) {
            shutdown();
            MessageResponderApp.getInstance().markDone();
        }
    }


    /**
     * Shutdown because flag file detected.
     * 
     * @param expectedNumberOfMessages
     *            Number of expected messages
     */
    public void handleFlagFileDetected(final long expectedNumberOfMessages)
    {
        writeInfo(NAME + " found flag file, shutting down");

        currentContext.set(null);

        this.numExpectedProductMessages = expectedNumberOfMessages;

        shutdown();

        MessageResponderApp.getInstance().markDone();
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.watcher.IMessageHandler#setAppHelper(jpl.gds.watcher.IResponderAppHelper)
     */
    @Override
    public synchronized void setAppHelper(final IResponderAppHelper app)
    {
        this.appHelper = (ProductHandlerApp)app;
        this.joinTimeout = this.appHelper.getDrainTime();
        this.queueLimit = this.appHelper.getQueueLimit();
    }

    /** This class works off the product queue. */
    private class ProductQueueThread extends Object implements Runnable
    {
        private int oldSize = 0;


        /**
         * Safety wrapper for real run logic.
         */
        @Override
        public void run()
        {
            try
            {
                internalRun();
            }
            catch (final RuntimeException re)
            {
                writeError(TNAME + " handle unexpected error: " + re);

                throw re;
            }
            catch (final Exception e)
            {
                writeError(TNAME + " handle unexpected error: " + e);
            }
        }


        /**
         * Take from queue, and wait if nothing is available. If we are
         * shutting down, we wait until the queue is drained.
         */
        private void internalRun()
        {
            long reportTime = System.currentTimeMillis() + REPORT_TIMEOUT;

            while (! queue.isEmpty() || ! shuttingDown.get())
            {
                try
                {
                    final IMessage m = queue.poll(PERIODIC_TIMEOUT,
                                                  TimeUnit.MILLISECONDS);

                    if (m != null)
                    {
                        internalHandle(m);
                    }
                }
                catch (final InterruptedException ie)
                {
                    // Nothing to do
                }
                catch (final RuntimeException re)
                {
                    writeError(TNAME + " handle error: " + re);

                    throw re;
                }
                catch (final Exception e)
                {
                    writeError(TNAME + " handle error: " + e);
                }

                final long now = System.currentTimeMillis();

                if (now >= reportTime)
                {
                    reportTime = now + REPORT_TIMEOUT;

                    final int size = queue.size();

                    // Report if the queue is non-empty, or if it has just
                    // become empty.

                    if ((size > 0) || (oldSize > 0))
                    {
                        writeInfo(TNAME + " queue size is " + size);
                    }

                    oldSize = size;
                }
            }

            writeInfo(TNAME + " exits");
        }
    }


    /** Timer task to watch for flag file */
    private class FileWatcher extends TimerTask
    {
        private final File file;


        /**
         * Constructor.
         * @param fileName file to watch for
         */
        public FileWatcher(final String fileName)
        {
            file = new File(fileName);
        }


        /**
         * Check for file periodically. If we find it, we shut things down
         * and cancel ourselves.
         */
        @Override
        public void run()
        {
            if (file.exists())
            {
                writeInfo(NAME                +
                          " found flag file " +
                          file.getAbsolutePath());

                long numMessages = 0L;
                try (
                        FileInputStream fis = new FileInputStream(file);
                        DataInputStream din = new DataInputStream(fis)) {
                    numMessages = din.readLong();
                }
                catch (final IOException e) {
                    writeError(NAME + " had error trying to read number of expected"
                            + " product messages from flag file. Not all products maybe" + " be processed as a result: "
                            + e);
                }

                try
                {
                    handleFlagFileDetected(numMessages);
                }
                catch (final RuntimeException re)
                {
                    writeError(NAME                                +
                               " had problem handling flag file: " +
                               re);

                    throw re;
                }
                catch (final Exception e)
                {
                    writeError(NAME                                +
                               " had problem handling flag file: " +
                               e);
                }
                finally
                {
                    // We're no longer needed, and neither is the timer

                    cancel();

                    timer.cancel();
                }
            }
        }
    }
}
