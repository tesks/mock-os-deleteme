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

import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.template.FullyTemplatable;

/**
 * An interface to be implemented by suspect channel messages.
 * 
 * @since R8
 */
public interface ISuspectChannelsMessage extends IMessage, FullyTemplatable {

    /**
     * Gets the whole suspect channel table from this message.
     * @return the SuspectChannelTable object
     */
    public ISuspectChannelTable getSuspectTable();

}