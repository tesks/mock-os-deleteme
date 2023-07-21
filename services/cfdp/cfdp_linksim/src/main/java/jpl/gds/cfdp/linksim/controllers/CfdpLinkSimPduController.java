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
package jpl.gds.cfdp.linksim.controllers;

import static jpl.gds.cfdp.linksim.CfdpLinkSimApp.INPUT_FROM_JMS_PROPERTY_KEY;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jpl.gds.cfdp.linksim.CfdpLinkSimPduQueueManager;
import jpl.gds.cfdp.linksim.datastructures.ReceivedPduContainer;
import jpl.gds.product.api.auto.AutoPduHolder;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * {@code CfdpLinkSimPduController} is the Spring controller class that handles inbound PDUs.
 *
 */
@RestController(value = "send_pdu")
@ConditionalOnProperty(name = "input.from.jms", havingValue = "false")
public class CfdpLinkSimPduController {

    private Tracer log;

    private boolean inputFromJms;

    @Autowired
    CfdpLinkSimPduQueueManager queueManager;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private Environment env;

    @PostConstruct
    private void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
        log.info("PDU input from REST API");
        inputFromJms = Boolean.parseBoolean(env.getProperty(INPUT_FROM_JMS_PROPERTY_KEY));
    }

    @PostMapping(value = {"send_pdu/{vcid}", "send_pdu"})
    public ResponseEntity<Object> sendCfdpPdu(@RequestBody final AutoPduHolder inPdu,
                                              @PathVariable(value = "vcid", required = false) final Integer vcid) {

        try {

            if (queueManager.getQueue().offer(new ReceivedPduContainer(inPdu.getPduData(), null), 5, TimeUnit.SECONDS)) {
                log.trace("New PDU queued successfully");
                return new ResponseEntity<>("Received OK", HttpStatus.OK);
            } else {
                log.error("PDU offer to queue failed");
                return new ResponseEntity<>("Could not queue", HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (final InterruptedException e) {
            log.error("PDU offer to queue threw exception", e);
            return new ResponseEntity<>("Could not queue", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}