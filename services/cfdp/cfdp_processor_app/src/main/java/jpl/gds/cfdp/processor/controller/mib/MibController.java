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

package jpl.gds.cfdp.processor.controller.mib;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jpl.gds.cfdp.common.GenericPropertiesResponse;
import jpl.gds.cfdp.common.GenericPropertiesSetRequest;
import jpl.gds.cfdp.processor.ResponseFactory;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.controller.ARequestPublishingController;
import jpl.gds.cfdp.processor.mib.MibManager;

@RestController
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
// MPCS-11266  - 09/17/19 : Most CFDP endpoints have lost their "/cfdp/" prefixes.
@RequestMapping("/cfdp")
@DependsOn(value = {"mibManager", "configurationManager"})
@Api(value = "mib", tags = "mib")
public class MibController extends ARequestPublishingController {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private MibManager mibManager;

    @Autowired
    ConfigurationManager configurationManager;

    @Autowired
    private ResponseFactory responseFactory;

    @Autowired
    private HttpServletRequest httpRequest;

    @RequestMapping(method = RequestMethod.GET, value = "/mib/local")
    @ApiOperation(value = "Query CFDP Processor instance's MIB settings for local entities")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully queried CFDP Processor instance's MIB settings for local entities"),
            @ApiResponse(code = 404, message = "No local MIB settings exist in the CFDP Processor instance")})
    public ResponseEntity<Object> getAllLocalEntityMibProperties() {

        if (mibManager.getMibLocal().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(responseFactory.createNonActionErrorText("No local entity MIB properties exist"));
        } else {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseFactory.createGenericPropertiesMapResponse(mibManager.getMibLocal()));
        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/mib/remote")
    @ApiOperation(value = "Query CFDP Processor instance's MIB settings for remote entities")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully queried CFDP Processor instance's MIB settings for remote entities"),
            @ApiResponse(code = 404, message = "No remote MIB settings exist in the CFDP Processor instance")})
    public ResponseEntity<Object> getAllRemoteEntityMibProperties() {

        if (mibManager.getMibRemote().isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(responseFactory.createNonActionErrorText("No remote entity MIB properties exist"));
        } else {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseFactory.createGenericPropertiesMapResponse(mibManager.getMibRemote()));
        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/mib/local/{id}")
    @ApiOperation(value = "Query CFDP Processor instance's MIB settings for specified local entity")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully queried CFDP Processor instance's MIB settings for specified local entity"),
            @ApiResponse(code = 404, message = "Specified local entity does not have MIB settings in the CFDP Processor instance")})
    public ResponseEntity<Object> getNamedLocalEntityMibProperties(@PathVariable("id") final String id) {

        if (mibManager.getMibLocal(id) == null || mibManager.getMibLocal(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(responseFactory.createNonActionErrorText("No local entity " + id + " MIB properties exist"));
        } else {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseFactory.createGenericPropertiesResponse(mibManager.getMibLocal(id)));
        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/mib/remote/{id}")
    @ApiOperation(value = "Query CFDP Processor instance's MIB settings for specified remote entity")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully queried CFDP Processor instance's MIB settings for specified remote entity"),
            @ApiResponse(code = 404, message = "Specified remote entity does not have MIB settings in the CFDP Processor instance")})
    public ResponseEntity<Object> getNamedRemoteEntityMibProperties(@PathVariable("id") final String id) {

        if (mibManager.getMibRemote(id) == null || mibManager.getMibRemote(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(responseFactory.createNonActionErrorText("No remote entity " + id + " MIB properties exist"));
        } else {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseFactory.createGenericPropertiesResponse(mibManager.getMibRemote(id)));
        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/mib/local/{id}/{key:.+}")
    @ApiOperation(value = "Query CFDP Processor instance's MIB settings for specified local entity's specificied item")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully queried CFDP Processor instance's MIB settings for specified local entity's specified item"),
            @ApiResponse(code = 404, message = "No MIB setting for the specified item for specified local entity was found in the CFDP Processor instance")})
    public ResponseEntity<Object> getNamedLocalEntityMibProperty(@PathVariable("id") final String id,
                                                                 @PathVariable("key") final String key) {

        if (mibManager.getMibLocal(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(responseFactory.createNonActionErrorText("No local entity " + id + " MIB properties exist"));
        } else if (!mibManager.getMibLocal(id).containsKey(key)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseFactory.createNonActionErrorText(
                    "Local entity " + id + " does not have MIB property " + key + " defined"));
        } else {
            final Properties propertyContainer = new Properties();
            propertyContainer.setProperty(key, mibManager.getMibLocal(id).getProperty(key));
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseFactory.createGenericPropertiesResponse(propertyContainer));
        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/mib/remote/{id}/{key:.+}")
    @ApiOperation(value = "Query CFDP Processor instance's MIB settings for specified remote entity's specificied item")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully queried CFDP Processor instance's MIB settings for specified remote entity's specified item"),
            @ApiResponse(code = 404, message = "No MIB setting for the specified item for specified remote entity was found in the CFDP Processor instance")})
    public ResponseEntity<Object> getNamedRemoteEntityMibProperty(@PathVariable("id") final String id,
                                                                  @PathVariable("key") final String key) {

        if (mibManager.getMibRemote(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(responseFactory.createNonActionErrorText("No remote entity " + id + " MIB properties exist"));
        } else if (!mibManager.getMibRemote(id).containsKey(key)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseFactory.createNonActionErrorText(
                    "Remote entity " + id + " does not have MIB property " + key + " defined"));
        } else {
            final Properties propertyContainer = new Properties();
            propertyContainer.setProperty(key, mibManager.getMibRemote(id).getProperty(key));
            return ResponseEntity.status(HttpStatus.OK)
                    .body(responseFactory.createGenericPropertiesResponse(propertyContainer));
        }

    }

    @RequestMapping(method = RequestMethod.PUT, value = "/mib/local/{id}")
    @ApiOperation(value = "Set CFDP Processor instance's MIB settings for specified local entity with provided key-value pairs")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully set CFDP Processor instance's MIB settings for specified local entity with provided key-value pairs"),
            @ApiResponse(code = 400, message = "No key-value pairs for local entity MIB settings were provided")})
    public ResponseEntity<Object> putNamedLocalEntityMibProperties(@PathVariable("id") final String id,
                                                                   @RequestBody final GenericPropertiesSetRequest req) {
        log.info("Request " + requestId + ": Local entity " + id + " MIB properties update initiated from "
                + (httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user")
                + "@" + httpRequest.getRemoteHost() + " [" + req.toOneLineString() + "]");
        publishRequest(req.getRequesterId(),
                httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user",
                httpRequest.getRemoteHost(), req.toOneLineString());

        if (req.getPropertiesToSet() == null || req.getPropertiesToSet().isEmpty()) {
            final String resp = responseFactory
                    .createNonActionErrorText("mib set parameter cannot be null or empty on HTTP PUT");
            log.debug("Request " + requestId + " result: " + resp);
            publishResult(true, resp);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        } else {
            final GenericPropertiesResponse resp = responseFactory
                    .createGenericPropertiesResponse(mibManager.updateMibLocal(id, req.getPropertiesToSet()));
            log.debug("Request " + requestId + " result: " + resp);
            publishResult(false, resp.getFlattenedString());
            return ResponseEntity.status(HttpStatus.OK).body(resp);
        }

    }

    @RequestMapping(method = RequestMethod.PUT, value = "/mib/remote/{id}")
    @ApiOperation(value = "Set CFDP Processor instance's MIB settings for specified remote entity with provided key-value pairs")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully set CFDP Processor instance's MIB settings for specified remote entity with provided key-value pairs"),
            @ApiResponse(code = 400, message = "No key-value pairs for remote entity MIB settings were provided")})
    public ResponseEntity<Object> putNamedRemoteEntityMibProperties(@PathVariable("id") final String id,
                                                                    @RequestBody final GenericPropertiesSetRequest req) {
        log.info("Request " + requestId + ": Remote entity " + id + " MIB properties update initiated from "
                + (httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user")
                + "@" + httpRequest.getRemoteHost() + " [" + req.toOneLineString() + "]");
        publishRequest(req.getRequesterId(),
                httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user",
                httpRequest.getRemoteHost(), req.toOneLineString());

        if (req.getPropertiesToSet() == null || req.getPropertiesToSet().isEmpty()) {
            final String resp = responseFactory
                    .createNonActionErrorText("mib set parameter cannot be null or empty on HTTP PUT");
            log.debug("Request " + requestId + " result: " + resp);
            publishResult(true, resp);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        } else {
            final GenericPropertiesResponse resp = responseFactory
                    .createGenericPropertiesResponse(mibManager.updateMibRemote(id, req.getPropertiesToSet()));
            log.debug("Request " + requestId + " result: " + resp);
            publishResult(false, resp.getFlattenedString());
            return ResponseEntity.status(HttpStatus.OK).body(resp);
        }

    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/mib/local/{id}")
    @ApiOperation(value = "Delete the MIB settings for specified local entity from CFDP Processor instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully deleted the MIB settings for specified local entity from CFDP Processor instance"),
            @ApiResponse(code = 404, message = "Specified local entity did not have MIB settings in the CFDP Processor instance")})
    public ResponseEntity<Object> deleteNamedLocalEntityMibProperties(@PathVariable("id") final String id) {
        log.info("Request " + requestId + ": Local entity " + id + " MIB properties delete initiated from "
                + (httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user") + "@"
                + httpRequest.getRemoteHost());
        publishRequest("(Requester ID unavailable for DELETE)",
                httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user",
                httpRequest.getRemoteHost(), "delete of local/" + id);

        if (mibManager.deleteMibLocal(id)) {
            log.debug("Request " + requestId + " result: Deleted local entity " + id);
            publishResult(false, "Deleted local entity " + id);
            return ResponseEntity.status(HttpStatus.OK).body(null);
        } else {
            log.debug("Request " + requestId + " result: Local entity " + id + " not found");
            publishResult(true, "Local entity " + id + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(responseFactory.createNonActionErrorText("Local entity " + id + " does not exist in MIB"));
        }

    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/mib/remote/{id}")
    @ApiOperation(value = "Delete the MIB settings for specified remote entity from CFDP Processor instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully deleted the MIB settings for specified remote entity from CFDP Processor instance"),
            @ApiResponse(code = 404, message = "Specified remote entity did not have MIB settings in the CFDP Processor instance")})
    public ResponseEntity<Object> deleteNamedRemoteEntityMibProperties(@PathVariable("id") final String id) {
        log.info("Request " + requestId + ": Remote entity " + id + " MIB properties delete initiated from " +
                (httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user") + "@" +
                httpRequest.getRemoteHost());
        publishRequest("(Requester ID unavailable for DELETE)",
                httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user",
                httpRequest.getRemoteHost(), "delete of remote/" + id);

        if (mibManager.deleteMibRemote(id)) {
            log.debug("Request " + requestId + " result: Deleted remote entity " + id);
            publishResult(false, "Deleted remote entity " + id);
            return ResponseEntity.status(HttpStatus.OK).body(null);
        } else {
            log.debug("Request " + requestId + " result: Remote entity " + id + " not found");
            publishResult(true, "Remote entity " + id + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(responseFactory.createNonActionErrorText("Remote entity " + id + " does not exist in MIB"));
        }

    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/mib/local/{id}/{key:.+}")
    @ApiOperation(value = "Delete the specified MIB setting for specified local entity from CFDP Processor instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully deleted the specified MIB setting for specified local entity from CFDP Processor instance"),
            @ApiResponse(code = 404, message = "Specified MIB setting for specified local entity was not found in the CFDP Processor instance")})
    public ResponseEntity<Object> deleteNamedLocalEntityMibProperty(@PathVariable("id") final String id,
                                                                    @PathVariable("key") final String key) {
        log.info("Request " + requestId + ": Local entity " + id + " MIB property delete initiated from " +
                (httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user") + "@" +
                httpRequest.getRemoteHost() + " [" + key + "]");
        publishRequest("(Requester ID unavailable for DELETE)",
                httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user",
                httpRequest.getRemoteHost(), "delete of local/" + id + "/" + key);

        if (mibManager.deleteMibLocal(id, key)) {
            log.debug("Request " + requestId + " result: Deleted local entity " + id + " property " + key);
            publishResult(false, "Deleted local entity " + id + " property " + key);
            return ResponseEntity.status(HttpStatus.OK).body(null);
        } else {
            log.debug("Request " + requestId + " result: Local entity " + id + " property " + key
                    + " not found");
            publishResult(true, "Local entity " + id + " property " + key + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseFactory
                    .createNonActionErrorText("No local entity " + id + " MIB property " + key + " to delete"));
        }

    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/mib/remote/{id}/{key:.+}")
    @ApiOperation(value = "Delete the specified MIB setting for specified remote entity from CFDP Processor instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully deleted the specified MIB setting for specified remote entity from CFDP Processor instance"),
            @ApiResponse(code = 404, message = "Specified MIB setting for specified remote entity was not found in the CFDP Processor instance")})
    public ResponseEntity<Object> deleteNamedRemoteEntityMibProperty(@PathVariable("id") final String id,
                                                                     @PathVariable("key") final String key) {
        log.info("Request " + requestId + ": Remote entity " + id + " MIB property delete initiated from "
                + (httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user") + "@" +
                httpRequest.getRemoteHost() + " [" + key + "]");
        publishRequest("(Requester ID unavailable for DELETE)",
                httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user",
                httpRequest.getRemoteHost(), "delete of remote/" + id + "/" + key);

        if (mibManager.deleteMibRemote(id, key)) {
            log.debug("Request " + requestId + " result: Deleted remote entity " + id + " property " + key);
            publishResult(false, "Deleted remote entity " + id + " property " + key);
            return ResponseEntity.status(HttpStatus.OK).body(null);
        } else {
            log.debug("Request " + requestId + " result: Remote entity " + id + " property " + key + " not found");
            publishResult(true, "Remote entity " + id + " property " + key + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseFactory
                    .createNonActionErrorText("No remote entity " + id + " MIB property " + key + " to delete"));
        }

    }

}