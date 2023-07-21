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
package jpl.gds.product.automation;

import java.util.List;

import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.performance.IPerformanceData;

/**
 * An interface for the automation downlink service.
 * 
 * @since R8
 */
public interface IAutomationDownlinkService extends IService {

    /**
     * The AutomationDownlinkService is forcibly terminated and all contained
     * data is lost. This method is NOT meant to be used under normal circumstances
     * 
     * MPCS-8568 - 01/04/17 - added
     */
    void forceStop();

    /**
     * Update the performance data regarding the disruptor's ring buffer.
     */
    void updateDisruptorPerformanceData();

    /**
     * Update and report the performance data regarding the disruptor's ring buffer.
     * 
     * @return a list of the performance data items for the disruptor ring buffer
     */
    List<IPerformanceData> getDisruptorPerformanceData();

}