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

package jpl.gds.tc.impl.icmd;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.tc.api.UplinkLogger;
import jpl.gds.tc.api.icmd.ICpdClient;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesPoller;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesSubscriber;
import jpl.gds.tc.api.icmd.config.IntegratedCommandProperties;
import jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages;

/**
 * Polls CPD for DMS broadcast status messages. Since these are long-polls, we
 * can use an infinite loop.
 *
 * @since AMPCS R7.1
 */
public class CpdDmsBroadcastStatusMessagesPoller implements Runnable, ICpdDmsBroadcastStatusMessagesPoller {

	/** Logger */
	private final UplinkLogger logger;

	/**
	 * The configured number of consecutive failed polls before considering the
	 * data stale
	 */
	private static int FAILED_POLLS_BEFORE_STALE;
	
	/**
	 * The configured interval for the polling thread
	 */
	private static int DMS_POLLING_INTERVAL;

	/** The poller thread (for shutdown/interrupt purposes) */
	private ScheduledExecutorService thread = null;

	/** Flag that tells the poller to continue polling */
	private boolean keepGoing = true;

	/** The client class used to talk to CPD */
	private ICpdClient client;

	/** The number of consecutive failed polls this model has encountered */
	private int failedPolls = 0;

	/**
	 * Subscribers interested in getting callbacks when new DMS broadcast status
	 * messages are received
	 */
	private final Set<ICpdDmsBroadcastStatusMessagesSubscriber> subscribers;

	/**
     * Default but private constructor for CpdDmsBroadcastStatusMessagesPoller.
     * 
     * @param appContext
     *            The current application context
     */
	public CpdDmsBroadcastStatusMessagesPoller(final ApplicationContext appContext) {
		
		logger = new UplinkLogger(appContext, TraceManager.getTracer(appContext,
				                  Loggers.CPD_UPLINK), false);

		try {
		    /* MPCS-9642 - 4/181/8. Fix to use interface name. */
			this.client = appContext.getBean(ICpdClient.class);

		} catch (final Exception e) {
			final String errorMessage = e.getMessage() == null 
					? e.toString() : e.getMessage();

			logger.error("Unable to initialize CPD client for "	+ getClass().getName() + 
					": " + errorMessage);
		}
		
		IntegratedCommandProperties integratedCmdProps = appContext.getBean(
				                                          IntegratedCommandProperties.class);
		CpdDmsBroadcastStatusMessagesPoller.FAILED_POLLS_BEFORE_STALE = integratedCmdProps
				                                                        .getNumFailedPollsBeforeStale();
		CpdDmsBroadcastStatusMessagesPoller.DMS_POLLING_INTERVAL = integratedCmdProps
				                                                     .getDmsPollingInterval();

		this.failedPolls = 0;
		this.subscribers = new HashSet<ICpdDmsBroadcastStatusMessagesSubscriber>(3);
		logger.debug("Creating a new (but not starting) CpdDmsBroadcastStatusMessagesPoller thread");
		thread = Executors.newScheduledThreadPool(1);
        Runtime.getRuntime().addShutdownHook(new QuitSignalHandler(LoggerContext.getContext()));
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		while (keepGoing) {
			logger.trace("Poller polling for DMS broadcast status messages");

			try {
				final CpdDmsBroadcastStatusMessages newMsgs = this.client
						.getDmsBroadcastStatusMessages();
				failedPolls = 0;

				/*
				 * MPCS-5934 - 4/30/2015: Now that unsubscribe can
				 * happen while this thread is running, make sure subscribers
				 * list is handled thread-safely.
				 */
				synchronized (this.subscribers) {

					for (final ICpdDmsBroadcastStatusMessagesSubscriber subscriber : this.subscribers) {
						logger.trace("Calling back ICpdDmsBroadcastStatusMessagesSubscriber " + subscriber);
						subscriber.handleNewMessages(newMsgs);
					}

				}

			} catch (final Exception e) {
				failedPolls++;
				// Comment from earlier method of polling: "This used to be a
				// warning, but it seems that security service is
				// causing the initial poll to fail more often than desired"
				logger.debug(
						"Poll failed for DMS broadcast status messages (fail count is "
								+ failedPolls + ")", e);

				if (failedPolls >= FAILED_POLLS_BEFORE_STALE) {
					logger.debug("Poll fail count for DMS broadcast status messages reached max ("
							+ FAILED_POLLS_BEFORE_STALE
							+ "). Notifying all subscribers of staleness.");

					/*
					 * MPCS-5934 - 4/30/2015: Now that unsubscribe can
					 * happen while this thread is running, make sure subscribers
					 * list is handled thread-safely.
					 */
					synchronized (this.subscribers) {

						for (final ICpdDmsBroadcastStatusMessagesSubscriber subscriber : this.subscribers) {
							subscriber.dataNowStale();
						}

					}

				}

			}

		}

		logger.debug("Poller for DMS broadcast status messages finished");
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void subscribe(final ICpdDmsBroadcastStatusMessagesSubscriber newSubscriber) {
		logger.debug("New subscription made to poller for DMS broadcast status messages: "
				+ newSubscriber);

		synchronized (this.subscribers) {
			this.subscribers.add(newSubscriber);
		}

	}

	/**
     * {@inheritDoc}
     */
	@Override
    public void unsubscribe(final ICpdDmsBroadcastStatusMessagesSubscriber subscriberToRemove) {

		synchronized (this.subscribers) {
			this.subscribers.remove(subscriberToRemove);
		}

		logger.debug("Unsubscribed from poller for DMS broadcast status messages: "
				+ subscriberToRemove);
	}

	/**
     * {@inheritDoc}
     */
	@Override
    public synchronized void stop() {
		logger.debug("CpdDmsBroadcastStatusMessagesPoller stop called");
		keepGoing = false;
		
		if((thread == null) || thread.isTerminated()) {
			logger.trace("CpdDmsBroadcastStatusMessagePoller has not started");
		} else {
			logger.trace("Stopping CpdDmsBroadcastStatusMessagePoller thread");
			thread.shutdown();
		}
	}

/**
	 * Shutdown hook for CpdDmsBroadcastStatusMessagesPoller.
	 *
	 * @since AMPCS R7.1
	 */
	private class QuitSignalHandler extends Thread {

        private final LoggerContext logContext;

        /**
         * @param logContext
         *            The parent thread's logging context
         */
        public QuitSignalHandler(final LoggerContext logContext) {
            this.logContext = logContext;
        }

		/**
		 * {@inheritDoc}
		 *
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			logger.debug("Shutdown handler for CpdDmsBroadcastStatusMessagesPoller firing");
			CpdDmsBroadcastStatusMessagesPoller.this.stop();
            TraceManager.shutdown(logContext);
		}

	}

	/**
     * {@inheritDoc}
     */
	@Override
    public synchronized void start() {
		logger.debug("Starting the CpdDmsBroadcastStatusMessagesPoller thread on a " 
                      + DMS_POLLING_INTERVAL + " millisecond interval");
		thread.scheduleWithFixedDelay(this, 1L, DMS_POLLING_INTERVAL, TimeUnit.MILLISECONDS);
	}

}
