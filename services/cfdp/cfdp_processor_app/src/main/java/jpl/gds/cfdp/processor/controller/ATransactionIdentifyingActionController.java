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

package jpl.gds.cfdp.processor.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import cfdp.engine.ampcs.TransIdUtil;
import jpl.gds.cfdp.common.action.EActionCommandType;
import jpl.gds.cfdp.common.action.TransactionIdentifyingActionRequest;
import jpl.gds.cfdp.common.action.TransactionsActionAppliedResponse;
import jpl.gds.cfdp.processor.ResponseFactory;
import jpl.gds.cfdp.processor.action.disruptor.ActionEvent;
import jpl.gds.cfdp.processor.action.disruptor.ActionRingBufferManager;
import jpl.gds.cfdp.processor.controller.action.InternalActionRequest;

@DependsOn("configurationManager")
public abstract class ATransactionIdentifyingActionController extends AActionController {

	@Autowired
	private ResponseFactory responseFactory;

	@Autowired
	private ActionRingBufferManager actionDisruptor;

	protected ResponseEntity<Object> handlePost(EActionCommandType type, TransactionIdentifyingActionRequest req) {
		logAndPublishRequest(type, req);
		InternalActionRequest<TransactionIdentifyingActionRequest> iar = new InternalActionRequest<>(type, req, getRequestId());
		actionDisruptor.getRingBuffer().publishEvent(ActionEvent::translateTransactionIdentifyingAction, iar);
		ResponseEntity<Object> resp = getResultAndHandleErrors(type, iar.getResponseQueue());

		if (resp != null) {
			return resp;
		} else {
			return createAcceptedOrOkResponse(type);
		}

	}

	@Override
	protected ResponseEntity<Object> createAcceptedOrOkResponse(EActionCommandType type) {

		if (getResult() == null) {
			TransactionsActionAppliedResponse resp = responseFactory
					.createTransactionsActionAppliedResponse(getRequestId(), type.name()
							+ " action was accepted but timed out waiting for final result (action may complete later)");
			logAndPublishResult(false, resp.getMessage());
			return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
		} else {
			TransactionsActionAppliedResponse resp = responseFactory.createTransactionsActionAppliedResponse(
					getRequestId(), getResult().getTransIdsActionSuccessfullyApplied());

			String flattenedTxId = "";
			String delim = "";

			for (List<Long> txId : resp.getTransactionIdsActionApplied()) {
				flattenedTxId += delim + TransIdUtil.INSTANCE.toString(txId.get(0), txId.get(1));
				delim = ",";
			}

			logAndPublishResult(false, flattenedTxId);
			return ResponseEntity.status(HttpStatus.OK).body(resp);
		}

	}

}
