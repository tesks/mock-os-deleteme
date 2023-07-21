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

package jpl.gds.cfdp.processor.controller.action.log;

import io.swagger.annotations.*;
import jpl.gds.cfdp.processor.controller.AActionController;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.types.IDbLogProvider;
import jpl.gds.db.api.rest.IRestfulDbHelper;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.RestfulTelemetryException;
import jpl.gds.shared.string.StringResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.ws.rs.core.MediaType;
import java.util.List;

import static jpl.gds.context.cli.app.mc.IRestfulClientCommandLineApp.LOGS_CMD;
import static jpl.gds.context.cli.app.mc.IRestfulClientCommandLineApp.LOG_CMD;

/**
 * Rest controller for CFDP to insert and retrieve log messages
 *
 */
@RestController
// MPCS-11189  - 09/13/19 : chill_cfdp_processor should host the chill_cfdp Web GUI
@RequestMapping("/cfdp")
@EnableSwagger2
@Api(value = "log", tags = {"log"})
public class CfdpLogController extends AActionController {

    private static final String LOG_LIMIT_PARAM = "limit";
    private static final String PARAM_LOG_LIMIT = "log results limit, default is unlimited; positive integer. 0 or negative indicates unlimited.";

    @Autowired
    private IRestfulDbHelper logHelper;

    @Autowired
    private IContextConfiguration contextConfiguration;

    @ApiOperation(value = "Directs CFDP server to log a specified message",
            notes = "The message will be related to the server's database entry")
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid log message request"),
            @ApiResponse(code = 200, message = "Successfully logged message")})
    @PostMapping(value = LOG_CMD, produces = MediaType.APPLICATION_JSON)
    public StringResponse logMessage(@RequestParam(value = "level")
                                     @ApiParam(value = "The log message severity", required = true)
                                     final String level,
                                     @RequestParam(value = "message")
                                     @ApiParam(value = "The message to log", required = true)
                                     final String message) {
        try {
            return logHelper.logMessage(level, message, log);
        } catch(Exception e) {
            throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST, ExceptionTools.getMessage(e));
        }
    }

    @ApiOperation(value = "Query for log message entries in the database")
    @ApiResponses(value = {
            @ApiResponse(code = 412, message = "Invalid log request"),
            @ApiResponse(code = 200, message = "Successfully retrieved log messages")})
    @GetMapping(value = {LOGS_CMD } , produces = MediaType.APPLICATION_JSON)
    public List<IDbLogProvider> getServerLogs(@RequestParam(value = LOG_LIMIT_PARAM, required = false, defaultValue = "-1")
                                                  @ApiParam(value = PARAM_LOG_LIMIT)
                                                  final int limit) {
        try {
            return logHelper.getLogsFromContext(contextConfiguration.getContextId().getNumber(),
                                                contextConfiguration.getContextId().getHost(), limit);
        }  catch(Exception e) {
            throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST, ExceptionTools.getMessage(e));
        }
    }

}