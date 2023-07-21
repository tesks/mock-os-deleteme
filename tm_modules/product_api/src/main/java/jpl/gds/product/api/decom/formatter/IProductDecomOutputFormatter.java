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
package jpl.gds.product.api.decom.formatter;

import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.product.api.IProductMetadataProvider;

/**
 * The IProductDecomOutputFormatter interface is implemented by output formatters
 * that need to output text product decom information.
 * 
 *
 */
public interface IProductDecomOutputFormatter extends IDecomOutputFormatter {

	/**
	 * Sets the APID dictionary, for use in mapping product APIDs to types.
	 * 
	 * @param dict the IApidDictionary to set
	 */
	abstract public void setApidDictionary(IApidDefinitionProvider dict);
	
    /**
     * Writes the formatted product header.
     * 
     * @param metadata the project-specific product metadata
     */
    abstract public void headerStart(IProductMetadataProvider metadata);

    /**
     * Writes the formatted product trailer.
     */
    abstract public void headerEnd();
    
    /**
     * Prints the start of a DPO.
     * @param name the DPO name
     * @param dpoId the DPO virtual identifier (VID)
     */
    public abstract void dpoStart(String name, int dpoId);

    /**
     * Ends the printing of the DPO.
     */
    public abstract void dpoEnd();
    
    /**
     * Prints a blank line to the output stream.
     */
    public void println();

}