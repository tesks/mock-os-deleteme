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
package jpl.gds.dictionary.api.decom.params;

import java.util.HashMap;
import java.util.Map;

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.types.AlgorithmType;
import jpl.gds.dictionary.api.decom.types.IAlgorithmInvocation;

/**
 * Parameter builder class for creating {@link IAlgorithmInvocation} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class AlgorithmParams implements IDecomDefinitionParams {
	
	private AlgorithmType type;
	private String id;
	private Map<String, String> args = new HashMap<>();

	/**
	 * Add add an argument name / value pair.
	 * @param argName the algorithm's argument name
	 * @param value the variable value, which may be a value  encoded as a string
	 * 		  or a variable name
	 */
	public void addArg(String argName, String value) {
		args.put(argName, value);
	}

	/**
	 * Get the map of argument names to argument values
	 * stored in this instance.
	 * @return the map of arguments and their values
	 */
	public Map<String, String> getArgs() {
		return args;
	}
	

	/**
	 * Set the algorithm ID. 
	 * @param id the identifier for the algorithm configuration
	 * 		  Cannot be null.
	 * @throws IllegalArgumentException if the id String is null
	 */
	public void setAlgoId(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Algorithm ID cannot be null");
		}
		this.id = id;
	}
	
	/**
	 * Set the algorithm type.
	 * @param type the algorithm type associated with this parameter instance
	 */
	public void setType(AlgorithmType type) {
		this.type = type;
	}

	/**
	 * Get the algorithm type
	 * @return the algorithm type associated with this parameter instance
	 */
	public AlgorithmType getType() {
		return type;
	}
	
	/**
	 * Get the algorithm ID
	 * @return the identifier for the algorithm configuration
	 */
	public String getAlgoId() {
		return id;
	}

	@Override
	public void reset() {
		// Create a new hashmap; Do not clear
		// since someone else may hold on to the reference
		args = new HashMap<>();
		// No reasonable defaults really exist; set to null
		type = null;
		id = null;
	}

}
