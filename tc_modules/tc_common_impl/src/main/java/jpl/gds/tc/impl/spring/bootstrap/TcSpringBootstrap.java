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
package jpl.gds.tc.impl.spring.bootstrap;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.tc.api.*;
import jpl.gds.tc.api.cltu.IBchCodeBlockBuilder;
import jpl.gds.tc.api.cltu.ICltuBuilder;
import jpl.gds.tc.api.cltu.ICltuFactory;
import jpl.gds.tc.api.config.*;
import jpl.gds.tc.api.echo.ICommandEchoInputFactory;
import jpl.gds.tc.api.echo.IEchoDecomService;
import jpl.gds.tc.api.frame.ITcTransferFrameBuilder;
import jpl.gds.tc.api.icmd.ICpdClient;
import jpl.gds.tc.api.icmd.ICpdDmsBroadcastStatusMessagesPoller;
import jpl.gds.tc.api.icmd.ICpdObjectFactory;
import jpl.gds.tc.api.icmd.config.IntegratedCommandProperties;
import jpl.gds.tc.api.message.CommandMessageType;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import jpl.gds.tc.api.output.ICommandMessageUtility;
import jpl.gds.tc.api.output.IRawOutputAdapterFactory;
import jpl.gds.tc.api.output.ISseCommandSocket;
import jpl.gds.tc.api.output.IUplinkResponseFactory;
import jpl.gds.tc.api.packet.ITelecommandPacketFactory;
import jpl.gds.tc.api.plop.ICommandLoadBuilder;
import jpl.gds.tc.impl.CommandObjectFactory;
import jpl.gds.tc.impl.cltu.CltuFactory;
import jpl.gds.tc.impl.cltu.parsers.BchCodeBlockBuilder;
import jpl.gds.tc.impl.cltu.parsers.CltuBuilder;
import jpl.gds.tc.impl.echo.CommandEchoDecom;
import jpl.gds.tc.impl.echo.CommandEchoInputFactory;
import jpl.gds.tc.impl.frame.TcTransferFrameBuilder;
import jpl.gds.tc.impl.icmd.CpdClient;
import jpl.gds.tc.impl.icmd.CpdDmsBroadcastStatusMessagesPoller;
import jpl.gds.tc.impl.icmd.CpdObjectFactory;
import jpl.gds.tc.impl.message.*;
import jpl.gds.tc.impl.output.SseCommandSocket;
import jpl.gds.tc.impl.output.adapter.CommandMessageUtility;
import jpl.gds.tc.impl.output.adapter.RawOutputAdapterFactory;
import jpl.gds.tc.impl.output.adapter.UplinkResponseFactory;
import jpl.gds.tc.impl.packet.TelecommandPacketBuilder;
import jpl.gds.tc.impl.plop.CommandLoadBuilder;
import jpl.gds.tc.impl.scmf.ScmfFactory;
import jpl.gds.tc.impl.scmf.ScmfInternalMessageFactory;
import jpl.gds.tc.impl.util.CommandWriteUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The Spring Bootstrap for telecommand classes
 *
 * 09/18/17  - MPCS-9106 - Added getCommandEchoMessageFactory and getCommandEchoInputFactory
 * 10/23/17  - MPCS-9178 - Update getCommandEchoDecomService to take filename argument
 * 04/09/19  - MPCS-10813 - Added getUplinkResponseFactory and getCommandMessageUtility
 */
@Configuration
public class TcSpringBootstrap {
	
	@Autowired
	ApplicationContext appContext;

	public TcSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommandMessageType.FileLoad,
                FileLoadMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(
                CommandMessageType.FlightSoftwareCommand, FlightSoftwareCommandMessage.XmlParseHandler.class.getName(),
                null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommandMessageType.HardwareCommand,
                HardwareCommandMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommandMessageType.RawUplinkData,
                RawUplinkDataMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommandMessageType.Scmf,
                ScmfMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommandMessageType.SequenceDirective,
                SequenceDirectiveMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommandMessageType.SseCommand,
                SseCommandMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommandMessageType.UplinkStatus,
                CpdUplinkStatusMessage.XmlParseHandler.class.getName(), null, new String[] {"CpdUplinkStatus"}));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommandMessageType.UplinkGuiLog,
                null, null, null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommandMessageType.ClearUplinkGuiLog,
                null, null, null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(CommandMessageType.InternalCpdUplinkStatus,
                null, null, null));
	}

	@Bean(name = TcApiBeans.INTEGRATED_COMMAND_PROPERTIES)
	@Scope("singleton")
	@Lazy
	public IntegratedCommandProperties getIntegratedCommandProperties() {
		return new IntegratedCommandProperties();
	}

    /**
     * @param missionProps
     *            Mission Properties
     * @param sseFlag
     *            The SSE context flag
     * @return CommandProperties if uplink is enabled, otherwise null
     */
	@Bean(name = TcApiBeans.COMMAND_PROPERTIES )
	@Scope("singleton")
	@Lazy
    public CommandProperties getCommandProperties(final MissionProperties missionProps, final SseContextFlag sseFlag) {
		if (missionProps.isUplinkEnabled()) {
            return new CommandProperties(missionProps, sseFlag);
		} else {
			return null;
		}
	}

    /**
     * @param missionProps
     *            MissionProperties
     * @param sseFlag
     *            The SSE context flag
     * @return CltuProperties if uplink is enabled, otherwise null
     */
	@Bean(name = TcApiBeans.CLTU_PROPERTIES )
	@Scope("singleton")
	@Lazy
    public CltuProperties getCltuProperties(final MissionProperties missionProps, final SseContextFlag sseFlag) {
		if (missionProps.isUplinkEnabled()) {
            return new CltuProperties(sseFlag);
		} else {
			return null;
		}
	}

    /**
     * @param missionProps
     *            MissionProperties
     * @param sseFlag
     *            The SSE context flag
     * @return CommandProperties if uplink is enabled, otherwise null
     */
	@Bean(name = TcApiBeans.FRAME_PROPERTIES)
	@Scope("singleton")
	@Lazy
    public CommandFrameProperties getFrameProperties(final MissionProperties missionProps, final SseContextFlag sseFlag) {
		if (missionProps.isUplinkEnabled()) {
            return new CommandFrameProperties(sseFlag);
		} else {
			return null;
		}
	}

    /**
     * @param missionProps
     *            MissionProperties
     * @param sseFlag
     *            The SSE context flag
     * @return Plop if uplink is enabled, otherwise null
     */
	@Bean(name = TcApiBeans.PLOP_PROPERTIES)
	@Scope("singleton")
	@Lazy
    public PlopProperties getPlopProperties(final MissionProperties missionProps, final SseContextFlag sseFlag) {
		if (missionProps.isUplinkEnabled()) {
            return new PlopProperties(sseFlag);
		} else {
			return null;
		}
	}

    /**
     * @param missionProps
     *            MissionProperties
     * @param sseFlag
     *            The SSE context flag
     * @return ScmfProperties if uplink is enabled, otherwise null
     */
	@Bean(name = TcApiBeans.SCMF_PROPERTIES)
	@Scope("singleton")
	@Lazy
    public ScmfProperties getScmfProperties(final MissionProperties missionProps, final SseContextFlag sseFlag) {
		if (missionProps.isUplinkEnabled()) {
            return new ScmfProperties(sseFlag);
		} else {
			return null;
		}
	}

	@Bean(name = TcApiBeans.CPD_CLIENT )
	@Scope("singleton")
	@Lazy
	public ICpdClient getCpdClient(final ApplicationContext appContext, final MissionProperties missionProps) throws JAXBException, ParserConfigurationException {
		if (missionProps.isUplinkEnabled()) {
			return new CpdClient(appContext);
		} else {
			return null;
		}
	}

	@Bean(name = TcApiBeans.OUTPUT_ADAPTER_FACTORY)
	@Scope("singleton")
	@Lazy
	public IRawOutputAdapterFactory getOutputAdapterFactory(final ApplicationContext appContext) {
		return new RawOutputAdapterFactory(appContext);
	}

	@Bean(name = TcApiBeans.CPD_STATUS_POLLER )
	@Scope("singleton")
	@Lazy
	public ICpdDmsBroadcastStatusMessagesPoller getCpdDmsBroadcastStatusMessagesPoller(final ApplicationContext appContext) {
	    return new CpdDmsBroadcastStatusMessagesPoller(appContext);
	}

	@Bean(name = TcApiBeans.SCMF_FACTORY)
	@Scope("singleton")
	@Lazy
	public IScmfFactory getScmfFactory(final ApplicationContext appContext) {
	    return new ScmfFactory(appContext);
	}

	@Bean(name = TcApiBeans.COMMAND_MESSAGE_FACTORY)
	@Scope("singleton")
	@Lazy
	public ICommandMessageFactory getCommandMessageFactory() {
	    return new CommandMessageFactory();
	}

    @Bean(name = TcApiBeans.COMMAND_OBJECT_FACTORY)
    @Scope("singleton")
    @Lazy
    public ICommandObjectFactory getCommandObjectFactory(final ApplicationContext appContext) {
        return new CommandObjectFactory(appContext);
    }


    /*
     * MPCS-10473 - 03/21/19 - REMOVED ICommandArgumentFactory from bootstrap
     *  DO NOT WANT anything outside of flight commands from accessing it
     */

    @Bean(name = TcApiBeans.UPLINK_RESPONSE_FACTORY)
    @Scope("singleton")
    @Lazy
    public IUplinkResponseFactory getUplinkResponseFactory() {
    	return new UplinkResponseFactory();
    }


    @Bean(name = TcApiBeans.SSE_COMMAND_SOCKET)
    @Scope("singleton")
    @Lazy
    public ISseCommandSocket getSseCommandSocket(final ApplicationContext appContext) {
        return new SseCommandSocket(appContext);
    }

    @Bean(name = TcApiBeans.COMMAND_WRITE_UTILITY)
    @Scope("singleton")
    @Lazy
    public ICommandWriteUtility getCommandWriteUtility(final ApplicationContext appContext) {
        return new CommandWriteUtil(appContext);
    }

    @Bean(name = TcApiBeans.COMMAND_MESSAGE_UTILITY)
    @Scope("singleton")
    @Lazy
    public ICommandMessageUtility getCommandMessageUtility(final ApplicationContext appContext) {
    	return new CommandMessageUtility(appContext);
    }

    @Bean(name = TcApiBeans.CPD_OBJECT_FACTORY)
    @Scope("singleton")
    @Lazy
    public ICpdObjectFactory getCpdObjectFactory() {
        return new CpdObjectFactory();
    }

    @Bean(name = TcApiBeans.COMMAND_ECHO_INPUT_FACTORY)
    @Scope("singleton")
    @Lazy
    public ICommandEchoInputFactory getCommandEchoInputFactory() {
        return new CommandEchoInputFactory();
    }

    @Bean(name = TcApiBeans.COMMAND_ECHO_DECOM_SERVICE)
    @Scope("singleton")
    @Lazy
    public IEchoDecomService getCommandEchoDecomService(final String filename) {
        return new CommandEchoDecom(appContext, filename);
    }

    @Bean("BCH_BUILDER")
    @Scope("prototype")
    @Lazy
    public IBchCodeBlockBuilder getBchCodeBlockBuilder() {
        return new BchCodeBlockBuilder();
    }

    @Bean(name = TcApiBeans.CLTU_BUILDER_FACTORY)
    @Scope("singleton")
    @Lazy
    @Primary
    public ICltuFactory getCltuFactory(final ApplicationContext appContext) {
        return new CltuFactory(appContext);
    }

    @Bean(name = TcApiBeans.TELECOMMAND_PACKET_FACTORY)
    @Scope("singleton")
    @Lazy
    public ITelecommandPacketFactory getTelecommandPacketFactory(final ApplicationContext appContext) {
        return new TelecommandPacketBuilder(appContext);
    }

    @Bean(name = TcApiBeans.COMMAND_LOAD_BUILDER)
    @Scope("prototype")
    @Lazy
    public ICommandLoadBuilder getCommandLoadBuilder() {
        return new CommandLoadBuilder();
    }

    @Bean(name = TcApiBeans.SCMF_INTERNAL_MESSAGE_FACTORY)
    @Scope("singleton")
    @Lazy
    public IScmfInternalMessageFactory getScmfInternalMessageFactory(final ApplicationContext appContext) {
        return new ScmfInternalMessageFactory(appContext);
    }

    @Bean(TcApiBeans.TC_TRANSFER_FRAME_BUILDER)
    @Scope("prototype")
    @Lazy
    public ITcTransferFrameBuilder getTcTransferFrameBuilder() {
        return new TcTransferFrameBuilder();
    }

    @Bean(TcApiBeans.CLTU_BUILDER)
    @Lazy
    public ICltuBuilder getCltuBuilder(final CltuProperties cltuProperties, PlopProperties plopProperties) {
        CltuBuilder.setSequences(cltuProperties, plopProperties);
        return new CltuBuilder();
    }
}
