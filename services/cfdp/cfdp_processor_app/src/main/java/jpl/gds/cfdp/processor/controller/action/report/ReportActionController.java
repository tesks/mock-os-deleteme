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

package jpl.gds.cfdp.processor.controller.action.report;

import static jpl.gds.cfdp.common.action.EActionCommandType.REPORT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jpl.gds.cfdp.common.action.EActionCommandType;
import jpl.gds.cfdp.common.action.TransactionIdentifyingActionRequest;
import jpl.gds.cfdp.processor.ResponseFactory;
import jpl.gds.cfdp.processor.controller.ATransactionIdentifyingActionController;

@RestController
// MPCS-11189 - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
@RequestMapping("/cfdp")
@Api(value = "report", tags = {"cfdp-request", "action"})
public class ReportActionController extends ATransactionIdentifyingActionController {

	@Autowired
	private ResponseFactory responseFactory;

	@RequestMapping(method = RequestMethod.POST, value = "/action/report")
    @ApiOperation(value = "Initiate Report action")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Report action completed"),
    		@ApiResponse(code = 202, message = "Report action request accepted but not yet completed"),
    		@ApiResponse(code = 400, message = "Report request is invalid"),
            @ApiResponse(code = 500, message = "CFDP Processor unable to handle this Report request at this time") })
	public ResponseEntity<Object> report(@RequestBody TransactionIdentifyingActionRequest req) {
		return handlePost(REPORT, req);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.cfdp.processor.controller.ATransactionIdentifyingActionController#
	 * createAcceptedOrOkResponse(jpl.gds.cfdp.common.action.EActionCommandType)
	 */
	@Override
	protected ResponseEntity<Object> createAcceptedOrOkResponse(EActionCommandType type) {

		if (getResult() == null) {
			return ResponseEntity.status(HttpStatus.ACCEPTED)
					.body(responseFactory.createReportActionResponse(getRequestId(), type.name()
							+ " action was accepted but timed out waiting for final result (action may complete later)"));
		} else {
			return ResponseEntity.status(HttpStatus.OK).body(responseFactory.createReportActionResponse(getRequestId(),
					getResult().getLiveTransactionsReportMap(), getResult().getFinishedTransactionsReportMap()));
		}

	}

}