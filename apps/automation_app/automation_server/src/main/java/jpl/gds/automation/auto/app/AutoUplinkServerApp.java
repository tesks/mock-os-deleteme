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

import jpl.gds.automation.auto.AutoManager;
import jpl.gds.automation.auto.cfdp.service.IAutoCfdpService;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.gdsdb.options.DatabaseCommandOptions;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.security.options.AccessControlCommandOptions;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.cli.app.mc.AbstractRestfulServerCommandLineApp;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.io.PrintWriter;

/**
 * AutoUplinkServerApp is the proxy between AUTO and AMPCS. It handles incoming
 * requests from AUTO and forwards it to the appropriate handler in AMPCS.
 * @since R8
 */
public class AutoUplinkServerApp extends AbstractRestfulServerCommandLineApp implements IAutoProxyApp {

    private final FileOption               logFileOpt   = new FileOption("l", "logFile", "logFile",
                                                                         "Location of file to log activity to", false,
                                                                         true);
    private final FlagOption               logToDbOpt   = new FlagOption("L", "logToDb",
                                                                         "Specify this option to write logs to the database",
                                                                         false);


    private final ApplicationContext       springContext;
    private IContextConfiguration          contextConfig;

    private DatabaseCommandOptions         dbOpts;
    private AccessControlCommandOptions    securityOpts;

    @Autowired
    ApplicationArguments                   appArgs;

    /**
     * Constructor for Automation Uplink Server application
     * 
     * @param appContext
     *            the current application context
     */
    public AutoUplinkServerApp(final ApplicationContext appContext) {
        super(false);
        springContext = appContext;
        ILogMessageLDIStore.setMsgPrefixForSession("");
    }


    /**
     * Parses the command line after initializing the constructor
     * 
     */
    @PostConstruct
    public void configure() {
        contextConfig = new SessionConfiguration(springContext);
        try {
            configure(createOptions()
                    .parseCommandLine(appArgs.getSourceArgs(), true));
        } catch (final ParseException e) {
            TraceManager.getDefaultTracer().error(ExceptionTools.getMessage(e));
        }
        springContext.getBean(AutoManager.class).setSessionConfiguration(contextConfig);
    }

    @Override
    public BaseCommandOptions createOptions() {
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions(springContext.getBean(BaseCommandOptions.class, this));
        
        restOptions.REST_PORT_OPTION.setDefaultValue(8384);

        dbOpts = new DatabaseCommandOptions(springContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class));
        securityOpts = new AccessControlCommandOptions(springContext.getBean(SecurityProperties.class),
                contextConfig.getAccessControlParameters());

        options.addOptions(dbOpts.getAllOptionsWithoutNoDb());

        options.addOption(logFileOpt);

        // Added logToDb flag to enable/disable AUTO database logging

        options.addOption(logToDbOpt);

        options.addOptions(securityOpts.getAllNonGuiOptions());

        return options;
    }


    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);
        dbOpts.parseAllOptionsAsOptional(commandLine);
        securityOpts.parseAllNonGuiOptionsAsOptional(commandLine);

        // Loading the manager here will allow it to pick up any parsed security and archive options
        final AutoManager manager = springContext.getBean(AutoManager.class);

        // Set AutoManager database logging accordingly
        manager.setLogToDb(logToDbOpt.parse(commandLine));

        final String logFile = logFileOpt.parse(commandLine);
        if (logFile != null && logFile.length() > 0) {
            manager.setLogFile(logFile);
        }

    }

    @Override
    public void showHelp() {
        if (helpDisplayed.getAndSet(true)) {
            return;
        }

        final PrintWriter pw = new PrintWriter(System.out);
        pw.print("Usage: " + jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName());

        pw.println(
                " <options>\n\n" + "This utility acts as a proxy for communication " + "between AUTO and AMPCS.\n\n");

        options.getOptions().printOptions(pw);
        
        pw.flush();
    }

}
