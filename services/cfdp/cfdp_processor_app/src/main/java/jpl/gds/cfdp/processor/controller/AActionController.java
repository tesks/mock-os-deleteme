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

import cfdp.engine.ampcs.RequestResult;
import jpl.gds.cfdp.common.GenericActionResponse;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.common.action.EActionCommandType;
import jpl.gds.cfdp.processor.ResponseFactory;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.shared.exceptions.ExceptionTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static jpl.gds.cfdp.common.action.EActionCommandType.REPORT;

/**
 * Class AActionController
 * MPCS-10869  - 06/03/19 - pulled out logic for logging request and logging result to new functions.
 */
public abstract class AActionController extends ARequestPublishingController {

    /* MPCS-11233 - 9/12/2019
    Not all action requests should be logged in INFO level. This lookup table will indicate which actions should be
    logged in DEBUG level instead of INFO. */
    private final static Set<EActionCommandType> requestsToLogAsDebug = new HashSet<>();

    static {
        requestsToLogAsDebug.add(REPORT);
    }


    // Save for logging result
    private EActionCommandType type;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private ResponseFactory responseFactory;

    @Autowired
    protected HttpServletRequest httpRequest;

    private RequestResult result = null;

    /**
     * @return the requestId
     */
    protected String getRequestId() {
        return requestId;
    }

    /**
     * @return the result
     */
    protected RequestResult getResult() {
        return result;
    }

    
    protected void logRequest(final EActionCommandType type, final GenericRequest req) {
        this.type = type;
        final String logMessage = "Request " + requestId + ": " + type.name() + " action requested by " + req.getRequesterId() + " ("
                + (httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user")
                + "@" + httpRequest.getRemoteHost() + ") [" + req.toOneLineString() + "]";

        if (requestsToLogAsDebug.contains(type)) {
            log.debug(logMessage);
        } else {
            log.info(logMessage);
        }

    }
    
    protected void logAndPublishRequest(final EActionCommandType type, final GenericRequest req) {
        logRequest(type, req);
        publishRequest(req.getRequesterId(),
                httpRequest.getRemoteUser() != null ? httpRequest.getRemoteUser() : "unauthenticated-user",
                httpRequest.getRemoteHost(), req.toOneLineString());
    }

    protected void logResult(final boolean rejected, final String resultContent) {
        log.debug("Request " + requestId + " result: " + type != null? type.name() : "UNKNOWN" + " action "
                + (rejected ? "rejected" : "accepted") + " [" + resultContent + "]");
    }
    
    protected void logAndPublishResult(final boolean rejected, final String resultContent) {
        logResult(rejected, resultContent);
        publishResult(rejected, resultContent);
    }

    protected ResponseEntity<Object> getResultAndHandleErrors(final EActionCommandType type,
                                                              final BlockingQueue<RequestResult> resultQueue) {

        try {
            result = resultQueue.poll(configurationManager.getActionResultTimeoutMillis(), MILLISECONDS);
        } catch (final InterruptedException e) {
            log.error("Interrupted while waiting for " + type.name() + " action result: " + ExceptionTools.getMessage(e), e);
        }

        if (result == null) {
            return null;
        } else if (result.isBadRequest()) {
            logAndPublishResult(true, result.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(responseFactory.createActionErrorText(requestId, result.getMessage()));
        } else if (result.hasInternalError()) {
            logAndPublishResult(true, result.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseFactory.createActionErrorText(requestId, result.getMessage()));
        } else {
            return null;
        }

    }

    protected ResponseEntity<Object> createAcceptedOrOkResponse(final EActionCommandType type) {

        if (getResult() == null) {
            final GenericActionResponse resp = responseFactory.createGenericActionResponse(getRequestId(), type.name()
                    + " action was accepted but timed out waiting for final result (action may complete later)");
            logAndPublishResult(false, resp.getMessage());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
        } else {
            final GenericActionResponse resp = responseFactory.createGenericActionResponse(getRequestId(),
                    getResult().getMessage());
            logAndPublishResult(false, resp.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(resp);
        }

    }

}
