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

import java.io.IOException;

import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.product.api.ProductException;

/**
 * IProductDefinitionDumper is an interface to be implemented by classes that can
 * dump a product or DPO definition from the product dictionary to the console
 * in some format.
 * 
 *
 */
public interface IProductDefinitionDumper {
	
    /** 
     * Dumps a product or DPO definition to the console.
     * 
     * @param apidDefs the APID Dictionary to find APID definitions in
     * @param productDict the Product Dictionary to find product definitions in
     * @param id the primary identifier for the product in the dictionary; generally this is APID or
     * DPO VID
     * @param subId the secondary identifier for the product in the dictionary, if used by the current mission
     * @param name the dictionary name of the product or DPO definition
     * @param version the version of the definition to dump; pass null if no preference
     * @throws ProductException if there is an error access the product dictionary or interpreting the
     * definition
     * @throws IOException if there is a file I/O error reading the definition file(s)
     */
    public void dumpDefinition(IApidDefinitionProvider apidDefs, IProductDictionary productDict, int id, String subId, String name, String version) 
     throws ProductException, IOException;
}
