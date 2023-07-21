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
 * ChannelFlushListener is an interface to be implemented by monitor classes
 * that want a periodic channel display flush notification (such as channel lists and
 * alarm views). 
 * 
 * The notification thread runs in the MonitorTimers object and the
 * notification interval corresponds to the value of the channel list update 
 * interval global monitor parameter.
 *
 */
public interface ChannelFlushListener {
    /**
     * Notifies a ChannelFlushListener that the flush timer has fired.
     * This notification will occur on the user interface thread.
     */
    public void flushTimerFired();
}
