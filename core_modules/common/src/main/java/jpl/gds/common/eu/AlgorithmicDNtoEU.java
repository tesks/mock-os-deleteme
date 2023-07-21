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
import java.util.HashMap;
import java.util.Map;

import jpl.gds.dictionary.api.eu.EUGenerationException;
import jpl.gds.dictionary.api.eu.IAlgorithmicEUDefinition;
import jpl.gds.dictionary.api.eu.IEUCalculation;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * This class implements an algorithmic DN to EU conversion for channel values,
 * in which the EU computation is performed by a Java class that must implement
 * IEUCalculation. If the class cannot be loaded or the EU method located the first 
 * time invoked, then the failed state is saved and no further attempts to perform
 * the EU calculation will be made.
 * 
 *
 * @see IEUCalculationFactory
 * @see IEUCalculation
 * 
 */
public class AlgorithmicDNtoEU implements IEUCalculation {
    
    /**
     * Map of loaded classes, per class loader.
     */
    protected static Map<ClassLoader, Map<String, Class<?>>> loadedClasses = new HashMap<ClassLoader, Map<String, Class<?>>>();
	
	/**
	 * The dictionary definition of the EU algorithm.
	 */
	protected IAlgorithmicEUDefinition definition;

	/**
	 * Class loader to use.
	 */
	protected ClassLoader loader;
	
	/**
	 * Algorithm class.
	 */
	protected Class<?> algorithmClass;
	
	/**
	 * Flag indicating class load has failed.
	 */
	protected boolean loadFailed = false;
	
	protected Object converter;
	
	/**
	 * Constructor.
	 * 
	 * @param def the dictionary definition object for this DN to EU conversion.
     * @param loader the class loader to use
	 */
	public AlgorithmicDNtoEU(final IAlgorithmicEUDefinition def, final ClassLoader loader)  {
		this.definition = def;
		this.loader = loader;
		synchronized(loadedClasses) {
		    loadedClasses.put(this.loader, new HashMap<String, Class<?>>());
		}
	}

	/**
	 * Gets the dictionary definition object for this DN to EU conversion.
	 * 
	 * @return IAlgorithmicEUDefinition
	 */
	public IAlgorithmicEUDefinition getDefinition() {
	    return this.definition;
	}
	
	/**
	 * Instantiates the user's EU algorithm instance. The first time this is called, 
	 * it creates the instance. Thereafter, it returns the same instance.
	 * 
	 * @return an instance of the Java algorithm class called for by the
	 *         the EU definition object
	 * @throws EUGenerationException if the instance could not be created
	 * 
	 */
	public Object getAlgorithmInstance() throws EUGenerationException {
	    
	    if (converter != null) {
	        return converter;
	    }
	    /* 
         * Moved code to load class and locate
         * EU method into separate methods so they can be accessed or
         * overridden by subclasses.
         */
        final Class<?> algorithmClass = loadClass(IEUCalculation.class);

        if (loadFailed) {
            return null;
        }
        
        IEUCalculation converter = null;
        final String className = this.definition.getClassName();

        try {
            converter = (IEUCalculation) ReflectionToolkit.createObject(algorithmClass);
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
	 * Loads the algorithm class and verifies the supplied interface class is
	 * assignable from it.
	 * 
	 * @param interfaceClass
	 *            the interface class we want the loaded class to implement
	 * @return the Class object for the loaded class, or null if the class has
	 *         previously failed loading;the first time, this method will throw
	 * @throws EUGenerationException
	 *             the first time it fails to load the class, or if the loaded
	 *             class does not implement the desired interface
	 */
	protected Class<?> loadClass(final Class<?> interfaceClass) throws EUGenerationException {
		Class<?> algorithmClass = null;
		
		final String className = this.definition.getClassName();

		if (loadFailed) {
			return null;
		}

		final Map<String, Class<?>> map = loadedClasses.get(loader);
		algorithmClass = map.get(className);
		
		if (algorithmClass == null) {
			try
			{
				algorithmClass = Class.forName(className, true, loader);
			}
			catch (final ClassNotFoundException | NoClassDefFoundError cnfe)
			{
				loadFailed = true;
				throw new EUGenerationException("Unable to load class '" +
						className + "': class not found");
			}

			if (! interfaceClass.isAssignableFrom(algorithmClass))
			{
				loadFailed = true;
				throw new EUGenerationException("Class '"                 +
						className            +
						"' is not a subclass of " +
						IEUCalculation.class.getName());
			}
		}

		return algorithmClass;
	}

	/**
	 * Locates and returns the eu Method object for the current algorithm class.
	 * 
	 * @param algorithmClass
	 *            the Class object for the current algorithm
	 * @return the Method object for eu(), or null if the eu() method was not
	 *         found in a previous try .
	 * 
	 * @throws EUGenerationException
	 *             the first time the desired eu() method is not found or in
	 *             inaccessible in the class
	 *                      
	 */
	protected Method getEuMethod(final Class<?> algorithmClass) throws EUGenerationException {
		Method euMethod = null;
		
		final String className = this.definition.getClassName();

		if (loadFailed) {
			return null;
		}

		try {
			euMethod = algorithmClass.getMethod("eu", double.class);
		} catch (final SecurityException e) {
			e.printStackTrace();
			loadFailed = true;
			throw new EUGenerationException("Class '" +
					className +
					"' does not allow access to the eu(Double) method");

		} catch (final NoSuchMethodException e) {
			e.printStackTrace();
			loadFailed = true;
			throw new EUGenerationException("Class '" +
					className +
					"' does not have an eu(Double) method");
		}
		return euMethod;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double eu(final double val) throws EUGenerationException {

		/* 
		 * Moved code to load class and locate
		 * EU method into separate methods so they can be accessed or
		 * overridden by subclasses.
		 */
		final Class<?> algorithmClass = loadClass(IEUCalculation.class);

		if (algorithmClass == null || loadFailed) {
			return 0.0;
		}

		final Method euMethod = getEuMethod(algorithmClass);

		final IEUCalculation converter = (IEUCalculation) getAlgorithmInstance();
		
		if (converter == null) {
		    return 0.0;
		}

		Double result = 0.0;
		try {
			result = (Double)euMethod.invoke(converter, val);
		} catch (final Exception e) {
			/* 
			 *  PMD warning suppressed here. If is very important
			 * that EU algorithms ONLY throw EUGenerationException.  We catch all violators
			 * here. But the algorithm may throw the right exception, so we re-throw it
			 * if so. The catch of Exception in general, the instanceOf check, and the 
			 * rethrow make PMD complain, but I feel the code is justified in this case.
			 */
			if (e instanceof EUGenerationException) {
				throw((EUGenerationException)e);
			}  else {
				throw new EUGenerationException("Error performing DN to EU algorithmic conversion", e);
			}
		}
		return result;
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
