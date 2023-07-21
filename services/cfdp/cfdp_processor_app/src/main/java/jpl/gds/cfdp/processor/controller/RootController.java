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

package jpl.gds.cfdp.processor.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import jpl.gds.cfdp.common.config.CfdpCommonProperties;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.ws.rs.core.MediaType;
import java.lang.management.ManagementFactory;

@RestController
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
@RequestMapping("/cfdp")
@EnableSwagger2
@Api(value = "root", tags = {"root"})
public class RootController {
    @Autowired
    ConfigurationManager configurationManager;

    @Autowired
    CfdpCommonProperties cfdpCommonProperties;

    /**
     * Gets information about the running CFDP Processor
     *
     * @return Information about the running CFDP processor
     */
    @GetMapping(value = "/root", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays CFDP Processor instance identifying information")
    @ApiResponse(code = 200, message = "CFDP Processor instance information queried successfully")
    public String getCfdpProcessorInfo() throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final ArrayNode summary = mapper.createArrayNode();

        summary.add(mapper.createObjectNode().put("Instance ID", configurationManager.getInstanceId()));
        summary.add(mapper.createObjectNode().put("PID",ManagementFactory.getRuntimeMXBean().getName().split("@")[0]));
        summary.add(mapper.createObjectNode().put("Context ID",configurationManager.getContextConfig().getContextId().getContextKey().toString()));
        summary.add(mapper.createObjectNode().put("Mnemonic IDs", cfdpCommonProperties.getMnemonicMap().toString()));
        summary.add(mapper.createObjectNode().put("Version", configurationManager.getAppNameVer()));

        return mapper.writer().writeValueAsString(summary);
    }

}