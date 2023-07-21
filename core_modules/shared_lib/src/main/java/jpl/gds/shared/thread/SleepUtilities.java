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
package jpl.gds.shared.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.Pair;


/**
 * Class SleepUtilities. These take care of InterruptedException in a nice
 * manner. They either check for the interrupt and return the status, or reissue
 * the action to allow the full sleep interval to elapse.
 *
 * Note that some underlying methods interpret an interval of zero as infinite,
 * and others as zero. These methods use the same interpretation as the
 * underlying.
 *
 * Time units of microseconds or nanoseconds are rounded to milliseconds,
 * because I can't see any other way of doing the full sleep versions. There's
 * no System.currentTimeMicros, for example.
 *
 * If the time interval is non-zero but rounds to zero (e.g., an interval of one
 * microsecond) it is set to one millisecond. This was initially done to avoid
 * turning a non-infinite sleep into an infinite one inadvertently. It may not
 * be necessary, but seems harmless for MPCS.
 *
 */
public class SleepUtilities extends Object
{
    /**
     * Constructor. Not used.
     */
    private SleepUtilities()
    {
        super();
    }

    /**
     * Tries to sleep for a randomized interval, less than or equal to the
     * specified time, but at least one millisecond.
     *
     * @param sleep
     *            Sleep interval
     *
     * @return True if interrupted
     */
    public static boolean randomSleep(final long sleep) {
        return SleepUtilities.checkedSleep(Math.max((long) (Math.random() * sleep), 1L));
    }

    /**
     * Sleeps the desired interval, even in the presence of interrupts. Returns
     * true if interrupted at least once. Does NOT reset interrupt status.
     *
     * Zero means zero, but we don't bother to make the call in that case.
     *
     * @param millis Sleep interval
     *
     * @return True if interrupt(s) encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     */
    public static boolean fullSleep(final long millis)
        throws ExcessiveInterruptException
    {
        if (millis == 0L)
        {
            return false;
        }

        final long target      = System.currentTimeMillis() + millis;
        long       interval    = millis;
        boolean    interrupted = false;

        for (int i = 0; i < ExcessiveInterruptException.MAX_INTERRUPT; ++i)
        {
            try
            {
                Thread.sleep(interval);

                return interrupted;
            }
            catch (final InterruptedException ie)
            {
                interrupted = true;
            }

            final long now = System.currentTimeMillis();

            if (now >= target)
            {
                // We got an interrupt, but we still slept long enough

                return interrupted;
            }

            // Figure out how much we still need to sleep
            interval = target - now;
        }

        throw ExcessiveInterruptException.constructTypical(
                  "SleepUtilities.fullSleep");
    }


    /**
     * Sleeps the desired interval, even in the presence of interrupts. Returns
     * true if interrupted at least once. Does NOT reset interrupt status.
     * Traps and logs ExcessiveInterruptException.
     *
     * @param millis  Interval in milliseconds
     * @param tracer  Tracer
     * @param message Message
     *
     * @return True if interrupt(s) encountered
     */
    public static boolean fullSleep(final long   millis,
                                    final Tracer tracer,
                                    final String message)
    {
        boolean interrupted = false;

        try
        {
            interrupted = fullSleep(millis);
        }
        catch (final ExcessiveInterruptException eie)
        {
            interrupted = true;

            if (tracer != null)
            {
                tracer.error(((message != null) ? message : "Error sleeping") +
                             ": "                                             +
                        ExceptionTools.rollUpMessages(eie), eie.getCause());
            }
        }

        return interrupted;
    }


    /**
     * Tries to sleep the desired interval, aborts if interrupted. Returns
     * true if interrupted. Does NOT reset interrupt status.
     *
     * Zero means zero, but we don't bother to make the call in that case.
     *
     * @param millis Sleep interval
     *
     * @return True if interrupt encountered
     */
    public static boolean checkedSleep(final long millis)
    {
        if (millis == 0L)
        {
            return false;
        }

        boolean interrupted = false;

        try
        {
            Thread.sleep(millis);
        }
        catch (final InterruptedException ie)
        {
            interrupted = true;
        }

        return interrupted;
    }


    /**
     * Join, waiting the desired interval, even in the presence of interrupts.
     * Returns true if interrupted at least once. Does NOT reset interrupt
     * status.
     *
     * A wait of zero means forever.
     *
     * Caller must check whether the thread is alive after this method returns.
     * Even if the join times out, the thread may exit on its own.
     *
     * @param thread Thread to join
     * @param millis Sleep interval
     *
     * @return True if interrupt(s) encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     */
    public static boolean fullJoin(final Thread thread,
                                   final long   millis)
        throws ExcessiveInterruptException
    {
        long    target      = -1L;
        long    interval    = millis;
        boolean interrupted = false;
        boolean infinite    = false;

        if (millis > 0L)
        {
            target = System.currentTimeMillis() + millis;
        }
        else
        {
            // Negative millis will be trapped on the join call

            infinite = true;
        }

        for (int i = 0; i < ExcessiveInterruptException.MAX_INTERRUPT; ++i)
        {
            try
            {
                thread.join(interval);

                // At this point we either joined or timed out
                // (if infinite, must be a join)

                return interrupted;
            }
            catch (final InterruptedException ie)
            {
                interrupted = true;
            }

            if (! thread.isAlive())
            {
                // We got an interrupt, but we still finished the join

                return interrupted;
            }

            if (infinite)
            {
                // Continue infinite waiting
                continue;
            }

            final long now = System.currentTimeMillis();

            if (now >= target)
            {
                // We got an interrupt, but we still slept long enough

                return interrupted;
            }

            // Figure out how much we still need to sleep
            interval = target - now;
        }

        throw ExcessiveInterruptException.constructTypical(
                  "SleepUtilities.fullJoin");
    }


    /**
     * Join, waiting forever. See fullJoin/2 for description.
     *
     * @param thread Thread to join
     *
     * @return True if interrupt(s) encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     */
    public static boolean fullJoin(final Thread thread)
        throws ExcessiveInterruptException
    {
        return fullJoin(thread, 0L);
    }

    /**
     * Tries to join another thread, waiting for the indicated amount of
     * time, and logging status to the given FastTracer.
     *
     * @param thread   Thread to join with
     * @param wait     Time to wait
     * @param name     Who we are joining
     * @param logger   FastTracer to log debug and error status to; may be null
     *
     * @return True if we joined
     * 
     */
    /*
    public static boolean checkedJoin(final Thread thread,
    		final long         wait,
    		final String       name,
    		final FastTracer   logger)
    {
    	assert thread != null : "Thread argument cannot be null";
    	assert wait > 0L : "Wait time cannot be <= 0";

    	if (logger != null) {
    		logger.logDebug("Waiting to join thread ",  name);
    	}

    	boolean joined = false;

    	try
    	{
    		SleepUtilities.fullJoin(thread, wait);

    		joined = ! thread.isAlive();

    		if (joined)
    		{
    			if (logger != null) {
    				logger.logDebug("Joined thread ", name);
    			}
    		}
    		else
    		{
    			if (logger != null) {
    				logger.logError("Could not join thread ", name);
    			}
    		}
    	}
    	catch (final ExcessiveInterruptException eie)
    	{
    		if (logger != null) {
    			logger.logError("Could not join thread ",
    					name,
    					": Excessive Interrupt Exception");
    		}
    	}

    	return joined;
    }
    */
    
    public static boolean checkedJoin(final Thread thread,
    		final long         wait,
    		final String       name,
    		final Tracer   logger)
    {
    	assert thread != null : "Thread argument cannot be null";
    	assert wait > 0L : "Wait time cannot be <= 0";

    	if (logger != null) {
            logger.debug("Waiting to join thread ", name);
    	}

    	boolean joined = false;

    	try
    	{
    		SleepUtilities.fullJoin(thread, wait);

    		joined = ! thread.isAlive();

    		if (joined)
    		{
    			if (logger != null) {
                    logger.debug("Joined thread ", name);
    			}
    		}
    		else
    		{
    			if (logger != null) {
                    logger.debug("Could not join thread ", name);
    			}
    		}
    	}
    	catch (final ExcessiveInterruptException eie)
    	{
    		if (logger != null) {
    			logger.debug("Could not join thread " +
    					name +
                        ": Excessive Interrupt Exception " + eie.getMessage(), eie.getCause());
    		}
    	}

    	return joined;
    }

    /**
     * Join, waiting the desired interval, aborts if interrupted. Returns
     * true if interrupted. Does NOT reset interrupt status.
     *
     * A wait of zero means forever.
     *
     * Caller must check whether the thread is alive after this method returns.
     * Even if the join times out, the thread may exit on its own.
     *
     * @param thread Thread to join
     * @param millis Sleep interval
     *
     * @return True if interrupt encountered
     */
    public static boolean checkedJoin(final Thread thread,
                                      final long   millis)
    {
        boolean interrupted = false;

        try
        {
            thread.join(millis);
        }
        catch (final InterruptedException ie)
        {
            interrupted = true;
        }

        return interrupted;
    }


    /**
     * Join, waiting forever. See checkedJoin/2 for description.
     *
     * @param thread Thread to join
     *
     * @return True if interrupt encountered
     */
    public static boolean checkedJoin(final Thread thread)
    {
        return checkedJoin(thread, 0L);
    }


    /**
     * Offer, waiting the desired interval, even in the presence of interrupts.
     * Returns true if interrupted at least once. Does NOT reset interrupt
     * status.
     *
     * A wait of zero means zero, unless infinite flag is set.
     *
     * @param queue      Blocking queue
     * @param offering   Data offered
     * @param timeout    Wait interval
     * @param tu         Time unit
     * @param asInfinite True if zero means infinite
     *
     * @return Pair of: True if taken, True if interrupt encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     */
    private static <T> OfferStatus fullOffer(final BlockingQueue<T> queue,
                                             final T                offering,
                                             final long             timeout,
                                             final TimeUnit         tu,
                                             final boolean          asInfinite)
        throws ExcessiveInterruptException
    {
        final long    millis      = toMillis(timeout, tu);
        final long    target      = System.currentTimeMillis() + millis;
        final boolean infinite    = (millis == 0L) && asInfinite;
        long          interval    = millis;
        boolean       interrupted = false;

        for (int i = 0; i < ExcessiveInterruptException.MAX_INTERRUPT; ++i)
        {
            try
            {
                final boolean taken =
                    queue.offer(offering, interval, TimeUnit.MILLISECONDS);

                return new OfferStatus(taken, interrupted);
            }
            catch (final InterruptedException ie)
            {
                interrupted = true;
            }

            if (infinite)
            {
                // Continue infinite waiting
                continue;
            }

            final long now = System.currentTimeMillis();

            if (now >= target)
            {
                // We got an interrupt, but we still slept long enough

                return new OfferStatus(false, interrupted);
            }

            // Figure out how much we still need to sleep
            interval = target - now;
        }

        throw ExcessiveInterruptException.constructTypical(
                  "SleepUtilities.fullOffer");
    }


    /**
     * Offer, waiting the desired interval. See fullOffer/5 for description.
     *
     * A wait of zero means zero, just like underlying offer.
     * @param <T> Queue element type
     *
     * @param queue    Blocking queue
     * @param offering Data offered
     * @param timeout  Wait interval
     * @param tu       Time unit
     *
     * @return Pair of: True if taken, True if interrupt encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     */
    public static <T> OfferStatus fullOffer(final BlockingQueue<T> queue,
                                            final T                offering,
                                            final long             timeout,
                                            final TimeUnit         tu)
        throws ExcessiveInterruptException
    {
        return fullOffer(queue, offering, timeout, tu, false);
    }


    /**
     * Offer, not waiting at all. See fullOffer/5 for description.
     *
     * @param <T> Queue element type
     * @param queue    Blocking queue
     * @param offering Data offered
     *
     * @return Pair of: True if taken, True if interrupt encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     *
     */
    public static <T> OfferStatus fullOffer(final BlockingQueue<T> queue,
                                            final T                offering)
        throws ExcessiveInterruptException
    {
        return fullOffer(queue, offering, 0L, TimeUnit.MILLISECONDS, false);
    }


    /**
     * Offer, waiting the desired interval, aborts if interrupted. Returns
     * true in second of pair if interrupted. Does NOT reset interrupt status.
     *
     * A wait of zero means zero.
     *
     * @param queue    Blocking queue
     * @param offering Data offered
     * @param timeout  Wait interval
     * @param tu       Time unit
     *
     * @return Pair of: True if taken, True if interrupt encountered
     *
     * @param <T> Queue element type
     */
    public static <T> OfferStatus checkedOffer(final BlockingQueue<T> queue,
                                               final T                offering,
                                               final long             timeout,
                                               final TimeUnit         tu)
    {
        boolean taken       = false;
        boolean interrupted = false;

        try
        {
            taken = queue.offer(offering, timeout, tu);
        }
        catch (final InterruptedException ie)
        {
            interrupted = true;
        }

        return new OfferStatus(taken, interrupted);
    }


    /**
     * Offer, not waiting at all. See checkedOffer/4 for description.
     *
     * @param queue    Blocking queue
     * @param offering Data offered
     *
     * @return Pair of: True if taken, True if interrupt encountered
     *
     * @param <T> Queue element type
     */
    public static <T> OfferStatus checkedOffer(final BlockingQueue<T> queue,
                                               final T                offering)
    {
        return checkedOffer(queue, offering, 0L, TimeUnit.MILLISECONDS);
    }


    /**
     * Put, waiting forever. See fullOffer/5 for description, except for return
     * value.
     *
     * @param queue    Blocking queue
     * @param offering Data offered
     * @param <T> Queue element type
     *
     * @return True if interrupt encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     *
     */
    public static <T> boolean fullPut(final BlockingQueue<T> queue,
                                      final T                offering)
        throws ExcessiveInterruptException
    {
        return fullOffer(queue,
                         offering,
                         0L,
                         TimeUnit.MILLISECONDS,
                         true).getInterrupted();
    }


    /**
     * Put, waiting forever.
     *
     * @param queue    Blocking queue
     * @param offering Data offered
     *
     * @return True if interrupt encountered
     *
     * @param <T> Queue element type
     */
    public static <T> boolean checkedPut(final BlockingQueue<T> queue,
                                         final T                offering)
    {
        boolean interrupted = false;

        try
        {
            queue.put(offering);
        }
        catch (final InterruptedException ie)
        {
            interrupted = true;
        }

        return interrupted;
    }


    /**
     * Poll, waiting the desired interval, even in the presence of interrupts.
     * Returns true if interrupted at least once. Does NOT reset interrupt
     * status.
     *
     * A wait of zero means zero, unless asInfinite flag is set.
     *
     * @param queue      Blocking queue
     * @param timeout    Wait interval
     * @param tu         Time unit
     * @param asInfinite Zero means infinite
     *
     * @return Pair of: Value or null, True if interrupt encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     */
    private static <T> PollStatus<T> fullPoll(final BlockingQueue<T> queue,
                                              final long             timeout,
                                              final TimeUnit         tu,
                                              final boolean          asInfinite)
        throws ExcessiveInterruptException
    {
        final long    millis      = toMillis(timeout, tu);
        final long    target      = System.currentTimeMillis() + millis;
        final boolean infinite    = (millis == 0L) && asInfinite;
        long          interval    = millis;
        boolean       interrupted = false;

        for (int i = 0; i < ExcessiveInterruptException.MAX_INTERRUPT; ++i)
        {
            try
            {
                final T taken = queue.poll(interval, TimeUnit.MILLISECONDS);

                return new PollStatus<T>(taken, interrupted);
            }
            catch (final InterruptedException ie)
            {
                interrupted = true;
            }

            if (infinite)
            {
                // Continue infinite waiting
                continue;
            }

            final long now = System.currentTimeMillis();

            if (now >= target)
            {
                // We got an interrupt, but we still slept long enough

                return new PollStatus<T>(null, interrupted);
            }

            // Figure out how much we still need to sleep
            interval = target - now;
        }

        throw ExcessiveInterruptException.constructTypical(
                  "SleepUtilities.fullPoll");
    }


    /**
     * Poll, waiting the desired interval. See fullPoll/4 for description.
     *
     * A wait of zero means zero, just like the underlying poll.
     *
     * @param <T> Queue element type
     * @param queue   Blocking queue
     * @param timeout Wait interval
     * @param tu      Time unit
     *
     * @return Pair of: Value or null, True if interrupt encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     *
     */
    public static <T> PollStatus<T> fullPoll(final BlockingQueue<T> queue,
                                             final long             timeout,
                                             final TimeUnit         tu)
        throws ExcessiveInterruptException
    {
        return fullPoll(queue, timeout, tu, false);
    }


    /**
     * Poll, not waiting. See fullPoll/4 for description.
     *
     * @param queue Blocking queue
     *
     * @param <T> Queue element type
     * @return Pair of: Value or null, True if interrupt encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     *
     */
    public static <T> PollStatus<T> fullPoll(final BlockingQueue<T> queue)
        throws ExcessiveInterruptException
    {
        return fullPoll(queue, 0L, TimeUnit.MILLISECONDS, false);
    }


    /**
     * Poll, waiting the desired interval, aborts if interrupted. Returns
     * true in second of pair if interrupted. Does NOT reset interrupt status.
     *
     * A wait of zero means zero.
     *
     * @param queue   Blocking queue
     * @param timeout Wait interval
     * @param tu      Time unit
     *
     * @return Pair of: Value or null, True if interrupt encountered
     *
     * @param <T> Queue element type
     */
    public static <T> PollStatus<T> checkedPoll(final BlockingQueue<T> queue,
                                                final long             timeout,
                                                final TimeUnit         tu)
    {
        T       taken       = null;
        boolean interrupted = false;

        try
        {
            taken = queue.poll(timeout, tu);
        }
        catch (final InterruptedException ie)
        {
            interrupted = true;
        }

        return new PollStatus<T>(taken, interrupted);
    }


    /**
     * Poll, not waiting at all. See checkedPoll/3 for description.
     *
     * @param queue Blocking queue
     *
     * @return Pair of: Value or null, True if interrupt encountered
     *
     * @param <T> Queue element type
     */
    public static <T> PollStatus<T> checkedPoll(final BlockingQueue<T> queue)
    {
        return checkedPoll(queue, 0L, TimeUnit.MILLISECONDS);
    }


    /**
     * Take, waiting forever. See fullPoll/4 for description.
     *
     * @param <T> Queue element type
     * @param queue Blocking queue
     *
     * @return Pair of: Value or null, True if interrupt encountered
     *
     * @throws ExcessiveInterruptException Too many interrupts
     *
     */
    public static <T> PollStatus<T> fullTake(final BlockingQueue<T> queue)
        throws ExcessiveInterruptException
    {
        return fullPoll(queue, 0L, TimeUnit.MILLISECONDS, true);
    }


    /**
     * Take, waiting forever. Abort if interrupted.
     *
     * @param queue Blocking queue
     *
     * @return Pair of: Value or null, True if interrupt encountered
     *
     * @param <T> Queue element type
     */
    public static <T> PollStatus<T> checkedTake(final BlockingQueue<T> queue)
    {
        T       taken       = null;
        boolean interrupted = false;

        try
        {
            taken = queue.take();
        }
        catch (final InterruptedException ie)
        {
            interrupted = true;
        }

        return new PollStatus<T>(taken, interrupted);
    }


    /**
     * Wait, handling interrupts.
     *
     * @param waiter Object to wait on
     * @param millis Wait interval
     *
     * @return True if interrupted
     */
    public static boolean checkedWait(final Object waiter,
                                      final long   millis)
    {
        boolean interrupted = false;

        try
        {
            waiter.wait(millis);
        }
        catch (final InterruptedException ie)
        {
            interrupted = true;
        }

        return interrupted;
    }


    /**
     * Wait, handling interrupts. Wait forever.
     *
     * @param waiter Object to wait on
     *
     * @return True if interrupted
     */
    public static boolean checkedWait(final Object waiter)
    {
        return checkedWait(waiter, 0L);
    }


    /**
     * Wait for process, handling interrupts.
     *
     * @param process Process
     *
     * @return Return status and interrupted flag as pair
     *
     * @throws ExcessiveInterruptException Too many interrupts
     */
    public static WaitForStatus fullWaitFor(final Process process)
        throws ExcessiveInterruptException
    {
        boolean interrupted = false;

        for (int i = 0; i < ExcessiveInterruptException.MAX_INTERRUPT; ++i)
        {
            try
            {
                return new WaitForStatus(process.waitFor(), interrupted);
            }
            catch (final InterruptedException ie)
            {
                interrupted = true;
            }
        }

        throw ExcessiveInterruptException.constructTypical(
                  "SleepUtilities.fullWaitFor");
    }


    /**
     * Wait for process, aborting if interrupted.
     *
     * @param process Process
     *
     * @return Return status and interrupted flag as pair
     */
    public static WaitForStatus checkedWaitFor(final Process process)
    {
        int     status      = 1;
        boolean interrupted = false;

        try
        {
            status = process.waitFor();
        }
        catch (final InterruptedException ie)
        {
            interrupted = true;
        }

        return new WaitForStatus(status, interrupted);
    }


    /**
     * Acquire, handling interrupts.
     *
     * @param semaphore Semaphore
     * @param permits   Number of permits
     *
     * @return True if interrupted
     *
     * @throws ExcessiveInterruptException Too many interrupts
     */
    public static boolean fullAcquire(final Semaphore semaphore,
                                      final int       permits)
        throws ExcessiveInterruptException
    {
        boolean interrupted = false;

        for (int i = 0; i < ExcessiveInterruptException.MAX_INTERRUPT; ++i)
        {
            try
            {
                semaphore.acquire(permits);

                return interrupted;
            }
            catch (final InterruptedException ie)
            {
                interrupted = true;
            }
        }

        throw ExcessiveInterruptException.constructTypical(
                  "SleepUtilities.fullAcquire");
    }


    /**
     * Acquire, handling interrupts.
     *
     * @param semaphore Semaphore
     *
     * @return True if interrupted
     *
     * @throws ExcessiveInterruptException Too many interrupts
     */
    public static boolean fullAcquire(final Semaphore semaphore)
        throws ExcessiveInterruptException
    {
        return fullAcquire(semaphore, 1);
    }


    /**
     * Acquire, handling interrupts.
     *
     * @param semaphore Semaphore
     * @param tracer    Tracer
     * @param message   Message
     *
     * @return True if interrupted
     */
    public static boolean fullAcquire(final Semaphore semaphore,
                                      final Tracer    tracer,
                                      final String    message)
    {
        boolean interrupted = false;

        try
        {
            interrupted = fullAcquire(semaphore, 1);
        }
        catch (final ExcessiveInterruptException eie)
        {
            interrupted = true;

            if (tracer != null)
            {
                tracer.error(((message != null) ? message : "Error acquiring") +
                             ": "                                              +
                             ExceptionTools.rollUpMessages(eie));
            }
        }

        return interrupted;
    }


    /**
     * Acquire, aborting if interrupted.
     *
     * @param semaphore Semaphore
     * @param permits   Number of permits
     *
     * @return True if interrupted
     */
    public static boolean checkedAcquire(final Semaphore semaphore,
                                         final int       permits)
    {
        boolean interrupted = false;

        try
        {
            semaphore.acquire(permits);
        }
        catch (final InterruptedException ie)
        {
            interrupted = true;
        }

        return interrupted;
    }


    /**
     * Acquire, aborting if interrupted.
     *
     * @param semaphore Semaphore
     *
     * @return True if interrupted
     */
    public static boolean checkedAcquire(final Semaphore semaphore)
    {
        return checkedAcquire(semaphore, 1);
    }


    /**
     * Convert time interval to milliseconds.
     *
     * @param interval
     * @param tu
     *
     * @return Interval in milliseconds
     */
    private static long toMillis(final long     interval,
                                 final TimeUnit tu)
    {
        if (interval < 0L)
        {
            throw new IllegalArgumentException("SleepUtilities.toMillis " +
                                               "Negative interval");
        }

        long millis = 0L;

        switch (tu)
        {
            // case DAYS:   Wait for 1.6
            // case HOURS:
            // case MINUTES:
            case SECONDS:
                millis = tu.toMillis(interval);
                break;

            case MILLISECONDS:
                millis = interval;
                break;

            case MICROSECONDS:
                millis = (interval + 500L) / 1000L;
                break;

            case NANOSECONDS:
                millis = (interval + 500000L) / 1000000L;
                break;

            default:
                throw new IllegalArgumentException("SleepUtilities.toMillis " +
                                                   "Unknown TimeUnit: "       +
                                                   tu);
        }

        if ((interval > 0L) && (millis == 0L))
        {
            // Don't underflow
            millis = 1L;
        }

        return millis;
    }


    /**
     * Class to implement wait for notify with time-outs and checking for
     * interrupts.
     *
     * How to use:
     *
     * final FullWait fw = new FullWait(waitObject, 1000L);
     *
     * synchronized(waitObject)
     * {
     *     while (<condition does not hold>)
     *     {
     *         final WaitStatus status = fw.fullWait();
     *
     *         if (status.getTimedOut())
     *         {
     *             <Take action for time-out>
     *         }
     *
     *         if (status.getInterrupted())
     *         {
     *             <action to take if interrupted, if any>
     *         }
     *     }
     * }
     *
     * Note that you can have both time-out and interrupted.
     *
     * Note also that you can skip the check for interrupted if you don't care
     * about interrupts. But if the wait interval is not infinite, you must
     * check for the time-out, otherwise you have an infinite loop. The loop is
     * broken by special checks.
     */
    public static class FullWait extends Object
    {
        private static final int MAX_TIMEDOUT = 1000;

        private final Object  _waiter;
        private final long    _millis;
        private final long    _target;
        private final boolean _infinite;

        private int _interrupts = 0;
        private int _timedOut   = 0;


        /**
         * Construct object to implement waiting for notify.
         *
         * @param waiter Object to wait on
         * @param millis Wait interval in milliseconds (zero means infinite)
         */
        public FullWait(final Object waiter,
                        final long   millis)
        {
            super();

            if (waiter == null)
            {
                throw new IllegalArgumentException(
                              "SleepUtilities.FullWait Null waiter");
            }

            if (millis < 0L)
            {
                // Just like wait would have done
                throw new IllegalArgumentException(
                              "SleepUtilities.FullWait Negative millis");
            }

            _waiter   = waiter;
            _millis   = millis;
            _infinite = (_millis == 0L);
            _target   = ! _infinite ? (System.currentTimeMillis() + _millis)
                                    : -1L;
        }


        /**
         * Construct object to implement waiting for notify with infinite wait.
         *
         * @param waiter Object to wait on
         */
        public FullWait(final Object waiter)
        {
            this(waiter, 0L);
        }


        /**
         * Wait, waiting the desired interval, even in the presence of
         * interrupts. Returns true if interrupted at least once.
         * Does NOT reset interrupt status.
         *
         * A wait of zero means forever.
         *
         * Caller must check whether the condition is true after this method
         * returns.
         *
         * If not infinite, caller must check for time-out as well. Failure to
         * do so may cause an infinite loop which is eventually broken with a
         * RunTimeException. Note that the timed-out call count must not be too
         * small, certainly a count of one is perfectly legitimate.
         *
         * @return Timed-out and interrupted, as a pair
         *
         * @throws ExcessiveInterruptException Too many interrupts
         */
        public WaitStatus fullWait() throws ExcessiveInterruptException
        {
            long interval = 0L;

            if (! _infinite)
            {
                final long now = System.currentTimeMillis();

                if (now >= _target)
                {
                    // We've timed out

                    ++_timedOut;

                    if (_timedOut > MAX_TIMEDOUT)
                    {
                        // Caller is not checking for timeout

                        throw new IllegalStateException(
                                      "SleepUtilities.fullWait");
                    }

                    return new WaitStatus(true, getInterrupted());
                }

                interval = _target - now;
            }

            try
            {
                _waiter.wait(interval);

                // At this point we were either notified or timed out or it's
                // spurious (if infinite, then not a timeout.)
                //
                // Since we can't know which it is, we cannot declare a timeout,
                // so we let the caller check the condition. If that is not yet
                // true, he'll make another call, and then we will declare the
                // timeout.
            }
            catch (final InterruptedException ie)
            {
                setInterrupted(); // Throws if too many
            }

            return new WaitStatus(false, getInterrupted());
        }


        /**
         * Increments interrupt count and throws if too many.
         *
         * @throws ExcessiveInterruptException Too many interrupts
         */
        private void setInterrupted() throws ExcessiveInterruptException
        {
            ++_interrupts;

            if (_interrupts > ExcessiveInterruptException.MAX_INTERRUPT)
            {
                throw ExcessiveInterruptException.constructTypical(
                    "SleepUtilities.FullWait.fullWait");
            }
        }


        /**
         * Get interrupted status.
         *
         * @return True if interrupted at least once
         */
        private boolean getInterrupted()
        {
            return (_interrupts > 0);
        }
    }


    /**
     * Utility class to make return values more friendly.
     */
	public static class OfferStatus extends Pair<Boolean, Boolean>
    {
        private static final long serialVersionUID = 0L;


        /**
         * Constructor.
         *
         * @param taken       True if was taken
         * @param interrupted True if was interruupted
         */
        public OfferStatus(final boolean taken,
                           final boolean interrupted)
        {
            super(taken, interrupted);
        }


        /**
         * Get taken status.
         *
         * @return boolean
         */
        public boolean getTaken()
        {
            return getOne();
        }


        /**
         * Get interrupted status.
         *
         * @return boolean
         */
        public boolean getInterrupted()
        {
            return getTwo();
        }
    }


    /**
     * Utility class to make return values more friendly.
     */
	public static class WaitStatus extends Pair<Boolean, Boolean>
    {
        private static final long serialVersionUID = 0L;


        /**
         * Constructor.
         *
         * @param timedOut    True if timed out
         * @param interrupted True if was interrupted
         */
        public WaitStatus(final boolean timedOut,
                          final boolean interrupted)
        {
            super(timedOut, interrupted);
        }


        /**
         * Get timed-out status.
         *
         * @return boolean
         */
        public boolean getTimedOut()
        {
            return getOne();
        }


        /**
         * Get interrupted status.
         *
         * @return boolean
         */
        public boolean getInterrupted()
        {
            return getTwo();
        }
    }


    /**
     * Utility class to make return values more friendly.
     */
	public static class WaitForStatus extends Pair<Integer, Boolean>
    {
        private static final long serialVersionUID = 0L;


        /**
         * Constructor.
         *
         * @param status      Status value
         * @param interrupted True if was interrupted
         */
        public WaitForStatus(final int     status,
                             final boolean interrupted)
        {
            super(status, interrupted);
        }


        /**
         * Get status value.
         *
         * @return int
         */
        public int getStatus()
        {
            return getOne();
        }


        /**
         * Get interrupted state.
         *
         * @return boolean
         */
        public boolean getInterrupted()
        {
            return getTwo();
        }
    }


    /**
     * Utility class to make return values more friendly.
     * @param <T> Queue element type
     */
	public static class PollStatus<T> extends Pair<T, Boolean>
    {
        private static final long serialVersionUID = 0L;


        /**
         * Constructor.
         *
         * @param value       Status value
         * @param interrupted True if was interrupted
         */
        public PollStatus(final T       value,
                          final boolean interrupted)
        {
            super(value, interrupted);
        }


        /**
         * Get status value.
         *
         * @return T
         */
        public T getValue()
        {
            return getOne();
        }


        /**
         * Get interrupted status.
         *
         * @return boolean
         */
        public boolean getInterrupted()
        {
            return getTwo();
        }
    }
}
