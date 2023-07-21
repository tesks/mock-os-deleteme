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

import java.util.List;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.ICacheableDictionary;
import jpl.gds.product.api.decom.IProductDecomFieldFactory;

/**
 * The <code>IProductDictionary</code> interface is to be implemented by all 
 * product dictionary adaptation classes. 
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
 * of IProductDefinition objects. Product Dictionaries may be mission-specific
 * but all must implement this interface.  Product Dictionaries should never
 * be created directly, but rather only through the ProductDictionaryFactory). The resulting
 * objects should only be access through this interface. Direct interaction with 
 * objects that implement product dictionaries is a violation of multi-mission 
 * development standards.
 * <p>
 *
 *
 * @see IProductDefinition
 */
public interface IProductDictionary extends ICacheableDictionary {

    /**
     * Sets the root directory path to the XML product definitions.
     * 
     * @param directoryPath the root path to the product dictionary files
     * @throws DictionaryException if the directory does not exist or is not a
     *             directory
     */
    public void setDirectory(String directoryPath) throws DictionaryException;

    /**
     * Loads all the product definitions in the dictionary directory.
     * 
     * @throws DictionaryException if the product definitions cannot be loaded
     */
    public void loadAll() throws DictionaryException;
    
    /**
     * Loads and/or gets the product definition with the given unique key.
     * Definition will be loaded from the filesystem if not already cached
     * in the product dictionary.
     * 
     * @param key mission-specific IProductDefinitionKey object identifying
     * the product definition to be loaded.
     * 
     * @return new IProductDefinition object, or null if no such product key found
     */
    public abstract IProductDefinition getDefinition(IProductDefinitionKey key);
    
    /**
     * Returns a list of all currently loaded IProductDefinition objects.
     * @return list of IProductDefinition; may be empty
     */
    public List<IProductDefinition> getAllDefinitions();
    
    /**
     * Disposes of all in-memory product definitions. The next time a product definition
     * is requested, it will be re-loaded by the product dictionary.
     */
    public void clear();
    
    /**
     * Gets a mission-specific IProductDefinitionBuilder, which is capable of parsing a
     * product definition or data product object definition file.
     * @param fieldFactory the product decom field factory to use in the returned builder
     * 
     * @return IProductDefinitionBuilder object
     */
    public IProductDefinitionBuilder createBuilder(IProductDecomFieldFactory fieldFactory);
    
}
