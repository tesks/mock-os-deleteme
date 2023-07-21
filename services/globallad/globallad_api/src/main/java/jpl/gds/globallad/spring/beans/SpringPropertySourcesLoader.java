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
package jpl.gds.globallad.spring.beans;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySources;

import jpl.gds.security.spring.bootstrap.SecuritySpringBootstrap;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

@Configuration
public class SpringPropertySourcesLoader {
    /** The name of the Downlink Spring Properties file */
    public static final String      GLAD_SPRING_PROPERTIES_FILENAME = "globallad_app_spring.properties";

    @Autowired
    private ConfigurableEnvironment env;

    @Autowired
    private SseContextFlag          sseFlag;

    /**
     * @return a Property Resource representing
     * @throws IOException
     *             if an error occurs reading the properties file.
     */
    @Bean(name = BeanNames.GLAD_SPRING_PROPERTY_SOURCES)
    @Scope("singleton")
    @Lazy(value = false)
    @DependsOn(SecuritySpringBootstrap.DEFAULT_SSL_PROPERTY_SOURCES)
    public PropertySources loadAMPCSProperySource() throws IOException {
        return GdsSpringSystemProperties.loadAMPCSProperySources(env,
                                                                 TraceManager.getTracer(Loggers.GLAD),
                                                                 GLAD_SPRING_PROPERTIES_FILENAME, sseFlag);
    }
}
