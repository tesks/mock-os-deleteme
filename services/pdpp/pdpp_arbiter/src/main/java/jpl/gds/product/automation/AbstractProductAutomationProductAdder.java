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
package jpl.gds.product.automation;

import com.lmax.disruptor.EventHandler;

import jpl.gds.product.automation.disruptor.ProductMetadataEvent;

/**
 *
 * The abstract thread class that deals with adding products to the
 * hibernate database for product automation.
 * 
 * MPCS-8179 - 06/07/16 - Added to AMPCS, updated from original
 *          version in MPCS for MSL G9.
 * MPCS-8295 - 11/28/16 - Updated. Restructured class from
 *          running as a thread to a EventHandler<AbstractProductMetadataEvent>
 *          used by the AutomationDownlinkService Disruptor
 */
public abstract class AbstractProductAutomationProductAdder implements EventHandler<ProductMetadataEvent> {
	protected boolean isRealtimeExtactionOn;

	/**
	 * default constructor
	 */
	public AbstractProductAutomationProductAdder(final boolean isRealtimeExtactionOn) {
		super();
		this.isRealtimeExtactionOn = isRealtimeExtactionOn;
	}
	
	/**
	 * This method will process a single event, which contains an
	 * AbstractProductMetadata object, by inserting it into the database
	 * 
	 * @param event
	 *            event containing the metadata to be stored in the PDPP
	 *            database
	 * @param sequence
	 *            sequence number
	 * @param endOfBatch
	 *            true if the event is the end of the batch, false otherwise
	 * @throws Exception
	 *             if there is a problem encountered while inserting the
	 *             metadata into the database
	 * 
	 * MPCS-8295  - 11/28/16 - run method removed and replaced
	 *          with onEvent.
	 * 
	 */
	public abstract void onEvent(ProductMetadataEvent event, long sequence, boolean endOfBatch) throws Exception;
	
}
