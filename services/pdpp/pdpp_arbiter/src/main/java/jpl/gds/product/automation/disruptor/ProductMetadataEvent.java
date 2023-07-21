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

import jpl.gds.product.api.IProductMetadataProvider;

/**
 * Disruptor event used to pass AbstractProudctMetadata objects
 * 
 * MPCS-8179 - 06/07/16 - Created
 */
public class ProductMetadataEvent {
	private IProductMetadataProvider metadata;
	
	/**
	 * Place an AbstractProductMetadata in this event
	 * 
	 * @param metadata
	 *            new value of AbstractProductMetadata
	 */
	public void set(IProductMetadataProvider metadata) {
		this.metadata = metadata;
	}
	
	/**
	 * Get the AbstractProductMetadata from this event
	 * 
	 * @return the contained AbstractProductMetadata
	 */
	public IProductMetadataProvider get(){
		return this.metadata;
	}
}