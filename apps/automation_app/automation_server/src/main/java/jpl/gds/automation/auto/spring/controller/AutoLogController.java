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
package jpl.gds.automation.auto.spring.controller;

import jpl.gds.shared.log.TraceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jpl.gds.automation.auto.AutoManager;
import jpl.gds.automation.auto.AutoProxyException;
import jpl.gds.automation.spring.controller.AutoController;
import jpl.gds.shared.exceptions.ExceptionTools;

@RestController(value = "log")
@Api(value = "log", tags = "log")
public class AutoLogController extends AutoController implements IAutoController {

    /** The AutoManager to use with requests */
    @Autowired
    protected AutoManager manager;

    /**
     * @param level
     *            the level severity to log
     * @param message
     *            the message to log
     * @return ResponseEntity 
     */
    @ResponseBody
    @ApiOperation(value = "Sends a logs message to the Uplink service", notes = "A session must be initialized")
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid user request"),
            @ApiResponse(code = 200, message = "Successfully logged message"),
            @ApiResponse(code = 500, message = "Not connected to a session") })
    @PostMapping(value = "log")
    public ResponseEntity<Object> logMessage(@RequestParam(value = "level") @ApiParam(value = "The log message severity", required = true) final String level,
                                              @RequestParam(value = "message") @ApiParam(value = "The message to log", required = true) final String message) {
        if (!message.isEmpty()) {
            try {
                manager.log(message, TraceManager.mapMtakLevel(level));
            } catch (final AutoProxyException e) {
                return new ResponseEntity<>("Unable to log message: "
                        + ExceptionTools.getMessage(e) + ". Verify the session has been initialized",
                                            HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("AUTO cannot log empty messages", HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.OK.getReasonPhrase(), HttpStatus.OK);
    }

}
