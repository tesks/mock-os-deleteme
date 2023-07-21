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
package jpl.gds.sleproxy.server.sleinterface.rtn;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lsespace.sle.user.service.RafStatusReport;
import com.lsespace.sle.user.service.RcfStatusReport;
import com.lsespace.sle.user.service.SLEUserRafInstance;
import com.lsespace.sle.user.service.SLEUserRafInstanceListener;
import com.lsespace.sle.user.service.SLEUserRcfInstance;
import com.lsespace.sle.user.service.SLEUserRcfInstanceListener;
import com.lsespace.sle.user.service.SLEUserServiceInstance;
import com.lsespace.sle.user.service.UserServiceState;
import com.lsespace.sle.user.service.data.DiscardedDataNotification;
import com.lsespace.sle.user.service.data.EndOfDataNotification;
import com.lsespace.sle.user.service.data.LossOfSyncNotification;
import com.lsespace.sle.user.service.data.TmProductionData;
import com.lsespace.sle.user.service.data.TmProductionStatusNotification;
import com.lsespace.sle.user.service.data.TransferBufferData;

import jpl.gds.sleproxy.server.sleinterface.rtn.action.EReturnActionType;
import jpl.gds.sleproxy.server.time.DateTimeFormattingUtil;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;

/**
 * This handling class listens for incoming SLE return service events. It
 * processes it right away, then returns. This way, we can throttle the return
 * provider's data rate in case the chill interface downlink connection isn't
 * able to keep up. This approach has been decided after observing that the
 * downlink provider blasts the chill_sle_proxy with a very high data rate.
 * 
 * This is a callback class, implementing the interface provided in the LSE
 * Space's SLE framework.
 * 
 * For most of the events, this handler will just log them. If it's a transfer
 * buffer type of event, however, the handler will push the received frame(s) to
 * another queue, the frames queue.
 * 
 */
public class SLEInterfaceReturnServiceListeningEventHandler
		implements SLEUserRafInstanceListener, SLEUserRcfInstanceListener {

	/**
	 * The queue to push received frames into.
	 */
	private volatile BlockingQueue<TmProductionData> frameQueue;

	/**
	 * Lock object for the frame queue.
	 */
	private volatile Object frameQueueLock;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceReturnServiceListeningEventHandler.class);

	/**
	 * Default constructor.
	 */
	public SLEInterfaceReturnServiceListeningEventHandler() {
		frameQueueLock = new Object();
	}

	/**
	 * Actual implementation of the onTransferBuffer callback method. This way
	 * we can handle the start event in a RAF/RCF-agnostic way.
	 * 
	 * @param service
	 *            RAF or RCF user service instance
	 * @param numFrames
	 *            Number of frames in data
	 * @param numGoodFrames
	 *            Number of good frames in data
	 * @param data
	 *            The actual data of the transfer buffer event
	 */
	private void onTransferBufferImpl(final SLEUserServiceInstance service, final int numFrames,
			final int numGoodFrames, final List<TransferBufferData> data) {
		logger.trace("Entered onTransferBufferImpl(SIID: {}, numFrames: {}, numGoodFrames: {}, data count: {})",
				service.getSiid(), numFrames, numGoodFrames, data.size());
		List<TmProductionData> frames = new ArrayList<>(numFrames);
		int counterForLogging = 1;

		for (TransferBufferData singleData : data) {

			if (singleData instanceof TmProductionData) {
				logger.trace(
						"onTransferBufferImpl() data for loop: TransferBufferData {} of {} is TmProductionData. Adding to current frames list.",
						counterForLogging, data.size());
				frames.add((TmProductionData) singleData);
			} else if (singleData instanceof DiscardedDataNotification) {
				logger.trace(
						"onTransferBufferImpl() data for loop: TransferBufferData {} of {} is DiscardedDataNotification",
						counterForLogging, data.size());
				logger.info("{} SLE interface return service received an event: DATA DISCARDED. SIID: {}",
						SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
						service.getSiid());
			} else if (singleData instanceof EndOfDataNotification) {
				logger.trace(
						"onTransferBufferImpl() data for loop: TransferBufferData {} of {} is EndOfDataNotification",
						counterForLogging, data.size());
				logger.info("{} SLE interface return service received an event: END OF DATA. SIID: {}",
						SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
						service.getSiid());
			} else if (singleData instanceof LossOfSyncNotification) {
				LossOfSyncNotification lossOfSyncNotification = (LossOfSyncNotification) singleData;
				logger.trace(
						"onTransferBufferImpl() data for loop: TransferBufferData {} of {} is LossOfSyncNotification",
						counterForLogging, data.size());
				logger.info(
						"{} SLE interface return service received an event: LOSS OF FRAME SYNC. SIID: {}, time: {}, symbol-status: {}, subcarrier-status: {}, carrier-status: {}",
						SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
						service.getSiid(),
						DateTimeFormattingUtil.INSTANCE.toAMPCSDateTimeString(lossOfSyncNotification.getTime()),
						lossOfSyncNotification.getSymbolStatus(), lossOfSyncNotification.getSubcarrierStatus(),
						lossOfSyncNotification.getCarrierStatus());
			} else if (singleData instanceof TmProductionStatusNotification) {
				TmProductionStatusNotification tmProductionStatusNotification = (TmProductionStatusNotification) singleData;
				logger.trace(
						"onTransferBufferImpl() data for loop: TransferBufferData {} of {} is TmProductionStatusNotification",
						counterForLogging, data.size());
				logger.info(
						"{} SLE interface return service received an event: PRODUCTION STATUS CHANGE. SIID: {}, production-status: {}",
						SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
						service.getSiid(), tmProductionStatusNotification.getStatus());
			} else {
				logger.trace("onTransferBufferImpl() data for loop: TransferBufferData {} of {} is INDETERMINABLE",
						counterForLogging, data.size());
				logger.warn("{} SLE interface return service received an event that is INDETERMINABLE. SIID: {}",
						SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
						service.getSiid());
			}

			counterForLogging++;
		}

		logger.trace("onTransferBufferImpl() exited data for loop. {} frames to transfer.", frames.size());

		if (frames.size() > 0) {
			SLEInterfaceReturnService.INSTANCE.incrementTransferredDataCount(ZonedDateTime.now(ZoneOffset.UTC),
					frames.size());
			MessageDistributor.INSTANCE.sleReturnProviderDataFlow(
					SLEInterfaceReturnService.INSTANCE.getLastTransferDataTimeSinceLastBind(),
					SLEInterfaceReturnService.INSTANCE.getTransferredDataCountSinceLastBind());
		}

		synchronized (frameQueueLock) {

			if (frameQueue != null) {
				counterForLogging = 1;

				for (TmProductionData frame : frames) {

					try {
						logger.trace(
								"onTransferBufferImpl() frames for loop: Putting frame {} of {} into frame queue. Current queue size: {}, remaining: {}",
								counterForLogging, frames.size(), frameQueue.size(), frameQueue.remainingCapacity());
						/*
						 * Block, as in, don't return from this method until
						 * chill interface downlink is consuming. This way we
						 * throttle the return provider.
						 */
						frameQueue.put(frame);
						counterForLogging++;
					} catch (InterruptedException e) {
						logger.debug(
								"onTransferBufferImpl() frames for loop: Caught InterruptedException. Leave loop.");
						logger.warn(
								"{} SLE interface return service event handler interrupted. {} frames will be discarded locally.",
								SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
								frames.size() - counterForLogging + 1);
						Thread.currentThread().interrupt();
						break;
					}

				}

			} else {
				logger.warn(
						"{} SLE interface return service cannot transfer the new frames because chill interface downlink is unavailable. {} frames will be discarded locally.",
						SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), frames.size());
			}

		}

		logger.trace("Exiting onTransferBufferImpl()");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserRafInstanceListener#onStart(com.
	 * lsespace.sle.user.service.SLEUserRafInstance)
	 */
	@Override
	public void onStart(final SLEUserRafInstance service) {
		logger.info("{} SLE interface return service received an event: START. SIID: {}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserRafInstanceListener#onStateChange(
	 * com.lsespace.sle.user.service.SLEUserRafInstance,
	 * com.lsespace.sle.user.service.UserServiceState)
	 */
	@Override
	public void onStateChange(final SLEUserRafInstance service, final UserServiceState state) {
		logger.info("{} SLE interface return service received an event: STATE CHANGE. SIID: {}, service state: {}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
				state);
		
		// Need to capture the case where the "Peer Abort" is done from the Provider's
		// side and notify the GUI clients to change their state to UNBOUND
		if (state == UserServiceState.UNBOUND) {
			MessageDistributor.INSTANCE.sleReturnProviderStateChange(EReturnActionType.UNBIND, null,
					ZonedDateTime.now(ZoneOffset.UTC), null);
			
			/*
			 * MPCS-8683: Remote-initiated peer abort causes the state to go
			 * UNBOUND, and connection number and delivery mode need to be
			 * reinitialized.
			 */
			SLEInterfaceReturnService.INSTANCE.initializeConnectionNumberAndDeliveryMode();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserRafInstanceListener#onStatusReport(
	 * com.lsespace.sle.user.service.SLEUserRafInstance,
	 * com.lsespace.sle.user.service.RafStatusReport)
	 */
	@Override
	public void onStatusReport(final SLEUserRafInstance service, final RafStatusReport report) {
		logger.info(
				"{} SLE interface return service received an event: STATUS REPORT. SIID: {}, number-of-error-free-frames-delivered: {}, number-of-frames-delivered: {}, frame-sync-lock-status: {}, symbol-sync-lock-status: {}, subcarrier-lock-status: {}, carrier-lock-status: {}, production-status: {}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
				report.getErrorFreeFrameCounter(), report.getDeliveredFrameCounter(), report.getFrameSyncLockStatus(),
				report.getSymbolSyncLockStatus(), report.getSubcarrierLockStatus(), report.getCarrierLockStatus(),
				report.getTmProductionStatus());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lsespace.sle.user.service.SLEUserRafInstanceListener#onStop(com.
	 * lsespace.sle.user.service.SLEUserRafInstance)
	 */
	@Override
	public void onStop(final SLEUserRafInstance service) {
		logger.info("{} SLE interface return service received an event: STOP. SIID: {}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserRafInstanceListener#onTransferBuffer
	 * (com.lsespace.sle.user.service.SLEUserRafInstance, int, int,
	 * java.util.List)
	 */
	@Override
	public void onTransferBuffer(final SLEUserRafInstance service, final int numFrames, final int numGoodFrames,
			final List<TransferBufferData> data) {
		logger.trace("Entered onTransferBuffer(RAF): Calling onTransferBufferImpl()");
		onTransferBufferImpl(service, numFrames, numGoodFrames, data);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserRcfInstanceListener#onStart(com.
	 * lsespace.sle.user.service.SLEUserRcfInstance)
	 */
	@Override
	public void onStart(final SLEUserRcfInstance service) {
		logger.info("{} SLE interface return service received an event: START. SIID: {}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserRcfInstanceListener#onStateChange(
	 * com.lsespace.sle.user.service.SLEUserRcfInstance,
	 * com.lsespace.sle.user.service.UserServiceState)
	 */
	@Override
	public void onStateChange(final SLEUserRcfInstance service, final UserServiceState state) {
		logger.info("{} SLE interface return service received an event: STATE CHANGE. SIID: {}, service state: {}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
				state);
		
		// Need to capture the case where the "Peer Abort" is done from the Provider's
		// side and notify the GUI clients to change their state to UNBOUND
		if (state == UserServiceState.UNBOUND) {
			MessageDistributor.INSTANCE.sleReturnProviderStateChange(EReturnActionType.UNBIND, null,
					ZonedDateTime.now(ZoneOffset.UTC), null);
			
			/*
			 * MPCS-8683: Remote-initiated peer abort causes the state to go
			 * UNBOUND, and connection number and delivery mode need to be
			 * reinitialized.
			 */
			SLEInterfaceReturnService.INSTANCE.initializeConnectionNumberAndDeliveryMode();
		}		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserRcfInstanceListener#onStatusReport(
	 * com.lsespace.sle.user.service.SLEUserRcfInstance,
	 * com.lsespace.sle.user.service.RcfStatusReport)
	 */
	@Override
	public void onStatusReport(final SLEUserRcfInstance service, final RcfStatusReport report) {
		logger.info(
				"{} SLE interface return service received an event: STATUS REPORT. SIID: {}, number-of-frames-delivered: {}, frame-sync-lock-status: {}, symbol-sync-lock-status: {}, subcarrier-lock-status: {}, carrier-lock-status: {}, production-status: {}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid(),
				report.getDeliveredFrameCounter(), report.getFrameSyncLockStatus(), report.getSymbolSyncLockStatus(),
				report.getSubcarrierLockStatus(), report.getCarrierLockStatus(), report.getTmProductionStatus());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.lsespace.sle.user.service.SLEUserRcfInstanceListener#onStop(com.
	 * lsespace.sle.user.service.SLEUserRcfInstance)
	 */
	@Override
	public void onStop(final SLEUserRcfInstance service) {
		logger.info("{} SLE interface return service received an event: STOP. SIID: {}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), service.getSiid());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.lsespace.sle.user.service.SLEUserRcfInstanceListener#onTransferBuffer
	 * (com.lsespace.sle.user.service.SLEUserRcfInstance, int, int,
	 * java.util.List)
	 */
	@Override
	public void onTransferBuffer(final SLEUserRcfInstance service, final int numFrames, final int numGoodFrames,
			final List<TransferBufferData> data) {
		logger.trace("Entered onTransferBuffer(RCF): Calling onTransferBufferImpl()");
		onTransferBufferImpl(service, numFrames, numGoodFrames, data);
	}

	/**
	 * Set the handler's frame queue with the provided one.
	 * 
	 * @param frameQueue
	 *            The queue that the handler should use to relay frames
	 */
	public final void setFrameQueue(final BlockingQueue<TmProductionData> frameQueue) {

		synchronized (frameQueueLock) {
			this.frameQueue = frameQueue;
		}

		logger.debug("setFrameQueue(frameQueue: {})", Integer.toHexString(System.identityHashCode(frameQueue)));
	}

}
