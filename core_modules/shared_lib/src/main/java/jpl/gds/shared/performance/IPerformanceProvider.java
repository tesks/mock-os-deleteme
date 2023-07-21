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
 * This interface must be implemented by all classes wanting to provide
 * performance data to the performance summary publisher.
 * 
 */
public interface IPerformanceProvider {

	/**
	 * Gets the name of this performance provider.
	 * 
	 * @return name string
	 */
	public String getProviderName();

	/**
	 * Gets the list of IPerformanceData objects that describe the current
	 * performance for this provider.
	 * 
	 * @return list of IPerformanceData objects; may be null or empty if there
	 *         is no performance data
	 */
	public List<IPerformanceData> getPerformanceData();

}
