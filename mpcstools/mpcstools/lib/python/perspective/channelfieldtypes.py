#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines the channel sub-fields that can be displayed by channel
fields in an MPCS perspective.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil


class ChannelFieldTypes(mpcsutil.enum.EnumType):
    ''' Defines the possible types of channel sub-fields displayed by a channel field in a
    Fixed Layout view.'''

    DN_FIELD = 0
    RAW_FIELD = 1
    EU_FIELD = 2
    VALUE_FIELD = 3
    STATUS_FIELD = 4
    SCET_FIELD = 5
    SCLK_FIELD = 6
    ERT_FIELD = 7
    MST_FIELD = 8
    LST_FIELD = 9
    RCT_FIELD = 10
    ID_FIELD = 11
    TITLE_FIELD = 12
    FSW_NAME_FIELD = 13
    MODULE_FIELD = 14
    OPS_CAT_FIELD = 15
    SUBSYSTEM_FIELD = 16
    DN_UNIT_FIELD = 17
    EU_UNIT_FIELD = 18
    ALARM_STATE_FIELD = 19
    # 11/4/13 - MPCS-5501: Add DSS and Recorded fields
    DSS_ID_FIELD = 20
    RECORDED_FIELD = 21

    VALUES = ['DN','RAW','EU','VALUE','STATUS','SCET', 'SCLK', 'ERT', 'MST', 'LST', 'RCT', \
              'ID', 'TITLE', 'FSW_NAME', 'MODULE', 'OPS_CAT', 'SUBSYSTEM', 'DN_UNIT',
              'EU_UNIT', 'ALARM_STATE', 'DSS_ID', 'RECORDED']

    def __init__(self,numval=None,strval=None):
        super(ChannelFieldTypes,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'perspective.channelfieldtypes.ChannelFieldTypes(numval=%d)' % self.val_num

    def get_default_title(self):

        return self.val_str

    def get_values(self):
        return ChannelFieldTypes.VALUES

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return ChannelFieldTypes(numval=self.val_num)

    def getDefaultHighlight(self):
        if self.val_str in ['DN','RAW','EU','VALUE','STATUS','ALARM_STATE']:
            return True;
        else:
            return False

# Pre-defined convenience instances
DN = ChannelFieldTypes(numval=ChannelFieldTypes.DN_FIELD)
RAW = ChannelFieldTypes(numval=ChannelFieldTypes.RAW_FIELD)
EU = ChannelFieldTypes(numval=ChannelFieldTypes.EU_FIELD)
VALUE = ChannelFieldTypes(numval=ChannelFieldTypes.VALUE_FIELD)
STATUS = ChannelFieldTypes(numval=ChannelFieldTypes.STATUS_FIELD)
SCET = ChannelFieldTypes(numval=ChannelFieldTypes.SCET_FIELD)
SCLK = ChannelFieldTypes(numval=ChannelFieldTypes.SCLK_FIELD)
ERT = ChannelFieldTypes(numval=ChannelFieldTypes.ERT_FIELD)
MST = ChannelFieldTypes(numval=ChannelFieldTypes.MST_FIELD)
LST = ChannelFieldTypes(numval=ChannelFieldTypes.LST_FIELD)
RCT = ChannelFieldTypes(numval=ChannelFieldTypes.RCT_FIELD)
ID = ChannelFieldTypes(numval=ChannelFieldTypes.ID_FIELD)
TITLE = ChannelFieldTypes(numval=ChannelFieldTypes.TITLE_FIELD)
FSW_NAME = ChannelFieldTypes(numval=ChannelFieldTypes.FSW_NAME_FIELD)
MODULE = ChannelFieldTypes(numval=ChannelFieldTypes.MODULE_FIELD)
OPS_CAT = ChannelFieldTypes(numval=ChannelFieldTypes.OPS_CAT_FIELD)
SUBSYSTEM = ChannelFieldTypes(numval=ChannelFieldTypes.SUBSYSTEM_FIELD)
DN_UNIT = ChannelFieldTypes(numval=ChannelFieldTypes.DN_UNIT_FIELD)
EU_UNIT = ChannelFieldTypes(numval=ChannelFieldTypes.EU_UNIT_FIELD)
ALARM_STATE = ChannelFieldTypes(numval=ChannelFieldTypes.ALARM_STATE_FIELD)
DSS_ID = ChannelFieldTypes(numval=ChannelFieldTypes.DSS_ID_FIELD)
RECORDED = ChannelFieldTypes(numval=ChannelFieldTypes.RECORDED_FIELD)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
