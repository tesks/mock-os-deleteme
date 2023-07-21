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

import java.util.Map;

/**
 * The IParameterizedAlgorithmicEUDefinition interface is to be implemented by
 * all classes that represent the dictionary definition of an EU (engineering
 * unit) calculation that uses a custom algorithm and takes parameters. 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * IParameterizedAlgorithmicEUDefinition defines the methods that must be
 * implemented by all EU algorithm classes that take arguments other than just
 * the channel DN value. At runtime, the EU is produced by passing the algorithm
 * a channel DN through the IParameterizedEUCalculation interface.
 * 
 *
 */
public interface IParameterizedAlgorithmicEUDefinition extends
     IAlgorithmicEUDefinition {

    /**
     * Adds a parameter to the parameter map.
     * 
     * @param name
     *            the name of the parameter; if null, a name is created to match
     *            the parameter index + 1
     * @param val
     *            the value of the parameter
     */
    public void addParameter(String name, String val);

    /**
     * Gets the map of all parameters as name/value pairs.
     * 
     * @return Map of parameter name to value.
     */
    public Map<String, String> getParameters();

    /**
     * Gets the ID of the channel to which the algorithm applies.
     * 
     * @return channel ID
     */
    public String getChannelId();

    /**
     * Sets the ID of the channel to which the algorithm applies.
     * 
     * @param chan
     *            the channel ID to set; may not be null
     */
    public void setChannelId(String chan);

}