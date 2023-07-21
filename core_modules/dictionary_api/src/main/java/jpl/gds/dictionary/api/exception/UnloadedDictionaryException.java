/**
 * 
 */
package jpl.gds.dictionary.api.exception;

import jpl.gds.dictionary.api.config.DictionaryType;

/**
 * A runtime exception that indicates a dictionary that has not been loaded
 * is accessed in a way that it must be loaded.
 * 
 *
 */
@SuppressWarnings("serial")
public class UnloadedDictionaryException extends RuntimeException {

	/**
	 * @param message
	 */
	public UnloadedDictionaryException(DictionaryType type) {
		super(String.format("Dictionary of type %s has been accessed before it was loaded.", type));
	}
}
