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

package jpl.gds.cfdp.processor.action.disruptor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import cfdp.engine.ampcs.RequestResult;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.common.action.EActionCommandType;
import jpl.gds.cfdp.common.action.ETransactionIdentificationType;
import jpl.gds.cfdp.common.action.TransactionIdentifyingActionRequest;
import jpl.gds.cfdp.common.action.ingest.EIngestSource;
import jpl.gds.cfdp.common.action.ingest.IngestActionRequest;
import jpl.gds.cfdp.common.action.put.EBoolean;
import jpl.gds.cfdp.common.action.put.PutActionRequest;
import jpl.gds.cfdp.processor.controller.action.InternalActionRequest;
import jpl.gds.shared.types.UnsignedLong;

/**
 * {@code ActionEvent} is used by the disruptor queue for carrying CFDP action requests from the client to the
 * CFDP Processor.
 *
 */
public class ActionEvent {

	private EActionCommandType actionCommandType;

	// Put fields
	private long destinationEntity;
	private String sourceFileName;
	private byte serviceClass;
	private String destinationFileName;
	private byte[] uploadedFile;
	private UnsignedLong sessionKey;
	private Collection<String> messagesToUser;

	// Transaction ID fields
	private ETransactionIdentificationType transactionIdentificationType;
	private long remoteEntityId;
	private long transactionEntityId;
	private List<List<Long>> transactionSequenceNumbers;

	// Ingest PDUs fields
	private EIngestSource ingestSource;
	private String ingestFileName;

	// Response blocking queue
	private BlockingQueue<RequestResult> responseQueue;

	// Action request ID
	private String requestId;

	void clear() {
		actionCommandType = null;

		destinationEntity = -1;
		sourceFileName = null;
		serviceClass = -1;
		destinationFileName = null;
		uploadedFile = null;
		sessionKey = null;
		messagesToUser = null;

		transactionIdentificationType = null;
		remoteEntityId = -1;
		transactionEntityId = -1;
		transactionSequenceNumbers = null;

		ingestSource = null;
		ingestFileName = null;

		responseQueue = null;

		requestId = null;
	}

	public static void translatePut(final ActionEvent event, final long sequence,
			final InternalActionRequest<PutActionRequest> internalReq) {
		event.clear();
		event.setActionCommandType(internalReq.getAction());
		event.setDestinationEntity(internalReq.getOriginalRequest().getDestinationEntity());
		event.setSourceFileName(internalReq.getOriginalRequest().getSourceFileName());
		event.setServiceClass(internalReq.getOriginalRequest().getServiceClass());
		event.setDestinationFileName(internalReq.getOriginalRequest().getDestinationFileName());
		event.setUploadedFile(internalReq.getOriginalRequest().getUploadFile());
		event.setSessionKey(internalReq.getOriginalRequest().getSessionKey());
		event.setMessagesToUser(internalReq.getOriginalRequest().getMessagesToUser());
		event.setResponseQueue(internalReq.getResponseQueue());
		event.setRequestId(internalReq.getRequestId());
	}

	public static void translateTransactionIdentifyingAction(final ActionEvent event, final long sequence,
			final InternalActionRequest<TransactionIdentifyingActionRequest> internalReq) {
		event.clear();
		event.setActionCommandType(internalReq.getAction());
		event.setRemoteEntityId(internalReq.getOriginalRequest().getRemoteEntityId());
		event.setServiceClass(internalReq.getOriginalRequest().getServiceClass());
		event.setTransactionEntityId(internalReq.getOriginalRequest().getTransactionEntityId());
		event.setTransactionIdentificationType(internalReq.getOriginalRequest().getTransactionIdentificationType());
		event.setTransactionSequenceNumbers(internalReq.getOriginalRequest().getTransactionSequenceNumbers());
		event.setResponseQueue(internalReq.getResponseQueue());
		event.setRequestId(internalReq.getRequestId());
	}

	public static void translateGenericAction(final ActionEvent event, final long sequence,
			final InternalActionRequest<GenericRequest> internalReq) {
		event.clear();
		event.setActionCommandType(internalReq.getAction());
		event.setResponseQueue(internalReq.getResponseQueue());
		event.setRequestId(internalReq.getRequestId());
	}

	public static void translateIngest(final ActionEvent event, final long sequence,
			final InternalActionRequest<IngestActionRequest> internalReq) {
		event.clear();
		event.setActionCommandType(internalReq.getAction());
		event.setIngestSource(internalReq.getOriginalRequest().getIngestSource());
		event.setIngestFileName(internalReq.getOriginalRequest().getIngestFileName());
		event.setResponseQueue(internalReq.getResponseQueue());
		event.setRequestId(internalReq.getRequestId());
	}

	public static void copyIngest(final ActionEvent toEvent, final long sequence, final ActionEvent fromEvent) {
		toEvent.clear();
		toEvent.setActionCommandType(fromEvent.getActionCommandType());
		toEvent.setIngestSource(fromEvent.getIngestSource());
		toEvent.setIngestFileName(fromEvent.getIngestFileName());
		toEvent.setResponseQueue(fromEvent.getResponseQueue());
		toEvent.setRequestId(fromEvent.getRequestId());
	}

	/**
	 * @return the actionCommandType
	 */
	public EActionCommandType getActionCommandType() {
		return actionCommandType;
	}

	/**
	 * @param actionCommandType
	 *            the actionCommandType to set
	 */
	public void setActionCommandType(final EActionCommandType actionCommandType) {
		this.actionCommandType = actionCommandType;
	}

	/**
	 * @return the destinationEntity
	 */
	public long getDestinationEntity() {
		return destinationEntity;
	}

	/**
	 * @param destinationEntity
	 *            the destinationEntity to set
	 */
	public void setDestinationEntity(final long destinationEntity) {
		this.destinationEntity = destinationEntity;
	}

	/**
	 * @return the sourceFileName
	 */
	public String getSourceFileName() {
		return sourceFileName;
	}

	/**
	 * @param sourceFileName
	 *            the sourceFileName to set
	 */
	public void setSourceFileName(final String sourceFileName) {
		this.sourceFileName = sourceFileName;
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
	public void setServiceClass(final byte serviceClass) {
		this.serviceClass = serviceClass;
	}

	/**
	 * @return the destinationFileName
	 */
	public String getDestinationFileName() {
		return destinationFileName;
	}

	/**
	 * @param destinationFileName
	 *            the destinationFileName to set
	 */
	public void setDestinationFileName(final String destinationFileName) {
		this.destinationFileName = destinationFileName;
	}

	/**
	 * @return the uploadedFile
	 */
	public byte[] getUploadedFile() {
		return this.uploadedFile;
	}

	/**
	 * @param uploadedFile the uploadedFile to set
	 */
	public void setUploadedFile(final byte[] uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	/**
	 * @return the sessionKey
	 */
	public UnsignedLong getSessionKey() {
		return sessionKey;
	}

	/**
	 * @param sessionKey
	 *            the sessionKey to set
	 */
	public void setSessionKey(final UnsignedLong sessionKey) {
		this.sessionKey = sessionKey;
	}

	/**
	 * @return Messages to User
	 */
	public Collection<String> getMessagesToUser() {
		return messagesToUser;
	}

	/**
	 * @param messagesToUser Messages to User to set
	 */
	public void setMessagesToUser(final Collection<String> messagesToUser) {
		this.messagesToUser = messagesToUser;
	}

	/**
	 * @return the ingestSource
	 */
	public EIngestSource getIngestSource() {
		return ingestSource;
	}

	/**
	 * @param ingestSource
	 *            the ingestSource to set
	 */
	public void setIngestSource(final EIngestSource ingestSource) {
		this.ingestSource = ingestSource;
	}

	/**
	 * @return the ingestFileName
	 */
	public String getIngestFileName() {
		return ingestFileName;
	}

	/**
	 * @param ingestFileName
	 *            the ingestFileName to set
	 */
	public void setIngestFileName(final String ingestFileName) {
		this.ingestFileName = ingestFileName;
	}

	/**
	 * @return the responseQueue
	 */
	public BlockingQueue<RequestResult> getResponseQueue() {
		return responseQueue;
	}

	/**
	 * @param responseQueue
	 *            the responseQueue to set
	 */
	public void setResponseQueue(final BlockingQueue<RequestResult> responseQueue) {
		this.responseQueue = responseQueue;
	}

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
	public void setTransactionIdentificationType(final ETransactionIdentificationType transactionIdentificationType) {
		this.transactionIdentificationType = transactionIdentificationType;
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
	public void setRemoteEntityId(final long remoteEntityId) {
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
	public void setTransactionEntityId(final long transactionEntityId) {
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
	public void setTransactionSequenceNumbers(final List<List<Long>> transactionSequenceNumbers) {
		this.transactionSequenceNumbers = transactionSequenceNumbers;
	}

	/**
	 * @return the requestId
	 */
	public String getRequestId() {
		return requestId;
	}

	/**
	 * @param requestId
	 *            the requestId to set
	 */
	public void setRequestId(final String requestId) {
		this.requestId = requestId;
	}

}
