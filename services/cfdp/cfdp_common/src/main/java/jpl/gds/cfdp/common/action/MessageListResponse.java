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

import jpl.gds.cfdp.common.GenericActionResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageListResponse extends GenericActionResponse {

	private List<String> messageList;

	/**
	 * @return the messageList
	 */
	public List<String> getMessageList() {
		return messageList;
	}

	/**
	 * @param messageList
	 *            the messageList to set
	 */
	public void setMessageList(List<String> messageList) {
		this.messageList = messageList;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.cfdp.common.GenericActionResponse#printToSystemOut()
	 */
	@Override
	public void printToSystemOut() {
		super.printToSystemOut();

		if (getMessageList() != null) {

			if (!getMessageList().isEmpty()) {

				for (String m : getMessageList()) {
					System.out.println(m);
				}

			} else {
				System.out.println(actionNotAppliedToAnyTransactionMessage);
			}

		}

	}

}