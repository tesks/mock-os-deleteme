/*
 * Copyright 2006-2020. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.mds.server;

import jpl.gds.common.error.ErrorCode;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.spring.context.SpringContextFactory;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.integration.config.EnableIntegration;

/**
 *
 * MonitorDataServiceApp is the main class for the Monitor Data Service.
 */

// this variable is resolved on class load, through #buildScanPath(Package pkg).
@SpringBootApplication(scanBasePackages = {"${springScanPath}"})
@PropertySource("classpath:config/mds_spring.properties")
@EnableIntegration
public class MonitorDataServiceApp {
    @SuppressWarnings("unused")
    private static final List<String> SCAN_PATH = buildScanPath(MonitorDataServiceApp.class.getPackage());

    @Autowired
    private MonitorDataService app;

    /**
     * Main application class.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        try {
            final SpringApplication app = new SpringApplicationBuilder().bannerMode(Banner.Mode.OFF).
                     sources(MonitorDataServiceApp.class, MdsSpringBootstrap.class).
                     build();

            //do not start embedded server
            app.setWebApplicationType(WebApplicationType.NONE);
            app.setRegisterShutdownHook(false);
            //MPCS-11493 - Allow bean override
            app.setAllowBeanDefinitionOverriding(true);

            app.run(args);
        }
        catch (Exception e){
            ExceptionTools.handleSpringBootStartupError(e);
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }
    }

    /**
     * Launch app on event
     * @param evt the event to process (ApplicationReadyEvent)
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

    /**
     * Returns a List of scan path locations customized for MDS.
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
        scanPath.addAll(SpringContextFactory.getSpringBootstrapScanPath(true));
        scanPath.add(bootStrapPackage.getName() + ".mc.rest.controllers");
        GdsSpringSystemProperties.setScanPath(scanPath);
        return scanPath;
    }
}
