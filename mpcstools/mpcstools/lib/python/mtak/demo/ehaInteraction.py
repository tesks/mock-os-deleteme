#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
Sample script to send an SSE command and then check
some of the corresponding telemetry.
"""

from __future__ import (absolute_import, division, print_function)

#Import the wrapper functions
from mtak.wrapper import *

def test():
    #Startup the MTAK connection to FSW/SSE
    startup()

    #Send the FSW command and check the result
    console('Sending command to SSE')
    result = send_sse_cmd(command="uldl rcea enable_uplink elta on")
    if not result:
        console('My command failed to send!')

    #Use a wait condition to wait for the EVR to arrive
    console('Waiting for EHA to arrive...')
    channel = wait_eha(channelId='SSE1-1234',dn="1",timeout='10',lookback='5')
    if channel:

        console('Got the channel value I wanted at ERT %s' % (channel.ert))

    else:

        console('The desired channel value did not arrive')

    #Pause the script for 10 seconds
    wait(seconds="10")

    #Verify another channel has the given value
    result = verify_eha(channelId='SSE1-4321',dn='5')
    if result:

        console('Channel SSE1-4321 had a value of 5')

    else:

        console('Channel SSE1-4321 did not have a value of 5')

    #Prompt the user for a channel ID and get that channel's DN value. Then
    #use if/elif/else to print out how big the channel value was
    inputChanId = prompt(message="Enter the ID of the channel to check: ")
    dn = get_eha_dn(channelId=inputChanId)
    if dn < 0:

        console('DN was negative!')

    elif dn > 0 and dn < 10:

        console('DN was small!')

    else:

        console('DN was large!')

    dn2 = get_eha_dn(channelId='SSE1-0001')
    dn3 = get_eha_dn(channelId='SSE1-0002')
    if dn2 > dn3:

        console('SSE1-0001 > SSE1-0002')

    elif dn2 < dn3:

        console('SSE1-0001 < SSE1-0002')

    else:

        console('SSE1-0001 = SSE1-0002')

    shutdown()


def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
