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

import com.lsespace.sle.user.SLEUserServiceAkkaFactory;
import com.lsespace.sle.user.SLEUserServiceFactory;
import com.lsespace.sle.user.proxy.isp1.AuthenticationMode;
import com.lsespace.sle.user.proxy.isp1.ISP1SLEUserProxyAkkaFactory;
import com.lsespace.sle.user.proxy.isp1.ISP1SLEUserProxyFactory;
import com.lsespace.sle.user.service.GVCID;
import com.lsespace.sle.user.service.RafGetParameterResponse;
import com.lsespace.sle.user.service.RafParamName;
import com.lsespace.sle.user.service.RcfGetParameterResponse;
import com.lsespace.sle.user.service.RcfParamName;
import com.lsespace.sle.user.service.ReturnServiceDeliveryMode;
import com.lsespace.sle.user.service.SLEUserRafInstance;
import com.lsespace.sle.user.service.SLEUserRcfInstance;
import com.lsespace.sle.user.service.SLEUserServiceInstance;
import com.lsespace.sle.user.service.UnbindReason;
import com.lsespace.sle.user.service.UserServiceState;
import com.lsespace.sle.user.service.data.TmProductionData;
import com.lsespace.sle.user.util.concurrent.OperationFuture;
import jpl.gds.sleproxy.server.sleinterface.AutoBindSleInstanceFactory;
import jpl.gds.sleproxy.server.sleinterface.AutoBindSleServiceInstance;
import jpl.gds.sleproxy.server.sleinterface.internal.config.SLEInterfaceInternalConfigManager;
import jpl.gds.sleproxy.server.sleinterface.profile.EInterfaceType;
import jpl.gds.sleproxy.server.sleinterface.profile.ProviderHost;
import jpl.gds.sleproxy.server.sleinterface.profile.ReturnSLEInterfaceProfile;
import jpl.gds.sleproxy.server.sleinterface.profile.SLEInterfaceProfileManager;
import jpl.gds.sleproxy.server.sleinterface.rtn.action.EReturnActionType;
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

/**
 * This singleton is the central point of all capabilities and operations
 * related to the SLE return service.
 * 
 */
public enum SLEInterfaceReturnService {

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
	 * SLE return service listening event handler object. The listener-handler
	 * will process the events received from the SLE return service.
	 */
	private SLEInterfaceReturnServiceListeningEventHandler listeningEventsHandler;

	/**
	 * LSE Space's RAF or RCF service instance.
	 */
	private SLEUserServiceInstance userRafOrRcfServiceInstance;

	/**
	 * The name of the SLE interface profile that was used to do the latest
	 * BIND.
	 */
	private String lastBoundProfileName;

	/**
	 * Interface type of the current SLE return service, needed to differentiate
	 * between RAF and RCF logic.
	 */
	private EInterfaceType lastBoundInterfaceType;

	/**
	 * The timestamp of when the SLE return service last changed states (e.g.
	 * BIND, START, etc.).
	 */
	private ZonedDateTime returnServiceStateChangeTime;

	/**
	 * The 'R' connection number counter.
	 */
	private volatile long currentConnectionNumber;

	/**
	 * The profile name of the return service that the last BIND was attempted
	 * with.
	 */
	private volatile String lastSLEReturnServiceProfileName;

	/**
	 * Timestamp of the last time that a frame was received from the SLE return
	 * service. Resets at every new BIND.
	 */
	private volatile ZonedDateTime lastTransferDataTimeSinceLastBind;

	/**
	 * Counter to keep track of the number of frames received from the SLE
	 * return service. Resets at every new BIND.
	 */
	private volatile long transferredDataCountSinceLastBind;

	/**
	 * Lock object for the frames transfer time and count.
	 */
	private volatile Object transferDataTimeAndCountLock;

	/**
	 * Delivery mode of the SLE return service.
	 */
	private volatile ReturnServiceDeliveryMode returnServiceDeliveryMode;

	/**
	 * The queue to push the received frames from the SLE return service into.
	 */
	private volatile BlockingQueue<TmProductionData> frameQueue;

	/**
	 * Lock object for the frame queue.
	 */
	private volatile Object frameQueueLock;

	private MessageDistributor messageDistributor;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceReturnService.class);

	/**
	 * Default constructor.
	 */
	SLEInterfaceReturnService() {

		// Create the Proxy Factory
		proxyFactory = new ISP1SLEUserProxyAkkaFactory();

		// Create the Service Factory
		serviceFactory = new SLEUserServiceAkkaFactory();
		
		messageDistributor = MessageDistributor.INSTANCE;
		
		initializeConnectionNumberAndDeliveryMode();
		lastSLEReturnServiceProfileName = ProxyStateManager.INSTANCE.getLastSLEReturnServiceProfileName();
		transferDataTimeAndCountLock = new Object();
		frameQueueLock = new Object();

	}

	/**
	 * Bind to a SLE return service provider.
	 *
	 * @param profile the SLE profile
	 * @throws Throwable Thrown when an unexpected error occurs during the BIND
	 */
	public synchronized void bind(final ReturnSLEInterfaceProfile profile) throws Throwable {
		// Interface type of the SLE return service (RAF vs RCF)
		final EInterfaceType     interfaceType              = profile.getInterfaceType();
		// Profile name of the SLE return service to bind to
		final String             profileName                = profile.getProfileName();
		// Version of the SLE standard to use
		final int                serviceVersion             = SLEInterfaceInternalConfigManager.INSTANCE.getReturnServiceVersion();
		// Name of the SLE return service provider
		final String             providerName               = profile.getProviderName();
		// Host names and ports of the SLE return service provider
		final List<ProviderHost> providerHosts              = profile.getProviderHosts();
		// Authentication mode of the SLE return service provider
		final AuthenticationMode providerAuthenticationMode = profile.getProviderAuthenticationMode();
		// SIID of the SLE return service
		final String             serviceInstanceID          = profile.getServiceInstanceID();
		// User name for the SLE return service
		final String             userName                   = profile.getUserName();
		// User authentication mode of the SLE return service
		final AuthenticationMode        userAuthenticationMode     = profile.getUserAuthenticationMode();

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
				"Initiating SLE interface return service BIND. Profile name: {}, service version: {}, provider name: {}, {}, provider password: <masked>, provider authentication mode: {}, service instance ID: {}, user name: {}, user password: <masked>, user authentication mode: {}",
				profileName, serviceVersion, providerName, sb.toString(), providerAuthenticationMode,
				serviceInstanceID, userName, userAuthenticationMode);

		lastSLEReturnServiceProfileName = profileName;
		ProxyStateManager.INSTANCE.setLastSLEReturnServiceProfileName(lastSLEReturnServiceProfileName);

		if (userRafOrRcfServiceInstance != null && userRafOrRcfServiceInstance.getState() != UserServiceState.UNBOUND) {
			logger.error(
					"{} SLE interface return service BIND FAILED. Service already bound. Current service instance state: {}",
					getCurrentConnectionNumberStringForLogging(), userRafOrRcfServiceInstance.getState());
			logger.debug("bind(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface return service BIND is not allowed because already bound. Current service instance state: "
							+ userRafOrRcfServiceInstance.getState());
		}

		final AutoBindSleServiceInstance<SLEUserServiceInstance> autoBindSleServiceInstance = AutoBindSleInstanceFactory
				.getRcfOrRafInstance(proxyFactory, serviceFactory, profile);

		logger.debug("bind(): Invoking the BIND request");
		// Invoke the bind request
		autoBindSleServiceInstance.bind(SLEInterfaceInternalConfigManager.INSTANCE.getReturnBindUnbindTimeoutMillis());
		if (autoBindSleServiceInstance.isSuccessfullyBound()) {
			// Bind succeeded
			userRafOrRcfServiceInstance = autoBindSleServiceInstance.getSuccessfullyBoundInstance();

			returnServiceStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
			currentConnectionNumber = ProxyStateManager.INSTANCE.getNewSLEReturnServiceConnectionNumber();
			messageDistributor.sleReturnProviderStateChange(EReturnActionType.BIND, profileName,
					returnServiceStateChangeTime, currentConnectionNumber);
			logger.info("{} SLE interface return service BIND SUCCEEDED at {}",
					getCurrentConnectionNumberStringForLogging(),
					returnServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
			lastBoundProfileName = profileName;
			lastBoundInterfaceType = interfaceType;
			lastTransferDataTimeSinceLastBind = null;
			transferredDataCountSinceLastBind = 0;
		} else {
			// Bind failed. Throw reason.
			final Map<ProviderHost, Throwable> failureCauses = autoBindSleServiceInstance.getFailureCauses();
			logger.debug("bind(): Throwing the Throwable to abort this method");
			final StringBuilder builder = new StringBuilder("SLE interface return service BIND failed to all defined hosts. ");
			failureCauses.forEach((h, c) -> builder.append("[").append(h.toString()).append("] ").append(c.getMessage()).append(" "));
			throw new Exception(builder.toString());
		}

		// Instantiate the events listener-handler object
		listeningEventsHandler = new SLEInterfaceReturnServiceListeningEventHandler();

		switch (interfaceType) {
			case RETURN_ALL:
				logger.debug("bind(): Adding a new event listener to the RAF service instance");
				((SLEUserRafInstance) userRafOrRcfServiceInstance).addListener(listeningEventsHandler);
				break;
			case RETURN_CHANNEL:
				logger.debug("bind(): Adding a new event listener to the RCF service instance");
				((SLEUserRcfInstance) userRafOrRcfServiceInstance).addListener(listeningEventsHandler);
				break;
			default:
				logger.error("SLE interface return service BIND FAILED. UNEXPECTED INTERFACE TYPE: {}", interfaceType);
				logger.debug("bind(): Throwing an IllegalArgumentException to abort this method");
				throw new IllegalArgumentException(
						"SLE interface return service BIND FAILED. UNEXPECTED INTERFACE TYPE: " + interfaceType);
		}

		// Get the delivery mode
		switch (interfaceType) {
		case RETURN_ALL:
			OperationFuture<RafGetParameterResponse> futureRafGetParameterResult = null;

			// Invoke the get parameter request
			try {
				logger.debug("bind(): Invoking GET PARAMETER request for delivery-mode");
				futureRafGetParameterResult = ((SLEUserRafInstance) userRafOrRcfServiceInstance)
						.getRafParameter(RafParamName.DELIVERY_MODE);

				/*
				 * Await for the get parameter operation to be completed. The
				 * wait time before timeout should be greater than the response
				 * timeout.
				 */
				futureRafGetParameterResult.awaitUninterruptibly(
						SLEInterfaceInternalConfigManager.INSTANCE.getReturnGetParameterTimeoutMillis());

				if (futureRafGetParameterResult.isSuccess()) {
					logger.debug("bind(): GET PARAMETER of delivery-mode SUCCEEDED");
					RafGetParameterResponse getRafParameterResponse = futureRafGetParameterResult.result();
					returnServiceDeliveryMode = getRafParameterResponse.getDeliveryMode();
					// returnServiceDeliveryMode now has the delivery mode value
					logger.info("{} SLE interface return service GET PARAMETER of delivery-mode SUCCEEDED: {}",
							getCurrentConnectionNumberStringForLogging(), returnServiceDeliveryMode);
					// notify web clients of delivery mode change
					messageDistributor.sleDeliveryModeChange(SLEProviderType.RETURN,
							returnServiceDeliveryMode != null ? returnServiceDeliveryMode.toString() : "");

				} else {
					// Get parameter failed. Log the reason.
					logger.warn("{} SLE interface return service GET PARAMETER of delivery-mode FAILED",
							getCurrentConnectionNumberStringForLogging(), futureRafGetParameterResult.cause());
				}

			} catch (Exception e) {
				logger.warn("{} SLE interface return service GET PARAMETER of delivery-mode generated an exception",
						getCurrentConnectionNumberStringForLogging(), e);
			}

			break;
		case RETURN_CHANNEL:
			OperationFuture<RcfGetParameterResponse> futureRcfGetParameterResult = null;

			// Invoke the get parameter request
			try {
				logger.debug("bind(): Invoking GET PARAMETER request for delivery-mode");
				futureRcfGetParameterResult = ((SLEUserRcfInstance) userRafOrRcfServiceInstance)
						.getRcfParameter(RcfParamName.DELIVERY_MODE);

				/*
				 * Await for the get parameter operation to be completed. The
				 * wait time before timeout should be greater than the response
				 * timeout.
				 */
				futureRcfGetParameterResult.awaitUninterruptibly(
						SLEInterfaceInternalConfigManager.INSTANCE.getReturnGetParameterTimeoutMillis());

				if (futureRcfGetParameterResult.isSuccess()) {
					logger.debug("bind(): GET PARAMETER of delivery-mode SUCCEEDED");
					RcfGetParameterResponse getRcfParameterResponse = futureRcfGetParameterResult.result();
					returnServiceDeliveryMode = getRcfParameterResponse.getDeliveryMode();
					// returnServiceDeliveryMode now has the delivery mode value
					logger.info("{} SLE interface return service GET PARAMETER of delivery-mode SUCCEEDED: {}",
							getCurrentConnectionNumberStringForLogging(), returnServiceDeliveryMode);
					// notify web clients of delivery mode change
					messageDistributor.sleDeliveryModeChange(SLEProviderType.RETURN,
							returnServiceDeliveryMode != null ? returnServiceDeliveryMode.toString() : "");

				} else {
					// Get parameter failed. Log the reason.
					logger.warn("{} SLE interface return service GET PARAMETER of delivery-mode FAILED",
							getCurrentConnectionNumberStringForLogging(), futureRcfGetParameterResult.cause());
				}

			} catch (Exception e) {
				logger.warn("{} SLE interface return service GET PARAMETER of delivery-mode generated an exception",
						getCurrentConnectionNumberStringForLogging(), e);
			}

			break;
		default:
			logger.error(
					"SLE interface return service GET PARAMETER of delivery-mode FAILED. UNEXPECTED INTERFACE TYPE: {}",
					interfaceType);
		}

		// Invoke the schedule status report request if the delivery mode is not
		// offline
		if (returnServiceDeliveryMode != null && returnServiceDeliveryMode != ReturnServiceDeliveryMode.OFFLINE) {
			logger.info(
					"{} SLE interface return service delivery-mode is {} so invoking SCHEDULE STATUS REPORT with cycle of {} seconds",
					getCurrentConnectionNumberStringForLogging(), returnServiceDeliveryMode,
					SLEInterfaceInternalConfigManager.INSTANCE.getReturnScheduleStatusReportReportingCycleSeconds());
			OperationFuture<Void> futureStatusReportRequestResult = userRafOrRcfServiceInstance.statusReportRequest(
					SLEInterfaceInternalConfigManager.INSTANCE.getReturnScheduleStatusReportReportingCycleSeconds());

			/*
			 * Await for the schedule status report operation to be completed.
			 * The wait time before timeout should be greater than the response
			 * timeout.
			 */
			futureStatusReportRequestResult.awaitUninterruptibly(
					SLEInterfaceInternalConfigManager.INSTANCE.getReturnScheduleStatusReportTimeoutMillis());

			if (futureStatusReportRequestResult.isSuccess()) {
				// Schedule status report succeeded
				logger.info("{} SLE interface forward service SCHEDULE STATUS REPORT SUCCEEDED",
						getCurrentConnectionNumberStringForLogging());
			} else {
				/*
				 * Schedule status report failed. Don't throw, but log the
				 * reason.
				 */
				logger.error("{} SLE interface forward service SCHEDULE STATUS REPORT FAILED",
						getCurrentConnectionNumberStringForLogging(), futureStatusReportRequestResult.cause());
			}

		} else {
			logger.info("{} SLE interface return service delivery-mode is {} so skipping SCHEDULE STATUS REPORT",
					getCurrentConnectionNumberStringForLogging(), returnServiceDeliveryMode);
		}

		logger.info("{} All BIND-associated operations are now complete", getCurrentConnectionNumberStringForLogging());
	}

	/**
	 * UNBIND from the currently bound SLE return service provider.
	 * 
	 * @throws Throwable
	 *             Thrown when unexpected error is encoutered
	 */
	public synchronized void unbind() throws Throwable {
		logger.info("{} Initiating SLE interface return service UNBIND", getCurrentConnectionNumberStringForLogging());

		if (userRafOrRcfServiceInstance == null || userRafOrRcfServiceInstance.getState() != UserServiceState.READY) {
			logger.error(
					"SLE interface return service UNBIND FAILED. Service not in READY state. Current service instance state: {}",
					(userRafOrRcfServiceInstance != null ? userRafOrRcfServiceInstance.getState()
							: "Service instance not intialized"));
			logger.debug("unbind(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface return service UNBIND is not allowed because not in READY state. Current service instance state: "
							+ (userRafOrRcfServiceInstance != null ? userRafOrRcfServiceInstance.getState()
									: "Service instance not intialized"));
		}

		OperationFuture<Void> futureUnbindResult = null;

		// Invoke the unbind request
		try {
			// Using 'suspend' for all unbinds
			logger.debug("unbind(): Invoking the UNBIND request with SUSPEND");
			futureUnbindResult = userRafOrRcfServiceInstance.unbindRequest(UnbindReason.SUSPEND);
		} catch (Exception e) {
			logger.error(
					"{} SLE interface return service UNBIND request caused an exception. Current servince instance state: {}",
					getCurrentConnectionNumberStringForLogging(), userRafOrRcfServiceInstance.getState(), e);
			logger.debug("unbind(): Throwing a RuntimeException to abort this method");
			throw new RuntimeException(
					"SLE interface return service UNBIND caused an exception. Current service instance state: "
							+ userRafOrRcfServiceInstance.getState(),
					e);
		}

		/*
		 * Await for the unbind operation to be completed. The wait time before
		 * timeout should be greater than the response timeout.
		 */
		futureUnbindResult
				.awaitUninterruptibly(SLEInterfaceInternalConfigManager.INSTANCE.getReturnBindUnbindTimeoutMillis());

		if (futureUnbindResult.isSuccess()) {
			// Unbind succeeded
			returnServiceStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
			messageDistributor.sleReturnProviderStateChange(EReturnActionType.UNBIND, null,
					returnServiceStateChangeTime, null);
			logger.info("{} SLE interface return service UNBIND SUCCEEDED at {}",
					getCurrentConnectionNumberStringForLogging(),
					returnServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
			initializeConnectionNumberAndDeliveryMode();
		} else {
			/*
			 * Unbind failed, but don't automatically throw because DSN provider
			 * seems to respond to UNBIND in a way that causes an error in the
			 * LSE Space's SLE library
			 */
			Throwable failCause = futureUnbindResult.cause();
			logger.error("{} SLE interface return service UNBIND FAILED", getCurrentConnectionNumberStringForLogging(),
					failCause);

			if (userRafOrRcfServiceInstance.getState() != UserServiceState.UNBOUND) {
				logger.debug("unbind(): Service instance state is {}. Throwing the Throwable to abort this method",
						userRafOrRcfServiceInstance.getState());
				throw failCause;
			} else {
				logger.debug("unbind(): But because service instance state is now {}, not throwing",
						userRafOrRcfServiceInstance.getState());
			}

		}

		listeningEventsHandler = null;
		logger.info("All UNBIND-associated operations are now complete");
	}

	/**
	 * START the currently bound SLE return service.
	 * 
	 * @throws Throwable
	 *             Thrown when an unexpected error is encountered
	 */
	public synchronized void start() throws Throwable {
		logger.info("{} Initiating SLE interface return service START", getCurrentConnectionNumberStringForLogging());

		if (userRafOrRcfServiceInstance == null || userRafOrRcfServiceInstance.getState() != UserServiceState.READY) {
			logger.error(
					"{} SLE interface return service START FAILED. Service not in READY state. Current service instance state: {}",
					getCurrentConnectionNumberStringForLogging(), (userRafOrRcfServiceInstance != null
							? userRafOrRcfServiceInstance.getState() : "Service instance not intialized"));
			logger.debug("start(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface forward service START is not allowed because not in READY state. Current service instance state: "
							+ (userRafOrRcfServiceInstance != null ? userRafOrRcfServiceInstance.getState()
									: "Service instance not intialized"));
		} else if (frameQueue == null) {
			logger.error(
					"{} SLE interface return service START FAILED. chill interface downlink seems to be disconnected.",
					getCurrentConnectionNumberStringForLogging());
			logger.debug(
					"start(): frameQueue is null, so preventing START. Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface return service START is not allowed because chill interface downlink seems to be disconnected");
		}

		/*
		 * Fetch the currently bound profile to extract the required START
		 * parameters from
		 */
		ReturnSLEInterfaceProfile profile = (ReturnSLEInterfaceProfile) SLEInterfaceProfileManager.INSTANCE
				.getProfile(lastBoundProfileName);

		if (profile == null) {
			logger.error("{} SLE interface return service START FAILED. Profile \"{}\" is missing.",
					getCurrentConnectionNumberStringForLogging(), lastBoundProfileName);
			logger.debug("start(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException("SLE interface return service START is not allowed because profile \""
					+ lastBoundProfileName + "\" is missing");
		}

		logger.debug("start(): Setting events handler's frame queue to {}",
				Integer.toHexString(System.identityHashCode(frameQueue)));
		listeningEventsHandler.setFrameQueue(frameQueue);

		logger.debug("start(): Invoking the START request");
		// Invoke the start request
		OperationFuture<Void> futureStartResult = null;
		String serviceSpecificLogText = null;

		switch (lastBoundInterfaceType) {
		case RETURN_ALL:
			SLEUserRafInstance userRafServiceInstance = (SLEUserRafInstance) userRafOrRcfServiceInstance;

			/*
			 * Check that the profile's frame quality matches with the service
			 * instance's configured one
			 */
			if (!userRafServiceInstance.getAllowedQualities().contains(profile.getReturnFrameQuality())) {
				logger.error(
						"{} SLE interface return service START FAILED. Profile \"{}\" specifies frame quality that is different than the one originally bound with: {}",
						getCurrentConnectionNumberStringForLogging(), profile.getProfileName(),
						userRafServiceInstance.getAllowedQualities());
				logger.debug("start(): Throwing an IllegalStateException to abort this method");
				throw new IllegalStateException(
						"SLE interface return service START is not allowed because profile \"" + lastBoundProfileName
								+ "\" specifies frame quality that is different than the one originally bound with: "
								+ userRafServiceInstance.getAllowedQualities());
			}

			serviceSpecificLogText = "start-time: " + profile.getStartTime() + ", stop-time: " + profile.getStopTime()
					+ ", requested-frame-quality: " + profile.getReturnFrameQuality();
			futureStartResult = userRafServiceInstance.startRequest(profile.getStartTime(), profile.getStopTime(),
					profile.getReturnFrameQuality());
			break;
		case RETURN_CHANNEL:
			SLEUserRcfInstance userRcfServiceInstance = (SLEUserRcfInstance) userRafOrRcfServiceInstance;

			/*
			 * Check that the profile's GVCID with the service instance's
			 * configured one
			 */
			boolean foundMatchingGVCID = false;
			int configuredSpacecraftId = -1;
			int configuredFrameVersion = -1;
			int configuredVirtualChannel = -1;

			for (GVCID aGVCIDAlreadyConfigured : userRcfServiceInstance.getPermittedGVCIDs()) {
				configuredSpacecraftId = aGVCIDAlreadyConfigured.getSpacecraftId();
				configuredFrameVersion = aGVCIDAlreadyConfigured.getFrameVersion();
				configuredVirtualChannel = aGVCIDAlreadyConfigured.getVirtualChannel();
				logger.trace("start() for loop: userRcfServiceInstance's permitted GVCID is {},{},{}",
						configuredSpacecraftId, configuredFrameVersion, configuredVirtualChannel);

				if (configuredSpacecraftId == profile.getSpacecraftID()
						&& configuredFrameVersion == profile.getFrameVersion()
						&& configuredVirtualChannel == profile.getVirtualChannel()) {
					foundMatchingGVCID = true;
				}

			}

			if (!foundMatchingGVCID) {
				logger.error(
						"{} SLE interface return service START FAILED. Profile \"{}\" specifies global VCID that is different than the one originally bound with: {},{},{}",
						getCurrentConnectionNumberStringForLogging(), profile.getProfileName(), configuredSpacecraftId,
						configuredFrameVersion, configuredVirtualChannel);
				logger.debug("start(): Throwing an IllegalStateException to abort this method");
				throw new IllegalStateException("SLE interface return service START is not allowed because profile \""
						+ lastBoundProfileName
						+ "\" specifies global VCID quality that is different than the one originally bound with: "
						+ configuredSpacecraftId + "," + configuredFrameVersion + "," + configuredVirtualChannel);
			}

			serviceSpecificLogText = "start-time: " + profile.getStartTime() + ", stop-time: " + profile.getStopTime()
					+ ", SCID: " + profile.getSpacecraftID() + ", TFVN: " + profile.getFrameVersion() + ", VCID: "
					+ profile.getVirtualChannel();
			futureStartResult = userRcfServiceInstance.startRequest(profile.getStartTime(), profile.getStopTime(),
					profile.getSpacecraftID(), profile.getFrameVersion(), profile.getVirtualChannel());
			break;
		default:
			logger.error("SLE interface return service START FAILED. UNEXPECTED INTERFACE TYPE: {}",
					lastBoundInterfaceType);
			logger.debug("start(): Throwing an IllegalArgumentException to abort this method");
			throw new IllegalArgumentException(
					"SLE interface return service START FAILED. UNEXPECTED INTERFACE TYPE: " + lastBoundInterfaceType);
		}

		/*
		 * Await for the start operation to be completed. The wait time before
		 * timeout should be greater than the response timeout.
		 */
		futureStartResult
				.awaitUninterruptibly(SLEInterfaceInternalConfigManager.INSTANCE.getReturnStartStopTimeoutMillis());

		if (futureStartResult.isSuccess()) {
			// Start succeeded
			returnServiceStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
			messageDistributor.sleReturnProviderStateChange(EReturnActionType.START, null, returnServiceStateChangeTime,
					null);
			logger.info("{} SLE interface return service START SUCCEEDED at {}. {}",
					getCurrentConnectionNumberStringForLogging(),
					returnServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()),
					serviceSpecificLogText);
		} else {
			// Start failed. Throw reason.
			Throwable failCause = futureStartResult.cause();
			logger.error("{} SLE interface return service START FAILED", getCurrentConnectionNumberStringForLogging(),
					failCause);
			logger.debug("start(): Throwing the Throwable to abort this method");
			throw failCause;
		}

		logger.info("{} All START-associated operations are now complete",
				getCurrentConnectionNumberStringForLogging());
	}

	/**
	 * STOP the currently bound SLE return service.
	 * 
	 * @throws Throwable
	 *             Thrown when an unexpected error is encountered
	 */
	public synchronized void stop() throws Throwable {
		logger.info("{} Initiating SLE interface return service STOP", getCurrentConnectionNumberStringForLogging());

		if (userRafOrRcfServiceInstance == null || userRafOrRcfServiceInstance.getState() != UserServiceState.ACTIVE) {
			logger.error(
					"{} SLE interface return service STOP FAILED. Service not in ACTIVE state. Current service instance state: {}",
					(userRafOrRcfServiceInstance != null ? userRafOrRcfServiceInstance.getState()
							: "Service instance not intialized"));
			logger.debug("stop(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface return service STOP is not allowed because not in ACTIVE state. Current service instance state: "
							+ (userRafOrRcfServiceInstance != null ? userRafOrRcfServiceInstance.getState()
									: "Service instance not intialized"));
		}

		logger.debug("stop(): Invoking the STOP request");
		// Invoke the stop request
		OperationFuture<Void> futureStopResult = userRafOrRcfServiceInstance.stopRequest();

		/*
		 * Await for the stop operation to be completed. The wait time before
		 * timeout should be greater than the response timeout.
		 */
		futureStopResult
				.awaitUninterruptibly(SLEInterfaceInternalConfigManager.INSTANCE.getReturnStartStopTimeoutMillis());

		if (futureStopResult.isSuccess()) {
			// Stop succeeded
			returnServiceStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
			messageDistributor.sleReturnProviderStateChange(EReturnActionType.STOP, null, returnServiceStateChangeTime,
					null);
			logger.info("{} SLE interface return service STOP SUCCEEDED at {}",
					getCurrentConnectionNumberStringForLogging(),
					returnServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
		} else {
			// Stop failed. Throw reason.
			Throwable failCause = futureStopResult.cause();
			logger.error("{} SLE interface return service STOP FAILED", getCurrentConnectionNumberStringForLogging(),
					failCause);
			logger.debug("stop(): Throwing the Throwable to abort this method");
			throw failCause;
		}

		logger.info("{} All STOP-associated operations are now complete", getCurrentConnectionNumberStringForLogging());
	}

	/**
	 * PEER ABORT the current SLE return service.
	 * 
	 * @throws Throwable
	 *             Thrown when an unexpected error is encountered
	 */
	public synchronized void peerAbort() throws Throwable {
		logger.info("{} Initiating SLE interface return service PEER ABORT",
				getCurrentConnectionNumberStringForLogging());

		if (userRafOrRcfServiceInstance == null || userRafOrRcfServiceInstance.getState() == UserServiceState.UNBOUND) {
			logger.error(
					"SLE interface return service PEER ABORT FAILED. Service already unbound. Current service instance state: {}",
					(userRafOrRcfServiceInstance != null ? userRafOrRcfServiceInstance.getState()
							: "Service instance not intialized"));
			logger.debug("peerAbort(): Throwing an IllegalStateException to abort this method");
			throw new IllegalStateException(
					"SLE interface return service PEER ABORT is not allowed because already unbound. Current service instance state: "
							+ (userRafOrRcfServiceInstance != null ? userRafOrRcfServiceInstance.getState()
									: "Service instance not intialized"));
		}

		logger.debug("peerAbort(): Invoking the PEER ABORT request");
		// Invoke the peer abort
		OperationFuture<Void> futurePeerAbortResult = userRafOrRcfServiceInstance.abort();

		/*
		 * Await for the peer abort operation to be completed. The wait time
		 * before timeout should be greater than the response timeout.
		 */
		futurePeerAbortResult
				.awaitUninterruptibly(SLEInterfaceInternalConfigManager.INSTANCE.getReturnPeerAbortTimeoutMillis());

		if (futurePeerAbortResult.isSuccess()) {
			// Peer abort succeeded
			returnServiceStateChangeTime = ZonedDateTime.now(ZoneOffset.UTC);
			messageDistributor.sleReturnProviderStateChange(EReturnActionType.ABORT, null, returnServiceStateChangeTime,
					null);
			logger.info("{} SLE interface return service PEER ABORT SUCCEEDED at {}",
					getCurrentConnectionNumberStringForLogging(),
					returnServiceStateChangeTime.format(DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter()));
			initializeConnectionNumberAndDeliveryMode();
		} else {
			// Peer abort failed. Throw reason.
			Throwable failCause = futurePeerAbortResult.cause();
			logger.error("{} SLE interface return service PEER ABORT FAILED",
					getCurrentConnectionNumberStringForLogging(), failCause);
			logger.debug("peerAbort(): Throwing the Throwable to abort this method");
			throw failCause;
		}

		listeningEventsHandler = null;
		logger.info("All PEER-ABORT-associated operations are now complete");
	}

	/**
	 * Get the current SLE state of the SLE return service.
	 * 
	 * @return The current SLE state
	 */
	public UserServiceState getServiceState() {

		if (userRafOrRcfServiceInstance == null) {
			return UserServiceState.UNBOUND;
		} else {
			return userRafOrRcfServiceInstance.getState();
		}

	}

	/**
	 * Get the timestamp of when the SLE return service changed states.
	 * 
	 * @return The timestamp of when the SLE return service changed states
	 */
	public ZonedDateTime getReturnServiceStateChangeTime() {
		return returnServiceStateChangeTime;
	}

	/**
	 * Atomically increment (by the amount specified) the number of frames
	 * received from the SLE return service and update the time that a frame was
	 * last received with the provided timestamp.
	 * 
	 * @param dataTransferredTime
	 *            Timestamp of when the last frame was received
	 * @param additionalDataTransferredCount
	 *            Number of frames to increment the frame counter by
	 * 
	 */
	public void incrementTransferredDataCount(final ZonedDateTime dataTransferredTime,
			final int additionalDataTransferredCount) {

		synchronized (transferDataTimeAndCountLock) {
			lastTransferDataTimeSinceLastBind = dataTransferredTime;
			transferredDataCountSinceLastBind += additionalDataTransferredCount;
		}

	}

	/**
	 * Get the timestamp of when the last frame was received. Timestamp is
	 * cleared at every BIND.
	 * 
	 * @return The timestamp of when the last frame was received
	 */
	public ZonedDateTime getLastTransferDataTimeSinceLastBind() {
		return lastTransferDataTimeSinceLastBind;
	}

	/**
	 * Get the total count of frames received from the SLE return service.
	 * Counter resets at every BIND.
	 * 
	 * @return The total count of frames received during the current service
	 *         instance
	 */
	public long getTransferredDataCountSinceLastBind() {
		return transferredDataCountSinceLastBind;
	}

	/**
	 * Get the return service delivery mode.
	 * 
	 * @return The return service delivery mode
	 */
	public ReturnServiceDeliveryMode getReturnServiceDeliveryMode() {
		return returnServiceDeliveryMode;
	}

	/**
	 * Sets the object's frames queue.
	 * 
	 * Calling this method with a null value will terminate an active SLE return
	 * service session. It is seen as taking away the pipe to which this object
	 * was feeding the received frames. So in this way, the
	 * <code>BlockingQueue</code> method parameter also serves as a signaling
	 * mechanism.
	 * 
	 * @param frameQueue
	 *            The <code>BlockingQueue</code> object (<code>null</code> is
	 *            also accepted)
	 */
	public void setFrameQueue(final BlockingQueue<TmProductionData> frameQueue) {
		logger.debug("Entered setFrameQueue(frameQueue: )", Integer.toHexString(System.identityHashCode(frameQueue)));

		synchronized (frameQueueLock) {
			this.frameQueue = frameQueue;
		}

		if (frameQueue == null && userRafOrRcfServiceInstance != null
				&& userRafOrRcfServiceInstance.getState() == UserServiceState.ACTIVE) {
			logger.warn(
					"{} chill interface downlink seems to have disconnected. Triggering SLE interface return service STOP.",
					getCurrentConnectionNumberStringForLogging());

			try {
				stop();
			} catch (Throwable e) {
				logger.error("{} Triggered SLE interface return service STOP FAILED",
						getCurrentConnectionNumberStringForLogging(), e);
			}

		}

		if (listeningEventsHandler != null) {
			logger.debug("setFrameQueue(): listeningEventsHandler is not null so setting its frame queue to {}",
					Integer.toHexString(System.identityHashCode(frameQueue)));
			listeningEventsHandler.setFrameQueue(frameQueue);
		} else {
			logger.debug("setFrameQueue(): listeningEventsHandler is null");
		}

		logger.debug("Exiting setFrameQueue()");
	}

	/**
	 * Get the name of the SLE interface profile that is currently bound.
	 * 
	 * @return Name of the SLE interface profile that is currently bound, or
	 *         <code>null</code> if not bound
	 */
	public String getBoundProfileName() {

		if (userRafOrRcfServiceInstance == null || userRafOrRcfServiceInstance.getState() == UserServiceState.UNBOUND) {
			return null;
		} else {
			return lastBoundProfileName;
		}

	}

	/**
	 * Get the current 'R' connection number.
	 * 
	 * @return The current 'R' connection number
	 */
	public long getCurrentConnectionNumber() {
		return currentConnectionNumber;
	}

	/**
	 * Get the current 'R' connection number as a formatted string for the
	 * purpose of logging. The returned string will be either "[R?]" if 'R'
	 * connection number is unknown, or "[R#]" where # is the connection number
	 * if the number is known.
	 * 
	 * @return The 'R' connection number string formatted for logging
	 */
	public String getCurrentConnectionNumberStringForLogging() {

		if (currentConnectionNumber >= 0) {
			return "[R" + currentConnectionNumber + "]";
		} else {
			return "[R?]";
		}

	}

	/**
	 * Get the profile name of the return service that the last BIND was
	 * attempted with.
	 * 
	 * @return The last used SLE return service profile name
	 */
	public String getLastSLEReturnServiceProfileName() {
		return lastSLEReturnServiceProfileName;
	}

	/**
	 * Set the current connection number and delivery mode to initial values.
	 */
	void initializeConnectionNumberAndDeliveryMode() {
		currentConnectionNumber = -1;
		returnServiceDeliveryMode = null;
		
		// notify web clients of delivery mode change
		messageDistributor.sleDeliveryModeChange(SLEProviderType.RETURN, "");
	}

}