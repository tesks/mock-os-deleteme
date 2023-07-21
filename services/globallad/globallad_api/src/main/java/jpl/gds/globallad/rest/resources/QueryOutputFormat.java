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
package jpl.gds.globallad.rest.resources;

/**
 * Enum to define output format from te global lad.
 */
public enum QueryOutputFormat {
	/*
	 * Adding special output format.
	 */
	json("json"), // Default output.
	csv("csv"), // Configurable csv.
	lm_csv("lm_csv") // Special case csv for use with chill_check_channel and Lockheed for Insight
	;

	private String value;

	/**
	 * @param value
	 */
	private QueryOutputFormat(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
