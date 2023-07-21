/*
 * Copyright 2006-2021. California Institute of Technology.
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
package jpl.gds.telem.input.impl.spring.bootstrap;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.input.api.RawInputException;
import jpl.gds.telem.input.api.TmInputApiBeans;
import jpl.gds.telem.input.api.TmInputMessageType;
import jpl.gds.telem.input.api.config.RawDataFormat;
import jpl.gds.telem.input.api.config.StreamType;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import jpl.gds.telem.input.api.connection.IRawInputConnection;
import jpl.gds.telem.input.api.data.IRawDataProcessor;
import jpl.gds.telem.input.api.data.helper.IDataProcessorHelper;
import jpl.gds.telem.input.api.message.ITelemetryInputMessageFactory;
import jpl.gds.telem.input.api.service.ITelemetryInputService;
import jpl.gds.telem.input.api.stream.IParsedFrameFactory;
import jpl.gds.telem.input.api.stream.IRawStreamProcessor;
import jpl.gds.telem.input.impl.connection.*;
import jpl.gds.telem.input.impl.data.PacketDataProcessor;
import jpl.gds.telem.input.impl.data.TransferFrameDataProcessor;
import jpl.gds.telem.input.impl.data.helper.*;
import jpl.gds.telem.input.impl.message.RawInputMessenger;
import jpl.gds.telem.input.impl.message.TelemetryInputMessageFactory;
import jpl.gds.telem.input.impl.message.TelemetrySummaryMessage;
import jpl.gds.telem.input.impl.service.TelemetryInputService;
import jpl.gds.telem.input.impl.stream.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import static jpl.gds.telem.input.api.config.StreamType.*;

/**
 * Spring bootstrap configuration class for the tm_input projects.
 *
 *
 * @since R8
 *
 */
@Configuration
public class TelemetryInputSpringBootstrap {

	/**
	 * Constructor.
	 */
	public TelemetryInputSpringBootstrap() {
		MessageRegistry
				.registerMessageType(new RegisteredMessageConfiguration(TmInputMessageType.TelemetryInputSummary,
						TelemetrySummaryMessage.XmlParseHandler.class.getName(), null, new String[] {"RawDataSummary"}));
	}


	/**
	 * Gets the singleton TelemetryInputConfig bean.
	 *
	 * @param sseFlag
	 *            The SSE context flag
	 *
	 * @return TelemetryInputConfig bean
	 */
	@Bean(name=TmInputApiBeans.TELEMETRY_INPUT_PROPERTIES)
	@Scope("singleton")
	@Lazy(value=true)
	public TelemetryInputProperties getTelemetryInputProperties(final SseContextFlag sseFlag) {
		return new TelemetryInputProperties(sseFlag);
	}


	/**
	 * Gets the singleton RawInputMessenger bean.
	 * @param appContext the current application context, autowired
	 *
	 * @return RawInputMessenger bean
	 */
	@Bean(name=TmInputApiBeans.RAW_INPUT_MESSENGER)
	@Scope("singleton")
	@Lazy(value=true)
	public RawInputMessenger getRawInputMessenger(final ApplicationContext appContext) {
		return new RawInputMessenger(appContext);
	}

	/**
	 * Gets a prototype IRawInputConnection bean.
	 * @param appContext the current application context, autowired
	 *
	 * @return IRawInputConection bean
	 * @throws RawInputException if the connection object cannot be created
	 */
	@Bean(name=TmInputApiBeans.RAW_INPUT_CONNECTION)
	@Scope("prototype")
	@Lazy(value=true)
	public IRawInputConnection createRawInputConnection(final ApplicationContext appContext) throws RawInputException {
		final TelemetryConnectionType connType = appContext.getBean(IConnectionMap.class).getDownlinkConnection().getDownlinkConnectionType();

		switch(connType) {
			case NEN_SN_CLIENT:
			case CLIENT_SOCKET:
				return new ClientSocketInputConnection(appContext);
			case DATABASE:
				return new DatabaseInputConnection(appContext);
			case FILE:
				return new FileInputConnection(appContext);
			case NEN_SN_SERVER:
			case SERVER_SOCKET:
				return new ServerSocketInputConnection(appContext);
			case TDS:
				return new TdsInputConnection(appContext);
			case UNKNOWN:
			default:
				throw new RawInputException("No raw input connection class is defined for connection type " + connType);

		}

	}

	/**
	 * Gets a prototype IRawDataProcessor bean.
	 * @param appContext the current application context, autowired
	 *
	 * @return IRawDataProcessor bean
	 * @throws RawInputException if the connection object cannot be created
	 */
	@Bean(name=TmInputApiBeans.RAW_DATA_PROCESSOR)
	@Scope("prototype")
	@Lazy(value=true)
	public IRawDataProcessor createRawDataProcessor(final ApplicationContext appContext) throws RawInputException {

		final TelemetryInputType inputType = appContext.getBean(IConnectionMap.class).getDownlinkConnection().getInputType();

		final RawDataFormat format = RawDataFormat.getRawDataFormat(inputType);

		switch(format) {
			case PACKET:
				return new PacketDataProcessor(appContext);
			case TRANSFER_FRAME :
				return new TransferFrameDataProcessor(appContext);
			case UNKNOWN:
			default:
				throw new RawInputException("No raw data processor class is defined for format " + format);

		}

	}


	/**
	 * Gets a prototype IDataProcessorHelper bean.
	 * @param appContext the current application context, autowired
	 *
	 * @return IDataProcessorHelper bean
	 * @throws RawInputException if the connection object cannot be created
	 */
	@Bean(name=TmInputApiBeans.DATA_PROCESSOR_HELPER)
	@Scope("prototype")
	@Lazy(value=true)
	public IDataProcessorHelper createDataProcessorHelper(final ApplicationContext appContext) throws RawInputException {
		final TelemetryConnectionType connType = appContext.getBean(IConnectionMap.class).getDownlinkConnection().getDownlinkConnectionType();
		final TelemetryInputType inputType = appContext.getBean(IConnectionMap.class).getDownlinkConnection().getInputType();

		StreamType streamType = StreamType.getStreamType(inputType);

		if (connType == TelemetryConnectionType.DATABASE) {
			streamType = StreamType.DATABASE;
		}

		if(streamType.equals(DATABASE)) {
			return new DbDataProcessorHelper(appContext);
		}
		else if(streamType.equals(LEOT_TF) || streamType.equals(TRANSFER_FRAME) || streamType.equals(SLE_TF)) {
			return new TfDataProcessorHelper(appContext);
		}
		else if(streamType.equals(PACKET)) {
			return new PktDataProcessorHelper(appContext);
		}
		else if(streamType.equals(SFDU_PKT) || streamType.equals(SFDU_TF)) {
			return new SfduDataProcessorHelper(appContext);
		}
		//dynamic values (for sync transfer frames)
		else if(!streamType.equals(CMD_ECHO) && !streamType.equals(UNKNOWN) &&
				StreamType.valuesAsString().contains(streamType.name())){
			return new TfDataProcessorHelper(appContext);
		}

		throw new RawInputException("No data processor helper class is defined for stream type " + streamType);

	}

	/**
	 * Gets a prototype IRawStreamProcessor bean.
	 * @param appContext the current application context, autowired
	 *
	 * @return IRawStreamProcessor bean
	 * @throws RawInputException if the connection object cannot be created
	 */
	@Bean(name=TmInputApiBeans.RAW_STREAM_PROCESSOR)
	@Scope("prototype")
	@Lazy(value=true)
	public IRawStreamProcessor createRawStreamProcessor(final ApplicationContext appContext)
			throws RawInputException {

		final TelemetryConnectionType connType = appContext.getBean(IConnectionMap.class).getDownlinkConnection().getDownlinkConnectionType();
		final TelemetryInputType inputType = appContext.getBean(IConnectionMap.class).getDownlinkConnection().getInputType();

		final StreamType streamType = StreamType.getStreamType(inputType);

		if(connType == TelemetryConnectionType.DATABASE) {
			return new DatabaseStreamProcessor(appContext);
		} else {

			if(streamType.equals(LEOT_TF)){
				return new LeotTfStreamProcessor(appContext);
			}
			else if(streamType.equals(PACKET)){
				return new PacketStreamProcessor(appContext);
			}
			else if(streamType.equals(SFDU_PKT)){
				return new SfduPktStreamProcessor(appContext);
			}
			else if(streamType.equals(SFDU_TF)){
				return new SfduTfStreamProcessor(appContext);
			}
			else if(streamType.equals(TRANSFER_FRAME)){
				return new TransferFrameStreamProcessor(appContext);
			}
			else if(streamType.equals(SLE_TF)){
				return new SleFrameStreamProcessor(appContext);
			}
			//dynamic types (for sync transfer frames)
			else if(!streamType.equals(CMD_ECHO) && !streamType.equals(UNKNOWN) &&
					TelemetryInputType.valuesAsString().contains(streamType.name())){
				return new GenericSyncTfStreamProcessor(appContext);
			}
			throw new RawInputException("No raw stream processor class is defined for stream type " + streamType);
		}
	}

	/**
	 * Gets the singleton ITelemetryInputMessageFactory bean.
	 *
	 * @return ITelemetryInputMessageFactory bean
	 */
	@Bean(name=TmInputApiBeans.TELEM_INPUT_MESSAGE_FACTORY)
	@Scope("singleton")
	@Lazy(value=true)
	public ITelemetryInputMessageFactory getTelemetryInputMessageFactory() {
		return new TelemetryInputMessageFactory();
	}

	/**
	 * Gets the singleton ITelemetryInputService bean.
	 * @param appContext the current application context, autowired
	 *
	 * @return ITelemetryInputService bean
	 */
	@Bean(name=TmInputApiBeans.TELEM_INPUT_SERVICE)
	@Scope("singleton")
	@Lazy(value=true)
	public ITelemetryInputService getTelemetryInputService(final ApplicationContext appContext) {
		return new TelemetryInputService(appContext);
	}

	/**
	 * Gets the singleton IParsedFrameFactory bean.
	 *
	 * @return IParsedFrameFactory bean
	 */
	@Bean(name=TmInputApiBeans.PARSED_FRAME_FACTORY)
	@Scope("singleton")
	public IParsedFrameFactory getParsedFrameFactory() {
		return new ParsedFrameFactory();
	}
}