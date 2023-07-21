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

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.shared.message.IMessage;

/**
 * An interface to be implemented by complete product messages.
 * 
 *
 * @since R8
 */
public interface IProductAssembledMessage extends IMessage {

    /**
     * MTAK field count for tests
     */
	public final int MTAK_FIELD_COUNT = 22;

    /**
     * Retrieves the product builder transaction ID for this product.
     *
     * @return the transactionId as a String
     */
	public String getTransactionId();

    /**
     * Retrieves the product metadata.
     *
     * @return the metadata, which must extend IProductMetadataProvider
     */
	public IProductMetadataProvider getMetadata();
}