/**
 * 
 */
package jpl.gds.dictionary.api.client.decom;

import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.decom.IChannelDecomDefinitionProvider;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapId;

/**
 * Class ChannelDecomUtilityDictionaryManager
 *
 */
public class ChannelDecomUtilityDictionaryManager implements IChannelDecomUtilityDictionaryManager {
	private final DictionaryProperties dictConfig;
	private final IDictionaryCache cache;
	private final boolean isSse;

	private IChannelDecomDefinitionProvider provider;

	/** 
	 * If the dictionary loading is enabled.
	 */
	private final boolean isEnabled;
	private boolean isLoaded;

	/**
	 * @param dictConfig
	 * @param cache
	 * @param isEnabled
	 */
	public ChannelDecomUtilityDictionaryManager(final DictionaryProperties dictConfig, final IDictionaryCache cache,
			final boolean isEnabled, final boolean isSse) {
		super();
		this.dictConfig = dictConfig;
		this.cache = cache;
		this.isEnabled = isEnabled;
		this.isSse = isSse;

		/**
		 * Set this to the unloaded type by default.  Using this makes it 
		 * so we do not have to check if the provider is null every time one of
		 * the pass through to the dictionary methods is called.
		 */
		this.provider = new UnloadedDecomChannelDefinitionProvider();
		this.isLoaded = false;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.decom.adaptation.IChannelDecomDefinitionProvider#addDecomPacketMap(int)
	 */
	@Override
	public IDecomMapDefinition addDecomPacketMap(final int apid) throws DictionaryException {
		return provider.addDecomPacketMap(apid);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.decom.adaptation.IChannelDecomDefinitionProvider#getDecomMapByApid(int)
	 */
	@Override
	public IDecomMapDefinition getDecomMapByApid(final int apid) {
		return provider.getDecomMapByApid(apid);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.decom.adaptation.IChannelDecomDefinitionProvider#getGeneralDecomMap()
	 */
	@Override
	public IDecomMapDefinition getGeneralDecomMap() {
		return provider.getGeneralDecomMap();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.decom.adaptation.IChannelDecomDefinitionProvider#getAllDecomMaps()
	 */
	@Override
	public Map<Integer, IDecomMapDefinition> getAllDecomMaps() {
		return provider.getAllDecomMaps();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.decom.adaptation.IChannelDecomUtilityDictionaryManager#load()
	 */
	@Override
	public void load(final Map<String, IChannelDefinition> channelIdMapping) throws DictionaryException {
		if (isLoaded) {
			throw new DictionaryException("Dictionary has already been loaded");
		}

		/**
		 * Load can be called even if this dictionary type is disabled.
		 */
		if (isEnabled) {
			this.provider = cache.getChannelDecomDictionary(dictConfig, channelIdMapping, isSse);

			isLoaded = true;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.decom.adaptation.IChannelDecomUtilityDictionaryManager#clear()
	 */
	@Override
	public void clear() {
//		this.provider = new UnloadedDecomChannelDefinitionProvider();
		this.provider.clear();
//		this.isLoaded = false;
	}

	@Override
	public void setGeneralMap(final IDecomMapDefinition map) {
		provider.setGeneralMap(map);
		
	}

	@Override
	public IDecomMapDefinition addDecomMapFromFile(final String filename) throws DictionaryException {
		return provider.addDecomMapFromFile(filename);
	}

	@Override
	public void setChannelMap(final Map<String, IChannelDefinition> chanMap) {
		provider.setChannelMap(chanMap);
		
	}

	@Override
	public void addMap(final int apid, final IDecomMapDefinition map) {
		provider.addMap(apid, map);
	}

	@Override
	public IDecomMapDefinition getDecomMapById(final IDecomMapId id) {
		return provider.getDecomMapById(id);
	}

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.api.IDefinitionProviderLoadStatus#isLoaded()
     */
    @Override
    public boolean isLoaded() {
        return this.isLoaded;
    }

}
