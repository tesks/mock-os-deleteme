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
package jpl.gds.tm.service.api.frame;

import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * The EncodingSummaryRecord stores a summary of the encoding for a frame.
 * 
 */

public class EncodingSummaryRecord {
	private EncodingType type;
	private long instanceCount;
	private long badFrameCount;
	private long errorCount;
	private IAccurateDateTime lastErt;
	private long lastSeq;
	private int vcid;

	/**
	 * A basic constructor
	 */
	public EncodingSummaryRecord() {}
	
	/**
	 * A constructor that takes several parameters to set basic variables.
	 * @param type The encoding type to set.
	 * @param vcid The vcid to set.
	 * @param count The count to set.
	 */
	public EncodingSummaryRecord(EncodingType type, int vcid, int count) {
		this.type = type;
		this.vcid = vcid;
		this.instanceCount = count;
	}

//	/**
//	 * This increments the count of encoded frames.
//	 * @param addCount This is the count to add
//	 * @param ert This is the current ERT  
//	 * @param seq This is the sequence
//	 * @param errorCount This is the errorCount
//	 * @param isBad This is whether or not the frame is bad.
//	 */
//	public void increment(long addCount, IAccurateDateTime ert, long seq, long errorCount, boolean isBad) {
//		instanceCount += addCount;
//		lastErt = ert;
//		lastSeq = seq;
//		if (isBad) {
//			badFrameCount++;
//		}
//	}

	/**
	 * This gets the encoding type
	 * @return The encoding type
	 */
	public EncodingType getType() {
		return type;
	}

	/**
	 * This sets the encoding type
	 * @param type The encoding type to set
	 */
	public void setType(EncodingType type) {
		this.type = type;
	}

	/**
	 * This returns the instance count
	 * @return The instance count.
	 */
	public long getInstanceCount() {
		return instanceCount;
	}

	/**
	 * This sets the instance count
	 * @param instanceCount This is the instance count to set.
	 */
	public void setInstanceCount(long instanceCount) {
		this.instanceCount = instanceCount;
	}

	/**
	 * This gets the bad frame count
	 * @return The bad frame count
	 */
	public long getBadFrameCount() {
		return badFrameCount;
	}

	/**
	 * This sets the bad frame count
	 * @param badFrameCount The bad frame count to set.
	 */
	public void setBadFrameCount(long badFrameCount) {
		this.badFrameCount = badFrameCount;
	}

	/**
	 * This gets the error count.
	 * @return The error count.
	 */
	public long getErrorCount() {
		return errorCount;
	}

	/**
	 * This sets the error count.
	 * @param errorCount The error count to set
	 */
	public void setErrorCount(long errorCount) {
		this.errorCount = errorCount;
	}

	/**
	 * This gets the last ert time.
	 * @return The last ert time.
	 */
	public IAccurateDateTime getLastErt() {
		return lastErt;
	}

	/**
	 * This gets the last ert as a string.
	 * @return The last ert as a string.
	 */
	public String getLastErtStr() {
		if (lastErt == null) {
			return "";
		} else {
			return lastErt.getFormattedErt(true);
		}
	}
	
	/**
	 * This sets the last ert.
	 * @param lastErt This is the last ert time to set.
	 */
	public void setLastErt(IAccurateDateTime lastErt) {
		this.lastErt = lastErt;
	}

	/**
	 * This gets the last sequence
	 * @return The last sequence
	 */
	public long getLastSequence() {
		return lastSeq;
	}

	/**
	 * This sets the Last Sequence
	 * @param lastSeq The last sequence to set.
	 */
	public void setLastSequence(long lastSeq) {
		this.lastSeq = lastSeq;
	}

	/**
	 * This gets the vcid
	 * @return The vcid
	 */
	public int getVcid() {
		return vcid;
	}

	/**
	 * This sets the vcid
	 * @param vcid The vcid to set.
	 */
	public void setVcid(int vcid) {
		this.vcid = vcid;
	}

}
