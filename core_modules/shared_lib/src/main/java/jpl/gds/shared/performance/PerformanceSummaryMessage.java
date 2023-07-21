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

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.Message;

/**
 * An internal message class that summarizes application performance, as
 * gathered from multiple IPerformanceProviders.
 * 
*/
public class PerformanceSummaryMessage extends Message {

	/** The map of provider performance data objects, keyed by provider name. */
	private final Map<String, ProviderPerformanceSummary> performanceData;

	/** The current heap performance object */
	private HeapPerformanceData heapStatus;

	/** Indicates whether any provider is throttling the application */
	private final boolean isThrottling;

	/** Indicates whether any provider is generating application backlog */
	private final boolean isBacklogging;

	/** Overall health status, taking into account all performance providers */
	private HealthStatus overallHealth = HealthStatus.NONE;

	/**
	 * Constructor.
	 * 
	 * @param perfData
	 *            the map of provider names to provider performance summary
	 *            objects
	 * @param status
	 *            the overall health status
	 * @param backlog
	 *            indicates whether any provider is generating application
	 *            backlog
	 * @param throttle
	 *            indicates whether any provider is throttling the application
	 */
	public PerformanceSummaryMessage(
			final Map<String, ProviderPerformanceSummary> perfData,
			final HealthStatus status, final boolean backlog, final boolean throttle) {
        super(CommonMessageType.PerformanceSummary, System.currentTimeMillis());
		this.overallHealth = status;
		this.performanceData = perfData;
		this.isThrottling = throttle;
		this.isBacklogging = backlog;
	}

	/**
	 * Gets the heap performance object. If none has been set, one will be
	 * created and populated with current heap status.
	 * 
	 * @return HeapPerformanceData
	 */
	public HeapPerformanceData getHeapStatus() {
		return heapStatus;
	}

	/**
	 * Gets the heap performance object. Setting a null value means that this
	 * object will automatically create a new heap performance data object in
	 * response to the next getHeapStatus() call.
	 * 
	 * @param heapStatus
	 *            the HeapPerformanceData to set
	 */
	public void setHeapStatus(final HeapPerformanceData heapStatus) {
		this.heapStatus = heapStatus;
	}

	/**
	 * Gets the map of provider name to provider performance summary object for
	 * each performance provider.
	 * 
	 * @return map of provider to ProviderPerformanceSummary
	 */
	public Map<String, ProviderPerformanceSummary> getPerformanceData() {
		return performanceData;
	}

	/**
	 * Indicates if the application is currently throttling based on all
	 * provider status.
	 * 
	 * @return true if throttling, false if not
	 */
	public boolean isThrottling() {
		return isThrottling;
	}

	/**
	 * Indicates if the application is currently generating backlog based on all
	 * provider status.
	 * 
	 * @return true if backlogging, false if not
	 */
	public boolean isBacklogging() {
		return isBacklogging;
	}

	/**
	 * Gets the overall health, taking all providers into account
	 * 
	 * @return HealthStatus
	 */
	public HealthStatus getOverallHealth() {
		return overallHealth;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {
		throw new UnsupportedOperationException(
				"This message cannot be serialized to XML; it is internal");

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
	 */
	@Override
	public String getOneLineSummary() {
		final StringBuilder detail = new StringBuilder(
				"Overall health of the application is " + this.overallHealth);
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

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#toString()
	 */
	@Override
	public String toString() {
		return getOneLineSummary();
	}

}
