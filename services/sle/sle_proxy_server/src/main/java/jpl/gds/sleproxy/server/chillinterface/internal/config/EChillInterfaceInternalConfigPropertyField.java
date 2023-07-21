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

/**
 * Enumeration of all the configuration properties definable for the chill
 * interface internal configuration.
 * 
 *
 */
public enum EChillInterfaceInternalConfigPropertyField {

	/**
	 * Configuration property field for uplink total buffer size.
	 */
	UPLINK_TOTAL_BUFFER_SIZE,

	/**
	 * Configuration property field for uplink read buffer size.
	 */
	UPLINK_READ_BUFFER_SIZE,

	/**
	 * Configuration property field for uplink CLTUs buffer capacity.
	 */
	UPLINK_CLTUS_BUFFER_CAPACITY,

	/**
	 * Configuration property field for CLTU start sequence.
	 */
	CLTU_START_SEQUENCE,

	/**
	 * Configuration property field for CLTU tail sequence.
	 */
	CLTU_TAIL_SEQUENCE,

	/**
	 * Configuration property field for CLTU acquisition or idle sequence byte.
	 */
	CLTU_ACQUISITION_OR_IDLE_SEQUENCE_BYTE,

	/**
	 * Configuration property field for downlink frames buffer capacity.
	 */
	DOWNLINK_FRAMES_BUFFER_CAPACITY,

	/**
	 * Configuration property field for downlink ASM header.
	 */
	DOWNLINK_ASM_HEADER,

	/**
	 * Configuration property field for the wait interval after the downlink
	 * client is interrupted, before deciding whether to do a harder disconnect.
	 */
	DOWNLINK_CLIENT_INTERRUPT_WAIT_MILLIS,

	/**
	 * Configuration property field for the frame output format
	 */
	DOWNLINK_OUTPUT_FORMAT


}