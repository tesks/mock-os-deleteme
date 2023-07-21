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
package jpl.gds.shared.sys;

import java.lang.Thread.State;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.Pair;


/**
 * Class Shutdown. This class is designed to accept either functors or threads
 * that implement clean-up and shutdown actions. It exists primarly because
 * the Runtime shutdown hook capability is beyond our control; we need to be
 * able to order the actions, for example. A specific problem we are having
 * involves the invalidation of a security token at application end. It is
 * strongly desired that we invalidate the token, but trying to do it in the
 * shutdown-hook facility causes problems because the token becomes invalidated
 * when it is still needed. This is because we cannot easily order the shutdown
 * activities.
 *
 * The functors are the desired way to implement these actions. They would
 * typically be anonymous inner classes, but need not be. For each functor, an
 * enum constant is created, and the functor is then added under that constant.
 * The enum class provides an ordering, so the users must juggle the ordering
 * as required each time a new functor is added.
 *
 * Thread hooks are also accepted. Those are just like the ones for the Runtime
 * facility; in fact, some of the existing hooks might well be moved here.
 * The threads are started if necessary and waited for.
 *
 * Finally, arbitrary threads that may or may not be already started can be
 * added. Those are not started, but are waited for.
 *
 * Each thread has a timeout associated with it, which may be infinite (zero.)
 *
 * The intent is that the application populates Shutdown and then directs it
 * to run the actions right before calling System.exit. Shutdown is NOT run
 * as a Runtime shutdown hook. Shutdown does not call System.exit itself in
 * order to avoid a lot of complexity.
 *
 * A simple thread is created and added as a Runtime shutdown hook for the
 * purpose of checking that the Shutdown actions were performed. All it does
 * is log.
 *
 * The functors and threads should not call Shutdown methods or System.exit.
 * They should be simple and fast.
 *
 * This is not a singleton class, but you would probably have only one.
 *
 *
 * @param <ShutdownEnum> Enum type
 */
public final class Shutdown<ShutdownEnum extends Enum<ShutdownEnum>>
    extends Object
{
    /** Functional interface */
    public static interface IShutdown
    {
        /**
         * Run the shutdown procedure. Should not throw.
         *
         * @param doLog If true log result
         */
        public void runShutdown(final boolean doLog);
    }

    /** Thread name to be used with SIGTERM handlers */
    public static final String                 THREAD_NAME        = "SIGTERM Handler";

    /** Wait forever */
    public static final long INFINITE_WAIT = 0L;

    /** All registered functors */
    private final Map<ShutdownEnum, IShutdown> _functors;

    /** All registered unstarted threads */
    private final Set<TimedThread> _unstarted = new HashSet<TimedThread>();

    /** All registered monitored threads */
    private final Set<TimedThread> _monitored = new HashSet<TimedThread>();

    private final AtomicBoolean _shutdownInitiated = new AtomicBoolean(false);
    private final AtomicBoolean _shutdownCompleted = new AtomicBoolean(false);

    private final String _whoami;

    private final Tracer _log;


    /**
     * Constructor.
     *
     * @param clss       Class of ShutdownEnum
     * @param background True if background thread is to be run
     * @param trace the context Tracer
     */
    public Shutdown(final Class<ShutdownEnum> clss,
                    final boolean             background, 
                    final Tracer trace)
    {
        super();

        final String enumName = clss.getSimpleName();

        _functors = new EnumMap<ShutdownEnum, IShutdown>(clss);
        _whoami   = "Shutdown<" + enumName + ">: ";
        _log      = trace;
        _log.setPrefix(_whoami);

        if (background)
        {
            // Register a thread to check that we have completed

            final Thread t =
                new Thread(new CheckThread(),
                           "ShutdownCheckThread<" + enumName + ">");

            t.setDaemon(true);

            Runtime.getRuntime().addShutdownHook(t);
        }
    }


    /**
     * Add a functor of a specific type.
     *
     * @param se Type of functor
     * @param is Functor
     */
    public synchronized void addFunctorHook(final ShutdownEnum se,
                                            final IShutdown    is)
    {
        if (se == null)
        {
            throw new IllegalArgumentException(_whoami +
                                               " Enum cannot be null");
        }

        if (is == null)
        {
            throw new IllegalArgumentException(_whoami +
                                               se      +
                                               " functor cannot be null");
        }

        if (! shutdownInitiated())
        {
            _functors.put(se, is);
        }
        else if (! shutdownCompleted())
        {
            _log.warn("Attempt to add functor hook ", se,
                            " after shutdown started");
        }
        else
        {
            _log.warn("Attempt to add functor hook ", se,
                            " after shutdown completed");
        }
    }


    /**
     * Add an unstarted thread. We do not insist that it be unstarted.
     *
     * @param t       Thread
     * @param timeout Time to wait in milliseconds
     */
    public synchronized void addThreadHook(final Thread t,
                                           final long   timeout)
    {
        if (! shutdownInitiated())
        {
            if (t.getState() != State.TERMINATED)
            {
                _unstarted.add(new TimedThread(t, timeout));
            }
        }
        else if (! shutdownCompleted())
        {
            _log.warn("Attempt to add thread hook ", t.getName(),
                            " after shutdown started");
        }
        else
        {
            _log.warn("Attempt to add thread hook ", t.getName(),
                            " after shutdown completed");
        }
    }


    /**
     * Add an unstarted thread. We do not insist that it be unstarted.
     *
     * @param t Thread
     */
    public void addThreadHook(final Thread t)
    {
        addThreadHook(t, INFINITE_WAIT);
    }


    /**
     * Add a thread to monitor. We will never attempt to start it. It may be
     * started later, so we don't complain.
     *
     * @param t       Thread
     * @param timeout Time to wait in milliseconds
     */
    public synchronized void addMonitoredThread(final Thread t,
                                                final long   timeout)
    {
        if (! shutdownInitiated())
        {
            if (t.getState() != State.TERMINATED)
            {
                _monitored.add(new TimedThread(t, timeout));
            }
        }
        else if (! shutdownCompleted())
        {
            _log.warn("Attempt to add monitored thread hook ", t.getName(),
                            " after shutdown started");
        }
        else
        {
            _log.warn("Attempt to add monitored thread hook ", t.getName(),
                            " after shutdown completed");
        }
    }


    /**
     * Add a thread to monitor. We will never attempt to start it. It may be
     * started later, so we don't complain.
     *
     * @param t Thread
     */
    public void addMonitoredThread(final Thread t)
    {
        addMonitoredThread(t, INFINITE_WAIT);
    }


    /**
     * Get shutdown start state.
     *
     * @return True if shutdown has been initiated
     */
    public boolean shutdownInitiated()
    {
        return _shutdownInitiated.get();
    }


    /**
     * Get shutdown completion state.
     *
     * @return True if shutdown has been completed
     */
    public boolean shutdownCompleted()
    {
        return _shutdownCompleted.get();
    }


    /**
     * Run shutdown procedures.
     *
     * We run unsynchronized in case one of our functors or threads calls a
     * method of Shutdown. They will think that everything has been done.
     * We cannot protect against them calling System.exit.
     *
     * Once we get past the initial clause we can access the maps and sets
     * without worry.
     *
     * Note that the threads may have terminated after we put them in the sets
     * so we check again.
     *
     * @return True if the shutdown was actually performed
     */
    public boolean shutdown()
    {
        synchronized (this)
        {
            if (_shutdownInitiated.getAndSet(true))
            {
                return false;
            }
        }

        _log.debug("Started shutdown");

        // Run all functors first (they will be run in enum order)

        for (final Map.Entry<ShutdownEnum, IShutdown> entry :
                 _functors.entrySet())
        {
            final ShutdownEnum se = entry.getKey();

            try
            {
                entry.getValue().runShutdown(true);

                _log.debug("Performed functor hook ", se);
            }
            catch (final Exception e)
            {
                // Should not throw on purpose.
                // We catch to protect the shutdown process.
                // Purposely catch runtimes as well.

                _log.warn("Problem performing functor hook " +
                                se +
                                ": " +
                        ExceptionTools.rollUpMessages(e), e.getCause());
            }
        }

        _functors.clear();

        // Start all unstarted threads. If they have already been started or
        // have exited, that is OK; we did our job in any case.
        // Add them to the monitored set so we will wait for them.

        for (final TimedThread tt : _unstarted)
        {
            final Thread t = tt.getThread();

            if (t.getState() == State.NEW)
            {
                try
                {
                    t.start();
                }
                catch (final IllegalThreadStateException itse)
                {
                    // Must have been a race condition on starting it

                    SystemUtilities.doNothing();
                }
            }

            // Could have terminated immediately or was already or didn't start

            if (t.isAlive())
            {
                _monitored.add(tt);
            }
        }

        _unstarted.clear();

        // Wait for threads to complete or time-out or die.

        for (final TimedThread tt : _monitored)
        {
            final Thread t    = tt.getThread();
            final String name = t.getName();

            if (! t.isAlive())
            {
                _log.debug("Skipped join of thread hook ", name);

                continue;
            }

            final long timeout = tt.getTimeout();

            try
            {
                SystemUtilities.ignoreStatus(
                    SleepUtilities.fullJoin(t, timeout));

                if (! t.isAlive())
                {
                    _log.debug("Joined thread hook ", name);
                }
                else
                {
                    _log.warn("Timeout joining thread hook ", name, " after ", timeout, " in state ",
                                    t.getState());
                }
            }
            catch (final ExcessiveInterruptException eie)
            {
                _log.warn("Problem joining thread hook " +
                                name + 
                                " in state " +
                                t.getState() +
                                ": " +
                        ExceptionTools.rollUpMessages(eie), eie.getCause());
            }
        }

        _monitored.clear();

        _log.debug("Completed shutdown");

        Configurator.shutdown(LoggerContext.getContext());

        _shutdownCompleted.set(true);

        return true;
    }


    /**
     * Try to shutdown, and exit if we actually do it. Otherwise, return.
     *
     * If this method returns it means that someone else is handling or has
     * handled the shutdown and will presumably call exit. The caller can
     * then do something else.
     *
     * @param status Exit status
     *
     * @return True if shutdown has completed
     */
    public boolean shutdownAndConditionalExit(final int status)
    {
        if (shutdown())
        {
            System.exit(status);
        }

        return shutdownCompleted();
    }


    /**
     * Shutdown hook to check that we have been called.
     */
    private final class CheckThread extends Object implements Runnable
    {
        /**
         * Constructor.
         */
        public CheckThread()
        {
            super();
        }


        /**
         * Wiil run as a shutdown hook. Do not synchronize.
         * We expect the flags to be set at this point.
         *
         * The catches are just in case there is a logging problem.
         */
        @Override
        public void run()
        {
            if (! shutdownInitiated())
            {
                try
                {
                    _log.debug("Shutdown process was not started");
                }
                catch (final Exception e)
                {
                    SystemUtilities.doNothing();
                }
            }
            else if (! shutdownCompleted())
            {
                try
                {
                    _log.debug("Shutdown process has not completed");
                }
                catch (final Exception e)
                {
                    SystemUtilities.doNothing();
                }
            }
        }
    }


    /** Class to hold a thread and a timeout */
    private final class TimedThread extends Pair<Thread, Long>
    {
		private static final long serialVersionUID = 1L;


		/**
         * Constructor.
         *
         * @param t       Thread
         * @param timeout Timeout
         */
        public TimedThread(final Thread t,
                           final long   timeout)
        {
            super(t, (timeout > 0L) ? timeout : INFINITE_WAIT);

            if (t == null)
            {
                throw new IllegalArgumentException(_whoami +
                                                   "Thread cannot be null");
            }
        }


        /**
         * Getter for Thread.
         *
         * @return Thread
         */
        public Thread getThread()
        {
            return getOne();
        }


        /**
         * Getter for timeout.
         *
         * @return Timeout
         */
        public long getTimeout()
        {
            return getTwo();
        }
    }
}
