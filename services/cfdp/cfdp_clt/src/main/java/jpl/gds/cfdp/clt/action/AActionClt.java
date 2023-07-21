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
package jpl.gds.cfdp.clt.action;

import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jpl.gds.cfdp.clt.ACfdpClt;
import jpl.gds.cfdp.common.GenericActionResponse;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.common.action.EActionCommandType;

/**
 * Class AActionClt
 */
public abstract class AActionClt extends ACfdpClt {
	
	protected final EActionCommandType actionType;
	
	public AActionClt(final EActionCommandType actionType) {
		this.actionType = actionType;
	}

    protected void responseSpecificPostAndPrint(final String absoluteUri, final GenericRequest req)
            throws HttpClientErrorException, RestClientException {
    	final RequestEntity<GenericRequest> requestEntity = new RequestEntity<>(req, headers, HttpMethod.POST, URI.create(absoluteUri));
        final ResponseEntity<GenericActionResponse> resp = new RestTemplate().exchange(requestEntity, GenericActionResponse.class);
        resp.getBody().printToSystemOut();
    }

    protected void postAndPrint(final String relativeUri, final GenericRequest req) {
        final String absoluteUri = getAbsoluteUri(relativeUri);

        try {
            responseSpecificPostAndPrint(absoluteUri, req);
        } catch (HttpClientErrorException | HttpServerErrorException hee) {
            System.err.println("HTTP Status Code " + hee.getStatusCode() + ": " + hee.getResponseBodyAsString()
                    + " [POST: " + absoluteUri + "]");
        } catch (final RestClientException rce) {
            System.err.println(rce);
            if (headers == null) {
				System.err.println("Server may require authentication. Please supply \""+this.accessOptions.LOGIN_METHOD_NON_GUI.getLongOrShort()+"\" option.");
			}
        }

    }
    
    
    /*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#showHelp()
	 */
	@Override
	public void showHelp() {
		showHelp(actionType.getCltCommandStr());
	}

}
