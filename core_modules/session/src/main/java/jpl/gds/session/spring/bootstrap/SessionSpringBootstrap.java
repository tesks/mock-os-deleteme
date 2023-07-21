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
package jpl.gds.session.spring.bootstrap;

import org.springframework.context.annotation.Configuration;

import jpl.gds.session.message.EndOfSessionMessage;
import jpl.gds.session.message.SessionHeartbeatMessage;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.session.message.StartOfSessionMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;

/**
 * Spring bootstrap configuration class for the session project.
 * 
 *
 * @since R8
 */
@Configuration
public class SessionSpringBootstrap {

    /**
     * Constructor.
     */
    public SessionSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(SessionMessageType.EndOfSession,
                EndOfSessionMessage.XmlParseHandler.class.getName(), null, new String[] {"EndOfTest"}));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(SessionMessageType.StartOfSession,
                StartOfSessionMessage.XmlParseHandler.class.getName(), null, new String[] {"StartOfTest"}));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(SessionMessageType.SessionHeartbeat,
                SessionHeartbeatMessage.XmlParseHandler.class.getName(), null, new String[] {"Heartbeat"}));
    }

}
