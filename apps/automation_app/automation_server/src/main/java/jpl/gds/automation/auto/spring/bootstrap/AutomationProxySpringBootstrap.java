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
package jpl.gds.automation.auto.spring.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.automation.auto.AutoManager;
import jpl.gds.automation.auto.app.AutoUplinkServerApp;
import jpl.gds.automation.auto.app.IAutoProxyApp;
import jpl.gds.automation.auto.cfdp.config.AutoProxyProperties;
import jpl.gds.automation.auto.cfdp.service.AutoPduAggregator;
import jpl.gds.automation.config.AutomationAppProperties;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.exceptions.PropertyLoadException;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Spring bootstrap configuration class for the automation proxy applications
 * ONLY. Do not place this class into any Spring bootstrap package to be
 * automatically loaded. This bootstrap should be explicitly loaded by the AUTO
 * proxy applications only. Loading it automatically may result in the wrong
 * perspective properties bean getting returned from the application context.
 */
@Configuration
public class AutomationProxySpringBootstrap {
    /** Name of the auto proxy app bean **/
    public static final String      AUTO_PROXY_APP               = "AUTOMATION_PROXY_APP";

    /** Name of the auto proxy manager bean */
    public static final String      AUTO_PROXY_MANAGER           = "AUTO_PROXY_MANAGER";

    /** Name of the auto cfdp configuration bean */
    public static final String      AUTO_PROXY_PROPERTIES        = "AUTO_PROXY_PROPERTIES";

    /** Name of the AUTO pdu aggregator bean */
    public static final String      AUTO_PROXY_AGGREGATOR        = "AUTO_PROXY_AGGREGATOR";
    /** Name of the Automation APP properties bean */
    public static final String AUTOMATION_APP_PROPERTIES = "AUTOMATION_APP_PROPERTIES";

    @Autowired
    ApplicationContext              appContext;

    /**
     * Gets the AUTO proxy app
     * 
     * @return AutoUplinkServerApp
     */

    @Bean(name = AUTO_PROXY_APP)
    @Scope("singleton")
    @Lazy(value = true)
    public IAutoProxyApp getAutomationProxyApp() {
        return new AutoUplinkServerApp(appContext);
    }

    /**
     * Gets the AUTO proxy manager
     * 
     * @return AutoManager
     */
    @Bean(name = AUTO_PROXY_MANAGER)
    @Scope("singleton")
    @Lazy(value = true)
    public AutoManager getAutomationProxyManager() {
        return new AutoManager(appContext);
    }
    
    /**
     * Gets the AUTO cfdp properties
     * 
     * @param connectionProperties
     *            Connection Properties
     * @param missionProps
     *            Mission Properties
     * @param sseFlag
     *            The SSE Context flag
     * 
     * @return CfdpProxyProperties
     * @throws PropertyLoadException
     *             error loading properties
     */
    @Bean(name = AUTO_PROXY_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = true)
    public AutoProxyProperties getAutoProxyProperties(final ConnectionProperties connectionProperties,
                                                      final MissionProperties missionProps,
                                                      final SseContextFlag sseFlag)
            throws PropertyLoadException {
        return new AutoProxyProperties(connectionProperties, missionProps, sseFlag);
    }

    /**
     * Gets the AUTO proxy PDU aggregator
     * 
     * @return AutoPduAggregator
     */
    @Bean(name = AUTO_PROXY_AGGREGATOR)
    @Scope("singleton")
    @Lazy(value = true)
    public AutoPduAggregator getPduAggregationManager() {
        return new AutoPduAggregator(appContext);
    }

    /**
     * Gets the Automation App Properties
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return AutoPduAggregator
     */
    @Bean(name = AUTOMATION_APP_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = true)
    public AutomationAppProperties getAutomationAppProperties(final SseContextFlag sseFlag) {
        return new AutomationAppProperties(sseFlag);
    }
}
