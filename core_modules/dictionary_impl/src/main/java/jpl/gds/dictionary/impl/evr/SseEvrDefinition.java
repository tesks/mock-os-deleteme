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
package jpl.gds.dictionary.impl.evr;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.evr.EvrDefinitionType;
import jpl.gds.dictionary.api.evr.IEvrArgumentDefinition;

/**
 * SseEvrDefinition contains the definition of a single Evr instance for use by 
 * the SseEvrDictionary and methods for operating on it. The primary difference
 * between JPL SSE Evr definitions and the general multimission definition is that the
 * SSE EVRs do not support explicit definition of EVR arguments.
 *
 *
 */
public class SseEvrDefinition extends AbstractEvrDefinition {

    private Categories categories = new Categories();


    /**
     * Basic constructor.
     * 
     */
    SseEvrDefinition() {
        super();
        /*  Default the definition type to SSE */
        setDefinitionType(EvrDefinitionType.SSE);
    }



    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrDefinition#getNargs()
     */
    @Override
    public int getNargs() {
        // This EVR class does not support arguments
        return 0;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrDefinition#setNargs(int)
     */
    @Override
    public void setNargs(int nargs) {
        // This EVR class does not support arguments
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrDefinition#getArgs()
     */
    @Override
    public List<IEvrArgumentDefinition> getArgs() {
        // This EVR class does not support arguments
        return new ArrayList<IEvrArgumentDefinition>(0);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrDefinition#setArgs(List)
     */
    @Override
    public void setArgs(List<IEvrArgumentDefinition> args) {
        // This EVR class does not support arguments
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#setCategories(jpl.gds.dictionary.impl.impl.api.Categories)
     */
    @Override
    public void setCategories(Categories map) {
        categories.copyFrom(map);
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#getCategories()
     */
    @Override
    public Categories getCategories() {
        return categories;
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#setCategory(java.lang.String, java.lang.String)
     */
    @Override
    public void setCategory(String catName, String catValue) {
        categories.setCategory(catName, catValue);        
    }

    /** {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.ICategorySupport#getCategory(java.lang.String)
     */
    @Override
    public String getCategory(String name) {
        return categories.getCategory(name);
    }

}
