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

package jpl.gds.cfdp.common.action.report;

import java.util.Enumeration;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cfdp.engine.ampcs.OrderedProperties;
import jpl.gds.cfdp.common.GenericActionResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportActionResponse extends GenericActionResponse {

	private Map<String, OrderedProperties> liveTransactionsReportMap;
	private Map<String, OrderedProperties> finishedTransactionsReportMap;

	/**
	 * @return the liveTransactionsReportMap
	 */
	public Map<String, OrderedProperties> getLiveTransactionsReportMap() {
		return liveTransactionsReportMap;
	}

	/**
	 * @param liveTransactionsReportMap
	 *            the liveTransactionsReportMap to set
	 */
	public void setLiveTransactionsReportMap(Map<String, OrderedProperties> liveTransactionsReportMap) {
		this.liveTransactionsReportMap = liveTransactionsReportMap;
	}

	/**
	 * @return the finishedTransactionsReportMap
	 */
	public Map<String, OrderedProperties> getFinishedTransactionsReportMap() {
		return finishedTransactionsReportMap;
	}

	/**
	 * @param finishedTransactionsReportMap
	 *            the finishedTransactionsReportMap to set
	 */
	public void setFinishedTransactionsReportMap(Map<String, OrderedProperties> finishedTransactionsReportMap) {
		this.finishedTransactionsReportMap = finishedTransactionsReportMap;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.cfdp.common.GenericActionResponse#printToSystemOut()
	 */
	@Override
	public void printToSystemOut() {
		super.printToSystemOut();
		boolean noLiveTransactionReports = false;
		boolean noFinishedTransactionReports = false;

		if (getLiveTransactionsReportMap() != null) {

			if (!getLiveTransactionsReportMap().isEmpty()) {

				getLiveTransactionsReportMap().entrySet().stream().forEachOrdered(e -> {
					System.out.println(
							"--------------------------------------------------------------------------------");
					System.out.println("Live transaction " + e.getKey() + ":");

					// Iterating this way takes advantage of the keys ordering
					for (Enumeration<?> en = e.getValue().keys(); en.hasMoreElements();) {
						String key = (String) en.nextElement();
						String val = (String) e.getValue().get(key);
						System.out.println("     " + key + ": " + val);
					}

				});

			} else {
				noLiveTransactionReports = true;
			}

		}

		if (getFinishedTransactionsReportMap() != null) {

			if (!getFinishedTransactionsReportMap().isEmpty()) {

				getFinishedTransactionsReportMap().entrySet().stream().forEachOrdered(e -> {
					System.out.println(
							"--------------------------------------------------------------------------------");
					System.out.println("Finished transaction " + e.getKey() + ":");

					// Iterating this way takes advantage of the keys ordering
					for (Enumeration<?> en = e.getValue().keys(); en.hasMoreElements();) {
						String key = (String) en.nextElement();
						String val = (String) e.getValue().get(key);
						System.out.println("     " + key + ": " + val);
					}

				});

			} else {
				noFinishedTransactionReports = true;
			}

		}

		if (noLiveTransactionReports && noFinishedTransactionReports) {
			System.out.println("No transactions to report");
		}

	}

}