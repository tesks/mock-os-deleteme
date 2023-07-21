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

import java.util.List;
import java.util.Map;

import jpl.gds.shared.annotation.AmpcsLocked;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Mutator;

/**
 * The IDerivationAlgorithm interface is to be implemented by all custom
 * algorithmic channel derivation classes. Customer classes should not extend
 * this interface.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * The IDerivationAlgorithm interface is to be implemented by all custom
 * algorithmic channel derivation classes. Ihe implementation should extend the
 * DerivationAlgorithmBase subclass, which defines base implementations for the
 * interface methods and helper methods. Derivation implementations should not
 * use classes and interfaces outside of adaptation interfaces/classes.
 * 
 *
 * @see DerivationAlgorithmBase
 */
@CustomerAccessible(immutable = false)
public interface IDerivationAlgorithm extends IAlgorithmUtility {
    /**
     * Performs the actual channel derivation, given parent channel values, and
     * returns child channel values. MUST be overridden by the actual algorithm
     * class. All child channels produced must be present in the current
     * telemetry dictionary.
     *
     * @param parentChannelValues
     *            Map of current values for the parent channels, keyed by
     *            channel ID; never null
     *
     * @return the map of child channel values, keyed by channel ID; should
     *         never be null
     *
     * @throws DerivationException
     *             if the derivation fails in any way. Algorithm implementations
     *             should catch other types of exceptions and only throw this
     *             one.
     */
    public abstract Map<String, IChannelValue> deriveChannels(final Map<String, IChannelValue> parentChannelValues)
            throws DerivationException;

    /**
     * Algorithm classes should use this method to set up any member variables
     * that can hang around and be used for every run of the algorithm. The
     * algorithm's constructor will only ever be called once, not every time the
     * algorithm is run. This method is called BEFORE every single run of the
     * algorithm.
     * <p>
     * If this method is overridden, the override method should invoke
     * super.init().
     */
    public abstract void init();

    /**
     * Algorithm classes should use this method to clean up any member variables
     * that should not hang around and be used during the next run of the
     * algorithm. The algorithm's constructor will only ever be called once, not
     * every time the algorithm is run. This method is called AFTER every single
     * run of the algorithm.
     * <p>
     * If this method is overridden, the override method should invoke
     * super.cleanup().
     */
    public abstract void cleanup();

    /**
     * Gets the return value for the latest algorithm run. This value is reset
     * to 0 when init() is invoked. Any non-0 value will be considered a failure
     * condition by the downlink processing.
     * 
     * @return algorithm completion status (0 for success)
     */
    public int getReturnValue();

    /**
     * Sets the entire set of algorithm parameters prior to invocation of the
     * derivation. To set no parameters, send an empty list and and empty map.
     * <p>
     * This is an AMPCS locked method. It cannot be called by customer-supplied
     * derivation classes.
     * 
     * @param parametersList
     *            list of parameter values as Strings, in the order the are
     *            defined in the telemetry dictionary; may be empty but not null
     * @param parametersMap
     *            map of parameter name to parameter value, for each parameter
     *            declared by name in the telemetry dictionary; may be empty but
     *            not null
     * @throws DerivationException
     *             if invoked by a non-authorized class
     */
    @Mutator
    @AmpcsLocked
    public void setParameters(final List<String> parametersList, final Map<String, String> parametersMap)
            throws DerivationException;

    /**
     * Gets the map of algorithm parameter names and values, keyed by parameter name.
     * These are the parameters named in the telemetry dictionary, and their values
     * are fixed by dictionary as well.
     * 
     * @return map of parameter name to parameter value; never null
     */
    public Map<String, String> getParametersMap();
    
    /**
     * Gets a list of all the algorithm parameter names. These are the fixed parameter
     * values as defined in the telemetry dictionary, in the order they are declared
     * in the dictionary.
     * <p>
     * The preferred method is to access parameters by name using the parameter map.
     * 
     * @return List of Strings, or an empty list if no parameters defined
     */
    public List<String> getParametersList();
    
    /**
     * Gets the value of the parameter at the given index. Parameters are in the same order
     * as the are declared in the algorithm definition in the telemetry dictionary.
     * <p>
     * The preferred method is to access parameters by name.
     * 
     * @param index index of the parameter value to return
     * @return the parameter value at the given index, or null if no such parameter
     */
    public String getParameter(final int index);
    
    /**
     * Gets the value of the parameter with the given name. The parameter name is
     * established by the algorithm definition in the telemetry dictionary.
     * 
     * @param name the name of the parameter to return
     * 
     * @return the parameter value with the given name, or null if no such parameter
     */
    public String getParameter(final String name);
    
    /**
     * Sets the unique derivation identifier associated with this algorithm,
     * which basically identifies the derivations that use the algorithm, prior
     * to its invocation.
     * <p>
     * This is an AMPCS locked method. It cannot be called by customer-supplied
     * derivation classes.
     * 
     * @param derivationId
     *            the derivation ID text to set; should not be null
     * @throws DerivationException
     *             if invoked by a non-authorized class
     */
    @Mutator
    @AmpcsLocked
    public void setDerivationId(final String derivationId) throws DerivationException;
    
    /**
     * Gets the unique derivation identifier associated with this algorithm, 
     * which basically identifies the derivations that use the algorithm in the
     * telemetry dictionary.
     * 
     * @return derivation ID text
     */
    public String getDerivationId();

    /**
     * Sets the list of parent channel IDs for this algorithm, per the
     * definition in the telemetry dictionary, prior to its invocation.
     * <p>
     * This is an AMPCS locked method. It cannot be called by customer-supplied
     * derivation classes.
     * 
     * @param parents
     *            list of channel IDs as Strings; should never be empty or null
     * @throws DerivationException
     *             if invoked by a non-authorized class
     */
    @Mutator
    @AmpcsLocked
    public void setParents(final List<String> parents) throws DerivationException;
    
    /**
     * Gets the list of parent channel IDs for this algorithm, per the
     * definition in the telemetry dictionary.
     * 
     * @return list of channel IDs; never null
     */
    public List<String> getParents();
    
    /**
     * Sets the return value for the latest algorithm run. Must be set by the
     * deriveChannels() method if it is to be set at all. Any non-zero value
     * represents a failure of the algorithm. This value is reset
     * to 0 when initialize() is invoked.
     * 
     * @param returnValue algorithm completion status (0 for success)
     */
    public void setReturnValue(final int returnValue);

    /**
     * Sets the sample factory for use by derivations. This allows derivations
     * to create child samples.
     * <p>
     * This is an AMPCS locked method. It cannot be called by customer-supplied
     * derivation classes
     * 
     * @param factory
     *            the sample factory to set; may not be null
     * @throws DerivationException
     *             if invoked by a non-authorized class
     */
    @Mutator
    @AmpcsLocked
    public void setSampleFactory(IChannelSampleFactory factory) throws DerivationException;

    /**
     * Convenience method to create a new numeric channel value.
     * 
     * @param ci
     *            channel ID of the channel value to create; may not be null
     * @param value
     *            numeric channel value; may not be null; actual type must match
     *            the data type of the channel
     * @return IChannelValue object
     * 
     * @throws InvalidChannelValueException if the data type of the value is not consistent
     *         with the channel dictionary definition
     * @throws IllegalStateException if the channel does not exist in the 
     *         dictionary or is not a numeric channel
     */
    public IChannelValue createChannelValue(String ci, Number value);

    /**
     * Convenience method to create a new string channel value.
     * 
     * @param ci
     *            channel ID of the channel value to create; may not be null
     * @param value
     *            string channel value; may not be null
     * @return IChannelValue object
     */
    public IChannelValue createChannelValue(String ci, String value);
    
    /**
     * Convenience method to create a new boolean channel value.
     * 
     * @param ci
     *            channel ID of the channel value to create; may not be null
     * @param value
     *            the boolean value of the channel
     *            
     * @return IChannelValue object
     */
    public IChannelValue createChannelValue(String ci, boolean value);

}
