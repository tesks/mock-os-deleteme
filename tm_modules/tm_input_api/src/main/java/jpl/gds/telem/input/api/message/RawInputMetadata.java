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
package jpl.gds.telem.input.api.message;

import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.InvalidFrameCode;
import jpl.gds.telem.input.api.config.RawDataFormat;

/**
 * This class holds raw input metadata. Metadata are added by
 * <code>IRawInputConnection</code> classes from data obtained from input
 * sources or by <code>IRawStreamProcessor</code> from metadata within the data
 * streams
 * 
 */
public class RawInputMetadata implements Cloneable {
	private RawDataFormat dataFormat;
	private boolean isTurboData;
	/**
     * MPCS-7674 9/11/2015 - isPadded has been returned for SFDU packets but not frames (removed by MPCS-7014).
     * Is padded is a valid value for an SFDU packet CHDO, return its value to the rawInputMetadata object so it can be utilized
     * where necessary.
     */
	private boolean isPaddedData;
	private boolean isIdleData;
	private String turboEncodingRate;
	private IAccurateDateTime ert;
	private int dataLength;
	
	/**
	 * MPCS-7014 8/26/2015 - Added variable to report bit size of the transfer frame
	 * 
	 * the isPadded information is not part of raw SFDU frames. Currently, the
	 * bit size information contained within CHDO 69 is used for data size calculations of frames.
	 */
	private int bitSize;
	
	private Double bitRate;
	private IStationTelemInfo dsnInfo;
	private boolean isOutOfSyncData;
	private String outOfSyncReason;
	private boolean needInSyncMessage;
	private boolean isBadData;
	private InvalidFrameCode badFrameReason;
	private Boolean needsFrameSync;
	private boolean discard;

	/**
	 * Constructor
	 */
	public RawInputMetadata() {
		this.bitRate = null;
		this.ert = null;
		this.isTurboData = false;
		/**
         * MPCS-7674 9/11/2015 - isPadded has been returned for SFDU packets but not frames (removed by MPCS-7014).
         */
		this.isPaddedData = false;
		this.isOutOfSyncData = false;
		this.needInSyncMessage = false;
		this.needsFrameSync = null;
		this.discard = false;
	}

	/**
	 * Set whether or not the data should be discarded by the
	 * <code>IRawStreamProcessor</code> class
	 * 
	 * @param discard true if the data should be discarded, false if it should
	 *        be processed
	 */
	public void setDiscard(boolean discard) {
		this.discard = discard;
	}

	/**
	 * Indicates whether or not the data should be discarded by the
	 * <code>IRawStreamProcessor</code> class
	 * 
	 * @return true if the data should be discarded, false if it should be
	 *         processed
	 */
	public boolean shouldDiscard() {
		return discard;
	}

	/**
	 * For use with frames only. Set if the frame is bad and the reason it is
	 * bad.
	 * 
	 * @param isBad whether or not the frame is bad
	 * @param badReason the reason the frame is bad
	 */
	public void setIsBad(boolean isBad, InvalidFrameCode badReason) {
		this.isBadData = isBad;
		this.badFrameReason = badReason;
	}

	/**
	 * For use with frames only. Indicates if the frame is bad.
	 * 
	 * @return true if the frame is bad, false otherwise.
	 */
	public boolean isBad() {
		return this.isBadData;
	}

	/**
	 * Retrieves the reason the frame is bad, if applicable
	 * 
	 * @return an <code>InvalidFrameCode</code> object describing the reason the
	 *         frame is bad, or null if the frame is not bad
	 */
	public InvalidFrameCode getBadFrameReason() {
		return this.badFrameReason;
	}

	/**
	 * Indicates if this data is the first in sync data after an out of sync
	 * phase. If it is, then the <code>IRawDataProcessor</code> needs to send an
	 * <code>InSyncMessage</code>
	 * 
	 * @return true if the <code>IRawDataProcessor</code> needs to send an
	 *         <code>InSyncMessage</code>, false otherwise
	 */
	public boolean needsInSyncMessage() {
		return needInSyncMessage;
	}

	/**
	 * Set if this data is the first in sync data after an out of sync phase. If
	 * it is, then the <code>IRawDataProcessor</code> needs to send an
	 * <code>InSyncMessage</code>
	 * 
	 * @param needInSyncMessage if the <code>IRawDataProcessor</code> needs to
	 *        send an <code>InSyncMessage</code>, false otherwise
	 */
	public void setNeedInSyncMessage(boolean needInSyncMessage) {
		this.needInSyncMessage = needInSyncMessage;
	}

	/**
	 * Indicates if this data is out of sync
	 * 
	 * @return the true if the data is out of sync, false otherwise
	 */
	public boolean isOutOfSync() {
		return isOutOfSyncData;
	}

	/**
	 * Set if the data is out of sync
	 * 
	 * @param isOutOfSync if the data is out of sync
	 * @param reason the reason for the out of sync data, if known
	 */
	public void setOutOfSync(boolean isOutOfSync, String reason) {
		this.isOutOfSyncData = isOutOfSync;
		this.outOfSyncReason = reason;
	}

	/**
	 * Get the reason the frame is out of sync
	 * 
	 * @return the reason the frame is out of sync
	 */
	public String getOutOfSyncReason() {
		return this.outOfSyncReason;
	}

	/**
	 * Retrieve the DSNInfo object associated with the chunk of telemetry
	 * 
	 * @return the DSNInfo object associated with the chunk of telemetry
	 */
	public IStationTelemInfo getDsnInfo() {
		return dsnInfo;
	}

	/**
	 * Set the DSNInfo associated with the chunk of telemetry
	 * 
	 * @param dsnInfo the dsnInfo to set
	 */
	public void setDsnInfo(IStationTelemInfo dsnInfo) {
		this.dsnInfo = dsnInfo;
	}

	/**
	 * Set the data length in bytes
	 * 
	 * @param dataLength data length in bytes
	 */
	public void setDataLength(int dataLength) {
		this.dataLength = dataLength;
	}

	/**
	 * Retrieve the data length in bytes
	 * 
	 * @return the data length in bytes
	 */
	public int getDataLength() {
		return this.dataLength;
	}

	/**
	 * MPCS-7014 8/26/2015 - added set and getBitSize
	 * 
	 * Because this variable was added to the object, corresponding
	 * functions were needed manipulate these values.
	 */
	
	/**
	 * Set the data length in bits.
	 * Is not necessarily the size of
	 * dataLength*8.
	 * 
	 * @param bitSize bit size to set
	 */
	public void setBitSize(int bitSize) {
		this.bitSize = bitSize;
	}
	
	/**
	 * Retrieve the data length in bits
	 * 
	 * @return the data length in bits
	 */
	public int getBitSize() {
		return this.bitSize;
	}

	/**
	 * Indicates if the data is turbo encoded
	 * 
	 * @return true if data is turbo encoded, false otherwise
	 */
	public boolean isTurbo() {
		return this.isTurboData;
	}

	/**
	 * Retrieve the bit rate
	 * 
	 * @return the bit rate
	 */
	public Double getBitRate() {
		return bitRate;
	}

	/**
	 * Set the bit rate
	 * 
	 * @param bitRate the bit rate
	 */
	public void setBitRate(Double bitRate) {
		this.bitRate = bitRate;
	}

	/**
	 * Retrieve the turbo encoding rate
	 * 
	 * @return the turboEncodingRate
	 */
	public String getTurboEncodingRate() {
		return turboEncodingRate;
	}

	/**
	 * Set the turbo encoding rate
	 * 
	 * @param turboEncodingRate the turbo encoding rate
	 */
	public void setTurboEncodingRate(String turboEncodingRate) {
		if (turboEncodingRate != null) {
			this.turboEncodingRate = turboEncodingRate;
			this.isTurboData = true;
		}
	}

	/**
	 * Returns the ERT time of this chunk of telemetry.
	 * 
	 * @return IAccurateDateTime object representing the ERT of this chunk of
	 *         telemetry.
	 */
	public IAccurateDateTime getErt() {
		return ert;
	}

	/**
	 * Set the ERT
	 * 
	 * @param ert the ERT
	 */
	public void setErt(IAccurateDateTime ert) {
		this.ert = ert;
	}

	/**
     * MPCS-7674 9/11/2015 - isPadded has been returned for SFDU packets but not frames (removed by MPCS-7014).
     * Is padded is a valid value for an SFDU packet CHDO, return the isPadded and setPadded functios that allow
     * it to be set and retrieved.
     */
	/**
	 * Indicates if this the data is padded to make an even number of bytes
	 *
	 * @return true if the data payload is padded, false otherwise
	 */
	public Boolean isPadded() {
		return this.isPaddedData;
	}

	/**
	 * Set if the data is padded to make an even number of bytes
	 *
	 * @param padded if the data is padded
	 */
	public void setPadded(boolean padded) {
		this.isPaddedData = padded;
	}

	/**
	 * Get the data format of the data
	 * 
	 * @return the data format
	 */
	public RawDataFormat getDataFormat() {
		return dataFormat;
	}

	/**
	 * Set the data format of he data
	 * 
	 * @param dataFormat the data format of the data
	 */
	public void setDataFormat(RawDataFormat dataFormat) {
		this.dataFormat = dataFormat;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public RawInputMetadata clone() throws CloneNotSupportedException {
		final RawInputMetadata clone = (RawInputMetadata) super.clone();
		
		if(ert != null) {
			final IAccurateDateTime ertClone = new AccurateDateTime(ert.getTime(), ert.getNanoseconds());

			clone.setErt(ertClone);
		}

		if (this.needsFrameSync != null) {
			clone.setNeedsFrameSync(this.needsFrameSync);
		}

		if (this.bitRate != null) {
			clone.setBitRate(this.bitRate);
		}

		return clone;
	}

	/**
	 * Indicates if the data calls for frame synchronization.
	 * 
	 * @return true if the data needs to go through FrameSync, false otherwise
	 */
	public Boolean getNeedsFrameSync() {
		return needsFrameSync;
	}

	/**
	 * Set if the data calls for frame synchronization.
	 * 
	 * @param needsFrameSync if the data needs to go through FrameSync
	 */
	public void setNeedsFrameSync(Boolean needsFrameSync) {
		this.needsFrameSync = needsFrameSync;
	}

	/**
	 * Indicates if the frame is an idle frame
	 * 
	 * @return true if the frame is an idle frame, false otherwise
	 */
	public boolean isIdle() {
		return isIdleData;
	}

	/**
	 * Set if the frame is an idle frame
	 * 
	 * @param isIdle if the frame is an idle frame
	 */
	public void setIdle(boolean isIdle) {
		this.isIdleData = isIdle;
	}
}
