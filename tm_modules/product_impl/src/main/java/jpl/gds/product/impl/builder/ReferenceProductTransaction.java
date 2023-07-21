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
package jpl.gds.product.impl.builder;

import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.IPduType;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.builder.IProductStorageMetadata;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

/**
 * This is the reference implementation of the ProductTransaction class, for use
 * by the product builder.
 * 
 */
public class ReferenceProductTransaction extends AbstractProductTransaction {

    /**
     * Creates an instance of ReferenceProductTransaction.
     * @param appContext the current context
     */
    public ReferenceProductTransaction(final ApplicationContext appContext) {
        super(appContext, 
                appContext.getBean(IProductBuilderObjectFactory.class).createMetadataUpdater());
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProductStorageMetadata createProductStorageMetadata(final int number, final long offset, final long localOffset,
			final int length, final IAccurateDateTime ert, final ISclk sclk, final IAccurateDateTime scet, final ILocalSolarTime sol, final int pktSequence,
			final int relayScid, final int groupingFlags, final IPduType partPduType) {
		
		return new ReferenceProductStorageMetadata(number, offset, localOffset, length, ert, sclk, scet, sol, pktSequence, relayScid, groupingFlags, partPduType);
	}

	/**
	 * {@inheritDoc} 
	 * @see jpl.gds.product.impl.builder.AbstractProductTransaction#setExtraTemplateContext(java.util.Map)
	 */
	@Override
	public void setExtraTemplateContext(final Map<String, Object> map) {
		// No extras
	}
	
    /** 
     * Indicates whether the product being built still has data gaps.
     * @return true if the product has gaps, or if neither the total file size nor total 
     * number of parts is known
     */
	@Override
    public boolean hasGaps() {
        if (metadata.getTotalParts() == 0) {
            if (fileSize == -1) { // no EPDU or file length received
                return true;
            } else if (fileSize == 0) { // EPDU says there is no data in the product
                return false;
            } else {
                // If we know the file size but not the total parts, we can still check for 
                // gaps by total length
                long receivedSize = 0;
                for (int i = 0; i < parts.size(); i++) {
                    final IProductStorageMetadata p = parts.get(i);
                    if (p != null) {
                        receivedSize += p.getLength();
                    }   
                }   
                if (receivedSize != fileSize) {
                    return true;
                }   
            }   
        } else {
            // We have total part count
			/**
			 * The parts are stored in the parts list
			 * as the actual part number not a minus one, so start at the first data part.
			 */
            for (int i = 1; i <= metadata.getTotalParts(); ++i) {
                if (parts.get(i) == null) {
                    return true;
                }   
            }   
        }   
        return false;
    }
}
