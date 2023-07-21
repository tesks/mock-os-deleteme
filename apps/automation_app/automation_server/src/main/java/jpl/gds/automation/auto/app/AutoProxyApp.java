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
package jpl.gds.automation.auto.app;

import jpl.gds.automation.auto.cfdp.config.AutoProxyProperties;
import jpl.gds.automation.auto.cfdp.service.IAutoCfdpService;
import jpl.gds.automation.auto.cfdp.spring.controller.ICfdpController;
import jpl.gds.automation.auto.spring.controller.IAutoController;
import jpl.gds.automation.spring.controller.IAutomationController;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.error.ErrorCode;
import jpl.gds.security.ssl.ISslConfiguration;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import org.apache.commons.cli.ParseException;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * AUTO Automation Proxy server
 *  @since R8
 */
@SpringBootApplication
@EnableSwagger2
@EnableCaching
@PropertySource("classpath:config/auto_app_spring.properties")
@ComponentScan("${springScanPath}")
public class AutoProxyApp implements WebServerFactoryCustomizer {
    @SuppressWarnings("unused")
    private static final List<String> SCAN_PATH = buildScanPath(AutoProxyApp.class.getPackage());

    /** The name of the Auto Spring Properties file */
    public static final String        AUTO_SPRING_PROPERTIES_FILENAME = "auto_app_spring.properties";

    @Autowired
    private IAutoProxyApp                 app;

    Tracer log;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    private AutoProxyProperties proxyConfig;

    @PostConstruct
    private void init() {
        log = TraceManager.getTracer(appContext, Loggers.AUTO_UPLINK);
    }

    /**
     * Returns a List of scan path locations customized for Spring AUTO applications.
     * Side Effect: sets the System Property "scanPath" with a comma-delimited version of this path for consumption by
     * the spring-boot / spring-mvc annotations and initialization.
     *
     * @param bootStrapPackage
     *            the Package to scan for bootstrap class specialized for AUTO.
     *
     * @return Build spring-boot classpath. Located in static method so that tests can also take advantage of it.
     */
    public static List<String> buildScanPath(final Package bootStrapPackage) {
        final List<String> scanPath = new ArrayList<String>();
        scanPath.add(bootStrapPackage.getName());
        scanPath.add(IAutomationController.class.getPackage().getName()); // status and shutdown
        scanPath.add(IAutoController.class.getPackage().getName());
        scanPath.add(ICfdpController.class.getPackage().getName());
        scanPath.add(IAutoCfdpService.class.getPackage().getName());
        scanPath.addAll(SpringContextFactory.getSpringBootstrapScanPath(true));
        GdsSpringSystemProperties.setScanPath(scanPath);
        return scanPath;
    }

    /**
     * Main method
     *
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        try {
            final SpringApplication springApp = new SpringApplicationBuilder()
                    .bannerMode(Banner.Mode.OFF)
                    .sources(AutoProxyApp.class).build();
            springApp.setWebApplicationType(WebApplicationType.SERVLET);
            springApp.setAllowBeanDefinitionOverriding(true);
            springApp.run(args).registerShutdownHook();
        }
        catch (final Exception e) {
            ExceptionTools.handleSpringBootStartupError(e);
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }
    }


    private void validateConfig() throws ParseException {
        final int uplinkPort = proxyConfig.getUplinkPort();
        final UplinkConnectionType connectionType = proxyConfig.getUplinkType();

        if (uplinkPort <= 0) {
            throw new ParseException("Invalid uplink port " + uplinkPort);
        }
        else if (connectionType == null || (!(connectionType.equals(UplinkConnectionType.SOCKET)
                || connectionType.equals(UplinkConnectionType.COMMAND_SERVICE)))) {
            throw new ParseException("Invalid uplink connection type " + connectionType + " is not supported");
        }
        else {
            final IConnectionMap connectionMap = appContext.getBean(IConnectionMap.class);
            connectionMap.createFswUplinkConnection(connectionType);
            connectionMap.getFswUplinkConnection().setHost(proxyConfig.getUplinkHost());
            connectionMap.getFswUplinkConnection().setPort(uplinkPort);
        }
    }

    @Override
    public void customize(final WebServerFactory container) {
        /*
         * Explicitly load the security properties -- normal SpringBoot loading does not appear to work for some reason.
         */
        try {
            GdsSpringSystemProperties.loadAMPCSProperySources(appContext.getBean(ConfigurableEnvironment.class),
                                                              TraceManager.getTracer(appContext, Loggers.AUTO_UPLINK),
                                                              AUTO_SPRING_PROPERTIES_FILENAME,
                                                              appContext.getBean(SseContextFlag.class));
        }
        catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getLocalizedMessage());
        }

        final TomcatServletWebServerFactory tomcat = (TomcatServletWebServerFactory) container;
        try {
            final int restPort = app.getRestPort();

            validateConfig();
            tomcat.setPort(restPort);
            tomcat.addConnectorCustomizers(connector -> {
                connector.setPort(restPort);

                /*
                 * Retrieve HTTPS (SSL/TLS) properties from, and sete its secure status.
                 * NOTE: Setting secure status to true will set all JAVAX System Properties appropriately.
                 * Setting secure status to false will NOT set any JAVAX System Properties
                 */
                final ISslConfiguration sslConfig = appContext.getBean(ISslConfiguration.class);
                sslConfig.setSecure(app.isRestSecure());
                log.debug(sslConfig);

                final Http11NioProtocol sslProtocol = (Http11NioProtocol) connector.getProtocolHandler();

                if (sslConfig.isSecure()) {
                    connector.setSecure(true);
                    connector.setScheme("https");

                    sslProtocol.setSSLEnabled(true);

                    sslProtocol.setKeystoreFile(sslConfig.getKeystorePath());
                    sslProtocol.setKeystorePass(sslConfig.getKeystorePassword());
                    sslProtocol.setKeystoreType(sslConfig.getKeystoreType());

                    sslProtocol.setTruststoreFile(sslConfig.getTruststorePath());
                    sslProtocol.setTruststorePass(sslConfig.getTruststorePassword());
                    sslProtocol.setTruststoreType(sslConfig.getTruststoreType());

                    if (sslConfig.hasHttpsProtocols()) {
                        sslProtocol.setSSLProtocol(sslConfig.getHttpsProtocol());
                    }
                    if (sslConfig.hasCiphers()) {
                        sslProtocol.setCiphers(sslConfig.getCiphers());
                    }

                    log.info(ApplicationConfiguration.getApplicationName(),
                             " M&C RESTful using HTTPS (SSL/TLS) Secure Transport on port ", restPort);
                }
                else {
                    connector.setSecure(false);
                    connector.setScheme("http");
                    connector.setPort(restPort);

                    /*
                     * This needs to be here. Not sure why it was working before, but it appears
                     * that the centralization of the SSL/TLS properties in the security module
                     * changed the behavior in such a way that it is now required to explicitly disable SSL
                     */
                    sslProtocol.setSSLEnabled(false);

                    log.info(ApplicationConfiguration.getApplicationName(),
                             " M&C RESTful using HTTP (Unencrypted) Insecure Transport on port ", restPort);
                }
            });
        }
        catch (final Exception e) {
            TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(e));
        }
    }

}
