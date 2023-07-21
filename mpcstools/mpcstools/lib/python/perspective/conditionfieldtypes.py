#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines the channel sub-fields that can be checked by channel conditions
in an MPCS perspective.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil


class ConditionFieldTypes(mpcsutil.enum.EnumType):
    ''' Defines the possible types of channel sub-fields that can be checked by a channel condition
    in a Fixed Layout view.'''

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

    VALUES = ['DN','RAW','EU','VALUE','STATUS', 'SCET', 'SCLK', 'ERT', 'MST', 'LST', 'RCT']

    def __init__(self,numval=None,strval=None):
        super(ConditionFieldTypes,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'perspective.conditionfieldtypes.ConditionFieldTypes(numval=%d)' % self.val_num

    def get_default_title(self):

        return self.val_str

    def get_values(self):
        return ConditionFieldTypes.VALUES

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return ConditionFieldTypes(numval=self.val_num)


# Pre-defined convenience instances
DN = ConditionFieldTypes(numval=ConditionFieldTypes.DN_FIELD)
RAW = ConditionFieldTypes(numval=ConditionFieldTypes.RAW_FIELD)
EU = ConditionFieldTypes(numval=ConditionFieldTypes.EU_FIELD)
VALUE = ConditionFieldTypes(numval=ConditionFieldTypes.VALUE_FIELD)
STATUS = ConditionFieldTypes(numval=ConditionFieldTypes.STATUS_FIELD)
SCET = ConditionFieldTypes(numval=ConditionFieldTypes.SCET_FIELD)
SCLK = ConditionFieldTypes(numval=ConditionFieldTypes.SCLK_FIELD)
ERT = ConditionFieldTypes(numval=ConditionFieldTypes.ERT_FIELD)
MST = ConditionFieldTypes(numval=ConditionFieldTypes.MST_FIELD)
LST = ConditionFieldTypes(numval=ConditionFieldTypes.LST_FIELD)
RCT = ConditionFieldTypes(numval=ConditionFieldTypes.RCT_FIELD)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
