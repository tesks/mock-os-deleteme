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

import jpl.gds.dictionary.api.eu.EUDefinitionFactory;
import jpl.gds.dictionary.api.eu.EUType;
import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.dictionary.api.eu.IPolynomialEUDefinition;

/**
 * This class represents the dictionary definition of a polynomial DN to EU
 * conversion on channel values. A polynomial EU is computed from the DN and a
 * series of coefficients: coeff0 * dn^0 + coeff1 * dn^1 + coeff2 * dn^2 etc.
 * 
 *
 *
 * @see EUDefinitionFactory
 * @see IEUCalculation
 */
public class PolynomialEUDefinition implements IPolynomialEUDefinition {

	/**
	 * Maximum number of polynomial coefficients in a polynomial EU calculation.
	 */
	private static final int MAX_POLY = 8; 

	/**
	 * Number of coefficients in the conversion.
	 */
	private int len;

	/**
	 * Actual polynomial coefficients for the conversion.
	 */
	private final double poly[];

	/**
	 * Creates an instance of PolynomialDNtoEUDefinition. 
	 */
	PolynomialEUDefinition() {
		super();
		poly = new double[MAX_POLY];
		len = 0;
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IPolynomialEUDefinition#getLength()
     */
	@Override
    public int getLength() {
		return len;
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IPolynomialEUDefinition#setLength(int)
     */
	@Override
    public void setLength(final int len) {
		this.len = len;
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IPolynomialEUDefinition#setCoefficient(int, double)
     */
	@Override
    public void setCoefficient(final int index, final double coeff) {
		if (index >= MAX_POLY) {
			throw new IllegalArgumentException("Polynomial index out of range");
		}
		poly[index] = coeff;
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IPolynomialEUDefinition#getCoefficient(int)
     */
	@Override
    public double getCoefficient(final int index) {
		return poly[index];
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder text = new StringBuilder("Polynomial\n");
		for (int i = 0; i < len; i++) {
			text.append("   coeff[" + i + "] = " + poly[i] + "\n");
		}
		return text.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.eu.IEUDefinition#getEuType()
	 */
	@Override
	public EUType getEuType() {
		return EUType.POLYNOMIAL;
	}
}
