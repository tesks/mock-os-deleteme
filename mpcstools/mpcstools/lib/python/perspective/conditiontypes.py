#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines the types of conditions that can be checked for channels
in an MPCS perspective.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil


class ConditionTypes(mpcsutil.enum.EnumType):
    ''' Defines the possible types of conditions that can be checked for channels
    in a Fixed Layout view.'''

    LT_COND = 0         # less than
    GT_COND = 1         # greater than
    LE_COND = 2         # less than or equal
    GE_COND = 3         # greater than or equal
    EQ_COND = 4         # equal
    NE_COND = 5         # not equal
    SET_COND = 6        # alarm set
    NOT_SET_COND = 7    # alarm not set
    STALE_COND = 8      # value stale
    TYPE_COND = 9       # channel is type
    IS_NULL_COND = 10   # channel value is null

    VALUES = ['LT','GT','LE','GE','EQ', 'NE', 'SET', 'NOT_SET', 'STALE', 'TYPE', 'IS_NULL']

    def __init__(self,numval=None,strval=None):
        super(ConditionTypes,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'perspective.conditiontypes.ConditionTypes(numval=%d)' % self.val_num

    def get_default_title(self):

        return self.val_str

    def get_values(self):
        return ConditionTypes.VALUES

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return ConditionTypes(numval=self.val_num)


# Pre-defined convenience instances
LT = ConditionTypes(numval=ConditionTypes.LT_COND)
GT = ConditionTypes(numval=ConditionTypes.GT_COND)
LE = ConditionTypes(numval=ConditionTypes.LE_COND)
GE = ConditionTypes(numval=ConditionTypes.GE_COND)
EQ = ConditionTypes(numval=ConditionTypes.EQ_COND)
NE = ConditionTypes(numval=ConditionTypes.NE_COND)
SET = ConditionTypes(numval=ConditionTypes.SET_COND)
NOT_SET = ConditionTypes(numval=ConditionTypes.NOT_SET_COND)
STALE = ConditionTypes(numval=ConditionTypes.STALE_COND)
TYPE = ConditionTypes(numval=ConditionTypes.TYPE_COND)
IS_NULL = ConditionTypes(numval=ConditionTypes.IS_NULL_COND)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
