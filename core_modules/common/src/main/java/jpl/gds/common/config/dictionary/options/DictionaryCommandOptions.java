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
package jpl.gds.common.config.dictionary.options;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.ParseException;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.StringOptionParser;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOptionParser;
import jpl.gds.shared.config.GdsSystemProperties;

/**
 * This class creates command line option objects used for parsing dictionary
 * options and automatically setting the parsed values into a
 * DictionaryConfiguration object. It can utilize the global
 * DictionaryConfiguration object, or can be supplied with a unique instance in
 * the constructor. Once an instance of this class is constructed, it provides
 * public members for each defined option, which can be individually added to a
 * class that extends BaseCommandOptions and can be individually parsed by an
 * application. Alternatively, there are convenience methods to get or parse
 * collections of options.
 * 
 */
public class DictionaryCommandOptions implements ICommandLineOptionsGroup {
    
	/**
	 * Long option name for the FSW dictionary directory.
	 */
    public static final String FSW_DIR_LONG_OPT = "fswDictionaryDir";
    /**
	 * Long option name for the FSW version.
	 */
    public static final String FSW_VERSION_LONG_OPT = "fswVersion";
    /**
     *  FSW directory description.
     */
    public static final String FSW_DIR_DESC = "FSW dictionary directory (may be command, telemetry or some other entity)";
    /**
     *  FSW version description.
     */
    public static final String FSW_VER_DESC = "flight software version";
    /**
   	 * Long option name for the SSE dictionary directory.
   	 */
    public static final String SSE_DIR_LONG_OPT = "sseDictionaryDir";
    /**
     * SSE directory description
     */
    public static final String SSE_DIR_DESC = "SSE dictionary directory (may be command, telemetry or some other entity)";
    /**
	 * Long option name for the FSW version.
	 */
    public static final String SSE_VERSION_LONG_OPT = "sseVersion";
    /**
     * SSE version description
     */
    public static final String SSE_VERSION_DESC = "simulation & support equipment software dictionary version";

    private final DictionaryProperties dictConfig;

    /**
     * The FSW_DICTIONARY_DIRECTORY command option. Parsing this option sets the
     * FSW dictionary directory in the DictionaryConfiguration member instance.
     * The existence of the directory is verified. A default value is defined.
     */
    public final DirectoryOption FSW_DICTIONARY_DIRECTORY = new DirectoryOption(
            "F",
            FSW_DIR_LONG_OPT,
            "directory",
            FSW_DIR_DESC,
            false, true);

    /**
     * The FSW_VERSION command option. Parsing this option sets the FSW
     * dictionary version in the DictionaryConfiguration member instance. A
     * default value is defined.
     */
    public final StringOption FSW_VERSION = new StringOption("D", FSW_VERSION_LONG_OPT,
            "version", FSW_VER_DESC, false);

    /**
     * The SSE_DICTIONARY_DIRECTORY command option. Parsing this option sets the
     * SSE dictionary directory in the DictionaryConfiguration member instance.
     * The existence of the directory is verified. A default value is defined.
     */
    public final DirectoryOption SSE_DICTIONARY_DIRECTORY = new DirectoryOption(
            "T",
            SSE_DIR_LONG_OPT,
            "directory", SSE_DIR_DESC,
            false, true);

    /**
     * The SSE_DICTIONARY_DIRECTORY command option. Parsing this option sets the
     * SSE dictionary version in the DictionaryConfiguration member instance. A
     * default value is defined.
     */
    public final StringOption SSE_VERSION = new StringOption("W", SSE_VERSION_LONG_OPT,
            "version",
            SSE_VERSION_DESC, false);

    /**
     * Constructor that takes a unique instance of DictionaryConfiguration. The
     * supplied DictionaryConfiguration will be used both to determine defaults,
     * and to set parsed values into.
     * 
     * @param config
     *            the DictionaryConfiguration instance to use
     */
    public DictionaryCommandOptions(final DictionaryProperties config) {
        this.dictConfig = config;

        FSW_DICTIONARY_DIRECTORY.setParser(new FswDictionaryDirOptionParser());
        FSW_VERSION.setParser(new FswVersionOptionParser());
        SSE_DICTIONARY_DIRECTORY.setParser(new SseDictionaryDirOptionParser());
        SSE_VERSION.setParser(new SseVersionOptionParser());

    }

 
    /**
     * Gets the collection of flight software related command options.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getFswOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();

        result.add(FSW_DICTIONARY_DIRECTORY);
        result.add(FSW_VERSION);

        return result;

    }

    /**
     * Gets the collection of SSE related command options.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getSseOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();

        result.add(SSE_DICTIONARY_DIRECTORY);
        result.add(SSE_VERSION);

        return result;

    }

    /**
     * Gets the collection of all command options defined by this class.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();

        result.addAll(getFswOptions());
        result.addAll(getSseOptions());

        return result;
    }

    /**
     * Gets the collection of all command options defined by this class that
     * cannot be used to override the contents of a session configuration file.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getNonOverridableOptions() {
        return getAllOptions();

    }

    /**
     * Parses the all the options defined by this class from the supplied
     * command line object. Requires none of the options, and sets no defaults.
     * Result values are set into the DictionaryConfiguration member.
     * 
     * @param commandLine
     *            the parsed command line options
     * @throws ParseException
     *             if there is a parse error
     */
    public void parseAllOptionsAsOptional(final ICommandLine commandLine) throws ParseException {
        FSW_DICTIONARY_DIRECTORY.parse(commandLine);
        FSW_VERSION.parse(commandLine);
        SSE_DICTIONARY_DIRECTORY.parse(commandLine);
        SSE_VERSION.parse(commandLine);
    }
    
    /**
     * Parses the all the options defined by this class from the supplied
     * command line object. Requires none of the options, and sets defaults for
     * those not present.
     * Result values are set into the DictionaryConfiguration member.
     * 
     * @param commandLine
     *            the parsed command line options
     * @throws ParseException
     *             if there is a parse error
     */
    public void parseAllOptionsAsOptionalWithDefaults(final ICommandLine commandLine) throws ParseException {
        FSW_DICTIONARY_DIRECTORY.parseWithDefault(commandLine, false, true);
        FSW_VERSION.parseWithDefault(commandLine, false, true);
        SSE_DICTIONARY_DIRECTORY.parseWithDefault(commandLine, false, true);
        SSE_VERSION.parseWithDefault(commandLine, false, true);
    }

    /**
     * Gets the DictionaryConfiguration member object.
     * 
     * @return DictionaryConfiguration; never null
     */
    public DictionaryProperties getDictionaryConfiguration() {
        return dictConfig;
    }

    /**
     * Check if a directory exists
     * @param directory full absolute path to directory
     * @return boolean, directory exists
     */
    private boolean dictionaryDirExists(final String directory) {
        final File f = new File(directory);
        return f.exists() && f.isDirectory();
    }

    /**
     * An option parser class for the FSW_VERSION command line option. Parsed
     * value will be set into the DictionaryConfiguration.
     * 
     */
    protected class FswVersionOptionParser extends StringOptionParser {

        /**
         * Constructor. Sets the default value.
         */
        public FswVersionOptionParser() {
            setDefaultValue(dictConfig.getDefaultFswVersion());
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.AbstractOptionParser#parseWithDefault(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption, boolean, boolean)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String version = super.parse(commandLine, opt);

            if (version != null) {
                final String dir = dictConfig.getFswDictionaryDir() + File.separator + GdsSystemProperties
                        .getSystemMission() + File.separator + version;
                if (!dictionaryDirExists(dir)) {
                    throw new ParseException("FSW dictionary version directory " + dir + " does not exist");
                }
                dictConfig.setFswVersion(version);
            }

            return version;
        }

    }

    /**
     * An option parser class for the SSE_VERSION command line option. Parsed
     * value will be set into the DictionaryConfiguration.
     * 
     */
    protected class SseVersionOptionParser extends StringOptionParser {

        /**
         * Constructor. Sets the default value.
         */
        public SseVersionOptionParser() {
            setDefaultValue(dictConfig.getDefaultSseVersion());
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String version = super.parse(commandLine, opt);

            if (version != null) {
                final String dir = dictConfig.getSseDictionaryDir() + File.separator + GdsSystemProperties
                        .getSseNameForSystemMission() + File.separator + version;
                if (!dictionaryDirExists(dir)) {
                    throw new ParseException("SSE dictionary version directory " + dir + " does not exist");
                }
                dictConfig.setSseVersion(version);
            }

            return version;
        }

    }

    /**
     * An option parser class for the FSW_DICTIONARY_DIR command line option.
     * Parsed value will be set into the DictionaryConfiguration.
     * 
     */
    protected class FswDictionaryDirOptionParser extends DirectoryOptionParser {

        /**
         * Constructor. Sets the default value.
         */
        public FswDictionaryDirOptionParser() {
            super(true);
            setDefaultValue(dictConfig.getDefaultFswDictionaryDir());
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
                dictConfig.setFswDictionaryDir(dir);

                final String overrideVersion = dictConfig.getDefaultFswVersion();
                if (overrideVersion == null) {
                    throw new ParseException(
                            "No FSW dictionaries present in FSW dictionary override path: " + dir);
                }

                dictConfig.setFswVersion(overrideVersion);
                FSW_VERSION.setDefaultValue(overrideVersion);
            }

            return dir;

        }

    }

    /**
     * An option parser class for the SSE_DICTIONARY_DIR command line option.
     * Parsed value will be set into the DictionaryConfiguration.
     * 
     */
    public class SseDictionaryDirOptionParser extends DirectoryOptionParser {

        /**
         * Constructor. Sets the default value.
         */
        public SseDictionaryDirOptionParser() {
            super(true);
            setDefaultValue(dictConfig.getDefaultSseDictionaryDir());
        }

        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String dir = super.parse(commandLine, opt);

            if (dir != null) {
                dictConfig.setSseDictionaryDir(dir);
                final String overrideVersion = dictConfig.getDefaultSseVersion();
                if (overrideVersion == null) {
                    throw new ParseException(
                            "No SSE dictionaries present in SSE dictionary override path: " + dir);
                }

                dictConfig.setSseVersion(overrideVersion);
                SSE_VERSION.setDefaultValue(overrideVersion);
            }
            return dir;

        }

    }

    /**
     * @param fswDictionaryDir
     *            fsw dictionary directory long option value
     * @param fswVersion
     *            fsw version long option value
     * @param sseDictionaryDir
     *            sse dictionary directory long option value
     * @param sseVersion
     *            sse version long option value
     * @return an array list of command-line arguments and their arguments - if present
     */
    public static List<String> buildDictionaryCliFromArgs(final String fswDictionaryDir, final String fswVersion,
                                                               final String sseDictionaryDir, final String sseVersion) {
        final List<String> argList = new ArrayList<>();

        if (fswDictionaryDir != null) {
            argList.add(DASHES + DictionaryCommandOptions.FSW_DIR_LONG_OPT);
            argList.add(fswDictionaryDir);
        }
        if (fswVersion != null) {
            argList.add(DASHES + DictionaryCommandOptions.FSW_VERSION_LONG_OPT);
            argList.add(fswVersion);
        }
        if (sseDictionaryDir != null) {
            argList.add(DASHES + DictionaryCommandOptions.SSE_DIR_LONG_OPT);
            argList.add(sseDictionaryDir);
        }
        if (sseVersion != null) {
            argList.add(DASHES + DictionaryCommandOptions.SSE_VERSION_LONG_OPT);
            argList.add(sseVersion);
        }
        return argList;
    }

}
