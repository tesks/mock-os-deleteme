#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines the types of fields that can be utilized in a fixed layout view.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil


class FixedFieldTypes(mpcsutil.enum.EnumType):
    ''' Defines the possible field types used by a Fixed Layout view.'''

    TEXT_TYPE = 0
    LINE_TYPE = 1
    BOX_TYPE = 2
    CHANNEL_TYPE = 3
    TIME_TYPE = 4
    IMAGE_TYPE = 5
    BUTTON_TYPE = 6
    HEADER_TYPE = 7

    VALUES = ['TEXT','LINE','BOX','CHANNEL','TIME','IMAGE','BUTTON','HEADER']

    def __init__(self,numval=None,strval=None):
        super(FixedFieldTypes,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'perspective.fixedfieldtypes.FixedFieldTypes(numval=%d)' % self.val_num

    def get_default_title(self):
        return self.val_str

    def get_values(self):
        return FixedFieldTypes.VALUES

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return FixedFieldTypes(numval=self.val_num)

# Pre-defined convenience instances
TEXT = FixedFieldTypes(numval=FixedFieldTypes.TEXT_TYPE)
LINE = FixedFieldTypes(numval=FixedFieldTypes.LINE_TYPE)
CHANNEL = FixedFieldTypes(numval=FixedFieldTypes.CHANNEL_TYPE)
TIME = FixedFieldTypes(numval=FixedFieldTypes.TIME_TYPE)
BUTTON = FixedFieldTypes(numval=FixedFieldTypes.BUTTON_TYPE)
IMAGE = FixedFieldTypes(numval=FixedFieldTypes.IMAGE_TYPE)
BOX = FixedFieldTypes(numval=FixedFieldTypes.BOX_TYPE)
HEADER = FixedFieldTypes(numval=FixedFieldTypes.HEADER_TYPE)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
