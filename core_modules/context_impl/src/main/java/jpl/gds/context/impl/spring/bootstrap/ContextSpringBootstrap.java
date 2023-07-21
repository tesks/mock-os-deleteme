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
package jpl.gds.context.impl.spring.bootstrap;

import jpl.gds.context.api.message.util.IContextMessageFactory;
import jpl.gds.context.impl.message.util.ContextMessageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.EnableFswDownlinkContextFlag;
import jpl.gds.context.api.EnableLstContextFlag;
import jpl.gds.context.api.EnableRemoteDbContextFlag;
import jpl.gds.context.api.EnableSseDownlinkContextFlag;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextConfigurationFactory;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.context.api.message.ContextMessageType;
import jpl.gds.context.impl.ContextConfiguration;
import jpl.gds.context.impl.ContextConfigurationFactory;
import jpl.gds.context.impl.ContextFilterInformation;
import jpl.gds.context.impl.ContextIdentification;
import jpl.gds.context.impl.GeneralContextInformation;
import jpl.gds.context.impl.VenueConfiguration;
import jpl.gds.context.impl.message.ContextHeartbeatMessage;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.metadata.context.IContextKey;

/**
 * Spring configuration class for beans in the common project. Also defines
 * dictionary-related beans.
 * 
 *
 * @since R8
 */
@Configuration
public class ContextSpringBootstrap {

    /**
     * Constructor.
     */
    public ContextSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(ContextMessageType.ContextHeartbeat,
                                                                               ContextHeartbeatMessage.XmlParseHandler.class.getName(),
                                                                               null, new String[] { "Heartbeat" }));
    }

	/**
	 * Bean name for the TimeComparisonStrategyContextFlag bean.
	 */
	public static final String CURRENT_TIME_COMPARISON_STRATEGY = "CURRENT_TIME_COMPARISON_STRATEGY";
	/**
	 * Bean name for the IVenueConfiguration bean.
	 */
	public static final String VENUE_CONFIG = "VENUE_CONFIGURATION";
	/**
	 * Bean name for the ISpacecraftFilterInformation bean.
	 */
	public static final String FILTER_INFO = "SPACECRAFT_FILTER_INFO";
	/**
	 * Bean name for the Context Identification bean.
	 */
	public static final String CONTEXT_IDENTIFICATION = "CONTEXT_IDENTIFICATION";
	/**
	 * Bean name for the IGeneralContextInformation bean.
	 */
	public static final String GENERAL_CONTEXT_INFO = "GENERAL_CONTEXT_INFO";
	/**
	 * Bean name for the EnableLstContextFlag bean.
	 */
	public static final String ENABLE_LST_FLAG = "ENABLE_LST_FLAG";
	/**
	 * Bean name for the EnableRemoteDbContextFlag bean.
	 */
	public static final String ENABLE_REMOTE_DB_FLAG = "ENABLE_REMOTE_DB_FLAG";
	/**
	 * Bean name for the SprintfFormatter bean.
	 */
	public static final String SPRINTF_FORMATTER = "SPRINTF_FORMATTER";
	/**
     * Bean name for the IContextConfiguration bean.
     */
    public static final String CONTEXT_CONFIGURATION = "CONTEXT_CONFIGURATION";
	/**
     * Bean name for the IContextConfigurationFactory bean.
     */
    public static final String CONTEXT_CONFIGURATION_FACTORY = "CONTEXT_CONFIGURATION_FACTORY";
    /**
     * Bean name for the EnableFswDownlinkContextFlag bean.
     */
    public static final String ENABLE_FSW_DOWNLINK_FLAG = "ENABLE_FSW_DOWNLINK_FLAG";
    /**
     * Bean name for the EnableSseDownlinkContextFlag bean.
     */
    public static final String ENABLE_SSE_DOWNLINK_FLAG = "ENABLE_SSE_DOWNLINK_FLAG";
	/**
	 * Bean name for the ContextMessageFactory bean
	 */
	public static final String CONTEXT_MESSAGE_UTIL = "CONTEXT_MESSAGE_UTIL";

    @Autowired
    ApplicationContext         appContext;
	
	/**
	 * Gets the singleton CurrentTimeComparisonStrategy bean. Autowiring causes
	 * the GeneralProperties object to be created and loaded the first time this
	 * is invoked.
	 * 
	 * @param genProps
	 *            the current GeneralProperties bean, autowired
	 * @return CurrentTimeComparisonStrategy bean
	 */
	@Bean(name = CURRENT_TIME_COMPARISON_STRATEGY)
	@Scope("singleton")
	@Lazy(value = true)
	public TimeComparisonStrategyContextFlag getCurrentTimeComparisonStrategy(
			final GeneralProperties genProps) {
		return new TimeComparisonStrategyContextFlag(
				genProps.getDefaultTimeComparisonStrategy());
	}
	
	/**
	 * Gets the singleton IVenueConfiguration bean. Autowiring causes the
	 * MissionProperties object to be created and loaded the first time this is
	 * invoked.
	 * 
	 * @param missionProps
	 *            the current MissionProperties bean, autowired
	 * @return IVenueConfiguration bean
	 */
	@Bean(name = VENUE_CONFIG)
	@Scope("singleton")
	@Lazy(value = true)
	public IVenueConfiguration getVenueConfiguration(final MissionProperties missionProps) {
		return new VenueConfiguration(missionProps);
	}
	
	/**
     * Gets the singleton IContextFilterInformation bean.
     * 
     * @return IContextFilterInformation bean
     */
	@Bean(name = FILTER_INFO)
	@Scope("singleton")
	@Lazy(value = true)
    public IContextFilterInformation getSpacecraftFilterInformation() {
        return new ContextFilterInformation();
	}
	
	/**
	 * Gets the singleton IContextIdentification bean.
	 * 
	 * @param key the current IContextKey bean, autowired
	 * @param mprops the MissionProperties bean, autowired
	 *  
	 * @return IContextIdentification bean
	 */
	@Bean(name = CONTEXT_IDENTIFICATION)
	@Scope("singleton")
	@Lazy(value = true)
	public IContextIdentification getContextIdentification(final IContextKey key, final MissionProperties mprops) {
		return new ContextIdentification(mprops, key, mprops.getDefaultScid());
	}
	
	/**
	 * Gets the singleton IGeneralContextInformation bean.
	 * 
	 * @param appContext the current ApplicationContext, autowired
	 * 
	 * @return IGeneralContextInformation bean
	 */
	@Bean(name = GENERAL_CONTEXT_INFO)
	@Scope("singleton")
	@Lazy(value = true)
	public IGeneralContextInformation getGeneralContextInformation(final ApplicationContext appContext) {
		return new GeneralContextInformation(appContext);
	}	

	/**
	 * Gets the singleton EnableLstContextFlag bean.
	 * 
	 * @param appContext the current ApplicationContext, autowired
	 * 
	 * @return EnableLstContextFlag bean
	 */
	@Bean(name = ENABLE_LST_FLAG)
	@Scope("singleton")
	@Lazy(value = true)
	public EnableLstContextFlag getEnableLstFlag(final ApplicationContext appContext) {
		return new EnableLstContextFlag(appContext);
	}
	
	/**
	 * Gets the singleton SprintfFormat bean.
	 * 
	 * @param scidInfo the current ISpacecraftFilterInformation bean, autowired
	 * 
	 * @return SprintFormat bean
	 */
	@Bean(name = SPRINTF_FORMATTER)
	@Scope("singleton")
	@Lazy(value = true)
	public SprintfFormat createSprintfFormatter(final IContextIdentification scidInfo) {
        return new SprintfFormat(scidInfo.getSpacecraftId());
	}
	
	/**
	 * Gets the singleton EnableRemoteDbContextFlag bean.
	 * 
	 * @return EnableRemoteDbContextFlag bean
	 */
	@Bean(name = ENABLE_REMOTE_DB_FLAG)
	@Scope("singleton")
	@Lazy(value = true)
	public EnableRemoteDbContextFlag getEnableRemoteDbContextFlag() {
		return new EnableRemoteDbContextFlag();
	}
	
	/**
     * Gets the singleton IContextConfiguration bean.
     * 
	 * @param appContext the current application context, autowired
     * 
     * @return IContextConfiguration bean
     */
    @Bean(name = CONTEXT_CONFIGURATION)
    @Scope("singleton")
    @Lazy(value = true)
    public IContextConfiguration getContextConfiguration(final ApplicationContext appContext) {
        return new ContextConfiguration(appContext);
    }
    
    /**
     * Gets the singleton context configuration factory bean.
     * 
     * @param appContext the current application context, autowired
     * 
     * @return IContextConfigurationFactory bean
     */
    @Bean(name = CONTEXT_CONFIGURATION_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IContextConfigurationFactory getContextConfigurationFactory(final ApplicationContext appContext) {
        return new ContextConfigurationFactory(appContext);
    }
    
    /**
     * Gets the singleton EnableFswDownlinkContextFlag bean.
     * 
     * @return EnableFswDownlinkContextFlag bean
     */
    @Bean(name = ENABLE_FSW_DOWNLINK_FLAG)
    @Scope("singleton")
    @Lazy(value = true)
    public EnableFswDownlinkContextFlag getEnableFswDownlinkContextFlag() {
        return new EnableFswDownlinkContextFlag();
    }
    
    /**
     * Gets the singleton EnableSseDownlinkContextFlag bean.
     * 
     * @return EnableSseDownlinkContextFlag bean
     */
    @Bean(name = ENABLE_SSE_DOWNLINK_FLAG)
    @Scope("singleton")
    @Lazy(value = true)
    public EnableSseDownlinkContextFlag getEnableSseDownlinkContextFlag() {
        return new EnableSseDownlinkContextFlag();
    }

	/**
	 * Gets the singleton ContextMessageFactory bean
	 *
	 * @return ContextMessageFactory bean
	 */
	@Bean(name = CONTEXT_MESSAGE_UTIL)
	@Scope("singleton")
	@Lazy(value = true)
	public IContextMessageFactory getContextMessageUtility() {
		return new ContextMessageFactory();
	}

}