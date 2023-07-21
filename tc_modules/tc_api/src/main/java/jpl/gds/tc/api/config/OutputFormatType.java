/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tc.api.config;

/**
 * 
 * Defines an enumeration of formats for displaying command arguments.  These fields
 * really only affect displaying command information and are used by some of the uplink
 * GUI classes as well as SCMF reverse.
 * 
 *
 * 11/5/13 - MPCS-5512. Correct static analysis and javadoc issues.
 * 07/01/19 - MPCS-10745 - changed from extending EnumeratedTypes to an actual enum
 */

public enum OutputFormatType {
    /** Enum value for a binary output format */
    BINARY("BIN", 2),
    /** Enum value for an octal output format */
    OCTAL("OCT", 8),
    /** Enum value for a decimal output format */
    DECIMAL("DEC", 10),
    /** Enum value for a hex output format */
    HEXADECIMAL("HEX", 16),
    /** Enum value for a String output format */
    STRING("STR"),
    /** Enum value for a SCLK output format */
    SCLK("SCLK"),
    /** Enum value for a SCET output format */
    SCET("SCET"),
    /** Enum value for an LST output format */
    LST("LST");

    /** static string values for the enumerated values */
    private final String typeString;
    private final int radix;

    OutputFormatType(final String typeString) {
        this(typeString, 0);
    }

    OutputFormatType(final String typeString, final int radix) {
        this.typeString = typeString;
        this.radix = radix;
    }

    /**
     * Get the Radix associated with the particular output type.
     *
     * @return Return the radix for the current type.  If the current type is
     * non-numeric, 0 is returned.
     */
    public int getRadix() {
        return radix;
    }

    @Override
    public String toString() {
        return this.typeString;
    }
}