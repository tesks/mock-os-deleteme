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
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

/**
 * Resource interface for querying, updating, creating, and deleting SLE
 * interface profiles. Supports GET, PUT, POST, and DELETE operations.
 * 
 */
public interface ISLEInterfaceProfilesResource {

	/**
	 * Return a JSON representation of the queried SLE interface profiles.
	 * 
	 * @return JSON representation of SLE interface profiles
	 */
	@Get
	Representation toJson();

	/**
	 * Create a new SLE interface profile.
	 * 
	 * @param rep
	 *            Representation of the new SLE interface profile to create
	 */
	@Put
	void store(Representation rep);

	/**
	 * Update an existing SLE interface profile.
	 * 
	 * @param rep
	 *            Representation of the updated values for an existing SLE
	 *            interface profile
	 */
	@Post
	void accept(Representation rep);

	/**
	 * Delete an existing SLE interface profile.
	 */
	@Delete
	void remove();

}