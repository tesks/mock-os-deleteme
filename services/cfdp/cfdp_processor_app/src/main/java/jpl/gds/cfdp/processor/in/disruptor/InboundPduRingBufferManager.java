/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.processor.in.disruptor;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.lmax.disruptor.RingBuffer;

import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;

@Service
@DependsOn("configurationManager")
public class InboundPduRingBufferManager {

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private ConfigurationManager configurationManager;

    private RingBuffer<InboundPduEvent> ringBuffer;

    @PostConstruct
    public void init() {
        InboundPduEvent.setLog(TraceManager.getTracer(appContext, Loggers.CFDP));
        ringBuffer = RingBuffer.createMultiProducer(InboundPduEvent::new,
                configurationManager.getInboundPduRingBufferSize());
    }

    /**
     * @return the ringBuffer
     */
    public RingBuffer<InboundPduEvent> getRingBuffer() {
        return ringBuffer;
    }

}