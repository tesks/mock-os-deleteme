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

package jpl.gds.telem.common.app.mc.rest.error;

import org.springframework.http.HttpStatus;

/**
 * Class to represent REST API error
 *
 */
public class ApiError {
    private int code;
    private String message;
    private String status;

    /**
     * Constructor
     * @param message Error message
     * @param status HTTP Status
     */
    public ApiError(final String message, final HttpStatus status) {
        this.message = message;
        this.status = status.name();
        this.code = status.value();
    }

    /**
     * Getter for message
     * @return Error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Getter for status
     * @return Error Status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Getter for code
     * @return Status code
     */
    public int getCode(){
        return code;
    }
}
