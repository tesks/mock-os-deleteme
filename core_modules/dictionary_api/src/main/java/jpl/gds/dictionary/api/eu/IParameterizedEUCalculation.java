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

import java.util.Map;

import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * The IParameterizedEUCalculation interface is to be implemented by classes
 * that perform EU calculation using a parameterized algorithm.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * IParameterizedEUCalculation defines the methods that must be implemented EU
 * calculation classes that need parameters from the dictionary. These classes
 * must also throw the EUGenerationException if an error of any type occurs in
 * the calculation, and must throw no other exception types.
 * 
 */
@CustomerAccessible(immutable = true)
public interface IParameterizedEUCalculation {
	/**
	 * Computes the Engineering Units (EU) value from an input channel (ID) and
	 * Data Number (DN), using the supplied parameter map.
	 * 
	 * @param channelId
	 *            the ID of the channel to which the DN value belongs
	 * @param parameters
	 *            a key-value map of named parameters from the dictionary. If
	 *            the parameters in the dictionary had no names (were positional)
	 *            then the key in the parameter map is set by setting the key of
	 *            each parameter to its index in the list + 1. In other words,
	 *            the first parameter in the supplied list will be added to the
	 *            parameter map with key "1"
	 * @param dn
	 *            the input DN
	 * @return the computed EU value
	 * @throws EUGenerationException
	 *             if any error occurs in the EU computation
	 */
	public double eu(String channelId, Map<String, String> parameters, double dn)
			throws EUGenerationException;
}
