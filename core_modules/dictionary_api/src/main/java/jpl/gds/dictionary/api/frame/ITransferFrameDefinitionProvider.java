package jpl.gds.dictionary.api.frame;

import java.util.List;

import jpl.gds.dictionary.api.IDefinitionProviderLoadStatus;

public interface ITransferFrameDefinitionProvider extends IDefinitionProviderLoadStatus {
    /**
     * Gets the maximum unencoded frame CADU size in bytes, from among all 
     * the frame definitions in the dictionary.
     * 
     * @return the frame size in bytes
     */
    public int getMaxFrameSize();

	/**
	 * Gets the list of transfer frame definitions from this dictionary.
	 * 
	 * @return a list of ITransferFrameDefinition objects, or an empty list if
	 *         none found/loaded
	 */
	List<ITransferFrameDefinition> getFrameDefinitions();

	/**
	 * Gets the ITransferFrameDefinition object that matches the given frame
	 * type name.
	 * 
	 * @param type
	 *            the unique frame type name
	 * @return matching ITransferFrameDefinition object, or null if no match.
	 * 
	 */
	ITransferFrameDefinition findFrameDefinition(String type);

	/**
	 * 
	 * Gets the TransferFrameFormat object that matches the given CADU bit
	 * size.
	 * 
	 * @param sizeBits
	 * 	          the CADU size to match in bits.
	 * 
	 * @return the matching ITransferFrameDefinition object, or null if no match
	 *         found
	 */
	ITransferFrameDefinition findFrameDefinition(int sizeBits);

	/**
	 * 
	 * Gets the TransferFrameFormat object that matches the given CADU bit
	 * size.
	 * 
	 * @param sizeBits
	 * 	          the CADU size to match in bits.
	 * @param turboRate
	 * 	           the turbo rate ("1/2", "1/3", or "1/6"), or null if not specified.
	 * @return the matching ITransferFrameDefinition object, or null if no match
	 *         found
	 */
	ITransferFrameDefinition findFrameDefinition(int sizeBits, String turboRate);

}