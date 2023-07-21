#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import mpcsplot
import mpcsutil
from decimal import Decimal

class AxisTypes(mpcsutil.enum.EnumType):

    # MPCS-8237 9/29/2016 - Added LST support
    SCLK_TYPE = 0
    SCET_TYPE = 1
    ERT_TYPE = 2
    EHA_TYPE = 3
    EVR_TYPE = 4
    LST_TYPE = 5
    VALUES = ['SCLK','SCET','ERT','EHA','EVR', "LST"]

    def __init__(self,numval=None,strval=None):
        super(AxisTypes,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'mpcsplot.axistypes.AxisTypes(numval=%d)' % self.val_num

    def get_default_title(self):

        if self.val_num == AxisTypes.EHA_TYPE:
            return 'EHA Value'
        elif self.val_num == AxisTypes.EVR_TYPE:
            return 'EVR Level'

        return self.val_str.upper()

    def get_values(self):
        return AxisTypes.VALUES

    @staticmethod
    def is_x_axis_type(value):

        return value.upper() in [SCLK.val_str,SCET.val_str,ERT.val_str, LST.val_str]

    @staticmethod
    def is_y_axis_type(value):

        return value.upper() in [EHA.val_str,EVR.val_str]

    def is_redefinable_range(self):

        return self.val_num != AxisTypes.EVR_TYPE

    def get_sort_field(self):

        if self.val_num == AxisTypes.SCLK_TYPE:

            return 'sclkExact'

        elif self.val_num == AxisTypes.SCET_TYPE:

            return 'scetExact'

        elif self.val_num == AxisTypes.ERT_TYPE:

            return 'ertExact'

        raise 'Unsupported sort ordering for axis!'

    def get_tick_formatters(self):

        if self.val_num == AxisTypes.SCLK_TYPE:

            return (mpcsplot.formatter.SclkFormatter(),
                    mpcsplot.formatter.SclkFormatter())

        elif self.val_num == AxisTypes.SCET_TYPE or self.val_num == AxisTypes.ERT_TYPE:
            precision = mpcsutil.timeutil.getScetPrecision() if self.val_num == AxisTypes.SCET_TYPE else mpcsutil.timeutil.getErtPrecision()
            return (mpcsplot.formatter.DateFormatter(precision),
                    mpcsplot.formatter.DateFormatter(precision))

        elif self.val_num == AxisTypes.EVR_TYPE:

            return (mpcsplot.formatter.EvrFormatter(),None)
        elif self.val_num == AxisTypes.LST_TYPE:
          return (mpcsplot.formatter.LstFormatter(),mpcsplot.formatter.LstFormatter())

        elif self.val_num == AxisTypes.EHA_TYPE:

            #raise NotImplementedError('Tick formatters for DN & EU are not yet implemented.')
            return (mpcsplot.formatter.EhaFormatter(),
                    mpcsplot.formatter.EhaFormatter())

    def user_to_axis_format(self,user_entry):
        '''Convert the value from the value entered by the
        user to a value used by the axis.'''

        if not self.is_redefinable_range():
            return user_entry
        if self.val_num == AxisTypes.SCLK_TYPE:
            return mpcsutil.timeutil.getSclkFloatingPoint(mpcsutil.timeutil.parseSclkString(user_entry))
        elif self.val_num == AxisTypes.SCET_TYPE:
            return mpcsutil.timeutil.parseTimeString(user_entry)
            #return Decimal(mpcsutil.timeutil.parseTimeString(user_entry)/ 1000)
            #scetMs = mpcsutil.timeutil.parseTimeString(user_entry)
            #if user_entry.find(".") > 0 and len(user_entry.split(".")[1]) > 3 and mpcsutil.timeutil.getScetPrecision() > 3:
                #return Decimal(scetMs) + Decimal(int(user_entry.split(".")[1][3:]) / 100000000.0)
            #else:
            #    return scetMs
        elif self.val_num == AxisTypes.ERT_TYPE:
            return mpcsutil.timeutil.parseTimeString(user_entry)
        elif self.val_num == AxisTypes.EHA_TYPE:
            return float(user_entry)

        elif self.val_num == AxisTypes.LST_TYPE:
            return mpcsutil.timeutil.lstToSclk(user_entry)
        return user_entry

    def format_item_for_axis(self,item,useEu=False):

        if self.val_num == AxisTypes.SCLK_TYPE:
            return mpcsutil.timeutil.getSclkFloatingPoint(mpcsutil.timeutil.parseSclkString(item.sclk))
        elif self.val_num == AxisTypes.SCET_TYPE:
            scetMs = mpcsutil.timeutil.parseTimeString(item.scet)
            if item.scetNano > 0 and mpcsutil.timeutil.getScetPrecision() > 3:
                #print "scetMs=%s, NN=%s, nanos=%f, added=%s" % (scetMs, item.scetNano, float(int(item.scetNano)/100000000.0), scetMs+float(int(item.scetNano)/100000000.0))
                return Decimal(scetMs) + Decimal((item.scetNano)/1000000.0)
            else:
                return scetMs
        elif self.val_num == AxisTypes.ERT_TYPE:
            ertMs = mpcsutil.timeutil.parseTimeString(item.ert)
            if item.ertExactFine > 0 and mpcsutil.timeutil.getErtPrecision() > 3:
                return Decimal(ertMs) + Decimal((item.ertExactFine)/1000000.0)
            else:
                return ertMs
        elif self.val_num == AxisTypes.LST_TYPE:
            # Return the sclk value from the item.  Convert to float first.
            return mpcsutil.timeutil.getSclkFloatingPoint(mpcsutil.timeutil.parseSclkString(item.sclk))
        elif self.val_num == AxisTypes.EHA_TYPE or self.val_num == AxisTypes.EVR_TYPE:
            if useEu:
                return item.eu
            else:
                return item.dn
        elif self.val_num == AxisTypes.EVR_TYPE:
            if item in mpcsutil.evr.value_to_level_map:
                return mpcsutil.evr.value_to_level_map[item]
            return 'UNKNOWN'

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return AxisTypes(numval=self.val_num)

SCLK = AxisTypes(numval=AxisTypes.SCLK_TYPE)
SCET = AxisTypes(numval=AxisTypes.SCET_TYPE)
# 5/15/2012 - MPCS-3673 - Updating for LST time types.
LST = AxisTypes(numval=AxisTypes.LST_TYPE)
ERT = AxisTypes(numval=AxisTypes.ERT_TYPE)
EHA = AxisTypes(numval=AxisTypes.EHA_TYPE)
EVR = AxisTypes(numval=AxisTypes.EVR_TYPE)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
