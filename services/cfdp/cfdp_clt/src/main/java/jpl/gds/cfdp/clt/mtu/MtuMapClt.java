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
package jpl.gds.cfdp.clt.mtu;

import static jpl.gds.cfdp.common.action.EActionCommandType.MTU_MAP;

import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jpl.gds.cfdp.clt.ACfdpClt;
import jpl.gds.cfdp.common.GenericPropertiesResponse;

/**
 * {@code MtuMapClt} is the subcommand application for 'mtumap'.
 *
 * @since 8.2
 */
public class MtuMapClt extends ACfdpClt {

    /*
     * (non-Javadoc)
     *
     * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
     */
    @Override
    public void showHelp() {
        showHelp(MTU_MAP.getCltCommandStr());
    }

    @Override
    public void run() {
        final RestTemplate restTemplate = new RestTemplate();
        final String absoluteUri = getAbsoluteUri(MTU_MAP.getRelativeUri());
        ResponseEntity<GenericPropertiesResponse> resp = null;
        try {
            final RequestEntity<GenericPropertiesResponse> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, URI.create(absoluteUri));
            resp = restTemplate.exchange(requestEntity, GenericPropertiesResponse.class);
            resp.getBody().printToSystemOut();
        } catch (final HttpClientErrorException hcee) {
            System.err.println("HTTP Status Code " + hcee.getStatusCode() + ": " + hcee.getResponseBodyAsString()
                    + " [GET: " + absoluteUri + "]");
        } catch (final RestClientException rce) {
            System.err.println(rce);
            if (headers == null) {
                System.err.println("Server may require authentication. Please supply \"" + accessOptions.LOGIN_METHOD_NON_GUI.getLongOrShort() + "\" option.");
            }
        }

    }

}