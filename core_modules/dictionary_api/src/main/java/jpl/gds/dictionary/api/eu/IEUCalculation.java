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

import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * The IEUCalculation interface is to be implemented by all classes that perform
 * EU calculation, including custom simple (non-parameterized) EU algorithms.
 * Note that this is the runtime interface to EU calculation classes. The
 * dictionary interface class is IEUDefinition. 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * IEUCalculation defines the methods that must be implemented by all EU
 * calculation classes. These classes must also throw the EUGenerationException
 * if an error of any type occurs in the calculation, and must throw no other
 * exception types.
 * 
 */
@CustomerAccessible(immutable = true)
public interface IEUCalculation {
    /**
     * Computes the Engineering Units (EU) value from an input Data Number (DN).
     * 
     * @param val the input DN
     * @return the computed EU value
     * @throws EUGenerationException if any error occurs in the EU computation
     */
    public abstract double eu(double val) throws EUGenerationException;
}
