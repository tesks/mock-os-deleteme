#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)
import math

import matplotlib
import matplotlib.ticker
from decimal import Decimal

import mpcsutil

class DateFormatter(matplotlib.ticker.Formatter):

    def __init__(self, precision):

        self.lower_annotation = None
        self.upper_annotation = None

        if precision and not isinstance(precision, int):
            self.precision = int(precision)
        else:
            self.precision = precision #Pass in precision to use with formatter or accept default=3

    def __call__(self,val,pos=None):
        nanos=""
        if str(val/1000).find(".") > 0: # nanoseconds exist
            _nanos = str(round(Decimal(val) % 1 , self.precision)).split(".")
            if len(_nanos)>2:
                nanos=_nanos[1]# everything after decimal
        val_str = mpcsutil.timeutil.getTimeStringExt(ms=int(val),nanos=nanos, precision=self.precision)
        return val_str.replace('T','\n')

# MPCS-8237 9/29/2016 - Added LST support
class LstFormatter(matplotlib.ticker.Formatter):
    def __init__(self):
        pass

    def __call__(self,val,pos=None):
      # This will get the sclk value passed as val.  Convert sclk to lst and return.
      return mpcsutil.timeutil.sclkToLst(val)

class SclkFormatter(matplotlib.ticker.Formatter):
    def __init__(self):
        pass

    def __call__(self,val,pos=None):
        return '%10.5f' % (val)

class EhaFormatter(matplotlib.ticker.Formatter):

    def __init__(self):
        pass

    def __call__(self,val,pos=None):

        return '%s' % (val)

class EvrFormatter(matplotlib.ticker.Formatter):

    def __init__(self):

        self.level_to_value_map = mpcsutil.evr.level_to_value_map
        self.value_to_level_map = mpcsutil.evr.value_to_level_map

    def __call__(self,val,pos=None):

        current = int(round(val))
        if current in self.value_to_level_map:
            return self.value_to_level_map[current]

        return ''

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
