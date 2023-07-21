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
package jpl.gds.common.config.types;

import jpl.gds.shared.config.DynamicEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class has a list  of all the possible types of input that can be
 * provided to the downlink telemetry processor. This class is used to
 * determine how to handle incoming data.
 *
 * We cannot use an Enum here since we want to dynamically extend these values at
 * runtime via property files, see connection.properties
 * connection.flight.downlink.sourceFormat.[CUSTOM_SOURCE_FORMAT].*
 * connection.sse.downlink.sourceFormat.[CUSTOM_SOURCE_FORMAT].*
 *
 * Existing types defined below cannot be updated via property files
 *
 */
public class TelemetryInputType extends DynamicEnum<TelemetryInputType> {
	/** Incoming data format is unknown  */
	public static final TelemetryInputType UNKNOWN = new TelemetryInputType(
			"UNKNOWN", 0, false, false, false, false);

	/** Incoming data format is raw transfer frame */
	public static final TelemetryInputType RAW_TF = new TelemetryInputType(
			"RAW_TF", 1, true, true, true, false);

	/** Incoming data format is raw space or instrument packets */
	public static final TelemetryInputType RAW_PKT = new TelemetryInputType(
			"RAW_PKT", 2, false, false, false, false);

	/** Incoming telemetry format is DSN SFDU transfer frames */
	public static final TelemetryInputType SFDU_TF = new TelemetryInputType(
			"SFDU_TF", 3, false, true, true, true);

	/** Incoming telemetry format is DSN SFDU packets */
	public static final TelemetryInputType SFDU_PKT = new TelemetryInputType(
			"SFDU_PKT", 4, false, false, false, true);

	/** Incoming data format is Command Echo (basically, CLTUs) */
	public static final TelemetryInputType CMD_ECHO = new TelemetryInputType(
			"CMD_ECHO", 5, false, false, false, false);

	/** Incoming data format is Low Earth Orbit transfer frames  */
	public static final TelemetryInputType LEOT_TF = new TelemetryInputType(
			"LEOT_TF", 6, false, true, true, false);

	/** Incoming data format is SLE frames  */
	public static final TelemetryInputType SLE_TF = new TelemetryInputType(
			"SLE_TF", 7, true, true, true, false);

	private boolean needsFrameSync;
	private boolean needsPacketExtract;
	private boolean hasFrames;
	private boolean hasSfdus;

	/**
	 * Constructor
	 *
	 * @param name Name
	 * @param ordinal Ordinal
	 * @param needsFrameSync Indicates if this type calls for frame synchronization
	 * @param needsPacketExtract Indicates if this type calls for packet extraction.
	 * @param hasFrames Indicates if this type encloses frames.
	 * @param hasSfdus  Indicates if this input type is an SFDU format.
	 */
	public TelemetryInputType(final String name, final int ordinal,
	                          final boolean needsFrameSync, final boolean needsPacketExtract,
	                          final boolean hasFrames, final boolean hasSfdus) {

		//will register the value in the superclass map
		//Store keys in uppercase
		super(name.toUpperCase(), ordinal);
		this.needsFrameSync = needsFrameSync;
		this.needsPacketExtract = needsPacketExtract;
		this.hasFrames = hasFrames;
		this.hasSfdus = hasSfdus;

		//overwrite with actual telemetry input type object with properties
		elements.get(getClass()).put(name, this);
	}

	/**
	 * Indicates if this type calls for frame synchronization.
	 *
	 * @return true if the data needs framesync
	 */
	public boolean needsFrameSync() {
		return needsFrameSync;
	}

	/**
	 * Indicates if this type calls for packet extraction.
	 *
	 * @return true if the data needs packet extraction
	 *
	 */
	public boolean needsPacketExtract() {
		return needsPacketExtract;
	}

	/**
	 * Indicates if this type encloses frames.
	 *
	 * @return true if data contains transfer farmes, false if not.
	 *
	 */
	public boolean hasFrames() {
		return hasFrames;
	}

	/**
	 * Indicates if this input type is an SFDU format.
	 *
	 * @return true if SFDU, false if not
	 */
	public boolean hasSfdus() {
		return hasSfdus;
	}

	/**
	 * Get object from string
	 *
	 * @param name Name of option
	 * @return TelemetryInputType object
	 */
	public static TelemetryInputType valueOf(String name) {
		Map<String, DynamicEnum<?>> map = elements.get(TelemetryInputType.class);
		if(map != null && map.containsKey(name)) {
			return (TelemetryInputType) map.get(name);
		}

		throw new IllegalArgumentException("No enum constant " + name);
	}

	/**
	 * Explicit definition of values() is needed here to trigger static initializer.
	 * @return array of TelemetryInputType
	 */
	public static TelemetryInputType[] values() {
		return values(TelemetryInputType.class);
	}

	/**
	 * Get string representation of this enum
	 * @return Comma separated string
	 */
	public static String valuesAsString(){
		List<String> names = new ArrayList<>();
		for(TelemetryInputType type : values()){
			names.add(type.name());
		}
		return String.join(",", names);
	}
}
