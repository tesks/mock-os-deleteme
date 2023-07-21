#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import copy
import mpcsutil



class DrawStyles(mpcsutil.enum.EnumType):

    LINE_TYPE = 0
    STEP_TYPE = 1
    PRE_STEP_TYPE = 2
    MID_STEP_TYPE = 3
    POST_STEP_TYPE = 4

    VALUES = ['line','step','pre-step','mid-step','post-step']

    def __init__(self,numval=None,strval=None):
        super(DrawStyles,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'mpcsplot.drawstyles.DrawStyles(numval=%d)' % self.val_num

    def get_prop_val(self):

        if self.val_num == DrawStyles.LINE_TYPE:
            return 'default'
        elif self.val_num == DrawStyles.STEP_TYPE:
            return 'steps'
        elif self.val_num == DrawStyles.PRE_STEP_TYPE:
            return 'steps-pre'
        elif self.val_num == DrawStyles.MID_STEP_TYPE:
            return 'steps-mid'
        elif self.val_num == DrawStyles.POST_STEP_TYPE:
            return 'steps-post'

    def get_values(self):
        return DrawStyles.VALUES

    @staticmethod
    def get_default():
        return DrawStyles(numval=DrawStyles.LINE_TYPE)

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return DrawStyles(numval=self.val_num)

LINE = DrawStyles(numval=DrawStyles.LINE_TYPE)
STEP = DrawStyles(numval=DrawStyles.STEP_TYPE)
PRE_STEP = DrawStyles(numval=DrawStyles.PRE_STEP_TYPE)
MID_STEP = DrawStyles(numval=DrawStyles.MID_STEP_TYPE)
POST_STEP = DrawStyles(numval=DrawStyles.POST_STEP_TYPE)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
