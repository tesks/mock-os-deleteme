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
package jpl.gds.telem.down;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.down.perspective.view.DownMessageViewConfiguration;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;


/**
 * DownlinkConfiguration contains configuration values used by the downlink
 * application. These values actually reside in the property files or in
 * the user perspective. This is just a convenience class for
 * accessing configuration parameters in the downlink processor. 
 * <p>
 * Changing configuration values in this object will not affect the 
 * configuration or perspective files; those cannot be updated from here.
 *
 *
 */
public class DownConfiguration { 
    private boolean useJms;
    private boolean useDb;
    private long meterInterval;
    private DownMessageViewConfiguration messageViewConfig;
    private DownlinkAppProperties featureSet;
    private final ApplicationContext appContext;
    private final IDatabaseProperties    dbProperties;
  
    /**
     * Creates an instance of DownConfiguration. This triggers
     * loading of the configuration properties.
     * @param appContext the current application context
     */
    public DownConfiguration(final ApplicationContext appContext) {
    	this.featureSet = new DownlinkAppProperties(appContext.getBean(MissionProperties.class), appContext.getBean(SseContextFlag.class));
    	this.appContext = appContext;
    	this.dbProperties = this.appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class);
    	loadDefaultConfig();
    }
    
    /**
     * Loads defaults from the configuration property files.
     */
    private void loadDefaultConfig() {
        this.meterInterval = this.appContext.getBean(TelemetryInputProperties.class).getMeterInterval();
        this.useJms = this.appContext.getBean(MessageServiceConfiguration.class).getUseMessaging();
        this.useDb = this.dbProperties.getUseDatabase(); 
    }

    /**
     * Gets the downlink feature set, indicating which downlink processor features 
     * are enabled and disabled in the configuration.  
     * 
	 * @return the populated feature set object
	 */
	public DownlinkAppProperties getFeatureSet() {
		return featureSet;
	}

	/**
	 * Sets the feature set indicating which downlink processor features 
	 * are enabled or disabled, temporarily overriding the values in the 
	 * configuration.
	 *
	 * @param featureSet The feature set object to set.
	 */
	public void setFeatureSet(final DownlinkAppProperties featureSet) {
		this.featureSet = featureSet;
	}

	/**
     * Gets the wait interval (in milliseconds) between reads from the data telemetry 
     * source. This is a crude throttling mechanism.
     * 
     * @return the meter interval in milliseconds
     */
    public long getMeterInterval() {
        return meterInterval;
    }

    /**
     * Sets the meter interval. This is the wait interval (in milliseconds)
     * between reads from the data telemetry source. Any value set by this call 
     * temporarily overrides the value in the configuration. 
     *
     * @param meterInterval The meterInterval to set.
     */
    public void setMeterInterval(final long meterInterval) {
        this.meterInterval = meterInterval;
    }

    /**
     * Gets the use database flag, indicating whether the downlink processor
     * will write to the databases.
     * 
     * @return true if writing to the database, false if not
     */
    public boolean isUseDb() {
        return useDb;
    }

    /**
     * Sets the use database flag, indicating whether the downlink processor
     * will write to the database. Any value set by this call
     * temporarily overrides the value in the configuration. 
     *
     * @param useDb The useDb to set.
     */
    public void setUseDb(final boolean useDb) {
        this.useDb = useDb;
        this.dbProperties.setUseDatabase(this.useDb); 
    }

    /**
     * Gets the use message service flag, indicating whether the downlink 
     * processor will publish messages to the message service. 
     * 
     * @return true if publishing, false if not
     */
    public boolean isUseMessageService() {
        return useJms;
    }

    /**
     * Sets the use message service flag, indicating whether the downlink processor
     * will publish messages. Any value set by this call
     * temporarily overrides the value in the configuration.
     *
     * @param useJms The useJms to set.
     */
    public void setUseMessageService(final boolean useJms) {
        this.useJms = useJms;
        this.appContext.getBean(MessageServiceConfiguration.class).setUseMessaging(this.useJms);
    }

    /**
     * Returns the view configuration object for the message table view
     * that is displayed in the downlink window. This object comes from the
     * user perspective.
     * 
     * @return DownMessageViewConfiguration object
     */
    public DownMessageViewConfiguration getMessageViewConfig() {
        return this.messageViewConfig;
    }
    
    /**
     * Sets the downlink message view configuration object for the message table view
     * that is displayed in the downlink window. Setting this object will
     * not result in any modifications to the perspective.
     * 
     * @param config the DownMessageViewConfiguration object to set
     */
    public void setMessageViewConfig(final DownMessageViewConfiguration config) {
        this.messageViewConfig = config;
    }
}
