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
package jpl.gds.tc.api.options;

import java.util.*;

import jpl.gds.shared.cli.options.*;
import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.filesystem.FileOptionParser;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.ExecutionStringType;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.config.UplinkParseException;

/**
 * Class UplinkCommandOptions
 *
 */
public class UplinkCommandOptions implements ICommandLineOptionsGroup {
    /** Short command option for binary output **/
    public static final String              UPLINK_FILE_PARAM_SHORT            = "b";

    /** Long command option for binary output **/
    public static final String              UPLINK_FILE_PARAM_LONG             = "binaryOutput";

    /** Short command option for SCMF generation only **/
    public static final String              UPLINK_ONLY_WRITE_SCMF_PARAM_SHORT = "k";

    /** Long command option for SCMF generation only **/
    public static final String              UPLINK_ONLY_WRITE_SCMF_PARAM_LONG  = "onlySCMF";

    /** Short command option for execution string ID **/
    public static final String              UPLINK_STRINGID_PARAM_SHORT        = "s";

    /** Long command option for execution string ID **/
    public static final String              UPLINK_STRINGID_PARAM_LONG         = "stringId";

    /** Short command option for SCMF file path **/
    public static final String              UPLINK_SCMF_PARAM_SHORT            = "o";

    /** Long command option for SCMF file path **/
    public static final String              UPLINK_SCMF_PARAM_LONG             = "scmf";

    /** Short command option for the view dictionary release option **/
    public static final String              UPLINK_RELEASE_PARAM_SHORT         = "r";

    /** Long command option for the view dictionary release option **/
    public static final String              UPLINK_RELEASE_PARAM_LONG          = "release";

    /** Uplink command option for binary files */
    public final FileOption                 UPLINK_BINARY_FILE_PARAM;

    /** Uplink command option for ONLY SCMF */
    public final FlagOption                 UPLINK_ONLY_WRITE_SCMF_PARAM;

    /** Uplink command option for String ID execution */
    public final DynamicEnumOption<ExecutionStringType> UPLINK_STRINGID_PARAM;

    /** Uplink command option for SCMF generation */
    public final FileOption                 UPLINK_SCMF_PARAM;

    /** Uplink command option for Release verification */
    public final StringOption               UPLINK_RELEASE_PARAM;

    /** Uplink command option for bit rates */
    public final UplinkBitRateCommandOption UPLINK_BIT_RATE_PARAM;

    /** Local private storage of constructor arguments and their derivatives. */
    private final ApplicationContext        appContext;
    private final CommandProperties         cmdProperties;
    private final CommandFrameProperties    cmdFrameProperties;
    private final ScmfProperties            scmfProperties;
    private final MissionProperties         missionProperties;

    /**
     * @param appContext
     *            the Sprint Application Context
     */
    public UplinkCommandOptions(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.cmdProperties = appContext.getBean(CommandProperties.class);
        this.cmdFrameProperties = appContext.getBean(CommandFrameProperties.class);
        this.scmfProperties = appContext.getBean(ScmfProperties.class);
        this.missionProperties = appContext.getBean(MissionProperties.class);

        this.UPLINK_BINARY_FILE_PARAM = new FileOption(UPLINK_FILE_PARAM_SHORT, UPLINK_FILE_PARAM_LONG,
                "binaryUplinkCaptureFile", "Specify a binary file on disk to write uplink data to instead "
                        + "of sending uplink to FSW or SSE. No SCMF will be written.",
                false, false);
        this.UPLINK_BINARY_FILE_PARAM.setParser(new BinaryUplinkFileParser());

        this.UPLINK_ONLY_WRITE_SCMF_PARAM = new FlagOption(UPLINK_ONLY_WRITE_SCMF_PARAM_SHORT,
                UPLINK_ONLY_WRITE_SCMF_PARAM_LONG,
                "Only create SCMF file (do not send uplink over the socket or publish to the message service).", false);
        this.UPLINK_ONLY_WRITE_SCMF_PARAM.setParser(new OnlyWriteScmfOptionParser());

        this.UPLINK_BIT_RATE_PARAM = new UplinkBitRateCommandOption(missionProperties.getAllowedUplinkBitrates(), true);
        this.UPLINK_BIT_RATE_PARAM.setParser(new UplinkBitRateParser(missionProperties.getAllowedUplinkBitrates()));

        this.UPLINK_STRINGID_PARAM = new DynamicEnumOption<>(ExecutionStringType.class,
                                                             UPLINK_STRINGID_PARAM_SHORT, UPLINK_STRINGID_PARAM_LONG, "stringId",
                                                             "Specify the Spacecraft String ID to identify to which RCE commands are being directed.",
                                                             false, new LinkedList<>(Arrays.asList(ExecutionStringType.values())));


        this.UPLINK_STRINGID_PARAM.setParser(new StringIdOptionParser());

        this.UPLINK_SCMF_PARAM = new FileOption(UPLINK_SCMF_PARAM_SHORT, UPLINK_SCMF_PARAM_LONG, "scmfFile",
                "Specify the SCMF file to send", false, false);
        this.UPLINK_SCMF_PARAM.setParser(new ScmfOptionParser());

        this.UPLINK_RELEASE_PARAM = new StringOption(UPLINK_RELEASE_PARAM_SHORT, UPLINK_RELEASE_PARAM_LONG, "release",
                "Specify the Dictionary ID", false);
        this.UPLINK_RELEASE_PARAM.setParser(new ReleaseOptionParser());
    }

    /**
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllUplinkOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();
        result.add(UPLINK_BINARY_FILE_PARAM);
        result.add(UPLINK_ONLY_WRITE_SCMF_PARAM);
        result.add(UPLINK_BIT_RATE_PARAM);
        result.add(UPLINK_SCMF_PARAM);
        result.add(UPLINK_STRINGID_PARAM);
        result.add(UPLINK_RELEASE_PARAM);
        return result;
    }

    /**
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getBasicUplinkOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();
        result.add(UPLINK_BINARY_FILE_PARAM);
        result.add(UPLINK_BIT_RATE_PARAM);
        return result;
    }

    /**
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getNonBasicUplinkOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();
        result.add(UPLINK_ONLY_WRITE_SCMF_PARAM);
        result.add(UPLINK_SCMF_PARAM);
        result.add(UPLINK_STRINGID_PARAM);
        result.add(UPLINK_RELEASE_PARAM);
        return result;
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
    public void parseAllOptionsAsOptionalWithDefault(final ICommandLine commandLine) throws ParseException {
        UPLINK_BINARY_FILE_PARAM.parseWithDefault(commandLine, false, true);
        UPLINK_ONLY_WRITE_SCMF_PARAM.parseWithDefault(commandLine, false, true);
        UPLINK_BIT_RATE_PARAM.parseWithDefault(commandLine, false, true);
        UPLINK_SCMF_PARAM.parseWithDefault(commandLine, false, true);
        UPLINK_STRINGID_PARAM.parseWithDefault(commandLine, false, true);
        UPLINK_RELEASE_PARAM.parseWithDefault(commandLine, false, true);
    }

    /**
     * Parses the all the basic options defined by this class from the supplied
     * command line object. Requires none of the options, and sets no defaults.
     * Result values are set into the DictionaryConfiguration member.
     * 
     * @param commandLine
     *            the parsed command line options
     * @throws ParseException
     *             if there is a parse error
     */
    public void parseAllBasicOptionsAsOptionalWithDefault(final ICommandLine commandLine) throws ParseException {
        UPLINK_BINARY_FILE_PARAM.parseWithDefault(commandLine, false, true);
        UPLINK_BIT_RATE_PARAM.parseWithDefault(commandLine, false, true);
    }

    /**
     * Parses the all the basic options defined by this class from the supplied
     * command line object. Requires none of the options, and sets no defaults.
     * Result values are set into the DictionaryConfiguration member.
     * 
     * @param commandLine
     *            the parsed command line options
     * @throws ParseException
     *             if there is a parse error
     */
    public void parseAllNonBasicOptionsAsOptionalWithDefault(final ICommandLine commandLine) throws ParseException {
        UPLINK_ONLY_WRITE_SCMF_PARAM.parseWithDefault(commandLine, false, true);
        UPLINK_STRINGID_PARAM.parseWithDefault(commandLine, false, true);
        UPLINK_RELEASE_PARAM.parseWithDefault(commandLine, false, true);
        UPLINK_SCMF_PARAM.parseWithDefault(commandLine, false, true);
    }


    /**
     * Class OnlyWriteScmfOptionParser
     *
     */
    protected class OnlyWriteScmfOptionParser extends FlagOptionParser {
        /**
         * No-arg Constructor
         */
        public OnlyWriteScmfOptionParser() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean parse(final ICommandLine commandLine, final ICommandLineOption<Boolean> opt)
                throws ParseException {
            final boolean onlyWriteScmf = super.parse(commandLine, opt);
            scmfProperties.setOnlyWriteScmf(onlyWriteScmf);
            return onlyWriteScmf;
        }
    }

    /**
     * Parse the string ID for the uplink (what RCE is this uplink targetting)
     */
    protected class StringIdOptionParser extends DynamicEnumOptionParser<ExecutionStringType> {
        /**
         * No-arg Constructor
         */
        public StringIdOptionParser() {
            super(ExecutionStringType.class, new LinkedList<>(Arrays.asList(ExecutionStringType.values())));
            setConvertToUpperCase(true);
            setAllowUnknown(false);
        }

        /**
         * Parse the string ID for the uplink (what RCE is this uplink
         * targetting)
         *
         * @param commandLine
         *            The Apache CLI command line interface
         *
         * @throws ParseException
         *             If an invalid string ID is specified
         */
        @Override
        public ExecutionStringType parse(final ICommandLine commandLine, final ICommandLineOption<ExecutionStringType> opt)
                throws ParseException {
            ExecutionStringType stringId = super.parse(commandLine, opt);

            if (stringId != null) {
                ExecutionStringType et = null;
                try {
                    et = ExecutionStringType.valueOf(stringId.toString());
                } catch (final IllegalArgumentException iae) {
                    throw new ParseException(
                            "The input execution string ID value '" + stringId + "' is not a valid execution string. "
                                    + Arrays.toString(ExecutionStringType.values()));
                }
                cmdFrameProperties.setStringId(et.toString());
                return et;
            }
            return null;

        }
    }

    /**
     * Class BinaryUplinkFileParser
     */
    protected class BinaryUplinkFileParser extends FileOptionParser {
        /**
         * 
         */
        public BinaryUplinkFileParser() {
            super(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String parse(final ICommandLine commandLine, final ICommandLineOption<String> opt)
                throws ParseException {
            final String uplinkFileName = super.parse(commandLine, opt);
            if (uplinkFileName != null) {
                cmdProperties.setBinaryOutputFile(uplinkFileName.trim());
                scmfProperties.setWriteScmf(false);
            }
            return uplinkFileName;
        }
    }

    /**
     * 
     * Class UplinkBitRateParser
     */
    protected class UplinkBitRateParser extends CsvStringOptionParser {
        /**
         * @param valid
         *            a list of valid bit rates
         */
        public UplinkBitRateParser(final List<String> valid) {
            super(true, true, valid);
        }

        /**
         * MPCS-9426 - 02/01/18 - Added overriden method
         * 
         * @see jpl.gds.shared.cli.options.AbstractOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public Collection<String> parse(final ICommandLine commandLine,
                                        final ICommandLineOption<Collection<String>> opt)
                throws ParseException {
            final Collection<String> selectedBitRateStrings = super.parse(commandLine, opt);
            // parent class can return empty collection but not null
            if (selectedBitRateStrings.isEmpty()) {
                return selectedBitRateStrings;
            }

            for (final String val : selectedBitRateStrings) {
                try {
                    Double.parseDouble(val);
                }
                catch (final NumberFormatException nfe) {
                    throw new ParseException("Invalid bit rate specified: " + val);
                }
            }
            try {
                cmdProperties.setUplinkRates(selectedBitRateStrings.toArray(new String[selectedBitRateStrings.size()]));
            }
            catch (final UplinkParseException e) {
                throw new ParseException(e.getLocalizedMessage());
            }
            return selectedBitRateStrings;
        }

        /**
         * MPCS-9426- 02/01/18 - Added overriden method
         * 
         * @see jpl.gds.shared.cli.options.AbstractOptionParser#parseWithDefault(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption, boolean, boolean)
         */
        @Override
        public Collection<String> parseWithDefault(final ICommandLine commandLine,
                                                   final ICommandLineOption<Collection<String>> opt,
                                                   final boolean required, final boolean doSetDefault)
                throws ParseException {
            final Collection<String> selectedBitRateStrings = super.parseWithDefault(commandLine, opt, required, doSetDefault);
            // parent class can return empty collection but not null
            if (selectedBitRateStrings.isEmpty()) {
                return selectedBitRateStrings;
            }

            for (final String val : selectedBitRateStrings) {
                try {
                    Double.parseDouble(val);
                } catch (final NumberFormatException nfe) {
                    throw new ParseException("Invalid bit rate specified: " + val);
                }
            }
            try {
                cmdProperties.setUplinkRates(selectedBitRateStrings.toArray(new String[selectedBitRateStrings.size()]));
            } catch (final UplinkParseException e) {
                throw new ParseException(e.getLocalizedMessage());
            }
            return selectedBitRateStrings;
        }

    }

    /**
     * Parse the SCMF name option
     */
    protected class ScmfOptionParser extends FileOptionParser {
        /**
         * No-arg constructor
         */
        public ScmfOptionParser() {
            super(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String parse(final ICommandLine commandLine, final ICommandLineOption<String> opt)
                throws ParseException {
            String scmf = super.parse(commandLine, opt);
            if (scmf != null) {
                scmf = scmf.trim();
                scmfProperties.setScmfName(scmf);
            }
            // throw new ParseException("The argument --" +
            // UPLINK_SCMF_PARAM.getLongOpt() + " requires a value.");

            return scmf;
        }
    }

    /**
     * Class ReleaseOptionParser
     */
    protected class ReleaseOptionParser extends StringOptionParser {
        /**
         * No-arg Constructor
         */
        public ReleaseOptionParser() {
            super();
        }

        /**
         * Parse the string ID for the uplink (what RCE is this uplink
         * targetting)
         *
         * @param commandLine
         *            The Apache CLI command line interface
         *
         * @throws ParseException
         *             If an invalid string ID is specified
         */
        @Override
        public String parse(final ICommandLine commandLine, final ICommandLineOption<String> opt)
                throws ParseException {
            final String release = super.parse(commandLine, opt);

            if (release != null) {
                ICommandDefinitionProvider dictionary = null;
                try {
                    dictionary = appContext.getBean(ICommandDefinitionProvider.class);
                } catch (final Exception e) {
                    throw new ParseException("Error retrieving command dictionary: " + e.getMessage());
                }

                /*
                 * MPCS-7434 - 2/3/16. Technically only MSL-formatted
                 * dictionaries are required to have a build or release version
                 * ID. I have modified the CommandDefinitionTable to return the
                 * GDS version ID for build and release IDs if they are null.
                 * This used to be the other way around, basically. The null
                 * check here on build version is no longer required.
                 */
                if (dictionary != null) {
                    System.out.println(
                            "Command dictionary FSW version ID = " + dictionary.getBuildVersionId() + ", dictionary = "
                                    + dictionary.getGdsVersionId() + ", release = " + dictionary.getReleaseVersionId());
                    System.exit(0);
                }
                throw new ParseException("Could not retrieve version number from command dictionary.");
            }
            return release;
        }
    }
}
