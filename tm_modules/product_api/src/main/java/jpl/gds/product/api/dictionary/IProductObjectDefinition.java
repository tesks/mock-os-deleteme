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

import jpl.gds.dictionary.api.ICategorySupport;
import jpl.gds.dictionary.api.IDecomHandlerSupport;
import jpl.gds.product.api.decom.IChannelBlockSupport;
import jpl.gds.product.api.decom.IFieldContainer;

/**
 * The <code>IProductObjectDefinition</code> interface is to be implemented by all 
 * product dictionary adaptation classes that represent Product Object 
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
 * to their respective definitions.  A product dictionary essentially contains a list
 * of IProductDefinition objects. Those in turn contain IProductObjectDefinition 
 * objects that describe the data fields in the product file. Product Object 
 * Definitions should never be created directly, but rather only through the 
 * ProductObjectDefinitionFactory. The resulting objects should only be access 
 * through this interface. Direct interaction with objects that implement 
 * Product Object Definitions is a violation of multi-mission development standards.
 * <p>
 * In documentation, a product object is often referred to as a DPO.
 * 
 *
 * @see IProductDefinition
 */
public interface IProductObjectDefinition extends IFieldContainer, IChannelBlockSupport, IDecomHandlerSupport, ICategorySupport {

    /**
     * Writes the product definition as formatted text to the given output stream.
     * @param out the output stream to write to
     * @throws IOException if there is an error writing to the output stream
     */
     void print(final PrintStream out) throws IOException;
    
    /**
     * Retrieves the product object virtual identifier (VID) for this 
     * product object definition.
     * 
     * @return the product object id
     */
     int getDpoId();

    /**
     * Sets the product object virtual identifier (VID) for this 
     * product object definition.
     * 
     * @param dpoId the id value to set
     */
     void setDpoId(final int dpoId);

    /**
     * Retrieves the operational category associated with this product object definition.
     * This is a flight dictionary value.
     * 
     * @return the operational category name, or null if none set
     */
    @Deprecated /* Replaced with ICategorySupport. */
     String getOperationalCategory();

    /**
     * Sets the operational category associated with this product object definition.
     * This is a flight dictionary value.
     * 
     * @param operationalCategory the operationalCategory to set
     */
    @Deprecated /* 11/2/15 Replaced with ICategorySupport. */
     void setOperationalCategory(final String operationalCategory);

    /**
     * Retrieves the flight software module name associated with this product object
     * definition. This is a flight dictionary value.
     * 
     * @return the module
     */
    @Deprecated /* 11/2/15 Replaced with ICategorySupport. */
     String getModule();

    /**
     * Sets the flight software module name associated with this product object
     * definition. This is a flight dictionary value.
     * 
     * @param module the module to set
     */
    @Deprecated /* 11/2/15 Replaced with ICategorySupport. */
     void setModule(final String module);

    /**
     * Set the FSW version associated with this product object
     * definition.
     * 
     * @param fswVersionId flight software version
     */
    // TODO - Make this version a string. This numeric ID is too MSL specific
     void setFswVersionId(final long fswVersionId);

    /**
     * Return the FSW version associated with this product object
     * definition.
     * 
     * @return the flight software version
     */
    // TODO - Make this version a string. This numeric ID is too MSL specific
     long getFswVersionId();

}