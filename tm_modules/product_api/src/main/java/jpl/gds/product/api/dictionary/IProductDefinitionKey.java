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
package jpl.gds.product.api.dictionary;

/**
 * The <code>IProductDefinitionKey</code> interface is to be implemented by the
 * product key class in all product dictionary adaptations
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * The product dictionary is used in downlink processing to map data products to
 * their respective definitions. Product Definition Keys provide a unique method
 * for identifying a product definition within the set of all product
 * definitions in the dictionary. Product Definition Keys may be
 * mission-specific, but all must implement this interface. For this reason,
 * Product Definition Keys should never be created directly (only fetched from
 * an IProductDefinition object). The resulting objects should only be accessed
 * through this interface. Direct interaction with objects that implement
 * Product Definition Keys is a violation of multi-mission development
 * standards.
 * <p>
 * 
 *
 * @see IProductDefinition
 */
public interface IProductDefinitionKey {
	
	/**
	 * Creates a formatted output string of the key suitable for output
	 * to descriptive text files.
	 * 
	 * @return output text
	 */
	public String toOutputString();
}
