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

package jpl.gds.cfdp.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import cfdp.engine.TransID;
import cfdp.engine.ampcs.OrderedProperties;
import jpl.gds.cfdp.common.AResponse;
import jpl.gds.cfdp.common.GenericActionResponse;
import jpl.gds.cfdp.common.GenericMessageResponse;
import jpl.gds.cfdp.common.GenericPropertiesMapResponse;
import jpl.gds.cfdp.common.GenericPropertiesResponse;
import jpl.gds.cfdp.common.action.MessageListResponse;
import jpl.gds.cfdp.common.action.TransactionsActionAppliedResponse;
import jpl.gds.cfdp.common.action.put.PutActionResponse;
import jpl.gds.cfdp.common.action.report.ReportActionResponse;
import jpl.gds.cfdp.common.stat.StatResponse;
import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.gsfc.util.GsfcUtil;

/**
 * Class ResponseFactory
 *
 */
@Service
@DependsOn("configurationManager")
public class ResponseFactory {

	@Autowired
	private ConfigurationManager configurationManager;

	@Autowired
	private GsfcUtil gsfcUtil;

	public GenericActionResponse createGenericActionResponse(String requestId, String message) {
		GenericActionResponse resp = new GenericActionResponse();
		setCfdpProcessorInfo(resp);
		resp.setRequestId(requestId);
		resp.setMessage(message);
		return resp;
	}

	public PutActionResponse createPutActionResponse(String requestId, String message) {
		PutActionResponse resp = new PutActionResponse();
		setCfdpProcessorInfo(resp);
		resp.setRequestId(requestId);
		resp.setMessage(message);
		return resp;
	}

	public PutActionResponse createPutActionResponse(String requestId, TransID newTransId) {
		PutActionResponse resp = new PutActionResponse();
		setCfdpProcessorInfo(resp);
		resp.setRequestId(requestId);
		List<Long> newTransactionId = new ArrayList<>(2);
		newTransactionId.add(gsfcUtil.convertEntityId(newTransId.getSource()));
		newTransactionId.add((long) newTransId.getNumber());
		resp.setNewTransactionId(newTransactionId);
		return resp;
	}

	public TransactionsActionAppliedResponse createTransactionsActionAppliedResponse(String requestId, String message) {
		TransactionsActionAppliedResponse resp = new TransactionsActionAppliedResponse();
		setCfdpProcessorInfo(resp);
		resp.setRequestId(requestId);
		resp.setMessage(message);
		return resp;
	}

	public TransactionsActionAppliedResponse createTransactionsActionAppliedResponse(String requestId,
			List<TransID> transIdsActionApplied) {
		TransactionsActionAppliedResponse resp = new TransactionsActionAppliedResponse();
		setCfdpProcessorInfo(resp);
		resp.setRequestId(requestId);
		List<List<Long>> transactionIdsActionApplied = new ArrayList<>(transIdsActionApplied.size());

		for (TransID transId : transIdsActionApplied) {
			List<Long> transactionId = new ArrayList<>(2);
			transactionId.add(gsfcUtil.convertEntityId(transId.getSource()));
			transactionId.add((long) transId.getNumber());
			transactionIdsActionApplied.add(transactionId);
		}

		resp.setTransactionIdsActionApplied(transactionIdsActionApplied);
		return resp;
	}

	public MessageListResponse createMessageListResponse(String requestId, String message) {
		MessageListResponse resp = new MessageListResponse();
		setCfdpProcessorInfo(resp);
		resp.setRequestId(requestId);
		resp.setMessage(message);
		return resp;
	}

	public MessageListResponse createMessageListResponse(String requestId, List<String> messageList) {
		MessageListResponse resp = new MessageListResponse();
		setCfdpProcessorInfo(resp);
		resp.setRequestId(requestId);
		resp.setMessageList(messageList);
		return resp;
	}

	public ReportActionResponse createReportActionResponse(String requestId, String message) {
		ReportActionResponse resp = new ReportActionResponse();
		setCfdpProcessorInfo(resp);
		resp.setRequestId(requestId);
		resp.setMessage(message);
		return resp;
	}

	public ReportActionResponse createReportActionResponse(String requestId, Map<String, OrderedProperties> liveTransactionsReportMap,
			Map<String, OrderedProperties> finishedTransactionsReportMap) {
		ReportActionResponse resp = new ReportActionResponse();
		setCfdpProcessorInfo(resp);
		resp.setRequestId(requestId);
		resp.setLiveTransactionsReportMap(liveTransactionsReportMap);
		resp.setFinishedTransactionsReportMap(finishedTransactionsReportMap);
		return resp;
	}

	private void setCfdpProcessorInfo(AResponse resp) {
		resp.setCfdpProcessorPort(configurationManager.getCurrentActualPort());
		resp.setCfdpProcessorInstanceId(configurationManager.getInstanceId());
	}

	public String createActionErrorText(String requestId, String errorMessage) {
		GenericActionResponse resp = new GenericActionResponse();
		resp.setCfdpProcessorPort(configurationManager.getCurrentActualPort());
		resp.setCfdpProcessorInstanceId(configurationManager.getInstanceId());
		StringBuilder sb = new StringBuilder();
		sb.append(resp.getFullCfdpProcessorIdentification());
		sb.append("\n");
		sb.append("Request ID: ");
		sb.append(requestId);
		sb.append("\n");
		sb.append("Error Message: ");
		sb.append(errorMessage);
		sb.append("\n");
		return sb.toString();
	}

	public GenericPropertiesResponse createGenericPropertiesResponse(Properties properties) {
		GenericPropertiesResponse resp = new GenericPropertiesResponse();
		setCfdpProcessorInfo(resp);
		resp.setProperties(properties);
		return resp;
	}

	public GenericPropertiesMapResponse createGenericPropertiesMapResponse(Map<String, Properties> propertiesMap) {
		GenericPropertiesMapResponse resp = new GenericPropertiesMapResponse();
		setCfdpProcessorInfo(resp);
		resp.setPropertiesMap(propertiesMap);
		return resp;
	}

	public String createNonActionErrorText(String errorMessage) {
		GenericMessageResponse resp = new GenericMessageResponse();
		resp.setCfdpProcessorPort(configurationManager.getCurrentActualPort());
		resp.setCfdpProcessorInstanceId(configurationManager.getInstanceId());
		StringBuilder sb = new StringBuilder();
		sb.append(resp.getFullCfdpProcessorIdentification());
		sb.append("\n");
		sb.append("Error Message: ");
		sb.append(errorMessage);
		sb.append("\n");
		return sb.toString();
	}

	public StatResponse createStatResponse(OrderedProperties status, OrderedProperties statistics) {
		StatResponse resp = new StatResponse();
		setCfdpProcessorInfo(resp);
		resp.setStatus(status);
		resp.setStatistics(statistics);
		return resp;
	}

}