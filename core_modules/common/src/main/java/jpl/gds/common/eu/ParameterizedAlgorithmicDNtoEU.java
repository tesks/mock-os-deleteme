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

import java.lang.reflect.Method;
import java.util.Map;

import jpl.gds.dictionary.api.eu.EUGenerationException;
import jpl.gds.dictionary.api.eu.IParameterizedAlgorithmicEUDefinition;
import jpl.gds.dictionary.api.eu.IParameterizedEUCalculation;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * This class implements an algorithmic DN to EU conversion for channel values
 * that supports parameters from the dictionary. in which the EU computation is
 * performed by a Java class that must implement IParameterizedEUCalculation.
 * If the class cannot be loaded or the EU method located the first 
 * time invoked, then the failed state is saved and no further attempts to perform
 * the EU calculation will be made.
 * 
 *
 * @see EUCalculationFactory
 * @see IParameterizedEUCalculation
 */
public class ParameterizedAlgorithmicDNtoEU extends AlgorithmicDNtoEU {
   
    /**
     * Constructor.
     * 
     * @param def the dictionary definition object for this DN to EU 
     *        conversion
     * @param loader the class loader to use       
     */
    public ParameterizedAlgorithmicDNtoEU(final IParameterizedAlgorithmicEUDefinition def, final ClassLoader loader) {
        super(def, loader);
    }


    @Override
    public Object getAlgorithmInstance() throws EUGenerationException {
        
        if (converter != null) {
            return converter;
        }
        /* 
         * Moved code to load class and locate
         * EU method into separate methods so they can be accessed or
         * overridden by subclasses.
         */
        final Class<?> algorithmClass = loadClass(IParameterizedEUCalculation.class);

        if (loadFailed) {
            return null;
        }
        
        final String className = this.definition.getClassName();

        try {
            converter = ReflectionToolkit.createObject(algorithmClass);
        } catch (final ReflectionException e) {
            e.printStackTrace();
            loadFailed = true;
            throw new EUGenerationException("An instance of class '" +
                    className +
                    "' cannot be instantiated");
        }
        
        return converter;
    }
    
    
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.eu.AlgorithmicDNtoEU#getEuMethod(java.lang.Class)
	 */
	@Override
	protected Method getEuMethod(final Class<?> algorithmClass)
			throws EUGenerationException {
	    final String className = definition.getClassName();
	    
	    if (algorithmClass == null || loadFailed) {
	        return null;
	    }
	    
		Method euMethod = null;
		try {
			euMethod = algorithmClass.getMethod("eu", String.class, Map.class,
					double.class);
		} catch (final SecurityException e) {
			e.printStackTrace();
			loadFailed = true;
			throw new EUGenerationException(
					"Class '"
							+ className
							+ "' does not allow access to the eu(String, Map, Double) method");

		} catch (final NoSuchMethodException e) {
			e.printStackTrace();
			loadFailed = true;
			throw new EUGenerationException("Class '" + className
					+ "' does not have an eu(String, Map, Double) method");
		}
		return euMethod;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.eu.AlgorithmicDNtoEU#eu(double)
	 */
	@Override
	public double eu(final double val) throws EUGenerationException {

		final Class<?> algorithmClass = loadClass(IParameterizedEUCalculation.class);

		if (algorithmClass == null || loadFailed) {
			return 0.0;
		}

		final Method euMethod = getEuMethod(algorithmClass);

        final IParameterizedEUCalculation converter = (IParameterizedEUCalculation) getAlgorithmInstance();
        
        if (converter == null) {
            return 0.0;
        }
	
		Double result = 0.0;
		
		final IParameterizedAlgorithmicEUDefinition pdef = (IParameterizedAlgorithmicEUDefinition) getDefinition();
		
		try {
			result = (Double) euMethod.invoke(converter, pdef.getChannelId(), pdef.getParameters(),
					val);
		} catch (final Exception e) {
			/*
			 * PMD warning suppressed here. If is
			 * very important that EU algorithms ONLY throw
			 * EUGenerationException. We catch all violators here. But the
			 * algorithm may throw the right exception, so we re-throw it if so.
			 * The catch of Exception in general, the instanceOf check, and the
			 * rethrow make PMD complain, but I feel the code is justified in
			 * this case.
			 */
			if (e instanceof EUGenerationException) {
				throw ((EUGenerationException) e);
			} else {
				throw new EUGenerationException(
						"Error performing DN to EU algorithmic conversion", e);
			}
		}
		return result;
	}

}
