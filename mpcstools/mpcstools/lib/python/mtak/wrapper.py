#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module is the MTAK Wrapper, a.k.a. the MTAK Wrapper functions,
and it is intended to provide a less programmer-like interface to the MTAK
library functionality.  It factors out common operations into simple, one-line
function calls that are easy to use.
"""

from __future__ import (absolute_import, division, print_function)


# 7/31/13 - MPCS-4992: replaced password file with keytab file

import atexit
import email.mime.text
import logging
import mpcsutil
import mtak
import math
import operator
import os.path
import select
import smtplib
import subprocess
import six
import sys
import tempfile
import termios
import time
import tty
import auto.err
# MPCS-9813 - 8/30/2018
# getpass used to get the current username for CFDP PUT request
import getpass
# MPCS-9813 - 8/30/2018
# requests used to make REST API calls for CFDP PUT, json to parse the responses
import requests
import json
import six
long = long if six.PY2 else int

######################
#Uplink/Downlink
######################
#The session configuration used by all the wrapper functions
_sessionConfig = None
#The uplink proxy used by all the wrapper functions
#(Handles all functions related to uplink transmission)
_uplinkProxy = None
#The downlink proxy used by all the wrapper functions
#(Handles all interactions with received telemetry)
_downlinkProxy = None
#True if startup has been called, False otherwise or False if
#shutdown has been called
_running = False
#The time, in ms since epoch, when this script started
_startTimeSecs = 0
#The time, in ms since epoch, when this script ended
_endTimeSecs = 0
#Flag that is set to true in startup() if the inject_XXX functions should be disabled
_disableInjectFncsFlag = False
#Flag that is set to true if an automatic call to startup() was used.  Used to display the log message
#this was done prior to displaying startup info.
_automaticStart = False
#The default stringId that all the send_fsw_* functions will use.  If this is None, the Java
#behind the scenes will decide what String ID to use (usually 'A')
_defaultStringId = None
#The default Spacecraft ID (SCID) that all the send_fsw_* functions will use.  If this is None, the Java
#behind the scenes will decide what String ID to use (mission-specific)
_defaultScid = None

######################
#Logging
######################
#The log file being written to by the wrapper functions
_logFile = None
#Console log handler used by the wrapper
_consoleLogHandler = None
#File log handler used by the wrapper
_fileLogHandler = None
#Database log handler used by the wrapper
_databaseLogHandler = None
#Filter for turning off loggers
_denyAllFilter = mpcsutil.logutil.DenyAllFilter()
#Grab the logger specific to the MTAK wrapper
_log = lambda : logging.getLogger('mpcs.mtak.wrapper')

# Whether to throw cmd errors or swallow and return False
#  them. See MPCS-8932
_throwOnError = False

# MPCS-10085 1/29/19: Track these flags
_useDownlink = False
_useUplink = False

##############################################
#
# Internal Functions
#
##############################################

def _error(message):
    global _running
    _log().error(message)
    if not(_running):
        sys.stderr.write('ERROR: {}\n'.format(message))
        sys.stderr.flush()

def _warn(message):
    global _running
    _log().warning(message)
    if not(_running):
        sys.stderr.write('WARNING: {}\n'.format(message))
        sys.stderr.flush()

def _createWrapperLogHandlers(logDir, logFile, logTag, consoleEnable, fileEnable, databaseEnable):
    '''Create a log handlers for the MTAK wrapper logger.  We can't do this in the normal
    logging python-logging.properties config file because we generate a lot of this information dynamically based
    on the user, session, and the time.  We do this because a user will likely run multiple scripts against the same
    session and we don't want their logging to conflict.  In addition, we don't know the session directory
    until runtime and that's the default place log files will be stored.

    Currently this method generates three different types of loggers: console, file, and database.

    This method is only used internally, DO NOT call it directly...the results will be unpredictable at best.

    Arguments
    ----------
    logDir - A string pointing to the desired log directory to write a log file to (make sure that if you use
    this parameter that you make sure the directory is writable by MTAK).  If this is not specified, it will default to
    the MPCS session output directory. (string)

    logFile - The absolute path to a logFile to write logs to.  This will override the logDir argument. (string)

    logTag - The tag to prepend to each log entry. (string)

    consoleEnable - True if console logging is enabled, false otherwise. (boolean)

    fileEnable - True if file logging is enabled, false otherwise. (boolean)

    databaseEnable - True if database logging is enabled, false otherwise. (boolean)

    Returns
    --------
    The full path to the log file (string)'''

    global _sessionConfig, _uplinkProxy, _startTimeSecs, _logFile, _consoleLogHandler, _fileLogHandler, _databaseLogHandler, _error

    #Read our logging preferences out of the GDS configuration...figure out which handlers are enabled
    gdsConfig = mpcsutil.config.GdsConfig()

    if consoleEnable is None:
        consoleEnable = mpcsutil.getBooleanFromString(gdsConfig.getProperty('automationApp.mtak.logging.console.wrapper.enable', default='true'))
    if fileEnable is None:
        fileEnable = mpcsutil.getBooleanFromString(gdsConfig.getProperty('automationApp.mtak.logging.file.wrapper.enable', default='true'))
    if databaseEnable is None:
        databaseEnable = mpcsutil.getBooleanFromString(gdsConfig.getProperty('automationApp.mtak.logging.database.wrapper.enable', default='false'))

    filename = ''

    if not(_log().disabled):

        if consoleEnable:

            if _consoleLogHandler is not(None):
                _log().removeHandler(_consoleLogHandler)

            consoleLevel = logging.getLevelName(gdsConfig.getProperty('automationApp.mtak.logging.console.wrapper.level', default=logging.getLevelName(logging.INFO)))
            consoleFormat = gdsConfig.getProperty('automationApp.mtak.logging.console.wrapper.recordFormat', default='[%(levelname)s @ %(asctime)s]: %(message)s')

            #Try to create logs in the following places (in order)
            try:
                _consoleLogHandler = logging.StreamHandler(sys.stderr)
            except IOError:
                _error('Could not write MTAK log messages to the console')
                _consoleLogHandler = None

            if _consoleLogHandler is not(None):

                #Once we get the log console handler created, add it to our logger
                #(If the handler has a lower logging level than the logger...update the logger
                #to have the same level...otherwise you won't see any of the lower level stuff
                #out of the handler)
                _consoleLogHandler.setLevel(consoleLevel)
                if _log().getEffectiveLevel() > _consoleLogHandler.level:
                    _log().setLevel(_consoleLogHandler.level)

                _consoleLogHandler.setFormatter(logging.Formatter(consoleFormat, mpcsutil.timeutil.isoFmt))
                _log().addHandler(_consoleLogHandler)

        if fileEnable:
            if _fileLogHandler is not(None):
                _log().removeHandler(_fileLogHandler)

            datetimeFormat = gdsConfig.getProperty('automationApp.mtak.logging.file.wrapper.fileTimeFormat', '%Y_%m_%d_%H_%M_%S')
            logFilePrefix = gdsConfig.getProperty('automationApp.mtak.logging.file.wrapper.logFilePrefix', default='mtak')
            logFileSuffix = gdsConfig.getProperty('automationApp.mtak.logging.file.wrapper.logFileSuffix', default='.log')
            fileLevel = logging.getLevelName(gdsConfig.getProperty('automationApp.mtak.logging.file.wrapper.level', default=logging.getLevelName(logging.INFO)))
            fileFormat = gdsConfig.getProperty('automationApp.mtak.logging.file.wrapper.recordFormat', default='[%(levelname)s @ %(asctime)s]: %(message)s')

            fileFormat = logTag + ": " + fileFormat if logTag else fileFormat

            #Setup the file handler for the MTAK wrapper logger
            datetimeValue = mpcsutil.timeutil.getTimeString(seconds=_startTimeSecs, format=datetimeFormat)

            #if we are provided a log file, use it. otherwise use the log dir
            if logFile:
                try:
                    _fileLogHandler = logging.FileHandler(logFile)
                    _sessionConfig.logDir = os.path.dirname(logFile)
                    _logFile = logFile
                except IOError:
                    _error('Could not write the MTAK Wrapper log file to provided log file %s. ' % (logFile))

            else:

                filename = '%s_%s_%s%s' % (logFilePrefix, mpcsutil.user, datetimeValue, logFileSuffix)

                #Try to create logs in the following places (in order)
                logDirs = [logDir, _sessionConfig.logDir, _sessionConfig.outputDirectory, mpcsutil.homedir, tempfile.gettempdir()]

                for dir in logDirs:

                    if not dir:
                        continue

                    try:
                        tempLogFile = os.path.join(dir, filename)
                        #tempLogFile = '%s/%s' % (dir,filename)
                        _fileLogHandler = logging.FileHandler(tempLogFile, 'w')
                        _sessionConfig.logDir = dir
                        _logFile = tempLogFile
                        break
                    except IOError:
                        _warn('Could not write the MTAK Wrapper log file to disk in the directory %s. ' % (dir) +
                                   'You may want to supply the "logDir" parameter to your "startup()" call. ' +
                                   'Attempting to write logs elsewhere...')

            if _fileLogHandler is None:

                _error('Could not write MTAK Wrapper log file anywhere on disk. Logging will not be done to a file.')
                _sessionConfig.logDir = None

            else:

                #Once we get the log file handler created, add it to our logger
                #(If the handler has a lower logging level than the logger...update the logger
                #to have the same level...otherwise you won't see any of the lower level stuff
                #out of the handler)
                _fileLogHandler.setLevel(fileLevel)
                if _log().getEffectiveLevel() > _fileLogHandler.level:
                        _log().setLevel(_fileLogHandler.level)

                _fileLogHandler.setFormatter(logging.Formatter(fileFormat, mpcsutil.timeutil.isoFmt))
                _log().addHandler(_fileLogHandler)

                #Tell the user where to find their log file
                if filename != 'stdout':
                    print('')
                    print('MTAK log messages being written to %s' % (_logFile))
                    print('')

        if databaseEnable:
            if _databaseLogHandler is not None:
                _log().removeHandler(_databaseLogHandler)

            databaseLevel = logging.getLevelName(gdsConfig.getProperty('automationApp.mtak.logging.database.wrapper.level', default=logging.getLevelName(logging.INFO)))
            databaseFormat = gdsConfig.getProperty('automationApp.mtak.logging.database.wrapper.recordFormat', default='%(message)s')

            try:
                _databaseLogHandler = mtak.DatabaseHandler(_uplinkProxy)
            except IOError:
                _error('Could not write MTAK log messages to backend database. Logging will not be done to the database.')
                _databaseLogHandler = None

            if _databaseLogHandler is not None:

                #Once we get the log database handler created, add it to our logger
                #(If the handler has a lower logging level than the logger...update the logger
                #to have the same level...otherwise you won't see any of the lower level stuff
                #out of the handler)
                _databaseLogHandler.setLevel(databaseLevel)
                if _log().getEffectiveLevel() > _databaseLogHandler.level:
                    _log().setLevel(_databaseLogHandler.level)

                _databaseLogHandler.setFormatter(logging.Formatter(databaseFormat, mpcsutil.timeutil.isoFmt))
                _log().addHandler(_databaseLogHandler)

        return filename

def _generateLogSummary():
    '''Generates a summary of all script activity that will be placed at the end of
    the MTAK wrapper log file.  This method writes the summary directly to the log file, it does
    not hand the summary string back.

    This method is only used internally, DO NOT call it directly.

    Arguments
    ----------
    None

    Returns
    --------
    None'''

    global _sessionConfig, _uplinkProxy, _downlinkProxy, _startTimeSecs, _endTimeSecs, _log

    #Generate the summary string and write it to the log file
    _log().info(
'''

#################################################################
Script Summary Information
#################################################################
Start Date/Time = %s
End Date/Time = %s

%s

%s

#################################################################

''' % (mpcsutil.timeutil.getTimeString(seconds=_startTimeSecs),
       mpcsutil.timeutil.getTimeString(seconds=_endTimeSecs),
       _uplinkProxy.getSummary() if _uplinkProxy is not None else '',
       _downlinkProxy.getSummary() if _downlinkProxy is not None else ''))

##############################################
#
# Administrative Functions
#
##############################################

def startup(key=None, host=None, logDir=None, dbHost=None, dbPort=None, dbUser=None, dbPassword=None,
            jmsHost=None, jmsPort=None, receiveEha=None, receiveEvrs=None, receiveProducts=None,
            receiveFsw=None, receiveSse=None,
            fetchLad=None, fswHost=None, fswUplinkPort=None, sseHost=None, sseUplinkPort=None,
            enableConsoleLog=None, enableFileLog=None, enableDatabaseLog=None, disableInject=None,
            channelIds=None, modules=None, subsystems=None, opsCategories=None,
            defaultStringId=None, defaultScid=None, uplinkKeytabFile=None, uplinkUserRole=None, uplinkUsername=None, logFile=None, logTag=None, throwOnError=None):
    '''Start MTAK running so it will receive telemetry.  This should be called in the beginning of your script.
    This function creates an uplink proxy and a downlink proxy behind the scenes and makes their functionality
    available to the user. Keep in mind that when you call "startup", MTAK actually spawns two Java processes
    in the background (one for uplink and one for downlink).

    Immediately after MTAK startup begins, information from the GlobalLAD is queried for the latest available data. If you query
    for data before the asynchronous GlobalLAD query for the LAD data to yield results, MTAK get_* methods will return that
    the data doesn't exist when it could actually exist in the GlobalLAD but hasn't yet returned from the in-progress query

    By default, just calling "startup()" without the session key parameter will result in MTAK connecting to the default MPCS session
    (the current running session or the last ran session) based on the file pointed to by mpcsutil.config.getDefaultSessionConfigFilename()
    (this file is generally $HOME/CHILL/<hostname>_TempTestConfig.xml).  This means that if you bring up downlink and uplink separately
    (without using integrated chill), then MTAK will only pick up the configuration of the downlink session OR the uplink session (not both)
    depending on the order of startup. The best practice is to call startup with the key parameter so that you know exactly which session MTAK is
    connecting to.

    Calling startup multiple times will have no effect.

    The channelIds, modules, subsystems, and opsCategories options only pertain to EHA channel telemetry.  If more than 1 of
    these options are supplied, then telemetry satisfying EITHER of the conditions will be processed by MTAK.
    For instance, if channelIds="PWR-0001" and opsCategories="DMX", then both channels PWR-0001 and
    DMX-0009 will be processed by MTAK.

    Arguments
    ----------
    key - A numeric session key describing the session to connect to.  This will cause MTAK to go retrieve
    the session from the database. Should normally be used in conjunction with the "host" parameter because
    it takes a session key AND a session host to uniquely identify a database session. (optional) (unsigned int)

    host - A session host value describing the session to connect to.  Keep in mind this may not actually be your current host, but rather the
    value entered in the "host" field on the session that you're trying to connect to. (optional) (string)

    logDir - A string pointing to the desired log directory to write this log file to (make sure that if you use
    this parameter that you make sure the directory is writable by MTAK).  If this is not specify, it will default to
    the MPCS session output directory.  This cannot be specified if the logFile argument is specified. (optional) (string)

    dbHost - The database host for MTAK to connect to.  You don't generally want to mess with this, it's usually covered by values
    in the MPCS configuration files. This value will control what database MTAK looks at if you supply a session key/host. (optional) (string)

    dbPort - The database port for MTAK to connect to.  You don't generally want to mess with this, it's usually covered by values
    in the MPCS configuration files. This value will control what database MTAK looks at if you supply a session key/host.(optional) (unsigned int)

    dbUser - The database username MTAK will use.  You don't generally want to mess with this, it's usually covered by values
    in the MPCS configuration files. This value will control whow MTAK logs into the database if you supply a session key/host. (optional) (string)

    dbPassword - The database password MTAK will use.  You don't generally want to mess with this, it's usually covered by values
    in the MPCS configuration files. This value will control whow MTAK logs into the database if you supply a session key/host. (optional) (string)

    jmsHost - The JMS host MTAK will connect to for receiving telemetry.  You don't generally want to mess with this, it's usually covered by values
    in the MPCS configuration files. (optional) (string)

    jmsPort - The JMS port MTAK will connect to for receiving telemetry.  You don't generally want to mess with this, it's usually covered by values
    in the MPCS configuration files. (optional) (string)

    receiveEha - A boolean value indicating whether or not MTAK should process EHA telemetry. Used for improving performance when EHA is not
    important to the given test. By default, MTAK will receive & process EHA telemetry. (optional) (boolean)

    receiveEvrs - A boolean value indicating whether or not MTAK should process EVR telemetry. Used for improving performance when EVRs are not
    important to the given test. By default, MTAK will receive & process EVR telemetry. (optional) (boolean)

    receiveProducts - A boolean value indicating whether or not MTAK should process data product telemetry. Used for improving performance when products
    are not important to the given test. By default, MTAK will receive & process data products. (optional) (boolean)

    receiveFsw - A boolean value indicating whether or not MTAK should process FSW telemetry. Used for improving performance when FSW telemetry is not
    important to the given test.  By default, MTAK will receive & process FSW telemetry. (optional) (boolean)

    receiveSse - A boolean value indicating whether or not MTAK should process SSE telemetry. Used for improving performance when SSE telemetry is not
    important to the given test.  By default, MTAK will receive & process SSE telemetry. (optional) (boolean)

    fetchLad - A boolean value indicating whether or not MTAK should attempt to fetch the LAD on startup.  By default, MTAK will fetch the LAD and this
    will result in the call to startup taking awhile (10 seconds or so). (optional) (boolean)

    fswHost - The network host where FSW uplink should be sent.  Overrides the value in the session configuration. (optional) (string)

    fswUplinkPort - The port number where FSW uplink should be sent (on the fswUplinkHost). Overrides the value in the session
    configuration. (optional) (string)

    sseHost - The network host where SSE uplink should be sent.  Overrides the value in the session configuration. (optional) (string)

    sseUplinkPort - The port number where SSE uplink should be sent (on the sseUplinkHost). Overrides the value in the session
    configuration. (optional) (string)

    enableConsoleLog - Boolean value indicating whether or not logging should be done to the console. Default is True. (optional) (boolean)

    enableFileLog - Boolean value indicating whether or not logging should be done to a file. Default is True. (optional) (boolean)

    enableDatabaseLog - Boolean value indicating whether or not logging should be done to the database. Default is False. (optional) (boolean)

    disableInject - Boolean value that when set to True disables the inject_XXX methods. Default is False. (optional) (boolean)

    channelIds - MTAK should process EHA channels that have these channel ids. Used for improving performance when those channels
    are the only ones needed for the given test.  Can be a comma separated list or a range (ie C-123,B-111,D-123 or C-001..C-007).
    By default, MTAK will receive & process all EHA telemetry. (optional) (string)

    modules - MTAK should process EHA channels that belong to these modules. Used for improving performance when those modules
    are the only ones needed for the given test.  Can be a comma separated list.
    By default, MTAK will receive & process all EHA channel modules. (optional) (string)

    subsystems - MTAK should process EHA channels that belong to these subsystems. Used for improving performance when those subsystems
    are the only ones needed for the given test.  Can be a comma separated list.
    By default, MTAK will receive & process all EHA channel subsystems. (optional) (string)

    opsCategories - MTAK should process EHA channels that belong to these operational categories. Used for improving performance when those operational categories
    are the only ones needed for the given test.  Can be a comma separated list.
    By default, MTAK will receive & process all EHA channel operational categories. (optional) (string)

    defaultStringId - The default RCE string to target for all uplink sent using "send_fsw_*" functions. Changing this value will change the
    virtual channel ID portion of the telecommand frame header. Values are generally 'A','B','AB'.  Usually defaults to 'A' (mission-dependent). (optional) (string)

    defaultScid - The default spacecraft ID to use for all uplink sent using "send_fsw_*" functions.  Changing this value will change the SCID portion
    of the telecommand frame header.  Values are mission-specific. (optional) (unsigned int)

    uplinkKeytabFile - The path to the Kerberos keytab file used for uplink authentication with the Common Access Manager (CAM) when using COMMAND_SERVICE uplink connection type.
    Failure to specify a path to a valid keytab file will result in an error if the session's uplink connection type is COMMAND_SERVICE and security is
    enabled. (optional) (string)

    uplinkUserRole - The SCC role of the user that will be performing uplink. This role needs to be the role of the user whose keytab file is
    being used. (optional) (string)

    uplinkUsername - The username of the user that will be performing uplink. This is required when using keytab file authentication. (optional) (string)

    logFile - The absolute path to a log file that MTAK should use.  MTAK will append to the file if it already exists.  This cannot be specified if logDir is specified. (optional) (string)

    logTag - The tag to prepend to each log issued by MTAK. (optional) (string)

    throwOnError - Whether commanding operations should swallow exceptions and return False on failures. If not provided, MTAK sets this
    based on legacy behavior for the current mission. (optional) (boolean)

    Raises
    ------
    auto.err.AuthenticationError - if the keytab file provide does not contain the proper credentials for authentication.
    '''

    global _sessionConfig, _uplinkProxy, _downlinkProxy, _startTimeSecs, _logFile, _running, _disableInjectFncsFlag, _automaticStart, _error, _defaultStringId, _defaultScid,  _throwOnError, _useDownlink, _useUplink

    #Make sure we haven't already started up and if we did, warn the user
    if _running:
        _log().warning('MTAK wrapper has already been started using "startup" (it looks like you have called "startup" twice).' +
                     ' If you called another wrapper function before this, it likely has already called "startup".')
        return

    #Displaying log message if this startup was called automatically be a wrapper function
    if _automaticStart:
        _log().warning('The MTAK "startup" had not been called so MTAK automatically called "startup" with default parameters...')
        _automaticStart = False

    gdsConfig = mpcsutil.config.GdsConfig()

    #Store the time that the script started
    _startTimeSecs = time.time()

    #Parse all the input parameters into a usable form
    try:
        if dbHost is not None:
            mpcsutil.database.setDatabaseHost(dbHost)
        if dbPort is not None:
            mpcsutil.database.setDatabasePort(dbPort)
        if dbUser is not None:
            mpcsutil.database.setDatabaseUserName(dbUser)
        if dbPassword is not None:
            mpcsutil.database.setDatabasePassword(dbPassword)
        if jmsHost is not None:
            mpcsutil.config.setJmsHost(jmsHost)
        if jmsPort is not None:
            mpcsutil.config.setJmsPort(jmsPort)
        if receiveFsw is not None:
            mtak.setReceiveFsw(receiveFsw)
        if receiveSse is not None:
            mtak.setReceiveSse(receiveSse)
        if receiveEha is not None:
            mtak.setReceiveEha(receiveEha)
        if receiveEvrs is not None:
            mtak.setReceiveEvrs(receiveEvrs)
        if receiveProducts is not None:
            mtak.setReceiveProducts(receiveProducts)
        if fetchLad is not None:
            mtak.setFetchLad(fetchLad)
        if fswHost is not None:
            fswHost = str(fswHost)
        if fswUplinkPort is not None:
            fswUplinkPort = int(fswUplinkPort)
        if sseHost is not None:
            sseHost = str(sseHost)
        if sseUplinkPort is not None:
            sseUplinkPort = int(sseUplinkPort)
        if enableConsoleLog is not None:
            enableConsoleLog = mpcsutil.getBooleanFromString(enableConsoleLog)
        if enableFileLog is not None:
            enableFileLog = mpcsutil.getBooleanFromString(enableFileLog)
        if enableDatabaseLog is not None:
            enableDatabaseLog = mpcsutil.getBooleanFromString(enableDatabaseLog)
        if not mtak.getReceiveEha():
            mtak.setFetchLad(False)
        if disableInject is not None:
            _disableInjectFncsFlag = disableInject
        if channelIds is not None:
            mtak.setReceiveChannelIds(channelIds)
        if modules is not None:
            mtak.setReceiveModules(modules)
        if subsystems is not None:
            mtak.setReceiveSubsystems(subsystems)
        if opsCategories is not None:
            mtak.setReceiveOpsCategories(opsCategories)
        if defaultStringId is not None:
            _defaultStringId = str(defaultStringId).upper()
        if defaultScid is not None:
            _defaultScid = int(defaultScid)
        if throwOnError is not None:
            if type(throwOnError) is not bool:
                raise ValueError('Illegal value provided for argument "suppressErrors"')
            _throwOnError = throwOnError
        else:
            _throwOnError = _isThrowOnErrorMission()

    except Exception as exc:
        _error('Could not parse input parameters to startup(...) call.:\n{}'.format(exc))
        sys.exit(1)

    #Connect to an MPCS session
    try:
        if host is not None:
            host = str(host)
        if key is not None:
            key = int(key)
            _sessionConfig = mpcsutil.config.SessionConfig(key=key, host=host, validate_session=True)
        else:
            _sessionConfig = mpcsutil.config.SessionConfig(filename=mpcsutil.config.getDefaultSessionConfigFilename(), validate_session=True)
    except Exception as exc:
        sys.__stderr__.write('\nCould not connect to the supplied MPCS session. Ensure that you have a valid MPCS session to connect to (have you started MPCS?).\n{}'.format(exc))
        #NOTE: The loggers haven't been setup yet...this is why this is going to the console
        sys.exit(1)

    if fswHost is not None:
        _sessionConfig.fswUplinkHost = fswHost
    if fswUplinkPort is not None:
        _sessionConfig.fswUplinkPort = fswUplinkPort
    if sseHost is not None:
        _sessionConfig.sseHost = sseHost
    if sseUplinkPort is not None:
        _sessionConfig.sseUplinkPort = sseUplinkPort

    _sessionConfig.uplinkKeytabFile = uplinkKeytabFile
    _sessionConfig.uplinkUserRole = uplinkUserRole
    _sessionConfig.uplinkUsername = uplinkUsername

    _useUplink = _sessionConfig.fswUplinkHost is not None and _sessionConfig.uplinkConnectionType != 'UNKNOWN'
    _useDownlink = _sessionConfig.fswDownlinkHost is not None and _sessionConfig.downlinkConnectionType != 'UNKNOWN'

    #Create the uplink proxy and start it...this has to happen before creating log handlers
    #so the proxy that gets passed to the database log handler actually exists
    _uplinkProxy = mtak.factory.getUplinkProxy(_sessionConfig)
    _downlinkProxy = mtak.factory.getDownlinkProxy(_sessionConfig)

    #if the user specified both logDir and logFile, it is an illegal state so raise an error
    if logDir is not None and logFile is not None:
        raise mpcsutil.err.InvalidInitError("Cannot specify both logDir and logFile. Use one or the other.")

    #Create the log handler that will write to the MTAK wrapper log file
    _createWrapperLogHandlers(logDir, logFile, logTag, enableConsoleLog, enableFileLog, enableDatabaseLog)

    #Start the proxies running
    if _useDownlink:
        _downlinkProxy.start()
    if _useUplink:
        _uplinkProxy.start()

    #log a warning message
    if _sessionConfig.isUplinkOnly():
        _log().warning('This session is configured to be uplink only - downlink related capabilities may not function properly')
    elif _sessionConfig.isDownlinkOnly():
        _log().warning('This session is configured to be downlink only - uplink related capabilities may not function properly')

    _running = True

    #Write the initial session information to the script
    _log().info('''
############################
Script Startup Information
############################
MPCS Version = %s
MPCSTools Version = %s
CHILL_GDS = %s

Date/Time = %s
Script name = %s
Log name = %s

Username = %s
Host = %s
Session ID = %s
############################

''' % (str(mpcsutil.config.ReleaseProperties().version),
        str(mpcsutil.__version__),
        mpcsutil.chillGdsDirectory,
        mpcsutil.timeutil.getTimeString(seconds=_startTimeSecs),
        'Python Interactive Interpreter' if not sys.argv[0] else sys.argv[0],
        _logFile,
        mpcsutil.user,
        _sessionConfig.host,
        _sessionConfig.key))

    _log().info('startup(key="%s",logDir="%s",logFile="%s")' % (key, logDir, logFile))

    #This logic has to do with giving MTAK enough time to fetch the LAD before it starts doing business.
    #Essentially we try to wait for exactly two full heartbeat intervals and no longer because the LAD port
    #value comes in a heartbeat message.
    #
    #heartbeatTime = The number of milliseconds between heartbeat messages from chill_down
    if mtak.getFetchLad():
        heartbeatTime = long(gdsConfig.getProperty('general.context.heartbeatInterval', 0))
        _log().warning('Attempting to fetch LAD (this may take a few seconds)...')
        sleepTime = heartbeatTime * 2
        if sleepTime > 0:
            time.sleep(sleepTime / 1000) #Make sure we make this milliseconds instead of real seconds

    _log().info('MTAK has been started...')

def shutdown():
    '''Shutdown MTAK and stop the flow of telemetry.  This will destroy the uplink and downlink
    proxies that have been created and cause a script activity summary to be written to the log file.

    Calling shutdown if startup has not been called will have no effect. Calling shutdown multiple times
    will have no effect.

    Arguments
    ----------
    None

    Returns
    --------
    None'''

    global _sessionConfig, _uplinkProxy, _downlinkProxy, _endTimeSecs, _running, _logFile, _consoleLogHandler, _fileLogHandler, _databaseLogHandler, _disableInjectFncsFlag, _automaticStart, _useUplink, _useDownlink

    #Grab the time at which this script ended
    _endTimeSecs = time.time()

    if not _running:
        _log().warning('MTAK wrapper has already been shutdown or it has never been started.' +
                     ' It looks like you either called "shutdown" twice or you never called "startup".')
        return

    _running = False

    _log().info('shutdown()')

    # Write the script activity summary to the log file
    # MPCS-10779  4/1/19: This needs to happen before calling stop()
    _generateLogSummary()

    if _useDownlink and _downlinkProxy is not None:

        #Stop MTAK receiving telemetry by stopping the downlink proxy
        _downlinkProxy.stop()
        _downlinkProxy = None

    #We don't stop this until after generating the log summary because we want to
    #make sure the log summary gets into the database and that happens via the
    #uplink proxy
    if _useUplink and _uplinkProxy is not None:
        #Stop MTAK sending uplink
        _uplinkProxy.stop()
        _uplinkProxy = None

    for handler in [_consoleLogHandler, _fileLogHandler, _databaseLogHandler]:
        if handler in _log().handlers:
            _log().removeHandler(handler)
    _consoleLogHandler, _fileLogHandler, _databaseLogHandler = None, None, None
    #_log = None

    _disableInjectFncsFlag = False
    _automaticStart = False
    _sessionConfig = None
    _logFile = None

def get_down_proxy():
    '''Accessor method to retrieve the downlink proxy being used by the wrapper.  This method is
    provided for users who need to do more complicated operations that are not provided by the
    normal MTAK wrapper functions.

    IMPORTANT: Making changes to this object can adversely affect the operation of the rest of the
    MTAK wrapper functions.

    NOTE: This method will NOT call the MTAK "startup" method automatically if it has not been called already.

    Arguments
    ----------
    None

    Returns
    --------
    The downlink proxy object used by the wrapper (mtak.down.DownlinkProxy)'''

    global _downlinkProxy

    return _downlinkProxy

def get_up_proxy():
    '''Accessor method to retrieve the uplink proxy being used by the wrapper.  This method is
    provided for users who need to do more complicated operations that are not provided by the
    normal MTAK wrapper functions.

    IMPORTANT: Making changes to this object can adversely affect the operation of the rest of the
    MTAK wrapper functions.

    NOTE: This method will NOT call the MTAK "startup" method automatically if it has not been called already.

    Arguments
    ----------
    None

    Returns
    --------
    The uplink proxy object used by the wrapper (mtak.up.UplinkProxy)'''

    global _uplinkProxy

    return _uplinkProxy

def get_session_config():
    '''Accessor method to retrieve the session configuration being used by the wrapper.  This method is
    provided for users who need to do more complicated operations that are not provided by the
    wrapper functions.

    This method will do things like let you retrieve the session key or your current venue
    (e.g. are you in TESTSET or TESTBED?)

    IMPORTANT: Making changes to this object can adversely affect the operation of the rest of the
    MTAK wrapper functions.

    NOTE: This method will NOT call the MTAK "startup" method automatically if it has not been called already.

    Arguments
    ----------
    None

    Returns
    --------
    The session configuration object used by the wrapper (mpcsutil.config.SessionConfig)'''

    global _sessionConfig

    return _sessionConfig

def mtak_version():
    '''Retrieve the current MPCSTools (MTAK) version.

    NOTE: This method will NOT call the MTAK "startup" method automatically if it has not been called already.

    Arguments
    ----------
    None

    Returns
    --------
    The current MPCSTools version (string).'''

    return str(mpcsutil.__version__)

def mpcs_version():
    '''Retrieve the current MPCS version.

    NOTE: This method will NOT call the MTAK "startup" method automatically if it has not been called already.

    Arguments
    ----------
    None

    Returns
    --------
    The current MPCS version (string).'''

    return str(mpcsutil.config.ReleaseProperties().version)

def _atexitCleanup():
    '''Cleanup method to be called when the script exits or is killed.  Attempts to cleanup
    MTAK resources (e.g. the uplink/downlink proxies).  This will hopefully cover cases where the user
    forgot to call shutdown or Ctrl-C's the script.

    IMPORTANT: This method is only used internally, DO NOT call it directly.

    Arguments
    ----------
    None

    Returns
    --------
    None'''

    global _running

    if _running:

        _log().info('MTAK received a shutdown request, terminating gracefully...')
        shutdown()

#Try to register an exit handler that will call shutdown so we can
#be sure everything terminates properly on exit
try:
    atexit.register(_atexitCleanup)
except:
    pass

def _checkRunning():
    '''Check if MTAK is currently running. This is a dumb check that simply checks the global
    _running environment variable (a boolean) that is toggled on & off by the startup(...) and
    shutdown(...) methods.  If MTAK is not currently running, this method will start it running by passing the default information
    to startup.

    This method is used by most other wrapper methods to call startup before they start to make sure that startup
    gets called if the user forgot to call it.

    IMPORTANT: This method is only used internally, DO NOT call it directly.

    Arguments
    ----------
    None

    Returns
    --------
    None'''

    global _running, _automaticStart

    if not _running:
        _automaticStart = True
        startup()


##############################################
#
# Utility Functions
#
##############################################
def wait(seconds=None, until=None, updateFreq=None):
    '''Pause the script execution for a period of time.  The input is a value in seconds of how long to wait
       (as an integer or floating point number) or a string representation of a wall clock time to wait until.

      The "seconds" argument says "wait this many seconds."

      The "until" argument says "wait until this time."

      You should call this method with either the "seconds" argument OR the "until" argument, NOT both. Calling
      this method with both arguments or no arguments will result in an error.

    NOTE: This will NOT call the MTAK "startup" method automatically if it has not been called already. Be aware
    that if you call this method before "startup" has been called, no log messages will be written to your MTAK
    log files.

      Arguments
      ----------
      seconds - The number of seconds to wait (can be fractional). (optional) (float)

      until - The string representation (in ISO or DOY format) of the time to wait until. (e.g. 2009-202T12:35:00) (optional) (string)

      updateFreq - The number of seconds between displayed updates of the remaining time left to wait (optional) (float)

      Returns
      --------
      None'''

    global _running

    #_checkRunning()

    _log().info('wait(seconds="%s",until="%s",updateFreq="%s")' % (seconds, until, updateFreq))

    if seconds is not None and until is not None:
        _error('The wait(...) functions takes either a "seconds" argument OR an "until" argument, NOT both.')
        return
    elif seconds is None and until is None:
        _error('The wait(...) functions takes either a "seconds" argument OR an "until" argument, but no values were supplied.')
        return

    if seconds is not None:
        seconds_to_wait = float(seconds)

    else:
        until = str(until)
        seconds_to_wait = mpcsutil.timeutil.getSleepTime(until)

        if seconds_to_wait < 0:
            _warn("The until time supplied occurs in the past.  Thus, no wait will occur.")
            return

    if updateFreq is not None:
        updateFreq = float(updateFreq)
        if updateFreq > seconds_to_wait:
            _warn('The input update frequency "%s" is greater than the actual wait time of "%s" seconds.' % (updateFreq, seconds_to_wait))
            updateFreq = None

    if updateFreq is not None:

        numIter = int(seconds_to_wait // updateFreq)     # // divides and then rounds down
        remainTime = seconds_to_wait % updateFreq

        print("%s seconds remaining to wait..." % (seconds_to_wait))
        for i in range(1, numIter + 1):
            time.sleep(updateFreq)
            print("%s seconds remaining to wait..." % (str(seconds_to_wait - updateFreq * i)))

        time.sleep(remainTime)

    else:
        time.sleep(seconds_to_wait)

def wait_sclk(ticks=None, until=None):
    '''Pause the script execution for a period of time.  The input is a value in SCLK ticks of how long to wait
       (as an integer or floating point number) or a string/numeric representation of a SCLK time to wait until.

       The "ticks" arguments says "wait this many SCLK ticks."

       The "until" argument says "wait until this SCLK time."

      WARNING: This method is dependent on the SCLK times delivered in telemetry.  If no telemetry is flowing,
      this method could theoretically wait indefinitely. SCLK times are only read from realtime EHA and EVR telemetry,
      so if MTAK is not getting realtime EHA or EVRs, this method could theoretically wait indefinitely.

      You should call this method with either the "ticks" argument OR the "until" argument, NOT both. Calling
      this method with both arguments or no arguments will result in an error.

      NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
      been called already.

      Arguments
      ----------
      ticks - The number of SCLK ticks to wait (can be fractional). (optional) (float)

      until - The SCLK time to wait until formatted either as an integer, a float, or a string in
              the format of ticks<separator>subticks (separator is usually something like . or -, e.g. '52003-4325) (optional) (string)

      Returns
      --------
      None'''

    global _running, _downlinkProxy

    _checkRunning()

    _log().info('wait_sclk(ticks="%s",until="%s")' % (ticks, until))

    if ticks is not None and until is not None:
        _log().error('The wait_sclk(...) function takes either a "ticks" argument OR an "until" argument, NOT both.')
        return
    elif ticks is None and until is None:
        _log().error('The wait_sclk(...) function takes either a "ticks" argument OR an "until" argument, but no arguments were supplied.')
        return

    if ticks is not None:

        ticks = float(ticks)
        _downlinkProxy.waitBySclk(ticks)

    else:

        until = str(until)
        _downlinkProxy.waitUntilSclk(until)


##############################################
#
# User Interaction Functions
#
##############################################
def prompt(message='Please enter a value: '):
    '''Convenience function will pause the script to prompt the user to enter a value on the console and then hand
    back the value that was entered (as a string).

    NOTE: This will NOT call the MTAK "startup" method automatically if it has not been called already. Be aware
    that if you call this method before "startup" has been called, no log messages will be written to your MTAK
    log files.

    Arguments
    ----------
    message - The message to display to the user (defaults to "Please enter a value: ") (optional) (string)

    Returns
    --------
    The value entered by the user (string)'''

    global _running

    #_checkRunning()

    _log().info('prompt(message="%s")' % (message))

    #Prompt the user for a value from the console
    #(and strip the newline character off the end)
    print (message)
    value = sys.stdin.readline()[:-1]

    _log().info('Read in value %s' % (value))
    return value

def wait_for_key_press(message='Press any key to continue...'):
    '''Convenience function that will pause script execution until the user presses a key.

    NOTE: This will NOT call the MTAK "startup" method automatically if it has not been called already. Be aware
    that if you call this method before "startup" has been called, no log messages will be written to your MTAK
    log files.

    Arguments
    ----------
    message - The message to display to the user (default to "Press any key to continue...") (optional) (string)

    Returns
    --------
    None'''

    global _running

    #_checkRunning()

    _log().info('wait_for_key_press(message="%s")' % (message))

    #write out the prompt
    sys.stdout.write(message)
    sys.stdout.flush()

    #save terminal setup
    terminalAttrs = termios.tcgetattr(sys.stdin.fileno())

    #catch things like Ctrl-C as well as normal keystrokes...stay in this loop
    #until a key is pressed
    tty.setraw(sys.stdin.fileno())
    while True:
        (inputReady, dummy_outputReady, dummy_exceptionReady) = select.select((sys.stdin,), (), (), 1.0)
        if sys.stdin in inputReady:
            break

    #restore terminal setup
    termios.tcsetattr(sys.stdin.fileno(), termios.TCSAFLUSH, terminalAttrs)

    sys.stdout.write('\n')
    _log().info('Detected key press')

##############################################
#
# Communication Functions
#
##############################################

def send_email(to=None, subject=None, message=None, cc=None, bcc=None, sender=None):
    '''
    Send an email message via SMTP.

    NOTE: Script must have access to the network mail server for this to work (e.g. isolated networks
    like ATLO may not be able to use this capability).

    You must supply at least one of the "to", "cc" and "bcc" fields.

    You must supply at least one of the "subject" and "message" fields.

    Arguments
    ----------
    to - A list of recipients for the "to" field of the email (comma-separated). (string) (optional)
    subject - The subject line of the email. (string) (optional)
    message - The body text of the email. (string) (optional)
    cc - A list of recipients for the "CC" field of the email (comma-separated). (string) (optional)
    bcc - A list of recipients for the "BCC" field of the email (comma-separated). (string) (optional)
    sender - The string that will show up as the "sender" of this email. Does not have to actually exist;
    could be something like "MTAK@is-awesome.com". Will default to "<unix_username>@jpl.nasa.gov" (string) (optional)
    '''

    global _running

    _log().info('send_email(to=%s,subject=%s,message=%s,cc=%s,bcc=%s,sender=%s' % (to, subject, message, cc, bcc, sender))

    mail_host = mpcsutil.config.GdsConfig().getProperty('notification.email.host', 'smtp.jpl.nasa.gov')

    if to is None and cc is None and bcc is None:
        _error('Email transmission failed: No recipients specified.')
        return False
    elif subject is None and message is None:
        _error('Email transmission failed: Your subject and message cannot both be empty.')
        return False

    if message is None:
        message = ''
    message = str(message)

    #Construct the actual email message object based on the text we got from the user
    msg = email.mime.text.MIMEText(message)
    to_addresses = []

    if to is not None:
        msg['To'] = str(to)
        to_addresses.extend(msg['To'].split(','))

    if subject is not None:
        subject = str(subject)
        msg['Subject'] = subject

    if cc is not None:
        msg['Cc'] = str(cc)
        to_addresses.extend(msg['Cc'].split(','))

    if bcc is not None:
        msg['Bcc'] = str(bcc)
        to_addresses.extend(msg['Bcc'].split(','))

    if sender is None:
        sender = '%s@jpl.nasa.gov' % (mpcsutil.user)
    sender = str(sender)
    msg['From'] = sender

    smtp = None
    try:
        try:
            smtp = smtplib.SMTP(host=mail_host)
            bad_addresses = smtp.sendmail(sender, to_addresses, msg.as_string())
            if bad_addresses:
                for address in bad_addresses:
                    err_code, err_string = bad_addresses[address]
                    _warn('Email transmission to %s failed with error code %s: %s' % (address, err_code, err_string))
            if len(bad_addresses) == len(to_addresses):
                    return False

        except smtplib.SMTPException as e:
            _error('Email transmission failed: %s' % (e))
            return False
    finally:
        if smtp:
            smtp.quit()

    _log().info('Transmission succeeded.')

    return True

##############################################
#
# Uplink Functions
#
##############################################

def send_cmd_list_file(filename, validate=None, stringId=None, virtualChannel=None, scid=None, uplinkRates=None, waitForRadiation=0, throwOnError=None):
    '''Send a command list file (equivalent to the -f option of chill_send_cmd or the "Load cmd list..." button
    on the main chill_up GUI.

    Using a file input, commands should be formatted as they would be for the send_hw_cmd, send_fsw_cmd and
    send_sse_cmd functions.  These commands should be placed in an ASCII text file with each individual command
    newline delimited. Any line in this file beginning with "//" will be treated as a comment. All types of commands
    may be interleaved in command input files. The order of commands in the input file is maintained when parsing and sending
    commands.

    This file can contain commands destined for FSW, SSE or both.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    filename - The input command list file whose contents should be transmitted.

    validate - True if the commands in the file should be validated, false otherwise. Only applies to FSW commands,
    not SSE commands.  Defaults to "true." (optional) (boolean)

    stringId - The RCE string to target with all the commands in the file (desinted for FSW). Changing this value will change the
    virtual channel ID portion of the telecommand frame header. Values are generally 'A','B','AB'.
    Defaults to the value provided to "defaultStringId" in the "startup" function or 'A' otherwise. (optional) (string)

    virtualChannel - The virtual channel # to send this uplink on. Values are mission-dependent.
    This will only affect commands desinted for FSW. Value should be an integer. (optional) (unsigned int)

    scid - The spacecraft ID to target with this transmission.  Changing this value will change the
    SCID portion of the telecommand frame header.  This will only affect commands destined for FSW.
    Values are mission-specific. (optional) (unsigned int)

    uplinkRates - A list of uplink rates in bits per second that the request may be radiated with
    Only applicable with uplinkConnectionType=COMMAND_SERVICE. Defaults to ANY if not specified.
    (optional) (list of floats)

    waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
    This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
    uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
    (optional) (positive int)

    throwOnError - Whether commanding operations should swallow exceptions and return False on failures. If not provided, the default as set in the
    startup function is used (optional) (boolean)

    Returns
    --------
    True if the entire command list file transmitted successfully, false otherwise. (boolean)

    Raises
    ------
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to perform this function.
    auto.err.CommandServiceError -  if the directive is received by the command serivce, but an error is encountered by the command service while processing the directive.
    auto.err.ConnectionError - if MTAK is unable to contact the command service at the specified/configured uplink host/port.
    '''

    global _uplinkProxy, _running, _defaultStringId, _defaultScid,  _throwOnError

    _checkRunning()

    _log().info('send_cmd_list_file(filename="%s",validate="%s",stringId="%s",virtualChannel="%s",waitForRadiation=%d,throwOnError=%s)' %
              (filename, validate, stringId, virtualChannel, waitForRadiation, throwOnError))
    if filename is None or not os.path.exists(filename):
        _log().error('The input filename "%s" does not exist.' % (filename))

    if stringId is None and _defaultStringId is not None:
        stringId = _defaultStringId

    if scid is None and _defaultScid is not None:
        scid = _defaultScid

    if throwOnError is None:
        throwOnError = _throwOnError

    with open(filename, 'r') as ff:
        _log().info('Command List File Contents:\n\t{}'.format('\t'.join(_line.strip() for _line in ff.readlines())))

    #Send the command and check the success
    success = False
    try:
        _uplinkProxy.setUplinkRates(uplinkRates)
        _uplinkProxy.sendCommandListFile(filename, validate=validate, stringId=stringId, virtualChannel=virtualChannel, scid=scid, waitForRadiation=waitForRadiation)
        success = True

        _log().info('Transmission succeeded')

    except (mtak.err.MtakError, mpcsutil.err.InvalidStateError, NotImplementedError) as e:

        success = False
        _log().error('%s' % (e))

    except (auto.err.AmpcsError, auto.err.AUTOError) as e:
        success = False
        _log().error('%s' % (e))
        if throwOnError:
            raise e

    return success

send_cmd_list = send_cmd_list_file

def send_hw_cmd(command, stringId=None, virtualChannel=None, scid=None, uplinkRates=None, waitForRadiation=0, throwOnError=None):
    '''Send a hardware command to the flight system.  The input value should be the command stem (hardware commands have no arguments).

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    command - The command stem of the command to send (hardware commands have no arguments). (required) (string)

    validate - True if the command should be validated, false otherwise. Defaults to "true." (optional) (boolean)

    stringId - The RCE string to target with this command. Changing this value will change the
    virtual channel ID portion of the telecommand frame header. Values are generally 'A','B','AB'.
    Defaults to the value provided to "defaultStringId" in the "startup" function or 'A' otherwise. (optional) (string)

    virtualChannel - The virtual channel # to send this uplink on. Mission-dependent, but HW commands
    normally transmit on VC #0. Value should be an integer. (optional) (unsigned int)

    scid - The spacecraft ID to target with this transmission.  Changing this value will change the
    SCID portion of the telecommand frame header.  Values are mission-specific. (optional) (unsigned int)

    uplinkRates - A list of uplink rates in bits per second that the request may be radiated with
    Only applicable with uplinkConnectionType=COMMAND_SERVICE. Defaults to ANY if not specified.
    (optional) (list of int)

    waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
    This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
    uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
    (optional) (positive int)

    throwOnError - Whether commanding operations should swallow exceptions and return False on failures. If not provided, the default as set in the
    startup function is used (optional) (boolean)

    Returns
    --------
    True if the command transmitted successfully, false otherwise. (boolean)

    Raises
    ------
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to perform this function.
    auto.err.CommandServiceError -  if the directive is received by the command serivce, but an error is encountered by the command service while processing the directive.
    auto.err.ConnectionError - if MTAK is unable to contact the command service at the specified/configured uplink host/port.'''

    global _uplinkProxy, _running, _defaultStringId, _defaultScid, _throwOnError

    _checkRunning()

    _log().info('send_hw_cmd(command="%s",stringId="%s",virtualChannel="%s",waitForRadiation=%d,throwOnError=%s)' %
              (command, stringId, virtualChannel, waitForRadiation, throwOnError))

    if stringId is None and _defaultStringId is not None:
        stringId = _defaultStringId

    if scid is None and _defaultScid is not None:
        scid = _defaultScid

    if throwOnError is None:
        throwOnError = _throwOnError

    #Send the command and check the success
    success = False
    try:
        _uplinkProxy.setUplinkRates(uplinkRates)
        _uplinkProxy.sendFlightCommand(command, stringId=stringId, virtualChannel=virtualChannel, scid=scid, waitForRadiation=waitForRadiation)
        success = True

        _log().info('Transmission succeeded')

    except (mtak.err.MtakError, mpcsutil.err.InvalidStateError, NotImplementedError) as e:

        success = False
        _log().error('%s' % (e))

    except (auto.err.AmpcsError, auto.err.AUTOError) as e:
        success = False
        _log().error('%s' % (e))
        if throwOnError:
            raise e

    return success

def send_fsw_cmd(command, validate=None, stringId=None, virtualChannel=None, scid=None, uplinkRates=None, waitForRadiation=0, throwOnError=None):
    '''Send a flight software command to the flight system.  The command format used
    should be the same as used with chill_up (e.g. stem,arg_val1,...,arg_valN).

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    command - The command, with arguments, to send.  See the chill_up user guide page for more formatting details. (string)
    validate - True if argument values should be validated against the command dictionary, false otherwise.
    By default, validation will be done. (required) (boolean)

    validate - True if the command should be validated, false otherwise. Defaults to "true." (optional) (boolean)

    stringId - The RCE string to target with this command. Changing this value will change the
    virtual channel ID portion of the telecommand frame header. Values are generally 'A','B','AB'.
    Defaults to the value provided to "defaultStringId" in the "startup" function or 'A' otherwise. (optional) (string)

    virtualChannel - The virtual channel # to send this uplink on. Mission-dependent, but FSW commands
    normally transmit on VC #1. Value should be an integer. (optional) (unsigned int)

    scid - The spacecraft ID to target with this transmission.  Changing this value will change the
    SCID portion of the telecommand frame header.  Values are mission-specific. (optional) (unsigned int)

    uplinkRates - A list of uplink rates in bits per second that the request may be radiated with
    Only applicable with uplinkConnectionType=COMMAND_SERVICE. Defaults to ANY if not specified.
    (optional) (list of int)

    waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
    This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
    uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
    (optional) (positive int)

    throwOnError - Whether commanding operations should swallow exceptions and return False on failures. If not provided, the default as set in the
    startup function is used (optional) (boolean)

    Returns
    --------
    True if the command transmitted successfully, false otherwise. (boolean)

    Raises
    ------
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to perform this function.
    auto.err.CommandServiceError -  if the directive is received by the command serivce, but an error is encountered by the command service while processing the directive.
    auto.err.ConnectionError - if MTAK is unable to contact the command service at the specified/configured uplink host/port.'''

    global _uplinkProxy, _running, _defaultStringId, _defaultScid, _throwOnError

    _checkRunning()

    _log().info('send_fsw_cmd(command="%s",validate="%s",stringId="%s",virtualChannel="%s",waitForRadiation=%d,throwOnError=%s)' %
              (command, validate, stringId, virtualChannel, waitForRadiation, throwOnError))

    if stringId is None and _defaultStringId is not None:
        stringId = _defaultStringId

    if scid is None and _defaultScid is not None:
        scid = _defaultScid

    if throwOnError is None:
       throwOnError = _throwOnError

    #Send the command and check the success
    success = False
    try:
        _uplinkProxy.setUplinkRates(uplinkRates)
        _uplinkProxy.sendFlightCommand(command, validate=validate, stringId=stringId, virtualChannel=virtualChannel, scid=scid, waitForRadiation=waitForRadiation)
        success = True

        _log().info('Transmission succeeded')

    except (mtak.err.MtakError, mpcsutil.err.InvalidStateError, NotImplementedError) as e:

        success = False
        _log().error('%s' % (e))

    except (auto.err.AmpcsError, auto.err.AUTOError) as e:
        _log().error('%s' % (e))
        success = False
        if throwOnError:
            raise e

    return success

def send_fsw_file(source, target, type=None, overwrite=None, stringId=None, virtualChannel=None, scid=None, uplinkRates=None, waitForRadiation=0, throwOnError=None):
    '''Send a file load to the flight software (sequence file, parameter file, etc.).

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    source - The full path to the local file on disk that is to be transmitted. (required) (string)

    target - The full path to the location on the flight system where the file is to be placed. (required) (string)

    type - The type of the file being uploaded. Defaults to "0".  For most missions, "1" denotes a sequence file and 2 through
    255 are various other types. Keyword values such as "SEQUENCE" are also accepted (see the MPCS configuration file
    or the chill_up GUI for more details). The value of this field will be used to populate the 7-bit file type field
    in the file load header. (optional) (int or string)

    overwrite - True if this file load should overwrite an existing file in the target location, false otherwise.
    Defaults to False. The value of this field will be used to populate the 1-bit overwrite flag in the
    file load header. (optional) (boolean)

    stringId - The RCE string to target with this command. Changing this value will change the
    virtual channel ID portion of the telecommand frame header. Values are generally 'A','B','AB'.
    Defaults to the value provided to "defaultStringId" in the "startup" function or 'A' otherwise. (optional) (string)

    virtualChannel - The virtual channel # to send this uplink on. Mission-dependent, but file loads
    normally transmit on VC #2. Value should be an integer. (optional) (unsigned int)

    scid - The spacecraft ID to target with this transmission.  Changing this value will change the
    SCID portion of the telecommand frame header.  Values are mission-specific. (optional) (unsigned int)

    uplinkRates - A list of uplink rates in bits per second that the request may be radiated with
    Only applicable with uplinkConnectionType=COMMAND_SERVICE. Defaults to ANY if not specified.
    (optional) (list of int)

    waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
    This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
    uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
    (optional) (positive int)

    throwOnError - Whether commanding operations should swallow exceptions and return False on failures. If not provided, the default as set in the
    startup function is used (optional) (boolean)

    Returns
    --------
    True if the file transmitted successfully, false otherwise. (boolean)

    Raises
    ------
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to perform this function.
    auto.err.CommandServiceError -  if the directive is received by the command serivce, but an error is encountered by the command service while processing the directive.
    auto.err.ConnectionError - if MTAK is unable to contact the command service at the specified/configured uplink host/port.'''

    global _uplinkProxy, _running, _defaultStringId, _defaultScid, _throwOnError

    _checkRunning()

    _log().info('send_fsw_file(source="%s",target="%s",type="%s",overwrite="%s",stringId="%s",virtualChannel="%s",waitForRadiation=%d,throwOnError=%s)' %
              (source, target, type, overwrite, stringId, virtualChannel, waitForRadiation, throwOnError))

    if stringId is None and _defaultStringId is not None:
        stringId = _defaultStringId

    if scid is None and _defaultScid is not None:
        scid = _defaultScid

    if throwOnError is None:
        throwOnError = _throwOnError

    #Send the file load and check the success
    success = False
    try:
        _uplinkProxy.setUplinkRates(uplinkRates)
        _uplinkProxy.sendFile(source=source, target=target, type=type, overwrite=overwrite, stringId=stringId, virtualChannel=virtualChannel, scid=scid, waitForRadiation=waitForRadiation)
        success = True

        _log().info('Transmission succeeded')

    except (mtak.err.MtakError, mpcsutil.err.InvalidStateError, NotImplementedError) as e:

        success = False
        _log().error('%s' % (e))

    except (auto.err.AmpcsError, auto.err.AUTOError) as e:
        success = False
        _log().error('%s' % (e))
        if throwOnError:
            raise e

    return success

def send_fsw_scmf(filename, disableChecks=None, uplinkRates=None, waitForRadiation=0, throwOnError=None):
    '''Send the contents of an SCMF to the flight software.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    filename - The full path to the SCMF file on disk. (required) (string)

    disableChecks - Disable checksum validation and dictionary version validation on the SCMF. Used to send
    eithe rintentionally corrupted SCMFs or SCMFs built for previous FSW versions to the FSW.
    If set to true, this will also cause MTAK to ignore bad checksums in the SCMF or dictionary
    version mismatches between the current running FSW version and the version used to generate
    the SCMF. Defaults to false. (optional) (boolean)

    uplinkRates - A list of uplink rates in bits per second that the request may be radiated with
    Only applicable with uplinkConnectionType=COMMAND_SERVICE. Defaults to ANY if not specified.
    (optional) (list of int)

    waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
    This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
    uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
    (optional) (positive int)

    throwOnError - Whether commanding operations should swallow exceptions and return False on failures. If not provided, the default as set in the
    startup function is used (optional) (boolean)

    Returns
    --------
    True if the SCMF transmitted successfully, false otherwise. (boolean)

    Raises
    ------
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to perform this function.
    auto.err.CommandServiceError -  if the directive is received by the command serivce, but an error is encountered by the command service while processing the directive.
    auto.err.ConnectionError - if MTAK is unable to contact the command service at the specified/configured uplink host/port.'''

    global _uplinkProxy, _running, _throwOnError

    _checkRunning()

    _log().info('send_fsw_scmf(filename="%s",disableChecks="%s",waitForRadiation=%d, throwOnError=%s)' % (filename, disableChecks, waitForRadiation, throwOnError))

    if throwOnError is None:
        throwOnError = _throwOnError

    #Send the SCMF and check the success
    success = False
    try:
        _uplinkProxy.setUplinkRates(uplinkRates)
        _uplinkProxy.sendScmf(filename, disableChecks=disableChecks, waitForRadiation=waitForRadiation)
        success = True

        _log().info('Transmission succeeded')

    except (mtak.err.MtakError, mpcsutil.err.InvalidStateError, NotImplementedError) as e:

        success = False
        _log().error('%s' % (e))

    except (auto.err.AmpcsError, auto.err.AUTOError) as e:
        success = False
        _log().error('%s' % (e))
        if throwOnError:
            raise e

    return success

def send_fsw_raw_data(filename, hex=None, uplinkRates=None, waitForRadiation=0, throwOnError=None):
    '''Send an uplink raw data file to the flight software.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    filename - The full path to the raw data file on disk. (required) (string)

    hex - Indicate whether the file contains raw binary values or ASCII hex values.  True if the file contains
    ASCII hex values, false if the file should be sent as-is. (optional) (boolean)

    uplinkRates - A list of uplink rates in bits per second that the request may be radiated with
    Only applicable with uplinkConnectionType=COMMAND_SERVICE. Defaults to ANY if not specified.
    (optional) (list of int)

    waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
    This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
    uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
    (optional) (positive int)

    throwOnError - Whether commanding operations should swallow exceptions and return False on failures. If not provided, the default as set in the
    startup function is used (optional) (boolean)

    Returns
    --------
    True if the raw data file transmitted successfully, false otherwise. (boolean)

    Raises
    ------
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to perform this function.
    auto.err.CommandServiceError -  if the directive is received by the command serivce, but an error is encountered by the command service while processing the directive.
    auto.err.ConnectionError - if MTAK is unable to contact the command service at the specified/configured uplink host/port.'''

    global _uplinkProxy, _running, _throwOnError

    _checkRunning()

    _log().info('send_fsw_raw_data(filename="%s",hex="%s",waitForRadiation=%d,throwOnError=%s)' % (filename, hex, waitForRadiation, throwOnError))

    if throwOnError is None:
        throwOnError = _throwOnError

    #Send the raw data file and check the success
    success = False
    try:
        _uplinkProxy.setUplinkRates(uplinkRates)
        _uplinkProxy.sendRawDataFile(filename, hex=hex, waitForRadiation=waitForRadiation)
        success = True

        _log().info('Transmission succeeded')

    except (mtak.err.MtakError, mpcsutil.err.InvalidStateError, NotImplementedError) as e:

        success = False
        _log().error('%s' % (e))

    except (auto.err.AmpcsError, auto.err.AUTOError) as e:
        success = False
        _log().error('%s' % (e))
        if throwOnError:
            raise e

    return success

def send_sse_cmd(command, throwOnError=None):
    '''Send a directive to the SSE.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Unlike when using chill_up directly, the input string to this function
    does not need to begin with the SSE prefix (usually "sse:"), but if it does
    begin with the prefix, it will not hurt anything. Use of the prefix is optional.

    Arguments
    ----------
    command - The full directive, with arguments, to transmit. (required) (string)

    throwOnError - Whether commanding operations should swallow exceptions and return False on failures. If not provided, the default as set in the
    startup function is used (optional) (boolean)

    Returns
    --------
    True if the SSE command transmitted successfully, false otherwise. (boolean)'''

    global _uplinkProxy, _running, _throwOnError

    _checkRunning()

    gdsConfig = mpcsutil.config.GdsConfig()
    if not gdsConfig.hasSse() and not gdsConfig.isSse():
        _log().critical('Cannot send SSE command.  The current mission does not have an SSE.')
        return

    if throwOnError is None:
        throwOnError = _throwOnError

    _log().info('send_sse_cmd(command="%s,throwOnError=%s")' % (command,throwOnError))

    #Send the command and check the success
    success = False
    try:

        _uplinkProxy.sendSseCommand(command)
        success = True

        _log().info('Transmission succeeded')

    except (mtak.err.MtakError, mpcsutil.err.InvalidStateError, NotImplementedError) as e:

        success = False
        _log().error('%s' % (e))

    except (auto.err.AmpcsError, auto.err.AUTOError) as e:
        success = False
        _log().error('%s' % (e))
        if throwOnError:
            raise e

    return success

def cfdp_put(sourceFileName,
    destinationEntity=None,
    destinationEntityMnemonic=None,
    destinationFileName=None,
    serviceClass=0,
    messagesToUser=None,
    waitForCompletion=0,
    cfdpProcessorBaseUrl=None,
    throwOnError=None):
    '''Initiate a CFDP PUT user action to the CFDP Processor.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    NOTE: Either the destinationEntity or destinationEntityMnemonic must be supplied as parameters

    Arguments
    ----------
    sourceFileName - Source file name (path) relative to the configured uplink files directory (required) (string)

    destinationEntity - The CFDP entity ID of the destination (optional) (int)
        If this argument is not provided, a valid destinationEntityMnemonic must be

    destinationEntityMnemonic - The CFDP entity ID mnemonic for the destination (optional) (string)
        If this argument is not provided, a valid destinationEntity must be

    destinationFileName - Destination file name; if not supplied, sourceFileName will be used (optional) (string)

    serviceClass - 1 for Unacknowledged mode, 2 for Acknowledged mode, and 0 to use CFDP Processor's configured default. Default is 0. (optional) (int: 0, 1, or 2)

    messagesToUser - A single "messages to user" (MTU) string OR a list of strings. Default is None.
        (required when cfdp.processor.mtu.always.required=True, optional otherwise)    (string or list of strings)

    waitForCompletion - Number of seconds to wait for transaction finished indication to arrive after issuing the CFDP PUT.
    Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds. (optional) (positive int)

    cfdpProcessorBaseUrl - Base URL for the CFDP Processor, overrides the configured setting (optional) (string)

    throwOnError - Whether exceptions should be swalled and return False on failures. If not provided, the default as set
    in the startup function is used (optional) (boolean)

    Returns
    --------
    If waitForCompletion <= 0, True if the CFDP PUT operation was successful, false otherwise.
    If waitForCompletion > 0, True if the transaction finished within the allotted time. (boolean)

    Raises
    ------
    ValueError - if argument value(s) is/are illegal
    RuntimeError - if waiting for transaction to complete and transaction is abandoned or faulted
    '''

    global _uplinkProxy, _running, _throwOnError, _gdsConfig, _sessionConfig

    _checkRunning()

    _log().info('cfdp_put(destinationEntity="%s",destinationEntityMnemonic="%s",sourceFileName="%s",destinationFileName="%s",'\
            'serviceClass=%d,messagesToUser="%s",waitForCompletion=%d,cfdpProcessorBaseUrl="%s",throwOnError=%s)' %
              (destinationEntity, destinationEntityMnemonic, sourceFileName, destinationFileName, serviceClass, messagesToUser,
                waitForCompletion, cfdpProcessorBaseUrl, throwOnError))

    gdsConfig = mpcsutil.config.GdsConfig()

    if throwOnError is None:
       throwOnError = _throwOnError

    if serviceClass < 0 or serviceClass > 2:
        _log().error("CFDP service class must be either 1 or 2 (or 0 to use CFDP Processor's default) but %d was provided"
                     % serviceClass)
        if throwOnError:
            raise ValueError("CFDP service class must be either 1 or 2 (or 0 to use CFDP Processor's default) but %d was provided"
                             % serviceClass)
        return False

    # MPCS-12066 1/2021
    # Added support for mnemonic -> ID mapping
    if destinationEntity is None:
        if destinationEntityMnemonic is None:
            _log().error("CFDP destination Entity must be provided as a valid integer ID or configured mnemonic")
            if throwOnError:
                raise ValueError("CFDP destination Entity must be provided as a valid integer ID or configured mnemonic")
            return False
        else:
            mappedMnemonic = gdsConfig.getProperty('cfdpCommon.mnemonic.{}.entity.id'.format(destinationEntityMnemonic))
            _log().debug("mapped mnemonic ID for %s is %s " % (destinationEntityMnemonic, mappedMnemonic))

            if mappedMnemonic is None:
                _log().error("No valid destination Entity=%s configuration found for the provided mnemonic %s "
                             % (mappedMnemonic, destinationEntityMnemonic))
                if throwOnError:
                    raise ValueError("No valid destination Entity=%s configuration found for the provided mnemonic %s "
                                     % (mappedMnemonic, destinationEntityMnemonic))
                return False
            else:
                destinationEntity = int(mappedMnemonic)

    # MPCS-12066 1/2021
    # destinationEntity is now mapped from config if it was not provided in lieu of a mnemonic
    # need to ensure destinationEntity is a valid integer
    if not isinstance(int(destinationEntity), int):
        _log().error("Invalid destination Entity %s, value must be an integer" % destinationEntity)
        if throwOnError:
            raise ValueError("Invalid destination Entity %s, value must be an integer" % destinationEntity)
        return False


    if cfdpProcessorBaseUrl is None:
        cfdpProcessorBaseUrl = gdsConfig.getProperty('automationApp.mtak.cfdp.cfdpProcessorBaseUrl', default='http://localhost:8080/cfdp')

    # Compose the JSON object to POST, as a dictionary
    putDict = { 'requesterId': getpass.getuser(),
                'destinationEntity': destinationEntity,
               'sourceFileName': sourceFileName,
                'serviceClass': serviceClass,
                'sessionKey': _sessionConfig.key }

    # MPCS-12052 1/2021 - Added support for "Messages To User".
    #   If it has been specified, add to the request
    # REWORK: Don't validate against "is mtu required" config because it would mean:
    #   1) Introducing another duplicate MTAK property (see URL above) OR..
    #   2) Querying CFDP processor for the value before each PUT
    # NOTE: GdsConfig() gets properties from chill_property_dump
    #   For CFDP, that means it picks up all the 'init' properties
    #       NOT the currently configured value
    if messagesToUser is not None:
        # REST API Expects a list of objects for MTU
        putDict['messagesToUser'] = messagesToUser if isinstance(messagesToUser, list) else [messagesToUser]

    if destinationFileName:
        putDict['destinationFileName'] = destinationFileName
    else:
        putDict['destinationFileName'] = sourceFileName

    # These two are not supported at this time
    putDict['saveFirst'] = None
    putDict['saveFileAs'] = None

    _log().debug("CFDP PUT @ %s = %s" % (cfdpProcessorBaseUrl, putDict))
    # Now POST
    try:
        resp = requests.post(cfdpProcessorBaseUrl + '/action/put', json=putDict)
    except requests.ConnectionError as ce:
        _log().error('Could not send PUT request to CFDP Processor at %s. Check connection or check if CFDP Processor is running.' % (cfdpProcessorBaseUrl))
        _uplinkProxy.failedCfdpPuts += 1
        if throwOnError:
            raise RuntimeError('Could not send PUT request to CFDP Processor at %s. Check connection or check if CFDP Processor is running.' % (cfdpProcessorBaseUrl))
        return False

    if resp.status_code == 200:
        r = json.loads(resp.content)
        requestId = str(r['requestId'])
        sourceEntityId = str(r['newTransactionId'][0])
        transactionSequenceNumber = str(r['newTransactionId'][1])
        _log().info('CFDP PUT succeeded: requestId=%s transactionId=%s:%s'
            % (requestId, sourceEntityId, transactionSequenceNumber))
        _uplinkProxy.successfulCfdpPuts += 1

        if waitForCompletion > 0:
            _log().info('Waiting maximum %d seconds for CFDP transaction %s:%s to finish...'
                % (waitForCompletion, sourceEntityId, transactionSequenceNumber))
            result = wait_cfdp_ind(indicationTypeList=['tf','ft','ab'],
                sourceEntityId=sourceEntityId,
                transactionSequenceNumber=transactionSequenceNumber,
                timeout=waitForCompletion,
                lookback=5)

            if result:
                if result.indicationType == 'ab':
                    _log().error('CFDP transaction %s:%s was abandoned' % (sourceEntityId, transactionSequenceNumber))
                    if throwOnError:
                        raise RuntimeError('CFDP transaction %s:%s was abandoned' % (sourceEntityId, transactionSequenceNumber))
                    return False
                elif result.indicationType == 'ft':
                    _log().error('CFDP transaction %s:%s faulted' % (sourceEntityId, transactionSequenceNumber))
                    if throwOnError:
                        raise RuntimeError('CFDP transaction %s:%s faulted' % (sourceEntityId, transactionSequenceNumber))
                    return False
                else:
                    _log().info('CFDP transaction %s:%s completed successfully' % (sourceEntityId, transactionSequenceNumber))
                    return True
            else:
                _log().warning('CFDP transaction %s:%s did not finish before specified timeout' % (sourceEntityId, transactionSequenceNumber))
                return False
        else:
            return True

    else:
        _log().info('CFDP PUT failed: %s\n%s' % (resp, resp.text))
        return False


##############################################
#
# Logging Functions
#
##############################################

def console(message, level='INFO'):
    '''Write a message to the console and the MTAK log file.  NOTE: The message in
    the log file will be prepended with the text "User Console Message: " to distinguish it
    from normal MTAK-generated log messages.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    message - The message to write to the console (and elsewhere). (required) (string)

    level - The logging level of the message (e.g. DEBUG,INFO,WARNING,ERROR,CRITICAL). Defaults to INFO. (optional) (string)

    Returns
    --------
    None'''

    global _running

    _checkRunning()

    #Format inputs
    message = str(message)
    level = str(level)

    #Write to console
    print (message)

    #Write to log file
    _log().log(logging.getLevelName(level), 'User Console Message: ' + message)

def log(message, level='INFO'):
    '''Write a message to the MTAK log file.  NOTE: The message in
    the log file will be prepended with the text "User Log Message: " to distinguish it
    from normal log messages.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    message - The message to write to the log. (required) (string)

    level - The logging level of the message (e.g. DEBUG,INFO,WARNING,ERROR,CRITICAL). Defaults to INFO. (optional) (string)

    Returns
    --------
    None'''

    global _running

    _checkRunning()

    #Format inputs
    message = str(message)
    level = str(level)

    #Write to log file
    _log().log(logging.getLevelName(level), 'User Log Message: ' + message)

def get_log_dir():
    '''Accessor method to retrieve the log directory being used by the wrapper. This gives the directory location
    of where the MTAK log file(s) are being written.

    IMPORTANT: Changing the value returned by this function will have no effect on the MTAK wrapper. To change
    the log directory, you must do so during the startup function.

    NOTE: This function may return "None" if MTAK's "startup" has not been called yet.

    Arguments
    ----------
    None

    Returns
    --------
    The full path to the log directory currently being used by the MTAK wrapper. (string)'''

    global _sessionConfig

    #_checkRunning()

    #_log().info('get_log_dir()')

    logDir = None

    if _sessionConfig:
        logDir = _sessionConfig.logDir

    #_log().info('Log directory = %s' % (logDir))

    return logDir

def get_log_file():
    '''Accessor method to retrieve the log file being used by the wrapper. This gives the file location of
    where the MTAK log file is being written.

    IMPORTANT: Changing the value returned by this function will have no effect on the MTAK wrapper. To change
    the log location, you must do so during the call to the "startup" method.

    NOTE: This function may return "None" if MTAK's "startup" has not been called yet.

    Arguments
    ----------
    None

    Returns
    --------
    The full path to the log file currently being used by the MTAK wrapper. (string)'''

    global _logFile

    #_checkRunning()

    #_log().info('get_log_file()')
    #_log().info('Log file = %s' % (_logFile))

    return _logFile

def modify_log(type, enable=None, level=None):
    '''Method to modify the logging behavior of MTAK.  Currently MTAK has three loggers: one that writes to the console,
    one that writes to a file, and one that writes to the database.  Using this method you can enable/disable a logger or
    change the level of a logger.  For instance, if you wanted to turn off MTAK console output, you could do this:

    modify_log(type='console',enable='false')

    IMPORTANT: If you disabled a logger at startup by passing parameters to the "startup" method, you will not be able to
    re-enable that logger with this method.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ==========
    type - The type of logger you're modifying. This value should be either 'file', 'database' or 'console'. (required) (string)

    enable - A value of 'true' will (re)enable the given logger, a value of 'false' will disable the given logger. (optional) (boolean)

    level - The logging level of this logger (e.g. DEBUG,INFO,WARNING,ERROR,CRITICAL). (optional) (string)

    Returns
    --------
    None'''

    global _consoleLogHandler, _fileLogHandler, _databaseLogHandler, _logFile, _denyAllFilter

    _checkRunning()

    _log().info('modify_log(type="%s",enable="%s",level="%s")' % (type, enable, level))

    #Use the type to figure out if we're changing the console handler or file handler
    type = str(type).lower()
    handler = None
    if type == 'file':
        handler = _fileLogHandler
    elif type == 'console':
        handler = _consoleLogHandler
    elif type == 'database':
        handler = _databaseLogHandler
    else:
        _log().error('Unknown logger type "%s" specified. Valid values are "console", "file", or "database".' % type)
        return

    if enable is None and level is None:
        _log().error('No "enable" or "level" parameters were specified. Nothing can be modified.')
        return

    #Enable/disable the given logger if so desired
    if enable is not None:
        enable = mpcsutil.getBooleanFromString(enable)
        if not enable:
            if handler in _log().handlers:
                if not _denyAllFilter in handler.filters:
                    handler.addFilter(_denyAllFilter)
        else:
            if handler in _log().handlers:
                if _denyAllFilter in handler.filters:
                    handler.removeFilter(_denyAllFilter)

    #Change the level of the given logger if so desired
    if level is not None:
        level = str(level)
        handler.setLevel(logging.getLevelName(level))


##############################################
#
# EHA Functions
#
##############################################

def flush_eha():
    '''Clear all the EHA channel values from the current MTAK LAD.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.'''

    global _downlinkProxy, _running

    _checkRunning()

    _log().info('flush_eha()')

    _downlinkProxy.flush_eha()

def get_eha(channelId=None, name=None, realtime=True, recorded=False):
    '''Get the channel value object that corresponds to the input channel ID. The 'realtime' and 'recorded'
    values can be used to differentiate between if realtime or recorded values are desired. If both 'realtime' and
    'recorded' are set to True, then this function will check for a realtime value first and then a recorded
    value if the realtime one did not exist.

    IMPORTANT: You must supply either the "channelId" or "name" parameter to use this method.  If both of these parameters
    are specified, the channel name will be ignored.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already. See startup() documentation for more information about this.

    NOTE: This method will return the channel value object of the requested channel if and only if the channel has
    arrived at this process. If the channel has not arrived this query will return None. If MTAK should wait for EHA
    after finding none have arrived, use wait_eha with its lookback parameter.

    Arguments
    ----------
    channelId - The ID of the channel whose value should be retrieved. (case-sensitive) (optional) (string)

    name - The name of the channel whose value should be retrieved. (case-sensitive) (optional) (string)

    realtime - A boolean value that is True if a realtime value should be grabbed & false otherwise.
    Defaults to true. (optional) (boolean)

    recorded - A boolean value that is True if a recorded value should be grabbed & false otherwise. This value is
    overridden by the "realtime" flag.  If "realtime" is set to true, a realtime value will be returned if it exists. If realtime
    is set to True and there is no realtime value, then the recorded parameter will be checked if this parameter is True.
    Defaults to false. (optional) (boolean)

    Returns
    --------
    The channel value object of the requested channel or None if a value could not be found. (mpcsutil.channel.ChanVal).
    ChanVal objects contain raw data values that are not formatted according to the dictionary'''

    global _downlinkProxy, _running

    _checkRunning()

    #Format inputs
    if channelId is not None:
        channelId = str(channelId)
    if name is not None:
        name = str(name)

    if channelId is None and name is None:
        _log().error('You must specify either the "channelId" or "name" parameters to the "get_eha" method.')
        return None

    realtime = mpcsutil.getBooleanFromString(realtime)
    recorded = mpcsutil.getBooleanFromString(recorded)

    value = None

    _log().info('get_eha(channelId="%s",name="%s",realtime="%s",recorded="%s")' % (channelId, name, realtime, recorded))

    if not realtime and not recorded:
        _log().error('Both the "realtime" and "recorded" flags were set to false. No value can be retrieved.')
        return

    #Check the realtime LAD for the value
    if realtime:
        _downlinkProxy.channelValueTable.lock.acquire()
        try:
            if channelId is not None:
                values = _downlinkProxy.channelValueTable.getById(channelId)
                if values:
                    value = values[0]
            else:
                values = _downlinkProxy.channelValueTable.getByName(name)
                if values:
                    value = values[0]
        finally:
            _downlinkProxy.channelValueTable.lock.release()

    #Check the recorded LAD for the value (if there was no realtime value)
    if value is None and recorded:
        _downlinkProxy.recordedChannelValueTable.lock.acquire()
        try:
            if channelId is not None:
                values = _downlinkProxy.recordedChannelValueTable.getById(channelId)
                if values:
                    value = values[0]
            else:
                values = _downlinkProxy.recordedChannelValueTable.getByName(name)
                if values:
                    value = values[0]
        finally:
            _downlinkProxy.recordedChannelValueTable.lock.release()

    if value is not None:
        _log().info('Found a channel value (DN="%s",EU="%s") for channel (ID=%s,name=%s) (realtime="%s")' % (str(value.dn), str(value.eu), str(channelId), str(name), str(value.realtime)))
        return value

    _log().warning('No value found for channel (ID=%s,name=%s)' % (channelId, name))
    return None

def get_eha_dn(channelId=None, name=None, realtime=True, recorded=False):
    '''Get the current DN value of a channel. A convenience method that calls "get_eha" and then just
    returns the DN value from the result.

    IMPORTANT: You must supply either the "channelId" or "name" parameter to use this method.  If both of these parameters
    are specified, the channel name will be ignored.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    channelId - The ID of the channel whose DN value should be retrieved. (case-sensitive) (optional) (string)

    name - The name of the channel whose DN value should be retrieved. (case-sensitive) (optional) (string)

    realtime - A boolean value that is True if a realtime value should be grabbed & false otherwise.
    Defaults to true. (optional) (boolean)

    recorded - A boolean value that is True if a recorded value should be grabbed & false otherwise. This value is
    overridden by the "realtime" flag.  If "realtime" is set to true, a realtime value will be returned if it exists. If realtime
    is set to True and there is no realtime value, then the recorded parameter will be checked if this parameter is True.
    Defaults to false. (optional) (boolean)

    Returns
    --------
    The raw DN value of the requested channel or None if a value could not be found.
    The DN value that is returned is not formatted according to the dictionary (varying types)'''

    global _log

    value = get_eha(channelId, name, realtime, recorded)
    if value is not None:
        return value.dn

    return None

def get_eha_eu(channelId=None, name=None, realtime=True, recorded=False):
    '''Get the current EU value of a channel. A convenience method that calls "get_eha" and then just
    returns the EU value from the result.

    IMPORTANT: You must supply either the "channelId" or "name" parameter to use this method.  If both of these parameters
    are specified, the channel name will be ignored.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    channelId - The ID of the channel whose EU value should be retrieved. (case-sensitive) (optional) (string)

    name - The name of the channel whose EU value should be retrieved. (case-sensitive) (optional) (string)

    realtime - A boolean value that is True if a realtime value should be grabbed & false otherwise.
    Defaults to true. (optional) (boolean)

    recorded - A boolean value that is True if a recorded value should be grabbed & false otherwise. This value is
    overridden by the "realtime" flag.  If "realtime" is set to true, a realtime value will be returned if it exists. If realtime
    is set to True and there is no realtime value, then the recorded parameter will be checked if this parameter is True.
    Defaults to false. (optional) (boolean)

    Returns
    --------
    The raw EU value of the requested channel or None if a value could not be found.
    The EU value returned is not formatted according to the dictionary (float)'''

    global _log

    value = get_eha(channelId, name, realtime, recorded)
    if value is not None:
        return value.eu

    return None

def verify_eha(channelId=None, name=None, dn=None, eu=None, dnStart=None, dnEnd=None, euStart=None, euEnd=None, realtime=True, recorded=False):
    '''Verify that the current value of a given channel has the given DN and/or EU values.

    IMPORTANT: You must supply either the "channelId" or "name" parameter to use this method.  If both of these parameters
    are specified, the channel name will be ignored.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    channelId - The ID of the channel whose DN and/or EU values should be verified. (case-sensitive) (optional) (string)

    name - The name of the channel whose DN and/or EU values should be verified. (case-sensitive) (optional) (string)

    dn - The desired DN value of the channel (optional) (various types)

    eu - The desired EU value of the channel (optional) (float)

    dnStart - Inclusive lower bound for the DN value (various types).

    dnEnd - Inclusive upper bound for the DN value (various types).

    euStart - Inclusive lower bound for the EU value (floating point)

    euEnd - Inclusive upper bound for the EU value (floating point)

    realtime - A boolean value that is True if a realtime value should be grabbed & false otherwise.
    Defaults to true. (optional) (boolean)

    recorded - A boolean value that is True if a recorded value should be grabbed & false otherwise. This value is
    overridden by the "realtime" flag.  If "realtime" is set to true, a realtime value will be returned if it exists. If realtime
    is set to True and there is no realtime value, then the recorded parameter will be checked if this parameter is True.
    Defaults to false. (optional) (boolean)

    Returns
    --------
    True if the actual channel values matched the desired inputs, False otherwise (boolean)'''

    global _downlinkProxy, _running

    _checkRunning()

    #Format inputs
    if channelId is not None:
        channelId = str(channelId)
    if name is not None:
        name = str(name)
    if channelId is None and name is None:
        _log().error('You must specify either the "channelId" or "name" parameter to the "verify_eha" method.')
        return None

    realtime = mpcsutil.getBooleanFromString(realtime)
    recorded = mpcsutil.getBooleanFromString(recorded)

    _log().info('verify_eha(channelId="%s",name="%s",dn="%s",eu="%s",dnStart="%s",dnEnd="%s",euStart="%s",euEnd="%s",realtime="%s",recorded="%s")' % (channelId, name, dn, eu, dnStart, dnEnd, euStart, euEnd, realtime, recorded))

    value = None

    if not realtime and not recorded:
        log.error('Both the "realtime" and "recorded" flags were set to false. No value can be retrieved.')
        return

    #Check the realtime LAD for the value
    if realtime:
        _downlinkProxy.channelValueTable.lock.acquire()
        try:
            if channelId:
                values = _downlinkProxy.channelValueTable.getById(channelId)
                if values:
                    value = values[0]
            else:
                values = _downlinkProxy.channelValueTable.getByName(name)
                if values:
                    value = values[0]
        finally:
            _downlinkProxy.channelValueTable.lock.release()

    #Check the recorded LAD for the value (if there was no realtime value)
    if value is None and recorded:
        _downlinkProxy.recordedChannelValueTable.lock.acquire()
        try:
            if channelId:
                values = _downlinkProxy.recordedChannelValueTable.getById(channelId)
                if values:
                    value = values[0]
            else:
                values = _downlinkProxy.recordedChannelValueTable.getByName(channelId)
                if values:
                    value = values[0]
        finally:
            _downlinkProxy.recordedChannelValueTable.lock.release()

    #Fail if no current value exists
    if value is None:
        _log().info('No values found for channel (ID=%s,name=%s). Verification failed.' % (channelId, name))
        return False

    #Format the DN
    dnVal = None
    if dn is not None:
        dnVal = mpcsutil.channel.formatDn(value.type, dn)

    dnStartVal = None
    if dnStart is not None:
        dnStartVal = mpcsutil.channel.formatDn(value.type, dnStart)

    dnEndVal = None
    if dnEnd is not None:
        dnEndVal = mpcsutil.channel.formatDn(value.type, dnEnd)

    #Format the EU
    euVal = None
    if eu is not None:
        euVal = float(eu)

    euStartVal = None
    if euStart is not None:
        euStartVal = float(euStart)

    euEndVal = None
    if euEnd is not None:
        euEndVal = float(euEnd)

    #Verify the input DN matches the actual DN
    if dnVal is not None and value.dn != dnVal:

        _log().info('DN value verification failed for channel (ID=%s,name=%s). Actual DN value of %s does not equal desired DN value of %s.'
                 % (channelId, name, value.dn, dnVal))
        return False

    #Test if DN is not within the lower bound
    if dnStartVal is not None and value.dn < dnStartVal:
        _log().info('DN value verification failed for channel (ID=%s,name=%s). Actual DN value of %s is less than lower bound DN value of %s.'
                 % (channelId, name, value.dn, dnStartVal))
        return False

    #Test if DN is not within the upper bound
    if dnEndVal is not None and value.dn > dnEndVal:
        _log().info('DN value verification failed for channel (ID=%s,name=%s). Actual DN value of %s is greater than upper bound DN value of %s.'
                 % (channelId, name, value.dn, dnEndVal))
        return False

    #Verify the input EU matches the actual EU
    if euVal is not None and value.eu != euVal:

        _log().info('EU value verification failed for channel (ID=%s,name=%s). Actual EU value %s does not equal desired EU value %s.'
                 % (channelId, name, value.eu, euVal))

        if value.eu is None:
            _log().warning('The channel (ID=%s,name=%s) has no defined DN-to-EU conversion' % (channelId, name))

        return False

    #Test if EU is not within the lower bound
    if euStartVal is not None and value.eu < euStartVal:
        _log().info('EU value verification failed for channel (ID=%s,name=%s). Actual EU value of %s is less than lower bound EU value of %s.'
                 % (channelId, name, value.eu, euStartVal))
        return False

    #Test if EU is not within the upper bound
    if euEndVal is not None and value.eu > euEndVal:
        _log().info('EU value verification failed for channel (ID=%s,name=%s). Actual EU value of %s is greater than upper bound EU value of %s.'
                 % (channelId, name, value.eu, euEndVal))
        return False

    if dnVal is None and dnStartVal is None and dnEndVal is None and euVal is None and euStartVal is None and euEndVal is None:
        _log().info('Verification succeeded.  The channel (ID=%s,name=%s) has a value set.' % (channelId, name))
    else:
        _log().info('Value verification succeeded. Channel (ID=%s,name=%s) has DN = %s and EU = %s' % (channelId, name, dnVal, euVal))

    return True

def wait_eha(channelId=None, name=None, dn=None, eu=None, dnStart=None, dnEnd=None, euStart=None, euEnd=None,
             ertStart=None, ertEnd=None, scetStart=None, scetEnd=None, sclkStart=None, sclkEnd=None, realtime=True, recorded=False,
             timeout=None, lookback=None, sclkTimeout=None, sclkLookback=None):
    '''Pause the script to wait for a particular EHA channel value to arrive.

    IMPORTANT: You must supply either the "channelId" or "name" parameter to use this method.  You can supply both, but since both are
    normally unique, you should generally only specify one or the other.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    channelId - The ID of the channel to wait for. (case-sensitive) (optional) (string)

    name - The name of the channel to wait for. (case-sensitive) (optional) (string)

    dn - The desired DN value of the channel. (optional) (various types)

    eu - The desired EU value of the channel. (optional) (int)

    dnStart - Inclusive lower bound for the DN value (optional) (various types).

    dnEnd - Inclusive upper bound for the DN value (optional) (various types).

    euStart - Inclusive lower bound for the EU value (optional) (floating point)

    euEnd - Inclusive upper bound for the EU value (optional) (floating point)

    ertStart - The lower bound on the ERT time of the EHA value to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    ertEnd - The upper bound on the ERT time of the EHA value to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    scetStart - The lower bound on the SCET time of the EHA value to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    scetEnd - The upper bound on the SCET time of the EHA value to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    sclkStart - The lower bound on the SCLK time of the EHA value to wait for.  The time should be formatted
    either as an integer, a float, or a string in the format of ticks<separator>subticks
    (separator is usually something like . or -, e.g. '52003-4325). (optional) (string)

    sclkEnd - The upper bound on the SCLK time of the EHA value to wait for.  The time should be formatted
    either as an integer, a float, or a string in the format of ticks<separator>subticks
    (separator is usually something like . or -, e.g. '52003-4325). (optional) (string)

    realtime - A boolean value that is set to True if a realtime value is desired. If this is false, realtime values
    will be rejected. Defaults to true. (optional) (boolean)

    recorded - A boolean value that is set to True if a recorded value is desired. If this is false, recorded values
    will be rejected. Defaults to false. (optional) (boolean)

    timeout - The length of time, in seconds, to wait for the value to arrive before timing out. Defaults to 1 minute.
    If a sclkTimeout value is specified, this value is ignored. (optional) (float)

    lookback - The length of time, in seconds, to look back in time for telemetry to make sure it didn't already
    arrive before this call was made. Defaults to 0. If a sclkLookback value is specified, this value is ignored. (optional) (int)

    sclkTimeout - The length of time, in SCLK ticks (can be fractional), to wait for the value to arrive before timing out. Keep in mind
    that telemetry must be flowing in order for this timeout to function properly because MTAK updates its SCLK values based on incoming
    telemetry values.  SCLK times are only read from realtime EHA and EVR telemetry, so if MTAK is not getting realtime EHA or EVRs, this
    method could theoretically wait indefinitely. (optional) (floating point)

    sclkLookback - The length of time, in SCLK ticks (can be fractional), to look back in time for telemetry to make sure it didn't already
    arrive before this call was made. (optional) (floating point)

    Returns
    --------
    The channel value object (mpcsutil.channel.ChanVal) that met the condition or None if the wait times out.
    ChanVal objects contain raw data values that are not formatted according to the dictionary'''

    global _downlinkProxy, _running

    _checkRunning()

    if channelId is not None:
        channelId = str(channelId)
    if name is not None:
        name = str(name)
    if channelId is None and name is None:
        _log().error('You must specify either the "channelId" or "name" parameter to the "wait_eha" method.')
        return None

    realtime = mpcsutil.getBooleanFromString(realtime)
    recorded = mpcsutil.getBooleanFromString(recorded)

    #Format the EU
    euVal = None
    if eu is not None:
        euVal = float(eu)

    euStartVal = None
    if euStart is not None:
        euStartVal = float(euStart)

    euEndVal = None
    if euEnd is not None:
        euEndVal = float(euEnd)

    ertExactStartVal = None
    if ertStart is not None:
        ertExactStartVal = mpcsutil.timeutil.parseTimeString(ertStart)

    ertExactEndVal = None
    if ertEnd is not None:
        ertExactEndVal = mpcsutil.timeutil.parseTimeString(ertEnd)

    scetExactStartVal = None
    if scetStart is not None:
        scetExactStartVal = mpcsutil.timeutil.parseTimeString(scetStart)

    scetExactEndVal = None
    if scetEnd is not None:
        scetExactEndVal = mpcsutil.timeutil.parseTimeString(scetEnd)

    sclkExactStartVal = None
    if sclkStart is not None:
        sclkExactStartVal = mpcsutil.timeutil.parseSclkString(sclkStart)

    sclkExactEndVal = None
    if sclkEnd is not None:
        sclkExactEndVal = mpcsutil.timeutil.parseSclkString(sclkEnd)

    realtimeVal = None
    if realtime and not recorded:
        realtimeVal = True
    elif not realtime and recorded:
        realtimeVal = False

    sclkTimeoutVal = None
    if sclkTimeout is not None:
        sclkTimeoutVal = float(sclkTimeout)

    sclkLookbackVal = None
    if sclkLookback is not None:
        sclkLookbackVal = float(sclkLookback)

    timeoutVal = None
    if timeout is not None:
        timeoutVal = int(timeout)
    elif sclkTimeoutVal is None:
        timeoutVal = mtak.defaultWaitTimeout
    if timeoutVal is not None and sclkTimeoutVal is not None:
        _log().warning("Supplied both timeout and sclkTimeout to wait_eha. Will ignore timeout and use sclkTimeout.")
        timeoutVal = None

    lookbackVal = None
    if lookback is not None:
        lookbackVal = int(lookback)
    elif sclkLookbackVal is None:
        lookbackVal = mtak.defaultWaitLookback
    if lookbackVal is not None and sclkLookbackVal is not None:
        _log().warning("Supplied both lookback and sclkLookback to wait_eha. Will ignore lookback and use sclkLookback.")
        lookbackVal = None

    _log().info(('wait_eha(channelId="%s",name="%s",dn="%s",eu="%s",dnStart="%s",dnEnd="%s",euStart="%s",euEnd="%s",' + \
              'ertStart="%s",ertEnd="%s",scetStart="%s",scetEnd="%s",sclkStart="%s",sclkEnd="%s",' + \
              'realtime="%s",recorded="%s",timeout="%s",lookback="%s",sclkTimeout="%s",sclkLookback="%s")') % \
              (channelId, name, dn, eu, dnStart, dnEnd, euStart, euEnd, ertStart, ertEnd,
               scetStart, scetEnd, sclkStart, sclkEnd, realtime, recorded,
               timeoutVal, lookbackVal, sclkTimeoutVal, sclkLookbackVal))

    #Create the wait condition and do the waiting
    waitCondition = mtak.wait.ChanValWait(channelId=channelId, name=name, dn=dn, eu=euVal, dnStart=dnStart, dnEnd=dnEnd, euStart=euStartVal, euEnd=euEndVal,
                                          ertExactStart=ertExactStartVal, ertExactEnd=ertExactEndVal,
                                          scetExactStart=scetExactStartVal, scetExactEnd=scetExactEndVal,
                                          sclkExactStart=sclkExactStartVal, sclkExactEnd=sclkExactEndVal,
                                          realtime=realtimeVal)
    result = _downlinkProxy.registerSyncWait(waitCondition, timeout=timeoutVal, lookback=lookbackVal, sclkTimeout=sclkTimeoutVal, sclkLookback=sclkLookbackVal)

    #Check the wait result
    if result:
        _log().info('Wait succeeded.')
    else:
        _log().info('Wait failed.')

    return result

def inject_eha(channelId=None, name=None, eventTime='', sclk='', ert='', scet='', type='',
               dnUnits='', euUnits='', status=None, dn=None, eu=None, alarms=[], realtime=True):
    '''Creates a fake EHA channel value.  This function is generally used for testing out your logic
    without having to have the associated telemetry to go with it.  This function will make MTAK act
    like it has received an EHA value that was not actually in the real telemetry stream. THIS ONLY
    AFFECTS THE CURRENT MTAK SCRIPT AND NOTHING ELSE.

    You could see an example usage as something like (in pseudocode):

    - startup MTAK
    - inject PWR-4000
    - test if/else logic with PWR-4000

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    channelId - The ID of the channel. (string) (required)
    name - The name of the channel. (case-sensitive) (optional) (string)
    eventTime - The time that MPCS sent out the JMS message for this value (string) (optional)
    sclk - The SCLK value for when this value was read (format CCCCCCCCCC[.-]FFF[FF] (string) (optional)
    ert - The ERT for when this value was received in ISO format (string) (optional)
    scet - The correlated SCLK time in ISO format (string) (optional)
    type - The type of this channel (known values are 'ascii','signed_int','float','double','boolean','bool','unsigned_int','status','digital') (string) (optional)
    dnUnits - The units for the DN (string) (optional)
    euUnits - The units for the EU (string) (optional)
    dn - The DN value for the channel...varies based on type (string, int, long, or float) (optional)
    eu - The EU value for the channel (float) (optional)
    status - The string value of the channel for table-driven channels (e.g. status, boolean, etc.) (optional)
    alarms - A list of any alarms set on this channel ( list of tuples [(alarmDef,alarmLevel)] ) (optional)

    Returns
    --------
    None'''

    global _downlinkProxy, _running, _disableInjectFncsFlag

    if _disableInjectFncsFlag:

        _log().warning("inject_XXX methods have been disabled by the user.  This call will have no affect.")

    else:

        _checkRunning()

        _log().info('inject_eha(channelId="%s", name="%s", eventTime="%s", sclk="%s", ert="%s", scet="%s", type="%s", dnUnits="%s", euUnits="%s", status="%s", dn="%s", eu="%s", alarms="%s",realtime="%s")'
                  % (channelId, name, eventTime, sclk, ert, scet, type, dnUnits, euUnits, status, dn, eu, alarms, realtime))

        if dn is None:
            _log().error('Could not inject EHA value for channelId (ID=%s,name=%s) because no DN value was specified.' % (channelId, name))
            return
        else:
            try:
                dn = mpcsutil.channel.formatDn(type, dn)
            except ValueError as ve:
                _log().error('Could not inject EHA value for channel (ID=%s,name=%s). %s' % (channelId, name, ve))
                return

        try:
            float(str(eu))
        except ValueError:
            if eu is not None:
                _log().error('Could not inject EHA value for channel (ID=%s,name=%s) because the EU value "%s" is not a valid floating point number.' % (channelId, name, eu))
                return

        #TODO: Calculate exact times for sclk/ert/scet/eventtime
        eventTimeExact = 0
        ertExact = 0
        scetExact = 0
        sclkExact = 0
        sclkCoarse = 0
        sclkFine = 0

        #Dictionary holding all the attribute information for this EHA
        d = {"eventTime": eventTime, "sclk": sclk, "ert": ert, "scet": scet, "channelId": channelId, "name": name, "type": type, "dnUnits": dnUnits,
             "euUnits":euUnits, "status": status, "dn":dn, "eu": eu, "alarms": alarms, "eventTimeExact":eventTimeExact,
             "ertExact":ertExact, "scetExact":scetExact, "realtime":mpcsutil.getBooleanFromString(realtime),
             "sclkExact":sclkExact, "sclkCoarse":sclkCoarse, "sclkFine":sclkFine, "injected":True}

        #Create a Channel and pass in the attributes as a variable-length list
        fakeChanVal = mpcsutil.channel.ChanVal(**d)

        _downlinkProxy._parseObject(fakeChanVal)
        #_downlinkProxy._parseMessage(fakeChanVal.toCsv())


##############################################
#
# EVR Functions
#
##############################################

def flush_evr():
    '''Clear all the EVRs from the current MTAK history.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.'''

    global _downlinkProxy, _running

    _checkRunning()

    _log().info('flush_evr()')

    _downlinkProxy.flush_evr()

def get_evr(name=None, eventId=None, level=None, module=None, ertStart=None, ertEnd=None,
            scetStart=None, scetEnd=None, sclkStart=None, sclkEnd=None, realtime=True, recorded=False, message=None, messageSubstr=None, maxNum=None):
    '''Get all the EVRs that match the input parameters.  Supplying no
    parameters will fetch all EVR(s).

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    name - The name of the EVR(s) to retrieve. (string)

    eventId - The event IDs of the EVR(s) to retrieve. (int)

    level - The level of the EVR(s) to retrieve. (string)

    module - The module of the EVR(s) to retrieve. (string)

    ertStart - The lower bound on the ERT time of the EVRs to retrieve. The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    ertEnd - The upper bound on the ERT time of the EVRs to retrieve. The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    scetStart - The lower bound on the SCET time of the EVRs to retrieve. The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    scetEnd - The upper bound on the SCET time of the EVRs to retrieve. The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    sclkStart - The lower bound on the SCLK time of the EVRs to retrieve. The time should be formatted
    either as an integer, a float, or a string in the format of ticks<separator>subticks
    (separator is usually something like . or -, e.g. '52003-4325). (optional) (string)

    sclkEnd - The upper bound on the SCLK time of the EVRs to retrieve. The time should be formatted
    either as an integer, a float, or a string in the format of ticks<separator>subticks
    (separator is usually something like . or -, e.g. '52003-4325). (optional) (string)

    realtime - A boolean value that is set to True if realtime EVRs are desired. If this is false, realtime EVRs will be ignored.
    Defaults to true. (optional) (boolean)

    recorded - A boolean value that is set to True if recorded EVRs are desired. If this is false, recorded EVRs will be ignored.
    Defaults to false. (optional) (boolean)

    message - The message of the EVR(s) to retrieve. (optional) (string)

    messageSubstr - The message substring of the EVR(s) to retrieve.  Thus, EVRs with messages that contain the supplied substring are retreieved. (optional) (string)

    maxNum - Is the maximum number of most recent EVRs that should be returned.  Must be greater than or equal to 1. (optional) (int)

    Returns
    --------
    A list of EVR objects that match the input criteria (list of mpcsutil.evr.Evr)'''

    global _downlinkProxy, _running

    _checkRunning()

    _log().info('get_evr(name="%s",eventId="%s",level="%s",module="%s",ertStart="%s",ertEnd="%s",'\
              'scetStart="%s",scetEnd="%s",sclkStart="%s",sclkEnd="%s",realtime="%s",recorded="%s",message="%s",messageSubstr="%s",maxNum="%s")' % \
              (name, eventId, level, module, ertStart, ertEnd, scetStart, scetEnd, sclkStart, sclkEnd, realtime, recorded, message, messageSubstr, maxNum))

    realtime = mpcsutil.getBooleanFromString(realtime)
    recorded = mpcsutil.getBooleanFromString(recorded)

    evrs = []

    #TODO:  is it a problem that the final list that comes back isn't necessarily time-ordered?
    # if realtime and recorded are both true we'll be concatenating together two sorted lists to make
    #one larger unsorted list
    if realtime:
        _downlinkProxy.evrTable.lock.acquire()
        try:
            evrs.extend(_downlinkProxy.evrTable.getByAttributes(name, eventId, level, module, ertStart, ertEnd, scetStart, scetEnd, sclkStart, sclkEnd, message, messageSubstr))
        finally:
            _downlinkProxy.evrTable.lock.release()

    if recorded:
        _downlinkProxy.recordedEvrTable.lock.acquire()
        try:
            evrs.extend(_downlinkProxy.recordedEvrTable.getByAttributes(name, eventId, level, module, ertStart, ertEnd, scetStart, scetEnd, sclkStart, sclkEnd, message, messageSubstr))
        finally:
            _downlinkProxy.recordedEvrTable.lock.release()

    if evrs:
        evrs.sort(key=operator.attrgetter('receiveTime'), reverse=True)    # sort based on receiveTime

        if maxNum is not None:
            if maxNum >= 1:
                evrs = evrs[:maxNum]    # Take the more recent evrs
            else:
                _log().warning('maxNum must be greater than or equal to 1.  All found evrs will be returned.')

        _log().info('Found %d EVR(s) that matched the input criteria' % (len(evrs)))
    else:
        _log().info('The desired EVR(s) could not be found.')

    return evrs

def wait_evr(name=None, eventId=None, level=None, module=None, message=None, ertStart=None, ertEnd=None,
             scetStart=None, scetEnd=None, sclkStart=None, sclkEnd=None, realtime=True, recorded=False,
             timeout=None, lookback=None, sclkTimeout=None, sclkLookback=None, messageSubstr=None):
    '''Pause the script to wait for a particular EVR to arrive.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    name - The name of the EVR to wait for. (string)

    eventId - The event ID of the EVR to wait for. (int)

    level - The level of the EVR to wait for. (optional) (string)

    module - The module of the EVR to wait for. (optional) (string)

    message - The message of the EVR to wait for (must match exactly!) (optional) (string)

    ertStart - The lower bound on the ERT time of the EVR to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    ertEnd - The upper bound on the ERT time of the EVR to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    scetStart - The lower bound on the SCET time of the EVR to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    scetEnd - The upper bound on the SCET time of the EVR to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    sclkStart - The lower bound on the SCLK time of the EVR to wait for.  The time should be formatted
    either as an integer, a float, or a string in the format of ticks<separator>subticks
    (separator is usually something like . or -, e.g. '52003-4325). (optional) (string)

    sclkEnd - The upper bound on the SCLK time of the EVR to wait for.  The time should be formatted
    either as an integer, a float, or a string in the format of ticks<separator>subticks
    (separator is usually something like . or -, e.g. '52003-4325). (optional) (string)

    realtime - A boolean value that is set to True if a realtime EVR is desired. If this is false, realtime EVRs
    will be rejected. Defaults to true. (optional) (boolean)

    recorded - A boolean value that is set to True if a recorded EVR is desired. If this is false, recorded EVRs
    will be rejected. Defaults to false. (optional) (boolean)

    timeout - The length of time, in seconds, to wait for the EVR to arrive before timing out. Defaults to 1 minute.
    If a sclkTimeout value is specified, this value is ignored. (optional) (float)

    lookback - The length of time, in seconds, to look back in time for the EVR to make sure it didn't already
    arrive before this call was made. Defaults to 0. If a sclkLookback value is specified, this value is ignored. (optional) (int)

    sclkTimeout - The length of time, in SCLK ticks (can be fractional), to wait for the EVR to arrive before timing out. Keep in mind
    that telemetry must be flowing in order for this timeout to function properly because MTAK updates its SCLK values based on incoming
    telemetry values. SCLK times are only read from realtime EHA and EVR telemetry, so if MTAK is not getting realtime EHA or EVRs, this
    method could theoretically wait indefinitely. (optional) (floating point)

    sclkLookback - The length of time, in SCLK ticks (can be fractional), to look back in time for the EVR to make sure it didn't already
    arrive before this call was made. (optional) (floating point)

    messageSubstr - The message substring of the EVR to wait for.  Thus, the EVR with a message that contains the supplied substring is waited on. (optional) (string)

    Returns
    --------
    The EVR object (mpcsutil.evr.Evr) that met the condition or None if the wait times out.'''

    global _downlinkProxy, _running

    _checkRunning()

    if eventId is not None:
        eventId = int(eventId)
    if name is not None:
        name = str(name)
    if eventId is None and name is None:
        _log().error('You must specify either the "eventId" or "name" parameter to the "wait_evr" method.')
        return None

    #Format input
    realtime = mpcsutil.getBooleanFromString(realtime)
    recorded = mpcsutil.getBooleanFromString(recorded)

    if level:
        level = str(level)

    if module:
        module = str(module)

    if message:
        message = str(message)

    ertExactStartVal = None
    if ertStart is not None:
        ertExactStartVal = mpcsutil.timeutil.parseTimeString(ertStart)

    ertExactEndVal = None
    if ertEnd is not None:
        ertExactEndVal = mpcsutil.timeutil.parseTimeString(ertEnd)

    scetExactStartVal = None
    if scetStart is not None:
        scetExactStartVal = mpcsutil.timeutil.parseTimeString(scetStart)

    scetExactEndVal = None
    if scetEnd is not None:
        scetExactEndVal = mpcsutil.timeutil.parseTimeString(scetEnd)

    sclkExactStartVal = None
    if sclkStart is not None:
        sclkExactStartVal = mpcsutil.timeutil.parseSclkString(sclkStart)

    sclkExactEndVal = None
    if sclkEnd is not None:
        sclkExactEndVal = mpcsutil.timeutil.parseSclkString(sclkEnd)

    realtimeVal = None
    if realtime and not recorded:
        realtimeVal = True
    elif not realtime and recorded:
        realtimeVal = False

    sclkTimeoutVal = None
    if sclkTimeout is not None:
        sclkTimeoutVal = float(sclkTimeout)

    sclkLookbackVal = None
    if sclkLookback is not None:
        sclkLookbackVal = float(sclkLookback)

    timeoutVal = None
    if timeout is not None:
        timeoutVal = int(timeout)
    elif sclkTimeoutVal is None:
        timeoutVal = mtak.defaultWaitTimeout
    if timeoutVal is not None and sclkTimeoutVal is not None:
        _log().warning("Supplied both timeout and sclkTimeout to wait_evr. Will ignore timeout and use sclkTimeout.")
        timeoutVal = None

    lookbackVal = None
    if lookback is not None:
        lookbackVal = int(lookback)
    elif sclkLookbackVal is None:
        lookbackVal = mtak.defaultWaitLookback
    if lookbackVal is not None and sclkLookbackVal is not None:
        _log().warning("Supplied both lookback and sclkLookback to wait_evr. Will ignore lookback and use sclkLookback.")
        lookbackVal = None

    if messageSubstr:
        messageSubstr = str(messageSubstr)

    _log().info(('wait_evr(name="%s",eventId="%s",level="%s",module="%s",message="%s",'\
              'ertStart="%s",ertEnd="%s",scetStart="%s",scetEnd="%s",sclkStart="%s",sclkEnd="%s",'\
              'realtime="%s",recorded="%s",timeout="%s",lookback="%s",sclkTimeout="%s",sclkLookback="%s",messageSubstr="%s")') % \
              (name, eventId, level, module, message, ertStart, ertEnd, scetStart, scetEnd,
               sclkStart, sclkEnd, realtime, recorded, timeout, lookback, sclkTimeout, sclkLookback, messageSubstr))

    #Create the wait condition and do the waiting
    waitCondition = mtak.wait.EvrWait(name=name, eventId=eventId, level=level, module=module, message=message,
                                      ertExactStart=ertExactStartVal, ertExactEnd=ertExactEndVal,
                                      scetExactStart=scetExactStartVal, scetExactEnd=scetExactEndVal,
                                      sclkExactStart=sclkExactStartVal, sclkExactEnd=sclkExactEndVal,
                                      realtime=realtimeVal, messageSubstr=messageSubstr)
    result = _downlinkProxy.registerSyncWait(waitCondition, timeout=timeoutVal, lookback=lookbackVal,
                                             sclkTimeout=sclkTimeoutVal, sclkLookback=sclkLookbackVal)

    #Check the results of the wait
    if result:
        _log().info('Wait succeeded.')
    else:
        _log().info('Wait failed.')

    return result

def inject_evr(name='', eventId=0, module='', level='', fromSse=False, eventTime='', eventTimeExact=0,
               ert='', ertExact=0, sclk='', scet='', scetExact=0, message='', metadata=[], realtime=True):
    '''Creates a fake event record (EVR).   This function is generally used for testing out your logic
    without having to have the associated telemetry to go with it.  This function will make MTAK act
    like it has received an EVR that was not actually in the real telemetry stream. THIS ONLY
    AFFECTS THE CURRENT MTAK SCRIPT AND NOTHING ELSE.

    You could see an example usage as something like (in pseudocode):

    - startup MTAK
    - inject an EVR
    - test if/else logic with that EVR

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    name - The name of the EVR (string)

    eventId - The EVR event ID (int)

    module - The FSW module that generated this EVR (string)

    level - The level of the EVR (string)

    fromSse - True if the EVR is from the SSE, false otherwise (Boolean)

    eventTime - The ISO-formatted time at which MPCS generated the EVR message for this EVR (string)

    ert - The ISO-formatted earth receive time for this EVR (string)

    sclk - The Spacecraft Clock Time for this EVR in the form CCCCCCCCCC-FFFFF(string)

    scet - The ISO-formatted spacecraft event time for this EVR (string)

    realtime - True if this is a realtime EVR, false otherwise (Boolean)

    message - The message associated with this EVR (string)

    metadata - A list of tuples containing associated pairs of metadata keywords and values ( type is [(string,string)] )

    Returns
    --------
    None'''

    global _downlinkProxy, _running, _disableInjectFncsFlag

    if _disableInjectFncsFlag:

        _log().warning("inject_XXX methods have been disabled by the user.  This call will have no affect.")

    else:

        _checkRunning()

        _log().info('inject_evr(name="%s", eventId="%s", module="%s", level="%s", fromSse="%s", eventTime="%s", ert="%s", sclk="%s", scet="%s", realtime="%s", message="%s", metadata="%s")'
                  % (name, eventId, module, level, fromSse, eventTime, ert, sclk, scet, realtime, message, metadata))

        #Dictionary holding all the attribute information for this EVR
        d = {"name": name, "eventId": eventId, "module": module, "level": level, "fromSse": fromSse, "eventTime": eventTime, "ert":ert,
             "sclk": sclk, "realtime": realtime, "message": message, "metadata": metadata, "eventTimeExact":eventTimeExact,
             "ertExact":ertExact, "scetExact":scetExact, "realtime":mpcsutil.getBooleanFromString(realtime), "injected":True}

        #Create an EVR and pass in the attributes as a variable-length list
        fakeEvr = mpcsutil.evr.Evr(**d)

        _downlinkProxy._parseObject(fakeEvr)
        #_downlinkProxy._parseMessage(fakeEvr.toCsv())


##############################################
#
# Product Functions
#
##############################################

def flush_dp():
    '''Clear all the data products from the current MTAK history.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.'''

    global _downlinkProxy, _running

    _checkRunning()

    _log().info('flush_dp()')

    _downlinkProxy.flush_dp()

def get_dp(name=None, transactionId=None, status=None, apid=None,
           ertStart=None, ertEnd=None, scetStart=None, scetEnd=None, sclkStart=None, sclkEnd=None, maxNum=None):
    '''Get all the received data products with the given characteristics.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    transactionId - The transaction ID of the product (optional) (string)

    status - The status of the product (optional) (string)

    apid - The APID of the product (optional) (int)

    ertStart - The lower bound on the ERT time of the data products to retrieve. The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    ertEnd - The upper bound on the ERT time of the data products to retrieve. The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    scetStart - The lower bound on the SCET time of the data products to retrieve. The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    scetEnd - The upper bound on the SCET time of the data products to retrieve. The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    sclkStart - The lower bound on the SCLK time of the data products to retrieve. The time should be formatted
    either as an integer, a float, or a string in the format of ticks<separator>subticks
    (separator is usually something like . or -, e.g. '52003-4325). (optional) (string)

    sclkEnd - The upper bound on the SCLK time of the data products to retrieve. The time should be formatted
    either as an integer, a float, or a string in the format of ticks<separator>subticks
    (separator is usually something like . or -, e.g. '52003-4325). (optional) (string)

    maxNum - Is the maximum number of most recent products that should be returned.  Must be greater than or equal to 1. (optional) (int)

    Returns
    --------
    A list of product objects that matched the input criteria (list of mpcsutil.product.Product)'''

    global _downlinkProxy, _running

    _checkRunning()

    if apid is not None:
        apid = int(apid)
    if name is not None:
        name = str(name)
    if apid is None and name is None:
        _log().error('You must specify either the "apid" or "name" parameter to the "get_dp" method.')
        return None

    _log().info('get_dp(name="%s",transactionId="%s",status="%s",apid="%s",'\
              'ertStart="%s",ertEnd="%s",scetStart="%s",scetEnd="%s",sclkStart="%s",sclkEnd="%s")' % \
              (name, transactionId, status, apid, ertStart, ertEnd, scetStart, scetEnd, sclkStart, sclkEnd))

    products = []

    _downlinkProxy.productTable.lock.acquire()
    try:
        products.extend(_downlinkProxy.productTable.getByAttributes(name, transactionId, status, apid, ertStart, ertEnd, scetStart, scetEnd, sclkStart, sclkEnd))
    finally:
        _downlinkProxy.productTable.lock.release()

    if products:
        products.sort(key=operator.attrgetter('receiveTime'), reverse=True)    # sort based on receiveTime

        if maxNum is not None:
            if maxNum >= 1:
                products = products[:maxNum]
            else:
                _log().warning('maxNum must be greater than or equal to 1.  All found products will be returned.')

        _log().info('Found %d product(s) that matched the input criteria' % (len(products)))
    else:
        _log().info('The desired product(s) could not be found.')

    return products

def wait_dp(name=None, transactionId=None, status=None, apid=None,
            ertStart=None, ertEnd=None, scetStart=None, scetEnd=None, sclkStart=None, sclkEnd=None,
            timeout=None, lookback=None, sclkTimeout=None, sclkLookback=None):
    '''Pause the script to wait for a particular data product message to arrive.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    transactionId - The transaction ID of the product (optional) (string)

    status - The status of the product (optional) (string)

    apid - The APID of the product (optional) (int)

    ertStart - The lower bound on the ERT time of the data product to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    ertEnd - The upper bound on the ERT time of the data product to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    scetStart - The lower bound on the SCET time of the data product to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    scetEnd - The upper bound on the SCET time of the data product to wait for.  The time should be formatted
    in DOY or ISO format. (e.g. 2009-202T12:35:00). (optional) (string)

    sclkStart - The lower bound on the SCLK time of the data product to wait for.  The time should be formatted
    either as an integer, a float, or a string in the format of ticks<separator>subticks
    (separator is usually something like . or -, e.g. '52003-4325). (optional) (string)

    sclkEnd - The upper bound on the SCLK time of the data product to wait for.  The time should be formatted
    either as an integer, a float, or a string in the format of ticks<separator>subticks
    (separator is usually something like . or -, e.g. '52003-4325). (optional) (string)

    timeout - The length of time, in seconds, to wait for the data product to arrive before timing out. Defaults to 1 minute.
    If a sclkTimeout value is specified, this value is ignored. (optional) (float)

    lookback - The length of time, in seconds, to look back in time for the data product to make sure it didn't already
    arrive before this call was made. Defaults to 0. If a sclkLookback value is specified, this value is ignored. (optional) (int)

    sclkTimeout - The length of time, in SCLK ticks (can be fractional), to wait for the data product to arrive before timing out. Keep in mind
    that telemetry must be flowing in order for this timeout to function properly because MTAK updates its SCLK values based on incoming
    telemetry values. SCLK times are only read from realtime EHA and EVR telemetry, so if MTAK is not getting realtime EHA or EVRs, this method
    could theoretically wait indefinitely. (optional) (floating point)

    sclkLookback - The length of time, in SCLK ticks (can be fractional), to look back in time for the data product to make sure it didn't already
    arrive before this call was made. (optional) (floating point)

    Returns
    --------
    The product object (mpcsutil.product.Product) that met the condition or None otherwise.'''

    global _downlinkProxy, _running

    _checkRunning()


    #Format inputs
    if name:
        name = str(name)

    if transactionId:
        transactionId = str(transactionId)

    if status:
        status = str(status)

    if apid:
        apid = int(apid)

    ertExactStartVal = None
    if ertStart is not None:
        ertExactStartVal = mpcsutil.timeutil.parseTimeStringExt(ertStart)

    ertExactEndVal = None
    if ertEnd is not None:
        ertExactEndVal = mpcsutil.timeutil.parseTimeStringExt(ertEnd)

    scetExactStartVal = None
    if scetStart is not None:
        scetExactStartVal = mpcsutil.timeutil.parseDvtString(scetStart) if mpcsutil.timeutil.dvtRegexpObj.match(scetStart) else mpcsutil.timeutil.parseTimeStringExt(scetStart)

    scetExactEndVal = None
    if scetEnd is not None:
        scetExactEndVal = mpcsutil.timeutil.parseDvtString(scetEnd) if mpcsutil.timeutil.dvtRegexpObj.match(scetEnd) else mpcsutil.timeutil.parseTimeStringExt(scetEnd)

    sclkExactStartVal = None
    if sclkStart is not None:
        sclkExactStartVal = mpcsutil.timeutil.parseSclkString(sclkStart)

    sclkExactEndVal = None
    if sclkEnd is not None:
        sclkExactEndVal = mpcsutil.timeutil.parseSclkString(sclkEnd)

    sclkTimeoutVal = None
    if sclkTimeout is not None:
        sclkTimeoutVal = float(sclkTimeout)

    sclkLookbackVal = None
    if sclkLookback is not None:
        sclkLookbackVal = float(sclkLookback)

    timeoutVal = None
    if timeout is not None:
        timeoutVal = int(timeout)
    elif sclkTimeoutVal is None:
        timeoutVal = mtak.defaultWaitTimeout
    if timeoutVal is not None and sclkTimeoutVal is not None:
        _log().warning("Supplied both timeout and sclkTimeout to wait_dp. Will ignore timeout and use sclkTimeout.")
        timeoutVal = None

    lookbackVal = None
    if lookback is not None:
        lookbackVal = int(lookback)
    elif sclkLookbackVal is None:
        lookbackVal = mtak.defaultWaitLookback
    if lookbackVal is not None and sclkLookbackVal is not None:
        _log().warning("Supplied both lookback and sclkLookback to wait_dp. Will ignore lookback and use sclkLookback.")
        lookbackVal = None

    _log().info(('wait_dp(name="%s",transactionId="%s",status="%s",apid="%s",' + \
              'ertStart="%s",ertEnd="%s",scetStart="%s",scetEnd="%s",sclkStart="%s",sclkEnd="%s",' + \
              'timeout="%s",lookback="%s",sclkTimeout="%s",sclkLookback="%s")') % \
              (name, transactionId, status, apid, ertStart, ertEnd, scetStart, scetEnd,
               sclkStart, sclkEnd, timeout, lookback, sclkTimeout, sclkLookback))

    #Create the wait condition and do the waiting
    waitCondition = mtak.wait.ProductWait(name=name, transactionId=transactionId, status=status, apid=apid,
                                          ertExactStart=ertExactStartVal, ertExactEnd=ertExactEndVal,
                                          dvtScetExactStart=scetExactStartVal, dvtScetExactEnd=scetExactEndVal,
                                          dvtSclkExactStart=sclkExactStartVal, dvtSclkExactEnd=sclkExactEndVal)
    result = _downlinkProxy.registerSyncWait(waitCondition, timeout=timeoutVal, lookback=lookbackVal,
                                             sclkTimeout=sclkTimeoutVal, sclkLookback=sclkLookbackVal)

    #Report the results
    if result:
        _log().info('Wait succeeded.')
    else:
        _log().info('Wait failed.')

    return result


def inject_dp(name='', transactionId='', apid=0, dvtCoarse=0, dvtFine=0, dvtSclk='', dvtScet='', dvtScetExact=0,
              eventTime='', eventTimeExact=0, ert='', ertExact=0, totalParts=0, \
              completeFile='', dataFile='', status='Complete', reason=''):
    '''Creates a fake data product (DP).  This function is generally used for testing out your logic
    without having to have the associated telemetry to go with it.  This function will make MTAK act
    like it has received a data product that was not actually in the real telemetry stream. THIS ONLY
    AFFECTS THE CURRENT MTAK SCRIPT AND NOTHING ELSE. (Note that the actual data product file will not exist on disk)

    You could see an example usage as something like (in pseudocode):

    - startup MTAK
    - inject data product
    - test if/else logic with data product

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    name - The product name (string)

    transactionId - The transaction ID unique to this product (string)

    apid - The APID of this product (int)

    dvtCoarse - The coarse portion of the DVT SCLK (int)

    dvtFine - The fine portion of the DVT SCLK (int)

    dvtSclk - The DVT SCLK in the form CCCCCCCCCC-FFFFF (string)

    dvtScet - The DVT Spacecraft Event Time (ISO format) (string)

    eventTime - The time (ISO format) that MPCS published the product message (string)

    totalParts - The total number of parts in this product (int)

    completeFile - The complete product file path (string)

    dataFile - The data file path (string)

    status - The current status of the product('NotStarted','InProgress','Complete','Partial') (string)

    reason - The reason a partial product was pushed out (string)

    Returns
    --------
    None'''

    global _downlinkProxy, _running, _disableInjectFncsFlag

    if _disableInjectFncsFlag:

        _log().warning("inject_XXX methods have been disabled by the user.  This call will have no affect.")

    else:
        _checkRunning()

        _log().info('inject_dp(name="%s", transactionId="%s", apid="%s", dvtCoarse="%s", dvtFine="%s", dvtSclk="%s", dvtScet="%s", eventTime="%s",'\
                  ' totalParts="%s", completeFile="%s", dataFile="%s", status="%s", reason="%s",'\
                  'eventTimeExact=%s,dvtScetExact=%s,ert="%s",ertExact=%s)' % \
                  (name, transactionId, apid, dvtCoarse, dvtFine, dvtSclk, dvtScet, eventTime,
                   totalParts, completeFile, dataFile, status, reason,
                   eventTimeExact, dvtScetExact, ert, ertExact))

        partialFiles = [dataFile, '', '']

        #Dictionary holding all the attribute information for this product
        d = {"name": name, "transactionId": transactionId, "apid": apid, "dvtCoarse": dvtCoarse, "dvtFine": dvtFine, "dvtSclk": dvtSclk,
             "dvtScet": dvtScet, "eventTime": eventTime, "totalParts": totalParts, "completeFile": completeFile,
             "partialFiles": partialFiles, "status": status, "reason": reason,
             'eventTimeExact':eventTimeExact, 'dvtScetExact':dvtScetExact, 'ert':ert, 'ertExact':ertExact, 'injected':True}

        #Create a Product and pass in the attributes as a variable-length list
        fakeDp = mpcsutil.product.Product(**d)

        #Convert the attributes to a comma separated format
        #_downlinkProxy._parseMessage(fakeDp.toCsv())
        _downlinkProxy._parseObject(fakeDp)

def get_cfdp_ind(indicationTypeList=None, sourceEntityId=None, transactionSequenceNumber=None, maxNum=None):
    '''Get all the received CFDP Indications with the given characteristics.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    indicationTypeList - The CFDP Indication types (optional) (list of strings)

    sourceEntityId - Source entity ID for the transaction (optional) (string)

    transactionSequenceNumber - The transaction sequence number (optional) (string)

    maxNum - Is the maximum number of most recent CFDP Indications that should be returned.  Must be greater than or equal to 1. (optional) (int)

    Returns
    --------
    A list of CFDP Indication objects that matched the input criteria (list of mpcsutil.cfdp.CfdpIndication)'''

    global _downlinkProxy, _running

    _checkRunning()

    _log().info('get_cfdp_ind(indicationTypeList=%s,sourceEntityId="%s",transactionSequenceNumber="%s")' % \
              (",".join([indType for indType in indicationTypeList]) if indicationTypeList != None else None,
                sourceEntityId, transactionSequenceNumber))

    cfdpIndications = []

    _downlinkProxy.cfdpIndicationTable.lock.acquire()
    try:
        ind_list = None
        if sourceEntityId and transactionSequenceNumber:
            ind_list = _downlinkProxy.cfdpIndicationTable.getByTransactionId(sourceEntityId, transactionSequenceNumber)
        elif sourceEntityId:
            ind_list = [ind for ind in _downlinkProxy.cfdpIndicationTable.to_list() if ind.sourceEntityId == sourceEntityId]
        elif transactionSequenceNumber:
            ind_list = [ind for ind in _downlinkProxy.cfdpIndicationTable.to_list() if ind.transactionSequenceNumber == transactionSequenceNumber]
        else:
            ind_list = _downlinkProxy.cfdpIndicationTable.to_list()

        if indicationTypeList:
            ind_list = [ind for ind in ind_list if ind.indicationType in indicationTypeList]

        cfdpIndications.extend(ind_list)
    finally:
        _downlinkProxy.cfdpIndicationTable.lock.release()

    if cfdpIndications:
        cfdpIndications.sort(key=operator.attrgetter('receiveTime'), reverse=True)    # sort based on receiveTime

        if maxNum is not None:
            if maxNum >= 1:
                cfdpIndications = cfdpIndications[:maxNum]
            else:
                _log().warning('maxNum must be greater than or equal to 1. All found CFDP Indications will be returned.')

        _log().info('Found %d CFDP Indication(s) that matched the input criteria' % (len(cfdpIndications)))
    else:
        _log().info('The desired CFDP Indications(s) could not be found.')

    return cfdpIndications

def wait_cfdp_ind(indicationTypeList=None, sourceEntityId=None, transactionSequenceNumber=None,
            timeout=None, lookback=None):
    '''Pause the script to wait for a particular CFDP Indication message to arrive.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    indicationTypeList - The CFDP Indication types (optional) (list of strings)

    sourceEntityId - Source entity ID for the transaction (optional) (string)

    transactionSequenceNumber - The transaction sequence number (optional) (string)

    timeout - The length of time, in seconds, to wait for the CFDP Indication to arrive before timing out. Defaults to 1 minute. (optional) (float)

    lookback - The length of time, in seconds, to look back in time for the CFDP Indication to make sure it didn't already
    arrive before this call was made. Defaults to 0. (optional) (int)

    Returns
    --------
    The CFDP Indication object (mpcsutil.cfdp.CfdpIndication) that met the condition or None otherwise.'''

    global _downlinkProxy, _running

    _checkRunning()

    timeoutVal = None
    if timeout is not None:
        timeoutVal = int(timeout)
    else:
        timeoutVal = mtak.defaultWaitTimeout


    lookbackVal = None
    if lookback is not None:
        lookbackVal = int(lookback)
    else:
        lookbackVal = mtak.defaultWaitLookback

    _log().info(('wait_cfdp_ind(indicationTypeList=%s,sourceEntityId="%s",transactionSequenceNumber="%s",' + \
              'timeout="%s",lookback="%s")') % \
              (",".join([indType for indType in indicationTypeList]) if indicationTypeList != None else None,
                sourceEntityId, transactionSequenceNumber, timeout, lookback))

    #Create the wait condition and do the waiting
    waitCondition = mtak.wait.CfdpIndicationWait(indicationTypeList=indicationTypeList,
        sourceEntityId=sourceEntityId,
        transactionSequenceNumber=transactionSequenceNumber)
    result = _downlinkProxy.registerSyncWait(waitCondition, timeout=timeoutVal, lookback=lookbackVal,
                                             sclkTimeout=None, sclkLookback=None)

    #Report the results
    if result:
        _log().info('Wait succeeded.')
    else:
        _log().info('Wait failed.')

    return result

def inject_cfdp_ind(indicationType='', sourceEntityId='', transactionSequenceNumber='', eventTime=''):
    '''Creates a fake CFDP Indication. This function is generally used for testing out your logic
    without having to have the associated JMS message flow to go with it. This function will make MTAK act
    like it has received a CFDP Indication that was not actually generated by the CFDP Processor. THIS ONLY
    AFFECTS THE CURRENT MTAK SCRIPT AND NOTHING ELSE.

    You could see an example usage as something like (in pseudocode):

    - startup MTAK
    - inject CFDP Indication
    - test if/else logic with CFDP Indication

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    indicationType - The CFDP Indication type (string)

    sourceEntityId - Source entity ID for the transaction (string)

    transactionSequenceNumber - The transaction sequence number (string)

    eventTime - The time (ISO format) that CFDP Processor published the CFDP Indication message (string)

    Returns
    --------
    None'''

    global _downlinkProxy, _running, _disableInjectFncsFlag

    if _disableInjectFncsFlag:

        _log().warning("inject_XXX methods have been disabled by the user.  This call will have no affect.")

    else:
        _checkRunning()

        _log().info('inject_cfdp_ind(indicationType="%s", sourceEntityId="%s",'\
            ' transactionSequenceNumber="%s", eventTime="%s")' % \
                  (indicationType, sourceEntityId, transactionSequenceNumber, eventTime))

        #Dictionary holding all the attribute information for this CFDP Indication
        d = {"indicationType": indicationType,
        "sourceEntityId": sourceEntityId,
        "transactionSequenceNumber": transactionSequenceNumber,
        "eventTime": eventTime,
        'injected':True}

        #Create a CfdpIndication and pass in the attributes as a variable-length list
        fakeCfdpInd = mpcsutil.cfdp.CfdpIndication(**d)

        _downlinkProxy._parseObject(fakeCfdpInd)


##############################################
#
# Misc Telemetry Functions
#
##############################################

def flush_all():
    '''Clear all the telemetry (EHA, EVRs, data products) from the current MTAK history.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.'''

    global _downlinkProxy, _running

    _checkRunning()

    _log().info('flush_all()')

    _downlinkProxy.flush_all()

##############################################
#
# Unix Interactive Functions
#
##############################################

def shell_exec(command):
    '''Exec a Unix shell command and continue running the script.  No information is provided directly back to the user on
    whether or not the command succeeded.

    NOTE: This method will NOT call the MTAK "startup" method automatically if it has not been called already. Be aware
    that if you call this method before "startup" has been called, no log messages will be written to your MTAK
    log files.

    Arguments
    ----------
    command - The full Unix command (with arguments) to exec. (string)

    Returns
    --------
    None'''

    global _running, _error

    #_checkRunning()

    cmd = str(command)

    _log().info('shell_exec(command="%s")' % (cmd))

    #Execute the shell command
    try:
        subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stdin=None, stderr=subprocess.STDOUT)
        #subprocess.Popen(cmd,shell=True,stdout=None,stdin=None,stderr=None)
    except OSError:
        _error('Error executing shell command "%s": ' % (cmd))

def shell_cmd(command):
    '''Execute a Unix shell command, wait for it to finish, and return the result in a string.

    NOTE: This method will NOT call the MTAK "startup" method automatically if it has not been called already. Be aware
    that if you call this method before "startup" has been called, no log messages will be written to your MTAK
    log files.

    Arguments
    ----------
    command - The full Unix command (with arguments) to execute. (string)

    Returns
    --------
    A string containing all of the output from the executed command. (string)'''

    global _running, _error

    #_checkRunning()

    cmd = command.split()

    #process = None
    returnCode = None
    output = None

    _log().info('shell_cmd(command="%s")' % (command))

    # MPCS-3918  - 10/31/2014: commands module deprecated in Python 2.7; use subprocess module
    output = ''
    try:
        output = subprocess.check_output(cmd)
    except subprocess.CalledProcessError as e:
        _error('Error executing shell command "{}" (return code was {}): {}'.format(cmd, e.returncode, e.output))
    else:
        output=output.decode('utf-8') if isinstance(output, bytes) else output
    return output

##############################################
#
# Time Functions
#
##############################################

#ignoreSeconds
def get_current_sclk(ignoreSubseconds=False, asString=False):
    '''Get the most recent SCLK time seen by MTAK. Default format is a floating point value.

    IMPORTANT: Fetching the current SCLK relies on telemetry flowing. If no telemetry is flowing, this value
    will never update.  SCLK times are only read from realtime EHA and EVR telemetry, so if MTAK is not getting
    realtime EHA or EVRs, this value will never update.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    ignoreSubseconds - If True, only the coarse counter of the SCLK will be returned.  If false, the entire
                       SCLK coarse and fine will be returned.
    asString - Whether to return the value as a formatted string or a floating point value.  If True,
            the return value will be a string, if False, the return value is a floating point number. (boolean)

    Returns
    --------
    The most recent SCLK seen by MTAK formatted according to the input parameters.'''

    global _running, _downlinkProxy

    _checkRunning()

    _log().info('get_current_sclk()')

    ignoreSubseconds_bool = mpcsutil.getBooleanFromString(ignoreSubseconds)
    asString_bool = mpcsutil.getBooleanFromString(asString)

    if asString_bool:
        if ignoreSubseconds_bool:
            return str(mpcsutil.timeutil.getSclkSecondsOnly(_downlinkProxy.currentSclkExact))
        else:
            return mpcsutil.timeutil.getSclkString(_downlinkProxy.currentSclkExact)
    else:
        if ignoreSubseconds_bool:
            return mpcsutil.timeutil.getSclkSecondsOnly(_downlinkProxy.currentSclkExact)
        else:
            return mpcsutil.timeutil.getSclkFloatingPoint(_downlinkProxy.currentSclkExact)

    return None

def get_current_scet(epoch='UNIX', asString=False):
    '''Get the most recent SCET time seen by MTAK. Default format is floating point # of seconds since the Unix epoch.

    IMPORTANT: Fetching the current SCET relies on telemetry flowing. If no telemetry is flowing, this value
    will never update.  SCET times are only read from realtime EHA and EVR telemetry, so if MTAK is not getting
    realtime EHA or EVRs, this value will never update.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    epoch - The epoch to use for floating point return values.  Values are "UNIX" and "J2000".
            Only applicable if "asString" attribute is False. (string)
    asString - Whether to return the value as a formatted string or a floating point value.  If True,
            the return value will be a string, if False, the return value is a floating point number. (boolean)

    Returns
    --------
    The most recent SCET seen by MTAK. (varies)'''

    global _running, _downlinkProxy

    _checkRunning()

    _log().info('get_current_scet()')

    epoch_string = str(epoch).upper()
    asString_bool = mpcsutil.getBooleanFromString(asString)

    if asString_bool:
        return mpcsutil.timeutil.getTimeStringExt(_downlinkProxy.currentScetExact, \
                                                  nanos=_downlinkProxy.currentScetNano, \
                                                  precision=mpcsutil.timeutil.getScetPrecision())
    else:
        if epoch_string == 'UNIX':
            return float(_downlinkProxy.currentScetExact) / 1000.0
        elif epoch_string == 'J2000':
           return mpcsutil.timeutil.unixToJ2000(float(_downlinkProxy.currentScetExact) / 1000.0)
        else:
            _log().error('The input epoch %s is not recognized.  The valid values are "UNIX" or "J2000".' % (epoch))

    return None

def get_current_ert(epoch='UNIX', asString=False):
    '''Get the most recent ERT time seen by MTAK. Default format is floating point # of seconds since the Unix epoch.

    IMPORTANT: Fetching the current ERT relies on telemetry flowing. If no telemetry is flowing, this value
    will never update.  ERT times are only read from realtime EHA and EVR telemetry, so if MTAK is not getting
    realtime EHA or EVRs, this value will never update.

    NOTE: This method will automatically call the MTAK "startup" method with default parameters if startup has not
    been called already.

    Arguments
    ----------
    epoch - The epoch to use for floating point return values.  Values are "UNIX" and "J2000".
            Only applicable if "asString" attribute is False. (string)
    asString - Whether to return the value as a formatted string or a floating point value.  If True,
            the return value will be a string, if False, the return value is a floating point number. (boolean)

    Returns
    --------
    The most recent ERT seen by MTAK. (varies)'''

    global _running, _downlinkProxy

    _checkRunning()

    _log().info('get_current_ert()')

    epoch_string = str(epoch).upper()
    asString_bool = mpcsutil.getBooleanFromString(asString)

    #TODO implement nanosecond precision here
    if asString_bool:
        return mpcsutil.timeutil.getTimeString(float(_downlinkProxy.currentErtExact) / 1000.0)
    else:
        if epoch_string == 'UNIX':
            return float(_downlinkProxy.currentErtExact) / 1000.0
        elif epoch_string == 'J2000':
            return mpcsutil.timeutil.unixToJ2000(float(_downlinkProxy.currentErtExact) / 1000.0)
        else:
            _log().error('The input epoch %s is not recognized.  The valid values are "UNIX" or "J2000".' % (epoch))

    return None

def get_current_time(epoch='UNIX', asString=False):
    '''Get the current system time. Default format is floating point # of seconds since the Unix epoch.

    NOTE: This will NOT call the MTAK "startup" method automatically if it has not been called already. Be aware
    that if you call this method before "startup" has been called, no log messages will be written to your MTAK
    log files.

    Arguments
    ----------
    epoch - The epoch to use for floating point return values.  Values are "UNIX" and "J2000".
            Only applicable if "asString" attribute is False. (string)
    asString - Whether to return the value as a formatted string or a floating point value.  If True,
            the return value will be a string, if False, the return value is a floating point number. (boolean)

    Returns
    --------
    The current wall clock time. (varies)'''

    global _running, _downlinkProxy

    #_checkRunning()

    _log().info('get_current_ert()')

    epoch_string = str(epoch).upper()
    asString_bool = mpcsutil.getBooleanFromString(asString)

    if asString_bool:
        return mpcsutil.timeutil.getTimeString(time.time())
    else:
        if epoch_string == 'UNIX':
            return time.time()
        elif epoch_string == 'J2000':
            return mpcsutil.timeutil.unixToJ2000(time.time())
        else:
            _error('The input epoch %s is not recognized.  The valid values are "UNIX" or "J2000".' % (epoch))

    return None

def _isThrowOnErrorMission():
    ''' Check if the mission expects default behavior for sending commands
    to never result in errors being raised '''

    # I don't ask for much, only forgiveness
    # This is being added because M2020 wants strict backwards
    # compatibility with error behavior from MPCS for MSL.
    return mpcsutil.config.GdsConfig().getMission() != u'm20'

get_current_wkstn_time = get_current_time

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
