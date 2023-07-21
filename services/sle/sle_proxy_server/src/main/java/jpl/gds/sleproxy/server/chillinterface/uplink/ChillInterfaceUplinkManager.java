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
package jpl.gds.sleproxy.server.chillinterface.uplink;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jpl.gds.sleproxy.server.chillinterface.uplink.action.EUplinkActionType;
import jpl.gds.sleproxy.server.sleinterface.fwd.SLEInterfaceForwardService;
import jpl.gds.sleproxy.server.state.EChillInterfaceUplinkState;
import jpl.gds.sleproxy.server.state.ProxyStateManager;
import jpl.gds.sleproxy.server.time.DateTimeFormattingUtil;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;

/**
 * Singleton management class for the chill interface uplink (i.e. interface
 * module for communicating with chill_up and other AMPCS telecommanding
 * applications.
 * 
 */
public enum ChillInterfaceUplinkManager {

	/**
	 * The singleton object.
	 */
	INSTANCE;

	/**
	 * Flag that indicates whether the chill interface uplink is enabled or
	 * disabled.
	 */
	private boolean enabled;

	/**
	 * The timestamp when the uplink interface's state last changed.
	 */
	private ZonedDateTime uplinkStateChangeTime;

	/**
	 * The timestamp of when the latest CLTU was received from the AMPCS
	 * telecommanding application.
	 */
	private volatile ZonedDateTime lastCLTUsReceivedTimeSinceLastEnable;

	/**
	 * A counter. Number of CLTUs received since the interface was last enabled.
	 */
	private volatile long cltusReceivedCountSinceLastEnable;

	/**
	 * Lock object for the CLTUs received time and count.
	 */
	private volatile Object cltusReceivedTimeAndCountLock;

	/**
	 * The host name of the currently connected AMPCS telecommanding
	 * application.
	 */
	private volatile String connectedHost;

	/**
	 * Queue of CLTUs. The chill interface for uplink will fill this queue and
	 * someone else will pick them up from the other side.
	 */
	private volatile BlockingQueue<byte[]> cltuQueue;

	/**
	 * Lock object for the CLTU queue.
	 */
	private volatile Object cltuQueueLock;

	/**
	 * The actual worker that does the low-level connection and data transfer
	 * from the AMPCS telecommanding application.
	 */
	private ChillInterfaceUplinkServer chillInterfaceUplinkServer;

	/**
	 * The Thread object that will run the chill interface uplink server in a
	 * different thread.
	 */
	private Thread chillInterfaceUplinkServerThread;

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ChillInterfaceUplinkManager.class);

	/**
	 * Default constructor.
	 */
	private ChillInterfaceUplinkManager() {
		enabled = false;
		cltusReceivedTimeAndCountLock = new Object();
		cltuQueueLock = new Object();
	}

	/**
	 * Initialize the chill interface uplink manager.
	 */
	public synchronized void init() {
		logger.trace("Entered init()");

		if (ProxyStateManager.INSTANCE.getChillInterfaceUplinkState() == EChillInterfaceUplinkState.ENABLED) {
			logger.debug(
					"init(): ProxyStateManager says chill interface uplink state should be enabled, so calling enable()");
			enable();
		} else {
			logger.debug(
					"init(): ProxyStateManager says chill interface uplink state should be disabled, so manager's enabled state is {}",
					enabled);
		}

		logger.trace("Exiting init()");
	}

	/**
	 * Calling this method with a non-null <code>BlockingQueue</code> will start
	 * the chill interface uplink server (if it's enabled by the user). Calling
	 * this method with a <code>null</code> value will terminate the chill
	 * interface uplink server. So in this way, the <code>BlockingQueue</code>
	 * object also serves as a signaling mechanism.
	 * 
	 * @param cltuQueue
	 *            The <code>BlockingQueue</code> object (<code>null</code> is
	 *            also accepted)
	 */
	public void setCLTUQueue(final BlockingQueue<byte[]> cltuQueue) {
		logger.debug("Entered setCLTUQueue(cltuQueue: {})", Integer.toHexString(System.identityHashCode(cltuQueue)));

		synchronized (cltuQueueLock) {
			this.cltuQueue = cltuQueue;
		}

		if (cltuQueue != null && enabled
				&& (chillInterfaceUplinkServerThread == null || !chillInterfaceUplinkServerThread.isAlive())) {
			logger.debug(
					"setCLTUQueue(): cltuQueue received is not null, uplink is enabled, but chill interface uplink server thread is not alive. So starting uplink server.");
			// Start the uplink server
			chillInterfaceUplinkServer = new ChillInterfaceUplinkServer();
			chillInterfaceUplinkServer.setCLTUQueue(cltuQueue);
			logger.debug(
					"setCLTUQueue(): Instantiated new ChillInterfaceUplinkServer, called its enable(), and set its cltuQueue to {}",
					Integer.toHexString(System.identityHashCode(cltuQueue)));

			if (chillInterfaceUplinkServerThread != null && chillInterfaceUplinkServerThread.isAlive()) {
				logger.warn(
						"{} Unexpected condition: chill interface uplink server thread is still alive. Interrupting it and starting a new one. (setCLTUQueue())",
						SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging());
				chillInterfaceUplinkServerThread.interrupt();
				/*
				 * MPCS-8702: If the uplink server is in listening mode, just
				 * interrupting will have no immediate effect. Close the
				 * listening socket so that the interrupt operations will take
				 * effect right away.
				 */
				chillInterfaceUplinkServer.stopListening();
			}

			chillInterfaceUplinkServerThread = new Thread(chillInterfaceUplinkServer);
			logger.debug("setCLTUQueue(): Instantiated new thread for ChillInterfaceUplinkServer, starting it");
			chillInterfaceUplinkServerThread.start();
		} else if (cltuQueue == null && chillInterfaceUplinkServerThread != null
				&& chillInterfaceUplinkServerThread.isAlive()) {
			logger.debug(
					"setCLTUQueue(): cltuQueue received is null and chill interface uplink server thread is alive. First, interrupt the uplink server thread.");
			/*
			 * Interrupt the thread first in case it has a lock on the CLTU
			 * queue (if it does, calling setCLTUQueue will simply block
			 * indefinitely waiting for the lock to be released)
			 */
			chillInterfaceUplinkServerThread.interrupt();
			/*
			 * MPCS-8702: If the uplink server is in listening mode, just
			 * interrupting will have no immediate effect. Close the
			 * listening socket so that the interrupt operations will take
			 * effect right away.
			 */
			chillInterfaceUplinkServer.stopListening();
			logger.debug("setCLTUQueue(): Setting uplink server's CLTU queue to null");
			chillInterfaceUplinkServer.setCLTUQueue(null);
		}

		logger.debug("Exiting setCLTUQueue()");
	}

	/**
	 * Enable the chill interface for uplink. If a valid CLTU queue is in place,
	 * this means that there's a listener on the other side (handling class for
	 * the SLE forward service), so start the uplink server right away (we're
	 * ready to receive AMPCS telecommanding applications' incoming
	 * connections).
	 * 
	 * @throws IllegalStateException
	 *             Thrown when chill interface for uplink is already enabled
	 */
	public synchronized void enable() throws IllegalStateException {
		logger.debug("Entered enable()");

		if (enabled) {
			logger.debug("enable(): Enabled flag is already true. Aborting enable().");
			throw new IllegalStateException("chill interface uplink is already enabled");
		}

		enabled = true;
		logger.debug("enable(): Setting proxy state's chill interface uplink state to ENABLED");
		ProxyStateManager.INSTANCE.setChillInterfaceUplinkState(EChillInterfaceUplinkState.ENABLED);
		uplinkStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
		// Notify clients of uplink state change to Enabled
		MessageDistributor.INSTANCE.chillUpStateChangeAction(EUplinkActionType.ENABLE, uplinkStateChangeTime);
		logger.info("{} chill interface uplink enabled at {}",
				SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
				uplinkStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
		lastCLTUsReceivedTimeSinceLastEnable = null;
		cltusReceivedCountSinceLastEnable = 0;

		/*
		 * Uplink server is started only when the SLE interface forward service
		 * is ready to process the incoming CLTUs. This readiness is indicated
		 * by whether the cltuQueue is null or non-null.
		 */
		if (cltuQueue != null) {
			logger.info(
					"{} SLE interface forward service is already started so proceeding with starting chill interface uplink server",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging());

			// Start the uplink server
			logger.debug("enable(): Instantiating new ChillInterfaceUplinkServer, setting its CLTU queue to {}",
					Integer.toHexString(System.identityHashCode(cltuQueue)));
			chillInterfaceUplinkServer = new ChillInterfaceUplinkServer();
			chillInterfaceUplinkServer.setCLTUQueue(cltuQueue);

			if (chillInterfaceUplinkServerThread != null && chillInterfaceUplinkServerThread.isAlive()) {
				logger.warn(
						"{} Unexpected condition: chill interface uplink server thread is still alive. Interrupting it and starting a new one. (enable())",
						SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging());
				chillInterfaceUplinkServerThread.interrupt();
				/*
				 * MPCS-8702: If the uplink server is in listening mode, just
				 * interrupting will have no immediate effect. Close the
				 * listening socket so that the interrupt operations will take
				 * effect right away.
				 */
				chillInterfaceUplinkServer.stopListening();
			}

			chillInterfaceUplinkServerThread = new Thread(chillInterfaceUplinkServer);
			logger.debug("enable(): Starting a new thread for ChillInterfaceUplinkServer");
			chillInterfaceUplinkServerThread.start();
		} else {
			logger.info("{} SLE interface forward service is not started so chill interface uplink server remains down",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging());
		}

		logger.debug("Exiting enable()");
	}

	/**
	 * Disable the chill interface for uplink. This will also terminate the
	 * uplink server, as well as any connections that it has with an AMPCS
	 * telecommanding application.
	 * 
	 * @throws IllegalStateException
	 *             Thrown when chill interface for uplink is already disabled
	 */
	public synchronized void disable() throws IllegalStateException {
		logger.debug("Entered disable()");

		if (!enabled) {
			logger.debug("disable(): Enabled flag is already false. Aborting disable().");
			throw new IllegalStateException("chill interface uplink is already disabled");
		}

		enabled = false;
		logger.debug("disable(): Setting proxy state's chill interface uplink state to DISABLED");
		ProxyStateManager.INSTANCE.setChillInterfaceUplinkState(EChillInterfaceUplinkState.DISABLED);
		uplinkStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
		// Notify clients of uplink state change to Disabled
		MessageDistributor.INSTANCE.chillUpStateChangeAction(EUplinkActionType.DISABLE, uplinkStateChangeTime);
		logger.info("{} chill interface uplink disabled at {}",
				SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging(),
				uplinkStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));

		if (chillInterfaceUplinkServer != null) {
			logger.info("{} Stopping the chill interface uplink server",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging());
			chillInterfaceUplinkServer = null;
			logger.debug("disable(): Interrupting ChillInterfaceUplinkServer thread");
			chillInterfaceUplinkServerThread.interrupt();
			/*
			 * MPCS-8702: If the uplink server is in listening mode, just
			 * interrupting will have no immediate effect. Close the
			 * listening socket so that the interrupt operations will take
			 * effect right away.
			 */
			chillInterfaceUplinkServer.stopListening();
			chillInterfaceUplinkServerThread = null;
		} else {
			logger.info("{} Chill interface uplink server is already down",
					SLEInterfaceForwardService.INSTANCE.getCurrentConnectionNumberStringForLogging());
		}

		logger.debug("Exiting disable()");
	}

	/**
	 * Return the enabled/disabled condition of the chill interface uplink
	 * manager. Note: This does not indicate whether an AMPCS telecommanding
	 * application is currently connected or not.
	 * 
	 * @return true if uplink is enabled, false if disabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Get the timestamp of when the uplink manager's state last changed.
	 * 
	 * @return Uplink state change time
	 */
	public ZonedDateTime getUplinkStateChangeTime() {
		return uplinkStateChangeTime;
	}

	/**
	 * Increment the CLTUs received counter by the amount specified, and
	 * simultaneously update the timestamp of when the CLTU(s) was last received
	 * with the specified timestamp.
	 * 
	 * @param cltusReceivedCount
	 *            Number of CLTUs to add to the running counter
	 * @param cltusReceivedTime
	 *            Timestamp of when the latest CLTUs were received
	 */
	public void incrementCLTUsReceivedCount(final int cltusReceivedCount, final ZonedDateTime cltusReceivedTime) {

		synchronized (cltusReceivedTimeAndCountLock) {
			lastCLTUsReceivedTimeSinceLastEnable = cltusReceivedTime;
			cltusReceivedCountSinceLastEnable += cltusReceivedCount;
			logger.trace(
					"incrementCLTUsReceivedCount(): New values are lastCLTUsReceivedTimeSinceLastEnable: {}, cltusReceivedCountSinceLastEnable: {}",
					lastCLTUsReceivedTimeSinceLastEnable
							.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()),
					cltusReceivedCountSinceLastEnable);
		}

	}

	/**
	 * Get the timestamp of when the latest CLTUs were received since the uplink
	 * manager was last enabled.
	 * 
	 * @return Timestamp of when the CLTUs were last received since uplink
	 *         manager was enabled
	 */
	public ZonedDateTime getLastCLTUsReceivedTimeSinceLastEnable() {
		return lastCLTUsReceivedTimeSinceLastEnable;
	}

	/**
	 * Get the CLTUs received count since the uplink manager was last enabled.
	 * 
	 * @return CLTUs received count since the last time the uplink manager was
	 *         enabled
	 */
	public long getCLTUsReceivedCountSinceLastEnable() {
		return cltusReceivedCountSinceLastEnable;
	}

	/**
	 * Update the host name of the connected AMPCS telecommanding application
	 * with the specified one.
	 * 
	 * @param connectedHost
	 *            The host name of the AMPCS telecommanding application that has
	 *            connected
	 */
	public void setConnectedHost(final String connectedHost) {
		this.connectedHost = connectedHost;
	}

	/**
	 * Get the host name of the connected AMPCS telecommanding application.
	 * 
	 * @return Host name of the connected AMPCS telecommanding application. null
	 *         indicates no application is connected.
	 */
	public String getConnectedHost() {
		return connectedHost;
	}

}