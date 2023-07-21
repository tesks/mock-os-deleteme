/*
 * Copyright 2006-2021. California Institute of Technology.
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
package jpl.gds.station.api.sle;

/**
 * Enum for SLE Frame Quality. Limited to three possible values, per CCSDS RCF Bluebook 911.2-B-3 and CCSDS RAF Bluebook 911.1-B-4
 *
 */
public enum ESleFrameQuality {
    GOOD(0),
    ERRED(1),
    UNDETERMINED(2),
    INVALID(-1);

    private final int frameQuality;

    ESleFrameQuality(int value) {
        frameQuality = value;
    }

    /**
     * @return Int value associated with the quality descriptor
     */
    public int getValue() { return frameQuality; }

    /**
     * Gets an enumeration of ESleFrameQuality
     *
     * @param intVal
     *            The value for this enumeration. Should be a valid SLE frame quality value (0, 1, 2)
     * @return the ESleFrameQuality for the supplied value
     */
    public static ESleFrameQuality getByValue(final int intVal) {
        for (ESleFrameQuality vct : ESleFrameQuality.values()) {
            if (vct.ordinal() == intVal) {

                return vct;
            }
        }
        return INVALID;
    }
}
