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
package jpl.gds.automation.common;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesPoller;
import jpl.gds.tc.api.icmd.config.IntegratedCommandProperties;
import jpl.gds.tc.api.message.CommandMessageType;
import jpl.gds.tc.api.message.ICpdUplinkMessage;
import jpl.gds.tcapp.app.gui.icmd.model.CpdRequestPoolModel;

/**
 * The UplinkRequestManager class listens to the internal message bus for
 * outgoing commands. It provides a method that blocks until the specified
 * command has reached a final state. This class may only be constructed in an
 * AMPCS session where the uplink connection type is COMMAND_SERVICE.
 * @since AMPCS R6
 */
public class UplinkRequestManager implements MessageSubscriber {
    private final Tracer                 logger;

    
    private final IMessagePublicationBus context;

	/**
	 * The configured number of milliseconds between checks to see if command reached finalized state.
	 */
	private static int RADIATION_CHECK_INTERVAL;

    /** The CPD request pool representation in this process */
    private CpdRequestPoolModel requestPool;

    /** List of command messages that have not reached finalized states */
    private Map<Integer, List<String>> commandMessages;

    /** List of command messages that have reached finalized state */
    private List<Integer> processedMessageIds;

    /** Flag to indicate whether or not to actively perform the wait */
    private final boolean active;
    
    private final ApplicationContext appContext;

    /**
     * Constructor
     *
     */
    public UplinkRequestManager() {
    	
    	appContext = SpringContextFactory.getSpringContext(true);
        logger = TraceManager.getDefaultTracer(appContext);
    	context = appContext.getBean(IMessagePublicationBus.class);
    	RADIATION_CHECK_INTERVAL = appContext.getBean(IntegratedCommandProperties.class).getRadiationCheckInterval();
        final UplinkConnectionType connType = appContext.getBean(IConnectionMap.class).
                getFswUplinkConnection().getUplinkConnectionType();

        // only activate this class if the session has COMMAND_SERVICE uplink
        // connection type
        this.active =
                (connType != null)
                        && connType
                                .equals(UplinkConnectionType.COMMAND_SERVICE);

        logger.debug(String.format("UplinkRequestManager is%s active",
                this.active ? "" : " not"));
        if (this.active) {
            this.processedMessageIds = new LinkedList<Integer>();
            this.commandMessages =
                    new ConcurrentHashMap<Integer, List<String>>();
            this.requestPool = new CpdRequestPoolModel(appContext);
            this.startSubscriptions();

            /*
             * CpdRequestPoolModel relies on
             * CpdDmsBroadcastStatusMessagesPoller thread to start.
             */

    		appContext.getBean(ICpdDmsBroadcastStatusMessagesPoller.class).start();
        }
    }

    private void startSubscriptions() {
        context.subscribe(CommandMessageType.FlightSoftwareCommand, this);
        context.subscribe(CommandMessageType.HardwareCommand, this);
        context.subscribe(CommandMessageType.Scmf, this);
        context.subscribe(CommandMessageType.FileLoad, this);
        context.subscribe(CommandMessageType.RawUplinkData, this);
    }

    /**
     * Method that blocks until a finalized status is received. Only applicable
     * when UplinkConnectionType.COMMAND_SERVICE is used. Returns instantly for
     * all other UplinkConnectionType
     *
     * @param id the unique id of the uplink to wait for
     * @param timeout the number of seconds to wait before timing out
     * @throws TimeoutException If wait times out
     */
    public void waitForRadiation(final int id, final Integer timeout)
            throws TimeoutException {
        // return immediately if not activated
        if (!this.active) {
            return;
        }

        if (((timeout != null) && (timeout > 0))) {
            final long currTime = System.currentTimeMillis();

			while (!this.commandMessages.containsKey(id)) {
				SleepUtilities.checkedSleep(RADIATION_CHECK_INTERVAL);

                final long currTime2 = System.currentTimeMillis();

                if ((currTime2 - currTime) > (timeout * 1000)) {
                    final String errorMessage =
                            "Did not receive Command Message before timeout period of: "
                                    + timeout;
                    logger.warn("UplinkRequestManager wait for radiation timed out: "
                            + errorMessage);
                    throw new TimeoutException(errorMessage);
                }
            }

            final List<String> requestIds = this.commandMessages.get(id);

            for (final String rid : requestIds) {
                CommandStatusType status = this.requestPool.getStatus(rid);
				while ((status == null) || !status.isFinal()) {
					SleepUtilities
							.checkedSleep(RADIATION_CHECK_INTERVAL + 1000);

                    final long currTime2 = System.currentTimeMillis();

                    if ((currTime2 - currTime) > (timeout * 1000)) {
                        final String errorMessage =
                                "Did not detect a finalized status for request with ID="
                                        + rid + " before timeout period of: "
                                        + timeout;
                        logger.warn("UplinkRequestManager wait for radiation timed out: "
                                + errorMessage);
                        throw new TimeoutException(errorMessage);
                    }

                    status = this.requestPool.getStatus(rid);
                }
            }

            this.processedMessageIds.add(id);
            this.cleanUp();
        }
    }

    /**
     * Cleans up stored CommandMessages that have been processed
     */
    private void cleanUp() {
        final Iterator<Integer> iter = this.processedMessageIds.iterator();

        while (iter.hasNext()) {
            final Integer i = iter.next();

            if (this.commandMessages.containsKey(i)) {
                this.commandMessages.remove(i);
                iter.remove();
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * jpl.gds.shared.message.MessageSubscriber#handleMessage(jpl.gds.shared
     * .message.Message)
     */
    @Override
    public void handleMessage(final IMessage message) {
        if (message instanceof ICpdUplinkMessage) {
            final ICpdUplinkMessage msg = (ICpdUplinkMessage) message;

            List<String> requestIds = null;

            final Integer key = msg.getTransmitEventId();

            if (this.commandMessages.containsKey(key)) {
                requestIds = this.commandMessages.get(key);
            } else {
                requestIds = new LinkedList<String>();
                this.commandMessages.put(key, requestIds);
            }

            requestIds.add(msg.getICmdRequestId());
        }
    }
}
