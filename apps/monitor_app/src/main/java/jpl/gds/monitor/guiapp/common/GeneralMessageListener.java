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

import jpl.gds.shared.message.IMessage;


/**
 * GeneralMessageListener is an interface to be implemented by classes that need to receive
 * messages from GeneralMessageDistributor. All non-channel messages are distributed to these
 * listeners.
 *
 */
public interface GeneralMessageListener {

    /**
     * Called to notify listeners when the monitor has received a list of 
     * messages. 
     * 
     * @param m the array of internal messages received
     */
    public void messageReceived(IMessage[] m);
}
