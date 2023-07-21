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
package jpl.gds.context.api;

import jpl.gds.context.api.filtering.IFilterableDataItem;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.xml.stax.StaxSerializable;

/**
 * Top-level interface to be implemented by simple context configuration
 * classes.
 * 
 *
 * @since R8
 */
public interface ISimpleContextConfiguration extends Templatable, StaxSerializable {

	/**
	 * Retrieves the context identification object for this context configuration.
	 * 
	 * @return IContextIdentification object
	 */
	IContextIdentification getContextId();

	/**
	 * Gets the general context information object used by this context
	 * configuration to store general parameters.
	 * 
	 * @return IGeneralContextInformation object; never null
	 */
	IGeneralContextInformation getGeneralInfo();

	/**
	 * Gets the metadata for this object. Implementors must check for overridden metadata.
	 *
	 * @return the populated metadata
	 */
	MetadataMap getMetadata();

    /**
     * Sets the REST port into the current context configuration
     * 
     * @param restPort
     *            port being used
     */
    void setRestPort(UnsignedInteger restPort);

	/**
	 * Gets the REST port from the current context configuration
	 *
	 * @return rest port
	 */
	int getRestPort();

	/**
	 * Gets the context filter information.
	 *
	 * @return IContextFilterInformation object
	 */
	IContextFilterInformation getFilterInformation();

	/**
	 * Determines if the supplied data item should be accepted given the current
	 * configured context filters.
	 *
	 * @param toFilter the data item to check
	 * @return true if the given data item passes the filter, false if not
	 */
	boolean accept(IFilterableDataItem toFilter);

	/**
	 * Get list of Session IDs  that have been associated with this server
	 *
	 * @return session IDs as string
	 */
	String getSessionIdsAsString();

	/**
	 * Add a session ID to the list
	 *
	 * @param sessionId New session ID to add
	 */
	void addSessionId(Long sessionId);
}