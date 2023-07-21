/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.tc.mps.impl.app;

import jpl.gds.common.error.ErrorCode;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.bootstrap.SharedSpringBootstrap;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.exception.CommandFileParseException;
import jpl.gds.tc.impl.spring.bootstrap.TcSpringBootstrap;
import jpl.gds.tc.mps.impl.ctt.repo.CommandTranslationTableFileRepository;
import jpl.gds.tc.mps.impl.ctt.repo.ICommandTranslationTableRepository;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Spring command line app for managing compiled command dictionaries.
 *
 * only compilation is supported
 */
@Configuration
public class CompileCommandDictionaryApp extends AbstractCommandLineApp {

    private static final String DEFAULT_APP_NAME = "chill_compile_command_dict";

    public static void main(final String[] args) {
        // set the app name if not configured from env vars
        if (GdsSystemProperties.getSystemProperty(ApplicationConfiguration.APP_NAME) == null) {
            GdsSystemProperties.setSystemProperty(ApplicationConfiguration.APP_NAME, DEFAULT_APP_NAME);
        }

        final SpringApplication springApplication = new SpringApplicationBuilder(CompileCommandDictionaryApp.class,
                TcSpringBootstrap.class, CommonSpringBootstrap.class, SharedSpringBootstrap.class)
                .bannerMode(Banner.Mode.OFF).web(WebApplicationType.NONE).build();
        springApplication.setRegisterShutdownHook(false);

        try {
            springApplication.run(args);
        } catch (final Exception e) {
            ExceptionTools.handleSpringBootStartupError(e);
            System.exit(1);
        }
    }

    /**
     * Spring event listener
     *
     * @param evt application ready event
     */
    @EventListener
    public void runApp(final ApplicationReadyEvent evt) {

        final ApplicationContext   applicationContext = evt.getApplicationContext();
        final ApplicationArguments args               = applicationContext.getBean(ApplicationArguments.class);

        try {
            final CompileCommandDictionaryApp ccd = applicationContext.getBean(CompileCommandDictionaryApp.class);
            final ICommandLine cmdLine = ccd.createOptions()
                    .parseCommandLine(args.getSourceArgs(), true);
            ccd.configure(cmdLine);

            ccd.run();
            System.exit(0);
        } catch (final Exception e) {
            final Throwable cause = e.getCause();
            System.err.println("\n*** Initialization Error: "
                    + ((cause != null) ? cause.getLocalizedMessage() : e.getLocalizedMessage()));
            System.exit(ErrorCode.UNKNOWN_ERROR_CODE.getNumber());
        }
    }

    private final Tracer            tracer;
    private final CommandProperties commandProperties;

    private FileOption      commandFileXmlOpt;
    private DirectoryOption outputDirOpt;
    private String          commandXmlPath;
    private String          outputDirPath;


    /**
     * Constructor accepting an app context. Will extract a tracer from the app context.
     *
     * @param applicationContext spring application context
     */
    @Autowired
    public CompileCommandDictionaryApp(final ApplicationContext applicationContext) {
        this(applicationContext.getBean(CommandProperties.class), TraceManager.getDefaultTracer(applicationContext));
    }

    /**
     * Constructor accepting a tracer.
     *
     * @param tracer log tracer
     */
    public CompileCommandDictionaryApp(final CommandProperties commandProperties, final Tracer tracer) {
        this.commandProperties = commandProperties;
        this.tracer = tracer;
    }

    /**
     * Run method. Will create a repository instance and attempt to compile the configured command dictionary XML.
     */
    public void run() {

        try {
            final ICommandTranslationTableRepository repo = new CommandTranslationTableFileRepository(
                    commandXmlPath, outputDirPath, commandProperties, tracer);
            final String forwardCompiledPath = repo.getForwardTranslationTablePath();
            final String reverseCompiledPath = repo.getReverseTranslationTablePath();

            tracer.info("Forward command translation table has been compiled to ", forwardCompiledPath);
            tracer.info("Reverse command translation table has been compiled to ", reverseCompiledPath);
        } catch (final CommandFileParseException e) {
            tracer.error("An error occurred while compiling the XML command dictionary file. ", e.getMessage());
        }
    }

    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }

        super.createOptions();

        commandFileXmlOpt = new FileOption("c", "commandXml", "filename",
                "Path to command dictionary XML", true, true);
        outputDirOpt = new DirectoryOption("o", "outputDir", "directory",
                "Output directory for compiled command dictionary", true, true);

        options.addOption(commandFileXmlOpt);
        options.addOption(outputDirOpt);

        return options;
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        this.commandXmlPath = commandFileXmlOpt.parse(commandLine);
        this.outputDirPath = outputDirOpt.parse(commandLine);
    }
}
