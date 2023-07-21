#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
A sample script of MTAK wrapper functionality

Date: 12/14/2007
"""

from __future__ import (absolute_import, division, print_function)

from mtak.wrapper import *

def test():
    startup()

    if send_fsw_cmd('PWR_UTIL_TEST,0,283,0,0'):
        log('this is a log message')
        console('it worked')
    else:
        console('PWR_UTIL_TEST command failed!')

    wait(2)

    result = verify_eha('SSE4-6400',dn=12)

    if result:

        console('Verified value of SSE4-6400 is 12.')

    else:

        console('The value of SSE4-6400 is not 12.  Check the log file.')

    shutdown()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
