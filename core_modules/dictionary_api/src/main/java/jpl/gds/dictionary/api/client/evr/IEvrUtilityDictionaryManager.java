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
package jpl.gds.dictionary.api.client.evr;

import java.util.Map;
import java.util.Set;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinitionProvider;

/**
 * An interface to be implemented by EVR dictionary management utilities.
 * 
 *
 * @since R8
 */
public interface IEvrUtilityDictionaryManager extends IEvrDefinitionProvider {

    /**
     * Loads EVR dictionaries using the dictionary configuration already
     * established in the dictionary manager, including both flight and SSE if
     * so indicated.
     * 
     * @throws DictionaryException
     *             if there is a problem loading the dictionary
     */
    public void loadAll() throws DictionaryException;

    /**
     * Loads EVR dictionaries, including both flight and SSE if so indicated,
     * using the provided dictionary configuration, which will become the
     * default for this dictionary manager.
     * 
     * @param config
     *            dictionary configuration
     * @throws DictionaryException
     *             if there is a problem loading the dictionary
     */
    public void loadAll(DictionaryProperties config) throws DictionaryException;

    /**
     * Loads the flight EVR dictionary, according to the established dictionary
     * configuration.
     * 
     * @throws DictionaryException
     *             if there is a problem loading the dictionary
     */
    public void loadFsw() throws DictionaryException;

    /**
     * Loads the SSE EVR dictionary, according to the established dictionary
     * configuration.
     * 
     * @throws DictionaryException
     *             if there is a problem loading the dictionary
     */
    public void loadSse() throws DictionaryException;

    /**
     * Adds a single EVR definition to the internal tables.
     * 
     * @param def
     *            the IEvrDefinition to add
     */
    public void addDefinition(IEvrDefinition def);

    /**
     * Returns the definition of the EVR with the given FSW event ID.
     * 
     * @param id
     *            the EVR event ID to look for
     * @return <code>IEvrDefinition</code> object, or <code>null</code> if no
     *         match
     */
    public IEvrDefinition getFswDefinition(long id);

    /**
     * Returns the definition of the EVR with the given SSE event ID.
     * 
     * @param id
     *            the EVR event ID to look for
     * @return <code>IEvrDefinition</code> object, or <code>null</code> if no
     *         match
     */
    public IEvrDefinition getSseDefinition(long id);

    /**
     * Gets an EVR definition by FSW EVR name. Names are not guaranteed to be
     * unique. Will match the first EVR found with the given name. The comparison
     * is not case-sensitive.
     * 
     * @param name
     *            the name of the EVR to look for
     * @return the <code>IEvrDefinition</code> object for the specified EVR
     *         name, or <code>null</code> if not found
     */
    public IEvrDefinition getFswDefinition(String name);

    /**
     * Gets an EVR definition by SSE EVR name. Names are not guaranteed to be
     * unique. Will match the first EVR found with the given name.
     * 
     * @param name
     *            the name of the EVR to look for
     * @return the <code>IEvrDefinition</code> object for the specified EVR
     *         name, or <code>null</code> if not found
     */
    public IEvrDefinition getSseDefinition(String name);

    /**
     * Gets the map of all FSW EVR definitions by event ID.
     * 
     * @return A copy of the FSW EVR definition Map.
     */
    public Map<Long, IEvrDefinition> getFswEvrDefinitionMap();

    /**
     * Gets the map of all SSE EVR definitions by event ID.
     * 
     * @return A copy of the SSE EVR definition Map.
     */
    public Map<Long, IEvrDefinition> getSseEvrDefinitionMap();

    /**
     * Gets the list of unique category names, for a specific category key, for
     * all FSW EVR definitions.
     * 
     * @param categoryName
     *            the name of the category to fetch
     * 
     * @return Copy of the set of unique FSW category names; may be empty but
     *         never null
     *         
     * @see Categories        
     */
    public Set<String> getFswCategories(String categoryName);

    /**
     * Gets the list of unique category names, for a specific category key, for
     * all SSE EVR definitions.
     * 
     * @param categoryName
     *            the name of the category to fetch
     * 
     * @return Copy of the set of unique SSE category names; may be empty but
     *         never null
     *         
     * @see Categories 
     */
    public Set<String> getSseCategories(String categoryName);

    /**
     * Gets the list of unique category names, for a specific category key,
     * combined for all FSW and SSE EVR definitions.
     * 
     * @param categoryName
     *            the name of the category to fetch
     * 
     * @return Copy of the set of unique FSW/SSE category names; may be empty
     *         but never null
     *         
     * @see Categories 
     */
    public Set<String> getAllCategories(String categoryName);

    /**
     * Gets the list of unique EVR levels for all FSW EVR definitions.
     * 
     * @return Copy of the set of unique FSW levels; may be empty but never null
     */
    public Set<String> getFswLevels();

    /**
     * Gets the list of unique EVR levels for all SSE EVR definitions.
     * 
     * @return Copy of the set of unique SSE levels; may be empty but never null
     */
    public Set<String> getSseLevels();

    /**
     * Gets the list of unique EVR levels, combined for all FSW and SSE EVR
     * definitions.
     * 
     * @return Copy of the set of unique FSW and SSE levels; may be empty but
     *         never null
     */
    public Set<String> getAllLevels();

    /**
     * Clears the internal tables of all FSW EVR information.
     */
    public void clearFsw();

    /**
     * Clears the internal tables of all SSE EVR information.
     */
    public void clearSse();

    /**
     * Clears all EVR information for both FSW and SSE.
     */
    public void clearAll();

}