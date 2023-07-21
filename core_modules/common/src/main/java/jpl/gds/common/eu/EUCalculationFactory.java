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

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.eu.IAlgorithmicEUDefinition;
import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.dictionary.api.eu.IEUDefinition;
import jpl.gds.dictionary.api.eu.IParameterizedAlgorithmicEUDefinition;
import jpl.gds.dictionary.api.eu.IPolynomialEUDefinition;
import jpl.gds.dictionary.api.eu.ITableEUDefinition;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
/**
 * EuCalculationFactory is used to create DN to EU conversion objects
 * (IEUCalculations) for use in the channel and product processing.
 * There is an associated definition object from the dictionary that
 * must be supplied to the factory to create a new DN to EU instance.

 *
 * @see IEUCalculation
 * @see IEUDefinition
 */
public class EUCalculationFactory implements IEUCalculationFactory {
    
    private final ApplicationContext appContext;
    private final AmpcsUriPluginClassLoader secureLoader;

    /**
     * Constructor.
     * 
     * @param context the current application context
     */
    public EUCalculationFactory(final ApplicationContext context) {
        this.appContext = context;
        this.secureLoader = context.getBean(AmpcsUriPluginClassLoader.class); 
    }

	/**
     * {@inheritDoc}
     */
	@Override
    public IEUCalculation createEuCalculator(final IEUDefinition def) {
	    
	    switch (def.getEuType()) {
        case NONE:
            break;
        case PARAMETERIZED_ALGORITHM: 
            return new ParameterizedAlgorithmicDNtoEU(
                    (IParameterizedAlgorithmicEUDefinition) def,
                    secureLoader);
        case POLYNOMIAL:
            return new PolynomialDNtoEU((IPolynomialEUDefinition)def);
        case SIMPLE_ALGORITHM:
            return new AlgorithmicDNtoEU((IAlgorithmicEUDefinition) def,
                    secureLoader);
        case TABLE:
            return new TableDNtoEU((ITableEUDefinition)def);
        default:
            throw new IllegalArgumentException("Unknown EU type " + def.getEuType());
	    
	    }
	    return null;
	}

}
