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

package jpl.gds.cfdp.processor.action.disruptor;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.lmax.disruptor.RingBuffer;

import jpl.gds.cfdp.processor.config.ConfigurationManager;

@Service
@DependsOn("configurationManager")
public class ActionRingBufferManager {

	@Autowired
	private ConfigurationManager configurationManager;

	private RingBuffer<ActionEvent> ringBuffer;

	@PostConstruct
	public void init() {
		ringBuffer = RingBuffer.createMultiProducer(ActionEvent::new, configurationManager.getActionRingBufferSize());
	}

	/**
	 * @return the ringBuffer
	 */
	public RingBuffer<ActionEvent> getRingBuffer() {
		return ringBuffer;
	}

}