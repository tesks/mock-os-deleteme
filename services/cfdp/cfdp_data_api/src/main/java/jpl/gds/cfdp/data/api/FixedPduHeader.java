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

/**
 * Class FixedPduHeader
 */
public class FixedPduHeader {

	private byte version;
	private ECfdpPduType type;
	private ECfdpPduDirection direction;
	private ECfdpTransmissionMode transmissionMode;
	private boolean crcFlagPresent;
	private short dataFieldLength;
	private byte entityIdLength;
	private byte transactionSequenceNumberLength;
	private long sourceEntityId;
	private long transactionSequenceNumber;
	private long destinationEntityId;

	@Override
	public String toString() {
		return "Version=" + version + ",  Type=" + type.name() + ", Direction=" + direction.name() + ", TxMode="
				+ transmissionMode.name() + ", CrcFlagPresent=" + crcFlagPresent + ", DataFieldLength="
				+ dataFieldLength + ", EntityIdLength=" + entityIdLength + ", TxSeqNumLength="
				+ transactionSequenceNumberLength + ", SourceEntity=" + Long.toUnsignedString(sourceEntityId)
				+ ", TxSeqNum=" + Long.toUnsignedString(transactionSequenceNumber) + ", DestinationEntity="
				+ Long.toUnsignedString(destinationEntityId);
	}

	public byte getVersion() {
		return version;
	}

	public void setVersion(byte version) {
		this.version = version;
	}

	public ECfdpPduType getType() {
		return type;
	}

	public void setType(ECfdpPduType type) {
		this.type = type;
	}

	public ECfdpPduDirection getDirection() {
		return direction;
	}

	public void setDirection(ECfdpPduDirection direction) {
		this.direction = direction;
	}

	public ECfdpTransmissionMode getTransmissionMode() {
		return transmissionMode;
	}

	public void setTransmissionMode(ECfdpTransmissionMode transmissionMode) {
		this.transmissionMode = transmissionMode;
	}

	public boolean isCrcFlagPresent() {
		return crcFlagPresent;
	}

	public void setCrcFlagPresent(boolean crcFlagPresent) {
		this.crcFlagPresent = crcFlagPresent;
	}

	public short getDataFieldLength() {
		return dataFieldLength;
	}

	public void setDataFieldLength(short dataFieldLength) {
		this.dataFieldLength = dataFieldLength;
	}

	public byte getEntityIdLength() {
		return entityIdLength;
	}

	public void setEntityIdLength(byte entityIdLength) {
		this.entityIdLength = entityIdLength;
	}

	public byte getTransactionSequenceNumberLength() {
		return transactionSequenceNumberLength;
	}

	public void setTransactionSequenceNumberLength(byte transactionSequenceNumberLength) {
		this.transactionSequenceNumberLength = transactionSequenceNumberLength;
	}

	public long getSourceEntityId() {
		return sourceEntityId;
	}

	public void setSourceEntityId(long sourceEntityId) {
		this.sourceEntityId = sourceEntityId;
	}

	public long getTransactionSequenceNumber() {
		return transactionSequenceNumber;
	}

	public void setTransactionSequenceNumber(long transactionSequenceNumber) {
		this.transactionSequenceNumber = transactionSequenceNumber;
	}

	public long getDestinationEntityId() {
		return destinationEntityId;
	}

	public void setDestinationEntityId(long destinationEntityId) {
		this.destinationEntityId = destinationEntityId;
	}

}
