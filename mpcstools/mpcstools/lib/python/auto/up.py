#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import mpcsutil
import logging
import subprocess
import os
import threading
import time
import auto.err
import requests
import six
if six.PY2:
    import httplib
    from urllib import urlencode
else:
    import http.client as httplib
    from urllib.parse import urlencode

import socket
import errno
from requests.adapters import HTTPAdapter
import mpcsutil.timeutil

class UplinkProxy(HTTPAdapter):
    ''' The UplinkProxy is a proxy object that allows AUTO to interact with the
    AMPCS uplink module.
    '''

    #MPCS-5262  9/9/13: Added logToDb flag
    def  __init__(self, fswUplinkHost, fswUplinkPort, userRole,
                 keytabFile, username, logger, logFile=None, logToDb=None,
                 sessionConfigFile=None, sessionId=None, sessionHost=None,
                 databaseHost=None, databasePort=None, databaseUser=None,
                 databasePassword=None, https=False, restHost=None):
        '''
        Initialize the UplinkProxy

        Args
        -----
        fswUplinkHost - the host name of the uplink server to communicate with. If not
                    specified, it will use the default uplink host configured
                    on AMPCS.

        fswUplinkPort - the port of the uplink server to communicate with. If not
                    specified, it will use the default uplink port configured
                    on AMPCS.

        userRole - the command security role to issue the directive as

        keytabFile - the location of keytab file to use for authentication

        username - the username associated with the keytab file

        logger - the logger to use for logging

        logFile - the file to log to

        logToDb - set to True to enable logging to database, False to disable

        sessionConfigFile - the path to a valid session configuration file that
                        will be used to create a new AMPCS session to log
                        command activities and log messages under. If this
                        is not provided, the latest AMPCS uplink session in the
                        database will be used.
                        Must be None or not provided if sessionId and sessionHost
                        are provided.

        sessionId - the session ID to log command activities and log messages under.
                    Required if sessionHost is provided.
                    Must be None or not provided if sessionConfigFile is provided.

        sessionHost - the session host to log command activities and log messages under.
                    Required if sessionId is provided.
                    Must be None or not provided if sessionConfigFile is provided.

        databaseHost - the host of the database to look for sessions and log uplink activites to.

        databasePort - the port of the database to look for sessions and log uplink activites to.

        databaseUser - the username of the database to look for sessions and log uplink activites to.

        databasePassword - the password of the database to look for sessions and log uplink activites to.

        https - whether or not to use an HTTPS connection. Defaults to False

        restHost - the uplink proxy host. Defaults to None, meaning AUTO will lookup host from config.

        '''

        super(UplinkProxy, self).__init__()

        self._fswUplinkHost = fswUplinkHost
        self._fswUplinkPort = fswUplinkPort
        self._sessionConfigFile = sessionConfigFile
        self._databaseHost = databaseHost
        self._databasePort = databasePort
        self._databaseUser = databaseUser
        self._databasePassword = databasePassword
        self._userRole = userRole
        self._keytabFile = keytabFile
        self._username = username
        self._exitCode = None
        self._errorMessages = None
        self._logger = logger if logger else logging.getLogger('mpcs.auto')
        self._stdoutLogger = logging.getLogger('mpcs.auto')
        self._logFile = logFile
        self._venue = None
        self._sessionId = sessionId
        self._sessionHost = sessionHost
        self._sessionScid = None
        self._uplinkRates = 'ANY'

        # MPCS-9663 5/17/18: Added fields for HTTPS support
        self._https = https
        self._restHost = restHost
        self._protocol = "https" if self._https else "http"

        #MPCS-5262  9/9/13: Added logToDb flag
        self._logToDb = logToDb

        tmpDir = "/tmp"

        if 'TMPDIR' in os.environ:
            tmpDir = os.environ['TMPDIR']
        if self._logFile:
            self._serverProcessLogName = self._logFile
        else:
            self._serverProcessLogName = os.path.join(tmpDir, '{}-{}.log'.format('chill_auto_uplink_server', long(round(time.time()))))


        # MPCS-9955 6/28/18: Added buffering param 0 for immediate flushing
        self._serverProcessLog = open(self._serverProcessLogName, 'w', 10)

        self._logger.info("AUTO uplink proxy log file written to {}".format(self._serverProcessLogName))

        #check for parameter conflicts
        sessionConfigFileSpecified = sessionConfigFile is not None

        sessionIdSpecified = sessionId is not None
        sessionHostSpecified = sessionHost is not None

        if sessionConfigFileSpecified:
            if sessionIdSpecified or sessionHostSpecified:
                raise auto.err.AUTOError('Cannot specify both sessionId or sessionHost if sessionConfigFile is specified')
        else:
            if sessionIdSpecified != sessionHostSpecified:
                raise auto.err.AUTOError('sessionId and sessionHost must be specified together')

        gdsConfig = mpcsutil.config.GdsConfig()
        self._uplinkProxyApp = gdsConfig.getProperty('automationApp.internal.auto.uplink.app.uplinkProxyApp', 'internal/chill_auto_uplink_server')
        self._uplinkProxyAppPath = '%s/%s' % (mpcsutil.chillBinDirectory, self._uplinkProxyApp)

        #prefixes
        self._proxyPort = gdsConfig.getProperty('automationApp.auto.uplink.proxy.port', '8384')

        # MPCS-9663 5/17/18: Get host information from auto spring config file for SSL
        if self._restHost == None or self._restHost == "":
            # Check AUTO proxy host/port property
            self._restHost = gdsConfig.getProperty('autoProxy.connection.host')

            # If host not set, get uplink host and default to localhost if that is not set
            if self._restHost == None or self._restHost == "":
                #TODO: Add check for sse ?
                self._restHost = gdsConfig.getProperty('connection.flight.uplink.defaultHost', "localhost")


        #arguments
        self._portArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.port', '--restPort')
        self._databaseHostArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.databaseHost', '--databaseHost')
        self._databasePortArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.databasePort', '--databasePort')
        self._databaseUserArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.databaseUser', '--dbUser')
        self._databasePasswordArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.databasePassword', '--dbPwd')
        self._fswHostArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.fswHost', '--fswUplinkHost')
        self._fswUplinkPortArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.fswUplinkPort', '--fswUplinkPort')
        self._sseHostArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.sseHost', '--sseHost')
        self._loginMethodArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.loginMethod', '--loginMethod')
        self._loginMethodVal = gdsConfig.getProperty('automationApp.auto.uplink.proxy.args.loginMethodValue', 'KEYTAB_FILE')
        self._keytabFileArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.keytabFile', '--keytabFile')
        self._usernameArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.username', '--username')
        self._roleArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.role', '--role')
        self._logFileArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.logFile', '--logFile')

        #MPCS-5262  9/9/13: Added logToDb arg
        self._logToDbArg = gdsConfig.getProperty('automationApp.internal.auto.uplink.proxy.args.logToDb', '--logToDb')

        # MPCS-9663 5/17/18: HTTPS connection support
        if self._https is True:
            with requests.Session() as _sess:
                _sess.mount('{protocol}://{host}:{port}'.format(protocol=self._protocol, host=self._restHost, port=self._proxyPort), self)

    @property
    def venue(self):
        '''
        Getter for the venue. May be None if session has not been initialized.
        Call sessionInit to initialize a session.
        '''
        return self._venue

    @property
    def sessionKey(self):
        '''
        Getter for the session key. May be None if session has not been initialized.
        Call sessionInit to initialize a session.
        '''
        return self._sessionId

    @property
    def sessionHost(self):
        '''
        Getter for the session host. May be None if session has not been initialized.
        Call sessionInit to initialize a session.
        '''
        return self._sessionHost

    @property
    def sessionScid(self):
        '''
        Getter for the session scid.  May be None if session has not been initialized.
        Call sessionInit to initialize a session.
        '''
        return self._sessionScid

    @property
    def applicationPath(self):
        '''
        Getter for the AUTO application path to determine which AUTO app is running
        '''
        return self._uplinkProxyAppPath

    @property
    def _newline(self):
        '''
        Return the newline character
        '''
        return '\n'

    def start(self):
        '''
        Start the uplink proxy
        '''

        #determine the port to use
        port = int(self._proxyPort)

        lastPort = int(self._proxyPort) + 100
        #the 100 ports from 8384 and 8483 is reserved for the AUTO uplink proxy
        while port <= lastPort:
            try:
                conn = httplib.HTTPConnection("%s:%s" % (self._restHost, port))
                conn.request('GET', '')
            except socket.error as e:
                #connection refused error. means we found an available port
                # MPCS-7928  - 2/1/2016: Need to check for ENETUNREACH on RHEL7
                if e.args[0] == errno.ECONNREFUSED or e.args[0] == errno.ENETUNREACH or e.args[0] == errno.EHOSTUNREACH:
                    self._proxyPort = str(port)
                    break
            port += 1

        if port > lastPort:
            raise auto.err.AUTOError("Cannot find an available port to start the AUTO uplink proxy")

        #start AUTO
        processString = '%s %s %s' % (self._uplinkProxyAppPath, self._portArg, self._proxyPort)

        # python openSSL version needs to be upgraded: run "openssl version" on cmdline
        # OpenSSL 0.9.8zh 14 Jan 2016 -> 'OpenSSL 1.0.2l  25 May 2017'
        # https://stackoverflow.com/questions/18752409/updating-openssl-in-python-2-7 (
        # *** IF USING BREW FOR NEW INSTALL, DONT FORGET --with-poll option
        #
        if not self._https:
            processString += ' --restInsecure'

        # Process following cmdlines only if running auto_uplink_server
        if self._databaseHost is not None and str(self._databaseHost).strip():
            processString += ' %s %s' % (self._databaseHostArg, self._databaseHost)

        if self._databasePort is not None and str(self._databasePort).strip():
            processString += ' %s %s' % (self._databasePortArg, self._databasePort)

        if self._databaseUser is not None and str(self._databaseUser).strip():
            processString += ' %s %s' % (self._databaseUserArg, self._databaseUser)

        if self._databasePassword is not None and str(self._databasePassword).strip():
            processString += ' %s %s' % (self._databasePasswordArg, self._databasePassword)

        if self._keytabFile is not None and str(self._keytabFile).strip():
            processString += (' %s %s ' % (self._loginMethodArg, self._loginMethodVal))
            processString += (' %s %s' % (self._keytabFileArg, self._keytabFile))
            processString += (' %s %s' % (self._usernameArg, self._username))

        if self._userRole is not None and str(self._userRole).strip():
            processString += (' %s %s' % (self._roleArg, self._userRole))

        if self._logFile is not None and str(self._logFile).strip():
            processString += (' %s %s' % (self._logFileArg, self._logFile))

         # MPCS-5262 9/9/13: Added logToDb flag
        if self._logToDb:
            processString += (' %s' % (self._logToDbArg))

        self._logger.debug(processString)
        self._uplinkServerProcess = subprocess.Popen(processString,
                                                     shell=True,
                                                     stdout=self._serverProcessLog,
                                                     stderr=self._serverProcessLog,
                                                     stdin=subprocess.PIPE,
                                                     env=os.environ)

        # MPCS-4700 - Added a thread that monitors the uplink log proxy.
        # 5/24/13     Logs to the database are disabled if this fails to start up
        #       or if it exits unexpectedly.
        uplink_mon_thread = threading.Thread(target=self.monitor, name='monitor')
        isAutoStarted = False
        startAutoAttempts = 10
        resp = None

        #MPCS-5281  9/16/13: Make uplink monitor thread a daemon so it doesn't hold up shutdown.
        uplink_mon_thread.setDaemon(True)
        uplink_mon_thread.start()

        self._logger.debug("Pinging AUTO @ URL url={protocol}://{host}:{port}/auto/status".format(protocol=self._protocol, host=self._restHost, port=port))
        while not isAutoStarted and startAutoAttempts > 0:
            try:
                resp = self.getStatus()
                if resp is not None and resp == "OK":
                    self._logger.debug("Received AUTO proxy status response %s" % resp)
                    isAutoStarted = True
                    startAutoAttempts = 0
            except Exception as e:
                self._logger.debug("Unable to connect to AUTO uplink proxy. Attempt: %d %s" % ((10 - (startAutoAttempts - 1)),e))

            startAutoAttempts -= 1
            time.sleep(3)

        if not isAutoStarted:
            raise auto.err.AmpcsError("Unable to GET AUTO status @ {protocol}://{host}:{port}/auto/status".format(port=port,host=self._restHost, protocol=self._protocol))

        # initialize a session
        if self._sessionConfigFile is not None:
            self.sessionInit(sessionConfigFile=self._sessionConfigFile)
        elif self._sessionId is not None and self._sessionHost is not None:
            self.sessionInit(sessionId=self._sessionId, sessionHost=self._sessionHost)
        else:
            self.sessionInit()

    def getStatus(self, **kwargs):
        '''
        Send a GET status request to AUTO to determine if it is up

        :returns: response
        '''
        resp = self._makeProxyRequest('GET', "auto/status", **kwargs)
        self._logger.debug('Proxy Response: %s' % (resp))
        return resp



    def sendLog(self, message, level, **kwargs):
        '''
        Send a request to AMPCS to log a message

        Args
        ____
        message - The message to log

        level - The log message severity

        **kwargs - response keyword arguments
        '''
        params = urlencode({'level': level, 'message': message})
        #must only use _stdoutLogger here, otherwise we go into a logging infinite loop
        #self._stdoutLogger.debug(uri)
        resp = self._makeProxyRequest('POST', "auto/log", params, **kwargs)
        self._stdoutLogger.debug('Proxy Response: %s' % (resp))

    def sessionInit(self, sessionConfigFile=None, sessionId=None, sessionHost=None, **kwargs):
        '''
        Initializes a session in AMPCS

        Args
        ____
        sessionConfigFile - the session configuration file to use to start a new
                            AMPCS session. If none, the most recent session found
                            in the AMPCS database will be used.
        '''

        params = ''

        if sessionConfigFile is not None:
            self._logger.debug('Initializing new session with %s' % (sessionConfigFile))
            params = urlencode({'sessionConfigFile': sessionConfigFile})
        elif sessionId is not None and sessionHost is not None:
            self._logger.debug('Attaching to session (id=%d, host=%s)' % (sessionId, sessionHost))
            params = urlencode({'sessionId': sessionId, 'sessionHost': sessionHost})
        else:
            self._logger.debug('Attaching to latest uplink session')

        resp = self._makeProxyRequest('POST', "auto/session", params, **kwargs)
        self._logger.debug('AUTO Proxy Response: %s' % (resp))

        resp_dict = dict([kv.split('=') for kv in resp.split(',')])

        (self._venue, self._sessionId, self._sessionHost, self._sessionScid) = (resp_dict['venue'], resp_dict['key'], resp_dict['host'], resp_dict['scid'])

    def sendScmf(self, scmfFile, validateScmf=False, waitForRadiation=0, **kwargs):
        '''
        Send an SCMF

        Args
        ____
        scmfFile - path to the SCMF file to send

        waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
        This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
        uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
        (optional) (positive int)

        **kwargs - response keyword arguments
        '''
        if scmfFile is None:
            raise auto.err.AUTOError('SCMF file cannot be None')

        params = urlencode({'scmfFile': scmfFile, 'validateScmf': validateScmf, 'waitForRadiation': waitForRadiation, 'uplinkRates': self._uplinkRates})

        resp = self._makeProxyRequest('POST', "auto/send_scmf", params, **kwargs)
        self._logger.debug('Proxy Response: %s' % (resp))

    def sendPdu(self, pduFile, entityId, vcid=None, **kwargs):
        '''
        Send a PDU file

        Args
        ____
        pduFile - path to the pdu file to send

        entityId - the destination entityId

        vcid - (optional) the vcid destination

        **kwargs - response keyword arguments
        '''
        if pduFile is None or not os.path.exists(pduFile):
            raise auto.err.AUTOError('PDU file is not valid')
        if entityId is None or not entityId.isdigit():
            raise auto.err.AUTOError('EntityId must be a number')
        if vcid is not None and not vcid.isdigit():
            raise ValueError('Vcid must be a number')

        params = urlencode({'destinationEntityId' : entityId})
        uri="auto/send_pdu_file" if vcid is None else "auto/send_pdu_file/" + vcid

        if pduFile is not None:
            kwargs['files'] = {'pduFile': open(pduFile, 'rb')}

        resp = self._makeProxyRequest('POST', uri, params, **kwargs)
        self._logger.debug('Proxy Response: %s' % (resp))


    def setUplinkRates(self, uplinkRates=None):
        '''
        Set the uplink rates that will be attached to subsequent uplinks. This is only applicable
        when using the COMMAND_SERVICE uplink connection type

        Arguments
        ----------
        uplinkRates - A list of uplink rates in bits per second
        that subsequent requests may be radiated with. Defaults to ANY if not
        specified.
        '''

        self._logger.debug('auto.up.UplinkProxy.setUplinkRate()')

        uplinkRates = ('ANY',) if uplinkRates is(None) else uplinkRates

        uplinkRatesStr = ""

        try:
            #if we didnt get a list but instead got a number, convert it
            if isinstance(uplinkRates, (float, int)):
                uplinkRates = (uplinkRates,)

            uplinkRatesStr = ','.join('{}'.format(_uplink_rate) for _uplink_rate in uplinkRates)

        except TypeError:
            self._logger.warning('Encountered invalid uplink rate')
            raise

        self._logger.info('auto.up.UplinkProxy.setUplinkRate(uplinkRates={})'.format(uplinkRatesStr))

        self._uplinkRates = uplinkRatesStr

    def _makeProxyRequest(self, method, uri, params=None, body=None, **kwargs):
        if self._exitCode is not None:
            #do not use main logger, since main logger needs the proxy.
            self._stdoutLogger.fatal('AMPCS proxy exited unexpectedly')
            raise auto.err.AUTOError('AMPCS proxy exited unexpectedly:\n{}'.format(self._errorMessages))

        if uri is None:
            self._logger.warning('URI not specified, proxy request aborted')
            return
        if params is None:
            self._logger.warning("Parameters not specified")

        # MPCS-7278  - 05/13/15 (Ported as MPCS-8849 on 06/14/17):
        # No longer use http, communicate over subprocess STDIN
        # MPCS-9155  - 10/3/17. Revert the above change
        # Continue to use http for communication and address security later
        full_uri = "{protocol}://{host}:{port}/{uri}".format(port=self._proxyPort, uri=uri, host=self._restHost, protocol=self._protocol)

        #must only use _stdoutLogger here, otherwise we go into a logging infinite loop
        self._stdoutLogger.debug("Requesting {} to URI {}".format(method, full_uri))
        self._stdoutLogger.debug("With parameters {} ".format(params))
        self._stdoutLogger.debug("{}{}".format(full_uri, '?{}'.format(params) if params else ''))

        # resp = requests.Session().request(method=method, url=full_uri, params=params, **kwargs)
        with requests.Session() as _sess:
            resp=_sess.request(method=method, url=full_uri, params=params, **kwargs)

        err = auto.err.get_error_from_http_status(resp.status_code, resp.reason)

        if err is not None:
            self._logger.error(resp.text)
            raise err
        else:
            return resp.text

    def stop(self):
        '''
        Stop the uplink proxy
        '''
        #check for termination
        self._uplinkServerProcess.poll()

        #MPCS-5395  9/30/13: Reversed incorrect condition check for termination
        if self._uplinkServerProcess.returncode is None:
            self._uplinkServerProcess.terminate()

        self._serverProcessLog.close()

    # MPCS-4700 - Added a thread that monitors the uplink log proxy.
    # 5/24/13     Logs to the database are disabled if this fails to start up
    #       or if it exits unexpectedly.
    def monitor(self):
        '''
        Wait for the uplink proxy to quit, then set the exit code and any error messages
        '''
        #wait for process to terminate

        while self._exitCode is None:
            time.sleep(2)
            self._uplinkServerProcess.poll()

            self._exitCode = self._uplinkServerProcess.returncode

        if self._exitCode != 0:
            self._errorMessages = 'Uplink proxy terminated with errors, exit code %d. See %s for more details' % (self._exitCode, self._serverProcessLogName)
        else:
            self._errorMessages = 'Uplink proxy terminated with exit code 0'


def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
