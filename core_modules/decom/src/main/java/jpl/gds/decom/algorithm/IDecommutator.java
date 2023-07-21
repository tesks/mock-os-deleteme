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
package jpl.gds.decom.algorithm;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import jpl.gds.decom.IDecomDelegate;
import jpl.gds.shared.algorithm.IGenericAlgorithm;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.types.BitBuffer;

/**
 * An interface to be implemented by classes that provide
 * custom decommutation logic.  
 *
 */
@CustomerAccessible(immutable = false)
public interface IDecommutator extends IGenericAlgorithm {

	/**
	 * Consumes data from the provided buffer. No output
	 * is explicitly required for implementations of this method.
	 * Implementations may wish to provide output in the form of the
	 * return values of {@link #collectChannelValues()} or {@link #collectEvrs()}
	 * @param buffer the input data buffer to operate on
	 * @param args the runtime arguments to the algorithm
	 */
	public void decom(BitBuffer buffer, Map<String, Object> args);
	
	/**
	 * Consumes data from the provided buffer. This method will be called
	 * by an {@link IDecomDelegate} if it has such an output stream.
	 * @param buffer the input data buffer to operate on
	 * @param args the runtime arguments to the algorithm
	 * @param outStream output stream to write decommed data to
	 */
	public void decom(BitBuffer buffer, Map<String, Object> args, OutputStream outStream);
	
	/**
     * Returns and clears the list of channel value builders
     * that may have been populated by the last call to {@link #decom(BitBuffer, Map, OutputStream)}
     * These builders each represent a decommutated channel value.
     * 
     * @return the list of builders
     */
	public List<ChannelValueBuilder>  collectChannelValues();

	/**
     * Returns and clears the list of EVR builders
     * that may have been populated by the last call to {@link #decom(BitBuffer, Map, OutputStream)}
     * These builders each represent a decommutated event record.
     * 
     * @return the list of builders
     */
	public List<EvrBuilder> collectEvrs();

}
