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
package jpl.gds.message.api;

import jpl.gds.shared.message.IMessage;

/**
 * IStreamMessage is an interface implemented by messages that have a stream ID.
 * Stream ID is used to associate groups of messages together.
 */
public interface IStreamMessage extends IMessage {
    /**
     * Gets the message stream ID.
     * @return the stream ID.
     */
    public String getStreamId();

    /**
     * Sets the message stream ID.
     * @param id the stream ID to set
     */
    public void setStreamId(String id);
}
