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

package jpl.gds.cfdp.clt.action.report;

import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jpl.gds.cfdp.clt.action.ATransactionIdentifyingClt;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.common.action.EActionCommandType;
import jpl.gds.cfdp.common.action.report.ReportActionResponse;

public class ReportClt extends ATransactionIdentifyingClt {

	public ReportClt() {
		super(EActionCommandType.REPORT);
	}

	@Override
	protected void responseSpecificPostAndPrint(final String absoluteUri, final GenericRequest req)
			throws HttpClientErrorException, RestClientException {
		final RequestEntity<GenericRequest> requestEntity = new RequestEntity<>(req, headers, HttpMethod.POST, URI.create(absoluteUri));
		final ResponseEntity<ReportActionResponse> resp = new RestTemplate().exchange(requestEntity, ReportActionResponse.class);
		resp.getBody().printToSystemOut();
	}

}