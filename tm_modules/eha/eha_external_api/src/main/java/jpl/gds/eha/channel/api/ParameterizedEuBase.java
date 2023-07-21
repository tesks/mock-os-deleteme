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
package jpl.gds.eha.channel.api;

import java.util.Map;

import jpl.gds.dictionary.api.eu.IParameterizedEUCalculation;
import jpl.gds.shared.annotation.CustomerExtensible;

/**
 * ParameterizedEuBase is an abstract class that implements the
 * IParameterizedEUCalculation interface. It should be used as a base class for
 * building custom parameterized EU computation classes. The subclass must
 * define the eu() method. This class provides access the the VCID, station ID, and
 * realtime status of the channel sample whose EU is being computed by adding them to
 * the parameter map for the algorithm.  Because it extends
 * GeneralAlgorithmBase, it also provides utility methods for accessing the
 * channel LAD and performing some basic dictionary-related functions.
 * 
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 *
 *
 * @see IParameterizedEUCalculation
 */
@CustomerExtensible(immutable = true)
public abstract class ParameterizedEuBase extends GeneralAlgorithmBase implements IParameterizedEUCalculation {

    /** Mapping key for Station parameters */
    public static final String STATION_PARAM  = "builtin.eu.parentStation";

    /** Mapping key for Parent VCID */
    public static final String VCID_PARAM     = "builtin.eu.parentVcid";

    /** Mapping key for Realtime parameters */
    public static final String REALTIME_PARAM = "builtin.eu.parentIsRealtime";

    /**
     * Convenience method to get the built-in station parameter from the
     * parameter map for the channel whose EU is currently being computed.
     * 
     * @param parameters
     *            Map of parameters to the EU calculation
     * @return the station ID associated with the parent channel
     * 
     */
    public static int getStation(final Map<String, String> parameters) {
        return Integer.valueOf(parameters.get(STATION_PARAM));
    }

    /**
     * Convenience method to get the built-in virtual channel ID parameter from
     * the parameter map for the channel whose EU is currently being computed.
     * 
     * @param parameters
     *            Map of parameters to the EU calculation
     * @return the VCID associated with the parent channel
     * 
     */
    public static int getVcid(final Map<String, String> parameters) {
        return Integer.valueOf(parameters.get(VCID_PARAM));
    }

    /**
     * Convenience method to get the built-in realtime parameter from the
     * parameter map for the channel whose EU is currently being computed.
     * 
     * @param parameters
     *            Map of parameters to the EU calculation
     * @return the realtime flag associated with the parent channel; true if the
     *         channel sample is realtime, false if not
     * 
     */
    public static boolean getIsRealtime(final Map<String, String> parameters) {
        return Boolean.valueOf(parameters.get(REALTIME_PARAM));
    }

}
