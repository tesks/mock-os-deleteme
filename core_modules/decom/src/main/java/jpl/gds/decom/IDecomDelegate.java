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
package jpl.gds.decom;

import java.util.Map;

import jpl.gds.decom.algorithm.DecomArgs;
import jpl.gds.decom.algorithm.IDecommutator;
import jpl.gds.decom.exception.DecomException;
import jpl.gds.shared.types.BitBuffer;

/**
 * This interface allows the {@link DecomEngine} to delegate the execution
 * of any custom {@link IDecommutator} algorithms encountered  to another object.
 * This is needed because unlike other custom algorithms, some side effect such as creating
 * telemetry objects or writing to an output stream.
 * 
 * IDecomDelegate implementations are responsible for providing certain arguments relating to the telemetry context
 * in which decom is being performed, e.g., SCLK, to the IDecommutator algorithm it calls.
 * @see DecomArgs
 *
 */
public interface IDecomDelegate {

	/**
	 * Executes the given algorithm on the data. The delegate implementation class
	 * may have side-effects, such as storing results in a data structure or writing to an
	 * output stream.  However, it should remain aware that its performance affects the performance
	 * of decommutation.
	 * Implementers MUST throw only DecomException or its subclasses..
	 * @param algorithm the algorithm to invoke
	 * @param data the data to pass to the algorithm
	 * @param args the runtime arguments to the algorithm.
	 * @throws DecomException if any error occurs during decommutation.
	 */
	public void decom(IDecommutator algorithm, BitBuffer data, Map<String, Object> args) throws DecomException;
}
