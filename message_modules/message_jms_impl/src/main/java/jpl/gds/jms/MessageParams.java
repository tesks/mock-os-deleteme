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
package jpl.gds.jms;

import java.io.Serializable;
import java.util.Map;

import jpl.gds.message.api.external.ExternalDeliveryMode;
import jpl.gds.shared.message.IMessageType;

/**
 * Encapsulates information needed to publish either a text or binary
 * external message.
 */
public class MessageParams implements Serializable {
    /**
     * Serialize version number.
     */
    public static final long   serialVersionUID = 0L;

    /**
     * Message parameter type.
     */
    private final IMessageType type;
    
    /**
     * Message.
     */
    private final String       msg;

    /**
     * Data blob.
     */
    private final byte[]               blob;

    /**
     * Time to live.
     */
    private final long                 ttl;

    /**
     * Delivery mode.
     */
    private final ExternalDeliveryMode delivMode;

    /**
     * Message properties.
     */
    private final Map<String, Object>  properties;

    /**
     * Is a binary type message.
     */
    private boolean                    isBinary;

    /**
     * @return the type
     */
    public IMessageType getType() {
        return type;
    }

    /**
     * @return the msg
     */
    public String getMsg() {
        return msg;
    }

    /**
     * @return the blob
     */
    public byte[] getBlob() {
        return blob;
    }

    /**
     * @return the ttl
     */
    public long getTtl() {
        return ttl;
    }

    /**
     * @return the delivMode
     */
    public ExternalDeliveryMode getDelivMode() {
        return delivMode;
    }

    /**
     * @return the properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * @return the isBinary
     */
    public boolean isBinary() {
        return isBinary;
    }

    /**
     * Constructor for binary type messages.
     * 
     * @param type2
     *            Message type.
     * @param b
     *            Binary data.
     * @param time
     *            Time to live.
     * @param mode
     *            Message mode.
     * @param props
     *            Message properties.
     */
    public MessageParams(final IMessageType type2, final byte[] b, final long time,
            final ExternalDeliveryMode mode, final Map<String, Object> props) {
        super();

        type = type2;
        blob = b;
        ttl = time;
        delivMode = mode;
        properties = props;
        isBinary = true;
        msg = null;
    }

    /**
     * Constructor for text type messages.
     * 
     * @param t
     *            Message type.
     * @param m
     *            Message text.
     * @param time
     *            Time to live.
     * @param mode
     *            Message mode.
     * @param props
     *            Message properties.
     */
    public MessageParams(final IMessageType t, final String m, final long time,
            final ExternalDeliveryMode mode, final Map<String, Object> props) {
        super();

        type = t;
        msg = m;
        ttl = time;
        delivMode = mode;
        properties = props;
        blob = null;
    }
}
