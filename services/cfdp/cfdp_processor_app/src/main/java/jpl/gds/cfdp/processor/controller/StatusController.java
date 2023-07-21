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

package jpl.gds.cfdp.processor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MediaType;
import java.lang.management.ManagementFactory;

/**
 * Class StatusController
 */
@RestController
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
@RequestMapping("/cfdp")
@EnableSwagger2
@Api(value = "status", tags = {"status"})
public class StatusController {

    /**
     * Gets the "up" status for the CFDP Processor. Returns OK if the server is up
     *
     * @return ResponseEntity<Object>
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON)
    @ApiResponse(code = 200, message = "Server is up")
    @ApiOperation(value = "Check if the server has started", notes = "No response if the server has not started")
    public ResponseEntity getStatus() throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final ArrayNode summary = mapper.createArrayNode().add(HttpStatus.OK.getReasonPhrase());
        return new ResponseEntity<>(mapper.writer().writeValueAsString(summary), HttpStatus.OK);
    }
    
}