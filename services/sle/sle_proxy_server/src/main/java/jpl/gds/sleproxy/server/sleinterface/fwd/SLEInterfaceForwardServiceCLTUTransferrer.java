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
import com.lsespace.sle.user.service.data.FcltuTransferDataResponse;
import com.lsespace.sle.user.util.concurrent.OperationFuture;

import jpl.gds.sleproxy.server.sleinterface.internal.config.SLEInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;

/**
 * The worker class that reads CLTUs from a shared queue and forwards it to the
 * SLE forward service provider.
 * 
 */
public class SLEInterfaceForwardServiceCLTUTransferrer implements Runnable {

	/**
	 * LSE Space's FCLTU service instance.
	 */
	private final SLEUserFcltuInstance userFcltuServiceInstance;

	/**
	 * ID to mark the next CLTU with when forwarding to the SLE forward service
	 * provider.
	 */
	private long nextCLTUID;

	/**
	 * The queue to read for new CLTUs to transfer.
	 */
	private final BlockingQueue<byte[]> cltuQueue;

	/**
	 * A flag to set when transferring a CLTU. It's required as part of the SLE
	 * FCLTU interface.
	 */
	private final boolean transferDataReportFlag;

	/**
	 * A flag that will be set when this transferrer should just abandon the
	 * remaining CLTUs in the queue and not attempt to transfer them, i.e. when
	 * the sending pipe has already been broken.
	 */
	private boolean doNotTransferRemainingCLTUs;

	/**
	 * Timeout threshold for CLTU transfer.
	 */
	private final long transferDataTimeoutMillis;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceForwardServiceCLTUTransferrer.class);

	/**
	 * Constructor.
	 * 
	 * @param userFcltuServiceInstance
	 *            The SLE FCLTU service instance that is the basis of this
	 *            transferrer's operations
	 * @param startingCLTUID
	 *            The ID number to use for the next CLTU that'll be transferred
	 * @param cltuQueue
	 *            The queue to monitor for new CLTUs to transfer
	 */
	SLEInterfaceForwardServiceCLTUTransferrer(final SLEUserFcltuInstance userFcltuServiceInstance,
			final long startingCLTUID, final BlockingQueue<byte[]> cltuQueue) {
		this.userFcltuServiceInstance = userFcltuServiceInstance;
		this.nextCLTUID = startingCLTUID;
		this.cltuQueue = cltuQueue;
		this.transferDataReportFlag = SLEInterfaceInternalConfigManager.INSTANCE.getForwardTransferDataReportFlag();
		this.doNotTransferRemainingCLTUs = false;
		this.transferDataTimeoutMillis = SLEInterfaceInternalConfigManager.INSTANCE
				.getForwardTransferDataTimeoutMillis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public final void run() {
		logger.debug("Entering run(). Entering while loop (exits when thread interrupted).");

		while (!(Thread.currentThread().isInterrupted())) {

			try {
				transfer(cltuQueue.take());
			} catch (InterruptedException ie) {
				logger.debug("run() while loop: Caught InterruptedException");
				Thread.currentThread().interrupt();
			}

		}

		logger.debug("run(): Exited while loop");

		/*
		 * Thread is interrupted so we'll be exiting run(). But handle all
		 * remaining CLTUs in the queue before we go.
		 */
		final List<byte[]> remainingCLTUs = new LinkedList<>();
		cltuQueue.drainTo(remainingCLTUs);

		if (doNotTransferRemainingCLTUs) {
			logger.warn(
					"{} CLTU transferrer is now stopping. Abandoning {} CLTUs left in the queue because TRANSFER DATA is failing.",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
					remainingCLTUs.size());
		} else {
			logger.info(
					"{} CLTU transferrer is now stopping. Draining the CLTU queue and transferring the remaining {} CLTUs.",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
					remainingCLTUs.size());

			int remainingCLTUsTransferred = 0;
			
			for (byte[] cltu : remainingCLTUs) {
				transfer(cltu);
				remainingCLTUsTransferred++;
				
				if (doNotTransferRemainingCLTUs) {
					logger.warn(
							"{} TRANSFER DATA is failing. Abandoning {} CLTUs left in the queue.",
							SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
							remainingCLTUs.size() - remainingCLTUsTransferred);
					break;
				}

			}

		}

		logger.debug("Exiting run()");
	}

	/**
	 * Transfer the provided CLTU data to the SLE forward service provider.
	 * 
	 * @param cltu
	 *            The CLTU data to transfer
	 */
	private void transfer(final byte[] cltu) {
		logger.trace("Entered transfer(cltu: {})", cltu);

		// Invoke the transfer data request
		OperationFuture<FcltuTransferDataResponse> futureTransferDataResult = null;

		logger.trace(
				"transfer(): Invoking the TRANSFER DATA request (cltu-identification: {}, report: {}, delay-time: {})",
				nextCLTUID, transferDataReportFlag, 0);
		// Immediate radiation
		futureTransferDataResult = userFcltuServiceInstance.transferData(nextCLTUID, cltu, transferDataReportFlag, 0);

		/*
		 * Await for the transfer data operation to be completed. The wait time
		 * before timeout should be greater than the response timeout.
		 */
		futureTransferDataResult.awaitUninterruptibly(transferDataTimeoutMillis);

		if (futureTransferDataResult.isSuccess()) {
			// Transfer data succeeded
			SLEInterfaceForwardService.INSTANCE.incrementTransferredDataCount(ZonedDateTime.now(ZoneOffset.UTC));
			MessageDistributor.INSTANCE.sleForwardProviderDataFlow(
					SLEInterfaceForwardService.INSTANCE.getLastTransferDataTimeSinceLastBind(),
					SLEInterfaceForwardService.INSTANCE.getTransferredDataCountSinceLastBind());
			logger.info("{} SLE interface forward service TRANSFER DATA (cltu-identification: {}) SUCCEEDED",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), nextCLTUID);
			nextCLTUID++;
		} else {
			// Transfer data failed. Log reason.
			logger.error("{} SLE interface forward service TRANSFER DATA (cltu-identification: {}) FAILED",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(), nextCLTUID,
					futureTransferDataResult.cause());
			logger.warn(
					"{} Triggering stop of CLTU transferrer. To resume CLTU transfer, SLE interface forward service needs another START.",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging());
			doNotTransferRemainingCLTUs = true;
			Thread.currentThread().interrupt();
		}

		logger.trace("Exiting transfer()");
	}

}