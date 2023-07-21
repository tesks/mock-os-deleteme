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
package jpl.gds.monitor.guiapp.channel;

import jpl.gds.monitor.perspective.view.channel.MonitorChannelSample;


/**
 * This interface must be implemented by classes that want to receive channel 
 * data samples from the ChannelMessageDistributor.
 *
 */
public interface ChannelSampleListener
{
    /**
     * Notifies the listener that a new channel value has been received.
     * @param data the received ChannelSample
     */
    public void receive(MonitorChannelSample data);
}
