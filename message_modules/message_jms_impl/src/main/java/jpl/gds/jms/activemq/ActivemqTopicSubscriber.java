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

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.springframework.context.ApplicationContext;

import jpl.gds.jms.AbstractTopicSubscriber;
import jpl.gds.jms.JmsTopicConfiguration;

/**
 * ActiveMQ implementation of a topic subscriber.
 */
public class ActivemqTopicSubscriber extends
        AbstractTopicSubscriber {
    /**
     * Construct an ActiveMQ topic subscriber.
     * 
     * @param appContext the current application context
     * @param config Jms Topic configuration used to configure the shared
     *            connection.
     * @param name Subscriber name.
     * @param filter Subscriber filter.
     * @param isTransacted Whether the subscriber is transacted.
     * @throws NamingException Thrown if a Naming Exception is encountered.
     * @throws JMSException Thrown if a Jms Exception is encountered.
     */
    public ActivemqTopicSubscriber(
    		final ApplicationContext appContext,
            final JmsTopicConfiguration config, final String name,
            final String filter, final boolean isTransacted)
            throws NamingException, JMSException {
        super(appContext);

        this.transacted = isTransacted;

        final Context context = createNamingContext(config);
        final TopicConnectionFactory factory =
                (TopicConnectionFactory) context.lookup(jmsConfig.getTopicFactoryName());
        createConnection(factory);
        final ActiveMQPrefetchPolicy prefetchPolicy =
                new ActiveMQPrefetchPolicy();
        prefetchPolicy.setTopicPrefetch(jmsConfig
                .getPrefetchLimit());
        if (jmsConfig
                .getPendingMessageLimit() != -1) {
            prefetchPolicy.setMaximumPendingMessageLimit(jmsConfig.getPendingMessageLimit());
        }
        ((ActiveMQConnection) connection).setPrefetchPolicy(prefetchPolicy);

        session =
                connection.createTopicSession(isTransacted, isTransacted
                        ? Session.SESSION_TRANSACTED
                                : Session.AUTO_ACKNOWLEDGE);


        final Topic topic = session.createTopic(config.getTopicName());

        if (name != null) {
            this.subscriberName = name;
            this.subscriber =
                    session.createDurableSubscriber(topic, name, filter, false);
        } else {
            this.subscriber = session.createSubscriber(topic, filter, false);
        }
    }
}
