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
package jpl.gds.product.impl.dictionary;

import java.io.File;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.product.api.decom.IProductDecomFieldFactory;
import jpl.gds.product.api.dictionary.IProductDefinition;
import jpl.gds.product.api.dictionary.IProductDefinitionBuilder;
import jpl.gds.product.api.dictionary.IProductDefinitionKey;
import jpl.gds.shared.log.TraceManager;



/**
 * ReferenceProductDictionary is the REFERENCE specific product dictionary class. It handles
 * the set of all product dictionary files.
 *
 *
 */
public class ReferenceProductDictionary extends AbstractProductDictionary {
 
    /**
     * Creates an instance of ReferenceProductDictionary.
     * @param fieldFactory the decom field factory to use
     */
    public ReferenceProductDictionary(final IProductDecomFieldFactory fieldFactory) { 
    	super(fieldFactory);
    }
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProductDefinitionBuilder createBuilder(final IProductDecomFieldFactory fieldFactory) {
        return new ReferenceProductDefinitionBuilder(fieldFactory);
    }
    
    /**
     * Gets a File object for the product definition file corresponding to
     * the given product definition key.
     * @param key the key for the desired product definition
     * @return the File object, or null if no definition file found
     */
	protected File getXmlFile(final IProductDefinitionKey key) {
        final ReferenceDpFileFilter filter = new ReferenceDpFileFilter((ReferenceProductDefinitionKey)key);
        getDefinitionDir().listFiles(filter);
        return filter.getLatestFile();
    }
    
    /**
     * Loads and returns the ProductDefinition corresponding to
     * the given product definition APID and Version
     * @param key the key for the desired product definition
     * @return the ProductDefinition, or null if no definition file is found
     * 
     * @throws DictionaryException if there is a problem loading or interpreting the definition file
     */
	protected IProductDefinition loadDefinition(final IProductDefinitionKey key) throws DictionaryException{

        final File file = getXmlFile(key);
        if (file == null) {
            return null;
        }
        return loadDefinition(file);
    }
    
    /**
     * Returns the ProductDefinition corresponding to the given product
     * definition key. This method is preferred over loadDefinition().
     * because it caches definitions that have already been loaded.
     * @param key the key for the desired product definition
     * @return the ProductDefinition, or null if no definition is found
     */
    @Override
	public IProductDefinition getDefinition(final IProductDefinitionKey key) {
        if (getDefinitionDir() == null) {
            return null;
        }

        // try to get definition from the cache
        IProductDefinition def = definitions.get(key);
        if (def != null) {
            return def;
        }
        // see if it's a known bad definition
        if (badDefinitions.contains(key)) {
            return null;
        }
        
        try {

            // try to load definition from the directory
            def = loadDefinition(key);

        } catch (final DictionaryException e) {

            // if we get here, we just found a new bad definition
            badDefinitions.add(key);
        }
        
        return def;
    }
    
    /**
     * Gets the Product Definition objects for the given product name.
     * @param apid the product type ID
     * @param version the product version
     * @return a ProductDefinition object, or null if none found or the definition could not be read
     */
	public IProductDefinition getDefinitionByApid(final int apid, final String version) {
    	if (getDefinitionDir() == null) {
    		return null;
    	}
    	
    	IProductDefinition def = null;

    	try {

    		final IProductDefinitionKey key = new ReferenceProductDefinitionKey(apid, Integer.valueOf(version));

    		// try to get definition from the cache
    		def = definitions.get(key);
    		if (def != null) {
    			return def;
    		}

    		// try to load definition from the directory
    		def = loadDefinition(key);

    	} catch (final DictionaryException e) {
    		 TraceManager.getDefaultTracer().error("Problem loading dp.xml file for DP with apid " + apid + ": " + e.toString(), e.getCause());

    	}
    	return def;
    }

}
