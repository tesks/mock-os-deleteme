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
package jpl.gds.product.processors;

import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.file.IProductMetadata;

/**
 * Interface class for all post downlink product processors (eg: classes that
 * correct, decompress, extract, etc)
 *
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20
 *          adaptation
 */
public interface IPostDownlinkProductProcessor {
	/**
	 * Abstract method for processing a product file.
	 *
	 * @param filename filename of the product to be processed
	 * @param processId process id of the product being processed
	 * @param productId product id of the product being processed
	 * @return the metadata of the newly processed data
	 * @throws ProductException an error occured while processing the product
	 *
	 */
	public IProductMetadata processProduct(final String filename, final long processId, final long productId) throws ProductException;

	/**
	 * Abstract method for processing a product metadata data object.
	 *
	 * @param md metadata object of the product to be processed
	 * @param processId process id of the product being processed
	 * @param productId product id of the product being processed      
	 * @return the metadata of the newly processed data
	 * @throws ProductException an error occured while processing the product
	 */
	public IProductMetadata processProduct(final IProductMetadata md, final long processId, final long productId) throws ProductException;
	/**
	 * Total number of products processed by this processor
	 * @return number of processed products
	 */
	public int getTotalProductsProcessed();

	/**
	 * Number of products successfully processed by this processor
	 * @return number of successfully processed products
	 */
	public int getProductsSuccessfullyProcessed();

	/**
	 * Number of products that failed processing by this processor due to dictionary mismatch
	 * @return number of products that failed processing due to dictionary mismatch
	 */
	public int getProductsAbortedDueToDictionaryMismatch();

	/**
	 * Number of products that failed processing, but not due to dictionary mismatch
	 * @return number of products that failed processing not due to dictionary mismatch
	 */
	public int getProductsFailedForOtherReasons();

	/**
	 * Terminate this processor
	 */
	public void close();

}
