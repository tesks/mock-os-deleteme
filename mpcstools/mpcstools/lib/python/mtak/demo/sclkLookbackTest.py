#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

from mtak.wrapper import *

def test():
    startup()

    wait(20)

    print('Current SCLK = {}'.format(get_current_sclk()))
    print('{}'.format(get_eha(channelId='THRM-2904')))

    result = wait_eha(channelId='THRM-2904',sclkLookback='12000',timeout='1')

    print('{}'.format(result))

    shutdown()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
