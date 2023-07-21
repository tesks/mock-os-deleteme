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
package jpl.gds.monitor.guiapp.app;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.config.MonitorDictionaryUtility;
import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.guiapp.MonitorTimers;
import jpl.gds.monitor.guiapp.channel.ChannelMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.gui.MonitorMessageController;
import jpl.gds.monitor.guiapp.gui.views.nattable.EvrEventSubscriber;
import jpl.gds.monitor.perspective.view.channel.MonitorChannelLad;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.TimeProperties;
/**
 * This is the spring bootstrap configuration class for the chill_monitor GUI.  
 * It should be loaded by the chill_monitor application and related unit
 * tests only. Beans defined by this class are not intended to be overridden
 * or adapted in any way.
 *
 */
@Configuration
public class ChillMonitorSpringBootstrap {

	/** Name for the monitor GUI properties bean */
	public static final String MONITOR_GUI_PROPERTIES = "MONITOR_GUI_PROPERTIES";
	/** Name for the monitor dictionary utility bean */
	public static final String MONITOR_DICTIONARY_UTIL = "MONITOR_DICTIONARY_UTIL";
	/** Name for the general message distributor bean */
	public static final String GENERAL_MESSAGE_DISTRIBUTOR = "GENERAL_MESSAGE_DISTRIBUTOR";
	/** Name for the channel message distributor bean */
	public static final String CHANNEL_MESSAGE_DISTRIBUTOR = "CHANNEL_MESSAGE_DISTRIBUTOR";
	/** Name for the monitor message controller bean */
	public static final String MONITOR_MESSAGE_CONTROLLER = "MONITOR_MESSAGE_CONTROLLER";
	/** Name for the monitor config values bean */
	public static final String MONITOR_CONFIG_VALUES = "MONITOR_CONFIG_VALUES";
	/** Name for the monitor timers bean */
	public static final String MONITOR_TIMERS = "MONITOR_TIMERS";
	/** Name for the monitor channel LAD bean */
	public static final String MONITOR_CHANNEL_LAD = "MONITOR_CHANNEL_LAD";
	/** Name for the EVR event subscriber bean bean */
	public static final String EVR_EVENT_SUBSCRIBER = "EVR_EVENT_SUBSCRIBER";
    
    /**
     * Gets the Monitor GUI configuration
     * 
     * @param sseFlag
     *            The SSE context flag
     * @return MonitorGuiProperties
     */
	@Bean(name=MONITOR_GUI_PROPERTIES) 
	@Scope("singleton")
	@Lazy(value = true)
    public MonitorGuiProperties getMonitorGuiProperties(final SseContextFlag sseFlag) {
        return new MonitorGuiProperties(sseFlag);
	}
	

	@Bean(name=MONITOR_DICTIONARY_UTIL) 
	@Scope("singleton")
	@Lazy(value = true)
	public MonitorDictionaryUtility getMonitorDictionaryUtility(final ApplicationContext appContext) throws BeansException, InvalidMetadataException {
		final MonitorDictionaryUtility mdu = new MonitorDictionaryUtility();
		mdu.init(appContext);
		return mdu;	
	}
	
	@Bean(name=GENERAL_MESSAGE_DISTRIBUTOR) 
	@Scope("singleton")
	@Lazy(value = true)
	public GeneralMessageDistributor getGeneralMessageDistributor(final ApplicationContext appContext) throws BeansException, InvalidMetadataException {
		return new GeneralMessageDistributor(appContext);
	}
	
	@Bean(name=CHANNEL_MESSAGE_DISTRIBUTOR) 
	@Scope("singleton")
	@Lazy(value = true)
	public ChannelMessageDistributor getChannelMessageDistributor(final ApplicationContext appContext) throws BeansException, InvalidMetadataException {
		return new ChannelMessageDistributor(appContext);
	}
	
	@Bean(name=MONITOR_MESSAGE_CONTROLLER) 
	@Scope("singleton")
	@Lazy(value = true)
	public MonitorMessageController getMonitorMessageController(final ApplicationContext appContext) throws BeansException, InvalidMetadataException {
		return new MonitorMessageController(appContext);
	}
	

	@Bean(name=MONITOR_CONFIG_VALUES) 
	@Scope("singleton")
	@Lazy(value = true)
	public MonitorConfigValues getMonitorConfigValues(final MonitorGuiProperties guiProps) throws BeansException, InvalidMetadataException {
		return new MonitorConfigValues(guiProps, TimeProperties.getInstance());
	}
	
	@Bean(name=MONITOR_TIMERS) 
	@Scope("singleton")
	@Lazy(value = true)
	public MonitorTimers getMonitorTimers(final MonitorConfigValues configVals) throws BeansException, InvalidMetadataException {
		return new MonitorTimers(configVals);
	}
	

	@Bean(name=EVR_EVENT_SUBSCRIBER) 
	@Scope("singleton")
	@Lazy(value = true)
	public EvrEventSubscriber getEvrEventSubscriber(final ApplicationContext appContext) throws BeansException, InvalidMetadataException {
		return new EvrEventSubscriber(appContext);
	}
	
	@Bean(name=MONITOR_CHANNEL_LAD) 
	@Scope("singleton")
	@Lazy(value = true)
	public MonitorChannelLad getMonitorChannelLad(final ApplicationContext appContext){
		return new MonitorChannelLad(appContext);
	}	
}

