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

import jpl.gds.shared.message.IMessageType;

/**
 * An enumeration of public message types in the EHA projects.
 * 
 * @since R8
 */
public enum EhaMessageType implements IMessageType {
    /** Raw (non-final) channel value message */
    ChannelValue,
    /** Start of channel processing stream message */
    StartChannelProcessing,
    /** End of channel processing stream message */
    EndChannelProcessing,
    /** Final EHA channel (alarmed channel value) message type */
    AlarmedEhaChannel,
     /** Alarmed EHA channel group message type */
    GroupedEhaChannels,
    /** Suspect channels message type */
    SuspectChannels,
    /** Channels with alarm state changes **/
    AlarmChange;
 
    @Override
    public String getSubscriptionTag() {
        return name();
    }
       
}
