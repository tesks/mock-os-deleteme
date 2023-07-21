#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import mpcsutil
import mtak
import importlib

mission = mpcsutil.config.GdsConfig().getMission()

try:
    importlib.import_module('{}.mtak'.format(mission))
    # exec 'import %s.mtak'  % (mission)
except ImportError:
    pass

def getUplinkProxy(sessionConfig):

    global mission

    objname = mpcsutil.config.GdsConfig().getProperty('automationApp.internal.mtak.adaptation.uplinkProxy','mtak.up.UplinkProxy')

    return eval('%s(sessionConfig)' % objname)

def getDownlinkProxy(sessionConfig):

    global mission

    objname = mpcsutil.config.GdsConfig().getProperty('automationApp.internal.mtak.adaptation.downlinkProxy','mtak.down.DownlinkProxy')

    return eval('%s(sessionConfig)' % objname)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
