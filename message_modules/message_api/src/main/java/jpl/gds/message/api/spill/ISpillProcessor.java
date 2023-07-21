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

package jpl.gds.message.api.spill;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * An interface for a Spill processor
 *
 * @param <T> any type that extends Serializable
 */
public interface ISpillProcessor<T extends Serializable> {

    /**
     * Return true if OUR queue is empty, we are not spilling, and the target
     * queue is empty.
     *
     * @return Remaining capacity
     */
    boolean isEmpty();

    /**
     * Poll the target queue. Note, not OUR queue, without timeout. Blocks
     * until an item can be fetched from the queue, or the thread is interrupted.
     *
     * @return Next element
     * @throws InterruptedException if the poll is interrupted
     */
    T poll() throws InterruptedException;

    /**
     * Poll the target queue. Note, not OUR queue.
     *
     * @param timeout
     *            Maximum time to wait to poll from the queue.
     * @param tu
     *            TimeUnit used to determine how to interpret the maximum
     *            timeout unit.
     * @return Next element or null if none.
     */
    T poll(final long timeout, final TimeUnit tu);

    /**
     * Place message on queue for processing. There is no delay, except when
     * bypassing.
     *
     * @param object
     *            Object to place on the queue.
     * @return True if the operation was interrupted.
     */
    boolean put(final T object);

    /**
     * Return remaining capacity of OUR queue, which is always large.
     *
     * @return Remaining capacity
     */
    int remainingCapacity();

    /**
     * Request shutdown when empty.
     */
    void shutDown();

    /**
     * Shut down the thread, wait for quiescence, release resources and join the
     * thread. Thread won't exit until queues are empty and we are no longer
     * spilling. Note NOT synchronized. Join does not give up the lock.
     *
     * @return True if interrupted
     */
    boolean shutDownAndClose();

    /**
     * Return summary length of OUR queue, target queue, and spilled records.
     *
     * @return Length
     *
     * Removed synchronized keyword.
     *          Synchronizing this call is too risky, as it will block
     *          if an offer is currently blocked. The only time we care about
     *          total accuracy of this return value is during shutdown.
     */
    int size();

    /**
     * Start processing by starting internal thread.
     */
    void start();
}