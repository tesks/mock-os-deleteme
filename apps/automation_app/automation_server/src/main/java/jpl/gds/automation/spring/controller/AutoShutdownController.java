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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "shutdown", tags = "shutdown")
@RestController(value = "shutdown")
public class AutoShutdownController extends AutoController {

    @Autowired
    ApplicationContext appContext;

    /**
     * Sends a shutdown request to the AUTO proxy server.
     */
    @ApiOperation(value = "Shutdown the server", notes = "No response if the server has not started")
    @PostMapping(value = "shutdown")
    public void execShutdown() {
        log.info("Received shutdown request");
        SpringApplication.exit(appContext, () -> 0); // cfdp server uses spring shutdown hook
    }

}
