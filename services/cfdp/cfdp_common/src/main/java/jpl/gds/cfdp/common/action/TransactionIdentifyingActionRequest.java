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

package jpl.gds.cfdp.common.action;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jpl.gds.cfdp.common.GenericRequest;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionIdentifyingActionRequest extends GenericRequest {

	private ETransactionIdentificationType transactionIdentificationType;
	private byte serviceClass;
	private long remoteEntityId;
	private long transactionEntityId;
	private List<List<Long>> transactionSequenceNumbers;

	/**
	 * @return the transactionIdentificationType
	 */
	public ETransactionIdentificationType getTransactionIdentificationType() {
		return transactionIdentificationType;
	}

	/**
	 * @param transactionIdentificationType
	 *            the transactionIdentificationType to set
	 */
	public void setTransactionIdentificationType(ETransactionIdentificationType transactionIdentificationType) {
		this.transactionIdentificationType = transactionIdentificationType;
	}

	/**
	 * @return the serviceClass
	 */
	public byte getServiceClass() {
		return serviceClass;
	}

	/**
	 * @param serviceClass
	 *            the serviceClass to set
	 */
	public void setServiceClass(byte serviceClass) {
		this.serviceClass = serviceClass;
	}

	/**
	 * @return the remoteEntityId
	 */
	public long getRemoteEntityId() {
		return remoteEntityId;
	}

	/**
	 * @param remoteEntityId
	 *            the remoteEntityId to set
	 */
	public void setRemoteEntityId(long remoteEntityId) {
		this.remoteEntityId = remoteEntityId;
	}

	/**
	 * @return the transactionEntityId
	 */
	public long getTransactionEntityId() {
		return transactionEntityId;
	}

	/**
	 * @param transactionEntityId
	 *            the transactionEntityId to set
	 */
	public void setTransactionEntityId(long transactionEntityId) {
		this.transactionEntityId = transactionEntityId;
	}

	/**
	 * @return the transactionSequenceNumbers
	 */
	public List<List<Long>> getTransactionSequenceNumbers() {
		return transactionSequenceNumbers;
	}

	/**
	 * @param transactionSequenceNumbers
	 *            the transactionSequenceNumbers to set
	 */
	public void setTransactionSequenceNumbers(List<List<Long>> transactionSequenceNumbers) {
		this.transactionSequenceNumbers = transactionSequenceNumbers;
	}

}
