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
package jpl.gds.cfdp.linksim.out;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jpl.gds.ccsds.api.cfdp.ICfdpPdu;
import jpl.gds.cfdp.linksim.datastructures.ReceivedPduContainer;
import jpl.gds.product.api.auto.AutoPduHolder;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * {@code CfdpLinkSimOutboundPduRestApiSink} sends PDUs via REST API.
 *
 */
@Service
public class CfdpLinkSimOutboundPduRestApiSink implements ICfdpLinkSimOutboundPduSink {

    @Autowired
    private ApplicationContext appContext;

    private Tracer log;

    private String url;

    public void setUrl(final String url) {
        this.url = url;
    }

    @PostConstruct
    private void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
    }

    @Override
    public void send(final ICfdpPdu outPdu, final ReceivedPduContainer originalPduContainer) {

        try {
            log.info("Calling REST endpoint with PDU: ", outPdu);
            final AutoPduHolder autoPduHolder = new AutoPduHolder(outPdu.getData(), outPdu.getHeader().getDestinationEntityId());
            final ResponseEntity<String> resp = new RestTemplate().postForEntity(url,
                    autoPduHolder, String.class);
            log.info("... ", resp.getStatusCode().name());
        } catch (final RestClientException rce) {
            log.error("Could not POST PDU to URL " + url + ": " + ExceptionTools.getMessage(rce));
            log.debug(rce);
        }

    }

}
