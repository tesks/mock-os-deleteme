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

/**
 * Resource interface for querying the state of chill_sle_proxy. Supports only
 * the GET operation.
 * 
 */
public interface IStateResource {

	/**
	 * Return the JSON representation of chill_sle_proxy's state.
	 * 
	 * @return JSON representation of chill_sle_proxy's state
	 */
	@Get
	Representation toJson();

}