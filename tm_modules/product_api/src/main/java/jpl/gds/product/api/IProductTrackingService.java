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
package jpl.gds.product.api;

import jpl.gds.shared.interfaces.IService;

/**
 * An interface to be implemented by services that track number of product
 * packets processed and number of products generated.
 * 
 *
 * @since R8
 */
public interface IProductTrackingService extends IService {

	/**
	 * Gets the elapsed time that the meter has been running.
	 * 
	 * @return the elapsed time in seconds
	 */
	long getSecondsElapsed();

	/**
	 * Gets the Mbps data rate of product processing.
	 * 
	 * @return the data rated in Mega-bits per second
	 */
	float getDataMbps();

	/**
	 * Gets the bytes/second data rate of product processing.
	 * 
	 * @return the data rated in bytes per second
	 */
	float getDataBytesPerSecond();

	/**
	 * Gets the parts per second rate of product processing.
	 * 
	 * @return the parts per second
	 */
	float getPartsPerSecond();

	/**
	 * Gets the number of bytes of product data processed.
	 * 
	 * @return the number of data bytes
	 */
	long getDataByteCount();

	/**
	 * Gets the number of complete products assembled.
	 * 
	 * @return the number of products
	 */
	long getProductCount();

	/**
	 * Gets the number of product parts assembled.
	 * 
	 * @return the number of parts
	 */
	long getPartCount();

	/**
	 * Gets the number of partial products assembled.
	 * 
	 * @return the number of partial products
	 */
	long getPartialProductCount();

}