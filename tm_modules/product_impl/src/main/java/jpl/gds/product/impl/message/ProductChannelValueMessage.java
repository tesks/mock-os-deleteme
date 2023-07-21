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

import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.product.api.message.InternalProductMessageType;
import jpl.gds.shared.message.Message;

/**
 * ProductChannelValueMessage is the most primitive message type for publishing
 * a channelized telemetry values extracted from instrument data products. The
 * channel value contained has not been timestamped, and so has no
 * SCLK/SCET/ERT.
 * 
 *
 */
public class ProductChannelValueMessage extends Message  {

    private final IServiceChannelValue channelVal;

    /**
     * Creates an instance of ProductChannelValueMessage with a current event
     * time.
     * @param chanVal the channel value
     * 
     */
    public ProductChannelValueMessage(final IServiceChannelValue chanVal) {
        super(InternalProductMessageType.ProductChannelValue);
        this.channelVal = chanVal;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        if (channelVal == null) {
            return "Empty Product Channel Value";
        }
        return "ID=" + channelVal.getChanId() + " type= "
                + channelVal.getChannelType().toString()
                + " DN=" + channelVal.stringValue();
    }


    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return getOneLineSummary();
    }

    /**
     * Returns the channel value.
     * 
     * @return channel value
     */
	public IServiceChannelValue getChannelValue() {
        return channelVal;
    }
}
