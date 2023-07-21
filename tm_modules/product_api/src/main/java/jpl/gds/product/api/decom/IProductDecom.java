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
package jpl.gds.product.api.decom;

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.IProductArrivedMessage;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.shared.types.ByteStream;

/**
 * A read-only interface to be implemented by product decommutation classes.
 * 
 *
 */
public interface IProductDecom {

    /** Decom was successful. **/
    int SUCCESS = 0;
    /** Decom failed. **/
    int FAILURE = 1;
    /** Decom request was only for product viewing, and no product viewers were defined. **/
    int NO_PROD_VIEWER = 2;
    /** Decom request was only for DPO viewing, and no DPO viewers were defined. **/
    int NO_DPO_VIEWER = 3;
    /**
     * Decom failed because no product dictionary definition.
     */
    int NO_PRODUCT_DEF = 4;

    /**
     * Gets the result status from the ProductDecom object.
     * @return completion status: must be SUCCESS, FAILURE, NO_PROD_VIEWER, NO_DPO_VIEWER.
     */
    int getReturnCode();

    /**
     * Handles "product arrived" message receipt, which tell this object that
     * a product's metadata has been read from storage. This triggers the decom of
     * the product.
     *
     * @param message the IProductArrivedMessage to handle
     */
    void handleProductArrivedMessage(IProductArrivedMessage message);

    /**
     * Handles "partial product" message receipt, which tell this object that
     * a partial product and its metadata have been written to storage. This triggers
     * decom of the product.
     *
     * @param message the IPartialProductMessage to handle
     */
    void handlePartialProductMessage(IPartialProductMessage message);

    /**
     * Handles "product assembled" message receipt, which tell this object that
     * a complete product and its metadata have been written to storage.
     *
     * @param message the IPartialProductMessage to handle
     */
    void handleProductAssembledMessage(IProductAssembledMessage message);

    /**
     * Processes the arrival/storage of a new product by dumping content
     * according to its definition, using the currently defined output formatter
     * to format decom output.
     * @param metadata the product metadata for the product
     * @param bytestream the product data stream as a ByteStream
     * 
     * @return true if the product was handled without error, false if not
     * 
     */
    boolean handleProduct(IProductMetadataProvider metadata, ByteStream bytestream);
}