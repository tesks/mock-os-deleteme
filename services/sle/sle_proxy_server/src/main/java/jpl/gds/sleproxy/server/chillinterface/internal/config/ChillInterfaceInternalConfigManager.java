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
package jpl.gds.sleproxy.server.chillinterface.internal.config;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This singleton class manages the SLE interface configuration in memory.
 *
 */
public enum ChillInterfaceInternalConfigManager {

	/**
	 * The singleton object.
	 */
	INSTANCE;

	/**
	 * Hex object that helps converting hexadecimal character strings to the
	 * actual hex values and vice versa.
	 */
	private final Hex hex;

	/**
	 * File path of the chill interface configuration file.
	 */
	private String configFilePath;

	/**
	 * Configured value for maximum uplink buffer size.
	 */
	private int uplinkTotalBufferSize;

	/**
	 * Configured value for the uplink read buffer size.
	 */
	private int uplinkReadBufferSize;

	/**
	 * Configured value for the uplink CLTUs buffer size.
	 */
	private int uplinkCLTUsBufferCapacity;

	/**
	 * Configured value of the CLTU start sequence/signature.
	 */
	private byte[] cltuStartSequence;

	/**
	 * Configured value of the CLTU tail sequence/signature.
	 */
	private byte[] cltuTailSequence;

	/**
	 * Configured value of the CLTU acquisition or idle sequence/signature.
	 */
	private byte cltuAcquisitionOrIdleSequenceByte;

	/**
	 * Configured value for the downlink frames buffer size.
	 */
	private int downlinkFramesBufferCapacity;

	/**
	 * Configured value of the downlink ASM header.
	 */
	private byte[] downlinkASMHeader;

	/**
	 * Configured value of the wait interval after the downlink client is
	 * interrupted, before deciding whether to do a harder disconnect.
	 */
	private long downlinkClientInterruptWaitMillis;

	private String downlinkOutputFormat;

	/**
	 * Default constructor.
	 */
	private ChillInterfaceInternalConfigManager() {
		hex = new Hex();
	}

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ChillInterfaceInternalConfigManager.class);

	/**
	 * Initialize the configuration manager.
	 *
	 * @param configFilePath
	 *            Configuration file for the manager to parse
	 * @throws IOException
	 *             Thrown when reading the configuration file generates an
	 *             exception
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
	 * Set the configuration properties from the provided properties object.
	 *
	 * @param configProperties
	 *            Configuration properties that will override the current
	 *            configuration in memory
	 * @throws IllegalArgumentException
	 *             Thrown when configProperties is null
	 */
	public synchronized void setFromProperties(final Properties configProperties) throws IllegalArgumentException {

		if (configProperties == null) {
			throw new IllegalArgumentException("Cannot process null properties");
		}

		if (configProperties.containsKey(EChillInterfaceInternalConfigPropertyField.UPLINK_TOTAL_BUFFER_SIZE.name())) {
			String uplinkTotalBufferSizeStr = configProperties
					.getProperty(EChillInterfaceInternalConfigPropertyField.UPLINK_TOTAL_BUFFER_SIZE.name()).trim();

			try {
				uplinkTotalBufferSize = Integer.valueOf(uplinkTotalBufferSizeStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"chill interface internal configuration has invalid uplink total buffer size value configured: "
								+ uplinkTotalBufferSizeStr,
						nfe);
			}

		}

		if (configProperties.containsKey(EChillInterfaceInternalConfigPropertyField.UPLINK_READ_BUFFER_SIZE.name())) {
			String uplinkReadBufferSizeStr = configProperties
					.getProperty(EChillInterfaceInternalConfigPropertyField.UPLINK_READ_BUFFER_SIZE.name()).trim();

			try {
				uplinkReadBufferSize = Integer.valueOf(uplinkReadBufferSizeStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"chill interface internal configuration has invalid uplink read buffer size value configured: "
								+ uplinkReadBufferSizeStr,
						nfe);
			}

		}

		if (configProperties
				.containsKey(EChillInterfaceInternalConfigPropertyField.UPLINK_CLTUS_BUFFER_CAPACITY.name())) {
			String uplinkCLTUsBufferCapacityStr = configProperties
					.getProperty(EChillInterfaceInternalConfigPropertyField.UPLINK_CLTUS_BUFFER_CAPACITY.name()).trim();

			try {
				uplinkCLTUsBufferCapacity = Integer.valueOf(uplinkCLTUsBufferCapacityStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"chill interface internal configuration has invalid uplink CLTUs buffer capacity value configured: "
								+ uplinkCLTUsBufferCapacityStr,
						nfe);
			}

		}

		if (configProperties.containsKey(EChillInterfaceInternalConfigPropertyField.CLTU_START_SEQUENCE.name())) {
			String cltuStartSequenceStr = configProperties
					.getProperty(EChillInterfaceInternalConfigPropertyField.CLTU_START_SEQUENCE.name()).trim();

			try {
				cltuStartSequence = (byte[]) hex.decode(cltuStartSequenceStr);
			} catch (DecoderException de) {
				throw new IllegalArgumentException(
						"chill interface internal configuration has invalid CLTU start sequence value configured: "
								+ cltuStartSequenceStr,
						de);
			}

		}

		if (configProperties.containsKey(EChillInterfaceInternalConfigPropertyField.CLTU_TAIL_SEQUENCE.name())) {
			String cltuTailSequenceStr = configProperties
					.getProperty(EChillInterfaceInternalConfigPropertyField.CLTU_TAIL_SEQUENCE.name()).trim();

			try {
				cltuTailSequence = (byte[]) hex.decode(cltuTailSequenceStr);
			} catch (DecoderException de) {
				throw new IllegalArgumentException(
						"chill interface internal configuration has invalid CLTU tail sequence value configured: "
								+ cltuTailSequenceStr,
						de);
			}

		}

		if (configProperties.containsKey(
				EChillInterfaceInternalConfigPropertyField.CLTU_ACQUISITION_OR_IDLE_SEQUENCE_BYTE.name())) {
			String cltuAcquisitionOrIdleSequenceByteStr = configProperties
					.getProperty(
							EChillInterfaceInternalConfigPropertyField.CLTU_ACQUISITION_OR_IDLE_SEQUENCE_BYTE.name())
					.trim();

			if (cltuAcquisitionOrIdleSequenceByteStr.length() > 2) {
				throw new IllegalArgumentException(
						"chill interface internal configuration has invalid CLTU acquisition/idle sequence byte value configured (cannot be longer than one byte): "
								+ cltuAcquisitionOrIdleSequenceByteStr);
			}

			try {
				cltuAcquisitionOrIdleSequenceByte = ((byte[]) hex.decode(cltuAcquisitionOrIdleSequenceByteStr))[0];
			} catch (DecoderException de) {
				throw new IllegalArgumentException(
						"chill interface internal configuration has invalid CLTU acquisition/idle sequence byte value configured: "
								+ cltuAcquisitionOrIdleSequenceByteStr,
						de);
			}

		}

		if (configProperties
				.containsKey(EChillInterfaceInternalConfigPropertyField.DOWNLINK_FRAMES_BUFFER_CAPACITY.name())) {
			String downlinkFramesBufferCapacityStr = configProperties
					.getProperty(EChillInterfaceInternalConfigPropertyField.DOWNLINK_FRAMES_BUFFER_CAPACITY.name())
					.trim();

			try {
				downlinkFramesBufferCapacity = Integer.valueOf(downlinkFramesBufferCapacityStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"chill interface internal configuration has invalid downlink frames buffer capacity value configured: "
								+ downlinkFramesBufferCapacityStr,
						nfe);
			}

		}

		if (configProperties.containsKey(EChillInterfaceInternalConfigPropertyField.DOWNLINK_ASM_HEADER.name())) {
			String downlinkASMHeaderStr = configProperties
					.getProperty(EChillInterfaceInternalConfigPropertyField.DOWNLINK_ASM_HEADER.name()).trim();

			try {
				downlinkASMHeader = (byte[]) hex.decode(downlinkASMHeaderStr);
			} catch (DecoderException de) {
				throw new IllegalArgumentException(
						"chill interface internal configuration has invalid downlink ASM header value configured: "
								+ downlinkASMHeaderStr,
						de);
			}

		}

		if (configProperties
				.containsKey(EChillInterfaceInternalConfigPropertyField.DOWNLINK_CLIENT_INTERRUPT_WAIT_MILLIS.name())) {
			String downlinkClientInterruptWaitMillisStr = configProperties
					.getProperty(
							EChillInterfaceInternalConfigPropertyField.DOWNLINK_CLIENT_INTERRUPT_WAIT_MILLIS.name())
					.trim();

			try {
				downlinkClientInterruptWaitMillis = Long.valueOf(downlinkClientInterruptWaitMillisStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"chill interface internal configuration has invalid downlink client interrupt wait value configured: "
								+ downlinkClientInterruptWaitMillisStr,
						nfe);
			}

		}
		if (configProperties.containsKey(EChillInterfaceInternalConfigPropertyField.DOWNLINK_OUTPUT_FORMAT.name())) {
			downlinkOutputFormat = configProperties.getProperty(EChillInterfaceInternalConfigPropertyField.DOWNLINK_OUTPUT_FORMAT
					.name());
			if (!(downlinkOutputFormat.equalsIgnoreCase("SLE") || downlinkOutputFormat.equalsIgnoreCase("LEOT"))) {
				// default to acceptable format
				downlinkOutputFormat = "SLE";
			}
		} else {
			// keep backwards compatible with existing configs that dont have this entry
			downlinkOutputFormat = "LEOT";
		}

	}

	/**
	 * Get the configured uplink total buffer size.
	 *
	 * @return Uplink total buffer size
	 */
	public int getUplinkTotalBufferSize() {
		return uplinkTotalBufferSize;
	}

	/**
	 * Get the configured uplink read buffer size.
	 *
	 * @return Uplink read buffer size
	 */
	public int getUplinkReadBufferSize() {
		return uplinkReadBufferSize;
	}

	/**
	 * Get the configured uplink CLTUs buffer capacity.
	 *
	 * @return Uplink CLTUs buffer capacity
	 */
	public int getUplinkCLTUsBufferCapacity() {
		return uplinkCLTUsBufferCapacity;
	}

	/**
	 * Get the configured CLTU start sequence.
	 *
	 * @return CLTU start sequence
	 */
	public byte[] getCLTUStartSequence() {
		return cltuStartSequence;
	}

	/**
	 * Get the configured CLTU tail sequence.
	 *
	 * @return CLTU tail sequence
	 */
	public byte[] getCLTUTailSequence() {
		return cltuTailSequence;
	}

	/**
	 * Get the configured CLTU acquisition or idle sequence byte.
	 *
	 * @return CLTU acquisition or idle sequence byte
	 */
	public byte getCLTUAcquisitionOrIdleSequenceByte() {
		return cltuAcquisitionOrIdleSequenceByte;
	}

	/**
	 * Get the configured downlink frame buffer capacity.
	 *
	 * @return Downlink frame buffer capacity
	 */
	public int getDownlinkFramesBufferCapacity() {
		return downlinkFramesBufferCapacity;
	}

	/**
	 * Get the configured downlink ASM header.
	 *
	 * @return Downlink ASM header
	 */
	public byte[] getDownlinkASMHeader() {
		return downlinkASMHeader;
	}

	/**
	 * Get the configured downlink client interrupt wait interval, in
	 * milliseconds.
	 *
	 * @return The downlink client interrupt wait interval
	 */
	public long getDownlinkClientInterruptWaitMillis() {
		return downlinkClientInterruptWaitMillis;
	}


	/**
	 * Get the configured downlink output format
	 *
	 * @return the downlink output format
	 */
	public String getDownlinkOutputFormat() {
		return downlinkOutputFormat;
	}

}