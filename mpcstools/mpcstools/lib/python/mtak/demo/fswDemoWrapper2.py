#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

from mtak.wrapper import *

def test():

    #Start up MTAK and connect to the current running MPCS session
    startup()

    #Write a message to the console and the log
    console('I am starting my test')

    #Wait for 5.1 seconds
    wait(5.1)

    #Send a VC-1 command
    send_fsw_cmd('CMD_NO_OP')

    #Wait for channel TLM1-1234 with a DN value of 1 to arrive (or timeout)
    chanval = wait_eha('TLM1-1234',dn=1,timeout=5,lookback=5)
    if not chanval:
        console('Channel ID TLM1-1234 did not have a value of 1')

    #Wait for an EVR with event ID 220 to arrive (or timeout)
    evr = wait_evr(eventId=220,timeout=5,lookback=5)
    if not evr:
        console('Could not find the EVR with event ID 220')

    #Execute a Unix shell command
    value = shell_cmd('date')
    console('The date is ' + value)

    #Send a command to the SSE
    success = send_sse_cmd('uldl rcea enable_uplink elta on')
    if not success:
        console('The command "uldl rcea enable_uplink elta on" failed to send!')

    #Write to the log
    log('Finished my test')

    #Shutdown this MTAK script
    shutdown()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
