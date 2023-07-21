#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines the types of actions that can be taken by a button in an MPCS
fixed layout.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil


class ActionTypes(mpcsutil.enum.EnumType):
    ''' Defines the possible types of button actions in a Fixed Layout view.'''

    LAUNCH_PAGE_TYPE = 0
    LAUNCH_SCRIPT_TYPE = 1

    VALUES = ['LAUNCH_PAGE','LAUNCH_SCRIPT']

    def __init__(self,numval=None,strval=None):
        super(ActionTypes,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'perspective.actiontypes.ActionTypes(numval=%d)' % self.val_num

    def get_default_title(self):

        return self.val_str

    def get_values(self):
        return ActionTypes.VALUES

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return ActionTypes(numval=self.val_num)


# Pre-defined convenience instances
LAUNCH_PAGE = ActionTypes(numval=ActionTypes.LAUNCH_PAGE_TYPE)
LAUNCH_SCRIPT = ActionTypes(numval=ActionTypes.LAUNCH_SCRIPT_TYPE)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
