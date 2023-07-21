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
package jpl.gds.shared.exceptions;

import org.springframework.http.HttpStatus;

/**
 * An exception thrown by Telemetry RESTful controllers if they cannot perform the requested operation.
 *
 */
public class RestfulTelemetryException extends RuntimeException {
    private final HttpStatus status;
    private final String     message;

    /**
     * Constructor
     *
     * @param status Error status
     * @param message Error message
     */
    public RestfulTelemetryException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * Getter for status
     * @return  Http status
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Getter for message
     * @return Error Message
     */
    public String getMessage() {
        return message;
    }
}
