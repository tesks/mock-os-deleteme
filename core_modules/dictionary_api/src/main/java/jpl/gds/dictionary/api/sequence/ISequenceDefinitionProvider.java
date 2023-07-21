package jpl.gds.dictionary.api.sequence;

import jpl.gds.dictionary.api.IDefinitionProviderLoadStatus;

public interface ISequenceDefinitionProvider extends IDefinitionProviderLoadStatus {

	/**
	 * Gets the sequence category name associated with the given category ID.
	 * 
	 * @param catId
	 *            an unsigned number from 0 to 63 that identifies a sequence
	 *            category
	 * @return category name associated with given ID, or null if no match is
	 *         found
	 */
	String getCategoryNameByCategoryId(int catId);

	/**
	 * Gets the sequence category ID associated with the given category name.
	 * 
	 * @param category
	 *            a non-null category name
	 * @return the ID associated with given name (an unsigned number from 0 to
	 *         63), or null if no match is found
	 */
	Integer getCategoryIdByCategoryName(String category);

	/**
	 * Creates a human-readable sequence name given a byte array containing a
	 * raw sequence ID by separating the category ID from the sequence number,
	 * mapping the category ID to a name, and using these two values to
	 * construct the final sequence name.
	 * 
	 * @param seqIdBytes
	 *            a minimum 4 byte array containing the raw sequence ID in
	 *            big-endian format
	 * @return Sequence name string
	 * 
	 */
	String getSeqNameFromSeqIdBytes(byte[] seqIdBytes);

	/**
	 * Extracts the category ID from a whole sequence ID. In the current
	 * implementation, the category ID is assumed to occupy bits 12 - 17 of the
	 * supplied integer, counting from bit LSB=0 on the right.
	 * 
	 * @param seqid
	 *            the sequence ID to extract from
	 * @return category ID number
	 */
	int getCategoryIdFromSeqId(int seqid);

	/**
	 * Extracts the sequence number from a whole sequence ID. In the current
	 * implementation, the sequence number is assumed to occupy bits 0 - 15 of
	 * the supplied integer, counting from bit LSB=0 on the right.
	 * 
	 * @param seqid
	 *            the sequence ID to extract from
	 * @return sequence number, 16 bit unsigned
	 */
	int getSequenceNumberFromSeqId(int seqid);

}