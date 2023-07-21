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

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import jpl.gds.globallad.spring.cli.GlobalLadCommandLineParser;
import jpl.gds.security.ssl.ISslConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Creates the tomcat embedded server and will use the port from the global lad
 * config or the override from the command line parser.
 */
@Configuration
public class EmbeddedServerConfiguration {
    @Autowired
    ApplicationContext appContext;

    /**
     * @return WebMvcConfigurer object
     */
    @Bean
    public WebMvcConfigurer configurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {

                final ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
                t.setMaxPoolSize(100);
                t.setQueueCapacity(50);
                t.setAllowCoreThreadTimeOut(true);
                t.setThreadNamePrefix("request-executor");
                t.initialize();

                configurer.setTaskExecutor(t);
                super.configureAsyncSupport(configurer);
            }

            // MPCS-9197 - Shakeh Brys - 02/10/20 - Add resource location to handle serving static globallad webapp
            final String dashboardHandler = "/dashboard";
            final String resourceHandlerPattern = "/dashboard/**";
            final String resourceLocation = "file:" + GdsSystemProperties.getGdsDirectory() + "/webapp/globallad/";
            final String indexViewRedirect = "redirect:" + dashboardHandler + "/index.html";


            @Override
            public void addResourceHandlers(ResourceHandlerRegistry resourceHandlerRegistry) {

                // Map handler "/globallad/**" to webapp location in GDS directory
                resourceHandlerRegistry
                    .addResourceHandler(resourceHandlerPattern)
                    .addResourceLocations(resourceLocation)
//                    .setCachePeriod(3600)
                    .resourceChain(true);
            }

            @Override
            public void addViewControllers(ViewControllerRegistry registry) {

                // MPCS-11687 - Shakeh Brys - 04.13.20 - add view controller to redirect
                // http://<host>:<port>/globallad/dashboard -> /globallad/dashboard/index.html
                registry.addViewController(dashboardHandler).setViewName(indexViewRedirect);
                // Forward /dashboard/ -> /dashboard/index.html
                registry.addViewController(dashboardHandler + "/").setViewName(indexViewRedirect);
            }
        };
    }

    /**
     * Creates the embedded tomcat server and sets all required values based on
     * the auto wired global lad config. This bean must be set up after the
     * global lad app can been configured so that all of the command line options are
     * properly set.
     * 
     * @param cli
     *            Globallad command line parser
     * @return the Spring EmbeddedServletContainerFactory
     */
    @Bean(name = BeanNames.GLAD_EMBEDDED_SERVER)
    @Autowired
    @DependsOn(BeanNames.GLAD_SPRING_PROPERTY_SOURCES)
    public ServletWebServerFactory servletContainer(final GlobalLadCommandLineParser cli) {

        final TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();

        final int port = cli.getRestPort(); // cli uses glad properties default port if command-line is not present

        final ISslConfiguration sslConfig = appContext.getBean(ISslConfiguration.class);
        sslConfig.setSecure(cli.isRestSecure());
        final Tracer log = TraceManager.getTracer(appContext, Loggers.GLAD);
        log.debug(sslConfig);

        /**
         * MPCS-8623 triviski 4/19/2016 - Must use a customizer for the default connection. Check the lad config
         * to see if we should use ssl (HTTPS) or not (HTTP).
         */
        tomcat.addConnectorCustomizers(connector -> {

            final Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();

            if (cli.isRestSecure()) {
                connector.setScheme("https");
                connector.setSecure(true);

                protocol.setSSLEnabled(true);
                protocol.setKeystoreFile(sslConfig.getKeystorePath());
                protocol.setKeystorePass(sslConfig.getKeystorePassword());
                protocol.setKeystoreType(sslConfig.getKeystoreType());

                protocol.setTruststoreFile(sslConfig.getTruststorePath());
                protocol.setTruststorePass(sslConfig.getTruststorePassword());
                protocol.setTruststoreType(sslConfig.getTruststoreType());

                if (sslConfig.hasCiphers()) {
                    protocol.setCiphers(sslConfig.getCiphers());
                }
                if (sslConfig.hasHttpsProtocols()) {
                    protocol.setSslProtocol(sslConfig.getHttpsProtocol());
                }
                log.info(ApplicationConfiguration.getApplicationName(),
                         " M&C RESTful using HTTPS (SSL/TLS) Secure Transport on port ", port);
            }
            else {
                connector.setSecure(false);
                connector.setScheme("http");

                /*
                 * MPCS-9376: Bug-fix: This needs to be here. Not sure why it was working before, but it appears
                 * that the centralizaiton of the SSL/TLS properties in the security module changed the behavior in
                 * such a way that it is now required to explicitly disable SSL
                 */
                protocol.setSSLEnabled(false);

                log.info(ApplicationConfiguration.getApplicationName(),
                         " M&C RESTful using HTTP (Unencrypted) Insecure Transport on port ", port);
            }

            connector.setPort(port);
        });
        return tomcat;
    }

    @Bean
    public MetricsEndpoint metricsEndpoint(MeterRegistry registry) {
        return new MetricsEndpoint(registry);
    }

}
