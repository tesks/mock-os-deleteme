/**
 * 
 */
package jpl.gds.dictionary.api.client.frame;

import java.util.List;

import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.exception.UnloadedDictionaryException;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider;

/**
 * Class UnloadedTransferFrameDefinitionProvider
 *
 */
public class UnloadedTransferFrameDefinitionProvider implements ITransferFrameDefinitionProvider {

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider#getFrameDefinitions()
	 */
	@Override
	public List<ITransferFrameDefinition> getFrameDefinitions() {
		throw new UnloadedDictionaryException(DictionaryType.FRAME);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider#findFrameDefinition(java.lang.String)
	 */
	@Override
	public ITransferFrameDefinition findFrameDefinition(String type) {
		throw new UnloadedDictionaryException(DictionaryType.FRAME);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider#findFrameDefinition(int)
	 */
	@Override
	public ITransferFrameDefinition findFrameDefinition(int sizeBits) {
		throw new UnloadedDictionaryException(DictionaryType.FRAME);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.frame.ITransferFrameDefinitionProvider#findFrameDefinition(int, java.lang.String)
	 */
	@Override
	public ITransferFrameDefinition findFrameDefinition(int sizeBits, String turboRate) {
		throw new UnloadedDictionaryException(DictionaryType.FRAME);
	}

	@Override
	public int getMaxFrameSize() {
		throw new UnloadedDictionaryException(DictionaryType.FRAME);
	}

}
