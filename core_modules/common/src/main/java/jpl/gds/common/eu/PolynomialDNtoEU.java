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

import jpl.gds.dictionary.api.eu.EUGenerationException;
import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.dictionary.api.eu.IPolynomialEUDefinition;

/**
 * This class is for performing polynomial DN to EU conversions on channel values.
 * 
 *
 * @see EUCalculationFactory
 * @see IEUCalculation
 */
public class PolynomialDNtoEU implements IEUCalculation {
    
    private IPolynomialEUDefinition definition;

	/**
	 * Creates an instance of PolynomialDNtoEU. 
	 * 
	 * @param def the dictionary definition object for this DN to EU conversion.
	 * 
	 */
	public PolynomialDNtoEU(IPolynomialEUDefinition def) {
	    this.definition = def;
	}

	/**
	 * Gets the dictionary definition object for this DN to EU conversion.
	 * 
	 * @return IPolynomialEUDefinition
	 */
	public IPolynomialEUDefinition getDefinition() {
	    return this.definition;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.eu.IEUCalculation#eu(double)
	 */
	@Override
	public double eu(final double val) throws EUGenerationException {

		double euV = 0.0d;
		int len = definition.getLength();
		for (int i = len - 1; i >= 0; i--) {
			euV = (euV * val) + definition.getCoefficient(i);
		}
		return euV;
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
