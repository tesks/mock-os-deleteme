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
package jpl.gds.sleproxy.server.chillinterface.downlink;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lsespace.sle.user.service.data.TmProductionData;

import jpl.gds.sleproxy.server.chillinterface.downlink.action.EDownlinkActionType;
import jpl.gds.sleproxy.server.chillinterface.internal.config.ChillInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.sleinterface.rtn.SLEInterfaceReturnService;
import jpl.gds.sleproxy.server.time.DateTimeFormattingUtil;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;

/**
 * Manages the client interface to chill_down and keeps states related to chill
 * interface downlink connection.
 * 
 */
public enum ChillInterfaceDownlinkManager {

	/**
	 * The singleton instance.
	 */
	INSTANCE;

	/**
	 * Holds the time when the chill interface downlink connection last changed
	 * state.
	 */
	private ZonedDateTime downlinkStateChangeTime;

	/**
	 * Holds the time when the latest frame was transferred to chill_down.
	 */
	private volatile ZonedDateTime lastFramesTransferredTimeSinceLastConnection;

	/**
	 * Keeps the count of transferred frames since the last connectino was made
	 * to a chill_down.
	 */
	private volatile long framesTransferredCountSinceLastConnection;

	/**
	 * Lock object for the frame transferred time and count.
	 */
	private volatile Object framesTransferredTimeAndCountLock;

	/**
	 * The <code>BlockingQueue</code> that serves as a pipe of transfer frame
	 * data from the SLE return service to the chill interface downlink client.
	 */
	private BlockingQueue<TmProductionData> frameQueue;

	/**
	 * The chill interface downlink client that needs to be managed.
	 */
	private ChillInterfaceDownlinkClient chillInterfaceDownlinkClient;

	/**
	 * The thread object that runs the chill interface downlink client
	 * concurrently from this thread.
	 */
	private Thread chillInterfaceDownlinkClientThread;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ChillInterfaceDownlinkManager.class);

	/**
	 * Default constructor.
	 */
	private ChillInterfaceDownlinkManager() {
		framesTransferredTimeAndCountLock = new Object();
	}

	/**
	 * Trigger the chill interface downlink manager to create a downlink client
	 * that will in turn establish a socket connection to chill_down.
	 * 
	 * @throws IllegalStateException
	 *             Thrown when an unexpected error is encountered during the
	 *             connection establishment
	 */
	public synchronized void connect() throws IllegalStateException {
		logger.debug("Entered connect()");

		if (isConnected()) {
			logger.debug("connect(): chillInterfaceDownlinkClientThread is alive. Aborting connect().");
			throw new IllegalStateException("chill interface downlink is already connected");
		}

		frameQueue = new LinkedBlockingQueue<>(
				ChillInterfaceInternalConfigManager.INSTANCE.getDownlinkFramesBufferCapacity());
		logger.debug(
				"connect(): Instantiated new frameQueue {}, instantiating new ChillInterfaceDownlinkClient with it",
				Integer.toHexString(System.identityHashCode(frameQueue)));
		chillInterfaceDownlinkClient = new ChillInterfaceDownlinkClient(frameQueue);

		try {
			logger.debug("connect(): Calling ChillInterfaceDownlinkClient's connect()");
			chillInterfaceDownlinkClient.connect();
		} catch (UnknownHostException uhe) {
			logger.debug("connect(): Caught UnknownHostException", uhe);
			throw new IllegalStateException("Cannot connect to unknown chill_down host", uhe);
		} catch (IOException ie) {
			logger.debug("connect(): Caught IOException", ie);
			throw new IllegalStateException(
					"Cannot establish socket and/or data input stream connection with chill_down", ie);
		}

		downlinkStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
		logger.info("{} chill interface downlink connected at {}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
				downlinkStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
		lastFramesTransferredTimeSinceLastConnection = null;
		framesTransferredCountSinceLastConnection = 0;

		// Notify clients of chill_down connection state as connected
		MessageDistributor.INSTANCE.chillDownStateChangeAction(EDownlinkActionType.CONNECT, downlinkStateChangeTime);

		// Start the chill_down connecting client
		if (chillInterfaceDownlinkClientThread != null && chillInterfaceDownlinkClientThread.isAlive()) {
			logger.warn(
					"{} Unexpected condition: chill interface downlink client thread is still alive. Interrupting it and starting a new one.",
					SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging());
			chillInterfaceDownlinkClientThread.interrupt();
		}

		chillInterfaceDownlinkClientThread = new Thread(chillInterfaceDownlinkClient);
		logger.debug("connect(): Starting a new thread for ChillInterfaceDownlinkClient");
		chillInterfaceDownlinkClientThread.start();
		logger.debug("connect(): Setting SLEInterfaceReturnService's frame queue to {}",
				Integer.toHexString(System.identityHashCode(frameQueue)));
		SLEInterfaceReturnService.INSTANCE.setFrameQueue(frameQueue);
		logger.debug("Exiting connect()");
	}

	/**
	 * Trigger the chill interface downlink manager to close the socket
	 * connection to chill_down and shut down the downlink client, also stopping
	 * the thread that runs the client.
	 * 
	 * @throws IllegalStateException
	 *             Thrown when an unexpected error is encountered during the
	 *             client shutdown process
	 */
	public synchronized void disconnect() throws IllegalStateException {
		logger.debug("Entered disconnect()");

		if (!isConnected()) {
			logger.debug("disconnect(): chillInterfaceDownlinkClientThread is not alive. Aborting disconnect().");
			throw new IllegalStateException("chill interface downlink is already disconnected");
		}

		logger.debug("disconnect(): Setting SLEInterfaceReturnService's frame queue to null");
		SLEInterfaceReturnService.INSTANCE.setFrameQueue(null);
		downlinkStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
		// Notify clients of chill_down connection state as disconnected
		MessageDistributor.INSTANCE.chillDownStateChangeAction(EDownlinkActionType.DISCONNECT, downlinkStateChangeTime);
		logger.info("{} chill interface downlink disconnected at {}",
				SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
				downlinkStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
		logger.debug("disconnect(): Interrupting chillInterfaceDownlinkClientThread");
		chillInterfaceDownlinkClientThread.interrupt();

		/*
		 * Wait a little bit and see if the client thread really stops. If it
		 * remains alive, it means it's blocked on an I/O operation and
		 * interrupting it had no effect.
		 */
		try {
			logger.debug(
					"disconnect(): Sleeping {} milliseconds to wait for chillInterfaceDownlinkClientThread to stop",
					ChillInterfaceInternalConfigManager.INSTANCE.getDownlinkClientInterruptWaitMillis());
			Thread.sleep(ChillInterfaceInternalConfigManager.INSTANCE.getDownlinkClientInterruptWaitMillis());
		} catch (InterruptedException ie) {
			logger.debug(
					"disconnect(): Sleep to wait for chillInterfaceDownlinkClientThread to stop caused InterruptedException",
					ie);
		}

		if (isConnected()) {
			logger.debug(
					"disconnect(): chillInterfaceDownlinkClientThread didn't stop so call chillInterfaceDownlinkClient.closeSocket()");

			try {
				chillInterfaceDownlinkClient.closeSocket();
			} catch (IOException ie) {
				logger.error("{} chill interface downlink client close socket call caused exception",
						SLEInterfaceReturnService.INSTANCE.getCurrentConnectionNumberStringForLogging(), ie);
			}

		} else {
			logger.debug(
					"disconnect(): chillInterfaceDownlinkClientThread is no longer alive. No further action needed.");
		}

		chillInterfaceDownlinkClient = null;
	}

	/**
	 * Check to see if the chill interface downlink connection is connected or
	 * not. If the chill interface downlink client thread is not null and
	 * returns true for isAlive(), then this method returns true.
	 * 
	 * @return true if connected, false if not
	 */
	public boolean isConnected() {
		return chillInterfaceDownlinkClientThread != null && chillInterfaceDownlinkClientThread.isAlive();
	}

	/**
	 * Get the downlink state change time.
	 * 
	 * @return the downlinkStateChangeTime
	 */
	public ZonedDateTime getDownlinkStateChangeTime() {
		return downlinkStateChangeTime;
	}

	/**
	 * Atomically update the last frame transferred time and the count of frames
	 * transferred (increment by one).
	 * 
	 * @param framesTransferredTime
	 *            The time when the lastest frame was transferred
	 */
	public void incrementFramesTransferredCount(final ZonedDateTime framesTransferredTime) {

		synchronized (framesTransferredTimeAndCountLock) {
			lastFramesTransferredTimeSinceLastConnection = framesTransferredTime;
			framesTransferredCountSinceLastConnection++;
			logger.trace(
					"incrementFramesTransferredCount(): New values are lastFramesTransferredTimeSinceLastConnection: {}, framesTransferredCountSinceLastConnection: {}",
					lastFramesTransferredTimeSinceLastConnection
							.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()),
					framesTransferredCountSinceLastConnection);
		}

	}

	/**
	 * Get the time when the latest frame was transferred, since last connection
	 * to chill_down.
	 * 
	 * @return the lastFramesTransferredTimeSinceLastConnection
	 */
	public ZonedDateTime getLastFramesTransferredTimeSinceLastConnection() {
		return lastFramesTransferredTimeSinceLastConnection;
	}

	/**
	 * @return the framesTransferredCountSinceLastConnection
	 */
	public long getFramesTransferredCountSinceLastConnection() {
		return framesTransferredCountSinceLastConnection;
	}

}