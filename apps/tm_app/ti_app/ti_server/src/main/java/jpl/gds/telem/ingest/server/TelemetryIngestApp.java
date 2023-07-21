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

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;

import jpl.gds.common.error.ErrorCode;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.telem.common.CommonTelemetryServerBootStrap;
import jpl.gds.telem.common.ITelemetryServer;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * 
 * TelemetryIngestApp is the main class for the downlink application. It just
 * instantiates and executes either the FSW or SSE chill down class
 * 
 */
// this variable is resolved on class load, through #buildScanPath(Package pkg).
@SpringBootApplication(scanBasePackages = {"${springScanPath}"})
@PropertySource("classpath:config/ingest_app_spring.properties")
@Configuration
@EnableSwagger2
public class TelemetryIngestApp {
    @SuppressWarnings("unused")
    private static final List<String> SCAN_PATH = buildScanPath(TelemetryIngestApp.class.getPackage());

    @Autowired
    private ITelemetryServer          app;

    /**
     * Returns a List of scan path locations customized for Downlink applications.
     * Side Effect: sets the System Property "scanPath" with a comma-delimited version of this path for consumption by
     * the spring-boot / spring-mvc annotations and initialization.
     * 
     * @param bootStrapPackage
     *            the Package to scan for bootstrap class specialized for Downlink.
     * 
     * @return The calculated value to be used to populate ${springScanPath} in the @ComponentScan annotation
     */
    public static List<String> buildScanPath(final Package bootStrapPackage) {
        final List<String> scanPath = new ArrayList<>();
        scanPath.add(bootStrapPackage.getName());
        scanPath.add(CommonTelemetryServerBootStrap.class.getPackage().getName());
        scanPath.addAll(SpringContextFactory.getSpringBootstrapScanPath(true));
        scanPath.add(bootStrapPackage.getName() + ".mc.rest.controllers");
        GdsSpringSystemProperties.setScanPath(scanPath);
        return scanPath;
    }

    /**
     * Main application class.
     *
     * @param args
     *            the command line arguments.
     */
    public static void main(final String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        try {
            final SpringApplication app = new SpringApplicationBuilder().bannerMode(Banner.Mode.OFF)
                                                                  .sources(TelemetryIngestApp.class, TelemetryIngestSeverBootstrap.class)
                                                                  .build();

            app.setWebApplicationType(WebApplicationType.SERVLET);

            app.setRegisterShutdownHook(false);
            // Allow bean override
            app.setAllowBeanDefinitionOverriding(true);
            app.run(args);
        } catch (final Exception e) {

            ExceptionTools.handleSpringBootStartupError(e);
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }

    }

    /**
     * @param evt
     *            the event to process (ApplicationReadyEvent)
     */
    @EventListener
    public void launchApp(final ApplicationReadyEvent evt) {
        try {
            app.run();
            //server process - leave running
        }
        catch (final Exception e) {
            final Throwable cause = e.getCause();
            System.err.println("\n*** Initialization Error: " + ((cause != null) ? cause.getLocalizedMessage() : e.getLocalizedMessage()));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }
    }
}
