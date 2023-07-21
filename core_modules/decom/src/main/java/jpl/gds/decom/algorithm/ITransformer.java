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

import java.util.Map;

import jpl.gds.shared.algorithm.IGenericAlgorithm;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.types.BitBuffer;

/**
 * Interface for algorithms that transforms input into
 * output data that can be generically decommutated.
 * 
 * Decom processors will continue applying generic decom starting with the
 * first bit returned by the invocation of this algorithm.
 * 
 *
 */
@CustomerAccessible(immutable = true)
public interface ITransformer extends IGenericAlgorithm {

    /**
     * Transforms the data given in the input buffer into an arbitrary set of bytes.
     * 
     * @param data
     *            read-only input data buffer
     * @param args
     *            the runtime arguments to the algorithm
     * @return transformed data
     */
    public BitBuffer transform(BitBuffer data, Map<String, Object> args);
}
