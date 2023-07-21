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
    [send_fsw_cmd('CMD_NO_OP') for _ in range(0,100)]

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
