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
package jpl.gds.sleproxy.common.resources;

import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

/**
 * Resource interface for chill interface configuration operations. Supports GET
 * and POST.
 * 
 */
public interface IChillInterfaceConfigResource {

	/**
	 * Return the JSON representation for GET.
	 * 
	 * @return JSON representation of chill interface configuration
	 */
	@Get
	Representation toJson();

	/**
	 * Handle a POST REST call up update the chill interface configuration.
	 * 
	 * @param rep
	 *            Representation to accept and apply to the chill interface
	 *            configuration
	 */
	@Post
	void accept(Representation rep);

}