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
package jpl.gds.telem.input.api.config;

import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.shared.config.DynamicEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Types of telemetry streams accepted by the raw input stream processors.
 *
 * We cannot use an Enum here since we want to dynamically extend these values at runtime
 *
 *
 * MPCS-7677 - 9/16/15. Added javadoc. Added method
 *          to get StreamType given RawInputType.
 *
 * MPCS-11193  - Modified to class that extends DynamicEnum
 */
public class StreamType extends DynamicEnum<StreamType> {
	/** Stream contains SFDU_TF data */
    public static final StreamType SFDU_TF = new StreamType("SFDU_TF", 0);
    /** Stream contains SFDU_PKT data */
    public static final StreamType SFDU_PKT = new StreamType("SFDU_PKT", 1);
	/** Stream contains raw transfer frame data */
    public static final StreamType TRANSFER_FRAME = new StreamType("TRANSFER_FRAME", 2);
    /** Stream contains raw space packet data. */
    public static final StreamType PACKET = new StreamType("PACKET", 3);
    /** Stream contains database records */
    public static final StreamType DATABASE = new StreamType("DATABASE", 4);
    /** Stream contains command echo (CLTU) data */
    public static final StreamType CMD_ECHO = new StreamType("CMD_ECHO", 5);
    /** Stream contains LEO-T transfer frame data */
    public static final StreamType LEOT_TF = new StreamType("LEOT_TF", 6);
    /** Stream contains SLE transfer frame data */
    public static final StreamType SLE_TF = new StreamType("SLE_TF", 7);
    /** Stream content is unknown */
    public static final StreamType UNKNOWN = new StreamType("UNKNOWN", 8);


    /**
     * Get the StreamType enum matching the given TelemetryInputType.
     *
     * @param inputType the telemetry input type to match
     * @return Stream type
     * 
     */
    public static StreamType getStreamType(final TelemetryInputType inputType) {
        if(inputType.equals(TelemetryInputType.RAW_TF)){
            return StreamType.TRANSFER_FRAME;
        }
        else if(inputType.equals(TelemetryInputType.RAW_PKT)){
            return StreamType.PACKET;
        }
        else if(inputType.equals(TelemetryInputType.SFDU_TF)){
            return StreamType.SFDU_TF;
        }
        else if(inputType.equals(TelemetryInputType.SFDU_PKT)){
            return StreamType.SFDU_PKT;
        }
        else if(inputType.equals(TelemetryInputType.CMD_ECHO)){
            return StreamType.CMD_ECHO;
        }
        else if(inputType.equals(TelemetryInputType.LEOT_TF)){
            return StreamType.LEOT_TF;
        }
        else if (inputType.equals(TelemetryInputType.SLE_TF)) {
            return StreamType.SLE_TF;
        }
        //check for dynamic values
        else if(TelemetryInputType.valuesAsString().contains(inputType.name())){
            //create new dynamic stream type
            return new StreamType(inputType.name(), StreamType.values().length);
        }
        else {
            return StreamType.UNKNOWN;
        }
    }

    /**
     * Constructor
     *
     * @param name Name
     * @param ordinal Ordinal
     */
    public StreamType(final String name, final int ordinal) {
        //will register the value in the superclass map
        super(name, ordinal);
    }

    /**
     * Explicit definition of values() is needed here to trigger static initializer.
     * @return array of StreamType
     */
    public static StreamType[] values() {
        return values(StreamType.class);
    }

    /**
     * Get object from string
     *
     * @param name Name of option
     * @return TelemetryInputType object
     */
    public static StreamType valueOf(String name) {
        Map<String, DynamicEnum<?>> map = elements.get(StreamType.class);
        if(map != null && map.containsKey(name)) {
            return (StreamType) map.get(name);
        }

        throw new IllegalArgumentException("No enum constant " + name);
    }

    /**
     * Get string representation of this enum
     * @return Comma separated string
     */
    public static String valuesAsString(){
        List<String> names = new ArrayList<>();
        for(StreamType type : values()){
            names.add(type.name());
        }
        return String.join(",", names);
    }
}
