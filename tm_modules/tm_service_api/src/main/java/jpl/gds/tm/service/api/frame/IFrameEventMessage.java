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
package jpl.gds.tm.service.api.frame;

import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.station.api.IStationTelemInfo;

/**
 * An interface to be implemented by messages that carry frame events.
 * 
 *
 * @since R8
 */
public interface IFrameEventMessage extends IPublishableLogMessage {

    /**
     * Gets the telemetry frame information object associated with this message.
     * 
     * @return telemetry frame information object
     */
    ITelemetryFrameInfo getFrameInfo();

    /**
     * Gets the station information object associated with this message.
     * 
     * @return station information object
     */
    IStationTelemInfo getStationInfo();

    /**
     * Gets the message type object for this message, since this interface
     * may be implemented for different types of frame events.
     * 
     * @return message type object
     */
    IMessageType getMessageType();

}