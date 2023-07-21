/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.cfdp.processor;

import jpl.gds.cfdp.common.commandline.CfdpCommandLineUtil;
import jpl.gds.cfdp.common.config.EConfigurationPropertyKey;
import jpl.gds.cfdp.processor.engine.CfdpProcessorEngine;
import jpl.gds.shared.cli.cmdline.AliasingApacheCommandLineParser;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.GdsSpringSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.SpringContextFactory;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.context.annotation.PropertySource;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static jpl.gds.cfdp.processor.config.ConfigurationManager.WRITABLE_CONFIG_FILE_PROPERTY_NAME;

/**
 * CFDP Processor Application
 */
@SpringBootApplication(scanBasePackages = {"${springScanPath}"})
@EnableSwagger2
@PropertySource("classpath:config/cfdp_processor_spring.properties")
public class CfdpProcessorApp implements ApplicationRunner {

    @SuppressWarnings("unused")
    private static final List<String> SCAN_PATH = buildScanPath(CfdpProcessorApp.class.getPackage());

    @Autowired
    private CfdpProcessorEngine cfdpProcessorEngine;

    /** command line handler */
    public static AmpcsStyleCommandLineHandler commandLineHandler;

    /**
     * Returns a list of scan path locations customized for this CFDP Processor
     * application. Side Effect: sets the System Property "scanPath" with a
     * comma-delimited version of this path for consumption by the spring-boot /
     * spring-mvc annotations and initialization.
     *
     * @param bootStrapPackage the Package to scan for bootstrap class specialized for CFDP
     *                         Processor.
     * @return Build spring-boot classpath. Located in static method so that tests
     * can also take advantage of it.
     */
    public static List<String> buildScanPath(final Package bootStrapPackage) {
        final List<String> scanPath = new ArrayList<>();
        scanPath.add(bootStrapPackage.getName());
        scanPath.addAll(SpringContextFactory.getSpringBootstrapScanPath(true));
        GdsSpringSystemProperties.setScanPath(scanPath);
        return scanPath;
    }

    /**
     * Main method
     * @param args App arguments
     */
    public static void main(final String[] args) {
        commandLineHandler = new AmpcsStyleCommandLineHandler();
        final List<String> optionArgs = new ArrayList<>(args.length);
        final List<String> nonOptionArgs = new ArrayList<>(args.length);

        try {
            // MPCS-11675 - Shakeh Brys - 04/01/2020 - Need to get dealiased version of options to pass to divideArgs
            String[] dealiasedArgs = new AliasingApacheCommandLineParser().dealias(commandLineHandler.createOptions().getOptions(), args);
            CfdpCommandLineUtil.divideArgs(new DefaultParser(), commandLineHandler.createCommonsCliOptions(), dealiasedArgs, optionArgs,
                    nonOptionArgs);
            final ICommandLine commandLine = commandLineHandler.createOptions()
                    .parseCommandLine(optionArgs.toArray(new String[0]), true);
            commandLineHandler.configure(commandLine);

        } catch (final ParseException pe) {
            TraceManager.getTracer(Loggers.CFDP).error("Error parsing arguments: " + ExceptionTools.getMessage(pe));
            System.exit(1);
        }

        // MPCS-11511 Do not allow non options
        if(!nonOptionArgs.isEmpty()) {
            TraceManager.getTracer(Loggers.CFDP).error("Unrecognized options: " + nonOptionArgs);
            System.exit(1);
        }

        if (!commandLineHandler.configLoadedOk()) {
            System.exit(1);
        }

        final Properties configProperties = commandLineHandler.getConfigProperties();

        // Set the configuration file path for ConfigurationManager to pick up
        configProperties.put(WRITABLE_CONFIG_FILE_PROPERTY_NAME, commandLineHandler.getConfigFile());

        // Set the server port property that the Spring Application will recognize
        configProperties.put("server.port",
                configProperties.get(EConfigurationPropertyKey.PORT_PROPERTY.toString()));

        final SpringApplication app = new SpringApplicationBuilder().sources(CfdpProcessorApp.class).bannerMode(
            Banner.Mode.OFF).properties(configProperties).build();

        // MPCS-11493 - Allow bean override
        app.setAllowBeanDefinitionOverriding(true);

        // Shakeh Brys - MPCS-11675 - Improve error when SSL issue crashed app
        try {
            app.run(nonOptionArgs.toArray(new String[0]));
        } catch (WebServerException wse) {
            TraceManager.getTracer(Loggers.CFDP).error("Error starting webserver: " + ExceptionTools.getMessage(wse));
        }
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        // 5/2022 MCSECLIV-992 -> MPCS-12390: removed static member objects that were not being set
        //      venue, testbed name, subtopic, and downlink stream ID

        cfdpProcessorEngine.start();
    }

}