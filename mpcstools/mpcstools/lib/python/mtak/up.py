#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
Blah.
"""

from __future__ import (absolute_import, division, print_function)


#6/27/13 - MPCS-4992: replaced password file with keytab file
import sys
# sys.stdin=open('/dev/tty')
import traceback
import errno
import logging
import mpcsutil
import mtak
import os.path
import signal
import select
import subprocess
import socket
# import sys
import time
import auto
import six
import traceback
import threading
import re
import pty
import pexpect
import collections
from mpcsutil import NamedTuple
from contextlib import contextmanager

if six.PY2:
    from Queue import Queue, Empty
else:
    from queue import Queue, Empty
    long=int


ON_POSIX = 'posix' in sys.builtin_module_names
_log = lambda : logging.getLogger('mpcs.mtak')

pcmd_dd = collections.OrderedDict([
    ('prefix','CMD'),
    ('validate',''),
    ('string_id',''),
    ('vcid',''),
    ('scid',''),
    ('wait_for_radiation',''),
    ('command','')])
ProxyCommand = NamedTuple('ProxyCommand', pcmd_dd.keys(), pcmd_dd)

pfile_dd = collections.OrderedDict([
    ('prefix','FILE'),
    ('overwrite',''),
    ('string_id',''),
    ('vcid',''),
    ('scid',''),
    ('source',''),
    ('target',''),
    ('type',''),
    ('wait_for_radiation','')])
ProxyFile = NamedTuple('ProxyFile', pfile_dd.keys(), pfile_dd)

pscmf_dd = collections.OrderedDict([
    ('prefix','SCMF'),
    ('disable_checks',''),
    ('scmf_string',''),
    ('wait_for_radiation','')])
ProxyScmf = NamedTuple('ProxyScmf', pscmf_dd.keys(), pscmf_dd)

praw_dd = collections.OrderedDict([
    ('prefix','RAW'),
    ('hex',''),
    ('filename_string',''),
    ('wait_for_radiation','')])
ProxyRaw = NamedTuple('ProxyRaw', praw_dd.keys(), praw_dd)

plog_dd = collections.OrderedDict([
    ('prefix','LOG'),
    ('level','INFO'),
    ('message','')])
ProxyLog = NamedTuple('ProxyLog', plog_dd.keys(), plog_dd)

class UplinkProxy(mtak.AbstractProxy):
    ''' The UplinkProxy is the MTAK object that enables the ability to send uplink through MPCS.  This class
    can send flight commands (hardware and FSW commands), SSE commands, file loads, raw data files, and SCMFs.
    It requires a valid session config object in order to be used.

    Object Attributes
    ------------------
    failedFlightCommands - The number of failed hw/fsw command transmissions (long)
    flightCommandsSent - The number of successful hw/fsw command transmissions (long)
    failedSseCommands - The number of failed SSE command transmissions (long)
    sseCommandsSent - The number of successful SSE command transmissions (long)
    failedFileLoads - The number of failed file load transmissions (long)
    fileLoadsSent - The number of successful file load transmissions (long)
    failedScmfs - The number of failed scmf transmissions (long)
    scmfsSent - The number of successful scmf transmissions (long)
    failedRawDatas - The number of failed raw data file transmissions (long)
    rawDatasSent - The number of successful raw data file transmissions (long)
    successfulCfdpPuts - The number of successful CFDP PUTs (long)
    failedCfdpPuts - The number of failed CFDP PUTs (long)
    _sessionConfig = The session config object used by the proxy to supply information to the MPCS
                         uplink applications (e.g. test key, hostnames, port numbers) (mpcsutil.config.SessionConfig)
    _sessionKey = The session key from the input session config (long)
    _ssePrefix = The configured prefix used to denote an SSE command.  Defaults to "sse:" if undefined
                         in the GDS config object. (string)
    _sessionKeyArg =  The MPCS command line argument for a session key (string)
    _ssePrefix - The prefix attached to the front of SSE commands (string)
    _mission - The current mission (string)
    _isSse - Whether or not the current mission is SSE (boolean)
    _hasSse - Whether or not the current mission has an SSE (boolean)
    _hasCfdp - Whether or not the current mission has CFDP support (boolean)'''

    def __init__(self, sessionConfig):
        ''' Initialize the uplink proxy.  Requires a valid session config.

        Args
        -----
        sessionConfig - The session config specifying the session that all uplink instances instantiated by the
                        MTAK will be associated with. (mpcsutil.config.SessionConfig)

        Returns
        --------
        None

        Raises
        -------
        InvalidInitError - If the input session config is missing a test key value'''

        self._running = False
        self._uplinkServerProcess = None
        self._sendServerScript = None
        self._serverStdout = None
        self._serverStderr = None
        self._serverStdin = None
        self._serverStdoutFileNo = None
        self._serverStderrFileNo = None
        self._serverStdinFileNo = None
        self._poller = None

        mtak.AbstractProxy.__init__(self, sessionConfig)

        self.failedFlightCommands = long(0)
        self.flightCommandsSent = long(0)
        self.failedSseCommands = long(0)
        self.sseCommandsSent = long(0)
        self.failedFileLoads = long(0)
        self.fileLoadsSent = long(0)
        self.failedScmfs = long(0)
        self.scmfsSent = long(0)
        self.failedRawDatas = long(0)
        self.rawDatasSent = long(0)
        self.successfulCfdpPuts = long(0)
        self.failedCfdpPuts = long(0)
        self.uplinkConnectionType = sessionConfig.uplinkConnectionType

        if hasattr(sessionConfig, 'uplinkKeytabFile'):
            self.uplinkKeytabFile = sessionConfig.uplinkKeytabFile

        if hasattr(sessionConfig, 'uplinkUserRole'):
            self.uplinkUserRole = sessionConfig.uplinkUserRole

        if hasattr(sessionConfig, 'uplinkUsername'):
            self.uplinkUsername = sessionConfig.uplinkUsername

    def isRunning(self):
        return self._running

    def _readConfig(self):
        '''Read all the values from the GDS configuration that this object uses internally.  Calling this function
        will cause all the attributes on this object to be written or overwritten with the values stored in the
        GdsConfig singleton.

        Args
        -----
        None

        Returns
        --------
        None'''

        _log().debug('mtak.up.UplinkProxy._readConfig()')

        gdsConfig = mpcsutil.config.GdsConfig()

        #Set up the script to send commands
        _sendServerApp = gdsConfig.getProperty('automationApp.internal.mtak.app.upServer', 'internal/chill_mtak_uplink_server')
        self._sendServerScript = '%s/%s' % (mpcsutil.chillBinDirectory, _sendServerApp)
        if not os.path.isfile(self._sendServerScript):
            errString = 'Cannot find the required script ' + self._sendServerScript
            _log().critical(errString)
            raise mpcsutil.err.EnvironmentError(errString)
        _log().debug('Using the script %s to send commands' % (self._sendServerScript))

        self._sessionKeyArg = gdsConfig.getProperty('automationApp.internal.mtak.args.sessionKey', '-K')
        self._fswHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.fswHost', '--fswUplinkHost')
        self._fswUplinkPortArg = gdsConfig.getProperty('automationApp.internal.mtak.args.fswUplinkPort', '--fswUplinkPort')
        self._sseHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.sseHost', '--sseHost')
        self._sseUplinkPortArg = gdsConfig.getProperty('automationApp.internal.mtak.args.sseUplinkPort', '--sseUplinkPort')
        self._databaseHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databaseHost', '--databaseHost')
        self._databasePortArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databasePort', '--databasePort')
        self._databaseUserArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databaseUser', '--dbUser')
        self._databasePasswordArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databasePassword', '--dbPwd')
        self._jmsHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.jmsHost', '--jmsHost')
        self._jmsPortArg = gdsConfig.getProperty('automationApp.internal.mtak.args.jmsPort', '--jmsPort')
        self._loginMethodArg = gdsConfig.getProperty('automationApp.internal.mtak.args.loginMethod', '--loginMethod')
        self._loginMethodVal = gdsConfig.getProperty('automationApp.mtak.args.loginMethodValue', 'KEYTAB_FILE')
        self._keytabFileArg = gdsConfig.getProperty('automationApp.internal.mtak.args.keytabFile', '--keytabFile')
        self._roleArg = gdsConfig.getProperty('automationApp.internal.mtak.args.role', '--role')
        self._usernameArg = gdsConfig.getProperty('automationApp.internal.mtak.args.username', '--username')

        # MPCS-9879 - 6/4/18 - This property name was wrong.
        self._ssePrefix = gdsConfig.getProperty('command.SSE.prefix', 'sse:')
        self._commandPrefix = gdsConfig.getProperty('automationApp.mtak.uplink.prefixes.command', 'CMD')
        self._filePrefix = gdsConfig.getProperty('automationApp.mtak.uplink.prefixes.file', 'FILE')
        self._scmfPrefix = gdsConfig.getProperty('automationApp.mtak.uplink.prefixes.scmf', 'SCMF')
        self._rawPrefix = gdsConfig.getProperty('automationApp.mtak.uplink.prefixes.raw', 'RAW')
        self._logPrefix = gdsConfig.getProperty('automationApp.mtak.uplink.prefixes.log', 'LOG')
        self._cmdListPrefix = gdsConfig.getProperty('automationApp.mtak.uplink.prefixes.cmdlist', 'CMDLIST')
        self._uplinkRatePrefix = gdsConfig.getProperty('automationApp.mtak.uplink.prefixes.uplinkRate', 'UPLINKRATE')
        self._pollingTimeout = int(gdsConfig.getProperty('automationApp.mtak.uplink.pollingTimeoutMillis', 1000))
        self._bufferSize = int(gdsConfig.getProperty('automationApp.mtak.uplink.bufferSize', 1))
        self._delimiter = gdsConfig.getProperty('automationApp.mtak.uplink.delimiter', ',')
        self._terminator = gdsConfig.getProperty('automationApp.mtak.uplink.terminator', ';;;')
        self._newline = gdsConfig.getProperty('automationApp.mtak.uplink.newline', ';')
        self._mission = gdsConfig.getMission()
        self._isSse = gdsConfig.isSse()
        self._hasSse = gdsConfig.hasSse()
        self._hasCfdp = gdsConfig.hasCfdp()

    def start(self):
        #We're already running...do nothing
        if self._running == True:
            _log().warning('Uplink Proxy has already been started.  Make sure you did not call "start()" twice.')
            return

        #Reset message counts
        self.failedFlightCommands = long(0)
        self.flightCommandsSent = long(0)
        self.failedSseCommands = long(0)
        self.sseCommandsSent = long(0)
        self.failedFileLoads = long(0)
        self.fileLoadsSent = long(0)
        self.failedScmfs = long(0)
        self.scmfsSent = long(0)
        self.failedRawDatas = long(0)
        self.rawDatasSent = long(0)
        self.successfulCfdpPuts = long(0)
        self.failedCfdpPuts = long(0)

        _args = [self._sendServerScript, self._sessionKeyArg, self._sessionConfig.key, self._fswHostArg, self._sessionConfig.fswUplinkHost, self._fswUplinkPortArg, self._sessionConfig.fswUplinkPort]


        if self._isSse or self._hasSse:
            _args.extend([self._sseHostArg, self._sessionConfig.sseHost,self._sseUplinkPortArg, self._sessionConfig.sseUplinkPort])

        db_host = mpcsutil.database.getDatabaseHost()
        if db_host is not None and str(db_host).strip():
            _args.extend([self._databaseHostArg, db_host])

        db_port = mpcsutil.database.getDatabasePort()
        if db_port is not None and str(db_port).strip():
            _args.extend([self._databasePortArg, db_port])

        db_user = mpcsutil.database.getDatabaseUserName()
        if db_user is not None and str(db_user).strip():
            _args.extend([self._databaseUserArg, db_user])

        db_pwd = mpcsutil.database.getDatabasePassword()
        if db_pwd is not None and str(db_pwd).strip():
            _args.extend([self._databasePasswordArg, db_pwd])

        jms_host = mpcsutil.config.getJmsHost()
        if jms_host is not None and str(jms_host).strip():
            _args.extend([self._jmsHostArg, jms_host])

        jms_port = mpcsutil.config.getJmsPort()
        if jms_port is not None and str(jms_port).strip():
            _args.extend([self._jmsPortArg, jms_port])

        #MPCS 6264 - 6/25/2014: this attribute is only added dynamically, so it should not be expected to exist
        if hasattr(self, 'uplinkKeytabFile') and self.uplinkKeytabFile is not None and str(self.uplinkKeytabFile).strip():
            _args.extend([self._loginMethodArg, self._loginMethodVal, self._keytabFileArg, self.uplinkKeytabFile])

        #MPCS 6264  - 6/25/2014: this attribute is only added dynamically, so it should not be expected to exist
        if hasattr(self, 'uplinkUserRole') and self.uplinkUserRole is not None and str(self.uplinkUserRole).strip():
            _args.extend([self._roleArg, self.uplinkUserRole])

        #MPCS 6264  - 6/25/2014: this attribute is only added dynamically, so it should not be expected to exist
        if hasattr(self, 'uplinkUsername') and self.uplinkUsername is not None and str(self.uplinkUsername).strip():
            _args.extend([self._usernameArg, self.uplinkUsername])

        processString = ' '.join('{}'.format(aa) for aa in _args)

        _log().debug('Launching process %s' % (processString))

        if not self._isSse:
            self._checkFswNetworkSettings()

        if self._hasSse or self._isSse:
            self._checkSseNetworkSettings()

        self._running = False
        # self.kill_event=threading.Event()
        _dd = dict(env=os.environ, timeout=120)
        if six.PY3: _dd.update(dict(encoding='utf-8'))
        _errors=[]

        self._uplinkServerProcess = pexpect.spawn(processString, **_dd)

        try:
            while not(self._running):
                ii = self._uplinkServerProcess.expect([pexpect.TIMEOUT, 'MTAK uplink server ready for commands', pexpect.EOF, '^.*(ERROR|CRITICAL|FATAL).*$']) #, timeout=120)
                if ii==0:
                    raise mpcsutil.err.InvalidStateError('Errors while launching uplink server process:\n{}\n'.format(processString))
                elif ii==1:
                    self._running = True
                elif ii==2:
                    raise mpcsutil.err.InvalidStateError('Could not launch uplink server process {}'.format(processString))
                elif ii==3:
                    _errors.extend(list(map(lambda x: x.strip(), self._uplinkServerProcess.after.split('\r\n'))))
                else:
                    raise mpcsutil.err.InvalidStateError('Errors while launching uplink server process:\n{}\n'.format(processString))
        except mpcsutil.err.InvalidStateError as e:
            _log_pattern=r'^.*(INFO|WARN|ERROR|FATAL|CRITICAL).*$'
            _done=None
            if self._running:
                self.stop()

            while _done is(None):
                jj = self._uplinkServerProcess.expect([pexpect.TIMEOUT, _log_pattern, pexpect.EOF], timeout=2)
                if jj == 1:
                    _lines=self._uplinkServerProcess.before.split('\r\n') + self._uplinkServerProcess.after.split('\r\n')
                    _lines = list(filter(lambda x: bool(x), map(lambda y: y.strip(), _lines)))
                else:
                    _lines=self._uplinkServerProcess.before.split('\r\n')
                    _lines = list(filter(lambda x: bool(x), map(lambda y: y.strip(), _lines)))
                    _done=True
                _lines = [ll for ll in _lines if ll not in _errors]
                _errors+=_lines

            six.raise_from(mpcsutil.err.InvalidStateError('Errors while launching uplink server process:\n{}\n'.format('\n'.join(_errors))), None)

        except (OSError, IOError) as exc:
            if not(bool(hasattr(exc, 'errno') and (exc.errno in [errno.EINTR]))):
                _log().error('Background uplink server thread encountered an error: {}'.format(sys.exc_info()))
                if self._running: self.stop()
                raise

        if _errors:
            time.sleep(7)
            if not(self._uplinkServerProcess.isalive()):
                error = auto.err.get_error(self._uplinkServerProcess.exitstatus)
                if error is not(None) and not(isinstance(error, auto.err.AUTOError)):
                    raise error
            if self._running: self.stop()
            raise mpcsutil.err.InvalidStateError('Errors while launching uplink server process:\n{}\n'.format('\n'.join(_errors)))

    def stop(self):
        if self._running == False:
            _log().warning('Uplink Proxy is already stopped.  Make sure you did not call "stop()" twice.')
            return

        try:
            # MPCS-12139: forcing termination will ungracefully stop the auto uplink proxy. terminate, then check for liveness
            current = time.time()
            #arbitrary limit; should take < 5s to shutdown uplink proxy
            limit = current + 20
            result = self._uplinkServerProcess.terminate(force=False)
            if not result:
                while time.time() < limit:
                    if self._uplinkServerProcess.isalive():
                        time.sleep(1)
                    else:
                        break
            if self._uplinkServerProcess.isalive():
                self._uplinkServerProcess.terminate(force=True)
                _log().warning('MTAK Uplink Proxy shutdown signal has been forced')
            _log().debug("MTAK Uplink Proxy shutdown took: ", time.time() - current, " s")
            self._running = False
            _log().info('MTAK Uplink Proxy shutdown signal has been sent')
        except OSError:
            _log().error('There was an error while attempting to kill the background Java uplink server process: %s' % (str(sys.exc_info())))
        finally:
            mtak.AbstractProxy._destroyCoreLogHandlers()

    def _sendString(self, uplinkString, doLog=True):
        _log_pattern=r'^.*(INFO|WARN|ERROR|FATAL|CRITICAL).*$'
        _success_pattern=r'(Success=)([\d][\d]?)'
        #Check if the proxy is running
        if not self._running:
            raise mpcsutil.err.InvalidStateError('The MTAK Uplink proxy is not running. Make sure the session MTAK is attached to is configured for uplink.')

        #Need to terminate the input
        if isinstance(uplinkString, tuple):
            uplinkString = self._delimiter.join('{}'.format(aa) for aa in uplinkString)

        sendString = '{}{}'.format(uplinkString, self._terminator)

        def _handle_success_lines(*_lines):
            def _extract(_line):
                _line = _line.strip()
                if not(_line): return
                _match = re.match(_success_pattern, _line)
                if not(_match): return
                _, _rc = _match.groups()
                return _rc
            return next(iter(list(filter(lambda x: x is not(None), map(_extract, _lines)))), None)

        _return_code=None
        _out=[]
        if self._uplinkServerProcess.isalive():
            self._uplinkServerProcess.sendline(sendString)

            while _return_code is(None):
                ii = self._uplinkServerProcess.expect([pexpect.TIMEOUT, pexpect.EOF, _log_pattern, _success_pattern])
                if ii in [0, 1]:
                    # print('Case 0|1')
                    break

                if ii == 2:
                    # print('Case 2')
                    _lines=self._uplinkServerProcess.before.split('\r\n') + self._uplinkServerProcess.after.split('\r\n')
                    _success = list(filter(lambda x: re.match(_success_pattern, x), _lines))
                    _lines = list(filter(lambda x: re.match(_log_pattern, x) and (sendString not in x) , _lines))
                    _lines = [ll for ll in _lines if ll not in _out]
                    print('\n'.join(_lines))
                    _out+=_lines
                    _return_code=_handle_success_lines(*_success)

                if ii == 3:
                    # print('Case 3')
                    _lines=self._uplinkServerProcess.before.split('\r\n') + self._uplinkServerProcess.after.split('\r\n')
                    _lines = list(filter(lambda x: re.match(_log_pattern, x) and (sendString not in x) , _lines))
                    _lines = [ll for ll in _lines if ll not in _out]
                    print('\n'.join(_lines))
                    _out+=_lines

                    _, _return_code=self._uplinkServerProcess.match.groups()

            if _return_code is(None):
                raise mtak.err.RadiationError('Error radiating uplink')

            _return_code = _return_code.strip()
            _return_code = int(_return_code) if re.match(r'^([\d]+)$', _return_code) else None

            _done=None
            while _done is(None):
                jj = self._uplinkServerProcess.expect([pexpect.TIMEOUT, _log_pattern, pexpect.EOF], timeout=1)
                if jj == 1:
                    _lines=self._uplinkServerProcess.before.split('\r\n') + self._uplinkServerProcess.after.split('\r\n')
                    _lines = list(filter(lambda x: bool(x), map(lambda y: y.strip(), _lines)))
                else:
                    _lines=self._uplinkServerProcess.before.split('\r\n')
                    _lines = list(filter(lambda x: bool(x), map(lambda y: y.strip(), _lines)))
                    _done=True
                _lines = [ll for ll in _lines if ll not in _out]
                print('\n'.join(_lines))
                _out+=_lines


            _exc = auto.err.get_error(_return_code, '\n'.join( list(filter(lambda x: not(re.match(r'^.*(WARN|INFO).*$', x)), _out )) ) ) if _return_code else None
            if isinstance(_exc, Exception):
                raise _exc

        else:
            raise OSError('Uplink server process died unexpectedly. Cannot transmit any uplink.')

    def getSummary(self):
        '''Generate a summary of this uplink proxy's activity.

        Args
        -----
        None

        Returns
        --------
        A string containing a summary of all the activity of this uplink proxy. (string)'''

        _log().debug('mtak.up.UplinkProxy.getSummary()')

        summary = [
            '-'*28,
            'Uplink Transmissions',
            '-'*28,
            '',
            'Total Successful Flight Commands = {}'.format(self.flightCommandsSent),
            'Total Failed Flight Commands = {}'.format(self.failedFlightCommands),
            '',
            'Total Successful File Load Sends = {}'.format(self.fileLoadsSent),
            'Total Failed File Load Sends = {}'.format(self.failedFileLoads),
            '',
            'Total Successful SCMF Sends = {}'.format(self.scmfsSent),
            'Total Failed SCMF Sends = {}'.format(self.failedScmfs),
            '',
            'Total Success Raw Data File Sends = {}'.format(self.rawDatasSent),
            'Total Failed Raw Data File Sends = {}'.format(self.failedRawDatas)]

        if self._hasSse or self._isSse:
            summary.extend([
                'Total Successful SSE Commands = {}'.format(self.sseCommandsSent),
                'Total Failed SSE Commands = {}'.format(self.failedSseCommands)])

        if self._hasCfdp:
            summary.extend([
                'Total Successful CFDP PUTs = {}'.format(self.successfulCfdpPuts),
                'Total Failed CFDP PUTs = {}'.format(self.failedCfdpPuts)])

        return '\n'.join(summary)

    def setUplinkRates(self, uplinkRates=None):
        '''
        Set the uplink rates that will be attached to subsequent uplinks. This is only applicable
        when using the COMMAND_SERVICE uplink connection type

        Arguments
        ----------
        uplinkRates - An uplink rate (or a comma-separated list of rates) in bits per second
        that the request may be radiated with. Defaults to ANY if not
        specified.
        '''
        if self.uplinkConnectionType != "COMMAND_SERVICE":
            _log().debug('Ignoring setUplinkRates() call since uplinkConnectionType != COMMAND_SERVICE')
            return

        _log().debug('mtak.up.UplinkProxy.setUplinkRate()')

        uplinkRates = uplinkRates if uplinkRates is not None else ('ANY',)

        uplinkRatesStr = ""

        try:
            if uplinkRates is not None and (isinstance(uplinkRates, float) or isinstance(uplinkRates, int)):
                uplinkRates = (uplinkRates,)

            for ur in uplinkRates:
                uplinkRatesStr += str(ur) + ","

            uplinkRatesStr = uplinkRatesStr[0:len(uplinkRatesStr) - 1]
        except TypeError as e:
            uplinkRatesStr = "ANY"

        _log().info('mtak.up.UplinkProxy.setUplinkRate(uplinkRates="{}")'.format(uplinkRatesStr))

        setUplinkRateString = '{}{}{}'.format(self._uplinkRatePrefix, self._delimiter, uplinkRatesStr)

        _log().debug(setUplinkRateString)

        exc = None
        try:

            self._sendString(setUplinkRateString)

        except mtak.err.RadiationError as re:

            exc = re

        if exc is not None:
            _log().error('Set uplink rate failed.')
            raise exc
        else:
            _log().info('Uplink rate set to {}'.format(uplinkRatesStr))

    #The equivalent to "load cmd list..." in the chill_up GUI
    def sendCommandListFile(self, filename, validate=None, stringId=None, virtualChannel=None, scid=None, waitForRadiation=0):
        _log().debug('mtak.up.UplinkProxy.sendCommandListFile()')

        commandString = str(filename)
        validate = validate if validate is not None else ''
        stringId = stringId if stringId is not None else ''
        virtualChannel = virtualChannel if virtualChannel is not None else ''
        scid = scid if scid is not None else ''

        _log().info('mtak.up.UplinkProxy.sendCommandListFile(filename="{}",validate="{}",stringId="{}",virtualChannel="{}",scid="{}",waitForRadiation={})'.format(commandString, validate, stringId, virtualChannel, scid, waitForRadiation))

        _args = [self._cmdListPrefix, self._delimiter,validate, self._delimiter,stringId, self._delimiter,virtualChannel, self._delimiter,scid, self._delimiter,filename, self._delimiter, waitForRadiation]
        uplinkString = ''.join('{}'.format(aa) for aa in _args)
        _log().debug(uplinkString)

        exc = None
        try:

            self._sendString(uplinkString)

        except (mtak.err.RadiationError, auto.err.AUTOError, auto.err.AmpcsError) as e:

            exc = e

        #The worst part of this is trying to count how many commands we've sent
        with open(filename) as file:
            for line in file:

                #Strip out all the comments and skip the lines that are all comments
                try:
                    index = line.index('//')
                    line = line[0:index]
                except ValueError:
                    pass
                line = line.strip()

                if not line:
                    continue

                if not commandString.startswith(self._ssePrefix):
                    if exc is not None:
                        self.failedFlightCommands += 1
                    else:
                        self.flightCommandsSent += 1
                else:
                    if exc is not None:
                        self.failedSseCommands += 1
                    else:
                        self.sseCommandsSent += 1

        if exc is not None:
            _log().error('Transmission failed.')
            raise exc
        else:
            _log().info('Transmission succeeded.')


    #The interface for sending hardware and FSW commands to the flight software
    def sendFlightCommand(self, command, validate=None, stringId=None, virtualChannel=None, scid=None, waitForRadiation=0):
        '''Send a hardware command, flight software command, or sequence directive

        Args
        -----
        command - A string, a HardwareCommand object, a FlightSoftwareCommand object, or a SequenceDirective object

        If "command" is a HardwareCommand objec, a FlightSoftwareCommand object, or a SequenceDirective object
        it will be automatically formatted and sent by this method.

        If "command" is a string object, it should be formatted in the same way
        that commands are formatted for input to the MPCS chill_send_cmd application (see description below):

        Individual Command Format:
        'stem,arg1_value,...,argN_value'
                OR
        '0xopcode,arg1_value,...,argN_value'
        (NOTE: The entire command is enclosed in single quotes)

        Argument Value Format (within the command format):
                Fill Arg Value: Fill arguments are not input (skip them)!
                Numeric Arg Value: value
                Look Arg Value: value
                String Arg Value: "value"
                (NOTE: String values should be enclosed in double quotes)
                Repeat Arg Value: #_repeats,arg1_value,...,argN_value

        Any argument value may be specified in ASCII format by simply typing its value.
        Any argument value may be specified  in hexadecimal or binary instead of ASCII by
        preceding it with a 0x or 0b.  Similarly, an opcode may be specified in place of the
        stem  value if it is preceded with a 0x or a 0b.  If an argument has a default value,
        the ASCII value "default" can be specified on the
        command line in place of an argument value.

        waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
        This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
        uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
        (optional) (positive int)

        Returns
        --------
        None

        Raised Exceptions
        ------------------
        InvalidStateError - If the FSW host and port have not been set in the session config
        RadiationError - If the MPCS (chill_send_cmd) application could not radiate the command'''

        _log().debug('mtak.up.UplinkProxy.sendFlightCommand()')

        commandString = str(command)
        validate = validate if validate is not None else ''
        stringId = stringId if stringId is not None else ''
        virtualChannel = virtualChannel if virtualChannel is not None else ''
        scid = scid if scid is not None else ''
        waitForRadiation = waitForRadiation if waitForRadiation is not None else 0

        _log().info('mtak.up.UplinkProxy.sendFlightCommand(command="{}",validate="{}",stringId="{}",virtualChannel="{}",scid="{}",waitForRadiation="{}")'.format(commandString, validate, stringId, virtualChannel, scid, waitForRadiation))

        #Strip off any leading or trailing quotes marks on the command
        # if (commandString.startswith("\"") and commandString.endswith("\"")) or (commandString.startswith("'") and commandString.endswith("'")): commandString = commandString[1:-1]
        _match=re.match(r'("|\')(?P<cmd>.+)("|\')$', commandString)
        if _match:
            commandString=next(iter(_match.groupdict().values()))

        #Make sure this isn't actually an SSE command
        if commandString.startswith(self._ssePrefix) and (self._isSse or self._hasSse):
            self.failedFlightCommands += 1
            errString = 'A flight command cannot start with the reserved SSE command prefix \'{}\''.format(self._ssePrefix)
            _log().error(errString)
            raise mtak.err.CommandFormatError(errString)

        uplinkString = ProxyCommand(prefix=self._commandPrefix, validate=validate, string_id=stringId, vcid=virtualChannel, scid=scid, wait_for_radiation=waitForRadiation, command=commandString)
        _log().debug(uplinkString)

        self._sendString(uplinkString)
        self.flightCommandsSent += 1
        _log().info('Transmission succeeded.')

    #An alias for sendFlightCommand
    sendFswCommand = sendFlightCommand

    #An alias for sendFlght Command
    sendHwCommand = sendFlightCommand

    #An alias for sendFlght Command
    sendSequenceDirective = sendFlightCommand

    #The interface for sending SSE commands
    def sendSseCommand(self, command):
        '''Send an SSE command to the SSE. If "command" is a string, it may be any ASCII text string that is recognized by the SSE (this module
        will not validate the input).  If the "command" string already begins with the defined "ssePrefix", then it will be sent as is.
        If the input string does not begin with the defined "ssePrefix", then the prefix will be appended before the command is sent.

        Args
        -----
        command - The SSE command to transmit (in ASCII text) or an SseCommand object.

        Returns
        --------
        None

        Raised Exceptions
        ------------------
        InvalidStateError - If the SSE host and port have not been set in the session config
        RadiationError - If the MPCS (chill_send_cmd) application could not radiate the command'''

        _log().debug('mtak.up.UplinkProxy.sendSseCommand()')

        commandString = str(command)

        #Strip off any leading or trailing quotes marks on the command
        if (commandString.startswith("\"") and commandString.endswith("\"")) or (commandString.startswith("'") and commandString.endswith("'")):
            commandString = commandString[1:-1]

        #Make sure that this string starts with the SSE prefix and fix it if it doesn't
        if not commandString.startswith(self._ssePrefix):
            commandString = self._ssePrefix + commandString

        _log().info('mtak.up.UplinkProxy.sendSseCommand(command="%s")' % (commandString))

        uplinkString = ProxyCommand(prefix=self._commandPrefix, wait_for_radiation='0', command=commandString)
        _log().debug(uplinkString)

        self._sendString(uplinkString)
        self.sseCommandsSent += 1
        _log().info('Transmission succeeded.')

    #The interface for uploading files
    def sendFile(self, source, target, type=None, overwrite=None, stringId=None, virtualChannel=None, scid=None, waitForRadiation=0):
        '''Send a file load to the flight software.

        Args
        -----
        fileLoad - A FileLoad object or a string representing the file load to send

        If fileLoad is a string, it should be formatted as follows:

        {file_type1,}input_filename1,targetfile_name1 ... {file_typeN,}input_filenameN,target_filenameN

        file_type - Should be "1" if the file is a sequence file or a "0" otherwise.  This value is
                    optional and will default to "0" if omitted.  Any value that can be represented
                    by 7 bits will be accepted as input.
        input_file_name - The name (including path) of the file to send.
        target_file_name - The target file name on the spacecraft file system where this
                           file will be placed.

        A file to send should be specified in a comma-separated triple of the form file_type,input_file_name,target_file_name or
        a comma-separated double of the form input_file_name,target_file_name. If a double is specified, the file type will
        default to "0" (see below). Multiple doubles and/or triples can be specified on the same command line and should be separated by whitespace.

        waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
        This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
        uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
        (optional) (positive int)

        Returns
        --------
        None

        Raised Exceptions
        ------------------
        InvalidStateError - If the FSW host and port have not been set in the session config
        RadiationError - If the MPCS (chill_send_file) application could not radiate the file'''

        type = type if type is not None else '0'
        overwrite = overwrite if overwrite is not None else ''
        stringId = stringId if stringId is not None else ''
        virtualChannel = virtualChannel if virtualChannel is not None else ''
        scid = scid if scid is not None else ''
        waitForRadiation = waitForRadiation if waitForRadiation is not None else 0

        _log().debug('mtak.up.UplinkProxy.sendFile()')
        _log().info('mtak.up.UplinkProxy.sendFile(source={},target={},type={},overwrite={},stringId={},virtualChannel={},scid={},waitForRadiation="{}")'.format(source, target, type, overwrite, stringId, virtualChannel, scid, waitForRadiation))

        uplinkString=ProxyFile(prefix=self._filePrefix, overwrite=overwrite, string_id=stringId, vcid=virtualChannel, scid=scid, source=source, target=target, type=type, wait_for_radiation=waitForRadiation)
        _log().debug(uplinkString)

        self._sendString(uplinkString)
        self.fileLoadsSent += 1
        _log().info('Transmission succeeded.')

    #The interface for sending an SCMF file
    def sendScmf(self, scmf, disableChecks=None, waitForRadiation=0):
        '''Send an SCMF file to the flight software.

        Args
        -----
        scmf - An Scmf object or a string that is the full path to the SCMF file
        waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
        This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
        uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
        (optional) (positive int)

        Returns
        --------
        None

        Raised Exceptions
        ------------------
        InvalidStateError - If the FSW host and port have not been set in the session config
        RadiationError - If the MPCS (chill_send_scmf) application could not radiate the SCMF contents'''

        scmfString = str(scmf)
        disableChecks = disableChecks if disableChecks is not None else ''
        waitForRadiation = waitForRadiation if waitForRadiation is not None else 0

        _log().debug('mtak.up.UplinkProxy.sendScmf()')
        _log().info('mtak.up.UplinkProxy.sendScmf(%s,%s,waitForRadiation="%d")' % (scmf, disableChecks, waitForRadiation))

        uplinkString=ProxyScmf(prefix=self._scmfPrefix, disable_checks=disableChecks, scmf_string=scmfString, wait_for_radiation=waitForRadiation)

        _log().debug(uplinkString)

        self._sendString(uplinkString)
        self.scmfsSent += 1
        _log().info('Transmission succeeded.')

    #The interface for sending a raw data file
    def sendRawDataFile(self, filename, hex=None, waitForRadiation=0):
        '''Send the contents of a filename to the flight software

        Args
        -----
        filename - A RawUplinkDatafilename object or a string that is the full path to the raw uplink data filename (string) (required)
        hex - True if the filename is an ASCII hex filename, false if it is raw binary (boolean) (optional, defaults to False)
        waitForRadiation - Number of seconds to wait for finalization of the uplink after the uplink data successfully leaves AMPCS.
        This is only applicable with uplinkConnectionType=COMMAND_SERVICE. Other uplink connection types will return when all the
        uplink data successfully leaves AMPCS. Defaults to 0 seconds, which means don't wait. Negative seconds will behave like 0 seconds.
        (optional) (positive int)

        Returns
        --------
        None

        Raised Exceptions
        ------------------
        InvalidStateError - If the FSW host and port have not been set in the session config
        RadiationError - If the MPCS (chill_send_scmf) application could not radiate the SCMF contents'''

        filenameString = str(filename)
        hex = hex if hex is not None else ''
        waitForRadiation = waitForRadiation if waitForRadiation is not None else 0

        _log().debug('mtak.up.UplinkProxy.sendRawDatafilename()')
        _log().info('mtak.up.UplinkProxy.sendRawDatafilename(%s,%s,waitForRadiation="%d")' % (filename, hex, waitForRadiation))

        uplinkString = ProxyRaw(prefix=self._rawPrefix, hex=hex, filename_string=filenameString, wait_for_radiation=waitForRadiation)
        _log().debug(uplinkString)

        try:

            self._sendString(uplinkString)
            self.rawDatasSent += 1
            _log().info('Transmission succeeded.')

        except (mtak.err.RadiationError, auto.err.AUTOError, auto.err.AmpcsError) as e:

            self.failedRawDatas += 1
            _log().error('Transmission failed.')
            raise

    def sendLog(self, message, level='INFO'):
        '''Send a log message to the MPCS database.'''

        #IMPORTANT: Don't call any log methods in here.  We could create an infinite loop because
        # this method is called by the database log handler attached to the logger

        uplinkString = ProxyLog(prefix=self._logPrefix, level=level, message=message)

        self._sendString(uplinkString, doLog=False)

    def _checkFswNetworkSettings(self):
        '''Ensure that all the proper information is available before a transmission is
        made to the FSW.

        Args
        -----
        None

        Returns
        --------
        None

        Raises
        -------
        InvalidStateError - If the FSW host and/or uplink port are not available.'''

        _log().debug('mtak.up.UplinkProxy._checkFswNetworkSettings()')

        if (not hasattr(self._sessionConfig, 'fswUplinkHost') or
           not self._sessionConfig.fswUplinkHost or self._sessionConfig.fswUplinkHost.isspace()):
            errString = 'Cannot send a flight command because the session config in the UplinkProxy has no defined FSW Host'
            _log().error(errString)
            raise mpcsutil.err.InvalidStateError(errString)
        elif (not hasattr(self._sessionConfig, 'fswUplinkPort') or self._sessionConfig.fswUplinkPort < 0):
            errString = 'Cannot send a flight command because the session config in the UplinkProxy an undefined FSW Uplink Port value'
            _log().error(errString)
            raise mpcsutil.err.InvalidStateError(errString)

    def _checkSseNetworkSettings(self):
        '''Ensure that all the proper information is available before a transmission is
        made to the SSE.

        Args
        -----
        None

        Returns
        --------
        None

        Raises
        -------
        InvalidStateError - If the SSE host and/or uplink port are not available or if the
        mission does not have an SSE.'''

        _log().debug('mtak.up.UplinkProxy._checkSseNetworkSettings()')

        if not self._isSse and not self._hasSse:
            errString = 'Cannot send an SSE command because this mission does not have an SSE'
            _log().critical(errString)
            raise mpcsutil.err.InvalidStateError(errString)

        if (not hasattr(self._sessionConfig, 'sseHost') or not self._sessionConfig.sseHost or self._sessionConfig.sseHost.isspace()):
            errString = 'Cannot send an SSE command because the session config in the UplinkProxy has no defined SSE Host'
            _log().error(errString)
            raise mpcsutil.err.InvalidStateError(errString)
        elif (not hasattr(self._sessionConfig, 'sseUplinkPort') or self._sessionConfig.sseUplinkPort < 0):
            errString = 'Cannot send an SSE command because the session config in the UplinkProxy an undefined SSE Uplink Port value'
            _log().error(errString)
            raise mpcsutil.err.InvalidStateError(errString)



class DummyUplinkProxy(UplinkProxy):
    '''Uplink proxy for use with misssions that have no uplink implementation.  We need to disable
    all uplink operations, but still allow log messages to go into the database.'''

    def __init__(self, sessionConfig):

        mtak.up.UplinkProxy.__init__(self, sessionConfig)

        _log().debug('mtak.up.DummyUplinkProxy()')
        _log().info('Created Uplink Proxy')
        _log().info('Using this session configuration: %s' % (sessionConfig))

    #The interface for sending hardware and FSW commands to the flight software
    def sendFlightCommand(self, command, validate=None, stringId=None, virtualChannel=None, scid=None, waitForRadiation=0):

        raise NotImplementedError('There is no existing command implementation for %s.' % (mpcsutil.config.GdsConfig().getMission()))

    #An alias for sendFlightCommand
    sendFswCommand = sendFlightCommand

    #An alias for sendFlght Command
    sendHwCommand = sendFlightCommand

    #An alias for sendFlght Command
    sendSequenceDirective = sendFlightCommand

    #The interface for sending SSE commands
    def sendSseCommand(self, command):

        raise NotImplementedError('There is no existing command implementation for %s.' % (mpcsutil.config.GdsConfig().getMission()))

    #The interface for uploading files
    def sendFile(self, source, target, type=None, overwrite=None, stringId=None, virtualChannel=None, scid=None,):

        raise NotImplementedError('There is no existing command implementation for %s.' % (mpcsutil.config.GdsConfig().getMission()))

    #The interface for sending an SCMF file
    def sendScmf(self, scmf, disableChecksums=None):

        raise NotImplementedError('There is no existing command implementation for %s.' % (mpcsutil.config.GdsConfig().getMission()))

    #The interface for sending a raw data file
    def sendRawDataFile(self, filename, hex=None):

        raise NotImplementedError('There is no existing command implementation for %s.' % (mpcsutil.config.GdsConfig().getMission()))

def test():
    import auto, mpcsutil, mtak
    import mpcsutil.test

    logger = _log()
    logger.setLevel(logging.getLevelName(logging.DEBUG))
    file_handler = logging.FileHandler('mtak.up.test.log')
    formatter    = logging.Formatter('%(asctime)s : %(levelname)s : %(name)s : %(message)s')
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)

    _log().disabled = False

    nextFswUplinkPort = 12345
    nextSseUplinkPort = 13345

    gdsConfig = mpcsutil.config.GdsConfig()
    mpcsutil.test.overrideDictionaries(gdsConfig)

    (key, host, conn) = mpcsutil.test.dbutil.insertSampleSession(
        fswUplinkHost='127.0.0.1',
        fswUplinkPort=nextFswUplinkPort,
        sseHost='127.0.0.1',
        sseUplinkPort=nextSseUplinkPort)
    config = mpcsutil.config.SessionConfig(key=key, host=host, validate_session=True)

    proxy = mtak.up.UplinkProxy(config)
    proxy.start()

    def _attemptBind(sock, host, port):
        exception_caught = True
        attempts = 0
        while exception_caught:
            try:
                sock.bind((host,port))
                print('mtak.up.test._attemptBind :: SUCCESS bound to socket ({}, {})'.format(host, port))
                exception_caught = False
            except Exception as e:
                exception_caught = True
                attempts += 1
                if attempts>10:
                    print('mtak.up.test._attemptBind :: FAILED bind to socket ({}, {})'.format(host, port))
                    break
                time.sleep(1)

    kill_event=threading.Event()
    def _ioLoop(serversocket, ke=None):
        ke = ke if ke else kill_event
        while not(ke.is_set()):
            serversocket.listen(1)
            (clientSock,address) = serversocket.accept()
            _buff = clientSock.recv(999999)
            data=_buff
            while _buff:
                _buff = clientSock.recv(99999)
                if _buff: data+=_buff
            if data: print('mtak.up.test._ioLoop :: data:\n\t{}'.format(data))

    @contextmanager
    def server_socket(ke=None, sse=False):
        ke = ke if ke else kill_event
        _host_key='sseHost' if sse else 'fswUplinkHost'
        _port_key='sseUplinkPort' if sse else 'fswUplinkPort'
        setattr(config, _host_key, getattr(config, _host_key, '127.0.0.1'))
        setattr(config, _port_key, getattr(config, _port_key, '12352'))
        _host = getattr(config, _host_key)
        _port = getattr(config, _port_key)

        _socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        _socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

        _attemptBind(_socket, _host, _port)

        _thread = threading.Thread(target=_ioLoop, name='{} Server Socket Thread'.format('SSE' if sse else 'FSW'),args=(_socket, ke))
        _thread.setDaemon(True)
        _thread.start()
        time.sleep(1)

        yield _socket

        ke.set()
        _socket.close()

    def _bad_fsw_cmd():
        print('\n{buff}\n# {title}\n{buff}\n'.format(buff='#'*80, title='Send Bad FSW Command'))
        def _fsw_as_sse():
            if gdsConfig.hasSse():
                print('\n\t## FSW Command with SSE Prefix\n')
                ssePrefix = gdsConfig.getProperty('Uplink.SSE.commandPrefix','sse:')
                try:
                    proxy.sendFlightCommand(ssePrefix + 'CMD_NO_OP')
                except mtak.err.CommandFormatError as e:
                    print('\n\t\tPASS :: Successfully caught Exception:\n\t\t\t{}\n'.format(e))
                else:
                    print('\n\t\tFAIL :: Did not detect SSE prefix on flight command\n')

        def _no_socket():
            print('\n\t## FSW Command with No Socket Connection\n')
            try:
                proxy.sendFlightCommand('CMD_NO_OP')
            except auto.err.ConnectionError as e:
                print('\n\t\tPASS :: Successfully caught Exception:\n\t\t\t{}'.format(e))
            else:
                print('\n\t\tFAIL :: Did not detect ConnectionError when no socket was present\n')

        def _no_fsw_up_host():
            print('\n\t## FSW Command with No FSW Uplink Host\n')
            fswUplinkHost_bkup = config.fswUplinkHost
            try:
                del config.fswUplinkHost
                proxy.sendFlightCommand('CMD_NO_OP')
            except auto.err.ConnectionError as e:
                print('\n\t\tPASS :: Successfully caught Exception:\n\t\t\t{}\n'.format(e))
            else:
                print('\n\t\tFAIL :: Did not detect ConnectionError when there was no FSW uplink host value\n')
            finally:
                config.fswHost = fswUplinkHost_bkup

        def _no_fsw_uplink_port():
            print('\n\t## FSW Command with No FSW Uplink Port\n')
            fswUplinkPort_bkup = config.fswUplinkPort
            try:
                del config.fswUplinkPort
                proxy.sendFlightCommand('CMD_NO_OP')
            except auto.err.ConnectionError as e:
                print('\n\t\tPASS :: Successfully caught Exception:\n\t\t\t{}'.format(e))
            else:
                print('\n\t\tFAIL :: Did not detect ConnectionError when there was no FSW uplink port value\n')
            finally:
                config.fswUplinkPort = fswUplinkPort_bkup

        _fsw_as_sse()
        _no_socket()
        _no_fsw_up_host()
        _no_fsw_uplink_port()

    def _good_fsw_cmd():
        print('\n{buff}\n# {title}\n{buff}\n'.format(buff='#'*80, title='Send Good FSW Command'))
        def _fsw_cmd():
            print('\n\t## FSW Command\n')
            try: proxy.sendFlightCommand('CMD_NO_OP')
            except Exception as e: print('\n\t\tFAIL :: Encountered an unexpected exception sending a flight command:\n\t\t\t{}'.format(e))
            else: print('\n\t\tPASS :: Successfully sent FSW command\n')

        def _fsw_cmd_str():
            print('\n\t## FSW Command String\n')
            try: proxy.sendFlightCommand(mpcsutil.command.FlightSoftwareCommand(cmdString='CMD_NO_OP'))
            except Exception as e: print('\n\t\tFAIL :: Encountered an unexpected exception sending a flight command:\n\t\t\t{}'.format(e))
            else: print('\n\t\tPASS :: Successfully sent FSW command\n')

        def _fsw_cmd_valid():
            print('\n\t## Unvalidated FSW Command String\n')
            try: proxy.sendFlightCommand('CMD_NO_OP',validate=False)
            except Exception as e: print('\n\t\tFAIL :: Encountered an unexpected exception sending a flight command:\n\t\t\t{}'.format(e))
            else: print('\n\t\tPASS :: Successfully sent FSW command\n')

        _fsw_cmd()
        _fsw_cmd_str()
        _fsw_cmd_valid()

    def _good_send_file():
        print('\n{buff}\n# {title}\n{buff}\n'.format(buff='#'*80, title='Send Good File'))
        configFilePath = os.path.join(os.path.dirname(__file__), 'test', 'TestConfig3.xml')
        def _send_file():
            print('\n\t## Send File\n')
            try: proxy.sendFile(source=configFilePath,target='/tmp/conf',type='0')
            except Exception as e: print('\n\t\tFAIL :: Encountered an unexpected exception sending a file load:\n\t\t\t{}'.format(e))
            else: print('\n\t\tPASS :: Successfully sent file\n')

        def _send_inv_file():
            print('\n\t## Send Invalid (Does Not Exist) File\n')
            try: proxy.sendFile(source='/some/non/existent/file',target='/tmp/conf',type='0')
            except Exception as e: print('\n\t\tPASS :: Exception caught while trying to send invalid file:\n\t\t\t{}\n'.format(e))
            else: print('\n\t\tFAIL :: Did not detect non-existent input file\n')

        _send_file()
        _send_file()
        _send_inv_file()

    def _good_sse_cmd():
        print('\n{buff}\n# {title}\n{buff}\n'.format(buff='#'*80, title='Send Good SSE Command'))

        if not(gdsConfig.hasSse()): return
        ssePrefix = gdsConfig.getProperty('Uplink.SSE.commandPrefix','sse:')

        cmd = 'some SSE command'

        def _prefix():
            print('\n\t## Prefixed SSE Command\n')
            _cmd='{}{}'.format(ssePrefix, cmd)
            try: proxy.sendSseCommand(_cmd)
            except Exception as e: print('\n\t\tFAIL :: Encountered an unexpected exception sending an SSE command:\n\t\t\t{}\n'.format(e))
            else: print('\n\t\tPASS :: Successfully radiated SSE Command:\n\t\t\t{}\n'.format(_cmd))

        def _no_prefix():
            print('\n\t## Unprefixed SSE Command\n')
            _cmd=cmd
            try: proxy.sendSseCommand(_cmd)
            except Exception as e: print('\n\t\tFAIL :: Encountered an unexpected exception sending an SSE command:\n\t\t\t{}\n'.format(e))
            else: print('\n\t\tPASS :: Successfully radiated SSE Command:\n\t\t\t{}\n'.format(_cmd))

        def _reprd():
            print('\n\t## Reprd SSE Command\n')
            _cmd='\'{}\''.format(cmd)
            try: proxy.sendSseCommand(_cmd)
            except Exception as e: print('\n\t\tFAIL :: Encountered an unexpected exception sending an SSE command:\n\t\t\t{}\n'.format(e))
            else: print('\n\t\tPASS :: Successfully radiated SSE Command:\n\t\t\t{}\n'.format(_cmd))

        def _quoted():
            print('\n\t## Quoted SSE Command\n')
            _cmd='"{}"'.format(cmd)
            try: proxy.sendSseCommand(_cmd)
            except Exception as e: print('\n\t\tFAIL :: Encountered an unexpected exception sending an SSE command:\n\t\t\t{}\n'.format(e))
            else: print('\n\t\tPASS :: Successfully radiated SSE Command:\n\t\t\t{}\n'.format(_cmd))

        def _mpcsutil_cmd():
            print('\n\t## MpcsUtil SSE Command\n')
            _cmd = mpcsutil.command.SseCommand(cmdString='{}{}'.format(ssePrefix, cmd))
            try: proxy.sendSseCommand(_cmd)
            except Exception as e: print('\n\t\tFAIL :: Encountered an unexpected exception sending an SSE command:\n\t\t\t{}\n'.format(e))
            else: print('\n\t\tPASS :: Successfully radiated SSE Command:\n\t\t\t{}\n'.format(_cmd))

        _prefix()
        _no_prefix()
        _reprd()
        _quoted()
        _mpcsutil_cmd()


    def _good_scmf():
        print('\n{buff}\n# {title}\n{buff}\n'.format(buff='#'*80, title='Send Good SCMF'))
        scmfPath = os.path.join(os.path.dirname(__file__), 'test', 'good.scmf')

        def _scmf_path():
            print('\n\t## Nominal SCMF')
            try: proxy.sendScmf(scmfPath)
            except Exception as e: print('\n\t\tFAIL :: Encountered an unexpected exception sending an SCMF:\n\t\t\t{}'.format(e))
            else: print('\n\t\tPASS :: Successfully sent SCMF')

        def _mpcsutil_scmf():
            print('\n\t## Mpcsutil SCMF')
            try: proxy.sendScmf(mpcsutil.command.Scmf(filename=scmfPath))
            except: print('\n\t\tFAIL :: Encountered an unexpected exception sending an SCMF:\n\t\t\t{}'.format(e))
            else: print('\n\t\tPASS :: Successfully sent SCMF')

        def _bad_csum():
            print('\n\t## Bad Checksum')
            try: proxy.sendScmf(scmfPath, disableChecks=False , waitForRadiation=30)
            except: print('\n\t\tPASS :: Successfully caught bad checksum')
            else: print('\n\t\tFAIL :: Did not detect input file that fails checksum test')

        def _invalid_file():
            print('\n\t## Nonexistent File')
            try: proxy.sendScmf('/some/non/existent/file.scmf')
            except: print('\n\t\tPASS :: Successfully caught invalid file')
            else: print('\n\t\tFAIL :: Did not detect non-existent input file')

        _scmf_path()
        _mpcsutil_scmf()
        # _bad_csum()
        _invalid_file()


    _bad_fsw_cmd()
    # with server_socket(sse=True) as _socket: _good_sse_cmd()

    with server_socket() as _socket:
        _good_scmf()
    #     _good_send_file()
    #     _good_fsw_cmd()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
