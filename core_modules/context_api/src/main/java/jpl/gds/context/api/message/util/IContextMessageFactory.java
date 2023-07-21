/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.context.api.message.util;

import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.api.message.IContextHeartbeatMessage;

/**
 * Message utility API for Context messages
 *
 */
public interface IContextMessageFactory {

    /**
     * Creates a ContextHeartbeatMessage for a given context configuration
     *
     * @param contextConfiguration the Context configuration to create a heartbeat message for
     *
     * @return IContextHeartbeatMessage
     */
    IContextHeartbeatMessage createContextHeartbeatMessage(ISimpleContextConfiguration contextConfiguration);
}