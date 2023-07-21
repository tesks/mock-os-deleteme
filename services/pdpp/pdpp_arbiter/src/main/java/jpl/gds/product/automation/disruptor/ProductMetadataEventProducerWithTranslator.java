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

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;

import jpl.gds.product.api.IProductMetadataProvider;

/**
 * Transforms any given AbstractProductMetadata into an AbstractProductMetadataEvent and adds it to the ring buffer.
 * 
 * MPCS-8179 - 06/07/16 - Created
 */
public class ProductMetadataEventProducerWithTranslator implements IAutomationDisruptorProducer<IProductMetadataProvider> {
	private final RingBuffer<ProductMetadataEvent> ringBuffer;
	
	/**
	 * Constructor
	 * @param ringBuffer the ring buffer to be used by this class
	 */
	public ProductMetadataEventProducerWithTranslator(RingBuffer<ProductMetadataEvent> ringBuffer){
		this.ringBuffer = ringBuffer;
	}
	
	// simple translator, just stores the metadata within the event
	private static final EventTranslatorOneArg<ProductMetadataEvent, IProductMetadataProvider> TRANSLATOR =
			new EventTranslatorOneArg<ProductMetadataEvent, IProductMetadataProvider>() {
				public void translateTo(ProductMetadataEvent event, long sequence, IProductMetadataProvider metadata) {
					event.set(metadata);
				}
			};
	
	/**
	 * Transforms and puts data into the ring buffer
	 * 
	 * @param metadata
	 *            metadata object to be placed into the ring buffer
	 * @see jpl.gds.product.automation.disruptor.IAutomationDiruptorProducer#onData(java.lang.Object)
	 */
	public void onData(IProductMetadataProvider metadata){
		ringBuffer.publishEvent(TRANSLATOR, metadata);
	}
}