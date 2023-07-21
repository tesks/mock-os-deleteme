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
package jpl.gds.dictionary.api.client.channel;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDerivation;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A utility class for managing multiple channel dictionaries
 * 
 * The managers map gets loaded initially with unloaded managers that will
 * throw if we attempt to use them without first loading them.  
 * 
 * Found a major performance issue with how we were accessing the channel definition
 * map.  Now it is aggregating at load time and removing them when clearing.
 * 
 *
 * @since R8
 *
 */
public class ChannelDictionaryClientUtility implements IChannelUtilityDictionaryManager {

	private static String SSE_TAG = "sse:";
	private static String FSW_TAG = "fsw:";

	private DictionaryProperties dictConfig;
	private final Map<String, IChannelDictionaryManager> dictionaryManagers = new HashMap<String, IChannelDictionaryManager>();

	private final IDictionaryCache cache;
	private final FlightDictionaryLoadingStrategy fsw;
	private final SseDictionaryLoadingStrategy sse;

	private final Map<String, IChannelDefinition> channelDefinitionMap = new HashMap<>();
	private final AtomicBoolean isLoaded = new AtomicBoolean();


	/**
	 * Constructor. Uses the dictionary properties and dictionary factory from the supplied
	 * application context.
	 * 
	 * @param appContext
	 *            the current application context
	 */
	public ChannelDictionaryClientUtility(final ApplicationContext appContext) {
		this(appContext, appContext.getBean(DictionaryProperties.class));
	}

	/**
	 * Constructor. Uses the dictionary properties and dictionary factory from the supplied
	 * application context.
	 * 
	 * @param appContext
	 *            the current application context
	 */
	ChannelDictionaryClientUtility(final ApplicationContext appContext, final DictionaryProperties dictConfig) {

		this(dictConfig, appContext.getBean(IDictionaryCache.class),
				appContext.getBean(FlightDictionaryLoadingStrategy.class),
				appContext.getBean(SseDictionaryLoadingStrategy.class),
				new UnloadedChannelDictionaryManager());
	}

	/**
	 * Constructor
	 * @param dictConfig dictionary properties config
	 * @param dictionaryCache dictionay cache
	 * @param flightDictionaryLoadingStrategy fsw loading strategy
	 * @param sseDictionaryLoadingStrategy sse loading strategy
	 * @param unloadedChannelDictionaryManager used for dictionary managers
	 */
	ChannelDictionaryClientUtility(final DictionaryProperties dictConfig,
										  IDictionaryCache dictionaryCache,
										  FlightDictionaryLoadingStrategy flightDictionaryLoadingStrategy,
										  SseDictionaryLoadingStrategy sseDictionaryLoadingStrategy,
										  final UnloadedChannelDictionaryManager unloadedChannelDictionaryManager) {

		this.dictConfig = dictConfig;
		this.cache = dictionaryCache;
		this.fsw = flightDictionaryLoadingStrategy;
		this.sse = sseDictionaryLoadingStrategy;

		dictionaryManagers.put(FSW_TAG + DictionaryType.CHANNEL.toString(), unloadedChannelDictionaryManager);
		dictionaryManagers.put(FSW_TAG + DictionaryType.HEADER.toString(), unloadedChannelDictionaryManager);
		dictionaryManagers.put(SSE_TAG + DictionaryType.CHANNEL.toString(), unloadedChannelDictionaryManager);
		dictionaryManagers.put(SSE_TAG + DictionaryType.HEADER.toString(), unloadedChannelDictionaryManager);
		dictionaryManagers.put(DictionaryType.MONITOR.toString(), unloadedChannelDictionaryManager);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.IDefinitionProviderLoadStatus#isLoaded()
	 */
	@Override
	public boolean isLoaded() {
		return isLoaded.get();
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#loadAll(boolean, boolean)
	 */
	@Override
	public synchronized void loadAll(final boolean requireMisc) throws DictionaryException {
		isLoaded.set(true);

		loadFsw(true);
		loadFswHeader(requireMisc);
		loadMonitor(requireMisc);
		loadSse(true);
		loadSseHeader(requireMisc);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#loadAll(jpl.gds.dictionary.api.config.DictionaryConfiguration, boolean, boolean)
	 */
	@Override
	public synchronized void loadAll(final DictionaryProperties config, final boolean requireMisc) throws DictionaryException {
		this.dictConfig = config;
		loadAll(requireMisc);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager#resetAllEmpty()
	 */
	@Override
	public void resetAllEmpty() {
		checkAndSetManager(FSW_TAG + DictionaryType.CHANNEL.toString(), DictionaryType.CHANNEL);
		checkAndSetManager(FSW_TAG + DictionaryType.HEADER.toString(), DictionaryType.HEADER);
		checkAndSetManager(SSE_TAG + DictionaryType.CHANNEL.toString(), DictionaryType.CHANNEL);
		checkAndSetManager(SSE_TAG + DictionaryType.HEADER.toString(), DictionaryType.HEADER);
		checkAndSetManager(DictionaryType.MONITOR.toString(), DictionaryType.MONITOR);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#clearAll()
	 */
	@Override
	public synchronized void clearAll() {
		dictionaryManagers.forEach((k,d)->{ d.clear();});

		channelDefinitionMap.clear();
		isLoaded.set(false);
	}

	/**
	 * Removes all the definitions from the aggregated result map for all the values in the manager.
	 * @param d the manager to remove
	 */
	private synchronized void removeFromDefinitonMap(IChannelDictionaryManager d) {
		d.getChannelIds().forEach(id -> channelDefinitionMap.remove(id));
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#clearAllFsw()
	 */
	@Override
	public synchronized void clearAllFsw() {
		dictionaryManagers.forEach((k,d)->
		{ 
			if (k.startsWith(FSW_TAG)) {
				// Remove all the definitions from the map.
				removeFromDefinitonMap(d);
				d.clear();
			}
		});
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#clearAllSse()
	 */
	@Override
	public synchronized void clearAllSse() {
		dictionaryManagers.forEach((k,d)->
		{ 
			if (k.startsWith(SSE_TAG)) {
				removeFromDefinitonMap(d);
				d.clear();
			}
		});
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#clearMonitor()
	 */
	@Override
	public synchronized void clearMonitor() {
		removeFromDefinitonMap(dictionaryManagers.get(DictionaryType.MONITOR.toString()));
		dictionaryManagers.get(DictionaryType.MONITOR.toString()).clear();
	}

	/**
	 * Checks if the value keyed by tag in dictionaryManagers is still the unloaded manager and creates 
	 * a new one with type type and adds it to the map.
	 * 
	 * @param tag the key for the manager
	 * @param type the type
	 */
	private void checkAndSetManager(String tag, DictionaryType type) {
		if (dictionaryManagers.get(tag) instanceof UnloadedChannelDictionaryManager) {
			dictionaryManagers.put(tag, new ChannelDictionaryManager(type));
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#loadFsw(boolean)
	 */
	@Override
	public synchronized void loadFsw(boolean require) throws DictionaryException {
		if (fsw.isChannelEnabled()) {
			checkAndSetManager(FSW_TAG + DictionaryType.CHANNEL.toString(), DictionaryType.CHANNEL);

			try {
				if (!dictionaryManagers.get(FSW_TAG + DictionaryType.CHANNEL.toString()).isLoaded()) {
					IChannelDictionaryManager m = dictionaryManagers.get(FSW_TAG + DictionaryType.CHANNEL.toString());
					m.load(cache.getFlightChannelDictionary(dictConfig));
					channelDefinitionMap.putAll(m.getChannelDefinitionMap());
				}
			} catch (Exception e) {
				if (require) {
					throw e;
				} else {
					// Not required, do nothing.
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#loadFswHeader(boolean)
	 */
	@Override
	public synchronized void loadFswHeader(boolean require) throws DictionaryException {
		if (fsw.isHeaderEnabled()) {
			checkAndSetManager(FSW_TAG + DictionaryType.HEADER.toString(), DictionaryType.HEADER);

			try {
				if (!dictionaryManagers.get(FSW_TAG + DictionaryType.HEADER.toString()).isLoaded()) {
					IChannelDictionaryManager m = dictionaryManagers.get(FSW_TAG + DictionaryType.HEADER.toString());
					m.load(cache.getHeaderChannelDictionary(dictConfig));
					channelDefinitionMap.putAll(m.getChannelDefinitionMap());
				}
			} catch (Exception e) {
				if (require) {
					throw e;
				} else {
					// Not required, do nothing.
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#loadSse(boolean)
	 */
	@Override
	public synchronized void loadSse(boolean require) throws DictionaryException {
		if (sse.isChannelEnabled()) {
			checkAndSetManager(SSE_TAG + DictionaryType.CHANNEL.toString(), DictionaryType.CHANNEL);
			try {
				if (!dictionaryManagers.get(SSE_TAG + DictionaryType.CHANNEL.toString()).isLoaded()) {
					IChannelDictionaryManager m = dictionaryManagers.get(SSE_TAG + DictionaryType.CHANNEL.toString());
					m.load(cache.getSseChannelDictionary(dictConfig));
					channelDefinitionMap.putAll(m.getChannelDefinitionMap());
				}
			} catch (Exception e) {
				if (require) {
					throw e;
				} else {
					// Not required, do nothing.
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#loadSseHeader(boolean)
	 */
	@Override
	public synchronized void loadSseHeader(boolean require) throws DictionaryException {
		if (sse.isHeaderEnabled()) {
			checkAndSetManager(SSE_TAG + DictionaryType.HEADER.toString(), DictionaryType.CHANNEL);
			try {
				if (!dictionaryManagers.get(SSE_TAG + DictionaryType.HEADER.toString()).isLoaded()) {
					IChannelDictionaryManager m = dictionaryManagers.get(SSE_TAG + DictionaryType.HEADER.toString());
					m.load(cache.getSseHeaderChannelDictionary(dictConfig));
					channelDefinitionMap.putAll(m.getChannelDefinitionMap());
				}
			} catch (Exception e) {
				if (require) {
					throw e;
				} else {
					// Not required, do nothing.
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#loadMonitor(boolean)
	 */
	@Override
	public synchronized void loadMonitor(boolean require) throws DictionaryException {
		if (fsw.isMonitorEnabled()) {
			checkAndSetManager(DictionaryType.MONITOR.toString(), DictionaryType.CHANNEL);
			try {
				if (!dictionaryManagers.get(DictionaryType.MONITOR.toString()).isLoaded()) {
					IChannelDictionaryManager m = dictionaryManagers.get(DictionaryType.MONITOR.toString());
					m.load(cache.getMonitorDictionary(dictConfig));
					channelDefinitionMap.putAll(m.getChannelDefinitionMap());
				}
			} catch (Exception e) {
				if (require) {
					throw e;
				} else {
					// Not required, do nothing.
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#addFswDefinition(jpl.gds.dictionary.api.channel.IChannelDefinition)
	 */
	@Override
	public void addFswDefinition(final IChannelDefinition toAdd) {

		switch(toAdd.getDefinitionType()) {
		case FSW:
			dictionaryManagers.get(FSW_TAG + DictionaryType.CHANNEL.toString()).processChannelDefinition(toAdd);
			channelDefinitionMap.put(toAdd.getId(), toAdd);
			break;
		case H:
			dictionaryManagers.get(FSW_TAG + DictionaryType.HEADER.toString()).processChannelDefinition(toAdd);
			channelDefinitionMap.put(toAdd.getId(), toAdd);
			break;
		case M:
		case SSE:
		default:
			throw new IllegalArgumentException("Attempted to add a non-fsw channel type to the fsw channel table");

		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#addSseDefinition(jpl.gds.dictionary.api.channel.IChannelDefinition)
	 */
	@Override
	public void addSseDefinition(final IChannelDefinition toAdd) {

		switch(toAdd.getDefinitionType()) {
		case SSE:
			dictionaryManagers.get(FSW_TAG + DictionaryType.CHANNEL.toString()).processChannelDefinition(toAdd);
			channelDefinitionMap.put(toAdd.getId(), toAdd);
			break;
		case H:
			dictionaryManagers.get(FSW_TAG + DictionaryType.HEADER.toString()).processChannelDefinition(toAdd);
			channelDefinitionMap.put(toAdd.getId(), toAdd);
			break;
		case FSW:
		case M:
		default:
			throw new IllegalArgumentException("Attempted to add a non-sse channel type to the sse channel table");

		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#addMonitorDefinition(jpl.gds.dictionary.api.channel.IChannelDefinition)
	 */
	@Override
	public void addMonitorDefinition(final IChannelDefinition toAdd) {
		if (toAdd.getDefinitionType() != ChannelDefinitionType.M) {
			throw new IllegalArgumentException("Attempted to add a non-monitor channel type to the monitor channel table");
		}
		dictionaryManagers.get(DictionaryType.MONITOR.toString()).processChannelDefinition(toAdd);
		channelDefinitionMap.put(toAdd.getId(), toAdd);
	}


	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getFswDefinitionFromChannelId(java.lang.String)
	 */
	@Override
	public IChannelDefinition getFswDefinitionFromChannelId(final String channelId) {
		/**
		 * Must support the case where fsw is disabled and header is enabled, or vice versa.
		 */
		IChannelDefinition result = fsw.isChannelEnabled() ? dictionaryManagers.get(FSW_TAG + DictionaryType.CHANNEL.toString()).getDefinition(channelId) : null;
		if (result == null && fsw.isHeaderEnabled()) {
			result = dictionaryManagers.get(FSW_TAG + DictionaryType.HEADER.toString()).getDefinition(channelId);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getSseDefinitionFromChannelId(java.lang.String)
	 */
	@Override
	public IChannelDefinition getSseDefinitionFromChannelId(final String channelId) {
		/**
		 * Must support the case where sse is disabled and sse header is enabled, or vice versa.
		 */
		IChannelDefinition result = sse.isChannelEnabled() ? dictionaryManagers.get(SSE_TAG + DictionaryType.CHANNEL.toString()).getDefinition(channelId) : null;
		if (result == null && sse.isHeaderEnabled()) {
			result = dictionaryManagers.get(SSE_TAG + DictionaryType.HEADER.toString()).getDefinition(channelId);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getMonitorDefinitionFromChannelId(java.lang.String)
	 */
	@Override
	public IChannelDefinition getMonitorDefinitionFromChannelId(final String channelId) {
		return fsw.isMonitorEnabled() ? dictionaryManagers.get(DictionaryType.MONITOR.toString()).getDefinition(channelId) : null;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getDefinitionFromChannelId(java.lang.String)
	 */
	@Override
	public IChannelDefinition getDefinitionFromChannelId(final String channelId) {
		IChannelDefinition def = getFswDefinitionFromChannelId(channelId);
		if (def == null) {
			def = getSseDefinitionFromChannelId(channelId);
		}
		if (def == null) {
			def = getMonitorDefinitionFromChannelId(channelId);
		}
		return def;

	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getModules()
	 */
	@Override
	public synchronized List<String> getModules() {

		final SortedSet<String> resultSet = new TreeSet<String>();
		dictionaryManagers.forEach((k,d)->{ resultSet.addAll(d.getModules());});
		return new LinkedList<String>(resultSet);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getSubsystems()
	 */
	@Override
	public synchronized List<String> getSubsystems() {
		final SortedSet<String> resultSet = new TreeSet<String>();
		dictionaryManagers.forEach((k,d)->{ resultSet.addAll(d.getSubsystems()); });
		return new LinkedList<String>(resultSet);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getOpsCategories()
	 */
	@Override
	public synchronized List<String> getOpsCategories() {
		final SortedSet<String> resultSet = new TreeSet<String>();
		dictionaryManagers.forEach((k,d)->{resultSet.addAll(d.getOpsCategories());});
		return new LinkedList<String>(resultSet);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getChanIds()
	 */
	@Override
	public synchronized SortedSet<String> getChanIds() {
		final SortedSet<String> resultSet = new TreeSet<String>();
		dictionaryManagers.forEach((k,d)-> { resultSet.addAll(d.getChannelIds());});
		return resultSet;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getDerivationForChannelId(java.lang.String)
	 */
	@Override
	public IChannelDerivation getDerivationForChannelId(final String chanId) {
		for (final IChannelDictionaryManager mgr : dictionaryManagers.values()) {
			if (mgr.getChannelDerivationMap().containsKey(chanId)) {
				return mgr.getChannelDerivationMap().get(chanId);
			}
		}       
		return null;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getChannelDefinitionMap()
	 */
	@Override
	public Map<String, IChannelDefinition> getChannelDefinitionMap() {
		return channelDefinitionMap;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#getChannelDerivations()
	 */
	@Override
	public synchronized List<IChannelDerivation> getChannelDerivations() {
		final List<IChannelDerivation> result = new LinkedList<IChannelDerivation>();
		dictionaryManagers.forEach((k,d)-> { result.addAll(d.getChannelDerivationMap().values());});
		return result;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#addFswDerivation(jpl.gds.dictionary.api.channel.IChannelDerivation)
	 */
	@Override
	public synchronized void addFswDerivation(final IChannelDerivation derive) {
		addFswDerivation(derive, false);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#addFswDerivation(jpl.gds.dictionary.api.channel.IChannelDerivation, boolean)
	 */
	@Override
	public synchronized void addFswDerivation(final IChannelDerivation derive, final boolean isHeader) {
		dictionaryManagers.get(FSW_TAG + (isHeader ? DictionaryType.HEADER.toString() : DictionaryType.CHANNEL.toString())).addDerivation(derive);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#addSseDerivation(jpl.gds.dictionary.api.channel.IChannelDerivation)
	 */
	@Override
	public synchronized void addSseDerivation(final IChannelDerivation derive) {
		addSseDerivation(derive, false);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#addSseDerivation(jpl.gds.dictionary.api.channel.IChannelDerivation, boolean)
	 */
	@Override
	public synchronized void addSseDerivation(final IChannelDerivation derive, final boolean isHeader) {
		dictionaryManagers.get(SSE_TAG + (isHeader ? DictionaryType.HEADER.toString() : DictionaryType.CHANNEL.toString())).addDerivation(derive);
	}

	/* (non-Javadoc)
	 * @see jpl.gds.dictionary.api.channel.IChannelDictionaryManager#addMonitorDerivation(jpl.gds.dictionary.api.channel.IChannelDerivation)
	 */
	@Override
	public synchronized void addMonitorDerivation(final IChannelDerivation derive) {
		dictionaryManagers.get(DictionaryType.MONITOR.toString()).addDerivation(derive);
	}

}
