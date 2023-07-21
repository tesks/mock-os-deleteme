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
package jpl.gds.db.api.sql.fetch.aggregate;

import java.util.List;

/**
 * An interface to be implemented by the Output Controller
 *
 */
public interface IOutputController<T> extends Runnable {
    
	/**
	 * Check thread running state
	 * 
	 * @return
	 */
	public boolean isRunning();

    /**
     * Sets the record stream filter list
     * 
     * @param outputStreamFilters the list of Channel Stream Filters
     */
    public void setStreamFilters(List<IChannelStreamFilter<T>> outputStreamFilters);
}
