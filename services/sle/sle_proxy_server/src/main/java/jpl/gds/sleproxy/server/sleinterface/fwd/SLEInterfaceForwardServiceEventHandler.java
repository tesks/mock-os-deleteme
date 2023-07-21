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
package jpl.gds.sleproxy.server.sleinterface.fwd;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lsespace.sle.user.service.SLEUserFcltuInstance;
import com.lsespace.sle.user.service.UserServiceState;

import jpl.gds.sleproxy.server.sleinterface.fwd.action.EForwardActionType;
import jpl.gds.sleproxy.server.time.DateTimeFormattingUtil;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;

/**
 * This class handles the received SLE forward service events that exists in a
 * queue. This class will monitor the queue, pick up any events that exist in
 * there, and for now, just logs them.
 * 
 */
public class SLEInterfaceForwardServiceEventHandler implements Runnable {

	/**
	 * The queue to monitor.
	 */
	private final BlockingQueue<SLEInterfaceForwardServiceEvent> eventQueue;

	/**
	 * The queue to push any confirmations received that a throw event has
	 * cleared at the provider.
	 */
	private final BlockingQueue<SLEInterfaceForwardServiceThrowEventClearanceNotification> throwEventClearedConfirmationQueue;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceForwardServiceEventHandler.class);

	/**
	 * Construct the handler using the provided queue to monitor.
	 * 
	 * @param eventQueue
	 *            The event queue to look for items to process
	 * @param throwEventClearedConfirmationQueue
	 *            The queue to push confirmations of throw event clearance
	 */
	SLEInterfaceForwardServiceEventHandler(final BlockingQueue<SLEInterfaceForwardServiceEvent> eventQueue,
			final BlockingQueue<SLEInterfaceForwardServiceThrowEventClearanceNotification> throwEventClearedConfirmationQueue) {
		this.eventQueue = eventQueue;
		this.throwEventClearedConfirmationQueue = throwEventClearedConfirmationQueue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public final void run() {
		logger.debug("Entered run()");

		while (!(Thread.currentThread().isInterrupted())) {

			try {
				logger.trace("run() while loop: Taking from event queue (may block)");
				handle(eventQueue.take());
			} catch (InterruptedException ie) {
				logger.debug("run() while loop: Caught InterruptedException");
				Thread.currentThread().interrupt();
			}

		}

		logger.debug("run(): Exited while loop");

		/*
		 * Thread is interrupted so we'll be exiting run(). But handle all
		 * remaining events in the queue before we go.
		 */
		final List<SLEInterfaceForwardServiceEvent> remainingEvents = new LinkedList<>();
		eventQueue.drainTo(remainingEvents);
		logger.info("{} Event handler is now stopping. Draining the event queue and handling the remaining {} events.",
				SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
				remainingEvents.size());
		remainingEvents.forEach(event -> handle(event));
		logger.debug("Exiting run()");
	}

	/**
	 * Handle an event.
	 * 
	 * @param event
	 *            The event to handle
	 */
	private void handle(final SLEInterfaceForwardServiceEvent event) {
		logger.trace("Entered handle(event type: {})", event.getType());
		SLEUserFcltuInstance service = event.getService();

		switch (event.getType()) {

		case ASYNC_NOTIFICATION:
			logger.info(
					"{} SLE interface forward service received an event: ASYNC NOTIFY. SIID: {}, notification-type: {}, event-thrown-identification: {}, cltu-last-processed: {}, cltu-last-OK: {}, cltu-status: {}, radiation-start-time: {}, radiation-stop-time: {}, production-status: {}, uplink-status: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
					event.getNotif().getNotificationType().getName(), event.getNotif().getEventThrowId(),
					event.getNotif().getCltuLastProcessedId(), event.getNotif().getCltuLastOkId(),
					event.getNotif().getCltuLastProcessedCltuStatus(),
					(event.getNotif().getCltuLastProcessedRadiationStartTime() != null ? DateTimeFormattingUtil.INSTANCE
							.toAMPCSDateTimeString(event.getNotif().getCltuLastProcessedRadiationStartTime()) : null),
					(event.getNotif().getCltuLastOkRadiationStopTime() != null ? DateTimeFormattingUtil.INSTANCE
							.toAMPCSDateTimeString(event.getNotif().getCltuLastOkRadiationStopTime()) : null),
					event.getNotif().getProductionStatus().getName(), event.getNotif().getUplinkStatus());

			/*
			 * Non-null event-thrown-identification means it's a clearance
			 * indicator of a formerly thrown event
			 */
			if (event.getNotif().getEventThrowId() != null) {

				try {
					throwEventClearedConfirmationQueue
							.add(new SLEInterfaceForwardServiceThrowEventClearanceNotification(
									event.getNotif().getEventThrowId(), event.getNotif().getNotificationType()));
				} catch (Exception e) {
					logger.error(
							"{} SLE interface forward service failed to queue a throw event clearance notification",
							SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), e);
				}

			}

			break;
		case START:
			logger.info("{} SLE interface forward service received an event: START. SIID: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
					service.getSiid());
			break;
		case STATE_CHANGE:
			// Need to capture the case where the "Peer Abort" is done from the Provider's
			// side and notify the GUI clients to change their state to UNBOUND
			if (event.getState() == UserServiceState.UNBOUND) {
				MessageDistributor.INSTANCE.sleForwardProviderStateChange(EForwardActionType.UNBIND, null,
						ZonedDateTime.now(ZoneOffset.UTC), null);
				
				/*
				 * MPCS-8683: Remote-initiated peer abort causes the state to go
				 * UNBOUND, and connection number and delivery mode need to be
				 * reinitialized.
				 */
				SLEInterfaceForwardService.INSTANCE.initializeConnectionNumberAndDeliveryMode();
			}
			logger.info("{} SLE interface forward service received an event: STATE CHANGE. SIID: {}, service state: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
					event.getState().getName());
			break;
		case STATUS_REPORT:
			logger.info(
					"{} SLE interface forward service received an event: STATUS REPORT. SIID: {}, cltu-last-processed: {}, cltu-last-OK: {}, cltu-status: {}, radiation-start-time: {}, radiation-stop-time: {}, production-status: {}, uplink-status: {}, number-of-cltus-received: {}, number-of-cltus-processed: {}, number-of-cltus-radiated: {}, cltu-buffer-available: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
					event.getReport().getCltuLastProcessed(), event.getReport().getCltuLastOk(),
					event.getReport().getCltuStatus(),
					(event.getReport().getRadiationStartTime() != null ? DateTimeFormattingUtil.INSTANCE
							.toAMPCSDateTimeString(event.getReport().getRadiationStartTime()) : null),
					(event.getReport().getRadiationStopTime() != null ? DateTimeFormattingUtil.INSTANCE
							.toAMPCSDateTimeString(event.getReport().getRadiationStopTime()) : null),
					event.getReport().getProductionStatus(), event.getReport().getUplinkStatus(),
					event.getReport().getNumberOfCltusReceived(), event.getReport().getNumberOfCltusProcessed(),
					event.getReport().getNumberOfCltusRadiated(), event.getReport().getCltuBufferAvailable());
			break;
		case STOP:
			logger.info("{} SLE interface forward service received an event: STOP. SIID: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
					service.getSiid());
			break;
		default:
			logger.error("{} UNEXPECTED EVENT TYPE RECEIVED: {}. SIID: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), event.getType(),
					service.getSiid());
			break;
		}

		logger.trace("Exiting handle()");
	}

}
