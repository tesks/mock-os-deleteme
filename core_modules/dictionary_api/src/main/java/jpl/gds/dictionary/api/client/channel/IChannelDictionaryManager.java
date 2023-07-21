package jpl.gds.dictionary.api.client.channel;

import java.util.Map;
import java.util.Set;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDerivation;
import jpl.gds.dictionary.api.channel.IChannelDictionary;

public interface IChannelDictionaryManager {

	void load(IChannelDictionary dict) throws DictionaryException;

	void clear();

	void processChannelDefinition(IChannelDefinition def);

	IChannelDefinition getDefinition(String chanId);

	Set<String> getModules();

	Set<String> getSubsystems();

	Set<String> getOpsCategories();

	Set<String> getChannelIds();

	Map<String, IChannelDefinition> getChannelDefinitionMap();

	Map<String, IChannelDerivation> getChannelDerivationMap();

	void addDerivation(IChannelDerivation derive);
	
	/**
	 * @return true if the dictionary was loaded.
	 */
	boolean isLoaded();
	
	
	/**
	 * sets the is loaded flag indicating the dictionary was loaded.
	 * 
	 */
	void wasLoaded(); 
	

}