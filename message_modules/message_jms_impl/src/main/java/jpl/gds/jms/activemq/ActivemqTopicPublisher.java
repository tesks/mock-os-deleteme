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

import jpl.gds.jms.AbstractTopicPublisher;
import jpl.gds.jms.JmsTopicConfiguration;
import jpl.gds.message.api.external.MessageServiceException;

/**
 * ActiveMQ implementation of AbstractTopicPublisher.
 */
public class ActivemqTopicPublisher extends
        AbstractTopicPublisher {
    /**
     * ActiveMQ topic publisher constructor.
     * 
     * @param appContext the current application context
     * @param config Jms configuration used to configure topic, connection, and
     *            session.
     * @param transactionSize The size of transactions. 1 signifies
     *            non-transacted publishing.
     * @throws MessageServiceException if any error occurs
     */
    public ActivemqTopicPublisher(ApplicationContext appContext,
    		final JmsTopicConfiguration config,
            final int transactionSize) throws MessageServiceException {
        super(appContext, config, transactionSize != 1);
    }
}
