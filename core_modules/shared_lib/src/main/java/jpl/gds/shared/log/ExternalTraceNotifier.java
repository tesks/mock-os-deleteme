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
package jpl.gds.shared.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jpl.gds.shared.metadata.context.IContextKey;

/**
 * ExternalTraceNotifier is a central distribution point for Trace messages to
 * TraceListeners. It uses a singleton design pattern. It is used to forward
 * messages that should go to the outside world (database, message service,
 * syslog, etc) to listeners that can do that forwarding. Classes that want
 * external trace messages must register with this class.
 * 
 */
public final class ExternalTraceNotifier implements TraceListener {
    private final Map<IContextKey, List<TraceListener>> contextListeners = new HashMap<IContextKey, List<TraceListener>>();

	/**
     * Creates an instance of ExternalTraceNotifier.
     * 
     */
    public ExternalTraceNotifier() {
	}


    /**
     * Registers a TraceListener for the given context id
     * 
     * @param key
     *            THe ContextKey to register with the TraceListener
     * @param l
     *            The TraceListener to register for log messages
     */
    public void addContextListener(final IContextKey key, final TraceListener l) {
        synchronized (contextListeners) {
            List<TraceListener> currentListeners = contextListeners.get(key);
            if (currentListeners != null) {
                if (!currentListeners.contains(l)) {
                    currentListeners.add(l);
                }
            } else {
                currentListeners = new ArrayList<TraceListener>();
                currentListeners.add(l);
            }
            contextListeners.put(key, currentListeners);
        }
    }


    /**
     * Removes the TraceListeners for external log messages on a supplied
     * ContextKey
     * 
     * @param key
     *            The ApplicationContext's ContextKey to remove the listener
     *            from
     * @param l
     *            the TraceListener to remove
     */
    public void removeContextListener(final IContextKey key, final TraceListener l) {
        synchronized (contextListeners) {
            final List<TraceListener> listeners = contextListeners.get(key);
            if (listeners != null && listeners.contains(l)) {
                listeners.remove(l);
                contextListeners.put(key, listeners);
            }
        }
    }


    @Override
    public void notifyTraceListeners(final AmpcsLog4jMessage message) {
        if (message.getApplicationContext() != null) {
            IContextKey key;
            List<TraceListener> temp;
            try { 
                key = message.getApplicationContext().getBean(IContextKey.class);

                synchronized (contextListeners) {
                    temp = contextListeners.get(key);

                    if (temp != null && !temp.isEmpty()) {
                        final Iterator<TraceListener> it = temp.iterator();

                        while (it.hasNext()) {
                            it.next().notifyTraceListeners(message);
                        }
                    } // silently drop if no iterator
                }
            } catch (final Exception e) {

            }
        }
    }

    /**
     * Returns the current listener map
     * 
     * @return Map<IContextKey, List<GuiTraceListener>>
     */
    public Map<IContextKey, List<TraceListener>> getContextListeners() {
        return contextListeners;
    }

}
