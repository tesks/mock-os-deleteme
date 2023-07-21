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

package jpl.gds.automation.auto.spring.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import jpl.gds.automation.spring.controller.AutoController;
import jpl.gds.common.config.ConfigurationDumpUtility;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.core.MediaType;
import java.util.Map;

import static jpl.gds.context.cli.app.mc.IRestfulClientCommandLineApp.QUERY_CMD;

/**
 * Rest Controller for AUTO proxy's configuration query API
 *  @since R8
 */
@RestController(value = "query")
@Api(value = "query", tags = "query")
public class AutoConfigController extends AutoController {

    /**
     *
     * QueryAUTO service properties
     *
     * @param regExOrNull a regular expression with which to filter results, e.g.:
     *        - query two properties: /status/properties?filter=mission.spacecraft.ids|time.date.useDoyOutputFormat
     *        - query station properties: /status/properties?filter=stationMap.id.*
     * @param includeDescriptionsOrNull
     *            if true, show descriptions, if false or null, do not show descriptions
     * @param includeSystemOrNull
     *            if true, show System properties, if false or null, do not show System properties
     * @param includeTemplateDirsOrNull
     *            if true, show Template Directories, if false or null, do not show Template Directories
     * @return a Map of property objects that satisfy the request
     */
    @GetMapping(value = QUERY_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays an optionally filtered list of currently active properties.", tags = QUERY_CMD)
    @ApiResponse(code = 200, message = "Query retrieved")
    public Map<String, String> queryConfiguration(
            @RequestParam(value = "filter", required = false) @ApiParam(value = "a regular expression with which to filter results, e.g.:\n"
                    + " - query two properties: /query?filter=mission.spacecraft.ids|time.date"
                    + ".useDoyOutputFormat\n"
                    + " - query station properties: /query?filter=stationMap.id.*") final String regExOrNull,
            @RequestParam(value = "includeDescriptions", required = false) @ApiParam(value = "if true, show descriptions, if false or null, do not show descriptions") final Boolean includeDescriptionsOrNull,
            @RequestParam(value = "includeSystem", required = false) final @ApiParam(value = "if true, show System properties, if false or null, do not show System properties") Boolean includeSystemOrNull,
            @RequestParam(value = "includeTemplateDirs", required = false) @ApiParam(value = "if true, show Template "
                    + "Directories, if false or null, do not show Template Directories") final Boolean includeTemplateDirsOrNull) {

        final boolean includeDescriptions = (includeDescriptionsOrNull != null) && includeDescriptionsOrNull;
        final boolean includeSystem = (includeSystemOrNull != null) && includeSystemOrNull;
        final boolean includeTemplateDirs = (includeTemplateDirsOrNull != null) && includeTemplateDirsOrNull;

        return new ConfigurationDumpUtility(appContext).collectProperties(regExOrNull, includeSystem, includeTemplateDirs,
                                                                          includeDescriptions ? GdsHierarchicalProperties.PropertySet.INCLUDE_DESCRIPTIVES :
                                                                                  GdsHierarchicalProperties.PropertySet.NO_DESCRIPTIVES);
    }
}