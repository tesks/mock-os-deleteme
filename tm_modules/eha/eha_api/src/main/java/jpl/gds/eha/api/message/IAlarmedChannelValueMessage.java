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
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.message.api.IStreamMessage;
import jpl.gds.shared.interfaces.EscapedCsvSupport;
import jpl.gds.shared.template.FullyTemplatable;

/**
 * An interface to be implemented by alarmed channel value messages. This is a
 * raw channel value message that has been passed through an alarm publisher.
 * 
 * @since R8
 *
 */
public interface IAlarmedChannelValueMessage extends IStreamMessage, FullyTemplatable, EscapedCsvSupport, IRealtimeRecordedSupport {
    
    /**
     * Returns the channel value.
     * @return channel value
     */
    public IClientChannelValue getChannelValue();
}