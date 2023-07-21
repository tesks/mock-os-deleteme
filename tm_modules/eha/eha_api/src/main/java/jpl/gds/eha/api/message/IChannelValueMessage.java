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
package jpl.gds.eha.api.message;

import jpl.gds.common.types.IRealtimeRecordedSupport;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.message.api.IStreamMessage;

/**
 * An interface to be implemented by raw (non-alarmed, internal) channel value
 * messages.
 * 
 * @since R8
 */
public interface IChannelValueMessage extends IStreamMessage, IRealtimeRecordedSupport {

    /**
     * Returns the channel value.
     * @return channel value
     */
    public IServiceChannelValue getChannelValue();

}