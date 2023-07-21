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

package jpl.gds.cfdp.processor.out;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import cfdp.engine.CommLink;
import cfdp.engine.Data;
import cfdp.engine.ID;
import cfdp.engine.PDUType;
import cfdp.engine.TransID;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.gsfc.util.GsfcUtil;
import jpl.gds.cfdp.processor.stat.StatManager;
import jpl.gds.product.api.auto.AutoPduHolder;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * This class receives PDUs from the GSFC library and transmits them to a
 * configured URI via REST.
 *
 */
@Service
@DependsOn("configurationManager")
public class OutboundPduUriSink implements CommLink {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ConfigurationManager configurationManager;

    @Autowired
    private StatManager statManager;

    @Autowired
    private GsfcUtil gsfcUtil;

    @Autowired
    private OutboundPduInternalStatManager outboundPduInternalStatManager;

    private String uri;

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
        uri = configurationManager.getOutboundPduUri();
    }

    void setUri(final String newUri) {
        uri = newUri;
    }

    @Override
    public boolean open(final ID partnerId) {
        log.trace("open " + partnerId);
        return true;
    }

    @Override
    public boolean ready(final PDUType pduType, final TransID transID, final ID partnerID) {

        if (configurationManager.isOutboundPduEnabled() && outboundPduInternalStatManager.getPduSentTimestamp()
                + configurationManager.getOutboundPduUriMinimumSendIntervalMillis() <= System.currentTimeMillis()) {
            log.trace("ready true " + pduType + " " + gsfcUtil.convertEntityId(transID.getSource()) + ":"
                    + Long.toUnsignedString(transID.getNumber()) + " -> " + gsfcUtil.convertEntityId(partnerID));
            return true;
        } else {
            log.trace("ready false " + pduType + " " + gsfcUtil.convertEntityId(transID.getSource()) + ":"
                    + Long.toUnsignedString(transID.getNumber()) + " -> " + gsfcUtil.convertEntityId(partnerID));
            return false;
        }

    }

    @Override
    public void send(final TransID transID, final ID partnerID, final Data pdu) {
        log.trace("send " + gsfcUtil.convertEntityId(transID.getSource()) + ":"
                + Long.toUnsignedString(transID.getNumber()) + " -> " + gsfcUtil.convertEntityId(partnerID)
                + " PDU " + pdu.length + " bytes");

        try {
            final AutoPduHolder autoPduHolder = new AutoPduHolder(pdu.get(), gsfcUtil.convertEntityId(partnerID));
            log.debug("AutoPduHolder: " + autoPduHolder);
            final ResponseEntity<String> resp = new RestTemplate().postForEntity(uri,
                    autoPduHolder, String.class);
            log.trace("POST " + uri + " result: " + resp.getBody());
            outboundPduInternalStatManager.setPduSentThisCycle(true);
            outboundPduInternalStatManager.setPduSentTimestamp(System.currentTimeMillis());
            statManager.setPduOutOk(true);
        } catch (final RestClientException rce) {
            statManager.setPduOutOk(false);
            log.error("Could not POST PDU to URI " + uri + ": " + ExceptionTools.getMessage(rce));
            log.debug(rce);
        }

    }

}