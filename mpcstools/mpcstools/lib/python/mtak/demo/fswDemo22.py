#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import logging
import mpcsutil
import mtak
import subprocess
import time

def test():
    
    logger = logging.getLogger('mpcs.mtak')

    #Choose the MPCS session to connect to
    sessionConfig = mpcsutil.config.SessionConfig(filename=mpcsutil.config.getDefaultSessionConfigFilename())

    #Create the uplink and downlink proxies
    uplinkProxy = mtak.up.UplinkProxy(sessionConfig)
    downlinkProxy = mtak.down.DownlinkProxy(sessionConfig)

    #Send a message to the log
    logger.info('Starting script at ' + mpcsutil.timeutil.getIsoTime())

    #Start receiving telemetry
    downlinkProxy.start()

    logger.info('I am starting my test')

    #Send an FSW command
    try:
        uplinkProxy.sendFlightCommand('CMD_NO_OP')
    except mtak.err.RadiationError:
        logger.error('The command CMD_NO_OP failed to send!')

    #Pause for 10 seconds
    time.sleep(10)

    #Wait for channel TLM1-1234 with a DN value of 1 to arrive (or timeout)
    #Wait for an EVR with event ID 220 to arrive (or timeout)
    chanvalwait = mtak.wait.ChanValWait(channelId='TLM1-1234',dn=1)
    evrwait = mtak.wait.EvrWait(eventId=220)
    compoundwait = mtak.wait.CompoundWait(operator='AND')
    compoundwait.waitList = [chanvalwait,evrwait]
    result = downlinkProxy.registerSyncWait(compoundwait,timeout=5,lookback=5)
    if not result:
        logger.error('The EVR with event ID 220 and the dn value of 1 on channel TLM1-1234 did not arrive')

    #Execute a UNIX shell command and get the result and return code
    cmd = 'date'
    process = subprocess.Popen(cmd,shell=True,stdout=subprocess.PIPE,stdin=None,stderr=subprocess.STDOUT)
    returnCode = process.wait()

    if returnCode != 0:
        logger.error('Error executing shell command "%s" (return code was %d): ' % (cmd,returnCode) + ''.join(process.stdout))

    dateValue = ''.join(process.stdout)

    logger.info('The date is ' + dateValue)

    #Send an SSE command
    try:
        uplinkProxy.sendSseCommand('uldl rcea enable_uplink elta on')
    except mtak.err.RadiationError:
        logger.error('The command "uldl rcea enable_uplink elta on" failed to send!')

    logger.info('Finished my test')

    #Stop receiving telemetry
    downlinkProxy.stop()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
