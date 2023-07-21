/**
 * 
 */
package jpl.gds.dictionary.api.client.frame;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;

/**
 * Interface ITransferFrameUtilityDictionaryManager
 *
 */
public interface ITransferFrameUtilityDictionaryManager extends ITransferFrameDefinitionProvider {
	
	/**
	 * @throws DictionaryException
	 */
	public void load() throws DictionaryException;
	
	public void clear();

	void load(String filePath) throws DictionaryException;

	

}
