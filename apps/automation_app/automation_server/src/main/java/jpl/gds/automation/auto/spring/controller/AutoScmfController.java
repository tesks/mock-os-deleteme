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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jpl.gds.automation.auto.AutoManager;
import jpl.gds.automation.auto.AutoProxyException;
import jpl.gds.automation.spring.controller.AutoController;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.UplinkParseException;
import jpl.gds.tc.api.icmd.exception.ICmdErrorManager;
@RestController(value = "send_scmf")
@Api(value = "send_scmf", tags = "scmf")
public class AutoScmfController extends AutoController implements IAutoController {
    @Autowired
    CommandProperties cmdProperties;

    /** The AutoManager to use with requests */
    @Autowired
    protected AutoManager manager;

    /**
     * 
     * @param scmfFilePath
     *            the scmf file path to use
     * @param validateScmf
     *            whether or not the scmf is validated
     * @param timeout
     *            for transmitting the scmf
     * @param uplinkRate
     *            the uplink bit rate to use for radiation
     * @return ResponseEntity
     */
    @ResponseBody
    @ApiOperation(value = "Sends a SCMF to the Uplink service", notes = "A session must be initialized")
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Bad request"),
            @ApiResponse(code = 200, message = "Successfully transmitted SCMF") })
    @PostMapping(value = "send_scmf")
    public ResponseEntity<Object> sendScmf(@RequestParam(value = "scmfFile") @ApiParam(value = "The SCMF file to send", required = true) final String scmfFilePath,
                                               @RequestParam(value = "validateScmf") @ApiParam(value = "If the SCMF should be validated", required = true) final boolean validateScmf,
                                               @RequestParam(value = "waitForRadiation", required = false, defaultValue = "0") @ApiParam(value = "If AMPCS should wait on the SCMF to radiate", required = false) final Integer timeout,
                                               @RequestParam(value = "uplinkRates", required = false) @ApiParam(value = "The uplink data rate", required = false) final String uplinkRate) {

        if (uplinkRate != null) {
            if (uplinkRate.equalsIgnoreCase("ANY")) {
                try {
                    cmdProperties.setUplinkRates(uplinkRate);
                } catch (final UplinkParseException e) {
                    return new ResponseEntity<>("Unable to parse uplink rates: " + ExceptionTools.getMessage(e),
                            HttpStatus.BAD_REQUEST);
                }
            } else {
                final String[] uplinkRates = uplinkRate.split(",");
                try {
                    cmdProperties.setUplinkRates(uplinkRates);
                } catch (final UplinkParseException e) {
                    return new ResponseEntity<>("Unable to parse uplink rates: " + ExceptionTools.getMessage(e),
                            HttpStatus.BAD_REQUEST);
                }
            }
            try {
                manager.log("Uplink rates set to: " + uplinkRate, TraceSeverity.INFO);
            } catch (final AutoProxyException e) {
                return new ResponseEntity<>("Error logging uplink rate '" + uplinkRate + "': " + ExceptionTools.getMessage(e),
                        HttpStatus.BAD_REQUEST);
            }
        }

        try {
            manager.log("SCMF file: " + scmfFilePath, TraceSeverity.DEBUG);
            manager.log("Validate SCMF: " + validateScmf, TraceSeverity.DEBUG);
            manager.log("Timeout: " + timeout.intValue(), TraceSeverity.DEBUG);
        } catch (final AutoProxyException e) {
            // don't return entity on log failure so we still try and send scmf
        }
        try {
            manager.sendScmf(scmfFilePath, !validateScmf, timeout.intValue());
        } catch (final Exception e) {
            final String err = ICmdErrorManager.getRestletStatus(e) + ": Encountered error while sending SCMF. "
                    + ExceptionTools.getMessage(e);
            try {
                manager.log(err, TraceSeverity.ERROR);
            } catch (final AutoProxyException e1) {
                // unable to send scmf or log the error
            }
            return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.OK.getReasonPhrase(), HttpStatus.OK);
    }

}
