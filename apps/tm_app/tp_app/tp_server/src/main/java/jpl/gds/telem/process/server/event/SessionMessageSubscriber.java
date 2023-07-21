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
package jpl.gds.telem.process.server.event;

import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.ITopicSubscriber;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.SessionIdentification;
import jpl.gds.session.message.EndOfSessionMessage;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.session.message.StartOfSessionMessage;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.metadata.context.IContextKey;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * JMS subscriber that consumes Start of Session and End of Session messages from Telemetry Ingestor and converts them
 * into SessionStartEvent and SessionEndEvent. The events are published to the application context,
 * and are consumed by the ProcessServerManager
 *
 */
public class SessionMessageSubscriber implements IMessageServiceListener {

    private ApplicationContext ctx;

    private Tracer tracer;

    private ITopicSubscriber sub;

    private IExternalMessageUtility util;

    private IMessageClientFactory factory;

    private IContextKey contextKey;

    /**
     * Constructor
     * @param ctx Spring application context
     * @param factory Client message factory
     * @param util IExternalMessageUtility implementation
     * @param contextKey Context Key
     * @param t Tracer
     */
    public SessionMessageSubscriber(ApplicationContext ctx, IMessageClientFactory factory, IExternalMessageUtility util,
                                    IContextKey contextKey, Tracer t) {
        this.ctx = ctx;
        this.factory = factory;
        this.tracer = t;
        tracer.debug("Session message subscriber created.");
        this.util = util;
        this.contextKey = contextKey;
    }

    /**
     * Start subscriber
     * @throws MessageServiceException if any error occurs
     */
    public void start() throws MessageServiceException {

        String generalTopic = ContextTopicNameFactory.getGeneralTopic(ctx);
        sub = factory.getTopicSubscriber(generalTopic,
                "MESSAGE_TYPE = 'StartOfSession' OR MESSAGE_TYPE = 'EndOfSession'", false);
        sub.setMessageListener(this);

        sub.start();
        tracer.debug("Session message subscriber started");
    }

    /**
     * Stop subscriber
     */
    public void stop() {
        sub.close();
        tracer.debug("Session message subscriber stopped.");
    }

    @Override
    public void onMessage(IExternalMessage message) {

        IMessageType type = null;

        try {
            type = util.getInternalType(message);
        } catch (MessageServiceException e) {
            tracer.error("Error parsing internal message type from external message.", e);
        }

        List<ApplicationEvent> events = new ArrayList<>();

        if (type == SessionMessageType.StartOfSession) {
            try {
                IMessage[] messages = util.instantiateMessages(message);

                for (IMessage msg : messages) {
                    if (msg.isType(SessionMessageType.StartOfSession)) {
                        tracer.info("Received start of session message: ", msg);
                        StartOfSessionMessage sosm = (StartOfSessionMessage) msg;

                        if (sosm.getContextKey().getParentNumber().equals(contextKey.getNumber())) {
                            // reject session start messages that originate from this TP/Context ID.
                            tracer.debug(
                                    "Rejecting session start message that originated from this TP with context ID: ",
                                    contextKey.getNumber());
                            return;
                        }

                        SessionConfiguration config = (SessionConfiguration) sosm.getContextConfiguration();
                        SessionStartEvent    event  = new SessionStartEvent(config, sosm.getContextKey());

                        events.add(event);
                    }
                }
            } catch (MessageServiceException e) {
                tracer.error("Error instantiating messages from external StartOfSession message", e);
            }
        } else if (type == SessionMessageType.EndOfSession) {
            tracer.debug("Received end of session message");
            try {
                IMessage[] messages = util.instantiateMessages(message);

                for (IMessage msg : messages) {
                    if (msg.isType(SessionMessageType.EndOfSession)) {
                        tracer.info("Received end of session message: ", msg);
                        EndOfSessionMessage   eosm  = (EndOfSessionMessage) msg;
                        SessionIdentification id    = (SessionIdentification) eosm.getContextId();
                        SessionEndEvent       event = new SessionEndEvent(id);

                        events.add(event);
                    }
                }
            } catch (MessageServiceException e) {
                tracer.error("Error instantiating messages from external EndOfSession message", e);
            }
        } else {
            tracer.trace("Received message that was not of proper type: expected StartOfSessionMessage or "
                                 + "EndOfSessionMessage");
        }

        for (ApplicationEvent event : events) {
            ctx.publishEvent(event);
            tracer.debug("Published ", type, " message to spring event bus. ", event.getSource());
        }
    }

}
