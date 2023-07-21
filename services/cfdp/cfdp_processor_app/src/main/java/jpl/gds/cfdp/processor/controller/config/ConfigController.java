/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.processor.controller.config;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jpl.gds.cfdp.common.GenericPropertiesResponse;
import jpl.gds.cfdp.common.GenericPropertiesSetRequest;
import jpl.gds.cfdp.processor.ResponseFactory;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.controller.ARequestPublishingController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

@RestController
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
// MPCS-11266  - 09/17/19 : Most CFDP endpoints have lost their "/cfdp/" prefixes.
@RequestMapping("/cfdp")
@DependsOn("configurationManager")
@Api(value = "config", tags = "config")
public class ConfigController extends ARequestPublishingController {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private ResponseFactory responseFactory;

    @Autowired
    private HttpServletRequest httpRequest;

    @RequestMapping(method = RequestMethod.GET, value = "/config")
    @ApiOperation(value = "Query CFDP Processor instance's configuration")
    @ApiResponse(code = 200, message = "Successfully queried CFDP Processor instance's configuration")
    public ResponseEntity<GenericPropertiesResponse> getAllConfigProperties() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(responseFactory.createGenericPropertiesResponse(configurationManager.getProperties()));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/config/{key:.+}")
    @ApiOperation(value = "Query CFDP Processor instance's configuration for the specified item")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully queried CFDP Processor instance's configuration for the specified item"),
            @ApiResponse(code = 404, message = "Specified item was not found in CFDP Processor instance's configuration")})
    public ResponseEntity<Object> getNamedConfigProperty(@PathVariable("key") String key) {
        String value = configurationManager.getProperty(key);

        if (value == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(responseFactory.createNonActionErrorText("Property " + key + " is not defined"));
        } else {
            Properties propertyContainer = new Properties();
            propertyContainer.setProperty(key, value);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseFactory.createGenericPropertiesResponse(propertyContainer));
        }

    }

    @RequestMapping(method = RequestMethod.PUT, value = "/config")
    @ApiOperation(value = "Set CFDP Processor instance's configuration with the provided key-value pairs")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully applied the provided key-value pairs to CFDP Processor instance's configuration"),
            @ApiResponse(code = 400, message = "No key-value pairs were provided")})
    public ResponseEntity<Object> setConfigProperties(@RequestBody GenericPropertiesSetRequest req) {
        log.info("Request " + requestId + ": Configuration set/change initiated by " + req.getRequesterId() + " (" +
                (httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user")
                + "@" + httpRequest.getRemoteHost() + ") [" + req.toOneLineString() + "]");
        publishRequest(req.getRequesterId(),
                httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user",
                httpRequest.getRemoteHost(), req.toOneLineString());

        if (req.getPropertiesToSet() == null || req.getPropertiesToSet().isEmpty()) {
            String resp = responseFactory
                    .createNonActionErrorText("config set parameter cannot be null or empty on HTTP PUT");
            log.debug("Request " + requestId + " result: " + resp);
            publishResult(true, resp);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        } else {
            GenericPropertiesResponse resp = responseFactory
                    .createGenericPropertiesResponse(configurationManager.updateProperties(req.getPropertiesToSet()));
            log.debug("Request " + requestId + " result: " + resp);
            publishResult(false, resp.getFlattenedString());
            return ResponseEntity.status(HttpStatus.OK).body(resp);
        }

    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/config/{key:.+}")
    @ApiOperation(value = "Delete the specified item from CFDP Processor instance's configuration")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully deleted the specified item from CFDP Processor instance's configuration"),
            @ApiResponse(code = 404, message = "The specified item does not exist in CFDP Processor instance's configuration")})
    public ResponseEntity<Object> deleteNamedConfigProperty(@PathVariable("key") String key) {
        log.info("Request " + requestId + ": Configuration delete initiated from "
                + (httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user")
                + "@" + httpRequest.getRemoteHost() + " [" + key + "]");
        publishRequest("(Requester ID unavailable for DELETE)",
                httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user",
                httpRequest.getRemoteHost(), "delete of " + key);
        boolean existed = configurationManager.deleteProperty(key);

        if (existed) {
            log.debug("Request " + requestId + " result: Deleted " + key);
            publishResult(false, "Deleted " + key);
            return ResponseEntity.status(HttpStatus.OK).body(null);
        } else {
            log.debug("Request " + requestId + " result: " + key + " not found");
            publishResult(true, key + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(responseFactory.createNonActionErrorText("Property " + key + " is not defined"));
        }

    }

}