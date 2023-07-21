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
package jpl.gds.cfdp.common.shutdown;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jpl.gds.cfdp.common.GenericActionResponse;

import java.util.List;

/**
 * {@code ShutdownResponse} is a Jackson model for Spring Boot Actuator's 'shutdown' endpoint response.
 *
 * @since 8.1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShutdownResponse {

	private String message;

	/**
	 * Getter for message.
	 *
	 * @return message
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * Setter for message
	 *
	 * @param message the message to set
	 */
	public void setMessage(final String message) {
		this.message = message;
	}

}
