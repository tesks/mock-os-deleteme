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
package jpl.gds.jms.activemq;

import org.springframework.context.ApplicationContext;

import jpl.gds.jms.AbstractAsynchronousTopicPublisher;
import jpl.gds.jms.JmsTopicConfiguration;
import jpl.gds.message.api.external.MessageServiceException;

/**
 * ActiveMQ implementation of an asynchronous topic publisher.
 */
public class ActivemqAsynchronousTopicPublisher extends
        AbstractAsynchronousTopicPublisher {
    /**
     * Constructor.
     * 
     * @param appContext the current application context
     * @param config Jms topic configuration used for topic name, and JMS
     *            persistence.
     * @param transactionSize Transaction size of the Publisher.
     * @param spillDir Test session configuration used to create the spill processor.
     * @throws MessageServiceException if there was an error creating the publisher
     */
    public ActivemqAsynchronousTopicPublisher(
    		ApplicationContext appContext,
            final JmsTopicConfiguration config, final int transactionSize,
            final String spillDir) throws MessageServiceException {
        super(appContext, config, transactionSize, spillDir);
    }
}
