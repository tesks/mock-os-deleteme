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
package jpl.gds.sleproxy.server.resources;

import org.restlet.resource.ServerResource;

import jpl.gds.sleproxy.common.resources.IRootResource;

/**
 * Restlet resource for the root API. Just returns application identification.
 * 
 */
public class RootServerResource extends ServerResource implements IRootResource {

	/* (non-Javadoc)
	 * @see jpl.gds.sle.proxy_common.resources.IRootResource#toTxt()
	 */
	@Override
	public final String toTxt() {
		return "This is the chill_sle_proxy server application version " + getContext().getParameters().getFirstValue("version");
	}

}