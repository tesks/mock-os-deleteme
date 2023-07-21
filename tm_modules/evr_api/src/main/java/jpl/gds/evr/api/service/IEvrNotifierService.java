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
package jpl.gds.evr.api.service;

import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.shared.interfaces.IService;

/**
 * An interface to be implemented by EVR notification services.
 * 
 * @since R8
 */
public interface IEvrNotifierService extends IService {
    /**
     * Sends EVR notifications relevant to the given EVR message.
     * 
     * @param message
     *            EVR message to send notifications for
     */
    public void sendNotifications(final IEvrMessage message);
}
