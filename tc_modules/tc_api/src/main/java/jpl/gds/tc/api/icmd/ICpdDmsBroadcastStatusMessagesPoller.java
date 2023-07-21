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
package jpl.gds.tc.api.icmd;

public interface ICpdDmsBroadcastStatusMessagesPoller extends Runnable {

    /**
     * Register newSubscriber for callbacks when new DMS broadcast status
     * messages arrive.
     *
     * @param newSubscriber
     *            subscriber to be registered for callbacks
     */
    void subscribe(ICpdDmsBroadcastStatusMessagesSubscriber newSubscriber);

    /**
     * Unregister subscriberToRemove from callbacks.
     *
     * @param subscriberToRemove
     *            subscriber to be unregister from callbacks
     */
    void unsubscribe(ICpdDmsBroadcastStatusMessagesSubscriber subscriberToRemove);

    void stop();

    /**
     * Start the poller thread. Run this when all the subscribers have finished
     * subscribing.
     */
    void start();

}