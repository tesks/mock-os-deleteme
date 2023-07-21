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

package jpl.gds.cfdp.processor.controller.files;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import jpl.gds.cfdp.common.config.EConfigurationPropertyKey;
import jpl.gds.cfdp.processor.controller.AActionController;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.shared.exceptions.ExceptionTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Endpoint to be able to query all the available files in the uplink directory, if enabled.
 *
 * TODO: Figure out how to get spring to make this endpoint conditional based on the config
 *
 */
@RestController
// MPCS-11189 - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
// MPCS-11266 - 09/17/19 : Most CFDP endpoints have lost their "/cfdp/" prefixes.
@RequestMapping("/cfdp")
@DependsOn("configurationManager")
@Api(value = "files", tags = {"cfdp-request"})
public class AvailableFileController extends AActionController {

    @Autowired
    private ConfigurationManager configurationManager;


    @RequestMapping(method = RequestMethod.GET, value = "/files")
    @ApiOperation(value = "Query CFDP Processor available files in the uplink directory")
    @ApiResponse(code = 200, message = "Successfully queried CFDP Processor available uplink files")
    public ResponseEntity<Object> getAvailableUplinkFiles() {

        if (!configurationManager.isQueryForAvailableUplinkFilesEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    EConfigurationPropertyKey.AVAILABLE_UPLINK_FILE_QUERY_PROPERTY.toString() + " is not enabled");
        }

        try(Stream<Path> walk = Files.walk(Paths.get(configurationManager.getUplinkFilesTopLevelDirectory()))) {

            List<String> results = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());

            if (results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                                     .body("No files found in " + configurationManager.getUplinkFilesTopLevelDirectory());
            }

            return ResponseEntity.status(HttpStatus.OK).body(results.toArray());
        } catch(final IOException e) {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(ExceptionTools.getMessage(e));
        }
    }
}