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

import java.util.List;


/**
 * An interface to be implemented by decom field classes that represent
 * fields that can contain other fields.
 * 
 *
 */
public interface IFieldContainer extends IProductDecomField, IChannelBlockSupport {

    /**
     * Adds a product decom field to the list of fields that
     * describe values in the container.
     * 
     * @param element the DefinitionElement to add
     */
    public abstract void addField(IProductDecomField element);

    /**
     * Returns a list of the top-level fields in the container.
     * 
     * @return a list of decom fields
     */
    public abstract List<IProductDecomField> getFields();

}