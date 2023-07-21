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
package jpl.gds.cfdp.clt.root;

import jpl.gds.cfdp.clt.ACfdpClt;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static jpl.gds.cfdp.clt.ENonActionCommandType.ROOT;
import static jpl.gds.security.cli.rest.AbstractRestfulClientCommandLineApp.UNAUTHENTICATED_RESPONSE_HEADER_KEYWORD;

public class RootClt extends ACfdpClt {

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        showHelp(ROOT.getCltCommandStr());
    }

    @Override
    public void run() {
		final RestTemplate restTemplate = new RestTemplate();
		final String absoluteUri = getAbsoluteUri(ROOT.getRelativeUri());

        try {
			final RequestEntity<String> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, URI.create(absoluteUri));
			final ResponseEntity<String> resp = restTemplate.exchange(requestEntity, String.class);
            if (resp.getHeaders() != null && resp.getHeaders().toString().contains(UNAUTHENTICATED_RESPONSE_HEADER_KEYWORD) && headers == null) {
                System.err.println("Server requires authentication. Please supply \"" + this.accessOptions.LOGIN_METHOD_NON_GUI.getLongOrShort() + "\" option.");
                return;
            }
            System.out.println(resp.getBody());
		} catch (final HttpClientErrorException hcee) {
            System.err.println("HTTP Status Code " + hcee.getStatusCode() + ": " + hcee.getResponseBodyAsString()
                    + " [GET: " + absoluteUri + "]");
		} catch (final RestClientException rce) {
            System.err.println(rce);
        }

    }

}