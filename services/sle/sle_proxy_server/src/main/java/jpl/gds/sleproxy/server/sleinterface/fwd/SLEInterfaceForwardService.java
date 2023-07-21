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

import com.lsespace.sle.user.SLEUserServiceAkkaFactory;
import com.lsespace.sle.user.SLEUserServiceFactory;
import com.lsespace.sle.user.proxy.isp1.AuthenticationMode;
import com.lsespace.sle.user.proxy.isp1.ISP1SLEUserProxyAkkaFactory;
import com.lsespace.sle.user.proxy.isp1.ISP1SLEUserProxyFactory;
import com.lsespace.sle.user.service.FcltuGetParameterResponse;
import com.lsespace.sle.user.service.FcltuParamName;
import com.lsespace.sle.user.service.ForwardServiceDeliveryMode;
import com.lsespace.sle.user.service.SLEUserFcltuInstance;
import com.lsespace.sle.user.service.UnbindReason;
import com.lsespace.sle.user.service.UserServiceState;
import com.lsespace.sle.user.service.data.FcltuAsyncNotificationType;
import com.lsespace.sle.user.util.concurrent.OperationFuture;
import jpl.gds.sleproxy.server.chillinterface.internal.config.ChillInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.chillinterface.uplink.ChillInterfaceUplinkManager;
import jpl.gds.sleproxy.server.sleinterface.AutoBindSleInstanceFactory;
import jpl.gds.sleproxy.server.sleinterface.AutoBindSleServiceInstance;
import jpl.gds.sleproxy.server.sleinterface.fwd.action.EForwardActionType;
import jpl.gds.sleproxy.server.sleinterface.internal.config.ESLEInterfaceForwardThrowEventScheme;
import jpl.gds.sleproxy.server.sleinterface.internal.config.SLEInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.sleinterface.profile.ISLEInterfaceProfile;
import jpl.gds.sleproxy.server.sleinterface.profile.ProviderHost;
import jpl.gds.sleproxy.server.state.ProxyStateManager;
import jpl.gds.sleproxy.server.time.DateTimeFormattingUtil;
import jpl.gds.sleproxy.server.websocket.MessageDistributor;
import jpl.gds.sleproxy.server.websocket.SLEProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This singleton is the central point of all capabilities and operations
 * related to the SLE forward service.
 *
 */
public enum SLEInterfaceForwardService {

	/**
	 * The singleton object.
	 */
	INSTANCE;

	/**
	 * LSE Space's ISP1 proxy factory.
	 */
	private final ISP1SLEUserProxyFactory proxyFactory;

	/**
	 * LSE Space's SLE user service factory.
	 */
	private final SLEUserServiceFactory serviceFactory;

	/**
	 * Thread object to run the asynchronous events handler. The handler will
	 * process the events received from the SLE forward service.
	 */
	private Thread eventsHandlingThread;

	/**
	 * Thread object to run the throw event serializer. The serializer will
	 * process the throw events sequentially.
	 */
	private Thread throwEventSerializerThread;

	/**
	 * Thread object to run the CLTUs transferrer in a separate thread.
	 */
	private Thread cltusTransferringThread;

	/**
	 * LSE Space's FCLTU service instance.
	 */
	private SLEUserFcltuInstance userFcltuServiceInstance;

	/**
	 * The name of the SLE interface profile that was used to do the latest
	 * BIND.
	 */
	private String lastBoundProfileName;

	/**
	 * The timestamp of when the SLE forward service last changed states (e.g.
	 * BIND, START, etc.).
	 */
	private ZonedDateTime forwardServiceStateChangeTime;

	/**
	 * The 'F' connection number counter.
	 */
	private volatile long currentConnectionNumber;

	/**
	 * The profile name of the forward service that the last BIND was attempted
	 * with.
	 */
	private volatile String lastSLEForwardServiceProfileName;

	/**
	 * Timestamp of the last time that a CLTU was forwarded to the SLE forward
	 * service. Resets at every new BIND.
	 */
	private volatile ZonedDateTime lastTransferDataTimeSinceLastBind;

	/**
	 * Counter to keep track of the number of CLTUs forwarded to the SLE forward
	 * service. Resets at every new BIND.
	 */
	private volatile long transferredDataCountSinceLastBind;

	/**
	 * Lock object for the CLTUs transfer time and count.
	 */
	private volatile Object transferDataTimeAndCountLock;

	/**
	 * Delivery mode of the SLE return service.
	 */
	private volatile ForwardServiceDeliveryMode forwardServiceDeliveryMode;

	/**
	 * Blocking queue to insert new throw events. The
	 * SLEInterfaceForwardServiceThrowEventSerializer will then read from this
	 * queue to send throw events.
	 */
	private BlockingQueue<SLEInterfaceForwardServiceThrowEvent> throwEventQueue;

	/**
	 * The string constant that should precede the bit-rate value in the DSN's
	 * throw event qualifier for event ID 4.
	 */
	private static final String DSN_THROW_EVENT_BITRATE_PREFIX = "br ";

	/**
	 * The string constant that should precede the mod-index value (but trail
	 * the bit-rate value) in the DSN's throw event qualifier for event ID 4.
	 */
	private static final String DSN_THROW_EVENT_MODINDEX_PREFIX = " mi ";
	/**
	 * Saves the bit rate from the last accepted throw event that specified it.
	 */
	private String lastAcceptedBitrate;

	/**
	 * Saves the command modulation state from the last accepted throw event
	 * that specified it.
	 */
	private String lastAcceptedCommandModState;

	/**
	 * Saves the range modulation state from the last accepted throw event that
	 * specified it.
	 */
	private String lastAcceptedRangeModState;

	private MessageDistributor messageDistributor;

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceForwardService.class);

	/**
	 * Default constructor.
	 */
	SLEInterfaceForwardService() {

		// Create the Proxy Factory
		proxyFactory = new ISP1SLEUserProxyAkkaFactory();

		// Create the Service Factory
		serviceFactory = new SLEUserServiceAkkaFactory();

		messageDistributor = MessageDistributor.INSTANCE;

		initializeConnectionNumberAndDeliveryMode();
		lastSLEForwardServiceProfileName = ProxyStateManager.INSTANCE.getLastSLEForwardServiceProfileName();
		transferDataTimeAndCountLock = new Object();
	}

	/**
	 * BIND to a SLE forward service provider.
	 *
	 * @param profile the SLE profile
	 * @throws Throwable Thrown when an unexpected error occurs during the BIND
	 */
	public synchronized void bind(final ISLEInterfaceProfile profile) throws Throwable {

		// Profile name of the SLE forward service to bind to
		final String             profileName                = profile.getProfileName();
		// Version of the SLE standard to use
		final int                serviceVersion             = SLEInterfaceInternalConfigManager.INSTANCE.getForwardServiceVersion();
		// Name of the SLE forward service provider
		final String             providerName               = profile.getProviderName();
		// Host names and ports of the SLE forward service provider
		final List<ProviderHost> providerHosts              = profile.getProviderHosts();
		// Authentication mode of the SLE forward service provider
		final AuthenticationMode providerAuthenticationMode = profile.getProviderAuthenticationMode();
		// SIID of the SLE forward service
		final String             serviceInstanceID          = profile.getServiceInstanceID();
		// User name for the SLE forward service
		final String             userName                   = profile.getUserName();
		// User authentication mode of the SLE forward service
		final AuthenticationMode userAuthenticationMode     = profile.getUserAuthenticationMode();

		final StringBuilder sb = new StringBuilder();
		int i = 1;
		for (final ProviderHost host : providerHosts) {
			sb.append("provider host ");
			sb.append(i++);
			sb.append(":");
			sb.append(host.getHostName());
			sb.append(":");
			sb.append(host.getPort());
			if (i <= providerHosts.size()) {
				sb.append(", ");
			}
		}

		logger.info(
				"Initiating SLE interface forward service BIND. Profile name: {}, service version: {}, provider name: {}, {}, provider password: <masked>, provider authentication mode: {}, service instance ID: {}, user name: {}, user password: <masked>, user authentication mode: {}",
				profileName, serviceVersion, providerName, sb.toString(), providerAuthenticationMode,
				serviceInstanceID, userName, userAuthenticationMode);

		lastSLEForwardServiceProfileName = profileName;
		ProxyStateManager.INSTANCE.setLastSLEForwardServiceProfileName(lastSLEForwardServiceProfileName);

		if (userFcltuServiceInstance != null && userFcltuServiceInstance.getState() != UserServiceState.UNBOUND) {
			logger.error(
					"{} SLE interface forward service BIND FAILED. Service already bound. Current service instance state: {}",
					getCurrentConnectionNumberStringForLogging(), userFcltuServiceInstance.getState());
			logger.debug("bind(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface forward service BIND is not allowed because already bound. Current service instance state: "
							+ userFcltuServiceInstance.getState());
		}

		AutoBindSleServiceInstance<SLEUserFcltuInstance> autoBindSLEUserFcltuInstance = AutoBindSleInstanceFactory
				.getFcltuInstance(proxyFactory, serviceFactory, profile);

		logger.debug("bind(): Invoking the BIND request");
		// Invoke the bind request
		autoBindSLEUserFcltuInstance.bind(SLEInterfaceInternalConfigManager.INSTANCE.getForwardBindUnbindTimeoutMillis());

		if (autoBindSLEUserFcltuInstance.isSuccessfullyBound()) {
			// Bind succeeded
			userFcltuServiceInstance = autoBindSLEUserFcltuInstance.getSuccessfullyBoundInstance();

			forwardServiceStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
			currentConnectionNumber = ProxyStateManager.INSTANCE.getNewSLEForwardServiceConnectionNumber();
			messageDistributor.sleForwardProviderStateChange(EForwardActionType.BIND, profileName,
					forwardServiceStateChangeTime, currentConnectionNumber);
			logger.info("{} SLE interface forward service BIND SUCCEEDED at {}",
					getCurrentConnectionNumberStringForLogging(),
					forwardServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
			lastBoundProfileName = profileName;
			lastTransferDataTimeSinceLastBind = null;
			transferredDataCountSinceLastBind = 0;
		} else {
			// Bind failed. Throw reason.
			final Map<ProviderHost, Throwable> failureCauses = autoBindSLEUserFcltuInstance.getFailureCauses();
			logger.debug("bind(): Throwing the Throwable to abort this method");
			final StringBuilder builder = new StringBuilder("SLE interface forward service BIND failed to all defined hosts. ");
			failureCauses.forEach((h, c) -> builder.append("[").append(h.toString()).append("] ").append(c.getMessage()).append(" "));
			throw new Exception(builder.toString());
		}

		/*
		 * Create the blocking queue for receiving and processing
		 * SLEUserFcltuInstance events
		 */
		BlockingQueue<SLEInterfaceForwardServiceEvent> eventQueue = new LinkedBlockingQueue<>();
		logger.debug("bind(): Instantiated new FCLTU event queue {}",
				Integer.toHexString(System.identityHashCode(eventQueue)));

		/*
		 * Create the blocking queue for pushing clearance confirmations of
		 * throw events
		 */
		BlockingQueue<SLEInterfaceForwardServiceThrowEventClearanceNotification> throwEventClearedConfirmationQueue = new LinkedBlockingQueue<>();
		logger.debug("bind(): Instantiated new FCLTU throw event clearance confirmation queue {}",
				Integer.toHexString(System.identityHashCode(throwEventClearedConfirmationQueue)));

		// Create the thread that will handle incoming service events
		if (eventsHandlingThread != null && eventsHandlingThread.isAlive()) {
			logger.warn(
					"Unexpected condition: SLE interface forward service event handling thread is still alive. Interrupting it and starting a new one.");
			eventsHandlingThread.interrupt();
		}

		eventsHandlingThread = new Thread(
				new SLEInterfaceForwardServiceEventHandler(eventQueue, throwEventClearedConfirmationQueue));

		logger.debug("bind(): Starting the FCLTU event queue handling thread");
		/*
		 * Add listener for events. Events should be received and queued, and
		 * processed by a separate thread.
		 */
		eventsHandlingThread.start();
		logger.debug("bind(): Adding a new event listener to the FCLTU service instance");
		userFcltuServiceInstance.addListener(new SLEInterfaceForwardServiceListener(eventQueue));

		// Get the delivery mode
		OperationFuture<FcltuGetParameterResponse> futureGetParameterResult = null;

		// Invoke the get parameter request
		try {
			logger.debug("bind(): Invoking GET PARAMETER request for delivery-mode");
			futureGetParameterResult = userFcltuServiceInstance.getFcltuParameter(FcltuParamName.DELIVERY_MODE);

			/*
			 * Await for the get parameter operation to be completed. The wait
			 * time before timeout should be greater than the response timeout.
			 */
			futureGetParameterResult.awaitUninterruptibly(
					SLEInterfaceInternalConfigManager.INSTANCE.getForwardGetParameterTimeoutMillis());

			if (futureGetParameterResult.isSuccess()) {
				logger.debug("bind(): GET PARAMETER of delivery-mode SUCCEEDED");
				FcltuGetParameterResponse getParameterResponse = futureGetParameterResult.result();
				forwardServiceDeliveryMode = getParameterResponse.getDeliveryMode();
				// forwardServiceDeliveryMode now has the delivery mode value
				logger.info("{} SLE interface forward service GET PARAMETER of delivery-mode SUCCEEDED: {}",
						getCurrentConnectionNumberStringForLogging(), forwardServiceDeliveryMode);
				// notify web clients of delivery mode change
				messageDistributor.sleDeliveryModeChange(SLEProviderType.FORWARD,
						forwardServiceDeliveryMode != null ? forwardServiceDeliveryMode.toString() : "");

			} else {
				// Get parameter failed. Log the reason.
				logger.warn("{} SLE interface forward service GET PARAMETER of delivery-mode FAILED",
						getCurrentConnectionNumberStringForLogging(), futureGetParameterResult.cause());
			}

		} catch (Exception e) {
			logger.warn("{} SLE interface forward service GET PARAMETER of delivery-mode generated an exception",
					getCurrentConnectionNumberStringForLogging(), e);
		}

		logger.info("{} Initiating SLE interface forward service SCHEDULE STATUS REPORT. Reporting cycle: {} seconds",
				getCurrentConnectionNumberStringForLogging(),
				SLEInterfaceInternalConfigManager.INSTANCE.getForwardScheduleStatusReportReportingCycleSeconds());
		// Invoke the schedule status report request
		OperationFuture<Void> futureStatusReportRequestResult = userFcltuServiceInstance.statusReportRequest(
				SLEInterfaceInternalConfigManager.INSTANCE.getForwardScheduleStatusReportReportingCycleSeconds());

		/*
		 * Await for the schedule status report operation to be completed. The
		 * wait time before timeout should be greater than the response timeout.
		 */
		futureStatusReportRequestResult.awaitUninterruptibly(
				SLEInterfaceInternalConfigManager.INSTANCE.getForwardScheduleStatusReportTimeoutMillis());

		if (futureStatusReportRequestResult.isSuccess()) {
			// Schedule status report succeeded
			logger.info("{} SLE interface forward service SCHEDULE STATUS REPORT SUCCEEDED",
					getCurrentConnectionNumberStringForLogging());
		} else {
			// Schedule status report failed. Don't throw, but log the reason.
			logger.error(
					"{} SLE interface forward service SCHEDULE STATUS REPORT FAILED. NO PERIODIC STATUS REPORTS WILL BE RECEIVED.",
					getCurrentConnectionNumberStringForLogging(), futureStatusReportRequestResult.cause());
		}

		/*
		 * Now start the SLEInterfaceForwardServiceThrowEventSerializer thread.
		 * First, instantiate a new blocking queue. This will never grow too big
		 * since it's user initiated.
		 */
		throwEventQueue = new LinkedBlockingQueue<>();
		logger.debug("bind(): Instantiated new throw event queue {}",
				Integer.toHexString(System.identityHashCode(throwEventQueue)));

		/*
		 * Create the thread that will run the
		 * SLEInterfaceForwardServiceThrowEventSerializer
		 */
		if (throwEventSerializerThread != null && throwEventSerializerThread.isAlive()) {
			logger.warn(
					"{} Unexpected condition: SLE interface forward service throw event serializer thread is still alive. Interrupting it and starting a new one.",
					getCurrentConnectionNumberStringForLogging());
			throwEventSerializerThread.interrupt();
		}

		throwEventSerializerThread = new Thread(new SLEInterfaceForwardServiceThrowEventSerializer(
				userFcltuServiceInstance, throwEventQueue, throwEventClearedConfirmationQueue));

		logger.debug("bind(): Starting the throw event serializer thread");
		/*
		 * Add listener for events. Events should be received and queued, and
		 * processed by a separate thread.
		 */
		throwEventSerializerThread.start();

		/*
		 * If using the DSN's THROW EVENT scheme, initialize the
		 * throw-event-configurable service management values to the configured
		 * defaults.
		 */
		if (SLEInterfaceInternalConfigManager.INSTANCE
				.getForwardThrowEventScheme() == ESLEInterfaceForwardThrowEventScheme.DSN) {
			logger.info(
					"{} Using the DSN throw event scheme. Starting initialization of the provider with configured throw event default values.",
					getCurrentConnectionNumberStringForLogging());

			try {
				logger.info("{} Initiating throw event of bit-rate and mod-index: {} and {}",
						getCurrentConnectionNumberStringForLogging(),
						SLEInterfaceInternalConfigManager.INSTANCE.getForwardDefaultBitrate(),
						SLEInterfaceInternalConfigManager.INSTANCE.getForwardDefaultModindex());
				throwDSNBitrateModindexChangeEvent(
						SLEInterfaceInternalConfigManager.INSTANCE.getForwardDefaultBitrate(),
						SLEInterfaceInternalConfigManager.INSTANCE.getForwardDefaultModindex());
			} catch (IllegalArgumentException | IllegalStateException e) {
				logger.error("{} Initialization throw event of bit-rate and mod-index failed with an exception",
						getCurrentConnectionNumberStringForLogging(), e);
			}

			try {
				logger.info("{} Initiating throw event of command modulation state change: {}",
						getCurrentConnectionNumberStringForLogging(),
						SLEInterfaceInternalConfigManager.INSTANCE.getForwardDefaultCommandModState());
				throwDSNCommandModStateChangeEvent(
						SLEInterfaceInternalConfigManager.INSTANCE.getForwardDefaultCommandModState());
			} catch (IllegalArgumentException | IllegalStateException e) {
				logger.error(
						"{} Initialization throw event of command modulation state change failed with an exception",
						getCurrentConnectionNumberStringForLogging(), e);
			}

			if (SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventRangeModEnable()) {

				try {
					logger.info("{} Initiating throw event of range modulation state change: {}",
							getCurrentConnectionNumberStringForLogging(),
							SLEInterfaceInternalConfigManager.INSTANCE.getForwardDefaultRangeModState());
					throwDSNRangeModStateChangeEvent(
							SLEInterfaceInternalConfigManager.INSTANCE.getForwardDefaultRangeModState());
				} catch (IllegalArgumentException | IllegalStateException e) {
					logger.error(
							"{} Initialization throw event of range modulation state change failed with an exception",
							getCurrentConnectionNumberStringForLogging(), e);
				}

			} else {
				logger.debug("Range modulation state change is disabled by configuration, so skipping its throw event");
			}

		}

		logger.info("{} All BIND-associated operations are now complete", getCurrentConnectionNumberStringForLogging());
	}

	/**
	 * UNBIND from the currently bound SLE forward service provider.
	 *
	 * @throws Throwable
	 *             Thrown when unexpected error is encountered
	 */
	public synchronized void unbind() throws Throwable {
		logger.info("{} Initiating SLE interface forward service UNBIND", getCurrentConnectionNumberStringForLogging());

		if (userFcltuServiceInstance == null || userFcltuServiceInstance.getState() != UserServiceState.READY) {
			logger.error(
					"SLE interface forward service UNBIND FAILED. Service not in READY state. Current service instance state: {}",
					(userFcltuServiceInstance != null ? userFcltuServiceInstance.getState()
							: "Service instance not intialized"));
			logger.debug("unbind(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface forward service UNBIND is not allowed because not in READY state. Current service instance state: "
							+ (userFcltuServiceInstance != null ? userFcltuServiceInstance.getState()
									: "Service instance not intialized"));
		}

		OperationFuture<Void> futureUnbindResult = null;

		// Invoke the unbind request
		try {
			// Using 'suspend' for all unbinds
			logger.debug("unbind(): Invoking the UNBIND request with SUSPEND");
			futureUnbindResult = userFcltuServiceInstance.unbindRequest(UnbindReason.SUSPEND);
		} catch (Exception e) {
			logger.error(
					"{} SLE interface forward service UNBIND request caused an exception. Current servince instance state: {}",
					getCurrentConnectionNumberStringForLogging(), userFcltuServiceInstance.getState(), e);
			logger.debug("unbind(): Throwing a RuntimeException to abort this method");
			throw new RuntimeException(
					"SLE interface forward service UNBIND caused an exception. Current service instance state: "
							+ userFcltuServiceInstance.getState(),
					e);
		}

		/*
		 * Await for the unbind operation to be completed. The wait time before
		 * timeout should be greater than the response timeout.
		 */
		futureUnbindResult
				.awaitUninterruptibly(SLEInterfaceInternalConfigManager.INSTANCE.getForwardBindUnbindTimeoutMillis());

		if (futureUnbindResult.isSuccess()) {
			// Unbind succeeded
			forwardServiceStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
			messageDistributor.sleForwardProviderStateChange(EForwardActionType.UNBIND, null,
					forwardServiceStateChangeTime, null);
			logger.info("{} SLE interface forward service UNBIND SUCCEEDED at {}",
					getCurrentConnectionNumberStringForLogging(),
					forwardServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
			initializeConnectionNumberAndDeliveryMode();
		} else {
			/*
			 * Unbind failed, but don't automatically throw because DSN provider
			 * seems to respond to UNBIND in a way that causes an error in the
			 * LSE Space's SLE library
			 */
			Throwable failCause = futureUnbindResult.cause();
			logger.error("{} SLE interface forward service UNBIND FAILED", getCurrentConnectionNumberStringForLogging(),
					failCause);

			if (userFcltuServiceInstance.getState() != UserServiceState.UNBOUND) {
				logger.debug("unbind(): Service instance state is {}. Throwing the Throwable to abort this method",
						userFcltuServiceInstance.getState());
				throw failCause;
			} else {
				logger.debug("unbind(): But because service instance state is now {}, not throwing",
						userFcltuServiceInstance.getState());
			}

		}

		logger.debug("unbind(): Interrupting the events handling thread");
		// Stop the event handling thread
		eventsHandlingThread.interrupt();

		logger.debug("unbind(): Interrupting the throw event serializer thread");
		// Stop the throw event serializer thread
		throwEventSerializerThread.interrupt();

		logger.info("All UNBIND-associated operations are now complete");
	}

	/**
	 * START the currently bound SLE forward service.
	 *
	 * @throws Throwable
	 *             Thrown when an unexpected error is encountered
	 */
	public synchronized void start() throws Throwable {
		logger.info("{} Initiating SLE interface forward service START", getCurrentConnectionNumberStringForLogging());

		if (userFcltuServiceInstance == null || userFcltuServiceInstance.getState() != UserServiceState.READY) {
			logger.error(
					"{} SLE interface forward service START FAILED. Service not in READY state. Current service instance state: {}",
					getCurrentConnectionNumberStringForLogging(), (userFcltuServiceInstance != null
							? userFcltuServiceInstance.getState() : "Service instance not intialized"));
			logger.debug("start(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface forward service START is not allowed because not in READY state. Current service instance state: "
							+ (userFcltuServiceInstance != null ? userFcltuServiceInstance.getState()
									: "Service instance not intialized"));
		}

		/*
		 * To avoid specifying our own first-cltu-identification value, do what
		 * APL's sle_cmd_gateway does and use the value that the provider is
		 * already expecting
		 */
		OperationFuture<FcltuGetParameterResponse> futureGetParameterResult = null;

		// Invoke the get parameter request
		try {
			logger.debug("start(): Invoking GET PARAMETER request for expected-cltu-identification");
			futureGetParameterResult = userFcltuServiceInstance
					.getFcltuParameter(FcltuParamName.EXPECTED_SLDU_IDENTIFICATION);
		} catch (Exception e) {
			logger.error(
					"{} SLE interface forward service START FAILED. GET PARAMETER of expected-cltu-identification generated an exception",
					getCurrentConnectionNumberStringForLogging(), e);
			logger.debug("start(): Throwing a RuntimeException to abort this method");
			throw new RuntimeException(
					"SLE interface forward service START FAILED. GET PARAMETER of expected-cltu-identification generated an exception",
					e);
		}

		/*
		 * Await for the get parameter operation to be completed. The wait time
		 * before timeout should be greater than the response timeout.
		 */
		futureGetParameterResult
				.awaitUninterruptibly(SLEInterfaceInternalConfigManager.INSTANCE.getForwardGetParameterTimeoutMillis());

		if (futureGetParameterResult.isSuccess()) {
			logger.debug("start(): GET PARAMETER of expected-cltu-identification SUCCEEDED");
		} else {
			// Get parameter failed. Throw reason.
			Throwable failCause = futureGetParameterResult.cause();
			logger.error(
					"{} SLE interface forward service START FAILED. GET PARAMETER of expected-cltu-identification FAILED",
					getCurrentConnectionNumberStringForLogging(), failCause);
			logger.debug("start(): Throwing the Throwable to abort this method");
			throw failCause;
		}

		FcltuGetParameterResponse getParameterResponse = futureGetParameterResult.result();
		long expectedSlduId = getParameterResponse.getExpectedSlduId();
		// expectedSlduId now has the first-cltu-identification value
		logger.info(
				"{} SLE interface forward service provider expects first-cltu-identification of {}. This will be the ID for both START and the next CLTU.",
				getCurrentConnectionNumberStringForLogging(), expectedSlduId);

		logger.debug("start(): Invoking the START request");
		// Invoke the start request
		OperationFuture<Void> futureStartResult = userFcltuServiceInstance.startRequest(expectedSlduId);

		/*
		 * Await for the start operation to be completed. The wait time before
		 * timeout should be greater than the response timeout.
		 */
		futureStartResult
				.awaitUninterruptibly(SLEInterfaceInternalConfigManager.INSTANCE.getForwardStartStopTimeoutMillis());

		if (futureStartResult.isSuccess()) {
			// Start succeeded
			forwardServiceStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
			messageDistributor.sleForwardProviderStateChange(EForwardActionType.START, null,
					forwardServiceStateChangeTime, null);
			logger.info("{} SLE interface forward service START SUCCEEDED at {}. First CLTU ID: {}",
					getCurrentConnectionNumberStringForLogging(),
					forwardServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()),
					expectedSlduId);
		} else {
			// Start failed. Throw reason.
			Throwable failCause = futureStartResult.cause();
			logger.error("{} SLE interface forward service START FAILED", getCurrentConnectionNumberStringForLogging(),
					failCause);
			logger.debug("start(): Throwing the Throwable to abort this method");
			throw failCause;
		}

		// Create the queue for receiving CLTU data from the chill interface
		BlockingQueue<byte[]> cltuQueue = new LinkedBlockingQueue<>(
				ChillInterfaceInternalConfigManager.INSTANCE.getUplinkCLTUsBufferCapacity());
		logger.debug("start(): Instantiated new CLTU queue (for receiving data from chill interface) {}",
				Integer.toHexString(System.identityHashCode(cltuQueue)));

		logger.debug("start(): Starting the CLTU transferrer thread");
		/*
		 * Create and start the thread that will monitor the queue and transfer
		 * the CLTU data
		 */
		if (cltusTransferringThread != null && cltusTransferringThread.isAlive()) {
			logger.warn(
					"{} Unexpected condition: SLE interface forward service CLTUs transferring thread is still alive. Interrupting it and starting a new one.",
					getCurrentConnectionNumberStringForLogging());
			cltusTransferringThread.interrupt();
		}

		cltusTransferringThread = new Thread(
				new SLEInterfaceForwardServiceCLTUTransferrer(userFcltuServiceInstance, expectedSlduId, cltuQueue));
		cltusTransferringThread.start();

		/*
		 * Now share the CLTU queue with the chill interface uplink manager, so
		 * that the chill interface will push to it
		 */
		logger.debug("start(): Setting ChillInterfaceUplinkManager's CLTU queue to {}",
				Integer.toHexString(System.identityHashCode(cltuQueue)));
		ChillInterfaceUplinkManager.INSTANCE.setCLTUQueue(cltuQueue);

		logger.info("{} All START-associated operations are now complete",
				getCurrentConnectionNumberStringForLogging());
	}

	/**
	 * STOP the currently bound SLE forward service.
	 *
	 * @throws Throwable
	 *             Thrown when an unexpected error is encountered
	 */
	public synchronized void stop() throws Throwable {
		logger.info("{} Initiating SLE interface forward service STOP", getCurrentConnectionNumberStringForLogging());

		if (userFcltuServiceInstance == null || userFcltuServiceInstance.getState() != UserServiceState.ACTIVE) {
			logger.error(
					"{} SLE interface forward service STOP FAILED. Service not in ACTIVE state. Current service instance state: {}",
					(userFcltuServiceInstance != null ? userFcltuServiceInstance.getState()
							: "Service instance not intialized"));
			logger.debug("stop(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface forward service STOP is not allowed because not in ACTIVE state. Current service instance state: "
							+ (userFcltuServiceInstance != null ? userFcltuServiceInstance.getState()
									: "Service instance not intialized"));
		}

		logger.debug("stop(): Setting ChillInterfaceUplinkManager's CLTU queue to null");
		/*
		 * "Take away" the CLTU queue from the chill interface uplink manager,
		 * so that the chill interface will no longer push to it
		 */
		ChillInterfaceUplinkManager.INSTANCE.setCLTUQueue(null);

		logger.debug("stop(): Interrupting the CLTU transferrer thread");
		// Stop the CLTU transferring thread
		cltusTransferringThread.interrupt();

		try {
			cltusTransferringThread.join(
					SLEInterfaceInternalConfigManager.INSTANCE.getForwardCLTUTransferrerTerminationTimeoutMillis());
		} catch (InterruptedException ie) {
			logger.warn(
					"{} SLE interface forward service STOP encountered interruption while waiting for CLTU transferring thread to finish. Some CLTUs may not have transferred. Proceeding with STOP.",
					getCurrentConnectionNumberStringForLogging(), ie);
		}

		logger.debug("stop(): Invoking the STOP request");
		// Invoke the stop request
		OperationFuture<Void> futureStopResult = userFcltuServiceInstance.stopRequest();

		/*
		 * Await for the stop operation to be completed. The wait time before
		 * timeout should be greater than the response timeout.
		 */
		futureStopResult
				.awaitUninterruptibly(SLEInterfaceInternalConfigManager.INSTANCE.getForwardStartStopTimeoutMillis());

		if (futureStopResult.isSuccess()) {
			// Stop succeeded
			forwardServiceStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
			messageDistributor.sleForwardProviderStateChange(EForwardActionType.STOP, null,
					forwardServiceStateChangeTime, null);
			logger.info("{} SLE interface forward service STOP SUCCEEDED at {}",
					getCurrentConnectionNumberStringForLogging(),
					forwardServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
		} else {
			// Stop failed. Throw reason.
			Throwable failCause = futureStopResult.cause();
			logger.error("{} SLE interface forward service STOP FAILED", getCurrentConnectionNumberStringForLogging(),
					failCause);
			logger.debug("stop(): Throwing the Throwable to abort this method");
			throw failCause;
		}

		logger.info("{} All STOP-associated operations are now complete", getCurrentConnectionNumberStringForLogging());
	}

	/**
	 * PEER ABORT the current SLE forward service.
	 *
	 * @throws Throwable
	 *             Thrown when an unexpected error is encountered
	 */
	public synchronized void peerAbort() throws Throwable {
		logger.info("{} Initiating SLE interface forward service PEER ABORT",
				getCurrentConnectionNumberStringForLogging());

		if (userFcltuServiceInstance == null || userFcltuServiceInstance.getState() == UserServiceState.UNBOUND) {
			logger.error(
					"SLE interface forward service PEER ABORT FAILED. Service already unbound. Current service instance state: {}",
					(userFcltuServiceInstance != null ? userFcltuServiceInstance.getState()
							: "Service instance not intialized"));
			logger.debug("peerAbort(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface forward service PEER ABORT is not allowed because already unbound. Current service instance state: "
							+ (userFcltuServiceInstance != null ? userFcltuServiceInstance.getState()
									: "Service instance not intialized"));
		}

		logger.debug("peerAbort(): Interrupting the throw event serializer thread");
		// Stop the throw event serializer thread
		throwEventSerializerThread.interrupt();

		logger.debug("peerAbort(): Setting ChillInterfaceUplinkManager's CLTU queue to null");
		/*
		 * "Take away" the CLTU queue from the chill interface uplink manager,
		 * so that the chill interface will no longer push to it
		 */
		ChillInterfaceUplinkManager.INSTANCE.setCLTUQueue(null);

		if (cltusTransferringThread != null && cltusTransferringThread.isAlive()) {
			logger.debug("peerAbort(): Interrupting the CLTU transferrer thread and waiting for it to finish");
			// Stop the CLTU transferring thread
			cltusTransferringThread.interrupt();

			try {
				cltusTransferringThread.join(
						SLEInterfaceInternalConfigManager.INSTANCE.getForwardCLTUTransferrerTerminationTimeoutMillis());
			} catch (InterruptedException ie) {
				logger.warn(
						"{} SLE interface forward service PEER ABORT encountered interruption while waiting for CLTU transferring thread to finish. Some CLTUs may not have transferred. Proceeding with PEER ABORT.",
						getCurrentConnectionNumberStringForLogging(), ie);
			}

		} else {
			logger.debug("peerAbort(): CLTU transferrer thread already dead so taking no action");
		}

		logger.debug("peerAbort(): Invoking the PEER ABORT request");
		// Invoke the peer abort
		OperationFuture<Void> futurePeerAbortResult = userFcltuServiceInstance.abort();

		/*
		 * Await for the peer abort operation to be completed. The wait time
		 * before timeout should be greater than the response timeout.
		 */
		futurePeerAbortResult
				.awaitUninterruptibly(SLEInterfaceInternalConfigManager.INSTANCE.getForwardPeerAbortTimeoutMillis());

		if (futurePeerAbortResult.isSuccess()) {
			// Peer abort succeeded
			forwardServiceStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
			messageDistributor.sleForwardProviderStateChange(EForwardActionType.ABORT, null,
					forwardServiceStateChangeTime, null);
			logger.info("{} SLE interface forward service PEER ABORT SUCCEEDED at {}",
					getCurrentConnectionNumberStringForLogging(),
					forwardServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
			initializeConnectionNumberAndDeliveryMode();
		} else {
			// Peer abort failed. Throw reason.
			Throwable failCause = futurePeerAbortResult.cause();
			logger.error("{} SLE interface forward service PEER ABORT FAILED",
					getCurrentConnectionNumberStringForLogging(), failCause);
			logger.debug("peerAbort(): Throwing the Throwable to abort this method");
			throw failCause;
		}

		logger.debug("peerAbort(): Interrupting the FCLTU event queue handling thread");
		// Stop the event handling thread
		eventsHandlingThread.interrupt();

		logger.info("All PEER-ABORT-associated operations are now complete");
	}

	/**
	 * Throw event wrapper for the DSN's bitrate/modindex change.
	 *
	 * @param newBitrate
	 *            New bitrate value to include in the throw event
	 * @param newModindex
	 *            New modindex value to include in the throw event
	 * @throws IllegalArgumentException
	 *             Thrown when either the bitrate or the modindex parameter is
	 *             unacceptable
	 * @throws IllegalStateException
	 *             Thrown when the throw event fails
	 */
	public void throwDSNBitrateModindexChangeEvent(final String newBitrate, final int newModindex)
			throws IllegalArgumentException, IllegalStateException {
		logger.debug("Entered throwDSNBitrateModindexChangeEvent(newBitrate: {}, newModindex: {})", newBitrate,
				newModindex);

		// Check: Is the provided bitrate value allowed?
		if (!SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventAllowableBitrates().contains(newBitrate)) {
			logger.error("{} Bitrate value is not allowed: {} not included in {}",
					getCurrentConnectionNumberStringForLogging(), newBitrate,
					SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventAllowableBitrates());
			logger.debug(
					"throwDSNBitrateModindexChangeEvent(): Throwing an IllegalArgumentException to abort this method");
			throw new IllegalArgumentException("Bitrate value is not allowed: " + newBitrate + " not included in "
					+ SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventAllowableBitrates());
		}

		// Check: Is the provided modindex value allowed?
		if (!SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventAllowableModindexRange()
				.contains(newModindex)) {
			logger.error("{} Modindex value {} is out of range: {}", getCurrentConnectionNumberStringForLogging(),
					newModindex,
					SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventAllowableModindexRange());
			logger.debug(
					"throwDSNBitrateModindexChangeEvent(): Throwing an IllegalArgumentException to abort this method");
			throw new IllegalArgumentException("Modindex value " + newModindex + " is out of range: "
					+ SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventAllowableModindexRange());
		}

		// Validation passed. Submit throw event to the queue.
		String qualifierStr = new String(
				DSN_THROW_EVENT_BITRATE_PREFIX + newBitrate + DSN_THROW_EVENT_MODINDEX_PREFIX + newModindex);

		try {
			logger.info("{} Submitting throw event into the queue: event-identifier: {}, event-qualifier: \"{}\"",
					getCurrentConnectionNumberStringForLogging(), 4, qualifierStr);
			throwEventQueue.add(new SLEInterfaceForwardServiceThrowEvent(4, qualifierStr.getBytes()));
		} catch (Throwable e) {
			logger.error("{} Failed to add throw event (event-identifier: {}, event-qualifier: \"{}\") into the queue",
					getCurrentConnectionNumberStringForLogging(), 4, qualifierStr, e);
			logger.debug(
					"throwDSNBitrateModindexChangeEvent(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException("Failed to add throw event (event-identifier: 4, event-qualifier: \""
					+ qualifierStr + "\") into the queue", e);
		}

		logger.debug("Exiting throwDSNBitrateModindexChangeEvent()");
	}

	/**
	 * Throw event wrapper for the DSN's command modulation state change.
	 *
	 * @param newCommandModState
	 *            New command modulation state value to include in the throw
	 *            event
	 * @throws IllegalArgumentException
	 *             Thrown when the command modulation state parameter is
	 *             unacceptable
	 * @throws IllegalStateException
	 *             Thrown when the throw event fails
	 */
	public void throwDSNCommandModStateChangeEvent(final String newCommandModState)
			throws IllegalArgumentException, IllegalStateException {
		logger.debug("Entered throwDSNCommandModStateChangeEvent(newCommandModState: {})", newCommandModState);
		byte[] qualifier = null;

		if ("on".equalsIgnoreCase(newCommandModState)) {
			qualifier = "on".getBytes();
		} else if ("off".equalsIgnoreCase(newCommandModState)) {
			qualifier = "off".getBytes();
		} else {
			throw new IllegalArgumentException(
					"Command modulation state value " + newCommandModState + " is invalid. Must be 'on' or 'off'.");
		}

		try {
			logger.info("{} Submitting throw event into the queue: event-identifier: {}, event-qualifier: {}",
					getCurrentConnectionNumberStringForLogging(), 5, new String(qualifier));
			throwEventQueue.add(new SLEInterfaceForwardServiceThrowEvent(5, qualifier));
		} catch (Throwable e) {
			logger.error("{} Failed to add throw event (event-identifier: {}, event-qualifier: \"{}\") into the queue",
					getCurrentConnectionNumberStringForLogging(), 5, new String(qualifier), e);
			logger.debug(
					"throwDSNCommandModStateChangeEvent(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException("Failed to add throw event (event-identifier: 5, event-qualifier: \""
					+ new String(qualifier) + "\") into the queue", e);
		}

		logger.debug("Exiting throwDSNCommandModStateChangeEvent()");
	}

	/**
	 * Throw event wrapper for the DSN's range modulation state change.
	 *
	 * @param newRangeModState
	 *            New range modulation state value to include in the throw event
	 * @throws IllegalArgumentException
	 *             Thrown when the range modulation state parameter is
	 *             unacceptable
	 * @throws IllegalStateException
	 *             Thrown when the throw event fails
	 */
	public void throwDSNRangeModStateChangeEvent(final String newRangeModState)
			throws IllegalArgumentException, IllegalStateException {
		logger.debug("Entered throwDSNRangeModStateChangeEvent(newRangeModState: {})", newRangeModState);

		if (!SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventRangeModEnable()) {
			// Range modulation state change is disabled. Exit.
			logger.info("{} Range modulation state change is disabled. Aborting throw event.",
					getCurrentConnectionNumberStringForLogging());
			logger.debug("throwDSNRangeModStateChangeEvent(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException("Range modulation state change is disabled");
		}

		byte[] qualifier = null;

		if ("on".equalsIgnoreCase(newRangeModState)) {
			qualifier = "on".getBytes();
		} else if ("off".equalsIgnoreCase(newRangeModState)) {
			qualifier = "off".getBytes();
		} else {
			throw new IllegalArgumentException(
					"Range modulation state value " + newRangeModState + " is invalid. Must be 'on' or 'off'.");
		}

		try {
			logger.info("{} Submitting throw event into the queue: event-identifier: {}, event-qualifier: {}",
					getCurrentConnectionNumberStringForLogging(), 6, new String(qualifier));
			throwEventQueue.add(new SLEInterfaceForwardServiceThrowEvent(6, qualifier));
		} catch (Throwable e) {
			logger.error("{} Failed to add throw event (event-identifier: {}, event-qualifier: \"{}\") into the queue",
					getCurrentConnectionNumberStringForLogging(), 6, new String(qualifier), e);
			logger.debug("throwDSNRangeModStateChangeEvent(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException("Failed to add throw event (event-identifier: 6, event-qualifier: \""
					+ new String(qualifier) + "\") into the queue", e);
		}

		logger.debug("Exiting throwDSNRangeModStateChangeEvent()");
	}

	/**
	 * Get the current SLE state (in printable form) of the SLE forward service.
	 *
	 * @return The current SLE state
	 */
	public String getServiceStateString() {

		if (userFcltuServiceInstance == null) {
			return UserServiceState.UNBOUND.name();
		} else {
			return userFcltuServiceInstance.getState().name();
		}

	}

	/**
	 * Get the timestamp of when the SLE forward service changed states.
	 *
	 * @return The timestamp of when the SLE forward service changed states
	 */
	public ZonedDateTime getForwardServiceStateChangeTime() {
		return forwardServiceStateChangeTime;
	}

	/**
	 * Atomically increment the number of CLTUs transferred via the SLE forward
	 * service (by 1) and update the time that a CLTU was last transferred with
	 * the provided timestamp.
	 *
	 * @param dataTransferredTime
	 *            Timestamp of when the last CLTU transfer was done
	 */
	public void incrementTransferredDataCount(final ZonedDateTime dataTransferredTime) {

		synchronized (transferDataTimeAndCountLock) {
			lastTransferDataTimeSinceLastBind = dataTransferredTime;
			transferredDataCountSinceLastBind++;
		}

	}

	/**
	 * Get the timestamp of when the last CLTU was transferred. Timestamp is
	 * cleared at every BIND.
	 *
	 * @return The timestamp of when the last CLTU was transferred
	 */
	public ZonedDateTime getLastTransferDataTimeSinceLastBind() {
		return lastTransferDataTimeSinceLastBind;
	}

	/**
	 * Get the total count of CLTUs transferred via the SLE forward service.
	 * Counter resets at every BIND.
	 *
	 * @return The total count of CLTUs transferred during the current service
	 *         instance
	 */
	public long getTransferredDataCountSinceLastBind() {
		return transferredDataCountSinceLastBind;
	}

	/**
	 * Get the forward service delivery mode.
	 *
	 * @return The forward service delivery mode
	 */
	public ForwardServiceDeliveryMode getForwardServiceDeliveryMode() {
		return forwardServiceDeliveryMode;
	}

	/**
	 * Get the name of the SLE interface profile that is currently bound.
	 *
	 * @return Name of the SLE interface profile that is currently bound, or
	 *         <code>null</code> if not bound
	 */
	public String getBoundProfileName() {

		if (userFcltuServiceInstance == null || userFcltuServiceInstance.getState() == UserServiceState.UNBOUND) {
			return null;
		} else {
			return lastBoundProfileName;
		}

	}

	/**
	 * Get the current 'F' connection number.
	 *
	 * @return The current 'F' connection number
	 */
	public long getCurrentConnectionNumber() {
		return currentConnectionNumber;
	}

	/**
	 * Get the current 'F' connection number as a formatted string for the
	 * purpose of logging. The returned string will be either "[F?]" if 'F'
	 * connection number is unknown, or "[F#]" where # is the connection number
	 * if the number is known.
	 *
	 * @return The 'F' connection number string formatted for logging
	 */
	public String getCurrentConnectionNumberStringForLogging() {

		if (currentConnectionNumber >= 0) {
			return "[F" + currentConnectionNumber + "]";
		} else {
			return "[F?]";
		}

	}

	/**
	 * Get the profile name of the forward service that the last BIND was
	 * attempted with.
	 *
	 * @return The last used SLE forward service profile name
	 */
	public String getLastSLEForwardServiceProfileName() {
		return lastSLEForwardServiceProfileName;
	}

	/**
	 * Set the last accepted bit rate.
	 *
	 * @param newBitrate
	 *            The new bit rate value
	 */
	public void setLastAcceptedBitrate(final String newBitrate) {
		lastAcceptedBitrate = newBitrate;
	}

	/**
	 * Returns the bit rate. The value is saved from the last throw event that
	 * set it, so if no such throw event has been issued, null will be returned.
	 * If the SLE service is not bound, null will be returned.
	 *
	 * @return The last accepted bit rate, or null if no throw event has been
	 *         issued that set it or the SLE service is not bound
	 */
	public String getLastAcceptedBitrateForReporting() {

		if (userFcltuServiceInstance == null || userFcltuServiceInstance.getState() == UserServiceState.UNBOUND) {
			return null;
		} else {
			return lastAcceptedBitrate;
		}

	}

	/**
	 * Returns the actual value of the bit rate variable in memory, regardless
	 * of whether the SLE service is bound or not.
	 *
	 * @return The last accepted bit rate value in memory
	 */
	public String getLastAcceptedBitrateInMemory() {
		return lastAcceptedBitrate;
	}

	/**
	 * Returns the modulation index. The value is queried from the provider via
	 * CLTU-GET-PARAMETER.
	 *
	 * @return The modulation index, or null if the parameter is not available
	 */
	public Integer getModindexFromProvider() {
		logger.trace("Entered getModindexFromProvider()");

		if (userFcltuServiceInstance == null || userFcltuServiceInstance.getState() == UserServiceState.UNBOUND) {
			logger.trace("getModindexFromProvider(): Service instance is null or unbound so returning with null");
			return null;
		}

		OperationFuture<FcltuGetParameterResponse> futureGetParameterResult = null;

		// Invoke the get parameter request
		try {
			logger.trace("getModindexFromProvider(): Invoking the GET PARAMETER request for modulation-index");
			futureGetParameterResult = userFcltuServiceInstance.getFcltuParameter(FcltuParamName.MODULATION_INDEX);
		} catch (Exception e) {
			logger.error(
					"{} SLE interface forward service GET PARAMETER request of modulation-index caused an exception",
					getCurrentConnectionNumberStringForLogging(), e);
			logger.trace("getModindexFromProvider(): Returning with null");
			return null;
		}

		/*
		 * Await for the get parameter operation to be completed. The wait time
		 * before timeout should be greater than the response timeout.
		 */
		futureGetParameterResult
				.awaitUninterruptibly(SLEInterfaceInternalConfigManager.INSTANCE.getForwardGetParameterTimeoutMillis());

		if (futureGetParameterResult.isSuccess()) {
			logger.trace("getModindexFromProvider(): GET PARAMETER request of modulation-index SUCCEEDED");
		} else {
			// Get parameter failed. Log the caught reason and return null.
			Throwable failCause = futureGetParameterResult.cause();
			logger.error("{} SLE interface forward service GET PARAMETER request of modulation-index FAILED",
					getCurrentConnectionNumberStringForLogging(), failCause);
			logger.trace("getModindexFromProvider(): Returning with null");
			return null;
		}

		FcltuGetParameterResponse getParameterResponse = futureGetParameterResult.result();
		int modindex = getParameterResponse.getModulationIndex();
		logger.trace("getModindexFromProvider(): Returning with modulation-index of {}", modindex);
		return modindex;
	}

	/**
	 * Set the last accepted command modulation state.
	 *
	 * @param newCommandModState
	 *            The new command modulation state value
	 */
	public void setLastAcceptedCommandModState(final String newCommandModState) {
		lastAcceptedCommandModState = newCommandModState;
	}

	/**
	 * Returns the command modulation state. The value is saved from the last
	 * throw event that set it, so if no such throw event has been issued, null
	 * will be returned. If the SLE service is not bound, null will be returned.
	 *
	 * @return The last accepted command modulation state, or null if no throw
	 *         event has been issued that set it or the SLE service is not bound
	 */
	public String getLastAcceptedCommandModStateForReporting() {

		if (userFcltuServiceInstance == null || userFcltuServiceInstance.getState() == UserServiceState.UNBOUND) {
			return null;
		} else {
			return lastAcceptedCommandModState;
		}

	}

	/**
	 * Returns the actual value of the command modulation state variable in
	 * memory, regardless of whether the SLE service is bound or not.
	 *
	 * @return The last accepted command modulation state value in memory
	 */
	public String getLastAcceptedCommandModStateInMemory() {
		return lastAcceptedCommandModState;
	}

	/**
	 * Set the last accepted range modulation state.
	 *
	 * @param newRangeModState
	 *            The new range modulation state value
	 */
	public void setLastAcceptedRangeModState(final String newRangeModState) {
		lastAcceptedRangeModState = newRangeModState;
	}

	/**
	 * Returns the range modulation state. The value is saved from the last
	 * throw event that set it, so if no such throw event has been issued, null
	 * will be returned. If the SLE service is not bound, null will be returned.
	 * If chill_sle_proxy is configured to disable any range modulation state
	 * changes, "disabled" will be returned.
	 *
	 * @return The last accepted range modulation state, null if no throw event
	 *         has been issued that set it or the SLE service is not bound, or
	 *         "disabled" if configured to disable range mod state changes
	 */
	public String getLastAcceptedRangeModStateForReporting() {

		if (!SLEInterfaceInternalConfigManager.INSTANCE.getForwardThrowEventRangeModEnable()) {
			return "disabled";
		} else if (userFcltuServiceInstance == null
				|| userFcltuServiceInstance.getState() == UserServiceState.UNBOUND) {
			return null;
		} else {
			return lastAcceptedRangeModState;
		}

	}

	/**
	 * Returns the actual value of the range modulation state variable in
	 * memory, regardless of whether the SLE service is bound or not.
	 *
	 * @return The last accepted range modulation state value in memory
	 */
	public String getLastAcceptedRangeModStateInMemory() {
		return lastAcceptedRangeModState;
	}

	/**
	 * Notify the SLEInterfaceForwardService of a cleared throw event. This is
	 * needed in order for SLEInterfaceForwardService to save its internal state
	 * of which throw event was accepted, for the purpose of reporting it in the
	 * "state" REST API.
	 *
	 * @param eventId
	 *            The original ID of the throw event
	 * @param qualifier
	 *            The original qualifier of the throw event
	 * @param notificationType
	 *            The notification type that indicates how the throw event
	 *            cleared
	 */
	public void notifyOfClearedThrowEvent(final int eventId, final byte[] qualifier,
			final FcltuAsyncNotificationType notificationType) {
		logger.debug("Entered notifyOfClearedThrowEvent(eventId: {}, qualifier: {}, notificationType: {}", eventId,
				qualifier, notificationType.name());

		/*
		 * If using the DSN's THROW EVENT scheme and the notification indicates
		 * that the action completed, translate back the event ID and qualifier
		 * for updating the internal state.
		 */
		if (SLEInterfaceInternalConfigManager.INSTANCE
				.getForwardThrowEventScheme() == ESLEInterfaceForwardThrowEventScheme.DSN
				&& notificationType == FcltuAsyncNotificationType.ACTION_LIST_COMPLETED) {

			switch (eventId) {
			case 4: // Bit-rate and mod-index change
				logger.debug(
						"notifyOfClearedThrowEvent(): Reverse translating DSN bit-rate/mod-index change throw event");
				String bitrateModindexQualifierStr = new String(qualifier);
				logger.debug("notifyOfClearedThrowEvent(): Qualifier is \"{}\"", bitrateModindexQualifierStr);
				Matcher matcher = Pattern.compile(
						DSN_THROW_EVENT_BITRATE_PREFIX + "([^\\s]+)" + DSN_THROW_EVENT_MODINDEX_PREFIX + "([^\\s]+)")
						.matcher(bitrateModindexQualifierStr);

				if (matcher.find()) {
					String first = matcher.group(1);
					String second = matcher.group(2);
					logger.debug("notifyOfClearedThrowEvent(): Pattern match OK. Group 1: {}, group 2: {}", first,
							second);
					logger.debug("notifyOfClearedThrowEvent(): Setting last accepted bit-rate to {}", first);
					setLastAcceptedBitrate(first);
				} else {
					logger.error(
							"{} Failed reverse translating the completed bit-rate/mod-index change throw event (qualifier: {})",
							getCurrentConnectionNumberStringForLogging(), bitrateModindexQualifierStr);
					logger.warn("{} chill_sle_proxy's knowledge of the last accepted bit-rate may now be out-of-date",
							getCurrentConnectionNumberStringForLogging());
				}

				break;
			case 5: // Command modulation state change
				logger.debug(
						"notifyOfClearedThrowEvent(): Reverse translating DSN command modulation state change throw event");
				String commandModQualifierStr = new String(qualifier);
				logger.debug(
						"notifyOfClearedThrowEvent(): Qualifier is \"{}\". Setting last accepted command mod state to it.",
						commandModQualifierStr);
				setLastAcceptedCommandModState(commandModQualifierStr);
				break;
			case 6: // Range modulation state change
				logger.debug(
						"notifyOfClearedThrowEvent(): Reverse translating DSN range modulation state change throw event");
				String rangeModQualifierStr = new String(qualifier);
				logger.debug(
						"notifyOfClearedThrowEvent(): Qualifier is \"{}\". Setting last accepted range mod state to it.",
						rangeModQualifierStr);
				setLastAcceptedRangeModState(rangeModQualifierStr);
				break;
			default:
				logger.error("{} UNEXPECTED EVENT ID RECEIVED BY notifyOfClearedThrowEvent: {}",
						getCurrentConnectionNumberStringForLogging(), eventId);
			}

		}

		logger.debug("Exiting notifyOfClearedThrowEvent()");
	}

	/**
	 * Set the current connection number and delivery mode to initial values.
	 */
	void initializeConnectionNumberAndDeliveryMode() {
		currentConnectionNumber = -1;
		forwardServiceDeliveryMode = null;
		messageDistributor.sleDeliveryModeChange(SLEProviderType.FORWARD, "");
	}

}