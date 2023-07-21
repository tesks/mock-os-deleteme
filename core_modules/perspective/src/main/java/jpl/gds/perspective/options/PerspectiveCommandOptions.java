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
package jpl.gds.perspective.options;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.ParseException;

import jpl.gds.perspective.ApplicationConfiguration;
import jpl.gds.perspective.ApplicationType;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.IOptionParser;
import jpl.gds.shared.cli.options.AbstractOptionParser;
import jpl.gds.shared.cli.options.CommandLineOption;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOptionParser;
import jpl.gds.shared.exceptions.ExceptionTools;

/**
 * This class creates command line option objects used for parsing perspective
 * options and automatically creating perspective and application configuration
 * objects. Once an instance of this class is constructed, it provides public
 * members for each defined option, which can be individually added to a class
 * that extends BaseCommandOptions and can be individually parsed by an
 * application. Alternatively, there are convenience methods to get or parse
 * collections of options.
 * <p>
 * CAVEATS: At this time, it is impossible to create a unique instance of
 * Perspective. Parsing options in this class will affect the one and only
 * global Perspective.
 * 
 *
 */
public final class PerspectiveCommandOptions implements ICommandLineOptionsGroup {

    /**
     * Short option name.
     */
    public static final String SHORT_OPTION = "p";
    
    /**
     * Long option name.
     */
    public static final String LONG_OPTION = "perspective";

    private final ApplicationType appType;

    /**
     * The APPLICATION_CONFIGURATION command line option. This is a HIDDEN
     * option, i.e., it will not show up in the option help. Verifies that the
     * value points to an existing file. Creates the global perspective
     * instance, parses the application configuration file, and assigns the
     * resulting ApplicationConfiguration object to the global perspective.
     */
    public final ICommandLineOption<ApplicationConfiguration> APPLICATION_CONFIGURATION  = new CommandLineOption<>(
            "U",
            "appConfig",
            true,
            "filename",
            "hidden option for the location of the application configuration file.",
            false, new ApplicationConfigurationParser());

    /**
     * The INTEGRATED_CHILL command line option. This is a HIDDEN option, i.e.,
     * it will not show up in the option help. Nothing is done with the parsed
     * value, which must be handled by the application.
     */
    public final FlagOption INTEGRATED_CHILL = new FlagOption(
            null,
            "integratedChill",
            "hidden option used to determine if processes are truly standalone or child processes.",
            false);

    /**
     * The PERSPECTIVE command line option. Verifies that the value points to an
     * existing directory. Creates the global perspective instance from the
     * supplied directory. Will overwrite any existing application ID file.
     */
    public final DirectoryOption PERSPECTIVE = new DirectoryOption(SHORT_OPTION,
            LONG_OPTION, "perspectivePath", "directory name of GUI perspective.",
            false, true);

    /**
     * The PERSPECTIVE_NULL_OVERWRITE command line option. Verifies that the
     * value points to an existing directory. Creates the global perspective
     * instance from the supplied directory. Will generate a new application ID
     * file only if the existing application ID in the perspective is null.
     */
    public final DirectoryOption PERSPECTIVE_NULL_OVERWRITE = new DirectoryOption(
            SHORT_OPTION, LONG_OPTION, "perspectivePath",
            "directory name of GUI perspective.", false, true);
    
    private final boolean fswOnly;
    private final boolean sseOnly;

	private final PerspectiveConfiguration perspective;
    
    /**
     * Constructor. Integrated applications should supply the UNKNOWN
     * ApplicationType as argument.
     * 
     * @param config
     *            the perspective configuration
     * @param type
     *            the current ApplicationType
     * @param fswOnly
     *            only use fsw
     * @param sseOnly
     *            only use sse
     */
    public PerspectiveCommandOptions(final PerspectiveConfiguration config, final ApplicationType type, final boolean fswOnly, final boolean sseOnly) {
    	if (type == null) {
    		throw new IllegalArgumentException("Application type cannot be null");
    	}
    	this.perspective = config;
    	this.fswOnly = fswOnly;
    	this.sseOnly = sseOnly;
        this.appType = type;
        PERSPECTIVE.setParser(new PerspectiveOptionParser(true));
        PERSPECTIVE_NULL_OVERWRITE
        .setParser(new PerspectiveOptionParser(false));
        INTEGRATED_CHILL.setHidden(true);
        APPLICATION_CONFIGURATION.setHidden(true);

    }

    /**
     * Constructor. Integrated applications should supply the UNKNOWN
     * ApplicationType as argument.
     * 
     * @param config
     *            the perspective configuration
     * @param type
     *            the current ApplicationType
     */
    public PerspectiveCommandOptions(final PerspectiveConfiguration config, final ApplicationType type) {
    	this(config, type, false, false);
    }
    
    /**
     * Returns the application type associated with these command options.
     * 
     * @return ApplicationType; never null
     */
    public ApplicationType getApplicationType() {
        return this.appType;
    }

    /**
     * An option parser for the PERSPECTIVE option. Verifies the directory
     * exists and creates the global perspective instance from it. A new
     * application ID will be assigned, and a new application ID file written,
     * if the overwriteAppId flag is set to true, or if the current application
     * ID is null.
     * 
     *
     */
    private class PerspectiveOptionParser extends DirectoryOptionParser {

        private final boolean overwriteId;

        /**
         * Constructor
         * 
         * @param overwriteAppId
         *            true if the existing application ID file should be
         *            overwritten.
         */
        public PerspectiveOptionParser(final boolean overwriteAppId) {
            super(true);
            this.overwriteId = overwriteAppId;
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.filesystem.DirectoryOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String dir = super.parse(commandLine, opt);

            if (dir != null) {
                try {

                	perspective.create(dir);
                	if (overwriteId
                			|| perspective
                			.getAppId() == null) {

                		try {
                			perspective.assignNewAppId();
                			perspective.writeAppId();
                		} catch (final IOException e) {
                			throw new ParseException(
                					"Cannot write application ID file. Cannot execute");
                		}
                    }

                } catch (final Exception e) {
                    throw new ParseException(
                            "Problem loading perspective configuration " + dir
                                    + ": " + ExceptionTools.getMessage(e));
                }
            }
            return dir;
        }
    }

    /**
     * An option parser for the APPLICATION_CONFIGURATION option. Verifies the
     * file exists, creates the global perspective instance, parses the
     * application file, and assigns it to the global perspective.
     * 
     *
     */
    private class ApplicationConfigurationParser extends
    AbstractOptionParser<ApplicationConfiguration> implements
    IOptionParser<ApplicationConfiguration> {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public ApplicationConfiguration parse(final ICommandLine commandLine,
                final ICommandLineOption<ApplicationConfiguration> opt)
                        throws ParseException {

            ApplicationConfiguration appConfig = null;

            final String file = getValue(commandLine, opt);

            if (file != null) {

                final File f = new File(file);
                if (!f.exists()) {
                    throw new ParseException("Application configuration file "
                            + file + " was not found");
                }

                try {
                    perspective.createFromApplicationIdFile(file);
                } catch (final Exception e) {
                    throw new ParseException(
                            "Problem loading perspective configuration from application ID file "
                                    + file + ": " + e.getMessage());
                }

                appConfig = perspective
                        .getApplicationConfiguration(appType, fswOnly, sseOnly);

                if (appConfig == null) {
                    throw new ParseException(
                            "Application configuration file could not be read from perspective directory "
                                    + perspective
                                    .getConfigPath());
                }
            }
            return appConfig;
        }

    }
}
