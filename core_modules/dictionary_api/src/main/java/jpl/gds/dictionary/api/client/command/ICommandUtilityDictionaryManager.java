/**
 * 
 */
package jpl.gds.dictionary.api.client.command;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;

/**
 * Interface ICommandUtilityDictionaryManager
 *
 */
public interface ICommandUtilityDictionaryManager extends ICommandDefinitionProvider {

	public void load() throws DictionaryException;

	/**
	 * Loads the command dictionary.
	 * 
	 * @param loadMapsOnly
	 * 	  If true this will drop the dictionary and only hold onto
	 *    the stem to opcode and opcode to stem mappings.  
	 * @throws DictionaryException
	 */
	public void load(boolean loadMapsOnly) throws DictionaryException;

	
	public void clear();
	

}
