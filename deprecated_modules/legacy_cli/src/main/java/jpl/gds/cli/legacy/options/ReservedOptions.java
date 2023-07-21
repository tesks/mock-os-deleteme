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

package jpl.gds.cli.legacy.options;

import java.io.File;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.springframework.context.ApplicationContext;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.cli.legacy.app.CommandLineApp;
import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.INetworkConnection;
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.db.api.types.IDbSessionUpdater;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.message.api.config.MessageServiceConfiguration;
import jpl.gds.perspective.ApplicationConfiguration;
import jpl.gds.perspective.ApplicationType;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.ParseDssException;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.holders.VcidHolder;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.util.HostPortUtility;
import jpl.gds.shared.xml.validation.XmlValidationException;

/**
 * ReservedOptions is a class for specifying command line options whose
 * usages are universally common in MPCS. ReservedOptions has been made completely static
 * so that an instance need not be created in order to use the class.
 *
 * 7/31/13 - MPCS-4992: replaced password file with keytab file
 * 
 * MPCS-7772 - 11/30/15. Removed calls to HostConfiguration force methods
 * MPCS-9572 - 4/3/18 - Changes throughout to pull stuff from the application
 *          context upon demand
 */          
public class ReservedOptions
{
    /** min session key */
    public static final int MIN_SESSION_KEY = 1;

    /** max session key */
    public static final int MAX_SESSION_KEY = 16777215;

    /** min port */
    public static final int MIN_PORT = 0;

    /** max port */
    public static final int MAX_PORT = HostPortUtility.MAX_PORT_NUMBER;

    /** min SCID */
    public static final int MIN_SCID = 0;

    /** max SCID */
    public static final int MAX_SCID = 65535;

    /** min VCID*/
    public static final long MIN_VCID = VcidHolder.MIN_VALUE;

    /** max VCID */
    public static final long MAX_VCID = VcidHolder.MAX_VALUE;

    /** min station */
    public static final int MIN_STATION = StationIdHolder.MIN_VALUE;

    /** max station */
    public static final int MAX_STATION = StationIdHolder.MAX_VALUE;

    //Static option declarations. This is the new desirable way to implement things in this class since option values can overlap.

    /** APPLICATION_CONFIGURATION */
    public static final Option APPLICATION_CONFIGURATION = new MpcsOption("U","appConfig",true,"filename","the location of the application configuration file.");

    /** AUTORUN */
    public static final Option AUTORUN = new MpcsOption("a","autoRun",false,null,"run without any prompts or session configuration window.");

    /** CONNECTION_TYPE */
    public static final Option CONNECTION_TYPE =
        new MpcsOption("c",
                       "downlinkConnectionType",
                       true,
                       "downlinkConnection",
                       "the input source for the session");
    
    /** UPLINK_CONNECTION_TYPE */
    public static final Option UPLINK_CONNECTION_TYPE =
        new MpcsOption("y",
                       "uplinkConnectionType",
                       true,
                       "uplinkConnection",
                       "the output destination for the session");
   
    /** DATABASE_HOST */
    public static final Option DATABASE_HOST = new MpcsOption("j","databaseHost",true,"host","The host that the database resides on.   This value may also be the" +
            " name of one of the testbeds (e.g. FSWTB) and the application will automatically determine the proper hostname" +
            " of the GDS machine in the specified testbed.");

    /** DATABASE_PASSWORD */
    public static final Option DATABASE_PASSWORD = new MpcsOption(null,"dbPwd",true,"password","The password required to connect to the database.");

    /** DATABASE_PORT */
    public static final Option DATABASE_PORT = new MpcsOption("n","databasePort",true,"port","The port number that the database to query is listening on.");

    /** DATABASE_USERNAME */
    public static final Option DATABASE_USERNAME = new MpcsOption(null,"dbUser",true,"username","The username required to connect to the database.");

    /** DEBUG */
    public static final Option DEBUG = new MpcsOption("d","debug",false,null,"run in debug mode.");

    /** DOWNLINK_STREAM_ID */
    public static final Option DOWNLINK_STREAM_ID = new MpcsOption("E","downlinkStreamId",true,"stream","downlink stream ID for TESTBED or ATLO:'Selected DL',LV,TZ,'Command Echo'");

    /** FSW_DICTIONARY_DIRECTORY */
    public static final Option FSW_DICTIONARY_DIRECTORY = new MpcsOption("F","fswDictionaryDir",true,"directory","dictionary directory (may be command, telemetry or some other entity)");

    /** FSW_DOWNLINK_HOST */
    public static final Option FSW_DOWNLINK_HOST = new MpcsOption("A","fswDownlinkHost",true,"hostname","the host machine for flight software downlink.");

    /** FSW_UPLINK_HOST */
    public static final Option FSW_UPLINK_HOST = new MpcsOption(null, "fswUplinkHost",true,"hostname","the host machine for flight software uplink.");

    /** FSW_UPLINK_PORT */
    public static final Option FSW_UPLINK_PORT = new MpcsOption("B","fswUplinkPort",true,"port","I/O port to use for flight software uplink.");

    /** FSW_DOWNLINK_PORT */
    public static final Option FSW_DOWNLINK_PORT = new MpcsOption("C","fswDownlinkPort",true,"port","I/O port to use for flight software downlink.");

    /** FSW_VERSION */
    public static final Option FSW_VERSION = new MpcsOption("D","fswVersion",true,"version","flight software version");

    /** GUI */
    public static final Option GUI = new MpcsOption("g","gui",false,null,"run with gui.");

    /** HELP */
    public static final Option HELP = new MpcsOption("h","help",false,null,"display help information.");

    /** JMS_HOST */
    public static final Option JMS_HOST = new MpcsOption(null,"jmsHost",true,"hostname","Host where the JMS message server is running.");

    /** JMS_PORT */
    public static final Option JMS_PORT = new MpcsOption(null,"jmsPort",true,"port","Port on which the JMS message server is listening.");

    /** JMS_SUBTOPIC */
    public static final Option JMS_SUBTOPIC = new MpcsOption(null,"jmsSubtopic",true,"subtopic","Name of the JMS subtopic for Ops venues.");

    /** NO_DATABASE */
    public static final Option NO_DATABASE = new MpcsOption("I","noDatabase",false,null,"execute without connecting to a database.");

    /** NO_GUI */
    public static final Option NO_GUI = new MpcsOption("H","noGUI",false,null,"execute without a GUI.");

    /** NO_JMS */
    public static final Option NO_JMS = new MpcsOption("J","noJMS",false,null,"execute without using the Java Messaging Service.");

    /** OUTPUT_DIRECTORY */
    public static final Option OUTPUT_DIRECTORY = new MpcsOption("R","outputDir",true,"directory","directory for saving output files of program.");

    /** QUIET */
    public static final Option QUIET = new MpcsOption("q","quiet",false,null,"suppress writing to standard output; writing to standard error will still be performed.");

    /** SESSION_CONFIGURATION */
    public static final Option SESSION_CONFIGURATION = new MpcsOption("N","testConfig",true,"filename","the session configuration to be executed.");

    /** SESSION_DESCRIPTION */
    public static final Option SESSION_DESCRIPTION = new MpcsOption("L","testDescription",true,"description","a description of the session to be executed.");

    /** SESSION_HOST */
    public static final Option SESSION_HOST = new MpcsOption("O","testHost",true,"hostname","the name of the host machine for executing the session.");

    /** SESSION_KEY */
    public static final Option SESSION_KEY =
        new MpcsOption("K","testKey",true,
                       "sessionId",                
        		"the unique numeric identifier for a session.");

    /** DATABASE_SOURCE_HOST */
    public static final Option DB_SESSION_HOST = new MpcsOption(null ,"dbSourceHost",true,"hostname","the name of the host for a session to be used as telemetry data source");

    /** DATABASE_SOURCE_KEY */
    public static final Option DB_SESSION_KEY =
        new MpcsOption(null,"dbSourceKey",true,
                       "sessionId",               
        		"the unique numeric identifier for a session to be used as telemetry data source.");
    
    /** SESSION_NAME */
    public static final Option SESSION_NAME = new MpcsOption("M","testName",true,"name","the name of the session to be executed.");

    /** SESSION_TYPE */
    public static final Option SESSION_TYPE = new MpcsOption("Q","testType",true,"type","the type of the session to be executed.");

    /** SESSION_USER */
    public static final Option SESSION_USER = new MpcsOption("P","testUser",true,"username","the name of the user/entity executing the session.");

    /** SPACECRAFT_ID */
    public static final Option SPACECRAFT_ID = new MpcsOption("S","spacecraftID",true,"scid","spacecraft id; must be numeric and must be derived from 820-013 OPS-6-21.");

    /** SSE_DICTIONARY_DIRECTORY */
    public static final Option SSE_DICTIONARY_DIRECTORY = new MpcsOption("T","sseDictionaryDir",true,"directory","dictionary directory (may be command, telemetry or some other entity).");

    /** SSE_DOWNLINK_PORT */
    public static final Option SSE_DOWNLINK_PORT = new MpcsOption("Z","sseDownlinkPort",true,"port","I/O port to use for downlinking from subsystem support equipment software.");

    /** SSE_HOST */
    public static final Option SSE_HOST = new MpcsOption("X","sseHost",true,"hostname","host machine for subsystem support equipment");

    /** SSE_UPLINK_PORT */
    public static final Option SSE_UPLINK_PORT = new MpcsOption("Y","sseUplinkPort",true,"port","I/O port to use for uplinking to subsystem support equipment software.");

    /** SSE_VERSION */
    public static final Option SSE_VERSION = new MpcsOption("W","sseVersion",true,"version","Simulation & support equipment software dictionary version.");

    /** TESTBED_NAME */
    public static final Option TESTBED_NAME = new MpcsOption("G","testbedName",true,"testbed","The name of the testbed.  Only applicable if venue type is TESTBED or ATLO.");

    /** VENUE_TYPE */
    public static final Option VENUE_TYPE = new MpcsOption("V","venueType",true,"venue","operational or test venue to use");

    /** VERSION */
    public static final Option VERSION = new MpcsOption("v","version",false,null,"display the version of the software being executed.");


    /** SESSION_VCID */
    public static final Option SESSION_VCID = new MpcsOption(null,"sessionVcid",true,"id","virtual channel identifier (VCID) for session downlink.");

    /** SESSION_DSSID */
    public static final Option SESSION_DSSID = new MpcsOption(null,"sessionDssId",true,"id","station identifier (DSSID) for session downlink.");
    
    /** INTEGRATED_CHILL */
    public static final Option INTEGRATED_CHILL = new MpcsOption(null,"integratedChill",false,"integrated","hidden option used to determine if processes are truly standalone or child processes.");
 

    /** USER ROLE */
    public static final Option USER_ROLE = new MpcsOption(null,"role",true,"userRole","Security user role.");
    
    /** LOGIN METHOD FOR GUI APPS */
    public static final Option LOGIN_METHOD_GUI = new MpcsOption(null,"loginMethod",true,"loginMethod","Security login method. Choices: " + LoginEnum.guiChoices());
    
    /** LOGIN METHOD FOR COMMAND LINE APPS */
    public static final Option LOGIN_METHOD_NON_GUI = new MpcsOption(null,"loginMethod",true,"loginMethod","Security login method. Choices: " + LoginEnum.nonGuiChoices());
    
    // 7/31/13 - MPCS-4992: added username option for keytab
    /** KEYTAB FILE */
    public static final Option KEYTAB_FILE = new MpcsOption(null,"keytabFile",true,"keytabFile","Security keytab file. Needed with --" + LOGIN_METHOD_NON_GUI.getLongOpt()
						+ " of " + LoginEnum.KEYTAB_FILE);
    
    /** USERNAME */
    public static final Option USERNAME = new MpcsOption(null,"username",true,"username","Security username. Needed with --" + LOGIN_METHOD_NON_GUI.getLongOpt()
			+ " of " + LoginEnum.KEYTAB_FILE);

    /** INPUT_FORMAT */
    public static final Option INPUT_FORMAT = new MpcsOption("f","inputFormat",true,"format","source format of input; defaults based upon venue type.");
    
    /** INPUT_FILE */
    public static final Option INPUT_FILE = new MpcsOption("i","inputFile",true,"fileName","FSW downlink data input file or TDS PVL query file");
    
    // MPCS-7766 12/10/15 - Adding option to enable BufferedRawInputStream
    /** BUFFERED INPUT STREAM_FIELD*/
    public static final Option BUFFERED_INPUT_STREAM = new MpcsOption(null,"bufferedInput", true,"enabledIn","Enable Buffered Input Stream for downlink mode NONE, FSW, SSE, or BOTH "
    		+ " and CLIENT_SOCKET or TDS connections.");
    
    /** AUTORUN_SHORT_VALUE */
    public static final String AUTORUN_SHORT_VALUE = AUTORUN.getOpt();

    /** CONNECTION_TYPE_SHORT_VALUE */
    public static final String CONNECTION_TYPE_SHORT_VALUE = CONNECTION_TYPE.getOpt();

    /** UPLINK_CONNECTION_TYPE_SHORT_VALUE */
    public static final String UPLINK_CONNECTION_TYPE_SHORT_VALUE = UPLINK_CONNECTION_TYPE.getOpt();

    /** DEBUG_SHORT_VALUE */
    public static final String DEBUG_SHORT_VALUE = DEBUG.getOpt();

    /** GUI_SHORT_VALUE */
    public static final String GUI_SHORT_VALUE = GUI.getOpt();

    /** HELP_SHORT_VALUE */
    public static final String HELP_SHORT_VALUE = HELP.getOpt();

    /** QUIET_SHORT_VALUE */
    public static final String QUIET_SHORT_VALUE = QUIET.getOpt();

    /** VERSION_SHORT_VALUE */
    public static final String VERSION_SHORT_VALUE = VERSION.getOpt();

    /** FSW_DOWNLINK_HOST_SHORT_VALUE */
    public static final String FSW_DOWNLINK_HOST_SHORT_VALUE = FSW_DOWNLINK_HOST.getOpt();

    /** FSWUPLINKPORT_SHORT_VALUE */
    public static final String FSWUPLINKPORT_SHORT_VALUE = FSW_UPLINK_PORT.getOpt();

    /** FSWDOWNLINKPORT_SHORT_VALUE */
    public static final String FSWDOWNLINKPORT_SHORT_VALUE = FSW_DOWNLINK_PORT.getOpt();

    /** FSWVERSION_SHORT_VALUE */
    public static final String FSWVERSION_SHORT_VALUE = FSW_VERSION.getOpt();

    /** DOWNLINKSTREAM_SHORT_VALUE */
    public static final String DOWNLINKSTREAM_SHORT_VALUE = DOWNLINK_STREAM_ID.getOpt();

    /** FSW_DICTIONARYDIR_SHORT_VALUE */
    public static final String FSW_DICTIONARYDIR_SHORT_VALUE = FSW_DICTIONARY_DIRECTORY.getOpt();

    /** TESTBEDNAME_SHORT_VALUE */
    public static final String TESTBEDNAME_SHORT_VALUE = TESTBED_NAME.getOpt();

    /** NOGUI_SHORT_VALUE */
    public static final String NOGUI_SHORT_VALUE = NO_GUI.getOpt();

    /** NODATABASE_SHORT_VALUE */
    public static final String NODATABASE_SHORT_VALUE = NO_DATABASE.getOpt();

    /** NOJMS_SHORT_VALUE */
    public static final String NOJMS_SHORT_VALUE = NO_JMS.getOpt();

    /** TESTKEY_SHORT_VALUE */
    public static final String TESTKEY_SHORT_VALUE = SESSION_KEY.getOpt();

    /** TESTDESCRIPTION_SHORT_VALUE */
    public static final String TESTDESCRIPTION_SHORT_VALUE = SESSION_DESCRIPTION.getOpt();

    /** TESTNAME_SHORT_VALUE */
    public static final String TESTNAME_SHORT_VALUE = SESSION_NAME.getOpt();

    /** TESTCONFIG_SHORT_VALUE */
    public static final String TESTCONFIG_SHORT_VALUE = SESSION_CONFIGURATION.getOpt();

    /** TESTHOST_SHORT_VALUE */
    public static final String TESTHOST_SHORT_VALUE = SESSION_HOST.getOpt();

    /** TESTUSER_SHORT_VALUE */
    public static final String TESTUSER_SHORT_VALUE = SESSION_USER.getOpt();

    /** TESTTYPE_SHORT_VALUE */
    public static final String TESTTYPE_SHORT_VALUE = SESSION_TYPE.getOpt();

    /** OUTPUTDIR_SHORT_VALUE */
    public static final String OUTPUTDIR_SHORT_VALUE = OUTPUT_DIRECTORY.getOpt();

    /** SPACECRAFTID_SHORT_VALUE */
    public static final String SPACECRAFTID_SHORT_VALUE = SPACECRAFT_ID.getOpt();

    /** SSE_DICTIONARYDIR_SHORT_VALUE */
    public static final String SSE_DICTIONARYDIR_SHORT_VALUE = SSE_DICTIONARY_DIRECTORY.getOpt();

    /** APPCONFIG_SHORT_VALUE */
    public static final String APPCONFIG_SHORT_VALUE = APPLICATION_CONFIGURATION.getOpt();

    /** VENUETYPE_SHORT_VALUE */
    public static final String VENUETYPE_SHORT_VALUE = VENUE_TYPE.getOpt();

    /** SSEVERSION_SHORT_VALUE */
    public static final String SSEVERSION_SHORT_VALUE = SSE_VERSION.getOpt();

    /** SSEHOST_SHORT_VALUE */
    public static final String SSEHOST_SHORT_VALUE = SSE_HOST.getOpt();

    /** SSEUPLINKPORT_SHORT_VALUE */
    public static final String SSEUPLINKPORT_SHORT_VALUE = SSE_UPLINK_PORT.getOpt();

    /** SSEDOWNLINKPORT_SHORT_VALUE */
    public static final String SSEDOWNLINKPORT_SHORT_VALUE = SSE_DOWNLINK_PORT.getOpt();

    /** DATABASE_HOST_SHORT_VALUE */
    public static final String DATABASE_HOST_SHORT_VALUE = DATABASE_HOST.getOpt();

    /** DATABASE_PORT_SHORT_VALUE */
    public static final String DATABASE_PORT_SHORT_VALUE = DATABASE_PORT.getOpt();
    
    // 8/5/13 - Added for MPCS-5109.
    /** INPUT_FORMAT_SHORT_VALUE */
    public static final String INPUT_FORMAT_SHORT_VALUE = INPUT_FORMAT.getOpt();
    
    /** INPUT_FILE_SHORT_VALUE */
    public static final String INPUT_FILE_SHORT_VALUE = INPUT_FILE.getOpt();

    /** AUTORUN_LONG_VALUE */
    public static final String AUTORUN_LONG_VALUE = AUTORUN.getLongOpt();

    /** CONNECTION_TYPE_LONG_VALUE */
    public static final String CONNECTION_TYPE_LONG_VALUE = CONNECTION_TYPE.getLongOpt();

    /** UPLINK_CONNECTION_TYPE_LONG_VALUE */
    public static final String UPLINK_CONNECTION_TYPE_LONG_VALUE = UPLINK_CONNECTION_TYPE.getLongOpt();

    /** DEBUG_LONG_VALUE */
    public static final String DEBUG_LONG_VALUE = DEBUG.getLongOpt();

    /** GUI_LONG_VALUE */
    public static final String GUI_LONG_VALUE = GUI.getLongOpt();

    /** HELP_LONG_VALUE */
    public static final String HELP_LONG_VALUE = HELP.getLongOpt();

    /** QUIET_LONG_VALUE */
    public static final String QUIET_LONG_VALUE = QUIET.getLongOpt();

    /** VERSION_LONG_VALUE */
    public static final String VERSION_LONG_VALUE = VERSION.getLongOpt();

    /** FSW_DOWNLINK_HOST_LONG_VALUE */
    public static final String FSW_DOWNLINK_HOST_LONG_VALUE = FSW_DOWNLINK_HOST.getLongOpt();

    /** FSW_UPLINK_HOST_LONG_VALUE */
    public static final String FSW_UPLINK_HOST_LONG_VALUE = FSW_UPLINK_HOST.getLongOpt();

    /** FSWUPLINKPORT_LONG_VALUE */
    public static final String FSWUPLINKPORT_LONG_VALUE = FSW_UPLINK_PORT.getLongOpt();

    /** FSWDOWNLINKPORT_LONG_VALUE */
    public static final String FSWDOWNLINKPORT_LONG_VALUE = FSW_DOWNLINK_PORT.getLongOpt();

    /** FSWVERSION_LONG_VALUE */
    public static final String FSWVERSION_LONG_VALUE = FSW_VERSION.getLongOpt();

    /** DOWNLINKSTREAM_LONG_VALUE */
    public static final String DOWNLINKSTREAM_LONG_VALUE = DOWNLINK_STREAM_ID.getLongOpt();

    /** FSW_DICTIONARYDIR_LONG_VALUE */
    public static final String FSW_DICTIONARYDIR_LONG_VALUE = FSW_DICTIONARY_DIRECTORY.getLongOpt();

    /** TESTBEDNAME_LONG_VALUE */
    public static final String TESTBEDNAME_LONG_VALUE = TESTBED_NAME.getLongOpt();

    /** NOGUI_LONG_VALUE */
    public static final String NOGUI_LONG_VALUE = NO_GUI.getLongOpt();

    /** NODATABASE_LONG_VALUE */
    public static final String NODATABASE_LONG_VALUE = NO_DATABASE.getLongOpt();

    /** NOJMS_LONG_VALUE */
    public static final String NOJMS_LONG_VALUE = NO_JMS.getLongOpt();

    /** TESTKEY_LONG_VALUE */
    public static final String TESTKEY_LONG_VALUE = SESSION_KEY.getLongOpt();

    /** TESTDESCRIPTION_LONG_VALUE */
    public static final String TESTDESCRIPTION_LONG_VALUE = SESSION_DESCRIPTION.getLongOpt();
    
    /** DB_SESSIONKEY_LONG_VALUE */
    public static final String DB_SESSIONKEY_LONG_VALUE = DB_SESSION_KEY.getLongOpt();

    /** TESTNAME_LONG_VALUE */
    public static final String TESTNAME_LONG_VALUE = SESSION_NAME.getLongOpt();

    /** TESTCONFIG_LONG_VALUE */
    public static final String TESTCONFIG_LONG_VALUE = SESSION_CONFIGURATION.getLongOpt();

    /** TESTHOST_LONG_VALUE */
    public static final String TESTHOST_LONG_VALUE = SESSION_HOST.getLongOpt();
    
    /** DB_SESSIONHOST_LONG_VALUE */
    public static final String DB_SESSIONHOST_LONG_VALUE = DB_SESSION_HOST.getLongOpt();

    /** TESTUSER_LONG_VALUE */
    public static final String TESTUSER_LONG_VALUE = SESSION_USER.getLongOpt();

    /** TESTTYPE_LONG_VALUE */
    public static final String TESTTYPE_LONG_VALUE = SESSION_TYPE.getLongOpt();

    /** OUTPUTDIR_LONG_VALUE */
    public static final String OUTPUTDIR_LONG_VALUE = OUTPUT_DIRECTORY.getLongOpt();

    /** SPACECRAFTID_LONG_VALUE */
    public static final String SPACECRAFTID_LONG_VALUE = SPACECRAFT_ID.getLongOpt();

    /** SSE_DICTIONARYDIR_LONG_VALUE */
    public static final String SSE_DICTIONARYDIR_LONG_VALUE = SSE_DICTIONARY_DIRECTORY.getLongOpt();

    /** APPCONFIG_LONG_VALUE */
    public static final String APPCONFIG_LONG_VALUE = APPLICATION_CONFIGURATION.getLongOpt();

    /** VENUETYPE_LONG_VALUE */
    public static final String VENUETYPE_LONG_VALUE = VENUE_TYPE.getLongOpt();

    /** SSEVERSION_LONG_VALUE */
    public static final String SSEVERSION_LONG_VALUE = SSE_VERSION.getLongOpt();

    /** SSEHOST_LONG_VALUE */
    public static final String SSEHOST_LONG_VALUE = SSE_HOST.getLongOpt();

    /** SSEUPLINKPORT_LONG_VALUE */
    public static final String SSEUPLINKPORT_LONG_VALUE = SSE_UPLINK_PORT.getLongOpt();

    /** SSEDOWNLINKPORT_LONG_VALUE */
    public static final String SSEDOWNLINKPORT_LONG_VALUE = SSE_DOWNLINK_PORT.getLongOpt();

    /** JMSHOST_LONG_VALUE */
    public static final String JMSHOST_LONG_VALUE = JMS_HOST.getLongOpt();

    /** JMSPORT_LONG_VALUE */
    public static final String JMSPORT_LONG_VALUE = JMS_PORT.getLongOpt();

    /** JMSSUBTOPIC_LONG_VALUE */
    public static final String JMSSUBTOPIC_LONG_VALUE = JMS_SUBTOPIC.getLongOpt();

    /** DATABASE_HOST_LONG_VALUE */
    public static final String DATABASE_HOST_LONG_VALUE = DATABASE_HOST.getLongOpt();

    /** DATABASE_PORT_LONG_VALUE */
    public static final String DATABASE_PORT_LONG_VALUE = DATABASE_PORT.getLongOpt();


    /** USER_ROLE_LONG_VALUE */
    public static final String USER_ROLE_LONG_VALUE = USER_ROLE.getLongOpt();
    
    /** LOGIN METHOD FOR GUI APPS */
    public static final String LOGIN_METHOD_GUI_LONG_VALUE =  LOGIN_METHOD_GUI.getLongOpt();
     
    /** LOGIN METHOD FOR GUI APPS */
    public static final String LOGIN_METHOD_NON_GUI_LONG_VALUE =  LOGIN_METHOD_NON_GUI.getLongOpt();
   
    // 7/31/13 - MPCS-4992: added username option for keytab
    /** KEYTAB_FILE_LONG_VALUE */
    public static final String KEYTAB_FILE_LONG_VALUE = KEYTAB_FILE.getLongOpt();
    
    /** USERNAME_LONG_VALUE */
    public static final String USERNAME_LONG_VALUE = USERNAME.getLongOpt();


    /** SESSION_VCID_LONG_VALUE */
    public static final String SESSION_VCID_LONG_VALUE = SESSION_VCID.getLongOpt();

    /** SESSION_DSSID_LONG_VALUE */
    public static final String SESSION_DSSID_LONG_VALUE = SESSION_DSSID.getLongOpt();
    
    /** INTEGRATED_CHILL_LONG_VALUE */
    public static final String INTEGRATED_CHILL_LONG_VALUE = INTEGRATED_CHILL.getLongOpt();
    
    /** INPUT_FORMAT_LONG_VALUE */
    public static final String INPUT_FORMAT_LONG_VALUE = INPUT_FORMAT.getLongOpt();
    
    /** INPUT_FILE_LONG_VALUE */
    public static final String INPUT_FILE_LONG_VALUE = INPUT_FILE.getLongOpt();
    
    // MPCS-7766 12/10/15 - Adding option to enable BufferedRawInputStream
    /** BUFFERED_INPUT_STREAM_LONG_VALUE*/
    public static final String BUFFERED_INPUT_STREAM_LONG_VALUE = BUFFERED_INPUT_STREAM.getLongOpt();
    
    /** AUTORUN_DESCRIPTION */
    public static final String AUTORUN_DESCRIPTION = AUTORUN.getDescription();

    /** CONNECTION_TYPE_DESCRIPTION */
    public static final String CONNECTION_TYPE_DESCRIPTION = CONNECTION_TYPE.getDescription();

    /** UPLINK_CONNECTION_TYPE_DESCRIPTION */
    public static final String UPLINK_CONNECTION_TYPE_DESCRIPTION = UPLINK_CONNECTION_TYPE.getDescription();

    /** DEBUG_DESCRIPTION */
    public static final String DEBUG_DESCRIPTION = DEBUG.getDescription();

    /** GUI_DESCRIPTION */
    public static final String GUI_DESCRIPTION = GUI.getDescription();

    /** HELP_DESCRIPTION */
    public static final String HELP_DESCRIPTION = HELP.getDescription();

    /** QUIET_DESCRIPTION */
    public static final String QUIET_DESCRIPTION = QUIET.getDescription();

    /** VERSION_DESCRIPTION */
    public static final String VERSION_DESCRIPTION = VERSION.getDescription();

    /** FSW_DOWNLINK_HOST_DESCRIPTION */
    public static final String FSW_DOWNLINK_HOST_DESCRIPTION = FSW_DOWNLINK_HOST.getDescription();

    /** FSW_UPLINK_HOST_DESCRIPTION */
    public static final String FSW_UPLINK_HOST_DESCRIPTION = FSW_UPLINK_HOST.getDescription();

    /** FSWUPLINKPORT_DESCRIPTION */
    public static final String FSWUPLINKPORT_DESCRIPTION = FSW_UPLINK_PORT.getDescription();

    /** FSWDOWNLINKPORT_DESCRIPTION */
    public static final String FSWDOWNLINKPORT_DESCRIPTION = FSW_DOWNLINK_PORT.getDescription();

    /** FSWVERSION_DESCRIPTION */
    public static final String FSWVERSION_DESCRIPTION = FSW_VERSION.getDescription();

    /** DOWNLINKSTREAM_DESCRIPTION */
    public static final String DOWNLINKSTREAM_DESCRIPTION = DOWNLINK_STREAM_ID.getDescription();

    /** FSW_DICTIONARYDIR_DESCRIPTION */
    public static final String FSW_DICTIONARYDIR_DESCRIPTION = FSW_DICTIONARY_DIRECTORY.getDescription();

    /** TESTBEDNAME_DESCRIPTION */
    public static final String TESTBEDNAME_DESCRIPTION = TESTBED_NAME.getDescription();

    /** NOGUI_DESCRIPTION */
    public static final String NOGUI_DESCRIPTION = NO_GUI.getDescription();

    /** NODATABASE_DESCRIPTION */
    public static final String NODATABASE_DESCRIPTION = NO_DATABASE.getDescription();

    /** NOJMS_DESCRIPTION */
    public static final String NOJMS_DESCRIPTION = NO_JMS.getDescription();

    /** TESTKEY_DESCRIPTION */
    public static final String TESTKEY_DESCRIPTION = SESSION_KEY.getDescription();
    
    /** DB_SESSIONKEY_DESCRIPTION */
    public static final String DB_SESSIONKEY_DESCRIPTION = DB_SESSION_KEY.getDescription();

    /** TESTDESCRIPTION_DESCRIPTION */
    public static final String TESTDESCRIPTION_DESCRIPTION = SESSION_DESCRIPTION.getDescription();

    /** TESTNAME_DESCRIPTION */
    public static final String TESTNAME_DESCRIPTION = SESSION_NAME.getDescription();

    /** TESTCONFIG_DESCRIPTION */
    public static final String TESTCONFIG_DESCRIPTION = SESSION_CONFIGURATION.getDescription();

    /** TESTHOST_DESCRIPTION */
    public static final String TESTHOST_DESCRIPTION = SESSION_HOST.getDescription();
    
    /** DB_SESSIONHOST_DESCRIPTION */
    public static final String DB_SESSIONHOST_DESCRIPTION = DB_SESSION_HOST.getDescription();

    /** TESTUSER_DESCRIPTION */
    public static final String TESTUSER_DESCRIPTION = SESSION_USER.getDescription();

    /** TESTTYPE_DESCRIPTION */
    public static final String TESTTYPE_DESCRIPTION = SESSION_TYPE.getDescription();

    /** OUTPUTDIR_DESCRIPTION */
    public static final String OUTPUTDIR_DESCRIPTION = OUTPUT_DIRECTORY.getDescription();

    /** SPACECRAFTID_DESCRIPTION */
    public static final String SPACECRAFTID_DESCRIPTION = SPACECRAFT_ID.getDescription();

    /** SSE_DICTIONARYDIR_DESCRIPTION */
    public static final String SSE_DICTIONARYDIR_DESCRIPTION = SSE_DICTIONARY_DIRECTORY.getDescription();

    /** APPCONFIG_DESCRIPTION */
    public static final String APPCONFIG_DESCRIPTION = APPLICATION_CONFIGURATION.getDescription();

    /** VENUETYPE_DESCRIPTION */
    public static final String VENUETYPE_DESCRIPTION = VENUE_TYPE.getDescription();

    /** SSEVERSION_DESCRIPTION */
    public static final String SSEVERSION_DESCRIPTION = SSE_VERSION.getDescription();

    /** SSEHOST_DESCRIPTION */
    public static final String SSEHOST_DESCRIPTION = SSE_HOST.getDescription();

    /** SSEUPLINKPORT_DESCRIPTION */
    public static final String SSEUPLINKPORT_DESCRIPTION = SSE_UPLINK_PORT.getDescription();

    /** SSEDOWNLINKPORT_DESCRIPTION */
    public static final String SSEDOWNLINKPORT_DESCRIPTION = SSE_DOWNLINK_PORT.getDescription();

    /** JMSHOST_DESCRIPTION */
    public static final String JMSHOST_DESCRIPTION = JMS_HOST.getDescription();

    /** JMSPORT_DESCRIPTION */
    public static final String JMSPORT_DESCRIPTION = JMS_PORT.getDescription();

    /** JMSSUBTOPIC_DESCRIPTION */
    public static final String JMSSUBTOPIC_DESCRIPTION = JMS_SUBTOPIC.getDescription();

    /** DATABASE_HOST_DESCRIPTION */
    public static final String DATABASE_HOST_DESCRIPTION = DATABASE_HOST.getDescription();

    /** DATABASE_PORT_DESCRIPTION */
    public static final String DATABASE_PORT_DESCRIPTION = DATABASE_PORT.getDescription();
    

    /** SESSION_VCID_DESCRIPTIO */
    public static final String SESSION_VCID_DESCRIPTION = SESSION_VCID.getDescription();

    /** SESSION_DSSID_DESCRIPTION */
    public static final String SESSION_DSSID_DESCRIPTION = SESSION_DSSID.getDescription();
    
    /** INTEGRATED_CHILL_DESCRIPTION */
    public static final String INTEGRATED_CHILL_DESCRIPTION = INTEGRATED_CHILL.getDescription();
   
    // 8/5/13 - Added for MPCS-5109.
    /** INPUT_FORMAT_DESCRIPTION */
    public static final String INPUT_FORMAT_DESCRIPTION = INPUT_FORMAT.getDescription();
    
    /** INPUT_FILE_DESCRIPTION */
    public static final String INPUT_FILE_DESCRIPTION = INPUT_FILE.getDescription();
    
    // MPCS-7766 12/10/15 - Adding option to enable BufferedRawInputStream
    /**BUFFERED_INPUT_STREAM_DESCRIPTION*/
    public static final String BUFFERED_INPUT_STREAM_DESCRIPTION = BUFFERED_INPUT_STREAM.getDescription();
    
    /** AUTORUN_ARGNAME */
    public static final String AUTORUN_ARGNAME = AUTORUN.getArgName();

    /** CONNECTION_TYPE_ARGNAME */
    public static final String CONNECTION_TYPE_ARGNAME = CONNECTION_TYPE.getArgName();

    /** UPLINK_CONNECTION_TYPE_ARGNAME */
    public static final String UPLINK_CONNECTION_TYPE_ARGNAME = UPLINK_CONNECTION_TYPE.getArgName();

    /** DEBUG_ARGNAME */
    public static final String DEBUG_ARGNAME = DEBUG.getArgName();

    /** GUI_ARGNAME */
    public static final String GUI_ARGNAME = GUI.getArgName();

    /** HELP_ARGNAME */
    public static final String HELP_ARGNAME = HELP.getArgName();

    /** QUIET_ARGNAME */
    public static final String QUIET_ARGNAME = QUIET.getArgName();

    /** VERSION_ARGNAME */
    public static final String VERSION_ARGNAME = VERSION.getArgName();

    /** FSW_DOWNLINK_HOST_ARGNAME */
    public static final String FSW_DOWNLINK_HOST_ARGNAME = FSW_DOWNLINK_HOST.getArgName();

    /** FSW_UPLINK_HOST_ARGNAME */
    public static final String FSW_UPLINK_HOST_ARGNAME = FSW_UPLINK_HOST.getArgName();

    /** FSWUPLINKPORT_ARGNAME */
    public static final String FSWUPLINKPORT_ARGNAME = FSW_UPLINK_PORT.getArgName();

    /** FSWDOWNLINKPORT_ARGNAME */
    public static final String FSWDOWNLINKPORT_ARGNAME = FSW_DOWNLINK_PORT.getArgName();

    /** FSWVERSION_ARGNAME */
    public static final String FSWVERSION_ARGNAME = FSW_VERSION.getArgName();

    /** DOWNLINKSTREAM_ARGNAME */
    public static final String DOWNLINKSTREAM_ARGNAME = DOWNLINK_STREAM_ID.getArgName();

    /** DICTIONARYDIR_ARGNAME */
    public static final String DICTIONARYDIR_ARGNAME = FSW_DICTIONARY_DIRECTORY.getArgName();

    /** TESTBEDNAME_ARGNAME */
    public static final String TESTBEDNAME_ARGNAME = TESTBED_NAME.getArgName();

    /** NOGUI_ARGNAME */
    public static final String NOGUI_ARGNAME = NO_GUI.getArgName();

    /** NODATABASE_ARGNAME */
    public static final String NODATABASE_ARGNAME = NO_DATABASE.getArgName();

    /** NOJMS_ARGNAME */
    public static final String NOJMS_ARGNAME = NO_JMS.getArgName();

    /** TESTKEY_ARGNAME */
    public static final String TESTKEY_ARGNAME = SESSION_KEY.getArgName();

    /** TESTDESCRIPTION_ARGNAME */
    public static final String TESTDESCRIPTION_ARGNAME = SESSION_DESCRIPTION.getArgName();

    /** TESTNAME_ARGNAME */
    public static final String TESTNAME_ARGNAME = SESSION_NAME.getArgName();

    /** TESTCONFIG_ARGNAME */
    public static final String TESTCONFIG_ARGNAME = SESSION_CONFIGURATION.getArgName();

    /** TESTHOST_ARGNAME */
    public static final String TESTHOST_ARGNAME = SESSION_HOST.getArgName();

    /** TESTUSER_ARGNAME */
    public static final String TESTUSER_ARGNAME = SESSION_USER.getArgName();

    /** TESTTYPE_ARGNAME */
    public static final String TESTTYPE_ARGNAME = SESSION_TYPE.getArgName();

    /** OUTPUTDIR_ARGNAME */
    public static final String OUTPUTDIR_ARGNAME = OUTPUT_DIRECTORY.getArgName();

    /** SPACECRAFTID_ARGNAME */
    public static final String SPACECRAFTID_ARGNAME = SPACECRAFT_ID.getArgName();

    /** APPCONFIG_ARGNAME */
    public static final String APPCONFIG_ARGNAME = APPLICATION_CONFIGURATION.getArgName();

    /** VENUETYPE_ARGNAME */
    public static final String VENUETYPE_ARGNAME = VENUE_TYPE.getArgName();

    /** SSEVERSION_ARGNAME */
    public static final String SSEVERSION_ARGNAME = SSE_VERSION.getArgName();

    /** SSEHOST_ARGNAME */
    public static final String SSEHOST_ARGNAME = SSE_HOST.getArgName();

    /** SSEUPLINKPORT_ARGNAME */
    public static final String SSEUPLINKPORT_ARGNAME = SSE_UPLINK_PORT.getArgName();

    /** SSEDOWNLINKPORT_ARGNAME */
    public static final String SSEDOWNLINKPORT_ARGNAME = SSE_DOWNLINK_PORT.getArgName();

    /** JMSHOST_ARGNAME */
    public static final String JMSHOST_ARGNAME = JMS_HOST.getArgName();

    /** JMSPORT_ARGNAME */
    public static final String JMSPORT_ARGNAME = JMS_PORT.getArgName();

    /** JMSSUBTOPIC_ARGNAME */
    public static final String JMSSUBTOPIC_ARGNAME = JMS_SUBTOPIC.getArgName();

    /** DATABASE_HOST_ARGNAME */
    public static final String DATABASE_HOST_ARGNAME = DATABASE_HOST.getArgName();

    /** DATABASE_PORT_ARGNAME */
    public static final String DATABASE_PORT_ARGNAME = DATABASE_PORT.getArgName();
    

    /** SESSION_VCID_ARGNAME */
    public static final String SESSION_VCID_ARGNAME = SESSION_VCID.getArgName();

    /** SESSION_DSSID_ARGNAM */
    public static final String SESSION_DSSID_ARGNAME = SESSION_DSSID.getArgName();
    
    /** INTEGRATED_CHILL_ARGNAM */
    public static final String INTEGRATED_CHILL_ARGNAME = INTEGRATED_CHILL.getArgName();

    // 8/5/13 - Added for MPCS-5109.
    /** INPUT_FORMAT_ARGNAME */
    public static final String INPUT_FORMAT_ARGNAME = INPUT_FORMAT.getArgName();
    
    /** INPUT_FILE_ARGNAME */
    public static final String INPUT_FILE_ARGNAME = INPUT_FILE.getArgName();
    
    // MPCS-7766 12/10/15 - Adding option to enable BufferedRawInputStream
    /**BUFFERED_INPUT_STREAM_ARGNAME*/
    public static final String BUFFERED_INPUT_STREAM_ARGNAME = BUFFERED_INPUT_STREAM.getArgName();
    
    // 6-9-2015 Added for MPCS-7336
    // Anything in this list is allowed only once on the command line.
    public static final String[] INPUT_LIMIT_ARGNAMES = {"--"+LOGIN_METHOD_NON_GUI.getLongOpt()};
    
    // the internal data structures for storing the relationships
    // (keep these static so the class doesn't have to be instantiated)
    private static Map<String,Option> shortToOptions;
    private static Map<String,Option> longToOptions;
    private static Map<String,String> shortToLongs;
    private static Map<String,String> longToShorts;
    private static Map<String,String> shortToDescriptions;
    private static Map<String,Boolean> shortToHasArgs;
    private static Map<String,String> shortToArgNames;

    //Fill up all the hashtables with values the first time that this class is accessed
    //(this is necessary to allow the methods that use these hashtables to be static)
    static
    {
        // build the short-to-long mapping

        shortToOptions = new HashMap<String,Option>(64);
        longToOptions = new HashMap<String,Option>(64);
        shortToLongs = new HashMap<String,String>(64);
        longToShorts = new HashMap<String,String>(64);
        shortToDescriptions = new HashMap<String,String>(64);
        shortToHasArgs = new HashMap<String,Boolean>(64);
        shortToArgNames = new HashMap<String,String>(64);

        shortToOptions.put(AUTORUN_SHORT_VALUE,AUTORUN);
        shortToOptions.put(CONNECTION_TYPE_SHORT_VALUE,CONNECTION_TYPE);
        shortToOptions.put(UPLINK_CONNECTION_TYPE_SHORT_VALUE, UPLINK_CONNECTION_TYPE);
        shortToOptions.put(DEBUG_SHORT_VALUE,DEBUG);
        shortToOptions.put(GUI_SHORT_VALUE,GUI);
        shortToOptions.put(HELP_SHORT_VALUE,HELP);
        shortToOptions.put(QUIET_SHORT_VALUE,QUIET);
        shortToOptions.put(VERSION_SHORT_VALUE,VERSION);
        shortToOptions.put(FSW_DOWNLINK_HOST_SHORT_VALUE,FSW_DOWNLINK_HOST);
        shortToOptions.put(FSWUPLINKPORT_SHORT_VALUE,FSW_UPLINK_PORT);
        shortToOptions.put(FSWDOWNLINKPORT_SHORT_VALUE,FSW_DOWNLINK_PORT);
        shortToOptions.put(FSWVERSION_SHORT_VALUE,FSW_VERSION);
        shortToOptions.put(DOWNLINKSTREAM_SHORT_VALUE,DOWNLINK_STREAM_ID);
        shortToOptions.put(FSW_DICTIONARYDIR_SHORT_VALUE,FSW_DICTIONARY_DIRECTORY);
        shortToOptions.put(TESTBEDNAME_SHORT_VALUE,TESTBED_NAME);
        shortToOptions.put(NOGUI_SHORT_VALUE,NO_GUI);
        shortToOptions.put(NODATABASE_SHORT_VALUE,NO_DATABASE);
        shortToOptions.put(NOJMS_SHORT_VALUE,NO_JMS);
        shortToOptions.put(TESTKEY_SHORT_VALUE,SESSION_KEY);
        shortToOptions.put(TESTDESCRIPTION_SHORT_VALUE,SESSION_DESCRIPTION);
        shortToOptions.put(TESTNAME_SHORT_VALUE,SESSION_NAME);
        shortToOptions.put(TESTCONFIG_SHORT_VALUE,SESSION_CONFIGURATION);
        shortToOptions.put(TESTHOST_SHORT_VALUE,SESSION_HOST);
        shortToOptions.put(TESTUSER_SHORT_VALUE,SESSION_USER);
        shortToOptions.put(TESTTYPE_SHORT_VALUE,SESSION_TYPE);
        shortToOptions.put(OUTPUTDIR_SHORT_VALUE,OUTPUT_DIRECTORY);
        shortToOptions.put(SPACECRAFTID_SHORT_VALUE,SPACECRAFT_ID);
        shortToOptions.put(SSE_DICTIONARYDIR_SHORT_VALUE,SSE_DICTIONARY_DIRECTORY);
        shortToOptions.put(APPCONFIG_SHORT_VALUE,APPLICATION_CONFIGURATION);
        shortToOptions.put(VENUETYPE_SHORT_VALUE,VENUE_TYPE);
        shortToOptions.put(SSEVERSION_SHORT_VALUE,SSE_VERSION);
        shortToOptions.put(SSEHOST_SHORT_VALUE,SSE_HOST);
        shortToOptions.put(SSEUPLINKPORT_SHORT_VALUE,SSE_UPLINK_PORT);
        shortToOptions.put(SSEDOWNLINKPORT_SHORT_VALUE,SSE_DOWNLINK_PORT);
        shortToOptions.put(DATABASE_HOST_SHORT_VALUE,DATABASE_HOST);
        shortToOptions.put(DATABASE_PORT_SHORT_VALUE,DATABASE_PORT);
        // 8/5/13 - Added for MPCS-5109.
        shortToOptions.put(INPUT_FORMAT_SHORT_VALUE,INPUT_FORMAT);
        shortToOptions.put(INPUT_FILE_SHORT_VALUE,INPUT_FILE);

        longToOptions.put(AUTORUN_LONG_VALUE,AUTORUN);
        longToOptions.put(CONNECTION_TYPE_LONG_VALUE,CONNECTION_TYPE);
        longToOptions.put(UPLINK_CONNECTION_TYPE_LONG_VALUE, UPLINK_CONNECTION_TYPE);
        longToOptions.put(DEBUG_LONG_VALUE,DEBUG);
        longToOptions.put(GUI_LONG_VALUE,GUI);
        longToOptions.put(HELP_LONG_VALUE,HELP);
        longToOptions.put(QUIET_LONG_VALUE,QUIET);
        longToOptions.put(VERSION_LONG_VALUE,VERSION);
        longToOptions.put(FSW_DOWNLINK_HOST_LONG_VALUE,FSW_DOWNLINK_HOST);
        longToOptions.put(FSW_UPLINK_HOST_LONG_VALUE,FSW_UPLINK_HOST);
        longToOptions.put(FSWUPLINKPORT_LONG_VALUE,FSW_UPLINK_PORT);
        longToOptions.put(FSWDOWNLINKPORT_LONG_VALUE,FSW_DOWNLINK_PORT);
        longToOptions.put(FSWVERSION_LONG_VALUE,FSW_VERSION);
        longToOptions.put(DOWNLINKSTREAM_LONG_VALUE,DOWNLINK_STREAM_ID);
        longToOptions.put(FSW_DICTIONARYDIR_LONG_VALUE,FSW_DICTIONARY_DIRECTORY);
        longToOptions.put(TESTBEDNAME_LONG_VALUE,TESTBED_NAME);
        longToOptions.put(NOGUI_LONG_VALUE,NO_GUI);
        longToOptions.put(NODATABASE_LONG_VALUE,NO_DATABASE);
        longToOptions.put(NOJMS_LONG_VALUE,NO_JMS);
        longToOptions.put(TESTKEY_LONG_VALUE,SESSION_KEY);
        longToOptions.put(TESTDESCRIPTION_LONG_VALUE,SESSION_DESCRIPTION);
        longToOptions.put(TESTNAME_LONG_VALUE,SESSION_NAME);
        longToOptions.put(TESTCONFIG_LONG_VALUE,SESSION_CONFIGURATION);
        longToOptions.put(TESTHOST_LONG_VALUE,SESSION_HOST);
        longToOptions.put(TESTUSER_LONG_VALUE,SESSION_USER);
        longToOptions.put(TESTTYPE_LONG_VALUE,SESSION_TYPE);
        longToOptions.put(OUTPUTDIR_LONG_VALUE,OUTPUT_DIRECTORY);
        longToOptions.put(SPACECRAFTID_LONG_VALUE,SPACECRAFT_ID);
        longToOptions.put(SSE_DICTIONARYDIR_LONG_VALUE,SSE_DICTIONARY_DIRECTORY);
        longToOptions.put(APPCONFIG_LONG_VALUE,APPLICATION_CONFIGURATION);
        longToOptions.put(VENUETYPE_LONG_VALUE,VENUE_TYPE);
        longToOptions.put(SSEVERSION_LONG_VALUE,SSE_VERSION);
        longToOptions.put(SSEHOST_LONG_VALUE,SSE_HOST);
        longToOptions.put(SSEUPLINKPORT_LONG_VALUE,SSE_UPLINK_PORT);
        longToOptions.put(SSEDOWNLINKPORT_LONG_VALUE,SSE_DOWNLINK_PORT);
        longToOptions.put(JMSHOST_LONG_VALUE,JMS_HOST);
        longToOptions.put(JMSPORT_LONG_VALUE,JMS_PORT);
        longToOptions.put(JMSSUBTOPIC_LONG_VALUE,JMS_SUBTOPIC);
        longToOptions.put(DATABASE_HOST_LONG_VALUE,DATABASE_HOST);
        longToOptions.put(DATABASE_PORT_LONG_VALUE,DATABASE_PORT);
        longToOptions.put(SESSION_VCID_LONG_VALUE,SESSION_VCID);
        longToOptions.put(SESSION_DSSID_LONG_VALUE,SESSION_DSSID);
        longToOptions.put(INTEGRATED_CHILL_LONG_VALUE,INTEGRATED_CHILL);
        longToOptions.put(DB_SESSIONHOST_LONG_VALUE, DB_SESSION_HOST);
        longToOptions.put(DB_SESSIONKEY_LONG_VALUE, DB_SESSION_KEY);
 
        longToOptions.put(USER_ROLE_LONG_VALUE,  USER_ROLE);
        longToOptions.put(LOGIN_METHOD_GUI_LONG_VALUE, LOGIN_METHOD_GUI);
        longToOptions.put(LOGIN_METHOD_NON_GUI_LONG_VALUE, LOGIN_METHOD_NON_GUI);
        // 7/22/13 - MPCS-4992: added username option for keytab
        longToOptions.put(KEYTAB_FILE_LONG_VALUE, KEYTAB_FILE);
        longToOptions.put(USERNAME_LONG_VALUE, USERNAME);
        // 8/5/13 - Added for MPCS-5109.
        longToOptions.put(INPUT_FORMAT_LONG_VALUE, INPUT_FORMAT);
        longToOptions.put(INPUT_FILE_LONG_VALUE, INPUT_FILE);
        
        // MPCS-7766 12/10/2015 - Adding option to enable BufferedRawInputStream
        longToOptions.put(BUFFERED_INPUT_STREAM_LONG_VALUE, BUFFERED_INPUT_STREAM);
        
        shortToLongs.put( AUTORUN_SHORT_VALUE, AUTORUN_LONG_VALUE );
        shortToLongs.put( CONNECTION_TYPE_SHORT_VALUE, CONNECTION_TYPE_LONG_VALUE );
        shortToLongs.put( UPLINK_CONNECTION_TYPE_SHORT_VALUE, UPLINK_CONNECTION_TYPE_LONG_VALUE );
        shortToLongs.put( DEBUG_SHORT_VALUE, DEBUG_LONG_VALUE );
        shortToLongs.put( GUI_SHORT_VALUE, GUI_LONG_VALUE );
        shortToLongs.put( HELP_SHORT_VALUE, HELP_LONG_VALUE );
        shortToLongs.put( QUIET_SHORT_VALUE, QUIET_LONG_VALUE );
        shortToLongs.put( VERSION_SHORT_VALUE, VERSION_LONG_VALUE );
        shortToLongs.put( FSW_DOWNLINK_HOST_SHORT_VALUE, FSW_DOWNLINK_HOST_LONG_VALUE );
        shortToLongs.put( FSWUPLINKPORT_SHORT_VALUE, FSWUPLINKPORT_LONG_VALUE );
        shortToLongs.put( FSWDOWNLINKPORT_SHORT_VALUE, FSWDOWNLINKPORT_LONG_VALUE );
        shortToLongs.put( FSWVERSION_SHORT_VALUE, FSWVERSION_LONG_VALUE );
        shortToLongs.put( DOWNLINKSTREAM_SHORT_VALUE, DOWNLINKSTREAM_LONG_VALUE );
        shortToLongs.put( FSW_DICTIONARYDIR_SHORT_VALUE, FSW_DICTIONARYDIR_LONG_VALUE );
        shortToLongs.put( TESTBEDNAME_SHORT_VALUE, TESTBEDNAME_LONG_VALUE );
        shortToLongs.put( NOGUI_SHORT_VALUE, NOGUI_LONG_VALUE );
        shortToLongs.put( NODATABASE_SHORT_VALUE, NODATABASE_LONG_VALUE );
        shortToLongs.put( NOJMS_SHORT_VALUE, NOJMS_LONG_VALUE );
        shortToLongs.put( TESTKEY_SHORT_VALUE, TESTKEY_LONG_VALUE);
        shortToLongs.put( TESTDESCRIPTION_SHORT_VALUE, TESTDESCRIPTION_LONG_VALUE );
        shortToLongs.put( TESTNAME_SHORT_VALUE, TESTNAME_LONG_VALUE );
        shortToLongs.put( TESTCONFIG_SHORT_VALUE, TESTCONFIG_LONG_VALUE );
        shortToLongs.put( TESTHOST_SHORT_VALUE, TESTHOST_LONG_VALUE );
        shortToLongs.put( TESTUSER_SHORT_VALUE, TESTUSER_LONG_VALUE );
        shortToLongs.put( TESTTYPE_SHORT_VALUE, TESTTYPE_LONG_VALUE );
        shortToLongs.put( OUTPUTDIR_SHORT_VALUE, OUTPUTDIR_LONG_VALUE );
        shortToLongs.put( SPACECRAFTID_SHORT_VALUE, SPACECRAFTID_LONG_VALUE );
        shortToLongs.put( SSE_DICTIONARYDIR_SHORT_VALUE, SSE_DICTIONARYDIR_LONG_VALUE );
        shortToLongs.put( APPCONFIG_SHORT_VALUE, APPCONFIG_LONG_VALUE );
        shortToLongs.put( VENUETYPE_SHORT_VALUE, VENUETYPE_LONG_VALUE );
        shortToLongs.put( SSEVERSION_SHORT_VALUE, SSEVERSION_LONG_VALUE );
        shortToLongs.put( SSEHOST_SHORT_VALUE, SSEHOST_LONG_VALUE );
        shortToLongs.put( SSEUPLINKPORT_SHORT_VALUE, SSEUPLINKPORT_LONG_VALUE );
        shortToLongs.put( SSEDOWNLINKPORT_SHORT_VALUE, SSEDOWNLINKPORT_LONG_VALUE );
        shortToLongs.put( DATABASE_HOST_SHORT_VALUE, DATABASE_HOST_LONG_VALUE );
        shortToLongs.put( DATABASE_PORT_SHORT_VALUE, DATABASE_PORT_LONG_VALUE );
        // 8/5/13 - Added for MPCS-5109.
        shortToLongs.put( INPUT_FORMAT_SHORT_VALUE, INPUT_FORMAT_LONG_VALUE );
        shortToLongs.put( INPUT_FILE_SHORT_VALUE, INPUT_FILE_LONG_VALUE );

        longToShorts.put( AUTORUN_LONG_VALUE, AUTORUN_SHORT_VALUE );
        longToShorts.put( CONNECTION_TYPE_LONG_VALUE, CONNECTION_TYPE_SHORT_VALUE );
        longToShorts.put( UPLINK_CONNECTION_TYPE_LONG_VALUE, UPLINK_CONNECTION_TYPE_SHORT_VALUE );
        longToShorts.put( DEBUG_LONG_VALUE, DEBUG_SHORT_VALUE );
        longToShorts.put( GUI_LONG_VALUE, GUI_SHORT_VALUE );
        longToShorts.put( HELP_LONG_VALUE, HELP_SHORT_VALUE );
        longToShorts.put( QUIET_LONG_VALUE, QUIET_SHORT_VALUE );
        longToShorts.put( VERSION_LONG_VALUE, VERSION_SHORT_VALUE );
        longToShorts.put( FSW_DOWNLINK_HOST_LONG_VALUE, FSW_DOWNLINK_HOST_SHORT_VALUE );
        longToShorts.put( FSWUPLINKPORT_LONG_VALUE, FSWUPLINKPORT_SHORT_VALUE );
        longToShorts.put( FSWDOWNLINKPORT_LONG_VALUE, FSWDOWNLINKPORT_SHORT_VALUE );
        longToShorts.put( FSWVERSION_LONG_VALUE, FSWVERSION_SHORT_VALUE );
        longToShorts.put( DOWNLINKSTREAM_LONG_VALUE, DOWNLINKSTREAM_SHORT_VALUE );
        longToShorts.put( FSW_DICTIONARYDIR_LONG_VALUE, FSW_DICTIONARYDIR_SHORT_VALUE );
        longToShorts.put( TESTBEDNAME_LONG_VALUE, TESTBEDNAME_SHORT_VALUE );
        longToShorts.put( NOGUI_LONG_VALUE, NOGUI_SHORT_VALUE );
        longToShorts.put( NODATABASE_LONG_VALUE, NODATABASE_SHORT_VALUE );
        longToShorts.put( NOJMS_LONG_VALUE, NOJMS_SHORT_VALUE );
        longToShorts.put( TESTKEY_LONG_VALUE, TESTKEY_SHORT_VALUE);
        longToShorts.put( TESTDESCRIPTION_LONG_VALUE, TESTDESCRIPTION_SHORT_VALUE );
        longToShorts.put( TESTNAME_LONG_VALUE, TESTNAME_SHORT_VALUE );
        longToShorts.put( TESTCONFIG_LONG_VALUE, TESTCONFIG_SHORT_VALUE );
        longToShorts.put( TESTHOST_LONG_VALUE, TESTHOST_SHORT_VALUE );
        longToShorts.put( TESTUSER_LONG_VALUE, TESTUSER_SHORT_VALUE );
        longToShorts.put( TESTTYPE_LONG_VALUE, TESTTYPE_SHORT_VALUE );
        longToShorts.put( OUTPUTDIR_LONG_VALUE, OUTPUTDIR_SHORT_VALUE );
        longToShorts.put( SPACECRAFTID_LONG_VALUE, SPACECRAFTID_SHORT_VALUE );
        longToShorts.put( SSE_DICTIONARYDIR_LONG_VALUE, SSE_DICTIONARYDIR_SHORT_VALUE );
        longToShorts.put( APPCONFIG_LONG_VALUE, APPCONFIG_SHORT_VALUE );
        longToShorts.put( VENUETYPE_LONG_VALUE, VENUETYPE_SHORT_VALUE );
        longToShorts.put( SSEVERSION_LONG_VALUE, SSEVERSION_SHORT_VALUE );
        longToShorts.put( SSEHOST_LONG_VALUE, SSEHOST_SHORT_VALUE );
        longToShorts.put( SSEUPLINKPORT_LONG_VALUE, SSEUPLINKPORT_SHORT_VALUE );
        longToShorts.put( SSEDOWNLINKPORT_LONG_VALUE, SSEDOWNLINKPORT_SHORT_VALUE );
        longToShorts.put( DATABASE_HOST_LONG_VALUE, DATABASE_HOST_SHORT_VALUE );
        longToShorts.put( DATABASE_PORT_LONG_VALUE, DATABASE_PORT_SHORT_VALUE );
        // 8/5/13 - Added for MPCS-5109.
        longToShorts.put( INPUT_FORMAT_LONG_VALUE, INPUT_FORMAT_SHORT_VALUE );
        longToShorts.put( INPUT_FILE_LONG_VALUE, INPUT_FILE_SHORT_VALUE );


        shortToDescriptions.put( AUTORUN_SHORT_VALUE, AUTORUN_DESCRIPTION );
        shortToDescriptions.put( CONNECTION_TYPE_SHORT_VALUE, CONNECTION_TYPE_DESCRIPTION);
        shortToDescriptions.put( UPLINK_CONNECTION_TYPE_SHORT_VALUE, UPLINK_CONNECTION_TYPE_DESCRIPTION);
        shortToDescriptions.put( DEBUG_SHORT_VALUE, DEBUG_DESCRIPTION );
        shortToDescriptions.put( GUI_SHORT_VALUE, GUI_DESCRIPTION );
        shortToDescriptions.put( HELP_SHORT_VALUE, HELP_DESCRIPTION );
        shortToDescriptions.put( QUIET_SHORT_VALUE, QUIET_DESCRIPTION );
        shortToDescriptions.put( VERSION_SHORT_VALUE, VERSION_DESCRIPTION );
        shortToDescriptions.put( FSW_DOWNLINK_HOST_SHORT_VALUE, FSW_DOWNLINK_HOST_DESCRIPTION );
        shortToDescriptions.put( FSWUPLINKPORT_SHORT_VALUE, FSWUPLINKPORT_DESCRIPTION );
        shortToDescriptions.put( FSWDOWNLINKPORT_SHORT_VALUE, FSWDOWNLINKPORT_DESCRIPTION );
        shortToDescriptions.put( FSWVERSION_SHORT_VALUE, FSWVERSION_DESCRIPTION );
        shortToDescriptions.put( DOWNLINKSTREAM_SHORT_VALUE, DOWNLINKSTREAM_DESCRIPTION );
        shortToDescriptions.put( FSW_DICTIONARYDIR_SHORT_VALUE, FSW_DICTIONARYDIR_DESCRIPTION );
        shortToDescriptions.put( TESTBEDNAME_SHORT_VALUE, TESTBEDNAME_DESCRIPTION );
        shortToDescriptions.put( NOGUI_SHORT_VALUE, NOGUI_DESCRIPTION );
        shortToDescriptions.put( NODATABASE_SHORT_VALUE, NODATABASE_DESCRIPTION );
        shortToDescriptions.put( NOJMS_SHORT_VALUE, NOJMS_DESCRIPTION );
        shortToDescriptions.put( TESTKEY_SHORT_VALUE, TESTKEY_DESCRIPTION);
        shortToDescriptions.put( TESTDESCRIPTION_SHORT_VALUE, TESTDESCRIPTION_DESCRIPTION );
        shortToDescriptions.put( TESTNAME_SHORT_VALUE, TESTNAME_DESCRIPTION );
        shortToDescriptions.put( TESTCONFIG_SHORT_VALUE, TESTCONFIG_DESCRIPTION );
        shortToDescriptions.put( TESTHOST_SHORT_VALUE, TESTHOST_DESCRIPTION );
        shortToDescriptions.put( TESTUSER_SHORT_VALUE, TESTUSER_DESCRIPTION );
        shortToDescriptions.put( TESTTYPE_SHORT_VALUE, TESTTYPE_DESCRIPTION );
        shortToDescriptions.put( OUTPUTDIR_SHORT_VALUE, OUTPUTDIR_DESCRIPTION );
        shortToDescriptions.put( SPACECRAFTID_SHORT_VALUE, SPACECRAFTID_DESCRIPTION );
        shortToDescriptions.put( SSE_DICTIONARYDIR_SHORT_VALUE, SSE_DICTIONARYDIR_DESCRIPTION );
        shortToDescriptions.put( APPCONFIG_SHORT_VALUE, APPCONFIG_DESCRIPTION );
        shortToDescriptions.put( VENUETYPE_SHORT_VALUE, VENUETYPE_DESCRIPTION );
        shortToDescriptions.put( SSEVERSION_SHORT_VALUE, SSEVERSION_DESCRIPTION );
        shortToDescriptions.put( SSEHOST_SHORT_VALUE, SSEHOST_DESCRIPTION );
        shortToDescriptions.put( SSEUPLINKPORT_SHORT_VALUE, SSEUPLINKPORT_DESCRIPTION );
        shortToDescriptions.put( SSEDOWNLINKPORT_SHORT_VALUE, SSEDOWNLINKPORT_DESCRIPTION );
        shortToDescriptions.put( DATABASE_HOST_SHORT_VALUE, DATABASE_HOST_DESCRIPTION );
        shortToDescriptions.put( DATABASE_PORT_SHORT_VALUE, DATABASE_PORT_DESCRIPTION );
        // 8/5/13 - Added for MPCS-5109.
        shortToDescriptions.put( INPUT_FORMAT_SHORT_VALUE, INPUT_FORMAT_DESCRIPTION );
        shortToDescriptions.put( INPUT_FILE_SHORT_VALUE, INPUT_FILE_DESCRIPTION );

        shortToArgNames.put( AUTORUN_SHORT_VALUE, AUTORUN_ARGNAME );
        shortToArgNames.put( CONNECTION_TYPE_SHORT_VALUE, CONNECTION_TYPE_ARGNAME );
        shortToArgNames.put( UPLINK_CONNECTION_TYPE_SHORT_VALUE, UPLINK_CONNECTION_TYPE_ARGNAME );
        shortToArgNames.put( DEBUG_SHORT_VALUE, DEBUG_ARGNAME );
        shortToArgNames.put( GUI_SHORT_VALUE, GUI_ARGNAME );
        shortToArgNames.put( HELP_SHORT_VALUE, HELP_ARGNAME );
        shortToArgNames.put( QUIET_SHORT_VALUE, QUIET_ARGNAME );
        shortToArgNames.put( VERSION_SHORT_VALUE, VERSION_ARGNAME );
        shortToArgNames.put( FSW_DOWNLINK_HOST_SHORT_VALUE, FSW_DOWNLINK_HOST_ARGNAME );
        shortToArgNames.put( FSWUPLINKPORT_SHORT_VALUE, FSWUPLINKPORT_ARGNAME );
        shortToArgNames.put( FSWDOWNLINKPORT_SHORT_VALUE, FSWDOWNLINKPORT_ARGNAME );
        shortToArgNames.put( FSWVERSION_SHORT_VALUE, FSWVERSION_ARGNAME );
        shortToArgNames.put( DOWNLINKSTREAM_SHORT_VALUE, DOWNLINKSTREAM_ARGNAME );
        shortToArgNames.put( FSW_DICTIONARYDIR_SHORT_VALUE, DICTIONARYDIR_ARGNAME );
        shortToArgNames.put( TESTBEDNAME_SHORT_VALUE, TESTBEDNAME_ARGNAME );
        shortToArgNames.put( NOGUI_SHORT_VALUE, NOGUI_ARGNAME );
        shortToArgNames.put( NODATABASE_SHORT_VALUE, NODATABASE_ARGNAME );
        shortToArgNames.put( NOJMS_SHORT_VALUE, NOJMS_ARGNAME );
        shortToArgNames.put( TESTKEY_SHORT_VALUE, TESTKEY_ARGNAME);
        shortToArgNames.put( TESTDESCRIPTION_SHORT_VALUE, TESTDESCRIPTION_ARGNAME );
        shortToArgNames.put( TESTNAME_SHORT_VALUE, TESTNAME_ARGNAME );
        shortToArgNames.put( TESTCONFIG_SHORT_VALUE, TESTCONFIG_ARGNAME );
        shortToArgNames.put( TESTHOST_SHORT_VALUE, TESTHOST_ARGNAME );
        shortToArgNames.put( TESTUSER_SHORT_VALUE, TESTUSER_ARGNAME );
        shortToArgNames.put( TESTTYPE_SHORT_VALUE, TESTTYPE_ARGNAME );
        shortToArgNames.put( OUTPUTDIR_SHORT_VALUE, OUTPUTDIR_ARGNAME );
        shortToArgNames.put( SPACECRAFTID_SHORT_VALUE, SPACECRAFTID_ARGNAME );
        shortToArgNames.put( SSE_DICTIONARYDIR_SHORT_VALUE, DICTIONARYDIR_ARGNAME );
        shortToArgNames.put( APPCONFIG_SHORT_VALUE, APPCONFIG_ARGNAME );
        shortToArgNames.put( VENUETYPE_SHORT_VALUE, VENUETYPE_ARGNAME );
        shortToArgNames.put( SSEVERSION_SHORT_VALUE, SSEVERSION_ARGNAME );
        shortToArgNames.put( SSEHOST_SHORT_VALUE, SSEHOST_ARGNAME );
        shortToArgNames.put( SSEUPLINKPORT_SHORT_VALUE, SSEUPLINKPORT_ARGNAME );
        shortToArgNames.put( SSEDOWNLINKPORT_SHORT_VALUE, SSEDOWNLINKPORT_ARGNAME );
        shortToArgNames.put( DATABASE_HOST_SHORT_VALUE, DATABASE_HOST_ARGNAME );
        shortToArgNames.put( DATABASE_PORT_SHORT_VALUE, DATABASE_PORT_ARGNAME );
        // 8/5/13 - Added for MPCS-5109.
        shortToArgNames.put( INPUT_FORMAT_SHORT_VALUE, INPUT_FORMAT_ARGNAME );
        shortToArgNames.put( INPUT_FILE_SHORT_VALUE, INPUT_FILE_ARGNAME );

        shortToHasArgs.put( AUTORUN_SHORT_VALUE, Boolean.FALSE.booleanValue());
        shortToHasArgs.put( CONNECTION_TYPE_SHORT_VALUE, Boolean.TRUE.booleanValue());
        shortToHasArgs.put( UPLINK_CONNECTION_TYPE_SHORT_VALUE, Boolean.TRUE.booleanValue());
        shortToHasArgs.put( DEBUG_SHORT_VALUE, Boolean.FALSE.booleanValue() );
        shortToHasArgs.put( GUI_SHORT_VALUE, Boolean.FALSE.booleanValue() );
        shortToHasArgs.put( HELP_SHORT_VALUE, Boolean.FALSE.booleanValue() );
        shortToHasArgs.put( QUIET_SHORT_VALUE, Boolean.FALSE.booleanValue() );
        shortToHasArgs.put( VERSION_SHORT_VALUE, Boolean.FALSE.booleanValue() );
        shortToHasArgs.put( FSW_DOWNLINK_HOST_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( FSWUPLINKPORT_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( FSWDOWNLINKPORT_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( FSWVERSION_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( DOWNLINKSTREAM_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( FSW_DICTIONARYDIR_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( TESTBEDNAME_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( NOGUI_SHORT_VALUE, Boolean.FALSE.booleanValue() );
        shortToHasArgs.put( NODATABASE_SHORT_VALUE, Boolean.FALSE.booleanValue() );
        shortToHasArgs.put( NOJMS_SHORT_VALUE, Boolean.FALSE.booleanValue() );
        shortToHasArgs.put( TESTKEY_SHORT_VALUE, Boolean.TRUE.booleanValue());
        shortToHasArgs.put( TESTDESCRIPTION_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( TESTNAME_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( TESTCONFIG_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( TESTHOST_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( TESTUSER_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( TESTTYPE_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( OUTPUTDIR_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( SPACECRAFTID_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( SSE_DICTIONARYDIR_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( APPCONFIG_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( VENUETYPE_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( SSEVERSION_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( SSEHOST_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( SSEUPLINKPORT_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( SSEDOWNLINKPORT_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( DATABASE_HOST_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( DATABASE_PORT_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        // 8/5/13 - Added for MPCS-5109.
        shortToHasArgs.put( INPUT_FORMAT_SHORT_VALUE, Boolean.TRUE.booleanValue() );
        shortToHasArgs.put( INPUT_FILE_SHORT_VALUE, Boolean.TRUE.booleanValue() );

    }
    
    /* R8 Refactor TODO - Kludge of the century. There is just no way to move forward
     * with this class using global objects and still utilize the spring context, but also no
     * way to immediately replace the usage of this class. These globals MUST now be initialized 
     * by applications using the static set methods below in order to actually parse command 
     * line options.
     */
    private static ApplicationContext appContext = null;
    private static IContextConfiguration contextConfig = null;
    private static DictionaryProperties dictConfig = null;
    private static IConnectionMap connectConfig = null;
    private static SecurityProperties securityProps = null;
    private static MissionProperties missionProps = null;
    private static ConnectionProperties connectProps = null;
    private static IDatabaseProperties         dbProperties        = null;
    private static MessageServiceConfiguration messageConfig = null;
    private static PerspectiveConfiguration perspectiveConfig = null;
    private static AccessControlParameters accessControlParams = null;
    private static IVenueConfiguration venueConfig = null;
    private static IContextIdentification contextId = null;
    private static IContextFilterInformation scFilterInfo = null;
    private static IGeneralContextInformation generalInfo = null;
    private static SseContextFlag              sseFlag             = null;
    
    /** 
     * R8 Refactor TODO - This meets needs for now. This whole class
     * should go away eventually.
     * 
     * @param inAppContext the current application context
     * 
     * MPCS-9572 - 3/31/18.  Do not pull so many beans out of the context,
     *          for performance reasons, this is now done on demand.
     */
    public static void setApplicationContext(final ApplicationContext inAppContext) {
        appContext = inAppContext;
        missionProps = inAppContext.getBean(MissionProperties.class);
        connectProps = inAppContext.getBean(ConnectionProperties.class);
        sseFlag = inAppContext.getBean(SseContextFlag.class);
        contextConfig = null;
        dictConfig = null;
        connectConfig = null;
        securityProps = null;
        dbProperties  = null;
        messageConfig = null;
        perspectiveConfig = null;
        accessControlParams = null;
        venueConfig = null;
        contextId = null;
        scFilterInfo = null;
        generalInfo = null;
        updateOptionDescriptions();
    }
    
    private static void checkAppContext() throws ParseException {
        if (appContext == null) {
            throw new ParseException("App context is null in ReservedOptions");
        }
    }
    
    private synchronized static void initMessageConfig() throws ParseException {
        if (messageConfig == null) {
            checkAppContext();         
            messageConfig = appContext.getBean(MessageServiceConfiguration.class);   
        }  
    }

    private synchronized static void initConnectConfig() throws ParseException {
        if (connectConfig == null) {
            checkAppContext();
            connectConfig = appContext.getBean(IConnectionMap.class);   
        }  
    }
    
    private synchronized static void initVenueConfig() throws ParseException {
        if (venueConfig == null) {
            checkAppContext();
            venueConfig = appContext.getBean(IVenueConfiguration.class);   
        }  
    }
    
    private synchronized static void initContextConfig() throws ParseException {
        if (contextConfig == null) {
            checkAppContext();
            contextConfig = appContext.getBean(IContextConfiguration.class); 
            contextId = appContext.getBean(IContextIdentification.class);
        }  
    }
    
    private synchronized static void initFilterInfo() throws ParseException {
        if (scFilterInfo == null) {
            checkAppContext();
            scFilterInfo = appContext.getBean(IContextFilterInformation.class);   
        }  
    }
    
    private synchronized static void initAccessControl() throws ParseException {
        if (accessControlParams == null) {
            accessControlParams = appContext.getBean(AccessControlParameters.class);  
            securityProps = appContext.getBean(SecurityProperties.class);
        }  
    }
    
    private synchronized static void initDictConfig() throws ParseException {
        if (dictConfig == null) {
            checkAppContext();
            dictConfig = appContext.getBean(DictionaryProperties.class);   
        }  
    }
    
    private synchronized static void initPerspectiveConfig() throws ParseException {
        if (perspectiveConfig == null) {
            checkAppContext();
            perspectiveConfig = appContext.getBean(PerspectiveConfiguration.class);   
        }  
    }
    
    private synchronized static void initGeneralInfo() throws ParseException {
        if (generalInfo == null) {
            checkAppContext();
            generalInfo = appContext.getBean(IGeneralContextInformation.class);   
        }  
    }
    
    private synchronized static void initDbProperties() throws ParseException {
        if (dbProperties == null) {
            checkAppContext();
            dbProperties = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class);   
        }  
    }

    private static void updateOptionDescriptions() {
    	
    	/* R8 Refactor TODO . This is truly ugly, but I cannot think of another way to
    	 * maintain the same help output without using global instances of configuration objects */
    	
    	CONNECTION_TYPE.setDescription("the input source for the session acceptable inputs: " +
                connectProps.getAllowedDownlinkConnectionTypesAsStrings(sseFlag.isApplicationSse()));
    	
    	UPLINK_CONNECTION_TYPE.setDescription("the output destination for the session acceptable inputs: " +
                connectProps.getAllowedUplinkConnectionTypesAsStrings(sseFlag.isApplicationSse()));
    	VENUE_TYPE.setDescription("operational or test venue to use: " + missionProps.getAllowedVenueTypesAsStrings());
    	
    	DATABASE_HOST.setDescription("The host that the database resides on.   This value may also be the" +
            " name of one of the testbeds (e.g. FSWTB) and the application will automatically determine the proper hostname" +
            " of the GDS machine in the specified testbed. The known testbed names are: " + Arrays.toString(getAllTestbedNames()));

    	
    }
       

    /**
     * getOption returns an instance of the command line option specified by
     * the parameter inputOption.  If inputOption refers to an option which is
     * not reserved, getOption returns null.
     *
     * @param inputOption Option name
     * @return an instance of the Option class with the opt, longOpt, and description
     * fields set
     */
    public static Option getOption(final String inputOption)
    {
        //try string as a short option name first
        Option returnedOption = shortToOptions.get(inputOption);
        if(returnedOption == null)
        {
            //try string as long option if short didn't work
            final String shortOpt = longToShorts.get(inputOption);
            if(shortOpt != null)
            {
                returnedOption = shortToOptions.get(shortOpt);
            } else {
                // Maybe there is no short option. Try long option directly
               returnedOption = longToOptions.get(inputOption);
            }
        }

        return(returnedOption);
    }

    /**
     * Create a new command line option.
     *
     * @param shortOpt the short name (letter) for the option
     * @param longOpt the long name for the option
     * @param argName the name of the option argument, or null if the
     * option takes no argument
     * @param description the description of the option
     *
     * @return Option object
     */
    public static Option createOption(final String shortOpt,final String longOpt,final String argName,final String description)
    {
        return(new MpcsOption(shortOpt,longOpt,argName != null,argName,description));
    }

    /**
     * Create a new command line option.
     *
     * @param longOpt the long name for the option
     * @param argName the name of the option argument, or null if the
     * option takes no argument
     * @param description the description of the option
     *
     * @return Option object
     */
    public static Option createOption(final String longOpt,final String argName,final String description)
    {
        return(createOption(null,longOpt,argName,description));
    }

    /**
     * Parse and display help if present.
     *
     * @param commandLine Command-line object
     * @param app         Command-line application object
     */
    public static void parseHelp(final CommandLine commandLine,final CommandLineApp app)
    {
        if(commandLine.hasOption(HELP.getOpt()))
        {
            app.showHelp();
            System.exit(2);
        }
    }

    /**
     * Parse and display version if present.
     *
     * @param commandLine Command-line object
     */
    public static void parseVersion(final CommandLine commandLine)
    {
        if(commandLine.hasOption(VERSION.getOpt()))
        {
            showVersion();
            System.exit(2);
        }
    }


    /**
     * Display version.
     */
    public static void showVersion()
    {
        final String appName = jpl.gds.shared.cli.app.ApplicationConfiguration.getApplicationName();
        System.out.println(ReleaseProperties.getProductLine() + " " + appName + " " + ReleaseProperties.getVersion());
    }

    /**
     * Parse JMS host.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseJmsHost(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initMessageConfig();
        if(commandLine.hasOption(JMS_HOST.getLongOpt()))
        {
            final String jmsHost = commandLine.getOptionValue(JMS_HOST.getLongOpt());
            if(jmsHost == null)
            {
                throw new ParseException("You must supply a value for the JMS host option.");
            }

            messageConfig.setMessageServerHost(jmsHost);
        }
        else if(required)
        {
            throw new MissingOptionException("You must supply the JMS host.");
        }
    }

  

    /**
     * Parse JMS port.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseJmsPort(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initMessageConfig();
        if (commandLine.hasOption(JMS_PORT.getLongOpt()))
        {
            final String jmsPortStr = commandLine.getOptionValue(JMS_PORT.getLongOpt());

            if(jmsPortStr == null)
            {
                throw new ParseException("You must supply a value for the JMS port option.");
            }

            messageConfig.setMessageServerPort(
                parseInt(jmsPortStr, "JMS port", MIN_PORT, MAX_PORT));
        }
        else if(required)
        {
            throw new MissingOptionException("You must supply the JMS port.");
        }
    }


    /**
     * Parse JMS subtopic.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseJmsSubtopic(final CommandLine commandLine, final boolean required) throws ParseException
    {

        initVenueConfig();
        initGeneralInfo();
        
        if (commandLine.hasOption(JMS_SUBTOPIC.getLongOpt()))
        {
            final String subtopic = commandLine.getOptionValue(JMS_SUBTOPIC.getLongOpt());
            if (subtopic == null)
            {
                throw new ParseException("You must supply a value for the JMS subtopic option.");
            }

            final VenueType venueType = venueConfig.getVenueType();
            if(venueType == null)
            {
                throw new ParseException("Venue type on session configuration object is null.");
            }

            /* MPCS-7677 - 9/15/15. Use session properties to check validity of venue for mission.*/
            if (! venueType.isOpsVenue() || !missionProps.getAllowedVenueTypes().contains(venueType))
            {
            	//MPCS-5363 09/25/13 issue non-mission specific message
                throw new ParseException("The JMS subtopic option is only valid for OPS venue");
            }

            boolean foundSt = false;
            final List<String> subtopics = missionProps.getAllowedSubtopics();

            if (subtopics == null) {
                throw new ParseException("No JMS subtopics have been found in the GDS configuration.");
            }

            for (final String st: subtopics)
            {
                if (st.equals(subtopic))
                {
                    foundSt = true;
                    break;
                }
            }

            if (foundSt == false)
            {
                final StringBuffer buf = new StringBuffer(1024);
                buf.append("The JMS subtopic '");
                buf.append(subtopic);
                buf.append("' is not one of the configured subtopics.\n");
                buf.append("Allowable values are: ");
                for (int j = 0; j < subtopics.size(); j++)
                {
                    buf.append(subtopics.get(j));
                    if (j != subtopics.size() - 1)
                    {
                        buf.append(", ");
                    }
                }
                throw new ParseException(buf.toString());
            }

            generalInfo.setSubtopic(subtopic);
        }
        else if(required)
        {
            throw new MissingOptionException("You must supply the JMS subtopic.");
        }
    }

    /**
     * Parse venue type.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseVenueType(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initVenueConfig();
        
        if(commandLine.hasOption(VENUE_TYPE.getOpt()))
        {
            String srcStr = null;
            try
            {
                srcStr = commandLine.getOptionValue(VENUE_TYPE.getOpt());
                if(srcStr == null)
                {
                    throw new ParseException("You must supply a value for the venue type option.");
                }
                final VenueType src = VenueType.valueOf(srcStr.toUpperCase());

                if(venueConfig == null)
                {
                    throw new ParseException("Venue configuration object in ReservedOptions is null.");
                }

                if (! missionProps.getAllowedVenueTypes().contains(src))
                {
                    throw new ParseException("Unsupported venue type for mission: " + src);
                }

                venueConfig.setVenueType(src);
            }
            catch (final IllegalArgumentException e)
            {
                throw new ParseException("Invalid venue type value of '" + srcStr + "'.");
            }
        }
        else if(required)
        {
            throw new MissingOptionException("You must supply the venue type.");
        }
    }


    /**
     * Parse downlink connection type.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseConnectionType(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initConnectConfig();
        if(commandLine.hasOption(CONNECTION_TYPE.getOpt()))
        {
            String ctStr = null;

            try
            {
                ctStr = commandLine.getOptionValue(CONNECTION_TYPE.getOpt());
                final TelemetryConnectionType ct = TelemetryConnectionType.valueOf(ctStr);

                if ((ct == TelemetryConnectionType.UNKNOWN) ||
                        !connectProps.getAllowedDownlinkConnectionTypes(sseFlag.isApplicationSse()).contains(ct))
                {
                    throw new ParseException("'" + ct + "' is an invalid downlink connection type " +
                                             "for this mission.");
                }
                
                if(connectConfig == null)
                {
                    throw new ParseException("Connection configuration object in ReservedOptions is null.");
                }
                final IDownlinkConnection current = connectConfig.getDownlinkConnection();
                if (current == null || current.getDownlinkConnectionType() != ct) {
                	connectConfig.createDownlinkConnection(ct);
                }
            }
            catch (final IllegalArgumentException e){
                throw new ParseException("'" + ctStr + "' is an invalid downlink connection type.");
            }
            catch (final NullPointerException e){
                throw new ParseException("No downlink connection type specified.");
            }

            if(!commandLine.hasOption(VENUE_TYPE.getOpt()))
            {
                throw new ParseException("If you specify the downlink connection type, you must also specify the VenueType.");
            }

        }
        else if(required)
        {
            throw new MissingOptionException("You must supply the downlink connection type.");
        }
    }


    /**
     * Parse uplink connection type.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseUplinkConnectionType(final CommandLine commandLine,
                                                 final boolean     required) throws ParseException
    {
        initConnectConfig();
        if (commandLine.hasOption(UPLINK_CONNECTION_TYPE.getOpt()))
        {
            String ctStr = null;

            try
            {
                ctStr = commandLine.getOptionValue(UPLINK_CONNECTION_TYPE.getOpt());

                final UplinkConnectionType ct = UplinkConnectionType.valueOf(ctStr);

                if (ct == UplinkConnectionType.UNKNOWN)
                {
                    throw new ParseException("'" + ct + "' is an invalid uplink connection type.");
                }

                if (connectConfig == null)
                {
                    throw new ParseException("Connection configuration object in ReservedOptions is null.");
                }

                final IUplinkConnection current = connectConfig.getFswUplinkConnection();
                if (current == null || current.getUplinkConnectionType() != ct) {
                	connectConfig.createFswUplinkConnection(ct);
                }
            }
            catch (final IllegalArgumentException e)
            {
                throw new ParseException("'" + ctStr + "' is an invalid uplink connection type.");
            }
            catch (final NullPointerException e)
            {
                throw new ParseException("No uplink connection type specified.");
            }

            if (! commandLine.hasOption(VENUE_TYPE.getOpt()))
            {
                throw new ParseException("If you specify the uplink connection type, " +
                                         "you must also specify the venueType.");
            }

        }
        else if (required)
        {
            throw new MissingOptionException("You must supply the uplink connection type.");
        }
    }


    /**
     * Parse testbed name.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseTestbedName(final CommandLine commandLine, final boolean required) throws ParseException
    {
        
        initVenueConfig();

        if (commandLine.hasOption(TESTBED_NAME.getOpt()))
        {
            final String tbName = commandLine.getOptionValue(TESTBED_NAME.getOpt());
            if (tbName == null)
            {
                throw new ParseException("You must supply a value for the testbed name option.");
            }

            final VenueType venueType = venueConfig.getVenueType();
            if(venueType == null)
            {
                throw new ParseException("Venue type on session configuration object is null.");
            }

            if (venueType != VenueType.TESTBED && venueType != VenueType.ATLO)
            {
                throw new ParseException("The testbed name option is only valid for an venue type of " + VenueType.TESTBED + " or " + VenueType.ATLO);
            }

            boolean foundTb = false;
            final List<String> testbeds = missionProps.getAllowedTestbedNames(venueConfig.getVenueType());
            for (final String name: testbeds)
            {
                if (name.equals(tbName))
                {
                    foundTb = true;
                    break;
                }
            }

            if (foundTb == false)
            {
                final StringBuffer buf = new StringBuffer(1024);
                buf.append("The testbed name '");
                buf.append(tbName);
                buf.append("' is not one of the configured testbed names.\n");
                buf.append("Allowable values are: ");
                for (int j = 0; j < testbeds.size(); j++)
                {
                    buf.append(testbeds.get(j));
                    if (j != testbeds.size() - 1)
                    {
                        buf.append(", ");
                    }
                }
                throw new ParseException(buf.toString());
            }

            venueConfig.setTestbedName(tbName);
        }
        else if(required)
        {
            throw new MissingOptionException("You must supply the testbed name.");
        }

        if(venueConfig.getVenueType() != null &&
            ((venueConfig.getVenueType().equals(VenueType.TESTBED) ||
            venueConfig.getVenueType().equals(VenueType.ATLO)) && venueConfig.getTestbedName() == null))
        {
            throw new ParseException("The testbed name must be supplied when the TESTBED or ATLO venue is specified.");
        }
    }


    /**
     * Parse downlink stream id.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseDownlinkStreamId(final CommandLine commandLine, final boolean required) throws ParseException
    {
        parseDownlinkStreamId(commandLine, required, false);
    }


    /**
     * Parse downlink stream id. For chill_monitor we allow any venue type, and also allow
     * a value of NORMAL. This allows for the fact that chill_monitor does not have
     * connection types and hence cannot always build the topic without help.
     *
     * @param commandLine Command line
     * @param required    True if required
     * @param monitor     True if for chill monitor
     *
     * @throws ParseException Parse error
     */
    public static void parseDownlinkStreamId(final CommandLine commandLine,
                                             final boolean     required,
                                             final boolean     monitor)
        throws ParseException
    {
        initVenueConfig();
        initConnectConfig();
        
        if (commandLine.hasOption(DOWNLINK_STREAM_ID.getOpt()))
        {
            final String streamId = commandLine.getOptionValue(DOWNLINK_STREAM_ID.getOpt());

            if (streamId == null)
            {
                throw new ParseException("You must supply a value for the stream ID option.");
            }

            final VenueType venueType = venueConfig.getVenueType();

            if (venueType == null)
            {
                throw new ParseException("Venue type on session configuration object is null.");
            }

            /* 
             * 6/10/13 - MPCS-4895 Remove check that allowed downlink stream ID through
             * for all venues if being called by a monitor client.
             */   
            if ((streamId != null) && (venueType != VenueType.TESTBED) && (venueType != VenueType.ATLO))
            {
                throw new ParseException("The stream ID option is only valid for an venue type of " +
                                         VenueType.TESTBED + " or " + VenueType.ATLO);
            }

            boolean  foundStream = false;
            final List<String> streams = missionProps.getAllowedDownlinkStreamIdsAsStrings(venueType);

            // MPCS-3819 - 4/17/13 Allow the NORMAL stream type if the
            // connection type and venue combination supports it

            final boolean allowNaStream = venueConfig.getVenueType() != null &&
            	    connectConfig.getFswDownlinkConnection().getDownlinkConnectionType() != null &&
                    ((venueConfig.getVenueType().equals(VenueType.TESTBED) ||
                    venueConfig.getVenueType().equals(VenueType.ATLO)) && 
                    !connectConfig.getFswDownlinkConnection().getDownlinkConnectionType().requiresStreamId());
            	
            if (monitor || allowNaStream)
            {
                // Make sure NOT_APPLICABLE is in the list
                /* MPCS-7710 - 10/8/15. Now add to the list directly without
                 * having to resize an array.
                 */
                if (!streams.contains(DownlinkStreamType.NOT_APPLICABLE.toString())) {
                    streams.add(DownlinkStreamType.NOT_APPLICABLE.toString());
                }

            }

            final String temp = streamId.replace(' ', '_');

            for (final String s : streams)
            {
                if (s.replace(' ', '_').equalsIgnoreCase(temp))
                {
                    foundStream = true;
                    break;
                }
            }

            if (! foundStream)
            {
                final StringBuilder buffer = new StringBuilder(1024);
                buffer.append("Allowable stream ID values are:");
                int j = 0;
                for (final String s: streams)
                {
                    buffer.append(s);
                    if (j != streams.size() - 1)
                    {
                        buffer.append(", ");
                    }
                    j++;
                }
                throw new ParseException("The stream ID value '" + streamId +
                                         "' is not one of the allowable Stream ID values in the configuration.");
            }

            final DownlinkStreamType dsi = DownlinkStreamType.convert(streamId);

            venueConfig.setDownlinkStreamId(dsi);
        }
        else if(required)
        {
            throw new MissingOptionException("You must supply the downlink stream id.");
        }

        // MPCS-3819 (4/17/13) Made this check sensitive to connection type value so
        // we know whether to accept a null stream ID.
        if (venueConfig.getVenueType() != null &&
            ((venueConfig.getVenueType().equals(VenueType.TESTBED) ||
            venueConfig.getVenueType().equals(VenueType.ATLO)) && 
            connectConfig.getFswDownlinkConnection().getDownlinkConnectionType() != null &&
            connectConfig.getFswDownlinkConnection().getDownlinkConnectionType().usesStreamId() && 
            venueConfig.getDownlinkStreamId() == null))
        {
            throw new ParseException("The downlink stream ID must be supplied when the TESTBED or ATLO venue is specified.");
        }
    }


    /**
     * Parse application configuration.
     *
     * @param commandLine Command line
     * @param required    True if required
     * @param type        Application type
     *
     * @return Application configuration
     *
     * @throws ParseException Parse error
     */
    public static ApplicationConfiguration parseAppConfig(final CommandLine commandLine, final boolean required, final ApplicationType type) throws ParseException
    {
        initPerspectiveConfig();
        ApplicationConfiguration appConfig = null;
        if(commandLine.hasOption(APPLICATION_CONFIGURATION.getOpt()))
        {
            final String configFile = commandLine.getOptionValue(APPLICATION_CONFIGURATION.getOpt());
            if(configFile == null)
            {
                throw new MissingOptionException("The command line option for application configuration requires a value.");
            }

            final File f = new File(configFile);
            if (!f.exists())
            {
                throw new ParseException("Application configuration file " + configFile + " was not found");
            }

            try
            {
                perspectiveConfig.createFromApplicationIdFile(configFile);
            }
            catch (final Exception e)
            {
                throw new ParseException("Problem loading perspective configuration from application ID file " + configFile + ": " + e.getMessage());
            }

            appConfig = perspectiveConfig.getApplicationConfiguration(type);
            if(appConfig == null)
            {
                throw new ParseException("Application configuration file was not found in perspective directory " +
                		perspectiveConfig.getConfigPath());
            }
        }
        else if(required)
        {
            throw new MissingOptionException("Missing required command line option for application configuration.");
        }

        return(appConfig);
    }

    /**
     * Parse session configuration.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSessionConfig(final CommandLine commandLine,final boolean required) throws ParseException
    {
        final  String GDS_SCHEMA_LOC = "/schema/SessionConfigFile.rnc";

        initContextConfig();

        if(commandLine.hasOption(SESSION_CONFIGURATION.getOpt()))
        {
            final String configFile = commandLine.getOptionValue(SESSION_CONFIGURATION.getOpt(),null);
            if(configFile == null)
            {
                throw new MissingOptionException("The argument -" + 
                		SESSION_CONFIGURATION.getOpt() + " requires a value.");
            }

            final File f = new File(configFile);
            if (f.exists() == false)
            {
                throw new ParseException("The input session " +
                		"configuration file '" + configFile + "' does not " +
                		"exist.");
            }
            // validate config file against schema
            final String gdsConfigDir = GdsSystemProperties.getGdsDirectory();      
            final File schemaFile = new File(gdsConfigDir+GDS_SCHEMA_LOC);
            if (!schemaFile.exists())
            {
                throw new ParseException("The schema file: " +
                		"SessionConfigFile.rnc does not exist.");
            }    
            try {
                final boolean pass = IContextConfiguration.schemaVsConfigFileCheck(schemaFile,f);
                if (!pass) {
                  	throw new ParseException("The configuration " +
                  			"file does not match schema definition");
                }
            } catch (final XmlValidationException ve) {
            	    throw new  ParseException("Parsing XML error" + 
            	    		ve.getMessage());
            }
            
            //prohibit commandLine overwrite of sessionConfig items   
            // 6/5/13 - MPCS-4883. Make the message better.  If the option name changes we still want
            // it to be accurate.
            // 2/5/14 - MPCS-5751: Add argument for option currently being parsed

            final String bad = commandLineOverwriteConfig(commandLine, TESTCONFIG_LONG_VALUE);

            if (bad != null)
            {
                throw new ParseException("Command line "       +
                                         "overwrite of --"     +
                                         TESTCONFIG_LONG_VALUE +
                                         " file by option --"  +
                                         bad                   +
                                         " is not supported.");
            }
        
            contextConfig.load(configFile.trim());

            if(!GdsSystemProperties.isIntegratedGui())
            {
                contextId.clearFieldsForNewConfiguration();
            }
        }
        else if(required)
        {
            throw new ParseException("The required command line " +
            		"option for session configuration file is missing.");
        }
    }


    /**
     * Parse session key.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSessionKey(final CommandLine commandLine,
                                       final boolean     required) throws ParseException
    {
        // MPCS-5909 3/3/14: Added checkValidSession flag
        parseSessionKey(commandLine, required, false, true);
    }

    /**
     * Parse session key.
     *
     * @param commandLine Command line
     * @param required    True if required
     * @param minimal     True if just key desired
     *
     * @throws ParseException Parse error
     */
    public static void parseSessionKey(final CommandLine commandLine,
                                       final boolean     required,
                                       final boolean     minimal) throws ParseException {
        // MPCS-5909 3/3/14: Added checkValidSession flag
        parseSessionKey(commandLine, required, minimal, true);
    }
    
    // MPCS-5909 3/3/14: Added method to parse session key without
    // checking to see if the session is in the database. This is for use with
    // chill_return_lad, which queries a Global LAD. The Global LAD is not
    // always guaranteed to be on the same host as the LOM database containing
    // the session
    /**
     * Parse session key, with the option to check if the session is a valid
     * session in the database. Similar to calling parseSessionKey with minimal
     * = true, except this method does not check if the session is a valid
     * session.
     * 
     * @param commandLine Command line
     * @param required True if required
     * 
     * @throws ParseException Parse error
     */
    public static void parseSessionKeyWithoutDbCheck(final CommandLine commandLine,
                                       final boolean     required) throws ParseException {
        parseSessionKey(commandLine, required, true, false);
    }

    // MPCS-5909 3/3/14: Added checkValidSession flag, made private.
    // Refactored public interface to not include the checkValidSession flag
    /**
     * Parse session key.
     *
     * @param commandLine Command line
     * @param required    True if required
     * @param minimal     True if just key desired
     *
     * @throws ParseException Parse error
     */
    private static void parseSessionKey(final CommandLine commandLine,
                                       final boolean     required,
                                       final boolean     minimal,
                                       final boolean     checkValidSession) throws ParseException
    {

        initContextConfig();

        if (commandLine.hasOption(SESSION_KEY.getOpt()))
        {
            final String testKey = commandLine.getOptionValue(SESSION_KEY.getOpt(),null);

            if (testKey == null)
            {
                throw new MissingOptionException("The argument -" + SESSION_KEY.getOpt() + " requires a value.");
            }

            try
            {
                // MPCS-5909 3/3/14: check session only if flag is true
                if(checkValidSession) {
                    /* MPCS-9572  - 4/4/18 - Use fetch factory rather than archive controller */
                    final IDbSqlFetch tsf = appContext.getBean(IDbSqlFetchFactory.class).getSessionFetch(false);
                    final IDbSessionInfoFactory dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);

                    /** MPCS-8322 - 07/25/2016: Database Session now adaptable. */
                    final IDbSessionInfoUpdater tsi = dbSessionInfoFactory.createQueryableUpdater();
					tsi.addSessionKey(parseLong(testKey, "Test key", MIN_SESSION_KEY, MAX_SESSION_KEY));
                    
                    final List<? extends IDbRecord> testSessions = tsf.get(tsi,null,1,(IDbOrderByType)null);
                    if(testSessions.isEmpty())
                    {
                        throw new ParseException("Value of -" + SESSION_KEY.getOpt() + " option must be a valid pre-existing session key. No session with the key '" + testKey + "' was found.");
                    }

                    final IDbSessionUpdater dsc = (IDbSessionUpdater) testSessions.get(0);

                    if (! minimal)
                    {
                        dsc.setIntoContextConfiguration(contextConfig);
                    }
                    else
                    {
                        contextId.setNumber(dsc.getSessionId());
                    }
                } else {
                    contextId.setNumber(parseLong(testKey, "Test key", MIN_SESSION_KEY, MAX_SESSION_KEY));
                }
            }
            catch(final DatabaseException s)
            {
                throw new ParseException("Error Connecting to the database while looking up the specified session key: " + s.getMessage());
            }
        }
        else if(required)
        {
            throw new ParseException("Missing the required command line option for session key.");
        }
    }


    /**
     * Parse session name.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSessionName(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initContextConfig();

        if(commandLine.hasOption(SESSION_NAME.getOpt()))
        {
            final String testName = commandLine.getOptionValue(SESSION_NAME.getOpt(),null);
            if(testName == null)
            {
                throw new MissingOptionException("The option -" + SESSION_NAME.getOpt() + " requires a value.");
            }

            for (int i = 0; i < testName.length(); i++) {
                final char c = testName.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                    throw new ParseException("Session name can contain only letters, digits, dashes, and underscores.");
                }
            }
            contextId.setName(testName.trim());
        }
        else if(required)
        {
            throw new ParseException("Missing required command line option for session name.");
        }
    }

    /**
     * Parse session type.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSessionType(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initContextConfig();
        
        if(commandLine.hasOption(SESSION_TYPE.getOpt()))
        {
            final String testType = commandLine.getOptionValue(SESSION_TYPE.getOpt(),null);
            if(testType == null)
            {
            	throw new MissingOptionException("The option -" + SESSION_TYPE.getOpt() + " requires a value.");
            }

            for (int i = 0; i < testType.length(); i++) {
                final char c = testType.charAt(i);
                /* MPCS-7772 - 11/30/15. Corrected invalid conditional */
                if (c == '&' || c == '<' || c == '>' || c == '%') {
                    throw new ParseException("Session type cannot contain the characters &, %, <, or >.");
                }
            }
            contextId.setType(testType.trim());
        }
        else if(required)
        {
            throw new ParseException("Missing required command line option for session type.");
        }
    }

    /**
     * Parse session description.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSessionDescription(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initContextConfig();

        if(commandLine.hasOption(SESSION_DESCRIPTION.getOpt()))
        {
            final String testDesc = commandLine.getOptionValue(SESSION_DESCRIPTION.getOpt(),null);
            if(testDesc == null)
            {
                throw new MissingOptionException("The option -" + SESSION_DESCRIPTION.getOpt() + " requires a value.");
            }

            for (int i = 0; i < testDesc.length(); i++) {
                final char c = testDesc.charAt(i);
                /* MPCS-7772 - 11/30/15. Corrected invalid conditional */
                if (c == '&' || c == '<' || c == '>' || c == '%') {
                    throw new ParseException("Session description cannot contain the characters &, %, <, or >.");
                }
            }
            contextId.setDescription(testDesc.trim());
        }
        else if(required)
        {
            throw new ParseException("Missing required command line option for session description.");
        }
    }

    /**
     * Parse session user.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSessionUser(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initContextConfig();

        if(commandLine.hasOption(SESSION_USER.getOpt()))
        {
            final String user = commandLine.getOptionValue(SESSION_USER.getOpt(),null);
            if(user == null)
            {
                throw new MissingOptionException("The option -" + SESSION_USER.getOpt() + " requires a value.");
            }

            contextId.setUser(user.trim());
        }
        else if(required)
        {
            throw new ParseException("Missing required command line option for session user.");
        }
    }

    /**
     * Parse session host.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSessionHost(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initContextConfig();

        if(commandLine.hasOption(SESSION_HOST.getOpt()))
        {
            final String host = commandLine.getOptionValue(SESSION_HOST.getOpt(),null);
            if(host == null)
            {
                throw new MissingOptionException("The option --" + SESSION_HOST.getLongOpt() + " requires a value.");
            }

            contextId.setHost(host.trim());
        }
        else if(required)
        {
            throw new ParseException("Missing required command line option --" + SESSION_HOST.getLongOpt());
        }
    }

    /**
     * Parse S/C id.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSpacecraftId(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initFilterInfo();

        if(commandLine.hasOption(SPACECRAFT_ID.getOpt()))
        {
            final String spacecraftId = commandLine.getOptionValue(SPACECRAFT_ID.getOpt(), null);
            if(spacecraftId == null)
            {
                throw new MissingOptionException("The option -" + SPACECRAFT_ID.getOpt() + " requires a value.");
            }

            final int scid = parseInt(spacecraftId, "Spacecraft Id", MIN_SCID, MAX_SCID);

            // If no mission, skip test

            if (!missionProps.isScidValid(scid))
            {
                throw new ParseException("Input spacecraft ID "  +
                                         scid                    +
                                         " is not a configured " +
                                         "spacecraft ID for this mission.");
            }

            contextId.setSpacecraftId(scid);
        }
        else if(required)
        {
            throw new ParseException("The required command line option Spacecraft ID is missing.");
        }
    }
    
    /**
     * Parse FSW downlink host.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseFswDownlinkHost(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initConnectConfig();
        
        if(commandLine.hasOption(FSW_DOWNLINK_HOST.getOpt()))
        {
            String host = commandLine.getOptionValue(FSW_DOWNLINK_HOST.getOpt(), null);
            if(host == null)
            {
                throw new MissingOptionException("The option -" + FSW_DOWNLINK_HOST.getOpt() + " requires a value.");
            }

            host = host.trim();
            
            final IDownlinkConnection dc = connectConfig.getFswDownlinkConnection();
            if (dc instanceof INetworkConnection) {
            	((INetworkConnection)dc).setHost(host);
            }
        }
        else if(required)
        {
            throw new ParseException("The required command line option FSW downlink host is missing.");
        }
    }


    /**
     * Parse FSW uplink host.
     *
     * @param commandLine Command line
     * @param required    True if required
     * @param useDefaults True if defaults should be used if option is absent from command line
     *
     * @throws ParseException Parse error
     */
    public static void parseFswUplinkHost(final CommandLine commandLine,final boolean required, final boolean useDefaults) throws ParseException
    {
        initConnectConfig();
        
        if(commandLine.hasOption(FSW_UPLINK_HOST.getLongOpt()))
        {
            String host = commandLine.getOptionValue(FSW_UPLINK_HOST.getLongOpt(), null);
            if(host == null)
            {
                throw new MissingOptionException("The option -" + FSW_UPLINK_HOST.getLongOpt() + " requires a value.");
            }

            host = host.trim();
            
            final IUplinkConnection uc = connectConfig.getFswUplinkConnection();
            if (uc != null) {
            	uc.setHost(host);
            }
        }
        else if(required)
        {
            throw new ParseException("The required command line option FSW uplink host is missing.");
        } else if (useDefaults) {
        	final String uplinkHost = connectConfig.getConnectionProperties().getDefaultUplinkHost(false);
        	
        	if(uplinkHost == null) {
        		throw new MissingOptionException("No default uplink host is configured, so the option -" + FSW_UPLINK_HOST.getLongOpt() + " requires a value.");
        	}

        	final IUplinkConnection uc = connectConfig.getFswUplinkConnection();
        	if (uc != null) {
        		uc.setHost(uplinkHost);
        	}
        }
    }
    
    /**
     * Parse FSW uplink host.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseFswUplinkHost(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initConnectConfig();
        
        if(commandLine.hasOption(FSW_UPLINK_HOST.getLongOpt()))
        {
            String host = commandLine.getOptionValue(FSW_UPLINK_HOST.getLongOpt(), null);
            if(host == null)
            {
                throw new MissingOptionException("The option -" + FSW_UPLINK_HOST.getLongOpt() + " requires a value.");
            }

            host = host.trim();
            final IUplinkConnection uc = connectConfig.getFswUplinkConnection();
            if (uc != null) {
            	uc.setHost(host);
            }
        }
        else if(required)
        {
            throw new ParseException("The required command line option FSW uplink host is missing.");
        }
    }


    /**
     * Parse SSE host.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSseHost(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initConnectConfig();
        
        if(commandLine.hasOption(SSE_HOST.getOpt()))
        {
            String host = commandLine.getOptionValue(SSE_HOST.getOpt(), null);
            if(host == null)
            {
                throw new MissingOptionException("The option -" + SSE_HOST.getOpt() + " requires a value.");
            }

            host = host.trim();
            final IUplinkConnection uc = connectConfig.getSseUplinkConnection();
            if (uc != null) {
            	uc.setHost(host);
            }
        }
        else if(required)
        {
            throw new ParseException("The required command line option SSE host is missing.");
        }
    }

    /**
     * Parse virtual channel id.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseVirtualChannel(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initFilterInfo();

        if(commandLine.hasOption(SESSION_VCID.getLongOpt()))
        {
            final String vcidStr = commandLine.getOptionValue(SESSION_VCID.getLongOpt(), null);
            if(vcidStr == null)
            {
                throw new MissingOptionException("The option -" + SESSION_VCID.getLongOpt() + " requires a value.");
            }

            final int vcid = (int) parseLong(vcidStr, "Virtual channel id", MIN_VCID, MAX_VCID);

            if (! missionProps.getAllDownlinkVcids().contains(vcid))
            {
                throw new ParseException("Input VCID "                         +
                                         vcid                                  +
                                         " is not found in the configuration " +
                                         "for this mission");
            }

            // MPCS-8226 05/24/16 - no longer check if spacecraft side is also being set - option was deprecated

            scFilterInfo.setVcid(vcid);

        }
        else if(required)
        {
            throw new ParseException("The required command line option session VCID is missing.");
        }
    }

    /**
     * Parse station id.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    
    public static void parseStationId(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initFilterInfo();
        
        if(commandLine.hasOption(SESSION_DSSID.getLongOpt()))
        {
            final String dsidStr = commandLine.getOptionValue(SESSION_DSSID.getLongOpt(), null);
            if(dsidStr == null)
            {
                throw new MissingOptionException("The option -" + SESSION_DSSID.getLongOpt() + " requires a value.");
            }

            int dsid = 0;

            try
            {
                dsid = parseInt(dsidStr, "Station id", MIN_STATION, MAX_STATION);
            }
            catch (final ParseException pe)
            {
                ExceptionTools.addCauseAndThrow(new ParseDssException(pe.getMessage()), pe);
            }

            /*
             * 8/26/13 - MPCS-5214. Replaced ConfiguredDssIds access with
             * StationMapper access to consolidate station configuration.
             */
            if (! missionProps.getStationMapper().getStationIdsAsSet().contains(dsid))
            {
            	/* 12/2/13 - MPCS-4483: Throw more specific exception */
            	throw new ParseDssException("Input DSSID '"                        +
                                         dsid                                   +
                                         "' is not found in the configuration " +
                                         "for this mission");
            }

            /*
             * 1/28/14 - Remove code that disallows --sessionDssId for
             * non-OPS venues.
             */

            scFilterInfo.setDssId(dsid);

        }
        else if(required)
        {
            throw new ParseException("The required command line option session VCID is missing.");
        }
    }

    /**
     * Parse FSW uplink port.
     *
     * @param commandLine Command line
     * @param required    True if required
     * @param useDefaults True if defaults should be used if option is absent from command line
     *
     * @throws ParseException Parse error
     */
    public static void parseFswUplinkPort(final CommandLine commandLine,final boolean required, final boolean useDefaults) throws ParseException
    {
        initConnectConfig();
        
        if(commandLine.hasOption(FSW_UPLINK_PORT.getOpt()))
        {
            final String port = commandLine.getOptionValue(FSW_UPLINK_PORT.getOpt(), null);
            if(port == null)
            {
                throw new MissingOptionException("The option -" + FSW_UPLINK_PORT.getOpt() + " requires a value.");
            }

            final int fswPort = parseInt(port, "FSW uplink port", MIN_PORT, MAX_PORT);

            final IUplinkConnection uc = connectConfig.getFswUplinkConnection();
            if (uc != null) {
            	uc.setPort(fswPort);
            }

        }
        else if(required)
        {
            throw new ParseException("The required command line option FSW uplink port is missing.");
        } else if(useDefaults) {
        	final int uplinkPort = connectConfig.getConnectionProperties().getDefaultUplinkPort(false);
        	
        	if(uplinkPort == HostPortUtility.UNDEFINED_PORT) {
        		throw new MissingOptionException("No default uplink port is configured, so the option -" + FSW_UPLINK_PORT.getLongOpt() + " requires a value.");
        	}
        	final IUplinkConnection uc = connectConfig.getFswUplinkConnection();
        	if (uc != null) {
        		uc.setPort(uplinkPort);
        	}
        }
    }
    
    /**
     * Parse FSW uplink port.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseFswUplinkPort(final CommandLine commandLine,final boolean required) throws ParseException
    {
        parseFswUplinkPort(commandLine, required, false);
    }

    /**
     * Parse FSW downlink port.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseFswDownlinkPort(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initConnectConfig();
        
        if(commandLine.hasOption(FSW_DOWNLINK_PORT.getOpt()))
        {
            final String port = commandLine.getOptionValue(FSW_DOWNLINK_PORT.getOpt(), null);
            if(port == null)
            {
                throw new MissingOptionException("The option -" + FSW_DOWNLINK_PORT.getOpt() + " requires a value.");
            }

            final int fswPort = parseInt(port, "FSW downlink port", MIN_PORT, MAX_PORT);

            final IDownlinkConnection dc = connectConfig.getFswDownlinkConnection();
            if (dc instanceof INetworkConnection) {
            	((INetworkConnection)dc).setPort(fswPort);
            }
        }
        else if(required)
        {
            throw new ParseException("The required command line option FSW downlink port is missing.");
        }
    }

    /**
     * Parse SSE uplink port.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSseUplinkPort(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initConnectConfig();
        
        if(commandLine.hasOption(SSE_UPLINK_PORT.getOpt()))
        {
            final String port = commandLine.getOptionValue(SSE_UPLINK_PORT.getOpt(), null);
            if(port == null)
            {
                throw new MissingOptionException("The option -" + SSE_UPLINK_PORT.getOpt() + " requires a value.");
            }

            final int ssePort = parseInt(port, "SSE uplink port", MIN_PORT, MAX_PORT);

            final IUplinkConnection uc = connectConfig.getSseUplinkConnection();
            if (uc != null) {
            	uc.setPort(ssePort);
            }
        }
        else if(required)
        {
            throw new ParseException("The required command line option SSE uplink port is missing.");
        }
    }

    /**
     * Parse SSE downlink port.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseSseDownlinkPort(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initConnectConfig();
        
        if(commandLine.hasOption(SSE_DOWNLINK_PORT.getOpt()))
        {
            final String port = commandLine.getOptionValue(SSE_DOWNLINK_PORT.getOpt(), null);
            if(port == null)
            {
                throw new MissingOptionException("The option -" + SSE_DOWNLINK_PORT.getOpt() + " requires a value.");
            }

            final int ssePort = parseInt(port, "SSE downlink port", MIN_PORT, MAX_PORT);

            final IDownlinkConnection dc = connectConfig.getSseDownlinkConnection();
            if (dc instanceof INetworkConnection) {
            	((INetworkConnection)dc).setPort(ssePort);
            }
        }
        else if(required)
        {
            throw new ParseException("The required command line option SSE downlink port is missing.");
        }
    }

    /**
     * Parse FSW dictionary directory.
     *
     * @param commandLine  Command line
     * @param required     True if required
     * @param doSetDefault True if default is used when not provided
     *
     * @throws ParseException Parse error
     */
    public static void parseFswDictionaryDir(final CommandLine commandLine,final boolean required,final boolean doSetDefault) throws ParseException
    {
        
        initDictConfig();

        if(commandLine.hasOption(FSW_DICTIONARY_DIRECTORY.getOpt()))
        {
            final String dictDir = commandLine.getOptionValue(FSW_DICTIONARY_DIRECTORY.getOpt(), null);
            if(dictDir == null)
            {
                throw new MissingOptionException("The argument -" + FSW_DICTIONARY_DIRECTORY.getOpt() + " requires a value.");
            }

            final File dictDirFile = new File(dictDir);
            if(dictDirFile.exists() == false)
            {
                throw new ParseException("The specified FSW dictionary directory does not exist.");
            }

            dictConfig.setFswDictionaryDir(dictDir.trim());
        }
        else if(required)
        {
            throw new ParseException("The required command line option FSW dictionary directory is missing.");
        }
        else if(doSetDefault)
        {
            /* MPCS-6085 - 5/4/14 - Use global dictionary configuration instance instead of static method.*/
            dictConfig.setFswDictionaryDir(dictConfig.getDefaultFswDictionaryDir());
        }
    }

    /**
     * Parse SSE dictionary directory.
     *
     * @param commandLine  Command line
     * @param required     True if required
     * @param doSetDefault True if default is used when not provided
     *
     * @throws ParseException Parse error
     */
    public static void parseSseDictionaryDir(final CommandLine commandLine,final boolean required,final boolean doSetDefault) throws ParseException
    {
        initDictConfig();

        if(commandLine.hasOption(SSE_DICTIONARY_DIRECTORY.getOpt()))
        {
            final String dictDir = commandLine.getOptionValue(SSE_DICTIONARY_DIRECTORY.getOpt(), null);
            if(dictDir == null)
            {
                throw new MissingOptionException("The argument -" + SSE_DICTIONARY_DIRECTORY.getOpt() + " requires a value.");
            }

            final File dictDirFile = new File(dictDir);
            if(dictDirFile.exists() == false)
            {
                throw new ParseException("The specified SSE dictionary directory does not exist.");
            }

            dictConfig.setSseDictionaryDir(dictDir.trim());
        }
        else if(required)
        {
            throw new ParseException("The required command line option SSE dictionary directory is missing.");
        }
        else if(doSetDefault)
        {
            /* MPCS-6085 - 5/4/14 - Use global dictionary configuration instance instead of static method.*/
            dictConfig.setSseDictionaryDir(dictConfig.getDefaultSseDictionaryDir());
        }
    }

    /**
     * Parse FSW version.
     *
     * @param commandLine  Command line
     * @param required     True if required
     * @param doSetDefault True if default is used when not provided
     *
     * @throws ParseException Parse error
     */
    public static void parseFswVersion(final CommandLine commandLine,final boolean required,final boolean doSetDefault) throws ParseException
    {
        initDictConfig();

        if(commandLine.hasOption(FSW_VERSION.getOpt()))
        {
            final String fswVersion = commandLine.getOptionValue(FSW_VERSION.getOpt(), null);
            if(fswVersion == null)
            {
                throw new MissingOptionException("The argument -" + FSW_VERSION.getOpt() + " requires a value.");
            }

            dictConfig.setFswVersion(fswVersion.trim());
        }
        else if(required)
        {
            throw new ParseException("The required command line option FSW version is missing.");
        }
        else if(doSetDefault)
        {
            dictConfig.setFswVersion(dictConfig.getDefaultFswVersion());
        }
    }

    /**
     * Parse SSE version.
     *
     * @param commandLine  Command line
     * @param required     True if required
     * @param doSetDefault True if default is used when not provided
     *
     * @throws ParseException Parse error
     */
    public static void parseSseVersion(final CommandLine commandLine,final boolean required,final boolean doSetDefault) throws ParseException
    {
        initDictConfig();

        if(commandLine.hasOption(SSE_VERSION.getOpt()))
        {
            final String sseVersion = commandLine.getOptionValue(SSE_VERSION.getOpt(), null);
            if(sseVersion == null)
            {
                throw new MissingOptionException("The argument -" + SSE_VERSION.getOpt() + " requires a value.");
            }

            dictConfig.setSseVersion(sseVersion.trim());
        }
        else if(required)
        {
            throw new ParseException("The required command line option SSE version is missing.");
        }
        else if(doSetDefault)
        {
            dictConfig.setSseVersion(dictConfig.getDefaultSseVersion());
        }
    }

    /**
     * Parse database host.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseDatabaseHost(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initDbProperties();
        
        if(commandLine.hasOption(DATABASE_HOST.getOpt()))
        {
            String host = commandLine.getOptionValue(DATABASE_HOST.getOpt());
            if(host == null)
            {
                throw new MissingArgumentException("-" + DATABASE_HOST.getOpt() + " requires a host name argument");
            }
            host = host.trim();


            dbProperties.setHost(host);
        }
        else if(required)
        {
            throw new ParseException("The required command line option for database host is missing.");
        }
    }

    /**
     * Parse database port.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseDatabasePort(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initDbProperties();
        
        //read in the database port number
        if(commandLine.hasOption(DATABASE_PORT.getOpt()))
        {
            final String portString = commandLine.getOptionValue(DATABASE_PORT.getOpt());
            if(portString == null)
            {
                throw new MissingArgumentException("-" + DATABASE_PORT.getOpt() + " requires a port number argument");
            }

            final int port = parseInt(portString, "Database port", MIN_PORT, MAX_PORT);

            dbProperties.setPort(port);
        }
        else if(required)
        {
            throw new ParseException("The required command line option for database port is missing.");
        }
    }

    /**
     * Parse database user name.
     *
     * @param commandLine Command line
     * @param required    True if required
     *
     * @throws ParseException Parse error
     */
    public static void parseDatabaseUsername(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initDbProperties();
        
        //read in the database username
        if(commandLine.hasOption(DATABASE_USERNAME.getLongOpt()))
        {
            String username = commandLine.getOptionValue(DATABASE_USERNAME.getLongOpt());
            if(username == null)
            {
                throw new MissingArgumentException("-" + DATABASE_USERNAME.getLongOpt() + " requires a username argument");
            }
            username = username.trim();

            dbProperties.setUsername(username);
        }
        else if(required)
        {
            throw new ParseException("The required command line option for database username is missing.");
        }
    }


    /**
     * Parse database password.
     *
     * @param commandLine Command line
     * @param required    True if password is required
     *
     * @throws ParseException Parse error
     */
    public static void parseDatabasePassword(final CommandLine commandLine,final boolean required) throws ParseException
    {
        initDbProperties();
        
        //read in the database password
        if(commandLine.hasOption(DATABASE_PASSWORD.getLongOpt()))
        {
            final String password = commandLine.getOptionValue(DATABASE_PASSWORD.getLongOpt());
            if(password == null)
            {
                throw new MissingArgumentException("-" + DATABASE_PASSWORD.getLongOpt() + " requires a password argument");
            }

            dbProperties.setPassword(password);
        }
        else if(required)
        {
            throw new ParseException("The required command line option for database password is missing.");
        }
    }
    
    /**
     * Parse CPD user role.
     * 
     * @param commandLine Command line
     * @return CommandUserRole
     * @throws ParseException if role could not be parsed
     */
    public static CommandUserRole parseUserRole(final CommandLine commandLine) throws ParseException {
        CommandUserRole role = null;
        
        initAccessControl();

        if(commandLine.hasOption(USER_ROLE.getLongOpt())) {
            final String roleString = StringUtil.safeTrim(
                    commandLine.getOptionValue(USER_ROLE.getLongOpt(), null)).toUpperCase();
            try
            {
                role = CommandUserRole.valueOf(roleString);
            }
            catch (final IllegalArgumentException iae)
            {
                throw new ParseException("--" + USER_ROLE.getLongOpt()
                + " does not support value '" + roleString + "'");
            }

            accessControlParams.setUserRole(role);
        }
        else if (role == null) {
            final CommandUserRole currentRole =  accessControlParams.getUserRole();

            if (currentRole == null)
            {
                role = securityProps.getDefaultRole();
            }
            else
            {
                role = currentRole;
            }
        }

        accessControlParams.setUserRole(role);

        return role;
    }
    
    /**
     * Parse login method.
     * 
     * @param commandLine Command line
     * @param isGui true if GUI application, false otherwise
     * @return login enumeration value
     * @throws ParseException if login method could not be parsed
     */
    public static LoginEnum parseLoginMethod(final CommandLine commandLine, final boolean isGui) throws ParseException {
    
        initAccessControl();
        
        LoginEnum loginMethod = null;

        if (commandLine.hasOption(LOGIN_METHOD_GUI.getLongOpt()))
        {
            final String loginMethodString =
                    StringUtil.safeTrim(
                            commandLine.getOptionValue(
                                    LOGIN_METHOD_GUI.getLongOpt(), null)).toUpperCase();
            try
            {
                loginMethod = LoginEnum.valueOf(loginMethodString);
            }
            catch (final IllegalArgumentException iae)
            {
                throw new ParseException("--"                        +
                        LOGIN_METHOD_GUI.getLongOpt() +
                        " does not support value '" +
                        loginMethodString           +
                        "'");
            }
        } else {
            final LoginEnum current =  accessControlParams.getLoginMethod();

            if (current == null)
            {
                if (isGui)
                {
                    loginMethod =
                            securityProps.getDefaultGuiAuthorizationMode();
                }
                else
                {
                    loginMethod =
                            securityProps.getDefaultCliAuthorizationMode();
                }
            }
            else
            {
                loginMethod = current;
            }
        }

        /**
         * MPCS-7185 - 4/10/15: Keytab file and username command line
         * options should only be allowed with KEYTAB login method
         */
        if(!loginMethod.equals(LoginEnum.KEYTAB_FILE) && 
                (commandLine.hasOption(KEYTAB_FILE.getLongOpt()) || 
                        commandLine.hasOption(USERNAME.getLongOpt()))) {
            throw new ParseException("Login method: " + loginMethod + " does "
                    + "not require a keytab file and username");
        }
        accessControlParams.setLoginMethod(loginMethod);

        return loginMethod;
    }
    
    /**
     * Parse the keytab file
     * 
     * @param commandLine Command line
     * @return keytab file location
     * @throws ParseException if keytab file cannot be parsed
     */
    public static String parseKeytabFile(final CommandLine commandLine) throws ParseException {
        
        initAccessControl();
        
        String keytabFile = "";

        if (commandLine.hasOption(KEYTAB_FILE.getLongOpt()))
        {
            keytabFile = StringUtil.safeTrim(
                    commandLine.getOptionValue(KEYTAB_FILE.getLongOpt(),
                            null));

            checkKeytabFileExists(keytabFile);

            accessControlParams.setKeytabFile(keytabFile);
        }
        else
        {
            final String current =  accessControlParams.getKeytabFile();

            if (current == null)
            {
                keytabFile = securityProps.getDefaultKeytabFile();

                checkKeytabFileExists(keytabFile);

                accessControlParams.setKeytabFile(keytabFile);
            }
            else
            {
                keytabFile = current;

                checkKeytabFileExists(keytabFile);
            }
        }
        accessControlParams.setKeytabFile(keytabFile);

        return keytabFile;
    }
    
    // 7/31/13 - MPCS-4992: added username option for keytab
    /**
     * Parses command line for username
     * 
     * @param commandLine Command line
     * @return username or empty string if not set
     * @throws ParseException if username can't be parsed
     */
    public static String parseUsername(final CommandLine commandLine) throws ParseException {
		
        initAccessControl();
        
        String username = "";

        if (commandLine.hasOption(USERNAME.getLongOpt()))
        {
            username = StringUtil.safeTrim(
                    commandLine.getOptionValue(USERNAME.getLongOpt(),
                            null));
        }
        else
        {
            final String current =  accessControlParams.getUserId();

            if (current != null)
            {
                username = current;
            }
        }
        accessControlParams.setUserId(username);

        return username;
    }

    /**
     * See if keytab file exists. If the string is empty we don't throw
     * because that will be checked by other code.
     *
     * @param keytabFile Keytab file
     *
     * @throws ParseException If it doesn't exist
     */
    private static void checkKeytabFileExists(final String keytabFile)
        throws ParseException
    {
        final String useKeytabFile = StringUtil.safeTrim(keytabFile);

        if (! useKeytabFile.isEmpty() && ! (new File(useKeytabFile)).exists())
        {
            throw new ParseException("Keytab file '" +
                                     useKeytabFile            +
                                     "' does not exist");
        }
    }


    /**
     * Parses the application command line.
     *
     * @param args the list of command line arguments
     * @param app  Application object
     *
     * @return a CommandLine object
     *
     * @throws ParseException Parse error
     */
    public static CommandLine parseCommandLine(final String[]       args,
                                               final CommandLineApp app)
        throws ParseException
    {
        return parseCommandLine(args, app, true);
    }


    /**
     * Parses the application command line.
     *
     * @param args the list of command line arguments
     * @param app  Application object
     * @param exit If true, exit
     *
     * @return a CommandLine object
     *
     * @throws ParseException Parse error
     */
    @SuppressWarnings({"DM_EXIT"})
    public static CommandLine parseCommandLine(final String[]       args,
                                               final CommandLineApp app,
                                               final boolean        exit)
        throws ParseException
    {
        CommandLine commandLine = null;
        try {
            final CommandLineParser parser = new PosixParser();
            commandLine = parser.parse(app.createOptions(), args);
            
            // MPCS-7336
            // if items in this array appear more than once, throw exception            
            for (int i=0;i<INPUT_LIMIT_ARGNAMES.length;i++) {
            	if (Collections.frequency(Arrays.asList(args), INPUT_LIMIT_ARGNAMES[i]) > 1) {
            		throw new UnrecognizedOptionException("Too many "+INPUT_LIMIT_ARGNAMES[i]+" arguments.");
            	}
            }
        }
        catch (final MissingOptionException e)
        {
            if (! exit)
            {
                throw e;
            }

            app.showHelp();
            if (args.length==0) {
                app.showHelp();
                System.exit(1);
            }

            // if -h,--help is specified, suppress "missing option" errors
            for (int i = 0; i < args.length; ++i) {
                if (args[i].equals("-" + ReservedOptions.HELP_SHORT_VALUE)
                        || args[i].equals("--" + ReservedOptions.HELP_LONG_VALUE)) {
                    System.exit(2);
                }
            }
            throw e;
        }
        catch (final MissingArgumentException e2)
        {
            if (exit)
            {
                System.err.println(e2.getMessage());
                System.exit(1);
            }
            else
            {
                throw e2;
            }
        }
        catch (final UnrecognizedOptionException e3)
        {
            if (exit)
            {
                System.err.println(e3.getMessage());
                System.exit(1);
            }
            else
            {
                throw e3;
            }
        }

        return commandLine;
    }
     
    /**
     * Indicates whether a session-config command line option other than a session
     * configuration file has been supplied.
     * 
     * @param commandLine
     *            the user's command line
     * @param longOption
     *            option that is currently being parsed
     *
     * @return Offending option or null if OK
     * 
     * 8/5/13 - Rewritten as it should be for MPCS-5109.
     * 2/5/14 - MPCS-5751: Added parameter for option currently
     *          being parsed so it's not classified as an option that shouldn't be
     *          allowed
     */
     public static String commandLineOverwriteConfig(final CommandLine commandLine, final String longOption)
     {
            /** MPCS-5913 03/04/14 Pass out the offending option */
    	    final List<Option> notAllowedOptions = new LinkedList<Option>();
    	    notAllowedOptions.add(getOption(TESTCONFIG_LONG_VALUE));
    	    notAllowedOptions.add(getOption(TESTKEY_LONG_VALUE));
    	    notAllowedOptions.add(getOption(TESTHOST_LONG_VALUE));
    	    notAllowedOptions.add(getOption(TESTUSER_LONG_VALUE));
    	    notAllowedOptions.add(getOption(TESTNAME_LONG_VALUE));
    	    notAllowedOptions.add(getOption(TESTDESCRIPTION_LONG_VALUE));
    	    notAllowedOptions.add(getOption(TESTTYPE_LONG_VALUE));
    	    notAllowedOptions.add(getOption(OUTPUTDIR_LONG_VALUE));
    	    notAllowedOptions.add(getOption(VENUETYPE_LONG_VALUE));
    	    notAllowedOptions.add(getOption(JMSSUBTOPIC_LONG_VALUE));
    	    notAllowedOptions.add(getOption(CONNECTION_TYPE_LONG_VALUE));
    	    notAllowedOptions.add(getOption(UPLINK_CONNECTION_TYPE_LONG_VALUE));
    	    notAllowedOptions.add(getOption(INPUT_FORMAT_LONG_VALUE));
    	    notAllowedOptions.add(getOption(INPUT_FILE_LONG_VALUE));
    	    notAllowedOptions.add(getOption(TESTBEDNAME_LONG_VALUE));
    	    notAllowedOptions.add(getOption(DOWNLINKSTREAM_LONG_VALUE));
    	    notAllowedOptions.add(getOption(SESSION_VCID_LONG_VALUE));
    	    notAllowedOptions.add(getOption(SESSION_DSSID_LONG_VALUE));
    	    notAllowedOptions.add(getOption(DB_SESSIONHOST_LONG_VALUE));
    	    notAllowedOptions.add(getOption(SPACECRAFTID_LONG_VALUE));
    	    notAllowedOptions.add(getOption(FSW_DOWNLINK_HOST_LONG_VALUE));
    	    notAllowedOptions.add(getOption(FSW_UPLINK_HOST_LONG_VALUE));
    	    notAllowedOptions.add(getOption(FSWDOWNLINKPORT_LONG_VALUE));
    	    notAllowedOptions.add(getOption(FSWUPLINKPORT_LONG_VALUE));
    	    notAllowedOptions.add(getOption(FSWVERSION_LONG_VALUE));
    	    notAllowedOptions.add(getOption(SSE_DICTIONARYDIR_LONG_VALUE));
    	    notAllowedOptions.add(getOption(SSEVERSION_LONG_VALUE));
    	    notAllowedOptions.add(getOption(SSEHOST_LONG_VALUE));
    	    notAllowedOptions.add(getOption(SSEUPLINKPORT_LONG_VALUE));
    	    notAllowedOptions.add(getOption(SSEDOWNLINKPORT_LONG_VALUE));
    	    notAllowedOptions.add(getOption(FSW_DICTIONARYDIR_LONG_VALUE));
    	    notAllowedOptions.add(getOption(DB_SESSIONKEY_LONG_VALUE));
    	    
    	    // 2/5/14 - MPCS-5751: remove option that is being parsed
    	    notAllowedOptions.remove(getOption(longOption));
    	    		   	
    	    for (final Option o: notAllowedOptions)
            {
                final String option = o.getLongOpt();

    	    	if (commandLine.hasOption(option))
                {
    	    		return option;
    	    	}
    	    }

    	    return null;
     }


    /**
     * Parse string as big integer.
     *
     * @param s    String to parse
     * @param what Description of parameter
     * @param min  Minimum value
     * @param max  Maximum value
     *
     * @return Parsed result
     *
     * @throws ParseException If unparseable or out of range
     */
    private static BigInteger parseBig(final String     s,
                                       final String     what,
                                       final BigInteger min,
                                       final BigInteger max)
        throws ParseException
    {

        final String value  = StringUtil.safeTrim(s);
        BigInteger   result = null;

        try
        {
            result = new BigInteger(value);
        }
        catch (final NumberFormatException nfe)
        {
            throw new ParseException(what                         +
                                     " must be an integer, but '" +
                                     value                        +
                                     "' is invalid");
        }

        if ((result.compareTo(min) < 0) || (result.compareTo(max) > 0))
        {
            throw new ParseException(what                                 +
                                     " of "                               +
                                     result                               +
                                     " is outside of permissible range [" +
                                     min                                  +
                                     ","                                  +
                                     max                                  +
                                     "]");
        }

        return result;
    }


    /**
     * Parse string as long.
     *
     * @param s    String to parse
     * @param what Description of parameter
     * @param min  Minimum value
     * @param max  Maximum value
     *
     * @return Long result
     *
     * @throws ParseException If unparseable or out of range
     */
    private static long parseLong(final String s,
                                  final String what,
                                  final long   min,
                                  final long   max)
        throws ParseException
    {

        return parseBig(s,
                        what,
                        BigInteger.valueOf(min),
                        BigInteger.valueOf(max)).longValue();
    }


    /**
     * Parse string as int.
     *
     * @param s    String to parse
     * @param what Description of parameter
     * @param min  Minimum value
     * @param max  Maximum value
     *
     * @return Int result
     *
     * @throws ParseException If unparseable or out of range
     */
    private static int parseInt(final String s,
                                final String what,
                                final int    min,
                                final int    max)
        throws ParseException
    {

        return parseBig(s,
                        what,
                        BigInteger.valueOf(min),
                        BigInteger.valueOf(max)).intValue();
    }
    
    /**
     * Gets all the possible testbed names for TESTBED-like venues (currently
     * only TESTBED and ATLO).
     * 
     * @return array of all testbed names
     * 
     */
    private static String[] getAllTestbedNames() {
        final List<String> allNames = new LinkedList<String>();
        for (final VenueType vt: VenueType.values()) {
            if (!vt.hasTestbedName()) {
                continue;
            }
            allNames.addAll(missionProps.getAllowedTestbedNames(vt));
            allNames.remove(MissionProperties.DEFAULT_TESTBED_NAME);
        }

        return (allNames.toArray(new String[] {}));
    }

}
