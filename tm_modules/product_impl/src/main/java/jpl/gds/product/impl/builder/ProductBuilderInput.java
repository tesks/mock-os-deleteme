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
package jpl.gds.product.impl.builder;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.builder.IProductBuilderService;
import jpl.gds.product.api.builder.IProductMissionAdaptor;
import jpl.gds.product.api.builder.IProductPacketFilter;
import jpl.gds.product.api.message.IPartReceivedMessage;
import jpl.gds.product.api.message.IProductMessageFactory;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;
import org.springframework.context.ApplicationContext;

/**
 *  ProductBuilderInput listens for Packet messages on the internal bus, for a single virtual
 *  channel, creates Product Parts from the packets, and publishes Product Part Received
 *  messages to the internal message bus. ProductBuilderInput objects are created and shutdown
 *  by the ProductBuilder.
 *
 *  @see IProductBuilderService
 *
 */
public class ProductBuilderInput {
    private final Tracer log ;


    private final IMessagePublicationBus messageContext;
    private IProductMissionAdaptor missionAdaptation;
	private IProductPacketFilter productPacketFilter;
    private final int vcid;
    private ProductInputMessageSubscriber messageHandler;

    private final IProductBuilderObjectFactory instanceFactory;
    private final ApplicationContext appContext;
    private final IStatusMessageFactory statusMessageFactory;
    private final IProductMessageFactory productMessageFactory;
    private final IContextConfiguration contextConfig;

    /**
     * Creates an instance of ProductInput.
     * @param appContext the current application context
     * @param vcid the virtual channel ID of product packets to process
     */
    public ProductBuilderInput(final ApplicationContext appContext, final int vcid) {
        this.vcid = vcid;
        this.appContext = appContext;
        this.log = TraceManager.getTracer(appContext, Loggers.PRODUCT_BUILDER);
        this.instanceFactory = appContext.getBean(IProductBuilderObjectFactory.class);
        this.statusMessageFactory = appContext.getBean(IStatusMessageFactory.class);
        this.productMessageFactory = appContext.getBean(IProductMessageFactory.class);
        this.messageContext = appContext.getBean(IMessagePublicationBus.class);
        this.contextConfig = appContext.getBean(IContextConfiguration.class);
    }

    /**
     * Sets the mission adaptation and causes this object to subscribe to packet messages.
     * @param missionAdaptation the ProductMissionAdaptor to be used for
     * building product parts
     */
    public void subscribeToPackets(final IProductMissionAdaptor missionAdaptation) {
        this.missionAdaptation = missionAdaptation;
        messageHandler = new ProductInputMessageSubscriber();
        productPacketFilter = appContext.getBean(IProductPacketFilter.class, vcid);
    }

    /**
     * Publishes a ProductPartMessage for the given product part.
     * @param part the part to publish
     */
    private void publishProductPart(final IProductPartProvider part) {
        final IPartReceivedMessage m = productMessageFactory.createPartReceivedMessage(part);

        this.instanceFactory.convertToMetadataUpdater(part.getMetadata()).setProductType(missionAdaptation.getProductType(part));
        if (log.isDebugEnabled()) {
            log.debug("Publishing: " + m);
        }
        messageContext.publish(m);
    }

    /**
     * Processes a TelemetryPacketMessage by creating a product part from it and
     * publishing a part received message for the new part.
     * @param message the IPacketMessage
     */
    private void handlePacketMessage(final ITelemetryPacketMessage message) {

		if (!contextConfig.accept(message) || !productPacketFilter.matches(message)) {
			return;
		}
		try {
			final IProductPartProvider part = instanceFactory.createPart(message);
            if (part == null) {
                log.error("Part is null in ProductInput.handlePacketMessage()");
                final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(
                        TraceSeverity.ERROR, "Null part error processing data product part; part has been discarded");
                messageContext.publish(lm);
                return;
            }
			publishProductPart(part);
        } catch (final ProductException e) {
            final IPublishableLogMessage lm = statusMessageFactory.createPublishableLogMessage(TraceSeverity.ERROR,
                    "Error processing data product part:  " + e.getMessage() + " part has been discarded",
                    LogMessageType.INVALID_PDU_DATA);
            messageContext.publish(lm);
            log.log(lm);
        }
    }

    /**
     * Stops processing of packet messages.
     */
    public void shutdown() {
        if (messageHandler != null) {
            messageContext.unsubscribeAll(messageHandler);
        }
    }

    /**
     *
     * ProductInputMessageSubscriber is the listener for internal packet messages.
     *
     *
     */
    private class ProductInputMessageSubscriber extends BaseMessageHandler {

        /**
         * Creates an instance of ProductInputMessageSubscriber.
         */
        public ProductInputMessageSubscriber() {
            messageContext.subscribe(TmServiceMessageType.TelemetryPacket, this);
        }

        /**
         * {@inheritDoc}
         * @see jpl.gds.shared.message.BaseMessageHandler#handleMessage(IMessage)
         */
        @Override
        public void handleMessage(final IMessage m) {
            final ITelemetryPacketMessage pktMsgFromPktExtract = (ITelemetryPacketMessage)m;
            handlePacketMessage(pktMsgFromPktExtract);
        }
    }
}