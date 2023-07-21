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

package jpl.gds.shared.string;

/**
 * A POJO to contain a string, to assist in Jackson serialization
 *
 */
public class StringResponse {
    private String value;

    /**
     * Constructor
     * @param str String to encapsulate
     */
    public StringResponse(String str) {
        this.value = str;
    }

    /**
     * Gets POJO value
     * @return Value as string
     */
    public String getValue() {
        return value;
    }
}
