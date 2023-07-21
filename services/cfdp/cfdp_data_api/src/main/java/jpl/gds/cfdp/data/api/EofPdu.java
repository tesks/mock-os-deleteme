/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.data.api;

public class EofPdu implements IFileDirectivePdu {

	private ICfdpCondition condition;
	private long fileChecksum;
	private long fileSize;
//	private long 
	
	public ICfdpCondition getCondition() {
		return condition;
	}
	public void setCondition(ICfdpCondition condition) {
		this.condition = condition;
	}
	public long getFileChecksum() {
		return fileChecksum;
	}
	public void setFileChecksum(long fileChecksum) {
		this.fileChecksum = fileChecksum;
	}
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	
}
