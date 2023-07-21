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
package jpl.gds.common.eu;

import jpl.gds.dictionary.api.eu.EUDefinitionFactory;
import jpl.gds.dictionary.api.eu.EUGenerationException;
import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.dictionary.api.eu.ITableEUDefinition;

/**
 * This class is for performing table interpolation DN to EU conversion on channels.
 * 
 *
 * @see EUDefinitionFactory
 * @see IEUCalculation
 */
public class TableDNtoEU implements IEUCalculation {
    
    private ITableEUDefinition definition;

    /**
     * Creates an instance of TableDNtoEU. 
     * 
     * @param def the dictionary definition object for this DN to EU conversion. .
     */
    public TableDNtoEU(ITableEUDefinition def) {
        this.definition = def;
    }

    /**
     * Gets the dictionary definition object for this DN to EU conversion.
     * 
     * @return ITableEUDefinition
     */
    public ITableEUDefinition getDefinition() {
        return this.definition;
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.eu.IEUCalculation#eu(double)
	 */
	@Override
	public double eu(final double val) throws EUGenerationException {
		int i = findLowerBound(val);
		int len = definition.getLength();

		if (i >= len - 1)  {
			// DN is greater than max value in table
			i = len - 2;
		} else if (i < 0) {
			++i;
		}
		
		if (definition.getDn(i) == val) {
			return definition.getEu(i);
		}

		final double low_co = definition.getEu(i);
		final double hi_co = definition.getEu(i + 1);
		final double low_dn = definition.getDn(i);
		final double hi_dn = definition.getDn(i + 1);
		double euv;
		euv = low_co
				+ (((hi_co - low_co) / (hi_dn - low_dn)) * (val - low_dn));
		return euv;

	}

	private int findLowerBound(double dn) {
		// Modified binary search. At the end, the return index is decreased
		// by one if the DN at the current index is greater than the DN that is passed in.
		int lowerBoundIndex = 0;
		int upperBoundIndex = definition.getLength() - 1;
		int i = 0;
		while (lowerBoundIndex <= upperBoundIndex) {
			i = lowerBoundIndex + (upperBoundIndex - lowerBoundIndex) / 2;
			double currentDn = definition.getDn(i);
			if (dn == currentDn) {
				break;
			} else if (dn < currentDn) {
				upperBoundIndex = i - 1;
			} else {
				lowerBoundIndex = i + 1;
			}
		}
		if (definition.getDn(i) > dn) {
			--i;
		}
		return i;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
	    return this.definition.toString();
	}
	
	
}
