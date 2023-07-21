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
package jpl.gds.jms;


import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.jms.config.JmsProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.CheckMessageService;
import jpl.gds.message.api.external.ExternalDeliveryMode;
import jpl.gds.message.api.external.IAsyncTopicPublisher;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.spill.ISpillProcessor;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.QueuePerformanceData;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.thread.SleepUtilities;

/**
 * This is a JMS publisher class that is capable of queuing up submitted
 * messages and publishing them on a separate thread. It can be transacted or
 * not transacted in its publication. It is abstract because it must be extended
 * per-JMS provider.
 */
public abstract class AbstractAsynchronousTopicPublisher extends
        AbstractTopicPublisher implements Runnable, IAsyncTopicPublisher
{

    /* String constant used for log messages. */
    private static final String JMS_PUBLISHER_FOR_TOPIC =
        "JMS publisher for topic ";

    /**
     * Jms Debug logger. Using non-publishing fast tracer.
     */
    protected static Tracer JMS_DEBUG_LOGGER;

 
    /** Delay this much each time between checks for idle down*/
    private static final long STATUS_IDLE_DOWN_DELAY = 250L;

    /** Give up after this much time trying to idle down. */
    private static final long STATUS_IDLE_DOWN_ABORT = 600000L;  // 10 minutes

    /** Time to wait for publication thread to complete. */
    private static final long PUBLICATION_THREAD_JOIN_WAIT = 10000L;
    
    /**
     * Underlying queue used by spill processing.
     */
    private final BlockingQueue<MessageParams> internalMessageToSend;

    /**
     * Queue used to hold messages waiting to be sent.
     */
    private ISpillProcessor<MessageParams> messageToSend;

    /**
     * Signifies if the publisher is closed.
     */
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    
    /**
     * Stop toggle used to publish queued messages when a close is issued.
     */
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    /**
     * The topic name published to.
     */
    private final String topicName;
    /**
     * How many messages to publish at once.
     */
    private final int transactionSize;
    /**
     * Holds the transaction messages until published.
     */
    private final MessageParams[] uncommittedMessages;
    /**
     * How many uncommitted messages are currently being held.
     */
    private int uncommittedCount = 0;
    /**
     * Synchronize object used to prevent closing the publisher while it is
     * currently processing.
     */
    private final Object waitObject = new Object();
    
    /**
     * Queue performance data.
     */
    private final QueuePerformanceData queuePerformance;
    
    /** Queue high water mark */
    private long highWaterMark = 0;
    
    /**
     * Flag indicating if the JMS connection is working.
     */
    private boolean jmsIsActive = true; // assume it's up and running

    /**
     * Worker thread that performs the publication.
     */
	private Thread publisherThread;
	
	  /**
     * Default max queue length if sfp output queuing is enabled and the
     * getSfpOutputQueueSize is above this value, or of the internal queue 
     * if sfp output is disabled.
     */
    private final int maxQueueLen;
    
    /**
     * The current message service configuration.
     */
    protected MessageServiceConfiguration msgConfig;

    private final SseContextFlag               sseFlag;
        
    /**
     * Constructor.
     * @param appContext the current application context
     * @param topicConfig topic configuration used to configure the publisher.
     * @param paramTransactionSize Number of messages to send to JMS before committing 
     * @param outputDir the output directory for spilling messages
     * @throws MessageServiceException if an error creating the publisher occurs
     */
    public AbstractAsynchronousTopicPublisher(final ApplicationContext appContext,
            final JmsTopicConfiguration topicConfig, final int paramTransactionSize,
            final String outputDir) throws MessageServiceException {
        super(appContext, topicConfig, paramTransactionSize == 1 ? false : true);

        msgConfig = appContext.getBean(MessageServiceConfiguration.class);
        final JmsProperties jmsc = appContext.getBean(JmsProperties.class);
        jmsc.init(); // complete initialization (if required) before use.
        final PerformanceProperties perfProps = appContext.getBean(PerformanceProperties.class);
        
        JMS_DEBUG_LOGGER = TraceManager.getTracer(appContext, Loggers.JMS);
        JMS_DEBUG_LOGGER.setPrefix("Async JMS Publisher: ");
        this.sseFlag = appContext.getBean(SseContextFlag.class);
        
        maxQueueLen = jmsc.getMaxAsyncPublisherQueueSize();
        
        topicName = getName();

        final boolean sfp = jmsc.isSpillEnabled();
        int size = maxQueueLen;

        if (sfp) {
            size = Math.min(maxQueueLen, jmsc.getMaxSpillQueueSize());
        }

        /*
         * This is the in-memory queue of messages to be published.
         * It is then wrapped by the SpillProcessor, which also has a 
         * disk queue.
         */
        internalMessageToSend = new ArrayBlockingQueue<>(size);

        // Get spill processor bean
        messageToSend = appContext.getBean(ISpillProcessor.class, outputDir, internalMessageToSend, size, sfp,
                                           MessageParams.class, topicName, jmsc.isKeepSpillFilesEnabled(),
                                           jmsc.getSpillOutputWait(), JMS_DEBUG_LOGGER, sseFlag);

        /*
         * Set up queue performance tracking.  Red and
         * yellow levels are percentages of the in-memory queue size. 
         */
        final long redLevel =  (long)((jmsc.getRedQueuePercentage()/100.0) * size);
        final long yellowLevel =  (long)((jmsc.getYellowQueuePercentage()/100.0) * size);
        
        /* Create different performance data depending on SFP enable state. */
        if (sfp) {
        	queuePerformance  = new QueuePerformanceData(perfProps,
        			"JMS Publisher (" + this.topicName + ")", true, yellowLevel, redLevel, "messages");
        } else {
        	queuePerformance  = new QueuePerformanceData(perfProps,
        			"JMS Publisher (" + this.topicName + ")", maxQueueLen, false, true, "messages");
        }
        /*
         * Start the spill processor.
         */
        messageToSend.start();

        this.transactionSize = paramTransactionSize;
        uncommittedMessages = new MessageParams[paramTransactionSize];
    }
    
    /**
     * Idle down and stop the sending of status messages.
     *
     * The idea is we want to be able to report a zero count
     * without necessarily stopping the portal.
     */
    private void waitToClearMessages()
    {

        int size = 0;

        synchronized (waitObject)
        {
            final long tooLong = System.currentTimeMillis() +
                                 STATUS_IDLE_DOWN_ABORT;

            while (messageToSend.size() != 0)
            {
                // Grab current size so it's consistent throughout loop

                size = messageToSend.size();

                final boolean exit = (size == 0) || (System.currentTimeMillis() >= tooLong);

          
                // If we have zero, we're done and we reported it.
                // Also bail if we have waited long enough

                if (exit)
                {
                    break;
                }

                // Otherwise, wait for a while and try again

                SleepUtilities.checkedWait(waitObject, STATUS_IDLE_DOWN_DELAY);
            }
        }

        if (size == 0)
        {
            JMS_DEBUG_LOGGER.debug(JMS_PUBLISHER_FOR_TOPIC ,
                                   topicName ,
                                   " has no backlog");
        }
        else
        {
            JMS_DEBUG_LOGGER.debug(JMS_PUBLISHER_FOR_TOPIC ,
                                   topicName ,
                                   " still has a backlog of " ,
                                   size);
        }
    }
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.message.api.external.IAsyncTopicPublisher#flushQueue()
	 */
	@Override
	public void flushQueue() {
    	
    	JMS_DEBUG_LOGGER.debug("Waiting to clear async publisher queue for topic " , this.topicName); 
    	
    	/** Idle down by sending all messages. This can block a long time. */
    	waitToClearMessages();

        
        JMS_DEBUG_LOGGER.debug("Final publisher queue for topic " , this.topicName , " at shutdown is " , messageToSend.size());

    }

    /**
	 * {@inheritDoc}
	 * @see jpl.gds.message.api.external.IAsyncTopicPublisher#close()
	 */
    @Override
    public synchronized void close()
    {
    	/*
    	 * Close the spill processor. This blocks and clears all queues. 
    	 */
    	messageToSend.shutDownAndClose();
 
        /*
         * Tell the publication thread to stop and wait for it.s   
         */
    	stopping.set(true);
        this.publisherThread.interrupt();
        
        SleepUtilities.checkedJoin(this.publisherThread, PUBLICATION_THREAD_JOIN_WAIT, "BUS:: " + this.topicName,
     			JMS_DEBUG_LOGGER);
     	
     	super.close();
     	stopped.set(true);
    }

    /**
     * @{inheritDoc}
     */
    @Override
	public synchronized void queueMessageForPublication(final IMessageType type,
            final byte[] blob, final Map<String, Object> properties,
            final long timeToLive, final ExternalDeliveryMode deliveryMode) {
        if (stopping.get()) {
            throw new IllegalStateException(
                "Async publisher is stopping. Cannot queue " + type);
        }
        if (messageToSend.remainingCapacity() == 0) {
            JMS_DEBUG_LOGGER.debug("Bottleneck in JMS Topic Publisher for " ,
                    topicName , ". Queue is full.");
        }

        final MessageParams mp =
                new MessageParams(type, blob, timeToLive, deliveryMode,
                    properties);

        messageToSend.put(mp);
    }

    /**
     * @{inheritDoc}
     */
    @Override
	public synchronized void queueMessageForPublication(final IMessageType type,
            final String text, final Map<String, Object> properties,
            final long timeToLive, final ExternalDeliveryMode deliveryMode) {
    
        if (stopping.get()) {
             throw new IllegalStateException(
                "Async publisher is stopping. Cannot queue " + type);
        }
        if (messageToSend.remainingCapacity() == 0) {
            JMS_DEBUG_LOGGER.debug("Bottleneck in JMS Topic Publisher for " ,
                    topicName , ". Queue is full.");
        }

        final MessageParams mp =
                new MessageParams(type, text, timeToLive, deliveryMode,
                    properties);

        messageToSend.put(mp);
    }
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.message.api.external.IAsyncTopicPublisher#start()
	 */
	@Override
	public synchronized void start() {
    	stopped.set(false);
    	stopping.set(false);
        publisherThread = new Thread(this, "BUS:: " + this.topicName);
        publisherThread.start();
    }

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
		final boolean isDebug = JMS_DEBUG_LOGGER.isEnabledFor(TraceSeverity.DEBUG);
		long mark = 0L;

		while (!stopping.get()) {
			try {
				// If the JMS connection is dead, attempt to revive it. If that fails,
				// make sure we sleep a few seconds before we try again, in order to not
				// loop tightly.
				checkJmsConnected();

				MessageParams mp = null;
				
				try {
					/* Get the next message from the queue. This will block until a message
					 * is found or the thread is interrupted.
					 */
					mp = messageToSend.poll();
					this.highWaterMark = Math.max(this.messageToSend.size(), this.highWaterMark);
					
				} catch (final InterruptedException e) {
					JMS_DEBUG_LOGGER.debug("publication run thread was interrupted; probably stopping", ExceptionTools.getMessage(e), e);
					mp = null;
				}

				if (mp != null) {
					/* We have a message to publish */

                    mark = internalPublish(isDebug, mark, mp);
					
                    JMS_DEBUG_LOGGER.trace("Last publish on " , topicName , " took " , mark
                            , " milliseconds; transaction size " , transactionSize , "; uncommitted count "
                            , uncommittedCount , " queue size " , "commiting " , uncommittedCount , " messages");
				} else if (stopping.get()){
					// we appear to be stopping so commit if there is anything to
					// commit
					if (transactionSize != 1 && uncommittedCount != 0) {
						JMS_DEBUG_LOGGER.trace("idle commiting " ,
								uncommittedCount , " messages");
						this.commit();
						Arrays.fill(uncommittedMessages, null);
						uncommittedCount = 0;
					}

				}
			} 
			 catch (final MessageServiceException e) {
			    
			    if (e.isDisconnect()) {
			        // This is where we end up if the JMS connection died, but oddly enough,
			        // only usually when JMS comes back. Close the publisher and set the JMS active
			        // state to false.
			    	if (jmsIsActive && !CheckMessageService.checkMessageServiceRunning(msgConfig, null, 0, JMS_DEBUG_LOGGER, false, false)) {
						JMS_DEBUG_LOGGER.error("JMS publisher connection on topic " , this.topicName , " was lost", ExceptionTools.getMessage(e), e);
						JMS_DEBUG_LOGGER.error("JMS server appears to be down  (topic " , this.topicName , "); messages will be queued until it is back up", ExceptionTools.getMessage(e), e);
						super.close();
						jmsIsActive = false;
					}
			        continue;
			    }
                JMS_DEBUG_LOGGER.error(ExceptionTools.getMessage(e), e);
				// Could be due to loss of ActiveMQ, but I have never seen this.
				// Check the connection, 
				// and if JMS is not there, set the JMS active state to false and close
				// the publisher.
				

				JMS_DEBUG_LOGGER
						.error("JMS error on message publication for topic " ,
								topicName, ExceptionTools.getMessage(e), e);
				retryPublishAndCommit(e);

			} catch (final Exception e) {
                JMS_DEBUG_LOGGER.error("Unexpected error on message publication for topic " ,
								topicName, ExceptionTools.getMessage(e), e);
                JMS_DEBUG_LOGGER.error(ExceptionTools.getMessage(e), e);
			}
		}
		
		
        JMS_DEBUG_LOGGER.trace("Publication loop stopped for topic " ,
				topicName);
		
		stopped.set(true);
		
	}

    private void retryPublishAndCommit(final MessageServiceException e) {
				JMS_DEBUG_LOGGER.error(ExceptionTools.getMessage(e), e);
        if (transactionSize != 1 && uncommittedCount != 0) {
        	// Retry the commit
        	try {
        		for (int i = 0; i < uncommittedCount; i++) {
        			final MessageParams mp = uncommittedMessages[i];
                    this.publishTextMessage(mp.getMsg(), mp.getProperties(), mp.getTtl(), mp.getDelivMode());
        		}

        		this.commit();

        	} catch (final MessageServiceException ex) {
        		// Nothing else we can do
                JMS_DEBUG_LOGGER.error(ExceptionTools.getMessage(ex), ex);
        		JMS_DEBUG_LOGGER
								.error("Unrecoverable JMS commit error: " ,
										ExceptionTools.getMessage(e), e);

        	} finally {
        		Arrays.fill(uncommittedMessages, null);
        		uncommittedCount = 0;
        	}
        }
    }

    private void checkJmsConnected() {
        if (!jmsIsActive) {
        	try {
        		open();
        		jmsIsActive = true;
                JMS_DEBUG_LOGGER.info(JMS_PUBLISHER_FOR_TOPIC , this.topicName , " was recreated");
        	} catch (final MessageServiceException e) {
        		super.close();
        		SleepUtilities.checkedSleep(3000L);
        	} 
        }
    }

    private long internalPublish(final boolean isDebug, long mark, MessageParams mp) throws MessageServiceException {
        if (isDebug) {
            JMS_DEBUG_LOGGER.trace("Trying to publish to topic " ,
                    topicName , " message type " , mp.getProperties().get(MetadataKey.MESSAGE_TYPE.toString()));

            mark = System.currentTimeMillis();
        }

        /* Publish the message as binary or text */
        if (mp.isBinary()) {
            this.publishBinaryMessage(mp.getBlob(), mp.getProperties(), mp.getTtl(), mp.getDelivMode());
        } else {
            this.publishTextMessage(mp.getMsg(), mp.getProperties(), mp.getTtl(), mp.getDelivMode());
        }

        mp = null;
        
        if (isDebug) {
            mark = System.currentTimeMillis() - mark;

            JMS_DEBUG_LOGGER.trace("Last publish on " , topicName ,
                            " took " , mark ,
                            " milliseconds; transaction size " ,
                            transactionSize , "; uncommitted count " ,
                            uncommittedCount , " queue size " ,
                            messageToSend.size());

        }

        if (transactionSize != 1) {
            uncommittedMessages[uncommittedCount++] = mp;

            // Time to commit?
            if (uncommittedCount == transactionSize) {
                JMS_DEBUG_LOGGER.trace("commiting " , uncommittedCount , " messages");
                this.commit();
                Arrays.fill(uncommittedMessages, null);
                uncommittedCount = 0;
            }
        }
        return mark;
    }
  
    /**
     * @{inheritDoc}
     * @see jpl.gds.message.api.external.IAsyncTopicPublisher#getPerformanceData()
     */
    @Override
	public List<IPerformanceData> getPerformanceData() {
		this.queuePerformance.setCurrentQueueSize(this.messageToSend.size());
		this.queuePerformance.setHighWaterMark(highWaterMark);
		
		return Arrays.asList((IPerformanceData)this.queuePerformance);
	}


}
