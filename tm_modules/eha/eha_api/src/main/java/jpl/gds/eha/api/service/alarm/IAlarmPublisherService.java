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
package jpl.gds.eha.api.service.alarm;

import jpl.gds.shared.interfaces.IService;

/**
 * An interface to be implemented by channel alarm publisher services.
 * 
 * @wince R8
 */
public interface IAlarmPublisherService extends IService {

    /**
     * Enables or disables alarm calculation. If calculation is disabled, then
     * raw channel value messages will still be converted to alarmed channel
     * value messages, but alarms will never be attached to those messages.
     * 
     * @param isEnable
     *            true to enable
     */
    public void enableCalculation(boolean isEnable);

}