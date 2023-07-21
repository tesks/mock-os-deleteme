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
 * An interface to be implemented by message service client heartbeat publishers,
 * which periodically publishes a message for each client, simply in order to confirm
 * there is still a message service connection.
 */
public interface IClientHeartbeatPublisher {

    /**
     * Adds an IClientHeartbeatListener to this publisher.
     * 
     * @param listener the listener to add
     */
    public void addListener(IClientHeartbeatListener listener);

    /**
     * Removes an IClientHeartbeatListener from this publisher.
     * 
     * @param listener the listener to add
     */
    public void removeListener(IClientHeartbeatListener listener);

    /**
     * Starts the actual publication of client heart beats.
     * 
     * @return true if the heartbeat was actually started,
     * false if not
     */
    public boolean startPublishing();

    /**
     * Stops the publication of client heart beats.
     */
    public void stopPublishing();
    
    /**
     * Gets the ID string for the client that started this publisher.
     * 
     * @return client ID string
     */
    public String getClientId();

}