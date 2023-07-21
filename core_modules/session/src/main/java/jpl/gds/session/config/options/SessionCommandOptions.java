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
package jpl.gds.session.config.options;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.cli.ParseException;

import jpl.gds.common.config.ConfigurationConstants;
import jpl.gds.common.config.connection.HostNameValidator;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.options.ConnectionCommandOptions;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.mission.StationMapper;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.common.options.DownlinkStreamTypeOption;
import jpl.gds.common.options.DssIdOption;
import jpl.gds.common.options.SpacecraftIdOption;
import jpl.gds.common.options.SubtopicOption;
import jpl.gds.common.options.TestbedNameOption;
import jpl.gds.common.options.VcidOption;
import jpl.gds.common.options.VenueTypeOption;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.session.config.SessionConfigurationValidator;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.AbstractListCheckingOptionParser;
import jpl.gds.shared.cli.options.EnumOption;
import jpl.gds.shared.cli.options.EnumOptionParser;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.FlagOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.StringOptionParser;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOptionParser;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.filesystem.FileOptionParser;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOption;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOptionParser;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.shared.xml.validation.XmlValidationException;

/**
 * This class creates command line option objects used for parsing session
 * options and automatically setting the parsed values into a
 * SessionConfiguration object. It can utilize the global SessionConfiguration,
 * SessionProperties, and MissionProperties objects, or it can be supplied with
 * unique instances in the constructor. Once an instance of this class is
 * constructed, it provides public members for each defined option, which can be
 * individually added to a class that extends BaseCommandOptions and can be
 * individually parsed by an application. Alternatively, there are convenience
 * methods to get or parse collections of options.
 * 
 *
 */
public class SessionCommandOptions implements ICommandLineOptionsGroup {

    private static final String SCHEMA_LOCATION = "/schema/SessionConfigFile.rnc";
    private static final UnsignedLong MIN_SESSION_KEY = UnsignedLong.valueOf(1);
    private static final UnsignedLong MAX_SESSION_KEY = UnsignedLong.valueOf(
            16777215);
    
    /**
     * Long option name for the session name option.
     */
    public static final String SESSION_NAME_LONG = "sessionName";
    /**
     * Session name description.
     */
    public static final String SESSION_NAME_DESC = "the name of the session";
    /**
     * Alias long option name for the session name option
     */
    public static final String SESSION_TESTNAME_ALIAS      = "testName";

    /**
     * Long option name for the session type option.
     */
    public static final String SESSION_TYPE_LONG = "sessionType";
    /**
     * Session type description.
     */
    public static final String SESSION_TYPE_DESC = "the type of the session";
    /**
     * Long option name for the session description option.
     */
    public static final String SESSION_DESC_LONG = "sessionDescription";
    /**
     * Session description.
     */
    public static final String SESSION_DESC_DESC = "a description of the session";
    //
    /**
     * Long option name for the session DSS ID option.
     */
    public static final String SESSION_DSSID_LONG = "sessionDssId";
    /**
     * Session DSSID description
     */
    public static final String SESSION_DSSID_DESC = "station identifier for session downlink";
    /**
     * Long option name for the session VCID option.
     */
    public static final String SESSION_VCID_LONG = "sessionVcid";
    /**
     * Session vcid description
     */
    public static final String SESSION_VCID_DESC = "input virtual channel ID for session downlink";
    /**
     * Long option name for the session configuration file option.
     */
    public static final String SESSION_CONFIG_LONG = "sessionConfig";
    /**
     * Long option name for the session host option.
     */
    public static final String SESSION_HOST_LONG = "sessionHost";
    /**
     * Session host description.
     */
    public static final String SESSION_HOST_DESC = "the name of the host computing system executing the session";
    /**
     * Long option name for the session user option.
     */
    public static final String SESSION_USER_LONG = "sessionUser";
    /**
     * Session user description.
     */
    public static final String SESSION_USER_DESC = "the name of the user/entity executing the session";
    /**
     * Long option name for the session key option.
     */
    public static final String SESSION_KEY_LONG = "sessionKey";
    /**
     * Session key description.
     */
    public static final String SESSION_KEY_DESC = "the unique numeric identifier for a session";
    /**
     * Long option name for the suppress FSW downlink option.
     */
    public static final String SUPPRESS_FSW_DOWN_LONG = "suppressFswDown";
    /**
     * Long option name for the suppress SSE downlink option.
     */
    public static final String SUPPRESS_SSE_DOWN_LONG = "suppressSseDown";
    /**
     * Long option name for the output directory option.
     */
    public static final String OUTPUT_DIRECTORY_LONG = "outputDir";
    /**
     * Output directory description.
     */
    public static final String OUTPUT_DIRECTORY_DESC = "directory for saving session output files";

    private final SessionConfiguration sessionConfig;
    private final MissionProperties missionProps;
    
    private boolean preventOverrides = true;
    
    private boolean setContextSensitiveDefaults = false;

    /**
     * The VENUE_TYPE command option. Allowed values are restricted to those
     * allowed for the current mission. A default value is supplied. The parsed
     * value is assigned to the "venue type" property in the
     * SessionConfiguration member instance.
     */
    public final EnumOption<VenueType> VENUE_TYPE;

    /**
     * The DOWNLINK_STREAM_TYPE command option for non-monitoring applications.
     * For this option, the Not_Applicable stream type is not accepted. Allowed
     * values are restricted to those allowed for the current mission and the
     * currently established venue. This means that the venue type must either
     * be set in the SessionConfiguration already, or the VENUE_TYPE option MUST
     * be parsed first. The parsed value is assigned to the "downlink stream ID"
     * property in the SessionConfiguration member instance.
     */
    public final EnumOption<DownlinkStreamType> DOWNLINK_STREAM_TYPE;

    /**
     * The MONITOR_DOWNLINK_STREAM_TYPE command option for monitoring
     * applications. For this option, the Not_Applicable stream type is
     * accepted. Allowed values are restricted to those allowed for the current
     * mission and the currently established venue. This means that the venue
     * type must either be set in the SessionConfiguration already, or the
     * VENUE_TYPE option MUST be parsed first. The parsed value is assigned to
     * the "downlink stream ID" property in the SessionConfiguration member
     * instance.
     */
    public final EnumOption<DownlinkStreamType> MONITOR_DOWNLINK_STREAM_TYPE;

    /**
     * The OUTPUT_DIRECTORY command option. The directory is NOT validated and
     * will not be created if it does not exist, to replicate the existing
     * behavior in ReservedOptions. The parsed value is assigned to the
     * "output directory" property in the SessionConfiguration member object.
     */
    public final DirectoryOption OUTPUT_DIRECTORY;

    /**
     * The SPACECRAFT_ID command option. The value must be in the list of
     * configured spacecraft for the current mission. A default value is
     * supplied. The parsed value is assigned to the "spacecraft ID" property in
     * the SessionConfiguration member object.
     */
    public final UnsignedIntOption SPACECRAFT_ID;

    /**
     * The SESSION_CONFIGURATION command option. The supplied filename will be
     * verified to ensure it refers to an existing file. The file will be
     * validated against the session schema. If accepted, the contents of the
     * file will be parsed and the contents of the SessionConfiguration member
     * object will be replaced by the contents of the file, for all fields in
     * the file. While the current primary option name is "testConfig", the
     * alias "sessionConfig" is accepted if an aliasing command line parser is
     * enabled.
     */
    public final FileOption SESSION_CONFIGURATION;

    /**
     * The SESSION_DESCRIPTION command option. The value will be scanned for
     * invalid characters. The parsed value will be set into the
     * SessionConfiguration member object as the "session/test description"
     * property. While the current primary option name is "testDescription", the
     * alias "sessionDescription" is accepted if an aliasing command line parser
     * is enabled.
     */
    public final StringOption SESSION_DESCRIPTION;

    /**
     * The SESSION_HOST command option. The parsed value will be set into the
     * SessionConfiguration member object as the "session/test host" property.
     * While the current primary option name is "testHost", the alias
     * "sessionHost" is accepted if an aliasing command line parser is enabled.
     */
    public final StringOption SESSION_HOST;

    /**
     * The SESSION_USER command option. The parsed value will be set into the
     * SessionConfiguration member object as the "session/test user" property.
     * While the current primary option name is "testUser", the alias
     * "sessionUser" is accepted if an aliasing command line parser is enabled.
     */
    public final StringOption SESSION_USER;

    /**
     * The SESSION_KEY command option. The parsed value will be checked for
     * range and set into the SessionConfiguration member object as the
     * "session/test number" property. It is important to note that this option
     * WILL NOT query the database and populate the entire SessionConfiguration
     * from it, unlike the former option in the ReservedOptions class. While the
     * current primary option name is "testKey", the alias "sessionKey" is
     * accepted if an aliasing command line parser is enabled.
     */
    public final UnsignedLongOption SESSION_KEY;

    /**
     * The SESSION_SUBTOPIC command option. Allowed values are restricted to
     * those allowed for the current mission and the current venue type must be
     * an OPS venue.This means that the venue type must either be set in the
     * SessionConfiguration already, or the VENUE_TYPE option MUST be parsed
     * first. A default value is supplied. The parsed value will be set into the
     * SessionConfiguration member object as the "session/test subtopic"
     * property.
     */
    public final StringOption SESSION_SUBTOPIC;

    /**
     * The SESSION_NAME command option. The value will be scanned for invalid
     * characters. The parsed value will be set into the SessionConfiguration
     * member object as the "session/test name" property. While the current
     * primary option name is "testName", the alias "sessionName" is accepted if
     * an aliasing command line parser is enabled.
     */
    public final StringOption SESSION_NAME;

    /**
     * The SESSION_TYPE command option. The value will be scanned for invalid
     * characters. The parsed value will be set into the SessionConfiguration
     * member object as the "session/test type" property. While the current
     * primary option name is "testType", the alias "sessionType" is accepted if
     * an aliasing command line parser is enabled.
     */
    public final StringOption SESSION_TYPE;

    /**
     * The TESTBED_NAME command option. Allowed values are restricted to those
     * allowed for the current mission and the currently established venue. This
     * means that the venue type must either be set in the SessionConfiguration
     * already, or the VENUE_TYPE option MUST be parsed first. The parsed value
     * is assigned to the "testbed name" property in the SessionConfiguration
     * member instance.
     */
    public final StringOption TESTBED_NAME;

    /**
     * The SESSION_VCID command option. The value must be among the configured
     * downlink virtual channels for the current mission. The parsed value will
     * be set into the SessionConfiguration member instance as the
     * "session vcid" property.
     */
    public final UnsignedIntOption SESSION_VCID;

    /**
     * The SESSION_DSSID command option. The value must be among the configured
     * station IDs for the current mission. The parsed value will be set into
     * the SessionConfiguration member instance as the "session dssid" property.
     */
    public final UnsignedIntOption SESSION_DSSID;
    
    /**
     * The SUPPRESS_FSW_DOWN option. The parsed value will set the
     * "runFsw" property in the SessionConfiguration member instance.
     */
    public final FlagOption SUPPRESS_FSW_DOWN;
    
    /**
     * The SUPPRESS_SSE_DOWN option. The parsed value will set the
     * "runSse" property in the SessionConfiguration member instance.
     */
    public final FlagOption SUPPRESS_SSE_DOWN;

    /**
     * Constructor that takes unique instances of SessionConfiguration,
     * SessionProperties, MissionProperties, and StationMapper,to be used for
     * determination of defaults (in the case of the properties objects) or to
     * set parsed values into (in the case of SessionConfiguration).
     * 
     * @param sc
     *            unique instance of SessionConfiguration
     */
    public SessionCommandOptions(final SessionConfiguration sc) {
        if (sc == null) {
            throw new IllegalArgumentException("Invalid null session argument");
        }
        this.sessionConfig = sc;
        this.missionProps = sc.getMissionProperties();

        VENUE_TYPE = new VenueTypeOption(new LinkedList<VenueType>(missionProps.getAllowedVenueTypes()), false);

        VENUE_TYPE.setParser(new SessionVenueTypeOptionParser());
        final List<DownlinkStreamType> atloTypes = missionProps.getAllowedDownlinkStreamIds(VenueType.ATLO);
        final List<DownlinkStreamType> tbTypes = missionProps.getAllowedDownlinkStreamIds(VenueType.TESTBED);
        final SortedSet<DownlinkStreamType> typeSet = new TreeSet<>(atloTypes);
        typeSet.addAll(tbTypes);
        typeSet.remove(DownlinkStreamType.NOT_APPLICABLE);
        
        DOWNLINK_STREAM_TYPE = new DownlinkStreamTypeOption(new LinkedList<DownlinkStreamType>(typeSet), false);
        
        DOWNLINK_STREAM_TYPE
                .setParser(new SessionDownlinkStreamTypeOptionParser(false));

        MONITOR_DOWNLINK_STREAM_TYPE = new DownlinkStreamTypeOption(false);

        MONITOR_DOWNLINK_STREAM_TYPE
                .setParser(new SessionDownlinkStreamTypeOptionParser(true));

        OUTPUT_DIRECTORY = new DirectoryOption("R", OUTPUT_DIRECTORY_LONG, "directory",
                OUTPUT_DIRECTORY_DESC, false, false,
                false);
        OUTPUT_DIRECTORY.setParser(new OutputDirectoryOptionParser());

        SPACECRAFT_ID = new SpacecraftIdOption(missionProps, false);
        SPACECRAFT_ID.setParser(new SessionSpacecraftIdOptionParser());

        SESSION_CONFIGURATION = new FileOption("N", SESSION_CONFIG_LONG, "filename",
                "the session configuration to be executed", false, true);

        SESSION_CONFIGURATION.setParser(new SessionConfigurationOptionParser());
        SESSION_CONFIGURATION.addAlias("testConfig");

        SESSION_DESCRIPTION = new StringOption("L", SESSION_DESC_LONG,
                "description", SESSION_DESC_DESC, false);
        SESSION_DESCRIPTION.setParser(new SessionDescriptionOptionParser());

        SESSION_DESCRIPTION.addAlias("testDescription");

        SESSION_HOST = new StringOption("O", SESSION_HOST_LONG, "hostname",
                SESSION_HOST_DESC,
                false);
        SESSION_HOST.setParser(new SessionHostOptionParser());
        SESSION_HOST.addAlias("testHost");

        SESSION_USER = new StringOption("P", SESSION_USER_LONG, "username",
                SESSION_USER_DESC, false);

        SESSION_USER.setParser(new SessionUserOptionParser());
        SESSION_USER.addAlias("testUser");

        SESSION_KEY = new UnsignedLongOption("K", SESSION_KEY_LONG, "sessionId",
                SESSION_KEY_DESC, false);

        SESSION_KEY.setParser(new SessionKeyOptionParser());
        SESSION_KEY.addAlias("testKey");

        SESSION_SUBTOPIC = new SubtopicOption(false);
        
        SESSION_SUBTOPIC.setParser(new SessionSubtopicOptionParser());

        SESSION_NAME = new StringOption("M", SESSION_NAME_LONG, "name",
                SESSION_NAME_DESC, false);

        SESSION_NAME.setParser(new SessionNameOptionParser());
        SESSION_NAME.addAlias(SESSION_TESTNAME_ALIAS);

        SESSION_TYPE = new StringOption("Q", SESSION_TYPE_LONG, "type",
                SESSION_TYPE_DESC, false);

        SESSION_TYPE.setParser(new SessionTypeOptionParser());
        SESSION_TYPE.addAlias("testType");

        TESTBED_NAME = new TestbedNameOption(false);
        
        TESTBED_NAME.setParser(new SessionTestbedNameOptionParser());

        SESSION_VCID = new VcidOption(missionProps, false);
        SESSION_VCID.setLongOpt(SESSION_VCID_LONG);
        SESSION_VCID.setDescription(SESSION_VCID_DESC);

        SESSION_VCID.setParser(new SessionVcidOptionParser());

        SESSION_DSSID = new DssIdOption(missionProps, false);
        SESSION_DSSID.setLongOpt(SESSION_DSSID_LONG);
        SESSION_DSSID.setDescription(SESSION_DSSID_DESC);
        
        SESSION_DSSID.setParser(new SessionDssIdOptionParser());
        
        SUPPRESS_FSW_DOWN = new FlagOption("r", SUPPRESS_FSW_DOWN_LONG,
                "suppress FSW downlink application", false);
        SUPPRESS_FSW_DOWN.setParser(new SuppressFswDownOptionParser());
        
        SUPPRESS_SSE_DOWN = new FlagOption("e",SUPPRESS_SSE_DOWN_LONG,
                "suppress SSE downlink application", false);
        SUPPRESS_SSE_DOWN.setParser(new SuppressSseDownOptionParser());


    }

    /**
     * Gets the SessionConfiguration member instance.
     * 
     * @return SessionConfiguration; never null
     */
    public SessionConfiguration getSessionConfiguration() {
        return sessionConfig;
    }

    /**
     * Gets the MissionProperties member instance.
     * 
     * @return MissionProperties; never null
     */
    public MissionProperties getMissionProperties() {
        return missionProps;
    }
    
    /**
     * Sets the flag indicating whether to prevent entry of session-related command
     * line options that override session configuration file content.
     * 
     * @param checkOverride true to prevent overrides, false to not
     */
    public void setPreventOverrides(final boolean checkOverride) {
        preventOverrides = checkOverride;
    }
    
    /**
     * Sets the flag indicating whether to set context-sensitive defaults in the
     * session configuration while parsing.
     * 
     * @param setDefaults true to set defaults, false to not
     */
    public void setUseContextSensitiveDefaults(final boolean setDefaults) {
        setContextSensitiveDefaults = setDefaults;
    }

    /**
     * Gets the Collection of options defined by this class that are used by the
     * FSW downlink processor.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllFswDownlinkOptions() {
        /* Use List rather than Set because parsing order of the options matters */
        final List<ICommandLineOption<?>> result = new LinkedList<ICommandLineOption<?>>();

        result.add(VENUE_TYPE);
        result.add(DOWNLINK_STREAM_TYPE);
        result.add(OUTPUT_DIRECTORY);
        result.add(SESSION_DESCRIPTION);
        result.add(SESSION_DSSID);
        result.add(SESSION_HOST);
        result.add(SESSION_NAME);
        result.add(SESSION_SUBTOPIC);
        result.add(SESSION_TYPE);
        result.add(SESSION_USER);
        result.add(SESSION_VCID);
        result.add(SPACECRAFT_ID);
        result.add(TESTBED_NAME);
        result.add(SESSION_CONFIGURATION);

        return result;
    }
    
    /**
     * Gets the Collection of options defined by this class that are used by the
     * SSE downlink processor.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllSseDownlinkOptions() {
        /* Use List rather than Set because parsing order of the options matters */
        final List<ICommandLineOption<?>> result = new LinkedList<ICommandLineOption<?>>();

        result.add(VENUE_TYPE);
        result.add(OUTPUT_DIRECTORY);
        result.add(SESSION_DESCRIPTION);
        result.add(SESSION_HOST);
        result.add(SESSION_NAME);
        result.add(SESSION_SUBTOPIC);
        result.add(SESSION_TYPE);
        result.add(SESSION_USER);
        result.add(SPACECRAFT_ID);
        result.add(TESTBED_NAME);
        result.add(SESSION_CONFIGURATION);

        return result;
    }


    /**
     * Gets the Collection of options defined by this class that are used by the
     * uplink processor.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllUplinkOptions() {
        /* Use List rather than Set because parsing order of the options matters */
        final LinkedList<ICommandLineOption<?>> result = new LinkedList<ICommandLineOption<?>>();

        result.add(VENUE_TYPE);
        result.add(OUTPUT_DIRECTORY);
        result.add(SESSION_DESCRIPTION);
        result.add(SESSION_DSSID);
        result.add(SESSION_KEY);
        result.add(SESSION_HOST);
        result.add(SESSION_NAME);
        result.add(SESSION_SUBTOPIC);
        result.add(SESSION_TYPE);
        result.add(SESSION_USER);
        result.add(SPACECRAFT_ID);
        result.add(TESTBED_NAME);
        result.add(SESSION_CONFIGURATION);

        return result;
    }

    /**
     * Gets the Collection of options defined by this class that are used by the
     * uplink processor.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllUplinkOptionsNoDssId() {
        /*
         * Use List rather than Set because parsing order of the options matters
         */
        final LinkedList<ICommandLineOption<?>> result = new LinkedList<ICommandLineOption<?>>();

        result.add(VENUE_TYPE);
        result.add(OUTPUT_DIRECTORY);
        result.add(SESSION_DESCRIPTION);
        result.add(SESSION_KEY);
        result.add(SESSION_HOST);
        result.add(SESSION_NAME);
        result.add(SESSION_SUBTOPIC);
        result.add(SESSION_TYPE);
        result.add(SESSION_USER);
        result.add(SPACECRAFT_ID);
        result.add(TESTBED_NAME);
        result.add(SESSION_CONFIGURATION);

        return result;
    }

    /**
     * Gets the Collection of options defined by this class that are used by
     * both the downlink and uplink processors.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllUplinkAndDownlinkOptions() {
        /* Use List rather than Set because parsing order of the options matters */
        final List<ICommandLineOption<?>> result = new LinkedList<ICommandLineOption<?>>();

        /* Need all the uplink.downlink options,
         * but SESSION_CONFIGURATION must be the LAST one, so any values in 
         * the config file override anything else set before that is parsed.
         */
        for (final ICommandLineOption<?> opt : getAllFswDownlinkOptions()) {
            if (!result.contains(opt) && opt != SESSION_CONFIGURATION) {
                result.add(opt);
            }
        }
        for (final ICommandLineOption<?> opt : getAllUplinkOptions()) {
            if (!result.contains(opt) && opt != SESSION_KEY &&  opt != SESSION_CONFIGURATION) {
                result.add(opt);
            }
        }
        result.add(SESSION_CONFIGURATION);

        return result;
    }

    /**
     * Gets the Collection of options defined by this class that are used by
     * monitor applications, including watchers.
     * 
     * @param includeTestKey true to include the session key option, false to not
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllMonitorOptions(final boolean includeTestKey) {
        final List<ICommandLineOption<?>> result = new LinkedList<ICommandLineOption<?>>();

        result.add(VENUE_TYPE);
        result.add(MONITOR_DOWNLINK_STREAM_TYPE);
        result.add(SESSION_DSSID);
        result.add(SESSION_HOST);
        result.add(SESSION_SUBTOPIC);
        result.add(SESSION_USER);
        result.add(SESSION_VCID);
        result.add(SPACECRAFT_ID);
        result.add(TESTBED_NAME);
        if (includeTestKey) {
            result.add(SESSION_KEY);
        }
        result.add(SESSION_CONFIGURATION);

        return result;
    }

    /**
     * Gets the collection of command line options defined by this class that
     * cannot be used to override the contents of a session configuration file.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getNonOverridableOptions() {
        /* Use List rather than Set because parsing order of the options matters */
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();
        
        result.add(DOWNLINK_STREAM_TYPE);
        result.add(OUTPUT_DIRECTORY);
        result.add(SESSION_DSSID);
        result.add(SESSION_HOST);
        result.add(SESSION_KEY);
        result.add(SESSION_SUBTOPIC);
        result.add(SESSION_USER);
        result.add(SESSION_HOST);
        result.add(SESSION_NAME);
        result.add(SESSION_DESCRIPTION);
        result.add(SESSION_TYPE);
        result.add(SESSION_VCID);
        result.add(SPACECRAFT_ID);
        result.add(TESTBED_NAME);
        result.add(VENUE_TYPE);
        result.add(SUPPRESS_FSW_DOWN);
        result.add(SUPPRESS_SSE_DOWN);

        return result;

    }

    /**
     * Gets the output directory command line option
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getOutputDirOption(){
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();
        result.add(OUTPUT_DIRECTORY);
        return result;
    }

    /**
     * Gets the testbed name command line option
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getTestbedNameOption(){
        final Set<ICommandLineOption<?>> result = new TreeSet<ICommandLineOption<?>>();
        result.add(TESTBED_NAME);
        return result;
    }

    /**
     * Parses all defined uplink and downlink command options from the supplied
     * command line. Sets no defaults and requires none of the options. Options
     * not present on the specified command line are effectively ignored.
     * 
     * @param commandLine
     *            parsed ICommandLine object containing the supplied command
     *            line arguments
     * @param setDefaults indicates whether to set context sensitive defaults in the 
     *        session configuration while parsing
     * @param autoRun true if in "auto run" (no session config GUI) mode
     * @throws ParseException
     *             if a parse error occurs
     *             
     */
    public void parseAllUplinkAndDownlinkOptionsAsOptional(
            final ICommandLine commandLine, final boolean setDefaults, final boolean autoRun) throws ParseException {
        
    	final boolean oldUseDefaults = this.setContextSensitiveDefaults;

    	setUseContextSensitiveDefaults(setDefaults);

    	try {

    		final Collection<ICommandLineOption<?>> allOpts = getAllUplinkAndDownlinkOptions();
    		for (final ICommandLineOption<?> opt : allOpts) {
    			opt.parse(commandLine);
    		}

    	}
    	finally {
    		setUseContextSensitiveDefaults(oldUseDefaults);
    	}
    }
    
    /**
     * Parses all defined uplink command options from the supplied
     * command line. Sets no defaults and requires none of the options. Options
     * not present on the specified command line are effectively ignored.
     * 
     * @param commandLine
     *            parsed ICommandLine object containing the supplied command
     *            line arguments
     * @param setDefaults indicates whether to set context sensitive defaults in the 
     *        session configuration while parsing
     * @param autoRun true if in "auto run" (no session config GUI) mode
     * @throws ParseException
     *             if a parse error occurs
     */
    public void parseAllUplinkOptionsAsOptional(
            final ICommandLine commandLine, final boolean setDefaults, final boolean autoRun) throws ParseException {
        
     	final boolean oldUseDefaults = this.setContextSensitiveDefaults;
     	
        setUseContextSensitiveDefaults(setDefaults);
        
    	try {

    		final Collection<ICommandLineOption<?>> allOpts = getAllUplinkOptions();
    		for (final ICommandLineOption<?> opt : allOpts) {
    			opt.parse(commandLine);
    		}

    	}
    	finally {
    		setUseContextSensitiveDefaults(oldUseDefaults);
    	}
    }
    
    /**
     * Parses all defined uplink command options minus dssId from the supplied
     * command line. Sets no defaults and requires none of the options. Options
     * not present on the specified command line are effectively ignored.
     * 
     * @param commandLine
     *            parsed ICommandLine object containing the supplied command
     *            line arguments
     * @param setDefaults
     *            indicates whether to set context sensitive defaults in the
     *            session configuration while parsing
     * @param autoRun
     *            true if in "auto run" (no session config GUI) mode
     * @throws ParseException
     *             if a parse error occurs
     */
    public void parseAllUplinkOptionsNoDssIdAsOptional(final ICommandLine commandLine, final boolean setDefaults,
            final boolean autoRun) throws ParseException {
        final boolean oldUseDefaults = this.setContextSensitiveDefaults;

        setUseContextSensitiveDefaults(setDefaults);

        try {

            final Collection<ICommandLineOption<?>> allOpts = getAllUplinkOptionsNoDssId();
            for (final ICommandLineOption<?> opt : allOpts) {
                opt.parse(commandLine);
            }

        } finally {
            setUseContextSensitiveDefaults(oldUseDefaults);
        }
    }

    /**
     * Parses all defined downlink command options from the supplied
     * command line. Sets no defaults and requires none of the options. Options
     * not present on the specified command line are effectively ignored.
     * 
     * @param commandLine
     *            parsed ICommandLine object containing the supplied command
     *            line arguments
     * @param setDefaults indicates whether to set context sensitive defaults in the 
     *        session configuration while parsing
     * @param autoRun true if in "auto run" (no session config GUI) mode
     * @throws ParseException
     *             if a parse error occurs
     */
    public void parseAllDownlinkOptionsAsOptional(
            final ICommandLine commandLine, final boolean setDefaults, final boolean autoRun) throws ParseException {
        
    	final boolean oldUseDefaults = this.setContextSensitiveDefaults;
    	
        setUseContextSensitiveDefaults(setDefaults);
        
        try {

    		Collection<ICommandLineOption<?>> allOpts = getAllFswDownlinkOptions();
    		for (final ICommandLineOption<?> opt : allOpts) {
    			opt.parse(commandLine);
    		}
    		
            allOpts = getAllSseDownlinkOptions();
            for (final ICommandLineOption<?> opt : allOpts) {
                opt.parse(commandLine);
            }
    	}
    	finally {
    		setUseContextSensitiveDefaults(oldUseDefaults);
    	}
    }

    /**
     * Parses all monitor command options from the supplied command line. Sets
     * no defaults and requires none of the options. Options not present on the
     * specified command line are effectively ignored.
     * 
     * @param commandLine
     *            the parser ICommandLine object containing the parsed command
     *            line arguments
     * @param includeSessionKey true to include parsing of the session key option          
     * @param setDefaults indicates whether to set context sensitive defaults in the 
     *        session configuration while parsing
     * @param validate true to validate the resulting session configuration
     * @param autoRun true if in "auto run" (no session config GUI) mode
     * 
     * @throws ParseException
     *             if there is a parsing error
     */
    public void parseAllMonitorOptionsAsOptional(final ICommandLine commandLine, final boolean includeSessionKey, 
            final boolean setDefaults, final boolean validate, final boolean autoRun)
            throws ParseException {
        
     	final boolean oldUseDefaults = this.setContextSensitiveDefaults;
    	
        setUseContextSensitiveDefaults(setDefaults);
        
        try {
        	final Collection<ICommandLineOption<?>> allOpts = getAllMonitorOptions(includeSessionKey);
        	for (final ICommandLineOption<?> opt : allOpts) {
        		opt.parse(commandLine);
        	}

        	if (validate) {
        		validateConfig(true, autoRun);
        	}
        } finally {
        		setUseContextSensitiveDefaults(oldUseDefaults);
        	}
    }

    /**
     * Parses output directory command option from the supplied command line. Sets
     * no defaults and requires none of the options. Options not present on the
     * specified command line are effectively ignored.
     *
     * @param commandLine
     *            the parser ICommandLine object containing the parsed command line arguments
     *
     * @throws ParseException
     *             if there is a parsing error
     */
    public void parseOutputDir(final ICommandLine commandLine) throws ParseException{
        OUTPUT_DIRECTORY.parse(commandLine);
    }

    /**
     * Parses testbed name command option from the supplied command line with default
     *
     * @param commandLine
     *            the parser ICommandLine object containing the parsed command line arguments
     *
     * @throws ParseException
     *             if there is a parsing error
     */
    public void parseTestbedNameWithDefault(final ICommandLine commandLine) throws ParseException{
        final boolean oldUseDefaults = this.setContextSensitiveDefaults;
        setUseContextSensitiveDefaults(true);

        TESTBED_NAME.parse(commandLine);

        setUseContextSensitiveDefaults(oldUseDefaults);
    }

    private void validateConfig(final boolean isMonitor, final boolean autoRun) throws ParseException {
        final SessionConfigurationValidator validator = new SessionConfigurationValidator(sessionConfig);
        if (!validator.validate(isMonitor, autoRun)) {
            throw new ParseException("The session configuration contains the following errors:\n" +
              validator.getErrorsAsMultilineString());
        }
    }
    
    private void checkCommandLineOverride(final ICommandLine commandLine)
            throws ParseException {
        if (!commandLine.hasOption(SESSION_CONFIGURATION.getLongOpt())) {
            return;
        }

        final Collection<ICommandLineOption<?>> optionsToCheck = getNonOverridableOptions();

        /*
         * Add the host command options that cannot be overridden. It is ok to
         * use the constructor that uses the global HostConfiguration, because
         * we are not doing anything with the options other than accessing their
         * long option names.
         */
        optionsToCheck.addAll(new ConnectionCommandOptions(sessionConfig.getConnectionConfiguration())
                .getNonOverridableOptions());

        /*
         * Add the dictionary command options that cannot be overridden. It is
         * ok to use the constructor that uses the global
         * DictionaryConfiguration.
         */
        optionsToCheck.addAll(new DictionaryCommandOptions(this.sessionConfig.getDictionaryConfig())
                .getNonOverridableOptions());

        for (final ICommandLineOption<?> opt : optionsToCheck) {
            if (commandLine.hasOption(opt.getLongOpt())) {
                throw new ParseException("Command line overwrite of --"
                        + SESSION_CONFIGURATION.getLongOpt()
                        + " file by option --" + opt.getLongOpt()
                        + " is not supported. If a session configuration file is specified, its contents may not be overridden");

            }
        }

    }

    /**
     * Option parser class for the SESSION_CONFIGURATION option. The file is
     * validated against the session schema. We check for conflicting command
     * line options. Then the SessionConfiguration member instance is loaded
     * from the contents of the file.
     * 
     *
     */
    protected class SessionConfigurationOptionParser extends FileOptionParser {

        /**
         * Constructor.
         */
        public SessionConfigurationOptionParser() {
            super(true);

        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.filesystem.FileOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String file = super.parse(commandLine, opt);
            if (file != null) {

                if (preventOverrides) {
                    checkCommandLineOverride(commandLine);
                }

                final File configFile = new File(file);

                // validate config file against schema
                final String gdsConfigDir = GdsSystemProperties.getGdsDirectory();
                final File schemaFile = new File(gdsConfigDir + SCHEMA_LOCATION);
                if (!schemaFile.exists()) {
                    throw new ParseException("The schema file "
                            + schemaFile.getAbsolutePath() + " does not exist.");
                }
                try {
                    final boolean pass = IContextConfiguration
                            .schemaVsConfigFileCheck(schemaFile, configFile);
                    if (!pass) {
                        throw new ParseException("The session configuration "
                                + "file does not match schema definition");
                    }
                } catch (final XmlValidationException ve) {
                    throw new ParseException("XML parsing error reading the session configuration file: "
                            + ve.getMessage());
                }

                sessionConfig.load(file);

                if (!GdsSystemProperties.isIntegratedGui()) {
                    sessionConfig.getContextId().setNumber(null);
                    sessionConfig.getContextId().setStartTime(new AccurateDateTime());
                }
            }

            return file;
        }

    }

    /**
     * Option parser class for the VENUE_TYPE command option. Will validate the
     * value against the list of allowed venues for the current mission. A
     * default value is established. The parsed value will be set in the
     * SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionVenueTypeOptionParser extends EnumOptionParser<VenueType> {

        /**
         * Constructor.
         */
        public SessionVenueTypeOptionParser() {
            super(VenueType.class, new LinkedList<VenueType>(
                    missionProps.getAllowedVenueTypes()));
            setDefaultValue(missionProps.getDefaultVenueType());
            setConvertToUpperCase(true);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.EnumOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public VenueType parse(final ICommandLine commandLine,
                final ICommandLineOption<VenueType> opt) throws ParseException {
            final VenueType type = super.parse(commandLine, opt);
            if (type != null) {
                sessionConfig.getVenueConfiguration().setVenueType(type);
                
            } else if (setContextSensitiveDefaults) {
                
                sessionConfig.getVenueConfiguration().setVenueType(missionProps.getDefaultVenueType());
            }
            return type;
        }
    }

    /**
     * Option parser class for the TESTBED_NAME option. Will validate the value
     * is a valid testbed for the current venue. The parsed value will be set
     * into the SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionTestbedNameOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String tb = super.parse(commandLine, opt);

            if (tb != null) {
                final VenueType vt = sessionConfig.getVenueConfiguration().getVenueType();
                if (vt == null) {
                    throw new ParseException(
                            "Venue type on session configuration object is null.");
                }

                if (!vt.hasTestbedName()) {
                    throw new ParseException("The --"
                            + TESTBED_NAME.getLongOpt()
                            + " option is only valid for an venue type of "
                            + VenueType.TESTBED + " or " + VenueType.ATLO);
                }

                final List<String> testbeds = missionProps.getAllowedTestbedNames(vt);
                if (!testbeds.contains(tb)) {
                    final StringBuffer buf = new StringBuffer(1024);
                    buf.append("The testbed name '");
                    buf.append(tb);
                    buf.append("' is not one of the configured testbed names for venue " + vt);
                    buf.append(". Allowable values are: ");
                    for (int j = 0; j < testbeds.size(); j++) {
                        buf.append(testbeds.get(j));
                        if (j != testbeds.size() - 1) {
                            buf.append(", ");
                        }
                    }
                    throw new ParseException(buf.toString());
                }
                sessionConfig.getVenueConfiguration().setTestbedName(tb);
            } else if (setContextSensitiveDefaults) {
                final VenueType vt = sessionConfig.getVenueConfiguration().getVenueType();
                if (vt == null) {
                    throw new ParseException(
                            "Venue type on session configuration object is null.");
                }
                if (vt.hasTestbedName()) {
                    sessionConfig.getVenueConfiguration().setTestbedName(missionProps.getDefaultTestbedName(vt, HostPortUtility.getLocalHostName()));
                }

            }
            return tb;
        }
    }

    /**
     * Option parser class for the TESTBED_NAME option. Will validate the value
     * is a valid subtopic for the current mission and that the current venue is
     * an OPS venue. A default value is established. The parsed value will be
     * set into the SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionSubtopicOptionParser extends StringOptionParser {

        /**
         * Constructor
         */
        public SessionSubtopicOptionParser() {
            super(missionProps.getAllowedSubtopics());
            setDefaultValue(missionProps.getDefaultSubtopic());
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            String topic = super.parse(commandLine, opt);

            if (topic != null) {
                
                topic = topic.toUpperCase().trim();

                if (sessionConfig.getVenueConfiguration().getVenueType() == null) {
                    throw new ParseException(
                            "Venue type on session configuration object is null.");
                }

                if (!sessionConfig.getVenueConfiguration().getVenueType().isOpsVenue()) {
                    throw new ParseException("The --"
                            + SESSION_SUBTOPIC.getLongOpt()
                            + " option is only valid for OPS venues");
                }
                sessionConfig.getGeneralInfo().setSubtopic(topic);
                
            } else if (setContextSensitiveDefaults) {
                if (sessionConfig.getVenueConfiguration().getVenueType() != null && sessionConfig.getVenueConfiguration().getVenueType().isOpsVenue()) {
                    sessionConfig.getGeneralInfo().setSubtopic(missionProps.getDefaultSubtopic());
                }
            }
            return topic;
        }

    }

 

    /**
     * Option parser class for the SESSION_KEY option. Note that this method
     * will not ensure the session is in the database. The parsed value is set
     * into the SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionKeyOptionParser extends UnsignedLongOptionParser {

        /**
         * Constructor
         */
        public SessionKeyOptionParser() {
            super(MIN_SESSION_KEY, MAX_SESSION_KEY);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedLongOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedLong parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedLong> opt) throws ParseException {

            final UnsignedLong key = super.parse(commandLine, opt);

            if (key != null) {
                sessionConfig.getContextId().setNumber(Long.valueOf(key.longValue()));
            }
            return key;
        }

    }

    /**
     * Option parser for the SESSION_HOST option. The parsed value will be set
     * into the SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionHostOptionParser extends StringOptionParser {
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String host = super.parse(commandLine, opt);

            if (host != null) {
            	final HostNameValidator valid = new HostNameValidator();
            	final String error = valid.isValid(host);
            	if (error != null) {
            		throw new ParseException(
                            "The value of the --"
                                    + SESSION_HOST.getLongOpt() + " option is invalid: " + error);
            	}
                sessionConfig.getContextId().setHost(host);
            }

            return host;
        }
    }

    /**
     * Option parser for the SESSION_USER option. The parsed value will be set
     * into the SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionUserOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String user = super.parse(commandLine, opt);
            if (user != null) {
                sessionConfig.getContextId().setUser(user);
            }
            return user;
        }
    }

    /**
     * Option parser for the SESSION_TYPE option. The parsed value will be
     * scanned for invalid characters. The parsed value will be set into the
     * SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionTypeOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String type = super.parse(commandLine, opt);
            if (type != null) {
                for (int i = 0; i < type.length(); i++) {
                    final char c = type.charAt(i);
                    if (c == '&' || c == '<' || c == '>' || c == '%') {
                        throw new ParseException(
                                "The value of the --"
                                        + SESSION_TYPE.getLongOpt()
                                        + " option cannot contain the characters &, %, <, or >.");
                    }
                }
                sessionConfig.getContextId().setType(type);
            }
            return type;
        }
    }

    /**
     * Option parser for the SESSION_NAME option. The parsed value will be
     * scanned for invalid characters. The parsed value will be set into the
     * SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionNameOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String name = super.parse(commandLine, opt);
            if (name != null) {
                for (int i = 0; i < name.length(); i++) {
                    final char c = name.charAt(i);
                    if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                        throw new ParseException(
                                "The value of the --"
                                        + SESSION_NAME.getLongOpt()
                                        + " option can contain only letters, digits, dashes, and underscores.");
                    }
                }
                if (name.trim().length() > ConfigurationConstants.NAME_LENGTH) {
                    throw new ParseException(
                            "The value of the --"
                                    + SESSION_NAME.getLongOpt()
                                    + " option cannot be longer than " + ConfigurationConstants.NAME_LENGTH);
                }
                sessionConfig.getContextId().setName(name.trim());
            }

            return name;
        }
    }

    /**
     * Option parser for the SESSION_DESCRIPTION option. The parsed value will
     * be scanned for invalid characters. The parsed value will be set into the
     * SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionDescriptionOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String desc = super.parse(commandLine, opt);

            if (desc != null) {
                for (int i = 0; i < desc.length(); i++) {
                    final char c = desc.charAt(i);
                    if (c == '&' || c == '<' || c == '>' || c == '%') {
                        throw new ParseException(
                                "The value of the --"
                                        + SESSION_DESCRIPTION.getLongOpt()
                                        + " option cannot contain the characters &, %, <, or >.");
                    }
                }
                sessionConfig.getContextId().setDescription(desc);
            }
            return desc;
        }
    }

    /**
     * A base option parser class for UnsignedInt options that have a list of
     * value values.
     * 
     *
     */
    protected class ListCheckingUnsignedIntOptionParser extends
            AbstractListCheckingOptionParser<UnsignedInteger> {

        /**
         * Constructor.
         */
        public ListCheckingUnsignedIntOptionParser() {
            super();
            setValidate(true);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final String value = getValue(commandLine, opt);
            if (value == null) {
                return null;

            }
            UnsignedInteger val = null;
            try {
                final Integer intValue = Integer.valueOf(value);
                val = UnsignedInteger.valueOf(intValue);
            } catch (final IllegalArgumentException e) {
                throw new ParseException(THE_VALUE + opt.getLongOpt()
                        + " must be an unsigned integer");
            }

            super.checkValueInList(opt, val, true);

            return val;

        }
    }

    /**
     * Option parser class for the SPACECRAFT_ID option. Will validate that the
     * parsed value is among the spacecraft IDs configured for the current
     * mission. A default value is established. The resulting value will put set
     * into the SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionSpacecraftIdOptionParser extends
            ListCheckingUnsignedIntOptionParser {

        /**
         * Constructor.
         */
        public SessionSpacecraftIdOptionParser() {
            super();
            if (missionProps.getDefaultScid() != MissionProperties.UNKNOWN_ID) {
                setDefaultValue(UnsignedInteger.valueOf(missionProps.getDefaultScid()));
            } else {
                setDefaultValue(UnsignedInteger.valueOf(0));
            }
            final List<UnsignedInteger> restrictedVals = new LinkedList<UnsignedInteger>();
            for (final Integer scid : missionProps.getAllScids()) {
                if (scid >= 0) {
                    restrictedVals.add(UnsignedInteger.valueOf(scid));
                }
            }
            setRestrictionList(restrictedVals);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final UnsignedInteger scid = super.parse(commandLine, opt);

            if (scid != null) {
                sessionConfig.getContextId().setSpacecraftId(scid.intValue());
            } else if (setContextSensitiveDefaults) {
                sessionConfig.getContextId().setSpacecraftId(getDefaultValue().intValue());
            }
            return scid;

        }
    }

    /**
     * An option parser class for the SESSION_VCID option. The value is checked
     * to ensure it is on the list of configured downlink VCIDs for the mission.
     * The parsed value is set into the SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionVcidOptionParser extends
            ListCheckingUnsignedIntOptionParser {

        /**
         * Constructor.
         */
        public SessionVcidOptionParser() {
            super();
            final List<UnsignedInteger> restrictedVals = new LinkedList<UnsignedInteger>();
            for (final Integer vcid : missionProps.getAllDownlinkVcids()) {
                restrictedVals.add(UnsignedInteger.valueOf(vcid));
            }
            setRestrictionList(restrictedVals);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final UnsignedInteger vcid = super.parse(commandLine, opt);
            if (vcid != null) {
                sessionConfig.getFilterInformation().setVcid(vcid.intValue());
            }
            return vcid;

        }
    }

    /**
     * An option parser class for the SESSION_DSSID option. The value is checked
     * to ensure it is on the list of configured downlink station IDs for the
     * mission. The parsed value is set into the SessionConfiguration member
     * instance.
     * 
     *
     */
    protected class SessionDssIdOptionParser extends
            ListCheckingUnsignedIntOptionParser {

        /**
         * Constructor.
         */
        public SessionDssIdOptionParser() {
            super();
            final List<UnsignedInteger> restrictedVals = new LinkedList<UnsignedInteger>();
            final StationMapper stationMap = missionProps.getStationMapper();
            for (final Integer id : stationMap.getStationIdsAsSet()) {
                restrictedVals.add(UnsignedInteger.valueOf(id));
            }
            setRestrictionList(restrictedVals);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.AbstractOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final UnsignedInteger dsid = super.parse(commandLine, opt);

            if (dsid != null) {
                sessionConfig.getFilterInformation().setDssId(dsid.intValue());
            }
            return dsid;

        }
    }

    /**
     * An option parser class for the OUTPUT_DIRECTORY option. Will not validate
     * the existence of the directory. The parsed value will be set into the
     * SessionConfiguration member instance.
     * 
     *
     */
    protected class OutputDirectoryOptionParser extends DirectoryOptionParser {

        /**
         * Constructor.
         */
        public OutputDirectoryOptionParser() {
            super(false);
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
                sessionConfig.getGeneralInfo().setOutputDir(dir);
                sessionConfig.getGeneralInfo().setOutputDirOverridden(true);
            }
            return dir;
        }

    }

  

    /**
     * Option parser class for DOWNLINK_STREAM_TYPE. Can be configured to allow
     * or disallow the Not_Applicable stream type. Value must be valid for the
     * current venue and mission. The parsed value is set into the
     * SessionConfiguration member instance.
     * 
     *
     */
    protected class SessionDownlinkStreamTypeOptionParser extends
            EnumOptionParser<DownlinkStreamType> {

        private final boolean allowNaStream;

        /**
         * Constructor.
         * 
         * @param allowNA
         *            true if the Not_Applicable stream type should be allowed
         */
        public SessionDownlinkStreamTypeOptionParser(final boolean allowNA) {
            super(DownlinkStreamType.class);
            this.allowNaStream = allowNA;
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.EnumOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public DownlinkStreamType parse(final ICommandLine commandLine,
                final ICommandLineOption<DownlinkStreamType> opt)
                throws ParseException {

            final String rawVal = getValue(commandLine, opt);
            if (rawVal == null) {
               if (!setContextSensitiveDefaults) {
            	   return null;
               }
            }

            DownlinkStreamType streamId = null;

            if (rawVal != null) {
            	try {
            		streamId = DownlinkStreamType.convert(rawVal);
            	} catch (final IllegalArgumentException e) {
            		throw new ParseException("Invalid value for the --"
            				+ DOWNLINK_STREAM_TYPE.getLongOpt() + " option ("
            				+ rawVal + ")");
            	}
            }

            final VenueType venueType = sessionConfig.getVenueConfiguration().getVenueType();

            if (venueType == null) {
                throw new ParseException(
                        "Venue type on session configuration object is null.");
            }

            /*
             * Remove check that allowed downlink
             * stream ID through for all venues if being called by a monitor
             * client.
             */
            if (!venueType.hasStreams() && streamId != null) {
                throw new ParseException("The --"
                        + DOWNLINK_STREAM_TYPE.getLongOpt()
                        + " option is not valid for a venue type of "
                        + venueType);
            }

            final List<DownlinkStreamType> streams = missionProps
                    .getAllowedDownlinkStreamIds(venueType);

            final IDownlinkConnection dc = sessionConfig.
            		getConnectionConfiguration().getFswDownlinkConnection();
            
            final boolean allowNaStreamForThisInstance = dc != null &&
                    dc.getDownlinkConnectionType() != null
                    && venueType.hasStreams()
                    && !dc.getDownlinkConnectionType().requiresStreamId();

            if (venueType.hasStreams() && (allowNaStream || allowNaStreamForThisInstance)) {
                if (!streams.contains(DownlinkStreamType.NOT_APPLICABLE)) {
                    streams.add(0, DownlinkStreamType.NOT_APPLICABLE);
                }

            }
            
            if (streamId == null && setContextSensitiveDefaults && venueType.hasStreams()) {
            	streamId = streams.get(0);
            }

            if (streamId != null) {
            	if (!streams.contains(streamId)) {
            		final StringBuilder buffer = new StringBuilder(1024);
            		buffer.append("Allowable stream ID values are:");
            		int j = 0;
            		for (final DownlinkStreamType s : streams) {
            			buffer.append(s);
            			if (j != streams.size() - 1) {
            				buffer.append(", ");
            			}
            			j++;
            		}
            		throw new ParseException(
            				"The value of the --"
            						+ DOWNLINK_STREAM_TYPE.getLongOpt()
            						+ " option is not one of the allowable Stream ID values in the configuration.");
            	}
            }

            if (venueType.hasStreams() && dc != null
                    && dc.getDownlinkConnectionType() != null
                    && dc.getDownlinkConnectionType().requiresStreamId()
                    && streamId == null) {
                throw new ParseException(
                        "The downlink stream ID must be supplied when the "
                                + venueType + " venue is specified.");
            }

            sessionConfig.getVenueConfiguration().setDownlinkStreamId(streamId);
            return streamId;

        }

    }
    
    /**
     * Class to parse the "suppress FSW downlink" option.
     *
     */
    protected class SuppressFswDownOptionParser extends FlagOptionParser {
        /**
         * Constructor.
         */
        public SuppressFswDownOptionParser() {
            super();
        }
        
        @Override
        public Boolean parse(final ICommandLine commandLine,
                final ICommandLineOption<Boolean> opt) throws ParseException {
            Boolean flag = super.parse(commandLine, opt);
            if (flag == null) {
                flag = Boolean.valueOf(false);
            }
            sessionConfig.getRunFsw().setFswDownlinkEnabled(!flag);
            return flag;
        }
             
    }
    
    
    /**
     * Class to parse the "suppress SSE downlink" option.
     *
     */
    protected class SuppressSseDownOptionParser extends FlagOptionParser {
        /**
         * Constructor.
         */
        public SuppressSseDownOptionParser() {
            super();
        }
        
        @Override
        public Boolean parse(final ICommandLine commandLine,
                final ICommandLineOption<Boolean> opt) throws ParseException {
            Boolean flag = super.parse(commandLine, opt);
            if (flag == null) {
                flag = Boolean.valueOf(false);
            }
            sessionConfig.getRunSse().setSseDownlinkEnabled(!flag);
            return flag;
        }  
        
    }

    /**
     * Helper method to create command-line args[] for session options
     * 
     * @param sessionHost
     *            session host long option value
     * @param sessionName
     *            session name long option value
     * @param sessionUser
     *            session user long option value
     * @param sessionDesc
     *            session description long option value
     * @param sessionType
     *            session type long option value
     * @param outputDir
     *            session output directory long option value
     * @return an array list of command-line arguments and their arguments - if present
     */
    public static List<String> buildSessionOptionsFromCli(final String sessionHost, final String sessionName,
                                                    final String sessionUser, final String sessionDesc,
                                                    final String sessionType, final String outputDir,
                                                    final Integer sessionDssId, final Integer sessionVcid) {
        final List<String> argList = new ArrayList<>();
        if (sessionHost != null) {
            argList.add(DASHES + SessionCommandOptions.SESSION_HOST_LONG);
            argList.add(sessionHost);
        }
        if (sessionName != null) {
            argList.add(DASHES + SessionCommandOptions.SESSION_NAME_LONG);
            argList.add(sessionName);
        }
        if (sessionUser != null) {
            argList.add(DASHES + SessionCommandOptions.SESSION_USER_LONG);
            argList.add(sessionUser);
        }
        if (sessionDesc != null) {
            argList.add(DASHES + SessionCommandOptions.SESSION_DESC_LONG);
            argList.add(sessionDesc);
        }
        if (sessionType != null) {
            argList.add(DASHES + SessionCommandOptions.SESSION_TYPE_LONG);
            argList.add(sessionType);
        }
        if (outputDir != null) {
            argList.add(DASHES + SessionCommandOptions.OUTPUT_DIRECTORY_LONG);
            argList.add(outputDir);
        }
        if (sessionDssId != null) {
            argList.add(DASHES + SessionCommandOptions.SESSION_DSSID_LONG);
            argList.add(sessionDssId.toString());
        }
        if (sessionVcid != null) {
            argList.add(DASHES + SessionCommandOptions.SESSION_VCID_LONG);
            argList.add(sessionVcid.toString());
        }

        return argList;
    }

   
}
