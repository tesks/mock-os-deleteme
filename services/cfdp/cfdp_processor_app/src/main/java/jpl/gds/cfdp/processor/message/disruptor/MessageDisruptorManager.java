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

package jpl.gds.cfdp.processor.message.disruptor;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.lmax.disruptor.dsl.Disruptor;

import jpl.gds.cfdp.processor.config.ConfigurationManager;
import jpl.gds.cfdp.processor.executors.WorkerTasksExecutorManager;

@Service
@DependsOn("configurationManager")
public class MessageDisruptorManager {

	@Autowired
	private ConfigurationManager configurationManager;

	@Autowired
	private WorkerTasksExecutorManager workerTasksExecutorManager;

	private Disruptor<MessageEvent> disruptor;

	@SuppressWarnings("deprecation")
	@PostConstruct
	public void init() {
		disruptor = new Disruptor<>(MessageEvent::new, configurationManager.getMessageDisruptorRingBufferSize(),
				workerTasksExecutorManager.getNonScheduledExecutorService());
	}

	/**
	 * @return the disruptor
	 */
	public Disruptor<MessageEvent> getDisruptor() {
		return disruptor;
	}

}