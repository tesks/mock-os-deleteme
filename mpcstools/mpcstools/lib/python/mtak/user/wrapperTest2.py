#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

from mtak.wrapper import *

def test():
    startup()

    #Send the CMD_NO_OP
    result = send_fsw_cmd('CMD_NO_OP')

    if not result:
        log('CMD_NO_OP failed!')

    wait_evr(eventId=55,timeout=5,lookback=2)

    shutdown()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
