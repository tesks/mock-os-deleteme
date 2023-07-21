#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import mpcsutil

import copy

class LegendLocations(mpcsutil.enum.EnumType):

    BEST_TYPE = 0
    UPPER_RIGHT_TYPE = 1
    UPPER_LEFT_TYPE = 2
    LOWER_LEFT_TYPE = 3
    LOWER_RIGHT_TYPE = 4
    RIGHT_TYPE = 5
    CENTER_LEFT_TYPE = 6
    CENTER_RIGHT_TYPE = 7
    LOWER_CENTER_TYPE = 8
    UPPER_CENTER_TYPE = 9
    CENTER_TYPE = 10

    VALUES = ['best','upper_right','upper_left','lower_left','lower_right','right',
              'center_left','center_right','lower_center','upper_center','center']

    def __init__(self,numval=None,strval=None):
        super(LegendLocations,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'mpcsplot.legendlocations.LegendLocations(numval=%d)' % self.val_num

    def get_prop_val(self):
        return self.val_num

    def get_values(self):
        return LegendLocations.VALUES

    @staticmethod
    def get_default():
        return LegendLocations(numval=LegendLocations.BEST_TYPE)

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return LegendLocations(numval=self.val_num)

BEST = LegendLocations(numval=LegendLocations.BEST_TYPE)
UPPER_RIGHT = LegendLocations(numval=LegendLocations.UPPER_RIGHT_TYPE)
UPPER_LEFT = LegendLocations(numval=LegendLocations.UPPER_LEFT_TYPE)
LOWER_LEFT = LegendLocations(numval=LegendLocations.LOWER_LEFT_TYPE)
LOWER_RIGHT = LegendLocations(numval=LegendLocations.LOWER_RIGHT_TYPE)
RIGHT = LegendLocations(numval=LegendLocations.RIGHT_TYPE)
CENTER_LEFT = LegendLocations(numval=LegendLocations.CENTER_LEFT_TYPE)
CENTER_RIGHT = LegendLocations(numval=LegendLocations.CENTER_RIGHT_TYPE)
LOWER_CENTER = LegendLocations(numval=LegendLocations.LOWER_CENTER_TYPE)
UPPER_CENTER = LegendLocations(numval=LegendLocations.UPPER_CENTER_TYPE)
CENTER = LegendLocations(numval=LegendLocations.CENTER_TYPE)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
