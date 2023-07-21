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
package jpl.gds.message.api.external;



/**
 * This interface allows IClientHeartbeatListeners to register for notification
 * when the heartbeat publisher connection to the external message service is lost or regained by
 * the IClientHeartbeatPublisher. This notification, in turn, can be used by an
 * application that subscribed to the message service to know that the message service connection is dead.
 * Otherwise, subscribers will never know. The heartbeat interval is
 * configurable.
 */
public interface IClientHeartbeatListener {
	
    /**
     * Listener method that is called when publication of
     * a heartbeat fails.
     */
    public void publicationFailed();
    
    /**
     * Listener method that is called when publication of
     * heartbeats is restored following a previous failure.
     */
    public void publicationRegained();
}
