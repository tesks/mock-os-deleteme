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
package jpl.gds.db.api.sql.store.ldi;

import java.util.List;

import jpl.gds.shared.performance.IPerformanceData;

public interface IInserter {
    /**
     * Tell thread to shut down
     */
    void informShutDown();

    /**
     * Indicates if the thread has been started. Note: this flag does not change
     * when the Inserter is shut down.
     *
     * @return true if started, false if not
     *
     * @version MPCS-7168 - Added method.
     */
    boolean isStarted();

    /**
     * Get shut down status
     *
     * @return True if shut down
     */
    boolean getShutDown();

    /**
     * See Class comments.
     *
     * @return The logical queue size
     */
    int getQueueSize();

    /**
     * Add insert-item to our queue.
     *
     * @param ii
     *            New item
     */
    void add(InsertItem ii);

    /**
     * Return size of queue.
     *
     * @return Count from internal queue
     */
    int size();

    /**
     * Return status of queue.
     *
     * @return True if internal queue empty
     */
    boolean isEmpty();

    /**
     * Returns the performance data for this object, which consists of one queue
     * data object for the inserter queue.
     *
     * @return List of IPerformance data objects
     *
     * @version MPCS-7168 - Added method.
     */
    List<IPerformanceData> getPerformanceData();
    
    /**
     * @see java.lang.Thread.start()
     * 
     * @return
     */
    void startInserter();
    
    void interruptInserter();
}