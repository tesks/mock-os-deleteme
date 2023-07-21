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

package jpl.gds.cfdp.processor.controller.action.pausetimer;

import static jpl.gds.cfdp.common.action.EActionCommandType.PAUSE_TIMER;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jpl.gds.cfdp.common.action.TransactionIdentifyingActionRequest;
import jpl.gds.cfdp.processor.controller.ATransactionIdentifyingActionController;

@RestController
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
@RequestMapping("/cfdp")
@Api(value = "pausetimer", tags = "action")
public class PauseTimerActionController extends ATransactionIdentifyingActionController {

	@RequestMapping(method = RequestMethod.POST, value = "/action/pausetimer")
	@ApiOperation(value = "Initiate Pause Timer action")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Pause Timer action completed"),
			@ApiResponse(code = 202, message = "Pause Timer action request accepted but not yet completed"),
			@ApiResponse(code = 400, message = "Pause Timer request is invalid"),
			@ApiResponse(code = 500, message = "CFDP Processor unable to handle this Pauser Timer request at this time") })
	public ResponseEntity<Object> pauseTimer(@RequestBody TransactionIdentifyingActionRequest req) {
		return handlePost(PAUSE_TIMER, req);
	}

}