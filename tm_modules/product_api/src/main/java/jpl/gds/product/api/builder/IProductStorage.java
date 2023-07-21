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
package jpl.gds.product.api.builder;

import java.io.File;
import java.util.List;

import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * IProductStorage is an interface implemented by objects that will store data product parts.
 * 
 */
public interface IProductStorage {

    /**
     * Stores the given product part.
     * @param part the AbstractProductPart to store
     * @throws ProductStorageException if the product part cannot be stored.
     */
    void storePart(IProductPartProvider part) throws ProductStorageException;

	/**
	 * From Thread 
	 */
	void start();

    /**
     * Ask to shut down processing in this object.
     */
	void shutdown();

    /**
     * Indicates if this object has been asked to stop
     * AND the thread has exited (which implies that the
     * queue is empty AND the last element has been processed).
     *
     * Note that the thread could have died before we were asked
     * to stop, so we check the done flag as well. Normally
     * the done flag would have been set before the thread
     * finished.
     *
     * No need to check the queue size of in-progress flag.
     *
     * @return True if truly done
     */
	boolean isDone();

    /**
     * Sets the directory to which products and parts are written.
     * 
     * @param baseDirectory
     *            the File object for the product directory
     */
	void setDirectory(File baseDirectory);

    /**
     * Sets the IProductMissionAdaptor object used for creating mission-specific
     * product-related objects.
     * 
     * @param mission
     *            the IProductMissionAdaptor object for the current mission
     */
	void setMissionAdaptation(IProductMissionAdaptor mission);

    /**
     * Starts processing of messages by this object.
     * 
     */
	void startSubscriptions();

    /**
     * Stops all the subscriptions to the internal message bus.
     */
	void closeSubscriptions();

    /**
     * Supplies performance data for the performance summary. This object just
     * has one performance object for the message queue.
     * 
     * @return List of IPerformanceData objects
     */
	List<IPerformanceData> getPerformanceData();

    /**
     * Get the file object that represents the the directory in which partial
     * product transaction logs and other metadata are stored.
     * 
     * @return File object for the active directory
     */
    File getActiveProductDirectory();
    
	/**
	 * Checks to see if the given product with transaction id is being assembled.
	 *
	 * @param transactionId product transaction to check
	 * 
	 * @return true if the product is being assembled, false if not
	 */
	boolean isBeingAssembled(String transactionId);
}
