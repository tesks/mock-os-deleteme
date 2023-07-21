/**
 * 
 */
package jpl.gds.dictionary.api.client.sequence;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;

/**
 * Interface ISequenceUtilityDictionaryManager
 *
 */
public interface ISequenceUtilityDictionaryManager extends ISequenceDefinitionProvider {
	
	/**
	 * Loads sequence dictonary
	 * @param required if true will error if not found.
	 * @throws DictionaryException
	 */
	public void load(boolean required) throws DictionaryException;
	
	public void clear();

}
