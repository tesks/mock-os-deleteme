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
package jpl.gds.eha.impl.spring.bootstrap;

import jpl.gds.eha.impl.message.AlarmChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.eha.api.EhaApiBeans;
import jpl.gds.eha.api.channel.IChannelLad;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.alarm.IAlarmFactory;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryFactory;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryProvider;
import jpl.gds.eha.api.channel.alarm.IAlarmValueFactory;
import jpl.gds.eha.api.channel.alarm.IAlarmValueSetFactory;
import jpl.gds.eha.api.config.EhaProperties;
import jpl.gds.eha.api.feature.IEhaFeatureManager;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.eha.api.service.alarm.IAlarmNotifierService;
import jpl.gds.eha.api.service.alarm.IAlarmPublisherService;
import jpl.gds.eha.api.service.channel.IChannelLadService;
import jpl.gds.eha.api.service.channel.IChannelPublisherUtility;
import jpl.gds.eha.api.service.channel.IDecomListenerFactory;
import jpl.gds.eha.api.service.channel.IDsnMonitorDecomService;
import jpl.gds.eha.api.service.channel.IFrameHeaderChannelizerService;
import jpl.gds.eha.api.service.channel.IGenericPacketDecomService;
import jpl.gds.eha.api.service.channel.IGroupedChannelAggregationService;
import jpl.gds.eha.api.service.channel.IHybridGenericPacketDecomService;
import jpl.gds.eha.api.service.channel.INenStatusDecomService;
import jpl.gds.eha.api.service.channel.IPacketHeaderChannelizerService;
import jpl.gds.eha.api.service.channel.IPrechannelizedAdapter;
import jpl.gds.eha.api.service.channel.IPrechannelizedPublisherService;
import jpl.gds.eha.api.service.channel.ISfduHeaderChannelizerService;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;
import jpl.gds.eha.impl.alarm.AlarmFactory;
import jpl.gds.eha.impl.alarm.AlarmHistory;
import jpl.gds.eha.impl.alarm.AlarmHistoryFactory;
import jpl.gds.eha.impl.alarm.AlarmValueFactory;
import jpl.gds.eha.impl.alarm.AlarmValueSetFactory;
import jpl.gds.eha.impl.channel.ChannelValueFactory;
import jpl.gds.eha.impl.channel.aggregation.GroupedChannelAggregationService;
import jpl.gds.eha.impl.feature.EhaFeatureManager;
import jpl.gds.eha.impl.message.AlarmedChannelValueMessage;
import jpl.gds.eha.impl.message.EhaGroupedChannelValueMessage;
import jpl.gds.eha.impl.message.EhaMessageFactory;
import jpl.gds.eha.impl.message.SuspectChannelsMessage;
import jpl.gds.eha.impl.service.channel.ChannelLad;
import jpl.gds.eha.impl.service.channel.ChannelLadService;
import jpl.gds.eha.impl.service.channel.ChannelPublisherUtility;
import jpl.gds.eha.impl.service.channel.DecomListenerFactory;
import jpl.gds.eha.impl.service.channel.DsnMonitorDecomService;
import jpl.gds.eha.impl.service.channel.FrameHeaderChannelizerService;
import jpl.gds.eha.impl.service.channel.GenericPacketDecomService;
import jpl.gds.eha.impl.service.channel.HybridGenericPacketDecomService;
import jpl.gds.eha.impl.service.channel.NenStatusDecomService;
import jpl.gds.eha.impl.service.channel.PacketHeaderChannelizerService;
import jpl.gds.eha.impl.service.channel.PrechannelizedPublisherService;
import jpl.gds.eha.impl.service.channel.SfduHeaderChannelizerService;
import jpl.gds.eha.impl.service.channel.SuspectChannelService;
import jpl.gds.eha.impl.service.channel.adapter.MultimissionPrechannelizedAdapter;
import jpl.gds.eha.impl.service.channel.adapter.SsePrechannelizedAdapter;
import jpl.gds.eha.impl.service.channel.alarm.AlarmNotifierService;
import jpl.gds.eha.impl.service.channel.alarm.AlarmPublisherService;
import jpl.gds.eha.impl.service.channel.derivation.DerivationMap;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Spring bootstrap configuration for the EHA projects.
 * 
 * @since R8
 */
@Configuration
public class EhaSpringBootstrap {
    
    /**
     * Constructor.
     */
    public EhaSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(EhaMessageType.AlarmedEhaChannel, null,
                AlarmedChannelValueMessage.BinaryParseHandler.class.getName(), new String [] {"EhaChannel"}));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(EhaMessageType.GroupedEhaChannels, null,
                null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(EhaMessageType.SuspectChannels,
                SuspectChannelsMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry
                .registerMessageType(new RegisteredMessageConfiguration(EhaMessageType.ChannelValue, null, null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(EhaMessageType.StartChannelProcessing,
                null, null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(EhaMessageType.EndChannelProcessing,
                null, null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(EhaMessageType.GroupedEhaChannels,
                null, EhaGroupedChannelValueMessage.BinaryParseHandler.class.getName()));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(EhaMessageType.AlarmChange, null,
                AlarmChangeMessage.BinaryParseHandler.class.getName()));
    }

    @Autowired
    private ApplicationContext appContext;

    /**
     * Creates or gets the singleton DerivationMap bean.
     * 
     * @return DerivationMap bean
     */
    @Bean(name = EhaApiBeans.DERIVATION_MAP)
    @Scope("singleton")
    @Lazy(value = true)
    public DerivationMap getDerivationMap() {
        return new DerivationMap(appContext);
    }

    /**
     * Creates or gets the singleton EhaProperties bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return EhaProperties bean
     */
    @Bean(name = EhaApiBeans.EHA_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = true)
    public EhaProperties getEhaProperties(final SseContextFlag sseFlag) {
        return new EhaProperties(sseFlag);
    }

    /**
     * Creates or gets the singleton IAlarmValueFactory bean.
     * 
     * @return IAlarmValueFactory bean
     */
    @Bean(name = EhaApiBeans.ALARM_VALUE_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IAlarmValueFactory getAlarmValueFactory() {
        return new AlarmValueFactory();
    }

    /**
     * Creates or gets the singleton IAlarmValueSetFactory bean.
     * 
     * @return IAlarmValueSetFactory bean
     */
    @Bean(name = EhaApiBeans.ALARM_VALUE_SET_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IAlarmValueSetFactory getAlarmValueSetFactory() {
        return new AlarmValueSetFactory();
    }

    /**
     * Creates or gets the singleton IAlarmFactory bean.
     * 
     * @return IAlarmFactory bean
     */
    @Bean(name = EhaApiBeans.ALARM_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IAlarmFactory getAlarmFactory() {
        return new AlarmFactory(TraceManager.getTracer(appContext, Loggers.ALARM));
    }

    /**
     * Creates or gets the singleton IAlarmHistoryProvider bean.
     * 
     * @return IAlarmHistoryProvider bean
     */
    @Bean(name = EhaApiBeans.ALARM_HISTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IAlarmHistoryProvider getAlarmHistoryProvider() {
        return new AlarmHistory();
    }

    @Bean(name = EhaApiBeans.ALARM_HISTORY_FACTORY)
    @Scope("prototype")
    @Lazy(value = true)
    public IAlarmHistoryFactory getAlarmHistoryFactory() {
        return new AlarmHistoryFactory();
    }

    /**
     * Creates or gets the singleton IAlarmPublisherService bean.
     * 
     * @return IAlarmPublisherService bean
     */
    @Bean(name = EhaApiBeans.ALARM_PUBLISHER_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public IAlarmPublisherService createAlarmPublisherService() {
        return new AlarmPublisherService(appContext);
    }

    /**
     * Creates or gets the singleton IAlarmNotifierService bean.
     * 
     * @return IAlarmNotifierService bean
     */
    @Bean(name = EhaApiBeans.ALARM_NOTIFIER_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public IAlarmNotifierService createAlarmNotifierService() {
        return new AlarmNotifierService(appContext);
    }

    /**
     * Creates or gets the singleton IEhaMessageFactory bean.
     * 
     * @return IEhaMessageFactory bean
     */
    @Bean(name = EhaApiBeans.EHA_MESSAGE_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IEhaMessageFactory createEhaMessageFactory(final MissionProperties missionProperties) {
        return new EhaMessageFactory(missionProperties);
    }

    /**
     * Creates or gets the singleton IChannelValueFactory bean.
     * 
     * @return IChannelValueFactory bean
     */
    @Bean(name = EhaApiBeans.CHANNEL_VALUE_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IChannelValueFactory createChannelValueFactory() {
        return new ChannelValueFactory();
    }

    /**
     * Creates or gets the singleton IPrechannelizedAdapter bean.
     * 
     * @param mprops
     *            current MissionProperties bean, autowired
     * @param sseFlag
     *            current SseContextFlag bean, autowired
     * 
     * @return IPrechannelizedAdapter bean
     */
    @Bean(name = EhaApiBeans.PRECHANNELIZED_ADAPTOR)
    @Scope("singleton")
    @Lazy(value = true)
    public IPrechannelizedAdapter createPrechannelizedAdaptor(final MissionProperties mprops,
                                                              final SseContextFlag sseFlag) {
        /**
         * Gets either the sse or flight pre-channelizer.
         */
        final boolean isSse = sseFlag.isApplicationSse();
        final boolean isJplSse = mprops.sseIsJplStyle();

        if (isSse && isJplSse) {
            return new SsePrechannelizedAdapter(appContext);
        } else {
            return new MultimissionPrechannelizedAdapter(appContext);
        }
    }
    
    /**
     * Creates or gets the singleton IPacketHeaderChannelizerService bean.
     * 
     * @return IPacketHeaderChannelizerService bean
     */
    @Bean(name = EhaApiBeans.PACKET_HEADER_CHANNELIZER_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public IPacketHeaderChannelizerService createPacketHeaderChannelizerService() {
        return new PacketHeaderChannelizerService(appContext);
    }

    /**
     * Creates or gets the singleton ISfduHeaderChannelizerService bean.
     * 
     * @return ISfduHeaderChannelizerService bean
     */
    @Bean(name = EhaApiBeans.SFDU_HEADER_CHANNELIZER_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public ISfduHeaderChannelizerService getSfduHeaderChannelizerService() {
        return new SfduHeaderChannelizerService(appContext);
    }

    /**
     * Creates or gets the singleton ISuspectChannelService bean.
     * 
     * @return ISuspectChannelService bean
     */
    @Bean(name = EhaApiBeans.SUSPECT_CHANNEL_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public ISuspectChannelService getSuspectChannelService() {
        return new SuspectChannelService(appContext);
    }

    /**
     * Creates or gets the singleton IGroupedChannelAggregationService bean.
     * 
     * @return IGroupedChannelAggregationServiceISuspectChannelService bean
     */
    @Bean(name = EhaApiBeans.GROUPED_CHANNEL_AGGREGATION_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public IGroupedChannelAggregationService createGroupedChannelAggregationService() {
        return new GroupedChannelAggregationService(appContext);
    }

    /**
     * Creates or gets the singleton IDecomListenerFactory bean.
     * 
     * @return IDecomListenerFactory bean
     */
    @Bean(name = EhaApiBeans.DECOM_LISTENER_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IDecomListenerFactory createDecomListenerFactory() {
        return new DecomListenerFactory();
    }

    /**
     * Creates or gets the singleton IGenericPacketDecomService bean.
     * 
     * @return IGenericPacketDecomService bean
     */
    @Bean(name = EhaApiBeans.GENERIC_PACKET_DECOM_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public IGenericPacketDecomService createGenericPacketDecomService() {
        return new GenericPacketDecomService(appContext);
    }

    /**
     * Creates or gets the singleton IHybridGenericPacketDecomService bean.
     * 
     * @return IHybridGenericPacketDecomService bean
     */
    @Bean(name = EhaApiBeans.HYBRID_GENERIC_PACKET_DECOM_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public IHybridGenericPacketDecomService createHybridGenericPacketDecomService() {
        return new HybridGenericPacketDecomService(appContext);
    }

    /**
     * Creates or gets the singleton INenStatusDecomService bean.
     * 
     * @return INenStatusDecomService bean
     */
    @Bean(name = EhaApiBeans.NEN_STATUS_DECOM_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public INenStatusDecomService createNenStatusDecomService() {
        return new NenStatusDecomService(appContext);
    }

    /**
     * Creates or gets the singleton IPrechannelizedPublisherService bean.
     * 
     * @return IPrechannelizedPublisherService bean
     */
    @Bean(name = EhaApiBeans.PRECHANNELIZED_PUBLISHER_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public IPrechannelizedPublisherService createPrechannelizedPublisherService() {
        return new PrechannelizedPublisherService(appContext);
    }

    /**
     * Creates or gets the singleton IFrameHeaderChannelizerService bean.
     * 
     * @return IFrameHeaderChannelizerService bean
     */
    @Bean(name = EhaApiBeans.FRAME_HEADER_CHANNELIZER)
    @Scope("singleton")
    @Lazy(value = true)
    public IFrameHeaderChannelizerService createFrameHeaderChannelizer() {
        return new FrameHeaderChannelizerService(appContext);
    }

    /**
     * Creates or gets the singleton IDsnMonitorDecomService bean.
     * 
     * @return IDsnMonitorDecomService bean
     */
    @Bean(name = EhaApiBeans.MONITOR_DATA_CHANNEL_PROCESSOR)
    @Scope("singleton")
    @Lazy(value = true)
    public IDsnMonitorDecomService createMonitorDataChannelProcessor() {
        return new DsnMonitorDecomService(appContext);
    }

    /**
     * Creates or gets the singleton IChannelLadService bean.
     * 
     * @return IChannelLadService bean
     */
    @Bean(name = EhaApiBeans.CHANNEL_LAD_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public IChannelLadService createChannelLadService() {
        return new ChannelLadService(appContext);
    }

    /**
     * Creates or gets the singleton IChannelPublisherUtility bean.
     * 
     * @return IChannelPublisherUtility bean
     */
    @Bean(name = EhaApiBeans.CHANNEL_PUBLISHER_UTILITY)
    @Scope("singleton")
    @Lazy(value = true)
    public IChannelPublisherUtility getEhaPublisherUtility() {
        return new ChannelPublisherUtility(appContext);
    }

    /**
     * Creates or gets the singleton IChannelLad bean.
     * 
     * @param chanProvider
     *            the current channel definition provider bean, autowired
     * @param timeStrategy
     *            the current time comparison strategy bean, autowired
     * 
     * @return IChannelLad bean
     */
    @Bean(name = EhaApiBeans.CHANNEL_LAD)
    @Scope("singleton")
    @Lazy(value = true)
    public IChannelLad getChannelLad(final IChannelDefinitionProvider chanProvider,
                                     final TimeComparisonStrategyContextFlag timeStrategy) {
        return new ChannelLad(chanProvider, timeStrategy);
    }
    
    /**
     * Creates or gets the singleton IEhaFeatureManager bean.
     * 
     * @return IEhaFeatureManager bean
     */
    @Bean(name=EhaApiBeans.EHA_FEATURE_MANAGER, destroyMethod="")
    @Scope("singleton")
    @Lazy(value = true)
    public IEhaFeatureManager getEhaFeatureManager() {
        return new EhaFeatureManager();
    }

}
