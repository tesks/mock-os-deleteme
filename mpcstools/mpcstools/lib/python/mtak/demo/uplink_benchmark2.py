#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

from mtak.wrapper import *
import time

def test():
    startupBegin = time.time()
    startup()
    startupEnd = time.time()

    cmdsBegin = time.time()
    send_fsw_cmd('CMD_NO_OP')
    send_hw_cmd('HDW_SYSTEM_RESET')
    send_sse_cmd('sse:asdf fdsa 124 sfda')
    send_fsw_file('/Users/bnash/.bash_profile','/tmp/file')
    send_fsw_scmf('/Users/bnash/test.scmf')
    send_fsw_raw_data('/Users/bnash/test.scmf')
    cmdsEnd = time.time()

    shutdownBegin = time.time()
    shutdown()
    shutdownEnd = time.time()

    print('Startup Time = {}'.format(startupEnd-startupBegin))
    print('CMD Time = {}'.format(cmdsEnd-cmdsBegin))
    print('Shutdown Time = {}'.format(shutdownEnd-shutdownBegin))

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
