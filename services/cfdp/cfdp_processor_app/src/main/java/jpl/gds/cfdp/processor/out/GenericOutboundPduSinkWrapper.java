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
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import cfdp.engine.CommLink;
import cfdp.engine.Data;
import cfdp.engine.ID;
import cfdp.engine.PDUType;
import cfdp.engine.TransID;
import jpl.gds.cfdp.processor.config.ConfigurationManager;

@Service
@DependsOn("configurationManager")
public class GenericOutboundPduSinkWrapper implements CommLink {

	@Autowired
	private ConfigurationManager configurationManager;

	@Autowired
	private OutboundPduFilesystemSink outboundPduFilesystemSink;

	@Autowired
	private OutboundPduUriSink outboundPduUriSink;

	private CommLink currentOutboundSink;

	@Autowired
	private OutboundPduInternalStatManager outboundPduInternalStatManager;
	
	@PostConstruct
	public void init() {

		/*
		 * This is just to initialize. Once the application is started, the engine
		 * cycler will set the proper outbound sink.
		 */
		if (configurationManager.getOutboundPduSinkType() == EOutboundPduSinkType.FILESYSTEM) {
			currentOutboundSink = outboundPduFilesystemSink;
		} else {
			currentOutboundSink = outboundPduUriSink;
		}

	}

	/**
	 * Change the outbound PDU sink type.
	 * 
	 * @param newSinkType
	 *            the new sink type value
	 */
	public void setSinkType(final EOutboundPduSinkType newSinkType) {

		if (newSinkType == EOutboundPduSinkType.FILESYSTEM) {
			currentOutboundSink = outboundPduFilesystemSink;
		} else {
			currentOutboundSink = outboundPduUriSink;
		}

	}

	/**
	 * Change the URI of the URI-based outbound PDU sink.
	 * 
	 * @param newUri
	 *            the new URI value
	 */
	public void setUri(final String newUri) {
		// TODO
	}

	@Override
	public boolean open(final ID partnerId) {
		return currentOutboundSink.open(partnerId);
	}

	@Override
	public boolean ready(final PDUType pduType, final TransID transID, final ID partnerID) {
		return currentOutboundSink.ready(pduType, transID, partnerID);
	}

	@Override
	public void send(final TransID transID, final ID partnerID, final Data pdu) {
		currentOutboundSink.send(transID, partnerID, pdu);
	}

	/**
	 * @return the pduSentThisCycle
	 */
	public boolean hasPduBeenSentThisCycle() {
		return outboundPduInternalStatManager.isPduSentThisCycle()
;
	}

	/**
	 * @param pduSentThisCycle
	 *            the pduSentThisCycle to set
	 */
	public void setPduSentThisCycle(final boolean pduSentThisCycle) {
		outboundPduInternalStatManager.setPduSentThisCycle(pduSentThisCycle);
	}

}