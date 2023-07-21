/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.mds.server.tcp;

import org.springframework.messaging.Message;

import java.io.Serializable;
import java.util.List;

/**
 * Container for a message list
 */
public class MessageListContainer implements Serializable {
    private List<Message> messageList;

    /**
     * Constructor
     * @param messageList List of Message
     */
    public MessageListContainer(List<Message> messageList) {
        this.messageList = messageList;
    }

    /**
     * Get message list
     * @return List of message
     */
    public List<Message> getMessageList() {
        return messageList;
    }

    /**
     * Set message list
     * @param messageList List of Message
     */
    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
    }
}
