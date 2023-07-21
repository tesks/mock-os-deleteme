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
package jpl.gds.product.impl;


import org.springframework.context.ApplicationContext;

import jpl.gds.product.api.IProductTrackingService;
import jpl.gds.product.api.message.IPartReceivedMessage;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;

/**
 * ProductMeter keeps track of statistics related to product construction
 * by listening for product part, product assembled, and partial product 
 * messages on the internal message context. ProductMeter is a downlink
 * service, meaning it is instantiated by the downlink processor's
 * service management infrastructure.
 *
 */
public class ProductTrackingService implements MessageSubscriber, IProductTrackingService {

    private final IMessagePublicationBus messageContext;
    private long startTime;
    private long stopTime;
    private long partCount;
    private long productCount;
    private long dataByteCount;
    private long partialProductCount;
    
    /**
     * Creates an instance of ProductTrackingService.
     * @param context the current application context
     */
    public ProductTrackingService(final ApplicationContext context) {
        this.messageContext = context.getBean(IMessagePublicationBus.class);
    }
    
    /**
     * Subscribes to product part, product assembled, and partial product messages.
     */
    private void subscribeAll() {
        messageContext.subscribe(ProductMessageType.ProductPart, this);
        messageContext.subscribe(ProductMessageType.ProductAssembled, this);
        messageContext.subscribe(ProductMessageType.PartialProduct, this);
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#startService()
     */
    @Override
    public boolean startService() {
        stopTime = 0;
        partCount = 0;
        productCount = 0;
        dataByteCount = 0;
        partialProductCount = 0;
    	startTime = System.currentTimeMillis();
    	subscribeAll();
    	return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void handleMessage(final IMessage genericMessage) {
    	if (genericMessage instanceof IPartReceivedMessage) {
              final IPartReceivedMessage m = (IPartReceivedMessage) genericMessage;
    		/*
    		 * placed count in an if statement, don't
    		 * want to count MPDUs or EPDUs as product data bytes.
    		 */
    		if(m.getPart().getPartPduType().isData() || m.getPart().getPartPduType().isEndOfData()){
    			++partCount;
    			dataByteCount += m.getPart().getPartLength();
    		}
    	} else if (genericMessage instanceof IProductAssembledMessage) {
    		++productCount;
    	} else if (genericMessage instanceof IPartialProductMessage) {
    		++partialProductCount;
    	}
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.IService#stopService()
     */
    @Override
	public void stopService() {
        stopTime = System.currentTimeMillis();
        messageContext.unsubscribeAll(this);
    }

    /**
     * Gets the elapsed time that the meter has been running.
     * 
     * @return the elapsed time in seconds
     */
    @Override
	public long getSecondsElapsed() {
        return (stopTime - startTime) / 1000;
    }

    /**
     * Gets the Mbps data rate of product processing.
     * 
     * @return the data rated in Mega-bits per second
     */
    @Override
	public float getDataMbps() {
        long end = stopTime;
        if (end == 0L) {
            end = System.currentTimeMillis();
        }
        return (8.0f * dataByteCount) / (1000.0f * (end - startTime));
    }

    /**
     * Gets the bytes/second data rate of product processing.
     * 
     * @return the data rated in bytes per second
     */
    @Override
	public float getDataBytesPerSecond() {
        long end = stopTime;
        if (end == 0L) {
            end = System.currentTimeMillis();
        }
        return (1000.0f * dataByteCount) / (end - startTime);
    }

    /**
     * Gets the parts per second rate of product processing.
     * 
     * @return the parts per second
     */
    @Override
	public float getPartsPerSecond() {
        long end = stopTime;
        if (end == 0L) {
            end = System.currentTimeMillis();
        }
        return (1000.0f * partCount) / (end - startTime);
    }

    /**
     * Gets the number of bytes of product data processed.
     * 
     * @return the number of data bytes
     */
    @Override
	public long getDataByteCount() {
        return dataByteCount;
    }

    /**
     * Gets the number of complete products assembled.
     * 
     * @return the number of products
     */
    @Override
	public long getProductCount() {
        return productCount;
    }
    
    /**
     * Gets the number of product parts assembled.
     * 
     * @return the number of parts
     */
    @Override
	public long getPartCount() {
        return partCount;
    }
    
    /**
     * Gets the number of partial products assembled.
     * 
     * @return the number of partial products
     */
    @Override
	public long getPartialProductCount() {
        return partialProductCount;
    }
}


