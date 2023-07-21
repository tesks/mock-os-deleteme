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
package jpl.gds.watcher;

import jpl.gds.shared.message.IMessageType;

/**
 * This enumeration just represents a fake message type called "All" for used by
 * message responders when defining message handlers that apply to all incoming messages.
 */
public enum ResponderMessageType implements IMessageType {
    /** Matches any message type */
    Any;

    @Override
    public String getSubscriptionTag() {
        return name();
    }

}
