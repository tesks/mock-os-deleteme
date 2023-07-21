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
package jpl.gds.cfdp.processor.controller.mtu;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import jpl.gds.cfdp.common.GenericPropertiesResponse;
import jpl.gds.cfdp.processor.ResponseFactory;
import jpl.gds.cfdp.processor.mtu.MessagesToUserMapManager;
import jpl.gds.cfdp.processor.stat.StatManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for querying CFDP Message to User mappings.
 *
 * @since 8.2
 */
@RestController
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
// MPCS-11266  - 09/17/19 : Most CFDP endpoints have lost their "/cfdp/" prefixes.
@RequestMapping("/cfdp")
@DependsOn("configurationManager")
@Api(value = "mtumap", tags = "mtumap")
public class MtuMapController {

	@Autowired
	private MessagesToUserMapManager mtuMapManager;

	@Autowired
	private ResponseFactory responseFactory;

	@RequestMapping(method = RequestMethod.GET, value = "/mtumap")
	@ApiOperation("Query CFDP Processor instance's Message to User mappings")
	@ApiResponse(code = 200, message = "Successfully queried CFDP Processor instance's Message to User Mappings")
	public ResponseEntity<GenericPropertiesResponse> getMtuMap() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(responseFactory.createGenericPropertiesResponse(mtuMapManager.getMtuMapProperties()));
	}

}