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
public class DictionaryLoadingException extends RuntimeException {

	/**
	 * @param message
	 */
	public DictionaryLoadingException(DictionaryType type, Throwable cause) {
		super(String.format("Dictionary of type %s failed to load.", type), cause);
	}
}
