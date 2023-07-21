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
package jpl.gds.product.automation.disruptor;

import com.lmax.disruptor.EventFactory;

/**
 * Factory to create ProductMetadataEvents.
 * 
 * MPCS-8179 - 06/07/16 - Created
 */
public class ProductMetadataEventFactory implements EventFactory<ProductMetadataEvent> {

	/* (non-Javadoc)
	 * @see com.lmax.disruptor.EventFactory#newInstance()
	 */
	@Override
	public ProductMetadataEvent newInstance() {
		return new ProductMetadataEvent();
	}
	
}