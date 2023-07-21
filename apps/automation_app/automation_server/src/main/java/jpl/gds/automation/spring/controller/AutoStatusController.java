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
package jpl.gds.automation.spring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;

@Api(value = "status", tags = "status")
@RestController(value = "status")
public class AutoStatusController extends AutoController {

    /**
     * Gets the "up" status for the AUTO proxy server. Returns OK if the server is up
     * 
     * @return ResponseEntity<Object>
     */
    @ResponseBody
    @ApiResponse(code = 200, message = "Server is up")
    @ApiOperation(value = "Check if the server has started", notes = "No response if the server has not started")
    @GetMapping(value = "status")
    public ResponseEntity<Object> getStatus() {
        return new ResponseEntity<>(HttpStatus.OK.getReasonPhrase(), HttpStatus.OK);
    }

}
