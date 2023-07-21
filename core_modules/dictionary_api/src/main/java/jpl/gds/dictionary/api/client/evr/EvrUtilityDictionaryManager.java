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

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.evr.EvrDefinitionType;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDictionary;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * EvrDictionaryClientUtility is a management class for EVR dictionaries. Its
 * intended use is to store in memory all of the EVR Definitions parsed from EVR
 * dictionary files, including both FSW and SSE EVRs. An EVR Definition is
 * looked up by its EVR ID or name, but it should be noted that only the ID is
 * guaranteed to be unique. This class can also provides lists of EVR levels and
 * categories.
 * 
 * 
 *
 *
 */
public final class EvrUtilityDictionaryManager implements IEvrUtilityDictionaryManager {

	/**
	 * Main FSW EVR container. Maps EVR IDs to their FSW definition objects.
	 */
	private final Map<Long, IEvrDefinition> fswEntries;

	/**
	 * Main SSE EVR container. Maps EVR IDs to their SSE definition objects.
	 */
	private final Map<Long, IEvrDefinition> sseEntries;

	/**
	 * FSW categories container. Stores unique category names for FSW EVRs.
	 */
	private final Map<String, Set<String>> fswCategories;

	/**
	 * SSE categories container. Stores unique category names for SSE EVRs.
	 */
	private final Map<String, Set<String>> sseCategories;

	/**
	 * FSW levels container. Stores unique levels for FSW EVRs.
	 */
	private final Set<String> fswLevels;

	/**
	 * SSE levels container. Stores unique levels for SSE EVRs.
	 */
	private final Set<String> sseLevels;

	/**
	 * Current dictionary configuration.
	 */
	private DictionaryProperties dictConfig;
	private final Tracer                          dictTracer;

	/** 
	 * FSW load lock.
	 */
	private final AtomicBoolean fswLoaded = new AtomicBoolean(false);
	/**
	 * SSE load lock.
	 */
	private final AtomicBoolean sseLoaded = new AtomicBoolean(false);


	private final IDictionaryCache cache;
	private final FlightDictionaryLoadingStrategy fsw;
	private final SseDictionaryLoadingStrategy sse;

	/**
	 * Constructor. Dictionary properties and the dictionary factory will
	 * be taken from the supplied application context.
	 * 
	 * @param appContext the current application context.
	 * 
	 */
	public EvrUtilityDictionaryManager(final ApplicationContext appContext) {

		this(appContext.getBean(IDictionaryCache.class), appContext.getBean(DictionaryProperties.class),
				appContext.getBean(FlightDictionaryLoadingStrategy.class),
				appContext.getBean(SseDictionaryLoadingStrategy.class),
				TraceManager.getTracer(appContext, Loggers.DICTIONARY));
	}

	/**
	 *
	 * @param dictionaryCache
	 * @param dictConfig
	 * @param flightDictionaryLoadingStrategy
	 * @param sseDictionaryLoadingStrategy
	 * @param tracer
	 */
	EvrUtilityDictionaryManager(IDictionaryCache dictionaryCache, DictionaryProperties dictConfig,
									   FlightDictionaryLoadingStrategy flightDictionaryLoadingStrategy,
									   SseDictionaryLoadingStrategy sseDictionaryLoadingStrategy,
									   Tracer tracer) {

		this.cache = dictionaryCache;
		this.dictConfig = dictConfig;
		this.dictTracer = tracer;

		this.fsw = flightDictionaryLoadingStrategy;
		this.sse = sseDictionaryLoadingStrategy;

		fswEntries = new HashMap<>();
		sseEntries = new HashMap<>();
		fswCategories = new HashMap<>();
		sseCategories = new HashMap<>();
		fswLevels = new TreeSet<>();
		sseLevels = new TreeSet<>();

	}

	@Override
	public synchronized void loadAll() throws DictionaryException {
		loadFsw();
		loadSse();
	}

	@Override
	public synchronized void loadAll(final DictionaryProperties config) throws DictionaryException {
		this.dictConfig = config;
		loadAll();
	}

	@Override
	public synchronized void loadFsw() throws DictionaryException {
		if (fsw.isEvrEnabled() && !fswLoaded.getAndSet(true)) {
			final IEvrDictionary evrDict = cache.getEvrDictionary(dictConfig, false);
			updateFromParsedDictionary(evrDict);
		}
	}

	@Override
	public synchronized void loadSse() throws DictionaryException {
		if (sse.isEvrEnabled() && !sseLoaded.getAndSet(true)) {
			final IEvrDictionary evrDict = cache.getEvrDictionary(dictConfig, true);
			updateFromParsedDictionary(evrDict);
		}
	}

	/**
	 * Adds the EVR definitions found in the given parsed dictionary instance.
	 * 
	 * @param dict
	 *            IEvrDictionary containing parsed definitions
	 */
	private void updateFromParsedDictionary(final IEvrDictionary dict) {

		// Get the parsed EVR definitions from the parser
		final List<IEvrDefinition> evrMap = dict.getEvrDefinitions();

		if (evrMap != null) {

			// Add all the new EVRs to the EVR maps
			for (final IEvrDefinition ed : evrMap) {
				addDefinition(ed);
				addCategories(ed);
				addLevel(ed);
			}
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void addDefinition(final IEvrDefinition def) {

		if (def == null) {
			throw new IllegalArgumentException("Null EVR definition input!");
		}

		switch (def.getDefinitionType()) {
		case FSW:
			if (fswEntries.get(def.getId()) != null) {
				dictTracer.warn("Existing definition of FSW EVR with ID " + def.getId()
				+ " is being overridden");
			}
			fswEntries.put(def.getId(), def);
			break;
		case SSE:
			if (fswEntries.get(def.getId()) != null) {
				dictTracer.warn("Existing definition of SSE EVR with ID " + def.getId()
				+ " is being overridden");
			}
			sseEntries.put(def.getId(), def);
			break;
		default:
			throw new IllegalStateException(
					"Attempting to add EVR with unknown definition type: "
							+ def.getDefinitionType());
		}

	}

	/**
	 * Adds the categories attached to the given EVR definition object to the
	 * internal categories tables.
	 * 
	 * @param def
	 *            the IEvrDefinition containing categories to add
	 */
	private void addCategories(final IEvrDefinition def) {

		final Categories cats = def.getCategories();
		if (cats != null) {
			for (final String catKey : cats.getKeys()) {
				Set<String> existingList = (def.getDefinitionType() == EvrDefinitionType.FSW) ? fswCategories
						.get(catKey) : sseCategories.get(catKey);

						if (existingList == null) {
							existingList = new HashSet<String>();
							if (def.getDefinitionType() == EvrDefinitionType.FSW) {
								fswCategories.put(catKey, existingList);
							} else {
								sseCategories.put(catKey, existingList);
							}
						}
						existingList.add(cats.getCategory(catKey));
			}
		}

	}

	/**
	 * Adds the level of the supplied EVR definition to the set of unique
	 * levels.
	 * 
	 * @param def
	 *            the evr definition
	 * 
	 */
	private void addLevel(final IEvrDefinition def) {

		if (def.getDefinitionType() == EvrDefinitionType.FSW) {
			fswLevels.add(def.getLevel());
		} else {
			sseLevels.add(def.getLevel());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IEvrDefinition getFswDefinition(final long id) {
		return this.fswEntries.get(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IEvrDefinition getSseDefinition(final long id) {
		return this.sseEntries.get(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IEvrDefinition getFswDefinition(final String name) {
		for (final IEvrDefinition evr : fswEntries.values()) {
			if (evr.getName() != null && evr.getName().equalsIgnoreCase(name)) {
				return evr;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IEvrDefinition getSseDefinition(final String name) {
		for (final IEvrDefinition evr : sseEntries.values()) {
			if (evr.getName() != null && evr.getName().equalsIgnoreCase(name)) {
				return evr;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Long, IEvrDefinition> getFswEvrDefinitionMap() {
		return new HashMap<Long, IEvrDefinition>(fswEntries);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Long, IEvrDefinition> getSseEvrDefinitionMap() {
		return new HashMap<Long, IEvrDefinition>(sseEntries);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getFswCategories(final String categoryName) {
		if (fswCategories.get(categoryName) == null) {
			return new TreeSet<String>();
		} else {
			return new TreeSet<String>(fswCategories.get(categoryName));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getSseCategories(final String categoryName) {
		if (sseCategories.get(categoryName) == null) {
			return new TreeSet<String>();
		} else {
			return new TreeSet<String>(sseCategories.get(categoryName));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getAllCategories(final String categoryName) {
		final Set<String> result = new TreeSet<String>(getFswCategories(categoryName));
		result.addAll(getSseCategories(categoryName));
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getFswLevels() {
		return new TreeSet<String>(fswLevels);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getSseLevels() {
		return new TreeSet<String>(sseLevels);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getAllLevels() {
		final Set<String> result = new TreeSet<String>(getFswLevels());
		result.addAll(getSseLevels());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void clearFsw() {
		fswEntries.clear();
		fswLevels.clear();
		fswCategories.clear();
		fswLoaded.set(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void clearSse() {
		sseEntries.clear();
		sseLevels.clear();
		sseCategories.clear();
		sseLoaded.set(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void clearAll() {
		clearFsw();
		clearSse();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.evr.IEvrDefinitionProvider#getDefinitionFromEvrId(java.lang.Long)
	 */
	@Override
	public IEvrDefinition getDefinitionFromEvrId(final Long id) {
		IEvrDefinition def = this.fswEntries.get(id);
		if (def == null) {
			def = this.sseEntries.get(id);
		}
		return def;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.evr.IEvrDefinitionProvider#getEvrDefinitionMap()
	 */
	@Override
	public Map<Long, IEvrDefinition> getEvrDefinitionMap() {
		final Map<Long, IEvrDefinition> result = new HashMap<Long, IEvrDefinition>(this.fswEntries);
		result.putAll(this.sseEntries);
		return result;
	}

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.api.IDefinitionProviderLoadStatus#isLoaded()
     */
    @Override
    public boolean isLoaded() {
        return this.fswLoaded.get() || this.sseLoaded.get();
    }

}
