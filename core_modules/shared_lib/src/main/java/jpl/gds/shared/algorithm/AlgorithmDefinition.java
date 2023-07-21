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
package jpl.gds.shared.algorithm;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AlgorithmDefinition is a simple data class for grouping configuration parameters.
 * It is immutable and should stay that way.  Any collections it returns from accessor methods
 * are unmodifiable.
 */
public class AlgorithmDefinition {

	private final String id;
	private final String algorithmClass;
	private final Map<String, Object> staticArgs;
	private final List<String> varArgs;

	/**
	 * Create an AlgorithmDefinition instance
	 * @param id the ID uniquely identify this algorithm.
	 * @param algorithmClass the fully qualified class name, as a string, that this definition represents an 
	 * 		  an instance of.
	 * @param staticArgs map of argument names to values that remain constant across invocations of the algorithm.
	 * @param varArgs the list of parameter names that should be provided on each invocation of this algorithm.
	 */
	public AlgorithmDefinition(String id, String algorithmClass, Map<String, Object> staticArgs, List<String> varArgs) {
		this.id = id;
		this.algorithmClass = algorithmClass;
		this.staticArgs = staticArgs;
		this.varArgs = varArgs;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AlgorithmDefinition [id=").append(id).append(", algorithmClass=").append(getAlgorithmClass())
				.append(", staticArgs=").append(staticArgs).append(", varArgs=").append(varArgs).append("]");
		return builder.toString();
	}

	/**
	 * @return the fully qualified Java class name for the algorithm
	 */
	public String getAlgorithmClass() {
		return algorithmClass;
	}

	/**
	 * @return the algorithm ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return unmodifiable view on the static argument name-to-value map
	 */
	public Map<String, Object> getStaticArgs() {
		return Collections.unmodifiableMap(staticArgs);
	}

	/**
	 * 
	 * @return unmodifiable view on the variable argument names list
	 */
	public List<String> getVarArgs() {
		return Collections.unmodifiableList(varArgs);
	}
}