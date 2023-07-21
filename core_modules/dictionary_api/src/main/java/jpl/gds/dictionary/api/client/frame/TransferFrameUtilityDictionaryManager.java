package jpl.gds.dictionary.api.client.frame;

import java.util.List;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;

public class TransferFrameUtilityDictionaryManager implements ITransferFrameUtilityDictionaryManager {
	private final DictionaryProperties dictConfig;
	private final IDictionaryCache cache;
	private ITransferFrameDefinitionProvider provider;

	/** 
	 * If the dictionary loading is enabled.
	 */
	private final boolean isEnabled;
	private boolean isLoaded;


	/**
	 * @param dictConfig
	 * @param cache
	 */
	public TransferFrameUtilityDictionaryManager(final DictionaryProperties dictConfig, final IDictionaryCache cache,
			final boolean isEnabled) {
		super();
		this.dictConfig = dictConfig;
		this.cache = cache;
		this.isEnabled = isEnabled;

		/**
		 * Set this to the unloaded type by default.  Using this makes it 
		 * so we do not have to check if the provider is null every time one of
		 * the pass through to the dictionary methods is called.
		 */
		this.provider = new UnloadedTransferFrameDefinitionProvider();
		this.isLoaded = false;
	}

	@Override
	public List<ITransferFrameDefinition> getFrameDefinitions() {
		return provider.getFrameDefinitions();
	}

	@Override
	public ITransferFrameDefinition findFrameDefinition(final String type) {
		return provider.findFrameDefinition(type);
	}

	@Override
	public ITransferFrameDefinition findFrameDefinition(final int sizeBits) {
		return provider.findFrameDefinition(sizeBits);
	}

	@Override
	public ITransferFrameDefinition findFrameDefinition(final int sizeBits, final String turboRate) {
		return provider.findFrameDefinition(sizeBits, turboRate);
	}

	@Override
	public void load() throws DictionaryException {
		if (isLoaded) {
			throw new DictionaryException("Dictionary has already been loaded");
		}

		/**
		 * Load can be called even if this dictionary type is disabled.
		 */
		if (isEnabled) {
			this.provider = cache.getTransferFrameDictionary(dictConfig);
			isLoaded = true;
		}
	}

	@Override
	public void load(final String filePath) throws DictionaryException {
		if (isLoaded) {
			throw new DictionaryException("Dictionary has already been loaded");
		}

		/**
		 * Load can be called even if this dictionary type is disabled.
		 */
		if (isEnabled) {
			this.provider = cache.getTransferFrameDictionary(dictConfig, filePath);

            isLoaded = true;
		}
	}

	@Override
	public void clear() {
		provider = new UnloadedTransferFrameDefinitionProvider();
		isLoaded = false;
	}

	@Override
	public int getMaxFrameSize() {
		return provider.getMaxFrameSize();
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
