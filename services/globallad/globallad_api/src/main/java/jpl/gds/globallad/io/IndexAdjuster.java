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
package jpl.gds.globallad.io;

/**
 * Class to keep track of the internal data. This is needed because global lad
 * data words may span byte arrays in the internal store.
 */
public class IndexAdjuster {
	private int arrayIndex;
	private int offsetAdjust;
	private int offset;

	private boolean valuesSet;

	/**
	 * @param arrayIndex
	 * @param offsetAdjust
	 * @param offset
	 */
	public IndexAdjuster(int arrayIndex, int offsetAdjust, int offset) {
		this.arrayIndex = arrayIndex;
		this.offsetAdjust = offsetAdjust;
		this.offset = offset;

		valuesSet = arrayIndex >= 0 && offsetAdjust >= 0;
	}

	
	/**
	 * Added a way to change this object.
	 */
	public void adjust(int arrayIndex, int offsetAdjust, int offset) {
		this.arrayIndex = arrayIndex;
		this.offsetAdjust = offsetAdjust;
		this.offset = offset;

		valuesSet = arrayIndex >= 0 && offsetAdjust >= 0;
	}

	/**
	 * 
	 */
	public IndexAdjuster() {
		this(-1, -1, -1);
	}

	/**
	 * @return true if all values are set and not the default
	 */
	public boolean isSet() {
		return valuesSet;
	}

	/**
	 * @return get the array index, IE which array has the data at offset
	 */
	public int getIndex() {
		return arrayIndex;
	}

	/**
	 * @return get the offset adjust, which is the index into the array offset
	 *         resides in.
	 */
	public int getOffsetAdjust() {
		return offsetAdjust;
	}

	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @return the adjusted offset
	 */
	public int getAdjustedOffset() {
		return getOffset() - getOffsetAdjust();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("ArrayIndex=%5d    OffsetAdjust=%5d     Adjusted=%5d", getIndex(), getOffsetAdjust(),
				getAdjustedOffset());
	}
}
