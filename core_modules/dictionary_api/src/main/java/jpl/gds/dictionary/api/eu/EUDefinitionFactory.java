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
package jpl.gds.dictionary.api.eu;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * EUDefinitionFactory is used to create DN (raw) to EU (engineering unit)
 * conversion definitions (IEUDefinitions) for use in the channel and product
 * dictionaries.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * DN to EU definitions are used to convert a raw spacecraft telemetry value,
 * traditionally called a Data Number (DN), to a more ground-useful value,
 * traditionally called the Engineering Unit (EU) value. The factory creates all
 * the types of IEUDefinition objects that are currently support by the ground
 * system. Once created by this factory, these objects should be access only via
 * the IEUDefinition interface. Direct creation or access of the underlying
 * objects is a violation of multi-mission development standards.
 * <p>
 *
 *  use only reflection for object creation.
 *
 *
 * @see IEUDefinition
 */
public class EUDefinitionFactory {
    
    /*  Added constants for cached reflection objects. */
    private static final Constructor<?> POLY_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> TABLE_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> ALGO_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> PARAM_ALGO_MAP_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> PARAM_ALGO_LIST_DEFINITION_CONSTRUCTOR;

    /*  Added initialization of cached reflection objects. */
    static {
        try {
            Class<?> c = Class.forName(DictionaryProperties.PACKAGE + "eu.PolynomialEUDefinition");
            POLY_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] {} );
            c = Class.forName(DictionaryProperties.PACKAGE + "eu.TableEUDefinition");
            TABLE_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] {} );
            c = Class.forName(DictionaryProperties.PACKAGE + "eu.AlgorithmicEUDefinition");
            ALGO_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] {} );
            c = Class.forName(DictionaryProperties.PACKAGE + "eu.ParameterizedAlgorithmicEUDefinition");
            PARAM_ALGO_MAP_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    {String.class, String.class, Map.class });
            c = Class.forName(DictionaryProperties.PACKAGE + "eu.ParameterizedAlgorithmicEUDefinition");
            PARAM_ALGO_LIST_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    {String.class, String.class, List.class });
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate EU definition class or its constructor", e);
        } 
    }

	/**
	 * Creates a polynomial-type DN to EU conversion definition.
	 * 
	 * @param coeffTable
	 *            the list of polynomial coefficients, which may not be null;
	 *            the value at index 0 is assumed to correspond to the first
	 *            coefficient
	 * @return Polynomial IEUDefinition object
	 * 
	 * throws IllegalArgumentException
	 *          rather than NPE if a coefficient in the list is null.
	 */
	public static IPolynomialEUDefinition createPolynomialEU (
			final List<Double> coeffTable) {
		if (coeffTable == null) {
			throw new IllegalArgumentException("coeffTable cannot be null");
		}
		IPolynomialEUDefinition result;
		try {
		    result = (IPolynomialEUDefinition) ReflectionToolkit.createObject(
		            POLY_DEFINITION_CONSTRUCTOR, new Object[] {});
		} catch (ReflectionException e) {
		    e.printStackTrace();
		    return null;
		}
		int i = 0;
		for (Double d : coeffTable) {
			if (d == null) {
				throw new IllegalArgumentException(
						"Polynomial Coefficient at index " + i + " is null");
			}
			result.setCoefficient(i++, d);
		}
		result.setLength(coeffTable.size());
		return result;
	}

	/**
	 * Creates a polynomial-type DN to EU conversion definition.
	 * 
	 * @param coeffTable
	 *            the list of polynomial coefficients, which may not be null;
	 *            the value at index 0 is assumed to correspond to the first
	 *            coefficient
	 * @return Polynomial IEUDefinition object
	 */
	public static IPolynomialEUDefinition createPolynomialEU(final double[] coeffTable) {
		if (coeffTable == null) {
			throw new IllegalArgumentException("coeffTable cannot be null");
		}
		IPolynomialEUDefinition result;
		try {
		    result = (IPolynomialEUDefinition) ReflectionToolkit.createObject(
		            POLY_DEFINITION_CONSTRUCTOR, new Object[] {});
		} catch (ReflectionException e) {
		    e.printStackTrace();
		    return null;
		}
		int i = 0;
		for (double d : coeffTable) {
			result.setCoefficient(i++, d);
		}
		result.setLength(coeffTable.length);
		return result;
	}

	/**
	 * Creates a table interpolation-type DN to EU conversion definition.
	 * 
	 * @param dnTable
	 *            the list of data number values in the interpolation table; may
	 *            not be null; must contain at least 2 points
	 * @param euTable
	 *            the list of engineering unit values in the interpolation
	 *            table; may not be null; must contain the same number of
	 *            elements as dnTable
	 * 
	 * @return Table Interpolation IEUDefinition object
	 */
	public static ITableEUDefinition createTableEU(final List<Double> dnTable,
			final List<Double> euTable) {
		if (dnTable == null || euTable == null) {
			throw new IllegalArgumentException(
					"dnTable and euTable must both be non-null");
		}
		if (dnTable.size() != euTable.size()) {
			throw new IllegalArgumentException(
					"dnTable and euTable must have the same lengths");
		}
		/** Make sure enough points exist for a line (2) */
		if (dnTable.size() < 2) {
			throw new IllegalArgumentException(
					"There must be at least (dn, eu) points for a table converison to be valid");
		}
		ITableEUDefinition result;
		try {
		    result = (ITableEUDefinition) ReflectionToolkit.createObject(
		            TABLE_DEFINITION_CONSTRUCTOR, new Object[] {});
		} catch (ReflectionException e) {
		    e.printStackTrace();
		    return null;
		}
		for (int i = 0; i < dnTable.size(); i++) {
			result.setDn(i, dnTable.get(i));
			result.setEu(i, euTable.get(i));
		}
		result.setLength(dnTable.size());
		return result;
	}

	/**
	 * Creates a table interpolation-type DN to EU conversion definition.
	 * 
	 * @param dnTable
	 *            the list of data number values in the interpolation table; may
	 *            not be null
	 * @param euTable
	 *            the list of engineering unit values in the interpolation
	 *            table; may not be null; must contain the same number of
	 *            elements as dnTable
	 * 
	 * @return Table Interpolation IEUDefinition object
	 */
	public static ITableEUDefinition createTableEU(final double[] dnTable,
			final double[] euTable) {
		if (dnTable == null || euTable == null) {
			throw new IllegalArgumentException(
					"dnTable and euTable must both be non-null");
		}
		if (dnTable.length != euTable.length) {
			throw new IllegalArgumentException(
					"dnTable and euTable must have the same lengths");
		}
		ITableEUDefinition result;
		try {
		    result = (ITableEUDefinition) ReflectionToolkit.createObject(
		            TABLE_DEFINITION_CONSTRUCTOR, new Object[] {});
		} catch (ReflectionException e) {
		    e.printStackTrace();
		    return null;
		}
		for (int i = 0; i < dnTable.length; i++) {
			result.setDn(i, dnTable[i]);
			result.setEu(i, euTable[i]);
		}
		result.setLength(dnTable.length);
		return result;
	}

	/**
	 * Creates a simple algorithmic-type DN to EU conversion definition (no
	 * parameters to algorithm).
	 * 
	 * @param className
	 *            the full java class name of the EU algorithm to invoke
	 * @return Algorithmic IEUDefinition object
	 */
	public static IAlgorithmicEUDefinition createAlgorithmicEU(final String className) {
		IAlgorithmicEUDefinition result;
		try {
		    result = (IAlgorithmicEUDefinition) ReflectionToolkit.createObject(
		            ALGO_DEFINITION_CONSTRUCTOR, new Object[] {});
		} catch (ReflectionException e) {
		    e.printStackTrace();
		    return null;
		}
		result.setClassName(className);
		return result;
	}

	/**
	 * Creates a parameterized algorithmic-type DN to EU conversion definition
	 * with a key/value map of parameters.
	 * 
	 * @param channelId
	 *            the ID of the telemetry channel this EU definition is for
	 * @param className
	 *            the full java class name of the EU algorithm to invoke
	 * @param parameters
	 *            a key-value map of parameters
	 * @return Parameterized Algorithmic IEUDefinition object
	 * 
	 */
	public static IParameterizedAlgorithmicEUDefinition createAlgorithmicEU(String channelId,
			final String className, Map<String, String> parameters) {
	    try {
	        return (IParameterizedAlgorithmicEUDefinition) ReflectionToolkit.createObject(
	                PARAM_ALGO_MAP_DEFINITION_CONSTRUCTOR, new Object[] {channelId, className, parameters});
	    } catch (ReflectionException e) {
	        e.printStackTrace();
	        return null;
	    }
	}

	/**
	 * Creates a parameterized algorithmic-type DN to EU conversion definition
	 * with a list of parameter values.
	 * 
	 * @param channelId
	 *            the ID of the telemetry channel this EU definition is for
	 * @param className
	 *            the full java class name of the EU algorithm to invoke
	 * @param parameters
	 *            a list of parameter values, in order per the dictionary
	 * 
	 * @return Parameterized Algorithmic IEUDefinition object. Parameters map
	 *         will be set by setting the key of each parameter to its index in
	 *         the list + 1. In other words, the first parameter in the supplied
	 *         list will be added to the parameter map with key "1".
	 */
	public static IParameterizedAlgorithmicEUDefinition createAlgorithmicEU(String channelId,
			final String className, List<String> parameters) {
	    try {
	        return (IParameterizedAlgorithmicEUDefinition) ReflectionToolkit.createObject(
	                PARAM_ALGO_LIST_DEFINITION_CONSTRUCTOR, new Object[] {channelId, className, parameters});
	    } catch (ReflectionException e) {
	        e.printStackTrace();
	        return null;
	    }
	}
}
