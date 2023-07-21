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
package jpl.gds.time.api.message;

import jpl.gds.shared.message.IMessageType;

/**
 * An enumeration of public message types in the time_correlation modules.
 * 
 *
 * @since R8
 */
public enum TimeCorrelationMessageType implements IMessageType {
    /** Flight time correlation message. */
    FswTimeCorrelation,
    /** SSE time correlation message */
    SseTimeCorrelation;

    @Override
    public String getSubscriptionTag() {
        return name();
    }

}
