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
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.builder.AssemblyTrigger;

/**
 * An interface to be implemented by product message factories.
 * 
 *
 * @since R8
 */
public interface IProductMessageFactory {

    /**
     * Creates a complete product (product assembled) message.
     * 
     * @param md product metadata
     * @param txId product transaction ID
     * @return new message instance
     */
    public IProductAssembledMessage createProductAssembledMessage(IProductMetadataUpdater md, String txId);
    
    /**
     * Creates a partial product message.
     * 
     * @param md product metadata
     * @param txId product transaction ID
     * @param txLog product transaction log file location
     * @param why reason the partial was generated
     * @return new message instance
     */
    public IPartialProductMessage createPartialProductMessage(final String txId, final String txLog, final AssemblyTrigger why, final IProductMetadataUpdater md);
    
    /**
     * Creates a product arrived message, used during product decom.
     * 
     * @param md product metadata
     * @return new message instance
     */
    public IProductArrivedMessage createProductArrivedMessage(IProductMetadataProvider md);
    
    /**
     * Creates a product part received message.
     * 
     * @param part the product part provider
     * 
     * @return new message instance
     */
    public IPartReceivedMessage createPartReceivedMessage(IProductPartProvider part);
    
    /**
     * Creates a product assembly started message.
     * 
     * @param type product type name
     * @param typeId product numeric type ID (e.g., APID)
     * @param vcid the product virtual channel ID
     * @param txId product transaction ID
     * @param totalParts total number of parts expected for the product
     * @return new message instance
     */
    public IProductStartedMessage createProductStartedMessage(final String type, final int typeId, final int vcid, final String txId, final int totalParts);
}
