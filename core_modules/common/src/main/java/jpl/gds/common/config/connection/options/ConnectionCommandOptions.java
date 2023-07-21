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
package jpl.gds.common.config.connection.options;

import jpl.gds.common.config.ConfigurationConstants;
import jpl.gds.common.config.connection.*;
import jpl.gds.common.config.types.DatabaseConnectionKey;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.*;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.cli.options.filesystem.FileOptionParser;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOption;
import jpl.gds.shared.cli.options.numeric.UnsignedLongOptionParser;
import jpl.gds.shared.holders.PortHolder;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.types.UnsignedLong;
import jpl.gds.shared.util.HostPortUtility;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.util.*;

/**
 * This class creates command line option objects used for parsing host and port
 * options and automatically setting the parsed values into an IConnectionMap
 * object. Once an instance of this class is constructed, it provides public
 * members for each defined option, which can be individually added to a class
 * that extends BaseCommandOptions and can be individually parsed by an
 * application. Alternatively, there are convenience methods to get or parse
 * collections of options.
 * <p>
 * <br>
 * It is important to note that currently there are not separate options for
 * downlink connection type, uplink connection type, telemetry input type,
 * downlink input file, or database source host and key. This is owing to the
 * limits of the old session configuration. When these options are parsed, the
 * results are placed into the connection map in EITHER of the FSW or SSE
 * connection objects, based upon the current value of the
 * <p>
 * <br>
 * CAVEATS: The implementation here attempts to duplicate the behavior of the
 * old ReservedOptions class as closely as possible, but this results in
 * inconsistencies in behavior of the options. FSW uplink options have defaults,
 * but the others do not. This is clearly an evolutionary artifact and is
 * disconcerting, but I am wary of changing it.
 * 
 */
public class ConnectionCommandOptions implements ICommandLineOptionsGroup {

    private static final UnsignedInteger MIN_PORT = UnsignedInteger.valueOf(
            PortHolder.MIN_VALUE);

    private static final UnsignedInteger MAX_PORT = UnsignedInteger.valueOf(
            PortHolder.MAX_VALUE);
    
    private static final UnsignedLong MIN_SESSION_KEY = UnsignedLong.valueOf(1);
    private static final UnsignedLong MAX_SESSION_KEY = UnsignedLong.valueOf(
            16777215);

    private static final String YOU_MUST_SPECIFY    = "You must specify --";
    private static final String DOWNLINK_CONNECTION = " downlink connection";
    
    /**
     * Long option name for FSW Downlink Host option.
     */
    public static final String FSW_DOWNLINK_HOST_LONG = "fswDownlinkHost";
    /**
     * FSW Downlink Host description.
     */
    public static final String FSW_DOWNLINK_HOST_DESC = "the host (source) computing system for flight software downlink";
    /**
     * Long option name for FSW Downlink Port option.
     */
    public static final String FSW_DOWNLINK_PORT_LONG = "fswDownlinkPort";
    /**
     * FSW Downlink Port description.
     */
    public static final String FSW_DOWNLINK_PORT_DESC = "network port to use for flight software downlink";
    /**
     * Long option name for FSW Uplink Host option.
     */
    public static final String FSW_UPLINK_HOST_LONG = "fswUplinkHost";
    /**
     * Long option name for FSW Uplink Port option.
     */
    public static final String FSW_UPLINK_PORT_LONG = "fswUplinkPort";
    /**
     * Long option name for general SSE Host option. Left here for backward compatibility.
     */
    public static final String SSE_HOST_LONG = "sseHost";
    /**
     * SSE Host description
     */
    public static final String SSE_HOST_DESC = "host machine for system support or simulation equipment";
    /**
     * Short option name for the general SSE Host option.
     */
    public static final String                       SSE_HOST_SHORT           = "X";
    /**
     * Long option name for SSE Uplink Host option.
     */
    public static final String SSE_UPLINK_HOST_LONG = "sseHost";
    /**
     * Long option name for SSE Downlink Host option.
     */
    public static final String SSE_DOWNLINK_HOST_LONG = "sseHost";
    /**
     * Long option name for SSE Uplink Port option.
     */
    public static final String SSE_UPLINK_PORT_LONG = "sseUplinkPort";
    /**
     * Short option name for SSE Uplink Port option.
     */
    public static final String                       SSE_UPLINK_PORT_SHORT    = "Y";
    /**
     * Long option name for FSW Downlink Port option.
     */
    public static final String SSE_DOWNLINK_PORT_LONG = "sseDownlinkPort";
    /**
     * FSW Downlink Port description.
     */
    public static final String SSE_DOWNLINK_PORT_DESC = "network port to use for downlinking from system support equipment software";
    /**
     * Long option name for the downlink connection type option.
     */
    public static final String DOWNLINK_CONNECTION_LONG = "downlinkConnectionType";
    /**
     * Downlink connectiontype  description
     */
    public static final String DOWNLINK_CONNECTION_DESC = "the connection type for telemetry input";
    /**
     * Downlink input format description
     */
    public static final String DOWNLINK_INPUT_DESC = "source format of telemetry input; defaults based upon venue type";
    /**
     * Long option name for the uplink connection type option.
     */
    public static final String UPLINK_CONNECTION_LONG = "uplinkConnectionType";
    /**
     * Long option name for the database source key option.
     */
    public static final String DB_SOURCE_KEY_LONG = "dbSourceKey";
    /**
     * Database source key description.
     */
    public static final String DB_SOURCE_KEY_DESC = "the unique numeric identifier for a database session to be used as telemetry data source";
    /**
     * Long option name for the database source host option.
     */
    public static final String DB_SOURCE_HOST_LONG = "dbSourceHost";
    /**
     * Database source host description.
     */
    public static final String DB_SOURCE_HOST_DESC = "the name of the host for a database session to be used as "
            + "telemetry data source";
    /**
     * Long option name for the input source format option.
     */
    public static final String INPUT_TYPE_LONG = "inputFormat";
    /**
     * Long option name for the downlink input file option.
     */
    public static final String INPUT_FILE_LONG = "inputFile";
    /**
     * Downlink input file description.
     */
    public static final String INPUT_FILE_DESC = "FSW downlink data input file or TDS PVL query file";

    private final IConnectionMap connectionMap;
    private final ConnectionProperties connectProps;

    /**
     * FSW_DOWNLINK_HOST command option. Parsing this option sets the
     * "FSW downlink host" property in the IConnectionMap member instance.
     */
    public final StringOption FSW_DOWNLINK_HOST;

    /**
     * FSW_UPLINK_HOST command option. Parsing this option sets the
     * "FSW uplink host" property in the IConnectionMap member instance. A
     * default value is supplied.
     */
    public final StringOption FSW_UPLINK_HOST;

    /**
     * FSW_UPLINK_PORT command option. Parsing this option sets the
     * "FSW uplink port" property in the IConnectionMap member instance. A
     * default value is supplied.
     */
    public final PortOption FSW_UPLINK_PORT;

    /**
     * FSW_DOWLINK_PORT command option. Parsing this option sets the
     * "FSW downlink port" property in the IConnectionMap member instance.
     */
    public final PortOption FSW_DOWNLINK_PORT;

    /**
     * General SSE_HOST command option. Left here for backward compatibility.
     */
    public final StringOption SSE_HOST;
    
	/**
	 * SSE_UPLINK_HOST command option. Parsing this option sets the
	 * "SSE uplink host" property in the IConfigurationMap member instance.
	 */
    public final StringOption SSE_UPLINK_HOST;
    
    /**
	 * SSE_DOWNLINK_HOST command option. Parsing this option sets the
	 * "SSE downlink host" property in the IConfigurationMap member instance.
	 */
    public final StringOption SSE_DOWNLINK_HOST;

    /**
     * SSE_UPLINK_PORT command option. Parsing this option sets the
     * "SSE uplink port" property in the IConnectionMap member instance.
     */
    public final PortOption SSE_UPLINK_PORT;

    /**
     * SSE_DOWNLINK_PORT command option. Parsing this option sets the
     * "SSE downlink port" property in the IConnectionMap member instance.
     */
    public final PortOption SSE_DOWNLINK_PORT;
    
    /**
     * The DOWNLINK_CONNECTION_TYPE command option. Allowed values are
     * restricted to those allowed for the current mission. The parsed value is
     * assigned to the "(downlink) connection type" property in the
     * SessionConfiguration member instance.
     */
    public final EnumOption<TelemetryConnectionType> DOWNLINK_CONNECTION_TYPE;

    /**
     * The UPLINK_CONNECTION_TYPE command option. Allowed values are restricted
     * to those allowed for the current mission. The parsed value is assigned to
     * the "uplink connection type" property in the SessionConfiguration member
     * instance.
     */
    public final EnumOption<UplinkConnectionType> UPLINK_CONNECTION_TYPE;
    
    /**
     * The DB_SOURCE_KEY command option. The value will be range checked. Can
     * only be supplied if the downlink connection type is DATABASE, so the
     * DOWNLINK_CONNECTION_TYPE must already be set into the
     * SessionConfiguration, or the DOWNLINK_CONNECTION_TYPE option must be
     * parsed first. The parsed value will be set into the SessionConfiguration
     * member object as on of the fields in the "database connection key"
     * object.
     */
    public final UnsignedLongOption DB_SOURCE_KEY;

    /**
     * The DB_SOURCE_HOST command option. The value will be range checked. Can
     * only be supplied if the downlink connection type is DATABASE, so the
     * DOWNLINK_CONNECTION_TYPE must already be set into the
     * SessionConfiguration, or the DOWNLINK_CONNECTION_TYPE option must be
     * parsed first. The parsed value will be set into the SessionConfiguration
     * member object as on of the fields in the "database connection key"
     * object.
     */
    public final StringOption DB_SOURCE_HOST;
    
    /**
     * The DOWNLINK_INPUT_TYPE command option. The value must be among the
     * allowed input formats for the current mission. The parsed value will be
     * set as the "raw input type" property in the SessionConfiguration member
     * instance.
     */
    public final DynamicEnumOption<TelemetryInputType> DOWNLINK_INPUT_TYPE;

    /**
     * The DOWNLINK_INPUT_FILE command option. The file path will be verified to
     * exist and be a file. The parsed value will be set as the
     * "downlink input file" property in the SessionConfiguration member
     * instance.
     */
    public final FileOption DOWNLINK_INPUT_FILE;
    

    /**
     * Constructor that uses a unique instance of IConnectionMap, which will
     * be used to set parsed values into.
     * 
     * @param hc
     *            unique connection map instance
     */
    public ConnectionCommandOptions(final IConnectionMap hc) {
        if (hc == null) {
            throw new IllegalArgumentException(
                    "Invalid null argument for host configuration");
        }
        this.connectionMap = hc;
        this.connectProps = hc.getConnectionProperties();

        FSW_DOWNLINK_HOST = new StringOption(
                "A",
                FSW_DOWNLINK_HOST_LONG,
                "hostname",
                FSW_DOWNLINK_HOST_DESC,
                false);

        FSW_DOWNLINK_HOST.setParser(new FswDownlinkHostOptionParser());

        FSW_UPLINK_HOST = new StringOption(null, FSW_UPLINK_HOST_LONG, "hostname",
                "the host (destination) computing system for flight software uplink",
                false);

        FSW_UPLINK_HOST.setParser(new FswUplinkHostOptionParser());

        FSW_UPLINK_PORT = new PortOption("B", FSW_UPLINK_PORT_LONG, "port",
                "network port to use for flight software uplink", false);

        FSW_UPLINK_PORT.setParser(new FswUplinkPortOptionParser());

        FSW_DOWNLINK_PORT = new PortOption("C", FSW_DOWNLINK_PORT_LONG, "port",
                FSW_DOWNLINK_PORT_DESC, false);

        FSW_DOWNLINK_PORT.setParser(new FswDownlinkPortOptionParser());

        SSE_HOST = new StringOption(SSE_HOST_SHORT, SSE_HOST_LONG, "hostname",
                SSE_HOST_DESC,
                false);

        SSE_HOST.setParser(new SseHostOptionParser());
        

        SSE_UPLINK_HOST = new StringOption("X", SSE_UPLINK_HOST_LONG, "hostname",
                "host machine for system support or simulation equipment uplink",
                false);

        SSE_UPLINK_HOST.setParser(new SseUplinkHostOptionParser());
        
        SSE_DOWNLINK_HOST = new StringOption("X", SSE_DOWNLINK_HOST_LONG, "hostname",
                "host machine for system support or simulation equipment downlink",
                false);

        SSE_DOWNLINK_HOST.setParser(new SseDownlinkHostOptionParser());

        SSE_UPLINK_PORT = new PortOption(
                SSE_UPLINK_PORT_SHORT,
                SSE_UPLINK_PORT_LONG,
                "port",
                "network port to use for uplinking to system support equipment software",
                false);

        SSE_UPLINK_PORT.setParser(new SseUplinkPortOptionParser());

        SSE_DOWNLINK_PORT = new PortOption(
                "Z",
                SSE_DOWNLINK_PORT_LONG,
                "port",
                SSE_DOWNLINK_PORT_DESC,
                false);

        SSE_DOWNLINK_PORT.setParser(new SseDownlinkPortOptionParser());
        

        DOWNLINK_CONNECTION_TYPE = new EnumOption<>(TelemetryConnectionType.class, "c",
                                                                           DOWNLINK_CONNECTION_LONG,
                                                                           "downlinkConnection",
                                                                           DOWNLINK_CONNECTION_DESC,
                                                                           false,
                                                                           new LinkedList<TelemetryConnectionType>(connectProps.getAllowedDownlinkConnectionTypes(hc.getSseContextFlag()
                                                                                                                                                                    .isApplicationSse())));

        DOWNLINK_CONNECTION_TYPE
                .setParser(new DownlinkConnectionTypeOptionParser());

        UPLINK_CONNECTION_TYPE = new EnumOption<>(UplinkConnectionType.class, "y", UPLINK_CONNECTION_LONG,
                                                  "uplinkConnection", "the connection type for command output", false,
                                                  new LinkedList<UplinkConnectionType>(connectProps.getAllowedUplinkConnectionTypes(hc.getSseContextFlag()
                                                                                                                                      .isApplicationSse())));

        UPLINK_CONNECTION_TYPE
                .setParser(new UplinkConnectionTypeOptionParser());
        
        DB_SOURCE_KEY = new UnsignedLongOption(
                null,
                DB_SOURCE_KEY_LONG,
                "sessionId",
                DB_SOURCE_KEY_DESC,
                false);

        DB_SOURCE_KEY.setParser(new DbSourceKeyOptionParser());

        DB_SOURCE_HOST = new StringOption(
                null,
                DB_SOURCE_HOST_LONG,
                "hostname",
                DB_SOURCE_HOST_DESC,
                false);

        DB_SOURCE_HOST.setParser(new DbSourceHostOptionParser());

        DOWNLINK_INPUT_TYPE = new DynamicEnumOption<>(TelemetryInputType.class, "f", INPUT_TYPE_LONG, "format",
                                               DOWNLINK_INPUT_DESC,
                                               false,
                                               new LinkedList<TelemetryInputType>(connectProps.getAllowedDownlinkSourceFormats(connectionMap.getSseContextFlag()
                                                                                                                                            .isApplicationSse())));

        DOWNLINK_INPUT_TYPE.setParser(new TelemetryInputTypeOptionParser());

        DOWNLINK_INPUT_FILE = new FileOption("i", INPUT_FILE_LONG, "fileName",
                INPUT_FILE_DESC, false,
                true);
        DOWNLINK_INPUT_FILE.setParser(new InputFileOptionParser());
        

    }

    /**
     * Gets the IConnectionMap member object.
     * 
     * @return IConnectionMap; never null
     */
    public IConnectionMap getConnectionConfiguration() {
        return this.connectionMap;
    }

    /**
     * Gets a Collection of all FSW downlink-related options.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllFswDownlinkOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.add(DOWNLINK_CONNECTION_TYPE);
        result.add(DOWNLINK_INPUT_FILE);
        result.add(DOWNLINK_INPUT_TYPE);
        result.add(DB_SOURCE_HOST);
        result.add(DB_SOURCE_KEY);
        result.add(FSW_DOWNLINK_HOST);
        result.add(FSW_DOWNLINK_PORT);

        return result;
    }

    /**
     * Gets a Collection of all SSE downlink-related options.
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllSseDownlinkOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.add(DOWNLINK_CONNECTION_TYPE);
        result.add(DOWNLINK_INPUT_FILE);
        result.add(DOWNLINK_INPUT_TYPE);
        result.add(DB_SOURCE_HOST);
        result.add(DB_SOURCE_KEY);
        result.add(SSE_DOWNLINK_PORT);
        result.add(SSE_HOST);

        return result;
    }

    /**
     * Gets a Collection of all downlink-related options (FSW and SSE).
     * 
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllDownlinkOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.addAll(getAllFswDownlinkOptions());
        result.addAll(getAllSseDownlinkOptions());

        return result;
    }

    /**
     * Gets a Collection of all FSW uplink-related options.
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllFswUplinkOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.add(UPLINK_CONNECTION_TYPE);
        result.add(FSW_UPLINK_HOST);
        result.add(FSW_UPLINK_PORT);

        return result;
    }

    /**
     * Gets a Collection of all SSE uplink-related options.
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllSseUplinkOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.add(UPLINK_CONNECTION_TYPE);
        result.add(SSE_HOST);
        result.add(SSE_UPLINK_PORT);

        return result;
    }

    /**
     * Gets a Collection of all SSE uplink-related options.
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllSseUplinkOptionsNoConnectionType() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.add(SSE_HOST);
        result.add(SSE_UPLINK_PORT);

        return result;
    }

    /**
     * Gets a Collection of all uplink-related options (FSW and SSE).
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllUplinkOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.addAll(getAllFswUplinkOptions());
        result.addAll(getAllSseUplinkOptions());

        return result;
    }

    /**
     * Gets a Collection of all FSW-related options (uplink and downlink).
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllFswOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.addAll(getAllFswDownlinkOptions());
        result.addAll(getAllFswUplinkOptions());

        return result;

    }

    /**
     * Gets a Collection of all SSE-related options (uplink and downlink).
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllSseOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.addAll(getAllSseDownlinkOptions());
        result.addAll(getAllSseUplinkOptions());

        return result;

    }

    /**
     * Gets the Collection of all options defined by this class.
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllOptions() {
        final Set<ICommandLineOption<?>> result = new TreeSet<>();

        result.addAll(getAllFswOptions());
        result.addAll(getAllSseOptions());

        return result;
    }

    /**
     * Gets the Collection of all options defined by this class that cannot be
     * used to override values in a session configuration file.
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getNonOverridableOptions() {

       return getAllOptions();
    }

    /**
     * Parses uplinkConnectionType option from commandline. Sets
     * no defaults and requires none of the options. Options not present on the
     * specified command line are effectively ignored.
     *
     * @param commandLine
     *            parsed ICommandLine object containing the supplied command
     *            line arguments
     * @throws ParseException
     *             if a parse error occurs
     */
    public void parseUplinkConnectionTypeOption(final ICommandLine commandLine)
    		throws ParseException {
    	UPLINK_CONNECTION_TYPE.parse(commandLine);
    }
    
    /**
     * Parses all defined command options from the supplied command line. Sets
     * no defaults and requires none of the options. Options not present on the
     * specified command line are effectively ignored.
     * 
     * @param commandLine
     *            parsed ICommandLine object containing the supplied command
     *            line arguments
     * @throws ParseException
     *             if a parse error occurs
     */
    public void parseAllOptionsAsOptional(final ICommandLine commandLine)
            throws ParseException {
    	    DOWNLINK_CONNECTION_TYPE.parse(commandLine);
        DOWNLINK_INPUT_FILE.parse(commandLine);
        DOWNLINK_INPUT_TYPE.parse(commandLine);
        DB_SOURCE_HOST.parse(commandLine);
        DB_SOURCE_KEY.parse(commandLine);
        FSW_DOWNLINK_HOST.parse(commandLine);
        FSW_DOWNLINK_PORT.parse(commandLine);
        FSW_UPLINK_HOST.parse(commandLine);
        FSW_UPLINK_PORT.parse(commandLine);
        SSE_HOST.parse(commandLine);
        SSE_DOWNLINK_PORT.parse(commandLine);
        SSE_UPLINK_PORT.parse(commandLine);
    }
    
    /**
     * Parses all defined command options from the supplied command line. Sets
     * no defaults and requires none of the options. Options not present on the
     * specified command line are effectively ignored.
     * 
     * @param commandLine
     *            parsed ICommandLine object containing the supplied command
     *            line arguments
     * @throws ParseException
     *             if a parse error occurs
     *             
     */
    public void parseAllOptionsAsOptionalWithDefaults(final ICommandLine commandLine)
            throws ParseException {
        DOWNLINK_CONNECTION_TYPE.parseWithDefault(commandLine, false, true);
        DOWNLINK_INPUT_FILE.parseWithDefault(commandLine, false, true);
        DOWNLINK_INPUT_TYPE.parseWithDefault(commandLine, false, true);
        DB_SOURCE_HOST.parseWithDefault(commandLine, false, true);
        DB_SOURCE_KEY.parseWithDefault(commandLine, false, true);
        FSW_DOWNLINK_HOST.parseWithDefault(commandLine, false, true);
        FSW_DOWNLINK_PORT.parseWithDefault(commandLine, false, true);
        FSW_UPLINK_HOST.parseWithDefault(commandLine, false, true);
        FSW_UPLINK_PORT.parseWithDefault(commandLine, false, true);
        SSE_HOST.parseWithDefault(commandLine, false, true);
        SSE_DOWNLINK_PORT.parseWithDefault(commandLine, false, true);
        SSE_UPLINK_PORT.parseWithDefault(commandLine, false, true);
    }

    /**
     * Parses all defined command options from the supplied command line. Sets
     * no defaults and requires none of the options. Options not present on the
     * specified command line are effectively ignored.
     *
     * @param commandLine
     *            parsed ICommandLine object containing the supplied command
     *            line arguments
     * @throws ParseException
     *             if a parse error occurs
     *
     */
    public void parseAllOptionsAsOptionalWithoutDefaults(final ICommandLine commandLine)
            throws ParseException {
        DOWNLINK_CONNECTION_TYPE.parseWithDefault(commandLine, false, false);
        DOWNLINK_INPUT_FILE.parseWithDefault(commandLine, false, false);
        DOWNLINK_INPUT_TYPE.parseWithDefault(commandLine, false, false);
        DB_SOURCE_HOST.parseWithDefault(commandLine, false, false);
        DB_SOURCE_KEY.parseWithDefault(commandLine, false, false);
        FSW_DOWNLINK_HOST.parseWithDefault(commandLine, false, false);
        FSW_DOWNLINK_PORT.parseWithDefault(commandLine, false, false);
        FSW_UPLINK_HOST.parseWithDefault(commandLine, false, false);
        FSW_UPLINK_PORT.parseWithDefault(commandLine, false, false);
        SSE_HOST.parseWithDefault(commandLine, false, false);
        SSE_DOWNLINK_PORT.parseWithDefault(commandLine, false, false);
        SSE_UPLINK_PORT.parseWithDefault(commandLine, false, false);
    }

    /**
     * An options parser for the FSW_DOWNLINK_HOST command option. The resulting
     * parsed value will be set into the IConfigurationMap member instance, into the
     * FSW Downlink connection map entry.
     * 
     *
     */
    protected class FswDownlinkHostOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String host = super.parse(commandLine, opt);

            // Make sure Downlink Connection is defined
            final IDownlinkConnection downlinkConnection = connectionMap.getFswDownlinkConnection();
            if (downlinkConnection == null) {
                throw new IllegalStateException("FSW downlink connection not defined");
            }
            // Modified logic here to match what chill_down GUI does
            // See class SessionConfigFswComposite setDataFromFields() method
            if (host != null && (downlinkConnection instanceof INetworkConnection)) {
                ((INetworkConnection)downlinkConnection).setHost(host);
            }
            return host;
        }
    }

    /**
     * An options parser for the FSW_DOWNLINK_PORT command option. The resulting
     * parsed value will be set into the IConnectionMap member instance, into the
     * FSW Downlink connection map entry.
     * 
     *
     */
    protected class FswDownlinkPortOptionParser extends UnsignedIntOptionParser {

        /**
         * Constructor.
         */
        public FswDownlinkPortOptionParser() {
            super(MIN_PORT, MAX_PORT);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final UnsignedInteger port = super.parse(commandLine, opt);

            // Make sure Downlink Connection is defined
            final IDownlinkConnection downlinkConnection = connectionMap.getFswDownlinkConnection();
            if (downlinkConnection == null) {
                throw new IllegalStateException("FSW downlink connection not defined");
            }
            // Modified logic here to match what chill_down GUI does
            // See class SessionConfigFswComposite setDataFromFields() method
            if (port != null && (downlinkConnection instanceof INetworkConnection)) {
            	((INetworkConnection)downlinkConnection).setPort(port.intValue());
            }
            return port;

        }

    }

    /**
     * An options parser for the FSW_UPLINK_PORT command option. The resulting
     * parsed value will be set into the IConnectionMap member instance, into the
     * FSW Uplink connection map entry.
     * 
     *
     */
    protected class FswUplinkHostOptionParser extends StringOptionParser {

        /**
         * Constructor.
         */
        public FswUplinkHostOptionParser() {
            super();
            setDefaultValue(connectProps.getDefaultUplinkHost(false));

        }

        /**
         * @{inheritDoc}
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String host = super.parse(commandLine, opt);
            if (host != null) {
            	if (connectionMap.getFswUplinkConnection() == null) {
            		throw new IllegalStateException("FSW uplink connection object is null");
            	}
                connectionMap.getFswUplinkConnection().setHost(host);
            }
            return host;

        }
        
        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.AbstractOptionParser#parseWithDefault(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption, boolean, boolean)
         */
        @Override
        public String parseWithDefault(final ICommandLine commandLine,
                final ICommandLineOption<String> opt, final boolean required,
                final boolean useDefaults) throws ParseException {
            String host = super.parse(commandLine, opt, required);
            
            if (host == null && useDefaults) {
                host = getDefaultValue();
            }

            /* Matching the behavior of ReservedOptions - it is an
             * error to call this method with useDefaults=true if there 
             * is no configured default and the option is not supplied
             * on the command line.
             */
            if (useDefaults && host == null) {
                throw new MissingOptionException(
                        "No default uplink host is configured, so the option --"
                                + FSW_UPLINK_HOST.getLongOpt()
                                + " requires a value.");
            }
            if (host != null) {
            	if (connectionMap.getFswUplinkConnection() == null) {
            		throw new IllegalStateException("FSW uplink connection object is null");
            	}
            	connectionMap.getFswUplinkConnection().setHost(host);
            }

            return host;

        }

    }

    /**
     * An options parser for the FSW_UPLINK_PORT command option. The resulting
     * parsed value will be set into the IConnectionMap member instance, into the
     * FSW Uplink connection map entry.
     * 
     *
     */
    protected class FswUplinkPortOptionParser extends UnsignedIntOptionParser {

        /**
         * Constructor.
         */
        public FswUplinkPortOptionParser() {
            super(MIN_PORT, MAX_PORT);
            if (connectProps.getDefaultUplinkPort(false) != HostPortUtility.UNDEFINED_PORT) {
                setDefaultValue(UnsignedInteger.valueOf(
                     connectProps.getDefaultUplinkPort(false)));
            }
        }
        
        /**
         * @{inheritDoc}
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {
            final UnsignedInteger port = super.parse(commandLine, opt);

            if (port != null) {
            	if (connectionMap.getFswUplinkConnection() == null) {
            		throw new IllegalStateException("FSW uplink connection object is null");
            	}
                connectionMap.getFswUplinkConnection().setPort(port.intValue());
            }
            return port;

        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.AbstractOptionParser#parseWithDefault(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption, boolean, boolean)
         */
        @Override
        public UnsignedInteger parseWithDefault(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt, final boolean required,
                final boolean useDefaults) throws ParseException {
            UnsignedInteger port = super.parse(commandLine, opt,
                    required);
            
            if (port == null && useDefaults) {
                port = getDefaultValue();
            }


            /* Matching the behavior of ReservedOptions - it is an
             * error to call this method with useDefaults=true if there 
             * is no configured default and the option is not supplied
             * on the command line.
             */
            if (useDefaults && (port == null ||
                     port.intValue() == HostPortUtility.UNDEFINED_PORT)) {
                throw new MissingOptionException(
                        "No default uplink port is configured, so the option --"
                                + FSW_UPLINK_PORT.getLongOpt()
                                + " requires a value.");

            }
            if (port != null) {
            	if (connectionMap.getFswUplinkConnection() == null) {
            		throw new IllegalStateException("FSW uplink connection object is null");
            	}
                connectionMap.getFswUplinkConnection().setPort(port.intValue());
            }
            return port;

        }

    }

    /**
     * An options parser for the SSE_HOST command option. The resulting parsed
     * value will be set into the IConfigurationMap member instance as BOTH SSE 
     * uplink and SSE downlink host, if those connection objects are present in
     * the map.
     * 
     *
     */
    protected class SseHostOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String host = super.parse(commandLine, opt);
            if (host != null) {   
                // Make this check detect missing connections
                // so it behaves like the other parsers in this class
                if (connectionMap.getSseUplinkConnection() != null) {
                    connectionMap.getSseUplinkConnection().setHost(host);
                } else {
                    throw new IllegalStateException("SSE uplink connection is null");
                }
                if (connectionMap.getSseDownlinkConnection() instanceof INetworkConnection) {
                    ((INetworkConnection)connectionMap.getSseDownlinkConnection()).setHost(host);
                } else if (connectionMap.getSseDownlinkConnection() == null) {
                    throw new IllegalStateException("SSE downlink connection is null");
                }
            }
            return host;
        }
    }

    /**
     * An options parser for the SSE_UPLINK_HOST command option. The resulting parsed
     * value will be set into the IConnectionMap member instance, into the
     * SSE Uplink connection map entry.
     * 
     *
     */
    protected class SseUplinkHostOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String host = super.parse(commandLine, opt);
            if (host != null) {
            	if (connectionMap.getSseUplinkConnection() == null) {
            		throw new IllegalStateException("Sse uplink connection object is null");
            	}
                connectionMap.getSseUplinkConnection().setHost(host);
            }
            return host;
        }
    }
    
    /**
     * An options parser for the SSE_UPLINK_HOST command option. The resulting parsed
     * value will be set into the IConnectionMap member instance, into the
     * SSE Downlink connection map entry.
     * 
     *
     */
    protected class SseDownlinkHostOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String host = super.parse(commandLine, opt);
            if (host != null) {
            	if (!(connectionMap.getSseDownlinkConnection() instanceof INetworkConnection)) {
            		throw new IllegalStateException("SSE downlink connection object is null or is not a network connection");
            	}
            	((INetworkConnection)connectionMap.getSseDownlinkConnection()).setHost(host);
            }
            return host;
        }
    }

    /**
     * An options parser for the SSE_DOWNLINK_PORT command option. The resulting
     * parsed value will be set into the IConnectionMap member instance, into the
     * SSE Downlink connection map entry.
     * 
     *
     */
    protected class SseDownlinkPortOptionParser extends UnsignedIntOptionParser {

        /**
         * Constructor.
         */
        public SseDownlinkPortOptionParser() {
            super(MIN_PORT, MAX_PORT);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final UnsignedInteger port = super.parse(commandLine, opt);
            if (port != null) {
            	if (!(connectionMap.getSseDownlinkConnection() instanceof INetworkConnection)) {
            		throw new IllegalStateException("SSE downlink connection object is null or is not a network connection");
            	}
            	((INetworkConnection)connectionMap.getSseDownlinkConnection()).setPort(port.intValue());
            }
            return port;

        }

    }

    /**
     * An options parser for the SSE_UPLINK_PORT command option. The resulting
     * parsed value will be set into the IConnectionMap member instance, into the
     * SSE Uplink connection map entry.
     * 
     *
     */
    protected class SseUplinkPortOptionParser extends UnsignedIntOptionParser {

        /**
         * Constructor.
         */
        public SseUplinkPortOptionParser() {
            super(MIN_PORT, MAX_PORT);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final UnsignedInteger port = super.parse(commandLine, opt);
            if (port != null) {
            	if (connectionMap.getSseUplinkConnection() == null) {
            		throw new IllegalStateException("Sse uplink connection object is null");
            	}
                connectionMap.getSseUplinkConnection().setPort(port.intValue());
            }
            return port;

        }

    }
    
	/**
	 * An option parser class for the DOWNLINK_CONNECTION_TYPE option. The value
	 * is checked against the list of downlink connection types allowed by the
	 * current mission. The resulting parsed value will be set into the
	 * IConnectionMap member instance, into the FSW Downlink or SSE Downlink
	 * connection map entry, depending on whether we are in an SSE context.
	 *
	 *
	 */
    protected class DownlinkConnectionTypeOptionParser extends
            EnumOptionParser<TelemetryConnectionType> {

        /**
         * Constructor.
         */
        public DownlinkConnectionTypeOptionParser() {
            super(TelemetryConnectionType.class,
                    new LinkedList<TelemetryConnectionType>(
                                                          connectProps.getAllowedDownlinkConnectionTypes(connectionMap.getSseContextFlag()
                                                                                                                      .isApplicationSse())));
            setConvertToUpperCase(true);
            setAllowUnknown(false);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.EnumOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public TelemetryConnectionType parse(final ICommandLine commandLine,
                                             final ICommandLineOption<TelemetryConnectionType> opt)
                throws ParseException {
            final TelemetryConnectionType type = super.parse(commandLine, opt);
            if (type != null) {
                final IDownlinkConnection dc = connectionMap.getDownlinkConnection();

                // No downlink connection has been created yet, just do it
                if (dc == null) {
                    connectionMap.createDownlinkConnection(type);
                }
                else if (dc.getDownlinkConnectionType() != type) {
                    /**
                     * Create a new IDownlinkConnection for the TelemetryConnectionType
                     * We have a downlink connection, but of a different type than the command-line value. Copy relevant
                     * configuration from the existing connection because it may have already been parsed
                     */
                    final String host = dc instanceof INetworkConnection ? ((INetworkConnection) dc).getHost() : "";
                    final int port = dc instanceof INetworkConnection ? ((INetworkConnection) dc).getPort() : HostPortUtility.UNDEFINED_PORT;

                    connectionMap.createDownlinkConnection(type);

                    if (connectionMap.getDownlinkConnection() instanceof INetworkConnection) {
                        if (HostPortUtility.isPortValid(port)) {
                            ((INetworkConnection) connectionMap.getDownlinkConnection()).setPort(port);
                        }
                        if (!host.isEmpty()) {
                            ((INetworkConnection) connectionMap.getDownlinkConnection()).setHost(host);
                        }
                    }
                }
            }

            return type;
        }

    }

    /**
     * An option parser class for the UPLINK_CONNECTION_TYPE option. The value
     * is checked against the list of uplink connection types allowed by the
     * current mission. The resulting parsed value will be set into the
	 * IConnectionMap member instance, into the FSW Uplink or SSE Uplink
	 * connection map entry, depending on whether we are in an SSE context.
     * 
     *
     */
    protected class UplinkConnectionTypeOptionParser extends
            EnumOptionParser<UplinkConnectionType> {

        /**
         * Constructor.
         */
        public UplinkConnectionTypeOptionParser() {
            super(UplinkConnectionType.class,
                  new LinkedList<UplinkConnectionType>(connectProps.getAllowedUplinkConnectionTypes(connectionMap.getSseContextFlag()
                                                                                                                 .isApplicationSse())));
            setConvertToUpperCase(true);
            setAllowUnknown(false);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.EnumOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UplinkConnectionType parse(final ICommandLine commandLine,
                final ICommandLineOption<UplinkConnectionType> opt)
                throws ParseException {

            final UplinkConnectionType type = super.parse(commandLine, opt);

            if (type != null) {
          
                final IUplinkConnection uc = connectionMap.getUplinkConnection(); 
                
                int port = -1;
                String host = "";
                
                if(uc instanceof INetworkConnection) {
                	host = ((INetworkConnection) connectionMap.getFswUplinkConnection()).getHost();
                    port = ((INetworkConnection) connectionMap.getFswUplinkConnection()).getPort();
                }
                
                if (uc == null || uc.getUplinkConnectionType() != type) {
                	connectionMap.createUplinkConnection(type);
                	
                	if (connectionMap.getFswUplinkConnection() instanceof INetworkConnection) {
                		if (port >=0) {
                			((INetworkConnection) connectionMap.getFswUplinkConnection()).setPort(port);
                		}
                		if (!host.isEmpty()) {
                            ((INetworkConnection) connectionMap.getFswUplinkConnection()).setHost(host);
                        }
                	}
                }
                
            } 
            
            return type;
        }
        
    }

    /**
     * An option parser class for the DOWNLINK_INPUT_TYPE option. The value will
     * be checked against the list of allowed input types for the current
     * mission. The resulting parsed value will be set into the
	 * IConnectionMap member instance, into the FSW Downlink or SSE Downlink
	 * connection map entry, depending on whether we are in an SSE context.
     *
     *
     */
    protected class TelemetryInputTypeOptionParser extends
            DynamicEnumOptionParser<TelemetryInputType> {

        /**
         * Constructor.
         */
        public TelemetryInputTypeOptionParser() {
            super(TelemetryInputType.class,
                  new LinkedList<TelemetryInputType>(connectProps.getAllowedDownlinkSourceFormats(connectionMap.getSseContextFlag().isApplicationSse())));
            setConvertToUpperCase(true);
            setAllowUnknown(false);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.EnumOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public TelemetryInputType parse(final ICommandLine commandLine,
                final ICommandLineOption<TelemetryInputType> opt) throws ParseException {

            TelemetryInputType type = super.parse(commandLine, opt);
            final IDownlinkConnection dc = connectionMap.getDownlinkConnection();

            if (type != null) {

                if (dc == null) {
                	throw new IllegalStateException("Downlink connection in session configuration is null");
                }

                dc.setInputType(type);

            } else if (dc instanceof DownlinkFileConnection) {
            	if (((IFileConnectionSupport)dc).getFile() != null) {
            		final TelemetryInputType extractedType = extractRawInputType(((IFileConnectionSupport)dc).getFile());
            		if (extractedType != null) {
            			dc.setInputType(extractedType);
            			type = extractedType;
            		}
            	}
            }

            final TelemetryConnectionType tct = dc.getDownlinkConnectionType();
            if (type != null
                    && tct != null && !connectProps
                                                   .getAllowedDownlinkSourceFormats(tct,
                                                                                    connectionMap.getSseContextFlag()
                                                                                                 .isApplicationSse())
                                                   .contains(type)) {
                throw new ParseException(type + " is not an allowed value for the --" +
                        DOWNLINK_INPUT_TYPE.getLongOpt() + " option" +
                        (tct == null ? "" : " when the downlink connection type is " +
                                tct));
            }
            return type;
        }
    }

    /**
     * Option parser class for the DOWNLINK_INPUT_FILE option. Will validate the
     * value is an existing file. The resulting parsed value will be set into the
	 * IConnectionMap member instance, into the FSW Downlink or SSE Downlink
	 * connection map entry, depending on whether we are in an SSE context.
     * 
     *
     */
    protected class InputFileOptionParser extends FileOptionParser {

        /**
         * Consstructor.
         */
        public InputFileOptionParser() {
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

            String file = super.parse(commandLine, opt);

            if (file == null) {
                final String[] extraArgs = commandLine.getTrailingArguments();
                if (extraArgs != null && extraArgs.length != 0) {
                    file = extraArgs[0];
                }
            }

            if (file != null) {

                final File temp = new File(file);
                if (!temp.exists() || !temp.isFile()) {
                    throw new ParseException("The file specified on the command line ("
                                                     +  file
                                                     + ") does not exist or is not a file");
                }

                if (file.length() > ConfigurationConstants.FILE_LENGTH) {
                    throw new ParseException(
                            "The value of the --"
                                    + DOWNLINK_INPUT_FILE.getLongOpt()
                                    + " option cannot be longer than " +  ConfigurationConstants.FILE_LENGTH);
                }
            	                
                final IDownlinkConnection dc = connectionMap.getDownlinkConnection();

                if (!(dc instanceof IFileConnectionSupport)) {
                	throw new ParseException("Downlink input file specified but the input connection type does not require a file");
                }

                ((IFileConnectionSupport)dc).setFile(file);
                
                if (!commandLine.hasOption(INPUT_TYPE_LONG)) {
                    final TelemetryInputType extractedType = extractRawInputType(((IFileConnectionSupport)dc).getFile());
                    if (extractedType != null) {
                        dc.setInputType(extractedType);
                    }
                }
            } 
            return file;
        }
    }
    
    /**
     * Option parser class for the DB_SOURCE_KEY option. Checks to see that the
     * downlink connection type is DATABASE.The resulting parsed value will be set into the
	 * IConnectionMap member instance, into the FSW Downlink or SSE Downlink
	 * connection map entry, depending on whether we are in an SSE context.
     * 
     *
     */
    protected class DbSourceKeyOptionParser extends UnsignedLongOptionParser {

        /**
         * Constructor.
         */
        public DbSourceKeyOptionParser() {
            super(MIN_SESSION_KEY, MAX_SESSION_KEY);
        }

        @Override
        public UnsignedLong parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedLong> opt) throws ParseException {
            final UnsignedLong key = super.parse(commandLine, opt);
            if (key != null) {

                final IDownlinkConnection dc = connectionMap.getDownlinkConnection();

                if (dc == null) {
                    // Made exception consistent with the other missing connection handling
                    throw new IllegalStateException(
                            "Downlink connection is not set in the current session configuration");
                }

                if (dc.getDownlinkConnectionType() != TelemetryConnectionType.DATABASE) {
                    throw new ParseException(
                            "The --"
                                    + DB_SOURCE_KEY.getLongOpt()
                                    + " option can only be used with a downlink connection type of "
                                    + TelemetryConnectionType.DATABASE);
                }

                final DatabaseConnectionKey keyObj = ((IDatabaseConnectionSupport)dc)
                        .getDatabaseConnectionKey();
                keyObj.addSessionKey(key.longValue());

                checkKeyOption(keyObj);
            }
            return key;
        }

        private void checkKeyOption(final DatabaseConnectionKey dsi) throws ParseException{
            boolean noKey       = dsi.getSessionKeyList().isEmpty();
            final Long currentKey  = !noKey ? dsi.getSessionKeyList().get(0) : null;

            // If key is less than 1, ignore it
            noKey = noKey || (currentKey == null) || (currentKey <= 0L);
            if (noKey) {
                throw new ParseException(YOU_MUST_SPECIFY + DB_SOURCE_KEY.getLongOpt() + " option for a "
                                                 + TelemetryConnectionType.DATABASE + DOWNLINK_CONNECTION);
            }

            dsi.setSessionKey(currentKey);
        }

    }

    /**
     * Option parser class for the DB_SOURCE_HOST option. Checks to see that the
     * downlink connection type is DATABASE. The resulting parsed value will be set into the
	 * IConnectionMap member instance, into the FSW Downlink or SSE Downlink
	 * connection map entry, depending on whether we are in an SSE context.
     * 
     *
     */
    protected class DbSourceHostOptionParser extends StringOptionParser {

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {

            final String host = super.parse(commandLine, opt);

            if (host != null) {

            	final IDownlinkConnection dc = connectionMap.getDownlinkConnection();

                if (dc == null) {
                    // Made exception consistent with the other missing connection detection
                    throw new IllegalStateException(
                            "Downlink connection is not set in the current session configuration");
                }

                if (dc.getDownlinkConnectionType() != TelemetryConnectionType.DATABASE) {
                    throw new ParseException(
                            "The --"
                                    + DB_SOURCE_HOST.getLongOpt()
                                    + " option can only be used with a downlink connection type of "
                                    + TelemetryConnectionType.DATABASE);
                }
                             
                final HostNameValidator valid = new HostNameValidator();
                final String error = valid.isValid(host);
                if (error != null) {
                    throw new ParseException(
                            "The value of the --"
                                    + DB_SOURCE_HOST.getLongOpt() + " option is invalid: " + error);
                }

                final DatabaseConnectionKey keyObj = ((IDatabaseConnectionSupport)dc)
                        .getDatabaseConnectionKey();
                keyObj.addHostPattern(host);

                checkHostOption(keyObj);
            }
            return host;
        }

        private void checkHostOption(final DatabaseConnectionKey dsi) throws ParseException{
            boolean noHost      = dsi.getHostPatternList().isEmpty();
            String       currentHost = !noHost ? StringUtil.emptyAsNull(dsi.getHostPatternList().get(0)) : null;
            // If host is null or empty, ignore it
            noHost = noHost || (currentHost == null);

            if (noHost) {
                throw new ParseException(YOU_MUST_SPECIFY + DB_SOURCE_HOST.getLongOpt() + " option for a " +
                                TelemetryConnectionType.DATABASE + DOWNLINK_CONNECTION);
            }

            // Do not allow "localhost", rather convert it to the real name.
            if (HostPortUtility.LOCALHOST.equalsIgnoreCase(currentHost)) {
                currentHost = HostPortUtility.getLocalHostName();
            }

            dsi.setHostPattern(currentHost.toLowerCase());
        }
    }
    
    private TelemetryInputType extractRawInputType(final String file)
    {
        final String name = StringUtil.safeTrimAndUppercase(file);
        final int    dot  = name.lastIndexOf('.');

        if (dot < 0)
        {
            return null;
        }

        final String type = name.substring(dot + 1);

        if ("TF".equals(type))
        {
            return TelemetryInputType.RAW_TF;
        }

        TelemetryInputType rit = null;

        try
        {
            rit = TelemetryInputType.valueOf(type);
        }
        catch (final IllegalArgumentException iae)
        {
            return null;
        }

        if (rit != null && rit.equals(TelemetryInputType.UNKNOWN))
        {
            return null;
        }

        return rit;
    }

    /**
     * @param connType
     *            Telemetry connection type long option value
     * @param inputFormat
     *            Input format long option value
     * @param inputFile
     *            Input file long option value
     * @param fswHost
     *            fsw host long option value
     * @param fswPort
     *            fsw port long option value
     * @param sseHost
     *            sse host long option value
     * @param ssePort
     *            sse port long option value
     * @param dbSourceHost
     *            database source host long option value
     * @param dbSourceKey
     *            database key long option value
     * @return an array list of command-line arguments and their arguments - if present
     */
    public static List<String> buildConnectionCliFromArgs(final TelemetryConnectionType connType,
                                                     final TelemetryInputType inputFormat, final String inputFile,
                                                     final String fswHost, final Integer fswPort, final String sseHost,
                                                     final Integer ssePort, final String dbSourceHost,
                                                     final Integer dbSourceKey) {
        final List<String> argList = new ArrayList<>();

        if (connType != null) {
            argList.add(DASHES + ConnectionCommandOptions.DOWNLINK_CONNECTION_LONG);
            argList.add(connType.toString());
        }

        if (inputFormat != null) {
            argList.add(DASHES + ConnectionCommandOptions.INPUT_TYPE_LONG);
            argList.add(inputFormat.toString());
        }
        if (inputFile != null) {
            argList.add(DASHES + ConnectionCommandOptions.INPUT_FILE_LONG);
            argList.add(inputFile);
        }
        if (fswHost != null) {
            argList.add(DASHES + ConnectionCommandOptions.FSW_DOWNLINK_HOST_LONG);
            argList.add(fswHost);
        }
        if (fswPort != null) {
            argList.add(DASHES + ConnectionCommandOptions.FSW_DOWNLINK_PORT_LONG);
            argList.add(fswPort.toString());
        }
        if (sseHost != null) {
            argList.add(DASHES + ConnectionCommandOptions.SSE_HOST_LONG);
            argList.add(sseHost);
        }
        if (ssePort != null) {
            argList.add(DASHES + ConnectionCommandOptions.SSE_DOWNLINK_PORT_LONG);
            argList.add(ssePort.toString());
        }
        if (dbSourceHost != null) {
            argList.add(DASHES + ConnectionCommandOptions.DB_SOURCE_HOST_LONG);
            argList.add(dbSourceHost);
        }
        if (dbSourceKey != null) {
            argList.add(DASHES + ConnectionCommandOptions.DB_SOURCE_KEY_LONG);
            argList.add(dbSourceKey.toString());
        }

        return argList;
    }


}
