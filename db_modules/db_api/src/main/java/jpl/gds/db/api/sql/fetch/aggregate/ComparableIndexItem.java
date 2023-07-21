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
package jpl.gds.db.api.sql.fetch.aggregate;

import java.util.Comparator;

/**
 * Comparable batch index container
 *
 * @param <T>
 */
public class ComparableIndexItem<T extends Comparable<T>> implements Comparable<T>{
	private String batchId;
	private int index;
	private T comparable;
	
	/**
	 * Constructor.
	 * 
	 * @param batchId
	 * @param index
	 * @param comparable
	 */
	public ComparableIndexItem(final String batchId, final int index, final T comparable) {
		this.batchId = batchId;
		this.index = index;
		this.comparable = comparable;
	}
	
	public String getBatchId() {
		return batchId;
	}

	public void setBatchId(final String batchId) {
		this.batchId = batchId;
	}
	
	public int getIndex() {
		return index;
	}

	public void setIndex(final int index) {
		this.index = index;
	}

	public T getComparable() {
		return comparable;
	}

	public void setComparable(final T comparable) {
		this.comparable = comparable;
	}
	
	public static Comparator<ComparableIndexItem<String>> NATURAL_ORDER = new Comparator<ComparableIndexItem<String>>() {
		@Override
		public int compare(final ComparableIndexItem<String> o1, final ComparableIndexItem<String> o2) {
			return o1.getComparable().compareTo(o2.getComparable());
		}
	};
	
	public static Comparator<ComparableIndexItem<String>> NUMERIC_ORDER = new Comparator<ComparableIndexItem<String>>() {
		@Override
		public int compare(final ComparableIndexItem<String> o1, final ComparableIndexItem<String> o2) {
			return Integer.valueOf(o1.getComparable()).compareTo(Integer.valueOf(o2.getComparable()));
		}
	};
	
	@Override
	public int compareTo(final T o) {
		return comparable.compareTo(o);
	}
}

