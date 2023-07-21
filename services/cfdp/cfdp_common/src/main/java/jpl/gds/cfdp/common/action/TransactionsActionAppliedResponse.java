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
public class TransactionsActionAppliedResponse extends GenericActionResponse {

	private List<List<Long>> transactionIdsActionApplied;

	/**
	 * @return the transactionIdsActionApplied
	 */
	public List<List<Long>> getTransactionIdsActionApplied() {
		return transactionIdsActionApplied;
	}

	/**
	 * @param transactionIdsActionApplied
	 *            the transactionIdsActionApplied to set
	 */
	public void setTransactionIdsActionApplied(List<List<Long>> transactionIdsActionApplied) {
		this.transactionIdsActionApplied = transactionIdsActionApplied;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.cfdp.common.GenericActionResponse#printToSystemOut()
	 */
	@Override
	public void printToSystemOut() {
		super.printToSystemOut();

		if (getTransactionIdsActionApplied() != null) {

			if (!getTransactionIdsActionApplied().isEmpty()) {
				System.out.println("Action applied to following transactions...");

				for (List<Long> transactionId : getTransactionIdsActionApplied()) {
					System.out.print(Long.toUnsignedString(transactionId.get(0).longValue()));
					System.out.print(":");
					System.out.println(Long.toUnsignedString(transactionId.get(1).longValue()));
				}

			} else {
				System.out.println(actionNotAppliedToAnyTransactionMessage);
			}

		}

	}

}