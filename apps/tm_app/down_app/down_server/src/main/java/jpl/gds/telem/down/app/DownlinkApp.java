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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;

import jpl.gds.common.error.ErrorCode;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.telem.common.CommonTelemetryServerBootStrap;
import jpl.gds.telem.down.IDownlinkApp;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * 
 * DownlinkApp is the main class for the downlink application. It just
 * instantiates and executes either the FSW or SSE chill down class
 * 
 */
@Configuration
@SpringBootApplication
@EnableSwagger2
@PropertySource("classpath:config/down_app_spring.properties")
@ComponentScan("${springScanPath}")
public class DownlinkApp {
    @SuppressWarnings("unused")
    private static final List<String> SCAN_PATH = buildScanPath(DownlinkApp.class.getPackage());

    @Autowired
    private IDownlinkApp              app;

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
            final SpringApplication springApp = new SpringApplicationBuilder().bannerMode(Banner.Mode.OFF)
                                                                              .sources(DownlinkApp.class).build();

            springApp.setWebApplicationType(WebApplicationType.SERVLET);
            /* 
             * This application must not allow Spring to handle shutdown hooks.
             */
            springApp.setRegisterShutdownHook(false);
            // Allow bean override
            springApp.setAllowBeanDefinitionOverriding(true);
            springApp.run(args);
        }
        catch (final Throwable t) {
            ExceptionTools.handleSpringBootStartupError(t);
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }
    }

    /**
     * @param evt
     *            the event to process (ApplicationReadyEvent)
     * @throws Exception
     *             on error
     */
    @EventListener
    public void launchApp(final ApplicationReadyEvent evt) throws Exception {
        try {
            app.launchApp();
            System.exit(0);
        }
        catch (final Throwable t) {
            final Throwable cause = t.getCause();
            System.err.println("\n*** Initialization Error: "
                    + ((cause != null) ? cause.getLocalizedMessage() : t.getLocalizedMessage()));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }
    }
}
