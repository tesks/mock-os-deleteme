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
package jpl.gds.sleproxy.server.sleinterface.internal.config;

import org.apache.commons.lang3.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This singleton class manages, in-memory, the configuration for the entire SLE
 * interface.
 * 
 */
public enum SLEInterfaceInternalConfigManager {

	/**
	 * The singleton object.
	 */
	INSTANCE;

	/**
	 * File path of the SLE interface configuration file.
	 */
	private String configFilePath;

	/**
	 * Configured SLE forward service version to use.
	 */
	private int forwardServiceVersion;

	/**
	 * Configured forward service BIND and UNBIND timeout threshold, in
	 * milliseconds.
	 */
	private long forwardBindUnbindTimeoutMillis;

	/**
	 * Configured forward service START and STOP timeout threshold, in
	 * milliseconds.
	 */
	private long forwardStartStopTimeoutMillis;

	/**
	 * Configured forward service GET PARAMETER timeout threshold, in
	 * milliseconds.
	 */
	private long forwardGetParameterTimeoutMillis;

	/**
	 * Configured forward service PEER ABORT timeout threshold, in milliseconds.
	 */
	private long forwardPeerAbortTimeoutMillis;

	/**
	 * Configured forward service THROW EVENT timeout threshold, in
	 * milliseconds.
	 */
	private long forwardThrowEventTimeoutMillis;

	/**
	 * Configured forward service THROW EVENT clearance timeout threshold, in
	 * seconds.
	 */
	private int forwardThrowEventClearanceTimeoutSeconds;

	/**
	 * Configured forward service THROW EVENT scheme.
	 */
	private ESLEInterfaceForwardThrowEventScheme forwardThrowEventScheme;

	/**
	 * Configured forward service THROW EVENT's allowable bit rates.
	 */
	private final List<String> forwardThrowEventAllowableBitrates;

	/**
	 * Configured forward service default bit rate.
	 */
	private String forwardDefaultBitrate;

	/**
	 * Configured forward service THROW EVENT's allowable mod-index range.
	 */
	private Range<Integer> forwardThrowEventAllowableModindexRange;

	/**
	 * Configured forward service default mod-index.
	 */
	private int forwardDefaultModindex;

	/**
	 * Configured forward service default command modulation state.
	 */
	private String forwardDefaultCommandModState;

	/**
	 * Configured flag for enabling/disabling throw events for range modulation.
	 */
	private boolean forwardThrowEventRangeModEnable;

	/**
	 * Configured forward service default range modulation state.
	 */
	private String forwardDefaultRangeModState;

	/**
	 * Configured forward service transfer data reporting flag.
	 */
	private boolean forwardTransferDataReportFlag;

	/**
	 * Configured forward service TRANSFER DATA timeout threshold, in
	 * milliseconds.
	 */
	private long forwardTransferDataTimeoutMillis;

	/**
	 * Configured forward service SCHEDULE STATUS REPORT reporting cycle, in
	 * seconds.
	 */
	private int forwardScheduleStatusReportReportingCycleSeconds;

	/**
	 * Configured forward service SCHEDULE STATUS REPORT timeout threshold, in
	 * milliseconds.
	 */
	private long forwardScheduleStatusReportTimeoutMillis;

	/**
	 * Configured timeout threshold for the forward service to wait for
	 * remaining CLTUs to be transferred out once the transferring thread has
	 * been interrupted.
	 */
	private long forwardCLTUTransferrerTerminationTimeoutMillis;

	/**
	 * Configured SLE return service version to use.
	 */
	private int returnServiceVersion;

	/**
	 * Configured return service BIND and UNBIND timeout threshold, in
	 * milliseconds.
	 */
	private long returnBindUnbindTimeoutMillis;

	/**
	 * Configured return service START and STOP timeout threshold, in
	 * milliseconds.
	 */
	private long returnStartStopTimeoutMillis;

	/**
	 * Configured return service GET PARAMETER timeout threshold, in
	 * milliseconds.
	 */
	private long returnGetParameterTimeoutMillis;

	/**
	 * Configured return service PEER ABORT timeout threshold, in milliseconds.
	 */
	private long returnPeerAbortTimeoutMillis;

	/**
	 * Configured return service SCHEDULE STATUS REPORT reporting cycle, in
	 * seconds.
	 */
	private int returnScheduleStatusReportReportingCycleSeconds;

	/**
	 * Configured return service SCHEDULE STATUS REPORT timeout threshold, in
	 * milliseconds.
	 */
	private long returnScheduleStatusReportTimeoutMillis;

	private int deadFactor;

	private int closeAfterPeerAbortTimeoutMillis;

	private int acceptableDelayMillis;

	private int startupTimeoutPeriodMillis;

	private int heartbeatIntervalMillis;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceInternalConfigManager.class);

	/**
	 * Default constructor.
	 */
	SLEInterfaceInternalConfigManager() {
		forwardThrowEventAllowableBitrates = new ArrayList<String>();
	}

	/**
	 * Initialize the configuration manager with the provided configuration
	 * file.
	 * 
	 * @param configFilePath
	 *            Path to the configuration file to initialize the manager with
	 * @throws IOException
	 *             Thrown when there is an error reading the specified
	 *             configuration file
	 */
	public synchronized void init(final String configFilePath) throws IOException {
		logger.debug("Initializing from {}", configFilePath);
		this.configFilePath = configFilePath;

		Properties configProperties = new Properties();
		InputStream is = new FileInputStream(this.configFilePath);
		configProperties.load(is);
		setFromProperties(configProperties);
	}

	/**
	 * Override the configuration with the properties defined in the provided
	 * properties object.
	 * 
	 * @param configProperties
	 *            Configuration properties to update the configuration manager
	 *            with
	 * @throws IllegalArgumentException
	 *             Thrown when <code>null</code> is provided as the properties
	 *             object
	 */
	public synchronized void setFromProperties(final Properties configProperties) throws IllegalArgumentException {

		if (configProperties == null) {
			throw new IllegalArgumentException("Cannot process null properties");
		}

		if (configProperties.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_SERVICE_VERSION.name())) {
			String forwardServiceVersionStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_SERVICE_VERSION.name()).trim();

			try {
				forwardServiceVersion = Integer.valueOf(forwardServiceVersionStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service version value configured: "
								+ forwardServiceVersionStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_BIND_UNBIND_TIMEOUT_MILLIS.name())) {
			String forwardBindUnbindTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_BIND_UNBIND_TIMEOUT_MILLIS.name())
					.trim();

			try {
				forwardBindUnbindTimeoutMillis = Long.valueOf(forwardBindUnbindTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service bind/unbind timeout value configured: "
								+ forwardBindUnbindTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_START_STOP_TIMEOUT_MILLIS.name())) {
			String forwardStartStopTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_START_STOP_TIMEOUT_MILLIS.name())
					.trim();

			try {
				forwardStartStopTimeoutMillis = Long.valueOf(forwardStartStopTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service start/stop timeout value configured: "
								+ forwardStartStopTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_GET_PARAMETER_TIMEOUT_MILLIS.name())) {
			String forwardGetParameterTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_GET_PARAMETER_TIMEOUT_MILLIS.name())
					.trim();

			try {
				forwardGetParameterTimeoutMillis = Long.valueOf(forwardGetParameterTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'get parameter' timeout value configured: "
								+ forwardGetParameterTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_PEER_ABORT_TIMEOUT_MILLIS.name())) {
			String forwardPeerAbortTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_PEER_ABORT_TIMEOUT_MILLIS.name())
					.trim();

			try {
				forwardPeerAbortTimeoutMillis = Long.valueOf(forwardPeerAbortTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'peer abort' timeout value configured: "
								+ forwardPeerAbortTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_TIMEOUT_MILLIS.name())) {
			String forwardThrowEventTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_TIMEOUT_MILLIS.name())
					.trim();

			try {
				forwardThrowEventTimeoutMillis = Long.valueOf(forwardThrowEventTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'throw event' timeout value configured: "
								+ forwardThrowEventTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties.containsKey(
				ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_CLEARANCE_TIMEOUT_SECONDS.name())) {
			String forwardThrowEventClearanceTimeoutSecondsStr = configProperties.getProperty(
					ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_CLEARANCE_TIMEOUT_SECONDS.name())
					.trim();

			try {
				forwardThrowEventClearanceTimeoutSeconds = Integer.valueOf(forwardThrowEventClearanceTimeoutSecondsStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'throw event' clearance timeout value configured: "
								+ forwardThrowEventClearanceTimeoutSecondsStr,
						nfe);
			}

		}

		if (configProperties.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_SCHEME.name())) {
			String forwardThrowEventSchemeStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_SCHEME.name()).trim();

			try {
				forwardThrowEventScheme = ESLEInterfaceForwardThrowEventScheme.valueOf(forwardThrowEventSchemeStr);
			} catch (IllegalArgumentException iae) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'throw event' scheme value configured: "
								+ forwardThrowEventSchemeStr,
						iae);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_ALLOWABLE_BITRATES.name())) {
			String forwardThrowEventAllowableBitratesStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_ALLOWABLE_BITRATES.name())
					.trim();

			// Split a vertical-bar-delimited string while handling all
			// whitespaces
			String[] splitBitrates = forwardThrowEventAllowableBitratesStr.trim().split("\\s*\\|\\s*");
			forwardThrowEventAllowableBitrates.clear();

			for (int i = 0; i < splitBitrates.length; i++) {
				forwardThrowEventAllowableBitrates.add(splitBitrates[i]);
			}

		}

		if (configProperties.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_DEFAULT_BITRATE.name())) {
			forwardDefaultBitrate = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_DEFAULT_BITRATE.name()).trim();
		}

		if (configProperties.containsKey(
				ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_ALLOWABLE_MODINDEX_RANGE.name())) {
			String forwardThrowEventAllowableModindexRangeStr = configProperties.getProperty(
					ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_ALLOWABLE_MODINDEX_RANGE.name())
					.trim();

			// Split a comma-delimited string while handling all whitespaces
			String[] splitModindexRange = forwardThrowEventAllowableModindexRangeStr.trim().split("\\s*\\.\\.\\s*");

			if (splitModindexRange.length != 2) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'throw event' modulation index range value configured: "
								+ forwardThrowEventAllowableModindexRangeStr);
			}

			try {
				int modindexMin = Integer.parseInt(splitModindexRange[0]);
				int modindexMax = Integer.parseInt(splitModindexRange[1]);
				forwardThrowEventAllowableModindexRange = Range.between(modindexMin, modindexMax);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'throw event' modulation index range value configured: ",
						nfe);
			}

		}

		if (configProperties.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_DEFAULT_MODINDEX.name())) {
			String forwardDefaultModindexStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_DEFAULT_MODINDEX.name()).trim();

			try {
				forwardDefaultModindex = Integer.valueOf(forwardDefaultModindexStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service default modulation index value configured: "
								+ forwardDefaultModindexStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_DEFAULT_COMMAND_MOD_STATE.name())) {
			forwardDefaultCommandModState = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_DEFAULT_COMMAND_MOD_STATE.name())
					.trim();
		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_RANGE_MOD_ENABLE.name())) {
			String forwardThrowEventRangeModEnableStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_THROW_EVENT_RANGE_MOD_ENABLE.name())
					.trim();

			try {
				forwardThrowEventRangeModEnable = Boolean.valueOf(forwardThrowEventRangeModEnableStr);
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service enable/disable flag (boolean) of range modulation throw events configured: "
								+ forwardThrowEventRangeModEnableStr,
						e);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_DEFAULT_RANGE_MOD_STATE.name())) {
			forwardDefaultRangeModState = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_DEFAULT_RANGE_MOD_STATE.name())
					.trim();
		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_TRANSFER_DATA_REPORT_FLAG.name())) {
			String forwardTransferDataReportFlagStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_TRANSFER_DATA_REPORT_FLAG.name())
					.trim();

			try {
				forwardTransferDataReportFlag = Boolean.valueOf(forwardTransferDataReportFlagStr);
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'transfer data' report flag (boolean) value configured: "
								+ forwardTransferDataReportFlagStr,
						e);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.FORWARD_TRANSFER_DATA_TIMEOUT_MILLIS.name())) {
			String forwardTransferDataTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.FORWARD_TRANSFER_DATA_TIMEOUT_MILLIS.name())
					.trim();

			try {
				forwardTransferDataTimeoutMillis = Long.valueOf(forwardTransferDataTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'transfer data' timeout value configured: "
								+ forwardTransferDataTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties.containsKey(
				ESLEInterfaceInternalConfigPropertyField.FORWARD_SCHEDULE_STATUS_REPORT_REPORTING_CYCLE_SECONDS
						.name())) {
			String forwardScheduleStatusReportReportingCycleSecondsStr = configProperties.getProperty(
					ESLEInterfaceInternalConfigPropertyField.FORWARD_SCHEDULE_STATUS_REPORT_REPORTING_CYCLE_SECONDS
							.name())
					.trim();

			try {
				forwardScheduleStatusReportReportingCycleSeconds = Integer
						.valueOf(forwardScheduleStatusReportReportingCycleSecondsStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'schedule status report' reporting cycle value configured: "
								+ forwardScheduleStatusReportReportingCycleSecondsStr,
						nfe);
			}

		}

		if (configProperties.containsKey(
				ESLEInterfaceInternalConfigPropertyField.FORWARD_SCHEDULE_STATUS_REPORT_TIMEOUT_MILLIS.name())) {
			String forwardScheduleStatusReportTimeoutMillisStr = configProperties.getProperty(
					ESLEInterfaceInternalConfigPropertyField.FORWARD_SCHEDULE_STATUS_REPORT_TIMEOUT_MILLIS.name())
					.trim();

			try {
				forwardScheduleStatusReportTimeoutMillis = Long.valueOf(forwardScheduleStatusReportTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service 'schedule status report' timeout value configured: "
								+ forwardScheduleStatusReportTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties.containsKey(
				ESLEInterfaceInternalConfigPropertyField.FORWARD_CLTU_TRANSFERRER_TERMINATION_TIMEOUT_MILLIS.name())) {
			String forwardCLTUTransferrerTerminationTimeoutMillisStr = configProperties.getProperty(
					ESLEInterfaceInternalConfigPropertyField.FORWARD_CLTU_TRANSFERRER_TERMINATION_TIMEOUT_MILLIS.name())
					.trim();

			try {
				forwardCLTUTransferrerTerminationTimeoutMillis = Long
						.valueOf(forwardCLTUTransferrerTerminationTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid forward service CLTU transferrer termination timeout value configured: "
								+ forwardCLTUTransferrerTerminationTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties.containsKey(ESLEInterfaceInternalConfigPropertyField.RETURN_SERVICE_VERSION.name())) {
			String returnServiceVersionStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.RETURN_SERVICE_VERSION.name()).trim();

			try {
				returnServiceVersion = Integer.valueOf(returnServiceVersionStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service version value configured: "
								+ returnServiceVersionStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.RETURN_BIND_UNBIND_TIMEOUT_MILLIS.name())) {
			String returnBindUnbindTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.RETURN_BIND_UNBIND_TIMEOUT_MILLIS.name())
					.trim();

			try {
				returnBindUnbindTimeoutMillis = Long.valueOf(returnBindUnbindTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service bind/unbind timeout value configured: "
								+ returnBindUnbindTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.RETURN_START_STOP_TIMEOUT_MILLIS.name())) {
			String returnStartStopTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.RETURN_START_STOP_TIMEOUT_MILLIS.name())
					.trim();

			try {
				returnStartStopTimeoutMillis = Long.valueOf(returnStartStopTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service start/stop timeout value configured: "
								+ returnStartStopTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.RETURN_GET_PARAMETER_TIMEOUT_MILLIS.name())) {
			String returnGetParameterTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.RETURN_GET_PARAMETER_TIMEOUT_MILLIS.name())
					.trim();

			try {
				returnGetParameterTimeoutMillis = Long.valueOf(returnGetParameterTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service 'get parameter' timeout value configured: "
								+ returnGetParameterTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.RETURN_PEER_ABORT_TIMEOUT_MILLIS.name())) {
			String returnPeerAbortTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.RETURN_PEER_ABORT_TIMEOUT_MILLIS.name())
					.trim();

			try {
				returnPeerAbortTimeoutMillis = Long.valueOf(returnPeerAbortTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service 'peer abort' timeout value configured: "
								+ returnPeerAbortTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties.containsKey(
				ESLEInterfaceInternalConfigPropertyField.RETURN_SCHEDULE_STATUS_REPORT_REPORTING_CYCLE_SECONDS
						.name())) {
			String returnScheduleStatusReportReportingCycleSecondsStr = configProperties.getProperty(
					ESLEInterfaceInternalConfigPropertyField.RETURN_SCHEDULE_STATUS_REPORT_REPORTING_CYCLE_SECONDS
							.name())
					.trim();

			try {
				returnScheduleStatusReportReportingCycleSeconds = Integer
						.valueOf(returnScheduleStatusReportReportingCycleSecondsStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service 'schedule status report' reporting cycle value configured: "
								+ returnScheduleStatusReportReportingCycleSecondsStr,
						nfe);
			}

		}

		if (configProperties.containsKey(
				ESLEInterfaceInternalConfigPropertyField.RETURN_SCHEDULE_STATUS_REPORT_TIMEOUT_MILLIS.name())) {
			String returnScheduleStatusReportTimeoutMillisStr = configProperties.getProperty(
					ESLEInterfaceInternalConfigPropertyField.RETURN_SCHEDULE_STATUS_REPORT_TIMEOUT_MILLIS.name())
					.trim();

			try {
				returnScheduleStatusReportTimeoutMillis = Long.valueOf(returnScheduleStatusReportTimeoutMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service 'schedule status report' timeout value configured: "
								+ returnScheduleStatusReportTimeoutMillisStr,
						nfe);
			}

		}

		if (configProperties.containsKey(ESLEInterfaceInternalConfigPropertyField.DEAD_FACTOR.name())) {
			String deadFactorStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.DEAD_FACTOR.name()).trim();

			try {
				deadFactor = Integer.parseInt(deadFactorStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service 'dead factor' timeout value configured: " + deadFactorStr,
						nfe);
			}
		}

		if (configProperties
				.containsKey(ESLEInterfaceInternalConfigPropertyField.CLOSE_AFTER_PEER_ABORT_TIMEOUT_MILLIS.name())) {
			String capTimeoutMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.CLOSE_AFTER_PEER_ABORT_TIMEOUT_MILLIS.name())
					.trim();

			try {
				closeAfterPeerAbortTimeoutMillis = Integer.parseInt(capTimeoutMillisStr);
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service 'close after peer abort' timeout value configured: " + capTimeoutMillisStr,
						nfe);
			}
		}

		if (configProperties.containsKey(ESLEInterfaceInternalConfigPropertyField.ACCEPTABLE_DELAY_MILLIS.name())) {
			String acceptableDelayMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.ACCEPTABLE_DELAY_MILLIS.name()).trim();

			try {
				acceptableDelayMillis = Integer.parseInt(acceptableDelayMillisStr);
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service 'acceptable delay' timeout value configured: " + acceptableDelayMillisStr,
						nfe);
			}
		}

		if (configProperties.containsKey(ESLEInterfaceInternalConfigPropertyField.STARTUP_TIMEOUT_PERIOD_MILLIS.name())) {
			String startupTimeoutPeriodMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.STARTUP_TIMEOUT_PERIOD_MILLIS.name()).trim();

			try {
				startupTimeoutPeriodMillis = Integer.parseInt(startupTimeoutPeriodMillisStr);
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service 'startup timeout period' value configured: " + startupTimeoutPeriodMillisStr,
						nfe);
			}
		}

		if (configProperties.containsKey(ESLEInterfaceInternalConfigPropertyField.HEARTBEAT_INTERVAL_MILLIS.name())) {
			String heartbeatIntervalMillisStr = configProperties
					.getProperty(ESLEInterfaceInternalConfigPropertyField.HEARTBEAT_INTERVAL_MILLIS.name()).trim();

			try {
				heartbeatIntervalMillis = Integer.parseInt(heartbeatIntervalMillisStr);
			} catch (final NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"SLE interface configuration has invalid return service 'heartbeat interval' value configured: " + heartbeatIntervalMillisStr,
						nfe);
			}
		}

	}

	/**
	 * Get the SLE forward service version configured.
	 * 
	 * @return SLE forward service version
	 */
	public int getForwardServiceVersion() {
		return forwardServiceVersion;
	}

	/**
	 * Get the SLE forward service BIND and UNBIND timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE forward service BIND and UNBIND timeout threshold
	 */
	public long getForwardBindUnbindTimeoutMillis() {
		return forwardBindUnbindTimeoutMillis;
	}

	/**
	 * Get the SLE forward service START and STOP timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE forward service START and STOP timeout threshold
	 */
	public long getForwardStartStopTimeoutMillis() {
		return forwardStartStopTimeoutMillis;
	}

	/**
	 * Get the SLE forward service GET PARAMETER timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE forward service GET PARAMETER timeout threshold
	 */
	public long getForwardGetParameterTimeoutMillis() {
		return forwardGetParameterTimeoutMillis;
	}

	/**
	 * Get the SLE forward service PEER ABORT timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE forward service PEER ABORT timeout threshold
	 */
	public long getForwardPeerAbortTimeoutMillis() {
		return forwardPeerAbortTimeoutMillis;
	}

	/**
	 * Get the SLE forward service THROW EVENT timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE forward service THROW EVENT timeout threshold
	 */
	public long getForwardThrowEventTimeoutMillis() {
		return forwardThrowEventTimeoutMillis;
	}

	/**
	 * Get the SLE forward service THROW EVENT clearance timeout threshold, in
	 * seconds.
	 * 
	 * @return SLE forward service THROW EVENT clearance timeout threshold
	 */
	public long getForwardThrowEventClearanceTimeoutSeconds() {
		return forwardThrowEventClearanceTimeoutSeconds;
	}

	/**
	 * Get the SLE forward service THROW EVENT scheme configured.
	 * 
	 * @return SLE forward service THROW EVENT scheme
	 */
	public ESLEInterfaceForwardThrowEventScheme getForwardThrowEventScheme() {
		return forwardThrowEventScheme;
	}

	/**
	 * Get the allowable bit rates configured for the SLE forward service THROW
	 * EVENT.
	 * 
	 * @return The list of bit rates that are allowable
	 */
	public List<String> getForwardThrowEventAllowableBitrates() {
		return forwardThrowEventAllowableBitrates;
	}

	/**
	 * Get the default bit rate configured for the SLE forward service.
	 * 
	 * @return The default bit rate
	 */
	public String getForwardDefaultBitrate() {
		return forwardDefaultBitrate;
	}

	/**
	 * Get the configured range of the mod-index that is allowable for the SLE
	 * forward service THROW EVENT.
	 * 
	 * @return The range of the mod-index that is allowable
	 */
	public Range<Integer> getForwardThrowEventAllowableModindexRange() {
		return forwardThrowEventAllowableModindexRange;
	}

	/**
	 * Get the default mod-index configured for the SLE forward service.
	 * 
	 * @return The default bit rate
	 */
	public int getForwardDefaultModindex() {
		return forwardDefaultModindex;
	}

	/**
	 * Get the default command modulation state configured for the SLE forward
	 * service.
	 * 
	 * @return The default command modulation state value
	 */
	public String getForwardDefaultCommandModState() {
		return forwardDefaultCommandModState;
	}

	/**
	 * Get the configured flag for enabling/disabling range modulation throw
	 * events.
	 * 
	 * @return The flag for enabling/disabling range modulation throw events
	 */
	public boolean getForwardThrowEventRangeModEnable() {
		return forwardThrowEventRangeModEnable;
	}

	/**
	 * Get the default range modulation state configured for the SLE forward
	 * service.
	 * 
	 * @return The default range modulation state value
	 */
	public String getForwardDefaultRangeModState() {
		return forwardDefaultRangeModState;
	}

	/**
	 * Get the configured report flag for the SLE forward service TRANSFER DATA
	 * operation.
	 * 
	 * @return The report flag for SLE forward service TRANSFER DATA
	 */
	public boolean getForwardTransferDataReportFlag() {
		return forwardTransferDataReportFlag;
	}

	/**
	 * Get the SLE forward service TRANSFER DATA timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE forward service TRANSFER DATA timeout threshold
	 */
	public long getForwardTransferDataTimeoutMillis() {
		return forwardTransferDataTimeoutMillis;
	}

	/**
	 * Get the SLE forward service SCHEDULE STATUS REPORT reporting cycle
	 * configured, in seconds.
	 * 
	 * @return SLE forward service SCHEDULE STATUS REPORT reporting cycle
	 */
	public int getForwardScheduleStatusReportReportingCycleSeconds() {
		return forwardScheduleStatusReportReportingCycleSeconds;
	}

	/**
	 * Get the SLE forward service SCHEDULE STATUS REPORT timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE forward service SCHEDULE STATUS REPORT timeout threshold
	 */
	public long getForwardScheduleStatusReportTimeoutMillis() {
		return forwardScheduleStatusReportTimeoutMillis;
	}

	/**
	 * Get the timeout threshold for the SLE forward service to wait for
	 * remaining CLTUs to be transferred after the transferring thread has been
	 * interrupted, in milliseconds
	 * 
	 * @return Timeout threshold for the CLTU transferring thread to stop
	 */
	public long getForwardCLTUTransferrerTerminationTimeoutMillis() {
		return forwardCLTUTransferrerTerminationTimeoutMillis;
	}

	/**
	 * Get the SLE return service version configured.
	 * 
	 * @return SLE return service version
	 */
	public int getReturnServiceVersion() {
		return returnServiceVersion;
	}

	/**
	 * Get the SLE return service BIND and UNBIND timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE return service BIND and UNBIND timeout threshold
	 */
	public long getReturnBindUnbindTimeoutMillis() {
		return returnBindUnbindTimeoutMillis;
	}

	/**
	 * Get the SLE return service START and STOP timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE return service START and STOP timeout threshold
	 */
	public long getReturnStartStopTimeoutMillis() {
		return returnStartStopTimeoutMillis;
	}

	/**
	 * Get the SLE return service GET PARAMETER timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE return service GET PARAMETER timeout threshold
	 */
	public long getReturnGetParameterTimeoutMillis() {
		return returnGetParameterTimeoutMillis;
	}

	/**
	 * Get the SLE return service PEER ABORT timeout threshold, in milliseconds.
	 * 
	 * @return SLE return service PEER ABORT timeout threshold
	 */
	public long getReturnPeerAbortTimeoutMillis() {
		return returnPeerAbortTimeoutMillis;
	}

	/**
	 * Get the SLE return service SCHEDULE STATUS REPORT reporting cycle
	 * configured, in seconds.
	 * 
	 * @return SLE return service SCHEDULE STATUS REPORT reporting cycle
	 */
	public int getReturnScheduleStatusReportReportingCycleSeconds() {
		return returnScheduleStatusReportReportingCycleSeconds;
	}

	/**
	 * Get the SLE return service SCHEDULE STATUS REPORT timeout threshold, in
	 * milliseconds.
	 * 
	 * @return SLE return service SCHEDULE STATUS REPORT timeout threshold
	 */
	public long getReturnScheduleStatusReportTimeoutMillis() {
		return returnScheduleStatusReportTimeoutMillis;
	}

	public int getDeadFactor() {
		return deadFactor;
	}

	public int getCloseAfterPeerAbortTimeoutMillis() {
		return closeAfterPeerAbortTimeoutMillis;
	}

	public int getAcceptableDelayMillis() {
		return acceptableDelayMillis;
	}

	public int getStartupTimeoutPeriodMillis() {
		return startupTimeoutPeriodMillis;
	}

	public int getHeartbeatIntervalMillis() {
		return heartbeatIntervalMillis;
	}

}