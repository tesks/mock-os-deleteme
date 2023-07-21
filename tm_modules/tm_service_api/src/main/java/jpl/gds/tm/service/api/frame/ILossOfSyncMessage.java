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

import jpl.gds.shared.time.IAccurateDateTime;

/**
 * An interface to be implement by loss-of-sync frame event messages.
 * 
 *
 * @since R8
 */
public interface ILossOfSyncMessage extends IFrameEventMessage {

    /**
     * This gets the last valid frame seen ERT time.
     * @return The last ERT time.
     */
    IAccurateDateTime getLastErt();

    /**
     * This gets the text describing the reason for this event.
     * @return the text describing the reason for this event
     */
    String getReason();


}