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

import jpl.gds.context.api.TopicNameToken;
import jpl.gds.message.api.IInternalBusPublisher;
import jpl.gds.message.api.external.*;
import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.log.Tracer;


/**
 * A message subscriber for telemetry and packet messages sent from the telemetry ingest app.
 * It captures messages sent for the current session and re-publishes them on the internal bus
 *
 * 06/25/19 - Added disruptor to buffer incoming messages
 * 07/12/19 - Added internal publishing via spill processor
 *
 */
public class TelemetryMessageSubscriber implements IMessageServiceListener {
    private Tracer tracer;

    private IQueuingMessageHandler frameHandler;
    private IQueuingMessageHandler packetHandler;
    private IQueuingMessageHandler stationHandler;
    private IInternalBusPublisher publisher;

    private long lastMessageTime = System.nanoTime();

    /**
     * Constructor
     *
     * @param frameHandler         An IQueuingMessageHandler bean to handle frames
     * @param packetHandler        An IQueuingMessageHandler bean to handle packets
     * @param stationHandler        An IQueuingMessageHandler bean to handle stations
     * @param tracer               A tracer
     * @param sessionConfig        A session configuration bean
     * @param contextIdFilter      Filter for context ID
     * @param publisher            Internal publisher bean
     * @throws MessageServiceException If message related errors occur
     */
    public TelemetryMessageSubscriber(final IQueuingMessageHandler frameHandler,
                                      final IQueuingMessageHandler packetHandler,
                                      final IQueuingMessageHandler stationHandler,
                                      final Tracer tracer,
                                      final SessionConfiguration sessionConfig,
                                      final String contextIdFilter,
                                      final IInternalBusPublisher publisher
                                     )
            throws MessageServiceException {
        this.tracer = tracer;


        final String rootTopic = sessionConfig.getGeneralInfo().getRootPublicationTopic();
        this.publisher = publisher;

        /**
         *
         * Adding 'LIKE' to the CONTEXT_ID filter to match messages using a
         * wildcard pattern for the fragment portion instead of exact match.
         *
         * The contextIdFilter should be <Session_Number>/<Session_Host>/%
         * Ex: 20/atb-ocio-7/%
         */
        String filterContext = "CONTEXT_ID LIKE '" + contextIdFilter + "'";
        final String packetTopic = rootTopic + TopicNameToken.DELIMITER + TopicNameToken.APPLICATION_PACKET.getTopicNameComponent();
        final String frameTopic  = rootTopic + TopicNameToken.DELIMITER + TopicNameToken.APPLICATION_FRAME.getTopicNameComponent();
        final String stationTopic  = rootTopic + TopicNameToken.DELIMITER + TopicNameToken.APPLICATION_STATION.getTopicNameComponent();

        tracer.debug(this.getClass().getSimpleName(),
                     " context filter: ", filterContext,
                     " frameTopic: ", frameTopic,
                     " packetTopic:", packetTopic,
                     " stationTopic: ", stationTopic,
                     " derived from root topic: " , rootTopic);
        this.frameHandler = frameHandler;
        this.packetHandler = packetHandler;
        this.stationHandler = stationHandler;

        frameHandler.setSubscription(frameTopic, filterContext, false);
        frameHandler.addListener(this);

        packetHandler.setSubscription(packetTopic, filterContext, false);
        packetHandler.addListener(this);

        stationHandler.setSubscription(stationTopic, filterContext, false);
        stationHandler.addListener(this);

        start();
    }

    /**
     * Start the telemetry message subscriber
     *
     * @throws MessageServiceException If message related errors occur
     */
    public void start() throws MessageServiceException {
        packetHandler.start();
        frameHandler.start();
        stationHandler.start();
        tracer.info("Starting message subscriptions to topics: packet, frame, station");

        publisher.start();
        tracer.info("Starting internal bus publisher");
    }

    /**
     * Stop the telemetry message subscriber
     */
    public void stop() {
        packetHandler.shutdown(false, true);
        frameHandler.shutdown(false, true);
        stationHandler.shutdown(false, true);
        tracer.info("Stopping message subscriptions to topics: packet, frame, station");

        publisher.close();
        tracer.info("Stopping internal bus publisher");
    }

    @Override
    public void onMessage(IExternalMessage extMessage) {
        lastMessageTime = System.nanoTime();
        //send the message  to internal bus publisher (uses a spill processor)
        publisher.queueMessageForPublication(extMessage);
    }

    /**
     * Return idle time in seconds
     *
     * @return idle time in seconds
     */
    public long getIdleTime() {
        long idleTime = System.nanoTime() - lastMessageTime;
        return idleTime / 1_000_000_000;
    }

    /**
     * Returns whether there are items in backlog for the message handlers
     * @return True if more items to process, false otherwise
     */
    public boolean hasBacklog(){
        return frameHandler.hasBacklog() || packetHandler.hasBacklog() || publisher.hasBacklog();
    }
}
