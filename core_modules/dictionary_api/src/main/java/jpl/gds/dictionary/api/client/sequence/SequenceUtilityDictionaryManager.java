/**
 * 
 */
package jpl.gds.dictionary.api.client.sequence;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;

/**
 * Class SequenceUtilityDictionaryManager
 */
public class SequenceUtilityDictionaryManager implements ISequenceUtilityDictionaryManager {
	private final DictionaryProperties dictConfig;
	private final IDictionaryCache cache;
	private ISequenceDefinitionProvider provider;

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
	public SequenceUtilityDictionaryManager(final DictionaryProperties dictConfig, final IDictionaryCache cache, final boolean isEnabled) {
		super();
		this.dictConfig = dictConfig;
		this.cache = cache;
		this.isEnabled = isEnabled;

		/**
		 * Set this to the unloaded type by default.  Using this makes it 
		 * so we do not have to check if the provider is null every time one of
		 * the pass through to the dictionary methods is called.
		 */
		this.provider = new UnloadedSequenceDefinitionProvider();
		this.isLoaded = false;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider#getCategoryNameByCategoryId(int)
	 */
	@Override
	public String getCategoryNameByCategoryId(final int catId) {
		return provider.getCategoryNameByCategoryId(catId);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider#getCategoryIdByCategoryName(java.lang.String)
	 */
	@Override
	public Integer getCategoryIdByCategoryName(final String category) {
		return provider.getCategoryIdByCategoryName(category);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider#getSeqNameFromSeqIdBytes(byte[])
	 */
	@Override
	public String getSeqNameFromSeqIdBytes(final byte[] seqIdBytes) {
		return provider.getSeqNameFromSeqIdBytes(seqIdBytes);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider#getCategoryIdFromSeqId(int)
	 */
	@Override
	public int getCategoryIdFromSeqId(final int seqid) {
		return provider.getCategoryIdFromSeqId(seqid);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider#getSequenceNumberFromSeqId(int)
	 */
	@Override
	public int getSequenceNumberFromSeqId(final int seqid) {
		return provider.getSequenceNumberFromSeqId(seqid);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.sequence.ISequenceUtilityDictionaryManager#load(boolean)
	 */
	@Override
	public void load(final boolean required) throws DictionaryException {
		if (isLoaded) {
			throw new DictionaryException("Dictionary has already been loaded");
		}

		/**
		 * Load can be called even if this dictionary type is disabled.
		 */
		if (isEnabled) {
			try {
				this.provider = cache.getSequenceDictionary(dictConfig);
                isLoaded = true;
			} catch (final Exception e) {
				if (required) {
					throw e;
				} else {
					// Don't care, not required.
				}
			}
		}

	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.sequence.ISequenceUtilityDictionaryManager#clear()
	 */
	@Override
	public void clear() {
		provider = new UnloadedSequenceDefinitionProvider();
		isLoaded = false;
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
