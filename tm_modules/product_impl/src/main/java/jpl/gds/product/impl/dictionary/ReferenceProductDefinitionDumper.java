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

import java.io.IOException;

import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.dictionary.IProductDefinition;
import jpl.gds.product.api.dictionary.IProductDefinitionDumper;
import jpl.gds.product.api.dictionary.IProductDictionary;

/**
 * ReferenceProductDefinitionDumper implements display of product definitions for the common 
 * implementation of product definition files.
 * 
 *
 */
public class ReferenceProductDefinitionDumper implements IProductDefinitionDumper {


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.dictionary.IProductDefinitionDumper#dumpDefinition(jpl.gds.dictionary.impl.impl.api.apid.IApidDictionary, jpl.gds.product.api.dictionary.IProductDictionary, int, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void dumpDefinition(final IApidDefinitionProvider apidDict, final IProductDictionary productDict, final int id, final String subId, final String name, final String version)
			throws ProductException, IOException {
		IProductDefinition def = null;
		if (name ==  null) {
			def = productDict.getDefinition(new ReferenceProductDefinitionKey(id, Integer.valueOf(version)));
			if (def == null) {
				throw new ProductException("Cannot load definition for data product with APID " + id + " and version " + version);
			}
		} else {
		    
			IApidDefinition apid = apidDict.getApidDefinition(name);
			if (apid == null) {
				throw new ProductException("Cannot load APID definition for data product with name '" + name + "' and version " + version);
			}
			def = ((ReferenceProductDictionary)productDict).getDefinitionByApid(apid.getNumber(), version);
			if (def == null) {
				throw new ProductException("Cannot load definition for data product with name '" + name + "' and version " + version);
			}
		}
		if (def != null) {
			def.print(System.out);
		}
	}
}
