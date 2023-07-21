/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.shared.performance;

import java.util.List;

/**
 * This class summarizes the health status of a single IPerformanceProvider
 * instance, for use in performance summary messages.
 * 
 */
public class ProviderPerformanceSummary {

	/** Name of the performance provider. */
	private final String providerName;

	/** List of performance data objects supplied by the provider. */
	private final List<IPerformanceData> perfList;

	/** Overall health state of the provider. */
	private final HealthStatus overallHealth;

	/** Indicates whether the provider is currently throttling performance. */
	private final boolean isThrottling;

	/** Indicates whether the provider is currently generating backlog */
	private final boolean isBacklogging;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            the provider name
	 * @param perfData
	 *            the list of performance data objects from this provider; may
	 *            be null if the provider has only simple state
	 * @param status
	 *            the current health status of this provider
	 * @param backlog
	 *            indicates if the provider is generating backlog
	 * @param throttle
	 *            indicates of the provider is throttling
	 */
	public ProviderPerformanceSummary(String name,
			List<IPerformanceData> perfData, HealthStatus status,
			boolean backlog, boolean throttle) {
		assert name != null : "provider name cannot be null";
		assert status != null : "provider health status cannot be null";

		this.providerName = name;
		this.overallHealth = status;
		this.perfList = perfData;
		this.isThrottling = throttle;
		this.isBacklogging = backlog;
	}

	/**
	 * Gets the provider name.
	 * 
	 * @return name
	 */
	public String getProviderName() {
		return providerName;
	}

	/**
	 * Gets the list of performance data objects from the provider; may be null
	 * 
	 * @return list of IPerformanceData objects
	 */
	public List<IPerformanceData> getPerformanceData() {
		return perfList;
	}

	/**
	 * Gets the overall health status of the provider.
	 * 
	 * @return HealthStatus
	 */
	public HealthStatus getOverallHealth() {
		return overallHealth;
	}

	/**
	 * Indicates whether the provider is currently throttling performance.
	 * 
	 * @return true if throttling, false if not
	 */
	public boolean isThrottling() {
		return isThrottling;
	}

	/**
	 * Indicates whether the provider is currently generating application
	 * backlog.
	 * 
	 * @return true if backlogging, false if not
	 */
	public boolean isBacklogging() {
		return isBacklogging;
	}

	/**
	 * Gets a summary string describing the provider health, suitable for
	 * writing to a log file.
	 * 
	 * @return log string
	 */
	public String toLogString() {
		StringBuilder detail = new StringBuilder("Overall health of "
				+ this.providerName + " is " + this.overallHealth);
		if (this.isBacklogging || this.isThrottling) {
			detail.append(";");
		}
		if (isThrottling) {
			detail.append(" throttling");
		}
		if (isBacklogging && isThrottling) {
			detail.append(" and");
		}
		if (isBacklogging) {
			detail.append(" backlogging");
		}
		return detail.toString();
	}
}
