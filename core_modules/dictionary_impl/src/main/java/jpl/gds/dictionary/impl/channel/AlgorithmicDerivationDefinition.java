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
package jpl.gds.dictionary.impl.channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jpl.gds.dictionary.api.channel.DerivationDefinitionException;
import jpl.gds.dictionary.api.channel.DerivationType;
import jpl.gds.dictionary.api.channel.IAlgorithmicChannelDerivation;

/**
 * AlgorithmicDerivationDefinition represents a many-to-many channel derivation
 * in which zero-to-many parent channels are used to generate zero-to-many child
 * channels through the invocation of a user-specified Java algorithm class. It
 * is used to store the definition of an algorithm as loaded from a dictionary.
 * It does not store or use the algorithm instance itself. The actual work is
 * done through the AlgorithmDerivation class at runtime.
 * 
 *
 */
public class AlgorithmicDerivationDefinition extends Object implements IAlgorithmicChannelDerivation
{
    private final List<String> parents  = new ArrayList<String>();
    private final List<String> children = new ArrayList<String>();

    private final List<String> safe_parents  = 
            Collections.unmodifiableList(parents);
    private final List<String> safe_children =
            Collections.unmodifiableList(children);

    private final String _derivationId;

    private String   _algorithmName  = null;
    private String algorithmDescription = null;

    private String triggerId;

    /*
     * The two data structures below are duplicates. parametersList allows
     * us to pick out entries by their order, and parametersMap allows us
     * to pick out entries by a key.
     */
    private final List<String> parametersList = new ArrayList<String>();
    /*
     * changes parametersMap to a TreeMap to preserve the order
     * of the original dictionary
     */
    private final Map<String, String> parametersMap = new TreeMap<String, String>();

    /**
     * Creates an instance of AlgorithmicDerivationDefinition.
     *
     * @param id The unique ID or trigger channel of the derivation.
     *
     * @throws DerivationDefinitionException if the derivation fails
     * 
     *
     */
    AlgorithmicDerivationDefinition(final String id) throws DerivationDefinitionException
    {
        super();

        if ((id == null) || (id.length() == 0))
        {
            throw new DerivationDefinitionException("Missing id");
        }

        _derivationId = id;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#getParametersList()
     */
    @Override
    public List<String> getParametersList()
    {
        return(parametersList);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#getParametersMap()
     */
    @Override
    public Map<String, String> getParametersMap()
    {
        return(parametersMap);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#addParameter(java.lang.String)
     */
    @Override
    public void addParameter(final String parameter)
    {
        parametersList.add(parameter);
        parametersMap.put(String.valueOf(parametersList.size() - 1), parameter);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#putParameter(java.lang.String, java.lang.String)
     */
    @Override
    public void putParameter(final String key, final String parameter)
    {
        parametersMap.put(key, parameter);
        parametersList.add(parameter);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#getParameter(java.lang.String)
     */
    @Override
    public String getParameter(final String key)
    {
        return(parametersMap.get(key));
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IChannelDerivation#getParents()
     */
    @Override
    public List<String> getParents()
    {
        return safe_parents;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IChannelDerivation#getChildren()
     */
    @Override
    public List<String> getChildren()
    {
        return safe_children;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IChannelDerivation#addParent(java.lang.String)
     */
    @Override
    public void addParent(final String parent)
    {
        if (parent != null && parents.contains(parent) == false)
        {
            parents.add(parent);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#addParents(java.util.Collection)
     */
    @Override
    public void addParents(final Collection<String> parents)
    {
        for(String parent : parents)
        {
            addParent(parent);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IChannelDerivation#addChild(java.lang.String)
     */
    @Override
    public void addChild(final String child)
    {
        if (child != null && children.contains(child) == false)
        {
            children.add(child);
        }
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#addChildren(java.util.Collection)
     */
    @Override
    public void addChildren(final Collection<String> children)
    {
        for(String child : children)
        {
            addChild(child);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IChannelDerivation#getId()
     */
    @Override
    public String getId()
    {
        return _derivationId;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#setAlgorithmName(java.lang.String)
     */
    @Override
    public void setAlgorithmName(final String name) 
    {    
        _algorithmName = name;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#getAlgorithmName()
     */
    @Override
    public String getAlgorithmName()
    {
        return _algorithmName;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(final Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (! (o instanceof AlgorithmicDerivationDefinition))
        {
            return false;
        }

        final AlgorithmicDerivationDefinition other = (AlgorithmicDerivationDefinition) o;

        if (! _derivationId.equals(other._derivationId))
        {
            return false;
        }

        if (_algorithmName == null)
        {
            return (other._algorithmName == null);
        }

        return _algorithmName.equals(other._algorithmName);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#getDescription()
     */
    @Override
    public String getDescription()
    {
        return(algorithmDescription);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#setDescription(java.lang.String)
     */

    @Override
    public void setDescription(final String description)
    {
        algorithmDescription = description;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode()
    {
        return _derivationId.hashCode() +
                ((_algorithmName != null) ? _algorithmName.hashCode() : 0);
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder parentBuffer = null;
        for (String parentId : parents)
        {
            if (parentBuffer == null)
            {
                parentBuffer = new StringBuilder(1024);
                parentBuffer.append("    Parents: ");
                parentBuffer.append(parentId);
            }
            else
            {
                parentBuffer.append(", ");
                parentBuffer.append(parentId);
            }
        }

        StringBuilder childBuffer = new StringBuilder();
        for (String childId : children)
        {
            if (childBuffer.length() == 0)
            {
                childBuffer.append("    Children: ");
                childBuffer.append(childId);
            }
            else
            {
                childBuffer.append(", ");
                childBuffer.append(childId);
            }
        }

        StringBuilder value = new StringBuilder(2048);
        value.append(parentBuffer.toString());
        value.append("\n");
        value.append(childBuffer.toString());
        value.append("\n");
        value.append("    Algorithm Name: ");
        value.append(_algorithmName);
        value.append("\n");
        if(algorithmDescription != null)
        {
            value.append("    Algorithm Description: ");
            value.append(algorithmDescription);
        }

        return(value.toString());
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#setTriggerId(java.lang.String)
     */
    @Override
    public void setTriggerId(String triggerChannel) {
        this.triggerId = triggerChannel;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.channel.IAlgorithmicChannelDerivation#getTriggerId()
     */
    @Override
    public String getTriggerId() {
        return this.triggerId;
    }

   /**
    * Gets the derivation type when all that is known is that the object is of
    * a class that implements IChannelDerivation.  
    *
    * @return ALGORITHMIC in all cases
    *
    */

    @Override
    public DerivationType getDerivationType() {

        return DerivationType.ALGORITHMIC;

    }

}
