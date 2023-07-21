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

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.RestfulTelemetryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * REST Exception handler for Telemetry applications
 *
 */

@ControllerAdvice
public class RestfulTelemetryExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RestfulTelemetryException.class)
    ResponseEntity<?> handleControllerException(RestfulTelemetryException ex) {
        return new ResponseEntity<>(new ApiError(ex.getMessage(), ex.getStatus()), ex.getStatus());
    }


    /**
     * General exception handler for REST resources
     *
     * Intended as a replacement for the following try/catch pattern which was showing up in a lot of places
     *
     * try {
     *     ....
     * } catch () {
     *     new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionTools.getMessage(e))
     * }
     *
     * @param e Exception
     * @return ResponseEntity<?>
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> handleExceptions(final Exception e) {
        if (e instanceof RestfulTelemetryException) {
            return handleControllerException((RestfulTelemetryException) e);
        } else {
            return handleControllerException(
                    new RestfulTelemetryException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            ExceptionTools.getMessage(e)));
        }
    }
}
