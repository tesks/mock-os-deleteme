/*
 * Copyright 2006-2018. California Institute of Technology.
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

import io.swagger.annotations.*;
import jpl.gds.automation.auto.AutoManager;
import jpl.gds.automation.auto.AutoProxyException;
import jpl.gds.automation.spring.controller.AutoController;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.tc.api.icmd.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@RestController(value = "session")
@Api(value = "session", tags = "session")
public class AutoSessionController extends AutoController implements IAutoController {

    /** The AutoManager to use with requests */
    @Autowired
    protected AutoManager manager;

    /**
     * @param sessionConfigFilePath
     *            the session configuration file to load
     * @param sessionId
     *            the session id to use
     * @param sessionHost
     *            the session host to use
     * @return ResponseEntity
     * @throws AutoProxyException
     *             when unable to complete proxy operation
     */
    @ResponseBody
    @ApiOperation(value = "Initialize a SessionConfiguration")
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 500, message = "Session not initialized") })
    @PostMapping(value = "session", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity startSession(
                                                  @RequestParam(value = "sessionConfigFile", required = false) @ApiParam(value = "Session configuration file to load", required = false) final String sessionConfigFilePath,
                                                  @RequestParam(value = "sessionId", required = false, defaultValue = "0") @ApiParam(value = "Session ID to use", required = false) final Long sessionId,
                                                  @RequestParam(value = "sessionHost", required = false) @ApiParam(value = "Session host to use", required = false) final String sessionHost) {
        if (sessionConfigFilePath == null && (sessionId == null && sessionHost == null)) {
            return new ResponseEntity("Session parameters must be supplied ", HttpStatus.BAD_REQUEST);
        }

        IContextConfiguration sessionConfig = null;
        try {
            HttpStatus errorStatus = null;
            String errorMessage = "";
            if ((sessionConfigFilePath != null) && !sessionConfigFilePath.isEmpty()) {
                sessionConfig = manager.createNewSession(sessionConfigFilePath);

                // if there is an error, use this message
                errorMessage = "Unable to identify session from supplied parameters: sessionConfigFile="
                        + sessionConfigFilePath;
                errorStatus = HttpStatus.BAD_REQUEST;
            } else if ((sessionId != null) && !StringUtil.isNullOrEmpty(sessionHost)) {
                sessionConfig = manager.getUplinkSessionFromDatabase(sessionId, sessionHost);
                manager.log("Initializing session with id " + sessionId, TraceSeverity.DEBUG);
                errorMessage = "Unable to identify session from supplied parameters: sessionId=" + sessionId
                        + ", sessionHost=" + sessionHost;
                errorStatus = HttpStatus.BAD_REQUEST;
            } else {
                sessionConfig = manager.getLatestUplinkSession();

                errorMessage = "Unable to attach AUTO to a valid AMPCS session";
                errorStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }

            if (sessionConfig == null) {
                return new ResponseEntity(errorMessage, errorStatus);
            }
            manager.sessionInit(sessionConfig);
            manager.initAccessControl();
        } catch (final AutoProxyException e) {
            try {
                manager.log("Unable to initialize session: " + ExceptionTools.getMessage(e), TraceSeverity.ERROR);
            } catch (final AutoProxyException e1) {
            }
            return new ResponseEntity("Unable to initialize session: " + ExceptionTools.getMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (AuthenticationException e) {
            return new ResponseEntity("Unable to initialize session. Could not authenticate user: " + ExceptionTools.getMessage(e),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        final IContextKey idObj = sessionConfig.getContextId().getContextKey();
        manager.log("Successfully initialized the session configuration ", TraceSeverity.DEBUG);

        final String respString = String.format("venue=%s,key=%d,host=%s,scid=%d",
                                                sessionConfig.getVenueConfiguration().getVenueType().toString(), idObj.getNumber(), idObj.getHost(),
                                                sessionConfig.getContextId().getSpacecraftId());

        manager.log(respString, TraceSeverity.INFO);
        return ResponseEntity.status(HttpStatus.OK).body(respString);

    }

}
