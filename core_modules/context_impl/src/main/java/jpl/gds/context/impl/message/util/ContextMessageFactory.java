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

package jpl.gds.context.impl.message.util;

import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.context.api.message.util.IContextMessageFactory;
import jpl.gds.context.impl.message.ContextHeartbeatMessage;

/**
 * Class ContextMessageFactory
 */
public class ContextMessageFactory implements IContextMessageFactory {


    @Override
    public IContextHeartbeatMessage createContextHeartbeatMessage(ISimpleContextConfiguration contextConfiguration) {
        return new ContextHeartbeatMessage(contextConfiguration);
    }
}