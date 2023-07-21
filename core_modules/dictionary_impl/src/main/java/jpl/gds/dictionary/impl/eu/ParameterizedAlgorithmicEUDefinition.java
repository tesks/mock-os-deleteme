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
package jpl.gds.dictionary.impl.eu;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jpl.gds.dictionary.api.eu.EUDefinitionFactory;
import jpl.gds.dictionary.api.eu.EUType;
import jpl.gds.dictionary.api.eu.IParameterizedAlgorithmicEUDefinition;
import jpl.gds.dictionary.api.eu.IParameterizedEUCalculation;

/**
 * This class represents the dictionary definition of an algorithmic DN to EU
 * conversion for channel values, in which the algorithm that supports
 * parameters from the dictionary. The runtime EU computation is performed by a
 * Java class that must implement IParameterizedEUCalculation.
 * 
 *
 *
 * @see EUDefinitionFactory
 * @see IParameterizedEUCalculation
 */
public class ParameterizedAlgorithmicEUDefinition extends AlgorithmicEUDefinition 
    implements IParameterizedAlgorithmicEUDefinition {

    /**
     * The map of parameters to the algorithm.
     */
    private Map<String, String> parameters = new TreeMap<String, String>();

    /**
     * The channel ID to which this algorithm applies.
     */
    private String channelId;

    /**
     * Constructor.
     * 
     * @param channelId
     *            the ID of the channel to which the algorithm applies.
     * @param className
     *            the full package name of the user-supplied algorithm class
     * @param params
     *            the key/value map of parameters. If these were named in the
     *            dictionary, the map is keyed by name. If they were positional,
     *            the keys are the string equivalent of the parameter numbers,
     *            starting with "1". If the map is null, the map is set to an
     *            empty map.
     * 
     */
    ParameterizedAlgorithmicEUDefinition(final String channelId,
            final String className, final Map<String, String> params) {
        super(className);
        setChannelId(channelId);

        if (params != null) {
            this.parameters = params;
        }
    }

    /**
     * Constructor.
     * 
     * @param channelId
     *            the ID of the channel to which the algorithm applies.
     * @param className
     *            the full package name of the user-supplied algorithm class
     * @param params
     *            the positional list of parameter values.
     */
    ParameterizedAlgorithmicEUDefinition(final String channelId,
            final String className, final List<String> params) {
        super(className);
        setChannelId(channelId);
        if (params != null) {
            for (final String p : params) {
                addParameter(null, p);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.IParameterizedAlgorithmicEUDefinition#addParameter(java.lang.String,
     *      java.lang.String)
     */
    @Override
    public void addParameter(final String name, final String val) {
        if (name != null) {
            parameters.put(name, val);
        } else {
            final String numericName = String.valueOf(parameters.size() + 1);
            parameters.put(numericName, val);
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.IParameterizedAlgorithmicEUDefinition#getChannelId()
     */
    @Override
    public String getChannelId() {
        return this.channelId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.IParameterizedAlgorithmicEUDefinition#setChannelId(java.lang.String)
     */
    @Override
    public void setChannelId(final String chan) {
        if (chan == null) {
            throw new IllegalArgumentException(
                    "channel ID cannot be set to null");
        }
        this.channelId = chan;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.impl.impl.api.eu.IParameterizedAlgorithmicEUDefinition#getParameters()
     */
    @Override
    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.impl.eu.AlgorithmicEUDefinition#getEuType()
     */
    @Override
    public EUType getEuType() {
        return EUType.PARAMETERIZED_ALGORITHM;
    }
}
