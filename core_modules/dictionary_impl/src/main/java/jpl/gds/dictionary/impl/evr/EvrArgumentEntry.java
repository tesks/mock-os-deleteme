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

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.evr.EvrArgumentType;
import jpl.gds.dictionary.api.evr.IEvrArgumentDefinition;
import jpl.gds.shared.sys.SystemUtilities;

/**
 * This class represents an EVR argument for the EVR dictionaries that support
 * explicit argument definitions.
 * 
 *
 */
public class EvrArgumentEntry implements IEvrArgumentDefinition {


    /*
     *  Moved ArgumentType inner enum class to its own
     * class file and renamed it to EvrArgumentType.
     */

    private int num;
    private String name;
    private EvrArgumentType type;
    private int len;
    private String enumTableName;
    private EnumerationDefinition enumValues;


    /**
     * Basic Constructor. Creates an empty object.
     */
    EvrArgumentEntry() { 
        SystemUtilities.doNothing();
    }

    /**
     * Creates an instance of EvrArgumentEntry.
     * 
     * @param num
     *            number of the argument
     * @param name
     *            name of the argument
     * @param type
     *            data type of the argument (must match one of the enumeration
     *            names in ArgumentType)
     * @param len
     *            length of the argument in bytes
     * @param enumValues
     *            list of enumeration values for the argument; may be null
     * @param tableName enumeration table/typedef name, or null if not an ENUM
     *            argument
     * 
     * 
     * @deprecated use EvrArgumentFactory to create an instance
     */
    @Deprecated
    public EvrArgumentEntry(final int num, final String name, final EvrArgumentType type, final int len, final EnumerationDefinition enumValues, final String tableName) {
        this.num = num;
        this.name = name;
        this.type = type;
        this.enumValues = enumValues;
        this.enumTableName = tableName;

        if (this.type.equals(EvrArgumentType.ENUM) && (enumValues == null || tableName == null)) {
            throw new IllegalArgumentException("EVR argument uses undefined enum table");
        }

        this.len = len;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#getEnumTableName()
     */
    @Override
    public String getEnumTableName() {	
        return this.enumTableName;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#setEnumTableName(java.lang.String)
     */
    @Override
    public void setEnumTableName(final String enumTableName) {	
        this.enumTableName = enumTableName;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#getNumber()
     */
    @Override
    public int getNumber() {
        return this.num;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#setNumber(int)
     */
    @Override
    public void setNumber(final int num) {
        this.num = num;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#setName(java.lang.String)
     */
    @Override
    public void setName(final String name) {
        this.name = name;
    }

    
    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#getType()
     */
    public EvrArgumentType getType() {
        return this.type;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#setType(jpl.gds.dictionary.impl.impl.api.evr.EvrArgumentType)
     */
    public void setType(final EvrArgumentType type) throws IllegalArgumentException {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#getLength()
     */
    @Override
    public int getLength() {
        return this.len;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#setLength(int)
     */
    @Override
    public void setLength(final int len) {
        this.len = len;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#getEnumeration()
     */
    @Override
    public EnumerationDefinition getEnumeration() {
        return this.enumValues;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.evr.IEvrArgumentDefinition#setEnumeration(jpl.gds.dictionary.impl.impl.api.EnumerationDefinition)
     */
    @Override
    public void setEnumeration(final EnumerationDefinition v) {
        this.enumValues=v;
    }
}
