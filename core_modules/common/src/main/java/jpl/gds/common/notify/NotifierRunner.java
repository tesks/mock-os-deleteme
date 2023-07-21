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
package jpl.gds.common.notify;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.shared.log.Tracer;


/**
 * Class to do notification processing in a separate thread, including shutdown
 * and drain.
 *
 * This class is used by the EVR and alarm notifiers.
 *
 * The idea is that a queue of "notify pairs" is provided. The pairs are some
 * kind of an object to do the notification, and some kind of an object that
 * represents the notification.
 *
 * We do not care what those pairs are, we just want to call a method to cause
 * the notification to happen. That method is performNotification and it is
 * present because of the INotifyPair interface.
 *
 * Note that the actual notify pair class is not needed, and in fact is most
 * likely a private class local to the class that is using NotifierRunner, and
 * that implements the interface.
 *
 */
public class NotifierRunner extends Object implements Runnable
{
    private static final long PERIODIC_TIMEOUT =  5L * 1000L;
    private static final long REPORT_TIMEOUT   = 15L * 1000L;

    private final BlockingQueue<? extends INotifyPair> _queue;
    private final AtomicBoolean                        _shuttingDown;
    private final Tracer                               _log;
    private final String                               _name;


    /**
     * Constructor.
     *
     * @param queue        Queue to monitor
     * @param shuttingDown Set true if we are to shut down
     * @param log          Tracer
     * @param name         Name for logging purposes
     */
    public NotifierRunner(
               final BlockingQueue<? extends INotifyPair> queue,
               final AtomicBoolean                        shuttingDown,
               final Tracer                               log,
               final String                               name)
    {
        super();

        _queue        = queue;
        _shuttingDown = shuttingDown;
        _log          = log;
        _name         = name;
    }


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
        catch (RuntimeException re)
        {
            _log.error(_name + " handle unexpected error: " + re);

            throw re;
        }
        catch (Exception e)
        {
            _log.error(_name + " handle unexpected error: " + e);
        }
    }


    /**
     * Perform actual work in this thread.
     */
    private void internalRun()
    {
        _log.info(_name + " is starting");

        long reportTime = System.currentTimeMillis() + REPORT_TIMEOUT;
        int _oldSize    = 0;

        while (! _queue.isEmpty() || ! _shuttingDown.get())
        {
            INotifyPair next = null;

            try
            {
                next = _queue.poll(PERIODIC_TIMEOUT,
                                   TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ie)
            {
                // No handling necessary
            }

            if (next != null)
            {
                next.performNotification();
            }

            final long now = System.currentTimeMillis();

            if (now >= reportTime)
            {
                reportTime = now + REPORT_TIMEOUT;

                final int size = _queue.size();

                // Report if the queue is non-empty, or if it has just
                // become empty.

                if ((size > 0) || (_oldSize > 0))
                {
                    _log.info(_name + " queue size is " + size);
                }

                _oldSize = size;
            }
        }

        _log.info(_name + " is exiting");
    }


    /** Interface for all notify pairs that will use NotifierRunner */
    public static interface INotifyPair
    {
        /**
         * Perform notification.
         */
        public void performNotification();
    }
}
