/*
 * Copyright 2006-2020. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.mds.server.disruptor;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.messaging.Message;

/**
 * Message event
 */
public class MessageEvent {

    private Message message;
    private long sequence;

    /**
     * Getter for message
     * @return Message object
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Setter for Message
     * @param message Message object
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    /**
     * Getter for sequence
     * @return Sequence as long
     */
    public long getSequence() {
        return this.sequence;
    }

    /**
     * Setter for sequence
     * @param sequence Sequence as long
     */
    public void setSequence(long sequence) { this.sequence = sequence; }

    /**
     * Event factory, used from RingBuffer
     */
    public static final EventFactory<MessageEvent> EVENT_FACTORY = MessageEvent::new;

    /**
     * Static event translator to set the data in the data events from the disruptor.
     */
    public static final EventTranslatorOneArg<MessageEvent, Message> DATA_TRANSLATOR = (event, sequence, data) -> {
        event.setMessage(data);
        event.setSequence(sequence);
    };

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
