/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.message.api;

import jpl.gds.message.api.external.IExternalMessage;

/**
 *
 * An interface to be implemented by topic publishers that keep a queue
 * so messages can be published asynchronously.
 */
public interface IInternalBusPublisher extends Runnable{

    /**
     * Starts the publishing thread
     */
    void start();

    /**
     * Run method that does the publishing
     */
    void run();

    /**
     * Closes publishing thread
     */
    void close();

    /**
     * Queue message for publication
     * @param extMessage External message
     */
    void queueMessageForPublication(final IExternalMessage extMessage);


    /**
     * Check to see if there are more items to process in backlog
     * @return True if more items, false otherwise
     */
    boolean hasBacklog();
}
