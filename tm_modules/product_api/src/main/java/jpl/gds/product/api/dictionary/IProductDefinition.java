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
import java.io.PrintStream;
import java.util.List;

import jpl.gds.dictionary.api.IDecomHandlerSupport;

/**
 * The <code>IProductDefinition</code> interface is to be implemented by all 
 * product dictionary adaptation classes that represent top-level Product 
 * Definitions. 
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * The product dictionary is used in downlink processing to map data products
 * to their respective definitions.  A product dictionary contains a list
 * of IProductDefinition objects. Product Definitions may be mission-specific
 * but all must implement this interface.  Product Definitions should never
 * be created directly (only via an IProductDictionary object). The resulting
 * objects should only be accessed through this interface. Direct interaction with 
 * objects that implement Product Definitions is a violation of multi-mission 
 * development standards.
 * <p>
 *
 *
 * @see IProductDictionary
 */
public interface IProductDefinition extends IDecomHandlerSupport {

    /**
     * Sets the application process ID (APID) for the product represented by this
     * definition. Packets that have this APID will be assumed to belong to this product.
     * @param apid the application process ID
     */
    public abstract void setApid(final int apid);

    /**
     * Gets the application process ID (APID) for the product represented by this
     * definition. Packets that have this APID will be assumed to belong to this 
     * product.
     * @return the APID
     */
    public abstract int getApid();

    /**
     * Sets the product definition version.
     * @param version the version number
     */
    public abstract void setVersion(final String version);

    /**
     * Gets the product definition version.
     * @return the version number
     */
    public abstract String getVersion();

    /**
     * Sets the product type/APID name.
     * @param name the product type/APID name
     */
    public abstract void setName(final String name);

    /**
     * Gets the product type name/APID name.
     * @return the product type name, or "Unknown" if no product type defined
     */
    public abstract String getName();

    /**
     * Sets the product type description.
     * 
     * @param description the descriptive text
     */
    public abstract void setDescription(final String description);

    /**
     * Gets the product type description.
     * 
     * @return the descriptive text, or null if none defined
     */
    public abstract String getDescription();

    /**
     * Adds a product object definition to this definition. The product
     * object definition(s) in a product definition defines the actual 
     * fields in the data product in a manner that allows their decommutation. 
     * 
     * @param def the IProductObjectDefinition object to add
     */
    public abstract void addProductObject(IProductObjectDefinition def);
    
    /**
     * Gets the product object definitions that are part of this product definition. The product
     * object definition(s) in a product definition defines the actual fields in the data product
     * in a manner that allows their decommutation. 
     * 
     * @return List of IProductObjectDefinition; there should always be at least one
     */
    public abstract List<IProductObjectDefinition> getProductObjects();
    

    /**
     * Writes the product definition to the given output stream as formatted text.
     * @param out the output stream
     * @throws IOException if there is an error writing to the output stream
     */
    public abstract void print(final PrintStream out) throws IOException;

    /**
     * Retrieves the product definition key for this definition. The product key
     * must be unique within the entire product dictionary. The key itself
     * is project specific.
     * 
     * @return IProductDefinitionKey
     */
    public abstract IProductDefinitionKey getKey();

    /**
     * Gets the command stem for the flight command that builds this product.
     * @return the command stem, or null if none defined
     */
    public abstract String getCommand();

    /**
     * Sets the command stem for the flight command that builds this product.
     * @param stem the command stem; or null if none defined
     */
    public abstract void setCommand(final String stem);

}