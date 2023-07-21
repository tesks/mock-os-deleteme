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

package jpl.gds.cfdp.processor.controller.action.put;

import cfdp.engine.ampcs.TransIdUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.common.action.EActionCommandType;
import jpl.gds.cfdp.common.action.put.PutActionRequest;
import jpl.gds.cfdp.common.action.put.PutActionResponse;
import jpl.gds.cfdp.processor.ResponseFactory;
import jpl.gds.cfdp.processor.action.disruptor.ActionEvent;
import jpl.gds.cfdp.processor.action.disruptor.ActionRingBufferManager;
import jpl.gds.cfdp.processor.controller.AActionController;
import jpl.gds.cfdp.processor.controller.action.InternalActionRequest;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static jpl.gds.cfdp.common.action.EActionCommandType.PUT;

@RestController
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
@RequestMapping("/cfdp")
@Api(value = "put", tags = {"cfdp-request", "action"})
public class PutActionController extends AActionController {

    @Autowired
    private ResponseFactory responseFactory;

    @Autowired
    private ActionRingBufferManager actionDisruptor;

    @RequestMapping(method = RequestMethod.POST, value = "/action/put")
    @ApiOperation(value = "Initiate Put action")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Put action completed"),
            @ApiResponse(code = 202, message = "Put action request accepted but not yet completed"),
            @ApiResponse(code = 400, message = "Put request is invalid"),
            @ApiResponse(code = 500, message = "CFDP Processor unable to handle this Put request at this time")})
    public ResponseEntity<Object> put(@RequestBody final PutActionRequest req) {

		/* If user supplied a local file to upload to this CFDP Processor, the req object can have a very lengthy
		"uploadFile" field. We don't want that logged and published. So create a clone of the request with that field
		 replaced with an empty array. */
        if (req.getUploadFile() != null) {
            log.info("Filtering out user-supplied upload file bytes from PUT request log entry");
            logAndPublishRequest(PUT, req.cloneWithUploadFileFilteredOut());
        } else {
            logAndPublishRequest(PUT, req);
        }

        final InternalActionRequest<PutActionRequest> iar = new InternalActionRequest<>(PUT, req, getRequestId());
        actionDisruptor.getRingBuffer().publishEvent(ActionEvent::translatePut, iar);
        final ResponseEntity<Object> resp = getResultAndHandleErrors(PUT, iar.getResponseQueue());

        if (resp != null) {
            return resp;
        } else {

            if (getResult() == null) {
                final PutActionResponse putResp = responseFactory.createPutActionResponse(getRequestId(), PUT.name()
                        + " action was accepted but timed out waiting for final result (action may complete later)");
                logAndPublishResult(false, req.getSessionKey(), putResp.getMessage());
                logCommand(req);
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(putResp);
            } else {
                final PutActionResponse putResp = responseFactory.createPutActionResponse(getRequestId(),
                        getResult().getTransIdCreated());
                logAndPublishResult(false, req.getSessionKey(), TransIdUtil.INSTANCE.toString(putResp.getNewTransactionId().get(0),
                        putResp.getNewTransactionId().get(1)));
                logCommand(req);
                return ResponseEntity.status(HttpStatus.OK).body(putResp);

            }

        }

    }

    /**
     * MCSECLIV-965 -> MPCS-12371 1/2022
     * Create a command record for this CFDP request, only if session ID was provided
     * no session ID means command would get logged with the servers context key instead
     * @param request the command request to log
     */
    private void logCommand(PutActionRequest request) {
        if (request.getSessionKey() != null && request.getSessionKey().longValue() > 0) {
            appContext.getBean(IMessagePublicationBus.class).publish(
                    appContext.getBean(ICommandMessageFactory.class).
                            createCfdpCommandMessage(request.toOneLineString(),
                                                     true,
                                                     request.getSessionKey()),
                    true);
        }
    }

    // MPCS-10869  - 06/03/19 - added logAndPublishRequest and logAndPublishResut for session in message support

    @Override
    protected void logAndPublishRequest(final EActionCommandType type, final GenericRequest req) {

        logRequest(type, req);
        publishRequest(req.getRequesterId(),
                httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user",
                httpRequest.getRemoteHost(), ((PutActionRequest) req).getSessionKey(), req.toOneLineString());
    }

    protected void logAndPublishResult(final boolean rejected, final UnsignedLong sessionId, final String resultContent) {
        logResult(rejected, resultContent);
        publishResult(rejected, sessionId, resultContent);
    }

}