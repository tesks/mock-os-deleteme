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
package jpl.gds.tm.service.impl.spring.bootstrap;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.ccsds.api.packet.IPacketFormatDefinition;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.config.IFrameFormatDefinition.TypeName;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.tm.service.api.TmServiceApiBeans;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.cfdp.ICfdpMessageFactory;
import jpl.gds.tm.service.api.cfdp.IPduExtractService;
import jpl.gds.tm.service.api.frame.IFrameMessageFactory;
import jpl.gds.tm.service.api.frame.IFrameSyncService;
import jpl.gds.tm.service.api.frame.IFrameTrackingService;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfoFactory;
import jpl.gds.tm.service.api.packet.IPacketExtractService;
import jpl.gds.tm.service.api.packet.IPacketMessageFactory;
import jpl.gds.tm.service.api.packet.IPacketTrackingService;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory;
import jpl.gds.tm.service.config.TelemetryServiceProperties;
import jpl.gds.tm.service.impl.cfdp.CfdpMessageFactory;
import jpl.gds.tm.service.impl.cfdp.CfdpPduMessage;
import jpl.gds.tm.service.impl.cfdp.PduFromPacketExtractService;
import jpl.gds.tm.service.impl.cfdp.PduFromV1FrameExtractService;
import jpl.gds.tm.service.impl.frame.BadFrameMessage;
import jpl.gds.tm.service.impl.frame.FrameMessageFactory;
import jpl.gds.tm.service.impl.frame.FrameSequenceAnomalyMessage;
import jpl.gds.tm.service.impl.frame.FrameSummaryMessage;
import jpl.gds.tm.service.impl.frame.FrameSyncService;
import jpl.gds.tm.service.impl.frame.FrameTrackingService;
import jpl.gds.tm.service.impl.frame.InSyncMessage;
import jpl.gds.tm.service.impl.frame.LossOfSyncMessage;
import jpl.gds.tm.service.impl.frame.OutOfSyncDataMessage;
import jpl.gds.tm.service.impl.frame.TelemetryFrameInfoFactory;
import jpl.gds.tm.service.impl.frame.TelemetryFrameMessage;
import jpl.gds.tm.service.impl.packet.PacketExtractService;
import jpl.gds.tm.service.impl.packet.PacketMessageFactory;
import jpl.gds.tm.service.impl.packet.PacketSummaryMessage;
import jpl.gds.tm.service.impl.packet.PacketTrackingService;
import jpl.gds.tm.service.impl.packet.TelemetryPacketInfoFactory;
import jpl.gds.tm.service.impl.packet.TelemetryPacketMessage;

/**
 * This is the spring bootstrap configuration class for the tm_service projects.
 * 
 * @since R8
 */
@Configuration
public class TmServiceSpringBootstrap {
	
    private final Map<Integer, IPacketExtractService> packetExtractMap = new HashMap<Integer, IPacketExtractService>();
    private final Map<Integer, IPduExtractService> pduExtractMap = new HashMap<Integer, IPduExtractService>();
    
    @Autowired
    private ApplicationContext appContext;
    
    /**
     * Constructor
     */
    public TmServiceSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.BadTelemetryFrame,
                BadFrameMessage.XmlParseHandler.class.getName(), null, new String[] {"BadFrame"}));
        MessageRegistry
                .registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.CfdpPdu, null, null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(
                TmServiceMessageType.FrameSequenceAnomaly, FrameSequenceAnomalyMessage.XmlParseHandler.class.getName(),
                null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.InSync,
                InSyncMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.LossOfSync,
                LossOfSyncMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.OutOfSyncData,
                OutOfSyncDataMessage.XmlParseHandler.class.getName(), null,  new String[] {"OutOfSyncBytes"}));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.PresyncFrameData,
                null, null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.TelemetryFrame,
                null, null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(
                TmServiceMessageType.TelemetryFrameSummary, FrameSummaryMessage.XmlParseHandler.class.getName(), null,  
                new String[] {"FrameSyncSum"}));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.TelemetryFrame,
                null, TelemetryFrameMessage.BinaryParseHandler.class.getName()));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.TelemetryPacket,
                null, TelemetryPacketMessage.BinaryParseHandler.class.getName()));
        MessageRegistry
                .registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.TelemetryPacketSummary,
                        PacketSummaryMessage.XmlParseHandler.class.getName(), null, 
                        new String[] {"PacketExtractSum"}));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TmServiceMessageType.CfdpPdu,
                null, CfdpPduMessage.BinaryParseHandler.class.getName()));
    }
    
	/**
     * Gets the singleton TelemetryServiceProperties bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return TelemetryServiceProperties bean
     */
	@Bean(name=TmServiceApiBeans.TELEMETRY_SERVICE_PROPERTIES)
	@Scope("singleton")
	@Lazy(value=true)
    public TelemetryServiceProperties getTelemetryServiceProperties(final SseContextFlag sseFlag) {
        return new TelemetryServiceProperties(sseFlag);
	}
	
	/**
	 * Gets the singleton ITelemetryPacketInfoFactory bean. Causes the
	 * IApidDictionary bean to be created the first time it is called.
	 * 
	 * @return ITelemetryPacketInfoFactory bean
	 */
    @Bean(name=TmServiceApiBeans.TELEMETRY_PACKET_INFO_FACTORY)
	@Scope("singleton")
	@Lazy(value=true)
	public ITelemetryPacketInfoFactory getTelemetryPacketInfoFactory() {
        final DictionaryProperties dictConfig = appContext.getBean(DictionaryProperties.class);
        IApidDefinitionProvider apidDefs = null;
        try {
            dictConfig.findFileForSystemMission(DictionaryType.APID);
            apidDefs = appContext.getBean(IApidDefinitionProvider.class);
        } catch (final DictionaryException e) {
            TraceManager.getTracer(appContext, Loggers.DEFAULT).debug(
                    "APID dictionary file was not found in CcsdsSpringBootstrap.getSecondaryPacketHeaderLookup().");
            TraceManager.getTracer(appContext, Loggers.DEFAULT)
                    .debug("Default secondary header format assumed for telemetry packets.");
        }
   
		return new TelemetryPacketInfoFactory(apidDefs);
	}
    
    /**
     * Gets the singleton ITelemetryFrameInfoFactory bean.
     * 
     * @return ITelemetryFrameInfoFactory bean.
     */
    @Bean(name=TmServiceApiBeans.TELEMETRY_FRAME_INFO_FACTORY)
    @Scope("singleton")
    @Lazy(value=true)
    public ITelemetryFrameInfoFactory getFrameInfoFactory() {
        return new TelemetryFrameInfoFactory();
    }
	
    /**
     * Gets the singleton IFrameMessageFactory bean.
     * 
     * @return IFrameMessageFactory bean.
     */
	@Bean(name=TmServiceApiBeans.FRAME_MESSAGE_FACTORY)
    @Scope("singleton")
    @Lazy(value=true)
    public IFrameMessageFactory getFrameMessageFactory() {
        return new FrameMessageFactory();
    }

	/**
     * Gets the singleton IFrameSyncService bean.
     * 
     * @return IFrameSyncService bean.
     */
	@Bean(name=TmServiceApiBeans.FRAME_SYNC_SERVICE)
	@Scope("singleton")
	@Lazy(value=true)
	public IFrameSyncService createFrameSynchronizerService() {
	    return new FrameSyncService(appContext);

	}

	/**
     * Gets the singleton IFrameTrackingService bean.
     * 
     * @return IFrameTrackingService bean.
     */
    @Bean(name=TmServiceApiBeans.FRAME_TRACKING_SERVICE)
    @Scope("singleton")
    @Lazy(value=true)
	public IFrameTrackingService createFrameTrackingService() {
	    return new FrameTrackingService(appContext);
	}
    
    /**
     * Gets the singleton IPacketTrackingService bean.
     * 
     * @return IPacketTrackingService bean.
     */
    @Bean(name=TmServiceApiBeans.PACKET_TRACKING_SERVICE)
    @Scope("singleton")
    @Lazy(value=true)
    public IPacketTrackingService createPacketTrackingService() {
        return new PacketTrackingService(appContext);      
    }
    

    /**
     * Gets a prototype IPacketExtractService bean. A unique service 
     * instance will be returned for each virtual channel ID. These are
     * cached. If the service is requested again for the same VCID, 
     * the previously-created instance is returned.
     * 
     * @param vcid virtual channel ID
     * 
     * @return IPacketExtractService bean.
     */
    @Bean(name=TmServiceApiBeans.PACKET_EXTRACT_SERVICE)
    @Scope("prototype")
    @Lazy(value=true)
    public IPacketExtractService createPacketExtractService(final int vcid) {
        IPacketExtractService service = this.packetExtractMap.get(vcid);
        if (service == null) {
            service = new PacketExtractService(appContext, vcid);
            this.packetExtractMap.put(vcid,  service);
        }
        return service;       
    }
    
    /**
     * Gets a prototype IPduExtractService bean. A unique service
     * instance will be returned for each virtual channel ID. These are
     * cached. If the service is requested again for the same VCID,
     * the previously-created instance is returned.
     * 
     * @param type
     *            the type of data to have PDUs extracted from it
     * @param vcid
     *            virtual channel ID
     * 
     * @return IPduExtractService bean.
     */
    @Bean(name=TmServiceApiBeans.PDU_EXTRACT_SERVICE)
    @Scope("prototype")
    @Lazy(value=true)
    public IPduExtractService createPduExtractService(final String type, final int vcid) {
        IPduExtractService service = this.pduExtractMap.get(vcid);
        if (service == null) {
            if (type.equalsIgnoreCase(TypeName.CCSDS_TM_1.toString())) {
                service = new PduFromV1FrameExtractService(appContext, vcid);
            }
            else if (type.equalsIgnoreCase(TypeName.CCSDS_AOS_2_BPDU.toString())) {
                TraceManager.getTracer(appContext, Loggers.DEFAULT)
                            .warn("CFDP PDUs cannot be extracted from CCSDS AOS (V2) B_PDU frames at this time.");
            }
            /* is CCSDS_AOS_2_MPDU or a packet */
            else if (type.equalsIgnoreCase(TypeName.CCSDS_AOS_2_MPDU.toString())
                    || type.equalsIgnoreCase(IPacketFormatDefinition.TypeName.CCSDS.toString())) {
                service = new PduFromPacketExtractService(appContext, vcid);
            }
            this.pduExtractMap.put(vcid,  service);

        }
        return service;       
    }
    

    /**
     * Gets the singleton IPacketMessageFactory bean.
     * 
     * @return IPacketMessageFactory bean.
     */
    @Bean(name=TmServiceApiBeans.PACKET_MESSAGE_FACTORY)
    @Scope("singleton")
    @Lazy(value=true)
    public IPacketMessageFactory getPacketMessageFactory() {
        return new PacketMessageFactory();
    }


    /**
     * Gets the singleton ICfduMessageFactory bean.
     * 
     * @return ICfdpMessageFactory bean.
     */
    @Bean(name=TmServiceApiBeans.CFDP_MESSAGE_FACTORY)
    @Scope("singleton")
    @Lazy(value=true)
    public ICfdpMessageFactory getCfdpMessageFactory() {
        return new CfdpMessageFactory();
    }
}
