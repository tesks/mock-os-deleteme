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
package jpl.gds.telem.input.impl.data;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.config.StreamType;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import jpl.gds.telem.input.api.data.IRawDataProcessor;
import jpl.gds.telem.input.api.data.helper.IDataProcessorHelper;
import jpl.gds.telem.input.impl.message.RawInputMessenger;
/**
 * This is an abstract class that implements <code>IRawDataProcessor</code>
 * and provides common implementations for various methods
 * 
 *
 */
public abstract class AbstractRawDataProcessor extends BaseMessageHandler
        implements IRawDataProcessor {
	/** The current data stream type */
	protected StreamType streamType;
	/** The internal message publication bus */
	protected IMessagePublicationBus context;
	/** The current telemetry input configuration properties */
	protected TelemetryInputProperties rawConfig;
	/** The messenger object used to send input-related messages */
	protected RawInputMessenger messenger;
	/** The shared logger */
	protected Tracer logger;

	/**
	 * Constructor.
	 * 
	 * @param serviceContext the current application context
	 */
	protected AbstractRawDataProcessor(final ApplicationContext serviceContext) {
		this.context = serviceContext.getBean(IMessagePublicationBus.class);
		this.rawConfig = serviceContext.getBean(TelemetryInputProperties.class);
		this.messenger = serviceContext.getBean(RawInputMessenger.class);
        this.logger = TraceManager.getTracer(serviceContext, Loggers.TLM_INPUT);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.IRawDataProcessor#init(jpl.gds.telem.input.api.data.helper.IDataProcessorHelper, jpl.gds.telem.input.api.config.StreamType, jpl.gds.common.config.types.TelemetryConnectionType)
	 */
	@Override
	public void init(final IDataProcessorHelper helper, final StreamType streamType,
	        final TelemetryConnectionType connType) throws RawInputException {
		if (connType == TelemetryConnectionType.DATABASE) {
			this.streamType = StreamType.DATABASE;
		} else {
			this.streamType = streamType;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.IRawDataProcessor#start()
	 */
	@Override
	public void start() {
	    final IMessageType type = rawConfig.getRawDataMessageSubscriptionType(this.streamType);
		this.context.subscribe(type, this);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.telem.input.api.data.IRawDataProcessor#stop()
	 */
	@Override
	public void stop() {
		this.context.unsubscribeAll(this);
	}
}
