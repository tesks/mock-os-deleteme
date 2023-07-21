/**
 * 
 */
package jpl.gds.dictionary.api.client.command;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.command.CommandStemMappingOnlyDefinitionProvider;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * Class CommandUtilityDictionaryManager
 *
 */
public class CommandUtilityDictionaryManager implements ICommandUtilityDictionaryManager {
	private final DictionaryProperties dictConfig;
	private final IDictionaryCache cache;

	/** 
	 * If the dictionary loading is enabled.
	 */
	private final boolean isEnabled;
	

	private ICommandDefinitionProvider provider;
	boolean isLoaded;

	/**
	 * @param dictConfig
	 * @param cache
	 * @param isEnabled
	 */
	public CommandUtilityDictionaryManager(final DictionaryProperties dictConfig, final IDictionaryCache cache, final boolean isEnabled) {
		super();
		this.dictConfig = dictConfig;
		this.cache = cache;
		this.isEnabled = isEnabled;

		/**
		 * Set this to the unloaded type by default.  Using this makes it 
		 * so we do not have to check if the provider is null every time one of
		 * the pass through to the dictionary methods is called.
		 */
		this.provider = new UnloadedCommandDefinitionProvider();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getOpcodeForStem(java.lang.String)
	 */
	@Override
	public int getOpcodeForStem(final String stem) {
		return provider.getOpcodeForStem(stem);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getStemForOpcode(int)
	 */
	@Override
	public String getStemForOpcode(final int opcode) {
		return provider.getStemForOpcode(opcode);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getStemByOpcodeMap()
	 */
	@Override
	public Map<String, String> getStemByOpcodeMap() {
		// Check if Command is enabled
		if(isEnabled) {
			return provider.getStemByOpcodeMap();
		}
		else{
			return Collections.emptyMap();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getOpcodeByStemMap()
	 */
	@Override
	public Map<String, String> getOpcodeByStemMap() {
		// Check if Command is enabled
		if(isEnabled) {
			return provider.getOpcodeByStemMap();
		}
		else{
			return Collections.emptyMap();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getCommandDefinitionForStem(java.lang.String)
	 */
	@Override
	public ICommandDefinition getCommandDefinitionForStem(final String stem) {
		return provider.getCommandDefinitionForStem(stem);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getCommandDefinitionForOpcode(java.lang.String)
	 */
	@Override
	public ICommandDefinition getCommandDefinitionForOpcode(final String opcode) {
		return provider.getCommandDefinitionForOpcode(opcode);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getStems()
	 */
	@Override
	public List<String> getStems() {
		return provider.getStems();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.command.ICommandUtilityDictionaryManager#load()
	 */
	@Override
	public void load() throws DictionaryException {
		load(false);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.command.ICommandUtilityDictionaryManager#load(boolean)
	 */
	@Override
	public void load(final boolean loadMapsOnly) throws DictionaryException {
        if (isLoaded) {
            throw new DictionaryException("Dictionary has already been loaded");
        }

		if (isEnabled) {
			final ICommandDefinitionProvider _provider = cache.getCommandDictionary(dictConfig);

			this.provider = loadMapsOnly ? new CommandStemMappingOnlyDefinitionProvider(dictConfig, _provider) : _provider;

			isLoaded = true;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.command.ICommandUtilityDictionaryManager#clear()
	 */
	@Override
	public void clear() {
		provider = new UnloadedCommandDefinitionProvider();
		isLoaded = false;
	}

	@Override
	public String getBuildVersionId() {
		return provider.getBuildVersionId();
	}

	@Override
	public String getGdsVersionId() {
		return provider.getGdsVersionId();
	}

	@Override
	public String getReleaseVersionId() {
		return provider.getReleaseVersionId();
	}

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.api.IDefinitionProviderLoadStatus#isLoaded()
     */
    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

}
