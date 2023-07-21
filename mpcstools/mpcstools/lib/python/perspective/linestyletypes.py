#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines the types of line styles that can be used by line/box fields in an MPCS
perspective.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil


class LineStyleTypes(mpcsutil.enum.EnumType):
    ''' Defines the possible types of line styles in a Fixed Layout view.'''

    SOLID_TYPE = 0
    DASHED_TYPE = 1

    VALUES = ['SOLID','DASHED']

    def __init__(self,numval=None,strval=None):
        super(LineStyleTypes,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'perspective.linestyletypes.LineStyleTypes(numval=%d)' % self.val_num

    def get_default_title(self):

        return self.val_str

    def get_values(self):
        return LineStyleTypes.VALUES

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return LineStyleTypes(numval=self.val_num)


# Pre-defined convenience instances
SOLID = LineStyleTypes(numval=LineStyleTypes.SOLID_TYPE)
DASHED = LineStyleTypes(numval=LineStyleTypes.DASHED_TYPE)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
