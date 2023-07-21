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
 * This is an enumeration of possible severities of LogMessages.
 * 
 *
 */
public enum TraceSeverity {


    /** ALL integer constant */
	ALL("All"),

    /** TRACE integer constant */
	TRACE("Trace"),

    /** DEBUG integer constant */
	DEBUG("Debug"),

    /** INFO integer constant */
	INFO("Info"),

    /** USER integer constant */
    @Deprecated
	USER("User"),
	
	/**
	 * New WARN integer constant, replaces "WARNING", however
	 * the database schema needs "Warning", therefore the displayString MUST remain
	 * "Warning" for the database inserts to continue to work with Log4j2
	 */
	WARN("Warning"),

    /** WARN integer constant */
    @Deprecated
	WARNING("Warning"),

    /** ERROR integer constant */
	ERROR("Error"),

    /** FATAL integer constant */
	@Deprecated
	FATAL("Fatal"),

    /** OFF integer constant */
	OFF("Off");
	
	private String displayString;

	
	/**
	 * 
	 * Creates an instance of LogSeverityType.
	 * 
	 * @param strVal
	 *            The initial value of this enumerated type
	 */
	private TraceSeverity(final String strVal) {
		this.displayString = strVal;
	}

	/**
	 * Gets the display string for this severity level.
	 * 
	 * @return display text
	 */
	public String getValueAsString() {
	    return this.displayString;
	}
	
	@Override
    public String toString() {
	    return getValueAsString();
	}

	/**
	 * Convert from string. As a special case, null, the empty string, and
	 * UNKNOWN are accepted as synonyms for All. This is done for use by
	 * log messages, to support illegal values in the AMPCS database.
	 *
	 * @param strVal the value to convert
	 * @return matching TraceSeverity value
	 */
	public static TraceSeverity fromStringValue(String strVal) {
	    if (strVal != null) {
	        strVal = strVal.trim();
	    }
	    if (strVal == null || strVal.isEmpty() || strVal.equalsIgnoreCase("Unknown")) {
	        return ALL;
	    }
	    for (final TraceSeverity sev: values()) {
            if (sev.getValueAsString().equalsIgnoreCase(strVal) || sev.name().equalsIgnoreCase(strVal)) {
	            return sev;
	        }
	    }
	    throw new IllegalArgumentException(strVal + " is not a valid TraceSeverity string");
	}
}
