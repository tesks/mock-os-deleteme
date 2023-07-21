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
import jpl.gds.product.api.builder.AssemblyTrigger;
import jpl.gds.shared.message.IMessage;

/**
 * An interface to be implemented by partial product messages.
 * 
 * @since R8
 */
public interface IPartialProductMessage extends IMessage {

    /**
     * MTAK field count for tests
     */
	public final int MTAK_FIELD_COUNT = 26;

    /**
     * Retrieves the product builder transaction ID.
     *
     * @return the transaction ID
     */
	public String getTransactionId();

    /**
     * Retrieves the file path to the product builder's transaction log for this product.
     *
     * @return the file path
     */
	public String getTransactionLog();

    /**
     * Retrieves the reason the partial product was generated.
     *
     * @return the reason String
     */
	public AssemblyTrigger getReason();

    /**
     * Retrieves the metadata member, which contains the metadata for the product.
     *
     * @return the metadata, which must extend IProductMetadataProvider
     */
	public IProductMetadataProvider getMetadata();
}