#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines the types of times that can be displayed by time fields in an MPCS
perspective.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil


class TimeTypes(mpcsutil.enum.EnumType):
    ''' Defines the possible types of times displayed by a time field in a Fixed Layout view.'''

    SCLK_TYPE = 0
    SCET_TYPE = 1
    ERT_TYPE = 2
    UTC_TYPE = 3
    LST_TYPE = 4
    MST_TYPE = 5
    RCT_TYPE = 6

    VALUES = ['SCLK','SCET','ERT','UTC','LST','MST','RCT']

    def __init__(self,numval=None,strval=None):
        super(TimeTypes,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'perspective.timetypes.TimeTypes(numval=%d)' % self.val_num

    def get_default_title(self):

        return self.val_str

    def get_values(self):
        return TimeTypes.VALUES

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return TimeTypes(numval=self.val_num)


# Pre-defined convenience instances
SCLK = TimeTypes(numval=TimeTypes.SCLK_TYPE)
SCET = TimeTypes(numval=TimeTypes.SCET_TYPE)
ERT = TimeTypes(numval=TimeTypes.ERT_TYPE)
UTC = TimeTypes(numval=TimeTypes.UTC_TYPE)
LST = TimeTypes(numval=TimeTypes.LST_TYPE)
MST = TimeTypes(numval=TimeTypes.MST_TYPE)
RCT = TimeTypes(numval=TimeTypes.RCT_TYPE)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
