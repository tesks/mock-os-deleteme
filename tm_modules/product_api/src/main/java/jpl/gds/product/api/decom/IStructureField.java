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
package jpl.gds.product.api.decom;

/**
 * An interface to be implemented by decom structure fields.
 * 
 *
 */
public interface IStructureField extends IFieldContainer {

    /**
     * Retrieves the name of the structure data type.
     * 
     * @return the type name
     */
    public abstract String getTypeName();

    /**
     * Sets the name of the structure data type.
     * 
     * @param typeName the type name to set
     */
    public abstract void setTypeName(final String typeName);

}