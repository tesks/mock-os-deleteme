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
package jpl.gds.dictionary.api.channel;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The IAlgorithmicChannelDerivation interface is to be implemented by all 
 * channel derivation definition classes that use a custom algorithm to 
 * compute child channels from parent channels.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * IAlgorithmicChannelDerivation defines methods needed to interact with 
 * Algorithmic Channel Derivation objects as required by the IChannelDictionary 
 * interface. It is primarily used by channel file parser implementations 
 * in conjunction with the ChannelDerivationFactory, which is used to create 
 * actual Channel Derivation objects in the parsers. IChannelDictionary objects 
 * should interact with Channel Derivation objects only through the Factory and
 * the IChannelDerivation interface and its extensions. Interaction with the 
 * actual Channel Derivation implementation classes in an IChannelDictionary 
 * implementation is contrary to multi-mission development standards.
 * 
 *
 * @see IChannelDictionary
 * @see ChannelDerivationFactory
 */
public interface IAlgorithmicChannelDerivation extends IChannelDerivation {

    /**
     * Gets a list of all the algorithm parameter names.
     * 
     * @return List of Strings, or an empty list if no parameters defined
     */
    public List<String> getParametersList();

    /**
     * Gets the Map of parameters to this algorithm as key-value pairs, where
     * the key is the parameter name.
     * 
     * @return Map of String key to String value
     */
    public Map<String, String> getParametersMap();

    /**
     * Adds a parameter to the algorithm. Since no parameter name is supplied,
     * the key for the parameter is based upon index. The first parameter added
     * has key "1", the second has key "2", etc.
     * 
     * @param parameter
     *            the parameter value to add
     */
    public void addParameter(String parameter);

    /**
     * Adds a parameter to the map of parameters for this algorithm.
     * 
     * @param key
     *            parameter name
     * @param parameter
     *            parameter value
     */
    public void putParameter(String key, String parameter);

    /**
     * Gets the value of the parameter with the given name.
     * 
     * @param key
     *            the name of the parameter to return
     * @return the parameter value with the given name, or null if no such
     *         parameter
     */
    public String getParameter(String key);

    /**
     * Adds parent channels to this derivation.
     *
     * @param parents
     *            the list of parent channel IDs to add
     */
    public void addParents(Collection<String> parents);

    /**
     * Add child channels to this derivation.
     *
     * @param children
     *            the list of children channel IDs to add
     */
    public void addChildren(Collection<String> children);

    /**
     * Sets the name of the algorithm to invoke. This should be the name of a
     * java class.
     *
     * @param name
     *            the name of the algorithm
     */
    public void setAlgorithmName(String name);

    /**
     * Gets the name of the algorithm to invoke. This should be the name of a
     * java class.
     *
     * @return the name of the algorithm
     */
    public String getAlgorithmName();

    /**
     * Gets the optional description string for this algorithm.
     * 
     * @return description text or null if none set
     */
    public String getDescription();

    /**
     * Sets the optional description string for this algorithm.
     * 
     * @param description
     *            the description text to set
     */
    public void setDescription(String description);

    /**
     * Sets the trigger channel identifier for this derivation. If set, and the
     * mission is configured to use trigger channels, the derivation will be
     * triggered when this channel is seen incoming. If null, the derivation
     * will trigger when any incoming parent channel is seen.
     * 
     * @param triggerChannel
     *            the ID of the trigger channel, which must be a parent if
     *            non-null, and may be null
     */
    public void setTriggerId(String triggerChannel);

    /**
     * Gets the trigger channel identifier for this derivation. If set, and the
     * mission is configured to use trigger channels, the derivation will be
     * triggered when this channel is seen incoming. If null, the derivation
     * will trigger when any incoming parent channel is seen.
     * 
     * @return the ID of the trigger channel, which must be a parent if
     *         non-null, and may be null
     */
    public String getTriggerId();

}