#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines the coordinate systems used by Fixed Layout views.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil


class CoordinateTypes(mpcsutil.enum.EnumType):
    ''' Defines the possible coordinate systems used by a Fixed Layout view.'''

    PIXEL_TYPE = 0
    CHARACTER_TYPE = 1

    VALUES = ['PIXEL','CHARACTER']

    def __init__(self,numval=None,strval=None):
        super(CoordinateTypes,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'perspective.coordinatetypes.CoordinateTypes(numval=%d)' % self.val_num

    def get_default_title(self):

        return self.val_str

    def get_values(self):
        return CoordinateTypes.VALUES

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return CoordinateTypes(numval=self.val_num)


# Pre-defined convenience instances
PIXEL = CoordinateTypes(numval=CoordinateTypes.PIXEL_TYPE)
CHARACTER = CoordinateTypes(numval=CoordinateTypes.CHARACTER_TYPE)
