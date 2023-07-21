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
package jpl.gds.automation.auto.cfdp.spring.controller;

import io.swagger.annotations.*;
import jpl.gds.automation.auto.cfdp.config.AutoProxyProperties;
import jpl.gds.automation.auto.cfdp.service.CfdpAdapterCache;
import jpl.gds.automation.auto.cfdp.service.IAutoCfdpService;
import jpl.gds.automation.spring.controller.AutoController;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.product.api.auto.AutoPduHolder;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.output.IRawOutputAdapter;
import jpl.gds.tc.api.output.IRawOutputAdapterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Rest controller for the AUTO CFDP proxy to send pdu's
 */
@Api(value = "send_pdu", tags = "pdu")
@RestController(value = "send_pdu")
public class CfdpPduController extends AutoController implements ICfdpController {


    @Autowired
    private IRawOutputAdapterFactory adapterFactory;
    @Autowired
    private AutoProxyProperties      config;
    @Autowired
    private CfdpAdapterCache    cfdpCache;
    @Autowired
    private ApplicationContext       appContext;
    @Autowired
    private IAutoCfdpService autoCfdpAggregator;

    /**
     * AUTO Proxy interface for transmitting an <IPduHolder> to the command sink
     * 
     * @param pdu
     *            The IPduHolder to send
     * 
     * @param vcid
     *            The vcid to use
     * @return ResponseEntity<Object> response status
     */
    @ApiOperation(value = "Radiate an AutoPduHolder to the command sink", notes = "Forwards an AutoPduHolder to uplink connection")
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 500, message = "Problem establishing uplink connection"),
            @ApiResponse(code = 200, message = "Successfully forwarded PDU") })
    @PostMapping(value = { "send_pdu/{vcid}", "send_pdu" })
    public ResponseEntity<Object> sendCfdpPdu(@RequestBody
                                                  @ApiParam(value = "AutoPduHolder to send", type = "object", required = true) final AutoPduHolder pdu,
                                              @PathVariable(value = "vcid", required = false) @ApiParam(value = "Destination VCID", required = false) final Integer vcid) {
        final Long entityId = pdu.getDestinationEntityId();
        final int vc = vcid == null ? config.getVcidForEntity(entityId.intValue()) : vcid ;

        if (pdu.getPduData() == null || pdu.getPduData().length > config.getMaxPayloadForEntity(entityId.intValue())) {
            return new ResponseEntity<>("Received PDU file " +
                                                (pdu.getPduData() == null ? "0" : pdu.getPduData().length )
                                                + " is greater than the maximum payload allowed " + config.getMaxPayloadForEntity(entityId.intValue()),
                                        HttpStatus.BAD_REQUEST);
        }
                
        return sendPduData(pdu.getPduData(), entityId.intValue(), vc);
    }
    
    /**
     * AUTO Proxy interface for transmitting PDU DATA to the command sink
     * 
     * @param pduData
     *            The PDU data
     * @param entityId
     *            The destination entity id
     * @param vcid
     *            The vcid to use
     * @return ResponseEntity<Object> response status
     */
    @ApiOperation(value = "Radiate PDU data to the command sink", notes = "Forwards PDU data to uplink connection")
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 500, message = "Problem establishing uplink connection"),
            @ApiResponse(code = 200, message = "Successfully forwarded PDU") })
    @PostMapping(value = { "send_pdu_data/{vcid}", "send_pdu_data" })
    @ResponseBody
    public ResponseEntity<Object> sendPduData(@RequestParam("pduData") @ApiParam(value = "PDU data to send", type = "array", required = true) final byte[] pduData,
                                              @RequestParam("destinationEntityId") @ApiParam(value = "Destination Entity ID", required = true) final Integer entityId,
                                              @PathVariable(value = "vcid", required = false) @ApiParam(value = "Destination VCID", required = false) final Integer vcid) {
        if (pduData == null || pduData.length > config.getMaxPayloadForEntity(entityId)) {
            return new ResponseEntity<>("Received PDU file " +
                                                (pduData == null ? "0" : pduData.length )
                                                + " is greater than the maximum payload allowed " + config.getMaxPayloadForEntity(entityId),
                                        HttpStatus.BAD_REQUEST);
        }

        final int vc = vcid == null ? config.getVcidForEntity(entityId) : vcid ;
        final int scid = config.getScidForEntity(entityId);
        final int apid = config.getApidForEntity(entityId);
        final int port = config.getUplinkPort();
        final String host = config.getUplinkHost();
        final UplinkConnectionType connectionType = config.getUplinkType();

        IRawOutputAdapter outputAdapter = cfdpCache.get(entityId);
        try {
            if (outputAdapter == null) {
                outputAdapter = adapterFactory.getUplinkOutput(connectionType);
                outputAdapter.init();

                log.info("Initialized output adapter ", outputAdapter.getClass().getName(), " for uplink via ",
                         connectionType);

                cfdpCache.put(outputAdapter, entityId);
            }
        }
        catch (final RawOutputException e) {
            log.error("Unable to establish output adapter: ", ExceptionTools.getMessage(e));
            return new ResponseEntity<>("Unable to establish output adapter: " + ExceptionTools.getMessage(e),
                                        HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.trace("PDU DATA\n", BinOctHexUtility.formatHexString(BinOctHexUtility.toHexFromBytes(pduData), 40));
        if (config.getAggregateStrategyForEntity(entityId)) {
            if (!autoCfdpAggregator.isRunning()) {
                final boolean started = autoCfdpAggregator.startService();
                if (!started) {
                    return new ResponseEntity<>("Aggregation is enabled for for " + entityId + " but the PDU "
                                                        + "Aggregator service was unable to start ",
                                                HttpStatus.INTERNAL_SERVER_ERROR );
                }
            }
            return autoCfdpAggregator.aggregatePdus(entityId, pduData, vc);
        }

        log.debug("Sending ", pduData.length, " bytes of pdu '",
                  " to destination entityId ", entityId, ". Routing to vcid ", vc, " on spacecraft ", scid,
                  " on host ", host, " using port ", port);

        try {
            outputAdapter.sendPdus(pduData, vc, scid, apid);
        }
        catch (final UplinkException e) {
            return new ResponseEntity<>(ExceptionTools.getMessage(e) + "\n"
                    + e.getUplinkResponse().getDiagnosticMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        catch (final IllegalArgumentException e) {
        	return new ResponseEntity<>(ExceptionTools.getMessage(e), HttpStatus.BAD_REQUEST);
        }

        log.debug("Forwarded pdu data to entityId ", entityId, ". Routed to vcid ", vc,
                 " on spacecraft ", scid, " via ", connectionType, " using ", outputAdapter.getClass().getName(),
                 " output adapter");


        return new ResponseEntity<>("Forwarded pdu data to entityId " + entityId,HttpStatus.OK);

    }

    
    /**
     * AUTO Proxy interface for transmitting PDU files to the command sink
     * 
     * @param file
     *            The PDU file
     * @param entityId
     *            The destination entity id
     * @param vcid
     *            The vcid to use
     * 
     * @return ResponseEntity<Object> containing request status
     */
    @ApiOperation(value = "Radiate PDU file to the command sink", notes = "Forwards PDU file to uplink connection")
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 500, message = "Problem establishing uplink connection"),
            @ApiResponse(code = 200, message = "Successfully forwarded PDU") })
    @PostMapping(value = { "send_pdu_file/{vcid}", "send_pdu_file" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Object> sendPduFile(@RequestParam("pduFile") @ApiParam(value = "PDU file to send", type = "file", required = true) final MultipartFile file,
                                              @RequestParam("destinationEntityId") @ApiParam(value = "Destination Entity ID", required = true) final Integer entityId,
                                              @PathVariable(value = "vcid", required = false) @ApiParam(value = "Destination VCID", required = false) final Integer vcid) {

        if (file.getSize() > config.getMaxPayloadForEntity(entityId)) {
            return new ResponseEntity<>("Received PDU file " + file.getSize()
                    + " is greater than the maximum payload allowed " + config.getMaxPayloadForEntity(entityId),
                                        HttpStatus.BAD_REQUEST);
        }
        try {
            file.getBytes();
            final int vc = vcid == null ? config.getVcidForEntity(entityId) : vcid;
            log.debug("Received pdu file ", file.getOriginalFilename(), ".. forwarding PDU data to entityId", entityId);
            return sendPduData(file.getBytes(), entityId, vc);
        }
        catch (final Exception e) {
            return new ResponseEntity<>(ExceptionTools.getMessage(e) + " Error loading bytes from PDU file "
                    + file.getOriginalFilename(), HttpStatus.BAD_REQUEST);
        }

    }

}
