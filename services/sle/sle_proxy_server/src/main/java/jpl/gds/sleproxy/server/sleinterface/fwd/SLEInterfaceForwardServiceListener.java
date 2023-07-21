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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lsespace.sle.user.service.FcltuStatusReport;
import com.lsespace.sle.user.service.SLEUserFcltuInstance;
import com.lsespace.sle.user.service.SLEUserFcltuInstanceListener;
import com.lsespace.sle.user.service.UserServiceState;
import com.lsespace.sle.user.service.data.FcltuAsyncNotification;

/**
 * This handling class listens for incoming, asynchronous SLE forward service
 * events. It doesn't process it, but rather sticks them in a queue so that a
 * separate handler can individually process them. This is looking toward the
 * future, when possibly more compute/IO intensive work needs to be done on the
 * events.
 * 
 * This is a callback class, implementing the interface provided in the LSE
 * Space's SLE framework.
 * 
 */
class SLEInterfaceForwardServiceListener implements SLEUserFcltuInstanceListener {

	/**
	 * The queue to push new, incoming events to.
	 */
	private final BlockingQueue<SLEInterfaceForwardServiceEvent> eventQueue;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceForwardServiceListener.class);

	/**
	 * Construct the listener with the provided queue to push incoming events
	 * to.
	 * 
	 * @param eventQueue
	 *            Queue to push incoming events to
	 */
	SLEInterfaceForwardServiceListener(final BlockingQueue<SLEInterfaceForwardServiceEvent> eventQueue) {
		this.eventQueue = eventQueue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lsespace.sle.user.service.SLEUserFcltuInstanceListener#
	 * onAsyncNotification(com.lsespace.sle.user.service.SLEUserFcltuInstance,
	 * com.lsespace.sle.user.service.data.FcltuAsyncNotification)
	 */
	@Override
	public void onAsyncNotification(final SLEUserFcltuInstance service, final FcltuAsyncNotification notif) {
		logger.debug("Entered onAsyncNotification(SIID: {})", service.getSiid());

		try {
			eventQueue.add(new SLEInterfaceForwardServiceEvent(SLEInterfaceForwardServiceEvent.Type.ASYNC_NOTIFICATION,
					service, notif));
		} catch (Exception e) {
			logger.error(
					"{} SLE interface forward service failed to queue a received ASYNC NOTIFY event for later handling. SIID: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
					e);
		}

		logger.debug("Exiting onAsyncNotification()");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserFcltuInstanceListener#onStart(com.
	 * lsespace.sle.user.service.SLEUserFcltuInstance)
	 */
	@Override
	public void onStart(final SLEUserFcltuInstance service) {
		logger.debug("Entered onStart(SIID: {})", service.getSiid());

		try {
			eventQueue.add(new SLEInterfaceForwardServiceEvent(SLEInterfaceForwardServiceEvent.Type.START, service));
		} catch (Exception e) {
			logger.error(
					"{} SLE interface forward service failed to queue a received START event for later handling. SIID: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
					e);
		}

		logger.debug("Exiting onStart()");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserFcltuInstanceListener#onStateChange(
	 * com.lsespace.sle.user.service.SLEUserFcltuInstance,
	 * com.lsespace.sle.user.service.UserServiceState)
	 */
	@Override
	public void onStateChange(final SLEUserFcltuInstance service, final UserServiceState state) {
		logger.debug("Entered onStateChange(SIID: {})", service.getSiid());
		
		try {
			eventQueue.add(new SLEInterfaceForwardServiceEvent(SLEInterfaceForwardServiceEvent.Type.STATE_CHANGE,
					service, state));
		} catch (Exception e) {
			logger.error(
					"{} SLE interface forward service failed to queue a received STATE CHANGE event for later handling. SIID: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
					e);
		}

		logger.debug("Exiting onStateChange()");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserFcltuInstanceListener#onStatusReport
	 * (com.lsespace.sle.user.service.SLEUserFcltuInstance,
	 * com.lsespace.sle.user.service.FcltuStatusReport)
	 */
	@Override
	public void onStatusReport(final SLEUserFcltuInstance service, final FcltuStatusReport report) {
		logger.debug("Entered onStatusReport(SIID: {})", service.getSiid());

		try {
			eventQueue.add(new SLEInterfaceForwardServiceEvent(SLEInterfaceForwardServiceEvent.Type.STATUS_REPORT,
					service, report));
		} catch (Exception e) {
			logger.error(
					"{} SLE interface forward service failed to queue a received STATUS REPORT event for later handling. SIID: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
					e);
		}

		logger.debug("Exiting onStatusReport()");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserFcltuInstanceListener#onStop(com.
	 * lsespace.sle.user.service.SLEUserFcltuInstance)
	 */
	@Override
	public void onStop(final SLEUserFcltuInstance service) {
		logger.debug("Entered onStop(SIID: {})", service.getSiid());

		try {
			eventQueue.add(new SLEInterfaceForwardServiceEvent(SLEInterfaceForwardServiceEvent.Type.STOP, service));
		} catch (Exception e) {
			logger.error(
					"{} SLE interface forward service failed to queue a received STOP event for later handling. SIID: {}",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
					e);
		}

		logger.debug("Exiting onStop()");
	}

}
