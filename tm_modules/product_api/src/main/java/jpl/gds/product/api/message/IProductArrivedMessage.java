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
 * An interface to be implemented by product arrived messages, used during decom.
 * 
 *
 * @since R8
 */
public interface IProductArrivedMessage extends IMessage {

    /**
     * Retrieves the product metadata associated with this message. Return
     * value must be cast to mission-specific metadata type.
     * 
     * @return the IProductMetadataProvider
     */
	public IProductMetadataProvider getProductMetadata();
}