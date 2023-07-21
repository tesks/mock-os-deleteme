/**
 * 
 */
package jpl.gds.dictionary.api.client.apid;

import java.util.SortedSet;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.apid.ApidDefinitionFactory;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.apid.IApidDictionary;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * Apid dictionary manager.
 * 
 *
 */
public class ApidUtilityDictionaryManager implements IApidUtilityDictionaryManager {
	private final DictionaryProperties dictConfig;
	private final IDictionaryCache cache;
	private final boolean isSse;

	/** 
	 * If the dictionary loading is enabled.
	 */
	private final boolean isEnabled;
	

	private IApidDefinitionProvider provider;
	boolean isLoaded;

	/**
     * @param cache
     *            IDictionaryCache manager
     * @param dictConfig
     *            configured DictionaryProperties
     * @param isEnabled
     *            whether or not this dictionary is enabled
     * @param isSse
     *            whether or not this dictionary is SSE
     */
	public ApidUtilityDictionaryManager(final IDictionaryCache cache, final DictionaryProperties dictConfig, 
			final boolean isEnabled, final boolean isSse) {
		this.dictConfig = dictConfig;
		this.isSse = isSse;
		this.isEnabled = isEnabled;
		this.cache = cache;
		
		
		/**
		 * Set this to the unloaded type by default.  Using this makes it 
		 * so we do not have to check if the provider is null every time one of
		 * the pass through to the dictionary methods is called.
		 */
		this.provider = new UnloadedApidDefinitionProvider();
	}
	

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.apid.IApidDefinitionProvider#getApidDefinition(int)
	 */
	@Override
	public IApidDefinition getApidDefinition(final int apid) {
		// Check if APID is enabled
		if(isEnabled) {
			return provider.getApidDefinition(apid);
		}
		return ApidDefinitionFactory.createApid();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.apid.IApidDefinitionProvider#getApidDefinition(java.lang.String)
	 */
	@Override
	public IApidDefinition getApidDefinition(final String apid) {
		// Check if APID is enabled
		if(isEnabled) {
			return provider.getApidDefinition(apid);
		}
		return ApidDefinitionFactory.createApid();
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.apid.IApidUtilityDictionaryManager#clear()
	 */
	@Override
	public void clear() {
		provider = new UnloadedApidDefinitionProvider();
		isLoaded = false;
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.apid.IApidUtilityDictionaryManager#load()
	 */
	@Override
	public void load() throws DictionaryException {
		if (isLoaded) {
			throw new DictionaryException("Dictionary has already been loaded");
		}
		
		/**
		 * Load can be called even if this dictionary type is disabled.
		 */
		if (isEnabled) {
			this.provider = cache.getApidDictionary(dictConfig, isSse);
			isLoaded = true;
		}
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.apid.IApidDefinitionProvider#getChannelApids()
	 */
	@Override
	public SortedSet<Integer> getChannelApids() {
		return provider.getChannelApids();
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.apid.IApidDefinitionProvider#getDecomApids()
	 */
	@Override
	public SortedSet<Integer> getDecomApids() {
		return provider.getDecomApids();
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.apid.IApidDefinitionProvider#getEvrApids()
	 */
	@Override
	public SortedSet<Integer> getEvrApids() {
		return provider.getEvrApids();
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.apid.IApidDefinitionProvider#getProductApids()
	 */
	@Override
	public SortedSet<Integer> getProductApids() {
		return provider.getProductApids();
	}

    @Override
    public SortedSet<Integer> getCfdpApids() {
        return provider.getCfdpApids();
    }


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.apid.IApidDefinitionProvider#isDefinedApid(int)
	 */
	@Override
	public boolean isDefinedApid(final int apid) {
		return provider.isDefinedApid(apid);
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.apid.IApidDefinitionProvider#getApidName(int)
	 */
	@Override
	public String getApidName(final int apid) {
		return provider.getApidName(apid);
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.apid.IApidUtilityDictionaryManager#getApidDictionary()
	 */
	@Override
	public IApidDictionary getApidDictionary() throws DictionaryException {
		if (provider instanceof IApidDictionary) {
			return (IApidDictionary) provider;
		} else {
			throw new DictionaryException("Managed apid dictionary provider is not an IApidDictionary.  Most likely the dictionary was disabled or was not loaded.");
		}
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
