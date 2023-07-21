/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package ammos.datagen.dictionary.genericdecom;

import jpl.gds.dictionary.api.channel.ChannelType;

/**
 * This class is used by the decom map pre-processor to represent a channel
 * sample to be written to a decom packet on a specific decom branch.
 * 
 *
 * MPCS-6944 - 12/10/14. Added class.
 */
public class DecomPreprocessorChannel implements IDecomPreprocessorElement {

	/**
	 * Separator used between elements in the information string.
	 */
	public static final String CHANNEL_COMPONENT_SEP = ",";

	/**
	 * Tag for information lines that represent a channel sample.
	 */
	public static final String CHANNEL_TAG = "CHANNEL: ";

	/**
	 * Tag that will precede the channel type in the element information string.
	 */
	public static final String TYPE_TAG = "type=";

	private final DecomPreprocessorBranch branchId;
	private final String chanId;
	private final int bitOffset;
	private final int bitLength;
	private final int byteOffset;
	private final ChannelType type;

	/**
	 * Constructor. Automatically adds the channel sample definition to the
	 * specified parent branch.
	 * 
	 * @param branch
	 *            the parent branch on which this channel sample is written
	 * @param chanId
	 *            the FSW channel ID from the channel dictionary
	 * @param byteOffset
	 *            the byte offset of the channel sample in the data
	 * @param bitOffset
	 *            the bit offset (within byte) of the channel sample in the data
	 * @param bitLength
	 *            the bit length of the sample value in the data
	 * @param type
	 *            the data type of the channel
	 */
	public DecomPreprocessorChannel(final DecomPreprocessorBranch branch,
			final String chanId, final int byteOffset, final int bitOffset,
			final int bitLength, final ChannelType type) {
		this.branchId = branch;
		this.chanId = chanId;
		this.bitOffset = bitOffset;
		this.byteOffset = byteOffset;
		this.bitLength = bitLength;
		this.type = type;
		branch.addChannel(this);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getParentBranch()
	 */
	@Override
	public DecomPreprocessorBranch getParentBranch() {
		return this.branchId;
	}

	/**
	 * Gets the byte offset of the channel sample in the packet data.
	 * 
	 * @return byte offset
	 */
	public int getByteOffset() {
		return this.byteOffset;
	}

	/**
	 * Gets the bit offset of the channel sample in the packet data, within the
	 * byte offset. Must be 0-7.
	 * 
	 * @return bit offset
	 */
	public int getBitOffset() {
		return this.bitOffset;
	}

	/**
	 * Gets the bit length of the channel sample in the packet data.
	 * 
	 * @return bit length
	 */
	public int getBitLength() {
		return this.bitLength;
	}

	/**
	 * Gets the data type of the channel sample.
	 * 
	 * @return ChannelType
	 */
	public ChannelType getType() {
		return this.type;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Return valye orresponds to the channel ID.
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getName()
	 */
	@Override
	public String getName() {
		return this.chanId;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getPath()
	 */
	@Override
	public String getPath() {
		return this.branchId.getPath() + BRANCH_PATH_SEP + getName();
	}

	@Override
	public String getInfoString() {
		final StringBuilder sb = new StringBuilder(CHANNEL_TAG + this.getPath()
				+ BRANCH_PATH_SEP);
		sb.append("name=" + this.getName());
		sb.append(CHANNEL_COMPONENT_SEP + OFFSET_TAG + this.byteOffset
				+ OFFSET_SEP + this.bitOffset);
		sb.append(CHANNEL_COMPONENT_SEP + LENGTH_TAG + this.bitLength);
		sb.append(CHANNEL_COMPONENT_SEP + TYPE_TAG + this.type);
		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getInfoString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof DecomPreprocessorChannel)) {
			return false;
		}
		return this.getPath().equals(((DecomPreprocessorChannel) o).getPath());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.getPath().hashCode();
	}

}