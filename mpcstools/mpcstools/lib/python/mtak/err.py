#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module contains exceptions that will be used by MTAK modules.
"""

from __future__ import (absolute_import, division, print_function)


import mpcsutil

class MtakError(mpcsutil.err.MpcsUtilError):
    '''CommonError is the base exception for all of the common exceptions
    that can be raised'''
    def __init__(self, args=None):
        '''constructor

            args = the string description of the exception (string)'''
        Exception.__init__(self,args)

class UplinkError(MtakError):
    '''UplinkError is the superclass for all exceptions
    specifically related to uplink issues'''

    pass

class DownlinkError(MtakError):
    '''DownlinkError is the superclass for all exceptions
    specifically related to downlink issues'''

    pass

class RadiationError(UplinkError):
    '''Radiation error is the exception that is raised when there is a
    problem transmitting (radiating) uplink via MPCS'''

    pass

class CommandFormatError(UplinkError):
    '''CmdFormatError is the exception that is raised when there is a
    an illegally formatted input given to an UplinkProxy send method'''

    pass

class WaitError(MtakError):
    '''WaitError is an exception raised when an improperly formed wait
    condition object is used.'''

    pass

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
