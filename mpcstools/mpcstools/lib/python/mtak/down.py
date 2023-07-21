#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module encapsulates the MTAK interaction with the MPCS downlink utilities.  This
module contains the downlink proxy which is responsible for receiving and storing incoming telemetry
(EVRs, channel values, and Products).  In addition, this module allows the user to register conditions to wait on
the receipt of particular pieces of telemetry (or a combination thereof).
"""

from __future__ import (absolute_import, division, print_function)


import collections
import errno
import logging
import mpcsutil
import mtak
import os.path
import signal
import select
import socket
import subprocess
import sys
import threading
import time
import six
long = long if six.PY2 else int
_log = lambda : logging.getLogger('mpcs.mtak')

class DownlinkProxy(mtak.AbstractProxy):
    '''The downlink proxy is essentially the MTAK interface to the JMS message bus.  It pulls messages off the
    message through a command line executed version of chill_monitor and receives all the messages from the
    stdout stream of the monitor.  The downlink proxy has a daemon thread whose sole purpose is to interface with
    the monitor instance to receive messages.

    The downlink proxy has local lists/tables to keep track of all pertinent JMS messages received and also keeps a
    running total of the number of each type of message received.

    The downlink proxy also allows users to register 'wait conditions' on incoming messages so that they may be synchronously
    informed of when a particular piece of telemetry arrives (see the AbstractWait, CompoundWait, EvrWait, ProductWait,
    ChanValWait, and CfdpIndicationWait for more information).

    Attributes
    -----------
    evrCount - The number of EVR messages that have been received since the "start()" method was called (long)
    productCount - The number of product messages that have been received since the "start()" method was called (long)
    chanValCount - The number of EHA channel messages that have been received since the "start()" method was called (long)
    cfdpIndicationCount - The number of CFDP Indication messages that have been received since the "start()" method was called (long)
    evrList - The list of the last N received EVRs (list size is configurable in the GDS configuration) (mtak.evr.EvrList)
    productTable - The table of received products keyed by the transaction ID (mtak.prod.ProductTable)
    channelValueTable - The LAD table of received channel values keyed by channel ID (mtak.chan.ChannelValueTable)
    cfdpIndicationTable - The table of received CFDP Indications keyed by the sourceEntityId:transactionSequenceNumber string (mtak.cfdp.CfdpIndicationTable)
    _sessionConfig = The session config object used by the proxy to supply information to the MPCS
                     monitor application (e.g. test host, test user, etc.) (mtak.config.SessionConfig)
    _waitList - The list of wait conditions that have been registered (collections.deque)
    _running - True if the proxy is receiving telemetry, False otherwise (Boolean)
    _ioThread - The thread connected to the stdout and stderr streams of chill_monitor (threading.Thread)
    _monitorBufferSize - The buffer size of the output streams of chill_monitor (int)
    _monitorApp - The name of the chill_monitor app (string)
    _typesArg - The types of messages for chill_monitor to listen for (string)
    _noGuiArg = The argument to suppress the chill monitor GUI (string)
    _outputFormatArg = The argument to specify the chill monitor output format (string)
    _fswDictDirArg = The argument to specify the chill monitor FSW dictionary directory (string)
    _fswVersionArg = The argument to specify the chill monitor FSW version (string)
    _sseDictDirArg = The argument to specify the chill monitor SSE dictionary directory (string)
    _sseVersionArg = The argument to specify the chill monitor SSE version (string)
    _downlinkStreamArg = The argument to specify the chill monitor downlink stream ID
    _hostArg = The argument to specify the chill monitor host
    _userArg = The argument to specify the chill monitor user
    _evrListSize = The max number of EVRs stored at once in the EVR list
    _lookbackListSize = The max number of messages stored at once in the lookback list'''

    def __init__(self,sessionConfig):
        ''' Initialize the downlink proxy.  Requires a valid session config.

        Args
        -----
        sessionConfig - The session config specifying the session that this component will listen to

        Returns
        --------
        None

        Raises
        ------------------
        InvalidInitError - If the input session config does not have a proper session key set on it'''

        mtak.AbstractProxy.__init__(self,sessionConfig)

        self._running = False
        self._monitorProcess = None

        #Product-related objects
        self.productTable = mtak.collection.ProductTable()
        self.productCount = long(0)

        #EHA-related objects
        self.channelValueTable = mtak.collection.ChannelValueTable()
        self.recordedChannelValueTable = mtak.collection.ChannelValueTable()
        self.chanValCount = long(0)
        self.recordedChanValCount = long(0)

        #EVR-related objects
        self.evrTable = mtak.collection.EvrTable()
        self.recordedEvrTable = mtak.collection.EvrTable()
        self.evrCount = long(0)
        self.recordedEvrCount = long(0)

        #CFDP Indication-related objects
        self.cfdpIndicationTable = mtak.collection.CfdpIndicationTable()
        self.cfdpIndicationCount = long(0)

        #create the wait list
        self._waitList = collections.deque()
        self._waitListLock = threading.Lock()

        #Store the most recent exact time received for each time of telemetry
        self.currentSclkExact = long(0)
        self.currentScetExact = long(0)
        self.currentScetNano = int(0)
        self.currentErtExact = long(0)

        self.server_socket = None
        self.client_socket = None

    def flush_eha(self):
        '''Empty out all the EHA-related data structures.'''

        self.channelValueTable.lock.acquire()
        self.channelValueTable.clear()
        self.channelValueTable.lock.release()

        self.recordedChannelValueTable.lock.acquire()
        self.recordedChannelValueTable.clear()
        self.recordedChannelValueTable.lock.release()

    def flush_evr(self):
        '''Empty out all the EVR-related data structures.'''

        self.evrTable.lock.acquire()
        self.evrTable.clear()
        self.evrTable.lock.release()

        self.recordedEvrTable.lock.acquire()
        self.recordedEvrTable.clear()
        self.recordedEvrTable.lock.release()

    def flush_dp(self):
        '''Empty out all the product-related data structures.'''

        self.productTable.lock.acquire()
        self.productTable.clear()
        self.productTable.lock.release()

    def flush_cfdp_ind(self):
        '''Empty out all the CFDP Indication-related data structures.'''

        self.cfdpIndicationTable.lock.acquire()
        self.cfdpIndicationTable.clear()
        self.cfdpIndicationTable.lock.release()

    def flush_all(self):
        '''Empty out all the telemetry-related data structures.'''

        self.flush_eha()
        self.flush_evr()
        self.flush_dp()
        self.flush_cfdp_ind()

    def isRunning(self):
        '''Check to see if the downlink proxy is currently running (has a JMS connection)

        Args
        -----
        None

        Returns
        --------
        A Boolean value of True if the proxy is running, False otherwise'''

        _log().debug('mtak.down.DownlinkProxy.isRunning()')

        return self._running

    def _readConfig(self):
        '''Read all the values from the GDS configuration that this object uses internally.  Calling this function
        will cause all configuration attributes on this object to be written or overwritten with the values stored in the
        GdsConfig singleton.

        Args
        -----
        None

        Returns
        --------
        None'''

        _log().debug('mtak.down.DownlinkProxy._readConfig()')

        gdsConfig = mpcsutil.config.GdsConfig()
        # automationApp.internal.mtak.args
        self._monitorBufferSize = int(gdsConfig.getProperty('automationApp.mtak.monitorBufferSize',0))
        self._monitorApp = gdsConfig.getProperty('automationApp.internal.mtak.app.downServer','internal/chill_mtak_downlink_server')
        self._sessionKeyArg = gdsConfig.getProperty('automationApp.internal.mtak.args.sessionKey','--testKey')
        self._databaseHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databaseHost','--databaseHost')
        self._databasePortArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databasePort','--databasePort')
        self._databaseUserArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databaseUser','--dbUser')
        self._databasePasswordArg = gdsConfig.getProperty('automationApp.internal.mtak.args.databasePassword','--databasePassword')
        self._jmsHostArg = gdsConfig.getProperty('automationApp.internal.mtak.args.jmsHost','-j')
        self._jmsPortArg = gdsConfig.getProperty('automationApp.internal.mtak.args.jmsPort','-n')
        self._ignoreEhaArg = gdsConfig.getProperty('automationApp.internal.mtak.args.ignoreEha','--ignoreEha')
        self._ignoreEvrsArg = gdsConfig.getProperty('automationApp.internal.mtak.args.ignoreEvrs','--ignoreEvrs')
        self._ignoreProductsArg = gdsConfig.getProperty('automationApp.internal.mtak.args.ignoreProducts','--ignoreProducts')
        self._ignoreFswArg = gdsConfig.getProperty('automationApp.internal.mtak.args.ignoreFsw','--ignoreFsw')
        self._ignoreSseArg = gdsConfig.getProperty('automationApp.internal.mtak.args.ignoreSse','--ignoreSse')
        self._ignoreCfdpIndicationsArg = gdsConfig.getProperty('automationApp.internal.mtak.args.ignoreCfdpIndications','--ignoreCfdpIndications')
        self._mtakDownlinkPortArg = gdsConfig.getProperty('automationApp.internal.mtak.args.mtakDownlinkPort','--mtakDownlinkPort')
        self._fetchLadArg = gdsConfig.getProperty('automationApp.internal.mtak.args.fetchLad','--fetchLad')
        self._pollingTimeout = int(gdsConfig.getProperty('automationApp.mtak.pollingTimeoutMillis',500))
        self._channelValueListSize = int(gdsConfig.getProperty('automationApp.mtak.listSize.channel',25))
        self._evrListSize = int(gdsConfig.getProperty('automationApp.mtak.listSize.evr',100))
        self._productListSize = int(gdsConfig.getProperty('automationApp.mtak.listSize.product',100))
        self._cfdpIndicationListSize = int(gdsConfig.getProperty('automationApp.mtak.listSize.cfdpIndication',5))
        self._downlinkPort = int(gdsConfig.getProperty('automationApp.mtak.ports.down',60001))
        self._heartbeat = long(gdsConfig.getProperty('general.context.heartbeatInterval',0))
        self._channelIdsArg = gdsConfig.getProperty('automationApp.internal.mtak.args.channelIds','--channelIds')
        self._modulesArg = gdsConfig.getProperty('automationApp.internal.mtak.args.modules','--modules')
        self._subsystemsArg = gdsConfig.getProperty('automationApp.internal.mtak.args.subsystems','--subsystems')
        self._opsCategoriesArg = gdsConfig.getProperty('automationApp.internal.mtak.args.opsCategories','--opsCategories')

        self._isSse = gdsConfig.isSse()
        self._hasSse = gdsConfig.hasSse()
        self._mission = gdsConfig.getMission()

    def getSummary(self):
        '''Generate a summary of all downlink activity as a string that can be written
        to the log file.

        Args
        -----
        None

        Returns
        --------
        A string containing the summary of all downlink activity.'''

        _log().debug('mtak.down.DownlinkProxy.getSummary()')

        self.channelValueTable.lock.acquire()
        unique_rt_chan_count = len(self.channelValueTable)
        self.channelValueTable.lock.release()

        self.recordedChannelValueTable.lock.acquire()
        unique_rc_chan_count = len(self.recordedChannelValueTable)
        self.recordedChannelValueTable.lock.release()

        self.evrTable.lock.acquire()
        evr_rt_summary = mtak.collection.getSummaryString(self.evrTable.getLevelSummary())
        self.evrTable.lock.release()

        self.recordedEvrTable.lock.acquire()
        evr_rc_summary = mtak.collection.getSummaryString(self.recordedEvrTable.getLevelSummary())
        self.recordedEvrTable.lock.release()

        self.productTable.lock.acquire()
        product_summary = mtak.collection.getSummaryString(self.productTable.getApidSummary(),'APID ')
        self.productTable.lock.release()

        summary = str(
'''
----------------------------
EHA Telemetry
----------------------------

Realtime
=========
Total Channel Values Received = %d
Total Unique Channels Updated = %d

Recorded
=========
Total Channel Values Received = %d
Total Unique Channels Updated = %d

----------------------------
EVR Telemetry
----------------------------

Realtime
=========
Total EVRs Received = %d
Level Summary

%s

Recorded
=========
Total EVRs Received = %d
Level Summary

%s

----------------------------
Product Telemetry
----------------------------
Total Product Messages Received = %d

APID Summary

%s
''' % (self.chanValCount,unique_rt_chan_count,
       self.recordedChanValCount,unique_rc_chan_count,
       self.evrCount,evr_rt_summary,
       self.recordedEvrCount,evr_rc_summary,
       self.productCount,product_summary))

        return summary

    def _setup_server_socket(self):

        #Find an open port to use for receiving telemetry from the downlink server app
        self.server_socket = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
        found_socket = False
        while not found_socket:
            try:
                #self.server_socket.bind((socket.gethostname(),self._downlinkPort))
                self.server_socket.bind(('localhost',self._downlinkPort))
                found_socket = True
            except socket.error:
                self._downlinkPort = (self._downlinkPort + 1) % mpcsutil.MAX_PORT_NUMBER
        self.server_socket.listen(1)

        self.server_socket.settimeout(30) #Setting this to 30 seconds
#        timeout =  self._heartbeat*2
#        if timeout > 0:
#            self.server_socket.settimeout(timeout/1000) #Make sure we make this milliseconds instead of real seconds

    def _destroy_sockets(self):

        try:
            if self.client_socket is not None:
                    self.client_socket.close()

            if self.server_socket is not None:
                self.server_socket.close()
        except socket.error:

            pass

    def start(self):
        '''Start the downlink proxy running.  This method will fork off a chill_monitor Java process to connect to the JMS
        bus and the proxy will use the stdout/stderr streams of the monitor to receive telemetry.

        Calling start on this object when it has already been started will have no effect.

        NOTE: Calling this method will reset all message counts on the object.

        Args
        -----
        None

        Returns
        --------
        None

        Raises
        ------------------
        EnvironmentError - If the chill_monitor application cannot be found
        InvalidStateError - If any of the required session information (dict dirs, versions, user & host) are not set in the session config'''

        _log().debug('mtak.down.DownlinkProxy.start()')

        #We're already running...do nothing
        if self._running == True:
            _log().warning('Downlink Proxy has already been started.  Make sure you did not call "start()" twice.')
            return

        #Reset message counts
        self.evrCount = long(0)
        self.productCount = long(0)
        self.chanValCount = long(0)
        self.cfdpIndicationCount = long(0)

        #Reset time fields
        self.currentSclkExact = long(0)
        self.currentScetExact = long(0)
        self.currentScetNano = int(0)
        self.currentErtExact = long(0)

        self._setup_server_socket()

        #Fire up another thread that will read telemetry received with the monitor process
        processString = None
        try:
            processString = self._createMonitorProcessString()
        except (mpcsutil.err.InvalidStateError, mpcsutil.err.EnvironmentError):
            dummy_exc_class, exc, traceback = sys.exc_info()
            my_exc = mtak.err.DownlinkError('Error initializing downlink: %s' % (exc))
            raise my_exc.__class__(my_exc, traceback)

        _log().debug("Running process %s" % processString)
        self._ioThread = threading.Thread(target=self._ioLoop,name='Downlink Server Thread',args=())
        self._ioThread.setDaemon(True)
        self._running = True

        #Fork off the Java process to connect to the JMS bus and grab a handle to its stderr stream
        try:
            self._monitorProcess = subprocess.Popen(processString,
                                          bufsize=self._monitorBufferSize,
                                          shell=True,
                                          stdout=sys.stdout,
                                          stdin=None,
                                          stderr=subprocess.PIPE,
                                          env=os.environ)
        except OSError:

            _log().critical('Could not start the background downlink server app: %s' % (str(sys.exc_info())))
            dummy_exc_class, exc, traceback = sys.exc_info()
            my_exc = mtak.err.DownlinkError('Error initializing downlink: %s' % (exc))

            self._destroy_sockets()

            raise my_exc.__class__(my_exc, traceback)
        _log().debug("Started mtak downlink ...")
        self._ioThread.start()


    def stop(self):
        '''Stop the downlink proxy from running.  This will kill the forked off Java process.

        Calling this method if the downlink proxy has already been stopped or has not been started will have
        no effect.

        Args
        -----
        None

        Returns
        --------
        None'''

        if self._running == False:
            _log().warning('Downlink Proxy is already stopped.  Make sure you did not call "stop()" twice.')
            return

        #Wait for the background thread to timeout and die
        self._running = False
        _log().info('Waiting for Downlink Proxy background downlink server thread to terminate...')

        #if self._ioThread.isAlive()
        self._ioThread.join(self._pollingTimeout / 1000.0 * 4)

        if self._ioThread is not None:
            if self._ioThread.is_alive():

                if self._monitorProcess:
                    _log().warning('''Background downlink server thread did not die.  You may have a zombie Java process
                with PID = %s still running in the background.''' % self._monitorProcess.pid)

        else:
            self._ioThread = None

        _log().info('MTAK Downlink Proxy shutdown signal has been sent (telemetry has stopped flowing)...')

        mtak.AbstractProxy._destroyCoreLogHandlers()

    def _createMonitorProcessString(self):
        '''Create the command line string that will be used to invoke the background chill_monitor
        process which this downlink proxy will use to receive telemetry.

        Args
        -----
        None

        Returns
        --------
        A string that can be used to launch a chill_monitor process in the background. (string)'''

        #Check that the downlink server app exists
        processString = '%s/%s' % (mpcsutil.chillBinDirectory,self._monitorApp)
        if not os.path.isfile(processString):
            errMsg = 'Cannot find the required script ' + processString
            _log().critical(errMsg)
            raise mpcsutil.err.EnvironmentError(errMsg)

        #
        # Construct the process string by checking that all the required arguments exist
        #
#        if not hasattr(self._sessionConfig,'venueType') or not self._sessionConfig.venueType or self._sessionConfig.venueType.isspace():
#            raise mpcsutil.err.InvalidStateError('Cannot start the downlink proxy because the venue type is not set on the session config')
#        processString += (' %s %s ' % (self._venueTypeArg,self._sessionConfig.venueType))
#
#        if not hasattr(self._sessionConfig,'user') or not self._sessionConfig.user or self._sessionConfig.user.isspace():
#            raise mpcsutil.err.InvalidStateError('Cannot start the downlink proxy because the user is not set on the session config')
#        processString += (' %s %s ' % (self._userArg,self._sessionConfig.user))
#
#        if not hasattr(self._sessionConfig,'host') or not self._sessionConfig.host or self._sessionConfig.host.isspace():
#            raise mpcsutil.err.InvalidStateError('Cannot start the downlink proxy because the host is not set on the session config')
#        processString += (' %s %s ' % (self._hostArg,self._sessionConfig.host))
#
#        if self._sessionConfig.testbedName and not self._sessionConfig.testbedName.isspace() and self._sessionConfig.testbedName != 'UNKNOWN':
#            processString += (' %s "%s" ' % (self._testbedNameArg,self._sessionConfig.testbedName))
#
#        if self._sessionConfig.downlinkStreamId and not self._sessionConfig.downlinkStreamId.isspace() and self._sessionConfig.downlinkStreamId != 'UNKNOWN':
#            processString += (' %s "%s" ' % (self._downlinkStreamArg,self._sessionConfig.downlinkStreamId))
#
#        if not hasattr(self._sessionConfig,'fswVersion') or not self._sessionConfig.fswVersion or self._sessionConfig.fswVersion.isspace():
#            raise mpcsutil.err.InvalidStateError('Cannot start the downlink proxy because the FSW version is not set on the session config')
#        processString += (' %s %s ' % (self._fswVersionArg,self._sessionConfig.fswVersion))
#
#        if not hasattr(self._sessionConfig,'fswDictDir') or not self._sessionConfig.fswDictDir or self._sessionConfig.fswDictDir.isspace():
#            raise mpcsutil.err.InvalidStateError('Cannot start the downlink proxy because the FSW dictionary directory is not set on the session config')
#        processString += (' %s %s ' % (self._fswDictDirArg,self._sessionConfig.fswDictDir))
#
#        if (not hasattr(self._sessionConfig,'sseVersion') or not self._sessionConfig.sseVersion or self._sessionConfig.sseVersion.isspace()) and \
#           (mpcsutil.config.GdsConfig.isSse() or mpcsutil.config.GdsConfig.hasSse()):
#                raise mpcsutil.err.InvalidStateError('Cannot start the downlink proxy because the SSE version is not set on the session config')
#        processString += (' %s %s ' % (self._sseVersionArg,self._sessionConfig.sseVersion))
#
#        if (not hasattr(self._sessionConfig,'sseDictDir') or not self._sessionConfig.sseDictDir or self._sessionConfig.sseDictDir.isspace()) and \
#           (mpcsutil.config.GdsConfig.isSse() or mpcsutil.config.GdsConfig.hasSse()):
#            raise mpcsutil.err.InvalidStateError('Cannot start the downlink proxy because the SSE dictionary directory is not set on the session config')
#        processString += (' %s %s ' % (self._sseDictDirArg,self._sessionConfig.sseDictDir))

        if not hasattr(self._sessionConfig,'key') or not str(self._sessionConfig.key).isdigit():
            raise mpcsutil.err.InvalidStateError('Cannot start the downlink proxy because the session key "%s" is invalid.' % (self._sessionConfig.key))
        processString += (' %s %s ' % (self._sessionKeyArg,self._sessionConfig.key))

        db_host = mpcsutil.database.getDatabaseHost()
        if db_host is not None and str(db_host).strip():
            processString += ' %s %s' % (self._databaseHostArg,db_host)

        db_port = mpcsutil.database.getDatabasePort()
        if db_port is not None and str(db_port).strip():
            processString += ' %s %s' % (self._databasePortArg,db_port)

        db_user = mpcsutil.database.getDatabaseUserName()
        if db_user is not None and str(db_user).strip():
            processString += ' %s %s' % (self._databaseUserArg,db_user)

        db_pwd = mpcsutil.database.getDatabasePassword()
        if db_pwd is not None and str(db_pwd).strip():
            processString += ' %s %s' % (self._databasePasswordArg,db_pwd)

        jmsHost = mpcsutil.config.getJmsHost()
        if jmsHost is not None:
            processString += (' %s %s ' % (self._jmsHostArg,jmsHost))

        jmsPort = mpcsutil.config.getJmsPort()
        if jmsPort is not None:
            processString += (' %s %s ' % (self._jmsPortArg,jmsPort))

        if not mtak.getReceiveEha():
            processString += (' %s ' % (self._ignoreEhaArg))
        if not mtak.getReceiveEvrs():
            processString += (' %s ' % (self._ignoreEvrsArg))
        if not mtak.getReceiveProducts():
            processString += (' %s ' % (self._ignoreProductsArg))
        if not mtak.getReceiveFsw():
            processString += (' %s ' % (self._ignoreFswArg))
        if not mtak.getReceiveSse():
            processString += (' %s ' % (self._ignoreSseArg))
        if not mtak.getReceiveCfdpIndications():
            processString += (' %s ' % (self._ignoreCfdpIndicationsArg))
        if mtak.getFetchLad():
            processString += (' %s ' % (self._fetchLadArg))

        channelIds = mtak.getReceiveChannelIds()
        if channelIds is not None:
            processString += (' %s %s ' % (self._channelIdsArg,channelIds))
        modules = mtak.getReceiveModules()
        if modules is not None:
            processString += (' %s %s ' % (self._modulesArg,modules))
        subsystems = mtak.getReceiveSubsystems()
        if subsystems is not None:
            processString += (' %s %s ' % (self._subsystemsArg,subsystems))
        opsCategories = mtak.getReceiveOpsCategories()
        if opsCategories is not None:
            processString += (' %s %s ' % (self._opsCategoriesArg,opsCategories))

        processString += (' %s %s ' % (self._mtakDownlinkPortArg,self._downlinkPort))

        return processString

    def _ioLoop(self,*args):
        '''The IO loop is the internal method used by the downlink proxy daemon thread that reads
        incoming data from the chill monitor Java process.  The daemon thread will start running through
        this method when the 'start()' method is called.  The thread will continue to run in this
        function until the 'stop()' method is called'.

        Args
        -----
        None

        Returns
        --------
        None'''

        try:
            while self._running:

                try:
                    (self.client_socket,dummy_address) = self.server_socket.accept()

                except socket.timeout:
                    message = 'Background MTAK telemetry thread could not connect to MTAK downlink server at "localhost:%d". Connection timed out.' % (self._downlinkPort)
                    _log().critical(message)
                    #mtak.log_stack_trace()
                    self._running = False
                    break

                #Look at the error output from the background downlink server in case it dies or has an issue
                server_stderr = self._monitorProcess.stderr
                stderrFileNo = server_stderr.fileno()

                #Get a file-like representation of the socket where the telemetry strings are coming in
                client_socket_file = self.client_socket.makefile()
                client_socket_fileno = client_socket_file.fileno()

                #Set up some pollers so we're not doing busy waiting looking for messages
                poller = select.poll()
                poller.register(client_socket_file,select.POLLIN|select.POLLPRI|select.POLLERR|select.POLLHUP)
                poller.register(server_stderr,select.POLLIN|select.POLLPRI|select.POLLERR|select.POLLHUP)

                #Run until someone stops the downlink proxy
                while self._running:

                    try:
                        #Poll for new messages
                        events = poller.poll(self._pollingTimeout)
                        for event in events:

                            #server must've died or been killed
                            if (event[1] & select.POLLHUP):

                                _log().critical('MTAK background downlink server died or was killed.  Cannot process any more telemetry.')
                                self._running = False
                                break

                            #If the event came on stdout, it's a new message for us to read
                            #(Messages are newline delimited so we use readline() and the [:-1] strips the newline character)
                            elif event[0] == client_socket_fileno:

                                message = client_socket_file.readline()[:-1]
                                message = message.decode('utf-8') if isinstance(message, bytes) else message
                                #for message in client_socket_file:
                                try:
                                    self._parseMessage(message)
                                except Exception as e:
                                    _log().error('Error processing the following telemetry item in MTAK: %s\n\n%s\n%s'
                                                 % (message, sys.exc_info(), e))
                                    mtak.log_stack_trace()
                                    #If the event came on stderr, it's an error message
                            #(goes to the console for now)
                            elif event[0] == stderrFileNo:

                                errline = server_stderr.readline().strip()
                                errline = errline.decode('utf-8') if isinstance(errline, bytes) else errline

                                if errline:
                                        if errline.startswith('ERROR'):
                                            _log().error('Background error detected while receiving telemetry: %s' % (errline))
                                        elif errline.startswith('CRITICAL') or errline.startswith('FATAL'):
                                            _log().critical('Background critical error detected while receiving telemetry: %s' % (errline))
                    except select.error as v:

                        if v[0] == errno.EINTR:
                            continue

                        _log().error('Background downlink server thread encountered an error: %s. Attempting to continue processing...' % (str(sys.exc_info())))
                        mtak.log_stack_trace()

                    except Exception as exc:

                        if hasattr(exc,'errno'):
                            if exc.errno == errno.EINTR:
                                continue

                        _log().error('Background downlink server thread encountered an error: %s. Attempting to continue processing...' % (str(sys.exc_info())))
                        mtak.log_stack_trace()
            try:
                os.kill(self._monitorProcess.pid,signal.SIGTERM)
            except OSError:
                _log().error('There was an error while attempting to kill the background downlink server process: %s' % (str(sys.exc_info())))
                mtak.log_stack_trace()

        finally:
            self._destroy_sockets()

    def _parseObject(self,obj):
        '''Parse the passed Evr, Product, Channel Value, or CFDP Indication object.  This method is currently only
        called by _parseMessage() and mtak.wrapper.inject**() methods.

        This method is responsible for parsing the object, adding it to the appropriate history lists, incrementing the
        local message counts, and checking the message against any existing waiting conditions.

        Args
        -----
        obj - The object that should be parsed (mpcsutil.channel.ChanVal,mpcsutil.evr.Evr,mpcsutil.product.Product,mpcsutil.cfdp.CfdpIndication)

        Returns
        --------
        None'''

        receiveTime = time.time()
        is_realtime = False


        # obj is a ChanVal
        if isinstance(obj,mpcsutil.channel.ChanVal):

            is_realtime = getattr(obj,'realtime',False)

            if is_realtime:

                self.chanValCount+=1
                self.channelValueTable.lock.acquire()
                try:
#                    oldObj = self.channelValueTable.getById(obj.channelId)
#                    #Make sure the channel value we just got has a newer SCLK than the one
#                    #in the table...otherwise it's not the most recent value
#                    if oldObj is None or obj.sclkExact >= oldObj.sclkExact:
#                        self.channelValueTable[obj.channelId] = obj
                    list = self.channelValueTable.get(obj.channelId,collections.deque(maxlen=self._channelValueListSize))
                    list.appendleft(obj)
                    self.channelValueTable[obj.channelId] = list
                finally:
                    self.channelValueTable.lock.release()

            else:

                self.recordedChanValCount += 1
                self.recordedChannelValueTable.lock.acquire()
                try:
#                    oldObj = self.recordedChannelValueTable.getById(obj.channelId)
#                    #Make sure the channel value we just got has a newer SCLK than the one
#                    #in the table...otherwise it's not the most recent value
#                    if oldObj is None or obj.sclkExact >= oldObj.sclkExact:
#                        self.recordedChannelValueTable[obj.channelId] = obj
                    list = self.recordedChannelValueTable.get(obj.channelId,collections.deque(maxlen=self._channelValueListSize))
                    list.appendleft(obj)
                    self.recordedChannelValueTable[obj.channelId] = list
                finally:
                    self.recordedChannelValueTable.lock.release()

            #print "Parsed %s " % obj

        # obj is a EVR
        elif isinstance(obj,mpcsutil.evr.Evr):

            is_realtime = getattr(obj,'realtime',False)

            #Need to add to the lookback list before adding to the actual table
            #to keep lists/tables in sync for wait condition usage
            if is_realtime:

                self.evrCount += 1
                self.evrTable.lock.acquire()
                try:
                    list = self.evrTable.get(obj.eventId,collections.deque(maxlen=self._evrListSize))
                    list.appendleft(obj)
                    self.evrTable[obj.eventId] = list
                finally:
                    self.evrTable.lock.release()

            else:

                self.recordedEvrCount += 1
                self.recordedEvrTable.lock.acquire()
                try:
                    list = self.recordedEvrTable.get(obj.eventId,collections.deque(maxlen=self._evrListSize))
                    list.appendleft(obj)
                    self.recordedEvrTable[obj.eventId] = list
                finally:
                    self.recordedEvrTable.lock.release()

        #obj is a Product
        elif isinstance(obj,mpcsutil.product.Product):

            self.productCount += 1
            self.productTable.lock.acquire()
            try:
                list = self.productTable.get(obj.apid,collections.deque(maxlen=self._productListSize))
#                #Add this back in if we want a single product object per transaction ID
#                #If this is commented out, we'll have separate entries for each partial/complete product
#                product_exists = False
#                for product in list:
#                    if obj.transactionId == product.transactionId:
#                        product_exists = True
#                        product.update(obj)
#                        break
#                if not product_exists:
                list.appendleft(obj)
                self.productTable[obj.apid] = list
            finally:
                self.productTable.lock.release()

        #obj is a CFDP Indication
        elif isinstance(obj,mpcsutil.cfdp.CfdpIndication):

            self.cfdpIndicationCount += 1
            self.cfdpIndicationTable.lock.acquire()
            try:
                list = self.cfdpIndicationTable.get(obj.sourceEntityId + ':' + obj.transactionSequenceNumber,collections.deque(maxlen=self._cfdpIndicationListSize))
                list.appendleft(obj)
                self.cfdpIndicationTable[obj.sourceEntityId + ':' + obj.transactionSequenceNumber] = list
            finally:
                self.cfdpIndicationTable.lock.release()

        else:
            return

        obj.receiveTime = receiveTime
        if is_realtime:
            self._updateTimeFields(obj)
        #Check this object against any existing wait conditions
        self._checkConditions(obj)

    def _parseMessage(self,data):
        '''Parse a message received by the downlink proxy daemon thread from the Java monitor process.  This method
        is only ever called by the downlink proxy daemon thread.

        The if/else ladder in this method does absolute minimal string compares in order to find out what type of message
        was received.

        This method is responsible for parsing the message, adding it to the appropriate history lists, incrementing the
        local message counts, and checking the message against any existing waiting conditions.

        Args
        -----
        data - The message received by the daemon thread from the Java chill monitor process (string)

        Returns
        --------
        None'''

        # Does it matter that I set the recieveTime in _parseObject()???

        if not data:
            return

        #Got a channel value (CSV formatted)
        if data.startswith("ch"):
            obj = mpcsutil.channel.ChanVal(csvString=data)

        #Got an EVR (CSV formatted)
        elif data.startswith("e"):
            obj = mpcsutil.evr.Evr(csvString=data)

        #Got a product (CSV formatted)
        elif data.startswith("p"):
            obj = mpcsutil.product.Product(csvString=data)

        #Got a CFDP Indication (CSV formatted)
        elif data.startswith("cf"):
            obj = mpcsutil.cfdp.CfdpIndication(csvString=data)

        else:
            return

        self._parseObject(obj)  # Performs the rest of the processing on the object

    def _updateTimeFields(self,obj):
        '''Take in an EHA sample, an EVR, or a Product update and
        update the fields that store the current SCLK, SCET, and ERT to
        have the most recent values.'''

        # So it turns out that there are two major things to consider here...
        #
        # 1. Realtime stuff is ok, but recorded stuff can have weird SCLKs.
        # Weird SCLKs are bad.
        #
        # 2. Products can also have weird SCLKs (e.g. during boot time) that can
        # mess up these calculations.
        #
        # So for SCLK sanity, we only look at realtime EHA and EVRs
        # (the realtime enforcement is currently handled by the caller of this function)

        if hasattr(obj,'sclkExact') and obj.sclkExact > self.currentSclkExact:
            self.currentSclkExact = obj.sclkExact

        if hasattr(obj,'scetExact') and obj.scetExact > self.currentScetExact:
            self.currentScetExact = obj.scetExact

        if hasattr(obj, 'scetNano') and obj.scetNano > self.currentScetNano:
            self.currentScetNano = obj.scetNano
        if hasattr(obj,'ertExact') and obj.ertExact > self.currentErtExact:
            self.currentErtExact = obj.ertExact

    def waitBySclk(self,ticks=0):
        '''Pause for "ticks" number of SCLK ticks before returning.  Like the normal Python time.sleep method,
        but operates on SCLK ticks instead of wall clock seconds.  The actual amount of time spent waiting is approximate. It
        is not possible to have 1 to 1 fidelity with the SCLK in MTAK.

        Args
        -----
        The number of SCLK ticks to pause for.  Can be fractional. (float)

        Returns
        --------
        None'''

        _log().debug('mtak.down.DownlinkProxy.waitBySclk()')

        ticksToWait = float(ticks)
        if ticksToWait <= 0:
            return

        currentSclkFloat = mpcsutil.timeutil.getSclkFloatingPoint(self.currentSclkExact)

        endSclkFloat = ticksToWait + currentSclkFloat
        endSclkExact = mpcsutil.timeutil.parseSclkString(str(endSclkFloat))

        self._waitForSclk(endSclkExact)

    def waitUntilSclk(self,until):
        '''Pauses until the spacecraft SCLK advances past the input value "until". The actual amount
        of time spent waiting is approximate.  It is not possible to have 1 to 1 fidelity with the SCLK
        in MTAK.

        Args
        -----
        until - The SCLK time to wait until formatted either as an integer, a float, or a string in
                the format of ticks<separator>subticks (separator is usually something like . or - (string)

        Returns
        --------
        None'''

        _log().debug('mtak.down.DownlinkProxy.waitUntilSclk()')

        if until is None:
            return

        until = str(until)
        endSclkExact = mpcsutil.timeutil.parseSclkString(until)
        self._waitForSclk(endSclkExact)

    def _waitForSclk(self,endSclkExact):

        # This modified busy-wait algorithm came straight out of the
        # Python threading.py module in the "wait" method on a
        # synchronization variable
        #
        # Balancing act:  We can't afford a pure busy loop, so we
        # have to sleep; but if we sleep the whole timeout time,
        # we'll be unresponsive.  The scheme here sleeps very
        # little at first, longer as time goes on, but never longer
        # than 20 times per second.
        difference = six.MAXSIZE
        delay = 0.0005
        while difference > 0:

            difference = endSclkExact - self.currentSclkExact
            delay = min(delay*2,.05) #Keep increasing our delay up to .05 seconds max
            time.sleep(delay)

    def registerSyncWait(self,waitCondition,timeout=None,lookback=None,sclkTimeout=None,sclkLookback=None):
        '''Register a wait condition.  Conditions are registered synchronously and will
        wait for certain conditions to be satisfied by incoming messages.  This method can
        either wait indefinitely and not return until the condition has been met or it can
        time out after a certain number of seconds and return whether or not the condition was
        met within that time amount.

        NOTE: Registering wait conditions will greatly reduce the throughput performance of
        the MTAK's downlink.

        Args
        -----
        waitCondition - The condition that is to be satisfied (a subclass of mtak.wait.AbstractWait)
        timeout - If this value is zero, this method will not return until the condition has been satisfied.  Otherwise
                  this is a value in seconds specifying how long to wait for the condition to be satisfied before returning.  If
                  the condition is satisfied, the method will return immediately, otherwise it will return when the timeout has been
                  reached. (int)
        lookback - If this value is zero, it is ignored.  Otherwise, this value should be a positive number of seconds
                       that specify how far to look back in time to see if messages that satisfy the condition have already arrived
                       (this is to prevent the case where a wait condition is registered, but while the condition was registering, the
                       messages that resolve the condition already arrived). (int)

        Returns
        --------
        The object that satisfied the wait condition if the condition was met or None otherwise (various types)'''

        _log().debug('mtak.down.DownlinkProxy.registerSyncWait()')

        if sclkTimeout is not None:
            sclkTimeout = float(sclkTimeout)

        if sclkLookback is not None:
            sclkLookback = float(sclkLookback)

        if timeout is not None:
            timeout = int(timeout)
        elif sclkTimeout is None:
            timeout = mtak.defaultWaitTimeout
        if timeout is not None and sclkTimeout is not None:
            timeout = None

        if lookback is not None:
            lookback = int(lookback)
        elif sclkLookback is None:
            lookback = mtak.defaultWaitLookback
        if lookback is not None and sclkLookback is not None:
            lookback = None

        #Attach temporary variables to the condition to use
        waitCondition.conditionMet = False
        waitCondition.result = None
        waitCondition.sclkTimeoutExact = None
        waitCondition.conditionSyncVar = threading.Condition()
        waitCondition.checkRequiredSettings()

        #If a look back time was specified, we need to look back through our message history for all messages
        #received in the last 'lookback' seconds and see if they solve the condition
        result = self._doLookback(lookback, sclkLookback, waitCondition)
        if result is not None:
            return result

        return self._doWait(waitCondition,timeout,sclkTimeout)

    def _doWait(self,waitCondition,timeout,sclkTimeout):

        #We need to acquire the condition's lock in order to utilize the
        #synchronization variable attached to the condition
        waitCondition.conditionSyncVar.acquire()

        if timeout is not None:

            #Wait indefinitely
            if timeout < 0:
                waitCondition.conditionSyncVar.wait(six.MAXSIZE) #if we try to do infinite wait, we can't respond to CTRL-C, so we just have to make it a HUGE timeout
                                                                #(NOTE: using the wait(int) version of wait means we're doing busy waiting)
            #Wait until a timeout of 'timeout' seconds
            else:
                waitCondition.conditionSyncVar.wait(timeout=timeout)

        elif sclkTimeout is not None:

            ticksToWait = float(sclkTimeout)
            if ticksToWait <= 0:

                waitCondition.conditionSyncVar.wait(six.MAXSIZE) #if we try to do infinite wait, we can't respond to CTRL-C, so we just have to make it a HUGE timeout
                                                                #(NOTE: using the wait(int) version of wait means we're doing busy waiting)
            else:

                currentSclkFloat = mpcsutil.timeutil.getSclkFloatingPoint(self.currentSclkExact)
                endSclkFloat = ticksToWait + currentSclkFloat
                endSclkExact = mpcsutil.timeutil.parseSclkString(str(endSclkFloat))

                #Attach the SCLK timeout value to this wait condition...we'll need this value elsewhere (in checkCondition) to determine
                #when we've timed out on this wait
                waitCondition.sclkTimeoutExact = endSclkExact

                waitCondition.conditionSyncVar.wait(six.MAXSIZE) #if we try to do infinite wait, we can't respond to CTRL-C, so we just have to make it a HUGE timeout
                                                                #(NOTE: using the wait(int) version of wait means we're doing busy waiting)

        #We broke the wait condition, so we can give the lock back
        waitCondition.conditionSyncVar.release()

        #Remove the condition from our condition list
        self._waitListLock.acquire()
        self._waitList.remove(waitCondition)
        self._waitListLock.release()

        return waitCondition.result

    def _doLookback(self,lookback,sclkLookback,waitCondition):

        lowerBoundTime = None
        lowerBoundSclkExact = None

        if lookback is not None:
            lookback = int(lookback)
            if lookback > 0:
                #Need to convert the lookback seconds to an ISO time (subtract 'lookback' seconds from now) and then
                #check all messages whose eventTime is greater than this time
                lowerBoundTime = time.time() - lookback

        elif sclkLookback is not None:
            ticksToLookback = float(sclkLookback)
            if ticksToLookback > 0:
                currentSclkFloat = mpcsutil.timeutil.getSclkFloatingPoint(self.currentSclkExact)
                lowerBoundSclkFloat = currentSclkFloat - ticksToLookback
                if lowerBoundSclkFloat > 0:
                    lowerBoundSclkExact = mpcsutil.timeutil.parseSclkString(str(lowerBoundSclkFloat))


        if lowerBoundTime is None and lowerBoundSclkExact is None:
            self._waitListLock.acquire()
            self._waitList.append(waitCondition)
            self._waitListLock.release()
            return None

        isEhaWait = isinstance(waitCondition,mtak.wait.ChanValWait)
        isEvrWait = isinstance(waitCondition,mtak.wait.EvrWait)
        isProductWait = isinstance(waitCondition,mtak.wait.ProductWait)
        isCfdpIndicationWait = isinstance(waitCondition,mtak.wait.CfdpIndicationWait)

        result = None
        realtime_lookback_list = []
        recorded_lookback_list = []
        do_realtime = not hasattr(waitCondition,'realtime') or getattr(waitCondition,'realtime') == True
        do_recorded = not hasattr(waitCondition,'realtime') or getattr(waitCondition,'realtime') == False

        if isEhaWait:
            channelId = getattr(waitCondition,'channelId',None)
            name = getattr(waitCondition,'name',None)

            #Must do this inside the lock so that we're sure no more telemetry of this type can come in and not get checked against this wait condition
            self.channelValueTable.lock.acquire()
            self.recordedChannelValueTable.lock.acquire()
            try:
                if do_realtime:
                    if channelId is not None:
                        realtime_lookback_list.extend(self.channelValueTable.getById(channelId))
                    elif name is not None:
                        realtime_lookback_list.extend(self.channelValueTable.getByName(name))
                    result = self._checkLookback(lowerBoundTime, lowerBoundSclkExact, waitCondition, realtime_lookback_list)

                if do_recorded and result is None:
                    if channelId is not None:
                        recorded_lookback_list.extend(self.recordedChannelValueTable.getById(channelId))
                    elif name is not None:
                        recorded_lookback_list.extend(self.recordedChannelValueTable.getByName(name))
                    result = self._checkLookback(lowerBoundTime, lowerBoundSclkExact, waitCondition, recorded_lookback_list)

            finally:
                self.channelValueTable.lock.release()
                self.recordedChannelValueTable.lock.release()

        elif isEvrWait:
            eventId = getattr(waitCondition,'eventId',None)
            name = getattr(waitCondition,'name',None)

            #Must do this inside the lock so that we're sure no more telemetry of this type can come in and not get checked against this wait condition
            self.evrTable.lock.acquire()
            self.recordedEvrTable.lock.acquire()
            try:
                if do_realtime:
                    if eventId is not None:
                        realtime_lookback_list.extend(self.evrTable.getByEventId(eventId))
                    elif name is not None:
                        realtime_lookback_list.extend(self.evrTable.getByName(name))
                    result = self._checkLookback(lowerBoundTime, lowerBoundSclkExact, waitCondition, realtime_lookback_list)

                if do_recorded and result is None:
                    if eventId is not None:
                        recorded_lookback_list.extend(self.recordedEvrTable.getByEventId(eventId))
                    elif name is not None:
                        recorded_lookback_list.extend(self.recordedEvrTable.getByName(name))
                    result = self._checkLookback(lowerBoundTime, lowerBoundSclkExact, waitCondition, recorded_lookback_list)
            finally:
                self.evrTable.lock.release()
                self.recordedEvrTable.lock.release()

        elif isProductWait:
            apid = getattr(waitCondition,'apid',None)
            name = getattr(waitCondition,'name',None)

            #There's no such thing as a "recorded" product so we just treat them all as realtime
            #Must do this inside the lock so that we're sure no more telemetry of this type can come in and not get checked against this wait condition
            self.productTable.lock.acquire()
            try:
                if apid is not None:
                    realtime_lookback_list.extend(self.productTable.getByApid(apid))
                elif name is not None:
                    realtime_lookback_list.extend(self.productTable.getByName(name))
                result = self._checkLookback(lowerBoundTime, lowerBoundSclkExact, waitCondition, realtime_lookback_list)
            finally:
                self.productTable.lock.release()

        elif isCfdpIndicationWait:
            sourceEntityId = getattr(waitCondition,'sourceEntityId', None)
            transactionSequenceNumber = getattr(waitCondition,'transactionSequenceNumber', None)
            indicationTypeList = getattr(waitCondition,'indicationTypeList', None)

            self.cfdpIndicationTable.lock.acquire()
            try:
                ind_list = None

                if sourceEntityId and transactionSequenceNumber:
                    ind_list = self.cfdpIndicationTable.getByTransactionId(sourceEntityId, transactionSequenceNumber)
                elif sourceEntityId:
                    ind_list = [ind for ind in self.cfdpIndicationTable.to_list() if ind.sourceEntityId == sourceEntityId]
                elif transactionSequenceNumber:
                    ind_list = [ind for ind in self.cfdpIndicationTable.to_list() if ind.transactionSequenceNumber == transactionSequenceNumber]
                else:
                    ind_list = self.cfdpIndicationTable.to_list()

                if indicationTypeList:
                    ind_list = [ind for ind in ind_list if ind.indicationType in indicationTypeList]

                realtime_lookback_list.extend(ind_list)
                result = self._checkLookback(lowerBoundTime, lowerBoundSclkExact, waitCondition, realtime_lookback_list)
            finally:
                self.cfdpIndicationTable.lock.release()

        if result is None:
            self._waitListLock.acquire()
            self._waitList.append(waitCondition)
            self._waitListLock.release()

        return result

    def _checkLookback(self,lowerBoundTime,lowerBoundSclkExact,waitCondition,lookbackList):

        if not lookbackList:
            return None

        if lowerBoundTime:
            for telemetryItem in lookbackList:
                if telemetryItem.receiveTime >= lowerBoundTime:
                    if waitCondition.checkCondition(telemetryItem):
                        _log().info('Wait succeeded (Found value during time lookback).')
                        return waitCondition.result
                else:
                    _log().info('No value found during time lookback...will wait for new telemetry to arrive')
                    break;
                    #This isn't perfect since messages can come out of order, but it should be close enough
                    #(break the first time we find a message below our lower time bound)...it'll keep us
                    #from looking through the entire list every time we make a condition since the list could be HUGE

        elif lowerBoundSclkExact:
            for telemetryItem in lookbackList:
                #sclkToCheck = 0
                #if lookbackList is self._productLookbackList:
                #    sclkToCheck = telemetryItem.dvtSclkExact
                #else:
                #    sclkToCheck = telemetryItem.sclkExact

                #if sclkToCheck >= lowerBoundSclkExact:
                if telemetryItem.sclkExact >= lowerBoundSclkExact:
                    if waitCondition.checkCondition(telemetryItem):
                        _log().info('Wait succeeded (Found value during SCLK lookback).')
                        return waitCondition.result
                else:
                    _log().info('No value found during SCLK lookback...will wait for new telemetry to arrive')
                    break;
                    #This isn't perfect since messages can come out of order, but it should be close enough
                    #(break the first time we find a message below our lower time bound)...it'll keep us
                    #from looking through the entire list every time we make a condition since the list could be HUGE


        #By appending to the wait list within this lock, we ensure that no EHA message can arrive and not get checked
        #against this condition that we're registering
        #self._waitListLock.acquire()
        #self._waitList.append(waitCondition)
        #self._waitListLock.release()

        return None

    def _checkConditions(self,obj):
        '''Check the input 'obj' (object representation of a message we received) against all the
        wait conditions that are currently registered on the downlink proxy. If the input object solves
        the wait condition, a "notify" will be issued on the synchronization variable that is being
        waited on.

        Args
        -----
        obj - The obj representation of a message just received off the message bus (any object)

        Returns
        --------
        None'''

        _log().debug('mtak.down.DownlinkProxy._checkCondition()')

        #If there are no wait conditions, get out of this method quickly
        self._waitListLock.acquire()
        if not self._waitList:
            self._waitListLock.release()
            return

        #Loop through all the registered conditions
        for condition in self._waitList:

            #Acquire the lock attached to the condition, check if the
            #condition is solved by the input object and if so, notify the
            #thread waiting in the registerSyncWait(...) method
            condition.conditionSyncVar.acquire()

            if condition.checkCondition(obj):
                condition.conditionSyncVar.notify()

            if condition.sclkTimeoutExact is not None:
                if self.currentSclkExact >= condition.sclkTimeoutExact:
                    condition.conditionSyncVar.notify()

            condition.conditionSyncVar.release()
        self._waitListLock.release()

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
