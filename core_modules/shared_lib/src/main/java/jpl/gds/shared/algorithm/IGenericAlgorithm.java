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

import java.util.Map;

import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Mutator;

/**
 * Algorithms are constructed by reflection with a no-arg constructor. If an algorithm requires parameters in order to
 * function, this method allows setting those parameters in the algorithm after construction.
 * 
 * TODO: 08/21/2017: Modify the way these algorithms are constructed such that they may take arguments in their
 * constructor
 * 
 */
@CustomerAccessible(immutable = false)
public interface IGenericAlgorithm {
    /**
     * @param args
     *            the arguments required for this algorithm
     */
    @Mutator
	public void setStaticArgs(Map<String, Object> args);
}
