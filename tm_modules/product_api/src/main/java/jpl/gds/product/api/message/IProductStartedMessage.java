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
package jpl.gds.product.api.message;

import jpl.gds.shared.message.IMessage;

/**
 * An interface to be implemented by product started messages.
 * 
 *
 * @since R8
 */
public interface IProductStartedMessage extends IMessage {

	/**
	 * Gets the virtual channel ID on which this product was received.
	 * @return the VICD.
	 */
	public int getVcid();

	/**
	 * Gets the product APID.
	 * @return the apid.
	 */
	public int getApid();

	/**
	 * Gets the total number of expected parts for the product.
	 * @return the number of parts
	 */
	public int getTotalParts();

	/**
	 * Retrieves the product type (description) as determined from the APID dictionary.
	 * 
	 * @return the product type
	 */
	public String getProductType();

	/**
	 * Retrieves the product builder transaction ID.
	 * @return the transaction ID 
	 */
	public String getTransactionId();
}