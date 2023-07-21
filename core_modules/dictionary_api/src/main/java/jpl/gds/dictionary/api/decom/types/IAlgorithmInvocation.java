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
package jpl.gds.dictionary.api.decom.types;

import java.util.Map;

import jpl.gds.dictionary.api.decom.IDecomStatement;

/**
 * Represents an algorithm invocation declared in a general decom map.
 * Contains the information necessary for a decom processor to
 * identify and call the actual algorithm at runtime.
 * 
 * Each algorithm that can be invoked in custom decom is uniquely
 * identified by the combination of its {@link AlgorithmType}
 * and its algorithm ID string. These should correspond to
 * and actual algorithm definition know by the system, but
 * an IAlgorithmInvocation instance does not imply that such a
 * definition is accessible.
 *
 */
public interface IAlgorithmInvocation extends IDecomStatement {
	
	/**
	 * Get the id of the algorithm to invoke.
	 * @return the string that uniquely identifies, within the algorithm type,
	 *         the algorithm configuration to invoke when performing processing.
	 */
	public String getAlgorithmId();
	
	/**
	 * Get the map of argument names and argument values that should be passed
	 * to the algorithm invocation.  The values can be references to decom variables
	 * or values.
	 * @return the map
	 */
	public Map<String, String> getArgs();
	
	/**
	 * Get the type of algorithm to be invoked.
	 * @return the algorithm type
	 */
	public AlgorithmType getAlgorithmType();

}
