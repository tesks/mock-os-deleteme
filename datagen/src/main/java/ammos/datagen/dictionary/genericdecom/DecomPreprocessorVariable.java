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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is used by the decom map pre-processor to represent a variable to
 * be written to a decom packet on a specific decom branch.
 * 
 *
 * MPCS-6944 - 12/10/14. Added class.
 */
public class DecomPreprocessorVariable implements IDecomPreprocessorVariable {

	/**
	 * Tag for information lines that represent a variable.
	 */
	public static final String VARIABLE_TAG = "VARIABLE: ";

	private final DecomPreprocessorBranch branch;
	private final String variableName;
	private final int bitOffset;
	private final int bitLength;
	private final int byteOffset;
	private final List<Long> potentialValues = new LinkedList<Long>();
	private boolean allowOther;

	/**
	 * Constructor. Automatically adds the variable to the specified parent
	 * branch.
	 * 
	 * @param base
	 *            the parent decom branch for the variable
	 * @param varName
	 *            the name of the variable in the decom map
	 * @param byteOffset
	 *            the byte offset of the variable in the data
	 * @param bitOffse
	 *            the bit offset (within byte) of the variable in the data
	 * @param bitLength
	 *            the bit length of the variable in the data
	 */
	public DecomPreprocessorVariable(final DecomPreprocessorBranch base,
			final String varName, final int byteOffset, final int bitOffset,
			final int bitLength) {
		this.branch = base;
		this.variableName = varName;
		this.bitOffset = bitOffset;
		this.byteOffset = byteOffset;
		this.bitLength = bitLength;
		base.addVariable(this);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getPath()
	 */
	@Override
	public String getPath() {
		return this.branch.getPath() + BRANCH_PATH_SEP + this.variableName;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getName()
	 */
	@Override
	public String getName() {
		return this.variableName;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getParentBranch()
	 */
	@Override
	public DecomPreprocessorBranch getParentBranch() {
		return this.branch;
	}

	/**
	 * Gets the byte offset of the variable value in the packet data.
	 * 
	 * @return byte offset
	 */
	public int getByteOffset() {
		return this.byteOffset;
	}

	/**
	 * Gets the bit offset of the variable value in the packet data, within
	 * byte. Must be 0-7.
	 * 
	 * @return bit offset within byte
	 */
	public int getBitOffset() {
		return this.bitOffset;
	}

	/**
	 * Gets the bit length of the variable value in the packet data.
	 * 
	 * @return bit length
	 */
	public int getBitLength() {
		return this.bitLength;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorVariable#getPotentialValues()
	 */
	@Override
	public List<Long> getPotentialValues() {
		return Collections.unmodifiableList(this.potentialValues);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorVariable#addPotentialValue(long)
	 */
	@Override
	public void addPotentialValue(final long val) {
		if (!this.potentialValues.contains(val)) {
			this.potentialValues.add(val);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorVariable#setAllowOther(boolean)
	 */
	@Override
	public void setAllowOther(final boolean allow) {
		this.allowOther = allow;

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorVariable#isAllowOther()
	 */
	@Override
	public boolean isAllowOther() {
		return this.allowOther;
	}

	@Override
	public String getInfoString() {
		final StringBuilder sb = new StringBuilder(VARIABLE_TAG
				+ this.getPath() + BRANCH_PATH_SEP);
		sb.append("name=" + this.getName());
		sb.append(VARIABLE_COMPONENT_SEP + OFFSET_TAG + this.byteOffset
				+ OFFSET_SEP + this.bitOffset);
		sb.append(VARIABLE_COMPONENT_SEP + LENGTH_TAG + this.bitLength);
		sb.append(VARIABLE_COMPONENT_SEP + VALUES_TAG);
		int i = 0;
		for (final Long v : this.potentialValues) {
			sb.append(v);
			if (i < this.potentialValues.size() - 1) {
				sb.append(VARIABLE_VALUE_SEP);
			}
			i++;
		}
		if (this.isAllowOther()) {
			sb.append(VARIABLE_VALUE_SEP + OTHER_VALUE);
		}

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
		if (!(o instanceof DecomPreprocessorVariable)) {
			return false;
		}
		return this.getPath().equals(((DecomPreprocessorVariable) o).getPath());
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