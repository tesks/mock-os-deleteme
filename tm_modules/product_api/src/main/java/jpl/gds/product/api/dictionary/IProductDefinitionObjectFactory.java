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
 * A factory that creates instances of objects for the product builder.
 * This factory is for objects we create a lot of.  Objects we create a few of
 * are created from the application context.
 * 
 *
 * @since R8
 *
 */
public interface IProductDefinitionObjectFactory {

    /**
     * Creates a product definition key, used to lookup a product in the dictionary.
     * 
     * @param type numeric type identifier for the product (such as APID)
     * @param version product version; ignored if the dictionary does not support versions
     * @return new key instance
     */
    public IProductDefinitionKey createDefinitionKey(int type, int version);
    
    /**
     * Creates a product object definition.
     * 
     * @return new product object definition instance
     */
    public IProductObjectDefinition createProductObjectDefinition();

}