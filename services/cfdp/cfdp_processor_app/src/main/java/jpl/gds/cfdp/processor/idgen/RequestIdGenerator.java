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

package jpl.gds.cfdp.processor.idgen;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import jpl.gds.cfdp.processor.config.ConfigurationManager;

@Service
@DependsOn("configurationManager")
public class RequestIdGenerator {

	@Autowired
	ConfigurationManager configurationManager;

	private AtomicLong nextId;

	@PostConstruct
	public void init() {

		/*-
		 * Request ID is constructed this way:
		 *
		 * Higher 32 bits: Hash code of this CFDP Processor's instance ID
		 * Lower 32 bits: Initialized to current system time millis - Epoch.millis
		 *
		 * To reverse encode:
		 *
		 * x = (int) (nextId >> 32);
		 * y = (int) nextId;
		 */
		nextId = new AtomicLong((((long) configurationManager.getInstanceId().hashCode()) << 32)
				| ((System.currentTimeMillis() - Epoch.millis) & 0xffffffffL));
	}

	public long getNewId() {
		return nextId.getAndIncrement();
	}

}