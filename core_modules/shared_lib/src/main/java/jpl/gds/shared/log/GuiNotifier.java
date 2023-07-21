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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * GuiNotifier is a central distribution point for Trace messages to GUI
 * Listeners. It uses a singleton design pattern. GUI components that need to
 * display Trace messages can register with this object.
 * 
 *
 */
public final class GuiNotifier implements GuiTraceListener {
	// Access must be synchronized
    private final List<GuiTraceListener>                   guiListeners     = new LinkedList<>();
	
	/**
     * Creates an instance of GuiNotifier.
     * 
     */
    public GuiNotifier() {
    }


    /**
     * Adds a GUI log message listener for the given Context Key
     * 
     * 
     * @param l
     *            GuiListener
     */
    public void addContextListener(final GuiTraceListener l) {
        synchronized (guiListeners) {
            if (!guiListeners.contains(l)) {
                guiListeners.add(l);
            }
        }
    }

    /**
     * Removes a GUI log message listener for the given Context Key
     * 
     * @param l
     *            Registered GUI Listener
     * 
     * 
     */
    public void removeContextListener(final GuiTraceListener l) {
        synchronized (guiListeners) {
            if (guiListeners.contains(l)) {
                guiListeners.remove(l);
            }
        }
    }

    /**
     * The receipt point for WARNING and INFO messages from the Trace Appender.
     * This method distributes the message to its registered listeners.
     * 
     * @param message
     *            the internal AMPCS log4j message
     */
    @Override
    public void notifyTraceListeners(final AmpcsLog4jMessage message) {
        synchronized (guiListeners) {
            if (!guiListeners.isEmpty()) {
                final Iterator<GuiTraceListener> it = guiListeners.iterator();

                while (it.hasNext()) {
                    it.next().notifyTraceListeners(message);
                }
            }
        }
    }

    /**
     * Returns the current listener map
     * 
     * @return Map<IContextKey, List<GuiTraceListener>>
     */
    public List<GuiTraceListener> getContextListeners() {
        return guiListeners;
    }


}
