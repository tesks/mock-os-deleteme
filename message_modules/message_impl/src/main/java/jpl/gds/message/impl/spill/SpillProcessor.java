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
package jpl.gds.message.impl.spill;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jpl.gds.message.api.spill.SpillProcessorException;
import jpl.gds.message.impl.spill.SpillFileFactory.SpillFileFactoryException;
import jpl.gds.message.impl.spill.SpillHandler.SpillHandlerException;
import jpl.gds.message.impl.spill.SpillSerializer.SpillSerializerException;
import jpl.gds.message.api.spill.ISpillProcessor;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.thread.SleepUtilities.OfferStatus;

/**
 * Runs spill file processing. We act like a queue, trying to keep the target
 * queue at the specified size, spilling elements that arrive too rapidly to
 * files on disc. Essentially, we extend the target queue into a queue of
 * unbounded size, and never block the sender. If enable is false, we just pass
 * through to the target queue.
 * 
 * @param <T> any type that extends Serializable
 */
public class SpillProcessor<T extends Serializable> implements ISpillProcessor<T> {

    /**
     * Private thread to handle spilling.
     */
    private class SpillThread extends Thread {
        private volatile boolean _threadShutDown = false;

        /**
         * Construct a SpillThread.
         */
        public SpillThread() {
            super("SpillThread");

            setPriority(Thread.MAX_PRIORITY);

            setDaemon(true);
        }

        /**
         * Thread processing, handling all exceptions. Since this is a thread,
         * rethrowing isn't necessary, except for ThreadDeath.
         */
        @Override
        public void run() {
            try {
                safeRun();

                SpillProcessor.this._trace.trace(ME , ".run Shutting down " ,
                        SpillProcessor.this._name);
            } catch (final ThreadDeath td) {
                // Don't catch this one
                throw td;
            } catch (final RuntimeException re) {
                // Split out to make FindBugs happy
                SpillProcessor.this._trace.error(ME , ".run Shutting down " ,
                        SpillProcessor.this._name , ": " , rollUpMessages(re), ExceptionTools.getMessage(re), re);
            } catch (final Throwable t) {
                SpillProcessor.this._trace.error(ME , ".run Shutting down " ,
                        SpillProcessor.this._name , ": " , rollUpMessages(t), ExceptionTools.getMessage(t), t);
            }
        }

        /**
         * Actual spill processing high-level logic is here.
         */
        private void safeRun() {
            while (true) {
                final T object = SleepUtilities.checkedPoll(_queue, _timeout,
                        TimeUnit.MILLISECONDS).getOne();
                if (object != null) {
                    // We have a message

                    if (!spilling()) {
                        if ((_targetQueue.remainingCapacity() == 0)
                                || !_targetQueue.offer(object)) {
                            // Couldn't get rid of it; spill it
                            safeSpill(object);
                        }
                    } else {
                        // Already spilling, spill again
                        safeSpill(object);

                        // But we may still be able to unspill

                        unspillIfPossible();
                    }

                    continue;
                }

                // No message

                if (!spilling()) {
                    if (threadShuttingDown()) {
                        _trace.trace(ME , ".safeRun Shutting down " , _name);
                        break;
                    }

                    continue;
                }

                // No message and spilling

                unspillIfPossible();
            }
        }

        /**
         * Request thread to shut down when there is no work.
         */
        public synchronized void threadShutDown() {
            this._threadShutDown = true;
        }

        private synchronized boolean threadShuttingDown() {
            return this._threadShutDown;
        }
    }

    private static final String ME = "SpillProcessor";
    private static final String PREFIX = "SFP_";
    private static final int TRY_UNIQUE = 100;
    private static final long DELAY_UNIQUE = 500L;

    private static final long JOIN_GIVE_UP = 30L * 1000L;

    private static final long PUT_RETRY = 5L * 1000L;
    // Maximum number of records per spill file
    private static final int FILE_SIZE = 10000;

    /**
     * Make sure name is not null, has no blanks, etc.
     * 
     * @param name
     *            String to cleanup.
     * @return Cleaned-up name
     */
    private static String clean(final String name) {
        if (name == null) {
            return "Unknown";
        }

        final String n = name.trim();

        if (n.length() == 0) {
            return "Unknown";
        }

        return n.replace(' ', '_').replace('.', '_');
    }

    /**
     * Get the output directory, or create it if possible.
     * 
     * @param outputDir
     *            Output dir
     * @param trace
     *            Custom tracer for log messages.
     * @param name
     *            Name of file being created.
     * @return File File based on the session configuration output path.
     */
    private static File getPath(final String outputDir,
            final Tracer trace, final String name) {
        if (outputDir == null) {
            throw new IllegalArgumentException("Spill output directory cannot be null");

        }

        final String od = outputDir;

        final File path = new File(od);

        if (!path.exists()) {
            if (path.mkdirs()) {
                trace.trace(ME , " Output directory '" , od ,
                        "' did not exist, created for " , name);
            } else {
                trace.error(ME , " Output directory '" , od ,
                        "' does not exist, could not be created " , "for ", name);
                return null;
            }
        }

        return path;
    }

    private final SpillFileFactory _factory;
    private final SpillHandler _handler;
    private final SpillSerializer<T> _serializer;
    private final BlockingQueue<T> _targetQueue;
    private final int _quota;
    private final BlockingQueue<T> _queue = new LinkedBlockingQueue<T>();
    private final long _timeout;
    private final Tracer _trace;
    private final SseContextFlag     sseFlag;

    private final SpillThread _thread = new SpillThread();
    private final Class<T> _clss;
    private final boolean _enable;

    private final String _name;

    private boolean _shutDown = false;

    private int _records_written = 0;

    private int _records_read = 0;

    /**
     * Constructs SpillProcessor.
     * 
     * @param outputDir
     *            Root output directory for spill files
     * @param targetQueue
     *            Queue of messages to spill from. Used to unspill messages from
     *            the spill file as well as size checks. Extends the
     *            targetQueue's size.
     * @param quota
     *            Quota for target queue Max.
     * @param enable
     *            True to enable spill processing.
     * @param clss
     *            Class type used in spill serialization.
     * @param name
     *            Topic name for spill processing.
     * @param keep
     *            Keep spill files if true.
     * @param timeout
     *            Wait to poll from the queue for up to this time.
     * @param trace
     *            Custom tracer or null for JmsFastTracer.
     * @param sseFlag
     *            The SSE context flag
     * @throws SpillProcessorException
     *             Thrown if an error occurs in constructing the spill
     *             processor.
     */
    public SpillProcessor(final String outputDir,
            final BlockingQueue<T> targetQueue, final int quota,
            final boolean enable, final Class<T> clss, final String name,
            final boolean keep, final long timeout, final Tracer trace, final SseContextFlag sseFlag)
            throws SpillProcessorException {
        super();

        if (clss == null) {
            throw new SpillProcessorException("Null class");
        }

        this._name = clean(name);
        this._trace = (trace != null) ? trace : TraceManager.getTracer(Loggers.JMS);
        this._timeout = Math.max(timeout, 1L);
        this.sseFlag = sseFlag;

        File path = null;

        if (enable) {
            // Try to get a path

            path = getPath(outputDir, this._trace, this._name);

            if (path == null) {
                this._trace.warn(ME ,
                        " Could not get output directory from test " ,
                        "configuration, spill processing will be " ,
                        "disabled for " , this._name);
            }
        }

        this._clss = clss;
        this._enable = enable && (path != null);

        try {
            this._factory = this._enable ? new SpillFileFactory(
                    createSubdirectory(path), keep, this._trace) : null;
        } catch (final SpillFileFactoryException sffe) {
            throw new SpillProcessorException(sffe);
        }

        try {
            this._handler = this._enable ? new SpillHandler(this._factory,
                    FILE_SIZE, this._trace) : null;
        } catch (final SpillHandlerException she) {
            throw new SpillProcessorException(she);
        }

        try {
            this._serializer = new SpillSerializer<T>(this._clss, this._trace);
        } catch (final SpillSerializerException sse) {
            throw new SpillProcessorException(sse);
        }

        if (targetQueue == null) {
            throw new SpillProcessorException("Null target queue");
        }

        this._targetQueue = targetQueue;
        this._quota = Math.max(quota, 1);

        if (this._enable) {
            this._trace.trace(ME, " Spill processing enabled for topic '",
                    this._name, "' with quota of ", this._quota);
        } else {
            this._trace.trace(ME, " Spill processing disabled for topic '",
                    this._name, "'");
        }
    }

    /**
     * Create a subdirectory of the parent directory to hold our spill files.
     * 
     * @param parent
     *            Parent directory to create the subdirectory in.
     * @return Subdirectory of parent.
     * @throws SpillProcessorException
     *             Thrown if unable to create the subdirectory.
     */
    private File createSubdirectory(final File parent)
            throws SpillProcessorException {
        if (parent == null) {
            throw new SpillProcessorException("Null parent directory");
        }

        if (!parent.isDirectory()) {
            throw new SpillProcessorException("Parent is not a directory");
        }

        final StringBuilder sb = new StringBuilder(PREFIX);

        sb.append(this._name);
        sb.append(sseFlag.isApplicationSse() ? "_SSE_" : "_FSW_");
        sb.append(GdsSystemProperties.getPid()).append("_");
        sb.append(System.currentTimeMillis()).append("_");

        final int prefixLength = sb.length();
        File result = null;

        for (int i = 0; i < TRY_UNIQUE; ++i) {
            sb.append(i);

            result = new File(parent, sb.toString());

            if (result.mkdir()) {
                break;
            }

            sb.setLength(prefixLength);

            result = null;

            randomSleep(DELAY_UNIQUE);
        }

        if (result == null) {
            throw new SpillProcessorException("Unable to create subdirectory "
                    + "under " + parent.getAbsolutePath());
        }

        return result;
    }

    /**
     * Return number of records sent.
     * @return number of records sent
     */
    private synchronized int getRecordCount() {
        return this._records_written;
    }

    /**
     * Return number of records read.
     * @return number of records read
     */
    private synchronized int getRecordReadCount() {
        return this._records_read;
    }


    @Override
    public synchronized boolean isEmpty() {
        return this._queue.isEmpty() && !spilling()
                && this._targetQueue.isEmpty();
    }


    @Override
    public T poll(final long timeout, final TimeUnit tu) {
        final T next = SleepUtilities.checkedPoll(_targetQueue, timeout, tu)
                .getValue();

        if (next != null) {
            synchronized (this) {
                ++this._records_read;
            }
        } else if (this._enable && spilling()) {
            // The target queue has nothing but our queue does.
            // That shouldn't happen very often. It usually happens when the
            // queue size is set artificially small for testing.

            this._trace.warn(ME, ".poll Unexpected empty poll on ",
                    this._name);
        }

        return next;
    }
    
    @Override
    public T poll() throws InterruptedException {
    	final T next = _targetQueue.take();

    	if (next != null) {
    		++this._records_read;
    	} else if (this._enable && spilling()) {
    		// The target queue has nothing but our queue does.
    		// That shouldn't happen very often. It usually happens when the
    		// queue size is set artificially small for testing.

    		this._trace.warn(ME, ".poll Unexpected empty poll on ",
    				this._name);
    	}

    	return next;
    }

    @Override
    public synchronized boolean put(final T object) {
        boolean interrupted = false;

        if (object == null) {
            return interrupted;
        }

        if (shuttingDown()) {
            this._trace.warn(ME, ".put Message ignored, shutting down ",
                    this._name);
            return interrupted;
        }

        // If not enabled, place directly on target queue, perhaps blocking

        final BlockingQueue<T> which = (_enable ? _queue : _targetQueue);

        while (true) {
            try {
                final OfferStatus os = SleepUtilities.fullOffer(which, object,
                        PUT_RETRY, TimeUnit.MILLISECONDS);

                interrupted = os.getInterrupted();

                if (os.getTaken()) {
                    break;
                }

                this._trace.trace(ME, ".put Timeout, retrying ", this._name);
            } catch (final ExcessiveInterruptException eie) {
                interrupted = true;

                this._trace.warn(ME, ".put Error offering ", this._name,
                        ", ", rollUpMessages(eie) , ExceptionTools.getMessage(eie), eie);
            }
        }

        ++this._records_written;

        return interrupted;
    }

    /**
     * Tries to sleep for a randomized interval, less than or equal to the
     * specified time, but at least one millisecond.
     * 
     * @param sleep
     *            Max time to sleep for.
     * @return True if interrupted
     */
    private boolean randomSleep(final long sleep) {
        return SleepUtilities.checkedSleep(Math.max(
                (long) (Math.random() * sleep), 1L));
    }

  @Override
    public int remainingCapacity() {
        if (this._enable) {
            return Integer.MAX_VALUE;
        } else {
            return this._targetQueue.remainingCapacity();
        }
    }

    /**
     * Spill, handling all exceptions.
     * 
     * @param object
     *            Object to spill.
     */
    private synchronized void safeSpill(final T object) {
        if (!this._enable) {
            return;
        }

        byte[] encoded = null;

        try {
            encoded = this._serializer.encode(object);
        } catch (final SpillSerializerException sse) {
            this._trace.error(ME, ".spill Unable to spill ", this._name,
                    ": ", rollUpMessages(sse) ,ExceptionTools.getMessage(sse), sse);
            return;
        }

        try {
            this._handler.spill(encoded);
        } catch (final SpillHandlerException she) {
            this._trace.error(ME, ".spill Unable to spill ", this._name,
                    ": ", rollUpMessages(she), ExceptionTools.getMessage(she), she);
        }
    }

    @Override
    public synchronized void shutDown() {
        this._shutDown = true;

        if (this._enable) {
            this._thread.threadShutDown();

            this._trace.trace(ME, ".shutDown Shutting down '", this._name,
                    "'");
        }
    }


    @Override
    public boolean shutDownAndClose() {
        boolean interrupted = false;
        int spillCount = 0;

        shutDown();

        if (this._enable) {
            while (true) {
                this._trace.trace(ME, ".shutDownAndClose Joining ",
                        this._name);
                try {
                    interrupted |= SleepUtilities.fullJoin(_thread,
                            JOIN_GIVE_UP);
                    if (!this._thread.isAlive()) {
                        break;
                    }

                    this._trace.warn(ME,
                            ".shutDownAndClose Join timeout for ", this._name,
                            " with ", spillSize(), " remaining");
                } catch (final ExcessiveInterruptException eie) {
                    this._trace.warn(ME , ".shutDownAndClose Error joining ", this._name,
                            ", ", rollUpMessages(eie), ExceptionTools.getMessage(eie), eie);
                    interrupted = true;
                }
            }

            synchronized (this) {
                this._factory.deleteClosed();

                spillCount = this._handler.getSpilled();
            }

            this._trace.trace(ME, ".shutDownAndClose Done with ", this._name);
        }

        this._trace.trace(ME, ".shutDownAndClose ", this._name, " received ",
                        getRecordCount(), " and read ", getRecordReadCount(),
                        " of which ", spillCount, " were spilled" );

        return interrupted;
    }

    /**
     * Return shutdown status.
     * @return if shutting down
     */
    private synchronized boolean shuttingDown() {
        return this._shutDown;
    }


    @Override
    public int size() {
    	if (!_enable) {
    		return this._targetQueue.size();
    	}
        return spillSize() + this._targetQueue.size();
    }

    /**
     * Return true if we are spilling
     * @return if spilling
     */
    private synchronized boolean spilling() {
        return (this._enable && this._handler.spilling());
    }

    /**
     * Return summary length of OUR queue and spilled records.
     * 
     * @return Length
     */
    private synchronized int spillSize() {
        int result = this._queue.size();

        if (this._enable) {
            result += this._handler.size();
        }

        return result;
    }

    @Override
    public void start() {
        if (this._enable) {
            this._thread.start();

            this._trace.trace(ME, ".start Starting thread for ", this._name);
        }
    }

    /**
     * Unspill if it is possible to do so. Handle all exceptions.
     */
    private synchronized void unspillIfPossible() {
        if (!this._enable) {
            return;
        }

        while ((this._targetQueue.remainingCapacity() > 0) && spilling()) {
            byte[] bytes = null;

            try {
                bytes = this._handler.unspill();
            } catch (final SpillHandlerException she) {
                this._trace.error(ME,".unspillIfPossible Unable to unspill ", this._name,
                        ": ", rollUpMessages(she), ExceptionTools.getMessage(she), she);
                break;
            }

            if (bytes == null) {
                // Nothing to unspill (somewhat odd, though)
                break;
            }

            T object = null;

            try {
                object = this._serializer.decode(bytes);
            } catch (final SpillSerializerException sse) {
                this._trace.error(ME,  ".unspillIfPossible Unable to decode ", this._name,
                        ": ", rollUpMessages(sse), ExceptionTools.getMessage(sse), sse);
                break;
            }

            if (!this._targetQueue.offer(object)) {
                this._trace.error(ME, ".unspillIfPossible Target queue ",
                        this._name, " won't take message");
                break;
            }
        }
    }
}
