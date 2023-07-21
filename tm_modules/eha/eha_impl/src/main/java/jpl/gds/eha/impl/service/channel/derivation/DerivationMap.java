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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.channel.IAlgorithmicChannelDerivation;
import jpl.gds.dictionary.api.channel.IBitUnpackChannelDerivation;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.channel.IChannelDerivation;
import jpl.gds.eha.api.channel.BadDerivationClassException;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.eha.channel.api.DerivationException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.types.Pair;


/**
 * DerivationMap stores derivation objects for all defined channel derivations,
 * accessible by parent channel id. It uses a singleton design pattern.
 *
 * Algorithmic derivations are matched if they have the same id and algorithm.
 * That is required when the id is a trigger channel.
 *
 */
public class DerivationMap extends Object
{
    
    private final Tracer log;

    
    /*  10/29/14. Get flag indicating whether we are doing derivations. */
    /*  11/22/2016: Moved Channel Processing Properties to EhaProperties */
    private final boolean                                          doDerivation;
    
    /**
     * Set of bad derivation algorithms that have already been reported.
     * 
     */
    private final SortedSet<String> badAlgorithms = new TreeSet<String>();

    private final Map<String, Set<BitUnpackDerivation>> _parentToBitDerivations = 
            new HashMap<String, Set<BitUnpackDerivation>>(64);

    private final Map<String, Set<AlgorithmicDerivation>> _parentToAlgoDerivations = 
            new HashMap<String, Set<AlgorithmicDerivation>>(64);

    private final Map<Pair<String, String>, AlgorithmicDerivation> _idToAlgoDerivation = 
            new HashMap<Pair<String, String>, AlgorithmicDerivation>(64);

    private final Map<String, AlgorithmicDerivation> _childToAlgoDerivation = 
            new HashMap<String, AlgorithmicDerivation>(64);

    private final ClassLoader loader;
    
    /**
     * Creates an instance of DerivationMap. 
     * 
     * @param appContext the current application context
     */
    public DerivationMap(final ApplicationContext appContext)
    {
        super();
        log = TraceManager.getTracer(appContext, Loggers.TLM_DERIVATION);
        doDerivation = appContext.getBean(EhaProperties.class).isDerivationEnabled();

        loader = appContext.getBean(AmpcsUriPluginClassLoader.class); 
        
        populate(appContext.getBean(IChannelDefinitionProvider.class).getChannelDerivations());
        
    }

    /**
     * Removes all derivations from the map.
     */
    private void clear()
    {
        _parentToBitDerivations.clear();

        _parentToAlgoDerivations.clear();

        _idToAlgoDerivation.clear();
        
        _childToAlgoDerivation.clear();
        
        badAlgorithms.clear();
        
    }
    
    private void populate(final List<IChannelDerivation> derivations) {
        
        clear();
        /* 
         * Do not add derivations to the map if
         * derivation is not enabled, because this step tries to load the 
         * derivation classes.
         */
        if (derivations != null && doDerivation) {
            for (final IChannelDerivation derivation: derivations) {
                try {
                    /* Note this step now tries to load
                     * the derivation class for algorithmic derivations.
                     */
                    addDerivation(derivation);
                } catch (final DerivationException e) {
                    log.error("Problem loading channel derivation: " + e.getMessage());
                } catch (final BadDerivationClassException be) {
                    /*
                     * Inability to load a derivation class
                     * now results in this exception. Log this event only once for
                     * each bad class we find.
                     */
                    final String clazz = be.getDerivationClass();
                    if (!this.badAlgorithms.contains(clazz)) {
                        log.warn(" Invalid channel derivation: Algorithm is not found or algorithm name is invalid: " + clazz);
                        this.badAlgorithms.add(clazz);
                    }
                }
            }
        }
        
    }
    

    /**
     * Adds a derivation to the map.
     *
     * @param idef the IChannelDerivation to add
     *
     * @throws DerivationException if the channel definition is null
     * 
     */
    private void addDerivation(final IChannelDerivation idef)
            throws DerivationException, BadDerivationClassException
            {
        if (idef == null)
        {
            throw new DerivationException("Derivation def cannot be null");
        }

        /*
         * The object coming is no longer the derivation
         * itself. It is the derivation definition. The derivation itself is now
         * created from the definition before adding it to the map.
         */
        if (idef instanceof IBitUnpackChannelDerivation) {
            final IBitUnpackChannelDerivation def = (IBitUnpackChannelDerivation)idef;
            final String parent = def.getParent();

            Set<BitUnpackDerivation> defs = _parentToBitDerivations.get(parent);

            if (defs == null)
            {
                defs = new HashSet<BitUnpackDerivation>();

                _parentToBitDerivations.put(parent, defs);
            }

            defs.add(new BitUnpackDerivation(def));
        } else {
            final IAlgorithmicChannelDerivation def = (IAlgorithmicChannelDerivation)idef;
            addDerivation(new AlgorithmicDerivation(def, loader));
        }
    }


    /**
     * Adds an algorithmic derivation to the map. If the map already contains a
     * derivation with that id, then the old definition will be updated to add
     * the parents and children of the new derivation.
     * 
     * @param derivation
     *            the algorithmic derivation to add
     * 
     * @throws DerivationException
     *             if the input definition or its ID is null, or the algorithm
     *             name is null.
     * @throws BadDerivationClassException if the derivation class cannot be loaded            
     * 
     * 5/2/14. Changed name of argument. Throughout,
     *          changes to go through the definition object to get definition
     *          attributes such ad ID and name.
     */
    private void addDerivation(final AlgorithmicDerivation derivation)
            throws DerivationException, BadDerivationClassException
            {
        if (derivation == null)
        {
            throw new DerivationException("Derivation cannot be null");
        }

        final String id = derivation.getDefinition().getId();

        if (id == null)
        {
            throw new DerivationException("Derivation id cannot be null");
        }
        
        /*
         * Derivation class is no longer loaded
         * when the dictionary is. It must be loaded here.  
         */
        derivation.loadAlgorithmClass();

        final String algo = derivation.getDefinition().getAlgorithmName();

        if (algo == null)
        {
            throw new DerivationException("Derivation algorithm cannot " +
                    "be null");
        }

        final Pair<String, String>  pair          = new Pair<String,String>(id, algo);
        final AlgorithmicDerivation oldDefinition = _idToAlgoDerivation.get(
                pair);

        // If we have an id but we don't have a definition that matches it, it
        // is a new derivation

        if (oldDefinition == null)
        {
            for (final String parent : derivation.getDefinition().getParents())
            {
                Set<AlgorithmicDerivation> defs =
                        _parentToAlgoDerivations.get(parent);

                if (defs == null)
                {
                    defs = new HashSet<AlgorithmicDerivation>(64);

                    _parentToAlgoDerivations.put(parent, defs);
                }

                defs.add(derivation);
            }

            _idToAlgoDerivation.put(pair, derivation);

            return;
        }

        // Derivation exists

        // Combine the new with the old

        for (final String parent : derivation.getDefinition().getParents())
        {
            Set<AlgorithmicDerivation> defs = _parentToAlgoDerivations.get(
                    parent);

            // If parent has no derivations, create a new derivation set for
            // it and add the original definition

            if (defs == null)
            {
                defs = new HashSet<AlgorithmicDerivation>();

                _parentToAlgoDerivations.put(parent, defs);
            }

            // Attach the old definition in any case
            defs.add(oldDefinition);
        }

        // Now combine the child sets of the old and new derivations

        oldDefinition.getDefinition().addChildren(derivation.getDefinition().getChildren());
        oldDefinition.getDefinition().addParents(derivation.getDefinition().getParents());
    }

    /**
     * Gets the set of defined bit derivations for the given parent channel id.
     *
     * @param parent the parent channel id to look for
     *
     * @return a set of bit derivations, or null if none defined for
     *         the given parent
     */
    public Set<BitUnpackDerivation> getBitDerivationsForParent(final String parent)
    {
        final Set<BitUnpackDerivation> set =
                _parentToBitDerivations.get(parent);

        return ((set != null) ? Collections.unmodifiableSet(set) : null);
    }


    /**
     * Gets the set of defined algorithmic derivations for the given parent channel id.
     *
     * @param parent the parent channel id to look for
     *
     * @return a set of algorithmic derivations, or null if none defined for
     *         the given parent
     */
    public Set<AlgorithmicDerivation> getAlgoDerivationsForParent(final String parent)
    {
        final Set<AlgorithmicDerivation> set =
                _parentToAlgoDerivations.get(parent);

        return ((set != null) ? Collections.unmodifiableSet(set) : null);
    }


    /**
     * Gets all defined algorithmic derivations.
     *
     * @return algorithmic derivations
     */
    public Set<AlgorithmicDerivation> getAlgorithmicDerivations()
    {
        return Collections.unmodifiableSet(
                new HashSet<AlgorithmicDerivation>(_idToAlgoDerivation.values()));
    }

    /**
     * Gets the defined algorithmic derivations, given a list of channel values that are
     * potential parents.
     *
     * @param parents list of parent channel values
     * @return algorithmic derivations
     */
    public Set<AlgorithmicDerivation> getAlgorithmicDerivations(final List<IServiceChannelValue> parents) 
    {
        final Set<AlgorithmicDerivation> result = new HashSet<AlgorithmicDerivation>(5);

        for (final IServiceChannelValue parentVal : parents) 
        {
            final Set<AlgorithmicDerivation> set =
                    _parentToAlgoDerivations.get(parentVal.getChanId());
            if (set != null) 
            {
                result.addAll(set);
            }
        }	
        return Collections.unmodifiableSet(result);
    }


    /**
     * Gets a derivation object by id.
     *
     * @param id   the derivation id
     * @param algo the algorithm name
     *
     * @return the AlgorithmicDerivation with the given id and algorithm,
     *         or null if none found
     */
    public AlgorithmicDerivation getDerivationById(final String id,
            final String algo)
    {
        return _idToAlgoDerivation.get(new Pair<String,String>(id, algo));
    }

    /**
     * Get the Bit Unpack derivation for the given child channel
     * @param id the child channel ID
     * @return the BitUnpackDerivation that produces the child, or null if none defined
     */
    public BitUnpackDerivation getBitUnpackDerivationForChild(final String id) {
        final Set<String> derivationIds = _parentToBitDerivations.keySet();
        for (final String parent: derivationIds) {
            final Set<BitUnpackDerivation> derivations = _parentToBitDerivations.get(parent);
            for (final BitUnpackDerivation derivation : derivations) {
                /* Go through definition object to get child. */
                if (derivation.getDefinition().getChild().equals(id)) {
                    return derivation;
                }
            }
        }
        return null;    	
    }

    /**
     * Get the algorithm derivation for the given child channel.
     * 
     * @param id the child channel ID
     * @return the AlgorithmicDerivation that produces the child, or null if none defined
     */
    public AlgorithmicDerivation getAlgorithmicDerivationForChild(final String id) {
        final AlgorithmicDerivation algo = _childToAlgoDerivation.get(id);
        if (algo != null) {
            return algo;
        }
        final Set<String> derivationIds = _parentToAlgoDerivations.keySet();
        for (final String parent: derivationIds) {
            final Set<AlgorithmicDerivation> derivations = _parentToAlgoDerivations.get(parent);
            for (final AlgorithmicDerivation derivation : derivations) {
                /*  Go through definition object to get children. */
                final List<String> children = derivation.getDefinition().getChildren();
                for (final String childId : children) {
                    if (childId.equals(id)) {
                        _childToAlgoDerivation.put(id, derivation);
                        return derivation;
                    }
                }
            }
        }
        return null;    	
    }
}
