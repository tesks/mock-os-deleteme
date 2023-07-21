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
package jpl.gds.shared.cli.app;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import jpl.gds.shared.cli.cmdline.AliasingApacheCommandLineParser;
import jpl.gds.shared.cli.cmdline.ApacheCommandLineParser;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.cmdline.ICommandLineParser;
import jpl.gds.shared.cli.cmdline.OptionSet;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;

/**
 * BaseCommandOptions is a class for containing command line options and parsing
 * command lines. It must be instantiated for a specific instance of
 * ICommandLineApp, and initially contains no command options. Thereafter,
 * CommanLineOption objects can be added, essentially defining the command line
 * for the ICommandLineApp. This class will prevent the addition of conflicting
 * options. It provides special methods for enabling the HELP and VERSION
 * options, because those must call back to the application. It provides an
 * aliasing capability that can replace aliases on the command line with their
 * current standard values. It does this by using a different command line
 * parser when aliasing is enabled by the constructor. The parser itself can be
 * overridden with any instance of ICommandLineParser. Also, this class defines
 * a few standard command line options themselves can be accessed as static
 * objects.
 * 
 * 
 */
public class BaseCommandOptions implements ICommandLineOptionsGroup {

    /** DEBUG Option object */
    public static final FlagOption DEBUG = new FlagOption("d", "debug",
            "run in debug mode", false);

    /** QUIET Option object */
    public static final FlagOption QUIET = new FlagOption(
            "q",
            "quiet",
            "suppress writing to standard output; writing to standard error will still be performed",
            false);
    
    /** GUI option object */
    public static final FlagOption GUI = new FlagOption(
            "g",
            "gui",
            "run with graphical user interface.",
            false);
    
    /** N_GUI option object */
    public static final FlagOption NO_GUI = new FlagOption(
            "H",
            "noGUI",
            "run without a graphical user interface.",
            false);
    
    /**
     * The AUTORUN Option object, used to indicate when application should run
     * without any startup prompts or GUI. 
     */
    public static final FlagOption AUTORUN = new FlagOption(
            "a",
            "autoRun",
            "run without any prompts or configuration window",
            false);


    /** Carries the collection of options that have been added. */
    protected OptionSet options = new OptionSet();

    /** The associated application instance */
    protected final ICommandLineApp application;

    /** The adaptable command line parser */
    protected ICommandLineParser commandLineParser;

    /** The HELP option attached to this instance */
    protected HelpOption helpOption;

    /** The VERSION option attached to this instance */
    protected VersionOption versionOption;

    /** Flag indicating whether aliasing is supported */
    protected boolean aliasing;

    /**
     * Basic constructor for an instance without aliasing.
     * 
     * @param app
     *            the ICommandLineApp instance these options are for.
     * 
     */
    public BaseCommandOptions(final ICommandLineApp app) {
        this(app, false);
    }
    
    /**
     * Constructor for an aliasing or non-aliasing instance that also supplies a parser.
     * 
     * @param app
     *            the ICommandLineApp instance these options are for.
     * @param allowAliasing
     *            true to allow alias substitution on the command line
     * @param parser the command line parser instance to use           
     */
    public BaseCommandOptions(final ICommandLineApp app, final boolean allowAliasing, final ICommandLineParser parser) {
        this.application = app;
        setParser(parser);
        aliasing = allowAliasing;
    }

    /**
     * Constructor for an aliasing or non-aliasing instance.
     * 
     * @param app
     *            the ICommandLineApp instance these options are for.
     * @param allowAliasing
     *            true to allow alias substitution on the command line
     */
    public BaseCommandOptions(final ICommandLineApp app, final boolean allowAliasing) {
        this.application = app;
        if (allowAliasing) {
            setParser(new AliasingApacheCommandLineParser());
            aliasing = true;
        } else {
            setParser(new ApacheCommandLineParser());
        }
    }

    /**
     * Overrides the pre-defined command line parser with a custom parser
     * 
     * @param parserToSet
     *            the ICommandLineParser to set
     */
    public void setParser(final ICommandLineParser parserToSet) {
        if (parserToSet == null) {
            throw new IllegalArgumentException(
                    "Command line parser cannot be null");
        }
        commandLineParser = parserToSet;
    }

    /**
     * Finds a command line option in the set of defined options, by short
     * option name, long option name, or alias.
     * 
     * 
     * @param inputOption
     *            Option name, long or short
     * @return ICommandLineOption, or null if no match is found in the current
     *         option set
     */
    public ICommandLineOption<?> getOption(final String inputOption) {
        return getOption(inputOption, true, true, true);
    }

    /**
     * Finds a command line option in the set of defined options. Flags allow
     * the search to be restricted to short option name, long option name,
     * alias, or any combination. Match is always to short option first, then
     * long option, then alias.
     * 
     * 
     * @param inputOption
     *            Option name, long or short
     * @param matchShort true to match short option name
     * @param matchLong true to match long option name
     * @param matchAlias true to match alias
     * 
     * @return ICommandLineOption, or null if no match is found in the current
     *         option set
     */
    public ICommandLineOption<?> getOption(final String inputOption,
            final boolean matchShort, final boolean matchLong, final boolean matchAlias) {
        
        final Collection<ICommandLineOption<?>> allOptions = options.getAllOptions();
        for (final ICommandLineOption<?> copt : allOptions) {
            if (matchShort && copt.getOpt() != null
                    && inputOption.equals(copt.getOpt())) {
                return copt;
            } else if (matchLong && copt.getLongOpt() != null
                    && inputOption.equals(copt.getLongOpt())) {
                return copt;
            } else if (matchAlias && aliasing && copt.isAlias(inputOption)) {
                return copt;
            }

        }
        return null;
    }

    /**
     * Parses the application command line argument to a ParsedICommandLine object.
     * Looks for missing options, missing arguments, and unrecognized options
     * that can be spotted by the command line parser. If no arguments are
     * entered but some are defined, and the exit flag is true, the application
     * help is displayed before the VM exits with
     * ICommandLineOption.COMMAND_LINE_ERROR. If options are missing but help was
     * also requested, help is displayed and the VM exits with
     * ICommandLineOption.USER_HELP_REQUEST. For all other command line errors,
     * this method will throw and display nothing if the exit flag is false, or
     * display an error to stderr and exit the VM with COMMAND_LINE_ERROR if the
     * exit flag is true.
     * 
     * @param args
     *            the array of command line arguments
     * @param exit
     *            true to exit if a command line parsing error is encountered,
     *            false to throw
     * @return the resulting parsedICommandLine object
     * 
     * @throws ParseException
     *             If there was a command line parsing error and the exit flag
     *             is not true
     */
    public ICommandLine parseCommandLine(final String[] args, final boolean exit) throws ParseException {
        return parseCommandLine(args, exit, true, true);
    }

    /**
     * Parses the application command line argument to a ParsedICommandLine object.
     * Looks for missing options, missing arguments, and unrecognized options
     * that can be spotted by the command line parser. If no arguments are
     * entered but some are defined, and the exit flag is true, the application
     * help is displayed before the VM exits with
     * ICommandLineOption.COMMAND_LINE_ERROR. If options are missing but help was
     * also requested, help is displayed and the VM exits with
     * ICommandLineOption.USER_HELP_REQUEST. For all other command line errors,
     * this method will throw and display nothing if the exit flag is false, or
     * display an error to stderr and exit the VM with COMMAND_LINE_ERROR if the
     * exit flag is true.
     * 
     * @param args
     *            the array of command line arguments
     * @param exit
     *            true to exit if a command line parsing error is encountered,
     *            false to throw
     * @param displayHelp
     *            true to display the help in this method, false to just throw a HelpOrVersionDisplayedException to
     *            indicate that help or version was asked for.
     * @param displayVersion
     *            true to display the version in this method, false to just throw a HelpOrVersionDisplayedException to
     *            indicate that help or version was asked for.
     * @return the resulting parsedICommandLine object
     * 
     * @throws ParseException
     *             If there was a command line parsing error and the exit flag
     *             is not true
     */
    public ICommandLine parseCommandLine(final String[] args, final boolean exit, final boolean displayHelp,
                                         final boolean displayVersion)
            throws ParseException {
        for (int i = 0; i < args.length; ++i) {
            if ((helpOption != null) && !ICommandLineApp.helpDisplayed.get()
                    && (args[i].equals("-" + helpOption.getOpt())
                    || args[i].equals("--" + helpOption.getLongOpt()))) {
                if (displayHelp) {
                    this.application.showHelp();
                    ICommandLineApp.helpDisplayed.set(true);
                }
            }
            if ((versionOption != null) && !ICommandLineApp.versionDisplayed.get()
                    && (args[i].equals("-" + versionOption.getOpt())
                    || args[i].equals("--" + versionOption.getLongOpt()))) {
                if (displayVersion) {
                    this.application.showVersion();
                    ICommandLineApp.versionDisplayed.set(true);
                }
            }
        }
        
        ICommandLine commandLine = null;
        try {
            commandLine = commandLineParser.parse(this.options, args);
        }
        catch (final MissingOptionException | MissingArgumentException | UnrecognizedOptionException e) {
            if (exit) {
                if (ICommandLineApp.versionDisplayed.get() || ICommandLineApp.helpDisplayed.get()) {
                    this.application.setErrorCode(ICommandLineOption.USER_HELP_REQUEST);
                    System.exit(ICommandLineOption.USER_HELP_REQUEST);
                }
                else {
                    System.err.println(e.getMessage());
                    this.application.setErrorCode(ICommandLineOption.COMMAND_LINE_ERROR);
                    System.exit(ICommandLineOption.COMMAND_LINE_ERROR);
                }
            }
            else {
                if (ICommandLineApp.versionDisplayed.get() || ICommandLineApp.helpDisplayed.get()) {
                    throw new HelpOrVersionDisplayedException(ICommandLineApp.helpDisplayed.get(),
                                                              ICommandLineApp.versionDisplayed.get(), e);
                }
                else {
                    throw e;
                }
            }
        }
        return commandLine;
    }

    /**
     * Gets the HelpOption; will return null if no help option has been added.
     * 
     * @return HelpOption, or null if none established
     */
    public HelpOption getHelpOption() {
        return helpOption;
    }

    /**
     * Gets the VersionOption; will return null if no help option has been
     * added.
     * 
     * @return VersionOption, or null if none established
     */
    public VersionOption getVersionOption() {
        return versionOption;
    }

    /**
     * Establishes the help option and adds it to the option set.
     */
    public void addHelpOption() {
        helpOption = new HelpOption(this.application);
        addOption(helpOption);
    }

    /**
     * Establishes the version option and adds it to the option set.
     */
    public void addVersionOption() {
        versionOption = new VersionOption(this.application);
        addOption(versionOption);
    }

    /**
     * Adds a Collection of CommandLineOptions to the option set.
     * 
     * @param optionsToAdd
     *            Collection of options to add
     */
    public void addOptions(final Collection<ICommandLineOption<?>> optionsToAdd) {
        for (final ICommandLineOption<?> opt : optionsToAdd) {
            addOption(opt);
        }

    }

    /**
     * Adds a single ICommandLineOption to the option set.
     * 
     * @param opt
     *            the ICommandLineOption to add
     */
    public void addOption(final ICommandLineOption<?> opt) {
        final String shortName = opt.getOpt();
        final String longName = opt.getLongOpt();
        final Set<String> aliases = opt.getAliases();

        /* Check for overlap with existing short option */
        if (shortName != null
                && getOption(shortName, true, false, false) != null) {
            throw new IllegalArgumentException(
                    "Attempted to add duplicate short command line option: "
                            + opt + ". Option already exists as : "
                            + getOption(shortName, true, false, false).getDescription());
        }

        /* Check for overlap with existing long option */
        if (longName != null && getOption(longName, false, true, false) != null) {
            throw new IllegalArgumentException(
                    "Attempted to add duplicate long command line option: "
                            + opt + ". Option already exists as : "
                            + getOption(longName, false, true, false).getDescription());
        }
        /* If aliasing, check for alias overlaps. */
        if (aliasing) {

            ICommandLineOption<?> aliasOpt = null;

            /* Does the short option overlap with any existing alias? */
            if (shortName != null) {
                aliasOpt = getOption(shortName, false, false, true);
                if (aliasOpt != null) {
                    throw new IllegalArgumentException(
                            "Attempted to add short command line option ("
                                    + shortName
                                    + ") that matches an alias for an existing option: "
                                    + aliasOpt);
                }
            }
            /* Does the long option overlap with any existing alias? */
            if (longName != null) {
                aliasOpt = getOption(longName, false, false, true);
                if (aliasOpt != null) {
                    throw new IllegalArgumentException(
                            "Attempted to add long command line option that matches an alias for an existing option: "
                                    + aliasOpt);
                }
            }

            /*
             * Do any aliases in the new option overlap with any existing short
             * option, long option, or alias?
             */
            if (aliases != null) {
                for (final String alias : aliases) {
                    aliasOpt = getOption(alias);
                    if (aliasOpt != null) {
                        throw new IllegalArgumentException(
                                "Attempted to add command line option with an alias ("
                                        + alias
                                        + ") that matches the short name, long name, or alias of existing option "
                                        + aliasOpt);
                    }
                }
            }

        }
        options.addOption(opt);

    }

    /**
     * Gets the underlying OptionSet object that contains all the defined options.
     * 
     * @return OptionSet object
     */
    public OptionSet getOptions() {
        return this.options;
    }

}
