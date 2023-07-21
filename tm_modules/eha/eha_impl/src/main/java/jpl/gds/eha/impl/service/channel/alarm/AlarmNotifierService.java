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
package jpl.gds.eha.impl.service.channel.alarm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import jpl.gds.common.notify.NotificationProperties;
import jpl.gds.common.notify.NotifierRunner;
import jpl.gds.common.notify.NotifierRunner.INotifyPair;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.service.alarm.IAlarmNotifierService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.Pair;


/**
 * Parses configuration file to set up notifications triggered by alarms on
 * channels. Sends notifications as required.
 * 
 * Since notifications are slow, they are performed in a thread. That means that
 * the thread's queue must be drained on shutdown.
 *
 */
public class AlarmNotifierService implements IAlarmNotifierService
{
    private static final String NAME        = "AlarmNotifierService";
    private static final String THREAD_NAME = "AlarmNotifierRunner";
    private static final int MAP_SIZE = 2048;

    private final Tracer                     LOG;


    private boolean notifierOkToStart;
    private List<AlarmNotification> notificationList;

    private final Map<String,String> channelIds;
    private final Map<String,String> modules;
    private final Map<String,String> operationalCategories;
    private final Map<String,String> subsystems;
    
	/**
	 * Added message context and and
	 * subscriber member to listen to internal EhaChannelMessages. Also we now
	 * use a local "LAD" replacement to keep last copies of channel values.
	 */
    private final IMessagePublicationBus messageContext;
    private MessageSubscriber subscriber;
    private final LastChannelValueCache lastChannelValueCache;

    /** Presumably at least one of these is set, or we shouldn't be running */
    private final boolean _handleRealtime;
    private final boolean _handleRecorded;

    private final AtomicBoolean _shuttingDown = new AtomicBoolean(false);

    private final BlockingQueue<INotifyPair> _queue = new LinkedBlockingQueue<>();

    /** Logically-started flag */
    private final AtomicBoolean _started = new AtomicBoolean(false);

    /** Thread-started flag */
    private final AtomicBoolean _threadStarted = new AtomicBoolean(false);

    /** Thread that handles notifications on queue */
    private final Thread                     _thread;
    
    private final String host;
    private final DictionaryProperties dictConfig;
    private final IChannelDefinitionProvider chanTable;
	private final NotificationProperties notifyProps;
    private final SseContextFlag             sseFlag;


    /**
     * Constructor.
     * 
     * @param context
     *            the current application context
     * 
     */
    public AlarmNotifierService(final ApplicationContext context)
    {
        notificationList = new ArrayList<>();
        channelIds = new HashMap<>(MAP_SIZE);
        modules = new HashMap<>(MAP_SIZE);
        operationalCategories = new HashMap<>(MAP_SIZE);
        subsystems = new HashMap<>(MAP_SIZE);
        chanTable = context.getBean(IChannelDefinitionProvider.class);
        sseFlag = context.getBean(SseContextFlag.class);
        LOG = TraceManager.getTracer(context, Loggers.NOTIFIER);

		// Initializing valid configuration
		// flag, message context, and last channel value store.
		notifierOkToStart = false;
		messageContext = context.getBean(IMessagePublicationBus.class);
		lastChannelValueCache = new LastChannelValueCache();
		
		host = context.getBean(IContextIdentification.class).getHost();
		dictConfig = context.getBean(DictionaryProperties.class);

		// Moved property initaization before usage
        notifyProps = context.getBean(NotificationProperties.class);

        notifierOkToStart = parseConfiguration();

        _handleRealtime = notifyProps.isRealtimeAlarmNotificationEnabled();
        _handleRecorded = notifyProps.isRecordedAlarmNotificationEnabled();

        _thread = new Thread(new NotifierRunner(_queue, _shuttingDown, LOG, THREAD_NAME), THREAD_NAME);
        _thread.setDaemon(true);

        _thread.setPriority(Math.max(Thread.NORM_PRIORITY - 1,
                                     Thread.MIN_PRIORITY));
    }

    @Override
	public boolean startService()
    {
        if (_started.getAndSet(true))
        {
            // Already called
            return true;
        }

        if ((_handleRealtime || _handleRecorded) && notifierOkToStart
				&& !notificationList.isEmpty())
        {
            _threadStarted.set(true);

            _thread.start();

			// Start the cache off clean (in case of restart).
			lastChannelValueCache.clear();

			// Initializing internal EHA channel message subscriber.
			subscriber = new EhaChannelMessageSubscriber();
            
            LOG.info(Markers.NOTIFY, NAME + " has started");
        }
        else
        {
            LOG.info(Markers.NOTIFY, NAME +
                     " will not be started because not configured " +
                     "or nothing to do");
        }

        return true;
    }

    @Override
	public void stopService()
    {
        if (! _started.get())
        {
            // Never started
            return;
        }

        if (_shuttingDown.getAndSet(true))
        {
            // Already stopped
            return;
        }

        if (subscriber != null)
        {
        	messageContext.unsubscribeAll(subscriber);
        }

        if (_threadStarted.get())
        {
            final long delay = notifyProps.getAlarmIdledownDelay();

            LOG.info(Markers.NOTIFY, NAME +
                     " is draining queue in order to shut down in " +
                     delay                                          +
                     " ms");

            final long timeout = System.currentTimeMillis() + delay;

            while (System.currentTimeMillis() < timeout)
            {
                SleepUtilities.checkedJoin(_thread, 5L * 1000L);

                if (! _thread.isAlive())
                {
                    break;
                }
            }

            if (_thread.isAlive())
            {
                LOG.error(Markers.NOTIFY, NAME +
                          " timed out draining queue, " +
                          "exiting anyway");
            }
            else
            {
                LOG.info(Markers.NOTIFY, NAME + " has drained queue and is shutting down");
            }
            LOG.info(NAME, " has shut down");
        }
        else
        {
            LOG.debug(NAME, " was never started ", "Shutdown complete");
        }
    }


    private boolean parseConfiguration()
    {
        final AlarmNotificationConfigParser parser =
            new AlarmNotificationConfigParser(notifyProps);
        
        try {
            parser.parseConfiguration(dictConfig, host, sseFlag);
        } catch (final IOException e) {
            LOG.debug(NAME + " could not find/load a notification file; notification will not be performed.");
            return false;
        } catch (final ParserConfigurationException e) {
            LOG.error(Markers.NOTIFY,
                    NAME + " could not configure the parser correctly; notification will not be performed.");
            return false;
        } catch (final SAXException e) {
            LOG.error(Markers.NOTIFY, NAME + " encountered parsing errors; notification will not be performed");
            return false;
        }

		// No exception thrown at this
		// point? Then we must have valid notifications to process
		notifierOkToStart = true;

        notificationList = parser.getNotificationList();

        for (final AlarmNotification notification : notificationList)
        {
            for (final AbstractAlarmTrigger trigger: notification.getTriggers())
            {
                if (trigger instanceof ChannelListTrigger)
                {
                    for (final String channelId :
                         ((ChannelListTrigger) trigger).getChannelIds())
                    {
                        if (channelIds.get(channelId) == null)
                        {
                            channelIds.put(channelId,channelId);
                        }
                    }
                }
                else if (trigger instanceof ModuleTrigger)
                {
                    for (final String module :
                         ((ModuleTrigger) trigger).getModules())
                    {
                        if (modules.get(module) == null)
                        {
                            modules.put(module,module);
                        }
                    }
                }
                else if (trigger instanceof OpsCategoryTrigger)
                {
                    for (final String category :
                         ((OpsCategoryTrigger) trigger).getCategories())
                    {
                        if(operationalCategories.get(category) == null)
                        {
                            operationalCategories.put(category,category);
                        }
                    }
                }
                else if (trigger instanceof SubsystemTrigger)
                {
                    for (final String subsystem :
                         ((SubsystemTrigger) trigger).getSubsystems())
                    {
                        if (subsystems.get(subsystem) == null)
                        {
                            subsystems.put(subsystem,subsystem);
                        }
                    }
                }
            }
        }
        
        return notifierOkToStart;
    }


    private boolean hasNotification(final IServiceChannelValue newValue)
    {
        return (channelIds.containsKey(newValue.getChanId())          ||
                modules.containsKey(
                    newValue.getCategory(IChannelDefinition.MODULE))   ||
                operationalCategories.containsKey(
                    newValue.getCategory(IChannelDefinition.OPS_CAT)) ||
                subsystems.containsKey(
                    newValue.getCategory(IChannelDefinition.SUBSYSTEM)));
    }


    /**
     * Send alarm notifications on a channel.
     *
     * @param message EHA message
     */
    public void sendNotifications(final IAlarmedChannelValueMessage message)
    {
        if (! _threadStarted.get() || notificationList.isEmpty())
        {
            return;
        }

        final IServiceChannelValue newValue = (IServiceChannelValue) message.getChannelValue();

        if (! hasNotification(newValue))
        {
            return;
        }

        final boolean realtime = newValue.isRealtime();

        if (realtime)
        {
            if (! _handleRealtime)
            {
                return;
            }
        }
        else if (! _handleRecorded)
        {
            return;
        }

		/*
		 * Removed dependency on the
		 * ChannelLad, due to race conditions. Use a local "LAD" instead, which
		 * is the LastChannelValueCache.
		 */
		final IServiceChannelValue oldValue = lastChannelValueCache
				.getLastValue(newValue.getChanId(), message.isRealtime(),
						newValue.getDssId());
		// Now save the latest value.
		lastChannelValueCache.saveValue(newValue.getChanId(),
				message.isRealtime(), newValue.getDssId(), newValue);

        for (final AlarmNotification notification : notificationList)
        {
            if (realtime)
            {
                if (! notification.isRealtime())
                {
                    continue;
                }
            }
            else if (! notification.isRecorded())
            {
                continue;
            }

            for (final AbstractAlarmTrigger trigger : notification.getTriggers())
            {
                if (trigger.isTriggered(newValue, oldValue))
                {
                    _queue.add(new NotifyPair(trigger, message));
                    break;
                }
            }
        }
    }


    /** Holder class for notifier and message */
    private static class NotifyPair
        extends Pair<AbstractAlarmTrigger, IAlarmedChannelValueMessage>
        implements INotifyPair
    {
        private static final long serialVersionUID = 0L;


        /**
         * Constructor.
         *
         * @param notifier AbstractAlarmTrigger
         * @param message  EHA message
         */
        public NotifyPair(final AbstractAlarmTrigger notifier,
                          final IAlarmedChannelValueMessage    message)
        {
            super(notifier, message);
        }


        /**
         * Perform notification.
         */
        @Override
        public void performNotification()
        {
            getOne().doNotify(getTwo());
        }
    }
    
    /**
	 * This class is a listener for internal EHA messages.
	 * 
	 */
    private class EhaChannelMessageSubscriber extends BaseMessageHandler {

		/**
		 * Creates an instance of EhaChannelMessageSubscriber.
		 */
		public EhaChannelMessageSubscriber() {
			messageContext.subscribe(EhaMessageType.AlarmedEhaChannel, this);
		}

        @Override
        public void handleMessage(final IMessage m) {
            sendNotifications((IAlarmedChannelValueMessage) m);
        }
    }
    
    /**
	 * 10/22/2013 sendNotifications(AlarmedChannelValueMessage)
	 * method used to rely on the LAD to obtain the previous channel value. With
	 * the splitting away of the alarm notification from the LadUpdateHandler,
	 * the ordering of the sendNotification and input into the LAD no longer
	 * became deterministic. So here we establish local cache of the channel
	 * values, so that AlarmNotifierService's dependency on the LAD is removed.
	 * 
	 * @since AMPCS 6.3.0
	 */
	private class LastChannelValueCache {
		final Map<String, Map<Integer, IServiceChannelValue>> realtimeCache = new HashMap<>();
		final Map<String, Map<Integer, IServiceChannelValue>> recordedCache = new HashMap<>();

		/**
		 * Save the value into the cache.
		 * 
		 * If the channel value is for a monitor channel, then the value will be
		 * cached using its actual DSS number. If it is not a monitor channel
		 * value, then it will be cached using DSS 0.
		 * 
		 * @param channelId
		 *            Channel ID of the channel value
		 * @param isRealtime
		 *            Flag that indicates whether or not it is a realtime
		 *            channel value
		 * @param dssId
		 *            DSS number
		 * @param newValue
		 *            The value to save
		 */
		void saveValue(final String channelId,
				final boolean isRealtime, final int dssId, final IServiceChannelValue newValue) {
			
			final Map<String, Map<Integer, IServiceChannelValue>> map = isRealtime ? realtimeCache
					: recordedCache;
			
			if (!map.containsKey(channelId)) {
				map.put(channelId, new HashMap<Integer, IServiceChannelValue>(1));
			}

			final Map<Integer, IServiceChannelValue> dssIdToValueMap = map.get(channelId);

			// If the channel value is not monitor channel, we do not care about
			// the DSS it is from, so use 0.
			dssIdToValueMap
					.put(newValue.getDefinitionType() == ChannelDefinitionType.M ? dssId
							: 0, newValue);
		}

		/**
		 * Clears the cache.
		 */
		void clear() {
			realtimeCache.clear();
			recordedCache.clear();
		}

		/**
		 * Retrieves the cached, last channel value. If retrieving a non-monitor
		 * channel value, then the dssId will be ignored (non-monitor channel
		 * values are always cached as DSS of 0).
		 * 
		 * @param channelId
		 *            Channel ID of the value to retrieve
		 * @param isRealtime
		 *            Flag that indicates whether the value for realtime or
		 *            recorded should be retrieved.
		 * @param dssId
		 *            DSS number of the channel value (ignored if retrieving a
		 *            non-monitor channel value)
		 * @return The saved channel value for the provided parameters, or null
		 *         if no such value has been saved.
		 */
		IServiceChannelValue getLastValue(final String channelId,
				final boolean isRealtime, final int dssId) {

			final Map<String, Map<Integer, IServiceChannelValue>> map = isRealtime ? realtimeCache
					: recordedCache;

			if (!map.containsKey(channelId)) {
				return null;
			}

			final Map<Integer, IServiceChannelValue> dssIdToValueMap = map.get(channelId);

			// An if-condition was checking for existence of entry keyed by DSS
			// ID. But this will not apply if value is for a non-monitor
			// channel. Removed the condition, since the last return statement
			// handles the case anyway.

			return dssIdToValueMap
					.get(chanTable
							.getDefinitionFromChannelId(channelId)
							.getDefinitionType() == ChannelDefinitionType.M ? dssId
							: 0);
		}

	}

	/* Add public method for clearing alarm notification history */
	@Override
    public void clearCache() {
		lastChannelValueCache.clear();
	}
}
