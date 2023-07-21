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

/**
 * 
 * GeneralFlushListener is an interface to be implemented by monitor classes
 * that want a periodic display flush notification. This is used for general
 * displays that do not have a specific timer (unlike channel lists and plots).
 *
 */
public interface GeneralFlushListener {
    /**
     * Notifies a Flush Listener that the flush timer has fired.
     */
    public void flushTimerFired();
}
