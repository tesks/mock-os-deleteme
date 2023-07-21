package jpl.gds.dictionary.api.client.apid;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.apid.IApidDictionary;

public interface IApidUtilityDictionaryManager extends IApidDefinitionProvider {

	public void load() throws DictionaryException;
	
	public void clear();

	/**
	 * Provides a way to get the apid dictionary object.
	 * @return apid dictionary if it was loaded.  Null if it was not.
	 */
	IApidDictionary getApidDictionary() throws DictionaryException;

}