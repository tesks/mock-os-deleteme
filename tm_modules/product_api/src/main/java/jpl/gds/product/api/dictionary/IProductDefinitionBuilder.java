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

import jpl.gds.dictionary.api.DictionaryException;

/**
 * The <code>IProductDefinitionBuilder</code> interface is to be implemented by
 * all product definition and data product object definition file parsers.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * The product dictionary is used in downlink processing to map data products to
 * their respective definitions. Product Definition Builder classes are used to
 * parse the individual data product or data product object definition Product
 * Definition Builders may be mission-specific, but all must implement this
 * interface. For this reason, Product Definition Builders should never be
 * created directly (only via an IProductDictionary object). The resulting
 * objects should only be accessed through this interface. Direct interaction
 * with objects that implement Product Definition Builders is a violation of
 * multi-mission development standards.
 * <p>
 * 
 *
 * @see IProductDictionary
 */
public interface IProductDefinitionBuilder {

	/**
	 * Creates an IProductDefinition object by parsing the definition file with the
	 * given path.
	 * 
	 * @param path full path to the product definition file
	 * @return new IProductDefinition object for the current mission
	 * @throws DictionaryException if there is a problem parsing the product definition
	 */
    public IProductDefinition buildProductDefinition(String path) throws DictionaryException;
    
    /**
	 * Creates an IProductObjectDefinition object by parsing the definition file with the
	 * given path.
	 * 
	 * @param path full path to the product object definition file
	 * @return new IProductObjectDefinition object for the current mission
	 * @throws DictionaryException if there is a problem parsing the product object definition
	 */
    public IProductObjectDefinition buildProductObjectDefinition(String path) throws DictionaryException;
    
    /**
     * Resets the builder to its initial state.
     */
    public void reset();
}
