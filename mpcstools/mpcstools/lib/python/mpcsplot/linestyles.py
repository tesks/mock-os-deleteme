#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import mpcsutil

class LineStyles(mpcsutil.enum.EnumType):

    SOLID_TYPE = 0
    DASHED_TYPE = 1
    DASHDOT_TYPE = 2
    DOTTED_TYPE = 3

    VALUES = ['solid','dashed','dashdot','dotted']

    def __init__(self,numval=None,strval=None):
        super(LineStyles,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'mpcsplot.linestyles.LineStyles(numval=%d)' % self.val_num

    def get_prop_val(self):

        if self.val_num == LineStyles.SOLID_TYPE:
            return '-'
        elif self.val_num == LineStyles.DASHED_TYPE:
            return '--'
        elif self.val_num == LineStyles.DASHDOT_TYPE:
            return '-.'
        elif self.val_num == LineStyles.DOTTED_TYPE:
            return ':'

    def get_values(self):
        return LineStyles.VALUES

    @staticmethod
    def get_default():
        return LineStyles(numval=LineStyles.SOLID_TYPE)

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return LineStyles(numval=self.val_num)

SOLID = LineStyles(numval=LineStyles.SOLID_TYPE)
DASHED = LineStyles(numval=LineStyles.DASHED_TYPE)
DASHDOT = LineStyles(numval=LineStyles.DASHDOT_TYPE)
DOTTED = LineStyles(numval=LineStyles.DOTTED_TYPE)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
