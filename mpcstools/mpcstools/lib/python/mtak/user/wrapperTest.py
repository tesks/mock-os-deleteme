#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

from mtak.wrapper import *

def test():
    #this is a one line comment

    '''
    This is a
    multiple line
    comment.
    '''

    console('Start up MTAK (connect to current running "chill" instance by default)')
    startup()
    console('')

    console('Pause the script execution for 1.25 seconds')
    wait(1.25)
    console('')

    console('Prompt the user for data input and print their response back out')
    response = prompt()
    console('Response = ' + response)
    console('')

    console('Pause the script until the user hits a key to continue')
    wait_for_key_press()
    console('')

    console('Send FSW command with argument validation')
    success = send_fsw_cmd('CMD_NO_OP')
    print(success)
    console('')

    console('Send FSW command with no argument validation')
    success = send_fsw_cmd('CMD_NO_OP',validate=False)
    print(success)
    console('')

    console('Send an SSE command')
    success = send_sse_cmd('my_sse_command 12 43 542')
    print(success)
    console('')

    console('Upload the generic file from the local disk to the target location on the FSW')
    success = send_fsw_file(source='/home/bnash/file.txt',target='/tmp/a001/a.out')
    print(success)
    console('')

    console('Upload the sequence file from the local disk to the target location on the FSW')
    success = send_fsw_file(source='/home/bnash/file.txt',target='/tmp/a001/a.out',type=1)
    print(success)
    console('')

    console('Send the contents of an SCMF to the FSW')
    success = send_fsw_scmf('/Users/bnash/test.scmf')
    print(success)
    console('')

    console('Write a message to the MTAK log file')
    log('This is a log message')
    console('')

    console('Get the most recent value for a channel')
    dnValue = get_eha('A-0001')
    dnValue = get_eha_dn('A-0001')
    euValue = get_eha_eu('A-0001')
    console('')

    console('Verify that a particular channel has the given value(s)')
    success = verify_eha('B-0001',dn=12)
    success = verify_eha('C-1234',eu=9.86)
    success = verify_eha('FSW1-1484',dn=1,eu=2.2)
    console('')

    console('Wait for a value on a particular channel to arrive')
    #success = wait_eha('A-0001')
    #success = wait_eha('A-0001',dn=400)
    #success = wait_eha('A-0001',eu=123.456)
    success = wait_eha('FSW1-1000',dn=12,timeout=10)
    #success = wait_eha('SSE1-1000',dn=45,lookback=5)
    success = wait_eha('FSW1-1234',dn=90,eu=12.21,timeout=10,lookback=5)
    console('')

    console('Get the most recent EVR with the given characteristics')
    evr = get_evr(eventId=23)
    evr = get_evr(level='ACTIVITY')
    evr = get_evr(sclk='0000012345-113')
    evr = get_evr(message='The command CMD_NO_OP was received successfully.')
    evr = get_evr(module='PWR')
    evr = get_evr(eventId=54,level='WARNING_LO',module='APXS')
    console('')

    console('Wait for an EVR with the given characteristics to arrive')
    #def wait_evr(eventId=None,level=None,sclk=None,message=None,module=None,timeout=0,lookback=0):
    #success = wait_evr(eventId=123,level='ACTIVITY')
    #success = wait_evr(sclk='0000011122-234',message='Transmission of command CMD_NO_OP was successful.',module='PWR')
    success = wait_evr(eventId=22,timeout=15)
    success = wait_evr(eventId=87,timeout=10,lookback=5)
    console('')

    console('Get the most recent EVR with the given characteristics')
    evr = get_dp()
    console('')

    console('Wait for an EVR with the given characteristics to arrive')
    success = wait_dp()
    console('')

    console('Shutdown MTAK')
    shutdown()
    console('')

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
