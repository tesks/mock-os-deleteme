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
package jpl.gds.jms.config;

import jpl.gds.context.api.TopicNameToken;
import jpl.gds.jms.config.JmsProperties.PreparerClassType;
import jpl.gds.message.api.external.ExternalDeliveryMode;
import jpl.gds.shared.message.IMessageType;

/**
 * Configuration object which holds message configuration properties for a specific 
 * message type.
 */
public class PublishableMessageConfig {
    private final long timeToLive;
    private final ExternalDeliveryMode persistence;
    private final int batchSize;
    private final long batchTimeout;
    private final TopicNameToken[] topics;
    private final IMessageType type;
    private JmsProperties.PreparerClassType preparerClassType;
    private final boolean formatIsBinary;

    /**
     * Constructor which specifies the initial configuration properties.
     * 
     * @param type
     *            message type
     * @param ttl
     *            time to live
     * @param persist
     *            level of persistence
     * @param topics
     *            message topics
     * @param batch
     *            batch size
     * @param timeout
     *            message timeout
     * @param prepClassType
     *            preparer class name
     * @param bin
     *            whether message is binary
     */
    public PublishableMessageConfig(final IMessageType type, final long ttl,
            final ExternalDeliveryMode persist, final TopicNameToken[] topics, final int batch,
            final long timeout, final PreparerClassType prepClassType,
            final boolean bin) {
        this.type = type;
        this.timeToLive = ttl;
        this.batchSize = batch;
        this.batchTimeout = timeout;
        this.persistence = persist;
        this.topics = topics;
        this.preparerClassType = prepClassType;
        this.formatIsBinary = bin;
    }

    /**
     * Returns how many messages will be batched together.
     * 
     * @return batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Returns the timeout before sending the batch.
     * 
     * @return batch timeout
     */
    public long getBatchTimeout() {
        return batchTimeout;
    }

    /**
     * Returns the level of persistence.
     * 
     * @return persistence level
     */
    public ExternalDeliveryMode getPersistence() {
        return persistence;
    }

    /**
     * Returns the preparer class type.
     * 
     * @return preparar type
     */
    public JmsProperties.PreparerClassType getPreparerType() {
        return preparerClassType;
    }

    /**
     * Returns the time to live for messages.
     * 
     * @return time to live
     */
    public long getTimeToLive() {
        return timeToLive;
    }

    /**
     * Returns a topic token array of message topics.
     * 
     * @return message topic tokens
     */
    public TopicNameToken[] getTopics() {
        return topics;
    }

    /**
     * Returns the message type.
     * 
     * @return message type
     */
    public IMessageType getType() {
        return type;
    }

    /**
     * Returns whether the message is in binary format or not.
     * 
     * @return if binary
     */
    public boolean isBinary() {
        return formatIsBinary;
    }

    /**
     * Sets the preparer type.
     * 
     * @param preparer
     *            preparer type
     */
    public void setPreparerClassType(final JmsProperties.PreparerClassType preparer) {
        this.preparerClassType = preparer;
    }

}
