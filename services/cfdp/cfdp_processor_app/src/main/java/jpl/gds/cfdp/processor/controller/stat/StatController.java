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

package jpl.gds.cfdp.processor.controller.stat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import jpl.gds.cfdp.processor.ResponseFactory;
import jpl.gds.cfdp.processor.stat.StatManager;

@RestController
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
// MPCS-11266  - 09/17/19 : Most CFDP endpoints have lost their "/cfdp/" prefixes.
@RequestMapping("/cfdp")
@DependsOn("configurationManager")
@Api(value = "stat", tags = "stat")
public class StatController {

	@Autowired
	private StatManager statManager;

	@Autowired
	private ResponseFactory responseFactory;

	@RequestMapping(method = RequestMethod.GET, value = "/stat")
	@ApiOperation(value = "Query CFDP Processor instance's state and statistics")
	@ApiResponse(code = 200, message = "Successfully queried CFDP Processor instance's state and statistics")
	public ResponseEntity<Object> getStat() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(responseFactory.createStatResponse(statManager.getStatus(), statManager.getStatistics()));
	}

}