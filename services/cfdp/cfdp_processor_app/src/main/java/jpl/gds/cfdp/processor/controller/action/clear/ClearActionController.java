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

package jpl.gds.cfdp.processor.controller.action.clear;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.processor.action.disruptor.ActionEvent;
import jpl.gds.cfdp.processor.action.disruptor.ActionRingBufferManager;
import jpl.gds.cfdp.processor.controller.AActionController;
import jpl.gds.cfdp.processor.controller.action.InternalActionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static jpl.gds.cfdp.common.action.EActionCommandType.CLEAR;

@RestController
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
@RequestMapping("/cfdp")
@Api(value = "clear", tags = {"action"})
public class ClearActionController extends AActionController {

    @Autowired
    private ActionRingBufferManager actionDisruptor;

    @RequestMapping(method = RequestMethod.POST, value = "/action/clear")
    @ApiOperation(value = "Initiate Clear action")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Clear action completed"),
            @ApiResponse(code = 202, message = "Clear action request accepted but not yet completed"),
            @ApiResponse(code = 400, message = "Clear request is invalid"),
            @ApiResponse(code = 500, message = "CFDP Processor unable to handle this Clear request at this time")})
    public ResponseEntity<Object> clear(@RequestBody GenericRequest req) {
        logAndPublishRequest(CLEAR, req);
        InternalActionRequest<GenericRequest> iar = new InternalActionRequest<>(CLEAR, req, getRequestId());
        actionDisruptor.getRingBuffer().publishEvent(ActionEvent::translateGenericAction, iar);
        ResponseEntity<Object> resp = getResultAndHandleErrors(CLEAR, iar.getResponseQueue());

        if (resp != null) {
            return resp;
        } else {
            return createAcceptedOrOkResponse(CLEAR);
        }
    }

}