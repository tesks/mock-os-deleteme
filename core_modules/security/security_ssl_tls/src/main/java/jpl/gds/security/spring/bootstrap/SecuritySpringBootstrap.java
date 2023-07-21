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
package jpl.gds.security.spring.bootstrap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySources;

import jpl.gds.security.ssl.ISslConfiguration;
import jpl.gds.security.ssl.SslServerProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Class SecuritySpringBootstrap
 */
@Configuration
public class SecuritySpringBootstrap {
    /**
     * The name of the default SSL Spring Properties File
     */
    public static final String DEFAULT_SSL_PROPERTIES                 = "DEFAULT_SSL_PROPERTIES";

    /**
     * The name of the default SSL Spring Properties File
     */
    public static final String DEFAULT_SSL_SPRING_PROPERTIES_FILENAME = "default_spring_server_ssl.properties";

    /**
     * Bean name for the command line parser bean
     */
    public static final String DEFAULT_SSL_PROPERTY_SOURCES            = "DEFAULT_SSL_PROPERTY_SOURCE";

    @Autowired
    ConfigurableEnvironment    env;

    /**
     * @return an instance of an ISslConfiguration object populated with the default SSL/TLS properties from the
     *         Security module. These may be overridden by subsequent property file loads.
     */
    @Bean(name = DEFAULT_SSL_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = true)
    @DependsOn(DEFAULT_SSL_PROPERTY_SOURCES)
    public ISslConfiguration getDefaultSslSpringProperties() {
        return new SslServerProperties(env);
    }

    /**
     * Load the Default SSL Spring Properties file
     * 
     * @return the current Spring PropertySources after adding in the Default SSL Properties
     * @throws IOException
     *             if there is an error loading an existing file
     * @throws FileNotFoundException
     *             if file does not exist
     */
    @Bean(name = DEFAULT_SSL_PROPERTY_SOURCES)
    @Scope("singleton")
    @Lazy(value = true)
    public PropertySources getDefaultSslSpringPropertySources() throws FileNotFoundException, IOException {
        final MutablePropertySources sources = env.getPropertySources();
        final Properties p = new Properties();
        final Tracer log = TraceManager.getTracer(Loggers.CONFIG);

        /** Added mission and user override for default spring properties ssl config file */

        /*
         * Retrieve System Load Path
         */
        final File systemFile = Paths.get(GdsSystemProperties.getSystemConfigDir(), DEFAULT_SSL_SPRING_PROPERTIES_FILENAME).toFile();

        if (systemFile.exists() && systemFile.canRead()) {
            p.load(new FileReader(systemFile));
            sources.addFirst(new PropertiesPropertySource(systemFile.getAbsolutePath(), p));
            log.debug("Loading ", systemFile.getAbsolutePath(), "=\n", p.toString());
        } else { 
            log.debug("SKIPPING ", systemFile.getAbsolutePath());
        }

        /*
         * Retrieve Mission Load Path
         */
        final File missionFile = Paths.get(GdsSystemProperties.getSystemConfigDir(), GdsSystemProperties.getSystemMission(), 
                                           DEFAULT_SSL_SPRING_PROPERTIES_FILENAME).toFile();
        
        if (missionFile.exists() && missionFile.canRead()) {
            p.load(new FileReader(missionFile));
            sources.addFirst(new PropertiesPropertySource(missionFile.getAbsolutePath(), p));
            log.debug("Loading ", missionFile.getAbsolutePath(), "=\n", p.toString());
        } else { 
            log.debug("SKIPPING ", missionFile.getAbsolutePath());
        }

        /*
         * Retrieve User Load Path
         */
        final File userFile = Paths.get(GdsSystemProperties.getUserConfigDir(), DEFAULT_SSL_SPRING_PROPERTIES_FILENAME).toFile();

        if (userFile.exists() && userFile.canRead()) {
            p.load(new FileReader(userFile));
            sources.addFirst(new PropertiesPropertySource(userFile.getAbsolutePath(), p));
            log.debug("Loading ", userFile.getAbsolutePath(), "=\n", p.toString());
        } else { 
            log.debug("SKIPPING ", userFile.getAbsolutePath());
        }

        return sources;
    }

}
