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
package jpl.gds.shared.performance;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.PublishableLogMessage;

/**
 * PerformanceSummaryPublisher allows objects to register as performance
 * providers using the IPerformanceProvider interface. A timer thread
 * periodically requests performance data from each provider, logs any serious
 * states it finds, and send out a summary message containing the whole set of
 * information. There are two timer intervals, one for normal operation, and one
 * for application shutdown, which is shorter.
 * <p>
 * <br>
 * As to the general manner in which performance data is supplied, there should
 * really be relatively few providers. These providers can deliver status of
 * multiple components they manage as a list of IPerformanceData objects. Status
 * will be summarized per provider in the summary message. IPerformanceData
 * objects are copied before being put into the summary message so the providers
 * can feel free to change their IPerformanceData objects between summary
 * sweeps.
 * 
 */
public class PerformanceSummaryPublisher {
	/**
	 * Special tracer for performance information.
	 */
    private final Tracer                                     logger;

	/**
	 *  Setting the defaults for these intervals and adding a way to set them via
	 * methods so we don not need the configuration anymore.
	 */
	
	/**
	 * The standard interval at which performance data is gathered and summary
	 * is published.
	 */
	public static final int NORMAL_INTERVAL_DEFAULT = 5000;
	/**
	 * The interval at which performance data is gathered and summary is
	 * published during application shutdown.
	 */
	public static final int SHUTDOWN_INTERVAL_DEFAULT = 5000;

	
	/**
	 * The list of performance providers. Copy on write to avoid
	 * synchronization.
	 */
	private final CopyOnWriteArrayList<IPerformanceProvider> providers = new CopyOnWriteArrayList<IPerformanceProvider>();

	/**
	 * The timer that triggers performance collection and summaries.
	 */
	private Timer summaryTimer;

	/**
	 * Flag indicating if the timer has been started.
	 */
	private final AtomicBoolean started = new AtomicBoolean(false);

	/**
	 * Flag indicating if switch to shutdown mode has occurred.
	 */
	private final AtomicBoolean inShutdownMode = new AtomicBoolean(false);

	/**
	 * Heap performance object, populated by this class.
	 */
	private final HeapPerformanceData heapPerf;
	
	private final IMessagePublicationBus pubContext;	
	private final PerformanceProperties perfProperties;

	/**
	 * Constructor.
	 * @param context the current ApplicationContext
	 */
	public PerformanceSummaryPublisher(final ApplicationContext context) {
		perfProperties = context.getBean(PerformanceProperties.class);
		heapPerf = new HeapPerformanceData(perfProperties);
		pubContext = context.getBean(IMessagePublicationBus.class);
        logger = TraceManager.getTracer(context, Loggers.PERFORMANCE);
	}


	/**
	 * Starts performance gathering and publication.
	 * 
	 * @param interval - If value is less than or equal to zero will use the default interval.
	 */
	public void start(final int interval) {
	    

		if (!started.getAndSet(true)) {
			startTimer(interval <= 0 ? NORMAL_INTERVAL_DEFAULT : interval);
		}
	}
	
	/**
	 * Stops performance gathering and publication. Also sets the shutdown mode
	 * flag to true, and sends out one last performance summary.
	 */
	public void stop() {
		if (!started.get()) {
			return;
		}

		this.inShutdownMode.set(true);

		if (summaryTimer != null) {
			logger.debug("Shutting down performance summary");

			summaryTimer.cancel();
			summaryTimer = null;
		}

		/* Send out one last summary */
		gatherAndSend();

		started.set(false);
	}

	/**
	 * Starts the summary timer. If the interval is 0, the timer is not started.
	 * 
	 * @param interval
	 *            the interval at which the timer should fire, milliseconds.
	 */
	private void startTimer(final int interval) {

		if (summaryTimer != null) {
			summaryTimer.cancel();
			summaryTimer = null;
		}

		if (interval == 0) {
			return;
		}

        summaryTimer = new Timer("Performance Summary");
		summaryTimer.scheduleAtFixedRate(new TimerTask() {

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void run() {
				try {
					gatherAndSend();
				} catch (final Exception e) {
					logger.error("Unknown exception in Performance Summary Publisher run()");
					e.printStackTrace();
				}

			}
		}, interval, interval);
	}

	/**
	 * The method that actually gathers performance data and publishes
	 * summaries.
	 */
	private void gatherAndSend() {
		
		/*
		 * This is the map that will go into the summary message. Must be
		 * created every time.
		 */
		final Map<String, ProviderPerformanceSummary> perfMap = new HashMap<String, ProviderPerformanceSummary>();

		/* Assume all is well with all providers. */
		HealthStatus overallWorst = HealthStatus.GREEN;
		boolean overallThrottle = false;
		boolean overallBacklog = false;

		/* Moved init of perfListCopy inside the loop below. */

		/* Loop through the providers and collect performance data. */
		for (final IPerformanceProvider provider : providers) {
		    /*
		     * We will copy all performance data to this list. We copy it because
		     * the next time the timer goes off, the providers may update the same
		     * objects, and we don't want users of the summary message to be
		     * affected by those changes.
		     */
		    final List<IPerformanceData> perfListCopy = new LinkedList<IPerformanceData>();

			/* Ask the provider for its data. If it is empty, skip this provider */
			final List<IPerformanceData> perfList = provider.getPerformanceData();
			if (perfList == null || perfList.isEmpty()) {
				continue;
			}

			/* Assume all is well with this provider. */
			HealthStatus providerWorst = HealthStatus.GREEN;
			boolean isThrottling = false;
			boolean isBacklogging = false;

			/* Look at each performance data object returned by the provider. */
			for (final IPerformanceData perf : perfList) {

				/*
				 * If debug logging is enabled, log the data in this performance
				 * data object
				 */
				if (logger.isDebugEnabled()) {
					logger.debug(perf.toLogString());
				}

				/*
				 * Check the health status in the object. If it is worse than
				 * the worst already recorded for this provider, update the
				 * worst provider status.
				 */
				if (perf.getHealthStatus().ordinal() > providerWorst.ordinal()) {
					providerWorst = perf.getHealthStatus();
				}

				/*
				 * If the provider status is RED, then we want to log the
				 * performance data with warning severity. If this performance
				 * data is for a queue, check to see if the queue causes
				 * throttling or backlog, and update the throttling and backlog
				 * flags for this provider.
				 */
				if (perf.getHealthStatus() == HealthStatus.RED) {
					publishLogMessage(TraceSeverity.WARNING, perf.toLogString());
					if (perf instanceof QueuePerformanceData) {
						isThrottling = isThrottling
								| ((QueuePerformanceData) perf).isThrottle();
						isBacklogging = isBacklogging
								| ((QueuePerformanceData) perf).isBacklog();
					}
				}

				/*
				 * Create a copy of the performance data and put it on the list
				 * of copies for this provider.
				 */
				perfListCopy.add(perf.copy());
			}

			/*
			 * We have processed all the performance data for this provider.
			 * Create a ProviderPerformanceData object for this provider and put
			 * it in the summary map. Then log the provider's status.
			 */

			final ProviderPerformanceSummary providerData = new ProviderPerformanceSummary(
					provider.getProviderName(), perfListCopy, providerWorst,
					isBacklogging, isThrottling);
			perfMap.put(provider.getProviderName(), providerData);

            /*
             * Now only log this if status is RED or in debug.
             * Too verbose otherwise.
             */
			if (providerWorst == HealthStatus.RED) {
			    publishLogMessage(TraceSeverity.WARNING, providerData.toLogString());
			} else if (logger.isDebugEnabled()) {
			    publishLogMessage(TraceSeverity.INFO, providerData.toLogString());
			}

			/*
			 * Update overall worst status, throttling, and backlog flags for
			 * all providers.
			 */
			if (providerWorst.ordinal() > overallWorst.ordinal()) {
				overallWorst = providerWorst;
			}
			overallThrottle = overallThrottle | isThrottling;
			overallBacklog = overallBacklog | isBacklogging;

		}

		/*
		 * All providers have been processed. Update the heap status and log it.
		 */
		heapPerf.updateHeapData();
		/* Account for heap status in overall health
		 * assessment.
		 */
		if (heapPerf.getHealthStatus().ordinal() > overallWorst.ordinal()) {
			overallWorst = heapPerf.getHealthStatus();
		}

		/* Log the health of the heap. */
		publishLogMessage(
				heapPerf.getHealthStatus() == HealthStatus.RED ? TraceSeverity.WARNING
						: TraceSeverity.INFO, heapPerf.toLogString());

		/*
		 * Create and publish the summary message. Heap performance data is
		 * copied, because the next time the timer goes off, the one held by
		 * this class will be updated. This arrangement allows tracking of high
		 * water mark across invocations.
		 */
		final PerformanceSummaryMessage sumMsg = new PerformanceSummaryMessage(
				perfMap, overallWorst, overallBacklog, overallThrottle);
		sumMsg.setHeapStatus((HeapPerformanceData) heapPerf.copy());
		pubContext.publish(sumMsg);
		publishLogMessage(sumMsg.getOverallHealth() == HealthStatus.RED ? TraceSeverity.WARNING
                : TraceSeverity.INFO, sumMsg.getOneLineSummary());
	}

	/**
	 * Registers a performance provider.
	 * 
	 * @param provider
	 *            the provider object to register
	 */
	public void registerProvider(final IPerformanceProvider provider) {
		assert provider != null : "registering provider cannot be null";

		if (!providers.contains(provider)) {
			providers.add(provider);
		}
	}

	/**
	 * De-registers a performance provider.
	 * 
	 * @param provider
	 *            the provider object to de-register
	 */
	public void deregisterProvider(final IPerformanceProvider provider) {
		assert provider != null : "de-registering provider cannot be null";

		this.providers.remove(provider);
	}

	/**
	 * Adjusts the publication timer to the shorter shutdown interval, for more
	 * rapid status reporting during shutdown.
	 * 
	 * @param shutdownInterval - If interval is less than or equal to 0 will use the default value.
	 */
	public void setShutdownRate(final int shutdownInterval) {
		this.inShutdownMode.set(true);
		startTimer(shutdownInterval <= 0 ? SHUTDOWN_INTERVAL_DEFAULT : shutdownInterval);
	}
	

	/**
     * Logs the message to the logger only since we do not care about going to a
     * database.
     * 
     * @param severity
     *            the severity level of the message
     * @param message
     *            the detailed message text
     */

    protected void publishLogMessage(final TraceSeverity severity, final String message) {
        final PublishableLogMessage elm = new PublishableLogMessage(CommonMessageType.PerformanceSummary, severity,
                LogMessageType.PERFORMANCE, message);
		if (!this.inShutdownMode.get()) {
            pubContext.publish(elm);
            logger.log(elm);
        }
	}

}
