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

package jpl.gds.cfdp.stat;

import static jpl.gds.cfdp.clt.ENonActionCommandType.STAT;

import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jpl.gds.cfdp.clt.ACfdpClt;
import jpl.gds.cfdp.common.stat.StatResponse;

public class StatClt extends ACfdpClt {

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
	 */
	@Override
	public void showHelp() {
		showHelp(STAT.getCltCommandStr());
	}

	@Override
	public void run() {
		final RestTemplate restTemplate = new RestTemplate();
		final String absoluteUri = getAbsoluteUri(STAT.getRelativeUri());
		ResponseEntity<StatResponse> resp = null;
		try {
			final RequestEntity<StatResponse> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, URI.create(absoluteUri));
			resp = restTemplate.exchange(requestEntity, StatResponse.class);
			resp.getBody().printToSystemOut();
		} catch (final HttpClientErrorException hcee) {
			System.err.println("HTTP Status Code " + hcee.getStatusCode() + ": " + hcee.getResponseBodyAsString()
			+ " [GET: " + absoluteUri + "]");
		} catch (final RestClientException rce) {
			System.err.println(rce);
			if (headers == null) {
            	System.err.println("Server may require authentication. Please supply \""+this.accessOptions.LOGIN_METHOD_NON_GUI.getLongOrShort()+"\" option.");
			}
		}

	}

}