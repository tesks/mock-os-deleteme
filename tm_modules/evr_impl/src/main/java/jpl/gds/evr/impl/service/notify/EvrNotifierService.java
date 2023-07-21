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
package jpl.gds.evr.impl.service.notify;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import jpl.gds.common.notify.INotifier;
import jpl.gds.common.notify.NotificationProperties;
import jpl.gds.common.notify.NotifierRunner;
import jpl.gds.common.notify.NotifierRunner.INotifyPair;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.evr.api.service.IEvrNotificationTrigger;
import jpl.gds.evr.api.service.IEvrNotifierService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.spring.bootstrap.SharedSpringBootstrap;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.shared.types.Pair;


/**
 * EvrNotifier is a downlink service that looks for <code>EvrMessage</code>
 * objects being published through the <code>SessionBasedMessageContext</code>
 * and determines if notifications should be sent out for those EVRs. The
 * notifications are defined in a notification configuration XML file that this
 * service will attempt to read when it's instantiated. If the service is
 * enabled and a set of notifications are in fact defined, then the service will
 * look for EVR messages, see if it triggers any of the notifications, and for
 * those notifications that are triggered send out notifications to the defined
 * destinations.
 *
 * This service can be enabled/disabled through the GDS configuration file. See
 * the <code>NotificationConfiguration</code> class.
 *
 * Since notifications are slow, they are performed in a thread. That means
 * that the thread's queue must be drained on shutdown.
 *
 * New feature in AMPCS R2.
 *
 * @see NotificationProperties
 * @see IEvrMessage
 *
 */
public class EvrNotifierService implements IEvrNotifierService
{
    private static final String NAME        = "EvrNotifier";
    private static final String THREAD_NAME = "EvrNotifierRunner";


    private boolean notifierOkToStart;
    private List<EvrNotificationDefinition> notificationList;
    private Map<IEvrNotificationTrigger, EvrNotificationDefinition>
        triggersMap;

    private final IMessagePublicationBus messageContext;

    private MessageSubscriber subscriber;

    private final Tracer                                            LOG;


    /** Presumably at least one of these is set, or we shouldn't be running */
    private final boolean _handleRealtime;
    private final boolean _handleRecorded;

    private final AtomicBoolean _shuttingDown = new AtomicBoolean(false);

    private final BlockingQueue<INotifyPair> _queue =
        new LinkedBlockingQueue<INotifyPair>();

    /** Logically-started flag */
    private final AtomicBoolean _started = new AtomicBoolean(false);

    /** Thread-started flag */
    private final AtomicBoolean _threadStarted = new AtomicBoolean(false);

    /** Thread that handles notifications on queue */
    private final Thread                                            _thread;
    
    private final String host;
    private final DictionaryProperties dictConfig;
	private final NotificationProperties notifyProps;
    private final SseContextFlag                                    sseFlag;


    /**
     * Default constructor. This will start the parsing of notification file.
     * @param context the current application context
     */
    public EvrNotifierService(final ApplicationContext context) 
    {
        super();
        LOG = TraceManager.getTracer(context, Loggers.NOTIFIER);
        _thread = new Thread(new NotifierRunner(
                _queue,
                _shuttingDown,
                LOG,
                THREAD_NAME),
        THREAD_NAME);
        _thread.setDaemon(true);

        _thread.setPriority(Math.max(Thread.NORM_PRIORITY - 1,
                                     Thread.MIN_PRIORITY));

        notifierOkToStart = false;
        messageContext = context.getBean(SharedSpringBootstrap.PUBLICATION_BUS, IMessagePublicationBus.class);
        host = context.getBean(IContextIdentification.class).getHost();
        dictConfig = context.getBean(DictionaryProperties.class);
        this.sseFlag = context.getBean(SseContextFlag.class);
        notifyProps = context.getBean(NotificationProperties.class);
    
        notifierOkToStart = parseConfiguration();

        _handleRealtime = notifyProps.isRealtimeEvrNotificationEnabled();
        _handleRecorded = notifyProps.isRecordedEvrNotificationEnabled();
    }


    private boolean parseConfiguration() {
        final EvrNotificationFileParser parser = new EvrNotificationFileParser(this.notifyProps);

        try {
            parser.parseConfiguration(dictConfig, host, sseFlag);
        } catch (final IOException e) {
            LOG.debug(Markers.NOTIFY,
                    NAME + " could not find/load a notification file; notification will not be performed");
            return false;
        } catch (final ParserConfigurationException e) {
            LOG.error(Markers.NOTIFY,
                    NAME + " could not configure the parser correctly; notification will not be performed");
            return false;
        } catch (final SAXException e) {
            LOG.error(Markers.NOTIFY, NAME + " encountered parsing errors; notification will not be performed");
            return false;
        }

        LOG.debug(Markers.NOTIFY, NAME + " finished parsing notification file.");
        notifierOkToStart = true; // Because we actually do have notifications
                                    // to process
        notificationList = parser.getNotificationList();
        triggersMap = new HashMap<IEvrNotificationTrigger, EvrNotificationDefinition>(
                notificationList.size());

        /*
         * Put the triggers in hashmaps for quick access.
         */
        for (final EvrNotificationDefinition notification : notificationList) {
            final List<IEvrNotificationTrigger> triggers = notification.getTriggers();

            for (final IEvrNotificationTrigger trigger : triggers) {
                triggersMap.put(trigger, notification);
            }

        }
        
        return notifierOkToStart;

    }

    private Set<EvrNotificationDefinition> getTriggeredNotifications(
            final IEvr evr) {
        final Set<EvrNotificationDefinition> triggeredNotifications = new HashSet<EvrNotificationDefinition>(
                2);

        for (final Map.Entry<IEvrNotificationTrigger, EvrNotificationDefinition> entry : triggersMap
                .entrySet()) {

            if (entry.getKey().evrTriggersNotification(evr)) {
                triggeredNotifications.add(entry.getValue());
            }

        }

        return triggeredNotifications;
    }

    /**
     * For the provided EVR message, determine if it triggers any notifications
     * and if so, send out notifications to their destinations.
     *
     * @param message
     *            EVR message to check against triggers
     */
    @Override
    public void sendNotifications(final IEvrMessage message) {

        if (notificationList.isEmpty()) {
            return;
        }

        final IEvr evr = message.getEvr();

        final Set<EvrNotificationDefinition> triggeredNotifications = getTriggeredNotifications(evr);

        if (triggeredNotifications.isEmpty()) {
            return;
        }

        // Definitions can be for real-time or recorded or both

        final boolean realtime = evr.isRealtime();

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

        for (final EvrNotificationDefinition notification : triggeredNotifications)
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

            for (final INotifier destination :
                     notification.getNotificationDestinations())
            {
                _queue.add(new NotifyPair(destination, message));
            }
        }
    }


    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.shared.interfaces.IService#startService()
     */
    @Override
    public boolean startService()
    {
        if (_started.getAndSet(true))
        {
            // Called already
            return true;
        }

        if (notifierOkToStart && (_handleRealtime || _handleRecorded))
        {
            _threadStarted.set(true);

            _thread.start();

            subscriber = new EvrMessageSubscriber();

            LOG.info(Markers.NOTIFY, NAME, " has started");
        }
        else
        {
            LOG.info(Markers.NOTIFY, NAME, " will not run because no notifications have been ",
                    "configured or not wanted");
        }

        /*
         * Whether or not the notifier is actually running (if there are no
         * notification files then this notifier isn't "running") return true
         * to let the caller know that we're okay and it can go about its
         * business.
         */
        return true;
    }


    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.shared.interfaces.IService#stopService()
     */
    @Override
    public void stopService()
    {
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

            final long delay =
                notifyProps.getEvrIdledownDelay();

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
            LOG.info(NAME + " has shut down");
        }
        else
        {
            LOG.debug(NAME, " was never started ", "Shutdown complete");
        }

    }


    /**
     * This class is a listener for internal EVR messages.
     *
     */
    private class EvrMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of EvrMessageSubscriber.
         */
        public EvrMessageSubscriber() {
            messageContext.subscribe(EvrMessageType.Evr, this);
        }

        /**
         * {@inheritDoc}
         * @see jpl.gds.shared.message.BaseMessageHandler#handleMessage(jpl.gds.shared.message.IMessage)
         */
        @Override
        public void handleMessage(final IMessage m) {
            sendNotifications((IEvrMessage) m);
        }
    }


    /** Holder class for notifier and message */
    private static class NotifyPair
        extends Pair<INotifier, IEvrMessage>
        implements INotifyPair
    {
        private static final long serialVersionUID = 0L;


        /**
         * Constructor.
         *
         * @param notifier AbstractAlarmNotifier
         * @param message  EVR message
         */
        public NotifyPair(final INotifier notifier,
                          final IEvrMessage            message)
        {
            super(notifier, message);
        }


        /**
         * Perform notification.
         */
        @Override
        public void performNotification()
        {
            getOne().notify(getTwo());
        }
    }

    /** package protected */ NotificationProperties getNotifyProps() {
        return notifyProps;
    }
}
