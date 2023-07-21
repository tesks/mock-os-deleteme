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

import jpl.gds.dictionary.api.ILookupSupport;
import jpl.gds.dictionary.api.eu.IEUSupport;
import jpl.gds.product.api.decom.formatter.IDecomOutputFormatter;

/**
 * An interface to be implemented by simple (primitive) decom field classes.
 * 
 *
 */
public interface IPrimitiveField extends IProductDecomField, IEUSupport, ILookupSupport {
    /**
     * Gets the data type of this primitive field.
     * 
     * @return decom data type
     */
    public DecomDataType getDataType();

    /**
     * Sets the units of the field value.
     * 
     * @param unit the unit designator (e.g., seconds, SCLK, etc)
     */
    public void setUnit(final String unit);

    /**
     * Gets the units of the field value.
     * 
     * @return the unit designator
     */
    public String getUnit();
    
    /**
     * Gets the resolved value of the field, which includes mapping of status
     * values and EU calculation.
     * 
     * @param value the incoming (unresolved) field value
     * @param out the IDecomOutputFormatter to use as context
     * @return resolved value
     */
    public Object getResolvedValue(final Object value, final IDecomOutputFormatter out);
}
