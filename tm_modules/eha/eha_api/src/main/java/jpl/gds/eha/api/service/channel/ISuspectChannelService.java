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
package jpl.gds.eha.api.service.channel;

import jpl.gds.eha.api.message.ISuspectChannelTable;
import jpl.gds.shared.interfaces.IService;

/**
 * An interface to be implemented by suspect channel services, which
 * periodically publish messages identifying channels that flight has identified
 * as unreliable.
 * 
 * @since R8
 */
public interface ISuspectChannelService extends IService {

    /**
     * Gets the suspect channel table being used/maintained by the service.
     * 
     * @return suspect channel table
     */
    public ISuspectChannelTable getTable();

    /**
     * Immediately publishes this table to the message bus. Usually, publication
     * is automatic on a timer.
     */
    public void sendUpdate();

}