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

package jpl.gds.cfdp.processor.controller.action;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import cfdp.engine.ampcs.RequestResult;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.cfdp.common.action.EActionCommandType;

public class InternalActionRequest<T extends GenericRequest> {

	private EActionCommandType action;
	private T originalRequest;
	private BlockingQueue<RequestResult> responseQueue;
	private String requestId;

	public InternalActionRequest(final EActionCommandType action,
								 final T originalRequest,
								 final BlockingQueue<RequestResult> responseQueue,
								 final String requestId) {
		super();
		this.action = action;
		this.originalRequest = originalRequest;
		this.responseQueue = responseQueue;
		this.requestId = requestId;
	}

	public InternalActionRequest(final EActionCommandType action, final T originalRequest, final String requestId) {
		this(action, originalRequest, new LinkedBlockingQueue<RequestResult>(1), requestId);
	}

	/**
	 * @return the action
	 */
	public EActionCommandType getAction() {
		return action;
	}

	/**
	 * @param action
	 *            the action to set
	 */
	public void setAction(final EActionCommandType action) {
		this.action = action;
	}

	/**
	 * @return the originalRequest
	 */
	public T getOriginalRequest() {
		return originalRequest;
	}

	/**
	 * @param originalRequest
	 *            the originalRequest to set
	 */
	public void setOriginalRequest(final T originalRequest) {
		this.originalRequest = originalRequest;
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