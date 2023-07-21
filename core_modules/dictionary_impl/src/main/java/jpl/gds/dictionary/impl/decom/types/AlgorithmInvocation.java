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
package jpl.gds.dictionary.impl.decom.types;

import java.util.Collections;
import java.util.Map;

import jpl.gds.dictionary.api.decom.params.AlgorithmParams;
import jpl.gds.dictionary.api.decom.types.AlgorithmType;
import jpl.gds.dictionary.api.decom.types.IAlgorithmInvocation;

/**
 * Immutable data class for holding data associated with {@link IAlgorithmInvocation}
 * statements in decommutation maps.
 *
 */
public class AlgorithmInvocation implements IAlgorithmInvocation {

	private final String algorithmId;
	private final AlgorithmType algorithmType;
	private final Map<String, String> args;

	/**
	 * Create a new instance.
	 * @param params the parameters that will be used to initialize
	 * 		  this objects fields
	 */
	public AlgorithmInvocation(AlgorithmParams params) {
		algorithmId = params.getAlgoId();
		algorithmType = params.getType();
		args = params.getArgs();
	}

	@Override
	public String getAlgorithmId() {
		return algorithmId;
	}

	@Override
	public Map<String, String> getArgs() {
		return Collections.unmodifiableMap(args);
	}

	@Override
	public AlgorithmType getAlgorithmType() {
		return algorithmType;
	}

}
