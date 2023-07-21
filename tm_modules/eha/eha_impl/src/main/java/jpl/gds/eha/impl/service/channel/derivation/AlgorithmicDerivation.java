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
package jpl.gds.eha.impl.service.channel.derivation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.channel.IAlgorithmicChannelDerivation;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;
import jpl.gds.eha.api.channel.BadDerivationClassException;
import jpl.gds.eha.api.channel.IChannelLad;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.channel.api.DerivationAlgorithmBase;
import jpl.gds.eha.channel.api.DerivationException;
import jpl.gds.eha.channel.api.IChannelValue;
import jpl.gds.eha.channel.api.IDerivationAlgorithm;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.types.Pair;


/**
 * AlgorithmicDerivation represents a many-to-many channel derivation in which
 * zero-to-many parent channels are used to generate zero-to-many child channels
 * through the invocation of a algorithm.
 *
 * The actual work is done through the user-specified algorithm class, which 
 * must extend DerivationAlgorithmBase.
 *
 *
 * @see IDerivationAlgorithm 
 * 
 * 5/1/14. Separated this class into two: this one,
 *          which performs the derivation, and AlgorithmicDerivationDefinition,
 *          which contains the dictionary definition of the derivation. The
 *          constructor now requires the definition object. The methods that
 *          perform the derivation here must go through the definition object 
 *          to get the attributes they need. Also, this object no longer loads
 *          the algorithm class at the time the dictionary is parsed. The
 *          new loadAlgorithm() method must be invoked.
 * Suppressed deprecation warnings for
 *          ACVMap, which has been temporarily deprecated to discourage its
 *          use by customers.
 */
public class AlgorithmicDerivation extends Object 
{

    private Class<?> _algorithmClass = null;
    private IDerivationAlgorithm algorithm = null;
    private int      _errors         = 0;

    private final IAlgorithmicChannelDerivation derivationDef;
    private final ClassLoader loader;
    protected ICommandDefinitionProvider opcodeProvider;

    /**
     * Creates an instance of AlgorithmicDerivation.
     *
     * @param def The dictionary definition of the definition.
     * @param loader class loader to use
     *
     * @throws DerivationException if the derivation fails
     */
    public AlgorithmicDerivation(final IAlgorithmicChannelDerivation def, final ClassLoader loader) throws DerivationException
    {
        super();

        derivationDef = def;
        this.loader = loader;
    }

    /**
     * Gets the dictionary definition object for this derivation.
     * 
     * @return definition object
     */
    public IAlgorithmicChannelDerivation getDefinition() {
        return this.derivationDef;
    }

    /**
     * Increment the error count, which indicates how many time this algorithm
     * has generated an exception in the current downlink session.
     *
     * @return The updated error count.
     */
    public int incrementErrorCount()
    {
        ++_errors;

        return _errors;
    }

    /**
     * 
     * Get the error count, which indicates how many time this algorithm
     * has generated an exception in the current downlink session.
     * 
     * @return The error count.
     */
    public int getErrorCount()
    {
        return _errors;
    }

    /**
     * Loads the class of the algorithm to invoke. The class is the same as the
     * algorithm name.
     *
     * @throws BadDerivationClassException
     *             if the java class cannot be loaded
     */
    public void loadAlgorithmClass() throws BadDerivationClassException
    {

        if (_algorithmClass != null) {
            return;
        }

        final String _algorithmName  = this.derivationDef.getAlgorithmName();;
        _algorithmClass = null;

        try
        {
            _algorithmClass = Class.forName(_algorithmName, true, loader);
        }
        catch (final ClassNotFoundException cnfe)
        {
            throw new BadDerivationClassException("Unable to load class '" +
                    _algorithmName           +
                    "'",
                    cnfe);
        }
        catch (final NoClassDefFoundError ncdfe)
        {
            throw new BadDerivationClassException("Unable to load class '" +
                    _algorithmName           +
                    "'",
                    ncdfe);
        }

        if (! DerivationAlgorithmBase.class.isAssignableFrom(_algorithmClass))
        {
            throw new BadDerivationClassException("Class '"                 +
                    _algorithmName            +
                    "' is not a subclass of " +
                    DerivationAlgorithmBase.class.getName());
        }
    }

    /**
     * Gets the Java class object for the algorithm to invoke.
     * 
     * This is derived from the algorithm name.
     *
     * @return the Class of the algorithm
     */
    public Class<?> getAlgorithmClass()
    {
        return _algorithmClass;
    }


    /**
     * Calls the user-defined (or provided standard) algorithm to derive the
     * child channels from the parent channels.
     *
     * @param appContext
     *            current application context
     * @param parents
     *            Map of parent/parameter channels
     *
     * @return Map of child/result channels and integer status
     *
     * @throws DerivationException
     *             if the algorithm name is not set or the algorithm class
     *             cannot be loaded
     * 
     */
    public Pair<ACVMap, Integer> deriveChannels(final ApplicationContext appContext, final ACVMap parents)
            throws DerivationException
            {
        final DictionaryProperties dictConfig = appContext.getBean(DictionaryProperties.class);
        final IChannelLad lad = appContext.getBean(IChannelLad.class);
        final ICommandDefinitionProvider commandDefProvider = appContext.getBean(ICommandDefinitionProvider.class);
        if (commandDefProvider == null) {
            throw new DerivationException("OpCode to Stem Map is null.");
        }
        
        if (dictConfig == null) {
            throw new DerivationException("Dictionary Configuration is null.");
        }
        
        if (lad == null) {
            throw new DerivationException("Global LAD is null.");
        }
        
        /*
         * Load the algorithm class if it has not
         * been. The dictionary parsers no longer do it. Technically, we should
         * not have to do it here, because the class should have been loaded
         * when this object was added to the DerivationMap. This is just in case
         * someone ever tries to use this without the DerivationMap.
         */
        if (_algorithmClass == null)
        {
            try {
                loadAlgorithmClass();
            } catch (final BadDerivationClassException e) {
                throw new DerivationException("Could not load algorithm class: " + e.getClass());
            }

        }

        if (parents == null)
        {
            throw new DerivationException("No parents for " + this.derivationDef.getAlgorithmName());
        }

        if(algorithm == null)
        {
            try
            {
                algorithm = (IDerivationAlgorithm) _algorithmClass.newInstance();
                algorithm.setParents(this.derivationDef.getParents());
                algorithm.setParameters(this.derivationDef.getParametersList(), this.derivationDef.getParametersMap());
                algorithm.setDerivationId(this.derivationDef.getId());
                algorithm.setSampleFactory(new ExternalChannelValueFactory(lad.getDefinitionProvider()));
                algorithm.setChannelDefinitionMap(lad.getDefinitionProvider().getChannelDefinitionMap());
                algorithm.setLogger(TraceManager.getTracer(appContext, Loggers.TLM_DERIVATION));
                algorithm.setDictionaryProperties(dictConfig);
                algorithm.setLadProvider(lad);
                try {
                    final ISequenceDefinitionProvider dict = appContext.getBean(ISequenceDefinitionProvider.class);
                    algorithm.setSequenceDictionary(dict);
                } catch (final Exception e) {
                    TraceManager.getDefaultTracer(appContext)
                                .warn("No sequence dicitonary found; sequence operations not available in derivations");
                }
                algorithm.setOpcodeToStemMap(commandDefProvider.getStemByOpcodeMap());
            }
            catch (final Exception e)
            {
                throw new DerivationException("Could not create instance for " + this.derivationDef.getAlgorithmName(),e);
            }
        }

        for (final String ci : this.derivationDef.getParents())
        {
            final IChannelDefinition cd = lad.getDefinitionProvider().getDefinitionFromChannelId(ci);

            if (cd == null)
            {
                MissingChannels.reportMissingParent(ci);

                return new Pair<ACVMap,Integer>(ACVMap.getEmptyMap(), 0);
            }
        }

        for (final String ci : this.derivationDef.getChildren())
        {
            final IChannelDefinition cd = lad.getDefinitionProvider().getDefinitionFromChannelId(ci);

            if (cd == null)
            {
                MissingChannels.reportMissingChild(ci);

                return new Pair<ACVMap,Integer>(ACVMap.getEmptyMap(), 0);
            }
        }

        // Do the real work, not trusting user's code
        Pair<ACVMap, Integer> result = null;
        try
        {
            result = derive(algorithm, new ACVMap(parents));
        }
        catch (final ThreadDeath td)
        {
            throw td;
        }
        catch (final Throwable t)
        {
            t.printStackTrace();
            throw new DerivationException("Error running " + this.derivationDef.getAlgorithmName(), t);
        }

        if (result == null)
        {
            return new Pair<ACVMap, Integer>(ACVMap.getEmptyMap(), 0);
        }

        final ACVMap children = result.getOne();
        final int    status   = result.getTwo();

        if (children == null)
        {
            return new Pair<ACVMap, Integer>(ACVMap.getEmptyMap(), status);
        }

        final Set<String> cids = children.keySet();

        final List<String> safe_children = this.derivationDef.getChildren();

        if (! safe_children.containsAll(cids))
        {
            final Set<String> extra = new HashSet<String>(cids);

            extra.removeAll(safe_children);

            final StringBuilder sb = new StringBuilder(this.derivationDef.getAlgorithmName());

            sb.append(" generated non-child channel(s): ");

            boolean first = true;

            for (final String ci : extra)
            {
                if (! first)
                {
                    sb.append(",");
                }
                else
                {
                    first = false;
                }

                sb.append(ci);
            }

            throw new DerivationException(sb.toString());
        }

        return new Pair<ACVMap, Integer>(new ACVMap.SafeACVMap(children),
                status);
            }


    /**
     * Perform channel derivation.
     *
     * @param parentChannelValues Map of current values for the parent channels
     *
     * @return the map of child channel values and integer status
     *
     * @throws DerivationException if the derivation fails
     */
	private final Pair<ACVMap, Integer> derive(final IDerivationAlgorithm algorithm, final ACVMap parentChannelValues) throws DerivationException
    {
        algorithm.init();
        
        // R8 Refactor TODO - The ACVMap object seems unncessary?
        final Map<String, IChannelValue> tempParentMap = new HashMap<String, IChannelValue>(parentChannelValues);
        final Map<String, IChannelValue> tempResultMap = algorithm.deriveChannels(tempParentMap);
        final ACVMap result = new ACVMap();
        if (tempResultMap != null) {
            tempResultMap.forEach((k,v)->result.put(k, (IServiceChannelValue) v));
        }
        final Pair<ACVMap, Integer> pairResult = new Pair<ACVMap,Integer>(result, algorithm.getReturnValue());
        algorithm.cleanup();
        return pairResult;
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

        if (! (o instanceof AlgorithmicDerivation))
        {
            return false;
        }

        final AlgorithmicDerivation other = (AlgorithmicDerivation) o;

        return this.derivationDef.equals(other.getDefinition());
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode()
    {
        return this.derivationDef.hashCode();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.derivationDef.toString();
    }
}
