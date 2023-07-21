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
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.evr.IEvrArgumentDefinition;


/**
 * MultimissionEvrDefinition contains the definition of a single Evr instance (specific 
 * and methods for operating on it. It is used by multiple EVR dictionary adaptations.
 * This EVR definition class supports formally-defined EVR arguments.
 *
 */
public class MultimissionEvrDefinition extends AbstractEvrDefinition
{
    private int nargs;
    /*  Argument list should be empty rather than null if no arguments. */
    private List<IEvrArgumentDefinition> args = new ArrayList<IEvrArgumentDefinition>();
    private Categories categories = new Categories();

    /**
     * Basic constructor.
     * 
     */
    MultimissionEvrDefinition() {
        super();
        /* Evr definition objects need to be consistent.
         * The interface says number of arguments is 0 if none defined.
         */
        this.nargs = 0;
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrDefinition#getNargs()
     */
    @Override
    public int getNargs() {
        return this.nargs;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrDefinition#setNargs(int)
     */
    @Override
    public void setNargs(final int nargs) {
        this.nargs = nargs;
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrDefinition#getArgs()
     */
    @Override
    public List<IEvrArgumentDefinition> getArgs() {
        return this.args;
    }



    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrDefinition#setArgs(List)
     */
    @Override
    public void setArgs(List<IEvrArgumentDefinition> args) {
        /* Never set the list to null. Use an empty list */
        if (args == null) {
            this.args.clear();
        } else {
            this.args = args;
        }
    }




	/**
	 *  Key-value attribute map to hold the keyword name and value of any project-
	 *  specific information.
	 *  
	 */	
	public KeyValueAttributes keyValueAttr = new KeyValueAttributes();


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.evr.AbstractEvrDefinition#setKeyValueAttribute(java.lang.String, java.lang.String)
	 */
	@Override
	public void setKeyValueAttribute(String key, String value) {
		keyValueAttr.setKeyValue(key, value);
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.evr.AbstractEvrDefinition#getKeyValueAttribute(java.lang.String)
	 */
	@Override
	public String getKeyValueAttribute(String key) {
		return keyValueAttr.getValueForKey(key);
	}

	/**
	 *  {@inheritDoc}
	 *  @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getKeyValueAttributes()
	 * 
	 */
	@Override
	public KeyValueAttributes getKeyValueAttributes() {
		return keyValueAttr;
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.impl.evr.AbstractEvrDefinition#setKeyValueAttributes(jpl.gds.dictionary.impl.impl.api.KeyValueAttributes)
	 */
	@Override
	public void setKeyValueAttributes(KeyValueAttributes kvAttr) {
		keyValueAttr.copyFrom(kvAttr);
	}

	/** {@inheritDoc}
	 *
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#clearKeyValue()
	 */
	@Override
	public void clearKeyValue() {
		keyValueAttr.clearKeyValue();	
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
