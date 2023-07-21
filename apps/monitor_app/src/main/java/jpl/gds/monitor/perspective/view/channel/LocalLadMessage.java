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
package jpl.gds.monitor.perspective.view.channel;

import java.util.LinkedList;
import java.util.List;

import jpl.gds.monitor.message.InternalMonitorMessageType;
import jpl.gds.shared.message.Message;

public class LocalLadMessage extends Message {

    private final List<MonitorChannelSample> samples = new LinkedList<MonitorChannelSample>();
    
    public LocalLadMessage(){
        super(InternalMonitorMessageType.MonitorLocalLad);
    }

    @Override
    public String getOneLineSummary() {
        return this.getType().toString();
    }

    @Override
    public String toString() {
        return getOneLineSummary();
    }
    
    public void addSample(final MonitorChannelSample toAdd) {
        this.samples.add(toAdd);
    }
    
    public List<MonitorChannelSample> getAllSamples() {
        return this.samples;
    }

}
