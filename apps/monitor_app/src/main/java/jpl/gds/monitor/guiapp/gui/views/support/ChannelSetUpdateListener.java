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
package jpl.gds.monitor.guiapp.gui.views.support;

import jpl.gds.monitor.perspective.view.channel.ChannelSet;

/**
 * This interface must be implemented by classes that need to be notified of 
 * ChannelSet changes from the channel selector.
 *
 */
public interface ChannelSetUpdateListener
{
	/**
	 * Notifies the listener that a new channel set has been selected
	 * by the user.
	 * 
	 * @param set the new ChannelSet object 
	 * @return true if the ChannelSet is accepted by the receiver, false if not
	 */
    public boolean updateSet(ChannelSet set);
}
