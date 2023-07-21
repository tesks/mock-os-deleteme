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
package jpl.gds.cfdp.clt.shutdown;

import jpl.gds.cfdp.clt.ACfdpClt;
import jpl.gds.cfdp.common.shutdown.ShutdownResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static jpl.gds.cfdp.clt.ENonActionCommandType.SHUTDOWN;
import static jpl.gds.security.cli.rest.AbstractRestfulClientCommandLineApp.UNAUTHENTICATED_RESPONSE_HEADER_KEYWORD;

/**
 * {@code ShutdownClt} is the {@code Runnable} class that runs as part of the CFDP Command-Line Tool (CLT) and calls
 * CFDP Processor's 'shutdown' REST API
 *
 * @since 8.1
 */
public class ShutdownClt extends ACfdpClt {

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        showHelp(SHUTDOWN.getCltCommandStr());
    }

    @Override
    public void run() {
        RestTemplate restTemplate = new RestTemplate();
        String absoluteUri = getAbsoluteUri(SHUTDOWN.getRelativeUri());

        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.add("Content-Type", "application/json");
        try {
            RequestEntity<String> requestEntity = new RequestEntity<>(headers, HttpMethod.POST,
                    URI.create(absoluteUri));
            ResponseEntity<ShutdownResponse> resp = restTemplate.exchange(requestEntity, ShutdownResponse.class);
            if (resp.getHeaders() != null && resp.getHeaders().toString().contains(UNAUTHENTICATED_RESPONSE_HEADER_KEYWORD) && headers == null) {
                System.err.println("Server requires authentication. Please supply \"" + this.accessOptions.LOGIN_METHOD_NON_GUI.getLongOrShort() + "\" option.");
                return;
            }
            System.out.println("Response from CFDP Processor: " + resp.getBody().getMessage());
        } catch (HttpClientErrorException hcee) {
            System.err.println("HTTP Status Code " + hcee.getStatusCode() + ": " + hcee.getResponseBodyAsString()
                    + " [GET: " + absoluteUri + "]");
        } catch (RestClientException rce) {
            System.err.println(rce);
        }

    }

}