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

import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Wrapper class for all AMPCS Log Markers
 * 
 */
public class Markers {
    private static final IMarkerFactory factory            = MarkerFactory.getIMarkerFactory();

    /** General */
    public static final Marker          GENERAL            = factory.getMarker("GENERAL");
    /** Performance */
    public static final Marker          PERFORMANCE        = factory.getMarker("PERFORMANCE");
    /** Time Correlation */
    public static final Marker          TIME_CORR          = factory.getMarker("TIME_CORR");
    /** Irig Time */
    public static final Marker          IRIG_TIME          = factory.getMarker("IRIG_TIME");
    /** Remote Session End */
    public static final Marker          REMOTE_SESSION_END = factory.getMarker("REMOTE_SESSION_END");

    /** Database */
    public static final Marker          DB                 = factory.getMarker("DB");
    /** Message Bus */
    public static final Marker          BUS                = factory.getMarker("BUS");
    /** Dictionary */
    public static final Marker          DICT               = factory.getMarker("DICT");
    /** PDU */
    public static final Marker          PDU                = factory.getMarker("PDU");
    /** Bad CFDP PDU Header */
    public static final Marker          BAD_PDU_HEAD       = factory.getMarker("BAD_PDU_HEAD");
    /** Bad CFDP PDU Data */
    public static final Marker          BAD_PDU_DATA       = factory.getMarker("BAD_PDU_DATA");

    /** Notification */
    public static final Marker          NOTIFY             = factory.getMarker("NOTIFY");
    /** Syslog/System */
    public static final Marker          SYS                = factory.getMarker("SYS");
    /** Execution */
    public static final Marker          EXECUTE            = factory.getMarker("EXECUTE");

    /** Uplink */
    public static final Marker          UPLINK             = factory.getMarker("UPLINK");
    /** Mtak */
    public static final Marker          MTAK               = factory.getMarker("MTAK");

    /** Bad Packet Header */
    public static final Marker          BAD_PKT_HEAD       = factory.getMarker("BAD_PKT_HEAD");
    /** Bad Packet Data */
    public static final Marker          BAD_PKT_DATA       = factory.getMarker("BAD_PKT_DATA");

    /** Transfer Frame Regression */
    public static final Marker          TF_REG             = factory.getMarker("TF_REG");
    /** Tramsfer Frame Repeat */
    public static final Marker          TF_REPEAT          = factory.getMarker("TF_REP");
    /** Transfer Frame Gap */
    public static final Marker          TF_GAP             = factory.getMarker("TF_GAP");
    /** Bad Transfer Frame */
    public static final Marker          BAD_TF             = factory.getMarker("BAD_TF");
    /** Frame Out of Sync */
    public static final Marker          OUT_OF_SYNC        = factory.getMarker("OUT_OF_SYNC");
    /** Frame In Sync */
    public static final Marker          IN_SYNC            = factory.getMarker("IN_SYNC");
    /** Frame Sync Loss */
    public static final Marker          SYNC_LOSS          = factory.getMarker("SYNC_LOSS");

    /** Connected */
    public static final Marker          CONNECT            = factory.getMarker("CONNECT");
    /** Disconnected */
    public static final Marker          DISCONNECT         = factory.getMarker("DISCONNECT");
    /** Started Processing */
    public static final Marker          START_DATA         = factory.getMarker("START_DATA");
    /** Paused Data */
    public static final Marker          PAUSE_DATA         = factory.getMarker("PAUSE_DATA");
    /** Resume Data */
    public static final Marker          RESUME_DATA        = factory.getMarker("RESUME_DATA");
    /** End of Data */
    public static final Marker          END_DATA           = factory.getMarker("END_DATA");
    /** Stop Data */
    public static final Marker          STOP_DATA          = factory.getMarker("STOP_DATA");

    /** Telemetry Input Summary */
    public static final Marker          INPUT_SUMMARY      = factory.getMarker("INPUT_SUMMARY");
    /** Frame Summary */
    public static final Marker          FRAME_SUMMARY      = factory.getMarker("FRAME_SUMMARY");
    /** Packet Summary */
    public static final Marker          PKT_SUMMARY        = factory.getMarker("PKT_SUMMARY");
    /** User Entry */
    public static final Marker          USER               = factory.getMarker("USER");
    /** RESTful Interface Status */
    public static final Marker          REST               = factory.getMarker("REST");
    /** Suppress from console */
    public static final Marker          SUPPRESS           = factory.getMarker("SUPPRESS");
    /** RESTful Interface Status */
    public static final Marker          SESSION               = factory.getMarker("SESSION");
    /** RESTful Interface Status */
    public static final Marker          CONTEXT               = factory.getMarker("CONTEXT");
    /** Telemetry update Status */
    public static final Marker          TLM                = factory.getMarker("TLM");
    /** SLE */
    public static final Marker          SLE                = factory.getMarker("SLE");


    private Markers() {
    }

    /**
     * Gets the Markers associated with certain log message types
     * 
     * @param type
     *            The LogMessageType of the log message; defaults to GENERIC
     * @return The Markers to be associated with a LogMessageType
     */
    @SuppressWarnings("javadoc")
    public static Marker markFromLogType(final LogMessageType type) {
        switch (type) {
            /** Type value for a generic log message */
            case GENERAL:
                return GENERAL;
            /** Type value for invalid packet header log messages */
            case INVALID_PKT_HEADER:
                return BAD_PKT_HEAD;
            /** Type value for invalid packet data log messages */
            case INVALID_PKT_DATA:
                return BAD_PKT_DATA;
            /** Type value for transfer frame gap log messages */
            case TF_GAP:
                return TF_GAP;
            /** Type value for transfer frame regression log messages */
            case TF_REGRESSION:
                return TF_REG;
            /** Type value for transfer frame repeat log messages */
            case TF_REPEAT:
                return TF_REPEAT;
            /** Type value for invalid transfer frame log messages */
            case INVALID_TF:
                return BAD_TF;
            /** Type value for out-of-sync data log messages */
            case OUT_OF_SYNC_DATA:
                return OUT_OF_SYNC;
            /** Type value for remote session end log messages */
            case REMOTE_SESSION_END:
                return REMOTE_SESSION_END;
            /** Type value for in-sync log messages */
            case IN_SYNC:
                return IN_SYNC;
            /** Type value for sync loss log messages */
            case LOSS_OF_SYNC:
                return SYNC_LOSS;
            /** Type value for frame summary log messages */
            case FRAME_SUMMARY:
                return FRAME_SUMMARY;
            /** Type value for packet summary log messages */
            case PACKET_SUMMARY:
                return PKT_SUMMARY;
            /** Type value for connection log messages */
            case CONNECT:
                return CONNECT;
            /** Type value for disconnect log messages */
            case DISCONNECT:
                return DISCONNECT;
            /** Type value for start-of-data log messages */
            case START_DATA:
                return START_DATA;
            /** Type value for raw input summary log messages */
            case RAW_INPUT_SUMMARY:
                return INPUT_SUMMARY;
            /** Type value for MTAK log messages */
            case MTAK:
                return MTAK;
            /** Type value for IRIG time log messages */
            case IRIG_TIME:
                return IRIG_TIME;
            case BACKLOG_STATUS:
            case BACKLOG_SUMMARY:
                /** Type value for performancemessage */
            case PERFORMANCE:
                return PERFORMANCE;
            /** Type value for uplink log messages */
            case UPLINK:
                return UPLINK;
            /** Type value for data stopped log messages */
            case STOP_PROCESSING:
                return STOP_DATA;
            /** Type value for a pause processing message */
            case PAUSE_PROCESSING:
                return PAUSE_DATA;
            /** Type value for a resume processing message */
            case RESUME_PROCESSING:
                return RESUME_DATA;
            /** Type value for a process running message */
            case RUNNING_PROCESS:
                return EXECUTE;
            /** Type value for an end of telemetry message */
            case END_DATA:
                return END_DATA;
            /** type value for a pdu message */
            case PDU:
                return PDU;
            /** type value for invalid CFDP PDU header log message */
            case INVALID_PDU_HEADER:
                return BAD_PDU_HEAD;
            /** type value for invalid CFDP PDU body log message */
            case INVALID_PDU_DATA:
                return BAD_PDU_DATA;
            /** Type value for an inserter message */
            case INSERTER:
                return DB;
            /** Type value for user-entered log message */    
            case USER:
                return USER;
            /** Type value for SESSION messages */
            case SESSION:
                return SESSION;
            /** Type value for CONTEXT messages */
            case CONTEXT:
                return CONTEXT;
			case REST:
                return REST;
            default:
                return GENERAL;
        }
    }

}
