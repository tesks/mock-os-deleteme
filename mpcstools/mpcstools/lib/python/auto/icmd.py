#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module is the integrated command API of the AUTO package. It provides functions
to change and observe the state of the command service.

THIS API IS CONTROLLED BY THE AUTO SIS (D-001008_MM_AUTO_SIS)
Any modifications to this API must be reflected in the SIS

"""

from __future__ import (absolute_import, division, print_function)


import requests
from lad.gdsclient import EvrQuery, ChanValQuery, GdsLadClient
from auto import cpdinterface
from auto import cpd_datastructures
import logging
import mpcsutil
import sys
import time
import tempfile
import os.path
import auto.err
import mtak
import auto.up
import atexit
from collections import deque


#The prefix for the default log file
DEFAULT_LOG_FILE_PREFIX = 'ampcs_auto_log'

#The AUTO warn logger name
STDOUT_LOGGER_NAME = 'mpcs.auto'

#The CpdClient object that is used to communicate with CPD
_cpdClient = None

#logger object to log warnings to stderr
_stdoutLogger = None

#logger object for logging AUTO logs
_logger = None

#logger object for logging user logs
_userLogger = None

#log handler for warning logs
_warnLogHandler = None

#log handler for AUTO logs for file logging
_autoLogHandler = None

#log handler for AUTO logs for database logging
_autoIntegratedLogHandler = None

#log handler for user logs
_userLogHandler = None

#log handler for user logs for database logging
_userIntegratedLogHandler = None

#AUTO AMPCS uplink proxy
_uplinkProxy = None

#the venue of the session AUTO is currently attached to
_venue = None

#the key of the session AUTO is currently attached to
_sessionId = None

#the host of the session AUTO is currently attached to
_sessionHost = None

#a dictionary of queues of injected EHA values by channel ID
_injectedEha = dict()

#a queue of injected EVR values
_injectedEvr = deque([])

#whether or not to enable inject functions
_enableInjectFunctions = False

#the global LAD host
_globalLadHost = None

#the global LAD port
_globalLadPort = None

_globalLadClient = None

#MPCS-5286  9/25/13: flag for whether or not AUTO icmd has been started
_running = False

def _setQueryParam(query, fromSclk, fromScet, fromErt):
    '''
    Check to make sure that only one of fromSclk, fromScet, or fromErt is specified. Add parameters
    to query object.
    For internal use only
    '''
    foundQueryParam = False
    timestamp = None

    if fromSclk is not None:
        if foundQueryParam:
            raise ValueError('Cannot specify more than one query parameter')
        foundQueryParam = True
        timestamp = mpcsutil.timeutil.getSclkString(fromSclk)
        query.useSclk(_uplinkProxy.sessionScid).after(timestamp)

    def unixMillisHelper(milliseconds, precision):
        """Converts milliseconds from unix epoch to DOY string."""
        return mpcsutil.timeutil.getDoyTimeExt(milliseconds, nanos=0, precision=precision)

    if fromScet is not None:
        if foundQueryParam:
            raise ValueError('Cannot specify more than one query parameter')
        foundQueryParam = True
        timestamp = unixMillisHelper(fromScet, mpcsutil.timeutil.getScetPrecision())
        query.useScet().after(timestamp)

    if fromErt is not None:
        if foundQueryParam:
            raise ValueError('Cannot specify more than one query parameter')
        foundQueryParam = True
        timestamp = unixMillisHelper(fromErt, mpcsutil.timeutil.getErtPrecision())
        query.useErt().after(timestamp)

    if not foundQueryParam:
        raise ValueError('Must specify one of fromSclk, fromScet, fromErt, or queryToken')

    """
    if timestamp is not None and not (isinstance(timestamp, long) or isinstance(timestamp, int)):
        raise auto.err.AUTOError('Time parameter must be an integer representing milliseconds from the Unix epoch')
    """

def _check_running():
    '''
    Check if AUTO startup has been called (if AUTO is "running")
    If so, does nothing
    If not, raises mpcs.InvalidStateError
    '''
    global _running

    if not _running:
        raise mpcsutil.err.InvalidStateError('The AUTO framework must be initialized ' +
                            'with the  AUTO "startup" function prior to using ' +
                            'any methods in the auto.icmd module. ' +
                            'This function call will abort.')

#MPCS-4974 6/24/13:
#Added sessionConfigFile parameter to specify the sessionConfigFile to use to find or create session
def startup(cpdHost=None, cpdPort=None, logFile=None, logToDb=False,
            sessionConfigFile=None, sessionId=None, sessionHost=None,
            databaseHost=None, databasePort=None, databaseUser=None,
            databasePassword=None, globalLadHost=None, globalLadPort=None,
            userRole=None, keytabFile=None, username=None, autoIsHttps=False, autoRestHost=None):
    '''
    Initialize the integrated commanding API

    Args
    -----
    cpdHost - the host name of the CPD server to communicate with. If not
                specified, it will use the default uplink host configured
                on AMPCS.

    cpdPort - the port of the CPD server to communicate with. If not
                specified, it will use the default uplink port configured
                on AMPCS.

    logFile - location of file to log activity to. If not
                specified, it will write logs to the tmp directory

    logToDb - set to True to enable logging to database.

    sessionConfigFile - the path to a valid session configuration file that
                        will be used to create a new AMPCS session to log
                        command activities and log messages under. If this
                        is not provided, the latest AMPCS uplink session in the
                        database will be used.
                        Must be None or not provided if sessionId and sessionHost
                        is provided.

    sessionId - the session ID to log command activities and log messages under.
                    Required if sessionHost is provided.
                    Must be None or not provided if sessionConfigFile is provided.

    sessionHost - the session host to log command activities and log messages under.
                    Required if sessionId is provided.
                    Must be None or not provided if sessionConfigFile is provided.

    databaseHost - the host of the database to log under. Used only if logToDb=True.

    databasePort - the port of the database to log under. Used only if logToDb=True.

    databaseUser - the username of the database to log under. Used only if logToDb=True.

    databasePassword - the password of the database to log under. Used only if logToDb=True.

    globalLadHost - the host that the global LAD lives on. This is where AUTO
                    will query for downlink data

    globalLadPort - the port that the global LAD lives on. This is where AUTO
                    will query for downlink data

    userRole - the command security role to issue the directive as

    keytabFile - the location of keytab file to use for authentication

    username - the username associated with the keytab file

    autoIsHttps - whether or not the auto uplink proxy should use https.
                    Defaults to False

    autoRestHost - the rest host AUTO uplink proxy is deployed on.
                    Defaults to None, meaning AUTO will lookup host from config

    Returns
    --------
    None

    Raises
    ------
    mpcsutil.err.InvalidInitError - if the loggers fail to initialize
    auto.err.AuthenticationError - if the credentials provided to the auto.icmd startup function failed authentication with the security service, or if a non-existent keytab file is provided.
    auto.err.AUTOError - if the uplink proxy fails to start
    auto.err.AmpcsError -  if the CPD server cannot be pinged
    '''

    try:

        global _cpdClient, _logger, _stdoutLogger, _logger, _userLogger
        global _warnLogHandler, _autoLogHandler, _autoIntegratedLogHandler, _userLogHandler
        global _userIntegratedLogHandler, _uplinkProxy, _venue, _sessionId, _sessionHost
        global _enableInjectFunctions, _globalLadHost, _globalLadPort, _running
        global _globalLadClient

        #MPCS-5286 9/25/13: Make sure we haven't already started up and if we did, warn the user
        if _running:
            #the logger should be available if startup was called.
            _stdoutLogger.warning('AUTO icmd has already been started using the "startup" function. ' +
                                  'This current startup call will abort without performing any startup actions.')
            return

        #MPCS-5238 9/4/13: Check if keytab file exists
        if keytabFile is not None:
            try:
                with open(keytabFile): pass
            except IOError:
                raise auto.err.AuthenticationError('Keytab file does not exist')

        gdsConfig = mpcsutil.config.GdsConfig()

        #we want to log in local time zone, mpcsutil forces UTC and we inherit that when we import it.
        if 'TZ' in os.environ:
            del(os.environ['TZ'])

        time.tzset()

        #MPCS-5283 9/16/13: Get configured logging level
        log_level = logging.getLevelName(gdsConfig.getProperty('automationApp.auto.logging.level', default=logging.getLevelName(logging.INFO)))

        #set up the warning logger
        _stdoutLogger = logging.getLogger(STDOUT_LOGGER_NAME)
        _stdoutLogger.setLevel(log_level)

        logFormat = '%(levelname)s [%(asctime)s]: %(message)s'
        logFormatter = logging.Formatter(logFormat, mpcsutil.timeutil.isoFmtTz)

        #if there is an existing logger created by a previous startup call,
        #just use it
        if _warnLogHandler is None:
            _warnLogHandler = logging.StreamHandler(sys.stderr)
            _warnLogHandler.setFormatter(logFormatter)
            _warnLogHandler.setLevel(log_level)
            _stdoutLogger.addHandler(_warnLogHandler)

        #set up the AUTO logger
        _logger = logging.getLogger(__name__)
        _logger.setLevel(log_level)

        logFormat = 'AUTO.icmd: %(levelname)s [%(asctime)s]: %(message)s'
        logFormatter = logging.Formatter(logFormat, mpcsutil.timeutil.isoFmtTz)

        if logFile is None:
            logFile = os.path.join(tempfile.gettempdir(), '{}_{}'.format(DEFAULT_LOG_FILE_PREFIX, time.time()))
            _stdoutLogger.warning('No log file specified, logging to default location: {}'.format(logFile))

        #remove the old log handler there is one from a previous startup call.
        #we want to create a new one with the new log file
        if _autoLogHandler is not None:
            _autoLogHandler.close()
            _logger.removeHandler(_autoLogHandler)

        if _autoIntegratedLogHandler is not None:
            _autoIntegratedLogHandler.close()
            _logger.removeHandler(_autoIntegratedLogHandler)

        _stdoutLogger.info("Starting uplink proxy")
        #MPCS-4974  7/2/13 - Always start the uplink proxy now, since it will handle session management
        #MPCS-5262  9/9/13: Added logToDb flag to enable/disable AUTO database logging
        _uplinkProxy = auto.up.UplinkProxy(fswUplinkHost=cpdHost,
                                           fswUplinkPort=cpdPort,
                                           userRole=userRole,
                                           keytabFile=keytabFile,
                                           username=username,
                                           logger=_logger,
                                           logFile=logFile,
                                           logToDb=logToDb,
                                           sessionConfigFile=sessionConfigFile,
                                           sessionId=sessionId,
                                           sessionHost=sessionHost,
                                           databaseHost=databaseHost,
                                           databasePort=databasePort,
                                           databaseUser=databaseUser,
                                           databasePassword=databasePassword,
                                           https=autoIsHttps,
                                           restHost=autoRestHost)
        _uplinkProxy.start()
        _stdoutLogger.info("Uplink proxy sucessfully started")

        # MPCS-4700 - Added a thread that monitors the uplink log proxy.
        # 5/24/13     Logs to the database are disabled if this fails to start up
        #       or if it exits unexpectedly.

        #did not start up properly.
        if _uplinkProxy._exitCode is not None:
            errorMessage = 'AMPCS uplink proxy did not start up properly. Unable to proceed with startup.'
            _stdoutLogger.warning(errorMessage)
            _stdoutLogger.warning(_uplinkProxy._errorMessages)
            logToDb = False
            raise auto.err.AUTOError(errorMessage)

        #grab the venue, session host and key
        (_venue, _sessionId, _sessionHost, _sessionScid) = (_uplinkProxy.venue, _uplinkProxy.sessionKey, _uplinkProxy.sessionHost, _uplinkProxy.sessionScid)

        gdsConfig = mpcsutil.config.GdsConfig()
        _enableInjectFunctions = mpcsutil.getBooleanFromString(gdsConfig.getProperty('automationApp.auto.uplink.injectFunctions.%s' % (_venue), 'false'))

        _globalLadHost = globalLadHost if globalLadHost is not None else gdsConfig.getLadHost()
        _globalLadPort = globalLadPort if globalLadPort is not None else gdsConfig.getLadPort()

        _globalLadClient = GdsLadClient(_globalLadHost, _globalLadPort)

        #should not be None, if it is we cannot proceed
        if _sessionId is None or _sessionHost is None and "auto" in _uplinkProxy.applicationPath:
            errorMessage = 'Unable to initialize an AMPCS session'

            if sessionConfigFile is not None:
                errorMessage += ' using the session configuration file: %s' % (sessionConfigFile)

            _stdoutLogger.fatal(errorMessage)
            raise mpcsutil.err.InvalidInitError(errorMessage)

        try:
            #MPCS-5262  9/9/13: This log handler needs to be started regardless of logToDb, since it also handles file logging
            logFormat = 'AUTO.icmd: %(message)s'
            logFormatter = logging.Formatter(logFormat, mpcsutil.timeutil.isoFmtTz)

            #MPCS-5283  9/16/13: We are using MTAK's DatabaseHandler to
            #forward log messages to our uplink proxy, which creates the integrated
            #log inside the database and a log file
            _autoIntegratedLogHandler = mtak.DatabaseHandler(_uplinkProxy)
            _autoIntegratedLogHandler.setFormatter(logFormatter)
            _logger.addHandler(_autoIntegratedLogHandler)

        except IOError as e:
            _stdoutLogger.fatal('Could not initialize the AUTO logger using log file %s.' % (logFile))
            raise mpcsutil.err.InvalidInitError(e)

        #set up the user logger
        _userLogger = logging.getLogger(__name__ + '.user')
        _userLogger.propagate = False
        _userLogger.setLevel(log_level)

        logFormat = 'AUTO.user: %(levelname)s [%(asctime)s]: %(message)s'
        logFormatter = logging.Formatter(logFormat, mpcsutil.timeutil.isoFmtTz)

        #remove the old log handler there is one from a previous startup call.
        #we want to create a new one with the new log file
        if _userLogHandler is not None:
            _userLogHandler.close()
            _userLogger.removeHandler(_userLogHandler)
        if _userIntegratedLogHandler is not None:
            _userIntegratedLogHandler.close()
            _userLogger.removeHandler(_userIntegratedLogHandler)

        try:
            #MPCS-5262  9/9/13: This log handler needs to be started regardless of logToDb, since it also handles file logging
            _userLogHandler = logging.FileHandler(logFile)
            _userLogHandler.setFormatter(logFormatter)
            _userLogHandler.setLevel(log_level)
            _userLogger.addHandler(_userLogHandler)

            logFormat = 'AUTO.user: %(message)s'
            logFormatter = logging.Formatter(logFormat, mpcsutil.timeutil.isoFmtTz)

            #MPCS-5283  9/16/13: We are using MTAK's DatabaseHandler to
            #forward log messages to our uplink proxy, which creates the integrated
            #log inside the database and a log file
            _userIntegratedLogHandler = mtak.DatabaseHandler(_uplinkProxy)
            _userIntegratedLogHandler.setFormatter(logFormatter)
            _userLogger.addHandler(_userIntegratedLogHandler)

        except IOError as e:
            _stdoutLogger.fatal('Could not initialize the user logger using log file %s.' % (logFile))
            raise mpcsutil.err.InvalidInitError(e)

        _logger.info('Connected to %s session %s on %s' % (_venue, _sessionId, _sessionHost))
        _logger.info('Initializing the AMPCS Utility Toolkit for Operations (AUTO), Integrated Commanding (ICMD) API...')
        _logger.debug('Startup Parameters: cpdHost=%s, cpdPort=%s, userRole=%s, keytabFile=%s, username=%s, proxyPort=%s proxyHost=%s' %
                     (cpdHost, cpdPort, userRole, keytabFile, username, str(_uplinkProxy._proxyPort), _uplinkProxy._restHost))

        #MPCS-5242  9/4/13: Removed _cpdClient.clean_up(). clean_up() was
        #implemented in R5, but R6 got rid of it since logging was moved to the proxy

        #create the client object that we will use to communicate with CPD
        _cpdClient = cpdinterface.CpdClient(cpdHost, cpdPort, _logger, userRole, keytabFile, username, _uplinkProxy._proxyPort, _uplinkProxy._restHost, _uplinkProxy._https, _uplinkProxy._serverProcessLog)

        #ping CPD server
        _logger.info('Attempting to contact CPD server...')

        tries = 0
        error = None
        success = False

        while not success and tries < 3:
            try:
                if tries > 0:
                    time.sleep(1)

                success = _cpdClient.ping()
            except auto.err.AmpcsError as e:
                _logger.warning("Unable to contact CPD server...trying again...")
                success = False
                error = e

            tries += 1

        if not success:
            _logger.fatal("Unable to initialize AMPCS proxy. Aborting startup...")
            if error:
                raise error
            else:
                raise auto.err.AUTOError("Unable to initialize AMPCS proxy")
        else:
            _logger.info('Successfully contacted CPD server. AUTO ICMD API successfully initialized and ready to issue directives.')

        #MPCS-5286  9/25/13: Keep track of if we are running, so we can prevent multiple startup() calls
        _running = True
    except:
        #MPCS-5286  9/25/13: Clean up if startup fails
        shutdown()
        raise
    finally:
        #MPCS-5281  9/16/13: Register shutdown with atexit to be called when interpreter exits
        atexit.register(shutdown)

#MPCS-9156 10/13/17: Added send_pdu
def send_pdu(pduFile, entityId, vcid=None):
    '''
    Send a pdu file to CPD
    Args
    ____
    pduFile - path to the pdu file to send (string)
    entityId - destination entity id to send to (string)
    vcid - vcid to send to (string)

    Raises
    ------
    auto.err.AUTOERROR - if an error is encountered by the AUTO library when trying to issue the directive or process CPD's response.
    ValueError - if an invalid parameter is specified
    '''
    _check_running()

    if pduFile is None or not os.path.exists(pduFile):
        raise ValueError('PDU file is not valid')
    if entityId is None or not entityId.isdigit():
        raise ValueError('EntityId must be a number')
    if vcid is not None and not vcid.isdigit():
        raise ValueError('Vcid must be a number')

    _uplinkProxy.sendPdu(pduFile, entityId, vcid)


#MPCS-5239  9/16/13: Default validateScmf to True
def send_scmf(scmfFile, validateScmf=True, waitForRadiation=0):
    '''
    Send an SCMF to CPD

    Args
    ____
    scmfFile - path to the SCMF file to send

    validateScmf - Enable checksum validation and dictionary version validation on the SCMF. Defaults to True. (optional) (boolean)

    waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
    This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
    uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
    (optional) (positive int)

    Raises
    ------
    ValueError - if the scmfFile argument is None
    auto.err.AuthenticationError - if the credentials provided to the auto.icmd startup function failed authentication with the security service.
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to issue this directive.
    auto.err.AUTOError - if an error is encountered by the AUTO library when trying to issue the directive or process CPD's response.
    auto.err.CommandServiceError -  if the directive is received by CPD, but an error is encountered by CPD while processing the directive.
    auto.err.ConnectionError - if the AUTO library is unable to contact the CPD server at the specified/configured uplink host/port.
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    if scmfFile is None:
        raise ValueError("scmfFile parameter cannot be None")

    _uplinkProxy.sendScmf(scmfFile, validateScmf, waitForRadiation)

def connect_to_station(stationId):
    '''
    Directs CPD to accept an incoming connection request from the station with station ID matching the specified stationID parameter.
    If no errors are raised, then the directive was successfully issued and processed by CPD.

    Args
    -----
    stationId - the station ID of the station to accept the connection request

    Returns
    --------
    auto.cpd_datastructures.CpdResponse object containing the CPD server's response

    Raises
    ------
    ValueError - if no station ID is provided
    auto.err.AuthenticationError - if the credentials provided to the auto.icmd startup function failed authentication with the security service.
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to issue this directive.
    auto.err.AUTOError - if an error is encountered by the AUTO library when trying to issue the directive or process CPD's response.
    auto.err.CommandServiceError -  if the directive is received by CPD, but an error is encountered by CPD while processing the directive.
    auto.err.ConnectionError - if the AUTO library is unable to contact the CPD server at the specified/configured uplink host/port.
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    if stationId is None:
        raise ValueError("stationId parameter cannot be None")

    resp_dict = _cpdClient.send_directive(directive=cpdinterface.CONNECT_TO_STATION_DIRECTIVE, station_id=stationId)

    return cpd_datastructures.CpdResponse(**resp_dict)

def disconnect_from_station():
    '''
    Directs CPD to disconnect from a currently connected station or to stop waiting for a station connection
    If no errors are raised, then the directive was successfully issued and processed by CPD.

    Args
    -----
    None

    Returns
    --------
    auto.cpd_datastructures.CpdResponse object containing the CPD server's response

    Raises
    ------
    auto.err.AuthenticationError - if the credentials provided to the auto.icmd startup function failed authentication with the security service.
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to issue this directive.
    auto.err.AUTOError - if an error is encountered by the AUTO library when trying to issue the directive or process CPD's response.
    auto.err.CommandServiceError -  if the directive is received by CPD, but an error is encountered by CPD while processing the directive.
    auto.err.ConnectionError - if the AUTO library is unable to contact the CPD server at the specified/configured uplink host/port.
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    resp_dict = _cpdClient.send_directive(directive=cpdinterface.DISCONNECT_FROM_STATION_DIRECTIVE)

    return cpd_datastructures.CpdResponse(**resp_dict)

def query_connection_status():
    '''
    Query CPD's current station connection status. If CPD is connected to a station, then a station ID will be returned indicating the connected station.
    If no errors are raised, then the directive was successfully issued and processed by CPD.

    Args
    -----
    None

    Returns
    --------
    auto.cpd_datastructures.CpdConnectionStatus object containing the CPD server's response to the issued directive, connection status, and if applicable, connected station.

    Raises
    ------
    auto.err.AuthenticationError - if the credentials provided to the auto.icmd startup function failed authentication with the security service.
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to issue this directive.
    auto.err.AUTOError - if an error is encountered by the AUTO library when trying to issue the directive or process CPD's response.
    auto.err.CommandServiceError -  if the directive is received by CPD, but an error is encountered by CPD while processing the directive.
    auto.err.ConnectionError - if the AUTO library is unable to contact the CPD server at the specified/configured uplink host/port.
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    resp_dict = _cpdClient.send_directive(directive=cpdinterface.QUERY_CONNECTION_STATUS_DIRECTIVE)
    # print('icmd.query_connection_status:: resp_dict: {}'.format(resp_dict))

    return cpd_datastructures.CpdConnectionStatus(**resp_dict)

def set_execution_state(executionState):
    '''
    Set the Execution State of CPD
    If no errors are raised, then the directive was successfully issued and processed by CPD.

    Args
    -----
    executionState - the execution state to set CPD to.

    Returns
    --------
    auto.cpd_datastructures.CpdResponse object containing the CPD server's response

    Raises
    ------
    auto.err.AuthenticationError - if the credentials provided to the auto.icmd startup function failed authentication with the security service.
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to issue this directive.
    auto.err.AUTOError - if an error is encountered by the AUTO library when trying to issue the directive or process CPD's response.
    auto.err.CommandServiceError -  if the directive is received by CPD, but an error is encountered by CPD while processing the directive.
    auto.err.ConnectionError - if the AUTO library is unable to contact the CPD server at the specified/configured uplink host/port.
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    if executionState is None:
        raise ValueError("executionState parameter cannot be None")

    resp_dict = _cpdClient.send_directive(directive=cpdinterface.SET_EXECUTION_STATE_DIRECTIVE, execution_state=executionState)
    # print('icmd.set_execution_state:: resp_dict: {}'.format(resp_dict))
    return cpd_datastructures.CpdResponse(**resp_dict)

def query_configuration():
    '''
    Query CPD's current configuration (CPD's Preparation State, Execution State, Execution Mode, Execution Method, and Aggregation Method).
    If no errors are raised, then the directive was successfully issued and processed by CPD.

    Args
    -----
    None

    Returns
    --------
    auto.cpd_datastructures.CpdConfiguration object containing the CPD server's response, Preparation State, Execution State, Execution Mode, Execution Method, and Aggregation Method.

    Raises
    ------
    auto.err.AuthenticationError - if the credentials provided to the auto.icmd startup function failed authentication with the security service.
    auto.err.AuthorizationError - if the user and/or role that authenticated with the command service does not have the permissions to issue this directive.
    auto.err.AUTOError - if an error is encountered by the AUTO library when trying to issue the directive or process CPD's response.
    auto.err.CommandServiceError -  if the directive is received by CPD, but an error is encountered by CPD while processing the directive.
    auto.err.ConnectionError - if the AUTO library is unable to contact the CPD server at the specified/configured uplink host/port.
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    resp_dict = _cpdClient.send_directive(directive=cpdinterface.QUERY_CONFIGURATION_DIRECTIVE)
    # print('icmd.query_configuration:: resp_dict: {}'.format(resp_dict))
    return cpd_datastructures.CpdConfiguration(**resp_dict)

#MPCS-5411  10/03/13: Removed concept of SCN from Global LAD, so no more queryToken
def get_monitor(channelId, stationId, fromErt, realtime=True):
    '''
    Get a list of downlinked monitor channel values from a specific station.

    Args
    ----

    channelId - the channel ID of the monitor channel value to get. (string) (required)

    stationId - the station ID from which the monitor channel value was created (int) (required)

    fromErt - the ERT time to get EHAs from, as the number of
                milliseconds since the epoch. EHAs with a ERT value after
                fromErt will be returned. (64-bit int) (required)

    realtime - if True, the function will get realtime values.
                If False, the function will get recorded values.
                (boolean) (optional)

    Return
    ------
    A list of mpcsutil.channel.ChanVal

    Raises
    ______
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    # MPCS-7909  = 03/17/2016: New lad client
    _check_running()

    query = (
             ChanValQuery()
            .monitorOnly()
            .addChannelId(channelId)
            .addDssId(stationId)
            .setMaxResults(-1)
            )
    if realtime:
        query.realtimeOnly()
    else:
        query.recordedOnly()

    _setQueryParam(query, None, None, fromErt)

    return _getEha(channelId, query)

def _getEha(channelId, query):
    """
    Takes a pre-configured query object and queries based on that, or, returns an inject eha for channelId.

    Args
    ---

    channelId - the channel ID of the channel to get. (string) (required).
    query - a pre-configured lad.client.ChanValQuery object to use.

    returns: A list of mpcsutil.channel.ChanVal objects containing either the last injected value for provided
            channel ID or actual LAD query results

    Raises
    ______
    ValueError - if more than one of fromSclk, fromScet, or fromErt is specified

    ConnectionError - if Global LAD service is not available

    '''
    """

    try:
        if _enableInjectFunctions and (channelId in _injectedEha and _injectedEha[channelId]):
            injectedEhaList = list(_injectedEha[channelId])
            _injectedEha[channelId].clear()

            return injectedEhaList

        try:
            channelValues = _globalLadClient.fetchChanVals(query)
        except requests.exceptions.HTTPError as e:
            raise auto.err.AUTOError(e)

    except requests.exceptions.ConnectionError as e:
        #connection refused error. this means global LAD is not up
        raise auto.err.ConnectionError("Global LAD service at %s:%s is not available" % (_globalLadHost, _globalLadPort))

    return channelValues

#MPCS-5411  10/02/13: Removed concept of SCN from Global LAD, so no more queryToken
def get_eha(channelId, realtime=True, fromSclk=None, fromScet=None, fromErt=None):
    '''
    Get a list of downlinked EHA channel values.
    One of fromSclk, fromScet, or fromErt must be specified.
    The rest must be None.

    Args
    ----

    channelId - the channel ID of the channel value to get. (string) (required)

    realtime - if True, the function will get realtime values.
                If False, the function will get recorded values.
                (boolean) (optional)

    fromSclk - the SCLK time to get EHAs from, as a 64-bit SCLK exact.
                EHAs with a SCLK value after fromSclk will be returned.
                (64-bit int) (optional)

    fromScet - the SCET time to get EHAs from, as the number of
                milliseconds since the epoch. EHAs with a SCET value after
                fromScet will be returned. (64-bit int) (optional)

    fromErt - the ERT time to get EHAs from, as the number of
                milliseconds since the epoch. EHAs with a ERT value after
                fromErt will be returned. (64-bit int) (optional)

    Return
    ------
    A list of ChanVal objects matching the parameters

    Raises
    ______
    ValueError - if more than one of fromSclk, fromScet, or fromErt is specified

    ConnectionError - if Global LAD service is not available

    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''
    # MPCS-7909  = 03/17/2016: New lad client

    _check_running()

    query = ChanValQuery().addChannelId(channelId).setMaxResults(-1).fswOnly()
    if realtime: query.realtimeOnly()
    else: query.recordedOnly()

    _setQueryParam(query, fromSclk, fromScet, fromErt)

    return _getEha(channelId, query)

def get_evr(eventId, realtime=True, fromSclk=None, fromScet=None, fromErt=None):
    '''
    Get a list of downlinked EVRs.
    One of fromSclk, fromScet, or fromErt must be specified.
    The rest must be None.

    Args
    ----

    eventId - the EVR eventID.
                Only EVRs matching the event ID will be returned
                (int) (required)

    realtime - if True, the function will get realtime EVRs.
                If False, the function will get recorded EVRs.
                (boolean) (optional)

    fromSclk - the SCLK time to get EVRs from, as a 64-bit SCLK exact.
                EVRs with a SCLK value after fromSclk will be returned.
                (64-bit int) (optional)

    fromScet - the SCET time to get EVRs from, as the number of
                milliseconds since the epoch. EVRs with a SCET value after
                fromScet will be returned. (64-bit int) (optional)

    fromErt - the ERT time to get EVRs from, as the number of
                milliseconds since the epoch. EVRs with a ERT value after
                fromErt will be returned. (64-bit int) (optional)

    Return
    ------
    A list of Evr objects matching the parameters

    Raises
    ______
    ValueError - if more than one of fromSclk, fromScet, or fromErt is specified

    ConnectionError - if Global LAD service is not available

    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    #MPCS-5262  9/9/13: Event ID needs to be a valid integer
    try:
        int(eventId)
    except:
        raise ValueError('Invalid EVR Event ID')

    query = EvrQuery().addEventId(eventId).setMaxResults(-1).fswOnly()
    if realtime: query.realtimeOnly()
    else: query.recordedOnly()
    _setQueryParam(query, fromSclk, fromScet, fromErt)

    try:
        #if there are injected values, return that
        if _enableInjectFunctions and _injectedEvr:
            injectedEvrList = list(_injectedEvr)
            _injectedEvr.clear()

            return injectedEvrList

        try:
            evrs = _globalLadClient.fetchEvrs(query)
        except requests.exceptions.HTTPError as e:
            raise auto.err.AUTOError(e)

    except requests.exceptions.ConnectionError as e:
        #connection refused error. this means global LAD is not up
        raise auto.err.ConnectionError("Global LAD service at %s:%s is not available" % (_globalLadHost, _globalLadPort))
    return evrs

def inject_eha(channelId=None, name=None, eventTime='', sclk='', ert='', scet='', type='',
               dnUnits='', euUnits='', status=None, dn=None, eu=None, alarms=[], realtime=True):
    '''
    Inject a fake EHA into the downlink stream. This only affects the current AUTO session. The next call to get_eha will return the injected EHA

    Args
    ----
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
    None

    Raises
    --------
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()
    global _logger, _injectedEha, _stdoutLogger, _venue
    if not _enableInjectFunctions:
        _stdoutLogger.warning("Inject functions have been disabled for this venue ({})".format(_venue))
    else:
        _logger.info('inject_eha(channelId="{}", name="{}", eventTime="{}", sclk="{}", ert="{}", scet="{}", type="{}", dnUnits="{}", euUnits="{}", status="{}", dn="{}", eu="{}", alarms="{}",realtime="{}")'.format(channelId, name, eventTime, sclk, ert, scet, type, dnUnits, euUnits, status, dn, eu, alarms, realtime))

        if dn is None:
            _logger.error('Could not inject EHA value for channelId (ID={}, name={}) because no DN value was specified.'.format(channelId, name))
            return
        else:
            try:
                dn = mpcsutil.channel.formatDn(type, dn)
            except ValueError:
                _logger.error('Could not inject EHA value for channel (ID={}, name={}).\n{}'.format(channelId, name, traceback.format_exc()))
                return

        try:
            float(str(eu))
        except ValueError:
            if eu is not None:
                _logger.error('Could not inject EHA value for channel (ID={}, name={}) because the EU value "{}" is not a valid floating point number.'.format(channelId, name, eu))
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

        theQueue = None

        if channelId in _injectedEha:
            theQueue = _injectedEha[channelId]
        else:
            theQueue = deque([])
            _injectedEha[channelId] = theQueue

        theQueue.append(mpcsutil.channel.ChanVal(**d))

def inject_evr(name='', eventId=0, module='', level='', fromSse=False, eventTime='', eventTimeExact=0,
               ert='', ertExact=0, sclk='', scet='', scetExact=0, message='', metadata=[], realtime=True):
    '''Injects a fake event record (EVR) into the AUTO downlink stream.  Upon successfully calling this function
    the next get_evr function call will return the injected EVR.  If multiple EVRs are injected, the next get_evr
    function call will return all injected EVRs. THIS ONLY AFFECTS THE CURRENT AUTO SCRIPT AND NOTHING ELSE.

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
    None

    Raises
    --------
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    global _logger, _injectedEvr, _stdoutLogger, _venue
    if not _enableInjectFunctions:
        _stdoutLogger.warning("Inject functions have been disabled for this venue (%s)" % (_venue))
    else:
        _logger.info('inject_evr(name="%s", eventId="%s", module="%s", level="%s", fromSse="%s", eventTime="%s", ert="%s", sclk="%s", scet="%s", realtime="%s", message="%s", metadata="%s")'
                  % (name, eventId, module, level, fromSse, eventTime, ert, sclk, scet, realtime, message, metadata))

        #Dictionary holding all the attribute information for this EVR
        d = {"name": name, "eventId": eventId, "module": module, "level": level, "fromSse": fromSse, "eventTime": eventTime, "ert":ert,
             "sclk": sclk, "realtime": realtime, "message": message, "metadata": metadata, "eventTimeExact":eventTimeExact,
             "ertExact":ertExact, "scetExact":scetExact, "realtime":mpcsutil.getBooleanFromString(realtime), "injected":True}

        #Create an EVR and pass in the attributes as a variable-length list
        fakeEvr = mpcsutil.evr.Evr(**d)

        _injectedEvr.append(fakeEvr)

def new_session(sessionConfigFile):
    '''
    Create a new AMPCS session with the provided session config file and attach AUTO to the new session.
    After successfully calling this function, any uplink or log messages will be under the newly created session

    Args
    -----

    sessionConfigFile - the path to a valid session configuration file that
                        will be used to create a new AMPCS session to log
                        command activities and log messages under.
    Raises
    -------
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    global _venue, _sessionId, _sessionHost

    _uplinkProxy.sessionInit(sessionConfigFile=sessionConfigFile)
    (_venue, _sessionId, _sessionHost) = (_uplinkProxy.venue, _uplinkProxy._sessionId, _uplinkProxy.sessionHost)

def set_uplink_rates(uplinkRates=None):
    '''
    Set the uplink rates that will be attached to subsequent uplinks. This is only applicable
    when using the COMMAND_SERVICE uplink connection type

    Arguments
    ----------
    uplinkRates - A list of rates in bits per second (list)
    that the request may be radiated with. Defaults to ANY if None or not
    specified.

    Raises
    -------
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    global _logger, _uplinkProxy

    _uplinkProxy.setUplinkRates(uplinkRates=uplinkRates)

def log(message, level=logging.INFO):
    '''
    Writes a user log to the log file provided to the startup() function.
    Must call startup() prior to using this function so the AUTO logger is initialized

    Args
    -----
    messeage - the log message

    level - the log level (see logging module)

    Raises
    -------
    mpcsutil.InvalidStateError - if this function is called prior to a successful AUTO startup
    '''

    _check_running()

    global _userLogger

    if _userLogger is not None:
        _userLogger.log(level, message)
    else:
        #If we get here, startup function is NOT working/implemented properly
        raise auto.err.AUTOError("Unexpected startup error: AUTO logger failed initialized.")

def shutdown():
    '''
    Shuts down the integrated commanding API by terminating any spawned process and releasing held resources.

    This should be called at the end of every script that called startup().
    '''

    global _uplinkProxy, _logger, _userLogger, _autoLogHandler, _autoIntegratedLogHandler, _userLogHandler, _userIntegratedLogHandler, _running, _stdoutLogger

    #MPCS-5286  9/25/13: Flip the running flag to False so startup() can be called again
    _running = False

    if _uplinkProxy is not None:
        _uplinkProxy.stop()
        _uplinkProxy = None

    if _autoLogHandler is not None:
        _autoLogHandler.close()
        _logger.removeHandler(_autoLogHandler)
        _autoLogHandler = None

    if _autoIntegratedLogHandler is not None:
        _autoIntegratedLogHandler.close()
        _logger.removeHandler(_autoIntegratedLogHandler)
        _autoIntegratedLogHandler = None

    if _userLogHandler is not None:
        _userLogHandler.close()
        _userLogger.removeHandler(_userLogHandler)
        _userLogHandler = None

    if _userIntegratedLogHandler is not None:
        _userIntegratedLogHandler.close()
        _userLogger.removeHandler(_userIntegratedLogHandler)
        _userIntegratedLogHandler = None

    if _stdoutLogger is not None:
        _stdoutLogger.handlers = []
        _stdoutLogger = None

def test():
    import subprocess, traceback
    import mpcsutil.test
    from mpcsutil.ColorString import pretty_pyg as _pretty
    from mpcsutil.ColorString import PrettyTypes

    logger = logging.getLogger(STDOUT_LOGGER_NAME)
    logger.setLevel(logging.getLevelName(logging.DEBUG))
    file_handler = logging.FileHandler('{}.test.log'.format(STDOUT_LOGGER_NAME))
    stream_handler = logging.StreamHandler(sys.stdout)
    formatter    = logging.Formatter('{} :: %(asctime)s : %(levelname)s : %(name)s : %(message)s'.format(STDOUT_LOGGER_NAME))
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)
    logger.addHandler(stream_handler)

    logger = logging.getLogger(__name__)
    logger.setLevel(logging.getLevelName(logging.DEBUG))
    file_handler = logging.FileHandler('auto.icmd.test.log')
    stream_handler = logging.StreamHandler(sys.stdout)
    formatter    = logging.Formatter('{} :: %(asctime)s : %(levelname)s : %(name)s : %(message)s'.format(__name__))
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)
    logger.addHandler(stream_handler)

    def pretty_(*args, **kwargs): print(_pretty(*args, **kwargs))

    CPD_SIM_PORT        = '8186'
    CPD_SIM_HOST        = 'localhost'
    CPD_SIM_SCRIPT      = os.path.join(mpcsutil.chillBinDirectory, 'test', 'chill_cpd_sim')
    SESSION_CONFIG_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)), 'test', 'icmdtest_session_config.xml')

    config = mpcsutil.config.GdsConfig()

    os.environ.update({'GDS_JAVA_OPTS': '-DGdsUserConfigDir={}'.format(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'test'))})

    mpcsutil.test.overrideDictionaries(config)
    mpcsutil.test.insertSessionDictionaries(SESSION_CONFIG_FILE, config)

    _ctx=dict(
        CPD_SIM_PORT        = '8186',
        CPD_SIM_HOST        = 'localhost',
        CPD_SIM_SCRIPT      = os.path.join(mpcsutil.chillBinDirectory, 'test', 'chill_cpd_sim'),
        SESSION_CONFIG_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)), 'test', 'icmdtest_session_config.xml') )

    def _start_cpd_sim():
        _cmd=[CPD_SIM_SCRIPT, '--port', CPD_SIM_PORT]
        _icmd_kwargs=dict(
            cpdHost=CPD_SIM_HOST,
            cpdPort=CPD_SIM_PORT,
            sessionConfigFile=SESSION_CONFIG_FILE)
        print('Starting CPD Sim:\n\t{}'.format(_pretty(' '.join(_cmd), _type=PrettyTypes.BASH)))
        cpd_sim_process = subprocess.Popen(
            _cmd,
            stdout=sys.stdout,
            stderr=sys.stderr)
        _ctx.update({'cpd_sim_process':cpd_sim_process})
        pretty_(_ctx)
        print('\nCalling auto.icmd.startup:\n{}'.format(  _pretty(_icmd_kwargs)  ))
        startup(**_icmd_kwargs)

    def _teardown():
        print('auto.icmd.test._teardown:\n{}'.format(_pretty(_ctx)))
        shutdown()
        _proc=_ctx.get('cpd_sim_process')
        if _proc:
            _proc.terminate()

    try:
        _start_cpd_sim()
    except KeyboardInterrupt:
        print('\rCaught Keyboard Interrupt... exiting gracefully')
    except:
        pretty_(traceback.format_exc(), _type=PrettyTypes.TRACEBACK)
    finally:
        _teardown()


def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
