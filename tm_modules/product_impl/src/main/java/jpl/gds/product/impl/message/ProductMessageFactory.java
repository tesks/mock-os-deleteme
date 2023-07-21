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
package jpl.gds.product.impl.message;

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.builder.AssemblyTrigger;
import jpl.gds.product.api.message.IPartReceivedMessage;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.IProductArrivedMessage;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.product.api.message.IProductMessageFactory;
import jpl.gds.product.api.message.IProductStartedMessage;

/**
 * Factory that creates public messages for the product modules.
 * 
 *
 * @since R8
 *
 */
public class ProductMessageFactory implements IProductMessageFactory {

    @Override
    public IProductAssembledMessage createProductAssembledMessage(final IProductMetadataUpdater md, final String txId) {
        return new ProductAssembledMessage(md, txId);
    }

    @Override
    public IPartialProductMessage createPartialProductMessage(final String txId, final String txLog, final AssemblyTrigger why,
            final IProductMetadataUpdater md) {
        return new PartialProductMessage(txId, txLog, why, md);
    }

    @Override
    public IProductArrivedMessage createProductArrivedMessage(final IProductMetadataProvider md) {
        return new ProductArrivedMessage(md);
    }

    @Override
    public IPartReceivedMessage createPartReceivedMessage(final IProductPartProvider part) {
        return new ProductPartMessage(part);
    }

    @Override
    public IProductStartedMessage createProductStartedMessage(final String type, final int typeId, final int vcid, final String txId,
            final int totalParts) {
        return new ProductStartedMessage(type, typeId, vcid, txId, totalParts);
    }

}
