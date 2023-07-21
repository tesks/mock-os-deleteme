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
 * The <code>IProductObjectDictionary</code> interface is to be implemented by all 
 * product dictionary adaptation classes that include support for data product
 * object definitions within data products. 
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * The product dictionary is used in downlink processing to map data products
 * to their respective definitions.  A product dictionary essentially contains a list
 * of IProductDefinition objects. Those in turn contain IProductObjectDefinition 
 * objects that describe the data fields in the product file. In the reference 
 * implementation, there is only one IProductObjectDefinition in the 
 * IProductDefinition container, and it represents the entire content of
 * the data product. 
 * <br>
 * In order to support definition of product objects in the product dictionary,
 * the mission's product dictionary adaptation must implement this interface,
 * as opposed to the basic IProductDictionary interface. Instances of 
 * IProductObjectDictionary should only be created via the ProductDictionaryFactory, 
 * The resulting objects should only be accessed through this interface. 
 * Direct interaction with objects that implement Product Object Dictionaries 
 * is a violation of multi-mission development standards.
 * <p>
 * In documentation, a product object is often referred to as a DPO.
 * 
 *
 * @see IProductObjectDefinition
 */
public interface IProductObjectDictionary extends IProductDictionary {
	
    /**
     * Gets the Product Object Definition with the given product object
     * virtual identifier (VID). If necessary, triggers the loading of the product object
     * definition file.
     *
     * @param dpoId the product object ID
     * @return the IProductObjectDefinition object with the given ID, or null if no match found
     */
    public IProductObjectDefinition getProductObjectDefinition(final int dpoId);
}
