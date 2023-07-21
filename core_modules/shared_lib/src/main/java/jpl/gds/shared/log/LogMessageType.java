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
package jpl.gds.shared.log;


/**
 * This is an enumeration of possible types of log messages, so specific types
 * of important log entries can be distinguished from others.
 * 
 *
 */
public enum LogMessageType {
	// static integer values for the enumerated values
	/**	Type value for general log messages */
	GENERAL("General"),
	/**	Type value for invalid packet header log messages */
	INVALID_PKT_HEADER("Invalid Packet Header"),
	/**	Type value for invalid packet data log messages */
	INVALID_PKT_DATA("Invalid Packet Data"),
	/**	Type value for transfer frame gap log messages */
	TF_GAP("Transfer Frame Gap"),
	/**	Type value for transfer frame regression log messages */
	TF_REGRESSION("Transfer Frame Regression"),
	/**	Type value for transfer frame repeat log messages */
	TF_REPEAT("Transfer Frame Repeat"),
	/**	Type value for invalid transfer frame log messages */
	INVALID_TF("Bad Transfer Frame"),
	/**	Type value for out-of-sync data log messages */
	OUT_OF_SYNC_DATA("Out of Sync Data"),
	/**	Type value for remote session end log messages */
	REMOTE_SESSION_END("Remote Session End"),
	/**	Type value for in-sync log messages */
	IN_SYNC("In Sync"),
	/**	Type value for sync loss log messages */
	LOSS_OF_SYNC("Loss of Sync"),
	/**	Type value for frame summary log messages */
	FRAME_SUMMARY("Frame Sync Summary"),
	/**	Type value for packet summary log messages */
	PACKET_SUMMARY("Packet Extract Summary"),
	/**	Type value for connection log messages */
	CONNECT("Connect"),
	/**	Type value for disconnect log messages */
	DISCONNECT("Disconnect"),
	/**	Type value for start-of-data log messages */
	START_DATA("Start of Telemetry"),
    @Deprecated
    START_OF_DATA("Start of Data"),
	/**	Type value for raw input summary log messages */
	RAW_INPUT_SUMMARY("Raw Input Summary"),
	/**	Type value for MTAK log messages */
	MTAK("MTAK"),
	/**	Type value for IRIG time log messages */
	IRIG_TIME("IRIG Time Event"),
	/**	Type value for backlog status log messages */
	@Deprecated
	BACKLOG_STATUS("Processing Backlog Status"),
	/**	Type value for backlog summary log messages */
	@Deprecated
	BACKLOG_SUMMARY("Processing Backlog Summary"),
	/** Type value for uplink log messages */
	UPLINK("Uplink"),
	/** Type value for performance log messages */
	PERFORMANCE("Performance"),	
	/** Type value for a stop processing message */
	STOP_PROCESSING("Stop Processing"),
	/** Type value for a pause processing message */
	PAUSE_PROCESSING("Pause Processing"),
	/** Type value for a resume processing message */
	RESUME_PROCESSING("Resume Processing"),
	/** Type value for a process running message */
	RUNNING_PROCESS("Process Running"),
	/** Type value for an end of telemetry message */
    END_DATA("End of Telemetry"),
    @Deprecated
    END_OF_DATA("End of Data"),
    /** Type value for an Inserter message */
    INSERTER("Inserter"),
    /** Type value for a PDU message */
    PDU("PDU"),
    /** Type value for invalid PDU header log message */
    INVALID_PDU_HEADER("Invalid PDU Header"),
    /** Type value for invalid PDU data log message */
    INVALID_PDU_DATA("Invalid PDU Data"),
    /** Type value for USER-entered log */
    USER("User"),
    /** Type value for messages indicating status of RESTful interface */
    REST("REST"),
    /** Type value for SESSION messages */
    SESSION("SESSION"),
	/** Type value for SESSION messages */
	CONTEXT("CONTEXT");

	private String displayString;

	
	/**
	 * 
	 * Creates an instance of LogMessageType.
	 * 
	 * @param strVal
	 *            The initial value of this enumerated type
	 * @throws IllegalArgumentException if the input string cannot be mapped to one of the
	 * enumeration values           
	 */
	private LogMessageType(final String strVal) {
	    this.displayString = strVal;
	}

	/**
	 * Gets the display string for this log type.
	 * 
	 * @return display text
	 */
	public String getValueAsString() {
	    return displayString;
	}
	
	/**
	 * Creates a LogMessageType value from a display string.
	 * 
	 * @param strVal string value 
	 * @return LogMessageType value
	 * 
	 */
	public static LogMessageType fromStringValue(String strVal) {
	    if (strVal == null) {
	        throw new IllegalArgumentException("log type string may ot be null");
	    }
	    strVal = strVal.trim();
	    for (final LogMessageType t: values()) {
	        if (t.getValueAsString().equalsIgnoreCase(strVal)) {
	            return t;
	        }
	    }
	    throw new IllegalArgumentException(strVal + " does not map to a declared log message type");
	}

	@Override
    public String toString() {
	    return getValueAsString();
	}
	
}
