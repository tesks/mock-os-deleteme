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
import jpl.gds.dictionary.api.eu.IAlgorithmicEUDefinition;
import jpl.gds.dictionary.api.eu.IEUCalculation;

/**
 * This class implements an the dictionary definition of an non-parameterized
 * algorithmic DN to EU conversion for channel values, in which the EU
 * computation is performed by a Java class that must implement IEUCalculation.
 * 
 *
 *
 * @see EUDefinitionFactory
 * @see IEUCalculation
 */
public class AlgorithmicEUDefinition implements IAlgorithmicEUDefinition {

	/**
	 * Constructor. 
	 */
	AlgorithmicEUDefinition() {
		super();
	}

	/**
	 * Constructor that supplies class name. Used only by subclasses.
	 * 
	 * @param className the full package name of the algorithm class
	 */
	AlgorithmicEUDefinition(final String className) {
		setClassName(className);
	}

	/**
	 * Java class name of the algorithm class to use for computation.
	 */
	private String className;

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IAlgorithmicEUDefinition#getClassName()
     */
	@Override
    public String getClassName() {
		return className;
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.eu.IAlgorithmicEUDefinition#setClassName(java.lang.String)
     */
	@Override
    public void setClassName(final String name) {
		className = name;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Algorithmic\n   Name = " + this.getClassName() + "\n";
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.eu.IEUDefinition#getEuType()
	 */
	@Override
	public EUType getEuType() {
		return EUType.SIMPLE_ALGORITHM;
	}
}
