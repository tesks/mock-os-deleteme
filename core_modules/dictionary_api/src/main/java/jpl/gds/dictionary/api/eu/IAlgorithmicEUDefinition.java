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

/**
 * The IAlgorithmicEUDefinition interface is to be implemented by all classes that
 * represent the dictionary definition of an EU (engineering unit) calculation
 * that uses a custom algorithm.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * IAlgorithmicEUDefinition defines the methods that must be implemented by all EU
 * algorithm classes. At runtime, the EU is produced by passing the algorithm 
 * DN through the IEUCalculation interface. This basic interface assumes there
 * are no other parameters to the algorithm.
 * 
 *
 */
public interface IAlgorithmicEUDefinition extends IEUDefinition {

    /**
     * Gets the Java class name of the class that contains the EU conversion.
     * @return full Java class name (with package). This class must implement
     * IEUCalculation.
     */
    public String getClassName();

    /**
     * Sets the Java class name of the class that contains the EU conversion.
     * @param name full Java class name (with package). This class must implement
     * IEUCalculation.
     */
    public void setClassName(String name);

}