/*
 * Copyright 2006-2020. California Institute of Technology.
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

package jpl.gds.tcapp.app;

/**
 * Exit codes that can be used for applications that require
 * more variation than just the standard 0 or 1.
 *
 */
public enum ReversalErrorCode {

    /** success */
    SUCCESS,
    /** partial success */
    PARTIAL_SUCCESS,
    /** failure */
    FAILURE;

    /**
     * Get value
     * @param code Code
     * @return int
     */
    public static int getValue(ReversalErrorCode code) {
        switch (code) {
            case SUCCESS: return 0;
            case PARTIAL_SUCCESS: return 1;
            default: return 2;
        }
    }
}