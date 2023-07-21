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
package jpl.gds.telem.process.server;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.error.ErrorCode;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.security.ssl.ISslConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.*;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.ITelemetryServer;
import jpl.gds.telem.common.app.ExitCodeHandler;
import jpl.gds.telem.process.ProcessConfiguration;
import jpl.gds.telem.process.ProcessServerManagerApp;
import jpl.gds.telem.process.app.mc.rest.controllers.RestfulTelemetryProcessorControl;
import jpl.gds.telem.process.app.mc.rest.controllers.RestfulTelemetryProcessorStatus;
import jpl.gds.telem.process.server.event.SessionMessageSubscriber;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Telemetry Processor Server Spring Bootstrapper
 * 
 *
 */
@Configuration
public class TelemetryProcessorServerBootstrap {

    /** Threadpooler */
    public static final  String  THREADPOOL                    = "THREADPOOL";
    /** Telemetry Processor Manager */
    public static final  String  TPS_MANAGER                   = "TPS_MANAGER";
    /** Session subscriber */
    public static final  String  SESSION_SUBSCRIBER            = "SESSION_SUBSCRIBER";
    /** Telemetry Processor configuration */
    public static final  String  PROCESS_CONFIGURATION         = "PROCESS_CONFIGURATION";
    /** Rest container */
    public static final  String  REST_CONTAINER                = "REST_CONTAINER";
    private static final String  INITIALIZATION_ERROR_PREAMBLE = "\n*** Initialization Error: ";

    private ApplicationContext    appContext;
    private IStatusMessageFactory statusMessageFactory;
    private Tracer                tracer;

    /**
     * Sets the current application context
     * 
     * @param appContext
     *            Spring Application context
     */
    @Autowired
    public void setAppContext(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.tracer = TraceManager.getTracer(appContext, Loggers.PROCESSOR);
    }

    /**
     * Sets the status message factory
     * 
     * @param statusMessageFactory
     *            <IStatusMessageFactory>
     */
    @Autowired
    public void setStatusMessageFactory(final IStatusMessageFactory statusMessageFactory) {
        this.statusMessageFactory = statusMessageFactory;
    }

    /**
     * Creates and returns the Telemetry Processor Server Manager bean
     *
     * @param appContext
     *            spring application context
     * @param exec
     *            executor service
     * @param dictProps
     *            dictionary properties
     * @param cache
     *            dictionary cache
     * @param processConfig
     *            <ProcessConfiguration>
     * @return Telemetry Processor Server Manager bean
     */
    @Bean(TPS_MANAGER)
    @Lazy(false)
    public ITelemetryServer getManager(final ApplicationContext appContext, final ThreadPoolTaskExecutor exec,
                                       final DictionaryProperties dictProps,
                                       final IDictionaryCache cache,
                                       final ProcessConfiguration processConfig) {
        return new ProcessServerManagerApp(appContext, tracer,
                                           exec, dictProps, cache, processConfig);

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
     * Creates and returns a Session Message Subscriber bean
     *
     * @param appContext
     *            spring application context
     * @param factory
     *            message client factory
     * @param contextKey
     *            The <IContextKey>
     * @param util
     *            external message utility
     * @return Session Message Subscriber bean
     */
    @Bean(SESSION_SUBSCRIBER)
    public SessionMessageSubscriber getSessionSubscriber(final ApplicationContext appContext,
                                                         final IMessageClientFactory factory,
                                                         final IContextKey contextKey,
                                                         final IExternalMessageUtility util) {
        return new SessionMessageSubscriber(appContext, factory, util, contextKey, tracer);
    }

    /**
     * Creates and returns a process configuration bean
     * 
     * @param appContext
     *            the current spring application context
     * @param missionProps
     *            mission properties
     * @param sseFlag
     *            sse context flag
     * @param msgServiceConfig
     *            message service configuration
     * @return ProcessConfiguration bean
     */
    @Bean(PROCESS_CONFIGURATION)
    public ProcessConfiguration getProcessConfig(final ApplicationContext appContext,
                                                 final MissionProperties missionProps,
                                                 final SseContextFlag sseFlag,
                                                 final MessageServiceConfiguration msgServiceConfig) {
        return new ProcessConfiguration(missionProps, sseFlag, msgServiceConfig, appContext.getBean(
                CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
    }

    /* -------- REST beans -------- */

    /**
     * @param appContext
     *            The current spring application context
     * @param app
     *            <ITelemetryServer> application
     * @return the embedded tomcat server
     */
    @Bean(name = REST_CONTAINER)
    @Scope("singleton")
    @Lazy(value = true)
    public ServletWebServerFactory servletContainer(final ApplicationContext appContext,
                                                    final ITelemetryServer app) {
        TomcatServletWebServerFactory tomcat = null;
        try {

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
                 * Retrieve HTTPS (SSL/TLS) properties from, and sete its secure status.
                 * NOTE: Setting secure status to true will set all JAVAX System Properties appropriately.
                 *       Setting secure status to false will NOT set any JAVAX System Properties
                 */
                final ISslConfiguration sslConfig = appContext.getBean(ISslConfiguration.class);
                sslConfig.setSecure(app.isRestSecure());
                tracer.debug(sslConfig);

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
                } else {
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
        } catch (final Exception e) {
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
     * @param manager ITelemetryServer bean
     * @return RestfulTelemetryProcessorControl bean
     */
    @Bean
    public RestfulTelemetryProcessorControl getRestControl(final ApplicationContext applicationContext,
                                                           final ProcessServerManagerApp manager) {
        return new RestfulTelemetryProcessorControl(applicationContext, manager);
    }

    /**
     * Creates and returns a RestfulTelemetryProcessorStatus bean
     *
     * @param appContext spring application context
     * @param manager    ITelemetryServer bean
     * @return RestfulTelemetryProcessorStatus bean
     */
    @Bean
    public RestfulTelemetryProcessorStatus getRestStatus(final ApplicationContext appContext,
                                                         final ProcessServerManagerApp manager) {
        return new RestfulTelemetryProcessorStatus(appContext, manager);
    }

    /**
     * Shut down embedded servlet container if port set to -1C
     *
     * @param event ServletWebServerInitializedEvent
     */
    @EventListener
    public void containerInitialized(final ServletWebServerInitializedEvent event) {
        try {
            final WebServer container = event.getWebServer();
            final ITelemetryServer app = appContext.getBean(ITelemetryServer.class);

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

        } catch (final Throwable t) {
            final Throwable cause = t.getCause();
            System.err.println(INITIALIZATION_ERROR_PREAMBLE
                    + ((cause != null) ? cause.getLocalizedMessage() : t.getLocalizedMessage()));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }
    }
}
