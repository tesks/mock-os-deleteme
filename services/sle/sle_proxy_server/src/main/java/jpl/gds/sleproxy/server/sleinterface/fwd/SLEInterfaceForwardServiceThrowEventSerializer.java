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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lsespace.sle.user.service.FcltuGetParameterResponse;
import com.lsespace.sle.user.service.FcltuParamName;
import com.lsespace.sle.user.service.SLEUserFcltuInstance;
import com.lsespace.sle.user.service.UserServiceState;
import com.lsespace.sle.user.util.concurrent.OperationFuture;

import jpl.gds.sleproxy.server.sleinterface.internal.config.SLEInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.websocket.EMessageType;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;

/**
 * This runnable class helps ensure that throw events are not sent to the FCLTU
 * provider before the previous event completes its cycle at the provider. The
 * provider invokes a CLTU-ASYNC-NOTIFY when it finishes performing an event.
 * This serializer holds all throw events and sends them as each event "clears"
 * at the provider. A throw event is considered to be cleared when the provider
 * sends an asynchronous notification with the throw event's ID.
 * 
 */
public class SLEInterfaceForwardServiceThrowEventSerializer implements Runnable {

	/**
	 * LSE Space's FCLTU service instance.
	 */
	private final SLEUserFcltuInstance userFcltuServiceInstance;

	/**
	 * The queue to read for new throw events.
	 */
	private final BlockingQueue<SLEInterfaceForwardServiceThrowEvent> throwEventQueue;

	/**
	 * The queue to read for confirmation that the previous throw event has
	 * cleared.
	 */
	private final BlockingQueue<SLEInterfaceForwardServiceThrowEventClearanceNotification> throwEventClearedConfirmationQueue;

	/**
	 * Timeout threshold for get parameter request.
	 */
	private final long getParameterTimeoutMillis;

	/**
	 * Timeout threshold for throw event request.
	 */
	private final long throwEventTimeoutMillis;

	/**
	 * Timeout threshold for throw event "clearance" at the provider.
	 */
	private final long throwEventClearanceTimeoutSeconds;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceForwardServiceThrowEventSerializer.class);

	/**
	 * Constructor.
	 * 
	 * @param userFcltuServiceInstance
	 *            The SLE FCLTU service instance that is the basis of this throw
	 *            event serializer's operations
	 * @param throwEventQueue
	 *            The queue that contains the throw events from which this
	 *            serializer should consume for sending to the FCLTU provider
	 * @param throwEventClearedIndicationQueue
	 *            The queue that contains the indication objects that inform
	 *            this serializer of the "clearance" of a previously sent throw
	 *            event
	 */
	SLEInterfaceForwardServiceThrowEventSerializer(final SLEUserFcltuInstance userFcltuServiceInstance,
			final BlockingQueue<SLEInterfaceForwardServiceThrowEvent> throwEventQueue,
			final BlockingQueue<SLEInterfaceForwardServiceThrowEventClearanceNotification> throwEventClearedIndicationQueue) {
		this.userFcltuServiceInstance = userFcltuServiceInstance;
		this.throwEventQueue = throwEventQueue;
		this.throwEventClearedConfirmationQueue = throwEventClearedIndicationQueue;
		this.getParameterTimeoutMillis = SLEInterfaceInternalConfigManager.INSTANCE
				.getForwardGetParameterTimeoutMillis();
		this.throwEventTimeoutMillis = SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventTimeoutMillis();
		this.throwEventClearanceTimeoutSeconds = SLEInterfaceInternalConfigManager.INSTANCE
				.getForwardThrowEventClearanceTimeoutSeconds();
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
				SLEInterfaceForwardServiceThrowEvent event = throwEventQueue.take();
				logger.info(
						"{} Took throw event (event-identifier: {}, event-qualifier (bytes): {}) from the queue. Initiating SLE interface forward service THROW EVENT",
						SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
						event.getEventId(), event.getQualifier());
				int eventInvocationId = -1;

				try {
					logger.debug("run() while loop: Calling getNextEventInvocationID()");
					eventInvocationId = getNextEventInvocationID();
				} catch (Throwable t) {
					/*
					 * Without the expected event ID value, can't throw the
					 * event. Abort this throw event.
					 */
					logger.error(
							"{} Aborting the throw event because unable to obtain expected-event-invocation-identification from the provider",
							SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
							event.getEventId(), event.getQualifier());
					
					MessageDistributor.INSTANCE.sleForwardProviderThrowEvent(
							event.getEventId(), 
							event.getQualifier(), 
							EMessageType.SLE_FORWARD_PROVIDER_THROW_EVENT_FAILURE,
							"Aborting the throw event because unable to obtain expected-event-invocation-identification from the provider");
					
					logger.debug("run() while loop: continue (skipping rest of the loop)");
					continue;
				}

				try {
					logger.debug("run() while loop: Calling throwEvent()");
					throwEvent(eventInvocationId, event);
				} catch (Throwable t) {
					/*
					 * Throw event failed. Abort this one.
					 */
					logger.error("{} Throw event failed. Aborting this particular throw event.",
							SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging());
					
					MessageDistributor.INSTANCE.sleForwardProviderThrowEvent(
							event.getEventId(), 
							event.getQualifier(), 
							EMessageType.SLE_FORWARD_PROVIDER_THROW_EVENT_FAILURE,
							"Throw event failed. Aborting this particular throw event.");

					logger.debug("run() while loop: continue (skipping rest of the loop)");
					continue;
				}

				logger.info(
						"{} Now awaiting for the provider to confirm completion of throw event event-invocation-identification: {}",
						SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
						eventInvocationId);
				boolean keepPolling = true;

				do {
					SLEInterfaceForwardServiceThrowEventClearanceNotification throwEventClearanceNotification = throwEventClearedConfirmationQueue
							.poll(throwEventClearanceTimeoutSeconds, TimeUnit.SECONDS);

					if (throwEventClearanceNotification == null) {
						logger.warn(
								"{} Timed out waiting for throw event to clear. event-invocation-identification: {}",
								SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
								eventInvocationId);
						
						MessageDistributor.INSTANCE.sleForwardProviderThrowEvent(
								event.getEventId(), 
								event.getQualifier(), 
								EMessageType.SLE_FORWARD_PROVIDER_THROW_EVENT_FAILURE,
								"Timed out waiting for throw event to clear. event-invocation-identification: " + eventInvocationId);
						
						keepPolling = false;
					} else if (throwEventClearanceNotification.getEventThrowId() == eventInvocationId) {
						// Our throw event cleared. Success.
						logger.info(
								"{} Throw event cleared at the provider. event-invocation-identification: {}, event-identifier: {}",
								SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
								eventInvocationId, event.getEventId(), event.getQualifier());
						keepPolling = false;
						
						MessageDistributor.INSTANCE.sleForwardProviderThrowEvent(
								event.getEventId(), 
								event.getQualifier(), 
								EMessageType.SLE_FORWARD_PROVIDER_THROW_EVENT_SUCCESS,
								"");

						SLEInterfaceForwardService.INSTANCE.notifyOfClearedThrowEvent(event.getEventId(),
								event.getQualifier(), throwEventClearanceNotification.getNotificationType());
					} else {
						// We received a clearance, but the IDs don't match
						logger.warn(
								"{} Received throw event clearance of event-invocation-identification: {}, which doesn't match that of one we sent, event-invocation-identification: {}. Continue waiting.",
								SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
								throwEventClearanceNotification, eventInvocationId);
					}

				} while (keepPolling);

			} catch (InterruptedException ie) {
				logger.debug("run() while loop: InterruptedException caught");
				Thread.currentThread().interrupt();
			}

		}

		logger.debug("run(): Exited the while loop");

		/*
		 * Thread is interrupted so we'll be exiting run(). Abandon throw
		 * events, if any. Probably no use sending them now.
		 */
		logger.debug("Exiting run()");
	}

	/**
	 * Obtains the expected event invocation identification from the SLE forward
	 * service provider.
	 * 
	 * @return The expected event invocation identification
	 * @throws Throwable
	 *             Thrown if obtaining the event invocation identification from
	 *             the provider results in an error
	 */
	private int getNextEventInvocationID() throws Throwable {
		logger.debug("Entered getNextEventInvocationID()");
		OperationFuture<FcltuGetParameterResponse> futureGetParameterResult = null;

		try {
			logger.debug(
					"getNextEventInvocationID(): Invoking the GET PARAMETER request for expected-event-invocation-identification");
			futureGetParameterResult = userFcltuServiceInstance
					.getFcltuParameter(FcltuParamName.EXPECTED_EVENT_INVOCATION_IDENTIFICATION);
		} catch (Exception e) {
			logger.error(
					"{} SLE interface forward service GET PARAMETER of expected-event-invocation-identification generated an exception",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), e);
			logger.debug("getNextEventInvocationID(): Throwing the Exception to abort this method");
			throw e;
		}

		/*
		 * Await for the get parameter operation to be completed. The wait time
		 * before timeout should be greater than the response timeout.
		 */
		futureGetParameterResult.awaitUninterruptibly(getParameterTimeoutMillis);

		if (futureGetParameterResult.isSuccess()) {
			logger.debug(
					"getNextEventInvocationID(): GET PARAMETER of expected-event-invocation-identification SUCCEEDED");
		} else {
			Throwable failCause = futureGetParameterResult.cause();
			logger.error(
					"{} SLE interface forward service GET PARAMETER of expected-event-invocation-identification FAILED",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), failCause);
			logger.debug("getNextEventInvocationID(): Throwing the Throwable to abort this method");
			throw failCause;
		}

		FcltuGetParameterResponse getParameterResponse = futureGetParameterResult.result();
		int expectedEventInvocationId = getParameterResponse.getExpectedEventInvocationId();
		logger.debug("getNextEventInvocationID(): GET PARAMETER of expected-event-invocation-identification is {}",
				expectedEventInvocationId);
		logger.debug("Exiting getNextEventInvocationID()");
		return expectedEventInvocationId;
	}

	/**
	 * Throw the event to the SLE forward service provider using the specified
	 * event invocation ID.
	 * 
	 * @param eventInvocationId
	 *            The event invocation ID for the event throw
	 * @param event
	 *            The event to throw
	 * @throws Throwable
	 *             Thrown when SLE interface forward service is not bound or
	 *             throw event fails
	 */
	private void throwEvent(final int eventInvocationId, final SLEInterfaceForwardServiceThrowEvent event)
			throws Throwable {
		logger.debug("Entered throwEvent(eventInvocationId: {}, eventId: {}, qualifier: {}", eventInvocationId,
				event.getEventId(), event.getQualifier());

		if (userFcltuServiceInstance == null || userFcltuServiceInstance.getState() == UserServiceState.UNBOUND) {
			logger.error("{} SLE interface forward service THROW EVENT is not allowed because not bound",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging());
			logger.debug("throwEvent(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException();
		}

		logger.debug("throwEvent(): Invoking the THROW EVENT request");
		// Invoke the throw event
		OperationFuture<Void> futureThrowEventResult = userFcltuServiceInstance.throwEvent(eventInvocationId,
				event.getEventId(), event.getQualifier());

		/*
		 * Await for the throw event operation to be completed. The wait time
		 * before timeout should be greater than the response timeout.
		 */
		futureThrowEventResult.awaitUninterruptibly(throwEventTimeoutMillis);

		if (futureThrowEventResult.isSuccess()) {
			// Throw event succeeded
			logger.info(
					"{} SLE interface forward service THROW EVENT of event-invocation-identification: {}, event-identifier: {}, event-qualifier (bytes): {} SUCCEEDED",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), eventInvocationId,
					event.getEventId(), event.getQualifier());
		} else {
			// Throw event failed. Log and throw reason.
			logger.error(
					"{} SLE interface forward service THROW EVENT of event-invocation-identification: {}, event-identifier: {}, event-qualifier (bytes): {} FAILED",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), eventInvocationId,
					event.getEventId(), event.getQualifier(), futureThrowEventResult.cause());
			logger.debug("throwEvent(): Throwing the Throwable to abort this method");
			throw futureThrowEventResult.cause();
		}

		logger.debug("Exiting throwEvent()");
	}

}
