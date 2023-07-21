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
package jpl.gds.telem.input.api;

/**
 * Interface containing bean names for the Spring configuration in the tm_input
 * projects.
 * 
 *
 * @since R8
 */
public interface TmInputApiBeans {
    /** Bean name for the TelemetryInputConfig bean */
    public static final String TELEMETRY_INPUT_PROPERTIES = "TELEMETRY_INPUT_PROPERTIES";
    /** Bean name for the RawInputMessenger bean */
    public static final String RAW_INPUT_MESSENGER = "RAW_INPUT_MESSENGER";
    /** Bean name for IRawInputConnection bean */
    public static final String RAW_INPUT_CONNECTION = "RAW_INPUT_CONNECTION";
    /** Bean name for IRawDataProcessor bean */
    public static final String RAW_DATA_PROCESSOR = "RAW_DATA_PROCESSOR";
    /** Bean name for IDataProcessorHelper bean */
    public static final String DATA_PROCESSOR_HELPER = "DATA_PROCESSOR_HELPER";
    /** Bean name for IRawStreamProcessor bean */
    public static final String RAW_STREAM_PROCESSOR = "RAW_STREAM_PROCESSOR";
    /** Bean name for ITelemetryInputMessageFactory bean */
    public static final String TELEM_INPUT_MESSAGE_FACTORY = "TELEM_INPUT_MESSAGE_FACTORY";
    /** Bean name for ITelemetryInputService bean */
    public static final String TELEM_INPUT_SERVICE = "TELEM_INPUT_SERVICE";
    /** Bean name for IParsedFrameFactory bean */
    public static final String PARSED_FRAME_FACTORY = "PARSED_FRAME_FACTORY";
}
