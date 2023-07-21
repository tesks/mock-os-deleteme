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
package jpl.gds.telem.process.app.bootstrap;

import jpl.gds.common.config.bootstrap.ChannelLadBootstrapConfiguration;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.eha.api.channel.alarm.IAlarmHistoryFactory;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.app.AlarmHistoryBootstrapper;
import jpl.gds.telem.common.app.ChannelLadBootstrapper;
import jpl.gds.telem.process.IProcessWorker;
import jpl.gds.telem.process.ProcessConfiguration;
import jpl.gds.telem.process.app.TelemetryProcessWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

/**
 * Telemetry Processor Worker Spring Bootstrap
 *
 */
@Configuration
public class TelemetryProcessWorkerBootstrap {

    /** process configuration bean name */
    public static final String PROCESS_CONFIGURATION = "PROCESS_CONFIGURATION";

    /** dictionary properties bean name */
    public static final  String DICTIONARY_PROPERTIES = "DICTIONARY_PROPERTIES";

    private static final String SECURE_CLASS_LOADER   = "SECURE_CLASS_LOADER";
    protected static final String WORKER_BOOTSTRAP_LAD_CONFIG = "WORKER_BOOTSTRAP_LAD_CONFIG";
    protected static final String WORKER_ALARM_HIST_BOOTSTRAPPER = "WORKER_ALARM_HIST_BOOTSTRAPPER";
    protected static final String WORKER_LAD_BOOTSTRAPPER = "WORKER_LAD_BOOTSTRAPPER";

    private ApplicationContext                appContext;

    /**
     * Autowires the spring application context
     *
     * @param appContext spring application context
     */
    @Autowired
    public void setAppContext(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    /**
     * Creates and returns a telemetry process worker bean
     *
     * @param sessionConfig session configuration
     * @param foreignContextKey Foreign context key
     * @param timeComparisonStrategyContextFlag Time comparison strategy
     * @param processConfig Process configuration peoperties
     * @param classLoader The parent servers secure classloader
     *
     * @return Telemetry Process Worker bean
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public IProcessWorker getProcessWorker(SessionConfiguration sessionConfig, IContextKey foreignContextKey,
                                        TimeComparisonStrategyContextFlag timeComparisonStrategyContextFlag,
                                        ProcessConfiguration processConfig, final AmpcsUriPluginClassLoader classLoader) {
        return new TelemetryProcessWorker(appContext, timeComparisonStrategyContextFlag, sessionConfig,
                foreignContextKey, processConfig, classLoader);
    }

    /**
     * Creates and returns the process configuration bean
     *
     * @param appContext Application context
     * @param missionProperties mission properties
     * @param sseFlag           sse context flag
     * @param msgServiceConfig  message service configuration
     * @return ProcessConfiguration bean
     */
    @Bean(PROCESS_CONFIGURATION)
    @Lazy
    public ProcessConfiguration getProcessConfig(ApplicationContext appContext, MissionProperties missionProperties,
                                                 SseContextFlag sseFlag,
                                                 MessageServiceConfiguration msgServiceConfig) {
        return new ProcessConfiguration(missionProperties, sseFlag, msgServiceConfig,
                appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
    }

    /**
     * Creates and returns a dictionary properties bean
     *
     * @return Dictionary Properties bean
     */
    @Bean(DICTIONARY_PROPERTIES)
    public DictionaryProperties getDictProps() {
        return new DictionaryProperties(true);
    }

    /**
     * Bootstrap channel LAD and alarm history configuration. Each TP worker needs its own copy.
     *
     * @return channel lad bootstrap config
     */
    @Bean(WORKER_BOOTSTRAP_LAD_CONFIG)
    @Primary
    public ChannelLadBootstrapConfiguration bootstrapConfiguration() {
        return new ChannelLadBootstrapConfiguration();
    }

    /**
     * Alarm history bootstrapper. Each TP worker needs its own instance.
     *
     * @param alarmHistoryFactory       alarm history factory
     * @param channelDefinitionProvider channel definition provider
     * @param context                   spring app context
     * @param config                    channel lad bootstrap config
     * @return alarm history bootstrapper
     */
    @Bean(WORKER_ALARM_HIST_BOOTSTRAPPER)
    @Primary
    public AlarmHistoryBootstrapper bootstrapper(final IAlarmHistoryFactory alarmHistoryFactory,
                                                 final IChannelDefinitionProvider channelDefinitionProvider,
                                                 final ApplicationContext context,
                                                 final ChannelLadBootstrapConfiguration config) {
        return new AlarmHistoryBootstrapper(alarmHistoryFactory,
                channelDefinitionProvider,
                context,
                config);
    }

    /**
     * Channel LAD bootstrapper. Each TP worker needs its own instance.
     *
     * @param timeComparisonStrategyContextFlag time comparison strategy
     * @param context                           spring app context
     * @param config                            lad bootstrap config
     * @return channel lad bootstrapper
     */
    @Bean(WORKER_LAD_BOOTSTRAPPER)
    @Primary
    public ChannelLadBootstrapper ChannelLadBootstrapper(
            final TimeComparisonStrategyContextFlag timeComparisonStrategyContextFlag,
            final ApplicationContext context,
            final ChannelLadBootstrapConfiguration config) {
        return new ChannelLadBootstrapper(timeComparisonStrategyContextFlag,
                context,
                config);
    }

}
