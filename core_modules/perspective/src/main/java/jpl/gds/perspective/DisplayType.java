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
package jpl.gds.perspective;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.shared.types.EnumeratedType;

/**
 * An enumeration of display types in a user perspective.
 * 
 */
public class DisplayType extends EnumeratedType {
    // --------------------------------------------------
    // Add new types here
    // static integer values for the enumerated values
    // must map directly to types
    /**
     * Display type is not set
     */
    static public final int UNKNOWN_DISPLAY = 0;
    
    /**
     * Monitor display for telemetry visualization
     */
    static public final int MESSAGE_DISPLAY = 1;
    
    /**
     * FSW downlink display for fetching telemetry from the spacecraft
     */
    static public final int DOWN_DISPLAY = 2;
    
    /**
     * Uplink display for sending commands to the spacecraft
     */
    static public final int UPLINK_DISPLAY = 3;
    
    /**
     * SSE downlink display for fetching telemetry from the spacecraft
     */
    static public final int SSE_DOWN_DISPLAY = 4;

    /**
     * static string values for the enumerated values
     */
    @SuppressWarnings( { "MALICIOUS_CODE", "MS_PKGPROTECT" })
    static public final String[] displayTypes = { "UNKNOWN", "MESSAGE_DISPLAY",
            "DOWN_DISPLAY", "UPLINK_DISPLAY", "SSE_DOWN_DISPLAY" };

    /**
     * static string values for the "pretty" display values
     */
    @SuppressWarnings( { "MALICIOUS_CODE", "MS_PKGPROTECT" })
    static public final String[] fancyDisplayTypes = { "Unknown", "Monitor",
            "Downlink", "Uplink", "SSE Downlink" };

    /**
     * Convenience instance for the unknown display type
     */
    public static final DisplayType UNKNOWN = new DisplayType(UNKNOWN_DISPLAY);
    
    /**
     * Convenience instance for the monitor display type
     */
    public static final DisplayType MESSAGE = new DisplayType(MESSAGE_DISPLAY);

    /**
     * Convenience instance for the FSW downlink display type
     */
    public static final DisplayType FSW_DOWN = new DisplayType(DOWN_DISPLAY);
    
    /**
     * Convenience instance for the uplink display type
     */
    public static final DisplayType UPLINK = new DisplayType(UPLINK_DISPLAY);
    
    /**
     * Convenience instance for the SSE downlink display type
     */
    public static final DisplayType SSE_DOWN = new DisplayType(SSE_DOWN_DISPLAY);
    
    // End of add new types
    // -------------------------------------------------
    
    /**
     * Creates an instance of DisplayType.
     * 
     * @param intVal
     *            the integer value constant from this class
     * @throws IllegalArgumentException
     *             if the supplied argument does not map to a known display type
     */
    public DisplayType(final int intVal) throws IllegalArgumentException {
        super(intVal);
    }

    /**
     * Creates an instance of DisplayType.
     * 
     * @param stringVal
     *            the string value constant from this class
     * @throws IllegalArgumentException
     *             if the supplied argument does not map to a known display type
     */
    public DisplayType(final String stringVal) throws IllegalArgumentException {
        super(stringVal);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.types.EnumeratedType#getStringValue(int)
     */
    @Override
    protected String getStringValue(final int index) {
        if (index < 0 || index > getMaxIndex()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (displayTypes[index]);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.types.EnumeratedType#getMaxIndex()
     */
    @Override
    protected int getMaxIndex() {
        return (displayTypes.length - 1);
    }

    /**
     * Gets a "nice" display name for this display type.
     * 
     * @return the display name
     */
    public String getFancyDisplayName() {
        return (fancyDisplayTypes[this.getValueAsInt()]);
    }

    /**
     * Generates an XML representation of the DisplayType
     * 
     * @return the XML string
     */
    public String toXML() {
        final StringBuffer buf = new StringBuffer();
        buf.append("<displayType> name=" + this.getValueAsString()
                + " </displayType>");
        return buf.toString();
    }
}
