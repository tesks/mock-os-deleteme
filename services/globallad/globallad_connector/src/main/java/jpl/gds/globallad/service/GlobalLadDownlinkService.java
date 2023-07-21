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
package jpl.gds.globallad.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.ApplicationContext;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.AlarmHistoryGlobalLadData;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.GlobalLadUtilities;
import jpl.gds.globallad.disruptor.GlobalLadDataEvent;
import jpl.gds.globallad.service.GlobalLadSinkIsDeadPerformance.GlobalLadSinkPerformanceData;
import jpl.gds.globallad.service.io.GlobalLadMessageEventHandler;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.IPerformanceProvider;
import jpl.gds.shared.performance.PerformanceSummaryPublisher;
import jpl.gds.shared.performance.QueuePerformanceData;


/**
 * Downlink service for the global lad.
 * 
 * Moved the connection logic into the event handler class.
 * Added isRunning flag to prevent double publishing of alarm history
 */
public class GlobalLadDownlinkService implements IPerformanceProvider, IGlobalLadDownlinkService {
	private static final Tracer tracer = GlobalLadProperties.getTracer();



	private Disruptor<GlobalLadDataEvent> disruptor;
	private ExecutorService executor;
	
	private EhaEvrGladInserterSubscriber subscriber;
	private GlobalLadMessageEventHandler handler;

	private final QueuePerformanceData performance;
	
	private final IMessagePublicationBus bus;
	private final ApplicationContext appContext;
	
	/**
	 * Getting the number of publish retries from the configuration
	 */
	private final int publishRetries;
	
	/**
	 * Make this parameter configurable. Was a constant.
	 */
	private final long publishRetryIntervalMs;
	
	private final long publishRetryLoggingIntervalNs;
	
    private final Tracer                  log;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

	/**
     * Constructor for the GlobalLad downlink service
     * 
     * @param context
     *            The current spring ApplicationContext
     * 
     */
	public GlobalLadDownlinkService(final ApplicationContext context) {
		super();
		
		appContext = context;
		
	    bus = context.getBean(IMessagePublicationBus.class);
			
	    publishRetries = GlobalLadProperties.getGlobalInstance().getDownlinkSinkPublishRetry();
		publishRetryIntervalMs = GlobalLadProperties.getGlobalInstance().getDownlinkSinkPublishInterval();
		publishRetryLoggingIntervalNs = GlobalLadProperties.getGlobalInstance().getDownlinkSinkPublishLoggingInterval() * 1000000L;
		
		final int maxSize = GlobalLadProperties.getGlobalInstance().getDownlinkRingBufferSize();

		performance = new QueuePerformanceData(
				context.getBean(PerformanceProperties.class),
				"GlobalLadSink", 
				maxSize,  // Max size
				true, // isBacklog, 
				true, // isThrottle,
				"messages" // unit
				);

        log = TraceManager.getTracer(context, Loggers.GLAD);

		/**
		 * Set the red and yellow threshold percentage.
		 */
		performance.setYellowBound(GlobalLadProperties.getGlobalInstance().getDisruptorYellowThreshold());
		performance.setRedBound(GlobalLadProperties.getGlobalInstance().getDisruptorRedThreshold());
	}

	/* (non-Javadoc)
	 * @see jpl.gds.shared.performance.IPerformanceProvider#getProviderName()
	 */
	@Override
	public String getProviderName() {
		return "GlobalLadDownlinkService";
	}

	/* (non-Javadoc)
	 * @see jpl.gds.shared.performance.IPerformanceProvider#getPerformanceData()
	 */
	@Override
	public List<IPerformanceData> getPerformanceData() {
		performance.setCurrentQueueSize(subscriber.getRingBufferBacklog());
		performance.setHighWaterMark(subscriber.getHighWaterMark());
		
		return Arrays.<IPerformanceData>asList(performance);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.shared.interfaces.IService#startService()
	 */
	@Override
	public boolean startService() {
		/**
		 * First create the event handlers, that will connect to the glad.  If it fails 
		 * to connect return false.
		 */
		final EventHandler<GlobalLadDataEvent>[] handlers = new GlobalLadMessageEventHandler[1];
		try {
			handlers[0] = new GlobalLadMessageEventHandler();
		} catch (final IOException e) {
			// If this fails we could not start the service.
			tracer.error("Could not start the global lad service: " + e.getMessage(), e.getCause());
			return false;
		}
		
		executor = Executors.newCachedThreadPool(GlobalLadUtilities.createThreadFactory("global-lad-writer-%d"));
		
		/**
		 * Set up the disruptor that will be used instead of a queue.  
		 * Creating the wait strategy here so the global lad configuration does not need to depend on the disruptor.
		 */
		WaitStrategy downlinkWaitStrategy;
		
		switch(GlobalLadProperties.getGlobalInstance().getDownlinkWaitStrategy()) {
		case SLEEP:
			downlinkWaitStrategy = new SleepingWaitStrategy();
			break;
		case SPIN:
			downlinkWaitStrategy = new BusySpinWaitStrategy();
			break;
		case YIELD:
			downlinkWaitStrategy = new YieldingWaitStrategy();
			break;
		case BLOCK:
		default:
			downlinkWaitStrategy = new BlockingWaitStrategy();
			break;
		}

		disruptor = new Disruptor<GlobalLadDataEvent>(
				GlobalLadDataEvent.DATA_EVENT_FACTORY, //eventFactory, 
				GlobalLadProperties.getGlobalInstance().getDownlinkRingBufferSize(), 
				executor, 
				ProducerType.MULTI, 
				downlinkWaitStrategy);
		
		disruptor.handleEventsWith(handlers);
		handler = (GlobalLadMessageEventHandler) handlers[0];
		
		/**
		 * Must handle exceptions in events such as dictionary issues or any other issue that is
		 * unforseen. If not will kill the downlink.
		 */
		final ExceptionHandler<GlobalLadDataEvent> exceptionHandler = new ExceptionHandler<GlobalLadDataEvent>() {

			@Override
			public void handleEventException(final Throwable ex, final long sequence, final GlobalLadDataEvent event) {
				/**
				 * Check if we have failed to connect for good
				 * and we need to stop connecting to the global lad. This case is meant to handle
				 * a loss of connectivity to the global lad after the downlink has started and it 
				 * will allow the downlink to continue.  This has nothing to do with starting the downlink 
				 * and the LAD is not running.
				 */
				if (ex instanceof GlobalLadDownlinkServiceConnectionException) {
					tracer.error("Global LAD is not responding.  GlobalLadDownlinkService shutting down", ex.getCause());
					stopService();
				} else {
					tracer.error(String.format("Exception encountered processing Global LAD data event with sequence %d: %s", sequence, ex.getMessage()), ex.getCause());
				}
			}

			/* (non-Javadoc)
			 * @see com.lmax.disruptor.ExceptionHandler#handleOnStartException(java.lang.Throwable)
			 */
			@Override
			public void handleOnStartException(final Throwable ex) {
				// Don't care
			}

			/* (non-Javadoc)
			 * @see com.lmax.disruptor.ExceptionHandler#handleOnShutdownException(java.lang.Throwable)
			 */
			@Override
			public void handleOnShutdownException(final Throwable ex) {
				// Don't care
			}
		};
		
		disruptor.handleExceptionsFor(handler).with(exceptionHandler);
		
		disruptor.start();
		
		/**
		 * Set up the message listener.  The translator instance can be reused so pass it to all of the glad inserters.
		 */
		final EventTranslatorOneArg<GlobalLadDataEvent, IMessage> translator = new BasicMessageToGlobalLadEventTranslator(appContext);
		subscriber = new EhaEvrGladInserterSubscriber(disruptor.getRingBuffer(), translator);
		
		bus.subscribe(EvrMessageType.Evr, subscriber);
		bus.subscribe(EhaMessageType.AlarmedEhaChannel, subscriber);
		
		tracer.debug("Global LAD service has been initialized.");

		/**
		 * Once everything is up and running add self for performance monitoring.
		 */
		appContext.getBean(PerformanceSummaryPublisher.class).registerProvider(this);
		
		this.isRunning.set(true);

		return isRunning.get();
	}
	
	private void publishAlarmHistory() {
		/**
		 * Once we are asked to shut down, get the alarm history and publish to the GLAD.
		 */
		final IGlobalLADData ad = new AlarmHistoryGlobalLadData(appContext.getBean(IAlarmHistoryProvider.class),
				appContext.getBean(IContextConfiguration.class));
		
        log.debug("publishing alarm history to glad: ", ad);

		disruptor.getRingBuffer().publishEvent(GlobalLadDataEvent.DATA_TRANSLATOR, ad);
	}

	/**
	 * This calls shutdown on this feature and will add the global lad performance sink to indicate that the lad is dead.
	 * 
	 */
	private void shutdownServiceDueToError() {
		stopService();

		final GlobalLadSinkPerformanceData data = new GlobalLadSinkPerformanceData(true, appContext.getBean(PerformanceProperties.class));
		data.setGood(false);

		appContext.getBean(PerformanceSummaryPublisher.class).registerProvider(new GlobalLadSinkIsDeadPerformance(data));
	}

	@Override
	public void stopService() {
		if(isRunning.compareAndSet(true, false)) {
			publishAlarmHistory();
			bus.unsubscribeAll(subscriber);
			appContext.getBean(PerformanceSummaryPublisher.class).deregisterProvider(this);

			/**
			 * Wait for everything to get flushed from the ring buffer.
			 * Must halt the disruptor first in case this is killing the glad service after loosing connection.
			 */
			if (null != disruptor) {
				disruptor.halt();
				disruptor.shutdown();
				disruptor = null;
			}

			/**
			 * Must also shutdown the executor service.  If a worker thread is running and this is not shutdown the 
			 * process will never exit.
			 */
			if (null != executor) {
				executor.shutdown();
				executor = null;
			}

			tracer.debug("Global LAD service has shutdown.");
		}
	}
	
	/**
	 * This is the message subscriber as well as the disruptor data producer.
	 */
	private class EhaEvrGladInserterSubscriber implements MessageSubscriber {
		private final RingBuffer<GlobalLadDataEvent> ringBuffer;
		private final EventTranslatorOneArg<GlobalLadDataEvent, IMessage> translator;
        protected AtomicLong                                              messageCount;
        protected AtomicLong                                              highWaterMark;
		
		/**
         * @param ringBuffer
         *            The GlobalLad event ringbuffer
         * @param translator
         *            Event translator to help with converting a <GlobalLadDataEvent> to an AMPCS <IMessage>
         */
		public EhaEvrGladInserterSubscriber(final RingBuffer<GlobalLadDataEvent> ringBuffer, 
				final EventTranslatorOneArg<GlobalLadDataEvent, IMessage> translator) {
			super();
			this.ringBuffer = ringBuffer;
			this.translator = translator;
			this.messageCount = new AtomicLong();
			this.highWaterMark = new AtomicLong();
		}
		
		@Override
		public void handleMessage(final IMessage message) {
			/**
			 * We only want to publish evr or eha events.  However we will only subscribe to those 
			 * types so do no filtering here and assume we are good.
			 * 
			 * Going to detect when we cannot publish to the ring buffer.
			 * 	If we are not dropping data this will try to add the data in a retry loop.
			 * 	If that fails, we start to dump data.
			 * 	If the dump data flag is set, that means we already detected a bogus state and from now on if
			 * 		we cannot add the first time we just throw the data away.
			 */
			boolean wasPublished = ringBuffer.tryPublishEvent(translator, message);

			if (wasPublished) {
				messageCount.incrementAndGet();
			} else {
				int retryCount = 0;
                tracer.warn("Event was not published to ring buffer due to low capacity.  Entering retry loop. ");

                tracer.debug("BufferBacklog=", getRingBufferBacklog(),
                             ", BufferSize=", ringBuffer.getBufferSize(), 
                             ", ItemsInRingBuffer=", (ringBuffer.getBufferSize() - ringBuffer.remainingCapacity()),
                             ", RetryCount=", retryCount, 
                             ", RetryInterval=", publishRetryIntervalMs,
                             ", publishRetries=", publishRetries);

				long lastRetryLog = System.nanoTime();
				do {
					retryCount++;
					wasPublished = ringBuffer.tryPublishEvent(translator, message);
					
					if (!wasPublished) {
						// Small timeout...
						final long currentNanoTime = System.nanoTime();
						if (currentNanoTime - lastRetryLog >= publishRetryLoggingIntervalNs) {
							// Log only once a second
							tracer.warn("Still attempting buffer publish retry...");
							lastRetryLog = currentNanoTime;
						}
						try {
							Thread.sleep(publishRetryIntervalMs);
						} catch (final InterruptedException e) {
							// Don't care just move on.
						}
					}
				} while (!wasPublished && retryCount < publishRetries);
			}
			
			if (wasPublished) {
				/**
				 * Figure out the high water mark from the ring buffer.  The size is the difference between the message count 
				 * and the sequence of the subscriber.
				 */
			final long backLog = getRingBufferBacklog();

				highWaterMark.set(Math.max(highWaterMark.get(), backLog));
			} else {
				tracer.error("All retry attempts to publish to global lad ring buffer failed.  Shutting down the Global LAD feature.");
				shutdownServiceDueToError();
			}
		}
		
		public long getRingBufferBacklog() {
			final long seq = GlobalLadDownlinkService.this.handler.lastSequence.get();
			/**
			 * Must be minus one because the sequence starts at 0 and when we get the sequence
			 * that is the sequence number for the next message that comes in.
			 */
			final long msgCount = messageCount.get();
			
			return msgCount == 0 ? 0 : msgCount - seq - 1;
		}
		
		public long getHighWaterMark() {
			return highWaterMark.get();
		}
	}
}
