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
package jpl.gds.telem.ingest.server;

import jpl.gds.shared.cli.app.ApplicationConfiguration;

import org.apache.catalina.Context;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySources;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.error.ErrorCode;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.security.spring.bootstrap.SecuritySpringBootstrap;
import jpl.gds.security.ssl.ISslConfiguration;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.ITelemetryServer;
import jpl.gds.telem.common.app.ExitCodeHandler;
import jpl.gds.telem.ingest.IngestConfiguration;
import jpl.gds.telem.ingest.IngestServerManagerApp;
import jpl.gds.telem.ingest.app.mc.rest.controllers.RestfulTelemetryIngestorControl;
import jpl.gds.telem.ingest.app.mc.rest.controllers.RestfulTelemetryIngestorStatus;
import jpl.gds.telem.input.api.config.TelemetryInputProperties;
import org.springframework.web.servlet.config.annotation.*;


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
public class TelemetryIngestSeverBootstrap {
    /** The name of the Downlink Spring Properties file */
    public static final String INGEST_SPRING_PROPERTIES_FILENAME = "ingest_app_spring.properties";

    /** Name of the downlink configuration bean */
    public static final String      INGEST_CONFIGURATION              = "INGEST_CONFIGURATION";

    /** Name of the downlink app bean */
    public static final String      TIS_MANAGER                        = "TIS_MANAGER";

    /** Name of the Embedded Tomcat Bean */
    public static final String      REST_CONTAINER                      = "REST_CONTAINER";

    /** Name of the Secure Class Loader Bean */
    public static final String      SECURE_CLASS_LOADER                 = "SECURE_CLASS_LOADER";

    /**
     * Name of Downlink Spring Property Sources
     */
    public static final String      INGEST_SPRING_PROPERTY_SOURCES    = "INGEST_SPRING_PROPERTY_SOURCES";

    /**thread pool from workers */
    public static final  String  THREADPOOL                    = "THREADPOOL";

    private static final String     INITIALIZATION_ERROR_PREAMBLE       = "\n*** Initialization Error: ";

    @Autowired
    private ApplicationContext      appContext;

    @Autowired
    private IStatusMessageFactory   statusMessageFactory;

    @Autowired
    private ConfigurableEnvironment env;


    /**
     * @return a Property Resource representing
     */
    @Bean(name = INGEST_SPRING_PROPERTY_SOURCES)
    @Scope("singleton")
    @Lazy(value = true)
    @DependsOn({ SecuritySpringBootstrap.DEFAULT_SSL_PROPERTY_SOURCES })
    public PropertySources loadAMPCSProperySource() {
        try {
            return GdsSpringSystemProperties.loadAMPCSProperySources(env,
                                                                     TraceManager.getTracer(appContext, Loggers.INGEST),
                                                                     INGEST_SPRING_PROPERTIES_FILENAME,
                                                                     appContext.getBean(SseContextFlag.class));
        }
        catch (final Exception e) {
            System.err.println(INITIALIZATION_ERROR_PREAMBLE + ExceptionTools.getMessage(e));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
            return null;
        }
    }

    /**
     * Creates and/or returns the singleton downlink configuration bean.
     *
     * @param sseFlag
     *            sse context flag
     * @param inputConfig
     *            telemetry input properties
     * @param msgServiceConfig
     *            message service configuration
     * @return IngestConfiguration bean
     */
    @Bean(name = INGEST_CONFIGURATION)
    @Scope("singleton")
    @Lazy(value = true)
    public IngestConfiguration getDownConfiguration(final SseContextFlag sseFlag,
                                                    final TelemetryInputProperties inputConfig,
                                                    final MessageServiceConfiguration msgServiceConfig) {
        try {
            return new IngestConfiguration(sseFlag, inputConfig, msgServiceConfig,
                                           appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES,
                                                              IDatabaseProperties.class));
        }
        catch (final Exception e) {
            System.err.println(INITIALIZATION_ERROR_PREAMBLE + ExceptionTools.getMessage(e));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
            return null;
        }
    }

    /**
     * Creates and returns a Threadpool Task Executor bean
     *
     * @return ThreadpoolTaskExecutor bean
     */
    @Bean(THREADPOOL)
    @Lazy(false)
    public ThreadPoolTaskExecutor getExecutor() {
        final ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();

        exec.setCorePoolSize(4);
        final int availableProcessors = Runtime.getRuntime().availableProcessors();
        final int maxPoolSize         = availableProcessors >= 8 ? availableProcessors : 8;
        exec.setMaxPoolSize(maxPoolSize);

        return exec;
    }

    /**
     * Creates and returns the Telemetry Ingestor Server Manager bean
     *
     * @param appContext
     *            spring application context
     * @param exec
     *            executor service
     * @param downConfig
     *            <IngestConfiguration> Telemetry Ingestor config
     * @return Telemetry Ingestor Server Manager bean
     */
    @Bean(TIS_MANAGER)
    @Lazy(false)
    public ITelemetryServer getManager(final ApplicationContext appContext, final ThreadPoolTaskExecutor exec,
                                       final IngestConfiguration downConfig) {
        return new IngestServerManagerApp(appContext, TraceManager.getTracer(appContext, Loggers.INGEST),
                                          exec, downConfig);

    }

    /* -------- REST beans -------- */

    /**
     * @param appContext
     *            The current spring application context
     * @param app
     *            The <ITelemetryServerApp>
     * @return the embedded tomcat server
     */
    @Bean(name = REST_CONTAINER)
    @Scope("singleton")
    @Lazy(value = true)
    @DependsOn(INGEST_SPRING_PROPERTY_SOURCES)
    public ServletWebServerFactory servletContainer(final ApplicationContext appContext,
                                                    final ITelemetryServer app) {
        TomcatServletWebServerFactory tomcat = null;
        try{
            tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(final Context context) {
                if (app.isRestSecure()) {
                    final SecurityConstraint securityConstraint = new SecurityConstraint();
                    securityConstraint.setUserConstraint("CONFIDENTIAL");
                    final SecurityCollection collection = new SecurityCollection();
                    collection.addPattern("/*");
                    securityConstraint.addCollection(collection);
                    context.addConstraint(securityConstraint);
                }
            }
        };

            tomcat.addConnectorCustomizers(connector -> {
                /* Set the rest port */
                final int restPort = app.getRestPort();
                connector.setPort(restPort);

                final Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

                /*
                 * Retrieve HTTPS (SSL/TLS) properties from, and set its secure status.
                 */
                final ISslConfiguration sslConfig = appContext.getBean(ISslConfiguration.class);
                sslConfig.setSecure(app.isRestSecure());

                if (sslConfig.isSecure()) {
                    /*
                     * Using Secure Transport: SSL/TLS encryption
                     * Manage transport parameters scheme, SSL/TLS, Ciphers, KeyStore and TrustStore
                     */
                    connector.setSecure(true);
                    connector.setScheme("https");
                    protocol.setSSLEnabled(true);

                    protocol.setKeystoreFile(sslConfig.getKeystorePath());
                    protocol.setKeystorePass(sslConfig.getKeystorePassword());
                    protocol.setKeystoreType(sslConfig.getKeystoreType());

                    protocol.setTruststoreFile(sslConfig.getTruststorePath());
                    protocol.setTruststorePass(sslConfig.getTruststorePassword());
                    protocol.setTruststoreType(sslConfig.getTruststoreType());

                    if (sslConfig.hasHttpsProtocols()) {
                        protocol.setSSLProtocol(sslConfig.getHttpsProtocol());
                    }

                    if (sslConfig.hasCiphers()) {
                        protocol.setCiphers(sslConfig.getCiphers());
                    }
                }
                else {
                    /*
                     * Using Insecure Transport
                     * Manage transport parameters: scheme
                     */
                    connector.setSecure(false);
                    connector.setScheme("http");

                    /*
                     * Bug-fix: This needs to be here. Not sure why it was working before, but it appears
                     * that the centralizaiton of the SSL/TLS properties in the security module changed the behavior in
                     * such a way that it is now required to explicitly disable SSL
                     */
                    protocol.setSSLEnabled(false);
                }
            });
        }
        catch (final Exception e) {
        		final String errMsg = e.getCause() != null && !e.getCause().getMessage().isEmpty() ?
        				e.getCause().getMessage() : ExceptionTools.getMessage(e);

        		System.err.println(INITIALIZATION_ERROR_PREAMBLE + errMsg);

            // This must be SpringApplication.exit, NOT System.exit
            SpringApplication.exit(appContext, new ExitCodeHandler(app));
        }
        return tomcat;
    }

    /**
     * Creates and returns the RestfulTelemetryProcessorControl bean
     *
     * @param applicationContext The current spring application context
     * @param manager ITelemetryServer bean
     * @return RestfulTelemetryProcessorControl bean
     */
    @Bean
    public RestfulTelemetryIngestorControl getRestControl(final ApplicationContext applicationContext,
                                                          final IngestServerManagerApp manager) {
        return new RestfulTelemetryIngestorControl(applicationContext, manager);
    }

    /**
     * Creates and returns a RestfulTelemetryProcessorStatus bean
     *
     * @param appContext spring application context
     * @param manager    ITelemetryServer bean
     * @return RestfulTelemetryProcessorStatus bean
     */
    @Bean
    public RestfulTelemetryIngestorStatus getRestStatus(final ApplicationContext appContext, final IngestServerManagerApp manager) {
        return new RestfulTelemetryIngestorStatus(appContext, manager);
    }

    /**
     * Shut down embedded servlet container if port set to -1C
     * 
     * @param event
     *            ServletWebServerInitializedEvent
     */
    @EventListener
    public void containerInitialized(final ServletWebServerInitializedEvent event) {
        try {
            final Tracer tracer = TraceManager.getDefaultTracer(appContext);
            final ITelemetryServer app = appContext.getBean(ITelemetryServer.class);
            final WebServer container = event.getWebServer();

            if (app.isRestSecure()) {
                tracer.log(statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO, ApplicationConfiguration
                        .getApplicationName()
                        + " M&C RESTful Service is using (SSL/TLS) Secure Transport Protocol", LogMessageType.REST));
            }
            else {
                tracer.log(statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO, ApplicationConfiguration.getApplicationName()
                        + " M&C RESTful Service is using (UNENCRYPTED) Plaintext Transport Protocol",
                                                                            LogMessageType.REST));
            }
            tracer.log(statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO, ApplicationConfiguration.getApplicationName()
                    + " M&C RESTful Service is listening on port " + container.getPort(), LogMessageType.REST));

        }
        catch (final Throwable t) {
            final Throwable cause = t.getCause();
            System.err.println(INITIALIZATION_ERROR_PREAMBLE
                    + ((cause != null) ? cause.getLocalizedMessage() : t.getLocalizedMessage()));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }
    }}
