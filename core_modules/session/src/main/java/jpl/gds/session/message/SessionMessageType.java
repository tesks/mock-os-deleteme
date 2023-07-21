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
package jpl.gds.session.message;

import jpl.gds.shared.message.IMessageType;

/**
 * A message type enumeration for session-related messages.
 * 
 * @since R8
 */
public enum SessionMessageType implements IMessageType {
    /** End of session (end of test) message type */
    EndOfSession,
    /** Session heartbeat message type */
    SessionHeartbeat,
    /** Start of session (start of test) message type */
    StartOfSession;

    @Override
    public String getSubscriptionTag() {
        return name();
    } 
}
