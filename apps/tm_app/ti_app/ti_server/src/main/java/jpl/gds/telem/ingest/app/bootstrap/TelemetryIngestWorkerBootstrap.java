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
package jpl.gds.telem.ingest.app.bootstrap;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.ingest.IIngestWorker;
import jpl.gds.telem.ingest.IngestConfiguration;
import jpl.gds.telem.ingest.app.TelemetryIngestWorker;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * Spring bootstrap configuration class for the chill_telem_ingest application ONLY. Do
 * not place this class into any Spring bootstrap package to be automatically
 * loaded. This bootstrap should be explicitly loaded by the chill_telem_ingest
 * application only. Loading it automatically may result in the wrong
 * perspective properties bean getting returned from the application context.
 *
 *
 * @since R8
 */
@Configuration
public class TelemetryIngestWorkerBootstrap {
    /** Name of the ingest configuration bean */
    public static final String INGEST_CONFIGURATION = "INGEST_CONFIGURATION";

    /** dictionary properties bean */
    public static final String DICTIONARY_PROPERTIES = "DICTIONARY_PROPERTIES";

    private ApplicationContext                appContext;

    @Autowired
    public void setAppContext(ApplicationContext ctx) {
        this.appContext = ctx;
    }

    /**
     * Telemetry Ingest Worker spring bootstrap bean definition
     *
     * @param sessionConfig The SessionConfiguration to use with the telemetry worker
     * @param sseContextFlag The SSE Context flag to use with the telemetry worker
     * @param secureLoader The parent servers secure classloader
     *
     * @return TelemetryIngestWorker
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public IIngestWorker getIngestWorker(final SessionConfiguration sessionConfig,
                                         final SseContextFlag sseContextFlag,
                                         final AmpcsUriPluginClassLoader secureLoader) {
        return new TelemetryIngestWorker(appContext, sessionConfig, sseContextFlag, secureLoader);
    }

    /**
     * Creates and returns the process configuration bean
     *
     * @param sseFlag           sse context flag
     * @param appContext the current application context
     * @param inputProps telemetry input properties
     * @param msgServiceConfig  message service configuration
     * @return ProcessConfiguration bean
     */
    @Bean(INGEST_CONFIGURATION)
    @Lazy
    public IngestConfiguration getIngestConfig(ApplicationContext appContext,
                                                TelemetryInputProperties inputProps, SseContextFlag sseFlag,
                                                MessageServiceConfiguration msgServiceConfig) {
        return new IngestConfiguration(sseFlag, inputProps, msgServiceConfig,
                                       appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
    }

    @Bean(DICTIONARY_PROPERTIES)
    public DictionaryProperties getDictProps() {
        return new DictionaryProperties(true);
    }


}
