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

import java.util.List;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.channel.IChannelDerivation;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * An interface to be implemented by classes that manage multiple channel 
 * dictionaries.
 * 
 *
 * @since R8
 */
public interface IChannelUtilityDictionaryManager extends IChannelDefinitionProvider {

	/**
	 * Loads all project channel dictionaries using the DictionaryConfiguration
	 * already established when the manager was created. Once loaded, another
	 * invocation will not reload them unless one of the clear methods is used
	 * to clear one or more of the loaded dictionaries.
	 * 
	 * @param requireMisc
	 *            true if miscellaneous (header and monitor channel
	 *            dictionaries) are required. An attempt will be made to load
	 *            them, regardless, but if this flag is false an error will not
	 *            be generated if they are not found
	 * @throws DictionaryException
	 *             of there is a problem loading the dictionaries
	 */
	public abstract void loadAll(boolean requireMisc)
			throws DictionaryException;

	/**
	 * Loads all project channel dictionaries using the supplied dictionary
	 * configuration. Also causes the default dictionary configuration in the
	 * manager to be set to the supplied one. Once loaded, another invocation
	 * will not reload them unless one of the clear methods is used to clear one
	 * or more of the loaded dictionaries.
	 * 
	 * @param config
	 *            the DictionaryConfiguration to use, will become the new default
	 * @param requireMisc
	 *            true if miscellaneous (header and monitor channel
	 *            dictionaries) are required. An attempt will be made to load
	 *            them, regardless, but if this flag is false an error will not
	 *            be generated if they are not found
	 * @throws DictionaryException
	 *             of there is a problem loading the dictionaries
	 */
	public abstract void loadAll(DictionaryProperties config, boolean requireMisc) throws DictionaryException;

	/**
	 * Clear all content from loaded dictionaries and resets for another
	 * load.
	 */
	public abstract void clearAll();

	/**
	 * Clear all content from loaded flight dictionaries and resets for another
	 * load.
	 */
	public abstract void clearAllFsw();

	/**
	 * Clear all content from loaded SSE dictionaries and resets for another
	 * load.
	 */
	public abstract void clearAllSse();

	/**
	 * Clear all content from the loaded monitor dictionary and resets for another
	 * load.
	 */
	public abstract void clearMonitor();

	/**
	 * Loads all FSW dictionaries.  They will all be considered required
	 * unless the require flag is set to false. If required, loading issues
	 * will cause this method to throw. Once loaded, another
	 * invocation will not reload them unless one of the clear methods is used
	 * to clear one or more of the loaded dictionaries.
	 * 
	 * @param require true means any load issues will result in an exception, false
	 *        means they will be ignored
	 * @throws DictionaryException if there are problems loading the dictionaries
	 */
	public abstract void loadFsw(boolean require) throws DictionaryException;

	/**
	 * Loads the FSW header channel dictionary.  It will all be considered required
	 * unless the require flag is set to false. If required, loading issues
	 * will cause this method to throw. Once loaded, another
	 * invocation will not reload it unless one of the clear methods is used
	 * to clear the loaded dictionary.
	 * 
	 * @param require true means any load issues will result in an exception, false
	 *        means they will be ignored
	 * @throws DictionaryException if there are problems loading the dictionaries
	 */
	public abstract void loadFswHeader(boolean require)
			throws DictionaryException;

	/**
	 * Loads all SSE dictionaries.  They will all be considered required
	 * unless the require flag is set to false. If required, loading issues
	 * will cause this method to throw. Once loaded, another
	 * invocation will not reload them unless one of the clear methods is used
	 * to clear one or more of the loaded dictionaries.
	 * 
	 * @param require true means any load issues will result in an exception, false
	 *        means they will be ignored
	 * @throws DictionaryException if there are problems loading the dictionaries
	 */
	public abstract void loadSse(boolean require) throws DictionaryException;

	/**
	 * Loads the SSE header channel dictionary.  It will be considered required
	 * unless the require flag is set to false. If required, loading issues
	 * will cause this method to throw. Once loaded, another
	 * invocation will not reload it unless one of the clear methods is used
	 * to clear the loaded dictionary.
	 * 
	 * @param require true means any load issues will result in an exception, false
	 *        means they will be ignored
	 * @throws DictionaryException if there are problems loading the dictionaries
	 */
	public abstract void loadSseHeader(boolean require)
			throws DictionaryException;

	/**
	 * Loads the station monitor channel dictionary.  It will be considered required
	 * unless the require flag is set to false. If required, loading issues
	 * will cause this method to throw. Once loaded, another
	 * invocation will not reload it unless one of the clear methods is used
	 * to clear the loaded dictionary.
	 * 
	 * @param require true means any load issues will result in an exception, false
	 *        means they will be ignored
	 * @throws DictionaryException if there are problems loading the dictionaries
	 */
	public abstract void loadMonitor(boolean require)
			throws DictionaryException;

	/**
	 * Manually adds a channel definition for a flight software channel.
	 * Should be used only by unit tests. Will overwrite any existing definition
	 * for the channel.
	 * 
	 * @param toAdd the definition to add
	 */
	public abstract void addFswDefinition(IChannelDefinition toAdd);

	/**
	 * Manually adds a channel definition for an SSE channel.
	 * Should be used only by unit tests. Will overwrite any existing definition
	 * for the channel.
	 * 
	 * @param toAdd the definition to add
	 */
	public abstract void addSseDefinition(IChannelDefinition toAdd);

	/**
	 * Manually adds a channel definition for a station monitor channel.
	 * Should be used only by unit tests. Will overwrite any existing definition
	 * for the channel.
	 * 
	 * @param toAdd the definition to add
	 */
	public abstract void addMonitorDefinition(IChannelDefinition toAdd);

	/**
	 * Fetches the definitions of a FSW channel.
	 * 
	 * @param channelId the channel ID to look for
	 * @return the channel definition object, or null if not found
	 */
	public abstract IChannelDefinition getFswDefinitionFromChannelId(
			String channelId);

	/**
	 * Fetches the definitions of an SSE channel.
	 * 
	 * @param channelId the channel ID to look for
	 * @return the channel definition object, or null if not found
	 */
	public abstract IChannelDefinition getSseDefinitionFromChannelId(
			String channelId);

	/**
	 * Fetches the definitions of a station monitor channel.
	 * 
	 * @param channelId the channel ID to look for
	 * @return the channel definition object, or null if not found
	 */
	public abstract IChannelDefinition getMonitorDefinitionFromChannelId(
			String channelId);
	
	/**
	 * Gets the complete list of channel modules found in all the loaded dictionaries.  
	 * 
	 * @return Sorted list of module strings; never null
	 * 
	 */
	public abstract List<String> getModules();

	/**
	 * Gets the complete list of channel subsystems found in all the loaded
	 * dictionaries.  
	 * 
	 * @return Sorted list of module strings; never null
	 * 
	 */
	public abstract List<String> getSubsystems();

	/**
	 * Gets the complete list of channel subsystems found in all the loaded
	 * dictionaries.  
	 * 
	 * @return Sorted list of module strings; never null
	 * 
	 */
	public abstract List<String> getOpsCategories();

	/**
	 * Gets the channel derivation definition that includes the specified channel 
	 * as a child.
	 * 
	 * @param chanId child channel ID
	 * @return channel derivation definition for the derivation that produces the
	 *         child, or null if not found
	 */
	public abstract IChannelDerivation getDerivationForChannelId(String chanId);

	/**
	 * Manually adds a FSW channel non-header derivation definition.
	 * Should be used only by unit tests. 
	 * 
	 * @param derive the definition to add
	 */
	public abstract void addFswDerivation(IChannelDerivation derive);

	/**
	 * Manually adds a FSW or FSW header channel derivation definition.
	 * Should be used only by unit tests. 
	 * 
	 * @param derive the definition to add
	 * @param isHeader true if a header derivation, false if not
	 */
	public abstract void addFswDerivation(IChannelDerivation derive,
			boolean isHeader);

	/**
	 * Manually adds an SSE channel non-header derivation definition.
	 * Should be used only by unit tests. 
	 * 
	 * @param derive the definition to add
	 */
	public abstract void addSseDerivation(IChannelDerivation derive);

	/**
	 * Manually adds an SSE or SSE header channel derivation definition.
	 * Should be used only by unit tests. 
	 * 
	 * @param derive the definition to add
	 * @param isHeader true if a header derivation, false if not
	 */
	public abstract void addSseDerivation(IChannelDerivation derive,
			boolean isHeader);

	/**
	 * Manually adds a station monitor derivation definition.
	 * Should be used only by unit tests. 
	 * 
	 * @param derive the definition to add
	 */
	public abstract void addMonitorDerivation(IChannelDerivation derive);
	
	/**
	 * This is mainly a test function, but it allows for the map to be empty, meaning
	 * no dictionary is loaded and you do not get unloaded exceptions.
	 */
	public void resetAllEmpty();

}