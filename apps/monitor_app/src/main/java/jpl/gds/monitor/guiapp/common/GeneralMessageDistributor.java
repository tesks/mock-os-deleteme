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
package jpl.gds.monitor.guiapp.common;

import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.common.config.service.ServiceConfiguration.ServiceType;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.message.IContextMessage;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.monitor.config.MonitorDictionaryUtility;
import jpl.gds.session.message.SessionHeartbeatMessage;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.session.message.StartOfSessionMessage;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageConstants;
import jpl.gds.shared.metadata.context.IContextKey;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * GeneralMessageDistributor is the central message receipt and distribution point for all non-channel messages in the
 * monitor. It uses a singleton design pattern.
 * <p>
 * The main entry point to this class is the onMessage() method, which is invoked by the MonitorData class. Received
 * messages are checked for message type. If there are registered listeners for the message, the message is parsed and
 * distributed.
 * <p>
 * This class also handles automatic dictionary loading when the active session changes, if so configured, and also
 * handles update of monitor's global session configuration when session information changes.
 */
public final class GeneralMessageDistributor implements IMessageServiceListener {

    private final int    prefixLen = MessageConstants.EXTERNAL_MESSAGE_TYPE_PREFIX.length();
    private final Tracer tracer;


    private final Tracer jmsDebugLogger;


    private final Map<IMessageType, List<GeneralMessageListener>> subscribers          = new HashMap<>();
    private final Map<IMessageType, Long>                         messageCounts        = new HashMap<>();
    private final AtomicBoolean                                   isShutdown           = new AtomicBoolean(false);
    private final ApplicationContext                              appContext;
    private final IExternalMessageUtility                         externalMessageUtil;
    private       long                                            receiveCount         = 0;
    private       boolean                                         autoLoadDict         = true;
    private       String                                          oldFswDir;
    private       String                                          oldSseDir;
    private       String                                          oldFswVersion;
    private       String                                          oldSseVersion;
    private       boolean                                         awaitingFirstSession = true;
    private       boolean                                         fswDictLoaded;
    private       boolean                                         sseDictLoaded;
    private       boolean                                         fswDictWarningIssued;
    private       boolean                                         sseDictWarningIssued;

    /**
     * Creates an instance of GeneralMessageDistributor.
     *
     * @param appContext the current application context
     */
    public GeneralMessageDistributor(final ApplicationContext appContext) {
        this.appContext = appContext;
        tracer = TraceManager.getDefaultTracer(appContext);
        jmsDebugLogger = TraceManager.getTracer(appContext, Loggers.JMS);
        this.externalMessageUtil = appContext.getBean(IExternalMessageUtility.class);

        final DictionaryProperties dc = this.appContext.getBean(DictionaryProperties.class);
        if (dc != null) {
            oldFswDir = dc.getFswDictionaryDir();
            oldSseDir = dc.getSseDictionaryDir();
            oldFswVersion = dc.getFswVersion();
            oldSseVersion = dc.getSseVersion();
        }
    }

    /**
     * Gets the total count of messages received by this object.
     *
     * @return the receive count
     */
    public long getReceiveCount() {
        return receiveCount;
    }

    /**
     * Gets the flag indicating whether dictionaries should be automatically loaded if they change in the active
     * session.
     *
     * @return true to auto-load; false to not
     */
    public boolean isAutoLoadDictionary() {
        return autoLoadDict;
    }

    /**
     * Sets the flag indicating whether dictionaries should be automatically loaded if they change in the active
     * session.
     *
     * @param enable true to auto-load; false to not
     */
    public void setAutoLoadDictionary(final boolean enable) {
        autoLoadDict = enable;
        GdsSystemProperties.setDictionaryIsOverridden(!enable);
    }

    /**
     * Gets the list of subscribed listeners. For test purposes only.
     *
     * @return subscribers List of subscribed listeners
     */
    public Map<IMessageType, List<GeneralMessageListener>> getSubscribers() {
        return subscribers;
    }

    /**
     * Adds a GeneralMessageListener for the given message type.
     *
     * @param l           the GeneralMessageListener to add
     * @param messageType the internal message type the listener is interested in
     */
    public void addDataListener(final GeneralMessageListener l, final IMessageType messageType) {
        synchronized (subscribers) {
            List<GeneralMessageListener> listeners = subscribers.get(messageType);
            if (listeners == null) {
                listeners = new ArrayList<>();
                subscribers.put(messageType, listeners);
            }
            if (!listeners.contains(l)) {
                listeners.add(l);
            }
        }
    }

    /**
     * Removes the GeneralMessageListener for all message types.
     *
     * @param l the GeneralMessageListener to remove
     */
    public void removeDataListener(final GeneralMessageListener l) {
        synchronized (subscribers) {
            final Set<IMessageType>      types = subscribers.keySet();
            final Iterator<IMessageType> keys  = types.iterator();
            while (keys.hasNext()) {
                final List<GeneralMessageListener> listeners = subscribers.get(keys.next());
                listeners.remove(l);
            }
        }
    }

    /**
     * Notifies listeners that new messages have arrived.
     *
     * @param m           the list of messages to send
     * @param messageType the type of the messages
     */
    @java.lang.SuppressWarnings("unchecked")
    private void notifyListeners(final jpl.gds.shared.message.IMessage[] m, final IMessageType messageType) {
        List<GeneralMessageListener> list = null;
        synchronized (subscribers) {
            list = subscribers.get(messageType);

            if (list == null || list.isEmpty()) {
                return;
            }
            list = (ArrayList<GeneralMessageListener>) ((ArrayList<GeneralMessageListener>) list).clone();
        }
        final Iterator<GeneralMessageListener> e = list.iterator();
        while (e.hasNext()) {
            final GeneralMessageListener l = e.next();
            l.messageReceived(m);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"REC_CATCH_EXCEPTION"})
    public synchronized void onMessage(final IExternalMessage m) {
        try {
            if (isShutdown.get()) {
                return;
            }

            // Keep track of received message count
            receiveCount++;

            final IMessageType type = externalMessageUtil.getInternalType(m);

            // This prints out debug stuff
            final Long count = messageCounts.get(type);
            if (count == null) {
                messageCounts.put(type, Long.valueOf(1));
            } else {
                messageCounts.put(type, Long.valueOf(count + 1));
            }

            // Get the subscribers for the message type
            final List<GeneralMessageListener> list = subscribers.get(type);

            //If there are no subscribers for this message and it's NOT a StartOfSession or Heartbeat message
            if ((list == null || list.isEmpty()) && !IMessageType.matches(type, SessionMessageType.StartOfSession) &&
                    !(IMessageType.matches(type, SessionMessageType.SessionHeartbeat))) {
                return;
            }

            /*
             * Do not extract message text here. Messages can be binary.
             * We cannot log the content here, and the getText() on the message object is time
             * consuming to do.
             */

            try {

                // Parse the external message into internal message object
                final jpl.gds.shared.message.IMessage[] newMessageList = externalMessageUtil.instantiateMessages(m);
                final jpl.gds.shared.message.IMessage   msg            = newMessageList[0];

                // If we have a heart beat or start of session message, the session info and dictionaries
                // may have changed.
                if (IMessageType.matches(type, SessionMessageType.StartOfSession)
                        || (IMessageType.matches(type, SessionMessageType.SessionHeartbeat))) {
                    processSessionMessage((IContextMessage) msg);
                }
                // Otherwise, just update the session number, host (also applies to ContextHeartbeat)
                else {
                    final IContextKey key = msg.getContextKey();

                    if (key.getNumber() != null) {
                        if (!msg.isFromSse()) {
                            appContext.getBean(IContextIdentification.class).setNumber(key.getNumber());
                            appContext.getBean(IContextIdentification.class).setHost(key.getHost());
                        }
                    }
                }

                // Notify listeners for this message type with the new list of messages
                notifyListeners(newMessageList, type);

            } catch (final Exception e) {
                tracer.error("Error parsing message: " + e.toString(), e);
                tracer.warn(externalMessageUtil.getContentDump(m));
            }

        } catch (final MessageServiceException e) {
            jmsDebugLogger.error("Error receiving message: " + e.toString(), e);
        }
    }

    /**
     * Replaces subscriptions for the given GeneralMessageListener by unsubscribing old message types and subscribing to
     * a new list of message types.
     *
     * @param l             the message listener
     * @param iMessageTypes the list of types to subscribe to; if null, nothing is subscribed to
     */
    public void replaceDataListeners(final GeneralMessageListener l, final IMessageType[] iMessageTypes) {
        removeDataListener(l);
        if (iMessageTypes == null) {
            return;
        }
        synchronized (subscribers) {
            for (int i = 0; i < iMessageTypes.length; i++) {
                if (iMessageTypes[i] == null) {
                    continue;
                }
                addDataListener(l, iMessageTypes[i]);
            }
        }
    }

    /**
     * Shuts down the distributor.
     */
    public void shutdown() {
        if (isShutdown.getAndSet(true)) {
            return;
        }
        long total = 0;

        final Set<IMessageType>      keySet = messageCounts.keySet();
        final Iterator<IMessageType> it     = keySet.iterator();
        while (it.hasNext()) {
            final IMessageType key = it.next();
            final Long         val = messageCounts.get(key);
            jmsDebugLogger.debug(
                    "GeneralMessageDistributor has a receive count of " + val + " messages for type " + key + " at time of shutdown");
            total += val;
        }
        jmsDebugLogger.debug("GeneralMessageDistributor total receipt count is " + total);

    }

    private void copySessionToContext(final IContextConfiguration newConfig) {
        appContext.getBean(IContextConfiguration.class).copyValuesFrom(newConfig);
    }

    private void processSessionMessage(final IContextMessage msg) {

        final IContextConfiguration msgTestConfig = (IContextConfiguration) msg.getContextConfiguration();

        // Gets the dictionaries from the new message
        final String newFswDir     = msgTestConfig.getDictionaryConfig().getFswDictionaryDir();
        final String newSseDir     = msgTestConfig.getDictionaryConfig().getSseDictionaryDir();
        final String newFswVersion = msgTestConfig.getDictionaryConfig().getFswVersion();
        final String newSseVersion = msgTestConfig.getDictionaryConfig().getSseVersion();

        final boolean fswVersionChanged = !msg.isFromSse() &&
                (oldFswDir != null && !oldFswDir.equals(newFswDir)) ||
                (oldFswVersion != null && !oldFswVersion.equals(newFswVersion));

        final boolean sseVersionChanged = (oldSseDir != null && !oldSseDir.equals(newSseDir)) ||
                (oldSseVersion != null && !oldSseVersion.equals(newSseVersion));

        final IContextKey currentKey    = appContext.getBean(IContextKey.class);
        final long        currentKeyNum = currentKey.getNumber() == null ? 0 : currentKey.getNumber().longValue();
        final IContextKey msgKey        = msg.getContextKey();
        final long        msgKeyNum     = msgKey.getNumber() == null ? 0 : msgKey.getNumber().longValue();

        // If this is the first session message we have seen or the message
        // is not from SSE and the context number has changed, copy
        // the configuration to the monitor's global application context and
        if (this.awaitingFirstSession || (!msg.isFromSse() && currentKeyNum != msgKeyNum)) {
            this.copySessionToContext(msgTestConfig);
        }

        // Reload new dictionaries if necessary
        if (autoLoadDict && fswVersionChanged && !fswDictLoaded) {

            tracer.info("Session dictionary change detected and autoload is enabled: reloading fight dictionaries");

            try {
                final DictionaryProperties dc = appContext.getBean(DictionaryProperties.class);
                dc.setFswVersion(newFswVersion);
                dc.setFswDictionaryDir(newFswDir);
                appContext.getBean(MonitorDictionaryUtility.class).loadFswDictionaries();
                oldFswDir = dc.getFswDictionaryDir();
                oldFswVersion = dc.getFswVersion();

                fswDictLoaded = true;

            } catch (BeansException | DictionaryException e) {
                tracer.error("Problem loading dictionaries: " + ExceptionTools.getMessage(e), e);
            }
        } else if (autoLoadDict && fswVersionChanged && fswDictLoaded && !fswDictWarningIssued) {
            tracer.warn("This monitor is seeing messages from sessions using different flight dictionaries.");
            tracer.warn("The monitor cannot be used with more than one version of the dictionary.");
            tracer.warn("One dictionary set has been loaded; this monitor refuses to load another one.");
            fswDictWarningIssued = true;
        }

        if (autoLoadDict && sseVersionChanged && !sseDictLoaded) {

            tracer.info("Session dictionary change detected and autoload is enabled: reloading SSE dictionaries");

            try {
                final DictionaryProperties dc = appContext.getBean(DictionaryProperties.class);
                dc.setSseVersion(newSseVersion);
                dc.setSseDictionaryDir(newSseDir);
                appContext.getBean(MonitorDictionaryUtility.class).loadSseDictionaries();
                oldSseDir = dc.getSseDictionaryDir();
                oldSseVersion = dc.getSseVersion();

                sseDictLoaded = true;

            } catch (BeansException | DictionaryException e) {
                tracer.error("Problem loading dictionaries: " + ExceptionTools.getMessage(e), e);
            }
        } else if (autoLoadDict && sseVersionChanged && sseDictLoaded && !sseDictWarningIssued) {
            tracer.warn("This monitor is seeing messages from sessions using different SSE dictionaries.");
            tracer.warn("The monitor cannot be used with more than one version of the dictionary.");
            tracer.warn("One dictionary set has been loaded; this monitor refuses to load another one.");
            sseDictWarningIssued = true;
        }

        if (this.awaitingFirstSession) {

            // Grab the Global LAD from
            // StartOfSessionMessage or HearbeatMessage
            ServiceConfiguration serviceConfig = null;

            if (msg.isType(SessionMessageType.StartOfSession)) {
                serviceConfig = ((StartOfSessionMessage) msg).getServiceConfiguration();
            } else if (msg.isType(SessionMessageType.SessionHeartbeat)) {
                serviceConfig = ((SessionHeartbeatMessage) msg).getServiceConfiguration();
            }

            if (serviceConfig != null) {
                final GlobalLadProperties globalLadConfig = GlobalLadProperties.getGlobalInstance();
                if (serviceConfig.getService(ServiceType.GLAD) != null) {
                    globalLadConfig.setGlobalLadRestServerPort(serviceConfig.getService(ServiceType.GLAD).getPort());
                    globalLadConfig.setGlobalLadHost(serviceConfig.getService(ServiceType.GLAD).getHost());
                }
            }
        }

        this.awaitingFirstSession = false;
    }
}
