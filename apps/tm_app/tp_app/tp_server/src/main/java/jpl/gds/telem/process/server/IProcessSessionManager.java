/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.telem.process.server;

import jpl.gds.eha.api.service.alarm.IAlarmNotifierService;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.telem.common.manager.ISessionManager;

/**
 * Interface for TP Session Manager specific functions
 *
 */
public interface IProcessSessionManager extends ISessionManager {

    /**
     * Gets the Suspect Channel Service
     *
     * @return ISuspectChannelService, or null if none has been initialized
     */
    ISuspectChannelService getSuspectChannelService();

    /**
     * Gets the Alarm Notifier Service
     *
     * @return IAlarmNotifierService, or null if none has been initialized
     */
    IAlarmNotifierService getAlarmNotifier();

    /**
     * Gets whether or not the Processor has received the end of data
     *
     * @return whether or not the processor has received the end of data
     */
    boolean hasReceivedEndOfData();


}
