package jpl.gds.dictionary.api.client.channel;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDerivation;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.exception.UnloadedDictionaryException;

/**
 * Implementation that will throw when a dictionary is accessed in a way that it should not if it is not loaded.
 * Not all methods will throw because we want to support not loading some dictionaries.
 * 
 *
 */
public class UnloadedChannelDictionaryManager implements IChannelDictionaryManager {

	@Override
	public void load(IChannelDictionary dict) throws DictionaryException {
		throw new UnloadedDictionaryException(DictionaryType.CHANNEL);
	}

	@Override
	public void clear() {
		// This one does not throw so we can not load and clear others.
	}

	@Override
	public void processChannelDefinition(IChannelDefinition def) {
		throw new UnloadedDictionaryException(DictionaryType.CHANNEL);
	}

	@Override
	public IChannelDefinition getDefinition(String chanId) {
		throw new UnloadedDictionaryException(DictionaryType.CHANNEL);
	}

	@Override
	public void addDerivation(IChannelDerivation derive) {
		throw new UnloadedDictionaryException(DictionaryType.CHANNEL);
	}


	@Override
	public Set<String> getModules() {
		// This is used to aggregate, so just return empty set.
		return Collections.emptySet();
	}

	@Override
	public Set<String> getSubsystems() {
		// This is used to aggregate, so just return empty set.
		return Collections.emptySet();
	}

	@Override
	public Set<String> getOpsCategories() {
		// This is used to aggregate, so just return empty set.
		return Collections.emptySet();
	}

	@Override
	public Set<String> getChannelIds() {
		// This is used to aggregate, so just return empty set.
		return Collections.emptySet();
	}

	@Override
	public Map<String, IChannelDefinition> getChannelDefinitionMap() {
		/**
		 * Used to find by id, return empty map.
		 */
		return Collections.emptyMap();
	}

	@Override
	public Map<String, IChannelDerivation> getChannelDerivationMap() {
		/**
		 * Used to find by id, return empty map.
		 */
		return Collections.emptyMap();
	}

	@Override
	public boolean isLoaded() {
		// Always returns false.
		return false;
	}
	
	@Override
	public void wasLoaded() { }
}
