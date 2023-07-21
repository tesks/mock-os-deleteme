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
package jpl.gds.dictionary.impl.eu;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.dictionary.api.eu.EUDefinitionFactory;
import jpl.gds.dictionary.api.eu.EUType;
import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.dictionary.api.eu.ITableEUDefinition;

/**
 * This class represents the dictionary definition of a tabular DN to EU
 * conversion on channel values. A table interpolation has a series of DN/EU
 * pairs. Calculation of an EU from a DN means finding where the DN fits into
 * the table (between two elements, for instance) and then interpolates the EU
 * from the EU entries in the table at the same location(s).
 * 
 *
 * @see EUDefinitionFactory
 * @see IEUCalculation
 */
public class TableEUDefinition implements ITableEUDefinition {

    /**
     * Changed from maximum table size to an initial list size
     */
    private static final int INITIAL_TABLE_SIZE = 32;

    /**
     * Length of the interpolation table.
     */
    private int len;
    /**
     * DN values in the table.
     */
    private final List<Double> dnTable;
    /**
     * Corresponding EU values in the table.
     */
    private final List<Double> euTable;

    /**
     * Creates an instance of TableEUDefinition.
     */
    TableEUDefinition() {
        super();
        len = 0;
        dnTable = new ArrayList<>(INITIAL_TABLE_SIZE);
        euTable = new ArrayList<>(INITIAL_TABLE_SIZE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.ITableEUDefinition#getLength()
     */
    @Override
    public int getLength() {
        return len;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.ITableEUDefinition#setLength(int)
     */
    @Override
    public void setLength(final int len) {
        this.len = len;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.ITableEUDefinition#setDn(int,
     *      double)
     */
    @Override
    public void setDn(final int index, final double dn) {
        if (index >= dnTable.size()) {
        	int i = dnTable.size();
        	while (i <= index) {
        		dnTable.add(0.0);
        		++i;
        	}
        }
        this.dnTable.set(index, dn);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.ITableEUDefinition#setEu(int,
     *      double)
     */
    @Override
    public void setEu(final int index, final double eu) {
        if (index >= euTable.size()) {
        	int i = euTable.size();
        	while (i <= index) {
        		euTable.add(0.0);
        		++i;
        	}
        }
        this.euTable.set(index, eu);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.ITableEUDefinition#getDn(int)
     */
    @Override
    public double getDn(final int index) {
        return dnTable.get(index);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.ITableEUDefinition#getEu(int)
     */
    @Override
    public double getEu(final int index) {
        return euTable.get(index);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(
                "Table Interpolation\n       DN                EU\n");
        for (int i = 0; i < len; i++) {
            final String aLine = String.format("% 10f\t- % 10f", new Object[] {
                    dnTable.get(i), euTable.get(i) });
            result.append(aLine + "\n");
        }
        return result.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.ITableEUDefinition#getEuType()
     */
    @Override
    public EUType getEuType() {
        return EUType.TABLE;
    }
}
