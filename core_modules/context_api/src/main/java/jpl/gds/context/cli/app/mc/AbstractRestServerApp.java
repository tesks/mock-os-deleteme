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
package jpl.gds.context.cli.app.mc;

import java.io.File;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.context.api.options.RestCommandOptions;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.config.IWritableProperties;

/**
 * Abstract class for persistent Restful Servers that utilize a writable configuration file
 * (e.g Telemetry Ingestor, Telemetry Processor)
 *
 *
 */
public abstract class AbstractRestServerApp extends AbstractRestfulServerCommandLineApp {

    private static final String   CONFIG_FILE_SHORT = "k";
    /** Writable config file long command-line */
    public static final String CONFIG_FILE_LONG  = "configDir";

    /** Command-line option for specifying a writable config file */
    protected final DirectoryOption configFileOption;

    /** The Writable properties oject */
    protected IWritableProperties writeableProperties;

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     */
    public AbstractRestServerApp() {
        this(true);
    }

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     * 
     * @param addHook
     *            whether or not to add the shutdown hook
     */
    public AbstractRestServerApp(final boolean addHook) {
        super(addHook);
        this.configFileOption = new DirectoryOption(CONFIG_FILE_SHORT, CONFIG_FILE_LONG, "path",
                                               "Writable properties directory for the persistent process", false,
                                               false);
    }


    @Override
    protected BaseCommandOptions createOptions(final BaseCommandOptions opts) {
        if (optionsCreated.get()) {
            return options;
        }
        super.createOptions(opts);

        // This abstract class is intended for server applications that
        // DO NOT allow a user to disable the REST interface
        restOptions.REST_PORT_OPTION.setDescription(getTruncatedRestPortDescription());
        restOptions.REST_PORT_OPTION.setDefaultValue(RestCommandOptions.SPECIFIED_RANDOM_REST_PORT);

        options.addOption(configFileOption);
        return options;
        
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        super.configure(commandLine);

        final String overrideDir = configFileOption.parse(commandLine);
        if (overrideDir != null) {
            writeableProperties.setWritablePropertiesDir(overrideDir);
        }

        final File f = new File(writeableProperties.getWritablePropertiesPath());

        if (!f.exists()) {
            writeableProperties.createAndPopulateNewConfigFile();
        }
    }

}
