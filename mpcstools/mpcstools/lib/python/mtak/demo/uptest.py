#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import mpcsutil
import mpcsutil.test
import mtak
import mtak.test
import os
import os.path
import random
import signal
import socket
import subprocess
import sys
import tempfile
import threading
import time
import unittest

class UpTest(unittest.TestCase):

    def setUp(self):

        self._ioThread = None
        self._running = False
        self._serverSocket = None
        self._uplinkPort = None
        self._uplinkHost = 'localhost'
        self.gdsConfig = mpcsutil.config.GdsConfig()

        self.key = None
        self.conn = None
        self.host = None

        (self.key,self.host,self.conn) = mpcsutil.test.dbutil.insertSampleSession()

        self._uplinkPort = random.randint(40000,65000)

        self.config = mpcsutil.config.SessionConfig(key=self.key,host=self.host)
        self.config.fswHost = self._uplinkHost
        self.config.fswUplinkPort = self._uplinkPort
        self.config.sseHost = self._uplinkHost
        self.config.sseUplinkPort = self._uplinkPort

        self.proxy = mtak.up.UplinkProxy(self.config)

    def tearDown(self):

        mpcsutil.test.dbutil.removeDatabaseSession(self.key,self.host,self.conn)
        self._closeUplinkPort()

        self.config = None
        self.proxy = None
        self.key = None
        self.conn = None
        self.host = None

    def testCreate(self):

        try:
            mtak.up.UplinkProxy(None)
            self.fail('Did not detect SessionConfig as "None" in __init__(...)')
        except mpcsutil.err.InvalidInitError:
            pass

        try:
            self.config.key = None
            mtak.up.UplinkProxy(self.config)
            self.fail('Did not detect missing session key on session self.config in __init__(...)')
        except mpcsutil.err.InvalidInitError:
            pass

        try:
            del self.config.key
            mtak.up.UplinkProxy(self.config)
            self.fail('Did not detect missing session key on session self.config in __init__(...)')
        except mpcsutil.err.InvalidInitError:
            pass

    def testSendFlightCommand(self):

        ssePrefix = self.gdsConfig.getProperty('Uplink.SSE.commandPrefix','sse:')

        try:
            self.proxy.sendFlightCommand(ssePrefix + 'CMD_NO_OP')
            self.fail('Did not detect SSE prefix on flight command')
        except mtak.err.CommandFormatError:
            pass

        try:
            self.proxy.sendFlightCommand('CMD_NO_OP')
            self.fail('Did not detect RadiationError when no socket was present')
        except mtak.err.RadiationError:
            pass

        try:
            del self.config.fswHost
            self.proxy.sendFlightCommand('CMD_NO_OP')
            self.fail('Did not detect InvalidStateError when there was no FSW host value')
        except mpcsutil.err.InvalidStateError:
            self.config.fswHost = self._uplinkHost

        try:
            del self.config.fswUplinkPort
            self.proxy.sendFlightCommand('CMD_NO_OP')
            self.fail('Did not detect InvalidStateError when there was no FSW uplink port value')
        except mpcsutil.err.InvalidStateError:
            self.config.fswUplinkPort = self._uplinkPort

#        self._openUplinkPort()
#
#        try:
#            self.proxy.sendFlightCommand('CMD_NO_OP')
#        except:
#            self.fail('Encountered an unexpected exception sending a flight command: %s' % (str(sys.exc_info())))
#
#        try:
#            self.proxy.sendFlightCommand(mpcsutil.command.FlightSoftwareCommand(cmdString='CMD_NO_OP'))
#        except:
#            self.fail('Encountered an unexpected exception sending a flight command: %s' % (str(sys.exc_info())))
#
#        try:
#            self.proxy.sendFlightCommand('CMD_NO_OP',validate=False)
#        except:
#            self.fail('Encountered an unexpected exception sending a flight command: %s' % (str(sys.exc_info())))

    def testSendSseCommand(self):

        ssePrefix = self.gdsConfig.getProperty('Uplink.SSE.commandPrefix','sse:')

        try:
            self.proxy.sendSseCommand(ssePrefix + 'some SSE command')
            self.fail('Did not detect RadiationError when no socket was present')
        except mtak.err.RadiationError:
            pass

        try:
            del self.config.sseHost
            self.proxy.sendSseCommand(ssePrefix + 'some SSE command')
            self.fail('Did not detect InvalidStateError when there was no SSE host value')
        except mpcsutil.err.InvalidStateError:
            self.config.sseHost = self._uplinkHost

        try:
            del self.config.sseUplinkPort
            self.proxy.sendSseCommand(ssePrefix + 'some SSE command')
            self.fail('Did not detect InvalidStateError when there was no SSE uplink port value')
        except mpcsutil.err.InvalidStateError:
            self.config.sseUplinkPort = self._uplinkPort

        self._openUplinkPort()

        try:
            self.proxy.sendSseCommand(ssePrefix + 'some SSE command')
        except:
            self.fail('Encountered an unexpected exception sending an SSE command: %s' % (str(sys.exc_info())))

        try:
            self.proxy.sendSseCommand('some SSE command')
        except mtak.err.CommandFormatError:
            self.fail('Encountered an unexpected exception sending an SSE command: %s' % (str(sys.exc_info())))

        try:
            self.proxy.sendSseCommand('\'some SSE command\'')
        except mtak.err.CommandFormatError:
            self.fail('Encountered an unexpected exception sending an SSE command: %s' % (str(sys.exc_info())))

        try:
            self.proxy.sendSseCommand('"some SSE command"')
        except mtak.err.CommandFormatError:
            self.fail('Encountered an unexpected exception sending an SSE command: %s' % (str(sys.exc_info())))

        try:
            self.proxy.sendSseCommand(mpcsutil.command.SseCommand(cmdString=ssePrefix + 'some SSE command'))
        except:
            self.fail('Encountered an unexpected exception sending an SSE command: %s' % (str(sys.exc_info())))

        self._closeUplinkPort()


    def testSendFile(self):

        try:
            self.proxy.sendFile(source='./TestConfig3.xml',target='/tmp/conf',type='0')
            self.fail('Did not detect RadiationError when no socket was present')
        except mtak.err.RadiationError:
            pass

        try:
            del self.config.fswHost
            self.proxy.sendFile(source='./TestConfig3.xml',target='/tmp/conf',type='0')
            self.fail('Did not detect InvalidStateError when there was no FSW host value')
        except mpcsutil.err.InvalidStateError:
            self.config.fswHost = self._uplinkHost

        try:
            del self.config.fswUplinkPort
            self.proxy.sendFile(source='./TestConfig3.xml',target='/tmp/conf',type='0')
            self.fail('Did not detect InvalidStateError when there was no FSW uplink port value')
        except mpcsutil.err.InvalidStateError:
            self.config.fswUplinkPort = self._uplinkPort

        self._openUplinkPort()

        try:
            self.proxy.sendFile(source='/some/non/existent/file',target='/tmp/conf',type='0')
            self.fail('Did not detect non-existent input file')
        except:
            pass

        self._closeUplinkPort()


    def testScmf(self):

        try:
            self.proxy.sendScmf('./good.scmf')
            self.fail('Did not detect RadiationError when no socket was present')
        except mtak.err.RadiationError:
            pass

        try:
            del self.config.fswHost
            self.proxy.sendScmf('./good.scmf')
            self.fail('Did not detect InvalidStateError when there was no FSW host value')
        except mpcsutil.err.InvalidStateError:
            self.config.fswHost = self._uplinkHost

        try:
            del self.config.fswUplinkPort
            self.proxy.sendScmf('./good.scmf')
            self.fail('Did not detect InvalidStateError when there was no FSW uplink port value')
        except mpcsutil.err.InvalidStateError:
            self.config.fswUplinkPort = self._uplinkPort

        self._openUplinkPort()

        try:
            self.proxy.sendScmf('./good.scmf')
        except:
            self.fail('Encountered an unexpected exception sending an SCMF: %s' % (str(sys.exc_info())))

        try:
            self.proxy.sendScmf(mpcsutil.command.Scmf(filename='./good.scmf'))
        except:
            self.fail('Encountered an unexpected exception sending an SCMF: %s' % (str(sys.exc_info())))

        try:
            self.proxy.sendScmf('./bad.scmf')
            self.fail('Did not detect input file that fails checksum test')
        except:
            pass

        try:
            self.proxy.sendScmf('/some/non/existent/file.scmf')
            self.fail('Did not detect non-existent input file')
        except:
            pass

        self._closeUplinkPort()


    def _openUplinkPort(self):

        self._ioThread = threading.Thread(target=self._ioLoop,name='Server Socket Thread',args=())
        self._ioThread.setDaemon(True)
        self._running = False
        self._ioThread.start()

        time.sleep(5)

    def _ioLoop(self,*args):

        self._serversocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

        exceptionCaught = True
        while exceptionCaught == True:
            try:
                self._serversocket.bind((self._uplinkHost,self._uplinkPort))
            except:
                exceptionCaught = True
            else:
                exceptionCaught = False

        self._serversocket.listen(1)

        self._running = True
        while self._running:
            (clientSock,address) = self._serversocket.accept()
            while clientSock.recv(999999):
                pass

    def _closeUplinkPort(self):

        self._running = False
        if self._serverSocket is not None:
            self._serverSocket.close()
            self._serverSocket = None

        if self._ioThread is not None:
            self._ioThread = None

        time.sleep(5)

def test():
    print('Running mtak.test.uptest.py...')
    unittest.main()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
