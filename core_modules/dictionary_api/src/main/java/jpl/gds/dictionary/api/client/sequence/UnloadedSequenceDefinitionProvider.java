/**
 * 
 */
package jpl.gds.dictionary.api.client.sequence;

import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.exception.UnloadedDictionaryException;
import jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider;

/**
 * Class UnloadedSequenceDefinitionProvider
 *
 */
public class UnloadedSequenceDefinitionProvider implements ISequenceDefinitionProvider {

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider#getCategoryNameByCategoryId(int)
	 */
	@Override
	public String getCategoryNameByCategoryId(int catId) {
		throw new UnloadedDictionaryException(DictionaryType.SEQUENCE);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider#getCategoryIdByCategoryName(java.lang.String)
	 */
	@Override
	public Integer getCategoryIdByCategoryName(String category) {
		throw new UnloadedDictionaryException(DictionaryType.SEQUENCE);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider#getSeqNameFromSeqIdBytes(byte[])
	 */
	@Override
	public String getSeqNameFromSeqIdBytes(byte[] seqIdBytes) {
		throw new UnloadedDictionaryException(DictionaryType.SEQUENCE);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider#getCategoryIdFromSeqId(int)
	 */
	@Override
	public int getCategoryIdFromSeqId(int seqid) {
		throw new UnloadedDictionaryException(DictionaryType.SEQUENCE);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.sequence.ISequenceDefinitionProvider#getSequenceNumberFromSeqId(int)
	 */
	@Override
	public int getSequenceNumberFromSeqId(int seqid) {
		throw new UnloadedDictionaryException(DictionaryType.SEQUENCE);
	}

}
