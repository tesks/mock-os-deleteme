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

package jpl.gds.cfdp.common.stat;

import java.util.Enumeration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cfdp.engine.ampcs.OrderedProperties;
import jpl.gds.cfdp.common.AResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StatResponse extends AResponse {

	private OrderedProperties status;
	private OrderedProperties statistics;

	/**
	 * @return the status
	 */
	public OrderedProperties getStatus() {
		return status;
	}

	/**
	 * @param status
	 *            the status to set
	 */
	public void setStatus(OrderedProperties status) {
		this.status = status;
	}

	/**
	 * @return the statistics
	 */
	public OrderedProperties getStatistics() {
		return statistics;
	}

	/**
	 * @param statistics
	 *            the statistics to set
	 */
	public void setStatistics(OrderedProperties statistics) {
		this.statistics = statistics;
	}

	@Override
	public void printToSystemOut() {
		super.printToSystemOut();

		System.out.println("Status:");

		for (Enumeration<?> en = getStatus().keys(); en.hasMoreElements();) {
			String key = (String) en.nextElement();
			String val = (String) getStatus().get(key);
			System.out.println("     " + key + ": " + val);
		}

		System.out.println("Statistics:");

		for (Enumeration<?> en = getStatistics().keys(); en.hasMoreElements();) {
			String key = (String) en.nextElement();
			String val = (String) getStatistics().get(key);
			System.out.println("     " + key + ": " + val);
		}

	}

}