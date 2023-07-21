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
package jpl.gds.telem.down.app;

import org.apache.catalina.Context;
import org.apache.commons.cli.ParseException;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
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

import jpl.gds.common.error.ErrorCode;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.security.spring.bootstrap.SecuritySpringBootstrap;
import jpl.gds.security.ssl.ISslConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.context.cli.app.mc.IRestFulServerCommandLineApp;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.telem.common.app.ExitCodeHandler;
import jpl.gds.telem.down.DownConfiguration;
import jpl.gds.telem.down.IDownlinkApp;

/**
 * Spring bootstrap configuration class for the chill_down application ONLY. Do
 * not place this class into any Spring bootstrap package to be automatically
 * loaded. This bootstrap should be explicitly loaded by the chill_down
 * application only. Loading it automatically may result in the wrong
 * perspective properties bean getting returned from the application context.
 * 
 *
 * @since R8
 */
@Configuration
public class ChillDownSpringBootstrap {
    /** The name of the Downlink Spring Properties file */
    public static final String      DOWNLINK_SPRING_PROPERTIES_FILENAME = "down_app_spring.properties";

    /** Name of the downlink configuration bean */
    public static final String      DOWNLINK_CONFIGURATION              = "DOWNLINK_CONFIGURATION";

    /** Name of the downlink app bean */
    public static final String      DOWNLINK_APP                        = "DOWNLINK_APP";

    /** Name of the command line bean */
    public static final String      COMMAND_LINE                        = "COMMAND_LINE";

    /** Name of the Embedded Tomcat Bean */
    public static final String      REST_CONTAINER                      = "REST_CONTAINER";

    /** Name of the Servlet Registration Bean */
    public static final String      SERVLET_REGISTRATION                = "SERVLET_REGISTRATION";

    /**
     * Name of the Boolean bean that indicates whether the RESTful interface has been enabled
     */
    public static final String      REST_CONTAINER_ENABLED              = "REST_CONTAINER_ENABLED";

    /**
     * Name of Downlink Spring Property Sources
     */
    public static final String      DOWNLINK_SPRING_PROPERTY_SOURCES    = "DOWNLINK_SPRING_PROPERTY_SOURCES";

    private static final String     INITIALIZATION_ERROR_PREAMBLE       = "\n*** Initialization Error: ";

    /**
     * This is set to true if the RESTful interface container is initialized, false if
     * not, meaning that the RESTful interface has been disabled.
     */
    private boolean                 restFulInterfaceEnabled;

    @Autowired
    private ApplicationContext      appContext;

    @Autowired
    private IStatusMessageFactory   statusMessageFactory;

    @Autowired
    private ConfigurableEnvironment env;


    /**
     * @return a Property Resource representing
     */
    @Bean(name = DOWNLINK_SPRING_PROPERTY_SOURCES)
    @Scope("singleton")
    @Lazy(value = true)
    @DependsOn({ SecuritySpringBootstrap.DEFAULT_SSL_PROPERTY_SOURCES })
    public PropertySources loadAMPCSProperySource() {
        try {
            return GdsSpringSystemProperties.loadAMPCSProperySources(env,
                                                                     TraceManager.getTracer(appContext,
                                                                                            Loggers.DOWNLINK),
                                                                     DOWNLINK_SPRING_PROPERTIES_FILENAME,
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
     * @return DownConfiguration bean
     */
    @Bean(name = DOWNLINK_CONFIGURATION)
    @Scope("singleton")
    @Lazy(value = true)
    public DownConfiguration getDownConfiguration() {
        try {
            return new DownConfiguration(appContext);
        }
        catch (final Exception e) {
            System.err.println(INITIALIZATION_ERROR_PREAMBLE + ExceptionTools.getMessage(e));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
            return null;
        }
    }

    /**
     * @return the correct and appropriate IDownlinkApp implementation for FSW or SSE, depending upon the configuration
     *         of GdsSystemProperties
     */
    @Bean(name = DOWNLINK_APP)
    @Scope("singleton")
    @Lazy(value = true)
    public IDownlinkApp getDownlinkApp() {
        try {
            // For now, Change SSE system property lookup to check if app name "sse chill down"
            return ApplicationConfiguration.getApplicationName().contains("sse_") ? new SseDownlinkApp(appContext)
                    : new FswDownlinkApp(appContext);
        }
        catch (final Throwable t) {
            final Throwable cause = t.getCause();
            System.err.println(INITIALIZATION_ERROR_PREAMBLE
                    + ((cause != null) ? cause.getLocalizedMessage() : t.getLocalizedMessage()));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
            return null;
        }
    }

    /**
     * @param app
     *            the Spring Application Context
     * @param appArgs
     *            the Spring representation of the command line
     * @return the correct and appropriate IDownlinkApp implementation for FSW or SSE, depending upon the configuration
     *         of GdsSystemProperties
     * @throws ParseException 
     */
    @Bean(name = COMMAND_LINE)
    @Scope("singleton")
    @Lazy(value = true)
    public ICommandLine getCommandLine(final IDownlinkApp app, final ApplicationArguments appArgs) throws ParseException {

	    	final ICommandLine result = app.createOptions().parseCommandLine(appArgs.getSourceArgs(), true);
	    	/* 
	    	 * Just exit if help or version requested.
	    	 */
	    	if (app.helpDisplayed.get() || app.versionDisplayed.get()) {
	    		System.exit(app.getErrorCode());
	    	}
	    	return result;

    }

    /**
     * @return the embedded tomcat server
     */
    @Bean(name = REST_CONTAINER)
    @Scope("singleton")
    @Lazy(value = true)
    @DependsOn(DOWNLINK_SPRING_PROPERTY_SOURCES)
    public ServletWebServerFactory servletContainer() {
        final IDownlinkApp app = appContext.getBean(IDownlinkApp.class);
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
                connector.setPort((restPort == IRestFulServerCommandLineApp.SPECIFIED_DISABLE_REST_PORT)
                        ? IRestFulServerCommandLineApp.DUMMY_DISABLED_REST_PORT
                        : restPort);

                final Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

                /*
                 * Retrieve HTTPS (SSL/TLS) properties from, and sete its secure status.
                 * NOTE: Setting secure status to true will set all JAVAX System Properties appropriately.
                 *       Setting secure status to false will NOT set any JAVAX System Properties
                 */
                final ISslConfiguration sslConfig = appContext.getBean(ISslConfiguration.class);
                sslConfig.setSecure(app.isRestSecure());
                TraceManager.getTracer(appContext, Loggers.DOWNLINK).debug(sslConfig);

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
                     * This needs to be here. Not sure why it was working before, but it appears
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
     * Get status of RESTful interface.
     *
     * @return a boolean indicating whether the EmbeddedServletContainer (Tomcat) has been enabled
     */
    @Bean(name = REST_CONTAINER_ENABLED)
    @Scope("singleton")
    @Lazy(value = true)
    @DependsOn(REST_CONTAINER)
    public Boolean isRestContainerEnabled() {
        return restFulInterfaceEnabled;
    }

    /**
     * Shut down embedded servlet container if port set to -1C
     * 
     * @param event
     *            EmbeddedServletContainerInitializedEvent
     */
    @EventListener
    public void containerInitialized(final ServletWebServerInitializedEvent event) {
        try {
            final Tracer tracer = TraceManager.getTracer(appContext, Loggers.DOWNLINK);
            final WebServer container = event.getWebServer();
            final IDownlinkApp app = appContext.getBean(IDownlinkApp.class);
            if (app.getRestPort() != IRestFulServerCommandLineApp.SPECIFIED_DISABLE_REST_PORT) {
                restFulInterfaceEnabled = true;
                if (app.isRestSecure()) {
                    tracer.log(statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO, app.getAppName()
                            + " M&C RESTful Service is using (SSL/TLS) Secure Transport Protocol",
                                                                                LogMessageType.REST));
                }
                else {
                    tracer.log(statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO, app.getAppName()
                            + " M&C RESTful Service is using (UNENCRYPTED) Plaintext Transport Protocol",
                                                                                LogMessageType.REST));
                }
                tracer.log(statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO, app.getAppName()
                        + " M&C RESTful Service is listening on port " + container.getPort(), LogMessageType.REST));
            }
            else {
                restFulInterfaceEnabled = false;
                container.stop();
                tracer.log(statusMessageFactory.createPublishableLogMessage(TraceSeverity.INFO, app.getAppName()
                        + " M&C RESTful Service is DISABLED!!!", LogMessageType.REST));
            }
        }
        catch (final Throwable t) {
            final Throwable cause = t.getCause();
            System.err.println(INITIALIZATION_ERROR_PREAMBLE
                    + ((cause != null) ? cause.getLocalizedMessage() : t.getLocalizedMessage()));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }
    }
}
