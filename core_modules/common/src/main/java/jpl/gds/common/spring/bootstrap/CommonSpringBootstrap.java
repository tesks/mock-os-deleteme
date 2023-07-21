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
package jpl.gds.common.spring.bootstrap;

import java.io.IOException;
import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.bootstrap.ChannelLadBootstrapConfiguration;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.connection.FourConnectionMap;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.gdsdb.DatabaseProperties;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.mission.RealtimeRecordedConfiguration;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.eu.EUCalculationFactory;
import jpl.gds.common.eu.IEUCalculationFactory;
import jpl.gds.common.notify.NotificationProperties;
import jpl.gds.common.websocket.IWebSocketConnectionManager;
import jpl.gds.common.websocket.WebSocketConnectionHandler;
import jpl.gds.common.websocket.WebSocketConnectionManager;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.alarm.AlarmDictionaryClientUtility;
import jpl.gds.dictionary.api.client.alarm.IAlarmDictionaryManager;
import jpl.gds.dictionary.api.client.apid.ApidUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.apid.IApidUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.client.channel.ChannelDictionaryClientUtility;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.command.CommandUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.command.ICommandUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.decom.ChannelDecomUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.decom.IChannelDecomUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.evr.EvrUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.evr.IEvrUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.frame.ITransferFrameUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.frame.TransferFrameUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.sequence.ISequenceUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.sequence.SequenceUtilityDictionaryManager;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.shared.config.WebGuiProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.socket.WebSocketHandler;


/**
 * Spring configuration class for beans in the common project. Also defines
 * dictionary-related beans.
 * 
 *
 * @since R8
 */
@Configuration
public class CommonSpringBootstrap {

	/**
	 * Bean name for transfer frame dictionary provider bean.
	 */
	public static final String FRAME_DEF_PROVIDER = "FRAME_DEF_PROVIDER";
	/**
	 * Bean name for APID dictionary provider bean.
	 */
	public static final String APID_DEF_PROVIDER = "APID_DEF_PROVIDER";
	/**
	 * Bean name for channel dictionary manager bean.
	 */
	public static final String CHANNEL_DICT_MANAGER = "CHANNEL_DICT_MANAGER";
	/**
	 * Bean name for channel dictionary provider bean. Note that this just
	 * redirects to the channel dictionary manager bean, which extends the
	 * provider interface.
	 */
	public static final String CHANNEL_DEF_PROVIDER = CHANNEL_DICT_MANAGER;
	    /**
     * Bean name for EVR dictionary manager bean.
     */
    public static final String EVR_DICT_MANAGER                 = "EVR_DICT_MANAGER";
    /**
     * Bean name for EVR dictionary provider bean. Note that this just redirects
     * to the channel dictionary manager bean, which extends the provider
     * interface.
     */
    public static final String EVR_DEF_PROVIDER                 = EVR_DICT_MANAGER;
    /**
     * Bean name for the command dictionary provider bean.
     */
	public static final String COMMAND_DEF_PROVIDER = "COMMAND_DEF_PROVIDER";

	/**
	 * Bean name for the alarm dictionary manager bean.
	 */
	public static final String ALARM_DICT_MANAGER = "ALARM_DICT_MANAGER";
	/**
	 * Bean name for the alarm dictionary provider bean. Note that this just
	 * redirects to the alarm dictionary manager bean, which extends the
	 * provider interface.
	 */
	public static final String ALARM_DEF_PROVIDER = ALARM_DICT_MANAGER;
	/**
	 * Bean name for the sequence dictionary provider bean.
	 */
	public static final String SEQUENCE_DEF_PROVIDER = "SEQUENCE_DEF_PROVIDER";
	/**
	 * Bean name for the decom dictionary provider bean.
	 */
	public static final String DECOM_DEF_PROVIDER = "DECOM_DEF_PROVIDER";
	/**
	 * Bean name for the MissionProperties bean.
	 */
	public static final String MISSION_PROPERTIES = "MISSION_PROPERTIES";
	/**
	 * Bean name for the ConnectionProperties bean.
	 */
	public static final String CONNECTION_PROPERTIES = "CONNECTION_PROPERTIES";
	/**
	 * Bean name for the GeneralProperties bean.
	 */
	public static final String GENERAL_PROPERTIES = "GENERAL_PROPERTIES";
	/**
	 * Bean name for the NotificationProperties bean.
	 */
	public static final String NOTIFICATION_PROPERTIES = "NOTIFICATION_PROPERTIES";
	/**
	 * Bean name for the SecurityProperties bean.
	 */
	public static final String SECURITY_PROPERTIES = "SECURITY_PROPERTIES";
	/**
	 * Bean name for the RealtimeRecordedConfiguration bean.
	 */
	public static final String REALTIME_RECORDED_CONFIG = "REALTIME_RECORDED_CONFIG";
	/**
	 * Bean name for the IConnectionMap bean. 
	 */
	public static final String CONNECTION_CONFIG = "CONNECTION_CONFIG";
	/**
	 * Bean name for AccessControlParameters.
	 */
	public static final String ACCESS_CONTROL_PARAMS = "ACCESS_CONTROL_PARAMS";
    /**
     * Bean name for the DatabaseProperties bean.
     */
    public static final String DATABASE_PROPERTIES = "DATABASE_PROPERTIES";
    /**
     * Bean name for the EU calculation factory bean.
     */
    public static final String EU_CALCULATION_FACTORY = "EU_CALCULATION_FACTORY";
    /**
     * Bean name for the ChannelLadBootstrapConfiguration bean.
     */
    public static final String LAD_BOOTSTRAP_PROPERTIES = "LAD_BOOTSTRAP_PROPERTIES";
    /**
     * Bean name for the Secure Class Loader bean.
     */
    public static final String SECURE_CLASS_LOADER = "SECURE_CLASS_LOADER";
    /**
	 * Bean name for the Web Socket Connection Manager bean.
	 */
    public static final String WEBSOCKET_CONNECTION_MANAGER = "WEBSOCKET_CONNECTION_MANAGER";
    /**
	 * Bean name for the Web Socket Handler bean.
	 */
    private static final String WEBSOCKET_HANDLER = "WEBSOCKET_HANDLER";

	@Autowired
    ApplicationContext         appContext;
   
	/**
	 * Gets the singleton IChannelDictionaryManager bean. Note that this manager
	 * is NOT populated with any channel definitions. No dictionaries are read.
	 * Autowiring results in the creation and loading of the DictionaryConfiguration
	 * bean the first time this is called.
	 * 
	 * @return IChannelDictionaryManager bean
	 */
	@Bean(name = CHANNEL_DICT_MANAGER)
	@Scope("singleton")
	@Lazy(value = true)
	public IChannelUtilityDictionaryManager getChannelDictionaryManager() {
		return new ChannelDictionaryClientUtility(appContext);

	}

    /**
     * Gets the singleton IEvrDictionaryManager bean. Note that this manager is
     * NOT populated with any channel definitions. No dictionaries are read.
     * Autowiring results in the creation and loading of the DictionaryConfiguration
     * bean the first time this is called. 
     * 
     * @param dictConfig
     *            current DictionaryConfiguration bean, autowired
     * @return IEvrDictionaryManager bean
     */
    @Bean(name = EVR_DICT_MANAGER)
    @Scope("singleton")
    @Lazy(value = true)
    public IEvrUtilityDictionaryManager getEvrDictionaryManager(
            final DictionaryProperties dictConfig) {
        return new EvrUtilityDictionaryManager(appContext);
  
    }

	/**
	 * Gets the singleton IAlarmDictionaryManager bean. Note that this manager
	 * is NOT populated with any alarm definitions. Autowiring results in the
	 * creation and loading of the DictionaryConfiguration bean the first time
	 * this is called.
	 * 
	 * @return IAlarmDictionaryManager bean
	 */
	@Bean(name = ALARM_DICT_MANAGER)
	@Scope("singleton")
	@Lazy(value = true)
	public IAlarmDictionaryManager getAlarmDictionaryManager() {
        return new AlarmDictionaryClientUtility(appContext, true);

	}

	/**
     * Gets the singleton SecurityProperties bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return SecurityProperties bean
     */
	@Bean(name = SECURITY_PROPERTIES)
	@Scope("singleton")
	@Lazy(value = true)
    public SecurityProperties getSecurityProperties(final SseContextFlag sseFlag) {
        return new SecurityProperties(sseFlag);
	}
	
	/**
	 * Gets the singleton AccessControlParameters bean.
	 * 
	 * @return AccessControlParameters bean
	 */
	@Bean(name = ACCESS_CONTROL_PARAMS)
	@Scope("singleton")
	@Lazy(value = true)
	public AccessControlParameters getAccessControlParameters() {
		return new AccessControlParameters();
	}

	/**
     * Gets the singleton NotificationProperties bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return NotificationProperties bean
     */
	@Bean(name = NOTIFICATION_PROPERTIES)
	@Scope("singleton")
	@Lazy(value = true)
    public NotificationProperties getNotificationProperties(final SseContextFlag sseFlag) {
        return new NotificationProperties(sseFlag, TraceManager.getTracer(appContext, Loggers.NOTIFIER));
	}

	/**
     * Gets the singleton GeneralProperties bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return GeneralProperties bean
     */
	@Bean(name = GENERAL_PROPERTIES)
	@Scope("singleton")
	@Lazy(value = true)
    public GeneralProperties getGeneralProperties(final SseContextFlag sseFlag) {
        return new GeneralProperties(sseFlag);
	}

	/**
     * Gets the singleton ConnectionProperties bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return ConnectionProperties bean
     */
	@Bean(name = CONNECTION_PROPERTIES)
	@Scope("singleton")
	@Lazy(value = true)
    public ConnectionProperties getConnectionProperties(final SseContextFlag sseFlag) {
        return new ConnectionProperties(sseFlag);
	}

	/**
     * Gets the singleton MissionProperties bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return MissionProperties bean
     */
	@Bean(name = MISSION_PROPERTIES)
	@Scope("singleton")
	@Lazy(value = true)
    public MissionProperties getMissionProperties(final SseContextFlag sseFlag) {
        return new MissionProperties(sseFlag);
	}

	/**
     * Gets the singleton IApidDefProvider bean. Autowiring will cause the
     * creation or fetch of the DictionaryConfiguration bean when this is called
     * the first time, if it has not already been created. The
     * DictionaryConfiguration bean should be fetched and populated before this
     * is called, or the wrong dictionary may be loaded.
     * 
     * @param dictConfig
     *            the current DictionaryConfiguration bean, autowired
     * @param sseDictLoading
     *            - sse load strategy, autowired
     * @param flightDictLoading
     *            - flight load strategy, autowired
     * @param cache
     *            - The dictionary cache
     * @param sseFlag
     *            - SSE context flag
     * @return IApidDefProvider bean
     */
	@Bean(name = APID_DEF_PROVIDER)
	@Scope("singleton")
	@Lazy(value = true)
    public IApidUtilityDictionaryManager getApidDictionaryManager(final DictionaryProperties dictConfig,
                                                                  final SseDictionaryLoadingStrategy sseDictLoading,
                                                                  final FlightDictionaryLoadingStrategy flightDictLoading,
                                                                  final IDictionaryCache cache,
                                                                  final SseContextFlag sseFlag) {
		
        final boolean isSse = sseFlag.isApplicationSse();
		final boolean isEnabled = isSse ? sseDictLoading.isApidEnabled() : flightDictLoading.isApidEnabled();

		return new ApidUtilityDictionaryManager(cache, dictConfig, isEnabled, isSse);
	}

	/**
	 * Gets the singleton IChannelDecomDictionary bean. Autowiring will cause
	 * the creation or fetch of the DictionaryConfiguration, MissionProperties,
	 * and IChannelDefinitionProvider beans when this is called the first time,
	 * if they have not already been created. The DictionaryConfiguration bean
	 * should be fetched and populated before this is called, or the wrong
	 * dictionary may be loaded.
	 * 
	 * @param dictConfig
	 *            the current DictionaryConfiguration bean, autowired
	 * @param missionProperties
	 *            the current MissionProperties bean, autowired
	 * @param sseDictLoading - sse load strategy, autowired
     * @param flightDictLoading - flight load strategy, autowired
     * @param cache - The dictionary cache
	 * 
	 * @return IChannelDecomDictionary bean
	 */
	@Bean(name = DECOM_DEF_PROVIDER)
	@Scope("singleton")
	@Lazy(value = true)
	public IChannelDecomUtilityDictionaryManager getDecomDictionary(
			final DictionaryProperties dictConfig,
			final MissionProperties missionProperties,
			final SseDictionaryLoadingStrategy sseDictLoading, 
			final FlightDictionaryLoadingStrategy flightDictLoading,
			final IDictionaryCache cache) {
		
		// This does not seem to care about is sse, so assuming it is always false.
		/**
		boolean isSse = GdsSystemProperties.applicationIsSse();
		boolean isEnabled = isSse ? sseDictLoading.isApidEnabled() : flightDictLoading.isApidEnabled();
		*/
		final boolean isSse = false;
		final boolean isEnabled = flightDictLoading.isDecomEnabled();
		
		/**
		 * This did not do anything so ?
		if (!missionProperties.areEvrsEnabled()
				&& !missionProperties.isEhaEnabled()) {

		}
		 */
		return new ChannelDecomUtilityDictionaryManager(dictConfig, cache, isEnabled, isSse);
	}

	/**
	 * Gets the singleton ISequenceDictionary bean. Autowiring will cause the
	 * creation or fetch of the DictionaryConfiguration bean
	 * when this is called the first time, if they have
	 * not already been created. The DictionaryConfiguration bean should be
	 * fetched and populated before this is called, or the wrong dictionary may
	 * be loaded.
	 * 
	 * @param dictConfig
	 *            the current DictionaryConfiguration bean, autowired
	 * @param flightDictLoading - flight load strategy, autowired
     * @param cache - The dictionary cache
	 * 
	 * @return ISequenceUtilityDictionaryManager bean
	 */
	@Bean(name = SEQUENCE_DEF_PROVIDER)
	@Scope("singleton")
	@Lazy(value = true)
	public ISequenceUtilityDictionaryManager getSequenceDefinitionProvider(
			final DictionaryProperties dictConfig,
			final FlightDictionaryLoadingStrategy flightDictLoading,
			final IDictionaryCache cache) {
		
		final boolean isEnabled = flightDictLoading.isSequenceEnabled();		
		return new SequenceUtilityDictionaryManager(dictConfig, cache, isEnabled);
	}

	/**
	 * Gets the singleton ITransferFrameDictionary bean. Autowiring will cause
	 * the creation or fetch of the DictionaryConfiguration bean when this is
	 * called the first time, if it has not already been created. The
	 * DictionaryConfiguration bean should be fetched and populated before this
	 * is called, or the wrong dictionary may be loaded.
	 * 
	 * @param dictConfig
	 *            the current DictionaryConfiguration bean, autowired
	 * @param flightDictLoading - flight load strategy, autowired
     * @param cache - The dictionary cache
	 * 
	 * @return ITransferFrameDictionary bean
	 */
	@Bean(name = FRAME_DEF_PROVIDER)
	@Scope("singleton")
	@Lazy(value = true)
	public ITransferFrameUtilityDictionaryManager getTransferFrameDictionary(
			final DictionaryProperties dictConfig,
			final FlightDictionaryLoadingStrategy flightDictLoading,
			final IDictionaryCache cache) {
		
		final boolean isEnabled = flightDictLoading.isFrameEnabled();
		return new TransferFrameUtilityDictionaryManager(dictConfig, cache, isEnabled);
	}

	/**
	 * Gets the singleton ICommandDictionary bean. Autowiring will cause the
	 * creation or fetch of the DictionaryConfiguration bean 
	 * when this is called the first time, if they have
	 * not already been created. The DictionaryConfiguration bean should be
	 * fetched and populated before this is called, or the wrong dictionary may
	 * be loaded. 
	 * 
	 * @param dictConfig
	 *            the current DictionaryConfiguration bean, autowired
	 * @param flightDictLoading - flight load strategy, autowired
     * @param cache - The dictionary cache
     * 
	 * 
	 * @return ICommandDictionaryManager bean
	 */
	@Bean(name = COMMAND_DEF_PROVIDER)
	@Scope("singleton")
	@Lazy(value = true)
	public ICommandUtilityDictionaryManager getCommandDictionary(
			final DictionaryProperties dictConfig,
			final FlightDictionaryLoadingStrategy flightDictLoading,
			final IDictionaryCache cache)  {
		
		final boolean isEnabled = flightDictLoading.isCommandEnabled();
		return new CommandUtilityDictionaryManager(dictConfig, cache, isEnabled);
	}

	/**
	 * Gets the singleton RealtimeRecordedConfiguration bean. Note that the
	 * IApidDictionary and MissionProperties beans will be created and loaded
	 * the first time this is called. The DictionaryConfiguration bean should be
	 * fetched and populated before this is called, or the wrong dictionary may
	 * be loaded.
	 * 
	 * @param missionProps
	 *            the current MissionProperties bean, autowired
	 * @param apidDefs
	 *            the current IApidDictionary bean, autowired
	 * 
	 * @return RealtimeRecordedConfiguration bean
	 */
	@Bean(name = REALTIME_RECORDED_CONFIG)
	@Scope("singleton")
	@Lazy(value = true)
	public RealtimeRecordedConfiguration getRealtimeRecordedConfiguration(
			final MissionProperties missionProps, final IApidDefinitionProvider apidDefs) {
        return new RealtimeRecordedConfiguration(apidDefs, missionProps,
                                                 TraceManager.getTracer(appContext, Loggers.CONFIG));
	}

	/**
     * Gets the singleton IConnectionMap bean. Autowiring causes the
     * ConnectionProperties and MissionProperties objects to be created and
     * loaded the first time this is invoked.
     * 
     * @param connectionProps
     *            the current ConnectionProperties bean, autowired
     * @param missionProps
     *            the current MissionProperties bean, autowired
     * @param sseFlag
     *            the SSE context flag
     * @return IConnectionMap bean
     */
	@Bean(name = CONNECTION_CONFIG)
	@Scope("singleton")
	@Lazy(value = true)
    public IConnectionMap getConnectionConfiguration(final ConnectionProperties connectionProps,
                                                     final MissionProperties missionProps,
                                                     final SseContextFlag sseFlag) {
        return new FourConnectionMap(connectionProps, missionProps, sseFlag);
	}
	

    /**
     * Gets the singleton DatabaseConfiguration bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return IDatabaseProperties bean
     */
    @Bean(name = DATABASE_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = true)
    public IDatabaseProperties getDatabaseProperties(final SseContextFlag sseFlag) {
        return new DatabaseProperties(sseFlag);
    }
    
    /**
     * Gets the singleton IEUCalculationFactory bean.
     * @param appContext the current application context, autowired
     * 
     * @return IEUCalculationFactory bean
     */
    @Bean(name = EU_CALCULATION_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IEUCalculationFactory getEUCalculationFactory(final ApplicationContext appContext) {
        return new EUCalculationFactory(appContext);
    }
    
    
    /**
     * Gets the singleton ChannelLadBootstrapConfiguration bean.
     * 
     * @return ChannelLadBootstrapConfiguration bean
     * 
     */
    @Bean(name = LAD_BOOTSTRAP_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = true)
    public ChannelLadBootstrapConfiguration getLadBootstrapConfiguration() {
        return new ChannelLadBootstrapConfiguration();
    }

    /**
     * @return the secure classloader
     */
    @Bean(name = SECURE_CLASS_LOADER)
    @Scope("singleton")
    @Lazy(value = true)
    public AmpcsUriPluginClassLoader secureClassLoader() {
        return new AmpcsUriPluginClassLoader(appContext.getClassLoader(),
                                             TraceManager.getTracer(appContext, Loggers.UTIL));
    }

	/**
	 * Gets the Websocket Connection Manager
	 *
	 * @return the IWebSocketConnectionManager
	 */
	@Bean(name = WEBSOCKET_CONNECTION_MANAGER)
	@Scope("singleton")
	@Lazy(value = true)
	public IWebSocketConnectionManager getWebsocketConnectionManager() {
    	return new WebSocketConnectionManager(TraceManager.getTracer(appContext, Loggers.WEBSOCKET));
	}

	/**
	 * Gets the Websocket handler
	 *
	 * @return the WebSocketHandler
	 */
	@Bean(name = WEBSOCKET_HANDLER)
	@Scope("prototype")
	@Lazy(value = true)
	public WebSocketHandler getWebsocketHandler() {
		return new WebSocketConnectionHandler(TraceManager.getTracer(appContext, Loggers.WEBSOCKET));
	}

	 /**
	 * Gets the WebMvcConfigurerAdapter bean which has been
	 * overridden to enable CORS (Cross-Origin Resource Sharing)
	 * for Restful web services like TI, TP, CFDP, etc.
	 *
	 * @return the WebMvcConfigurerAdapter bean
	 */
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**");
			}
		};
	}

	/**
	 * Gets the WebMvcConfigurerAdapter bean which configures the
	 * mappings necessary for serving the MC GUI code from disk
	 *
	 * @param webGuiProperties the properties file which contains the
	 *                         Web GUI configuration parameters
	 * @return the WebMvcConfigurerAdapter bean
	 */
	@Bean
	// chill_cfdp_processor should host the chill_cfdp Web GUI
	// The following conditional annotation allows us to load this Bean only when the
	// GdsAppName is chill_telem_ingest or chill_cfdp_processor. Chose this approach over
	// duplicate code in two separate Bootstrap files.
	@ConditionalOnExpression("'${GdsAppName}' == 'chill_telem_ingest' or '${GdsAppName}' == 'chill_cfdp_processor'")
	public WebMvcConfigurer webResourceConfigurer(final WebGuiProperties webGuiProperties) {

		final String webGuiHandler = webGuiProperties.getResourceHandler();
		final String webGuiLocation = webGuiProperties.getResourceLocation();
		final String webGuiRedirectRoutes = webGuiProperties.getResourceRedirectRoutes();

		final String resourceHandlerPattern = webGuiHandler + "/**";
		final String resourceLocation = "file:" + webGuiLocation;
		final String indexViewRedirect = "redirect:" + webGuiHandler + "/index.html";
		final String indexViewForward = "forward:" + webGuiHandler + "/index.html";

		return new WebMvcConfigurerAdapter() {

			@Override
			public void addResourceHandlers(ResourceHandlerRegistry resourceHandlerRegistry) {
				// Map handler "/ampcs/**" to location "file:/ammos/ampcs/services/mcgui/"
				resourceHandlerRegistry
						.addResourceHandler(resourceHandlerPattern)
						.addResourceLocations(resourceLocation)
						.resourceChain(true)
						.addResolver(new PathResourceResolver() {
							@Override
							protected Resource getResource(String resourcePath, Resource location) throws IOException {
								Resource requestedResource = location.createRelative(resourcePath);
								return requestedResource.exists() && requestedResource.isReadable() ? requestedResource
									: new ClassPathResource(resourceLocation + "index.html");
							}
						});
			}


			@Override
			public void addViewControllers(ViewControllerRegistry registry) {
				// These define the 2 base routes
				// Redirect http://<host>:<port>/ampcs -> /ampcs/index.html
				registry.addViewController(webGuiHandler).setViewName(indexViewRedirect);
				// forward to allow Angular to handle routing
				// Forward http://<host>:<port>/ampcs/fsw -> /ampcs/index.html - to load all css and js
				registry.addViewController(webGuiHandler + "/").setViewName(indexViewForward);
				registry.addViewController(webGuiHandler + "/fsw*").setViewName(indexViewForward);
				registry.addViewController(webGuiHandler + "/sse*").setViewName(indexViewForward);
				registry.addViewController(webGuiHandler + "/cfdp/*").setViewName(indexViewForward);
			}
		};
	}
}