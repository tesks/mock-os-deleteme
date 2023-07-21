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
package jpl.gds.tc.api;

public enum UplinkFailureReason {
	AMPCS_SEND_FAILURE("AMPCS encountered an error while attempting to uplink"),
	AUTHENTICATION_ERROR("Failed to authenticate user"),
	AUTHORIZATION_ERROR("Current user/role not authorized to perform action"),
	COMMAND_SERVICE_REJECTION("The Command Service rejected the request"),
	COMMAND_SERVICE_CONNECTION_ERROR("Unable to contact the command service"),
	UNKNOWN("Unknown error encountered"),
	NONE("No Error");
	
	private String message;

	private UplinkFailureReason(String message) {
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

	public static UplinkFailureReason safeValueOf(String value) {
		try {
			return UplinkFailureReason.valueOf(value);
		} catch (Exception e) {
			return UplinkFailureReason.UNKNOWN;
		}
	}
}
