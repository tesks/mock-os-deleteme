#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines the data types of MPCS telemetry channels.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil


class ChannelTypes(mpcsutil.enum.EnumType):
    ''' Defines the possible data types for telemetry channels.'''

    UNSIGNED_INT = 0
    SIGNED_INT = 1
    BOOLEAN = 2
    FLOAT = 3
    DOUBLE = 4
    STATUS = 5
    ASCII = 6
    DIGITAL = 7

    VALUES = ['UNSIGNED_INT','SIGNED_INT','BOOLEAN','FLOAT','DOUBLE', 'STATUS', 'ASCII', 'DIGITAL']

    def __init__(self,numval=None,strval=None):
        super(ChannelTypes,self).__init__(numval=numval,strval=strval)

    def __repr__(self):
        return 'perspective.channeltypes.ChannelTypes(numval=%d)' % self.val_num

    def get_default_title(self):

        return self.val_str

    def get_values(self):
        return ChannelTypes.VALUES

    def __copy__(self):
        return self.__deepcopy__({})

    def __deepcopy__(self, memo):
        return ChannelTypes(numval=self.val_num)


# Pre-defined convenience instances
UNSIGNED_INT = ChannelTypes(numval=ChannelTypes.UNSIGNED_INT)
SIGNED_INT = ChannelTypes(numval=ChannelTypes.SIGNED_INT)
BOOLEAN = ChannelTypes(numval=ChannelTypes.BOOLEAN)
FLOAT = ChannelTypes(numval=ChannelTypes.FLOAT)
DOUBLE = ChannelTypes(numval=ChannelTypes.DOUBLE)
STATUS = ChannelTypes(numval=ChannelTypes.STATUS)
ASCII = ChannelTypes(numval=ChannelTypes.ASCII)
DIGITAL = ChannelTypes(numval=ChannelTypes.DIGITAL)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
